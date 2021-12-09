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
define([ 'dojo/_base/declare', '../../_util', '../../ObservableResource', '../../Observer', 'jsExplore/utils/ID' ],
    function(declare, util, ObservableResource, Observer, ID) {

  /**
   * AppInstancesByCluster collection represents the set of all AppOnServer objects which are grouped together
   * into a 'cluster application group' which is all of the application instances with the same name in a cluster.
   * It links back to both the Cluster and AppOnCluster object from which it was created. The AppInstancesByCluster
   * collection is also an Observer, as it watches its parent Cluster object and will update its tally or the list
   * of AppOnServer objects in response to the Cluster object changing.
   * 
   * The cumulative public attributes for AppInstancesByCluster are as follows:
   * @class
   * @typedef {Object} AppInstancesByCluster
   * @property {string} id                    - The resource's unique ID within the set of same type
   * @property {string} type                  - The resource's type
   * @property {string} viewType              - The resource's viewType, which indicates which view to use to display the resource 
   * @property {number} up                    - The number of application instances that are considered to be completely started
   * @property {number} down                  - The number of application instances that are considered to be stopped or otherwise unavailable
   * @property {number} unknown               - The number of application instances that are considered to be in an unknown state
   * @property {Array.<AppOnServer>} list     - The list of AppOnServer objects, based on the Cluster's members
   * @property {Cluster} cluster              - The Cluster which this collection was based on
   * @property {Cluster} parentResource       - The Cluster which this collection was based on
   * @property {AppOnCluster} application     - The AppOnCluster object which this collection was based on
   */
  return declare('AppInstancesByCluster', [ObservableResource, Observer], {
    /** Hard-code the type to be 'appInstancesByCluster' **/
    /** @type {string} */ id: ID.getAppInstancesByClusterUpper(), // TODO: clean this up too. Its required by type hierarchy
    /** @type {string} */ type: 'appInstancesByCluster',
    /** @type {string} */ viewType: 'Instances',

    /** Set by the constructor only. Acts as the link to the parent resource. **/
    /** @type {Cluster} */ cluster: null,
    /** @type {Cluster} */ parentResource: null,
    /** @type {AppOnCluster} */ application: null,

    /** Attributes required to be set by creator **/
    /** @type {number} */ up: 0,
    /** @type {number} */ down: 0,
    /** @type {number} */ unknown: 0,
    /** @type {Array.<AppOnServer>} */ list: [],

    /**
     * Perform all required initialization.
     */
    constructor: function(init) {
      if (!init) this._throwError('AppInstancesByCluster created without an initialization object');
      (init.cluster !== undefined) ? this.cluster = init.cluster : this._throwError('AppInstancesByCluster created without a Cluster');
      (init.application !== undefined) ? this.application = init.application : this._throwError('AppInstancesByCluster created without an AppOnCluster');

      var instances = [];
      (init.appOnServer) ? instances = init.appOnServer : this._throwError('AppInstancesByCluster created without an array of AppOnServer');

      this.id = ID.getResourceOnResource(this.type, ID.commaDelimit(this.cluster.id, this.application.id));
      this.parentResource = this.cluster;

      this.list = [];
      for (var i = 0; i < instances.length; i++) {
        var instance = instances[i];
        if (!instance) { continue; } // Tolerate non-homogeneous app deployments
        this.list.push(instance);

        increaseTally(this, instance.state);
      }
      this.cluster.subscribe(this);
      this.application.subscribe(this);
    },

    destroy: function() {
      this.cluster.unsubscribe(this);
      this.application.unsubscribe(this);
    },

    _handleChangeEvent: function() { /* noop - here purely because of the type hierarchy */ },

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
     * When a server is added to the cluster, it should have a copy of this Application.
     * Clusters should be, but are not guaranteed to be, homogeneous. For each added server
     * to the cluster, try to resolve the AppOnServer with the name of this collection of
     * instances and add it to the list.
     * 
     * @param newList
     * @param oldList
     * @param added
     * @param removed
     */
    onServersListChange: function(newList, oldList, added, removed) {
      // Process removals first
      if (added || removed) {
        // Store the previous values
        var prevTally = {
            up: this.up,
            down: this.down,
            unknown: this.unknown
        };

        var prevList = this.list.slice(0); // Make a shallow copy of the list

        if (removed) {
          for (var i = (this.list.length - 1); i >= 0 ; i--) {
            var curAppOnServer = this.list[i];
            // If it matches removal, splice it out
            for (var r = 0; r < removed.length; r++) {
              if (removed[r] === curAppOnServer.server.id) {
                this.list.splice(i, 1);
                curAppOnServer.unsubscribe(this);
                decreaseTally(this, curAppOnServer.state);
              }
            }
          }
        }

        // Finally append new additions
        if (added) {
          // Something was added, we need to resolve it then notify
          var me = this;
          this.resourceManager.getServer(added).then(function(serverList) {
            me.resourceManager.getAppOnServer(serverList, me.application.name).then(function(appOnServerList) {
              for (var i = 0; i < appOnServerList.length; i++) {
                var instance = appOnServerList[i];
                if (instance !== null) {
                  me.list.push(instance);
                  increaseTally(me, instance.state);
                } else { 
                	// a race condition when web socket is used as the server may not have all the apps yet.
                	console.log("something is wrong, destroy AppInstancesByCluster object");
                	me.__destroySelf(); // to recreate appInstancesByCluster again
                }
              }

              var newTally = {
                  up: me.up,
                  down: me.down,
                  unknown: me.unknown
              };
              me._notifyObservers('onTallyChange', [newTally, prevTally]);
              me._notifyObservers('onAppsListChange', [me.list, prevList, added, removed]);
            });
          });
        } else {
          // Store the new values
          var newTally = {
              up: this.up,
              down: this.down,
              unknown: this.unknown
          };
          this._notifyObservers('onTallyChange', [newTally, prevTally]);
          // Nothing added, notify now
          this._notifyObservers('onAppsListChange', [this.list, prevList, added, removed]);
        }
      }
    },
    
    onTallyChange: function(newTally, prevTally) {
      this._updateTally('onTallyChange', newTally, this, ['up', 'down', 'unknown']);
    }

  });

  /**
   * Increase the obj's up, down or unknown tally based on the input state.
   * These values are from com.ibm.ws.ui.collective.internal.rest.resource.State.
   * 
   * @param {Object} obj The object with the up, down and unknown attributes
   * @param {String} state The state to update the tally
   */
  function increaseTally(obj, state) {
    if (state === 'STARTED' || state === 'PARTIALLY_STARTED') {
      obj.up++;
    } else if (state === 'UNKNOWN') {
      obj.unknown++;
    } else {
      obj.down++;
    }
  }

  /**
   * Decrease the obj's up, down or unknown tally based on the input state.
   * These values are from com.ibm.ws.ui.collective.internal.rest.resource.State.
   * 
   * @param {Object} obj The object with the up, down and unknown attributes
   * @param {String} state The state to update the tally
   */
  function decreaseTally(obj, state) {
    if (state === 'STARTED' || state === 'PARTIALLY_STARTED') {
      obj.up--;
    } else if (state === 'UNKNOWN') {
      obj.unknown--;
    } else {
      obj.down--;
    }
  }
});
