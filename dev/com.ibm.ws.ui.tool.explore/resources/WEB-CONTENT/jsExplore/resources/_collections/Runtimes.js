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
define([ 'dojo/_base/declare', '../ObservableResource' ],
    function(declare, ObservableResource) {

  /**
   * Runtimes represents a collection of Runtime objects. The values stored in Runtimes are a high-level tally
   * of the number of runtimes which are considered to be running (up), stopped (down) or unknown (unknown).
   * The list of runtime IDs is also stored. A runtime ID is a runtime tuple, which is represented as "host,userDir,runtime".
   * The direct Runtime objects are not stored as that would require a very large amount of memory in a large collective topology.
   * 
   * The Runtimes collection is created based on the REST API response. See the ResourceManager
   * for details on how the REST API response is converted to the initialization object.
   * 
   * The cumulative public attributes for Runtimes are as follows:
   * @class
   * @typedef {Object} Runtimes
   * @property {string} id            - The resource's unique ID within the set of same type
   * @property {string} type          - The resource's type
   * @property {number} up            - The number of runtimes that are running
   * @property {number} down          - The number of runtimes that are stopped
   * @property {number} unknown       - The number of runtimes that are in an unknown state
   * @property {number} partial       - The number of runtimes that are considered to have some servers running
   * @property {number} empty         - The number of runtimes that are considered to have no servers at all
   * @property {Array.<string>} list  - The list of runtime IDs. The list will have no duplicate values.
   */
  return declare('Runtimes', [ObservableResource], {
    /** Hard-code the id and type to be 'runtimes' **/
    /** @type {string} */ id: 'runtimes',
    /** @type {string} */ type: 'runtimes',

    /** Set during construction and handleChangeEvent **/
    /** @type {number} */ up: 0,
    /** @type {number} */ down: 0,
    /** @type {number} */ unknown: 0,
    /** @type {number} */ partial: 0,
    /** @type {number} */ empty: 0,
    /** @type {Array.<string>} */ list: [],

    /**
     * Construct the initial Runtimes state.
     * 
     * @param {Object} init The initialization object
     */
    constructor: function(init) {
      if (!init) { this._throwError('Runtimes created without an initialization object'); }
      (init.up >= 0)             ? this.up      = init.up      : this._throwError('Runtimes created without an initial up tally');
      (init.down >= 0)           ? this.down    = init.down    : this._throwError('Runtimes created without an initial down tally');
      (init.unknown >= 0)        ? this.unknown = init.unknown : this._throwError('Runtimes created without an initial unknown tally');
      (init.partial >= 0)        ? this.partial = init.partial : this._throwError('Runtimes created without an initial partial tally');
      (init.empty >= 0)          ? this.empty   = init.empty   : this._throwError('Runtimes created without an initial empty tally');
      (Array.isArray(init.list))     ? this.list    = init.list    : this._throwError('Runtimes created without an initial list of runtime names');
    },

    /**
     * Handle a change event for this resource.
     * 
     * The event can have the following properties:
     * Runtimes Event {
     *   type: 'runtimes',
     *   allServersRunning, allServersStopped, allServersUnknown, someServersRunning, noServers
     *   added: [ "tuple" ] (optional),
     *   removed: [ "tuple" ] (optional)
     * }
     * 
     * If the inbound event has the wrong type, the event is ignored. This does not provide any sort of security,
     * but does guard against programming errors and will make the code more serviceable / robust.
     * 
     * The following runtime methods may be called:
     * -- onTallyChange(newTally, oldTally) - parameters are objects: {up, down, unknown}
     * -- onListChange(newList, oldList, added, removed) - parameters are Array.<string>
     * 
     * @param {Object} e The received Runtimes Event object
     */
    _handleChangeEvent: function(e) {
      console.log('Runtimes got an event', e);

      if (e.type !== this.type) { 
        console.error('Runtimes got an event which did not have the correct type. The event will be ignored. Received event type: ' + e.type);
        return;
      }

      this._updateArray('onListChange', this.list, e.added, e.removed);
      this._updateTally('onTallyChange', e, this, ['up', 'down', 'unknown', 'partial', 'empty']);
    }

  });

});
