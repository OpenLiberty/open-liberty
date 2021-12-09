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
define(["intern!tdd","intern/chai!assert","dojo/Deferred","js/toolbox/toolbox"], function(tdd,assert,Deferred,toolbox) {

    var server, testBookmark, testFeatureTool;
    
    with(assert) {
      
      /**
       * Defines the 'toolbox' module test suite.
       */
      tdd.suite("Toolbox Tests", function() {
        
            tdd.before(function() {
                testBookmark = {
                  id: "myTool",
                  type:"bookmark",
                  name:"myTool",
                  url:"ibm.com",
                  icon:"default.png"
                };
              
                testToolEntry = {
                  id: "myFeature-1.0",
                  type:"featureTool",
                };
            });
            
            tdd.beforeEach(function() {
              // Mock the admin center server since it is not available in a unittest
              server = sinon.fakeServer.create();
           });
            
           tdd.afterEach(function() {
              server.restore();
           });
  
           tdd.test("ToolEntry - create from ToolEntry", function() {
               var tool = new Toolbox.ToolEntry(testToolEntry);
               console.log("DEBUG", tool);
               assert.equal(tool.id, testToolEntry.id, "ToolEntry was not constructed with correct value for 'id'");
               assert.equal(tool.type, testToolEntry.type, "ToolEntry was not constructed with correct value for 'type'");
               assert.isUndefined(tool.name, "ToolEntry should not have a name");
           });
           
           tdd.test("ToolEntry - create from Bookmark", function() {
               var tool = new Toolbox.ToolEntry(testBookmark);
               console.log("DEBUG", tool);
               assert.equal(tool.id, testBookmark.id, "ToolEntry was not constructed with correct value for 'id'");
               assert.equal(tool.type, testBookmark.type, "ToolEntry was not constructed with correct value for 'type'");
               assert.isUndefined(tool.name, "ToolEntry should not have a name even though it was created from an Object that had a name");
           });

           tdd.test("Bookmark - create", function() {
               var tool = new Toolbox.Bookmark(testBookmark);
               assert.isUndefined(tool.id, "Tool was constructed with an 'id'");
               assert.isUndefined(tool.type, "Tool was constructed with an 'type'");
               assert.equal(tool.name, testBookmark.name, "Tool was not constructed with correct value for 'name'");
               assert.equal(tool.url, testBookmark.url, "Tool was not constructed with correct value for 'url'");
               assert.equal(tool.icon, testBookmark.icon, "Tool was not constructed with correct value for 'icon'");
           });

           tdd.test("Toolbox - construct", function() {
               var tb = new Toolbox();
               assert.isTrue(tb instanceof Toolbox, "Unable to construct Toolbox");
           });

           tdd.test("Toolbox - get instance", function() {
               var tb = toolbox.getToolbox();
               assert.isTrue(tb instanceof Toolbox, "Unable to get instance of Toolbox");
           });

           tdd.test("Toolbox - get instance is same instance", function() {
               var tbNew = new Toolbox();
               var tbInst = toolbox.getToolbox();
               assert.isFalse(tbInst ===  tbNew, "The 'singleton' instance of Toolbox should not match a 'new' instance of Toolbox");
               assert.isTrue(tbInst ===  toolbox.getToolbox(), "The 'singleton' instance of Toolbox should match the previous return for the singleton");
           });

           tdd.test("toolbox.getToolEntries - returns Deferred", function() {
               assert.isTrue(toolbox.getToolbox().getToolEntries() instanceof Deferred, "Toolbox.getToolEntries should return a Deferred");
           });

           tdd.test("toolbox.getToolEntries - resolves with Array (no Tools)", function() {
               var dfd = this.async(1000);

               toolbox.getToolbox().getToolEntries().then(dfd.callback(function(tools) {
                       assert.isTrue(tools instanceof Array, "Toolbox.getToolEntries should resolve with an Array");
               }), function(err) {
                   dfd.reject(err);
               });

               server.respondWith("GET", "/ibm/api/adminCenter/v1/toolbox/toolEntries",
                       [200, { "Content-Type": "application/json" },'{"toolEntries":[]}']);
               server.respond();

               return dfd;
           });

           tdd.test("toolbox.getToolEntries - resolves with Array of Tool objects", function() {
               var dfd = this.async(1000);

               toolbox.getToolbox().getToolEntries().then(dfd.callback(function(tools) {
                       assert.isTrue(tools instanceof Array, "Toolbox.getToolEntries should resolve with an Array");
                       assert.equal(tools.length, 1, "Expected exactly 1 tool back from the mock response");

                       var tool = tools[0];
                       assert.equal(tool.id, testToolEntry.id, "Tool was not constructed with correct value for 'id'");
                       assert.equal(tool.type, testToolEntry.type, "Tool was not constructed with correct value for 'type'");
               }), function(err) {
                   dfd.reject(err);
               });

               server.respondWith("GET", "/ibm/api/adminCenter/v1/toolbox/toolEntries",
                       [200, { "Content-Type": "application/json" },'['+JSON.stringify(testToolEntry)+']']);
               server.respond();

               return dfd;
           });

           tdd.test("toolbox.getTool - returns Deferred", function() {
               assert.isTrue(toolbox.getToolbox().getTool('myTool') instanceof Deferred, "Toolbox.getTool should return a Deferred");
           });

           tdd.test("toolbox.getTool - returns a Tool", function() {
               var dfd = this.async(1000);

               toolbox.getToolbox().getTool('myTool').then(dfd.callback(function(tool) {
                       assert.equal(tool.id, testBookmark.id, "Returned tool did not have correct value for 'id'");
                       assert.equal(tool.type, testBookmark.type, "Returned tool did not have correct value for 'type'");
                       assert.equal(tool.name, testBookmark.name, "Returned tool did not have correct value for 'name'");
                       assert.equal(tool.url, testBookmark.url, "Returned tool did not have correct value for 'url'");
                       assert.equal(tool.icon, testBookmark.icon, "Returned tool did not have correct value for 'icon'");
               }), function(err) {
                   dfd.reject(err);
               });

               server.respondWith("GET", "/ibm/api/adminCenter/v1/toolbox/toolEntries/myTool",
                       [200, { "Content-Type": "application/json" }, JSON.stringify(testBookmark)]);
               server.respond();

               return dfd;
           });

           tdd.test("Toolbox.getTool - filtered Tool", function() {
               var dfd = this.async(1000);

               toolbox.getToolbox().getTool('myTool', 'name,url').then(dfd.callback(function(tool) {
                       assert.equal(tool.id, null, "Returned tool was filtered and should not have an 'id'");
                       assert.equal(tool.type, null, "Returned tool was filtered and should not have a 'type'");
                       assert.equal(tool.name, testBookmark.name, "Returned tool did not have correct value for 'name'");
                       assert.equal(tool.url, testBookmark.url, "Returned tool did not have correct value for 'url'");
                       assert.equal(tool.icon, null, "Returned tool was filtered and should not have an 'icon'");
               }), function(err) {
                   dfd.reject(err);
               });

               server.respondWith("GET", "/ibm/api/adminCenter/v1/toolbox/toolEntries/myTool?fields=name,url",
                       [200, { "Content-Type": "application/json" }, '{"name":"'+testBookmark.name+'","url":"'+testBookmark.url+'"}']);
               server.respond();

               return dfd;
           });

           tdd.test("Toolbox.getTool - no provided Tool ID", function() {
               try {
                   toolbox.getToolbox().getTool();
                   assert.isTrue(false, "Toolbox.getTool should throw an error when no tool ID is provided");
               } catch(err) {
                   // Pass
               }
           });

           tdd.test("Toolbox.addToolEntry - returns Deferred", function() {
               assert.isTrue(toolbox.getToolbox().addToolEntry(testBookmark) instanceof Deferred, "Toolbox.addToolEntry should return a Deferred");
           });

           tdd.test("Toolbox.addToolEntry - returns the created ToolEntry", function() {
               var dfd = this.async(1000);

               toolbox.getToolbox().addToolEntry(testBookmark).then(dfd.callback(function(tool) {
                       assert.equal(tool.id, testBookmark.id, "Returned tool did not have correct value for 'id'");
                       assert.equal(tool.type, testBookmark.type, "Returned tool did not have correct value for 'type'");
                       assert.equal(tool.name, testBookmark.name, "Returned tool did not have correct value for 'name'");
                       assert.equal(tool.url, testBookmark.url, "Returned tool did not have correct value for 'url'");
                       assert.equal(tool.icon, testBookmark.icon, "Returned tool did not have correct value for 'icon'");
               }), function(err) {
                   dfd.reject(err);
               });

               server.respondWith("POST", "/ibm/api/adminCenter/v1/toolbox/toolEntries",
                       [201, { "Content-Type": "application/json" }, JSON.stringify(testBookmark)]);
               server.respond();

               return dfd;
           });

           tdd.test("Toolbox.addToolEntry - no provided ToolEntry props", function() {
               try {
                   toolbox.getToolbox().addToolEntry();
                   assert.isTrue(false, "Toolbox.addToolEntry should throw an error when no tool ID is provided");
               } catch(err) {
                   // Pass
               }
           });

           tdd.test("Toolbox.addBookmark - returns Deferred", function() {
               assert.isTrue(toolbox.getToolbox().addBookmark(testBookmark) instanceof Deferred, "Toolbox.addBookmark should return a Deferred");
           });

           tdd.test("Toolbox.addBookmark - returns the created Bookmark", function() {
               var dfd = this.async(1000);

               toolbox.getToolbox().addBookmark(testBookmark).then(dfd.callback(function(tool) {
                       assert.equal(tool.id, testBookmark.id, "Returned tool did not have correct value for 'id'");
                       assert.equal(tool.type, testBookmark.type, "Returned tool did not have correct value for 'type'");
                       assert.equal(tool.name, testBookmark.name, "Returned tool did not have correct value for 'name'");
                       assert.equal(tool.url, testBookmark.url, "Returned tool did not have correct value for 'url'");
                       assert.equal(tool.icon, testBookmark.icon, "Returned tool did not have correct value for 'icon'");
               }), function(err) {
                   dfd.reject(err);
               });

               server.respondWith("POST", "/ibm/api/adminCenter/v1/toolbox/bookmarks",
                       [201, { "Content-Type": "application/json" }, JSON.stringify(testBookmark)]);
               server.respond();

               return dfd;
           });

           tdd.test("Toolbox.addBookmark - no provided Bookmark props", function() {
               try {
                   toolbox.getToolbox().addBookmark();
                   assert.isTrue(false, "Toolbox.addBookmark should throw an error when no tool ID is provided");
               } catch(err) {
                   // Pass
               }
           });

           tdd.test("Toolbox.deleteTool - returns Deferred", function() {
               assert.isTrue(toolbox.getToolbox().deleteTool('myTool') instanceof Deferred, "Toolbox.deleteTool should return a Deferred");
           });

           tdd.test("Toolbox.deleteTool - returns the deleted entry's JSON", function() {
               var dfd = this.async(1000);

               toolbox.getToolbox().deleteTool(testBookmark.id).then(dfd.callback(function(tool) {
                       assert.equal(tool.id, testBookmark.id, "Returned tool did not have correct value for 'id'");
                       assert.equal(tool.type, testBookmark.type, "Returned tool did not have correct value for 'type'");
                       assert.equal(tool.name, testBookmark.name, "Returned tool did not have correct value for 'name'");
                       assert.equal(tool.url, testBookmark.url, "Returned tool did not have correct value for 'url'");
                       assert.equal(tool.icon, testBookmark.icon, "Returned tool did not have correct value for 'icon'");
               }), function(err) {
                   dfd.reject(err);
               });

               server.respondWith("DELETE", "/ibm/api/adminCenter/v1/toolbox/toolEntries/myTool",
                       [200, { "Content-Type": "application/json" }, JSON.stringify(testBookmark)]);
               server.respond();

               return dfd;
           });

           tdd.test("Toolbox.deleteTool - no provided Tool ID", function() {
               try {
                   toolbox.getToolbox().deleteTool();
                   assert.isTrue(false, "Toolbox.deleteTool should throw an error when no tool ID is provided");
               } catch(err) {
                   // Pass
               }
           });
      });
    }
});
