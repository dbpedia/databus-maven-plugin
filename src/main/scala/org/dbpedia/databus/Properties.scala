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
import java.util
import java.util.{List => JavaList}


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

  @Parameter(defaultValue = "${project.groupId}", readonly = true)
  val groupId: String = null

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


  /**
    * Project internal parameters
    */

  /**
    * pluginDirectory Default: ${project.build.directory}/databus
    * all the generated files will be written here, i.e. parselogs, dataids, feeds
    * the path is relative to the module, i.e. target/databus/
    * `mvn clean` will delete all `target` folders in your project
    */
  @Parameter(property = "databus.pluginDirectory", defaultValue = "${project.build.directory}/databus", required = true)
  val pluginDirectory: File = null


  /**
    * SH: I marked this one as deprecated as it does not seem to work correctly
    * reproduce with running mvn help:evaluate -Dexpression=maven.multiModuleProjectDirectory in parent and module dir
    * I tried to implement an isParent method below to use centrally
    * At the moment, we are working with the assumption that we only have one parent with modules, no deeper
    */
  //@deprecated(message = "see above", since = "early days")
  //@Parameter(defaultValue = "${maven.multiModuleProjectDirectory}", readonly = true)
  //val multiModuleBaseDirectory: String = ""

  /**
    * The parselogs are written to ${databus.pluginDirectory}/parselogs and then packaged with the data
    * We keep the parselogs in a separate file, because they can be quite large (repeating the triples that have errors)
    */
  @Parameter(property = "databus.includeParseLogs", defaultValue = "true")
  val includeParseLogs: Boolean = true

  /**
    * Used to deploy to test repo
    */
  @Parameter(property = "databus.deployToTestRepo", defaultValue = "false")
  val deployToTestRepo: Boolean = false

  @Parameter val allowOverwriteOnDeploy: Boolean = true
  @Parameter(property = "databus.insertVersion") val insertVersion: Boolean = true



  /**
    * properties that need to be configured by the user
    *
    */

    /**
    * input folder for data
    * copy/move all your datafiles in the respective modules
    * all files have to start with the animals of the module, i.e. src/main/databus/input/$artifactId_en.nt.bz2
    */
  @Parameter(property = "databus.dataInputDirectory", defaultValue = "src/main/databus/${project.version}", required = true)
  val dataInputDirectory: File = null

  //TODO maybe we need this later when groupid is used for the software, but now it is too complicated
  //@Parameter val alternateGroupId: String = null

  /**
    * Configure downloadUrlPath, where the dataset will be deployed:
    * DBpedia will publish 4 more bundles with dozens of artifacts and new versions at regular intervals,
    * our downloadurl looks like this:
    * <databus.downloadUrlPath>http://downloads.dbpedia.org/repo/${databus.bundle}/${project.artifactId}/${project.version}/</databus.downloadUrlPath>
    * We recommend to do the same, as you can add more bundles and datasets later.
    */
  @Parameter(property = "databus.downloadUrlPath",  required = true)
  val downloadUrlPath: URL = null

  /**
    * DEFAULT ${session.executionRootDirectory}/target/databus/package
    * all files are copied into this directory where mvn databus:package-export is run
    */
  @Parameter(property = "databus.packageDirectory", defaultValue = "${session.executionRootDirectory}/target/databus/package/${project.groupId}", required = true)
  val packageDirectory: File = null


  /**
    * TODO the absolute path here is different for parent and modules the function
    * read privatekeyfiles in hash and signs searches in the parent folder using ../
    * works for now, but could fail
    *  pkcs12File
    *  The PKCS12 bundle providing the cryptographic identity information associated to the WebID of the agent operating the databus plugin. This bundle should combine the X509 certificate and the private RSA key for the corresponding WebID.
    *  We recommend putting the file in ~/.m2 next to the settings.xml (Maven user dir):
    *  <databus.pkcs12File>${user.home}/.m2/webid_bundle.p12</databus.pkcs12File>
    *  SECURITY NOTICE:
    * Protect your private key file, do not loose it, do not send it over (unencrypted) network
    * Limit access to this file to your own user: chmod 700 $HOME/.m2/webid_bundle.p12
    * <!--<databus.pkcs12File>${user.home}/.m2/webid_bundle.p12</databus.pkcs12File>-->
    */
  @Parameter(property = "databus.pkcs12File",  required = true)
  val pkcs12File: File = null

  /**
    * refers to the WebID that does the publication on the web and on the databus
    * This one is matched against the private key file (next option)
    * Please read on https://github.com/dbpedia/webid how to create such file
    * We include a dev-dummy webid file here, please don't use it for publishing, use your own
    */
  @Parameter(property = "databus.publisher",  required = true)
  val publisher: URL = null

  /**
    * todo seems to be broken
    * The maintainer of the data release, normally the person to contact, often the same as publisher
    */
  @Parameter(property = "databus.maintainer")
  val maintainer: URL = null

  /**
    * Pick one from here: http://rdflicense.linkeddata.es/
    */
  @Parameter(property = "databus.license",  required = true) val license: String = null

  /**
    * default today
     */
  @Parameter(property = "databus.issuedDate") val issuedDate: String = null
  /**
    * default from file, else today
    */
  @Parameter(property = "databus.modifiedDate") val modifiedDate: String = null


  /**
    * for each modules
    */
  @Parameter val labels: JavaList[String] = new util.ArrayList[String]()
  @Parameter val datasetDescription: String = ""
  @Parameter val wasDerivedFrom: JavaList[BaseEntity] = new util.ArrayList[BaseEntity]()


  /**
    * common variables used in the code
    */

  val invocationTime = LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault())

  def isParent(): Boolean = {
    packaging.equals("pom")
  }

  def getDataIdFile(): File = dataIdFile.toJava

  def dataIdFile = getDataIdDirectory.toScala / s"${finalName}_dataid.ttl"

  def dataIdPackageTarget = locations.packageTargetDirectory / dataIdFile.name

  def dataIdDownloadLocation = downloadUrlPath.toString + getDataIdFile.getName

  def getParseLogFile(): File = {
    new File(getParselogDirectory, "/" + finalName + "_parselog.ttl")
  }

  def getDataIdDirectory: File = {
    create(new File(pluginDirectory, "/dataid"))
  }

  def getParselogDirectory: File = {
    create(new File(pluginDirectory, "/parselog"))
  }


  private def create(dir: File): File = {
    if (!dir.exists()) {
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
  def getListOfInputFiles(): List[File] = {

    getLog.debug("")


    if (dataInputDirectory.exists && dataInputDirectory.isDirectory) {

      val dataFiles = dataInputDirectory.listFiles
        .filter(_.isFile)
        .filter(_.getName.startsWith(artifactId))
        .filter(_ != getDataIdFile())
        .filter(_ != getParseLogFile())
        .toList

      if (dataFiles.isEmpty) {
        getLog.warn(s"no matching in put files found within ${dataInputDirectory.listFiles().size} files in " +
          s"data input directory ${dataInputDirectory.getAbsolutePath}")
      }

      dataFiles
    } else {

      getLog.warn(s"data input location '${dataInputDirectory.getAbsolutePath}' does not exist or is not a directory!")

      List[File]()
    }
  }
}
