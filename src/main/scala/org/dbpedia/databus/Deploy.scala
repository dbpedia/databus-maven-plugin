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

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.nio.file.Files
import java.util.Calendar

import org.apache.jena.datatypes.xsd.XSDDatatype
import org.apache.jena.graph.Graph
import org.apache.jena.rdf.model.{Model, ModelFactory, Statement}
import org.apache.jena.riot.{Lang, RDFFormat, RDFLanguages, RDFParserBuilder, RDFWriter}
import org.apache.jena.riot.system.StreamRDFLib
import org.dbpedia.databus.lib._
import scalaj.http.Http

import scala.collection.mutable.ArrayBuffer
import scala.util.Try
import scala.collection.JavaConverters._
import org.apache.maven.plugin.{AbstractMojo, MojoExecutionException}
import org.apache.maven.plugins.annotations.{LifecyclePhase, Mojo, Parameter}
import org.dbpedia.databus.shared.authentification.AccountHelpers
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

import org.apache.jena.util.ResourceUtils


@Mojo(name = "deploy", defaultPhase = LifecyclePhase.DEPLOY, threadSafe = true)
class Deploy extends AbstractMojo with Properties with SigningHelpers {

  @Parameter(property = "databus.deployRepoURL", defaultValue = "https://databus.dbpedia.org/repo")
  val deployRepoURL: String = ""

  @Parameter(property = "databus.verifyParts", defaultValue = "false")
  val verifyParts: Boolean = false

  @Parameter(property = "databus.apiKey", defaultValue = "", required = true)
  val apiKey: String = ""

  def langToFormat(lang: Lang): RDFFormat = lang match {
    case RDFLanguages.TURTLE => RDFFormat.TURTLE_PRETTY
    case RDFLanguages.TTL => RDFFormat.TTL
    case RDFLanguages.JSONLD => RDFFormat.JSONLD_FLATTEN_PRETTY
    case RDFLanguages.TRIG => RDFFormat.TRIG_PRETTY
    case RDFLanguages.RDFXML => RDFFormat.RDFXML_PRETTY
    case RDFLanguages.NTRIPLES => RDFFormat.NTRIPLES
    case RDFLanguages.NQUADS => RDFFormat.NQUADS
    case RDFLanguages.TRIX => RDFFormat.TRIX
  }

  def readModel(data: Array[Byte], lang: Lang): Try[Model] = Try {
    val model = ModelFactory.createDefaultModel()
    val dataStream = new ByteArrayInputStream(data)
    val dest = StreamRDFLib.graph(model.getGraph)
    val parser = RDFParserBuilder.create()
      .source(dataStream)
      .base(null)
      .lang(lang)
    parser.parse(dest)
    model
  }

  def convertModel(model: Model): Model = {
    val buf = ArrayBuffer[Statement]()
    buf ++= model.listStatements().filterKeep(
      _.getObject.toString.contains("SingleFile")
    ).asScala
    buf.foreach(s => {
      val o = model.createResource("http://dataid.dbpedia.org/ns/core#Part")
      s.changeObject(o)
    })
    buf.clear()

    buf ++= model.listStatements().filterKeep(s => {
      s.getPredicate.toString.contains("title")
    }).asScala
    buf.foreach(s => {
      s.changeObject(s.getObject.asLiteral().getString)
    })
    buf.clear()

    buf ++= model.listStatements().filterKeep(s => {
      s.getPredicate.toString.contains("compression") && s.getObject.toString.contentEquals("None")
    }).asScala
    buf.foreach(s => {
      s.changeObject(s.getObject.toString.toLowerCase)
    })
    buf.clear()

    buf ++= model.listStatements().filterKeep(s => {
      s.getPredicate.toString.contains("type") &&
        s.getObject.toString.contentEquals("http://dataid.dbpedia.org/ns/core#Group")
    }).asScala
    buf.foreach(s => {
      s.remove()
    })
    buf.clear()

    buf ++= model.listStatements().filterKeep(s => {
      s.getPredicate.toString.contains("type") &&
        s.getObject.toString.contentEquals("http://dataid.dbpedia.org/ns/core#Version")
    }).asScala
    buf.foreach(s => {
      s.remove()
    })
    buf.clear()

    buf ++= model.listStatements().filterKeep(s => {
      s.getPredicate.toString.contains("type") &&
        s.getObject.toString.contentEquals("http://dataid.dbpedia.org/ns/core#Artifact")
    }).asScala
    buf.foreach(s => {
      s.remove()
    })
    buf.clear()

    buf ++= model.listStatements().filterKeep(s => {
      s.getObject.toString.contains("Dataset") && s.getPredicate.toString.contains("type")
    }).asScala
    buf.foreach(s => {
      val dsProps = model.listStatements().filterKeep(ss => {
        ss.getSubject.toString.contentEquals(s.getSubject.toString)
      }).asScala
      dsProps.find(p => p.getPredicate.toString.endsWith("comment"))
        .foreach(l =>
          s.getSubject.addProperty(
            model.createProperty("http://purl.org/dc/terms/abstract"),
            model.createLiteral(l.getObject.asLiteral().getString)
          ))
      s.getSubject.addProperty(
        model.createProperty("http://purl.org/dc/terms/modified"),
        model.createTypedLiteral(
          XSDDatatype.XSDdateTime.parse(
            Calendar.getInstance().toInstant.toString
          ),
          XSDDatatype.XSDdateTime
        )
      )
    })
    buf.clear()

    buf ++= model.listStatements().filterKeep(s => {
      s.getSubject.toString.contains("/dataid.ttl") && s.getPredicate.toString.contains("type")
    }).asScala
    buf.foreach(s => {
      ResourceUtils.renameResource(s.getSubject, s.getSubject.toString.replace("/dataid.ttl", ""))
    })
    buf.clear()

    buf ++= model.listStatements().filterKeep(s => {
      s.getPredicate.toString.contains("contentVariant")
    }).asScala
    buf.foreach(s => {
      s.remove()
    })
    buf.clear()

    val allCvs = model.listStatements().filterKeep(s =>
      s.getPredicate.toString.startsWith("http://dataid.dbpedia.org/ns/cv#")
    ).asScala.toList

    val cvsSet = allCvs.map(_.getPredicate.toString).toSet
    val cvsStats = allCvs.map(s => (s.getSubject.toString, s.getPredicate.toString))
      .groupBy(s => s._1)
      .map(p => (p._1, p._2.map(_._2)))

    buf ++= model.listStatements().filterKeep(s => {
      s.getObject.toString.contains("http://dataid.dbpedia.org/ns/core#Part")
    }).asScala

    buf.foreach(s => {
      val c = cvsStats.getOrElse(s.getSubject.toString, List.empty)
      (cvsSet -- c).foreach(cv => s.getSubject.addProperty(
        model.createProperty(cv),
        model.createLiteral("none")
      ))
    })
    buf.clear()

    model
  }

