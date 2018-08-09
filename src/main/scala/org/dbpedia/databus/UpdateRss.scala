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
@Mojo(name = "update-rss", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
class UpdaeRss extends AbstractMojo with Properties {

  val types: List[String] = List("rss_0.9", "rss_0.91", "rss_0.92", "rss_0.93", "rss_0.94", "rss_1.0", "rss_2.0", "atom_0.3")

  @throws[MojoExecutionException]
  override def execute(): Unit = {

    //skip the parent module for now
    if (isParent()){
      getLog.info("skipping parent module")
      return
    }

    val feedDirectory: File  = new File(mavenTargetDirectory+"/"+artifactId)
//    var feedInit = initializationFeed

    if( !feedDirectory.exists()) {
      feedDirectory.mkdirs()
    }

    val input = new SyndFeedInput
    var fd:SyndFeed = null

    // init URL, init file or existing file
    var feedFile: File = new File (feedDirectory+"/feed.xml");
    // TODO remove
    if(feedFile.exists()) {
      fd = input.build(feedFile)
//    } else if( feedInit.startsWith("http://")){
//      fd = input.build(new InputStreamReader(new URL(feedInit).openStream))
    } else {
//      fd = input.build(new File(feedInit))
      fd = new SyndFeedImpl()
    }
    // channel setup based on pom
    fd.setTitle(artifactId)
    fd.setFeedType(types.apply(5))
    fd.setAuthor(maintainer.toString)
    fd.setDescription(datasetDescription)
    var categories: util.ArrayList[SyndCategory] = new util.ArrayList[SyndCategory]()
    var cat = new SyndCategoryImpl()
    cat.setName("Databus and TODO")
    categories.add(cat)
    fd.setCategories(categories)
    fd.setCopyright("Copyright TODO")
    // TODO which link
    fd.setLink("path/to/dataid_catalog.ttl")

    // check if already included
    var isDone: Boolean = false
    for( syndentry: SyndEntry <- fd.getEntries() ) {
      if( syndentry.getTitle.contentEquals(finalName)) isDone = true
    }

    //skip if already updated
    if(!isDone) {
      var entries: util.List[SyndEntry] = fd.getEntries()
      var entry: SyndEntry = new SyndEntryImpl()

      entry.setTitle(finalName)

      // path to dataid 
      val dataidPath: Path = Paths.get(dataIdDirectory+"/"+artifactId+"-"+version+"-dataid.ttl")
      val feedPath: Path = Paths.get(mavenTargetDirectory+"/"+artifactId+"/")
      val relative = feedPath.relativize(dataidPath)
      entry.setLink(relative.toString)

      val format = new java.text.SimpleDateFormat("yyyy-MM-dd")
      entry.setPublishedDate(format.parse(modifiedDate))

      // changelog file ?
      var description: SyndContent = new SyndContentImpl()
      description.setType("text/plain")
      description.setValue("TODO data from changelog")
      entry.setDescription(description)

      entries.add(entry)
      fd.setEntries(entries)


      val output = new SyndFeedOutput
      output.output(fd, new FileWriter(feedFile))
    }
  }
}
