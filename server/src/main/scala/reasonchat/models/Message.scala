package reasonchat.models

case class Message(
  id: Id[Message],
  content: String,
  from: User
)
