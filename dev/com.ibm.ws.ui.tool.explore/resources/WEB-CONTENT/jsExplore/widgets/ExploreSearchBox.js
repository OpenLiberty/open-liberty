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
        'jsExplore/utils/ID',
        "jsShared/utils/userConfig",
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
        "jsExplore/widgets/ExploreSearchPill",
        "dijit/form/TextBox",
        "dijit/layout/ContentPane",
        "dojo/store/Memory",
        "jsExplore/views/viewFactory",
        "dojo/i18n!../nls/explorerMessages",
        "jsExplore/resources/resourceManager",
        "jsExplore/resources/viewToHash",
        "dojo/Deferred",
        "dojo/dom-attr",
        "jsShared/utils/utils",
        "jsShared/search/SearchBox",
        "dojo/dom-construct",
        "dojo/query"
        ], function(
                ID,
                userConfig,
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
                ExploreSearchPill,
                TextBox,
                ContentPane,
                Memory,
                viewFactory,
                i18n,
                resourceManager,
                viewToHash,
                Deferred,
                domAttr,
                utils,
                SearchBox,
                domConstruct,
                query
            ){

    var ExploreSearchBox = declare("ExploreSearchBox", [ SearchBox ], {
        searchByStore : null,
        currentType : null,
        labelWidth : 75,
        MAX_NUM_RESOURCES_FOR_AUTOCOMPLETE: 250,

        postCreate : function() {
            var me = this;
            aspect.after(this.searchPillPane, "resize", function(){
              me.__calculateNumRowsNeededToShowAll();
            });

            // Initialize the utility that will read/write persisted data
            // (aka user's driven configuration)
            userConfig.init("com.ibm.websphere.appserver.adminCenter.tool.explore");

            // Get the user past search history,
            // and update recent searches local cache.
            userConfig.load(lang.hitch(this, function(response) {
                if (response.recentSearches) {
                  // build the recent searches pill list
                  this.recentSearches = response.recentSearches;
                }
              }), function(err){
                console.log("No recent search history");
            });
            this.__setupMainTextArea();
        },

        checkKeyCode : function(event, searchPill){
            var isShiftTab = false;
            switch(event.charOrCode){
              case keys.TAB :
                if(event.shiftKey)
                  isShiftTab = true;
              case '|' :
              case ',' :
              case '/' :
              case ':' :
              case keys.ENTER :
                /*There seems to be a dojo bug similar to #15133, that displays a node as having focus
                 * (in this case the cursor is in the text box), but focusUtil.curNode sometimes returns null,
                 * (in this case, consistently returns null when the search widget is accessed directly via
                 * url: /devExplore/#explore/search).  Adding what (should be) an unnecessary condition to if stmt*/
                //console.log("focus: "+focusUtil.curNode);

                if((this.searchTextBox.get("focused")||(searchPill==null&&focusUtil.curNode==null))&&!isShiftTab){

                    var newValue = this.searchTextBox.get("value");
                    var toSearchCamel = utils.toCamelCase(newValue); //to check for casesensitive cases like TYPE, Type,tYpe etc
                    var results = this.searchByStore.query({value:toSearchCamel});
                    event.preventDefault();

                    if(results.length>0) {//value is a known keyword
                      this.__createPillFromSearchBySelect(results[0].value);
                      this.__setInitialSearchByStore();
                    } else { //value is not a keyword, so create a search pill with name=value (name will by Any in the future)
                      if (toSearchCamel.length > 0) { // input entered
                        this.__createPillFromTextInput(event);
                      } else { // no input entered
                        if(event.keyCode == keys.TAB) {
                          this.searchPaneAddButton.focus();
                        }
                      }

                    }
                }else if(isShiftTab){
                    var newValue = this.searchTextBox.get("value");
                    if(newValue.length>0){
                      event.preventDefault();
                      this.__createPillFromTextInput(event,isShiftTab);
                    }
                }else if (searchPill != null && event.keyCode == keys.ENTER){
                    this.performSearch();
                }
                break;
              case keys.DELETE :
                if((searchPill != null && searchPill.get("focused")) && (focusUtil.curNode !=null && focusUtil.curNode.type==null)) /*need to make sure an inner component isn't what really has focus*/
                  {
                    this.removeSearchPill(searchPill);
                    focusUtil.focus(dom.byId("search-text-box"));
                  }
                break;
              case keys.BACKSPACE :
                this.currentType = null;
                var newValue = this.searchTextBox.get("value");
                if((searchPill != null && searchPill.get("focused")) && (focusUtil.curNode !=null && focusUtil.curNode.type==null)){
                  event.preventDefault();
                  this.removeSearchPill(searchPill);
                  focusUtil.focus(dom.byId("search-text-box"));
                }else if(newValue.length ==1){/**TODO - determine why "value" is 1 character behind. this really should be ==0  */
                    this.__setInitialSearchByStore();
                }
                break;
              default :
                this.currentType = null;
                this.searchByStore = this.defaultSearchesStore;
                this.searchTextBox.set("store", this.searchByStore);
                var newValue = this.searchTextBox.get("value");
                if(newValue.length ==0){/**TODO can/will change to 1, 2, etc. if needed for performance **/
                  if(searchPill != null) {
                    this.__performAutocompleteForSearchPillInput(event.charOrCode, searchPill);                  
                  } else {
                    this.__performAutocomplete(event.charOrCode);
                  }
                }
            }
        },

        showRecentSearches : function(event) {
          if(event != null && event.type == "click") {
            this.__setInitialSearchByStore();
            this.searchTextBox.loadAndOpenDropDown();
          }
        },

        createPillFromSearchBySelect : function(event, calledFromPillInput){
          if(event.keyCode== keys.ENTER){
            event.preventDefault();
            if(!calledFromPillInput) { //only if Enter is pressed from outside a pill
              this.__updateStoreAndInput();
            }
            this.checkKeyCode(event);

            this.performSearch();
          }
       },

        setFocus : function(){ //onFocus and onBlur appear to not send an event object, otherwise this would call displayAllRows
          this.keepSearchAreaExpanded = true;
          this.displayAllRows(null);
        },

        removeFocus : function(){ //onFocus and onBlur appear to not send an event object, otherwise this would call displayAllRows
          this.keepSearchAreaExpanded = false;
          this.hideExtraRows(null);
        },

        hideExtraRows : function(event){
          if(event!=null&&event.type=="blur"){
            this.keepSearchAreaExpanded = false;
          }

          if(this.numRowsNeededToShowAll>1&&!this.keepSearchAreaExpanded){ //this should prevent unnecessarily trying to resize
            this.__showMultipleRows(false);
          }
          this.inMouseOver=false;
        },

        resetSearchBox : function (){
          var me = this;
          this.searchPillPane.getChildren().forEach( function(searchPill, i){
            me.removeSearchPill(searchPill);
          });
          this.totalNumSearchPillsCreated = 0;
          this.__calculateNumRowsNeededToShowAll();
//          this.__setInitialSearchByStore();
          this.__showPillOverlay(false);
          viewFactory.searchView.clearSearchResults();
          viewToHash.updateHash(viewToHash.getHash() + "/search");
          //viewFactory.searchView.clearErrorMessagePane();
          this.__setFocusOnSearchPillOrBox();

          /**Design is now asking to remove the "Search Results" button. Leaving here in case it changes back
          registry.byId('breadcrumbPane-id').setSearchBreadCrumb(false) ;
          **/
        },

        searchTextBoxBlur : function(event){
          this.removeFocus();
        },

        /**
         * Add new searchPill with default Name/textbox. Specifying a searchby and value, when a user clicks an item on the
         * search view, will show the appropriate input types
         * @param searchby - String - must be exact match to searchByDropDownMenu menuItem values; currently
         *                  'name', 'type','state'
         * @param value - String - can be any acceptable value for name, but must be exact match if searchby is 'type' or 'state'
         *                If searchby is 'type', the possible values for resourceTypeDropDownMenu menuItems are:
         *                    'all', 'applications', 'servers', 'clusters', 'hosts'
         *                If searchby is 'state', the possible values for resourceTypeDropDownMenu menuItems are:
         *                    'all', 'STARTED', 'STOPPED', 'UNKNOWN'

         */

        populateSearchPillPane : function (options){ /**Arrays of values**/
          this.resetSearchBox();
          for (var option in options) {
            var value = options[option];
            this.__addPill(option, options[option]);
          }
        },

        __addPill : function(searchBy, option){
          if (option != null){
            if (!("string" == typeof option)){
              for (var op in option) {
                var value = option[op];
                if (value.indexOf("~eq~") === 0) {
                  value = value.substring(4);
                }
                this.addNewSearchPill(searchBy, value);
              }
            } else{
              if (option.indexOf("~eq~") === 0) {
                option = option.substring(4);
              }
              this.addNewSearchPill(searchBy, option);
            }
          }
        },

        addNewSearchPill : function (searchBy, value){
            if(typeof value === "undefined") {
              // We cannot have value undefined or the template mixin for
              // search pills will break
              value = null;
            }
            var searchPill = new ExploreSearchPill({
              id:"search-searchPill"+this.totalNumSearchPillsCreated,
              searchBy: searchBy,
              value: value
            });

            this.totalNumSearchPillsCreated++;

            this.searchPillPane.addChild(searchPill);
            searchPill.checkForInvalidCombinations_All();

            this.__calculateNumRowsNeededToShowAll();
            this.displayAllRows(null,true);

            return searchPill;

        },

        removeSearchPill : function (searchPill){
          this.inherited(arguments);
          viewFactory.searchView.clearSearchResults();
        },

        performSearch : function(){
          var me = this;
          var type = [];
          var state = [];
          var name = [];
          var tag = [];
          var runtimeType = [];
          var container = [];
          var owner = [];
          var contact = [];
          var note = [];
          var allPillsValid = true;
          var invalidErrorMsg = "";
          var saveSearch = [];

          this.searchPillPane.getChildren().forEach( function(searchPill, i){
              if(!searchPill.isValid){
                allPillsValid=false;
                invalidErrorMsg += buildSearchErrorPane();
              }
              var searchBy = searchPill.selectedSearchByMenuItem.value;
              var input = registry.byId(searchPill.id + "_" + searchBy + "-input");
              saveSearch.push({searchBy:searchBy, value:input.value});
              switch(searchBy) {
                  case 'name' :
                    name[name.length] = input.value;
                    break;
                  case 'type' :
                    type[type.length] = input.value;
                    break;
                  case 'state' :
                    state[state.length] = input.value;
                    break;
                  case 'tag' :
                    // For Tags, we want to default to an exact search instead of contains.
                    if (input.value.indexOf("~eq~") === 0 || input.value.indexOf("~neq~") === 0 || input.value.indexOf("~sw~") === 0 || input.value.indexOf("~nsw~") === 0 || input.value.indexOf("~ew~") === 0 || input.value.indexOf("~new~") === 0 || input.value.indexOf("~has~") === 0 || input.value.indexOf("~nhas~") === 0) {
                      tag[tag.length] = input.value;
                    } else {
                      tag[tag.length] = "~eq~" + input.value;
                    }
                    break;
                  case 'runtimeType' :
                    runtimeType[runtimeType.length] = input.value;
                    break;
                  case 'container' :
                    container[container.length] = input.value;
                    break;
                  case 'owner' :
                    owner[owner.length] = input.value;
                    break;
                  case 'contact' :
                    contact[contact.length] = input.value;
                    break;
                  case 'note' :
                    note[note.length] = input.value;
                    break;
                }
            });

          if(saveSearch.length !== 0) { // check if non-pill were input
            // handle adding/replacing this search in the recent search list and save it
            this.__updateRecentSearches(saveSearch);
          }

          var searchView = viewFactory.searchView;
          if(allPillsValid) {
                if(type.length==0){
                  type[0] = "";
                }
                if(state.length==0){
                  state[0] = "";
                }
                if(name.length==0){
                  name[0] = "";
                }
                if(tag.length==0){
                  tag[0] = "";
                }
                if(runtimeType.length==0){
                  runtimeType[0] = "";
                }
                if(container.length==0){
                  container[0] = "";
                }
                if(owner.length==0){
                  owner[0] = "";
                }
                if(contact.length==0){
                  contact[0] = "";
                }
                if(note.length==0){
                  note[0] = "";
                }

                // Check to see if the search parameters exist
                var searchParamsExist = (type[0] !== "") || (state[0] !== "") || (name[0] !== "") || (tag[0] !== "") || (runtimeType[0] !== "") || (container[0] !== "") || (owner[0] !== "") || (contact[0] !== "") || (note[0] !== "");

                if (searchParamsExist) {
                  searchView.loading(true);
                  var searchResults = resourceManager.getSearchResults({"type":type, "state":state, "name":name, "tag":tag, "runtimeType":runtimeType, "container":container, "owner":owner, "contact":contact, "note":note});
                  searchResults.then(function(resources) {
                      searchView.loading(false);
                      searchView.refresh(resources.apps, resources.servers, resources.clusters, resources.hosts, resources.runtimes);
                      me.searchTextBox.set("value","");
                  });
                }
          } else {
              this.createSearchErrorPane(invalidErrorMsg);
          }
        },

        clearErrorMsgAndSearchView : function(){
          viewFactory.searchView.clearSearchResults();
        },

        createSearchErrorPane : function(invalidErrorMsg) {
          var searchView = viewFactory.searchView;
          searchView.refresh({}, {}, {}, {}, {}, {});
          var summaryPane = registry.byId("searchView-SummaryPane");
          var errorMsgPane = registry.byId("searchView-ErrorMessagePane");
          if (summaryPane) {
            summaryPane.set("style", {
              "display":"block"
            });
            summaryPane.set('class', "invalidSearchDiv");
            summaryPane.set('content', i18n.SEARCH_CRITERIA_INVALID);
          }
          if (errorMsgPane) {
            errorMsgPane.set("style", {
              "display":"block"
            });
            errorMsgPane.set('content', invalidErrorMsg);
          }
        },

        buildSearchErrorPane : function() {
          var searchInvalidErrorMsg = "";
          this.searchPillPane.getChildren().forEach(function(searchPill, i) {
            if (!searchPill.isValid) {
              searchInvalidErrorMsg += searchPill.inValidErrorMessage + "<br>";
            }
          });
          return searchInvalidErrorMsg;
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

          aspect.after(this.searchTextBox, "openDropDown", function() {
            // fix batchScan error with tabindex not being set
            for (var i = 0; i < me.searchTextBox.dropDown.containerNode.childElementCount; i++) {
              domAttr.set(me.searchTextBox.dropDown.containerNode.childNodes[i], "tabindex", "-1");
            }

            // This logic is to add a custom banner to the popup when displaying search history
            var bannerId = ID.getRecentSearchHistoryBanner();
            if(me.searchByStore === me.recentSearchesStore) {
              // The popup for search history.  Let's add a banner to describe the popup
              me.__createRecentSearchBanner(me.searchTextBox.dropDown);
            } else {
              // The popup for autocomplete.  Let's add a banner to describe the popup
              me.__hideRecentSearchBanner(me.searchTextBox.dropDown);
            }
          });

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
          }

          /**Override labelFunc to control layout of dropdown during type ahead.
           * This function returns either text or html to insert as the menu item **/
          this.searchTextBox.labelFunc=function(item, store){
            if (store === me.recentSearchesStore) {
              // it is an array so need to build pills
              var pillPane = new ContentPane();
              for (var i = 0; i < item.label.length; i++) {
                var searchPill = new ExploreSearchPill({
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
            if(item.isDefault || (item.searchByLabel!=null && item.searchByLabel!=me.currentType)){
              outerClass += " resourceAutocompleteOuterBorder";

              innerDivs += "<div class='resourceAutocompleteLeft' style='width:"+me.labelWidth+"px;'>"+ item.searchByLabel+ "</div>";

              if(!item.isDefault) {
                me.currentType = item.searchByLabel;
              }
            }else{
              innerDivs += "<div class='resourceAutocompleteLeft' style='width:"+me.labelWidth+"px;'>&nbsp;</div>";
            }

            var iconDiv = "<div ";
            if(item.typeIconClass){
              iconDiv += "class='"+item.typeIconClass+"'";
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
                                "<span dir='" + utils.getStringTextDirection(item.label) + "'>"+
                                itemLabel+
                                "</span>"+
                             "</div>"+
                          "</div>";

              return output;
          };
        },

        __setInitialSearchByStore : function(){
          /**TODO - would prefer to pull this list instead of duplicating list of options,
           * but this searchby list currently gets created in a searchpill and searchpills
           *  may not always be present
           *
           *   ...can add static list in searchpill
           */
          var searchByOptions = [
            {label:i18n.SEARCH_RESOURCE_NAME, searchByLabel:i18n.SEARCH_RESOURCE_NAME , value:"name", isDefault:true},
            {label:i18n.SEARCH_RESOURCE_TYPE, searchByLabel:i18n.SEARCH_RESOURCE_TYPE , value:"type", isDefault:true},
            {label:i18n.SEARCH_RESOURCE_STATE, searchByLabel:i18n.SEARCH_RESOURCE_STATE , value:"state", isDefault:true},
            {label:i18n.SEARCH_RESOURCE_TAG, searchByLabel:i18n.SEARCH_RESOURCE_TAG, value:"tag", isDefault:true},
            {label:i18n.SEARCH_RESOURCE_RUNTIMETYPE, searchByLabel:i18n.SEARCH_RESOURCE_RUNTIMETYPE, value:"runtimeType", isDefault:true},
            {label:i18n.SEARCH_RESOURCE_CONTAINER, searchByLabel:i18n.SEARCH_RESOURCE_CONTAINER, value:"container", isDefault:true},
            {label:i18n.SEARCH_RESOURCE_OWNER, searchByLabel:i18n.SEARCH_RESOURCE_OWNER, value:"owner", isDefault:true},
            {label:i18n.SEARCH_RESOURCE_CONTACT, searchByLabel:i18n.SEARCH_RESOURCE_CONTACT, value:"contact", isDefault:true},
            {label:i18n.SEARCH_RESOURCE_NOTE, searchByLabel:i18n.SEARCH_RESOURCE_NOTE, value:"note", isDefault:true}
          ];
          this.defaultSearchesStore = new Memory({data:searchByOptions});
          this.recentSearchesStore = new Memory({data:this.recentSearches});
          this.searchByStore = this.defaultSearchesStore;
          if (this.recentSearches && this.recentSearches.length > 0) {
            this.searchByStore = this.recentSearchesStore;
          }
          this.searchTextBox.set("store", this.searchByStore);
          this.currentType = null;
          this.searchTextBox.set("value","");

          //Figure out width needed for defaultSearches view label column
          //find font and given width
          var elem = document.createElement("div");
          elem.className = "resourceAutocompleteOuter";
          elem.innerHTML = "<div class='resourceAutocompleteLeft'></div>";
          elemInner = elem.firstChild;
          document.body.appendChild(elem);
          var style = getComputedStyle(elemInner);
          this.labelWidth = parseInt(style.width, 10);
          var fontSize = style.getPropertyValue('font-size');
          //go through each label, check the width (based on font) and update colWidth if necessary
          for (var x = 0; x < searchByOptions.length; x++){
            label = searchByOptions[x].searchByLabel;
            elemInner.innerHTML = label;
            if(elemInner.offsetWidth > this.labelWidth){
              this.labelWidth = elemInner.offsetWidth;
            }
          }
          document.body.removeChild(elem);
        },

        __performAutocomplete : function(newValue){
          /**
           * Type ahead items will be grouped by name, tag, status, etc. The following will find and populate items for each.
           * *Load "State" items
           * Note: keep a check on the behavior that could be caused by the time needed for searches sent to the server (e.g. name). This could have an impact on the order of items, the "searchBy" var, etc.  */
          var searchBy = i18n.SEARCH_RESOURCE_STATE;
          var searchByValue = "state";

          // Check if each of the labels and search type already exist in the search store for each drop down before adding them
          if(this.searchByStore.query({label:i18n.SEARCH_RESOURCE_TYPE_ALL, searchByValue: searchByValue}).total === 0){
            this.searchByStore.put({label:i18n.SEARCH_RESOURCE_TYPE_ALL, searchByLabel:searchBy, searchByValue: searchByValue, value:"all"});
          }
          if(this.searchByStore.query({label:i18n.RUNNING, searchByValue: searchByValue}).total === 0){
            this.searchByStore.put({label:i18n.RUNNING, searchByLabel:searchBy, searchByValue: searchByValue, value:"STARTED", typeIconClass:"runningMenuItemIcon"});
          }
          if(this.searchByStore.query({label:i18n.STOPPED, searchByValue: searchByValue}).total === 0){
            this.searchByStore.put({label:i18n.STOPPED, searchByLabel:searchBy, searchByValue: searchByValue, value:"STOPPED", typeIconClass:"stoppedMenuItemIcon"});
          }
          if(this.searchByStore.query({label:i18n.UNKNOWN, searchByValue: searchByValue}).total === 0){
            this.searchByStore.put({label:i18n.UNKNOWN, searchByLabel:searchBy, searchByValue: searchByValue, value:"UNKNOWN", typeIconClass:"unknownMenuItemIcon"});
          }

          searchBy = i18n.SEARCH_RESOURCE_CONTAINER;
          searchByValue = "container";

          if(this.searchByStore.query({label:i18n.SEARCH_RESOURCE_CONTAINER_DOCKER}).total === 0){
            this.searchByStore.put({label:i18n.SEARCH_RESOURCE_CONTAINER_DOCKER, searchByLabel:searchBy , searchByValue: searchByValue, value:"all"});
          }

          searchBy = i18n.SEARCH_RESOURCE_RUNTIMETYPE;
          searchByValue = "runtimeType";

          if(this.searchByStore.query({label:i18n.CONTAINER_LIBERTY, searchByValue: searchByValue}).total === 0){
            this.searchByStore.put({label:i18n.CONTAINER_LIBERTY, searchByLabel:searchBy , searchByValue: searchByValue, value:"liberty"});
          }
          if(this.searchByStore.query({label:i18n.CONTAINER_NODEJS, searchByValue: searchByValue}).total === 0){
            this.searchByStore.put({label:i18n.CONTAINER_NODEJS, searchByLabel:searchBy , searchByValue: searchByValue, value:"Node.js"});
          }

          searchBy = i18n.SEARCH_RESOURCE_TYPE;
          searchByValue = "type";

          // Check for searchByValue for state:all and type:all because State and Type share this label
          if(this.searchByStore.query({label:i18n.SEARCH_RESOURCE_TYPE_ALL, searchByValue: searchByValue}).total === 0){
            this.searchByStore.put({label:i18n.SEARCH_RESOURCE_TYPE_ALL, searchByLabel:searchBy , searchByValue: searchByValue, value:"all"});
          }
          if(this.searchByStore.query({label:i18n.APPLICATION, searchByValue: searchByValue}).total === 0){
            this.searchByStore.put({label:i18n.APPLICATION, searchByLabel:searchBy , searchByValue: searchByValue, value:"application", typeIconClass:"applicationsMenuItemIcon"});
          }
          if(this.searchByStore.query({label:i18n.SERVER, searchByValue: searchByValue}).total === 0){
            this.searchByStore.put({label:i18n.SERVER, searchByLabel:searchBy , searchByValue: searchByValue, value:"server", typeIconClass:"serversMenuItemIcon"});
          }
          if(this.searchByStore.query({label:i18n.CLUSTER, searchByValue: searchByValue}).total === 0){
            this.searchByStore.put({label:i18n.CLUSTER, searchByLabel:searchBy , searchByValue: searchByValue, value:"cluster", typeIconClass:"clustersMenuItemIcon"});
          }
          if(this.searchByStore.query({label:i18n.HOST, searchByValue: searchByValue}).total === 0){
            this.searchByStore.put({label:i18n.HOST, searchByLabel:searchBy , searchByValue: searchByValue, value:"host", typeIconClass:"hostsMenuItemIcon"});
          }
          if(this.searchByStore.query({label:i18n.RUNTIME, searchByValue: searchByValue}).total === 0){
            this.searchByStore.put({label:i18n.RUNTIME, searchByLabel:searchBy , searchByValue: searchByValue, value:"runtime", typeIconClass:"runtimesMenuItemIcon"});
          }

          if(!this.searchByStore){
            this.__setupMainTextArea();
          }
          this.__setAutocompleteTagContent(this.searchByStore, newValue);
          this.__setAutocompleteNameContent(this.searchByStore, newValue);

        },

        __performAutocompleteForSearchPillInput : function(newValue,searchPill){
           var inputStore = new Memory();
           var searchBy = searchPill.selectedSearchByMenuItem.value;
           var me = this;
           switch(searchBy) {
           case 'name' :
           case 'tag' :
//           Enable auto-complete for the following three (need API from SM similar to their getTags())
//           case 'owner' :
//           case 'contact' :
//           case 'note' :
             this['__setAutocomplete' + searchBy.charAt(0).toUpperCase() + searchBy.slice(1) + 'Content'](inputStore, newValue);
             searchPill[searchBy + 'Input'].set("queryExpr","*${0}*"); /**Allows type ahead to show "contains" searches instead of just matching first letters **/
             searchPill[searchBy + 'Input'].set("store",inputStore);
             var setTabIndex = true;
             searchPill[searchBy + 'Input'].doHighlight = function(label, find){
               // On the first pass, set tabIndex=-1 for the hidden previous & next buttons or batchScan will fail with a false positive
               if (setTabIndex) {
                 domAttr.set(this.dropDown.previousButton, "tabIndex", "-1");
                 domAttr.set(this.dropDown.nextButton, "tabIndex", "-1");
                 setTabIndex = false;
               }

               return me.__setAutocompleteLabelHighlight(this, label);             
             };
             aspect.after(searchPill[searchBy + 'Input'], "openDropDown", function() {
               // fix batchScan problem with tabindex not being set
               for (var i = 0; i < this.dropDown.containerNode.childElementCount; i++) {
                 domAttr.set(this.dropDown.containerNode.childNodes[i], "tabindex", "-1");
               }
             });
             break;
           }
        },

        __setAutocompleteNameContent : function(store, newValue){
          this.__turnOnAutocompleteForName().then(function(okay){
            if(okay) {
              var searchBy = i18n.SEARCH_RESOURCE_NAME;
              var searchByValue = "name";

              var url = '/ibm/api/collective/v1/search?name='+encodeURIComponent(newValue);
              var options = {
                  handleAs : 'json',
                  preventCache : true,
                  headers : {
                    'Content-type' : 'application/json'
                  }
              };
              request(url, options).then(function(resources) {
                if (resources){
                  if (resources.applications && resources.applications.list){
                    resources.applications.list.forEach(function(app, i){
                      if(store.query({label:app.name, searchByValue: searchByValue}).total === 0){
                        store.put({label:app.name, value:app.name, type:"app", typeIconClass:"applicationsMenuItemIcon", searchByLabel: searchBy, searchByValue: searchByValue});
                      }
                    });
                  }
                  if (resources.servers && resources.servers.list){
                    resources.servers.list.forEach(function(server, i){
                      if(store.query({label:server.name, searchByValue: searchByValue}).total === 0){
                        store.put({label:server.name, value:server.name, type:"server", typeIconClass: "serversMenuItemIcon",searchByLabel: searchBy, searchByValue: searchByValue});
                      }
                    });
                  }
                  if (resources.hosts && resources.hosts.list){
                    resources.hosts.list.forEach(function(host, i){
                      if(store.query({label:host.name, searchByValue: searchByValue}).total === 0){
                        store.put({label:host.name, value:host.name, type:"host", typeIconClass: "hostsMenuItemIcon",searchByLabel: searchBy, searchByValue: searchByValue});
                      }
                    });
                  }
                  if (resources.clusters && resources.clusters.list){
                    resources.clusters.list.forEach(function(cluster, i){
                      if(store.query({label:cluster.name, searchByValue: searchByValue}).total === 0){
                        store.put({label:cluster.name, value:cluster.name, type:"cluster", typeIconClass: "clustersMenuItemIcon",searchByLabel: searchBy, searchByValue: searchByValue});
                      }
                    });
                  }
                }
              });
            }
          });

        },

        __setAutocompleteTagContent : function(store, newValue){
          var searchBy = i18n.SEARCH_RESOURCE_TAG;
          var searchByValue = "tag";

          var url = '/IBMJMXConnectorREST/mbeans/WebSphere:feature=collectiveController,name=AdminMetadataManager,type=AdminMetadataManager/attributes/Tags';
          var options = {
              handleAs : 'json',
              headers : {
                'Content-type' : 'application/json'
              },
              sync: true
          };

          request.get(url, options).then(lang.hitch(this, function(response) {
              // response.value is an array of strings (tags).  Add each to the
              // dojo store to be used in the Tag ComboBoxes.
              for (var i=0; i<response.value.length; i++) {
                if(store.query({label:response.value[i], searchByValue: searchByValue}).total === 0){
                  store.put({label:response.value[i], value:response.value[i], searchByLabel: searchBy, searchByValue: searchByValue});
                }
              }

          }), lang.hitch(this, function(err) {
            console.error('Error ' + err.response.status + ' occurred when requesting ' + url + ': ', err);
          })
          );
        },

        __setAutocompleteLabelHighlight : function(node,label){
          var newValue = node.get("value");
          var regex = new RegExp( '(' + newValue + ')', 'gi' );
          return label.replace(regex, "<strong>$1</strong>");
        },

        /*Until autocomplete and performance have been properly addressed,
         * this function will use some logic to decide if autocomplete should run.
         * Currently, it checks the total # of resources and if it's over a given threshold,
         * autocomplete for resource names will not run, since this searches for all resources
         * with the specified character(s) and puts it into a Memory store for ComboBox to use.
         * */
        __turnOnAutocompleteForName : function(){
          var deferred = new Deferred();
          var me = this;


          //get total resources, then compare to MAX
         // var resourceSummary = resourceManager.getSummary();
         // console.log("ac app summary: ",resourceSummary);

          resourceManager.getSummary().then(function(summary){

            var totalServers = summary.servers.up + summary.servers.down + summary.servers.unknown;
            var totalApplications = summary.applications.up + summary.applications.down + summary.applications.unknown + summary.applications.partial;
            var totalClusters = summary.clusters.up + summary.clusters.down + summary.clusters.unknown + summary.clusters.partial;
            var totalHosts = summary.hosts.up + summary.hosts.down + summary.hosts.unknown + summary.hosts.partial + summary.hosts.empty;
            var totalResources = totalServers + totalApplications + totalClusters + totalHosts;

            console.log("ac summary total: "+totalResources+", turnOn1: "+(totalResources <= me.MAX_NUM_RESOURCES_FOR_AUTOCOMPLETE));
            var turnOn = totalResources <= me.MAX_NUM_RESOURCES_FOR_AUTOCOMPLETE;
            console.log("turnon: "+turnOn);

            deferred.resolve(turnOn);
          });
          return deferred;
        },

        __updateStoreAndInput : function(){
          var selectedItem = this.searchTextBox.get("item"); //get selected item from recent/autocomplete list
          if (this.searchByStore === this.recentSearchesStore && selectedItem) { // this is a saved/recent search
            selectedItem.label.forEach(lang.hitch(this, function(selectedItem){
              this.__createPillFromSearchBySelect(selectedItem.searchBy,selectedItem.value);
            }));
          }
          else if(selectedItem) { //item exists in autocomplete list
              if(selectedItem.isDefault) {
                this.__createPillFromSearchBySelect(selectedItem.value);
              }else {
                this.__createPillFromSearchBySelect(selectedItem.searchByValue,selectedItem.value);
              }
          }else { //no item is selected
            //do nothing - pill will be created in checkKeyCode for typed input
          }
        },

        /*TODO: compare and possibly combine the following 2 functions*/
        __createPillFromTextInput : function(event, isShiftTab){
          var searchValue = this.searchTextBox.get("value");
          var isEmpty = true;
          searchValue.trim();

          if(searchValue.length>0)
            isEmpty = false;

          if(!isEmpty || (isEmpty && !this.searchPillPane.hasChildren())){ //--> use this to stop empty search pills when others exist
            var pill = this.addNewSearchPill("name", searchValue);
            this.__setFocusOnSearchPillOrBox(pill,isShiftTab);
          }

          this.searchTextBox.set("value","");

          return isEmpty;
        },

        __createPillFromSearchBySelect : function(searchBy, value){
          var pill = this.addNewSearchPill(searchBy, value);
          this.searchTextBox.set("value","");
          this.__setFocusOnSearchPillOrBox(pill);
        },

        /**
         * Add or update the search in the recent searches list. If it is already there, simply switch the order.
         * If there are already 5 in the list (the max), add this one and remove the oldest. Then save as tool data.
         */
        __updateRecentSearches : function(saveSearch) {

          // if this search already exists simply switch the order
          var existingIndex = this.__findSearch(saveSearch, this.recentSearches);
          if (existingIndex != -1) {
            // remove this one from its current position
            this.recentSearches.splice(existingIndex,1);
          } else {
            if (this.recentSearches.length > 4) {
              // remove the last one
              this.recentSearches.pop();
            }
          }
          // add this search to recent searches as the first one
          this.recentSearches.unshift({label:saveSearch});
          userConfig.save("recentSearches", this.recentSearches);
        },

        __findSearch : function(search, searches) {
          var index = -1;
          for (var i = 0; i < searches.length; i++) {
            if (this.__compareSearch(search, searches[i].label)) {
              index = i;
              break;
            }
          }
          return index;
        },

        __compareSearch : function(s1, s2) {
          // need to compare regardless of order of labels
          var found = true;
          if (s1 && s2 && s1.length === s2.length) {
            // check to see if each criteria is in both regardless of order
            for (var i = 0; i < s1.length; i++) {
              var singleMatch = false;
              for (var j = 0; j < s2.length; j++) {
                if (s1[i].searchBy === s2[j].searchBy && s1[i].value === s2[j].value) {
                  singleMatch = true;
                  break;
                }
              }
              if (!singleMatch) {
                found = false;
              }
            }
          } else {
            found = false;
          }
          return found;
        },

        /**
         * dropDown - attach the banner to this dropdown
         */
        __createRecentSearchBanner : function(dropDown) {
          var bannerId = ID.getRecentSearchHistoryBanner();
          var banner = document.getElementById(bannerId);
          if(! banner) {  // avoid duplicating banner
            var dropDownId = dropDown.id;
            var popup = document.getElementById(dropDownId);          

            var newDiv = document.createElement("div")
            var text = document.createTextNode(i18n.SEARCH_RECENT);
            newDiv.appendChild(text);
            newDiv.className += "recentSearchHistoryBanner";
            newDiv.id = bannerId;   
 
            popup.parentNode.insertBefore(newDiv, popup);
          }
        },

        __hideRecentSearchBanner : function(dropDown) {
          var bannerId = ID.getRecentSearchHistoryBanner();
          var banner = document.getElementById(bannerId);
          if(banner) {
            banner.remove();
          }
        },

        buildRendering : function() {
          /*
           * Here we will attach all the events that are specific
           * to the search box in explore tool
           */
          var attachEventId = "data-dojo-attach-event";
          var baseTemplate = domConstruct.toDom(this.templateString);

          baseTemplate.setAttribute(attachEventId, "mouseOver:displayAllRows");

          var search_searchPill_pane = query('[id="search-searchPill-pane"]', baseTemplate)[0];
          var existingEvents = this.getExistingEvents(search_searchPill_pane, attachEventId);
          search_searchPill_pane.setAttribute(attachEventId, existingEvents + "mouseOver:displayAllRows,click:displayAllRows,focus:setFocus,mouseOut:hideExtraRows,blur:removeFocus,keypress:checkKeyCode");

          var search_text_box = query('[id="search-text-box"]', baseTemplate)[0];
          existingEvents = this.getExistingEvents(search_text_box, attachEventId);
          search_text_box.setAttribute(attachEventId, existingEvents + "blur:searchTextBoxBlur,mouseOver:displayAllRows,click:showRecentSearches,focus:displayAllRows,input:createPillFromSearchBySelect");

          this.templateString = utils.domToString(baseTemplate);
          this.inherited(arguments);
        }

    });
    return ExploreSearchBox;

});
