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
define([ 'dojo/_base/declare', '../ObservableResource'  ],
    function(declare, ObservableResource) {

  /**
   * StatelessResource represents a resource or object in the collective that has a name but no state.
   * The most direct example of this is a host. All resources must have a display name. A StatelessResource
   * may also have a set of alerts which are applicable to it. If provided during initialization, these
   * alerts are stored. 
   * 
   * The cumulative public attributes for StatelessResource are as follows:
   * @class
   * @typedef {Object} StatelessResource
   * @property {string} id                    - The resource's unique ID within the set of same type
   * @property {string} type                  - The resource's type
   * @property {string} name                  - The resource's display name, which typically defaults to the resource's id
   * @property {string} note                  - The notes associated to the resource
   * @property {string} owner                 - The owner associated to the resource
   * @property {Array.<string>} contacts      - The list of contacts associated to the resource
   * @property {Array.<string>} tags          - The list of tags associated to the resource
   * @property {Alerts} alerts                - The set of alerts that apply to the resource
   */
  return declare('StatelessResource', [ObservableResource], {
    /** Attributes required to be set by creator **/
    /** @type {string} */ name: null,
    /** @type {string} */ note: null,
    /** @type {string} */ owner: null,
    /** @type {Array.<string>} */ contacts: null,
    /** @type {Array.<string>} */ tags: null,
    /** @type {Alerts} */ alerts: null,

    /**
     * Constructor - validates that the minimal amount of data was injected and that
     * all of the necessary fields have been either injected or overridden.
     * 
     * @param {Object} init The initialization object
     */
    constructor: function(init) {
      // ID check is done by the parent object ObservableResource
      if (!this.name) {
        if (!init) this._throwError('StatelessResource created without an initialization object');
        // If we don't have a name, pick a reasonable default (the id!)
        (init.name) ? this.name = init.name : this.name = this.id; 
      }
      if (init.note) { this.note = init.note; }
      if (init.owner) { this.owner = init.owner; }
      if (Array.isArray(init.contacts)) { this.contacts = init.contacts; }
      if (Array.isArray(init.tags)) { this.tags = init.tags; }
      if (init.alerts) { this.alerts = init.alerts; }
    }

  });
});
