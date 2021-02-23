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
/* jshint laxcomma: true */
/* globals imgUtils: false */
define([ "jsShared/utils/utils", "dojo/dom-class", "dojo/_base/lang", "dijit/registry", "dojo/_base/declare",
    "dojo/text!../templates/JavaBatchSearchPill.html", "dijit/MenuItem", "dojo/i18n!../../nls/javaBatchMessages", "dojox/xml/parser",
    "jsShared/utils/ID", "dojo/query", "jsShared/search/SearchPill", "dojo/dom-construct", "dojo/keys", "dojo/aspect", "dojo/on",
    "dojo/dom", "dojo/dom-attr" ], function(sharedUtils, domClass, lang, registry, declare, JavaBatchSearchPillTemplate, MenuItem, i18n,
    xmlParser, sharedID, query, SearchPill, domConstruct, keys, aspect, on, dom, domAttr) {
  /**
   * The JavaBatchSearchPill is an extension of the SearchPill base widget.
   *
   * This widget will throw an exception if you give it bad parameters.
   */
  var JavaBatchSearchPill = declare("JavaBatchSearchPill", [ SearchPill ], {

    prevSelected : null,
    /**String value**/
    selectedSearchByMenuItem : null,
    /**MenuItem **/
    tagInputLabel : i18n.SEARCH,
    ariaLabel : i18n.SEARCH_PILL_INPUT,

    // Valid searchBy parameters
    menuOptions : [ {
      value : "instanceState",
      label : i18n.INSTANCE_STATE
    }, {
      value : "jobInstanceId",
      label : i18n.INSTANCE_ID
    }, {
      value : "lastUpdate",
      label : i18n.LAST_UPDATE
    }, {
      value : "lastUpdateRange",
      label : i18n.LAST_UPDATE_RANGE
    }, {
      value : "jesJobName",
      label : i18n.JES_JOB_NAME
    }, {
      value : "jesJobId",
      label : i18n.JES_JOB_ID
    }, {
      value : "submitter",
      label : i18n.SUBMITTER
    }, {
      value : "appName",
      label : i18n.APPLICATION_NAME
    }, {
      value : "jobName",
      label : i18n.BATCH_JOB_NAME
    }
    // ,{value:"", label:""}
    ],

    /**
     * @throws exception if parms contains unsupported searchBy parameters
     * @param params
     */
    constructor : function(params) {
      if (params.searchBy) {
        var isValidSearchBy = this.__isValidSeachBy(params.searchBy);
        if (!isValidSearchBy) {
          // I tried to make the constructor return a null because I didn't want to drive the
          // code by exception.  However, when using return, the widget life cycle continues
          // and postCreate still runs.  I want this code to fail fast when bad input is given.
          // So throwing an exception to fail fast.
          throw "Bad search by parameter!"; // FIXME: change this to use tr.js once work item 226366 is delivered.
        }
      }
    },

    /**
     * Return true or false when checking to see if the given searchBy is a known valid set of searchBy.
     * Any false-y parameters will always return false
     *
     * @param searchBy - case insensitive
     */
    __isValidSeachBy : function(searchBy) {
      if (!searchBy) {
        return false;
      }

      var isSearchByValid = false;
      for (var i = 0; i < this.menuOptions.length; i++) {
        var validSearchBy = this.menuOptions[i].value;
        if (searchBy.toUpperCase() === validSearchBy.toUpperCase()) {
          isSearchByValid = true;
          break;
        }
      }
      return isSearchByValid;
    },

    postCreate : function() {
      var me = this;

      // The default search pill is search by instance state
      this.prevSelected = "instanceState";
      // Populate the search by dropdown menu with the various "search by" choices
      this.menuOptions.forEach(function(option) {
        // Build up the drop down menu on the left side of the pill.
        // In here, we will add the left pill choices that are specific
        // to the java batch UI tool.
        me.searchByDropDownMenu.addChild(new MenuItem({
          id : me.searchByDropDownMenu.id + "_" + option.value + "-menuItem",
          label : option.label,
          value : option.value,
          onClick : function() {
            // Call the method to change the pill to show what the user
            // choice by clicking.
            me.setSelectedMenuItemAndInput(this);
            // Check if we are creating invalid search combination
            me.__checkForInvalidSearchCombination();
          }
        }));
      });

      var firstMenuItem = this.searchByDropDownMenu.getChildren()[0];
      if (this.searchBy && typeof this.searchBy === "string") {
        // need to set the left side of the pill to a specific search by criteria
        firstMenuItem = this.__getMenuItem(this.searchByDropDownMenu, this.searchBy);
      }
      this.searchByDropDown.set("label", firstMenuItem.label);
      this.selectedSearchByMenuItem = firstMenuItem;
      this.setSelectedMenuItemAndInput(firstMenuItem);

      this.__setupAllDropDownMenus();
      this.__setupLastUpdateDateSelector();
      this.__setupLastUpdateDateRangeSelector();

      var placeholder = i18n.SEARCH;

      // TODO: Is there an easier way to loop through only the dojo attachment points that need
      // a placeholder?
      this.jobInstanceIdInput.set("placeholder", placeholder);
      this.jesJobNameInput.set("placeholder", placeholder);
      this.jesJobIdInput.set("placeholder", placeholder);
      this.submitterInput.set("placeholder", placeholder);
      this.appNameInput.set("placeholder", placeholder);
      this.jobNameInput.set("placeholder", placeholder);

      this.__setInputField(this.searchBy, this.value);

      this.inherited(arguments);
    }, // end of postCreate

    destroy : function() {
      // After pill is deleted, check if there is an invalid search combination
      this.inherited(arguments);
      this.__checkForInvalidSearchCombination();
    },

    __getMenuItem : function(dropDownMenu, itemValue) {
      // default to the first one so never returns null
      var menuItem = dropDownMenu.getChildren()[0];
      dropDownMenu.getChildren().forEach(function(item) {
        if (item.value.toLowerCase() === itemValue.toLowerCase()) {
          menuItem = item;
        }
      });
      return menuItem;
    }, // end of __getMenuItem

    __setupAllDropDownMenus : function() {
      this.__setupInstanceStateDropDown();
    }, // end of __setupAllDropDownMenus

    __setupInstanceStateDropDown : function() {

      // CSS: Add an empty styling called 'tabbedState'.  'tabbedState' is an indicator that we need to add
      //      tabbing to the presentation of drop down item.  'tabbedState' is used for performing
      //      CSS multi class selector to get at the HTML element that needs to be tabbed.
      this.__setupResourceDropDown(this.resourceInstanceStateDropDown, this.resourceInstanceStateDropDownMenu, true, [ {
        value : "QUEUED",
        label : i18n.QUEUED,
        iconClass : "",
        icon : ''
      }, {
        value : "SUBMITTED",
        label : i18n.SUBMITTED,
        iconClass : "",
        icon : '',
        customMenuItemClass : 'tabbedState'
      }, {
        value : "JMS_QUEUED",
        label : i18n.JMS_QUEUED,
        iconClass : "",
        icon : '',
        customMenuItemClass : 'tabbedState'
      }, {
        value : "JMS_CONSUMED",
        label : i18n.JMS_CONSUMED,
        iconClass : "",
        icon : '',
        customMenuItemClass : 'tabbedState'
      }, {
        value : "DISPATCHED",
        label : i18n.DISPATCHED,
        iconClass : "",
        icon : ''
      }, {
        value : "ENDED",
        label : i18n.ENDED,
        iconClass : "",
        icon : ''
      }, {
        value : "FAILED",
        label : i18n.FAILED,
        iconClass : "",
        icon : '',
        customMenuItemClass : 'tabbedState'
      }, {
        value : "STOPPED",
        label : i18n.STOPPED,
        iconClass : "",
        icon : '',
        customMenuItemClass : 'tabbedState'
      }, {
        value : "COMPLETED",
        label : i18n.COMPLETED,
        iconClass : "",
        icon : '',
        customMenuItemClass : 'tabbedState'
      }, {
        value : "ABANDONED",
        label : i18n.ABANDONED,
        iconClass : "",
        icon : '',
        customMenuItemClass : 'tabbedState'
      } ]);

      // Add customization to the drop down menu that is specific to java batch UI search by instance states
      this.__setupMultipleChoiceSelection(this.resourceInstanceStateDropDownMenu);

    }, // end of __setupInstanceStateDropDown

    // In java batch, there exists non-JSR352 states that encompass multiple JSR352 states.  This method
    // will highlight multiple states when the user hovers over the non-JSR352 states.
    __setupMultipleChoiceSelection : function(rightSideOfSearchPill) {
      var me = this;
      registry.findWidgets(rightSideOfSearchPill.domNode).forEach(function(widget) {
        on(widget, "mouseover, mouseleave, focus, blur", function(event) {
          var id = event.currentTarget.id;
          if (id.indexOf("QUEUED") !== -1 && id.indexOf("JMS_QUEUED") === -1) {
            // Hovering over QUEUED was detected, highlight the group of states
            // that make up non-JSR352 "QUEUED" states.
            var statesInQueued = [ "SUBMITTED", "JMS_QUEUED", "JMS_CONSUMED" ];
            for (var i = 0; i < statesInQueued.length; i++) {
              var targetState = statesInQueued[i];
              me.__setupMultipleHighlightingOfStates(event, "QUEUED", targetState);
            }
          } else if (id.indexOf("ENDED") !== -1) {
            // Hovering over ENDED was detected, highlight the group of states
            // that make up non-JSR352 "ENDED" states.
            var statesInEnded = [ "FAILED", "STOPPED", "COMPLETED", "ABANDONED" ];
            for (var k = 0; k < statesInEnded.length; k++) {
              var tempTargetState = statesInEnded[k];
              me.__setupMultipleHighlightingOfStates(event, "ENDED", tempTargetState);
            }
          }
        });
      });
    }, // end of __setupMultipleChoiceSelection

    // This method will make groups of instance state highlight when mouse is over
    // the non-JSR352 states
    __setupMultipleHighlightingOfStates : function(event, parentState, targetState) {
      var id = event.currentTarget.id;

      // Really wanted to avoid adding a hardcoded class, but the Dojo documentation states this
      // is the class that controls hover styling. Tried to steal the classes from the
      // element that already had styling for hover over, but does not work on all browsers
      // Exampple of stealing hoverOver classes to apply to other elements
      //      event.currentTarget.getAttribute("class");
      var classesOnMouseOver = "dijitMenuItemHover";

      if (event.type === "mouseleave" || event.type === "blur") {
        // Because the search pills are dymanically generated, use the current id as a basis for finding
        // another id within the same search pill.
        var targetId = id.replace(parentState, targetState);
        var targetDom = dom.byId(targetId); // dom.byId is case sensitive
        domClass.remove(targetDom, classesOnMouseOver);
      } else if (event.type === "mouseover" || event.type === "focus") {
        // Because the search pills are dymanically generated, use the current id as a basis for finding
        // another id within the same search pill.
        var targetIdTwo = id.replace(parentState, targetState);
        var targetDomTwo = dom.byId(targetIdTwo); // dom.byId is case sensitive
        domClass.add(targetDomTwo, classesOnMouseOver);
      }
    }, // end of __setupMultipleHighlightingOfStates

    __handleDropDownChange : function(dropdown, label, value) {
      dropdown.set("label", label);
      dropdown.set("value", value);
      this.checkForInvalidCombinations_All();
      this.__performSearch();
    }, // end of __handleDropDownChange

    // Setup the right side of the pill
    __setInputField : function(searchBy, value) {
      var id = this.id;
      if (!value) {
        return;
      }

      var newValue = value;

      if (searchBy === "instanceState") {
        if ("ALL" === newValue.toUpperCase()) {
          // Legacy code uses lower case all in the ID.
          // Since we use newValue to construct an ID, lower case it.
          newValue = newValue.toLowerCase();
        } else {
          // Everything else needs to be uppercase because the IDs are uppercase
          // This will allow the query string in the URL to be case insensitive
          // *camelToSnake is to create JMS_QUEUED and JMS_CONSUMED
          newValue = sharedUtils.camelToSnake(newValue).toUpperCase();
        }
      }

      // if an initial value is passed in, set the correct selections
      var input = registry.byId(id + "_" + searchBy + "-input");
      if (searchBy === "lastUpdateRange") {
        this.__setValuesForTheTwoCalendars(value);
      } else if (input.type === "button") {
        // it is a dropdown set of options
        var dropDownMenu = registry.byId(id + "_" + searchBy + "-dropDownMenu");
        var menuItem = registry.byId(dropDownMenu.id + "_" + newValue + "-menuItem");
        if (menuItem !== null) {
          input.set("label", menuItem.label);
          input.set("value", newValue);
        }
      } else if (searchBy === "lastUpdate") {
        // Let the widget format the date to the locale.
        // Don't set the "displayedValue" atttribute.
        // Search pill for a single day
        input.set("value", value);
      } else {
        input.set("displayedValue", value);
      }
    }, // end of __setInputField

    // The date range search pill has two input fields: from date, to date.
    // Parse and set the two calendars to the dates specified by the query param
    __setValuesForTheTwoCalendars : function(value) {
      var id = this.id;
      value = value.split(':');
      var fromDate = value[0];
      var toDate = value[1];

      var startInput = registry.byId(id + "_lastUpdateRangeStart-input");
      var endInput = registry.byId(id + "_lastUpdateRangeEnd-input");
      startInput.set("value", fromDate);
      endInput.set("value", toDate);
    }, // end of __setValuesForTheTwoCalendars

    checkKeyCode : function(event) {
      var keyPushed = event.charOrCode;

      if (!keyPushed) {
        return;
      }

      if (keyPushed === keys.DELETE || keyPushed === keys.BACKSPACE) {
        var isPillSelected = this.isPillSelected();
        if (isPillSelected) {
          // Call search box to delete THIS pill.  Since the search box
          // is in charge of keep track of all the current pills,
          // we best let the search box handle it rather than have
          // this search pill destroy itself.
          registry.byId(sharedID.SEARCH_MAIN_BOX).removeSearchPill(this);
        }
      }

    }, // end of checkKeyCode

    checkForInvalidCombinations_All : function() {
      // Why did the explore tool code base put this method in the search pill object?
      // Instead, we are going to put this in the search box object because the box
      // is aware of all of the search pills that are current in the box.
    }, // end of checkForInvalidCombinations_All

    buildRendering : function() {
      /*
       * Replace the empty right side of the search pill with explore
       * specific drop down menu elements.
       */
      var newRightDivDom = domConstruct.toDom(JavaBatchSearchPillTemplate);
      var baseTemplate = domConstruct.toDom(this.templateString);
      var attachPointNode = query('div[data-dojo-attach-point="searchPillRightDiv"]', baseTemplate)[0];

      domConstruct.place(newRightDivDom, attachPointNode, "replace");
      this.templateString = sharedUtils.domToString(baseTemplate);

      this.inherited(arguments);

    }, // end of buildRendering

    __handleInputChange : function(event) {
      var keyPushed = event.charOrCode;
      if (!keyPushed) {
        return;
      }
      if (keyPushed === keys.ENTER) {
        this.__performSearch();
      }
    },

    // Attach a search action when users select a date from the calendar popup
    __setupLastUpdateDateSelector : function() {
      var me = this;
      aspect.after(this.lastUpdateInput, "closeDropDown", function(deferred) {
        me.__performSearch();
      });
      aspect.after(this.lastUpdateInput, "openDropDown", function(deferred) {
        for (var i = 0; i < me.lastUpdateInput.dropDown.domNode.childElementCount; i++) {
          if (me.lastUpdateInput.dropDown.domNode.children[i].tagName.toLowerCase() === "table") {
            domAttr.set(me.lastUpdateInput.dropDown.domNode.children[i], "aria-label", "calendar");
            break;
          }
        }
      });
    }, // end of __setupLastUpdateDateSelector

    // Add logic to the search pill that filters on a range of dates.  Prevent the starting
    // date from being more recent that the ending date.
    __setupLastUpdateDateRangeSelector : function() {
      var me = this;

      // Flag to check if the date range selector is open to prevent multiple closeDropDown events from firing
      me.lastUpdateRangeStartInput.dropDownOpen = false;
      me.lastUpdateRangeEndInput.dropDownOpen = false;

      aspect.before(this.lastUpdateRangeStartInput, "openDropDown", function(deferred) {
        me.lastUpdateRangeStartInput.dropDownOpen = true;
        me.lastUpdateRangeStartInput.constraints.max = me.lastUpdateRangeEndInput.getValue();
      });
      aspect.after(this.lastUpdateRangeStartInput, "openDropDown", function(deferred) {
        for (var i = 0; i < me.lastUpdateRangeStartInput.dropDown.domNode.childElementCount; i++) {
          if (me.lastUpdateRangeStartInput.dropDown.domNode.children[i].tagName.toLowerCase() === "table") {
            domAttr.set(me.lastUpdateRangeStartInput.dropDown.domNode.children[i], "aria-label", "calendarstart");
            break;
          }
        }
      });
      aspect.before(this.lastUpdateRangeEndInput, "openDropDown", function(deferred) {
        me.lastUpdateRangeEndInput.dropDownOpen = true;
        me.lastUpdateRangeEndInput.constraints.min = me.lastUpdateRangeStartInput.getValue();
      });

      aspect.after(this.lastUpdateRangeEndInput, "openDropDown", function(deferred) {
        for (var i = 0; i < me.lastUpdateRangeEndInput.dropDown.domNode.childElementCount; i++) {
          if (me.lastUpdateRangeEndInput.dropDown.domNode.children[i].tagName.toLowerCase() === "table") {
            domAttr.set(me.lastUpdateRangeEndInput.dropDown.domNode.children[i], "aria-label", "calendarend");
            break;
          }
        }
      });
      aspect.after(this.lastUpdateRangeStartInput, "closeDropDown", function(deferred) {
        if (me.lastUpdateRangeStartInput.dropDownOpen) {
          me.lastUpdateRangeStartInput.dropDownOpen = false;
          me.__performSearch();
        }
      });
      aspect.after(this.lastUpdateRangeEndInput, "closeDropDown", function(deferred) {
        if (me.lastUpdateRangeEndInput.dropDownOpen) {
          me.lastUpdateRangeEndInput.dropDownOpen = false;
          me.__performSearch();
        }
      });
    },

    // Highlights a search pill that is not correct
    markPillAsInvalid : function() {
      domClass.add(this.domNode, "searchPillInvalid");
      domClass.add(this.searchPillRightDiv, "searchPillInvalidMenuItemValue");
    }, // end of markPillAsInvalid

    // Removes the styling that marks a search pill as incorrect
    markPillAsValid : function() {
      domClass.remove(this.domNode, "searchPillInvalid");
      domClass.remove(this.searchPillRightDiv, "searchPillInvalidMenuItemValue");
    }, // end of markPillAsValid

    __checkForInvalidSearchCombination : function() {
      var searchBox = registry.byId(sharedID.SEARCH_MAIN_BOX);
      searchBox.markAnyInvalidSearchPillCombinations();
    }

  });
  return JavaBatchSearchPill;
});
