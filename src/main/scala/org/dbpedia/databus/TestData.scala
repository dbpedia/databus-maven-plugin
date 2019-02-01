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

import java.io.{ByteArrayOutputStream, File}

import org.dbpedia.databus.lib.{Datafile, FilenameHelpers}
import org.dbpedia.databus.parse.{LineBasedRioDebugParser, RioOtherParser}
import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.apache.jena.riot.RDFLanguages
import org.apache.maven.plugin.{AbstractMojo, MojoExecutionException}
import org.apache.maven.plugins.annotations.{LifecyclePhase, Mojo, Parameter}
import org.eclipse.rdf4j.rio.{RDFFormat, RDFParser, Rio}
import java.security.MessageDigest
import java.util.Base64

import org.dbpedia.databus.voc.RDFBased

import scala.collection.{immutable, mutable}

@Mojo(name = "test-data", defaultPhase = LifecyclePhase.TEST, threadSafe = true)
class TestData extends AbstractMojo with Properties {

  @Parameter(property = "databus.test.parseRDF", defaultValue = "false")
  val parseRDF: Boolean = false


  @Parameter(property = "databus.test.allVersions", defaultValue = "false", required = false)
  val allVersions: Boolean = false

  @Parameter(property = "databus.test.detailed", defaultValue = "false", required = false)
  val detailed: Boolean = false

  @Parameter(property = "databus.test.strict", defaultValue = "false", required = false)
  val strict: Boolean = false


  @throws[MojoExecutionException]
  override def execute(): Unit = {

    //skip the parent module
    if (isParent()) {
      getLog.info("skipping parent module")
      return
    }

    params.validateMarkdown()

    validateVersions(getVersionsToValidate)

    if (parseRDF) {
      // prepare parselogfile
      locations.buildParselogFile.createFileIfNotExists(true).clear()
      generateParselogForRDFSyntax

    }
  }


