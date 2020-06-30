package org.dbpedia.databus.ipfs

import org.dbpedia.databus.{CommonMavenPluginTest, PrepareMetadata}

class IpfsPluginTest extends CommonMavenPluginTest {

  override def setUp(): Unit = super.setUp()

  override def tearDown(): Unit = super.tearDown()

  def testIpfsPluginGoal() = {
    var mojo = new PrepareMetadata()
    val conf = extractPluginConfiguration("databus-maven-plugin", configFile.toFile)
    mojo = configureMojo(mojo, conf).asInstanceOf[PrepareMetadata]
    mojo.execute()
  }

}
