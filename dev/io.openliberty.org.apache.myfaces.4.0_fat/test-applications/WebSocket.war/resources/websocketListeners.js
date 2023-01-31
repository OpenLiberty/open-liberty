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
