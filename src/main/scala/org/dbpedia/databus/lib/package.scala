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

import better.files._

package object lib {

  def findFileMaybeInParent(file: File): File = {

    def innerRecursion(file: File, origPath: Option[File]): File = {

      if(file.isRegularFile) { file } else {

        origPath match {

          case Some(origPath) => sys.error("Unable to find the private key file at " +
            s"'${origPath.pathAsString}' or '${file.pathAsString}'")

          case None => {

            val parentDirPath = file.parent.parent / file.name

            innerRecursion(parentDirPath, Some(file))
          }
        }
      }
    }

    innerRecursion(file, None)
  }
}
