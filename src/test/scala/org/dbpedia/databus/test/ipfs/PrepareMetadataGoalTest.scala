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
package org.dbpedia.databus.test.ipfs

import java.nio.file.{Files, Path}

import org.dbpedia.databus.PrepareMetadata
import org.dbpedia.databus.ipfs.IpfsCliClient.Chunker
import org.dbpedia.databus.ipfs.IpfsClientOps
import org.dbpedia.databus.test.CommonMavenPluginTest
import org.mockito.ArgumentMatchers

import scala.collection.JavaConverters._
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._


class PrepareMetadataGoalTest extends CommonMavenPluginTest {

  override def setUp(): Unit = {
    super.setUp()
  }

  override def tearDown(): Unit = super.tearDown()

  def testIpfsParentProj(): Unit = {
    val mojo = initMojo[PrepareMetadata]("sampleProj", "metadata")
    val client = mock[IpfsClientOps]
    mojo.setIpfsClient(client)
    val log = interceptLogs(mojo)

    assert(mojo.isParent())
    mojo.execute()
    val re = log.logs.asScala.exists(m => m.message.exists(_.contains("skipping parent testArtifact")))
    assert(re)
    verify(client, times(0)).add(any[Path], any[Chunker], anyBoolean, anyBoolean, anyBoolean, anyBoolean, ArgumentMatchers.eq(true))
  }

  def testIpfsDataProj(): Unit = {
    val mojo = initMojo[PrepareMetadata]("sampleProj/test-set-meta", "metadata")
    val client = mock[IpfsClientOps]
    val hashVal = "dummy_hash"
    when(client.add(any[Path], any[Chunker], anyBoolean, anyBoolean, anyBoolean, anyBoolean, anyBoolean))
      .thenReturn(Seq("some_hash", hashVal))
    mojo.setIpfsClient(client)
    val log = interceptLogs(mojo)

    assert(!mojo.isParent())
    mojo.execute()
    val re = log.logs.asScala
      .exists(m => m.message.exists(_.contains("Found 1 files:")))
    assert(re)
    val lns = Files.readAllLines(mojo.locations.buildDataIdFile.toJava.toPath).asScala
    assert(lns.exists(_.contains(s"dcat:downloadURL         <https://ipfs.io/ipfs/$hashVal")))
    verify(client, times(1)).add(any[Path], any[Chunker], anyBoolean, anyBoolean, anyBoolean, anyBoolean, ArgumentMatchers.eq(true))
  }

}