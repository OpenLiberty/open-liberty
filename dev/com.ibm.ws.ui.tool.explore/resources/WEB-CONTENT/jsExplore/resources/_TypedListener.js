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
define([ 'dojo/_base/declare', 'dojo/topic', './_topicUtil', 'js/common/tr' ],
    function(declare, topic, topicUtil, tr) {
  
  /**
   * The TypedListener is the parent class for all resources.
   * 
   * The TypedListener is an object which will subscribe itself to a topic.
   * The topic string is determined by topicUtil. It will subscribe to the topic
   * in response to the init() method being called, and it will unsubscribe from
   * the topic in response to it being destroyed.
   * 
   * A TypedListener establishes an event listener on the topic for the resource which it represents.
   * See jsExplore/resources/_topicUtil for details on the various topics.
   * 
   * The cumulative public attributes for a TypedListener are as follows:
   * @property {string} type - The resource's type
   * 
   */
  return declare('TypedListener', [], {
    /** Public attributes typically set by sub-class **/
    /** @type {string} */ type: null,

    /** Protected operations typically set by sub-class **/
    /** @type {function} */ _handleChangeEvent: null,

    /** Private attributes **/
    /** @type {string} */ __myTopic: null,
    /** @type {Object} */ __subHandle: null,

    /**
     * Declare destroy as a chained method. As per dojo doc:
     * https://dojotoolkit.org/reference-guide/1.10/dojo/_base/declare.html
     */
    '-chains-': {
      destroy: 'before'
    },

    /** Protected helper method to allow us to throw errors to tell ourselves we screwed up **/
    _throwError: function(msg) {
      tr.throwMsg(msg);
    },

    /**
     * Constructor - validates that the minimal amount of data was injected and that
     * all of the necessary fields have been either injected or overridden. Note that
     * we do not establish a listener during construction. The object must be .init()
     * in order to establish the listener.
     * 
     * @param {Object} init The initialization object
     */
    constructor: function(init) {
      if (!this.type || !this._handleChangeEvent) { // If this object already defines the type and _handleChangeEvent, it does not need to be injected
        // If we're not created with an explicit type...
        if (!init) this._throwError('TypedListener created without an initialization object');
        if (!this.type) { (init.type) ? this.type = init.type : this._throwError('TypedListener created without a type'); }
        if (!this._handleChangeEvent) { (init._handleChangeEvent) ? this._handleChangeEvent = init._handleChangeEvent : this._throwError('TypedListener created without a handleChangeEvent operation'); }
      }
    },

    /** 
     * Initialize the listener.
     * 
     * @return {_TypedListener} Returns this so the call can be chained. 
     */
    init: function() {
      // Establish a listener on myself to change my own state
      var me = this;
      this.__myTopic = topicUtil.getTopic(this);
      this.__subHandle = topic.subscribe(this.__myTopic, function(e) {
        me._handleChangeEvent(e);
      });
      return this;
    },

    /**
     * Destroy cleans up the object, most notably it removes the topic listener.
     */
    destroy: function() {
      if (this.__subHandle) {
        this.__subHandle.remove();
        this.__subHandle = null;
      }
    }
  });
});
