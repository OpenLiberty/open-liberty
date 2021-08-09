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
define([ 'dojo/_base/declare', 'dojo/Deferred', 'dojo/promise/all', '../../_util', '../../ObservableResource', '../../Observer', 'jsExplore/utils/ID' ],
    function(declare, Deferred, all, util, Observable, Observer, ID) {

  /**
   * The AppOnCluster object represents an 'application group' running on a cluster. The term
   * 'application group' is an Admin Center specific concept, no formal group exists in the collective.
   * The 'application group' is both an object and a collection. It acts as an object, representing the
   * collection of application instances deployed to a set of servers in a cluster. The AppOnCluster
   * object contains a tally of all of the instances that are up, down and unknown.
   * 
   * The AppOnCluster object also contains the set of servers to which these application
   * instances are deployed. The set of servers contain the tally and list of IDs.
   * 
   * The cumulative public attributes for AppOnCluster are as follows:
   * @class
   * @typedef {Object} AppOnCluster
   * @property {string} id                    - The resource's unique ID within the set of same type
   * @property {string} type                  - The resource's type
   * @property {string} name                  - The resource's display name, defaults to the ID
   * @property {string} state                 - The resource's state [STARTED, PARTIALLY_STARTED, STOPPED or UNKNOWN]. The state represents the combined effective state of the application instances.
   * @property {number} up                    - The number of application instances that are running
   * @property {number} down                  - The number of application instances that are stopped or otherwise unavailable
   * @property {number} unknown               - The number of application instances that are in an unknown state
   * @property {string} scalingPolicy         - The name of the scalingPolicy for the cluster this 'application group' is for
   * @property {boolean} scalingPolicyEnabled - Flag to indicate whether or not the scalingPolicy for the cluster this 'application group' is for is enabled
   * @property {Object} servers               - The set of servers to which this application is deployed
   * @property {number} servers.up            - The number of servers which are running
   * @property {number} servers.down          - The number of servers which are stopped
   * @property {number} servers.unknown       - The number of servers which are in an unknown state
   * @property {Array.<string>} servers.list  - The list of server IDs to which this application is deployed. The list will have no duplicate values.
   * @property {function} start               - The start operation for the resource
   * @property {function} stop                - The stop operation for the resource
   * @property {function} restart             - The restart operation for the resource
   * @property {Cluster} cluster              - The Cluster to which the application is deployed
   * @property {Cluster} parentResource       - The Cluster to which the application is deployed
   * @property {string} note                  - The notes associated to the resource
   * @property {string} owner                 - The owner associated to the resource
   * @property {Array.<string>} contacts      - The list of contacts associated to the resource
   * @property {Array.<string>} tags          - The list of tags associated to the resource
   */
  return declare('AppOnCluster', [ObservableResource, Observer], {
    /** Hard-code the type to be 'appOnCluster' **/
    /** @type {string} */ id: ID.getAppOnClusterUpper(),//'AppOnCluster', // TODO: clean this up too. Its required by type hierarchy
    /** @type {string} */ type: 'appOnCluster',

    /** Set by the constructor only. Acts as the link to the parent resource. **/
    /** @type {Cluster} */ cluster: null,
    /** @type {Cluster} */ parentResource: null,

    /** @type {Object} */ alerts: null,

    /** Set by the constructor **/
    /** @type {number} */ up: 0,
    /** @type {number} */ down: 0,
    /** @type {number} */ unknown: 0,
    /** @type {string} */ scalingPolicy: null,
    /** @type {boolean} */ scalingPolicyEnabled: null,
    /** @type {Object} */ servers: null,

    /**
     * Perform all required initialization.
     */
    constructor: function(init) {
      if (!init) this._throwError('AppOnCluster created without an initialization object');
      (init.cluster) ? this.cluster = init.cluster : this._throwError('AppOnCluster created without a Cluster');
      (init.name) ? this.name = init.name : this._throwError('AppOnCluster created without a name');

      var cluster = this.cluster;
      this.id = ID.getResourceOnResource(this.name, cluster.id);
      this.parentResource = cluster;
      this.state = 'UNKNOWN'; // Default the state to be UNKNOWN in case we can't set it to a proper value

      // Find the app in the cluster's list of apps and set the values
      for(var i = 0; i < cluster.apps.list.length; i++) {
        var app = cluster.apps.list[i];
        if (app.name === this.name) {
          this.state = app.state;
          this.up = app.up;
          this.down = app.down;
          this.unknown = app.unknown;
          this.tags = app.tags;
          this.owner = app.owner;
          this.contacts = app.contacts;
          this.note = app.note;
          this.servers = app.servers;
          this.servers.list = app.servers.ids; //FIXME: server->client mapping: ids->list
        }
      }

      // Pull the attributes from the cluster object
      this.scalingPolicy = this.cluster.scalingPolicy;
      this.scalingPolicyEnabled = this.cluster.scalingPolicyEnabled;
      this.__updateAlerts(false);

      this.cluster.subscribe(this);
    },

    destroy: function() {
      this.cluster.unsubscribe(this);
    },

    /**
     * Sets the AppOnCluster alerts based on the computed tallies.
     * 
     * @param {boolean} notifyObservers Flag to indicate whether or not the notifyObservers method should be called.
     */
    __updateAlerts: function(notifyObservers) {
      var prevAlerts = this.alerts;
      this.alerts = { count: 0, unknown: [], app: [] };

      // Pull the app alerts from the parent cluster
      if (this.cluster.alerts) {
        if (this.cluster.alerts.unknown) {
          for (var i = 0; i < this.cluster.alerts.unknown.length; i++) {
            var unknownAlert = this.cluster.alerts.unknown[i];
            if (unknownAlert.id === this.id && unknownAlert.type === this.type) {
              this.alerts.unknown.push(unknownAlert);
              this.alerts.count++;
            }
          }
        }
        if (this.cluster.alerts.app) {
          for (var i = 0; i < this.cluster.alerts.app.length; i++) {
            var appAlert = this.cluster.alerts.app[i];
            // The name for an app alert is of the form
            //     serverTuple|cluster,appName.
            // Pull out the app name to compare to the name for this appOnCluster
            if (appAlert.name.substring(appAlert.name.lastIndexOf(',') + 1) === this.name) {
              this.alerts.app.push(appAlert);
              this.alerts.count++;
            }
          }
        }
      }

      if (notifyObservers) {
        this._notifyObservers('onAlertsChange', [this.alerts, prevAlerts]);
      }
    },

    /**
     * This event handler only handles specific State Events.
     * 
     * Operational State Event {
     *   type: 'appOnCluster',
     *   id: 'id',
     *   state: 'STARTING|STOPPING'
     * }
     * 
     * The State Event only indicates that the state of a resource has changed to a temporary transient state.
     * The transient states  are basically 'STARTING' and 'STOPPING'. Receiving any other state is ignored.
     * 
     * The following observer methods may be called:
     * -- onStateChange(newState, oldState) - parameters are Strings
     * 
     * @param {Object} e The received State Event object
     */
    _handleChangeEvent: function(e) {
      console.log('AppOnCluster '+this.id+' got an event!', e);

      if (e.id !== this.id) { 
        console.error('AppOnCluster '+this.id+' got an event which did not have the correct id. The event will be ignored. Received event id: ' + e.id);
        return;
      }
      if (e.type !== this.type) { 
        console.error('AppOnCluster '+this.id+' got an event which did not have the correct type. The event will be ignored. Received event type: ' + e.type);
        return;
      }

      if (e.state && (e.state === 'STARTING' || e.state === 'STOPPING' || e.state === 'RESTARTED')) {
        if (e.state === 'RESTARTED') {
          // Override the state to be 'STARTED' as that is the real final state for 'RESTARTED'
          e.state = 'STARTED';
        }
        this._updateAttribute('onStateChange', e, 'state');
      } else {
        console.error('AppOnCluster ' + this.id + ' received an Operational State Event with a state value which will be ignored: ' + e.state);
      }
    },
    
    /**
     * Common method to trigger the destruction of this resource and mark it removed from the collective.
     */
    __destroySelf: function() {
      this.isDestroyed = true;
      this._notifyObservers('onDestroyed');
      this.destroy();
    },

    /**
     * The Observer method to detect when the observed Cluster is removed from the collective.
     * If the Cluster was removed, we've also been removed, so destroy ourself.
     */
    onDestroyed: function() {
      this.__destroySelf();
    },

    /**
     * Observer for the Cluster. When the Cluster state changes to STOPPED, and only STOPPED, we need to update our tallies to be all down.
     * Any other value for the state is ignored.
     */
    onStateChange: function(newState, oldState) {
      if (newState !== 'STOPPED') {
        return; // Ignore this state change as it is not guarunteed to impact any other states
      }

      // Store the previous values
      var prevState = this.state;
      var prevTally = {
          up: this.up,
          down: this.down,
          unknown: this.unknown
      };

      // Set the state and all tallies to down
      this.state = "STOPPED";
      this.up = 0;
      this.down = this.servers.list.length;
      this.unknown = 0;

      // Store the new values
      var newTally = {
          up: this.up,
          down: this.down,
          unknown: this.unknown
      };

      this._notifyObservers('onTallyChange', [newTally, prevTally]);
      this._notifyObservers('onStateChange', [this.state, prevState]);
    },

    /**
     * The Observer method to detect when the observed Cluster changes its scaling policy.
     * If the Cluster changes its scaling policy, update our value.
     * 
     * @param {string} scalingPolicy The new value for the scaling policy of the Cluster
     */
    onScalingPolicyChange: function(scalingPolicy) {
      var prev = this.scalingPolicy;
      this.scalingPolicy = scalingPolicy;
      this._notifyObservers('onScalingPolicyChange', [this.scalingPolicy, prev]);
    },

    /**
     * The Observer method to detect when the observed Cluster changes its scaling policy enabled flag.
     * If the Cluster changes its scaling policy, update our value.
     * 
     * @param {string} scalingPolicyEnabled The new value for the scaling policy enabled flag of the Cluster
     */
    onScalingPolicyEnabledChange: function(scalingPolicyEnabled) {
      var prev = this.scalingPolicyEnabled;
      this.scalingPolicyEnabled = scalingPolicyEnabled;
      this._notifyObservers('onScalingPolicyEnabledChange', [this.scalingPolicyEnabled, prev]);
    },

    /**
     * The Observer method to detect when the observed Server has an application collection change.
     * If the application state for this application changed, it will be in the changed array.
     * If this application was removed, it will be in the removed array.
     * 
     * @param {Array.<Object>} newApps The final set of application names and state. This is ignored by this method because we only care about ourself.
     * @param {Array.<Object>} oldApps The previous set of application names and state. This is ignored by this method.
     * @param {Array.<Object>} added The added set of application names and state. This is ignored by this method.
     * @param {Array.<Object>} removed The remove set of application names. If our name is in this set, then this object should be destroyed.
     * @param {Array.<Object>} changed The changed set of application names and state. If our name is in this set, then update our state.
     */
    onAppsListChange: function(newApps, oldApps, added, removed, changed) {
      console.log("AppOnCluster got an event! " + this.name);

      if(added && this.state === 'UNKNOWN'){        
        // Check if we are an added app.  This can happen for appOnCluster due to the fact that appOnServer can create the appOnCluster object (since it
        // needs to subscribe to it for metadata) before the cluster object knows about the new appOnCluster.  As such, the init will not be able to populate 
        // all the fields that come from the cluster's app object (like servers).  
        // However, once the cluster learns of the new appOnCluster, we can then update the blank fields as done below.
        for(var i = 0; i < added.length; i++) {
          var app = added[i];
          if (app.name === this.name) {
            this.state = app.state;
            this.up = app.up;
            this.down = app.down;
            this.unknown = app.unknown;
            this.tags = app.tags;
            this.owner = app.owner;
            this.contacts = app.contacts;
            this.note = app.note;
            this.servers = app.servers;
            this.servers.list = app.servers.ids;
          }
        }
      }
      
      // Iterate through all of the changed apps. If we've changed, update our state
      if (changed) {
        for (var i = 0; i < changed.length; i++) {
          var appChanges = changed[i];
          if (appChanges.name === this.name) {
            this._updateAttribute('onStateChange', appChanges, 'state');
            this._updateTally('onTallyChange', appChanges, this, ['up', 'down', 'unknown']);
            this._updateAttribute('onNoteChange', appChanges, 'note');
            this._updateAttribute('onOwnerChange', appChanges, 'owner');
            this._updateAttribute('onContactsChange', appChanges, 'contacts');
            this._updateAttribute('onTagsChange', appChanges, 'tags');
            if (appChanges.servers) {
              this._updateTally('onServersTallyChange', appChanges.servers, this.servers, ['up', 'down', 'unknown']);

              // Compute added and removed
              var serversAdded = [];
              var serversRemoved = [];
              
              var findObjectInList = function(list, ele) {
                for (var i = 0; i < list.length; i++) {
                  // Not === because we want to match String primitives with String objects too
                  if (list[i] == ele) {
                    return list[i];
                  }
                }
              };

              // First, find things that were 'added'
              for (var j = 0; j < appChanges.servers.ids.length; j++) {
                var ele = appChanges.servers.ids[j];
                if (!findObjectInList(this.servers.ids, ele)) {
                  serversAdded.push(ele);
                }
              }

              // Second, find things that were 'removed'
              for (var k = 0; k < this.servers.ids.length; k++) {
                var ele = this.servers.ids[k];
                if (!findObjectInList(appChanges.servers.ids, ele)) {
                  serversRemoved.push(ele);
                }
              }

              this.servers.list = appChanges.servers.ids; // Bind new ids to list for consistency
              this._updateArray('onServersListChange', this.servers.ids, serversAdded, serversRemoved);
            }
          }
        }
      }
      // Iterate through all of the removed apps. If we've been removed, then destroy ourself
      if (removed) {
        for (var i = 0; i < removed.length; i++) {
          if (removed[i] === this.name) {
            this.__destroySelf();
          }
        }        
      }
    },

    /**
     * Observer method for Cluster's alert list. Need to select the relevant alerts from the cluster's alerts
     * for the AppOnCluster's alerts.
     * 
     * @param newAlerts Ignored
     * @param oldAlerts Ignored
     */
    onAlertsChange: function(newAlert, oldAlert) {
      this.__updateAlerts(true);
    },

    // TODO: BEGIN REMOVE THIS
    // This is a temporary work around while we refactor the code to rely on resourceManager directly.
    // For now, we'll reference an instance of ResourceManager that is injected post-construction by
    // the ResourceManager code.
    /**
     * @returns Deferred which resolves with a list of Cluster objects.
     */
    getClusters: function() {
      // this.clusters is the real Cluster object from which this was derived
      return util.returnValueAsDeferred([this.cluster]);
    },

    getCluster: function() {
      // this.cluster is the real Cluster object from which this was derived
      return util.returnValueAsDeferred(this.cluster);
    },

    /**
     * @returns Deferred which resolves with a list of Server objects.
     */
    getServers: function() {
      return this.resourceManager.getServer(this.cluster.servers.list);
    },

    /**
     * @returns Deferred which resolves with an AppInstancesByCluster objects.
     */
    getInstances: function() {
      return this.resourceManager.getAppInstancesByCluster(this.cluster, this.name);
    },
    // TODO: END REMOVE THIS

    /**
     * Starts the applications on the cluster.
     * 
     * @return {Promise} - The callback is passed the operation result.
     */
    start: function() {
      return processApplicationActionOnServers(this, 'start');
    },

    /**
     * Stops the applications on the cluster.
     * 
     * @return {Promise} - The callback is passed the operation result.
     */
    stop: function() {
      return processApplicationActionOnServers(this, 'stop');
    },

    /**
     * Restarts the applications on the cluster.
     * 
     * @return {Promise} - The callback is passed the operation result.
     */
    restart: function() {
      return processApplicationActionOnServers(this, 'restart');
    }

  });

  function processApplicationActionOnServers(appOnClusterObj, action) {
    var state = 'UNKNOWN';
    if (action === 'start') {
      state = 'STARTING';
    } else if (action === 'stop') {
      state = 'STOPPING';
    } else if (action === 'restart') {
      state = 'STARTING';
    }
    util.sendStateNotification(appOnClusterObj, state, 'AppOnCluster.js:418');
    
    // debugging for AppOnCluster stopping problem
    var comment = document.createComment(" In processApplicationActionOnServer with action " + action + " - " + Date.now());
	document.head.appendChild(comment);
	comment = document.createComment(" appOnClusterObj: " + appOnClusterObj);
	document.head.appendChild(comment);
	// end debugging codes
	
    var deferred = new Deferred();
    
    appOnClusterObj.getInstances().then(function(instances) {
      var allAppOnServerPromises = [];
      for (var i = 0; i < instances.list.length; i++) {
        allAppOnServerPromises.push(util.appOperation(instances.list[i], action));
      };

      all(allAppOnServerPromises).then(function(allObjs) {
        if (action === 'restart') {
          util.sendStateNotification(appOnClusterObj, 'RESTARTED', 'AppOnCluster.js:430');
        }
        deferred.resolve(allObjs, true);
      }, function(err) {
    	// debugging for AppOnCluster stopping problem
        var comment = document.createComment(" In processApplicationActionOnServer - Failed to perform " + action + " on app");
      	document.head.appendChild(comment);
      	// end debugging codes
        deferred.reject('Failed to ' + action + ' an app: ' + JSON.stringify(err), true);
      });
    }, function(err) {
    	// debugging for AppOnCluster stopping problem
        var comment = document.createComment(" In processApplicationActionOnServer - Failed to get appOnCluster instance");
    	document.head.appendChild(comment);
    	// end debugging codes
    	deferred.reject('Failed to get appOnCluster instance to perform ' + action + 'on app: ' + JSON.stringify(err), true);
    });
    
    return deferred;
  };

});
