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
 * Test cases for Runtime
 */
define([ 
        "intern!tdd",
        "intern/chai!assert",
        "intern/dojo/declare",
        "resources/_objects/Host",
        "resources/_objects/Server",
        "resources/_derived/objects/Runtime",
        "resources/Observer",
        "dojo/i18n!nls/explorerMessages",
        "dojo/Deferred"
        ],
    function(tdd, assert, declare, Host, Server, Runtime, Observer, i18n, Deferred) {

    var RuntimeObserver = declare([Observer], {
      id: 'testObserver',
  
      onDestroyed: function() {
        this.destroyed = true;
      },
  
      onStateChange: function(newState, oldState) {
        this.newState = newState;
        this.oldState = oldState;
      },
  
      onServersTallyChange: function(newTally, oldTally) {
        this.newTally = newTally;
        this.oldTally = oldTally;
      },
  
      onServersListChange: function(newList, oldList, added, removed) {
        this.newList = newList;
        this.oldList = oldList;
        this.added = added;
        this.removed = removed;
      },
  
      onAlertsChange: function(newAlerts, oldAlerts) {
        this.newAlerts = newAlerts;
        this.oldAlerts = oldAlerts;
      }
    });

    with(assert) {
      

    /**
     * Defines the 'Runtime Object Tests' module test suite.
     */
    tdd.suite('Runtime Object Tests', function() {

         tdd.test('constructor - no initialization object', function() {
           try {
             new Runtime();
             assert.isTrue(false, 'Runtime was successfully created when it should have failed - an initialization object is required');
           } catch(error) {
             assert.equal(error, 'Runtime created without an initialization object', 'Error reported did not match expected error');
           }
         }),
  
         tdd.test('constructor - no path', function() {
           try {
             new Runtime({host: {/*...*/}, servers:[]});
             assert.isTrue(false, 'Runtime was successfully created when it should have failed - a path is required');
           } catch(error) {
             assert.equal(error, 'Runtime created without a path', 'Error reported did not match expected error');
           }
         }),
  
         tdd.test('constructor - no host', function() {
           try {
             new Runtime({path: '/wlp', servers:[]});
             assert.isTrue(false, 'Runtime was successfully created when it should have failed - a host is required');
           } catch(error) {
             assert.equal(error, 'Runtime created without a host', 'Error reported did not match expected error');
           }
         }),
  
         tdd.test('constructor - no servers', function() {
           try {
             new Runtime({host: {/*...*/}, path: '/wlp'});
             assert.isTrue(false, 'Runtime was successfully created when it should have failed - an array of Server is required');
           } catch(error) {
             assert.equal(error, 'Runtime created without an array of Server', 'Error reported did not match expected error');
           }
         }),
  
         tdd.test('constructor - host with empty runtimes', function() {
           var host = new Host({id: 'localhost', runtimes: {list: [{id:'/wlp', name:'/wlp', type:'runtime', runtimeType: "Liberty"}]}, servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
           var runtime = new Runtime({host: host, path: '/wlp', servers: [], runtimeType: "Liberty"});
  
           assert.equal(runtime.id,                  'localhost,/wlp', 'Runtime.id did not have the correct initialized value');
           assert.equal(runtime.name,                i18n.RUNTIME_LIBERTY, 'Runtime.name did not have the correct initialized value');
           assert.equal(runtime.state,               'STOPPED',        'Runtime.state did not have the correct initialized value');
           assert.equal(runtime.runtimeType,         'Liberty',        'Runtime.runtimeType did not have the correct initialized value');
           assert.equal(runtime.servers.up,          0,                'Runtime.servers.up not have the correct initialized value');
           assert.equal(runtime.servers.down,        0,                'Runtime.servers.down not have the correct initialized value');
           assert.equal(runtime.servers.unknown,     0,                'Runtime.servers.unknown not have the correct initialized value');
           assert.isTrue(Array.isArray(runtime.servers.list),            'Runtime.servers.list was not an Array');
           assert.equal(runtime.servers.list.length, 0,                'Runtime.servers.list.length was not initially empty');
           assert.notOk(runtime.alerts,                                'Runtime.alerts should not be set as there are no servers');
         }),
  
         tdd.test('constructor - host with runtimes with running server', function() {
           var host = new Host({id: 'localhost', runtimes: {list: [{id:'/wlp', name:'/wlp', type:'runtime', runtimeType: "Liberty"}]}, servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
           var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', apps: {up: 0, down: 0, unknown: 0, list: []}});
           var runtime = new Runtime({host: host, path: '/wlp', servers: [server], runtimeType: "Liberty"});
  
           assert.equal(runtime.id,                  'localhost,/wlp', 'Runtime.id did not have the correct initialized value');
           assert.equal(runtime.name,                i18n.RUNTIME_LIBERTY, 'Runtime.name did not have the correct initialized value');
           assert.equal(runtime.state,               'STARTED',        'Runtime.state did not have the correct initialized value');
           assert.equal(runtime.runtimeType,         'Liberty',        'Runtime.runtimeType did not have the correct initialized value');
           assert.equal(runtime.servers.up,          1,                'Runtime.servers.up not have the correct initialized value');
           assert.equal(runtime.servers.down,        0,                'Runtime.servers.down not have the correct initialized value');
           assert.equal(runtime.servers.unknown,     0,                'Runtime.servers.unknown not have the correct initialized value');
           assert.isTrue(Array.isArray(runtime.servers.list),            'Runtime.servers.list was not an Array');
           assert.equal(runtime.servers.list.length, 1,                'Runtime.servers.list.length was not initially empty');
           assert.equal(runtime.servers.list[0],     server,           'Runtime.servers.list[0] was not correct');
           assert.notOk(runtime.alerts,                                'Runtime.alerts should not be set as there are no servers with alerts');
           assert.notOk(runtime.containerType,                         'Runtime.containerType should not be set as it was not specified');
         }),
  
         tdd.test('constructor - host with runtimes with stopped server', function() {
           var host = new Host({id: 'localhost', runtimes: {list: [{id:'/wlp', name:'/wlp', type:'runtime', runtimeType: "Liberty"}]}, servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
           var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STOPPED', apps: {up: 0, down: 0, unknown: 0, list: []}});
           var runtime = new Runtime({host: host, path: '/wlp', servers: [server], runtimeType: "Liberty"});
  
           assert.equal(runtime.id,                  'localhost,/wlp', 'Runtime.id did not have the correct initialized value');
           assert.equal(runtime.name,                i18n.RUNTIME_LIBERTY,  'Runtime.name did not have the correct initialized value');
           assert.equal(runtime.state,               'STOPPED',        'Runtime.state did not have the correct initialized value');
           assert.equal(runtime.runtimeType,         'Liberty',        'Runtime.runtimeType did not have the correct initialized value');
           assert.equal(runtime.servers.up,          0,                'Runtime.servers.up not have the correct initialized value');
           assert.equal(runtime.servers.down,        1,                'Runtime.servers.down not have the correct initialized value');
           assert.equal(runtime.servers.unknown,     0,                'Runtime.servers.unknown not have the correct initialized value');
           assert.isTrue(Array.isArray(runtime.servers.list),            'Runtime.servers.list was not an Array');
           assert.equal(runtime.servers.list.length, 1,                'Runtime.servers.list.length was not initially empty');
           assert.equal(runtime.servers.list[0],     server,           'Runtime.servers.list[0] was not correct');
           assert.notOk(runtime.alerts,                                'Runtime.alerts should not be set as there are no servers with alerts');
         }),
  
         tdd.test('constructor - host with runtimes with multiple servers (one with alerts)', function() {
           var host = new Host({id: 'localhost', runtimes: {list: [{id:'/wlp', name:'/wlp', type:'runtime', runtimeType: "Liberty"}]}, servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
           var server1 = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', apps: {up: 0, down: 0, unknown: 0, list: []}});
           var server2 = new Server({id: 'localhost,/wlp/usr,server2', wlpInstallDir: '/wlp', state: 'STOPPED', apps: {up: 0, down: 0, unknown: 0, list: []}});
           var server3 = new Server({id: 'localhost,/wlp/usr,server3', wlpInstallDir: '/wlp', state: 'UNKNOWN', apps: {up: 0, down: 0, unknown: 0, list: []},
             alerts: { count: 1, unknown: [{id: 'localhost,/wlp/usr,server3', type: 'server'}]}});
           var runtime = new Runtime({host: host, path: '/wlp', servers: [server1, server2, server3], runtimeType: "Liberty"});
  
           assert.equal(runtime.id,                  'localhost,/wlp', 'Runtime.id did not have the correct initialized value');
           assert.equal(runtime.name,                i18n.RUNTIME_LIBERTY, 'Runtime.name did not have the correct initialized value');
           assert.equal(runtime.state,            'PARTIALLY_STARTED', 'Runtime.state did not have the correct initialized value');
           assert.equal(runtime.runtimeType,         'Liberty',        'Runtime.runtimeType did not have the correct initialized value');
           assert.equal(runtime.servers.up,          1,                'Runtime.servers.up not have the correct initialized value');
           assert.equal(runtime.servers.down,        1,                'Runtime.servers.down not have the correct initialized value');
           assert.equal(runtime.servers.unknown,     1,                'Runtime.servers.unknown not have the correct initialized value');
           assert.isTrue(Array.isArray(runtime.servers.list),            'Runtime.servers.list was not an Array');
           assert.equal(runtime.servers.list.length, 3,                'Runtime.servers.list.length was not initially empty');
           assert.equal(runtime.servers.list[0],     server1,          'Runtime.servers.list[0] was not correct');
           assert.equal(runtime.servers.list[1],     server2,          'Runtime.servers.list[1] was not correct');
           assert.equal(runtime.servers.list[2],     server3,          'Runtime.servers.list[2] was not correct');
  
           assert.isNotNull(runtime.alerts,                                 'Runtime.alerts should have been set as there are servers with alerts');
           assert.equal(runtime.alerts.count,                       1, 'Runtime.alerts.count was not correct');
           assert.equal(runtime.alerts.unknown.length,              1, 'Runtime.alerts.unknown.length was not correct');
           assert.equal(runtime.alerts.unknown[0].id, 'localhost,/wlp/usr,server3', 'Runtime.alerts.unknown[0].id was not correct');
           assert.equal(runtime.alerts.unknown[0].type,   'server',    'Runtime.alerts.unknown[0].type was not correct');
         }),
  
         tdd.test('constructor - host with runtimes with multiple servers with multiple alerts', function() {
           // The app alerts don't match the server's apps, but that doesn't matter for the purposes of this test
           var host = new Host({id: 'localhost', runtimes: {list: [{id:'/wlp', name:'/wlp', type:'runtime', runtimeType: "Liberty"}]}, servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
           var server1 = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', apps: {up: 0, down: 0, unknown: 0, list: []}, 
             alerts: { count: 2, app: [{name: 'localhost,/wlp/usr,server1,Website', servers: ['localhost,/wlp/usr,server1']}, {name: 'localhost,/wlp/usr,server1,Testing', servers: ['localhost,/wlp/usr,server1']} ]} });
  
           var server2 = new Server({id: 'localhost,/wlp/usr,server2', wlpInstallDir: '/wlp', state: 'UNKNOWN', apps: {up: 0, down: 0, unknown: 0, list: []}, 
             alerts: { count: 2, unknown: [{id: 'localhost,/wlp/usr,server2', type: 'server'}], app: [{name: 'localhost,/wlp/usr,server2,Website', servers: ['localhost,/wlp/usr,server2']}] } });
  
           var server3 = new Server({id: 'localhost,/wlp/usr,server3', wlpInstallDir: '/wlp', state: 'UNKNOWN', apps: {up: 0, down: 0, unknown: 0, list: []}, 
             alerts: { count: 1, unknown: [{id: 'localhost,/wlp/usr,server3', type: 'server'}] } });
  
           var runtime = new Runtime({host: host, path: '/wlp', servers: [server1, server2, server3], runtimeType: "Liberty"});
  
           assert.equal(runtime.id,                  'localhost,/wlp', 'Runtime.id did not have the correct initialized value');
           assert.equal(runtime.name,                i18n.RUNTIME_LIBERTY, 'Runtime.name did not have the correct initialized value');
           assert.equal(runtime.state,            'PARTIALLY_STARTED', 'Runtime.state did not have the correct initialized value');
           assert.equal(runtime.runtimeType,         'Liberty',        'Runtime.runtimeType did not have the correct initialized value');
           assert.equal(runtime.servers.up,          1,                'Runtime.servers.up not have the correct initialized value');
           assert.equal(runtime.servers.down,        0,                'Runtime.servers.down not have the correct initialized value');
           assert.equal(runtime.servers.unknown,     2,                'Runtime.servers.unknown not have the correct initialized value');
           assert.isTrue(Array.isArray(runtime.servers.list),            'Runtime.servers.list was not an Array');
           assert.equal(runtime.servers.list.length, 3,                'Runtime.servers.list.length was not initially empty');
           assert.equal(runtime.servers.list[0],     server1,          'Runtime.servers.list[0] was not correct');
           assert.equal(runtime.servers.list[1],     server2,          'Runtime.servers.list[1] was not correct');
           assert.equal(runtime.servers.list[2],     server3,          'Runtime.servers.list[2] was not correct');
  
           // When there are multiple alerts, just add them together because there can be no duplicates across servers
           assert.isNotNull(runtime.alerts,                                 'Runtime.alerts should have been set as there are servers with alerts');
           assert.equal(runtime.alerts.count,                       5, 'Runtime.alerts.count was not correct - it should only be 4: 2 unknown servers and 2 apps with instances not running. The app stopped on 2 servers is not counted twice');
           assert.equal(runtime.alerts.unknown.length,              2, 'Runtime.alerts.unknown.length was not correct');
           assert.equal(runtime.alerts.unknown[0].id, 'localhost,/wlp/usr,server2', 'Runtime.alerts.unknown[0].id was not correct');
           assert.equal(runtime.alerts.unknown[0].type,   'server',    'Runtime.alerts.unknown[0].type was not correct');
           assert.equal(runtime.alerts.unknown[1].id, 'localhost,/wlp/usr,server3', 'Runtime.alerts.unknown[0].id was not correct');
           assert.equal(runtime.alerts.unknown[1].type,   'server',    'Runtime.alerts.unknown[1].type was not correct');
  
           assert.equal(runtime.alerts.app.length,                  3, 'Runtime.alerts.app.length was not correct');
           for (var i = 0; i < runtime.alerts.app.length; i++) {
             var appAlert = runtime.alerts.app[i];
             if (appAlert.name === 'localhost,/wlp/usr,server1,Testing') {
               // Testing is only on 1 server
               assert.equal(appAlert.name,   'localhost,/wlp/usr,server1,Testing',  'Testing appAlert.name was not correct');
               assert.equal(appAlert.servers.length,       1,          'Testing appAlert.servers.length was not correct');
               assert.equal(appAlert.servers[0], 'localhost,/wlp/usr,server1', 'Testing appAlert.servers[0] was not correct');
             } else if (appAlert.name === 'localhost,/wlp/usr,server1,Website') {
               // Website is on 2 servers.  This checks for the one on server1.
               assert.equal(appAlert.name,   'localhost,/wlp/usr,server1,Website',  'Website appAlert.name was not correct');
               assert.equal(appAlert.servers.length,       1,          'Website appAlert.servers.length was not correct');
               if (appAlert.servers[0] === 'localhost,/wlp/usr,server1') {
                 // Pass
               } else {
                 assert.isTrue(false, 'Runtime.alerts.app.servers had an unexpected element for localhost,/wlp/usr,server1,Website!');
               }
             } else if (appAlert.name === 'localhost,/wlp/usr,server2,Website') {
               // Website is on 2 servers.  This checks for the one on server1.
               assert.equal(appAlert.name,   'localhost,/wlp/usr,server2,Website',  'Website appAlert.name was not correct');
               assert.equal(appAlert.servers.length,       1,          'Website appAlert.servers.length was not correct');
               if (appAlert.servers[0] === 'localhost,/wlp/usr,server2') {
                 // Pass
               } else {
                 assert.isTrue(false, 'Runtime.alerts.app.servers had an unexpected element for localhost,/wlp/usr,server2,Website!');
               }
             } else {
               assert.isTrue(false, 'Runtime.alerts.app had an unexpected element!');  
             }
           }
         }),
  
         tdd.test('constructor - host with runtimes with ignored servers', function() {
           var host = new Host({id: 'localhost', runtimes: {list: [{id:'/wlp', name:'/wlp', type:'runtime', runtimeType: "Liberty"}]}, servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
           var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp2', state: 'STARTED', apps: {up: 0, down: 0, unknown: 0, list: []}});
           var runtime = new Runtime({host: host, path: '/wlp', servers: [server], runtimeType: "Liberty"});
  
           assert.equal(runtime.id,                  'localhost,/wlp', 'Runtime.id did not have the correct initialized value');
           assert.equal(runtime.name,                i18n.RUNTIME_LIBERTY, 'Runtime.name did not have the correct initialized value');
           assert.equal(runtime.state,               'STOPPED',        'Runtime.state did not have the correct initialized value');
           assert.equal(runtime.runtimeType,         'Liberty',        'Runtime.runtimeType did not have the correct initialized value');
           assert.equal(runtime.servers.up,          0,                'Runtime.servers.up not have the correct initialized value');
           assert.equal(runtime.servers.down,        0,                'Runtime.servers.down not have the correct initialized value');
           assert.equal(runtime.servers.unknown,     0,                'Runtime.servers.unknown not have the correct initialized value');
           assert.isTrue(Array.isArray(runtime.servers.list),            'Runtime.servers.list was not an Array');
           assert.equal(runtime.servers.list.length, 0,                'Runtime.servers.list.length was not initially empty');
         }),
  
         tdd.test('Host changes - runtime removed', function() {
           var host = new Host({id: 'localhost', runtimes: {list: [{id:'/wlp', name:'/wlp', type:'runtime', runtimeType: "Liberty"}]}, servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
           var runtime = new Runtime({host: host, path: '/wlp', servers: [], runtimeType: "Liberty"});
           var observer = new RuntimeObserver();
  
           runtime.subscribe(observer);
  
           host._handleChangeEvent({type: 'host', id: 'localhost', runtimes: { removed: [{id: 'localhost,/wlp', name: 'localhost,/wlp', type: 'runtime'}] } });
  
           assert.equal(runtime.state, 'STOPPED', 'Runtime.state did not have the correct initialized value. It should not change in response to a "removed" event state');
           assert.isTrue(runtime.isDestroyed,       'Runtime.isDestroyed flag did not get set in response to a "removed" event');
  
           assert.isTrue(observer.destroyed,        'RuntimeObserver.onDestroyed was not called');
         }),
  
         tdd.test('Host changes - host removed from the collective', function() {
           var host = new Host({id: 'localhost', runtimes: {list: [{id:'/wlp', name:'/wlp', type:'runtime', runtimeType: "Liberty"}]}, servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
           var runtime = new Runtime({host: host, path: '/wlp', servers: [], runtimeType: "Liberty"});
           var observer = new RuntimeObserver();
  
           runtime.subscribe(observer);
  
           host._handleChangeEvent({type: 'host', id: 'localhost', state: 'removed'});
  
           assert.equal(runtime.state, 'STOPPED', 'Runtime.state did not have the correct initialized value. It should not change in response to a "removed" event state');
           assert.isTrue(runtime.isDestroyed,       'Runtime.isDestroyed flag did not get set in response to a "removed" event');
  
           assert.isTrue(observer.destroyed,        'RuntimeObserver.onDestroyed was not called');
         }),
  
         tdd.test('Host changes - server added to empty runtime', function() {
           var host = new Host({id: 'localhost', runtimes: {list: [{id:'/wlp', name:'/wlp', type:'runtime', runtimeType: "Liberty"}]}, servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
           var runtime = new Runtime({host: host, path: '/wlp', servers: [], runtimeType: "Liberty"});
           var observer = new RuntimeObserver();
  
           runtime.subscribe(observer);
  
           assert.equal(runtime.__serverObservers.length,         0,   'Runtime.__serverObservers.length did not start with the correct size');
  
           var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', apps: {up: 0, down: 0, unknown: 0, list: []}});
           runtime.resourceManager = {
               getServer: function(list) {
                 var deferred = new Deferred();
                 deferred.resolve([server], true);
                 return deferred;
               }
           };
  
           var dfd = this.async(1000);
  
           // Need to wait until the onListChange method fires before we 
           observer.onServersListChange = function(newList, oldList, added, removed) {
             try {
               assert.equal(runtime.state,           'STARTED',   'Runtime.state did not have the correct updated value');
               assert.equal(runtime.servers.up,      1,           'Runtime.up did not have the correct updated value');
               assert.equal(runtime.servers.down,    0,           'Runtime.down did not have the correct updated value');
               assert.equal(runtime.servers.unknown, 0,           'Runtime.unknown did not have the correct updated value');
               assert.isTrue(Array.isArray(runtime.servers.list),   'Runtime.list was not an Array');
               assert.equal(runtime.servers.list.length, 1,           'Runtime.list.length was not the correct updated value');
               assert.equal(runtime.servers.list[0].id, 'localhost,/wlp/usr,server1', 'Runtime.list[0].id did not have the correct updated value for the object name');
  
               // Validate the Observer was passed the correct tally objects after the first event
               assert.isNotNull(observer.newTally,             'RuntimeObserver.newTally did not get set, when it should have been');
               assert.equal(1, observer.newTally.up,      'RuntimeObserver did not get the correct new value for the up tally');
               assert.equal(0, observer.newTally.down,    'RuntimeObserver did not get the correct new value for the down tally');
               assert.equal(0, observer.newTally.unknown, 'RuntimeObserver did not get the correct new value for the unknown tally');
  
               assert.isNotNull(observer.oldTally,             'RuntimeObserver.oldTally did not get set, when it should have been');
               assert.equal(observer.oldTally.up,      0, 'RuntimeObserver did not get the correct old value for the up tally');
               assert.equal(observer.oldTally.down,    0, 'RuntimeObserver did not get the correct old value for the down tally');
               assert.equal(observer.oldTally.unknown, 0, 'RuntimeObserver did not get the correct old value for the unknown tally');
  
               assert.equal(observer.newState, 'STARTED', 'RuntimeObserver did not get the correct NEW value for state');
               assert.equal(observer.oldState, 'STOPPED', 'RuntimeObserver did not get the correct OLD value for state');
  
               assert.isNotNull(newList,                       'RuntimeObserver.newList did not get set, when it should have been');
               assert.equal(newList.length,            1, 'RuntimeObserver.newList was not of expected size');
               assert.equal(newList[0].id, 'localhost,/wlp/usr,server1', 'RuntimeObserver.newList[0].id was not of expected value');
               assert.isNotNull(oldList,                       'RuntimeObserver.oldList did not get set, when it should have been');
               assert.equal(oldList.length,            0, 'RuntimeObserver.oldList was not empty');
               assert.isNotNull(added,                         'RuntimeObserver.added did not get set, when it should have been');
               assert.equal(added.length,              1, 'RuntimeObserver.added was not of expected size');
               assert.equal(added[0], 'localhost,/wlp/usr,server1', 'RuntimeObserver.added[0] was not of expected value');
               assert.notOk(removed,             'RuntimeObserver.removed got set when it should not have been');
  
               assert.equal(runtime.__serverObservers.length, 1, 'Runtime.__serverObservers.length did not update to the correct size');
  
               dfd.resolve('OK');
             } catch(err) {
               dfd.reject(err);
             }
           };
  
           // Trigger the server's onAppsTallyChange and onAppsListChange method via the Server event handler
           host._handleChangeEvent({type: 'host', id: 'localhost', servers: {up: 1, down: 0, unknown: 0, added: ['localhost,/wlp/usr,server1']}});
  
           return dfd;
         }),
  
         tdd.test('Host changes - server added to other runtime', function() {
           var host = new Host({id: 'localhost', runtimes: {list: [{id:'/wlp', name:'/wlp', type:'runtime', runtimeType: "Liberty"}]}, servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
           var runtime = new Runtime({host: host, path: '/wlp', servers: [], runtimeType: "Liberty"});
           var observer = new RuntimeObserver();
  
           runtime.subscribe(observer);
  
           var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/opt/wlp', state: 'STARTED', apps: {up: 0, down: 0, unknown: 0, list: []}});
           runtime.resourceManager = {
               getServer: function(list) {
                 var deferred = new Deferred();
                 deferred.resolve([server], true);
                 return deferred;
               }
           };
  
           // Trigger the server's onAppsTallyChange and onAppsListChange method via the Server event handler
           host._handleChangeEvent({type: 'host', id: 'localhost', servers: {up: 1, down: 0, unknown: 0, added: ['localhost,/wlp/usr,server1']}});
  
           assert.equal(runtime.state,           'STOPPED',   'Runtime.state did not have the correct updated value');
           assert.equal(runtime.servers.up,      0,           'Runtime.up did not have the correct updated value');
           assert.equal(runtime.servers.down,    0,           'Runtime.down did not have the correct updated value');
           assert.equal(runtime.servers.unknown, 0,           'Runtime.unknown did not have the correct updated value');
           assert.isTrue(Array.isArray(runtime.servers.list),   'Runtime.list was not an Array');
           assert.equal(runtime.servers.list.length, 0,           'Runtime.list.length was not the correct updated value');
         }),
  
         tdd.test('Host changes - server removed from runtime', function() {
           var host = new Host({id: 'localhost', runtimes: {list: [{id:'/wlp', name:'/wlp', type:'runtime', runtimeType: "Liberty"}]}, servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
           var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', apps: {up: 0, down: 0, unknown: 0, list: []}});
           var runtime = new Runtime({host: host, path: '/wlp', servers: [server], runtimeType: "Liberty"});
           var observer = new RuntimeObserver();
  
           runtime.subscribe(observer);
  
           // Trigger the server's onAppsTallyChange and onAppsListChange method via the Server event handler
           host._handleChangeEvent({type: 'host', id: 'localhost', servers: {up: 0, down: 0, unknown: 0, removed: ['localhost,/wlp/usr,server1']}});
  
           assert.equal(runtime.state,           'STOPPED',   'Runtime.state did not have the correct updated value');
           assert.equal(runtime.servers.up,      0,           'Runtime.up did not have the correct updated value');
           assert.equal(runtime.servers.down,    0,           'Runtime.down did not have the correct updated value');
           assert.equal(runtime.servers.unknown, 0,           'Runtime.unknown did not have the correct updated value');
           assert.isTrue(Array.isArray(runtime.servers.list),   'Runtime.list was not an Array');
           assert.equal(runtime.servers.list.length, 0,           'Runtime.list.length was not the correct updated value');
  
           // Validate the Observer was passed the correct tally objects after the first event
           assert.isNotNull(observer.newTally,             'RuntimeObserver.newTally did not get set, when it should have been');
           assert.equal(observer.newTally.up,      0, 'RuntimeObserver did not get the correct new value for the up tally');
           assert.equal(observer.newTally.down,    0, 'RuntimeObserver did not get the correct new value for the down tally');
           assert.equal(observer.newTally.unknown, 0, 'RuntimeObserver did not get the correct new value for the unknown tally');
  
           assert.isNotNull(observer.oldTally,             'RuntimeObserver.oldTally did not get set, when it should have been');
           assert.equal(observer.oldTally.up,      1, 'RuntimeObserver did not get the correct old value for the up tally');
           assert.equal(observer.oldTally.down,    0, 'RuntimeObserver did not get the correct old value for the down tally');
           assert.equal(observer.oldTally.unknown, 0, 'RuntimeObserver did not get the correct old value for the unknown tally');
  
           // Check Observer state change
           assert.equal(observer.newState, 'STOPPED', 'RuntimeObserver did not get the correct NEW value for state');
           assert.equal(observer.oldState, 'STARTED', 'RuntimeObserver did not get the correct OLD value for state');
  
           assert.isNotNull(observer.newList,              'RuntimeObserver.newList did not get set, when it should have been');
           assert.equal(observer.newList.length,   0, 'RuntimeObserver.newList was not of expected size');
           assert.isNotNull(observer.oldList,              'RuntimeObserver.oldList did not get set, when it should have been');
           assert.equal(observer.oldList.length,   1, 'RuntimeObserver.oldList was not empty');
           assert.equal(observer.oldList[0].id, 'localhost,/wlp/usr,server1', 'RuntimeObserver.oldList[0].id was not of expected value');
           assert.notOk(observer.added,               'RuntimeObserver.added did not get set, when it should have been');
           assert.isNotNull(observer.removed,              'RuntimeObserver.removed got set when it should not have been');
           assert.equal(observer.removed.length,   1, 'RuntimeObserver.removed was not of expected size');
           assert.equal(observer.removed[0], 'localhost,/wlp/usr,server1', 'RuntimeObserver.removed[0] was not of expected value');
         }),
  
         tdd.test('Host changes - server removed which is not in runtime', function() {
           var host = new Host({id: 'localhost', runtimes: {list: [{id:'/wlp', name:'/wlp', type:'runtime', runtimeType: "Liberty"}]}, servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
           var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', apps: {up: 0, down: 0, unknown: 0, list: []}});
           var runtime = new Runtime({host: host, path: '/wlp', servers: [server], runtimeType: "Liberty"});
           var observer = new RuntimeObserver();
  
           runtime.subscribe(observer);
  
           // Trigger the server's onAppsTallyChange and onAppsListChange method via the Server event handler
           host._handleChangeEvent({type: 'host', id: 'localhost', servers: {up: 0, down: 0, unknown: 0, removed: ['localhost,/wlp/usr,server2']}});
  
           assert.equal(runtime.id,                  'localhost,/wlp', 'Runtime.id did not have the correct initialized value');
           assert.equal(runtime.name,                i18n.RUNTIME_LIBERTY, 'Runtime.name did not have the correct initialized value');
           assert.equal(runtime.state,               'STARTED',        'Runtime.state did not have the correct initialized value');
           assert.equal(runtime.runtimeType,         'Liberty',        'Runtime.runtimeType did not have the correct initialized value');
           assert.equal(runtime.servers.up,          1,                'Runtime.servers.up not have the correct initialized value');
           assert.equal(runtime.servers.down,        0,                'Runtime.servers.down not have the correct initialized value');
           assert.equal(runtime.servers.unknown,     0,                'Runtime.servers.unknown not have the correct initialized value');
           assert.isTrue(Array.isArray(runtime.servers.list),            'Runtime.servers.list was not an Array');
           assert.equal(runtime.servers.list.length, 1,                'Runtime.servers.list.length was not initially empty');
           assert.equal(runtime.servers.list[0],     server,           'Runtime.servers.list[0] was not correct');
  
           // Validate the Observer was passed the correct tally objects after the first event
           assert.notOk(observer.newTally,             'RuntimeObserver.newTally should not be set, no event hould have fired when the removed servers are not in the list');
           assert.notOk(observer.oldTally,             'RuntimeObserver.oldTally should not be set, no event hould have fired when the removed servers are not in the listn');
           assert.notOk(observer.newList,              'RuntimeObserver.newList should not be set, no event hould have fired when the removed servers are not in the listn');
           assert.notOk(observer.oldList,              'RuntimeObserver.oldList should not be set, no event hould have fired when the removed servers are not in the listn');
           assert.notOk(observer.added,                'RuntimeObserver.added should not be set, no event hould have fired when the removed servers are not in the listn');
           assert.notOk(observer.removed,              'RuntimeObserver.removed should not be set, no event hould have fired when the removed servers are not in the list');
         }),
  
         tdd.test('Server on runtime changes - state changes', function() {
           var host = new Host({id: 'localhost', runtimes: {list: [{id:'/wlp', name:'/wlp', type:'runtime', runtimeType: "Liberty"}]}, servers: {up: 0, down: 1, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
           var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STOPPED', apps: {up: 0, down: 0, unknown: 0, list: []}});
           var runtime = new Runtime({host: host, path: '/wlp', servers: [server], runtimeType: "Liberty"});
           var observer = new RuntimeObserver();
  
           runtime.subscribe(observer);
  
           assert.equal(runtime.state,              'STOPPED',        'Runtime.state did not have the correct initialized value');
           assert.equal(runtime.servers.up,          0,                'Runtime.servers.up did not have the correct initialized value');
           assert.equal(runtime.servers.down,        1,                'Runtime.servers.down did not have the correct initialized value');
           assert.equal(runtime.servers.unknown,     0,                'Runtime.servers.unknown did not have the correct initialized value');
  
           server._handleChangeEvent({type: 'server', id: 'localhost,/wlp/usr,server1', state: 'STARTED' });
  
           assert.equal(runtime.id,                  'localhost,/wlp', 'Runtime.id did not have the correct initialized value');
           assert.equal(runtime.name,                i18n.RUNTIME_LIBERTY,  'Runtime.name did not have the correct initialized value');
           assert.equal(runtime.state,               'STARTED',        'Runtime.state did not have the correct initialized value');
           assert.equal(runtime.runtimeType,         'Liberty',        'Runtime.runtimeType did not have the correct initialized value');
           assert.equal(runtime.servers.up,          1,                'Runtime.servers.up did not have the correct updated value');
           assert.equal(runtime.servers.down,        0,                'Runtime.servers.down did not have the correct updated value');
           assert.equal(runtime.servers.unknown,     0,                'Runtime.servers.unknown did not have the correct updated value');
           assert.isTrue(Array.isArray(runtime.servers.list),            'Runtime.servers.list was not an Array');
           assert.equal(runtime.servers.list.length, 1,                'Runtime.servers.list.length was not initially empty');
           assert.equal(runtime.servers.list[0],     server,           'Runtime.servers.list[0] was not correct');
  
           // Check Observer state change
           assert.equal(observer.newState,           'STARTED',        'RuntimeObserver did not get the correct NEW value for state');
           assert.equal(observer.oldState,           'STOPPED',        'RuntimeObserver did not get the correct OLD value for state');
         }),
  
         tdd.test('Server on runtime changes - multiple servers with multiple state changes', function() {
           var host = new Host({id: 'localhost', runtimes: {list: [{id:'/wlp', name:'/wlp', type:'runtime', runtimeType: "Liberty"}]}, servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
           var server1 = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', apps: {up: 0, down: 0, unknown: 0, list: []}});
           var server2 = new Server({id: 'localhost,/wlp/usr,server2', wlpInstallDir: '/wlp', state: 'STOPPED', apps: {up: 0, down: 0, unknown: 0, list: []}});
           var server3 = new Server({id: 'localhost,/wlp/usr,server3', wlpInstallDir: '/wlp', state: 'UNKNOWN', apps: {up: 0, down: 0, unknown: 0, list: []}});
           var runtime = new Runtime({host: host, path: '/wlp', servers: [server1, server2, server3], runtimeType: "Liberty"});
           var observer = new RuntimeObserver();
  
           runtime.subscribe(observer);
  
           // Initial sanity check
           assert.equal(runtime.id,                  'localhost,/wlp', 'Runtime.id did not have the correct initialized value');
           assert.equal(runtime.name,                i18n.RUNTIME_LIBERTY, 'Runtime.name did not have the correct initialized value');
           assert.equal(runtime.state,           'PARTIALLY_STARTED',  'Runtime.state did not have the correct initialized value');
           assert.equal(runtime.runtimeType,         'Liberty',        'Runtime.runtimeType did not have the correct initialized value');
           assert.equal(runtime.servers.up,          1,                'Runtime.servers.up not have the correct initialized value');
           assert.equal(runtime.servers.down,        1,                'Runtime.servers.down not have the correct initialized value');
           assert.equal(runtime.servers.unknown,     1,                'Runtime.servers.unknown not have the correct initialized value');
           assert.isTrue(Array.isArray(runtime.servers.list),            'Runtime.servers.list was not an Array');
           assert.equal(runtime.servers.list.length, 3,                'Runtime.servers.list.length was not initially empty');
           assert.equal(runtime.servers.list[0],     server1,          'Runtime.servers.list[0] was not correct');
           assert.equal(runtime.servers.list[1],     server2,          'Runtime.servers.list[1] was not correct');
           assert.equal(runtime.servers.list[2],     server3,          'Runtime.servers.list[2] was not correct');
  
           // Fire multiple server state changes, set all to started
           server1._handleChangeEvent({type: 'server', id: 'localhost,/wlp/usr,server1', state: 'STARTED' });
           server2._handleChangeEvent({type: 'server', id: 'localhost,/wlp/usr,server2', state: 'STARTED' });
           server3._handleChangeEvent({type: 'server', id: 'localhost,/wlp/usr,server3', state: 'STARTED' });
  
           assert.equal(runtime.id,                  'localhost,/wlp', 'Runtime.id did not have the correct initialized value');
           assert.equal(runtime.name,                i18n.RUNTIME_LIBERTY, 'Runtime.name did not have the correct initialized value');
           assert.equal(runtime.state,               'STARTED',        'Runtime.state did not have the correct initialized value');
           assert.equal(runtime.runtimeType,         'Liberty',        'Runtime.runtimeType did not have the correct initialized value');
           assert.equal(runtime.servers.up,          3,                'Runtime.servers.up did not have the correct updated value');
           assert.equal(runtime.servers.down,        0,                'Runtime.servers.down did not have the correct updated value');
           assert.equal(runtime.servers.unknown,     0,                'Runtime.servers.unknown did not have the correct updated value');
  
           // Check Observer state change
           assert.equal(observer.newState,           'STARTED',        'RuntimeObserver did not get the correct NEW value for state');
           assert.equal(observer.oldState,        'PARTIALLY_STARTED', 'RuntimeObserver did not get the correct OLD value for state');
         }),
  
         tdd.test('Server removed - disconnected from observers', function() {
           var host = new Host({id: 'localhost', runtimes: {list: [{id:'/wlp', name:'/wlp', type:'runtime', runtimeType: "Liberty"}]}, servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
           var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STOPPED', apps: {up: 0, down: 0, unknown: 0, list: []}});
           var runtime = new Runtime({host: host, path: '/wlp', servers: [server], runtimeType: "Liberty"});
           var observer = new RuntimeObserver();
  
           runtime.subscribe(observer);
  
           assert.equal(runtime.__serverObservers.length,         1,   'Runtime.__serverObservers.length did not start with the correct size');
  
           host._handleChangeEvent({type: 'host', id: 'localhost', servers: {up: 0, down: 0, unknown: 0, removed: ['localhost,/wlp/usr,server1']}});
  
           assert.equal(runtime.__serverObservers.length,         0,   'Runtime.__serverObservers.length did not update to the correct size');
         }),
  
         tdd.test('Host changes - server with alerts added with to runtime', function() {
           var host = new Host({id: 'localhost', runtimes: {list: [{id:'/wlp', name:'/wlp', type:'runtime', runtimeType: "Liberty"}]}, servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
           var runtime = new Runtime({host: host, path: '/wlp', servers: [], runtimeType: "Liberty"});
           var observer = new RuntimeObserver();
  
           runtime.subscribe(observer);
  
           var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'UNKNOWN', apps: {up: 0, down: 0, unknown: 0, list: []},
             alerts: { count: 1, unknown: [{id: 'localhost,/wlp/usr,server1', type: 'server'}]} });
           runtime.resourceManager = {
               getServer: function(list) {
                 var deferred = new Deferred();
                 deferred.resolve([server], true);
                 return deferred;
               }
           };
  
           var dfd = this.async(1000);
  
           // Need to wait until the onAlertsChange method fires before we validate
           observer.onAlertsChange = function(newAlerts, oldAlerts) {
             try {
               assert.isNotNull(runtime.alerts,                                 'Runtime.alerts should have been set as there was a server with alerts added');
               assert.equal(runtime.alerts.count,                       1, 'Runtime.alerts.count was not correct');
               assert.equal(runtime.alerts.unknown.length,              1, 'Runtime.alerts.unknown.length was not correct');
               assert.equal(runtime.alerts.unknown[0].id, 'localhost,/wlp/usr,server1', 'Runtime.alerts.unknown[0].id was not correct');
               assert.equal(runtime.alerts.unknown[0].type,   'server',    'Runtime.alerts.unknown[0].type was not correct');
  
               // Validate the Observer was passed the correct tally objects after the first event
               assert.isNotNull(newAlerts,                                      'RuntimeObserver.newAlerts should have been set as there was a server with alerts added');
               assert.equal(newAlerts.count,                            1, 'RuntimeObserver.newAlerts.count was not correct');
               assert.equal(newAlerts.unknown.length,                   1, 'RuntimeObserver.newAlerts.unknown.length was not correct');
               assert.equal(newAlerts.unknown[0].id, 'localhost,/wlp/usr,server1', 'RuntimeObserver.newAlerts.unknown[0].id was not correct');
               assert.equal(newAlerts.unknown[0].type,          'server',  'RuntimeObserver.newAlerts.unknown[0].type was not correct');
  
               assert.notOk(oldAlerts,                                     'RuntimeObserver.oldAlerts should have no truthy value as they were not previous set');
  
               dfd.resolve('OK');
             } catch(err) {
               dfd.reject(err);
             }
           };
  
           // // Trigger the server's onAppsTallyChange and onAppsListChange method via the Server event handler
           host._handleChangeEvent({type: 'host', id: 'localhost', servers: {up: 1, down: 0, unknown: 0, added: ['localhost,/wlp/usr,server1']}});
  
           return dfd;
         }),
  
         tdd.test('Host changes - server with alerts removed from runtime, no servers left', function() {
           var host = new Host({id: 'localhost', runtimes: {list: [{id:'/wlp', name:'/wlp', type:'runtime', runtimeType: "Liberty"}]}, servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
           var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'UNKNOWN', apps: {up: 0, down: 0, unknown: 0, list: []},
             alerts: { count: 1, unknown: [{id: 'localhost,/wlp/usr,server1', type: 'server'}]}});
           var runtime = new Runtime({host: host, path: '/wlp', servers: [server], runtimeType: "Liberty"});
           var observer = new RuntimeObserver();
  
           // Sanity check, make sure we start with alerts
           assert.isNotNull(runtime.alerts,                                 'Runtime.alerts should have been set as the Runtime was constructed with servers with alerts');
  
           runtime.subscribe(observer);
  
           // Trigger the server's onAppsTallyChange and onAppsListChange method via the Server event handler
           host._handleChangeEvent({type: 'host', id: 'localhost', servers: {up: 0, down: 0, unknown: 0, removed: ['localhost,/wlp/usr,server1']}});
  
           assert.notOk(runtime.alerts,                                'Runtime.alerts should have been cleared as there are no more servers with alerts');
  
           // Validate the Observer was passed the correct tally objects after the first event
           assert.notOk(observer.newAlerts,                            'RuntimeObserver.newAlerts should have no truthy value as the new alerts are unset');
  
           assert.isNotNull(observer.oldAlerts,                             'RuntimeObserver.oldAlerts should have been set as there was a server with alerts added');
           assert.equal(observer.oldAlerts.count,                   1, 'RuntimeObserver.oldAlerts.count was not correct');
           assert.equal(observer.oldAlerts.unknown.length,          1, 'RuntimeObserver.oldAlerts.unknown.length was not correct');
           assert.equal(observer.oldAlerts.unknown[0].id, 'localhost,/wlp/usr,server1', 'RuntimeObserver.oldAlerts.unknown[0].id was not correct');
           assert.equal(observer.oldAlerts.unknown[0].type, 'server',  'RuntimeObserver.oldAlerts.unknown[0].type was not correct');
         }),
  
         tdd.test('Server on runtime changes - alerts added', function() {
           var host = new Host({id: 'localhost', runtimes: {list: [{id:'/wlp', name:'/wlp', type:'runtime', runtimeType: "Liberty"}]}, servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
           var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STOPPED', apps: {up: 0, down: 0, unknown: 0, list: []}});
           var runtime = new Runtime({host: host, path: '/wlp', servers: [server], runtimeType: "Liberty"});
           var observer = new RuntimeObserver();
  
           // Sanity check, make sure we start with alerts
           assert.notOk(runtime.alerts,                                'Runtime.alerts should not be set as the Runtime was constructed with servers without alerts');
  
           runtime.subscribe(observer);
  
           assert.equal(runtime.servers.up,          0,                'Runtime.servers.up did not have the correct initialized value');
           assert.equal(runtime.servers.down,        1,                'Runtime.servers.down did not have the correct initialized value');
           assert.equal(runtime.servers.unknown,     0,                'Runtime.servers.unknown did not have the correct initialized value');
  
           server._handleChangeEvent({type: 'server', id: 'localhost,/wlp/usr,server1', alerts: { count: 1, unknown: [{id: 'localhost,/wlp/usr,server1', type: 'server'}]} });
  
           assert.isNotNull(runtime.alerts,                                 'Runtime.alerts should have been set as there was a server\'s alerts changes to have alerts');
           assert.equal(runtime.alerts.count,                       1, 'Runtime.alerts.count was not correct');
           assert.equal(runtime.alerts.unknown.length,              1, 'Runtime.alerts.unknown.length was not correct');
           assert.equal(runtime.alerts.unknown[0].id, 'localhost,/wlp/usr,server1', 'Runtime.alerts.unknown[0].id was not correct');
           assert.equal(runtime.alerts.unknown[0].type,   'server',    'Runtime.alerts.unknown[0].type was not correct');
  
           // Validate the Observer was passed the correct tally objects after the first event
           assert.isNotNull(observer.newAlerts,                             'RuntimeObserver.newAlerts should have been set as there was a server with alerts added');
           assert.equal(observer.newAlerts.count,                   1, 'RuntimeObserver.newAlerts.count was not correct');
           assert.equal(observer.newAlerts.unknown.length,          1, 'RuntimeObserver.newAlerts.unknown.length was not correct');
           assert.equal(observer.newAlerts.unknown[0].id, 'localhost,/wlp/usr,server1', 'RuntimeObserver.newAlerts.unknown[0].id was not correct');
           assert.equal(observer.newAlerts.unknown[0].type, 'server',  'RuntimeObserver.newAlerts.unknown[0].type was not correct');
  
           assert.notOk(observer.oldAlerts,                            'RuntimeObserver.oldAlerts should have no truthy value as they were not previous set');
         }),
  
         tdd.test('Server on runtime changes - alerts removed', function() {
           var host = new Host({id: 'localhost', runtimes: {list: [{id:'/wlp', name:'/wlp', type:'runtime', runtimeType: "Liberty"}]}, servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
           var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STOPPED', apps: {up: 0, down: 0, unknown: 0, list: []},
             alerts: { count: 1, unknown: [{id: 'localhost,/wlp/usr,server1', type: 'server'}]}});
           var runtime = new Runtime({host: host, path: '/wlp', servers: [server], runtimeType: "Liberty"});
           var observer = new RuntimeObserver();
  
           // Sanity check, make sure we start with alerts
           assert.isNotNull(runtime.alerts,                                 'Runtime.alerts should have been set as the Runtime was constructed with servers with alerts');
  
           runtime.subscribe(observer);
  
           assert.equal(runtime.servers.up,          0,                'Runtime.servers.up did not have the correct initialized value');
           assert.equal(runtime.servers.down,        1,                'Runtime.servers.down did not have the correct initialized value');
           assert.equal(runtime.servers.unknown,     0,                'Runtime.servers.unknown did not have the correct initialized value');
  
           server._handleChangeEvent({type: 'server', id: 'localhost,/wlp/usr,server1', alerts: { count: 0} });
  
           assert.notOk(runtime.alerts,                                'Runtime.alerts should have been cleared as there are no more servers with alerts');
  
           // Validate the Observer was passed the correct tally objects after the first event
           assert.notOk(observer.newAlerts,                             'RuntimeObserver.newAlerts should have no truthy value as the new alerts are unset');
  
           assert.isNotNull(observer.oldAlerts,                             'RuntimeObserver.oldAlerts should have been set as there was a server with alerts added');
           assert.equal(observer.oldAlerts.count,                   1, 'RuntimeObserver.oldAlerts.count was not correct');
           assert.equal(observer.oldAlerts.unknown.length,          1, 'RuntimeObserver.oldAlerts.unknown.length was not correct');
           assert.equal(observer.oldAlerts.unknown[0].id, 'localhost,/wlp/usr,server1', 'RuntimeObserver.oldAlerts.unknown[0].id was not correct');
           assert.equal(observer.oldAlerts.unknown[0].type, 'server',  'RuntimeObserver.oldAlerts.unknown[0].type was not correct');
         }),
         
         tdd.test('constructor - host with Node.js runtime with running server', function() {
           var host = new Host({id: 'localhost', runtimes: {list: [{id:'/wlp', name:'/wlp', type:'runtime', runtimeType: "Node.js"}]}, servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
           var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', apps: {up: 0, down: 0, unknown: 0, list: []}});
           var runtime = new Runtime({host: host, path: '/wlp', servers: [server], runtimeType: "Node.js"});
  
           assert.equal(runtime.id,                  'localhost,/wlp', 'Runtime.id did not have the correct initialized value');
           assert.equal(runtime.name,                i18n.RUNTIME_NODEJS, 'Runtime.name did not have the correct initialized value');
           assert.equal(runtime.state,               'STARTED',        'Runtime.state did not have the correct initialized value');
           assert.equal(runtime.runtimeType,         'Node.js',        'Runtime.runtimeType did not have the correct initialized value');
           assert.equal(runtime.servers.up,          1,                'Runtime.servers.up not have the correct initialized value');
           assert.equal(runtime.servers.down,        0,                'Runtime.servers.down not have the correct initialized value');
           assert.equal(runtime.servers.unknown,     0,                'Runtime.servers.unknown not have the correct initialized value');
           assert.isTrue(Array.isArray(runtime.servers.list),            'Runtime.servers.list was not an Array');
           assert.equal(runtime.servers.list.length, 1,                'Runtime.servers.list.length was not initially empty');
           assert.equal(runtime.servers.list[0],     server,           'Runtime.servers.list[0] was not correct');
           assert.notOk(runtime.alerts,                                'Runtime.alerts should not be set as there are no servers with alerts');
         }),
         
         tdd.test('constructor - host with Liberty runtime in a Docker container', function() {
           var host = new Host({id: 'localhost', runtimes: {list: [{id:'/wlp', name:'/wlp', type:'runtime', runtimeType: "Liberty", containerType: "Docker"}]}, servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
           var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', apps: {up: 0, down: 0, unknown: 0, list: []}});
           var runtime = new Runtime({host: host, path: '/wlp', servers: [server], runtimeType: "Liberty", containerType: "Docker"});
  
           assert.equal(runtime.id,                  'localhost,/wlp', 'Runtime.id did not have the correct initialized value');
           assert.equal(runtime.name,                i18n.RUNTIME_LIBERTY, 'Runtime.name did not have the correct initialized value');
           assert.equal(runtime.state,               'STARTED',        'Runtime.state did not have the correct initialized value');
           assert.equal(runtime.runtimeType,         'Liberty',        'Runtime.runtimeType did not have the correct initialized value for runtimeType');
           assert.equal(runtime.containerType,       'Docker',         'Runtime.runtimeType did not have the correct initialized value for containerType');
           assert.equal(runtime.servers.up,          1,                'Runtime.servers.up not have the correct initialized value');
           assert.equal(runtime.servers.down,        0,                'Runtime.servers.down not have the correct initialized value');
           assert.equal(runtime.servers.unknown,     0,                'Runtime.servers.unknown not have the correct initialized value');
           assert.isTrue(Array.isArray(runtime.servers.list),           'Runtime.servers.list was not an Array');
           assert.equal(runtime.servers.list.length, 1,                'Runtime.servers.list.length was not initially empty');
           assert.equal(runtime.servers.list[0],     server,           'Runtime.servers.list[0] was not correct');
           assert.notOk(runtime.alerts,                                'Runtime.alerts should not be set as there are no servers with alerts');
         });
      });
    }
});
