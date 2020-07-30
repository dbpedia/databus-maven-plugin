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
package org.dbpedia.databus.test.it

import java.nio.file.Path

import org.apache.maven.it.Verifier
import org.dbpedia.databus.DatabusPluginVersion
import org.dbpedia.databus.test.MockHttpServerOps
import org.scalatest.FunSuiteLike

/**
 * Created by .
 */
trait CommonMavenPluginIT extends MockHttpServerOps with FunSuiteLike {

  def projectPath: Path

  def initVerifier = {
    val ver = new Verifier(projectPath.toAbsolutePath.toString)
    ver.setSystemProperty("pluginVersion", DatabusPluginVersion.toString)
    ver
  }

}
