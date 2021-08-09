/*
    Copyright (c) 2017, 2018 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License v1.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-v10.html
   
    Contributors:
        IBM Corporation - initial API and implementation
*/
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