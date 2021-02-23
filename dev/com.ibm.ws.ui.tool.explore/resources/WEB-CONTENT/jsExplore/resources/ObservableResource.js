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
define([ 'dojo/_base/declare', 'js/common/tr', './_TypedListener' ], function(declare, tr, TypedListener) {

  /**
   * The ObservableResource is the parent class for all resources which implement the observable pattern.
   * An ObservableResource allows an Observer object to subscribe and unsubscribe to it. Any subscribed
   * Observer will be notified when the ObservableResource changes.
   * 
   * The ObservableResource / Observer pattern establishes a tight relationship between objects in
   * how they subscribe and unsubscribe from each other. The exact interaction for notifications is
   * loosely defined so that the ObservableResource objects can define their own notification methods
   * to be invoked.
   * 
   * The cumulative public attributes for an ObservableResource are as follows:
   * @property {string} id    - The resource's unique ID within the set of same type
   * @property {string} type  - The resource's type (inherited from TypedListener)
   */
  return declare('ObservableResource', [TypedListener], {
    /** Public attributes typically set by sub-class **/
    /** @type {string} */ id: null,

    /** Private attributes **/
    /** @type {Array.<Observer>} */ __observers: null, 

    /**
     * Constructor - each instance of the ObservableResource needs to have its own copy of observers.
     * Create the observers array during construction.
     * 
     * @param {Object} init The initialization object
     */
    constructor: function(init) {
      if (!this.id) { // If we already have an ID, then we're done. If not, it needs to be provided via the initialization object.
        if (!init) this._throwError('ObservableResource created without an initialization object');
        (init.id) ? this.id = init.id: this._throwError('ObservableResource created without an id');
      }
      this.__observers = []; // Create a new copy of the __observers array each time an ObservableResource is created
    },

    /**
     * Protected method to update an attribute and notify any observers.
     * If the attribute is not present in the Event object, no changes are made and no
     * Observers are notified.
     * 
     * The arguments to the notifyMethod are (newValue, oldValue).
     * 
     * @param {String} notifyMethod The method to invoke for any observers if this attribute has changed
     * @param {Object} e The event from which to find the attribute update
     * @param {String} attributeName The name of the attribute to update
     */
    _updateAttribute: function(notifyMethod, e, attributeName) {
      if (e.hasOwnProperty(attributeName)) {
        var prevValue = this[attributeName];
        this[attributeName] = e[attributeName];
        this._notifyObservers(notifyMethod, [this[attributeName], prevValue]);
      }
    },

    /**
     * Protected method to update a set of tallies and notify any observers.
     * 
     * The arguments to the notifyMethod are (newTally, oldTally) where the parameters are objects containing the respective value for the tally attributes.
     *
     * @param {String} notifyMethod The method to invoke for any observers
     * @param {Object} e The event from which to find the tally attribute updates
     * @param {Object} tallyObject The object which holds the tallies
     * @param {Array.<String>} tallyList The list of tally attribute names
     */
    _updateTally: function(notifyMethod, e, tallyObject, tallyList) {
      // Iterate through provided tally names. Store the previous value and update to new value from event
      var prevTally = {};
      var newTally = {};
      for (var i = 0; i < tallyList.length; i++) {
        var tally = tallyList[i]; // Get the name of the tally attribute
        prevTally[tally] = tallyObject[tally];
        tallyObject[tally] = e[tally];
        newTally[tally] = e[tally];
      }

      this._notifyObservers(notifyMethod, [newTally, prevTally]);
    },

    /**
     * Protected method to update an array with elements (simple data types only)
     * that have been added and/or removed and notify any observers.
     * 
     * The arguments to the notifyMethod are (newList, oldList, added, removed) where the 
	 * parameters are Arrays.
     * 
     * TODO: The way we're processing the array does not seem efficient.
     * For large lists, a better way would be to use object and remove keys
     * 
     * @param {String} notifyMethod The method to invoke for any observers
     * @param {Array.<SimpleDataType>} list The list to be modified
     * @param {Array.<SimpleDataType>} added The list of added entries
     * @param {Array.<SimpleDataType>} removed The list of removed entries
     * @param {Array.<SimpleDataType>} changed The list of changed entries (Optional)
     */
    _updateArray: function(notifyMethod, list, added, removed, changed) {
      if ((added && added.length > 0) || (removed && removed.length > 0) || (changed && changed.length > 0)) {
        var prevList = list.slice(0); // Make a shallow copy of the original list

        // Iterate through and remove things first, otherwise we iterate over things that were just added and thats a waste of time
        if (removed) {
          for (var i = (list.length - 1); i >= 0; i--) {
            if (removed.indexOf(list[i]) > -1) {
              list.splice(i, 1);
            }
          }
        }
        if (added) {
          Array.prototype.push.apply(list, added);
        }

        this._notifyObservers(notifyMethod, [list, prevList, added, removed, changed]);
      }
    },

    /**
     * Protected method to update an array of JSON data with elements that have been added 
     * and/or removed and notify any observers. Each element in the array must have an
     * 'id' property.
     * 
     * The arguments to the notifyMethod are (newList, oldList, added, removed) where the 
     * parameters are Arrays of JSON objects.
     * 
     * @param {String} notifyMethod The method to invoke for any observers
     * @param {Array.<JSONObject>} list The list to be modified
     * @param {Array.<JSONObject>} added The list of added entries
     * @param {Array.<JSONObject>} removed The list of removed entries
     * @param {Array.<JSONObject>} changed The list of changed entries (Optional)
     */
    _updateArrayOfObjects: function(notifyMethod, list, added, removed, changed) {
      if ((added && added.length > 0) || (removed && removed.length > 0) || (changed && changed.length > 0)) {
        var prevList = list.length > 0 ? JSON.parse(JSON.stringify(list)) : list;  // Make a copy of the original list
        
        // Iterate through and remove things first, otherwise we iterate over things that were just added and thats a waste of time
        if (removed && removed.length > 0) {
          for (var i = (list.length - 1); i >= 0; i--) {
            if (this._indexOfId(removed, list[i].id) > -1) {
              list.splice(i, 1);
            }
          }
        }
        if (added && added.length > 0) {
// SHOULD I MAKE A COPY OF THE ADDED ELEMENT PRIOR TO ADDING IT TO THE LIST? var toAdd = JSON.parse(JSON.stringify(added[i]));          
          Array.prototype.push.apply(list, added);
        }

        this._notifyObservers(notifyMethod, [list, prevList, added, removed, changed]);
      }
    },
    
    /**
     * Helper method to return the index of the element in an array of objects with the given Id.
     * 
     * @param {Array.<object>} list  The list to search.  Each element of the array 
     *                               should have an id property.
     * @param {String} id            ID to search for.
     * 
     * @return {integer} index       Index of element in the array with the matching id,
     *                               or -1 if no match is found.
     */
    _indexOfId: function(list, id) {
      for (var i=0; i<list.length; i++) {
         if (list[i].id==id) return i;
      }
      return -1;
    },

    /**
     * Subscribes an Observer to this ObservableResource.
     * 
     * @param {Observer} observer An instance of Observable
     */
    subscribe: function(observer) {         
      this.__observers.push(observer);           
    },

    /**
     * Unsubscribes an Observer from this Observable.
     * If the resource is not subscribed, no change is made.
     * 
     * @param {Observer} observer An instance of Observable
     */
    unsubscribe: function(observer) {
      var index = this.__observers.indexOf(observer);
      if (index !== -1) {
        this.__observers.splice(index, 1);
      }    
    },

    /**
     * Method to drive the notification of the registered Observers that this resource has changed.
     * It is the responsibility of the object extending ObservableResource to drive this method,
     * and to indicate what method on the Observers should be called.
     * 
     * @param {string} method The method to call in response to the attribute change
     * @param {Array.<Object>} args The arguments to pass through to the observers
     */
    _notifyObservers: function(method, args) {
      var thisId = this.id;
      var observers = this.__observers.slice(0); // Hold a reference to the current list of observers since things can remove themselves in response to a change
      observers.forEach(function(observer) {
        if (observer[method]) {
          try {
            tr.debug('Notifying observer ' + observer.id + ' of observable ' + thisId + ' for method ' + method);
            observer[method].apply(observer, args);
          } catch(e) {
            tr.ffdc('Error occurred while notifying observer ' + observer.id + ' of observable ' + thisId + ' for method ' + method, e, method, args);
          }
        }
      });
    }
  });

});