  def generateParselogForRDFSyntax = {
    //val parseLogFileWriter = Files.newBufferedWriter(getParseLogFile().toPath, StandardCharsets.UTF_8)

    locations.inputFileList.foreach(datafile => {

      var parseLog = new StringBuilder
      var details = new StringBuilder
      val df: Datafile = Datafile(datafile.toJava)(getLog).ensureExists()
      val model: Model = ModelFactory.createDefaultModel
      val finalBasename = df.finalBasename(params.versionToInsert)
      //TODO
      val thisResource = model.createResource("#" + finalBasename)

      val prefixParse = "http://dataid.dbpedia.org/ns/pl#"

      parseLog.append(s"parsing ${datafile.name} with ${df.format}\n")

      //val config = new ParserConfig
      //config.set(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES, false)
      //config.set(BasicParserSettings.FAIL_ON_UNKNOWN_LANGUAGES, false)
      //config.set(BasicParserSettings.VERIFY_DATATYPE_VALUES, false)
      //config.set(BasicParserSettings.VERIFY_LANGUAGE_TAGS, false)
      //config.set(BasicParserSettings.VERIFY_RELATIVE_URIS, false)

      var rdfParser: RDFParser = null
      //rdfParser.setParserConfig(config)

      if (df.format.isRDF() && (df.format.asInstanceOf[RDFBased].rio != null)) {


        val rioformat: RDFFormat = df.format.asInstanceOf[RDFBased].rio
        rdfParser = Rio.createParser(rioformat)

        if (df.format.lineBased) {

          val (lines, all, good, bad) = df.getInputStream().apply { in =>
            LineBasedRioDebugParser.parse(in, rdfParser)
          }

          thisResource.addProperty(model.createProperty(prefixParse + "lines"), lines.toString);
          thisResource.addProperty(model.createProperty(prefixParse + "triples"), all.toString);
          thisResource.addProperty(model.createProperty(prefixParse + "valid"), good.toString);
          thisResource.addProperty(model.createProperty(prefixParse + "errors"), bad.size.toString);
          //parseLog.append(s"Lines: $lines\nTriples: $all\nValid: $good\nErrors: ${bad.size}\n")

          if (bad.isEmpty) {
            parseLog.append(s"Success, no errors")
          } else {
            parseLog.append(s"Errors found, ${bad.size}")
            details.append(s"\n#Error details for $datafile\n#${bad.mkString("\n#")}\n")
          }
        } else {
          val (success, errors) = df.getInputStream().apply { in =>
            RioOtherParser.parse(in, rdfParser)
          }

          if (success) {
            parseLog.append(s"Success, no errors")
          } else {
            parseLog.append(s"Errors found: $errors\n")
          }


        }
      }
      else {
        parseLog.append("currently only RDF parsing is implemented, skipped")
      }

      // parselog
      thisResource.addProperty(model.createProperty(prefixParse + "parselog"), parseLog.toString);
      val baos = new ByteArrayOutputStream()
      model.write(baos, RDFLanguages.strLangTurtle)
      locations.buildParselogFile.appendByteArray(baos.toByteArray)
      locations.buildParselogFile.append(details.toString())
      getLog.info(parseLog)
    })

    getLog.info(s"Parselog written to ${locations.prettyPath(locations.buildParselogFile)}")
  }

  def strict(reason: String): Unit = {
    if (strict) {
      getLog.error(s"[strict==true] failing reason: ${reason}")
      System.exit(-1)
    } else {
      getLog.warn(reason)
    }
  }


  def getVersionsToValidate(): immutable.Seq[(String, File, List[File], List[FilenameHelpers], List[Datafile])] = {
    val versions: mutable.SortedSet[String] = mutable.SortedSet(version)

    // add allVersions to the set
    if (allVersions) {
      versions.++=(inputDirectory.listFiles()
        .filter(_.isDirectory)
        .filter(!_.getName.startsWith("target"))
        .map(f => {
          f.toString.replace(inputDirectory.toString + "/", "")
        }).toSet)
      getLog.info(s"[databus.test.allVersion=true] $artifactId found ${versions.size} version(s): ${versions.mkString(", ")}\n")
    }

    //val versions: mutable.SortedSet[String] = mutable.SortedSet(dataInputDirectory.toString.replace(dataInputDirectoryParent.toString, ""))

    // collect all information
    val versionDirs: immutable.Seq[(String, File, List[File], List[FilenameHelpers], List[Datafile])] = versions.toList.flatMap(v => {
      val versionDir: File = new File(inputDirectory, v)
      if (versionDir.exists && versionDir.isDirectory && versionDir.listFiles().nonEmpty) {

        val wrongFiles = versionDir.listFiles.filterNot(_.getName.startsWith(artifactId)).toList
        if (wrongFiles.nonEmpty) {
          strict(s"$artifactId ${wrongFiles.mkString(s" not starting with $artifactId\n")}")
        }

        var fileList: List[File] = versionDir.listFiles
          .filter(_.isFile)
          .filter(_.getName.startsWith(artifactId))
          //.filter(_ != locations.prepareDataIdFile.name)
          //TODO .filter(_ != getParseLogFile())
          .toList

        val filenameHelpers: List[FilenameHelpers] = fileList.map(f => {
          new FilenameHelpers(f)(getLog)
        })

        val datafiles: List[Datafile] = fileList.map(f => {

          val df = Datafile(f)(getLog).ensureExists()
          if (detailed) {
            df.updateFileMetrics()
          }
          df
        })
        Some((v, versionDir, fileList, filenameHelpers, datafiles))
      } else {
        strict(s"$artifactId empty or not existing directory: ${versionDir}")
        None
      }
    })
    versionDirs
  }


  /**
    * validate one or several versions
    * NOTE: UGLY CODE AHEAD
    * REASON: running with -T will scramble the messages, would be much nicer, else
    */
  def validateVersions(versionDirs: immutable.Seq[(String, File, List[File], List[FilenameHelpers], List[Datafile])]) = {


    // now validation starts

    var l: String = ""
    for ((v, dir, fileList: List[File], fileNames, datafiles) <- versionDirs) {
      val md5 = Base64.getEncoder().encodeToString(MessageDigest.getInstance("MD5").digest(fileList.map(f => f.getName).sorted.mkString("").getBytes("UTF-8")))
      l += s"${v} with ${fileList.size} files,  $md5\n"
    }
    getLog.info(s"[${artifactId}] Number of files plus a hash over the sorted filenames:\n" + l)


    l = ""
    for ((v, dir, fileList: List[File], fileNames, datafiles) <- versionDirs) {
      val compFilenames: mutable.SortedSet[String] = mutable.SortedSet()
      fileNames.foreach(f => {
        f.compressionVariantExtensions.foreach(a => {
          compFilenames.add(a)
        })
      })
      val compFile: mutable.SortedSet[String] = mutable.SortedSet()
      datafiles.foreach(f => {
        compFile.add(f.compressionVariant.getOrElse("None"))
      })
      l += s"${v} from file ending: {${compFilenames.mkString(", ")}}, " +
        s"from file {${compFile.mkString(", ")}}\n"
    }
    getLog.info(s"[${artifactId}] Compression:\n" + l)


    l = ""
    for ((v, dir, fileList: List[File], fileNames, datafiles) <- versionDirs) {
      val formfilenames: mutable.SortedSet[String] = mutable.SortedSet()
      fileNames.foreach(f => {
        f.formatVariantExtensions.foreach(a => {
          formfilenames.add(a)
        })
      })
      val formFile: mutable.SortedSet[String] = mutable.SortedSet()
      datafiles.foreach(f => {
        formFile.add(f.format.mimeType)
      })
      l += (s"${v} from file name: {${formfilenames.mkString(", ")}}, from file content {${formFile.mkString(", ")}}\n")
    }
    getLog.info(s"[${artifactId}] Format:\n" + l)

    l = ""

    for ((v, dir, fileList: List[File], fileNames, datafiles) <- versionDirs) {
      val contfilenames: mutable.SortedSet[String] = mutable.SortedSet()
      fileNames.foreach(f => {
        f.contentVariantExtensions.foreach(a => {
          contfilenames.add(a)
        })
      })
      l += (s"${v} from file name: {${contfilenames.mkString(", ")}}\n")
    }
    getLog.info(s"[${artifactId}] ContentVariant:\n" + l)


    l = ""
    for ((v, dir, fileList: List[File], fileNames, datafiles) <- versionDirs) {
      val contfilenames: mutable.SortedSet[String] = mutable.SortedSet()
      fileNames.foreach(f => {
        contfilenames.add(f.filePrefix)
      })
      l += (s"${v} from name: {${contfilenames.mkString(", ")}}\n")
    }
    getLog.info(s"[${artifactId}] Prefix:\n" + l)

    if (detailed) {

      l = ""
      for ((v, dir, fileList: List[File], fileNames, datafiles) <- versionDirs) {
        val contfilenames: mutable.SortedSet[String] = mutable.SortedSet()
        var sorted = 0
        var unsorted = 0
        datafiles.foreach(df => {
          if (df.sorted) {
            sorted += 1
          } else {
            unsorted += 1
            contfilenames.add(df.file.getName)
          }
        })
        l += (s"${v} sorted: ${sorted}, not sorted: ${unsorted} {${contfilenames.mkString(", ").replaceAll(artifactId, "")}}\n")
      }
      getLog.info(s"[${artifactId}] Byte sorted (LC_ALL=C):\n" + l)

      l = ""
      for ((v, dir, fileList: List[File], fileNames, datafiles) <- versionDirs) {
        var duplicates = 0
        val contfilenames: mutable.SortedSet[String] = mutable.SortedSet()

        datafiles.foreach(df => {
          if (df.duplicates > 0) {
            duplicates += df.duplicates
            contfilenames.add(df.file.getName)

          }
        })
        l += (s"${v} duplicates: ${duplicates} in {${contfilenames.mkString(", ").replaceAll(artifactId, "")}}\n")
      }
      getLog.info(s"[${artifactId}] Duplicates:\n" + l)


      l = ""
      for ((v, dir, fileList: List[File], fileNames, datafiles) <- versionDirs) {
        val contfilenames: mutable.SortedSet[String] = mutable.SortedSet()
        datafiles.foreach(df => {
          if (df.nonEmptyLines == 0) {
            contfilenames.add(df.file.getName)
          }
        })
        l += (s"${v} has ${contfilenames.size} empty files:  {${contfilenames.mkString(", ").replaceAll(artifactId, "")}}\n")
      }
      getLog.info(s"[${artifactId}] Empty files:\n" + l)

    }
  }

}
