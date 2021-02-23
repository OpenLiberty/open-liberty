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
 * Test cases for Host
 */
define([
        "intern!tdd",
        "intern/chai!assert",
        "intern/dojo/declare",
        "resources/_objects/Host",
        "resources/Observer"
        ],

        function(tdd, assert, declare, Host, Observer) {

  var HostObserver = declare([Observer], {
    id: 'testObserver',

    onDestroyed: function() {
      this.isDestroyed = true;
    },

    onStateChange: function(newState, oldState) {
      this.newState = newState;
      this.oldState = oldState;
    },

    onRuntimesChange: function(newList, oldList, added, removed) {
      this.newRuntimesList = newList;
      this.oldRuntimesList = oldList;
      this.addedRuntimes = added;
      this.removedRuntimes = removed;
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

    onAlertsChange: function(newAlerts, oldAlerts) {
      this.newAlerts = newAlerts;
      this.oldAlerts = oldAlerts;
    }
  });

  with (assert) {

    /**
     * Defines the 'Host Object Tests' module test suite.
     */
    tdd.suite('Host Object Tests', function() {

      tdd.test('constructor - no initialization object', function() {
        try {
          new Host();
          assert.ok(false, 'Host was successfully created when it should have failed - an initialization object is required');
        } catch(error) {
          assert.equal(error, 'ObservableResource created without an initialization object', 'Error reported did not match expected error');
        }
      });

      tdd.test('constructor - no id', function() {
        try {
          new Host({runtimes: [], servers: {up: 0, down: 0, unknown: 0, list: []}});
          assert.ok(false, 'Host was successfully created when it should have failed - an id is required');
        } catch(error) {
          assert.equal(error, 'ObservableResource created without an id', 'Error reported did not match expected error');
        }
      });

      tdd.test('constructor - no initial runtimes', function() {
        try {
          new Host({id: 'localhost', servers: {up: 0, down: 0, unknown: 0, list: []}});
          assert.ok(false, 'Host "localhost" was successfully created when it should have failed - an initial runtimes array is required');
        } catch(error) {
          assert.equal(error, 'Host "localhost" created without an initial runtimes array', 'Error reported did not match expected error');
        }
      });

      tdd.test('constructor - no initial servers', function() {
        try {
          new Host({id: 'localhost', runtimes: {list:[]}});
          assert.ok(false, 'Host "localhost" was successfully created when it should have failed - an initial servers object is required');
        } catch(error) {
          assert.equal(error, 'Host "localhost" created without an initial servers object', 'Error reported did not match expected error');
        }
      });   

      tdd.test('constructor - no initial servers.up', function() {
        try {
          new Host({id: 'localhost', runtimes: {list:[]}, servers: {down: 0, unknown: 0, list: []}});
          assert.ok(false, 'Host "localhost" was successfully created when it should have failed - an initial servers.up tally is required');
        } catch(error) {
          assert.equal(error, 'Host "localhost" created without an initial servers.up tally', 'Error reported did not match expected error');
        }
      });

      tdd.test('constructor - no initial servers.down', function() {
        try {
          new Host({id: 'localhost', runtimes: {list:[]}, servers: {up: 0, unknown: 0, list: []}});
          assert.ok(false, 'Host "localhost" was successfully created when it should have failed - an initial servers.down tally is required');
        } catch(error) {
          assert.equal(error, 'Host "localhost" created without an initial servers.down tally', 'Error reported did not match expected error');
        }
      });

      tdd.test('constructor - no initial servers.unknown', function() {
        try {
          new Host({id: 'localhost', runtimes: {list:[]}, servers: {up: 0, down: 0, list: []}});
          assert.ok(false, 'Host "localhost" was successfully created when it should have failed - an initial servers.unknown tally is required');
        } catch(error) {
          assert.equal(error, 'Host "localhost" created without an initial servers.unknown tally', 'Error reported did not match expected error');
        }
      });

      tdd.test('constructor - no initial servers.list', function() {
        try {
          new Host({id: 'localhost', runtimes: {list:[]}, servers: {up: 0, down: 0, unknown: 0}});
          assert.ok(false, 'Host "localhost" was successfully created when it should have failed - an initial servers.list array is required');
        } catch(error) {
          assert.equal(error, 'Host "localhost" created without an initial servers.list array', 'Error reported did not match expected error');
        }
      });

      tdd.test('constructor - initial servers.list not an array', function() {
        try {
          new Host({id: 'localhost', runtimes: {list:[]}, servers: {up: 0, down: 0, unknown: 0, list: 'list'}});
          assert.ok(false, 'Host "localhost" was successfully created when it should have failed - an initial servers.list array is required');
        } catch(error) {
          assert.equal(error, 'Host "localhost" created without an initial servers.list array', 'Error reported did not match expected error');
        }
      });

      tdd.test('constructor - empty runtimes or servers', function() {
        var host = new Host({id: 'localhost', runtimes: {list:[]}, servers: {up: 0, down: 0, unknown: 0, list: []}});

        assert.equal(host.id,               'localhost', 'Host.id did not have the correct initialized value');
        assert.equal(host.name,             'localhost', 'Host.name did not have the correct initialized value');
        assert.equal(host.state,            'STOPPED',   'Host.state did not have the correct initialized value');

        assert.ok(Array.isArray(host.runtimes.list),     'Host.runtimes.list was not an array');
        assert.equal(host.runtimes.list.length,  0,      'Host.runtimes.length was not initially empty');

        assert.equal(host.servers.up,       0,           'Host.servers.up did not have the correct initialized value');
        assert.equal(host.servers.down,     0,           'Host.servers.down did not have the correct initialized value');
        assert.equal(host.servers.unknown,  0,           'Host.servers.unknown did not have the correct initialized value');
        assert.ok(Array.isArray(host.servers.list),      'Host.servers.list was not an Array');
        assert.equal(host.servers.list.length, 0,        'Host.servers.list.length was not initially empty');
      });

      tdd.test('constructor - with runtimes and servers', function() {
        var host = new Host({id: 'localhost', runtimes: {list: [{id: 'localhost,/wlp', type: 'runtime', name: 'localhost,/wlp'}]}, servers: {up: 1, down: 2, unknown: 3, list: ['localhost,/wlp/usr,server1']}, alerts: {count: 8}});

        assert.equal(host.id,               'localhost', 'Host.id did not have the correct initialized value');
        assert.equal(host.name,             'localhost', 'Host.name did not have the correct initialized value');
        assert.equal(host.state,    'PARTIALLY_STARTED', 'Host.state did not have the correct initialized value');

        assert.ok(Array.isArray(host.runtimes.list),     'Host.runtimes.list was not an array');
        assert.equal(host.runtimes.list.length,  1,      'Host.runtimes.length was not the correct value');
        assert.equal(host.runtimes.list[0].id, 'localhost,/wlp', 'Host.runtimes.list[0].id did not have the correct value');

        assert.equal(host.servers.up,       1,           'Host.servers.up did not have the correct initialized value');
        assert.equal(host.servers.down,     2,           'Host.servers.down did not have the correct initialized value');
        assert.equal(host.servers.unknown,  3,           'Host.servers.unknown did not have the correct initialized value');
        assert.ok(Array.isArray(host.servers.list),      'Host.servers.list was not an Array');
        assert.equal(host.servers.list.length, 1,        'Host.servers.list.length was not the correct value');
        assert.equal(host.servers.list[0], 'localhost,/wlp/usr,server1', 'Host.servers.list[0] did not have the correct value');

        assert.ok(host.alerts,                         'Host did not have an alerts attribute');
        assert.equal(host.alerts.count,     8,           'Host.alerts.count did not have the correct initialized value');
      });

      tdd.test('handleChangeEvent - added runtimes', function() {
        var host = new Host({id: 'localhost', runtimes: {list: []}, servers: {up: 0, down: 0, unknown: 0, list: []}});

        // Simulate event
        host._handleChangeEvent({type: 'host', id: 'localhost', runtimes: { added: [{id: 'localhost,/wlp', type: 'runtime', name: 'localhost,/wlp'}]} });

        assert.equal(host.id,               'localhost', 'Host.id did not have the correct initialized value');
        assert.equal(host.name,             'localhost', 'Host.name did not have the correct initialized value');
        assert.equal(host.state,            'STOPPED',   'Host.state did not have the correct updated value');

        assert.ok(Array.isArray(host.runtimes.list),     'Host.runtimes.list was not an array');
        assert.equal(host.runtimes.list.length,  1,      'Host.runtimes.list.length did not update to the new count');
        assert.equal(host.runtimes.list[0].id, 'localhost,/wlp', 'Host.runtimes.list[0].id did not have the correct value');

        assert.equal(host.servers.up,       0,           'Host.servers.up did not have the correct initialized value');
        assert.equal(host.servers.down,     0,           'Host.servers.down did not have the correct initialized value');
        assert.equal(host.servers.unknown,  0,           'Host.servers.unknown did not have the correct initialized value');
        assert.ok(Array.isArray(host.servers.list),      'Host.servers.list was not an Array');
        assert.equal(host.servers.list.length, 0,        'Host.servers.list.length was not initially empty');
      });

      tdd.test('handleChangeEvent - removed runtimes', function() {
        var host = new Host({id: 'localhost', runtimes: {list: [{id:'localhost,/wlp', name:'localhost,/wlp', type:'runtime'},
                                                                {id:'localhost,/opt/wlp', name:'localhost,/opt/wlp', type:'runtime'}]},
                                                         servers: {up: 0, down: 0, unknown: 0, list: []}});

        // Simulate event
        host._handleChangeEvent({type: 'host', id: 'localhost', runtimes: { removed: [{id:'localhost,/opt/wlp', name:'localhost,/opt/wlp', type:'runtime'}] } });

        assert.equal(host.id,               'localhost', 'Host.id did not have the correct initialized value');
        assert.equal(host.name,             'localhost', 'Host.name did not have the correct initialized value');
        assert.equal(host.state,            'STOPPED',   'Host.state did not have the correct updated value');

        assert.ok(Array.isArray(host.runtimes.list),     'Host.runtimes.list was not an array');
        assert.equal(host.runtimes.list.length,  1,      'Host.runtimes.list.length did not update to the new count');
        assert.equal(host.runtimes.list[0].id, 'localhost,/wlp', 'Host.runtimes.list[0].id did not have the correct value');

        assert.equal(host.servers.up,       0,           'Host.servers.up did not have the correct initialized value');
        assert.equal(host.servers.down,     0,           'Host.servers.down did not have the correct initialized value');
        assert.equal(host.servers.unknown,  0,           'Host.servers.unknown did not have the correct initialized value');
        assert.ok(Array.isArray(host.servers.list),      'Host.servers.list was not an Array');
        assert.equal(host.servers.list.length, 0,        'Host.servers.list.length was not initially empty');
      });

      tdd.test('handleChangeEvent - updated servers tallies', function() {
        var host = new Host({id: 'localhost', runtimes: {list: []}, servers: {up: 0, down: 0, unknown: 0, list: []}});

        // Simulate event
        host._handleChangeEvent({type: 'host', id: 'localhost', servers: { up: 1, down: 2, unknown: 3 } });

        assert.equal(host.id,               'localhost', 'Host.id did not have the correct initialized value');
        assert.equal(host.name,             'localhost', 'Host.name did not have the correct initialized value');
        assert.equal(host.state,    'PARTIALLY_STARTED', 'Host.state did not have the correct updated value');

        assert.ok(Array.isArray(host.runtimes.list),     'Host.runtimes.list was not an array');
        assert.equal(host.runtimes.list.length,  0,      'Host.runtimes.list.length was not initially empty');

        assert.equal(host.servers.up,       1,           'Host.servers.up did not have the updated value');
        assert.equal(host.servers.down,     2,           'Host.servers.down did not have the updated value');
        assert.equal(host.servers.unknown,  3,           'Host.servers.unknown did not have the updated value');
        assert.ok(Array.isArray(host.servers.list),      'Host.servers.list was not an Array');
        assert.equal(host.servers.list.length, 0,        'Host.servers.list.length was not initially empty');
      });

      tdd.test('handleChangeEvent - added servers', function() {
        var host = new Host({id: 'localhost', runtimes: {list: []}, servers: {up: 0, down: 0, unknown: 0, list: []}});

        // Simulate event
        host._handleChangeEvent({type: 'host', id: 'localhost', servers: { up: 1, down: 0, unknown: 0, added: ['localhost,/wlp/usr,server1'] } });

        assert.equal(host.id,               'localhost', 'Host.id did not have the correct initialized value');
        assert.equal(host.name,             'localhost', 'Host.name did not have the correct initialized value');
        assert.equal(host.state,            'STARTED',   'Host.state did not have the correct updated value');

        assert.ok(Array.isArray(host.runtimes.list),     'Host.runtimes.list was not an array');
        assert.equal(host.runtimes.list.length,  0,      'Host.runtimes.list.length was not initially empty');

        assert.equal(host.servers.up,       1,           'Host.servers.up did not have the updated value');
        assert.equal(host.servers.down,     0,           'Host.servers.down did not have the updated value');
        assert.equal(host.servers.unknown,  0,           'Host.servers.unknown did not have the updated value');
        assert.ok(Array.isArray(host.servers.list),      'Host.servers.list was not an Array');
        assert.equal(host.servers.list.length, 1,        'Host.servers.list.length did not have the correct value');
        assert.equal(host.servers.list[0], 'localhost,/wlp/usr,server1', 'Host.servers.list[0] did not have the correct value');
      });

      tdd.test('handleChangeEvent - removed servers', function() {
        var host = new Host({id: 'localhost', runtimes: {list: []}, servers: {up: 1, down: 0, unknown: 1, list: ['localhost,/wlp/usr,server1','localhost,/wlp/usr,server2']}});

        assert.equal(host.state,  'PARTIALLY_STARTED',   'Host.state did not have the correct initialized value');

        // Simulate event
        host._handleChangeEvent({type: 'host', id: 'localhost', servers: { up: 0, down: 0, unknown: 1, removed: ['localhost,/wlp/usr,server2'] } });

        assert.equal(host.id,               'localhost', 'Host.id did not have the correct initialized value');
        assert.equal(host.name,             'localhost', 'Host.name did not have the correct initialized value');
        assert.equal(host.state,            'STOPPED',   'Host.state did not have the correct updated value');

        assert.ok(Array.isArray(host.runtimes.list),     'Host.runtimes.list was not an array');
        assert.equal(host.runtimes.list.length,  0,      'Host.runtimes.list was not initially empty');

        assert.equal(host.servers.up,       0,           'Host.servers.up did not have the updated value');
        assert.equal(host.servers.down,     0,           'Host.servers.down did not have the updated value');
        assert.equal(host.servers.unknown,  1,           'Host.servers.unknown did not have the updated value');
        assert.ok(Array.isArray(host.servers.list),      'Host.servers.list was not an Array');
        assert.equal(host.servers.list.length, 1,        'Host.servers.list.length did not have the correct value');
        assert.equal(host.servers.list[0], 'localhost,/wlp/usr,server1', 'Host.servers.list[0] did not have the correct value');
      });

      tdd.test('handleChangeEvent - changed alerts', function() {
        var host = new Host({id: 'localhost', runtimes: {list: []}, servers: {up: 0, down: 0, unknown: 0, list: []}});

        // Check pre-event conditions
        assert.notOk(host.alerts,                        'Host.alerts was not falsy. It should have no default value');

        // Simulate event
        var alerts = { count: 1, unknown: [{id: 'application', type: 'snoop'}] };
        host._handleChangeEvent({type: 'host', id: 'localhost', alerts: alerts });

        assert.equal(host.id,               'localhost', 'Host.id did not have the correct initialized value');
        assert.equal(host.name,             'localhost', 'Host.name did not have the correct initialized value');
        assert.equal(host.state,            'STOPPED',   'Host.state did not have the correct initialized value');

        assert.ok(Array.isArray(host.runtimes.list),     'Host.runtimes.list was not an array');
        assert.equal(host.runtimes.list.length,  0,      'Host.runtimes.list.length was not initially empty');

        assert.equal(host.servers.up,       0,           'Host.servers.up did not have the correct initialized value');
        assert.equal(host.servers.down,     0,           'Host.servers.down did not have the correct initialized value');
        assert.equal(host.servers.unknown,  0,           'Host.servers.unknown did not have the correct initialized value');
        assert.ok(Array.isArray(host.servers.list),      'Host.servers.list was not an Array');
        assert.equal(host.servers.list.length, 0,        'Host.servers.list.length was not initially empty');

        assert.equal(host.alerts,           alerts,      'Host.alerts did not update based on the event value');
      });

      tdd.test('handleChangeEvent - multiple changes', function() {
        var observer = new HostObserver();
        var host = new Host({id: 'localhost', runtimes: {list: [{id:'localhost,/wlp', name:'localhost,/wlp', type:'runtime'}]}, servers: {up: 0, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}});

        host.subscribe(observer);

        // Simulate event
        var alerts = { count: 1, unknown: [{id: 'application', type: 'snoop'}] };
        host._handleChangeEvent({type: 'host', id: 'localhost',
          runtimes: { added: [{id:'localhost,/opt/wlp', name:'localhost,/opt/wlp', type:'runtime'}],
                      removed: [{id:'localhost,/wlp', name:'localhost,/wlp', type:'runtime'}] },
          servers: { up: 1, down: 2, unknown: 3, added: ['localhost,/wlp/usr,server2'], removed: ['localhost,/wlp/usr,server1'] },
          alerts: alerts });

        assert.equal(host.id,               'localhost', 'Host.id did not have the correct initialized value');
        assert.equal(host.name,             'localhost', 'Host.name did not have the correct initialized value');
        assert.equal(host.state,    'PARTIALLY_STARTED', 'Host.state did not have the correct initialized value');

        assert.ok(Array.isArray(host.runtimes.list),     'Host.runtimes.list was not an array');
        assert.equal(host.runtimes.list.length,  1,      'Host.runtimes.list.length did not have the correct value');
        assert.equal(host.runtimes.list[0].id, 'localhost,/opt/wlp', 'Host.runtimes.list[0].id did not have the correct value');

        assert.equal(host.servers.up,       1,           'Host.servers.up did not have the correct initialized value');
        assert.equal(host.servers.down,     2,           'Host.servers.down did not have the correct initialized value');
        assert.equal(host.servers.unknown,  3,           'Host.servers.unknown did not have the correct initialized value');
        assert.ok(Array.isArray(host.servers.list),      'Host.servers.list was not an Array');
        assert.equal(host.servers.list.length, 1,        'Host.servers.list.length did not have the correct value');
        assert.equal(host.servers.list[0], 'localhost,/wlp/usr,server2', 'Host.servers.list[0] did not have the correct value');

        assert.equal(host.alerts,           alerts,      'Host.alerts did not update based on the event value');

        // Validate the Observer was passed the correct tally objects after the second event
        // Check runtimes lists
        assert.ok(observer.newRuntimesList,                         'HostObserver.newRuntimesList did not get set, when it should have been');
        assert.equal(observer.newRuntimesList.length,  1,           'HostObserver.newRuntimesList was not of expected size');
        assert.equal(observer.newRuntimesList[0].id, 'localhost,/opt/wlp', 'HostObserver.newRuntimesList[0].id was not of expected value');

        assert.ok(observer.oldRuntimesList,                         'HostObserver.oldRuntimesList did not get set, when it should have been');
        assert.equal(observer.oldRuntimesList.length,  1,           'HostObserver.oldRuntimesList was not empty');
        assert.equal(observer.oldRuntimesList[0].id, 'localhost,/wlp', 'HostObserver.oldRuntimesList[0].id was not of expected value');

        assert.ok(observer.addedRuntimes,                           'HostObserver.addedRuntimes did not get set, when it should have been');
        assert.equal(observer.addedRuntimes.length,    1,           'HostObserver.addedRuntimes was not of expected size');
        assert.equal(observer.addedRuntimes[0].id, 'localhost,/opt/wlp', 'HostObserver.addedRuntimes[0].id was not of expected value');

        assert.ok(observer.removedRuntimes,                         'HostObserver.removedRuntimes did not get set, when it should have been');
        assert.equal(observer.removedRuntimes.length,  1,           'HostObserver.removedRuntimes was not empty');
        assert.equal(observer.removedRuntimes[0].id, 'localhost,/wlp', 'HostObserver.removedRuntimes[0].id was not of expected value');

        // Check servers tallies
        assert.ok(observer.newServersTally,                         'HostObserver did not get the newServersTally object set');
        assert.equal(observer.newServersTally.up,      1,           'HostObserver did not get the correct new value for the Servers up tally');
        assert.equal(observer.newServersTally.down,    2,           'HostObserver did not get the correct new value for the Servers down tally');
        assert.equal(observer.newServersTally.unknown, 3,           'HostObserver did not get the correct new value for the Servers unknown tally');

        assert.ok(observer.oldServersTally,                         'HostObserver did not get the oldServersTally object set');
        assert.equal(observer.oldServersTally.up,      0,           'HostObserver did not get the correct old value for the Servers up tally');
        assert.equal(observer.oldServersTally.down,    0,           'HostObserver did not get the correct old value for the Servers down tally');
        assert.equal(observer.oldServersTally.unknown, 0,           'HostObserver did not get the correct old value for the Servers unknown tally');

        // Check affected state change
        assert.equal(observer.newState,        'PARTIALLY_STARTED', 'HostObserver did not get the correct NEW value for state');
        assert.equal(observer.oldState,                'STOPPED',   'HostObserver did not get the correct OLD value for state');

        // Check servers lists
        assert.ok(observer.newServersList,                          'HostObserver.newServersList did not get set, when it should have been');
        assert.equal(observer.newServersList.length,   1,           'HostObserver.newServersList was not of expected size');
        assert.equal(observer.newServersList[0], 'localhost,/wlp/usr,server2', 'HostObserver.newServersList[0] was not of expected value');

        assert.ok(observer.oldServersList,                          'HostObserver.oldServersList did not get set, when it should have been');
        assert.equal(observer.oldServersList.length,   1,           'HostObserver.oldServersList was not empty');
        assert.equal(observer.oldServersList[0], 'localhost,/wlp/usr,server1', 'HostObserver.oldServersList[0] was not of expected value');

        assert.ok(observer.addedServers,                            'HostObserver.addedServers did not get set, when it should have been');
        assert.equal(observer.addedServers.length,     1,           'HostObserver.addedServers was not of expected size');
        assert.equal(observer.addedServers[0], 'localhost,/wlp/usr,server2', 'HostObserver.addedServers[0] was not of expected value');

        assert.ok(observer.removedServers,                          'HostObserver.removedServers did not get set, when it should have been');
        assert.equal(observer.removedServers.length,   1,           'HostObserver.removedServers was not empty');
        assert.equal(observer.removedServers[0], 'localhost,/wlp/usr,server1', 'HostObserver.removedServers[0] was not of expected value');

        assert.equal(observer.newAlerts,            alerts,      'HostObserver did not get the correct NEW value for alerts');
        assert.notOk(observer.oldAlerts,                         'HostObserver did not get the correct OLD value for alerts');
      });

      tdd.test('handleChangeEvent - host was removed should trigger onDestroyed', function() {
        var host = new Host({id: 'localhost', runtimes: {list: []}, servers: {up: 0, down: 0, unknown: 0, list: []}});
        var observer = new HostObserver();

        host.subscribe(observer);

        // Simulate removal event
        host._handleChangeEvent({id: 'localhost', type: 'host', state: 'removed'});

        assert.equal(host.state, 'STOPPED', 'Host.state did not have the correct initialized value. It should not change in response to a "removed" event state');
        assert.ok(host.isDestroyed,       'Host.isDestroyed flag did not get set in response to a "removed" event');

        assert.ok(observer.isDestroyed,   'HostObserver did not get the isDestroyed flag set');
      });

      tdd.test('handleChangeEvent - unset or wrong event id or type is ignored', function() {
        var host = new Host({id: 'localhost', runtimes: {list: []}, servers: {up: 0, down: 0, unknown: 0, list: []}});

        // Simulate ignored events
        host._handleChangeEvent({type: 'host', up: 1, down: 2, unknown: 3, partial: 4, empty: 5});
        host._handleChangeEvent({id: 'wronghost', type: 'host', up: 1, down: 2, unknown: 3, partial: 4, empty: 5});
        host._handleChangeEvent({id: 'localhost', up: 1, down: 2, unknown: 3, partial: 4, empty: 5});
        host._handleChangeEvent({id: 'localhost', type: 'server', up: 1, down: 2, unknown: 3, partial: 4, empty: 5});

        assert.equal(host.id,               'localhost', 'Host.id did not have the correct initialized value');
        assert.equal(host.name,             'localhost', 'Host.name did not have the correct initialized value');
        assert.equal(host.state,            'STOPPED',   'Host.state did not have the correct initialized value');

        assert.ok(Array.isArray(host.runtimes.list),     'Host.runtimes.list was not an array');
        assert.equal(host.runtimes.list.length,  0,      'Host.runtimes.list.length was not initially empty');

        assert.equal(host.servers.up,       0,           'Host.servers.up did not have the correct initialized value');
        assert.equal(host.servers.down,     0,           'Host.servers.down did not have the correct initialized value');
        assert.equal(host.servers.unknown,  0,           'Host.servers.unknown did not have the correct initialized value');
        assert.ok(Array.isArray(host.servers.list),      'Host.servers.list was not an Array');
        assert.equal(host.servers.list.length, 0,        'Host.servers.list.length was not initially empty');
      });
    });
  }
});
