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
   * ServersOnRuntime is a collection that represents the set of all of the servers using on a Runtime.
   * It links back to the Runtime from which it was created. The ServersOnRuntime collection is also an
   * Observer, as it watches its parent Runtime object and will update its tally and servers list in
   * response to the Runtime object changing.
   * 
   * The cumulative public attributes for ServersOnRuntime are as follows:
   * @class
   * @typedef {Object} ServersOnRuntime
   * @property {string} id                    - The resource's unique ID within the set of same type
   * @property {string} type                  - The resource's type
   * @property {string} viewType              - The resource's viewType, which indicates which view to use to display the resource 
   * @property {number} up                    - The number of servers that are considered to be started
   * @property {number} down                  - The number of servers that are considered to be stopped
   * @property {number} unknown               - The number of servers that are considered to be in an unknown state
   * @property {Array.<Server>} list          - The list of Server objects, based on the Runtime's list of Server objects
   * @property {Host} host                    - The host for which this collection applies
   * @property {Host} parentResource          - The host for which this collection applies
   */
  return declare('ServersOnRuntime', [ObservableResource, Observer], {
    /** Hard-code the type to be 'serversOnRuntime' **/
    /** @type {string} */ id: ID.getServersOnRuntimeUpper(),//'ServersOnRuntime', // TODO: clean this up too. Its required by type hierarchy
    /** @type {string} */ type: 'serversOnRuntime',
    /** @type {string} */ viewType: 'Servers',

    /** Set by the constructor only. Acts as the link to the parent resource. **/
    /** @type {Runtime} */ runtime: null,
    /** @type {Runtime} */ parentResource: null,

    /** Set by the constructor **/
    /** @type {number} */ up: 0,
    /** @type {number} */ down: 0,
    /** @type {number} */ unknown: 0,
    /** @type {Array.<Server>} */ list: null,

    /**
     * Perform all required initialization.
     */
    constructor: function(init) {
      if (!init) this._throwError('ServersOnRuntime created without an initialization object');
      (init.runtime) ? this.runtime = init.runtime : this._throwError('ServersOnRuntime created without a runtime');

      this.id = ID.getResourceOnResource(this.type, this.runtime.id);
      this.parentResource = this.runtime;
      this.up = this.runtime.servers.up;
      this.down = this.runtime.servers.down;
      this.unknown = this.runtime.servers.unknown;
      this.list = this.runtime.servers.list;

      this.runtime.subscribe(this);
    },

    destroy: function() {
      this.runtime.unsubscribe(this);
    },

    _handleChangeEvent: function() { /* noop - here purely because of the type hierarchy */ },

    onDestroyed: function() {
      this.isDestroyed = true;
      this._notifyObservers('onDestroyed');
      //this.destroy();
    },

    onServersTallyChange: function(tallies) {
      this._updateTally('onTallyChange', tallies, this, ['up', 'down', 'unknown']);
    },

    onServersListChange: function(newList, oldList, added, removed) {
      // Because we are directly mapped to the Runtime's list, and that is a list of Server objects, there is nothing to actually do!
      // Just pass the values through and call our Observers.
      this._notifyObservers('onListChange', [newList, oldList, added, removed]);
    },

    /**
     * @returns Deferred which resolves with a Host object.
     */
    getHost: function() {
      // this.host is the real Host object from which this was derived
      return util.returnValueAsDeferred(this.runtime.host);
    },

    /**
     * @returns Deferred which resolves with a Runtime object.
     */
    getRuntime: function() {
      // this.runtime is the real Runtime object from which this was derived
      return util.returnValueAsDeferred(this.runtime);
    }

  });

});
