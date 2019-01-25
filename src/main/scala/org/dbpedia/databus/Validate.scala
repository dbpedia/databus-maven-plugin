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

import java.io.{DataInput, File}

import org.dbpedia.databus.shared.authentification.{AccountHelpers, RSAModulusAndExponent}
import com.typesafe.scalalogging.LazyLogging
import org.apache.jena.rdf.model.ModelFactory
import org.apache.maven.plugin.{AbstractMojo, MojoExecutionException}
import org.apache.maven.plugins.annotations.{Execute, LifecyclePhase, Mojo, Parameter}
import org.dbpedia.databus.lib.{Datafile, FilenameHelpers, SigningHelpers}

import scala.collection.mutable
import org.apache.maven.settings.Settings


/**
  * Validate setup and resources
  *
  * WebID
  * - dereference and download
  * - get the public key from the webid
  * - get the private key from the config, generate a public key and compare to the public key
  *
  */
@Mojo(name = "validate", defaultPhase = LifecyclePhase.VALIDATE, requiresOnline = true, threadSafe = true)
class Validate extends AbstractMojo with SigningHelpers with LazyLogging with Properties {

  @throws[MojoExecutionException]
  override def execute(): Unit = {

    // parent module, i.e. packaging pom
    if (isParent()) {
      validateWebId()
      validateAccount()
    }
  }



  def validateWebId(): Unit = {

    if (locations.pkcs12File == null) {
      getLog.error(s"no private key bundle (pkcs12/.pfx) configured [databus.pkcs12File = ${locations.pkcs12File}]\n" +
        s"fix with:\n" +
        s"* adding <databus.pkcs12file> to pom.xml\n" +
        s"* adding server config to settings.xml of maven")
      System.exit(-1)
    }

    getLog.debug("PKCS12 bundle location: " + locations.pkcs12File.pathAsString)

    def keyPair = singleKeyPairFromPKCS12

    val modulusExponentFromPKCS12 =
      RSAModulusAndExponent(keyPair.privateKey.getModulus, keyPair.publicKey.getPublicExponent)

    /**
      * Read the webid
      */

    val webIdModel = ModelFactory.createDefaultModel
    webIdModel.read(publisher.toString)
    getLog.debug("Read publisher webid: " + webIdModel.size() + " triples from " + publisher)

    val matchingKeyInWebId = modulusExponentFromPKCS12.matchAgainstWebId(webIdModel, publisher.toString, Some(getLog))

    if (matchingKeyInWebId.isDefined) {
      getLog.info("SUCCESS: Private Key validated against WebID")
    } else {
      getLog.error("FAILURE: Private Key and WebID do not match")
      System.exit(-1)
    }
  }

  def validateAccount() = {
    getLog.info("Checking for registered DBpedia account")
    AccountHelpers.getAccountOption(publisher) match {
      case Some(account) => {
        getLog.info(s"SUCCESS: DBpedia Account found: ${account.getURI}")
      }
      case None => {
        getLog.warn(s"DBpedia account for $publisher not found at https://github.com/dbpedia/accounts " +
          s", some features might be deactivated")
      }
    }
  }


}
