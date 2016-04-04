package cli.printer

import java.io.{ OutputStreamWriter, OutputStream }

import core._

/**
 * Created by OpenDataFlow on 31/12/15.
 */
class PlainTextPrinter extends Printer {
  override def renderPipeline(p: Pipeline, o: OutputStream): Unit = {
    val w = new OutputStreamWriter(o)
    w.write(s"Pipeline: ${p.name}\n")
    renderComponent(p, 0, w)
    w.flush()
  }

  def renderComponent(c: PComponent, indent: Int, w: OutputStreamWriter, cnn: Set[CConnection] = Set.empty): Unit = {
    // closure to print indented
    def p(s: String) = { w.write("  " * indent + s + "\n") }
    p("-" * 50)
    p(s"Component: ${c.getComponentPathAsString()}")
    if (c.getInputConnectors().size > 0) {
      p(s"Input connectors: ")
      c.getInputConnectors().map { x ⇒
        renderConnector(x, indent + 1, w)
        if (!cnn.isEmpty) {
          val mycnn = cnn.filter(p ⇒ p.source == c)
          if (!mycnn.isEmpty) p(s"Connected to " +
            mycnn.map { f ⇒ f.destination.getId }.mkString(", "))
        }
      }
    }
    if (c.getOutputConnectors().size > 0) {
      p(s"Output connectors:")
      c.getOutputConnectors().map { x ⇒
        renderConnector(x, indent + 1, w)
        if (!cnn.isEmpty) {
          val mycnn = cnn.filter(p ⇒ p.source == c)
          if (!mycnn.isEmpty) p(s"Connected to " +
            mycnn.map { f ⇒ f.destination.getId }.mkString(", "))
        }
      }
    }

    // render all subcomponents
    if (c.isInstanceOf[CompositePComponent]) {
      val cc = c.asInstanceOf[CompositePComponent]
      cc.asInstanceOf[CompositePComponent].components.foreach {
        sc ⇒ renderComponent(sc, indent + 1, w, cc.connections.toSet)
      }
    }
  }

  def renderConnector(c: PConnector, indent: Int, w: OutputStreamWriter) = {
    def p(s: String) = { w.write("  " * indent + s + "\n") }

    p(s"Name: ${c.getName}")
    p(s"Class: ${c.getData.getClass}")
    p(s"Description: ${c.getDescription}")

  }
}
