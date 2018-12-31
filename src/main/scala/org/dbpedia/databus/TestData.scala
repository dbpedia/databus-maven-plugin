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

import org.dbpedia.databus.lib.Datafile
import org.dbpedia.databus.parse.{LineBasedRioDebugParser, RioOtherParser}

import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.apache.jena.riot.RDFLanguages
import org.apache.maven.plugin.{AbstractMojo, MojoExecutionException}
import org.apache.maven.plugins.annotations.{LifecyclePhase, Mojo}
import org.eclipse.rdf4j.rio.{RDFParser, Rio}

import java.nio.charset.StandardCharsets
import java.nio.file.Files

@Mojo(name = "test-data", defaultPhase = LifecyclePhase.TEST)
class TestData extends AbstractMojo with Properties {


  @throws[MojoExecutionException]
  override def execute(): Unit = {

    //skip the parent module
    if (isParent()) {
      getLog.info("skipping parent module")
      return
    }

    val parseLogFileWriter = Files.newBufferedWriter(getParseLogFile().toPath, StandardCharsets.UTF_8) 

    getListOfInputFiles().foreach(datafile => {

      var parseLog = new StringBuilder
      var details = new StringBuilder
      val df: Datafile = Datafile(datafile)(getLog).ensureExists()
      val model: Model = ModelFactory.createDefaultModel
      val finalBasename = df.finalBasename(params.versionToInsert)
      val thisResource = model.createResource("#" + finalBasename)
      val prefixParse ="http://dataid.dbpedia.org/ns/pl#"

      parseLog.append(s"${finalBasename}\n${df.format}\n")

      //val config = new ParserConfig
      //config.set(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES, false)
      //config.set(BasicParserSettings.FAIL_ON_UNKNOWN_LANGUAGES, false)
      //config.set(BasicParserSettings.VERIFY_DATATYPE_VALUES, false)
      //config.set(BasicParserSettings.VERIFY_LANGUAGE_TAGS, false)
      //config.set(BasicParserSettings.VERIFY_RELATIVE_URIS, false)

      var rdfParser: RDFParser = null
      //rdfParser.setParserConfig(config)

      if (!(df.format.rio eq null)) {

        if (df.format.lineBased) {
          rdfParser = Rio.createParser(df.format.rio)

          val (lines, all, good, bad) = df.getInputStream().apply { in =>

            LineBasedRioDebugParser.parse(in, rdfParser)
          }

          thisResource.addProperty(model.createProperty(prefixParse+"lines"), lines.toString);
          thisResource.addProperty(model.createProperty(prefixParse+"triples"), all.toString);
          thisResource.addProperty(model.createProperty(prefixParse+"valid"), good.toString);
          thisResource.addProperty(model.createProperty(prefixParse+"errors"), bad.size.toString);
          //parseLog.append(s"Lines: $lines\nTriples: $all\nValid: $good\nErrors: ${bad.size}\n")

          if (bad.size > 0) {
            details.append(s"\n#Error details for $datafile\n#${bad.mkString("\n#")}\n")
          }
        } else {
          rdfParser = Rio.createParser(df.format.rio)
          val (success, errors) = df.getInputStream().apply { in =>
            RioOtherParser.parse(in, rdfParser)
          }
          parseLog.append(s"Success = $success\nErrors = $errors\n")

        }
      }
      else {
        parseLog.append("no rdf format")
      }

      // parselog
      thisResource.addProperty(model.createProperty(prefixParse+"parselog"), parseLog.toString);
      model.write(parseLogFileWriter, RDFLanguages.strLangTurtle)
      parseLogFileWriter.write(details.toString())
      getLog.info(parseLog)


    })

    getLog.info(s"Parselog written to ${getParseLogFile()}")
    parseLogFileWriter.close()
  }
}
