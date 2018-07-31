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

import java.io.{BufferedInputStream, File, FileInputStream, InputStream}

import org.apache.commons.compress.archivers.{ArchiveEntry, ArchiveException, ArchiveInputStream, ArchiveStreamFactory}
import org.apache.commons.compress.compressors.{CompressorException, CompressorInputStream, CompressorStreamFactory}

object Compression {

  //TODO streams need to be closed properly
  def detectCompression(datafile: File): String = {
    try {
      val fi = new BufferedInputStream(new FileInputStream(datafile))
      CompressorStreamFactory.detect(fi)
    } catch {
      case ce: CompressorException => "None"
    }

  }

  def detectArchive(datafile: File): String = {
    try {
      val fi = new BufferedInputStream(new FileInputStream(datafile))
      ArchiveStreamFactory.detect(fi)
    } catch {
      case ce: ArchiveException => "None"
    }

  }

  /*
  def readFirstFileFromArchive ( in : ArchiveInputStream, out:InputStream) : Unit = {

    in.getNextEntry

    val out: OutputStream = Files.newOutputStream(dir.toPath.resolve(entry.getName))
    IOUtils.copy(in, out)
    out.close()
  }

  import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
val is: InputStream = Files.newInputStream(input.toPath)
 val in: ArchiveInputStream = new ArchiveStreamFactory().createArchiveInputStream(ArchiveStreamFactory.ZIP, is)
 val entry: ZipArchiveEntry = in.getNextEntry.asInstanceOf[ZipArchiveEntry]
 val out: OutputStream = Files.newOutputStream(dir.toPath.resolve(entry.getName))
 IOUtils.copy(in, out)
 out.close()
   */

}
