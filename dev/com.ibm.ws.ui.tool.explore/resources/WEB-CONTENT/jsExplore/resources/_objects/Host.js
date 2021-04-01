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
define([ 'dojo/_base/declare', './_StatelessResource', 'jsExplore/utils/constants', '../_util', 'jsShared/utils/apiMsgUtils',  'dojo/Deferred', 'dojo/request', 'dojo/_base/lang', 'dojo/i18n!../nls/resourcesMessages' ],
    function(declare, StatelessResource, constants, util, apiMsgUtils, Deferred, request, lang, resourcesMessages) {

  /**
   * The Host object represents a host which is registered to the collective. A host has no state in the collective,
   * and therefore no operations. The host may or may not have runtimes, servers or applications deployed to those servers.
   * 
   * The Host object contains the list of runtimes, the set of servers which are present on the host, and a set of
   * applications which are deployed to those servers. The set of servers and applications contain the tally and list of IDs.
   * 
   * The Host object is created based on the REST API response. See the ResourceManager
   * for details on how the REST API response is converted to the initialization object.
   * 
   * The cumulative public attributes for Host are as follows:
   * @class
   * @typedef {Object} Host
   * @property {string} id                    - The resource's unique ID within the set of same type (inherited and set by ObservableResource)
   * @property {string} type                  - The resource's type
   * @property {string} name                  - The resource's display name, defaults to the ID (inherited and set by StatefulResource)
   * @property {string} state                 - The resource's FAKED state [STARTED, PARTIALLY_STARTED, STOPPED]. The state is FAKED because it represents the combined effective state of the servers, and not the host itself.
   * @property {Array.<object>} runtimes      - The list of runtime IDs which are defined on this host. The list will have no duplicate values.
   * @property {Object} servers               - The set of servers which are present of this host
   * @property {number} servers.up            - The number of servers which are running
   * @property {number} servers.down          - The number of servers which are stopped
   * @property {number} servers.unknown       - The number of servers which are in an unknown state
   * @property {Array.<string>} servers.list  - The list of server IDs which are present of this host. The list will have no duplicate values.
   * @property {Alerts} alerts                - The set of alerts that apply to the resource (inherited and set by StatelesslResource)
   * @property {string} note                  - The notes associated to the resource
   * @property {string} owner                 - The owner associated to the resource
   * @property {string} maintenanceMode       - The state of the maintenance mode of the resource, could be one of these: inMaintenanceMode/notInMaintenanceMode/alternateServerStarting/alternateServerUnavailable
   * @property {Array.<string>} contacts      - The list of contacts associated to the resource
   * @property {Array.<string>} tags          - The list of tags associated to the resource
   * @property {function} setMaintenanceMode  - Enable maintenance mode operation the resource
   * @property {function} unsetMaintenanceMode - Disable maintenance mode operation for the resource

   */
  
  return declare('HostResource', [StatelessResource], {
    /** Hard-code the type to be 'host' **/
    /** @type {string} */ type: 'host',

    /** Set during construction and handleChangeEvent **/
    /** @type {Array.<object>} */ runtimes: null,
    /** @type {Object} */ servers: null,
    /** @type {string} */ maintenanceMode: null,

    /**
     * Construct the initial Host state.
     * 
     * @param {Object} init The initialization object
     */
    constructor: function(init) {
      (init.runtimes) && (Array.isArray(init.runtimes.list)) ? this.runtimes = init.runtimes : this._throwError('Host "'+this.id+'" created without an initial runtimes array');

      (init.servers) ? this.servers = init.servers : this._throwError('Host "'+this.id+'" created without an initial servers object');
      (init.servers.up >= 0) ? this.servers.up = init.servers.up : this._throwError('Host "'+this.id+'" created without an initial servers.up tally');
      (init.servers.down >= 0) ? this.servers.down = init.servers.down : this._throwError('Host "'+this.id+'" created without an initial servers.down tally');
      (init.servers.unknown >= 0) ? this.servers.unknown = init.servers.unknown : this._throwError('Host "'+this.id+'" created without an initial servers.unknown tally');
      (Array.isArray(init.servers.list)) ? this.servers.list = init.servers.list : this._throwError('Host "'+this.id+'" created without an initial servers.list array');
      if (init.hasOwnProperty('maintenanceMode')) { this.maintenanceMode = init.maintenanceMode; } // Set if present
      // Fake the Host 'state'
      this.__updateState(false);
    },

    /**
     * Sets the FAKE Host state based on the server tallies.
     * 
     * @param {boolean} notifyObservers Flag to indicate whether or not the notifyObservers method should be called.
     */
    __updateState: function(notifyObservers) {
      var prevState = this.state;

      if (this.servers.up > 0 && this.servers.down === 0 && this.servers.unknown === 0) {
        this.state = 'STARTED';
      } else if (this.servers.up > 0 && (this.servers.down > 0 || this.servers.unknown> 0)) {
        this.state = 'PARTIALLY_STARTED';      
      } else {
        // We used to consider no servers to be 'EMPTY', but that feels weird in the views.
        this.state = 'STOPPED';
      }

      if (notifyObservers && this.state != prevState) {
        this._notifyObservers('onStateChange', [this.state, prevState]);
      }
    },

    /**
     * Handle a change event for this resource.
     * 
     * The event can have the following properties:
     * Host Event {
     *   type: 'host',
     *   id: 'name',
     *   runtimes: {
     *     added: [ {id: "host,path", name: "host,path", type: "runtime"} ],   (optional)
     *     removed: [ {id: "host,path", name: "host,path", type: "runtime"} ]   (optional)
     *   }, (optional)
     *   runtimesMetadata: [
     *     { id: 'runtimeId',
     *       tags: String[], (optional)
     *       owner: String, (optional)
     *       contacts: String[], (optional)
     *       note: String (optional)
     *     }
     *   ], (optional)
     *   servers: {
     *     up, down, unknown,
     *     added: [ "tuple" ],   (optional)
     *     removed: [ "tuple" ]   (optional)
     *   }, (optional)
     *   note, owner, contacts, tags,   (optional)
     *   alerts   (optional)
     * }
     * 
     * If the inbound event has the wrong id or type, the event is ignored. This does not provide any sort of security,
     * but does guard against programming errors and will make the code more serviceable / robust.
     * 
     * The following observer methods may be called:
     * -- onRuntimesChange(newList, oldList, added, removed) - parameters are Array.<string>
     * -- onServersTallyChange(newTally, oldTally) - parameters are objects: {up, down, unknown}
     * -- onServersListChange(newList, oldList, added, removed) - parameters are Array.<string>
     * -- onDestroyed() - no parameters
     * -- onAlertsChange(newAlerts, oldAlerts) - parameters are Strings
     * -- onTagsChange(newTags, oldTags) - parameters are {String[]}
     * -- onOwnerChange(newOwner, oldOwner) - parameters are {String}
     * -- onContactsChange(newContacts, oldContacts) - parameters are {String[]}
     * -- onNoteChange(newNote, oldNote) - parameters are {String}
     * 
     * @param {Object} e The received Host Event object
     */
    _handleChangeEvent: function(e) {
      console.log('Host '+this.id+' got an event!', e);

      if (e.id !== this.id) { 
        console.error('Host '+this.id+' got an event which did not have the correct id. The event will be ignored. Received event id: ' + e.id);
        return;
      }
      if (e.type !== this.type) { 
        console.error('Host '+this.id+' got an event which did not have the correct type. The event will be ignored. Received event type: ' + e.type);
        return;
      }

      // If we've been removed, trigger the destroy
      if (e.state === 'removed') {
        this.isDestroyed = true;
        this._notifyObservers('onDestroyed');
        return;
      }

      if (e.runtimes) {  // Runtimes were added to or removed from the host.
        this._updateArrayOfObjects('onRuntimesChange', this.runtimes.list, e.runtimes.added, e.runtimes.removed);
      }
      
      if (e.runtimesMetadata) { // Runtime metadata changes.  At least one runtime on
                                // the host had metadata updates.
        this._updateRuntimesMetadata(e.runtimesMetadata);
      }
      
      if (e.servers) {
        this._updateTally('onServersTallyChange', e.servers, this.servers, ['up', 'down', 'unknown', 'inMaintenanceMode']);
        this.__updateState(true); // Update the tallies first, then the state and finally the list
        this._updateArray('onServersListChange', this.servers.list, e.servers.added, e.servers.removed);
      }

      this._updateAttribute('onAlertsChange', e, 'alerts');
      this._updateAttribute('onTagsChange', e, 'tags');
      this._updateAttribute('onOwnerChange', e, 'owner');
      this._updateAttribute('onContactsChange', e, 'contacts');
      this._updateAttribute('onNoteChange', e, 'note');
      this._updateAttribute('onMaintenanceModeChange', e, 'maintenanceMode');

    },

    // TODO: BEGIN REMOVE THIS
    // This is a temporary work around while we refactor the code to rely on resourceManager directly.
    // For now, we'll reference an instance of ResourceManager that is injected post-construction by
    // the ResourceManager code.
    /**
     * @returns Deferred which resolves with ServersOnHost.
     */
    getServers: function() {
      return this.resourceManager.getServersOnHost(this);
    },

    /**
     * @returns Deferred which resolves with RuntimesOnHost.
     */
    getRuntimes: function() {
      return this.resourceManager.getRuntimesOnHost(this);
    },

    /**
     * @returns Deferred which resolves with a Runtime object.
     */
    getRuntimeForServer: function(server) {
      return this.resourceManager.getRuntimeForServer(server);
    },
    // TODO: END REMOVE THIS
    
    
    /**
     * Enables the maintenance mode on the host.
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
      console.log("Call enterMaintenanceMode on host with params=" + JSON.stringify(params));
      return __hostOperation(this, 'enterMaintenanceMode', params);
    },

    /**
     * Disables the maintenance mode on the host.
     * 
     * @return {Promise} response - The result of the operation. If an error occurred, the result will contain not only the full err object,
     *         but also an 'errMsg' property that can be used to display a prepared message regarding the result. 
     */
    unsetMaintenanceMode: function() {
      console.log("Call exitMaintenanceMode on host.");
      return __hostOperation(this, 'exitMaintenanceMode');
    },
    
  /**
     * Protected method to determine which runtimes on the host had metadata updates
     * and notify any observers.
     * 
     * The argument to the notifyMethod:
     *     { id: 'runtimeId',
     *       tags: String[],     (optional)
     *       owner: String,      (optional)
     *       contacts: String[], (optional)
     *       note: String        (optional)
     *     }
     * 
     * @param {Object} runtimeMetadataChanges An array of metadata changes for Runtimes
     *                                        on this host.   Each element in the array 
     *                                        represents a different runtime that was
     *                                        updated.
     *                 [{ id: 'runtimeId',
     *                    tags: String[],     (optional)
     *                    owner: String,      (optional)
     *                    contacts: String[], (optional)
     *                    note: String        (optional)
     *                  }]
     */
    _updateRuntimesMetadata: function(runtimeMetadataChanges) {
      for (var i=0; i < runtimeMetadataChanges.length; i++) {
        for (var x = 0; x < this.runtimes.list.length; x++) {
          if (runtimeMetadataChanges[i].id === this.runtimes.list[x].id) {
            if (Array.isArray(runtimeMetadataChanges[i].tags)) { this.runtimes.list[x].tags = runtimeMetadataChanges[i].tags; }
            if (runtimeMetadataChanges[i].owner) { this.runtimes.list[x].owner = runtimeMetadataChanges[i].owner; }
            if (Array.isArray(runtimeMetadataChanges[i].contacts)) { this.runtimes.list[x].contacts = runtimeMetadataChanges[i].contacts; }
            if (runtimeMetadataChanges[i].note) this.runtimes.list[x].note = runtimeMetadataChanges[i].note;
            this._notifyObservers('onRuntimesMetadataChange',[runtimeMetadataChanges[i]]);
          }
        } 
      }
    }
  });

  /**
   * Implementation for Host.enterMaintenanceMode() and Host.exitMaintenanceMode().
   * 
   * @private
   * @param {String}
   *          server - The host resource to receive the operation.
   * @param {String}
   *          operation The mode, either 'enterMaintenanceMode' or 'exitMaintenanceMode'
   * @return {Promise} response - The result of the operation. If an error occurred, the result will contain not only the full err object,
   *         but also an 'errMsg' property that can be used to display a prepared message regarding the result. The returned object may also
   *         include a 'msg' property which would contain output from the server's stdout. Returns null if standalone server.
   */
  function __hostOperation(host, operation, optionalParams) {
    
    // Fire off an 'acting' event
    if (operation === 'enterMaintenanceMode') {
      // do nothing
    } else if (operation === 'exitMaintenanceMode') {
      // do nothing
    } else {
      console.error('__hostOperation, unknown operation: ' + operation);
    }

   //collective/v1/hosts/hostID/enterMaintenanceMode
    var  url = '/ibm/api/collective/v1/hosts/' + encodeURIComponent(host.id) + '/' + operation;
      
    var  options = {
          handleAs: 'json',
          headers: {
            'Content-type': 'application/json'
          },
          data: ( optionalParams === null || optionalParams === undefined ) ? "" : JSON.stringify(optionalParams)
      };
    
    console.log("POST: URL = [" + url +"], data = [" + JSON.stringify(options) + "]" );
    var deferred = new Deferred();

    request.post(url, options).then(function(response) {
      var opSuccessful = false;
      console.log(response);
     
      if (( response instanceof Array ) &&
          response.hasOwnProperty(0) && 
          response[0].hasOwnProperty(host.id)) {
        var obj = response[0];
        if ( obj[host.id] === constants.MAINTENANCE_MODE_FAILURE || obj[host.id] === constants.MAINTENANCE_MODE_NOT_FOUND) {
          response.errMsg = lang.replace(resourcesMessages.ERROR_SERVER_OPERATION_SET_MAINTENANCE_MODE, [ host.name ]);
        } else if ( obj[host.id] === constants.MAINTENANCE_MODE_ALTERNATE_SERVER_UNAVAILABLE ) {
          response.showForce = true;
        }
      }

      // We used to send the final state as an event, but we do not do this anymore to ensure all affected groups are updated.
      // Do not add the state change notifications back!
      deferred.resolve(response, true);
    },
    function(err) {
      console.log("Error " + err.response.status + " occurred when requesting " + url + ": ", err);

      var errMsg = getHostOperationErrorMessage(host, operation, err);
      console.log(errMsg);
      err.errMsg = errMsg;

      deferred.resolve(err, true);
    });

    return deferred;
  }
  
  function getHostOperationErrorMessage(host, operation, err) {
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
          return err.response.status + " " + lang.replace(resourcesMessages.ERROR_HOST_OPERATION_SET_MAINTENANCE_MODE, [ host.name ]);
        } else if ( operation === 'exitMaintenanceMode ' ) {
          return err.response.status + " " + lang.replace(resourcesMessages.ERROR_HOST_OPERATION_UNSET_MAINTENANCE_MODE, [ host.name ]);
        }
      }
    }
    // Unable to determine what caused the operation to fail. Return errMsg regarding the failure
    if ( operation === 'enterMaintenanceMode' ) {
      return err.response.status + " " + lang.replace(resourcesMessages.ERROR_HOST_OPERATION_SET_MAINTENANCE_MODE, [ host.name ]);
    } else if ( operation === 'exitMaintenanceMode ' ) {
      return err.response.status + " " + lang.replace(resourcesMessages.ERROR_HOST_OPERATION_UNSET_MAINTENANCE_MODE, [ host.name ]);
    }
  }

});