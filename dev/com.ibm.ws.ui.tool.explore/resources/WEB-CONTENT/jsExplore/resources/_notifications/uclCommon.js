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
/**
 * Common methods for unifiedChangeListener.js and its associated modules.
 */
define([ ], function() {

  return {

    /**
     * Find the element in the list with the matching name in the list.
     * 
     * @return The matched element, or undefined if no match was present
     */
    findObjectInList: function(list, ele) {
      for (var i = 0; i < list.length; i++) {
        // Not === because we want to match String primitives with String objects too
        if (list[i] == ele) {
          return list[i];
        };
      };
    },

    /**
     * Find an object with the matching name in the list.
     * 
     * @return The matched element, or undefined if no match was present
     */
    findNamedObjectInList: function(list, name) {
      for (var i = 0; i < list.length; i++) {
        if (list[i].name === name) {
          return list[i];
        }
      }
    },

    /**
     * Find an object with the matching id in the list of objects.
     * 
     * @param {Array.<Object>} list An array of objects, each element having an id property.
     * @param {String} id           The id to match in the list.
     * 
     * @return The matched element, or undefined if no match was present
     */
    findObjectInListById: function(list, id) {
      for (var i = 0; i < list.length; i++) {
        if (list[i].id === id) {
          return list[i];
        }
      }
    },

    /**
     * Compares two lists and if they differ, returns an Object with added and removed fields.
     * An element will be considered to be 'added' if it is present in the nowList but not in the
     * cachedList. An element will be considered to be 'removed' if it is present in the cachedList
     * but not in the nowList.
     * 
     * @param {Object} cachedList The cached list (may include removed elements)
     * @param {Object} nowList The now list (may include added elements)
     * @return {Object} Returns an Object with an added and removed field or null if the array contents are the same
     */
    compareTwoLists: function(cachedList, nowList) {
      var addedRemoved = { added: [], removed: [] };

      // First, find things that were 'added'
      for (var i = 0; i < nowList.length; i++) {
        var ele = nowList[i];
        if (!this.findObjectInList(cachedList, ele)) {
          addedRemoved.added.push(ele);
        }
      }

      // Second, find things that were 'removed'
      for (var i = 0; i < cachedList.length; i++) {
        var ele = cachedList[i];
        if (!this.findObjectInList(nowList, ele)) {
          addedRemoved.removed.push(ele);
        }
      }

      return (addedRemoved.added.length > 0 || addedRemoved.removed.length > 0 ? addedRemoved : null);
    },

    /**
     * Compares two lists of objects where each element has an ID property. If the id values in the 
     * lists differ, returns an Object with added and removed elements.
     * An element will be considered to be 'added' if it is present in the nowList but not in the
     * cachedList. An element will be considered to be 'removed' if it is present in the cachedList
     * but not in the nowList.
     * 
     * @param {Array.<Object>} cachedList The cached list (may include removed elements)
     * @param {Array.<Object>} nowList    The now list (may include added elements)
     * @return {Object} Returns an Object with list of Ids that were added and removed
     *                  or null if the IDs of the elements in the arrays of objects are the same.
     */
    compareTwoListsById: function(cachedList, nowList) {
      var addedRemoved = { added: [], removed: [] };

      // First, find things that were 'added'
      for (var i = 0; i < nowList.length; i++) {
        var eleId = nowList[i].id;
        if (!this.findObjectInListById(cachedList, eleId)) {
          addedRemoved.added.push(nowList[i]);
        }
      }

      // Second, find things that were 'removed'
      for (var i = 0; i < cachedList.length; i++) {
        var eleId = cachedList[i].id;
        if (!this.findObjectInListById(nowList, eleId)) {
          addedRemoved.removed.push(cachedList[i]);
        }
      }

      return (addedRemoved.added.length > 0 || addedRemoved.removed.length > 0 ? addedRemoved : null);
    },

    /**
     * Compares the metadata between the cached and the current REST API values.
     * Any changes that are detected will be stored in the changes parameter.
     * 
     * @param {Object} cached The object with metadata which was cached
     * @param {Object} now The object with metadata from the current REST API request
     * @param {Object} changes The object which stores the changes
     * @return {boolean} Returns true if changes were detected, or false if there were no changes
     */
    compareMetadata: function(cached, now, changes) {
      // Need to do a little pre-processing here
      // This ensures that when the cached object has metadata, but the now object does not
      // that there is an explicit field in the now object, so that the change event will force the field to clear
      if (cached.tags && !now.tags) { now.tags = null; }
      if (cached.owner && !now.owner) { now.owner = null; }
      if (cached.contacts && !now.contacts) { now.contacts = null; }
      if (cached.note && !now.note) { now.note = null; }
      // End pre-processing
      
      var hasChanged = false;
      if (now.tags) {
        if (cached.tags) {
          if (now.tags.length != cached.tags.length) {
            hasChanged = true;
            changes.tags = now.tags;
          } else {
            // same length so need to compare tags
            for (var i=0; i < now.tags.length; i++) {
              if (now.tags[i] !== cached.tags[i]) {
                hasChanged = true;
                changes.tags = now.tags;
                break;
              }
            }
          }
        } else {
          hasChanged = true;
          changes.tags = now.tags;
        }
      } else if (cached.tags) {
        hasChanged = true;
        changes.tags = null;
      }

      // We only support one note
      if (now.note) {
        if (cached.note) {
          if (now.note != cached.note) {
            hasChanged = true;
            changes.note = now.note;
          } 
        } else {
          hasChanged = true;
          changes.note = now.note;
        }
      } else if (cached.note) {
        hasChanged = true;
        changes.note = null;
      }

      // We only support one owner
      if (now.owner) {
        if (cached.owner) {
          if (now.owner != cached.owner) {
            hasChanged = true;
            changes.owner = now.owner;
          } 
        } else {
          hasChanged = true;
          changes.owner = now.owner;
        }
      } else if (cached.owner) {
        hasChanged = true;
        changes.owner = null;
      }

      if (now.contacts) {
        if (cached.contacts) {
          if (now.contacts.length != cached.contacts.length) {
            hasChanged = true;
            changes.contacts = now.contacts;
          } else {
            // same length so need to compare tags
            for (var i=0; i < now.contacts.length; i++) {
              if (now.contacts[i] !== cached.contacts[i]) {
                hasChanged = true;
                changes.contacts = now.contacts;
                break;
              }
            }
          }
        } else {
          hasChanged = true;
          changes.contacts = now.contacts;
        }
      } else if (cached.contacts) {
        hasChanged = true;
        changes.contacts = null;
      }
      return hasChanged;
    },

    /**
     * Compares the alerts between the cached and the current REST API values.
     * Any changes that are detected will be stored in the changes parameter.
     * 
     * @param {Object} cached The object with alerts which was cached
     * @param {Object} now The object with alerts from the current REST API request
     * @param {Object} changes The object which stores the changes
     * @return {boolean} Returns true if changes were detected, or false if there were no changes
     */
    compareAlerts: function(cached, now, changes) {
      var hasChanged = false;

      // An alerts object is present in both the cache and current value, need to compare
      if (cached.alerts.count != now.alerts.count) {
        hasChanged = true;
      }

      if (!hasChanged) {
        // Only check if need to, if we know we've hasChanged no work is needed
        if (JSON.stringify(cached.alerts.unknown) !== JSON.stringify(now.alerts.unknown) ){
          hasChanged = true;
        }
      }

      if (!hasChanged) {
        // Only check if need to, if we know we've hasChanged no work is needed
        if (JSON.stringify(cached.alerts.app) !== JSON.stringify(now.alerts.app) ){ 
          hasChanged = true;
        }
      }

      if (hasChanged) {
        changes.alerts = now.alerts;
      }

      return hasChanged;
    }

  };

});
