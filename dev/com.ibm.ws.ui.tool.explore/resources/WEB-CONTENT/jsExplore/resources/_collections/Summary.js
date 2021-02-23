/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
define([ 'dojo/_base/declare', '../ObservableResource', "jsExplore/utils/ID" ],
    function(declare, ObservableResource, ID) {

  /**
   * Summary represents the collection of collections. It contains only the tallies of the available
   * applications, clusters, servers and hosts in the collective.
   * 
   * The Summary collection is created based on the REST API response. See the ResourceManager
   * for details on how the REST API response is converted to the initialization object.
   * 
   * The cumulative public attributes for Summary are as follows:
   * @class
   * @typedef {Object} Summary
   * @property {string} id                   - The resource's unique ID within the set of same type
   * @property {string} type                 - The resource's type
   * @property {string} name                 - The name of the collective
   * @property {string} uuid                 - The UUID of the collective
   * @property {Object} applications         - The set of application tallies
   * @property {number} applications.up      - The number of applications that are considered to be completely started
   * @property {number} applications.down    - The number of applications that are considered to be stopped or otherwise unavailable
   * @property {number} applications.unknown - The number of applications that are considered to be in an unknown state
   * @property {number} applications.partial - The number of applications that are considered to be partially running, with some instances started
   * @property {Object} clusters             - The set of clusters tallies
   * @property {number} clusters.up          - The number of clusters that are considered to be completely started
   * @property {number} clusters.down        - The number of clusters that are considered to be stopped or otherwise unavailable
   * @property {number} clusters.unknown     - The number of clusters that are considered to be in an unknown state
   * @property {number} clusters.partial     - The number of clusters that are considered to be partially running, with some instances started
   * @property {Object} servers              - The set of servers tallies
   * @property {number} servers.up           - The number of servers that are running
   * @property {number} servers.down         - The number of servers that are stopped
   * @property {number} servers.unknown      - The number of servers that are in an unknown state
   * @property {Object} hosts                - The set of hosts tallies
   * @property {number} hosts.up             - The number of hosts that are considered to have all servers running
   * @property {number} hosts.down           - The number of hosts that are considered to have all servers stopped
   * @property {number} hosts.unknown        - The number of hosts that are considered to have all servers in an unknown state
   * @property {number} hosts.partial        - The number of hosts that are considered to have some servers running
   * @property {number} hosts.empty          - The number of hosts that are considered to have no servers at all
   */
  return declare('Summary', [ObservableResource], {
    /** Hard-code the id and type to be 'servers' **/
    /** @type {string} */ id: ID.getSummary(),
    /** @type {string} */ type: 'summary',

    /** Set during construction ONLY */
    /** @type {string} */ name: null,
    /** @type {string} */ uuid: null,

    /** Set during construction and handleChangeEvent **/
    /** @type {Object} */ applications: null,
    /** @type {Object} */ clusters: null,
    /** @type {Object} */ servers: null,
    /** @type {Object} */ hosts: null,
    /** @type {Object} */ runtimes : null,

    /**
     * Construct the initial Summary state.
     * 
     * @param {Object} init The initialization object
     */
    constructor: function(init) {
      if (!init) { this._throwError('Summary created without an initialization object'); }
      (init.name)         ? this.name         = init.name         : this._throwError('Summary created without a name');
      (init.uuid)         ? this.uuid         = init.uuid         : this._throwError('Summary created without a uuid');
      (init.applications) ? this.applications = init.applications : this._throwError('Summary created without an applications tally');
      (init.clusters)     ? this.clusters     = init.clusters     : this._throwError('Summary created without an clusters tally');
      (init.servers)      ? this.servers      = init.servers      : this._throwError('Summary created without an servers tally');
      (init.hosts)        ? this.hosts        = init.hosts        : this._throwError('Summary created without an hosts tally');
      (init.runtimes)     ? this.runtimes     = init.runtimes     : this._throwError('Summary created without a runtimes tally');
    },

    /**
     * Handle a change event for this resource.
     * 
     * The event can have the following properties:
     * Summary Event {
     *   type: 'summary',
     *   applications: { (optional)
     *     up, down, unknown, partial (required)
     *   },
     *   clusters: { (optional)
     *     up, down, unknown, partial (required)
     *   },
     *   servers: { (optional)
     *     up, down, unknown (required)
     *   },
     *   hosts: { (optional)
     *     up, down, unknown, partial, empty (required)
     *   },
     *   runtimes: { (optional)
     *     up, down, unknown, partial, empty (required)
     *   }
     * }
     * 
     * If the inbound event has the wrong type, the event is ignored. This does not provide any sort of security,
     * but does guard against programming errors and will make the code more serviceable / robust.
     * 
     * The following observer methods may be called:
     * -- onApplicationsTallyChange(newTally, oldTally) - parameters are objects: {up, down, unknown, partial}
     * -- onClustersTallyChange(newTally, oldTally) - parameters are objects: {up, down, unknown, partial}
     * -- onServersTallyChange(newTally, oldTally) - parameters are objects: {up, down, unknown}
     * -- onHostsTallyChange(newTally, oldTally) - parameters are objects: {up, down, unknown, partial, empty}
     * -- onRuntimesTallyChange(newTally, oldTally) - parameters are objects: {up, down, unknown, partial, empty}
     * 
     * @param {Object} e The received Summary Event object
     */
    _handleChangeEvent: function(e) {
      console.log('Summary got an event', e);

      if (e.type !== this.type) { 
        console.error('Summary got an event which did not have the correct type. The event will be ignored. Received event type: ' + e.type);
        return;
      }

      if (e.applications) {
        var prev = this.applications;
        this.applications = e.applications;
        this._notifyObservers('onApplicationsTallyChange', [this.applications, prev]);
      }
      if (e.clusters) {
        var prev = this.clusters;
        this.clusters = e.clusters;
        this._notifyObservers('onClustersTallyChange', [this.clusters, prev]);
      }
      if (e.servers) {
        var prev = this.servers;
        this.servers = e.servers;
        this._notifyObservers('onServersTallyChange', [this.servers, prev]);
      }
      if (e.hosts) {
        var prev = this.hosts;
        this.hosts = e.hosts;
        this._notifyObservers('onHostsTallyChange', [this.hosts, prev]);
      }
      if (e.runtimes) {
        var prev = this.runtimes;
        this.runtimes = e.runtimes;
        this._notifyObservers('onRuntimesTallyChange', [this.runtimes, prev]);
      }
    }

  });

});
