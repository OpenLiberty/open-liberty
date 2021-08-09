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
 * Test cases for resourceManager
 */
define([
     "intern!tdd",
     "intern/chai!assert",
     "intern/order!../../resources/resourceManager.js"
       ],
       
   function (tdd, assert, resourceManager) {
     
      with(assert) {
   
          tdd.suite("resourceManager Test Suite", function() {
   
            tdd.before(function() {
                resourceManager.__reset();
              });
              
              tdd.test("resourceManager.getCached - sanity check all return null when not loaded", function() {
                var LOAD_ALL_NULL_MSG = 'resourceManager has not loaded anything, all cached requests should return null';
                
                assert.equal(resourceManager.getCached('applications'), null, LOAD_ALL_NULL_MSG);
                assert.equal(resourceManager.getCached('clusters'), null, LOAD_ALL_NULL_MSG);
                assert.equal(resourceManager.getCached('cluster', 'c1'), null, LOAD_ALL_NULL_MSG);
                assert.equal(resourceManager.getCached('servers'), null, LOAD_ALL_NULL_MSG);
                assert.equal(resourceManager.getCached('server', 's1'), null, LOAD_ALL_NULL_MSG);
                assert.equal(resourceManager.getCached('hosts'), null, LOAD_ALL_NULL_MSG);
                assert.equal(resourceManager.getCached('host', 'h1'), null, LOAD_ALL_NULL_MSG);
              });

              tdd.test("resourceManager.getCached - sanity check all when populated", function() {
                var LOAD_OK_MSG = 'When the resourceManager has fake loaded things, the calls should returned the cached value';
                var LOAD_NULL_MSG = 'resourceManager has not loaded this resource, so null should be returned';
                
                resourceManager.__applications.collection.instance = 'set';
                resourceManager.__applications.objects['a1'] = {instance: 'set'};
                resourceManager.__clusters.collection.instance = 'set';
                resourceManager.__clusters.objects['c1']= {instance: 'set'};
                resourceManager.__servers.collection.instance = 'set';
                resourceManager.__servers.objects['s1']= {instance: 'set'};
                resourceManager.__hosts.collection.instance = 'set';
                resourceManager.__hosts.objects['h1']= {instance: 'set'};
                
                assert.equal(resourceManager.getCached('applications'), 'set', LOAD_OK_MSG);
                assert.equal(resourceManager.getCached('application', 'a1'), 'set', LOAD_OK_MSG);
                assert.equal(resourceManager.getCached('application', 'a2'), null, LOAD_NULL_MSG);
                assert.equal(resourceManager.getCached('clusters'), 'set', LOAD_OK_MSG);
                assert.equal(resourceManager.getCached('cluster', 'c1'), 'set', LOAD_OK_MSG);
                assert.equal(resourceManager.getCached('cluster', 'c2'), null, LOAD_NULL_MSG);
                assert.equal(resourceManager.getCached('servers'), 'set', LOAD_OK_MSG);
                assert.equal(resourceManager.getCached('server', 's1'), 'set', LOAD_OK_MSG);
                assert.equal(resourceManager.getCached('server', 's2'), null, LOAD_NULL_MSG);
                assert.equal(resourceManager.getCached('hosts'), 'set', LOAD_OK_MSG);
                assert.equal(resourceManager.getCached('host', 'h1'), 'set', LOAD_OK_MSG);
                assert.equal(resourceManager.getCached('host', 'h2'), null, LOAD_NULL_MSG);
              });
              
          });
      }
  });