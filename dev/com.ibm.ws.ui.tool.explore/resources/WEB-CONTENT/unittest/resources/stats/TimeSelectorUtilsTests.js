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
/**
 * Test cases for TimeSelector
 */
define(["intern!tdd",
        "intern/chai!assert",
        "resources/stats/TimeSelectorUtils"
         ],
         
         function(tdd, assert, timeUtils) {

  /**
   * Local variable in which to store created TimeSelector instances. They will be destroyed
   * after the test executes.
   */
  var jan012015 = new Date("January 01, 2015 00:00:00");
  var jan012017 = new Date("January 01, 2017 00:00:00");
  
  var feb272015 = new Date("February 27, 2015 00:00:00");
  
  var feb282015 = new Date("February 28, 2015 00:00:00");
  var feb282016 = new Date("February 28, 2016 00:00:00");
  var feb282017 = new Date("February 28, 2017 00:00:00");
  
  var feb292016 = new Date("February 29, 2016 00:00:00");
  var feb292020 = new Date("February 29, 2020 00:00:00");
  
  var mar012015 = new Date("March 01, 2015 00:00:00");
  var mar012016 = new Date("March 01, 2016 00:00:00");
  
  var mar072015 = new Date("March 07, 2015 00:00:00");
  var mar072016 = new Date("March 07, 2016 00:00:00");
  
  var basicDateCheck = [{start: jan012015, end: new Date("December 31, 2014 23:59:59"), intervalType: timeUtils.INTERVAL_SEC, interval: 1},
                        {start: jan012015, end: new Date("December 31, 2014 23:59:00"), intervalType: timeUtils.INTERVAL_MIN, interval: 1},
                        {start: jan012015, end: new Date("December 31, 2014 23:00:00"), intervalType: timeUtils.INTERVAL_HOUR, interval: 1},
                        {start: jan012015, end: new Date("December 31, 2014 00:00:00"), intervalType: timeUtils.INTERVAL_DAY, interval: 1},
                        {start: jan012015, end: new Date("December 25, 2014 00:00:00"), intervalType: timeUtils.INTERVAL_WEEK, interval: 1},
                        {start: jan012015, end: new Date("December 01, 2014 00:00:00"), intervalType: timeUtils.INTERVAL_MONTH, interval: 1},
                        {start: jan012015, end: new Date("January 01, 2014 00:00:00"), intervalType: timeUtils.INTERVAL_YEAR, interval: 1}];
  
  var secondCheck = [{start: jan012015, end: new Date("December 31, 2014 23:59:50"), intervalType: timeUtils.INTERVAL_SEC, interval: 10},
                     {start: jan012015, end: new Date("December 31, 2014 23:58:00"), intervalType: timeUtils.INTERVAL_SEC, interval: 120},
                     {start: jan012015, end: new Date("December 31, 2014 23:00:00"), intervalType: timeUtils.INTERVAL_SEC, interval: 3600},
                     {start: jan012015, end: new Date("December 31, 2014 00:00:00"), intervalType: timeUtils.INTERVAL_SEC, interval: 86400},
                     {start: jan012015, end: new Date("December 01, 2014 00:00:00"), intervalType: timeUtils.INTERVAL_SEC, interval: 2678400},
                     {start: jan012015, end: new Date("January 01, 2014 00:00:00"), intervalType: timeUtils.INTERVAL_SEC, interval: 31536000},
                     {start: mar012016, end: new Date("February 29, 2016 23:59:59"), intervalType: timeUtils.INTERVAL_SEC, interval: 1},
                     {start: mar012016, end: feb292016, intervalType: timeUtils.INTERVAL_SEC, interval: 86400},
                     {start: mar012016, end: feb282016, intervalType: timeUtils.INTERVAL_SEC, interval: 172800},
                     {start: mar012015, end: feb282015, intervalType: timeUtils.INTERVAL_SEC, interval: 86400},
                     {start: mar012015, end: feb272015, intervalType: timeUtils.INTERVAL_SEC, interval: 172800}];
  
  var minuteCheck = [{start: jan012015, end: new Date("December 31, 2014 23:50:00"), intervalType: timeUtils.INTERVAL_MIN, interval: 10},
                     {start: jan012015, end: new Date("December 31, 2014 22:00:00"), intervalType: timeUtils.INTERVAL_MIN, interval: 120},
                     {start: jan012015, end: new Date("December 31, 2014 00:00:00"), intervalType: timeUtils.INTERVAL_MIN, interval: 1440},
                     {start: jan012015, end: new Date("December 01, 2014 00:00:00"), intervalType: timeUtils.INTERVAL_MIN, interval: 44640},
                     {start: jan012015, end: new Date("January 01, 2014 00:00:00"), intervalType: timeUtils.INTERVAL_MIN, interval: 525600},
                     {start: mar012016, end: new Date("February 29, 2016 23:59:00"), intervalType: timeUtils.INTERVAL_MIN, interval: 1},
                     {start: mar012016, end: feb292016, intervalType: timeUtils.INTERVAL_MIN, interval: 1440},
                     {start: mar012016, end: feb282016, intervalType: timeUtils.INTERVAL_MIN, interval: 2880},
                     {start: mar012015, end: feb282015, intervalType: timeUtils.INTERVAL_MIN, interval: 1440},
                     {start: mar012015, end: feb272015, intervalType: timeUtils.INTERVAL_MIN, interval: 2880},
                     {start: new Date("Jan 01, 2016 18:10:07"), end: new Date("Jan 01, 2016 18:05:07"), intervalType: timeUtils.INTERVAL_MIN, interval: 5}];
  
  var hourCheck = [{start: jan012015, end: new Date("December 31, 2014 19:00:00"), intervalType: timeUtils.INTERVAL_HOUR, interval: 5},
                   {start: jan012015, end: new Date("December 30, 2014 12:00:00"), intervalType: timeUtils.INTERVAL_HOUR, interval: 36},
                   {start: jan012015, end: new Date("December 01, 2014 00:00:00"), intervalType: timeUtils.INTERVAL_HOUR, interval: 744},
                   {start: mar012016, end: new Date("February 29, 2016 23:00:00"), intervalType: timeUtils.INTERVAL_HOUR, interval: 1},
                   {start: mar012016, end: feb292016, intervalType: timeUtils.INTERVAL_HOUR, interval: 24},
                   {start: mar012016, end: feb282016, intervalType: timeUtils.INTERVAL_HOUR, interval: 48},
                   {start: mar012015, end: feb282015, intervalType: timeUtils.INTERVAL_HOUR, interval: 24},
                   {start: mar012015, end: feb272015, intervalType: timeUtils.INTERVAL_HOUR, interval: 48}];
  
  var dayCheck = [{start: jan012015, end: new Date("December 27, 2014 00:00:00"), intervalType: timeUtils.INTERVAL_DAY, interval: 5},
                  {start: jan012015, end: new Date("November 30, 2014 00:00:00"), intervalType: timeUtils.INTERVAL_DAY, interval: 32},
                  {start: jan012015, end: new Date("January 01, 2014 00:00:00"), intervalType: timeUtils.INTERVAL_DAY, interval: 365},
                  {start: jan012015, end: new Date("January 01, 2013 00:00:00"), intervalType: timeUtils.INTERVAL_DAY, interval: 730},
                  {start: mar012016, end: feb292016, intervalType: timeUtils.INTERVAL_DAY, interval: 1},
                  {start: mar012016, end: feb282016, intervalType: timeUtils.INTERVAL_DAY, interval: 2},
                  {start: mar012015, end: feb282015, intervalType: timeUtils.INTERVAL_DAY, interval: 1},
                  {start: mar012015, end: feb272015, intervalType: timeUtils.INTERVAL_DAY, interval: 2}];
  
  var weekCheck = [{start: jan012015, end: new Date("December 18, 2014 00:00:00"), intervalType: timeUtils.INTERVAL_WEEK, interval: 2},
                   {start: jan012015, end: new Date("September 18, 2014 00:00:00"), intervalType: timeUtils.INTERVAL_WEEK, interval: 15},
                   {start: jan012015, end: new Date("January 02, 2014 00:00:00"), intervalType: timeUtils.INTERVAL_WEEK, interval: 52},
                   {start: mar072016, end: feb292016, intervalType: timeUtils.INTERVAL_WEEK, interval: 1},
                   {start: mar072016, end: new Date("February 22, 2016 00:00:00"), intervalType: timeUtils.INTERVAL_WEEK, interval: 2},
                   {start: mar072015, end: feb282015, intervalType: timeUtils.INTERVAL_WEEK, interval: 1},
                   {start: mar072015, end: new Date("February 21, 2015 00:00:00"), intervalType: timeUtils.INTERVAL_WEEK, interval: 2}];
  
  var monthCheck = [{start: jan012015, end: new Date("August 01, 2014 00:00:00"), intervalType: timeUtils.INTERVAL_MONTH, interval: 5},
                    {start:  new Date("Jun 12, 2015 00:00:00"), end: new Date("May 12, 2015 00:00:00"), intervalType: timeUtils.INTERVAL_MONTH, interval: 1},
                    {start: jan012015, end: new Date("January 01, 2014 00:00:00"), intervalType: timeUtils.INTERVAL_MONTH, interval: 12},
                    {start: jan012015, end: new Date("January 01, 2013 00:00:00"), intervalType: timeUtils.INTERVAL_MONTH, interval: 24}];
  
  var yearCheck = [
                   {start: jan012015, end: new Date("January 01, 2013 00:00:00"), intervalType: timeUtils.INTERVAL_YEAR, interval: 2},
                   {start: jan012015, end: new Date("January 01, 2000 00:00:00"), intervalType: timeUtils.INTERVAL_YEAR, interval: 15},
                   {start: jan012015, end: new Date("January 01, 1999 00:00:00"), intervalType: timeUtils.INTERVAL_YEAR, interval: 16},
                   {start: feb292020, end: feb292016, intervalType: timeUtils.INTERVAL_YEAR, interval: 4},
                   {start: feb282016, end: feb282015, intervalType: timeUtils.INTERVAL_YEAR, interval: 1}];
  
  var intervalFromDates = [
                           {end: jan012015, start: new Date("December 31, 2014 23:59:59"), secs: 1, mins: 0, hours: 0, days: 0, months: 0, years: 0, totalDays: 0},
                           {end: jan012015, start: new Date("December 31, 2014 23:59:00"), secs: 0, mins: 1, hours: 0, days: 0, months: 0, years: 0, totalDays: 0},
                           {end: jan012015, start: new Date("December 31, 2014 23:00:00"), secs: 0, mins: 0, hours: 1, days: 0, months: 0, years: 0, totalDays: 0},
                           {end: jan012015, start: new Date("December 31, 2014 00:00:00"), secs: 0, mins: 0, hours: 0, days: 1, months: 0, years: 0, totalDays: 1},
                           {end: jan012015, start: new Date("December 25, 2014 00:00:00"), secs: 0, mins: 0, hours: 0, days: 7, months: 0, years: 0, totalDays: 7},
                           {end: jan012015, start: new Date("December 01, 2014 00:00:00"), secs: 0, mins: 0, hours: 0, days: 0, months: 1, years: 0, totalDays: 31},
                           {end: jan012015, start: new Date("January 01, 2014 00:00:00"), secs: 0, mins: 0, hours: 0, days: 0, months: 0, years: 1, totalDays: 365},
                           {end: new Date("January 01, 2013 00:00:00"), start: new Date("January 01, 2012 00:00:00"), secs: 0, mins: 0, hours: 0, days: 0, months: 0, years: 1, totalDays: 366},
                           {end: jan012015, start: new Date("September 18, 2013 15:22:33"), secs: 27, mins: 37, hours: 8, days: 12, months: 3, years: 1, totalDays: 469},
                           {end: mar012015, start: feb282015, secs: 0, mins: 0, hours: 0, days: 1, months: 0, years: 0, totalDays: 1},
                           {end: mar012016, start: feb282016, secs: 0, mins: 0, hours: 0, days: 2, months: 0, years: 0, totalDays: 2},
                           {end:  new Date("June 24, 2015 18:40:10"), start: new Date("June 24, 2015 17:39:10"), secs: 0, mins: 1, hours: 1, days: 0, months: 0, years: 0, totalDays: 0},
                           {end:  new Date("June 24, 2015 18:40:10"), start: new Date("June 24, 2015 16:39:10"), secs: 0, mins: 1, hours: 2, days: 0, months: 0, years: 0, totalDays: 0},
                           {end:  new Date("June 24, 2015 18:40:10"), start: new Date("June 24, 2015 17:41:10"), secs: 0, mins: 59, hours: 0, days: 0, months: 0, years: 0, totalDays: 0},
                           {end:  new Date("June 25, 2015 00:10:10"), start: new Date("June 24, 2015 23:50:10"), secs: 0, mins: 20, hours: 0, days: 0, months: 0, years: 0, totalDays: 0}];
  
  var intervalFormattedMsgFromInterval = [{display: "Last 5s", secs: 5, mins: 0, hours: 0, days: 0, months: 0, years: 0, totalDays: 0},
                                          {display: "Last 1m", secs: 0, mins: 1, hours: 0, days: 0, months: 0, years: 0, totalDays: 0},
                                          {display:"Last 1h", secs: 0, mins: 0, hours: 1, days: 0, months: 0, years: 0, totalDays: 1},
                                          {display:"Last 1d", secs: 0, mins: 0, hours: 0, days: 1, months: 0, years: 0, totalDays: 1},
                                          {display:"Last 1w", secs: 0, mins: 0, hours: 0, days: 7, months: 0, years: 0, totalDays: 31},
                                          {display:"Last 1mo", secs: 0, mins: 0, hours: 0, days: 0, months: 1, years: 0, totalDays: 31},
                                          {display:"Last 1y", secs: 0, mins: 0, hours: 0, days: 0, months: 0, years: 1, totalDays: 365},
                                          {display:"Last 1m 5s", secs: 5, mins: 1, hours: 0, days: 0, months: 0, years: 0, totalDays: 0},
                                          {display:"Last 1h 5m", secs: 0, mins: 5, hours: 1, days: 0, months: 0, years: 0, totalDays: 0},
                                          {display:"Last 1d 6h", secs: 0, mins: 0, hours: 6, days: 1, months: 0, years: 0, totalDays: 0},
                                          {display:"Last 2w 4d", secs: 0, mins: 0, hours: 0, days: 18, months: 0, years: 0, totalDays: 18},
                                          {display:"Last 2mo 7d", secs: 0, mins: 0, hours: 0, days: 7, months: 2, years: 0, totalDays: 69},
                                          {display:"Last 1y 6mo", secs: 0, mins: 0, hours: 0, days: 0, months: 6, years: 1, totalDays: 0},
                                          
                                          {display:"Last 1h 6m", secs: 40, mins: 5, hours: 1, days: 0, months: 0, years: 0, totalDays: 0},
                                          {display:"Last 4d 8h", secs: 0, mins: 55, hours: 7, days: 4, months: 0, years: 0, totalDays: 4},
                                          {display:"Last 2w 6d", secs: 0, mins: 0, hours: 15, days: 19, months: 0, years: 0, totalDays: 19},
                                          {display:"Last 2mo 6d", secs: 0, mins: 0, hours: 13, days: 5, months: 2, years: 0, totalDays: 67},
                                          {display:"Last 1y 7mo", secs: 0, mins: 0, hours: 0, days: 28, months: 6, years: 1, totalDays: 0},
                                          
                                          {display:"Last 1h 5m", secs: 20, mins: 5, hours: 1, days: 0, months: 0, years: 0, totalDays: 0},
                                          {display:"Last 4d 7h", secs: 0, mins: 29, hours: 7, days: 4, months: 0, years: 0, totalDays: 4},
                                          {display:"Last 2w 5d", secs: 0, mins: 0, hours: 5, days: 19, months: 0, years: 0, totalDays: 19},
                                          {display:"Last 2mo 5d", secs: 0, mins: 0, hours: 11, days: 5, months: 2, years: 0, totalDays: 67},
                                          {display:"Last 1y 6mo", secs: 0, mins: 0, hours: 0, days: 13, months: 6, years: 1, totalDays: 561},
                                          
                                          {display:"Last 1y 6mo", secs: 0, mins: 1, hours: 0, days: 0, months: 6, years: 1, totalDays: 0},
                                          {display:"Last 4d 8h", secs: 25, mins: 55, hours: 7, days: 4, months: 0, years: 0, totalDays: 4},
                                          {display: "Last 1m", secs: 0, mins: 1, hours: 0, days: 0, months: 0, years: 0, totalDays: 0},
                                          {display:"Last 2w 5d", secs: 0, mins: 55, hours: 5, days: 19, months: 0, years: 0, totalDays: 19}];
  
  var forwardCheck = function(dateTimeArray) {
    for (var i = 0 ; i < dateTimeArray.length; i++) {
      
      // Test the date using the interval method 
      var newTimeFromInterval = timeUtils.calculateNewTimeFromInterval(dateTimeArray[i].end, dateTimeArray[i].intervalType, 
                                                                       dateTimeArray[i].interval, true);
      
      assert.equal(dateTimeArray[i].start.valueOf(), newTimeFromInterval.valueOf(), "Add " + dateTimeArray[i].interval + " " + dateTimeArray[i].intervalType + 
          " from " + dateTimeArray[i].end + ": " + newTimeFromInterval + " doesn't equal expected " + dateTimeArray[i].start);
      
      // Test the date using the interval object method 
      var newTimeFromIntervalObject = timeUtils.calculateNewTimeFromIntervalObject(dateTimeArray[i].end, 
                    timeUtils.generateTimeIntervalObjectFromInterval(dateTimeArray[i].end, dateTimeArray[i].intervalType, 
                                                           dateTimeArray[i].interval, true)
                                                           , true);
      
      assert.equal(dateTimeArray[i].start.valueOf(), newTimeFromIntervalObject.valueOf(), "Add " + dateTimeArray[i].interval + " " + dateTimeArray[i].intervalType + 
          " from " + dateTimeArray[i].end + ": " + newTimeFromIntervalObject + " doesn't equal expected " + dateTimeArray[i].start);
    };
  };
  
  var backwardCheck = function(dateTimeArray) {
    for (var i = 0 ; i < dateTimeArray.length; i++) {
      var newTimeFromInterval = timeUtils.calculateNewTimeFromInterval(dateTimeArray[i].start, dateTimeArray[i].intervalType, 
                                                           dateTimeArray[i].interval, false);
      assert.equal(dateTimeArray[i].end.valueOf(), newTimeFromInterval.valueOf(), "Deduct " + dateTimeArray[i].interval + " " + dateTimeArray[i].intervalType + 
          " from " + dateTimeArray[i].start + ": " + newTimeFromInterval + " doesn't equal expected " + dateTimeArray[i].end);
      
      var newTimeFromIntervalObject = timeUtils.calculateNewTimeFromIntervalObject(dateTimeArray[i].start, 
          timeUtils.generateTimeIntervalObjectFromInterval(dateTimeArray[i].start, dateTimeArray[i].intervalType, 
                                                           dateTimeArray[i].interval, false), 
                                                                     false);
      assert.equal(dateTimeArray[i].end.valueOf(), newTimeFromIntervalObject.valueOf(), "Deduct " + dateTimeArray[i].interval + " " + dateTimeArray[i].intervalType + 
          " from " + dateTimeArray[i].start + ": " + newTimeFromIntervalObject + " doesn't equal expected " + dateTimeArray[i].end);
    };
  };
  
  var checkIntervalObject = function(intervalObject, secs, mins, hours, days, months, years, totalDays, dateDisplay) {
    
    assert.equal(intervalObject.secs, secs, "Interval Object doesn't contain expected seconds (" + secs + "): " + 
        intervalObject.secs + " for " + dateDisplay);
    assert.equal(intervalObject.mins, mins, "Interval Object doesn't contain expected minutes (" + mins + "): " + 
        intervalObject.mins + " for " + dateDisplay);
    assert.equal(intervalObject.hours, hours, "Interval Object doesn't contain expected hours (" + hours + "): " + 
        intervalObject.hours + " for " + dateDisplay);
    assert.equal(intervalObject.days, days, "Interval Object doesn't contain expected days (" + days + "): " + 
        intervalObject.days + " for " + dateDisplay);
    assert.equal(intervalObject.months, months, "Interval Object doesn't contain expected months (" + months + "): " + 
        intervalObject.months + " for " + dateDisplay);
    assert.equal(intervalObject.years, years, "Interval Object doesn't contain expected years (" + years + "): " + 
        intervalObject.years + " for " + dateDisplay);
    assert.equal(intervalObject.totalDays, totalDays, "Interval Object doesn't contain expected totalDays (" + totalDays + "): " + 
        intervalObject.totalDays + " for " + dateDisplay);
  };

  with(assert) {
    
    /**
     * Defines the 'viewFactory' module test suite.
     */
    tdd.suite('TimeSelector Tests', function() {
  
      tdd.test('timeSelector - Running Basic Date Deduction check', function() {
          backwardCheck(basicDateCheck);
      });
        
      tdd.test('timeSelector - Running Basic Date Addition check', function() {
        forwardCheck(basicDateCheck);
      });
      
      tdd.test('timeSelector - Running Second Date Deduction check', function() {
        backwardCheck(secondCheck);
      });
      
      tdd.test('timeSelector - Running Second Date Addition check', function() {
        forwardCheck(secondCheck);
      });
      
      tdd.test('timeSelector - Running Minute Date Deduction check', function() {
        backwardCheck(minuteCheck);
      });
      
      tdd.test('timeSelector - Running Minute Date Addition check', function() {
        forwardCheck(minuteCheck);
      });
      
      tdd.test('timeSelector - Running Hour Date Deduction check', function() {
        backwardCheck(hourCheck);
      });
      
      tdd.test('timeSelector - Running Hour Date Addition check', function() {
        forwardCheck(hourCheck);
      });
      
      tdd.test('timeSelector - Running Day Date Deduction check', function() {
        backwardCheck(dayCheck);
      });
      
      tdd.test('timeSelector - Running Day Date Addition check', function() {
        forwardCheck(dayCheck);
      });
      
      tdd.test('timeSelector - Running Week Date Deduction check', function() {
        backwardCheck(weekCheck);
      });
      
      tdd.test('timeSelector - Running Week Date Addition check', function() {
        forwardCheck(weekCheck);
      });
      
      tdd.test('timeSelector - Running Month Date Deduction check', function() {
        backwardCheck(monthCheck);
        // Run some Leap Month checks that don't work in reverse.
        backwardCheck([{start: new Date("March 31, 2016 00:00:00"), end: feb292016, intervalType: timeUtils.INTERVAL_MONTH, interval: 1},
                       {start: new Date("March 31, 2015 00:00:00"), end: feb282015, intervalType: timeUtils.INTERVAL_MONTH, interval: 1}]);
      });
      
      tdd.test('timeSelector - Running Month Date Addition check', function() {
        forwardCheck(monthCheck);
        
        // Run some Leap Month checks that don't work in reverse.
        forwardCheck([{start: new Date("March 29, 2016 00:00:00"), end: feb292016, intervalType: timeUtils.INTERVAL_MONTH, interval: 1},
                      {start: new Date("March 28, 2015 00:00:00"), end: feb282015, intervalType: timeUtils.INTERVAL_MONTH, interval: 1}]);
      });
      
      tdd.test('timeSelector - Running Year Date Deduction check', function() {
        backwardCheck(yearCheck);
        
        // Run some Leap Month checks that don't work in reverse.
        backwardCheck([{start: feb292016, end: feb282015, intervalType: timeUtils.INTERVAL_YEAR, interval: 1}]);
      });
      
      tdd.test('timeSelector - Running Year Date Addition check', function() {
        forwardCheck(yearCheck);
        
        forwardCheck([{start: feb282017, end: feb282016, intervalType: timeUtils.INTERVAL_YEAR, interval: 1}]);
      });  
            
      tdd.test('timeSelector - Test generating IntervalObjects from Dates', function() {
        for (var i = 0 ; i < intervalFromDates.length; i++) {
          
          var intervalObject = timeUtils.generateTimeIntervalObjectFromDates(intervalFromDates[i].start, intervalFromDates[i].end);
          checkIntervalObject(intervalObject,
                              intervalFromDates[i].secs, 
                              intervalFromDates[i].mins, 
                              intervalFromDates[i].hours, 
                              intervalFromDates[i].days, 
                              intervalFromDates[i].months, 
                              intervalFromDates[i].years,
                              intervalFromDates[i].totalDays,
                              "StartDate: " + intervalFromDates[i].start + " to EndDate:" + intervalFromDates[i].end);
        };
      }); 
      
      tdd.test('timeSelector - Test Formatted Intervals', function() {
        
        for (var i = 0 ; i < intervalFormattedMsgFromInterval.length; i++) {
          
          var intervalObj = timeUtils.getInitialTimeIntervalObject();
          intervalObj.secs = intervalFormattedMsgFromInterval[i].secs;
          intervalObj.mins = intervalFormattedMsgFromInterval[i].mins;
          intervalObj.hours = intervalFormattedMsgFromInterval[i].hours;
          intervalObj.days = intervalFormattedMsgFromInterval[i].days;
          intervalObj.months = intervalFormattedMsgFromInterval[i].months;
          intervalObj.years = intervalFormattedMsgFromInterval[i].years;
          intervalObj.totalDays = intervalFormattedMsgFromInterval[i].totalDays;
          
          var formattedDisplay = timeUtils.getFormattedInterval(intervalObj);
          assert.equal(formattedDisplay, intervalFormattedMsgFromInterval[i].display, 
              "Display Label doesn't match: Expected(" + intervalFormattedMsgFromInterval[i].display + "): " + formattedDisplay);
        };
      }); 
    });
  }
});
