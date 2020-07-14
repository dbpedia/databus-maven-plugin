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

import java.nio.file.{Path, Paths}

import org.dbpedia.databus.ipfs.IpfsCliClient.{Chunker, DagMeta, Default, Rabin}
import spray.json.DefaultJsonProtocol

import scala.sys.process._
import scala.util.Try


object IpfsCliClient {

  case class DagMeta(data: String, links: Seq[DagLink])

  case class DagLink(cid: CidMeta, name: String, SizeBytes: Long)

  case class CidMeta(v0: String)


  object CliProtocol extends DefaultJsonProtocol {

    implicit val cidMetaProtocol = jsonFormat(CidMeta, "/")
    implicit val dagLinkProtocol = jsonFormat(DagLink.apply, "Cid", "Name", "Size")
    implicit val dagMetaProtocol = jsonFormat2(DagMeta.apply)

  }

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

  case class FixedSize(sizeBytes: Long) extends Chunker {

    override def cliParam: String = s"${paramName}size-$sizeBytes"

  }

  case object Buzhash extends Chunker {

    override def cliParam: String = s"${paramName}buzhash"

  }

  case class Rabin(min: Long, avg: Long, max: Long) extends Chunker {
    override def cliParam: String = s"${paramName}rabin-$min-$avg-$max"
  }

  case object DefaultRabin extends Chunker {
    override def cliParam: String = s"${paramName}rabin"
  }

  def apply(config: IpfsConfigOps): IpfsCliClient =
    new IpfsCliClient(ipfsCmd(config.isInDocker, Option(config.containerName)))

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

trait IpfsClientOps {

  def add(fn: Path,
          chunker: Chunker = Default,
          nocopy: Boolean = false,
          recursive: Boolean = false,
          wrapWithDir: Boolean = true,
          addHiddenFiles: Boolean = false,
          onlyHash: Boolean = false): Seq[String]

  def dagGet(hash: String): Seq[DagMeta]

  def objectLinks(hash: String): Seq[String]


}

class IpfsCliClient private(ipfsCmd: Seq[String]) extends IpfsClientOps {

  import spray.json._
  import IpfsCliClient.CliProtocol._

  def add(fn: Path,
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

  def dagGet(hash: String): Seq[DagMeta] = {
    val p = process(Seq("dag", "get", hash))
    p.lineStream
      .flatMap(p => Try(p.parseJson.convertTo[DagMeta]).toOption)
  }

  def objectLinks(hash: String): Seq[String] = {
    val p = process(Seq("object", "links", hash))
    p.lineStream
      .map(_.split("\\s+")(0))
  }

  private def process(params: Seq[String]) =
    Process(ipfsCmd ++ params)

}
