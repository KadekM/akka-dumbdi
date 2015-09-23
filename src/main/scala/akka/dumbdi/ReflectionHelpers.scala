package akka.dumbdi

private[this] object ReflectionHelpers {

  def getModuleForClassname(classname: String): ActorModule = {
    import scala.reflect.runtime.universe

    try { // first look for object
      val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)
      val module = runtimeMirror.staticModule(classname)
      runtimeMirror.reflectModule(module).instance.asInstanceOf[ActorModule]
    } catch { // if not found, try to make class
      case t: Throwable ⇒
        Class.forName(classname).newInstance().asInstanceOf[ActorModule]
    }
  }
}
