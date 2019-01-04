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
