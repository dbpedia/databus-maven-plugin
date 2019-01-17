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

import scala.collection.JavaConverters._
import java.time.format.DateTimeFormatter.ISO_INSTANT
import java.time.temporal.TemporalAccessor
import java.time.{Instant,  ZoneId, ZonedDateTime}


trait Parameters {
  this: Properties =>

  lazy val params = new Parameters(this)

  class Parameters(props: Properties) {

    val invocationTime = ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault())

    lazy val issuedDate: TemporalAccessor = try {
      ZonedDateTime.from(ISO_INSTANT.parse(props.issuedDate))
    } catch {
      case e: Throwable => {
        invocationTime
      }
    }

    lazy val modifiedDate: TemporalAccessor = try {
      ZonedDateTime.from(ISO_INSTANT.parse(props.modifiedDate))
    } catch {
      case e: Throwable => invocationTime
    }

    lazy val wasDerivedFrom = props.wasDerivedFrom.asScala.map(ScalaBaseEntity.fromJava).toSet

    lazy val versionToInsert = if (insertVersion) Some(version) else None
  }

}


