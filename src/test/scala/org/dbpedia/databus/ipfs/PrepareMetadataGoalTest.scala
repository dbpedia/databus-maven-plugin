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

import org.dbpedia.databus.{CommonMavenPluginTest, PrepareMetadata}

import scala.collection.JavaConverters._
import scala.util.Random


class PrepareMetadataGoalTest extends CommonMavenPluginTest {

  override def setUp(): Unit = {
    super.setUp()
  }

  override def tearDown(): Unit = super.tearDown()

  def testIpfsParentProj(): Unit = {
    val mojo = initMojo[PrepareMetadata]("sampleProj", "metadata")
    mojo.setIpfsClient(DummyIpfsClient)
    val log = interceptLogs(mojo)

    assert(mojo.isParent())
    mojo.execute()
    val re = log.logs.asScala.exists(m => m.message.exists(_.contains("skipping parent testArtifact")))
    assert(re)
  }

  def testIpfsDataProj(): Unit = {
    val mojo = initMojo[PrepareMetadata]("sampleProj/test-set", "metadata")
    mojo.setIpfsClient(DummyIpfsClient)
    val log = interceptLogs(mojo)

    assert(!mojo.isParent())
    mojo.execute()
  }


}

object DummyIpfsClient extends IpfsClientOps {
  override def add(fn: Path, chunker: IpfsCliClient.Chunker, nocopy: Boolean, recursive: Boolean, wrapWithDir: Boolean, addHiddenFiles: Boolean, onlyHash: Boolean): Seq[String] =
    randomStrings(10, 1)

  override def dagGet(hash: String): Seq[IpfsCliClient.DagMeta] = ???

  override def objectLinks(hash: String): Seq[String] = ???

  private def randomStrings(length: Int, number: Int) =
    Random.alphanumeric.take(length * number)
      .sliding(length, length)
      .map(_.foldLeft("")(_ + _))
      .toSeq

}