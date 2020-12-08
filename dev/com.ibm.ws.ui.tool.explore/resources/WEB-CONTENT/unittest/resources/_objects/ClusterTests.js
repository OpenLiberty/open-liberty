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
 * Test cases for Cluster
 */
define([
        "intern!tdd",
        "intern/chai!assert",
        "intern/dojo/declare",
        "resources/_objects/Cluster",
        "resources/Observer"
        ],

        function(tdd, assert, declare, Cluster, Observer) {

  var ClusterObserver = declare([Observer], {
    id: 'testObserver',

    onDestroyed: function() {
      this.isDestroyed = true;
    },

    onStateChange: function(newState, oldState) {
      this.newState = newState;
      this.oldState = oldState;
    },

    onScalingPolicyChange: function(newScalingPolicy, oldScalingPolicy) {
      this.newScalingPolicy = newScalingPolicy;
      this.oldScalingPolicy = oldScalingPolicy;
    },

    onScalingPolicyEnabledChange: function(newScalingPolicyEnabled, oldScalingPolicyEnabled) {
      this.newScalingPolicyEnabled = newScalingPolicyEnabled;
      this.oldScalingPolicyEnabled = oldScalingPolicyEnabled;
    },

    onServersTallyChange: function(newTally, oldTally) {
      this.newServersTally = newTally;
      this.oldServersTally = oldTally;
    },

    onServersListChange: function(newList, oldList, added, removed) {
      this.newServersList = newList;
      this.oldServersList = oldList;
      this.addedServers = added;
      this.removedServers = removed;
    },

    onAppsTallyChange: function(newTally, oldTally) {
      this.newAppsTally = newTally;
      this.oldAppsTally = oldTally;
    },

    onAppsListChange: function(newList, oldList, added, removed) {
      this.newAppsList = newList;
      this.oldAppsList = oldList;
      this.addedApps = added;
      this.removedApps = removed;
    },

    onAlertsChange: function(newAlerts, oldAlerts) {
      this.newAlerts = newAlerts;
      this.oldAlerts = oldAlerts;
    }
  });

  with(assert) {

    /**
     * Defines the 'Cluster Object Tests' module test suite.
     * 
     * Technically we can't have an empty cluster in the current environment, but this is just test code to make sure initialization / updates work.
     * It does not intend to check correctness as it pertains to the REST API responses (nor should it).
     */
    tdd.suite('Cluster Object Tests', function() {
      tdd.test('constructor - no initialization object', function() {
        try {
          new Cluster();
          assert.ok(false, 'Cluster was successfully created when it should have failed - an initialization object is required');
        } catch(error) {
          assert.equal(error, 'ObservableResource created without an initialization object', 'Error reported did not match expected error');
        }
      });

      tdd.test('constructor - no id', function() {
        try {
          new Cluster({state: 'STARTED', servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
          assert.ok(false, 'Cluster was successfully created when it should have failed - an id is required');
        } catch(error) {
          assert.equal(error, 'ObservableResource created without an id', 'Error reported did not match expected error');
        }
      }),

      tdd.test('constructor - no initial state', function() {
        try {
          new Cluster({id: 'cluster1', servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
          assert.ok(false, 'Cluster "cluster1" was successfully created when it should have failed - an initial state is required');
        } catch(error) {
          assert.equal(error, 'StatefulResource created without a state', 'Error reported did not match expected error');
        }
      }),

      tdd.test('constructor - no initial servers', function() {
        try {
          new Cluster({id: 'cluster1', state: 'STARTED', apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
          assert.ok(false, 'Cluster "cluster1" was successfully created when it should have failed - an initial servers object is required');
        } catch(error) {
          assert.equal(error, 'Cluster "cluster1" created without an initial servers object', 'Error reported did not match expected error');
        }
      }),   

      tdd.test('constructor - no initial servers.up', function() {
        try {
          new Cluster({id: 'cluster1', state: 'STARTED', servers: {down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
          assert.ok(false, 'Cluster "cluster1" was successfully created when it should have failed - an initial servers.up tally is required');
        } catch(error) {
          assert.equal(error, 'Cluster "cluster1" created without an initial servers.up tally', 'Error reported did not match expected error');
        }
      }),

      tdd.test('constructor - no initial servers.down', function() {
        try {
          new Cluster({id: 'cluster1', state: 'STARTED', servers: {up: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
          assert.ok(false, 'Cluster "cluster1" was successfully created when it should have failed - an initial servers.down tally is required');
        } catch(error) {
          assert.equal(error, 'Cluster "cluster1" created without an initial servers.down tally', 'Error reported did not match expected error');
        }
      }),

      tdd.test('constructor - no initial servers.unknown', function() {
        try {
          new Cluster({id: 'cluster1', state: 'STARTED', servers: {up: 0, down: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
          assert.ok(false, 'Cluster "cluster1" was successfully created when it should have failed - an initial servers.unknown tally is required');
        } catch(error) {
          assert.equal(error, 'Cluster "cluster1" created without an initial servers.unknown tally', 'Error reported did not match expected error');
        }
      }),

      tdd.test('constructor - no initial servers.list', function() {
        try {
          new Cluster({id: 'cluster1', state: 'STARTED', servers: {up: 0, down: 0, unknown: 0}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
          assert.ok(false, 'Cluster "cluster1" was successfully created when it should have failed - an initial servers.list array is required');
        } catch(error) {
          assert.equal(error, 'Cluster "cluster1" created without an initial servers.list array', 'Error reported did not match expected error');
        }
      }),

      tdd.test('constructor - initial servers.list not an array', function() {
        try {
          new Cluster({id: 'cluster1', state: 'STARTED', servers: {up: 0, down: 0, unknown: 0}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: 'list'}});
          assert.ok(false, 'Cluster "cluster1" was successfully created when it should have failed - an initial servers.list array is required');
        } catch(error) {
          assert.equal(error, 'Cluster "cluster1" created without an initial servers.list array', 'Error reported did not match expected error');
        }
      }),

      tdd.test('constructor - no initial apps', function() {
        try {
          new Cluster({id: 'cluster1', state: 'STARTED', servers: {up: 0, down: 0, unknown: 0, list: []}});
          assert.ok(false, 'Cluster "cluster1" was successfully created when it should have failed - an initial apps object is required');
        } catch(error) {
          assert.equal(error, 'Cluster "cluster1" created without an initial apps object', 'Error reported did not match expected error');
        }
      }),

      tdd.test('constructor - no initial apps.up', function() {
        try {
          new Cluster({id: 'cluster1', state: 'STARTED', servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {down: 0, unknown: 0, partial: 0, list: []}});
          assert.ok(false, 'Cluster "cluster1" was successfully created when it should have failed - an initial apps.up tally is required');
        } catch(error) {
          assert.equal(error, 'Cluster "cluster1" created without an initial apps.up tally', 'Error reported did not match expected error');
        }
      }),

      tdd.test('constructor - no initial apps.down', function() {
        try {
          new Cluster({id: 'cluster1', state: 'STARTED', servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, unknown: 0, partial: 0, list: []}});
          assert.ok(false, 'Cluster "cluster1" was successfully created when it should have failed - an initial apps.down tally is required');
        } catch(error) {
          assert.equal(error, 'Cluster "cluster1" created without an initial apps.down tally', 'Error reported did not match expected error');
        }
      }),

      tdd.test('constructor - no initial apps.unknown', function() {
        try {
          new Cluster({id: 'cluster1', state: 'STARTED', servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, partial: 0, list: []}});
          assert.ok(false, 'Cluster "cluster1" was successfully created when it should have failed - an initial apps.unknown tally is required');
        } catch(error) {
          assert.equal(error, 'Cluster "cluster1" created without an initial apps.unknown tally', 'Error reported did not match expected error');
        }
      }),

      tdd.test('constructor - no initial apps.partial', function() {
        try {
          new Cluster({id: 'cluster1', state: 'STARTED', servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, list: []}});
          assert.ok(false, 'Cluster "cluster1" was successfully created when it should have failed - an initial apps.partial tally is required');
        } catch(error) {
          assert.equal(error, 'Cluster "cluster1" created without an initial apps.partial tally', 'Error reported did not match expected error');
        }
      }),

      tdd.test('constructor - no initial apps.list', function() {
        try {
          new Cluster({id: 'cluster1', state: 'STARTED', servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0}});
          assert.ok(false, 'Cluster "cluster1" was successfully created when it should have failed - an initial apps.list array is required');
        } catch(error) {
          assert.equal(error, 'Cluster "cluster1" created without an initial apps.list array', 'Error reported did not match expected error');
        }
      }),

      tdd.test('constructor - empty scalingPolicy, servers or apps', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED', servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});

        assert.equal(cluster.id,               'cluster1',  'Cluster.id did not have the correct initialized value');
        assert.equal(cluster.name,             'cluster1',  'Cluster.name did not have the correct initialized value');
        assert.equal(cluster.state,            'STARTED',   'Cluster.state did not have the correct initialized value');
        assert.equal(cluster.scalingPolicy,    null,        'Cluster.scalingPolicy did not have the correct initialized value');

        assert.equal(cluster.servers.up,       0,           'Cluster.servers.up did not have the correct initialized value');
        assert.equal(cluster.servers.down,     0,           'Cluster.servers.down did not have the correct initialized value');
        assert.equal(cluster.servers.unknown,  0,           'Cluster.servers.unknown did not have the correct initialized value');
        assert.ok(Array.isArray(cluster.servers.list),    'Cluster.servers.list was not an Array');
        assert.equal(cluster.servers.list.length, 0,           'Cluster.servers.list.length was not initially empty');

        assert.equal(cluster.apps.up,          0,           'Cluster.apps.up did not have the correct initialized value');
        assert.equal(cluster.apps.down,        0,           'Cluster.apps.down did not have the correct initialized value');
        assert.equal(cluster.apps.unknown,     0,           'Cluster.apps.unknown did not have the correct initialized value');
        assert.equal(cluster.apps.partial,     0,           'Cluster.apps.partial did not have the correct initialized value');
        assert.ok(Array.isArray(cluster.apps.list),       'Cluster.apps.list was not an Array');
        assert.equal(cluster.apps.list.length, 0,           'Cluster.apps.list.length was not initially empty');
      }),

      tdd.test('constructor - with scalingPolicy, servers and apps', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED', scalingPolicy: 'default', servers: {up: 1, down: 2, unknown: 3, list: ['localhost,/wlp/usr,server1']}, apps: {up: 4, down: 5, unknown: 6, partial: 7, list: [ {name: 'snoop'}] }, alerts: {count: 8}});

        assert.equal(cluster.id,               'cluster1',  'Cluster.id did not have the correct initialized value');
        assert.equal(cluster.name,             'cluster1',  'Cluster.name did not have the correct initialized value');
        assert.equal(cluster.state,            'STARTED',   'Cluster.state did not have the correct initialized value');
        assert.equal(cluster.scalingPolicy,    'default',   'Cluster.scalingPolicy did not have the correct initialized value');

        assert.equal(cluster.servers.up,       1,           'Cluster.servers.up did not have the correct initialized value');
        assert.equal(cluster.servers.down,     2,           'Cluster.servers.down did not have the correct initialized value');
        assert.equal(cluster.servers.unknown,  3,           'Cluster.servers.unknown did not have the correct initialized value');
        assert.ok(Array.isArray(cluster.servers.list),      'Cluster.servers.list was not an Array');
        assert.equal(cluster.servers.list.length, 1,        'Cluster.servers.list.length was not the correct value');
        assert.equal(cluster.servers.list[0], 'localhost,/wlp/usr,server1', 'Cluster.servers.list[0] did not have the correct value');

        assert.equal(cluster.apps.up,          4,           'Cluster.apps.up did not have the correct initialized value');
        assert.equal(cluster.apps.down,        5,           'Cluster.apps.down did not have the correct initialized value');
        assert.equal(cluster.apps.unknown,     6,           'Cluster.apps.unknown did not have the correct initialized value');
        assert.equal(cluster.apps.partial,     7,           'Cluster.apps.partial did not have the correct initialized value');
        assert.ok(Array.isArray(cluster.apps.list),         'Cluster.apps.list was not an Array');
        assert.equal(cluster.apps.list.length, 1,           'Cluster.apps.list.length was not the correct value');
        assert.equal(cluster.apps.list[0].name, 'snoop',    'Cluster.apps.list[0].name did not have the correct value');

        assert.ok(cluster.alerts,                           'Cluster did not have an alerts attribute');
        assert.equal(cluster.alerts.count,     8,           'Cluster.alerts.count did not have the correct initialized value');
      }),

      tdd.test('handleChangeEvent - new tags', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED', servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
        // Check pre-event conditions
        assert.equal(cluster.tags,                    null, 'Cluster.tags did not have the correct initialized value');

        // Simulate event
        cluster._handleChangeEvent({type: 'cluster', id: 'cluster1', tags: ['newTag'] });

        assert.equal(JSON.stringify(cluster.tags),             JSON.stringify(['newTag']),  'Cluster.tags did not have the correct updated value');
      }),

      tdd.test('handleChangeEvent - tags changed', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED', servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}, tags: ['tag1']});
        // Check pre-event conditions
        assert.equal(JSON.stringify(cluster.tags),                JSON.stringify(['tag1']), 'Cluster.tags did not have the correct initialized value');

        // Simulate event
        cluster._handleChangeEvent({type: 'cluster', id: 'cluster1', tags: ['tag2'] });

        assert.equal(JSON.stringify(cluster.tags),               JSON.stringify(['tag2']),  'Cluster.tags did not have the correct updated value');
      }),

      tdd.test('handleChangeEvent - owner changed', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED', servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}, owner: 'Henry'});
        // Check pre-event conditions
        assert.equal(cluster.owner,                'Henry', 'Cluster.owner did not have the correct initialized value');

        // Simulate event
        cluster._handleChangeEvent({type: 'cluster', id: 'cluster1', owner: 'Ilene' });

        assert.equal(cluster.owner,               'Ilene',  'Cluster.owner did not have the correct updated value');
      }),

      tdd.test('handleChangeEvent - contacts changed', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED', servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}, contacts: ['John Smith']});
        // Check pre-event conditions
        assert.equal(JSON.stringify(cluster.contacts),      JSON.stringify(['John Smith']), 'Cluster.contacts did not have the correct initialized value');

        // Simulate event
        cluster._handleChangeEvent({type: 'cluster', id: 'cluster1', contacts: ['Elsa Smith'] });

        assert.equal(JSON.stringify(cluster.contacts),      JSON.stringify(['Elsa Smith']),  'Cluster.contacts did not have the correct updated value');
      }),

      tdd.test('handleChangeEvent - note changed', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED', servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}, note: 'Merry Christmas'});
        // Check pre-event conditions
        assert.equal(cluster.note,        'Merry Christmas', 'Cluster.note did not have the correct initialized value');

        // Simulate event
        cluster._handleChangeEvent({type: 'cluster', id: 'cluster1', note: 'Happy New Year' });

        assert.equal(cluster.note,         'Happy New Year',  'Cluster.note did not have the correct updated value');
      }),

      tdd.test('handleChangeEvent - changed state', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED', servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});

        // Check pre-event conditions
        assert.equal(cluster.state,            'STARTED',   'Cluster.state did not have the correct initialized value');

        // Simulate event
        cluster._handleChangeEvent({type: 'cluster', id: 'cluster1', state: 'STOPPED' });

        assert.equal(cluster.id,               'cluster1',  'Cluster.id did not have the correct initialized value');
        assert.equal(cluster.name,             'cluster1',  'Cluster.name did not have the correct initialized value');
        assert.equal(cluster.state,            'STOPPED',   'Cluster.state did not update based on the event value');
        assert.equal(cluster.scalingPolicy,    null,        'Cluster.scalingPolicy did not have the correct initialized value');

        assert.equal(cluster.servers.up,       0,           'Cluster.servers.up did not have the correct initialized value');
        assert.equal(cluster.servers.down,     0,           'Cluster.servers.down did not have the correct initialized value');
        assert.equal(cluster.servers.unknown,  0,           'Cluster.servers.unknown did not have the correct initialized value');
        assert.ok(Array.isArray(cluster.servers.list),      'Cluster.servers.list was not an Array');
        assert.equal(cluster.servers.list.length, 0,        'Cluster.servers.list.length was not initially empty');

        assert.equal(cluster.apps.up,          0,           'Cluster.apps.up did not have the correct initialized value');
        assert.equal(cluster.apps.down,        0,           'Cluster.apps.down did not have the correct initialized value');
        assert.equal(cluster.apps.unknown,     0,           'Cluster.apps.unknown did not have the correct initialized value');
        assert.equal(cluster.apps.partial,     0,           'Cluster.apps.partial did not have the correct initialized value');
        assert.ok(Array.isArray(cluster.apps.list),         'Cluster.apps.list was not an Array');
        assert.equal(cluster.apps.list.length, 0,           'Cluster.apps.list.length was not initially empty');
      }),

      tdd.test('handleChangeEvent - changed state from operation', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED', servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});

        // Check pre-event conditions
        assert.equal(cluster.state,            'STARTED',   'Cluster.state did not have the correct initialized value');

        // Simulate event
        cluster._handleChangeEvent({type: 'cluster', id: 'cluster1', state: 'STOPPING' });

        assert.equal(cluster.id,               'cluster1',  'Cluster.id did not have the correct initialized value');
        assert.equal(cluster.name,             'cluster1',  'Cluster.name did not have the correct initialized value');
        assert.equal(cluster.state,            'STOPPING',  'Cluster.state did not update based on the event value');
        assert.equal(cluster.scalingPolicy,    null,        'Cluster.scalingPolicy did not have the correct initialized value');

        assert.equal(cluster.servers.up,       0,           'Cluster.servers.up did not have the correct initialized value');
        assert.equal(cluster.servers.down,     0,           'Cluster.servers.down did not have the correct initialized value');
        assert.equal(cluster.servers.unknown,  0,           'Cluster.servers.unknown did not have the correct initialized value');
        assert.ok(Array.isArray(cluster.servers.list),      'Cluster.servers.list was not an Array');
        assert.equal(cluster.servers.list.length, 0,        'Cluster.servers.list.length was not initially empty');

        assert.equal(cluster.apps.up,          0,           'Cluster.apps.up did not have the correct initialized value');
        assert.equal(cluster.apps.down,        0,           'Cluster.apps.down did not have the correct initialized value');
        assert.equal(cluster.apps.unknown,     0,           'Cluster.apps.unknown did not have the correct initialized value');
        assert.equal(cluster.apps.partial,     0,           'Cluster.apps.partial did not have the correct initialized value');
        assert.ok(Array.isArray(cluster.apps.list),         'Cluster.apps.list was not an Array');
        assert.equal(cluster.apps.list.length, 0,           'Cluster.apps.list.length was not initially empty');

        cluster._handleChangeEvent({type: 'cluster', id: 'cluster1', state: 'STARTING' });
        assert.equal(cluster.state,            'STARTING',  'Cluster.state did not update based on the event value');
      }),

      tdd.test('handleChangeEvent - added scalingPolicy', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED', servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});

        // Check pre-event conditions
        assert.equal(null,        cluster.scalingPolicy,    'Cluster.scalingPolicy did not have the correct initialized value');

        // Simulate event
        cluster._handleChangeEvent({type: 'cluster', id: 'cluster1', scalingPolicy: 'default' });

        assert.equal(cluster.id,               'cluster1',  'Cluster.id did not have the correct initialized value');
        assert.equal(cluster.name,             'cluster1',  'Cluster.name did not have the correct initialized value');
        assert.equal(cluster.state,            'STARTED',   'Cluster.state did not have the correct initialized value');
        assert.equal(cluster.scalingPolicy,    'default',   'Cluster.scalingPolicy did not update based on the event value');

        assert.equal(cluster.servers.up,       0,           'Cluster.servers.up did not have the correct initialized value');
        assert.equal(cluster.servers.down,     0,           'Cluster.servers.down did not have the correct initialized value');
        assert.equal(cluster.servers.unknown,  0,           'Cluster.servers.unknown did not have the correct initialized value');
        assert.ok(Array.isArray(cluster.servers.list),      'Cluster.servers.list was not an Array');
        assert.equal(cluster.servers.list.length, 0,        'Cluster.servers.list.length was not initially empty');

        assert.equal(cluster.apps.up,          0,           'Cluster.apps.up did not have the correct initialized value');
        assert.equal(cluster.apps.down,        0,           'Cluster.apps.down did not have the correct initialized value');
        assert.equal(cluster.apps.unknown,     0,           'Cluster.apps.unknown did not have the correct initialized value');
        assert.equal(cluster.apps.partial,     0,           'Cluster.apps.partial did not have the correct initialized value');
        assert.ok(Array.isArray(cluster.apps.list),         'Cluster.apps.list was not an Array');
        assert.equal(cluster.apps.list.length, 0,           'Cluster.apps.list.length was not initially empty');
      }),

      tdd.test('handleChangeEvent - changed scalingPolicy', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED', scalingPolicy: 'default', servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});

        // Check pre-event conditions
        assert.equal(cluster.scalingPolicy,    'default',   'Cluster.scalingPolicy did not have the correct initialized value');

        // Simulate event
        cluster._handleChangeEvent({type: 'cluster', id: 'cluster1', scalingPolicy: 'newPolicy' });

        assert.equal(cluster.id,               'cluster1',  'Cluster.id did not have the correct initialized value');
        assert.equal(cluster.name,             'cluster1',  'Cluster.name did not have the correct initialized value');
        assert.equal(cluster.state,            'STARTED',   'Cluster.state did not have the correct initialized value');
        assert.equal(cluster.scalingPolicy,    'newPolicy', 'Cluster.scalingPolicy did not update based on the event value');

        assert.equal(cluster.servers.up,       0,           'Cluster.servers.up did not have the correct initialized value');
        assert.equal(cluster.servers.down,     0,           'Cluster.servers.down did not have the correct initialized value');
        assert.equal(cluster.servers.unknown,  0,           'Cluster.servers.unknown did not have the correct initialized value');
        assert.ok(Array.isArray(cluster.servers.list),      'Cluster.servers.list was not an Array');
        assert.equal(cluster.servers.list.length, 0,        'Cluster.servers.list.length was not initially empty');

        assert.equal(cluster.apps.up,          0,           'Cluster.apps.up did not have the correct initialized value');
        assert.equal(cluster.apps.down,        0,           'Cluster.apps.down did not have the correct initialized value');
        assert.equal(cluster.apps.unknown,     0,           'Cluster.apps.unknown did not have the correct initialized value');
        assert.equal(cluster.apps.partial,     0,           'Cluster.apps.partial did not have the correct initialized value');
        assert.ok(Array.isArray(cluster.apps.list),         'Cluster.apps.list was not an Array');
        assert.equal(cluster.apps.list.length, 0,           'Cluster.apps.list.length was not initially empty');
      }),

      tdd.test('handleChangeEvent - removed scalingPolicy', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED', scalingPolicy: 'default', servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});

        // Check pre-event conditions
        assert.equal(cluster.scalingPolicy,    'default',   'Cluster.scalingPolicy did not have the correct initialized value');

        // Simulate event
        cluster._handleChangeEvent({type: 'cluster', id: 'cluster1', scalingPolicy: null });

        assert.equal(cluster.id,               'cluster1',  'Cluster.id did not have the correct initialized value');
        assert.equal(cluster.name,             'cluster1',  'Cluster.name did not have the correct initialized value');
        assert.equal(cluster.state,            'STARTED',   'Cluster.state did not have the correct initialized value');
        assert.equal(cluster.scalingPolicy,    null,        'Cluster.scalingPolicy did not update based on the event value');

        assert.equal(cluster.servers.up,       0,           'Cluster.servers.up did not have the correct initialized value');
        assert.equal(cluster.servers.down,     0,           'Cluster.servers.down did not have the correct initialized value');
        assert.equal(cluster.servers.unknown,  0,           'Cluster.servers.unknown did not have the correct initialized value');
        assert.ok(Array.isArray(cluster.servers.list),      'Cluster.servers.list was not an Array');
        assert.equal(cluster.servers.list.length, 0,        'Cluster.servers.list.length was not initially empty');

        assert.equal(cluster.apps.up,          0,           'Cluster.apps.up did not have the correct initialized value');
        assert.equal(cluster.apps.down,        0,           'Cluster.apps.down did not have the correct initialized value');
        assert.equal(cluster.apps.unknown,     0,           'Cluster.apps.unknown did not have the correct initialized value');
        assert.equal(cluster.apps.partial,     0,           'Cluster.apps.partial did not have the correct initialized value');
        assert.ok(Array.isArray(cluster.apps.list),         'Cluster.apps.list was not an Array');
        assert.equal(cluster.apps.list.length, 0,           'Cluster.apps.list.length was not initially empty');
      }),

      tdd.test('handleChangeEvent - updated servers tallies', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED', servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});

        // Simulate event
        cluster._handleChangeEvent({type: 'cluster', id: 'cluster1', servers: { up: 1, down: 2, unknown: 3 } });

        assert.equal(cluster.id,               'cluster1',  'Cluster.id did not have the correct initialized value');
        assert.equal(cluster.name,             'cluster1',  'Cluster.name did not have the correct initialized value');
        assert.equal(cluster.state,            'STARTED',   'Cluster.state did not have the correct initialized value');
        assert.equal(cluster.scalingPolicy,    null,        'Cluster.scalingPolicy did not have the correct initialized value');

        assert.equal(cluster.servers.up,       1,           'Cluster.servers.up did not have the updated value');
        assert.equal(cluster.servers.down,     2,           'Cluster.servers.down did not have the updated value');
        assert.equal(cluster.servers.unknown,  3,           'Cluster.servers.unknown did not have the updated value');
        assert.ok(Array.isArray(cluster.servers.list),      'Cluster.servers.list was not an Array');
        assert.equal(cluster.servers.list.length, 0,        'Cluster.servers.list.length was not initially empty');

        assert.equal(cluster.apps.up,          0,           'Cluster.apps.up did not have the correct initialized value');
        assert.equal(cluster.apps.down,        0,           'Cluster.apps.down did not have the correct initialized value');
        assert.equal(cluster.apps.unknown,     0,           'Cluster.apps.unknown did not have the correct initialized value');
        assert.equal(cluster.apps.partial,     0,           'Cluster.apps.partial did not have the correct initialized value');
        assert.ok(Array.isArray(cluster.apps.list),         'Cluster.apps.list was not an Array');
        assert.equal(cluster.apps.list.length, 0,           'Cluster.apps.list.length was not initially empty');
      }),

      tdd.test('handleChangeEvent - added servers', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED', servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});

        // Simulate event
        cluster._handleChangeEvent({type: 'cluster', id: 'cluster1', servers: { up: 1, down: 2, unknown: 3, added: ['localhost,/wlp/usr,server1'] } });

        assert.equal(cluster.id,               'cluster1',  'Cluster.id did not have the correct initialized value');
        assert.equal(cluster.name,             'cluster1',  'Cluster.name did not have the correct initialized value');
        assert.equal(cluster.state,            'STARTED',   'Cluster.state did not have the correct initialized value');
        assert.equal(cluster.scalingPolicy,    null,        'Cluster.scalingPolicy did not have the correct initialized value');

        assert.equal(cluster.servers.up,       1,           'Cluster.servers.up did not have the updated value');
        assert.equal(cluster.servers.down,     2,           'Cluster.servers.down did not have the updated value');
        assert.equal(cluster.servers.unknown,  3,           'Cluster.servers.unknown did not have the updated value');
        assert.ok(Array.isArray(cluster.servers.list),      'Cluster.servers.list was not an Array');
        assert.equal(cluster.servers.list.length, 1,        'Cluster.servers.list.length did not have the correct value');
        assert.equal(cluster.servers.list[0], 'localhost,/wlp/usr,server1', 'Cluster.servers.list[0] did not have the correct value');

        assert.equal(cluster.apps.up,          0,           'Cluster.apps.up did not have the correct initialized value');
        assert.equal(cluster.apps.down,        0,           'Cluster.apps.down did not have the correct initialized value');
        assert.equal(cluster.apps.unknown,     0,           'Cluster.apps.unknown did not have the correct initialized value');
        assert.equal(cluster.apps.partial,     0,           'Cluster.apps.partial did not have the correct initialized value');
        assert.ok(Array.isArray(cluster.apps.list),         'Cluster.apps.list was not an Array');
        assert.equal(cluster.apps.list.length, 0,           'Cluster.apps.list.length was not initially empty');
      }),

      tdd.test('handleChangeEvent - removed servers', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED', servers: {up: 0, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1','localhost,/wlp/usr,server2']}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});

        // Simulate event
        cluster._handleChangeEvent({type: 'cluster', id: 'cluster1', servers: { up: 1, down: 2, unknown: 3, removed: ['localhost,/wlp/usr,server2'] } });

        assert.equal(cluster.id,               'cluster1',  'Cluster.id did not have the correct initialized value');
        assert.equal(cluster.name,             'cluster1',  'Cluster.name did not have the correct initialized value');
        assert.equal(cluster.state,            'STARTED',   'Cluster.state did not have the correct initialized value');
        assert.equal(cluster.scalingPolicy,    null,        'Cluster.scalingPolicy did not have the correct initialized value');

        assert.equal(cluster.servers.up,       1,           'Cluster.servers.up did not have the updated value');
        assert.equal(cluster.servers.down,     2,           'Cluster.servers.down did not have the updated value');
        assert.equal(cluster.servers.unknown,  3,           'Cluster.servers.unknown did not have the updated value');
        assert.ok(Array.isArray(cluster.servers.list),      'Cluster.servers.list was not an Array');
        assert.equal(cluster.servers.list.length, 1,        'Cluster.servers.list.length did not have the correct value');
        assert.equal(cluster.servers.list[0], 'localhost,/wlp/usr,server1', 'Cluster.servers.list[0] did not have the correct value');

        assert.equal(cluster.apps.up,          0,           'Cluster.apps.up did not have the correct initialized value');
        assert.equal(cluster.apps.down,        0,           'Cluster.apps.down did not have the correct initialized value');
        assert.equal(cluster.apps.unknown,     0,           'Cluster.apps.unknown did not have the correct initialized value');
        assert.equal(cluster.apps.partial,     0,           'Cluster.apps.partial did not have the correct initialized value');
        assert.ok(Array.isArray(cluster.apps.list),         'Cluster.apps.list was not an Array');
        assert.equal(cluster.apps.list.length, 0,           'Cluster.apps.list.length was not initially empty');
      }),

      tdd.test('handleChangeEvent - updated apps tallies', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED', servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});

        // Simulate event
        cluster._handleChangeEvent({type: 'cluster', id: 'cluster1', apps: { up: 1, down: 2, unknown: 3, partial: 4 } });

        assert.equal(cluster.id,               'cluster1',  'Cluster.id did not have the correct initialized value');
        assert.equal(cluster.name,             'cluster1',  'Cluster.name did not have the correct initialized value');
        assert.equal(cluster.state,            'STARTED',   'Cluster.state did not have the correct initialized value');
        assert.equal(cluster.scalingPolicy,    null,        'Cluster.scalingPolicy did not have the correct initialized value');

        assert.equal(cluster.servers.up,       0,           'Cluster.servers.up did not have the correct initialized value');
        assert.equal(cluster.servers.down,     0,           'Cluster.servers.down did not have the correct initialized value');
        assert.equal(cluster.servers.unknown,  0,           'Cluster.servers.unknown did not have the correct initialized value');
        assert.ok(Array.isArray(cluster.servers.list),      'Cluster.servers.list was not an Array');
        assert.equal(cluster.servers.list.length, 0,        'Cluster.servers.list.length was not initially empty');

        assert.equal(cluster.apps.up,          1,           'Cluster.apps.up did not have the updated value');
        assert.equal(cluster.apps.down,        2,           'Cluster.apps.down did not have the updated value');
        assert.equal(cluster.apps.unknown,     3,           'Cluster.apps.unknown did not have the updated value');
        assert.equal(cluster.apps.partial,     4,           'Cluster.apps.partial did not have the updated value');
        assert.ok(Array.isArray(cluster.apps.list),         'Cluster.apps.list was not an Array');
        assert.equal(cluster.apps.list.length, 0,           'Cluster.apps.list.length was not initially empty');
      }),

      tdd.test('handleChangeEvent - added apps', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED', servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});

        // Simulate event
        cluster._handleChangeEvent({type: 'cluster', id: 'cluster1', apps: { up: 1, down: 2, unknown: 3, partial: 4, added: ['snoop'] } });

        assert.equal(cluster.id,               'cluster1',  'Cluster.id did not have the correct initialized value');
        assert.equal(cluster.name,             'cluster1',  'Cluster.name did not have the correct initialized value');
        assert.equal(cluster.state,            'STARTED',   'Cluster.state did not have the correct initialized value');
        assert.equal(cluster.scalingPolicy,    null,        'Cluster.scalingPolicy did not have the correct initialized value');

        assert.equal(cluster.servers.up,       0,           'Cluster.servers.up did not have the correct initialized value');
        assert.equal(cluster.servers.down,     0,           'Cluster.servers.down did not have the correct initialized value');
        assert.equal(cluster.servers.unknown,  0,           'Cluster.servers.unknown did not have the correct initialized value');
        assert.ok(Array.isArray(cluster.servers.list),      'Cluster.servers.list was not an Array');
        assert.equal(cluster.servers.list.length, 0,        'Cluster.servers.list.length was not initially empty');

        assert.equal(cluster.apps.up,          1,           'Cluster.apps.up did not have the updated value');
        assert.equal(cluster.apps.down,        2,           'Cluster.apps.down did not have the updated value');
        assert.equal(cluster.apps.unknown,     3,           'Cluster.apps.unknown did not have the updated value');
        assert.equal(cluster.apps.partial,     4,           'Cluster.apps.partial did not have the updated value');
        assert.ok(Array.isArray(cluster.apps.list),         'Cluster.apps.list was not an Array');
        assert.equal(cluster.apps.list.length, 1,           'Cluster.apps.list.length did not have the correct value');
        assert.equal(cluster.apps.list[0],     'snoop',     'Cluster.apps.list[0] did not have the correct value');
      }),

      tdd.test('handleChangeEvent - changed apps', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED', servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: 
          [
           { name: 'snoop', state: 'STARTED', up: 1, down: 0, unknown: 0, servers: { up: 1, /* intentionally short*/ }, tags: [ 'tag1' ], owner: 'Mike', contacts: [ 'Felix' ], note: 'some note' }
           ]}});

        // Simulate event
        cluster._handleChangeEvent({type: 'cluster', id: 'cluster1', apps: { up: 1, down: 2, unknown: 3, partial: 4, changed: 
          [ 
           { name: 'snoop', state: 'STOPPED', up: 0, down: 1, unknown: 0, servers: { up: 1, /* intentionally short*/ }, tags: null, owner: null, contacts: null, note: null }
           ] } });

        assert.equal(cluster.id,               'cluster1',  'Cluster.id did not have the correct initialized value');
        assert.equal(cluster.name,             'cluster1',  'Cluster.name did not have the correct initialized value');
        assert.equal(cluster.state,            'STARTED',   'Cluster.state did not have the correct initialized value');
        assert.equal(cluster.scalingPolicy,    null,        'Cluster.scalingPolicy did not have the correct initialized value');

        assert.equal(cluster.servers.up,       0,           'Cluster.servers.up did not have the correct initialized value');
        assert.equal(cluster.servers.down,     0,           'Cluster.servers.down did not have the correct initialized value');
        assert.equal(cluster.servers.unknown,  0,           'Cluster.servers.unknown did not have the correct initialized value');
        assert.ok(Array.isArray(cluster.servers.list),      'Cluster.servers.list was not an Array');
        assert.equal(cluster.servers.list.length, 0,        'Cluster.servers.list.length was not initially empty');

        assert.equal(cluster.apps.up,          1,           'Cluster.apps.up did not have the updated value');
        assert.equal(cluster.apps.down,        2,           'Cluster.apps.down did not have the updated value');
        assert.equal(cluster.apps.unknown,     3,           'Cluster.apps.unknown did not have the updated value');
        assert.equal(cluster.apps.partial,     4,           'Cluster.apps.partial did not have the updated value');
        assert.ok(Array.isArray(cluster.apps.list),         'Cluster.apps.list was not an Array');
        assert.equal(cluster.apps.list.length, 1,           'Cluster.apps.list.length did not have the correct value');
        
        var appOnCluster = cluster.apps.list[0];
        assert.equal(appOnCluster.name,       'snoop',      'AppOnClusterdid not have the correct value for name');
        assert.equal(appOnCluster.up,         0,            'AppOnClusterdid not have the correct value for up');
        assert.equal(appOnCluster.down,       1,            'AppOnClusterdid not have the correct value for down');
        assert.equal(appOnCluster.unknown,    0,            'AppOnClusterdid not have the correct value for unknown');
        assert.notOk(appOnCluster.note,                     'AppOnClusterdid not have the correct value for note');
        assert.notOk(appOnCluster.owner,                    'AppOnClusterdid not have the correct value for owner');
        assert.notOk(appOnCluster.contacts,                 'AppOnClusterdid not have the correct value for contacts');
        assert.notOk(appOnCluster.tags,                     'AppOnClusterdid not have the correct value for tags');
      }),

      tdd.test('handleChangeEvent - removed apps', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED', servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: [ {name: 'snoop'}, {name: 'testApp'}] }});

        // Simulate event
        cluster._handleChangeEvent({type: 'cluster', id: 'cluster1', apps: { up: 1, down: 2, unknown: 3, partial: 4, removed: ['snoop'] } });

        assert.equal(cluster.id,               'cluster1',  'Cluster.id did not have the correct initialized value');
        assert.equal(cluster.name,             'cluster1',  'Cluster.name did not have the correct initialized value');
        assert.equal(cluster.state,            'STARTED',   'Cluster.state did not have the correct initialized value');
        assert.equal(cluster.scalingPolicy,    null,        'Cluster.scalingPolicy did not have the correct initialized value');

        assert.equal(cluster.servers.up,       0,           'Cluster.servers.up did not have the correct initialized value');
        assert.equal(cluster.servers.down,     0,           'Cluster.servers.down did not have the correct initialized value');
        assert.equal(cluster.servers.unknown,  0,           'Cluster.servers.unknown did not have the correct initialized value');
        assert.ok(Array.isArray(cluster.servers.list),      'Cluster.servers.list was not an Array');
        assert.equal(cluster.servers.list.length, 0,        'Cluster.servers.list.length was not initially empty');

        assert.equal(cluster.apps.up,          1,           'Cluster.apps.up did not have the updated value');
        assert.equal(cluster.apps.down,        2,           'Cluster.apps.down did not have the updated value');
        assert.equal(cluster.apps.unknown,     3,           'Cluster.apps.unknown did not have the updated value');
        assert.equal(cluster.apps.partial,     4,           'Cluster.apps.partial did not have the updated value');
        assert.ok(Array.isArray(cluster.apps.list),         'Cluster.apps.list was not an Array');
        assert.equal(cluster.apps.list.length, 1,           'Cluster.apps.list.length did not have the correct value');
        assert.deepEqual(cluster.apps.list[0],    {name: 'testApp'},   'Cluster.apps.list[0] did not have the correct value');
      }),

      tdd.test('handleChangeEvent - changed alerts', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED', servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});

        // Check pre-event conditions
        assert.notOk(cluster.alerts,                        'Cluster.alerts was not falsy. It should have no default value');

        // Simulate event
        var alerts = { count: 1, unknown: [{id: 'cluster1', type: 'cluster'}] };
        cluster._handleChangeEvent({type: 'cluster', id: 'cluster1', state: 'UNKNOWN', alerts: alerts });

        assert.equal(cluster.id,               'cluster1',  'Cluster.id did not have the correct initialized value');
        assert.equal(cluster.name,             'cluster1',  'Cluster.name did not have the correct initialized value');
        assert.equal(cluster.state,            'UNKNOWN',   'Cluster.state did not update based on the event value');
        assert.equal(cluster.scalingPolicy,    null,        'Cluster.scalingPolicy did not have the correct initialized value');

        assert.equal(cluster.servers.up,       0,           'Cluster.servers.up did not have the correct initialized value');
        assert.equal(cluster.servers.down,     0,           'Cluster.servers.down did not have the correct initialized value');
        assert.equal(cluster.servers.unknown,  0,           'Cluster.servers.unknown did not have the correct initialized value');
        assert.ok(Array.isArray(cluster.servers.list),      'Cluster.servers.list was not an Array');
        assert.equal(cluster.servers.list.length, 0,        'Cluster.servers.list.length was not initially empty');

        assert.equal(cluster.apps.up,          0,           'Cluster.apps.up did not have the correct initialized value');
        assert.equal(cluster.apps.down,        0,           'Cluster.apps.down did not have the correct initialized value');
        assert.equal(cluster.apps.unknown,     0,           'Cluster.apps.unknown did not have the correct initialized value');
        assert.equal(cluster.apps.partial,     0,           'Cluster.apps.partial did not have the correct initialized value');
        assert.ok(Array.isArray(cluster.apps.list),         'Cluster.apps.list was not an Array');
        assert.equal(cluster.apps.list.length, 0,           'Cluster.apps.list.length was not initially empty');

        assert.equal(cluster.alerts,           alerts,      'Cluster.alerts did not update based on the event value');
      }),

      tdd.test('handleChangeEvent - multiple changes', function() {
        var observer = new ClusterObserver();
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED', servers: {up: 0, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: [ {name: 'cluster1,snoop'} ]}});

        cluster.subscribe(observer);

        // Simulate event
        var alerts = { count: 1, unknown: [{id: 'cluster1', type: 'cluster'}] };
        cluster._handleChangeEvent({type: 'cluster', id: 'cluster1',
          state: 'STOPPED', scalingPolicy: 'newPolicy', scalingPolicyEnabled: true,
          servers: { up: 1, down: 2, unknown: 3, added: ['localhost,/wlp/usr,server2'], removed: ['localhost,/wlp/usr,server1'] },
          apps: { up: 4, down: 5, unknown: 6, partial: 7, added: [ {name: 'cluster1,testApp'} ], removed: ['cluster1,snoop'] },
          alerts: alerts });

        assert.equal(cluster.id,               'cluster1',  'Cluster.id did not have the correct initialized value');
        assert.equal(cluster.name,             'cluster1',  'Cluster.name did not have the correct initialized value');
        assert.equal(cluster.state,            'STOPPED',   'Cluster.state did not have the updated value');
        assert.equal(cluster.scalingPolicy,    'newPolicy', 'Cluster.scalingPolicy did not have the updated value');
        assert.isTrue(cluster.scalingPolicyEnabled,         'Cluster.scalingPolicyEnabled did not have the updated value');

        assert.equal(cluster.servers.up,       1,           'Cluster.servers.up did not have the updated value');
        assert.equal(cluster.servers.down,     2,           'Cluster.servers.down did not have the updated value');
        assert.equal(cluster.servers.unknown,  3,           'Cluster.servers.unknown did not have the updated valuek');
        assert.ok(Array.isArray(cluster.servers.list),      'Cluster.servers.list was not an Array');
        assert.equal(cluster.servers.list.length, 1,        'Cluster.servers.list.length did not have the correct value');
        assert.equal(cluster.servers.list[0], 'localhost,/wlp/usr,server2', 'Cluster.servers.list[0] did not have the correct value');

        assert.equal(cluster.apps.up,           4,           'Cluster.apps.up did not have the updated value');
        assert.equal(cluster.apps.down,         5,           'Cluster.apps.down did not have the updated value');
        assert.equal(cluster.apps.unknown,      6,           'Cluster.apps.unknown did not have the updated value');
        assert.equal(cluster.apps.partial,      7,           'Cluster.apps.partial did not have the updated value');
        assert.ok(Array.isArray(cluster.apps.list),          'Cluster.apps.list was not an Array');
        assert.equal(cluster.apps.list.length,  1,           'Cluster.apps.list.length did not have the correct value');
        assert.deepEqual(cluster.apps.list[0],  {name: 'cluster1,testApp'},   'Cluster.apps.list[0] did not have the correct value');

        // Validate the Observer was passed the correct tally objects after the second event
        assert.equal(observer.newState,         'STOPPED',   'ClusterObserver did not get the correct NEW value for state');
        assert.equal(observer.oldState,         'STARTED',   'ClusterObserver did not get the correct OLD value for state');
        assert.equal(observer.newScalingPolicy, 'newPolicy', 'ClusterObserver did not get the correct NEW value for scalingPolicy');
        assert.equal(observer.oldScalingPolicy, null,        'ClusterObserver did not get the correct OLD value for scalingPolicy');
        assert.isTrue(observer.newScalingPolicyEnabled,      'ClusterObserver did not get the correct NEW value for scalingPolicyEnabled');
        assert.notOk(observer.oldScalingPolicyEnabled,       'ClusterObserver did not get the correct OLD value for scalingPolicyEnabled');

        // Check servers tallies
        assert.ok(observer.newServersTally,                         'ClusterObserver did not get the newServersTally object set');
        assert.equal(observer.newServersTally.up,      1,           'ClusterObserver did not get the correct new value for the Servers up tally');
        assert.equal(observer.newServersTally.down,    2,           'ClusterObserver did not get the correct new value for the Servers down tally');
        assert.equal(observer.newServersTally.unknown, 3,           'ClusterObserver did not get the correct new value for the Servers unknown tally');

        assert.ok(observer.oldServersTally,                         'ClusterObserver did not get the oldServersTally object set');
        assert.equal(observer.oldServersTally.up,      0,           'ClusterObserver did not get the correct old value for the Servers up tally');
        assert.equal(observer.oldServersTally.down,    0,           'ClusterObserver did not get the correct old value for the Servers down tally');
        assert.equal(observer.oldServersTally.unknown, 0,           'ClusterObserver did not get the correct old value for the Servers unknown tally');

        // Check servers lists
        assert.ok(observer.newServersList,                          'ClusterObserver.newServersList did not get set, when it should have been');
        assert.equal(observer.newServersList.length,   1,           'ClusterObserver.newServersList was not of expected size');
        assert.equal(observer.newServersList[0], 'localhost,/wlp/usr,server2', 'ClusterObserver.newServersList[0] was not of expected value');

        assert.ok(observer.oldServersList,                          'ClusterObserver.oldServersList did not get set, when it should have been');
        assert.equal(observer.oldServersList.length,   1,           'ClusterObserver.oldServersList was not empty');
        assert.equal(observer.oldServersList[0], 'localhost,/wlp/usr,server1', 'ClusterObserver.oldServersList[0] was not of expected value');

        assert.ok(observer.addedServers,                            'ClusterObserver.addedServers did not get set, when it should have been');
        assert.equal(observer.addedServers.length,     1,           'ClusterObserver.addedServers was not of expected size');
        assert.equal(observer.addedServers[0], 'localhost,/wlp/usr,server2', 'ClusterObserver.addedServers[0] was not of expected value');

        assert.ok(observer.removedServers,                          'ClusterObserver.removedServers did not get set, when it should have been');
        assert.equal(observer.removedServers.length,   1,           'ClusterObserver.removedServers was not empty');
        assert.equal(observer.removedServers[0], 'localhost,/wlp/usr,server1', 'ClusterObserver.removedServers[0] was not of expected value');

        // Check Apps tallies
        assert.ok(observer.newAppsTally,                         'ClusterObserver did not get the newAppsTally object set');
        assert.equal(observer.newAppsTally.up,      4,           'ClusterObserver did not get the correct new value for the Apps up tally');
        assert.equal(observer.newAppsTally.down,    5,           'ClusterObserver did not get the correct new value for the Apps down tally');
        assert.equal(observer.newAppsTally.unknown, 6,           'ClusterObserver did not get the correct new value for the Apps unknown tally');
        assert.equal(observer.newAppsTally.partial, 7,           'ClusterObserver did not get the correct new value for the Apps partial tally');

        assert.ok(observer.oldAppsTally,                         'ClusterObserver did not get the oldAppsTally object set');
        assert.equal(observer.oldAppsTally.up,      0,           'ClusterObserver did not get the correct old value for the Apps up tally');
        assert.equal(observer.oldAppsTally.down,    0,           'ClusterObserver did not get the correct old value for the Apps down tally');
        assert.equal(observer.oldAppsTally.unknown, 0,           'ClusterObserver did not get the correct old value for the Apps unknown tally');
        assert.equal(observer.oldAppsTally.partial, 0,           'ClusterObserver did not get the correct old value for the Apps partial tally');

        // Check Apps lists
        assert.ok(observer.newAppsList,                          'ClusterObserver.newAppsList did not get set, when it should have been');
        assert.equal(observer.newAppsList.length,   1,           'ClusterObserver.newAppsList was not of expected size');
        assert.deepEqual(observer.newAppsList[0],   {name: 'cluster1,testApp'},   'ClusterObserver.newAppsList[0] was not of expected value');

        assert.ok(observer.oldAppsList,                          'ClusterObserver.oldAppsList did not get set, when it should have been');
        assert.equal(observer.oldAppsList.length,   1,           'ClusterObserver.oldAppsList was not empty');
        assert.deepEqual(observer.oldAppsList[0],   {name: 'cluster1,snoop'},     'ClusterObserver.oldAppsList[0] was not of expected value');

        assert.ok(observer.addedApps,                            'ClusterObserver.addedApps did not get set, when it should have been');
        assert.equal(observer.addedApps.length,     1,           'ClusterObserver.addedApps was not of expected size');
        assert.deepEqual(observer.addedApps[0],     {name: 'cluster1,testApp'},   'ClusterObserver.addedApps[0] was not of expected value');

        assert.ok(observer.removedApps,                          'ClusterObserver.removedApps did not get set, when it should have been');
        assert.equal(observer.removedApps.length,   1,           'ClusterObserver.removedApps was not empty');
        assert.equal(observer.removedApps[0],       'cluster1,snoop',     'ClusterObserver.removedApps[0] was not of expected value');

        assert.equal(observer.newAlerts,            alerts,      'ClusterObserver did not get the correct NEW value for alerts');
        assert.notOk(observer.oldAlerts,                         'ClusterObserver did not get the correct OLD value for alerts');
      }),

      tdd.test('handleChangeEvent - cluster was removed should trigger onDestroyed', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED', servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
        var observer = new ClusterObserver();

        cluster.subscribe(observer);

        // Simulate removal event
        cluster._handleChangeEvent({id: 'cluster1', type: 'cluster', state: 'removed'});

        assert.equal(cluster.state, 'STARTED',   'Cluster.state did not have the correct initialized value. It should not change in response to a "removed" event state');
        assert.ok(cluster.isDestroyed,         'Cluster.isDestroyed flag did not get set in response to a "removed" event');

        assert.ok(observer.isDestroyed,        'ClusterObserver did not get the isDestroyed flag set');
      }),

      tdd.test('handleChangeEvent - unset or wrong event id or type is ignored', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED', servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});

        // Simulate ignored events
        cluster._handleChangeEvent({type: 'cluster', up: 1, down: 2, unknown: 3, partial: 4, empty: 5});
        cluster._handleChangeEvent({id: 'wrongcluster', type: 'cluster', up: 1, down: 2, unknown: 3, partial: 4, empty: 5});
        cluster._handleChangeEvent({id: 'cluster1', up: 1, down: 2, unknown: 3, partial: 4, empty: 5});
        cluster._handleChangeEvent({id: 'cluster1', type: 'server', up: 1, down: 2, unknown: 3, partial: 4, empty: 5});

        assert.equal(cluster.id,               'cluster1',  'Cluster.id did not have the correct initialized value');
        assert.equal(cluster.name,             'cluster1',  'Cluster.name did not have the correct initialized value');
        assert.equal(cluster.state,            'STARTED',   'Cluster.state did not have the correct initialized value');
        assert.equal(cluster.scalingPolicy,    null,        'Cluster.scalingPolicy did not have the correct initialized value');

        assert.equal(cluster.servers.up,       0,           'Cluster.servers.up did not have the correct initialized value');
        assert.equal(cluster.servers.down,     0,           'Cluster.servers.down did not have the correct initialized value');
        assert.equal(cluster.servers.unknown,  0,           'Cluster.servers.unknown did not have the correct initialized value');
        assert.ok(Array.isArray(cluster.servers.list),      'Cluster.servers.list was not an Array');
        assert.equal(cluster.servers.list.length, 0,        'Cluster.servers.list.length was not initially empty');

        assert.equal(cluster.apps.up,          0,           'Cluster.apps.up did not have the correct initialized value');
        assert.equal(cluster.apps.down,        0,           'Cluster.apps.down did not have the correct initialized value');
        assert.equal(cluster.apps.unknown,     0,           'Cluster.apps.unknown did not have the correct initialized value');
        assert.equal(cluster.apps.partial,     0,           'Cluster.apps.partial did not have the correct initialized value');
        assert.ok(Array.isArray(cluster.apps.list),         'Cluster.apps.list was not an Array');
        assert.equal(cluster.apps.list.length, 0,           'Cluster.apps.list.length was not initially empty');
      });
    });
  }
});
