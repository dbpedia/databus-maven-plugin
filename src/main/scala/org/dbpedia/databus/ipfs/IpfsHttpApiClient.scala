package org.dbpedia.databus.ipfs

import java.net.URI

import org.dbpedia.databus.ipfs.IpfsApiClient.FileMeta
import scalaj.http.{BaseHttp, MultiPart}

import scala.concurrent.{ExecutionContext, Future}
import spray.json._


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


}

object RunnableApp extends App {

  implicit val ec = ExecutionContext.Implicits.global

  val cli = new IpfsApiClient("localhost", 5001, "http")

  cli.add("random_file", "hiiiiiii".getBytes)
    .onComplete {
      case Failure(exception) => exception.printStackTrace()
      case Success(value) => System.out.println(value)
    }


}



