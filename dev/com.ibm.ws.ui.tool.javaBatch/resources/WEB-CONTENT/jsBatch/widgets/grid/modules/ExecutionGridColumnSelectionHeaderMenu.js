/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/* jshint strict: false */
define('jsBatch/widgets/grid/modules/ExecutionGridColumnSelectionHeaderMenu', [
  'dojo/_base/declare',
  'jsBatch/utils/ID',
  'jsBatch/widgets/grid/ExecutionGridColumnSelectionButton',
  'jsShared/grid/modules/ColumnSelectionHeaderMenu'
], function(declare, ID, ExecutionGridColumnSelectionButton, ColumnSelectionHeaderMenu){

  /**
   * Summary:
   *  module name: ColumnSelectionHeaderMenu
   *  Adds a jsBatch/widgets/grid/ExecutionGridColumnSelectionButton on a header cell.
   *
   * This is an extension of jsShared/grid/modules/ColumnSelectionHeaderMenu that will work
   * with the special need the Execution Grids have to work all together when a column
   * is selected to be displayed or hidden from the column selection menu (gear icon).
   *
   * This module differs from the jsShared/grid/modules/ColumnSelectionHeaderMenu because
   * it creates an ExecutionGridColumnSelectionButton instead of the normal
   * jsShared gridColumnSelectionButton.  The ExecutionGridColumnSelectionButton will
   * create only ONE column selection menu and it will be shared amongst all
   * Execution Grids. When a column is selected/deselected from the menu, the columns
   * shown in ALL Execution Grids will be updated to reflect the column change.
   * 
   * 3Q17 migration update:  Prior to 3Q17, the 'Server' column originally displayed in 
   * the executions grids was a tuple value.  For 3Q17, it was split into 3 new columns:
   * 'Host', 'User Directory', and Server Name.  We also had to add migration code to the 
   * preload method for this grid module so that if the 'Server' column had been 
   * persisted as shown we would now show these three columns instead.
   */

  return declare(ColumnSelectionHeaderMenu, {

    name: 'columnSelectionHeaderMenu',
    one: true,
    
    preload: function() {
      var t = this,
      g = t.grid;

      this.inherited(arguments);

      if (g.gridStatePersist) {
        // MIGRATION code: Added 3Q17 to expand the original 'Server' column into 
        // 3 new columns: Host, User Directory, and Server Name.
        // If persisted data indicates that 'Server' column had been shown, then
        // set the 3 new columns to also be shown.  Otherwise they will default to
        // hidden.
        var cachedHiddenIds = g.gridStatePersist.registerAndLoad('hideableColumnInfo', function() {
          return t._get();             
        });
        // g.gridStatePersist() returns null if nothing was cached; returns an array of column ids
        // that were hidden if the grid data was persisted

        if (cachedHiddenIds) {
          if (cachedHiddenIds.indexOf(ID.SERVER) > -1) {
            // Server was a hidden column.   Hide the user dir and host columns also.
            // Let the server name column be as it was since that column already existed.
            for (var i = 0; i < g.structure.length; i++) {
              if (g.structure[i].id === ID.USER_DIR  || 
                  g.structure[i].id === ID.HOST) {
                  g.structure[i].hide = true;
              }
            } 
          }
          // The new set of hidden columns will be saved off as hidden as part of 
          // load processing for this module (see ColumnSleectionHeaderMenu.load())
        } 
        
        var displayedColIds = g.gridStatePersist.registerAndLoad('column', 
                                                     g.gridStatePersist._columnStateSaver, 
                                                     g.gridStatePersist);
        if (displayedColIds) {
          for (var j = 0; j < displayedColIds.length; j++) {
            if (displayedColIds[j].id === ID.SERVER) {
                // Server was a displayed column.   Now display all 3 new columns.
              for (var x = 0; x < g.structure.length; x++) {
                if (g.structure[x].id === ID.USER_DIR  || 
                    g.structure[x].id === ID.HOST ||
                    g.structure[x].id === ID.SERVER_NAME) {
                    g.structure[x].hide = false;
                }
              }                     
            }
          }
        }
      }
    
   },

   /**
    * Create the column selection button (the gear) that is placed in the column
    * header specified.
    *
    * colId - String - the ID of the column that will get the column selection button
    *         (gear) icon.
    */
   _createColumnSelectionButtonForHeader: function(colId) {
      var g = this.grid;

      return new ExecutionGridColumnSelectionButton({grid: g, columnId: colId});
   }

  });
});
