import better.files.File
import org.apache.maven.plugin.logging.SystemStreamLog
import org.dbpedia.databus.lib.Datafile
import org.scalatest.FunSuite

class FileMetadataTest extends FunSuite {

  test("file parameters/metadata test for gz test file: FileParamTest.nt.bz2") {
    val df = Datafile(File("src/main/resources/FileParamTest.nt.bz2").toJava)(new SystemStreamLog())
    df.updateFileMetrics();
    assert(df.sha256sum==="1ce31e72c9553e8aa3ed63acd22f3046321a0df2d8ecb85b59af28f5bfb3cbd7" , "sha256sum is calculated wrong")
    assert(df.nonEmptyLines === 6 , "non-empty lines count is calculated wrong")
    assert(df.duplicates === 2, "duplicate lines count is calculated wrong")
    assert(df.sorted === true, "sorted lines count is calculated wrong")
    print(df.toString)
    assert(df.bytes === 323, "bytes count is calculated wrong")
    assert(df.uncompressedByteSize === 734, "uncompressedByteSize is calculated wrong")
  }

  test("file parameters/metadata test for plain NT test file: FileParamTest.nt") {
    val df = Datafile(File("src/main/resources/FileParamTest.nt").toJava)(new SystemStreamLog())
    df.updateFileMetrics();
    assert(df.sha256sum==="108f3aff9389f8c83e398db0f354e6b3555fdae686bd8908c9b1172c1aa83697" , "sha256sum is calculated wrong")
    assert(df.nonEmptyLines === 6 , "non-empty lines count is calculated wrong")
    assert(df.duplicates === 2, "duplicate lines count is calculated wrong")
    assert(df.sorted === true, "sorted lines count is calculated wrong")
    print(df.toString)
    assert(df.bytes === 734, "bytes count is calculated wrong")
    assert(df.uncompressedByteSize === 734, "uncompressedByteSize is calculated wrong")
  }

}
