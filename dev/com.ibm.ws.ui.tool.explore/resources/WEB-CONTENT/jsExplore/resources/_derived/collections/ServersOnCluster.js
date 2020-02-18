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
   * ServersOnCluster is a collection that represents the set of all of the servers which are members of a cluster.
   * It links back to the Cluster from which it was created. The ServersOnCluster collection is also an
   * Observer, as it watches its parent Cluster object and will update its tally and servers list in
   * response to the Cluster object changing.
   * 
   * The cumulative public attributes for ServersOnCluster are as follows:
   * @class
   * @typedef {Object} ServersOnCluster
   * @property {string} id                    - The resource's unique ID within the set of same type
   * @property {string} type                  - The resource's type
   * @property {string} viewType              - The resource's viewType, which indicates which view to use to display the resource 
   * @property {number} up                    - The number of servers that are considered to be started
   * @property {number} down                  - The number of servers that are considered to be stopped
   * @property {number} unknown               - The number of servers that are considered to be in an unknown state
   * @property {Array.<Server>} list          - The list of Server objects, based on the Clusters's list of server IDs
   * @property {Cluster} cluster              - The Cluster for which this collection applies
   * @property {Cluster} parentResource       - The Cluster for which this collection applies
   */
  return declare('ServersOnCluster', [ObservableResource, Observer], {
    /** Hard-code the type to be 'serversOnCluster' **/
    /** @type {string} */ id: ID.getServersOnClusterUpper(), // TODO: clean this up too. Its required by type hierarchy
    /** @type {string} */ type: 'serversOnCluster',
    /** @type {string} */ viewType: 'Servers',

    /** Set by the constructor only. Acts as the link to the parent resource. **/
    /** @type {Cluster} */ cluster: null,
    /** @type {Cluster} */ parentResource: null,

    /** Set during construction and onAttributeChange events **/
    /** @type {number} */ up: 0,
    /** @type {number} */ down: 0,
    /** @type {number} */ unknown: 0,
    /** @type {Array.<Server>} */ list: null,

    /**
     * Perform all required initialization.
     * 
     * @param {Object} init
     */
    constructor: function(init) {
      if (!init) this._throwError('ServersOnCluster created without an initialization object');
      (init.cluster) ? this.cluster = init.cluster : this._throwError('ServersOnCluster created without a Cluster');

      var servers = [];
      (init.servers) ? servers = init.servers : this._throwError('ServersOnCluster created without an array of Server');

      this.id = ID.getResourceOnResource(this.type, this.cluster.id);
      this.parentResource = this.cluster;
      this.up = this.cluster.servers.up;
      this.down = this.cluster.servers.down;
      this.unknown = this.cluster.servers.unknown;

      this.list = [];
      // TODO: Store a copy of the names. We do not want to keep a copy of the objects as that breaks the pattern of lists of Strings.
      for (var i = 0; i < servers.length; i++) {
        this.list.push(servers[i]);
      }

      this.cluster.subscribe(this);
    },

    destroy: function() {
      this.cluster.unsubscribe(this);
    },

    _handleChangeEvent: function() { /* noop - here purely because of the type hierarchy */ },

    /**
     * The Observer method to detect when the observed Cluster is removed from the collective.
     * If the Cluster was removed, we've also been removed, so destroy ourself.
     */
    onDestroyed: function() {
      this.isDestroyed = true;
      this._notifyObservers('onDestroyed');
      //this.destroy();
    },

    /**
     * The Observer method to detect when the observed Cluster has the server tallies change.
     * When the tallies change, update our copy.
     * 
     * @param {string} tallies The new values for the servers tallies of the Cluster
     */
    onServersTallyChange: function(tallies) {
      this._updateTally('onTallyChange', tallies, this, ['up', 'down', 'unknown']);
    },

    /**
     * The Observer method to detect when the observed Cluster server list changes.
     * If things were added or removed, our list needs to be updated.
     * 
     * @param {Array.<stinrg>} newList The final list of server IDs. This is ignored by this method.
     * @param {Array.<string>} oldList The previous list of server IDs. This is ignored by this method.
     * @param {Array.<string>} added The added list of server IDs. Add any Server objects in our list if they were added.
     * @param {Array.<string>} removed The remove list of server IDs. Remove any Server objects in our list if they were removed.
     */
    onServersListChange: function(newList, oldList, added, removed) {
      // Process removals first
      if (added || removed) {
        var prevList = this.list.slice(0); // Make a shallow copy of the list

        if (removed) {
          for (var i = (this.list.length - 1); i >= 0 ; i--) {
            var curServer = this.list[i];
            // If it matches removal, splice it out
            for (var r = 0; r < removed.length; r++) {
              if (removed[r] === curServer.id) 
                this.list.splice(i, 1);
            }
          }
        }

        // Finally append new additions
        if (added) {
          // Something was added, we need to resolve it then notify
          var me = this;
          this.resourceManager.getServer(added).then(function(servers) {
            Array.prototype.push.apply(me.list, servers);
            me._notifyObservers('onServersListChange', [me.list, prevList, added, removed]);
          });
        } else {
          // Nothing added, notify now
          this._notifyObservers('onServersListChange', [this.list, prevList, added, removed]);
        }
      }
    },

    /**
     * @returns Deferred which resolves with a Cluster object.
     */
    getCluster: function() {
      // this.cluster is the real Cluster object from which this was derived
      return util.returnValueAsDeferred(this.cluster);
    }

  });

});
