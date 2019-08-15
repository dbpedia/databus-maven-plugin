/*-
 * #%L
 * DBpedia Databus Maven Plugin
 * %%
 * Copyright (C) 2018 - 2019 Sebastian Hellmann (on behalf of the DBpedia Association)
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
import java.io.File

import org.apache.maven.plugin.logging.SystemStreamLog
import org.dbpedia.databus.lib.Datafile
import org.scalatest.FunSuite

class FileMetadataTest extends FunSuite  {

  test("basic fixed value file parameters/metadata test for bz2 test file: basic.nt.bz2") {
    val df =datafile("filestat/basic.nt.bz2")
    assert(df.sha256sum==="1ce31e72c9553e8aa3ed63acd22f3046321a0df2d8ecb85b59af28f5bfb3cbd7" , "sha256sum is calculated wrong")
    assert(df.nonEmptyLines === 6 , "non-empty lines count is calculated wrong")
    assert(df.duplicates === 2, "duplicate lines count is calculated wrong")
    assert(df.sorted === true, "sorted lines count is calculated wrong")
    assert(df.bytes === 323, "bytes count is calculated wrong")
    assert(df.uncompressedByteSize === 734, "uncompressedByteSize is calculated wrong")
  }

  test("testing sort order US Sorted vs. ASCII") {
    var df = datafile("filestat/sorttest_us.ttl")
    assert(df.sorted === false, "sorted lines count is calculated wrong")
    df  = datafile("filestat/sorttest_ascii.txt")
    assert(df.sorted === true, "sorted lines count is calculated wrong")

  }

  def datafile(resourcename:String): Datafile = {
    val testFile = new File(getClass.getClassLoader.getResource(resourcename).getFile)

    val df = Datafile(testFile)(new SystemStreamLog())
    df.updateFileMetrics();
    print(df.toString)
    df

  }

}
