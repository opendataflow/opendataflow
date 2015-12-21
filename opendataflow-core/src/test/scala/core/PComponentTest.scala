package core

import com.typesafe.config.ConfigFactory
import core.data.FileBasedData
import collection.mutable.Stack
import org.scalatest._
import core.pcomponents._
import core._

class PComponentTest extends FlatSpec with Matchers {

  "A PComponent" should "manage input and outputs" in {
    val config = ConfigFactory.load("testconfig1")

    // create an empty PComponent
    val pc = new PComponent {
      /**
       * run method is the one that actually performs the work
       */
      override def run(): Unit = {}
    }
    val ic = InputConnector("myin", new FileBasedData, "")
    val oc = OutputConnector("myout", new FileBasedData, "")
    pc.addConnector(ic)
    pc.addConnector(oc)

    pc.getInput("myin") shouldEqual (Some(ic))
    pc.getOutput("myout") shouldEqual (Some(oc))

  }

}