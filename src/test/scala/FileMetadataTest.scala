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

  test("file parameters/metadata test for bz2 test file: FileParamTest.nt.bz2") {
    //val df = Datafile(File("src/main/resources/FileParamTest.nt.bz2").toJava)(new SystemStreamLog())
    val testFile = new File(getClass.getClassLoader.getResource("filestat/basic.nt.bz2").getFile)

    val df = Datafile(testFile)(new SystemStreamLog())
    df.updateFileMetrics();
    assert(df.sha256sum==="1ce31e72c9553e8aa3ed63acd22f3046321a0df2d8ecb85b59af28f5bfb3cbd7" , "sha256sum is calculated wrong")
    assert(df.nonEmptyLines === 6 , "non-empty lines count is calculated wrong")
    assert(df.duplicates === 2, "duplicate lines count is calculated wrong")
    assert(df.sorted === true, "sorted lines count is calculated wrong")
    print(df.toString)
    assert(df.bytes === 323, "bytes count is calculated wrong")
    assert(df.uncompressedByteSize === 734, "uncompressedByteSize is calculated wrong")
  }
/*
  test("file parameters/metadata test for plain NT test file: FileParamTest.nt") {
    val df = Datafile(File("src/main/resources/filestats/FileParamTest.nt").toJava)(new SystemStreamLog())
    df.updateFileMetrics();
    assert(df.sha256sum==="c9a4f5e0aaf1b04dc9646be0fe6d47cf676d6b031d29c20bbf03ea85f450a216" , "sha256sum is calculated wrong")
    assert(df.nonEmptyLines === 6 , "non-empty lines count is calculated wrong")
    assert(df.duplicates === 2, "duplicate lines count is calculated wrong")
    assert(df.sorted === true, "sorted lines count is calculated wrong")
    print(df.toString)
    assert(df.bytes === 734, "bytes count is calculated wrong")
    assert(df.uncompressedByteSize === 734, "uncompressedByteSize is calculated wrong")
  }
*/
}
