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
import java.net.{URI, URL}
import java.nio.file.{Path, Paths}

import org.apache.maven.plugins.annotations.Parameter
import org.dbpedia.databus.ipfs.{IpfsCliClient, IpfsClientOps}
import org.dbpedia.databus.ipfs.IpfsCliClient.{Chunker, IpfsClientConf}

import scala.util.{Failure, Success, Try}


/**
 * Convenience class with ipfs operations embeddable in mojo
 */
trait IpfsPluginOps {
  this: DatabusMojo =>

  @Parameter(property = "ipfsSettings", readonly = true)
  val ipfsSettings: IpfsConfig = null

  lazy val chunker: Chunker = IpfsCliClient.Rabin(
    ipfsSettings.chunkerMinBlockSize,
    ipfsSettings.chunkerAvgBlockSize,
    ipfsSettings.chunkerMaxBlockSize
  )

  /**
   * returns true if the plugin was configured to save files in ipfs
   */
  def saveToIpfs: Boolean = ipfsSettings != null

  private def endpointAddress(host: String, port: Int): String = s"/ip4/$host/tcp/$port"

  private def dirHash: String = processDirectory(filesDir, true).last

  private[databus] var cliClient: Option[IpfsClientOps] = None

  private lazy val projectFilesDir = locations.inputVersionDirectory.toJava.toPath

  private lazy val relativePath: Path =
    Paths.get(session.getExecutionRootDirectory)
      .relativize(projectFilesDir)

  private lazy val filesDir: Path = Option(ipfsSettings)
    .flatMap(s => Option(s.projectRootDockerPath))
    .map(_.toPath)
    .map(_.resolve(relativePath))
    .getOrElse(projectFilesDir)

  private[databus] def processDirectory(path: Path, onlyHash: Boolean) = {
    // todo this may be improved in case not enough performance
    synchronized {
      if (cliClient.isEmpty) {
        val conf = IpfsClientConf(
          ipfsSettings.isInDocker,
          ipfsSettings.containerName,
          endpointAddress(ipfsSettings.nodeHost, ipfsSettings.nodePort)
        )
        val c = IpfsCliClient(conf)
        cliClient = Some(c)
      }
    }
    cliClient.get.add(path, chunker, recursive = true, onlyHash = onlyHash)
  }

  /**
   * This method is needed for testing.
   * There is no good way to inject the client, have to do it with setter.
   */
  def setIpfsClient(client: IpfsClientOps): Unit =
    cliClient = Some(client)

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

  /**
   * Calculate ipfs hash for the file and return a link to download it from ipfs.
   */
  def downloadLink(file: File): URI = {
    ipfsSettings
      .ipfsEndpointLink
      .toURI
      .resolve(dirHash + "/")
      .resolve(
        projectFilesDir
          .getParent
          .relativize(file.toPath)
          .toString
      )
  }

}


/**
 * Configuration properties for working with ipfs.
 *
 * @param isInDocker            Specifies if the ipfs cli is in docker.
 * @param containerName         Name of the ipfs docker container. (optional)
 * @param ipfsEndpointLink      Link to an ipfs http endpoint for file downloads.
 * @param projectRootDockerPath Optional parameter, specify path in the docker container to which current project root is mounted.
 * @param nodeHost              host of the IPFS node
 * @param nodePort              port of the IPFS node
 * @param chunkerMinBlockSize   min block size of rabin chunker of IPFS
 * @param chunkerMaxBlockSize   max block size of rabin chunker of IPFS
 * @param chunkerAvgBlockSize   avg block size of rabin chunker of IPFS
 */
case class IpfsConfig(isInDocker: Boolean,
                      containerName: String,
                      ipfsEndpointLink: URL,
                      projectRootDockerPath: File,
                      nodeHost: String,
                      nodePort: Int,
                      chunkerMinBlockSize: Int,
                      chunkerAvgBlockSize: Int,
                      chunkerMaxBlockSize: Int) {

  /**
   * Needed for injection.
   */
  def this() = this(
    false,
    "ipfs_host",
    URI.create("https://ipfs.io/ipfs/").toURL,
    null,
    "127.0.0.1",
    5001,
    IpfsConfig.DefaultChunkSize / 2,
    IpfsConfig.DefaultChunkSize,
    IpfsConfig.DefaultChunkSize * 2
  )

}

object IpfsConfig {
  private val DefaultChunkSize = 262144
}