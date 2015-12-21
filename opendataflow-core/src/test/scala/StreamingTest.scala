import core.Pipeline
import core.pcomponents.{ FromSource, ToSink }
import org.scalatest.{ Matchers, FlatSpec }

import scala.collection.mutable.Stack

class StreamingTest extends FlatSpec with Matchers {

  "A Pipeline" should "be built like this" in {
    val p = Pipeline.create("mypipeline")
      .addComponent("readUserData", FromSource("hdfs:///tmp/data"))
      .addComponent("countWords", new WordCountPComponent())
      .addComponent("saveToDisk", ToSink("hdfs:///tmp/out"))
      .connect("readUserData", "source", "countWords", "sink")
      .connect("countWords", "saveToDisk")

  }

  //  it should "throw IncompletePipelineException if missing connections" in {
  //    val emptyStack = new Stack[Int]
  //    a[NoSuchElementException] should be thrownBy {
  //      emptyStack.pop()
  //    }
  //  }
}