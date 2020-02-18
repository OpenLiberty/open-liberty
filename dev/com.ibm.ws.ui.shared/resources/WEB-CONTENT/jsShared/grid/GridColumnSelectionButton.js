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
/* jshint strict: false */
/* globals imgUtils: false */
define('jsShared/grid/GridColumnSelectionButton', 
    [ 'dojo/_base/declare', 
      'dojo/_base/lang', 
      'dojo/aspect',
      'dojo/dom-class', 
      'dijit/_WidgetBase',
      'dijit/_FocusMixin', 
      'dijit/_TemplatedMixin', 
      'dijit/CheckedMenuItem', 
      'dijit/DropDownMenu',
      'dijit/registry',
      'dijit/form/DropDownButton', 
      'dijit/Menu', 
      'dijit/MenuItem',
      'dojo/i18n!jsShared/nls/sharedMessages'
    ], 
      
 function(declare, lang, aspect, domClass,
          _WidgetBase, _FocusMixin, _TemplatedMixin, 
          CheckedMenuItem, DropDownMenu, registry, 
          DropDownButton, Menu, MenuItem, i18n) {

  /**
   * Summary:
   *    A DropDownButton which loads a DropDownMenu of grid columns which can be hidden. 
   *    If the checkbox associated with each column name in the DropDownMenu is selected,
   *    the column is displayed.  If un-selected, the column is hidden.
   *    
   *    The DropDownMenu created has one menu item per column defined in the grid's
   *    structure.  Initialize the structure to include the following parameters for 
   *    each column:
   *      hide   - 'true' indicates the column should be hidden on the grid. 
   *      hideable - 'true' indicates this column can be selected/unselected to be hidden
   *                 and should therefore appear in the menu;
   *                 'false' or missing indicates the column is ALWAYS on the grid and 
   *                 will not appear in the DropDownMenu as a choice.   
   *                 
   *    If the grid was created with grid.tableIdentifier set to a string that identifies the
   *    table type (Cluster, Application, Job Instances, Executions) then the aria label
   *    on the dropDown menu will identify the table the list of columns belongs to.                          
   *    
   **/

  return declare([ DropDownButton ], {
    grid: null,

    baseClass: 'gridColumnSettings',
    iconClass: 'gridColumnSettingsIconClass',

    showLabel: false,
    label: i18n.GRID_COLUMN_SELECTION_BUTTON_LABEL,  // Used as the hover over text
    columnSelectionMenu: undefined,

    constructor: function(args) {
      declare.safeMixin(this, args);

      if (!this.grid) {
        console.error("GridColumnSelectionButton created without Grid");
      }

      if (!args.id) {
        this.id = this.grid.id + '-columnSelectionButton';
        var existing = registry.byId(this.id);
        if (existing) {
          existing.destroyRecursive();
        }
      }
    },

    postMixInProperties: function() {
      this.inherited(arguments);

      this.role = 'button';
    },

    postCreate: function() {
      this.inherited(arguments);
      var g = this.grid;

      this._setShowLabelAttr(false);

      this.columnSelectionMenu = this.__createColumnSelectionMenu(g.id);
      this.set('dropDown', this.columnSelectionMenu);

      this.iconNode.innerHTML = imgUtils.getSVGSmall('settings');
      
      // In __createColumnSelectionMenu, the DropDownMenu is created with an aria-label.
      // However, _HasDropDown.openDropDown() will assign an aria-labelledby attribute
      // to the dropDown each time the dropDown is opened.  This is causing some batchScan
      // errors and it would be better if the aria-labelledby is removed.  See 232105.
      aspect.after(this, "openDropDown", function() {
        var dropDownNode = this.dropDown.domNode;
        if (dropDownNode) {
          dropDownNode.removeAttribute("aria-labelledby");
        }
      })

    },

    __createColumnSelectionMenu: function(id) {
      // NOTE: Explore names it -actionMenu
      var columnSelectionMenuId = id + '-columnSelectionMenu';
      var columnSelectionMenu = registry.byId(columnSelectionMenuId);
      if (!columnSelectionMenu) {
        columnSelectionMenu = new DropDownMenu({
          id: columnSelectionMenuId,
          'aria-label': lang.replace(i18n.GRID_COLUMN_SELECTION_MENU_LABEL,
                                     [this.grid.tableIdentifier? this.grid.tableIdentifier: ""]),
          leftClickToOpen: true
        });
        this.__createColumnSelectionMenuItems(columnSelectionMenu);
      }
      return columnSelectionMenu;
    },

    __createColumnSelectionMenuItems: function(columnSelectionMenu) {
      var t = this, 
          g = this.grid, 
          columnTitles = [];

      for (var i = 0, j = 0; i < g.structure.length; i++) {
        if (g.structure[i].hideable) {
          columnSelectionMenu.addChild(new CheckedMenuItem({
            id: columnSelectionMenu.id + g.structure[i].id,    
            label: g.structure[i].name,
            value: g.structure[i].id,    // since multiple columns can point to the same db field,
                                         // set the value as the id of the column, not its field value.
            grid : g,
            baseClass: 'gridColumnMenuItem',
            iconClass: g.structure[i].hide ? 'gridColumnMenuItem_unchecked' : 'gridColumnMenuItem_checked',
            checked : g.structure[i].hide ? false : true,
            onChange: t._onChange
          }));

          columnTitles[j++] = g.structure[i].field;
        }
      }

      console.log(columnTitles);
      columnSelectionMenu.startup();

    },

    /** 
     * onChange method for a dijit/CheckedMenuItem.  This flags the column on the table
     * to be rendered or hidden.
     * 
     * The DropDownMenu containing the CheckedMenuItems displays the names of the columns
     * of a table.   If the CheckedMenuItem is selected (checked) it appears on the table;
     * when unselected (unchecked), the column is hidden. 
     * 
     * When the user selects a menu item (checks it), this method gets a value of 'true' 
     * and the column is removed from the list of hidden columns for the table so that it
     * is now displayed.
     * When the user unselects the menu item (unchecks it), this method gets a value of 'false'
     * and the column is added to the list of hidden columns for the table so that it is 
     * now hidden. 
     *  
     * @param checked - true: indicates that the column should be displayed and therefore it
     *                        is removed from the list of hidden columns for the table.
     *                  false: indicates that the column should be hidden and therefore it is
     *                        added to the list of hidden columns for the table.
     *                  
     */
    _onChange : function(checked) {
      if (checked) {
        this.grid.hiddenColumns.remove(this.value);
      } else {
        this.grid.hiddenColumns.add(this.value);
      }
      this.set('iconClass', this.checked ? 'gridColumnMenuItem_checked' : 'gridColumnMenuItem_unchecked');
      
      if (this.grid.gridStatePersist) {
        this.grid.gridStatePersist.save();
      }

    }

  });

});
