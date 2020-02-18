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
define([ 'dojo/_base/declare', '../../_util', '../../ObservableResource', '../../Observer', 
         'jsExplore/utils/constants', 'dojo/i18n!jsExplore/nls/explorerMessages', './_alerts', 'jsExplore/utils/ID' ],
    function(declare, util, ObservableResource, Observer, constants, i18n, alerts, ID) {

  /**
   * The ServerObserver is an internal, anonymous class which is used to observe the Server objects
   * which are part of the Runtime.
   */
  var ServerObserver = declare([Observer], {
    /** @type {string} */ id: ID.getServerObserverUpper(), // TODO: clean this up too. Its required by type hierarchy
    /** @type {Runtime} */ runtime: null,
    /** @type {Server} */ server: null,

    /**
     * Params: [ Runtime, Server ]
     */
    constructor: function(params) {
      this.runtime = params[0];
      this.server = params[1];
      this.id = ID.getResourceOnResource(this.runtime.id, this.server.id); 

      this.server.subscribe(this);
    },

    destroy: function() {
      this.server.unsubscribe(this);
    },

    /**
     * Observer for the Server. When the Server's state changes we need to update our tallies
     */
    onStateChange: function(newState, oldState) {
      if (newState === 'STARTING' || newState === 'STOPPING') {
        //map to stopped
        newState = 'STOPPED';
      }
      if (oldState === 'STARTING' || oldState === 'STOPPING') {
        //map to stopped
        oldState = 'STOPPED';
      }

      // Store the previous values
      var prevTally = {
          up: this.runtime.servers.up,
          down: this.runtime.servers.down,
          unknown: this.runtime.servers.unknown
      };

      // First, decrement the tallies based on the previous state
      if (oldState === 'STARTED') {
        this.runtime.servers.up--;
      } else if (oldState === 'STOPPED') {
        this.runtime.servers.down--;
      } else {
        this.runtime.servers.unknown--;
      }

      // Second, increment the tallies based on the current state
      if (newState === 'STARTED') {
        this.runtime.servers.up++;
      } else if (newState === 'STOPPED') {
        this.runtime.servers.down++;
      } else {
        this.runtime.servers.unknown++;
      }

      // Store the new values
      var newTally = {
          up: this.runtime.servers.up,
          down: this.runtime.servers.down,
          unknown: this.runtime.servers.unknown
      };
      this.runtime._notifyObservers('onServersTallyChange', [newTally, prevTally]);
      this.runtime.__updateState(true); // Update the tallies first, then the state and finally the list
    },

    /**
     * Observer for the Server. When the Server's alerts change we need to update our alerts.
     */
    onAlertsChange: function(alerts) {
      updateAlerts(this.runtime, true);
    }
  });

  /**
   * The Runtime object represents a Liberty runtime which is installed on a host. A runtime has no state in the collective,
   * and therefore no operations. The runtime may or may not have servers.
   * 
   * The Runtime observes both the Host which it was created from as well as the set of Servers which is monitors:
   * - The Host is monitored for server removals or additions. The Host server tally can not be used as those numbers
   *   may reflect the tally across multiple runtimes.
   * - The Host is monitored to see if the Runtime was removed from the Host, or if the Host itself was removed from
   *   the collective.
   * - The Server is monitored to see if its state changes. If the state changes, the tallies need to be updated.
   * - The Server is monitored for alerts. A Runtime's alerts are all of the alerts for all of its servers.
   * 
   * The cumulative public attributes for Runtime are as follows:
   * @class
   * @typedef {Object} Runtime
   * @property {string} id                    - The resource's unique ID within the set of same type
   * @property {string} type                  - The resource's type
   * @property {string} name                  - The resource's display name
   * @property {string} runtimeType           - The type: 'Liberty', 'Node.js' (defined in constants.js)
   * @property {string} containerType         - If the runtime executes within a container (ie. Docker) (defined in constants.js)
   * @property {string} state                 - The resource's FAKED state [STARTED, PARTIALLY_STARTED, STOPPED]. The state is FAKED because it represents the combined effective state of the servers, and not the runtime itself.
   * @property {string} path                  - The path of the Liberty installation directory
   * @property {Object} servers               - The set of servers which are present of this host
   * @property {number} servers.up            - The number of servers which are running
   * @property {number} servers.down          - The number of servers which are stopped
   * @property {number} servers.unknown       - The number of servers which are in an unknown state
   * @property {Array.<Server>} servers.list  - The list of Server objects which belong to this runtime. The list will have no duplicate values.
   * @property {Alerts} alerts                - The set of alerts that apply to the resource
   * @property {Host} host                    - The host on which this Runtime is installed
   * @property {Host} parentResource          - The host on which this Runtime is installed
   * @property {Array.<string>} tags          - The list of tags associated to the resource
   * @property {string} owner                 - The owner associated to the resource
   * @property {Array.<string>} contacts      - The list of contacts associated to the resource
   * @property {string} note                  - The notes associated to the resource
   */
  return declare('Runtime', [ObservableResource, Observer], {
    /** Hard-code the type to be 'runtime' **/
    /** @type {string} */ id: ID.getRuntimeUpper(), // TODO: clean this up too. Its required by type hierarchy
    /** @type {string} */ type: 'runtime',
    /** @type {string} */ name: null,
    /** @type {string} */ runtimeType: null,  
    /** @type {string} */ containerType: null,

    /** Set by the constructor only. Acts as the link to the parent resource. **/
    /** @type {Host} */ host: null,
    /** @type {Host} */ parentResource: null,
    /** @type {string} */ path: null,

    /** Set during construction and onAttributeChange events **/
    /** @type {string} */ note: null,
    /** @type {string} */ owner: null,
    /** @type {Array.<string>} */ contacts: null,
    /** @type {Array.<string>} */ tags: null,
    /** @type {Object} */ servers: null,
    /** @type {Object} */ alerts: null,
    /** @type {Array.<Object>} */ __serverObservers: null,

    /**
     * Perform all required initialization.
     */
    constructor: function(init) {
      if (!init) this._throwError('Runtime created without an initialization object');
      (init.host) ? this.host = init.host : this._throwError('Runtime created without a host');
      (init.path) ? this.path = init.path : this._throwError('Runtime created without a path');
      
      var seversToSelectFrom = [];
      (init.servers) ? seversToSelectFrom = init.servers : this._throwError('Runtime created without an array of Server');

      this.id = ID.commaDelimit(this.host.id, this.path);
      this.parentResource = this.host;
      this.servers = { up: 0, down: 0, unknown: 0, list: []};
      this.__serverObservers = [];

      // Need to load all of the servers for a host, and then filter out the ones with this runtime
      for (var i = 0; i < seversToSelectFrom.length; i++) {
        var server = seversToSelectFrom[i];
        if (server.wlpInstallDir === this.path) {
          this.servers.list.push(server);

          // Subscribe to the server so we can monitor its state and alerts
          this.__serverObservers.push(new ServerObserver([this, server]));

          if (server.state === 'STARTED') {
            this.servers.up++;
          } else if (server.state === 'STOPPED') {
            this.servers.down++;
          } else {
            this.servers.unknown++;
          }
        }
      }

      if (init.runtimeType) {
        this.runtimeType = init.runtimeType;
        switch (init.runtimeType) {
          case constants.RUNTIME_LIBERTY: 
            this.name = i18n.RUNTIME_LIBERTY;
            break;
          
          case constants.RUNTIME_NODEJS:
            this.name = i18n.RUNTIME_NODEJS;
            break;
            
          default:
            this.name = i18n.RUNTIME;
            console.error('Runtime init with unknown runtimeType: ' + init.runtimeType);
        }
      } else {  // Default to Liberty if not set
        this.runtimeType = constants.RUNTIME_LIBERTY;
        this.name = i18n.RUNTIME_LIBERTY;
      }
      
      if (init.containerType) { this.containerType = init.containerType; }  // Set if present
      
      if (init.metadata) {
        if (Array.isArray(init.metadata.tags)) { this.tags = init.metadata.tags; }
        if (init.metadata.owner) { this.owner = init.metadata.owner; }
        if (Array.isArray(init.metadata.contacts)) { this.contacts = init.metadata.contacts; }
        if (init.metadata.note) { this.note = init.metadata.note; }       
      }

      updateAlerts(this); // Set'em if you got'em

      // Fake the Runtime 'state'
      this.__updateState(false);

      this.host.subscribe(this);
    },

    /**
     * When the Runtime is destroyed, it will unsubscribe from the Host, and destroy all
     * of the ServerObserver objects it has created.
     */
    destroy: function() {
      this.host.unsubscribe(this);
      for (var i = 0; i < this.__serverObservers.length; i++) {
        this.__serverObservers[i].destroy();
      }
    },

    /**
     * Sets the FAKE Runtime state based on the server tallies.
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

    _handleChangeEvent: function(e) { 
      console.log('Runtime '+this.id+' got an event!', e);

      if (e.id !== this.id) { 
        console.error('Runtime '+this.id+' got an event which did not have the correct id. The event will be ignored. Received event id: ' + e.id);
        return;
      }
      if (e.type !== this.type) { 
        console.error('Runtime '+this.id+' got an event which did not have the correct type. The event will be ignored. Received event type: ' + e.type);
        return;
      }

      this._updateAttribute('onTagsChange', e, 'tags');
      this._updateAttribute('onOwnerChange', e, 'owner');
      this._updateAttribute('onContactsChange', e, 'contacts');
      this._updateAttribute('onNoteChange', e, 'note');

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
     * The Observer method to detect when the observed Host is removed from the collective.
     * If the Host was removed, we've also been removed, so destroy ourself.
     */
    onDestroyed: function() {
      this.__destroySelf();
    },

    /**
     * Observer for the host. If this runtime was removed from it we need to destroy ourselves.
     */
    onRuntimesChange: function(newList, oldList, added, removed) {
      for (var i = 0; i < removed.length; i++) {
        if (removed[i].id === this.id) {
          // If we've been removed, trigger the destroy
          this.__destroySelf();
        }
      }
    },
    
    onRuntimesMetadataChange: function(metadataChanges) {
      if (this.id === metadataChanges.id) {
        this._updateAttribute('onTagsChange', metadataChanges, 'tags');
        this._updateAttribute('onOwnerChange', metadataChanges, 'owner');
        this._updateAttribute('onContactsChange', metadataChanges, 'contacts');
        this._updateAttribute('onNoteChange', metadataChanges, 'note');
      }
    },

    /**
     * Observer for the host. When there are servers added or removed, we need to process our list
     */
    onServersListChange: function(newList, oldList, added, removed) {
      var changed = false;
      if (added || removed) {
        var prevList = this.servers.list.slice(0); // Make a shallow copy of the list
        // Store the previous values
        var prevTally = {
            up: this.servers.up,
            down: this.servers.down,
            unknown: this.servers.unknown
        };

        if (removed) {
          for (var i = (this.servers.list.length - 1); i >= 0; i--) {
            var curServer = this.servers.list[i];
            // If it matches removal, splice it out
            for (var r = 0; r < removed.length; r++) {
              if (removed[r] === curServer.id)  {
                if (curServer.state === 'STARTED') {
                  this.servers.up--;
                } else if (curServer.state === 'STOPPED') {
                  this.servers.down--;
                } else {
                  this.servers.unknown--;
                }

                this.servers.list.splice(i, 1);
                // Now that the server is removed, remove it from the list of __serverObservers
                for (var j = (this.__serverObservers.length - 1); j >= 0; j--) {
                  if (removed[r] === this.__serverObservers[j].server.id) {
                    this.__serverObservers.splice(j, 1);
                  }
                }
                changed = true;
              }
            }
          }
        }

        // Finally append new additions
        if (added) {
          // Something was added, we need to resolve it then notify
          var me = this;
          this.resourceManager.getServer(added).then(function(servers) {
            for (var i = 0; i < servers.length; i++) {
              var server = servers[i];
              if (server.wlpInstallDir === me.path) {
                changed = true;
                me.servers.list.push(server);
                // Add the Server observer
                me.__serverObservers.push(new ServerObserver([me, server]));
                if (server.state === 'STARTED') {
                  me.servers.up++;
                } else if (server.state === 'STOPPED') {
                  me.servers.down++;
                } else {
                  me.servers.unknown++;
                }
              }
            }

            if (changed) {
              // Store the new values
              var newTally = {
                  up: me.servers.up,
                  down: me.servers.down,
                  unknown: me.servers.unknown
              };
              me._notifyObservers('onServersTallyChange', [newTally, prevTally]);
              me.__updateState(true); // Update the tallies first, then the state and finally the list
              me._notifyObservers('onServersListChange', [me.servers.list, prevList, added, removed]);
              updateAlerts(me, true);
            }
          });
        } else {
          if (changed) {
            // Store the new values
            var newTally = {
                up: this.servers.up,
                down: this.servers.down,
                unknown: this.servers.unknown
            };
            this._notifyObservers('onServersTallyChange', [newTally, prevTally]);
            this.__updateState(true); // Update the tallies first, then the state and finally the list
            this._notifyObservers('onServersListChange', [this.servers.list, prevList, added, removed]);
            updateAlerts(this, true);
          }
        }
      }
    },

    /**
     * @returns Deferred which resolves with ServersOnRuntime.
     */
    getServers: function() {
      return this.resourceManager.getServersOnRuntime(this);
    }

  });

  /**
   * Sets the alerts object for the runtime, based on the alerts of the servers
   * 
   * @param {Runtime} The Runtime object to process
   * @param {boolean} Indicates whether or not the Observers should be notified of the onAlertsChange
   */
  function updateAlerts(runtime, shouldNotify) {
    // Store previous copy
    prevAlerts = runtime.alerts;

    // Clear the alerts and rebuild
    alerts.combineAlerts(runtime);

    if (shouldNotify) {
      runtime._notifyObservers('onAlertsChange', [runtime.alerts, prevAlerts]);
    }
  }

});
