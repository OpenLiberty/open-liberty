/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
define("jsShared/grid/modules/GridStatePersist", [
  "dojo/topic",
  "jsShared/utils/toolData",
  "jsShared/utils/userConfig",
  "dojo/_base/declare",
  "dojo/_base/array",
  "dojo/_base/lang",
  "dojo/json",  
  "dojo/cookie",
  "gridx/core/_Module",
  "dojo/Deferred"
], function(topic, toolData, userConfig, declare, array, lang, json, cookie, _Module, Deferred){

/**
 * Summary:
 *   module name: gridStatePersist
 *   
 *   Provide a mechanism to persist various other grid features enabled for gridStatePersist.
 *   This currently only includes the 
 *          jsShared/grid/modules/ColumnSelectionHeaderMenu
 *          gridx/modules/ColumnResizer  (done in this module)
 *   ****NOTE: Created our own Persist mechanism because the gridx Persist module fell short
 *             in a couple of areas:
 *             1) It persisted data only when the grid was destroyed, but we needed to 
 *                persist data for the column selection mechanism each and every time a 
 *                column was selected to be hidden or viewed. That way when a new instance
 *                of the same grid type was created on a page the information on which 
 *                columns to view was available (as with the Java Batch executionGrid which
 *                has multiple instances on one page).
 *             2) It persisted the column index value which affected where columns previously
 *                hidden were restored.  We did not want the original order of the columns
 *                to be affected.
 *
 *   The grid state data is stored using the persist API defined by
 *            "/ibm/api/adminCenter/v1/tooldata" (see jsShared/utils/tooldata.js),
 *   which is wrappered by jsShared/utils/userConfig.js.
 *  
 * Feature Parameters:
 *  enabled: Boolean     Whether this module is enabled
 *  key: String          This is the persistence key of this grid. If not provided, the grid id
 *                       is used as the key. This property is essential when a grid with a 
 *                       different id wants to load from this storage.
 *                       
 */                       

  return declare(_Module, {
    name: 'gridStatePersist',
  
    enabled: true,
    key: '',
    persistedData: null,
  
    constructor: function(grid) {      
      
      // grid: Object   The grid itself
      var t = this;
      
      // Initialize arguments
      t.arg('key', grid.id, function(arg){
        return arg;
      });
      
      if(t.arg('persistedData') && t.arg('persistedData')[t.key]){
        persistedData = t.arg('persistedData')[t.key];
      }

      t._persistedList = {};         // Features register themselves with this module
                                     // along with an associated method that will 
                                     // return an object to be persisted.  This 
                                     // list tracks the registered features.

      // Column width register/restore for the columnResizer module.
      t._restoreColumnState();
    },
  
    /**
     * Invoked when saving things using the "/ibm/api/adminCenter/v1/tooldata" API.
     * 
     * @param key       The persist key of this grid
     * @param value     An object containing everything we want to persist for this grid
     */
    put: function(key, list) {
      var contents = {};
      var t = this;
      
      for (name in list) {
        feature = list[name];
        if (feature.enabled) {
          // For each enabled feature, invoke it's 'Saver' method which
          // will return an object to persist.
          contents[name] = feature.saver.call(feature.scope || lang.global);
        }
      }
      
      userConfig.save(key, contents);
    },

    /**
     * Invoked when loading persisted things using the "/ibm/api/adminCenter/v1/tooldata" API.
     *
     * @param t         This gridStatePersist object
     * @param key       The persist key of this grid
     * @returns         The JSON object that was persisted for this grid
     */
    get: function(t, key) {      
       return (t.persistedData && t.persistedData[key]) ? t.persistedData[key] : null;
    },
  
    /**
     * Registers a feature to be persisted, and then loads (returns) its contents.
     * 
     * @param name      A unique name of the feature to be persisted.
     * @param saver     A function to be called when persisting the grid.
     * @param scope     
     * @returns         The loaded contents of the given feature.
     */
    registerAndLoad: function(name, saver, scope) {
      
      this._persistedList[name] = {
        saver: saver,
        scope: scope,
        enabled: true
      };
      
      var get = this.arg('get'),
      content = '_content' in this ? this._content : (this._content = get(this, this.arg('key')));
  
      return content ? content[name] : null;
    },

    /**
     * Get the names of all the registered features whose data is persisted by this module.
     * These names can be used in enable(), disable(), or isEnabled() methods.
     * 
     * @returns {Array}  An array of persistable feature names. 
     */
    features: function() {
      var list = this._persistedList,
        features = [],
        name;
      
      for (name in list) {
        if (list.hasOwnProperty(name)) {
          features.push(name);
        }
      }
      return features;
    },
  
    /**
     * Enable persistance of the given feature when the save function is called. If name is
     * not provided (undefined or null), then enable all registered features.
     *  
     * @param name      Name of the feature
     */
    enable: function(name) {
      this._setEnable(name, 1);
    },
  
    /**
     * Disable persistance of the given feature.  The feature will NOT be persisted when the save
     * function is called. If name is not provided (undefined or null), then disable all 
     * registered features.
     * 
     * @param name      Name of the feature
     */
    disable: function(name) {
      this._setEnable(name, 0);
    },
  
    /**
     * Check whether or not a feature is enabled.
     * 
     * @param name      Name of the feature
     * 
     * @returns boolean
     */
    isEnabled: function(name) {
      var feature = this._persistedList[name];
      if(feature){
        return feature.enabled;
      }
      return name ? false : this.arg('enabled');
    },
  
    /**
     * Save all the enabled features.
     */
    save: function() {
      var t = this,
          put = t.arg('put');
      
      if (t.arg('enabled')) {
        var name,
           feature;
        
        // If we have persisted data stored, we should attempt to get it, incase it has been created since we initially found it didn't exist.
        var persistedDataEmpty = true;
        for(var key in this._persistedList){
          if(this._persistedList.hasOwnProperty(key)){
            persistedDataEmpty = false;
            break;
          }
        }
        
        // Get persisted data and update our copy
        if (persistedDataEmpty) {
          t.get(t, t.arg('key')).then(lang.hitch(this, function(){
            put(t.arg('key'), t._persistedList);
          }));
        }
        else{
          put(t.arg('key'), t._persistedList);
        }
      }  
    },
  
    _setEnable: function(name, enabled) {
      var list = this._persistedList;
      enabled = !!enabled;
      if (list[name]) {
        list[name].enabled = enabled;
      } else if (!name) {
        for (name in list) {
          list[name].enabled = enabled;
        }
        this.enabled = enabled;
      }
    },

    // ---------------------------------------------------------------------
    // For ColumnResizer state restore
    _restoreColumnState: function() {
      var t = this,
      grid = t.grid,
      colHash = {},
      columns = t.registerAndLoad('column', t._columnStateSaver, t);
  
      if(lang.isArray(columns)) {
        array.forEach(columns, function(c) {
          colHash[c.id] = c;
        });
        
        // Persist column width
        array.forEach(grid._columns, function(col) {
          var c = colHash[col.id];
          if(c && c.id == col.id) {
            col.declaredWidth = col.width = c.width;
          }
        });
    
        grid.setColumns(grid._columns);
      }
    },
    
    _columnStateSaver: function() {
      return array.map(this.grid._columns, function(c) {
        return {
          id: c.id,
          width: c.width
        };
      });
    }
    
  });
});
