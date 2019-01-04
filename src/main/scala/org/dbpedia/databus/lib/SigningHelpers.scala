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
package org.dbpedia.databus.lib

import java.io.IOException
import java.net.URL
import java.security.GeneralSecurityException

import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.apache.jena.riot.RDFLanguages.TURTLE
import org.dbpedia.databus.Locations
import org.dbpedia.databus.shared.authentification.PKCS12File
import org.dbpedia.databus.shared.helpers.conversions._


import scala.collection.concurrent.TrieMap
import scala.io.StdIn
import scala.util.Try


object SigningHelpers {

  type FilePath = String
  type Password = String

  val pkcs12PasswordMemo = TrieMap[FilePath, Password]()


}

trait SigningHelpers {
  this: Locations =>

  import SigningHelpers._

  def singleKeyPairFromPKCS12 = openPKCS12.get.rsaKeyPairs match {

    case uniqueKeyPair :: Nil => uniqueKeyPair

    case Nil => sys.error(s"PKCS12 bundle at '${locations.pkcs12File.pathAsString}' contains no key pairs")

    case _ => sys.error(s"PKCS12 bundle at '${locations.pkcs12File.pathAsString}' has multiple key pairs - ambiguity")
  }

  def openPKCS12 = {

    Try {
      PKCS12File(locations.pkcs12File, password = "") tap {
        _.rsaKeyPairs
      }
    } recover {

      case _: IOException | _: GeneralSecurityException =>
        PKCS12File(locations.pkcs12File, askForPassword) tap {
          _.rsaKeyPairs
        }
    }
  }

  def pkcs12Password = openPKCS12.map(_ => pkcs12PasswordMemo.getOrElse(canonicalPath, ""))

  protected def canonicalPath = locations.pkcs12File.toJava.getCanonicalPath

  protected def askForPassword: String = SigningHelpers.synchronized {

    def inputRequest = s"Enter password for PKCS12 bundle at $canonicalPath: "

    pkcs12PasswordMemo.getOrElseUpdate(canonicalPath, {

      Option(System.console()) match {

        case Some(console) => new String(console.readPassword(inputRequest))

        case None => StdIn.readLine(inputRequest)
      }
    })
  }
}
