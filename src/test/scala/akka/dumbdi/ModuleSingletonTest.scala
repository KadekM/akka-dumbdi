package akka.dumbdi

import akka.actor.{ Actor, Props, ActorSystem }
import akka.testkit.{ ImplicitSender, TestKit }
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpecLike }
import akka.pattern.ask

import scala.concurrent.Await
import scala.concurrent.duration._

object Config3 {
  val cfg = ConfigFactory.parseString(
    """
      akka.actor {
          dependency {
            module = "akka.dumbdi.ModuleSingleton"
          }
        }
    """.stripMargin)
}

object ModuleSingleton extends ActorModuleConfigurable

class ModuleSingletonTest(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
    with WordSpecLike with Matchers with BeforeAndAfterAll {
  def this() = this(ActorSystem("Test", Config3.cfg))

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  implicit val timeout = Timeout(3.seconds)

  "Singleton module" should {
    "first starts from 1" in new Fixture {
      val actor = system.actorOf(Props(new TestGuy))
      Await.result(actor ? "back", 3.seconds) shouldBe "1"
      Await.result(actor ? "back", 3.seconds) shouldBe "2"
      Await.result(actor ? "back", 3.seconds) shouldBe "3"
    }

    "second also starts from 1" in new Fixture {
      val actor = system.actorOf(Props(new TestGuy))
      Await.result(actor ? "back", 3.seconds) shouldBe "1"
      Await.result(actor ? "back", 3.seconds) shouldBe "2"
      Await.result(actor ? "back", 3.seconds) shouldBe "3"
    }
  }

  trait Fixture {
    ModuleSingleton.bind[Service](new FakeServiceCounting)
  }

  class TestGuy extends Actor with ActorWithNamedModule {
    val service: Service = module.get[Service]

    override def receive: Actor.Receive = {
      case _ ⇒ sender ! service.whoAmI()
    }

    override protected def moduleConfigLocation: String = "module"

    override protected def moduleInit(module: ActorModuleConfigurable): Unit = {
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

