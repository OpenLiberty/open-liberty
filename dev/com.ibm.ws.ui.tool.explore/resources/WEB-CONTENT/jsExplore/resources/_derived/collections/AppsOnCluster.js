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
   * AppsOnCluster collection represents the set of all applications on a cluster. It links back to the
   * Cluster from which it was created. The AppsOnCluster collection is also an Observer, as it watches its parent
   * Cluster object and will update its tally and apps list in response to the Cluster object changing.
   * 
   * The cumulative public attributes for AppsOnCluster are as follows:
   * @class
   * @typedef {Object} AppsOnCluster
   * @property {string} id                    - The resource's unique ID within the set of same type
   * @property {string} type                  - The resource's type
   * @property {string} viewType              - The resource's viewType, which indicates which view to use to display the resource 
   * @property {number} up                    - The number of applications that are considered to be completely started
   * @property {number} down                  - The number of applications that are considered to be stopped or otherwise unavailable
   * @property {number} unknown               - The number of applications that are considered to be in an unknown state
   * @property {number} partial               - The number of applications that are considered to be partially started
   * @property {Array.<AppOnCluster>} list     - The list of AppOnCluster objects, based on the Cluster's list of application IDs.
   * @property {Cluster} cluster              - The Cluster to which the application is deployed
   * @property {Cluster} parentResource       - The Cluster to which the application is deployed
   */
  return declare('AppsOnCluster', [ObservableResource, Observer], {
    /** Hard-code the type to be 'appsOnCluster' **/
    /** @type {string} */ id: ID.getAppsOnClusterUpper(), // TODO: clean this up too. Its required by type hierarchy
    /** @type {string} */ type: 'appsOnCluster',
    /** @type {string} */ viewType: 'Apps',

    /** Set by the constructor only. Acts as the link to the parent resource. **/
    /** @type {Cluster} */ cluster: null,
    /** @type {Cluster} */ parentResource: null,

    /** Set during construction and onAttributeChange events **/
    /** @type {number} */ up: 0,
    /** @type {number} */ down: 0,
    /** @type {number} */ unknown: 0,
    /** @type {number} */ partial: 0,
    /** @type {Array.<AppOnCluster>} */ list: [],

    /**
     * Perform all required initialization.
     * 
     * @param {Object} init
     */
    constructor: function(init) {
      if (!init) this._throwError('AppsOnCluster created without an initialization object');
      (init.cluster) ? this.cluster = init.cluster : this._throwError('AppsOnCluster created without a Cluster');

      var apps = [];
      (init.appOnCluster) ? apps = init.appOnCluster : this._throwError('AppsOnCluster created without an array of AppOnCluster');

      this.id = ID.getResourceOnResource(this.type, this.cluster.id);
      this.parentResource = this.cluster;
      this.up = this.cluster.apps.up;
      this.down = this.cluster.apps.down;
      this.unknown = this.cluster.apps.unknown;
      this.partial = this.cluster.apps.partial;

      this.list = [];
      for (var i = 0; i < apps.length; i++) {
        this.list.push(apps[i]);
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
     * The Observer method to detect when the observed Cluster changes its apps collection.
     * If the Cluster changes its apps collection tallies, update our values.
     * 
     * @param {string} tallies The new values for the apps tallies of the Cluster
     */
    onAppsTallyChange: function(tallies) {
      this._updateTally('onTallyChange', tallies, this, ['up', 'down', 'unknown', 'partial']);
    },

    /**
     * The Observer method to detect when the observed Cluster changes its apps collection.
     * If the Cluster changes its apps collection list, update our stored instances.
     * 
     * @param {Array.<Object>} newList The final set of application names and state. This is ignored by this method.
     * @param {Array.<Object>} oldList The previous set of application names and state. This is ignored by this method.
     * @param {Array.<Object>} added The added set of application objects. Add any AppOnCluster objects in our list if they were added.
     * @param {Array.<string>} removed The remove set of application names. Remove any AppOnCluster objects in our list if they were removed.
     */
    onAppsListChange: function(newList, oldList, added, removed) {
      console.log("AppsOnCluster got an event! " + this.id);
      // Process removals first
      if (added || removed) {
        var prevList = this.list.slice(0); // Make a shallow copy of the list

        if (removed) {
          for (var i = (this.list.length - 1); i >= 0 ; i--) {
            var curApp = this.list[i];
            // If it matches removal, splice it out
            for (var r = 0; r < removed.length; r++) {
              if (removed[r] === curApp.name) {
                this.list.splice(i, 1);
              }
            }
          }
        }

        // Finally append new additions
        if (added) {
          // Something was added, we need to resolve it then notify
          var me = this;
          var addedNames = [];
          for (var j = 0; j < added.length; j++) {
            addedNames.push(added[j].name);
          }
          this.resourceManager.getAppOnCluster(this.cluster, addedNames).then(function(appsOnClusters) {
            Array.prototype.push.apply(me.list, appsOnClusters);
            me._notifyObservers('onAppsListChange', [me.list, prevList, addedNames, removed]);
          });
        } else {
          // Nothing added, notify now
          this._notifyObservers('onAppsListChange', [this.list, prevList, added, removed]);
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
