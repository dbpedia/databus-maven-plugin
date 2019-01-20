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

import org.dbpedia.databus.shared.authentification.{AccountHelpers, RSAModulusAndExponent}
import com.typesafe.scalalogging.LazyLogging
import org.apache.jena.rdf.model.ModelFactory
import org.apache.maven.plugin.{AbstractMojo, MojoExecutionException}
import org.apache.maven.plugins.annotations.{Execute, LifecyclePhase, Mojo, Parameter}
import org.dbpedia.databus.lib.{ Datafile, FilenameHelpers, SigningHelpers}

import scala.collection.mutable
import org.apache.maven.settings.Settings


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
  val allVersions: Boolean = false

  @Parameter(property = "databus.detailedValidation", required = false)
  val detailedValidation: Boolean = false

  @Parameter(property = "databus.strict", required = false)
  val strict: Boolean = false

  /**
    * TODO potential caveat: check if, else based on pom could fail
    *
    *
    */
  @throws[MojoExecutionException]
  override def execute(): Unit = {

    //System.exit(0)

    /**
      * validation
      */

    // parent module, i.e. packaging pom
    if (isParent()) {
      validateWebId()
      validateAccount()

    } else {

      validateVersions()
    }
  }

  def strict(reason: String): Unit = {
    if (strict) {
      getLog.error(s"[strict==true] failing reason: ${reason}")
      System.exit(-1)
    } else {
      getLog.warn(reason)
    }
  }


  /**
    *
    */
  def validateWebId(): Unit = {

    getLog.debug("PKCS12 bundle location: " + locations.pkcs12File.pathAsString)

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

  def validateAccount() = {
    getLog.info("Checking for registered DBpedia account")
    AccountHelpers.getAccountOption(publisher) match {
      case Some(account) => {
        getLog.info(s"SUCCESS: DBpedia Account found: ${account.getURI}")
      }
      case None => {
        strict(s"DBpedia account for $publisher not found at https://github.com/dbpedia/accounts " +
          s", some features might be deactivated")
      }
    }
  }

  /**
    * validate one or several versions
    * NOTE: UGLY CODE AHEAD
    */
  def validateVersions(): Unit = {

    val dataInputDirectoryParent = dataInputDirectory.getParentFile

    val versions: mutable.SortedSet[String] = mutable.SortedSet(dataInputDirectory.toString.replace(dataInputDirectoryParent.toString, ""))

    // add allVersions to the set
    if (allVersions) {
      versions.++=(dataInputDirectoryParent.listFiles().filter(_.isDirectory).map(f => {
        f.toString.replace(dataInputDirectoryParent.toString, "")
      }).toSet)
      getLog.info(s"[databus.allVersion=true] found ${versions.size} version(s): ${versions.mkString(", ")}")
    }

    //val versions: mutable.SortedSet[String] = mutable.SortedSet(dataInputDirectory.toString.replace(dataInputDirectoryParent.toString, ""))

    // collect all information

    val versionDirs = versions.toList.flatMap(v => {
      val versionDir: File = new File(dataInputDirectoryParent, v)
      if (versionDir.exists && versionDir.isDirectory) {

        val wrongFiles = versionDir.listFiles.filterNot(_.getName.startsWith(artifactId)).toList
        if (wrongFiles.nonEmpty) {
          strict(s" ${wrongFiles.mkString(s" not starting with $artifactId\n")}")
        }

        var fileList: List[File] = versionDir.listFiles
          .filter(_.isFile)
          .filter(_.getName.startsWith(artifactId))
          .filter(_ != getDataIdFile())
          .filter(_ != getParseLogFile())
          .toList

        val filenameHelpers: List[FilenameHelpers] = fileList.map(f => {
          new FilenameHelpers(f)(getLog)
        })

        val datafiles: List[Datafile] = fileList.map(f => {

          val df = Datafile(f)(getLog).ensureExists()
          if (detailedValidation) {
            df.updateFileMetrics()
          }
          df
        })
        Some((v, versionDir, fileList, filenameHelpers, datafiles))
      } else {
        strict(s"empty directory: ${versionDir}")
        None
      }
    })

    // now validation starts

    getLog.info("Number of files:")
    for ((v, dir, fileList: List[File], fileNames, datafiles) <- versionDirs) {
      getLog.info(s"${v} with ${fileList.size} files")
    }

    getLog.info("Compression:")
    for ((v, dir, fileList: List[File], fileNames, datafiles) <- versionDirs) {
      val compfilenames: mutable.SortedSet[String] = mutable.SortedSet()
      fileNames.foreach(f => {
        f.compressionVariantExtensions.foreach(a => {
          compfilenames.add(a)
        })
      })
      val compFile: mutable.SortedSet[String] = mutable.SortedSet()
      datafiles.foreach(f => {
        compFile.add(f.compressionVariant.toString)
      })
      getLog.info(s"${v} from file ending: {${compfilenames.mkString(", ")}}, from file {${compFile.mkString(", ")}}")
    }

    getLog.info("Format:")
    for ((v, dir, fileList: List[File], fileNames, datafiles) <- versionDirs) {
      val formfilenames: mutable.SortedSet[String] = mutable.SortedSet()
      fileNames.foreach(f => {
        f.formatVariantExtensions.foreach(a => {
          formfilenames.add(a)
        })
      })
      val formFile: mutable.SortedSet[String] = mutable.SortedSet()
      datafiles.foreach(f => {
        formFile.add(f.format.mimeType)
      })
      getLog.info(s"${v} from file name: {${formfilenames.mkString(", ")}}, from file {${formFile.mkString(", ")}}")
    }

    getLog.info("ContentVariant:")
    for ((v, dir, fileList: List[File], fileNames, datafiles) <- versionDirs) {
      val contfilenames: mutable.SortedSet[String] = mutable.SortedSet()
      fileNames.foreach(f => {
        f.contentVariantExtensions.foreach(a => {
          contfilenames.add(a)
        })
      })
      getLog.info(s"${v} from file name: {${contfilenames.mkString(", ")}}")
    }

    getLog.info("prefix:")
    for ((v, dir, fileList: List[File], fileNames, datafiles) <- versionDirs) {
      val contfilenames: mutable.SortedSet[String] = mutable.SortedSet()
      fileNames.foreach(f => {
        contfilenames.add(f.filePrefix)
      })
      getLog.info(s"${v} from name: {${contfilenames.mkString(", ")}}")
    }

    if (detailedValidation) {

      getLog.info("Sorted (LC_COLLATE=C):")
      for ((v, dir, fileList: List[File], fileNames, datafiles) <- versionDirs) {
        val contfilenames: mutable.SortedSet[String] = mutable.SortedSet()
        var sorted = 0
        var unsorted = 0
        datafiles.foreach(df => {
          if (df.sorted) {
            sorted += 1
          } else {
            unsorted += 1
            contfilenames.add(df.file.getName)
          }
        })
        getLog.info(s"${v} sorted: ${sorted}, not sorted: ${unsorted} {${contfilenames.mkString(", ").replaceAll(artifactId, "")}}")
      }

      getLog.info("Duplicates:")
      for ((v, dir, fileList: List[File], fileNames, datafiles) <- versionDirs) {
        var duplicates = 0
        val contfilenames: mutable.SortedSet[String] = mutable.SortedSet()

        datafiles.foreach(df => {
          if (df.duplicates > 0) {
            duplicates += df.duplicates
            contfilenames.add(df.file.getName)

          }
        })
        getLog.info(s"${v} duplicates: ${duplicates} in {${contfilenames.mkString(", ").replaceAll(artifactId, "")}}")
      }

      getLog.info("Empty files:")
      for ((v, dir, fileList: List[File], fileNames, datafiles) <- versionDirs) {
        val contfilenames: mutable.SortedSet[String] = mutable.SortedSet()
        datafiles.foreach(df => {
          if (df.nonEmptyLines == 0) {
            contfilenames.add(df.file.getName)
          }
        })
        getLog.info(s"${v} has ${contfilenames.size} empty files:  {${contfilenames.mkString(", ").replaceAll(artifactId, "")}} ")
      }

    }


  }


}
