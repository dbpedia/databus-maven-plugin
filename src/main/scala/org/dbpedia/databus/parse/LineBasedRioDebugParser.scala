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

package org.dbpedia.databus.parse

import java.io._
import org.eclipse.rdf4j.rio._

import scala.collection.mutable
import scala.io.{Codec, Source}


/**
  * currently supports only Ntriples, Nquad
  */
object LineBasedRioDebugParser {

  val batchSize = 500 * 1000

  /* Error from short and long abstract computepreview
  [INFO] collecting metadata for each file (from parameters in pom.xml and from file itself)
Err: Expected '<' or '_', found: [ [line 1, column 91]: [binary data - no preview]
Err: Unexpected character U+FF3BA at index 32: http://ja.dbpedia.org/resource/!<U+FF3BA>i-ou］ [line 1]: <http://ja.dbpedia.org
/resource/!<U+FF3BA>i-ou］> <http://dbpedia.org/ontology/abstract> "『! [ai-ou]』（アイ・オー）は<U+5E741>日公開の日本映画分、
カラー作品<U+5E744>月現在、<U+672AD>VD化。"@ja .
Err: Unexpected character U+10 at index 31: http://ja.dbpedia.org/resource/^P [line 1]: <http://ja.dbpedia.org/resource/^P> <ht
tp://dbpedia.org/ontology/abstract> "#9110（シャープきゅういちいちまる）は、日本において、生活の安全や悩み事に関する事柄を警察
に相談する目的で設定されている電話番号である。"@ja .
Err: Expected '<' or '_', found: - [line 1, column 45]: -80）。"@ja .
Err: Unexpected character U+12 at index 31: http://ko.dbpedia.org/resource/^R [line 1]: <http://ko.dbpedia.org/resource/^R> <ht
tp://dbpedia.org/ontology/abstract> "^R(Room2012)는 독일의 팝 밴드이다."@ko .
Err: Expected '<' or '_', found: [ [line 1, column 91]: [binary data - no preview]
Err: Expected '<' or '_', found: [ [line 1, column 91]: [binary data - no preview]
Err: Unexpected character U+0 at index 31: http://zh.dbpedia.org/resource/^@年的舱外活动列表 [line 1]: <http://zh.dbpedia.org/r
esource/^@年的舱外活动列表> <http://dbpedia.org/ontology/abstract> "本列表包含了所有的^@年的太空行走以及月表行走；即所有宇航员
完全或部分离开航天器的事件。 舱外活动开始及结束时间均为协调世界时（UTC）时区。"@zh .
Err: Expected '<' or '_', found: [ [line 1, column 91]: [binary data - no preview]
Err: Unexpected character U+F4BE at index 40: http://wa.dbpedia.org/resource/10_d'_oct<U+F4BE> [line 1]: <http://wa.dbpedia.org
/resource/10_d'_oct<U+F4BE>> <http://dbpedia.org/ontology/abstract> "Li 10 d' oct<U+F4BE>, c' est li 283inme djoû d' l' anêye d
o calindrî grigoryin (li 284inme po ls anêyes bizetes); gn è dmeure co 82 djoûs po disk' al fén d' l' anêye."@wa .
Err: Expected '<' or '_', found: [ [line 1, column 91]: [binary data - no preview]

   */

  def parse(in: InputStream, rdfParser: RDFParser): (Integer, Integer, Integer, mutable.HashSet[String]) = {


    var (lines, totalTriples, good, wrongTriples) = (0, 0, 0, new mutable.HashSet[String])

    var batch = new mutable.HashSet[String]
    val it = Source.fromInputStream(in)(Codec.UTF8).getLines()
    var lc = 0


    while (it.hasNext) {
      lc+=1
      val line = it.next().toString
      // batch it
      batch += line
      if (batch.size >= batchSize) {
        //todo better way
        var (a, b, c) = parseBatch(rdfParser, batch)
        totalTriples += a
        good += b
        wrongTriples ++= c
        System.out.println(s"${lc} parsed, more than ${lc%batchSize} duplicates")

        //reset batch
        batch = new mutable.HashSet[String]
      }
    }

    // remaining
    //todo better way
    var (a, b, c) = parseBatch(rdfParser, batch)
    totalTriples += a
    good += b
    wrongTriples ++= c

    (lc, totalTriples, good, wrongTriples)
  }

  def parseBatch(rdfParser: RDFParser, batch: mutable.HashSet[String]): (Integer, Integer, mutable.HashSet[String]) = {
    val (totalTriples, good, wrongTriples) = (0, 0, new mutable.HashSet[String])

    try {
      // hand batch to parser
      val baos: ByteArrayInputStream = new ByteArrayInputStream(batch.mkString("\n").getBytes());
      rdfParser.parse(baos, "")
      baos.close()
      // parsing of batch successfull
    } catch {
      case e: Exception => {
        //parsing failed somewhere, reiterate
        for (line <- batch) {
          try {
            // hand each line to parser
            // todo check whether baos needs to be closed
            val baos: ByteArrayInputStream = new ByteArrayInputStream(line.getBytes());
            rdfParser.parse(baos, "");

          } catch {
            case rio: Exception => {}
              //L.trace("parser error, the problem triple was:\n"+one+" "+rio.getMessage());
              wrongTriples.add("Err: " + rio.getMessage() + ": " + line);
          }
        }
      }
    }
    (batch.size, batch.size - wrongTriples.size, wrongTriples)
  }
}
