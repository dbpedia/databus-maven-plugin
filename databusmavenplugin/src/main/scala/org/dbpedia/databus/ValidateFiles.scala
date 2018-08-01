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
import org.apache.maven.plugins.annotations.{LifecyclePhase, Mojo, Parameter}
import org.dbpedia.databus.lib.{Datafile, FileHelper}
import org.dbpedia.databus.parse.DebugParser
import org.eclipse.rdf4j.rio.RDFFormat

@Mojo(name = "validate-files", defaultPhase = LifecyclePhase.VALIDATE)
class ValidateFiles extends AbstractMojo with Properties {


  @throws[MojoExecutionException]
  override def execute(): Unit = {
    //skip the parent module
    if (isParent()) {
      getLog.info("skipping parent module")
      return
    }

    FileHelper.getListOfFiles(dataDirectory).foreach(datafile => {
      if (datafile.getName.startsWith(artifactId)) {
        getLog.info(s"Validating file $datafile")
        val df: Datafile = Datafile.init(datafile)
        df.mimetype match {
          case "application/n-triples" => {
            getLog.info("parsing file " + datafile + " with " + RDFFormat.NTRIPLES + " parser")
            getLog.info(DebugParser.parse(df.getInputStream(), RDFFormat.NTRIPLES).mkString("/n"))
          }
          case "application/n-quads" => {
            getLog.info("parsing with nquads")
            getLog.info(DebugParser.parse(df.getInputStream(), RDFFormat.NQUADS).mkString("/n"))
          }
          case _ => {
            getLog.info(df.mimetype + " for" + datafile.toString)
          }
        }


      }
    })

  }
}
