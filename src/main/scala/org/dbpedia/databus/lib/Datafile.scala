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

import org.dbpedia.databus.Properties
import org.dbpedia.databus.parse.LineBasedRioDebugParser
import org.dbpedia.databus.shared.authentification.RSAKeyPair
import org.dbpedia.databus.shared.signing
import org.dbpedia.databus.voc.{ApplicationNTriples, DataFileToModel, Format, TextTurtle}

import better.files.{File => BetterFile, ManagedResource => _, _}
import org.apache.commons.compress.archivers.{ArchiveInputStream, ArchiveStreamFactory}
import org.apache.commons.compress.compressors.CompressorStreamFactory
import org.apache.jena.rdf.model.Model
import org.eclipse.rdf4j.rio.Rio
import resource._

import scala.io.{Codec, Source}
import scala.util.{Failure, Success, Try}

import java.io._
import java.nio.charset.MalformedInputException
import java.nio.file.Files
import java.security.PrivateKey
import java.util.Base64


/**
  * a simple dao to collect all values for a file
  * private constructor, must be called with init to handle compression detection
  */
class Datafile private(datafile: File) {

  var mimetype: Format = _
  var formatExtension: String = ""

  var sha256sum: String = ""
  var bytes: Long = _

  // compression option
  var isArchive: Boolean = false
  var isCompressed: Boolean = false
  var compressionVariant: String = "None"

  var signatureBytes: Array[Byte] = _
  var signatureBase64: String = ""
  var verified: Boolean = false

  var preview: String = ""

  def getName(): String = {
    datafile.getName
  }


  def toModel(props: Properties): Model = {
    DataFileToModel.datafile2Model(this, datafile, props)
  }

  private def updateMimetype(): Datafile = {
    val (ext, mt) = Format.detectMimetypeByFileExtension(datafile)
    mimetype = mt

    // downgrade turtle to ntriple, if linebased
    if (mt == TextTurtle) {
      val baos: ByteArrayInputStream = new ByteArrayInputStream(preview.getBytes);
      val (a, b, c, d) = LineBasedRioDebugParser.parse(baos, Rio.createParser(ApplicationNTriples.rio))
      if (d.size == 0) {
        mimetype = ApplicationNTriples
      }
    }


    formatExtension = ext
    this
  }

  def updateSHA256sum(): Datafile = {
    sha256sum = signing.sha256Hash(datafile.toScala).asBytes.map("%02x" format _).mkString
    this
  }

  def updateBytes(): Datafile = {
    bytes = datafile.length()
    this
  }

  /**
    *
    * @param lineCount gives the linecount of the preview, however it is limited to 500 chars per line, in case there is a very long line
    * @return
    */
  private def updatePreview(lineCount: Int): Datafile = {

    val unshortenedPreview = for {

        inputStream <- getInputStream
        source <- managed(Source.fromInputStream(inputStream)(Codec.UTF8))

      } yield {
        Try(source.getLines().take(lineCount).mkString("\n"))
    }

    def maxLength = lineCount * 500

    preview = unshortenedPreview apply {

      case Success(tooLong) if tooLong.size > maxLength => tooLong.substring(0, maxLength)

      case Success(shortEnough) => shortEnough

      case Failure(mie: MalformedInputException) => "[binary data - no preview]"
    }

    this
  }

  def updateSignature(keyPair: RSAKeyPair): Datafile = {

    signatureBytes = signing.signFile(keyPair.privateKey, datafile.toScala)

    signatureBase64 = new String(Base64.getEncoder.encode(signatureBytes))

    verified = signing.verifyFile(keyPair.publicKey, signatureBytes, datafile.toScala)
    this
  }

  /**
    * Opens the file with compression, etc.
    * NOTE: if file is an archive, we assume it is only one file in it and this will be on the stream
    *
    * @return
    */
  def getInputStream(): ManagedResource[InputStream] = managed({

    val bis = datafile.toScala.newInputStream.buffered
    if (isCompressed) {
      new CompressorStreamFactory()
        .createCompressorInputStream(compressionVariant, bis)
    } else if (isArchive) {
      val ais: ArchiveInputStream = new ArchiveStreamFactory()
        .createArchiveInputStream(compressionVariant, bis)
      ais.getNextEntry
      ais
    } else {
      bis
    }
  })


  override def toString =
    s"""
       |Datafile(sha256sum=$sha256sum
       |bytes=$bytes
       |isArchive=$isArchive
       |isCompressed=$isCompressed
       |compressionVariant=$compressionVariant
       |signatureBytes=${signatureBytes.map("%02X" format _).mkString}
       |signatureBase64=$signatureBase64
       |verified=$verified)
     """.stripMargin
}

object Datafile {
  /**
    * factory method
    * * checks whether the file exists
    * * detects compression for further reading
    *
    * @param datafile
    * @return
    */
  def init(datafile: File): Datafile = {

    if (!Files.exists(datafile.toPath)) {
      throw new FileNotFoundException("File not found: " + datafile)
    }
    var df: Datafile = new Datafile(datafile)

    // detect compression
    var comp = Compression.detectCompression(datafile)
    var arch = Compression.detectArchive(datafile)

    comp match {
      case "None" => {
        df.isCompressed = false
        df.compressionVariant = "None"
      }
      case _ => {
        df.isCompressed = true
        df.compressionVariant = comp
      }
    }

    arch match {
      // note that if compression is also none\nvalue has already been assigned above
      case "None" => {
        df.isArchive = false
      }
      // overrides compression
      case _ => {
        df.isArchive = true
        df.compressionVariant = arch
      }
    }

    // we need a preview for mimetype
    df.updatePreview(10)

    //detect mimetype
    df.updateMimetype()

    df
  }
}
