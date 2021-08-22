package book.club

enum ReadingRange:
  case PageNumber(from: Int, to: Int)
  case Chaptor(from: String, to: String)
