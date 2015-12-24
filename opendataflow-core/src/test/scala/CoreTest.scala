import com.typesafe.config.ConfigFactory
import core.{ PipelineException, Pipeline, PComponent }

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
      // one connection is done automatically, since pipeline will connect source->sink with the same name
      .compile()

  }

  "An incomplete pipeline" should "throw an exception when compiled" in {
    val config = ConfigFactory.load("testconfig1")
    intercept[PipelineException] {
      val p = Pipeline.create("mypipeline")
        .addComponent(PComponent.fromConfig(config, "readUserData")) // FromSource
        .addComponent("countWords", new WordCountWithStopWordsPComponent()) //
        .addComponent("saveToDisk", ToSink("hdfs:///tmp/out"))
        .compile()
    }

  }
  //
  //  it should "throw IncompletePipelineException if missing connections" in {
  //    val emptyStack = new Stack[Int]
  //    a[NoSuchElementException] should be thrownBy {
  //      emptyStack.pop()
  //    }
  //  }
}

