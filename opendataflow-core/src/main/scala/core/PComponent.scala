package core

import java.util

import com.typesafe.config.{ ConfigFactory, ConfigBeanFactory, Config }
import scala.beans.BeanProperty
import core.data._
import collection.JavaConversions._
import scala.collection._

abstract class PConnector(
  @BeanProperty var name: String,
  @BeanProperty var data: AbstractData,
  @BeanProperty var description: String = null)

case class InputConnector(s: String, d: AbstractData, desc: String = "Input") extends PConnector(s, d, desc)
case class OutputConnector(s: String, d: AbstractData, desc: String = "Output") extends PConnector(s, d, desc)

abstract class PComponent {

  @BeanProperty
  var name: String = ""

  // Set not supported by BeanFactory
  var connectors = Set[PConnector]()

  var parent: CompositePComponent = null.asInstanceOf[CompositePComponent]

  def getInput(name: String): Option[InputConnector] = getConnector(name,
    { p: PConnector ⇒ p.isInstanceOf[InputConnector] }).map { _.asInstanceOf[InputConnector] }
  def getOutput(name: String): Option[OutputConnector] = getConnector(name,
    { p: PConnector ⇒ p.isInstanceOf[OutputConnector] }).map { _.asInstanceOf[OutputConnector] }

  def getConnector(name: String, fn: PConnector ⇒ Boolean): Option[PConnector] = {
    connectors.find { p ⇒ fn(p) && p.name == name }
  }

  /**
   * Add a connector to the set
   * @param c
   */

  def addConnector(c: PConnector) = connectors += c
  /**
   * returns a sequence with the complete path of the component,
   * starting from root, and this as last element
   * @return
   */
  def getComponentPath: Seq[PComponent] = parent match {
    case null ⇒ Seq(this)
    case _    ⇒ parent.getComponentPath :+ this
  }
  // Return the component path as a string
  def getComponentPathAsString(sep: String = "/") = getComponentPath.mkString(sep)
  override def toString = getComponentPathAsString()

  /**
   * run method is the one that actually performs the work
   */
  def run()

}

object PComponent {
  def fromConfig(c: Config, name: String): (String, PComponent) = {

    return (name, BeanFactory(c, name))
  }


  /**
    * Bean factory used to build components from config
    * files
    * @param c
    * @param name
    * @return
    */
  def BeanFactory(c: Config, name: String): PComponent = {
    if (!c.hasPath("components")) throw new ConfigurationException("No data for 'components'")
    val components = c.getConfig("components")
    if (!components.hasPath(name)) throw new ConfigurationException(s"No data for component ${name}")
    val d = components.getConfig(name)

    if (!d.hasPath("data")) throw new ConfigurationException(s"No data for component ${name}")
    if (!d.hasPath("class")) throw new ConfigurationException(s"No class for component ${name}")

    val cl = d.getString("class")
    // try to lookup class and see if it's a subclass of component
    val cla = Class.forName(cl)
    val pcmpclass = Class.forName("core.PComponent")
    if (pcmpclass.isAssignableFrom(cla)) {
      return ConfigBeanFactory.create(d.getConfig("data").withFallback(ConfigFactory.parseMap(Map("name" -> name))), cla).asInstanceOf[PComponent]
    } else {
      throw new ConfigurationException(s"Class ${cla} is not assignable from ${PComponent.getClass}")
    }

  }
}

case class ConfigurationException(s: String) extends Exception(s)

/**
 * Companion object is used to build components, chaining
  * building methods.
 */
object CompositePComponent {
  class Builder[A <: CompositePComponent](val p:A) {
    /**
      * add component with name.
      * @param name
      * @param c
      * @return
      */
    def addComponent(name: String, c: PComponent):Builder[A] = {
      p.addComponent(name,c)
      this
    }

    /**
      * add component with a tuple
      * @param c
      * @return
      */
    def addComponent(c: (String, PComponent)):Builder[A] = {
      p.addComponent(c)
      this
    }

    def connect(sourceComponent: String, sout: String,
                destComponent: String, din: String):Builder[A] = {
      p.connect(sourceComponent, sout, destComponent, din)
      this
    }

    def connect(s:String,d:String):Builder[A] = { p.connect(s,d); this }

