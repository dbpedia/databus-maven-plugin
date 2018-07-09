package org.dbpedia.databus

import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugins.annotations.{LifecyclePhase, Mojo, Parameter}


/**
  * Validate setup and resources
  *
  * WebID
  * - dereference and download
  * - get the public key from the webid
  * - get the private key from the config, generate a public key and compare to the public key
  *
  */
@Mojo(name = "validate", defaultPhase = LifecyclePhase.VALIDATE)
class Validate extends AbstractMojo {

  @Parameter var maintainer:String = _

  @throws[MojoExecutionException]
  override def execute(): Unit = {
  }

}

