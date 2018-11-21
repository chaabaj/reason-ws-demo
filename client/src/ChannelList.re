
[%bs.raw {|require('./channel-list.css')|}];


type model = array(Channel.model)

let component = ReasonReact.statelessComponent("ChannelList")

let isSelected = (maybeSelected: option(Channel.model), current: Channel.model): bool => {
  open! Belt;
  maybeSelected 
  |> Option.map(_, selected => selected.id == current.id)
  |> Option.getWithDefault(_, false)
}

let make = (~channels: model, ~selectedChannel: option(Channel.model), ~onSelect: (Channel.model => unit), _children) => {
  ...component,
  render: _self => 
    <div className="channel-list">
      <h3>{ReasonReact.string("Channels")}</h3>
      {
        ReasonReact.array(
          Array.map(
            (channel: Channel.model) => 
              <Channel 
                key={string_of_int(channel.id)} 
                channel=channel
                selected={isSelected(selectedChannel, channel)}
                onSelect=onSelect />,
            channels
          )
       )
      }
    </div>
}