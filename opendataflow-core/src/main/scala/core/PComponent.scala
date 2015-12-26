package core

import java.util

import com.typesafe.config.{ConfigValue, ConfigFactory, ConfigBeanFactory, Config}
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
  def getComponentPathAsString(sep: String = "/") = getComponentPath.map( _.name ).mkString(sep)

  override def toString = getComponentPathAsString() + ":" + super.toString

  /**
   * run method is the one that actually performs the work
   */
  def run()

}

object PComponent {
  val DEFAULT_SOURCE_STRING = "source"
  val DEFAULT_SINK_STRING   = "sink"

  def fromConfig(c: Config, name: String): (String, PComponent) = {
    return (name, buildComponent(c, name))
  }


  /**
    * Bean factory used to build components from config
    * files
    * @param c
    * @param name
    * @return
    */
  def buildComponent(c: Config, name: String): PComponent = {
    println("Building " + name)
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

  /**
    * Build all components specified under the "components" key in configuration
    * @param c
    * @return
    */
  def buildComponents(c:Config): Seq[PComponent] = {
    if (!c.hasPath("components")) throw new ConfigurationException("No data for 'components'")
    import collection.JavaConversions._
    c.getObject("components").keySet().map( buildComponent(c,_)).toSeq
  }
}

case class ConfigurationException(s: String) extends Exception(s)


/**
 * Companion object is used to build components, chaining
  * building methods.
 */
object CompositePComponent {
  class BuildOptions {
    @BeanProperty
    var autoConnect = true
  }
  class Builder[A <: CompositePComponent](val p:A, val options:BuildOptions = new BuildOptions()) {
    /**
      * add component with name.
      * @param name
      * @param c
      * @return
      */
    def addComponent(name: String, c: PComponent): Builder[A] = {
      p.addComponent(name, c)

      // now c is the last
      if (options.autoConnect && (p.components.size > 1)) {
        autoConnect(p.components.get(p.components.length - 2), p.components.last)
      }
      this
    }

    /**
      * add component with a tuple
      * @param c
      * @return
      */
    def addComponent(c: (String, PComponent)): Builder[A] = {
      addComponent(c._1, c._2)
    }

    def addComponent(c: PComponent): Builder[A] = {
      addComponent(c.name, c)
    }

    /**
      * connects a components source to the others' sink,
      * if they are present with their default names.
      *
      * @param tail
      * @param nw
      * @return
      */
    def autoConnect(tail: PComponent, nw: PComponent) = {
      import PComponent.{DEFAULT_SINK_STRING, DEFAULT_SOURCE_STRING}
      (tail.getInput(DEFAULT_SOURCE_STRING), nw.getOutput(DEFAULT_SINK_STRING)) match {
        case (Some(a), Some(b)) => connect(tail.getName(), nw.getName())
        case _ => // do nothing if either source or sink missing
      }
    }


    def connect( cc:ComponentConnection): Builder[A] = {
      p.connections.add(cc)
      this
    }

    def connect(sourceComponent: String, sout: String,
                destComponent: String, din: String): Builder[A] = {
      p.connect(sourceComponent, sout, destComponent, din)
      this
    }

    def connect(s: String, d: String): Builder[A] = {
      p.connect(s, d); this
    }

    def compile(): A = {
      // check that all components are fully connected
      // that means all sources should all be connected to at least one sink
      // and every sink to at least one source
      val er = p.getPipelineConnectionErrors()

      if (!er.isEmpty) {
        throw new PipelineException("The pipeline is not completely connected: \n" +
          er.map("\t" + _).mkString("\n"))
      }
      this.p
    }


    def buildConnection(v: Config): ComponentConnection = {
      def splitNotation(s: String, d: String): (String, String) = {
        val v = s.split(":")
        if (v.size > 2) throw new PipelineException(s"Bad connection point ${s}")
        if (v.size == 2) (v(0), v(1)) else (v(0), d)
      }

      val (from, source) = if (v.hasPath("from")) {
        splitNotation(v.getString("from"), PComponent.DEFAULT_SOURCE_STRING)
      } else throw new PipelineException("missing from")

      val (to, sink) = if (v.hasPath("to")) {
        splitNotation(v.getString("to"), PComponent.DEFAULT_SINK_STRING)
      } else throw new PipelineException("missing to")

      return new ComponentConnection(p.getComponentByName(from).getOrElse(
          throw new PipelineException(s"Non-existent component : ${from} in ${p.getComponentPathAsString()}")),
        source, p.getComponentByName(to).getOrElse(
          throw new PipelineException(s"Non-existent component : ${to} in ${p.getComponentPathAsString()}")),
        sink)
    }

