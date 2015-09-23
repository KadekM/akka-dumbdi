package akka.dumbdi

import akka.actor.{ Actor, Props, ActorSystem }
import akka.testkit.TestActors.EchoActor
import akka.testkit.{TestProbe, ImplicitSender, TestKit}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.scalatest.{WordSpec, BeforeAndAfterAll, Matchers, WordSpecLike}
import akka.pattern.ask

import scala.concurrent.Await
import scala.concurrent.duration._

object ConfigModuleSingletonSingleShared {
  val cfg = ConfigFactory.parseString(
    """
      akka.actor {
          dependency {
            module = "akka.dumbdi.ModuleSingletonSingleShared"
          }
        }
    """.stripMargin)
}

object ModuleSingletonSingleShared extends ActorModuleTest

class ModuleSingletonSingleSharedTest
    extends WordSpec with Matchers with BeforeAndAfterAll {

  implicit val timeout = Timeout(3.seconds)

  "two systems " ignore {
    "be able to share same module" in new Fixture {
      val ac1 = system1.actorOf(Props(new TestGuy("module")), "tester")
      val ac2 = system2.actorOf(Props(new TestGuy("module")), "tester")
      try {
        ac1.tell("test", probe1.ref)
        ac2.tell("test", probe2.ref)
        Thread.sleep(500)

        probe1.expectMsg("1")
        probe2.expectMsg("1")

        ac1.tell("test", probe1.ref)
        ac2.tell("test", probe2.ref)
        Thread.sleep(1500)

        probe1.expectMsg("2")
        probe2.expectMsg("2")
      } finally {
        Await.ready(system1.terminate(), 10.seconds)
        Await.ready(system2.terminate(), 10.seconds)
      }
    }
  }

  trait Fixture {
    ModuleSingletonSingleShared.bind[Service](new FakeServiceCounting)
    val system1 = ActorSystem("system1", ConfigModuleSingletonSingleShared.cfg)
    val system2 = ActorSystem("system2", ConfigModuleSingletonSingleShared.cfg)
    val probe1 = TestProbe()(system1)
    val probe2 = TestProbe()(system2)
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

