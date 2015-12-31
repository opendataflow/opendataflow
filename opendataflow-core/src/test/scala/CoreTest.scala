import com.typesafe.config.ConfigFactory
import core.{ PipelineException, Pipeline, PComponent }

import org.scalatest._
import core.pcomponents._
import test.{ WordCountWithStopWordsPComponent, WordCountPComponent }

class CoreTest extends FlatSpec with Matchers {

  "A Pipeline" should "be built programmatically" in {
    val config = ConfigFactory.load("testconfig1")
    val p = Pipeline.create("mypipeline")
      .addComponent(PComponent.fromConfig(config, "readUserData")) // FromSource
      .addComponent("countWords", new WordCountPComponent())
      .addComponent("saveToDisk", ToSink("hdfs:///tmp/out"))
      .connect("readUserData", "source", "countWords", "sink")
      // one connection is done automatically, since pipeline will connect source->sink with the same name
      .compile()

  }

  "A Pipeline" should "be built from config" in {
    // load using file name
    val b = Pipeline.fromConfig("pipeline1")
    val p = b.compile()
    assertResult(3)(p.components.length)

    // load using config object
    val config = ConfigFactory.load("pipeline1")
    val p2 = Pipeline.fromConfig(config).compile()
    assertResult(3)(p.components.length)
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

}

