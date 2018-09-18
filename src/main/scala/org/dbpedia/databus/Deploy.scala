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

import java.io.{File, FileWriter, InputStream}
import java.nio.file.Files
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

import better.files._
import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.apache.maven.plugin.{AbstractMojo, MojoExecutionException}
import org.apache.maven.plugins.annotations.{LifecyclePhase, Mojo}
import org.dbpedia.databus.shared.signing
import org.dbpedia.databus.shared.authentification.{PKCS12File, RSAKeyPair}
import org.dbpedia.databus.shared.helpers._
import scalaj.http.MultiPart
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.KeyStore

import com.typesafe.scalalogging.LazyLogging
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory, X509TrustManager}
import scalaj.http.{BaseHttp, HttpConstants, HttpOptions}


// not sure if needed
//import org.apache.http.client.methods.HttpGet
//import org.scalatra.test.scalatest.ScalatraFlatSpec

@Mojo(name = "deploy", defaultPhase = LifecyclePhase.DEPLOY)
class Deploy extends AbstractMojo with Properties {


  @throws[MojoExecutionException]
  override def execute(): Unit = {
    //skip the parent module
    if (isParent()) {
      getLog.info("skipping parent module")
      return
    }


    // dataId files
    var dataIdCollect: Model = ModelFactory.createDefaultModel
    // resolving relative URIs into downloadURLs
    dataIdCollect.read(getDataIdFile().toURI.toString, downloadUrlPath.toString+getDataIdFile.getName,"turtle")
    val dataIdPackageTarget = new File(getPackageDirectory, "/" + getDataIdFile().getName)
    dataIdCollect.write(new FileWriter(dataIdPackageTarget),"turtle")
    //Files.copy(getDataIdFile().toPath, dataIdPackageTarget.toPath, REPLACE_EXISTING)


    import DataIdRepo.{UploadParams, UploadPartNames}
    val dataIdResourceName = "mammals-1.0.0_dataid.ttl"
    val dataIdTargetLocation = s"http://databus.dbpedia.org/test/$dataIdResourceName"
    val deploymentBaseIRI = s"http://databus.dbpedia.org/test/$dataIdResourceName"

    val dataIdSize = resourceAsStream(dataIdResourceName) acquireAndGet { is =>

      Stream.continually(is.read()).takeWhile(_ != -1).size
    }


    (resourceAsStream(dataIdResourceName) and resourceAsStream(dataIdResourceName)) apply {
      case (dataIdForSend, dataIdForSign) =>

        //todo check
        //val sslContext = testhelpers.pkcsClientCertSslContext("test-cert-bundle.p12")

        val pkcs12 = resoucreAsFile("test-cert-bundle.p12") apply (PKCS12File(_))

        // get the key pair from cert
        val RSAKeyPair(publicKey, privateKey) = pkcs12.rsaKeyPairs.head


        def dataIdPart = MultiPart(UploadPartNames.dataId, "dataid.ttl", "text/turtle", dataIdForSend, dataIdSize,
          bytesWritten => getLog.debug(s"$bytesWritten bytes written"))

        def signaturePart = MultiPart(UploadPartNames.dataIdSignature, "dataid.ttl.sig", "application/pkcs7-signature",
          signing.signInputStream(privateKey, dataIdForSign))

        val params = Map(
          UploadParams.dataIdLocation -> dataIdTargetLocation,
          UploadParams.allowOverwrite -> true.toString
        )

        def encodedParamsQueryString = {

          def encode: String => String = URLEncoder.encode(_, StandardCharsets.UTF_8.name())

          params.map({ case (k, v) => s"$k=${encode(v)}" }).mkString("&")
        }

        def paramsPart = MultiPart(UploadPartNames.uploadParams, "dataid.params", "application/x-www-form-urlencoded",
          encodedParamsQueryString)

        //todo check
        val sslHttp = testhelpers.scalajHttpWithClientCert("test-cert-bundle.p12")

        val req = sslHttp(s"$deploymentBaseIRI/dataid/upload")
          .postMulti(dataIdPart, signaturePart, paramsPart)

        val serviceResp = req.asString

        getLog.debug("response meta: " + serviceResp.toString)
        getLog.debug("response body: " + serviceResp.body)
        
    }




  }

  object DataIdRepo {

    lazy val expectedPartsForUpload = {

      import UploadPartNames._

      Set(dataId, dataIdSignature, uploadParams)
    }

    object UploadPartNames {

      val (dataId, dataIdSignature, uploadParams) = ("dataid", "dataid-signature", "upload-params")
    }

    object UploadParams {

      val (dataIdLocation, allowOverwrite) = ("DataIdLocation", "AllowOverwrite")
    }
  }

  object testhelpers extends LazyLogging {

    def scalajHttpWithClientCert(pkcs12BundleResourceName: String) = {

      val sslContext = pkcsClientCertSslContext(pkcs12BundleResourceName)

      val defaultOptionsAndSSL = HttpConstants.defaultOptions :+ HttpOptions.sslSocketFactory(sslContext.getSocketFactory)

      new BaseHttp(options = defaultOptionsAndSSL)
    }

    def pkcsClientCertSslContext(pkcs12BundleResourceName: String): SSLContext = {

      resourceAsStream(pkcs12BundleResourceName) apply { bundleStream =>

        pkcsClientCertSslContext(bundleStream)
      }
    }

    def pkcsClientCertSslContext(pkcs12BundleInput: InputStream) = {

      val password = ""

      val ks = KeyStore.getInstance("PKCS12")
      ks.load(pkcs12BundleInput, password.toCharArray)

      val kmf = KeyManagerFactory.getInstance("SunX509")
      kmf.init(ks, password.toCharArray)

      val sslContext = SSLContext.getInstance("TLS")
      sslContext.init(kmf.getKeyManagers, null, null)
      sslContext
    }

/*
    def defaultX509TrustManager = {

      val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm)

      tmf.init(null.asInstanceOf[KeyStore])
      tmf.getTrustManagers.collect({case x509: X509TrustManager => x509}).head
    }





    def httpClientWithClientCert(pkcs12BundleResourceName: String) = {

      val sslContext = pkcsClientCertSslContext(pkcs12BundleResourceName)

      val builder = HttpClientBuilder.create()
      builder.disableRedirectHandling()
      builder.setSSLContext(sslContext)

      builder.build()
    }
*/

  }

}
