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
package org.dbpedia.databus.test.it.ipfs

import java.nio.file.Path

import org.dbpedia.databus.test.CommonMavenPluginTest
import org.dbpedia.databus.test.it.CommonMavenPluginIT
import org.scalatest.BeforeAndAfterAll
import org.testcontainers.containers.{BindMode, GenericContainer}
import org.testcontainers.utility.DockerImageName

import scala.collection.JavaConverters._


class IpfsIt extends CommonMavenPluginIT with BeforeAndAfterAll {

  val projResourcePath = "it/ipfs-test"

  lazy val ipfsContainer: GenericContainer[_] =
    new GenericContainer(new DockerImageName("ipfs/go-ipfs:v0.6.0").toString)
      .withClasspathResourceMapping(
        projResourcePath.toString,
        "/export/sample",
        BindMode.READ_ONLY
      )

  override def beforeAll(): Unit = {
    super.beforeAll()
    ipfsContainer.start()
    mockHttpServer
  }

  test("plugin_works_with_ipfs_it"){
    val name = ipfsContainer.getContainerInfo.getName
    val ver = initVerifier
    ver.setSystemProperty("cn", name)
    ver.executeGoals(Seq("package", "deploy").asJava)
    ver.verifyTextInLog("SUCCESS: upload of DataId for artifact 'test-set' version 2020.06.05 ")
    ver.verifyTextInLog("Successfully added files from /export/sample/test-set/2020.06.05:")
    ver.verifyTextInLog("BUILD SUCCESS")
  }

  override def projectPath: Path = CommonMavenPluginTest.projectFolder(projResourcePath)

  override def afterAll(): Unit = {
    ipfsContainer.stop()
    mockHttpServer.stop()
    super.afterAll()
  }

}
