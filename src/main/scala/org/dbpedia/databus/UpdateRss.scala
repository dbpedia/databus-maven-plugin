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

import java.io._
import java.net.URL
import java.nio.file.{Path, Paths}
import java.util

import com.rometools.rome.feed.synd._
import com.rometools.rome.io.{SyndFeedInput, SyndFeedOutput}
import org.apache.maven.plugin.MojoExecutionException

import scala.collection.JavaConverters._


/**
  * TODO use right links to dataid (catalog)
  */
@deprecated(message = "RSS will be generated from the SPARQL API", since = "early days")
class UpdateRss extends DatabusMojo {

  // rometools offers different options:
  // "rss_0.9", "rss_0.91", "rss_0.92", "rss_0.93", "rss_0.94", "rss_1.0", "rss_2.0", "atom_0.3"
  val feedType = "rss_1.0"

  def getFeedDirectory() :File = {null}
  def getFeedFile() :File = {null}

  @throws[MojoExecutionException]
  override def execute(): Unit = {

    val feedFrom =""
    //skip the parent module for now
    if (isParent()) {
      getLog.info("skipping parent module")
      return
    }

    val input = new SyndFeedInput
    var newFeed: SyndFeed = new SyndFeedImpl()

    // TODO make this configurable
    // TODO this can also be a weburl
    var oldFeed: SyndFeed = null
    // use oldFeed from feedDirectory
    if( feedFrom.contentEquals("")) {
      oldFeed = fromFeedDirectory()

      // use oldFeed from url
      // TODO use form url only as init
    } else if ( feedFrom.startsWith("http://")) {
      try {
        oldFeed = input.build(new InputStreamReader(new URL(feedFrom).openStream))
      } catch {
        case e: Exception => {
          getLog.info("old feed not found")
          oldFeed = fromFeedDirectory()
        }
      }

      // use oldFeed from file
      // TODO use from file only as init
    } else {
      try {
        oldFeed = input.build(new File(feedFrom))
      } catch {
        case fne: FileNotFoundException => {
          getLog.info("old feed not found")
          oldFeed = fromFeedDirectory()
        }
      }
    }

    // create the new entry
    var entry: SyndEntry = new SyndEntryImpl()
    var title = proj.getBuild.getFinalName
    entry.setTitle(title)
    // path to dataid
    //val dataidPath: Path = Paths.get(getDataIdFile().toString)
    val dataidPath: Path = null
    val feedPath: Path = Paths.get(getFeedDirectory.toString)
    val relative = feedPath.relativize(dataidPath)
    entry.setLink(relative.toString)

    // published date
    entry.setPublishedDate(new java.text.SimpleDateFormat("yyyy-MM-dd").parse(modifiedDate))

    // Todo changelog
    var description: SyndContent = new SyndContentImpl()
    description.setType("text/plain")
    description.setValue("TODO data from changelog")
    entry.setDescription(description)

    if (oldFeed != null) {
      newFeed = oldFeed

      // check if already included, otherwise add entry
      oldFeed.getEntries.asScala.find(_.getTitle.contentEquals(title)) match {
        case Some(titleMatch) => getLog.info(s"$title already in feed")
        case None => newFeed.getEntries.add(entry)
      }

      // generate new feed
    } else {

      // channel setup based on pom
      newFeed.setTitle(artifactId)
      newFeed.setFeedType(feedType)
      newFeed.setAuthor(maintainer.toString)
      //newFeed.setDescription(datasetDescription)
      // TODO copyright
      newFeed.setCopyright("Copyright TODO")
      // TODO channel link
      newFeed.setLink("path/to/dataid_catalog.ttl")
      // Todo categories
      var categories: util.ArrayList[SyndCategory] = new util.ArrayList[SyndCategory]()
      var cat = new SyndCategoryImpl()
      cat.setName("Databus and TODO")
      categories.add(cat)
      newFeed.setCategories(categories)

      // create the entrylist and connect with feed
      var entries: util.List[SyndEntry] = newFeed.getEntries
      entries.add(entry)
      newFeed.setEntries(entries)

    }

    // write
    // tmpFeed workaround to add new items/seq/li/@resource on the first feed write
    val output = new SyndFeedOutput
    var tmpFeed:Writer = new StringWriter()
    output.output(newFeed,tmpFeed)
    newFeed = input.build(new StringReader(tmpFeed.toString))
    output.output(newFeed, new FileWriter(getFeedFile()))
  }

  // returns old feed from feedDirectory if already exists
  def fromFeedDirectory() : SyndFeed = {
    val input = new SyndFeedInput
    var oldReleaseFeedFile: File = getFeedFile()
    if (oldReleaseFeedFile.exists()) {
      return input.build(oldReleaseFeedFile)
    } else {
      return null
    }
  }
}
