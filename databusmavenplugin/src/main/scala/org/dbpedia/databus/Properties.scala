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

import java.io.File
import java.net.URL
import java.util

import org.apache.maven.plugins.annotations.Parameter

trait Properties {

  @Parameter(defaultValue = "${project.artifactId}", readonly = true)
  val artifactId: String = ""

  @Parameter(defaultValue = "${maven.multiModuleProjectDirectory}", readonly = true)
  val multiModuleBaseDirectory: String = ""

  @Parameter(defaultValue = "${project.packaging}", readonly = true)
  val packaging: String = ""

  @Parameter(defaultValue = "${project.build.outputDirectory}", readonly = true)
  val outputDirectory: String = ""

  @Parameter(defaultValue = "${project.build.directory}", readonly = true)
  val targetDirectory: String = ""

  @Parameter(defaultValue = "${project.version}", readonly = true)
  val version: String = ""

  @Parameter var maintainer: URL = _
  @Parameter var privateKeyFile: File = _
  @Parameter val resourceDirectory: String = ""

  //@Parameter var contentVariants:util.ArrayList[ContentVariant] = null
  //@Parameter var contentVariants: util.ArrayList[String] = _
  //@Parameter var formatVariants: util.ArrayList[String] = _



}
