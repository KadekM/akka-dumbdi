package akka.dumbdi

import akka.actor.Actor

import scala.reflect.ClassTag
import scala.util.{ Failure, Success, Try }

trait ActorModule { self ⇒
  protected val state = collection.mutable.HashMap.empty[Class[_], Any]

  final def get[A](implicit ct: ClassTag[A]): A = {
    state(ct.runtimeClass).asInstanceOf[A]
  }

  final def ++(other: ActorModule): ActorModule = new ActorModuleConfigurable {
    for (x ← self.state) bindClassOf(x._1, x._2)
    for (x ← other.state) bindClassOf(x._1, x._2) // overwrites
  }

  override def toString: String = state.mkString(",")
}

object EmptyModule extends ActorModule {}

trait ActorModuleConfigurable extends ActorModule {
  def bind[A](instance: A)(implicit ct: ClassTag[A]) =
    state(ct.runtimeClass) = instance

  def bindClassOf(clazz: Class[_], to: Any) =
    state(clazz) = to
}

trait ActorModulePressure extends ActorModuleConfigurable {
}

object ActorModuleConfigurable {
  def empty: ActorModuleConfigurable = new ActorModuleConfigurable {}
}

trait ActorWithModule extends ActorWithNamedModule {
  self: Actor ⇒
  final protected def moduleConfigLocation: String = "/" + context.self.path.elements.drop(1) // drop the parent to look-a-like deployment part
    .mkString("/")
}

trait ActorWithNamedModule {
  self: Actor ⇒
  protected def moduleConfigLocation: String

  private val fromConfigModule: ActorModule = {
    Try {
      context.system.settings.config.getString(s"akka.actor.dependency.${moduleConfigLocation}")
    } match {
      case Success(classname) ⇒ // path was found - if module does not exist, crash early
        val cl = Class.forName(classname)
        try { // first look for object
          val clazz = Class.forName(classname + "$")
          val obj = clazz.getField("MODULE$").get(cl)
          obj.asInstanceOf[ActorModule]
        } catch { // if not found, try to make class
          case t: Throwable =>
            cl.newInstance().asInstanceOf[ActorModule]
        }
      case Failure(_) ⇒ // path was not found - return empty module
        EmptyModule
    }
  }

  private val userModule = ActorModuleConfigurable.empty
  protected def moduleInit(module: ActorModuleConfigurable): Unit
  moduleInit(userModule)

  lazy protected val module: ActorModule = userModule ++ fromConfigModule
}