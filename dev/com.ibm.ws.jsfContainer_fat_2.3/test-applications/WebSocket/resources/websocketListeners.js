function websocketMessageListener(message, channel, event) {
   	document.getElementById("messageId").innerHTML += message + "<br/>";
	jsf.push.close("websocketId");
}

function websocketOpenListener(channel) {
	document.getElementById("messageId").innerHTML += "Called onopen listener" + "<br/>";
}

function websocketCloseListener(channel) {
	document.getElementById("messageId").innerHTML += "Called onclose listener" + "<br/>";
}
