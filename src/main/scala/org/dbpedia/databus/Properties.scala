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

import better.files.{File => _, _}
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.Parameter

import java.io.File
import java.net.URL
import java.time.{Instant, LocalDateTime, ZoneId}


/**
  * Collection of all properties
  *
  * Dev Note:
  * val targetDirectory = new File (mavenTargetDirectory,"/databus/"+artifactId+"/"+version)
  * or scripting does not work as these are executed on startup, the injection of values
  * by maven is done later, so all vars are empty on startup
  *
  */
trait Properties extends Locations with Parameters {

  this: AbstractMojo =>

  /**
    * Project vars given by Maven
    */

  @Parameter(defaultValue = "${project.artifactId}", readonly = true)
  val artifactId: String = null

  @Parameter(defaultValue = "${project.version}", readonly = true)
  val version: String = null

  @Parameter(defaultValue = "${project.packaging}", readonly = true)
  val packaging: String = null

  @Parameter(defaultValue = "${project.build.directory}", readonly = true)
  val mavenTargetDirectory: File = null

  @Parameter(defaultValue = "${project.build.finalName}", readonly = true)
  val finalName: String = null

  // not usable, needs to set explicitly in the pom.xml of modules to be queried
  // @Parameter(defaultValue = "${parent.relativePath}", readonly = true)
  // val relPath: String = ""

  /**
    * SH: I marked this one as deprecated as it does not seem to work correctly
    * reproduce with running mvn help:evaluate -Dexpression=maven.multiModuleProjectDirectory in parent and module dir
    * I tried to implement an isParent method below to use centrally
    * At the moment, we are working with the assumption that we only have one parent with modules, no deeper
    */
  @deprecated(message = "see above", since = "early days")
  @Parameter(defaultValue = "${maven.multiModuleProjectDirectory}", readonly = true)
  val multiModuleBaseDirectory: String = ""


  /**
    * directories as documented in the archetype
    * Note that these are also created in Validate
    */

  @Parameter val dataInputDirectory: File = null
  @Parameter val packageDirectory: File = null

  @Parameter val dataDependencyDirectory: File = null

  @Parameter val pluginDirectory: File = null
  @Parameter val includeParseLogs: Boolean = true

  @Parameter val bundle: String = null
  @Parameter val downloadUrlPath: URL = null
  @Parameter val allowOverwriteOnDeploy: Boolean = true
  @Parameter val deployToTestRepo: Boolean = false
  @Parameter val feedFrom: String = null


  /**
    * Plugin specific vars defined in parent module
    */

  //TODO the absolute path here is different for parent and modules the function
  // read privatekeyfiles in hash and signs searches in the parent folder using ../
  // works for now, but could fail
  @Parameter val pkcs12File: File = null

  @Parameter val maintainer: URL = null
  @Parameter val publisher: URL = null
  @Parameter val license: String = null
  @Parameter val downloadURL: String = null
  @Parameter val issuedDate: String = null
  @Parameter val modifiedDate: String = null


  /**
    * for each modules
    */

  @Parameter val labels: java.util.List[String] = new java.util.ArrayList[String]
  @Parameter val datasetDescription: String = ""

  val invocationTime = LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault())

  def isParent(): Boolean = {
    packaging.equals("pom")
  }

  def getDataIdFile(): File = dataIdFile.toJava

  def dataIdFile = getDataIdDirectory.toScala / s"${finalName}_dataid.ttl"

  def dataIdPackageTarget = packageSubdirectory / dataIdFile.name

  def dataIdDownloadLocation = downloadUrlPath.toString + getDataIdFile.getName

  def getParseLogFile(): File = {
    new File(getParselogDirectory, "/" + finalName + "_parselog.ttl")
  }

  def getFeedFile(): File = {
    new File(getFeedDirectory, "/" + finalName + "_feed.xml")
  }

  def getDataIdDirectory: File = {
    create(new File(pluginDirectory, "/dataid"))
  }

  def getParselogDirectory: File = {
    create(new File(pluginDirectory, "/parselog"))
  }

  def getFeedDirectory: File = {
    create(new File(pluginDirectory, "/feed"))
  }

  /**
    * below are functions for the package-export phase
    */

  def getPackageDirectory: File = packageSubdirectory.toJava

  def packageSubdirectory = (packageDirectory.toScala / artifactId / version).createDirectories()

  def getDatafileFinal(datafile: File): File = {
    new File(datafile.getParent, datafile.getName.replace(artifactId, finalName))
  }

  def getDatafilePackageTarget(datafile: File): File = {
    new File(getPackageDirectory, getDatafileFinal(datafile).getName)
  }

  private def create(dir: File): File = {
    if(!dir.exists()) {
      dir.mkdirs()
    }
    dir
  }


  /**
    * lists all appropriate data files, using these filters:
    * * is a file
    * * starts with artifactid
    * * is not a dataid
    * * is not a parselog
    *
    * @return
    */
  def getListOfDataFiles(): List[File] = {

    getLog.debug("")


   if(dataInputDirectory.exists && dataInputDirectory.isDirectory) {

     val dataFiles = dataInputDirectory.listFiles
        .filter(_.isFile)
        .filter(_.getName.startsWith(artifactId))
        .filter(_ != getDataIdFile())
        .filter(_ != getParseLogFile())
        .toList

      if(dataFiles.isEmpty) {
        getLog.warn(s"no matching in put files found within ${dataInputDirectory.listFiles().size} files in " +
          s"data input directory ${dataInputDirectory.getAbsolutePath}")
      }

      dataFiles
   } else {

     getLog.warn(s"data input location '${dataInputDirectory.getAbsolutePath}' is does not exist or is not " +
       "a directory!")

     List[File]()
    }
  }
}
