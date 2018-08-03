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

import org.apache.maven.plugin.{AbstractMojo, MojoExecutionException}
import org.apache.maven.plugins.annotations.{LifecyclePhase, Mojo}
import org.dbpedia.databus.lib.{Datafile, FileHelper}
import org.dbpedia.databus.parse.{LineBasedRioDebugParser, RioOtherParser}
import org.eclipse.rdf4j.rio.{RDFFormat, RDFParser, Rio}

@Mojo(name = "validate-files", defaultPhase = LifecyclePhase.VALIDATE)
class ValidateFiles extends AbstractMojo with Properties {


  @throws[MojoExecutionException]
  override def execute(): Unit = {
    //skip the parent module
    if (isParent()) {
      getLog.info("skipping parent module")
      return
    }

    var parseLog = new StringBuilder
    FileHelper.getListOfFiles(dataDirectory).foreach(datafile => {
      if (datafile.getName.startsWith(artifactId)) {
<<<<<<< HEAD
        val df: Datafile = Datafile.init(datafile)
        val in = df.getInputStream()

        parseLog.append(s"*****************${datafile.getName}\n${df.mimetype}\n")

        //val config = new ParserConfig
        //config.set(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES, false)
        //config.set(BasicParserSettings.FAIL_ON_UNKNOWN_LANGUAGES, false)
        //config.set(BasicParserSettings.VERIFY_DATATYPE_VALUES, false)
        //config.set(BasicParserSettings.VERIFY_LANGUAGE_TAGS, false)
        //config.set(BasicParserSettings.VERIFY_RELATIVE_URIS, false)

        var rdfParser: RDFParser = null
        //rdfParser.setParserConfig(config)

        if (df.mimetype.lineBased) {
          rdfParser = Rio.createParser(df.mimetype.rio)
          getLog.info("" + rdfParser.getRDFFormat)

          val (lines, all, good, bad) = LineBasedRioDebugParser.parse(in, rdfParser)
          parseLog.append(s"Triples: $all\nValid: $good\nErrors: ${bad.size}\n")
          if (bad.size > 0) {
            parseLog.append(s"Error details:\n${bad.mkString("\n")}")
=======
        getLog.info(s"Validating file $datafile")
        val df: Datafile = Datafile.init(datafile, this)
        df.mimetype match {
          case "application/n-triples" => {
            val (all, good, bad) = DebugParser.parse(df.getInputStream(), RDFFormat.NTRIPLES)
            getLog.info("format" + RDFFormat.NTRIPLES)
            getLog.info("total " + all + " good " + good + " bad "+bad.size)
            getLog.info(bad.mkString("/n"))
>>>>>>> 1dbd9c16cf525812f986d7c8025026a8c92922d2
          }
        } else {

          if (df.mimetype != null) {
            rdfParser = Rio.createParser(df.mimetype.rio)
            val (success, errors) = RioOtherParser.parse(in, rdfParser)
            parseLog.append(s"Success = $success\nErrors = $errors\n")
          }
          else {
            parseLog.append("no rdf format")
          }


        }
      }
    })


    getLog.info(parseLog.toString())

  }

}
