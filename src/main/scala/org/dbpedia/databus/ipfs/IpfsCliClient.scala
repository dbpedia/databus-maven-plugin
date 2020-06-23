package org.dbpedia.databus.ipfs

import java.nio.file.{Path, Paths}

import org.dbpedia.databus.ipfs.IpfsCliClient.{Chunker, DagMeta, Default, DefaultRabin, Rabin}
import spray.json.DefaultJsonProtocol
import scala.sys.process._


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
      .map(_.parseJson.convertTo[DagMeta])
  }

}

object RunbableAppli2 extends App {

  val hs = Seq(
    "QmQtiMsYzYcnj8Y5CTPSosgcMRjvZRf5bpuYzsah93Nkg9",
    "QmSchskU7Kv2UegpD7WXdJkqEHKXd5WFKQyghwwciRUXAo",
    "QmWyVnQyd4fb7oXqi2GW8mBapt5bg6kfdiUABYW6u5bmF3"
  )

  val cli = new IpfsCliClient()
  val dag = cli.dagGet("QmWyVnQyd4fb7oXqi2GW8mBapt5bg6kfdiUABYW6u5bmF3")

  println(dag)
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
  //    "/data/ipfs/fs/infobox-properties_lang=en.ttl.bz2",
  //    "/data/ipfs/fs/infobox-properties_lang=en.ttl-2.bz2"
  //  )
  val files = Seq(
    "/data/ipfs/fs/infobox-properties_lang=en.ttl",
    "/data/ipfs/fs/infobox-properties_lang=en.ttl-2"
  )

  val chk = Rabin(16, 200000, 1000000)

  val hashes = files
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
    .filter(_.links.length > 1)
    .map(_.links.map(v => v.cid.v0))

  println(hashes.size)
  hashes.foreach(s => println(s.size))

  hashes
    .dropRight(1)
    .zip(hashes.drop(1))
    .map(p => {
      println("le: " + p._1.size)
      println("ri: " + p._2.size)
      p
    })
    .map {
      case (l, r) => r.diff(l)
    }
    .map(hs => {
      println(hs.size);
      hs
    })
    .foreach(println)

}
