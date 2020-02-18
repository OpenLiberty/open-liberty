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
define([ 'dojo/_base/declare', 'dojo/_base/lang', 'dojo/Deferred', 'dojo/request',
         './_StatefulResource', '../_util', '../utils', 'jsShared/utils/apiMsgUtils',  'dojo/i18n!../nls/resourcesMessages' ],
         function(declare, lang, Deferred, request, StatefulResource, util, utils, apiMsgUtils, resourcesMessages) {

  /**
   * The Cluster object represents a management cluster defined in the collective. A management cluster
   * is a group of servers which has an effective state and operations which can be applied to all member
   * servers.
   * 
   * The Cluster object contains the set of servers which are members of the cluster, and a set of
   * applications which are deployed to those member servers. The set of servers and applications
   * contain the tally and list of IDs.
   * 
   * The Cluster object is created based on the REST API response. See the ResourceManager
   * for details on how the REST API response is converted to the initialization object.
   * 
   * The cumulative public attributes for Cluster are as follows:
   * @class
   * @typedef {Object} Cluster
   * @property {string} id                    - The resource's unique ID within the set of same type (inherited and set by ObservableResource)
   * @property {string} type                  - The resource's type
   * @property {string} name                  - The resource's display name, defaults to the ID (inherited and set by StatefulResource)
   * @property {string} state                 - The resource's state [STARTED, PARTIALLY_STARTED, STOPPED or UNKNOWN] (inherited and set by StatefulResource)
   * @property {string} scalingPolicy         - The scaling policy name of the cluster. If null then no policy is applied.
   * @property {boolean} scalingPolicyEnabled - Flag to indicate whether or not the scaling policy which is applied to this cluster is enabled
   * @property {Object} servers               - The set of servers which are members of this cluster
   * @property {number} servers.up            - The number of servers which are running
   * @property {number} servers.down          - The number of servers which are stopped
   * @property {number} servers.unknown       - The number of servers which are in an unknown state
   * @property {Array.<string>} servers.list  - The list of server IDs which are members of this cluster. The list will have no duplicate values.
   * @property {Object} apps                  - The set of applications which are deployed to members of this cluster
   * @property {number} apps.up               - The number of applications which are running
   * @property {number} apps.down             - The number of applications which are stopped
   * @property {number} apps.unknown          - The number of applications which are in an unknown state
   * @property {number} apps.partial          - The number of applications which are in a partially started state
   * @property {Array.<Object>} apps.list     - The list of application objects which are deployed to members of this cluster. The list will have no duplicate values.
   * @property {Alerts} alerts                - The set of alerts that apply to the resource (inherited and set by StatelesslResource)
   * @property {function} start               - The start operation for the resource
   * @property {function} stop                - The stop operation for the resource
   * @property {function} restart             - The restart operation for the resource
   * @property {string} note                  - The notes associated to the resource
   * @property {string} owner                 - The owner associated to the resource
   * @property {Array.<string>} contacts      - The list of contacts associated to the resource
   * @property {Array.<string>} tags          - The list of tags associated to the resource
   */
  return declare('Cluster', [StatefulResource], {
    /** Hard-code the type to be 'cluster' **/
    /** @type {string} */ type: 'cluster',

    /** Set during construction and handleChangeEvent **/
    /** @type {string} */ scalingPolicy: null,
    /** @type {boolean} */ scalingPolicyEnabled: null,
    /** @type {Object} */ servers: null,
    /** @type {Object} */ apps: null,
    
    /** @type {string} */ oldState: null,

    /**
     * Construct the initial Cluster state.
     * 
     * @param {Object} init The initialization object
     */
    constructor: function(init) {
      if (init.scalingPolicy) { this.scalingPolicy = init.scalingPolicy; } // Set if present
      if (init.hasOwnProperty('scalingPolicyEnabled')) { this.scalingPolicyEnabled = init.scalingPolicyEnabled; } // Set if present

      (init.servers) ? this.servers = init.servers : this._throwError('Cluster "'+this.id+'" created without an initial servers object');
      (init.servers.up >= 0) ? this.servers.up = init.servers.up : this._throwError('Cluster "'+this.id+'" created without an initial servers.up tally');
      (init.servers.down >= 0) ? this.servers.down = init.servers.down : this._throwError('Cluster "'+this.id+'" created without an initial servers.down tally');
      (init.servers.unknown >= 0) ? this.servers.unknown = init.servers.unknown : this._throwError('Cluster "'+this.id+'" created without an initial servers.unknown tally');
      (Array.isArray(init.servers.list)) ? this.servers.list = init.servers.list : this._throwError('Cluster "'+this.id+'" created without an initial servers.list array');

      (init.apps) ? this.apps = init.apps : this._throwError('Cluster "'+this.id+'" created without an initial apps object');
      (init.apps.up >= 0) ? this.apps.up = init.apps.up : this._throwError('Cluster "'+this.id+'" created without an initial apps.up tally');
      (init.apps.down >= 0) ? this.apps.down = init.apps.down : this._throwError('Cluster "'+this.id+'" created without an initial apps.down tally');
      (init.apps.unknown >= 0) ? this.apps.unknown = init.apps.unknown : this._throwError('Cluster "'+this.id+'" created without an initial apps.unknown tally');
      (init.apps.partial >= 0) ? this.apps.partial = init.apps.partial : this._throwError('Cluster "'+this.id+'" created without an initial apps.partial tally');
      (Array.isArray(init.apps.list)) ? this.apps.list = init.apps.list : this._throwError('Cluster "'+this.id+'" created without an initial apps.list array');
    },

    /**
     * Handle a change event for this resource.
     * 
     * The event can have the following properties:
     * Cluster Event {
     *   type: 'cluster',
     *   id: 'name',
     *   state, scalingPolicy, scalingPolicyEnabled, (optional)
     *   servers: {
     *     up, down, unknown,
     *     added: [ "tuple" ],   (optional)
     *     removed: [ "tuple" ]   (optional)
     *   }, (optional)
     *   apps: {
     *     up, down, unknown, partial,
     *     added: [ { AppOnCluster object } ],   (optional)
     *     changed: [ { AppOnCluster object } ],   (optional)
     *     removed: [ "name" ]   (optional)
     *   }, (optional)
     *   note, owner, contacts, tags,   (optional)
     *   alerts (optional)
     * }
     * 
     * Operational State Event {
     *   type: 'cluster',
     *   id: 'name',
     *   state: 'STARTING|STOPPING'
     * }
     * 
     * If the inbound event has the wrong id or type, the event is ignored. This does not provide any sort of security,
     * but does guard against programming errors and will make the code more serviceable / robust.
     * 
     * The following observer methods may be called:
     * -- onStateChange(newState, oldState) - parameters are Strings
     * -- onScalingPolicyChange(newScalingPolicy, oldScalingPolicy) - parameters are Strings
     * -- onScalingPolicyEnabledChange(newScalingPolicyEnabled, oldScalingPolicyEnabled) - parameters are boolean
     * -- onServersTallyChange(newTally, oldTally) - parameters are objects: {up, down, unknown}
     * -- onServersListChange(newList, oldList, added, removed) - parameters are Array.<string>
     * -- onAppsTallyChange(newTally, oldTally) - parameters are objects: {up, down, unknown, partial}
     * -- onAppsListChange(newList, oldList, added, removed) - parameters are Array.<string>
     * -- onDestroyed() - no parameters
     * -- onAlertsChange(newAlerts, oldAlerts) - parameters are Strings
     * -- onTagsChange(newTags, oldTags) - parameters are {String[]}
     * -- onOwnerChange(newOwner, oldOwner) - parameters are {String}
     * -- onContactsChange(newContacts, oldContacts) - parameters are {String[]}
     * -- onNoteChange(newNote, oldNote) - parameters are {String}
     * 
     * @param {Object} e The received Cluster Event object
     * @param boolean (optional) doStop Flag to skip the return when the Cluster goes from STARTING to STOPPED and stays in the STOPPED state after 5 seconds
     */
    _handleChangeEvent: function(e, doStop) {
      console.log('Cluster '+this.id+' got an event!', e);
      
      var me = this;

      if (e.id !== this.id) { 
        console.error('Cluster '+this.id+' got an event which did not have the correct id. The event will be ignored. Received event id: ' + e.id);
        return;
      }
      if (e.type !== this.type) { 
        console.error('Cluster '+this.id+' got an event which did not have the correct type. The event will be ignored. Received event type: ' + e.type);
        return;
      }

      // If we've been removed, trigger the destroy
      if (e.state === 'removed') {
        this.isDestroyed = true;
        this._notifyObservers('onDestroyed');
        return;
      }

      // If cluster is going from starting/stopped state to stopped
      // We check for going from stopped to stopped because the cluster receives around 9 stopped events after starting
      if((this.oldState === "STARTING" || this.oldState === "STOPPED") && e.state === "STOPPED"){
          if(!doStop){
              // Wait 5 seconds and if the old state is still STOPPED then handle the event by calling this function again with a flag to bypass the return
              setTimeout(function(){
                  if(me.oldState === "STOPPED"){
                      me._handleChangeEvent(e, true);
                  }
              }, 5000);
              return; 
          }
      }
      
      this._updateAttribute('onStateChange', e, 'state');
      this._updateAttribute('onScalingPolicyChange', e, 'scalingPolicy');
      this._updateAttribute('onScalingPolicyEnabledChange', e, 'scalingPolicyEnabled');

      this._updateAttribute('onTagsChange', e, 'tags');
      this._updateAttribute('onOwnerChange', e, 'owner');
      this._updateAttribute('onContactsChange', e, 'contacts');
      this._updateAttribute('onNoteChange', e, 'note');
      
      this._updateAttribute('onAlertsChange', e, 'alerts');

      if (e.servers) {
        this._updateTally('onServersTallyChange', e.servers, this.servers, ['up', 'down', 'unknown']);
        this._updateArray('onServersListChange', this.servers.list, e.servers.added, e.servers.removed);
      }
      if (e.apps) {
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
                    this.apps.list.splice(i, 1);
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
                    if (changedApp.hasOwnProperty('up')) {
                      curApp.up = changedApp.up;  
                    }
                    if (changedApp.hasOwnProperty('down')) {
                      curApp.down = changedApp.down;  
                    }
                    if (changedApp.hasOwnProperty('unknown')) {
                      curApp.unknown = changedApp.unknown;  
                    }
                    if (changedApp.servers) {
                      curApp.servers = changedApp.servers;  
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
        this._updateTally('onAppsTallyChange', e.apps, this.apps, ['up', 'down', 'unknown', 'partial']);
      }
      
      this.oldState = e.state;
    },

    // TODO: BEGIN REMOVE THIS
    // This is a temporary work around while we refactor the code to rely on resourceManager directly.
    // For now, we'll reference an instance of ResourceManager that is injected post-construction by
    // the ResourceManager code.
    /**
     * @returns Deferred which resolves with ServersOnCluster.
     */
    getServers: function() {
      return this.resourceManager.getServersOnCluster(this);
    },

    /**
     * @returns Deferred which resolves with AppsOnCluster.
     */
    getApps: function() {
      return this.resourceManager.getAppsOnCluster(this);
    },
    // TODO: END REMOVE THIS

    /**
     * Starts the cluster.
     * 
     * @return {Promise} response - The result of the operation. If an error occurred, the result will contain not only the full err object,
     *         but also a errMsg property that can be used to display a prepared message regarding the result. Returns null if standalone
     *         server.
     */
    start: function() {
      return __clusterOperation(this, 'startCluster');
    },
    
    /**
     * Starts the cluster with the clean flag set.
     * 
     * @return {Promise} response - The result of the operation. If an error occurred, the result will contain not only the full err object,
     *         but also an 'errMsg' property that can be used to display a prepared message regarding the result. The returned object may
     *         also include a 'msg' property which would contain output from the server's stdout. Returns null if standalone server.
     */
    startClean: function() {
      return __clusterOperation(this, 'startCluster', '--clean');
    },

    /**
     * Stops the cluster.
     * 
     * @return {Promise} response - The result of the operation. If an error occurred, the result will contain not only the full err object,
     *         but also a errMsg property that can be used to display a prepared message regarding the result. Returns null if standalone
     *         server.
     */
    stop: function() {
      return __clusterOperation(this, 'stopCluster');
    },

    /**
     * Restarts the cluster. Courtesy functions that calls stop and then start.
     * 
     * @return {Promise} response - The result of the operation. If an error occurred, the result will contain not only the full err object,
     *         but also a errMsg property that can be used to display a prepared message regarding the result. If no error occurred, the
     *         result will contain a msg property which will have the cluster status. Returns null if standalone server.
     */
    restart: function() {
      return __restart(this);
    }

  });

  /**
   * See exposed method for JSDoc.
   */
  function __restart(cluster) {
    var deferred = new Deferred();

    // For now, a cluster restart is nothing more than a stop followed by start; we do not check whether the stop that was successful
    // for each server; we do however check the cumulative result and add it to the 'msg' property of the response object
    __clusterOperation(cluster, 'stopCluster').then(function(response) {
      if (response.errMsg) {
        // Stop failed, let someone know!
        response.errMsg = lang.replace(resourcesMessages.STOP_FAILED_DURING_RESTART, [ response.errMsg ]);
        deferred.resolve(response, true);
      } else {
        // Stop operation completed successfully so now do a start
        __clusterOperation(cluster, 'startCluster').then(function(response) {
          var status = null;
          var msg = null;
          if (!response.errMsg && response.value) {
            for ( var i in response.value) {
              var server = response.value[i];
              if (status !== null && status !== server.returnCode) {
                msg = lang.replace(resourcesMessages.CLUSTER_STATUS, [ cluster.name, 'PARTIALLY_STARTED' ]);
              } else {
                status = server.returnCode;
              }
            }
          }
          if (!msg) {
            if (status === '0' || status === '1') {
              msg = lang.replace(resourcesMessages.CLUSTER_STATUS, [ cluster.name, 'STARTED' ]);
            } else {
              msg = lang.replace(resourcesMessages.CLUSTER_STATUS, [ cluster.name, 'STOPPED' ]);
            }
          }
          deferred.resolve(msg, true);
        });
      }
    });

    return deferred;
  };

  /**
   * Send the operation state change notification events for the cluster and all of its servers.
   * We do not update tallies here, as the change detection will handle that.
   */
  function sendStateNotification(cluster, state, origin) {
    util.sendStateNotification(cluster, state, origin);
    cluster.getServers().then(function(serversOnCluster) {
      for (var i = 0; i < serversOnCluster.list.length; i++) {
        var server = serversOnCluster.list[i];
        util.sendStateNotification(server, state, origin);
      }
    });
  }

  function getClusterOperationErrorMessage(cluster, operation, err) {
    var errMsg;

    if (err && err.response && err.response.data && err.response.data.stackTrace) {
      var stackTrace = err.response.data.stackTrace;
      errMsg = apiMsgUtils.findErrorMsg(stackTrace);
      if (errMsg) {
        return errMsg;
      } else if (err.response.status === 400
          && err.response.data.stackTrace.search('javax.management.RuntimeMBeanException: java.lang.IllegalStateException: Cluster ') != -1) {
        return lang.replace(resourcesMessages.CLUSTER_UNAVAILABLE, [ cluster.name ]);
      }
      errMsg = apiMsgUtils.firstLineOfStackTrace(stackTrace);
      if (errMsg) {
        return errMsg;
      } else if (err.response.status) {
        return err.response.status + ' ' + lang.replace(resourcesMessages.ERROR_CLUSTER_OPERATION, [ operation, cluster.name ]);
      }
    }
    // Unable to determine what caused the operation to fail. Return errMsg regarding the failure
    return lang.replace(resourcesMessages.ERROR_CLUSTER_OPERATION, [ operation, cluster.name ]);
  }

  /**
   * Implementation for Cluster.start(), Cluster.stop(), and Cluster.restart().
   * 
   * @private
   * @param {String}
   *          cluster - The cluster resource to act upon
   * @param {String}
   *          operation - The mode, either 'startCluster' or 'stopCluster'
   * @return {Promise} response - The result of the operation. If an error occurred, the result will contain not only the full err object,
   *         but also a errMsg property that can be used to display a prepared message regarding the result. Returns null if standalone
   *         server.
   */
  function __clusterOperation(cluster, operation, optionalParams) {
    if (utils.isStandalone()) {
      console.error('Cluster operations are not supported in standalone mode');
      return util.returnValueAsDeferred(null);
    }
    
    optionalParams = ( optionalParams === null || optionalParams === undefined ) ? 'null' : '{"value":"' + optionalParams + '","type":"java.lang.String"}';

    // Fire off an 'acting' event
    var currentState = cluster.state;
    if (operation === 'stopCluster') {
      sendStateNotification(cluster, 'STOPPING', 'Cluster.js:133');
    } else if (operation === 'startCluster') {
      sendStateNotification(cluster, 'STARTING', 'Cluster.js:135');
    } else {
      console.error('__clusterOperation, operation could not be completed. Attempted to execute unknown operation: ' + operation);
    }

    var url = '/IBMJMXConnectorREST/mbeans/WebSphere:feature=collectiveController,type=ClusterManager,name=ClusterManager/operations/' + operation;
    var options = {
        handleAs : 'json',
        headers : {
          'Content-type' : 'application/json'
        },
        data : '{"params":[{"value":"' + cluster.name + '","type":"java.lang.String"},'
        + optionalParams + '],"signature":["java.lang.String","java.lang.String"]}'
    };

    var deferred = new Deferred();

    request.post(url, options).then(function(response) {
      // We used to send the final state as an event, but we do not do this anymore to ensure all affected groups are updated.
      // Do not add the state change notifications back!
      // TODO: need to handle the response and see if the operation was really successful...
      deferred.resolve(response, true);
    }, function(err) {
      console.log('Error ' + err.response.status + ' occurred when requesting ' + url + ': ', err);
      util.sendStateNotification(cluster, currentState, 'Cluster.js:417');

      var errMsg = getClusterOperationErrorMessage(cluster, operation, err);
      console.log(errMsg);
      err.errMsg = errMsg;

      deferred.resolve(err, true); // Right now we're always resolving with the response... we could reject though!
    });

    return deferred;
  }

});
