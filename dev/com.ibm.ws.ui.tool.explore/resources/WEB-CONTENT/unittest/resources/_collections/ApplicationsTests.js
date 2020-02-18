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
 * Test cases for Applications
 */
define([
     "intern!tdd",
     "intern/chai!assert",
     "intern/dojo/declare",
     "resources/_collections/Applications",
     "resources/Observer"
       ],
       
    function(tdd, assert, declare, Applications, Observer) {

    var ApplicationsObserver = declare([Observer], {
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
       * Defines the 'Applications Collection Tests' module test suite.
       */
      tdd.suite('Applications Collection Tests', function() {
           tdd.test('constructor - no initialization object', function() {
             try {
               new Applications();
               assert.isTrue(false, 'Applications was successfully created when it should have failed - an initialization object is required');
             } catch(error) {
               assert.equal(error, 'Applications created without an initialization object', 'Error reported did not match expected error');
             }
           }),
    
           tdd.test('constructor - no initial up', function() {
             try {
               new Applications({down: 0, unknown: 0, partial: 0, list: []});
               assert.isTrue(false, 'Applications was successfully created when it should have failed - an initial up value is required');
             } catch(error) {
               assert.equal(error, 'Applications created without an initial up tally', 'Error reported did not match expected error');
             }
           }),
    
           tdd.test('constructor - no initial down', function() {
             try {
               new Applications({up: 0, unknown: 0, partial: 0, list: []});
               assert.isTrue(false, 'Applications was successfully created when it should have failed - an initial down value is required');
             } catch(error) {
               assert.equal(error, 'Applications created without an initial down tally', 'Error reported did not match expected error');
             }
           }),
    
           tdd.test('constructor - no initial unknown', function() {
             try {
               new Applications({up: 0, down: 0, partial: 0, list: []});
               assert.isTrue(false, 'Applications was successfully created when it should have failed - an initial unknown value is required');
             } catch(error) {
               assert.equal(error, 'Applications created without an initial unknown tally', 'Error reported did not match expected error');
             }
           }),
    
           tdd.test('constructor - no initial partial', function() {
             try {
               new Applications({up: 0, down: 0, unknown: 0, list: []});
               assert.isTrue(false, 'Applications was successfully created when it should have failed - an initial partial value is required');
             } catch(error) {
               assert.equal(error, 'Applications created without an initial partial tally', 'Error reported did not match expected error');
             }
           }),
    
           tdd.test('constructor - no initial list', function() {
             try {
               new Applications({up: 0, down: 0, unknown: 0, partial: 0});
               assert.isTrue(false, 'Applications was successfully created when it should have failed - an initial list is required');
             } catch(error) {
               assert.equal(error, 'Applications created without an initial list of application names', 'Error reported did not match expected error');
             }
           }),
    
           tdd.test('constructor - empty applications', function() {
             var applications = new Applications({up: 0, down: 0, unknown: 0, partial: 0, list: []});
    
             assert.equal(applications.up,      0, 'Applications.up did not have the correct initialized value');
             assert.equal(applications.down,    0, 'Applications.down did not have the correct initialized value');
             assert.equal(applications.unknown, 0, 'Applications.unknown did not have the correct initialized value');
             assert.equal(applications.partial, 0, 'Applications.partial did not have the correct initialized value');
    
             assert.isNotNull(applications.list,            'Applications.list was not present');
             assert.equal(0, applications.list.length, 'Applications.list was not empty');
           }),
    
           tdd.test('constructor - with applications', function() {
             var applications = new Applications({up: 2, down: 3, unknown: 4, partial: 5, list: ['snoop', 'melanie']});
    
             assert.equal(applications.up,      2, 'Applications.up did not have the correct initialized value');
             assert.equal(applications.down,    3, 'Applications.down did not have the correct initialized value');
             assert.equal(applications.unknown, 4, 'Applications.unknown did not have the correct initialized value');
             assert.equal(applications.partial, 5, 'Applications.partial did not have the correct initialized value');
    
             assert.isNotNull(applications.list,                'Applications.list was not present');
             assert.equal(applications.list.length,     2, 'Applications.list did not have the correct number of elements');
             assert.equal(applications.list[0],   'snoop', 'Applications.list[0] did not have the correct value');
             assert.equal(applications.list[1], 'melanie', 'Applications.list[1] did not have the correct value');
           }),
    
           tdd.test('handleChangeEvent - set tallies only', function() {
             var applications = new Applications({up: 0, down: 0, unknown: 0, partial: 0, list: []});
    
             // Simulate event
             applications._handleChangeEvent({type: 'applications', up: 1, down: 2, unknown: 3, partial: 4});
    
             assert.equal(applications.up,      1, 'Applications.up did not update based on the event value');
             assert.equal(applications.down,    2, 'Applications.down did not update based on the event value');
             assert.equal(applications.unknown, 3, 'Applications.unknown did not update based on the event value');
             assert.equal(applications.partial, 4, 'Applications.partial did not update based on the event value');
    
             assert.isNotNull(applications.list,            'Applications.list was not present');
             assert.equal(applications.list.length, 0, 'Applications.list was not empty');
           }),
    
           tdd.test('handleChangeEvent - set tallies and add new applications', function() {
             var applications = new Applications({up: 0, down: 0, unknown: 0, partial: 0, list: []});
    
             // Simulate event
             applications._handleChangeEvent({type: 'applications', up: 1, down: 2, unknown: 3, partial: 4, added: ['snoop', 'melanie']});
    
             assert.equal(applications.up,      1, 'Applications.up did not update based on the event value');
             assert.equal(applications.down,    2, 'Applications.down did not update based on the event value');
             assert.equal(applications.unknown, 3, 'Applications.unknown did not update based on the event value');
             assert.equal(applications.partial, 4, 'Applications.partial did not update based on the event value');
    
             assert.isNotNull(applications.list,                'Applications.list was not present');
             assert.equal(applications.list.length,     2, 'Applications.list did not have the correct number of elements');
             assert.equal(applications.list[0],   'snoop', 'Applications.list[0] did not have the correct value');
             assert.equal(applications.list[1], 'melanie', 'Applications.list[1] did not have the correct value');
           }),
    
           tdd.test('handleChangeEvent - set tallies and remove existing applications', function() {
             var applications = new Applications({up: 4, down: 0, unknown: 0, partial: 0, list: ['snoop', 'mike', 'lynne', 'melanie']});
    
             // Simulate event
             applications._handleChangeEvent({type: 'applications', up: 1, down: 2, unknown: 3, partial: 4, removed: ['snoop', 'melanie']});
    
             assert.equal(applications.up,      1, 'Applications.up did not update based on the event value');
             assert.equal(applications.down,    2, 'Applications.down did not update based on the event value');
             assert.equal(applications.unknown, 3, 'Applications.unknown did not update based on the event value');
             assert.equal(applications.partial, 4, 'Applications.partial did not update based on the event value');
    
             assert.isNotNull(applications.list,              'Applications.list was not present');
             assert.equal(applications.list.length,   2, 'Applications.list did not have the correct number of elements');
             assert.equal(applications.list[0],  'mike', 'Applications.list[0] did not have the correct value');
             assert.equal(applications.list[1], 'lynne', 'Applications.list[1] did not have the correct value');
           }),
    
           tdd.test('handleChangeEvent - set tallies, add and remove', function() {
             var applications = new Applications({up: 4, down: 0, unknown: 0, partial: 0, list: ['snoop', 'mike', 'lynne', 'melanie']});
    
             // Simulate event
             applications._handleChangeEvent({type: 'applications', up: 1, down: 2, unknown: 3, partial: 4, added: ['phoenix'], removed: ['snoop', 'melanie']});
    
             assert.equal(applications.up,      1, 'Applications.up did not update based on the event value');
             assert.equal(applications.down,    2, 'Applications.down did not update based on the event value');
             assert.equal(applications.unknown, 3, 'Applications.unknown did not update based on the event value');
             assert.equal(applications.partial, 4, 'Applications.partial did not update based on the event value');
    
             assert.isNotNull(applications.list,                'Applications.list was not present');
             assert.equal(applications.list.length,     3, 'Applications.list did not have the correct number of elements');
             assert.equal(applications.list[0],    'mike', 'Applications.list[0] did not have the correct value');
             assert.equal(applications.list[1],   'lynne', 'Applications.list[1] did not have the correct value');
             assert.equal(applications.list[2], 'phoenix', 'Applications.list[2] did not have the correct value');
           }),
    
           tdd.test('handleChangeEvent - unset or wrong event type is ignored', function() {
             var applications = new Applications({up: 0, down: 0, unknown: 0, partial: 0, list: []});
    
             // Simulate ignored events
             applications._handleChangeEvent({up: 1, down: 2, unknown: 3, partial: 4, added: ['snoop', 'melanie']});
             applications._handleChangeEvent({type: 'servers', up: 1, down: 2, unknown: 3, partial: 4, added: ['snoop', 'melanie']});
    
             assert.equal(applications.up,      0, 'Applications.up was changed when it should not have been, as the event should have been ignored');
             assert.equal(applications.down,    0, 'Applications.down was changed when it should not have been, as the event should have been ignored');
             assert.equal(applications.unknown, 0, 'Applications.unknown was changed when it should not have been, as the event should have been ignored');
             assert.equal(applications.partial, 0, 'Applications.partial was changed when it should not have been, as the event should have been ignored');
    
             assert.isNotNull(applications.list,            'Applications.list was not present');
             assert.equal(applications.list.length, 0, 'Applications.list was not empty');
           }),
    
           tdd.test('handleChangeEvent - invocation of Observer', function() {
             var observer = new ApplicationsObserver();
             var applications = new Applications({up: 0, down: 0, unknown: 0, partial: 0, list: ['melanie']});
    
             applications.subscribe(observer);
    
             // Send event 1
             applications._handleChangeEvent({type: 'applications', up: 1, down: 2, unknown: 3, partial: 4});
    
             // Validate the Observer was passed the correct tally objects after the first event
             assert.isNotNull(observer.newTally,             'event1: AppObserver.newTally did not get set, when it should have been');
             assert.equal(observer.newTally.up,      1, 'event1: AppObserver did not get the correct new value for the up tally');
             assert.equal(observer.newTally.down,    2, 'event1: AppObserver did not get the correct new value for the down tally');
             assert.equal(observer.newTally.unknown, 3, 'event1: AppObserver did not get the correct new value for the unknown tally');
             assert.equal(observer.newTally.partial, 4, 'event1: AppObserver did not get the correct new value for the partial tally');
    
             assert.isNotNull(observer.oldTally,             'event1: AppObserver.oldTally did not get set, when it should have been');
             assert.equal(observer.oldTally.up,      0, 'event1: AppObserver did not get the correct old value for the up tally');
             assert.equal(observer.oldTally.down,    0, 'event1: AppObserver did not get the correct old value for the down tally');
             assert.equal(observer.oldTally.unknown, 0, 'event1: AppObserver did not get the correct old value for the unknown tally');
             assert.equal(observer.oldTally.partial, 0, 'event1: AppObserver did not get the correct old value for the partial tally');
    
             assert.isUndefined(observer.newList,             'event1: AppObserver had the newList get set, when it should not have been changed');
             assert.isUndefined(observer.oldList,             'event1: AppObserver had the oldList get set, when it should not have been changed');
    
             // Send event 2
             applications._handleChangeEvent({type: 'applications', up: 5, down: 6, unknown: 7, partial: 8, added: ['snoop'], removed: ['melanie']});
    
             // Validate the Observer was passed the correct tally objects after the second event
             assert.equal(observer.newTally.up,      5, 'event2: AppObserver did not get the correct new value for the up tally');
             assert.equal(observer.newTally.down,    6, 'event2: AppObserver did not get the correct new value for the down tally');
             assert.equal(observer.newTally.unknown, 7, 'event2: AppObserver did not get the correct new value for the unknown tally');
             assert.equal(observer.newTally.partial, 8, 'event2: AppObserver did not get the correct new value for the partial tally');
    
             assert.equal(observer.oldTally.up,      1, 'event2: AppObserver did not get the correct old value for the up tally');
             assert.equal(observer.oldTally.down,    2, 'event2: AppObserver did not get the correct old value for the down tally');
             assert.equal(observer.oldTally.unknown, 3, 'event2: AppObserver did not get the correct old value for the unknown tally');
             assert.equal(observer.oldTally.partial, 4, 'event2: AppObserver did not get the correct old value for the partial tally');
    
             assert.isNotNull(observer.newList,              'event2: AppObserver.newList did not get set, when it should have been');
             assert.equal(observer.newList.length,   1, 'event2: AppObserver.newList was not of expected size');
             assert.equal(observer.newList[0], 'snoop', 'event2: AppObserver.newList[0] was not of expected value');
             assert.isNotNull(observer.oldList,              'event2: AppObserver.oldList did not get set, when it should have been');
             assert.equal(observer.oldList.length,   1, 'event2: AppObserver.oldList was not empty');
             assert.equal(observer.oldList[0], 'melanie', 'event2: AppObserver.oldList[0] was not of expected value');
             assert.isNotNull(observer.added,                'event2: AppObserver.added did not get set, when it should have been');
             assert.equal(observer.added.length,     1, 'event2: AppObserver.added was not of expected size');
             assert.equal(observer.added[0],   'snoop', 'event2: AppObserver.added[0] was not of expected value');
             assert.isNotNull(observer.removed,              'event2: AppObserver.removed did not get set, when it should have been');
             assert.equal(observer.removed.length,   1, 'event2: AppObserver.removed was not empty');
             assert.equal(observer.removed[0], 'melanie', 'event2: AppObserver.removed[0] was not of expected value');
           });
      });
    }
});
