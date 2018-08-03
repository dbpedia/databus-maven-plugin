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

import java.io.File
import java.net.URL

import org.apache.maven.plugins.annotations.Parameter


/**
  * Collection of all properties
  *
  * Dev Note:
  * val targetDirectory = new File (mavenTargetDirectory,"/databus/"+artifactId+"/"+version)
  * or scripting does not work as these are executed on startup, the injection of values
  * by maven is done later, so all vars are empty on startup
  *
  */
trait Properties {

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
    * Project vars
    */

  @Parameter(defaultValue = "${project.artifactId}", readonly = true)
  val artifactId: String = ""

  @Parameter(defaultValue = "${project.version}", readonly = true)
  val version: String = ""

  @Parameter(defaultValue = "${project.packaging}", readonly = true)
  val packaging: String = ""

  @Parameter(defaultValue = "${project.build.directory}", readonly = true)
  var mavenTargetDirectory: File = _




  // refers to target/classes
  @Parameter(defaultValue = "${project.build.outputDirectory}", readonly = true)
  val outputDirectory: String = ""

  // not usable, needs to set explicitly in the pom.xml of modules to be queried
  // @Parameter(defaultValue = "${parent.relativePath}", readonly = true)
  // val relPath: String = ""


  /**
    * Plugin specific vars for parent module
    */

  /**
    * the envisioned target dir
    * folder is created in validate
    */
  @Parameter var targetDirectory: File = _
  @Parameter var maintainer: URL = _
  @Parameter var publisher: URL = _

  //TODO the absolutepath here is different for parent and modules the function
  // read privatekeyfiles in hash and signs searches in the parent folder using ../
  // works for now, but could fail
  @Parameter var privateKeyFile: File = _
  @Parameter var dataDirectory: File = _


  /**
    * for the modules
    */
  @Parameter val labels: java.util.List[String] = new java.util.ArrayList[String]
  @Parameter val datasetDescription: String = ""


  @Parameter val dataset: String = ""
  @Parameter val license: String = ""
  @Parameter val latestVersion: String = ""
  @Parameter val downloadURL: String = ""
  @Parameter val issuedDate: String = ""
  @Parameter val modifiedDate: String = ""

  /**
    * Other
     */



  /**
    *
    * @return
    */

  def isParent(): Boolean = {
    packaging.equals("pom")
  }

}
