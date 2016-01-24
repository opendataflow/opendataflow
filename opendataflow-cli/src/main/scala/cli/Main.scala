package cli

import java.io.FileOutputStream

import cli.printer.Printer
import core.Pipeline
import scopt._
/**
 * Created by OpenDataFlow on 27/12/15.
 */
object Main {
  import java.io.File
  case class CliConfig(configFile: String = "",
                       printer: String = "text",
                       outfile: String = "")

  val parser = new scopt.OptionParser[CliConfig]("scopt") {
    head("odq", "1.0")
    opt[String]('c', "config") action { (x, c) ⇒
      c.copy(configFile = x)
    } text ("config - file with pipeline config")
  }

  def main(args: Array[String]) = {
    var (cmd, ar) = if (args.length > 0) {
      (args.head.toLowerCase, args.tail)
    } else {
      ("print", args)
    }

    // parser.parse returns Option[C]
    parser.parse(ar, CliConfig()) match {
      case Some(config) ⇒ cmd match {
        case "print" ⇒ doPrint(config)
        case "graph" ⇒ doGraph(config)
        case "run"   ⇒ doRun(config)
      }

      case None ⇒
      // arguments are bad, error message will have been displayed
    }
  }

  private def getPipelineFromOptions(c: CliConfig): Pipeline = {
    return Pipeline.fromConfig(c.configFile).compile()
  }
  /**
   * prints the details of a pipeline
   * @param c
   */
  def doPrint(c: CliConfig) = {
    val p = getPipelineFromOptions(c)

    val printer = Printer.getPrinter(c.printer)

    val outstream = if (c.outfile.isEmpty) {
      System.out
    } else {
      new FileOutputStream(c.outfile)
    }

    printer.renderPipeline(p, outstream)
  }

  def doGraph(c: CliConfig) = {
    doPrint(c.copy(printer = "dot"))
  }

  def doRun(c: CliConfig) = {

  }

}
