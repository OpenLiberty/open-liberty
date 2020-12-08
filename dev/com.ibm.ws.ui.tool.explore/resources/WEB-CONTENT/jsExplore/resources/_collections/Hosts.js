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
   * Hosts represents a collection of Host objects. The values stored in Hosts are a high-level tally
   * of the number of hosts which are considered to have all servers running (up), all servers stopped (down),
   * some servers running (partial), all servers unknown (unknown) and no servers (empty). The list of
   * host IDs is also stored. The direct Host objects are not stored as that would require a very large
   * amount of memory in a large collective topology.
   * 
   * The Hosts collection is created based on the REST API response. See the ResourceManager
   * for details on how the REST API response is converted to the initialization object.
   * 
   * The cumulative public attributes for Hosts are as follows:
   * @class
   * @typedef {Object} Hosts
   * @property {string} id            - The resource's unique ID within the set of same type
   * @property {string} type          - The resource's type
   * @property {number} up            - The number of hosts that are considered to have all servers running
   * @property {number} down          - The number of hosts that are considered to have all servers stopped
   * @property {number} unknown       - The number of hosts that are considered to have all servers in an unknown state
   * @property {number} partial       - The number of hosts that are considered to have some servers running
   * @property {number} empty         - The number of hosts that are considered to have no servers at all
   * @property {Array.<string>} list  - The list of host IDs. The list will have no duplicate values.
   */
  return declare('Hosts', [ObservableResource], {
    /** Hard-code the id and type to be 'hosts' **/
    /** @type {string} */ id: ID.getHosts(),
    /** @type {string} */ type: 'hosts',

    /** Set during construction and handleChangeEvent **/
    /** @type {number} */ up: 0,
    /** @type {number} */ down: 0,
    /** @type {number} */ unknown: 0,
    /** @type {number} */ partial: 0,
    /** @type {number} */ empty: 0,
    /** @type {Array.<string>} */ list: [],

    /**
     * Construct the initial Hosts state.
     * 
     * @param {Object} init The initialization object
     */
    constructor: function(init) {
      if (!init) { this._throwError('Hosts created without an initialization object'); }
      (init.up >= 0)             ? this.up      = init.up      : this._throwError('Hosts created without an initial up tally');
      (init.down >= 0)           ? this.down    = init.down    : this._throwError('Hosts created without an initial down tally');
      (init.unknown >= 0)        ? this.unknown = init.unknown : this._throwError('Hosts created without an initial unknown tally');
      (init.partial >= 0)        ? this.partial = init.partial : this._throwError('Hosts created without an initial partial tally');
      (init.empty >= 0)          ? this.empty   = init.empty   : this._throwError('Hosts created without an initial empty tally');
      (Array.isArray(init.list)) ? this.list    = init.list    : this._throwError('Hosts created without an initial list of host names');
    },

    /**
     * Handle a change event for this resource.
     * 
     * The event can have the following properties:
     * Hosts Event {
     *   type: 'hosts',
     *   up, down, unknown, partial, empty,
     *   added: [ "name" ] (optional),
     *   removed: [ "name" ] (optional)
     * }
     * 
     * If the inbound event has the wrong type, the event is ignored. This does not provide any sort of security,
     * but does guard against programming errors and will make the code more serviceable / robust.
     * 
     * The following observer methods may be called:
     * -- onTallyChange(newTally, oldTally) - parameters are objects: {up, down, unknown, partial, empty}
     * -- onListChange(newList, oldList, added, removed) - parameters are Array.<string>
     * 
     * @param {Object} e The received Hosts Event object
     */
    _handleChangeEvent: function(e) {
      console.log('Hosts got an event!', e);

      if (e.type !== this.type) { 
        console.error('Hosts got an event which did not have the correct type. The event will be ignored. Received event type: ' + e.type);
        return;
      }

      this._updateArray('onListChange', this.list, e.added, e.removed);
      this._updateTally('onTallyChange', e, this, ['up', 'down', 'unknown', 'partial', 'empty']);
    }

  });

});
