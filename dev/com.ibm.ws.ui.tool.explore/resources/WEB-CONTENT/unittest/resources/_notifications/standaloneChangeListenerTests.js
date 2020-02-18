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
 * See unifiedChangeListener for general eventing details.
 * 
 * StandaloneServer Event {
 *   type: 'standaloneServer',
 *   cluster, scalingPolicy, (optional)
 *   apps: {
 *     up, down, unknown,
 *     added: [ { name, state } ],   (optional)
 *     removed: [ name ],   (optional)
 *     changed: [ { name, state } ]   (optional)
 *   } (optional)
 * }
 */
define([ 
        "intern!tdd",
        "intern/chai!assert",
        "dojo/_base/lang",
        "dojo/topic",
        "resources/_topicUtil",
        "resources/_notifications/standaloneChangeListener"
        ],
    function(tdd, assert, lang, topic, topicUtil, listener) {
  
    // Re-assign the variable to get Eclipse to stop the warnings
    var topicListeners = [];
  
    /**
     * Simple helper method to ensure that all topic listeners are captured. The captured listeners
     * are automatically removed at the end of the test.
     * 
     * @param {string} t The topic string to subscribe to
     * @param {function} fn The callback function to invoke
     */
    function addTopicListener(t, fn) {
      topicListeners.push( topic.subscribe(t, fn) );
    }
  
    /**
     * Clones the mock response for use by the test. We clone the Object so that the production code can alter it
     * and we will safely discard the clone at the end of each test execution.
     * 
     * @param {!Object} mockResponse The mock REST API JSON response to clone and prepare
     */
    function clone(mockResponse) {
      return lang.clone(mockResponse);
    }
  
    /**
     * Validate that the event is null. This is a convenience method to make code nice and readable.
     * 
     * @param {string} eventTopic The topic the event was received from.
     * @param {Object} event The event object which may or may not have been received.
     */
    function validateEventWasNotReceived(eventTopic, event) {
      assert.isNull(event, 'An event was received for topic ' + eventTopic + ' but it should have been null');
    }
  
    /**
     * Common logic to compare two arrays which came from an event.
     * 
     * @param {string} eventString The identifier of the event.
     * @param {string} attributeName The identifier of the attribute being compared.
     * @param {Array} expected The array with the expected values. May be null. Must not be empty.
     * @param {Array} actual The array with the actual values. May be null or undefined. Must not be empty.
     */
    function compareArray(attributeName, expected, actual) {
      // If we are not expecting an array, ensure that the actual event did not have one
      if (!expected) {
        // The array is not expected, confirm that the array is not present (or at least falsey). The !! ensures the result is a boolean.
        assert.notOk(!!actual, 'The standaloneServer event had the "' + attributeName + '" attribute when it should not have been set');
      } else {
        // Basic type checking of the array
        assert.isNotNull(actual,                'The standaloneServer event did not have the "' + attributeName + '" attribute');
        assert.isTrue(Array.isArray(actual), 'The standaloneServer event attribute "' + attributeName + '" was not an array');
  
        // Basic validity checking - empty arrays should not be present
        assert.isTrue(expected.length > 0, 'The standaloneServer event attribute "' + attributeName + '" expected values was zero, this array should not have been set');
        assert.isTrue(actual.length > 0,   'The standaloneServer event attribute "' + attributeName + '" actual values was zero, this array should not have been set');
  
        assert.equal(actual.length, expected.length, 'The standaloneServer event attribute "' + attributeName + '" had the wrong length');
        // Iterate through and compare elements
        for (var a = 0; a < actual.length; a++) {
          assert.equal(JSON.stringify(actual[a]), JSON.stringify(expected[a]), 'The standaloneServer event attribute "' + attributeName + '" did not match the expected array entry at ' + a);
        }
      }
    }
  
    /**
     * Validates the values in a "StandaloneServer Event" are in fact what is expected.
     * 
     * A Server Event will always contain at least one changed attribute. It may also contain the apps.added, apps.changed and apps.removed arrays. The arrays must not be empty if set.
     * If the event contains no changed attributes or all arrays are empty, the event is invalid and should not have been sent.
     * 
     * @param {string} id The tuple of the server which received the event.
     * @param {Object} actual The received Server Event object.
     * @param {string} expected The expected Server Event object.
     */
    function validateStandaloneServerEvent(actual, expected) {
      assert.isNotNull(actual,                 'The standaloneServer event was not received');
      assert.equal('standaloneServer', actual.type, 'The standaloneServer event did not have the correct type');
  
      // Validate that the event should have actually been sent. Check for some kind of meaningful information
      if (!!actual.cluster && !!actual.scalingPolicy &&
          (actual.apps     && (!actual.apps.hasOwnProperty('up') || !actual.apps.hasOwnProperty('down') || !actual.apps.hasOwnProperty('unknown'))) &&
          (actual.apps     && (!actual.apps.added   || actual.apps.added.length === 0)) &&
          (actual.apps     && (!actual.apps.changed || actual.apps.changed.length === 0)) &&
          (actual.apps     && (!actual.apps.removed || actual.apps.removed.length === 0))) {
        assert.isTrue(false, 'The standaloneServer event had no values to report on. This event should never have been sent');
      }
  
      if (!expected.hasOwnProperty('cluster')) {
        // The cluster is not expected, confirm that the state attribute is not present (or at least falsey). The !! ensures the result is a boolean.
        assert.notOk(!!actual.cluster, 'The standaloneServer event had the "cluster" attribute when it should not have been set');
      } else {
        assert.equal(actual.cluster, expected.cluster, 'The standaloneServer event had the wrong value for "cluster"');
      }
  
      if (!expected.hasOwnProperty('scalingPolicy')) {
        // The scalingPolicy is not expected, confirm that the state attribute is not present (or at least falsey). The !! ensures the result is a boolean.
        assert.notOk(!!actual.scalingPolicy, 'The standaloneServer event had the "scalingPolicy" attribute when it should not have been set');
      } else {
        assert.equal(actual.scalingPolicy, expected.scalingPolicy, 'The standaloneServer event had the wrong value for "scalingPolicy"');
      }
  
      // Check standaloneServer event arrays
      if (expected.apps) {
        // Check to ensure the expectations have the tallies set
        assert.isTrue(expected.apps.hasOwnProperty('up'),      'The expected standaloneServer event is missing the apps.up tally');
        assert.isTrue(expected.apps.hasOwnProperty('down'),    'The expected standaloneServer event is missing the apps.down tally');
        assert.isTrue(expected.apps.hasOwnProperty('unknown'), 'The expected standaloneServer event is missing the apps.unknown tally');
        // Be paranoid, ensure all of the expected arrays have elements which are name/state objects.
        // We don't do this type checking on other arrays because all other arrays are supposed to contain Strings, and this is the one deviant case.
        if (expected.apps.added) {
          for (var i = 0; i < expected.apps.added.length; i++) {
            var curApp = expected.apps.added[i];
            if (!(curApp.hasOwnProperty('name') && curApp.hasOwnProperty('state'))) {
              assert.isTrue(false, 'The expected standaloneServer event has an element in apps.added which is not an Object with a name and a state');
            }
          }
        }
        if (expected.apps.changed) {
          for (var i = 0; i < expected.apps.changed.length; i++) {
            var curApp = expected.apps.changed[i];
            if (!(curApp.hasOwnProperty('name') && curApp.hasOwnProperty('state'))) {
              assert.isTrue(false, 'The expected standaloneServer event has an element in apps.added which is not an Object with a name and a state');
            }
          }
        }
  
        // Check the actual event has the correct values
        assert.isNotNull(actual.apps, 'The standaloneServer event expected an apps object but none was set');
        assert.equal(actual.apps.up,      expected.apps.up,      'The standaloneServer event had the wrong count for "apps.up"');
        assert.equal(actual.apps.down,    expected.apps.down,    'The standaloneServer event had the wrong count for "apps.down"');
        assert.equal(actual.apps.unknown, expected.apps.unknown, 'The standaloneServer event had the wrong count for "apps.unknown"');
        compareArray('apps.added',   expected.apps.added,   actual.apps.added);
        compareArray('apps.changed', expected.apps.changed, actual.apps.changed);
        compareArray('apps.removed', expected.apps.removed, actual.apps.removed);
      }
    }

    /**
     * Sets the cached hosts collections based on the previous computed baseline.
     */
    function setCached(obj) {
      listener.__resourceManager.__standaloneServer.instance = obj;
    }
    
    // Define the constructed standalone JSON payload with no applications
    var standaloneWithNoApps = { 
        host: 'localhost',
        userdir: '/wlp/usr',
        name: 'server1',
        apps: { up: 0, down: 0, unknown: 0, list: [] },
    };
  
    var standaloneClusteredWithNoApps = { 
        host: 'localhost',
        userdir: '/wlp/usr',
        name: 'server1',
        cluster: 'cluster1',
        apps: { up: 0, down: 0, unknown: 0, list: [] },
    };
  
    var standaloneScalingPolicyWithNoApps = { 
        host: 'localhost',
        userdir: '/wlp/usr',
        name: 'server1',
        cluster: 'cluster1',
        scalingPolicy: 'default',
        apps: { up: 0, down: 0, unknown: 0, list: [] },
    };
  
    var standaloneWithOneApp = { 
        host: 'localhost',
        userdir: '/wlp/usr',
        name: 'server1',
        apps: { up: 1, down: 0, unknown: 0, list: [{name: 'snoop', state: 'STARTED'}] },
    };
  
    var standaloneWithOneStoppedApp = { 
        host: 'localhost',
        userdir: '/wlp/usr',
        name: 'server1',
        apps: { up: 0, down: 1, unknown: 0, list: [{name: 'snoop', state: 'STOPPED'}] },
    };

    with(assert) {

      /**
       * Defines the 'Standalone Change Listener Tests' module test suite.
       */
      tdd.suite('Standalone Change Listener Tests', function() {
    
           tdd.beforeEach(function() {
               topicListeners = [];
           });
           
           tdd.afterEach(function() {
             for (var i = 0; i < topicListeners.length; i++) {
               topicListeners[i].remove();
             }  
           });

           /**
            * Comparing the exact same JSON should produce no standalone event.
            */
           tdd.test('1. No differences detected', function() {
             var standaloneEvent = null;
             addTopicListener(topicUtil.getTopicByType('standaloneServer'),  function(e) {
               standaloneEvent = e;
             });
    
             setCached(clone(standaloneWithNoApps));
             var now = clone(standaloneWithNoApps);
             listener.__sendStandaloneNotifications(now);
    
             validateEventWasNotReceived('standalone', standaloneEvent);
    
             setCached(clone(standaloneWithOneApp));
             now = clone(standaloneWithOneApp);
             listener.__sendStandaloneNotifications(now);
    
             validateEventWasNotReceived('standalone', standaloneEvent);
           }),
    
           tdd.test('2a. cluster set', function() {
             var standaloneEvent = null;
             addTopicListener(topicUtil.getTopicByType('standaloneServer'),  function(e) {
               standaloneEvent = e;
             });
    
             setCached(clone(standaloneWithNoApps));
             var now = clone(standaloneClusteredWithNoApps);
             listener.__sendStandaloneNotifications(now);
    
             validateStandaloneServerEvent(standaloneEvent, { cluster: 'cluster1' });
           }),
    
           tdd.test('2b. cluster changed', function() {
             var standaloneEvent = null;
             addTopicListener(topicUtil.getTopicByType('standaloneServer'),  function(e) {
               standaloneEvent = e;
             });
    
             setCached(clone(standaloneClusteredWithNoApps));
             var now = clone(standaloneClusteredWithNoApps);
             now.cluster = 'cluster2'; // Just override the cluster for the 'now' payload
             listener.__sendStandaloneNotifications(now);
    
             validateStandaloneServerEvent(standaloneEvent, { cluster: 'cluster2' });
           }),
    
           tdd.test('2c. cluster removed', function() {
             var standaloneEvent = null;
             addTopicListener(topicUtil.getTopicByType('standaloneServer'),  function(e) {
               standaloneEvent = e;
             });
    
             setCached(clone(standaloneClusteredWithNoApps));
             var now = clone(standaloneWithNoApps);
             listener.__sendStandaloneNotifications(now);
    
             validateStandaloneServerEvent(standaloneEvent, { cluster: null });
           }),
    
           /**
            * Comparing the exact same JSON should produce no standalone event.
            */
           tdd.test('3a. scalingPolicy set', function() {
             var standaloneEvent = null;
             addTopicListener(topicUtil.getTopicByType('standaloneServer'),  function(e) {
               standaloneEvent = e;
             });
    
             setCached(clone(standaloneClusteredWithNoApps));
             var now = clone(standaloneScalingPolicyWithNoApps);
             listener.__sendStandaloneNotifications(now);
    
             validateStandaloneServerEvent(standaloneEvent, { scalingPolicy: 'default' });
           }),
    
           /**
            * Comparing the exact same JSON should produce no standalone event.
            */
           tdd.test('3b. scalingPolicy changed', function() {
             var standaloneEvent = null;
             addTopicListener(topicUtil.getTopicByType('standaloneServer'),  function(e) {
               standaloneEvent = e;
             });
    
             setCached(clone(standaloneScalingPolicyWithNoApps));
             var now = clone(standaloneScalingPolicyWithNoApps);
             now.scalingPolicy = 'newPolicy'; // Just override the cluster for the 'now' payload
             listener.__sendStandaloneNotifications(now);
    
             validateStandaloneServerEvent(standaloneEvent, { scalingPolicy: 'newPolicy' });
           }),
    
           /**
            * Comparing the exact same JSON should produce no standalone event.
            */
           tdd.test('3c. scalingPolicy removed', function() {
             var standaloneEvent = null;
             addTopicListener(topicUtil.getTopicByType('standaloneServer'),  function(e) {
               standaloneEvent = e;
             });
    
             setCached(clone(standaloneScalingPolicyWithNoApps));
             var now = clone(standaloneClusteredWithNoApps);
             listener.__sendStandaloneNotifications(now);
    
             validateStandaloneServerEvent(standaloneEvent, { scalingPolicy: null });
           }),
    
           tdd.test('4a. cluster and scalingPolicy set', function() {
             var standaloneEvent = null;
             addTopicListener(topicUtil.getTopicByType('standaloneServer'),  function(e) {
               standaloneEvent = e;
             });
    
             setCached(clone(standaloneWithNoApps));
             var now = clone(standaloneScalingPolicyWithNoApps);
             listener.__sendStandaloneNotifications(now);
    
             validateStandaloneServerEvent(standaloneEvent, { cluster: 'cluster1', scalingPolicy: 'default' });
           }),
    
           tdd.test('4b. cluster and scalingPolicy changed', function() {
             var standaloneEvent = null;
             addTopicListener(topicUtil.getTopicByType('standaloneServer'),  function(e) {
               standaloneEvent = e;
             });
    
             setCached(clone(standaloneScalingPolicyWithNoApps));
             var now = clone(standaloneScalingPolicyWithNoApps);
             now.cluster = 'cluster2'; // Just override the cluster for the 'now' payload
             now.scalingPolicy = 'newPolicy'; // Just override the cluster for the 'now' payload
             listener.__sendStandaloneNotifications(now);
    
             validateStandaloneServerEvent(standaloneEvent, { cluster: 'cluster2', scalingPolicy: 'newPolicy' });
           }),
    
           tdd.test('4c. cluster and scalingPolicy removed', function() {
             var standaloneEvent = null;
             addTopicListener(topicUtil.getTopicByType('standaloneServer'),  function(e) {
               standaloneEvent = e;
             });
    
             setCached(clone(standaloneScalingPolicyWithNoApps));
             var now = clone(standaloneWithNoApps);
             listener.__sendStandaloneNotifications(now);
    
             validateStandaloneServerEvent(standaloneEvent, { cluster: null, scalingPolicy: null });
           }),
    
           tdd.test('5a. application added', function() {
             var standaloneEvent = null;
             addTopicListener(topicUtil.getTopicByType('standaloneServer'),  function(e) {
               standaloneEvent = e;
             });
    
             setCached(clone(standaloneWithNoApps));
             var now = clone(standaloneWithOneApp);
             listener.__sendStandaloneNotifications(now);
    
             validateStandaloneServerEvent(standaloneEvent, { apps: {up: 1, down: 0, unknown: 0, added: [{name: 'snoop', state: 'STARTED'}]} });
           }),
    
           tdd.test('5b. application changed', function() {
             var standaloneEvent = null;
             addTopicListener(topicUtil.getTopicByType('standaloneServer'),  function(e) {
               standaloneEvent = e;
             });
    
             setCached(clone(standaloneWithOneApp));
             var now = clone(standaloneWithOneStoppedApp);
             listener.__sendStandaloneNotifications(now);
    
             validateStandaloneServerEvent(standaloneEvent, { apps: {up: 0, down: 1, unknown: 0, changed: [{name: 'snoop', state: 'STOPPED'}]} });
           }),
    
           tdd.test('5c. application removed', function() {
             var standaloneEvent = null;
             addTopicListener(topicUtil.getTopicByType('standaloneServer'),  function(e) {
               standaloneEvent = e;
             });
    
             setCached(clone(standaloneWithOneApp));
             var now = clone(standaloneWithNoApps);
             listener.__sendStandaloneNotifications(now);
    
             validateStandaloneServerEvent(standaloneEvent, { apps: {up: 0, down: 0, unknown: 0, removed: ['snoop']} });
           });
      });
    }
});
