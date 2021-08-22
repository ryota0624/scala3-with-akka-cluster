package book.club

import akka.actor.typed.scaladsl.Behaviors
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EffectBuilder, EventSourcedBehavior, ReplyEffect}
import sample.User.UserCommand
import sample.UserActor.actorCommandHanlder

import scala.util.chaining._

case class Club(
                 id: Club.Id,
                 book: Book,
                 participants: Participants,
                 description: Club.Description
               ) extends helper.Entity[Club.ClubCommand, Club.ClubEvent]:
  def apply(event: Club.ClubEvent): Club =
    event match {
      case created: Club.ClubCreated =>
        throw new IllegalArgumentException(s"club is already created. $event")
      case added: Club.ParticipateAdded =>
        copy(participants = participants.add(added.participant))
      case modified: Club.DescriptionModified =>
        copy(description = modified.description)
    }


object Club:
  case class Id(value: String)
  case class Description(value: String)
  sealed trait ClubCommand extends helper.Command:
    def clubId: Club.Id
  case class CreateClub(clubId: Club.Id,
                        book: Book,
                        participants: Participants,
                        description: Club.Description) extends ClubCommand:
    override type Reply = Club.Id

  case class AddParticipate(clubId: Club.Id,
                        participant: Participant) extends ClubCommand:
    override type Reply = Unit

  case class ModifyDescription(clubId: Club.Id,
                               description: Description) extends ClubCommand:
    override type Reply = Unit

  type ClubCommandProcessor[C <: ClubCommand] =  helper.CommandProceessor[C, ClubEvent, Club]

  given createClubProcessor: helper.StatelesCommandProceessor[CreateClub, ClubEvent, Club] with
    def process(command: CreateClub): Either[Throwable, Seq[ClubEvent]] = Right(ClubCreated(command.clubId, command.book, command.participants, command.description) :: Nil)
    def buildReply(club: Club, command: CreateClub): command.Reply = club.id

  given addParticipateProcessor: ClubCommandProcessor[AddParticipate] with
    def process(club: Club, command: AddParticipate): Either[Throwable, Seq[ClubEvent]] = Right(ParticipateAdded(command.clubId, command.participant) :: Nil)
    def buildReply(club: Club, command: AddParticipate): command.Reply = ()

  given modifyDescriptionProcessor: ClubCommandProcessor[ModifyDescription] with
    def process(club: Club, command: ModifyDescription): Either[Throwable, Seq[ClubEvent]] = Right(DescriptionModified(command.clubId, command.description) :: Nil)
    def buildReply(club: Club, command: ModifyDescription): command.Reply = ()

  sealed trait ClubEvent:
    def clubId: Club.Id
  case class ClubCreated(clubId: Club.Id,
                        book: Book,
                        participants: Participants,
                        description: Club.Description) extends ClubEvent
  case class ParticipateAdded(clubId: Club.Id,
                            participant: Participant) extends ClubEvent
  case class DescriptionModified(clubId: Club.Id,
                               description: Description) extends ClubEvent




object ClubActor:
  import book.club.*
  type State = Option[Club]
  type ClubActorCommand = helper.akka.ActorCommand[Club.ClubCommand]


  def actorCommandHanlder = helper.akka.ActorCommandHandler[Club.ClubCommand, Club.ClubEvent, Club]()

  def commandHandler(state: State, command: ClubActorCommand): ReplyEffect[Club.ClubEvent, State] = {
    state match {
      case Some(club) =>
        command.cmd match {
          case _: Club.AddParticipate =>
            actorCommandHanlder(command[Club.AddParticipate], club)
          case _: Club.ModifyDescription =>
            actorCommandHanlder(command[Club.ModifyDescription], club)
          case _: Club.CreateClub =>
            Left(new IllegalArgumentException("Clubは作成済"))
        } fold(t => Effect.reply(command.replyTo)(StatusReply.error(t: Throwable)), identity)
      case None =>
        command.cmd match {
          case create: Club.CreateClub =>
            actorCommandHanlder(command[Club.CreateClub])
          case _ =>
            Left(new IllegalArgumentException("Club未作成"))
        } fold(t => Effect.reply(command.replyTo)(StatusReply.error(t: Throwable)), identity)
    }
  }

  def eventHandler(state: State, event: Club.ClubEvent): State = {
    state match {
      case Some(entity) => Some(entity(event))
      case None =>
        event match {
          case created: Club.ClubCreated => Some(Club(created.clubId, created.book, created.participants, created.description))
          case _ => throw new IllegalStateException("State is not Some")
        }
    }
  }


  def behavior(id: Club.Id) =  Behaviors.logMessages(EventSourcedBehavior.withEnforcedReplies[ClubActorCommand, Club.ClubEvent, State](
    commandHandler = commandHandler(_, _),
    eventHandler = eventHandler,
    emptyState = None,
    persistenceId = PersistenceId("Club", id.value)
  ))
