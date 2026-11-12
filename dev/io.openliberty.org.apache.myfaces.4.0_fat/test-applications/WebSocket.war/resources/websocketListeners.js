/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Force the faces.js push client to invoke the onerror listener.
 *
 * faces.js only calls onerror when a socket that has successfully connected is closed with a
 * code other than 1000 / 1008. Once the first real connection is established, close it with
 * the custom code 4000. faces.js then calls onerror and reconnects (which is not closed again).
 */
(function() {
	var NativeWebSocket = window.WebSocket;
	var closedOnce = false;

	window.WebSocket = class extends NativeWebSocket {
		constructor(url, protocols) {
			super(url, protocols);
			this.addEventListener("open", function() {
				if (!closedOnce) {
					closedOnce = true;
					this.close(4000, "Forced close to trigger onerror");
				}
			});
		}
	};
})();

function websocketMessageListener(message, channel, event) {
   	document.getElementById("messageId").innerHTML += "Called onmessage listener with message: " + message + "<br/>";
}

function websocketOpenListener(channel) {
	document.getElementById("messageId").innerHTML += "Called onopen listener" + "<br/>";
}

function websocketCloseListener(channel) {
	document.getElementById("messageId").innerHTML += "Called onclose listener" + "<br/>";
}

function websocketErrorListener(code, channel, event) {
	document.getElementById("messageId").innerHTML += "Called onerror listener" + "<br/>";
}
