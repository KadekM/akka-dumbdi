package akka.dumbdi

class FakeModule extends ActorModuleRuntime {
  bind[Service](new FakeService)
}
