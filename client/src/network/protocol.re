

type selectChannel = {
  userId: int,
  channelId: int
};

type userInfo = {
  userId: int,
  user: User.model
};

type getChannels = {
  userId: int
};

type sendMessage = {
  userId: int,
  channelId: int,
  message: string
};

type newMessage = {
  userId: int,
  channelId: int,
  message: Message.model
};

type allChannels = {
  userId: int,
  channels: ChannelList.model
};

type getMessages = {
  userId: int,
  channelId: int
};

type allMessages = {
  userId: int,
  channelId: int,
  messages: MessageList.model
};

type unknownMessage = {
  userId: int,
  content: string
};

type errorOccured = {
  userId: int,
  code: int
};

type chatRequest =
   SelectChannel(selectChannel)
 | GetChannels(getChannels)
 | GetMessages(getMessages)
 | SendMessage(sendMessage)

type chatResponse =
   UserInfo(userInfo)
 | AllChannels(allChannels)
 | AllMessages(allMessages)
 | NewMessage(newMessage)
 | UnknownMessage(unknownMessage)
 | ErrorOccured(errorOccured)


let getChannels = (userId: int): chatRequest => {
  GetChannels({
    userId: userId
  })
}

let getMessages = (userId: int, channelId: int): chatRequest => {
  GetMessages({
    userId: userId,
    channelId: channelId
  })
}

let sendMessage = (userId: int, channelId: int, message: string): chatRequest => {
  SendMessage({
    userId: userId,
    channelId: channelId,
    message: message
  })
}

let selectChannel = (userId: int, channelId: int): chatRequest => {
  SelectChannel({
    userId: userId,
    channelId: channelId
  })
}

let decodeChannel = json: Channel.model => {
  open! Json.Decode;
  {
    id: json |> field("id", int),
    name: json |> field("name", string)
  }
}

let decodeAllChannels = json: allChannels => {
  open! Json.Decode;
  {
    userId: json |> field("userId", int),
    channels: json |> field("channels", array(decodeChannel))
  }
}

let decodeUser = json: User.model => {
  open! Json.Decode;
  {
    id: json |> field("id", int),
    name: json |> field("name", string)
  }
}

let decodeUserInfo = json: userInfo => {
  open! Json.Decode;
  {
    userId: json |> field("userId", int),
    user: json |>  field("user", decodeUser)
  }
}

let decodeMessage = json: Message.model => {
  open! Json.Decode;
  {
    id: json |> field("id", int),
    from: json |> field("from", decodeUser),
    content: json |> field("content", string)
  }
}

let decodeAllMessages = json: allMessages => {
  open! Json.Decode;
  {
    userId: json |> field("userId", int),
    channelId: json |> field("channelId", int),
    messages: json |> field("messages", array(decodeMessage))
  }
}

let decodeNewMessage = json: newMessage => {
  open! Json.Decode;
  {
    userId: json |> field("userId", int),
    channelId: json |> field("channelId", int),
    message: json |> field("message", decodeMessage)
  }
}

let decodeUnknownMessage = json: unknownMessage => {
  open! Json.Decode;
  {
    userId: json |> field("userId", int),
    content: json |> field("content", string)
  }
}

let decodeErrorOccured = json: errorOccured => {
  open! Json.Decode;
  {
    userId: json |> field("userId", int),
    code: json |> field("code", int)
  }
}


let decodeChatResponse = json: chatResponse => {
  open! Json.Decode;
  let decoder = field("type", int) |> andThen(
    fun
    | 4 => decodeAllChannels |> map(res => AllChannels(res))
    | 6 => decodeAllMessages |> map(res => AllMessages(res))
    | 7 => decodeUserInfo |> map(res => UserInfo(res))
    | 8 => decodeNewMessage |> map(res => NewMessage(res))
    | 9 => decodeUnknownMessage |> map(res => UnknownMessage(res))
    | 10 => decodeErrorOccured |> map(res => ErrorOccured(res))
    | _ => failwith("unknown node type"),
  );
  json |> decoder
}

let encodeSelectChannel = (req: selectChannel): Js.Json.t => {
  open! Json.Encode;
  object_([
    ("type", int(1)),
    ("userId", int(req.userId)),
    ("channelId", int(req.channelId))
  ])
}

let encodeGetChannels = (req: getChannels): Js.Json.t => {
  open! Json.Encode;
  object_([
    ("type", int(3)),
    ("userId", int(req.userId))
  ])
}

let encodeSendMessage = (req: sendMessage): Js.Json.t => {
  open! Json.Encode;
  object_([
    ("type", int(2)),
    ("userId", int(req.userId)),
    ("channelId", int(req.channelId)),
    ("message", string(req.message))
  ])
}

let encodeGetMessages = (req: getMessages): Js.Json.t => {
  open! Json.Encode;
  object_([
    ("type", int(5)),
    ("userId", int(req.userId)),
    ("channelId", int(req.channelId))
  ])
}

let encodeChatRequest = (req: chatRequest): string => {
  let encoded = switch req {
  | SelectChannel(selectChannel) => encodeSelectChannel(selectChannel)
  | GetChannels(get_channels) => encodeGetChannels(get_channels)
  | GetMessages(get_messages) => encodeGetMessages(get_messages)
  | SendMessage(send_message) =>  encodeSendMessage(send_message)
  };
  Json.stringify(encoded);
}