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

import java.io.{DataInput, File}

import org.dbpedia.databus.shared.authentification.RSAModulusAndExponent
import com.typesafe.scalalogging.LazyLogging
import org.apache.jena.rdf.model.ModelFactory
import org.apache.maven.plugin.{AbstractMojo, MojoExecutionException}
import org.apache.maven.plugins.annotations.{LifecyclePhase, Mojo, Parameter}
import org.dbpedia.databus.lib.{AccountHelpers, FilenameHelpers, SigningHelpers}


/**
  * Validate setup and resources
  *
  * WebID
  * - dereference and download
  * - get the public key from the webid
  * - get the private key from the config, generate a public key and compare to the public key
  *
  */
@Mojo(name = "validate", defaultPhase = LifecyclePhase.VALIDATE, requiresOnline = true, threadSafe = true)
class Validate extends AbstractMojo with Properties with SigningHelpers with LazyLogging {

  @Parameter(property = "databus.allVersions", required = false)
  val allVersions: Boolean = true

  /**
    * TODO potential caveat: check if, else based on pom could fail
    *
    *
    */
  @throws[MojoExecutionException]
  override def execute(): Unit = {

    /**
      * validation
      */

    // parent module, i.e. packaging pom
    if (isParent()) {
      validateWebId()

      getLog.info("Checking for registered DBpedia account")
      AccountHelpers.getAccountOption(publisher) match {
        case Some(account) => {
          getLog.info(s"SUCCESS: DBpedia Account found: ${account.getURI}")
        }
        case None => {
          getLog.warn(s"DBpedia account for $publisher not found at https://github.com/dbpedia/accounts , some features might be deactivated")
        }
      }
    } else {

      if (allVersions) {
        val versions = getVersions(dataInputDirectory)
        getLog.info(s"found ${versions.size}\n${versions.mkString("\n")}")
      }

      versionInfo(dataInputDirectory)

    }
  }

  def getVersions(dataInputDirectory: File): List[File] = {
    dataInputDirectory.getParentFile.listFiles().filter(_.isDirectory).toList.sorted
  }

  def versionInfo(dir: File) = {

    //Version number
    // Total Files
    //
    if (dir.exists && dir.isDirectory) {

      val dataFiles = dir.listFiles
        .filter(_.isFile)
        .filter(_.getName.startsWith(artifactId))
        .filter(_ != getDataIdFile())
        .filter(_ != getParseLogFile())
        .toList

      dataFiles.foreach(f => {
        val a = new FilenameHelpers(f)(getLog)
        getLog.info(a.filePrefix)
        getLog.info(a.compressionVariantExtensions.toString())
        getLog.info(a.contentVariantExtensions.toString())
        getLog.info(a.formatVariantExtensions.toString())
      }

      )

    }

  }



  def getListOfInputFiles(dir: File): List[File] = {

    if (dir.exists && dir.isDirectory) {

      val dataFiles = dir.listFiles
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


  /**
    *
    */
  def validateWebId(): Unit = {

    getLog.debug("PKCS12 bundle location: " + locations.pkcs12File.pathAsString)


    if (!pkcs12password.isEmpty) {
      SigningHelpers.pkcs12PasswordMemo.update(locations.pkcs12File.toJava.getCanonicalPath, pkcs12password)
    }

    def keyPair = singleKeyPairFromPKCS12

    val modulusExponentFromPKCS12 =
      RSAModulusAndExponent(keyPair.privateKey.getModulus, keyPair.publicKey.getPublicExponent)

    /**
      * Read the webid
      */

    val webIdModel = ModelFactory.createDefaultModel
    webIdModel.read(publisher.toString)
    getLog.debug("Read publisher webid: " + webIdModel.size() + " triples from " + publisher)

    val matchingKeyInWebId = modulusExponentFromPKCS12.matchAgainstWebId(webIdModel, publisher.toString, Some(getLog))

    if (matchingKeyInWebId.isDefined) {
      getLog.info("SUCCESS: Private Key validated against WebID")
    } else {
      getLog.error("FAILURE: Private Key and WebID do not match")
      System.exit(-1)
    }
  }

}
