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

import java.io.{BufferedInputStream, File, FileInputStream}
import java.nio.file.Files
import java.security._
import java.security.interfaces.RSAPrivateCrtKey
import java.security.spec.{PKCS8EncodedKeySpec, RSAPublicKeySpec, X509EncodedKeySpec}

object Sign {

  /**
    * reads the private key file, also looks in ../
    *
    * @param privateKeyFile
    * @return
    */
  def readPrivateKeyFile(privateKeyFile: File, origPath: Option[File] = None): PrivateKey = {

    if(privateKeyFile.isFile) {

      val keyBytes = Files.readAllBytes(privateKeyFile.toPath)
      val keySpec = new PKCS8EncodedKeySpec(keyBytes)
      val keyFactory = KeyFactory.getInstance("RSA")
      keyFactory.generatePrivate(keySpec)
    } else {

      origPath match {

        case Some(origPath) => sys.error("Unable to find the private key file at " +
          s"'${origPath.getPath}' or '${privateKeyFile.getPath}'")

        case None => {

          val parentDirPath = new File(privateKeyFile.getParentFile.getParentFile, privateKeyFile.getName)

          readPrivateKeyFile(parentDirPath, Some(privateKeyFile))
        }
      }
    }
  }

  def publicKeyFromPrivateKey(privateKey: PrivateKey) = {

    val privk: RSAPrivateCrtKey = privateKey.asInstanceOf[RSAPrivateCrtKey]
    val publicKeySpec: RSAPublicKeySpec = new RSAPublicKeySpec(privk.getModulus, privk.getPublicExponent)
    val keyFactory: KeyFactory = KeyFactory.getInstance("RSA")
    keyFactory.generatePublic(publicKeySpec)
  }
}
