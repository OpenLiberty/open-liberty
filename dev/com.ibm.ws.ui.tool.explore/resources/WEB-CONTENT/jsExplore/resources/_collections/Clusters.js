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
   * Clusters represents a collection of Cluster objects. The values stored in Clusters are a high-level tally
   * of the number of clusters which are considered to be up, down, unknown, and partially started. The list of
   * cluster IDs is also stored. The direct Cluster objects are not stored as that would require a very large
   * amount of memory in a large collective topology.
   * 
   * The Clusters collection is created based on the REST API response. See the ResourceManager
   * for details on how the REST API response is converted to the initialization object.
   * 
   * The cumulative public attributes for Clusters are as follows:
   * @class
   * @typedef {Object} Clusters
   * @property {string} id            - The resource's unique ID within the set of same type
   * @property {string} type          - The resource's type
   * @property {number} up            - The number of clusters that are considered to be completely started
   * @property {number} down          - The number of clusters that are considered to be stopped or otherwise unavailable
   * @property {number} unknown       - The number of clusters that are considered to be in an unknown state
   * @property {number} partial       - The number of clusters that are considered to be partially running, with some instances started
   * @property {Array.<string>} list  - The list of cluster IDs. The list will have no duplicate values.
   */
  return declare('Clusters', [ObservableResource], {
    /** Hard-code the id and type to be 'clusters' **/
    /** @type {string} */ id: ID.getClusters(),
    /** @type {string} */ type: 'clusters',

    /** Set during construction and handleChangeEvent **/
    /** @type {number} */ up: 0,
    /** @type {number} */ down: 0,
    /** @type {number} */ unknown: 0,
    /** @type {number} */ partial: 0,
    /** @type {Array.<string>} */ list: [],

    /**
     * Construct the initial Clusters state.
     * 
     * @param {Object} init The initialization object
     */
    constructor: function(init) {
      if (!init) { this._throwError('Clusters created without an initialization object'); }
      (init.up >= 0)             ? this.up      = init.up      : this._throwError('Clusters created without an initial up tally');
      (init.down >= 0)           ? this.down    = init.down    : this._throwError('Clusters created without an initial down tally');
      (init.unknown >= 0)        ? this.unknown = init.unknown : this._throwError('Clusters created without an initial unknown tally');
      (init.partial >= 0)        ? this.partial = init.partial : this._throwError('Clusters created without an initial partial tally');
      (Array.isArray(init.list)) ? this.list    = init.list    : this._throwError('Clusters created without an initial list of cluster names');
    },

    /**
     * Handle a change event for this resource.
     * 
     * The event can have the following properties:
     * Clusters Event {
     *   type: 'clusters',
     *   up, down, unknown, partial,
     *   added: [ "name" ] (optional),
     *   removed: [ "name" ] (optional)
     * }
     * 
     * If the inbound event has the wrong type, the event is ignored. This does not provide any sort of security,
     * but does guard against programming errors and will make the code more serviceable / robust.
     * 
     * The following observer methods may be called:
     * -- onTallyChange(newTally, oldTally) - parameters are objects: {up, down, unknown, partial}
     * -- onListChange(newList, oldList, added, removed) - parameters are Array.<string>
     * 
     * @param {Object} e The received Clusters Event object
     */
    _handleChangeEvent: function(e) {
      console.log('Clusters got an event!', e);

      if (e.type !== this.type) { 
        console.error('Clusters got an event which did not have the correct type. The event will be ignored. Received event type: ' + e.type);
        return;
      }

      this._updateArray('onListChange', this.list, e.added, e.removed);
      this._updateTally('onTallyChange', e, this, ['up', 'down', 'unknown', 'partial']);
    }

  });

});
