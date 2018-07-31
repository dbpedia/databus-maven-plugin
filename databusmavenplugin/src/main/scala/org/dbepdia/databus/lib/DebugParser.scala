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

package org.dbepdia.databus.lib
import java.io._

import org.eclipse.rdf4j.rio._

import scala.collection.mutable
import scala.io.Source


/**
  * currently supports only Ntriples
  */
object DebugParser {
  val batchSize = 500 * 1000

  def parse(in: InputStream): mutable.HashSet[String] = {
    val wrongTriples = new mutable.HashSet[String]
    val batch = new mutable.HashSet[String]
    val it = Source.fromInputStream(in)
    //val config = new ParserConfig
    //config.set(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES, false)
    //config.set(BasicParserSettings.FAIL_ON_UNKNOWN_LANGUAGES, false)
    //config.set(BasicParserSettings.VERIFY_DATATYPE_VALUES, false)
    //config.set(BasicParserSettings.VERIFY_LANGUAGE_TAGS, false)
    //config.set(BasicParserSettings.VERIFY_RELATIVE_URIS, false)

    val rdfParser = Rio.createParser(RDFFormat.NTRIPLES)
    //rdfParser.setParserConfig(config)

    while (it.hasNext) {
      // batch it
      batch += it.next().toString
      if (batch.size >= batchSize) {
        wrongTriples ++= parseBatch(rdfParser, batch)
        //reset batch
        batch.empty
      }
    }

    // remaining
    wrongTriples ++= parseBatch(rdfParser, batch)

    wrongTriples
  }

  def parseBatch(rdfParser: RDFParser, batch: mutable.HashSet[String]): mutable.HashSet[String] = {
    val wrongTriples = new mutable.HashSet[String]

    try {
      val baos: ByteArrayInputStream = new ByteArrayInputStream(batch.mkString("\n").getBytes());


      rdfParser.parse(baos, "");
      baos.close()

      // parsing successfull
    } catch {
      case e:RDFParseException => {
        //parsing failed, reiterate
        for (line <- batch) {
          try {
            val baos: ByteArrayInputStream = new ByteArrayInputStream(line.getBytes());
            rdfParser.parse(baos, "");
          } catch {
            case rio: Exception => {}
              //L.trace("parser error, the problem triple was:\n"+one+" "+rio.getMessage());
              wrongTriples.add("#" + rio.getMessage() + "\n" + line);
          }
        }
      }
    }
    wrongTriples

  }
}
