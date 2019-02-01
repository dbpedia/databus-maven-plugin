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

import org.dbpedia.databus.lib._
import org.dbpedia.databus.shared.signing
import better.files._
import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.apache.maven.plugin.{AbstractMojo, MojoExecutionException}
import org.apache.maven.plugins.annotations.{LifecyclePhase, Mojo, Parameter}
import java.io.{File, FileWriter}
import java.nio.file.Files
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

@Mojo(name = "package-export", defaultPhase = LifecyclePhase.PACKAGE)
class PackageExport extends AbstractMojo with Properties {

  /**
    * The parselogs are written to ${databus.pluginDirectory}/parselogs and then packaged with the data
    * We keep the parselogs in a separate file, because they can be quite large (repeating the triples that have errors)
    */
  @Parameter(property = "databus.includeParseLogs", defaultValue = "true")
  val includeParseLogs: Boolean = true

  @Parameter(property = "databus.deactivateDownloadUrl", defaultValue = "false")
  val deactivateDownloadUrl = false

  @throws[MojoExecutionException]
  override def execute(): Unit = {
    //skip the parent module
    if (isParent()) {
      getLog.info(s"skipping parent ${artifactId}")
      return
    }


    if(locations.inputFileList.isEmpty){
      getLog.warn(s"${locations.prettyPath(locations.inputVersionDirectory)} is empty, skipping")
      return
    }

    if (!locations.buildDataIdFile.isRegularFile) {

      //val emptyVersion = if (locations.inputFileList.isEmpty) s"* ${version} does not contain any files\n" else ""

      getLog.warn(s"${locations.prettyPath(locations.buildDataIdFile)} not found for ${artifactId}/${version}, can not package\n" +
        s"fix with:\n" +
        s"* running mvn prepare-package or mvn databus:metadata first\n")
      System.exit(-1)
    }

    // for each module copy all files to target
    locations.inputFileList.foreach { inputFile =>

      val df = Datafile(inputFile.toJava)(getLog)
      val filePackageTarget = locations.packageVersionDirectory / df.finalBasename(params.versionToInsert)
      getLog.info(s"packaging from ${locations.prettyPath(locations.inputVersionDirectory)}")
      getLog.info(s"packaging to ${locations.prettyPath(locations.packageVersionDirectory)}")

      // check if files exist already
      if (filePackageTarget.isRegularFile) {

        val targetHash = signing.sha256Hash(filePackageTarget)
        val sourceHash = signing.sha256Hash(inputFile)

        //overwrite if different, else keep
        if (targetHash != sourceHash) {
          inputFile.copyTo(filePackageTarget, overwrite = true)
          getLog.info("packaged (in overwrite mode): " + filePackageTarget.name)
        } else {
          getLog.info("skipped (same file content): " + filePackageTarget.name)
        }

      } else {
        inputFile.copyTo(filePackageTarget, overwrite = true)
        getLog.info("packaged: " + filePackageTarget.name)
      }
    }

    //Parselogs
    if (includeParseLogs && locations.buildParselogFile.isRegularFile) {
      locations.buildParselogFile.copyTo(locations.packageParselogFile, true)
      getLog.info("packaged from build: " + locations.buildParselogFile.name)
    }

    if (deactivateDownloadUrl) {
      //copy
      locations.buildDataIdFile.copyTo(locations.packageDataIdFile, overwrite = true)

    } else {
      // resolve
      val baseResolvedDataId = resolveBaseForRDFFile(locations.buildDataIdFile, locations.dataIdDownloadLocation)
      locations.packageDataIdFile.writeByteArray((Properties.logo + "\n").getBytes())
      locations.packageDataIdFile.appendByteArray(baseResolvedDataId)
    }
    getLog.info("packaged from build: " + locations.packageDataIdFile.name)
    getLog.info(s"package written to ${locations.prettyPath(locations.packageVersionDirectory)}")
  }
}
