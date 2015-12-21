package core.pcomponents

import core.{ InputConnector, PComponent }
import core.data.FileBasedData

import scala.beans.BeanProperty

class FromSource extends PComponent {
  @BeanProperty
  var url: String = null

  addConnector(InputConnector("source", new FileBasedData, "Read data from a source"))

  override def run: Unit = ???
}

object FromSource {
  /**
   * Factory for sources, binding a data type to a concrete type.
   * // TODO: for now this implements only file sources
   * @param s
   */
  def apply(s: String): FromSource = {
    val source = new FromSource
    source.setUrl(s)
    return source
  }
}