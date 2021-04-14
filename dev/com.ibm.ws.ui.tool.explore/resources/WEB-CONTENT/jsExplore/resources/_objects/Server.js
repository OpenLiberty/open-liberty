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
define([ 'dojo/_base/declare', 'dojo/_base/lang', 'dojo/Deferred', 'dojo/request', '../_topicUtil', 'jsExplore/utils/constants',
         './_StatefulResource', '../_util', '../utils', 'jsShared/utils/apiMsgUtils', 'dojo/i18n!../nls/resourcesMessages' ],
         function(declare, lang, Deferred, request, topicUtil, constants, StatefulResource, util, utils, apiMsgUtils, resourcesMessages) {

  /**
   * The Server object represents a server which is a member of the collective. The server may have applications
   * deployed to it, may belong to a cluster, and may have a scaling policy.
   * 
   * The Server object contains the set of applications deployed to it. The set of applications contain the tally
   * and list of IDs.
   * 
   * The Server object is created based on the REST API response. See the ResourceManager
   * for details on how the REST API response is converted to the initialization object.
   * 
   * The cumulative public attributes for Server are as follows:
   * @class
   * @typedef {Object} Server
   * @property {string} id                    - The resource's unique ID within the set of same type (inherited and set by ObservableResource)
   * @property {string} type                  - The resource's type
   * @property {string} runtimeType           - Runtime - 'Liberty', 'Node.js'  (defined in constants.js)
   * @property {string} containerType         - 'Docker'  (defined in constants.js)
   * @property {string} name                  - The resource's display name, defaults to the ID
   * @property {string} state                 - The resource's state [STARTED, STOPPED or UNKNOWN] (inherited and set by StatefulResource)
   * @property {string} userdir               - The server's user directory
   * @property {string} host                  - The host on which the server is defined
   * @property {string} wlpInstallDir         - The runtime directory in use by the server
   * @property {string} cluster               - The cluster to which the server belongs. If null then the server does not belong to a cluster.
   * @property {string} scalingPolicy         - The scaling policy name which is applied to this server. If null then no policy is applied.
   * @property {boolean} scalingPolicyEnabled - Flag to indicate whether or not the scaling policy which is applied to this server is enabled
   * @property {boolean} isAdminCenterServer  - The flag to indicate if the server is hosting the Admin Center
   * @property {boolean} isCollectiveController - The flag to indicate if the server is a collective controller
   * @property {string} explorerURL           - URL to formatted Liberty REST APIs if server has apiDiscovery feature (ibm/api/explorer)
   * @property {Object} apps                  - The set of applications which are deployed to the server
   * @property {number} apps.up               - The number of applications which are running
   * @property {number} apps.down             - The number of applications which are stopped
   * @property {number} apps.unknown          - The number of applications which are in an unknown state
   * @property {Array.<Object>} apps.list     - The list of applications which are deployed to server. The list will have no duplicate values.
   * @property {string} apps.list[i].name     - The name of the application
   * @property {string} apps.list[i].state    - The state of the application
   * @property {Alerts} alerts                - The set of alerts that apply to the resource (inherited and set by StatelessResource)
   * @property {function} start               - The start operation for the resource
   * @property {function} stop                - The stop operation for the resource
   * @property {function} restart             - The restart operation for the resource
   * @property {function} setMaintenanceMode  - Enable maintenance mode operation the resource
   * @property {function} unsetMaintenanceMode - Disable maintenance mode operation for the resource
   * @property {string} maintenanceMode       - The state of the maintenance mode of the resource, could be one of these: inMaintenanceMode/notInMaintenanceMode/alternateServerStarting/alternateServerUnavailable
   * @property {string} ports                 - The ports the server is using
   * @property {string} note                  - The notes associated to the resource
   * @property {string} owner                 - The owner associated to the resource
   * @property {Array.<string>} contacts      - The list of contacts associated to the resource
   * @property {Array.<string>} tags          - The list of tags associated to the resource
   */

  return declare('ServerResource', [StatefulResource], {
    /** Hard-code the type to be 'server' **/
    /** @type {string} */ type: 'server',

    /** @type {string} */ runtimeType: null,    
    /** @type {string} */ containerType: null,  
    
    /** These values are computed from the 'id', they cannot be injected **/
    /** @type {string} */ host: null,
    /** @type {string} */ userdir: null,
    /** @type {string} */ name: null,

    /** Set during construction and handleChangeEvent **/
    /** @type {string} */ wlpInstallDir: null,
    /** @type {string} */ cluster: null,
    /** @type {string} */ scalingPolicy: null,
    /** @type {boolean} */ scalingPolicyEnabled: null,
    /** @type {boolean} */ isCollectiveController: null,
    /** @type {Object} */ apps: null,
    /** @type {string} */ maintenanceMode: null,
    /** @type {string} */ explorerURL: null,
    /** @type {string} */ ports: null,

    /** Set by constructor only **/
    /** @type {boolean} */ isAdminCenterServer: false,

    /**
     * Construct the initial Server state.
     * 
     * @param {Object} init The initialization object
     */
    constructor: function(init) {
      __updateNameComponents(this);
      (init.wlpInstallDir) ? this.wlpInstallDir = init.wlpInstallDir : this._throwError('Server "'+this.id+'" created without an initial wlpInstallDir');
      (init.runtimeType) ? this.runtimeType = init.runtimeType : this.runtimeType = constants.RUNTIME_LIBERTY;
      if (init.containerType) { this.containerType = init.containerType; }  // Set if present
      if (init.cluster) { this.cluster = init.cluster; } // Set if present
      if (init.scalingPolicy) { this.scalingPolicy = init.scalingPolicy; } // Set if present
      if (init.hasOwnProperty('scalingPolicyEnabled')) { this.scalingPolicyEnabled = init.scalingPolicyEnabled; } // Set if present
      if (init.hasOwnProperty('isCollectiveController')) { this.isCollectiveController = init.isCollectiveController; } // Set if present
      if (init.hasOwnProperty('maintenanceMode')) { this.maintenanceMode = init.maintenanceMode; } // Set if present
      (init.isAdminCenterServer) ? this.isAdminCenterServer = init.isAdminCenterServer : this.isAdminCenterServer = false;
      if (init.hasOwnProperty('explorerURL')) { this.explorerURL = init.explorerURL; } // Set if present
      (init.ports) ? this.ports = init.ports : this.ports = { httpPorts: ["9080"], httpsPorts: ["9443"] };

      (init.apps) ? this.apps = init.apps : this._throwError('Server "'+this.id+'" created without an initial apps object');
      (init.apps.up >= 0) ? this.apps.up = init.apps.up : this._throwError('Server "'+this.id+'" created without an initial apps.up tally');
      (init.apps.down >= 0) ? this.apps.down = init.apps.down : this._throwError('Server "'+this.id+'" created without an initial apps.down tally');
      (init.apps.unknown >= 0) ? this.apps.unknown = init.apps.unknown : this._throwError('Server "'+this.id+'" created without an initial apps.unknown tally');
      (Array.isArray(init.apps.list)) ? this.apps.list = init.apps.list : this._throwError('Server "'+this.id+'" created without an initial apps.list array');

      // Be paranoid, ensure the list is is a list of name/state objects. We don't do this type checking elsewhere because everything else is supposed to be
      // Strings, and this is the one deviant case.
      for (var i = 0; i < this.apps.list.length; i++) {
        var curApp = this.apps.list[i];
        if (!(curApp.hasOwnProperty('name') && curApp.hasOwnProperty('state'))) {
          this._throwError('Server "'+this.id+'" created without an initial apps.list array with some elements which are not objects with name and state');
        }
      }
    },

    /**
     * Handle a change event for this resource.
     * 
     * The event can have the following properties:
     * Server Event {
     *   type: 'server',
     *   id: 'tuple',
     *   state, wlpInstallDir, cluster, scalingPolicy, scalingPolicyEnabled, (optional)
     *   explorerURL, (optional)
     *   apps: {
     *     up, down, unknown,
     *     added: [ { name, state } ],   (optional)
     *     removed: [ name ],   (optional)
     *     changed: [ { name, state } ]   (optional)
     *   }, (optional)
     *   note, owner, contacts, tags,   (optional)
     *   alerts (optional)
     * }
     * 
     * Operational State Event {
     *   type: 'server',
     *   id: 'tuple',
     *   state: 'STARTING|STOPPING'
     * }
     * 
     * If the inbound event has the wrong id or type, the event is ignored. This does not provide any sort of security,
     * but does guard against programming errors and will make the code more serviceable / robust.
     * 
     * The following observer methods may be called:
     * -- onStateChange(newState, oldState) - parameters are Strings
     * -- onWlpInstallDirChange(newWlpInstallDir, oldWlpInstallDir) - parameters are Strings
     * -- onClusterChange(newCluster, oldCluster) - parameters are Strings
     * -- onScalingPolicyChange(newScalingPolicy, oldScalingPolicy) - parameters are Strings
     * -- onScalingPolicyEnabledChange(newScalingPolicyEnabled, oldScalingPolicyEnabled) - parameters are boolean
     * -- onIsCollectiveControllerChange(newIsCollectiveController, oldIsCollectiveController) - parameters are boolean
     * -- onAppsTallyChange(newTally, oldTally) - parameters are objects: {up, down, unknown, partial}
     * -- onAppsListChange(newList, oldList, added, removed, changed) - parameters are Array.{name,state} except for removed, which is Array.<string>
     * -- onDestroyed() - no parameters
     * -- onAlertsChange(newAlerts, oldAlerts) - parameters are Strings
     * -- onTagsChange(newTags, oldTags) - parameters are {String[]}
     * -- onOwnerChange(newOwner, oldOwner) - parameters are {String}
     * -- onContactsChange(newContacts, oldContacts) - parameters are {String[]}
     * -- onNoteChange(newNote, oldNote) - parameters are {String}
     * -- onApiDiscoveryChange() 
     * -- onPortsChange()
     * 
     * @param {Object} e The received Server Event object
     */
    _handleChangeEvent: function(e) {
      console.log('Server '+this.id+' got an event!', e);

      if (e.id !== this.id) { 
        console.error('Server '+this.id+' got an event which did not have the correct id. The event will be ignored. Received event id: ' + e.id);
        return;
      }
      if (e.type !== this.type) { 
        console.error('Server '+this.id+' got an event which did not have the correct type. The event will be ignored. Received event type: ' + e.type);
        return;
      }

      // If we've been removed, trigger the destroy
      if (e.state === 'removed') {
        this.isDestroyed = true;
        this._notifyObservers('onDestroyed');
        return;
      }

      this._updateAttribute('onStateChange', e, 'state');
      this._updateAttribute('onApiDiscoveryChange', e, 'state');
      
      this._updateAttribute('onWlpInstallDirChange', e, 'wlpInstallDir');
      this._updateAttribute('onClusterChange', e, 'cluster');
      this._updateAttribute('onScalingPolicyChange', e, 'scalingPolicy');
      this._updateAttribute('onScalingPolicyEnabledChange', e, 'scalingPolicyEnabled');
      this._updateAttribute('onIsCollectiveControllerChange', e, 'isCollectiveController');
      this._updateAttribute('onMaintenanceModeChange', e, 'maintenanceMode'); // send maintenance change notification if cache != incoming
      this._updateAttribute('onApiDiscoveryChange', e, 'explorerURL');
      
      this._updateAttribute('onTagsChange', e, 'tags');
      this._updateAttribute('onOwnerChange', e, 'owner');
      this._updateAttribute('onContactsChange', e, 'contacts');
      this._updateAttribute('onNoteChange', e, 'note');
      this._updateAttribute('onPortsChange', e, 'ports');
      
      this._updateAttribute('onAlertsChange', e, 'alerts');

      if (e.apps) {
        if ( e.hasOwnProperty("maintenanceMode") && this.hasOwnProperty("maintenanceMode") && (this.maintenanceMode != e.maintenanceMode ) ) {
          this._notifyObservers('onMaintenanceModeChange', e.maintenanceMode);
        }

        // Process removals and changes at the same time. This maybe an opportunity to refactor _updateArray?
        // Probably not the most efficient implementation here, but it should work for now as the lists should be small.
        if (e.apps.added || e.apps.changed || e.apps.removed) {
          var prevList = this.apps.list.slice(0); // Make a shallow copy of the list

          // If we have anything that was removed or change, handle that first
          if (e.apps.changed || e.apps.removed) {
            for (var i = (this.apps.list.length - 1); i >= 0; i--) {
              var curApp = this.apps.list[i];
              if (e.apps.removed) {
                // If it matches removal, splice it out
                for (var r = 0; r < e.apps.removed.length; r++) {
                  if (e.apps.removed[r] === curApp.name) 
                    this.apps.list.splice(i, 1); // Remove the entry in the apps list
                }              
              }
              if (e.apps.changed) {
                // If it matches changed, update it
                for (var c = 0; c < e.apps.changed.length; c++) {
                  var changedApp = e.apps.changed[c];
                  if (changedApp.name === curApp.name) {
                    if (changedApp.state) {
                      curApp.state = changedApp.state;  
                    }
                    if (changedApp.hasOwnProperty('tags')) {
                      curApp.tags = changedApp.tags;
                    }
                    if (changedApp.hasOwnProperty('owner')) {
                      curApp.owner = changedApp.owner;
                    }
                    if (changedApp.hasOwnProperty('contacts')) {
                      curApp.contacts = changedApp.contacts;
                    }
                    if (changedApp.hasOwnProperty('note')) {
                      curApp.note = changedApp.note;
                    }
                  }
                }
              }
            }
          }

          // Finally append new additions
          if (e.apps.added) {
            Array.prototype.push.apply(this.apps.list, e.apps.added);
          }
          this._notifyObservers('onAppsListChange', [this.apps.list, prevList, e.apps.added, e.apps.removed, e.apps.changed]);
        }
        this._updateTally('onAppsTallyChange', e.apps, this.apps, ['up', 'down', 'unknown']);
      }
    },

    // TODO: BEGIN REMOVE THIS
    // This is a temporary work around while we refactor the code to rely on resourceManager directly.
    // For now, we'll reference an instance of ResourceManager that is injected post-construction by
    // the ResourceManager code.
    /**
     * Returns a deferred which will resolve with the Host object.
     */
    getHost: function() {
      return this.resourceManager.getHost(this.host);
    },

    /**
     * Returns a deferred which will resolve with the Cluster object.
     */
    getCluster: function() {
      return this.resourceManager.getCluster(this.cluster);
    },

    /**
     * Returns a deferred which will resolve with the AppsOnServer object.
     */
    getApps: function() {
      return this.resourceManager.getAppsOnServer(this);
    },
    // TODO: END REMOVE THIS

    /**
     * Enables the maintenance mode on the server.
     * 
     * @return {Promise} response - The result of the operation. If an error occurred, the result will contain not only the full err object,
     *         but also an 'errMsg' property that can be used to display a prepared message regarding the result. The returned object may
     *         also include a 'showForce' property which indicates that the force flag was set to false, and customer could retry the operation
     *         with force set to true.
     */
    setMaintenanceMode: function(breakAffinity, force) {
      var params = {};
      params.maintainAffinity = !breakAffinity;
      params.force = force;
      console.log("Call enterMaintenanceMode on server with params=" + JSON.stringify(params));
      return __serverOperation(this, 'enterMaintenanceMode', params);
    },

    /**
     * Disables the maintenance mode on the server.
     * 
     * @return {Promise} response - The result of the operation. If an error occurred, the result will contain not only the full err object,
     *         but also an 'errMsg' property that can be used to display a prepared message regarding the result.
     */
    unsetMaintenanceMode: function() {
      console.log("Call exitMaintenanceMode on server.");
      return __serverOperation(this, 'exitMaintenanceMode');
    },

    
    /**
     * Starts the server.
     * 
     * @return {Promise} response - The result of the operation. If an error occurred, the result will contain not only the full err object,
     *         but also an 'errMsg' property that can be used to display a prepared message regarding the result. The returned object may
     *         also include a 'msg' property which would contain output from the server's stdout. Returns null if standalone server.
     */
    start: function() {
      return __serverOperation(this, 'startServer');
    },
    
    /**
     * Starts the server with the clean flag set.
     * 
     * @return {Promise} response - The result of the operation. If an error occurred, the result will contain not only the full err object,
     *         but also an 'errMsg' property that can be used to display a prepared message regarding the result. The returned object may
     *         also include a 'msg' property which would contain output from the server's stdout. Returns null if standalone server.
     */
    startClean: function() {
      return __serverOperation(this, 'startServer', '--clean');
    },

    /**
     * Stops the server.
     * 
     * @return {Promise} response - The result of the operation. If an error occurred, the result will contain not only the full err object,
     *         but also an 'errMsg' property that can be used to display a prepared message regarding the result. The returned object may
     *         also include a 'msg' property which would contain output from the server's stdout. Returns null if standalone server.
     */
    stop: function() {
      return __serverOperation(this , 'stopServer');
    },

    /**
     * Restarts the server. Courtesy function that calls stop followed by start
     * 
     * @return {Promise} response - The result of the operation. If an error occurred, the result will contain not only the full err object,
     *         but also an 'errMsg' property that can be used to display a prepared message regarding the result. The returned object may
     *         also include a 'msg' property which would contain output from the server's stdout. Returns null if standalone server.
     */
    restart: function() {
      return __restart(this);
    }

  });

  /**
   * Updates the Server resource object by expanding the server's id (tuple) into the three separate parts: host, userdir, name
   * 
   * @private
   * @param {Object} server - The Server resource object to update.
   */
  function __updateNameComponents(serverResource) {
    var id = serverResource.id;
    serverResource.host = id.substring(0, id.indexOf(','));
    serverResource.userdir = id.substring(id.indexOf(',') + 1, id.lastIndexOf(','));
    serverResource.name = id.substring(id.lastIndexOf(',') + 1);
  }

  /**
   * See exposed method for JSDoc.
   * @private
   */
  function __restart(server) {
    var deferred = new Deferred();

    __serverOperation(server, 'stopServer').then(function(response) {
      // This errMsg is a known bug on JDK 8 for Mac: http://bugs.java.com/bugdatabase/view_bug.do?bug_id=8022291
      // The message does not come from anything in Liberty and there's nothing we can do to suppress it from going
      // to System.err. It appears consistently in that specific environment. If this error occurs, we should still restart the server because it doesn't affect the server
      if (response.errMsg &&
          !(response.errMsg.indexOf && (response.errMsg.indexOf('Class JavaLaunchHelper is implemented in both ') > -1) && (response.errMsg.indexOf('Which one is undefined') > -1))) {
          // Stop failed, let someone know!
          response.errMsg = lang.replace(resourcesMessages.STOP_FAILED_DURING_RESTART, [ response.errMsg ]);        
          deferred.resolve(response, true);          
      } else {
        // Stop operation completed successfully so now do a start
        __serverOperation(server, 'startServer').then(function(response) {
          // The server will eventually become consistent, so let the change handler set the state
          deferred.resolve(response, true);
        });
      }
    });

    return deferred;
  }

  /**
   * Send the operation state change notification events for the server.
   * We send events to the server and its apps. We do not update tallies
   * here, as the change detection will handle that.
   */
  function sendStateNotification(server, state, origin) {
    console.log('Sending to ' + server.__myTopic);
    util.sendStateNotification(server, state, origin);
    server.getApps().then(function(appsOnServer) {
      for (var i = 0; i < appsOnServer.list.length; i++) {
        var appOnServer = appsOnServer.list[i];
        util.sendStateNotification(appOnServer, state, origin);
      }
    });
  }

  function getServerOperationErrorMessage(server, operation, err) {
    var errMsg;

    if (err && err.response && err.response.data && err.response.data.stackTrace) {
      var stackTrace = err.response.data.stackTrace;
      errMsg = apiMsgUtils.findErrorMsg(stackTrace);
      if (errMsg) {
        return errMsg;
      }
      errMsg = apiMsgUtils.firstLineOfStackTrace(stackTrace);
      if (errMsg) {
        return errMsg;
      } else if (err.response.status) {
        if ( operation === 'enterMaintenanceMode' ) {
          return err.response.status + " " + lang.replace(resourcesMessages.ERROR_SERVER_OPERATION_SET_MAINTENANCE_MODE, [ server.name, server.host, server.userdir ]);
        } else if ( operation === 'exitMaintenanceMode ' ) {
          return err.response.status + " " + lang.replace(resourcesMessages.ERROR_SERVER_OPERATION_UNSET_MAINTENANCE_MODE, [ server.name, server.host, server.userdir ]);
        } else {
          return err.response.status + " " + lang.replace(resourcesMessages.ERROR_SERVER_OPERATION, [ operation, server.name, server.host, server.userdir ]);
        }
      }
    }
    // Unable to determine what caused the operation to fail. Return errMsg regarding the failure
    if ( operation === 'enterMaintenanceMode' ) {
      return err.response.status + " " + lang.replace(resourcesMessages.ERROR_SERVER_OPERATION_SET_MAINTENANCE_MODE, [ server.name, server.host, server.userdir ]);
    } else if ( operation === 'exitMaintenanceMode ' ) {
      return err.response.status + " " + lang.replace(resourcesMessages.ERROR_SERVER_OPERATION_UNSET_MAINTENANCE_MODE, [ server.name, server.host, server.userdir ]);
    } else {
      return lang.replace(resourcesMessages.ERROR_SERVER_OPERATION, [ operation, server.name, server.host, server.userdir ]);
    }
  }

  /**
   * Implementation for Server.start(), Server.stop(), Server.restart(), Server.enterMaintenanceMode() and Server.exitMaintenanceMode().
   * 
   * @private
   * @param {String}
   *          server - The server resource to receive the operation.
   * @param {String}
   *          operation The mode, could be one of these: 'startServer', 'stopServer', 'restartServer', 'enterMaintenanceMode', 'exitMaintenanceMode'.
   * @return {Promise} response - The result of the operation. If an error occurred, the result will contain not only the full err object,
   *         but also an 'errMsg' property that can be used to display a prepared message regarding the result. The returned object may also
   *         include a 'msg' property which would contain output from the server's stdout. Returns null if standalone server.
   *         If the operation is 'enterMaintenanceMode', the return object may contain a 'showForce' property, which indicates that the force flag 
   *         was set to false, and customer could retry the operation with force set to true. 
   */
  function __serverOperation(server, operation, optionalParams) {
    if (utils.isStandalone()) {
      console.error('Server operations are not supported in standalone mode');
      return util.returnValueAsDeferred(null);
    }

    // Fire off an 'acting' event
    var currentState = server.state;
    if (operation === 'stopServer') {
      sendStateNotification(server, 'STOPPING', 'Server.js:449');
    } else if (operation === 'startServer') {
      sendStateNotification(server, 'STARTING', 'Server.js:451');
    } else if (operation === 'enterMaintenanceMode') {
      // do nothing
    } else if (operation === 'exitMaintenanceMode') {
      // do nothing
    } else {
      console.error('__serverOperation, unknown operation: ' + operation);
    }
    
    var url;
    var options;
    if ( operation === "enterMaintenanceMode" || operation === "exitMaintenanceMode" ) {
      // collective/v1/servers/serverID/enterMaintenanceMode
      url = '/ibm/api/collective/v1/servers/' + encodeURIComponent(server.id) + '/' + operation;
      
      options = {
          handleAs: 'json',
          headers: {
            'Content-type': 'application/json'
          },
          data: ( optionalParams === null || optionalParams === undefined ) ? "" : JSON.stringify(optionalParams)
      };
    } else {
      optionalParams = ( optionalParams === null || optionalParams === undefined ) ? 'null' : '{"value":"' + optionalParams + '","type":"java.lang.String"}';
      url = '/IBMJMXConnectorREST/mbeans/WebSphere:feature=collectiveController,type=ServerCommands,name=ServerCommands/operations/' + operation;
      var options = {
          handleAs: 'json',
          headers: {
            'Content-type': 'application/json'
          },
          data: '{"params":[{"value":"' + server.host + '","type":"java.lang.String"},{"value":"' + server.userdir
          + '","type":"java.lang.String"},{"value":"' + server.name
          + '","type":"java.lang.String"},' + optionalParams + '],"signature":["java.lang.String","java.lang.String","java.lang.String","java.lang.String"]}'   
      };
    }
    console.log("POST: URL = [" + url +"], data = [" + JSON.stringify(options) + "]" );
    var deferred = new Deferred();

    request.post(url, options).then(function(response) {
      var opSuccessful = false;
      console.log(response);
      if ( operation === "enterMaintenanceMode" || operation === "exitMaintenanceMode") {
        if (( response instanceof Array ) &&
            response.hasOwnProperty(0) && 
            response[0].hasOwnProperty(server.id)) {
            var obj = response[0];
            if ( obj[server.id] === constants.MAINTENANCE_MODE_FAILURE || obj[server.id] === constants.MAINTENANCE_MODE_NOT_FOUND) {
              response.errMsg = lang.replace(resourcesMessages.ERROR_SERVER_OPERATION_SET_MAINTENANCE_MODE, [ server.name, server.host, server.userdir ]);
            } else if ( obj[server.id] === constants.MAINTENANCE_MODE_ALTERNATE_SERVER_UNAVAILABLE ) {
              response.showForce = true;
            }
          }
      } else {
        // Returns Object {value: Object, type: Object} where value object is {returnCode, stderr, stdout}
        // We really should be checking (response.value.returnCode === '0') to determine success
        if (response && response.value) {
          if (response.value.stdout) {
            opSuccessful = true;
            response.msg = response.value.stdout;
          }
          if (response.value.stderr) {
            response.errMsg = response.value.stderr;
          }
          if (response.value.returnCode === '2' && !response.msg && !response.errMsg) {
            response.errMsg = lang.replace(resourcesMessages.SERVER_NONEXISTANT, [ server.name ]);
          }
        }
      }

      // We used to send the final state as an event, but we do not do this anymore to ensure all affected groups are updated.
      // Do not add the state change notifications back!
      deferred.resolve(response, true);
    },
    function(err) {
      console.log("Error " + err.response.status + " occurred when requesting " + url + ": ", err);
      if ( operation != "enterMaintenanceMode" && operation != "exitMaintenanceMode" ) {
        topicUtil.publish(server.__myTopic, {
          state: currentState,
          origin: 'Server.js:522'
        });
      }

      var errMsg = getServerOperationErrorMessage(server, operation, err);
      console.log(errMsg);
      err.errMsg = errMsg;

      deferred.resolve(err, true);
    });

    return deferred;
  }

});