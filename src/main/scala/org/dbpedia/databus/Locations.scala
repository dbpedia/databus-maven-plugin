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


import java.net.{MalformedURLException, URL}
import java.nio.file.NoSuchFileException

import better.files.File
import better.files._

import scala.collection.mutable

trait Locations {

  this: Properties =>

  lazy val locations = new Locations(this)

  class Locations(props: Properties) {

    val dataIdFileName = "dataid.ttl"
    val parselogFileName = "parselog.ttl"
    val pomFileName = "pom.xml"
    val provenanceFileName = "provenance.tsv"
    val markdownFileName = s"${props.artifactId}.md"
    val markdownVersionFileName = s"${props.artifactId}-${props.version}.md"

    def getLog = props.getLog

    //used for better paths in logging
    def prettyPath(f: File): String = {
      props.sessionRoot.toScala.relativize(f).toString
    }

    /**
      * INPUT
      */

    // main input directory
    lazy val inputDirectory: File = props.inputDirectory.toScala

    // version input directory
    lazy val inputVersionDirectory: File = (props.inputDirectory.toScala / props.version)

    // provenance tsv for each version
    lazy val inputProvenanceFile: File = (inputDirectory / provenanceFileName)

    // docu and changelog
    lazy val inputMarkdownFile: File = (inputDirectory / markdownFileName)

    lazy val inputPomFile: File = (inputDirectory / pomFileName)

    //lazy val inputVersionMarkdown: File = (inputDirectory / markdownVersionFileName)

    /**
      * BUILD
      */

    // target/databus
    lazy val buildDirectory: File = (props.buildDirectory.toScala / "databus").createDirectories()

    //
    lazy val buildVersionDirectory: File = (buildDirectory / props.version)

    lazy val buildVersionShaSumDirectory: File = (buildDirectory / props.version / "shasum").createDirectories()

    lazy val buildDataIdFile: File = (buildVersionDirectory / dataIdFileName)

    lazy val buildParselogFile: File = (buildVersionDirectory / parselogFileName)

    lazy val datasetIdNoSlash: String = s"${publisher.toString.replace("#this", "")}/${groupId}/${artifactId}/${version}"


    /**
      * repo/package
      */
    lazy val packageDirectory: File = (props.packageDirectory.toScala)

    lazy val packageDocumentationFile: File = (packageDirectory / markdownFileName)

    lazy val packageVersionDirectory: File = (packageDirectory / version).createDirectories()

    lazy val packageDataIdFile: File = (packageVersionDirectory / dataIdFileName)

    lazy val packageParselogFile: File = (packageVersionDirectory / parselogFileName)

    lazy val dataIdDownloadLocation: String = downloadUrlPath.toString + dataIdFileName

    lazy val packageProvenanceFile: File = (packageDirectory / provenanceFileName)

    lazy val packagePomFile: File = (packageDirectory / pomFileName)


    /**
      * lists all appropriate data files, using these filters:
      * * is a file
      * * starts with artifactid
      * * is not a dataid
      * * is not a parselog
      *
      * @return
      */
    lazy val inputFileList: List[File] = {

      if (inputVersionDirectory.isDirectory && inputVersionDirectory.nonEmpty) {

        //todo parselog
        val nonArtifactFiles: List[File] = inputVersionDirectory.list
          .filter(!_.name.startsWith(artifactId))
          .filter(_.name != dataIdFileName)
          .filter(_.name != parselogFileName)
          .toList

        if (nonArtifactFiles.nonEmpty) {
          getLog.warn(s"The following files not starting with artifactId are found in version dir ${props.version}:" +
            s" ${nonArtifactFiles.mkString(",")}")
        }

        val files = inputVersionDirectory.list
          .filter(_.isRegularFile)
          .filter(_.name.startsWith(artifactId))
          .toList

        files
      } else {
        getLog.error(s"Problem with databus.inputVersionDirectory\n " +
          s"Folder: ${inputVersionDirectory}" +
          s"isDirectory: ${inputVersionDirectory.isDirectory}\n " +
          s"isEmpty (no files found): ${inputVersionDirectory.isEmpty}\n"
        )

        List[File]()
      }
    }


    lazy val provenanceFull: Set[(String, URL)] = {

      val set: mutable.Set[(String, URL)] = mutable.Set()

      try {
        inputProvenanceFile.lineIterator
          .filter(_.nonEmpty)
          .map(line => line.split("\t"))
          .foreach(arr => {
            val url = new URL(arr(1))
            set.+=((arr(0), url))
          })
      } catch {
        case nsf: NoSuchFileException => {
          getLog.info(s"${artifactId}/provenance.tsv not found, skipping")
          set
        }
        case aie: ArrayIndexOutOfBoundsException => {
          getLog.error(s"parsing of ${artifactId}/${prettyPath(inputProvenanceFile)} failed\nfix with:\n* must be (version \\t url), ${aie}")
          System.exit(-1)
        }
        case mue: MalformedURLException => {
          getLog.error(s"parsing of ${artifactId}/${prettyPath(inputProvenanceFile)} failed\nfix with:\n* must be (version \\t url), ${mue}")
          System.exit(-1)
        }

      }

      set.toSet
    }

    def provenanceIRIs = {
      provenanceForVersion(props.version)
    }

    def provenanceForVersion(version: String): Set[URL] = {

      provenanceFull.filter(_._1 == version).map(t => t._2)

    }


    lazy val pkcs12File: File = {
      if (props.pkcs12File != null) {
        lib.findFileMaybeInParent(props.pkcs12File.toScala, "PKCS12 bundle")
      } else if (props.settings.getServer(pkcs12serverId) != null) {
        //TODO strictly not necessary to use find here
        lib.findFileMaybeInParent(File(settings.getServer(pkcs12serverId).getPrivateKey), "PKCS bundle")
      } else {
        null
      }
    }

    def pkcs12Password: String = {

      if (props.settings.getServer(pkcs12serverId) != null && settings.getServer(pkcs12serverId).getPassphrase !=null ) {
          settings.getServer(pkcs12serverId).getPassphrase
      } else {
        //empty
        ""
      }
    }
  }

}
