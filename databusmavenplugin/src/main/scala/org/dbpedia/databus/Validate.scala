/*-
 * #%L
 * databus-maven-plugin
 * %%
 * Copyright (C) 2018 Sebastian Hellmann (on behalf of the DBpedia Association)
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
package org.dbpedia.databus


import java.io.File
import java.net.URL
import java.security.interfaces.RSAPrivateCrtKey
import java.util

import org.apache.jena.rdf.model.{ModelFactory, NodeIterator, RDFNode}
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugins.annotations.{LifecyclePhase, Mojo, Parameter}
import org.dbpedia.databus.lib.{FileHelper, Sign}

import scala.collection.mutable


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
class Validate extends AbstractMojo with Properties {


  /**
    *  TODO potential caveat: check if, else based on pom could fail

    * @throws MojoExecutionException
    */
  @throws[MojoExecutionException]
  override def execute(): Unit = {

    /**
      * setup
       */
    // create targetDir
    targetDirectory.mkdirs()

    /**
      * validation
      */

    // parent module, i.e. packaging pom
    if (isParent()) {

      validateWebId()

    // all submodules
    } else {
      validateFileNames()
    }
  }

  /**
    *
    */
  def validateWebId(): Unit = {

    /**
      * Read the webid
      */
    val model = ModelFactory.createDefaultModel
    model.read(maintainer.toString)
    getLog.debug("Read " + model.size() + " triples from " + maintainer)


    val ni: NodeIterator = model.listObjectsOfProperty(model.getResource(maintainer.toString), model.getProperty("http://www.w3.org/ns/auth/cert#key"))
    // TODO add a
    getLog.info("Private Key File: " + privateKeyFile)
    //val fileHack: File = new File(privateKeyFile.getAbsolutePath.replace("", ""))
    val privateKey = Sign.readPrivateKeyFile(privateKeyFile)
    val privk: RSAPrivateCrtKey = privateKey.asInstanceOf[RSAPrivateCrtKey]
    val modulusPrivkHex = privk.getModulus.toString(16)
    val exponentPrivk = privk.getPublicExponent.toString()

    var matching:Boolean = false
    // iterate all public keys from webid
    while (ni.hasNext) {
      var node: RDFNode = ni.next()
      val exponentResource = model.listObjectsOfProperty(node.asResource(), model.getProperty("http://www.w3.org/ns/auth/cert#exponent")).next()
      val exponentWebId = exponentResource.asLiteral().getLexicalForm
      val modulusResource = model.listObjectsOfProperty(node.asResource(), model.getProperty("http://www.w3.org/ns/auth/cert#modulus")).next()
      val modulusWebId = modulusResource.asLiteral().getLexicalForm

      // some log output
      // TODO change to debug later
      getLog.info("BlankNode: " + node + "")
      getLog.info("Exponent (from webid): " + exponentWebId)
      getLog.info("Exponent (from privk): " + exponentPrivk)
      getLog.info("Modulus (from webid): " + modulusWebId.substring(0,30).toLowerCase+"...")
      getLog.info("Modulus (from privk): " + modulusPrivkHex.substring(0,30)+"...")


      // mainly for debugging
      if (exponentWebId.equalsIgnoreCase(exponentPrivk)) {
        getLog.info("Exponents match")
      } else {
        getLog.error("Exponents do NOT match")
      }

      if (modulusWebId.equalsIgnoreCase(privk.getModulus.toString(16))) {
        getLog.info("Moduli match")
      } else {
        getLog.error("Moduli do NOT match")
      }

      // the real condition
      if (exponentWebId.equalsIgnoreCase(exponentPrivk) && modulusWebId.equalsIgnoreCase(privk.getModulus.toString(16))){
        matching = true
      }
      //getLog.info("Modulus (from privk): " +  java.lang.Long.valueOf(privk.getModulus.toString,16))
    }

    if (matching) {
      getLog.info("SUCCESS: Private Key validated against WebID")
    } else {
      getLog.error("FAILURE: Private Key and WebID do not match")
    }

  }

  def validateFileNames(): Unit = {

    FileHelper.getListOfFiles(dataDirectory).foreach(datafile => {

      getLog.info("Checking files")
      val in = new mutable.HashSet[String]
      val out = new mutable.HashSet[String]
      // check for matching artifactId
      if (datafile.getName.startsWith(artifactId)) {
        in.add(datafile.toString)
      } else {
        out.add(datafile.toString)
      }

      getLog.info("including "+in.size+ " files starting with "+artifactId)
      getLog.info("/n"+in.mkString("\n"))
      getLog.info("excluding  "+in.size+ " files NOT starting with "+artifactId)
      // check mimetype

    })

   /*  val moduleDirectories = FileHelper.getModules(multiModuleBaseDirectory)
    getLog.info(moduleDirectories+"")
    getLog.info(multiModuleBaseDirectory)

    // processing each module
    moduleDirectories.foreach(moduleDir => {
      getLog.info(s"reading from module $moduleDir")

      // processing all file per module
      FileHelper.getListOfFiles(s"$moduleDir/$resourceDirectory").foreach(datafile => {

        // check for matching artifactId
        if (datafile.getName.startsWith(artifactId)) {
          //good
          getLog.info("include: "+datafile)
        } else {
          //bad
          getLog.info("exclude: "+datafile)

        }

        // check mimetype

      })
    })
*/
  }

}


