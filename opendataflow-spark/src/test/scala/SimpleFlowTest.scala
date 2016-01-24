import com.typesafe.config.ConfigFactory
import core.{ PipelineException, Pipeline, PComponent }
import core.pcomponents._
import engine.SparkEngine
import org.scalatest._

/**
 * Created by OpenDataFlow on 01/01/16.
 */
class SimpleFlowTest extends FlatSpec with Matchers {
  "a simple flow " should "execute normally" in {
    val b = Pipeline.fromConfig("simpleflow")
    assert(b != null)

    val p = b.compile()
    val se = new SparkEngine(ConfigFactory.load("spark").getConfig("engine.spark"))

    // engine will instantiate all the delegates
    // for each task
    val job = se.createJob(p, ConfigFactory.parseString(""" """))
    val res = se.submit(job)

  }
}
