package org.dbpedia.databus

import java.io.{File, FileInputStream}
import java.nio.file.Files
import java.security._
import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}

import org.apache.maven.plugin.{AbstractMojo, MojoExecutionException}
import org.apache.maven.plugins.annotations.{LifecyclePhase, Mojo, Parameter}


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
  private val multiModuleBaseDirectory : String = ""

  @Parameter
  val resourceDirectory: String = ""

  @throws[MojoExecutionException]
  override def execute(): Unit = {
    val moduleDirectories = getModules(multiModuleBaseDirectory)
    moduleDirectories.foreach(moduleDir => {
      getLog.info(s"reading from module $moduleDir")
      getListOfFiles(s"$moduleDir/$resourceDirectory").foreach(datafile => {
        getLog.info(s"found file $datafile")
        val md5 = computeHash(datafile.getAbsolutePath)
        getLog.info(s"md5: ${md5}")
        // triple-count != line-count? Comments, duplicates or other serializations would make them differ
        // TODO: implement a better solution
        val lines = io.Source.fromFile(datafile).getLines.size
        getLog.info(s"Lines: $lines")
        val bytes = datafile.length()
        getLog.info(s"ByteSize: $bytes")
        val mimetypes = getMimeType(datafile.getName)
        val innerMime = mimetypes.inner
        val outerMime = mimetypes.outer
        innerMime.foreach(v =>
          getLog.info(s"MimeTypes(inner): $v")
        )
        outerMime.foreach(v =>
          getLog.info(s"MimeTypes(outer): $v")
        )
      })
    })
  }

  /**
    * returns list of Subdirectories
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

  /** From https://stackoverflow.com/questions/41642595/scala-file-hashing
    * Compute a hash of a file
    * The output of this function should match the output of running "md5 -q <file>"
    */
  def computeHash(path: String): String = {
    val buffer = new Array[Byte](8192)
    val md5 = MessageDigest.getInstance("MD5")

    val dis = new DigestInputStream(new FileInputStream(new File(path)), md5)
    try {
      while (dis.read(buffer) != -1) {}
    } finally {
      dis.close()
    }

    md5.digest.map("%02x".format(_)).mkString
  }

  def sign(privateKeyPath: String, dataid: Array[Byte]): String = {
    val keyBytes = Files.readAllBytes(new File(privateKeyPath).toPath)
    val spec = new PKCS8EncodedKeySpec(keyBytes)
    val kf = KeyFactory.getInstance("RSA")
    val privateKey = kf.generatePrivate(spec)
    val rsa = Signature.getInstance("SHA1withRSA")
    rsa.initSign(privateKey)
    rsa.update(dataid)
    rsa.sign().toString
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
    outerMimeTypes.foreach{case (key, value) => {
      if(fileName.contains(key)){
        mimetypes.outer = Some(value)
      }
    }}
    innerMimeTypes.foreach{case (key, value) => {
      if(fileName.contains(key)){
        mimetypes.inner = Some(value)
      }
    }}
    mimetypes
  }
  case class MimeTypeHelper(var outer: Option[String], var inner: Option[String])
}

