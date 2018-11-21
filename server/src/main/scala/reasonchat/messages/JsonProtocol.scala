package reasonchat.messages

import reasonchat.dsl.ErrorCodes
import reasonchat.models.{Channel, Id, Message, User}
import spray.json.{DeserializationException, JsNumber, JsObject, JsString, JsValue, JsonFormat, RootJsonFormat}
import spray.json.DefaultJsonProtocol._

object ChatMessageType extends Enumeration {
  type ChatMessageType = Value

  val SelectChannel = Value(1)
  val SendMessage = Value(2)
  val GetChannels = Value(3)
  val AllChannels = Value(4)
  val GetMessages = Value(5)
  val AllMessages = Value(6)
  val UserInfo = Value(7)
  val NewMessage = Value(8)
  val UnknownMessage = Value(9)
  val ErrorOccured = Value(10)
  val Reserved = Value(-1)
}

object JsonProtocol {

  implicit def idFormat[A](): JsonFormat[Id[A]] = new JsonFormat[Id[A]] {
    override def read(json: JsValue): Id[A] = json match {
      case JsNumber(idVal) => Id(idVal.toInt)
      case _ =>
        throw DeserializationException(s"Expected id to be a number got $json")
    }

    override def write(obj: Id[A]): JsValue = JsNumber(obj.value)
  }

  sealed trait EnumType
  case object IntEnum extends EnumType
  case object StringEnum extends EnumType

  class EnumFormat[E <: Enumeration](enu: E, enumType: EnumType) extends RootJsonFormat[E#Value] {
    override def read(json: JsValue): E#Value = json match {
      case JsNumber(num) => enu.apply(num.toInt)
      case JsString(name) => enu.withName(name)
      case _ => throw DeserializationException(s"Unexpected input ${json.prettyPrint}")
    }

    override def write(obj: E#Value): JsValue = enumType match {
      case IntEnum => JsNumber(obj.id)
      case StringEnum => JsString(obj.toString)
    }
  }

  implicit val idUserFormat = idFormat[User]()
  implicit val idChannelFormat = idFormat[Channel]()
  implicit val idMessageFormat = idFormat[Message]()
  implicit val errorCodesFormat = new EnumFormat(ErrorCodes, IntEnum)
  implicit val channelFormat = jsonFormat2(Channel)
  implicit val userFormat = jsonFormat3(User)
  implicit val messageFormat = jsonFormat3(Message)
  val selectChannelFormat = jsonFormat2(SelectChannel)
  val sendMessageFormat = jsonFormat3(SendMessage)
  val getChannelsFormat = jsonFormat1(GetChannels)
  val allChannelsFormat = jsonFormat2(AllChannels)
  val getMessagesFormat = jsonFormat2(GetMessages)
  val allMessagesFormat = jsonFormat3(AllMessages)
  val userInfoFormat = jsonFormat2(UserInfo)
  val newMessageFormat = jsonFormat3(NewMessage)
  val unknownMessageFormat = jsonFormat2(UnknownMessage)
  val errorOccuredFormat = jsonFormat2(ErrorOccured)

  implicit val chatMessageFormat = new JsonFormat[ChatMessage] {
    override def read(json: JsValue): ChatMessage =
      json.asJsObject.getFields("type") match {
        case Seq(JsNumber(msgTypeVal)) =>
          ChatMessageType.apply(msgTypeVal.toInt) match {
            case ChatMessageType.SelectChannel => selectChannelFormat.read(json)
            case ChatMessageType.SendMessage => sendMessageFormat.read(json)
            case ChatMessageType.GetChannels => getChannelsFormat.read(json)
            case ChatMessageType.GetMessages => getMessagesFormat.read(json)
            case _ =>
              throw DeserializationException(s"Unknown message type $msgTypeVal")
          }
      }

    override def write(obj: ChatMessage): JsValue = {
      var `type` = ChatMessageType.UnknownMessage
      val data = obj match {
        case msg: SendMessage =>
          `type` = ChatMessageType.SendMessage
          sendMessageFormat.write(msg)
        case msg: AllChannels =>
          `type` = ChatMessageType.AllChannels
          allChannelsFormat.write(msg)
        case msg: AllMessages =>
          `type` = ChatMessageType.AllMessages
          allMessagesFormat.write(msg)
        case msg: UserInfo =>
          `type` = ChatMessageType.UserInfo
          userInfoFormat.write(msg)
        case msg: NewMessage =>
          `type` = ChatMessageType.NewMessage
           newMessageFormat.write(msg)
        case msg: UnknownMessage =>
          unknownMessageFormat.write(msg)
        case msg: ErrorOccured =>
          `type` = ChatMessageType.ErrorOccured
          errorOccuredFormat.write(msg)
        case _ =>
          throw DeserializationException("Cannot send internal message or request to clients")
      }
      val header = JsObject(
        "type" -> JsNumber(`type`.id)
      )
      header.copy(fields = header.fields ++ data.asJsObject.fields)
    }
  }
}
