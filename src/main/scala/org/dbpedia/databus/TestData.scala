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
package org.dbpedia.databus

import java.io.{File, FileWriter}

import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.apache.maven.plugin.{AbstractMojo, MojoExecutionException}
import org.apache.maven.plugins.annotations.{LifecyclePhase, Mojo}
import org.dbpedia.databus.lib.{Datafile}
import org.dbpedia.databus.parse.{LineBasedRioDebugParser, RioOtherParser}
import org.dbpedia.databus.voc.UNKNOWN
import org.eclipse.rdf4j.rio.{RDFFormat, RDFParser, Rio}

@Mojo(name = "test-data", defaultPhase = LifecyclePhase.TEST)
class TestData extends AbstractMojo with Properties {


  @throws[MojoExecutionException]
  override def execute(): Unit = {
    //skip the parent module
    if (isParent()) {
      getLog.info("skipping parent module")
      return
    }

    parseLogDirectory.mkdirs()
    val parseLogFileWriter = new FileWriter(getParseLogFile())

    getListOfDataFiles(dataDirectory).foreach(datafile => {

      var parseLog = new StringBuilder
      var details = new StringBuilder
      val df: Datafile = Datafile.init(datafile)
      var model: Model = ModelFactory.createDefaultModel
      val thisResource = model.createResource("#" + datafile.getName)


      parseLog.append(s"${datafile.getName}\n${df.mimetype}\n")

      //val config = new ParserConfig
      //config.set(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES, false)
      //config.set(BasicParserSettings.FAIL_ON_UNKNOWN_LANGUAGES, false)
      //config.set(BasicParserSettings.VERIFY_DATATYPE_VALUES, false)
      //config.set(BasicParserSettings.VERIFY_LANGUAGE_TAGS, false)
      //config.set(BasicParserSettings.VERIFY_RELATIVE_URIS, false)

      var rdfParser: RDFParser = null
      //rdfParser.setParserConfig(config)

      if (df.mimetype != UNKNOWN) {

        if (df.mimetype.lineBased) {
          rdfParser = Rio.createParser(df.mimetype.rio)

          val (lines, all, good, bad) = df.getInputStream().acquireAndGet { in =>

            LineBasedRioDebugParser.parse(in, rdfParser)
          }

          parseLog.append(s"Lines: $lines\nTriples: $all\nValid: $good\nErrors: ${bad.size}\n")

          if (bad.size > 0) {
            details.append(s"\n#Error details for $datafile\n#${bad.mkString("\n#")}")

          }
        } else {
          rdfParser = Rio.createParser(df.mimetype.rio)
          val (success, errors) = df.getInputStream().acquireAndGet { in =>
            RioOtherParser.parse(in, rdfParser)
          }
          parseLog.append(s"Success = $success\nErrors = $errors\n")
        }
      }
      else {
        parseLog.append("no rdf format")
      }

      // parselog
      thisResource.addProperty(model.createProperty("parseLog"), parseLog.toString);
      model.write(parseLogFileWriter, "turtle")
      parseLogFileWriter.write(details.toString())
      getLog.info(parseLog)


    })

    getLog.info(s"Parselog written to ${getParseLogFile()}")
    parseLogFileWriter.close()

  }

}
