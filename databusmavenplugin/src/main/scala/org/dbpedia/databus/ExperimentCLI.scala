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

/**
  * a junk class to test some code
  */
class ExperimentCLI {

  def main(args: Array[String]): Unit = {
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
    val contentVariants = List[String] (

    )

    for (file <- files){
      var f =file

      // check for matching artifactId
      if (file.startsWith(artifactId)){
        //good
        f = file.replace(artifactId,"").replace("_","")
      }

      var cv = f.substring(0,f.indexOf(fileEndingSeparator))
      var endings = f.substring(f.indexOf(fileEndingSeparator))

      System.out.println(cv)
      System.out.println(endings)

    }


  }

}
