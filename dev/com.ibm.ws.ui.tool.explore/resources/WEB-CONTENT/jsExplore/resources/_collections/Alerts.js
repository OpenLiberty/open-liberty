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
define([ 'dojo/_base/declare', '../ObservableResource', 'jsExplore/utils/ID' ],
    function(declare, ObservableResource, ID) {

  /**
   * Alerts represents the top-level collection of alerts in the collective. It contains all of the alerts
   * for the entire collection, including all unknown alerts, and all app alerts.
   * 
   * The Alerts collection is created based on the REST API response. See the ResourceManager
   * for details on how the REST API response is converted to the initialization object.
   * 
   * The cumulative public attributes for Alerts are as follows:
   * @class
   * @typedef {Object} Alerts
   * @property {string} id                   - The resource's unique ID within the set of same type
   * @property {string} type                 - The resource's type
   * @property {number} count                - The number of alerts spanning the entire collective
   * @property {Array.<Object>} unknown      - The list of unknown alerts
   * @property {string} unknown.id           - The id of the resource which has an unknown alert
   * @property {string} unknown.type         - The type of the resource which has an unknown alert
   * @property {Array.<Object>} app          - The list of app alerts
   * @property {string} app.name             - The ID of the application which has an app alert (the app is stopped on some number of running servers)
   *                                           It is of the form serverTuple|cluserName,appName.
   * @property {Array.<string>} app.servers  - The list of server IDs on which the application is stopped but the server is running
   */
  return declare('Alerts', [ObservableResource], {
    /** Hard-code the id and type to be 'servers' **/
    /** @type {string} */ id: ID.getAlerts(),
    /** @type {string} */ type: 'alerts',

    /** Set during construction and handleChangeEvent **/
    /** @type {number} */ count: 0,
    /** @type {Array.<Object>} */ unknown: [],
    /** @type {Array.<Object>} */ app: [],

    /**
     * Construct the initial Alerts state.
     * 
     * @param {Object} init The initialization object
     */
    constructor: function(init) {
      if (!init) { this._throwError('Alerts created without an initialization object'); }
      (init.count >= 0)  ? this.count     = init.count    : this._throwError('Alerts created without a count');
      (init.unknown)     ? this.unknown   = init.unknown  : this._throwError('Alerts created without an unknown list');
      (init.app)         ? this.app       = init.app      : this._throwError('Alerts created without an app list');
    },

    /**
     * Handle a change event for this resource.
     * 
     * The event can have the following properties:
     * Alerts Event {
     *   type: 'alerts',
     *   count,   (required)
     *   unknown, (required)
     *   app      (required)
     * }
     * 
     * If the inbound event has the wrong type, the event is ignored. This does not provide any sort of security,
     * but does guard against programming errors and will make the code more serviceable / robust.
     * 
     * The following observer methods may be called:
     * -- onAlertsChange(newAlerts, oldAlerts) - parameters are Strings
     * 
     * @param {Object} e The received Alerts Event object
     */
    _handleChangeEvent: function(e) {
      console.log('Alerts got an event', e);

      if (e.type !== this.type) { 
        console.error('Alerts got an event which did not have the correct type. The event will be ignored. Received event type: ' + e.type);
        return;
      }
      
      var prev = {
          count: this.count,
          unknown: this.unknown,
          app: this.app
          };
      this.count = e.count;
      this.unknown = e.unknown;
      this.app = e.app;

      this._notifyObservers('onAlertsChange', [this, prev]);
    }

  });

});
