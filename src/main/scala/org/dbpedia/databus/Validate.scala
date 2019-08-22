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


import java.net.URL

import org.dbpedia.databus.shared.authentification.{AccountHelpers, RSAModulusAndExponent}
import com.typesafe.scalalogging.LazyLogging
import org.apache.http.client.methods.HttpHead
import org.apache.http.impl.client.HttpClients
import org.apache.jena.rdf.model.ModelFactory
import org.apache.maven.plugin.{AbstractMojo, MojoExecutionException}
import org.apache.maven.plugins.annotations.{LifecyclePhase, Mojo}
import org.dbpedia.databus.lib.SigningHelpers


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

    if (!Validated.validated) {
      validateWebId()
      validateAccount()
      Validated.validated = true
    }

    // parent module, i.e. packaging pom
    if (isParent()) {
      //do nothing actually
      return
    }

    validateSelectedValues
    validateDebugURLs

  }

  def validateDebugURLs() = {

    val check: List[URL] = List(codeReference, feedbackChannel, issueTracker, documentationLocation)
    check.filter(_ != null).foreach(u => {
      val httpclient = HttpClients.createDefault();
      val httpHead = new HttpHead(u.toString)
      try {
        val code = httpclient.execute(httpHead).getStatusLine.getStatusCode;
        if (code == 404) {
          getLog.error(
            s"""
               |One of codeReference,feedbackChannel,issueTracker, documentationLocation, returned 404 NOT FOUND
               |URL: ${u.toString}
               |Fix with:
               |* check pom.xml <properties><databus.(codeReference|feedbackChannel|issueTracker|documentationLocation>
             """.stripMargin)
        }

      } catch {
        case _: Throwable => Unit
      }
    })

  }

  def validateSelectedValues = {
    val forbiddenchars = List("\\", " / ", ":", "\"", "<", ">", "|", "?", "*")
    if (version.toList.exists(forbiddenchars.contains)) {
      getLog.error(s"Version: ${version} contains forbidden chars: ${forbiddenchars.mkString("")}")
      System.exit(-1)
    }

    getLog.info("Issued date: " + params.issuedDate)
    params.validateMarkdown()
    getLog.info(s"${locations.provenanceFull.size} provenance urls found")
  }


  def validateWebId(): Unit = {

    if (locations.pkcs12File == null) {
      getLog.error(
        s"""
           |no private key bundle (pkcs12/.pfx) configured [databus.pkcs12File = ${locations.pkcs12File}]
           |fix with:
           |* adding property <databus.pkcs12file> to pom.xml
           |* adding server config to settings.xml of maven:
           |   <server>
           |      <id>databus.defaultkey</id>
           |      <privateKey>$${user.home}/.m2/certificate_generic.pfx</privateKey>
           |      <!-- optional -->
           |      <passphrase>this is my password</passphrase>
           |   </server>
           |
       """.stripMargin)
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
        getLog.warn(s"Databus account for $publisher not found at https://github.com/dbpedia/accounts " +
          s", some features might be deactivated\n" +
          s"You will not be able to publish on databus.dbpedia.org with mvn deploy without account")
      }
    }
  }


}

// a small static var
// if inheritance is used, parent is not validated
object Validated {
  var validated = false
}