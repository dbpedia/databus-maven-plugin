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

trait Locations {
  this: Properties =>

  lazy val locations = new Locations(this)

  class Locations(props: Properties) {

    def packageTargetDirectory = (packageDirectory.toScala / artifactId / version).createDirectories()

    lazy val pkcs12File: File = {
      if (props.pkcs12File != null) {
        lib.findFileMaybeInParent(props.pkcs12File.toScala, "PKCS12 bundle")
      } else if (props.settings.getServer(pkcs12serverId) != null) {
        lib.findFileMaybeInParent(File(settings.getServer(pkcs12serverId).getPrivateKey),"PKCS bundle")
      } else {
        null
      }
    }

    def pkcs12Password: String = {

      if (props.pkcs12password.nonEmpty) {
        props.pkcs12password
      } else if (props.settings.getServer(pkcs12serverId) != null) {
        settings.getServer(pkcs12serverId).getPassphrase
      } else {
        ""
      }
    }

  }

}
