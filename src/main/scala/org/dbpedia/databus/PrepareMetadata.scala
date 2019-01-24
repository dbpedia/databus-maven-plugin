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

import org.dbpedia.databus.lib.{Datafile, SigningHelpers}
import org.dbpedia.databus.params.{BaseEntity => ScalaBaseEntity}
import org.dbpedia.databus.shared.helpers.conversions._
import org.dbpedia.databus.shared.rdf.conversions._
import org.dbpedia.databus.shared.rdf.vocab._
import org.dbpedia.databus.voc.DataFileToModel
import better.files.{File => _, _}
import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.apache.jena.riot.RDFLanguages.TURTLE
import org.apache.maven.plugin.{AbstractMojo, MojoExecutionException}
import org.apache.maven.plugins.annotations.{LifecyclePhase, Mojo}

import scala.language.reflectiveCalls
import java.io._

import org.apache.jena.datatypes.xsd.XSDDatatype.XSDdateTime

import org.apache.jena.vocabulary.{RDF, RDFS, XSD}
import org.dbpedia.databus.shared.authentification.AccountHelpers

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
@Mojo(name = "metadata", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, requiresOnline = true, threadSafe = true)
class PrepareMetadata extends AbstractMojo with Properties with SigningHelpers with DataFileToModel {

  @throws[MojoExecutionException]
  override def execute(): Unit = {
    //skip the parent module
    if (isParent()) {
      getLog.info(s"skipping parent $artifactId")
      return
    }

    val dataIdCollect: Model = ModelFactory.createDefaultModel


    getLog.info(s"looking for data files in: ${dataInputDirectory.getCanonicalPath}")
    getLog.info(s"Found ${getListOfInputFiles().size} files:\n${
      getListOfInputFiles().mkString(", ").replaceAll(dataInputDirectory.getCanonicalPath + "/" + artifactId, "")
    }")
    getLog.info(s"collecting metadata for each file (from parameters in pom.xml, from ${markdown} and from the file itself)")
    getListOfInputFiles().foreach(datafile => {
      processFile(datafile, dataIdCollect)

    })

    //retrieving all User Accounts
    //var accountOption = {
    //implicit val userAccounts: Model = SigningHelpers.registeredAccounts
    //Option(publisher.toString.asIRI.getProperty(foaf.account)).map(_.getObject.asResource)
    //}

    getLog.info(s"writing metadata to ${getDataIdFile()}")
    if (!dataIdCollect.isEmpty) {
      {
        implicit val editContext = dataIdCollect

        // add DataId
        addDataId(dataIdCollect)


        // add dataset metadata
        val datasetResource = dataIdCollect.createResource(s"#Dataset")
        datasetResource.addProperty(RDF.`type`, dataid.Dataset)
        addBasicPropertiesToResource(dataIdCollect, datasetResource)
        if (changelog.nonEmpty) {
          datasetResource.addProperty(dataid.changelog, changelog.asPlainLiteral)
        }

        /**
          * match WebId to Account Name
          */
        AccountHelpers.getAccountOption(publisher) match {

          case Some(account) => {

            val groupIRI = s"${account.getURI}/${groupId}".asIRI
            val artifactIRI = s"${account.getURI}/${groupId}/${artifactId}".asIRI
            val versionIRI = s"${account.getURI}/${groupId}/${artifactId}/${version}".asIRI
            groupIRI.addProperty(RDF.`type`, dataid.Group)
            artifactIRI.addProperty(RDF.`type`, dataid.Artifact)
            versionIRI.addProperty(RDF.`type`, dataid.Version)

            datasetResource.addProperty(dataid.groupId, groupIRI)
            datasetResource.addProperty(dataid.artifact, artifactIRI)
            datasetResource.addProperty(dataid.version, versionIRI)
          }

          case None => {

            datasetResource.addProperty(dataid.groupId, "https://github.com/dbpedia/accounts/blob/master/README.md#ACCOUNTNEEDED".asIRI)
            datasetResource.addProperty(dataid.artifact, "https://github.com/dbpedia/accounts/blob/master/README.md#ACCOUNTNEEDED".asIRI)
            getLog.warn("Not registered, Dataset URIs will not work, please register at https://github.com/dbpedia/accounts/blob/master/README.md#ACCOUNTNEEDED")
          }
        }

        /**
          * adding wasDerivedFrom other datasets
          */
        params.wasDerivedFrom.foreach { case ScalaBaseEntity(artifact, version) =>

          val baseEntityBlankNode = editContext.createResource().tap { baseEntityRes =>

            baseEntityRes.addProperty(dataid.artifact, artifact.toString.asIRI)
            baseEntityRes.addProperty(dcterms.hasVersion, version)
          }

          datasetResource.addProperty(prov.wasDerivedFrom, baseEntityBlankNode)
        }
      }

      //writing the metadatafile
      getDataIdFile().toScala.outputStream.foreach { os =>
        os.write((Properties.logo + "\n").getBytes)
        dataIdCollect.write(os, "turtle")
      }
      getLog.info(s"${getDataIdFile()} written")

    }
  }

