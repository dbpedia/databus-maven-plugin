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

import org.dbpedia.databus.lib.Sign
import org.dbpedia.databus.shared.authentification.RSAModulusAndExponent

import better.files._
import org.apache.jena.rdf.model.ModelFactory
import org.apache.maven.plugin.{AbstractMojo, MojoExecutionException}
import org.apache.maven.plugins.annotations.{LifecyclePhase, Mojo}

import java.security.interfaces.RSAPrivateCrtKey


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
      * validation
      */

    // parent module, i.e. packaging pom
    if (isParent()) {
      validateWebId()

    // all submodules
    } else {
      // as we changed the phases this one goes out here
      //validateFileNames()
    }
  }

  /**
    *
    */
  def validateWebId(): Unit = {

    getLog.info("Private Key File: " + privateKeyFile)

    val modulusExponentFromFile = Sign.readPrivateKeyFile(privateKeyFile.toScala) match {

      case rsaPrivateKey: RSAPrivateCrtKey => {
        RSAModulusAndExponent(rsaPrivateKey.getModulus, rsaPrivateKey.getPublicExponent)
      }

      case otherKey => sys.error(s"Unexpected private key format: ${otherKey.getClass.getSimpleName}")
    }

    /**
      * Read the webid
      */
    val webIdModel = ModelFactory.createDefaultModel
    webIdModel.read(maintainer.toString)
    getLog.debug("Read " + webIdModel.size() + " triples from " + maintainer)

    val matchingKeyInWebId = modulusExponentFromFile.matchAgainstWebId(webIdModel, maintainer.toString, Some(getLog))

    if(matchingKeyInWebId.isDefined) {
      getLog.info("SUCCESS: Private Key validated against WebID")
    } else {
      getLog.error("FAILURE: Private Key and WebID do not match")
    }
  }

  def validateFileNames(): Unit = {

    //getLog.info("Checking files")
    //val in =  FileHelper.getListOfDataFiles(dataDirectory,artifactId,getDataIdFile().getName)
    //getLog.info("including "+in.size+ " files starting with "+artifactId + " and not pre-existing dataid files")
    //getLog.info("\n"+in.mkString("\n"))

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


