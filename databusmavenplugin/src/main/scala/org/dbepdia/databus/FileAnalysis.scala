package org.dbpedia.databus

import java.io.{File, FileInputStream}
import java.security.{DigestInputStream, MessageDigest}

import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter


/**
  * Analyse release files
  *
  * WebID
  * - dereference and download
  * - get the public key from the webid
  * - get the private key from the config, generate a public key and compare to the public key
  *
  * @phase generate-resources
  */
@Mojo(name = "analysis")
class FileAnalysis extends AbstractMojo {

  //@Parameter private var sourceDirectory
  //@Expression("${project.build.directory}")

  @Parameter
  var resourceDirectory: String = _

  @throws[MojoExecutionException]
  override def execute(): Unit = {

    getLog.info("reading from "+resourceDirectory);
    //val l = getListOfFiles(resourceDirectory)
    val l = new File(".").listFiles()
    getLog.info("reading from "+l);

    l.foreach(f => getLog.info(f.getAbsolutePath))
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

}

