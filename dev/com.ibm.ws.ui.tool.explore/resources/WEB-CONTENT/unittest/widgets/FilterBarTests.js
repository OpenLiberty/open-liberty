/*******************************************************************************
 * Copyright (c) 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/**
 * Test cases for FilterBar
 */
define([ "intern!tdd", "intern/chai!assert", "widgets/FilterBar", "dijit/_Widget" ],

/*
 * Grant the "Administrator" role to pass authorization checks.
 */
globalIsAdmin = true;
		
function(tdd, assert, FilterBar, Widget) {

  /**
   * Local variable in which to store created FilterBar instances. They will be destroyed after the test executes.
   */
  var filterBar = null;
  var dummyWidget = null;

  with (assert) {
    /**
     * Defines the 'viewFactory' module test suite.
     */
    tdd.suite('FilterBar Tests', function() {
      // ------------------------------------------------
      // Before each test
      // ------------------------------------------------
      tdd.before(function(){
        // executes before suite starts
        // Pass in the least amount of data to initalize FilterBar
        dummyWidget = new Widget();
        filterBar = new FilterBar({page: dummyWidget, id : "testId"});
      });
      
      tdd.beforeEach(function() {
        // executes before each test
      });

      // ------------------------------------------------
      // After each test
      // ------------------------------------------------
      tdd.afterEach(function() {
        // executes after each test
      });
      
      tdd.after(function () {
        // executes after suite ends
        if (filterBar) {
          filterBar.destroy();
          filterBar = null;
        }
      });

      // ------------------------------------------------
      // Tests
      // ------------------------------------------------
      // Test
      tdd.test('FilterBar - resourceIsInCurrentFilter - STOPPED filter - STOPPING resource', function() {
        var mockResource = {
          state : "STOPPING"
        };
        var filterView = "STOPPED";
        var expected = true;
        var result = filterBar.resourceIsInCurrentFilter(mockResource, filterView);
        assert.equal(expected, result, 'Did not get the expected ' + expected + ', instead got ' + result);
      });
      
      // Test
      tdd.test('FilterBar - resourceIsInCurrentFilter - STOPPED filter - STOPPED resource', function() {
        var mockResource = {
          state : "STOPPED"
        };
        var filterView = "STOPPED";
        var expected = true;
        var result = filterBar.resourceIsInCurrentFilter(mockResource, filterView);
        assert.equal(expected, result, 'Did not get the expected ' + expected + ', instead got ' + result);
      });
      
      // Test
      tdd.test('FilterBar - resourceIsInCurrentFilter - STOPPED filter - STARTING resource', function() {
        var mockResource = {
          state : "STARTING"
        };
        var filterView = "STOPPED";
        var expected = true;
        var result = filterBar.resourceIsInCurrentFilter(mockResource, filterView);
        assert.equal(expected, result, 'Did not get the expected ' + expected + ', instead got ' + result);
      });
      
      // Test
      tdd.test('FilterBar - resourceIsInCurrentFilter - STOPPED filter - STARTED resource', function() {
        var mockResource = {
          state : "STARTED"
        };
        var filterView = "STOPPED";
        var expected = false;
        var result = filterBar.resourceIsInCurrentFilter(mockResource, filterView);
        assert.equal(expected, result, 'Did not get the expected ' + expected + ', instead got ' + result);
      });
      
      // Test
      tdd.test('FilterBar - resourceIsInCurrentFilter - Total filter - STARTED resource', function() {
        var mockResource = {
          state : "STARTED"
        };
        var filterView = "Total";
        var expected = true;
        var result = filterBar.resourceIsInCurrentFilter(mockResource, filterView);
        assert.equal(expected, result, 'Did not get the expected ' + expected + ', instead got ' + result);
      });
      
      // Test
      tdd.test('FilterBar - resourceIsInCurrentFilter - Total filter - STARTED resource', function() {
        var mockResource = {
          state : "STARTED"
        };
        var filterView = "Total";
        var expected = true;
        var result = filterBar.resourceIsInCurrentFilter(mockResource, filterView);
        assert.equal(expected, result, 'Did not get the expected ' + expected + ', instead got ' + result);
      });
      
      // Test
      tdd.test('FilterBar - resourceIsInCurrentFilter - Total filter - STOPPED resource', function() {
        var mockResource = {
          state : "STOPPED"
        };
        var filterView = "Total";
        var expected = true;
        var result = filterBar.resourceIsInCurrentFilter(mockResource, filterView);
        assert.equal(expected, result, 'Did not get the expected ' + expected + ', instead got ' + result);
      });
      
      // Test
      tdd.test('FilterBar - resourceIsInCurrentFilter - Total filter - STARTING resource', function() {
        var mockResource = {
          state : "STARTING"
        };
        var filterView = "Total";
        var expected = true;
        var result = filterBar.resourceIsInCurrentFilter(mockResource, filterView);
        assert.equal(expected, result, 'Did not get the expected ' + expected + ', instead got ' + result);
      });
      
      // Test
      tdd.test('FilterBar - resourceIsInCurrentFilter - Total filter - STOPPING resource', function() {
        var mockResource = {
          state : "STOPPING"
        };
        var filterView = "Total";
        var expected = true;
        var result = filterBar.resourceIsInCurrentFilter(mockResource, filterView);
        assert.equal(expected, result, 'Did not get the expected ' + expected + ', instead got ' + result);
      });
      
      // Test
      tdd.test('FilterBar - resourceIsInCurrentFilter - Total filter - FAKE State resource', function() {
        var mockResource = {
          state : "FOOBAR"
        };
        var filterView = "Total";
        var expected = true;
        var result = filterBar.resourceIsInCurrentFilter(mockResource, filterView);
        assert.equal(expected, result, 'Did not get the expected ' + expected + ', instead got ' + result);
      });
      
      // Test
      tdd.test('FilterBar - resourceIsInCurrentFilter - Alert filter - has alerts', function() {
        var mockResource = {
            alerts: {count : 1}
        };
        var filterView = "Alert";
        var expected = true;
        var result = filterBar.resourceIsInCurrentFilter(mockResource, filterView);
        assert.equal(expected, result, 'Did not get the expected ' + expected + ', instead got ' + result);
      });
      
      // Test
      tdd.test('FilterBar - resourceIsInCurrentFilter - Alert filter - missing alerts', function() {
        var mockResource = {
            alerts: {}
        };
        var filterView = "Alert";
        var expected = false;
        var result = filterBar.resourceIsInCurrentFilter(mockResource, filterView);
        assert.equal(expected, result, 'Did not get the expected ' + expected + ', instead got ' + result);
      });
      
      // Test
      tdd.test('FilterBar - resourceIsInCurrentFilter - Alert filter - zero alerts', function() {
        var mockResource = {
            alerts: {count : 0}
        };
        var filterView = "Alert";
        var expected = false;
        var result = filterBar.resourceIsInCurrentFilter(mockResource, filterView);
        assert.equal(expected, result, 'Did not get the expected ' + expected + ', instead got ' + result);
      });
      
      // Test
      tdd.test('FilterBar - resourceIsInCurrentFilter - FAKE filter - FAKE resource state', function() {
        // Do not know if the product code was intentionally designed this way.  
        // This test captures the observed behavior when this was written.
        var mockResource = {
            state: "JUNK1234"
        };
        var filterView = "JUNK1234";
        var expected = true;
        var result = filterBar.resourceIsInCurrentFilter(mockResource, filterView);
        assert.equal(expected, result, 'Did not get the expected ' + expected + ', instead got ' + result);
      });
      
      // Test
      tdd.test('FilterBar - resourceIsInCurrentFilter - STARTED filter - STARTED resource state', function() {
        // Do not know if the product code was intentionally designed this way.  
        // This test captures the observed behavior when this was written.
        var mockResource = {
            state: "STARTED"
        };
        var filterView = "STARTED";
        var expected = true;
        var result = filterBar.resourceIsInCurrentFilter(mockResource, filterView);
        assert.equal(expected, result, 'Did not get the expected ' + expected + ', instead got ' + result);
      });
      
      
      
      
      
      
      
      
      
      
      
    });
  }
});
