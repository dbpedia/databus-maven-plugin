/*-
 * #%L
 * DBpedia Databus Maven Plugin
 * %%
 * Copyright (C) 2018 - 2019 Sebastian Hellmann (on behalf of the DBpedia Association)
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

import java.net.URL

import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.apache.jena.riot.RDFLanguages.TURTLE
import org.dbpedia.databus.shared.rdf.vocab.foaf
import org.dbpedia.databus.shared.rdf.conversions._
import org.dbpedia.databus.shared.helpers.conversions._

object AccountHelpers {

  //retrieving all User Accounts
  lazy val registeredAccounts = ModelFactory.createDefaultModel.tap { accountsModel =>
    accountsModel.read("https://raw.githubusercontent.com/dbpedia/accounts/master/accounts.ttl", TURTLE.getName)
  }

  /**
    * check for user account
    *
    * @param publisher
    */
  def getAccountOption(publisher: URL) = {
    lazy val accountOption = {
      implicit val userAccounts: Model = registeredAccounts
      Option(publisher.toString.asIRI.getProperty(foaf.account)).map(_.getObject.asResource)
    }
    accountOption
  }

}
