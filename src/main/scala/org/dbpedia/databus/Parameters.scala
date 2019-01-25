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


import org.dbpedia.databus.params.{BaseEntity => ScalaBaseEntity}

import better.files._

import scala.collection.JavaConverters._
import scala.collection.mutable

import java.net.URL
import java.time._
import java.time.format.DateTimeFormatterBuilder


trait Parameters {

  this: Properties =>

  lazy val params = new Parameters(this)

  val ISO_INSTANT_NO_NANO = new DateTimeFormatterBuilder().parseCaseInsensitive().appendInstant(0).toFormatter

  class Parameters(props: Properties) {

    def getLog = props.getLog

    val invocationTime: ZonedDateTime = ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault())

    lazy val issuedDate: ZonedDateTime =
      try {
        if (props.tryVersionAsIssuedDate) {
          val attempt = props.version.replace(".", "-") + "T00:00:00Z"
          ZonedDateTime.parse(attempt)
        } else {
          //ZonedDateTime.ofInstant(LocalDateTime.parse(props.issuedDate).toInstant(ZoneOffset.UTC), ZoneId.systemDefault())
          ZonedDateTime.parse(props.issuedDate)
        }
      } catch {
        case e: Throwable => {

          getLog.error("Error determining the issued date", e)
          invocationTime
        }
      }

    lazy val modifiedDate: ZonedDateTime = try {
      ZonedDateTime.parse(props.modifiedDate)
    } catch {
      case e: Throwable => invocationTime
    }

    //lazy val wasDerivedFrom = props.wasDerivedFrom.asScala.map(ScalaBaseEntity.fromJava).toSet

    lazy val versionToInsert = if (insertVersion) Some(version) else None

    lazy val provenanceIRIs = {
      val set: mutable.Set[URL] = mutable.Set()
      if (provenanceFileSimple.exists()) {
        for {
          line <- provenanceFileSimple.toScala.lineIterator
        } (if (line.trim.nonEmpty) {
          set.add(new URL(line.trim))
        })
      }
      set

    }

    lazy val (label, comment, description) = {
      if (!markdown.exists()) {
        ("", "", "")
      }

      val iter = markdown.toScala.lineIterator
      var firstline = ""
      var secondline = ""
      var rest = ""

      if (iter.hasNext) {
        var tmp = iter.next().trim
        if (tmp.startsWith("#")) {
          firstline = tmp.replace("#", "").trim
        }
        if (iter.hasNext) {
          secondline = iter.next().trim

          for {
            line <- iter
          } (rest += (line + "\n"))
        }
      }
      (firstline, secondline, rest.trim)
    }

  }

}


