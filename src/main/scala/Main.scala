import akka.actor.Cancellable
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import akka.cluster.MemberStatus
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.typed.{Cluster, Subscribe}
import akka.discovery.awsapi.ecs.AsyncEcsDiscovery
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import akka.pattern._
import com.typesafe.config.{Config, ConfigFactory}
import helper.akka.ProcessOnceActor
import sample.User.{UserActorCommand, UserId}
import sample.UserActor

import java.net.InetAddress
import scala.concurrent.{Await, ExecutionContextExecutor, Future, Promise}
import akka.actor.typed.scaladsl.AskPattern.Askable

@main def user: Unit =
  val serviceName = "book-reading-club"
  implicit val system: ActorSystem[SpawnProtocol.Command] = loadConfig() match {
    case Some(config) =>
      ActorSystem(
        SpawnProtocol(),
        serviceName,
        config)
    case None =>
      ActorSystem(
        SpawnProtocol(),
        serviceName)
  }

  implicit val cluster: Cluster = Cluster(system)
  AkkaManagement(system).start()
  ClusterBootstrap(system).start()
  implicit val ec: ExecutionContextExecutor = system.executionContext

  def waitClusterUp(): Future[Long] = {
    implicit val ec: ExecutionContextExecutor = system.executionContext
    import scala.concurrent.duration._
    val start = java.time.Instant.now()
    val wait = Promise[Long]()
    val c: Cancellable = system.scheduler.scheduleWithFixedDelay(0.seconds, 300.millisecond) { () =>

      if (cluster.selfMember.status == MemberStatus.Up) {
        wait.success(java.time.Instant.now().toEpochMilli - start.toEpochMilli)
      }
    }
    val f = wait.future
    f.foreach { _ => c.cancel() }
    f
  }

  waitClusterUp().foreach {
    import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity}
    import akka.util.Timeout

    import concurrent.duration.DurationInt

    implicit val timeout: Timeout = 3.seconds

    val sharding = ClusterSharding(system)

    val userShardRef: ActorRef[ShardingEnvelope[UserActorCommand]] = sharding.init(Entity(typeKey = UserActor.TypeKey) { entityContext =>
      sample.UserActor.behavior(UserId(entityContext.entityId))
    })

    def shardForwarder[Command](ref: ActorRef[ShardingEnvelope[Command]], extractId: Command => String) = Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case actorCommand: helper.akka.ActorCommand[_] =>
          ctx.log.info(s"shardForwarder receive ActorCommand(${actorCommand.cmd})")
      }
      ref ! ShardingEnvelope(extractId(msg), msg)
      Behaviors.same
    }
    import akka.actor.typed.scaladsl.AskPattern.schedulerFromActorSystem
    val userShardForwarder = shardForwarder[UserActorCommand](userShardRef, command => sample.User.UserId.toString(command.cmd.id))
    val userRef = Await.result(system.ask[ActorRef[sample.User.UserActorCommand]](SpawnProtocol.Spawn(userShardForwarder, "user-shard-forwarder", akka.actor.typed.Props.empty, _)), 3.seconds)

    import helper.akka.ActorCommand.sendActor
    clusterUpElpsedTime =>
      system.log.info(s"clusterUpElpsedTime: ${clusterUpElpsedTime} millisecond")
      userRef ! sample.User.CreateUser(UserId("xxx"), "suzuki").sendActor(system.ignoreRef)
      userRef ! sample.User.ChangeName(UserId("xxx"),"sam").sendActor(system.ignoreRef)
  }

def loadConfig(): Option[Config] = {
  lazy val privateAddress = getPrivateAddressOrExit
  sys.env.get("RUN_ENV").map {
    case "ECS" =>
      ConfigFactory.parseString(s"""
                                   |akka {
                                   |  actor.provider = "cluster"
                                   |  management {
                                   |    cluster.bootstrap.contact-point.fallback-port = 8558
                                   |    http.hostname = "${privateAddress.getHostAddress}"
                                   |  }
                                   |  discovery.method = aws-api-ecs-async
                                   |  remote.artery.canonical.hostname = "${privateAddress.getHostAddress}"
                                   |}
                 """.stripMargin).withFallback(ConfigFactory.load())
    case _ => ???
  }
}

def getPrivateAddressOrExit: InetAddress =
  AsyncEcsDiscovery.getContainerAddress match {
    case Left(error) =>
      System.err.println(s"$error Halting.")
      sys.exit(1)

    case Right(value) =>
      value
  }
