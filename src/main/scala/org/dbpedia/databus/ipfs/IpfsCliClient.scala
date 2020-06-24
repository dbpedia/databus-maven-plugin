package org.dbpedia.databus.ipfs

import java.nio.file.{Path, Paths}

import org.dbpedia.databus.ipfs.IpfsCliClient.{Buzhash, Chunker, DagMeta, Default, DefaultRabin, Rabin}
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

}

class IpfsCliClient {

  import spray.json._
  import IpfsCliClient.CliProtocol._

  private val ipfsCmd: Seq[String] = Seq(
    "docker",
    "exec",
    "ipfs_host",
    "ipfs"
  )

  private def process(params: Seq[String]) =
    Process(ipfsCmd ++ params)

  def add(fn: Path,
          chunker: Chunker = Default,
          nocopy: Boolean = false,
          recursive: Boolean = false,
          wrapWithDir: Boolean = true,
          addHiddenFiles: Boolean = false): Seq[String] = {
    val params = Seq(
      "add",
      chunker.cliParam,
      s"--wrap-with-directory=$wrapWithDir",
      s"--hidden=$addHiddenFiles",
      s"--nocopy=$nocopy",
      s"--recursive=$recursive",
      fn.toAbsolutePath.toString,
    )
    val p = process(params)
    p.lineStream
      .map(s => s.split("\\s+")(1))
  }

  def dagGet(hash: String): Seq[DagMeta] = {
    val p = process(Seq("dag", "get", hash))
    p.lineStream
      .flatMap(p => Try(p.parseJson.convertTo[DagMeta]).toOption)
  }

  def fetchAllHashes(hash: String): Seq[String] = {
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

  val cli = new IpfsCliClient()
  Util.outputFileDiff(hs, cli)
}

object RunnableAppli extends App {

  val cli = new IpfsCliClient()

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
        case (l, r) => (cli.fetchAllHashes(l), cli.fetchAllHashes(r))
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
