/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
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
 */
define([ '../utils', './unifiedChangeListener', './standaloneChangeListener', './pushNotifications' ],
    function(utils, unifiedChangeListener, standaloneChangeListener, pushNotifications) {

  var listener = null;
  
  return {
    
    /**
     * Enables the change detection mechanism.
     * The specific mechanism that is enabled will depend on the mode Explore is running in
     * (standalone or controller) and the available server-side support.
     */
    start: function() {

      // Standalone case does not use push notifications, and probably never will
      if (utils.isStandalone()) {
        standaloneChangeListener.start();
        listener = standaloneChangeListener;
      } else {
        // First, have a go with the push notifications
        var me = this;
        pushNotifications.start().then(function (successMsg) {
          // The resolved case is used to handle a lost connection to websockets.  We'll try to reconnect,
          // if we're unable to reconnect using websockets, then we'll fallback to polling.
          console.warn('Collective REST API push notifications connection has been lost, try to reconnect.', successMsg);
          me.start();
        },
        function(err) {
          // The errorback handles the case where we can't connect using websockets, so fallback to polling.
          console.warn('Collective REST API push notifications could not be enabled, falling back to polling.', err);
          unifiedChangeListener.start();
          listener = unifiedChangeListener;
        },
        function(update) {
          // The update handles the case where we connected using websockets
          console.warn('Collective REST API push notifications are now enabled.', update);
          listener = pushNotifications;
        });
      }
    },

    /**
     * Stops the enabled change detection mechanism.
     */
    stop: function() {
      if (listener) {
        listener.stop();
      }
    },
    
    /**
     * Attempts to reconnect to the server when the connection is lost.
     * TODO: This is not currently implemented, but should be soon.
     */
    retry: function() {
      // Do a reconnect
    },
    
    /**
     * Notifies the user that the connection to the server was lost.
     * When the connection is re-established, the notification should clear.
     * 
     * TODO: This is not currently implemented, but should be soon.
     */
    notifyLostConnection: function() {
      // Inform the user they have lost connection to the server
    }

  };

});
