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

import org.dbpedia.databus.shared.signing

import better.files._
import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.apache.maven.plugin.{AbstractMojo, MojoExecutionException}
import org.apache.maven.plugins.annotations.{LifecyclePhase, Mojo}

import java.io.{File, FileWriter}
import java.nio.file.Files
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

@Mojo(name = "package-export", defaultPhase = LifecyclePhase.PACKAGE)
class PackageExport extends AbstractMojo with Properties {


  @throws[MojoExecutionException]
  override def execute(): Unit = {
    //skip the parent module
    if (isParent()) {
      getLog.info("skipping parent module")
      return
    }

    // for each module copy all files to target
    getListOfDataFiles().foreach(datafile => {

      if (getDatafilePackageTarget(datafile).exists()) {

        val targetHash = signing.sha256Hash(getDatafilePackageTarget(datafile).toScala)

        val sourceHash = signing.sha256Hash(datafile.toScala)

        if (targetHash == sourceHash) {
          Files.copy(datafile.getAbsoluteFile.toPath, getDatafilePackageTarget(datafile).toPath, REPLACE_EXISTING)
          getLog.info("packaged: " + getDatafilePackageTarget(datafile).getName)
        } else {
          getLog.info("skipped (same file): " + getDatafilePackageTarget(datafile).getName)
        }

      } else {
        Files.copy(datafile.getAbsoluteFile.toPath, getDatafilePackageTarget(datafile).toPath, REPLACE_EXISTING)
        getLog.info("packaged: " + getDatafilePackageTarget(datafile).getName)
      }
    })

    //Parselogs
    if (includeParseLogs && getParseLogFile().exists()) {
      val ptarget = new File (getPackageDirectory,getParseLogFile().getName)
      Files.copy(getParseLogFile().toPath, ptarget.toPath, REPLACE_EXISTING)
      getLog.info("packaged: " + ptarget.getName)
    }

    // dataId files
    var dataIdCollect: Model = ModelFactory.createDefaultModel
    dataIdCollect.read(getDataIdFile().toURI.toString, downloadUrlPath.toString+getDataIdFile.getName,"turtle")
    val dataIdPackageTarget = new File(getPackageDirectory, "/" + getDataIdFile().getName)
    dataIdCollect.write(new FileWriter(dataIdPackageTarget),"turtle")
    //Files.copy(getDataIdFile().toPath, dataIdPackageTarget.toPath, REPLACE_EXISTING)
    getLog.info("packaged: " + dataIdPackageTarget.getName)


    getLog.info(s"package written to ${packageDirectory}")
  }
}
