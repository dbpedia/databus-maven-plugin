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
import org.apache.commons.compress.compressors.{CompressorInputStream, CompressorStreamFactory}
import org.apache.maven.plugin.logging.Log
import org.eclipse.rdf4j.rio.Rio
import resource._

import scala.io.{Codec, Source}
import scala.util.{Failure, Success, Try}
import java.io._
import java.nio.charset.MalformedInputException
import java.nio.file.Files
import java.util.Base64

import org.dbpedia.databus.FileInfoCache


/**
  * a simple dao to collect all values for a file
  * private constructor, must be called with init to handle compression detection
  */
class Datafile private(val file: File, previewLineCount: Int = 10)(implicit log: Log) {

  lazy val (filePrefix: String, contentVariantExtensions: Seq[String], formatVariantExtensions: Seq[String],
  compressionVariantExtensions) = new FilenameHelpers(file).filenameAnalysis

  lazy val (format, formatExtension) = computeMimeType(formatVariantExtensions)

  lazy val bytes: Long = file.length()

  // compression options
  lazy val archiveVariant: Option[String] = Compression.detectArchive(file.toScala)
  lazy val compressionVariant: Option[String] = Compression.detectCompression(file.toScala)

  def compressionOrArchiveDesc =
    (Stream.empty ++ compressionVariant ++ archiveVariant).headOption.getOrElse("None")

  // sum
  lazy val sha256sum: String = signing.sha256Hash(file.toScala).asBytes.map("%02x" format _).mkString

  var fileInfoCache: FileInfoCache = null

  // collected by updateSignature
  var signatureBytes: Array[Byte] = Array.empty
  var signatureBase64: String = ""
  var verified: Boolean = false

  // some quality metrics
  var nonEmptyLines = 0L
  var duplicates = 0L
  var sorted: Boolean = false
  var uncompressedByteSize = 0L


  lazy val preview: String = computePreview

  /**
    * initialization
    * * checks whether the file exists
    * * detects compression for further reading
    */
  def ensureExists(): Datafile = {

    if (file.exists()) {

      if (!file.toScala.isRegularFile) {

        throw new FileNotFoundException(s"${file.getAbsolutePath} is not a regular file")
      }
    } else throw new FileNotFoundException("File not found: " + file.getAbsolutePath)

    this
  }

  def basename(): String = {
    file.getName
  }

  def finalBasename(versionToAdd: Option[String]): String = {

    def prefix = filePrefix
    // deactivated adding of version for now
    //versionToAdd.fold(filePrefix) { version => s"$filePrefix-$version" }

    // we have to check for empty extension seqs, otherwise we get misplaced inital '.'/'_'
    def contentVariantsSuffix = if (contentVariantExtensions.nonEmpty) {
      contentVariantExtensions.mkString("_", "_", "")
    } else ""

    def formatVariantsSuffix = if (formatVariantExtensions.nonEmpty) {
      formatVariantExtensions.mkString(".", ".", "")
    } else ""

    def compressionVariantsSuffix = if (compressionVariantExtensions.nonEmpty) {
      compressionVariantExtensions.mkString(".", ".", "")
    } else ""

    prefix + contentVariantsSuffix + formatVariantsSuffix + compressionVariantsSuffix
  }

  private def computeMimeType(formatVariants: Seq[String]) = {

    val (ext, mimeTypeByFileName) = Format.detectMimeTypeByFileExtension(formatVariants)

    // downgrade turtle to ntriple, if linebased
    val mimeType: Format = if (mimeTypeByFileName == TextTurtle) {

      val baos: ByteArrayInputStream = new ByteArrayInputStream(preview.getBytes)
      val (_, _, good, wrongTriples) = LineBasedRioDebugParser.parse(baos, Rio.createParser(ApplicationNTriples.rio))


      if (good > 0) {
        ApplicationNTriples
      } else {
        //println(wrongTriples.mkString("\n"))
        mimeTypeByFileName
      }

    } else mimeTypeByFileName

    (mimeType, ext)
  }

  /*
  private def filenameAnalysis: (String, Seq[String], Seq[String], Seq[String]) = {

    parse(basename, Datafile.databusInputFilenameP(_)) match {

      case success@Parsed.Success(basenameParts, _) => basenameParts

      case failure: Parsed.Failure =>
        sys.error(s"Unable to analyse filename '${basename}': Please refer to " +
          s"http://dev.dbpedia.org/Databus%20Maven%20Plugin on conventions for input file naming:\n" +
          failure.trace().longAggregateMsg)
    }
  }*/

