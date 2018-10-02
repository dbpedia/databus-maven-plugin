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

import org.dbpedia.databus.Properties
import org.dbpedia.databus.lib.Datafile
import org.dbpedia.databus.shared.rdf.vocab._

import org.apache.jena.rdf.model.{Model, ModelFactory, Resource}
import org.apache.jena.vocabulary.RDF

import scala.collection.JavaConverters._
import scala.language.reflectiveCalls

import java.io.File
import java.text.SimpleDateFormat

object DataFileToModel {

  val prefixes = Map(
    "dataid" -> "http://dataid.dbpedia.org/ns/core#",
    "dataid-ld" -> "http://dataid.dbpedia.org/ns/ld#",
    "dataid-mt" -> "http://dataid.dbpedia.org/ns/mt#",
    "dataid-pl" -> "http://dataid.dbpedia.org/ns/pl#",
    "dmp" -> "http://dataid.dbpedia.org/ns/dmp#",
    "dc" -> "http://purl.org/dc/terms/",
    "dcat" -> "http://www.w3.org/ns/dcat#",
    "void" -> "http://rdfs.org/ns/void#",
    "prov" -> "http://www.w3.org/ns/prov#",
    "xsd" -> "http://www.w3.org/2001/XMLSchema#",
    "owl" -> "http://www.w3.org/2002/07/owl#",
    "foaf" -> "http://xmlns.com/foaf/0.1/",
    "rdf" -> "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
    "rdfs" -> "http://www.w3.org/2000/01/rdf-schema#",
    "datacite" -> "http://purl.org/spar/datacite/",
    "spdx" -> "http://spdx.org/rdf/terms#",
    "sd" -> "http://www.w3.org/ns/sparql-service-description#"
  )


  def datafile2Model(datafile: Datafile, file: File, properties: Properties): Model = {
    implicit val model: Model = ModelFactory.createDefaultModel
    for ((key, value) <- prefixes) {
      model.setNsPrefix(key, value)
    }


    // main uri of dataid for SingleFile
    val thisResource = model.createResource("#" + properties.getDatafileFinal(file).getName)

    /**
      * linking to other constructs
      */
    val datasetResource = model.createResource( s"#${properties.finalName}")
    thisResource.addProperty(dataid.isDistributionOf,datasetResource)
    datasetResource.addProperty(dcat.distribution,thisResource)

    //type properties
    thisResource.addProperty(RDF.`type`, dataid.SingleFile)
    datasetResource.addProperty(RDF.`type`, dataid.Dataset)




    addBasicPropertiesToResource( properties, model, thisResource)


    // specific info about the file
    val modifiedDate = new SimpleDateFormat("yyyy-MM-dd").format(file.lastModified())
    thisResource.addProperty(model.getProperty(model.getNsPrefixURI("dc"), "modified"), model.createTypedLiteral(modifiedDate, model.getNsPrefixURI("xsd") + "date"))
    thisResource.addProperty(dataid.sha256sum, model.createLiteral(datafile.sha256sum))
    thisResource.addProperty(dataid.signature, model.createLiteral(datafile.signatureBase64))
    thisResource.addProperty(dataid.preview, model.createLiteral(datafile.preview))
    // todo add uncompressedByteSize if possible
    //thisResource.addProperty(model.getProperty(model.getNsPrefixURI("dataid"), "uncompressedByteSize"), model.createLiteral(datafile.bytes.toString))
    thisResource.addProperty(dcat.byteSize, model.createLiteral(datafile.bytes.toString))
    // todo review creation of this statement: the used property is not declared in dcat; looks like a slip of mind
    // sh: it is a dataid property. However, dataid modeled it as a property of the mimetype, which is the wrong place
    // files have one format extension and maybe one compressionextension and mimetypes have a list of likely extensions
    thisResource.addProperty(dataid.prop.formatExtension, model.createLiteral(datafile.formatExtension))
    //todo handle correctly, if not default
    thisResource.addProperty(dcat.downloadURL, model.createResource(properties.getDatafileFinal(file).getName))

    // mediatype
    val mediaType = model.createResource(model.getNsPrefixURI("dataid-mt") + datafile.mimetype.getClass.getSimpleName.replace("$", ""))
    mediaType.addProperty(RDF.`type`, model.createResource(s"${model.getNsPrefixURI("dataid-mt")}MediaType"))
    thisResource.addProperty(dcat.mediaType, mediaType)
    mediaType.addProperty(dataid.mimetype, datafile.mimetype.mimeType)
    thisResource.addProperty(dataid.compression, datafile.compressionVariant)

    model


  }

  def addBasicPropertiesToResource( properties: Properties, model: Model, thisResource: Resource) = {

    implicit def vocabModel = model

    // label
    for (label :String <- properties.labels.asScala) {
      val split = label.split("@")
      thisResource.addProperty(
        model.getProperty(model.getNsPrefixURI("rdfs"), "label"),
        model.createLiteral(split.head, split.last))
      thisResource.addProperty(model.getProperty(model.getNsPrefixURI("dc"),
        "title"), model.createLiteral(split.head, split.last))

    }

    //basic properties
    thisResource.addProperty(model.getProperty(model.getNsPrefixURI("dc"), "description"), model.createLiteral(properties.datasetDescription))
    // todo add version number, but this is a dataid issue
    thisResource.addProperty(model.getProperty(model.getNsPrefixURI("dc"), "conformsTo"), model.createResource(model.getNsPrefixURI("dataid")))
    thisResource.addProperty(model.getProperty(model.getNsPrefixURI("dc"), "hasVersion"), model.createLiteral(properties.version))
    thisResource.addProperty(model.getProperty(model.getNsPrefixURI("dc"), "issued"), model.createTypedLiteral(properties.issuedDate, model.getNsPrefixURI("xsd") + "date"))
    thisResource.addProperty(model.getProperty(model.getNsPrefixURI("dc"), "license"), model.createResource(properties.license))
     thisResource.addProperty(dataid.associatedAgent, model.createResource(properties.maintainer.toString))
    thisResource.addProperty(model.getProperty(model.getNsPrefixURI("dc"), "publisher"), model.createResource(properties.maintainer.toString))
  }
}
