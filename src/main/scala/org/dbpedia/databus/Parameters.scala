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

import java.time.format.DateTimeFormatter.ISO_DATE


trait Parameters { this: Properties =>

  lazy val params = new Parameters(this)

  class Parameters(props: Properties) {

    lazy val issuedDate = Option(props.issuedDate).map(ISO_DATE.parse)

    lazy val modifiedDate = Option(props.modifiedDate).map(ISO_DATE.parse)

    lazy val wasDerivedFrom = props.wasDerivedFrom.asScala.map(ScalaBaseEntity.fromJava).toSet

    lazy val versionToInsert = if(insertVersion) Some(version) else None
  }

}
