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
import org.dbpedia.databus.voc.{ApplicationNTriples, DataFileToModel, Format, TextTurtle}

import better.files.{File => BetterFile, ManagedResource => _, _}
import org.apache.commons.compress.archivers.{ArchiveInputStream, ArchiveStreamFactory}
import org.apache.commons.compress.compressors.CompressorStreamFactory
import org.apache.jena.rdf.model.Model
import org.eclipse.rdf4j.rio.Rio
import resource._

import scala.io.Source

import java.io._
import java.nio.file.Files
import java.security.PrivateKey
import java.util.Base64

/**
  * a simple dao to collect all values for a file
  * private constructor, must be called with init to handle compression detection
  */
class Datafile private(datafile: File) {

  lazy val betterfile = BetterFile(datafile.toPath)

  var mimetype: Format = _
  var fileExtension: String = ""

  var md5: String = ""
  var bytes: Long = _

  // compression option
  var isArchive: Boolean = false
  var isCompressed: Boolean = false
  var compressionVariant: String = "None"

  var signatureBytes: Array[Byte] = _
  var signatureBase64: String = ""
  var verified: Boolean = false

  var preview: String = ""

  def getName(): String ={
    datafile.getName
  }

  def toModel( props: Properties): Model = {
    DataFileToModel.datafile2Model(this, datafile, props)
  }

  private def updateMimetype(): Datafile = {
    val (ext, mt )= Format.detectMimetypeByFileExtension(datafile)
    mimetype = mt

    // downgrade turtle to ntriple, if linebased
    if(mt == TextTurtle){
      val baos: ByteArrayInputStream = new ByteArrayInputStream(preview.getBytes);
      val (a,b,c,d) = LineBasedRioDebugParser.parse(baos,Rio.createParser(ApplicationNTriples.rio))
      if(d.size==0){
        mimetype = ApplicationNTriples
      }
    }


    fileExtension = ext
    this
  }

  def updateMD5(): Datafile = {
    md5 = Hash.computeHash(datafile)
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
      source <- managed(Source.fromInputStream(inputStream))
    } yield {
      source.getLines().take(lineCount).mkString("\n")
    }

    def maxLength = lineCount * 500

    preview = unshortenedPreview apply {

      case tooLong if tooLong.size > maxLength => tooLong.substring(0, maxLength)

      case shortEnough => shortEnough
    }

    this
  }

  def updateSignature(privateKey: PrivateKey): Datafile = {
    signatureBytes = Sign.sign(privateKey, datafile);
    signatureBase64 = new String(Base64.getEncoder.encode(signatureBytes))
    //verify
    verified = Sign.verify(privateKey, datafile, signatureBytes)
    this
  }

  /**
    * Opens the file with compression, etc.
    * NOTE: if file is an archive, we assume it is only one file in it and this will be on the stream
    *
    * @return
    */
  def getInputStream(): ManagedResource[InputStream] = managed({

    val bis = betterfile.newInputStream.buffered
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


  override def toString

  = s"Datafile(md5=$md5\nbytes=$bytes\nisArchive=$isArchive\nisCompressed=$isCompressed\ncompressionVariant=$compressionVariant\nsignatureBytes=$signatureBytes\nsignatureBase64=$signatureBase64\nverified=$verified\n})"
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
  //todo add exception to signature
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
