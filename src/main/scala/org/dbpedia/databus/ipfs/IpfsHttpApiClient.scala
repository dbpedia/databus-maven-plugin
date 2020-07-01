package org.dbpedia.databus.ipfs


import java.io.ByteArrayOutputStream
import java.net.URI
import java.nio.file.{Files, Path, Paths}
import java.util.stream.Collectors

import org.dbpedia.databus.ipfs.IpfsApiClient.{MultiPart, MultipartHeader}
import scalaj.http.{BaseHttp, PlainUrlFunc}

import scala.concurrent.{Await, ExecutionContext, Future}
import spray.json._

import scala.concurrent.duration.Duration
import scala.util.{Failure, Random, Success}
import collection.JavaConverters._


object IpfsApiClient {

  class Header(val name: String, val value: String)

  object Header {
    def apply(name: String, value: String): Header = new Header(name, value)
  }

  case class MultipartHeader(override val name: String, override val value: String) extends Header(name, value)

  case class MultiPart(headers: Seq[MultipartHeader], data: Array[Byte]) {

    import MultiPart._

    def getBytes(boundary: Array[Byte]) = {
      val arr = new ByteArrayOutputStream()
      val sb = headers.map(h => h.name + ": " + h.value)
        .foldLeft(StringBuilder.newBuilder)((b, r) => b.append(r).append(Newline))
      val bytes = sb.toString().getBytes
      arr.write("--".getBytes())
      arr.write(boundary)
      arr.write(Newline.getBytes)
      arr.write(bytes)
      arr.write(Newline.getBytes)
      arr.write(data)
      arr.write(Newline.getBytes())
      arr.toByteArray
    }
  }

  object MultiPart {
    private val Newline = "\r\n"
    private val random = Random.alphanumeric

    def buildRequest(parts: Seq[MultiPart]): (Header, Array[Byte]) = {
      val bnd = getBoundary()
      val hdr = getMultipartContentType(bnd)
      val body = new ByteArrayOutputStream()
      parts.foreach(p => body.write(p.getBytes(bnd.getBytes())))
      body.write("--".getBytes())
      body.write(getBoundary().getBytes())
      body.write("--".getBytes())
      body.write(Newline.getBytes())
      (hdr, body.toByteArray)
    }

    def getMultipartContentType(boundary: String): Header =
      Header("Content-Type", s"""multipart/form-data; boundary=$boundary""")

    def getBoundary(): String = {
      val s = random.take(20).foldLeft(StringBuilder.newBuilder)((b, c) => b.append(c))
      s"--${s.toString()}"
    }
  }



  object IpfsApiProtocol extends DefaultJsonProtocol {

    implicit val fileMetaProtocol = jsonFormat(FileMeta.apply, "Name", "Hash")

  }

  case class FileMeta(name: String, cid: String)

}

import IpfsApiClient.IpfsApiProtocol._

class IpfsApiClient(host: String, port: Int, protocol: String)(implicit ec: ExecutionContext) {

  private val baseIpfsApiUrl = URI.create(s"$protocol://$host:$port/api/v0/")
  private val client = new BaseHttp()

  //todo add files without sending over the network
  def addFolder(folder: Path, root: Path, dockerRoot: Path, nocopy: Boolean = false): Future[String] = {
    // todo set parameters in a neat way
    val uri = baseIpfsApiUrl.resolve(s"add?wrap-with-directory=true&nocopy=$nocopy")
    val req = client(uri.toURL.toString)

    val folderToAdd = root.resolve(folder)
    val files = Files.walk(folderToAdd)
      .collect(Collectors.toList())
      .asScala
      .map(_.toFile)
      .filter(!_.isHidden)

    val mp = files
      .map(fn => {
        val pathVal = root.relativize(fn.toPath).toString
        if (fn.isDirectory) {
          val headers = Seq(
            MultipartHeader("Content-Disposition", s"""form-data; name="file"; filename="$pathVal""""),
            MultipartHeader("Content-Type", "application/x-directory")
          )

          MultiPart(headers, new Array[Byte](0))
        } else {
          var headers = Seq(
            MultipartHeader("Content-Disposition", s"""form-data; name="file"; filename="$pathVal""""),
            MultipartHeader("Content-Type", "application/octet-stream")
          )
          if (nocopy) {
            val absPath: Path = dockerRoot.resolve(root.relativize(fn.toPath))
            headers = headers :+ MultipartHeader("Abspath", absPath.toString)
          }

          val data = if (nocopy) {
            new Array[Byte](0)
          } else {
            Files.readAllBytes(root.resolve(fn.toPath))
          }
          MultiPart(headers, data)
        }
      })

    // futures are needed for the ability to use async client in the future
    Future {
      val (hdr, bd) = MultiPart.buildRequest(mp)
      val mpReq = req
        .copy(urlBuilder = PlainUrlFunc)
        .header(hdr.name, hdr.value)
        .postData(bd)
      mpReq.asString.body
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

  def filesLs(path: String = "") = {
    val uri = baseIpfsApiUrl.resolve(s"files/ls")
    val req = client(uri.toURL.toString)
    Future {
      val mpReq = req
        .param("arg", path)
        .postData("")
      mpReq.asString.body
    }
  }

  def filestoreLs() = {
    val uri = baseIpfsApiUrl.resolve(s"filestore/ls")
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
  val fut = cli
    //    .addFolder(Paths.get("new_to_add"), Paths.get("/Users/Kirill/Desktop/DbPedia/ipfs_node_data/data"))
    .addFolder(
      Paths.get("new_to_add"),
      Paths.get("/Users/Kirill/Desktop/DbPedia/ipfs_node_data/data"),
      Paths.get("/data/ipfs"),
      true)
    .andThen {
      case Failure(exception) =>
        println("Error:")
        exception.printStackTrace()
      case Success(value) =>
        println("Value: " + value)
    }

  Await.ready(fut, Duration.Inf)

}



