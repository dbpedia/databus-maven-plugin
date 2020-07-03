/*-
 * #%L
 * DBpedia Databus Maven Plugin
 * %%
 * Copyright (C) 2018 - 2020 Sebastian Hellmann (on behalf of the DBpedia Association)
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
import java.net.URI
import java.nio.file.Path

import org.dbpedia.databus.ipfs.IpfsCliClient
import org.dbpedia.databus.ipfs.IpfsCliClient.DefaultRabin

import scala.util.{Failure, Success, Try}


trait IpfsPluginOps {
  this: DatabusMojo =>

  // todo make configurable
  private val defaultIpfsLocation = URI.create("https://ipfs.io/ipfs/")

  lazy val saveToIpfs = Option(ipfsSettings).isDefined

  private lazy val dirHash = processDirectory(filesDir, true).last

  private lazy val cliClient = IpfsCliClient(ipfsSettings)

  private lazy val projectFilesDir = locations.inputVersionDirectory.toJava.toPath

  private lazy val relativePath: Path =
    globalProjectRoot.toPath
      .relativize(projectFilesDir)

  private lazy val filesDir: Path = Option(ipfsSettings)
    .flatMap(s => Option(s.projectRootDockerPath))
    .map(_.toPath)
    .map(_.resolve(relativePath))
    .getOrElse(projectFilesDir)

  private def processDirectory(path: Path, onlyHash: Boolean) =
    cliClient.add(path, DefaultRabin, recursive = true, onlyHash=onlyHash)

  /**
   * @return true if successfully saved, false otherwise
   */
  def shareToIpfs(): Boolean = {
    val input = filesDir
    getLog.info(s"Adding directory $input to ipfs")
    Try(processDirectory(input, false)) match {
      case Failure(exception) =>
        getLog.error(s"Failed to add files from $input to ipfs", exception)
        false
      case Success(value) =>
        getLog.info(s"Successfully added files from $input: ${value.last}")
        true
    }
  }

  def downloadLink(file: File): URI = {
    defaultIpfsLocation
      .resolve(dirHash + "/")
      .resolve(
        projectFilesDir
        .getParent
        .relativize(file.toPath)
        .toString
      )
  }

}
