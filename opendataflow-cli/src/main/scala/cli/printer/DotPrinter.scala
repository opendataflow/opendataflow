package cli.printer

import java.io.{ OutputStreamWriter, OutputStream }

import core._

/**
 * Created by OpenDataFlow on 31/12/15.
 */
class DotPrinter extends Printer {

  case class ComponentDrawingOptions(boxed: Boolean = false)

  // TODO: check for possible clashes in names after sanitizing
  private def sanitizeName(s: String) = {
    s.replaceAll("\\W", "")
  }

  def renderComponent(p: PComponent, w: OutputStreamWriter, options: ComponentDrawingOptions): Unit = {
    p match {
      case cc: CompositePComponent ⇒
        w.write(s"subgraph ${sanitizeName(p.getComponentPathAsString())} { \n")
        cc.components.foreach { x ⇒ renderComponent(x, w, options) }

        cc.connections.foreach { conn ⇒
          w.write(s"   ${getConnectorId(conn.source, conn.sourceOutput)} -> ${getConnectorId(conn.destination, conn.destinationInput)} ;\n")
        }

        w.write("}\n")
      case c: PComponent ⇒
        w.write(sanitizeName(c.getComponentPathAsString()) + " [shape=box,label=\"" + c.getName() + "\"];\n")
        c.connectors.foreach { cn ⇒
          val id = getConnectorId(c, cn)
          w.write(id + "[style=dotted, label=\"" + cn.getName() + "\"];\n")
          cn match {
            case ic: InputConnector  ⇒ w.write(s"${getComponentId(c)} -> ${id};\n")
            case oc: OutputConnector ⇒ w.write(s"${id} -> ${getComponentId(c)};\n")
          }
        }
    }
  }

  private def getConnectorId(p: PComponent, c: PConnector): String = {
    sanitizeName(p.getComponentPathAsString() + "_" + c.getName)
  }
  private def getConnectorId(p: PComponent, c: String): String = {
    getConnectorId(p, p.getConnector(c, x ⇒ x.getName == c).get)
  }

  private def getComponentId(p: PComponent) = sanitizeName(p.getComponentPathAsString())

  override def renderPipeline(p: Pipeline, o: OutputStream): Unit = {
    val w = new OutputStreamWriter(o)
    w.write(s"digraph G {\n")

    renderComponent(p, w, new ComponentDrawingOptions)

    w.write("}\n")
    w.flush()
  }

}
