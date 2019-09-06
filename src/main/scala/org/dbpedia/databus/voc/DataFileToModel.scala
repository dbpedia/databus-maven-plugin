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

package org.dbpedia.databus.voc

import org.dbpedia.databus.{Parameters, Properties}
import org.dbpedia.databus.lib.Datafile
import org.dbpedia.databus.shared.rdf.conversions._
import org.dbpedia.databus.shared.rdf.vocab._
import better.files._
import org.apache.jena.datatypes.xsd.XSDDatatype._
import org.apache.jena.rdf.model.{Literal, Model, ModelFactory, Resource}
import org.apache.jena.vocabulary.{OWL, RDF, RDFS, XSD}
import org.apache.maven.plugin.AbstractMojo

import scala.collection.JavaConverters._
import scala.language.reflectiveCalls
import java.time.{ZoneId, ZonedDateTime}

object DataFileToModel {

  val prefixes = Map(
    "dataid" -> global.dataid.namespace,
    "dcat" -> global.dcat.namespace,
    "rdf" -> RDF.uri,
    "rdfs" -> RDFS.uri,
    "dataid-mt" -> "http://dataid.dbpedia.org/ns/mt#",
    "dataid-pl" -> "http://dataid.dbpedia.org/ns/pl#",
    "dataid-cv" -> "http://dataid.dbpedia.org/ns/cv#",
    "prov" -> global.prov.namespace,
    "databus" -> "https://databus.dbpedia.org/",
    "xsd" -> XSD.NS,
    "dct" -> global.dcterms.namespace
    //    "dataid-ld" -> "http://dataid.dbpedia.org/ns/ld#",
    //    "dmp" -> "http://dataid.dbpedia.org/ns/dmp#",
    //    "void" -> "http://rdfs.org/ns/void#",
    //    "owl" -> OWL.NS,
    //    "foaf" -> global.foaf.namespace,
    //    "datacite" -> "http://purl.org/spar/datacite/",
    //    "spdx" -> "http://spdx.org/rdf/terms#",
    //    "sd" -> "http://www.w3.org/ns/sparql-service-description#"
  )
}

trait DataFileToModel extends Properties with Parameters {

  this: AbstractMojo =>


  def modelForDatafile(datafile: Datafile, fileIriBase: String): Model = {
    import DataFileToModel._

    implicit val model: Model = ModelFactory.createDefaultModel

    for ((key, value) <- prefixes) {
      model.setNsPrefix(key, value)
    }

    /*
     main uri of dataid for SingleFile
     Type
      */
    val singleFileResource = ("#" + datafile.finalBasename(params.versionToInsert)).asIRI
    singleFileResource.addProperty(RDF.`type`, dataid.SingleFile)


    /**
      * adding file iri
      */
    singleFileResource.addProperty(dataid.file, (fileIriBase + datafile.finalBasename(params.versionToInsert)).asIRI)

    /**
      * linking to dataset
      */
    val datasetResource = model.createResource(s"#Dataset")
    singleFileResource.addProperty(dataid.isDistributionOf, datasetResource)
    datasetResource.addProperty(dcat.distribution, singleFileResource)

    /**
      * basic properties
      */
    addBasicPropertiesToResource(model, singleFileResource)

    /**
      * specific info about the file
      */
    addSpecificInfoToFile(model, datafile, singleFileResource)


    /**
      * mediatype
      * sh: it is a dataid property. However, dataid modeled it as a property of the mimetype, which is the wrong place
      * files have one format extension and maybe one compressionextension and mimetypes have a list of likely extensions
      */

    addMediatypeAndVariants(model, datafile, singleFileResource)

    model
  }