  def addDataId(dataIdCollect: Model): Unit = {

    implicit def vocabModel = dataIdCollect

    /**
      * <http://downloads.dbpedia.org/2016-10/core-i18n/en/2016-10_dataid_en.ttl>
      * ? dataid:underAuthorization  <http://downloads.dbpedia.org/2016-10/core-i18n/en/2016-10_dataid_en.ttl?auth=maintainerAuthorization> , <http://downloads.dbpedia.org/2016-10/core-i18n/en/2016-10_dataid_en.ttl?auth=creatorAuthorization> , <http://downloads.dbpedia.org/2016-10/core-i18n/en/2016-10_dataid_en.ttl?auth=contactAuthorization> ;
      * dc:modified                "2017-07-06"^^xsd:date ;
      * foaf:primaryTopic          <http://dbpedia.org/dataset/main_dataset?lang=en&dbpv=2016-10> .
      */
    val dataIdResource = dataIdCollect.createResource("")
    dataIdResource.addProperty(dcterms.title, s"DataID metadata for ${groupId}/${artifactId}", "en")
    dataIdResource.addProperty(RDFS.`label`, s"DataID metadata for ${groupId}/${artifactId}", "en")
    dataIdResource.addProperty(dcterms.hasVersion, Properties.pluginVersion)
    dataIdResource.addProperty(RDF.`type`, dataid.DataId)
    dataIdResource.addProperty(RDFS.`comment`,
      s"""Metadata created by the DBpedia Databus Maven Plugin: https://github.com/dbpedia/databus-maven-plugin (Version ${Properties.pluginVersion})
         |The DataID ontology is a metadata omnibus, which can be extended to be interoperable with all metadata formats
         |Note that the metadata (the dataid.ttl file) is always CC-0, the files are licensed individually
         |Metadata created by ${publisher}
          """.stripMargin)

    dataIdResource.addProperty(dcterms.issued, ISO_INSTANT_NO_NANO.format(params.invocationTime).asTypedLiteral(XSDdateTime))
    dataIdResource.addProperty(dcterms.license, "http://purl.oclc.org/NET/rdflicense/cc-zero1.0".asIRI)
    dataIdResource.addProperty(dcterms.conformsTo, global.dataid.namespace)
    dataIdResource.addProperty(dataid.associatedAgent, publisher.toString.asIRI)
    dataIdResource.addProperty(dcterms.publisher, publisher.toString.asIRI)
  }

  def processFile(datafile: File, dataIdCollect: Model): Unit = {

    getLog.debug(s"found file ${
      datafile.getCanonicalPath
    }")
    val df: Datafile = Datafile(datafile)(getLog).ensureExists()
    df.updateSignature(singleKeyPairFromPKCS12)
    df.updateFileMetrics()

    val model = modelForDatafile(df)
    getLog.debug(df.toString)
    dataIdCollect.add(model)
  }
}

object PrepareMetadata {


}
