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
 * Test cases for Server
 */
define([
        "intern!tdd",
        "intern/chai!assert",
        "intern/dojo/declare",
        "resources/_objects/Server",
        "resources/Observer"
        ],

        function (tdd, assert, declare, Server, Observer) {

  var ServerObserver = declare([Observer], {
    id: 'testObserver',

    onDestroyed: function() {
      this.isDestroyed = true;
    },

    onStateChange: function(newState, oldState) {
      this.newState = newState;
      this.oldState = oldState;
    },

    onWlpInstallDirChange: function(newWlpInstallDir, oldWlpInstallDir) {
      this.newWlpInstallDir = newWlpInstallDir;
      this.oldWlpInstallDir = oldWlpInstallDir;
    },

    onClusterChange: function(newCluster, oldCluster) {
      this.newCluster = newCluster;
      this.oldCluster = oldCluster;
    },

    onScalingPolicyChange: function(newScalingPolicy, oldScalingPolicy) {
      this.newScalingPolicy = newScalingPolicy;
      this.oldScalingPolicy = oldScalingPolicy;
    },

    onScalingPolicyEnabledChange: function(newScalingPolicyEnabled, oldScalingPolicyEnabled) {
      this.newScalingPolicyEnabled = newScalingPolicyEnabled;
      this.oldScalingPolicyEnabled = oldScalingPolicyEnabled;
    },

    onIsCollectiveControllerChange: function(newIsCollectiveController, oldIsCollectiveController) {
      this.newIsCollectiveController = newIsCollectiveController;
      this.oldIsCollectiveController = oldIsCollectiveController;
    },

    onAppsTallyChange: function(newTally, oldTally) {
      this.newTally = newTally;
      this.oldTally = oldTally;
    },

    onAppsListChange: function(newList, oldList, added, removed, changed) {
      this.newList = newList;
      this.oldList = oldList;
      this.added = added;
      this.removed = removed;
      this.changed = changed;
    },

    onAlertsChange: function(newAlerts, oldAlerts) {
      this.newAlerts = newAlerts;
      this.oldAlerts = oldAlerts;
    }
  });

  with(assert) {

    /**
     * Defines the 'Server Object Tests' module test suite.
     */
    tdd.suite('Server Object Tests', function() {
      tdd.test('constructor - no initialization object', function() {
        try {
          new Server();
          assert.ok(false, 'Server was successfully created when it should have failed - an initialization object is required');
        } catch(error) {
          assert.equal(error, 'ObservableResource created without an initialization object', 'Error reported did not match expected error');
        }
      }),

      tdd.test('constructor - no id', function() {
        try {
          new Server({wlpInstallDir: '/wlp', state: 'STARTED', apps: {up: 0, down: 0, unknown: 0, list: []}});
          assert.ok(false, 'Server was successfully created when it should have failed - an id is required');
        } catch(error) {
          assert.equal(error, 'ObservableResource created without an id', 'Error reported did not match expected error');
        }
      }),

      tdd.test('constructor - no initial state', function() {
        try {
          new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', apps: {up: 0, down: 0, unknown: 0, list: []}});
          assert.ok(false, 'Server "localhost,/wlp/usr,server1" was successfully created when it should have failed - an initial state is required');
        } catch(error) {
          assert.equal(error, 'StatefulResource created without a state', 'Error reported did not match expected error');
        }
      }),

      tdd.test('constructor - no initial wlpInstallDir', function() {
        try {
          new Server({id: 'localhost,/wlp/usr,server1', state: 'STARTED', apps: {up: 0, down: 0, unknown: 0, list: []}});
          assert.ok(false, 'Server "localhost,/wlp/usr,server1" was successfully created when it should have failed - an initial wlpInstallDir is required');
        } catch(error) {
          assert.equal(error, 'Server "localhost,/wlp/usr,server1" created without an initial wlpInstallDir', 'Error reported did not match expected error');
        }
      }),

      tdd.test('constructor - no initial apps object', function() {
        try {
          new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED'});
          assert.ok(false, 'Server "localhost,/wlp/usr,server1" was successfully created when it should have failed - an initial apps object is required');
        } catch(error) {
          assert.equal(error, 'Server "localhost,/wlp/usr,server1" created without an initial apps object', 'Error reported did not match expected error');
        }
      }),

      tdd.test('constructor - no initial apps.up', function() {
        try {
          new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', apps: {down: 0, unknown: 0, list: []}});
          assert.ok(false, 'Server "localhost,/wlp/usr,server1" was successfully created when it should have failed - an initial apps.up tally is required');
        } catch(error) {
          assert.equal(error, 'Server "localhost,/wlp/usr,server1" created without an initial apps.up tally', 'Error reported did not match expected error');
        }
      }),

      tdd.test('constructor - no initial apps.down', function() {
        try {
          new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', apps: {up: 0, unknown: 0, list: []}});
          assert.ok(false, 'Server "localhost,/wlp/usr,server1" was successfully created when it should have failed - an initial apps.down tally is required');
        } catch(error) {
          assert.equal(error, 'Server "localhost,/wlp/usr,server1" created without an initial apps.down tally', 'Error reported did not match expected error');
        }
      }),

      tdd.test('constructor - no initial apps.unknown', function() {
        try {
          new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', apps: {up: 0, down: 0, list: []}});
          assert.ok(false, 'Server "localhost,/wlp/usr,server1" was successfully created when it should have failed - an initial apps.unknown tally is required');
        } catch(error) {
          assert.equal(error, 'Server "localhost,/wlp/usr,server1" created without an initial apps.unknown tally', 'Error reported did not match expected error');
        }
      }),

      tdd.test('constructor - no initial apps.list', function() {
        try {
          new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', apps: {up: 0, down: 0, unknown: 0}});
          assert.ok(false, 'Server "localhost,/wlp/usr,server1" was successfully created when it should have failed - an initial apps.list array is required');
        } catch(error) {
          assert.equal(error, 'Server "localhost,/wlp/usr,server1" created without an initial apps.list array', 'Error reported did not match expected error');
        }
      }),

      tdd.test('constructor - initial apps.list has wrong element object', function() {
        try {
          new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', apps: {up: 0, down: 0, unknown: 0, list: ['app1']}});
          assert.ok(false, 'Server "localhost,/wlp/usr,server1" was successfully created when it should have failed - an initial apps.list array with the correct element objects is required');
        } catch(error) {
          assert.equal(error, 'Server "localhost,/wlp/usr,server1" created without an initial apps.list array with some elements which are not objects with name and state', 'Error reported did not match expected error');
        }
      }),

      tdd.test('constructor - empty cluster, scalingPolicy, and apps', function() {
        var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', apps: {up: 0, down: 0, unknown: 0, list: []}});

        assert.equal(server.id, 'localhost,/wlp/usr,server1', 'Server.id did not have the correct initialized value');
        assert.equal(server.name,             'server1',   'Server.name did not have the correct initialized value');
        assert.equal(server.userdir,          '/wlp/usr',  'Server.userdir did not have the correct initialized value');
        assert.equal(server.host,             'localhost', 'Server.host did not have the correct initialized value');
        assert.equal(server.wlpInstallDir,    '/wlp',      'Server.wlpInstallDir did not have the correct initialized value');
        assert.equal(server.state,            'STARTED',   'Server.state did not have the correct initialized value');
        assert.equal(server.cluster,          null,        'Server.cluster did not have the correct initialized value');
        assert.equal(server.scalingPolicy,    null,        'Server.scalingPolicy did not have the correct initialized value');
        assert.notOk(server.scalingPolicyEnabled,          'Server.scalingPolicyEnabled did not have the correct initialized value');
        assert.notOk(server.isCollectiveController,        'Server.isCollectiveController did not have the correct initialized value');
        assert.equal(server.runtimeType,      'Liberty',   'Server.runtimeType did not have the correct initialized type');
        assert.notOk(server.containerType,                 'Server.containerType was initialized when no value was set');

        assert.equal(server.apps.up,          0,           'Server.apps.up did not have the correct initialized value');
        assert.equal(server.apps.down,        0,           'Server.apps.down did not have the correct initialized value');
        assert.equal(server.apps.unknown,     0,           'Server.apps.unknown did not have the correct initialized value');
        assert.ok(Array.isArray(server.apps.list),         'Server.apps.list was not an Array');
        assert.equal(server.apps.list.length, 0,           'Server.apps.list.length was not initially empty');

        assert.equal(server.ports.httpPorts[0], "9080",		'Server.apps.ports.httpPorts did not have the correct initialized value');
        assert.equal(server.ports.httpsPorts[0], "9443",	'Server.apps.ports.httpsPorts did not have the correct initialized value');

        assert.notOk(server.isAdminCenterServer,           'The server should not be set to be an Admin Center server unless explicitly set in the constructor');
      }),

      tdd.test('constructor - set as AdminCenter server', function() {
        var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', isCollectiveController: true, isAdminCenterServer: true, apps: {up: 0, down: 0, unknown: 0, list: []}});

        assert.equal(server.id, 'localhost,/wlp/usr,server1', 'Server.id did not have the correct initialized value');
        assert.equal(server.name,             'server1',   'Server.name did not have the correct initialized value');
        assert.equal(server.userdir,          '/wlp/usr',  'Server.userdir did not have the correct initialized value');
        assert.equal(server.host,             'localhost', 'Server.host did not have the correct initialized value');
        assert.equal(server.wlpInstallDir,    '/wlp',      'Server.wlpInstallDir did not have the correct initialized value');
        assert.equal(server.state,            'STARTED',   'Server.state did not have the correct initialized value');
        assert.equal(server.cluster,          null,        'Server.cluster did not have the correct initialized value');
        assert.equal(server.scalingPolicy,    null,        'Server.scalingPolicy did not have the correct initialized value');
        assert.notOk(server.scalingPolicyEnabled,          'Server.scalingPolicyEnabled did not have the correct initialized value');
        assert.isTrue(server.isCollectiveController,       'Server.isCollectiveController did not have the correct initialized value');
        assert.equal(server.runtimeType,      'Liberty',   'Server.runtimeType did not have the correct initialized type');
        assert.notOk(server.containerType,                 'Server.containerType was initialized when no value was set');

        assert.equal(server.apps.up,          0,           'Server.apps.up did not have the correct initialized value');
        assert.equal(server.apps.down,        0,           'Server.apps.down did not have the correct initialized value');
        assert.equal(server.apps.unknown,     0,           'Server.apps.unknown did not have the correct initialized value');
        assert.ok(Array.isArray(server.apps.list),         'Server.apps.list was not an Array');
        assert.equal(server.apps.list.length, 0,           'Server.apps.list.length was not initially empty');

        assert.equal(server.ports.httpPorts[0], "9080",		'Server.apps.ports.httpPorts did not have the correct initialized value');
        assert.equal(server.ports.httpsPorts[0], "9443",	'Server.apps.ports.httpsPorts did not have the correct initialized value');

        assert.ok(server.isAdminCenterServer,              'The server was not be set to be an Admin Center server even though the flag was passed at construction');
      }),

      tdd.test('constructor - with cluster, scalingPolicy, and apps', function() {
        var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', cluster: 'cluster1', scalingPolicy: 'default', runtimeType: 'Liberty', apps: {up: 1, down: 2, unknown: 3, list: [{name: 'snoop', state: 'STARTED'}]}, alerts: {count: 8}});

        assert.equal(server.id,               'localhost,/wlp/usr,server1',  'Server.id did not have the correct initialized value');
        assert.equal(server.name,             'server1',   'Server.name did not have the correct initialized value');
        assert.equal(server.userdir,          '/wlp/usr',  'Server.userdir did not have the correct initialized value');
        assert.equal(server.host,             'localhost', 'Server.host did not have the correct initialized value');
        assert.equal(server.wlpInstallDir,    '/wlp',      'Server.wlpInstallDir did not have the correct initialized value');
        assert.equal(server.state,            'STARTED',   'Server.state did not have the correct initialized value');
        assert.equal(server.cluster,          'cluster1',  'Server.cluster did not have the correct initialized value');
        assert.equal(server.scalingPolicy,    'default',   'Server.scalingPolicy did not have the correct initialized value');
        assert.equal(server.runtimeType,      'Liberty',   'Server.runtimeType did not have the correct initialized type');
        assert.notOk(server.containerType,                 'Server.containerType was initialized when no value was set');

        assert.equal(server.apps.up,          1,           'Server.apps.up did not have the correct initialized value');
        assert.equal(server.apps.down,        2,           'Server.apps.down did not have the correct initialized value');
        assert.equal(server.apps.unknown,     3,           'Server.apps.unknown did not have the correct initialized value');
        assert.ok(Array.isArray(server.apps.list),         'Server.apps.list was not an Array');
        assert.equal(server.apps.list.length, 1,           'Server.apps.list.length was not the correct value');
        assert.equal(JSON.stringify(server.apps.list[0]), JSON.stringify({name:'snoop',state:'STARTED'}), 'Server.apps.list[0] did not have the correct value');

        assert.ok(server.alerts,                           'Server did not have an alerts attribute');
        assert.equal(server.alerts.count,     8,           'Server.alerts.count did not have the correct initialized value');

        assert.equal(server.ports.httpPorts[0], "9080",		'Server.apps.ports.httpPorts did not have the correct initialized value');
        assert.equal(server.ports.httpsPorts[0], "9443",	'Server.apps.ports.httpsPorts did not have the correct initialized value');

        assert.notOk(server.isAdminCenterServer,           'The server should not be set to be an Admin Center server unless explicitly set in the constructor');
      }),

      tdd.test('handleChangeEvent - changed state', function() {
        var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', runtimeType: 'Liberty', apps: {up: 0, down: 0, unknown: 0, list: []}});

        // Check pre-event conditions
        assert.equal(server.state,            'STARTED',   'Server.state did not have the correct initialized value');

        // Simulate event
        server._handleChangeEvent({type: 'server', id: 'localhost,/wlp/usr,server1', state: 'STOPPED' });

        assert.equal(server.id, 'localhost,/wlp/usr,server1', 'Server.id did not have the correct initialized value');
        assert.equal(server.name,             'server1',   'Server.name did not have the correct initialized value');
        assert.equal(server.userdir,          '/wlp/usr',  'Server.userdir did not have the correct initialized value');
        assert.equal(server.host,             'localhost', 'Server.host did not have the correct initialized value');
        assert.equal(server.wlpInstallDir,    '/wlp',      'Server.wlpInstallDir did not have the correct initialized value');
        assert.equal(server.state,            'STOPPED',   'Server.state did not update based on the event value');
        assert.equal(server.cluster,          null,        'Server.cluster did not have the correct initialized value');
        assert.equal(server.scalingPolicy,    null,        'Server.scalingPolicy did not have the correct initialized value');
        assert.equal(server.runtimeType,      'Liberty',   'Server.runtimeType did not have the correct initialized type');
        assert.notOk(server.containerType,                 'Server.containerType was initialized when no value was set');

        assert.equal(server.apps.up,          0,           'Server.apps.up did not have the correct initialized value');
        assert.equal(server.apps.down,        0,           'Server.apps.down did not have the correct initialized value');
        assert.equal(server.apps.unknown,     0,           'Server.apps.unknown did not have the correct initialized value');
        assert.ok(Array.isArray(server.apps.list),         'Server.apps.list was not an Array');
        assert.equal(server.apps.list.length, 0,           'Server.apps.list.length was not initially empty');

        assert.equal(server.ports.httpPorts[0], "9080",		'Server.apps.ports.httpPorts did not have the correct initialized value');
        assert.equal(server.ports.httpsPorts[0], "9443",	'Server.apps.ports.httpsPorts did not have the correct initialized value');

        assert.notOk(server.isAdminCenterServer,           'The server should not be set to be an Admin Center server unless explicitly set in the constructor');
      }),

      tdd.test('handleChangeEvent - changed state from operation', function() {
        var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', runtimeType: 'Liberty', apps: {up: 0, down: 0, unknown: 0, list: []}});

        // Check pre-event conditions
        assert.equal(server.state,            'STARTED',   'Server.state did not have the correct initialized value');

        // Simulate event
        server._handleChangeEvent({type: 'server', id: 'localhost,/wlp/usr,server1', state: 'STOPPING' });

        assert.equal(server.id, 'localhost,/wlp/usr,server1', 'Server.id did not have the correct initialized value');
        assert.equal(server.name,             'server1',   'Server.name did not have the correct initialized value');
        assert.equal(server.userdir,          '/wlp/usr',  'Server.userdir did not have the correct initialized value');
        assert.equal(server.host,             'localhost', 'Server.host did not have the correct initialized value');
        assert.equal(server.wlpInstallDir,    '/wlp',      'Server.wlpInstallDir did not have the correct initialized value');
        assert.equal(server.state,            'STOPPING',  'Server.state did not update based on the event value');
        assert.equal(server.cluster,          null,        'Server.cluster did not have the correct initialized value');
        assert.equal(server.scalingPolicy,    null,        'Server.scalingPolicy did not have the correct initialized value');
        assert.equal(server.runtimeType,      'Liberty',   'Server.runtimeType did not have the correct initialized type');
        assert.notOk(server.containerType,                 'Server.containerType was initialized when no value was set');

        assert.equal(server.apps.up,          0,           'Server.apps.up did not have the correct initialized value');
        assert.equal(server.apps.down,        0,           'Server.apps.down did not have the correct initialized value');
        assert.equal(server.apps.unknown,     0,           'Server.apps.unknown did not have the correct initialized value');
        assert.ok(Array.isArray(server.apps.list),         'Server.apps.list was not an Array');
        assert.equal(server.apps.list.length, 0,           'Server.apps.list.length was not initially empty');

        assert.equal(server.ports.httpPorts[0], "9080",		'Server.apps.ports.httpPorts did not have the correct initialized value');
        assert.equal(server.ports.httpsPorts[0], "9443",	'Server.apps.ports.httpsPorts did not have the correct initialized value');
        
        assert.notOk(server.isAdminCenterServer,           'The server should not be set to be an Admin Center server unless explicitly set in the constructor');

        server._handleChangeEvent({type: 'server', id: 'localhost,/wlp/usr,server1', state: 'STARTING' });
        assert.equal(server.state,            'STARTING',  'Server.state did not update based on the event value');
      }),

      tdd.test('handleChangeEvent - changed wlpInstallDir', function() {
        var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', runtimeType: 'Liberty', apps: {up: 0, down: 0, unknown: 0, list: []}});

        // Check pre-event conditions
        assert.equal(server.wlpInstallDir,    '/wlp',      'Server.wlpInstallDir did not have the correct initialized value');

        // Simulate event
        server._handleChangeEvent({type: 'server', id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp2' });

        assert.equal(server.id, 'localhost,/wlp/usr,server1', 'Server.id did not have the correct initialized value');
        assert.equal(server.name,             'server1',   'Server.name did not have the correct initialized value');
        assert.equal(server.userdir,          '/wlp/usr',  'Server.userdir did not have the correct initialized value');
        assert.equal(server.host,             'localhost', 'Server.host did not have the correct initialized value');
        assert.equal(server.wlpInstallDir,    '/wlp2',     'Server.wlpInstallDir did not update based on the event value');
        assert.equal(server.state,            'STARTED',   'Server.state did not have the correct initialized value');
        assert.equal(server.cluster,          null,        'Server.cluster did not have the correct initialized value');
        assert.equal(server.scalingPolicy,    null,        'Server.scalingPolicy did not have the correct initialized value');
        assert.equal(server.runtimeType,      'Liberty',   'Server.runtimeType did not have the correct initialized type');
        assert.notOk(server.containerType,                 'Server.containerType was initialized when no value was set');

        assert.equal(server.apps.up,          0,           'Server.apps.up did not have the correct initialized value');
        assert.equal(server.apps.down,        0,           'Server.apps.down did not have the correct initialized value');
        assert.equal(server.apps.unknown,     0,           'Server.apps.unknown did not have the correct initialized value');
        assert.ok(Array.isArray(server.apps.list),         'Server.apps.list was not an Array');
        assert.equal(server.apps.list.length, 0,           'Server.apps.list.length was not initially empty');

        assert.equal(server.ports.httpPorts[0], "9080",		'Server.apps.ports.httpPorts did not have the correct initialized value');
        assert.equal(server.ports.httpsPorts[0], "9443",	'Server.apps.ports.httpsPorts did not have the correct initialized value');

        assert.notOk(server.isAdminCenterServer,           'The server should not be set to be an Admin Center server unless explicitly set in the constructor');
      }),

      tdd.test('handleChangeEvent - added cluster', function() {
        var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', runtimeType: 'Liberty', apps: {up: 0, down: 0, unknown: 0, list: []}});

        // Check pre-event conditions
        assert.equal(server.cluster,          null,        'Server.cluster did not have the correct initialized value');

        // Simulate event
        server._handleChangeEvent({type: 'server', id: 'localhost,/wlp/usr,server1', cluster: 'cluster1' });

        assert.equal(server.id, 'localhost,/wlp/usr,server1', 'Server.id did not have the correct initialized value');
        assert.equal(server.name,             'server1',   'Server.name did not have the correct initialized value');
        assert.equal(server.userdir,          '/wlp/usr',  'Server.userdir did not have the correct initialized value');
        assert.equal(server.host,             'localhost', 'Server.host did not have the correct initialized value');
        assert.equal(server.wlpInstallDir,    '/wlp',      'Server.wlpInstallDir did not have the correct initialized value');
        assert.equal(server.state,            'STARTED',   'Server.state did not have the correct initialized value');
        assert.equal(server.cluster,          'cluster1',  'Server.cluster did not update based on the event value');
        assert.equal(server.scalingPolicy,    null,        'Server.scalingPolicy did not have the correct initialized value');
        assert.equal(server.runtimeType,      'Liberty',   'Server.runtimeType did not have the correct initialized type');
        assert.notOk(server.containerType,                 'Server.containerType was initialized when no value was set');

        assert.equal(server.apps.up,          0,           'Server.apps.up did not have the correct initialized value');
        assert.equal(server.apps.down,        0,           'Server.apps.down did not have the correct initialized value');
        assert.equal(server.apps.unknown,     0,           'Server.apps.unknown did not have the correct initialized value');
        assert.ok(Array.isArray(server.apps.list),         'Server.apps.list was not an Array');
        assert.equal(server.apps.list.length, 0,           'Server.apps.list.length was not initially empty');

        assert.equal(server.ports.httpPorts[0], "9080",		'Server.apps.ports.httpPorts did not have the correct initialized value');
        assert.equal(server.ports.httpsPorts[0], "9443",	'Server.apps.ports.httpsPorts did not have the correct initialized value');

        assert.notOk(server.isAdminCenterServer,           'The server should not be set to be an Admin Center server unless explicitly set in the constructor');
      }),

      tdd.test('handleChangeEvent - changed cluster', function() {
        var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', cluster: 'cluster1', runtimeType: 'Liberty', apps: {up: 0, down: 0, unknown: 0, list: []}});

        // Check pre-event conditions
        assert.equal(server.cluster,          'cluster1',  'Server.cluster did not have the correct initialized value');

        // Simulate event
        server._handleChangeEvent({type: 'server', id: 'localhost,/wlp/usr,server1', cluster: 'cluster2' });

        assert.equal(server.id, 'localhost,/wlp/usr,server1', 'Server.id did not have the correct initialized value');
        assert.equal(server.name,             'server1',   'Server.name did not have the correct initialized value');
        assert.equal(server.userdir,          '/wlp/usr',  'Server.userdir did not have the correct initialized value');
        assert.equal(server.host,             'localhost', 'Server.host did not have the correct initialized value');
        assert.equal(server.wlpInstallDir,    '/wlp',      'Server.wlpInstallDir did not have the correct initialized value');
        assert.equal(server.state,            'STARTED',   'Server.state did not have the correct initialized value');
        assert.equal(server.cluster,          'cluster2',  'Server.cluster did not update based on the event value');
        assert.equal(server.scalingPolicy,    null,        'Server.scalingPolicy did not have the correct initialized value');
        assert.equal(server.runtimeType,      'Liberty',   'Server.runtimeType did not have the correct initialized type');
        assert.notOk(server.containerType,                 'Server.containerType was initialized when no value was set');

        assert.equal(server.apps.up,          0,           'Server.apps.up did not have the correct initialized value');
        assert.equal(server.apps.down,        0,           'Server.apps.down did not have the correct initialized value');
        assert.equal(server.apps.unknown,     0,           'Server.apps.unknown did not have the correct initialized value');
        assert.ok(Array.isArray(server.apps.list),         'Server.apps.list was not an Array');
        assert.equal(server.apps.list.length, 0,           'Server.apps.list.length was not initially empty');

        assert.equal(server.ports.httpPorts[0], "9080",		'Server.apps.ports.httpPorts did not have the correct initialized value');
        assert.equal(server.ports.httpsPorts[0], "9443",	'Server.apps.ports.httpsPorts did not have the correct initialized value');
      }),

      tdd.test('handleChangeEvent - removed cluster', function() {
        var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', cluster: 'cluster1', runtimeType: 'Liberty', apps: {up: 0, down: 0, unknown: 0, list: []}});

        // Check pre-event conditions
        assert.equal(server.cluster,          'cluster1',  'Server.cluster did not have the correct initialized value');

        // Simulate event
        server._handleChangeEvent({type: 'server', id: 'localhost,/wlp/usr,server1', cluster: null });

        assert.equal(server.id, 'localhost,/wlp/usr,server1', 'Server.id did not have the correct initialized value');
        assert.equal(server.name,             'server1',   'Server.name did not have the correct initialized value');
        assert.equal(server.userdir,          '/wlp/usr',  'Server.userdir did not have the correct initialized value');
        assert.equal(server.host,             'localhost', 'Server.host did not have the correct initialized value');
        assert.equal(server.wlpInstallDir,    '/wlp',      'Server.wlpInstallDir did not have the correct initialized value');
        assert.equal(server.state,            'STARTED',   'Server.state did not have the correct initialized value');
        assert.equal(server.cluster,          null,        'Server.cluster did not update based on the event value');
        assert.equal(server.scalingPolicy,    null,        'Server.scalingPolicy did not have the correct initialized value');
        assert.equal(server.runtimeType,      'Liberty',   'Server.runtimeType did not have the correct initialized type');
        assert.notOk(server.containerType,                 'Server.containerType was initialized when no value was set');

        assert.equal(server.apps.up,          0,           'Server.apps.up did not have the correct initialized value');
        assert.equal(server.apps.down,        0,           'Server.apps.down did not have the correct initialized value');
        assert.equal(server.apps.unknown,     0,           'Server.apps.unknown did not have the correct initialized value');
        assert.ok(Array.isArray(server.apps.list),         'Server.apps.list was not an Array');
        assert.equal(server.apps.list.length, 0,           'Server.apps.list.length was not initially empty');

        assert.equal(server.ports.httpPorts[0], "9080",		'Server.apps.ports.httpPorts did not have the correct initialized value');
        assert.equal(server.ports.httpsPorts[0], "9443",	'Server.apps.ports.httpsPorts did not have the correct initialized value');
      }),

      tdd.test('handleChangeEvent - added scalingPolicy', function() {
        var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', runtimeType: 'Liberty', apps: {up: 0, down: 0, unknown: 0, list: []}});

        // Check pre-event conditions
        assert.equal(server.scalingPolicy,    null,        'Server.scalingPolicy did not have the correct initialized value');

        // Simulate event
        server._handleChangeEvent({type: 'server', id: 'localhost,/wlp/usr,server1', scalingPolicy: 'default' });

        assert.equal(server.id, 'localhost,/wlp/usr,server1', 'Server.id did not have the correct initialized value');
        assert.equal(server.name,             'server1',   'Server.name did not have the correct initialized value');
        assert.equal(server.userdir,          '/wlp/usr',  'Server.userdir did not have the correct initialized value');
        assert.equal(server.host,             'localhost', 'Server.host did not have the correct initialized value');
        assert.equal(server.wlpInstallDir,    '/wlp',      'Server.wlpInstallDir did not have the correct initialized value');
        assert.equal(server.state,            'STARTED',   'Server.state did not have the correct initialized value');
        assert.equal(server.cluster,          null,        'Server.cluster did not have the correct initialized value');
        assert.equal(server.scalingPolicy,    'default',   'Server.scalingPolicy did not update based on the event value');
        assert.equal(server.runtimeType,      'Liberty',   'Server.runtimeType did not have the correct initialized type');
        assert.notOk(server.containerType,                 'Server.containerType was initialized when no value was set');

        assert.equal(server.apps.up,          0,           'Server.apps.up did not have the correct initialized value');
        assert.equal(server.apps.down,        0,           'Server.apps.down did not have the correct initialized value');
        assert.equal(server.apps.unknown,     0,           'Server.apps.unknown did not have the correct initialized value');
        assert.ok(Array.isArray(server.apps.list),         'Server.apps.list was not an Array');
        assert.equal(server.apps.list.length, 0,           'Server.apps.list.length was not initially empty');

        assert.equal(server.ports.httpPorts[0], "9080",		'Server.apps.ports.httpPorts did not have the correct initialized value');
        assert.equal(server.ports.httpsPorts[0], "9443",	'Server.apps.ports.httpsPorts did not have the correct initialized value');
      }),

      tdd.test('handleChangeEvent - changed scalingPolicy', function() {
        var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', scalingPolicy: 'default', runtimeType: 'Liberty', apps: {up: 0, down: 0, unknown: 0, list: []}});

        // Check pre-event conditions
        assert.equal(server.scalingPolicy,    'default',   'Server.scalingPolicy did not have the correct initialized value');

        // Simulate event
        server._handleChangeEvent({type: 'server', id: 'localhost,/wlp/usr,server1', scalingPolicy: 'newPolicy' });

        assert.equal(server.id, 'localhost,/wlp/usr,server1', 'Server.id did not have the correct initialized value');
        assert.equal(server.name,             'server1',   'Server.name did not have the correct initialized value');
        assert.equal(server.userdir,          '/wlp/usr',  'Server.userdir did not have the correct initialized value');
        assert.equal(server.host,             'localhost', 'Server.host did not have the correct initialized value');
        assert.equal(server.wlpInstallDir,    '/wlp',      'Server.wlpInstallDir did not have the correct initialized value');
        assert.equal(server.state,            'STARTED',   'Server.state did not have the correct initialized value');
        assert.equal(server.cluster,          null,        'Server.cluster did not have the correct initialized value');
        assert.equal(server.scalingPolicy,    'newPolicy', 'Server.scalingPolicy did not update based on the event value');
        assert.equal(server.runtimeType,      'Liberty',   'Server.runtimeType did not have the correct initialized type');
        assert.notOk(server.containerType,                 'Server.containerType was initialized when no value was set');

        assert.equal(server.apps.up,          0,           'Server.apps.up did not have the correct initialized value');
        assert.equal(server.apps.down,        0,           'Server.apps.down did not have the correct initialized value');
        assert.equal(server.apps.unknown,     0,           'Server.apps.unknown did not have the correct initialized value');
        assert.ok(Array.isArray(server.apps.list),         'Server.apps.list was not an Array');
        assert.equal(server.apps.list.length, 0,           'Server.apps.list.length was not initially empty');

        assert.equal(server.ports.httpPorts[0], "9080",		'Server.apps.ports.httpPorts did not have the correct initialized value');
        assert.equal(server.ports.httpsPorts[0], "9443",	'Server.apps.ports.httpsPorts did not have the correct initialized value');
      }),

      tdd.test('handleChangeEvent - removed scalingPolicy', function() {
        var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', scalingPolicy: 'default', runtimeType: 'Liberty', apps: {up: 0, down: 0, unknown: 0, list: []}});

        // Check pre-event conditions
        assert.equal(server.scalingPolicy,    'default',   'Server.scalingPolicy did not have the correct initialized value');

        // Simulate event
        server._handleChangeEvent({type: 'server', id: 'localhost,/wlp/usr,server1', scalingPolicy: null });

        assert.equal(server.id,             'localhost,/wlp/usr,server1', 'Server.id did not have the correct initialized value');
        assert.equal(server.name,             'server1',   'Server.name did not have the correct initialized value');
        assert.equal(server.userdir,          '/wlp/usr',  'Server.userdir did not have the correct initialized value');
        assert.equal(server.host,             'localhost', 'Server.host did not have the correct initialized value');
        assert.equal(server.wlpInstallDir,    '/wlp',      'Server.wlpInstallDir did not have the correct initialized value');
        assert.equal(server.state,            'STARTED',   'Server.state did not have the correct initialized value');
        assert.equal(server.cluster,          null,        'Server.cluster did not have the correct initialized value');
        assert.equal(server.scalingPolicy,    null,        'Server.scalingPolicy did not update based on the event value');
        assert.equal(server.runtimeType,      'Liberty',   'Server.runtimeType did not have the correct initialized type');
        assert.notOk(server.containerType,                 'Server.containerType was initialized when no value was set');

        assert.equal(server.apps.up,          0,           'Server.apps.up did not have the correct initialized value');
        assert.equal(server.apps.down,        0,           'Server.apps.down did not have the correct initialized value');
        assert.equal(server.apps.unknown,     0,           'Server.apps.unknown did not have the correct initialized value');
        assert.ok(Array.isArray(server.apps.list),         'Server.apps.list was not an Array');
        assert.equal(server.apps.list.length, 0,           'Server.apps.list.length was not initially empty');

        assert.equal(server.ports.httpPorts[0], "9080",		'Server.apps.ports.httpPorts did not have the correct initialized value');
        assert.equal(server.ports.httpsPorts[0], "9443",	'Server.apps.ports.httpsPorts did not have the correct initialized value');
      }),

      tdd.test('handleChangeEvent - updated apps tallies', function() {
        var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', runtimeType: 'Liberty', apps: {up: 0, down: 0, unknown: 0, list: []}});

        // Simulate event
        server._handleChangeEvent({type: 'server', id: 'localhost,/wlp/usr,server1', apps: { up: 1, down: 2, unknown: 3 } });

        assert.equal(server.id, 'localhost,/wlp/usr,server1', 'Server.id did not have the correct initialized value');
        assert.equal(server.name,             'server1',   'Server.name did not have the correct initialized value');
        assert.equal(server.userdir,          '/wlp/usr',  'Server.userdir did not have the correct initialized value');
        assert.equal(server.host,             'localhost', 'Server.host did not have the correct initialized value');
        assert.equal(server.wlpInstallDir,    '/wlp',      'Server.wlpInstallDir did not have the correct initialized value');
        assert.equal(server.state,            'STARTED',   'Server.state did not have the correct initialized value');
        assert.equal(server.cluster,          null,        'Server.cluster did not have the correct initialized value');
        assert.equal(server.scalingPolicy,    null,        'Server.scalingPolicy did not have the correct initialized value');
        assert.equal(server.runtimeType,      'Liberty',   'Server.runtimeType did not have the correct initialized type');
        assert.notOk(server.containerType,                 'Server.containerType was initialized when no value was set');

        assert.equal(server.apps.up,          1,           'Server.apps.up did not have the updated value');
        assert.equal(server.apps.down,        2,           'Server.apps.down did not have the updated value');
        assert.equal(server.apps.unknown,     3,           'Server.apps.unknown did not have the updated value');
        assert.ok(Array.isArray(server.apps.list),         'Server.apps.list was not an Array');
        assert.equal(server.apps.list.length, 0,           'Server.apps.list.length was not initially empty');

        assert.equal(server.ports.httpPorts[0], "9080",		'Server.apps.ports.httpPorts did not have the correct initialized value');
        assert.equal(server.ports.httpsPorts[0], "9443",	'Server.apps.ports.httpsPorts did not have the correct initialized value');

        assert.notOk(server.isAdminCenterServer,           'The server should not be set to be an Admin Center server unless explicitly set in the constructor');
      }),

      tdd.test('handleChangeEvent - added apps', function() {
        var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', runtimeType: 'Liberty', apps: {up: 0, down: 0, unknown: 0, list: []}});

        // Simulate event
        server._handleChangeEvent({type: 'server', id: 'localhost,/wlp/usr,server1', apps: { up: 1, down: 2, unknown: 3, added: [{name: 'snoop', state: 'STARTED'}] }});

        assert.equal(server.id, 'localhost,/wlp/usr,server1', 'Server.id did not have the correct initialized value');
        assert.equal(server.name,             'server1',   'Server.name did not have the correct initialized value');
        assert.equal(server.userdir,          '/wlp/usr',  'Server.userdir did not have the correct initialized value');
        assert.equal(server.host,             'localhost', 'Server.host did not have the correct initialized value');
        assert.equal(server.wlpInstallDir,    '/wlp',      'Server.wlpInstallDir did not have the correct initialized value');
        assert.equal(server.state,            'STARTED',   'Server.state did not have the correct initialized value');
        assert.equal(server.cluster,          null,        'Server.cluster did not have the correct initialized value');
        assert.equal(server.scalingPolicy,    null,        'Server.scalingPolicy did not have the correct initialized value');
        assert.equal(server.runtimeType,      'Liberty',   'Server.runtimeType did not have the correct initialized type');
        assert.notOk(server.containerType,                 'Server.containerType was initialized when no value was set');

        assert.equal(server.apps.up,          1,           'Server.apps.up did not have the updated value');
        assert.equal(server.apps.down,        2,           'Server.apps.down did not have the updated value');
        assert.equal(server.apps.unknown,     3,           'Server.apps.unknown did not have the updated value');
        assert.ok(Array.isArray(server.apps.list),         'Server.apps.list was not an Array');
        assert.equal(server.apps.list.length, 1,           'Server.apps.list.length did not have the updated value');
        assert.equal(JSON.stringify(server.apps.list[0]), JSON.stringify({name:'snoop',state:'STARTED'}), 'Server.apps.list[0] did not have the updated value');

        assert.equal(server.ports.httpPorts[0], "9080",		'Server.apps.ports.httpPorts did not have the correct initialized value');
        assert.equal(server.ports.httpsPorts[0], "9443",	'Server.apps.ports.httpsPorts did not have the correct initialized value');

        assert.notOk(server.isAdminCenterServer,           'The server should not be set to be an Admin Center server unless explicitly set in the constructor');
      }),

      tdd.test('handleChangeEvent - changed apps', function() {
        var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', runtimeType: 'Liberty', apps: {up: 0, down: 0, unknown: 0, list: [{name:'snoop',state:'STARTED'}]}});

        // Simulate event
        server._handleChangeEvent({type: 'server', id: 'localhost,/wlp/usr,server1', apps: { up: 1, down: 2, unknown: 3, changed: [{name: 'snoop', state: 'STOPPED'}] }});

        assert.equal(server.id, 'localhost,/wlp/usr,server1', 'Server.id did not have the correct initialized value');
        assert.equal(server.name,             'server1',   'Server.name did not have the correct initialized value');
        assert.equal(server.userdir,          '/wlp/usr',  'Server.userdir did not have the correct initialized value');
        assert.equal(server.host,             'localhost', 'Server.host did not have the correct initialized value');
        assert.equal(server.wlpInstallDir,    '/wlp',      'Server.wlpInstallDir did not have the correct initialized value');
        assert.equal(server.state,            'STARTED',   'Server.state did not have the correct initialized value');
        assert.equal(server.cluster,          null,        'Server.cluster did not have the correct initialized value');
        assert.equal(server.scalingPolicy,    null,        'Server.scalingPolicy did not have the correct initialized value');
        assert.equal(server.runtimeType,      'Liberty',   'Server.runtimeType did not have the correct initialized type');
        assert.notOk(server.containerType,                 'Server.containerType was initialized when no value was set');

        assert.equal(server.apps.up,          1,           'Server.apps.up did not have the updated value');
        assert.equal(server.apps.down,        2,           'Server.apps.down did not have the updated value');
        assert.equal(server.apps.unknown,     3,           'Server.apps.unknown did not have the updated value');
        assert.ok(Array.isArray(server.apps.list),       'Server.apps.list was not an Array');
        assert.equal(server.apps.list.length, 1,           'Server.apps.list.length did not have the updated value');
        assert.equal(JSON.stringify(server.apps.list[0]), JSON.stringify({name:'snoop',state:'STOPPED'}), 'Server.apps.list[0] did not have the updated value');

        assert.equal(server.ports.httpPorts[0], "9080",		'Server.apps.ports.httpPorts did not have the correct initialized value');
        assert.equal(server.ports.httpsPorts[0], "9443",	'Server.apps.ports.httpsPorts did not have the correct initialized value');

        assert.notOk(server.isAdminCenterServer,           'The server should not be set to be an Admin Center server unless explicitly set in the constructor');
      }),

      tdd.test('handleChangeEvent - removed apps', function() {
        var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', runtimeType: 'Liberty', apps: {up: 0, down: 0, unknown: 0, list: [{name:'snoop',state:'STARTED'}]}});

        // Simulate event
        server._handleChangeEvent({type: 'server', id: 'localhost,/wlp/usr,server1', apps: { up: 1, down: 2, unknown: 3, removed: ['snoop'] } });

        assert.equal(server.id, 'localhost,/wlp/usr,server1', 'Server.id did not have the correct initialized value');
        assert.equal(server.name,             'server1',   'Server.name did not have the correct initialized value');
        assert.equal(server.userdir,          '/wlp/usr',  'Server.userdir did not have the correct initialized value');
        assert.equal(server.host,             'localhost', 'Server.host did not have the correct initialized value');
        assert.equal(server.wlpInstallDir,    '/wlp',      'Server.wlpInstallDir did not have the correct initialized value');
        assert.equal(server.state,            'STARTED',   'Server.state did not have the correct initialized value');
        assert.equal(server.cluster,          null,        'Server.cluster did not have the correct initialized value');
        assert.equal(server.scalingPolicy,    null,        'Server.scalingPolicy did not have the correct initialized value');
        assert.equal(server.runtimeType,      'Liberty',   'Server.runtimeType did not have the correct initialized type');
        assert.notOk(server.containerType,                 'Server.containerType was initialized when no value was set');

        assert.equal(server.apps.up,          1,           'Server.apps.up did not have the updated value');
        assert.equal(server.apps.down,        2,           'Server.apps.down did not have the updated value');
        assert.equal(server.apps.unknown,     3,           'Server.apps.unknown did not have the updated value');
        assert.ok(Array.isArray(server.apps.list),         'Server.apps.list was not an Array');
        assert.equal(server.apps.list.length, 0,           'Server.apps.list.length did not have the updated value');

        assert.equal(server.ports.httpPorts[0], "9080",		'Server.apps.ports.httpPorts did not have the correct initialized value');
        assert.equal(server.ports.httpsPorts[0], "9443",	'Server.apps.ports.httpsPorts did not have the correct initialized value');

        assert.notOk(server.isAdminCenterServer,           'The server should not be set to be an Admin Center server unless explicitly set in the constructor');
      }),


      tdd.test('handleChangeEvent - changed tags', function() {
        var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', runtimeType: 'Liberty', apps: {up: 0, down: 0, unknown: 0, list: [{name:'snoop',state:'STARTED',tags:['production']}]}});

        // Simulate event
        server._handleChangeEvent({type: 'server', id: 'localhost,/wlp/usr,server1', apps: { up: 1, down: 2, unknown: 3, changed: [{name: 'snoop', tags:['development']}] }});

        assert.equal(server.id, 'localhost,/wlp/usr,server1', 'Server.id did not have the correct initialized value');
        assert.equal(server.name,             'server1',   'Server.name did not have the correct initialized value');
        assert.equal(server.userdir,          '/wlp/usr',  'Server.userdir did not have the correct initialized value');
        assert.equal(server.host,             'localhost', 'Server.host did not have the correct initialized value');
        assert.equal(server.wlpInstallDir,    '/wlp',      'Server.wlpInstallDir did not have the correct initialized value');
        assert.equal(server.state,            'STARTED',   'Server.state did not have the correct initialized value');
        assert.equal(server.cluster,          null,        'Server.cluster did not have the correct initialized value');
        assert.equal(server.scalingPolicy,    null,        'Server.scalingPolicy did not have the correct initialized value');
        assert.equal(server.runtimeType,      'Liberty',   'Server.runtimeType did not have the correct initialized type');
        assert.notOk(server.containerType,                 'Server.containerType was initialized when no value was set');

        assert.equal(server.apps.up,          1,           'Server.apps.up did not have the updated value');
        assert.equal(server.apps.down,        2,           'Server.apps.down did not have the updated value');
        assert.equal(server.apps.unknown,     3,           'Server.apps.unknown did not have the updated value');
        assert.ok(Array.isArray(server.apps.list),         'Server.apps.list was not an Array');
        assert.equal(server.apps.list.length, 1,           'Server.apps.list.length did not have the updated value');
        assert.equal(JSON.stringify(server.apps.list[0]), JSON.stringify({name:'snoop',state:'STARTED', tags:['development']}), 'Server.apps.list[0] did not have the updated value');

        assert.equal(server.ports.httpPorts[0], "9080",		'Server.apps.ports.httpPorts did not have the correct initialized value');
        assert.equal(server.ports.httpsPorts[0], "9443",	'Server.apps.ports.httpsPorts did not have the correct initialized value');

        assert.notOk(server.isAdminCenterServer,           'The server should not be set to be an Admin Center server unless explicitly set in the constructor');
      }),

      tdd.test('handleChangeEvent - changed owner', function() {
        var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', runtimeType: 'Liberty', apps: {up: 0, down: 0, unknown: 0, list: [{name:'snoop',state:'STARTED',tags:['production'], owner:'Bob'}]}});

        // Simulate event
        server._handleChangeEvent({type: 'server', id: 'localhost,/wlp/usr,server1', apps: { up: 1, down: 2, unknown: 3, changed: [{name: 'snoop', tags:['production'], owner:'Bill'}] }});

        assert.equal(server.id, 'localhost,/wlp/usr,server1', 'Server.id did not have the correct initialized value');
        assert.equal(server.name,             'server1',   'Server.name did not have the correct initialized value');
        assert.equal(server.userdir,          '/wlp/usr',  'Server.userdir did not have the correct initialized value');
        assert.equal(server.host,             'localhost', 'Server.host did not have the correct initialized value');
        assert.equal(server.wlpInstallDir,    '/wlp',      'Server.wlpInstallDir did not have the correct initialized value');
        assert.equal(server.state,            'STARTED',   'Server.state did not have the correct initialized value');
        assert.equal(server.cluster,          null,        'Server.cluster did not have the correct initialized value');
        assert.equal(server.scalingPolicy,    null,        'Server.scalingPolicy did not have the correct initialized value');
        assert.equal(server.runtimeType,      'Liberty',   'Server.runtimeType did not have the correct initialized type');
        assert.notOk(server.containerType,                 'Server.containerType was initialized when no value was set');

        assert.equal(server.apps.up,          1,           'Server.apps.up did not have the updated value');
        assert.equal(server.apps.down,        2,           'Server.apps.down did not have the updated value');
        assert.equal(server.apps.unknown,     3,           'Server.apps.unknown did not have the updated value');
        assert.ok(Array.isArray(server.apps.list),         'Server.apps.list was not an Array');
        assert.equal(server.apps.list.length, 1,           'Server.apps.list.length did not have the updated value');
        assert.equal(JSON.stringify(server.apps.list[0]), JSON.stringify({name:'snoop',state:'STARTED', tags:['production'], owner: 'Bill'}), 'Server.apps.list[0] did not have the updated value');

        assert.equal(server.ports.httpPorts[0], "9080",		'Server.apps.ports.httpPorts did not have the correct initialized value');
        assert.equal(server.ports.httpsPorts[0], "9443",	'Server.apps.ports.httpsPorts did not have the correct initialized value');

        assert.notOk(server.isAdminCenterServer,           'The server should not be set to be an Admin Center server unless explicitly set in the constructor');
      }),

      tdd.test('handleChangeEvent - changed contacts', function() {
        var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', runtimeType: 'Liberty', apps: {up: 0, down: 0, unknown: 0, list: [{name:'snoop',state:'STARTED',tags:['production'], owner:'Bob', contacts: ['Peter', 'Paul']}]}});

        // Simulate event
        server._handleChangeEvent({type: 'server', id: 'localhost,/wlp/usr,server1', apps: { up: 1, down: 2, unknown: 3, changed: [{name: 'snoop', tags:['production'], owner:'Bob', contacts: ['Mary']}] }});

        assert.equal(server.id, 'localhost,/wlp/usr,server1', 'Server.id did not have the correct initialized value');
        assert.equal(server.name,             'server1',   'Server.name did not have the correct initialized value');
        assert.equal(server.userdir,          '/wlp/usr',  'Server.userdir did not have the correct initialized value');
        assert.equal(server.host,             'localhost', 'Server.host did not have the correct initialized value');
        assert.equal(server.wlpInstallDir,    '/wlp',      'Server.wlpInstallDir did not have the correct initialized value');
        assert.equal(server.state,            'STARTED',   'Server.state did not have the correct initialized value');
        assert.equal(server.cluster,          null,        'Server.cluster did not have the correct initialized value');
        assert.equal(server.scalingPolicy,    null,        'Server.scalingPolicy did not have the correct initialized value');
        assert.equal(server.runtimeType,      'Liberty',   'Server.runtimeType did not have the correct initialized type');
        assert.notOk(server.containerType,                 'Server.containerType was initialized when no value was set');

        assert.equal(server.apps.up,          1,           'Server.apps.up did not have the updated value');
        assert.equal(server.apps.down,        2,           'Server.apps.down did not have the updated value');
        assert.equal(server.apps.unknown,     3,           'Server.apps.unknown did not have the updated value');
        assert.ok(Array.isArray(server.apps.list),         'Server.apps.list was not an Array');
        assert.equal(server.apps.list.length, 1,           'Server.apps.list.length did not have the updated value');
        assert.equal(JSON.stringify(server.apps.list[0]), JSON.stringify({name:'snoop',state:'STARTED', tags:['production'], owner:'Bob', contacts: ['Mary']}), 'Server.apps.list[0] did not have the updated value');

        assert.equal(server.ports.httpPorts[0], "9080",		'Server.apps.ports.httpPorts did not have the correct initialized value');
        assert.equal(server.ports.httpsPorts[0], "9443",	'Server.apps.ports.httpsPorts did not have the correct initialized value');

        assert.notOk(server.isAdminCenterServer,           'The server should not be set to be an Admin Center server unless explicitly set in the constructor');
      }),

      tdd.test('handleChangeEvent - changed note', function() {
        var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', runtimeType: 'Liberty', apps: {up: 0, down: 0, unknown: 0, list: [{name:'snoop',state:'STARTED',tags:['production'], owner:'Bob', contacts: ['Peter', 'Paul'], note:'note for unittest only'}]}});

        // Simulate event
        server._handleChangeEvent({type: 'server', id: 'localhost,/wlp/usr,server1', apps: { up: 1, down: 2, unknown: 3, changed: [{name:'snoop',state:'STARTED',tags:['production'], owner:'Bob', contacts: ['Peter', 'Paul'], note:'note for FAT'}] }});

        assert.equal(server.id, 'localhost,/wlp/usr,server1', 'Server.id did not have the correct initialized value');
        assert.equal(server.name,             'server1',   'Server.name did not have the correct initialized value');
        assert.equal(server.userdir,          '/wlp/usr',  'Server.userdir did not have the correct initialized value');
        assert.equal(server.host,             'localhost', 'Server.host did not have the correct initialized value');
        assert.equal(server.wlpInstallDir,    '/wlp',      'Server.wlpInstallDir did not have the correct initialized value');
        assert.equal(server.state,            'STARTED',   'Server.state did not have the correct initialized value');
        assert.equal(server.cluster,          null,        'Server.cluster did not have the correct initialized value');
        assert.equal(server.scalingPolicy,    null,        'Server.scalingPolicy did not have the correct initialized value');
        assert.equal(server.runtimeType,      'Liberty',   'Server.runtimeType did not have the correct initialized type');
        assert.notOk(server.containerType,                 'Server.containerType was initialized when no value was set');

        assert.equal(server.apps.up,          1,           'Server.apps.up did not have the updated value');
        assert.equal(server.apps.down,        2,           'Server.apps.down did not have the updated value');
        assert.equal(server.apps.unknown,     3,           'Server.apps.unknown did not have the updated value');
        assert.ok(Array.isArray(server.apps.list),       'Server.apps.list was not an Array');
        assert.equal(server.apps.list.length, 1,           'Server.apps.list.length did not have the updated value');
        assert.equal(JSON.stringify(server.apps.list[0]), JSON.stringify({name:'snoop',state:'STARTED',tags:['production'], owner:'Bob', contacts: ['Peter', 'Paul'],  note:'note for FAT'}), 'Server.apps.list[0] did not have the updated value');

        assert.equal(server.ports.httpPorts[0], "9080",		'Server.apps.ports.httpPorts did not have the correct initialized value');
        assert.equal(server.ports.httpsPorts[0], "9443",	'Server.apps.ports.httpsPorts did not have the correct initialized value');

        assert.notOk(server.isAdminCenterServer,           'The server should not be set to be an Admin Center server unless explicitly set in the constructor');
      }),
      
      tdd.test('handleChangeEvent - clear app metadata', function() {
        var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', runtimeType: 'Liberty', apps: {up: 0, down: 0, unknown: 0, list: [{name:'snoop',state:'STARTED',tags:['production'], owner:'Bob', contacts: ['Peter', 'Paul'], note:'note for unittest only'}]}});

        // Simulate event
        server._handleChangeEvent({type: 'server', id: 'localhost,/wlp/usr,server1', apps: { up: 1, down: 2, unknown: 3, changed: [{name:'snoop',state:'STARTED', tags:null, owner:null, contacts: null, note:null}] }});

        assert.equal(server.id, 'localhost,/wlp/usr,server1', 'Server.id did not have the correct initialized value');
        assert.equal(server.name,             'server1',   'Server.name did not have the correct initialized value');
        assert.equal(server.userdir,          '/wlp/usr',  'Server.userdir did not have the correct initialized value');
        assert.equal(server.host,             'localhost', 'Server.host did not have the correct initialized value');
        assert.equal(server.wlpInstallDir,    '/wlp',      'Server.wlpInstallDir did not have the correct initialized value');
        assert.equal(server.state,            'STARTED',   'Server.state did not have the correct initialized value');
        assert.equal(server.cluster,          null,        'Server.cluster did not have the correct initialized value');
        assert.equal(server.scalingPolicy,    null,        'Server.scalingPolicy did not have the correct initialized value');
        assert.equal(server.runtimeType,      'Liberty',   'Server.runtimeType did not have the correct initialized type');
        assert.notOk(server.containerType,                 'Server.containerType was initialized when no value was set');

        assert.equal(server.apps.up,          1,           'Server.apps.up did not have the updated value');
        assert.equal(server.apps.down,        2,           'Server.apps.down did not have the updated value');
        assert.equal(server.apps.unknown,     3,           'Server.apps.unknown did not have the updated value');
        assert.ok(Array.isArray(server.apps.list),       'Server.apps.list was not an Array');
        assert.equal(server.apps.list.length, 1,           'Server.apps.list.length did not have the updated value');
        assert.equal(JSON.stringify(server.apps.list[0]), JSON.stringify({name:'snoop', state:'STARTED', tags:null, owner: null, contacts: null,  note: null}), 'Server.apps.list[0] did not have the updated value');
        
        assert.equal(server.ports.httpPorts[0], "9080",		'Server.apps.ports.httpPorts did not have the correct initialized value');
        assert.equal(server.ports.httpsPorts[0], "9443",	'Server.apps.ports.httpsPorts did not have the correct initialized value');

        assert.notOk(server.isAdminCenterServer,           'The server should not be set to be an Admin Center server unless explicitly set in the constructor');
      }),

      tdd.test('handleChangeEvent - changed alerts', function() {
        var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', runtimeType: 'Liberty', apps: {up: 0, down: 0, unknown: 0, list: []}});

        // Check pre-event conditions
        assert.notOk(server.alerts,                        'Server.alerts was not falsy. It should have no default value');

        // Simulate event
        var alerts = { count: 1, unknown: [{id: 'application', type: 'snoop'}] };
        server._handleChangeEvent({type: 'server', id: 'localhost,/wlp/usr,server1', state: 'UNKNOWN', alerts: alerts });

        assert.equal(server.id, 'localhost,/wlp/usr,server1', 'Server.id did not have the correct initialized value');
        assert.equal(server.name,             'server1',   'Server.name did not have the correct initialized value');
        assert.equal(server.userdir,          '/wlp/usr',  'Server.userdir did not have the correct initialized value');
        assert.equal(server.host,             'localhost', 'Server.host did not have the correct initialized value');
        assert.equal(server.wlpInstallDir,    '/wlp',      'Server.wlpInstallDir did not have the correct initialized value');
        assert.equal(server.state,            'UNKNOWN',   'Server.state did not update based on the event value');
        assert.equal(server.cluster,          null,        'Server.cluster did not have the correct initialized value');
        assert.equal(server.scalingPolicy,    null,        'Server.scalingPolicy did not have the correct initialized value');
        assert.equal(server.runtimeType,      'Liberty',   'Server.runtimeType did not have the correct initialized type');
        assert.notOk(server.containerType,                 'Server.containerType was initialized when no value was set');

        assert.equal(server.apps.up,          0,           'Server.apps.up did not have the correct initialized value');
        assert.equal(server.apps.down,        0,           'Server.apps.down did not have the correct initialized value');
        assert.equal(server.apps.unknown,     0,           'Server.apps.unknown did not have the correct initialized value');
        assert.ok(Array.isArray(server.apps.list),         'Server.apps.list was not an Array');
        assert.equal(server.apps.list.length, 0,           'Server.apps.list.length was not initially empty');
        
        assert.equal(server.ports.httpPorts[0], "9080",		'Server.apps.ports.httpPorts did not have the correct initialized value');
        assert.equal(server.ports.httpsPorts[0], "9443",	'Server.apps.ports.httpsPorts did not have the correct initialized value');

        assert.notOk(server.isAdminCenterServer,           'The server should not be set to be an Admin Center server unless explicitly set in the constructor');

        assert.equal(server.alerts,           alerts,      'Server.alerts did not update based on the event value');
      }),
      
      tdd.test('handleChangeEvent - changed ports', function() {
    	  var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', apps: {up: 0, down: 0, unknown: 0, list: []}, ports: {httpPorts:["9080"], httpsPorts:["9443"]}});
    	  
    	  // Simulate event
    	  server._handleChangeEvent({type: 'server', id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', apps: {up: 0, down: 0, unknown: 0, list: []}, ports: {httpPorts:["9081"], httpsPorts:["9444"]}});
    	  
    	  assert.equal(server.id, 'localhost,/wlp/usr,server1', 'Server.id did not have the correct initialized value');
          assert.equal(server.name,             'server1',   'Server.name did not have the correct initialized value');
          assert.equal(server.userdir,          '/wlp/usr',  'Server.userdir did not have the correct initialized value');
          assert.equal(server.host,             'localhost', 'Server.host did not have the correct initialized value');
          assert.equal(server.wlpInstallDir,    '/wlp',      'Server.wlpInstallDir did not have the correct initialized value');
          assert.equal(server.state,            'STARTED',   'Server.state did not update based on the event value');
          assert.equal(server.cluster,          null,        'Server.cluster did not have the correct initialized value');
          assert.equal(server.scalingPolicy,    null,        'Server.scalingPolicy did not have the correct initialized value');
          assert.equal(server.runtimeType,      'Liberty',   'Server.runtimeType did not have the correct initialized type');
          assert.notOk(server.containerType,                 'Server.containerType was initialized when no value was set');

          assert.equal(server.apps.up,          0,           'Server.apps.up did not have the correct initialized value');
          assert.equal(server.apps.down,        0,           'Server.apps.down did not have the correct initialized value');
          assert.equal(server.apps.unknown,     0,           'Server.apps.unknown did not have the correct initialized value');
          assert.ok(Array.isArray(server.apps.list),         'Server.apps.list was not an Array');
          assert.equal(server.apps.list.length, 0,           'Server.apps.list.length was not initially empty');

          assert.equal(server.ports.httpPorts[0], "9081", 	 'Server.ports.httpPorts did not update based on the event value');
          assert.equal(server.ports.httpsPorts[0],"9444", 	 'Server.ports.httpsPorts did not update based on the event value');
          
          assert.notOk(server.isAdminCenterServer,           'The server should not be set to be an Admin Center server unless explicitly set in the constructor');
      }),

      tdd.test('handleChangeEvent - multiple changes with notification', function() {
        var observer = new ServerObserver();
        var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', runtimeType: 'Liberty', apps: {up: 0, down: 0, unknown: 0, list: [{name:'snoop',state:'STARTED'}]}});

        server.subscribe(observer);

        assert.notOk(server.isCollectiveController,        'Server.isCollectiveController did not have the correct initialized value');

        // Simulate event
        var alerts = { count: 1, unknown: [{id: 'application', type: 'snoop'}] };
        server._handleChangeEvent({type: 'server', id: 'localhost,/wlp/usr,server1',
          wlpInstallDir: '/wlp2', state: 'STOPPED', cluster: 'cluster1', scalingPolicy: 'newPolicy', scalingPolicyEnabled: true,
          isCollectiveController: true,
          apps: { up: 1, down: 2, unknown: 3, added: [{name: 'testApp', state: 'STOPPED'}], changed: [], removed: ['snoop'] },
          alerts: alerts });

        assert.equal(server.id, 'localhost,/wlp/usr,server1', 'Server.id did not have the correct initialized value');
        assert.equal(server.name,             'server1',   'Server.name did not have the correct initialized value');
        assert.equal(server.userdir,          '/wlp/usr',  'Server.userdir did not have the correct initialized value');
        assert.equal(server.host,             'localhost', 'Server.host did not have the correct initialized value');
        assert.equal(server.wlpInstallDir,    '/wlp2',     'Server.wlpInstallDir did not have the updated value');
        assert.equal(server.state,            'STOPPED',   'Server.state did not have the updated value');
        assert.equal(server.cluster,          'cluster1',  'Server.cluster did not have the updated value');
        assert.equal(server.scalingPolicy,    'newPolicy', 'Server.scalingPolicy did not have the updated value');
        assert.isTrue(server.scalingPolicyEnabled,         'Server.scalingPolicyEnabled did not have the updated value');
        assert.isTrue(server.isCollectiveController,       'Server.isCollectiveController did not have the correct updated value');
        assert.equal(server.runtimeType,      'Liberty',   'Server.runtimeType did not have the correct initialized type');
        assert.notOk(server.containerType,                 'Server.containerType was initialized when no value was set');

        assert.equal(server.apps.up,          1,           'Server.apps.up did not have the updated value');
        assert.equal(server.apps.down,        2,           'Server.apps.down did not have the updated value');
        assert.equal(server.apps.unknown,     3,           'Server.apps.unknown did not have the updated value');
        assert.ok(Array.isArray(server.apps.list),       'Server.apps.list was not an Array');
        assert.equal(server.apps.list.length, 1,           'Server.apps.list.length did not have the updated value');
        assert.equal(JSON.stringify(server.apps.list[0]), JSON.stringify({name: 'testApp', state: 'STOPPED'}),   'Server.apps.list[0] did not have the updated value');

        assert.notOk(server.isAdminCenterServer,           'The server should not be set to be an Admin Center server unless explicitly set in the constructor');

        assert.equal(server.alerts,           alerts,      'Server.alerts did not update based on the event value');

        // Validate the Observer was passed the correct tally objects after the second event
        assert.equal(observer.newState,         'STOPPED',   'ServerObserver did not get the correct NEW value for state');
        assert.equal(observer.oldState,         'STARTED',   'ServerObserver did not get the correct OLD value for state');
        assert.equal(observer.newWlpInstallDir, '/wlp2',     'ServerObserver did not get the correct NEW value for wlpInstallDir');
        assert.equal(observer.oldWlpInstallDir, '/wlp',      'ServerObserver did not get the correct OLD value for wlpInstallDir');
        assert.equal(observer.newCluster,       'cluster1',  'ServerObserver did not get the correct OLD value for cluster');
        assert.equal(observer.oldCluster,       null,        'ServerObserver did not get the correct NEW value for cluster');
        assert.equal(observer.newScalingPolicy, 'newPolicy', 'ServerObserver did not get the correct OLD value for scalingPolicy');
        assert.equal(observer.oldScalingPolicy, null,        'ServerObserver did not get the correct NEW value for scalingPolicy');
        assert.isTrue(observer.newScalingPolicyEnabled,      'ServerObserver did not get the correct OLD value for scalingPolicyEnabled');
        assert.notOk(observer.oldScalingPolicyEnabled,       'ServerObserver did not get the correct NEW value for scalingPolicyEnabled');
        assert.isTrue(observer.newIsCollectiveController,    'ServerObserver did not get the correct OLD value for isCollectiveController');
        assert.notOk(observer.oldIsCollectiveController,     'ServerObserver did not get the correct NEW value for isCollectiveController');

        assert.ok(observer.newTally,                         'ServerObserver did not get the newTally object set');
        assert.equal(observer.newTally.up,      1,           'ServerObserver did not get the correct new value for the up tally');
        assert.equal(observer.newTally.down,    2,           'ServerObserver did not get the correct new value for the down tally');
        assert.equal(observer.newTally.unknown, 3,           'ServerObserver did not get the correct new value for the unknown tally');

        assert.ok(observer.oldTally,                         'ServerObserver did not get the oldTally object set');
        assert.equal(observer.oldTally.up,      0,           'ServerObserver did not get the correct old value for the up tally');
        assert.equal(observer.oldTally.down,    0,           'ServerObserver did not get the correct old value for the down tally');
        assert.equal(observer.oldTally.unknown, 0,           'ServerObserver did not get the correct old value for the unknown tally');

        assert.ok(observer.newList,                        'ServerObserver.newList did not get set, when it should have been');
        assert.equal(observer.newList.length,   1,           'ServerObserver.newList was not of expected size');
        assert.equal(JSON.stringify(observer.newList[0]), JSON.stringify({name: 'testApp', state: 'STOPPED'}), 'ServerObserver.newList[0] was not of expected value');

        assert.ok(observer.oldList,                        'ServerObserver.oldList did not get set, when it should have been');
        assert.equal(observer.oldList.length,   1,           'ServerObserver.oldList was not empty');
        assert.equal(JSON.stringify(observer.oldList[0]), JSON.stringify({name:'snoop',state:'STARTED'}), 'ServerObserver.oldList[0] was not of expected value');

        assert.ok(observer.added,                          'ServerObserver.added did not get set, when it should have been');
        assert.equal(observer.added.length,     1,           'ServerObserver.added was not of expected size');
        assert.equal(JSON.stringify(observer.added[0]), JSON.stringify({name: 'testApp', state: 'STOPPED'}), 'ServerObserver.added[0] was not of expected value');

        assert.ok(observer.removed,                        'ServerObserver.removed did not get set, when it should have been');
        assert.equal(observer.removed.length,   1,           'ServerObserver.removed was not empty');
        assert.equal(observer.removed[0],       'snoop',     'ServerObserver.removed[0] was not of expected value');

        assert.ok(observer.changed,                        'ServerObserver.changed did not get set, when it should have been');
        assert.equal(observer.changed.length,             0, 'ServerObserver.changed was not empty');

        assert.equal(observer.newAlerts,        alerts,      'ServerObserver did not get the correct NEW value for alerts');
        assert.notOk(observer.oldAlerts,                     'ServerObserver did not get the correct OLD value for alerts');
      }),

      tdd.test('handleChangeEvent - server was removed should trigger onDestroyed', function() {
        var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', runtimeType: 'Liberty', apps: {up: 0, down: 0, unknown: 0, list: []}});
        var observer = new ServerObserver();

        server.subscribe(observer);

        // Simulate removal event
        server._handleChangeEvent({id: 'localhost,/wlp/usr,server1', type: 'server', state: 'removed'});

        assert.equal(server.state, 'STARTED',   'Server.state did not have the correct initialized value. It should not change in response to a "removed" event state');
        assert.ok(server.isDestroyed,         'Server.isDestroyed flag did not get set in response to a "removed" event');

        assert.ok(observer.isDestroyed,       'ServerObserver did not get the isDestroyed flag set');
      }),

      tdd.test('handleChangeEvent - unset or wrong event id or type is ignored', function() {
        var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', runtimeType: 'Liberty', apps: {up: 0, down: 0, unknown: 0, list: []}});

        // Simulate ignored events
        server._handleChangeEvent({type: 'server', state: 'STOPPED'});
        server._handleChangeEvent({id: 'wrongserver', type: 'server', state: 'STOPPED'});
        server._handleChangeEvent({id: 'server1', state: 'STOPPED'});
        server._handleChangeEvent({id: 'server1', type: 'server', state: 'STOPPED'});

        assert.equal(server.id, 'localhost,/wlp/usr,server1', 'Server.id did not have the correct initialized value');
        assert.equal(server.name,             'server1',   'Server.name did not have the correct initialized value');
        assert.equal(server.userdir,          '/wlp/usr',  'Server.userdir did not have the correct initialized value');
        assert.equal(server.host,             'localhost', 'Server.host did not have the correct initialized value');
        assert.equal(server.wlpInstallDir,    '/wlp',      'Server.wlpInstallDir did not have the correct initialized value');
        assert.equal(server.state,            'STARTED',   'Server.state did not have the correct initialized value');
        assert.equal(server.cluster,          null,        'Server.cluster did not have the correct initialized value');
        assert.equal(server.scalingPolicy,    null,        'Server.scalingPolicy did not have the correct initialized value');
        assert.equal(server.runtimeType,      'Liberty',   'Server.runtimeType did not have the correct initialized type');
        assert.notOk(server.containerType,                 'Server.containerType was initialized when no value was set');

        assert.equal(server.apps.up,          0,           'Server.apps.up did not have the correct initialized value');
        assert.equal(server.apps.down,        0,           'Server.apps.down did not have the correct initialized value');
        assert.equal(server.apps.unknown,     0,           'Server.apps.unknown did not have the correct initialized value');
        assert.ok(Array.isArray(server.apps.list),         'Server.apps.list was not an Array');
        assert.equal(server.apps.list.length, 0,           'Server.apps.list.length was not initially empty');

        assert.notOk(server.isAdminCenterServer,           'The server should not be set to be an Admin Center server unless explicitly set in the constructor');
      }),
      
      tdd.test('constructor - Node.js server with empty cluster, scalingPolicy, and apps', function() {
        var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', runtimeType: 'Node.js', apps: {up: 0, down: 0, unknown: 0, list: []}});

        assert.equal(server.id, 'localhost,/wlp/usr,server1', 'Server.id did not have the correct initialized value');
        assert.equal(server.name,             'server1',   'Server.name did not have the correct initialized value');
        assert.equal(server.userdir,          '/wlp/usr',  'Server.userdir did not have the correct initialized value');
        assert.equal(server.host,             'localhost', 'Server.host did not have the correct initialized value');
        assert.equal(server.wlpInstallDir,    '/wlp',      'Server.wlpInstallDir did not have the correct initialized value');
        assert.equal(server.state,            'STARTED',   'Server.state did not have the correct initialized value');
        assert.equal(server.cluster,          null,        'Server.cluster did not have the correct initialized value');
        assert.equal(server.scalingPolicy,    null,        'Server.scalingPolicy did not have the correct initialized value');
        assert.notOk(server.scalingPolicyEnabled,          'Server.scalingPolicyEnabled did not have the correct initialized value');
        assert.notOk(server.isCollectiveController,        'Server.isCollectiveController did not have the correct initialized value');
        assert.equal(server.runtimeType,      'Node.js',   'Server.runtimeType did not have the correct initialized type');
        assert.notOk(server.containerType,                 'Server.containerType was initialized when no value was set');

        assert.equal(server.apps.up,          0,           'Server.apps.up did not have the correct initialized value');
        assert.equal(server.apps.down,        0,           'Server.apps.down did not have the correct initialized value');
        assert.equal(server.apps.unknown,     0,           'Server.apps.unknown did not have the correct initialized value');
        assert.ok(Array.isArray(server.apps.list),         'Server.apps.list was not an Array');
        assert.equal(server.apps.list.length, 0,           'Server.apps.list.length was not initially empty');

        assert.notOk(server.isAdminCenterServer,           'The server should not be set to be an Admin Center server unless explicitly set in the constructor');
      }),

      tdd.test('constructor - Liberty server in docker container, empty cluster, scalingPolicy, and apps', function() {
        var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', runtimeType: 'Liberty', containerType: 'Docker', apps: {up: 0, down: 0, unknown: 0, list: []}});

        assert.equal(server.id, 'localhost,/wlp/usr,server1', 'Server.id did not have the correct initialized value');
        assert.equal(server.name,             'server1',   'Server.name did not have the correct initialized value');
        assert.equal(server.userdir,          '/wlp/usr',  'Server.userdir did not have the correct initialized value');
        assert.equal(server.host,             'localhost', 'Server.host did not have the correct initialized value');
        assert.equal(server.wlpInstallDir,    '/wlp',      'Server.wlpInstallDir did not have the correct initialized value');
        assert.equal(server.state,            'STARTED',   'Server.state did not have the correct initialized value');
        assert.equal(server.cluster,          null,        'Server.cluster did not have the correct initialized value');
        assert.equal(server.scalingPolicy,    null,        'Server.scalingPolicy did not have the correct initialized value');
        assert.notOk(server.scalingPolicyEnabled,          'Server.scalingPolicyEnabled did not have the correct initialized value');
        assert.notOk(server.isCollectiveController,        'Server.isCollectiveController did not have the correct initialized value');
        assert.equal(server.runtimeType,      'Liberty',   'Server.runtimeType did not have the correct initialized type');
        assert.equal(server.containerType,    'Docker',    'Server.containerType did not have the correct initialized type');

        assert.equal(server.apps.up,          0,           'Server.apps.up did not have the correct initialized value');
        assert.equal(server.apps.down,        0,           'Server.apps.down did not have the correct initialized value');
        assert.equal(server.apps.unknown,     0,           'Server.apps.unknown did not have the correct initialized value');
        assert.ok(Array.isArray(server.apps.list),         'Server.apps.list was not an Array');
        assert.equal(server.apps.list.length, 0,           'Server.apps.list.length was not initially empty');

        assert.notOk(server.isAdminCenterServer,           'The server should not be set to be an Admin Center server unless explicitly set in the constructor');
      });
      
      tdd.test('constructor - Server with apiDiscovery feature enabled', function() {
        var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', runtimeType: 'Liberty', explorerURL: "https://localhost:9443/ibm/api/explorer", apps: {up: 0, down: 0, unknown: 0, list: []}});

        assert.equal(server.id, 'localhost,/wlp/usr,server1', 'Server.id did not have the correct initialized value');
        assert.equal(server.name,             'server1',   'Server.name did not have the correct initialized value');
        assert.equal(server.userdir,          '/wlp/usr',  'Server.userdir did not have the correct initialized value');
        assert.equal(server.host,             'localhost', 'Server.host did not have the correct initialized value');
        assert.equal(server.wlpInstallDir,    '/wlp',      'Server.wlpInstallDir did not have the correct initialized value');
        assert.equal(server.state,            'STARTED',   'Server.state did not have the correct initialized value');
        assert.equal(server.cluster,          null,        'Server.cluster did not have the correct initialized value');
        assert.equal(server.scalingPolicy,    null,        'Server.scalingPolicy did not have the correct initialized value');
        assert.notOk(server.scalingPolicyEnabled,          'Server.scalingPolicyEnabled did not have the correct initialized value');
        assert.notOk(server.isCollectiveController,        'Server.isCollectiveController did not have the correct initialized value');
        assert.equal(server.explorerURL,      'https://localhost:9443/ibm/api/explorer',  'Server.explorerURL did not have the correct initialized value');
        assert.equal(server.runtimeType,      'Liberty',   'Server.runtimeType did not have the correct initialized type');
        assert.notOk(server.containerType,                 'Server.containerType was initialized when no value was set');

        assert.equal(server.apps.up,          0,           'Server.apps.up did not have the correct initialized value');
        assert.equal(server.apps.down,        0,           'Server.apps.down did not have the correct initialized value');
        assert.equal(server.apps.unknown,     0,           'Server.apps.unknown did not have the correct initialized value');
        assert.ok(Array.isArray(server.apps.list),         'Server.apps.list was not an Array');
        assert.equal(server.apps.list.length, 0,           'Server.apps.list.length was not initially empty');

        assert.notOk(server.isAdminCenterServer,           'The server should not be set to be an Admin Center server unless explicitly set in the constructor');
      });

    });
  }
});
