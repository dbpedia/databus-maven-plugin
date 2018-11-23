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
import org.dbpedia.databus.shared.rdf.conversions._
import org.dbpedia.databus.shared.rdf.vocab._
import org.dbpedia.databus.voc.DataFileToModel

import better.files.{File => _, _}
import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.apache.maven.plugin.{AbstractMojo, MojoExecutionException}
import org.apache.maven.plugins.annotations.{LifecyclePhase, Mojo}

import scala.language.reflectiveCalls

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
class PrepareMetadata extends AbstractMojo with Properties with SigningHelpers {

  @throws[MojoExecutionException]
  override def execute(): Unit = {
    //skip the parent module
    if(isParent()) {
      getLog.info("skipping parent module")
      return
    }

    val dataIdCollect: Model = ModelFactory.createDefaultModel


    getLog.info(s"looking for data files in: ${dataInputDirectory.getCanonicalPath}")
    getListOfDataFiles().foreach(datafile => {
      processFile(datafile, dataIdCollect)
    })



    // write the model to target
    if(!dataIdCollect.isEmpty) {

      // add dataset metadata
      // retrieve User Account Name
      var accountOption = {
        implicit val userAccounts: Model = ModelFactory.createDefaultModel
        userAccounts.read("https://raw.githubusercontent.com/dbpedia/accounts/master/accounts.ttl", "turtle")

        Option(publisher.toString.asIRI.getProperty(foaf.account)).map(_.getObject.asResource)
      }

      {
        implicit val editContext = dataIdCollect

        val datasetResource = dataIdCollect.createResource(s"#${finalName}")
        DataFileToModel.addBasicPropertiesToResource(this, dataIdCollect, datasetResource)

        //adding todonote
        datasetResource.addProperty(dataid.prop.todonote, "we are still refactoring code for dataid creation, much " +
          "more information will be available at this resource later")

        accountOption match {

          case Some(account) => {

            datasetResource.addProperty(dataid.bundle, s"${account.getURI}/${bundle}".asIRI)
            datasetResource.addProperty(dataid.artifact, s"${account.getURI}/${bundle}/${artifactId}".asIRI)
          }

          case None => {

            datasetResource.addProperty(dataid.bundle, "http://dataid.dbpedia.org/ns/core#ACCOUNTNEEDED".asIRI)
            datasetResource.addProperty(dataid.artifact, "http://dataid.dbpedia.org/ns/core#ACCOUNTNEEDED".asIRI)
          }
        }
      }

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
      if(fileName.contains(key)) {
        mimetypes.outer = Some(value)
      }
    }
    }
    innerMimeTypes.foreach { case (key, value) => {
      if(fileName.contains(key)) {
        mimetypes.inner = Some(value)
      }
    }
    }
    mimetypes
  }

  case class MimeTypeHelper(var outer: Option[String], var inner: Option[String])
}
