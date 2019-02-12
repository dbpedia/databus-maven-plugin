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

      // check if files exist already and is same
      if (filePackageTarget.isRegularFile && sameFile(inputFile, filePackageTarget)) {
        getLog.info("skipped (same file content): " + filePackageTarget.name)
      } else {
        inputFile.copyTo(filePackageTarget, overwrite = true)
        getLog.info("packaged (create or overwrite): " + filePackageTarget.name)
      }

    }

    //todo
    //Parselogs
    if (includeParseLogs && locations.buildParselogFile.isRegularFile) {
      locations.buildParselogFile.copyTo(locations.packageParselogFile, true)
      getLog.info("packaged from build: " + locations.buildParselogFile.name)
    }

    // provenance file
    // extra if, since prov is optional
    if (locations.inputProvenanceFile.isRegularFile && locations.provenanceFull.nonEmpty) {
      if (locations.packageProvenanceFile.isRegularFile && sameFile(locations.inputProvenanceFile, locations.packageProvenanceFile)) {
        getLog.info("skipped (same file content): " + locations.packageProvenanceFile.name)
      } else {
        locations.inputProvenanceFile.copyTo(locations.packageProvenanceFile, true)
        getLog.info("packaged (create or overwrite): " + locations.packageProvenanceFile.name)
      }
    }

    // documentation file
    val content = s"# ${params.label}\n${params.comment}\n\n${params.description}\n\n + ${documentation.trim}"
    if (locations.packageDocumentationFile.isRegularFile && sameFile(locations.inputMarkdownFile, locations.packageDocumentationFile)) {
      getLog.info("skipped (same file content): " + locations.packageDocumentationFile.name)
    } else {
      locations.packageDocumentationFile.writeByteArray(content.getBytes())
      getLog.info("packaged (create or overwrite): " + locations.packageDocumentationFile.name)
    }

    // todo copy group pom?
    // pom file
    if (locations.packagePomFile.isRegularFile && sameFile(locations.inputPomFile, locations.packagePomFile)) {
      getLog.info("skipped (same file content): " + locations.packagePomFile.name)
    } else {
      locations.inputPomFile.copyTo(locations.packagePomFile, true)
      getLog.info("packaged (create or overwrite): " + locations.packagePomFile.name)
    }

    // dataid
    if (keepRelativeURIs) {
      //copy unmodified, always overwrite
      locations.buildDataIdFile.copyTo(locations.packageDataIdFile, overwrite = true)
    } else {
      // resolve uris, always overwrite
      val baseResolvedDataId = resolveBaseForRDFFile(locations.buildDataIdFile, locations.dataIdDownloadLocation)
      locations.packageDataIdFile.writeByteArray((Properties.logo + "\n").getBytes())
      locations.packageDataIdFile.appendByteArray(baseResolvedDataId)
    }
    getLog.info(s"packaged (create or overwrite): ${locations.prettyPath(locations.packageDataIdFile)}")
    getLog.info(s"package written to ${locations.prettyPath(locations.packageVersionDirectory)}")
  }

  def sameFile(inputFile: better.files.File, filePackageTarget: better.files.File): Boolean = {
    val targetHash = signing.sha256Hash(filePackageTarget)
    val sourceHash = signing.sha256Hash(inputFile)
    targetHash == sourceHash
  }
}
