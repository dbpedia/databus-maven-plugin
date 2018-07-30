package org.dbepdia.databus.lib

import java.io.{BufferedInputStream, File, FileInputStream}
import java.nio.file.Files
import java.security.{DigestInputStream, KeyFactory, MessageDigest, Signature}
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64


/**
  * The main purpose of this class is to group and compare functions for hashing and signing
  */
object HashAndSign {

  /**
    *
    */
  def computeHash(file: File): String = {
    computeHash(file, 8192)
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

  def sign(privateKeyFile: File, file: File): String = {

    sign(privateKeyFile, file, 1024)
  }

  def sign(privateKeyFile: File, file: File, bufferSize: Integer): String = {
    val keyBytes = Files.readAllBytes(privateKeyFile.toPath)
    val spec = new PKCS8EncodedKeySpec(keyBytes)
    val kf = KeyFactory.getInstance("RSA")
    val privateKey = kf.generatePrivate(spec)
    val rsa = Signature.getInstance("SHA1withRSA")

    // start signature
    rsa.initSign(privateKey)
    val fis = new FileInputStream(file)
    val bufin = new BufferedInputStream(fis)
    val buffer = new Array[Byte](bufferSize)
    var len = 0
    while ( {
      len = bufin.read(buffer)
      len >= 0
    }) {
      rsa.update(buffer, 0, len)
    }

    val bytes = rsa.sign()
    new String (Base64.getEncoder.encode(bytes))
  }
}
