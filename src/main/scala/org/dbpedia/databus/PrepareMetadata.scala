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

import org.dbpedia.databus.lib.{Datafile, Sign}
import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.apache.maven.plugin.{AbstractMojo, MojoExecutionException}
import org.apache.maven.plugins.annotations.{LifecyclePhase, Mojo}
import java.io._

import org.dbpedia.databus.shared.rdf.vocab
import org.dbpedia.databus.voc.DataFileToModel




/**
  * Analyse release data files
  *
  * Generates statistics from the release data files such as:
  * * md5sum
  * * file size in bytes
  * * compression algo used
  * * internal mimetype
  * Also creates a signature with the private key
  *
  * Later more can be added like
  * * links
  * * triple size
  *
  */
@Mojo(name = "metadata", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
class PrepareMetadata extends AbstractMojo with Properties {

  @throws[MojoExecutionException]
  override def execute(): Unit = {
    //skip the parent module
    if (isParent()) {
      getLog.info("skipping parent module")
      return
    }

    var dataIdCollect: Model = ModelFactory.createDefaultModel



    getLog.info(s"looking for data files in: ${dataInputDirectory.getCanonicalPath}")
    getListOfDataFiles().foreach(datafile => {
      processFile(datafile, dataIdCollect)
    })



    // write the model to target
    if(!dataIdCollect.isEmpty) {
      val datasetResource = dataIdCollect.createResource( s"#${finalName}")
      val dataid = vocab.dataid.inModel(dataIdCollect)
      DataFileToModel.addBasicPropertiesToResource( this, dataIdCollect, dataid, datasetResource)


      datasetResource.addProperty(dataIdCollect.createProperty("todonote"), "we are still refactoring code for dataid creation, much more information will be available at this resource later")
      var db = getDataIdFile()
      dataIdCollect.write(new FileWriter(db), "turtle")
    }
  }

  def processFile(datafile: File, dataIdCollect: Model): Unit = {
    getLog.info(s"found file ${datafile.getCanonicalPath}")
    val df: Datafile = Datafile.init(datafile)
    val privateKey = Sign.readPrivateKeyFile(privateKeyFile)


    df
      .updateSHA256sum()
      .updateBytes()
      .updateSignature(privateKey)

    val model = df.toModel(this)
    getLog.info(df.toString)
    dataIdCollect.add(model)
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

