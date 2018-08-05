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


/**
  * The main purpose of this class is to group and compare functions for hashing and signing
  */
object Hash {
  var bufferSizeHash: Int = 32768


  /**
    *
    */
  def computeHash(file: File): String = {
    computeHash(file, bufferSizeHash)
  }

  /** From https://stackoverflow.com/questions/41642595/scala-file-hashing
    * Compute a hash of a file
    * The output of this function should match the output of running "md5 -q <file>"
    */
  def computeHash(file: File, bufferSize: Integer): String = {
    val buffer = new Array[Byte](bufferSize)
    val md5 = MessageDigest.getInstance("MD5")

    val dis = new DigestInputStream(new FileInputStream(file), md5)
    try {
      while (dis.read(buffer) != -1) {}
    } finally {
      dis.close()
    }
    md5.digest.map("%02x".format(_)).mkString
  }

}

object Sign {
  var bufferSizeCrypt = 32768


  /**
    * reads the private key file, also looks in ../
    *
    * @param privateKeyFile
    * @return
    */
  def readPrivateKeyFile(privateKeyFile: File): PrivateKey = {
    var corrected = privateKeyFile
    if (!corrected.exists()) {
      corrected = new File(privateKeyFile.getParentFile.getParentFile, privateKeyFile.getName)
    }

    val keyBytes = Files.readAllBytes(corrected.toPath)
    val spec = new PKCS8EncodedKeySpec(keyBytes)
    val kf = KeyFactory.getInstance("RSA")
    val privateKey = kf.generatePrivate(spec)
    privateKey
  }

  def sign(privateKey: PrivateKey, datafile: File): Array[Byte] = {

    sign(privateKey, datafile, bufferSizeCrypt)
  }

  def sign(privateKey: PrivateKey, datafile: File, bufferSize: Integer): Array[Byte] = {

    val rsa = Signature.getInstance("SHA1withRSA")
    rsa.initSign(privateKey)
    update(rsa, datafile, bufferSize)
    rsa.sign()
    //new String (Base64.getEncoder.encode(bytes))
  }

  def verify(privateKey: PrivateKey, datafile: File, signature: Array[Byte]): Boolean = {
    verify(privateKey, datafile, signature, bufferSizeCrypt)
  }

  def verify(privateKey: PrivateKey, datafile: File, signature: Array[Byte], bufferSize: Int): Boolean = {

    val privk: RSAPrivateCrtKey = privateKey.asInstanceOf[RSAPrivateCrtKey]
    val publicKeySpec: RSAPublicKeySpec = new RSAPublicKeySpec(privk.getModulus, privk.getPublicExponent)
    val keyFactory: KeyFactory = KeyFactory.getInstance("RSA")
    val publicKey: PublicKey = keyFactory.generatePublic(publicKeySpec)
    val rsa = Signature.getInstance("SHA1withRSA")
    rsa.initVerify(publicKey)
    update(rsa, datafile, bufferSize)
    rsa.verify(signature)
  }

  //todo close stream
  private def update(rsa: Signature, datafile: File, bufferSize: Int): Unit = {
    val fis = new FileInputStream(datafile)
    val bufin = new BufferedInputStream(fis)
    val buffer = new Array[Byte](bufferSize)
    var len = 0
    while ( {
      len = bufin.read(buffer)
      len >= 0
    }) {
      rsa.update(buffer, 0, len)
    }
  }

  @Deprecated
  def verifyold(publicKeyBytes: Array[Byte], dataid: Array[Byte], signature: Array[Byte]): Boolean = {
    val spec = new X509EncodedKeySpec(publicKeyBytes)
    val kf = KeyFactory.getInstance("RSA")
    val publicKey = kf.generatePublic(spec)
    val rsa = Signature.getInstance("SHA1withRSA")
    rsa.initVerify(publicKey)
    rsa.update(dataid)
    rsa.verify(signature)
  }


}
