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
import org.dbpedia.databus.lib.{AccountHelpers, Datafile, FilenameHelpers, SigningHelpers}

import scala.collection.mutable


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

  @Parameter(property = "databus.detailedValidation", required = false)
  val detailedValidation: Boolean = true

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

      val dataInputDirectoryParent = dataInputDirectory.getParentFile

      val versions: mutable.SortedSet[String] = mutable.SortedSet(dataInputDirectory.toString.replace(dataInputDirectoryParent.toString, ""))

      // add allVersions to the set
      if (allVersions) {
        versions.++=(dataInputDirectoryParent.listFiles().filter(_.isDirectory).map(f => {
          f.toString.replace(dataInputDirectoryParent.toString, "")
        }).toSet)
        getLog.info(s"[databus.allVersion=true] found ${versions.size} version(s):\n${versions.mkString(", ")}")
      }
      versions.foreach(v => {
        val versionDir = new File(dataInputDirectoryParent, v)
        if (versionDir.exists && versionDir.isDirectory) {
          //check startswith
          val filesNotInFormat = versionDir.listFiles.filterNot(_.getName.startsWith(artifactId)).toList
          if (filesNotInFormat.size > 0) {
            //log
          }

          var headlineBasic = "comp\tcontent\tformat\tprefix\tname"
          var contentBasic = ""

          var headLineDetails = "nonEmpty\tduplicates\tsorted\tsize\tname"
          var contentDetails = ""

          val dataFiles = listDataFiles(versionDir)
          dataFiles.foreach(f => {
            val fileName = new FilenameHelpers(f)(getLog)
            contentBasic +=
              s"${fileName.compressionVariantExtensions.mkString(", ")} \t" +
                s"${fileName.contentVariantExtensions.mkString(", ")} \t" +
                s"${fileName.formatVariantExtensions.mkString(", ")} \t" +
                s"${fileName.filePrefix} \t" +
                s"${f.getName}\n"


            if (detailedValidation == true) {
              val df: Datafile = Datafile(f)(getLog).ensureExists()
              df.updateFileMetrics()
              contentDetails += s"" +
                s"${df.nonEmptyLines}\t" +
                s"${df.duplicates}\t" +
                s"${df.sorted}\t" +
                s"${df.uncompressedByteSize}\t" +
                s"${f.getName}\n"
            }
          })

          var tableBasic = s"${headlineBasic}\n${contentBasic}"
          var tableDetails = s"${headLineDetails}\n${contentDetails}"
          getLog.info(s"Version $v has ${dataFiles.size} files total\n$tableBasic\n$tableDetails")
        }
      })
    }
  }


  def listDataFiles(versionDir: File): List[File] = {
    val dataFiles = versionDir.listFiles
      .filter(_.isFile)
      .filter(_.getName.startsWith(artifactId))
      .filter(_ != getDataIdFile())
      .filter(_ != getParseLogFile())
      .toList
    dataFiles

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