    def compile() = {
      // check that all components are fully connected
      // that means all sources should all be connected to at least one sink
      // and every sink to at least one source
      val er =  p.getPipelineConnectionErrors()

      if (! er.isEmpty) {
        throw new PipelineException("The pipeline is not completely connected: " +
          er.map( "\t" + _).mkString("\n"))
      }
      this
    }
  }

}

/**
  * base class for all components that are built by composing
  * other components.
  */
abstract class CompositePComponent extends PComponent {
  case class ComponentConnection(source: PComponent, sourceOutput: String, destination: PComponent, destinationInput: String)

  /**
   * inner components are designated by a name, that has to be unique within the component
   */
  var components: mutable.Map[String, PComponent] = mutable.HashMap[String, PComponent]()
  var connections = Set[ComponentConnection]()

  /**
    * Adds a component as subcomponent.
    * @param name
    * @param c
    * @return
    */
  def addComponent(name: String, c: PComponent): CompositePComponent = {
    c.parent = this
    c.setName(name)
    components.put(name, c)
    return this // allows chaining
  }

  /**
    * Tuple2 version of addComponent
    * @param c
    * @return
    */
  def addComponent(c: (String, PComponent)): CompositePComponent = {
    return addComponent(c._1, c._2)
  }

  /**
    * Connects one component to another
    * Connects the output of sourceComponent to the
    * input of destComponent, throwing an error if incompatible or not existent
    */
  def connect(sourceComponent: String, sout: String,
              destComponent: String, din: String): CompositePComponent = {
    // source component
    val sc = components.get(sourceComponent).
      getOrElse(throw new PipelineException(s"No such component ${sourceComponent} "))

    val sd = sc.getInput(sout) match {
      case None    ⇒ throw new PipelineException(s"No such output '${sout}' for component ${sc.name}")
      case Some(s) ⇒ s.getData
    }

    // destination component
    val dc = components.get(destComponent).
      getOrElse(throw new PipelineException(s"No such component ${destComponent} "))

    // same for output
    val dd = dc.getOutput(din) match {
      case None    ⇒ throw new PipelineException(s"No such input '${din}' for component ${dc.name}")
      case Some(s) ⇒ s.getData
    }

    // the destination must be a superclass of the source.
    // for instance, we can connect a FileBased source to an OffsetBased destination
    // but not viceversa. That is, the receiving side decides which class
    // can accept.
    // if  A is a class and B extends A, a = new A and b = new B
    // a.getClass.isAssignableFrom(b.getClass)
    // Boolean = true
    // b.getClass.isAssignableFrom(a.getClass)
    // Boolean = false

    if (!dd.getClass.isAssignableFrom(sd.getClass))
      throw new PipelineException(s"Destination Data ${destComponent}/${din} is of type ${dd.getClass.getName}  " +
        s"which is not a  superclass of Source ${sourceComponent}/${sout} of type ${sd.getClass.getName}")
    connections = connections + ComponentConnection(sc, sout, dc, din)

    return this
  }

  /**
    * abbreviated form that connect source to destination
    * assuming source has an output named "output" and the
    * destination has an input named "input"
    * @param s
    * @param d
    */
  def connect(s: String, d: String): CompositePComponent = {
    connect(s, "source", d, "sink")
  }

  /**
   * Returns true if every input and output are in a connection
   * @return
   */
  def getPipelineConnectionErrors(): Seq[String] = {
    var errors = Seq.empty[String]
    for ((name, c) ← components if c.isInstanceOf[CompositePComponent]) {
      val cc = c.asInstanceOf[CompositePComponent]
      errors ++= cc.getPipelineConnectionErrors() // add pipeline errors as they are found in subcomponents

      for (con ← c.connectors) {
        // to be connected, every input must be connected to an output and viceversa
        con match {
          case i @ InputConnector(n, _, _) ⇒ if (!connections.exists(cc ⇒ cc.destination == c
            && cc.destinationInput == n)) {
            errors = errors :+ s"Input component ${c.getComponentPathAsString()} has its input ${n} disconnected"
          }
          case o @ OutputConnector(n, _, _) ⇒ if (!connections.exists(cc ⇒ cc.source == c
            && cc.sourceOutput == n)) {
            errors = errors :+ s"Output component ${c.getComponentPathAsString()} has its ouptu ${n} disconnected"
          }
        }
      }
    }
    return errors
  }



}

class PipelineException(msg: String) extends Exception(msg)