/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/* jshint strict: false */
define("jsExplore/widgets/graphs/TimeSelector", [
        "dojo/_base/declare", 
        "dojo/_base/lang",
        "dojo/_base/array",
        "dojo/dom",
        "dojo/date",
        "dojo/date/locale",
        "dojo/dom-class",
        "dojo/dom-style",
        "dojo/i18n!jsExplore/nls/explorerMessages",
        "dojo/text!jsExplore/widgets/graphs/templates/TimeSelector.html",
        "dojo/on",
        "dojo/Stateful",
        "dijit/_WidgetBase",
        "dijit/_WidgetsInTemplateMixin",
        "dijit/_TemplatedMixin",
        "dijit/form/ToggleButton",
        "dijit/layout/ContentPane",
        "dijit/form/Button", // Since TimeSelector is not shipped, replace dojox/mobile/Button with dijit/form/Button 
                             // without re-formatting the button css
        "js/widgets/TextBox",
        "jsExplore/resources/stats/TimeSelectorUtils",
        "jsExplore/utils/ID",
        "jsExplore/widgets/graphs/TimeSlider", 
        "jsExplore/resources/utils"
        ], function(
                declare,
                lang,
                array,
                dom,
                dojoDate,
                dateLocale,
                domClass,
                domStyle,
                i18n,
                template,
                on,
                Stateful,
                _WidgetBase,
                _WidgetsInTemplateMixin,
                _TemplatedMixin,
                ToggleButton,
                ContentPane,
                Button,
                TextBox,
                TimeSelectorUtils,
                ID,
                TimeSlider,
                utils
            ){
  
    // Start of TimeSelector Widget code.
    return declare("TimeSelector", [ ContentPane, Stateful, _WidgetBase, _TemplatedMixin ], {

        id : "",
        resource : null,
        baseClass : "graphTimeSelector",
        // The TimeSlider widget for the startTime selection
        startTimeSlider: null,
        // The TimeSlider Display for the startTime selection
        startTimeDisplay: null,
        // The Node for the TimeSlider widget for the startTime selection
        startTimeSliderNode: null,
        // The Node for the TimeSlider Display for the startTime selection
        startTimeDisplayNode: null,
        // The TimeSlider widget for the endTime selection
        endTimeSlider: null,
        // The TimeSlider Display for the startTime selection
        endTimeDisplay: null,
        // The Node for the TimeSlider widget for the endTime selection
        endTimeSliderNode: null,
        // The Node for the TimeSlider Display for the startTime selection
        endTimeDisplayNode: null,
        // The domNode for the toggle button for enabling/disabling live updating
        liveUpdatingButton: null,
        // The actual toggleButton Widget
        liveUpdatingToggleButton: null,
        // The summary display for the minimised timeSelector panel
        summaryDisplay: null,
        // The Node for the Summary Display for the minimised TimeSelector panel.
        summaryDisplayNode: null,
        // The minimise button on the expanded display
        minimiseButton: null,
        // The div that the preselectable buttons should be placed in
        preSelectableTimeRangeView: null,
        // The template itself 
        templateString: template,
        // The domNode for the expandable section
        expandedViewPanel: null,
        // The domNode for the minimized section
        minimizedViewPanel: null,
        // This boolean indicates whether the time slider has moved since the mouse was pressed.
        sliderMoved: false,
        
        // The following variables are Constant Interval Objects that are used in a number of places.
        // We can't do all intervals because they change depending on which month we are in. 
        ONE_WEEK_INTERVAL: TimeSelectorUtils.generateTimeIntervalObjectFromInterval(new Date(), TimeSelectorUtils.INTERVAL_WEEK, 1, false),
        ONE_DAY_INTERVAL: TimeSelectorUtils.generateTimeIntervalObjectFromInterval(new Date(), TimeSelectorUtils.INTERVAL_DAY, 1, false),
        ONE_HOUR_INTERVAL: TimeSelectorUtils.generateTimeIntervalObjectFromInterval(new Date(), TimeSelectorUtils.INTERVAL_HOUR, 1, false),
        TEN_MINUTE_INTERVAL: TimeSelectorUtils.generateTimeIntervalObjectFromInterval(new Date(), TimeSelectorUtils.INTERVAL_MIN, 10, false),
        FIVE_MINUTE_INTERVAL: TimeSelectorUtils.generateTimeIntervalObjectFromInterval(new Date(), TimeSelectorUtils.INTERVAL_MIN, 5, false),
        ONE_MINUTE_INTERVAL: TimeSelectorUtils.generateTimeIntervalObjectFromInterval(new Date(), TimeSelectorUtils.INTERVAL_MIN, 1, false),
        
        EARLIEST_ACCEPTABLE_DATE: new Date("January 1, 1970 00:00:00"),
        
        timeRanges: null,
        selectedButton : -1,
        
        startDate: 0,
        endDate: new Date(),
        // A boolean indicating whether the timeSelector is expanded or not.
        timeSelectorExpanded: true,
        // This interval object is used to store the current starttime interval from now. We can't store a date, because now is 
        // always changing but by storing an interval, everytime we request the starttime, we can apply this interval from the 
        // time at that point
        currentTimeInterval: null,
        
        
        /**
         * WATCH VARIABLES THAT OTHER WIDGETS CAN USE TO BE NOTIFIED OF CHANGES
         */
        // This variable is updated when the range has been selected. It doesn't update until a user has released the mouse button
        // of one of the sliders, or when they have selected a interval button.
        selectedRange : null,
        
        // This boolean variable is updated when the liver updating button is either selected or deselected.
        liveUpdating: true,
        
        /**
         * END OF WATCH VARIABLES
         */
        constructor: function(params) {
          this.resource = params.resource;
          this.perspective = params.perspective;
          this.id = this.perspective + ID.dashDelimit(this.resource.id, ID.getTimeSelectorUpper());
          this.idMinimisedView = ID.underscoreDelimit(this.id, ID.getMinimisedView());
          this.idExpandedView = ID.underscoreDelimit(this.id, ID.getExpandedView());
          this.idTimeSliderView = ID.underscoreDelimit(this.id, ID.getTimeSliderView());
          this.idLiveUpdatingView = ID.underscoreDelimit(this.id, ID.getLiveUpdatingView());
          this.liveUpdatingLabel = i18n.STATS_LIVE_UPDATE_LABEL;
          this.timeRanges = [  // 1y, 1mo, 1w, 1d, 1h, 10m, 5m, 1m
//                          {id: "all", "label" : i18n.STATS_TIME_ALL, interval: null, displayLabel: i18n.STATS_TIME_ALL},
                          {id: ID.get1y(), "label" : i18n.STATS_TIME_1YEAR, interval: null, intervalType: TimeSelectorUtils.INTERVAL_YEAR, intervalValue: 1, displayLabel: i18n.STATS_DISPLAY_TIME_LAST_YEAR_LABEL},
                          {id: ID.get1mo(), "label" : i18n.STATS_TIME_1MONTH, interval: null, intervalType: TimeSelectorUtils.INTERVAL_MONTH, intervalValue: 1, displayLabel: i18n.STATS_DISPLAY_TIME_LAST_MONTH_LABEL},
                          {id: ID.get1w(), "label" : i18n.STATS_TIME_1WEEK, interval: this.ONE_WEEK_INTERVAL, displayLabel: i18n.STATS_DISPLAY_TIME_LAST_WEEK_LABEL},
                          {id: ID.get1d(), "label" : i18n.STATS_TIME_1DAY, interval: this.ONE_DAY_INTERVAL, displayLabel: i18n.STATS_DISPLAY_TIME_LAST_DAY_LABEL},
                          {id: ID.get1h(), "label" : i18n.STATS_TIME_1HOUR, interval: this.ONE_HOUR_INTERVAL, displayLabel: i18n.STATS_DISPLAY_TIME_LAST_HOUR_LABEL},
                          {id: ID.get10m(), "label" : i18n.STATS_TIME_10MINUTES, interval: this.TEN_MINUTE_INTERVAL, displayLabel: i18n.STATS_DISPLAY_TIME_LAST_10_MINUTES_LABEL},
                          {id: ID.get5m(), "label" : i18n.STATS_TIME_5MINUTES, interval: this.FIVE_MINUTE_INTERVAL, displayLabel: i18n.STATS_DISPLAY_TIME_LAST_5_MINUTES_LABEL},
                          {id: ID.get1m(), "label" : i18n.STATS_TIME_1MINUTE, interval: this.ONE_MINUTE_INTERVAL, displayLabel: i18n.STATS_DISPLAY_TIME_LAST_MINUTE_LABEL}];
        },

        postCreate : function() {
          this.inherited(arguments);
          
          // Create the minimize button that switches to the minimized view of the Time Selector.
          var minimiseButton = new Button({
            id : ID.underscoreDelimit(this.id, ID.getMinimiseButton()),
            baseClass : 'graphTimeSelectorMinimiseButton',
            onClick : lang.hitch(this, function() {
              this.__toggleTimeSelectorExpansion();
            })
          }, this.minimiseButtonNode);
          
          // The on click function for the minimized timeSelector
          on(this.minimizedViewPanel, "click", lang.hitch(this, function() {
            this.__toggleTimeSelectorExpansion();
          }));
          
          // Create the displaying textbox for the end timestamp.
          this.summaryDisplay = new TextBox({id: this.id + ID.getSummaryDisplay(), readOnly: true}, this.summaryDisplayNode);
          
          // The Pane that contains the selectable TimeStamp buttons.
          var preSelectableTimeRangeViewPane = new ContentPane({}, this.preSelectableTimeRangeView);
          
          // build a button for each of the timeRanges
          array.forEach(this.timeRanges, lang.hitch(this, function(range, i){
            var rangeButton = new Button({
              id : this.id + range.id,
              baseClass : 'graphTimeSelectorButton',
              label : range.label,
              index : i,
              parentSelector: this,
              onClick : function() {
                var currButton = this;
                lang.hitch(this.parentSelector, this.parentSelector.__selectPreSelectedTimeSelectionButton(currButton.index, 
                      currButton.timeIntervalType, currButton.timeInterval));
                
              }
            });
            
            this.timeRanges[i].button = rangeButton;
            preSelectableTimeRangeViewPane.addChild(rangeButton);
          }));
          
          this.startTimeSlider = new TimeSlider({id: ID.underscoreDelimit(this.id, ID.getStartTimeSlider()),
                                                 maximum: 100, 
                                                 minimum: -100, 
                                                 showButtons: false, 
                                                 clickSelect: false, 
                                                 intermediateChanges: true,
                                                 sliderEnabled: true}, this.startTimeSliderNode);
          // Create the displaying textbox for the end timestamp.
          this.startTimeDisplay = new TextBox({id: this.id + ID.getStartTimeDisplay(), readOnly: true}, this.startTimeDisplayNode);
          
          // A watch of the startTimeSlider's var that gets updated when the slider is moved.
          this.startTimeSlider.watch("sliderDate", lang.hitch(this, function(obj, oldValue, newValue) {
            this.__setStartDate(newValue);
            this.sliderMoved = true;
            
            // The display depends on whether we in live update mode or in custom time mode.
            this.__configureStartTimeDisplay();
          }));
          
          // Set a minimum value for the start slider to be the standard computer date.
          this.startTimeSlider.setMinSliderValue(this.EARLIEST_ACCEPTABLE_DATE);
          
          this.endTimeSlider = new TimeSlider({id: ID.underscoreDelimit(this.id + ID.getEndTimeSlider()), 
                                               maximum: 100, 
                                               minimum: -100, 
                                               showButtons: false, 
                                               clickSelect: false, 
                                               intermediateChanges: true,
                                               sliderEnabled: false}, this.endTimeSliderNode);
          // Create the displaying textbox for the end timestamp.
          this.endTimeDisplay = new TextBox({id: this.id + ID.getEndTimeDisplay(), readOnly: true, value: i18n.STATS_TIME_SELECTOR_NOW_LABEL}, this.endTimeDisplayNode);
          
          // A watch of the endTimeSlider's var that gets updated when the slider is moved.
          this.endTimeSlider.watch("sliderDate", lang.hitch(this, function(obj, oldValue, newValue) {
            this.__setEndDate(newValue);
            this.sliderMoved = true;
            
            // Either display the Now translated String or display the chosen date, depending on whether the slider is enabled or not.
            this.__configureEndTimeDisplay();
          }));
          
          // This mouseup function listens for when the user has released the mouse button. If the slider has been
          // updated then we need to trigger the update of the selectedRange var. We do this because otherwise
          // updating the var when the start time or end time changes, can cause performance issues.
          this.own(
              on(document.body, "mouseup", lang.hitch(this, function(event) {
                if (this.sliderMoved)
                  this.__setSelectedRange();
              }))
          );
          
          // This method resets the sliderMoved boolean when the mouse down event is received.
          this.own(
              on(document.body, "mousedown", lang.hitch(this, function(event) {
                this.sliderMoved = false;
              }))
          );
          
          // Set a maximum value for the end slider to be now.
          this.endTimeSlider.setMaxSliderValue(0);
          // And set the max value of the startTimeSlider to be 1 second before the endDate.
          this.startTimeSlider.setMaxSliderValue(
              TimeSelectorUtils.calculateNewTimeFromInterval(this.endDate, TimeSelectorUtils.INTERVAL_SEC, 1, false));
          
          this.liveUpdatingToggleButton = new ToggleButton({showLabel: false,
                                               checked: true,
                                               baseClass: "graphTimeLiveUpdatingButton",
                                               iconClass: "graphTimeLiveUpdatingButtonSelected",
                                               onChange: lang.hitch(this, function(val) { 
                                                 this.__toggleLiveUpdating(val);
                                               })}, this.liveUpdatingButton);
       
        },
        
        startup : function() {
          // Click the 10m button which is the initial starting value.
          this.timeRanges[5].button.onClick();
          this.resize(); // per dojo documentation, resize should be called by startup
        },
        
        resize : function() {
          this.__movePanelsOffTheScrollBar();
        },
        
        _startDateGetter: function(){
          return this.startDate;
        },
        
        _startDateSetter: function(value){
          this.__setStartDate(value);
        },
        _selectedRangeGetter: function(){
          return this.selectedRange;
        },
        
        _selectedRangeSetter: function(value){
          this.selectedRange = value;
        },

        /**
         * This method is what is called by the graphs to get the current range to send to pipes to get the graph data.
         * It returns an array containing the starttime in ms, and the end time in ms.
         * 
         */
        getSelectedRange : function() {
          var dateRange = new Array();
          // If the liveUpdating Button is enabled, then we need to get the current time/date to calculate from.
          // If not, then use the startDate/endDate values.
          if (this.liveUpdating) {
            var currentTimeAndDate = new Date();
            dateRange = [TimeSelectorUtils.calculateNewTimeFromIntervalObject(
                currentTimeAndDate, this.currentTimeInterval, false).valueOf(), currentTimeAndDate.valueOf()];
          } else {
            dateRange = [this.startDate.valueOf(), this.endDate.valueOf()];
          }
          return dateRange;
        },
        
        /**
         * This method toggles the minimized view or the expanded view, depending on the toggle var.
         *  
         */
        __toggleTimeSelectorExpansion: function() {
          
          // If the TimeSelector is currently expanded, then hide the expanded section and display the minimized section.
          if (this.timeSelectorExpanded) {
            this.timeSelectorExpanded = false;
            domClass.add(this.expandedViewPanel, "graphTimeSelectorHide");
            if (domClass.contains(this.minimizedViewPanel, "graphTimeSelectorHide")) {
              domClass.remove(this.minimizedViewPanel, "graphTimeSelectorHide");
            }
            
            this.__configureMinimizedDisplay();
          } else {
            // If the TimeSelector is not currently expanded, then hide the minimized section and display the expanded section.
            this.timeSelectorExpanded = true;
            domClass.add(this.minimizedViewPanel, "graphTimeSelectorHide");
            if (domClass.contains(this.expandedViewPanel, "graphTimeSelectorHide")) {
              domClass.remove(this.expandedViewPanel, "graphTimeSelectorHide");
            }
          }
        },
        
        /**
         * This method is called whenever the live updating button is toggled, and we change the different styles 
         * within this code.
         */
        __toggleLiveUpdating: function(enabled) {
          // If the toggle is changed to enabled, then we need to disable the end time slider and base everything of 
          // Now time.
          if (enabled) {
            if (this.liveUpdating == false) {
              // Change the icon css to selected.
              this.liveUpdatingToggleButton.set("iconClass", "graphTimeLiveUpdatingButtonSelected");
              // Keeping the current interval
              this.currentTimeInterval = TimeSelectorUtils.generateTimeIntervalObjectFromDates(this.startDate, this.endDate);
              // We need to also update the startTime Display because that will change to the interval view.
              this.__setStartDate(TimeSelectorUtils.calculateNewTimeFromIntervalObject(this.endTimeSlider.get("sliderDate"), this.currentTimeInterval, false));
              this.startTimeSlider.setSliderTime(this.startDate);
              this.__setEndDate(new Date());
              // Disable the custom slider
              this.endTimeSlider.disableSlider();
              this.endTimeSlider.setSliderTime(new Date());
              // Replace the css on the slider Display to be the enabled view..
              domClass.replace(this.endTimeDisplay.domNode, "graphTimeSelectorSliderDisabled", "graphTimeSelectorSliderEnabled");
              
              this.set("liveUpdating", true);
              this.__configureStartTimeDisplay();
              this.__configureEndTimeDisplay();
              this.__setSelectedRange();
            }
          } else {
            if (this.liveUpdating) {
              this.set("liveUpdating", false);
              // If we're disabling Live updating, then we need to set the unselected css on the toggle button.
              this.liveUpdatingToggleButton.set("iconClass", "graphTimeLiveUpdatingButtonUnSelected");
              // We need to enable the end slider. We don't need to change the date, as this will be now.
              this.endTimeSlider.enableSlider();
              
              // As we're moving to custom sliders, we need to ensure that the startTimeSlider is the current interval from 
              // the current date otherwise, we'll see weird intervals displayed.
              this.startTimeSlider.setSliderTime(TimeSelectorUtils.calculateNewTimeFromIntervalObject(this.endTimeSlider.get("sliderDate"), 
                  this.currentTimeInterval, false));
              this.__configureEndTimeDisplay();
              domClass.replace(this.endTimeDisplay.domNode, "graphTimeSelectorSliderEnabled", "graphTimeSelectorSliderDisabled");
              this.__setSelectedRange();
              this.currentTimeInterval = TimeSelectorUtils.generateTimeIntervalObjectFromDates(this.startDate, this.endDate);
            }
          }
        },
        
        /**
         * This method is the processing when a PreSelected TimeInterval is selected by the user. It is also used when
         * the slider is used and it scrolls into a matching interval.
         */
        __selectPreSelectedTimeSelectionButton: function(buttonIndex, timeIntervalType, timeInterval) {
          
          // If we have a previous selection, remove the selection from the existing button
          this.__deSelectTimeSelectionButton();
          // Now highlight the new one.
          this.__highlightTimeSelectionButton(buttonIndex);
          
          // Calculate the new time interval for this button.
          var selectedButton = this.timeRanges[this.selectedButton];
          
          // Get the timeInterval for the selected range. 
          var buttonTimeInterval = selectedButton.interval;
          // If this value is null, it means we can't use a precalculated range, because the interval will be 
          // affected by days in month or leap years etc, so we need to get the interval type and calculate it.
          if (buttonTimeInterval === null) {
            // If we have an interval type instead of a interval Object, then calculate the interval object.
            if (selectedButton.intervalType) {
              buttonTimeInterval = TimeSelectorUtils.generateTimeIntervalObjectFromInterval(this.endDate, 
                  selectedButton.intervalType, selectedButton.intervalValue, false);
            }
          }
            
          // If we still don't have an interval object, then it is because the user has hit the All button. In this case
          // set the date to be the earliest date possible, otherwise we calculate the date from the interval object.
          if (buttonTimeInterval === null) {
            this.startTimeSlider.setSliderTime(this.EARLIEST_ACCEPTABLE_DATE);
          } else {
            this.startTimeSlider.setSliderTime(TimeSelectorUtils.calculateNewTimeFromIntervalObject(
                                                                        this.endDate, buttonTimeInterval, false));
          }
          this.__setSelectedRange();
        }, 
        
        /**
         * This method removes the selection of any preselected TimeInterval buttons, and highlights the 
         * requested button.
         * 
         * @param {int} The index of the required Time Interval Button.
         */
        __highlightTimeSelectionButton: function(buttonIndex) {
          // Now set the range to be the new button.
          this.selectedButton = buttonIndex;
          domClass.toggle(this.timeRanges[this.selectedButton].button.domNode, "selected", true);
        },
        
        /**
         * This method removes the selection of any preselected TimeInterval buttons.
         * 
         */
        __deSelectTimeSelectionButton: function() {
          // If we have a previous selection, remove the selection from the existing button
          if (this.selectedButton >= 0)
            domClass.toggle(this.timeRanges[this.selectedButton].button.domNode, "selected", false);
        },
        
        /**
         * This method set the startTimeDisplay field with the correct formatted string depending on the Live Update
         * mode.
         */
        __configureStartTimeDisplay: function() {
          
          // The display depends on whether we in live update mode or in custom time mode.
          if (this.liveUpdating) {
            
            // Check to see if we match any of the time intervals.          
            var displayLabel = this.__compareIntervalWithSelectionButtons(this.startDate, this.currentTimeInterval);
            
            // If the display label is null, meaning we haven't matched a preselection interval, then we need to calculate the 
            // interval label to display. 
            if (displayLabel === null)
              displayLabel = TimeSelectorUtils.getFormattedInterval(this.currentTimeInterval);
            
            this.startTimeDisplay.set("value", displayLabel);

          } else {
            // If we're not in live update mode, then just display the formatted date.
            this.startTimeDisplay.set("value", this.__getFormattedDate(this.startDate, "medium", false));
            // We need to check that we don't match a selection button, and this method will highlight the button if we do.
            // We don't need to use the display value.
            this.__compareIntervalWithSelectionButtons(this.startDate, TimeSelectorUtils.generateTimeIntervalObjectFromDates(this.startDate, this.endDate));
          }
        },
        
        /**
         * This method sets the endTimeDisplay field with the correct formatted string depending on the Live Update
         * mode         * 
         */
        __configureEndTimeDisplay: function() {
          // The display depends on whether we in live update mode or in custom time mode.
          if (this.liveUpdating) {
            this.endTimeDisplay.set("value", i18n.STATS_TIME_SELECTOR_NOW_LABEL);
          } else {
            // If we're not in live update mode, then just display the formatted date.
            this.endTimeDisplay.set("value", this.__getFormattedDate(this.endDate, "medium", false));
          }
          
          // We need to check that we don't match a selection button, and this method will highlight the button if we do.
          // We don't need to use the display value.
          this.__compareIntervalWithSelectionButtons(this.startDate, TimeSelectorUtils.generateTimeIntervalObjectFromDates(this.startDate, this.endDate));
        },
        
        /**
         * This method calculates what should be displayed in the minimized Time Selector view, depending on 
         * whether we are in Live updating mode or not.
         * 
         */
        __configureMinimizedDisplay: function() {
          
          var summaryValue;
          var smallDisplay = true;
          // We need to work out the correct format based on whether we are in live update mode, or on the actual
          // time interval we have between dates.
          // If we're in liveUpdate mode, then just take the value in the startTime Display and set it as this will 
          // have already been calculated and translated.
          if (this.liveUpdating) {
            summaryValue = this.startTimeDisplay.get("value");
            smallDisplay = true;
          } else {
            // If we're in custom timestamp mode check to see if both times are for today. In this case we only display 
            // the time rather than the date.
            var interval = TimeSelectorUtils.generateTimeIntervalObjectFromDates(this.startDate, this.endDate);
            // If the timestamp is not spanning multiple days, then we need to do further checks.
            if (interval.years === 0 && interval.months === 0 && interval.days === 0) {
              // If we have no hours set and the mins are less than 10 we need to do a full timestamp. If the 
              // time > 10 mins then we need to do hh:mm.
              if (interval.hours === 0 && interval.mins < 10) {
                // If we don't have any hours in the interval and the mins are < 10 we need to display the time in hh:mm:ss.
                summaryValue = this.__getFormattedDate(this.startDate, "medium", true) + "  -  " + this.__getFormattedDate(this.endDate, "medium", true); 
                smallDisplay = true;
                
              } else if (interval.hours === 0 || (this.startDate.getHours() <= this.endDate.getHours())) {
                // If we have no hours in the interval and the mins are > 10 or we just have an interval of hours, mins 
                // and secs and they are in the same day, i.e not over midnight, then only display the time.
                summaryValue = this.__getFormattedDate(this.startDate, "short", true) + "  -  " + this.__getFormattedDate(this.endDate, "short", true); 
                smallDisplay = true;
                  
              } else {
                // For anything else display the full date/timestamp.
                summaryValue = this.__getFormattedDate(this.startDate, "medium", false) + "  -  " + this.__getFormattedDate(this.endDate, "medium", false);
                smallDisplay = false;
              }
            // If we're spanning one or more days, display a full date/timestamp.  
            } else {
              summaryValue = this.__getFormattedDate(this.startDate, "medium", false) + "  -  " + this.__getFormattedDate(this.endDate, "medium", false);
              smallDisplay = false;
            }
          }
          
          // Now we know what to display we need to configure the css style to change the width of the collapsed box to 
          // fit in the date. We only change the style if we know that we need to change the size.
          var currentDisplaySizeIsSmall = domClass.contains(this.summaryDisplay.domNode, "graphTimeSelectorCollapsedSmallTextBox");
          if (smallDisplay && ! currentDisplaySizeIsSmall) {
            domClass.replace(this.summaryDisplay.domNode, "graphTimeSelectorCollapsedSmallTextBox", "graphTimeSelectorCollapsedLargeTextBox");
          } else if (! smallDisplay && currentDisplaySizeIsSmall) {
            domClass.replace(this.summaryDisplay.domNode, "graphTimeSelectorCollapsedLargeTextBox", "graphTimeSelectorCollapsedSmallTextBox");
          } else {
            //NOP            
          }
          
          this.summaryDisplay.set("value", summaryValue);
        },
        
        /**
         * This method returns a String representation of the supplied date, in the format:
         * 
         *  mmm dd yyyy hh:mm:ss <am/pm>
         *  
         * @param {Date} dateObj: The date to use to format the String to display.
         * @param {Boolean} timeOnly: A boolean to indicate whether we want a date/timestamp or just the
         *                            timestamp.
         * 
         */
        __getFormattedDate: function(dateObj, formatLength, timeOnly) {
          
          var formatType = "date time";
          if (timeOnly)
            formatType = "time";
          
          return dateLocale.format(dateObj, {formatLength: formatLength, selector: formatType});
        },
        /**
         * This method allows an external widget to configure the start/end time, e.g the drag selection of a graph
         * 
         * @param {Date} startDate - The Date object that details the from date and time of the range.
         * @param {Date} endDate - The Date object that details the to date and time of the range.
         * 
         */
        setStartAndEndDate: function(startDate, endDate) {
          var updateSelectedRange = false;
          // If the startDate is > 0 then we need to set the interval var, and indicate that we need to 
          // trigger the range variable update.
          if (startDate > 0) {
            this.startDate = startDate;
            updateSelectedRange = true;
          }
            
          // If the endDate is > 0 then we need to set the interval var, and indicate that we need to 
          // trigger the range variable update.
          if (endDate > 0) {
            this.endDate = endDate;
            updateSelectedRange = true;
          }
          
          // If we have made an update, then set the selectedRange which will trigger the watched value to 
          // be updated.
          if (updateSelectedRange) {
            // If we are setting custome start/end dates from external widgets, then turn off the live updating.
            if (this.liveUpdatingToggleButton.get("checked") == true) {
              this.liveUpdatingToggleButton.set("checked", false);
            }
            this.startTimeSlider.setSliderTime(this.startDate);
            this.endTimeSlider.setSliderTime(this.endDate);
            this.__configureMinimizedDisplay(); // Update the time shown in the minimized time selector
          }
        },
        
        /**
         * This method checks to see if the supplied time interval maps to one of the preselection buttons. If it does
         * it highlights the button and returns the string to display, or returns null if there are no matches. 
         * @param {Date} dateObj: The start (from) date that we use to check whether to display the All flag.
         * @param {object} intervalObj: An object containing sec, min, hour, day, month, year fields that can contain values
         *                              each contributing to a specific interval e.g. 1day, 6 hours and 2 mins ago.  
         */
        __compareIntervalWithSelectionButtons: function(startDate, currentTimeInterval) {
          
          // If we have a previous selection, remove the selection from the existing button
          this.__deSelectTimeSelectionButton();
          
          var displayLabel = null;
          
          // If the startDate matches the earliest date that can be selected, then we need to apply the All field.
          if (dojoDate.compare(startDate, this.EARLIEST_ACCEPTABLE_DATE) === 0) {
            this.__highlightTimeSelectionButton(0);
            displayLabel = this.timeRanges[0].displayLabel;
            
          // if the interval minutes are either 1 5 or 10, then it is worth us doing the minute check. If not move on.
          } else if (currentTimeInterval.mins === 1 || currentTimeInterval.mins === 5 || currentTimeInterval.mins === 10) {
            // Check to see if we have the 1 minute interval. If so highlight the button and return the 1 min label.
            if (TimeSelectorUtils.compareIntervalObjects(currentTimeInterval, this.ONE_MINUTE_INTERVAL)) {
              this.__highlightTimeSelectionButton(7);
              displayLabel = this.timeRanges[7].displayLabel;
            // Check to see if we have the 5 minute interval. If so highlight the button and return the 5 min label.  
            } else if (TimeSelectorUtils.compareIntervalObjects(currentTimeInterval, this.FIVE_MINUTE_INTERVAL)) {
              this.__highlightTimeSelectionButton(6);
              displayLabel = this.timeRanges[6].displayLabel;
            // Check to see if we have the 5 minute interval. If so highlight the button and return the 5 min label.    
            } else if (TimeSelectorUtils.compareIntervalObjects(currentTimeInterval, this.TEN_MINUTE_INTERVAL)) {
              this.__highlightTimeSelectionButton(5);
              displayLabel = this.timeRanges[5].displayLabel;
            } else {
              //NOP
            }
          // if the interval hours are 1, then it is worth us doing the hour check. If not move on.  
          } else if (currentTimeInterval.hours === 1) {
            // Check to see if we have the 1 hour interval. If so highlight the button and return the 1 hour label.    
            if (TimeSelectorUtils.compareIntervalObjects(currentTimeInterval, this.ONE_HOUR_INTERVAL)) {
              this.__highlightTimeSelectionButton(4);
              displayLabel = this.timeRanges[4].displayLabel;
            } 
          // if the interval days are either 1 or 7, then it is worth us doing the day check. If not move on.  
          } else if (currentTimeInterval.days === 1 || currentTimeInterval.days === 7) {
            // Check to see if we have the 1 day interval. If so highlight the button and return the 1 day label.    
            if (TimeSelectorUtils.compareIntervalObjects(currentTimeInterval, this.ONE_DAY_INTERVAL)) {
              this.__highlightTimeSelectionButton(3);
              displayLabel = this.timeRanges[3].displayLabel;
            // Check to see if we have the 1 week (7 day) interval. If so highlight the button and return the 1 week label.    
            } else if (TimeSelectorUtils.compareIntervalObjects(currentTimeInterval, this.ONE_WEEK_INTERVAL)) {
              this.__highlightTimeSelectionButton(2);
              displayLabel = this.timeRanges[2].displayLabel;
            } else {
              //NOP
            }
          // if the interval months are 1, then it is worth us doing the month check. If not move on.  
          } else if (currentTimeInterval.months === 1) {
            // Check to see if we have the 1 month interval. If so highlight the button and return the 1 month label.
            
            // Because we can't use precalculated intervals, as the days in the month change depending on the start/enddate,
            // we need to calculate the interval.
            var monthInterval = TimeSelectorUtils.generateTimeIntervalObjectFromInterval(this.endDate,
                TimeSelectorUtils.INTERVAL_MONTH, 1, false);
            if (TimeSelectorUtils.compareIntervalObjects(currentTimeInterval, monthInterval)) {
              this.__highlightTimeSelectionButton(1);
              displayLabel = this.timeRanges[1].displayLabel;
            } 
          // if the interval years are 1, then it is worth us doing the year check. If not move on. 
          } else if (currentTimeInterval.years === 1) {
            // Because we can't use precalculated intervals, as the days in the year change depending on the start/enddate,
            // we need to calculate the interval.
            var yearInterval = TimeSelectorUtils.generateTimeIntervalObjectFromInterval(this.endDate,
                TimeSelectorUtils.INTERVAL_YEAR, 1, false);
            
            // Check to see if we have the 1 year interval. If so highlight the button and return the 1 year label.    
            if (TimeSelectorUtils.compareIntervalObjects(currentTimeInterval, yearInterval)) {
              this.__highlightTimeSelectionButton(0);
              displayLabel = this.timeRanges[0].displayLabel;
            } 
          } else {
            //NOP
          }
          
          return displayLabel;
        },
        
        /**
         * This method accepts a date object and set this as the start date. It also sets the minimum value of the end time slider
         * as well.
         * 
         */
        __setStartDate: function(startDate) {
          this.startDate = startDate;
          
          if (this.liveUpdating) {
            // If we're in live update mode, we need to calculate the new interval, and store that away.
            this.currentTimeInterval = TimeSelectorUtils.generateTimeIntervalObjectFromDates(this.startDate, this.endDate);
          }
          
          // Set the endtime minimum to be 1 second after the startDate.
          this.endTimeSlider.setMinSliderValue(
              TimeSelectorUtils.calculateNewTimeFromInterval(this.startDate, TimeSelectorUtils.INTERVAL_SEC, 1, true));
        },
        
        /**
         * This method accepts a date object and set this as the end date. It also sets the maximum value of the start time slider
         * as well.
         * 
         */
        __setEndDate: function(endDate) {
          this.endDate = endDate;
          
          if (this.liveUpdating) {
            // If we're in live update mode, we need to calculate the new interval, and store that away.
            this.currentTimeInterval = TimeSelectorUtils.generateTimeIntervalObjectFromDates(this.startDate, this.endDate);
          }
          
          // Set the startTime maximum to be 1 second before the endDate.
          this.startTimeSlider.setMaxSliderValue(
              TimeSelectorUtils.calculateNewTimeFromInterval(this.endDate, TimeSelectorUtils.INTERVAL_SEC, 1, false));
        },
        /**
         * This method returns the selected range 
         */
        getSelectedTimeRangeId : function(){
          if(this.timeRanges[this.selectedButton]){
             return this.timeRanges[this.selectedButton].id;
          }else{
             return null;
          }
        },
        /**
         * This method calculates the selectedRange variable which is an array of [startTime in ms, endtime in ms].
         * It sets the selectedRange variable which is used by the graphs to indicate when the selector has changed.
         */
        __setSelectedRange: function() {
          
          this.set("selectedRange", [this.startDate.valueOf(), this.endDate.valueOf()]);
        },
        
        /**
         * Move all the the panels docked to the right side of the browser off the scrollbar.  
         * We don't want to cover the browser's right vertical scrollbar.
         */
        __movePanelsOffTheScrollBar : function () {
          var minId = ID.underscoreDelimit(this.id, ID.getMinimisedView());  // FIXME: Better way to get id?
          var expandId = ID.underscoreDelimit(this.id, ID.getExpandedView());  // FIXME: Better way to get id?
          var element = dom.byId(minId);
          
          if(element === null) {
            // Do not do anything if the elements are missing
            return;
          }
          
          // Assume that if minimized time selector ID exists, then the expanded version exists too
          
          var rightPadding = domStyle.get(minId, 'right');
          if(rightPadding === '0px') {
            var scrollBarWidth = utils.getScrollBarWidth();
            var scrollBarHeight = utils.getScrollBarHeight();
            domStyle.set(minId, 'right', scrollBarWidth+'px');
            domStyle.set(minId, 'bottom', scrollBarHeight+'px');
            domStyle.set(expandId, 'right', scrollBarWidth+'px');
            domStyle.set(expandId, 'bottom', scrollBarHeight+'px');
          }
        }
    });
});