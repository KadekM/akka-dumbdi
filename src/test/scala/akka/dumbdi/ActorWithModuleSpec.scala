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

object Config {
  val cfg = ConfigFactory.parseString(
    """
      akka.actor {
          dependency {
            /some-guy = "akka.dumbdi.FakeModule"
            /my-module-does-not-exist = "IDontExistModule"
            /parent/child = "akka.dumbdi.FakeModule"
          }
        }
    """.stripMargin)
}

class ActorWithModuleSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
    with WordSpecLike with Matchers with BeforeAndAfterAll {
  def this() = this(ActorSystem("ActorWithModule", Config.cfg))

  override def afterAll = {
    TestKit.shutdownActorSystem(system)
  }

  implicit val timeout = Timeout(3.seconds)

  "ActorWithModule" should {
    "be overriden by cfg" in {
      val actor = system.actorOf(Props(new SomeGuyWithModule), "some-guy")
      val value = actor ? "sendback"
      Await.result(value, 3.seconds) shouldBe "fake"
    }

    "stay same with missing configuration" in {
      val actor = system.actorOf(Props(new SomeGuyWithModule), "i-am-not-in-config")
      val value = actor ? "sendback"
      Await.result(value, 3.seconds) shouldBe "normal"
    }

    "throw actor exception for non-existing module" in {
      val actor = system.actorOf(Props(new SomeGuyWithModule), "my-module-does-not-exist")
      val f = actor ? "sendback"
      intercept[TimeoutException] {
        Await.ready(f, 1.seconds)
      }
    }

    "work with children" in {
      val parent = system.actorOf(Props(new SomeGuyWithModule {
        val child = context.actorOf(Props(new SomeGuyWithModule), "child")
      }), "parent")


      val parentValue = parent ? "sendback"
      Await.result(parentValue, 3.seconds) shouldBe "normal"

      val child = system.actorSelection("/user/parent/child")
      val childValue = child ? "sendback"
      Await.result(childValue, 3.seconds) shouldBe "fake"
    }
  }
}

trait HisModule extends ActorWithModule { self: Actor =>
  override protected def moduleInit(module: ActorModuleRuntime): Unit = {
    module.bind[Service](new NormalService)
  }
}

class SomeGuyWithModule extends Actor with HisModule {
  val service: Service = module.get[Service]

  override def receive: Actor.Receive = {
    case _ ⇒ sender ! service.whoAmI()
  }
}
