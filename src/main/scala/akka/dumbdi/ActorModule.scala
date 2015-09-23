package akka.dumbdi

import akka.actor.Actor

import scala.reflect.ClassTag
import scala.util.{ Failure, Success, Try }

private[this] case class Identifier()

trait ActorModule { self ⇒
  protected val state = collection.mutable.HashMap.empty[Class[_], Any]

  def get[A](implicit ct: ClassTag[A]): A = {
    state(ct.runtimeClass).asInstanceOf[A]
  }

  def overwriteWith(other: ActorModule): ActorModule = new ActorModuleRuntime {
    for (x ← self.state) bindClassOf(x._1, x._2)
    for (x ← other.state) bindClassOf(x._1, x._2) // overwrites
  }

  override def toString: String = state.mkString(",")
}

object EmptyModule extends ActorModule {}

trait ActorModuleRuntime extends ActorModule {
  def bind[A](instance: A)(implicit ct: ClassTag[A]): Unit = {
    state(ct.runtimeClass) = instance
  }

  def bindClassOf(clazz: Class[_], to: Any): Unit = {
    state(clazz) = to
  }
}

trait ActorModuleTest extends ActorModule {
  def bind[A](instance: A)(implicit ct: ClassTag[A]): Unit = {
    state(ct.runtimeClass) = instance
  }
}

object ActorModuleRuntime {
  def empty: ActorModuleRuntime = new ActorModuleRuntime {}
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
        val module = ReflectionHelpers.getModuleForClassname(classname)
        module
      case Failure(_) ⇒ // path was not found - return empty module
        EmptyModule
    }
  }

  private val userModule = ActorModuleRuntime.empty
  protected def moduleInit(module: ActorModuleRuntime): Unit
  moduleInit(userModule)

  protected val module: ActorModule =
     userModule overwriteWith fromConfigModule
}