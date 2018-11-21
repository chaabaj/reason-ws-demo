
open Belt;

type model = {
  connectedUser: option(User.model),
  selectedChannel: option(Channel.model)
};

let initialState: model = {
  connectedUser: None,
  selectedChannel: None
};

type syncin =
  ConnectedUser(User.model)
  | UpdateChannels(ChannelList.model)
  | UpdateMessages(MessageList.model)
  | AddMessage(Message.model);

type syncout =
  GetChannels
  | GetMessages
  | SwitchChannel(Channel.model)
  | SendMessage(string);

type action = Network.action

let syncout = (state: model, req: syncout): action => {
  switch req {
  | GetChannels when Option.isSome(state.connectedUser) =>
    let connectedUser = Option.getExn(state.connectedUser);
    Network.Request(
      Protocol.getChannels(
        connectedUser.id
      )
    )
  | GetMessages when Option.isSome(state.connectedUser) && Option.isSome(state.selectedChannel) =>
    let connectedUser = Option.getExn(state.connectedUser);
    let selectedChannel = Option.getExn(state.selectedChannel);
    Network.Request(
      Protocol.getMessages(
        connectedUser.id, selectedChannel.id
      )
    )
  | SendMessage(content) when Option.isSome(state.connectedUser) && Option.isSome(state.selectedChannel) =>
    let connectedUser = Option.getExn(state.connectedUser);
    let selectedChannel = Option.getExn(state.selectedChannel);
    Network.Request(
      Protocol.sendMessage(
        connectedUser.id, selectedChannel.id, content
      )
    )
  | SwitchChannel(channel) when Option.isSome(state.connectedUser) =>
    let connectedUser = Option.getExn(state.connectedUser);
    Network.Request(
      Protocol.selectChannel(
        connectedUser.id, channel.id
      )
    )
  | _ =>
    failwith("cannot build the request connectedUser or selectedChannel are not set")
  };
}

let syncin = (response: Protocol.chatResponse): syncin => {
  switch response {
  | Protocol.UserInfo(userInfo) => ConnectedUser(userInfo.user)
  | Protocol.AllChannels(response) => UpdateChannels(response.channels)
  | Protocol.AllMessages(response) => UpdateMessages(response.messages)
  | Protocol.NewMessage(response) => AddMessage(response.message)
  | Protocol.UnknownMessage(response) =>
    failwith("Unknown message sent to the server" ++ response.content)
  | Protocol.ErrorOccured(response) =>
    failwith("An error occured in the server code" ++ string_of_int(response.code))
  };
}

let setConnectedUser = (state: model, user: User.model): model =>
  {...state, connectedUser: Some(user)}

let setSelectedChannel = (state: model, channel: Channel.model): model =>
  {...state, selectedChannel: Some(channel)}