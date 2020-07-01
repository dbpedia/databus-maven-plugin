/*-
 * #%L
 * DBpedia Databus Maven Plugin
 * %%
 * Copyright (C) 2018 - 2019 Sebastian Hellmann (on behalf of the DBpedia Association)
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
package org.dbpedia.databus

import java.util

import better.files.File
import com.typesafe.scalalogging.LazyLogging
import org.apache.maven.plugins.annotations.Parameter

object skipmodules {
  var skipmodules = false
}

trait Operations extends LazyLogging {
  this: DatabusMojo =>

  @Parameter(
    property = "modules",
    defaultValue = "${project.modules}"
  )
  val modules: util.ArrayList[String] = new util.ArrayList[String]

  def listFiles(): List[File] = {
    val dir = File(s"$version")
    if (dir.isDirectory) {
      dir.list.toList
    } else {
      List[File]()
    }
  }

  def listFiles(artifact: String): List[File] = {
    val dir = File(s"$artifact/$version")
    if (dir.isDirectory) {
      dir.list.toList
    } else {
      List[File]()
    }

  }

}
