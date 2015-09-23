package akka.dumbdi

import akka.actor.{ Actor, Props, ActorSystem }
import akka.testkit.{ ImplicitSender, TestKit }
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpecLike }
import akka.pattern.ask

import scala.concurrent.Await
import scala.concurrent.duration._

object ConfigSingleton {
  val cfg = ConfigFactory.parseString(
    """
      akka.actor {
          dependency {
            module = "akka.dumbdi.ModuleSingleton"
            nested-module = "akka.dumbdi.Nested.InsideNested"
            nested-module2 = "akka.dumbdi.Nested.InsideNested.EvenMore"
          }
        }
    """.stripMargin)
}

object ModuleSingleton extends ActorModuleTest

object Nested {
  object InsideNested extends ActorModuleTest {
    bind[Service](new FakeService)

    object EvenMore extends ActorModuleTest {
      bind[Service](new FakeService)
    }
  }
}

class ModuleSingletonTest(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
    with WordSpecLike with Matchers with BeforeAndAfterAll {
  def this() = this(ActorSystem("module-singleton", ConfigSingleton.cfg))

  override def afterAll = {
    TestKit.shutdownActorSystem(system)
  }

  implicit val timeout = Timeout(3.seconds)

  "Singleton module" should {
    "first starts from 1" in new Fixture {
      val actor = system.actorOf(Props(new TestGuy("module")))
      Await.result(actor ? "back", 3.seconds) shouldBe "1"
      Await.result(actor ? "back", 3.seconds) shouldBe "2"
      Await.result(actor ? "back", 3.seconds) shouldBe "3"
    }

    "second also starts from 1" in new Fixture {
      val actor = system.actorOf(Props(new TestGuy("module")))
      Await.result(actor ? "back", 3.seconds) shouldBe "1"
      Await.result(actor ? "back", 3.seconds) shouldBe "2"
      Await.result(actor ? "back", 3.seconds) shouldBe "3"
    }

    "can access nested modulel" in new Fixture {
      val actor = system.actorOf(Props(new TestGuy("nested-module")))
      Await.result(actor ? "back", 3.seconds) shouldBe "fake"

      val actor2 = system.actorOf(Props(new TestGuy("nested-module2")))
      Await.result(actor2 ? "back", 3.seconds) shouldBe "fake"
    }
  }

  trait Fixture {
    ModuleSingleton.bind[Service](new FakeServiceCounting)
  }

  class TestGuy(modulePath: String) extends Actor with ActorWithNamedModule {
    val service: Service = module.get[Service]

    override def receive: Actor.Receive = {
      case _ ⇒ sender ! service.whoAmI()
    }

    override protected def moduleConfigLocation: String = modulePath

    override protected def moduleInit(module: ActorModuleRuntime): Unit = {
      module.bind[Service](new NormalService)
    }
  }

  class FakeServiceCounting(var counter: Int = 0) extends Service {
    override def whoAmI(): String = {
      counter += 1
      counter.toString
    }
  }
}

