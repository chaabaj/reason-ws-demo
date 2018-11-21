[%bs.raw {|require('./channel.css')|}];

type model = {
  id: int,
  name: string
}

let component = ReasonReact.statelessComponent("Channel")

let make = (~channel: model, ~selected: bool, ~onSelect: (model => unit), _children) => {
  ...component,
  render: _self => <p 
    className={selected ? "channel selected" : "channel"}
    onClick={_ => onSelect(channel)}> 
      {ReasonReact.string(channel.name)} 
    </p>
}