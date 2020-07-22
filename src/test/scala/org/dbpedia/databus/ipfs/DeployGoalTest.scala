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
package org.dbpedia.databus.ipfs

import java.nio.file.Path

import org.dbpedia.databus.ipfs.IpfsCliClient.Chunker
import org.dbpedia.databus.{CommonMavenPluginTest, Deploy, MockHttpServerOps, PrepareMetadata}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.{any, anyBoolean}
import org.mockito.Mockito.{times, verify, when}

import scala.collection.JavaConverters._

class DeployGoalTest extends CommonMavenPluginTest with MockHttpServerOps {

  override def setUp(): Unit = {
    super.setUp()
    mockHttpServer
  }

  override def tearDown(): Unit = {
    mockHttpServer.stop()
    super.tearDown()
  }

  def testDeploy(): Unit = {
    val mojo = initMojo[Deploy]("sampleProj", "deploy")
    val client = mock[IpfsClientOps]
    mojo.setIpfsClient(client)
    val log = interceptLogs(mojo)

    assert(mojo.isParent())
    mojo.execute()
    val re = log.logs.asScala.exists(m => m.message.exists(_.contains("skipping parent module")))
    assert(re)
    verify(client, times(0))
      .add(any[Path], any[Chunker], anyBoolean, anyBoolean, anyBoolean, anyBoolean, anyBoolean)
  }

  def testIpfsDataProj(): Unit = {
    val client = mock[IpfsClientOps]
    val hashVal = "dummy_hash"
    when(client.add(any[Path], any[Chunker], anyBoolean, anyBoolean, anyBoolean, anyBoolean, anyBoolean))
      .thenReturn(Seq("some_hash", hashVal))

    val preMojo = initMojo[PrepareMetadata](
      "sampleProj/test-set-deploy",
      "metadata",
      Seq("deployRepoURL"))
    preMojo.setIpfsClient(client)
    preMojo.execute()

    val mojo = initMojo[Deploy]("sampleProj/test-set-deploy", "deploy")
    mojo.setIpfsClient(client)
    val log = interceptLogs(mojo)

    assert(!mojo.isParent())
    mojo.execute()
    val re = log.logs.asScala
      .exists(m => m.message.exists(_.contains("SUCCESS: upload of DataId for artifact 'test-set' version 2020.06.05 to http://localhost:8081/ succeeded")))
    assert(re)
    val re2 = log.logs.asScala
      .exists(m => m.message.exists(_.contains("Found 1 files:")))
    assert(re2)
    verify(client, times(1))
      .add(any[Path], any[Chunker], anyBoolean, anyBoolean, anyBoolean, anyBoolean, ArgumentMatchers.eq(false))
  }

}
