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
define([
     "intern!tdd",
     "intern/chai!assert",
     "resources/hashUtils"
       ],
       
   function (tdd, assert, hashUtils) {
  
      var toolId = 'explore';
      var failureMsg = 'FAIL: Did not get back the expected hash string for the given resource';
     
      with(assert) {
   
        /**
         * Defines the 'URL Utils Tests' module test suite.
         */
        tdd.suite("Hash Utils Tests", function() {
   
            tdd.afterEach(function() {
              // Calling these functions results in the actual browser's window.top.location.hash
              // being set to whatever is passed to hashUtils.__setCurrentHash(). So this results
              // in a URL like 
              // https://localhost:9443/devExplore/testRunner.html?config=unittest/internBrowser#explore/servers/myHost,/wlp/usr,myServer
              // We don't want that. Calling the below results in URL like
              // https://localhost:9443/devExplore/testRunner.html?config=unittest/internBrowser#
              // No idea how to get rid of that trailing hash
              hashUtils.__setCurrentHash("");
            });
            
            // In this test, without calling hashUtils.getToolId(), getCurrentHash() returns 
            // undefined, but that function is never called in the DOH-based hashUtilsTests.
            // Should figure out why
            tdd.test("getToolId test", function() {
              assert.equal(hashUtils.getToolId(), toolId, failureMsg);
            });
            
            tdd.test("getCurrentHash: Explore toolId default", function() {
              assert.equal(hashUtils.getCurrentHash(), toolId, failureMsg);
            });
            
            tdd.test("getCurrentHash test: Server", function() {
              hashUtils.__setCurrentHash(toolId + '/servers/myHost,/wlp/usr,myServer');
              assert.equal(hashUtils.getCurrentHash(), toolId + '/servers/myHost,/wlp/usr,myServer', failureMsg);
            });
            
            tdd.test("getCurrentHash test: Servers", function() {
              hashUtils.__setCurrentHash(toolId + '/servers');
              assert.equal(hashUtils.getCurrentHash(), toolId + '/servers', failureMsg);
            });
            
            tdd.test("getCurrentHash test: ServersOnHost", function() {
              hashUtils.__setCurrentHash(toolId + '/hosts/myHost/servers');
              assert.equal(hashUtils.getCurrentHash(), toolId + '/hosts/myHost/servers', failureMsg);
            });
            
            tdd.test("getCurrentHash test: ServersOnCluster", function() {
              hashUtils.__setCurrentHash(toolId + '/clusters/myCluster/servers');
              assert.equal(hashUtils.getCurrentHash(), toolId + '/clusters/myCluster/servers', failureMsg);
            });
            
            tdd.test("getCurrentHash test: Clusters", function() {
              hashUtils.__setCurrentHash(toolId + '/clusters');
              assert.equal(hashUtils.getCurrentHash(), toolId + '/clusters', failureMsg);
            });

            tdd.test("getCurrentHash test: Cluster", function() {
              hashUtils.__setCurrentHash(toolId + '/clusters/myCluster');
              assert.equal(hashUtils.getCurrentHash(), toolId + '/clusters/myCluster', failureMsg);
            });
            
            tdd.test("getCurrentHash test: Hosts", function() {
              hashUtils.__setCurrentHash(toolId + '/hosts');
              assert.equal(hashUtils.getCurrentHash(), toolId + '/hosts', failureMsg);
            });
            
            tdd.test("getCurrentHash test: Host", function() {
              hashUtils.__setCurrentHash(toolId + '/hosts/myHost');
              assert.equal(hashUtils.getCurrentHash(), toolId + '/hosts/myHost', failureMsg);
            });
            
            tdd.test("getCurrentHash test: Applications", function() {
              hashUtils.__setCurrentHash(toolId + '/applications');
              assert.equal(hashUtils.getCurrentHash(), toolId + '/applications', failureMsg);
            });
            
            tdd.test("getCurrentHash test: AppsOnServer", function() {
              hashUtils.__setCurrentHash(toolId + '/servers/myHost,/wlp/usr,myServer/apps');
              assert.equal(hashUtils.getCurrentHash(), toolId + '/servers/myHost,/wlp/usr,myServer/apps', failureMsg);
            });
            
            tdd.test("getCurrentHash test: AppOnServer", function() {
              hashUtils.__setCurrentHash(toolId + '/servers/myHost,/wlp/usr,myServer/apps/snoop');
              assert.equal(hashUtils.getCurrentHash(), toolId + '/servers/myHost,/wlp/usr,myServer/apps/snoop', failureMsg);
            });
            
            tdd.test("getCurrentHash test: AppsOnCluster", function() {
              hashUtils.__setCurrentHash(toolId + '/clusters/myCluster/apps');
              assert.equal(hashUtils.getCurrentHash(), toolId + '/clusters/myCluster/apps', failureMsg);
            });
            
            tdd.test("getCurrentHash test: AppOnCluster", function() {
              hashUtils.__setCurrentHash(toolId + '/clusters/myCluster/apps/snoop');
              assert.equal(hashUtils.getCurrentHash(), toolId + '/clusters/myCluster/apps/snoop', failureMsg);
            });
            
            tdd.test("getCurrentHash test: Unknown type", function() {
              hashUtils.__setCurrentHash("");
              assert.equal(hashUtils.getCurrentHash(), toolId, failureMsg);
            });
            
          });
      }
  });