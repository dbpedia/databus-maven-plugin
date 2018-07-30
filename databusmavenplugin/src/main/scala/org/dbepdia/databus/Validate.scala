package org.dbpedia.databus


import java.io.File
import java.net.URL
import java.util

import org.apache.jena.rdf.model.{ModelFactory, NodeIterator, RDFNode}
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



  @Parameter(defaultValue = "${project.artifactId}", readonly = true)
  private val artifactId: String = ""

  @Parameter(defaultValue = "${project.packaging}", readonly = true)
  private val packaging: String = ""


  @Parameter var maintainer: URL = _
  @Parameter var privateKeyFile: File = _

  //@Parameter var contentVariants:util.ArrayList[ContentVariant] = null
  @Parameter var contentVariants: util.ArrayList[String] = _
  @Parameter var formatVariants: util.ArrayList[String] = _

  @throws[MojoExecutionException]
  override def execute(): Unit = {
    if (packaging.equals("pom")) {

      validateWebId()

    } else {

      validateFileNames()

    }
  }

  /**
    * for now just prints all keys
    */
  def validateWebId(): Unit = {

    /**
      * Read the webid
      */
    val model = ModelFactory.createDefaultModel
    model.read(maintainer.toString)
    getLog.info("Read " + model.size() + " triples from " + maintainer)
    val ni: NodeIterator = model.listObjectsOfProperty(model.getResource(maintainer.toString), model.getProperty("http://www.w3.org/ns/auth/cert#key"))

    //TODO validate against the private key
    getLog.info("Private Key File: " + privateKeyFile)
    while (ni.hasNext) {
      var node: RDFNode = ni.next()
      var modulus = model.listObjectsOfProperty(node.asResource(), model.getProperty("http://www.w3.org/ns/auth/cert#modulus")).next()
      var exponent = model.listObjectsOfProperty(node.asResource(), model.getProperty("http://www.w3.org/ns/auth/cert#exponent")).next()
      getLog.info("BlankNode: " + node + "")
      getLog.info("Exponent: " + exponent.asLiteral().getLexicalForm )
      getLog.info("Modulus: " + modulus.asLiteral().getLexicalForm )
    }

  }

  def validateFileNames(): Unit = {

  }


}


