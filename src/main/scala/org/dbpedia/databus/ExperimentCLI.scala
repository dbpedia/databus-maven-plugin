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

import java.io.{FileWriter, InputStreamReader}
import java.net.URL
import java.util

import com.rometools.rome.feed.synd._
import com.rometools.rome.io.{SyndFeedInput, SyndFeedOutput}

import scala.collection.mutable

/**
  * a junk class to test some code
  */
object ExperimentCLI {

  val knownCompressionFileEndings = mutable.HashSet[String]("gz", "bz2")


  def main(args: Array[String]): Unit = {


    val feedUrl = new URL("http://www.bbc.co.uk/feeds/rss/music/latest_releases.xml")
    val input = new SyndFeedInput
    val feed = input.build(new InputStreamReader(feedUrl.openStream))
    val types: List[String] = List("rss_0.9", "rss_0.91", "rss_0.92", "rss_0.93", "rss_0.94", "rss_1.0", "rss_2.0", "atom_0.3")
    feed.setFeedType(types.apply(5))
    var entries: util.ArrayList[SyndEntry] = new util.ArrayList[SyndEntry]()
    var entry: SyndEntry = new SyndEntryImpl()
    entry.setTitle("Rome v1.0")
    entry.setLink("http://wiki.java.net/bin/view/Javawsxml/Rome01")
    //entry.setPublishedDate(DATE_PARSER.parse("2004-06-08"))
    var description: SyndContent = new SyndContentImpl()
    description.setType("text/plain")
    description.setValue("Initial release of Rome")
    entry.setDescription(description)
    entries.add(entry)
    entry = new SyndEntryImpl()
    entry.setTitle("Rome v3.0")
    entry.setLink("http://wiki.java.net/bin/view/Javawsxml/Rome03")
    //entry.setPublishedDate(DATE_PARSER.parse("2004-07-27"))
    description = new SyndContentImpl()
    description.setType("text/html")
    description.setValue("<p>More Bug fixes, mor API changes, some new features and some Unit testing</p>" +
      "<p>For details check the <a href=\"http://wiki.java.net/bin/view/Javawsxml/RomeChangesLog#RomeV03\">Changes Log</a></p>")
    entry.setDescription(description)
    entries.add(entry)
    feed.setEntries(entries)

    val output = new SyndFeedOutput
    output.output(feed, new FileWriter("/tmp/test.xml"))

    System.exit(0)


    val contentVariantSeparator = "_"
    val fileEndingSeparator = "."

    val files = List[String](
      "infobox-properties_dewiki.tql",
      "infobox-properties_dewiki.tql.bz2",
      "infobox-properties_dewiki.ttl",
      "infobox-properties_dewiki.ttl.bz2",
      "infobox-properties_enwiki.tql",
      "infobox-properties_enwiki.tql.bz2",
      "infobox-properties_enwiki.ttl",
      "infobox-properties_enwiki.ttl.bz2"
    )

    val artifactId = "infobox-properties"
    val contentVariants = List[String](

    )

    for (file <- files) {
      var f = file

      // check for matching artifactId
      if (file.startsWith(artifactId)) {
        //good
        f = file.replace(artifactId, "").replace("_", "")
      }

      var cv = f.substring(0, f.indexOf(fileEndingSeparator))
      var endings = mutable.HashSet[String]()
      for (s <- f.substring(f.indexOf(fileEndingSeparator)).split(fileEndingSeparator)) {
        endings += s
      }
      endings --= knownCompressionFileEndings


      System.out.println(cv)
      System.out.println(endings)

    }


  }

}
