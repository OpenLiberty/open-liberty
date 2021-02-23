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
 * Test cases for ServersOnRuntime
 */
define([ 
        "intern!tdd",
        "intern/chai!assert",
        "intern/dojo/declare",
        "resources/_derived/collections/ServersOnRuntime",
        "resources/_derived/objects/Runtime",
        "resources/_objects/Host",
        "resources/_objects/Server",
        "resources/Observer"
        ],
        
    function(tdd, assert, declare, ServersOnRuntime, Runtime, Host, Server, Observer) {

    var ServersOnRuntimeObserver = declare([Observer], {
      id: 'testObserver',
  
      onTallyChange: function(newTally, oldTally) {
        this.newTally = newTally;
        this.oldTally = oldTally;
      },
  
      onListChange: function(newList, oldList, added, removed) {
        this.newList = newList;
        this.oldList = oldList;
        this.added = added;
        this.removed = removed;
      },
  
      onDestroyed: function() {
        this.destroyed = true;
      }
    });

    with(assert) {
      
      /**
       * Defines the 'ServersOnRuntime Collection Tests' module test suite.
       */
      tdd.suite('ServersOnRuntime Collection Tests', function() {
        
           tdd.test('constructor - no initialization object', function() {
             try {
               new ServersOnRuntime();
               assert.isTrue(false, 'ServersOnRuntime was successfully created when it should have failed - an initialization object is required');
             } catch(error) {
               assert.equal(error, 'ServersOnRuntime created without an initialization object', 'Error reported did not match expected error');
             }
           }),
    
           tdd.test('constructor - no runtime', function() {
             try {
               new ServersOnRuntime({});
               assert.isTrue(false, 'ServersOnRuntime was successfully created when it should have failed - a runtime is required');
             } catch(error) {
               assert.equal(error, 'ServersOnRuntime created without a runtime', 'Error reported did not match expected error');
             }
           }),
    
           tdd.test('constructor - runtime with no servers', function() {
             var host = new Host({id: 'localhost', runtimes: {list: [{id:'/wlp', name:'/wlp', type:'runtime'}]}, servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
             var runtime = new Runtime({host: host, path: '/wlp', servers: []});
             var serversOnRuntime = new ServersOnRuntime({runtime: runtime});
    
             assert.equal(serversOnRuntime.id, 'serversOnRuntime(localhost,/wlp)', 'ServersOnRuntime.id did not have the correct initialized value');
             assert.equal(serversOnRuntime.up,          0,           'ServersOnRuntime.up did not have the correct initialized value');
             assert.equal(serversOnRuntime.down,        0,           'ServersOnRuntime.down did not have the correct initialized value');
             assert.equal(serversOnRuntime.unknown,     0,           'ServersOnRuntime.unknown did not have the correct initialized value');
             assert.isTrue(Array.isArray(serversOnRuntime.list),       'ServersOnRuntime.list was not an Array');
             assert.equal(serversOnRuntime.list.length, 0,           'ServersOnRuntime.list.length was not initially empty');
           }),
    
           tdd.test('constructor - runtime with servers', function() {
             var host = new Host({id: 'localhost', runtimes: {list: [{id:'/wlp', name:'/wlp', type:'runtime'}]}, servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
             var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', apps: {up: 0, down: 0, unknown: 0, list: []}});
             var runtime = new Runtime({host: host, path: '/wlp', servers: [server]});
             var serversOnRuntime = new ServersOnRuntime({runtime: runtime});
    
             assert.equal(serversOnRuntime.id, 'serversOnRuntime(localhost,/wlp)', 'ServersOnRuntime.id did not have the correct initialized value');
             assert.equal(serversOnRuntime.up,          1,           'ServersOnRuntime.up did not have the correct initialized value');
             assert.equal(serversOnRuntime.down,        0,           'ServersOnRuntime.down did not have the correct initialized value');
             assert.equal(serversOnRuntime.unknown,     0,           'ServersOnRuntime.unknown did not have the correct initialized value');
             assert.isTrue(Array.isArray(serversOnRuntime.list),       'ServersOnRuntime.list was not an Array');
             assert.equal(serversOnRuntime.list.length, 1,           'ServersOnRuntime.list.length was not the correct value');
             assert.equal(serversOnRuntime.list[0].id, 'localhost,/wlp/usr,server1', 'ServersOnRuntime.list[0] did not have the correct value');
           }),
    
           tdd.test('Rutime changes - runtime removed', function() {
             var host = new Host({id: 'localhost', runtimes: {list: [{id:'/wlp', name:'/wlp', type:'runtime'}]}, servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
             var runtime = new Runtime({host: host, path: '/wlp', servers: []});
             var serversOnRuntime = new ServersOnRuntime({runtime: runtime});
             var observer = new ServersOnRuntimeObserver();
    
             // Subscribe the Observer
             serversOnRuntime.subscribe(observer);
    
             // Trigger the onDestroyed method - we don't need to drive this through the Host or the Runtime because tests already prove the Observer will be called
             serversOnRuntime.onDestroyed();
             
             assert.isNotNull(serversOnRuntime.isDestroyed, 'ServersOnRuntime.isDestroyed flag did not get set in response to a "removed" event');
    
             assert.isNotNull(observer.destroyed,           'ServersOnRuntimeObserver.onDestroyed was not called');
           }),
    
           tdd.test('Rutime changes - servers tally changes', function() {
             var host = new Host({id: 'localhost', runtimes: {list: [{id:'/wlp', name:'/wlp', type:'runtime'}]}, servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
             var runtime = new Runtime({host: host, path: '/wlp', servers: []});
             var serversOnRuntime = new ServersOnRuntime({runtime: runtime});
             var observer = new ServersOnRuntimeObserver();
    
             // Subscribe the Observer
             serversOnRuntime.subscribe(observer);
    
             // Trigger the onServersTallyChange method - we don't need to drive this through the Host or the Runtime because tests already prove the Observer will be called
             serversOnRuntime.onServersTallyChange({up: 1, down: 2, unknown: 3});
    
             // Validate the Observer was passed the correct tally objects after the first event
             assert.isNotNull(observer.newTally,             'ServersOnRuntimeObserver.newTally did not get set, when it should have been');
             assert.equal(observer.newTally.up,      1, 'ServersOnRuntimeObserver did not get the correct new value for the up tally');
             assert.equal(observer.newTally.down,    2, 'ServersOnRuntimeObserver did not get the correct new value for the down tally');
             assert.equal(observer.newTally.unknown, 3, 'ServersOnRuntimeObserver did not get the correct new value for the unknown tally');
    
             assert.isNotNull(observer.oldTally,             'ServersOnRuntimeObserver.oldTally did not get set, when it should have been');
             assert.equal(observer.oldTally.up,      0, 'ServersOnRuntimeObserver did not get the correct old value for the up tally');
             assert.equal(observer.oldTally.down,    0, 'ServersOnRuntimeObserver did not get the correct old value for the down tally');
             assert.equal(observer.oldTally.unknown, 0, 'ServersOnRuntimeObserver did not get the correct old value for the unknown tally');
           }),
    
           tdd.test('Rutime changes - servers list changes', function() {
             var host = new Host({id: 'localhost', runtimes: {list: [{id:'/wlp', name:'/wlp', type:'runtime'}]}, servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
             var runtime = new Runtime({host: host, path: '/wlp', servers: []});
             var serversOnRuntime = new ServersOnRuntime({runtime: runtime});
             var observer = new ServersOnRuntimeObserver();
    
             // Subscribe the Observer
             serversOnRuntime.subscribe(observer);
    
             // Trigger the onServersListChange method - we don't need to drive this through the Host or the Runtime because tests already prove the Observer will be called
             var newList = [1];
             var oldList = [2];
             var added = [3];
             var removed = [4];
             serversOnRuntime.onServersListChange(newList, oldList, added, removed);
    
             assert.equal(observer.newList,   newList, 'ServersOnRuntimeObserver.newList was not the expected array');
             assert.equal(observer.oldList,   oldList, 'ServersOnRuntimeObserver.oldList was not the expected array');
             assert.equal(observer.added,       added, 'ServersOnRuntimeObserver.added was not the expected array');
             assert.equal(observer.removed,   removed, 'ServersOnRuntimeObserver.removed was not the expected array');
           });
      });
    }
});
