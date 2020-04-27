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
 * Test cases for AppsOnCluster
 */
define([
        "intern!tdd",
        "intern/chai!assert",
        "intern/dojo/declare",
        "dojo/Deferred",
        "resources/_derived/collections/AppsOnCluster",
        "resources/_objects/Cluster",
        "resources/Observer",
        "dojo/aspect"
        ],

        function(tdd, assert, declare, Deferred, AppsOnCluster, Cluster, Observer, Aspect) {

  var AppsOnClusterObserver = declare([Observer], {
    id: 'testObserver',

    onTallyChange: function(newTally, oldTally) {
      this.newTally = newTally;
      this.oldTally = oldTally;
    },

    onAppsListChange: function(newList, oldList, added, removed, changed) {
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

  /**
   * Defines the 'AppsOnCluster Collection Tests' module test suite.
   */
  with(assert) {

    tdd.suite('AppsOnCluster Collection Tests', function() {

      tdd.test('constructor - no initialization object', function() {
        try {
          new AppsOnCluster();
          assert.ok(false, 'AppsOnCluster was successfully created when it should have failed - an initialization object is required');
        } catch(error) {
          assert.equal('AppsOnCluster created without an initialization object', error, 'Error reported did not match expected error');
        }
      }),

      tdd.test('constructor - no cluster', function() {
        try {
          new AppsOnCluster({ appOnCluster: [] });
          assert.ok(false, 'AppsOnCluster was successfully created when it should have failed - a Cluster is required');
        } catch(error) {
          assert.equal('AppsOnCluster created without a Cluster', error, 'Error reported did not match expected error');
        }
      }),

      tdd.test('constructor - no array of AppOnCluster', function() {
        try {
          new AppsOnCluster({ cluster: {/*...*/} });
          assert.ok(false, 'AppsOnCluster was successfully created when it should have failed - an array of AppOnCluster is required');
        } catch(error) {
          assert.equal('AppsOnCluster created without an array of AppOnCluster', error, 'Error reported did not match expected error');
        }
      }),

      tdd.test('constructor - cluster with no apps', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED', servers: {up: 0, down: 0, unknown: 0, list: []}, apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
        var appsOnCluster = new AppsOnCluster({cluster: cluster, appOnCluster: []});

        assert.equal('appsOnCluster(cluster1)', appsOnCluster.id, 'AppsOnCluster.id did not have the correct initialized value');
        assert.equal(0,           appsOnCluster.up,          'AppsOnCluster.up did not have the correct initialized value');
        assert.equal(0,           appsOnCluster.down,        'AppsOnCluster.down did not have the correct initialized value');
        assert.equal(0,           appsOnCluster.unknown,     'AppsOnCluster.unknown did not have the correct initialized value');
        assert.equal(0,           appsOnCluster.partial,     'AppsOnCluster.partial did not have the correct initialized value');
        assert.ok(Array.isArray(appsOnCluster.list),       'AppsOnCluster.list was not an Array');
        assert.equal(0,           appsOnCluster.list.length, 'AppsOnCluster.list.length was not initially empty');
      }),

      tdd.test('constructor - cluster with apps', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED', 
          servers: {up: 0, down: 0, unknown: 0, list: []}, 
          apps: {up: 1, down: 2, unknown: 3, partial: 4, list: [ { name: 'snoop', up: 1, down: 0, unknown: 0, servers: { up: 0, down: 0, unknown: 0, ids: []}} ] } });
        var appsOnCluster = new AppsOnCluster({cluster: cluster, appOnCluster: [ {id: 'snoop(cluster1)', name: 'snoop'} ]});

        assert.equal('appsOnCluster(cluster1)', appsOnCluster.id, 'AppsOnCluster.id did not have the correct initialized value');
        assert.equal(1,           appsOnCluster.up,          'AppsOnCluster.up did not have the correct initialized value');
        assert.equal(2,           appsOnCluster.down,        'AppsOnCluster.down did not have the correct initialized value');
        assert.equal(3,           appsOnCluster.unknown,     'AppsOnCluster.unknown did not have the correct initialized value');
        assert.equal(4,           appsOnCluster.partial,     'AppsOnCluster.partial did not have the correct initialized value');
        assert.ok(Array.isArray(appsOnCluster.list),       'AppsOnCluster.list was not an Array');
        assert.equal(1,           appsOnCluster.list.length, 'AppsOnCluster.list.length was not the correct value');
        assert.equal('snoop(cluster1)', appsOnCluster.list[0].id, 'AppsOnCluster.list[0].id did not have the correct value for the object name');
      }),

      tdd.test('Cluster changes - apps tallies changed', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED',
          servers: {up: 0, down: 0, unknown: 0, list: []},
          apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
        var appsOnCluster = new AppsOnCluster({cluster: cluster, appOnCluster: []});
        var observer = new AppsOnClusterObserver();

        appsOnCluster.subscribe(observer);

        assert.equal(0,           appsOnCluster.up,          'AppsOnCluster.up did not have the correct initialized value');
        assert.equal(0,           appsOnCluster.down,        'AppsOnCluster.down did not have the correct initialized value');
        assert.equal(0,           appsOnCluster.unknown,     'AppsOnCluster.unknown did not have the correct initialized value');
        assert.equal(0,           appsOnCluster.partial,     'AppsOnCluster.partial did not have the correct initialized value');
        assert.ok(Array.isArray(appsOnCluster.list),       'AppsOnCluster.list was not an Array');
        assert.equal(0,           appsOnCluster.list.length, 'AppsOnCluster.list.length was not initially empty');

        // Trigger the Cluster event handler
        cluster._handleChangeEvent({type: 'cluster', id: 'cluster1', apps: { up: 1, down: 2, unknown: 3, partial: 4 } });

        assert.equal(1,           appsOnCluster.up,          'AppsOnCluster.up did not have the correct updated value');
        assert.equal(2,           appsOnCluster.down,        'AppsOnCluster.down did not have the correct updated value');
        assert.equal(3,           appsOnCluster.unknown,     'AppsOnCluster.unknown did not have the correct updated value');
        assert.equal(4,           appsOnCluster.partial,     'AppsOnCluster.partial did not have the correct initialized value');
        assert.ok(Array.isArray(appsOnCluster.list),       'AppsOnCluster.list was not an Array');
        assert.equal(0,           appsOnCluster.list.length, 'AppsOnCluster.list.length was not initially empty');

        // Validate the Observer was passed the correct tally objects after the first event
        assert.ok(observer.newTally,             'AppsOnClusterObserver.newTally did not get set, when it should have been');
        assert.equal(1, observer.newTally.up,      'AppsOnClusterObserver did not get the correct new value for the up tally');
        assert.equal(2, observer.newTally.down,    'AppsOnClusterObserver did not get the correct new value for the down tally');
        assert.equal(3, observer.newTally.unknown, 'AppsOnClusterObserver did not get the correct new value for the unknown tally');
        assert.equal(4, observer.newTally.partial, 'AppsOnClusterObserver did not get the correct new value for the partial tally');

        assert.ok(observer.oldTally,             'AppsOnClusterObserver.oldTally did not get set, when it should have been');
        assert.equal(0, observer.oldTally.up,      'AppsOnClusterObserver did not get the correct old value for the up tally');
        assert.equal(0, observer.oldTally.down,    'AppsOnClusterObserver did not get the correct old value for the down tally');
        assert.equal(0, observer.oldTally.unknown, 'AppsOnClusterObserver did not get the correct old value for the unknown tally');
        assert.equal(0, observer.oldTally.partial, 'AppsOnClusterObserver did not get the correct old value for the partial tally');
      }),

      tdd.test('Cluster changes - added application - onAppsListChange', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED',
          servers: {up: 0, down: 0, unknown: 0, list: []},
          apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
        var appsOnCluster = new AppsOnCluster({cluster: cluster, appOnCluster: []});
        var observer = new AppsOnClusterObserver();

        appsOnCluster.subscribe(observer);

        // TODO: This mock is needed because of the temporary behaviour here we don't store names
        var appOnCluster = {id: 'snoop(cluster1)', name: 'snoop'};
        appsOnCluster.resourceManager = {
            getAppOnCluster: function(cluster, name) {
              var deferred = new Deferred();
              deferred.resolve([appOnCluster], true);
              return deferred;
            }
        };

        var dfd = this.async(1000);

        // Need to wait until the onListChange method fires before we 
        observer.onAppsListChange = function(newList, oldList, added, removed, changed) {
          try {
            assert.ok(newList,              'AppsOnClusterObserver.newList did not get set, when it should have been');
            assert.equal(1, newList.length,   'AppsOnClusterObserver.newList was not of expected size');
            assert.equal(appOnCluster, newList[0], 'AppsOnClusterObserver.newList[0] was not of expected value');
            assert.ok(oldList,              'AppsOnClusterObserver.oldList did not get set, when it should have been');
            assert.equal(0, oldList.length,   'AppsOnClusterObserver.oldList was not empty');
            assert.ok(added,                'AppsOnClusterObserver.added did not get set, when it should have been');
            assert.equal(1, added.length,     'AppsOnClusterObserver.added was not of expected size');
            assert.equal('snoop', added[0],   'AppsOnClusterObserver.added[0] was not of expected value');
            assert.notOk(removed,             'AppsOnClusterObserver.removed got set when it should not have been');

            dfd.resolve('OK');
          } catch(err) {
            dfd.reject(err);
          }
        };

        // Trigger the Cluster's onAppsTallyChange and onAppsListChange method via the Cluster event handler
        cluster._handleChangeEvent({type: 'cluster', id: 'cluster1', apps: {up: 1, down: 0, unknown: 0, partial: 0, added: [ { name: 'snoop', up: 1, down: 0, unknown: 0, servers: { up: 0, down: 0, unknown: 0, ids: []}} ]}});

        return dfd;
      }),
      
      tdd.test('Cluster changes - added application - onTallyChange', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED',
          servers: {up: 0, down: 0, unknown: 0, list: []},
          apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
        var appsOnCluster = new AppsOnCluster({cluster: cluster, appOnCluster: []});
        var observer = new AppsOnClusterObserver();

        appsOnCluster.subscribe(observer);

        // TODO: This mock is needed because of the temporary behaviour here we don't store names
        var appOnCluster = {id: 'snoop(cluster1)', name: 'snoop'};
        appsOnCluster.resourceManager = {
            getAppOnCluster: function(cluster, name) {
              var deferred = new Deferred();
              deferred.resolve([appOnCluster], true);
              return deferred;
            }
        };

        var dfd = this.async(1000);

        // Need to wait until the onListChange method fires before we 
        Aspect.after(observer, "onTallyChange", function(newList, oldList, added, removed, changed) {
          try {
            assert.equal(1,           appsOnCluster.up,          'AppsOnCluster.up did not have the correct updated value');
            assert.equal(0,           appsOnCluster.down,        'AppsOnCluster.down did not have the correct updated value');
            assert.equal(0,           appsOnCluster.unknown,     'AppsOnCluster.unknown did not have the correct updated value');
            assert.equal(0,           appsOnCluster.partial,     'AppsOnCluster.partial did not have the correct updated value');
            assert.ok(Array.isArray(appsOnCluster.list),       'AppsOnCluster.list was not an Array');
            assert.equal(1,           appsOnCluster.list.length, 'AppsOnCluster.list.length was not the correct updated value');
            assert.equal(appOnCluster, appsOnCluster.list[0],    'AppsOnCluster.list[0] did not have the correct updated value');

            // Validate the Observer was passed the correct tally objects after the first event
            assert.ok(observer.newTally,             'AppsOnClusterObserver.newTally did not get set, when it should have been');
            assert.equal(1, observer.newTally.up,      'AppsOnClusterObserver did not get the correct new value for the up tally');
            assert.equal(0, observer.newTally.down,    'AppsOnClusterObserver did not get the correct new value for the down tally');
            assert.equal(0, observer.newTally.unknown, 'AppsOnClusterObserver did not get the correct new value for the unknown tally');
            assert.equal(0, observer.newTally.partial, 'AppsOnClusterObserver did not get the correct new value for the partial tally');

            assert.ok(observer.oldTally,             'AppsOnClusterObserver.oldTally did not get set, when it should have been');
            assert.equal(0, observer.oldTally.up,      'AppsOnClusterObserver did not get the correct old value for the up tally');
            assert.equal(0, observer.oldTally.down,    'AppsOnClusterObserver did not get the correct old value for the down tally');
            assert.equal(0, observer.oldTally.unknown, 'AppsOnClusterObserver did not get the correct old value for the unknown tally');
            assert.equal(0, observer.oldTally.partial, 'AppsOnClusterObserver did not get the correct old value for the partial tally');

            dfd.resolve('OK');
          } catch(err) {
            dfd.reject(err);
          }
        });

        // Trigger the Cluster's onAppsTallyChange and onAppsListChange method via the Cluster event handler
        cluster._handleChangeEvent({type: 'cluster', id: 'cluster1', apps: {up: 1, down: 0, unknown: 0, partial: 0, added: [ { name: 'snoop', up: 1, down: 0, unknown: 0, servers: { up: 0, down: 0, unknown: 0, ids: []}} ]}});

        return dfd;
      }),

      tdd.test('Cluster changes - removed application', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED', 
          servers: {up: 0, down: 0, unknown: 0, list: []}, 
          apps: {up: 1, down: 0, unknown: 0, partial: 0, list: [ {name: 'snoop', up: 1, down: 0, unknown: 0, servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ]}} ] } });
        var appsOnCluster = new AppsOnCluster({cluster: cluster, appOnCluster: [ {id: 'snoop(cluster1)', name: 'snoop'} ]});
        var observer = new AppsOnClusterObserver();

        appsOnCluster.subscribe(observer);

        assert.equal(1,           appsOnCluster.up,          'AppsOnCluster.up did not have the correct initialized value');
        assert.equal(0,           appsOnCluster.down,        'AppsOnCluster.down did not have the correct initialized value');
        assert.equal(0,           appsOnCluster.unknown,     'AppsOnCluster.unknown did not have the correct initialized value');
        assert.equal(0,           appsOnCluster.partial,     'AppsOnCluster.partial did not have the correct initialized value');
        assert.ok(Array.isArray(appsOnCluster.list),       'AppsOnCluster.list was not an Array');
        assert.equal(1,           appsOnCluster.list.length, 'AppsOnCluster.list.length was not the correct initial value');
        assert.equal('snoop(cluster1)', appsOnCluster.list[0].id, 'AppsOnCluster.list[0].id did not have the correct value for the object name');

        // Trigger the Cluster's event handler
        cluster._handleChangeEvent({type: 'cluster', id: 'cluster1', apps: {up: 0, down: 0, unknown: 0, partial: 0, removed: ['snoop']}});

        assert.equal(0,           appsOnCluster.up,          'AppsOnCluster.up did not have the correct updated value');
        assert.equal(0,           appsOnCluster.down,        'AppsOnCluster.down did not have the correct updated value');
        assert.equal(0,           appsOnCluster.unknown,     'AppsOnCluster.unknown did not have the correct updated value');
        assert.equal(0,           appsOnCluster.partial,     'AppsOnCluster.partial did not have the correct updated value');
        assert.ok(Array.isArray(appsOnCluster.list),       'AppsOnCluster.list was not an Array');
        assert.equal(0,           appsOnCluster.list.length, 'AppsOnCluster.list.length was not updated to be empty');

        // Validate the Observer was passed the correct tally objects after the first event
        assert.ok(observer.newTally,             'AppsOnClusterObserver.newTally did not get set, when it should have been');
        assert.equal(0, observer.newTally.up,      'AppsOnClusterObserver did not get the correct new value for the up tally');
        assert.equal(0, observer.newTally.down,    'AppsOnClusterObserver did not get the correct new value for the down tally');
        assert.equal(0, observer.newTally.unknown, 'AppsOnClusterObserver did not get the correct new value for the unknown tally');
        assert.equal(0, observer.newTally.partial, 'AppsOnClusterObserver did not get the correct new value for the partial tally');

        assert.ok(observer.oldTally,             'AppsOnClusterObserver.oldTally did not get set, when it should have been');
        assert.equal(1, observer.oldTally.up,      'AppsOnClusterObserver did not get the correct old value for the up tally');
        assert.equal(0, observer.oldTally.down,    'AppsOnClusterObserver did not get the correct old value for the down tally');
        assert.equal(0, observer.oldTally.unknown, 'AppsOnClusterObserver did not get the correct old value for the unknown tally');
        assert.equal(0, observer.oldTally.partial, 'AppsOnClusterObserver did not get the correct old value for the partial tally');

        assert.ok(observer.newList,              'AppsOnClusterObserver.newList did not get set, when it should have been');
        assert.equal(0, observer.newList.length,   'AppsOnClusterObserver.newList was not of expected size');
        assert.ok(observer.oldList,              'AppsOnClusterObserver.oldList did not get set, when it should have been');
        assert.equal(1, observer.oldList.length,   'AppsOnClusterObserver.oldList was not empty');
        assert.equal('snoop(cluster1)', observer.oldList[0].id, 'AppsOnClusterObserver.oldList[0] did not have the correct value for the object name');
        assert.notOk(observer.added,               'AppsOnClusterObserver.added got set when it should not have been');
        assert.ok(observer.removed,              'AppsOnClusterObserver.removed did not get set, when it should have been');
        assert.equal(1, observer.removed.length,   'AppsOnClusterObserver.removed was not empty');
        assert.equal('snoop', observer.removed[0], 'AppsOnClusterObserver.removed[0] did not have the correct value for the object name');
      }),

      /**
       * This test is to ensure we have the right splice logic
       */
      tdd.test('Cluster changes - removed middle application', function() {
        var cluster = new Cluster({
          id: 'cluster1', 
          state: 'STARTED', 
          servers: {up: 0, down: 0, unknown: 0, list: []}, 
          apps: {up: 3, down: 0, unknown: 0, partial: 0, list: 
            [ {name: 'snoop', up: 1, down: 0, unknown: 0, servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ]}},
              {name: 'melanie', up: 1, down: 0, unknown: 0, servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ]}},
              {name: 'lynne', up: 1, down: 0, unknown: 0, servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ]}}] } });
        var appsOnCluster = new AppsOnCluster({cluster: cluster, appOnCluster: [ {id: 'snoop(cluster1)', name: 'snoop'}, {id: 'melanie(cluster1)', name: 'melanie'}, {id: 'lynne(cluster1)', name: 'lynne'} ]});
        var observer = new AppsOnClusterObserver();

        appsOnCluster.subscribe(observer);

        assert.equal(3,           appsOnCluster.up,          'AppsOnCluster.up did not have the correct initialized value');
        assert.equal(0,           appsOnCluster.down,        'AppsOnCluster.down did not have the correct initialized value');
        assert.equal(0,           appsOnCluster.unknown,     'AppsOnCluster.unknown did not have the correct initialized value');
        assert.equal(0,           appsOnCluster.partial,     'AppsOnCluster.partial did not have the correct initialized value');
        assert.ok(Array.isArray(appsOnCluster.list),       'AppsOnCluster.list was not an Array');
        assert.equal(3,           appsOnCluster.list.length, 'AppsOnCluster.list.length was not the correct initial value');
        assert.equal('snoop(cluster1)',   appsOnCluster.list[0].id, 'AppsOnCluster.list[0].id did not have the correct value for the object id');
        assert.equal('melanie(cluster1)', appsOnCluster.list[1].id, 'AppsOnCluster.list[1].id did not have the correct value for the object id');
        assert.equal('lynne(cluster1)',   appsOnCluster.list[2].id, 'AppsOnCluster.list[2].id did not have the correct value for the object id');

        // Trigger the Cluster's event handler
        cluster._handleChangeEvent({type: 'cluster', id: 'cluster1', apps: {up: 2, down: 0, unknown: 0, partial: 0, removed: ['melanie']}});

        assert.equal(2,           appsOnCluster.up,          'AppsOnCluster.up did not have the correct updated value');
        assert.equal(0,           appsOnCluster.down,        'AppsOnCluster.down did not have the correct updated value');
        assert.equal(0,           appsOnCluster.unknown,     'AppsOnCluster.unknown did not have the correct updated value');
        assert.equal(0,           appsOnCluster.partial,     'AppsOnCluster.partial did not have the correct updated value');
        assert.ok(Array.isArray(appsOnCluster.list),       'AppsOnCluster.list was not an Array');
        assert.equal(2,           appsOnCluster.list.length, 'AppsOnCluster.list.length was not updated to be 2');
        assert.equal('snoop(cluster1)', appsOnCluster.list[0].id, 'AppsOnCluster.list[0].id did not have the correct updated value for the object id');
        assert.equal('lynne(cluster1)', appsOnCluster.list[1].id, 'AppsOnCluster.list[1].id did not have the correct updated value for the object id');

        // Validate the Observer was passed the correct tally objects after the first event
        assert.ok(observer.newTally,             'AppsOnClusterObserver.newTally did not get set, when it should have been');
        assert.equal(2, observer.newTally.up,      'AppsOnClusterObserver did not get the correct new value for the up tally');
        assert.equal(0, observer.newTally.down,    'AppsOnClusterObserver did not get the correct new value for the down tally');
        assert.equal(0, observer.newTally.unknown, 'AppsOnClusterObserver did not get the correct new value for the unknown tally');
        assert.equal(0, observer.newTally.partial, 'AppsOnClusterObserver did not get the correct new value for the partial tally');

        assert.ok(observer.oldTally,             'AppsOnClusterObserver.oldTally did not get set, when it should have been');
        assert.equal(3, observer.oldTally.up,      'AppsOnClusterObserver did not get the correct old value for the up tally');
        assert.equal(0, observer.oldTally.down,    'AppsOnClusterObserver did not get the correct old value for the down tally');
        assert.equal(0, observer.oldTally.unknown, 'AppsOnClusterObserver did not get the correct old value for the unknown tally');
        assert.equal(0, observer.oldTally.partial, 'AppsOnClusterObserver did not get the correct old value for the partial tally');

        assert.ok(observer.newList,              'AppsOnClusterObserver.newList did not get set, when it should have been');
        assert.equal(2, observer.newList.length,   'AppsOnClusterObserver.newList was not of expected size');
        assert.equal('snoop(cluster1)', observer.newList[0].id, 'AppsOnClusterObserver.newList[0].id did not have the correct value for the object id');
        assert.equal('lynne(cluster1)', observer.newList[1].id, 'AppsOnClusterObserver.newList[1].id did not have the correct value for the object id');
        assert.ok(observer.oldList,              'AppsOnClusterObserver.oldList did not get set, when it should have been');
        assert.equal(3, observer.oldList.length,   'AppsOnClusterObserver.oldList was not empty');
        assert.equal('snoop(cluster1)',   observer.oldList[0].id, 'AppsOnClusterObserver.oldList[0] did not have the correct value for the object id');
        assert.equal('melanie(cluster1)', observer.oldList[1].id, 'AppsOnClusterObserver.oldList[1].id did not have the correct value for the object id');
        assert.equal('lynne(cluster1)',   observer.oldList[2].id, 'AppsOnClusterObserver.oldList[2].id did not have the correct value for the object id');
        assert.notOk(observer.added,               'AppsOnClusterObserver.added got set when it should not have been');
        assert.ok(observer.removed,              'AppsOnClusterObserver.removed did not get set, when it should have been');
        assert.equal(1, observer.removed.length,   'AppsOnClusterObserver.removed was not empty');
        assert.equal('melanie', observer.removed[0], 'AppsOnClusterObserver.removed[0] did not have the correct value for the object name');
      }),

      tdd.test('Cluster changes - cluster was removed from the collective', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED',
          servers: {up: 0, down: 0, unknown: 0, list: []},
          apps: {up: 0, down: 0, unknown: 0, partial: 0, list: []}});
        var appsOnCluster = new AppsOnCluster({cluster: cluster, appOnCluster: []});
        var observer = new AppsOnClusterObserver();

        appsOnCluster.subscribe(observer);

        // Cluster is removed from collective
        cluster._handleChangeEvent({type: 'cluster', id: 'cluster1', state: 'removed'});

        assert.ok(appsOnCluster.isDestroyed,     'AppsOnCluster.isDestroyed flag did not get set in response to a "removed" event');

        // Confirm the application is destroyed
        assert.ok(observer.destroyed,            'AppsOnClusterObserver.onDestroyed did not get called');
      });  
    });
  }
});
