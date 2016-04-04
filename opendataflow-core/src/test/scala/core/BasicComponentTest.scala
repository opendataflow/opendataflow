package core

import com.typesafe.config.ConfigFactory
import org.scalatest.{ Matchers, FlatSpec }

/**
 * Created by OpenDataFlow on 03/04/16.
 */
class BasicComponentTest extends FlatSpec with Matchers {
  "A PComponent" should "not habe a slash in its name " in {
    val config = ConfigFactory.load("testconfig1")

    // create an empty PComponent
    val pc = new PComponent {
      override def run(): Unit = {}
    }
    an[Exception] should be thrownBy (pc.setName("my/name"))
  }
}
