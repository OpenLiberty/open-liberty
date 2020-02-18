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
        "dojo/_base/declare",
        "dijit/_WidgetBase",
        "dijit/_TemplatedMixin",
        "dijit/_WidgetsInTemplateMixin",
        "dojo/text!./SearchBox.html",
        "dojo/store/Memory",
        "jsShared/utils/utils",
        "dojo/i18n!../nls/sharedMessages",
        "dojo/dom-geometry",
        "dojo/dom-style",
        "dijit/registry",
        "dijit/focus",
        "jsShared/utils/ID"
        ], function(
                declare,
                _WidgetBase,
                _TemplatedMixin,
                _WidgetsInTemplateMixin,
                template,
                Memory,
                utils,
                i18n,
                domGeom,
                domStyle,
                registry,
                focusUtil,
                sharedID
            ){

    var SearchBox = declare("SearchBox", [ _WidgetBase, _TemplatedMixin, _WidgetsInTemplateMixin ], {
        templateString : template,
        id : sharedID.SEARCH_MAIN_BOX,
        textDir: utils.getBidiTextDirectionSetting(),
        ariaLabel : i18n.SEARCH_ENTER_CRITERIA,
        addButtonLabel : i18n.SEARCH_BUTTON_ADD,
        clearButtonLabel : i18n.SEARCH_BUTTON_CLEAR,
        searchButtonLabel : i18n.SEARCH_BUTTON_SEARCH,
        totalNumSearchPillsCreated : 0, /*for searchpill ID*/
        origSearchPaneHeight : 75, /*hard code the value to match what we currently use. can update later to dynamically pull the height*/
        keepSearchAreaExpanded : false, /*the on (mouse in/out, focus, blur, keypress, etc.) have very specific behaviors that will need to know if the expanded search box is expected to remain open or not*/
        inMouseOver : false, /*use this to stop re-executing the mouseover function each time the mouse moves around while already in mouseover */
        numRowsNeededToShowAll : 1,

        recentSearches: new Array(),
        // store for the recent search dropdown
        recentSearchesStore: new Memory(),
        // store for default autocomplete dropdown
        defaultSearchesStore: new Memory(),

        __calculateNumRowsNeededToShowAll : function (){
          var buttonDiv = document.getElementById("searchBoxButtonsDiv");
          var buttonDivW = 115;
          if(buttonDiv!=null){
            buttonDivW = Math.ceil(domStyle.get(buttonDiv, "width"));
          }

          var searchBoxW = Math.ceil(domStyle.get(this.domNode, "width"));

          var searchPillsTotalWidth = 0;
          var searchPillHeight = 22;
          var max_searchPillPaneW = searchBoxW - buttonDivW - 20;// extra 20px for right margin;
          var lastVisibleSearchPillPosition = null;
          var foundLastVisibleSearchPill=false;

          this.searchPillPane.getChildren().forEach( function(searchPill, i){
            searchPillsTotalWidth += Math.ceil(domStyle.get(searchPill.domNode, "width"))+5; //5px right margin
            searchPillHeight = domStyle.get(searchPill.domNode, "height");
            if((searchPillsTotalWidth > max_searchPillPaneW)&& !foundLastVisibleSearchPill){
              lastVisibleSearchPillPosition = domGeom.position(searchPill.domNode);
              foundLastVisibleSearchPill = true;
            }
          });

          this.numRowsNeededToShowAll = Math.ceil(searchPillsTotalWidth /max_searchPillPaneW);
          if(max_searchPillPaneW-searchPillsTotalWidth-40 <200) { /*Check if textbox has space to properly display, if not, add a row*/
              this.numRowsNeededToShowAll++;
          }

          if(this.numRowsNeededToShowAll>1){
            if(this.keepSearchAreaExpanded) {
              this.__showMultipleRows(true);
            }else{
              this.__showMultipleRows(false, searchPillHeight, lastVisibleSearchPillPosition);
            }
          }else {
            this.__showMultipleRows(false, searchPillHeight, lastVisibleSearchPillPosition);
          }

          domStyle.set(this.searchPillPane.domNode, "maxWidth",max_searchPillPaneW+"px");
          this.__setMainTextAreaPosition();

        },

        __setMainTextAreaPosition : function(){
          var searchPills = this.searchPillPane.getChildren();
          var buttonDivPosition = domGeom.position(document.getElementById("searchBoxButtonsDiv"));

          if(searchPills.length>0){
              var lastPillPos = domGeom.position( (searchPills[searchPills.length-1]).domNode);
              var newX=lastPillPos.x+lastPillPos.w-14; /*14 = 20px left padding - 6px spacing*/
              var newY=lastPillPos.y-buttonDivPosition.y;
              var newW = buttonDivPosition.x - newX -30;

              if(newW>200){/*Use 200 as min width for textbox so that dropdown for autocomplete has enough space to show*/
                domStyle.set(this.searchTextBox.domNode, {
                  "float": "none",
                  "margin-top":"0px",
                  "left":newX+"px",
                  "top":newY+"px",
                  "width":newW+"px"
                });
              }else{
                newY = (lastPillPos.y-buttonDivPosition.y)+lastPillPos.h+10;
                newW = buttonDivPosition.x - newX -30;
                domStyle.set(this.searchTextBox.domNode, {
                  "float": "none",
                  "margin-top":"0px",
                  "left":"20px",
                  "top":newY+"px",
                  "width":"calc(100% - 200px)"
                });
              }

          }else{
           // this.searchTextBox.set("class","searchTextBox"); //this is not overwriting the values that were dynamically set, so have to re-assign needed values
           domStyle.set(this.searchTextBox.domNode, {
              "left":"20px",
              "width":"calc(100% - 200px)"
            });
          }
        },

        removeSearchPill : function (searchPill){
          this.searchPillPane.removeChild(searchPill);
          searchPill.checkForInvalidCombinations_All();
          searchPill.destroy();

          this.__calculateNumRowsNeededToShowAll();
          if(this.numRowsNeededToShowAll>1) {
            this.__showMultipleRows(true);
          } else {
            this.__showMultipleRows(false);
          }
        },

        __showMultipleRows : function (show,searchPillHeight, lastVisibleSearchPillPosition){
          var searchConditionDiv = registry.byId("searchConditionDiv");
          var newHeight = this.origSearchPaneHeight;

          if(show){
              newHeight = this.numRowsNeededToShowAll * (this.origSearchPaneHeight-35) + 35; //don't need the top and bottom margins multiplied
              domStyle.set(this.searchPillPane.domNode, "float", "left");
              this.__setMainTextAreaPosition();
              domStyle.set(this.searchTextBox.domNode, "display","inline-block");
              this.__showPillOverlay(false, "__showMult");
          }else{
            if(this.numRowsNeededToShowAll>1 ){
              this.__showPillOverlay(true,"__showMult",searchPillHeight, lastVisibleSearchPillPosition);
              domStyle.set(this.searchTextBox.domNode, "display","none");

            }
            domStyle.set(this.searchPillPane.domNode, "float", "none");
            domStyle.set(this.searchTextBox.domNode, "display","inline-block");

          }

          domStyle.set(searchConditionDiv.domNode, "height", newHeight+"px");

          domStyle.set(this.domNode, "height", (newHeight-35)+"px");
          domStyle.set(this.searchPillPane.domNode, "height", (newHeight-35)+"px");

          if(!show && this.numRowsNeededToShowAll>1 )
            this.__showPillOverlay(true,"__showMult2");

        },

        __showPillOverlay : function (show, callingFunc, height, lastVisibleSearchPillPosition){
          var pillOverlay = document.getElementById("fadingOverlay");

          //console.log("show overlay: "+show+", calledby "+callingFunc+", last: ", lastVisibleSearchPillPosition);
          if(pillOverlay!=null){
              if(show){

                domStyle.set(this.searchTextBox.domNode, "display","none");

                var searchPillPanePos = domGeom.position(this.searchPillPane.domNode);

                if(lastVisibleSearchPillPosition ==null ){
                  var foundLastVisibleSearchPill=false;
                  var searchPillsTotalWidth=0;

                  this.searchPillPane.getChildren().some( function(searchPill, i){
                    searchPillsTotalWidth += Math.ceil(domStyle.get(searchPill.domNode, "width"))+5;
                    //console.log("sp total w: "+searchPillsTotalWidth+", pane w: "+searchPillPanePos.w);

                    if((searchPillsTotalWidth > searchPillPanePos.w)&& !foundLastVisibleSearchPill){
                      lastVisibleSearchPillPosition = domGeom.position(searchPill.domNode);
                      //console.log("sp total w: "+searchPillsTotalWidth+", pane w: "+searchPillPanePos.w+", last. ",lastVisibleSearchPillPosition);
                      foundLastVisibleSearchPill = true;
                    }

                    return foundLastVisibleSearchPill;
                  });

                }

                var searchPillPaneRightEdge = searchPillPanePos.x + searchPillPanePos.w;

                if(lastVisibleSearchPillPosition!=null){// && (lastVisibleSearchPillPosition.x+10) < searchPillPaneRightEdge){
                    var newX=lastVisibleSearchPillPosition.x+10;
                    var newW= Math.floor(searchPillPaneRightEdge - newX);

                    if(height!=null)
                      pillOverlay.style.height = height+"px";
                    else
                      pillOverlay.style.height = lastVisibleSearchPillPosition.h+"px";

                    if(newW>0){
                      pillOverlay.style.display = "inline-block";
                      pillOverlay.style.left = newX+"px";
                      pillOverlay.style.top = lastVisibleSearchPillPosition.y+"px";
                      pillOverlay.style.width = newW+"px";
                    }else{
                      pillOverlay.style.display = "none";
                    }

                }
              } else {
                if(this.keepSearchAreaExpanded){
                  //this.__setMainTextAreaPosition();
                }
                pillOverlay.style.display = "none";
              }
          }
        },

        /*TODO: send all searchpill input or searchbox focus requests here. searchbox focus and dropdown don't always
         * behave as expected or described, so having a single place to manipulate *should* help with consistency*/
        __setFocusOnSearchPillOrBox: function(searchPill,isShiftTab){
          if(searchPill){
              if(isShiftTab) {
                searchPill.showPillAsSelected();
              }
              else{
                var id=searchPill.id+"_"+searchPill.selectedSearchByMenuItem.value+"-input";
                var focusNode = registry.byId(id);
                if (focusNode.focus) {
                  focusNode.focus();
                }
              }
          } else {
            focusUtil.focus(this.searchTextBox.textbox);
          }
        },

        displayAllRows : function(event,internalCall) {
          if((event!=null&&(event.type=="click"||event.type=="focus")) || internalCall){
            this.keepSearchAreaExpanded = true;
          }

          if(!this.inMouseOver && this.numRowsNeededToShowAll>1){
            this.__showMultipleRows(true);
            this.inMouseOver=true;
          }
          if(event!=null && event.type=="click" && event.currentTarget.id.indexOf(this.searchPillPane.id)>-1){
            domStyle.set(this.searchTextBox.domNode, "display","inline-block");
          }
        },

        recalculateRows : function(){
          this.__calculateNumRowsNeededToShowAll();
        },

        checkKeyCode : function() {
          // Extending widgets should implement this method since the
          // template has this method as a dojo event
        },

        /**
         * Helper method when appending more dojo events to a node.  Will return
         * a trailing comma if existing events are found on the node
         */
        getExistingEvents : function(node, attachEventId) {
          var existingEvents = node.getAttribute(attachEventId);
          if(existingEvents) {
            // Prepare the event list with a trailing comma because
            // expecting more events to be added to the existing events
            existingEvents += ',';
          }
          return existingEvents;
        }
    });
    return SearchBox;

});
