/*
    Copyright (c) 2017, 2025 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License 2.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-2.0/
    
    SPDX-License-Identifier: EPL-2.0
*/
function websocketMessageListener(message, channel, event) {
    addMessageToPage(message);
	jsf.push.close("websocketId");
}

function websocketOpenListener(channel) {
    addMessageToPage("Called onopen listener");
}

function websocketCloseListener(channel) {
    addMessageToPage("Called onclose listener");
}

function addMessageToPage(message){
    const element = document.getElementById("messageId");
    const div = document.createElement("div"); // for new lines
    div.textContent = message;
    element.appendChild(div);
}
