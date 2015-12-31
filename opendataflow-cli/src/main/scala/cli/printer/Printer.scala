package cli.printer

import java.io.OutputStream

import core.Pipeline

/**
 * Created by OpenDataFlow on 31/12/15.
 */
object Printer {
  def getPrinter(name: String): Printer = {
    name match {
      case "text" ⇒ new PlainTextPrinter()
      case "dot"  ⇒ new DotPrinter()
      // if we don't have it let's assume it's a class name
      case _ ⇒
        val c = Class.forName(name)
        if (c.getInterfaces.contains(Printer.getClass)) {
          return c.newInstance().asInstanceOf[Printer]
        } else {
          throw new Exception(s"Class ${name} does not extend Printer")
        }
    }
  }
}

trait Printer {
  def renderPipeline(p: Pipeline, o: OutputStream)
}
