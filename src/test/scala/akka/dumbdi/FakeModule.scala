package akka.dumbdi

class FakeModule extends ActorModuleConfigurable {
  bind[Service](new FakeService)
}
