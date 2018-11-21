
type event = Webapi.Dom.CustomEvent.t;
type socket;

[@bs.deriving abstract]
type options = {
  timeout: int,
  maxAttempts: int,
  onopen: event => unit,
  onreconnect: event => unit,
  onmaximum: event => unit,
  onmessage: event => unit,
  onclose: event => unit,
  onerror: event => unit
};


[@bs.new] [@bs.module "sockette"] external connect: (string, options) => socket = "default"
[@bs.send] external send: (socket, string) => unit = "send";
[@bs.send] external close: socket => unit = "close";

[@bs.get] external data: event => string = "data"