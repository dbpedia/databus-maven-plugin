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

import org.dbpedia.databus.{CommonMavenPluginTest, MockProperties, PrepareMetadata}

import scala.util.Random

class PrepareMetadataGoalTest extends CommonMavenPluginTest {

  override def setUp(): Unit = super.setUp()

  override def tearDown(): Unit = super.tearDown()

  def testIpfsPluginConfig() = {
    val mojo = lookupMojo("metadata", configFile.toFile)
      .asInstanceOf[PrepareMetadata]


    assert(mojo.ipfsSettings != null)
    assert(mojo.downloadUrlPath.toString.equals("http://pa"))
  }

}