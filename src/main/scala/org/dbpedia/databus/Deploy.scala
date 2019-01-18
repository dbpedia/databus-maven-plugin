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

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

import org.dbpedia.databus.lib._
import org.dbpedia.databus.shared._
import org.dbpedia.databus.shared.helpers.conversions._
import org.dbpedia.databus.shared.rdf.conversions._
import org.apache.maven.plugin.{AbstractMojo, MojoExecutionException}
import org.apache.maven.plugins.annotations.{LifecyclePhase, Mojo}
import org.dbpedia.databus.shared.authentification.AccountHelpers
import org.scalactic.Requirements._
import org.scalactic.TypeCheckedTripleEquals._
import java.net.URLEncoder



@Mojo(name = "deploy", defaultPhase = LifecyclePhase.DEPLOY)
class Deploy extends AbstractMojo with Properties with SigningHelpers {

  @throws[MojoExecutionException]
  override def execute(): Unit = {
    //skip the parent module
    if(isParent()) {
      getLog.info("skipping parent module")
      return
    }

    //val repoPathSegement = if(deployToTestRepo) "testrepo" else "repo"

    if(!deployRepoURL.startsWith("https://")){
      getLog.error(s"<databus.deployRepoURL> is not https:// ${deployRepoURL}")
    }

    val uploadEndpointIRI = s"$deployRepoURL/dataid/upload"

    val datasetIdentifier = AccountHelpers.getAccountOption(publisher) match {

      case Some(account) => {

        s"${account.getURI}/${groupId}/${artifactId}/${version}"
      }

      case None => {
        dataIdDownloadLocation
      }
    }


    getLog.info(s"Attemtpting upload to ${uploadEndpointIRI} with allowOverrideOnDeploy=${allowOverwriteOnDeploy} into graph ${datasetIdentifier}" )

    //TODO packageExport should do the resolution of URIs
    val response = if(dataIdPackageTarget.isRegularFile && dataIdPackageTarget.nonEmpty) {

      // if there is a (base-resolved) DataId Turtle file in the package directory, attempt to upload that one
      DataIdUpload.upload(uploadEndpointIRI, dataIdPackageTarget, locations.pkcs12File, pkcs12Password.get,
        dataIdDownloadLocation, allowOverwriteOnDeploy, datasetIdentifier)
    } else {

      getLog.warn(s"Did not find expected DataId file '${dataIdPackageTarget.pathAsString}' from " +
        "databus:package-export goal. Uploading a DataId prepared in-memory.")

      //else resolve the base in-memory and upload that
      val baseResolvedDataId = resolveBaseForRDFFile(dataIdFile, dataIdDownloadLocation)

      DataIdUpload.upload(uploadEndpointIRI, baseResolvedDataId, locations.pkcs12File, pkcs12Password.get,
        dataIdDownloadLocation, allowOverwriteOnDeploy, datasetIdentifier)
    }

    requireState(response.code === 200,
      s"""
         |The repository service refused to upload the DataId document ${dataIdFile.pathAsString}:
         |HTTP response code: ${response.code}
         |message from service:\n${response.body}
       """.stripMargin)

    val query = s"SELECT * {Graph <${datasetIdentifier}> {?s ?p ?o}} Limit 5"
    val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.name())
    getLog.info(
      s"""SUCCESS: upload of DataId for artifact '$artifactId' version ${version} to $deployRepoURL succeeded
         |Data should be available within some minutes at graph ${datasetIdentifier}
         |Test at ${deployRepoURL}/sparql  with query: ${query}
         |curl "${deployRepoURL}/sparql?query=${encoded}"
       """.stripMargin)
  }
}
