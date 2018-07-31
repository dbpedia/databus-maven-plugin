/*-
 * #%L
 * databus-maven-plugin
 * %%
 * Copyright (C) 2018 Sebastian Hellmann (on behalf of the DBpedia Association)
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

package org.dbpedia.databus

import java.io._
import java.util
import java.util.Base64

import org.apache.commons.compress.compressors.{CompressorException, CompressorInputStream, CompressorStreamFactory}
import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.apache.maven.plugin.{AbstractMojo, MojoExecutionException}
import org.apache.maven.plugins.annotations.{LifecyclePhase, Mojo, Parameter}
import org.dbepdia.databus.lib.{Datafile, Hash, Sign}


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
    val df: Datafile = Datafile.init(datafile)
    val privateKey = Sign.readPrivateKeyFile(privateKeyFile)


    df.updateSignature(privateKey)
    getLog.info(df.toString)

    /**
      * Begin basic stats



    // private key signature

    val signatureBytes: Array[Byte] = Sign.sign(privateKey, datafile);


    val signatureBase64 = new String(Base64.getEncoder.encode(signatureBytes))
    getLog.info(s"Signature: $signatureBase64")

    //verify
    val verified = Sign.verify(privateKey, datafile, signatureBytes)
    getLog.info(s"Verified: $verified")

    //compression
    val compressionVariant: String = detectCompression(datafile)
    getLog.info("Compression: " + compressionVariant)


    // mimetypes
    val mimetypes = getMimeType(datafile.getName)
    val innerMime = mimetypes.inner


    innerMime.foreach(v =>
      getLog.info(s"MimeTypes(inner): $v")
    )

    // triple-count != line-count? Comments, duplicates or other serializations would make them differ
    // TODO: implement a better solution
    // val lines = io.Source.fromFile(datafile).getLines.size
    //getLog.info(s"Lines: $lines")

    val model = ModelFactory.createDefaultModel
    //model.write( new FileWriter( new File(outputDirectory+"/"+datafile.getName+".dataid.ttl")),"turtle")
*/
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


  /**
    * replaced partly by detect compression
    **/
  @Deprecated
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

