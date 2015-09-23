package akka.dumbdi

import java.util.concurrent.TimeoutException

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.Await
import scala.concurrent.duration._
import akka.pattern._

object ConfigNamed {
  val cfg = ConfigFactory.parseString(
    """
      akka.actor {
          dependency {
            some-guy = "akka.dumbdi.FakeModule"
            my-module-does-not-exist = "IDontExistModule"
          }
        }
    """.stripMargin)
}

class ActorWithModuleNamedSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
    with WordSpecLike with Matchers with BeforeAndAfterAll {
  def this() = this(ActorSystem("ActorWithModule", ConfigNamed.cfg))

  override def afterAll = {
    TestKit.shutdownActorSystem(system)
  }

  implicit val timeout = Timeout(3.seconds)

  "ActorWithNamedModule" should {
    "be overriden by cfg" in {
      val actor = system.actorOf(Props(new SomeGuyWithNamedModule), "name-of-actor-does-not-matter")
      val value = actor ? "sendback"
      Await.result(value, 3.seconds) shouldBe "fake"
    }

    "stay same with missing configuration" in {
      val actor = system.actorOf(Props(new SomeGuyWithNamedModuleNotInCfg), "name-of-actor-does-not-matter2")
      val value = actor ? "sendback"
      Await.result(value, 3.seconds) shouldBe "normal"
    }

    "throw actor exception for non-existing module" in {
      val actor = system.actorOf(Props(new SomeGuyWithNamedModuleDoesNotExist), "name-of-actor-does-not-matter3")
      val f = actor ? "sendback"
      intercept[TimeoutException] {
        Await.ready(f, 1.seconds)
      }
    }
  }
}

trait HisNamedModule extends ActorWithNamedModule { self: Actor =>
  override protected def moduleConfigLocation: String = "some-guy"

  override protected def moduleInit(module: ActorModuleRuntime): Unit = {
    module.bind[Service](new NormalService)
  }
}

class SomeGuyWithNamedModule extends Actor with HisNamedModule {
  val service: Service = module.get[Service]

  override def receive: Actor.Receive = {
    case _ ⇒ sender ! service.whoAmI()
  }
}

trait HisNamedModuleNotInCfg extends ActorWithNamedModule { self: Actor =>
  override protected def moduleConfigLocation: String = "i-am-not-in-config"

  override protected def moduleInit(module: ActorModuleRuntime): Unit = {
    module.bind[Service](new NormalService)
  }
}

class SomeGuyWithNamedModuleNotInCfg extends Actor with HisNamedModuleNotInCfg {
  val service: Service = module.get[Service]

  override def receive: Actor.Receive = {
    case _ ⇒ sender ! service.whoAmI()
  }
}

trait HisNamedModuleDoesNotExist extends ActorWithNamedModule { self: Actor =>
  override protected def moduleConfigLocation: String = "my-module-does-not-exist"

  override protected def moduleInit(module: ActorModuleRuntime): Unit = {
    module.bind[Service](new NormalService)
  }
}

class SomeGuyWithNamedModuleDoesNotExist extends Actor with HisNamedModuleDoesNotExist {
  val service: Service = module.get[Service]

  override def receive: Actor.Receive = {
    case _ ⇒ sender ! service.whoAmI()
  }
}
