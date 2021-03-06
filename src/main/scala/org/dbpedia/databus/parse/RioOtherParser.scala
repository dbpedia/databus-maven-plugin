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
  */
object RioOtherParser {

  val batchSize = 500 * 1000


  def parse(in: InputStream, rdfParser: RDFParser): (Boolean, String) = {

    var success = false
    var errors = "None"
    try {
      rdfParser.parse(in, "")
      // parsing of successfull
      success = true
    } catch {
      case e: RDFParseException => {
        //parsing failed somewhere, reiterate
        success = false
        errors = e.getLineNumber + "" + e.getMessage

      }
    }
    (success, errors)
  }
}
