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

import java.io.File

import org.apache.jena.atlas.json.JSON
import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.apache.jena.vocabulary.RDF
import org.dbpedia.databus.Properties
import org.dbpedia.databus.lib.Datafile

import scala.collection.JavaConverters._

object DataFileToModel {

  val mediaTypeBase = "MediaType_"

  def datafile2Model(datafile: Datafile, file: File, properties: Properties): Model = {
    var model: Model = ModelFactory.createDefaultModel
    val thisResource = model.createResource(file.getName)
    val downloadURL = properties.downloadURL + file.getName
    val latestVersion =
      if (properties.latestVersion != "") properties.latestVersion
      else downloadURL

    //nsPrefixes
    val prefixes = JSON.parse(scala.io.Source.fromFile("prefixes.json").mkString.replaceAll("#[^\"]+", ""))
    val keys: java.util.Set[String] = prefixes.keys()
    keys.asScala.foreach(prefix => {
      model.setNsPrefix(prefix, prefixes.get(prefix).getAsString.value())
    })

    //type properties
    thisResource.addProperty(RDF.`type`, model.createResource(s"${model.getNsPrefixURI("dataid")}SingleFile"))
    thisResource.addProperty(model.getProperty(model.getNsPrefixURI("rdfs"), "label"), model.createLiteral(properties.englishLabel, "en"))

    //dataid properties
    thisResource.addProperty(model.getProperty(model.getNsPrefixURI("dataid"), "associatedAgent"), model.createResource(properties.maintainer.toString))
    thisResource.addProperty(model.getProperty(model.getNsPrefixURI("dataid"), "associatedAgent"), model.createResource(properties.publisher.toString))
    thisResource.addProperty(model.getProperty(model.getNsPrefixURI("dataid"), "checksum"), model.createLiteral(datafile.md5))
    thisResource.addProperty(model.getProperty(model.getNsPrefixURI("dataid"), "isDistributionOf"), model.createResource(properties.dataset))
    thisResource.addProperty(model.getProperty(model.getNsPrefixURI("dataid"), "latestVersion"), model.createResource(latestVersion))
    thisResource.addProperty(model.getProperty(model.getNsPrefixURI("dataid"), "preview"), model.createLiteral(datafile.preview))
    thisResource.addProperty(model.getProperty(model.getNsPrefixURI("dataid"), "uncompressedByteSize"), model.createLiteral(datafile.bytes.toString))

    //properties
    thisResource.addProperty(model.getProperty(model.getNsPrefixURI("dc"), "conformsTo"), model.createResource(model.getNsPrefixURI("dataid").split("#").head))
    thisResource.addProperty(model.getProperty(model.getNsPrefixURI("dc"), "description"), model.createLiteral(properties.datasetDescription))
    thisResource.addProperty(model.getProperty(model.getNsPrefixURI("dc"), "hasVersion"), model.createLiteral(properties.version))
    thisResource.addProperty(model.getProperty(model.getNsPrefixURI("dc"), "issued"), model.createLiteral(properties.issuedDate, model.getNsPrefixURI("xsd") + "date"))
    thisResource.addProperty(model.getProperty(model.getNsPrefixURI("dc"), "license"), model.createResource(properties.license))
    thisResource.addProperty(model.getProperty(model.getNsPrefixURI("dc"), "modified"), model.createLiteral(properties.modifiedDate, model.getNsPrefixURI("xsd") + "date"))
    thisResource.addProperty(model.getProperty(model.getNsPrefixURI("dc"), "publisher"), model.createResource(properties.publisher.toString))
    thisResource.addProperty(model.getProperty(model.getNsPrefixURI("dc"), "title"), model.createLiteral(properties.englishLabel, "en"))

    //dcat properties
    thisResource.addProperty(model.getProperty(model.getNsPrefixURI("dcat"), "byteSize"), model.createLiteral(datafile.bytes.toString))
    thisResource.addProperty(model.getProperty(model.getNsPrefixURI("dcat"), "downloadURL"), model.createResource(downloadURL))

    val format = datafile.mimetype
    // mediatype
    val mediaType = model.createResource(model.getNsPrefixURI("dataid-mt") + datafile.mimetype.getClass.getName)
    mediaType.addProperty(RDF.`type`, model.createResource(s"${model.getNsPrefixURI("dataid-mt")}MediaType"))
    thisResource.addProperty(model.getProperty(model.getNsPrefixURI("dcat"), "mediaType"), mediaType)


    mediaType.addProperty(model.getProperty(model.getNsPrefixURI("dataid"),
      "mimetype"), datafile.mimetype.mimeType)
    thisResource.addProperty(model.getProperty(model.getNsPrefixURI("dataid"),
      "compression"), datafile.compressionVariant)
    //compressedMediaType.addProperty(model.getProperty(model.getNsPrefixURI("dataid"), "typeExtension"), "." + file.getName.substring(file.getName.lastIndexOf(".") + 1))


    /*
    dataid-mt:MediaType_turtle_x-bzip2
        a                      dataid:MediaType ;
        dataid:innerMediaType  dataid:MediaType_turtle ;
        dataid:typeExtension   ".bz2" ;
        dataid:typeTemplate    "application/x-bzip2" ;
        dc:conformsTo          <http://dataid.dbpedia.org/ns/core> .
     */
    /*
    <http://dbpedia.org/dataset/article_categories?lang=en&dbpv=2016-10&file=article_categories_en.tql.bz2>
        a                            dataid:SingleFile ;


        rdfs:label                   "Article Categories"@en , "core-i18n/en/article_categories_en.tql.bz2" ;
        dataid:associatedAgent       <http://wiki.dbpedia.org/dbpedia-association> ;
        dataid:checksum              <http://dbpedia.org/dataset/article_categories?lang=en&dbpv=2016-10&file=article_categories_en.tql.bz2&checksum=md5> ;
        dataid:isDistributionOf      <http://dbpedia.org/dataset/article_categories?lang=en&dbpv=2016-10> ;
        dataid:latestVersion         <http://dbpedia.org/dataset/article_categories?lang=en&dbpv=2016-10&file=article_categories_en.tql.bz2> ;
        dataid:preview               <http://downloads.dbpedia.org/preview.php?file=2016-10_sl_core-i18n_sl_en_sl_article_categories_en.tql.bz2> ;
        dataid:uncompressedByteSize  6558796473 ;
        dc:conformsTo                <http://dataid.dbpedia.org/ns/core> ;
        dc:description               "Links from concepts to categories using the SKOS vocabulary."@en ;
        dc:hasVersion                <http://downloads.dbpedia.org/2016-10/core-i18n/en/2016-10_dataid_en.ttl?version=1.0.0> ;
        dc:issued                    "2017-07-01"^^xsd:date ;
        dc:license                   <http://purl.oclc.org/NET/rdflicense/cc-by-sa3.0> ;
        dc:modified                  "2017-07-06"^^xsd:date ;
        dc:publisher                 <http://wiki.dbpedia.org/dbpedia-association> ;
        dc:title                     "Article Categories"@en ;
        dcat:byteSize                396463888 ;
        dcat:downloadURL             <http://downloads.dbpedia.org/2016-10/core-i18n/en/article_categories_en.tql.bz2> ;
        dcat:mediaType               dataid-mt:MediaType_n-quads_x-bzip2 .
     */
    model


  }

}
