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

package org.dbpedia.databus.parse

import java.io._
import java.util.concurrent.TimeUnit
import org.eclipse.rdf4j.rio._
import scala.collection.mutable
import scala.io.Source
import com.codahale.metrics.{ConsoleReporter, Meter, MetricRegistry}


/**
  * currently supports only Ntriples
  */
object DebugParser {


  private val metrics = new MetricRegistry
  private val triples = metrics.meter("triples")
  private val reporter: ConsoleReporter = ConsoleReporter.forRegistry(metrics)
    .convertRatesTo(TimeUnit.SECONDS)
    .convertDurationsTo(TimeUnit.MILLISECONDS)
    .build()

  reporter.start(1, TimeUnit.SECONDS)

  val batchSize = 500 * 1000


  def parse(in: InputStream, rdfFormat: RDFFormat): (Integer, Integer, mutable.HashSet[String]) = {

    var (totalTriples, good, wrongTriples) = (0, 0, new mutable.HashSet[String])

    val batch = new mutable.HashSet[String]
    val it = Source.fromInputStream(in).getLines()
    //val config = new ParserConfig
    //config.set(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES, false)
    //config.set(BasicParserSettings.FAIL_ON_UNKNOWN_LANGUAGES, false)
    //config.set(BasicParserSettings.VERIFY_DATATYPE_VALUES, false)
    //config.set(BasicParserSettings.VERIFY_LANGUAGE_TAGS, false)
    //config.set(BasicParserSettings.VERIFY_RELATIVE_URIS, false)

    val rdfParser = Rio.createParser(rdfFormat)
    //rdfParser.setParserConfig(config)

    while (it.hasNext) {
      val tmp = it.next().toString
      // batch it
      batch += tmp
      if (batch.size >= batchSize) {
        //todo better way
        var (a, b, c) = parseBatch(rdfParser, batch)
        totalTriples += a
        good += b
        wrongTriples ++= c

        //reset batch
        batch.empty
      }
    }

    // remaining
    //todo better way
    var (a, b, c) = parseBatch(rdfParser, batch)
    totalTriples += a
    good += b
    wrongTriples ++= c

    reporter.report()
    (totalTriples, good, wrongTriples)
  }

  def parseBatch(rdfParser: RDFParser, batch: mutable.HashSet[String]): (Integer, Integer, mutable.HashSet[String]) = {
    val (totalTriples, good, wrongTriples) = (0, 0, new mutable.HashSet[String])

    try {
      val baos: ByteArrayInputStream = new ByteArrayInputStream(batch.mkString("\n").getBytes());


      rdfParser.parse(baos, "");
      triples.mark()
      baos.close()
      // parsing successfull
    } catch {
      case e: RDFParseException => {
        //parsing failed, reiterate
        for (line <- batch) {
          try {
            val baos: ByteArrayInputStream = new ByteArrayInputStream(line.getBytes());
            rdfParser.parse(baos, "");
            triples.mark()
          } catch {
            case rio: Exception => {}
              //L.trace("parser error, the problem triple was:\n"+one+" "+rio.getMessage());
              wrongTriples.add("#" + rio.getMessage() + "\n" + line);
          }
        }
      }
    }
    (batch.size, batch.size - wrongTriples.size, wrongTriples)
  }
}
