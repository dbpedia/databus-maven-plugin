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

import java.time._
import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}

import scala.util.Try


trait Parameters {
  this: Properties =>

  lazy val params = new Parameters(this)

  val ISO_INSTANT_NO_NANO: DateTimeFormatter = new DateTimeFormatterBuilder().parseCaseInsensitive().appendInstant(0).toFormatter

  class Parameters(props: Properties) {

    def getLog = props.getLog

    val invocationTime: ZonedDateTime = ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault())

    lazy val issuedDate: ZonedDateTime =
      try {

        if (props.tryVersionAsIssuedDate) {
          val attempt = props.version.replace(".", "-") + "T00:00:00Z"
          ZonedDateTime.parse(attempt)
        } else if (props.issuedDate == null) {
          getLog.info {
            s"<databus.issuedDate> is null, using invocation time: ${invocationTime}"
          }
          invocationTime
        } else {
          //ZonedDateTime.ofInstant(LocalDateTime.parse(props.issuedDate).toInstant(ZoneOffset.UTC), ZoneId.systemDefault())
          ZonedDateTime.parse(props.issuedDate)
        }
      } catch {
        case e: Throwable => {

          getLog.error("Error determining the issued date, using invocation time, issued data should be ", e)
          invocationTime
        }
      }

    lazy val modifiedDate: ZonedDateTime =
      Try(ZonedDateTime.parse(props.modifiedDate))
        .getOrElse(invocationTime)

    lazy val versionToInsert = if (insertVersion) Some(version) else None


    var (label, comment, description) = {
      if (!locations.inputMarkdownFile.exists()) {
        ("", "", "")
      }

      getLog.info(s"Loading docu from: ${locations.prettyPath(locations.inputMarkdownFile)}")

      val iter = locations.inputMarkdownFile.lineIterator
      var firstline = ""
      var secondline = ""
      var rest = ""


      if (iter.hasNext) {
        var tmp = iter.next().trim
        if (tmp.startsWith("#")) {
          firstline = tmp.replace("#", "")
        }
        if (iter.hasNext) {
          secondline = iter.next()

          for {
            line <- iter
          } (rest += (line + "\n"))
        }
      }
      (firstline.trim, secondline.trim, rest.trim)
    }

    // called in prepareMetadata
    def addLabelCommentPrefixes ():Unit ={
      label = labelPrefix+label
      comment =commentPrefix+comment
    }

    def validateMarkdown(): Unit = {

      var valmsg = ""
      val markdown = locations.inputMarkdownFile

      getLog.info(
        s"""
           |#################################################
           |Documention check of ${locations.prettyPath(locations.inputMarkdownFile)}
           |#################################################""".stripMargin)

      val f1 = s"* Create a markdown file with the same name of the artifact: ${markdown.name}\n"
      val f2 = s"* First line must be '# Title of all dataset versions' (abstract identity, used as rdfs:label and dct:title)\n"
      val f3 = s"* Second line should give a good one liner what can be expected (used as rdfs:comment)\n"
      val f4 = s"* Third line until end is regular markdown with the details (use ##, ###, #### header levels, used as dct:description)"
      val examples =
        s"""
           |Example 1:
           |# Collected data about animals
           |Contains basic information about animals collected by x using method y
           |
           |## Collection method
           |Detailed description in markdown as long as you want
         """.stripMargin

      if (!markdown.isRegularFile()) {
        getLog.error(s"No markdown file found at ${markdown}\n" +
          s"fix with:\n" + f1 + f2 + f3 + f4 + examples)
        System.exit(-1)
      }

      if (params.label.isEmpty) {
        getLog.error(s"label found in ${markdown.name} is empty '${params.label}'\n" +
          s"fix with:\n" + f2 + f3 + f4)
        System.exit(-1)
      }

      if(params.label.toLowerCase.contains("dataset") || params.label.toLowerCase.contains(subpathGroup)) {
        getLog.warn(s"Not recommended to include 'dataset' or groupId '$subpathGroup' in rdfs:label (first line of ${markdown.name}): ${params.label}")
      }
      if(params.label.length>40){
        getLog.warn(s"label (${params.label.length}) too long: ${params.label}")
      }


      if (params.comment.isEmpty) {
        getLog.error(s"comment found in ${markdown.name} is empty '${params.comment}'\n" +
          s"fix with:\n" + f3 + f4)
        System.exit(-1)
      }

      if (params.description.isEmpty) {
        getLog.warn(s"Empty description in ${markdown.name}\n " +
          s"fix with\n" + f4 +
          s"* This is lazy, but forgivable, continuing operation"
        )
      }

      if(params.description.length>2500) {
        getLog.info("\uD83D\uDC4D description longer than 2500 chars. Good job")
      }


      getLog.info(
        s"""
           |Documentation check finished, docu will look like:
           |rdfs:label(${params.label.length}) "${params.label}"
           |rdfs:comment(${params.comment.length}) "${params.comment}"
           |dct:description(${params.description.length}) "${params.description}"
         """.stripMargin)

    }

  }

}


