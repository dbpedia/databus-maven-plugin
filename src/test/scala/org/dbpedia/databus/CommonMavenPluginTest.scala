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
import java.nio.file.{Path, Paths}
import java.util

import org.apache.maven.execution.MavenSession
import org.apache.maven.model.Build
import org.apache.maven.model.io.xpp3.MavenXpp3Reader
import org.apache.maven.plugin.testing.AbstractMojoTestCase
import org.apache.maven.plugin.testing.stubs.MavenProjectStub
import org.apache.maven.shared.utils.ReaderFactory

abstract class CommonMavenPluginTest extends AbstractMojoTestCase {

  def initMojo[T <: DatabusMojo](projectName: String, goal: String): T = {
    val mj: DatabusMojo =
      lookupConfiguredMojo(initSession(projectName), newMojoExecution(goal))
        .asInstanceOf[T]
    configureMojo(
      mj,
      "databus-maven-plugin",
      new File(mj.proj.getBasedir, "pom.xml"))
      .asInstanceOf[T]
  }

  def initSession(projectName: String): MavenSession = {
    val proj = new TestProjectStub(CommonMavenPluginTest.projectFolder(projectName))
    val session = newMavenSession(proj)
    session.getRequest.setBaseDirectory(proj.getBasedir())
    session
  }

}

object CommonMavenPluginTest {

  def projectFolder(name: String): Path =
    Paths.get(
      getClass.getClassLoader
        .getResource(name)
        .getPath)

}


class TestProjectStub(projectFolder: Path) extends MavenProjectStub {

  val pomReader = new MavenXpp3Reader();
  val model = pomReader.read(ReaderFactory.newXmlReader(new File(getBasedir(), "pom.xml")));
  setModel(model)
  setGroupId(model.getGroupId)
  setArtifactId(model.getArtifactId)
  setVersion(model.getVersion)
  setName(model.getName)
  setUrl(model.getUrl)
  setPackaging(model.getPackaging)

  val build = new Build();
  build.setFinalName(model.getArtifactId);
  build.setDirectory(getBasedir() + "/target");
  build.setSourceDirectory(getBasedir() + "/src/main/java");
  build.setOutputDirectory(getBasedir() + "/target/classes");
  build.setTestSourceDirectory(getBasedir() + "/src/test/java");
  build.setTestOutputDirectory(getBasedir() + "/target/test-classes");
  setBuild(build);

  val compileSourceRoots = new util.ArrayList[String]();
  compileSourceRoots.add(getBasedir() + "/src/main/java");
  setCompileSourceRoots(compileSourceRoots);

  val testCompileSourceRoots = new util.ArrayList[String]();
  testCompileSourceRoots.add(getBasedir() + "/src/test/java");
  setTestCompileSourceRoots(testCompileSourceRoots);

  /** {@inheritDoc } */
  override def getBasedir() =
    projectFolder.toFile;
}
