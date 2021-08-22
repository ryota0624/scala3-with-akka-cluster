package book.club

case class Meeting(
                    id: Meeting.Id,
                    clubId: Club.Id,
                    owner: Participant,
                    participants: Participants,
                    ranges: ReadingRanges,
                    pickups: PickupIds,
                    subtitle: Meeting.Subtitle,
                    eventDate: java.time.Instant
) extends helper.Entity[Meeting.Command, Meeting.Event]:
  def apply(event: Meeting.Event): Meeting = ???

object Meeting:
  case class Id(value: String)
  case class Subtitle(text: String)
  object Subtitle:
    def empty = Subtitle("")
  sealed trait Event
  case class MeetingScheduled(meetingId: Id, clubId: Club.Id, owner: Participant, ranges: ReadingRanges, subtitle: Subtitle, eventDate: java.time.Instant) extends Event
  case class PartifipantJoined(meetingId: Id, participant: Participant)extends Event
  case class PartifipantLeaved(meetingId: Id, participant: Participant)extends Event
  case class PickupAdded(meetingId: Id, participant: Participant)extends Event
  case class ReadingRangeUpdated(meetingId: Id, ranges: ReadingRanges)extends Event
  case class EventDateChanged(meetingId: Id, eventDate: java.time.Instant)extends Event

  sealed trait Command extends helper.Command:
    def meetingId: Id
  case class ScheduleMeeting(meetingId: Id, clubId: Club.Id, owner: Participant, ranges: ReadingRanges, subtitle: Subtitle, eventDate: java.time.Instant) extends Command
  case class JoinPartifipant(meetingId: Id, participant: Participant) extends Command
  case class LeavePartifipant(meetingId: Id, participant: Participant) extends Command
  case class AddPickup(meetingId: Id, participant: Participant) extends Command
  case class UpdateReadingRange(meetingId: Id, ranges: ReadingRanges) extends Command
  case class ChangeEventDate(meetingId: Id, eventDate: java.time.Instant) extends Command
