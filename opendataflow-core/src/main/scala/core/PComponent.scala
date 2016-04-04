package core

import com.typesafe.config.{Config, ConfigBeanFactory, ConfigFactory}
import core.data._

import scala.annotation.tailrec
import scala.beans.BeanProperty
import scala.collection.JavaConversions._
import scala.collection._
import scala.reflect.ClassTag
import org.slf4j.LoggerFactory


/**
  * A Connector represents an input or output
  * in that a components provides.
  * The name must be unique within the component.
  *
  * @param name
  * @param data
  * @param description
  */

abstract class PConnector(
  @BeanProperty var name: String,
  @BeanProperty var data: AbstractData,
  @BeanProperty var description: String = null,
  @BeanProperty var optional:Boolean = false) {
  var component:PComponent = null
  def getId = {
    if (component == null) "*undefined*"
    else
    component.getComponentPathAsString() + ":" + name
  }
}

case class InputConnector( n: String, d: AbstractData, desc: String = "Input")  extends PConnector(n, d, desc)
case class OutputConnector(n: String, d: AbstractData, desc: String = "Output") extends PConnector(n, d, desc)


abstract class PComponent {
  // name is used to compose the id.
  var name: String = ""

  @BeanProperty
  def setName(s:String) = if (s.contains("/"))
    throw new Exception(s"Can't have slash in component name ${s}")
  else name = s
  def getName = name

  // Set not supported by BeanFactory
  var connectors = Map[String, PConnector]()
  var parent: CompositePComponent = null.asInstanceOf[CompositePComponent]
  
  def getConnector(name: String, fn: PConnector ⇒ Boolean = (x) => true): Option[PConnector] = {
    connectors.get(name)
  }

  def getInputConnector(name: String): Option[InputConnector] = getConnector(name,
    { p: PConnector ⇒ p.isInstanceOf[InputConnector] }).map { _.asInstanceOf[InputConnector] }
  def getOutputConnector(name: String): Option[OutputConnector] = getConnector(name,
    { p: PConnector ⇒ p.isInstanceOf[OutputConnector] }).map { _.asInstanceOf[OutputConnector] }



  // utility methods
  def getInputConnectors():Set[InputConnector] = connectors.values.seq.filter(
    _.isInstanceOf[InputConnector]).map( x => x.asInstanceOf[InputConnector]).toSet
  def getOutputConnectors():Set[OutputConnector] = connectors.values.seq.filter(
    _.isInstanceOf[OutputConnector]).map( x => x.asInstanceOf[OutputConnector]).toSet


  /**
   * Add a connector to the set
    *
    * @param c
   */
  def addConnector(c: PConnector) = {
    if (c.component != null) throw new Exception("Connector already assigned")
    c.component = this
    connectors +=  c.name -> c
  }
  def addConnectors(comps:PConnector*):Unit = comps.map { addConnector(_) }


  def findConnector(p:(PConnector) => Boolean):Option[PConnector] = {
    connectors.values.seq.find(p)
  }

  /**
   * returns a sequence with the complete path of the component,
   * starting from root, and this as last element
    *
    * @return
   */
  def getComponentPath: Seq[PComponent] = parent match {
    case null ⇒ Seq(this)
    case _    ⇒ parent.getComponentPath :+ this
  }