  def graphToBytes(model: Graph, outputLang: Lang): Try[Array[Byte]] = Try {
    val str = new ByteArrayOutputStream()
    val builder = RDFWriter.create.format(langToFormat(outputLang))
      .source(model)

    builder
      .build()
      .output(str)
    str.toByteArray
  }

  @throws[MojoExecutionException]
  override def execute(): Unit = {
    //skip the parent module
    if (isParent()) {
      getLog.info("skipping parent module")
      return
    }

    if (!deployRepoURL.startsWith("https://")) {
      getLog.error(s"<databus.deployRepoURL> is not https:// ${deployRepoURL}")
    }

    val datasetIdentifier = AccountHelpers.getAccountOption(publisher) match {

      case Some(account) => {

        s"${account.getURI}/${groupId}/${artifactId}/${version}"
      }

      case None => {
        downloadUrlPath
      }
    }

    getLog.info(s"Attempting upload to $deployRepoURL with verify-parts=$verifyParts into graph $datasetIdentifier")

    //TODO packageExport should do the resolution of URIs
    val dataid = if (locations.packageDataIdFile.isRegularFile && locations.packageDataIdFile.nonEmpty) {
      readModel(Files.readAllBytes(locations.packageDataIdFile.path), Lang.TTL)
        .map(convertModel)
        .flatMap(m => graphToBytes(m.getGraph, Lang.JSONLD)).get
    } else {
      getLog.warn(s"Did not find expected DataId file '${locations.packageDataIdFile.pathAsString}' from " +
        "databus:package-export goal. Uploading a DataId prepared in-memory.")
      resolveBaseForRDFFile(locations.buildDataIdFile, locations.dataIdDownloadLocation)
    }

    getLog.info(new String(dataid))
    val response = Http(deployRepoURL + "?verify-parts=" + verifyParts)
      .postData(dataid)
      .header("X-API-KEY", apiKey)
      .header("Content-Type", "application/json")
      .asString

    if (!response.is2xx) {
      getLog.error(
        s"""|FAILURE HTTP response code: ${response.code} (check https://en.wikipedia.org/wiki/HTTP_${response.code})
            |$deployRepoURL rejected ${locations.packageDataIdFile.pathAsString}
            |Message:\n${response.body}
       """.stripMargin)

      getLog.debug(s"Full ${response.toString}")
    } else {
      getLog.info("Response: " + response.body)

      val query =
        s"""PREFIX dataid: <http://dataid.dbpedia.org/ns/core#>
           |PREFIX dct: <http://purl.org/dc/terms/>
           |
           |SELECT ?name ?version ?date ?webid ?uploadtime ?account {
           |Graph <${datasetIdentifier}> {
           |  ?dataset a dataid:Dataset .
           |  ?dataset rdfs:label ?name .
           |  ?dataset dct:hasVersion ?version .
           |  ?dataset dct:issued ?date .
           |  ?dataset dataid:associatedAgent ?webid .
           |  ?dataid a dataid:DataId .
           |  ?dataid dct:issued ?uploadtime .
           |  }
           |# resides in other graph
           |OPTIONAL {?webid foaf:account ?account }
           |}
           |""".stripMargin

      val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.name())

      getLog.info(
        s"""SUCCESS: upload of DataId for artifact '$artifactId' version ${version} to $deployRepoURL succeeded
           |Data should be available within some minutes at graph ${datasetIdentifier}
           |Test at ${deployRepoURL.replace("/api/publish", "")}/sparql  with query: \n\n ${query}
           |curl "${deployRepoURL.replace("/api/publish", "")}/sparql?query=${encoded}"
           |
           |Note:
           |* To avoid denial of service attacks, we will sleep 5 minutes after your request is received, before processing it.
           |* First time account users: We cache WebIDs daily. So if your site is not shown, wait day.
       """
          .stripMargin)
    }
  }
}
