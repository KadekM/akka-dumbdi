package akka.dumbdi

import akka.actor.{ Actor, Props, ActorSystem }
import akka.testkit.TestActors.EchoActor
import akka.testkit.{ ImplicitSender, TestKit }
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.scalatest._
import akka.pattern.ask

import scala.concurrent.Await
import scala.concurrent.duration._

object RcConfig {
  val cfg = ConfigFactory.parseString(
    """
      akka.actor {
          dependency {
            rc-actor = "akka.dumbdi.RcModuleSingleton"
          }
        }
    """.stripMargin)
}

object RcModuleSingleton extends ActorModuleTest

class RcModuleSingletonTest(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
    with WordSpecLike with Matchers with BeforeAndAfterEach with BeforeAndAfterAll {
  def this() = this(ActorSystem("RcModuleSingletonSystem", RcConfig.cfg))

  override def afterAll = {
    TestKit.shutdownActorSystem(system)
  }

  implicit val timeout = Timeout(3.seconds)

  "Race conditions" ignore {
    for (i ← 1 to 3) {
      s"$i - protected" in new RcFixture {
        if (i != 2) {
          // for example test failure, yet actor still runs
          ac ! "query"

          Thread.sleep(1000)

          rcMock.cnt shouldBe 2
        }
      }
    }
  }

  trait RcFixture {
    val rcMock = new RcService {
      var cnt = 0

      override def counter(): Int = {
        cnt = cnt + 1
        cnt
      }
    }

    println("Bind", rcMock.hashCode)
    RcModuleSingleton.bind[RcService](rcMock)
    val ac = system.actorOf(Props(new RcSlowActor))
  }
}

class RcSlowActor extends Actor with ActorWithNamedModule {
  override protected def moduleConfigLocation: String = "rc-actor"

  override protected def moduleInit(module: ActorModuleRuntime): Unit =
    module.bind[RcService](new RcServiceImpl)

  val service = {
    Thread.sleep(600)
    val mod = module.get[RcService]
    println("got", mod.hashCode)
    mod
  }

  override def preStart(): Unit = {
    service.counter()
  }

  def receive = {
    case _ ⇒ sender ! service.counter()
  }
}

trait RcService {
  def counter(): Int
}

class RcServiceImpl extends RcService {
  override def counter(): Int = 666
}

