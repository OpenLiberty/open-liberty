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
 * General REST philosophy
 * 1) App Centric
 * 2) Minimal load, reference by name
 * 3) Consistency is good, but necessity is better
 * 4) Population by ResourceManager
 * 5) Replication layer is good for searches
 * 
 * General event philosophy:
 * 1) Only send an event when something is different (so processing is minimized)
 * 2) Only send an event with meaningful payload (so processing is minimized)
 * 3) Omit things which have not changed (so network payload is as small as possible)
 * 4) When a resource is removed, the event should only send a 'removed' state change. No other values should be specified.
 * 
 * A Collection Event is information about a collection and has the following properties:
 * - type: the event type, such as 'hosts' or 'servers'
 * - tallies: this is always 'up', 'down', 'unknown', and somtimes 'partial' and 'empty'
 * - added: Array of the name added resources
 * - removed: Array of the name removed resources
 * 
 * The Collection Event can be a top level event, or be a sub-collection for a resource:
 * 1. The 'top level' collections. These are Applications, Servers, Clusters, Hosts,and Runtimes.
 * 2. Sub-collection elements, such as the servers on a Host or the servers that make up a Cluster.
 * 
 * When resources are added to a collection, only the name is stored. The actual instance of the Object is not created until it is needed.
 * 
 * A Resource Event has the properties for the given resource, and generally includes at least one sub-collection.
 * 
 * The validateTypeEvent methods test for the supported event fields for all expected types.
 * 
 * As of 11/17/2014, these are the event formats and fields. Fields are required unless marked as optional.
 * 
 * Top-Level Collection Events
 * ---------------------------
 * 
 * Summary Event {
 *   type: 'summary',
 *   applications: { (optional)
 *     up, down, unknown, partial (required)
 *   },
 *   clusters: { (optional)
 *     up, down, unknown, partial (required)
 *   },
 *   servers: { (optional)
 *     up, down, unknown (required)
 *   },
 *   hosts: { (optional)
 *     up, down, unknown, partial, empty (required)
 *   }
 * }
 * 
 * Hosts Event {
 *   type: 'hosts',
 *   up, down, unknown, partial, empty,
 *   added: [ "name" ]   (optional),
 *   removed: [ "name" ]   (optional)
 * }
 * 
 * Runtimes Event {
 *   type: 'runtimes',
 *   up, down, unknown, partial, empty,
 *   added: [ {id: "host,path", name: "host,path", type: "runtime"} ]   (optional),
 *   removed: [ {id: "host,path", name: "host,path", type: "runtime"} ]   (optional)
 * }
 * 
 * Servers Event {
 *   type: 'servers',
 *   up, down, unknown,
 *   added: [ "tuple" ]   (optional),
 *   removed: [ "tuple" ]   (optional)
 * }
 * 
 * Clusters Event {
 *   type: 'clusters',
 *   up, down, unknown, partial,
 *   added: [ "name" ]   (optional),
 *   removed: [ "name" ]   (optional)
 * }
 * 
 * Applications Event {
 *   type: 'applications',
 *   up, down, unknown, partial,
 *   added: [ "name" ]   (optional),
 *   removed: [ "name" ]   (optional)
 * }
 * 
 * Resource Events
 * ---------------
 * 
 * Host Event {
 *   type: 'host',
 *   id: 'name',
 *   runtimes: {
 *     added: [ {id: "host,path", name: "host,path", type: "runtime"} ],   (optional)
 *     removed: [ {id: "host,path", name: "host,path", type: "runtime"} ]   (optional)
 *   }, (optional)
 *   servers: {
 *     up, down, unknown,
 *     added: [ "tuple" ],   (optional)
 *     removed: [ "tuple" ]   (optional)
 *   }, (optional)
 *   apps: {
 *     up, down, unknown, partial,
 *     added: [ "name" ],   (optional)
 *     removed: [ "name" ]   (optional)
 *   }, (optional)
 *   alerts (optional)
 * }
 * 
 * Runtime Event {
 *   type: 'runtime',
 *   id: 'host,path',
 *   servers: {
 *     up, down, unknown,
 *     added: [ {id: "host,path", name: "host,path", type: "runtime"} ],   (optional)
 *     removed: [ {id: "host,path", name: "host,path", type: "runtime"} ]   (optional)
 *   }, (optional)
 *   apps: {
 *     up, down, unknown, partial,
 *     added: [ "name" ],   (optional)
 *     removed: [ "name" ]   (optional)
 *   }, (optional)
 *   alerts (optional)
 * }
 * 
 * Server Event {
 *   type: 'server',
 *   id: 'tuple',
 *   state, wlpInstallDir, cluster, scalingPolicy, (optional)
 *   apps: {
 *     up, down, unknown,
 *     added: [ { name, state } ],   (optional)
 *     removed: [ name ],   (optional)
 *     changed: [ { name, state } ]   (optional)
 *   }, (optional)
 *   alerts (optional)
 * }
 * 
 * Cluster Event {
 *   type: 'cluster',
 *   id: 'name',
 *   state, scalingPolicy, (optional)
 *   servers: {
 *     up, down, unknown,
 *     added: [ "tuple" ],   (optional)
 *     removed: [ "tuple" ]   (optional)
 *   }, (optional)
 *   apps: {
 *     up, down, unknown, partial,
 *     added: [ "name" ],   (optional)
 *     removed: [ "name" ]   (optional)
 *   }, (optional)
 *   alerts (optional)
 * }
 * 
 * Application Event {
 *   type: 'application',
 *   id: 'name',
 *   state, up, down, unknown,
 *   scalingPolicy, (optional)
 *   servers: {
 *     up, down, unknown,
 *     added: [ "tuple" ],   (optional)
 *     removed: [ "tuple" ]   (optional)
 *   }, (optional)
 *   clusters: {
 *     up, down, unknown, partial,
 *     added: ["name" ],   (optional)
 *     removed: [ "name" ]   (optional)
 *   }, (optional)
 *   alerts (optional)
 * }
 */
