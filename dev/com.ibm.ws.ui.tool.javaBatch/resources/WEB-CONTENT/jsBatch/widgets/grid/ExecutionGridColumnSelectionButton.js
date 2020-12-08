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
define('jsBatch/widgets/grid/ExecutionGridColumnSelectionButton',
    [ 'dojo/_base/declare',
      'dijit/registry',
      'jsBatch/utils/ID',
      'jsShared/grid/GridColumnSelectionButton'
    ],
 function(declare, registry, ID, GridColumnSelectionButton) {

  /**
   * Summary:
   *    This is an extension of jsShared/grid/GridColumnSelectionButton that will work
   *    with the special need the Execution Grids have to work all together when a column
   *    is selected to be displayed or hidden from the column selection menu (gear icon).
   *
   *    It will create a DropDownButton (the gear icon) for each Execution Grid and associate
   *    each with the SAME DropDownMenu containing Execution Grid columns which can be hidden.
   *    There is only ONE DropDownMenu created for all Execution Grids.  A selection from
   *    the DropDownMenu will trigger activity on all created ExecutionGrids to hide or show
   *    the column selected.
   *
   **/

  return declare(GridColumnSelectionButton, {

    executionGridMenuIdentifier: ID.EXECUTION_GRID,

    /**
     * Forces the ID of the columnSelectionMenu to be 'executionGrid-columnSelectionMenu'.
     * That way, only ONE menu is created for all Execution Grids and this menu is
     * assigned as the dropDown for all Execution Grids' column selection buttons (the
     * gear icon).
     */
    __createColumnSelectionMenu: function(id) {
      id = this.executionGridMenuIdentifier;

      var columnSelectionMenu = this.inherited(arguments);
      return columnSelectionMenu;
    },

    /**
     * Unique handling for the Column Selection Menu Items for the columns specific to
     * Execution Grids.
     *
     * This is the onChange method for a dijit/CheckedMenuItem.  It flags the column on
     * the table to be rendered or hidden.
     *
     * The DropDownMenu containing the CheckedMenuItems displays the names of the columns
     * of a table.   If the CheckedMenuItem is selected (checked) it appears on the table;
     * when unselected (unchecked), the column is hidden.
     *
     * When the user selects a menu item (checks it), this method gets a value of 'true'
     * and the column is removed from the list of hidden columns for the table so that it
     * is now displayed.
     * When the user unselects the menu item (unchecks it), this method gets a value of
     * 'false' and the column is added to the list of hidden columns for the table so that
     * it is now hidden.
     *
     * For the Execution Grids....each change in the columns selected to be displayed
     * must be made to all created Execution Grids so that they all show the same columns.
     *
     * @param checked - true: indicates that the column should be displayed and therefore it
     *                        is removed from the list of hidden columns for the table.
     *                  false: indicates that the column should be hidden and therefore it is
     *                        added to the list of hidden columns for the table.
     *
     */
    _onChange : function(checked) {

      var jobInstanceGrid = registry.byId(ID.JOBINSTANCE_GRID);  // jobInstanceGrid has a
                                                                 // pointer to all created
                                                                 // Execution Grids.

      // Loop through created Execution Grids
      var eGrids = Object.keys(jobInstanceGrid.executionGrids);
      for (var i = 0; i < eGrids.length; i++) {
        if (checked) {
          jobInstanceGrid.executionGrids[eGrids[i]].executionGrid.hiddenColumns.remove(this.value);
        } else {
          jobInstanceGrid.executionGrids[eGrids[i]].executionGrid.hiddenColumns.add(this.value);
        }
      }

      this.set('iconClass', this.checked ? 'gridColumnMenuItem_checked' : 'gridColumnMenuItem_unchecked');

      if (this.grid.gridStatePersist) {
        this.grid.gridStatePersist.save();
      }

    }

  });

});
