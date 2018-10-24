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
import org.dbpedia.databus.lib._

import better.files._
import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.apache.jena.riot.RDFLanguages
import org.apache.maven.plugin.{AbstractMojo, MojoExecutionException}
import org.apache.maven.plugins.annotations.{LifecyclePhase, Mojo}
import org.scalactic.Requirements._
import org.scalactic.TypeCheckedTripleEquals._
import resource._

import java.io.ByteArrayOutputStream


@Mojo(name = "deploy", defaultPhase = LifecyclePhase.DEPLOY)
class Deploy extends AbstractMojo with Properties {

  @throws[MojoExecutionException]
  override def execute(): Unit = {
    //skip the parent module
    if(isParent()) {
      getLog.info("skipping parent module")
      return
    }

    val repoPathSegement = if(deployToTestRepo) "testrepo" else "repo"

    val uploadEndpointIRI = s"https://databus.dbpedia.org/$repoPathSegement/dataid/upload"

    val pkcs12FileResolved = lib.findFileMaybeInParent(pkcs12File.toScala)

    val response = if(dataIdPackageTarget.isRegularFile && dataIdPackageTarget.nonEmpty) {

      // if there is a (base-resolved) DataId Turtle file in the package directory, attempt to upload that one
      DataIdUpload.upload(uploadEndpointIRI, dataIdPackageTarget, pkcs12FileResolved,
        dataIdDownloadLocation, allowOverwriteOnDeploy)
    } else {

      //else resolve the base in-memory and upload that
      val baseResolvedDataId = resolveBaseForRDFFile(dataIdFile, dataIdDownloadLocation)

      getLog.warn(s"Did not find expected DataId file '${dataIdPackageTarget.pathAsString}' from " +
        "databus:package-export goal. Uploading a DataId prepared in-memory.")

      DataIdUpload.upload(uploadEndpointIRI, baseResolvedDataId, pkcs12FileResolved,
        dataIdDownloadLocation, allowOverwriteOnDeploy)
    }

    requireState(response.code === 200,
      s"""
         |The repository service refused to upload the DataId document ${dataIdFile.pathAsString}:
         |HTTP response code: ${response.code}
         |message from service:\n${response.body}
       """.stripMargin)

    getLog.info(s"upload of DataId succeeded for artifact '$artifactId'")
  }
}
