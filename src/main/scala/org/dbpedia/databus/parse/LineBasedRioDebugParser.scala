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
import org.eclipse.rdf4j.rio._

import scala.collection.mutable
import scala.io.{Codec, Source}


/**
  * currently supports only Ntriples, Nquad
  */
object LineBasedRioDebugParser {

  val batchSize = 500 * 1000


  def parse(in: InputStream, rdfParser: RDFParser): (Integer, Integer, Integer, mutable.HashSet[String]) = {


    var (lines, totalTriples, good, wrongTriples) = (0, 0, 0, new mutable.HashSet[String])

    var batch = new mutable.HashSet[String]
    val it = Source.fromInputStream(in)(Codec.UTF8).getLines()
    var lc = 0


    while (it.hasNext) {
      lc+=1
      val line = it.next().toString
      // batch it
      batch += line
      if (batch.size >= batchSize) {
        //todo better way
        var (a, b, c) = parseBatch(rdfParser, batch)
        totalTriples += a
        good += b
        wrongTriples ++= c
        System.out.println(s"${lc} parsed, more than ${lc%batchSize} duplicates")

        //reset batch
        batch = new mutable.HashSet[String]
      }
    }

    // remaining
    //todo better way
    var (a, b, c) = parseBatch(rdfParser, batch)
    totalTriples += a
    good += b
    wrongTriples ++= c

    (lc, totalTriples, good, wrongTriples)
  }

  def parseBatch(rdfParser: RDFParser, batch: mutable.HashSet[String]): (Integer, Integer, mutable.HashSet[String]) = {
    val (totalTriples, good, wrongTriples) = (0, 0, new mutable.HashSet[String])

    try {
      // hand batch to parser
      val baos: ByteArrayInputStream = new ByteArrayInputStream(batch.mkString("\n").getBytes());
      rdfParser.parse(baos, "")
      baos.close()
      // parsing of batch successfull
    } catch {
      case e: Exception => {
        //parsing failed somewhere, reiterate
        for (line <- batch) {
          try {
            // hand each line to parser
            // todo check whether baos needs to be closed
            val baos: ByteArrayInputStream = new ByteArrayInputStream(line.getBytes());
            rdfParser.parse(baos, "");

          } catch {
            case rio: Exception => {}
              //L.trace("parser error, the problem triple was:\n"+one+" "+rio.getMessage());
              wrongTriples.add("Err: " + rio.getMessage() + ": " + line);
          }
        }
      }
    }
    (batch.size, batch.size - wrongTriples.size, wrongTriples)
  }
}
