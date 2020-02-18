/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
define([
     "intern!tdd",
     "intern/chai!assert",
     "js/catalog/catalog",
     "dojo/Deferred"
       ],
       
   function (tdd, assert, catalog, Deferred) {
     
      var server, testBookmark, testFeatureTool;
      
      with(assert) {

        /**
         * Defines the 'catalog' module test suite.
         */
        tdd.suite("Catalog Tests", function() {
          
          tdd.before(function() {
              testBookmark = {
                id: "myTool",
                type: "bookmark",
                name: "myTool",
                url: "ibm.com",
                description: "IBM",
                icon: "default.png"
              };
              
              testFeatureTool = {
                  id: "testFeature-1.0",
                  type:"featureTool",
                  featureName: "testFeature",
                  featureVersion: "1.0",
                  name: "myTool",
                  url: "ibm.com",
                  description: "IBM",
                  icon: "default.png"
              };
          });
          
          tdd.beforeEach(function() {
            // Mock the admin center server since it is not available in a unittest
            server = sinon.fakeServer.create();
         });
          
         tdd.afterEach(function() {
            server.restore();
         });

         tdd.test("Bookmark - create", function() {
           var tool = new Catalog.Bookmark(testBookmark);
           assert.equal(tool.id, testBookmark.id, "Tool was not constructed with correct value for 'id'");
           assert.equal(tool.type, testBookmark.type, "Tool was not constructed with correct value for 'type'");
           assert.equal(tool.name, testBookmark.name, "Tool was not constructed with correct value for 'name'");
           assert.equal(tool.url, testBookmark.url, "Tool was not constructed with correct value for 'url'");
           assert.equal(tool.description, testBookmark.description, "Tool was not constructed with correct value for 'description'");
           assert.equal(tool.icon, testBookmark.icon, "Tool was not constructed with correct value for 'icon'");
         });
         
         tdd.test("FeatureTool - create", function() {
             var tool = new Catalog.FeatureTool(testFeatureTool);
             assert.equal(tool.id, testFeatureTool.id, "Tool was not constructed with correct value for 'id'");
             assert.equal(tool.type, testFeatureTool.type, "Tool was not constructed with correct value for 'type'");
             assert.equal(tool.featureName, testFeatureTool.featureName, "Tool was not constructed with correct value for 'featureName'");
             assert.equal(tool.featureVersion, testFeatureTool.featureVersion, "Tool was not constructed with correct value for 'featureVersion'");
             assert.equal(tool.name, testFeatureTool.name, "Tool was not constructed with correct value for 'name'");
             assert.equal(tool.url, testFeatureTool.url, "Tool was not constructed with correct value for 'url'");
             assert.equal(tool.description, testFeatureTool.description, "Tool was not constructed with correct value for 'description'");
             assert.equal(tool.icon, testFeatureTool.icon, "Tool was not constructed with correct value for 'icon'");
         });
  
         tdd.test("Catalog - construct", function() {
             var clog = new Catalog();
             assert.isTrue(clog instanceof Catalog, "Unable to construct Catalog");
         });
  
         tdd.test("Catalog - get instance", function() {
             var clog = catalog.getCatalog();
             assert.isTrue(clog instanceof Catalog, "Unable to construct Catalog");
         });
  
         tdd.test("Catalog - get instance is same instance", function() {
             var clogNew = new Catalog();
             var clogInst = catalog.getCatalog();
             assert.isFalse(clogInst ===  clogNew, "The 'singleton' instance of Catalog should not match a 'new' instance of Catalog");
             assert.isTrue(clogInst ===  catalog.getCatalog(), "The 'singleton' instance of Catalog should match the previous return for the singleton");
         });
  
         tdd.test("Catalog.getTools - returns Deferred", function() {
             assert.isTrue(catalog.getCatalog().getTools() instanceof Deferred, "Catalog.getTools should return a Deferred");
         });
  
         tdd.test("Catalog.getTools - resolves with Array (no Tools)", function() {
             var dfd = this.async(1000);
  
             catalog.getCatalog().getTools().then(dfd.callback(function(tools) {
                 assert.isTrue(tools instanceof Array, "Catalog.getTools should resolve with an Array");
             }), function(err) {
                 dfd.reject(err);
             });
  
             server.respondWith("GET", "/ibm/api/adminCenter/v1/catalog",
                     [200, { "Content-Type": "application/json" },'{"_metadata":{},"bookmarks":[]}']);
             server.respond();
  
             return dfd;
         });
  
         tdd.test("Catalog.getTools - resolves with Array of Tool objects", function() {
             var dfd = this.async(1000);
  
             catalog.getCatalog().getTools().then(dfd.callback(function(tools) {
                   assert.isTrue(tools instanceof Array, "Catalog.getTools should resolve with an Array");
                   assert.equal(1, tools.length, "Expected exactly 1 tool back from the mock response");

                   var tool = tools[0];
                   assert.equal(tool.id, testBookmark.id, "Tool was not constructed with correct value for 'id'");
                   assert.equal(tool.type, testBookmark.type, "Tool was not constructed with correct value for 'type'");
                   assert.equal(tool.name, testBookmark.name, "Tool was not constructed with correct value for 'name'");
                   assert.equal(tool.url, testBookmark.url, "Tool was not constructed with correct value for 'url'");
                   assert.equal(tool.description, testBookmark.description, "Tool was not constructed with correct value for 'description'");
                   assert.equal(tool.icon, testBookmark.icon, "Tool was not constructed with correct value for 'icon'");
             }), function(err) {
                 dfd.reject(err);
             });
  
             server.respondWith("GET", "/ibm/api/adminCenter/v1/catalog",
                     [200, { "Content-Type": "application/json" },'{"_metadata":{},"bookmarks":['+JSON.stringify(testBookmark)+']}']);
             server.respond();
  
             return dfd;
         });
  
         tdd.test("Catalog.getTools - filtered Tool", function() {
             var dfd = this.async(1000);
  
             catalog.getCatalog().getTools("bookmarks.name,bookmarks.url").then(dfd.callback(function(tools) {
                   assert.isTrue(tools instanceof Array, "Catalog.getTools should resolve with an Array");
                   assert.equal(1, tools.length, "Expected exactly 1 tool back from the mock response");

                   var tool = tools[0];
                   assert.equal(tool.id, null, "Tool was filtered and should not have an 'id'");
                   assert.equal(tool.type, null, "Tool was filtered and should not have a 'type'");
                   assert.equal(tool.name, testBookmark.name, "Tool was not constructed with correct value for 'name'");
                   assert.equal(tool.url, testBookmark.url, "Tool was not constructed with correct value for 'url'");
                   assert.equal(tool.description, null, "Tool was filtered and should not have a 'description'");
                   assert.equal(tool.icon, null, "Tool was filtered and should not have an 'icon'");
             }), function(err) {
                 dfd.reject(err);
             });
  
             server.respondWith("GET", "/ibm/api/adminCenter/v1/catalog?fields=bookmarks.name,bookmarks.url",
                     [200, { "Content-Type": "application/json" },'{"bookmarks":[{"name":"'+testBookmark.name+'","url":"'+testBookmark.url+'"}]}']);
             server.respond();
  
             return dfd;
         });
  
         tdd.test("Catalog.getTool - returns Deferred", function() {
             assert.isTrue(catalog.getCatalog().getTool('myTool') instanceof Deferred, "Catalog.getTool should return a Deferred");
         });
  
         tdd.test("Catalog.getTool - returns a Bookmark", function() {
             var dfd = this.async(1000);
  
             catalog.getCatalog().getTool('myTool').then(dfd.callback(function(tool) {
                   assert.isTrue(tool instanceof Catalog.Bookmark, "Catalog.getTool should resolve with a Catalog.Bookmark");
                   assert.equal(tool.id, testBookmark.id, "Tool was not constructed with correct value for 'id'");
                   assert.equal(tool.type, testBookmark.type, "Tool was not constructed with correct value for 'type'");
                   assert.equal(tool.name, testBookmark.name, "Tool was not constructed with correct value for 'name'");
                   assert.equal(tool.url, testBookmark.url, "Tool was not constructed with correct value for 'url'");
                   assert.equal(tool.description, testBookmark.description, "Tool was not constructed with correct value for 'description'");
                   assert.equal(tool.icon, testBookmark.icon, "Tool was not constructed with correct value for 'icon'");
             }), function(err) {
                 dfd.reject(err);
             });
  
             server.respondWith("GET", "/ibm/api/adminCenter/v1/catalog",
                     [200, { "Content-Type": "application/json" },'{"_metadata":{},"bookmarks":['+JSON.stringify(testBookmark)+'],"featureTools":['+JSON.stringify(testFeatureTool)+']}']);
             server.respond();
  
             return dfd;
         });
  
         tdd.test("Catalog.getTool - returns a Feature Tool", function() {
             var dfd = this.async(1000);
  
             catalog.getCatalog().getTool('testFeature-1.0').then(dfd.callback(function(tool) {
                     assert.isTrue(tool instanceof Catalog.FeatureTool, "Catalog.getTool should resolve with a Catalog.FeatureTool");
                     assert.equal(tool.id, testFeatureTool.id, "Tool was not constructed with correct value for 'id'");
                     assert.equal(tool.type, testFeatureTool.type, "Tool was not constructed with correct value for 'type'");
                     assert.equal(tool.featureName, testFeatureTool.featureName, "Tool was not constructed with correct value for 'featureName'");
                     assert.equal(tool.featureVersion, testFeatureTool.featureVersion, "Tool was not constructed with correct value for 'featureVersion'");
                     assert.equal(tool.name, testFeatureTool.name, "Tool was not constructed with correct value for 'name'");
                     assert.equal(tool.url, testFeatureTool.url, "Tool was not constructed with correct value for 'url'");
                     assert.equal(tool.description, testFeatureTool.description, "Tool was not constructed with correct value for 'description'");
                     assert.equal(tool.icon, testFeatureTool.icon, "Tool was not constructed with correct value for 'icon'");
             }), function(err) {
                 dfd.reject(err);
             });
  
             server.respondWith("GET", "/ibm/api/adminCenter/v1/catalog",
                     [200, { "Content-Type": "application/json" },'{"_metadata":{},"bookmarks":['+JSON.stringify(testBookmark)+'],"featureTools":['+JSON.stringify(testFeatureTool)+']}']);
             server.respond();
  
             return dfd;
         });
  
         tdd.test("Catalog.getTool - no provided Tool ID", function() {
             try {
                 catalog.getCatalog().getTool();
                 assert.isTrue(false, "Catalog.getTool should throw an error when no tool ID is provided");
             } catch(err) {
                 // Pass
             }
         });
  
         tdd.test("Catalog.addBookmark - returns Deferred", function() {
             assert.isTrue(catalog.getCatalog().addBookmark(testBookmark) instanceof Deferred, "Catalog.addBookmark should return a Deferred");
         });
  
         tdd.test("Catalog.addBookmark - returns the created Tool", function() {
             var dfd = this.async(1000);
  
             catalog.getCatalog().addBookmark(testBookmark).then(dfd.callback(function(tool) {
                     assert.isTrue(tool instanceof Catalog.Bookmark, "Catalog.deleteBookmark should resolve with a Catalog.Bookmark");
                     assert.equal(tool.id, testBookmark.id, "Returned tool did not have correct value for 'id'");
                     assert.equal(tool.name, testBookmark.name, "Returned tool did not have correct value for 'name'");
                     assert.equal(tool.version, testBookmark.version, "Returned tool did not have correct value for 'version'");
                     assert.equal(tool.url, testBookmark.url, "Returned tool did not have correct value for 'url'");
                     assert.equal(tool.description, testBookmark.description, "Returned tool did not have correct value for 'description'");
                     assert.equal(tool.icon, testBookmark.icon, "Returned tool did not have correct value for 'icon'");
             }), function(err) {
                 dfd.reject(err);
             });
  
             server.respondWith("POST", "/ibm/api/adminCenter/v1/catalog/bookmarks",
                     [201, { "Content-Type": "application/json" }, JSON.stringify(testBookmark)]);
             server.respond();
  
             return dfd;
         });
  
         tdd.test("Catalog.addBookmark - no provided Tool props", function() {
             try {
                 catalog.getCatalog().addBookmark();
                 assert.isTrue(false, "Catalog.addBookmark should throw an error when no tool ID is provided");
             } catch(err) {
                 // Pass
             }
         });
  
         tdd.test("Catalog.deleteBookmark - returns Deferred", function() {
             assert.isTrue(catalog.getCatalog().deleteBookmark('myTool') instanceof Deferred, "Catalog.deleteBookmark should return a Deferred");
         });
  
         tdd.test("Catalog.deleteBookmark - returns the deleted Tool", function() {
             var dfd = this.async(1000);
  
             catalog.getCatalog().deleteBookmark(testBookmark.id).then(dfd.callback(function(tool) {
                     assert.isTrue(tool instanceof Catalog.Bookmark, "Catalog.deleteBookmark should resolve with a Catalog.Bookmark");
                     assert.equal(tool.id, testBookmark.id, "Returned tool did not have correct value for 'id'");
                     assert.equal(tool.type, testBookmark.type, "Returned tool did not have correct value for 'type'");
                     assert.equal(tool.name, testBookmark.name, "Returned tool did not have correct value for 'name'");
                     assert.equal(tool.url, testBookmark.url, "Returned tool did not have correct value for 'url'");
                     assert.equal(tool.description, testBookmark.description, "Returned tool did not have correct value for 'description'");
                     assert.equal(tool.icon, testBookmark.icon, "Returned tool did not have correct value for 'icon'");
             }), function(err) {
                 dfd.reject(err);
             });
  
             server.respondWith("DELETE", "/ibm/api/adminCenter/v1/catalog/bookmarks/myTool",
                     [200, { "Content-Type": "application/json" }, JSON.stringify(testBookmark)]);
             server.respond();
  
             return dfd;
         });
  
         tdd.test("Catalog.deleteBookmark - no provided Tool ID", function() {
             try {
                 catalog.getCatalog().deleteBookmark();
                 assert.isTrue(false, "Catalog.deleteBookmark should throw an error when no tool ID is provided");
             } catch(err) {
                 // Pass
             }
         });         
        });        
      }
});
