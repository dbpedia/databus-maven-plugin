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

import org.apache.maven.it.Verifier
import org.apache.maven.shared.utils.io.FileUtils
import org.scalatest.FunSuite


trait CommonMavenIntegrationTest extends FunSuite {

  def verifier = {
    // path to the sample project
    val resPa = getClass.getClassLoader.getResource("").getPath
    val projectRoot = Paths.get(resPa).resolve("../../").normalize()
    val pathToSample = "example/animals"

    val tempDir = projectRoot.resolve("target/databus-plugin-test-sample")
    FileUtils.deleteDirectory(tempDir.toFile)

    val sampleMavenProjectPath = projectRoot.resolve(pathToSample)
    FileUtils.copyDirectoryStructure(sampleMavenProjectPath.toFile, tempDir.toFile)

    new Verifier(tempDir.toString)
  }

}
