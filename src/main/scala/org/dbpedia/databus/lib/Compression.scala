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

import better.files._
import java.io.{BufferedInputStream, FileInputStream, InputStream}
import com.codahale.metrics.MetricRegistry
import org.apache.commons.compress.archivers.{ArchiveEntry, ArchiveException, ArchiveInputStream, ArchiveStreamFactory}
import org.apache.commons.compress.compressors.{CompressorException, CompressorInputStream, CompressorStreamFactory}

import scala.util.Try

object Compression {

  def detectCompression(datafile: File): Option[String] = {
    try {
      Some(datafile.inputStream.map(_.buffered).apply(CompressorStreamFactory.detect))
    } catch {
      case ce: CompressorException => None
    }
  }

  def detectArchive(datafile: File): Option[String] = {
    try {
      Some(datafile.inputStream.map(_.buffered).apply(ArchiveStreamFactory.detect))
    } catch {
      case ce: ArchiveException => None
    }
  }
}
