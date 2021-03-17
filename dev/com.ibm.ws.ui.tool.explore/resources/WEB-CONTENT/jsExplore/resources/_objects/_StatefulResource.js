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
define([ 'dojo/_base/declare', './_StatelessResource' ],
    function(declare, StatelessResource) {

  /**
   * StatefulResource represents a resource or object in the collective that has a state. The most
   * direct example of this is a server. All stateful resources must have a display name and a state.
   * 
   * A StatefulResource also exposes operations which allow the resource to be started, stopped or
   * restarted.
   * 
   * The cumulative public attributes for StatefulResource are as follows:
   * @class
   * @typedef {Object} StatefulResource
   * @property {string} id            - The resource's unique ID within the set of same type
   * @property {string} type          - The resource's type
   * @property {string} name          - The resource's display name, which typically defaults to the resource's id
   * @property {string} state         - The resource's state (started, stopped, etc)
   * @property {function} start       - The start operation for the resource
   * @property {function} stop        - The stop operation for the resource
   * @property {function} restart     - The restart operation for the resource
   * @property {boolean} transition  - The transition flag for recording the change from stopping to stopped or starting to started
   */
  return declare('StatefulResource', [StatelessResource], {
    /** Attributes required to be set by creator **/
    /** @type {string} */ state: null,

    /** Operations typically defaulted or set by sub-class **/
    /** @type {function} */ start: null,
    /** @type {function} */ stop: null,
    /** @type {function} */ restart: null,
    /** @type {boolean} */ transition: null,
    /**
     * Constructor - validates that the minimal amount of data was injected and that
     * all of the necessary fields have been either injected or overridden.
     * 
     * @param {Object} init The initialization object
     */
    constructor: function(configObj) {
      if (!configObj) this._throwError('StatefulResource created without a configuration object');
      (configObj.state) ? this.state = configObj.state : this._throwError('StatefulResource created without a state');
      if (!this.start) (configObj.start) ? this.start = configObj.start : this._throwError('StatefulResource created without a start operation');
      if (!this.stop) (configObj.stop) ? this.stop = configObj.stop : this._throwError('StatefulResource created without a stop operation');
      if (!this.restart) (configObj.restart) ? this.restart = configObj.restart : this._throwError('StatefulResource created without a restart operation');
      transition = false;
    }

  });
});
