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
 * Test cases for ServersOnCluster
 */
define([ 
        "intern!tdd",
        "intern/chai!assert",
        "intern/dojo/declare",
        "resources/_derived/collections/ServersOnCluster",
        "resources/_objects/Cluster",
        "resources/Observer",
        "dojo/Deferred"
        ],
        
    function(tdd, assert, declare, ServersOnCluster, Cluster, Observer, Deferred) {

      var ServersOnClusterObserver = declare([Observer], {
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
         * Defines the 'ServersOnCluster Collection Tests' module test suite.
         */
        tdd.suite('ServersOnCluster Collection Tests', function() {
  
             tdd.test('constructor - no initialization object', function() {
               try {
                 new ServersOnCluster();
                 assert.isTrue(false, 'ServersOnCluster was successfully created when it should have failed - an initialization object is required');
               } catch(error) {
                 assert.equal(error, 'ServersOnCluster created without an initialization object', 'Error reported did not match expected error');
               }
             }),
      
             tdd.test('constructor - no cluster', function() {
               try {
                 new ServersOnCluster({servers: []});
                 assert.isTrue(false, 'ServersOnCluster was successfully created when it should have failed - a cluster is required');
               } catch(error) {
                 assert.equal(error, 'ServersOnCluster created without a Cluster', 'Error reported did not match expected error');
               }
             }),
             
             tdd.test('constructor - no servers', function() {
               try {
                 new ServersOnCluster({cluster: {}});
                 assert.isTrue(false, 'ServersOnCluster was successfully created when it should have failed - an array of servers is required');
               } catch(error) {
                 assert.equal(error, 'ServersOnCluster created without an array of Server', 'Error reported did not match expected error');
               }
             }),
      
             tdd.test('constructor - cluster with no servers', function() {
               var cluster = new Cluster({id: 'cluster1', state: 'STARTED', servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
               var serversOnCluster = new ServersOnCluster({cluster: cluster, servers: []});
      
               assert.equal(serversOnCluster.id, 'serversOnCluster(cluster1)', 'ServersOnCluster.id did not have the correct initialized value');
               assert.equal(serversOnCluster.up,          0,           'ServersOnCluster.up did not have the correct initialized value');
               assert.equal(serversOnCluster.down,        0,           'ServersOnCluster.down did not have the correct initialized value');
               assert.equal(serversOnCluster.unknown,     0,           'ServersOnCluster.unknown did not have the correct initialized value');
               assert.isTrue(Array.isArray(serversOnCluster.list),       'ServersOnCluster.list was not an Array');
               assert.equal(serversOnCluster.list.length, 0,           'ServersOnCluster.list.length was not initially empty');
             }),
      
             tdd.test('constructor - cluster with servers', function() {
               var cluster = new Cluster({id: 'cluster1', state: 'STARTED', servers: {up: 1, down: 2, unknown: 3, list: ['localhost,/wlp/usr,server1']}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
               var serversOnCluster = new ServersOnCluster({cluster: cluster, servers: [{id: 'localhost,/wlp/usr,server1'}]});
      
               assert.equal(serversOnCluster.id, 'serversOnCluster(cluster1)', 'ServersOnCluster.id did not have the correct initialized value');
               assert.equal(serversOnCluster.up,          1,           'ServersOnCluster.up did not have the correct initialized value');
               assert.equal(serversOnCluster.down,        2,           'ServersOnCluster.down did not have the correct initialized value');
               assert.equal(serversOnCluster.unknown,     3,           'ServersOnCluster.unknown did not have the correct initialized value');
               assert.isTrue(Array.isArray(serversOnCluster.list),       'ServersOnCluster.list was not an Array');
               assert.equal(serversOnCluster.list.length, 1,           'ServersOnCluster.list.length was not the correct value');
               assert.equal(serversOnCluster.list[0].id, 'localhost,/wlp/usr,server1', 'ServersOnCluster.list[0].id did not have the correct value for the object name');
             }),
      
             tdd.test('Cluster changes - servers tallies changed', function() {
               var cluster = new Cluster({id: 'cluster1', state: 'STARTED', servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
               var serversOnCluster = new ServersOnCluster({cluster: cluster, servers: []});
               var observer = new ServersOnClusterObserver();
      
               serversOnCluster.subscribe(observer);
      
               assert.equal(serversOnCluster.up,          0,           'ServersOnCluster.up did not have the correct initialized value');
               assert.equal(serversOnCluster.down,        0,           'ServersOnCluster.down did not have the correct initialized value');
               assert.equal(serversOnCluster.unknown,     0,           'ServersOnCluster.unknown did not have the correct initialized value');
               assert.isTrue(Array.isArray(serversOnCluster.list),       'ServersOnCluster.list was not an Array');
               assert.equal(serversOnCluster.list.length, 0,           'ServersOnCluster.list.length was not initially empty');
      
               // Trigger the server's onAppsTallyChange method via the Server event handler
               cluster._handleChangeEvent({type: 'cluster', id: 'cluster1', servers: { up: 1, down: 2, unknown: 3 } });
      
               assert.equal(serversOnCluster.up,          1,           'ServersOnCluster.up did not have the correct updated value');
               assert.equal(serversOnCluster.down,        2,           'ServersOnCluster.down did not have the correct updated value');
               assert.equal(serversOnCluster.unknown,     3,           'ServersOnCluster.unknown did not have the correct updated value');
               assert.isTrue(Array.isArray(serversOnCluster.list),       'ServersOnCluster.list was not an Array');
               assert.equal(serversOnCluster.list.length, 0,           'ServersOnCluster.list.length was not initially empty');
      
               // Validate the Observer was passed the correct tally objects after the first event
               assert.isNotNull(observer.newTally,             'ServersOnClusterObserver.newTally did not get set, when it should have been');
               assert.equal(observer.newTally.up,      1, 'ServersOnClusterObserver did not get the correct new value for the up tally');
               assert.equal(observer.newTally.down,    2, 'ServersOnClusterObserver did not get the correct new value for the down tally');
               assert.equal(observer.newTally.unknown, 3, 'ServersOnClusterObserver did not get the correct new value for the unknown tally');
      
               assert.isNotNull(observer.oldTally,             'ServersOnClusterObserver.oldTally did not get set, when it should have been');
               assert.equal(observer.oldTally.up,      0, 'ServersOnClusterObserver did not get the correct old value for the up tally');
               assert.equal(observer.oldTally.down,    0, 'ServersOnClusterObserver did not get the correct old value for the down tally');
               assert.equal(observer.oldTally.unknown, 0, 'ServersOnClusterObserver did not get the correct old value for the unknown tally');
             }),
      
             tdd.test('Cluster changes - added server', function() {
               var cluster = new Cluster({id: 'cluster1', state: 'STARTED', servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
               var serversOnCluster = new ServersOnCluster({cluster: cluster, servers: []});
               var observer = new ServersOnClusterObserver();
      
               serversOnCluster.subscribe(observer);
      
               // TODO: This mock is needed because of the temporary behaviour here we don't store names
               serversOnCluster.resourceManager = {
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
                   assert.equal(serversOnCluster.up,          1,           'ServersOnCluster.up did not have the correct updated value');
                   assert.equal(serversOnCluster.down,        0,           'ServersOnCluster.down did not have the correct updated value');
                   assert.equal(serversOnCluster.unknown,     0,           'ServersOnCluster.unknown did not have the correct updated value');
                   assert.isTrue(Array.isArray(serversOnCluster.list),       'ServersOnCluster.list was not an Array');
                   assert.equal(serversOnCluster.list.length, 1,           'ServersOnCluster.list.length was not the correct updated value');
                   assert.equal(serversOnCluster.list[0].id, 'localhost,/wlp/usr,server1', 'ServersOnCluster.list[0].id did not have the correct updated value for the object name');
      
                   // Validate the Observer was passed the correct tally objects after the first event
                   assert.isNotNull(observer.newTally,             'ServersOnClusterObserver.newTally did not get set, when it should have been');
                   assert.equal(observer.newTally.up,      1, 'ServersOnClusterObserver did not get the correct new value for the up tally');
                   assert.equal(observer.newTally.down,    0, 'ServersOnClusterObserver did not get the correct new value for the down tally');
                   assert.equal(observer.newTally.unknown, 0, 'ServersOnClusterObserver did not get the correct new value for the unknown tally');
      
                   assert.isNotNull(observer.oldTally,             'ServersOnClusterObserver.oldTally did not get set, when it should have been');
                   assert.equal(observer.oldTally.up,      0, 'ServersOnClusterObserver did not get the correct old value for the up tally');
                   assert.equal(observer.oldTally.down,    0, 'ServersOnClusterObserver did not get the correct old value for the down tally');
                   assert.equal(observer.oldTally.unknown, 0, 'ServersOnClusterObserver did not get the correct old value for the unknown tally');
      
                   assert.isNotNull(newList,              'ServersOnClusterObserver.newList did not get set, when it should have been');
                   assert.equal(newList.length,   1, 'ServersOnClusterObserver.newList was not of expected size');
                   assert.equal(newList[0].id, 'localhost,/wlp/usr,server1', 'ServersOnClusterObserver.newList[0].id was not of expected value');
                   assert.isNotNull(oldList,              'ServersOnClusterObserver.oldList did not get set, when it should have been');
                   assert.equal(oldList.length,   0, 'ServersOnClusterObserver.oldList was not empty');
                   assert.isNotNull(added,                'ServersOnClusterObserver.added did not get set, when it should have been');
                   assert.equal(added.length,     1, 'ServersOnClusterObserver.added was not of expected size');
                   assert.equal(added[0], 'localhost,/wlp/usr,server1', 'ServersOnClusterObserver.added[0] was not of expected value');
                   assert.isUndefined(removed,             'ServersOnClusterObserver.removed got set when it should not have been');
      
                   dfd.resolve('OK');
                 } catch(err) {
                   dfd.reject(err);
                 }
               };
      
               // Trigger the server's onAppsTallyChange and onAppsListChange method via the Server event handler
               cluster._handleChangeEvent({type: 'cluster', id: 'cluster1', servers: {up: 1, down: 0, unknown: 0, added: ['localhost,/wlp/usr,server1']}});
      
               return dfd;
             }),
      
             tdd.test('Cluster changes - removed server', function() {
               var cluster = new Cluster({id: 'cluster1', state: 'STARTED', servers: {up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
               var serversOnCluster = new ServersOnCluster({cluster: cluster, servers: [{id: 'localhost,/wlp/usr,server1'}]});
               var observer = new ServersOnClusterObserver();
      
               serversOnCluster.subscribe(observer);
      
               assert.equal(serversOnCluster.up,          1,           'ServersOnCluster.up did not have the correct initialized value');
               assert.equal(serversOnCluster.down,        0,           'ServersOnCluster.down did not have the correct initialized value');
               assert.equal(serversOnCluster.unknown,     0,           'ServersOnCluster.unknown did not have the correct initialized value');
               assert.isTrue(Array.isArray(serversOnCluster.list),       'ServersOnCluster.list was not an Array');
               assert.equal(serversOnCluster.list.length, 1,           'ServersOnCluster.list.length was not the correct initial value');
               assert.equal(serversOnCluster.list[0].id, 'localhost,/wlp/usr,server1',     'ServersOnCluster.list[0].id did not have the correct value for the object name');
      
               // Trigger the server's onAppsTallyChange method via the event handler
               cluster._handleChangeEvent({type: 'cluster', id: 'cluster1', servers: {up: 0, down: 0, unknown: 0, removed: ['localhost,/wlp/usr,server1']}});
      
               assert.equal(serversOnCluster.up,          0,           'ServersOnCluster.up did not have the correct updated value');
               assert.equal(serversOnCluster.down,        0,           'ServersOnCluster.down did not have the correct updated value');
               assert.equal(serversOnCluster.unknown,     0,           'ServersOnCluster.unknown did not have the correct updated value');
               assert.isTrue(Array.isArray(serversOnCluster.list),       'ServersOnCluster.list was not an Array');
               assert.equal(serversOnCluster.list.length, 0,           'ServersOnCluster.list.length was not updated to be empty');
      
               // Validate the Observer was passed the correct tally objects after the first event
               assert.isNotNull(observer.newTally,             'ServersOnClusterObserver.newTally did not get set, when it should have been');
               assert.equal(observer.newTally.up,      0, 'ServersOnClusterObserver did not get the correct new value for the up tally');
               assert.equal(observer.newTally.down,    0, 'ServersOnClusterObserver did not get the correct new value for the down tally');
               assert.equal(observer.newTally.unknown, 0, 'ServersOnClusterObserver did not get the correct new value for the unknown tally');
      
               assert.isNotNull(observer.oldTally,             'ServersOnClusterObserver.oldTally did not get set, when it should have been');
               assert.equal(observer.oldTally.up,      1, 'ServersOnClusterObserver did not get the correct old value for the up tally');
               assert.equal(observer.oldTally.down,    0, 'ServersOnClusterObserver did not get the correct old value for the down tally');
               assert.equal(observer.oldTally.unknown, 0, 'ServersOnClusterObserver did not get the correct old value for the unknown tally');
      
               assert.isNotNull(observer.newList,              'ServersOnClusterObserver.newList did not get set, when it should have been');
               assert.equal(observer.newList.length,   0, 'ServersOnClusterObserver.newList was not of expected size');
               assert.isNotNull(observer.oldList,              'ServersOnClusterObserver.oldList did not get set, when it should have been');
               assert.equal(observer.oldList.length,   1, 'ServersOnClusterObserver.oldList was not empty');
               assert.equal(observer.oldList[0].id, 'localhost,/wlp/usr,server1', 'ServersOnClusterObserver.oldList[0].id did not have the correct value for the object name');
               assert.isUndefined(observer.added,               'ServersOnClusterObserver.added got set when it should not have been');
               assert.isNotNull(observer.removed,              'ServersOnClusterObserver.removed did not get set, when it should have been');
               assert.equal(observer.removed.length,   1, 'ServersOnClusterObserver.removed was not empty');
               assert.equal(observer.removed[0], 'localhost,/wlp/usr,server1', 'ServersOnClusterObserver.removed[0] did not have the correct value for the object name');
             }),
             
             /**
              * This test is to ensure we have the right splice logic
              */
             tdd.test('Cluster changes - removed middle server', function() {
               var cluster = new Cluster({id: 'cluster1', state: 'STARTED', servers: {up: 3, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1','localhost,/wlp/usr,server2','localhost,/wlp/usr,server3']}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
               var serversOnCluster = new ServersOnCluster({cluster: cluster, servers: [{id: 'localhost,/wlp/usr,server1'},{id: 'localhost,/wlp/usr,server2'},{id: 'localhost,/wlp/usr,server3'}]});
               var observer = new ServersOnClusterObserver();
      
               serversOnCluster.subscribe(observer);
      
               assert.equal(serversOnCluster.up,          3,           'ServersOnCluster.up did not have the correct initialized value');
               assert.equal(serversOnCluster.down,        0,           'ServersOnCluster.down did not have the correct initialized value');
               assert.equal(serversOnCluster.unknown,     0,           'ServersOnCluster.unknown did not have the correct initialized value');
               assert.isTrue(Array.isArray(serversOnCluster.list),       'ServersOnCluster.list was not an Array');
               assert.equal(serversOnCluster.list.length, 3,           'ServersOnCluster.list.length was not the correct initial value');
               assert.equal(serversOnCluster.list[0].id, 'localhost,/wlp/usr,server1', 'ServersOnCluster.list[0].id did not have the correct value for the object name');
               assert.equal(serversOnCluster.list[1].id, 'localhost,/wlp/usr,server2', 'ServersOnCluster.list[1].id did not have the correct value for the object name');
               assert.equal(serversOnCluster.list[2].id, 'localhost,/wlp/usr,server3', 'ServersOnCluster.list[2].id did not have the correct value for the object name');
      
               // Trigger the server's onAppsTallyChange method via the event handler
               cluster._handleChangeEvent({type: 'cluster', id: 'cluster1', servers: {up: 2, down: 0, unknown: 0, removed: ['localhost,/wlp/usr,server2']}});
      
               assert.equal(serversOnCluster.up,          2,           'ServersOnCluster.up did not have the correct updated value');
               assert.equal(serversOnCluster.down,        0,           'ServersOnCluster.down did not have the correct updated value');
               assert.equal(serversOnCluster.unknown,     0,           'ServersOnCluster.unknown did not have the correct updated value');
               assert.isTrue(Array.isArray(serversOnCluster.list),       'ServersOnCluster.list was not an Array');
               assert.equal(serversOnCluster.list.length, 2,           'ServersOnCluster.list.length was not updated to be 2');
               assert.equal(serversOnCluster.list[0].id, 'localhost,/wlp/usr,server1', 'ServersOnCluster.list[0].id did not have the correct updated value for the object name');
               assert.equal(serversOnCluster.list[1].id, 'localhost,/wlp/usr,server3', 'ServersOnCluster.list[1].id did not have the correct updated value for the object name');
      
               // Validate the Observer was passed the correct tally objects after the first event
               assert.isNotNull(observer.newTally,             'ServersOnClusterObserver.newTally did not get set, when it should have been');
               assert.equal(observer.newTally.up,      2, 'ServersOnClusterObserver did not get the correct new value for the up tally');
               assert.equal(observer.newTally.down,    0, 'ServersOnClusterObserver did not get the correct new value for the down tally');
               assert.equal(observer.newTally.unknown, 0, 'ServersOnClusterObserver did not get the correct new value for the unknown tally');
      
               assert.isNotNull(observer.oldTally,             'ServersOnClusterObserver.oldTally did not get set, when it should have been');
               assert.equal(observer.oldTally.up,      3, 'ServersOnClusterObserver did not get the correct old value for the up tally');
               assert.equal(observer.oldTally.down,    0, 'ServersOnClusterObserver did not get the correct old value for the down tally');
               assert.equal(observer.oldTally.unknown, 0, 'ServersOnClusterObserver did not get the correct old value for the unknown tally');
      
               assert.isNotNull(observer.newList,              'ServersOnClusterObserver.newList did not get set, when it should have been');
               assert.equal(observer.newList.length,   2, 'ServersOnClusterObserver.newList was not of expected size');
               assert.equal(observer.newList[0].id, 'localhost,/wlp/usr,server1', 'ServersOnClusterObserver.newList[0].id did not have the correct value for the object name');
               assert.equal(observer.newList[1].id, 'localhost,/wlp/usr,server3', 'ServersOnClusterObserver.newList[1].id did not have the correct value for the object name');
               assert.isNotNull(observer.oldList,              'ServersOnClusterObserver.oldList did not get set, when it should have been');
               assert.equal(observer.oldList.length,   3, 'ServersOnClusterObserver.oldList was not empty');
               assert.equal(observer.oldList[0].id, 'localhost,/wlp/usr,server1', 'ServersOnClusterObserver.oldList[0].id did not have the correct value for the object name');
               assert.equal(observer.oldList[1].id, 'localhost,/wlp/usr,server2', 'ServersOnClusterObserver.oldList[1].id did not have the correct value for the object name');
               assert.equal(observer.oldList[2].id, 'localhost,/wlp/usr,server3', 'ServersOnClusterObserver.oldList[2].id did not have the correct value for the object name');
               assert.isUndefined(observer.added,               'ServersOnClusterObserver.added got set when it should not have been');
               assert.isNotNull(observer.removed,              'ServersOnClusterObserver.removed did not get set, when it should have been');
               assert.equal(observer.removed.length,   1, 'ServersOnClusterObserver.removed was not empty');
               assert.equal(observer.removed[0], 'localhost,/wlp/usr,server2', 'ServersOnClusterObserver.removed[0] did not have the correct value for the object name');
             }),
             
             tdd.test('Cluster changes - cluster was removed from the collective', function() {
               var cluster = new Cluster({id: 'cluster1', state: 'STARTED', servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
               var serversOnCluster = new ServersOnCluster({cluster: cluster, servers: []});
               var observer = new ServersOnClusterObserver();
      
               serversOnCluster.subscribe(observer);
      
               // Host is removed from collective
               cluster._handleChangeEvent({type: 'cluster', id: 'cluster1', state: 'removed'});
      
               assert.isTrue(serversOnCluster.isDestroyed,  'ServersOnCluster.isDestroyed flag did not get set in response to a "removed" event');
               
               // Confirm the application is destroyed
               assert.isTrue(observer.destroyed,            'ServersOnClusterObserver.onDestroyed did not get called');
             });
          });
      }
});
