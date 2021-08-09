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
/**
 * Test cases for StandaloneServer
 */
define([
     "intern!tdd",
     "intern/chai!assert",
     "intern/dojo/declare",
     "dojo/Deferred",
     "resources/_objects/StandaloneServer",
     "resources/Observer",
     "dojo/i18n!resources/nls/resourcesMessages"
       ],
       
   function (tdd, assert, declare, Deferred, StandaloneServer, Observer, i18n) {
      var StandaloneServerObserver = declare([Observer], {
        id: 'testObserver',
    
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
        }
      });
      
      with(assert) {
        
        tdd.suite("StandaloneServer Object Tests", function() {
 
          var mockServer;
          
          tdd.beforeEach(function() {
            mockServer = sinon.fakeServer.create();
          });
          
          tdd.afterEach(function() {
            mockServer.restore();
          });
     
          tdd.test('constructor - no initialization object', function() {
              try {
                new StandaloneServer();
                assert.ok(false, 'Server was successfully created when it should have failed - an initialization object is required');
              } catch(error) {
                assert.equal(error, 'StatelessResource created without an initialization object', 'Error reported did not match expected error');
              }
          });
            
          tdd.test('constructor - no name', function() {
            try {
              new StandaloneServer({userdir: '/wlp/usr', host: 'localhost', apps: {up: 0, down: 0, unknown: 0, list: []}});
              assert.ok(false, 'StandaloneServer was successfully created when it should have failed - a name is required');
            } catch(error) {
              assert.equal(error, 'StandaloneServer created without a name', 'Error reported did not match expected error');
            }
          });
          
          tdd.test('constructor - no userdir', function() {
            try {
              new StandaloneServer({name: 'server1', host: 'localhost', apps: {up: 0, down: 0, unknown: 0, list: []}});
              assert.ok(false, 'StandaloneServer was successfully created when it should have failed - a userdir is required');
            } catch(error) {
              assert.equal(error, 'StandaloneServer created without a userdir',  'Error reported did not match expected error');
            }
          });
          
          tdd.test('constructor - no host', function() {
            try {
              new StandaloneServer({name: 'server1', userdir: '/wlp/usr', apps: {up: 0, down: 0, unknown: 0, list: []}});
              assert.ok(false, 'StandaloneServer was successfully created when it should have failed - a host is required');
            } catch(error) {
              assert.equal(error, 'StandaloneServer created without a host', 'Error reported did not match expected error');
            }
          });

          tdd.test('constructor - no initial apps object', function() {
            try {
              new StandaloneServer({name: 'server1', userdir: '/wlp/usr', host: 'localhost'});
              assert.ok(false, 'StandaloneServer was successfully created when it should have failed - an initial apps object is required');
            } catch(error) {
              assert.equal(error, 'StandaloneServer created without an initial apps object', 'Error reported did not match expected error');
            }
          });

          tdd.test('constructor - no initial apps.up', function() {
            try {
              new StandaloneServer({name: 'server1', userdir: '/wlp/usr', host: 'localhost', apps: {down: 0, unknown: 0, list: []}});
              assert.ok(false, 'StandaloneServer was successfully created when it should have failed - an initial apps.up tally is required');
            } catch(error) {
              assert.equal(error, 'StandaloneServer created without an initial apps.up tally', 'Error reported did not match expected error');
            }
          });

          tdd.test('constructor - no initial apps.down', function() {
            try {
              new StandaloneServer({name: 'server1', userdir: '/wlp/usr', host: 'localhost', apps: {up: 0, unknown: 0, list: []}});
              assert.ok(false, 'StandaloneServer was successfully created when it should have failed - an initial apps.down tally is required');
            } catch(error) {
              assert.equal(error, 'StandaloneServer created without an initial apps.down tally', 'Error reported did not match expected error');
            }
          });

          tdd.test('constructor - no initial apps.unknown', function() {
            try {
              new StandaloneServer({name: 'server1', userdir: '/wlp/usr', host: 'localhost', apps: {up: 0, down: 0, list: []}});
              assert.ok(false, 'StandaloneServer was successfully created when it should have failed - an initial apps.unknown tally is required');
            } catch(error) {
              assert.equal(error, 'StandaloneServer created without an initial apps.unknown tally', 'Error reported did not match expected error');
            }
          });

          tdd.test('constructor - no initial apps.list', function() {
            try {
              new StandaloneServer({name: 'server1', userdir: '/wlp/usr', host: 'localhost', apps: {up: 0, down: 0, unknown: 0}});
              assert.ok(false, 'StandaloneServer was successfully created when it should have failed - an initial apps.list array is required');
            } catch(error) {
              assert.equal(error, 'StandaloneServer created without an initial apps.list array', 'Error reported did not match expected error');
            }
          });

          tdd.test('constructor - initial apps.list has wrong element object', function() {
            try {
              new StandaloneServer({name: 'server1', userdir: '/wlp/usr', host: 'localhost', apps: {up: 0, down: 0, unknown: 0, list: ['app1']}});
              assert.ok(false, 'StandaloneServer was successfully created when it should have failed - an initial apps.list array with the correct element objects is required');
            } catch(error) {
              assert.equal(error, 'StandaloneServer created without an initial apps.list array with some elements which are not objects with name and state', 'Error reported did not match expected error');
            }
          });

          tdd.test('constructor - empty cluster, scalingPolicy, or apps', function() {
            var server = new StandaloneServer({name: 'server1', userdir: '/wlp/usr', host: 'localhost', apps: {up: 0, down: 0, unknown: 0, list: []}});

            assert.equal(server.id,               'server1',   'StandaloneServer.id did not have the correct initialized value');
            assert.equal(server.name,             'server1',   'StandaloneServer.name did not have the correct initialized value');
            assert.equal(server.userdir,          '/wlp/usr',  'StandaloneServer.userdir did not have the correct initialized value');
            assert.equal(server.host,             'localhost', 'StandaloneServer.host did not have the correct initialized value');
            assert.equal(server.cluster,          null,        'StandaloneServer.cluster did not have the correct initialized value');
            assert.equal(server.scalingPolicy,    null,        'StandaloneServer.scalingPolicy did not have the correct initialized value');

            assert.equal(server.apps.up,          0,           'StandaloneServer.app.up did not have the correct initialized value');
            assert.equal(server.apps.down,        0,           'StandaloneServer.apps.down did not have the correct initialized value');
            assert.equal(server.apps.unknown,     0,           'StandaloneServer.apps.unknown did not have the correct initialized value');
            assert.ok(Array.isArray(server.apps.list),         'StandaloneServer.apps.list was not an Array');
            assert.equal(server.apps.list.length, 0,           'StandaloneServer.apps.list.length was not initially empty');
          });

          tdd.test('constructor - with cluster, scalingPolicy,  and apps', function() {
            var server = new StandaloneServer({name: 'server1', userdir: '/wlp/usr', host: 'localhost', cluster: 'cluster1', scalingPolicy: 'default', apps: {up: 1, down: 2, unknown: 3, list: [{name:'snoop',state:'STARTED'}]}});

            assert.equal(server.id,               'server1',   'StandaloneServer.id did not have the correct initialized value');
            assert.equal(server.name,             'server1',   'StandaloneServer.name did not have the correct initialized value');
            assert.equal(server.userdir,          '/wlp/usr',  'StandaloneServer.userdir did not have the correct initialized value');
            assert.equal(server.host,             'localhost', 'StandaloneServer.host did not have the correct initialized value');
            assert.equal(server.cluster,          'cluster1',  'StandaloneServer.cluster did not have the correct initialized value');
            assert.equal(server.scalingPolicy,    'default',   'StandaloneServer.scalingPolicy did not have the correct initialized value');

            assert.equal(server.apps.up,          1,           'StandaloneServer.app.up did not have the correct initialized value');
            assert.equal(server.apps.down,        2,           'StandaloneServer.apps.down did not have the correct initialized value');
            assert.equal(server.apps.unknown,     3,           'StandaloneServer.apps.unknown did not have the correct initialized value');
            assert.ok(Array.isArray(server.apps.list),         'StandaloneServer.apps.list was not an Array');
            assert.equal(server.apps.list.length, 1,           'StandaloneServer.apps.list.length was not the correct value');
            assert.equal(JSON.stringify(server.apps.list[0]), JSON.stringify({name:'snoop',state:'STARTED'}), 'StandaloneServer.apps.list[0] did not have the correct value');
          });

          tdd.test('handleChangeEvent - added cluster', function() {
            var server = new StandaloneServer({name: 'server1', userdir: '/wlp/usr', host: 'localhost', apps: {up: 0, down: 0, unknown: 0, list: []}});

            // Check pre-event conditions
            assert.equal(server.cluster,          null,        'StandaloneServer.cluster did not have the correct initialized value');

            // Simulate event
            server._handleChangeEvent({type: 'standaloneServer', cluster: 'cluster1' });

            assert.equal(server.name,             'server1',   'StandaloneServer.name did not have the correct initialized value');
            assert.equal(server.userdir,          '/wlp/usr',  'StandaloneServer.userdir did not have the correct initialized value');
            assert.equal(server.host,             'localhost', 'StandaloneServer.host did not have the correct initialized value');
            assert.equal(server.cluster,          'cluster1',  'StandaloneServer.cluster did not update based on the event value');
            assert.equal(server.scalingPolicy,    null,        'StandaloneServer.scalingPolicy did not have the correct initialized value');

            assert.equal(server.apps.up,          0,           'StandaloneServer.app.up did not have the correct initialized value');
            assert.equal(server.apps.down,        0,           'StandaloneServer.apps.down did not have the correct initialized value');
            assert.equal(server.apps.unknown,     0,           'StandaloneServer.apps.unknown did not have the correct initialized value');
            assert.ok(Array.isArray(server.apps.list),         'StandaloneServer.apps.list was not an Array');
            assert.equal(server.apps.list.length, 0,           'StandaloneServer.apps.list.length was not initially empty');
          });

          tdd.test('handleChangeEvent - changed cluster', function() {
            var server = new StandaloneServer({name: 'server1', userdir: '/wlp/usr', host: 'localhost', cluster: 'cluster1', apps: {up: 0, down: 0, unknown: 0, list: []}});

            // Check pre-event conditions
            assert.equal(server.cluster,          'cluster1',  'StandaloneServer.cluster did not have the correct initialized value');

            // Simulate event
            server._handleChangeEvent({type: 'standaloneServer', cluster: 'cluster2' });

            assert.equal(server.name,             'server1',   'StandaloneServer.name did not have the correct initialized value');
            assert.equal(server.userdir,          '/wlp/usr',  'StandaloneServer.userdir did not have the correct initialized value');
            assert.equal(server.host,             'localhost', 'StandaloneServer.host did not have the correct initialized value');
            assert.equal(server.cluster,          'cluster2',  'StandaloneServer.cluster did not update based on the event value');
            assert.equal(server.scalingPolicy,    null,        'StandaloneServer.scalingPolicy did not have the correct initialized value');

            assert.equal(server.apps.up,          0,           'StandaloneServer.app.up did not have the correct initialized value');
            assert.equal(server.apps.down,        0,           'StandaloneServer.apps.down did not have the correct initialized value');
            assert.equal(server.apps.unknown,     0,           'StandaloneServer.apps.unknown did not have the correct initialized value');
            assert.ok(Array.isArray(server.apps.list),         'StandaloneServer.apps.list was not an Array');
            assert.equal(server.apps.list.length, 0,           'StandaloneServer.apps.list.length was not initially empty');
          });

          tdd.test('handleChangeEvent - removed cluster', function() {
            var server = new StandaloneServer({name: 'server1', userdir: '/wlp/usr', host: 'localhost', cluster: 'cluster1', apps: {up: 0, down: 0, unknown: 0, list: []}});

            // Check pre-event conditions
            assert.equal('cluster1',  server.cluster,          'StandaloneServer.cluster did not have the correct initialized value');

            // Simulate event
            server._handleChangeEvent({type: 'standaloneServer', cluster: null });

            assert.equal(   server.name,             'server1','StandaloneServer.name did not have the correct initialized value');
            assert.equal(server.userdir,          '/wlp/usr',  'StandaloneServer.userdir did not have the correct initialized value');
            assert.equal(server.host,             'localhost', 'StandaloneServer.host did not have the correct initialized value');
            assert.equal(server.cluster,          null,        'StandaloneServer.cluster did not update based on the event value');
            assert.equal(server.scalingPolicy,    null,        'StandaloneServer.scalingPolicy did not have the correct initialized value');

            assert.equal(server.apps.up,          0,           'StandaloneServer.app.up did not have the correct initialized value');
            assert.equal(server.apps.down,        0,           'StandaloneServer.apps.down did not have the correct initialized value');
            assert.equal(server.apps.unknown,     0,           'StandaloneServer.apps.unknown did not have the correct initialized value');
            assert.ok(Array.isArray(server.apps.list),       'StandaloneServer.apps.list was not an Array');
            assert.equal(server.apps.list.length, 0,           'StandaloneServer.apps.list.length was not initially empty');
          });

          tdd.test('handleChangeEvent - added scalingPolicy', function() {
            var server = new StandaloneServer({name: 'server1', userdir: '/wlp/usr', host: 'localhost', apps: {up: 0, down: 0, unknown: 0, list: []}});

            // Check pre-event conditions
            assert.equal(server.scalingPolicy,    null,        'StandaloneServer.scalingPolicy did not have the correct initialized value');

            // Simulate event
            server._handleChangeEvent({type: 'standaloneServer', scalingPolicy: 'default' });

            assert.equal(server.name,             'server1',   'StandaloneServer.name did not have the correct initialized value');
            assert.equal(server.userdir,          '/wlp/usr',  'StandaloneServer.userdir did not have the correct initialized value');
            assert.equal(server.host,             'localhost', 'StandaloneServer.host did not have the correct initialized value');
            assert.equal(server.cluster,          null,        'StandaloneServer.cluster did not have the correct initialized value');
            assert.equal(server.scalingPolicy,    'default',   'StandaloneServer.scalingPolicy did not update based on the event value');

            assert.equal(server.apps.up,          0,           'StandaloneServer.app.up did not have the correct initialized value');
            assert.equal(server.apps.down,        0,           'StandaloneServer.apps.down did not have the correct initialized value');
            assert.equal(server.apps.unknown,     0,           'StandaloneServer.apps.unknown did not have the correct initialized value');
            assert.ok(Array.isArray(server.apps.list),         'StandaloneServer.apps.list was not an Array');
            assert.equal(server.apps.list.length, 0,           'StandaloneServer.apps.list.length was not initially empty');
          });

          tdd.test('handleChangeEvent - changed scalingPolicy', function() {
            var server = new StandaloneServer({name: 'server1', userdir: '/wlp/usr', host: 'localhost', scalingPolicy: 'default', apps: {up: 0, down: 0, unknown: 0, list: []}});

            // Check pre-event conditions
            assert.equal(server.scalingPolicy,    'default',   'StandaloneServer.scalingPolicy did not have the correct initialized value');

            // Simulate event
            server._handleChangeEvent({type: 'standaloneServer', scalingPolicy: 'newPolicy' });

            assert.equal(server.name,             'server1',   'StandaloneServer.name did not have the correct initialized value');
            assert.equal(server.userdir,          '/wlp/usr',  'StandaloneServer.userdir did not have the correct initialized value');
            assert.equal(server.host,             'localhost', 'StandaloneServer.host did not have the correct initialized value');
            assert.equal(server.cluster,          null,        'StandaloneServer.cluster did not have the correct initialized value');
            assert.equal(server.scalingPolicy,    'newPolicy', 'StandaloneServer.scalingPolicy did not update based on the event value');

            assert.equal(server.apps.up,          0,           'StandaloneServer.app.up did not have the correct initialized value');
            assert.equal(server.apps.down,        0,           'StandaloneServer.apps.down did not have the correct initialized value');
            assert.equal(server.apps.unknown,     0,           'StandaloneServer.apps.unknown did not have the correct initialized value');
            assert.ok(Array.isArray(server.apps.list),       'StandaloneServer.apps.list was not an Array');
            assert.equal(server.apps.list.length, 0,           'StandaloneServer.apps.list.length was not initially empty');
          });

          tdd.test('handleChangeEvent - removed scalingPolicy', function() {
            var server = new StandaloneServer({name: 'server1', userdir: '/wlp/usr', host: 'localhost', scalingPolicy: 'default', apps: {up: 0, down: 0, unknown: 0, list: []}});

            // Check pre-event conditions
            assert.equal(server.scalingPolicy,    'default',   'StandaloneServer.scalingPolicy did not have the correct initialized value');

            // Simulate event
            server._handleChangeEvent({type: 'standaloneServer', scalingPolicy: null });

            assert.equal(server.name,             'server1',   'StandaloneServer.name did not have the correct initialized value');
            assert.equal(server.userdir,          '/wlp/usr',  'StandaloneServer.userdir did not have the correct initialized value');
            assert.equal(server.host,             'localhost', 'StandaloneServer.host did not have the correct initialized value');
            assert.equal(server.cluster,          null,        'StandaloneServer.cluster did not have the correct initialized value');
            assert.equal(server.scalingPolicy,    null,        'StandaloneServer.scalingPolicy did not update based on the event value');

            assert.equal(server.apps.up,          0,           'StandaloneServer.app.up did not have the correct initialized value');
            assert.equal(server.apps.down,        0,           'StandaloneServer.apps.down did not have the correct initialized value');
            assert.equal(server.apps.unknown,     0,           'StandaloneServer.apps.unknown did not have the correct initialized value');
            assert.ok(Array.isArray(server.apps.list),       'StandaloneServer.apps.list was not an Array');
            assert.equal(server.apps.list.length, 0,           'StandaloneServer.apps.list.length was not initially empty');
          });

          tdd.test('handleChangeEvent - updated apps tallies', function() {
            var server = new StandaloneServer({name: 'server1', userdir: '/wlp/usr', host: 'localhost', apps: {up: 0, down: 0, unknown: 0, list: []}});

            // Simulate event
            server._handleChangeEvent({type: 'standaloneServer', apps: { up: 1, down: 2, unknown: 3 } });

            assert.equal(server.name,             'server1',   'StandaloneServer.name did not have the correct initialized value');
            assert.equal(server.userdir,          '/wlp/usr',  'StandaloneServer.userdir did not have the correct initialized value');
            assert.equal(server.host,             'localhost', 'StandaloneServer.host did not have the correct initialized value');
            assert.equal(server.cluster,          null,        'StandaloneServer.cluster did not have the correct initialized value');
            assert.equal(server.scalingPolicy,    null,        'StandaloneServer.scalingPolicy did not have the correct initialized value');

            assert.equal(server.apps.up,          1,           'StandaloneServer.app.up did not have the updated value');
            assert.equal(server.apps.down,        2,           'StandaloneServer.apps.down did not have the updated value');
            assert.equal(server.apps.unknown,     3,           'StandaloneServer.apps.unknown did not have the updated value');
            assert.ok(Array.isArray(server.apps.list),       'StandaloneServer.apps.list was not an Array');
            assert.equal(server.apps.list.length, 0,           'StandaloneServer.apps.list.length was not initially empty');
          });

          tdd.test('handleChangeEvent - added apps', function() {
            var server = new StandaloneServer({name: 'server1', userdir: '/wlp/usr', host: 'localhost', apps: {up: 0, down: 0, unknown: 0, list: []}});

            // Simulate event
            server._handleChangeEvent({type: 'standaloneServer', apps: { up: 1, down: 2, unknown: 3, added: [{name: 'snoop', state: 'STARTED'}] }});

            assert.equal(server.name,             'server1',   'StandaloneServer.name did not have the correct initialized value');
            assert.equal(server.userdir,          '/wlp/usr',  'StandaloneServer.userdir did not have the correct initialized value');
            assert.equal(server.host,             'localhost', 'StandaloneServer.host did not have the correct initialized value');
            assert.equal(server.cluster,          null,        'StandaloneServer.cluster did not have the correct initialized value');
            assert.equal(server.scalingPolicy,    null,        'StandaloneServer.scalingPolicy did not have the correct initialized value');

            assert.equal(server.apps.up,          1,           'StandaloneServer.app.up did not have the updated value');
            assert.equal(server.apps.down,        2,           'StandaloneServer.apps.down did not have the updated value');
            assert.equal(server.apps.unknown,     3,           'StandaloneServer.apps.unknown did not have the updated value');
            assert.ok(Array.isArray(server.apps.list),       'StandaloneServer.apps.list was not an Array');
            assert.equal(server.apps.list.length, 1,           'StandaloneServer.apps.list.length did not have the updated value');
            assert.equal(JSON.stringify(server.apps.list[0]), JSON.stringify({name:'snoop',state:'STARTED'}), 'StandaloneServer.apps.list[0] did not have the updated value');
          });

          tdd.test('handleChangeEvent - changed apps', function() {
            var server = new StandaloneServer({name: 'server1', userdir: '/wlp/usr', host: 'localhost', apps: {up: 0, down: 0, unknown: 0, list: [{name:'snoop',state:'STARTED'}]}});

            // Simulate event
            server._handleChangeEvent({type: 'standaloneServer', apps: { up: 1, down: 2, unknown: 3, changed: [{name: 'snoop', state: 'STOPPED'}] }});

            assert.equal(server.name,             'server1',   'StandaloneServer.name did not have the correct initialized value');
            assert.equal(server.userdir,          '/wlp/usr',  'StandaloneServer.userdir did not have the correct initialized value');
            assert.equal(server.host,             'localhost', 'StandaloneServer.host did not have the correct initialized value');
            assert.equal(server.cluster,          null,        'StandaloneServer.cluster did not have the correct initialized value');
            assert.equal(server.scalingPolicy,    null,        'StandaloneServer.scalingPolicy did not have the correct initialized value');

            assert.equal(server.apps.up,          1,           'StandaloneServer.app.up did not have the updated value');
            assert.equal(server.apps.down,        2,           'StandaloneServer.apps.down did not have the updated value');
            assert.equal(server.apps.unknown,     3,           'StandaloneServer.apps.unknown did not have the updated value');
            assert.ok(Array.isArray(server.apps.list),       'StandaloneServer.apps.list was not an Array');
            assert.equal(server.apps.list.length, 1,           'StandaloneServer.apps.list.length did not have the updated value');
            assert.equal(JSON.stringify(server.apps.list[0]), JSON.stringify({name:'snoop',state:'STOPPED'}), 'StandaloneServer.apps.list[0] did not have the updated value');
          });

          tdd.test('handleChangeEvent - removed apps', function() {
            var server = new StandaloneServer({name: 'server1', userdir: '/wlp/usr', host: 'localhost', apps: {up: 0, down: 0, unknown: 0, list: [{name:'snoop',state:'STARTED'}]}});

            // Simulate event
            server._handleChangeEvent({type: 'standaloneServer', apps: { up: 1, down: 2, unknown: 3, removed: ['snoop'] } });

            assert.equal(server.name,             'server1',   'StandaloneServer.name did not have the correct initialized value');
            assert.equal(server.userdir,          '/wlp/usr',  'StandaloneServer.userdir did not have the correct initialized value');
            assert.equal(server.host,             'localhost', 'StandaloneServer.host did not have the correct initialized value');
            assert.equal(server.cluster,          null,        'StandaloneServer.cluster did not have the correct initialized value');
            assert.equal(server.scalingPolicy,    null,        'StandaloneServer.scalingPolicy did not have the correct initialized value');

            assert.equal(server.apps.up,          1,           'StandaloneServer.app.up did not have the updated value');
            assert.equal(server.apps.down,        2,           'StandaloneServer.apps.down did not have the updated value');
            assert.equal(server.apps.unknown,     3,           'StandaloneServer.apps.unknown did not have the updated value');
            assert.ok(Array.isArray(server.apps.list),       'StandaloneServer.apps.list was not an Array');
            assert.equal(server.apps.list.length, 0,           'StandaloneServer.apps.list.length did not have the updated value');
          });
          
          tdd.test('handleChangeEvent - multiple changes with notification', function() {
            var observer = new StandaloneServerObserver();
            var server = new StandaloneServer({name: 'server1', userdir: '/wlp/usr', host: 'localhost', apps: {up: 0, down: 0, unknown: 0, list: [{name:'snoop',state:'STARTED'}]}});

            server.subscribe(observer);

            // Simulate event
            server._handleChangeEvent({type: 'standaloneServer', cluster: 'cluster1', scalingPolicy: 'newPolicy', scalingPolicyEnabled: true,
              apps: { up: 1, down: 2, unknown: 3, added: [{name: 'testApp', state: 'STOPPED'}], changed: [], removed: ['snoop'] } });

            assert.equal(server.name,             'server1',   'StandaloneServer.name did not have the correct initialized value');
            assert.equal(server.userdir,          '/wlp/usr',  'StandaloneServer.userdir did not have the correct initialized value');
            assert.equal(server.host,             'localhost', 'StandaloneServer.host did not have the correct initialized value');
            assert.equal(server.cluster,          'cluster1',  'StandaloneServer.cluster did not have the updated value');
            assert.equal(server.scalingPolicy,    'newPolicy', 'StandaloneServer.scalingPolicy did not have the updated value');
            assert.isTrue(server.scalingPolicyEnabled,         'StandaloneServer.scalingPolicyEnabled did not have the updated value');

            assert.equal(server.apps.up,          1,           'StandaloneServer.app.up did not have the updated value');
            assert.equal(server.apps.down,        2,           'StandaloneServer.apps.down did not have the updated value');
            assert.equal(server.apps.unknown,     3,           'StandaloneServer.apps.unknown did not have the updated value');
            assert.ok(Array.isArray(server.apps.list),       'StandaloneServer.apps.list was not an Array');
            assert.equal(server.apps.list.length, 1,           'StandaloneServer.apps.list.length did not have the updated value');
            assert.equal(JSON.stringify(server.apps.list[0]), JSON.stringify({name: 'testApp', state: 'STOPPED'}),   'StandaloneServer.apps.list[0] did not have the updated value');

            // Validate the Observer was passed the correct tally objects after the second event
            assert.equal(observer.newCluster,       'cluster1',  'ServerObserver did not get the correct OLD value for cluster');
            assert.equal(observer.oldCluster,       null,        'ServerObserver did not get the correct NEW value for cluster');
            assert.equal(observer.newScalingPolicy, 'newPolicy', 'ServerObserver did not get the correct OLD value for scalingPolicy');
            assert.equal(observer.oldScalingPolicy, null,        'ServerObserver did not get the correct NEW value for scalingPolicy');
            assert.isTrue(observer.newScalingPolicyEnabled,      'ServerObserver did not get the correct OLD value for scalingPolicyEnabled');
            assert.equal(null, observer.oldScalingPolicyEnabled, 'ServerObserver did not get the correct NEW value for scalingPolicyEnabled');

            assert.ok(observer.newTally,                       'ServerObserver did not get the newTally object set');
            assert.equal(observer.newTally.up,      1,           'ServerObserver did not get the correct new value for the up tally');
            assert.equal(observer.newTally.down,    2,           'ServerObserver did not get the correct new value for the down tally');
            assert.equal(observer.newTally.unknown, 3,           'ServerObserver did not get the correct new value for the unknown tally');

            assert.ok(observer.oldTally,                       'ServerObserver did not get the oldTally object set');
            assert.equal(observer.oldTally.up,      0,           'ServerObserver did not get the correct old value for the up tally');
            assert.equal(observer.oldTally.down,    0,           'ServerObserver did not get the correct old value for the down tally');
            assert.equal(observer.oldTally.unknown, 0,           'ServerObserver did not get the correct old value for the unknown tally');

            assert.ok(observer.newList,                        'ServerObserver.newList did not get set, when it should have been');
            assert.equal(observer.newList.length,   1,           'ServerObserver.newList was not of expected size');
            assert.equal(JSON.stringify(observer.newList[0]), JSON.stringify({name: "testApp", state: "STOPPED"}), 'ServerObserver.newList[0] was not of expected value');

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
          });

          tdd.test('handleChangeEvent - unset or wrong event id or type is ignored', function() {
            var server = new StandaloneServer({name: 'server1', userdir: '/wlp/usr', host: 'localhost', apps: {up: 0, down: 0, unknown: 0, list: []}});

            // Simulate ignored events
            server._handleChangeEvent({type: 'hosts', state: 'STOPPED'});

            assert.equal(server.name,             'server1',   'StandaloneServer.name did not have the correct initialized value');
            assert.equal(server.userdir,          '/wlp/usr',  'StandaloneServer.userdir did not have the correct initialized value');
            assert.equal(server.host,             'localhost', 'StandaloneServer.host did not have the correct initialized value');
            assert.equal(server.cluster,          null,        'StandaloneServer.cluster did not have the correct initialized value');
            assert.equal(server.scalingPolicy,    null,        'StandaloneServer.scalingPolicy did not have the correct initialized value');

            assert.equal(server.apps.up,          0,           'StandaloneServer.app.up did not have the correct initialized value');
            assert.equal(server.apps.down,        0,           'StandaloneServer.apps.down did not have the correct initialized value');
            assert.equal(server.apps.unknown,     0,           'StandaloneServer.apps.unknown did not have the correct initialized value');
            assert.ok(Array.isArray(server.apps.list),       'StandaloneServer.apps.list was not an Array');
            assert.equal(server.apps.list.length, 0,           'StandaloneServer.apps.list.length was not initially empty');
          });

          tdd.test('StandaloneServer.stop - returns Deferred', function() {
            var resource = new StandaloneServer({name: 'server1', userdir: '/wlp/usr', host: 'localhost', apps: {up: 0, down: 0, unknown: 0, list: []}});
            assert.ok(resource.stop() instanceof Deferred, 'StandaloneServer.stop should return a Deferred');
          });

          tdd.test('stop - GET encounters 400', function() {
            var dfd = this.async(1000);
            var resource = new StandaloneServer({name: 'server1', userdir: '/wlp/usr', host: 'localhost', apps: {up: 0, down: 0, unknown: 0, list: []}});

            resource.stop().then(function(successMsg) {
              dfd.errback('Operation succeeded but should have failed. Got success message: ' + successMsg);
            }, function(errMsg) {
              if (errMsg === i18n.STANDALONE_STOP_CANT_DETERMINE_MBEAN) {
                dfd.resolve('Operation failed with expected message: ' + errMsg);             
              } else {
                dfd.errback('Operation failed with unexpected error message: ' + errMsg);  
              }
            });

            mockServer.respondWith('GET', '/IBMJMXConnectorREST/mbeans?objectName=osgi.core:type=framework,version=*,framework=org.eclipse.osgi,uuid=*',
                [400, { 'Content-Type': 'application/json' }, '{}']);
            
            mockServer.respond();

            return dfd;
          });

          tdd.test('stop - GET matches no MBeans', function() {
            var dfd = this.async(1000);
            var resource = new StandaloneServer({name: 'server1', userdir: '/wlp/usr', host: 'localhost', apps: {up: 0, down: 0, unknown: 0, list: []}});

            resource.stop().then(function(successMsg) {
              dfd.errback('Operation succeeded but should have failed. Got success message: ' + successMsg);
            }, function(errMsg) {
              if (errMsg === i18n.STANDALONE_STOP_NO_MBEAN) {
                dfd.resolve('Operation failed with expected message: ' + errMsg);             
              } else {
                dfd.errback('Operation failed with unexpected error message: ' + errMsg);  
              }
            });

            mockServer.respondWith('GET', '/IBMJMXConnectorREST/mbeans?objectName=osgi.core:type=framework,version=*,framework=org.eclipse.osgi,uuid=*',
                [200, { 'Content-Type': 'application/json' },'[]']);
            mockServer.respond();

            return dfd;
          });

          tdd.test('stop - GET matches too many MBeans', function() {
            var dfd = this.async(1000);
            var resource = new StandaloneServer({name: 'server1', userdir: '/wlp/usr', host: 'localhost', apps: {up: 0, down: 0, unknown: 0, list: []}});

            resource.stop().then(function(successMsg) {
              dfd.errback('Operation succeeded but should have failed. Got success message: ' + successMsg);
            }, function(errMsg) {
              if (errMsg === i18n.STANDALONE_STOP_CANT_DETERMINE_MBEAN) {
                dfd.resolve('Operation failed with expected message: ' + errMsg);             
              } else {
                dfd.errback('Operation failed with unexpected error message: ' + errMsg);  
              }
            });

            mockServer.respondWith('GET', '/IBMJMXConnectorREST/mbeans?objectName=osgi.core:type=framework,version=*,framework=org.eclipse.osgi,uuid=*',
                [200, { 'Content-Type': 'application/json' },'[ {}, {}]']);
            mockServer.respond();

            return dfd;
          });

          tdd.test('stop - POST fails', function() {
            var dfd = this.async(1000);
            var resource = new StandaloneServer({name: 'server1', userdir: '/wlp/usr', host: 'localhost', apps: {up: 0, down: 0, unknown: 0, list: []}});

            resource.stop().then(function(successMsg) {
              dfd.errback('Operation succeeded but should have failed. Got success message: ' + successMsg);
            }, function(errMsg) {
              if (errMsg === i18n.STANDALONE_STOP_FAILED) {
                dfd.resolve('Operation failed with expected message: ' + errMsg);             
              } else {
                dfd.errback('Operation failed with unexpected error message: ' + errMsg);  
              }
            });

            mockServer.respondWith('GET', '/IBMJMXConnectorREST/mbeans?objectName=osgi.core:type=framework,version=*,framework=org.eclipse.osgi,uuid=*',
                [200, { 'Content-Type': 'application/json' },'[{"objectName":"...","className":"...","URL":"mbeanURL"}]']);
            mockServer.respondWith('POST', 'mbeanURL/operations/shutdownFramework',
                [400, { 'Content-Type': 'application/json' },'[]']);
            mockServer.respond();

            return dfd;
          });

          tdd.test('stop - succeeds', function() {
            var dfd = this.async(1000);
            var resource = new StandaloneServer({name: 'server1', userdir: '/wlp/usr', host: 'localhost', apps: {up: 0, down: 0, unknown: 0, list: []}});

            resource.stop().then(function(successMsg) {
              if (successMsg === i18n.STANDALONE_STOP_SUCCESS) {
                dfd.resolve('Operation succeeded with expected message: ' + successMsg);
              } else {
                dfd.errback('Operation succeeded but got an unexpected message. Got success message: ' + successMsg);  
              }
            }, function(errMsg) {
              dfd.errback('Operation failed but should have succeeded. Got success message: ' + errMsg);
            });

            mockServer.respondWith('GET', '/IBMJMXConnectorREST/mbeans?objectName=osgi.core:type=framework,version=*,framework=org.eclipse.osgi,uuid=*',
                [200, { 'Content-Type': 'application/json' },'[{"objectName":"...","className":"...","URL":"mbeanURL"}]']);
            mockServer.respondWith('POST', 'mbeanURL/operations/shutdownFramework',
                [200, { 'Content-Type': 'application/json' },'[]']);
            mockServer.respond();

            return dfd;
          });

        });
      }
  });