    def buildConnections(c: Config): Seq[ComponentConnection] = {
      if (!c.hasPath("connections")) return Seq.empty[ComponentConnection]
      import collection.JavaConversions._
      c.getConfigList("connections").map(buildConnection(_)).toSeq
    }
  }

}


case class ComponentConnection(source: PComponent, sourceOutput: String,
                               destination: PComponent, destinationInput: String)

/**
  * base class for all components that are built by composing
  * other components.
  */
abstract class CompositePComponent extends PComponent {

  /**
   * inner components are designated by a name, that has to be unique within the component
   */
  private var _components  = Seq[PComponent]()
  var connections = mutable.Set[ComponentConnection]()

  /**
    * Adds a component as subcomponent.
    * @param name
    * @param c
    * @return
    */
  def addComponent(name: String, c: PComponent): CompositePComponent = {
    c.setName(name)
    addComponent(c)
  }

  def components = _components
  def components_=(value:Seq[PComponent]):Unit = _components = value

  def addComponent(c:PComponent) : CompositePComponent = {
    if(c.getName == null) {
      throw new PipelineException(s"Supplied a component with a null name to ${this.getComponentPathAsString()}")
    }
    // throw exception if trying to add component with an already existent name
    components.find( x=> x.name == name) match {
      case Some(_) => throw new PipelineException(s"Component ${name} already present in ${this.getComponentPathAsString()}")
      case None =>
    }

    c.parent = this
    components  = components :+ c
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

  def getComponentByName(s:String):Option[PComponent] = {
    components.find( _.name == s )
  }

  /**
    * Connects one component to another
    * Connects the output of sourceComponent to the
    * input of destComponent, throwing an error if incompatible or not existent
    */
  def connect(sourceComponent: String, sout: String,
              destComponent: String, din: String): CompositePComponent = {
    // source component
    val sc = getComponentByName(sourceComponent).
      getOrElse(throw new PipelineException(s"No such component ${sourceComponent} "))

    val sd = sc.getInput(sout) match {
      case None    ⇒ throw new PipelineException(s"No such output '${sout}' for component ${sc.name}")
      case Some(s) ⇒ s.getData
    }

    // destination component
    val dc = getComponentByName(destComponent).
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

    if (!dd.getClass.isAssignableFrom(sd.getClass)) {
      throw new PipelineException(s"Destination Data ${destComponent}/${din} is of type ${dd.getClass.getName}  " +
        s"which is not a  superclass of Source ${sourceComponent}/${sout} of type ${sd.getClass.getName}")
    }

    connections.add( ComponentConnection(sc, sout, dc, din))

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
    connect(s, PComponent.DEFAULT_SOURCE_STRING, d, PComponent.DEFAULT_SINK_STRING)
  }



  /**
   * Returns true if every input and output are in a connection
   * @return
   */
  def getPipelineConnectionErrors(): Seq[String] = {
    var errors = Seq.empty[String]
    for (c ← components) {
      if(c.isInstanceOf[CompositePComponent]) {
        // add pipeline errors as they are found in subcomponents
        errors ++= c.asInstanceOf[CompositePComponent].getPipelineConnectionErrors()
      }

      for (con ← c.connectors) {
        // to be connected, every input must be connected to an output and viceversa
        con match {
          case i @ InputConnector(n, _, _) ⇒ if (!connections.exists(cc ⇒ cc.source == c
            && cc.sourceOutput == n)) {
            errors = errors :+ s"Input component ${c.getComponentPathAsString()} has its connector ${n} disconnected"
          }
          case o @ OutputConnector(n, _, _) ⇒ if (!connections.exists(cc ⇒ cc.destination == c
            && cc.destinationInput == n)) {
            errors = errors :+ s"Output component ${c.getComponentPathAsString()} has its connector ${n} disconnected"
          }
        }
      }
    }
    return errors
  }
}

class PipelineException(msg: String) extends Exception(msg)