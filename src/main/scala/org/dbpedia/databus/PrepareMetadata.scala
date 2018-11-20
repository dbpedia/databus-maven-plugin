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

import org.dbpedia.databus.lib.Datafile
import org.dbpedia.databus.voc.DataFileToModel

import better.files.{File => _, _}
import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.apache.maven.plugin.{AbstractMojo, MojoExecutionException}
import org.apache.maven.plugins.annotations.{LifecyclePhase, Mojo}

import java.io._


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
class PrepareMetadata extends AbstractMojo with Properties with Locations with SigningHelpers {

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
    if (!dataIdCollect.isEmpty) {
      val datasetResource = dataIdCollect.createResource(s"#${finalName}")
      DataFileToModel.addBasicPropertiesToResource(this, dataIdCollect, datasetResource)

      //adding todonote
      datasetResource.addProperty(dataIdCollect.createProperty("http://dataid.dbpedia.org/ns/core#todonote"), "we are still refactoring code for dataid creation, much more information will be available at this resource later")

      // add dataset metadata
      // retrieve User Account Name
      var userAccounts: Model = ModelFactory.createDefaultModel
      userAccounts.read("https://raw.githubusercontent.com/dbpedia/accounts/master/accounts.ttl", "turtle")
      var publisherResource = userAccounts.getResource(publisher.toString)
      var account = publisherResource.getProperty(userAccounts.getProperty("http://xmlns.com/foaf/0.1/account"))

      if (publisher == null || account == null) {
        datasetResource.addProperty(dataIdCollect.createProperty("http://dataid.dbpedia.org/ns/core#bundle"),
          dataIdCollect.createResource("http://dataid.dbpedia.org/ns/core#ACCOUNTNEEDED"))
        datasetResource.addProperty(dataIdCollect.createProperty("http://dataid.dbpedia.org/ns/core#artifact"),
          dataIdCollect.createResource("http://dataid.dbpedia.org/ns/core#ACCOUNTNEEDED"))

      } else {
        datasetResource.addProperty(dataIdCollect.createProperty("http://dataid.dbpedia.org/ns/core#bundle"),
          dataIdCollect.createResource(s"${account.getResource.getURI}/${bundle}"))
        datasetResource.addProperty(dataIdCollect.createProperty("http://dataid.dbpedia.org/ns/core#artifact"),
          dataIdCollect.createResource(s"${account.getResource.getURI}/${bundle}/${artifactId}"))

      }

      //datasetResource.addProperty(dataIdCollect.createProperty("http://dataid.dbpedia.org/ns/core#bundle"))
      //datasetResource.addProperty(dataIdCollect.createProperty("http://dataid.dbpedia.org/ns/core#artifact"))
      //datasetResource.addProperty(dataIdCollect.createProperty("http://dataid.dbpedia.org/ns/core#artifact"))

      getDataIdFile().toScala.outputStream.foreach { os =>

        dataIdCollect.write(os, "turtle")
      }
    }
  }

  def processFile(datafile: File, dataIdCollect: Model): Unit = {
    getLog.info(s"found file ${datafile.getCanonicalPath}")
    val df: Datafile = Datafile.init(datafile)

    df
      .updateSHA256sum()
      .updateBytes()
      .updateSignature(singleKeyPairFromPKCS12)

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

