/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/**
 * The changeDetection module will be the abstraction layer for enabling either WebSocket-based or polling based change
 * detection. The change listeners should not be invoked directly.
 * 
 * WebSocket documentation from:
 * https://developer.mozilla.org/en-US/docs/Web/API/WebSocket
 * https://developer.mozilla.org/en-US/docs/WebSockets/Writing_WebSocket_client_applications
 */
define([ 'dojo/Deferred', '../_topicUtil' ], function(Deferred, topicUtil) {

  var connectionAttempt;
  var websocket = null;
  var hasConnected = false;

  return {

    /**
     * Attempts to start the push notification change listener. Because this relies on
     * the server being websocket-enabled, this is could fail (in fact likely will for
     * quite a few releases, until EE7 is standard). Because push notification is the
     * preferred mechanism, this will always be attempted, and when it works will be used.
     * 
     * @return {Deferred} Returns a Deferred which resolves if the websocket was able to connect, or is rejected if the websocket connection could not be established (for any reason).
     */
    start: function() {
      console.log("Attempting to start the push notification mechanism. This relies on websockets, which may not be enabled on the Admin Center server");
      connectionAttempt = new Deferred();
      var websocket = createWebSocket(createNotifyURL());
      // IE9 fix
      if (websocket !== null && websocket !== undefined) {
        console.log('Created WebSocket for URL: ' + websocket.url, websocket);
        reportWebSocketState(websocket);
      }

      return connectionAttempt;
    },

    /**
     * Stops the push notification change listener.
     * In essence, close the websocket.
     */
    stop: function() {
      console.log("Stopping the push notification mechanism");
      if (websocket) {
        // It may be helpful to examine the socket's bufferedAmount attribute before attempting to close the connection to determine if any data has yet to be transmitted on the network.
        websocket.close();        
      }
    }

  };

  /**
   * Determines the WSS URL based on the current host/port.
   */
  function createNotifyURL() {
    console.log('Using window.location.host value of ' + window.location.host);
    return 'wss://'+window.location.host+'/ibm/api/collective/notify';
  }

  /**
   * Documentation lifted from: https://developer.mozilla.org/en-US/docs/Web/API/WebSocket
   * 
   * Ready state constants
   * These constants are used by the readyState attribute to describe the state of the WebSocket connection.
   * Constant  Value   Description
   * CONNECTING  0   The connection is not yet open.
   * OPEN  1   The connection is open and ready to communicate.
   * CLOSING   2   The connection is in the process of closing.
   * CLOSED  3   The connection is closed or couldn't be opened.
   * @param {WebSocket} ws The WebSocket to report the state of
   */
  function reportWebSocketState(ws) {
    switch(ws.readyState) {
    case 0:
      console.log('CONNECTING  0   The connection is not yet open.');
      break;
    case 1:
      console.log('OPEN        1   The connection is open and ready to communicate.');
      break;
    case 2:
      console.log('CLOSING     2   The connection is in the process of closing.');
      break;
    case 3:
      console.log('CLOSED      3   The connection is closed or couldn\'t be opened.');
      break;
    }
  }

  /**
   * Process the updates sent by the server. This essentially loops through the updates
   * array and routes them to the correct topic.
   */
  function process(updates) {
    console.log("Processing updates", updates);
    var i;
    for (i = 0; i < updates.length; i++) {
      var update = updates[i];
      topicUtil.publish(topicUtil.getTopic(update), update);
    }
  }

  /**
   * Notes:
   * The web socket seems to connect the moment its created, but it does so async.
   * In other words, it gets created, but when the thread that created it looses control
   * it actually does the connection and starts invoking the defined callbacks, if there
   * are any.
   * 
   * @returns {WebSocket}
   */
  function createWebSocket(url) {
    console.log('Creating Collective REST API push notifications websocket');
    var ws;
    try {
      ws = new WebSocket(url);
    } catch(e) {
      console.log('Caught error when creating WebSocket', e);
      connectionAttempt.reject('Collective REST API push notifications is not enabled. WebSocket could not be created');
      return;
    }

    /**
     * When the websocket opens, indicate the connection, resolve the connectionAttempt Deferred
     * and start the version handshake.
     */
    ws.onopen = function (event) {
      console.log('Successfully opened Collective REST API push notifications websocket to ' + url);

      // onopen is never called if the connection could not be established, so this is how we can check to see if
      // we were closed without being opened
      hasConnected = true;

      reportWebSocketState(ws);
      ws.send(JSON.stringify({op:"requestVersions"}));
    };

    /**
     * Log the error.
     */
    ws.onerror = function (event) {
      console.warn('Collective REST API push notifications websocket had an error', event);
      reportWebSocketState(ws);
      if (!hasConnected) {
        connectionAttempt.reject('Collective REST API push notifications is not available. Push notifications disabled');
      } else {
        // Try to reconnect using websockets.  If that fails, we'll try polling.
        hasConnected = false;
        connectionAttempt.resolve('Collective REST API push notifications connection lost.');
      }
    };

    /**
     * When the websocket closes, decide if we should reject the connection attempt or not.
     * Reject only when we've never been connected before.
     */
    ws.onclose = function (event) {
      console.warn('Collective REST API push notifications websocket is now closed', event);
      reportWebSocketState(ws);

      if (!hasConnected) {
        connectionAttempt.reject('Collective REST API push notifications is not available. Push notifications disabled');
      } else {
        // Try to reconnect using websockets.  If that fails, we'll try polling.
        hasConnected = false;
        connectionAttempt.resolve('Collective REST API push notifications connection lost.');
      }
    };

    /**
     * Handles the messages sent by the server.
     * Text received over a WebSocket connection is in UTF-8 format and should be in JSON format.
     */
    ws.onmessage = function(event) {
      var msg;
      try {
        msg = JSON.parse(event.data);
      } catch(e) {
        console.error("Message sent by server is not properly formatted JSON. Ignoring message: ", event.data, e);
      }

      if (!msg) {
        // No or invalid message data, nothing to do
        return;
      }

      switch(msg.op) {
      case 'supportedVersions':
        console.log("Server reported the following supported versions: " + msg.versions);
        // Right now, we are hard-coding version 1 because its the only thing the server supports
        var registerV1 = {op:'register', version: 'v1', clientId: 'AdminCenter-Explore'};
        this.send(JSON.stringify(registerV1));
        break;
      case 'registration':
        console.log('The server reported that the registration was ' + (msg.success ? 'successful' : 'did not succeed'));
        if (msg.success) {
          connectionAttempt.progress('Successfully registered for push notifications.', true);
        } else {
          connectionAttempt.reject('Collective REST API push notifications could be not enabled. Registration was not accepted.');
        }
        break;
      case 'notify':
        process(msg.updates);
        break;
      default:
        console.log('Message sent by server is not one of the supported JSON messages', msg);
      }
    };

    return ws;
  };

});
