package akka.dumbdi

trait Service {
  def whoAmI(): String
}

class NormalService extends Service {
  override def whoAmI(): String = "normal"
}

class FakeService extends Service {
  override def whoAmI(): String = "fake"
}