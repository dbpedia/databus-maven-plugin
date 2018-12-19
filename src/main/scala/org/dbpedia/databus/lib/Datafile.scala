/*-
 * #%L
 * databus-maven-plugin
 * %%
 * Copyright (C) 2018 Sebastian Hellmann (on behalf of the DBpedia Association)
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

package org.dbpedia.databus.lib

import org.dbpedia.databus.parse.LineBasedRioDebugParser
import org.dbpedia.databus.shared.authentification.RSAKeyPair
import org.dbpedia.databus.shared.signing
import org.dbpedia.databus.voc.{ApplicationNTriples, Format, TextTurtle}

import better.files.{File => _, _}
import com.typesafe.scalalogging.LazyLogging
import fastparse.NoWhitespace._
import fastparse._
import org.apache.commons.compress.archivers.{ArchiveInputStream, ArchiveStreamFactory}
import org.apache.commons.compress.compressors.CompressorStreamFactory
import org.apache.maven.plugin.logging.Log
import org.eclipse.rdf4j.rio.Rio
import resource._

import scala.io.{Codec, Source}
import scala.util.{Failure, Success, Try}

import java.io._
import java.nio.charset.MalformedInputException
import java.nio.file.Files
import java.util.Base64


/**
  * a simple dao to collect all values for a file
  * private constructor, must be called with init to handle compression detection
  */
class Datafile private(val file: File, previewLineCount: Int = 10, skipHashing: Boolean = false)(implicit log: Log) {

  lazy val (filePrefix: String, contentVariantExtensions: Seq[String], formatVariantExtensions: Seq[String],
    compressionVariantExtensions) = filenameAnalysis

  lazy val (format, formatExtension) = computeMimeType(formatVariantExtensions)

  lazy val sha256sum: String = if(skipHashing) "" else
    signing.sha256Hash(file.toScala).asBytes.map("%02x" format _).mkString

  lazy val bytes: Long = file.length()

  // compression option
  lazy val archiveVariant: Option[String] = Compression.detectArchive(file.toScala)
  lazy val compressionVariant: Option[String] = Compression.detectCompression(file.toScala)

  def compressionOrArchiveDesc =
    (Stream.empty ++ compressionVariant ++ archiveVariant).headOption.getOrElse("None")

  var signatureBytes: Array[Byte] = Array.empty
  var signatureBase64: String = ""
  var verified: Boolean = false

  lazy val preview: String = computePreview

  /**
    * initialization
    * * checks whether the file exists
    * * detects compression for further reading
    */
  def ensureExists(): Datafile = {

    if(file.exists()) {

      if(!file.toScala.isRegularFile) {

        throw new FileNotFoundException(s"${file.getAbsolutePath} is not a regular file")
      }
    } else throw new FileNotFoundException("File not found: " + file.getAbsolutePath)

    this
  }

  def basename(): String = {
    file.getName
  }

  def finalBasename(versionToAdd: Option[String]) = {

    def prefix = versionToAdd.fold(filePrefix) { version => s"$filePrefix-$version" }

    // we have to check for empty extension seqs, otherwise we get misplaced inital '.'/'_'
    def contentVariantsSuffix = if(contentVariantExtensions.nonEmpty) {
      contentVariantExtensions.mkString("_", "_", "")
    } else ""

    def formatVariantsSuffix = if(formatVariantExtensions.nonEmpty) {
      formatVariantExtensions.mkString(".", ".", "")
    } else ""

    def compressionVariantsSuffix = if(compressionVariantExtensions.nonEmpty) {
      compressionVariantExtensions.mkString(".", ".", "")
    } else ""

    prefix + contentVariantsSuffix + formatVariantsSuffix + compressionVariantsSuffix
  }

  private def computeMimeType(formatVariants: Seq[String]) = {

    val (ext, mimeTypeByFileName) = Format.detectMimeTypeByFileExtension(formatVariants)

    // downgrade turtle to ntriple, if linebased
    val mimeType: Format = if(mimeTypeByFileName == TextTurtle) {

      val baos: ByteArrayInputStream = new ByteArrayInputStream(preview.getBytes);
      val (_, _, _, wrongTriples) = LineBasedRioDebugParser.parse(baos, Rio.createParser(ApplicationNTriples.rio))

      if(wrongTriples.isEmpty) ApplicationNTriples else mimeTypeByFileName

    } else mimeTypeByFileName

    (mimeType, ext)
  }

