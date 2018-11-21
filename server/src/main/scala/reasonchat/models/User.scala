package reasonchat.models

case class User(
  id: Id[User],
  name: String,
  icon: Option[String] = None
)
