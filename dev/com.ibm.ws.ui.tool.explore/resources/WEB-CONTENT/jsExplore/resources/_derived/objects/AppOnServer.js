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
define([ 'dojo/_base/declare', '../../_util', '../../ObservableResource', '../../Observer', 'jsExplore/utils/ID'],
    function(declare, util, ObservableResource, Observer, ID) {

  /**
   * The AppOnServer object represents an 'application instance' running on a server in the collective. It links
   * back to the server to which it is deployed, and exposes operations. The AppOnServer is an Observer, as
   * it watches its parent Server object and will update its own state, cluster and scaling policy in response
   * to the server object changing.
   * 
   * The cumulative public attributes for AppOnServer are as follows:
   * @class
   * @typedef {Object} AppOnServer
   * @property {string} id                    - The resource's unique ID within the set of same type
   * @property {string} type                  - The resource's type
   * @property {string} name                  - The resource's display name, defaults to the ID
   * @property {string} state                 - The resource's state [INSTALLED, STARTED, STARTING, STOPPED, STOPPING, PARTIALLY_STARTED or UNKNOWN] (inherited and set by StatefulResource). These values are from com.ibm.ws.ui.collective.internal.rest.resource.State.
   * @property {string} cluster               - The cluster to which the server belongs. If null then the server does not belong to a cluster.
   * @property {string} scalingPolicy         - The scaling policy name which is applied to the server. It comes from the cluster. If null then no policy is applied.
   * @property {boolean} scalingPolicyEnabled - Indicates whether or not the scaling policy which is applied to the server is enabled. It comes from the cluster. If null then no policy is applied.
   * @property {function} start               - The start operation for the resource
   * @property {function} stop                - The stop operation for the resource
   * @property {function} restart             - The restart operation for the resource
   * @property {(Server|StandaloneServer)} server - The server to which the application is deployed. May be in a collective member server or a stand alone server.
   * @property {(Server|StandaloneServer)} parentResource - The server to which the application is deployed. May be in a collective member server or a stand alone server.
   * @property {Array.<string>} tags          - The list of tags associated to the resource
   * @property {string} owner                 - The owner associated to the resource
   * @property {string} maintenanceMode       - The state of the maintenance mode of the resource, could be one of these: inMaintenanceMode/notInMaintenanceMode/alternateServerStarting/alternateServerUnavailable
   * @property {Array.<string>} contacts      - The list of contacts associated to the resource
   * @property {string} note                  - The notes associated to the resource

   */
  return declare('AppOnServer', [ObservableResource, Observer], {
    /** Hard-code the type to be 'appOnServer' **/
    /** @type {string} */ id: ID.tbd(), // TODO: clean this up too. Its required by type hierarchy
    /** @type {string} */ type: 'appOnServer',

    /** Set by the constructor only. Acts as the link to the parent resource. **/
    /** @type {(Server|StandaloneServer)} */ server: null,
    /** @type {(Server|StandaloneServer)} */ parentResource: null,

    /** Set during construction and onAttributeChange events **/
    /** @type {string} */ cluster: null,
    /** @type {string} */ scalingPolicy: null,
    /** @type {string} */ maintenanceMode: null,
    /** @type {boolean} */ scalingPolicyEnabled: false,
    /** @type {Object} */ alerts: null,

    /**
     * Perform all required initialization.
     * 
     * @param {Object} init {
     */
    constructor: function(init) {
      if (!init) this._throwError('AppOnServer created without an initialization object');
      (init.server) ? this.server = init.server : this._throwError('AppOnServer created without a server');
      (init.name) ? this.name = init.name : this._throwError('AppOnServer created without a name');

      var server = this.server;
      this.id = ID.getResourceOnResource(this.name, server.id); // TODO: Consider changing the id to be tuple(appName)
      this.parentResource = server;
      this.state = 'UNKNOWN'; // Default the state to be UNKNOWN in case we can't set it to a proper value

      // Find the app in the server's list of apps and set the state
      for(var i = 0; i < server.apps.list.length; i++) {
        var app = server.apps.list[i];
        if (app.name === this.name) {
          this.state = app.state;
          this.tags = app.tags;
          this.owner = app.owner;
          this.contacts = app.contacts;
          this.note = app.note;
          break;
        }
      }

      // Pull the attributes from the server object
      if (server.cluster) {     // Set if present
        this.cluster = server.cluster; 
        
        // If this app instance is for a server that belongs to a cluster, then the metadata is
        // stored on the appOnCluster resource.  This appOnSever gets the same metadata values.
        var me = this;
        server.resourceManager.getAppOnCluster(this.cluster, this.name).then(function(appOnCluster){
          // appOnCluster resource  
          // Update this app instance's metadata to reflect that of the appOnCluster.
          // Notify subscribers of this update..
          me.onTagsChange(appOnCluster.tags);
          me.onOwnerChange(appOnCluster.owner);
          me.onContactsChange(appOnCluster.contacts);
          me.onNoteChange(appOnCluster.note);

          // Subscribe this app instance to changes made to the appOnCluster.
          appOnCluster.subscribe(me);
       });
      } 
      
      if (server.maintenanceMode) { this.maintenanceMode = server.maintenanceMode; } // Set if present
      if (server.scalingPolicy) { this.scalingPolicy = server.scalingPolicy; } // Set if present
      if (server.scalingPolicyEnabled) { this.scalingPolicyEnabled = server.scalingPolicyEnabled; } // Set if present
      
      this.alerts= {};
      this.alerts.count = 0;
      this.alerts.unknown = [];
      this.alerts.app = [];
      this.__updateAlerts(false);
      server.subscribe(this);
      
    },

    destroy: function() {
      this.server.unsubscribe(this);
    },

    /**
     * This event handler only handles specific State Events.
     * 
     * Operational State Event {
     *   type: 'appOnServer',
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
      console.log('AppOnServer '+this.id+' got an event!', e);

      if (e.id !== this.id) { 
        console.error('AppOnServer '+this.id+' got an event which did not have the correct id. The event will be ignored. Received event id: ' + e.id);
        return;
      }
      if (e.type !== this.type) { 
        console.error('AppOnServer '+this.id+' got an event which did not have the correct type. The event will be ignored. Received event type: ' + e.type);
        return;
      }

      if (e.state && (e.state === 'STARTING' || e.state === 'STOPPING' || e.state === 'RESTARTED')) {
        if (e.state === 'RESTARTED') {
          // Override the state to be 'STARTED' as that is the real final state for 'RESTARTED'
          e.state = 'STARTED';
        }
        this._updateAttribute('onStateChange', e, 'state');
      } else {
        console.error('AppOnServer ' + this.id + ' received an Operational State Event with a state value which will be ignored: ' + e.state);
      }
    },
    
    /**
     * Sets the AppOnServer alerts based on the computed tallies.
     * 
     * @param {boolean} notifyObservers Flag to indicate whether or not the notifyObservers method should be called.
     */
    __updateAlerts: function(notifyObservers) {
      var prevAlerts = this.alerts;
      this.alerts = { count: 0, unknown: [], app: [] };

      // Pull the app alerts from the parent server
      if (this.server.alerts) {
        if (this.server.alerts.unknown) {
          for (var i = 0; i < this.server.alerts.unknown.length; i++) {
            var unknownAlert = this.server.alerts.unknown[i];
            if (unknownAlert.id === this.id && unknownAlert.type === this.type) {
              this.alerts.unknown.push(unknownAlert);
              this.alerts.count++;
            }
          }
        }
        if (this.server.alerts.app) {
          for (var i = 0; i < this.server.alerts.app.length; i++) {
            var appAlert = this.server.alerts.app[i];
            // The name for an app alert is of the form
            //     serverTuple|cluster,appName.
            // Pull out the app name to compare to the name for this appOnServer
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
     * Observer method to detect when the observed Server changes its maintenance mode.
     * If the server changes its maintenance mode, update our value.
     * @param {string} maintenanceMode The new value for the maintenance mode of the Server
     */
    onMaintenanceModeChange: function(maintenanceMode) {
      var prev = this.maintenanceMode;
      this.maintenanceMode = maintenanceMode;
      this._notifyObservers('onMaintenanceModeChange', [this.maintenanceMode, prev]);
    },
    /**
     * Observer for the Server. When the Server state changes to STOPPED, and only STOPPED, we need to update our tallies to be all down.
     * Any other value for the state is ignored.
     */
    onAlertsChange: function(newAlert, oldAlert) {
      this.__updateAlerts(true);
    },

    /**
     * The Observer method to detect when the observed Server changes its cluster.
     * If the Server changes its cluster, update our value.
     * 
     * @param {string} cluster The new value for the cluster of the Server
     */
    onClusterChange: function(cluster) {
      var prev = this.cluster;
      this.cluster = cluster;
      this._notifyObservers('onClusterChange', [this.cluster, prev]);
    },

    /**
     * The Observer method to detect when the observed Server changes its scaling policy.
     * If the Server changes its scaling policy, update our value.
     * 
     * @param {string} scalingPolicy The new value for the scaling policy of the Server
     */
    onScalingPolicyChange: function(scalingPolicy) {
      var prev = this.scalingPolicy;
      this.scalingPolicy = scalingPolicy;
      this._notifyObservers('onScalingPolicyChange', [this.scalingPolicy, prev]);
    },
    
    /**
     * The Observer method to detect when the observed Server changes its scaling policy enabled flag.
     * If the Server changes its scaling policy, update our value.
     * 
     * @param {boolean} scalingPolicyEnabled The new value for the scaling policy enabled flag of the Server
     */
    onScalingPolicyEnabledChange: function(scalingPolicyEnabled) {
      var prev = this.scalingPolicyEnabled;
      this.scalingPolicyEnabled = scalingPolicyEnabled;
      this._notifyObservers('onScalingPolicyEnabledChange', [this.scalingPolicyEnabled, prev]);
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
     * The Observer method to detect when the observed Server is removed from the collective.
     * If the Server was removed, we've also been removed, so destroy ourself.
     */
    onDestroyed: function() {
      this.__destroySelf();
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
      console.log("AppOnServer got an event! " + this.name);
      // Iterate through all of the changed apps. If we've changed, update our state
      if (changed) {
        for (var i = 0; i < changed.length; i++) {
          if (changed[i].name === this.name) {
            var prev = this.state;
            this.state = changed[i].state;
            this._notifyObservers('onStateChange', [this.state, prev]);
            this._updateAttribute('onTagsChange', changed[i], 'tags');
            this._updateAttribute('onOwnerChange', changed[i], 'owner');
            this._updateAttribute('onContactsChange', changed[i], 'contacts');
            this._updateAttribute('onNoteChange', changed[i], 'note');
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
     * @returns Deferred which resolves with a Server objects.
     */
    getServer: function() {
      // this.server is the real Server object from which this was derived
      return util.returnValueAsDeferred(this.server);
    },

    /**
     * The Observer method to identify changes in the tags.
     * This appOnServer is notified of tag changes from the appOnCluster associated with the cluster
     * that this appOnServer's server belongs.   Tags must be kept consistent between the appOnCluster
     * and all its associated appOnServer objects.
     *
     * @param newTags
     * @param oldTags
     */
    onTagsChange: function(newTags, oldTags){     
      this.tags = newTags;
      this._notifyObservers("onTagsChange", [newTags, oldTags]);
    },

    /**
     * The Observer method to identify changes in the owner.
     * This appOnServer is notified of owner changes from the appOnCluster associated with the cluster
     * that this appOnServer's server belongs.   The owner must be kept consistent between the appOnCluster
     * and all its associated appOnServer objects.
     *
     * @param newOwner
     * @param oldOwner
     */
    onOwnerChange: function(newOwner, oldOwner){     
      this.owner = newOwner;
      this._notifyObservers("onOwnerChange", [newOwner, oldOwner]);
    },
    
    /**
     * The Observer method to identify changes in the contacts.
     * This appOnServer is notified of contact changes from the appOnCluster associated with the cluster
     * that this appOnServer's server belongs.   Contacts must be kept consistent between the appOnCluster
     * and all its associated appOnServer objects.
     *
     * @param newContacts
     * @param oldContacts
     */
    onContactsChange: function(newContacts, oldContacts){     
      this.contacts = newContacts;
      this._notifyObservers("onContactsChange", [newContacts, oldContacts]);
    },
    
    /**
     * The Observer method to identify changes in the note.
     * This appOnServer is notified of note changes from the appOnCluster associated with the cluster
     * that this appOnServer's server belongs.   The note must be kept consistent between the appOnCluster
     * and all its associated appOnServer objects.
     *
     * @param newNote
     * @param oldNote
     */
    onNoteChange: function(newNote, oldNote){     
      this.note = newNote;
      this._notifyObservers("onNoteChange", [newNote, oldNote]);
    },
    
    /**
     * Starts the application on the server.
     * 
     * @return {Promise} - The callback is passed the operation result.
     */
    start: function() {
      return util.appOperation(this, 'start');
    },

    /**
     * Stops the application on the server.
     * 
     * @return {Promise} - The callback is passed the operation result.
     */
    stop: function() {
      return util.appOperation(this, 'stop');
    },

    /**
     * Restarts the application on the server.
     * 
     * @return {Promise} - The callback is passed the operation result.
     */
    restart: function() {
      return util.appOperation(this, 'restart');
    }

  });

});

