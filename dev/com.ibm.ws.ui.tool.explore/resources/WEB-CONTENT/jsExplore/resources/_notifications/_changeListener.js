/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/**
 * This is the client-side back-end 'listener' that is the single point of communication
 * between the client-side front-end display and the collective REST APIs.
 * 
 * As of this initial writing, we are not using any WebRTC (Real Time Communications) such
 * as WebSockets, so this has employed a polling / delta calculation approach. Eventually,
 * the client-side logic to query the REST APIs and understand what has changed will be
 * replaced by a server listener a REST API which will push deltas to us. Until that time,
 * we need to implement the change logic ourselves. In short, today we use pull, tomorrow
 * we'll use push.
 *   
 * Currently, this is implemented so that a tick happens at a fixed frequency,
 * regardless of successfulness of the request. The next tick will happen at
 * a fixed interval after the previous tick completes.
 * 
 * Irrespective of how we get the delta (pulled or pushed) this module will remain the single
 * point of generating notifications. We have an event model (currently based on dojo/topic)
 * which notifies elements in the client-side front-end about changes. For full
 * details on our notifications, see:
 * jsExplore/resources/_topicUtil
 * jsExplore/wdigets/shared/StateIcon
 */
define([ 'dojo/_base/declare', 'dojox/timing', 'dojo/request/xhr', 'dojo/_base/window','dojo/i18n!../../nls/explorerMessages','jsShared/utils/imgUtils', 'jsExplore/utils/ID' ],
    function(declare, timing, xhr, win, i18n, imgUtils, ID) {

  var defaultPullFrequency = 2000;/*ms*/ // Default: update every 4 seconds. Should be no less than 1000ms
  var defaultDebug = false;
  var connectionLostDialog = null;

  /**
   * Locally scoped function.
   * Encapsulation of the XHR logic to get the latest REST API response.
   * The caller need only pass in a callback to ensure
   */
  var getAndProcess = function(changeListener, callback) {
    /**
     * The errback does nothing special. We log the error that occurred and we essentially ignore
     * the fact that something went wrong. We can't do anything in the error case besides scheduling
     * an immediate retry.
     */
    var errback = function(err) {
      // If this is a 'cancel' error, then its not really an error, we're just cleaning up
      if (err.message === 'Request canceled' || err.dojoType === 'cancel') {
        console.log(name+'::getAndProcess The current request has been canceled'); 
      } else {
        if (err.response && err.response.status === 401) {
          // We're not authenticated anymore, cancel the timer immediately
          changeListener.stop();
        } else {
          // Page isn't available, or something else has gone wrong. Try at most 3 times.
          changeListener.__failureCount++;
          if (changeListener.__failureCount > 3) {
            console.error(changeListener.name+'::getAndProcess The change listener has encountered 3 request errors. This suggests that the connection has been lost. The change listener will attempt to recover automatically.', err);
            // If connectionLostDialog doesn't exist, create it.  If it's not open, then open it.
            if (!connectionLostDialog) {
              manageConnectionLostDialog();
            }
            if (connectionLostDialog && !connectionLostDialog.open) {
              connectionLostDialog.show();
            }
          }
          console.info(changeListener.name+'::getAndProcess The request encountered an error. Error will be ignored if not ocurring 3 times in a now.', err);
        }
      }
      changeListener.__running = false;
    };

    var req;
    if (changeListener.customOnTickGetAndProcess) {
      req = changeListener.customOnTickGetAndProcess();
    } else {
      var options = {
          handleAs : "json",
          preventCache : true,
          sync : false,
          headers : {
            "Content-type" : "application/json"
          }
      };
      req = xhr.get(changeListener.url, options);
    }
    var manageConnectionLostDialog = function() {
      if (connectionLostDialog && connectionLostDialog.open) {
        // If the dialog is present and open, remove it on successful connection
        connectionLostDialog.destroyRecursive();
        connectionLostDialog = null;
      } else if (!connectionLostDialog) {
        // If the dialog does not exist, create it upon successful connection
        connectionLostDialog = new ConfirmDialog({
          id : ID.getConnectionToServerFailed(),//'connection-to-sever-failed',
          title: i18n.ERROR_MESSAGE,
          confirmDescriptionIcon : imgUtils.getSVGSmallName('status-alert'),
          confirmDescription : i18n.CONNECTION_FAILED_MESSAGE,
          confirmMessage : '',
          confirmButtonLabel : i18n.OK_DEFAULT_BUTTON,
          okFunction : function() {
            connectionLostDialog = null;
          }
        });
        connectionLostDialog.placeAt(win.body());
        connectionLostDialog.startup();
      }
      callback.apply(this, arguments);
    };

    // We do not need an event back
    req.then(manageConnectionLostDialog, errback);
    return req;
  };


  /**
   * Defines the _changeListener. The _changeListener is a parent class used by the resource change
   * listeners.
   */
  return declare("_changeListener", [], {
    /** Default attributes **/
    name: 'defaultChangeListener',
    pullFrequency: defaultPullFrequency,/*ms*/
    debug: defaultDebug,

    /** Injected attributes and  methods **/
    url: null,
    updateProcessor: null,

    /** Internal variables, not to be set by caller **/
    __pull: null,
    __req: null,
    __failureCount: 0, // Used to track how many times an update failed. If we're failing a lot, just stop.
    __isStopped: true,
    __running: false,

    constructor: function(configObj) {
      /** Grab required attributes **/
      this.url = configObj.url;
      this.updateProcessor = configObj.updateProcessor;

      /** Grab default overrides **/
      this.name = configObj.name;
      this.pullFrequency = configObj.pullFrequency;
      this.debug = configObj.debug;

      /** Locally scoped holder. This is used to cancel the request when we are stopped */
      var changeListener = this;

      /** DEFINE SCHEDULED PULL **/
      // Establish a repeating timer.
      this.__pull = new timing.Timer(this.pullFrequency);

      /**
       * For each tick of our pull timer, grab the latest REST API response and generate
       * a delta from this response and the previous response. From this delta, fire the
       * relevant change notification events and then update previous response with the
       * this response.
       */
      this.__pull.onTick = function() {
        if (changeListener.debug) console.log(changeListener.name+' timer has fired');

        if (changeListener.__isStopped) {
          return; // TODO Hacking around the dojo timer stop... which does not seem to be working
        }

        if (changeListener.__running) {
          return; // When we're running, ignore the tick (for when things are running slowly)
        }

        changeListener.__running = true;
        var success = function(response) {
          changeListener.__failureCount = 0; // Reset failure count each time

          if (changeListener.debug) console.log(changeListener.name + ' REST response: ', response);
          if (changeListener.updateProcessor) {
            try {
              changeListener.updateProcessor(response);
            } catch(e) {
              console.error(changeListener.name + ' did not finish its execution of updateProcessor. Error: ', e);              
            }
          }

          changeListener.__running = false;
        };
        changeListener.__req = getAndProcess(changeListener, success, false);
      };

      /**
       * When the pull timer is stopped, cancel the current request.
       */
      this.__pull.onStop = function() {
        console.log(changeListener.name + ' has stopped');
        if (changeListener.__req) {
          changeListener.__req.cancel();
        }
      };
    },

    /**
     * Start the notification engine.
     */
    start: function() {
      if ( this.__isStopped === true ) {
        this.__pull.start();
        this.__isStopped = false;
      }
    },

    /**
     * Change how often the notification engine ticks.
     * TODO: Do we really need this??
     */
    changeFrequency: function(interval/*ms*/) {
      this.__pull.setInterval(interval);
    },

    /**
     * Stop the notification engine.
     */
    stop: function() {
      this.__pull.stop();
      this.__isStopped = true;
    }

  });
});