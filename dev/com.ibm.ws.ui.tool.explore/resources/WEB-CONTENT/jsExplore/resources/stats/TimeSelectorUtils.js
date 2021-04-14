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
/* TimeSelector utilities for calculating a future or past date by adding or removing time/date values.
 * 
 * @author Tim Mitchell
 * @module resources/utils
 * 
 * @return {Object} Containing all the Time selector utils methods. This does all the time calculations itself, because the dojo date doesn't have the type of  
 *                  difference calculations that we need. The dojo date difference method gives you the difference between 2 dates in a single timeinterval rather
 *                  than an interval of a number of different intervals e.g.
 *                  
 *                  Apr 1st -> Jun 4st  - dojo date result is 2 months, or 64 days ( depending on which interval type you choose)
 *                                      - this utility result is 2 months, 3 days.
 *                  
 */
define(["dojo/i18n!jsExplore/nls/explorerMessages",
        "dojo/_base/lang",
        "dojo/date",
        "dojo/date/locale"
       ],
    function(i18n, lang, dojoDate, locale) {
  
    //A variable to store the different days in each month. We store the non-leap year feb, and add 1 in the calculations when 
    // needed.
    var daysInMonthArray = [31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
    var _INTERVAL_ALL = "all";
    var _INTERVAL_SEC = "sec";
    var _INTERVAL_MIN = "min";
    var _INTERVAL_HOUR = "hour";
    var _INTERVAL_DAY = "day";
    var _INTERVAL_WEEK = "week";
    var _INTERVAL_MONTH = "month";
    var _INTERVAL_YEAR = "year";
    
    
    return {
        
        INTERVAL_ALL: _INTERVAL_ALL,
        INTERVAL_SEC: _INTERVAL_SEC,
        INTERVAL_MIN: _INTERVAL_MIN,
        INTERVAL_HOUR: _INTERVAL_HOUR,
        INTERVAL_DAY: _INTERVAL_DAY,
        INTERVAL_WEEK: _INTERVAL_WEEK,
        INTERVAL_MONTH: _INTERVAL_MONTH,
        INTERVAL_YEAR: _INTERVAL_YEAR,
        
        calculateNewTimeFromIntervalObject : __calculateNewTimeFromIntervalObject,
        calculateNewTimeFromInterval : __calculateNewTimeFromInterval,
        getFormattedInterval: __getFormattedInterval,
        generateTimeIntervalObjectFromDates: __generateTimeIntervalObjectFromDates,
        generateTimeIntervalObjectFromInterval: __generateTimeIntervalObjectFromInterval,
        getInitialTimeIntervalObject: __getInitialTimeIntervalObject,
        compareIntervalObjects: __compareIntervalObjects
      };
    
      /**
       * This method returns a date which is the interval earlier or later than the supplied date, e.g.
       * 
       * If the passed in date is 20150101 13:10 and the interval obj has 10 mins configured, the returned date
       * will be 20150101 13:00, if we're going back in time, or 20150101 13:20
       * 
       * @param {Date} initialTime: The "TO" time in the From/To time range.
       * @param {object} intervalObj: An object containing sec, min, hour, day, month, year fields that can contain values
       *                           each contributing to a specific interval e.g. 1day, 6 hours and 2 mins ago. 
       * @param {boolean} addToDate: A boolean indicating whether we calculating a future date or a past date.
       */
      function __calculateNewTimeFromIntervalObject(baseTime, intervalObj, addToDate) {
        
        // Create the startDate from the endTime.
        var calculatedDate = new Date(baseTime.valueOf());
        
        if (intervalObj.secs > 0)
          calculatedDate = dojoDate.add(calculatedDate, "second", (addToDate ? intervalObj.secs : -intervalObj.secs));
        if (intervalObj.mins > 0)
          calculatedDate = dojoDate.add(calculatedDate, "minute", (addToDate ? intervalObj.mins : -intervalObj.mins));
        if (intervalObj.hours > 0)
          calculatedDate = dojoDate.add(calculatedDate, "hour", (addToDate ? intervalObj.hours : -intervalObj.hours));
        if (intervalObj.totalDays > 0)
          calculatedDate = dojoDate.add(calculatedDate, "day", (addToDate ? intervalObj.totalDays : -intervalObj.totalDays));

        return calculatedDate;
      };
      
      /**
       * This method returns a date which is the interval earlier or later than the supplied date, e.g.
       * 
       * If the passed in date is 20150101 13:10 and the interval is 10 mins, the returned date
       * will be 20150101 13:00, if we're going back in time, or 20150101 13:20
       * 
       * @param {Date} initialTime: The "TO" time in the From/To time range.
       * @param {int} intervalType: String indicating the interval type. Values can be min, hour, day, week, month and year. 
       * @param {int} interval: A positive integer that contains the amount of interval time to take off the endtime.
       * @param {boolean} addToDate: A boolean indicating whether we calculating a future date or a past date.
       */
      function __calculateNewTimeFromInterval(baseTime, intervalType, intervalValue, addToDate) {
        var intervalObj = __generateTimeIntervalObjectFromInterval(baseTime, intervalType, intervalValue, addToDate);
        return  __calculateNewTimeFromIntervalObject(baseTime, intervalObj, addToDate);
      }

      /**
       * This method converts a set number of months into number of days.
       * 
       * @param {int} months: An integer containing the number of months to convert to days
       * @param {Date} startDate: A Date object to calculate from.
       * @param {boolean} addToDate: A boolean indicating whether we calculating a future date or a past date.
       */
      function __monthsToDays (months, startDate, addToDate) {
        
        var days = 0;
        // The order of the dates in the dojo date call are important, so we need to 
        // have 2 different call depending on whether the calculated date is before or after 
        // the pased in date.
        if (addToDate) {
          var newDate = dojoDate.add(startDate, "month", months);
          days = dojoDate.difference(startDate, newDate, "day");
        } else {
          var newDate = dojoDate.add(startDate, "month", -months);
          days = dojoDate.difference(newDate, startDate, "day");
        }
        
        // Return the final result.
        return days;
      };
      
      /**
       * This method converts a set number of days into number of months. The result is an array of [days, months] where
       * days is the remaining days after the number of months has been calculated
       * 
       *  e.g if the days is 32 and the startDate is 4th April, and addToDate is false (go back in time) the result will be
       *  [1,1] because Mar has 31 days, and the remainder of days is 32 - 31 = 1.
       * 
       * @param {int} days: An integer containing the number of days to convert to months
       * @param {Date} startDate: A Date object to calculate from.
       * @param {boolean} addToDate: A boolean indicating whether we calculating a future date or a past date.
       */
      function __daysToMonths(days, startDate, addToDate) {
        // Create a new date object that is the same as the startDate, because it will
        // get amended as we do the calculation. We don't want to use the actual object,
        // because otherwise it will get amended multiple times and give us the wrong date.
        var tempDateToCalcFrom = new Date(startDate.valueOf());
        // Before we deduct or add a month, we need to set the date to be the 1st of the month. 
        // This is because a)  we don't care about which day of the month we start from, and also if we change the 
        // date object that has 31st March to be Feb, the system says there is no 31st Feb and 
        // changes the date to be the 2nd of Mar !!
        tempDateToCalcFrom.setDate(1);
        
        var result = new Array();
        var numberOfMonths = 0;
        
        // Loop through the number of days to convert.
        while (days > 0) {
          tempDateToCalcFrom = dojoDate.add(tempDateToCalcFrom, "month", (addToDate ? 1 : -1));
          var daysInNextMonth = dojoDate.getDaysInMonth(tempDateToCalcFrom);
          
          if (days >= daysInNextMonth) {
            days -= daysInNextMonth;
            numberOfMonths++;
            result = [days, numberOfMonths];
          } else {
            result = [days, numberOfMonths];
            days = 0;
          }
        }
        
        // Return the final result.
        return result;
      };
      
      /**
       * This method pads a string to the required length and prepends the pad char to fill the space.
       * 
       * e.g. ("abc", 5, "0") will result in the returned string 00abc
       * 
       * @param {String} stringToPad: The String that we need to append the pad char to.
       * @param {int} lengthOfFinalString: The required length of the final String.
       * @param {String} padChar: The String that we need to append to the stringToPad var.
       */
      function __padString(stringToPad, lengthOfFinalString, padChar) {
        
        var paddedString = "";
        for (var i = 0; i < lengthOfFinalString; i++)
          paddedString = paddedString.concat(padChar);
          
        paddedString = paddedString.concat(stringToPad);
        return paddedString.substr(paddedString.length - lengthOfFinalString, lengthOfFinalString);
      }
      
      /**
       * This method takes an interval Object and returns a formatted 
       * string back that displays the 2 most significant time intervals units
       * 
       *  e.g. TimeInterval 1hr 20m 15s would return 1h 20m
       *       TimeInterval 1hr 20m 35s would return 1h 21m
       *       TimeInterval 1yr 1mo 29d would return 1yr 2mo
       *       
       * @param {object} intervalObj: An object containing sec, min, hour, day, month, year fields that can contain values
       *                             each contributing to a specific interval e.g. 1day, 6 hours and 2 mins ago. 
       */
      function __getFormattedInterval(intervalObj) {
        var result;
        // If we do have a number of years listed in the interval, then we need to display years and possibly months, if it 
        // isn't 0.
        if (intervalObj.years > 0) {
          // Get the current months, and check to see how many days are also listed. If the number of days is more than half the
          // month, then round up the number of months.
          var months = intervalObj.months;
          if (months > 0) {
            if (intervalObj.days > 16)
              months++;
            
            // Set the translated message for both year and month.
            result = lang.replace(i18n.STATS_DISPLAY_CUSTOM_TIME_LAST_YEAR_AND_MONTH_LABEL, [intervalObj.years, months]);
            
          } else {
            // If we have no months, then just set the translated message for only year.
            result = lang.replace(i18n.STATS_DISPLAY_CUSTOM_TIME_LAST_YEAR_LABEL, [intervalObj.years]);
          }
        } else if (intervalObj.months > 0) {
          // Get the current days, and check to see how many hours are also listed. If the number of hours is more than 12, 
          // then round up the number of days.
          var days = intervalObj.days;
          if (days > 0) {
            if (intervalObj.hours > 11)
              days++;
            
            // Set the translated message for both month and day.
            result = lang.replace(i18n.STATS_DISPLAY_CUSTOM_TIME_LAST_MONTH_AND_DAY_LABEL, [intervalObj.months, days]);
            
          } else {
            // If we have no months, then just set the translated message for only month.
            result = lang.replace(i18n.STATS_DISPLAY_CUSTOM_TIME_LAST_MONTH_LABEL, [intervalObj.months]);
          }
        } else if (intervalObj.days > 6) {
          // If we have at least one multiple of 7 days, then we have at least one week. Work out how many weeks we have and 
          // how many days remain and this will be our message.
          var weeks = Math.floor(intervalObj.days / 7);
          var days = intervalObj.days % 7;
          // If the number of hours is > 11 then add one to the days.
          if (intervalObj.hours > 11)
            days++;
          if (days > 0) {
            // Set the translated message for both week and day.
            result = lang.replace(i18n.STATS_DISPLAY_CUSTOM_TIME_LAST_WEEK_AND_DAY_LABEL, [weeks, days]);
            
          } else {
            // If we have no months, then just set the translated message for only week.
            result = lang.replace(i18n.STATS_DISPLAY_CUSTOM_TIME_LAST_WEEK_LABEL, [weeks]);
          }
        } else if (intervalObj.days > 0) {
          // Get the current hours, and check to see how many mins are also listed. If the number of mins is more than 30, 
          // then round up the number of hours.
          var hours = intervalObj.hours;
          if (hours > 0) {
            if (intervalObj.mins > 30)
              hours++;
            
            // Set the translated message for both day and hour.
            result = lang.replace(i18n.STATS_DISPLAY_CUSTOM_TIME_LAST_DAY_AND_HOUR_LABEL, [intervalObj.days, hours]);
            
          } else {
            // If we have no months, then just set the translated message for only day.
            result = lang.replace(i18n.STATS_DISPLAY_CUSTOM_TIME_LAST_DAY_LABEL, [intervalObj.days]);
          }
        } else if (intervalObj.hours > 0) {
          // Get the current hours, and check to see how many mins are also listed. If the number of mins is more than 30, 
          // then round up the number of hours.
          var mins = intervalObj.mins;
          if (mins > 0) {
            if (intervalObj.secs > 30)
              mins++;
            
            // Set the translated message for both hours and mins.
            result = lang.replace(i18n.STATS_DISPLAY_CUSTOM_TIME_LAST_HOUR_AND_MINUTE_LABEL, [intervalObj.hours, mins]);
            
          } else {
            // If we have no months, then just set the translated message for only hours.
            result = lang.replace(i18n.STATS_DISPLAY_CUSTOM_TIME_LAST_HOUR_LABEL, [intervalObj.hours]);
          }
        } else if (intervalObj.mins > 0) {
          // Get the current mins, and check to see how many secs are also listed.
          var secs = intervalObj.secs;
          if (secs > 0) {
            // Set the translated message for both mins and secs.
            result = lang.replace(i18n.STATS_DISPLAY_CUSTOM_TIME_LAST_MINUTE_AND_SECOND_LABEL, [intervalObj.mins, secs]);
            
          } else {
            // If we have no months, then just set the translated message for only hour.
            result = lang.replace(i18n.STATS_DISPLAY_CUSTOM_TIME_LAST_MINUTE_LABEL, [intervalObj.mins]);
          }
        } else if (intervalObj.secs > 0) {
          result = lang.replace(i18n.STATS_DISPLAY_CUSTOM_TIME_LAST_SECOND_LABEL, [intervalObj.secs]);
        } else {
          // Interval is zero
          result = i18n.STATS_TIME_SELECTOR_NOW_LABEL;
        }
        
        return result;
      }
      
      /**
       * This method generates a TimeInterval Object that contains the difference between 2 dates.
       * 
       * @param {Date} startDate: The "FROM" time in the From/To time range.
       * @param {Date} endDate: The "TO" time in the From/To time range.
       * 
       */
      function __generateTimeIntervalObjectFromDates(startDate, endDate) {
        // Convert the Dates to millisecs
        var startDateInMs = startDate.valueOf();
        var endDateInMs = endDate.valueOf();
        
        var interval = endDateInMs - startDateInMs;
        
        // Get an initial time interval obj.
        var result = __getInitialTimeIntervalObject();
        // Convert the interval to secs, from ms.
        interval = Math.floor(interval / 1000);
        
        if (interval > 0) {
          // Now calculate the secs/mins/hours and days.
          result.secs = interval % 60;
          result.mins = Math.floor((interval % 3600)  / 60);
          // We need to calculate the hour difference using the getHours rather than using
          // maths on the interval, because Daylight saving time is blended into the time in ms.
          
          var endHours = endDate.getHours();
          var startHours = startDate.getHours();
          // If the start hour and end hour are different and number of mins/secs of the enddate are less that the start, then
          // it means that the interval is less than an hour, and we need to add 1 from the start date. 
          // e.g. 15:22:33 means we need to find the difference between 16:00 and midnight and not 15:00 whereas 
          //      15:22:33 and 00:22:34 means that we need to find the difference between 15:00 and midnight.
          if (startHours !== endHours) {
            // If we're not in the same hour
            if (startDate.getMinutes() > endDate.getMinutes() ||
                 (startDate.getMinutes() === endDate.getMinutes() && startDate.getSeconds() > endDate.getSeconds()))
              startHours++;
            
            result.hours = (startHours > endHours ? (24 + endHours) - startHours : endHours - startHours);
          }
          
          if (interval >= 86400) {
            // Total days is the number of days of the rest of the interval, so it can be in the hundreds, if we are 
            //calculating an interval which is a year or more.
            result.totalDays = Math.floor(interval / 86400);
            
            if (result.totalDays > 28) {
              // We can't just use hardcoded values to calculate the months/years, so work out how many months the days map to,
              // and use the result to set the days, months and years.
              __processTimeIntervalObjectDaysToMonths(endDate, result);
            } else {
              result.days = result.totalDays;
            }
          }
        };
        
        return result;
      }
      
      /**
       * This method is used to create an interval object for the specific time interval. 
       * @param {Date} baseDate: The time to base all add/removes from.
       * @param {int} intervalType: String indicating the interval type. Values can be min, hour, day, week, month and year. 
       * @param {int} interval: A positive integer that contains the amount of interval time to take off the endtime.
       * @param {boolean} addToDate: A boolean indicating whether we calculating a future date or a past date.
       */
      function __generateTimeIntervalObjectFromInterval(baseDate, timeIntervalType, timeInterval, addToDate) {
        
        var result = __getInitialTimeIntervalObject();
        
        if (timeIntervalType === _INTERVAL_SEC) {
          result.secs = timeInterval;
        } else if (timeIntervalType === _INTERVAL_MIN) {
          result.mins = timeInterval;
        } else if (timeIntervalType === _INTERVAL_HOUR) {
          result.hours = timeInterval;
        } else if (timeIntervalType === _INTERVAL_DAY) {
          result.totalDays = timeInterval;
          // If we have more days than the shortest month, then we should really calculate the month/year etc.
          if (result.totalDays > 28) {
            __processTimeIntervalObjectDaysToMonths(baseDate, result);
          } else {
            result.days = result.totalDays;
          }
        } else if (timeIntervalType === _INTERVAL_WEEK) {
          result.totalDays = timeInterval * 7;
          //If we have more days than the shortest month, then we should really calculate the month/year etc.
          if (result.totalDays > 28) {
            __processTimeIntervalObjectDaysToMonths(baseDate, result);
          } else {
            result.days = result.totalDays;
          }
        } else if (timeIntervalType === _INTERVAL_MONTH) {
          result.totalDays = __monthsToDays(timeInterval, baseDate, addToDate);
         // Calculate the month/year etc.
         __processTimeIntervalObjectDaysToMonths(baseDate, result);
        } else if (timeIntervalType === _INTERVAL_YEAR) {
          result.totalDays = __monthsToDays(timeInterval * 12, baseDate, addToDate);
          // Calculate the month/year etc.
          __processTimeIntervalObjectDaysToMonths(baseDate, result);
        } 
        
        return result;
      }
      /**
       * This utility method takes an interval object and converts the totals days into days, months and years.
       * It updates the object passed in with the new values.
       * @param {Date} initialTime: The "TO" time in the From/To time range.
       * @param {object} intervalObj: An object containing sec, min, hour, day, month, year fields that can contain values
       *                             each contributing to a specific interval e.g. 1day, 6 hours and 2 mins ago. 
       */
      function __processTimeIntervalObjectDaysToMonths(baseDate, invervalObj) {
        var daysAndMonths = __daysToMonths(invervalObj.totalDays, baseDate, false);
        if (daysAndMonths.length > 0) {
          // the resulting array is [days, months].
          invervalObj.days = daysAndMonths[0];
          invervalObj.months = daysAndMonths[1] % 12;
          invervalObj.years = Math.floor(daysAndMonths[1] / 12);
        }
      } 
      /**
       * This method returns a blank TimeIntervalObject that can be used by the other methods.
       */
      function __getInitialTimeIntervalObject() {
         return{secs: 0, mins: 0, hours: 0, days: 0, months: 0, years: 0, totalDays: 0};
      }
      
      /**
       * This method compares 2 interval objects and returns true if they match and false if they don't.
       * @param {object} intervalObj1: An object containing sec, min, hour, day, month, year fields that can contain values
       *                           each contributing to a specific interval e.g. 1day, 6 hours and 2 mins ago. 
       * @param {object} intervalObj2: An object containing sec, min, hour, day, month, year fields that can contain values
       *                           each contributing to a specific interval e.g. 1day, 6 hours and 2 mins ago. 
       * 
       */
      function __compareIntervalObjects(intervalObj1, intervalObj2) {
        return intervalObj1.totalDays == intervalObj2.totalDays &&
               intervalObj1.years == intervalObj2.years &&
               intervalObj1.months == intervalObj2.months &&
               intervalObj1.days == intervalObj2.days &&
               intervalObj1.hours == intervalObj2.hours &&
               intervalObj1.mins == intervalObj2.mins &&
               intervalObj1.secs == intervalObj2.secs;
      }
});