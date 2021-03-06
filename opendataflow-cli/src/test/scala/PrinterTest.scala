import java.io.ByteArrayOutputStream

import cli.printer.Printer
import com.typesafe.config.ConfigFactory
import core.{ PipelineException, Pipeline, PComponent }

import org.scalatest._
import core.pcomponents._

class PrinterTest extends FlatSpec with Matchers {
  "a pipeline printer" should "describe a pipeline in text" in {
    // load using file name
    val b = Pipeline.fromConfig("pipeline-t1")
    val p = b.compile()

    val printer = Printer.getPrinter("text")

    val s = new ByteArrayOutputStream()

    printer.renderPipeline(p, s)
  }
  "a pipeline printer " should "describe a pipeline in dot" in {
    val b = Pipeline.fromConfig("pipeline-t1")
    val p = b.compile()

    val printer = Printer.getPrinter("dot")

    val s = new ByteArrayOutputStream()

    printer.renderPipeline(p, s)

    val output = s.toString()
    // just count that there are as many open parenthesis as closed.
    // not sure how to debug dot output...
    assert(output.filter { _ == "{" }.size == output.filter(_ == "}").size)
  }
}
