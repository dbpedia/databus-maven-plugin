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

import collection.JavaConversions._
import com.rometools.rome.feed.synd._
import com.rometools.rome.io.{SyndFeedInput, SyndFeedOutput}
import org.apache.maven.plugin.{AbstractMojo, MojoExecutionException}
import org.apache.maven.plugins.annotations.{LifecyclePhase, Mojo, Parameter}


/**
  * TODO use right links to dataid (catalog)
  */
@Mojo(name = "update-rss", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
class UpdateRss extends AbstractMojo with Properties {

  // rometools offers different options:
  // "rss_0.9", "rss_0.91", "rss_0.92", "rss_0.93", "rss_0.94", "rss_1.0", "rss_2.0", "atom_0.3"
  val feedType = "rss_1.0"

  @throws[MojoExecutionException]
  override def execute(): Unit = {

    //skip the parent module for now
    if (isParent()) {
      getLog.info("skipping parent module")
      return
    }

    val input = new SyndFeedInput
    var newFeed: SyndFeed = new SyndFeedImpl()

    // TODO make this configurable
    // TODO this can also be a weburl
    var oldReleaseFeedFile = new File("/dev/null/feed.xml")
    var oldFeed: SyndFeed = null
    if (oldReleaseFeedFile.exists()) {
      oldFeed = input.build(oldReleaseFeedFile)
    }

    // the feedfile, where output is written
    var newFeedFile: File = new File(getFeedDirectory, "/feed.xml")

    // create the new entry
    var entry: SyndEntry = new SyndEntryImpl()
    var title = finalName
    entry.setTitle(finalName)
    // path to dataid
    val dataidPath: Path = Paths.get(getDataIdDirectory + "/" + artifactId + "-" + version + "-dataid.ttl")
    val feedPath: Path = Paths.get(mavenTargetDirectory + "/" + artifactId + "/")
    val relative = feedPath.relativize(dataidPath)
    entry.setLink(relative.toString)
    entry.setPublishedDate(new java.text.SimpleDateFormat("yyyy-MM-dd").parse(modifiedDate))

    // todo changelog file ?
    var description: SyndContent = new SyndContentImpl()
    description.setType("text/plain")
    description.setValue("TODO data from changelog")
    entry.setDescription(description)

    if (oldFeed != null) {
      newFeed = oldFeed
      // check if already included
      // todo check if criteria after implementing
      var hasEntry = false
      // TODO implement the scala way
      for (syndentry: SyndEntry <- oldFeed.getEntries()) {
        if (syndentry.getTitle.contentEquals(title)) {
          getLog.info(s"${title} already in feed")
        }
      }

      // add new Entry
      if (!hasEntry) {
        newFeed.getEntries.add(entry)
      }



      // generate new feed
    } else {

      // channel setup based on pom
      newFeed.setTitle(artifactId)
      newFeed.setFeedType(feedType)
      newFeed.setAuthor(maintainer.toString)
      newFeed.setDescription(datasetDescription)
      newFeed.setCopyright("Copyright TODO")
      // TODO which link
      newFeed.setLink("path/to/dataid_catalog.ttl")

      // todo categories
      var categories: util.ArrayList[SyndCategory] = new util.ArrayList[SyndCategory]()
      var cat = new SyndCategoryImpl()
      cat.setName("Databus and TODO")
      categories.add(cat)
      newFeed.setCategories(categories)

      // create the entrylist and connect with feed
      var entries: util.List[SyndEntry] = newFeed.getEntries()
      newFeed.setEntries(entries)


      entries.add(entry)
    }

    // write
    val output = new SyndFeedOutput
    output.output(newFeed, new FileWriter(newFeedFile))


  }
}
