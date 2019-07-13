/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
window.addEventListener("message", receiveMessage, false);

/**
 * Get the current browser state value from the browser state cookie.
 * 
 * @returns
 */
function getBrowserState() {
	var bscName = BROWSER_STATE_COOKIE_NAME + "=";
	var cookies = document.cookie.split(';');
	if (!cookies) {
		return null;
	}
	for (var i = 0; i < cookies.length; i++) {
		var cookie = cookies[i].trim();
		if (cookie.indexOf(bscName) == 0) {
			return cookie.substring(bscName.length, cookie.length);
		}
	}
	return null;
}

/**
 * Calculate the current session state based on the provided arguments.
 * 
 * @param clientId
 * @param browserState
 * @param salt
 * @returns A Base64-encoded SHA-256 hash of the concatenation of all of the
 * provided arguments.
 */
function calculateSessionState(clientId, browserState, salt) {
	var stringToHash = clientId + '' + browserState;
	if (salt) {
		stringToHash = stringToHash + '' + salt;
	}
	var sessionState = CryptoJS.SHA256(stringToHash);
	sessionState = sessionState.toString(CryptoJS.enc.Base64);
	if (salt) {
		sessionState = sessionState + '.' + salt;
	}
	return sessionState;
}

/**
 * Callback function for when a postMessage is received. The message is
 * expected to be in the format: Client ID + " " + Session state. The client
 * ID provided and the current browser state are used to calculate the current
 * session state. If the calculated value exactly matches the session state
 * value provided in the message, "unchanged" is postMessaged back to the
 * message sender. Otherwise, "changed" is postMessaged back to the message
 * sender.
 * 
 * @param message
 */
function receiveMessage(message) {
	if (message.origin !== EXPECTED_ORIGIN) {
		console.log("Unable to complete request from " + message.origin);
		return;
	}

	// Required format: Client ID + " " + Session State
	var splitData = message.data.split(" ");
	if (!splitData || splitData.length < 2) {
		console.log("Unable to complete request using given data: " + message.data);
		return;
	}

	var clientId = splitData[0];
	var sessionStateFromClient = splitData[1];

	var browserState = getBrowserState();
	if (!browserState) {
		// No browser state cookie value found
		message.source.postMessage("changed", message.origin);
		return;
	}

	var salt = "";
	var stateAndSalt = sessionStateFromClient.split(".");
	if (stateAndSalt.length > 1 && stateAndSalt[1].length > 0) {
		salt = stateAndSalt[1];
	}

	var sessionState = calculateSessionState(clientId, browserState, salt);

	var msg = "changed";
	// Ensure both the type and value of the two session states are equivalent
	if (sessionStateFromClient === sessionState) {
		msg = "unchanged";
	}
	message.source.postMessage(msg, message.origin);
};
