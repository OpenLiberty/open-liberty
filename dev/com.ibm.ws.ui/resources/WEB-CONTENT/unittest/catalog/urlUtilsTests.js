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
  define(["intern!tdd", "intern/chai!assert", "js/catalog/urlUtils", "dojo/Deferred"], function (tdd, assert, utils, Deferred) {

    var server;
    
    with(assert) {
    
      /**
       * Defines the 'catalog' module test suite.
       */
      tdd.suite("Catalog Utils Tests", function() {
         
           tdd.beforeEach(function() {
             // Mock the admin center server since it is not available in a unittest
             server = sinon.fakeServer.create();
           });
           
           tdd.afterEach(function() {
             server.restore();
           });
      
           tdd.test("isValidUrl - positive cases", function() {
               // Tests that web URLs are considered valid
               // These assertions should all pass
               assert.isTrue(utils.isValidUrl('http://www.nytimes.com/'));
               assert.isTrue(utils.isValidUrl('www.dearabby.com'));
               assert.isTrue(utils.isValidUrl('google.com'));
               assert.isTrue(utils.isValidUrl('123greetings.com'));
           });

           tdd.test("isValidUrl - negative cases", function() {
               // Though the following URLs are considered valid by 
               // RFC 1738, for the UI's purposes, these should not be 
               // considered valid
               assert.isFalse(utils.isValidUrl('file:///c:/wendy/notvalid/hello.json'));
               assert.isFalse(utils.isValidUrl('mailto:wendydarlingtx@yahoo.com'));

               // This is just a bad URL
               assert.isFalse(utils.isValidUrl('//yahoo.com'));
           });

           tdd.test("isUrlAccessible - reachable", function() {
               var dfd = this.async(1000);

               utils.isUrlAccessible('http://ibm.com').then(dfd.callback(function(isValid) {
                     assert.isTrue(isValid, "The URL was not considered to be valid (non 200 response in the API response)");
               }), function(err) {
                   dfd.reject(err);
               });

               server.respondWith("GET", "/ibm/api/adminCenter/v1/utils/url/getStatus?url=http://ibm.com",
                       [200, { "Content-Type": "application/json" },'{"status":200,"url":"http://ibm.com"}']);
               server.respond();

               return dfd;
           });

           tdd.test("isUrlAccessible - unreachable", function() {
               var dfd = this.async(1000);

               utils.isUrlAccessible('http://doesntExist.com').then(dfd.callback(function(isValid) {
                     assert.isFalse(isValid, "The URL was considered to be valid (200 response in the API response)");
               }), function(err) {
                   dfd.reject(err);
               });

               server.respondWith("GET", "/ibm/api/adminCenter/v1/utils/url/getStatus?url=http://doesntExist.com",
                       [200, { "Content-Type": "application/json" },'{"status":404,"url":"http://doesntExist.com"}']);
               server.respond();

               return dfd;
           });
           
           tdd.test("analyzeURL - returns Deferred", function() {
               assert.isTrue(utils.analyzeURL('http://ibm.com') instanceof Deferred, "analyzeURL should return a Deferred");
           });

           tdd.test("analyzeURL - no such URL", function() {
               var dfd = this.async(1000);

               utils.analyzeURL('http://doesntExist.com').then(dfd.callback(function(toolProps) {
                     assert.equal(toolProps.name, "", "Returned tool properties did not have correct value for 'name'");
                     assert.equal(toolProps.version, "1.0", "Returned tool properties did not have correct value for 'version'");
                     assert.equal(toolProps.url, "http://doesntExist.com", "Returned tool properties did not have correct value for 'url'");
                     assert.equal(toolProps.description, "", "Returned tool properties did not have correct value for 'description'");
                     assert.equal(toolProps.icon, "images/tools/defaultTool_142x142.png", "Returned tool properties did not have correct value for 'icon'");
               }), function(err) {
                   dfd.reject(err);
               });

               server.respondWith("GET", "/ibm/api/adminCenter/v1/utils/url/getTool?url=http://doesntExist.com",
                       [200, { "Content-Type": "application/json" },'{"tool":{"id":"-1.0","name":"","version":"1.0","url":"http://doesntExist.com","description":"","icon":"images/tools/defaultTool_142x142.png"},"urlReachable":false}']);
               server.respond();

               return dfd;
           });

           tdd.test("analyzeURL - valid URL (ibm.com)", function() {
               var dfd = this.async(1000);

               utils.analyzeURL('http://ibm.com').then(dfd.callback(function(toolProps) {
                     assert.equal(toolProps.id, "IBM+-+United+States-1.0", "Returned tool properties did not have correct value for 'name'");
                     assert.equal(toolProps.name, "IBM - United States", "Returned tool properties did not have correct value for 'name'");
                     assert.equal(toolProps.version, "1.0", "Returned tool properties did not have correct value for 'version'");
                     assert.equal(toolProps.url, "http://ibm.com", "Returned tool properties did not have correct value for 'url'");
                     assert.equal(toolProps.description, "", "Returned tool properties did not have correct value for 'description'");
                     assert.equal(toolProps.icon, "images/tools/defaultTool_142x142.png", "Returned tool properties did not have correct value for 'icon'");
               }), function(err) {
                   dfd.reject(err);
               });

               server.respondWith("GET", "/ibm/api/adminCenter/v1/utils/url/getTool?url=http://ibm.com",
                       [200, { "Content-Type": "application/json" },'{"tool":{"id":"IBM+-+United+States-1.0","name":"IBM - United States","version":"1.0","url":"http://ibm.com","description":"","icon":"images/tools/defaultTool_142x142.png"},"urlReachable":true}']);
               server.respond();

               return dfd;
           });

           tdd.test("analyzeURL - no provided URL", function() {
               try {
                   utils.analyzeURL();
                   assert.isTrue(false, "analyzeURL should throw an error when no URL is provided");
               } catch(err) {
                   // Pass
               }
           });
        });
    }
});
