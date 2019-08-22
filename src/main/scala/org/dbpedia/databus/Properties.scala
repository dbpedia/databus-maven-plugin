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

import org.apache.maven.plugin.{AbstractMojo, Mojo}
import org.apache.maven.plugins.annotations.Parameter

import java.io.File
import java.net.URL
import org.apache.maven.plugin.logging.Log
import org.apache.maven.settings.Settings


/**
  * Collection of all properties
  *
  * Dev Note:
  * val targetDirectory = new File (mavenTargetDirectory,"/databus/"+artifactId+"/"+version)
  * or scripting does not work as these are executed on startup, the injection of values
  * by maven is done later, so all vars are empty on startup
  *
  */
trait Properties extends Locations with Parameters with Mojo {

  this: AbstractMojo =>


  /** ***********************************
    * CODE THAT WILL BE EXECUTED BEFORE RUNNING EACH MOJO
    * ************************************/
  {
    Properties.printLogoOnce(getLog)
  }


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
  val buildDirectory: File = null

  @Parameter(defaultValue = "${project.build.finalName}", readonly = true)
  val finalName: String = null

  @Parameter(defaultValue = "${settings}", readonly = true)
  val settings: Settings = null

  @Parameter(defaultValue = "${session.executionRootDirectory}", readonly = true)
  val sessionRoot: File = null


  /**
    * Project internal parameters
    */

  /**
    * Input folder for data, defaultValue "src/main/databus/${project.version}"
    * Each artifact (abstract dataset identity) consists of several versions of the same dataset
    * These versions are kept in all in parallel subfolders
    * Tipp: src/main is the maven default, if you dislike having three folders you can also use "databus/${project.version}"
    */
  // done, good defaults
  @Parameter(property = "databus.inputDirectory", defaultValue = ".", required = true)
  val inputDirectory: File = null

  @Parameter(property = "databus.insertVersion") val insertVersion: Boolean = true


  /**
    * properties that need to be configured by the user
    *
    */


  /**
    * if true, parameters downloadUrlPath is ignored and also there is dataid.ttl Uris will stay like <>, i.e. relative
     */
  @Parameter(property = "databus.keepRelativeURIs", defaultValue = "false")
  val keepRelativeURIs = false

  /**
    * Sets the base URI for the dataid and also dcat:downloadURL
    * DataID URIs will be rewritten from local <> to <$databus.downloadUrlPath/dataid.ttl>
    *
    * Configure downloadUrlPath, where the dataset will be deployed:
    * DBpedia will publish its groups with dozens of artifacts and new versions at regular intervals,
    * our downloadurl looks like this:
    * <databus.downloadUrlPath>http://downloads.dbpedia.org/repo/${project.groupId}/${project.artifactId}/${project.version}/</databus.downloadUrlPath>
    * We recommend to do the same, as you can add more bundles and datasets later.
    */
  @Parameter(property = "databus.downloadUrlPath", required = true)
  val downloadUrlPath: URL = null

  /**
    * There are cases where it is impossible to keep the Dataid file in the same URLPath as the data files
    * An example is the DBpedia Ontology pushed to Github, where you first need to push the files
    * and then generate dcat:downloadUrl based on the last commit hash
    * TODO SH: please review the docu above, it is not helpful in understanding what this param should be and what it does (consequences)
    */
  @Parameter(property = "databus.absoluteDCATDownloadUrlPath", required = false)
  val absoluteDCATDownloadUrlPath: String = null

  /**
    * Options:
    * * aggregation ${session.executionRootDirectory}/target/databus/package/${project.groupId}/${project.artifactId}
    * * apache /var/www/www.example.org/repo/${project.groupId}/${project.artifactId}
    * * dbpedia example /media/bigone/25TB/www/downloads.dbpedia.org/repo/lts/${project.groupId}/$(project.artifactId)
    * * local as repo ./
    * *
    * DEFAULT ${session.executionRootDirectory}/target/databus/package
    * all files are copied into this directory relative to where mvn databus:package-export is run
    */
  @Parameter(property = "databus.packageDirectory", defaultValue = "${session.executionRootDirectory}/target/databus/repo/${project.groupId}", required = true)
  val packageDirectory: File = null