  def addMediatypeAndVariants(model: Model, datafile: Datafile, singleFileResource: Resource) = {

    implicit def vocabModel = model

    def mediaTypeName = datafile.format.getClass.getSimpleName.stripSuffix("$")

    //TODO move to shared lib
    val dataid_mt = "http://dataid.dbpedia.org/ns/mt#"
    val mediaTypeRes = (dataid_mt + mediaTypeName).asIRI
    mediaTypeRes.addProperty(RDF.`type`, s"${dataid_mt}MediaType".asIRI)
    singleFileResource.addProperty(dcat.mediaType, mediaTypeRes)
    mediaTypeRes.addProperty(dataid.mimetype, datafile.format.mimeType)
    singleFileResource.addProperty(dataid.formatExtension, datafile.formatExtension.asPlainLiteral)
    singleFileResource.addProperty(dataid.compression, datafile.compressionOrArchiveDesc)

    // content variant
    datafile.contentVariantExtensions.foreach { contentVariant =>
      if (contentVariant.contains("=")) {
        val cv = contentVariant.split("=")
        val (key, value): (String, String) = (cv(0), cv(1))

        // keeping the value part
        singleFileResource.addProperty(dataid.contentVariant, value)

        dataid.contentVariant.addProperty(RDF.`type`, OWL.DatatypeProperty)
        singleFileResource.addProperty(dataidcv.prop.selectDynamic(key), value)
        dataidcv.prop.selectDynamic(key).addProperty(RDFS.subPropertyOf, dataid.contentVariant)

      } else {
        singleFileResource.addProperty(dataid.contentVariant, contentVariant)
        singleFileResource.addProperty(dataidcv.prop.tag, contentVariant)
        dataidcv.prop.tag.addProperty(RDFS.subPropertyOf, dataid.contentVariant)
      }
    }
  }


  def addBasicPropertiesToResource(model: Model, resource: Resource) = {

    implicit def vocabModel = model
    // todo add version number, but this is a dataid issue
    resource.addProperty(dcterms.conformsTo, global.dataid.namespace)
    resource.addProperty(dcterms.hasVersion, version.asPlainLiteral)
    resource.addProperty(dcterms.issued, ISO_INSTANT_NO_NANO.format(params.issuedDate).asTypedLiteral(XSDdateTime))
    resource.addProperty(dcterms.license, license.asIRI)
    resource.addProperty(dataid.associatedAgent, publisher.toString.asIRI)
    resource.addProperty(dcterms.publisher, publisher.toString.asIRI)
    if (maintainer != null) {
      resource.addProperty(dataid.maintainer, maintainer.toString.asIRI)
    }
  }

  def addSpecificInfoToFile(model: Model, datafile: Datafile, singleFileResource: Resource) = {

    implicit def vocabModel = model


    val modificationTime = Option(ZonedDateTime.ofInstant(datafile.file.toScala.lastModifiedTime, ZoneId.systemDefault())) match {
      case Some(instant) => instant
      case None => params.modifiedDate
    }

    singleFileResource.addProperty(dcterms.modified, ISO_INSTANT_NO_NANO.format(modificationTime).asTypedLiteral(XSDdateTime))
    singleFileResource.addProperty(dataid.sha256sum, datafile.sha256sum.asPlainLiteral)
    singleFileResource.addProperty(dataid.signature, datafile.signatureBase64.asPlainLiteral)
    singleFileResource.addProperty(dataid.preview, datafile.preview)
    singleFileResource.addProperty(dcat.byteSize, datafile.bytes.toString.asTypedLiteral(XSDdecimal))
     val dcatdownloadurlpath: String = if (absoluteDCATDownloadUrlPath != null) {
      absoluteDCATDownloadUrlPath
    } else {
      ""
    }
    singleFileResource.addProperty(dcat.downloadURL, (dcatdownloadurlpath + datafile.finalBasename(params.versionToInsert)).asIRI)


    def decimalize(l:Long):Literal= {
      if (l <0L)
      {Float.NaN .toString.asTypedLiteral(XSDdecimal)}
      else
      {l.toString.asTypedLiteral(XSDdecimal)}
    }
    singleFileResource.addProperty(dataid.duplicates, decimalize(datafile.duplicates))
    singleFileResource.addProperty(dataid.uncompressedByteSize, decimalize(datafile.uncompressedByteSize))
    singleFileResource.addProperty(dataid.nonEmptyLines, decimalize(datafile.nonEmptyLines))
    singleFileResource.addProperty(dataid.sorted, datafile.sorted.toString.asTypedLiteral(XSDboolean))

 }
}
