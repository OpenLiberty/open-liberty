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
 * Test cases for RuntimesOnHost
 */
define([ 
        "intern!tdd",
        "intern/chai!assert",
        "intern/dojo/declare",
        "dojo/Deferred",
        "resources/_objects/Host",
        "resources/_derived/collections/RuntimesOnHost",
        "resources/Observer"
        ],
        
    function(tdd, assert, declare, Deferred, Host, RuntimesOnHost, Observer) {

    var RuntimesOnHostObserver = declare([Observer], {
      id: 'testObserver',
  
      onTallyChange: function(newTally, oldTally) {
        this.newTally = newTally;
        this.oldTally = oldTally;
      },
  
      onRuntimesListChange: function(newList, oldList, added, removed) {
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
       * Defines the 'RuntimesOnHost Collection Tests' module test suite.
       */
      tdd.suite('RuntimesOnHost Collection Tests', function() { 
        
           tdd.test('constructor - no initialization object', function() {
             try {
               new RuntimesOnHost();
               assert.isTrue(false, 'RuntimesOnHost was successfully created when it should have failed - an initialization object is required');
             } catch(error) {
               assert.equal(error, 'RuntimesOnHost created without an initialization object', 'Error reported did not match expected error');
             }
           }),
    
           tdd.test('constructor - no host', function() {
             try {
               new RuntimesOnHost({runtime: []});
               assert.isTrue(false, 'RuntimesOnHost was successfully created when it should have failed - a host is required');
             } catch(error) {
               assert.equal(error, 'RuntimesOnHost created without a host', 'Error reported did not match expected error');
             }
           }),
    
           tdd.test('constructor - no runtimes', function() {
             try {
               new RuntimesOnHost({host: {/*...*/}});
               assert.isTrue(false, 'RuntimesOnHost was successfully created when it should have failed - an array of Runtime is required');
             } catch(error) {
               assert.equal(error, 'RuntimesOnHost created without an array of Runtime', 'Error reported did not match expected error');
             }
           }),
    
           tdd.test('constructor - host with no runtimes', function() {
             var host = new Host({id: 'localhost', runtimes: {list: []}, servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
             var runtimesOnHost = new RuntimesOnHost({host: host, runtime: []});
    
             assert.equal('runtimesOnHost(localhost)', runtimesOnHost.id, 'RuntimesOnHost.id did not have the correct initialized value');
             assert.equal(runtimesOnHost.up,           0,           'RuntimesOnHost.up was not initially correct');
             assert.equal(runtimesOnHost.down,         0,           'RuntimesOnHost.down was not initially correct');
             assert.equal(runtimesOnHost.unknown,      0,           'RuntimesOnHost.unknown was not initially correct');
             assert.equal(runtimesOnHost.partial,      0,           'RuntimesOnHost.partial was not initially correct');
             assert.equal(runtimesOnHost.empty,        0,           'RuntimesOnHost.empty was not initially correct');
             assert.isTrue(Array.isArray(runtimesOnHost.list),      'RuntimesOnHost.list was not an Array');
             assert.equal(runtimesOnHost.list.length,  0,           'RuntimesOnHost.list.length was not initially empty');
           }),
    
           tdd.test('constructor - host with runtime with no servers', function() {
             var host = new Host({id: 'localhost', runtimes: {list: [{id: 'localhost,/wlp', type: 'runtime', name: 'localhost,/wlp'}]}, servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
             var mockRuntime = {id: 'localhost,/wlp', servers: {up: 0, down: 0, unknown: 0}};
             var runtimesOnHost = new RuntimesOnHost({host: host, runtime: [mockRuntime]});
    
             assert.equal('runtimesOnHost(localhost)', runtimesOnHost.id, 'RuntimesOnHost.id did not have the correct initialized value');
             assert.equal(runtimesOnHost.up,           0,           'RuntimesOnHost.up was not initially correct');
             assert.equal(runtimesOnHost.down,         0,           'RuntimesOnHost.down was not initially correct');
             assert.equal(runtimesOnHost.unknown,      0,           'RuntimesOnHost.unknown was not initially correct');
             assert.equal(runtimesOnHost.partial,      0,           'RuntimesOnHost.partial was not initially correct');
             assert.equal(runtimesOnHost.empty,        1,           'RuntimesOnHost.empty was not initially correct');
             assert.isTrue(Array.isArray(runtimesOnHost.list),        'RuntimesOnHost.list was not an Array');
             assert.equal(runtimesOnHost.list.length,  1,           'RuntimesOnHost.list.length was not correct');
             assert.equal(runtimesOnHost.list[0].id, 'localhost,/wlp', 'RuntimesOnHost.list.[0].id was not correct');
           }),
    
           tdd.test('constructor - host with multiple runtimes with servers', function() {
             var host = new Host({id: 'localhost', runtimes: {list: [{id: 'localhost,/wlp', type: 'runtime', name: 'localhost,/wlp'}]}, servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
             var mockRuntime1 = {id: 'localhost,/wlp1', servers: {up: 1, down: 0, unknown: 0}}; // All servers running
             var mockRuntime2 = {id: 'localhost,/wlp2', servers: {up: 0, down: 1, unknown: 0}}; // No servers running
             var mockRuntime3 = {id: 'localhost,/wlp3', servers: {up: 1, down: 1, unknown: 1}}; // Mix of servers running
             var mockRuntime4 = {id: 'localhost,/wlp4', servers: {up: 0, down: 0, unknown: 1}}; // All servers unknown
             var runtimesOnHost = new RuntimesOnHost({host: host, runtime: [mockRuntime1, mockRuntime2, mockRuntime3, mockRuntime4]});
    
             assert.equal('runtimesOnHost(localhost)', runtimesOnHost.id, 'RuntimesOnHost.id did not have the correct initialized value');
             assert.equal(runtimesOnHost.up,           1,           'RuntimesOnHost.up was not initially correct');
             assert.equal(runtimesOnHost.down,         1,           'RuntimesOnHost.down was not initially correct');
             assert.equal(runtimesOnHost.unknown,      1,           'RuntimesOnHost.unknown was not initially correct');
             assert.equal(runtimesOnHost.partial,      1,           'RuntimesOnHost.partial was not initially correct');
             assert.equal(runtimesOnHost.empty,        0,           'RuntimesOnHost.empty was not initially correct');
             assert.isTrue(Array.isArray(runtimesOnHost.list),      'RuntimesOnHost.list was not an Array');
             assert.equal(runtimesOnHost.list.length,  4,           'RuntimesOnHost.list.length was not correct');
             assert.equal(runtimesOnHost.list[0].id, 'localhost,/wlp1', 'RuntimesOnHost.list.[0].id was not correct');
             assert.equal(runtimesOnHost.list[1].id, 'localhost,/wlp2', 'RuntimesOnHost.list.[1].id was not correct');
             assert.equal(runtimesOnHost.list[2].id, 'localhost,/wlp3', 'RuntimesOnHost.list.[2].id was not correct');
             assert.equal(runtimesOnHost.list[3].id, 'localhost,/wlp4', 'RuntimesOnHost.list.[3].id was not correct');
    
           }),
    
           tdd.test('Host changes - runtime added', function() {
             var host = new Host({id: 'localhost', runtimes: {list: []}, servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
             var runtimesOnHost = new RuntimesOnHost({host: host, runtime: []});
             var observer = new RuntimesOnHostObserver();
    
             runtimesOnHost.subscribe(observer);
    
             // TODO: This mock is needed because of the temporary behaviour here we don't store names
             runtimesOnHost.resourceManager = {
                 getRuntime: function(list) {
                   var deferred = new Deferred();
                   deferred.resolve([{id: 'localhost,/wlp', servers: {up: 0, down: 0, unknown: 0}}], true);
                   return deferred;
                 }
             };
    
             var dfd = this.async(1000);
    
             // Need to wait until the onListChange method fires before we 
             observer.onRuntimesListChange = function(newList, oldList, added, removed, changed) {
               try {
                 assert.isTrue(Array.isArray(runtimesOnHost.list),      'RuntimesOnHost.list was not an Array');
                 assert.equal(runtimesOnHost.up,           0,           'RuntimesOnHost.up was not initially correct');
                 assert.equal(runtimesOnHost.down,         0,           'RuntimesOnHost.down was not initially correct');
                 assert.equal(runtimesOnHost.unknown,      0,           'RuntimesOnHost.unknown was not initially correct');
                 assert.equal(runtimesOnHost.partial,      0,           'RuntimesOnHost.partial was not initially correct');
                 assert.equal(runtimesOnHost.empty,        1,           'RuntimesOnHost.empty was not initially correct');
                 assert.equal(runtimesOnHost.list.length,  1,           'RuntimesOnHost.list.length was not initially empty');
                 assert.equal(runtimesOnHost.list[0].id, 'localhost,/wlp', 'RuntimesOnHost.list.[0].id was not initially empty');

                 assert.isNotNull(newList,              'RuntimesOnHostObserver.newList did not get set, when it should have been');
                 assert.equal(newList.length, 1,        'RuntimesOnHostObserver.newList was not of expected size');
                 assert.equal(newList[0].id,  'localhost,/wlp', 'RuntimesOnHostObserver.newList[0] was not of expected value');
                 assert.isNotNull(oldList,              'RuntimesOnHostObserver.oldList did not get set, when it should have been');
                 assert.equal(oldList.length, 0,        'RuntimesOnHostObserver.oldList was not empty');
                 assert.isNotNull(added,                'RuntimesOnHostObserver.added did not get set, when it should have been');
                 assert.equal(added.length,   1,        'RuntimesOnHostObserver.added was not of expected size');
                 assert.equal(added[0].id, 'localhost,/wlp', 'RuntimesOnHostObserver.added[0].id was not of expected value');
                 assert.isUndefined(removed,            'RuntimesOnHostObserver.removed got set when it should not have been');
    
                 dfd.resolve('OK');
               } catch(err) {
                 dfd.reject(err);
               }
             };
    
             // Trigger the hosts's onRuntimesChanged method via the Host event handler
             host._handleChangeEvent({type: 'host', id: 'localhost', runtimes: { added: [{id:'localhost,/wlp', type: 'runtime', name: 'localhost,/wlp'}] } });
    
             return dfd;
           }),
    
           tdd.test('Host changes - runtime removed', function() {
             var host = new Host({id: 'localhost', runtimes: {list: [{id: 'localhost,/wlp', type: 'runtime', name: 'localhost,/wlp'}]}, servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
             var runtimesOnHost = new RuntimesOnHost({host: host, runtime: [{id: 'localhost,/wlp', servers: {up: 0, down: 0, unknown: 0}}]});
             var observer = new RuntimesOnHostObserver();
    
             runtimesOnHost.subscribe(observer);
    
             host._handleChangeEvent({type: 'host', id: 'localhost', runtimes: { removed: [{id:'localhost,/wlp', type: 'runtime', name: 'localhost,/wlp'}] } });
    
             assert.equal(runtimesOnHost.up,           0,      'RuntimesOnHost.up was not initially correct');
             assert.equal(runtimesOnHost.down,         0,      'RuntimesOnHost.down was not initially correct');
             assert.equal(runtimesOnHost.unknown,      0,      'RuntimesOnHost.unknown was not initially correct');
             assert.equal(runtimesOnHost.partial,      0,      'RuntimesOnHost.partial was not initially correct');
             assert.equal(runtimesOnHost.empty,        0,      'RuntimesOnHost.empty was not initially correct');
             assert.isTrue(Array.isArray(runtimesOnHost.list), 'RuntimesOnHost.list was not an Array');
             assert.equal(runtimesOnHost.list.length,  0,      'RuntimesOnHost.list.length was not initially empty');
    
             assert.isNotNull(observer.newList,                'RuntimesOnHostObserver.newList did not get set, when it should have been');
             assert.equal(observer.newList.length,     0,      'RuntimesOnHostObserver.newList was not of expected size');
             assert.isNotNull(observer.oldList,                'RuntimesOnHostObserver.oldList did not get set, when it should have been');
             assert.equal(observer.oldList.length,     1,      'RuntimesOnHostObserver.oldList was not empty');
             assert.equal(observer.oldList[0].id, 'localhost,/wlp', 'RuntimesOnHostObserver.oldList[0].id did not have the correct value for the object');
             assert.isUndefined(observer.added,                'RuntimesOnHostObserver.added got set when it should not have been');
             assert.isNotNull(observer.removed,                'RuntimesOnHostObserver.removed did not get set, when it should have been');
             assert.equal(observer.removed.length,     1,      'RuntimesOnHostObserver.removed was not empty');
             assert.equal(observer.removed[0].id, 'localhost,/wlp', 'RuntimesOnHostObserver.removed[0] did not have the correct value for the object');
           }),
           
           tdd.test('Host changes - middle runtime removed', function() {
             var host = new Host({id: 'localhost', runtimes: {list: [{id:'localhost,/wlp', name:'localhost,/wlp', type:'runtime'},
                                                                     {id:'localhost,/opt/wlp', name:'localhost,/opt/wlp', type:'runtime'},
                                                                     {id:'localhost,/var/wlp', name:'localhost,/var/wlp', type:'runtime'} ]}, 
                                                              servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
             var runtimesOnHost = new RuntimesOnHost({host: host, runtime: [{id: 'localhost,/wlp', servers: {up: 0, down: 0, unknown: 0}}, {id: 'localhost,/opt/wlp', servers: {up: 0, down: 0, unknown: 0}}, {id: 'localhost,/var/wlp', servers: {up: 0, down: 0, unknown: 0}}]});
             var observer = new RuntimesOnHostObserver();
    
             runtimesOnHost.subscribe(observer);
    
             host._handleChangeEvent({type: 'host', id: 'localhost', runtimes: {  removed: [{id:'localhost,/opt/wlp', type: 'runtime', name: 'localhost,/opt/wlp'}] } });
    
             assert.equal(runtimesOnHost.up,           0,           'RuntimesOnHost.up was not updated correct');
             assert.equal(runtimesOnHost.down,         0,           'RuntimesOnHost.down was not updated correct');
             assert.equal(runtimesOnHost.unknown,      0,           'RuntimesOnHost.unknown was not updated correct');
             assert.equal(runtimesOnHost.partial,      0,           'RuntimesOnHost.partial was not updated correct');
             assert.equal(runtimesOnHost.empty,        2,           'RuntimesOnHost.empty was not updated correct');
             assert.isTrue(Array.isArray(runtimesOnHost.list),      'RuntimesOnHost.list was not an Array');
             assert.equal(runtimesOnHost.list.length,  2,           'RuntimesOnHost.list.length was not updated empty');
             assert.equal(runtimesOnHost.list[0].id, 'localhost,/wlp', 'RuntimesOnHost.list[0].id did not have the correct value for the object');
             assert.equal(runtimesOnHost.list[1].id, 'localhost,/var/wlp', 'RuntimesOnHost.list[1].id did not have the correct value for the object');
    
             assert.isNotNull(observer.newList,                     'RuntimesOnHostObserver.newList did not get set, when it should have been');
             assert.equal(observer.newList.length,     2,           'RuntimesOnHostObserver.newList was not of expected size');
             assert.equal(observer.newList[0].id, 'localhost,/wlp', 'RuntimesOnHostObserver.newList[0].id did not have the correct value for the object');
             assert.equal(observer.newList[1].id, 'localhost,/var/wlp', 'RuntimesOnHostObserver.newList[1].id did not have the correct value for the object');
             assert.isNotNull(observer.oldList,                     'RuntimesOnHostObserver.oldList did not get set, when it should have been');
             assert.equal(observer.oldList.length,     3,           'RuntimesOnHostObserver.oldList was not empty');
             assert.equal(observer.oldList[0].id, 'localhost,/wlp', 'RuntimesOnHostObserver.oldList[0].id did not have the correct value for the object');
             assert.equal(observer.oldList[1].id, 'localhost,/opt/wlp', 'RuntimesOnHostObserver.oldList[1].id did not have the correct value for the object');
             assert.equal(observer.oldList[2].id, 'localhost,/var/wlp', 'RuntimesOnHostObserver.oldList[2].id did not have the correct value for the object');
             assert.isUndefined(observer.added,                     'RuntimesOnHostObserver.added got set when it should not have been');
             assert.isNotNull(observer.removed,                     'RuntimesOnHostObserver.removed did not get set, when it should have been');
             assert.equal(observer.removed.length,     1,           'RuntimesOnHostObserver.removed was not empty');
             assert.equal(observer.removed[0].id, 'localhost,/opt/wlp', 'RuntimesOnHostObserver.removed[0] did not have the correct value for the object');
           }),
    
           tdd.test('Host changes - host was removed from the collective', function() {
             var host = new Host({id: 'localhost', runtimes: {list: []}, servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
             var runtimesOnHost = new RuntimesOnHost({host: host, runtime: []});
             var observer = new RuntimesOnHostObserver();
    
             runtimesOnHost.subscribe(observer);
    
             // Host is removed from collective
             host._handleChangeEvent({type: 'host', id: 'localhost', state: 'removed'});
    
             assert.isTrue(runtimesOnHost.isDestroyed,    'RuntimesOnHost.isDestroyed flag did not get set in response to a "removed" event');
             
             // Confirm the application is destroyed
             assert.isTrue(observer.destroyed,            'RuntimesOnHostObserver.onDestroyed did not get called');
           });
        });
    }
});
