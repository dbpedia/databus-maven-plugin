package org.dbpedia.databus

import java.io.{File, FileInputStream}
import java.security.{DigestInputStream, MessageDigest}

import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
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
  * @phase generate-resources
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

}

