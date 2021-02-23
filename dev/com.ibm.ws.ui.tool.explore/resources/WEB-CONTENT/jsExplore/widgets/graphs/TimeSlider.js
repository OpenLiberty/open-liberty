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
define("jsExplore/widgets/graphs/TimeSlider", [
        "dojo/dom", 
        "dojo/_base/declare", 
        "dojo/_base/lang",
        "dojo/date",
        "dojo/dom-class",
        "dojo/i18n!jsExplore/nls/explorerMessages",
        "dojo/mouse",
        "dojo/on",
        "dojo/query",
        "dojo/Stateful",
        "dijit/_WidgetBase",
        "dijit/focus",
        "dijit/form/HorizontalSlider",
        "js/widgets/TextBox",
        "jsExplore/resources/stats/TimeSelectorUtils"
        ], function(
                dom,
                declare,
                lang,
                date,
                domClass,
                i18n,
                mouse,
                on,
                query,
                Stateful,
                _WidgetBase,
                focus,
                HorizontalSlider,
                TextBox,
                TimeSelectorUtils
            ){
  
    // Start of TimeSlider Widget code.
    return declare("TimeSlider", [ HorizontalSlider, _WidgetBase, Stateful ], {

        sliderDate: new Date(),
        sliderInterval: null,
        sliderEnabled: false,
        maxValue: new Date(),
        maxValueInMs: 0,
        minValue: new Date("January 1, 1970 00:00:00"),
        minValueInMs: 0,
        zeroTimeInterval: TimeSelectorUtils.getInitialTimeIntervalObject(),

        constructor: function(params) {
          this.inherited(arguments);
          this.id = params.id;
          this.sliderEnabled = params.sliderEnabled;
          
          this.maxValueInMs = this.maxValue.valueOf();
          this.minValueInMs = this.minValue.valueOf();
        },

        postCreate : function() {
          this.inherited(arguments);

          if (! this.sliderEnabled) {
            this.disableSlider();
          } 
          
          this.set("value", 0);
          
          this._movable.onFirstMove = lang.hitch(this, function(mover, e) {
            if (this.sliderInterval === null) {
              this.sliderInterval = setInterval(lang.hitch(this, function() {
                this.__processSliderChange();
              }), 100);
            }
          });
          
          this.own(
            on(document.body, "mouseup", lang.hitch(this, function(event) {
              this.__resetSliderPosition();
            }))
          );
        },
        
        __resetSliderPosition: function() {
          clearInterval(this.sliderInterval);
          this.sliderInterval = null;
          this.set("value", 0);
        },
        
        /**
         * This method processes the change in the slider value. It maps the slider value to a time interval,
         * and calculates the new date based on the previous current date and the interval to either add or
         * subtract, depending on whether the slider is moved left or right. 
         * 
         */
        __processSliderChange: function() {
          
          // Only calculate this if we've still got an interval set. This means that the user hasn't released the
          // mouse button. We have this check because the slider changes can get backed up, and we don't want to 
          // change the value after the user has released the mouse button.
          if (this.sliderInterval !== null) {
            //FW
            console.log("FW sliderInterval=" + this.sliderInterval);
            var currSliderValue = this.get("value");
            
            var timeInverval;
            var timeIntervalValue;
            
            var timeIncrease = false;
            // The slider range is -100 to 100, so if the value is positive it means the time should be 
            // calculated into the future, and if negative it means we need to move the slider to a past date.
            if (currSliderValue > 0)
              timeIncrease = true;
            
            // The value of the slider determines what interval we add/subtract. As you move the slider
            // further from the center, increases the timeinterval.
            if (currSliderValue === 100 || currSliderValue === -100) {
              timeInverval = TimeSelectorUtils.INTERVAL_YEAR;
              timeIntervalValue = 1;
            } else if (currSliderValue > 96 || currSliderValue < -96) {
              timeInverval = TimeSelectorUtils.INTERVAL_MONTH;
              timeIntervalValue = 1;
            } else if (currSliderValue > 89 || currSliderValue < -89) {
              timeInverval = TimeSelectorUtils.INTERVAL_WEEK;
              timeIntervalValue = 1;
            } else if (currSliderValue > 78 || currSliderValue < -78) {
              timeInverval = TimeSelectorUtils.INTERVAL_DAY;
              timeIntervalValue = 1;
            } else if (currSliderValue > 64 || currSliderValue < -64) {
              timeInverval = TimeSelectorUtils.INTERVAL_HOUR;
              timeIntervalValue = 4;
            } else if (currSliderValue > 45 || currSliderValue < -45) {
              timeInverval = TimeSelectorUtils.INTERVAL_MIN;
              timeIntervalValue = 10;
            } else if (currSliderValue > 20 || currSliderValue < -20) {
              timeInverval = TimeSelectorUtils.INTERVAL_MIN;
              timeIntervalValue = 1;
            } else if (currSliderValue > 10 || currSliderValue < -10) {
              timeInverval = TimeSelectorUtils.INTERVAL_SEC;
              timeIntervalValue = 10;
            } else if (currSliderValue > 0 || currSliderValue < 0) {
              timeInverval = TimeSelectorUtils.INTERVAL_SEC;
              timeIntervalValue = 1;
            } else {
              // Set the default to no interval.
              timeInverval = TimeSelectorUtils.INTERVAL_SEC;
              timeIntervalValue = 0;
            }
            
            // As the Max value can be 0, meaning that we should always use the current date, everytime we get to this 
            // point we need to check whether to get the current timestamp or not. 
            var dateToCompare = this.maxValue;
            var dateToCompareInMs = this.maxValueInMs;
            if (this.maxValue === 0) {
              dateToCompare = new Date();
              dateToCompareInMs = dateToCompare.valueOf();
            }
            
            if (timeIntervalValue !== 0) {
              // If we are increasing the date and the current Slider value is less than the maxValue, or the maxValue is 0, or we are decreasing the 
              // date and the slider value is greater than the minimum value, or the minimum value is 0, then we need to do the calculations. Otherwise we just
              // leave everything the same.
              if ((timeIncrease && date.compare(this.sliderDate, dateToCompare) < 0) ||
                  (! timeIncrease && date.compare(this.sliderDate, this.minValue) > 0)) {
                
                // work out the interval 
                var timeIntervalObj = TimeSelectorUtils.generateTimeIntervalObjectFromInterval(this.sliderDate, timeInverval, timeIntervalValue, timeIncrease);
                // If we find that it is zero, then don't bother processing it.
                if (! TimeSelectorUtils.compareIntervalObjects(timeIntervalObj, this.zeroTimeInterval)) {
                  
                  var newDate = TimeSelectorUtils.calculateNewTimeFromIntervalObject(this.get("sliderDate"), timeIntervalObj, timeIncrease);
                  
                  // Now check that the date doesn't exceed the max or min values. If it does, then set it to either 
                  // the max or min value.
                  if (timeIncrease) {
                      if (newDate.valueOf() > dateToCompareInMs)
                        newDate = dateToCompare;
                  } else {
                    if (newDate.valueOf() < this.minValueInMs)
                      newDate = this.minValue;
                  }
                  
                  this.set("sliderDate", newDate);
                }
              }
            }
          }
        },
        
        /**
         * This method disables the current slider.
         * 
         */
        disableSlider: function() {
          domClass.add(this.domNode, "graphTimeSelectorHide");
          this.set("disabled", true);
//FW          this.set("sliderDate", new Date());
        },
        
        /**
         * This method enables the current slider.
         * 
         */
        enableSlider: function() {
          if (domClass.contains(this.domNode, "graphTimeSelectorHide"))
            domClass.remove(this.domNode, "graphTimeSelectorHide");
          
          this.set("disabled", false);
//FW          this.set("sliderDate", new Date());
        },
        
        /**
         * This method set the current Slider value to a particular date object.
         * 
         * @param {Date} sliderDate: The date object to set the sliderDate value to.
         */
        setSliderTime: function(sliderDate) {
          this.set("sliderDate", sliderDate);
        },
        
        /**
         * This method returns the current Slider value as a date object.
         */
        getSliderTime: function() {
          return this.get("sliderDate");
        },
        
        /**
         * This method sets the maximum value that the slider can go to. 
         * 
         * @param {Date} maxValue: The maximum value of the slider.
         */
        setMaxSliderValue: function(maxValue) {
          this.set("maxValue", maxValue);
          this.set("maxValueInMs", maxValue.valueOf());
        },
        
        /**
         * This method sets the minimum value that the slider can go to. If 0 is passed in (which is also the 
         * default) it means January 1, 1970 00:00:00 value is used in any calculations.
         * 
         * @param {Date} maxValue: The maximum value of the slider.
         */
        setMinSliderValue: function(minValue) {
          this.set("minValue", minValue);
          this.set("minValueInMs", minValue.valueOf());
        },
        
        /**
         * This method returns a boolean indicating whether the slider is enabled or not.
         */
        isEnabled: function() {
          return ! this.get("disabled");
        }
    });
});