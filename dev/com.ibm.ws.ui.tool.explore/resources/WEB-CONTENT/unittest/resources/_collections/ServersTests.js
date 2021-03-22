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
 * Test cases for Servers
 */
define([
        "intern!tdd",
        "intern/chai!assert",
        "intern/dojo/declare",
        "resources/_collections/Servers",
        "resources/Observer"
        ],
        
    function(tdd, assert, declare, Servers, Observer) {

    var ServersObserver = declare([Observer], {
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
      }
    });
  
    with(assert) {
      /**
       * Defines the 'Servers Collection Tests' module test suite.
       */
      tdd.suite('Servers Collection Tests', function() {
  
           tdd.test('constructor - no initialization object', function() {
             try {
               new Servers();
               assert.isTrue(false, 'Servers was successfully created when it should have failed - an initialization object is required');
             } catch(error) {
               assert.equal(error, 'Servers created without an initialization object', 'Error reported did not match expected error');
             }
           });
    
           tdd.test('constructor - no initial up', function() {
             try {
               new Servers({down: 0, unknown: 0, list: []});
               assert.isTrue(false, 'Servers was successfully created when it should have failed - an initial up value is required');
             } catch(error) {
               assert.equal(error, 'Servers created without an initial up tally', 'Error reported did not match expected error');
             }
           });
    
           tdd.test('constructor - no initial down', function() {
             try {
               new Servers({up: 0, unknown: 0, list: []});
               assert.isTrue(false, 'Servers was successfully created when it should have failed - an initial down value is required');
             } catch(error) {
               assert.equal(error, 'Servers created without an initial down tally', 'Error reported did not match expected error');
             }
           });
    
           tdd.test('constructor - no initial unknown', function() {
             try {
               new Servers({up: 0, down: 0, list: []});
               assert.isTrue(false, 'Servers was successfully created when it should have failed - an initial unknown value is required');
             } catch(error) {
               assert.equal(error, 'Servers created without an initial unknown tally', 'Error reported did not match expected error');
             }
           });
    
           tdd.test('constructor - no initial list', function() {
             try {
               new Servers({up: 0, down: 0, unknown: 0});
               assert.isTrue(false, 'Servers was successfully created when it should have failed - an initial list is required');
             } catch(error) {
               assert.equal(error, 'Servers created without an initial list of server names', 'Error reported did not match expected error');
             }
           });
    
           tdd.test('constructor - empty servers', function() {
             var servers = new Servers({up: 0, down: 0, unknown: 0, list: []});
    
             assert.equal(servers.up,      0, 'Servers.up did not have the correct initialized value');
             assert.equal(servers.down,    0, 'Servers.down did not have the correct initialized value');
             assert.equal(servers.unknown, 0, 'Servers.unknown did not have the correct initialized value');
    
             assert.isNotNull(servers.list,            'Servers.list was not present');
             assert.equal(servers.list.length, 0, 'Servers.list was not empty');
           });
    
           tdd.test('constructor - with servers', function() {
             var servers = new Servers({up: 2, down: 3, unknown: 4, list: ['localhost,/wlp,server1', 'localhost,/wlp,server2']});
    
             assert.equal(servers.up,      2, 'Servers.up did not have the correct initialized value');
             assert.equal(servers.down,    3, 'Servers.down did not have the correct initialized value');
             assert.equal(servers.unknown, 4, 'Servers.unknown did not have the correct initialized value');
    
             assert.isNotNull(servers.list,            'Servers.list was not present');
             assert.equal(servers.list.length, 2, 'Servers.list did not have the correct number of elements');
             assert.equal(servers.list[0], 'localhost,/wlp,server1', 'Servers.list[0] did not have the correct value');
             assert.equal(servers.list[1], 'localhost,/wlp,server2', 'Servers.list[1] did not have the correct value');
           });
    
           tdd.test('handleChangeEvent - set tallies only', function() {
             var servers = new Servers({up: 0, down: 0, unknown: 0, list: []});
    
             // Simulate event
             servers._handleChangeEvent({type: 'servers', up: 1, down: 2, unknown: 3});
    
             assert.equal(servers.up,      1, 'Servers.up did not update based on the event value');
             assert.equal(servers.down,    2, 'Servers.down did not update based on the event value');
             assert.equal(servers.unknown, 3, 'Servers.unknown did not update based on the event value');
    
             assert.isNotNull(servers.list,            'Servers.list was not present');
             assert.equal(servers.list.length, 0, 'Servers.list was not empty');
           });
    
           tdd.test('handleChangeEvent - set tallies and add', function() {
             var servers = new Servers({up: 0, down: 0, unknown: 0, list: []});
    
             // Simulate event
             servers._handleChangeEvent({type: 'servers', up: 1, down: 2, unknown: 3, added: ['localhost,/wlp,server1', 'localhost,/wlp,server2']});
    
             assert.equal(servers.up,      1, 'Servers.up did not update based on the event value');
             assert.equal(servers.down,    2, 'Servers.down did not update based on the event value');
             assert.equal(servers.unknown, 3, 'Servers.unknown did not update based on the event value');
    
             assert.isNotNull(servers.list,            'Servers.list was not present');
             assert.equal(servers.list.length, 2, 'Servers.list did not have the correct number of elements');
             assert.equal(servers.list[0], 'localhost,/wlp,server1', 'Servers.list[0] did not have the correct value');
             assert.equal(servers.list[1], 'localhost,/wlp,server2', 'Servers.list[1] did not have the correct value');
           });
    
           tdd.test('handleChangeEvent - set tallies and remove', function() {
             var servers = new Servers({up: 4, down: 0, unknown: 0, list: ['localhost', 'mike', 'lynne', 'melanie']});
    
             // Simulate event
             servers._handleChangeEvent({type: 'servers', up: 1, down: 2, unknown: 3, removed: ['localhost', 'melanie']});
    
             assert.equal(servers.up,      1, 'Servers.up did not update based on the event value');
             assert.equal(servers.down,    2, 'Servers.down did not update based on the event value');
             assert.equal(servers.unknown, 3, 'Servers.unknown did not update based on the event value');
    
             assert.isNotNull(servers.list,              'Servers.list was not present');
             assert.equal(servers.list.length,   2, 'Servers.list did not have the correct number of elements');
             assert.equal(servers.list[0],  'mike', 'Servers.list[0] did not have the correct value');
             assert.equal(servers.list[1], 'lynne', 'Servers.list[1] did not have the correct value');
           });
    
           tdd.test('handleChangeEvent - set tallies, add and remove', function() {
             var servers = new Servers({up: 4, down: 0, unknown: 0, list: ['localhost', 'mike', 'lynne', 'melanie']});
    
             // Simulate event
             servers._handleChangeEvent({type: 'servers', up: 1, down: 2, unknown: 3, added: ['phoenix'], removed: ['localhost', 'melanie']});
    
             assert.equal(servers.up,      1, 'Servers.up did not update based on the event value');
             assert.equal(servers.down,    2, 'Servers.down did not update based on the event value');
             assert.equal(servers.unknown, 3, 'Servers.unknown did not update based on the event value');
    
             assert.isNotNull(servers.list,                'Servers.list was not present');
             assert.equal(servers.list.length,     3, 'Servers.list did not have the correct number of elements');
             assert.equal(servers.list[0],    'mike', 'Servers.list[0] did not have the correct value');
             assert.equal(servers.list[1],   'lynne', 'Servers.list[1] did not have the correct value');
             assert.equal(servers.list[2], 'phoenix', 'Servers.list[2] did not have the correct value');
           });
    
           tdd.test('handleChangeEvent - unset or wrong event type is ignored', function() {
             var servers = new Servers({up: 0, down: 0, unknown: 0,list: []});
    
             // Simulate ignored events
             servers._handleChangeEvent({up: 1, down: 2, unknown: 3, added: ['localhost,/wlp,server1', 'localhost,/wlp,server2']});
             servers._handleChangeEvent({type: 'hosts', up: 1, down: 2, unknown: 3, added: ['localhost,/wlp,server1', 'localhost,/wlp,server2']});
    
             assert.equal(servers.up,      0, 'Servers.up was changed when it should not have been, as the event should have been ignored');
             assert.equal(servers.down,    0, 'Servers.down was changed when it should not have been, as the event should have been ignored');
             assert.equal(servers.unknown, 0, 'Servers.unknown was changed when it should not have been, as the event should have been ignored');
    
             assert.isNotNull(servers.list,            'Servers.list was not present');
             assert.equal(servers.list.length, 0, 'Servers.list was not empty');
           });
           
           tdd.test('handleChangeEvent - invocation of Observer', function() {
             var observer = new ServersObserver();
             var servers = new Servers({up: 0, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']});
    
             servers.subscribe(observer);
    
             // Send event 1
             servers._handleChangeEvent({type: 'servers', up: 1, down: 2, unknown: 3 });
    
             // Validate the Observer was passed the correct tally objects after the first event
             assert.isNotNull(observer.newTally,             'event1: ServersObserver.newTally did not get set, when it should have been');
             assert.equal(observer.newTally.up,      1, 'event1: ServersObserver did not get the correct new value for the up tally');
             assert.equal(observer.newTally.down,    2, 'event1: ServersObserver did not get the correct new value for the down tally');
             assert.equal(observer.newTally.unknown, 3, 'event1: ServersObserver did not get the correct new value for the unknown tally');
    
             assert.isNotNull(observer.oldTally,             'event1: ServersObserver.oldTally did not get set, when it should have been');
             assert.equal(observer.oldTally.up,      0, 'event1: ServersObserver did not get the correct old value for the up tally');
             assert.equal(observer.oldTally.down,    0, 'event1: ServersObserver did not get the correct old value for the down tally');
             assert.equal(observer.oldTally.unknown, 0, 'event1: ServersObserver did not get the correct old value for the unknown tally');
    
             assert.isUndefined(observer.newList,             'event1: ServersObserver had the newList get set, when it should not have been changed');
             assert.isUndefined(observer.oldList,             'event1: ServersObserver had the oldList get set, when it should not have been changed');
    
             // Send event 2
             servers._handleChangeEvent({type: 'servers', up: 4, down: 5, unknown: 6, added: ['localhost,/wlp/usr,server2'], removed: ['localhost,/wlp/usr,server1']});
    
             // Validate the Observer was passed the correct tally objects after the second event
             assert.equal(observer.newTally.up,      4, 'event2: ServersObserver did not get the correct new value for the up tally');
             assert.equal(observer.newTally.down,    5, 'event2: ServersObserver did not get the correct new value for the down tally');
             assert.equal(observer.newTally.unknown, 6, 'event2: ServersObserver did not get the correct new value for the unknown tally');
    
             assert.equal(observer.oldTally.up,      1, 'event2: ServersObserver did not get the correct old value for the up tally');
             assert.equal(observer.oldTally.down,    2, 'event2: ServersObserver did not get the correct old value for the down tally');
             assert.equal(observer.oldTally.unknown, 3, 'event2: ServersObserver did not get the correct old value for the unknown tally');
    
             assert.isNotNull(observer.newList,              'event2: ServersObserver.newList did not get set, when it should have been');
             assert.equal(observer.newList.length,   1, 'event2: ServersObserver.newList was not of expected size');
             assert.equal(observer.newList[0], 'localhost,/wlp/usr,server2', 'event2: ServersObserver.newList[0] was not of expected value');
             assert.isNotNull(observer.oldList,              'event2: ServersObserver.oldList did not get set, when it should have been');
             assert.equal(observer.oldList.length,   1, 'event2: ServersObserver.oldList was not empty');
             assert.equal(observer.oldList[0], 'localhost,/wlp/usr,server1', 'event2: ServersObserver.oldList[0] was not of expected value');
             assert.isNotNull(observer.added,                'event2: ServersObserver.added did not get set, when it should have been');
             assert.equal(observer.added.length,     1, 'event2: ServersObserver.added was not of expected size');
             assert.equal(observer.added[0], 'localhost,/wlp/usr,server2', 'event2: ServersObserver.added[0] was not of expected value');
             assert.isNotNull(observer.removed,              'event2: ServersObserver.removed did not get set, when it should have been');
             assert.equal(observer.removed.length,   1, 'event2: ServersObserver.removed was not empty');
             assert.equal(observer.removed[0], 'localhost,/wlp/usr,server1', 'event2: ServersObserver.removed[0] was not of expected value');
           });
    
      });
    }
});
