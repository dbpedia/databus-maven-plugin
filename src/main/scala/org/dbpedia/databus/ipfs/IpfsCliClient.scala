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

class IpfsCliClient private(ipfsCmd: Seq[String]) {

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

  // todo make tail recursive?
  private[ipfs] def fetchAllHashes2(hash: String): Seq[String] = {
    if (hash.length == 59) {
      Seq(hash)
    } else {
      val re = objectLinks(hash)
      if (re.isEmpty) {
        Seq(hash)
      } else {
        re.flatMap(fetchAllHashes2)
      }
    }
  }

  private[ipfs] def fetchAllHashes(hash: String): Seq[String] = {
    val re = dagGet(hash)

    re.flatMap(dm =>
      dm.links
        .flatMap(li =>
          if (li.cid.v0.length == 59) {
            Seq(li.cid.v0)
          } else {
            fetchAllHashes(li.cid.v0)
          }
        )
    )
  }

}

object RunbableAppli2 extends App {

  val hs = Seq(
    //        "QmQtiMsYzYcnj8Y5CTPSosgcMRjvZRf5bpuYzsah93Nkg9",
    //        "QmSchskU7Kv2UegpD7WXdJkqEHKXd5WFKQyghwwciRUXAo",
    //        "QmWyVnQyd4fb7oXqi2GW8mBapt5bg6kfdiUABYW6u5bmF3",
    //    "bafkreiahnckfv2hwtk4aztlducpyj5fala5xy6mfu4zp7uxpi3dduojcvu"
    "QmYmaWpDFBksYF7PUNPcd3xJ66UK2W6TkPzMNQ1ri5md1T",
    "Qmf2T1euZRYVoqEWHPY3oEeYpR1j76yxXan2ZJoMHNB5Vq"
  )

  val cli = IpfsCliClient(true)

  val lis = cli.objectLinks(hs(0))
  println(lis)

  Util.outputFileDiff(hs, cli)
}

object RunnableAppli extends App {

  val cli = IpfsCliClient(true)

  //  val files = Seq(
  //    "/data/ipfs/fs/preferences.txt",
  //    "/data/ipfs/fs/preferences2.txt",
  //    "/data/ipfs/fs/preferences3.txt"
  //  )
  //  val chk = Rabin(16, 100, 500)

  //  val files = Seq(
  //    "/data/ipfs/fs/mappingbased-literals_lang=en.ttl-2",
  //    "/data/ipfs/fs/mappingbased-literals_lang=en.ttl",
  //    "/data/ipfs/fs/mappingbased-literals_lang=en.ttl.copy"
  //  )
  //  val files = Seq(
  //    "/data/ipfs/fs/mappingbased-literals_lang=en.ttl.bz2",
  //    "/data/ipfs/fs/mappingbased-literals_lang=en.ttl-2.bz2",
  //  )
  val files = Seq(
    //      "/data/ipfs/fs/mappingbased-literals_lang=en.ttl.bz2.1.out.gz",
    "/data/ipfs/fs/mappingbased-literals_lang=en.ttl.gz",
    "/data/ipfs/fs/mappingbased-literals_lang=en.ttl.bz2.1.out.may.gz"
  )

  val chk = Rabin(16, 100000, 1000000)

  val useArgs = !args.isEmpty

  val filesToProcess = if (useArgs) {
    args.toSeq
  } else {
    files
  }

  val hashes = filesToProcess
    .map(Paths.get(_))
    .flatMap(cli.add(_, chk, true, true))
    .map(p => {
      println("hash: " + p)
      p
    })
    .flatMap(cli.dagGet)
    .map(p => {
      println("dag: ")
      println(p)
      p
    })
    .filter(_.links.length == 1)
    .flatMap(_.links.map(v => v.cid.v0))

  println(hashes.size)
  hashes.foreach(s => println(s))

  if (useArgs) {
    Util.outputFileDiff(hashes, cli)
  }

}

object Util {

  def outputFileDiff(hashes: Seq[String], cli: IpfsCliClient) = {
    hashes
      .dropRight(1)
      .zip(hashes.drop(1))
      .map(p => {
        println("le hash: " + p._1)
        println("ri hash: " + p._2)
        p
      })
      .map {
        case (l, r) => (cli.fetchAllHashes2(l), cli.fetchAllHashes2(r))
      }
      .map(p => {
        println("le sz: " + p._1.size)
        println("ri sz: " + p._2.size)
        p
      })
      .map {
        case (l, r) => r.diff(l)
      }
      .foreach(hs => {
        println("diff size: " + hs.size)
      })
  }

}
