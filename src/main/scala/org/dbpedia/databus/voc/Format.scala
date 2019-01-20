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

import org.apache.maven.plugin.logging.Log
import java.io.File


//class Format(val mimeType: String = "UNKNOWN", val lineBased: Boolean = false, val rio: org.eclipse.rdf4j.rio.RDFFormat, val jena: org.apache.jena.riot.RDFFormat) {
//}

class Format(val mimeType: String = "UNKNOWN", val lineBased: Boolean = false) {
  override def toString = s"$mimeType"

  def isRDF(): Boolean = {
    this.isInstanceOf[RDFBased]
  }
}

trait RDFBased {
  val rio: org.eclipse.rdf4j.rio.RDFFormat
  val jena: org.apache.jena.riot.RDFFormat
}

// unknown and binary
object UNKNOWN extends Format("UNKNOWN", false) {}

object ApplicationOctetStream extends Format("application/octet-stream", false)

object TextPlain extends Format("text/plain", true)

//csv
object TextCSV extends Format(mimeType = "text/csv", lineBased = true)

object TextTabSeparatedValues extends Format("text/tab-separated-values", lineBased = true)

/**
  * RDF Based Formats
  */
object ApplicationNTriples extends Format("application/n-triples", true) with RDFBased {
  val rio = org.eclipse.rdf4j.rio.RDFFormat.NTRIPLES
  val jena = org.apache.jena.riot.RDFFormat.NTRIPLES
}

object TextTurtle extends Format("text/turtle", false) with RDFBased {
  val rio = org.eclipse.rdf4j.rio.RDFFormat.TURTLE
  val jena = org.apache.jena.riot.RDFFormat.TURTLE
}

object ApplicationRDFXML extends Format("application/rdf+xml", false) with RDFBased {
  val rio = org.eclipse.rdf4j.rio.RDFFormat.RDFXML
  val jena = org.apache.jena.riot.RDFFormat.RDFXML
}

object ApplicationTrig extends Format("application/trig", false) with RDFBased {
  val rio = org.eclipse.rdf4j.rio.RDFFormat.TRIG
  val jena = org.apache.jena.riot.RDFFormat.TRIG
}

object ApplicationNQuad extends Format("application/n-quads", true) with RDFBased {
  val rio = org.eclipse.rdf4j.rio.RDFFormat.NQUADS
  val jena = org.apache.jena.riot.RDFFormat.NQUADS
}


object Format {

  lazy val knownFormats = Map(
    "nt" -> ApplicationNTriples,
    "ttl" -> TextTurtle,
    "tql" -> ApplicationNQuad,
    "nq" -> ApplicationNQuad,
    "rdf" -> ApplicationRDFXML,
    "csv" -> TextCSV,
    "tsv" -> TextTabSeparatedValues,
    "tab" -> TextTabSeparatedValues,
    "trig" -> ApplicationTrig,
    "bin" -> ApplicationOctetStream
  )

  def detectMimeTypeByFileExtension(extensions: Seq[String])(implicit log: Log): (String, Format) = {

    val extensionMatch = extensions.reverse.toStream.map({ ext =>

      knownFormats.get(ext) match {

        case None => {
          log.warn(s"Unable to assign file extension '$ext' to a known format, extend here: https://github.com/dbpedia/databus-maven-plugin/blob/master/src/main/scala/org/dbpedia/databus/voc/Format.scala")
          None
        }

        case Some(format) => Some((ext, format))
      }
    }).collectFirst { case Some(pair) => pair }

    extensionMatch.getOrElse(("", UNKNOWN))
  }
}

