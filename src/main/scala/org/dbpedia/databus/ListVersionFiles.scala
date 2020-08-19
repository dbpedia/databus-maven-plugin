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
package org.dbpedia.databus

import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugins.annotations.Mojo


/**
  *
  * * lists files
  *
  *
  */
@Mojo(name = "ls", requiresOnline = true, threadSafe = true)
class ListVersionFiles extends DatabusMojo with Operations {

  val separator = ", "

  @throws[MojoExecutionException]
  override def execute(): Unit = {
    if (isParent()) {
      skipmodules.skipmodules = true
      modules.forEach(m => {
        //todo .map(_.relativize(File("")))
        getLog.info(
          s"""$m/$version (${listFiles(m).size} files)
             |${listFiles(m).mkString(separator)}
           """.stripMargin)

      })
    } else {
      if (skipmodules.skipmodules) {
        return
      }
      getLog.info(
        s"""$artifactId/$version  (${listFiles().size} files)
           |${listFiles().mkString(separator)}
           """.stripMargin)
    }

  }

}

