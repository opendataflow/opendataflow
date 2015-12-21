package core

class Pipeline extends CompositePComponent {

  def run = {}
}

/**
 * Companion object used as factory
 */
object Pipeline {
  def create(name: String): Pipeline = {
    val p = new Pipeline
    p.setName(name)
    return p
  }
}
