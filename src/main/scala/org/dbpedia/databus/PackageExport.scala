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
import org.apache.maven.plugin.{AbstractMojo, MojoExecutionException}
import org.apache.maven.plugins.annotations.{LifecyclePhase, Mojo, Parameter}
import java.io.{File, FileWriter}

@Mojo(name = "package-export", defaultPhase = LifecyclePhase.PACKAGE)
class PackageExport extends AbstractMojo with Properties {

  /**
    */
  @Parameter(property = "databus.package.includeParseLogs", defaultValue = "false")
  val includeParseLogs: Boolean = false

  @throws[MojoExecutionException]
  override def execute(): Unit = {
    //skip the parent module
    if (isParent()) {
      getLog.info(s"skipping parent ${artifactId}")
      return
    }

    if (locations.inputFileList.isEmpty) {
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

    getLog.info(s"packaging from ${locations.prettyPath(locations.inputVersionDirectory)}")
    getLog.info(s"packaging to ${locations.prettyPath(locations.packageVersionDirectory)}")

    // for each module copy all files to target
    locations.inputFileList.foreach { inputFile =>

      val df = Datafile(inputFile.toJava)(getLog)
      val filePackageTarget = locations.packageVersionDirectory / df.finalBasename(params.versionToInsert)

      // check if files exist already
      if (filePackageTarget.isRegularFile) {

        //overwrite if different, else keep
        if (!sameFile(inputFile, filePackageTarget)) {
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

    //todo
    //Parselogs
    if (includeParseLogs && locations.buildParselogFile.isRegularFile) {
      locations.buildParselogFile.copyTo(locations.packageParselogFile, true)
      getLog.info("packaged from build: " + locations.buildParselogFile.name)
    }


    if (locations.provenanceFull.nonEmpty && locations.packageProvenanceFile.isRegularFile) {
      if (!sameFile(locations.inputProvenanceFile, locations.packageProvenanceFile)) {
        locations.inputProvenanceFile.copyTo(locations.packageProvenanceFile, true)
        getLog.info("packaged (in overwrite mode): " + locations.packageProvenanceFile.name)
      }
    } else {
      locations.inputProvenanceFile.copyTo(locations.packageProvenanceFile, true)
    }


    if (locations.packageDocumentationFile.isRegularFile) {
      if (!sameFile(locations.inputMarkdownFile, locations.packageDocumentationFile)) {
        locations.packageDocumentationFile.writeByteArray((params.description + "\n\n" + documentation.trim.).getBytes())
      }
    } else {
      locations.packageDocumentationFile.writeByteArray((params.description + "\n\n" + documentation.trim.).getBytes())
    }


    if (keepRelativeURIs) {
      //copy
      locations.buildDataIdFile.copyTo(locations.packageDataIdFile, overwrite = true)
    } else {
      // resolve
      val baseResolvedDataId = resolveBaseForRDFFile(locations.buildDataIdFile, locations.dataIdDownloadLocation)
      locations.packageDataIdFile.writeByteArray((Properties.logo + "\n").getBytes())
      locations.packageDataIdFile.appendByteArray(baseResolvedDataId)
    }
    getLog.info(s"packaged (in overwrite mode): ${locations.prettyPath(locations.packageDataIdFile)}")
    getLog.info(s"package written to ${locations.prettyPath(locations.packageVersionDirectory)}")
  }

  def sameFile(inputFile: better.files.File, filePackageTarget: better.files.File): Boolean = {
    val targetHash = signing.sha256Hash(filePackageTarget)
    val sourceHash = signing.sha256Hash(inputFile)
    targetHash == sourceHash
  }
}
