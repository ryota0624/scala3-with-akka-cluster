package helper

import scala.reflect.ClassTag

trait Command:
  type Reply

object Command:
  trait PauseMessage extends Command:
    override type Reply = Unit

trait ReplyBuilder[C <: Command, -E]:
  def buildReply(entity: E, command: C): command.Reply

trait CommandProceessor[C <: Command, Event, -E] extends ReplyBuilder[C, E]:
  def process(entity: E, command: C): Either[Throwable, Seq[Event]]
  def buildReply(entity: E, command: C): command.Reply

trait StatelesCommandProceessor[C <: Command, Event, -E] extends ReplyBuilder[C, E]:
  def process(command: C): Either[Throwable, Seq[Event]]
  def buildReply(entity: E, command: C): command.Reply

trait Entity[C <: Command, Event] {
  def process[C2 <: C](c: C2)(using processor:  CommandProceessor[C2, Event, this.type]): Either[Throwable, Seq[Event]] = processor.process(this, c)
  def buildReply[C2 <: C](c: C2)(using processor: ReplyBuilder[C2, this.type]): c.Reply = processor.buildReply(this, c)
}

