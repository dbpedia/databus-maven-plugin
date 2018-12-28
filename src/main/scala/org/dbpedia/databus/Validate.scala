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

import org.dbpedia.databus.shared.authentification.RSAModulusAndExponent

import com.typesafe.scalalogging.LazyLogging
import org.apache.jena.rdf.model.ModelFactory
import org.apache.maven.plugin.{AbstractMojo, MojoExecutionException}
import org.apache.maven.plugins.annotations.{LifecyclePhase, Mojo}


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
class Validate extends AbstractMojo with Properties with SigningHelpers with LazyLogging {


  /**
    *  TODO potential caveat: check if, else based on pom could fail

    *
    */
  @throws[MojoExecutionException]
  override def execute(): Unit = {

    /**
      * validation
      */

    // parent module, i.e. packaging pom
    if (isParent()) {
      validateWebId()

    // all submodules
    }
  }

  /**
    *
    */
  def validateWebId(): Unit = {

    getLog.debug("PKCS12 bundle: " + locations.pkcs12File.pathAsString)

    def keyPair = singleKeyPairFromPKCS12

    val modulusExponentFromPKCS12 =
      RSAModulusAndExponent(keyPair.privateKey.getModulus, keyPair.publicKey.getPublicExponent)


    /**
      * Read the webid
      */
    val webIdModel = ModelFactory.createDefaultModel
    webIdModel.read(maintainer.toString)
    getLog.debug("Read " + webIdModel.size() + " triples from " + maintainer)

    val matchingKeyInWebId = modulusExponentFromPKCS12.matchAgainstWebId(webIdModel, maintainer.toString, Some(getLog))

    if(matchingKeyInWebId.isDefined) {
      getLog.info("SUCCESS: Private Key validated against WebID")
    } else {
      getLog.error("FAILURE: Private Key and WebID do not match")
    }
  }

}
