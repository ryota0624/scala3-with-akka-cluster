package book.club

case class Participant(id: Participant.Id, icon: java.net.URI, nickName: String)

object Participant:
  case class Id(value: String)

case class Participants(values: Seq[Participant]):
  def add(participant: Participant): Participants = copy(values = values :+ participant)
  
object Participants:
  val empty = Participants(Nil)  