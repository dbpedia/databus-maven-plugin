package org.dbpedia.databus

import java.io.{File, FileInputStream, FileWriter, OutputStream}
import java.nio.file.Files
import java.security._
import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import java.util

import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.apache.maven.plugin.{AbstractMojo, MojoExecutionException}
import org.apache.maven.plugins.annotations.{LifecyclePhase, Mojo, Parameter}
import org.dbepdia.databus.lib.HashAndSign


/**
  * Analyse release data files
  *
  * Generates statistics from the release data files such as:
  * * md5sum
  * * bytesize
  * * compression algo used
  * * internal mimetype
  * Also creates a signature with the private key
  *
  * Later more can be added like
  * * links
  * * triple size
  *
  */
@Mojo(name = "analysis", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
class FileAnalysis extends AbstractMojo {

  @Parameter(defaultValue = "${maven.multiModuleProjectDirectory}", readonly = true)
  private val multiModuleBaseDirectory: String = ""

  @Parameter(defaultValue = "${project.packaging}", readonly = true)
  private val packaging: String = ""

  @Parameter(defaultValue = "${project.build.outputDirectory}", readonly = true)
  private val outputDirectory: String = ""


  @Parameter var privateKeyFile: File = _
  @Parameter val resourceDirectory: String = ""
  @Parameter var contentVariants: util.ArrayList[String] = _


  @throws[MojoExecutionException]
  override def execute(): Unit = {
    //skip the parent module
    if (packaging.equals("pom")) {
      getLog.info("skipping parent module")
      return
    }

    val moduleDirectories = getModules(multiModuleBaseDirectory)

    // processing each module
    moduleDirectories.foreach(moduleDir => {
      getLog.info(s"reading from module $moduleDir")

      // processing all file per module
      getListOfFiles(s"$moduleDir/$resourceDirectory").foreach(datafile => {
        processFile(datafile)
      })
    })
  }

  def processFile(datafile: File): Unit = {
    getLog.info(s"found file $datafile")

    /**
      * Begin basic stats
      */

    // md5
    val md5 = HashAndSign.computeHash(datafile)
    getLog.info(s"md5: ${md5}")

    // bytes
    val bytes = datafile.length()
    getLog.info(s"ByteSize: $bytes")

    // private key signature
    val signature = HashAndSign.sign(privateKeyFile, datafile);
    getLog.info(s"Signature: $signature")

    // mimetypes
    val mimetypes = getMimeType(datafile.getName)
    val innerMime = mimetypes.inner
    val outerMime = mimetypes.outer
    innerMime.foreach(v =>
      getLog.info(s"MimeTypes(inner): $v")
    )
    outerMime.foreach(v =>
      getLog.info(s"MimeTypes(outer): $v")
    )


    /**
      * extended stats
      */
    // triple-count != line-count? Comments, duplicates or other serializations would make them differ
    // TODO: implement a better solution
    val lines = io.Source.fromFile(datafile).getLines.size
    getLog.info(s"Lines: $lines")


    /**
      * write to file
      */

    val model = ModelFactory.createDefaultModel
    //model.write( new FileWriter( new File(outputDirectory+"/"+datafile.getName+".dataid.ttl")),"turtle")

  }

  /**
    * returns list of Subdirectories
    *
    * @param dir
    * @return
    */
  def getModules(dir: String): List[File] = {
    val d = new File(dir)
    if (d.exists && d.isDirectory) {
      d.listFiles().filter(_.isDirectory).toList
    } else {
      List[File]()
    }
  }

  /**
    * guess what
    *
    * @param dir
    * @return
    */
  def getListOfFiles(dir: String): List[File] = {
    val d = new File(dir)
    if (d.exists && d.isDirectory) {
      d.listFiles.filter(_.isFile).toList
    } else {
      List[File]()
    }
  }


  def verify(publicKeyBytes: Array[Byte], dataid: Array[Byte], signature: Array[Byte]): Boolean = {
    val spec = new X509EncodedKeySpec(publicKeyBytes)
    val kf = KeyFactory.getInstance("RSA")
    val publicKey = kf.generatePublic(spec)
    val rsa = Signature.getInstance("SHA1withRSA")
    rsa.initVerify(publicKey)
    rsa.update(dataid)
    rsa.verify(signature)
  }

  def getMimeType(fileName: String): MimeTypeHelper = {
    val innerMimeTypes = Map(
      "ttl" -> "text/turtle",
      "tql" -> "application/n-quads",
      "nt" -> "application/n-quads",
      "xml" -> "application/xml"
    )
    val outerMimeTypes = Map(
      "gz" -> "application/x-gzip",
      "bz2" -> "application/x-bzip2",
      "sparql" -> "application/sparql-results+xml"
    )
    var mimetypes = MimeTypeHelper(None, None)
    outerMimeTypes.foreach { case (key, value) => {
      if (fileName.contains(key)) {
        mimetypes.outer = Some(value)
      }
    }
    }
    innerMimeTypes.foreach { case (key, value) => {
      if (fileName.contains(key)) {
        mimetypes.inner = Some(value)
      }
    }
    }
    mimetypes
  }

  case class MimeTypeHelper(var outer: Option[String], var inner: Option[String])

}

