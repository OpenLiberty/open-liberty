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
 * Test cases for Hosts
 */
define([
        "intern!tdd",
        "intern/chai!assert",
        "intern/dojo/declare",
        "resources/_collections/Hosts",
        "resources/Observer"
        ],
        
    function(tdd, assert, declare, Hosts, Observer) {

    var HostsObserver = declare([Observer], {
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
     * Defines the 'Hosts Collection Tests' module test suite.
     */
    tdd.suite('Hosts Collection Tests', function() {
      
         tdd.test('constructor - no initialization object', function() {
           try {
             new Hosts();
             assert.isTrue(false, 'Hosts was successfully created when it should have failed - an initialization object is required');
           } catch(error) {
             assert.equal(error, 'Hosts created without an initialization object', 'Error reported did not match expected error');
           }
         }),
  
         tdd.test('constructor - no initial up', function() {
           try {
             new Hosts({down: 0, unknown: 0, partial: 0, empty: 0, list: []});
             assert.isTrue(false, 'Hosts was successfully created when it should have failed - an initial up value is required');
           } catch(error) {
             assert.equal(error, 'Hosts created without an initial up tally', 'Error reported did not match expected error');
           }
         }),
  
         tdd.test('constructor - no initial down', function() {
           try {
             new Hosts({up: 0, unknown: 0, partial: 0, empty: 0, list: []});
             assert.isTrue(false, 'Hosts was successfully created when it should have failed - an initial down value is required');
           } catch(error) {
             assert.equal(error, 'Hosts created without an initial down tally', 'Error reported did not match expected error');
           }
         }),
  
         tdd.test('constructor - no initial unknown', function() {
           try {
             new Hosts({up: 0, down: 0, partial: 0, empty: 0, list: []});
             assert.isTrue(false, 'Hosts was successfully created when it should have failed - an initial unknown value is required');
           } catch(error) {
             assert.equal(error, 'Hosts created without an initial unknown tally', 'Error reported did not match expected error');
           }
         }),
  
         tdd.test('constructor - no initial partial', function() {
           try {
             new Hosts({up: 0, down: 0, unknown: 0, empty: 0, list: []});
             assert.isTrue(false, 'Hosts was successfully created when it should have failed - an initial partial value is required');
           } catch(error) {
             assert.equal(error, 'Hosts created without an initial partial tally', 'Error reported did not match expected error');
           }
         }),
  
         tdd.test('constructor - no initial empty', function() {
           try {
             new Hosts({up: 0, down: 0, unknown: 0, partial: 0, list: []});
             assert.isTrue(false, 'Hosts was successfully created when it should have failed - an initial empty value is required');
           } catch(error) {
             assert.equal(error, 'Hosts created without an initial empty tally', 'Error reported did not match expected error');
           }
         }),
  
         tdd.test('constructor - no initial list', function() {
           try {
             new Hosts({up: 0, down: 0, unknown: 0, partial: 0, empty: 0});
             assert.isTrue(false, 'Hosts was successfully created when it should have failed - an initial list is required');
           } catch(error) {
             assert.equal(error, 'Hosts created without an initial list of host names', 'Error reported did not match expected error');
           }
         }),
  
         tdd.test('constructor - empty hosts', function() {
           var hosts = new Hosts({up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
  
           assert.equal(hosts.up,      0, 'Hosts.up did not have the correct initialized value');
           assert.equal(hosts.down,    0, 'Hosts.down did not have the correct initialized value');
           assert.equal(hosts.unknown, 0, 'Hosts.unknown did not have the correct initialized value');
           assert.equal(hosts.partial, 0, 'Hosts.partial did not have the correct initialized value');
           assert.equal(hosts.empty,   0, 'Hosts.unknown did not have the correct initialized value');
  
           assert.isNotNull(hosts.list,            'Hosts.list was not present');
           assert.equal(hosts.list.length, 0, 'Hosts.list was not empty');
         }),
  
         tdd.test('constructor - with hosts', function() {
           var hosts = new Hosts({up: 2, down: 3, unknown: 4, partial: 5, empty: 6, list: ['localhost', 'melanie']});
  
           assert.equal(hosts.up,      2, 'Hosts.up did not have the correct initialized value');
           assert.equal(hosts.down,    3, 'Hosts.down did not have the correct initialized value');
           assert.equal(hosts.unknown, 4, 'Hosts.unknown did not have the correct initialized value');
           assert.equal(hosts.partial, 5, 'Hosts.partial did not have the correct initialized value');
           assert.equal(hosts.empty,   6, 'Hosts.unknown did not have the correct initialized value');
  
           assert.isNotNull(hosts.list,                  'Hosts.list was not present');
           assert.equal(hosts.list.length,       2, 'Hosts.list did not have the correct number of elements');
           assert.equal(hosts.list[0], 'localhost', 'Hosts.list[0] did not have the correct value');
           assert.equal(hosts.list[1],   'melanie', 'Hosts.list[1] did not have the correct value');
         }),
  
         tdd.test('handleChangeEvent - set tallies only', function() {
           var hosts = new Hosts({up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
  
           // Simulate event
           hosts._handleChangeEvent({type: 'hosts', up: 1, down: 2, unknown: 3, partial: 4, empty: 5});
  
           assert.equal(hosts.up,      1, 'Hosts.up did not update based on the event value');
           assert.equal(hosts.down,    2, 'Hosts.down did not update based on the event value');
           assert.equal(hosts.unknown, 3, 'Hosts.unknown did not update based on the event value');
           assert.equal(hosts.partial, 4, 'Hosts.partial did not update based on the event value');
           assert.equal(hosts.empty,   5, 'Hosts.unknown did not update based on the event value');
  
           assert.isNotNull(hosts.list,            'Hosts.list was not present');
           assert.equal(hosts.list.length, 0, 'Hosts.list was not empty');
         }),
  
         tdd.test('handleChangeEvent - set tallies and add', function() {
           var hosts = new Hosts({up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
  
           // Simulate event
           hosts._handleChangeEvent({type: 'hosts', up: 1, down: 2, unknown: 3, partial: 4, empty: 5, added: ['localhost', 'melanie']});
  
           assert.equal(hosts.up,      1, 'Hosts.up did not update based on the event value');
           assert.equal(hosts.down,    2, 'Hosts.down did not update based on the event value');
           assert.equal(hosts.unknown, 3, 'Hosts.unknown did not update based on the event value');
           assert.equal(hosts.partial, 4, 'Hosts.partial did not update based on the event value');
           assert.equal(hosts.empty,   5, 'Hosts.unknown did not update based on the event value');
  
           assert.isNotNull(hosts.list,                  'Hosts.list was not present');
           assert.equal(hosts.list.length,       2, 'Hosts.list did not have the correct number of elements');
           assert.equal(hosts.list[0], 'localhost', 'Hosts.list[0] did not have the correct value');
           assert.equal(hosts.list[1],   'melanie', 'Hosts.list[1] did not have the correct value');
         }),
  
         tdd.test('handleChangeEvent - set tallies and remove', function() {
           var hosts = new Hosts({up: 4, down: 0, unknown: 0, partial: 0, empty: 0, list: ['localhost', 'mike', 'lynne', 'melanie']});
  
           // Simulate event
           hosts._handleChangeEvent({type: 'hosts', up: 1, down: 2, unknown: 3, partial: 4, empty: 5, removed: ['localhost', 'melanie']});
  
           assert.equal(hosts.up,      1, 'Hosts.up did not update based on the event value');
           assert.equal(hosts.down,    2, 'Hosts.down did not update based on the event value');
           assert.equal(hosts.unknown, 3, 'Hosts.unknown did not update based on the event value');
           assert.equal(hosts.partial, 4, 'Hosts.partial did not update based on the event value');
           assert.equal(hosts.empty,   5, 'Hosts.unknown did not update based on the event value');
  
           assert.isNotNull(hosts.list,              'Hosts.list was not present');
           assert.equal(hosts.list.length,   2, 'Hosts.list did not have the correct number of elements');
           assert.equal(hosts.list[0],  'mike', 'Hosts.list[0] did not have the correct value');
           assert.equal(hosts.list[1], 'lynne', 'Hosts.list[1] did not have the correct value');
         }),
  
         tdd.test('handleChangeEvent - set tallies, add and remove', function() {
           var hosts = new Hosts({up: 4, down: 0, unknown: 0, partial: 0, empty: 0, list: ['localhost', 'mike', 'lynne', 'melanie']});
  
           // Simulate event
           hosts._handleChangeEvent({type: 'hosts', up: 1, down: 2, unknown: 3, partial: 4, empty: 5, added: ['phoenix'], removed: ['localhost', 'melanie']});
  
           assert.equal(hosts.up,      1, 'Hosts.up did not update based on the event value');
           assert.equal(hosts.down,    2, 'Hosts.down did not update based on the event value');
           assert.equal(hosts.unknown, 3, 'Hosts.unknown did not update based on the event value');
           assert.equal(hosts.partial, 4, 'Hosts.partial did not update based on the event value');
           assert.equal(hosts.empty,   5, 'Hosts.unknown did not update based on the event value');
  
           assert.isNotNull(hosts.list,                'Hosts.list was not present');
           assert.equal(hosts.list.length,     3, 'Hosts.list did not have the correct number of elements');
           assert.equal(hosts.list[0],    'mike', 'Hosts.list[0] did not have the correct value');
           assert.equal(hosts.list[1],   'lynne', 'Hosts.list[1] did not have the correct value');
           assert.equal(hosts.list[2], 'phoenix', 'Hosts.list[2] did not have the correct value');
         }),
  
         tdd.test('handleChangeEvent - unset or wrong event type is ignored', function() {
           var hosts = new Hosts({up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: []});
  
           // Simulate ignored events
           hosts._handleChangeEvent({up: 1, down: 2, unknown: 3, partial: 4, empty: 5, added: ['localhost', 'melanie']});
           hosts._handleChangeEvent({type: 'servers', up: 1, down: 2, unknown: 3, partial: 4, empty: 5, added: ['localhost', 'melanie']});
  
           assert.equal(hosts.up,      0, 'Hosts.up was changed when it should not have been, as the event should have been ignored');
           assert.equal(hosts.down,    0, 'Hosts.down was changed when it should not have been, as the event should have been ignored');
           assert.equal(hosts.unknown, 0, 'Hosts.unknown was changed when it should not have been, as the event should have been ignored');
           assert.equal(hosts.partial, 0, 'Hosts.partial was changed when it should not have been, as the event should have been ignored');
           assert.equal(hosts.empty,   0, 'Hosts.unknown was changed when it should not have been, as the event should have been ignored');
  
           assert.isNotNull(hosts.list,            'Hosts.list was not present');
           assert.equal(hosts.list.length, 0, 'Hosts.list was not empty');
         }),
         
         tdd.test('handleChangeEvent - invocation of Observer', function() {
           var observer = new HostsObserver();
           var hosts = new Hosts({up: 0, down: 0, unknown: 0, partial: 0, empty: 0, list: ['localhost']});
  
           hosts.subscribe(observer);
  
           // Send event 1
           hosts._handleChangeEvent({type: 'hosts', up: 1, down: 2, unknown: 3, partial: 4, empty: 5});
  
           // Validate the Observer was passed the correct tally objects after the first event
           assert.isNotNull(observer.newTally,             'event1: HostsObserver.newTally did not get set, when it should have been');
           assert.equal(observer.newTally.up,      1, 'event1: HostsObserver did not get the correct new value for the up tally');
           assert.equal(observer.newTally.down,    2, 'event1: HostsObserver did not get the correct new value for the down tally');
           assert.equal(observer.newTally.unknown, 3, 'event1: HostsObserver did not get the correct new value for the unknown tally');
           assert.equal(observer.newTally.partial, 4, 'event1: HostsObserver did not get the correct new value for the partial tally');
           assert.equal(observer.newTally.empty,   5, 'event1: HostsObserver did not get the correct new value for the empty tally');
  
           assert.isNotNull(observer.oldTally,             'event1: HostsObserver.oldTally did not get set, when it should have been');
           assert.equal(observer.oldTally.up,      0, 'event1: HostsObserver did not get the correct old value for the up tally');
           assert.equal(observer.oldTally.down,    0, 'event1: HostsObserver did not get the correct old value for the down tally');
           assert.equal(observer.oldTally.unknown, 0, 'event1: HostsObserver did not get the correct old value for the unknown tally');
           assert.equal(observer.oldTally.partial, 0, 'event1: HostsObserver did not get the correct old value for the partial tally');
           assert.equal(observer.oldTally.empty,   0, 'event1: HostsObserver did not get the correct old value for the empty tally');
  
           assert.isUndefined(observer.newList,             'event1: HostsObserver had the newList get set, when it should not have been changed');
           assert.isUndefined(observer.oldList,             'event1: HostsObserver had the oldList get set, when it should not have been changed');
  
           // Send event 2
           hosts._handleChangeEvent({type: 'hosts', up: 6, down: 7, unknown: 8, partial: 9, empty: 10, added: ['melanie'], removed: ['localhost']});
  
           // Validate the Observer was passed the correct tally objects after the second event
           assert.equal(observer.newTally.up,      6, 'event2: HostsObserver did not get the correct new value for the up tally');
           assert.equal(observer.newTally.down,    7, 'event2: HostsObserver did not get the correct new value for the down tally');
           assert.equal(observer.newTally.unknown, 8, 'event2: HostsObserver did not get the correct new value for the unknown tally');
           assert.equal(observer.newTally.partial, 9, 'event2: HostsObserver did not get the correct new value for the partial tally');
           assert.equal(observer.newTally.empty,  10, 'event2: HostsObserver did not get the correct new value for the empty tally');
  
           assert.equal(observer.oldTally.up,      1, 'event2: HostsObserver did not get the correct old value for the up tally');
           assert.equal(observer.oldTally.down,    2, 'event2: HostsObserver did not get the correct old value for the down tally');
           assert.equal(observer.oldTally.unknown, 3, 'event2: HostsObserver did not get the correct old value for the unknown tally');
           assert.equal(observer.oldTally.partial, 4, 'event2: HostsObserver did not get the correct old value for the partial tally');
           assert.equal(observer.oldTally.empty,   5, 'event1: HostsObserver did not get the correct old value for the empty tally');
  
           assert.isNotNull(observer.newList,              'event2: HostsObserver.newList did not get set, when it should have been');
           assert.equal(observer.newList.length,   1, 'event2: HostsObserver.newList was not of expected size');
           assert.equal(observer.newList[0], 'melanie', 'event2: HostsObserver.newList[0] was not of expected value');
           assert.isNotNull(observer.oldList,              'event2: HostsObserver.oldList did not get set, when it should have been');
           assert.equal(observer.oldList.length,   1, 'event2: HostsObserver.oldList was not empty');
           assert.equal(observer.oldList[0], 'localhost', 'event2: HostsObserver.oldList[0] was not of expected value');
           assert.isNotNull(observer.added,                'event2: HostsObserver.added did not get set, when it should have been');
           assert.equal(observer.added.length,     1, 'event2: HostsObserver.added was not of expected size');
           assert.equal(observer.added[0], 'melanie', 'event2: HostsObserver.added[0] was not of expected value');
           assert.isNotNull(observer.removed,              'event2: HostsObserver.removed did not get set, when it should have been');
           assert.equal(observer.removed.length,   1, 'event2: HostsObserver.removed was not empty');
           assert.equal(observer.removed[0], 'localhost', 'event2: HostsObserver.removed[0] was not of expected value');
         });
      });
    }
});
