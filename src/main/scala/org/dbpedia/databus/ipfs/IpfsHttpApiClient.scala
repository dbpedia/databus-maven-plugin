package org.dbpedia.databus.ipfs

import java.net.URI

import org.dbpedia.databus.ipfs.IpfsApiClient.FileMeta
import scalaj.http.{BaseHttp, MultiPart}

import scala.concurrent.{Await, ExecutionContext, Future}
import spray.json._

import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}


object IpfsApiClient {

  object IpfsApiProtocol extends DefaultJsonProtocol {

    implicit val fileMetaProtocol = jsonFormat(FileMeta.apply, "Name", "Hash")

  }

  case class FileMeta(name: String, cid: String)

}

import IpfsApiClient.IpfsApiProtocol._

class IpfsApiClient(host: String, port: Int, protocol: String)(implicit ec: ExecutionContext) {

  private val baseIpfsApiUrl = URI.create(s"$protocol://$host:$port/api/v0/")
  private val client = new BaseHttp()

  def add(filename: String, data: Array[Byte]): Future[FileMeta] = {
    val uri = baseIpfsApiUrl.resolve("add")
    val req = client(uri.toURL.toString)

    val mp = MultiPart("file", filename, "application/octet-stream", data)
    // futures are needed for the ability to use async client in the future
    Future {
      val mpReq = req.postMulti(mp)
      mpReq.asString.body.parseJson.convertTo[FileMeta]
    }
  }

  def stats(): Future[String] = {
    val uri = baseIpfsApiUrl.resolve("stats/bw")
    val req = client(uri.toURL.toString)
    Future {
      val mpReq = req.postData("")
      mpReq.asString.body
    }
  }

  def keyList() = {
    val uri = baseIpfsApiUrl.resolve(s"key/list?l=true")
    val req = client(uri.toURL.toString)
    Future {
      val mpReq = req.postData("")
      mpReq.asString.body
    }
  }

  def dhtFindProvs(cid: String) = {
    val uri = baseIpfsApiUrl.resolve(s"dht/findprovs?arg=$cid")
    val req = client(uri.toURL.toString)
    Future {
      val mpReq = req.postData("")
      mpReq.asString.body
    }
  }

  def dhtGet(cid: String) = {
    val uri = baseIpfsApiUrl.resolve(s"dht/get?arg=$cid")
    val req = client(uri.toURL.toString)
    Future {
      val mpReq = req.postData("")
      mpReq.asString.body
    }
  }

  def filesLs() = {
    val uri = baseIpfsApiUrl.resolve(s"files/ls")
    val req = client(uri.toURL.toString)
    Future {
      val mpReq = req.postData("")
      mpReq.asString.body
    }
  }


}

object RunnableApp extends App {

  implicit val ec = ExecutionContext.Implicits.global

  val cli = new IpfsApiClient("localhost", 5001, "http")

  //  val cid = "QmZukcc5QKpQxBFWS3BNWt26HCCGGYC8HiwHHPLYHyoid8"
  val cid = "Qmez6piTp4yTJ6H5bYWHhd4jCWniuVXDGqAy5DGiMEhXCT"
  //  cli.add("random_file", "hiiiiiii".getBytes)
  println("Init")
  val fut = cli.keyList()
  fut
    .onComplete {
      case Failure(exception) => exception.printStackTrace()
      case Success(value) => System.out.println("Value: " + value)
    }

  Await.ready(fut, Duration.Inf)


}



