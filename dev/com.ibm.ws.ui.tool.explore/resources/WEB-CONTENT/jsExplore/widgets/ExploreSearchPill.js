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
        "dojo/dom-class",
        "dojo/_base/lang",
        "dijit/registry",
        "dojo/_base/declare",
        "dojo/text!./templates/ExploreSearchPill.html",
        "dijit/MenuItem",
        "dojo/i18n!../nls/explorerMessages",
        "jsShared/utils/imgUtils",
        "dojox/xml/parser",
        "dojo/query",
        "jsShared/search/SearchPill",
        "dojo/dom-construct",
        "jsShared/utils/ID"
        ], function(
                utils,
                domClass,
                lang,
                registry,
                declare,
                exploreSearchPillTemplate,
                MenuItem,
                i18n,
                imgUtils,
                xmlParser,
                query,
                SearchPill,
                domConstruct,
                sharedID
            ){

/**
 * The ExploreSearchPill is an extension of the SearchPill base widget.
 */
    var ExploreSearchPill = declare("ExploreSearchPill", [ SearchPill ], {
        constructor : function(params) {
        },

        prevSelected : null, /**String value**/
        selectedSearchByMenuItem : null, /**MenuItem **/
        tagInputLabel: i18n.SEARCH,

        postCreate : function() {
          var me = this;
          this.prevSelected = "name";

          var menuOptions = [
            {value:"name", label:i18n.SEARCH_RESOURCE_NAME},
            {value:"type", label:i18n.SEARCH_RESOURCE_TYPE},
            {value:"state", label:i18n.SEARCH_RESOURCE_STATE},
            {value:"tag", label:i18n.SEARCH_RESOURCE_TAG},
            {value:"runtimeType", label:i18n.SEARCH_RESOURCE_RUNTIMETYPE},
            {value:"container", label:i18n.SEARCH_RESOURCE_CONTAINER},
            {value:"owner", label:i18n.SEARCH_RESOURCE_OWNER},
            {value:"contact", label:i18n.SEARCH_RESOURCE_CONTACT},
            {value:"note", label:i18n.SEARCH_RESOURCE_NOTE}
          ];

          menuOptions.forEach(function(option) {
            me.searchByDropDownMenu.addChild(new MenuItem({
              id : me.searchByDropDownMenu.id+"_" + option.value + "-menuItem",
              label: option.label,
              value: option.value,
              onClick:function(){
                me.setSelectedMenuItemAndInput(this);
              }
            }));
          });

          var firstMenuItem = this.searchByDropDownMenu.getChildren()[0];
          if (this.searchBy && typeof this.searchBy === "string") {
            // need to initialize to a specific option
            firstMenuItem = this.__getMenuItem(this.searchByDropDownMenu, this.searchBy);
          }
          this.searchByDropDown.set("label", firstMenuItem.label); //defaultSearchByValue = "test";
          this.selectedSearchByMenuItem = firstMenuItem;
          this.setSelectedMenuItemAndInput(firstMenuItem);

          this.__setupResourceTypeDropDown();
          this.__setupResourceStateDropDown();
          this.__setupResourceContainerDropDown();
          this.__setupResourceRuntimeTypeDropDown();

          var placeholder = i18n.SEARCH;

          this.nameInput.set("placeholder", placeholder);
          this.tagInput.set("placeholder", placeholder);
          this.ownerInput.set("placeholder", placeholder);
          this.contactInput.set("placeholder", placeholder);
          this.noteInput.set("placeholder", placeholder);

          this.__setInputField(this.id, this.searchBy, this.value);
          this.inherited(arguments);
        },

        __getMenuItem : function(dropDownMenu, itemValue) {
          // default to the first one so never returns null
          var menuItem = dropDownMenu.getChildren()[0];
          dropDownMenu.getChildren().forEach(function(item) {
            if (item.value === itemValue) {
              menuItem = item;
            }
          });
          return menuItem;
        },

        checkKeyCode : function(event){
          registry.byId(sharedID.SEARCH_MAIN_BOX).checkKeyCode(event,this) ;
        },

        checkInput : function(event){
          registry.byId(sharedID.SEARCH_MAIN_BOX).createPillFromSearchBySelect(event,true) ;
        },

        showPillAsValid : function(valid){
          var searchBy = this.selectedSearchByMenuItem.value;
          var valueDomNode = null;
          var invalidPillClass = null;

          var input = registry.byId(this.id + "_" + searchBy + "-input");
          if (input.type === "button") {
            // it is a dropdown set of options
            valueDomNode = input.containerNode;
            invalidPillClass = "searchPillInvalidMenuItemValue";
          } else {
            // assume it is a text field
            valueDomNode = input.textbox;
            invalidPillClass = "searchPillInvalidTextValue";
          }
          if (valid) {
            if(domClass.contains(valueDomNode, invalidPillClass)){
              domClass.remove(valueDomNode, invalidPillClass);
            }
            if(domClass.contains(this.domNode, "searchPillInvalid")){
              domClass.remove(this.domNode, "searchPillInvalid");
            }
          } else {
            domClass.add(valueDomNode, invalidPillClass);
            domClass.add(this.domNode, "searchPillInvalid");
          }
        },

        /**
         *
         * Check the current (new or active) search pill against all others for possible conflicts
         */
        checkForInvalidCombinations_All : function(){
          var searchPillPane = registry.byId("search-searchPill-pane");
          var errorReason = null;

          searchPillPane.getChildren().forEach( function(searchPill, i){
            var givenSearchBy = searchPill.selectedSearchByMenuItem.value;
            var givenSearchByLabel = searchPill.selectedSearchByMenuItem.label;
            var givenSearchValue =registry.byId(searchPill.id+"_"+givenSearchBy+"-input").value;
            var givenSearchValueLabel =registry.byId(searchPill.id+"_"+givenSearchBy+"-input").label;
            var me = searchPill;

            /*Reset all other search pills back to regular style */
            searchPill.showPillAsValid();
            searchPill.isValid=true;
            var markedInvalid = false;

            //console.log("givenSearchBy: "+givenSearchBy+", givenSearchValue: "+givenSearchValue);
            searchPillPane.getChildren().forEach( function(searchPill2, i2){
                  if(searchPill2.id != me.id && i2<i){

                        var searchBy = searchPill2.selectedSearchByMenuItem.value;
                        var searchByLabel = searchPill2.selectedSearchByMenuItem.label;
                        var searchValue = registry.byId(searchPill2.id+"_"+searchBy+"-input").value;
                        var searchValueLabel = registry.byId(searchPill2.id+"_"+searchBy+"-input").label;

                        /*if(givenSearchBy.toUpperCase() === searchBy.toUpperCase()){
                          /*Check for exact same condition */
                          /*if(givenSearchValue.toUpperCase() === searchValue.toUpperCase() ){
                            markedInvalid = true;
                            errorReason = lang.replace(i18n.SEARCH_CRITERIA_INVALID_DUPLICATE,
                                                        [me.__getSpanOpen()+ givenSearchByLabel+": "+givenSearchValueLabel+"</span>"]);
                          }*/

                          /*Check for existing Status condition if this is a second Status condition */
                          /*Check for existing Type condition if this is a second Type condition */
                          /**Removing this until a search pill can handle 2 input values into the right side of search pill
                          if(givenSearchBy.toUpperCase() === "TYPE" || givenSearchBy.toUpperCase() === "STATE" ){
                            markedInvalid = true;
                            errorReason = lang.replace(i18n.SEARCH_CRITERIA_INVALID_DUPLICATE,
                                                        [me.__getSpanOpen()+ givenSearchByLabel+"</span>"]);
                          }
                          */
                        //}//end same searchby

                        /*Check for existing Status condition if this is Type = Host */
                        /*Check for existing Type = Host condition if this is Status */
                        /*Check for runtimeType and Container... type can't be host cluster or app (only runtime or server is valid) */
                        var typeHost = (givenSearchBy.toUpperCase() === "TYPE" && givenSearchValue.toUpperCase()==="HOST") ||
                                       (searchBy.toUpperCase() === "TYPE" && searchValue.toUpperCase()==="HOST");
                        var typeCluster = (givenSearchBy.toUpperCase() === "TYPE" && givenSearchValue.toUpperCase()==="CLUSTER") ||
                                       (searchBy.toUpperCase() === "TYPE" && searchValue.toUpperCase()==="CLUSTER");
                        var typeApp = (givenSearchBy.toUpperCase() === "TYPE" && givenSearchValue.toUpperCase()==="APPLICATION") ||
                                       (searchBy.toUpperCase() === "TYPE" && searchValue.toUpperCase()==="APPLICATION");
                        if( typeHost && searchBy.toUpperCase()==="STATE" ||
                            typeHost && givenSearchBy.toUpperCase()==="STATE" ||
                            (typeHost || typeCluster || typeApp) && givenSearchBy.toUpperCase()==="RUNTIMETYPE" ||
                            (typeHost || typeCluster || typeApp) && givenSearchBy.toUpperCase()==="RUNTIMETYPE" ||
                            (typeHost || typeCluster || typeApp) && givenSearchBy.toUpperCase()==="CONTAINER" ||
                            (typeHost || typeCluster || typeApp) && givenSearchBy.toUpperCase()==="CONTAINER"
                            ){
                          markedInvalid = true;
                          errorReason = lang.replace(i18n.SEARCH_CRITERIA_INVALID_COMBO,
                                                     [me.__getSpanOpen()+ givenSearchByLabel+": "+givenSearchValueLabel+"</span>",
                                                      me.__getSpanOpen()+ searchByLabel+": "+searchValueLabel+"</span>"]);
                        }
                  }

                });

                if(markedInvalid){
                  searchPill.showPillAsValid(false);
                  searchPill.isValid=false; /*TODO - revisit why searchPill didn't work to just set isValid for the searchpill in the for loop*/
                  searchPill.inValidErrorMessage = errorReason;
                  registry.byId(sharedID.SEARCH_MAIN_BOX).createSearchErrorPane(errorReason);
                }else{
                  searchPill.showPillAsValid(true);
                  searchPill.isValid=true;
                  searchPill.inValidErrorMessage = null;
                  registry.byId(sharedID.SEARCH_MAIN_BOX).clearErrorMsgAndSearchView() ;
                }
          });

        },

        /**
         * Creates a <span> with the correct text direction.
         */
         __getSpanOpen : function() {
          var spanOpen = "<span dir='" + utils.getBidiTextDirectionSetting() + "'>";
          return spanOpen;
        },

        __setupResourceTypeDropDown : function(){
          this.__setupResourceDropDown(this.resourceTypeDropDown, this.resourceTypeDropDownMenu, true, [
            {value:"application", label:i18n.APPLICATION, iconClass:"applicationsMenuItemIcon", smallIcon:'app'},
            {value:"server",      label:i18n.SERVER,      iconClass:"serversMenuItemIcon",      smallIcon:'server'},
            {value:"cluster",     label:i18n.CLUSTER,     iconClass:"clustersMenuItemIcon",     smallIcon:'cluster'},
            {value:"host",        label:i18n.HOST,        iconClass:"hostsMenuItemIcon",        smallIcon:'host'},
            {value:"runtime"    , label:i18n.RUNTIME,     iconClass:"runtimesMenuItemIcon",     smallIcon:'runtime'}
          ]);
        },

        __setupResourceStateDropDown : function(){
          this.__setupResourceDropDown(this.resourceStateDropDown, this.resourceStateDropDownMenu, true, [
            {value:"STARTED", label:i18n.RUNNING, iconClass:"runningMenuItemIcon", icon:'status-running'},
            {value:"STOPPED", label:i18n.STOPPED, iconClass:"stoppedMenuItemIcon", icon:'status-stopped'},
            {value:"UNKNOWN", label:i18n.UNKNOWN, iconClass:"unknownMenuItemIcon", icon:'unknown'}
          ]);
        },

        __setupResourceContainerDropDown : function(){
          this.__setupResourceDropDown(this.resourceContainerDropDown, this.resourceContainerDropDownMenu, false, [
            {value:"docker", label:i18n.CONTAINER_DOCKER, iconClass:""}
          ]);
        },

        __setupResourceRuntimeTypeDropDown : function(){
          this.__setupResourceDropDown(this.resourceRuntimeTypeDropDown, this.resourceRuntimeTypeDropDownMenu, false, [
            {value:"liberty", label:i18n.CONTAINER_LIBERTY, iconClass:""},
            {value:"Node.js", label:i18n.CONTAINER_NODEJS, iconClass:""}
          ]);
        },

        __handleDropDownChange : function(dropdown, label, value){
          var searchMainBox = registry.byId(sharedID.SEARCH_MAIN_BOX) ;
          dropdown.set("label",label);
          dropdown.set("value",value);
          this.checkForInvalidCombinations_All();
          searchMainBox.recalculateRows();
          searchMainBox.performSearch();
      },

      __setInputField : function(id, searchBy, newValue) {
        if(! newValue) {
          return;
        }

        // if an initial value is passed in, set the correct selections
        var input = registry.byId(id + "_" + searchBy + "-input");
        if (input.type === "button") {
          // it is a dropdown set of options
          var dropDownMenu = registry.byId(id + "_" + searchBy + "-dropDownMenu");
          var menuItem = registry.byId(dropDownMenu.id + "_" + newValue + "-menuItem");
          if(menuItem != null){
            input.set("label", menuItem.label);
            input.set("value", this.value);
          }
        } else {
          // FIXME: This doesn't work and I have no clue why.
          // assume it is a text field
          input.set("displayedValue", newValue);
        }

      }, // end of __setInputField

      buildRendering : function() {
        /*
         * Replace the empty right side of the search pill with explore
         * specific drop down menu elements.
         */
        var newRightDivDom = domConstruct.toDom(exploreSearchPillTemplate);
        var baseTemplate = domConstruct.toDom(this.templateString);
        var attachPointNode =
          query('div[data-dojo-attach-point="searchPillRightDiv"]', baseTemplate)[0];

        domConstruct.place(newRightDivDom, attachPointNode, "replace");
        this.templateString = utils.domToString(baseTemplate);

        this.inherited(arguments);
      } // end of buildRendering

    });
    return ExploreSearchPill;
});
