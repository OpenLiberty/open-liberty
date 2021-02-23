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
   * ServersOnHost is a collection that represents the set of all of the servers defined on a host.
   * It links back to the Host from which it was created. The ServersOnHost collection is also an
   * Observer, as it watches its parent Host object and will update its tally and servers list in
   * response to the Host object changing.
   * 
   * The cumulative public attributes for ServersOnHost are as follows:
   * @class
   * @typedef {Object} ServersOnHost
   * @property {string} id                    - The resource's unique ID within the set of same type
   * @property {string} type                  - The resource's type
   * @property {string} viewType              - The resource's viewType, which indicates which view to use to display the resource 
   * @property {number} up                    - The number of servers that are considered to be started
   * @property {number} down                  - The number of servers that are considered to be stopped
   * @property {number} unknown               - The number of servers that are considered to be in an unknown state
   * @property {Array.<Server>} list          - The list of Server objects, based on the Hosts's list of server IDs
   * @property {Host} host                    - The host for which this collection applies
   * @property {Host} parentResource          - The host for which this collection applies
   */
  return declare('ServersOnHost', [ObservableResource, Observer], {
    /** Hard-code the type to be 'serversOnHost' **/
    /** @type {string} */ id: ID.getServersOnHostUpper(),//'ServersOnHost', // TODO: clean this up too. Its required by type hierarchy
    /** @type {string} */ type: 'serversOnHost',
    /** @type {string} */ viewType: 'Servers',

    /** Set by the constructor only. Acts as the link to the parent resource. **/
    /** @type {Host} */ host: null,
    /** @type {Host} */ parentResource: null,

    /** Set during construction and onAttributeChange events **/
    /** @type {number} */ up: 0,
    /** @type {number} */ down: 0,
    /** @type {number} */ unknown: 0,
    /** @type {Array.<Server>} */ list: null,

    /**
     * Perform all required initialization.
     */
    constructor: function(init) {
      if (!init) this._throwError('ServersOnHost created without an initialization object');
      (init.host) ? this.parentResource = this.host = init.host : this._throwError('ServersOnHost created without a host');

      var servers = [];
      (init.servers) ? servers = init.servers : this._throwError('ServersOnHost created without an array of Server');

      this.id = ID.getResourceOnResource(this.type, this.host.id);

      this.parentResource = this.host;
      this.up = this.host.servers.up;
      this.down = this.host.servers.down;
      this.unknown = this.host.servers.unknown;

      this.list = [];
      // TODO: Store a copy of the names. We do not want to keep a copy of the objects as that breaks the pattern of lists of Strings.
      for (var i = 0; i < servers.length; i++) {
        this.list.push(servers[i]);
      }

      this.host.subscribe(this);
    },

    destroy: function() {
      this.host.unsubscribe(this);
    },

    _handleChangeEvent: function() { /* noop - here purely because of the type hierarchy */ },

    /**
     * The Observer method to detect when the observed Host is removed from the collective.
     * If the Host was removed, we've also been removed, so destroy ourself.
     */
    onDestroyed: function() {
      this.isDestroyed = true;
      this._notifyObservers('onDestroyed');
      //this.destroy();
    },

    /**
     * The Observer method to detect when the observed Host has the server tallies change.
     * When the tallies change, update our copy.
     * 
     * @param {string} tallies The new values for the servers tallies of the Host
     */
    onServersTallyChange: function(tallies) {
      this._updateTally('onTallyChange', tallies, this, ['up', 'down', 'unknown']);
    },

    /**
     * The Observer method to detect when the observed Host server list changes.
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
     * @returns Deferred which resolves with a Host object.
     */
    getHost: function() {
      // this.host is the real Host object from which this was derived
      return util.returnValueAsDeferred(this.host);
    }

  });

});
