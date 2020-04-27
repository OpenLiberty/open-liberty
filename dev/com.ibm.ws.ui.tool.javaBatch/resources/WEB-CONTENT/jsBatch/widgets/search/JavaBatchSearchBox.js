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
/* jshint strict : false */
define([
        "dijit/registry",
        "dojo/keys",
        "dojo/aspect",
        "dojo/dom-style",
        "dojo/dom-geometry",
        "dojo/request/xhr",
        "dijit/focus",
        "dojo/dom",
        "dojo/_base/declare",
        "dojo/_base/lang",
        "dojo/_base/array",
        "jsBatch/widgets/search/JavaBatchSearchPill",
        "dijit/form/TextBox",
        "dijit/layout/ContentPane",
        "dojo/store/Memory",
        "dojo/Deferred",
        "dojo/dom-attr",
        "jsShared/utils/utils",
        "jsShared/search/SearchBox",
        "dojo/dom-construct",
        "dojo/query",
        "jsBatch/utils/viewToHash",
        "jsBatch/utils/ID",
        "dojo/i18n!../../nls/javaBatchMessages"
        ], function(
                registry,
                keys,
                aspect,
                domStyle,
                domGeom,
                request,
                focusUtil,
                dom,
                declare,
                lang,
                array,
                JavaBatchSearchPill,
                TextBox,
                ContentPane,
                Memory,
                Deferred,
                domAttr,
                sharedUtils,
                SearchBox,
                domConstruct,
                query,
                viewToHash,
                ID,
                i18n
            ){

  var JavaBatchSearchBox = declare("JavaBatchSearchBox", [ SearchBox ], {
      searchByStore : null,
      ariaLabel : i18n.JAVA_BATCH_SEARCH_BOX_LABEL,

      postCreate : function() {
          var me = this;
          aspect.after(this.searchPillPane, "resize", function(){
            me.__calculateNumRowsNeededToShowAll();
          });
          this.__setupMainTextArea();
      },

      // Update the visible search pills to match the query parameters in the URL
      updateSearchPills : function(query) {

        // Sure, we can try to update existing pills if they exist
        // but thats too complicated.  Just delete all pills and recreate
        // the pills.  This is easier to maintain.
        this.__deleteCurrentPills();

        if(!query) {
          return;
        }

        if(query.indexOf('?') === 0) {
          // remove the leading ?
          query = query.substring(1);
        }

        var searchPills = this.__queryParamToSearchPill(query);
        for(var i = 0; i < searchPills.length; i++) {
          var pill = searchPills[i];
          this.addNewSearchPill(pill);
        }
      },

      /**
       * Parse all the URL query parameters.  Return an array of tuples.
       * Each tuple represents a single search pill's searchBy and value.
       * @param allQueryParameters
       */
      __queryParamToSearchPill : function(allQueryParameters) {
        // Multiple search by job instance ID or job instance state
        // are in a single queryParam with a list for value
        // Example:  jobInstanceId=1,2,3,4
        // We need to make the example into four tuples to represent
        // four individual job instance id search pills.

        var queryParamsThatCanHaveListValues = ["jobInstanceId", "instanceState"];
        var searchPills = [];

        // First split the query params up
        var queryParams = allQueryParameters.split("&");

        for(var i = 0; i < queryParams.length; i++) {
          var tempParam = queryParams[i];
          tempParam = tempParam.split("=");
          var searchBy = tempParam[0];
          var value = tempParam[1];

          // The pills are being created based off the URL's query string.  The query string
          // parameter values are in encoded format.  We want our code to work with decoded
          // special characters for methods like .split(',') rather than .split('%2C').
          value = decodeURIComponent(value);

          if(searchBy === 'sort' || searchBy === 'pageSize' || searchBy === 'ignoreCase') {
            // There is not a search pill for the sort query string
            continue;
          }

          if(queryParamsThatCanHaveListValues.indexOf(searchBy) > -1 && value.indexOf(',') > -1) {
            // This single query param will be represented by multiple
            // search pills with the same searchBy (left pill side)
            // Example: instanceState=DISPATCHED,COMPLETED is two pills
            var tempValues = value.split(',');
            for(var k = 0; k < tempValues.length; k++) {
              // init searchByAndValue variable here to always ensure references to the
              // object in previous iterations are severed
              var searchByAndValue = {searchBy: null, value: null};
              searchByAndValue.searchBy = searchBy;
              searchByAndValue.value = tempValues[k];
              searchPills.push(searchByAndValue);
            }
          } else {
            // init temp variable here to always ensure references to the object in
            // previous calls are severed
            var temp = {searchBy: null, value: null};
            temp.searchBy = searchBy;
            temp.value = value;
            searchPills.push(temp);
          }
        }
        return searchPills;
      },  // end of method __queryParamToSearchPill

      isSingleDateSearchPill : function(searchBy) {
        return searchBy.indexOf("lastUpdate") > -1 && !this.isDateRangeSearchPill(searchBy);
      },

      isDateRangeSearchPill : function(searchBy) {
        return searchBy.indexOf("lastUpdateRange") > -1;
      },

      // This method is to deal with Last Update and Last Update Range pill combinations.
      // The complexity is mostly for determining what combination needs to be called out 
      // in the error message.
      isDatePillInvalid : function(searchPill, doesOnePillExistFor, showErrorForSearchPill) {
        var searchBy = searchPill.selectedSearchByMenuItem.value;
        var isDateRangeSearchPill = this.isDateRangeSearchPill(searchBy);
        var isSingleDateSearchPill = this.isSingleDateSearchPill(searchBy);

        if(doesOnePillExistFor.rangeDate || doesOnePillExistFor.singleDate) {
          // Found more than one date search pill, mark the pill invalid
          searchPill.markPillAsInvalid();
          // Now determine if if we need to show Last Update or Last Update Range
          // in the error message indicating which pill is invalid
          if(doesOnePillExistFor.singleDate && isDateRangeSearchPill) {
            showErrorForSearchPill.singleDate = true;
            showErrorForSearchPill.rangeDate = true;
          } else if(doesOnePillExistFor.rangeDate && isSingleDateSearchPill) {
            showErrorForSearchPill.singleDate = true;
            showErrorForSearchPill.rangeDate = true;
          } else if(doesOnePillExistFor.rangeDate && isDateRangeSearchPill) {
            showErrorForSearchPill.rangeDate = true;
          } else if(doesOnePillExistFor.singleDate && isSingleDateSearchPill) {
            showErrorForSearchPill.singleDate = true;
          }
          return true; // true means the pill is invalid
        } else {
          // First date pill found, record what the first pill type was found
          if(isDateRangeSearchPill) {
            doesOnePillExistFor.rangeDate = true;
          }
          if(isSingleDateSearchPill) {
            doesOnePillExistFor.singleDate = true;
          }
          return false; // false means the pill is valid
        }
      }, // end of method isDatePillInvalid

      // Scan through the search pills and make sure we do not have any invalid
      // search pill combinations.  Toggle show and hide of the search error
      // pane.
      markAnyInvalidSearchPillCombinations : function() {
        // List of invalid search combinations
        //    1. Multiple search by date pills, any kind of date pill

        // Used to track how many number of pills found so far and which
        // search criteria needs to be listed in the error message when
        // there is a combination of criteria that is not supported
        var doesOnePillExistFor = {singleDate:false, rangeDate:false};
        var showErrorForSearchPill = {singleDate:false, rangeDate:false};

        // Build list of current search pills
        var listOfCurrentPills = this.searchPillPane.getChildren();

        var inErrorState = false;
        for(var i = 0; i < listOfCurrentPills.length; i++) {
          // Run through all of the pills and re-evaluate if each pill
          // needs to be styled as invalid
          var searchPill = listOfCurrentPills[i];

          // Assume all pills start out as a valid pill
          searchPill.markPillAsValid();

          var searchBy = searchPill.selectedSearchByMenuItem.value;
          var isDateRangeSearchPill = this.isDateRangeSearchPill(searchBy);
          var isSingleDateSearchPill = this.isSingleDateSearchPill(searchBy);

          if(isDateRangeSearchPill || isSingleDateSearchPill) {
            inErrorState = this.isDatePillInvalid(searchPill, doesOnePillExistFor, showErrorForSearchPill);
          } 

          // Commented out on purpose.  At one time we had prevent many types of duplicate search
          // criteria.  The server side enhanced the search to allow those duplicate search
          // criteria, so this "else if" is no longer needed.  However, I am keeping this here
          // because it helps explain why this method was written using Objects.  If we need to prevent
          // duplicate criteria in the future, just add to the Objects and uncomment this "else if" statement.

          // else if(searchBy in doesOnePillExistFor) {
          //   // Only check if we need to mark the pill invalid if
          //   // the search criteria is not allowed to have dupes
          //   if(doesOnePillExistFor[searchBy]) {
          //     searchPill.markPillAsInvalid();
          //     showErrorForSearchPill[searchBy] = true;
          //     inErrorState = true;
          //   } else {
          //     doesOnePillExistFor[searchBy] = true;
          //   }
          // }
        }

        if(inErrorState) {
          var errorMsg = this.buildListOfDuplicatePills(showErrorForSearchPill);
          this.showSearchCombinationErrorPane(errorMsg);
        } else {
          this.hideSearchCombinationErrorPane();
        }
      },  // end of method markAnyInvalidSearchPillCombinations

      showSearchCombinationErrorPane : function(listOfDupelicateSearchCriteria) {
        // Prepare the NLS message
        var errorMsg = lang.replace(i18n.ERROR_CANNOT_HAVE_MULTIPLE_SEARCHBY, [listOfDupelicateSearchCriteria]);
        var entireErrorPaneMsg = registry.byId(ID.SEARCH_COMBINATION_ERROR_PANEL_MSG);
        entireErrorPaneMsg.domNode.innerText = errorMsg;

        var entireErrorPane = registry.byId(ID.SEARCH_COMBINATION_ERROR_PANEL);
        domStyle.set(entireErrorPane.domNode, 'display', '');


        var grid = registry.byId(ID.JOBINSTANCE_GRID);
        domStyle.set(grid.domNode, 'display', 'none');
      },

      hideSearchCombinationErrorPane : function() {
        var entireErrorPane = registry.byId(ID.SEARCH_COMBINATION_ERROR_PANEL);
        domStyle.set(entireErrorPane.domNode, 'display', 'none');

        var grid = registry.byId(ID.JOBINSTANCE_GRID);
        domStyle.set(grid.domNode, 'display', '');
      },

      addNewSearchPill : function(searchPillInput) {
        var newSearchPill;

        var newpill_searchBy, newpill_value;
        // if created by autocomplete, set fields
        // if created by +button, skip this
        /* globals Event */
        if (!(searchPillInput instanceof Event)) {
          newpill_searchBy = searchPillInput.searchBy;
          newpill_value = searchPillInput.value;
        }

        try {
          if(newpill_searchBy && newpill_value !== undefined) {
            // Create a search pill with specific searchBy (right side of pill)
            // and value (left side of pill)
            // Create a search pill specific to the user input
            newpill_value = decodeURIComponent(newpill_value);
            newSearchPill = new JavaBatchSearchPill({
              id: ID.SEARCH_PILL + this.totalNumSearchPillsCreated,
              searchBy: newpill_searchBy,
              value: newpill_value
            });
          } else {
            // Create the default search pill
            newSearchPill = new JavaBatchSearchPill({
              id: ID.SEARCH_PILL + this.totalNumSearchPillsCreated
            });
          }
        } catch(err) {
          console.log("Cannot create the search pill: " + searchPillInput.searchBy + "=" + searchPillInput.value);
          console.log(err);
          return; // NOOP the creation of this new search pill
        }

        this.totalNumSearchPillsCreated++;

        this.searchPillPane.addChild(newSearchPill);

        // Make sure we are not creating any incorrect search filter combination
        this.markAnyInvalidSearchPillCombinations();
        this.__calculateNumRowsNeededToShowAll();
        this.displayAllRows(null, true);

        return newSearchPill;
      },

      resetSearchBox : function() {
        this.__deleteCurrentPills();
        viewToHash.updateView("");
        this.__setFocusOnSearchPillOrBox();
      },

      /**
       * Delete the pills that are current visible to the end user
       */
      __deleteCurrentPills : function() {
        var me = this;
        this.searchPillPane.getChildren().forEach( function(searchPill, i){
          me.removeSearchPill(searchPill);
        });
        this.totalNumSearchPillsCreated = 0;
        this.__calculateNumRowsNeededToShowAll();

        var searchBoxText = registry.byId("search-text-box");
        searchBoxText.reset(); // remove any characters from the input field
      },

      performSearch : function() {
        var searchParams = this.__getSearchQueryParams();
        if(searchParams) {
          viewToHash.updateView("?" + searchParams);
        } else {
          viewToHash.updateView("");
        }
      },

      /**
       * Take the src argument and merge them into destObj.  destObj will only
       * merge arguments that are not found already in destObj
       * @param src - a list
       * @param destObj - an object
       */
      __merge : function(src, destObj) {
        for(var i = 0; i < src.length; i++) {
          var element = src[i];
          if(! (element in destObj)) {
            destObj[element] = true;
          }
        }
      },

      /**
       * Cycle through all the search pills and return the query parameters used for the rest api
       * @returns {String}
       */
      __getSearchQueryParams : function() {
        var self = this;
        var queryParams = [];
        var instanceStates = {}; // Use this object like a "set" with no duplicate elements
        var jobInstanceIds = {}; // Use this object like a "set" with no duplicate elements

        this.searchPillPane.getChildren().forEach( function(searchPill) {
          var searchBy = searchPill.selectedSearchByMenuItem.value;
          var pill = registry.byId(searchPill.id + "_" + searchBy + "-input");
          var input = pill.value;
          input = encodeURIComponent(input);
          var isSearchByLastUpdateRange = (searchBy === "lastUpdateRange");
          if(! input) {
            // Need to guard against empty input, except the date range pill
            if(! isSearchByLastUpdateRange) {
              return;
            }
          }

          if(searchBy === 'instanceState') {
            input = input.toUpperCase(); // to ease String comparisons
            if(! (input in instanceStates)) {
              instanceStates[input] = true;
            }
          }
          if(searchBy === 'jobInstanceId') {
            self.__merge([input], jobInstanceIds);
          }
          if(searchBy === 'lastUpdate') {
            queryParams.push("lastUpdate=" + pill.toString());
          }
          if(searchBy === 'jesJobName') {
            queryParams.push("jesJobName=" + input);
          }
          if(searchBy === 'jesJobId') {
            queryParams.push("jesJobId=" + input);
          }
          if(searchBy === 'submitter') {
            queryParams.push("submitter=" + input);
          }
          if(searchBy === 'lastUpdateRange') {
           var fromDate = pill.getChildren()[0].toString();
           var toDate = pill.getChildren()[1].toString();
           queryParams.push("lastUpdateRange=" + fromDate + ":" + toDate);
          }
          if(searchBy === 'appName') {
           queryParams.push("appName=" + input);
          }
          if(searchBy === 'jobName') {
           queryParams.push("jobName=" + input);
          }
          // if(searchBy === 'exitStatus'){
          //   //placeholder for future feature
          // }
        });

        var statesParams = Object.keys(instanceStates);
        if(statesParams.length > 0) {
          // Because the rest APIs want multiple states as instanceState="","",""
          // I had to build a list of unique entries during processing of the pills
          // and build the query parameter here after aggregating all arguments
          var param1 = "instanceState=" + statesParams.toString();
          queryParams.push(param1);
        }

        var instanceIdParams = Object.keys(jobInstanceIds);
        if(instanceIdParams.length > 0) {
          // Because the rest APIs want multiple states as jobInstanceId=1,2,3,4
          // I had to build a list of unique entries during processing of the pills
          // and build the query parameter here after aggregating all arguments
          var param2 = "jobInstanceId=" + instanceIdParams.toString();
          queryParams.push(param2);
        }

        var finalSearchUri = "";
        for(var i = 0; i < queryParams.length; i++) {
          if(i > 0) {
            finalSearchUri += "&";
          }
          finalSearchUri += queryParams[i];
        }

        return finalSearchUri;
      },

      buildRendering : function() {
        /* jshint strict: false */
        /*
         * Here we will attach all the events that are specific to the search box in explore tool
         */
        var attachEventId = "data-dojo-attach-event";
        var baseTemplate = domConstruct.toDom(this.templateString);

        // baseTemplate.setAttribute(attachEventId, "mouseOver:displayAllRows");

        var search_searchPill_pane = query('[id='+ ID.SEARCH_PILL_PANE +']', baseTemplate)[0];
        search_searchPill_pane.setAttribute(attachEventId,"keypress:checkKeyCode,mouseOver:displayAllRows,click:displayAllRows,input:createPillFromSearchBySelect");

        var search_text_box = query('[id='+ ID.SEARCH_TEXT_BOX +']', baseTemplate)[0];
        search_text_box.setAttribute(attachEventId,"keypress:checkKeyCode,input:createPillFromSearchBySelect,mouseOver:displayAllRows,focus:displayAllRows");

        this.templateString = sharedUtils.domToString(baseTemplate);
        this.inherited(arguments);
      },

      checkKeyCode : function(event, searchPill) {
        var isShiftTab = false;
        var newValue;
        switch(event.charOrCode) {
          case keys.TAB :
            if(event.shiftKey){
              isShiftTab = true;
            }
            /* falls through */
          case keys.ENTER :
            if( (this.searchTextBox.get("focused")||(!searchPill && !focusUtil.curNode)) && !isShiftTab) {

              newValue = this.searchTextBox.get("value");
              var toSearchCamel = sharedUtils.toCamelCase(newValue); //to check for casesensitive cases like TYPE, Type,tYpe etc
              if (toSearchCamel.length > 0) {
                var results = this.searchByStore.query({searchBy:toSearchCamel});

                // Check if query string is a substring of a label
                if (results.length === 0){
                  var lowerCaseValue = newValue.toLowerCase();
                  for(var i=0; i < this.searchByOptions.length; i++){
                    var searchValue = this.searchByOptions[i].label;
                    // If search query is a substring of a label then re-query the search store
                    if(searchValue.toLowerCase().indexOf(lowerCaseValue) > -1){
                      if(results.length !== 0){
                        // A matching label was already found.  If there are already results, then this match means there are multiple matches.
                        // Do not autocomplete the pill because we don't want to choose which result to autocomplete for the user
                        // unless there is only 1 result.
                        return;
                      }
                      results = this.searchByStore.query({label:searchValue});
                    }
                  }
                }
                if (results.length > 0) {//value is a known keyword
                  this.__createPillFromSearchBySelect(results[0]);
                  this.__setInitialSearchByStore();
                } else { //value is not a keyword, do nothing
                  //  this.__createPillFromTextInput(event);
                  break;
                }
              }
            } else if (searchPill && event.keyCode === keys.ENTER){
                this.performSearch();
            }
            break;
          case keys.DELETE :
            if ((searchPill && searchPill.get("focused")) && (focusUtil.curNode && focusUtil.curNode.type===null)) /*need to make sure an inner component isn't what really has focus*/
              {
                this.removeSearchPill(searchPill);
                focusUtil.focus(dom.byId("search-text-box"));
              }
            break;
          case keys.BACKSPACE :
            this.currentType = null;
            newValue = this.searchTextBox.get("value");
            if ((searchPill && searchPill.get("focused")) && (focusUtil.curNode && focusUtil.curNode.type===null)){
              event.preventDefault();
              this.removeSearchPill(searchPill);
              focusUtil.focus(dom.byId("search-text-box"));
            } else if (newValue.length === 0){
              this.__setInitialSearchByStore();
            }
            break;
          default :
            this.currentType = null;
            this.searchByStore = this.defaultSearchesStore;
            this.searchTextBox.set("store", this.searchByStore);
            newValue = this.searchTextBox.get("value");
        }
      },

      __setupMainTextArea : function(){

          this.__setInitialSearchByStore();
          this.searchTextBox.set("queryExpr","*${0}*"); /**Allows type ahead to show "contains" searches instead of just matching first letters **/

          var me=this;

          /**The autocomplete dropdown may not exist until the first time it's opened, so add the onClick event after we know it exists.*/
          var onOpenHandle = aspect.after(this.searchTextBox, "openDropDown", function() {
                  onOpenHandle.remove();
                  me.searchTextBox.dropDown.on("click", function(node){
                    me.__updateStoreAndInput();
                    me.performSearch();
                  });

//                  Set tabIndex=-1 for the hidden previous & next buttons or batchScan will fail with a false positive
                  domAttr.set(me.searchTextBox.dropDown.previousButton.id, "tabIndex", "-1");
                  domAttr.set(me.searchTextBox.dropDown.nextButton.id, "tabIndex", "-1");
              });

          aspect.after(this.searchTextBox, "openDropDown", function() {
            // fix batchScan error with tabindex not being set
            for (var i = 0; i < me.searchTextBox.dropDown.containerNode.childElementCount; i++) {
              domAttr.set(me.searchTextBox.dropDown.containerNode.childNodes[i], "tabindex", "-1");
            }
          });

          aspect.after(this, "__createPillFromSearchBySelect", function() {
            if(me.searchByStore === me.recentSearchesStore) {
              // We do not want this logic to fire when we are trying
              // to bring up the recent search history popup
              return;
            }
            var onAddHandle = aspect.after(this.searchTextBox, "onSearch", function() {
                onAddHandle.remove();
              });
          });

          // aspect.after(this.searchTextBox, "_openResultList", function() {
          //   // This logic is to add a custom banner to the popup when displaying search history
          //   var bannerId = ID.getRecentSearchHistoryBanner();
          //   if(me.searchByStore === me.recentSearchesStore) {
          //     // The popup for search history.  Let's add a banner to describe the popup
          //     me.__createRecentSearchBanner(me.searchTextBox.dropDown);
          //
          //   } else {
          //     // The popup for autocomplete.  Let's add a banner to describe the popup
          //     me.__hideRecentSearchBanner(me.searchTextBox.dropDown);
          //   }
          // });

          this.searchTextBox._setItemAttr = function(/*item*/ item, /*Boolean?*/ priorityChange, /*String?*/ displayedValue){
            if (me.searchByStore === me.recentSearchesStore) {
              // must be picking a recent search from the list
              // clear any existing pills
              me.resetSearchBox();
              // add pills for the selected search
              this.set('value', "", priorityChange, displayedValue, item);
            } else {
              this.inherited('_setItemAttr', arguments);
            }
          };

          this.searchTextBox._autoCompleteText = function(/*String*/ text) {
            // We are disabling dojo's _AutoCompleterMixin _autoCompleteText() method
            // specifically for the recent search history popup.

            // When the recent search history popup appears, end users can use the
            // up and down arrow keys to traverse the rows in the popup.  When traversing,
            // dojo's _autoCompleteText() method takes the focused row's content
            // and injects the row's content into the search box field.  When this happens,
            // our search box displays [Object object], which is undesirable.  The _autoCompleteText()
            // also wipes out any existing search pills in the search box, which is bad.
            // This is all happening before, the customer chooses a row to use by mouse click or hitting enter key.
            // If we want this behavior, we will need to overwrite _autoCompleteText() so that it can
            // handle displaying our search pills rather than [Object object].  For now, disable it all together.
          };

          /**Override labelFunc to control layout of dropdown during type ahead.
           * This function returns either text or html to insert as the menu item **/
          this.searchTextBox.labelFunc=function(item, store){
            if (store === me.recentSearchesStore) {
              // it is an array so need to build pills
              var pillPane = new ContentPane();
              for (var i = 0; i < item.label.length; i++) { /* jshint loopfunc:true */
                var searchPill = new JavaBatchSearchPill({
                  searchBy: item.label[i].searchBy,
                  value: item.label[i].value,
                  ariaLabel : item.label[i].value,
                  checkForInvalidCombinations_All: function() {
                    // We do not need to do validation for the pills in the
                    // search history popup.  If we do, this method will
                    // clear the search results view, which we want to keep.
                  }
                });
                pillPane.addChild(searchPill);
              }
              return pillPane.containerNode.innerHTML;
            }
            /**TODO For now, adding html from SearchDropDown template here... will move out later */
            var innerDivs =  "";
            var outerClass = "resourceAutocompleteOuter";
            if(item.isDefault || (item.searchByLabel && item.searchByLabel !== me.currentType)){
              outerClass += " resourceAutocompleteOuterBorder";

              innerDivs += "<div class='resourceAutocompleteLeft' style='width:"+me.labelWidth+"px;'>"+ item.searchByLabel+ "</div>";

              if(!item.isDefault) {
                me.currentType = item.searchByLabel;
              } else { //if this is a default type, set to null. this is assuming default types are all unique
                me.currentType = null;
              }
            }else{
              innerDivs += "<div class='resourceAutocompleteLeft' style='width:"+me.labelWidth+"px;'>&nbsp;</div>";
            }

            var tabbed = "";
            var iconDiv = "<div ";
            if(item.typeIconClass){
              iconDiv += "class='"+item.typeIconClass+"'";
              tabbed = item.typeIconClass;
            }
            iconDiv += "></div>";

            var itemLabel=item.label;

            if(item.isDefault){
              itemLabel="<strong>"+item.label+":</strong>";
            }else{
              itemLabel = me.__setAutocompleteLabelHighlight(this, itemLabel);
            }

            var output = "<div class='"+outerClass+"'>" +
                              innerDivs +
                            "<div class='resourceAutocompleteRight'>"+
                                iconDiv+
                                "<span dir='" + sharedUtils.getStringTextDirection(item.label) + "' class='"+tabbed+"'>"+
                                itemLabel+
                                "</span>"+
                             "</div>"+
                          "</div>";
              return output;
          };
        },

      __setInitialSearchByStore : function(){
        //using searchBy for identifier, searchByLabel is what's displayed in autocomplete popup
        this.searchByOptions = [
          {label:i18n.INSTANCE_STATE, searchBy:"instanceState", searchByLabel:i18n.INSTANCE_STATE, value:"", isDefault:true},
          {label:i18n.INSTANCE_ID, searchBy:"jobInstanceId", searchByLabel:i18n.INSTANCE_ID, value:"", isDefault:true},
          {label:i18n.LAST_UPDATE, searchBy:"lastUpdate", searchByLabel:i18n.LAST_UPDATE, value:"", isDefault:true},
          {label:i18n.JES_JOB_NAME, searchBy:"jesJobName", searchByLabel:i18n.JES_JOB_NAME, value:"", isDefault:true},
          {label:i18n.JES_JOB_ID, searchBy:"jesJobId", searchByLabel:i18n.JES_JOB_ID, value:"", isDefault:true},
          {label:i18n.LAST_UPDATE_RANGE, searchBy:"lastUpdateRange", searchByLabel:i18n.LAST_UPDATE_RANGE, value:"", isDefault:true},
          {label:i18n.SUBMITTER, searchBy:"submitter", searchByLabel:i18n.SUBMITTER, value:"", isDefault:true},
          {label:i18n.APPLICATION_NAME, searchBy:"appName", searchByLabel:i18n.APPLICATION_NAME, value:"", isDefault:true},
          {label:i18n.BATCH_JOB_NAME, searchBy:"jobName", searchByLabel:i18n.BATCH_JOB_NAME, value:"", isDefault:true}
        ];

        this.defaultSearchesStore = new Memory({data:this.searchByOptions});
        this.searchByStore = this.defaultSearchesStore;

        this.__setAutocompleteInstanceStateContent();

        this.searchTextBox.set("store", this.searchByStore);
        this.currentType = null;
        this.searchTextBox.set("value","");

        var elem = document.createElement("div");
        elem.className = "resourceAutocompleteOuter";
        elem.innerHTML = "<div class='resourceAutocompleteLeft'></div>";
        var elemInner = elem.firstChild;
        document.body.appendChild(elem);
        var style = getComputedStyle(elemInner);
        this.labelWidth = parseInt(style.width, 10);
        var fontSize = style.getPropertyValue('font-size');
        //go through each label, check the width (based on font) and update colWidth if necessary
        for (var x = 0; x < this.searchByOptions.length; x++){
          var label = this.searchByOptions[x].searchByLabel;
          elemInner.innerHTML = label;
          if(elemInner.offsetWidth > this.labelWidth){
            this.labelWidth = elemInner.offsetWidth;
          }
        }
        document.body.removeChild(elem);
      },

      __setAutocompleteInstanceStateContent : function(){
        var searchBy = "instanceState";
        var searchByLabel = i18n.INSTANCE_STATE;

        var states = [
          [i18n.SEARCH_RESOURCE_TYPE_ALL, "all", false],
          [i18n.QUEUED, "queued", false],
          [i18n.SUBMITTED, "submitted", true],
          [i18n.JMS_QUEUED, "jmsQueued", true],
          [i18n.JMS_CONSUMED, "jmsConsumed", true],
          [i18n.DISPATCHED, "dispatched", false],
          [i18n.ENDED, "ended", false],
          [i18n.FAILED, "failed", true],
          [i18n.STOPPED, "stopped", true],
          [i18n.COMPLETED, "completed", true],
          [i18n.ABANDONED, "abandoned", true]
        ];

        for (var index = 0; index < states.length; index++){
          var state = states[index];
          var tabbed = false;
          if(this.searchByStore.query({label:state[0], searchBy: searchBy}).total === 0){
            tabbed = (state[2]) ? "tabbedState" : "";
            this.searchByStore.put({label:state[0], searchByLabel:searchByLabel, searchBy: searchBy, value: state[1], typeIconClass:tabbed});
          }
        }
      },

      __setAutocompleteLabelHighlight : function(node,label){
          var newValue = node.get("value");
          var regex = new RegExp( '(' + newValue + ')', 'gi' );
          return label.replace(regex, "<strong>$1</strong>");
        },
      __updateStoreAndInput : function(){
          var selectedItem = this.searchTextBox.get("item"); //get selected item from recent/autocomplete list
          if (this.searchByStore === this.recentSearchesStore && selectedItem) { // this is a saved/recent search
            selectedItem.label.forEach(lang.hitch(this, function(selectedItem){
              this.__createPillFromSearchBySelect(selectedItem);
            }));
          }
          else if(selectedItem) { //item exists in autocomplete list
              if(selectedItem.isDefault) {
                this.__createPillFromSearchBySelect(selectedItem);
              }else {
                this.__createPillFromSearchBySelect(selectedItem);
              }
          }
        },

      __createPillFromTextInput : function(event, isShiftTab){
        var searchValue = this.searchTextBox.get("value");
        var isEmpty = true;
        searchValue.trim();

        if(searchValue.length>0) {
          isEmpty = false;
        }

        if(!isEmpty || (isEmpty && !this.searchPillPane.hasChildren())){ //--> use this to stop empty search pills when others exist
          var pill = this.addNewSearchPill();
          this.__setFocusOnSearchPillOrBox(pill,isShiftTab);
        }

        this.searchTextBox.set("value","");

        return isEmpty;
      },

      __createPillFromSearchBySelect : function(item){
        var pill = this.addNewSearchPill(item);
        this.searchTextBox.set("value","");
        this.__setFocusOnSearchPillOrBox(pill);
      },

      createPillFromSearchBySelect : function(event, calledFromPillInput){
          if(event.keyCode === keys.ENTER){
            event.preventDefault();
            if(!calledFromPillInput) { //only if Enter is pressed from outside a pill
              this.__updateStoreAndInput();
            }
            this.checkKeyCode(event);

            this.performSearch();
          }
       },

      // This method is to deal with the complications with finding the corresponding
      // translated message that is mapped to a particular search pill
      buildListOfDuplicatePills : function(showErrorForSearchPill) {
        var list = "";
          for(var key in showErrorForSearchPill) {
            if(showErrorForSearchPill[key]) {
              var temp = "";
              if(list.length > 0) {
                list += ", ";
              }
              if(key === "singleDate") {
                temp = i18n.LAST_UPDATE;
              } else if(key === "rangeDate") {
                temp = i18n.LAST_UPDATE_RANGE;
              } else if(key === "submitter") {
                temp = i18n.SUBMITTER;
              } else if(key === "appName") {
                temp = i18n.APPLICATION_NAME;
              } else if(key === "jobName") {
                temp = i18n.BATCH_JOB_NAME;
              }
              list += temp;
            }
          }
        return list;
      }

  });
  return JavaBatchSearchBox;

});
