import com.typesafe.config.ConfigFactory
import core.Pipeline
import core.PComponent

import collection.mutable.Stack
import org.scalatest._
import core.pcomponents._

class CoreTest extends FlatSpec with Matchers {

  "A Pipeline" should "be built like this" in {
    val config = ConfigFactory.load("testconfig1")
    val p = Pipeline.create("mypipeline")
      .addComponent(PComponent.fromConfig(config, "readUserData")) // FromSource
      .addComponent("countWords", new WordCountPComponent())
      .addComponent("saveToDisk", ToSink("hdfs:///tmp/out"))
      .connect("readUserData", "source", "countWords", "sink")

  }
  //
  //  it should "throw IncompletePipelineException if missing connections" in {
  //    val emptyStack = new Stack[Int]
  //    a[NoSuchElementException] should be thrownBy {
  //      emptyStack.pop()
  //    }
  //  }
}

