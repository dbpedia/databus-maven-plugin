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


import better.files.File
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugins.annotations.Mojo

/**
 * Delete current version
 *
 * * lists files
 * * asks for approval, no -f option
 * * deletes
 *
 */
@Mojo(name = "rm", requiresOnline = true, threadSafe = true)
class RemoveVersion extends DatabusMojo with Operations {

  @throws[MojoExecutionException]
  override def execute(): Unit = {

    getLog.info("the following version folders will be deleted:")

    if (isParent()) {
      skipmodules.skipmodules = true
      modules.forEach(m => {
        getLog.info(
          s"""${listFiles(m).size} files in ${File(s"$m/$version").toJava.getAbsoluteFile}""".stripMargin)
      })

      getLog.info("proceed? [y/N]")
      val c: String = scala.io.StdIn.readLine()
      if (c.trim.equalsIgnoreCase("y")) {
        getLog.info("deleting")
        modules.forEach(m => {
          //DELETE
          val vdir = File(s"$m/$version")
          vdir.delete(true)
          getLog.info(s"${!vdir.isDirectory} $vdir")
        })
      } else {
        println(s"aborted, read '$c'")
      }
    } else {
      if (!skipmodules.skipmodules) {
        getLog.info(
          s"""##########
             |databus:rm works only on group to delete current version of all artifacts, use:
             |rm -r $artifactId/$version"
           """.stripMargin)
      }
    }
  }

}

