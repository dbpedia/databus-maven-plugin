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

import org.dbpedia.databus.lib._
import org.dbpedia.databus.shared._
//import org.dbpedia.databus.shared.authentification.AccountHelpers

import org.apache.maven.plugin.{AbstractMojo, MojoExecutionException}
import org.apache.maven.plugins.annotations.{LifecyclePhase, Mojo, Parameter}
import org.dbpedia.databus.shared.authentification.AccountHelpers

import java.net.URLEncoder
import java.nio.charset.StandardCharsets


@Mojo(name = "deploy", defaultPhase = LifecyclePhase.DEPLOY, threadSafe = true)
class Deploy extends AbstractMojo with Properties with SigningHelpers {

  @Parameter(property = "databus.deployRepoURL", defaultValue = "https://databus.dbpedia.org/repo")
  val deployRepoURL: String = ""

  @Parameter(property = "databus.allowOverwriteOnDeploy", defaultValue = "true")
  val allowOverwriteOnDeploy: Boolean = true

  @throws[MojoExecutionException]
  override def execute(): Unit = {
    //skip the parent module
    if (isParent()) {
      getLog.info("skipping parent module")
      return
    }

    //val repoPathSegement = if(deployToTestRepo) "testrepo" else "repo"

    if (!deployRepoURL.startsWith("https://")) {
      getLog.error(s"<databus.deployRepoURL> is not https:// ${deployRepoURL}")
    }

    val uploadEndpointIRI = s"$deployRepoURL/dataid/upload"

    val datasetIdentifier = AccountHelpers.getAccountOption(publisher) match {

      case Some(account) => {

        s"${account.getURI}/${groupId}/${artifactId}/${version}"
      }

      case None => {
        locations.dataIdDownloadLocation
      }
    }


    getLog.info(s"Attempting upload to ${uploadEndpointIRI} with allowOverrideOnDeploy=${allowOverwriteOnDeploy} into graph ${datasetIdentifier}")

    //TODO packageExport should do the resolution of URIs
    val response = if (locations.packageDataIdFile.isRegularFile && locations.packageDataIdFile.nonEmpty) {

      // if there is a (base-resolved) DataId Turtle file in the package directory, attempt to upload that one
      DataIdUpload.upload(uploadEndpointIRI, locations.packageDataIdFile, locations.pkcs12File, pkcs12Password.get,
        locations.dataIdDownloadLocation, allowOverwriteOnDeploy, datasetIdentifier)
    } else {

      getLog.warn(s"Did not find expected DataId file '${locations.packageDataIdFile.pathAsString}' from " +
        "databus:package-export goal. Uploading a DataId prepared in-memory.")

      //else resolve the base in-memory and upload that
      val baseResolvedDataId = resolveBaseForRDFFile(locations.buildDataIdFile, locations.dataIdDownloadLocation)

      DataIdUpload.upload(uploadEndpointIRI, baseResolvedDataId, locations.pkcs12File, pkcs12Password.get,
        locations.dataIdDownloadLocation, allowOverwriteOnDeploy, datasetIdentifier)
    }


    if (response.code != 200) {
      getLog.error(
        s"""|FAILURE HTTP response code: ${response.code} (check https://en.wikipedia.org/wiki/HTTP_${response.code})
            |$deployRepoURL rejected ${locations.packageDataIdFile.pathAsString}
            |Message:\n${response.body}
       """.stripMargin)

      getLog.debug(s"Full ${response.toString}")

      System.exit(-1)
    }


    val query =
      s"""PREFIX dataid: <http://dataid.dbpedia.org/ns/core#>
         |PREFIX dct: <http://purl.org/dc/terms/>
         |
         |SELECT ?name ?version ?date ?webid ?issuedate ?account {
         |Graph <${datasetIdentifier}> {
         |  ?dataset a dataid:Dataset .
         |  ?dataset rdfs:label ?name .
         |  ?dataset dct:hasVersion ?version .
         |  ?dataset dct:issued ?date .
         |  ?dataset dataid:associatedAgent ?webid .
         |  ?dataid a dataid:DataId .
         |  ?dataid dct:issued ?issuedate .
         |  }
         |# resides in other graph
         |OPTIONAL {?webid foaf:account ?account }
         |}
         |""".stripMargin

    val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.name())

    getLog.info(
      s"""SUCCESS: upload of DataId for artifact '$artifactId' version ${version} to $deployRepoURL succeeded
         |Data should be available within some minutes at graph ${datasetIdentifier}
         |Test at ${deployRepoURL}/sparql  with query: \n\n ${query}
         |curl "${deployRepoURL}/sparql?query=${encoded}"
       """.stripMargin)
  }
}
