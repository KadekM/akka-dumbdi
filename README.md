## Akka Dumb DI

If you don't want to commit to full blown DI for your actor system, but you'd still like to be
able to easily mock services and non-actor dependencies.

You normally specify your dependencies - module only in your test config.

In a config, if dependency path:
- is not found -> nothing is overriden, and production behaviour is used
- is found, but module is not found -> ActorInitializaitonException is thrown
- is found, and module specified is found -> module is executed and thus can override depenedncies

#### Installation
```
libraryDependencies += "com.marekkadek" %% "akka-dumbdi" % "0.0.1"
```


### ActorWithModule
You mix this trait if you want to specify dependancy module equal to deployment path:
```
akka.actor.dependency {
    /some-guy = "com.myproject.MyFakeModule"
    /parent/child = "com.myproject.SomeOtherModule"
}
```

```scala
package com.myproject

class MyFakeModule extends ActorModuleConfigurable {
  bind[Service](new FakeService)
  ...
}
```

Your actor could look like this. The initialize block is executed first.
```scala
class SomeGuy extends Actor with ActorWithModule {
  val service: Service = module.get[Service]

  override protected def initialize(module: ActorModuleConfigurable): Unit = {
    module.bind[Service](new NormalService) // <-- this will be overriden in test, as is specified in cfg
  }
  ...
}
```

You can extract the trait to more specific one, to separate module logic, if you wish:

```scala
trait MyModule extends ActorWithModule { self: Actor =>
  override protected def initialize(module: ActorModuleConfigurable): Unit = {
    module.bind[Service](new NormalService)
  }
}

class SomeGuyWithModule extends Actor with MyModule {
  val service: Service = module.get[Service]
  ...
}
```

### ActorWithModuleNamed
If you wish to speficy name yourself, and not derive one from path. Usage is very similar:

```
akka.actor.dependency {
    some-guy = "com.myproject.MyFakeModule"
}
```

```scala
trait HisNamedModule extends ActorWithNamedModule { self: Actor =>
  override protected def moduleConfigLocation: String = "some-guy"

  override protected def initialize(module: ActorModuleConfigurable): Unit = {
    module.bind[Service](new NormalService)
  }
}
```

Same as `ActorWithModule`, you can as well extract it to custom trait.

### Other info
You can't do any more binds outside `initialize` method (type system won't allow you).

### TODO
- documentation: update with snapshot changes
- documentation: module sigleton
- documentation: how to use fakes, mocks, fixtures
- production: non trivial binds (multiple of same type, named, scoped)
- production: nested modules detection
- tests: conditional, smart binds (in objects modules for tests - for mocking, first being different instance than second (for same interface... for example router with three children with same dependency to Service... we want all three different instances of mock)
