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

import java.io.File

import fastparse.{CharIn, CharPred, P, Start, StringIn}
import org.apache.maven.plugin.logging.Log
import com.typesafe.scalalogging.LazyLogging
import fastparse.NoWhitespace._
import fastparse._
import org.dbpedia.databus.lib.FilenameHelpers.databusInputFilenameP

class FilenameHelpers(val file: File, previewLineCount: Int = 10)(implicit log: Log) {

  lazy val (filePrefix: String, contentVariantExtensions: Seq[String], formatVariantExtensions: Seq[String],
  compressionVariantExtensions) = filenameAnalysis


  def basename(): String = {
    file.getName
  }

  /*def finalBasename(versionToAdd: Option[String]) = {

    def prefix = versionToAdd.fold(filePrefix) { version => s"$filePrefix-$version" }

    // we have to check for empty extension seqs, otherwise we get misplaced inital '.'/'_'
    def contentVariantsSuffix = if (contentVariantExtensions.nonEmpty) {
      contentVariantExtensions.mkString("_", "_", "")
    } else ""

    def formatVariantsSuffix = if (formatVariantExtensions.nonEmpty) {
      formatVariantExtensions.mkString(".", ".", "")
    } else ""

    def compressionVariantsSuffix = if (compressionVariantExtensions.nonEmpty) {
      compressionVariantExtensions.mkString(".", ".", "")
    } else ""

    prefix + contentVariantsSuffix + formatVariantsSuffix + compressionVariantsSuffix
  }*/

  //todo check if not private is okay
  def filenameAnalysis: (String, Seq[String], Seq[String], Seq[String]) = {

    parse(basename, databusInputFilenameP(_)) match {

      case success@Parsed.Success(basenameParts, _) => basenameParts

      case failure: Parsed.Failure =>
        sys.error(s"Unable to analyse filename '${basename}': Please refer to " +
          s"http://dev.dbpedia.org/Databus%20Maven%20Plugin on conventions for input file naming:\n" +
          failure.trace().longAggregateMsg)
    }
  }
}


object FilenameHelpers extends LazyLogging {

  def apply(file: File, previewLineCount: Int = 10)(implicit log: Log): FilenameHelpers = {
    new FilenameHelpers(file, previewLineCount)(log)
  }


  protected def alphaNumericP[_: P] = CharIn("A-Za-z0-9").rep(1)

  // the negative lookahead ensures that we do not parse into the format extension(s) if there is no content variant
  protected def artifactNameP[_: P] =
    P((!extensionP ~ CharPred(_ != '_')).rep(1).!)
      .opaque("<filename prefix>")

  //TODO added '=' here in a dirty way
  protected def contentVariantsP[_: P] =
  //P(("_" ~ alphaNumericP.!).rep())
    P(("_" ~ (alphaNumericP ~ "=" ~ alphaNumericP | alphaNumericP).!).rep())
      .opaque("<content variants>")

  protected def extensionP[_: P] = "." ~ (CharIn("A-Za-z") ~ CharIn("A-Za-z0-9").rep()).!

  // using a negative lookahead here to ensure that no compression extension is parsed as format extension
  protected def formatExtensionP[_: P] = P(!compressionExtensionP ~ extensionP)

  protected def formatExtensionsP[_: P] =
    P(formatExtensionP.rep(1))
      .opaque("<format variant extensions>")

  protected def compressionExtensionP[_: P] =
    P("." ~ StringIn("bz2", "gz", "tar", "xz", "zip").!)
      .opaque("<compression variant extensions>")

  protected def compressionExtensionsP[_: P] = P(compressionExtensionP.rep())

  protected def databusInputFilenameP[_: P]: P[(String, Seq[String], Seq[String], Seq[String])] =
    (Start ~ artifactNameP ~ contentVariantsP ~ formatExtensionsP ~ compressionExtensionsP ~ End)

}
