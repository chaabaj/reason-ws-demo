
[%bs.raw {|require('./message-list.css')|}];

type model = array(Message.model)

let component = ReasonReact.statelessComponent("MessageList")

let make = (~messages: model, ~channelName, _children) => {
  ...component,
  render: _self => {
    let items = Array.map(
        (message: Message.model) => <Message message=message key={string_of_int(message.id)} />,
        messages
      );
    
      (<article className="message-list">
        <section className="message-list-header">
          <h4>{ReasonReact.string(channelName)}</h4>
        </section>
        {ReasonReact.array(items)}
      </article>)
  }
}