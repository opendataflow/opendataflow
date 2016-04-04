package core

import com.typesafe.config.{ ConfigFactory, Config }
import core.CompositePComponent.BuildOptions

class Pipeline extends CompositePComponent {

  def run = {}
}

/**
 * Companion object used as factory
 */
object Pipeline {
  def fromConfig(s: String): CompositePComponent.Builder[Pipeline] = {
    fromConfig(ConfigFactory.load(s))
  }

  def fromConfig(c: Config): CompositePComponent.Builder[Pipeline] = {
    val p = new Pipeline
    if (c.hasPath("name")) p.setName(c.getString("name"))
    else throw new PipelineException("Pipeline name missing in config")
    val options = new BuildOptions()
    options.setAutoConnect(false)
    // no autoconnect since order in config file is not guaranteed
    val b = new CompositePComponent.Builder[Pipeline](p, options)
    // add all read components to pipeline
    for (c ← PComponent.buildComponents(c)) { b.addComponent(c) }

    for (con ← b.buildConnections(c)) {
      con match {
        case cc: CConnection ⇒ b.connect(cc)
        case _               ⇒
      }
    }

    return b
  }

  def create(name: String): CompositePComponent.Builder[Pipeline] = {

    val p = new Pipeline
    p.setName(name)
    new CompositePComponent.Builder[Pipeline](p)
  }

}
