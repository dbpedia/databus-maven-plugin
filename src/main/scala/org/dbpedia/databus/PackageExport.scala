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

import java.io.{File, FileWriter}
import java.nio.file.{CopyOption, Files}
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

import org.apache.maven.plugin.{AbstractMojo, MojoExecutionException}
import org.apache.maven.plugins.annotations.{LifecyclePhase, Mojo}

@Mojo(name = "package-export", defaultPhase = LifecyclePhase.PACKAGE)
class PackageExport extends AbstractMojo with Properties {


  @throws[MojoExecutionException]
  override def execute(): Unit = {
    //skip the parent module
    if (isParent()) {
      getLog.info("skipping parent module")
      return
    }

    // for each module

    // copy all files to target
    getListOfDataFiles().foreach(datafile => {
      val target = new File(getAndCreatePackageDirectory(), "/" + datafile.getName).toPath
      getLog.info(target+"")
      getLog.info(target+"")
      Files.copy(datafile.getAbsoluteFile.toPath, target, REPLACE_EXISTING)

    })

    //var dataIdCollect: Model = ModelFactory.createDefaultModel

    if (includeParseLogs) {
      //dataIdCollect.read(getDataIdFile().toString, "turtle","file:///"+getDataIdFile().toString)
    }
    val dataIdPackageTarget = new File(getAndCreatePackageDirectory(), "/" + getDataIdFile().getName)
    //dataIdCollect.write(new FileWriter(dataIdpackage),"turtle", "")
    Files.copy(getDataIdFile().toPath,dataIdPackageTarget.toPath, REPLACE_EXISTING)
    getLog.info(s"package written to ${packageDirectory}")
  }
}