  /**
    * @return gives the preview, however it is limited to 500 chars per line, in case there is a very long line
    */
  private def computePreview: String = {

    val unshortenedPreview = for {

      inputStream <- getInputStream
      source <- managed(Source.fromInputStream(inputStream)(Codec.UTF8))

    } yield {
      Try(source.getLines().take(previewLineCount).mkString("\n"))
    }

    def maxLength = previewLineCount * 3000

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
    * translates a java (signed!) byte into an unsigned byte (emulated via short)
    *
    * @param b signed byte to convert to unsigned byte value
    * @return the unsigned byte value stored as short
    */
  def toUnsignedByte(b: Byte) = {
    val aByte: Int = 0xff & b.asInstanceOf[Int]
    aByte.asInstanceOf[Short]
  }

  /**
    * does a bytewise string comparison in scala similar to LC_ALL=C sort does in Unix
    *
    * @param a
    * @param b
    * @return a negative value if a is in byte order before b, zero if a and b bytestreams match and, and a positive value else
    */
  def compareStringsBytewise(a: String, b: String): Int = {
    val ab = a.getBytes("UTF-8")
    val bb = b.getBytes("UTF-8")

    compareBytewise(ab, bb)
  }

  /**
    * does a bytewise comparison in scala
    *
    * @param ab
    * @param bb
    * @return a negative value if a is in byte order before b, zero if a and b bytestreams match and, and a positive value else
    */
  def compareBytewise(ab: Array[Byte], bb: Array[Byte]): Int = {

    var mLength = scala.math.min(ab.length, bb.length)

    for (i <- 0 to mLength - 1) {
      if (ab(i) == bb(i)) {}
      else
        return toUnsignedByte(ab(i)).compareTo(toUnsignedByte(bb(i)))
    }
    return ab.length - bb.length
  }


  def updateFileMetrics(): Datafile = {

    if (fileInfoCache != null) {
      log.debug("using cache for fileMetrics")
      nonEmptyLines = fileInfoCache.nonEmptyLines
      duplicates = fileInfoCache.duplicates
      sorted = fileInfoCache.sorted
      uncompressedByteSize = fileInfoCache.uncompressedByteSize
      this
    }

    var nonEmpty = 0L
    var dupes = 0L
    var sort: Boolean = true
    var charSize = 0L
    var uncompressedSize = 0L

    var previousLine: String = null
    try {
      val res = getInputStream()
      res.apply { in =>
        val it: Iterator[String] = Source.fromInputStream(in)(Codec.UTF8).getLines()
        while (it.hasNext) {
          val line = it.next()
          charSize += line.size //now counts the number of chars (not including linefeeds)

          // non empty lines
          if (!line.trim.isEmpty) {
            nonEmpty += 1
          }

          // sorted or duplicate
          if (previousLine != null) {
            val lineb = line.getBytes("UTF-8")
            val previousLineb = previousLine.getBytes("UTF-8")
            uncompressedSize += lineb.size + 1 //estimated bytesize count !!! this does not respect windows linefeeds properly (or potentially other control characters) that is why it is overwritten by stream information if possbile 
            val cmp = compareBytewise(lineb, previousLineb)
            if (cmp == 0) {
              dupes += 1
            } else if (cmp < 0) {
              def cut(s: String): String = {
                if (s.length > 41) s.substring(0, 40) else s
              }

              log.debug("Sortorder non-ascii line " + nonEmpty + ": |" + cut(previousLine) + ">" + cut(line) + "|")
              sort = false
            }
          }
          previousLine = line

        }

        //now try to determine the accurate uncompressed byte size by reading it from underlying stream an override estimated one if possible
        uncompressedSize = in match {
          case c: CompressorInputStream => c.getBytesRead
          case a: ArchiveInputStream => a.getBytesRead
          case i: BufferedInputStream => this.bytes
          case _ => log.warn(s"Bytesize only approximated for file: ${this.file.getAbsolutePath}"); uncompressedSize //TODO  maybe wrap the streams in Datafile with a generic counting input stream
        }

      }

      nonEmptyLines = nonEmpty
      duplicates = dupes
      sorted = sort
      uncompressedByteSize = uncompressedSize
    } catch {
      /*case mfe: MalformedInputException =>
        nonEmptyLines = nonEmpty
        duplicates = dupes
        sorted = sort
        uncompressedByteSize = uncompressedSize */
      case e: Exception =>
        log.warn(s"Read Error for file: ${this.file.getAbsolutePath} not calculate line-based file metadata statistics (e.g. #empty lines) and also uncompressed byte-size for File")
        nonEmptyLines = -1
        duplicates = -1
        sorted = false
        uncompressedByteSize = -1
    }


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
       |name=${file.getAbsoluteFile.toString}
       |sha256sum=$sha256sum
       |bytes=$bytes
       |uncompressedByteSize=$uncompressedByteSize
       |archiveVariant=$archiveVariant
       |compressionVariant=$compressionVariant
       |signatureBytes=${signatureBytes.map("%02X" format _).mkString}
       |signatureBase64=$signatureBase64
       |verified=$verified)
       |nonEmptyLines=$nonEmptyLines)
       |duplicates=$duplicates)
       |sorted=$sorted)
     """.stripMargin
}

object Datafile extends LazyLogging {


  def apply(file: File, previewLineCount: Int = 10)(implicit log: Log): Datafile = {
    new Datafile(file, previewLineCount)(log)
  }

  /*
    protected def alphaNumericP[_: P] = CharIn("A-Za-z0-9").rep(1)

    // the negative lookahead ensures that we do not parse into the format extension(s) if there is no content variant
    protected def artifactNameP[_: P] =
      P((!extensionP ~ CharPred(_ != '_')).rep(1).!)
        .opaque("<filename prefix>")

    protected def contentVariantsP[_: P] =
      P(("_" ~ alphaNumericP.!).rep())
        .opaque("<content variants>")

    protected def extensionP[_: P] = "." ~ (CharIn("A-Za-z") ~ CharIn("A-Za-z0-9").rep()).!

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
  */
}
