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
define([ 'dojo/_base/declare', '../../_util', '../../ObservableResource', '../../Observer', '../objects/AppOnServer', 'jsExplore/utils/ID' ],
    function(declare, util, ObservableResource, Observer, AppOnServer, ID) {

  /**
   * AppsOnServer collection represents the set of all applications on a single server. It links back to the
   * server from which it was created. The AppOnServers collection is also an Observer, as it watches its parent
   * server object and will update its tally and apps list in response to the server object changing.
   * 
   * The cumulative public attributes for AppsOnServer are as follows:
   * @class
   * @typedef {Object} AppsOnServer
   * @property {string} id                    - The resource's unique ID within the set of same type
   * @property {string} type                  - The resource's type
   * @property {string} viewType              - The resource's viewType, which indicates which view to use to display the resource 
   * @property {number} up                    - The number of applications that are considered to be completely started
   * @property {number} down                  - The number of applications that are considered to be stopped or otherwise unavailable
   * @property {number} unknown               - The number of applications that are considered to be in an unknown state
   * @property {Array.<AppOnServer>} list     - The list of AppOnServer objects, based on the Server's list of application IDs.
   * @property {(Server|StandaloneServer)} server - The server to which the application is deployed. May be in a collective member server or a stand alone server.
   * @property {(Server|StandaloneServer)} parentResource - The server to which the application is deployed. May be in a collective member server or a stand alone server.
   */
  return declare('AppsOnServer', [ObservableResource, Observer], {
    /** Hard-code the type to be 'appsOnServer' **/
    /** @type {string} */ id: ID.getAppsOnServerUpper(), // TODO: clean this up too. Its required by type hierarchy
    /** @type {string} */ type: 'appsOnServer',
    /** @type {string} */ viewType: 'Apps',

    /** Set by the constructor only. Acts as the link to the parent resource. **/
    /** @type {(Server|StandaloneServer)} */ server: null,
    /** @type {(Server|StandaloneServer)} */ parentResource: null,

    /** Set during construction and onAttributeChange events **/
    /** @type {number} */ up: 0,
    /** @type {number} */ down: 0,
    /** @type {number} */ unknown: 0,
    /** @type {Array.<AppOnServer>} */ list: [],

    /**
     * Perform all required initialization.
     * 
     * @param {Object} init
     */
    constructor: function(init) {
      if (!init) this._throwError('AppsOnServer created without an initialization object');
      (init.server) ? this.server = init.server : this._throwError('AppsOnServer created without a server');

      var instances = [];
      (init.appOnServer) ? instances = init.appOnServer : this._throwError('AppsOnServer created without an array of AppOnServer');

      this.id = ID.getResourceOnResource(this.type, this.server.id);
      this.parentResource = this.server;
      this.up = this.server.apps.up;
      this.down = this.server.apps.down;
      this.unknown = this.server.apps.unknown;

      this.list = [];
      // TODO: Store a copy of the names. We do not want to keep a copy of the objects as that breaks the pattern of lists of Strings.
      for (var i = 0; i < instances.length; i++) {
        this.list.push(instances[i]);
      }

      this.server.subscribe(this);
    },

    destroy: function() {
      this.server.unsubscribe(this);
    },

    _handleChangeEvent: function() { /* noop - here purely because of the type hierarchy */ },

    /**
     * The Observer method to detect when the observed Server is removed from the collective.
     * If the Server was removed, we've also been removed, so destroy ourself.
     */
    onDestroyed: function() {
      this.isDestroyed = true;
      this._notifyObservers('onDestroyed');
      //this.destroy();
    },

    /**
     * The Observer method to detect when the observed Server changes its apps collection.
     * If the Server changes its apps collection tallies, update our values.
     * 
     * @param {string} tallies The new values for the apps tallies of the Server
     */
    onAppsTallyChange: function(tallies) {
      this._updateTally('onTallyChange', tallies, this, ['up', 'down', 'unknown']);
    },

    /**
     * The Observer method to detect when the observed Server changes its apps collection.
     * If the Server changes its apps collection list, update our stored instances.
     * 
     * @param {Array.<Object>} newApps The final set of application names and state. This is ignored by this method.
     * @param {Array.<Object>} oldApps The previous set of application names and state. This is ignored by this method.
     * @param {Array.<Object>} added The added set of application names and state. Add any AppOnServer objects in our list if they were added.
     * @param {Array.<string>} removed The remove set of application names. Remove any AppOnServer objects in our list if they were removed.
     * @param {Array.<Object>} changed The changed set of application names and state. This is ignored by this method.
     */
    onAppsListChange: function(newList, oldList, added, removed, changed) {
      console.log("AppsOnServer got an event! " + this.id);
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
          var addedNames = [];
          for (var i = 0; i < added.length; i++) {
            addedNames.push(added[i].name);
          }
          var me = this;
          this.resourceManager.getAppOnServer(this.server, addedNames).then(function(appInstances) {
            Array.prototype.push.apply(me.list, appInstances);
            me._notifyObservers('onAppsListChange', [me.list, prevList, added, removed]);
          });
        } else {
          this._notifyObservers('onAppsListChange', [this.list, prevList, added, removed]);
        }
      }
    },

    /**
     * @returns Deferred which resolves with a Server object.
     */
    getServer: function() {
      // this.server is the real Server object from which this was derived
      return util.returnValueAsDeferred(this.server);
    }

  });
});
