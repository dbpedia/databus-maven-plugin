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

import java.nio.file.Paths

import org.apache.maven.plugin.testing.AbstractMojoTestCase

trait CommonMavenPluginTest extends AbstractMojoTestCase {

  val projectRoot = {
    val resPa = getClass.getClassLoader
      .getResource("")
      .getPath
    Paths.get(resPa)
      .resolve("../../")
      .normalize()
  }

  val configFile =
    Paths.get(
      getClass.getClassLoader
        .getResource("sampleProj/pom.xml")
        .getPath)

  val pluginBuildDir = projectRoot.resolve("target/tests")

}
