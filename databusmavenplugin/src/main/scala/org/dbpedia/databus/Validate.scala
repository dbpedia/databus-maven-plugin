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
      val exponentResource = model.listObjectsOfProperty(node.asResource(), model.getProperty("http://www.w3.org/ns/auth/cert#exponent")).next()
      val exponentWebId = exponentResource.asLiteral().getLexicalForm

      val modulusResource = model.listObjectsOfProperty(node.asResource(), model.getProperty("http://www.w3.org/ns/auth/cert#modulus")).next()
      val modulusWebId = modulusResource.asLiteral().getLexicalForm

      //TODO fix paths
      val fileHack: File = new File(privateKeyFile.getAbsolutePath.replace("${project.parent.basedir}/", ""))
      val privateKey = Sign.readPrivateKeyFile(fileHack)
      val privk: RSAPrivateCrtKey = privateKey.asInstanceOf[RSAPrivateCrtKey]


      getLog.info("BlankNode: " + node + "")
      getLog.info("Exponent (from webid): " + exponentWebId)
      getLog.info("Exponent (from privk): " + privk.getPublicExponent)
      if (exponentWebId.equalsIgnoreCase(privk.getPublicExponent.toString())) {
        getLog.info("Exponents match")
      } else {
        getLog.error("Exponents do NOT match")
      }

      getLog.info("Modulus (from webid): " + modulusWebId)
      getLog.info("Modulus (from privk): " + privk.getModulus.toString(16))


      if (modulusWebId.equalsIgnoreCase(privk.getModulus.toString(16))) {
        getLog.info("Moduli match")
      } else {
        getLog.error("Moduli do NOT match")
      }


      //getLog.info("Modulus (from privk): " +  java.lang.Long.valueOf(privk.getModulus.toString,16))


    }

  }

  def validateFileNames(): Unit = {
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


