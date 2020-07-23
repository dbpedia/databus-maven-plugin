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
import java.util.concurrent.CopyOnWriteArrayList

import org.apache.maven.execution.MavenSession
import org.apache.maven.model.Build
import org.apache.maven.model.io.xpp3.MavenXpp3Reader
import org.apache.maven.plugin.logging.Log
import org.apache.maven.plugin.testing.AbstractMojoTestCase
import org.apache.maven.plugin.testing.stubs.MavenProjectStub
import org.apache.maven.shared.utils.ReaderFactory
import org.codehaus.plexus.configuration.DefaultPlexusConfiguration
import org.dbpedia.databus.LogInterceptor.{Debug, Err, Info, LogMessage, Warn}
import org.mockserver.integration.ClientAndServer
import org.scalatest.mockito.MockitoSugar
import org.mockserver.integration.ClientAndServer._
import org.mockserver.model.HttpRequest._
import org.mockserver.model.HttpResponse._


abstract class CommonMavenPluginTest extends AbstractMojoTestCase with MockitoSugar {

  def initMojo[T <: DatabusMojo](projectRelativePath: String, goal: String, ignoreProps: Seq[String] = Seq.empty): T = {
    val session = initSession(projectRelativePath)
    val mj: DatabusMojo =
      lookupConfiguredMojo(session, newMojoExecution(goal))
        .asInstanceOf[T]
    val artifactId = "databus-maven-plugin"

    val config = extractPluginConfiguration(
      artifactId,
      new File(mj.session.getCurrentProject.getBasedir, "pom.xml")
    )

    val newConf = new DefaultPlexusConfiguration(config.getName)
    config.getChildren
      .filter(p => !ignoreProps.contains(p.getName))
      .foreach(p => newConf.addChild(p))

    val cmj = configureMojo(mj, newConf)
    cmj.asInstanceOf[T]
  }

  def interceptLogs(mj: DatabusMojo): LogInterceptor = {
    val log = new LogInterceptor(mj.getLog)
    mj.setLog(log)
    log
  }

  def initSession(projectRelativePath: String): MavenSession = {
    val proj = new TestProjectStub(CommonMavenPluginTest.projectFolder(projectRelativePath))
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

trait MockHttpServerOps {

  lazy val mockHttpServer: ClientAndServer = {
    val cs = startClientAndServer(8081)
    cs.when(request()
        .withMethod("POST"))
      .respond(
        response()
          .withStatusCode(200)
          .withReasonPhrase("Okk")
      )
    cs
  }

}

class LogInterceptor(log: Log) extends Log {
  val logs = new CopyOnWriteArrayList[LogMessage]()

  override def isDebugEnabled: Boolean = log.isDebugEnabled

  override def debug(content: CharSequence): Unit = {
    log.debug(content)
    logs.add(LogMessage(Some(content.toString), None, Debug))
  }

  override def debug(content: CharSequence, error: Throwable): Unit = {
    log.debug(content, error)
    logs.add(LogMessage(Some(content.toString), Some(error), Debug))
  }

  override def debug(error: Throwable): Unit = {
    log.debug(error)
    logs.add(LogMessage(None, Some(error), Debug))
  }

  override def isInfoEnabled: Boolean = log.isInfoEnabled

  override def info(content: CharSequence): Unit = {
    log.info(content)
    logs.add(LogMessage(Some(content.toString), None, Info))
  }

  override def info(content: CharSequence, error: Throwable): Unit = {
    log.info(content, error)
    logs.add(LogMessage(Some(content.toString), Some(error), Info))
  }

  override def info(error: Throwable): Unit = {
    log.info(error)
    logs.add(LogMessage(None, Some(error), Info))
  }

  override def isWarnEnabled: Boolean = log.isWarnEnabled

  override def warn(content: CharSequence): Unit = {
    log.warn(content)
    logs.add(LogMessage(Some(content.toString), None, Warn))
  }

  override def warn(content: CharSequence, error: Throwable): Unit = {
    log.warn(content, error)
    logs.add(LogMessage(Some(content.toString), Some(error), Warn))
  }

  override def warn(error: Throwable): Unit = {
    log.warn(error)
    logs.add(LogMessage(None, Some(error), Warn))
  }

  override def isErrorEnabled: Boolean = log.isErrorEnabled

  override def error(content: CharSequence): Unit = {
    log.error(content)
    logs.add(LogMessage(Some(content.toString), None, Err))
  }

  override def error(content: CharSequence, error: Throwable): Unit = {
    log.error(content, error)
    logs.add(LogMessage(Some(content.toString), Some(error), Err))
  }

  override def error(error: Throwable): Unit = {
    log.error(error)
    logs.add(LogMessage(None, Some(error), Err))
  }
}

object LogInterceptor {

  trait LogLevel

  object Info extends LogLevel

  object Err extends LogLevel

  object Warn extends LogLevel

  object Debug extends LogLevel

  case class LogMessage(message: Option[String], error: Option[Throwable], level: LogLevel)

}

class TestProjectStub(baseFolder: Path) extends MavenProjectStub {

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
    baseFolder.toFile;
}
