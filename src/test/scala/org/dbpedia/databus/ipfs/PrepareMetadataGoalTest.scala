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

import org.dbpedia.databus.{CommonMavenPluginTest, PrepareMetadata}

class PrepareMetadataGoalTest extends CommonMavenPluginTest {

  override def setUp(): Unit = super.setUp()

  override def tearDown(): Unit = super.tearDown()

  def testRegularPluginConfig() = {
    var mojo = new PrepareMetadata()
    val conf = extractPluginConfiguration("databus-maven-plugin", configFile.toFile)
    mojo = configureMojo(mojo, conf).asInstanceOf[PrepareMetadata]


  }

  def testIpfsPluginConfig() = {
    var mojo = new PrepareMetadata()
    val conf = extractPluginConfiguration("databus-maven-plugin", configFile.toFile)
    mojo = configureMojo(mojo, conf).asInstanceOf[PrepareMetadata]

    println(mojo)
  }

}
