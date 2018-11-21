[%bs.raw {|require('./message.css')|}];

type model = {
  id: int,
  from: User.model,
  content: string
}

let component = ReasonReact.statelessComponent("Message")


let make = (~message: model, _children) => {
  ...component,
  render: _self => <div className="message">
    <span className="username">{ReasonReact.string(message.from.name ++ " : ")}</span>
    <p className="content">{ReasonReact.string(message.content)}</p>
  </div>
}