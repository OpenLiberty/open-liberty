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
//@ sourceURL=_logAnalyticsUtilsTest.js
define([
     "intern!tdd",
     "intern/chai!assert",
     "intern/chai!expect",
     "dojo/json",
     "resources/stats/_logAnalyticsUtils"
       ],
       
  function(tdd, assert, expect, json, utils) {

    // This test module is not enabled in the existing DOH-based unittests
    with(assert) {
      
      /**
       * Defines the 'JVM Stats Tests' module test suite.
       */
      tdd.suite('Log Analytics Utils Tests', function() {
  
        var server;
        var serverResource;
        
        // ------------------------------------------------
        // Before each test
        // ------------------------------------------------
        tdd.beforeEach(function() {
          // Mock the admin center server since it is not available in a unittest
          server = sinon.fakeServer.create();
          
          // Mock a server resource
          serverResource = {
              fullName: "localhost,C:/libertyHome/usr/servers/mockServer,mockServer",
              host: "localhost",
              userdir: "C:/libertyHome/usr/servers/mockServer"
          };
        });
        
        // ------------------------------------------------
        // After each test
        // ------------------------------------------------
        tdd.afterEach(function() {
          server.restore();
        });
        
        // ------------------------------------------------
        // Tests:
        //        isLogAnalyticsEnabled - this is very difficult to mock the nested deferred objects.
        //        configureLogAnalysisElements - should be covered by functional test
        //        getAllAvailablePipes - this is a simple rest call utility - unit test with mocks brings no value in catching changes in the real analytics APIs - functional test will cover this method
        //        getPipeKeys - this is a simple rest call utility with minimal REST API response processing.  Mocking will bring no value in catching real analytics API changes - functional test will cover this method
        //        getLogAnalyticsXHROptions
        // ------------------------------------------------
        // Test
        tdd.test('isLogAnalyticsEnabled - server has analytics disabled', function() {
          var result = utils.isLogAnalyticsEnabled(server);
          var expected = false;
          assert.equal(expected, result, "Expected server analytics to be disabled but got: " + result);
        });
        
        // Test
        tdd.test('getLogAnalyticsXHROptions', function() {
          var result = utils.getLogAnalyticsXHROptions();
          result = json.stringify(result);
          var expected = {
              handleAs : "text",
              preventCache : true,
              sync : false,
              headers : { "Content-type" : " text/html; charset=UTF-8" }
          }
          expected = json.stringify(expected);
          
          // Trying out the chai expect library (BDD style).
          expect(result, "Did not get expected result: " + result).to.equal(expected);
        });
        
        /*
        // Test
        tdd.test('isLogAnalyticsEnabled - server has analytics enabled', function() {
          var expected = true;
          
          // The challenge is that isLogAnalyticsEnabled calls other methods that have deferred objects.
          // We do not have a handle to those deferred objects to do asynchronous testing.
          
          // Here we try to mock the server to always return a mock response. The hope is that the mock response will
          // cause isLogAnalyticsEnabled to behave as if it got a valid server response because the server returns
          // the mock response.  But this coding technique seems to do nothing unlike the unit tests in _mbeanUtilsTest.
          server.respondWith("GET", '\/IBMJMXConnectorREST\/mbeans/', function(xhr) {
            var mbeansResponse = [{value : "someFeature"}];
            xhr.respond(200, { "Content-Type": "application/json"}, JSON.stringify(mbeansResponse));
          });

          var dfd = this.async(2000);
          var result = utils.isLogAnalyticsEnabled(server);
          assert.equal(expected, result, "Expected server analytics to be enabled but got: " + result);
          
          // Trigger the server.  Causes all queued asynchronous requests to receive a response.
          server.respond();
        });
        */
        

        
        
        
      }); // Don't add tests past this line
    };
});
