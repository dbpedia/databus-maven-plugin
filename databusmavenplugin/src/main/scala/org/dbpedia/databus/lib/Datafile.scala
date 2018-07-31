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

import java.io._
import java.nio.file.Files
import java.security.PrivateKey
import java.util.Base64

import org.apache.commons.compress.archivers.{ArchiveInputStream, ArchiveStreamFactory}
import org.apache.commons.compress.compressors.CompressorStreamFactory
import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.apache.jena.vocabulary.RDF

import scala.io.Source

/**
  * a simple dao to collect all values for a file
  * private constructor, must be called with init to handle compression detection
  */
class Datafile private(datafile: File) {

  val mimetypes = Map(
    "ttl" -> "text/turtle",
    "tql" -> "application/n-quads",
    "nq" -> "application/n-quads",
    "nt" -> "text/ntriples",
    "rdf" -> "application/rdf+xml"
  )

  var mimetype = "UNKNOWN"
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

  def toModel(): Model = {
    var model: Model = ModelFactory.createDefaultModel
    val thisResource = model.createResource(datafile.getName)
    /*
    <http://dbpedia.org/dataset/article_categories?lang=en&dbpv=2016-10&file=article_categories_en.tql.bz2>
        a                            dataid:SingleFile ;
        rdfs:label                   "Article Categories"@en , "core-i18n/en/article_categories_en.tql.bz2" ;
        dataid:associatedAgent       <http://wiki.dbpedia.org/dbpedia-association> ;
        dataid:checksum              <http://dbpedia.org/dataset/article_categories?lang=en&dbpv=2016-10&file=article_categories_en.tql.bz2&checksum=md5> ;
        dataid:isDistributionOf      <http://dbpedia.org/dataset/article_categories?lang=en&dbpv=2016-10> ;
        dataid:latestVersion         <http://dbpedia.org/dataset/article_categories?lang=en&dbpv=2016-10&file=article_categories_en.tql.bz2> ;
        dataid:preview               <http://downloads.dbpedia.org/preview.php?file=2016-10_sl_core-i18n_sl_en_sl_article_categories_en.tql.bz2> ;
        dataid:uncompressedByteSize  6558796473 ;
        dc:conformsTo                <http://dataid.dbpedia.org/ns/core> ;
        dc:description               "Links from concepts to categories using the SKOS vocabulary."@en ;
        dc:hasVersion                <http://downloads.dbpedia.org/2016-10/core-i18n/en/2016-10_dataid_en.ttl?version=1.0.0> ;
        dc:issued                    "2017-07-01"^^xsd:date ;
        dc:license                   <http://purl.oclc.org/NET/rdflicense/cc-by-sa3.0> ;
        dc:modified                  "2017-07-06"^^xsd:date ;
        dc:publisher                 <http://wiki.dbpedia.org/dbpedia-association> ;
        dc:title                     "Article Categories"@en ;
        dcat:byteSize                396463888 ;
        dcat:downloadURL             <http://downloads.dbpedia.org/2016-10/core-i18n/en/article_categories_en.tql.bz2> ;
        dcat:mediaType               dataid-mt:MediaType_n-quads_x-bzip2 .
     */


    thisResource.addProperty(RDF.`type`,"SingleFile")
    model
  }

  def updateMimetype(): Datafile = {
    mimetypes.foreach { case (key, value) => {
      if (datafile.getName.contains(key)) {
        mimetype = value
      }
    }
    }
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

  def updatePreview(lineCount: Int): Datafile = {

    val source = Source.fromInputStream(getInputStream())
    var x = 0
    val sb = new StringBuilder
    val it = source.getLines()
    it.size
    while (it.hasNext && x <= lineCount) {
      sb.append(it.next()).append("\n")
      x += 1
    }
    preview = sb.toString()
    source.close
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
  def getInputStream(): InputStream = {
    val fi = new BufferedInputStream(new FileInputStream(datafile))
    if (isCompressed) {
      new CompressorStreamFactory()
        .createCompressorInputStream(compressionVariant, fi)
    } else if (isArchive) {
      val ais: ArchiveInputStream = new ArchiveStreamFactory()
        .createArchiveInputStream(compressionVariant, fi)
      ais.getNextEntry
      ais
    } else {
      fi
    }


  }


  override def toString

  = s"Datafile(md5=$md5\nbytes=$bytes\nisArchive=$isArchive\nisCompressed=$isCompressed\ncompressionVariant=$compressionVariant\nsignatureBytes=$signatureBytes\nsignatureBase64=$signatureBase64\nverified=$verified\npreview=$preview)"
}

object Datafile {
  /**
    * factory method
    * * checks wether the file exists
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

    //detect mimetype
    df.updateMimetype()

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


    df
  }
}
