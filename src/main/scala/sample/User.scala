package sample

import akka.actor.typed.scaladsl.Behaviors
import akka.pattern.StatusReply
import akka.persistence.Persistence
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect}
import helper.Entity
import sample.User.{ChangeName, CreateUser, UserActorCommand, UserCommand, UserCreated, UserEvent, UserId}


case class User(id: User.UserId, name: String) extends helper.Entity[User.UserCommand, User.UserEvent] {
    def apply(evt: UserEvent): User = {
    evt match {
      case _: User.UserCreated =>
        throw new IllegalStateException("user already created")
      case changed: sample.User.UserNameChanged =>
        copy(name = changed.name)
    }
  }
}

object User:
  type UserActorCommand = helper.akka.ActorCommand[UserCommand]

  opaque type UserId = String
  object UserId:
    def apply(id: String): UserId = id
    def toString(userId: UserId): String = userId

  sealed trait UserCommand extends helper.Command:
    def id: UserId
  case class CreateUser(id: UserId, name: String) extends UserCommand {
    override type Reply = UserId
  }

  case class ChangeName(id: UserId, name: String) extends UserCommand {
    override type Reply = Unit
  }

  case class PauseUser(id: UserId) extends UserCommand with helper.Command.PauseMessage

  given createUserProcessor: helper.StatelesCommandProceessor[CreateUser, UserEvent, User] with
    def process(command: CreateUser): Either[Throwable, Seq[UserEvent]] = Right(UserCreated(command.id, command.name) :: Nil)
    def buildReply(user: User, command: CreateUser): command.Reply = user.id

  given changeNameProcessor: helper.CommandProceessor[ChangeName, UserEvent, User] with
    def process(user: User, command: ChangeName): Either[Throwable, Seq[UserEvent]] = Right(UserNameChanged(user.id, command.name) :: Nil)
    def buildReply(user: User, command: ChangeName): command.Reply = ()

  given pauseProcessor: helper.CommandProceessor[PauseUser, UserEvent, User] with
    def process(user: User, command: PauseUser): Either[Throwable, Seq[UserEvent]] = Right(Nil)
    def buildReply(user: User, command: PauseUser): command.Reply = ()

  sealed trait UserEvent
  case class UserCreated(id: UserId, name: String) extends UserEvent
  case class UserNameChanged(id: UserId, name: String) extends UserEvent

object UserActor:
  import sample.User.*
  type State = Option[User]
  import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
  val TypeKey: EntityTypeKey[User.UserActorCommand] =
    EntityTypeKey[User.UserActorCommand]("User")

  def actorCommandHanlder = helper.akka.ActorCommandHandler[UserCommand, UserEvent, User]()

  def commandHandler(state: State, command: UserActorCommand): ReplyEffect[UserEvent, State] = {
    state match {
      case Some(user) =>
        command.cmd match {
          case _: ChangeName =>
            actorCommandHanlder(command[ChangeName], user)
          case _: sample.User.PauseUser =>
            actorCommandHanlder(command[PauseUser], user)
          case _: CreateUser =>
            Left(new IllegalArgumentException("Userは作成済"))
        } fold(t => Effect.reply(command.replyTo)(StatusReply.error(t: Throwable)), identity)
      case None =>
        command.cmd match {
          case create: CreateUser =>
            actorCommandHanlder(command[CreateUser])
          case _ =>
            Left(new IllegalArgumentException("User未作成"))
        } fold(t => Effect.reply(command.replyTo)(StatusReply.error(t: Throwable)), identity)
    }
  }

  def eventHandler(state: State, event: UserEvent): State = {
    state match {
      case Some(entity) => Some(entity(event))
      case None =>
        event match {
          case created: UserCreated => Some(User(created.id, created.name))
          case _ => throw new IllegalStateException("State is not Some")
        }
    }
  }


  def behavior(id: UserId) =  Behaviors.logMessages(EventSourcedBehavior.withEnforcedReplies[UserActorCommand, UserEvent, State](
    commandHandler = commandHandler(_, _),
    eventHandler = eventHandler,
    emptyState = None,
    persistenceId = PersistenceId(TypeKey.name, UserId.toString(id))
  ))
