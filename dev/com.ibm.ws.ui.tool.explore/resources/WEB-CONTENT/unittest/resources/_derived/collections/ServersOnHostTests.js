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
 * Test cases for ServersOnHost
 */
define([ 
        "intern!tdd",
        "intern/chai!assert",
        "intern/dojo/declare",
        "resources/_derived/collections/ServersOnHost",
        "resources/_objects/Host",
        "resources/Observer",
        "dojo/Deferred"
        ],
        
    function(tdd, assert, declare, ServersOnHost, Host, Observer, Deferred) {

    var ServersOnHostObserver = declare([Observer], {
      id: 'testObserver',
  
      onTallyChange: function(newTally, oldTally) {
        this.newTally = newTally;
        this.oldTally = oldTally;
      },
  
      onServersListChange: function(newList, oldList, added, removed, changed) {
        this.newList = newList;
        this.oldList = oldList;
        this.added = added;
        this.removed = removed;
        this.changed = changed;
      },
  
      onDestroyed: function() {
        this.destroyed = true;
      }
    });
  
    with(assert) {
      
      /**
       * Defines the 'ServersOnHost Collection Tests' module test suite.
       */
      tdd.suite('ServersOnHost Collection Tests', function() {
  
           tdd.test('constructor - no initialization object', function() {
             try {
               new ServersOnHost();
               assert.isTrue(false, 'ServersOnHost was successfully created when it should have failed - an initialization object is required');
             } catch(error) {
               assert.equal(error, 'ServersOnHost created without an initialization object', 'Error reported did not match expected error');
             }
           }),
    
           tdd.test('constructor - no host', function() {
             try {
               new ServersOnHost({servers: []});
               assert.isTrue(false, 'ServersOnHost was successfully created when it should have failed - a host is required');
             } catch(error) {
               assert.equal(error, 'ServersOnHost created without a host', 'Error reported did not match expected error');
             }
           }),
    
           tdd.test('constructor - no servers', function() {
             try {
               new ServersOnHost({host: {}});
               assert.isTrue(false, 'ServersOnHost was successfully created when it should have failed - an array of Server is required');
             } catch(error) {
               assert.equal(error, 'ServersOnHost created without an array of Server', 'Error reported did not match expected error');
             }
           }),
    
           tdd.test('constructor - host with no servers', function() {
             var host = new Host({id: 'localhost', runtimes: {list: []}, servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
             var serversOnHost = new ServersOnHost({host: host, servers: []});
    
             assert.equal(serversOnHost.id, 'serversOnHost(localhost)', 'ServersOnHost.id did not have the correct initialized value');
             assert.equal(serversOnHost.up,          0,           'ServersOnHost.up did not have the correct initialized value');
             assert.equal(serversOnHost.down,        0,           'ServersOnHost.down did not have the correct initialized value');
             assert.equal(serversOnHost.unknown,     0,           'ServersOnHost.unknown did not have the correct initialized value');
             assert.isTrue(Array.isArray(serversOnHost.list),       'ServersOnHost.list was not an Array');
             assert.equal(serversOnHost.list.length, 0,           'ServersOnHost.list.length was not initially empty');
           }),
    
           tdd.test('constructor - host with servers', function() {
             var host = new Host({id: 'localhost', runtimes: {list: []}, servers: {up: 1, down: 2, unknown: 3, list: ['localhost,/wlp/usr,server1']}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
             var serversOnHost = new ServersOnHost({host: host, servers: [{id: 'localhost,/wlp/usr,server1'}]});
    
             assert.equal('serversOnHost(localhost)', serversOnHost.id, 'ServersOnHost.id did not have the correct initialized value');
             assert.equal(serversOnHost.up,          1,           'ServersOnHost.up did not have the correct initialized value');
             assert.equal(serversOnHost.down,        2,           'ServersOnHost.down did not have the correct initialized value');
             assert.equal(serversOnHost.unknown,     3,           'ServersOnHost.unknown did not have the correct initialized value');
             assert.isTrue(Array.isArray(serversOnHost.list),       'ServersOnHost.list was not an Array');
             assert.equal(serversOnHost.list.length, 1,           'ServersOnHost.list.length was not the correct value');
             assert.equal(serversOnHost.list[0].id, 'localhost,/wlp/usr,server1', 'ServersOnHost.list[0].id did not have the correct value');
           }),
    
           tdd.test('Host changes - servers tallies changed', function() {
             var host = new Host({id: 'localhost', runtimes: {list: []}, servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
             var serversOnHost = new ServersOnHost({host: host, servers: []});
             var observer = new ServersOnHostObserver();
    
             serversOnHost.subscribe(observer);
    
             assert.equal(serversOnHost.up,          0,           'ServersOnHost.up did not have the correct initialized value');
             assert.equal(serversOnHost.down,        0,           'ServersOnHost.down did not have the correct initialized value');
             assert.equal(serversOnHost.unknown,     0,           'ServersOnHost.unknown did not have the correct initialized value');
             assert.isTrue(Array.isArray(serversOnHost.list),       'ServersOnHost.list was not an Array');
             assert.equal(serversOnHost.list.length, 0,           'ServersOnHost.list.length was not initially empty');
    
             // Trigger the host's onServersTallyChange method via the Host event handler
             host._handleChangeEvent({type: 'host', id: 'localhost', servers: {up: 1, down: 2, unknown: 3}});
    
             assert.equal(serversOnHost.up,          1,           'ServersOnHost.up did not have the correct updated value');
             assert.equal(serversOnHost.down,        2,           'ServersOnHost.down did not have the correct updated value');
             assert.equal(serversOnHost.unknown,     3,           'ServersOnHost.unknown did not have the correct updated value');
             assert.isTrue(Array.isArray(serversOnHost.list),       'ServersOnHost.list was not an Array');
             assert.equal(serversOnHost.list.length, 0,           'ServersOnHost.list.length was not initially empty');
    
             // Validate the Observer was passed the correct tally objects after the first event
             assert.isNotNull(observer.newTally,             'ServersOnHostObserver.newTally did not get set, when it should have been');
             assert.equal(observer.newTally.up,      1, 'ServersOnHostObserver did not get the correct new value for the up tally');
             assert.equal(observer.newTally.down,    2, 'ServersOnHostObserver did not get the correct new value for the down tally');
             assert.equal(observer.newTally.unknown, 3, 'ServersOnHostObserver did not get the correct new value for the unknown tally');
    
             assert.isNotNull(observer.oldTally,             'ServersOnHostObserver.oldTally did not get set, when it should have been');
             assert.equal(observer.oldTally.up,      0, 'ServersOnHostObserver did not get the correct old value for the up tally');
             assert.equal(observer.oldTally.down,    0, 'ServersOnHostObserver did not get the correct old value for the down tally');
             assert.equal(observer.oldTally.unknown, 0, 'ServersOnHostObserver did not get the correct old value for the unknown tally');
           }),
    
           tdd.test('Host changes - added server', function() {
             var host = new Host({id: 'localhost', runtimes: {list: []}, servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
             var serversOnHost = new ServersOnHost({host: host, servers: []});
             var observer = new ServersOnHostObserver();
    
             serversOnHost.subscribe(observer);
    
             // TODO: This mock is needed because of the temporary behaviour here we don't store names
             serversOnHost.resourceManager = {
                 getServer: function(list) {
                   var deferred = new Deferred();
                   deferred.resolve([{id: 'localhost,/wlp/usr,server1'}], true);
                   return deferred;
                 }
             };
    
             var dfd = this.async(1000);
    
             // Need to wait until the onListChange method fires before we 
             observer.onServersListChange = function(newList, oldList, added, removed, changed) {
               try {
                 assert.equal(serversOnHost.up,          1,           'ServersOnHost.up did not have the correct updated value');
                 assert.equal(serversOnHost.down,        0,           'ServersOnHost.down did not have the correct updated value');
                 assert.equal(serversOnHost.unknown,     0,           'ServersOnHost.unknown did not have the correct updated value');
                 assert.isTrue(Array.isArray(serversOnHost.list),       'ServersOnHost.list was not an Array');
                 assert.equal(serversOnHost.list.length, 1,           'ServersOnHost.list.length was not the correct updated value');
                 assert.equal(serversOnHost.list[0].id, 'localhost,/wlp/usr,server1', 'ServersOnHost.list[0].id did not have the correct updated value for the object name');
    
                 // Validate the Observer was passed the correct tally objects after the first event
                 assert.isNotNull(observer.newTally,             'ServersOnHostObserver.newTally did not get set, when it should have been');
                 assert.equal(observer.newTally.up,      1, 'ServersOnHostObserver did not get the correct new value for the up tally');
                 assert.equal(observer.newTally.down,    0, 'ServersOnHostObserver did not get the correct new value for the down tally');
                 assert.equal(observer.newTally.unknown, 0, 'ServersOnHostObserver did not get the correct new value for the unknown tally');
    
                 assert.isNotNull(observer.oldTally,             'ServersOnHostObserver.oldTally did not get set, when it should have been');
                 assert.equal(observer.oldTally.up,      0, 'ServersOnHostObserver did not get the correct old value for the up tally');
                 assert.equal(observer.oldTally.down,    0, 'ServersOnHostObserver did not get the correct old value for the down tally');
                 assert.equal(observer.oldTally.unknown, 0, 'ServersOnHostObserver did not get the correct old value for the unknown tally');
    
                 assert.isNotNull(newList,              'ServersOnHostObserver.newList did not get set, when it should have been');
                 assert.equal(newList.length,   1, 'ServersOnHostObserver.newList was not of expected size');
                 assert.equal(newList[0].id, 'localhost,/wlp/usr,server1', 'ServersOnHostObserver.newList[0].id was not of expected value');
                 assert.isNotNull(oldList,              'ServersOnHostObserver.oldList did not get set, when it should have been');
                 assert.equal(oldList.length,   0, 'ServersOnHostObserver.oldList was not empty');
                 assert.isNotNull(added,                'ServersOnHostObserver.added did not get set, when it should have been');
                 assert.equal(added.length,     1, 'ServersOnHostObserver.added was not of expected size');
                 assert.equal(added[0], 'localhost,/wlp/usr,server1', 'ServersOnHostObserver.added[0] was not of expected value');
                 assert.isNotNull(removed,             'ServersOnHostObserver.removed got set when it should not have been');
    
                 dfd.resolve('OK');
               } catch(err) {
                 dfd.reject(err);
               }
             };
    
             // Trigger the server's onAppsTallyChange and onAppsListChange method via the Server event handler
             host._handleChangeEvent({type: 'host', id: 'localhost', servers: {up: 1, down: 0, unknown: 0, added: ['localhost,/wlp/usr,server1']}});
    
             return dfd;
           }),
    
           tdd.test('Host changes - removed server', function() {
             var host = new Host({id: 'localhost', runtimes: {list: []}, servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
             var serversOnHost = new ServersOnHost({host: host, servers: [{id: 'localhost,/wlp/usr,server1'}]});
             var observer = new ServersOnHostObserver();
    
             serversOnHost.subscribe(observer);
    
             assert.equal(serversOnHost.up,          1,           'ServersOnHost.up did not have the correct initialized value');
             assert.equal(serversOnHost.down,        0,           'ServersOnHost.down did not have the correct initialized value');
             assert.equal(serversOnHost.unknown,     0,           'ServersOnHost.unknown did not have the correct initialized value');
             assert.isTrue(Array.isArray(serversOnHost.list),       'ServersOnHost.list was not an Array');
             assert.equal(serversOnHost.list.length, 1,           'ServersOnHost.list.length was not the correct initial value');
             assert.equal(serversOnHost.list[0].id, 'localhost,/wlp/usr,server1', 'ServersOnHost.list[0].id did not have the correct value for the object name');
    
             // Trigger the server's onAppsTallyChange method via the event handler
             host._handleChangeEvent({type: 'host', id: 'localhost', servers: {up: 0, down: 0, unknown: 0, removed: ['localhost,/wlp/usr,server1']}});
    
             assert.equal(0,           serversOnHost.up,          'ServersOnHost.up did not have the correct updated value');
             assert.equal(0,           serversOnHost.down,        'ServersOnHost.down did not have the correct updated value');
             assert.equal(0,           serversOnHost.unknown,     'ServersOnHost.unknown did not have the correct updated value');
             assert.isTrue(Array.isArray(serversOnHost.list),       'ServersOnHost.list was not an Array');
             assert.equal(0,           serversOnHost.list.length, 'ServersOnHost.list.length was not updated to be empty');
    
             // Validate the Observer was passed the correct tally objects after the first event
             assert.isNotNull(observer.newTally,             'ServersOnHostObserver.newTally did not get set, when it should have been');
             assert.equal(observer.newTally.up,      0, 'ServersOnHostObserver did not get the correct new value for the up tally');
             assert.equal(observer.newTally.down,    0, 'ServersOnHostObserver did not get the correct new value for the down tally');
             assert.equal(observer.newTally.unknown, 0, 'ServersOnHostObserver did not get the correct new value for the unknown tally');
    
             assert.isNotNull(observer.oldTally,             'ServersOnHostObserver.oldTally did not get set, when it should have been');
             assert.equal(observer.oldTally.up,      1, 'ServersOnHostObserver did not get the correct old value for the up tally');
             assert.equal(observer.oldTally.down,    0, 'ServersOnHostObserver did not get the correct old value for the down tally');
             assert.equal(observer.oldTally.unknown, 0, 'ServersOnHostObserver did not get the correct old value for the unknown tally');
    
             assert.isNotNull(observer.newList,              'ServersOnHostObserver.newList did not get set, when it should have been');
             assert.equal(observer.newList.length,   0, 'ServersOnHostObserver.newList was not of expected size');
             assert.isNotNull(observer.oldList,              'ServersOnHostObserver.oldList did not get set, when it should have been');
             assert.equal(observer.oldList.length,   1, 'ServersOnHostObserver.oldList was not empty');
             assert.equal(observer.oldList[0].id, 'localhost,/wlp/usr,server1', 'ServersOnHostObserver.oldList[0].id did not have the correct value for the object name');
             assert.isNotNull(observer.added,               'ServersOnHostObserver.added got set when it should not have been');
             assert.isNotNull(observer.removed,              'ServersOnHostObserver.removed did not get set, when it should have been');
             assert.equal(observer.removed.length,   1, 'ServersOnHostObserver.removed was not empty');
             assert.equal(observer.removed[0], 'localhost,/wlp/usr,server1', 'ServersOnHostObserver.removed[0] did not have the correct value for the object name');
           }),
           
           /**
            * This test is to ensure we have the right splice logic
            */
           tdd.test('Host changes - removed middle server', function() {
             var host = new Host({id: 'localhost', runtimes: {list: []}, servers: {up: 3, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1', 'localhost,/wlp/usr,server2', 'localhost,/wlp/usr,server3']}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
             var serversOnHost = new ServersOnHost({host: host, servers: [{id: 'localhost,/wlp/usr,server1'}, {id: 'localhost,/wlp/usr,server2'}, {id: 'localhost,/wlp/usr,server3'}]});
             var observer = new ServersOnHostObserver();
    
             serversOnHost.subscribe(observer);
    
             assert.equal(serversOnHost.up,          3,           'ServersOnHost.up did not have the correct initialized value');
             assert.equal(serversOnHost.down,        0,           'ServersOnHost.down did not have the correct initialized value');
             assert.equal(serversOnHost.unknown,     0,           'ServersOnHost.unknown did not have the correct initialized value');
             assert.isTrue(Array.isArray(serversOnHost.list),       'ServersOnHost.list was not an Array');
             assert.equal(serversOnHost.list.length, 3,           'ServersOnHost.list.length was not the correct initial value');
             assert.equal(serversOnHost.list[0].id, 'localhost,/wlp/usr,server1', 'ServersOnHost.list[0].id did not have the correct value for the object name');
             assert.equal(serversOnHost.list[1].id, 'localhost,/wlp/usr,server2', 'ServersOnHost.list[1].id did not have the correct value for the object name');
             assert.equal(serversOnHost.list[2].id, 'localhost,/wlp/usr,server3', 'ServersOnHost.list[2].id did not have the correct value for the object name');
    
             // Trigger the server's onAppsTallyChange method via the event handler
             host._handleChangeEvent({type: 'host', id: 'localhost', servers: {up: 2, down: 0, unknown: 0, removed: ['localhost,/wlp/usr,server2']}});
    
             assert.equal(serversOnHost.up,          2,           'ServersOnHost.up did not have the correct updated value');
             assert.equal(serversOnHost.down,        0,           'ServersOnHost.down did not have the correct updated value');
             assert.equal(serversOnHost.unknown,     0,           'ServersOnHost.unknown did not have the correct updated value');
             assert.isTrue(Array.isArray(serversOnHost.list),       'ServersOnHost.list was not an Array');
             assert.equal(serversOnHost.list.length, 2,           'ServersOnHost.list.length was not updated to be empty');
             assert.equal(serversOnHost.list[0].id, 'localhost,/wlp/usr,server1', 'ServersOnHost.list[0].id did not have the correct value for the object name');
             assert.equal(serversOnHost.list[1].id, 'localhost,/wlp/usr,server3', 'ServersOnHost.list[1].id did not have the correct value for the object name');
    
             // Validate the Observer was passed the correct tally objects after the first event
             assert.isNotNull(observer.newTally,             'ServersOnHostObserver.newTally did not get set, when it should have been');
             assert.equal(observer.newTally.up,      2, 'ServersOnHostObserver did not get the correct new value for the up tally');
             assert.equal(observer.newTally.down,    0, 'ServersOnHostObserver did not get the correct new value for the down tally');
             assert.equal(observer.newTally.unknown, 0, 'ServersOnHostObserver did not get the correct new value for the unknown tally');
    
             assert.isNotNull(observer.oldTally,             'ServersOnHostObserver.oldTally did not get set, when it should have been');
             assert.equal(observer.oldTally.up,      3, 'ServersOnHostObserver did not get the correct old value for the up tally');
             assert.equal(observer.oldTally.down,    0, 'ServersOnHostObserver did not get the correct old value for the down tally');
             assert.equal(observer.oldTally.unknown, 0, 'ServersOnHostObserver did not get the correct old value for the unknown tally');
    
             assert.isNotNull(observer.newList,              'ServersOnHostObserver.newList did not get set, when it should have been');
             assert.equal(observer.newList.length,   2, 'ServersOnHostObserver.newList was not of expected size');
             assert.equal(observer.newList[0].id, 'localhost,/wlp/usr,server1', 'ServersOnHostObserver.newList[0].id did not have the correct value for the object name');
             assert.equal(observer.newList[1].id, 'localhost,/wlp/usr,server3', 'ServersOnHostObserver.newList[1].id did not have the correct value for the object name');
             assert.isNotNull(observer.oldList,              'ServersOnHostObserver.oldList did not get set, when it should have been');
             assert.equal(observer.oldList.length,   3, 'ServersOnHostObserver.oldList was not empty');
             assert.equal(observer.oldList[0].id, 'localhost,/wlp/usr,server1', 'ServersOnHostObserver.oldList[0].id did not have the correct value for the object name');
             assert.equal(observer.oldList[1].id, 'localhost,/wlp/usr,server2', 'ServersOnHostObserver.oldList[1].id did not have the correct value for the object name');
             assert.equal(observer.oldList[2].id, 'localhost,/wlp/usr,server3', 'ServersOnHostObserver.oldList[2].id did not have the correct value for the object name');
             assert.isNotNull(observer.added,               'ServersOnHostObserver.added got set when it should not have been');
             assert.isNotNull(observer.removed,              'ServersOnHostObserver.removed did not get set, when it should have been');
             assert.equal(observer.removed.length,   1, 'ServersOnHostObserver.removed was not empty');
             assert.equal(observer.removed[0], 'localhost,/wlp/usr,server2', 'ServersOnHostObserver.removed[0] did not have the correct value for the object name');
           }),
           
           tdd.test('Host changes - host was removed from the collective', function() {
             var host = new Host({id: 'localhost', runtimes: {list: []}, servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
             var serversOnHost = new ServersOnHost({host: host, servers: []});
             var observer = new ServersOnHostObserver();
    
             serversOnHost.subscribe(observer);
    
             // Host is removed from collective
             host._handleChangeEvent({type: 'host', id: 'localhost', state: 'removed'});
    
             assert.isTrue(serversOnHost.isDestroyed,   'ServersOnHost.isDestroyed flag did not get set in response to a "removed" event');
             
             // Confirm the application is destroyed
             assert.isTrue(observer.destroyed,          'ServersOnHostObserver.onDestroyed did not get called');
           });
        });
    }
});
