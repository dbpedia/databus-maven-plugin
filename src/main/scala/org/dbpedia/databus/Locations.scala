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


import better.files.File
import better.files._

trait Locations {

  this: Properties =>

  lazy val locations = new Locations(this)

  class Locations(props: Properties) {

    def getLog = props.getLog

    // input
    val versionInputDirectory: File = (props.dataInputDirectory.toScala / props.version)


    // build
    lazy val databusBuildDirectory : File = (buildDirectory.toScala / "databus"  )


    lazy val prepareDataIdFile: File = (buildDirectory.toScala / "dataid.ttl")

    lazy val parselogDirectory: File = (buildDirectory.toScala / "parselogs").createDirectories()


    // repo/package
    lazy val packageVersionDirectory: File = (packageDirectory.toScala / artifactId / version).createDirectories()

    lazy val packageDataIdFile: File = (packageVersionDirectory / "dataid.ttl")

    lazy val dataIdDownloadLocation: String = downloadUrlPath.toString + "dataid.ttl"

    lazy val parseLogFile: File = (parselogDirectory / (props.finalName + "_parselog.ttl"))


    /**
      * lists all appropriate data files, using these filters:
      * * is a file
      * * starts with artifactid
      * * is not a dataid
      * * is not a parselog
      *
      * @return
      */
    def listInputFiles(): List[File] = {

      if (versionInputDirectory.exists && versionInputDirectory.isDirectory) {

        val dataFiles: List[File] = versionInputDirectory.toJava.listFiles()
          .filter(_.isFile)
          .filter(_.getName.startsWith(artifactId))
          .filter(_ != prepareDataIdFile.name)
          .map(f => f.toScala)
          .toList
        //TODO .filter(_ != getParseLogFile())

        if (dataFiles.isEmpty) {
          getLog.warn(s"no matching input files found within ${versionInputDirectory.toJava.listFiles().size} files in " +
            s"data input directory ${versionInputDirectory.pathAsString}")
        }

        dataFiles
      } else {

        getLog.warn(s"data input location '${props.versionDirectory.getAbsolutePath}' does not exist or is not a directory!")

        List[File]()
      }
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

      if (props.settings.getServer(pkcs12serverId) != null) {
        settings.getServer(pkcs12serverId).getPassphrase
      } else {
        ""
      }
    }
  }

}
