package book.club

case class Pickup(
    id: Pickup.Id,
    meetingId: Meeting.Id,
    author: Participant.Id,
    page: Pickup.BookPage,
    headline: Pickup.Headline,
    note: Pickup.Note,
    comments: Pickup.Comments
                 ) extends helper.Entity[Pickup.Command, Pickup.Event]:
   def apply(event: Unit): Pickup = ???

object Pickup:
  case class Id(value: String)
  case class BookPage(text: String)
  case class Headline(text: String)
  case class Note(text: String)
  case class Comment(id: Comment.Id, text: String)
  object Comment:
    case class Id(value: String)
  case class Comments(values: Seq[Comment])
  sealed trait Event
  sealed trait Command extends helper.Command

case class PickupIds(ids: Seq[Pickup.Id])

object PickupIds:
  def empty = PickupIds(Nil)
