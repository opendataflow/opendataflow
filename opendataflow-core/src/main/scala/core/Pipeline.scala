package core

class Pipeline extends CompositePComponent {

  def run = {}
}

/**
 * Companion object used as factory
 */
object Pipeline {
  def create(name: String): CompositePComponent.Builder[Pipeline] = {

    val p = new Pipeline
    p.setName(name)
    new CompositePComponent.Builder[Pipeline](p)
  }

}
