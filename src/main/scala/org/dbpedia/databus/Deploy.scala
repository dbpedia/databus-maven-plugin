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

import org.dbpedia.databus.shared._

import better.files._
import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.apache.jena.riot.RDFLanguages
import org.apache.maven.plugin.{AbstractMojo, MojoExecutionException}
import org.apache.maven.plugins.annotations.{LifecyclePhase, Mojo}
import org.scalactic.Requirements._
import org.scalactic.TypeCheckedTripleEquals._
import resource._


// not sure if needed
//import org.apache.http.client.methods.HttpGet
//import org.scalatra.test.scalatest.ScalatraFlatSpec

@Mojo(name = "deploy", defaultPhase = LifecyclePhase.DEPLOY)
class Deploy extends AbstractMojo with Properties {


  @throws[MojoExecutionException]
  override def execute(): Unit = {
    //skip the parent module
    if(isParent()) {
      getLog.info("skipping parent module")
      return
    }

    val dataIdWithResolvedIRIsPath = (getPackageDirectory.toScala / getDataIdFile().getName)

    val downloadLocation = downloadUrlPath.toString + getDataIdFile.getName

    // resolving relative URIs into downloadURLs
    def dataIdSource = getDataIdFile().toScala.newInputStream

    def dataIdSink = dataIdWithResolvedIRIsPath.newOutputStream

    (managed(dataIdSource) and managed(dataIdSink)) apply { case (inStream, outStream) =>

      val dataIdModel: Model = ModelFactory.createDefaultModel
      dataIdModel.read(inStream, downloadLocation, RDFLanguages.strLangTurtle)
      dataIdModel.write(outStream, RDFLanguages.strLangTurtle)
    }

    val pkcs12FileResolved = lib.findFileMaybeInParent(pkcs12File.toScala)

    val response = DataIdUpload.upload("https://databus.dbpedia.org/repo/dataid/upload",
      dataIdWithResolvedIRIsPath, pkcs12FileResolved, downloadLocation, allowOverwriteOnDeploy)

    requireState(response.code === 200,
      s"""
         |The repository service refused to upload the DataId document ${dataIdWithResolvedIRIsPath.pathAsString}:
         |HTTP response code: ${response.code}
         |message from service:\n${response.body}
       """.stripMargin)
  }
}
