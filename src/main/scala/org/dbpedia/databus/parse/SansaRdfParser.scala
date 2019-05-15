/*-
 * #%L
 * DBpedia Databus Maven Plugin
 * %%
 * Copyright (C) 2018 - 2019 Sebastian Hellmann (on behalf of the DBpedia Association)
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

import better.files.File

import org.apache.jena.riot.system.ErrorHandlerFactory
import net.sansa_stack.rdf.spark.io._
import org.apache.spark.sql.SparkSession

object SansaRdfParser {

  def parse(file: File, numberOfworkers: String = "*"): Unit = {

    val spakSession = SparkSession.builder()
      .master(s"local[$numberOfworkers]")
      .appName("org.dbpedia.databus.parse.SansaRdfParser")
      .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      .config("spark.local.dir","/tmp/spark.local.dir")
      .config("spark.ui.enabled","false")
      .getOrCreate()

    val tripleRDD = NTripleReader.load(
      session = spakSession,
      file.pathAsString,
      stopOnBadTerm = ErrorParseMode.SKIP,
      stopOnWarnings = WarningParseMode.SKIP,
      checkRDFTerms = true,
      errorLog = ErrorHandlerFactory.stdLogger
    )

    tripleRDD.saveAsNTriplesFile(
      s"${file.pathAsString}.parsed")

  }
}
