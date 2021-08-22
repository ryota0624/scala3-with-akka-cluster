package book.club

case class ReadingRanges(values: Map[ReadingRanges.RangeLabel, ReadingRange])

object ReadingRanges:
  case class RangeLabel(text: String)
