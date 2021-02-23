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
define([
        "jsShared/utils/utils",
        "dojo/text!./SearchPill.html",
        "dojo/_base/declare", 
        "dijit/_WidgetBase",
        "dijit/_TemplatedMixin",
        "dijit/_WidgetsInTemplateMixin",
        "dojo/i18n!../nls/sharedMessages",
        "dojo/dom-class",
        "dijit/MenuItem",
        "dijit/registry",
        "jsShared/utils/imgUtils",
        "dojo/on",
        "dojo/dom-style",
        "dojo/dom-attr",
        "jsShared/utils/ID"
        ], function(
                utils,
                template,
                declare,
                _WidgetBase,
                _TemplatedMixin,
                _WidgetsInTemplateMixin,
                i18n,
                domClass,
                MenuItem,
                registry,
                imgUtils,
                on,
                domStyle,
                domAttr,
                sharedID
            ){

    var SearchPill = declare("SearchPill", [ _WidgetBase, _TemplatedMixin, _WidgetsInTemplateMixin ], {
        id : '',
        templateString : template,
        isValid : true,
        inValidErrorMessage : null,
        ariaLabel : i18n.SEARCH_ENTER_CRITERIA,
        textDir: utils.getBidiTextDirectionSetting(),
        value : null,
        
        searchPillSelected : "searchPillSelected",

        postCreate : function() {
          var me = this;

          // For a11y: aria-label, title
          domAttr.set(this.searchPillDeleteButton, "aria-label", i18n.SEARCH_PILL_DELETE_BUTTON);
          domAttr.set(this.searchPillDeleteButton, "title", i18n.SEARCH_PILL_DELETE_BUTTON);

          on(this.searchPillDeleteButton, "click", function() {
            registry.byId(sharedID.SEARCH_MAIN_BOX).removeSearchPill(me);
            me.__performSearch();
            me.destroy();
          });
        },
        
        showPillAsSelected : function(event){
          if(!(event!=null && event.target.localName.indexOf("input")>-1)){ //maybe temp check, but clicking on the dropdowns or text input fires off this event which causes the entire searchPill to show as selected
            this.set("class", this.searchPillSelected);
            this.set("focused",true);
            this.searchByDropDown.set("class", "searchPillLeftSelected"); 
            domClass.replace(this.searchPillRightDiv, "searchPillRightSelected","searchPillRightNotSelected");
            domClass.add(this.searchPillDeleteButton, "searchPillDeleteButtonSelected");
          }
        },
        
        /**
         * Return true if the entire pill is selected, false otherwise
         */
        isPillSelected : function() {
          return domClass.contains(this.id, this.searchPillSelected);
        },
        
        showPillAsNotSelected : function(){
          this.set("focused", false);
          this.set("class","searchPillNotSelected");
          this.searchByDropDown.set("class","searchPillLeftNotSelected");
          this.searchByDropDown.set("iconClass", "searchByButtonIcon");
          domClass.replace(this.searchPillRightDiv, "searchPillRightNotSelected", "searchPillRightSelected");
          domClass.remove(this.searchPillDeleteButton, "searchPillDeleteButtonSelected");
        },
        
        searchByClicked : function(){
          this.set("class", "searchPillForLeftSelected");
          this.searchByDropDown.set("class","searchByDropDownButtonSelected");
          this.searchByDropDown.set("iconClass", "searchByButtonIconSelected");          
        },
        
        searchByBlur : function(){
          this.set("class", "searchPillNotSelected");
          this.searchByDropDown.set("class","searchPillLeftNotSelected");
          this.searchByDropDown.set("iconClass", "searchByButtonIcon");
        },
        
        checkForInvalidCombinations_All : function() {
        },
        
        checkKeyCode : function(event){   
        },
        
        /**
         * @param options: An object that contains information used to create each MenuItem in the drop down menu
         *        Current these are the keys being used from options parameters
         *        {label: '', value: '', iconClass: '', icon: '', smallIcon: '', customMenuItemClass: ''}
         */
        __setupResourceDropDown : function(dropDown, dropDownMenu, addAll, options) {
          var me = this;

          if (addAll) {
            var allMenuItem = new MenuItem({
              id : dropDownMenu.id+"_all-menuItem",
              label : i18n.SEARCH_RESOURCE_TYPE_ALL,
              value : "all",
              onClick:function(){
                me.__handleDropDownChange(dropDown,this.label, this.value);
              }

            });
            dropDownMenu.addChild(allMenuItem);
          }
          options.forEach(function(option){
            var menuItem = new MenuItem({
              id : dropDownMenu.id + "_" + option.value + "-menuItem",
              label : option.label,
              value : option.value,
              iconClass : option.iconClass,
              postCreate :  function() {
                if (option.smallIcon) {
                  this.iconNode.innerHTML = imgUtils.getSVGSmall(option.smallIcon);
                } else if (option.icon) {
                  this.iconNode.innerHTML = imgUtils.getSVG(option.icon);
                }
              },
              onClick : function(){
                me.__handleDropDownChange(dropDown, this.label, this.value);
              }
            });

            
            if(option.customMenuItemClass) {
              // For anyone who wants to add custom styling to the menu items
              domClass.add(menuItem.domNode, option.customMenuItemClass);
            }
            
            dropDownMenu.addChild(menuItem);
          });
          var firstMenuItem = dropDownMenu.getChildren()[0];
          dropDown.set("label", firstMenuItem.label); 
          dropDown.set("value", firstMenuItem.value);            
        },

        /*
        * This method controls which left side of the pill is visible to the 
        * user.
        */
        setSelectedMenuItemAndInput : function(menuItemNode) {
          var prevSelectedId= this.id+"_"+this.prevSelected+"-input"; 
          var label = menuItemNode.get("label");
          var selected = menuItemNode.get("value");
          var selectedId=this.id+"_"+selected+"-input";
                     
          if(prevSelectedId != selectedId){
            var prevInput = registry.byId(prevSelectedId);
            prevInput.set("class", "searchPillInputAreaHidden");

            var currentInput = registry.byId(selectedId);
            currentInput.set("class", "searchPillInputAreaVisible");
           
            this.searchByDropDown.set("label", label);
            this.prevSelected = selected;
            this.selectedSearchByMenuItem = menuItemNode;
            this.checkForInvalidCombinations_All();
            
            registry.byId(sharedID.SEARCH_MAIN_BOX).recalculateRows();
          }
        },

        // Performs a search with whatever pills are found in the search box
        __performSearch : function() {
          var searchMainBox = registry.byId(sharedID.SEARCH_MAIN_BOX);
          searchMainBox.recalculateRows();
          searchMainBox.performSearch();
        }
    });
    return SearchPill;
});