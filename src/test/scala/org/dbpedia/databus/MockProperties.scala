/*-
 * #%L
 * DBpedia Databus Maven Plugin
 * %%
 * Copyright (C) 2018 - 2020 Sebastian Hellmann (on behalf of the DBpedia Association)
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

import java.io.File
import java.util

import org.apache.maven.execution.MavenSession
import org.apache.maven.model.{Build, Model}
import org.apache.maven.model.io.xpp3.MavenXpp3Reader
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.testing.stubs.MavenProjectStub
import org.apache.maven.settings.Settings
import org.apache.maven.shared.utils.ReaderFactory


trait MockProperties extends Properties {
  this: AbstractMojo =>

  private def configData = {
    import org.apache.maven.model.io.xpp3.MavenXpp3Reader
    val pomReader = new MavenXpp3Reader
    val model = pomReader.read(ReaderFactory.newXmlReader(new File(getBasedir(), "pom.xml")))
    model
  }

  private def getBasedir() =
    new File(
      this.getClass.getClassLoader
        .getResource("sampleProj")
        .toURI
    )

}

