[%bs.raw {|require('./message-input.css')|}];

type model = {
  text: string
}

type action = SetText(string) | SetEmptyText;

let component = ReasonReact.reducerComponent("MessageInput")

let onSendText = (text: string, onSend: (string => unit), sendAction: (action => unit)): unit => {
  onSend(text);
  sendAction(SetEmptyText)
}

let make = (~onSend: (string => unit) ,_children) => {
  ...component,
  initialState: () => {text: ""},
  reducer: (action, _state) =>
    switch (action) {
    | SetText(text) => ReasonReact.Update({text: text})
    | SetEmptyText => ReasonReact.Update({text: ""})
    },
  render: self =>
    <form className="message-input" action="#" onSubmit={_ => onSendText(self.state.text, onSend, self.send)}>
      <input type_="text" value={self.state.text} placeholder="Type message..." onChange=(evt => self.send(SetText(ReactEvent.Form.currentTarget(evt)##value))) />
      <input type_="submit" value="send" />
    </form>
}