define([ 
        "intern!tdd",
        "intern/chai!assert",
        "dojo/_base/lang",
        "dojo/topic",
        "resources/_topicUtil",
        "resources/_notifications/unifiedChangeListener"
        ],

        function(tdd, assert, lang, topic, topicUtil, unified) {

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
   * Prepares the mock response for use by the test. This currently clones the Object and drives
   * the production code which processes the initial REST API response baseline.
   * 
   * @param {!Object} mockResponse The mock REST API JSON response to clone and prepare
   */
  function cloneAndPrep(mockResponse) {
    var cloned = clone(mockResponse);
    unified.__processInitialUnifiedPayload(cloned);
    return cloned;
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
   * Validates the values in a "Summary Event" are in fact what is expected.
   * 
   * A Summary Event will always contain at least one set of tallies.
   * 
   * @param {Object} actual The received Summary Event object.
   * @param {number} expected The expected Summary Event object.
   */
  function validateSummaryEvent(actual, expected) {
    if (!expected.applications && !expected.clusters && !expected.servers && !expected.hosts) {
      assert.isTrue(false, 'The expected Summary event did not have at least one of the collections tallies');
    }

    if (expected.applications) {
      assert.isTrue(expected.applications.up >= 0,        'The expected Summary.applications event did not have a valid value for "up"');
      assert.isTrue(expected.applications.down >= 0,      'The expected Summary.applications event did not have a valid value for "down"');
      assert.isTrue(expected.applications.unknown >= 0,   'The expected Summary.applications event did not have a valid value for "unknown"');
      assert.isTrue(expected.applications.partial >= 0,   'The expected Summary.applications event did not have a valid value for "partial"');
    }
    if (expected.clusters) {
      assert.isTrue(expected.clusters.up >= 0,      'The expected Summary.clusters event did not have a valid value for "up"');
      assert.isTrue(expected.clusters.down >= 0,    'The expected Summary.clusters event did not have a valid value for "down"');
      assert.isTrue(expected.clusters.unknown >= 0, 'The expected Summary.clusters event did not have a valid value for "unknown"');
      assert.isTrue(expected.clusters.partial >= 0, 'The expected Summary.clusters event did not have a valid value for "partial"');
    }
    if (expected.servers) {
      assert.isTrue(expected.servers.up >= 0,       'The expected Summary.servers event did not have a valid value for "up"');
      assert.isTrue(expected.servers.down >= 0,     'The expected Summary.servers event did not have a valid value for "down"');
      assert.isTrue(expected.servers.unknown >= 0,  'The expected Summary.servers event did not have a valid value for "unknown"');
    }
    if (expected.hosts) {
      assert.isTrue(expected.hosts.up >= 0,         'The expected Summary.hosts event did not have a valid value for "up"');
      assert.isTrue(expected.hosts.down >= 0,       'The expected Summary.hosts event did not have a valid value for "down"');
      assert.isTrue(expected.hosts.unknown >= 0,    'The expected Summary.hosts event did not have a valid value for "unknown"');
      assert.isTrue(expected.hosts.partial >= 0,    'The expected Summary.hosts event did not have a valid value for "partial"');
      assert.isTrue(expected.hosts.empty >= 0,      'The expected Summary.hosts event did not have a valid value for "empty"');
    }

    // Validate the actual observed event
    assert.isNotNull(actual,                            'The Summary event was not received');
    assert.equal(actual.type,    'summary',        'The Summary event did not have the correct type');
    if (expected.applications) {
      assert.isNotNull(actual.applications,            'The Summary event did not have an applications tally');
      assert.equal(actual.applications.up,      expected.applications.up,      'The Summary.applications event had the wrong count for "up"');
      assert.equal(actual.applications.down,    expected.applications.down,    'The Summary.applications event had the wrong count for "down"');
      assert.equal(actual.applications.unknown, expected.applications.unknown, 'The Summary.applications event had the wrong count for "unknown"');
      assert.equal(actual.applications.partial, expected.applications.partial, 'The Summary.applications event had the wrong count for "partial"');
    } else {
      assert.notOk(actual.applications,          'The Summary event had an applications tally when it was not expected');
    }
    if (expected.clusters) {
      assert.isNotNull(actual.clusters,            'The Summary event did not have an clusters tally');
      assert.equal(actual.clusters.up,      expected.clusters.up,      'The Summary.clusters event had the wrong count for "up"');
      assert.equal(actual.clusters.down,    expected.clusters.down,    'The Summary.clusters event had the wrong count for "down"');
      assert.equal(actual.clusters.unknown, expected.clusters.unknown, 'The Summary.clusters event had the wrong count for "unknown"');
      assert.equal(actual.clusters.partial, expected.clusters.partial, 'The Summary.clusters event had the wrong count for "partial"');
    } else {
      assert.notOk(actual.clusters,          'The Summary event had an clusters tally when it was not expected');
    }
    if (expected.servers) {
      assert.isNotNull(actual.servers,            'The Summary event did not have an servers tally');
      assert.equal(actual.servers.up,      expected.servers.up,      'The Summary.servers event had the wrong count for "up"');
      assert.equal(actual.servers.down,    expected.servers.down,    'The Summary.servers event had the wrong count for "down"');
      assert.equal(actual.servers.unknown, expected.servers.unknown, 'The Summary.servers event had the wrong count for "unknown"');
    } else {
      assert.notOk(actual.servers,          'The Summary event had an servers tally when it was not expected');
    }
    if (expected.hosts) {
      assert.isNotNull(actual.hosts,              'The Summary event did not have an hosts tally');
      assert.equal(actual.hosts.up,      expected.hosts.up,        'The Summary.hosts event had the wrong count for "up"');
      assert.equal(actual.hosts.down,    expected.hosts.down,      'The Summary.hosts event had the wrong count for "down"');
      assert.equal(actual.hosts.unknown, expected.hosts.unknown,   'The Summary.hosts event had the wrong count for "unknown"');
      assert.equal(actual.hosts.partial, expected.hosts.partial,   'The Summary.hosts event had the wrong count for "partial"');
      assert.equal(actual.hosts.empty,   expected.hosts.empty,     'The Summary.hosts event had the wrong count for "empty"');
    } else {
      assert.notOk(actual.hosts,          'The Summary event had an hosts tally when it was not expected');
    }
  }

  /**
   * Validates the values in a "Alerts Event" are in fact what is expected.
   * 
   * An Alerts Event will always contain all of the fields.
   * 
   * @param {Object} actual The received Alerts Event object.
   * @param {number} expected The expected Alerts Event object.
   */
  function validateAlertsEvent(actual, expected) {
    assert.isTrue(expected.count >= 0,  'The expected Alerts event did not have a valid value for "count"');
    assert.isNotNull(expected.unknown,     'The expected Alerts event did not have a valid value for "unknown"');
    assert.isNotNull(expected.app,         'The expected Alerts event did not have a valid value for "app"');

    // Validate the actual observed event
    assert.isNotNull(actual,                            'The Alerts event was not received');
    assert.equal(actual.type,    'alerts' ,        'The Alerts event did not have the correct type');
    assert.equal(actual.count,   expected.count,   'The Alerts event did not have the correct count');
    assert.equal(actual.unknown.length, expected.unknown.length,  'The Alerts event did not have the correct unknown list length');
    assert.equal(actual.app.length,     expected.app.length,      'The Alerts event did not have the correct app list length');

    // Note, order should not matter, but for simplicity it does in these tests
    var i;
    for (i = 0; i < expected.unknown.length; i++) {
      assert.equal(actual.unknown[i].id,   expected.unknown[i].id,     'The Alerts event did not have the correct unknown alert id at position ' + i);
      assert.equal(actual.unknown[i].type, expected.unknown[i].type,   'The Alerts event did not have the correct unknown alert type at position ' + i);
    }
    for (i = 0; i < expected.app.length; i++) {
      assert.equal(actual.app[i].id,             expected.app[i].id,               'The Alerts event did not have the correct app alert name at position ' + i);
      assert.equal(actual.app[i].servers.length, expected.app[i].servers.length,   'The Alerts event did not have the correct app alert servers legnth at position ' + i);
      for (var j = 0; j < expected.app[i].servers.length; j++) {
        assert.equal(actual.app[i].servers[j],     expected.app[i].servers[j],     'The Alerts event did not have the correct app alert servers at position ' + i + ' did not have the expected server name');  
      }
    }
  }

  /**
   * Common logic to compare two arrays which came from an event.
   * 
   * @param {string} eventString The identifier of the event.
   * @param {string} attributeName The identifier of the attribute being compared.
   * @param {Array} expected The array with the expected values. May be null. Must not be empty.
   * @param {Array} actual The array with the actual values. May be null or undefined. Must not be empty.
   */
  function __compareArray(eventString, attributeName, expected, actual) {
    // If we are not expecting an array, ensure that the actual event did not have one
    if (!expected) {
      // The array is not expected, confirm that the array is not present (or at least falsey). The !! ensures the result is a boolean.
      assert.notOk(!!actual, 'The ' + eventString + ' had the "' + attributeName + '" attribute when it should not have been set');
    } else {
      // Basic type checking of the array
      assert.isNotNull(actual,                'The ' + eventString + ' did not have the "' + attributeName + '" attribute');
      assert.isTrue(Array.isArray(actual), 'The ' + eventString + ' attribute "' + attributeName + '" was not an array');

      // Basic validity checking - empty arrays should not be present
      assert.isTrue(expected.length > 0, 'The ' + eventString + ' attribute "' + attributeName + '" expected values was zero, this array should not have been set');
      assert.isTrue(actual.length > 0,   'The ' + eventString + ' attribute "' + attributeName + '" actual values was zero, this array should not have been set');

      assert.equal(actual.length, expected.length, 'The ' + eventString + ' attribute "' + attributeName + '" had the wrong length');
      // Iterate through and compare elements
      for (var a = 0; a < actual.length; a++) {
        assert.equal(JSON.stringify(actual[a]), JSON.stringify(expected[a]), 'The ' + eventString + ' attribute "' + attributeName + '" did not match the expected array entry at ' + a);
      }
    }
  }

  /**
   * Common logic to compare two arrays which came from an event.
   * 
   * @param {string} type The type of event, such as 'host' or 'server'.
   * @param {string} resourceName The name of the resource the event was for.
   * @param {string} attributeName The identifier of the attribute being compared.
   * @param {Array.<string>} expected The array with the expected string values. May be null. Must not be empty.
   * @param {Array.<string>} actual The array with the actual string values. May be null or undefined. Must not be empty.
   */
  function compareEventArray(type, resourceName, attributeName, expected, actual) {
    __compareArray(type + ' event for "' + resourceName + '"', attributeName, expected, actual);

  }

  /**
   * Common logic to compare two arrays which came from a collection event.
   * 
   * @param {string} type The type of event, such as 'hosts' or 'server'.
   * @param {string} attributeName The identifier of the attribute being compared.
   * @param {Array.<string>} expected The array with the expected string values. May be null. Must not be empty.
   * @param {Array.<string>} actual The array with the actual string values. May be null or undefined. Must not be empty.
   */
  function compareCollectionsEventArray(type, attributeName, expected, actual) {
    __compareArray(type + ' event', attributeName, expected, actual);
  }

  /**
   * Validates the values in a "Collections Event".
   * 
   * A Collections Event will always contain the tallies. It may also contain the added and removed arrays. The arrays must not be empty if set.
   * 
   * @param {string} type The type of the event, e.g. 'hosts' or 'clusters'
   * @param {number} expected The expected Collections Event object.
   * @param {Object} actual The received Collections Event object
   * @param {bool} hasPartial Indicates if the Collections Event expects the 'partial' tally
   * @param {bool} hasEmpty Indicates if the Collections Event expects the 'empty' tally
   */
  function compareCollectionsEvent(type, expected, actual, hasPartial, hasEmpty) {
    // Validate the expected input - the tallies must always be specified
    assert.isTrue(expected.up >= 0,        'The expected ' + type + ' event did not have a valid value for "up"');
    assert.isTrue(expected.down >= 0,      'The expected ' + type + ' event did not have a valid value for "down"');
    assert.isTrue(expected.unknown >= 0,   'The expected ' + type + ' event did not have a valid value for "unknown"');
    if (hasPartial) {
      assert.isTrue(expected.partial >= 0, 'The expected ' + type + ' event did not have a valid value for "partial"');
    }
    if (hasEmpty) {
      assert.isTrue(expected.empty >= 0,   'The expected ' + type + ' event did not have a valid value for "empty"');
    }

    // Validate the actual observed event
    assert.isNotNull(actual,                            'The ' + type + ' event was not received');
    assert.equal(actual.type,    type,             'The ' + type + ' event did not have the correct type');
    assert.equal(actual.up,      expected.up,      'The ' + type + ' event had the wrong count for "up"');
    assert.equal(actual.down,    expected.down,    'The ' + type + ' event had the wrong count for "down"');
    assert.equal(actual.unknown, expected.unknown, 'The ' + type + ' event had the wrong count for "unknown"');
    assert.equal(actual.partial, expected.partial, 'The ' + type + ' event had the wrong count for "partial"');
    assert.equal(actual.empty,   expected.empty,   'The ' + type + ' event had the wrong count for "empty"');

    // Check the Collections Event arrays
    compareCollectionsEventArray(type, 'added',   expected.added,   actual.added);
    compareCollectionsEventArray(type, 'removed', expected.removed, actual.removed);
  }

  /**
   * Validates the values in a "Hosts Event" are in fact what is expected.
   * 
   * A Hosts Event will always contain the tallies. It may also contain the added and removed arrays. The arrays must not be empty if set.
   * 
   * @param {Object} actual The received Hosts Event object.
   * @param {number} expected The expected Host Event object.
   */
  function validateHostsEvent(actual, expected) {
    compareCollectionsEvent('hosts', expected, actual, true, true);
  }

  /**
   * Validates the values in a "Servers Event" are in fact what is expected.
   * 
   * A Servers Event will always contain the tallies. It may also contain the added and removed arrays. The arrays must not be empty if set.
   * 
   * @param {Object} actual The received Servers Event object.
   * @param {number} expected The expected Servers Event object.
   */
  function validateServersEvent(actual, expected) {
    compareCollectionsEvent('servers', expected, actual, false, false);
  }

  /**
   * Validates the values in a "Clusters Event" are in fact what is expected.
   * 
   * A Clusters Event will always contain the tallies. It may also contain the added and removed arrays. The arrays must not be empty if set.
   * 
   * @param {Object} actual The received Clusters Event object.
   * @param {number} expected The expected Clusters Event object.
   */
  function validateClustersEvent(actual, expected) {
    compareCollectionsEvent('clusters', expected, actual, true, false);
  }

  /**
   * Validates the values in an "Applications Event" are in fact what is expected.
   * 
   * An Applications Event will always contain the tallies. It may also contain theadded and removed arrays. The arrays must not be empty if set.
   * 
   * @param {Object} actual The received Applications Event object.
   * @param {number} expected The expected Applications Event object.
   */
  function validateApplicationsEvent(actual, expected) {
    compareCollectionsEvent('applications', expected, actual, true, false);
  }

  /**
   * Validates the values in a "Host Event" are in fact what is expected.
   * 
   * A Host Event will contain either the 'removed' state attribute or the runtimes.added, runtimes.removed, servers.added, and servers.removed arrays. Some of the arrays could be undefined or empty.
   * The state should only ever be equal to 'removed'. If the event contains no state attribute and all of the arrays are unset or empty, then the event is invalid and should not have been sent.
   * 
   * @param {string} id The name of the host which received the event.
   * @param {Object} actual The received Host Event object.
   * @param {Object} expected The expected Host Event object.
   */
  function validateHostEvent(id, actual, expected) {
    assert.isNotNull(actual,               'The host event for "' + id + '" was not received');
    assert.equal('host', actual.type, 'The host event for "' + id + '" did not have the correct type');
    assert.equal(id,     actual.id,   'The host event for "' + id + '" did not have the correct id');

    // Validate that the event should have actually been sent. Check for some kind of meaningful information
    if (!actual.state &&
        (actual.runtimes && (!actual.runtimes.added   || actual.runtimes.added.length === 0)) &&
        (actual.runtimes && (!actual.runtimes.removed || actual.runtimes.removed.length === 0)) &&
        (actual.servers  && (!actual.servers.hasOwnProperty('up') || !actual.servers.hasOwnProperty('down') || !actual.servers.hasOwnProperty('unknown'))) &&
        (actual.servers  && (!actual.servers.added    || actual.servers.added.length === 0)) &&
        (actual.servers  && (!actual.servers.removed  || actual.servers.removed.length === 0)) &&
        (actual.apps     && (!actual.apps.hasOwnProperty('up') || !actual.apps.hasOwnProperty('down') || !actual.apps.hasOwnProperty('unknown') || !actual.apps.hasOwnProperty('partial'))) &&
        (actual.apps     && (!actual.apps.added       || actual.apps.added.length === 0)) &&
        (actual.apps     && (!actual.apps.removed     || actual.apps.removed.length === 0))) {
      assert.isTrue(false, 'The host event for "' + id + '" had no values to report on. This event should never have been sent');
    }
    if (!expected.hasOwnProperty('alerts')) {
      // The scalingPolicy is not expected, confirm that the state attribute is not present (or at least falsey). The !! ensures the result is a boolean.
      assert.notOk(!!actual.alerts, 'The host event for "' + id + '" had the "alerts" attribute when it should not have been set');
    } else {
      assert.isTrue(actual.hasOwnProperty('alerts'),            'The actual host event for "' + id + '" is missing the host.alerts object');
      assert.isTrue(expected.alerts.hasOwnProperty('unknown'),  'The expected host event for "' + id + '" is missing the host.alerts.unknown object');
      assert.isTrue(expected.alerts.hasOwnProperty('app'),      'The expected host event for "' + id + '" is missing the host.alerts.app object');
      assert.isTrue(actual.alerts.hasOwnProperty('unknown'),    'The actual host event for "' + id + '" is missing the host.alerts.unknown object');
      assert.isTrue(actual.alerts.hasOwnProperty('app'),         'The actual host event for "' + id + '" is missing the host.alerts.app object');
      assert.deepEqual(expected.alerts.unknown, actual.alerts.unknown, 'The host event for "' + id + '" had the wrong unknown alerts"');
      assert.deepEqual(expected.alerts.app,   actual.alerts.app, 'The host event for "' + id + '" had the wrong app alerts"');
    }

    if (!expected.state) {
      // The state is not expected, confirm that the state attribute is not present (or at least falsey). The !! ensures the result is a boolean.
      assert.notOk(!!actual.state, 'The host event for "' + id + '" had the "state" attribute when it should not have been set');
    } else {
      assert.equal(actual.state,    'removed',      'The expected host event for "' + id + '" has a "state" value other than "removed". "removed" is the only supported value.');
      assert.equal(actual.state,      expected.state, 'The host event for "' + id + '" had the wrong value for "state"');
      assert.notOk(actual.runtimes || actual.servers || actual.apps, 'The host event for "' + id + '" has arrays set when it should not. A "remove" event should contain only the remove state');
    }

    if (!expected.hasOwnProperty('runtimes')) {
      // The runtimes is not expected, confirm that the state attribute is not present (or at least falsey). The !! ensures the result is a boolean.
      assert.notOk(!!actual.runtimes, 'The host event for "' + id + '" had the "runtimes" attribute when it should not have been set');
    } else {
      assert.isNotNull(actual.runtimes, 'The host event for "' + id + '" expected a runtimes object but none was set');
      compareEventArray('host', id, 'runtimes.added',   expected.runtimes.added,   actual.runtimes.added);
      compareEventArray('host', id, 'runtimes.removed', expected.runtimes.removed, actual.runtimes.removed);  
    }
    if (!expected.hasOwnProperty('servers')) {
      // The servers is not expected, confirm that the state attribute is not present (or at least falsey). The !! ensures the result is a boolean.
      assert.notOk(!!actual.servers, 'The host event for "' + id + '" had the "servers" attribute when it should not have been set');
    } else {
      // Check to ensure the expectations have the tallies set
      assert.isTrue(expected.servers.hasOwnProperty('up'),      'The expected host event for "' + id + '" is missing the servers.up tally');
      assert.isTrue(expected.servers.hasOwnProperty('down'),    'The expected host event for "' + id + '" is missing the servers.down tally');
      assert.isTrue(expected.servers.hasOwnProperty('unknown'), 'The expected host event for "' + id + '" is missing the servers.unknown tally');

      // Check the actual event has the correct values
      assert.isNotNull(actual.servers, 'The host event for "' + id + '" expected a servers object but none was set');
      assert.equal(actual.servers.up,      expected.servers.up,      'The host event for "' + id + '" had the wrong count for "servers.up"');
      assert.equal(actual.servers.down,    expected.servers.down,    'The host event for "' + id + '" had the wrong count for "servers.down"');
      assert.equal(actual.servers.unknown, expected.servers.unknown, 'The host event for "' + id + '" had the wrong count for "servers.unknown"');
      compareEventArray('host', id, 'servers.added',   expected.servers.added,   actual.servers.added);
      compareEventArray('host', id, 'servers.removed', expected.servers.removed, actual.servers.removed);
    }
    if (!expected.hasOwnProperty('apps')) {
      // The apps is not expected, confirm that the state attribute is not present (or at least falsey). The !! ensures the result is a boolean.
      assert.notOk(!!actual.apps, 'The host event for "' + id + '" had the "apps" attribute when it should not have been set');
    } else {
      // Check to ensure the expectations have the tallies set
      assert.isTrue(expected.apps.hasOwnProperty('up'),      'The expected host event for "' + id + '" is missing the apps.up tally');
      assert.isTrue(expected.apps.hasOwnProperty('down'),    'The expected host event for "' + id + '" is missing the apps.down tally');
      assert.isTrue(expected.apps.hasOwnProperty('unknown'), 'The expected host event for "' + id + '" is missing the apps.unknown tally');
      assert.isTrue(expected.apps.hasOwnProperty('partial'), 'The expected host event for "' + id + '" is missing the apps.partial tally');

      // Check the actual event has the correct values
      assert.isNotNull(actual.apps, 'The host event for "' + id + '" expected a apps object but none was set');
      assert.equal(actual.apps.up,      expected.apps.up,      'The host event for "' + id + '" had the wrong count for "apps.up"');
      assert.equal(actual.apps.down,    expected.apps.down,    'The host event for "' + id + '" had the wrong count for "apps.down"');
      assert.equal(actual.apps.unknown, expected.apps.unknown, 'The host event for "' + id + '" had the wrong count for "apps.unknown"');
      assert.equal(actual.apps.partial, expected.apps.partial, 'The host event for "' + id + '" had the wrong count for "apps.partial"');
      compareEventArray('host', id, 'apps.added',   expected.apps.added,   actual.apps.added);
      compareEventArray('host', id, 'apps.removed', expected.apps.removed, actual.apps.removed);
    }
  }

  /**
   * Validates the values in a "Server Event" are in fact what is expected.
   * 
   * A Server Event will always contain at least one changed attribute. It may also contain the apps.added, apps.changed and apps.removed arrays. The arrays must not be empty if set.
   * If the event contains no changed attributes or all arrays are empty, the event is invalid and should not have been sent.
   * 
   * @param {string} id The tuple of the server which received the event.
   * @param {Object} actual The received Server Event object.
   * @param {string} expected The expected Server Event object.
   */
  function validateServerEvent(id, actual, expected) {
    assert.isNotNull(actual,                 'The server event for "' + id + '" was not received');
    assert.equal(actual.type, 'server', 'The server event for "' + id + '" did not have the correct type');
    assert.equal(actual.id,   id,    'The server event for "' + id + '" did not have the correct id');

    // Validate that the event should have actually been sent. Check for some kind of meaningful information
    if (!!actual.state   && !!actual.wlpInstallDir &&
        !!actual.cluster && !!actual.scalingPolicy &&
        (actual.apps     && (!actual.apps.hasOwnProperty('up') || !actual.apps.hasOwnProperty('down') || !actual.apps.hasOwnProperty('unknown'))) &&
        (actual.apps     && (!actual.apps.added   || actual.apps.added.length === 0)) &&
        (actual.apps     && (!actual.apps.changed || actual.apps.changed.length === 0)) &&
        (actual.apps     && (!actual.apps.removed || actual.apps.removed.length === 0))) {
      assert.isTrue(false, 'The server event for "' + id + '" had no values to report on. This event should never have been sent');
    }

    if (!expected.state) {
      // The state is not expected, confirm that the state attribute is not present (or at least falsey). The !! ensures the result is a boolean.
      assert.notOk(!!actual.state, 'The server event for "' + id + '" had the "state" attribute when it should not have been set');
    } else {
      assert.equal(actual.state, expected.state, 'The server event for "' + id + '" had the wrong value for "state"');
      if (expected.state === 'removed') {
        // Because this is a removed event, we need to make sure we don't have other values
        assert.notOk(actual.wlpInstallDir || actual.cluster || actual.scalingPolicy || actual.apps, 'The server event for "' + id + '" has other values when it should only have remove');
      }
    }

    if (!expected.wlpInstallDir) {
      // The wlpInstallDir is not expected, confirm that the state attribute is not present (or at least falsey). The !! ensures the result is a boolean.
      assert.notOk(!!actual.wlpInstallDir, 'The server event for "' + id + '" had the "wlpInstallDir" attribute when it should not have been set');
    } else {
      assert.equal(actual.wlpInstallDir, expected.wlpInstallDir, 'The server event for "' + id + '" had the wrong value for "wlpInstallDir"');
    }

    if (!expected.hasOwnProperty('cluster')) {
      // The cluster is not expected, confirm that the state attribute is not present (or at least falsey). The !! ensures the result is a boolean.
      assert.notOk(!!actual.cluster, 'The server event for "' + id + '" had the "cluster" attribute when it should not have been set');
    } else {
      assert.equal(actual.cluster, expected.cluster, 'The server event for "' + id + '" had the wrong value for "cluster"');
    }

    if (!expected.hasOwnProperty('scalingPolicy')) {
      // The scalingPolicy is not expected, confirm that the state attribute is not present (or at least falsey). The !! ensures the result is a boolean.
      assert.notOk(!!actual.scalingPolicy, 'The server event for "' + id + '" had the "scalingPolicy" attribute when it should not have been set');
    } else {
      assert.equal(actual.scalingPolicy, expected.scalingPolicy, 'The server event for "' + id + '" had the wrong value for "scalingPolicy"');
    }

    if (!expected.hasOwnProperty('scalingPolicyEnabled')) {
      // The scalingPolicyEnabled is not expected, confirm that the state attribute is not present (or at least falsey). The !! ensures the result is a boolean.
      assert.notOk(!!actual.scalingPolicyEnabled, 'The server event for "' + id + '" had the "scalingPolicyEnabled" attribute when it should not have been set');
    } else {
      assert.equal(actual.scalingPolicyEnabled, expected.scalingPolicyEnabled, 'The server event for "' + id + '" had the wrong value for "scalingPolicyEnabled"');
    }

    if (!expected.hasOwnProperty('isCollectiveController')) {
      // The isCollectiveController is not expected, confirm that the state attribute is not present (or at least falsey). The !! ensures the result is a boolean.
      assert.notOk(!!actual.isCollectiveController, 'The server event for "' + id + '" had the "isCollectiveController" attribute when it should not have been set');
    } else {
      assert.equal(actual.isCollectiveController, expected.isCollectiveController, 'The server event for "' + id + '" had the wrong value for "isCollectiveController"');
    }

    if (!expected.hasOwnProperty('alerts')) {
      // The scalingPolicy is not expected, confirm that the state attribute is not present (or at least falsey). The !! ensures the result is a boolean.
      assert.notOk(!!actual.alerts, 'The server event for "' + id + '" had the "alerts" attribute when it should not have been set');
    } else {
      assert.isTrue(actual.hasOwnProperty('alerts'),             'The actual server event for "' + id + '" is missing the server.alerts object');
      assert.isTrue(expected.alerts.hasOwnProperty('unknown'),   'The expected server event for "' + id + '" is missing the server.alerts.unknown object');
      assert.isTrue(expected.alerts.hasOwnProperty('app'),       'The expected server event for "' + id + '" is missing the server.alerts.app object');
      assert.isTrue(actual.alerts.hasOwnProperty('unknown'),     'The actual server event for "' + id + '" is missing the server.alerts.unknown object');
      assert.isTrue(actual.alerts.hasOwnProperty('app'),         'The actual server event for "' + id + '" is missing the server.alerts.app object');
      assert.deepEqual(expected.alerts.unknown, actual.alerts.unknown, 'The server event for "' + id + '" had the wrong unknown alerts"');
      assert.deepEqual(expected.alerts.app,   actual.alerts.app, 'The server event for "' + id + '" had the wrong app alerts"');
    }

    // Check server event arrays
    if (!expected.hasOwnProperty('apps')) {
      // The apps is not expected, confirm that the state attribute is not present (or at least falsey). The !! ensures the result is a boolean.
      assert.notOk(!!actual.apps, 'The server event for "' + id + '" had the "apps" attribute when it should not have been set');
    } else {
      // Check to ensure the expectations have the tallies set
      assert.isTrue(expected.apps.hasOwnProperty('up'),      'The expected server event for "' + id + '" is missing the apps.up tally');
      assert.isTrue(expected.apps.hasOwnProperty('down'),    'The expected server event for "' + id + '" is missing the apps.down tally');
      assert.isTrue(expected.apps.hasOwnProperty('unknown'), 'The expected server event for "' + id + '" is missing the apps.unknown tally');
      // Be paranoid, ensure all of the expected arrays have elements which are name/state objects.
      // We don't do this type checking on other arrays because all other arrays are supposed to contain Strings, and this is the one deviant case.
      if (expected.apps.added) {
        for (var i = 0; i < expected.apps.added.length; i++) {
          var curApp = expected.apps.added[i];
          if (!(curApp.hasOwnProperty('name') && curApp.hasOwnProperty('state'))) {
            assert.isTrue(false, 'The expected server event for "' + id + '" has an element in apps.added which is not an Object with a name and a state');
          }
        }
      }
      if (expected.apps.changed) {
        for (var i = 0; i < expected.apps.changed.length; i++) {
          var curApp = expected.apps.changed[i];
          if (!(curApp.hasOwnProperty('name') && curApp.hasOwnProperty('state'))) {
            assert.isTrue(false, 'The expected server event for "' + id + '" has an element in apps.added which is not an Object with a name and a state');
          }
        }
      }

      // Check the actual event has the correct values
      assert.isNotNull(actual.apps, 'The server event for "' + id + '" expected an apps object but none was set');
      assert.equal(actual.apps.up,      expected.apps.up,      'The server event for "' + id + '" had the wrong count for "apps.up"');
      assert.equal(actual.apps.down,    expected.apps.down,    'The server event for "' + id + '" had the wrong count for "apps.down"');
      assert.equal(actual.apps.unknown, expected.apps.unknown, 'The server event for "' + id + '" had the wrong count for "apps.unknown"');
      compareEventArray('server', id, 'apps.added',   expected.apps.added,   actual.apps.added);
      compareEventArray('server', id, 'apps.changed', expected.apps.changed, actual.apps.changed);
      compareEventArray('server', id, 'apps.removed', expected.apps.removed, actual.apps.removed);
    }
  }

  /**
   * Validates the values in a "Cluster Event" are in fact what is expected.
   * 
   * A Cluster Event will always contain at least one changed attribute. It may also contain the servers.added, servers.removed, apps.added, and apps.removed arrays. The arrays must not be empty if set.
   * If the event contains no changed attributes or all arrays are empty, the event is invalid and should not have been sent.
   * 
   * @param {string} id The name of the cluster which received the event.
   * @param {Object} actual The received Cluster Event object.
   * @param {string} expected The expected Cluster Event object.
   */
  function validateClusterEvent(id, actual, expected) {
    assert.isNotNull(actual,                  'The cluster event for "' + id + '" was not received');
    assert.equal(actual.type, 'cluster', 'The cluster event for "' + id + '" did not have the correct type');
    assert.equal(actual.id,   id,        'The cluster event for "' + id + '" did not have the correct id');

    // Validate that the event should have actually been sent. Check for some kind of meaningful information
    if (!!actual.state  && !!actual.scalingPolicy &&
        (actual.servers && (!actual.servers.hasOwnProperty('up') || !actual.servers.hasOwnProperty('down') || !actual.servers.hasOwnProperty('unknown'))) &&
        (actual.servers && (!actual.servers.added   || actual.servers.added.length === 0)) &&
        (actual.servers && (!actual.servers.removed || actual.servers.removed.length === 0)) &&
        (actual.apps    && (!actual.apps.hasOwnProperty('up') || !actual.apps.hasOwnProperty('down') || !actual.apps.hasOwnProperty('unknown') || !actual.apps.hasOwnProperty('partial'))) &&
        (actual.apps    && (!actual.apps.added   || actual.apps.added.length === 0)) &&
        (actual.apps    && (!actual.apps.removed || actual.apps.removed.length === 0))) {
      assert.isTrue(false, 'The cluster event for "' + id + '" had no values to report on. This event should never have been sent');
    }
    if (!expected.hasOwnProperty('alerts')) {
      // The alerts is not expected, confirm that the state attribute is not present (or at least falsey). The !! ensures the result is a boolean.
      // TODO: Re-enable this check. Right now we're always sending alerts along and its inefficient, but its not the worst thing we could do
      //assert.notOk(!!actual.alerts, 'The cluster event for "' + id + '" had the "alerts" attribute when it should not have been set');
    } else {
      assert.isTrue(actual.hasOwnProperty('alerts'),                'The actual cluster event for "' + id + '" is missing the cluster.alerts object');
      assert.isTrue(expected.alerts.hasOwnProperty('unknown'),      'The expected cluster event for "' + id + '" is missing the cluster.alerts.unknown object');
      assert.isTrue(expected.alerts.hasOwnProperty('app'),          'The expected cluster event for "' + id + '" is missing the cluster.alerts.app object');
      assert.isTrue(actual.alerts.hasOwnProperty('unknown'),        'The actual cluster event for "' + id + '" is missing the cluster.alerts.unknown object');
      assert.isTrue(actual.alerts.hasOwnProperty('app'),            'The actual cluster event for "' + id + '" is missing the cluster.alerts.app object');
      assert.deepEqual(expected.alerts.unknown, actual.alerts.unknown, 'The cluster event for "' + id + '" had the wrong unknown alerts"');
      assert.deepEqual(expected.alerts.app,     actual.alerts.app, 'The cluster event for "' + id + '" had the wrong app alerts"');
    }

    if (!expected.state) {
      // The state is not expected, confirm that the state attribute is not present (or at least falsey). The !! ensures the result is a boolean.
      assert.notOk(!!actual.state, 'The cluster event for "' + id + '" had the "state" attribute when it should not have been set');
    } else {
      assert.equal(actual.state, expected.state, 'The cluster event for "' + id + '" had the wrong value for "state"');
      if (expected.state === 'removed') {
        // Because this is a removed event, we need to make sure we don't have other values
        assert.notOk(actual.scalingPolicy || actual.servers || actual.apps, 'The cluster event for "' + id + '" has other values when it should only have remove');
      }
    }

    if (!expected.hasOwnProperty('scalingPolicy')) {
      // The scalingPolicy is not expected, confirm that the state attribute is not present (or at least falsey). The !! ensures the result is a boolean.
      assert.notOk(!!actual.scalingPolicy, 'The cluster event for "' + id + '" had the "scalingPolicy" attribute when it should not have been set');
    } else {
      assert.equal(actual.scalingPolicy, expected.scalingPolicy, 'The cluster event for "' + id + '" had the wrong value for "scalingPolicy"');
    }

    // Check cluster event arrays
    if (!expected.hasOwnProperty('servers')) {
      // The servers is not expected, confirm that the state attribute is not present (or at least falsey). The !! ensures the result is a boolean.
      assert.notOk(!!actual.servers, 'The cluster event for "' + id + '" had the "servers" attribute when it should not have been set');
    } else {
      // Check to ensure the expectations have the tallies set
      assert.isTrue(expected.servers.hasOwnProperty('up'),      'The expected cluster event for "' + id + '" is missing the servers.up tally');
      assert.isTrue(expected.servers.hasOwnProperty('down'),    'The expected cluster event for "' + id + '" is missing the servers.down tally');
      assert.isTrue(expected.servers.hasOwnProperty('unknown'), 'The expected cluster event for "' + id + '" is missing the servers.unknown tally');

      // Check the actual event has the correct values
      assert.isNotNull(actual.servers, 'The cluster event for "' + id + '" expected a servers object but none was set');
      assert.equal(actual.servers.up,      expected.servers.up,      'The cluster event for "' + id + '" had the wrong count for "servers.up"');
      assert.equal(actual.servers.down,    expected.servers.down,    'The cluster event for "' + id + '" had the wrong count for "servers.down"');
      assert.equal(actual.servers.unknown, expected.servers.unknown, 'The cluster event for "' + id + '" had the wrong count for "servers.unknown"');
      compareEventArray('cluster', id, 'servers.added',   expected.servers.added,   actual.servers.added);
      compareEventArray('cluster', id, 'servers.removed', expected.servers.removed, actual.servers.removed);
    }
    if (!expected.hasOwnProperty('apps')) {
      // The apps is not expected, confirm that the state attribute is not present (or at least falsey). The !! ensures the result is a boolean.
      assert.notOk(!!actual.apps, 'The cluster event for "' + id + '" had the "apps" attribute when it should not have been set');
    } else {
      // Check to ensure the expectations have the tallies set
      assert.isTrue(expected.apps.hasOwnProperty('up'),      'The expected cluster event for "' + id + '" is missing the apps.up tally');
      assert.isTrue(expected.apps.hasOwnProperty('down'),    'The expected cluster event for "' + id + '" is missing the apps.down tally');
      assert.isTrue(expected.apps.hasOwnProperty('unknown'), 'The expected cluster event for "' + id + '" is missing the apps.unknown tally');
      assert.isTrue(expected.apps.hasOwnProperty('partial'), 'The expected cluster event for "' + id + '" is missing the apps.partial tally');

      // Check the actual event has the correct values
      assert.isNotNull(actual.apps, 'The cluster event for "' + id + '" expected a apps object but none was set');
      assert.equal(actual.apps.up,      expected.apps.up,      'The cluster event for "' + id + '" had the wrong count for "apps.up"');
      assert.equal(actual.apps.down,    expected.apps.down,    'The cluster event for "' + id + '" had the wrong count for "apps.down"');
      assert.equal(actual.apps.unknown, expected.apps.unknown, 'The cluster event for "' + id + '" had the wrong count for "apps.unknown"');
      assert.equal(actual.apps.partial, expected.apps.partial, 'The cluster event for "' + id + '" had the wrong count for "apps.partial"');
      compareEventArray('cluster', id, 'apps.added',   expected.apps.added,   actual.apps.added);
      compareEventArray('cluster', id, 'apps.removed', expected.apps.removed, actual.apps.removed);
    }
  }

  /**
   * Validates the values in an "Application Event" are in fact what is expected.
   * 
   * An Application Event will always contain at least one changed attribute. It may also contain the servers.added, servers.removed, apps.added, and apps.removed arrays. The arrays must not be empty if set.
   * If the event contains no changed attributes or all arrays are empty, the event is invalid and should not have been sent.
   * 
   * @param {string} id The name of the application which received the event.
   * @param {Object} actual The received Application Event object.
   * @param {string} expected The expected Application Event object.
   */
  function validateApplicationEvent(id, actual, expected) {
    assert.isNotNull(actual,                      'The application event for "' + id + '" was not received');
    assert.equal('application', actual.type, 'The application event for "' + id + '" did not have the correct type');
    assert.equal(id,            actual.id,   'The application event for "' + id + '" did not have the correct id');

    // Validate that the event should have actually been sent. Check for some kind of meaningful information
    if (!!actual.state && !actual.hasOwnProperty('up') && !actual.hasOwnProperty('down') && !actual.hasOwnProperty('unknown') &&
        (actual.servers  && (!actual.servers.hasOwnProperty('up') || !actual.servers.hasOwnProperty('down') || !actual.servers.hasOwnProperty('unknown'))) &&
        (actual.servers  && (!actual.servers.added   || actual.servers.added.length === 0)) &&
        (actual.servers  && (!actual.servers.removed || actual.servers.removed.length === 0)) &&
        (actual.clusters && (!actual.clusters.hasOwnProperty('up') || !actual.clusters.hasOwnProperty('down') || !actual.clusters.hasOwnProperty('unknown') || !actual.clusters.hasOwnProperty('partial'))) &&
        (actual.clusters && (!actual.clusters.added   || actual.clusters.added.length === 0)) &&
        (actual.clusters && (!actual.clusters.removed || actual.clusters.removed.length === 0))) {
      assert.isTrue(false, 'The application event for "' + id + '" had no values to report on. This event should never have been sent');
    }
    if (!expected.hasOwnProperty('alerts')) {
      // The scalingPolicy is not expected, confirm that the state attribute is not present (or at least falsey). The !! ensures the result is a boolean.
      assert.notOk(!!actual.alerts, 'The application event for "' + id + '" had the "alerts" attribute when it should not have been set');
    } else {
      assert.isTrue(actual.hasOwnProperty('alerts'),              'The actual application event for "' + id + '" is missing the application.alerts object');
      assert.isTrue(expected.alerts.hasOwnProperty('unknown'),    'The expected application event for "' + id + '" is missing the application.alerts.unknown object');
      assert.isTrue(expected.alerts.hasOwnProperty('app'),        'The expected application event for "' + id + '" is missing the application.alerts.app object');
      assert.isTrue(actual.alerts.hasOwnProperty('unknown'),      'The actual application event for "' + id + '" is missing the application.alerts.unknown object');
      assert.isTrue(actual.alerts.hasOwnProperty('app'),          'The actual application event for "' + id + '" is missing the application.alerts.app object');
      assert.deepEqual(expected.alerts.unknown, actual.alerts.unknown, 'The application event for "' + id + '" had the wrong unknown alerts"');
      assert.deepEqual(expected.alerts.app,     actual.alerts.app, 'The application event for "' + id + '" had the wrong app alerts"');
    }

    // Check to ensure the expectations have the tallies set when the state is not removed
    if (expected.state !== 'removed') {
      assert.isTrue(expected.hasOwnProperty('up'),      'The expected application event for "' + id + '" is missing the up tally');
      assert.isTrue(expected.hasOwnProperty('down'),    'The expected application event for "' + id + '" is missing the down tally');
      assert.isTrue(expected.hasOwnProperty('unknown'), 'The expected application event for "' + id + '" is missing the unknown tally');
    }

    // When up, down or unknown are set, we should set all
    assert.equal(actual.up,      expected.up,      'The application event for "' + id + '" had the wrong count for "up"');
    assert.equal(actual.down,    expected.down,    'The application event for "' + id + '" had the wrong count for "down"');
    assert.equal(actual.unknown, expected.unknown, 'The application event for "' + id + '" had the wrong count for "unknown"');

    if (!expected.state) {
      // The state is not expected, confirm that the state attribute is not present (or at least falsey). The !! ensures the result is a boolean.
      assert.notOk(!!actual.state, 'The application event for "' + id + '" had the "state" attribute when it should not have been set');
    } else {
      assert.equal(actual.state, expected.state, 'The application event for "' + id + '" had the wrong value for "state"');
      if (expected.state === 'removed') {
        // Because this is a removed event, we need to make sure we don't have other values
        assert.notOk(actual.up || actual.down || actual.unknown || actual.servers || actual.clusters, 'The application event for "' + id + '" has other values when it should only have remove');
      }
    }

    if (!expected.scalingPolicy) {
      // The scalingPolicy is not expected, confirm that the scalingPolicy attribute is not present (or at least falsey). The !! ensures the result is a boolean.
      assert.notOk(!!actual.scalingPolicy, 'The application event for "' + id + '" had the "scalingPolicy" attribute when it should not have been set');
    } else {
      assert.equal(actual.scalingPolicy, expected.scalingPolicy, 'The application event for "' + id + '" had the wrong value for "scalingPolicy"');
    }

    // Check application event arrays
    if (!expected.hasOwnProperty('servers')) {
      // The servers is not expected, confirm that the state attribute is not present (or at least falsey). The !! ensures the result is a boolean.
      assert.notOk(!!actual.servers, 'The application event for "' + id + '" had the "servers" attribute when it should not have been set');
    } else {
      // Check to ensure the expectations have the tallies set
      assert.isTrue(expected.servers.hasOwnProperty('up'),      'The expected application event for "' + id + '" is missing the application.up tally');
      assert.isTrue(expected.servers.hasOwnProperty('down'),    'The expected application event for "' + id + '" is missing the application.down tally');
      assert.isTrue(expected.servers.hasOwnProperty('unknown'), 'The expected application event for "' + id + '" is missing the application.unknown tally');

      // Check the actual event has the correct values
      assert.isNotNull(actual.servers, 'The application event for "' + id + '" expected a servers object but none was set');
      assert.equal(actual.servers.up,      expected.servers.up,      'The application event for "' + id + '" had the wrong count for "servers.up"');
      assert.equal(actual.servers.down,    expected.servers.down,    'The application event for "' + id + '" had the wrong count for "servers.down"');
      assert.equal(actual.servers.unknown, expected.servers.unknown, 'The application event for "' + id + '" had the wrong count for "servers.unknown"');
      compareEventArray('application', id, 'servers.added',   expected.servers.added,   actual.servers.added);
      compareEventArray('application', id, 'servers.removed', expected.servers.removed, actual.servers.removed);
    }
    if (!expected.hasOwnProperty('clusters')) {
      // The clusters is not expected, confirm that the state attribute is not present (or at least falsey). The !! ensures the result is a boolean.
      assert.notOk(!!actual.clusters, 'The application event for "' + id + '" had the "clusters" attribute when it should not have been set');
    } else {
      // Check to ensure the expectations have the tallies set
      assert.isTrue(expected.clusters.hasOwnProperty('up'),      'The expected application event for "' + id + '" is missing the clusters.up tally');
      assert.isTrue(expected.clusters.hasOwnProperty('down'),    'The expected application event for "' + id + '" is missing the clusters.down tally');
      assert.isTrue(expected.clusters.hasOwnProperty('unknown'), 'The expected application event for "' + id + '" is missing the clusters.unknown tally');
      assert.isTrue(expected.clusters.hasOwnProperty('partial'), 'The expected application event for "' + id + '" is missing the clusters.partial tally');

      // Check the actual event has the correct values
      assert.isNotNull(actual.clusters, 'The application event for "' + id + '" expected a clusters object but none was set');
      assert.equal(actual.clusters.up,      expected.clusters.up,      'The application event for "' + id + '" had the wrong count for "clusters.up"');
      assert.equal(actual.clusters.down,    expected.clusters.down,    'The application event for "' + id + '" had the wrong count for "clusters.down"');
      assert.equal(actual.clusters.unknown, expected.clusters.unknown, 'The application event for "' + id + '" had the wrong count for "clusters.unknown"');
      assert.equal(actual.clusters.partial, expected.clusters.partial, 'The application event for "' + id + '" had the wrong count for "clusters.partial"');
      compareEventArray('application', id, 'clusters.added',   expected.clusters.added,   actual.clusters.added);
      compareEventArray('application', id, 'clusters.removed', expected.clusters.removed, actual.clusters.removed);
    }
  }

  // Define the REST API response for an empty collective. While this is unlikely to happen, it could
  var emptyCollective = { 
      hosts: { allServersRunning: 0, allServersStopped: 0, allServersUnknown: 0, someServersRunning: 0, noServers: 0, ids: [], list: [] },
      servers: { up: 0, down: 0, unknown: 0, ids: [], list: [] },
      clusters: { up: 0, down: 0, unknown: 0, partial: 0, ids: [], list: [] },
      applications: { up: 0, down: 0, unknown: 0, partial: 0, ids: [], list: [] },
      runtimes: { allServersRunning: 0, allServersStopped: 0, allServersUnknown: 0, someServersRunning: 0, noServers: 0, ids: [], list: [] },
      alerts: { count: 0, unknown: [], app: [] }
  };

  // Define the REST API response for a single host with no servers.
  var oneEmptyHost = {
      hosts: { allServersRunning: 0, allServersStopped: 0, allServersUnknown: 0, someServersRunning: 0, noServers: 1,
        ids: [ 'localhost' ],
        list:
          [ { 
            id: 'localhost',
            name: 'localhost',
            wlpInstallDirs: [],
            wlpUserDirs: [],
            runtimes: [],
            serversState: 'EMPTY',
            servers: { up: 0, down: 0, unknown: 0, list: [] },
            alerts: { count: 0, unknown: [], app: [] }
          } ]
      },
      servers: { up: 0, down: 0, unknown: 0, ids: [], list: [] },
      clusters: { up: 0, down: 0, unknown: 0, partial: 0, ids: [], list: [] },
      applications: { up: 0, down: 0, unknown: 0, partial: 0, ids: [], list: [] },
      runtimes: { allServersRunning: 0, allServersStopped: 0, allServersUnknown: 0, someServersRunning: 0, noServers: 0, ids: [], list: [] },
      alerts: { count: 0, unknown: [], app: [] }
  };

  // Define the REST API response for a two empty hosts (host with no servers).
  var twoEmptyHosts = { 
      hosts: { allServersRunning: 0, allServersStopped: 0, allServersUnknown: 0, someServersRunning: 0, noServers: 2,
        ids: [ 'localhost', 'emptyhost' ],
        list:
          [ { 
            id: 'localhost',
            name: 'localhost',
            wlpInstallDirs: [],
            wlpUserDirs: [],
            runtimes: [],
            serversState: 'EMPTY',
            servers: { up: 0, down: 0, unknown: 0, list: [] },
            alerts: { count: 0, unknown: [], app: [] }
          }, { 
            id: 'emptyhost',
            name: 'emptyhost',
            wlpInstallDirs: [],
            wlpUserDirs: [],
            runtimes: [],
            serversState: 'EMPTY',
            servers: { up: 0, down: 0, unknown: 0, list: [] },
            alerts: { count: 0, unknown: [], app: [] }
          } ]
      },
      servers: { up: 0, down: 0, unknown: 0, ids: [], list: [] },
      clusters: { up: 0, down: 0, unknown: 0, partial: 0, ids: [], list: [] },
      applications: { up: 0, down: 0, unknown: 0, partial: 0, ids: [], list: [] },
      runtimes: { allServersRunning: 0, allServersStopped: 0, allServersUnknown: 0, someServersRunning: 0, noServers: 0, ids: [], list: [] },
      alerts: { count: 0, unknown: [], app: [] }
  };

  // Define the REST API response for a single running server with no applications.
  var oneRunningServerWithNoApps = { 
      hosts: { allServersRunning: 1, allServersStopped: 0, allServersUnknown: 0, someServersRunning: 0, noServers: 0,
        ids: [ 'localhost' ],
        list:
          [ { 
            id: 'localhost',
            name: 'localhost',
            wlpInstallDirs: [ '/wlp' ],
            wlpUserDirs: [ '/wlp/usr' ],
            runtimes: { list: [{id:'localhost,/wlp', type:'runtime', name:'localhost,wlp'}] },
            serversState: 'STARTED',
            servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ] },
            alerts: { count: 0, unknown: [], app: [] }
          } ]
      },
      servers: { up: 1, down: 0, unknown: 0,
        ids: [ 'localhost,/wlp/usr,server1' ],
        list: 
          [ {
            id: 'localhost,/wlp/usr,server1',
            name: 'server1',
            wlpUserDir: '/wlp/usr',
            host: 'localhost',
            state: 'STARTED',
            wlpInstallDir: '/wlp',
            apps: { up: 0, down: 0, unknown: 0, list: [] },
            alerts: { count: 0, unknown: [], app: [] }
          } ]
      },
      clusters: { up: 0, down: 0, unknown: 0, partial: 0, ids: [], list: [] },
      applications: { up: 0, down: 0, unknown: 0, partial: 0, ids: [], list: [] },
      runtimes: { allServersRunning: 0, allServersStopped: 0, allServersUnknown: 0, someServersRunning: 0, noServers: 0, ids: [], list: [] },
      alerts: { count: 0, unknown: [], app: [] }
  };

  // Define the REST API response for a single stopped server with no applications.
  var oneStoppedServerWithNoApps ={ 
      hosts: { allServersRunning: 0, allServersStopped: 1, allServersUnknown: 0, someServersRunning: 0, noServers: 0,
        ids: [ 'localhost' ],
        list:
          [ { 
            id: 'localhost',
            name: 'localhost',
            wlpInstallDirs: [ '/wlp' ],
            wlpUserDirs: [ '/wlp/usr' ],
            runtimes: { list: [{id:'localhost,/wlp', type:'runtime', name:'localhost,wlp'}] },
            serversState: 'STOPPED',
            servers: { up: 0, down: 1, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ] },
            alerts: { count: 0, unknown: [], app: [] }
          } ]
      },
      servers: { up: 0, down: 1, unknown: 0,
        ids: [ 'localhost,/wlp/usr,server1' ],
        list: 
          [ {
            id: 'localhost,/wlp/usr,server1',
            name: 'server1',
            wlpUserDir: '/wlp/usr',
            host: 'localhost',
            state: 'STOPPED',
            wlpInstallDir: '/wlp',
            apps: { up: 0, down: 0, unknown: 0, list: [] },
            alerts: { count: 0, unknown: [], app: [] }
          } ]
      },
      clusters: { up: 0, down: 0, unknown: 0, partial: 0, ids: [], list: [] },
      applications: { up: 0, down: 0, unknown: 0, partial: 0, ids: [], list: [] },
      runtimes: { allServersRunning: 0, allServersStopped: 0, allServersUnknown: 0, someServersRunning: 0, noServers: 0, ids: [], list: [] },
      alerts: { count: 0, unknown: [], app: [] }
  };

  // Define the REST API response for a single running server with no applications.
  var twoRunningServersWithNoAppsOnLocalhost = {
      hosts: { allServersRunning: 1, allServersStopped: 0, allServersUnknown: 0, someServersRunning: 0, noServers: 0,
        ids: [ 'localhost' ],
        list:
          [ { 
            id: 'localhost',
            name: 'localhost',
            wlpInstallDirs: [ '/wlp' ],
            wlpUserDirs: [ '/wlp/usr' ],
            runtimes: { list: [{id:'localhost,/wlp', type:'runtime', name:'localhost,wlp'}] },
            serversState: 'STARTED',
            servers: { up: 2, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1', 'localhost,/wlp/usr,server2' ] },
            alerts: { count: 0, unknown: [], app: [] }
          } ] 
      },
      servers: { up: 2, down: 0, unknown: 0,
        ids: [ 'localhost,/wlp/usr,server1', 'localhost,/wlp/usr,server2' ],
        list: 
          [ {
            id: 'localhost,/wlp/usr,server1',
            name: 'server1',
            wlpUserDir: '/wlp/usr',
            host: 'localhost',
            state: 'STARTED',
            wlpInstallDir: '/wlp',
            apps: { up: 0, down: 0, unknown: 0, list: [] },
            alerts: { count: 0, unknown: [], app: [] }
          },
          {
            id: 'localhost,/wlp/usr,server2',
            name: 'server2',
            wlpUserDir: '/wlp/usr',
            host: 'localhost',
            state: 'STARTED',
            wlpInstallDir: '/wlp',
            apps: { up: 0, down: 0, unknown: 0, list: [] },
            alerts: { count: 0, unknown: [], app: [] }
          } ]
      },
      clusters: { up: 0, down: 0, unknown: 0, partial: 0, ids: [], list: [] },
      applications: { up: 0, down: 0, unknown: 0, partial: 0, ids: [], list: [] },
      runtimes: { allServersRunning: 0, allServersStopped: 0, allServersUnknown: 0, someServersRunning: 0, noServers: 0, ids: [], list: [] },
      alerts: { count: 0, unknown: [], app: [] }
  };

  // Define the REST API response for a single running server with no applications.
  var oneRunningClusteredServerWithNoApps = {
      hosts: { allServersRunning: 1, allServersStopped: 0, allServersUnknown: 0, someServersRunning: 0, noServers: 0,
        ids: [ 'localhost' ],
        list:
          [ {
            id: 'localhost',
            name: 'localhost',
            wlpInstallDirs: [ '/wlp' ],
            wlpUserDirs: [ '/wlp/usr' ],
            runtimes: { list: [{id:'localhost,/wlp', type:'runtime', name:'localhost,wlp'}] },
            serversState: 'STARTED',
            servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ] },
            alerts: { count: 0, unknown: [], app: [] }
          } ] 
      },
      servers: { up: 1, down: 0, unknown: 0,
        ids: [ 'localhost,/wlp/usr,server1' ],
        list: 
          [ {
            id: 'localhost,/wlp/usr,server1',
            name: 'server1',
            wlpUserDir: '/wlp/usr',
            host: 'localhost',
            state: 'STARTED',
            wlpInstallDir: '/wlp',
            cluster: 'cluster1',
            apps: { up: 0, down: 0, unknown: 0, list: [] },
            alerts: { count: 0, unknown: [], app: [] }
          } ] 
      },
      clusters: { up: 1, down: 0, unknown: 0, partial: 0,
        ids: [ 'cluster1' ],
        list: [ {
          id: 'cluster1',
          name: 'cluster1',
          state: 'STARTED',
          servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ] },
          apps: { up: 0, down: 0, unknown: 0, partial: 0, list: [] },
          alerts: { count: 0, unknown: [], app: [] }
        }] 
      },
      applications: { up: 0, down: 0, unknown: 0, partial: 0, ids: [], list: [] },
      runtimes: { allServersRunning: 0, allServersStopped: 0, allServersUnknown: 0, someServersRunning: 0, noServers: 0, ids: [], list: [] },
      alerts: { count: 0, unknown: [], app: [] }
  };

  // Define the REST API response for a single running server with no applications with a scaling policy.
  var oneRunningClusteredServerWithNoAppsWithScalingPolicy = {
      hosts: { allServersRunning: 1, allServersStopped: 0, allServersUnknown: 0, someServersRunning: 0, noServers: 0,
        ids: [ 'localhost' ],
        list:
          [ {
            id: 'localhost',
            name: 'localhost',
            wlpInstallDirs: [ '/wlp' ],
            wlpUserDirs: [ '/wlp/usr' ],
            runtimes: { list: [{id:'localhost,/wlp', type:'runtime', name:'localhost,wlp'}] },
            serversState: 'STARTED',
            servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ] },
            alerts: { count: 0, unknown: [], app: [] }
          } ] 
      },
      servers: { up: 1, down: 0, unknown: 0,
        ids: [ 'localhost,/wlp/usr,server1' ],
        list: 
          [ {
            id: 'localhost,/wlp/usr,server1',
            name: 'server1',
            wlpUserDir: '/wlp/usr',
            host: 'localhost',
            state: 'STARTED',
            wlpInstallDir: '/wlp',
            cluster: 'cluster1',
            scalingPolicy: 'default',
            apps: { up: 0, down: 0, unknown: 0, list: [] },
            alerts: { count: 0, unknown: [], app: [] }
          } ] 
      },
      clusters: { up: 1, down: 0, unknown: 0, partial: 0,
        ids: [ 'cluster1' ],
        list: [ {
          id: 'cluster1',
          name: 'cluster1',
          state: 'STARTED',
          scalingPolicy: 'default',
          servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ] },
          apps: { up: 0, down: 0, unknown: 0, partial: 0, list: [] },
          alerts: { count: 0, unknown: [], app: [] }
        }] 
      },
      applications: { up: 0, down: 0, unknown: 0, partial: 0, ids: [], list: [] },
      runtimes: { allServersRunning: 0, allServersStopped: 0, allServersUnknown: 0, someServersRunning: 0, noServers: 0, ids: [], list: [] },
      alerts: { count: 0, unknown: [], app: [] }
  };

  // Define the REST API response for a single running server with no applications with a scaling policy.
  var oneRunningClusteredServerWithNoAppsWithNewScalingPolicy = {
      hosts: { allServersRunning: 1, allServersStopped: 0, allServersUnknown: 0, someServersRunning: 0, noServers: 0,
        ids: [ 'localhost' ],
        list:
          [ {
            id: 'localhost',
            name: 'localhost',
            wlpInstallDirs: [ '/wlp' ],
            wlpUserDirs: [ '/wlp/usr' ],
            runtimes: { list: [{id:'localhost,/wlp', type:'runtime', name:'localhost,wlp'}] },
            serversState: 'STARTED',
            servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ] },
            alerts: { count: 0, unknown: [], app: [] }
          } ] 
      },
      servers: { up: 1, down: 0, unknown: 0,
        ids: [ 'localhost,/wlp/usr,server1' ],
        list: 
          [ {
            id: 'localhost,/wlp/usr,server1',
            name: 'server1',
            wlpUserDir: '/wlp/usr',
            host: 'localhost',
            state: 'STARTED',
            wlpInstallDir: '/wlp',
            cluster: 'cluster1',
            scalingPolicy: 'newPolicy',
            apps: { up: 0, down: 0, unknown: 0, list: [] },
            alerts: { count: 0, unknown: [], app: [] }
          } ] 
      },
      clusters: { up: 1, down: 0, unknown: 0, partial: 0,
        ids: [ 'cluster1' ],
        list: [ {
          id: 'cluster1',
          name: 'cluster1',
          state: 'STARTED',
          scalingPolicy: 'newPolicy',
          servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ] },
          apps: { up: 0, down: 0, unknown: 0, partial: 0, list: [] },
          alerts: { count: 0, unknown: [], app: [] }
        }] 
      },
      applications: { up: 0, down: 0, unknown: 0, partial: 0, ids: [], list: [] },
      runtimes: { allServersRunning: 0, allServersStopped: 0, allServersUnknown: 0, someServersRunning: 0, noServers: 0, ids: [], list: [] },
      alerts: { count: 0, unknown: [], app: [] }
  };

  // Define the REST API response for a single running server with no applications.
  var oneStoppedClusteredServerWithNoApps = {
      hosts: { allServersRunning: 0, allServersStopped: 1, allServersUnknown: 0, someServersRunning: 0, noServers: 0,
        ids: [ 'localhost' ],
        list:
          [ {
            id: 'localhost',
            name: 'localhost',
            wlpInstallDirs: [ '/wlp' ],
            wlpUserDirs: [ '/wlp/usr' ],
            runtimes: { list: [{id:'localhost,/wlp', type:'runtime', name:'localhost,wlp'}] },
            serversState: 'STOPPED',
            servers: { up: 0, down: 1, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ] },
            alerts: { count: 0, unknown: [], app: [] }
          } ] 
      },
      servers: { up: 0, down: 1, unknown: 0,
        ids: [ 'localhost,/wlp/usr,server1' ],
        list: 
          [ {
            id: 'localhost,/wlp/usr,server1',
            name: 'server1',
            wlpUserDir: '/wlp/usr',
            host: 'localhost',
            state: 'STOPPED',
            wlpInstallDir: '/wlp',
            cluster: 'cluster1',
            apps: { up: 0, down: 0, unknown: 0, list: [] },
            alerts: { count: 0, unknown: [], app: [] }
          } ] 
      },
      clusters: { up: 0, down: 1, unknown: 0, partial: 0,
        ids: [ 'cluster1' ],
        list: [ {
          id: 'cluster1',
          name: 'cluster1',
          state: 'STOPPED',
          servers: { up: 0, down: 1, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ] },
          apps: { up: 0, down: 0, unknown: 0, partial: 0, list: [] },
          alerts: { count: 0, unknown: [], app: [] }
        }] 
      },
      applications: { up: 0, down: 0, unknown: 0, partial: 0, ids: [], list: [] },
      runtimes: { allServersRunning: 0, allServersStopped: 0, allServersUnknown: 0, someServersRunning: 0, noServers: 0, ids: [], list: [] },
      alerts: { count: 0, unknown: [], app: [] }
  };

  // Define the REST API response for a single starting clustered server
  var oneStartingClusteredServerWithNoApps = {
      hosts: { allServersRunning: 0, allServersStopped: 1, allServersUnknown: 0, someServersRunning: 0, noServers: 0,
        ids: [ 'localhost' ],
        list:
          [ {
            id: 'localhost',
            name: 'localhost',
            wlpInstallDirs: [ '/wlp' ],
            wlpUserDirs: [ '/wlp/usr' ],
            runtimes: { list: [{id:'localhost,/wlp', type:'runtime', name:'localhost,wlp'}] },
            serversState: 'STOPPED',
            servers: { up: 0, down: 1, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ] },
            alerts: { count: 0, unknown: [], app: [] }
          } ] 
      },
      servers: { up: 0, down: 1, unknown: 0,
        ids: [ 'localhost,/wlp/usr,server1' ],
        list: 
          [ {
            id: 'localhost,/wlp/usr,server1',
            name: 'server1',
            wlpUserDir: '/wlp/usr',
            host: 'localhost',
            state: 'STARTING',
            wlpInstallDir: '/wlp',
            cluster: 'cluster1',
            apps: { up: 0, down: 0, unknown: 0, list: [] },
            alerts: { count: 0, unknown: [], app: [] }
          } ] 
      },
      clusters: { up: 0, down: 1, unknown: 0, partial: 0,
        ids: [ 'cluster1' ],
        list: [ {
          id: 'cluster1',
          name: 'cluster1',
          state: 'STARTING',
          servers: { up: 0, down: 1, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ] },
          apps: { up: 0, down: 0, unknown: 0, partial: 0, list: [] },
          alerts: { count: 0, unknown: [], app: [] }
        }] 
      },
      applications: { up: 0, down: 0, unknown: 0, partial: 0, ids: [], list: [] },
      runtimes: { allServersRunning: 0, allServersStopped: 0, allServersUnknown: 0, someServersRunning: 0, noServers: 0, ids: [], list: [] },
      alerts: { count: 0, unknown: [], app: [] }
  };

  // Define the REST API response for a single starting clustered server
  var oneStoppingClusteredServerWithNoApps = {
      hosts: { allServersRunning: 0, allServersStopped: 1, allServersUnknown: 0, someServersRunning: 0, noServers: 0,
        ids: [ 'localhost' ],
        list:
          [ {
            id: 'localhost',
            name: 'localhost',
            wlpInstallDirs: [ '/wlp' ],
            wlpUserDirs: [ '/wlp/usr' ],
            runtimes: { list: [{id:'localhost,/wlp', type:'runtime', name:'localhost,wlp'}] },
            serversState: 'STOPPED',
            servers: { up: 0, down: 1, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ] },
            alerts: { count: 0, unknown: [], app: [] }
          } ] 
      },
      servers: { up: 0, down: 1, unknown: 0,
        ids: [ 'localhost,/wlp/usr,server1' ],
        list: 
          [ {
            id: 'localhost,/wlp/usr,server1',
            name: 'server1',
            wlpUserDir: '/wlp/usr',
            host: 'localhost',
            state: 'STOPPING',
            wlpInstallDir: '/wlp',
            cluster: 'cluster1',
            apps: { up: 0, down: 0, unknown: 0, list: [] },
            alerts: { count: 0, unknown: [], app: [] }
          } ] 
      },
      clusters: { up: 0, down: 1, unknown: 0, partial: 0,
        ids: [ 'cluster1' ],
        list: [ {
          id: 'cluster1',
          name: 'cluster1',
          state: 'STOPPING',
          servers: { up: 0, down: 1, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ] },
          apps: { up: 0, down: 0, unknown: 0, partial: 0, list: [] },
          alerts: { count: 0, unknown: [], app: [] }
        }] 
      },
      applications: { up: 0, down: 0, unknown: 0, partial: 0, ids: [], list: [] },
      runtimes: { allServersRunning: 0, allServersStopped: 0, allServersUnknown: 0, someServersRunning: 0, noServers: 0, ids: [], list: [] },
      alerts: { count: 0, unknown: [], app: [] }
  };

  // Define the REST API response for a single running server with no applications.
  var oneRunningClustered2ServerWithNoApps = {
      hosts: { allServersRunning: 1, allServersStopped: 0, allServersUnknown: 0, someServersRunning: 0, noServers: 0,
        ids: [ 'localhost' ],
        list:
          [ {
            id: 'localhost',
            name: 'localhost',
            wlpInstallDirs: [ '/wlp' ],
            wlpUserDirs: [ '/wlp/usr' ],
            runtimes: { list: [{id:'localhost,/wlp', type:'runtime', name:'localhost,wlp'}] },
            serversState: 'STARTED',
            servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ] },
            alerts: { count: 0, unknown: [], app: [] }
          } ] 
      },
      servers: { up: 1, down: 0, unknown: 0,
        ids: [ 'localhost,/wlp/usr,server1' ],
        list: 
          [ {
            id: 'localhost,/wlp/usr,server1',
            name: 'server1',
            wlpUserDir: '/wlp/usr',
            host: 'localhost',
            state: 'STARTED',
            wlpInstallDir: '/wlp',
            cluster: 'cluster2',
            apps: { up: 0, down: 0, unknown: 0, list: [] },
            alerts: { count: 0, unknown: [], app: [] }
          } ] 
      },
      clusters: { up: 1, down: 0, unknown: 0, partial: 0,
        ids: [ 'cluster2' ],
        list: [ {
          id: 'cluster2',
          name: 'cluster2',
          state: 'STARTED',
          servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ] },
          apps: { up: 0, down: 0, unknown: 0, partial: 0, list: [] },
          alerts: { count: 0, unknown: [], app: [] }
        }] 
      },
      applications: { up: 0, down: 0, unknown: 0, partial: 0, ids: [], list: [] },
      runtimes: { allServersRunning: 0, allServersStopped: 0, allServersUnknown: 0, someServersRunning: 0, noServers: 0, ids: [], list: [] },
      alerts: { count: 0, unknown: [], app: [] }
  };

  // Define the REST API response for a 2 running servers in 2 different clusters (each with no applications).
  var twoRunningClusteredServerWithNoApps = { 
      hosts: { allServersRunning: 1, allServersStopped: 0, allServersUnknown: 0, someServersRunning: 0, noServers: 0,
        ids: [ 'localhost' ],
        list:
          [ { 
            id: 'localhost',
            name: 'localhost',
            wlpInstallDirs: [ '/wlp' ],
            wlpUserDirs: [ '/wlp/usr' ],
            runtimes: { list: [{id:'localhost,/wlp', type:'runtime', name:'localhost,wlp'}] },
            serversState: 'STARTED',
            servers: { up: 2, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1', 'localhost,/wlp/usr,server2' ] },
            alerts: { count: 0, unknown: [], app: [] }
          } ] 
      },
      servers: { up: 2, down: 0, unknown: 0,
        ids: [ 'localhost,/wlp/usr,server1', 'localhost,/wlp/usr,server2' ],
        list: 
          [ {
            id: 'localhost,/wlp/usr,server1',
            name: 'server1',
            wlpUserDir: '/wlp/usr',
            host: 'localhost',
            state: 'STARTED',
            wlpInstallDir: '/wlp',
            cluster: 'cluster1',
            apps: { up: 0, down: 0, unknown: 0, list: [] },
            alerts: { count: 0, unknown: [], app: [] }
          },
          {
            id: 'localhost,/wlp/usr,server2',
            name: 'server2',
            wlpUserDir: '/wlp/usr',
            host: 'localhost',
            state: 'STARTED',
            wlpInstallDir: '/wlp',
            cluster: 'cluster2',
            apps: { up: 0, down: 0, unknown: 0, list: [] },
            alerts: { count: 0, unknown: [], app: [] }
          } ]
      },
      clusters: { up: 2, down: 0, unknown: 0, partial: 0,
        ids: [ 'cluster1', 'cluster2' ],
        list: [ {
          id: 'cluster1',
          name: 'cluster1',
          state: 'STARTED',
          servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ] },
          apps: { up: 0, down: 0, unknown: 0, partial: 0, list: [] },
          alerts: { count: 0, unknown: [], app: [] }
        }, {
          id: 'cluster2',
          name: 'cluster2',
          state: 'STARTED',
          servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server2' ] },
          apps: { up: 0, down: 0, unknown: 0, partial: 0, list: [] },
          alerts: { count: 0, unknown: [], app: [] }
        } ] 
      },
      applications: { up: 0, down: 0, unknown: 0, partial: 0, ids: [], list: [] },
      runtimes: { allServersRunning: 0, allServersStopped: 0, allServersUnknown: 0, someServersRunning: 0, noServers: 0, ids: [], list: [] },
      alerts: { count: 0, unknown: [], app: [] }
  };

  // Define the REST API response for a 2 running servers in 2 different clusters (each with no applications).
  var twoRunningSameClusterServerWithNoApps = { 
      hosts: { allServersRunning: 1, allServersStopped: 0, allServersUnknown: 0, someServersRunning: 0, noServers: 0,
        ids: [ 'localhost' ],
        list:
          [ { 
            id: 'localhost',
            name: 'localhost',
            wlpInstallDirs: [ '/wlp' ],
            wlpUserDirs: [ '/wlp/usr' ],
            runtimes: { list: [{id:'localhost,/wlp', type:'runtime', name:'localhost,wlp'}] },
            serversState: 'STARTED',
            servers: { up: 2, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1', 'localhost,/wlp/usr,server2' ] },
            alerts: { count: 0, unknown: [], app: [] }
          } ] 
      },
      servers: { up: 2, down: 0, unknown: 0,
        ids: [ 'localhost,/wlp/usr,server1', 'localhost,/wlp/usr,server2' ],
        list: 
          [ {
            id: 'localhost,/wlp/usr,server1',
            name: 'server1',
            wlpUserDir: '/wlp/usr',
            host: 'localhost',
            state: 'STARTED',
            wlpInstallDir: '/wlp',
            cluster: 'cluster1',
            apps: { up: 0, down: 0, unknown: 0, list: [] },
            alerts: { count: 0, unknown: [], app: [] }
          },
          {
            id: 'localhost,/wlp/usr,server2',
            name: 'server2',
            wlpUserDir: '/wlp/usr',
            host: 'localhost',
            state: 'STARTED',
            wlpInstallDir: '/wlp',
            cluster: 'cluster2',
            apps: { up: 0, down: 0, unknown: 0, list: [] },
            alerts: { count: 0, unknown: [], app: [] }
          } ]
      },
      clusters: { up: 1, down: 0, unknown: 0, partial: 0,
        ids: [ 'cluster1' ],
        list: [ {
          id: 'cluster1',
          name: 'cluster1',
          state: 'STARTED',
          servers: { up: 2, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1', 'localhost,/wlp/usr,server2' ] },
          apps: { up: 0, down: 0, unknown: 0, partial: 0, list: [] },
          alerts: { count: 0, unknown: [], app: [] }
        } ] 
      },
      applications: { up: 0, down: 0, unknown: 0, partial: 0, ids: [], list: [] },
      runtimes: { allServersRunning: 0, allServersStopped: 0, allServersUnknown: 0, someServersRunning: 0, noServers: 0, ids: [], list: [] },
      alerts: { count: 0, unknown: [], app: [] }
  };

  // Define the REST API response for a single host with one server with one application.
  var oneRunningServerWithOneApp =  {
      hosts: { allServersRunning: 1, allServersStopped: 0, allServersUnknown: 0, someServersRunning: 0, noServers: 0,
        ids: [ 'localhost' ],
        list:
          [ { 
            id: 'localhost',
            name: 'localhost',
            wlpInstallDirs: [ '/wlp' ],
            wlpUserDirs: [ '/wlp/usr' ],
            runtimes: { list: [{id:'localhost,/wlp', type:'runtime', name:'localhost,wlp'}] },
            serversState: 'STARTED',
            servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ] },
            alerts: { count: 0, unknown: [], app: [] }
          } ]
      },
      servers: { up: 1, down: 0, unknown: 0,
        ids: [ 'localhost,/wlp/usr,server1' ],
        list: 
          [ {
            id: 'localhost,/wlp/usr,server1',
            name: 'server1',
            wlpUserDir: '/wlp/usr',
            host: 'localhost',
            state: 'STARTED',
            wlpInstallDir: '/wlp',
            apps: { up: 1, down: 0, unknown: 0, list: [ { 
              id: 'localhost,/wlp/usr,server1,app1', name: 'app1', state: 'STARTED' 
            }] },
            alerts: { count: 0, unknown: [], app: [] }
          } ]
      },
      clusters: { up: 0, down: 0, unknown: 0, partial: 0, ids: [], list: [] },
      applications: { up: 1, down: 0, unknown: 0, partial: 0,
        ids: [ 'localhost,/wlp/usr,server1,app1' ],
        list: [ {
          id: 'localhost,/wlp/usr,server1,app1',
          name: 'app1',
          state: 'STARTED',
          up: 1, down: 0, unknown: 0,
          servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ] },
          clusters: { up: 0, down: 0, unknown: 0, partial: 0, ids: [] },
          alerts: { count: 0, unknown: [], app: [] }
        } ] 
      },
      runtimes: { allServersRunning: 0, allServersStopped: 0, allServersUnknown: 0, someServersRunning: 0, noServers: 0, ids: [], list: [] },
      alerts: { count: 0, unknown: [], app: [] }
  };

  // Define the REST API response for a single host with one server with one application.
  var oneRunningServerWithOneAppAndTagsChanged =  {
      hosts: { allServersRunning: 1, allServersStopped: 0, allServersUnknown: 0, someServersRunning: 0, noServers: 0,
        ids: [ 'localhost' ],
        list:
          [ { 
            id: 'localhost',
            name: 'localhost',
            wlpInstallDirs: [ '/wlp' ],
            wlpUserDirs: [ '/wlp/usr' ],
            runtimes: { list: [{id:'localhost,/wlp', type:'runtime', name:'localhost,wlp'}] },
            serversState: 'STARTED',
            servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ] },
            alerts: { count: 0, unknown: [], app: [] }
          } ]
      },
      servers: { up: 1, down: 0, unknown: 0,
        ids: [ 'localhost,/wlp/usr,server1' ],
        list: 
          [ {
            id: 'localhost,/wlp/usr,server1',
            name: 'server1',
            wlpUserDir: '/wlp/usr',
            host: 'localhost',
            state: 'STARTED',
            wlpInstallDir: '/wlp',
            apps: { up: 1, down: 0, unknown: 0, list: [ { 
              id: 'localhost,/wlp/usr,server1,app1', name: 'app1', state: 'STARTED'
            } ] },
            tags: ['tagA', 'tagB', 'newTag'],
            owner: 'Michal',
            contacts: ['Amy'],
            alerts: { count: 0, unknown: [], app: [] }
          } ]
      },
      clusters: { up: 0, down: 0, unknown: 0, partial: 0, ids: [], list: [] },
      applications: { up: 1, down: 0, unknown: 0, partial: 0,
        ids: [ 'localhost,/wlp/usr,server1,app1' ],
        list: [ {
          id: 'localhost,/wlp/usr,server1,app1',
          name: 'app1',
          state: 'STARTED',
          up: 1, down: 0, unknown: 0,
          servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ] },
          clusters: { up: 0, down: 0, unknown: 0, partial: 0, ids: [] },
          alerts: { count: 0, unknown: [], app: [] }
        } ] 
      },
      runtimes: { allServersRunning: 0, allServersStopped: 0, allServersUnknown: 0, someServersRunning: 0, noServers: 0, ids: [], list: [] },
      alerts: { count: 0, unknown: [], app: [] }
  };


  //Define the REST API response for a single host with one clustered server with one application.
  var oneRunningClusteredServerWithOneApp  = {
      hosts: { allServersRunning: 1, allServersStopped: 0, allServersUnknown: 0, someServersRunning: 0, noServers: 0,
        ids: [ 'localhost' ],
        list:
          [ { 
            id: 'localhost',
            name: 'localhost',
            wlpInstallDirs: [ '/wlp' ],
            wlpUserDirs: [ '/wlp/usr' ],
            runtimes: { list: [{id:'localhost,/wlp', type:'runtime', name:'localhost,wlp'}] },
            serversState: 'STARTED',
            servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ] },
            alerts: { count: 0, unknown: [], app: [] }
          } ]
      },
      servers: { up: 1, down: 0, unknown: 0,
        ids: [ 'localhost,/wlp/usr,server1' ],
        list: 
          [ {
            id: 'localhost,/wlp/usr,server1',
            name: 'server1',
            wlpUserDir: '/wlp/usr',
            host: 'localhost',
            state: 'STARTED',
            wlpInstallDir: '/wlp',
            cluster: 'cluster1',
            apps: { up: 1, down: 0, unknown: 0, list: [ { 
              id: 'cluster1,app1', name: 'app1', state: 'STARTED'
            }] },
            alerts: { count: 0, unknown: [], app: [] }
          } ]
      },
      clusters: { up: 1, down: 0, unknown: 0, partial: 0,
        ids: [ 'cluster1' ],
        list: [ {
          id: 'cluster1',
          name: 'cluster1',
          state: 'STARTED',
          servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ] },
          apps: { up: 1, down: 0, unknown: 0, partial: 0,
            list: [ {
              id: 'cluster1,app1',
              name: 'app1',
              state: 'STARTED',
              up: 1, down: 0, unknown: 0,
              servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ] }
            } ] },
            alerts: { count: 0, unknown: [], app: [] }
        } ] },
        applications: { up: 1, down: 0, unknown: 0, partial: 0,
          ids: [ 'cluster1,app1' ],
          list: [ {
            id: 'cluster1,app1',
            name: 'app1',
            state: 'STARTED',
            up: 1, down: 0, unknown: 0,
            servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ] },
            clusters: { up: 1, down: 0, unknown: 0, partial: 0, ids: [ 'cluster1' ] },
            alerts: { count: 0, unknown: [], app: [] }
          } ] 
        },
        runtimes: { allServersRunning: 0, allServersStopped: 0, allServersUnknown: 0, someServersRunning: 0, noServers: 0, ids: [], list: [] },
        alerts: { count: 0, unknown: [], app: [] }
  };

  //Define the REST API response for a single host with one clustered server with one application with a scaling policy.
  var oneRunningClusteredServerWithOneAppWithScalingPolicy  = {
      hosts: { allServersRunning: 1, allServersStopped: 0, allServersUnknown: 0, someServersRunning: 0, noServers: 0,
        ids: [ 'localhost' ],
        list:
          [ { 
            id: 'localhost',
            name: 'localhost',
            wlpInstallDirs: [ '/wlp' ],
            wlpUserDirs: [ '/wlp/usr' ],
            runtimes: { list: [{id:'localhost,/wlp', type:'runtime', name:'localhost,wlp'}] },
            serversState: 'STARTED',
            servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ] },
            alerts: { count: 0, unknown: [], app: [] }
          } ]
      },
      servers: { up: 1, down: 0, unknown: 0,
        ids: [ 'localhost,/wlp/usr,server1' ],
        list: 
          [ {
            id: 'localhost,/wlp/usr,server1',
            name: 'server1',
            wlpUserDir: '/wlp/usr',
            host: 'localhost',
            state: 'STARTED',
            wlpInstallDir: '/wlp',
            cluster: 'cluster1',
            scalingPolicy: 'default',
            apps: { up: 1, down: 0, unknown: 0, list: [ { 
              id: 'cluster1,app1', name: 'app1', state: 'STARTED'
            }] },
            alerts: { count: 0, unknown: [], app: [] }
          } ]
      },
      clusters: { up: 1, down: 0, unknown: 0, partial: 0,
        ids: [ 'cluster1' ],
        list: [ {
          id: 'cluster1',
          name: 'cluster1',
          state: 'STARTED',
          scalingPolicy: 'default',
          servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ] },
          apps: { up: 1, down: 0, unknown: 0, partial: 0,
            list: [ {
              id: 'cluster1,app1',
              name: 'app1',
              state: 'STARTED',
              up: 1, down: 0, unknown: 0,
              servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ] }
            } ] },
            alerts: { count: 0, unknown: [], app: [] }
        } ] },
        applications: { up: 1, down: 0, unknown: 0, partial: 0,
          ids: [ 'cluster1,app1' ],
          list: [ {
            id: 'cluster1,app1',
            name: 'app1',
            state: 'STARTED',
            up: 1, down: 0, unknown: 0,
            scalingPolicy: 'default',
            servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ] },
            clusters: { up: 1, down: 0, unknown: 0, partial: 0, ids: [ 'cluster1' ] },
            alerts: { count: 0, unknown: [], app: [] }
          } ] 
        },
        runtimes: { allServersRunning: 0, allServersStopped: 0, allServersUnknown: 0, someServersRunning: 0, noServers: 0, ids: [], list: [] },
        alerts: { count: 0, unknown: [], app: [] }
  };

  //Define the REST API response for a single host with one clustered server with one application with a different scaling policy.
  var oneRunningClusteredServerWithOneAppWithNewScalingPolicy  = {
      hosts: { allServersRunning: 1, allServersStopped: 0, allServersUnknown: 0, someServersRunning: 0, noServers: 0,
        ids: [ 'localhost' ],
        list:
          [ { 
            id: 'localhost',
            name: 'localhost',
            wlpInstallDirs: [ '/wlp' ],
            wlpUserDirs: [ '/wlp/usr' ],
            runtimes: { list: [{id:'localhost,/wlp', type:'runtime', name:'localhost,wlp'}] },
            serversState: 'STARTED',
            servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ] },
            alerts: { count: 0, unknown: [], app: [] }
          } ]
      },
      servers: { up: 1, down: 0, unknown: 0,
        ids: [ 'localhost,/wlp/usr,server1' ],
        list: 
          [ {
            id: 'localhost,/wlp/usr,server1',
            name: 'server1',
            wlpUserDir: '/wlp/usr',
            host: 'localhost',
            state: 'STARTED',
            wlpInstallDir: '/wlp',
            cluster: 'cluster1',
            scalingPolicy: 'newPolicy',
            apps: { up: 1, down: 0, unknown: 0, list: [ { 
              id: 'cluster1,app1', name: 'app1', state: 'STARTED'
            }] },
            alerts: { count: 0, unknown: [], app: [] }
          } ]
      },
      clusters: { up: 1, down: 0, unknown: 0, partial: 0,
        ids: [ 'cluster1' ],
        list: [ {
          id: 'cluster1',
          name: 'cluster1',
          state: 'STARTED',
          scalingPolicy: 'newPolicy',
          servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ] },
          apps: { up: 1, down: 0, unknown: 0, partial: 0,
            list: [ {
              id: 'cluster1,app1',
              name: 'app1',
              state: 'STARTED',
              up: 1, down: 0, unknown: 0,
              servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ] }
            } ] },
            alerts: { count: 0, unknown: [], app: [] }
        } ] },
        applications: { up: 1, down: 0, unknown: 0, partial: 0,
          ids: [ 'cluster1,app1' ],
          list: [ {
            id: 'cluster1,app1',
            name: 'app1',
            state: 'STARTED',
            up: 1, down: 0, unknown: 0,
            scalingPolicy: 'default',
            servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ] },
            clusters: { up: 1, down: 0, unknown: 0, partial: 0, ids: [ 'cluster1' ] },
            alerts: { count: 0, unknown: [], app: [] }
          } ] 
        },
        runtimes: { allServersRunning: 0, allServersStopped: 0, allServersUnknown: 0, someServersRunning: 0, noServers: 0, ids: [], list: [] },
        alerts: { count: 0, unknown: [], app: [] }
  };

  // Define the REST API response for a single host with one server with one application.
  var oneRunningServerWithOneStoppedApp = {
      hosts: { allServersRunning: 1, allServersStopped: 0, allServersUnknown: 0, someServersRunning: 0, noServers: 0,
        ids: [ 'localhost' ],
        list:
          [ { 
            id: 'localhost',
            name: 'localhost',
            wlpInstallDirs: [ '/wlp' ],
            wlpUserDirs: [ '/wlp/usr' ],
            runtimes: { list: [{id:'localhost,/wlp', type:'runtime', name:'localhost,wlp'}] },
            serversState: 'STARTED',
            servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ] },
            alerts: { count: 1, unknown: [], app: [ {name: "localhost,/wlp/usr,server1,app1", servers:["localhost,/wlp/usr,server1"]} ] }
          } ]
      },
      servers: { up: 1, down: 0, unknown: 0,
        ids: [ 'localhost,/wlp/usr,server1' ],
        list: 
          [ {
            id: 'localhost,/wlp/usr,server1',
            name: 'server1',
            wlpUserDir: '/wlp/usr',
            host: 'localhost',
            state: 'STARTED',
            wlpInstallDir: '/wlp',
            apps: { up: 0, down: 1, unknown: 0, list: [ { 
              id: 'localhost,/wlp/usr,server1,app1', name: 'app1', state: 'STOPPED' 
            }] },
            alerts: { count: 1, unknown: [], app: [ {name: "localhost,/wlp/usr,server1,app1", servers:["localhost,/wlp/usr,server1"]} ] }
          } ]
      },
      clusters: { up: 0, down: 0, unknown: 0, partial: 0, ids: [], list: [] },
      applications: { up: 0, down: 1, unknown: 0, partial: 0,
        ids: [ 'localhost,/wlp/usr,server1,app1' ],
        list: [ {
          id: 'localhost,/wlp/usr,server1,app1',
          name: 'app1',
          state: 'STOPPED',
          up: 0, down: 1, unknown: 0,
          servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ] },
          clusters: { up: 0, down: 0, unknown: 0, partial: 0, ids: [] },
          alerts: { count: 1, unknown: [], app: [{name: "localhost,/wlp/usr,server1,app1", servers: ["localhost,/wlp/usr,server1"]} ] }
        } ] 
      },
      runtimes: { allServersRunning: 0, allServersStopped: 0, allServersUnknown: 0, someServersRunning: 0, noServers: 0, ids: [], list: [] },
      alerts: { count: 1, unknown: [], app: [ {name: "localhost,/wlp/usr,server1,app1", servers: ["localhost,/wlp/usr,server1"]} ] }
  };


  //Define the REST API response for a single host with one clustered server with one application.
  var oneRunningClusteredServerWithOneStoppedApp = {
      hosts: { allServersRunning: 1, allServersStopped: 0, allServersUnknown: 0, someServersRunning: 0, noServers: 0,
        ids: [ 'localhost' ],
        list:
          [ { 
            id: 'localhost',
            name: 'localhost',
            wlpInstallDirs: [ '/wlp' ],
            wlpUserDirs: [ '/wlp/usr' ],
            runtimes: { list: [{id:'localhost,/wlp', type:'runtime', name:'localhost,wlp'}] },
            serversState: 'STARTED',
            servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ] },
            alerts: { count: 1, unknown: [], app: [ {name: "cluster1,app1", servers:["localhost,/wlp/usr,server1"]} ] }
          } ]
      },
      servers: { up: 1, down: 0, unknown: 0,
        ids: [ 'localhost,/wlp/usr,server1' ],
        list: 
          [ {
            id: 'localhost,/wlp/usr,server1',
            name: 'server1',
            wlpUserDir: '/wlp/usr',
            host: 'localhost',
            state: 'STARTED',
            wlpInstallDir: '/wlp',
            cluster: 'cluster1',
            apps: { up: 0, down: 1, unknown: 0, list: [ { 
              id: 'cluster1,app1', name: 'app1', state: 'STOPPED'
            }] },
            alerts: { count: 1, unknown: [], app: [ {name: "cluster1,app1", servers:["localhost,/wlp/usr,server1"]} ] }
          } ]
      },
      clusters: { up: 1, down: 0, unknown: 0, partial: 0,
        ids: [ 'cluster1' ],
        list: [ {
          id: 'cluster1',
          name: 'cluster1',
          state: 'STARTED',
          servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ] },
          apps: { up: 0, down: 1, unknown: 0, partial: 0,
            list: [ {
              id: 'cluster1,app1',
              name: 'app1',
              state: 'STOPPED',   //Denise
              up: 0, down: 1, unknown: 0,
              servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ] }
            } ] },
            alerts: { count: 1, unknown: [], app: [ {name: "cluster1,app1", servers:["localhost,/wlp/usr,server1"]} ] }
        } ] },
        applications: { up: 0, down: 1, unknown: 0, partial: 0,
          ids: [ 'cluster1,app1' ],
          list: [ {
            id: 'cluster1,app1',
            name: 'app1',
            state: 'STOPPED',
            up: 0, down: 1, unknown: 0,
            servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ] },
            clusters: { up: 1, down: 0, unknown: 0, partial: 0, ids: [ 'cluster1' ] },
            alerts: { count: 1, unknown: [], app: [ {name: "cluster1,app1", servers:["localhost,/wlp/usr,server1"]} ] }
          } ] 
        },
        runtimes: { allServersRunning: 0, allServersStopped: 0, allServersUnknown: 0, someServersRunning: 0, noServers: 0, ids: [], list: [] },
        alerts: { count: 1, unknown: [], app: [ {name: "cluster1,app1", servers:["localhost,/wlp/usr,server1"]} ] }
  };

  // Define the REST API response for a single host with one server with one application.
  // See 'oneRunningServerWithOneApp' for the original definition
  var appRunningOnTwoServers = {
      hosts: { allServersRunning: 1, allServersStopped: 0, allServersUnknown: 0, someServersRunning: 0, noServers: 0,
        ids: [ 'localhost' ],
        list:
          [ { 
            id: 'localhost',
            name: 'localhost',
            wlpInstallDirs: [ '/wlp' ],
            wlpUserDirs: [ '/wlp/usr' ],
            runtimes: { list: [{id:'localhost,/wlp', type:'runtime', name:'localhost,wlp'}] },
            serversState: 'STARTED',
            servers: { up: 2, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1', 'localhost,/wlp/usr,server2' ] },
            alerts: { count: 0, unknown: [], app: [] }
          } ] 
      },
      servers: { up: 2, down: 0, unknown: 0,
        ids: [ 'localhost,/wlp/usr,server1', 'localhost,/wlp/usr,server2' ],
        list: 
          [ {
            id: 'localhost,/wlp/usr,server1',
            name: 'server1',
            wlpUserDir: '/wlp/usr',
            host: 'localhost',
            state: 'STARTED',
            wlpInstallDir: '/wlp',
            apps: { up: 1, down: 0, unknown: 0, list: [ {
              id: 'localhost,/wlp/usr,server1,app1', name: 'app1', state: 'STARTED'
            } ] },
            alerts: { count: 0, unknown: [], app: [] }
          },
          {
            id: 'localhost,/wlp/usr,server2',
            name: 'server2',
            wlpUserDir: '/wlp/usr',
            host: 'localhost',
            state: 'STARTED',
            wlpInstallDir: '/wlp',
            apps: { up: 1, down: 0, unknown: 0, list: [ {
              id: 'localhost,/wlp/usr,server2,app1', name: 'app1', state: 'STARTED'
            }] },
            alerts: { count: 0, unknown: [], app: [] }
          } ]
      },
      clusters: { up: 0, down: 0, unknown: 0, partial: 0, ids: [], list: [] },
      applications: { up: 1, down: 0, unknown: 0, partial: 0,
        ids: [ 'localhost,/wlp/usr,server1,app1','localhost,/wlp/usr,server2,app1' ],
        list: [ {
          id: 'localhost,/wlp/usr,server1,app1',
          name: 'app1',
          state: 'STARTED',
          up: 1, down: 0, unknown: 0,
          servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ] },
          clusters: { up: 0, down: 0, unknown: 0, partial: 0, ids: [] },
          alerts: { count: 0, unknown: [], app: [] }
        },
        {
          id: 'localhost,/wlp/usr,server2,app1',
          name: 'app1',
          state: 'STARTED',
          up: 1, down: 0, unknown: 0,
          servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server2' ] },
          clusters: { up: 0, down: 0, unknown: 0, partial: 0, ids: [] },
          alerts: { count: 0, unknown: [], app: [] }
        }] 
      },
      runtimes: { allServersRunning: 0, allServersStopped: 0, allServersUnknown: 0, someServersRunning: 0, noServers: 0, ids: [], list: [] },
      alerts: { count: 0, unknown: [], app: [] }
  };

  /**
   * Clears the resourceManager of any cached objects
   */
  function clearCache() {
    unified.__resourceManager.__reset();
  }

  /**
   * Sets the cached applications collections.
   */
  function setCachedApplications(obj) {
    unified.__resourceManager.__applications.collection.instance = obj;
  }

  /**
   * Sets the cached clusters collections.
   */
  function setCachedClusters(obj) {
    unified.__resourceManager.__clusters.collection.instance = obj;
  }

  /**
   * Sets the cached servers collections.
   */
  function setCachedServers(obj) {
    unified.__resourceManager.__servers.collection.instance = obj;
  }

  /**
   * Sets the cached hosts collections.
   */
  function setCachedHosts(obj) {
    unified.__resourceManager.__hosts.collection.instance = obj;
  }

  /**
   * Sets the cached runtimes collections.
   */
  function setCachedRuntimes(obj) {
    unified.__resourceManager.__runtimes.collection.instance = obj;
  }

  /**
   * Sets the cached Summary and Alerts object based on the other already set collections.
   * 
   * @param {object} alerts The alerts object to set, if specified
   */
  function setCachedSummaryAndAlerts(alerts) {
    unified.__resourceManager.__summary.instance = {
        type: 'summary',
        applications: unified.__resourceManager.__applications.collection.instance,
        clusters: unified.__resourceManager.__clusters.collection.instance,
        servers: unified.__resourceManager.__servers.collection.instance,
        hosts: unified.__resourceManager.__hosts.collection.instance,
        runtimes: unified.__resourceManager.__runtimes.collection.instance
    };

    if (alerts) {
      unified.__resourceManager.__alerts.instance = {
          type: 'alerts',
          count: alerts.count,
          unknown: alerts.unknown,
          app: alerts.app
      };
    } else {
      unified.__resourceManager.__alerts.instance = {
          type: 'alerts',
          count: 0,
          unknown: [],
          app: []
      };
    }
  }

  /**
   * Sets the cached object for the given host.
   * Clones the input object to ensure no bleed-over during tests.
   */
  function setCachedHost(obj) {
    if (!obj.hasOwnProperty('alerts')) {
      // Inject the default alerts if the obj didn't define it
      obj.alerts = { count: 0, unknown: [], app: [] };
    }
    unified.__resourceManager.__hosts.objects[obj.id] = { instance: JSON.parse(JSON.stringify(obj)) };
  }

  /**
   * Sets the cached object for the given cluster.
   * Clones the input object to ensure no bleed-over during tests.
   */
  function setCachedCluster(obj) {
    if (!obj.hasOwnProperty('alerts')) {
      // Inject the default alerts if the obj didn't define it
      obj.alerts = { count: 0, unknown: [], app: [] };
    }
    unified.__resourceManager.__clusters.objects[obj.id] = { instance: JSON.parse(JSON.stringify(obj)) };
  }

  /**
   * Sets the cached object for the given server.
   * Clones the input object to ensure no bleed-over during tests.
   */
  function setCachedServer(obj) {
    if (!obj.hasOwnProperty('alerts')) {
      // Inject the default alerts if the obj didn't define it
      obj.alerts = { count: 0, unknown: [], app: [] };
    }
    
    if (!obj.hasOwnProperty('ports')) {
      // Inject the default ports if the obj didn't define it
      obj.ports = { httpPorts:{"9080"}, httpsPorts:{"9443"}};
    }
    unified.__resourceManager.__servers.objects[obj.id] = { instance: JSON.parse(JSON.stringify(obj)) };
  }

  /**
   * Sets the cached object for the given application.
   * Clones the input object to ensure no bleed-over during tests.
   */
  function setCachedApplication(obj) {
    if (!obj.hasOwnProperty('alerts')) {
      // Inject the default alerts if the obj didn't define it
      obj.alerts = { count: 0, unknown: [], app: [] };
    }
    unified.__resourceManager.__applications.objects[obj.id] = { instance: JSON.parse(JSON.stringify(obj)) };
  }

  /**
   * Defines the 'Applications Tests' module test suite.
   */
  tdd.suite('Unified Change Listener Tests', function() {

    tdd.beforeEach(function() {
      topicListeners = [];
      clearCache();
    });

    tdd.afterEach(function() {
      for (var i = 0; i < topicListeners.length; i++) {
        topicListeners[i].remove();
      }
    });

    /**
     * When there are no cached collections, nothing should be sent
     */
    tdd.test('0a. When there are no cached collections, nothing should be sent', function() {
      var summaryEvent = null;
      addTopicListener(topicUtil.getTopicByType('summary'),  function(e) {
        summaryEvent = e;
      });

      var alertsEvent = null;
      addTopicListener(topicUtil.getTopicByType('alerts'),  function(e) {
        alertsEvent = e;
      });

      var applicationsEvent = null;
      addTopicListener(topicUtil.getTopicByType('applications'),  function(e) {
        applicationsEvent = e;
      });

      var clustersEvent = null;
      addTopicListener(topicUtil.getTopicByType('clusters'),  function(e) {
        clustersEvent = e;
      });

      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      var hostsEvent = null;
      addTopicListener(topicUtil.getTopicByType('hosts'),  function(e) {
        hostsEvent = e;
      });

      // cached = emptyCollective
      clearCache();

      var now = clone(oneEmptyHost);

      unified.__sendUnifiedNotifications(now);

      validateEventWasNotReceived('summary', summaryEvent);
      validateEventWasNotReceived('alerts', alertsEvent);
      validateEventWasNotReceived('applications', applicationsEvent);
      validateEventWasNotReceived('clusters', clustersEvent);
      validateEventWasNotReceived('servers', serversEvent);
      validateEventWasNotReceived('hosts', hostsEvent);
    });

    /**
     * Adding a new empty host to an empty collective should produce the following events:
     * - hosts - updated tallies and 1 added host
     * 
     * No other events should be sent because this host has no server, cluster, app or runtime.
     */
    tdd.test('1a. Add a new empty host to an empty collective', function() {
      var summaryEvent = null;
      addTopicListener(topicUtil.getTopicByType('summary'),  function(e) {
        summaryEvent = e;
      });

      var alertsEvent = null;
      addTopicListener(topicUtil.getTopicByType('alerts'),  function(e) {
        alertsEvent = e;
      });

      var hostsEvent = null;
      addTopicListener(topicUtil.getTopicByType('hosts'),  function(e) {
        hostsEvent = e;
      });

      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      var clustersEvent = null;
      addTopicListener(topicUtil.getTopicByType('clusters'),  function(e) {
        clustersEvent = e;
      });

      var runtimesEvent = null;
      addTopicListener(topicUtil.getTopicByType('runtimes'),  function(e) {
        runtimesEvent = e;
      });

      // cached = emptyCollective
      setCachedApplications({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedClusters({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedServers({ up: 0, down: 0, unknown: 0, list: []});
      setCachedHosts({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedSummaryAndAlerts();

      var now = clone(oneEmptyHost);

      unified.__sendUnifiedNotifications(now);

      validateSummaryEvent(summaryEvent, { hosts: { up: 0, down: 0, unknown: 0, partial: 0, empty: 1 } });
      validateEventWasNotReceived('alerts', alertsEvent);
      validateHostsEvent(hostsEvent, { up: 0, down: 0, unknown: 0, partial: 0, empty: 1, added: ['localhost'] });
      validateEventWasNotReceived('servers', serversEvent);
      validateEventWasNotReceived('clusters', clustersEvent);
    });

    /**
     * Removing the last (empty) host from the collective should produce the following events:
     * - hosts - updated tallies and 1 remove host
     * - host event - update state to indicate it was removed
     * 
     * No other events should be sent, because this host has no server, cluster, app or runtime
     */
    tdd.test('1b. Remove last empty host from the collective', function() {
      var summaryEvent = null;
      addTopicListener(topicUtil.getTopicByType('summary'),  function(e) {
        summaryEvent = e;
      });

      var alertsEvent = null;
      addTopicListener(topicUtil.getTopicByType('alerts'),  function(e) {
        alertsEvent = e;
      });

      var hostsEvent = null;
      addTopicListener(topicUtil.getTopicByType('hosts'),  function(e) {
        hostsEvent = e;
      });

      var hostEvent = null;
      addTopicListener(topicUtil.getTopicByType('host', 'localhost'),  function(e) {
        hostEvent = e;
      });

      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      var clustersEvent = null;
      addTopicListener(topicUtil.getTopicByType('clusters'),  function(e) {
        clustersEvent = e;
      });

      var runtimesEvent = null;
      addTopicListener(topicUtil.getTopicByType('runtimes'),  function(e) {
        runtimesEvent = e;
      });

      // cached = oneEmptyHost
      setCachedApplications({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedClusters({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedServers({ up: 0, down: 0, unknown: 0, list: []});
      setCachedHosts({ up: 0, down: 0, unknown: 0, partial: 0, empty: 1, list: ['localhost']});
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedSummaryAndAlerts();
      setCachedHost({ id: 'localhost', type: 'host', name: 'localhost', state: 'STOPPED', runtimes: [], servers: {up: 0, down: 0, unknown: 0, list: []}});

      var now = clone(emptyCollective);

      unified.__sendUnifiedNotifications(now);

      validateSummaryEvent(summaryEvent, { hosts: { up: 0, down: 0, unknown: 0, partial: 0, empty: 0 } });
      validateEventWasNotReceived('alerts', alertsEvent);
      validateHostsEvent(hostsEvent, { up: 0, down: 0, unknown: 0, partial: 0, empty: 0, removed: ['localhost'] });
      validateEventWasNotReceived('servers', serversEvent);
      validateEventWasNotReceived('clusters', clustersEvent);

      validateHostEvent('localhost', hostEvent, { state: 'removed' });
    });

    /**
     * Adding 2 new empty hosts to an empty collective should produce the following events:
     * - hosts - updated tallies and 2 added host
     * 
     * No other events should be sent because this host has no server, cluster, app or runtime.
     */
    tdd.test('2a. Add 2 new empty hosts to an empty collective', function() {
      var summaryEvent = null;
      addTopicListener(topicUtil.getTopicByType('summary'),  function(e) {
        summaryEvent = e;
      });

      var alertsEvent = null;
      addTopicListener(topicUtil.getTopicByType('alerts'),  function(e) {
        alertsEvent = e;
      });

      var hostsEvent = null;
      addTopicListener(topicUtil.getTopicByType('hosts'),  function(e) {
        hostsEvent = e;
      });

      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      var clustersEvent = null;
      addTopicListener(topicUtil.getTopicByType('clusters'),  function(e) {
        clustersEvent = e;
      });

      var runtimesEvent = null;
      addTopicListener(topicUtil.getTopicByType('runtimes'),  function(e) {
        runtimesEvent = e;
      });

      // cached = emptyCollective
      setCachedApplications({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedClusters({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedServers({ up: 0, down: 0, unknown: 0, list: []});
      setCachedHosts({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedSummaryAndAlerts();

      var now = clone(twoEmptyHosts);

      unified.__sendUnifiedNotifications(now);

      validateSummaryEvent(summaryEvent, { hosts: { up: 0, down: 0, unknown: 0, partial: 0, empty: 2 } });
      validateEventWasNotReceived('alerts', alertsEvent);
      validateHostsEvent(hostsEvent, { up: 0, down: 0, unknown: 0, partial: 0, empty: 2, added: ['localhost', 'emptyhost'] });
      validateEventWasNotReceived('servers', serversEvent);
      validateEventWasNotReceived('clusters', clustersEvent);
    });

    /**
     * Removing the last 2 (empty) host from the collective should produce the following events:
     * - hosts - updated tallies and 2 remove host
     * - host events x2 - update state to indicate it was removed
     * 
     * No other events should be sent, because this host has no server, cluster, app or runtime
     */
    tdd.test('2b. Remove 2 empty host from the collective, ending in an empty collective', function() {
      var summaryEvent = null;
      addTopicListener(topicUtil.getTopicByType('summary'),  function(e) {
        summaryEvent = e;
      });

      var alertsEvent = null;
      addTopicListener(topicUtil.getTopicByType('alerts'),  function(e) {
        alertsEvent = e;
      });

      var hostsEvent = null;
      addTopicListener(topicUtil.getTopicByType('hosts'),  function(e) {
        hostsEvent = e;
      });
      var hostEvent1 = null;
      addTopicListener(topicUtil.getTopicByType('host', 'localhost'),  function(e) {
        hostEvent1 = e;
      });
      var hostEvent2 = null;
      addTopicListener(topicUtil.getTopicByType('host', 'emptyhost'),  function(e) {
        hostEvent2 = e;
      });

      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      var clustersEvent = null;
      addTopicListener(topicUtil.getTopicByType('clusters'),  function(e) {
        clustersEvent = e;
      });

      var runtimesEvent = null;
      addTopicListener(topicUtil.getTopicByType('runtimes'),  function(e) {
        runtimesEvent = e;
      });

      // cached = twoEmptyHosts
      setCachedApplications({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedClusters({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedServers({ up: 0, down: 0, unknown: 0, list: []});
      setCachedHosts({ up: 0, down: 0, unknown: 0, partial: 0, empty: 2, list: ['localhost', 'emptyhost']});
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedSummaryAndAlerts();
      setCachedHost({ id: 'localhost', type: 'host', name: 'localhost', state: 'STOPPED', runtimes: [], servers: {up: 0, down: 0, unknown: 0, list: []}});
      setCachedHost({ id: 'emptyhost', type: 'host', name: 'emptyhost', state: 'STOPPED', runtimes: [], servers: {up: 0, down: 0, unknown: 0, list: []}});

      var now = clone(emptyCollective);

      unified.__sendUnifiedNotifications(now);

      validateSummaryEvent(summaryEvent, { hosts: { up: 0, down: 0, unknown: 0, partial: 0, empty: 0 } });
      validateEventWasNotReceived('alerts', alertsEvent);
      validateHostsEvent(hostsEvent, { up: 0, down: 0, unknown: 0, partial: 0, empty: 0, removed: ['localhost', 'emptyhost'] });
      validateEventWasNotReceived('servers', serversEvent);
      validateEventWasNotReceived('clusters', clustersEvent);

      validateHostEvent('localhost', hostEvent1, { state: 'removed' });
      validateHostEvent('emptyhost', hostEvent2, { state: 'removed' });
    });

    /**
     * Adding a new host with a started server to an empty collective should produce the following events:
     * - hosts - updated tallies and 1 added host
     * - servers - updated tallies and 1 added server
     * 
     * No other events should be sent because this host has no cluster or app.
     */
    tdd.test('3a. Add a new host with a started server to an empty collective', function() {
      var summaryEvent = null;
      addTopicListener(topicUtil.getTopicByType('summary'),  function(e) {
        summaryEvent = e;
      });

      var alertsEvent = null;
      addTopicListener(topicUtil.getTopicByType('alerts'),  function(e) {
        alertsEvent = e;
      });

      var hostsEvent = null;
      addTopicListener(topicUtil.getTopicByType('hosts'),  function(e) {
        hostsEvent = e;
      });

      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      // cached = emptyCollective
      setCachedApplications({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedClusters({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedServers({ up: 0, down: 0, unknown: 0, list: []});
      setCachedHosts({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedSummaryAndAlerts();

      var now = clone(oneRunningServerWithNoApps);

      unified.__sendUnifiedNotifications(now);

      validateSummaryEvent(summaryEvent, { servers: { up: 1, down: 0, unknown: 0 }, hosts: { up: 1, down: 0, unknown: 0, partial: 0, empty: 0 } });
      validateEventWasNotReceived('alerts', alertsEvent);
      validateHostsEvent(hostsEvent, { up: 1, down: 0, unknown: 0, partial: 0, empty: 0, added: ['localhost'] });
      validateServersEvent(serversEvent, { up: 1, down: 0, unknown: 0, added: ['localhost,/wlp/usr,server1'] });
    });

    /**
     * Removes the last server and host from the collective, leaving it empty. This should produce the following events:
     * - hosts - updated tallies and 1 removed host
     * - host event - update state to indicate it was removed
     * - servers - updated tallies and 1 removed server
     * - server event - update state to indicate it was removed
     * 
     * No other events should be sent because this host has no cluster or app.
     */
    tdd.test('3b. Remove started server and host to leave an empty collective', function() {
      var summaryEvent = null;
      addTopicListener(topicUtil.getTopicByType('summary'),  function(e) {
        summaryEvent = e;
      });

      var alertsEvent = null;
      addTopicListener(topicUtil.getTopicByType('alerts'),  function(e) {
        alertsEvent = e;
      });

      var hostsEvent = null;
      addTopicListener(topicUtil.getTopicByType('hosts'),  function(e) {
        hostsEvent = e;
      });

      var hostEvent = null;
      addTopicListener(topicUtil.getTopicByType('host', 'localhost'),  function(e) {
        hostEvent = e;
      });

      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      var serverEvent = null;
      addTopicListener(topicUtil.getTopicByType('server', 'localhost,/wlp/usr,server1'),  function(e) {
        serverEvent = e;
      });

      // cached = oneRunningServerWithNoApps
      setCachedApplications({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedClusters({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedServers({ up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']});
      setCachedHosts({ up: 1, down: 0, unknown: 0, partial: 0, empty: 0, list: ['localhost']});
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedSummaryAndAlerts();

      setCachedHost({ id: 'localhost', type: 'host', name: 'localhost', state: 'STARTED', runtimes: [{id:'localhost,/wlp', name:'localhost,/wlp', type:'runtime'}], servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}});
      setCachedServer({ id: 'localhost,/wlp/usr,server1', type: 'server', name: 'localhost,/wlp/usr,server1', state: 'STARTED', apps: { up: 0, down: 0, unknown: 0, list: []}});

      var now = clone(emptyCollective);

      unified.__sendUnifiedNotifications(now);

      validateSummaryEvent(summaryEvent, { servers: { up: 0, down: 0, unknown: 0 }, hosts: { up: 0, down: 0, unknown: 0, partial: 0, empty: 0 } });
      validateEventWasNotReceived('alerts', alertsEvent);
      validateHostsEvent(hostsEvent, { up: 0, down: 0, unknown: 0, partial: 0, empty: 0, removed: ['localhost'] });
      validateServersEvent(serversEvent, { up: 0, down: 0, unknown: 0, removed: ['localhost,/wlp/usr,server1'] });
      validateHostEvent('localhost', hostEvent, { state: 'removed' });
      validateServerEvent('localhost,/wlp/usr,server1', serverEvent, { state: 'removed' });
    });

    /**
     * Adding a new host with a stopped server to an empty collective should produce the following events:
     * - hosts - updated tallies and 1 added host
     * - servers - updated tallies and 1 added server
     * 
     * No other events should be sent because this host has no cluster or app.
     */
    tdd.test('3c. Add a new host with a stopped server to an empty collective', function() {
      var summaryEvent = null;
      addTopicListener(topicUtil.getTopicByType('summary'),  function(e) {
        summaryEvent = e;
      });

      var alertsEvent = null;
      addTopicListener(topicUtil.getTopicByType('alerts'),  function(e) {
        alertsEvent = e;
      });

      var hostsEvent = null;
      addTopicListener(topicUtil.getTopicByType('hosts'),  function(e) {
        hostsEvent = e;
      });

      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      // cached = emptyCollective
      setCachedApplications({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedClusters({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedServers({ up: 0, down: 0, unknown: 0, list: []});
      setCachedHosts({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedSummaryAndAlerts();

      var now = clone(oneStoppedServerWithNoApps);

      unified.__sendUnifiedNotifications(now);

      validateSummaryEvent(summaryEvent, { servers: { up: 0, down: 1, unknown: 0 }, hosts: { up: 0, down: 1, unknown: 0, partial: 0, empty: 0 } });
      validateEventWasNotReceived('alerts', alertsEvent);
      validateHostsEvent(hostsEvent, { up: 0, down: 1, unknown: 0, partial: 0, empty: 0, added: ['localhost'] });
      validateServersEvent(serversEvent, { up: 0, down: 1, unknown: 0, added: ['localhost,/wlp/usr,server1'] });
    });

    /**
     * A server changing its state should produce the following events:
     * - hosts - updated tallies
     * - servers - updated tallies
     * - host - changed servers tallies event
     * - server - changed state event
     * 
     * No other events should be sent because this host has no cluster or app.
     */
    tdd.test('4a. Single server changes state to down', function() {
      var summaryEvent = null;
      addTopicListener(topicUtil.getTopicByType('summary'),  function(e) {
        summaryEvent = e;
      });

      var alertsEvent = null;
      addTopicListener(topicUtil.getTopicByType('alerts'),  function(e) {
        alertsEvent = e;
      });

      var hostsEvent = null;
      addTopicListener(topicUtil.getTopicByType('hosts'),  function(e) {
        hostsEvent = e;
      });

      var hostEvent = null;
      addTopicListener(topicUtil.getTopicByType('host', 'localhost'),  function(e) {
        hostEvent = e;
      });

      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      var serverEvent = null;
      addTopicListener(topicUtil.getTopicByType('server', 'localhost,/wlp/usr,server1'),  function(e) {
        serverEvent = e;
      });

      // cached = oneRunningServerWithNoApps
      setCachedApplications({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedClusters({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedServers({ up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']});
      setCachedHosts({ up: 1, down: 0, unknown: 0, partial: 0, empty: 0, list: ['localhost']});
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedSummaryAndAlerts();
      setCachedHost({ id: 'localhost', type: 'host', name: 'localhost', state: 'STARTED', runtimes: {list: [{id:'localhost,/wlp', name:'localhost,/wlp', type:'runtime'}]}, servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}});
      setCachedServer({ id: 'localhost,/wlp/usr,server1', type: 'server', name: 'localhost,/wlp/usr,server1', state: 'STARTED', wlpInstallDir: '/wlp', apps: { up: 0, down: 0, unknown: 0, list: []}});

      var now = clone(oneStoppedServerWithNoApps);

      unified.__sendUnifiedNotifications(now);

      validateSummaryEvent(summaryEvent, { servers: { up: 0, down: 1, unknown: 0 }, hosts: { up: 0, down: 1, unknown: 0, partial: 0, empty: 0 } });
      validateEventWasNotReceived('alerts', alertsEvent);
      validateHostsEvent(hostsEvent, { up: 0, down: 1, unknown: 0, partial: 0, empty: 0 });
      validateServersEvent(serversEvent, { up: 0, down: 1, unknown: 0 });

      validateHostEvent('localhost', hostEvent, { servers: { up: 0, down: 1, unknown: 0 } });
      validateServerEvent('localhost,/wlp/usr,server1', serverEvent, { state: 'STOPPED' });
    });

    /**
     * A server changing its state should produce the following events:
     * - hosts - updated tallies
     * - servers - updated tallies
     * - host - changed servers tallies event
     * - server - changed state event
     * 
     * No other events should be sent because this host has no cluster or app.
     */
    tdd.test('4b. Single server changes state to up', function() {
      var summaryEvent = null;
      addTopicListener(topicUtil.getTopicByType('summary'),  function(e) {
        summaryEvent = e;
      });

      var alertsEvent = null;
      addTopicListener(topicUtil.getTopicByType('alerts'),  function(e) {
        alertsEvent = e;
      });

      var hostsEvent = null;
      addTopicListener(topicUtil.getTopicByType('hosts'),  function(e) {
        hostsEvent = e;
      });

      var hostEvent = null;
      addTopicListener(topicUtil.getTopicByType('host', 'localhost'),  function(e) {
        hostEvent = e;
      });

      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      var serverEvent = null;
      addTopicListener(topicUtil.getTopicByType('server', 'localhost,/wlp/usr,server1'),  function(e) {
        serverEvent = e;
      });

      // cached = oneStoppedServerWithNoApps
      setCachedApplications({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedClusters({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedServers({ up: 0, down: 1, unknown: 0, list: ['localhost,/wlp/usr,server1']});
      setCachedHosts({ up: 0, down: 1, unknown: 0, partial: 0, empty: 0, list: ['localhost']});
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedSummaryAndAlerts();
      setCachedHost({ id: 'localhost', type: 'host', name: 'localhost', state: 'STOPPED', runtimes: {list: [{id:'localhost,/wlp', name:'localhost,/wlp', type:'runtime'}]}, servers: {up: 0, down: 1, unknown: 0, list: ['localhost,/wlp/usr,server1']}});
      setCachedServer({ id: 'localhost,/wlp/usr,server1', type: 'server', name: 'localhost,/wlp/usr,server1', state: 'STOPPED', wlpInstallDir: '/wlp', apps: { up: 0, down: 0, unknown: 0, list: []}});

      var now = clone(oneRunningServerWithNoApps);

      unified.__sendUnifiedNotifications(now);

      validateSummaryEvent(summaryEvent, { servers: { up: 1, down: 0, unknown: 0 }, hosts: { up: 1, down: 0, unknown: 0, partial: 0, empty: 0 } });
      validateEventWasNotReceived('alerts', alertsEvent);
      validateHostsEvent(hostsEvent, { up: 1, down: 0, unknown: 0, partial: 0, empty: 0 });
      validateServersEvent(serversEvent, { up: 1, down: 0, unknown: 0 });

      validateHostEvent('localhost', hostEvent, { servers: { up: 1, down: 0, unknown: 0 } });
      validateServerEvent('localhost,/wlp/usr,server1', serverEvent, { state: 'STARTED' });
    });

    /**
     * Adding a new server to an existing host should produce the following events:
     * - host event - update the servers which are on this host
     * - servers - updated tallies and 1 added server
     * 
     * No other events should be sent because the hosts have not changed, and there are no clusters or apps.
     */
    tdd.test('5a. Add new running server to existing host', function() {
      var summaryEvent = null;
      addTopicListener(topicUtil.getTopicByType('summary'),  function(e) {
        summaryEvent = e;
      });

      var alertsEvent = null;
      addTopicListener(topicUtil.getTopicByType('alerts'),  function(e) {
        alertsEvent = e;
      });

      var hostsEvent = null;
      addTopicListener(topicUtil.getTopicByType('hosts'),  function(e) {
        hostsEvent = e;
      });

      var hostEvent = null;
      addTopicListener(topicUtil.getTopicByType('host', 'localhost'),  function(e) {
        hostEvent = e;
      });

      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      // cached = oneRunningServerWithNoApps
      setCachedApplications({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedClusters({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedServers({ up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']});
      setCachedHosts({ up: 1, down: 0, unknown: 0, partial: 0, empty: 0, list: ['localhost']});
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedSummaryAndAlerts();
      setCachedHost({ id: 'localhost', type: 'host', name: 'localhost', state: 'STARTED', runtimes: {list: [{id:'localhost,/wlp', name:'localhost,/wlp', type:'runtime'}]}, servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}});
      setCachedServer({ id: 'localhost,/wlp/usr,server1', type: 'server', name: 'localhost,/wlp/usr,server1', state: 'STARTED', wlpInstallDir: '/wlp', apps: { up: 0, down: 0, unknown: 0, list: []}});

      var now = clone(twoRunningServersWithNoAppsOnLocalhost);

      unified.__sendUnifiedNotifications(now);

      validateSummaryEvent(summaryEvent, { servers: { up: 2, down: 0, unknown: 0 } });
      validateEventWasNotReceived('alerts', alertsEvent);
      validateEventWasNotReceived('hosts', hostsEvent);
      validateServersEvent(serversEvent, { up: 2, down: 0, unknown: 0, added: ['localhost,/wlp/usr,server2'] });

      validateHostEvent('localhost', hostEvent, { servers: { up: 2, down: 0, unknown: 0, added: ['localhost,/wlp/usr,server2'] } });
    });

    /**
     * Remove 1 server from an existing host but leaves at least 1 server should produce the following events:
     * - servers - updated tallies and 1 removed server
     * - server event - state changed to removed
     * 
     * No other events should be sent because the hosts have not changed, and there are no clusters or apps.
     */
    tdd.test('5b. Remove 1 running server from collective', function() {
      var summaryEvent = null;
      addTopicListener(topicUtil.getTopicByType('summary'),  function(e) {
        summaryEvent = e;
      });

      var alertsEvent = null;
      addTopicListener(topicUtil.getTopicByType('alerts'),  function(e) {
        alertsEvent = e;
      });

      var hostsEvent = null;
      addTopicListener(topicUtil.getTopicByType('hosts'),  function(e) {
        hostsEvent = e;
      });

      var hostEvent = null;
      addTopicListener(topicUtil.getTopicByType('host', 'localhost'),  function(e) {
        hostEvent = e;
      });

      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      var serverEvent = null;
      addTopicListener(topicUtil.getTopicByType('server', 'localhost,/wlp/usr,server2'),  function(e) {
        serverEvent = e;
      });

      // cached = twoRunningServersWithNoAppsOnLocalhost
      setCachedApplications({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedClusters({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedServers({ up: 2, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1','localhost,/wlp/usr,server2']});
      setCachedHosts({ up: 1, down: 0, unknown: 0, partial: 0, empty: 0, list: ['localhost']});
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedSummaryAndAlerts();
      setCachedHost({ id: 'localhost', type: 'host', name: 'localhost', state: 'STARTED', runtimes: {list: [{id:'localhost,/wlp', name:'localhost,/wlp', type:'runtime'}]}, servers: {up: 2, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1','localhost,/wlp/usr,server2']}});
      setCachedServer({ id: 'localhost,/wlp/usr,server1', type: 'server', name: 'localhost,/wlp/usr,server1', state: 'STARTED', wlpInstallDir: '/wlp', apps: { up: 0, down: 0, unknown: 0, list: []}});
      setCachedServer({ id: 'localhost,/wlp/usr,server2', type: 'server', name: 'localhost,/wlp/usr,server2', state: 'STARTED', wlpInstallDir: '/wlp', apps: { up: 0, down: 0, unknown: 0, list: []}});

      var now = clone(oneRunningServerWithNoApps);

      unified.__sendUnifiedNotifications(now);

      validateSummaryEvent(summaryEvent, { servers: { up: 1, down: 0, unknown: 0 } });
      validateEventWasNotReceived('alerts', alertsEvent);
      validateEventWasNotReceived('hosts', hostsEvent);
      validateServersEvent(serversEvent, { up: 1, down: 0, unknown: 0, removed: ['localhost,/wlp/usr,server2'] });

      validateHostEvent('localhost', hostEvent, { servers: { up: 1, down: 0, unknown: 0, removed: ['localhost,/wlp/usr,server2'] } });
      validateServerEvent('localhost,/wlp/usr,server2', serverEvent, { state: 'removed' });
    });

    /**
     * Removes the last 2 servers and host from the collective, leaving it empty. This should produce the following events:
     * - hosts - updated tallies and 1 removed host
     * - host event - update state to indicate it was removed
     * - servers - updated tallies and 1 removed server
     * - server event x2 - update state to indicate it was removed
     * 
     * No other events should be sent because this host has no cluster or app.
     */
    tdd.test('5c. Remove 2 started servers and host to leave an empty collective', function() {
      var summaryEvent = null;
      addTopicListener(topicUtil.getTopicByType('summary'),  function(e) {
        summaryEvent = e;
      });

      var alertsEvent = null;
      addTopicListener(topicUtil.getTopicByType('alerts'),  function(e) {
        alertsEvent = e;
      });

      var hostsEvent = null;
      addTopicListener(topicUtil.getTopicByType('hosts'),  function(e) {
        hostsEvent = e;
      });

      var hostEvent = null;
      addTopicListener(topicUtil.getTopicByType('host', 'localhost'),  function(e) {
        hostEvent = e;
      });

      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      var serverEvent1 = null;
      addTopicListener(topicUtil.getTopicByType('server', 'localhost,/wlp/usr,server1'),  function(e) {
        serverEvent1 = e;
      });

      var serverEvent2 = null;
      addTopicListener(topicUtil.getTopicByType('server', 'localhost,/wlp/usr,server2'),  function(e) {
        serverEvent2 = e;
      });

      // cached = twoRunningServersWithNoAppsOnLocalhost
      setCachedApplications({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedClusters({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedServers({ up: 2, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1','localhost,/wlp/usr,server2']});
      setCachedHosts({ up: 1, down: 0, unknown: 0, partial: 0, empty: 0, list: ['localhost']});
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedSummaryAndAlerts();
      setCachedHost({ id: 'localhost', type: 'host', name: 'localhost', state: 'STARTED', runtimes: [{id:'localhost,/wlp', name:'localhost,/wlp', type:'runtime'}], servers: {up: 2, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1','localhost,/wlp/usr,server2']}});
      setCachedServer({ id: 'localhost,/wlp/usr,server1', type: 'server', name: 'localhost,/wlp/usr,server1', state: 'STARTED', wlpInstallDir: '/wlp', apps: { up: 0, down: 0, unknown: 0, list: []}});
      setCachedServer({ id: 'localhost,/wlp/usr,server2', type: 'server', name: 'localhost,/wlp/usr,server2', state: 'STARTED', wlpInstallDir: '/wlp', apps: { up: 0, down: 0, unknown: 0, list: []}});

      var now = clone(emptyCollective);

      unified.__sendUnifiedNotifications(now);

      validateSummaryEvent(summaryEvent, { servers: { up: 0, down: 0, unknown: 0 }, hosts: { up: 0, down: 0, unknown: 0, partial: 0, empty: 0 } });
      validateEventWasNotReceived('alerts', alertsEvent);
      validateHostsEvent(hostsEvent, { up: 0, down: 0, unknown: 0, partial: 0, empty: 0, removed: ['localhost'] });
      validateServersEvent(serversEvent, { up: 0, down: 0, unknown: 0, removed: ['localhost,/wlp/usr,server1', 'localhost,/wlp/usr,server2'] });

      validateHostEvent('localhost', hostEvent, { state: 'removed' });
      validateServerEvent('localhost,/wlp/usr,server1', serverEvent1, { state: 'removed' });
      validateServerEvent('localhost,/wlp/usr,server2', serverEvent2, { state: 'removed' });
    });

    /**
     * Adding a new clustered server to an empty collective should produce the following events:
     * - hosts - updated tallies and 1 added host
     * - servers - updated tallies and 1 added server
     * - clusters - updated tallies and 1 added cluster
     */
    tdd.test('6a. Add new running clustered server to an empty collective', function() {
      var summaryEvent = null;
      addTopicListener(topicUtil.getTopicByType('summary'),  function(e) {
        summaryEvent = e;
      });

      var alertsEvent = null;
      addTopicListener(topicUtil.getTopicByType('alerts'),  function(e) {
        alertsEvent = e;
      });

      var hostsEvent = null;
      addTopicListener(topicUtil.getTopicByType('hosts'),  function(e) {
        hostsEvent = e;
      });

      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      var clustersEvent = null;
      addTopicListener(topicUtil.getTopicByType('clusters'),  function(e) {
        clustersEvent = e;
      });

      // cached = emptyCollective
      setCachedApplications({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedClusters({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedServers({ up: 0, down: 0, unknown: 0, list: []});
      setCachedHosts({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedSummaryAndAlerts();

      var now = clone(oneRunningClusteredServerWithNoApps);

      unified.__sendUnifiedNotifications(now);

      validateSummaryEvent(summaryEvent, { clusters: { up: 1, down: 0, unknown: 0, partial: 0 }, servers: { up: 1, down: 0, unknown: 0 }, hosts: { up: 1, down: 0, unknown: 0, partial: 0, empty: 0 } });
      validateEventWasNotReceived('alerts', alertsEvent);
      validateHostsEvent(hostsEvent, { up: 1, down: 0, unknown: 0, partial: 0, empty: 0, added: ['localhost'] });
      validateServersEvent(serversEvent, { up: 1, down: 0, unknown: 0, added: ['localhost,/wlp/usr,server1'] });
      validateClustersEvent(clustersEvent, { up: 1, down: 0, unknown: 0, partial: 0, added: ['cluster1'] });
    });

    /**
     * Removing the last clustered server to become an empty collective should produce the following events:
     * - hosts - updated tallies and 1 removed host
     * - servers - updated tallies and 1 removed server
     * - server event - update state to be removed
     * - clusters - updated tallies and 1 removed cluster
     * - cluster event - update state to be removed
     */
    tdd.test('6b. Remove last running clustered server to become an empty collective', function() {
      var summaryEvent = null;
      addTopicListener(topicUtil.getTopicByType('summary'),  function(e) {
        summaryEvent = e;
      });

      var alertsEvent = null;
      addTopicListener(topicUtil.getTopicByType('alerts'),  function(e) {
        alertsEvent = e;
      });

      var hostsEvent = null;
      addTopicListener(topicUtil.getTopicByType('hosts'),  function(e) {
        hostsEvent = e;
      });

      var hostEvent = null;
      addTopicListener(topicUtil.getTopicByType('host', 'localhost'),  function(e) {
        hostEvent = e;
      });

      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      var serverEvent = null;
      addTopicListener(topicUtil.getTopicByType('server', 'localhost,/wlp/usr,server1'),  function(e) {
        serverEvent = e;
      });

      var clustersEvent = null;
      addTopicListener(topicUtil.getTopicByType('clusters'),  function(e) {
        clustersEvent = e;
      });

      var clusterEvent = null;
      addTopicListener(topicUtil.getTopicByType('cluster', 'cluster1'),  function(e) {
        clusterEvent = e;
      });

      // cached = oneRunningClusteredServerWithNoApps
      setCachedApplications({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedClusters({ up: 1, down: 0, unknown: 0, partial: 0, list: ['cluster1']});
      setCachedServers({ up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']});
      setCachedHosts({ up: 1, down: 0, unknown: 0, partial: 0, empty: 0, list: ['localhost']});
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedSummaryAndAlerts();
      setCachedHost({ id: 'localhost', type: 'host', name: 'localhost', state: 'STARTED', runtimes: [{id:'localhost,/wlp', name:'localhost,/wlp', type:'runtime'}], servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}});
      setCachedServer({ id: 'localhost,/wlp/usr,server1', type: 'server', name: 'localhost,/wlp/usr,server1', state: 'STARTED', wlpInstallDir: '/wlp', cluster: 'cluster1', apps: { up: 0, down: 0, unknown: 0, list: []}});
      setCachedCluster({ id: 'cluster1', type: 'cluster', name: 'cluster1', state: 'STARTED', servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, apps: { up: 0, down: 0, unknown: 0, list: []}});

      var now = clone(emptyCollective);

      unified.__sendUnifiedNotifications(now);

      validateSummaryEvent(summaryEvent, { clusters: { up: 0, down: 0, unknown: 0, partial: 0 }, servers: { up: 0, down: 0, unknown: 0 }, hosts: { up: 0, down: 0, unknown: 0, partial: 0, empty: 0 } });
      validateEventWasNotReceived('alerts', alertsEvent);
      validateHostsEvent(hostsEvent, { up: 0, down: 0, unknown: 0, partial: 0, empty: 0, removed: ['localhost'] });
      validateServersEvent(serversEvent, { up: 0, down: 0, unknown: 0, removed: ['localhost,/wlp/usr,server1'] });
      validateClustersEvent(clustersEvent, { up: 0, down: 0, unknown: 0, partial: 0, removed: ['cluster1'] });

      validateHostEvent('localhost', hostEvent, { state: 'removed' });
      validateServerEvent('localhost,/wlp/usr,server1', serverEvent, { state: 'removed' });
      validateClusterEvent('cluster1', clusterEvent, { state: 'removed' });
    });

    /**
     * Adding 2 new clustered servers to an empty collective should produce the following events:
     * - hosts - updated tallies and 1 added host
     * - servers - updated tallies and 2 added servers
     * - clusters - updated tallies and 2 added clusters
     */
    tdd.test('7a. Add 2 new running clustered servers to an empty collective', function() {
      var summaryEvent = null;
      addTopicListener(topicUtil.getTopicByType('summary'),  function(e) {
        summaryEvent = e;
      });

      var alertsEvent = null;
      addTopicListener(topicUtil.getTopicByType('alerts'),  function(e) {
        alertsEvent = e;
      });

      var hostsEvent = null;
      addTopicListener(topicUtil.getTopicByType('hosts'),  function(e) {
        hostsEvent = e;
      });

      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      var clustersEvent = null;
      addTopicListener(topicUtil.getTopicByType('clusters'),  function(e) {
        clustersEvent = e;
      });

      // cached = emptyCollective
      setCachedApplications({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedClusters({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedServers({ up: 0, down: 0, unknown: 0, list: []});
      setCachedHosts({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedSummaryAndAlerts();

      var now = clone(twoRunningClusteredServerWithNoApps);

      unified.__sendUnifiedNotifications(now);

      validateSummaryEvent(summaryEvent, { clusters: { up: 2, down: 0, unknown: 0, partial: 0 }, servers: { up: 2, down: 0, unknown: 0 }, hosts: { up: 1, down: 0, unknown: 0, partial: 0, empty: 0 } });
      validateEventWasNotReceived('alerts', alertsEvent);
      validateHostsEvent(hostsEvent, { up: 1, down: 0, unknown: 0, partial: 0, empty: 0, added: ['localhost'] });
      validateServersEvent(serversEvent, { up: 2, down: 0, unknown: 0, added: ['localhost,/wlp/usr,server1', 'localhost,/wlp/usr,server2'] });
      validateClustersEvent(clustersEvent, { up: 2, down: 0, unknown: 0, partial: 0, added: ['cluster1', 'cluster2'] });
    });

    /**
     * Adding 2 new clustered servers to an empty collective should produce the following events:
     * - hosts - updated tallies and 1 added host
     * - servers - updated tallies and 2 added servers
     * - clusters - updated tallies and 2 added clusters
     */
    tdd.test('7b. Remove 2 running clustered servers to become an empty collective', function() {
      var summaryEvent = null;
      addTopicListener(topicUtil.getTopicByType('summary'),  function(e) {
        summaryEvent = e;
      });

      var alertsEvent = null;
      addTopicListener(topicUtil.getTopicByType('alerts'),  function(e) {
        alertsEvent = e;
      });

      var hostsEvent = null;
      addTopicListener(topicUtil.getTopicByType('hosts'),  function(e) {
        hostsEvent = e;
      });

      var hostEvent = null;
      addTopicListener(topicUtil.getTopicByType('host', 'localhost'),  function(e) {
        hostEvent = e;
      });

      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      var serverEvent1 = null;
      addTopicListener(topicUtil.getTopicByType('server', 'localhost,/wlp/usr,server1'),  function(e) {
        serverEvent1 = e;
      });

      var serverEvent2 = null;
      addTopicListener(topicUtil.getTopicByType('server', 'localhost,/wlp/usr,server2'),  function(e) {
        serverEvent2 = e;
      });

      var clustersEvent = null;
      addTopicListener(topicUtil.getTopicByType('clusters'),  function(e) {
        clustersEvent = e;
      });

      var clusterEvent1 = null;
      addTopicListener(topicUtil.getTopicByType('cluster', 'cluster1'),  function(e) {
        clusterEvent1 = e;
      });

      var clusterEvent2 = null;
      addTopicListener(topicUtil.getTopicByType('cluster', 'cluster2'),  function(e) {
        clusterEvent2 = e;
      });

      // cached = twoRunningClusteredServerWithNoApps
      setCachedApplications({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedClusters({ up: 2, down: 0, unknown: 0, partial: 0, list: ['cluster1','cluster2']});
      setCachedServers({ up: 2, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1','localhost,/wlp/usr,server2']});
      setCachedHosts({ up: 1, down: 0, unknown: 0, partial: 0, empty: 0, list: ['localhost']});
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedSummaryAndAlerts();
      setCachedHost({ id: 'localhost', type: 'host', name: 'localhost', state: 'STARTED', runtimes: [{id:'localhost,/wlp', name:'localhost,/wlp', type:'runtime'}], servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1','localhost,/wlp/usr,server2']}});
      setCachedServer({ id: 'localhost,/wlp/usr,server1', type: 'server', name: 'localhost,/wlp/usr,server1', state: 'STARTED', wlpInstallDir: '/wlp', cluster: 'cluster1', apps: { up: 0, down: 0, unknown: 0, list: []}});
      setCachedServer({ id: 'localhost,/wlp/usr,server2', type: 'server', name: 'localhost,/wlp/usr,server2', state: 'STARTED', wlpInstallDir: '/wlp', cluster: 'cluster2', apps: { up: 0, down: 0, unknown: 0, list: []}});
      setCachedCluster({ id: 'cluster1', type: 'cluster', name: 'cluster1', state: 'STARTED', servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, apps: { up: 0, down: 0, unknown: 0, partial: 0, list: []}});
      setCachedCluster({ id: 'cluster2', type: 'cluster', name: 'cluster2', state: 'STARTED', servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server2']}, apps: { up: 0, down: 0, unknown: 0, partial: 0, list: []}});

      var now = clone(emptyCollective);

      unified.__sendUnifiedNotifications(now);

      validateSummaryEvent(summaryEvent, { clusters: { up: 0, down: 0, unknown: 0, partial: 0 }, servers: { up: 0, down: 0, unknown: 0 }, hosts: { up: 0, down: 0, unknown: 0, partial: 0, empty: 0 } });
      validateEventWasNotReceived('alerts', alertsEvent);
      validateHostsEvent(hostsEvent, { up: 0, down: 0, unknown: 0, partial: 0, empty: 0, removed: ['localhost'] });
      validateServersEvent(serversEvent, { up: 0, down: 0, unknown: 0, removed: ['localhost,/wlp/usr,server1', 'localhost,/wlp/usr,server2'] });
      validateClustersEvent(clustersEvent, { up: 0, down: 0, unknown: 0, partial: 0, removed: ['cluster1', 'cluster2'] });

      validateHostEvent('localhost', hostEvent, { state: 'removed' });
      validateServerEvent('localhost,/wlp/usr,server1', serverEvent1, { state: 'removed' });
      validateServerEvent('localhost,/wlp/usr,server2', serverEvent2, { state: 'removed' });
      validateClusterEvent('cluster1', clusterEvent1, { state: 'removed' });
      validateClusterEvent('cluster2', clusterEvent2, { state: 'removed' });
    });

    /**
     * Adding a new clustered server to an empty collective should produce the following events:
     * - hosts - updated tallies and 1 added host
     * - servers - updated tallies and 1 added server
     * - clusters - updated tallies and 1 added cluster
     */
    tdd.test('8a. Clustered server changes state to down', function() {
      var summaryEvent = null;
      addTopicListener(topicUtil.getTopicByType('summary'),  function(e) {
        summaryEvent = e;
      });

      var alertsEvent = null;
      addTopicListener(topicUtil.getTopicByType('alerts'),  function(e) {
        alertsEvent = e;
      });

      var hostsEvent = null;
      addTopicListener(topicUtil.getTopicByType('hosts'),  function(e) {
        hostsEvent = e;
      });

      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      var serverEvent = null;
      addTopicListener(topicUtil.getTopicByType('server', 'localhost,/wlp/usr,server1'),  function(e) {
        serverEvent = e;
      });

      var clustersEvent = null;
      addTopicListener(topicUtil.getTopicByType('clusters'),  function(e) {
        clustersEvent = e;
      });

      var clusterEvent = null;
      addTopicListener(topicUtil.getTopicByType('cluster', 'cluster1'),  function(e) {
        clusterEvent = e;
      });

      // cached = oneRunningClusteredServerWithNoApps
      setCachedApplications({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedClusters({ up: 1, down: 0, unknown: 0, partial: 0, list: ['cluster1']});
      setCachedServers({ up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']});
      setCachedHosts({ up: 1, down: 0, unknown: 0, partial: 0, empty: 0, list: ['localhost']});
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedSummaryAndAlerts();
      setCachedHost({ id: 'localhost', type: 'host', name: 'localhost', state: 'STARTED', runtimes: {list: [{id:'localhost,/wlp', name:'localhost,/wlp', type:'runtime'}]}, servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}});
      setCachedServer({ id: 'localhost,/wlp/usr,server1', type: 'server', name: 'localhost,/wlp/usr,server1', state: 'STARTED', wlpInstallDir: '/wlp', cluster: 'cluster1', apps: { up: 0, down: 0, unknown: 0, list: []}});
      setCachedCluster({ id: 'cluster1', type: 'cluster', name: 'cluster1', state: 'STARTED', servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, apps: { up: 0, down: 0, unknown: 0, partial: 0, list: []}});

      var now = clone(oneStoppedClusteredServerWithNoApps);

      unified.__sendUnifiedNotifications(now);

      validateSummaryEvent(summaryEvent, { clusters: { up: 0, down: 1, unknown: 0, partial: 0 }, servers: { up: 0, down: 1, unknown: 0 }, hosts: { up: 0, down: 1, unknown: 0, partial: 0, empty: 0 } });
      validateEventWasNotReceived('alerts', alertsEvent);
      validateHostsEvent(hostsEvent, { up: 0, down: 1, unknown: 0, partial: 0, empty: 0 });
      validateServersEvent(serversEvent, { up: 0, down: 1, unknown: 0 });
      validateClustersEvent(clustersEvent, { up: 0, down: 1, unknown: 0, partial: 0 });

      validateServerEvent('localhost,/wlp/usr,server1', serverEvent, { state: 'STOPPED' });
      validateClusterEvent('cluster1', clusterEvent, { state: 'STOPPED', servers: { up: 0, down: 1, unknown: 0 } });
    });

    /**
     * Adding a new clustered server to an empty collective should produce the following events:
     * - hosts - updated tallies and 1 added host
     * - servers - updated tallies and 1 added server
     * - clusters - updated tallies and 1 added cluster
     */
    tdd.test('8b. Clustered server changes state to up', function() {
      var summaryEvent = null;
      addTopicListener(topicUtil.getTopicByType('summary'),  function(e) {
        summaryEvent = e;
      });

      var alertsEvent = null;
      addTopicListener(topicUtil.getTopicByType('alerts'),  function(e) {
        alertsEvent = e;
      });

      var hostsEvent = null;
      addTopicListener(topicUtil.getTopicByType('hosts'),  function(e) {
        hostsEvent = e;
      });

      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      var serverEvent = null;
      addTopicListener(topicUtil.getTopicByType('server', 'localhost,/wlp/usr,server1'),  function(e) {
        serverEvent = e;
      });

      var clustersEvent = null;
      addTopicListener(topicUtil.getTopicByType('clusters'),  function(e) {
        clustersEvent = e;
      });

      var clusterEvent = null;
      addTopicListener(topicUtil.getTopicByType('cluster', 'cluster1'),  function(e) {
        clusterEvent = e;
      });

      // cached = oneStoppedClusteredServerWithNoApps
      setCachedApplications({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedClusters({ up: 0, down: 1, unknown: 0, partial: 0, list: ['cluster1']});
      setCachedServers({ up: 0, down: 1, unknown: 0, list: ['localhost,/wlp/usr,server1']});
      setCachedHosts({ up: 0, down: 1, unknown: 0, partial: 0, empty: 0, list: ['localhost']});
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedSummaryAndAlerts();
      setCachedHost({ id: 'localhost', type: 'host', name: 'localhost', state: 'STOPPED', runtimes: {list: [{id:'localhost,/wlp', name:'localhost,/wlp', type:'runtime'}]}, servers: {up: 0, down: 1, unknown: 0, list: ['localhost,/wlp/usr,server1']}});
      setCachedServer({ id: 'localhost,/wlp/usr,server1', type: 'server', name: 'localhost,/wlp/usr,server1', state: 'STOPPED', wlpInstallDir: '/wlp', cluster: 'cluster1', apps: { up: 0, down: 0, unknown: 0, list: []}});
      setCachedCluster({ id: 'cluster1', type: 'cluster', name: 'cluster1', state: 'STOPPED', servers: {up: 0, down: 1, unknown: 0, list: ['localhost,/wlp/usr,server1']}, apps: { up: 0, down: 0, unknown: 0, partial: 0, list: []}});

      var now = clone(oneRunningClusteredServerWithNoApps);

      unified.__sendUnifiedNotifications(now);

      validateSummaryEvent(summaryEvent, { clusters: { up: 1, down: 0, unknown: 0, partial: 0 }, servers: { up: 1, down: 0, unknown: 0 }, hosts: { up: 1, down: 0, unknown: 0, partial: 0, empty: 0 } });
      validateEventWasNotReceived('alerts', alertsEvent);
      validateHostsEvent(hostsEvent, { up: 1, down: 0, unknown: 0, partial: 0, empty: 0 });
      validateServersEvent(serversEvent, { up: 1, down: 0, unknown: 0 });
      validateClustersEvent(clustersEvent, { up: 1, down: 0, unknown: 0, partial: 0 });

      validateServerEvent('localhost,/wlp/usr,server1', serverEvent, { state: 'STARTED' });
      validateClusterEvent('cluster1', clusterEvent, { state: 'STARTED',  servers: { up: 1, down: 0, unknown: 0 } });
    });

    /**
     * Adding a scaling policy to a clustered server should produce the following events:
     * - server event - updated scaling policy
     * - cluster event - updated scaling policy
     */
    tdd.test('8c. Clustered server gets a scalingPolicy', function() {
      var summaryEvent = null;
      addTopicListener(topicUtil.getTopicByType('summary'),  function(e) {
        summaryEvent = e;
      });

      var alertsEvent = null;
      addTopicListener(topicUtil.getTopicByType('alerts'),  function(e) {
        alertsEvent = e;
      });

      var hostsEvent = null;
      addTopicListener(topicUtil.getTopicByType('hosts'),  function(e) {
        hostsEvent = e;
      });

      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      var serverEvent = null;
      addTopicListener(topicUtil.getTopicByType('server', 'localhost,/wlp/usr,server1'),  function(e) {
        serverEvent = e;
      });

      var clustersEvent = null;
      addTopicListener(topicUtil.getTopicByType('clusters'),  function(e) {
        clustersEvent = e;
      });

      var clusterEvent = null;
      addTopicListener(topicUtil.getTopicByType('cluster', 'cluster1'),  function(e) {
        clusterEvent = e;
      });

      // cached = oneRunningClusteredServerWithNoApps
      setCachedApplications({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedClusters({ up: 1, down: 0, unknown: 0, partial: 0, list: ['cluster1']});
      setCachedServers({ up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']});
      setCachedHosts({ up: 1, down: 0, unknown: 0, partial: 0, empty: 0, list: ['localhost']});
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedSummaryAndAlerts();
      setCachedHost({ id: 'localhost', type: 'host', name: 'localhost', state: 'STARTED', runtimes: {list: [{id:'localhost,/wlp', name:'localhost,/wlp', type:'runtime'}]}, servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}});
      setCachedServer({ id: 'localhost,/wlp/usr,server1', type: 'server', name: 'localhost,/wlp/usr,server1', state: 'STARTED', wlpInstallDir: '/wlp', cluster: 'cluster1', apps: { up: 0, down: 0, unknown: 0, list: []}});
      setCachedCluster({ id: 'cluster1', type: 'cluster', name: 'cluster1', state: 'STARTED', servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, apps: { up: 0, down: 0, unknown: 0, partial: 0, list: []}});

      var now = clone(oneRunningClusteredServerWithNoAppsWithScalingPolicy);

      unified.__sendUnifiedNotifications(now);

      validateEventWasNotReceived('summary', summaryEvent);
      validateEventWasNotReceived('alerts', alertsEvent);
      validateEventWasNotReceived('hosts', hostsEvent);
      validateEventWasNotReceived('servers', serversEvent);
      validateEventWasNotReceived('clusters', clustersEvent);

      validateServerEvent('localhost,/wlp/usr,server1', serverEvent, { scalingPolicy: 'default' });
      validateClusterEvent('cluster1', clusterEvent, { scalingPolicy: 'default' });
    });

    /**
     * Changing the scaling policy on a clustered server should produce the following events:
     * - server event - updated scaling policy
     * - cluster event - updated scaling policy
     */
    tdd.test('8d. Clustered server changes scalingPolicy', function() {
      var summaryEvent = null;
      addTopicListener(topicUtil.getTopicByType('summary'),  function(e) {
        summaryEvent = e;
      });

      var alertsEvent = null;
      addTopicListener(topicUtil.getTopicByType('alerts'),  function(e) {
        alertsEvent = e;
      });

      var hostsEvent = null;
      addTopicListener(topicUtil.getTopicByType('hosts'),  function(e) {
        hostsEvent = e;
      });

      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      var serverEvent = null;
      addTopicListener(topicUtil.getTopicByType('server', 'localhost,/wlp/usr,server1'),  function(e) {
        serverEvent = e;
      });

      var clustersEvent = null;
      addTopicListener(topicUtil.getTopicByType('clusters'),  function(e) {
        clustersEvent = e;
      });

      var clusterEvent = null;
      addTopicListener(topicUtil.getTopicByType('cluster', 'cluster1'),  function(e) {
        clusterEvent = e;
      });

      // cached = oneRunningClusteredServerWithNoAppsWithScalingPolicy
      setCachedApplications({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedClusters({ up: 1, down: 0, unknown: 0, partial: 0, list: ['cluster1']});
      setCachedServers({ up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']});
      setCachedHosts({ up: 1, down: 0, unknown: 0, partial: 0, empty: 0, list: ['localhost']});
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedSummaryAndAlerts();
      setCachedHost({ id: 'localhost', type: 'host', name: 'localhost', state: 'STARTED', runtimes: {list: [{id:'localhost,/wlp', name:'localhost,/wlp', type:'runtime'}]}, servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}});
      setCachedServer({ id: 'localhost,/wlp/usr,server1', type: 'server', name: 'localhost,/wlp/usr,server1', state: 'STARTED', wlpInstallDir: '/wlp', cluster: 'cluster1', scalingPolicy: 'default', apps: { up: 0, down: 0, unknown: 0, list: []}});
      setCachedCluster({ id: 'cluster1', type: 'cluster', name: 'cluster1', state: 'STARTED', servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, apps: { up: 0, down: 0, unknown: 0, partial: 0, list: []}});

      var now = clone(oneRunningClusteredServerWithNoAppsWithNewScalingPolicy);

      unified.__sendUnifiedNotifications(now);

      validateEventWasNotReceived('summary', summaryEvent);
      validateEventWasNotReceived('alerts', alertsEvent);
      validateEventWasNotReceived('hosts', hostsEvent);
      validateEventWasNotReceived('servers', serversEvent);
      validateEventWasNotReceived('clusters', clustersEvent);

      validateServerEvent('localhost,/wlp/usr,server1', serverEvent, { scalingPolicy: 'newPolicy' });
      validateClusterEvent('cluster1', clusterEvent, { scalingPolicy: 'newPolicy' });
    });

    /**
     * Removing the scaling policy from a clustered server should produce the following events:
     * - server event - updated scaling policy
     * - cluster event - updated scaling policy
     */
    tdd.test('8e. Clustered server removes scalingPolicy', function() {
      var summaryEvent = null;
      addTopicListener(topicUtil.getTopicByType('summary'),  function(e) {
        summaryEvent = e;
      });

      var alertsEvent = null;
      addTopicListener(topicUtil.getTopicByType('alerts'),  function(e) {
        alertsEvent = e;
      });

      var hostsEvent = null;
      addTopicListener(topicUtil.getTopicByType('hosts'),  function(e) {
        hostsEvent = e;
      });

      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      var serverEvent = null;
      addTopicListener(topicUtil.getTopicByType('server', 'localhost,/wlp/usr,server1'),  function(e) {
        serverEvent = e;
      });

      var clustersEvent = null;
      addTopicListener(topicUtil.getTopicByType('clusters'),  function(e) {
        clustersEvent = e;
      });

      var clusterEvent = null;
      addTopicListener(topicUtil.getTopicByType('cluster', 'cluster1'),  function(e) {
        clusterEvent = e;
      });

      // cached = oneRunningClusteredServerWithNoAppsWithScalingPolicy
      setCachedApplications({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedClusters({ up: 1, down: 0, unknown: 0, partial: 0, list: ['cluster1']});
      setCachedServers({ up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']});
      setCachedHosts({ up: 1, down: 0, unknown: 0, partial: 0, empty: 0, list: ['localhost']});
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedSummaryAndAlerts();
      setCachedHost({ id: 'localhost', type: 'host', name: 'localhost', state: 'STARTED', runtimes: {list: [{id:'localhost,/wlp', name:'localhost,/wlp', type:'runtime'}]}, servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}});
      setCachedServer({ id: 'localhost,/wlp/usr,server1', type: 'server', name: 'localhost,/wlp/usr,server1', state: 'STARTED', wlpInstallDir: '/wlp', cluster: 'cluster1', scalingPolicy: 'default', apps: { up: 0, down: 0, unknown: 0, list: []}});
      setCachedCluster({ id: 'cluster1', type: 'cluster', name: 'cluster1', state: 'STARTED', scalingPolicy: 'default', servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, apps: { up: 0, down: 0, unknown: 0, partial: 0, list: []}});

      var now = clone(oneRunningClusteredServerWithNoApps);

      unified.__sendUnifiedNotifications(now);

      validateEventWasNotReceived('summary', summaryEvent);
      validateEventWasNotReceived('alerts', alertsEvent);
      validateEventWasNotReceived('hosts', hostsEvent);
      validateEventWasNotReceived('servers', serversEvent);
      validateEventWasNotReceived('clusters', clustersEvent);

      validateServerEvent('localhost,/wlp/usr,server1', serverEvent, { scalingPolicy: null });
      validateClusterEvent('cluster1', clusterEvent, { scalingPolicy: null });
    });

    /**
     * A starting clustered server should not change the cluster state from stopped
     * - server event - updated state to starting
     */
    tdd.test('8f. Clustered server is starting, cluster should still be considered down (STOPPED -> STARTING)', function() {
      var summaryEvent = null;
      addTopicListener(topicUtil.getTopicByType('summary'),  function(e) {
        summaryEvent = e;
      });

      var alertsEvent = null;
      addTopicListener(topicUtil.getTopicByType('alerts'),  function(e) {
        alertsEvent = e;
      });

      var hostsEvent = null;
      addTopicListener(topicUtil.getTopicByType('hosts'),  function(e) {
        hostsEvent = e;
      });

      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      var serverEvent = null;
      addTopicListener(topicUtil.getTopicByType('server', 'localhost,/wlp/usr,server1'),  function(e) {
        serverEvent = e;
      });

      var clustersEvent = null;
      addTopicListener(topicUtil.getTopicByType('clusters'),  function(e) {
        clustersEvent = e;
      });

      var clusterEvent = null;
      addTopicListener(topicUtil.getTopicByType('cluster', 'cluster1'),  function(e) {
        clusterEvent = e;
      });

      // cached = oneStoppedClusteredServerWithNoApps
      setCachedApplications({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedClusters({ up: 0, down: 1, unknown: 0, partial: 0, list: ['cluster1']});
      setCachedServers({ up: 0, down: 1, unknown: 0, list: ['localhost,/wlp/usr,server1']});
      setCachedHosts({ up: 0, down: 1, unknown: 0, partial: 0, empty: 0, list: ['localhost']});
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedSummaryAndAlerts();
      setCachedHost({ id: 'localhost', type: 'host', name: 'localhost', state: 'STOPPED', runtimes: {list: [{id:'localhost,/wlp', name:'localhost,/wlp', type:'runtime'}]}, servers: {up: 0, down: 1, unknown: 0, list: ['localhost,/wlp/usr,server1']}});
      setCachedServer({ id: 'localhost,/wlp/usr,server1', type: 'server', name: 'localhost,/wlp/usr,server1', state: 'STOPPED', wlpInstallDir: '/wlp', cluster: 'cluster1', apps: { up: 0, down: 0, unknown: 0, list: []}});
      setCachedCluster({ id: 'cluster1', type: 'cluster', name: 'cluster1', state: 'STOPPED', servers: {up: 0, down: 1, unknown: 0, list: ['localhost,/wlp/usr,server1']}, apps: { up: 0, down: 0, unknown: 0, partial: 0, list: []}});

      var now = clone(oneStartingClusteredServerWithNoApps);

      unified.__sendUnifiedNotifications(now);

      validateEventWasNotReceived('summary', summaryEvent);
      validateEventWasNotReceived('alerts', alertsEvent);
      validateEventWasNotReceived('hosts', hostsEvent);
      validateEventWasNotReceived('servers', serversEvent);
      validateEventWasNotReceived('clusters', clustersEvent);

      validateServerEvent('localhost,/wlp/usr,server1', serverEvent, { state: 'STARTING' });
      validateClusterEvent('cluster1', clusterEvent, { state: 'STARTING' });
    });

    /**
     * A stopping clustered server should not change the cluster state to stopped
     * - server event - updated state to stopping
     * - cluster event - updated state to stopped
     */
    tdd.test('8g. Clustered server is stopping, cluster should be set to down (STARTED -> STOPPING)', function() {
      var summaryEvent = null;
      addTopicListener(topicUtil.getTopicByType('summary'),  function(e) {
        summaryEvent = e;
      });

      var alertsEvent = null;
      addTopicListener(topicUtil.getTopicByType('alerts'),  function(e) {
        alertsEvent = e;
      });

      var hostsEvent = null;
      addTopicListener(topicUtil.getTopicByType('hosts'),  function(e) {
        hostsEvent = e;
      });

      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      var serverEvent = null;
      addTopicListener(topicUtil.getTopicByType('server', 'localhost,/wlp/usr,server1'),  function(e) {
        serverEvent = e;
      });

      var clustersEvent = null;
      addTopicListener(topicUtil.getTopicByType('clusters'),  function(e) {
        clustersEvent = e;
      });

      var clusterEvent = null;
      addTopicListener(topicUtil.getTopicByType('cluster', 'cluster1'),  function(e) {
        clusterEvent = e;
      });

      // cached = oneRunningClusteredServerWithNoApps
      setCachedApplications({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedClusters({ up: 1, down: 0, unknown: 0, partial: 0, list: ['cluster1']});
      setCachedServers({ up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']});
      setCachedHosts({ up: 1, down: 0, unknown: 0, partial: 0, empty: 0, list: ['localhost']});
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedSummaryAndAlerts();
      setCachedHost({ id: 'localhost', type: 'host', name: 'localhost', state: 'STARTED', runtimes: {list: [{id:'localhost,/wlp', name:'localhost,/wlp', type:'runtime'}]}, servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}});
      setCachedServer({ id: 'localhost,/wlp/usr,server1', type: 'server', name: 'localhost,/wlp/usr,server1', state: 'STARTED', wlpInstallDir: '/wlp', cluster: 'cluster1', apps: { up: 0, down: 0, unknown: 0, list: []}});
      setCachedCluster({ id: 'cluster1', type: 'cluster', name: 'cluster1', state: 'STARTED', servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, apps: { up: 0, down: 0, unknown: 0, partial: 0, list: []}});

      var now = clone(oneStoppingClusteredServerWithNoApps);

      unified.__sendUnifiedNotifications(now);

      validateSummaryEvent(summaryEvent, { clusters: { up: 0, down: 1, unknown: 0, partial: 0 }, servers: { up: 0, down: 1, unknown: 0 }, hosts: { up: 0, down: 1, unknown: 0, partial: 0, empty: 0 } });
      validateEventWasNotReceived('alerts', alertsEvent);
      validateHostsEvent(hostsEvent, { up: 0, down: 1, unknown: 0, partial: 0, empty: 0 });
      validateServersEvent(serversEvent, { up: 0, down: 1, unknown: 0 });
      validateClustersEvent(clustersEvent, { up: 0, down: 1, unknown: 0, partial: 0 });

      validateServerEvent('localhost,/wlp/usr,server1', serverEvent, { state: 'STOPPING' });
      validateClusterEvent('cluster1', clusterEvent, { state: 'STOPPING', servers: {up: 0, down: 1, unknown: 0} });
    });

    /**
     * A starting -> started clustered server should change the cluster state from stopped to started
     * - server event - updated state to started
     * - cluster event - updated state to started
     */
    tdd.test('8h. Clustered server is starting, cluster should still be considered down (STARTING -> STARTED)', function() {
      var summaryEvent = null;
      addTopicListener(topicUtil.getTopicByType('summary'),  function(e) {
        summaryEvent = e;
      });

      var alertsEvent = null;
      addTopicListener(topicUtil.getTopicByType('alerts'),  function(e) {
        alertsEvent = e;
      });

      var hostsEvent = null;
      addTopicListener(topicUtil.getTopicByType('hosts'),  function(e) {
        hostsEvent = e;
      });

      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      var serverEvent = null;
      addTopicListener(topicUtil.getTopicByType('server', 'localhost,/wlp/usr,server1'),  function(e) {
        serverEvent = e;
      });

      var clustersEvent = null;
      addTopicListener(topicUtil.getTopicByType('clusters'),  function(e) {
        clustersEvent = e;
      });

      var clusterEvent = null;
      addTopicListener(topicUtil.getTopicByType('cluster', 'cluster1'),  function(e) {
        clusterEvent = e;
      });

      // cached = oneStartingClusteredServerWithNoApps
      setCachedApplications({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedClusters({ up: 0, down: 1, unknown: 0, partial: 0, list: ['cluster1']});
      setCachedServers({ up: 0, down: 1, unknown: 0, list: ['localhost,/wlp/usr,server1']});
      setCachedHosts({ up: 0, down: 1, unknown: 0, partial: 0, empty: 0, list: ['localhost']});
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedSummaryAndAlerts();
      setCachedHost({ id: 'localhost', type: 'host', name: 'localhost', state: 'STARTING', runtimes: {list: [{id:'localhost,/wlp', name:'localhost,/wlp', type:'runtime'}]}, servers: {up: 0, down: 1, unknown: 0, list: ['localhost,/wlp/usr,server1']}});
      setCachedServer({ id: 'localhost,/wlp/usr,server1', type: 'server', name: 'localhost,/wlp/usr,server1', state: 'STOPPED', wlpInstallDir: '/wlp', cluster: 'cluster1', apps: { up: 0, down: 0, unknown: 0, list: []}});
      setCachedCluster({ id: 'cluster1', type: 'cluster', name: 'cluster1', state: 'STARTING', servers: {up: 0, down: 1, unknown: 0, list: ['localhost,/wlp/usr,server1']}, apps: { up: 0, down: 0, unknown: 0, partial: 0, list: []}});

      var now = clone(oneRunningClusteredServerWithNoApps);

      unified.__sendUnifiedNotifications(now);

      validateSummaryEvent(summaryEvent, { clusters: { up: 1, down: 0, unknown: 0, partial: 0 }, servers: { up: 1, down: 0, unknown: 0 }, hosts: { up: 1, down: 0, unknown: 0, partial: 0, empty: 0 } });
      validateEventWasNotReceived('alerts', alertsEvent);
      validateHostsEvent(hostsEvent, { up: 1, down: 0, unknown: 0, partial: 0, empty: 0 });
      validateServersEvent(serversEvent, { up: 1, down: 0, unknown: 0 });
      validateClustersEvent(clustersEvent, { up: 1, down: 0, unknown: 0, partial: 0 });

      validateServerEvent('localhost,/wlp/usr,server1', serverEvent, { state: 'STARTED' });
      validateClusterEvent('cluster1', clusterEvent, { state: 'STARTED', servers: {up: 1, down: 0, unknown: 0} });
    });

    /**
     * A stopping -> stopped clustered server should not change the cluster state
     * - server event - updated state to stopping
     * - cluster event - updated state to stopped
     */
    tdd.test('8i. Clustered server is stopping, cluster should be set to down (STOPPING -> STOPPED)', function() {
      var summaryEvent = null;
      addTopicListener(topicUtil.getTopicByType('summary'),  function(e) {
        summaryEvent = e;
      });

      var alertsEvent = null;
      addTopicListener(topicUtil.getTopicByType('alerts'),  function(e) {
        alertsEvent = e;
      });

      var hostsEvent = null;
      addTopicListener(topicUtil.getTopicByType('hosts'),  function(e) {
        hostsEvent = e;
      });

      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      var serverEvent = null;
      addTopicListener(topicUtil.getTopicByType('server', 'localhost,/wlp/usr,server1'),  function(e) {
        serverEvent = e;
      });

      var clustersEvent = null;
      addTopicListener(topicUtil.getTopicByType('clusters'),  function(e) {
        clustersEvent = e;
      });

      var clusterEvent = null;
      addTopicListener(topicUtil.getTopicByType('cluster', 'cluster1'),  function(e) {
        clusterEvent = e;
      });

      // cached = oneStoppingClusteredServerWithNoApps
      setCachedApplications({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedClusters({ up: 0, down: 1, unknown: 0, partial: 0, list: ['cluster1']});
      setCachedServers({ up: 0, down: 1, unknown: 0, list: ['localhost,/wlp/usr,server1']});
      setCachedHosts({ up: 0, down: 1, unknown: 0, partial: 0, empty: 0, list: ['localhost']});
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedSummaryAndAlerts();
      setCachedHost({ id: 'localhost', type: 'host', name: 'localhost', state: 'STOPPED', runtimes: {list: [{id:'localhost,/wlp', name:'localhost,/wlp', type:'runtime'}]}, servers: {up: 0, down: 1, unknown: 0, list: ['localhost,/wlp/usr,server1']}});
      setCachedServer({ id: 'localhost,/wlp/usr,server1', type: 'server', name: 'localhost,/wlp/usr,server1', state: 'STOPPING', wlpInstallDir: '/wlp', cluster: 'cluster1', apps: { up: 0, down: 0, unknown: 0, list: []}});
      setCachedCluster({ id: 'cluster1', type: 'cluster', name: 'cluster1', state: 'STOPPING', servers: {up: 0, down: 1, unknown: 0, list: ['localhost,/wlp/usr,server1']}, apps: { up: 0, down: 0, unknown: 0, partial: 0, list: []}});

      var now = clone(oneStoppedClusteredServerWithNoApps);

      unified.__sendUnifiedNotifications(now);

      validateEventWasNotReceived('summary', summaryEvent);
      validateEventWasNotReceived('alerts', alertsEvent);
      validateEventWasNotReceived('hosts', hostsEvent);
      validateEventWasNotReceived('servers', serversEvent);
      validateEventWasNotReceived('clusters', clustersEvent);

      validateServerEvent('localhost,/wlp/usr,server1', serverEvent, { state: 'STOPPED' });
      validateClusterEvent('cluster1', clusterEvent, { state: 'STOPPED' });
    });

    /**
     * Changing an existing server to become a clustered server should produce the following events:
     * - server event - update cluster value
     * - clusters - updated tallies and 1 added cluster
     */
    tdd.test('9a. Non-clustered server becomes clustered', function() {
      var summaryEvent = null;
      addTopicListener(topicUtil.getTopicByType('summary'),  function(e) {
        summaryEvent = e;
      });

      var alertsEvent = null;
      addTopicListener(topicUtil.getTopicByType('alerts'),  function(e) {
        alertsEvent = e;
      });

      var hostsEvent = null;
      addTopicListener(topicUtil.getTopicByType('hosts'),  function(e) {
        hostsEvent = e;
      });

      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      var serverEvent = null;
      addTopicListener(topicUtil.getTopicByType('server', 'localhost,/wlp/usr,server1'),  function(e) {
        serverEvent = e;
      });

      var clustersEvent = null;
      addTopicListener(topicUtil.getTopicByType('clusters'),  function(e) {
        clustersEvent = e;
      });

      // cached = oneRunningServerWithNoApps
      setCachedApplications({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedClusters({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedServers({ up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']});
      setCachedHosts({ up: 1, down: 0, unknown: 0, partial: 0, empty: 0, list: ['localhost']});
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedSummaryAndAlerts();
      setCachedHost({ id: 'localhost', type: 'host', name: 'localhost', state: 'STARTED', runtimes: {list: [{id:'localhost,/wlp', name:'localhost,/wlp', type:'runtime'}]}, servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}});
      setCachedServer({ id: 'localhost,/wlp/usr,server1', type: 'server', name: 'localhost,/wlp/usr,server1', state: 'STARTED', wlpInstallDir: '/wlp', apps: { up: 0, down: 0, unknown: 0, list: []}});

      var now = clone(oneRunningClusteredServerWithNoApps);

      unified.__sendUnifiedNotifications(now);
      validateSummaryEvent(summaryEvent, { clusters: { up: 1, down: 0, unknown: 0, partial: 0 } });
      validateEventWasNotReceived('alerts', alertsEvent);
      validateEventWasNotReceived('hosts', hostsEvent);
      validateEventWasNotReceived('servers', serversEvent);
      validateClustersEvent(clustersEvent, { up: 1, down: 0, unknown: 0, partial: 0, added: ['cluster1'] });

      validateServerEvent('localhost,/wlp/usr,server1', serverEvent, { cluster: 'cluster1' });
    });

    /**
     * Changing an existing clustered server to become a non-cluster server should produce the following events:
     * - server event - update cluster value to null
     * - clusters - updated tallies and 1 removed cluster
     */
    tdd.test('9b. Clustered server becomes non-clustered', function() {
      var summaryEvent = null;
      addTopicListener(topicUtil.getTopicByType('summary'),  function(e) {
        summaryEvent = e;
      });

      var alertsEvent = null;
      addTopicListener(topicUtil.getTopicByType('alerts'),  function(e) {
        alertsEvent = e;
      });

      var hostsEvent = null;
      addTopicListener(topicUtil.getTopicByType('hosts'),  function(e) {
        hostsEvent = e;
      });

      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      var serverEvent = null;
      addTopicListener(topicUtil.getTopicByType('server', 'localhost,/wlp/usr,server1'),  function(e) {
        serverEvent = e;
      });

      var clustersEvent = null;
      addTopicListener(topicUtil.getTopicByType('clusters'),  function(e) {
        clustersEvent = e;
      });

      // cached = oneRunningClusteredServerWithNoApps
      setCachedApplications({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedClusters({ up: 1, down: 0, unknown: 0, partial: 0, list: ['cluster1']});
      setCachedServers({ up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']});
      setCachedHosts({ up: 1, down: 0, unknown: 0, partial: 0, empty: 0, list: ['localhost']});
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedSummaryAndAlerts();
      setCachedHost({ id: 'localhost', type: 'host', name: 'localhost', state: 'STARTED', runtimes: {list: [{id:'localhost,/wlp', name:'localhost,/wlp', type:'runtime'}]}, servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}});
      setCachedServer({ id: 'localhost,/wlp/usr,server1', type: 'server', name: 'localhost,/wlp/usr,server1', state: 'STARTED', wlpInstallDir: '/wlp', cluster: 'cluster1', apps: { up: 0, down: 0, unknown: 0, list: []}});
      setCachedCluster({ id: 'cluster1', type: 'cluster', name: 'cluster1', state: 'STARTED', servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, apps: { up: 0, down: 0, unknown: 0, partial: 0, list: []}});

      var now = clone(oneRunningServerWithNoApps);

      unified.__sendUnifiedNotifications(now);

      validateSummaryEvent(summaryEvent, { clusters: { up: 0, down: 0, unknown: 0, partial: 0 } });
      validateEventWasNotReceived('alerts', alertsEvent);
      validateEventWasNotReceived('hosts', hostsEvent);
      validateEventWasNotReceived('servers', serversEvent);
      validateClustersEvent(clustersEvent, { up: 0, down: 0, unknown: 0, partial: 0, removed: ['cluster1'] });

      validateServerEvent('localhost,/wlp/usr,server1', serverEvent, { cluster: null });
    });

    /**
     * Adding an existing server to a new cluster should produce the following events:
     * - server event - update cluster value
     * - clusters - updated tallies and 1 added cluster
     */
    tdd.test('9c. Stopped non-clustered server becomes a running clustered server', function() {
      var summaryEvent = null;
      addTopicListener(topicUtil.getTopicByType('summary'),  function(e) {
        summaryEvent = e;
      });

      var alertsEvent = null;
      addTopicListener(topicUtil.getTopicByType('alerts'),  function(e) {
        alertsEvent = e;
      });

      var hostsEvent = null;
      addTopicListener(topicUtil.getTopicByType('hosts'),  function(e) {
        hostsEvent = e;
      });

      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      var serverEvent = null;
      addTopicListener(topicUtil.getTopicByType('server', 'localhost,/wlp/usr,server1'),  function(e) {
        serverEvent = e;
      });

      var clustersEvent = null;
      addTopicListener(topicUtil.getTopicByType('clusters'),  function(e) {
        clustersEvent = e;
      });

      // cached = oneStoppedServerWithNoApps
      setCachedApplications({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedClusters({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedServers({ up: 0, down: 1, unknown: 0, list: ['localhost,/wlp/usr,server1']});
      setCachedHosts({ up: 0, down: 1, unknown: 0, partial: 0, empty: 0, list: ['localhost']});
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedSummaryAndAlerts();
      setCachedHost({ id: 'localhost', type: 'host', name: 'localhost', state: 'STARTED', runtimes: {list: [{id:'localhost,/wlp', name:'localhost,/wlp', type:'runtime'}]}, servers: {up: 0, down: 1, unknown: 0, list: ['localhost,/wlp/usr,server1']}});
      setCachedServer({ id: 'localhost,/wlp/usr,server1', type: 'server', name: 'localhost,/wlp/usr,server1', state: 'STOPPED', wlpInstallDir: '/wlp', apps: { up: 0, down: 0, unknown: 0, list: []}});

      var now = clone(oneRunningClusteredServerWithNoApps);

      unified.__sendUnifiedNotifications(now);

      validateSummaryEvent(summaryEvent, { clusters: { up: 1, down: 0, unknown: 0, partial: 0 }, servers: { up: 1, down: 0, unknown: 0 }, hosts: { up: 1, down: 0, unknown: 0, partial: 0, empty: 0 } });
      validateEventWasNotReceived('alerts', alertsEvent);
      validateHostsEvent(hostsEvent, { up: 1, down: 0, unknown: 0, partial: 0, empty: 0 });
      validateServersEvent(serversEvent, { up: 1, down: 0, unknown: 0 });
      validateClustersEvent(clustersEvent, { up: 1, down: 0, unknown: 0, partial: 0, added: ['cluster1'] });

      validateServerEvent('localhost,/wlp/usr,server1', serverEvent, { state: 'STARTED', cluster: 'cluster1' });
    });

    /**
     * Changing the cluster which a server belongs to should produce the following events:
     * - server event - update cluster value
     * - clusters - updated tallies and 1 removed and 1 added cluster
     * - cluster event - update cluster to be removed
     */
    tdd.test('9d. Running server changes cluster', function() {
      var summaryEvent = null;
      addTopicListener(topicUtil.getTopicByType('summary'),  function(e) {
        summaryEvent = e;
      });

      var alertsEvent = null;
      addTopicListener(topicUtil.getTopicByType('alerts'),  function(e) {
        alertsEvent = e;
      });

      var hostsEvent = null;
      addTopicListener(topicUtil.getTopicByType('hosts'),  function(e) {
        hostsEvent = e;
      });

      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      var serverEvent = null;
      addTopicListener(topicUtil.getTopicByType('server', 'localhost,/wlp/usr,server1'),  function(e) {
        serverEvent = e;
      });

      var clustersEvent = null;
      addTopicListener(topicUtil.getTopicByType('clusters'),  function(e) {
        clustersEvent = e;
      });

      var clusterEvent = null;
      addTopicListener(topicUtil.getTopicByType('cluster', 'cluster1'),  function(e) {
        clusterEvent = e;
      });

      // cached = oneRunningClusteredServerWithNoApps
      setCachedApplications({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedClusters({ up: 2, down: 0, unknown: 0, partial: 0, list: ['cluster1']});
      setCachedServers({ up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']});
      setCachedHosts({ up: 1, down: 0, unknown: 0, partial: 0, empty: 0, list: ['localhost']});
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedSummaryAndAlerts();
      setCachedHost({ id: 'localhost', type: 'host', name: 'localhost', state: 'STARTED', runtimes: {list: [{id:'localhost,/wlp', name:'localhost,/wlp', type:'runtime'}]}, servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}});
      setCachedServer({ id: 'localhost,/wlp/usr,server1', type: 'server', name: 'localhost,/wlp/usr,server1', state: 'STARTED', wlpInstallDir: '/wlp', cluster: 'cluster1', apps: { up: 0, down: 0, unknown: 0, list: []}});
      setCachedCluster({ id: 'cluster1', type: 'cluster', name: 'cluster1', state: 'STARTED', servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, apps: { up: 0, down: 0, unknown: 0, partial: 0, list: []}});

      var now = clone(oneRunningClustered2ServerWithNoApps);

      unified.__sendUnifiedNotifications(now);

      validateSummaryEvent(summaryEvent, { clusters: { up: 1, down: 0, unknown: 0, partial: 0 } });
      validateEventWasNotReceived('alerts', alertsEvent);
      validateEventWasNotReceived('hosts', hostsEvent);
      validateEventWasNotReceived('servers', serversEvent);
      validateClustersEvent(clustersEvent, { up: 1, down: 0, unknown: 0, partial: 0, added: ['cluster2'], removed: ['cluster1'] });

      validateServerEvent('localhost,/wlp/usr,server1', serverEvent, { cluster: 'cluster2' });
      validateClusterEvent('cluster1', clusterEvent, { state: 'removed' });
    });

    /**
     * Adding a new server (on an existing host) to an existing cluster should produce the following events:
     * - servers - update tallies and 1 added server
     * - cluster event - 1 added server
     */
    tdd.test('9e. Adding a new server to an existing cluster', function() {
      var summaryEvent = null;
      addTopicListener(topicUtil.getTopicByType('summary'),  function(e) {
        summaryEvent = e;
      });

      var alertsEvent = null;
      addTopicListener(topicUtil.getTopicByType('alerts'),  function(e) {
        alertsEvent = e;
      });

      var hostsEvent = null;
      addTopicListener(topicUtil.getTopicByType('hosts'),  function(e) {
        hostsEvent = e;
      });

      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      var clustersEvent = null;
      addTopicListener(topicUtil.getTopicByType('clusters'),  function(e) {
        clustersEvent = e;
      });

      var clusterEvent = null;
      addTopicListener(topicUtil.getTopicByType('cluster', 'cluster1'),  function(e) {
        clusterEvent = e;
      });

      // cached = oneRunningClusteredServerWithNoApps
      setCachedApplications({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedClusters({ up: 1, down: 0, unknown: 0, partial: 0, list: ['cluster1']});
      setCachedServers({ up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']});
      setCachedHosts({ up: 1, down: 0, unknown: 0, partial: 0, empty: 0, list: ['localhost']});
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedSummaryAndAlerts();
      setCachedHost({ id: 'localhost', type: 'host', name: 'localhost', state: 'STARTED', runtimes: {list: [{id:'localhost,/wlp', name:'localhost,/wlp', type:'runtime'}]}, servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}});
      setCachedServer({ id: 'localhost,/wlp/usr,server1', type: 'server', name: 'localhost,/wlp/usr,server1', state: 'STARTED', wlpInstallDir: '/wlp', cluster: 'cluster1', apps: { up: 0, down: 0, unknown: 0, list: []}});
      setCachedCluster({ id: 'cluster1', type: 'cluster', name: 'cluster1', state: 'STARTED', servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, apps: { up: 0, down: 0, unknown: 0, partial: 0, list: []}});

      var now = clone(twoRunningSameClusterServerWithNoApps);

      unified.__sendUnifiedNotifications(now);

      validateSummaryEvent(summaryEvent, { servers: { up: 2, down: 0, unknown: 0 } });
      validateEventWasNotReceived('alerts', alertsEvent);
      validateEventWasNotReceived('hosts', hostsEvent);
      validateServersEvent(serversEvent, { up: 2, down: 0, unknown: 0, added: ['localhost,/wlp/usr,server2'] });
      validateEventWasNotReceived('clusters', clustersEvent);

      validateClusterEvent('cluster1', clusterEvent, { servers: { up: 2, down: 0, unknown: 0, added: ['localhost,/wlp/usr,server2'] } });
    });

    /**
     * Remove a running server from an existing running cluster should produce the following events:
     * - servers - update tallies and 1 removed server
     * - server event - removed notification
     * - cluster event - 1 removed server
     */
    tdd.test('9f. Removing a running server from an existing running cluster', function() {
      var summaryEvent = null;
      addTopicListener(topicUtil.getTopicByType('summary'),  function(e) {
        summaryEvent = e;
      });

      var alertsEvent = null;
      addTopicListener(topicUtil.getTopicByType('alerts'),  function(e) {
        alertsEvent = e;
      });

      var hostsEvent = null;
      addTopicListener(topicUtil.getTopicByType('hosts'),  function(e) {
        hostsEvent = e;
      });

      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      var serverEvent = null;
      addTopicListener(topicUtil.getTopicByType('server', 'localhost,/wlp/usr,server2'),  function(e) {
        serverEvent = e;
      });

      var clustersEvent = null;
      addTopicListener(topicUtil.getTopicByType('clusters'),  function(e) {
        clustersEvent = e;
      });

      var clusterEvent = null;
      addTopicListener(topicUtil.getTopicByType('cluster', 'cluster1'),  function(e) {
        clusterEvent = e;
      });

      // cached = twoRunningSameClusterServerWithNoApps
      setCachedApplications({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedClusters({ up: 1, down: 0, unknown: 0, partial: 0, list: ['cluster1']});
      setCachedServers({ up: 2, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1','localhost,/wlp/usr,server2']});
      setCachedHosts({ up: 1, down: 0, unknown: 0, partial: 0, empty: 0, list: ['localhost']});
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedSummaryAndAlerts();
      setCachedHost({ id: 'localhost', type: 'host', name: 'localhost', state: 'STARTED', runtimes: {list: [{id:'localhost,/wlp', name:'localhost,/wlp', type:'runtime'}]}, servers: {up: 2, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1','localhost,/wlp/usr,server2']}});
      setCachedServer({ id: 'localhost,/wlp/usr,server1', type: 'server', name: 'localhost,/wlp/usr,server1', state: 'STARTED', wlpInstallDir: '/wlp', cluster: 'cluster1', apps: { up: 0, down: 0, unknown: 0, list: []}});
      setCachedServer({ id: 'localhost,/wlp/usr,server2', type: 'server', name: 'localhost,/wlp/usr,server2', state: 'STARTED', wlpInstallDir: '/wlp', cluster: 'cluster1', apps: { up: 0, down: 0, unknown: 0, list: []}});
      setCachedCluster({ id: 'cluster1', type: 'cluster', name: 'cluster1', state: 'STARTED', servers: {up: 2, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1','localhost,/wlp/usr,server2']}, apps: { up: 0, down: 0, unknown: 0, partial: 0, list: []}});

      var now = clone(oneRunningClusteredServerWithNoApps);

      unified.__sendUnifiedNotifications(now);
      validateSummaryEvent(summaryEvent, { servers: { up: 1, down: 0, unknown: 0 } });
      validateEventWasNotReceived('alerts', alertsEvent);
      validateEventWasNotReceived('hosts', hostsEvent);
      validateServersEvent(serversEvent, { up: 1, down: 0, unknown: 0, removed: ['localhost,/wlp/usr,server2'] });
      validateEventWasNotReceived('clusters', clustersEvent);

      validateServerEvent('localhost,/wlp/usr,server2', serverEvent, { state: 'removed' });
      validateClusterEvent('cluster1', clusterEvent, { servers: { up: 1, down: 0, unknown: 0, removed: ['localhost,/wlp/usr,server2'] } });
    });

    /**
     * Adding a new server with apps to an empty collective should produce the following events:
     * - hosts - updated tallies and 1 added host
     * - servers - updated tallies and 1 added server
     * - applications - updated tallies and 1 added application
     */
    tdd.test('10a. Add new running server with apps to an empty collective', function() {
      var summaryEvent = null;
      addTopicListener(topicUtil.getTopicByType('summary'),  function(e) {
        summaryEvent = e;
      });

      var alertsEvent = null;
      addTopicListener(topicUtil.getTopicByType('alerts'),  function(e) {
        alertsEvent = e;
      });

      var hostsEvent = null;
      addTopicListener(topicUtil.getTopicByType('hosts'),  function(e) {
        hostsEvent = e;
      });

      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      var clustersEvent = null;
      addTopicListener(topicUtil.getTopicByType('clusters'),  function(e) {
        clustersEvent = e;
      });

      var applicationsEvent = null;
      addTopicListener(topicUtil.getTopicByType('applications'),  function(e) {
        applicationsEvent = e;
      });

      // cached = emptyCollective
      setCachedApplications({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedClusters({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedServers({ up: 0, down: 0, unknown: 0, list: []});
      setCachedHosts({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedSummaryAndAlerts();

      var now = clone(oneRunningServerWithOneApp);

      unified.__sendUnifiedNotifications(now);

      validateSummaryEvent(summaryEvent, { applications: { up: 1, down: 0, unknown: 0, partial: 0 }, servers: { up: 1, down: 0, unknown: 0 }, hosts: { up: 1, down: 0, unknown: 0, partial: 0, empty: 0 } });
      validateEventWasNotReceived('alerts', alertsEvent);
      validateHostsEvent(hostsEvent, { up: 1, down: 0, unknown: 0, partial: 0, empty: 0, added: ['localhost'] });
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      validateServersEvent(serversEvent, { up: 1, down: 0, unknown: 0, added: ['localhost,/wlp/usr,server1'] });
      validateEventWasNotReceived('clusters', clustersEvent);
      validateApplicationsEvent(applicationsEvent, { up: 1, down: 0, unknown: 0, partial: 0, added: ['localhost,/wlp/usr,server1,app1'] });
    });

    /**
     * Adding a new clustered server with apps to an empty collective should produce the following events:
     * - hosts - updated tallies and 1 added host
     * - servers - updated tallies and 1 added server
     * - clusters - updated tallies and 1 added cluster
     * - applications - updated tallies and 1 added application
     */
    tdd.test('10b. Add new running clustered server with apps to an empty collective', function() {
      var summaryEvent = null;
      addTopicListener(topicUtil.getTopicByType('summary'),  function(e) {
        summaryEvent = e;
      });

      var alertsEvent = null;
      addTopicListener(topicUtil.getTopicByType('alerts'),  function(e) {
        alertsEvent = e;
      });

      var hostsEvent = null;
      addTopicListener(topicUtil.getTopicByType('hosts'),  function(e) {
        hostsEvent = e;
      });

      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      var clustersEvent = null;
      addTopicListener(topicUtil.getTopicByType('clusters'),  function(e) {
        clustersEvent = e;
      });

      var applicationsEvent = null;
      addTopicListener(topicUtil.getTopicByType('applications'),  function(e) {
        applicationsEvent = e;
      });

      // cached = emptyCollective
      setCachedApplications({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedClusters({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedServers({ up: 0, down: 0, unknown: 0, list: []});
      setCachedHosts({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedSummaryAndAlerts();

      var now = clone(oneRunningClusteredServerWithOneApp);

      unified.__sendUnifiedNotifications(now);

      validateSummaryEvent(summaryEvent, { applications: { up: 1, down: 0, unknown: 0, partial: 0 }, clusters: { up: 1, down: 0, unknown: 0, partial: 0 }, servers: { up: 1, down: 0, unknown: 0 }, hosts: { up: 1, down: 0, unknown: 0, partial: 0, empty: 0 } });
      validateEventWasNotReceived('alerts', alertsEvent);
      validateHostsEvent(hostsEvent, { up: 1, down: 0, unknown: 0, partial: 0, empty: 0, added: ['localhost'] });
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      validateServersEvent(serversEvent, { up: 1, down: 0, unknown: 0, added: ['localhost,/wlp/usr,server1'] });
      validateClustersEvent(clustersEvent, { up: 1, down: 0, unknown: 0, partial: 0, added: ['cluster1'] });
      validateApplicationsEvent(applicationsEvent, { up: 1, down: 0, unknown: 0, partial: 0, added: ['cluster1,app1'] });
    });

    /**
     * Removing the last running server with an app to an empty collective should produce the following events:
     * - hosts - updated tallies and 1 removed host
     * - host event - indicate host was removed
     * - servers - updated tallies and 1 removed server
     * - server event - indicate server was removed
     * - applications - updated tallies and 1 removed application
     * - application event - indicate app was removed
     */
    tdd.test('10c. Remove last new running server with an app becomes an empty collective', function() {
      var summaryEvent = null;
      addTopicListener(topicUtil.getTopicByType('summary'),  function(e) {
        summaryEvent = e;
      });

      var alertsEvent = null;
      addTopicListener(topicUtil.getTopicByType('alerts'),  function(e) {
        alertsEvent = e;
      });

      var hostsEvent = null;
      addTopicListener(topicUtil.getTopicByType('hosts'),  function(e) {
        hostsEvent = e;
      });

      var hostEvent = null;
      addTopicListener(topicUtil.getTopicByType('host', 'localhost'),  function(e) {
        hostEvent = e;
      });

      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      var serverEvent = null;
      addTopicListener(topicUtil.getTopicByType('server', 'localhost,/wlp/usr,server1'),  function(e) {
        serverEvent = e;
      });

      var clustersEvent = null;
      addTopicListener(topicUtil.getTopicByType('clusters'),  function(e) {
        clustersEvent = e;
      });

      var applicationsEvent = null;
      addTopicListener(topicUtil.getTopicByType('applications'),  function(e) {
        applicationsEvent = e;
      });

      var appEvent = null;
      addTopicListener(topicUtil.getTopicByType('application', 'localhost,/wlp/usr,server1,app1'),  function(e) {
        appEvent = e;
      });

      // cached = oneRunningServerWithOneApp
      setCachedApplications({ up: 1, down: 0, unknown: 0, partial: 0, list: ['localhost,/wlp/usr,server1,app1']});
      setCachedClusters({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedServers({ up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']});
      setCachedHosts({ up: 1, down: 0, unknown: 0, partial: 0, empty: 0, list: ['localhost']});
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedSummaryAndAlerts();
      setCachedHost({ id: 'localhost', type: 'host', name: 'localhost', state: 'STARTED', runtimes: [{id:'localhost,/wlp', name:'localhost,/wlp', type:'runtime'}], servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, apps: { up: 1, down: 0, unknown: 0, partial: 0, list: ['localhost,/wlp/usr,server1,app1']}});
      setCachedServer({ id: 'localhost,/wlp/usr,server1', type: 'server', name: 'localhost,/wlp/usr,server1', state: 'STARTED', wlpInstallDir: '/wlp', apps: { up: 1, down: 0, unknown: 0, list: [{name: 'localhost,/wlp/usr,server1,app1', state: 'STARTED'}]}});
      setCachedApplication({ id: 'localhost,/wlp/usr,server1,app1', type: 'application', name: 'app1', state: 'STARTED', up: 1, down: 0, unknown: 0, servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, clusters: { up: 0, down: 0, unknown: 0, partial: 0, list: []}});

      var now = clone(emptyCollective);

      unified.__sendUnifiedNotifications(now);

      validateSummaryEvent(summaryEvent, { applications: { up: 0, down: 0, unknown: 0, partial: 0 }, servers: { up: 0, down: 0, unknown: 0 }, hosts: { up: 0, down: 0, unknown: 0, partial: 0, empty: 0 } });
      validateEventWasNotReceived('alerts', alertsEvent);
      validateHostsEvent(hostsEvent, { up: 0, down: 0, unknown: 0, partial: 0, empty: 0, removed: ['localhost'] });
      validateServersEvent(serversEvent, { up: 0, down: 0, unknown: 0, removed: ['localhost,/wlp/usr,server1'] });
      validateEventWasNotReceived('clusters', clustersEvent);
      validateApplicationsEvent(applicationsEvent, { up: 0, down: 0, unknown: 0, partial: 0, removed: ['localhost,/wlp/usr,server1,app1'] });

      validateHostEvent('localhost', hostEvent, { state: 'removed' });
      validateServerEvent('localhost,/wlp/usr,server1', serverEvent, { state: 'removed' });
      validateApplicationEvent('localhost,/wlp/usr,server1,app1', appEvent, { state: 'removed' });
    });

    /**
     * Removing the last running clustered server with apps to an empty collective should produce the following events:
     * - hosts - updated tallies and 1 removed host
     * - servers - updated tallies and 1 removed server
     * - clusters - updated tallies and 1 removed cluster
     * - applications - updated tallies and 1 removed application
     */
    tdd.test('10d. Remove last new running clustered server with an app becomes an empty collective', function() {
      var summaryEvent = null;
      addTopicListener(topicUtil.getTopicByType('summary'),  function(e) {
        summaryEvent = e;
      });

      var alertsEvent = null;
      addTopicListener(topicUtil.getTopicByType('alerts'),  function(e) {
        alertsEvent = e;
      });

      var hostsEvent = null;
      addTopicListener(topicUtil.getTopicByType('hosts'),  function(e) {
        hostsEvent = e;
      });

      var hostEvent = null;
      addTopicListener(topicUtil.getTopicByType('host', 'localhost'),  function(e) {
        hostEvent = e;
      });

      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      var serverEvent = null;
      addTopicListener(topicUtil.getTopicByType('server', 'localhost,/wlp/usr,server1'),  function(e) {
        serverEvent = e;
      });

      var clustersEvent = null;
      addTopicListener(topicUtil.getTopicByType('clusters'),  function(e) {
        clustersEvent = e;
      });

      var clusterEvent = null;
      addTopicListener(topicUtil.getTopicByType('cluster', 'cluster1'),  function(e) {
        clusterEvent = e;
      });

      var applicationsEvent = null;
      addTopicListener(topicUtil.getTopicByType('applications'),  function(e) {
        applicationsEvent = e;
      });

      var appEvent = null;
      addTopicListener(topicUtil.getTopicByType('application', 'cluster1,app1'),  function(e) {
        appEvent = e;
      });

      // cached = oneRunningClusteredServerWithOneApp
      setCachedApplications({ up: 1, down: 0, unknown: 0, partial: 0, list: ['cluster1,app1']});
      setCachedClusters({ up: 1, down: 0, unknown: 0, partial: 0, list: ['cluster1']});
      setCachedServers({ up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']});
      setCachedHosts({ up: 1, down: 0, unknown: 0, partial: 0, empty: 0, list: ['localhost']});
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedSummaryAndAlerts();
      setCachedHost({ id: 'localhost', type: 'host', name: 'localhost', state: 'STARTED', runtimes: [{id:'localhost,/wlp', name:'localhost,/wlp', type:'runtime'}], servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, apps: { up: 1, down: 0, unknown: 0, partial: 0, list: ['cluster1,app1']}});
      setCachedServer({ id: 'localhost,/wlp/usr,server1', type: 'server', name: 'localhost,/wlp/usr,server1', state: 'STARTED', wlpInstallDir: '/wlp', cluster: 'cluster1', apps: { up: 1, down: 0, unknown: 0, list: [{name: 'cluster1,app1', state: 'STARTED'}]}});
      setCachedCluster({ id: 'cluster1', type: 'cluster', name: 'cluster1', state: 'STARTED', servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, apps: { up: 1, down: 0, unknown: 0, partial: 0, list: [ {name: 'cluster1,app1'} ] }});
      setCachedApplication({ id: 'cluster1,app1', type: 'application', name: 'app1', state: 'STARTED', up: 1, down: 0, unknown: 0, servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, clusters: { up: 1, down: 0, unknown: 0, partial: 0, list: ['cluster1']}});

      var now = clone(emptyCollective);

      unified.__sendUnifiedNotifications(now);

      validateSummaryEvent(summaryEvent, { applications: { up: 0, down: 0, unknown: 0, partial: 0 }, clusters: { up: 0, down: 0, unknown: 0, partial: 0 }, servers: { up: 0, down: 0, unknown: 0 }, hosts: { up: 0, down: 0, unknown: 0, partial: 0, empty: 0 } });
      validateEventWasNotReceived('alerts', alertsEvent);
      validateHostsEvent(hostsEvent, { up: 0, down: 0, unknown: 0, partial: 0, empty: 0, removed: ['localhost'] });
      validateServersEvent(serversEvent, { up: 0, down: 0, unknown: 0, removed: ['localhost,/wlp/usr,server1'] });
      validateClustersEvent(clustersEvent, { up: 0, down: 0, unknown: 0, partial: 0, removed: ['cluster1'] });
      validateApplicationsEvent(applicationsEvent, { up: 0, down: 0, unknown: 0, partial: 0, removed: ['cluster1,app1'] });

      validateHostEvent('localhost', hostEvent, { state: 'removed' });
      validateServerEvent('localhost,/wlp/usr,server1', serverEvent, { state: 'removed' });
      validateClusterEvent('cluster1', clusterEvent, { state: 'removed' });
      validateApplicationEvent('cluster1,app1', appEvent, { state: 'removed' });
    });

    /**
     * An application on a non-clustered server changing state from up to down should produce the following events:
     * - host event - updated app tallies
     * - server event - updated app tallies and app changed
     * - applications - updated tallies
     * - application event - updated tallies
     */
    tdd.test('11a. App changes state from up to down on a non-clustered server', function() {
      var summaryEvent = null;
      addTopicListener(topicUtil.getTopicByType('summary'),  function(e) {
        summaryEvent = e;
      });

      var alertsEvent = null;
      addTopicListener(topicUtil.getTopicByType('alerts'),  function(e) {
        alertsEvent = e;
      });

      var hostsEvent = null;
      addTopicListener(topicUtil.getTopicByType('hosts'),  function(e) {
        hostsEvent = e;
      });

      var hostEvent = null;
      addTopicListener(topicUtil.getTopicByType('host', 'localhost'),  function(e) {
        hostEvent = e;
      });

      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      var serverEvent = null;
      addTopicListener(topicUtil.getTopicByType('server', 'localhost,/wlp/usr,server1'),  function(e) {
        serverEvent = e;
      });

      var clustersEvent = null;
      addTopicListener(topicUtil.getTopicByType('clusters'),  function(e) {
        clustersEvent = e;
      });

      var clusterEvent = null;
      addTopicListener(topicUtil.getTopicByType('cluster', 'cluster1'),  function(e) {
        clusterEvent = e;
      });

      var applicationsEvent = null;
      addTopicListener(topicUtil.getTopicByType('applications'),  function(e) {
        applicationsEvent = e;
      });

      var appEvent = null;
      addTopicListener(topicUtil.getTopicByType('application', 'localhost,/wlp/usr,server1,app1'),  function(e) {
        appEvent = e;
      });

      // cached = oneRunningServerWithOneApp
      setCachedApplications({ up: 1, down: 0, unknown: 0, partial: 0, list: ['localhost,/wlp/usr,server1,app1']});
      setCachedClusters({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedServers({ up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']});
      setCachedHosts({ up: 1, down: 0, unknown: 0, partial: 0, empty: 0, list: ['localhost']});
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedSummaryAndAlerts();
      setCachedHost({ id: 'localhost', type: 'host', name: 'localhost', state: 'STARTED', runtimes: {list: [{id:'localhost,/wlp', name:'localhost,/wlp', type:'runtime'}]}, servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, apps: { up: 1, down: 0, unknown: 0, partial: 0, list: ['localhost,/wlp/usr,server1,app1']}});
      setCachedServer({ id: 'localhost,/wlp/usr,server1', type: 'server', name: 'localhost,/wlp/usr,server1', state: 'STARTED', wlpInstallDir: '/wlp', apps: { up: 1, down: 0, unknown: 0, list: [{id: 'localhost,/wlp/usr,server1,app1', name: 'app1', state: 'STARTED'}]}});
      setCachedApplication({ id: 'localhost,/wlp/usr,server1,app1', type: 'application', name: 'app1', state: 'STARTED', up: 1, down: 0, unknown: 0, servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, clusters: { up: 0, down: 0, unknown: 0, partial: 0, list: []}});

      var now = clone(oneRunningServerWithOneStoppedApp);

      unified.__sendUnifiedNotifications(now);

      validateSummaryEvent(summaryEvent, { applications: { up: 0, down: 1, unknown: 0, partial: 0 } });
      validateAlertsEvent(alertsEvent, { count: 1, unknown: [], app: [ {name: "localhost,/wlp/usr,server1,app1", servers:["localhost,/wlp/usr,server1"]} ] });
      validateEventWasNotReceived('hosts', hostsEvent);
      validateEventWasNotReceived('servers', serversEvent);
      validateClustersEvent(clustersEvent, { up: 0, down: 0, unknown: 0, partial: 0 });
      validateApplicationsEvent(applicationsEvent, { up: 0, down: 1, unknown: 0, partial: 0 });

      validateHostEvent('localhost', hostEvent, { alerts: {count :1, unknown:[], app:[{name:"localhost,/wlp/usr,server1,app1", servers:["localhost,/wlp/usr,server1"]}]}});
      validateServerEvent('localhost,/wlp/usr,server1', serverEvent, { alerts: {count :1, unknown:[], app:[{name:"localhost,/wlp/usr,server1,app1", servers:["localhost,/wlp/usr,server1"]}]},
        apps: { up: 0, down: 1, unknown: 0, partial: 0, changed: [{ id: 'localhost,/wlp/usr,server1,app1', name: 'app1', state: 'STOPPED'}] } });
      validateEventWasNotReceived('cluster', clusterEvent);
      validateApplicationEvent('localhost,/wlp/usr,server1,app1', appEvent, { alerts: {count :1, unknown:[], app:[{name:"localhost,/wlp/usr,server1,app1", servers:["localhost,/wlp/usr,server1"]}]},
        state: 'STOPPED', up: 0, down: 1, unknown: 0 });
    });

    /**
     * An application on a non-clustered server changing state from down to up should produce the following events:
     * - host event - updated app tallies
     * - server event - updated app tallies and app changed
     * - application event - updated tallies
     */
    tdd.test('11b. App changes state from down to up on a non-clustered server', function() {
      var summaryEvent = null;
      addTopicListener(topicUtil.getTopicByType('summary'),  function(e) {
        summaryEvent = e;
      });

      var alertsEvent = null;
      addTopicListener(topicUtil.getTopicByType('alerts'),  function(e) {
        alertsEvent = e;
      });

      var hostsEvent = null;
      addTopicListener(topicUtil.getTopicByType('hosts'),  function(e) {
        hostsEvent = e;
      });

      var hostEvent = null;
      addTopicListener(topicUtil.getTopicByType('host', 'localhost'),  function(e) {
        hostEvent = e;
      });

      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      var serverEvent = null;
      addTopicListener(topicUtil.getTopicByType('server', 'localhost,/wlp/usr,server1'),  function(e) {
        serverEvent = e;
      });

      var clustersEvent = null;
      addTopicListener(topicUtil.getTopicByType('clusters'),  function(e) {
        clustersEvent = e;
      });

      var clusterEvent = null;
      addTopicListener(topicUtil.getTopicByType('cluster', 'cluster1'),  function(e) {
        clusterEvent = e;
      });

      var applicationsEvent = null;
      addTopicListener(topicUtil.getTopicByType('applications'),  function(e) {
        applicationsEvent = e;
      });

      var appEvent = null;
      addTopicListener(topicUtil.getTopicByType('application', 'localhost,/wlp/usr,server1,app1'),  function(e) {
        appEvent = e;
      });

      // cached = oneRunningServerWithOneStoppedApp
      setCachedApplications({ up: 0, down: 1, unknown: 0, partial: 0, list: ['localhost,/wlp/usr,server1,app1']});
      setCachedClusters({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedServers({ up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']});
      setCachedHosts({ up: 1, down: 0, unknown: 0, partial: 0, empty: 0, list: ['localhost']});
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedSummaryAndAlerts();
      setCachedHost({ id: 'localhost', type: 'host', name: 'localhost', state: 'STARTED', runtimes: {list: [{id:'localhost,/wlp', name:'localhost,/wlp', type:'runtime'}]}, servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, apps: { up: 0, down: 1, unknown: 0, partial: 0, list: ['localhost,/wlp/usr,server1,app1']}});
      setCachedServer({ id: 'localhost,/wlp/usr,server1', type: 'server', name: 'localhost,/wlp/usr,server1', state: 'STARTED', wlpInstallDir: '/wlp', apps: { up: 0, down: 1, unknown: 0, list: [{id: 'localhost,/wlp/usr,server1', name: 'app1', state: 'STOPPED'}]}});
      setCachedApplication({ id: 'localhost,/wlp/usr,server1,app1', type: 'application', name: 'app1', state: 'STOPPED', up: 0, down: 1, unknown: 0, 
        servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, 
        clusters: { up: 0, down: 0, unknown: 0, partial: 0, list: []},
        alerts: {count :1, unknown:[], app:[{name:"localhost,/wlp/usr,server1,app1", servers:["localhost,/wlp/usr,server1"]}]} });

      var now = clone(oneRunningServerWithOneApp);

      unified.__sendUnifiedNotifications(now);

      validateSummaryEvent(summaryEvent, { applications: { up: 1, down: 0, unknown: 0, partial: 0 } });
      validateEventWasNotReceived('alerts', alertsEvent);
      validateEventWasNotReceived('hosts', hostsEvent);
      validateEventWasNotReceived('servers', serversEvent);
      validateEventWasNotReceived('clusters', clustersEvent);
      validateApplicationsEvent(applicationsEvent, { up: 1, down: 0, unknown: 0, partial: 0 });

      validateEventWasNotReceived('localhost', hostEvent);
      validateServerEvent('localhost,/wlp/usr,server1', serverEvent, { apps: { up: 1, down: 0, unknown: 0, partial: 0, changed: [{ id: 'localhost,/wlp/usr,server1,app1', name: 'app1', state: 'STARTED'}] } });
      validateEventWasNotReceived('cluster', clusterEvent);
      validateApplicationEvent('localhost,/wlp/usr,server1,app1', appEvent, { alerts: {count: 0, unknown:[], app:[]}, state: 'STARTED', up: 1, down: 0, unknown: 0 });
    });

    /**
     * An application on a clustered server changing state from up to down should produce the following events:
     * - host event - updated app tallies
     * - server event - updated app tallies and app changed
     * - cluster event - updated app tallies
     * - application event - updated tallies
     */
    tdd.test('11c. App changes state from up to down on a clustered server', function() {
      var summaryEvent = null;
      addTopicListener(topicUtil.getTopicByType('summary'),  function(e) {
        summaryEvent = e;
      });

      var alertsEvent = null;
      addTopicListener(topicUtil.getTopicByType('alerts'),  function(e) {
        alertsEvent = e;
      });

      var hostsEvent = null;
      addTopicListener(topicUtil.getTopicByType('hosts'),  function(e) {
        hostsEvent = e;
      });

      var hostEvent = null;
      addTopicListener(topicUtil.getTopicByType('host', 'localhost'),  function(e) {
        hostEvent = e;
      });

      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      var serverEvent = null;
      addTopicListener(topicUtil.getTopicByType('server', 'localhost,/wlp/usr,server1'),  function(e) {
        serverEvent = e;
      });

      var clustersEvent = null;
      addTopicListener(topicUtil.getTopicByType('clusters'),  function(e) {
        clustersEvent = e;
      });

      var clusterEvent = null;
      addTopicListener(topicUtil.getTopicByType('cluster', 'cluster1'),  function(e) {
        clusterEvent = e;
      });

      var applicationsEvent = null;
      addTopicListener(topicUtil.getTopicByType('applications'),  function(e) {
        applicationsEvent = e;
      });

      var appEvent = null;
      addTopicListener(topicUtil.getTopicByType('application', 'cluster1,app1'),  function(e) {
        appEvent = e;
      });

      // cached = oneRunningClusteredServerWithOneApp
      setCachedApplications({ up: 1, down: 0, unknown: 0, partial: 0, list: ['cluster1,app1']});
      setCachedClusters({ up: 1, down: 0, unknown: 0, partial: 0, list: ['cluster1']});
      setCachedServers({ up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']});
      setCachedHosts({ up: 1, down: 0, unknown: 0, partial: 0, empty: 0, list: ['localhost']});
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedSummaryAndAlerts();
      setCachedHost({ id: 'localhost', type: 'host', name: 'localhost', state: 'STARTED', runtimes: {list: [{id:'localhost,/wlp', name:'localhost,/wlp', type:'runtime'}]}, servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, apps: { up: 1, down: 0, unknown: 0, partial: 0, list: ['cluster1,app1']}});
      setCachedServer({ id: 'localhost,/wlp/usr,server1', type: 'server', name: 'localhost,/wlp/usr,server1', state: 'STARTED', wlpInstallDir: '/wlp', cluster: 'cluster1', apps: { up: 1, down: 0, unknown: 0, list: [{id: 'cluster1,app1', name: 'app1', state: 'STARTED'}]}});
      setCachedCluster({ id: 'cluster1', type: 'cluster', name: 'cluster1', state: 'STARTED',
        servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']},
        apps: { up: 1, down: 0, unknown: 0, partial: 0, list: 
          [{ id: 'cluster1,app1', name: 'app1', state: 'STARTED', up: 1, down: 0, unknown: 0, servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ] } }]
        }});
      setCachedApplication({ id: 'cluster1,app1', type: 'application', name: 'app1', state: 'STARTED', up: 1, down: 0, unknown: 0, servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, clusters: { up: 1, down: 0, unknown: 0, partial: 0, list: ['cluster1']}});

      var now = clone(oneRunningClusteredServerWithOneStoppedApp);

      unified.__sendUnifiedNotifications(now);

      validateSummaryEvent(summaryEvent, { applications: { up: 0, down: 1, unknown: 0, partial: 0 } });
      validateAlertsEvent(alertsEvent, { count: 1, unknown: [], app: [ {name: "app1", servers:["localhost,/wlp/usr,server1"]} ] });
      validateEventWasNotReceived('hosts', hostsEvent);
      validateEventWasNotReceived('servers', serversEvent);
      validateClustersEvent(clustersEvent, { up: 1, down: 0, unknown: 0, partial: 0 });
      validateApplicationsEvent(applicationsEvent, {up: 0, down: 1, unknown: 0, partial: 0 });

      validateHostEvent('localhost', hostEvent, { alerts: {count: 1,  unknown:[], app:[{name:'cluster1,app1',servers:['localhost,/wlp/usr,server1']}]} });
      validateServerEvent('localhost,/wlp/usr,server1', serverEvent, { apps: { up: 0, down: 1, unknown: 0, changed: [{ id: 'cluster1,app1', name: 'app1', state: 'STOPPED'}] },
        alerts: {count :1, unknown:[], app:[{name:"cluster1,app1", servers:["localhost,/wlp/usr,server1"]}]} });
      validateClusterEvent('cluster1', clusterEvent, { apps: { up: 0, down: 1, unknown: 0, partial: 0 }, alerts: { count: 1,  unknown:[], app:[{name:'cluster1,app1',servers:['localhost,/wlp/usr,server1']}]} });
      validateApplicationEvent('cluster1,app1', appEvent, { state: 'STOPPED', up: 0, down: 1, unknown: 0,  alerts: {count: 1,  unknown:[], app:[{name:'cluster1,app1',servers:['localhost,/wlp/usr,server1']}]} });
    });

    /**
     * An application on a clustered server changing state from down to up should produce the following events:
     * - host event - updated app tallies
     * - server event - updated app tallies and app changed
     * - cluster event - updated app tallies
     * - application event - updated tallies
     */
    tdd.test('11d. App changes state from down to up on a clustered server', function() {
      var summaryEvent = null;
      addTopicListener(topicUtil.getTopicByType('summary'),  function(e) {
        summaryEvent = e;
      });

      var alertsEvent = null;
      addTopicListener(topicUtil.getTopicByType('alerts'),  function(e) {
        alertsEvent = e;
      });

      var hostsEvent = null;
      addTopicListener(topicUtil.getTopicByType('hosts'),  function(e) {
        hostsEvent = e;
      });

      var hostEvent = null;
      addTopicListener(topicUtil.getTopicByType('host', 'localhost'),  function(e) {
        hostEvent = e;
      });

      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      var serverEvent = null;
      addTopicListener(topicUtil.getTopicByType('server', 'localhost,/wlp/usr,server1'),  function(e) {
        serverEvent = e;
      });

      var clustersEvent = null;
      addTopicListener(topicUtil.getTopicByType('clusters'),  function(e) {
        clustersEvent = e;
      });

      var clusterEvent = null;
      addTopicListener(topicUtil.getTopicByType('cluster', 'cluster1'),  function(e) {
        clusterEvent = e;
      });

      var applicationsEvent = null;
      addTopicListener(topicUtil.getTopicByType('applications'),  function(e) {
        applicationsEvent = e;
      });

      var appEvent = null;
      addTopicListener(topicUtil.getTopicByType('application', 'cluster1,app1'),  function(e) {
        appEvent = e;
      });

      // cached = oneRunningClusteredServerWithOneStoppedApp
      setCachedApplications({ up: 0, down: 1, unknown: 0, partial: 0, list: ['cluster1,app1']});
      setCachedClusters({ up: 1, down: 0, unknown: 0, partial: 0, list: ['cluster1']});
      setCachedServers({ up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']});
      setCachedHosts({ up: 1, down: 0, unknown: 0, partial: 0, empty: 0, list: ['localhost']});
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedSummaryAndAlerts();
      setCachedHost({ id: 'localhost', type: 'host', name: 'localhost', state: 'STARTED', runtimes: {list: [{id:'localhost,/wlp', name:'localhost,/wlp', type:'runtime'}]}, servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, apps: { up: 0, down: 1, unknown: 0, partial: 0, list: ['cluster1,app1']}});
      setCachedServer({ id: 'localhost,/wlp/usr,server1', type: 'server', name: 'localhost,/wlp/usr,server1', state: 'STARTED', wlpInstallDir: '/wlp', cluster: 'cluster1', apps: { up: 0, down: 1, unknown: 0, list: [{id: 'cluster1,app1', name: 'app1', state: 'STOPPED'}]}});
      setCachedCluster({ id: 'cluster1', type: 'cluster', name: 'cluster1', state: 'STARTED', 
        servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, 
        apps: { up: 0, down: 1, unknown: 0, partial: 0, list: 
          [{ id: 'cluster1,app1', name: 'app1', state: 'STOPPED', up: 0, down: 1, unknown: 0, servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ] } }]
        }});
      setCachedApplication({ id: 'cluster1,app1', type: 'application', name: 'app1', state: 'STOPPED', up: 0, down: 1, unknown: 0, 
        servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, 
        clusters: { up: 1, down: 0, unknown: 0, partial: 0, list: ['cluster1']},
        alerts: { count: 1, unknown: [], app: [ {name: "cluster1,app1", servers:["localhost,/wlp/usr,server1"]} ] }});

      var now = clone(oneRunningClusteredServerWithOneApp);

      unified.__sendUnifiedNotifications(now);

      validateSummaryEvent(summaryEvent, { applications: { up: 1, down: 0, unknown: 0, partial: 0 } });
      validateEventWasNotReceived('alerts', alertsEvent);
      validateEventWasNotReceived('hosts', hostsEvent);
      validateEventWasNotReceived('servers', serversEvent);
      validateEventWasNotReceived('clusters', clustersEvent);
      validateApplicationsEvent(applicationsEvent, { up: 1, down: 0, unknown: 0, partial: 0 });

      validateEventWasNotReceived('localhost', hostEvent);
      validateServerEvent('localhost,/wlp/usr,server1', serverEvent, { apps: { up: 1, down: 0, unknown: 0, partial: 0, changed: [{ id: 'cluster1,app1', name: 'app1', state: 'STARTED'}] } });
      validateClusterEvent('cluster1', clusterEvent, { apps: { up: 1, down: 0, unknown: 0, partial: 0 } });
      validateApplicationEvent('cluster1,app1', appEvent, { alerts: {count: 0, unknown:[], app:[]}, state: 'STARTED', up: 1, down: 0, unknown: 0 });
    });

    /**
     * An application on a clustered server should produce no events when nothing changes.
     */
    tdd.test('11e. App on clustered server with no change should produce no events', function() {
      var summaryEvent = null;
      addTopicListener(topicUtil.getTopicByType('summary'),  function(e) {
        summaryEvent = e;
      });

      var alertsEvent = null;
      addTopicListener(topicUtil.getTopicByType('alerts'),  function(e) {
        alertsEvent = e;
      });

      var hostsEvent = null;
      addTopicListener(topicUtil.getTopicByType('hosts'),  function(e) {
        hostsEvent = e;
      });

      var runtimesEvent = null;
      addTopicListener(topicUtil.getTopicByType('runtimes'),  function(e) {
        runtimesEvent = e;
      });

      var hostEvent = null;
      addTopicListener(topicUtil.getTopicByType('host', 'localhost'),  function(e) {
        hostEvent = e;
      });

      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      var serverEvent = null;
      addTopicListener(topicUtil.getTopicByType('server', 'localhost,/wlp/usr,server1'),  function(e) {
        serverEvent = e;
      });

      var clustersEvent = null;
      addTopicListener(topicUtil.getTopicByType('clusters'),  function(e) {
        clustersEvent = e;
      });

      var clusterEvent = null;
      addTopicListener(topicUtil.getTopicByType('cluster', 'cluster1'),  function(e) {
        clusterEvent = e;
      });

      var applicationsEvent = null;
      addTopicListener(topicUtil.getTopicByType('applications'),  function(e) {
        applicationsEvent = e;
      });

      var appEvent = null;
      addTopicListener(topicUtil.getTopicByType('application', 'cluster1,app1'),  function(e) {
        appEvent = e;
      });

      // cached = oneRunningClusteredServerWithOneApp
      setCachedApplications({ up: 1, down: 0, unknown: 0, partial: 0, list: ['cluster1,app1']});
      setCachedClusters({ up: 1, down: 0, unknown: 0, partial: 0, list: ['cluster1']});
      setCachedServers({ up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']});
      setCachedHosts({ up: 1, down: 0, unknown: 0, partial: 0, empty: 0, list: ['localhost']});
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedSummaryAndAlerts();
      setCachedHost({ id: 'localhost', type: 'host', name: 'localhost', state: 'STARTED', runtimes: {list: [{id:'localhost,/wlp', name:'localhost,/wlp', type:'runtime'}]}, servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, apps: { up: 1, down: 0, unknown: 0, partial: 0, list: ['cluster1,app1']}});
      setCachedServer({ id: 'localhost,/wlp/usr,server1', type: 'server', name: 'localhost,/wlp/usr,server1', state: 'STARTED', wlpInstallDir: '/wlp', cluster: 'cluster1', apps: { up: 0, down: 0, unknown: 0, list: [{id: 'cluster1,app1', name: 'app1', state: 'STARTED'}]}});
      setCachedCluster({ id: 'cluster1', type: 'cluster', name: 'cluster1', state: 'STARTED', 
        servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, 
        apps: { up: 1, down: 0, unknown: 0, partial: 0, list: 
          [{ id: 'cluster1,app1', name: 'app1', state: 'STARTED', up: 1, down: 0, unknown: 0, servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ] } }]
        }});
      setCachedApplication({ id: 'cluster1,app1', type: 'application', name: 'app1', state: 'STARTED', up: 1, down: 0, unknown: 0, servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, clusters: { up: 1, down: 0, unknown: 0, partial: 0, list: ['cluster1']}});

      var now = clone(oneRunningClusteredServerWithOneApp);

      unified.__sendUnifiedNotifications(now);

      validateEventWasNotReceived('summary', summaryEvent);
      validateEventWasNotReceived('alerts', alertsEvent);
      validateEventWasNotReceived('hosts', hostsEvent);
      validateEventWasNotReceived('servers', serversEvent);
      validateEventWasNotReceived('clusters', clustersEvent);
      validateEventWasNotReceived('applications', applicationsEvent);
      validateEventWasNotReceived('localhost', hostEvent);
      validateEventWasNotReceived('localhost,/wlp/usr,server1', serverEvent);
      validateEventWasNotReceived('cluster1', clusterEvent);
      validateEventWasNotReceived('app1', appEvent);
    });

    /**
     * Adding a scaling policy to a clustered app should produce the following events:
     * - server event - updated scaling policy
     * - cluster event - updated scaling policy
     * - application event - updated scaling policy
     */
    tdd.test('11f. Clustered app change gets a scaling policy', function() {
      var summaryEvent = null;
      addTopicListener(topicUtil.getTopicByType('summary'),  function(e) {
        summaryEvent = e;
      });

      var alertsEvent = null;
      addTopicListener(topicUtil.getTopicByType('alerts'),  function(e) {
        alertsEvent = e;
      });

      var hostsEvent = null;
      addTopicListener(topicUtil.getTopicByType('hosts'),  function(e) {
        hostsEvent = e;
      });

      var hostEvent = null;
      addTopicListener(topicUtil.getTopicByType('host', 'localhost'),  function(e) {
        hostEvent = e;
      });

      var runtimesEvent = null;
      addTopicListener(topicUtil.getTopicByType('runtimes'),  function(e) {
        runtimesEvent = e;
      });

      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      var serverEvent = null;
      addTopicListener(topicUtil.getTopicByType('server', 'localhost,/wlp/usr,server1'),  function(e) {
        serverEvent = e;
      });

      var clustersEvent = null;
      addTopicListener(topicUtil.getTopicByType('clusters'),  function(e) {
        clustersEvent = e;
      });

      var clusterEvent = null;
      addTopicListener(topicUtil.getTopicByType('cluster', 'cluster1'),  function(e) {
        clusterEvent = e;
      });

      var applicationsEvent = null;
      addTopicListener(topicUtil.getTopicByType('applications'),  function(e) {
        applicationsEvent = e;
      });

      var appEvent = null;
      addTopicListener(topicUtil.getTopicByType('application', 'cluster1,app1'),  function(e) {
        appEvent = e;
      });

      // cached = oneRunningClusteredServerWithOneApp
      setCachedApplications({ up: 1, down: 0, unknown: 0, partial: 0, list: ['cluster1,app1']});
      setCachedClusters({ up: 1, down: 0, unknown: 0, partial: 0, list: ['cluster1']});
      setCachedServers({ up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']});
      setCachedHosts({ up: 1, down: 0, unknown: 0, partial: 0, empty: 0, list: ['localhost']});
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedSummaryAndAlerts();
      setCachedHost({ id: 'localhost', type: 'host', name: 'localhost', state: 'STARTED', runtimes: {list: [{id:'localhost,/wlp', name:'localhost,/wlp', type:'runtime'}]}, servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, apps: { up: 1, down: 0, unknown: 0, partial: 0, list: ['cluster1,app1']}});
      setCachedServer({ id: 'localhost,/wlp/usr,server1', type: 'server', name: 'localhost,/wlp/usr,server1', state: 'STARTED', wlpInstallDir: '/wlp', cluster: 'cluster1', apps: { up: 0, down: 0, unknown: 0, list: [{id: 'cluster1,app1', name: 'app1', state: 'STARTED'}]}});
      setCachedCluster({ id: 'cluster1', type: 'cluster', name: 'cluster1', state: 'STARTED', 
        servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, 
        apps: { up: 1, down: 0, unknown: 0, partial: 0, list: 
          [{ id: 'cluster1,app1', name: 'app1', state: 'STARTED', up: 1, down: 0, unknown: 0, servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ] } }]
        }});
      setCachedApplication({ id: 'cluster1,app1', type: 'application', name: 'app1', state: 'STARTED', up: 1, down: 0, unknown: 0, servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, clusters: { up: 1, down: 0, unknown: 0, partial: 0, list: ['cluster1']}});

      var now = clone(oneRunningClusteredServerWithOneAppWithScalingPolicy);

      unified.__sendUnifiedNotifications(now);

      validateEventWasNotReceived('summary', summaryEvent);
      validateEventWasNotReceived('alerts', alertsEvent);
      validateEventWasNotReceived('hosts', hostsEvent);
      validateEventWasNotReceived('servers', serversEvent);
      validateEventWasNotReceived('clusters', clustersEvent);
      validateEventWasNotReceived('applications', applicationsEvent);

      validateEventWasNotReceived('localhost', hostEvent);
      validateServerEvent('localhost,/wlp/usr,server1', serverEvent, { scalingPolicy: 'default' });
      validateClusterEvent('cluster1', clusterEvent, { scalingPolicy: 'default'});
      validateApplicationEvent('cluster1,app1', appEvent, { state: 'STARTED',  up: 1, down: 0, unknown: 0, scalingPolicy: true });
    });

    /**
     * Changing the scaling policy of a clustered app should produce the following events:
     * - server event - updated scaling policy
     * - cluster event - updated scaling policy
     * 
     * The application event should not be fired because changing the policy for the application group does not matter.
     * The scaling policy for the group is a boolean flag, its either set or its not. Changing the policy name is
     * therefore irrelevant.
     */
    tdd.test('11g. Clustered app changes scaling policy', function() {
      var summaryEvent = null;
      addTopicListener(topicUtil.getTopicByType('summary'),  function(e) {
        summaryEvent = e;
      });

      var alertsEvent = null;
      addTopicListener(topicUtil.getTopicByType('alerts'),  function(e) {
        alertsEvent = e;
      });

      var hostsEvent = null;
      addTopicListener(topicUtil.getTopicByType('hosts'),  function(e) {
        hostsEvent = e;
      });

      var hostEvent = null;
      addTopicListener(topicUtil.getTopicByType('host', 'localhost'),  function(e) {
        hostEvent = e;
      });

      var runtimesEvent = null;
      addTopicListener(topicUtil.getTopicByType('runtimes'),  function(e) {
        runtimesEvent = e;
      });

      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      var serverEvent = null;
      addTopicListener(topicUtil.getTopicByType('server', 'localhost,/wlp/usr,server1'),  function(e) {
        serverEvent = e;
      });

      var clustersEvent = null;
      addTopicListener(topicUtil.getTopicByType('clusters'),  function(e) {
        clustersEvent = e;
      });

      var clusterEvent = null;
      addTopicListener(topicUtil.getTopicByType('cluster', 'cluster1'),  function(e) {
        clusterEvent = e;
      });

      var applicationsEvent = null;
      addTopicListener(topicUtil.getTopicByType('applications'),  function(e) {
        applicationsEvent = e;
      });

      var appEvent = null;
      addTopicListener(topicUtil.getTopicByType('application', 'cluster1,app1'),  function(e) {
        appEvent = e;
      });

      // cached = oneRunningClusteredServerWithOneAppWithScalingPolicy
      setCachedApplications({ up: 1, down: 0, unknown: 0, partial: 0, list: ['cluster1,app1']});
      setCachedClusters({ up: 1, down: 0, unknown: 0, partial: 0, list: ['cluster1']});
      setCachedServers({ up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']});
      setCachedHosts({ up: 1, down: 0, unknown: 0, partial: 0, empty: 0, list: ['localhost']});
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedSummaryAndAlerts();
      setCachedHost({ id: 'localhost', type: 'host', name: 'localhost', state: 'STARTED', runtimes: {list: [{id:'localhost,/wlp', name:'localhost,/wlp', type:'runtime'}]}, servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, apps: { up: 1, down: 0, unknown: 0, partial: 0, list: ['cluster1,app1']}});
      setCachedServer({ id: 'localhost,/wlp/usr,server1', type: 'server', name: 'localhost,/wlp/usr,server1', state: 'STARTED', wlpInstallDir: '/wlp', cluster: 'cluster1', scalingPolicy: 'default', apps: { up: 0, down: 0, unknown: 0, list: [{id: 'cluster1,app1', name: 'app1', state: 'STARTED'}]}});
      setCachedCluster({ id: 'cluster1', type: 'cluster', name: 'cluster1', state: 'STARTED', scalingPolicy: 'default',
        servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']},
        apps: { up: 1, down: 0, unknown: 0, partial: 0, list: 
          [{ id: 'cluster1,app1', name: 'app1', state: 'STARTED', up: 1, down: 0, unknown: 0, servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ] } }]
        }});
      setCachedApplication({ id: 'cluster1,app1', type: 'application', name: 'app1', state: 'STARTED', up: 1, down: 0, unknown: 0, scalingPolicy: true, scalingPolicyEnabled: false, servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, clusters: { up: 1, down: 0, unknown: 0, partial: 0, list: ['cluster1']}});

      var now = clone(oneRunningClusteredServerWithOneAppWithNewScalingPolicy);

      unified.__sendUnifiedNotifications(now);

      validateEventWasNotReceived('summary', summaryEvent);
      validateEventWasNotReceived('alerts', alertsEvent);
      validateEventWasNotReceived('hosts', hostsEvent);
      validateEventWasNotReceived('servers', serversEvent);
      validateEventWasNotReceived('clusters', clustersEvent);
      validateEventWasNotReceived('applications', applicationsEvent);

      validateEventWasNotReceived('localhost', hostEvent);
      validateServerEvent('localhost,/wlp/usr,server1', serverEvent, { scalingPolicy: 'newPolicy' });
      validateClusterEvent('cluster1', clusterEvent, { scalingPolicy: 'newPolicy'});
      validateEventWasNotReceived('app1', appEvent);
    });

    /**
     * Removing the scaling policy from a clustered app should produce the following events:
     * - server event - updated scaling policy
     * - cluster event - updated scaling policy
     * - application event - updated scaling policy
     */
    tdd.test('11h. Clustered app change removes the scaling policy', function() {
      var summaryEvent = null;
      addTopicListener(topicUtil.getTopicByType('summary'),  function(e) {
        summaryEvent = e;
      });

      var alertsEvent = null;
      addTopicListener(topicUtil.getTopicByType('alerts'),  function(e) {
        alertsEvent = e;
      });

      var hostsEvent = null;
      addTopicListener(topicUtil.getTopicByType('hosts'),  function(e) {
        hostsEvent = e;
      });

      var hostEvent = null;
      addTopicListener(topicUtil.getTopicByType('host', 'localhost'),  function(e) {
        hostEvent = e;
      });

      var runtimesEvent = null;
      addTopicListener(topicUtil.getTopicByType('runtimes'),  function(e) {
        runtimesEvent = e;
      });

      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      var serverEvent = null;
      addTopicListener(topicUtil.getTopicByType('server', 'localhost,/wlp/usr,server1'),  function(e) {
        serverEvent = e;
      });

      var clustersEvent = null;
      addTopicListener(topicUtil.getTopicByType('clusters'),  function(e) {
        clustersEvent = e;
      });

      var clusterEvent = null;
      addTopicListener(topicUtil.getTopicByType('cluster', 'cluster1'),  function(e) {
        clusterEvent = e;
      });

      var applicationsEvent = null;
      addTopicListener(topicUtil.getTopicByType('applications'),  function(e) {
        applicationsEvent = e;
      });

      var appEvent = null;
      addTopicListener(topicUtil.getTopicByType('application', 'cluster1,app1'),  function(e) {
        appEvent = e;
      });

      // cached = oneRunningClusteredServerWithOneAppWithScalingPolicy
      setCachedApplications({ up: 1, down: 0, unknown: 0, partial: 0, list: ['cluster1,app1']});
      setCachedClusters({ up: 1, down: 0, unknown: 0, partial: 0, list: ['cluster1']});
      setCachedServers({ up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']});
      setCachedHosts({ up: 1, down: 0, unknown: 0, partial: 0, empty: 0, list: ['localhost']});
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedSummaryAndAlerts();
      setCachedHost({ id: 'localhost', type: 'host', name: 'localhost', state: 'STARTED', runtimes: {list: [{id:'localhost,/wlp', name:'localhost,/wlp', type:'runtime'}]}, servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, apps: { up: 1, down: 0, unknown: 0, partial: 0, list: ['cluster1,app1']}});
      setCachedServer({ id: 'localhost,/wlp/usr,server1', type: 'server', name: 'localhost,/wlp/usr,server1', state: 'STARTED', wlpInstallDir: '/wlp', cluster: 'cluster1', scalingPolicy: 'default', apps: { up: 0, down: 0, unknown: 0, list: [{id: 'cluster1,app1', name: "app1", state: 'STARTED'}]}});
      setCachedCluster({ id: 'cluster1', type: 'cluster', name: 'cluster1', state: 'STARTED', scalingPolicy: 'default',
        servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']},
        apps: { up: 1, down: 0, unknown: 0, partial: 0, list: 
          [{ id: 'cluster1,app1', name: 'app1', state: 'STARTED', up: 1, down: 0, unknown: 0, servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ] } }]
        }});
      setCachedApplication({ id: 'cluster1,app1', type: 'application', name: 'app1', state: 'STARTED', up: 1, down: 0, unknown: 0, scalingPolicy: true, servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, clusters: { up: 1, down: 0, unknown: 0, partial: 0, list: ['cluster1']}});

      var now = clone(oneRunningClusteredServerWithOneApp);

      unified.__sendUnifiedNotifications(now);

      validateEventWasNotReceived('summary', summaryEvent);
      validateEventWasNotReceived('alerts', alertsEvent);
      validateEventWasNotReceived('hosts', hostsEvent);
      validateEventWasNotReceived('servers', serversEvent);
      validateEventWasNotReceived('clusters', clustersEvent);
      validateEventWasNotReceived('applications', applicationsEvent);

      validateEventWasNotReceived('localhost', hostEvent);     
      validateServerEvent('localhost,/wlp/usr,server1', serverEvent, { scalingPolicy: null });
      validateClusterEvent('cluster1', clusterEvent, { scalingPolicy: null});
      validateApplicationEvent('cluster1,app1', appEvent, { state: 'STARTED',  up: 1, down: 0, unknown: 0, scalingPolicy: false });
    });

    /**
     * An application being added to a non-clustered server should produce the following events:
     * - applications - updated tallies and app added
     * - host event - updated app tallies and app added
     * - server event - updated app tallies and app added
     * - application event - updated tallies and app added
     */
    tdd.test('12a. App added to a non-clustered server', function() {
      var summaryEvent = null;
      addTopicListener(topicUtil.getTopicByType('summary'),  function(e) {
        summaryEvent = e;
      });

      var alertsEvent = null;
      addTopicListener(topicUtil.getTopicByType('alerts'),  function(e) {
        alertsEvent = e;
      });

      var hostsEvent = null;
      addTopicListener(topicUtil.getTopicByType('hosts'),  function(e) {
        hostsEvent = e;
      });

      var hostEvent = null;
      addTopicListener(topicUtil.getTopicByType('host', 'localhost'),  function(e) {
        hostEvent = e;
      });

      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      var serverEvent = null;
      addTopicListener(topicUtil.getTopicByType('server', 'localhost,/wlp/usr,server1'),  function(e) {
        serverEvent = e;
      });

      var clustersEvent = null;
      addTopicListener(topicUtil.getTopicByType('clusters'),  function(e) {
        clustersEvent = e;
      });

      var clusterEvent = null;
      addTopicListener(topicUtil.getTopicByType('cluster', 'cluster1'),  function(e) {
        clusterEvent = e;
      });

      var applicationsEvent = null;
      addTopicListener(topicUtil.getTopicByType('applications'),  function(e) {
        applicationsEvent = e;
      });

      var appEvent = null;
      addTopicListener(topicUtil.getTopicByType('application', 'localhost,/wlp/usr,server1,app1'),  function(e) {
        appEvent = e;
      });

      // cached = oneRunningServerWithNoApps
      setCachedApplications({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedClusters({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedServers({ up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']});
      setCachedHosts({ up: 1, down: 0, unknown: 0, partial: 0, empty: 0, list: ['localhost']});
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedSummaryAndAlerts();
      setCachedHost({ id: 'localhost', type: 'host', name: 'localhost', state: 'STARTED', runtimes: {list: [{id:'localhost,/wlp', name:'localhost,/wlp', type:'runtime'}]}, servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}});
      setCachedServer({ id: 'localhost,/wlp/usr,server1', type: 'server', name: 'localhost,/wlp/usr,server1', state: 'STARTED', wlpInstallDir: '/wlp', apps: { up: 0, down: 0, unknown: 0, list: []}});

      var now = clone(oneRunningServerWithOneApp);

      unified.__sendUnifiedNotifications(now);

      validateSummaryEvent(summaryEvent, { applications: { up: 1, down: 0, unknown: 0, partial: 0 } });
      validateEventWasNotReceived('alerts', alertsEvent);
      validateEventWasNotReceived('hosts', hostsEvent);
      validateEventWasNotReceived('servers', serversEvent);
      validateEventWasNotReceived('clusters', clustersEvent);
      validateApplicationsEvent(applicationsEvent, { up: 1, down: 0, unknown: 0, partial: 0, added: ['localhost,/wlp/usr,server1,app1'] });

      validateEventWasNotReceived('localhost', hostEvent);
      validateServerEvent('localhost,/wlp/usr,server1', serverEvent, { apps: { up: 1, down: 0, unknown: 0, partial: 0, added: [{ id: 'localhost,/wlp/usr,server1,app1', name: 'app1', state: 'STARTED'}] } });
      validateEventWasNotReceived('cluster', clusterEvent);
      validateEventWasNotReceived('app1', appEvent);
    });

    /**
     * An application being added to a clustered server should produce the following events:
     * - host event - updated app tallies and app added
     * - server event - updated app tallies and app added
     * - cluster event - updated app tallies and app added
     * - application event - updated tallies and app added
     */
    tdd.test('12b. App added to a clustered server', function() {
      var summaryEvent = null;
      addTopicListener(topicUtil.getTopicByType('summary'),  function(e) {
        summaryEvent = e;
      });

      var alertsEvent = null;
      addTopicListener(topicUtil.getTopicByType('alerts'),  function(e) {
        alertsEvent = e;
      });

      var hostsEvent = null;
      addTopicListener(topicUtil.getTopicByType('hosts'),  function(e) {
        hostsEvent = e;
      });

      var hostEvent = null;
      addTopicListener(topicUtil.getTopicByType('host', 'localhost'),  function(e) {
        hostEvent = e;
      });

      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      var serverEvent = null;
      addTopicListener(topicUtil.getTopicByType('server', 'localhost,/wlp/usr,server1'),  function(e) {
        serverEvent = e;
      });

      var clustersEvent = null;
      addTopicListener(topicUtil.getTopicByType('clusters'),  function(e) {
        clustersEvent = e;
      });

      var clusterEvent = null;
      addTopicListener(topicUtil.getTopicByType('cluster', 'cluster1'),  function(e) {
        clusterEvent = e;
      });

      var applicationsEvent = null;
      addTopicListener(topicUtil.getTopicByType('applications'),  function(e) {
        applicationsEvent = e;
      });

      var appEvent = null;
      addTopicListener(topicUtil.getTopicByType('application', 'cluster1,app1'),  function(e) {
        appEvent = e;
      });

      // cached = oneRunningClusteredServerWithNoApps
      setCachedApplications({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedClusters({ up: 1, down: 0, unknown: 0, partial: 0, list: ['cluster1']});
      setCachedServers({ up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']});
      setCachedHosts({ up: 1, down: 0, unknown: 0, partial: 0, empty: 0, list: ['localhost']});
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedSummaryAndAlerts();
      setCachedHost({ id: 'localhost', type: 'host', name: 'localhost', state: 'STARTED', runtimes: {list: [{id:'localhost,/wlp', name:'localhost,/wlp', type:'runtime'}]}, servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}});
      setCachedServer({ id: 'localhost,/wlp/usr,server1', type: 'server', name: 'localhost,/wlp/usr,server1', state: 'STARTED', wlpInstallDir: '/wlp', cluster: 'cluster1', apps: { up: 0, down: 0, unknown: 0, list: []}});
      setCachedCluster({ id: 'cluster1', type: 'cluster', name: 'cluster1', state: 'STARTED', servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, apps: { up: 0, down: 0, unknown: 0, partial: 0, list: []}});

      var now = clone(oneRunningClusteredServerWithOneApp);

      unified.__sendUnifiedNotifications(now);

      validateSummaryEvent(summaryEvent, { applications: { up: 1, down: 0, unknown: 0, partial: 0 } });
      validateEventWasNotReceived('alerts', alertsEvent);
      validateEventWasNotReceived('hosts', hostsEvent);
      validateEventWasNotReceived('servers', serversEvent);
      validateEventWasNotReceived('clusters', clustersEvent);
      validateApplicationsEvent(applicationsEvent, { up: 1, down: 0, unknown: 0, partial: 0, added: ['cluster1,app1'] });

      validateEventWasNotReceived('localhost', hostEvent);
      validateServerEvent('localhost,/wlp/usr,server1', serverEvent, { apps: { up: 1, down: 0, unknown: 0, partial: 0, added: [{ id: 'cluster1,app1', name: 'app1', state: 'STARTED'}] } });
      validateClusterEvent('cluster1', clusterEvent, { apps: { up: 1, down: 0, unknown: 0, partial: 0, added: [{ id: 'cluster1,app1', name: 'app1', state: 'STARTED', up: 1, down: 0, unknown: 0, servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ] } }] } });
      validateEventWasNotReceived('cluster1,app1', appEvent);
    });

    /**
     * An application being removed from a non-clustered server should produce the following events:
     * - host event - updated app tallies and app removed
     * - server event - updated app tallies and app removed
     * - application event - updated tallies and app removed
     */
    tdd.test('12c. App removed from a non-clustered server', function() {
      var summaryEvent = null;
      addTopicListener(topicUtil.getTopicByType('summary'),  function(e) {
        summaryEvent = e;
      });

      var alertsEvent = null;
      addTopicListener(topicUtil.getTopicByType('alerts'),  function(e) {
        alertsEvent = e;
      });

      var hostsEvent = null;
      addTopicListener(topicUtil.getTopicByType('hosts'),  function(e) {
        hostsEvent = e;
      });

      var hostEvent = null;
      addTopicListener(topicUtil.getTopicByType('host', 'localhost'),  function(e) {
        hostEvent = e;
      });

      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      var serverEvent = null;
      addTopicListener(topicUtil.getTopicByType('server', 'localhost,/wlp/usr,server1'),  function(e) {
        serverEvent = e;
      });

      var clustersEvent = null;
      addTopicListener(topicUtil.getTopicByType('clusters'),  function(e) {
        clustersEvent = e;
      });

      var clusterEvent = null;
      addTopicListener(topicUtil.getTopicByType('cluster', 'cluster1'),  function(e) {
        clusterEvent = e;
      });

      var applicationsEvent = null;
      addTopicListener(topicUtil.getTopicByType('applications'),  function(e) {
        applicationsEvent = e;
      });

      var appEvent = null;
      addTopicListener(topicUtil.getTopicByType('application', 'localhost,/wlp/usr,server1,app1'),  function(e) {
        appEvent = e;
      });

      // cached = oneRunningServerWithOneApp
      setCachedApplications({ up: 1, down: 0, unknown: 0, partial: 0, list: ['localhost,/wlp/usr,server1,app1']});
      setCachedClusters({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedServers({ up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']});
      setCachedHosts({ up: 1, down: 0, unknown: 0, partial: 0, empty: 0, list: ['localhost']});
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedSummaryAndAlerts();
      setCachedHost({ id: 'localhost', type: 'host', name: 'localhost', state: 'STARTED', runtimes: {list: [{id:'localhost,/wlp', name:'localhost,/wlp', type:'runtime'}]}, servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, apps: { up: 1, down: 0, unknown: 0, partial: 0, list: ['localhost,/wlp/usr,server1,app1']}});
      setCachedServer({ id: 'localhost,/wlp/usr,server1', type: 'server', name: 'localhost,/wlp/usr,server1', state: 'STARTED', wlpInstallDir: '/wlp', apps: { up: 1, down: 0, unknown: 0, list: [{name: 'localhost,/wlp/usr,server1,app1', state: 'STARTED'}]}});
      setCachedApplication({ id: 'localhost,/wlp/usr,server1,app1', type: 'application', name: 'app1', state: 'STARTED', up: 1, down: 0, unknown: 0, servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, clusters: { up: 0, down: 0, unknown: 0, partial: 0, list: []}});

      var now = clone(oneRunningServerWithNoApps);

      unified.__sendUnifiedNotifications(now);

      validateSummaryEvent(summaryEvent, { applications: { up: 0, down: 0, unknown: 0, partial: 0 } });
      validateEventWasNotReceived('alerts', alertsEvent);
      validateEventWasNotReceived('hosts', hostsEvent);
      validateEventWasNotReceived('servers', serversEvent);
      validateEventWasNotReceived('clusters', clustersEvent);
      validateApplicationsEvent(applicationsEvent, { up: 0, down: 0, unknown: 0, partial: 0, removed: ['localhost,/wlp/usr,server1,app1'] });

      validateEventWasNotReceived('localhost', hostEvent);
      validateServerEvent('localhost,/wlp/usr,server1', serverEvent, { apps: { up: 0, down: 0, unknown: 0, partial: 0, removed: [ 'localhost,/wlp/usr,server1,app1' ] } });
      validateEventWasNotReceived('cluster1', clusterEvent);
      validateApplicationEvent('localhost,/wlp/usr,server1,app1', appEvent, { state: 'removed' });
    });

    /**
     * An application being removed from a clustered server should produce the following events:
     * - host event - updated app tallies and app removed
     * - server event - updated app tallies and app removed
     * - cluster event - updated app tallies and app removed
     * - application event - updated tallies and app removed
     */
    tdd.test('12d. App removed from a clustered server', function() {
      var summaryEvent = null;
      addTopicListener(topicUtil.getTopicByType('summary'),  function(e) {
        summaryEvent = e;
      });

      var alertsEvent = null;
      addTopicListener(topicUtil.getTopicByType('alerts'),  function(e) {
        alertsEvent = e;
      });

      var hostsEvent = null;
      addTopicListener(topicUtil.getTopicByType('hosts'),  function(e) {
        hostsEvent = e;
      });

      var hostEvent = null;
      addTopicListener(topicUtil.getTopicByType('host', 'localhost'),  function(e) {
        hostEvent = e;
      });

      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      var serverEvent = null;
      addTopicListener(topicUtil.getTopicByType('server', 'localhost,/wlp/usr,server1'),  function(e) {
        serverEvent = e;
      });

      var clustersEvent = null;
      addTopicListener(topicUtil.getTopicByType('clusters'),  function(e) {
        clustersEvent = e;
      });

      var clusterEvent = null;
      addTopicListener(topicUtil.getTopicByType('cluster', 'cluster1'),  function(e) {
        clusterEvent = e;
      });

      var applicationsEvent = null;
      addTopicListener(topicUtil.getTopicByType('applications'),  function(e) {
        applicationsEvent = e;
      });

      var appEvent = null;
      addTopicListener(topicUtil.getTopicByType('application', 'cluster1,app1'),  function(e) {
        appEvent = e;
      });

      // cached = oneRunningClusteredServerWithOneApp
      setCachedApplications({ up: 1, down: 0, unknown: 0, partial: 0, list: ['cluster1,app1']});
      setCachedClusters({ up: 1, down: 0, unknown: 0, partial: 0, list: ['cluster1']});
      setCachedServers({ up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']});
      setCachedHosts({ up: 1, down: 0, unknown: 0, partial: 0, empty: 0, list: ['localhost']});
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedSummaryAndAlerts();
      setCachedHost({ id: 'localhost', type: 'host', name: 'localhost', state: 'STARTED', runtimes: {list: [{id:'localhost,/wlp', name:'localhost,/wlp', type:'runtime'}]}, servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, apps: { up: 1, down: 0, unknown: 0, partial: 0, list: ['cluster1,app1']}});
      setCachedServer({ id: 'localhost,/wlp/usr,server1', type: 'server', name: 'localhost,/wlp/usr,server1', state: 'STARTED', wlpInstallDir: '/wlp', cluster: 'cluster1', apps: { up: 1, down: 0, unknown: 0, list: [{id: 'cluster1,app1', name: 'app1', state: 'STARTED'}]}});
      setCachedCluster({ id: 'cluster1', type: 'cluster', name: 'cluster1', state: 'STARTED', 
        servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, 
        apps: { up: 1, down: 0, unknown: 0, partial: 0,
          list: [ {
            id: 'cluster1,app1',
            name: 'app1',
            state: 'STARTED',
            up: 1, down: 0, unknown: 0,
            servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ] }
          } ] }
      });
      setCachedApplication({ id: 'cluster1,app1', type: 'application', name: 'app1', state: 'STARTED', up: 1, down: 0, unknown: 0, servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, clusters: { up: 1, down: 0, unknown: 0, partial: 0, list: ['cluster1']}});

      var now = clone(oneRunningClusteredServerWithNoApps);

      unified.__sendUnifiedNotifications(now);

      validateSummaryEvent(summaryEvent, { applications: { up: 0, down: 0, unknown: 0, partial: 0 } });
      validateEventWasNotReceived('alerts', alertsEvent);
      validateEventWasNotReceived('hosts', hostsEvent);
      validateEventWasNotReceived('servers', serversEvent);
      validateEventWasNotReceived('clusters', clustersEvent);
      validateApplicationsEvent(applicationsEvent, { up: 0, down: 0, unknown: 0, partial: 0, removed: ['cluster1,app1'] });

      validateEventWasNotReceived('localhost', hostEvent);
      validateServerEvent('localhost,/wlp/usr,server1', serverEvent, { apps: { up: 0, down: 0, unknown: 0, partial: 0, removed: [ 'app1' ] } });
      validateClusterEvent('cluster1', clusterEvent, { apps: { up: 0, down: 0, unknown: 0, partial: 0, removed: ['app1'] } });
      validateApplicationEvent('cluster1,app1', appEvent, { state: 'removed' });
    });

    /**
     * The same application being added to a new non-clustered server should produce the following events:
     * - servers - updated server tallies and added server
     * - host event - updated server tallies
     * - application event - updated servers tallies and list
     */
    tdd.test('13a. App added to another non-clustered server', function() {
      var summaryEvent = null;
      addTopicListener(topicUtil.getTopicByType('summary'),  function(e) {
        summaryEvent = e;
      });

      var alertsEvent = null;
      addTopicListener(topicUtil.getTopicByType('alerts'),  function(e) {
        alertsEvent = e;
      });

      var hostsEvent = null;
      addTopicListener(topicUtil.getTopicByType('hosts'),  function(e) {
        hostsEvent = e;
      });

      var hostEvent = null;
      addTopicListener(topicUtil.getTopicByType('host', 'localhost'),  function(e) {
        hostEvent = e;
      });

      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      var serverEvent = null;
      addTopicListener(topicUtil.getTopicByType('server', 'localhost,/wlp/usr,server1'),  function(e) {
        serverEvent = e;
      });

      var clustersEvent = null;
      addTopicListener(topicUtil.getTopicByType('clusters'),  function(e) {
        clustersEvent = e;
      });

      var applicationsEvent = null;
      addTopicListener(topicUtil.getTopicByType('applications'),  function(e) {
        applicationsEvent = e;
      });

      var appEvent = null;
      addTopicListener(topicUtil.getTopicByType('application', 'localhost,/wlp/usr,server1,app1'),  function(e) {
        appEvent = e;
      });

      // cached = oneRunningServerWithOneApp
      setCachedApplications({ up: 1, down: 0, unknown: 0, partial: 0, list: ['localhost,/wlp/usr,server1,app1']});
      setCachedClusters({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedServers({ up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']});
      setCachedHosts({ up: 1, down: 0, unknown: 0, partial: 0, empty: 0, list: ['localhost']});
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedSummaryAndAlerts();
      setCachedHost({ id: 'localhost', type: 'host', name: 'localhost', state: 'STARTED', runtimes: {list: [{id:'localhost,/wlp', name:'localhost,/wlp', type:'runtime'}]}, servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, apps: { up: 1, down: 0, unknown: 0, partial: 0, list: ['localhost,/wlp/usr,server1,app1']}});
      setCachedServer({ id: 'localhost,/wlp/usr,server1', type: 'server', name: 'localhost,/wlp/usr,server1', state: 'STARTED', wlpInstallDir: '/wlp', apps: { up: 1, down: 0, unknown: 0, list: [{id: 'localhost,/wlp/usr,server1,app1', name: 'app1', state: 'STARTED'}]}});
      setCachedApplication({ id: 'localhost,/wlp/usr,server1,app1', type: 'application', name: 'app1', state: 'STARTED', up: 1, down: 0, unknown: 0, servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, clusters: { up: 0, down: 0, unknown: 0, partial: 0, list: []}});

      var now = clone(appRunningOnTwoServers);

      unified.__sendUnifiedNotifications(now);

      validateSummaryEvent(summaryEvent, { servers: { up: 2, down: 0, unknown: 0 } });
      validateEventWasNotReceived('alerts', alertsEvent);
      validateEventWasNotReceived('hosts', hostsEvent);
      validateServersEvent(serversEvent, { up: 2, down: 0, unknown: 0, added: ['localhost,/wlp/usr,server2'] });
      validateEventWasNotReceived('clusters', clustersEvent);
      validateApplicationsEvent(applicationsEvent, { up: 1, down: 0, unknown: 0, partial: 0, added: ['localhost,/wlp/usr,server2,app1'] });
            
      validateHostEvent('localhost', hostEvent, { servers: { up: 2, down: 0, unknown: 0, added: ['localhost,/wlp/usr,server2'] } });
      validateEventWasNotReceived('localhost,/wlp/usr,server1', serverEvent);
      validateEventWasNotReceived('localhost,/wlp/usr,server1,app1', appEvent);
    });

    /**
     * New tags added to a new non-clustered server should produce the following events:
     * - servers
     *   tags added: 'newTag'
     *   owner changed: 'Michal'
     *   contacts deleted: 'Wendy'
     */
    tdd.test('14a. New tags added to a non-clustered server', function() {
      var summaryEvent = null;
      addTopicListener(topicUtil.getTopicByType('summary'),  function(e) {
        summaryEvent = e;
      });

      var alertsEvent = null;
      addTopicListener(topicUtil.getTopicByType('alerts'),  function(e) {
        alertsEvent = e;
      });

      var hostsEvent = null;
      addTopicListener(topicUtil.getTopicByType('hosts'),  function(e) {
        hostsEvent = e;
      });

      var hostEvent = null;
      addTopicListener(topicUtil.getTopicByType('host', 'localhost'),  function(e) {
        hostEvent = e;
      });

      var runtimesEvent = null;
      addTopicListener(topicUtil.getTopicByType('runtimes'),  function(e) {
        runtimesEvent = e;
      });

      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      var serverEvent = null;
      addTopicListener(topicUtil.getTopicByType('server', 'localhost,/wlp/usr,server1'),  function(e) {
        serverEvent = e;
      });

      var clustersEvent = null;
      addTopicListener(topicUtil.getTopicByType('clusters'),  function(e) {
        clustersEvent = e;
      });

      var applicationsEvent = null;
      addTopicListener(topicUtil.getTopicByType('applications'),  function(e) {
        applicationsEvent = e;
      });

      var appEvent = null;
      addTopicListener(topicUtil.getTopicByType('application', 'localhost,/wlp/usr,server1,app1'),  function(e) {
        appEvent = e;
      });

      // cached = oneRunningServerWithOneApp
      setCachedApplications({ up: 1, down: 0, unknown: 0, partial: 0, list: ['localhost,/wlp/usr,server1,app1']});
      setCachedClusters({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedServers({ up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']});
      setCachedHosts({ up: 1, down: 0, unknown: 0, partial: 0, empty: 0, list: ['localhost']});
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedSummaryAndAlerts();
      setCachedHost({ id: 'localhost', type: 'host', name: 'localhost', state: 'STARTED', runtimes: {list: [{id:'localhost,/wlp', name:'localhost,/wlp', type:'runtime'}]}, servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, apps: { up: 1, down: 0, unknown: 0, partial: 0, list: ['localhost,/wlp/usr,server1,app1']}});
      setCachedServer({ id: 'localhost,/wlp/usr,server1', type: 'server', name: 'localhost,/wlp/usr,server1', state: 'STARTED', wlpInstallDir: '/wlp', apps: { up: 1, down: 0, unknown: 0, list: [{id: 'localhost,/wlp/usr,server1,app1', name: 'app1', state: 'STARTED'}]}});
      setCachedApplication({ id: 'localhost,/wlp/usr,server1,app1', type: 'application', name: 'app1', state: 'STARTED', up: 1, down: 0, unknown: 0, servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, clusters: { up: 0, down: 0, unknown: 0, partial: 0, list: []}});

      var now = clone(oneRunningServerWithOneAppAndTagsChanged);

      unified.__sendUnifiedNotifications(now);

      validateEventWasNotReceived('summary', summaryEvent);
      validateEventWasNotReceived('alerts', alertsEvent);
      validateEventWasNotReceived('hosts', hostsEvent);
      validateEventWasNotReceived('servers', serversEvent);
      validateEventWasNotReceived('clusters', clustersEvent);
      validateEventWasNotReceived('applications', applicationsEvent);

      validateEventWasNotReceived('host', hostEvent);
      validateServerEvent('localhost,/wlp/usr,server1', serverEvent, 
          { 
        tags: ['tagA', 'tagB', 'newTag'],
        owner: 'Michal',
        contacts: ['Amy']
          }
      );
      validateEventWasNotReceived('localhost,/wlp/usr,server1,app1', appEvent);
    });


    /**
     * An application being removed from a clustered server should produce the following events:
     * - host event - updated app tallies and app removed
     * - server event - updated app tallies and app removed
     * - cluster event - updated app tallies and app removed
     * - application event - updated tallies and app removed
     */
    tdd.test('15a. Only summary loaded - no change', function() {
      var summaryEvent = null;
      addTopicListener(topicUtil.getTopicByType('summary'),  function(e) {
        summaryEvent = e;
      });

      var alertsEvent = null;
      addTopicListener(topicUtil.getTopicByType('alerts'),  function(e) {
        alertsEvent = e;
      });

      var hostsEvent = null;
      addTopicListener(topicUtil.getTopicByType('hosts'),  function(e) {
        hostsEvent = e;
      });

      var hostEvent = null;
      addTopicListener(topicUtil.getTopicByType('host', 'localhost'),  function(e) {
        hostEvent = e;
      });

      var runtimesEvent = null;
      addTopicListener(topicUtil.getTopicByType('runtimes'),  function(e) {
        runtimesEvent = e;
      });

      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      var serverEvent = null;
      addTopicListener(topicUtil.getTopicByType('server', 'localhost,/wlp/usr,server1'),  function(e) {
        serverEvent = e;
      });

      var clustersEvent = null;
      addTopicListener(topicUtil.getTopicByType('clusters'),  function(e) {
        clustersEvent = e;
      });

      var clusterEvent = null;
      addTopicListener(topicUtil.getTopicByType('cluster', 'cluster1'),  function(e) {
        clusterEvent = e;
      });

      var applicationsEvent = null;
      addTopicListener(topicUtil.getTopicByType('applications'),  function(e) {
        applicationsEvent = e;
      });

      var appEvent = null;
      addTopicListener(topicUtil.getTopicByType('application', 'app1'),  function(e) {
        appEvent = e;
      });

      // cached = oneRunningClusteredServerWithOneApp
      setCachedApplications({ up: 1, down: 0, unknown: 0, partial: 0, list: ['cluster1,app1']});
      setCachedClusters({ up: 1, down: 0, unknown: 0, partial: 0, list: ['cluster1']});
      setCachedServers({ up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']});
      setCachedHosts({ up: 1, down: 0, unknown: 0, partial: 0, empty: 0, list: ['localhost']});
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedSummaryAndAlerts();

      var now = clone(oneRunningClusteredServerWithOneApp);

      unified.__sendUnifiedNotifications(now);

      validateEventWasNotReceived('summary', summaryEvent);
      validateEventWasNotReceived('alerts', alertsEvent);
      validateEventWasNotReceived('hosts', hostsEvent);
      validateEventWasNotReceived('servers', serversEvent);
      validateEventWasNotReceived('clusters', clustersEvent);
      validateEventWasNotReceived('applications', applicationsEvent);

      validateEventWasNotReceived('localhost', hostEvent);
      validateEventWasNotReceived('localhost,/wlp/usr,server1', serverEvent);
      validateEventWasNotReceived('cluster1', clusterEvent);
      validateEventWasNotReceived('app1', appEvent);
    });

    /**
     * When only the summary (dashboard) is loaded, only the collection objects should get the event
     */
    tdd.test('15b. Only summary loaded - to empty', function() {
      var summaryEvent = null;
      addTopicListener(topicUtil.getTopicByType('summary'),  function(e) {
        summaryEvent = e;
      });

      var alertsEvent = null;
      addTopicListener(topicUtil.getTopicByType('alerts'),  function(e) {
        alertsEvent = e;
      });

      var hostsEvent = null;
      addTopicListener(topicUtil.getTopicByType('hosts'),  function(e) {
        hostsEvent = e;
      });

      var hostEvent = null;
      addTopicListener(topicUtil.getTopicByType('host', 'localhost'),  function(e) {
        hostEvent = e;
      });

      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      var serverEvent = null;
      addTopicListener(topicUtil.getTopicByType('server', 'localhost,/wlp/usr,server1'),  function(e) {
        serverEvent = e;
      });

      var clustersEvent = null;
      addTopicListener(topicUtil.getTopicByType('clusters'),  function(e) {
        clustersEvent = e;
      });

      var clusterEvent = null;
      addTopicListener(topicUtil.getTopicByType('cluster', 'cluster1'),  function(e) {
        clusterEvent = e;
      });

      var applicationsEvent = null;
      addTopicListener(topicUtil.getTopicByType('applications'),  function(e) {
        applicationsEvent = e;
      });

      var appEvent = null;
      addTopicListener(topicUtil.getTopicByType('application', 'cluster1,app1'),  function(e) {
        appEvent = e;
      });

      // cached = oneRunningClusteredServerWithOneApp
      setCachedApplications({ up: 1, down: 0, unknown: 0, partial: 0, list: ['cluster1,app1']});
      setCachedClusters({ up: 1, down: 0, unknown: 0, partial: 0, list: ['cluster1']});
      setCachedServers({ up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']});
      setCachedHosts({ up: 1, down: 0, unknown: 0, partial: 0, empty: 0, list: ['localhost']});
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedSummaryAndAlerts();

      var now = clone(emptyCollective);

      unified.__sendUnifiedNotifications(now);

      validateSummaryEvent(summaryEvent, { applications: { up: 0, down: 0, unknown: 0, partial: 0 }, clusters: { up: 0, down: 0, unknown: 0, partial: 0 }, servers: { up: 0, down: 0, unknown: 0 }, hosts: { up: 0, down: 0, unknown: 0, partial: 0, empty: 0 } });
      validateEventWasNotReceived('alerts', alertsEvent);
      validateHostsEvent(hostsEvent, { up: 0, down: 0, unknown: 0, partial: 0, empty: 0, removed: ['localhost'] });
      validateServersEvent(serversEvent, { up: 0, down: 0, unknown: 0, removed: ['localhost,/wlp/usr,server1'] });
      validateClustersEvent(clustersEvent, { up: 0, down: 0, unknown: 0, partial: 0, removed: ['cluster1'] });
      validateApplicationsEvent(applicationsEvent, { up: 0, down: 0, unknown: 0, partial: 0, removed: ['cluster1,app1'] });

      validateEventWasNotReceived('localhost', hostEvent);
      validateEventWasNotReceived('localhost,/wlp/usr,server1', serverEvent);
      validateEventWasNotReceived('cluster1', clusterEvent);
      validateEventWasNotReceived('cluster1,app1', appEvent);
    });

    /**
     * When a server becomes a collective controller, the flag should be set via the event
     */
    tdd.test('16a. Server becomes collective controller', function() {
      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      var serverEvent = null;
      addTopicListener(topicUtil.getTopicByType('server', 'localhost,/wlp/usr,server1'),  function(e) {
        serverEvent = e;
      });

      // cached = oneRunningClusteredServerWithOneApp + isCollectiveController
      setCachedApplications({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedClusters({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedServers({ up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']});
      setCachedHosts({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedSummaryAndAlerts();
      setCachedServer({ id: 'localhost,/wlp/usr,server1', type: 'server', name: 'localhost,/wlp/usr,server1', state: 'STARTED', wlpInstallDir: '/wlp', apps: { up: 0, down: 0, unknown: 0, list: []}});

      var now = clone(oneRunningServerWithNoApps);
      now.servers.list[0].isCollectiveController = true; // Set the collective controller flag

      unified.__sendUnifiedNotifications(now);

      validateEventWasNotReceived('servers', serversEvent);
      validateServerEvent('localhost,/wlp/usr,server1', serverEvent, { isCollectiveController: true });
    });

    /**
     * When a server is no longer a collective controller, the flag should be set via the event
     */
    tdd.test('16b. Server is no longer collective controller', function() {
      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      var serverEvent = null;
      addTopicListener(topicUtil.getTopicByType('server', 'localhost,/wlp/usr,server1'),  function(e) {
        serverEvent = e;
      });

      // cached = oneRunningClusteredServerWithOneApp + isCollectiveController
      setCachedApplications({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedClusters({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedServers({ up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']});
      setCachedHosts({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedSummaryAndAlerts();
      setCachedServer({ id: 'localhost,/wlp/usr,server1', type: 'server', name: 'localhost,/wlp/usr,server1', state: 'STARTED', wlpInstallDir: '/wlp', isCollectiveController: true, apps: { up: 0, down: 0, unknown: 0, list: []}});

      var now = clone(oneRunningServerWithNoApps);
      now.servers.list[0].isCollectiveController = false; // Set the collective controller flag

      unified.__sendUnifiedNotifications(now);

      validateEventWasNotReceived('servers', serversEvent);
      validateServerEvent('localhost,/wlp/usr,server1', serverEvent, { isCollectiveController: false });
    });

    /**
     * When a server has an enabled scaling policy, the flag should be set via the event
     */
    tdd.test('16c. Server scaling policy enabled', function() {
      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      var serverEvent = null;
      addTopicListener(topicUtil.getTopicByType('server', 'localhost,/wlp/usr,server1'),  function(e) {
        serverEvent = e;
      });

      // cached = oneRunningClusteredServerWithOneApp + scalingPolicyEnabled
      setCachedApplications({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedClusters({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedServers({ up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']});
      setCachedHosts({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedSummaryAndAlerts();
      setCachedServer({ id: 'localhost,/wlp/usr,server1', type: 'server', name: 'localhost,/wlp/usr,server1', state: 'STARTED', wlpInstallDir: '/wlp', apps: { up: 0, down: 0, unknown: 0, list: []}});

      var now = clone(oneRunningServerWithNoApps);
      now.servers.list[0].scalingPolicyEnabled = true; // Set the collective controller flag

      unified.__sendUnifiedNotifications(now);

      validateEventWasNotReceived('servers', serversEvent);
      validateServerEvent('localhost,/wlp/usr,server1', serverEvent, { scalingPolicyEnabled: true });
    });

    /**
     * When a server has a disabled scaling policy, the flag should be set via the event
     */
    tdd.test('16c. Server scaling policy disabled', function() {
      var serversEvent = null;
      addTopicListener(topicUtil.getTopicByType('servers'),  function(e) {
        serversEvent = e;
      });

      var serverEvent = null;
      addTopicListener(topicUtil.getTopicByType('server', 'localhost,/wlp/usr,server1'),  function(e) {
        serverEvent = e;
      });

      // cached = oneRunningClusteredServerWithOneApp + scalingPolicyEnabled
      setCachedApplications({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedClusters({ up: 0, down: 0, unknown: 0, partial: 0, list: []});
      setCachedServers({ up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']});
      setCachedHosts({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedRuntimes({ up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
      setCachedSummaryAndAlerts();
      setCachedServer({ id: 'localhost,/wlp/usr,server1', type: 'server', name: 'localhost,/wlp/usr,server1', state: 'STARTED', wlpInstallDir: '/wlp', scalingPolicyEnabled: true, apps: { up: 0, down: 0, unknown: 0, list: []}});

      var now = clone(oneRunningServerWithNoApps);
      now.servers.list[0].scalingPolicyEnabled = false; // Set the collective controller flag

      unified.__sendUnifiedNotifications(now);

      validateEventWasNotReceived('servers', serversEvent);
      validateServerEvent('localhost,/wlp/usr,server1', serverEvent, { scalingPolicyEnabled: false });
    });

  });

});
