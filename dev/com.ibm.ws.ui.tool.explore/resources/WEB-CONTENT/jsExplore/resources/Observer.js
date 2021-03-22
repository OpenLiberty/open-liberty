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
define(['dojo/_base/lang','dojo/_base/declare', 'js/common/tr'], function(lang, declare, tr) {

  /**
   * The Observer base class for widgets or resources. The Observer should be subscribed to
   * an ObservableResource.
   * 
   * Implementors of Observer should define methods to follow the pattern:
   * onAttributeChange(newValue, oldValue)
   * 
   * where Attribute is an attribute name like State or Cluster. The specific types of onChange methods
   * vary based on the resource being observed. No default implementations of these onChange methods are
   * provided.
   * 
   * @property {string} id    - The observer's ID. It does not need to be unique, but it should be.
   */
  return declare('Observer', [], {
    /** Public attributes typically set by sub-class **/
    /** @type {String} */ id: null,
    
    /**
     * Declare destroy as a chained method. As per dojo doc:
     * https://dojotoolkit.org/reference-guide/1.10/dojo/_base/declare.html
     */
    // ToDo: has to ask Mike about this. With this, it would cause a chain error during destroy.
//    '-chains-': {
//      destroy: 'before'
//    },

    /**
     * Construct the initial Observer state.
     * 
     * @param {Object} init The initialization object
     */
    constructor: function(init) {
      if (!this.id) { // If we already have an ID, then we're done. If not, it needs to be provided via the initialization object.
        if (!init) tr.throwMsg('Observer created without an initialization object');
        (init.id) ? this.id = init.id : tr.throwMsg('Observer created without an id');
      }
    }

  });

});