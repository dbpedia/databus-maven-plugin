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
package org.dbpedia.databus.ipfs

import java.nio.file.Path

import org.dbpedia.databus.ipfs.IpfsCliClient.{Chunker, DagMeta, Default}
import spray.json.DefaultJsonProtocol

import scala.sys.process._
import scala.util.Try


object IpfsCliClient {

  /**
   * Dag node meta.
   *
   * @param data  contents of the node
   * @param links list of links to the children of this node
   */
  case class DagMeta(data: String, links: Seq[DagLink])

  /**
   * Link to a child node of a DAG.
   *
   * @param cid content identifier.
   */
  case class DagLink(cid: CidMeta, name: String, SizeBytes: Long)

  case class CidMeta(v0: String)


  object CliProtocol extends DefaultJsonProtocol {

    implicit val cidMetaProtocol = jsonFormat(CidMeta, "/")
    implicit val dagLinkProtocol = jsonFormat(DagLink.apply, "Cid", "Name", "Size")
    implicit val dagMetaProtocol = jsonFormat2(DagMeta.apply)

  }

  /**
   * Configuration properties for an ipfs client.
   *
   * @param isInDocker    Specifies if the ipfs cli is in docker.
   * @param containerName Name of the ipfs docker container. (optional)
   */
  case class IpfsClientConf(isInDocker: Boolean = false,
                            containerName: String = null,
                            nodeAddress: String)

  /**
   * Generic ipfs chunker.
   *
   * Find more about the chunkers in ipfs docs.
   */
  trait Chunker {
    def cliParam: String
  }

  object Chunker {
    val paramName = "--chunker="
  }

  import Chunker._

  object Default extends Chunker {
    override def cliParam: String = ""
  }

  /**
   * Fixed size chunks chunker.
   */
  case class FixedSize(sizeBytes: Long) extends Chunker {

    override def cliParam: String = s"${paramName}size-$sizeBytes"

  }

  /**
   * Buzhash smart chunker.
   */
  case object Buzhash extends Chunker {

    override def cliParam: String = s"${paramName}buzhash"

  }

  /**
   * Rabin smart chunker.
   *
   * @param min minimal size of a chunk
   * @param avg average size of a chunk
   * @param max maximal size of a chunk
   */
  case class Rabin(min: Long, avg: Long, max: Long) extends Chunker {
    override def cliParam: String = s"${paramName}rabin-$min-$avg-$max"
  }

  case object DefaultRabin extends Chunker {
    override def cliParam: String = s"${paramName}rabin"
  }

  /**
   * Creates ipfs client from config.
   */
  def apply(config: IpfsClientConf): IpfsCliClient =
    new IpfsCliClient(ipfsCmd(config.isInDocker, Option(config.containerName)) :+ ("--api=" + config.nodeAddress))

  /**
   * Creates ipfs client.
   *
   * @param isInDocker    specifies if the ipfs node is in docker
   * @param containerName name of the ipfs container name in case it is in docker
   */
  def apply(isInDocker: Boolean = false, containerName: String = "ipfs_host"): IpfsCliClient =
    new IpfsCliClient(ipfsCmd(isInDocker, Option(containerName)))

  private def ipfsCmd(isInDocker: Boolean, containerName: Option[String]): Seq[String] = {
    val cmd = Seq(
      "ipfs"
    )
    if (isInDocker) {
      Seq(
        "docker",
        "exec",
        containerName.getOrElse("ipfs_host") // default name
      ) ++ cmd
    } else {
      cmd
    }
  }

}

/**
 * CLI client interface.
 */
trait IpfsClientOps {

  /**
   * Ipfs add, read more in docs of ipfs.
   *
   * @param fn             filename/folder name
   * @param chunker        chunker type (Default or Rabin)
   * @param nocopy         don't copy the contents of the file/folder into ipfs storage
   * @param recursive      add recursively if `fn` is a folder
   * @param wrapWithDir    wrap the file with top level directory
   * @param addHiddenFiles in case `fn` is a folder, adds hidden files
   * @param onlyHash       do not save the file in ipfs, but calcualte only its ipfs hash
   * @return list of created hashes
   */
  def add(fn: Path,
          chunker: Chunker = Default,
          nocopy: Boolean = false,
          recursive: Boolean = false,
          wrapWithDir: Boolean = true,
          addHiddenFiles: Boolean = false,
          onlyHash: Boolean = false): Seq[String]

  /**
   * Return DAG data for a hash.
   */
  def dagGet(hash: String): Seq[DagMeta]

  /**
   * List of links for an object with particular hash.
   */
  def objectLinks(hash: String): Seq[String]

}

/**
 * CLI client wrapper. Invokes ipfs methods using standard cli.
 *
 * @param ipfsCmd command to invoke ipfs, for example Seq("docker",  "exec", "ipfs_host", "ipfs").
 */
class IpfsCliClient private(ipfsCmd: Seq[String]) extends IpfsClientOps {

  import IpfsCliClient.CliProtocol._
  import spray.json._

  override def add(fn: Path,
                   chunker: Chunker = Default,
                   nocopy: Boolean = false,
                   recursive: Boolean = false,
                   wrapWithDir: Boolean = true,
                   addHiddenFiles: Boolean = false,
                   onlyHash: Boolean = false): Seq[String] = {
    val params = Seq(
      "add",
      chunker.cliParam,
      s"--wrap-with-directory=$wrapWithDir",
      s"--hidden=$addHiddenFiles",
      s"--nocopy=$nocopy",
      s"--recursive=$recursive",
      s"--only-hash=$onlyHash",
      fn.toAbsolutePath.toString,
    )
    val p = process(params)
    p.lineStream
      .map(_.split("\\s+")(1))
  }

  override def dagGet(hash: String): Seq[DagMeta] = {
    val p = process(Seq("dag", "get", hash))
    p.lineStream
      .flatMap(p => Try(p.parseJson.convertTo[DagMeta]).toOption)
  }

  override def objectLinks(hash: String): Seq[String] = {
    val p = process(Seq("object", "links", hash))
    p.lineStream
      .map(_.split("\\s+")(0))
  }

  private def process(params: Seq[String]) =
    Process(ipfsCmd ++ params)

}
