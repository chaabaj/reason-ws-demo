
[%bs.raw {|require('./app.css')|}];

[@bs.module] external logo : string = "./logo.svg";

open Network;
open Belt;
open Sync;

type action =
  | SetNickname(string)
  | Connect
  | NetAction(Network.action)
  | SyncIn(syncin)
  | SelectChannel(Channel.model)

type model = {
  network: Network.model,
  nickname: string,
  channels: ChannelList.model,
  messages: MessageList.model,
  sync: Sync.model
}

let initialState: model = {
  network: Network.initialState,
  nickname: "",
  channels: [||],
  messages: [||],
  sync: Sync.initialState
}

let component = ReasonReact.reducerComponent("App");

let liftNetworkAction = (sendAction: (action => unit)) => (action: Network.action): unit =>
  sendAction(NetAction(action))

let makeUrl = (nickname: string): Network.url =>
  "ws://localhost:10000/connect/" ++ nickname

let add_message = (msg: Message.model, messages: MessageList.model): MessageList.model =>
  Array.concat(messages, [|msg|])

let getCurrentChannelName = (selectedChannel: option(Channel.model)): string =>
  selectedChannel 
  |> Option.map(_, channel => channel.name) 
  |> Option.getWithDefault(_, "No channel selected")

let make = (_children) => {
  ...component,
  initialState: () => initialState,
  reducer: (action, state) =>
    switch (action) {
      | SetNickname(nickname) =>
        ReasonReact.Update({...state, nickname});
      | SyncIn(ConnectedUser(user)) =>
        let newState = {...state, sync: setConnectedUser(state.sync, user)}
        ReasonReact.UpdateWithSideEffects(
          newState,
          self => self.send(NetAction(syncout(newState.sync, GetChannels)))
        )
      | SyncIn(UpdateChannels(channels)) =>
        ReasonReact.Update({...state, channels});
      | SelectChannel(channel) =>
        let newState = {...state, sync: setSelectedChannel(state.sync, channel)}
        ReasonReact.UpdateWithSideEffects(
          newState, 
          self => {
            self.send(NetAction(syncout(newState.sync, SwitchChannel(channel))))
            self.send(NetAction(syncout(newState.sync, GetMessages)))
          }
        )
      | SyncIn(UpdateMessages(messages)) =>
        ReasonReact.Update({...state, messages})
      | SyncIn(AddMessage(message)) =>
        ReasonReact.Update({...state, messages: add_message(message, state.messages)})
      | Connect =>
        ReasonReact.SideEffects(
          self => connect(makeUrl(self.state.nickname), liftNetworkAction(self.send))
        )
      | NetAction(ResponseReceived(msg)) =>
        ReasonReact.SideEffects(
          self => self.send(SyncIn(syncin(msg)))
        )
      | NetAction(netAction) =>
        ReasonReact.Update({...state, network: updateNetwork(state.network, netAction)})
    },
  render: self =>
    <main className="App">
      <section className="channel-list-container">
        <h3 className="title">{ReasonReact.string("Sample Chat")}</h3>
        <form onSubmit={_ => self.send(Connect)} action="#" className="connect-form">
          <input value={self.state.nickname}
                 placeholder="Enter a nickname..."
                 type_="text"
                 onChange={evt => self.send(SetNickname(ReactEvent.Form.currentTarget(evt)##value))}  />
          <input type_="submit" value="Connect" />
        </form>
        <ChannelList 
          channels={self.state.channels}
          selectedChannel={self.state.sync.selectedChannel}
          onSelect={channel => 
            self.send(
              SelectChannel(channel)
            )} />
      </section>
      <section className="message-list-container"> 
        <MessageList 
          messages={self.state.messages} 
          channelName={getCurrentChannelName(self.state.sync.selectedChannel)} /> 
        <MessageInput onSend={content => 
          self.send(
            NetAction(
              syncout(self.state.sync, SendMessage(content))
            )
          )} />
      </section>
    </main>,
};
