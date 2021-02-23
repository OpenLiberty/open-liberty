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
define("jsShared/grid/ColumnSelectionChanger", [
  "dojo/_base/declare",
  "dojo/_base/lang",
  "dijit/_WidgetBase",
  "dijit/_FocusMixin",
  "dijit/_TemplatedMixin",
  "dijit/CheckedMenuItem",
  "dijit/DropDownMenu", 
  "dijit/registry",
  "jsShared/grid/GridColumnSelectionButton"
], function(declare, lang, _WidgetBase, _FocusMixin, _TemplatedMixin, 
            CheckedMenuItem, DropDownMenu, registry, 
            GridColumnSelectionButton){
'use strict';

/**
 * Summary:
 *    This grid Bar plugin creates a DropDownButton associated with the columns of
 *    a grid.  When selected, a DropDownMenu of columns which can be hidden are displayed. 
 *    If the checkbox associated with each column name in the DropDownMenu is selected,
 *    the column is displayed.  If un-selected, the column is hidden.  The button should
 *    be used in conjunction with the gridx/Bar module and will place the button on a gridx 
 *    Bar.
 *    
 *    The DropDownMenu created has one menu item per column defined in the grid's
 *    structure.  The structure should include the following parameters for each 
 *    column:
 *      hide   - true indicates the column should be hidden on the grid. 
 *      hideable - true indicates this column can be selected/unselected to be hidden
 *               and should therefore appear in the menu;
 *               false or missing indicates the column is ALWAYS on the grid and 
 *               will not appear in the DropDownMenu as a choice.            
 *    
 **/
  return declare([_WidgetBase, _FocusMixin, _TemplatedMixin], {
    templateString: '<div></div>',

    grid: null,
    
    buttonProps: null,
    
    constructor: function(args){
    },

    postCreate: function(){
      var t = this,
          g = t.grid;
      
      var ddbutton = new GridColumnSelectionButton({
          grid: g
      });
  
      ddbutton.placeAt(this.domNode, "last");
  
      ddbutton.startup();
    }
  });
  
});