  private def filenameAnalysis: (String, Seq[String], Seq[String], Seq[String]) = {

    parse(basename, Datafile.databusInputFilenameP(_)) match {

      case success@Parsed.Success(basenameParts, _) => basenameParts

      case failure: Parsed.Failure =>
        sys.error(s"Unable to analyse filename '${basename}': Please refer to " +
          s"http://dev.dbpedia.org/Databus%20Maven%20Plugin on conventions for input file naming:\n" +
          failure.trace().longAggregateMsg)
    }
  }

  /**
    *
    * @param lineCount gives the linecount of the preview, however it is limited to 500 chars per line, in case there is a very long line
    * @return
    */
  private def computePreview: String = {

    val unshortenedPreview = for {

      inputStream <- getInputStream
      source <- managed(Source.fromInputStream(inputStream)(Codec.UTF8))

    } yield {
      Try(source.getLines().take(previewLineCount).mkString("\n"))
    }

    def maxLength = previewLineCount * 500

    unshortenedPreview apply {

      case Success(tooLong) if tooLong.size > maxLength => tooLong.substring(0, maxLength)

      case Success(shortEnough) => shortEnough

      case Failure(mie: MalformedInputException) => "[binary data - no preview]"
    }
  }

  def updateSignature(keyPair: RSAKeyPair): Datafile = {

    signatureBytes = signing.signFile(keyPair.privateKey, file.toScala)

    signatureBase64 = new String(Base64.getEncoder.encode(signatureBytes))

    verified = signing.verifyFile(keyPair.publicKey, signatureBytes, file.toScala)
    this
  }

  /**
    * Opens the file with compression, etc.
    * NOTE: if file is an archive, we assume it is only one file in it and this will be on the stream
    *
    * @return
    */
  def getInputStream(): ManagedResource[InputStream] = managed {

    val bis = file.toScala.newInputStream.buffered

    (compressionVariant, archiveVariant) match {

      case (Some(comp), None) => {
        new CompressorStreamFactory().createCompressorInputStream(bis)
      }

      case (None, Some(arch)) => {
        new ArchiveStreamFactory().createArchiveInputStream(bis)
      }

      case (None, None) => bis

      case (Some(comp), Some(arch)) => sys.error(s"file seems to be both compressed and an archive: $comp, $arch")
    }
  }

  override def toString =
    s"""
       |Datafile(format=$format
       |sha256sum=$sha256sum
       |bytes=$bytes
       |archiveVariant=$archiveVariant
       |compressionVariant=$compressionVariant
       |signatureBytes=${signatureBytes.map("%02X" format _).mkString}
       |signatureBase64=$signatureBase64
       |verified=$verified)
     """.stripMargin
}

object Datafile extends LazyLogging {

  def apply(file: File, previewLineCount: Int = 10, skipHashing: Boolean = false)(implicit log: Log): Datafile = {
    new Datafile(file, previewLineCount, skipHashing)(log)
  }

  protected def alphaNumericP[_: P] = CharIn("A-Za-z0-9").rep(1)

  // the negative lookahead ensures that we do not parse into the format extension(s) if there is no content variant
  protected def artifactNameP[_: P] =
    P((!extensionP ~ CharPred(_ != '_')).rep(1).!)
      .opaque("<filename prefix>")

  protected def contentVariantsP[_: P] =
    P(("_" ~ alphaNumericP.!).rep())
      .opaque("<content variants>")

  protected def extensionP[_ : P] = "." ~  (CharIn("A-Za-z") ~ CharIn("A-Za-z0-9").rep()).!

  // using a negative lookahead here to ensure that no compression extension is parsed as format extension
  protected def formatExtensionP[_: P] = P(!compressionExtensionP ~ extensionP)

  protected def formatExtensionsP[_: P] =
    P(formatExtensionP.rep(1))
      .opaque("<format variant extensions>")

  protected def compressionExtensionP[_: P] =
    P("." ~ StringIn("bz2", "gz", "tar", "xz", "zip").!)
      .opaque("<compression variant extensions>")

  protected def compressionExtensionsP[_: P] = P(compressionExtensionP.rep())

  protected def databusInputFilenameP[_: P]: P[(String, Seq[String], Seq[String], Seq[String])] =
    (Start ~ artifactNameP ~ contentVariantsP ~ formatExtensionsP ~ compressionExtensionsP ~ End)
}
