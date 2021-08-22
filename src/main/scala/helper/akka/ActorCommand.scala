package helper.akka

import akka.actor.typed.ActorRef
import akka.pattern.StatusReply
import akka.persistence.typed.scaladsl.Effect
import helper.{Command, CommandProceessor, Entity, StatelesCommandProceessor}

case class ActorCommand[C <: Command](cmd: C)(val replyTo: ActorRef[StatusReply[cmd.Reply]]):
  type Reply = cmd.Reply
  def apply(): (C, ActorRef[StatusReply[cmd.Reply]]) = (cmd, replyTo)
  def as[C2 <: C]: ActorCommand[C2] =
    val c = cmd.asInstanceOf[C2]
    ActorCommand[C2](c)(replyTo.asInstanceOf[ActorRef[StatusReply[c.Reply]]])
  def apply[C2 <: C]: ActorCommand[C2] =
    as[C2]

object ActorCommand:
  extension [C <: Command](cmd: C)
    def sendActor(replyTo: ActorRef[StatusReply[cmd.Reply]]) = ActorCommand(cmd)(replyTo)


case class ActorCommandHandler[C <: Command, Event, E <: Entity[C, Event]]():
  def apply[C2 <: C](actorCommand: ActorCommand[C2], entity: E)(using commandProceessor: CommandProceessor[C2, Event, E]) = {
    actorCommand.cmd match {
      case _: helper.Command.PauseMessage =>
        Right(Effect.stop[Event, Option[E]]().thenReply(actorCommand.replyTo)(state => {
          state match {
            case Some(entity) => StatusReply.success(entity.buildReply(actorCommand.cmd))
            case None => StatusReply.error(new IllegalStateException("state should not be None"))
          }
        }))
      case _ =>
        entity.process[C2](actorCommand.cmd)
          .map(events =>
            Effect.persist[Event, Option[E]](events)
//              .thenStop() // TODO: flagでon/off
              .thenReply(actorCommand.replyTo)
              (state => {
                state match {
                  case Some(entity) => StatusReply.success(entity.buildReply(actorCommand.cmd))
                  case None => StatusReply.error(new IllegalStateException("state should not be None"))
                }
              })
          )
    }
  }
  def apply[C2 <: C](actorCommand: ActorCommand[C2])(using commandProceessor: StatelesCommandProceessor[C2, Event, E]) = {
    actorCommand.cmd match {
      case _: helper.Command.PauseMessage =>
        val replyTo = actorCommand[helper.Command.PauseMessage with C2].replyTo
        Right(Effect.stop[Event, Option[E]]().thenReply(replyTo)(state => {
          StatusReply.success(())
        }))
      case _ =>
        commandProceessor.process(actorCommand.cmd)
          .map(events =>
            Effect.persist[Event, Option[E]](events)
//              .thenStop() // TODO: flagでon/off
              .thenReply(actorCommand.replyTo)
              (state => {
                state match {
                  case Some(entity) => StatusReply.success(entity.buildReply(actorCommand.cmd))
                  case None => StatusReply.error(new IllegalStateException("state should not be None"))
                }
              })
          )
    }


  }
