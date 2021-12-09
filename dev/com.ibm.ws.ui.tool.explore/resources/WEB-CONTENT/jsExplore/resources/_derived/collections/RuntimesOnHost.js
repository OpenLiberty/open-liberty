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
define([ 'dojo/_base/declare', '../../_util', '../../ObservableResource', '../../Observer', '../objects/Runtime', 'jsExplore/utils/ID' ],
    function(declare, util, ObservableResource, Observer, Runtime, ID) {

  /**
   * RuntimesOnHost collection represents the set of all runtimes on a host. It links back to the Host from
   * which it was created. The RuntimesOnHost collection is also an Observer, as it watches its parent
   * Host object and will update its list of Runtime objects in response to the Host object changing.
   * 
   * The cumulative public attributes for RuntimesOnHost are as follows:
   * @class
   * @typedef {Object} RuntimesOnHost
   * @property {string} id                    - The resource's unique ID within the set of same type
   * @property {string} type                  - The resource's type
   * @property {string} viewType              - The resource's viewType, which indicates which view to use to display the resource
   * @property {number} up                    - The number of runtimes that are considered to have all servers running
   * @property {number} down                  - The number of runtimes that are considered to have all servers stopped
   * @property {number} unknown               - The number of runtimes that are considered to have all servers in an unknown state
   * @property {number} partial               - The number of runtimes that are considered to have some servers running
   * @property {number} empty                 - The number of runtimes that are considered to have no servers at all 
   * @property {Array.<Runtime>} list         - The list of Runtime objects, based on the Host's list of runtimes.
   * @property {Host} host                    - The host for which this collection applies
   * @property {Host} parentResource          - The host for which this collection applies
   */
  return declare('RuntimesOnHost', [ObservableResource, Observer], {
    /** Hard-code the type to be 'runtimesOnHost' **/
    /** @type {string} */ id: ID.getRuntimesOnHostUpper(), // TODO: clean this up too. Its required by type hierarchy
    /** @type {string} */ type: 'runtimesOnHost',
    /** @type {string} */ viewType: 'Runtimes',

    /** Set by the constructor only. Acts as the link to the parent resource. **/
    /** @type {Host} */ host: null,
    /** @type {Host} */ parentResource: null,

    /** Set during construction and onAttributeChange events **/
    /** @type {number} */ up: 0,
    /** @type {number} */ down: 0,
    /** @type {number} */ unknown: 0,
    /** @type {number} */ partial: 0,
    /** @type {number} */ empty: 0,
    /** @type {Array.<Runtime>} */ list: null,

    /**
     * Perform all required initialization.
     */
    constructor: function(init) {
      if (!init) this._throwError('RuntimesOnHost created without an initialization object');
      (init.host) ? this.host = init.host : this._throwError('RuntimesOnHost created without a host');

      var runtimes = [];
      (init.runtime) ? runtimes = init.runtime : this._throwError('RuntimesOnHost created without an array of Runtime');

      this.id = ID.getResourceOnResource(this.type, this.host.id);
      this.parentResource = this.host;

      // Initialize defaults
      this.up = 0;
      this.down = 0;
      this.unknown = 0;
      this.partial = 0;
      this.empty = 0;
      this.list = [];

      for (var i = 0; i < runtimes.length; i++) {
        this.list.push(runtimes[i]);
        // Calculate the up, down, unknown, partial and empty values
        if (runtimes[i].servers.up > 0 && runtimes[i].servers.down === 0 && runtimes[i].servers.unknown === 0) {
          this.up++;
        } else if (runtimes[i].servers.up === 0 && runtimes[i].servers.down > 0 && runtimes[i].servers.unknown === 0) {
          this.down++;
        } else if (runtimes[i].servers.up === 0 && runtimes[i].servers.down === 0 && runtimes[i].servers.unknown > 0) {
          this.unknown++;
        } else if (runtimes[i].servers.up === 0 && runtimes[i].servers.down === 0 && runtimes[i].servers.unknown === 0) {
          this.empty++;
        } else {
          this.partial++;
        }
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
     * The Observer method to detect when the observed Host runtimes list changes.
     * If runtimes were added or removed, our list needs to be updated.
     * 
     * @param {Array.<object>} newList The final list of runtimes. This is ignored by this method.
     * @param {Array.<object>} oldList The previous list of runtimes. This is ignored by this method.
     * @param {Array.<object>} added The added list of runtimes. Add any Runtime objects in our list if they were added.
     * @param {Array.<object>} removed The remove list of runtimes. Remove any Runtime objects in our list if they were removed.
     */
    onRuntimesChange: function(newList, oldList, added, removed) {
      // Process removals first
      if (added && added.length > 0 || removed && removed.length > 0) {
        var prevList = this.list.slice(0); // Make a shallow copy of the list

        if (removed && removed.length > 0) {
          for (var i = (this.list.length - 1); i >= 0 ; i--) {
            var curRuntime = this.list[i];
            // If it matches removal, splice it out
            for (var r = 0; r < removed.length; r++) {
              if (removed[r].id === curRuntime.id) {
                this.list.splice(i, 1);
                // Calculate the up, down, unknown, partial and empty values
                if (curRuntime.servers.up > 0 && curRuntime.servers.down === 0 && curRuntime.servers.unknown === 0) {
                  this.up--;
                } else if (curRuntime.servers.up === 0 && curRuntime.servers.down > 0 && curRuntime.servers.unknown === 0) {
                  this.down--;
                } else if (curRuntime.servers.up === 0 && curRuntime.servers.down === 0 && curRuntime.servers.unknown > 0) {
                  this.unknown--;
                } else if (curRuntime.servers.up === 0 && curRuntime.servers.down === 0 && curRuntime.servers.unknown === 0) {
                  this.empty--;
                } else {
                  this.partial--;
                }
              }
            }
          }
        }

        // Finally append new additions
        if (added && added.length > 0) {
          // Something was added, we need to resolve it then notify
          var me = this;
          this.resourceManager.getRuntime(added).then(function(runtimes) {
            for (var i = 0; i < runtimes.length; i++) {
              me.list.push(runtimes[i]);
              // Calculate the up, down, unknown, partial and empty values
              if (runtimes[i].servers.up > 0 && runtimes[i].servers.down === 0 && runtimes[i].servers.unknown === 0) {
                me.up++;
              } else if (runtimes[i].servers.up === 0 && runtimes[i].servers.down > 0 && runtimes[i].servers.unknown === 0) {
                me.down++;
              } else if (runtimes[i].servers.up === 0 && runtimes[i].servers.down === 0 && runtimes[i].servers.unknown > 0) {
                me.unknown++;
              } else if (runtimes[i].servers.up === 0 && runtimes[i].servers.down === 0 && runtimes[i].servers.unknown === 0) {
                me.empty++;
              } else {
                me.partial++;
              }
            }
            me._notifyObservers('onRuntimesListChange', [me.list, prevList, added, removed]);
          });
        } else {
          // Nothing added, notify now
          this._notifyObservers('onRuntimesListChange', [this.list, prevList, added, removed]);
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