  /**
    * File ending on `.pfx` or `.p12`
    * Background information: https://github.com/dbpedia/webid#webid
    * The PKCS12 bundle providing the cryptographic identity information associated
    * to the WebID of the agent operating the databus plugin. This bundle should combine
    * the .X509 certificate and the private RSA key for the corresponding WebID.
    * We recommend putting the file in ~/.m2 next to the settings.xml (Maven user dir):
    * <databus.pkcs12File>${user.home}/.m2/webid_bundle.p12</databus.pkcs12File>
    *
    * SECURITY NOTICE:
    * Protect your private key file, do not loose it, do not send it over (unencrypted) network
    * Limit access to this file to your own user: chmod 700 $HOME/.m2/webid_bundle.p12
    * The data channel you are about to create requires it to republish new versions there.
    */
  @Parameter(property = "databus.pkcs12File", required = false)
  val pkcs12File: File = null


  @Parameter(property = "databus.pkcs12serverId", defaultValue = "databus.defaultkey", required = false)
  val pkcs12serverId: String = ""


  /**
    * refers to the WebID that does the publication on the web and on the databus
    * This one is matched against the private key file (next option)
    * Please read on https://github.com/dbpedia/webid how to create such file
    * We include a dev-dummy webid file here, please don't use it for publishing, use your own
    */
  @Parameter(property = "databus.publisher", required = true)
  val publisher: URL = null

  /**
    * The maintainer of the data release, normally the person to contact, often the same as publisher
    */
  @Parameter(property = "databus.maintainer")
  val maintainer: URL = null

  /**
    * Pick one from here: http://rdflicense.linkeddata.es/
    */
  @Parameter(property = "databus.license", required = true) val license: String = null

  /**
    * URL to the main issue tracker for reporting data errors
    */
  @Parameter(property = "databus.issueTracker", required = false) val issueTracker: URL = null

  /**
    * URL to the main feedback channel for questions and comments (e.g. "+1" )
    */
  @Parameter(property = "databus.feedbackChannel", required = false) val feedbackChannel: URL = null

  /**
    * URL to a concrete piece of code, e.g. on github
    * https://github.com/dbpedia/extraction-framework/blob/master/core/src/main/scala/org/dbpedia/extraction/mappings/LabelExtractor.scala
    */
  @Parameter(property = "databus.codeReference", required = false) val codeReference: URL = null

  /**
    * URL to the place the pom.xml and ${artifactId}.md are managed, e.g. DBpedia Labels on GitHub
    * https://github.com/dbpedia/databus-maven-plugin/blob/master/dbpedia/${groupId}/${artifactId}
    */
  @Parameter(property = "databus.documentationLocation", required = false) val documentationLocation: URL = null


  /**
    * default today
    */
  @Parameter(property = "databus.issuedDate") val issuedDate: String = null

  @Parameter(property = "databus.tryVersionAsIssuedDate", defaultValue = "false") val tryVersionAsIssuedDate: Boolean = false

  /**
    * default from file, else today
    */
  @Parameter(property = "databus.modifiedDate") val modifiedDate: String = null


  //documentation
  @Parameter(property = "databus.documentation", defaultValue = "")
  val documentation: String = ""


  /**
    * common variables used in the code
    */


  def isParent(): Boolean = {
    packaging.equals("pom")
  }


}

/**
  * Static property object, which contains all static code
  */
object Properties {

  val pluginVersion = "1.3-SNAPSHOT"

  var logoPrinted = false

  //NOTE: NEEDS TO BE COMPATIBLE WITH TURTLE COMMENTS
  val logo =
    s"""|
        |
        |######
        |#     #   ##   #####   ##   #####  #    #  ####
        |#     #  #  #    #    #  #  #    # #    # #
        |#     # #    #   #   #    # #####  #    #  ####
        |#     # ######   #   ###### #    # #    #      #
        |#     # #    #   #   #    # #    # #    # #    #
        |######  #    #   #   #    # #####   ####   ####
        |
        |# Plugin version ${pluginVersion} - https://github.com/dbpedia/databus-maven-plugin
        |
        |""".stripMargin

  def printLogoOnce(mavenlog: Log) = {
    if (!logoPrinted) {
      mavenlog.info(logo)
    }
    logoPrinted = true
  }
}
