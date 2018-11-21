
open Protocol;

type url = string

type connection_state =
    Connected
  | Connecting
  | Disconnected

type model = {
  connection_state: connection_state,
  socket: option(Sockette.socket)
}

type action =
  | ConnectionEstablished
  | ResponseReceived(Protocol.chatResponse)
  | Request(Protocol.chatRequest)
  | Disconnected
  | SetSocket(Sockette.socket)

let initialState: model = {
  connection_state: Disconnected,
  socket: None
}

let receiveMessage = (event: Sockette.event): Protocol.chatResponse =>
  Sockette.data(event) |> Json.parseOrRaise |> Protocol.decodeChatResponse

let request = (req: chatRequest): action => Request(req)


let connect = (url: url, sendAction: (action => unit)): unit => {
  let options = Sockette.options(
    ~timeout=200000,
    ~maxAttempts=10,
    ~onopen=(evt => sendAction(ConnectionEstablished)),
    ~onreconnect=(evt => Js.log("Reconnected")),
    ~onmessage={evt =>  sendAction(ResponseReceived(receiveMessage(evt)))},
    ~onmaximum=(evt => Js.log("max reconnection attempts reached")),
    ~onclose=(evt => sendAction(Disconnected)),
    ~onerror=(evt => Js.log("An error occured"))
  )
  sendAction(SetSocket(Sockette.connect(url, options)));
}

let updateNetwork = (state: model, action: action): model =>
  switch (action) {
    | ConnectionEstablished =>
      {...state, connection_state: Connected};
    | Request(req) =>
        switch state.socket {
        | Some(socket) =>
          Sockette.send(socket, Protocol.encodeChatRequest(req))
          state
        | None =>
          state
        };
    | Disconnected =>
      {...state, connection_state: Disconnected}
    | SetSocket(socket) =>
      {...state, socket: Some(socket)}
    | _ =>
      state
  }