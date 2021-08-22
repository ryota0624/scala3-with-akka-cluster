package helper.akka

import akka.actor.typed.{ActorRef, Props}
import akka.actor.typed.scaladsl.Behaviors

object ProcessOnceActor:
  def behavior[Command](
                         buildBahavior: Command => akka.actor.typed.Behavior[Command],
                         stopCommand: Command => ActorRef[Any] => Command
                       ): akka.actor.typed.Behavior[Command] = {
    Behaviors.receive { (ctx, command) =>
      val handleBehavior = buildBahavior(command)
      val handleRef = ctx.spawnAnonymous(handleBehavior, Props.empty)
      handleRef ! command
//      handleRef ! stopCommand(command)(ctx.system.ignoreRef)
      Behaviors.same
    }
  }
