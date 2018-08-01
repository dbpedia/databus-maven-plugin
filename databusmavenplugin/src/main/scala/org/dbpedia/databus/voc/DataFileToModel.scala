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

import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.apache.jena.vocabulary.RDF
import org.dbpedia.databus.lib.Datafile

object DataFileToModel {

  def datafile2Model (datafile:Datafile , file:File ):Model = {

    var model: Model = ModelFactory.createDefaultModel
    val thisResource = model.createResource(file.getName)
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


    thisResource.addProperty(RDF.`type`,"http://test.de/SingleFile")
    model



  }

}
