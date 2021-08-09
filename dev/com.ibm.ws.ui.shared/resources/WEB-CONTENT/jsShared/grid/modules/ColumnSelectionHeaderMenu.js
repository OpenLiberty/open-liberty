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
define('jsShared/grid/modules/ColumnSelectionHeaderMenu', [                                                           
  'dojo/_base/array',                                                         
  'dojo/_base/declare',
  'dojo/_base/event',
  'dojo/_base/lang',
  'dijit/registry',
  'dijit/Menu',
  'dijit/MenuItem',
  'dojo/dom-construct',
  'dojo/dom-class',
  'dojo/keys',
  'dojo/query',
  'jsShared/grid/GridColumnSelectionButton',
  'jsShared/grid/modules/HiddenColumns',
  'gridx/core/_Module',
  'gridx/modules/HeaderRegions'
], function(array, declare, event, lang, registry, Menu, MenuItem, domConstruct, domClass, keys, query,
            GridColumnSelectionButton, HiddenColumns, _Module, HeaderRegions){

/**
 * Summary:
 *  module name: ColumnSelectionHeaderMenu
 *  Adds a jsShared/grid/GridColumnSelectionButton on a header cell.
 *  
 * Description:
 *  Adds a GridColumnSelectionButton, a dropdown menu button which opens a menu  
 *  containing the names of columns which can be hidden in the grid
 *  (see jsShared/grid/GridColumnSelectionButton), to the column which has
 *  "columnSelectionMenu" set to true in it's structure definition.
 *  
 *  If the grid is also defined with the GridStatePersist module, the hidden
 *  column selection will be persisted and restored the next time a grid of the
 *  same type is created.
 */  

  return declare(_Module, {
   
    name: 'columnSelectionHeaderMenu',

    forced: ['headerRegions', 'hiddenColumns'],
    
    preload: function() {
      var t = this,
          g = t.grid;
      
      // Check if the hidden column data was persisted before you create the column 
      // selection header menu.   For each column that was hidden, set the column's 
      // hide attribute to true in the column structure of the grid so the menu is
      // created with the field marked as hidden (unchecked).
      if (g.gridStatePersist) {
        var cachedHiddenIds = g.gridStatePersist.registerAndLoad('hideableColumnInfo', function() {
          return t._get();             
        });
        // g.gridStatePersist() returns null if nothing was cached; returns an array of column ids
        // that were hidden if the grid data was persisted
        
        if (cachedHiddenIds) {
          for (var i = 0; i < g.structure.length; i++) {
            // If this is a hideable column that was selected to be hidden and 
            // cached as so, indicate to hide it again in the column structure.
            if ((cachedHiddenIds.indexOf(g.structure[i].id) > -1) && 
                g.structure[i].hideable) {
              g.structure[i].hide = true;
            } else {
              g.structure[i].hide = false;
            }
          }
        }
      }
      
      // Add a region containing the gear to the header.
      g.headerRegions.add(function(col){      
        var menu = col.columnSelectionMenu;   // Find the column that wants the gear in the header.
        if (menu) {
          // Create the gear dropDownButton, saving the column ID where it is shown on the grid header.
          t.columnSelectionButton = t._createColumnSelectionButtonForHeader(col.id);
          
          t.grid.on("headerCellKeyDown", lang.hitch(t.grid, function(evt) {
            if (evt.keyCode === keys.ENTER || evt.keyCode === keys.SPACE) {
              // Validate that grid has the columnSelectionButton...
              var columnSelectionButton = this.columnSelectionHeaderMenu.columnSelectionButton;
              if (columnSelectionButton) {        
                if (evt.columnId === columnSelectionButton.columnId) {
                  // Event was for the column containing the columnSelectionButton.
                  // Make the column selection menu appear and set focus to the first menu item.
                  columnSelectionButton.toggleDropDown();
                  var dd = columnSelectionButton.dropDown;  // drop down may not exist until 
                                                            // after toggleDropDown()
                  if(dd && dd.focus){
                    this.defer(lang.hitch(dd, "focus"), 1);
                  }
                }  
              }
              evt.stopPropagation();
            }
          }));
          

          return t.columnSelectionButton.domNode;
        }        
      }, 0, 1, 0); 
      
      // This is part of the fix for 230930.
      // The flow goes that the user selects a column from the column
      // selection menu to hide or show.  headerRegions._focusRegion
      // is invoked to reset focus to the gear button in the header.
      // The code below assigns the headerCell, then the Timeout
      // occurs.  During the timeout, the actual column selected is
      // added to/removed from the header...and dojo does this by
      // recreating all the <td> elements in the header.  
      // Subsequently the code in the Timeout block executes, but 
      // the <td> element pointed to by headerCell is now stale...
      // a new <td> element was created for the gear icon.
      // So, I rewrote the code below to reset the headerCell variable
      // to point to the active <td> element within the Timeout block.
      g.headerRegions._focusRegion = function(region, isByArrowkey) {
        var t = this;
        var g = t.grid;
        
        if (region && !t._lock) {
          //focus fires onFocus, which triggers _focusRegion recursively.
          //Add a lock to avoid recursion.
          t._lock = 1;
          var header = g.header.domNode,
              headerCell = query(region).closest('.gridxCell', header)[0];
          t._curRegionIdx = array.indexOf(t._regionNodes, region);
          try {
            region.focus();
          } catch(e) {
            //In IE if region is hidden, this line will throw error.
          }
          setTimeout(function() {
            query('.gridxHeaderRegionFocus', header).removeClass('gridxHeaderRegionFocus');
            
            /****  Reset stale reference to the header cell  ***/
            headerCell = query(region).closest('.gridxCell', header)[0];
            
            domClass.add(headerCell, 'gridxHeaderRegionFocus');
            domClass.add(header, 'gridxHeaderFocus');
            //make it asnyc so that IE will not lose focus
            //firefox and ie will lose focus when region is invisible, focus it again.
            region.focus();
            if(g.hScroller) {
              g.hScroller.scrollToColumn(headerCell.getAttribute('colid'));
            }
            t._lock = 0;
          }, 0);
        }
      };

    },
    
    load: function(args, deferStartup) {
      var t = this,
          g = t.grid;
      
      t._hideDefaultColumns(g);
      
      // After we have hidden the default columns for this grid, then any subsequent
      // column addition or removal could only happen using the Column Selection 
      // Header Menu.   Therefore, refocus the gear icon upon completion.
      t.aspect(g.hiddenColumns, 'remove', function() {
        query('.gridxHeaderCellFocus', header).removeClass('gridxHeaderCellFocus');        
        
        // We need to re-establish focus on the header node containing the 
        // column selection button.
        // Since a column was added back into the grid, the grid's headerRegions 
        // array has changed (added one in) and the _curRegionIdx is no longer
        // valid. 
        // Find the headerRegion node containing the column selection gear 
        // button and set the current header region index (_curRegionIdx) to 
        // point to this node.
        var header = g.header.domNode;
        var target = t.columnSelectionButton.domNode;
        var i = array.indexOf(g.headerRegions._regionNodes, target);
    
        if (i>=0) {
          // Set the header cell for this header region (the <td> node)
          query('.gridxHeaderRegionFocus', header).removeClass('gridxHeaderRegionFocus');
    
          var headerCell = query(target).closest('.gridxCell', header)[0];
          domClass.add(headerCell, 'gridxHeaderRegionFocus');
    
          g.headerRegions._curRegionIdx = i;
        }
      });
  
      t.aspect(g.hiddenColumns, 'add', function() {
        query('.gridxHeaderCellFocus', header).removeClass('gridxHeaderCellFocus');
        
        // We need to re-establish focus on the header node containing the 
        // column selection button.
        // Since a column was removed from the grid, the grid headerRegions 
        // array has changed (one less) and the _curRegionIdx is no longer
        // valid. 
        // Find the headerRegion node containing the column selection gear 
        // button and set the current header region index (_curRegionIdx) to 
        // point to this node.
        var header = g.header.domNode;
        var target = t.columnSelectionButton.domNode;
        var i = array.indexOf(g.headerRegions._regionNodes, target);
    
        if (i>=0) {
          // Set the header cell for this header region (the <td> node)
          query('.gridxHeaderRegionFocus', header).removeClass('gridxHeaderRegionFocus');
    
          var headerCell = query(target).closest('.gridxCell', header)[0];
          domClass.add(headerCell, 'gridxHeaderRegionFocus');
    
          g.headerRegions._curRegionIdx = i;
        }
        
      });

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
      
      return new GridColumnSelectionButton({grid: g, columnId: colId});
    },
    
    /** 
     *  Returns an array of column id's identifying those columns which are hidden in the grid.
	   *
	   *  This method returns the object that is persisted when the grid is defined with 
     *  the GridStatePersist module.
     */
    _get: function() {
      var t = this,
          g = t.grid,
          hiddenCols = g.hiddenColumns._cols || [],
          hiddenList = [];  
      
     
      for (var i=0; i < hiddenCols.length; ++i) {
        if(hiddenCols[i].hidden) {
          hiddenList.push(hiddenCols[i].id);
        }
      }
      
      return hiddenList;      
    },
    
    /** 
     * This method sets the default hidden columns for the grid.  
     * 
     * Two new properties were added for each column in the grid structure:
     * hideable - true indicates this column can be selected/unselected to be hidden;
     *            false indicates the column is ALWAYS displayed on the grid.           
     * hide - true indicates the column should be initially hidden. 
     */
    _hideDefaultColumns : function(grid) {
      // set initial hidden columns
      for (var i = 0; i < grid.structure.length; i++) {
        // if a hideable column and 'hide' is initialized to true, hide the column
        if (grid.structure[i].hideable && grid.structure[i].hide) {
          grid.hiddenColumns.add(grid.structure[i].id);
        }
      }
      
      if (grid.gridStatePersist) {             // If we are persisting the grid state   
        grid.gridStatePersist.save();          // save off the current hidden columns.    
      }
    }
    
  });
});
