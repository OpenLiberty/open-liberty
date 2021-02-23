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
 * Test cases for Clusters
 */
define([
        "intern!tdd",
        "intern/chai!assert",
        "intern/dojo/declare",
        "resources/_collections/Clusters",
        "resources/Observer"
        ],
        
    function(tdd, assert, declare, Clusters, Observer) {


  var ClustersObserver = declare([Observer], {
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
     * Defines the 'Clusters Collection Tests' module test suite.
     */
    tdd.suite('Clusters Collection Tests', function() {
      
         tdd.test('constructor - no initialization object', function() {
           try {
             new Clusters();
             assert.isTrue(false, 'Clusters was successfully created when it should have failed - an initialization object is required');
           } catch(error) {
             assert.equal(error, 'Clusters created without an initialization object', 'Error reported did not match expected error');
           }
         });
  
         tdd.test('constructor - no initial up', function() {
           try {
             new Clusters({down: 0, unknown: 0, partial: 0, list: []});
             assert.isTrue(false, 'Clusters was successfully created when it should have failed - an initial up value is required');
           } catch(error) {
             assert.equal(error, 'Clusters created without an initial up tally', 'Error reported did not match expected error');
           }
         });
  
         tdd.test('constructor - no initial down', function() {
           try {
             new Clusters({up: 0, unknown: 0, partial: 0, list: []});
             assert.isTrue(false, 'Clusters was successfully created when it should have failed - an initial down value is required');
           } catch(error) {
             assert.equal(error, 'Clusters created without an initial down tally', 'Error reported did not match expected error');
           }
         });
  
         tdd.test('constructor - no initial unknown', function() {
           try {
             new Clusters({up: 0, down: 0, partial: 0, list: []});
             assert.isTrue(false, 'Clusters was successfully created when it should have failed - an initial unknown value is required');
           } catch(error) {
             assert.equal(error, 'Clusters created without an initial unknown tally', 'Error reported did not match expected error');
           }
         });
  
         tdd.test('constructor - no initial partial', function() {
           try {
             new Clusters({up: 0, down: 0, unknown: 0, list: []});
             assert.isTrue(false, 'Clusters was successfully created when it should have failed - an initial partial value is required');
           } catch(error) {
             assert.equal(error, 'Clusters created without an initial partial tally', 'Error reported did not match expected error');
           }
         });
  
         tdd.test('constructor - no initial list', function() {
           try {
             new Clusters({up: 0, down: 0, unknown: 0, partial: 0});
             assert.isTrue(false, 'Clusters was successfully created when it should have failed - an initial list is required');
           } catch(error) {
             assert.equal(error, 'Clusters created without an initial list of cluster names', 'Error reported did not match expected error');
           }
         });
  
         tdd.test('constructor - empty clusters', function() {
           var clusters = new Clusters({up: 0, down: 0, unknown: 0, partial: 0, list: []});
  
           assert.equal(clusters.up,      0, 'Clusters.up did not have the correct initialized value');
           assert.equal(clusters.down,    0, 'Clusters.down did not have the correct initialized value');
           assert.equal(clusters.unknown, 0, 'Clusters.unknown did not have the correct initialized value');
           assert.equal(clusters.partial, 0, 'Clusters.partial did not have the correct initialized value');
  
           assert.isNotNull(clusters.list,            'Clusters.list was not present');
           assert.equal(clusters.list.length, 0, 'Clusters.list was not empty');
         });
  
         tdd.test('constructor - with clusters', function() {
           var clusters = new Clusters({up: 2, down: 3, unknown: 4, partial: 5, list: ['cluster1', 'cluster2']});
  
           assert.equal(clusters.up,      2, 'Clusters.up did not have the correct initialized value');
           assert.equal(clusters.down,    3, 'Clusters.down did not have the correct initialized value');
           assert.equal(clusters.unknown, 4, 'Clusters.unknown did not have the correct initialized value');
           assert.equal(clusters.partial, 5, 'Clusters.partial did not have the correct initialized value');
  
           assert.isNotNull(clusters.list,                  'Clusters.list was not present');
           assert.equal(clusters.list.length,       2, 'Clusters.list did not have the correct number of elements');
           assert.equal(clusters.list[0],  'cluster1', 'Clusters.list[0] did not have the correct value');
           assert.equal(clusters.list[1],  'cluster2', 'Clusters.list[1] did not have the correct value');
         });
  
         tdd.test('handleChangeEvent - set tallies only', function() {
           var clusters = new Clusters({up: 0, down: 0, unknown: 0, partial: 0, list: []});
  
           // Simulate event
           clusters._handleChangeEvent({type: 'clusters', up: 1, down: 2, unknown: 3, partial: 4});
  
           assert.equal(clusters.up,      1, 'Clusters.up did not update based on the event value');
           assert.equal(clusters.down,    2, 'Clusters.down did not update based on the event value');
           assert.equal(clusters.unknown, 3, 'Clusters.unknown did not update based on the event value');
           assert.equal(clusters.partial, 4, 'Clusters.partial did not update based on the event value');
  
           assert.isNotNull(clusters.list,            'Clusters.list was not present');
           assert.equal(clusters.list.length, 0, 'Clusters.list was not empty');
         });
  
         tdd.test('handleChangeEvent - set tallies and add', function() {
           var clusters = new Clusters({up: 0, down: 0, unknown: 0, partial: 0, list: []});
  
           // Simulate event
           clusters._handleChangeEvent({type: 'clusters', up: 1, down: 2, unknown: 3, partial: 4, added: ['cluster1', 'cluster2']});
  
           assert.equal(clusters.up,      1, 'Clusters.up did not update based on the event value');
           assert.equal(clusters.down,    2, 'Clusters.down did not update based on the event value');
           assert.equal(clusters.unknown, 3, 'Clusters.unknown did not update based on the event value');
           assert.equal(clusters.partial, 4, 'Clusters.partial did not update based on the event value');
  
           assert.isNotNull(clusters.list,                  'Clusters.list was not present');
           assert.equal(clusters.list.length,       2, 'Clusters.list did not have the correct number of elements');
           assert.equal(clusters.list[0],  'cluster1', 'Clusters.list[0] did not have the correct value');
           assert.equal(clusters.list[1],  'cluster2', 'Clusters.list[1] did not have the correct value');
         });
  
         tdd.test('handleChangeEvent - set tallies and remove', function() {
           var clusters = new Clusters({up: 4, down: 0, unknown: 0, partial: 0, list: ['cluster1', 'mike', 'lynne', 'cluster2']});
  
           // Simulate event
           clusters._handleChangeEvent({type: 'clusters', up: 1, down: 2, unknown: 3, partial: 4, removed: ['cluster1', 'cluster2']});
  
           assert.equal(clusters.up,      1, 'Clusters.up did not update based on the event value');
           assert.equal(clusters.down,    2, 'Clusters.down did not update based on the event value');
           assert.equal(clusters.unknown, 3, 'Clusters.unknown did not update based on the event value');
           assert.equal(clusters.partial, 4, 'Clusters.partial did not update based on the event value');
  
           assert.isNotNull(clusters.list,              'Clusters.list was not present');
           assert.equal(clusters.list.length,   2, 'Clusters.list did not have the correct number of elements');
           assert.equal(clusters.list[0],  'mike', 'Clusters.list[0] did not have the correct value');
           assert.equal(clusters.list[1], 'lynne', 'Clusters.list[1] did not have the correct value');
         });
  
         tdd.test('handleChangeEvent - set tallies, add and remove', function() {
           var clusters = new Clusters({up: 4, down: 0, unknown: 0, partial: 0, list: ['cluster1', 'mike', 'lynne', 'cluster2']});
  
           // Simulate event
           clusters._handleChangeEvent({type: 'clusters', up: 1, down: 2, unknown: 3, partial: 4, added: ['phoenix'], removed: ['cluster1', 'cluster2']});
  
           assert.equal(clusters.up,      1, 'Clusters.up did not update based on the event value');
           assert.equal(clusters.down,    2, 'Clusters.down did not update based on the event value');
           assert.equal(clusters.unknown, 3, 'Clusters.unknown did not update based on the event value');
           assert.equal(clusters.partial, 4, 'Clusters.partial did not update based on the event value');
  
           assert.isNotNull(clusters.list,                'Clusters.list was not present');
           assert.equal(clusters.list.length,     3, 'Clusters.list did not have the correct number of elements');
           assert.equal(clusters.list[0],    'mike', 'Clusters.list[0] did not have the correct value');
           assert.equal(clusters.list[1],   'lynne', 'Clusters.list[1] did not have the correct value');
           assert.equal(clusters.list[2], 'phoenix', 'Clusters.list[2] did not have the correct value');
         });
  
         tdd.test('handleChangeEvent - unset or wrong event type is ignored', function() {
           var clusters = new Clusters({up: 0, down: 0, unknown: 0, partial: 0, list: []});
  
           // Simulate ignored events
           clusters._handleChangeEvent({up: 1, down: 2, unknown: 3, partial: 4, added: ['cluster1', 'cluster2']});
           clusters._handleChangeEvent({type: 'servers', up: 1, down: 2, unknown: 3, partial: 4, added: ['cluster1', 'cluster2']});
  
           assert.equal(clusters.up,      0, 'Clusters.up was changed when it should not have been, as the event should have been ignored');
           assert.equal(clusters.down,    0, 'Clusters.down was changed when it should not have been, as the event should have been ignored');
           assert.equal(clusters.unknown, 0, 'Clusters.unknown was changed when it should not have been, as the event should have been ignored');
           assert.equal(clusters.partial, 0, 'Clusters.partial was changed when it should not have been, as the event should have been ignored');
  
           assert.isNotNull(clusters.list,            'Clusters.list was not present');
           assert.equal(clusters.list.length, 0, 'Clusters.list was not empty');
         });
  
         tdd.test('handleChangeEvent - invocation of Observer', function() {
           var observer = new ClustersObserver();
           var clusters = new Clusters({up: 0, down: 0, unknown: 0, partial: 0, list: ['cluster1']});
  
           clusters.subscribe(observer);
  
           // Send event 1
           clusters._handleChangeEvent({type: 'clusters', up: 1, down: 2, unknown: 3, partial: 4});
  
           // Validate the Observer was passed the correct tally objects after the first event
           assert.isNotNull(observer.newTally,             'event1: ClustersObserver.newTally did not get set, when it should have been');
           assert.equal(observer.newTally.up,      1, 'event1: ClustersObserver did not get the correct new value for the up tally');
           assert.equal(observer.newTally.down,    2, 'event1: ClustersObserver did not get the correct new value for the down tally');
           assert.equal(observer.newTally.unknown, 3, 'event1: ClustersObserver did not get the correct new value for the unknown tally');
           assert.equal(observer.newTally.partial, 4, 'event1: ClustersObserver did not get the correct new value for the partial tally');
  
           assert.isNotNull(observer.oldTally,             'event1: ClustersObserver.oldTally did not get set, when it should have been');
           assert.equal( observer.oldTally.up,      0,'event1: ClustersObserver did not get the correct old value for the up tally');
           assert.equal(observer.oldTally.down,    0, 'event1: ClustersObserver did not get the correct old value for the down tally');
           assert.equal(observer.oldTally.unknown, 0, 'event1: ClustersObserver did not get the correct old value for the unknown tally');
           assert.equal(observer.oldTally.partial, 0, 'event1: ClustersObserver did not get the correct old value for the partial tally');
  
           assert.isUndefined(observer.newList,             'event1: ClustersObserver had the newList get set, when it should not have been changed');
           assert.isUndefined(observer.oldList,             'event1: ClustersObserver had the oldList get set, when it should not have been changed');
  
           // Send event 2
           clusters._handleChangeEvent({type: 'clusters', up: 5, down: 6, unknown: 7, partial: 8, added: ['cluster2'], removed: ['cluster1']});
  
           // Validate the Observer was passed the correct tally objects after the second event
           assert.equal(observer.newTally.up,      5, 'event2: ClustersObserver did not get the correct new value for the up tally');
           assert.equal(observer.newTally.down,    6, 'event2: ClustersObserver did not get the correct new value for the down tally');
           assert.equal(observer.newTally.unknown, 7, 'event2: ClustersObserver did not get the correct new value for the unknown tally');
           assert.equal(observer.newTally.partial, 8, 'event2: ClustersObserver did not get the correct new value for the partial tally');
  
           assert.equal(observer.oldTally.up,      1, 'event2: ClustersObserver did not get the correct old value for the up tally');
           assert.equal(observer.oldTally.down,    2, 'event2: ClustersObserver did not get the correct old value for the down tally');
           assert.equal(observer.oldTally.unknown, 3, 'event2: ClustersObserver did not get the correct old value for the unknown tally');
           assert.equal(observer.oldTally.partial, 4, 'event2: ClustersObserver did not get the correct old value for the partial tally');
  
           assert.isNotNull(observer.newList,              'event2: ClustersObserver.newList did not get set, when it should have been');
           assert.equal(observer.newList.length,   1, 'event2: ClustersObserver.newList was not of expected size');
           assert.equal(observer.newList[0], 'cluster2', 'event2: ClustersObserver.newList[0] was not of expected value');
           assert.isNotNull(observer.oldList,              'event2: ClustersObserver.oldList did not get set, when it should have been');
           assert.equal(observer.oldList.length,   1, 'event2: ClustersObserver.oldList was not empty');
           assert.equal(observer.oldList[0], 'cluster1', 'event2: ClustersObserver.oldList[0] was not of expected value');
           assert.isNotNull(observer.added,                'event2: ClustersObserver.added did not get set, when it should have been');
           assert.equal(observer.added.length,     1, 'event2: ClustersObserver.added was not of expected size');
           assert.equal(observer.added[0], 'cluster2', 'event2: ClustersObserver.added[0] was not of expected value');
           assert.isNotNull(observer.removed,              'event2: ClustersObserver.removed did not get set, when it should have been');
           assert.equal(observer.removed.length,   1, 'event2: ClustersObserver.removed was not empty');
           assert.equal(observer.removed[0], 'cluster1', 'event2: ClustersObserver.removed[0] was not of expected value');
         });
    });
  }
});
