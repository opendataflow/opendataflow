package core.pcomponents

import core.{ InputConnector, OutputConnector, PComponent }
import core.data.{ BoundData, FileBasedData }

import scala.beans.BeanProperty

class ToSink extends PComponent {
  def this(s: String) = {
    this()
    url = s
  }

  @BeanProperty
  var url: String = null
  addConnector(InputConnector("sink", new BoundData, "Writes data somewhere"))

  override def run: Unit = ???
}

object ToSink {
  def apply(s: String) = {
    new ToSink(s)
  }
}
