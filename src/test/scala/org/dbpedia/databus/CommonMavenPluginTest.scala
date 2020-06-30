package org.dbpedia.databus

import java.nio.file.Paths

import org.apache.maven.plugin.testing.AbstractMojoTestCase

trait CommonMavenPluginTest extends AbstractMojoTestCase {

  val projectRoot = {
    val resPa = getClass.getClassLoader
      .getResource("")
      .getPath
    Paths.get(resPa)
      .resolve("../../")
      .normalize()
  }

  val configFile =
    Paths.get(
      getClass.getClassLoader
        .getResource("sample-pom.xml")
        .getPath)

  val pluginBuildDir = projectRoot.resolve("target/tests")

}
