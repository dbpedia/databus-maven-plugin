/*-
 * #%L
 * DBpedia Databus Maven Plugin
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

import fastparse.Parsed.{Failure, Success}
import fastparse.{P, parse}
import org.scalatest.{FlatSpec, Matchers, PrivateMethodTester}
import org.scalactic.Requirements._
import org.scalactic.Snapshots._
import org.scalactic.TypeCheckedTripleEquals._


class DatafileTest extends FlatSpec with Matchers with PrivateMethodTester {

  val filenamesThatShouldParse = Map(
    "mammals.nt" -> ("mammals", Seq(), Seq("nt"), Seq()),
    "mammals_cat.nt.bz2" -> ("mammals", Seq("cat"), Seq("nt"), Seq("bz2")),
    "mammals_cat.nt" -> ("mammals", Seq("cat"), Seq("nt"), Seq()),
    "mammals_cat.nt.patch" -> ("mammals", Seq("cat"), Seq("nt", "patch"), Seq()),
    "mammals-2018.08.15_cat.nt.patch.tar.bz2" ->
      ("mammals-2018.08.15", Seq("cat"), Seq("nt", "patch"), Seq("tar", "bz2"))
  )

  val parseAccessor =
    PrivateMethod[P[(String, Seq[String], Seq[String], Seq[String])]]('databusInputFilenameP)

  def checkParseSuccess(filename: String, expected: (String, Seq[String], Seq[String], Seq[String])) = {

    println(snap(filename, expected))

    parse(filename, Datafile invokePrivate parseAccessor(_)) match {

      case Success(actual, _) => {
        println(actual)
        actual shouldBe expected
      }

      case failure: Failure => fail(s"'$filename' did not parse:\n${failure.trace().longAggregateMsg}")
    }
  }

  filenamesThatShouldParse.headOption.foreach { case (filename, expected) =>

    "The parse for input filenames" should s"succeed for '$filename'" in checkParseSuccess(filename, expected)

  }

  filenamesThatShouldParse.tail.foreach { case (filename, expected) =>

    it should s"succeed for '$filename'" in checkParseSuccess(filename, expected)
  }
}
