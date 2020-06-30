/*-
 * #%L
 * DBpedia Databus Maven Plugin
 * %%
 * Copyright (C) 2018 - 2020 Sebastian Hellmann (on behalf of the DBpedia Association)
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
package org.dbpedia.databus.ipfs

import org.dbpedia.databus.CommonMavenIntegrationTest

import scala.util.{Failure, Try}


class IpfsPluginItTest extends CommonMavenIntegrationTest {

  test("the goal is executed") {
    val v = verifier
    v.setDebug(false)
    v.setMavenDebug(false)


    Try(v.executeGoal("package")) match {
      case Failure(exception) =>
        exception.printStackTrace()
        fail(exception)
      case _ =>
    }

    Try(v.verifyErrorFreeLog()) match {
      case Failure(exception) =>
        exception.printStackTrace()
        fail(exception)
      case _ =>
    }
  }

}
