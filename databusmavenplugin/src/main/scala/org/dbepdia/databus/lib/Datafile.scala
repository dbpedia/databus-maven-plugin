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

package org.dbepdia.databus.lib

import java.io._
import java.nio.file.Files
import java.security.PrivateKey
import java.util.Base64

import org.apache.commons.compress.archivers.{ArchiveInputStream, ArchiveStreamFactory}
import org.apache.commons.compress.compressors.CompressorStreamFactory

/**
  * a simple dao to collect all values for a file
  * private constructor, must be called with init to handle compression detection
  */
class Datafile private(datafile: File) {

  var md5: String = Hash.computeHash(datafile)
  val bytes = datafile.length()

  // compression option
  var isArchive: Boolean = false
  var isCompressed: Boolean = false
  var compressionVariant: String = "None"

  var signatureBytes: Array[Byte] = _
  var signatureBase64: String = ""
  var verified: Boolean = false


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

  override def toString = s"\nDatafile(\nmd5=$md5\n bytes=$bytes\n isArchive=$isArchive\n isCompressed=$isCompressed\n compressionVariant=$compressionVariant\n signatureBytes=$signatureBytes\n signatureBase64=$signatureBase64\n verified=$verified)"
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
      // note that if compression is also none, value has already been assigned above
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