  /**
    * Returns the component ID as a slash separated path
    * of strings, with the path through all the parent coponents
    * @return
    */
  def getId:String = getComponentPathAsString()

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
    *
    * @param c
    * @param name
    * @return
    */
  def buildComponent(c: Config, name: String): PComponent = {
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
    *
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

  class Builder[A <: CompositePComponent](val p: A, val options: BuildOptions = new BuildOptions()) {
    val LOG = LoggerFactory.getLogger(this.getClass)
    /**
      * add component with name.
      *
      * @param name
      * @param c
      * @return
      */
    def addComponent(name: String, c: PComponent): Builder[A] = {
      LOG.debug(s"Adding component ${name}")
      p.addComponent(name, c)

      // now c is the last
      if (options.autoConnect && (p.components.size > 1)) {
        autoConnect(p.components.get(p.components.length - 2), p.components.last)
      }
      this
    }

    /**
      * add component with a tuple
      *
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
      for {
        a <- tail.findConnector(_.isInstanceOf[OutputConnector])
        b <- nw.findConnector(_.isInstanceOf[InputConnector])
      } yield connect(a.asInstanceOf[OutputConnector],b.asInstanceOf[InputConnector])

    }


    /**
      * main connect method.
      * Other connect methods call this one
      * and are just adapters
      * @param cc
      * @return
      */
    def connect(cc: CConnection): Builder[A] = {
      LOG.debug(s"Connecting ${cc.source.getId} to ${cc.destination.getId}")
      p.connections.add(cc)
      this
    }

    def connect(sourceComponent: String, sout: String,
                destComponent: String, din: String): Builder[A] = {
      p.connect(sourceComponent, sout, destComponent, din)
      this
    }

    def connect(s:OutputConnector, d:InputConnector) = p.connect(s,d)

    def connect(s: String, d: String): Builder[A] = {
      p.connect(s, d)
      this
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


    def buildConnection(v: Config): Option[CConnection] = {
      for {
        from <- p.getConnectorById[OutputConnector](v.getString("from"))
        to <- p.getConnectorById[InputConnector](v.getString("to"))
      } yield new CConnection(from, to)
    }

    def buildConnections(c: Config): Seq[CConnection] = {
      if (!c.hasPath("connections")) return Seq.empty[CConnection]
      import collection.JavaConversions._
      c.getConfigList("connections").map { cc =>
        buildConnection(cc) match {
          case Some(x: CConnection) => x
          case None => throw new Exception(s"Can't build connection from config for config ${cc.toString}")
        }
      }
    }
  }
}


case class CConnection(val source:OutputConnector, val destination:InputConnector)

/**
  * base class for all components that are built by composing
  * other components.
  */
abstract class CompositePComponent extends PComponent {
  /**
   * inner components are designated by a name, that has to be unique within the component
   */
  private var _components  = Seq[PComponent]()
  var connections = mutable.Set[CConnection]()

  /**
    * Adds a component as subcomponent.
    *
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
    *
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
    * finds a connector, looking into subcomponents if necessary
    *
    * @param p
    * @return
    */
  override def findConnector( p:(PConnector) => Boolean):Option[PConnector] = {
    super.findConnector(p) match {
      case Some(s) => Some(s)
      case None =>
        @tailrec
        def findRec(c:Seq[PComponent]):Option[PConnector] = {
          c.head.findConnector(p) match {
            case Some(s) => Some(s)
            case None => findRec(c.tail)
          }
        }
        if(components.isEmpty) None
        else findRec(components)
    }
  }

  /**
    * Finds a component using its full path
    * /one/two/three
    *
    * returns None if any of the components in the
    * path do not exist.
    *
    * @param s : ID of component
    * @return
    */
  def getComponentById(s:String): Option[PComponent] = {
    val sep = "/"
    val path = s.split(sep).toList

    path match {
      case Nil => Some(this)
      case head :: tail => tail match {
        case Nil => getComponentByName(head)
        case _ => for {
                      c <- getComponentByName(head)
                        if c.isInstanceOf[CompositePComponent]
                      comp <- c.asInstanceOf[CompositePComponent].getComponentById(tail.mkString(sep))
        } yield comp
      }
    }
  }


  /**
    * finds a connector by ID, following path through subcomponents.
    *
    * @param s
    * @return
    */
  def getConnectorById[T <: PConnector](s:String )(implicit tag:ClassTag[T]):Option[T] = {
    // if the id is not component:connector then assume
    // it's the default
    if(! s.contains(":")) {
      getConnectorById[T](s + ":" + getDefaultConnectorName[T](tag) )
    } else {
      val parts = s.split(":")
      if (parts.length != 2) throw new Exception(s"Malformed id ${s}, should be path/to/my/component:connector")

      for {
        component <- getComponentById(parts.head)
        con: PConnector <- component.getConnector(parts.last)
      } yield con.asInstanceOf[T]
    }
  }


  def getDefaultConnectorName[T <: PConnector](implicit tag:ClassTag[T]):String = {
    if( classOf[OutputConnector].isAssignableFrom(tag.runtimeClass)) return PComponent.DEFAULT_SOURCE_STRING
    if( classOf[InputConnector].isAssignableFrom(tag.runtimeClass)) return PComponent.DEFAULT_SINK_STRING
    throw new Exception
  }

  /**
    * Connects one component to another
    * Connects the output of sourceComponent to the
    * input of destComponent, throwing an error if incompatible or not existent
    */
  def connect(sourceComponent: String, sout: String,
              destComponent: String, din: String): CompositePComponent = {
   val c =    for {
      sco <- getConnectorById[OutputConnector](s"${sourceComponent}:${sout}").
        orElse( throw new Exception(s"Cannot find ${sourceComponent}:${sout}" ))
      dco <- getConnectorById[InputConnector](s"${destComponent}:${din}").
        orElse( throw new Exception(s"Cannot find ${sourceComponent}:${sout}" ))
      if dco.getData.getClass.isAssignableFrom(sco.getData.getClass)
    }  yield CConnection(sco.asInstanceOf[OutputConnector], dco.asInstanceOf[InputConnector])

    c match {
      case Some(con) => addConnection(con)
      case None => throw new Exception(s"Can't connect ${sourceComponent}:${sout} and ${destComponent}:${din}, their data may not be compatible.")
    }

    return this
  }

  def connect(s:OutputConnector,d:InputConnector) =  addConnection(CConnection(s,d))
  def addConnection(con:CConnection) =  connections.add(con)
  /**
    * abbreviated form that connect source to destination
    * assuming source has an output named "output" and the
    * destination has an input named "input"
    *
    * @param s
    * @param d
    */
  def connect(s: String, d: String): CompositePComponent = {
    val con = for {
      in <- getConnectorById[OutputConnector](s)
      out <- getConnectorById[InputConnector](d)
    }  yield new CConnection(in,out)

    con match {
      case Some(x) => addConnection(x)
      case None => throw new Exception(s"Cannot connect ${s} to ${d}")
    }

    this
  }



  /**
   * Returns true if every input and output are in a connection
    *
    * @return
   */
  def getPipelineConnectionErrors(): Seq[String] = {
    var errors = Seq.empty[String]
    for (c ← components) {
      if(c.isInstanceOf[CompositePComponent]) {
        // add pipeline errors as they are found in subcomponents
        errors ++= c.asInstanceOf[CompositePComponent].getPipelineConnectionErrors()
      }

      for (con ← c.connectors.values.seq) {
        // to be connected, every input must be connected to an output and viceversa
        con match {
          case i @ InputConnector(n, _, _) ⇒ if (!connections.exists(_.destination == i)) {
            errors = errors :+ s"Input Connector ${i.getId} not connected"
          }
          case o @ OutputConnector(n, _, _) ⇒ if (!connections.exists(_.source == o)) {
            errors = errors :+ s"Output component ${o.getId} not connected"
          }
        }
      }
    }
    return errors
  }
}

class PipelineException(msg: String) extends Exception(msg)