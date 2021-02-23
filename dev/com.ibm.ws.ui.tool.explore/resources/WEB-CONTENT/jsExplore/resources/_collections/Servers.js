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
define([ 'dojo/_base/declare', '../ObservableResource', 'jsExplore/utils/ID' ],
    function(declare, ObservableResource, ID) {

  /**
   * Servers represents a collection of Server objects. The values stored in Servers are a high-level tally
   * of the number of servers which are considered to be running (up), stopped (down) or unknown (unknown).
   * The list of server IDs is also stored. A server ID is a server tuple, which is represented as "host,userDir,server".
   * The direct Server objects are not stored as that would require a very large amount of memory in a large collective topology.
   * 
   * The Servers collection is created based on the REST API response. See the ResourceManager
   * for details on how the REST API response is converted to the initialization object.
   * 
   * The cumulative public attributes for Servers are as follows:
   * @class
   * @typedef {Object} Servers
   * @property {string} id            - The resource's unique ID within the set of same type
   * @property {string} type          - The resource's type
   * @property {number} up            - The number of servers that are running
   * @property {number} down          - The number of servers that are stopped
   * @property {number} unknown       - The number of servers that are in an unknown state
   * @property {Array.<string>} list  - The list of server IDs. The list will have no duplicate values.
   */
  return declare('Servers', [ObservableResource], {
    /** Hard-code the id and type to be 'servers' **/
    /** @type {string} */ id: ID.getServers(),
    /** @type {string} */ type: 'servers',

    /** Set during construction and handleChangeEvent **/
    /** @type {number} */ up: 0,
    /** @type {number} */ down: 0,
    /** @type {number} */ unknown: 0,
    /** @type {Array.<string>} */ list: [],

    /**
     * Construct the initial Servers state.
     * 
     * @param {Object} init The initialization object
     */
    constructor: function(init) {
      if (!init) { this._throwError('Servers created without an initialization object'); }
      (init.up >= 0)             ? this.up      = init.up      : this._throwError('Servers created without an initial up tally');
      (init.down >= 0)           ? this.down    = init.down    : this._throwError('Servers created without an initial down tally');
      (init.unknown >= 0)        ? this.unknown = init.unknown : this._throwError('Servers created without an initial unknown tally');
      (Array.isArray(init.list)) ? this.list    = init.list    : this._throwError('Servers created without an initial list of server names');
    },

    /**
     * Handle a change event for this resource.
     * 
     * The event can have the following properties:
     * Servers Event {
     *   type: 'servers',
     *   up, down, unknown,
     *   added: [ "tuple" ] (optional),
     *   removed: [ "tuple" ] (optional)
     * }
     * 
     * If the inbound event has the wrong type, the event is ignored. This does not provide any sort of security,
     * but does guard against programming errors and will make the code more serviceable / robust.
     * 
     * The following observer methods may be called:
     * -- onTallyChange(newTally, oldTally) - parameters are objects: {up, down, unknown}
     * -- onListChange(newList, oldList, added, removed) - parameters are Array.<string>
     * 
     * @param {Object} e The received Servers Event object
     */
    _handleChangeEvent: function(e) {
      console.log('Servers got an event', e);

      if (e.type !== this.type) { 
        console.error('Servers got an event which did not have the correct type. The event will be ignored. Received event type: ' + e.type);
        return;
      }

      this._updateArray('onListChange', this.list, e.added, e.removed);
      this._updateTally('onTallyChange', e, this, ['up', 'down', 'unknown']);
    }

  });

});
