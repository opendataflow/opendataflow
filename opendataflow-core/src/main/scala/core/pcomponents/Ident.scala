package core.pcomponents

import core.data.{ AbstractData, BoundData }
import core.{ InputConnector, OutputConnector, PComponent }

/**
 * Created by OpenDataFlow on 31/12/15.
 */
class Ident extends PComponent {

  addConnector(OutputConnector("sink", new AbstractData, "Writes data somewhere"))
  addConnector(InputConnector("source", new AbstractData, "Reads data from somewhere"))
  /**
   * run method is the one that actually performs the work
   */
  override def run(): Unit = ???
}
