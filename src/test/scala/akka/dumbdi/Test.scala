package akka.dumbdi

import java.util.concurrent.TimeoutException

import akka.actor.{Actor, Props, ActorSystem}
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
            module = "akka.dumbdi.Module"
          }
        }
    """.stripMargin)
}

class Module extends ActorModulePressure {
  bind[Service](new FakeService)
}

class Test(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
    with WordSpecLike with Matchers with BeforeAndAfterAll {
  def this() = this(ActorSystem("Test", Config3.cfg))

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  implicit val timeout = Timeout(3.seconds)

  "Test" should {
    " cfg" in new Fixture {
      val actor = system.actorOf(Props(new TestGuy))
      val value = actor ? "sendback"
      Await.result(value, 3.seconds) shouldBe "fake"
    }
  }

  trait Fixture {
  }
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
