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
package org.dbepdia.databus

import org.apache.maven.plugin.{AbstractMojo, MojoExecutionException}
import org.apache.maven.plugins.annotations.{LifecyclePhase, Mojo, Parameter}
import org.dbepdia.databus.lib.{Datafile, FileHelper}

@Mojo(name = "debug", defaultPhase = LifecyclePhase.VALIDATE)
class DebugLineBasedFormats extends AbstractMojo {

  @Parameter(defaultValue = "${project.packaging}", readonly = true)
  private val packaging: String = ""

  @Parameter(defaultValue = "${maven.multiModuleProjectDirectory}", readonly = true)
  private val multiModuleBaseDirectory: String = ""

  @Parameter val resourceDirectory: String = ""


  @throws[MojoExecutionException]
  override def execute(): Unit = {
    //skip the parent module
    if (packaging.equals("pom")) {
      getLog.info("skipping parent module")
      return
    }

    val moduleDirectories = FileHelper.getModules(multiModuleBaseDirectory)

    // processing each module
    moduleDirectories.foreach(moduleDir => {
      getLog.info(s"reading from module $moduleDir")

      // processing all file per module
      FileHelper.getListOfFiles(s"$moduleDir/$resourceDirectory").foreach(datafile => {
        getLog.info(s"found file $datafile")
        val df: Datafile = Datafile.init(datafile)
        //TODO debug file with parser, if ntriples

      })
    })

  }
}
