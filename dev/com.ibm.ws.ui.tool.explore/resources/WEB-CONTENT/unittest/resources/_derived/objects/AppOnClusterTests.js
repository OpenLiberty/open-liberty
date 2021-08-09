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
 * Test cases for AppOnCluster
 */
define([ 
        "intern!tdd",
        "intern/chai!assert",
        "intern/dojo/declare",
        "resources/_derived/objects/AppOnCluster",
        "resources/_objects/Cluster",
        "resources/_objects/Server",
        "resources/_derived/objects/AppOnServer",
        "resources/Observer",
        "dojo/Deferred"
        ],

        function(tdd, assert, declare, AppOnCluster, Cluster, Server, AppOnServer, Observer, Deferred) {

  var AppOnClusterObserver = declare([Observer], {
    id: 'testObserver',

    onTallyChange: function(newTally, oldTally) {
      this.newTally = newTally;
      this.oldTally = oldTally;
    },

    onStateChange: function(newState, oldState) {
      this.newState = newState;
      this.oldState = oldState;
    },

    onScalingPolicyChange: function(newScalingPolicy, oldScalingPolicy) {
      this.newScalingPolicy = newScalingPolicy;
      this.oldScalingPolicy = oldScalingPolicy;
    },

    onScalingPolicyEnabledChange: function(newScalingPolicyEnabled, oldScalingPolicyEnabled) {
      this.newScalingPolicyEnabled = newScalingPolicyEnabled;
      this.oldScalingPolicyEnabled = oldScalingPolicyEnabled;
    },

    onListChange: function(newList, oldList, added, removed) {
      this.newList = newList;
      this.oldList = oldList;
      this.added = added;
      this.removed = removed;
    },

    onAlertsChange: function(newAlerts, oldAlerts) {
      this.newAlerts = newAlerts;
      this.oldAlerts = oldAlerts;
    },

    onTagsChange: function(newTags, oldTags) {
      this.newTags = newTags;
      this.oldTags = oldTags;
    },

    onOwnerChange: function(newOwner, oldOwner) {
      this.newOwner = newOwner;
      this.oldOwner = oldOwner;
    },

    onContactsChange: function(newContacts, oldContacts) {
      this.newContacts = newContacts;
      this.oldContacts = oldContacts;
    },

    onNoteChange: function(newNote, oldNote) {
      this.newNote = newNote;
      this.oldNote = oldNote;
    },

    onDestroyed: function() {
      this.destroyed = true;
    }
  });

  with(assert) {

    /**
     * Defines the 'AppOnCluster Object Tests' module test suite.
     */
    tdd.suite('AppOnCluster Object Tests', function() {

      tdd.test('constructor - no initialization object', function() {
        try {
          new AppOnCluster();
          assert.isTrue(false, 'AppOnCluster was successfully created when it should have failed - an initialization object is required');
        } catch(error) {
          assert.equal(error, 'AppOnCluster created without an initialization object', 'Error reported did not match expected error');
        }
      }),

      tdd.test('constructor - no name', function() {
        try {
          new AppOnCluster({cluster: {/*...*/}});
          assert.isTrue(false, 'AppOnCluster was successfully created when it should have failed - a name is required');
        } catch(error) {
          assert.equal(error, 'AppOnCluster created without a name', 'Error reported did not match expected error');
        }
      }),

      tdd.test('constructor - no cluster', function() {
        try {
          new AppOnCluster({name: 'snoop'});
          assert.isTrue(false, 'AppOnCluster was successfully created when it should have failed - a Cluster is required');
        } catch(error) {
          assert.equal(error, 'AppOnCluster created without a Cluster', 'Error reported did not match expected error');
        }
      }),

      tdd.test('constructor - one started app instance with tags', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED', 
          servers: { up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1'] },
          apps: { up: 1, down: 0, unknown: 0, partial: 0, list: 
            [ 
             { name: 'snoop', state: 'STARTED', up: 1, down: 0, unknown: 0, servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ]}, note: 'My note', owner: 'The owner', contacts: ['Mike'], tags: ['tag1'] }
             ]
          },
          alerts: { count: 0, unknown: [], app: []}
        });
        var appOnCluster = new AppOnCluster({name: 'snoop', cluster: cluster});

        assert.equal(appOnCluster.id,    'snoop(cluster1)', 'AppOnCluster.id did not have the correct initialized value');
        assert.equal(appOnCluster.state,       'STARTED',   'AppOnCluster.state did not have the correct initialized value');
        assert.equal(appOnCluster.up,          1,           'AppOnCluster.up did not have the correct initialized value');
        assert.equal(appOnCluster.down,        0,           'AppOnCluster.down did not have the correct initialized value');
        assert.equal(appOnCluster.unknown,     0,           'AppOnCluster.unknown did not have the correct initialized value');
        assert.equal(appOnCluster.scalingPolicy, null,      'AppOnCluster.scalingPolicy did not have the correct initialized value');
        assert.notOk(appOnCluster.scalingPolicyEnabled,     'AppOnCluster.scalingPolicyEnabled did not have the correct initialized value');
        assert.equal(appOnCluster.servers.up,      1,       'AppOnCluster.servers.up did not have the correct initialized value');
        assert.equal(appOnCluster.servers.down,    0,       'AppOnCluster.servers.down did not have the correct initialized value');
        assert.equal(appOnCluster.servers.unknown, 0,       'AppOnCluster.servers.unknown did not have the correct initialized value');
        assert.isTrue(Array.isArray(appOnCluster.servers.list), 'AppOnCluster.servers.list was not an Array');
        assert.equal(appOnCluster.servers.list.length,  1,  'AppOnCluster.servers.list was not the correct size');
        assert.equal(appOnCluster.servers.list[0], 'localhost,/wlp/usr,server1', 'AppOnCluster.servers.list[0] was not the element');
        assert.equal(appOnCluster.note,        'My note',   'AppOnCluster.note did not have the correct initialized value');
        assert.equal(appOnCluster.owner,       'The owner', 'AppOnCluster.owner did not have the correct initialized value');
        assert.sameMembers(appOnCluster.contacts, ['Mike'], 'AppOnCluster.contacts did not have the correct initialized value');
        assert.sameMembers(appOnCluster.tags,     ['tag1'], 'AppOnCluster.tags did not have the correct initialized value');
      }),

      tdd.test('constructor - one started app instance with disabled scaling policy', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED', scalingPolicy: 'default', scalingPolicyEnabled: false,
          servers: { up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1'] },
          apps: { up: 1, down: 0, unknown: 0, partial: 0, list: 
            [ 
             {name: 'snoop', state: 'STARTED', up: 1, down: 0, unknown: 0, servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ]}}
             ]
          }
        });
        var appOnCluster = new AppOnCluster({name: 'snoop', cluster: cluster});

        assert.equal(appOnCluster.id,    'snoop(cluster1)', 'AppOnCluster.id did not have the correct initialized value');
        assert.equal(appOnCluster.state,       'STARTED',   'AppOnCluster.state did not have the correct initialized value');
        assert.equal(appOnCluster.up,          1,           'AppOnCluster.up did not have the correct initialized value');
        assert.equal(appOnCluster.down,        0,           'AppOnCluster.down did not have the correct initialized value');
        assert.equal(appOnCluster.unknown,     0,           'AppOnCluster.unknown did not have the correct initialized value');
        assert.equal(appOnCluster.scalingPolicy, 'default', 'AppOnCluster.scalingPolicy did not have the correct initialized value');
        assert.isFalse(appOnCluster.scalingPolicyEnabled,   'AppOnCluster.scalingPolicyEnabled did not have the correct initialized value');
        assert.equal(appOnCluster.servers.up,      1,       'AppOnCluster.servers.up did not have the correct initialized value');
        assert.equal(appOnCluster.servers.down,    0,       'AppOnCluster.servers.down did not have the correct initialized value');
        assert.equal(appOnCluster.servers.unknown, 0,       'AppOnCluster.servers.unknown did not have the correct initialized value');
        assert.isTrue(Array.isArray(appOnCluster.servers.list), 'AppOnCluster.servers.list was not an Array');
        assert.equal(appOnCluster.servers.list.length,  1,  'AppOnCluster.servers.list was not the correct size');
        assert.equal(appOnCluster.servers.list[0], 'localhost,/wlp/usr,server1', 'AppOnCluster.servers.list[0] was not the element');
        assert.equal(appOnCluster.alerts.count, 0,           'AppOnCluster.alerts.count was not correct');
      }),

      tdd.test('constructor - two started app instances with enabled scaling policy', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED', scalingPolicy: 'default', scalingPolicyEnabled: true, 
          servers: { up: 2, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1', 'localhost,/wlp/usr,server2']},
          apps: { up: 1, down: 0, unknown: 0, partial: 0, list: 
            [ 
             { name: 'snoop', state: 'STARTED', up: 2, down: 0, unknown: 0, servers: { up: 2, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1', 'localhost,/wlp/usr,server2' ]}} 
             ]
          }
        });
        var appOnCluster = new AppOnCluster({name: 'snoop', cluster: cluster});
        var observer = new AppOnClusterObserver();

        appOnCluster.subscribe(observer);

        assert.equal(appOnCluster.id,    'snoop(cluster1)', 'AppOnCluster.id did not have the correct initialized value');
        assert.equal(appOnCluster.state,       'STARTED',   'AppOnCluster.state did not have the correct initialized value');
        assert.equal(appOnCluster.up,          2,           'AppOnCluster.up did not have the correct initialized value');
        assert.equal(appOnCluster.down,        0,           'AppOnCluster.down did not have the correct initialized value');
        assert.equal(appOnCluster.unknown,     0,           'AppOnCluster.unknown did not have the correct initialized value');
        assert.equal(appOnCluster.scalingPolicy, 'default', 'AppOnCluster.scalingPolicy did not have the correct initialized value');
        assert.isTrue(appOnCluster.scalingPolicyEnabled,    'AppOnCluster.scalingPolicyEnabled did not have the correct initialized value');
        assert.equal(appOnCluster.servers.up,      2,       'AppOnCluster.servers.up did not have the correct initialized value');
        assert.equal(appOnCluster.servers.down,    0,       'AppOnCluster.servers.down did not have the correct initialized value');
        assert.equal(appOnCluster.servers.unknown, 0,       'AppOnCluster.servers.unknown did not have the correct initialized value');
        assert.isTrue(Array.isArray(appOnCluster.servers.list), 'AppOnCluster.servers.list was not an Array');
        assert.equal(appOnCluster.servers.list.length,  2,  'AppOnCluster.servers.list was not the correct size');
        assert.equal(appOnCluster.servers.list[0], 'localhost,/wlp/usr,server1', 'AppOnCluster.servers.list[0] was not the element');
        assert.equal(appOnCluster.servers.list[1], 'localhost,/wlp/usr,server2', 'AppOnCluster.servers.list[1] was not the element');
        assert.equal(appOnCluster.alerts.count, 0,           'AppOnCluster.alerts.count was not correct');
      }),

      tdd.test('constructor - two app instances with alerts', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED', scalingPolicy: 'default', scalingPolicyEnabled: true, 
          servers: { up: 2, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1', 'localhost,/wlp/usr,server2']},
          apps: { up: 0, down: 1, unknown: 0, partial: 0, list: 
            [ 
             { name: 'snoop', state: 'STOPPED', up: 0, down: 2, unknown: 0, servers: { up: 2, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1', 'localhost,/wlp/usr,server2' ]}} 
             ]
          },
          alerts: {count:1, unknown: [], app: [{ name: 'cluster1,snoop', servers:['localhost,/wlp/usr,server1','localhost,/wlp/usr,server2'] }]}
        });
        var appOnCluster = new AppOnCluster({name: 'snoop', cluster: cluster});
        var observer = new AppOnClusterObserver();

        appOnCluster.subscribe(observer);

        assert.equal(appOnCluster.id,    'snoop(cluster1)', 'AppOnCluster.id did not have the correct initialized value');
        assert.equal(appOnCluster.state,       'STOPPED',   'AppOnCluster.state did not have the correct initialized value');
        assert.equal(appOnCluster.up,          0,           'AppOnCluster.up did not have the correct initialized value');
        assert.equal(appOnCluster.down,        2,           'AppOnCluster.down did not have the correct initialized value');
        assert.equal(appOnCluster.unknown,     0,           'AppOnCluster.unknown did not have the correct initialized value');
        assert.equal(appOnCluster.scalingPolicy, 'default', 'AppOnCluster.scalingPolicy did not have the correct initialized value');
        assert.isTrue(appOnCluster.scalingPolicyEnabled,    'AppOnCluster.scalingPolicyEnabled did not have the correct initialized value');
        assert.equal(appOnCluster.servers.up,      2,       'AppOnCluster.servers.up did not have the correct initialized value');
        assert.equal(appOnCluster.servers.down,    0,       'AppOnCluster.servers.down did not have the correct initialized value');
        assert.equal(appOnCluster.servers.unknown, 0,       'AppOnCluster.servers.unknown did not have the correct initialized value');
        assert.isTrue(Array.isArray(appOnCluster.servers.list), 'AppOnCluster.servers.list was not an Array');
        assert.equal(appOnCluster.servers.list.length,  2,  'AppOnCluster.servers.list was not the correct size');
        assert.equal(appOnCluster.servers.list[0], 'localhost,/wlp/usr,server1', 'AppOnCluster.servers.list[0] was not the element');
        assert.equal(appOnCluster.servers.list[1], 'localhost,/wlp/usr,server2', 'AppOnCluster.servers.list[1] was not the element');

        assert.equal(appOnCluster.alerts.count, 1,           'AppOnCluster.alerts.count was not correct');
        assert.equal(appOnCluster.alerts.unknown.length, 0,  'AppOnCluster.alerts.unknown.length was not correct');
        assert.equal(appOnCluster.alerts.app.length, 1,      'AppOnCluster.alerts.app.length was not correct');

        var appAlerts = appOnCluster.alerts.app[0];
        assert.equal(appAlerts.name,   'cluster1,snoop',     'appAlerts.name was not correct');
        assert.equal(appAlerts.servers.length, 2,            'appAlerts.servers.length was not correct');
        assert.equal(appAlerts.servers[0], 'localhost,/wlp/usr,server1', 'appAlerts.servers[0] was not correct');
        assert.equal(appAlerts.servers[1], 'localhost,/wlp/usr,server2', 'appAlerts.servers[1] was not correct');
      }),

      tdd.test('constructor - two app instances with unrelated alerts', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED', scalingPolicy: 'default', scalingPolicyEnabled: true, 
          servers: { up: 2, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1', 'localhost,/wlp/usr,server2']},
          apps: { up: 1, down: 0, unknown: 0, partial: 0, list: 
            [ 
             { name: 'snoop', state: 'STARTED', up: 2, down: 0, unknown: 0, servers: { up: 2, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1', 'localhost,/wlp/usr,server2' ]}} 
             ]
          },
          alerts: { count:1, unknown: [], app: [{ name: 'cluster1,other', servers:['localhost,/wlp/usr,server1'] }]}
        });
        var appOnCluster = new AppOnCluster({name: 'snoop', cluster: cluster});
        var observer = new AppOnClusterObserver();

        appOnCluster.subscribe(observer);

        assert.equal(appOnCluster.id,    'snoop(cluster1)', 'AppOnCluster.id did not have the correct initialized value');
        assert.equal(appOnCluster.state,       'STARTED',   'AppOnCluster.state did not have the correct initialized value');
        assert.equal(appOnCluster.up,          2,           'AppOnCluster.up did not have the correct initialized value');
        assert.equal(appOnCluster.down,        0,           'AppOnCluster.down did not have the correct initialized value');
        assert.equal(appOnCluster.unknown,     0,           'AppOnCluster.unknown did not have the correct initialized value');
        assert.equal(appOnCluster.scalingPolicy, 'default', 'AppOnCluster.scalingPolicy did not have the correct initialized value');
        assert.isTrue(appOnCluster.scalingPolicyEnabled,    'AppOnCluster.scalingPolicyEnabled did not have the correct initialized value');
        assert.equal(appOnCluster.servers.up,      2,       'AppOnCluster.servers.up did not have the correct initialized value');
        assert.equal(appOnCluster.servers.down,    0,       'AppOnCluster.servers.down did not have the correct initialized value');
        assert.equal(appOnCluster.servers.unknown, 0,       'AppOnCluster.servers.unknown did not have the correct initialized value');
        assert.isTrue(Array.isArray(appOnCluster.servers.list), 'AppOnCluster.servers.list was not an Array');
        assert.equal(appOnCluster.servers.list.length,  2,  'AppOnCluster.servers.list was not the correct size');
        assert.equal(appOnCluster.servers.list[0], 'localhost,/wlp/usr,server1', 'AppOnCluster.servers.list[0] was not the element');
        assert.equal(appOnCluster.servers.list[1], 'localhost,/wlp/usr,server2', 'AppOnCluster.servers.list[1] was not the element');
        assert.equal(appOnCluster.alerts.count, 0,           'AppOnCluster.alerts.count was not correct');
      }),

      tdd.test('constructor - two partially started app instances', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'PARTIALLY_STARTED', scalingPolicy: 'default', scalingPolicyEnabled: true, 
          servers: { up: 1, down: 1, unknown: 0, list: ['localhost,/wlp/usr,server1', 'localhost,/wlp/usr,server2']},
          apps: { up: 0, down: 0, unknown: 0, partial: 1, list: 
            [ 
             { name: 'snoop', state: 'PARTIALLY_STARTED', up: 1, down: 1, unknown: 0, servers: { up: 1, down: 1, unknown: 0, ids: [ 'localhost,/wlp/usr,server1', 'localhost,/wlp/usr,server2' ]}} 
             ]
          }
        });
        var appOnCluster = new AppOnCluster({name: 'snoop', cluster: cluster});
        var observer = new AppOnClusterObserver();

        appOnCluster.subscribe(observer);

        assert.equal(appOnCluster.id, 'snoop(cluster1)',      'AppOnCluster.id did not have the correct initialized value');
        assert.equal(appOnCluster.state, 'PARTIALLY_STARTED', 'AppOnCluster.state did not have the correct initialized value');
        assert.equal(appOnCluster.up,         1,              'AppOnCluster.up did not have the correct initialized value');
        assert.equal(appOnCluster.down,       1,              'AppOnCluster.down did not have the correct initialized value');
        assert.equal(appOnCluster.unknown,    0,              'AppOnCluster.unknown did not have the correct initialized value');
        assert.equal(appOnCluster.scalingPolicy, 'default',   'AppOnCluster.scalingPolicy did not have the correct initialized value');
        assert.isTrue(appOnCluster.scalingPolicyEnabled,      'AppOnCluster.scalingPolicyEnabled did not have the correct initialized value');
        assert.equal(appOnCluster.servers.up,      1,       'AppOnCluster.servers.up did not have the correct initialized value');
        assert.equal(appOnCluster.servers.down,    1,       'AppOnCluster.servers.down did not have the correct initialized value');
        assert.equal(appOnCluster.servers.unknown, 0,       'AppOnCluster.servers.unknown did not have the correct initialized value');
        assert.isTrue(Array.isArray(appOnCluster.servers.list), 'AppOnCluster.servers.list was not an Array');
        assert.equal(appOnCluster.servers.list.length,  2,  'AppOnCluster.servers.list was not the correct size');
        assert.equal(appOnCluster.servers.list[0], 'localhost,/wlp/usr,server1', 'AppOnCluster.servers.list[0] was not the element');
        assert.equal(appOnCluster.servers.list[1], 'localhost,/wlp/usr,server2', 'AppOnCluster.servers.list[1] was not the element');
        assert.equal(appOnCluster.alerts.count, 0,           'AppOnCluster.alerts.count was not correct');
      }),

      tdd.test('constructor - one unknown state app instance', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED', 
          servers: { up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1'] },
          apps: { up: 0, down: 0, unknown: 1, partial: 0, list: 
            [ 
             {name: 'snoop', state: 'UNKNOWN', up: 0, down: 0, unknown: 1, servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ]}}
             ]
          },
          alerts: { count: 1, unknown: [ { id: 'cluster1,snoop', type: 'appOnCluster' }], app: []}
        });
        var appOnCluster = new AppOnCluster({name: 'snoop', cluster: cluster});

        assert.equal(appOnCluster.id,    'snoop(cluster1)', 'AppOnCluster.id did not have the correct initialized value');
        assert.equal(appOnCluster.state,       'UNKNOWN',   'AppOnCluster.state did not have the correct initialized value');
        assert.equal(appOnCluster.up,          0,           'AppOnCluster.up did not have the correct initialized value');
        assert.equal(appOnCluster.down,        0,           'AppOnCluster.down did not have the correct initialized value');
        assert.equal(appOnCluster.unknown,     1,           'AppOnCluster.unknown did not have the correct initialized value');
        assert.equal(appOnCluster.scalingPolicy, null,      'AppOnCluster.scalingPolicy did not have the correct initialized value');
        assert.notOk(appOnCluster.scalingPolicyEnabled,     'AppOnCluster.scalingPolicyEnabled did not have the correct initialized value');
        assert.equal(appOnCluster.servers.up,      1,       'AppOnCluster.servers.up did not have the correct initialized value');
        assert.equal(appOnCluster.servers.down,    0,       'AppOnCluster.servers.down did not have the correct initialized value');
        assert.equal(appOnCluster.servers.unknown, 0,       'AppOnCluster.servers.unknown did not have the correct initialized value');
        assert.isTrue(Array.isArray(appOnCluster.servers.list), 'AppOnCluster.servers.list was not an Array');
        assert.equal(appOnCluster.servers.list.length,  1,  'AppOnCluster.servers.list was not the correct size');
        assert.equal(appOnCluster.servers.list[0], 'localhost,/wlp/usr,server1', 'AppOnCluster.servers.list[0] was not the element');

        // TODO: Re-enable this test logic once we fix our IDs
        /*
        assert.equal(appOnCluster.alerts.count, 1,           'AppOnCluster.alerts.count is dervied from the Cluster.alerts and was not correct');
        assert.equal(appOnCluster.alerts.unknown.length, 1,  'AppOnCluster.alerts.unknown.length pulls from the Cluster.alerts and was not correct');
        assert.equal(appOnCluster.alerts.app.length, 0,      'AppOnCluster.alerts.app.length pulls from the Cluster.alerts and was not correct');

        var unknownAlert = appOnCluster.alerts.unknown[0];
        assert.equal(unknownAlert.id,   'cluster1,snoop',  'unknownAlert.id was not correct');
        assert.equal(unknownAlert.type, 'appOnCluster',    'unknownAlert.type was not correct');
        */
      }),


      tdd.test('AppOnCluster changes - new tags', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED', 
          servers: { up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1'] },
          apps: { up: 1, down: 0, unknown: 0, partial: 0, list: 
            [ 
             { name: 'snoop', state: 'STARTED', up: 1, down: 0, unknown: 0, servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ]} }
             ]
          },
          alerts: { count: 0, unknown: [], app: []}
        });
        var appOnCluster = new AppOnCluster({name: 'snoop', cluster: cluster});
        var observer = new AppOnClusterObserver();

        appOnCluster.subscribe(observer);

        assert.equal(appOnCluster.tags,        null,      'AppOnCluster.tags was not correct');

        // Trigger the cluster's onAppsListChange method via the Cluster event handler
        cluster._handleChangeEvent({type: 'cluster', id: 'cluster1', apps: { up: 1, down: 0, unknown: 0, changed: [{id: 'snoop(cluster1)', name: 'snoop', state: 'STARTED', tags: ['newTag']}]}});

        assert.sameMembers(appOnCluster.tags,  ['newTag'], 'AppOnCluster.tags did not have the updated value');

        // Validate the Observer was passed the correct tags array after the first event
        assert.sameMembers(['newTag'], observer.newTags, 'AppOnClusterObserver did not get the correct new tags value');
        assert.equal(null, observer.oldTags, 'AppOnClusterObserver did not get the correct old tags value');
      }),

      tdd.test('AppOnCluster changes - tags updated', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED', 
          servers: { up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1'] },
          apps: { up: 1, down: 0, unknown: 0, partial: 0, list: 
            [ 
             { name: 'snoop', state: 'STARTED', up: 1, down: 0, unknown: 0, servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ]}, tags: ['tag1'] }
             ]
          },
          alerts: { count: 0, unknown: [], app: []}
        });
        var appOnCluster = new AppOnCluster({name: 'snoop', cluster: cluster});
        var observer = new AppOnClusterObserver();

        appOnCluster.subscribe(observer);

        assert.sameMembers(appOnCluster.tags,   ['tag1'], 'AppOnCluster.tags was not correct');

        // Trigger the cluster's onAppsListChange method via the Cluster event handler
        cluster._handleChangeEvent({type: 'cluster', id: 'cluster1', apps: {up: 1, down: 0, unknown: 0, changed: [{id: 'snoop(cluster1)', name: 'snoop', state: 'STARTED', tags: ['tag2']}]}});

        assert.sameMembers(appOnCluster.tags,   ['tag2'], 'AppOnCluster.tags did not have the updated value');

        // Validate the Observer was passed the correct tags array after the first event
        assert.sameMembers(observer.newTags,    ['tag2'], 'AppOnClusterObserver did not get the correct new tags value');
        assert.sameMembers(observer.oldTags,    ['tag1'], 'AppOnClusterObserver did not get the correct old tags value');
      }),

      tdd.test('AppOnCluster changes - owner updated', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED', 
          servers: { up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1'] },
          apps: { up: 1, down: 0, unknown: 0, partial: 0, list: 
            [ 
             { name: 'snoop', state: 'STARTED', up: 1, down: 0, unknown: 0, servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ]}, owner: 'Frank' }
             ]
          },
          alerts: { count: 0, unknown: [], app: []}
        });
        var appOnCluster = new AppOnCluster({name: 'snoop', cluster: cluster});
        var observer = new AppOnClusterObserver();

        appOnCluster.subscribe(observer);

        assert.equal(appOnCluster.owner,     'Frank',      'AppOnCluster.owner was not correct');

        // Trigger the cluster's onAppsListChange method via the Cluster event handler
        cluster._handleChangeEvent({type: 'cluster', id: 'cluster1', apps: {up: 1, down: 0, unknown: 0, changed: [{id: 'snoop(cluster1)', name: 'snoop', state: 'STARTED', owner: 'Bob'}]}});

        assert.equal(appOnCluster.owner,     'Bob',        'AppOnCluster.owner did not have the updated value');

        // Validate the Observer was passed the correct tags array after the first event
        assert.equal(observer.newOwner,     'Bob',         'AppOnClusterObserver did not get the correct new owner value');
        assert.equal(observer.oldOwner,     'Frank',       'AppOnClusterObserver did not get the correct old owner value');
      }),

      tdd.test('AppOnCluster changes - contacts updated', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED', 
          servers: { up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1'] },
          apps: { up: 1, down: 0, unknown: 0, partial: 0, list: 
            [ 
             { name: 'snoop', state: 'STARTED', up: 1, down: 0, unknown: 0, servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ]}, contacts: ['Mike'] }
             ]
          },
          alerts: { count: 0, unknown: [], app: []}
        });
        var appOnCluster = new AppOnCluster({name: 'snoop', cluster: cluster});
        var observer = new AppOnClusterObserver();

        appOnCluster.subscribe(observer);

        assert.sameMembers(appOnCluster.contacts,    ['Mike'], 'AppOnCluster.contacts was not correct');

        // Trigger the cluster's onAppsListChange method via the Cluster event handler
        cluster._handleChangeEvent({type: 'cluster', id: 'cluster1', apps: {up: 1, down: 0, unknown: 0, changed: [{id: 'snoop(cluster1)', name: 'snoop', state: 'STARTED', contacts: ['Dave', 'Ed', 'Frank']}]}});

        assert.sameMembers(appOnCluster.contacts,    ['Dave', 'Ed', 'Frank'], 'AppOnCluster.contacts did not have the updated value');

        // Validate the Observer was passed the correct tags array after the first event
        assert.sameMembers(observer.newContacts,     ['Dave', 'Ed', 'Frank'], 'AppOnClusterObserver did not get the correct new tags value');
        assert.sameMembers(observer.oldContacts,     ['Mike'],   'AppOnClusterObserver did not get the correct old tags value');
      }),

      tdd.test('AppOnCluster changes - note updated', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED', 
          servers: { up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1'] },
          apps: { up: 1, down: 0, unknown: 0, partial: 0, list: 
            [ 
             { name: 'snoop', state: 'STARTED', up: 1, down: 0, unknown: 0, servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ]}, note: 'Hello World' }
             ]
          },
          alerts: { count: 0, unknown: [], app: []}
        });
        var appOnCluster = new AppOnCluster({name: 'snoop', cluster: cluster});
        var observer = new AppOnClusterObserver();

        appOnCluster.subscribe(observer);

        assert.equal(appOnCluster.note,     'Hello World', 'AppOnCluster.note was not correct');

        // Trigger the cluster's onAppsListChange method via the Cluster event handler
        cluster._handleChangeEvent({type: 'cluster', id: 'cluster1', apps: {up: 1, down: 0, unknown: 0, changed: [{id: 'snoop(cluster1)', name: 'snoop', state: 'STARTED', note: 'Hello Kitty'}]}});

        assert.equal(appOnCluster.note,     'Hello Kitty', 'AppOnCluster.owner did not have the updated value');

        // Validate the Observer was passed the correct tags array after the first event
        assert.equal(observer.newNote,      'Hello Kitty', 'AppOnClusterObserver did not get the correct new note value');
        assert.equal(observer.oldNote,      'Hello World', 'AppOnClusterObserver did not get the correct old note value');
      }),

      tdd.test('AppOnCluster changes - state changed to down', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED',
          servers: { up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1'] },
          apps: { up: 1, down: 0, unknown: 0, partial: 0, list: 
            [ 
             { name: 'snoop', state: 'STARTED', up: 1, down: 0, unknown: 0, servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ]} }
             ]
          }
        });
        var appOnCluster = new AppOnCluster({name: 'snoop', cluster: cluster});
        var observer = new AppOnClusterObserver();

        appOnCluster.subscribe(observer);

        assert.equal(appOnCluster.state,       'STARTED',   'AppOnCluster.state did not have the correct initialized value');
        assert.equal(appOnCluster.up,          1,           'AppOnCluster.up did not have the correct initialized value');
        assert.equal(appOnCluster.down,        0,           'AppOnCluster.down did not have the correct initialized value');
        assert.equal(appOnCluster.unknown,     0,           'AppOnCluster.unknown did not have the correct initialized value');

        // Trigger the change event
        cluster._handleChangeEvent({type: 'cluster', id: 'cluster1', apps: {up: 0, down: 1, unknown: 0, changed: [{name: 'snoop', state: 'STOPPED', up: 0, down: 1, unknown: 0}]}});

        assert.equal(appOnCluster.state,       'STOPPED',   'AppOnCluster.state did not have the correct updated value');
        assert.equal(appOnCluster.up,          0,           'AppOnCluster.up did not have the correct updated value');
        assert.equal(appOnCluster.down,        1,           'AppOnCluster.down did not have the correct updated value');
        assert.equal(appOnCluster.unknown,     0,           'AppOnCluster.unknown did not have the correct updated value');

        // Validate the Observer was passed the correct tally objects after the first event
        assert.isNotNull(observer.newTally,             'AppOnClusterObserver.newTally did not get set, when it should have been');
        assert.equal(observer.newTally.up,      0, 'AppOnClusterObserver did not get the correct new value for the up tally');
        assert.equal(observer.newTally.down,    1, 'AppOnClusterObserver did not get the correct new value for the down tally');
        assert.equal(observer.newTally.unknown, 0, 'AppOnClusterObserver did not get the correct new value for the unknown tally');

        assert.isNotNull(observer.oldTally,             'AppOnClusterObserver.oldTally did not get set, when it should have been');
        assert.equal(observer.oldTally.up,      1, 'AppOnClusterObserver did not get the correct old value for the up tally');
        assert.equal(observer.oldTally.down,    0, 'AppOnClusterObserver did not get the correct old value for the down tally');
        assert.equal(observer.oldTally.unknown, 0, 'AppOnClusterObserver did not get the correct old value for the unknown tally');

        assert.equal(observer.newState, 'STOPPED', 'AppOnClusterObserver did not get the correct NEW value for state');
        assert.equal(observer.oldState, 'STARTED', 'AppOnClusterObserver did not get the correct OLD value for state');
      }),

      tdd.test('AppOnCluster changes - state changed to up', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED',
          servers: { up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1'] },
          apps: { up: 1, down: 0, unknown: 0, partial: 0, list: 
            [ 
             { name: 'snoop', state: 'STOPPED', up: 0, down: 1, unknown: 0, servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ]} }
             ]
          }
        });
        var appOnCluster = new AppOnCluster({name: 'snoop', cluster: cluster});
        var observer = new AppOnClusterObserver();

        appOnCluster.subscribe(observer);

        assert.equal(appOnCluster.state,       'STOPPED',   'AppOnCluster.state did not have the correct initialized value');
        assert.equal(appOnCluster.up,          0,           'AppOnCluster.up did not have the correct initialized value');
        assert.equal(appOnCluster.down,        1,           'AppOnCluster.down did not have the correct initialized value');
        assert.equal(appOnCluster.unknown,     0,           'AppOnCluster.unknown did not have the correct initialized value');

        // Trigger the change event
        cluster._handleChangeEvent({type: 'cluster', id: 'cluster1', apps: {up: 1, down: 0, unknown: 0, changed: [{name: 'snoop', state: 'STARTED', up: 1, down: 0, unknown: 0}]}});

        assert.equal(appOnCluster.state,       'STARTED',   'AppOnCluster.state did not have the correct initialized value');
        assert.equal(appOnCluster.up,          1,           'AppOnCluster.up did not have the correct updated value');
        assert.equal(appOnCluster.down,        0,           'AppOnCluster.down did not have the correct updated value');
        assert.equal(appOnCluster.unknown,     0,           'AppOnCluster.unknown did not have the correct updated value');

        // Validate the Observer was passed the correct tally objects after the first event
        assert.isNotNull(observer.newTally,             'AppOnClusterObserver.newTally did not get set, when it should have been');
        assert.equal(observer.newTally.up,      1, 'AppOnClusterObserver did not get the correct new value for the up tally');
        assert.equal(observer.newTally.down,    0, 'AppOnClusterObserver did not get the correct new value for the down tally');
        assert.equal(observer.newTally.unknown, 0, 'AppOnClusterObserver did not get the correct new value for the unknown tally');

        assert.isNotNull(observer.oldTally,             'AppOnClusterObserver.oldTally did not get set, when it should have been');
        assert.equal(observer.oldTally.up,      0, 'AppOnClusterObserver did not get the correct old value for the up tally');
        assert.equal(observer.oldTally.down,    1, 'AppOnClusterObserver did not get the correct old value for the down tally');
        assert.equal(observer.oldTally.unknown, 0, 'AppOnClusterObserver did not get the correct old value for the unknown tally');

        assert.equal(observer.newState, 'STARTED', 'AppOnClusterObserver did not get the correct NEW value for state');
        assert.equal(observer.oldState, 'STOPPED', 'AppOnClusterObserver did not get the correct OLD value for state');
      }),

      tdd.test('Cluster changes - state changed to starting is ignored', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED',
          servers: { up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1'] },
          apps: { up: 1, down: 0, unknown: 0, partial: 0, list: 
            [ 
             { name: 'snoop', state: 'STOPPED', up: 0, down: 1, unknown: 0, servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ]} }
             ]
          }
        });
        var appOnCluster = new AppOnCluster({name: 'snoop', cluster: cluster});
        var observer = new AppOnClusterObserver();

        appOnCluster.subscribe(observer);

        assert.equal(appOnCluster.state,       'STOPPED',   'AppOnCluster.state did not have the correct initialized value');
        assert.equal(appOnCluster.up,          0,           'AppOnCluster.up did not have the correct initialized value');
        assert.equal(appOnCluster.down,        1,           'AppOnCluster.down did not have the correct initialized value');
        assert.equal(appOnCluster.unknown,     0,           'AppOnCluster.unknown did not have the correct initialized value');

        // Trigger the change event
        cluster._handleChangeEvent({type: 'cluster', id: 'cluster1', state: 'STARTING'});

        assert.equal(appOnCluster.state,       'STOPPED',  'AppOnCluster.state did not have the correct updated value');
        assert.equal(appOnCluster.up,          0,           'AppOnCluster.up did not have the correct updated value');
        assert.equal(appOnCluster.down,        1,           'AppOnCluster.down did not have the correct updated value');
        assert.equal(appOnCluster.unknown,     0,           'AppOnCluster.unknown did not have the correct updated value');

        // Validate the Observer onTallyChange method was not called
        assert.notOk(observer.newTally,             'AppOnClusterObserver.newTally should not be set when there is no effective tally change');
        assert.notOk(observer.oldTally,             'AppOnClusterObserver.oldTally should not be set when there is no effective tally change');
        assert.notOk(observer.newState,             'AppOnClusterObserver should not have a NEW value for state as the state did not change');
        assert.notOk(observer.oldState,             'AppOnClusterObserver should not have a OLD value for state as the state did not change');
      }),

      tdd.test('Cluster changes - state changed to stopping is ignored', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED',
          servers: { up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1'] },
          apps: { up: 1, down: 0, unknown: 0, partial: 0, list: 
            [ 
             { name: 'snoop', state: 'STARTED', up: 1, down: 0, unknown: 0, servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ]} }
             ]
          }
        });
        var appOnCluster = new AppOnCluster({name: 'snoop', cluster: cluster});
        var observer = new AppOnClusterObserver();

        appOnCluster.subscribe(observer);

        assert.equal(appOnCluster.state,       'STARTED',   'AppOnCluster.state did not have the correct initialized value');
        assert.equal(appOnCluster.up,          1,           'AppOnCluster.up did not have the correct initialized value');
        assert.equal(appOnCluster.down,        0,           'AppOnCluster.down did not have the correct initialized value');
        assert.equal(appOnCluster.unknown,     0,           'AppOnCluster.unknown did not have the correct initialized value');

        // Trigger the change event
        cluster._handleChangeEvent({type: 'cluster', id: 'cluster1', state: 'STOPPING'});

        assert.equal(appOnCluster.state,       'STARTED',  'AppOnCluster.state did not have the correct updated value');
        assert.equal(appOnCluster.up,          1,           'AppOnCluster.up did not have the correct updated value');
        assert.equal(appOnCluster.down,        0,           'AppOnCluster.down did not have the correct updated value');
        assert.equal(appOnCluster.unknown,     0,           'AppOnCluster.unknown did not have the correct updated value');

        // Validate the Observer onTallyChange method was not called
        assert.notOk(observer.newTally,             'AppOnClusterObserver.newTally should not be set when there is no effective tally change');
        assert.notOk(observer.oldTally,             'AppOnClusterObserver.oldTally should not be set when there is no effective tally change');
        assert.notOk(observer.newState,             'AppOnClusterObserver should not have a NEW value for state as the state did not change');
        assert.notOk(observer.oldState,             'AppOnClusterObserver should not have a OLD value for state as the state did not change');
      }),

      /**
       * When the cluster goes down, the whole application should be set as down....
       */
      tdd.test('Cluster changes - state changed to down', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED',
          servers: { up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1'] },
          apps: { up: 1, down: 0, unknown: 0, partial: 0, list: 
            [ 
             { name: 'snoop', state: 'STARTED', up: 1, down: 0, unknown: 0, servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ]}, contacts: ['Mike'] }
             ]
          }
        });
        var appOnCluster = new AppOnCluster({name: 'snoop', cluster: cluster});
        var observer = new AppOnClusterObserver();

        appOnCluster.subscribe(observer);

        assert.equal(appOnCluster.state,       'STARTED',   'AppOnCluster.state did not have the correct initialized value');
        assert.equal(appOnCluster.up,          1,           'AppOnCluster.up did not have the correct initialized value');
        assert.equal(appOnCluster.down,        0,           'AppOnCluster.down did not have the correct initialized value');
        assert.equal(appOnCluster.unknown,     0,           'AppOnCluster.unknown did not have the correct initialized value');

        // Trigger the change event
        cluster._handleChangeEvent({type: 'cluster', id: 'cluster1', state: 'STOPPED'});

        assert.equal(appOnCluster.state,       'STOPPED',   'AppOnCluster.state did not have the correct updated value');
        assert.equal(appOnCluster.up,          0,           'AppOnCluster.up did not have the correct updated value');
        assert.equal(appOnCluster.down,        1,           'AppOnCluster.down did not have the correct updated value');
        assert.equal(appOnCluster.unknown,     0,           'AppOnCluster.unknown did not have the correct updated value');

        // Validate the Observer was passed the correct tally objects after the first event
        assert.isNotNull(observer.newTally,        'AppOnClusterObserver.newTally did not get set, when it should have been');
        assert.equal(observer.newTally.up,      0, 'AppOnClusterObserver did not get the correct new value for the up tally');
        assert.equal(observer.newTally.down,    1, 'AppOnClusterObserver did not get the correct new value for the down tally');
        assert.equal(observer.newTally.unknown, 0, 'AppOnClusterObserver did not get the correct new value for the unknown tally');

        assert.isNotNull(observer.oldTally,        'AppOnClusterObserver.oldTally did not get set, when it should have been');
        assert.equal(observer.oldTally.up,      1, 'AppOnClusterObserver did not get the correct old value for the up tally');
        assert.equal(observer.oldTally.down,    0, 'AppOnClusterObserver did not get the correct old value for the down tally');
        assert.equal(observer.oldTally.unknown, 0, 'AppOnClusterObserver did not get the correct old value for the unknown tally');

        assert.equal(observer.newState, 'STOPPED', 'AppOnClusterObserver did not get the correct NEW value for state');
        assert.equal(observer.oldState, 'STARTED', 'AppOnClusterObserver did not get the correct OLD value for state');
      }),

      tdd.test('Cluster changes - scalingPolicy added', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED',
          servers: { up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1'] },
          apps: { up: 1, down: 0, unknown: 0, partial: 0, list: 
            [ 
             { name: 'snoop', state: 'STARTED', up: 1, down: 0, unknown: 0, servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ]}}
             ]
          }
        });
        var appOnCluster = new AppOnCluster({name: 'snoop', cluster: cluster});
        var observer = new AppOnClusterObserver();

        appOnCluster.subscribe(observer);

        assert.equal(appOnCluster.scalingPolicy, null,        'AppOnCluster.scalingPolicy did not have the correct initialized value');

        // Trigger the change event
        cluster._handleChangeEvent({type: 'cluster', id: 'cluster1', scalingPolicy: 'default'});

        assert.equal(appOnCluster.scalingPolicy, 'default',   'AppOnCluster.scalingPolicy did not have the correct updated value');

        // Validate the Observer was passed the correct tally objects after the first event
        assert.equal(observer.newScalingPolicy,  'default',   'AppOnClusterObserver.newScalingPolicy did not get set, when it should have been');
        assert.equal(observer.oldScalingPolicy,  null,        'AppOnClusterObserver.oldScalingPolicy did not get set, when it should have been');
      }),

      tdd.test('Cluster changes - scalingPolicy changed', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED', scalingPolicy: 'default', scalingPolicyEnabled: true,
          servers: { up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1'] },
          apps: { up: 1, down: 0, unknown: 0, partial: 0, list: 
            [ 
             { name: 'snoop', state: 'STARTED', up: 1, down: 0, unknown: 0, servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ]}}
             ]
          }
        });
        var appOnCluster = new AppOnCluster({name: 'snoop', cluster: cluster});
        var observer = new AppOnClusterObserver();

        appOnCluster.subscribe(observer);

        assert.equal(appOnCluster.scalingPolicy, 'default',   'AppOnCluster.scalingPolicy did not have the correct initialized value');

        // Trigger the change event
        cluster._handleChangeEvent({type: 'cluster', id: 'cluster1', scalingPolicy: 'newPolicy'});

        assert.equal(appOnCluster.scalingPolicy, 'newPolicy', 'AppOnCluster.scalingPolicy did not have the correct updated value');

        // Validate the Observer was passed the correct tally objects after the first event
        assert.equal('newPolicy', observer.newScalingPolicy,  'AppOnClusterObserver.newScalingPolicy did not get set, when it should have been');
        assert.equal('default',   observer.oldScalingPolicy,  'AppOnClusterObserver.oldScalingPolicy did not get set, when it should have been');
      }),

      tdd.test('Cluster changes - scalingPolicy removed', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED', scalingPolicy: 'default', scalingPolicyEnabled: true,
          servers: { up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1'] },
          apps: { up: 1, down: 0, unknown: 0, partial: 0, list: 
            [ 
             { name: 'snoop', state: 'STARTED', up: 1, down: 0, unknown: 0, servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ]}}
             ]
          }
        });
        var appOnCluster = new AppOnCluster({name: 'snoop', cluster: cluster});
        var observer = new AppOnClusterObserver();

        appOnCluster.subscribe(observer);

        assert.equal(appOnCluster.scalingPolicy, 'default',   'AppOnCluster.scalingPolicy did not have the correct initialized value');

        // Trigger the change event
        cluster._handleChangeEvent({type: 'cluster', id: 'cluster1', scalingPolicy: null});

        assert.equal(appOnCluster.scalingPolicy, null,       'AppOnCluster.scalingPolicy did not have the correct updated value');

        // Validate the Observer was passed the correct tally objects after the first event
        assert.equal(observer.newScalingPolicy,  null,        'AppOnClusterObserver.newScalingPolicy did not get set, when it should have been');
        assert.equal(observer.oldScalingPolicy,  'default',   'AppOnClusterObserver.oldScalingPolicy did not get set, when it should have been');
      }),

      tdd.test('Cluster changes - scalingPolicyEnabled change', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED',
          servers: { up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1'] },
          apps: { up: 1, down: 0, unknown: 0, partial: 0, list: 
            [ 
             { name: 'snoop', state: 'STARTED', up: 1, down: 0, unknown: 0, servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ]}}
             ]
          }
        });
        var appOnCluster = new AppOnCluster({name: 'snoop', cluster: cluster});
        var observer = new AppOnClusterObserver();

        appOnCluster.subscribe(observer);

        assert.notOk(appOnCluster.scalingPolicyEnabled, 'AppOnCluster.scalingPolicyEnabled did not have the correct initialized value');

        // Trigger the change event
        cluster._handleChangeEvent({type: 'cluster', id: 'cluster1', scalingPolicyEnabled: true});

        assert.isTrue(appOnCluster.scalingPolicyEnabled, 'AppOnCluster.scalingPolicyEnabled did not have the correct updated value');

        // Validate the Observer was passed the correct tally objects after the first event
        assert.isTrue(observer.newScalingPolicyEnabled,  'AppOnClusterObserver.newScalingPolicyEnabled did not get set, when it should have been');
        assert.notOk(observer.oldScalingPolicyEnabled,  'AppOnClusterObserver.oldScalingPolicyEnabled did not get set, when it should have been');
      }),

      tdd.test('Cluster changes - added server', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED', scalingPolicy: 'default', scalingPolicyEnabled: true,
          servers: { up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1'] },
          apps: { up: 1, down: 0, unknown: 0, partial: 0, list: 
            [ 
             { name: 'snoop', state: 'STARTED', up: 1, down: 0, unknown: 0, servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ]}}
             ]
          }
        });
        var appOnCluster = new AppOnCluster({name: 'snoop', cluster: cluster});
        var observer = new AppOnClusterObserver();

        appOnCluster.subscribe(observer);

        // Trigger the change event
        cluster._handleChangeEvent({type: 'cluster', id: 'cluster1',
          servers: { up: 2, down: 0, unknown: 0, added: ['localhost,/wlp/usr,server2'] },
          apps: {up: 1, down: 0, unknown: 0, changed: 
            [
             { name: 'snoop', state: 'STARTED', up: 2, down: 0, unknown: 0, servers: { up: 2, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1', 'localhost,/wlp/usr,server2' ]} }
             ]}
        });

        assert.equal(appOnCluster.state,       'STARTED',   'AppOnCluster.state did not have the correct updated value');
        assert.equal(appOnCluster.up,          2,           'AppOnCluster.up did not have the correct updated value');
        assert.equal(appOnCluster.down,        0,           'AppOnCluster.down did not have the correct updated value');
        assert.equal(appOnCluster.unknown,     0,           'AppOnCluster.unknown did not have the correct updated value');
        assert.equal(appOnCluster.scalingPolicy, 'default',   'AppOnCluster.scalingPolicy did not have the correct initialized value');
        assert.isTrue(appOnCluster.scalingPolicyEnabled,    'AppOnCluster.scalingPolicyEnabled did not have the correct initialized value');
        assert.equal(appOnCluster.servers.up,      2,       'AppOnCluster.servers.up did not have the correct updated value');
        assert.equal(appOnCluster.servers.down,    0,       'AppOnCluster.servers.down did not have the correct updated value');
        assert.equal(appOnCluster.servers.unknown, 0,       'AppOnCluster.servers.unknown did not have the correct updated value');
        assert.isTrue(Array.isArray(appOnCluster.servers.list), 'AppOnCluster.servers.list was not an Array');
        assert.equal(appOnCluster.servers.list.length,  2,  'AppOnCluster.servers.list was not the correct size');
        assert.equal(appOnCluster.servers.list[0], 'localhost,/wlp/usr,server1', 'AppOnCluster.servers.list[0] was not the element');
        assert.equal(appOnCluster.servers.list[1], 'localhost,/wlp/usr,server2', 'AppOnCluster.servers.list[1] was not the element');

        // Validate the Observer was passed the correct tally objects after the first event
        assert.isNotNull(observer.newTally,             'AppOnClusterObserver.newTally did not get set, when it should have been');
        assert.equal(observer.newTally.up,      2, 'AppOnClusterObserver did not get the correct new value for the up tally');
        assert.equal(observer.newTally.down,    0, 'AppOnClusterObserver did not get the correct new value for the down tally');
        assert.equal(observer.newTally.unknown, 0, 'AppOnClusterObserver did not get the correct new value for the unknown tally');

        assert.isNotNull(observer.oldTally,             'AppOnClusterObserver.oldTally did not get set, when it should have been');
        assert.equal(observer.oldTally.up,      1, 'AppOnClusterObserver did not get the correct old value for the up tally');
        assert.equal(observer.oldTally.down,    0, 'AppOnClusterObserver did not get the correct old value for the down tally');
        assert.equal(observer.oldTally.unknown, 0, 'AppOnClusterObserver did not get the correct old value for the unknown tally');
      }),

      tdd.test('Cluster changes - removed server', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED', scalingPolicy: 'default', scalingPolicyEnabled: true,
          servers: { up: 2, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1', 'localhost,/wlp/usr,server2']},
          apps: { up: 1, down: 0, unknown: 0, partial: 0, list: 
            [ 
             { name: 'snoop', state: 'STARTED', up: 2, down: 0, unknown: 0, servers: { up: 2, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1', 'localhost,/wlp/usr,server2' ]}} 
             ]
          }
        });
        var appOnCluster = new AppOnCluster({name: 'snoop', cluster: cluster});
        var observer = new AppOnClusterObserver();

        appOnCluster.subscribe(observer);

        assert.equal(appOnCluster.state,       'STARTED',   'AppOnCluster.state did not have the correct updated value');
        assert.equal(appOnCluster.up,          2,           'AppOnCluster.up did not have the correct initialized value');
        assert.equal(appOnCluster.down,        0,           'AppOnCluster.down did not have the correct initialized value');
        assert.equal(appOnCluster.unknown,     0,           'AppOnCluster.unknown did not have the correct initialized value');
        assert.equal(appOnCluster.scalingPolicy, 'default', 'AppOnCluster.scalingPolicy did not have the correct initialized value');
        assert.isTrue(appOnCluster.scalingPolicyEnabled,    'AppOnCluster.scalingPolicyEnabled did not have the correct initialized value');
        assert.equal(appOnCluster.servers.up,      2,       'AppOnCluster.servers.up did not have the correct initialized value');
        assert.equal(appOnCluster.servers.down,    0,       'AppOnCluster.servers.down did not have the correct initialized value');
        assert.equal(appOnCluster.servers.unknown, 0,       'AppOnCluster.servers.unknown did not have the correct initialized value');
        assert.isTrue(Array.isArray(appOnCluster.servers.list), 'AppOnCluster.servers.list was not an Array');
        assert.equal(appOnCluster.servers.list.length,  2,  'AppOnCluster.servers.list was not the correct size');
        assert.equal(appOnCluster.servers.list[0], 'localhost,/wlp/usr,server1', 'AppOnCluster.servers.list[0] was not the element');
        assert.equal(appOnCluster.servers.list[1], 'localhost,/wlp/usr,server2', 'AppOnCluster.servers.list[1] was not the element');

        // Trigger the change event
        cluster._handleChangeEvent({type: 'cluster', id: 'cluster1',
          servers: { up: 1, down: 0, unknown: 0, removed: ['localhost,/wlp/usr,server2'] },
          apps: {up: 1, down: 0, unknown: 0, changed: 
            [
             { name: 'snoop', state: 'STARTED', up: 1, down: 0, unknown: 0,servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ]} }
             ]}
        });

        assert.equal(appOnCluster.state,       'STARTED',   'AppOnCluster.state did not have the correct updated value');
        assert.equal(appOnCluster.up,          1,           'AppOnCluster.up did not have the correct updated value');
        assert.equal(appOnCluster.down,        0,           'AppOnCluster.down did not have the correct updated value');
        assert.equal(appOnCluster.unknown,     0,           'AppOnCluster.unknown did not have the correct updated value');
        assert.equal(appOnCluster.scalingPolicy, 'default',   'AppOnCluster.scalingPolicy did not have the correct initialized value');
        assert.isTrue(appOnCluster.scalingPolicyEnabled,    'AppOnCluster.scalingPolicyEnabled did not have the correct initialized value');
        assert.equal(appOnCluster.servers.up,      1,       'AppOnCluster.servers.up did not have the correct initialized value');
        assert.equal(appOnCluster.servers.down,    0,       'AppOnCluster.servers.down did not have the correct initialized value');
        assert.equal(appOnCluster.servers.unknown, 0,       'AppOnCluster.servers.unknown did not have the correct initialized value');
        assert.isTrue(Array.isArray(appOnCluster.servers.list), 'AppOnCluster.servers.list was not an Array');
        assert.equal(appOnCluster.servers.list.length,  1,  'AppOnCluster.servers.list was not the correct size');
        assert.equal(appOnCluster.servers.list[0], 'localhost,/wlp/usr,server1', 'AppOnCluster.servers.list[0] was not the element');

        // Validate the Observer was passed the correct tally objects after the first event
        assert.isNotNull(observer.newTally,             'AppOnClusterObserver.newTally did not get set, when it should have been');
        assert.equal(observer.newTally.up,      1, 'AppOnClusterObserver did not get the correct new value for the up tally');
        assert.equal(observer.newTally.down,    0, 'AppOnClusterObserver did not get the correct new value for the down tally');
        assert.equal(observer.newTally.unknown, 0, 'AppOnClusterObserver did not get the correct new value for the unknown tally');

        assert.isNotNull(observer.oldTally,             'AppOnClusterObserver.oldTally did not get set, when it should have been');
        assert.equal(observer.oldTally.up,      2, 'AppOnClusterObserver did not get the correct old value for the up tally');
        assert.equal(observer.oldTally.down,    0, 'AppOnClusterObserver did not get the correct old value for the down tally');
        assert.equal(observer.oldTally.unknown, 0, 'AppOnClusterObserver did not get the correct old value for the unknown tally');
      }),

      /**
       * This test is to ensure we have the right splice logic
       */
      tdd.test('Cluster changes - removed middle server', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED', scalingPolicy: 'default', scalingPolicyEnabled: true, 
          servers: { up: 3, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1', 'localhost,/wlp/usr,server2', 'localhost,/wlp/usr,server3']},
          apps: { up: 1, down: 0, unknown: 0, partial: 0, list: 
            [ 
             { name: 'snoop', state: 'STARTED', up: 3, down: 0, unknown: 0, servers: { up: 3, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1', 'localhost,/wlp/usr,server2', 'localhost,/wlp/usr,server3' ]}} 
             ]
          }
        });
        var appOnCluster = new AppOnCluster({name: 'snoop', cluster: cluster});
        var observer = new AppOnClusterObserver();

        appOnCluster.subscribe(observer);

        assert.equal(appOnCluster.state,       'STARTED',   'AppOnCluster.state did not have the correct updated value');
        assert.equal(appOnCluster.up,          3,           'AppOnCluster.up did not have the correct initialized value');
        assert.equal(appOnCluster.down,        0,           'AppOnCluster.down did not have the correct initialized value');
        assert.equal(appOnCluster.unknown,     0,           'AppOnCluster.unknown did not have the correct initialized value');
        assert.equal(appOnCluster.scalingPolicy, 'default', 'AppOnCluster.scalingPolicy did not have the correct initialized value');
        assert.isTrue(appOnCluster.scalingPolicyEnabled,    'AppOnCluster.scalingPolicyEnabled did not have the correct initialized value');
        assert.equal(appOnCluster.servers.up,      3,       'AppOnCluster.servers.up did not have the correct initialized value');
        assert.equal(appOnCluster.servers.down,    0,       'AppOnCluster.servers.down did not have the correct initialized value');
        assert.equal(appOnCluster.servers.unknown, 0,       'AppOnCluster.servers.unknown did not have the correct initialized value');
        assert.isTrue(Array.isArray(appOnCluster.servers.list), 'AppOnCluster.servers.list was not an Array');
        assert.equal(appOnCluster.servers.list.length,  3,  'AppOnCluster.servers.list was not the correct size');
        assert.equal(appOnCluster.servers.list[0], 'localhost,/wlp/usr,server1', 'AppOnCluster.servers.list[0] was not the element');
        assert.equal(appOnCluster.servers.list[1], 'localhost,/wlp/usr,server2', 'AppOnCluster.servers.list[1] was not the element');
        assert.equal(appOnCluster.servers.list[2], 'localhost,/wlp/usr,server3', 'AppOnCluster.servers.list[2] was not the element');

        // Trigger the change event
        cluster._handleChangeEvent({type: 'cluster', id: 'cluster1',
          servers: { up: 2, down: 0, unknown: 0, removed: ['localhost,/wlp/usr,server2'] },
          apps: {up: 1, down: 0, unknown: 0, changed: 
            [
             { name: 'snoop', state: 'STARTED', up: 2, down: 0, unknown: 0, servers: { up: 2, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1', 'localhost,/wlp/usr,server3' ]} }
             ]}
        });

        assert.equal(appOnCluster.state,       'STARTED',   'AppOnCluster.state did not have the correct updated value');
        assert.equal(appOnCluster.up,          2,           'AppOnCluster.up did not have the correct updated value');
        assert.equal(appOnCluster.down,        0,           'AppOnCluster.down did not have the correct updated value');
        assert.equal(appOnCluster.unknown,     0,           'AppOnCluster.unknown did not have the correct updated value');
        assert.equal(appOnCluster.scalingPolicy, 'default', 'AppOnCluster.scalingPolicy did not have the correct initialized value');
        assert.isTrue(appOnCluster.scalingPolicyEnabled,    'AppOnCluster.scalingPolicyEnabled did not have the correct initialized value');
        assert.equal(appOnCluster.servers.up,      2,       'AppOnCluster.servers.up did not have the correct initialized value');
        assert.equal(appOnCluster.servers.down,    0,       'AppOnCluster.servers.down did not have the correct initialized value');
        assert.equal(appOnCluster.servers.unknown, 0,       'AppOnCluster.servers.unknown did not have the correct initialized value');
        assert.isTrue(Array.isArray(appOnCluster.servers.list), 'AppOnCluster.servers.list was not an Array');
        assert.equal(appOnCluster.servers.list.length,  2,  'AppOnCluster.servers.list was not the correct size');
        assert.equal(appOnCluster.servers.list[0], 'localhost,/wlp/usr,server1', 'AppOnCluster.servers.list[0] was not the element');
        assert.equal(appOnCluster.servers.list[1], 'localhost,/wlp/usr,server3', 'AppOnCluster.servers.list[1] was not the element');

        // Validate the Observer was passed the correct tally objects after the first event
        assert.isNotNull(observer.newTally,             'AppOnClusterObserver.newTally did not get set, when it should have been');
        assert.equal(observer.newTally.up,      2, 'AppOnClusterObserver did not get the correct new value for the up tally');
        assert.equal(observer.newTally.down,    0, 'AppOnClusterObserver did not get the correct new value for the down tally');
        assert.equal(observer.newTally.unknown, 0, 'AppOnClusterObserver did not get the correct new value for the unknown tally');

        assert.isNotNull(observer.oldTally,             'AppOnClusterObserver.oldTally did not get set, when it should have been');
        assert.equal(observer.oldTally.up,      3, 'AppOnClusterObserver did not get the correct old value for the up tally');
        assert.equal(observer.oldTally.down,    0, 'AppOnClusterObserver did not get the correct old value for the down tally');
        assert.equal(observer.oldTally.unknown, 0, 'AppOnClusterObserver did not get the correct old value for the unknown tally');
      }),

      tdd.test('AppOnCluster operation - operational state changed to starting', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED', scalingPolicy: 'default', scalingPolicyEnabled: true,
          servers: { up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1'] },
          apps: { up: 0, down: 1, unknown: 0, partial: 0, list: 
            [ 
             { name: 'snoop', state: 'STOPPED', up: 0, down: 1, unknown: 0, servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ]}}
             ]
          }
        });
        var appOnCluster = new AppOnCluster({name: 'snoop', cluster: cluster});
        var observer = new AppOnClusterObserver();

        appOnCluster.subscribe(observer);

        assert.equal(appOnCluster.state,       'STOPPED',   'AppOnCluster.state did not have the correct initialized value');
        assert.equal(appOnCluster.up,          0,           'AppOnCluster.up did not have the correct initialized value');
        assert.equal(appOnCluster.down,        1,           'AppOnCluster.down did not have the correct initialized value');
        assert.equal(appOnCluster.unknown,     0,           'AppOnCluster.unknown did not have the correct initialized value');

        // Trigger the change event
        appOnCluster._handleChangeEvent({type: 'appOnCluster', id: 'snoop(cluster1)', state: 'STARTING'});

        assert.equal(appOnCluster.state,       'STARTING',  'AppOnCluster.state did not have the correct updated value');
        assert.equal(appOnCluster.up,          0,           'AppOnCluster.up did not have the correct updated value');
        assert.equal(appOnCluster.down,        1,           'AppOnCluster.down did not have the correct updated value');
        assert.equal(appOnCluster.unknown,     0,           'AppOnCluster.unknown did not have the correct updated value');

        // Validate the Observer onTallyChange method was not called
        assert.notOk(observer.newTally,             'AppOnClusterObserver.newTally should not be set when there is no effective tally change');
        assert.notOk(observer.oldTally,             'AppOnClusterObserver.oldTally should not be set when there is no effective tally change');
        assert.equal(observer.newState, 'STARTING', 'AppOnClusterObserver did not have the correct NEW value for state');
        assert.equal(observer.oldState,  'STOPPED', 'AppOnClusterObserver did not have the correct OLD value for state');
      }),

      tdd.test('AppOnCluster operation - operational state changed to stopping', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED', scalingPolicy: 'default', scalingPolicyEnabled: true,
          servers: { up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1'] },
          apps: { up: 1, down: 0, unknown: 0, partial: 0, list: 
            [ 
             { name: 'snoop', state: 'STARTED', up: 1, down: 0, unknown: 0, servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ]}}
             ]
          }
        });
        var appOnCluster = new AppOnCluster({name: 'snoop', cluster: cluster});
        var observer = new AppOnClusterObserver();

        appOnCluster.subscribe(observer);

        assert.equal(appOnCluster.state,       'STARTED',   'AppOnCluster.state did not have the correct initialized value');
        assert.equal(appOnCluster.up,          1,           'AppOnCluster.up did not have the correct initialized value');
        assert.equal(appOnCluster.down,        0,           'AppOnCluster.down did not have the correct initialized value');
        assert.equal(appOnCluster.unknown,     0,           'AppOnCluster.unknown did not have the correct initialized value');

        // Trigger the change event
        appOnCluster._handleChangeEvent({type: 'appOnCluster', id: 'snoop(cluster1)', state: 'STOPPING'});

        assert.equal(appOnCluster.state,       'STOPPING',  'AppOnCluster.state did not have the correct updated value');
        assert.equal(appOnCluster.up,          1,           'AppOnCluster.up did not have the correct updated value');
        assert.equal(appOnCluster.down,        0,           'AppOnCluster.down did not have the correct updated value');
        assert.equal(appOnCluster.unknown,     0,           'AppOnCluster.unknown did not have the correct updated value');

        // Validate the Observer onTallyChange method was not called
        assert.notOk(observer.newTally,             'AppOnClusterObserver.newTally should not be set when there is no effective tally change');
        assert.notOk(observer.oldTally,             'AppOnClusterObserver.oldTally should not be set when there is no effective tally change');
        assert.equal(observer.newState, 'STOPPING', 'AppOnClusterObserver did not have the correct NEW value for state');
        assert.equal(observer.oldState,  'STARTED', 'AppOnClusterObserver did not have the correct OLD value for state');
      }),

      tdd.test('AppOnCluster operation - final state change is ignored', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED', scalingPolicy: 'default', scalingPolicyEnabled: true,
          servers: { up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1'] },
          apps: { up: 1, down: 0, unknown: 0, partial: 0, list: 
            [ 
             { name: 'snoop', state: 'STARTED', up: 1, down: 0, unknown: 0, servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ]}}
             ]
          }
        });
        var appOnCluster = new AppOnCluster({name: 'snoop', cluster: cluster});
        var observer = new AppOnClusterObserver();

        appOnCluster.subscribe(observer);

        // Trigger a State Event to indicate it is now starting
        appOnCluster._handleChangeEvent({type: 'appOnCluster', id: 'snoop(cluster1)', state: 'STOPPED'});
        appOnCluster._handleChangeEvent({type: 'appOnCluster', id: 'snoop(cluster1)', state: 'STARTED'});

        assert.equal(appOnCluster.state, 'STARTED', 'AppOnCluster.state did not have the correct updated value');
        assert.notOk(observer.newState,             'AppOnClusterObserver should not have a value for the new value for state');
        assert.notOk(observer.oldState,             'AppOnClusterObserver should not have a value for the old value for state');
      }),

      tdd.test('Cluster changes - cluster is destroyed', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED', scalingPolicy: 'default', scalingPolicyEnabled: true,
          servers: { up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1'] },
          apps: { up: 1, down: 0, unknown: 0, partial: 0, list: 
            [ 
             { name: 'snoop', state: 'STARTED', up: 1, down: 0, unknown: 0, servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ]}}
             ]
          }
        });
        var appOnCluster = new AppOnCluster({name: 'snoop', cluster: cluster});
        var observer = new AppOnClusterObserver();

        appOnCluster.subscribe(observer);

        // Server removes the app
        cluster._handleChangeEvent({type: 'cluster', id: 'cluster1', state: 'removed'});

        assert.equal('STARTED',   appOnCluster.state,         'AppOnCluster.state did not have the correct initialized value. It should not change in response to a "removed" event state');
        assert.isTrue(appOnCluster.isDestroyed,                 'AppOnCluster.isDestroyed flag did not get set in response to a "removed" event');

        // Confirm the application is destroyed
        assert.isTrue(observer.destroyed,                       'AppOnClusterObserver.onDestroyed did not get called');
      });
      
      tdd.test('AppOnCluster update - Cluster gets a STOPPED app change', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED', scalingPolicy: 'default', scalingPolicyEnabled: true,
          servers: { up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1'] },
          apps: { up: 1, down: 0, unknown: 0, partial: 0, list: 
            [ 
             { name: 'snoop', state: 'STARTED', up: 1, down: 0, unknown: 0, servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ]}}
             ]
          }
        });
        var appOnCluster = new AppOnCluster({name: 'snoop', cluster: cluster});
        var observer = new AppOnClusterObserver();

        appOnCluster.subscribe(observer);

        // Trigger a Cluster Event to indicate that the application is now stopped (with alerts)
        cluster._handleChangeEvent({type: 'cluster', id: 'cluster1', apps: {changed: [{name: 'snoop', state: 'STOPPED'}] },
          alerts: { count: 1, unknown: [], app: [ { name: 'cluster1,snoop', servers: ['localhost,/wlp/usr,server1'] } ] } });

        assert.equal(appOnCluster.state,                         'STOPPED',   'AppOnCluster.state did not have the correct updated value');
        assert.equal(appOnCluster.alerts.count,                  1,           'AppOnCluster.alerts.count should now be 1');
        assert.equal(appOnCluster.alerts.app.length,             1,           'AppOnCluster.alerts.app.length should now be length 1');
        assert.equal(appOnCluster.alerts.app[0].name,            'cluster1,snoop',  'AppOnCluster.alerts.app[0].name should now be snoop');
        assert.equal(appOnCluster.alerts.app[0].servers.length,  1,           'AppOnCluster.alerts.app[0].servers.length should now be length 1');
        assert.equal(appOnCluster.alerts.app[0].servers[0],      'localhost,/wlp/usr,server1', 'AppOnCluster.alerts.app[0].servers[0] should now be server1');
        assert.equal(observer.newState,                          'STOPPED',   'AppOnClusterObserver should not have a value for the new value for state');
        assert.equal(observer.oldState,                          'STARTED',   'AppOnClusterObserver should not have a value for the old value for state');
        assert.equal(observer.newAlerts.count,                   1,           'AppOnClusterObserver should have 1 count as the new alerts object');
        assert.equal(observer.oldAlerts.count,                   0,           'AppOnClusterObserver should have 0 count as the old alerts object');
      });
      
      tdd.test('AppOnCluster update - Cluster gets a STOPPING app change', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED', scalingPolicy: 'default', scalingPolicyEnabled: true,
          servers: { up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1'] },
          apps: { up: 1, down: 0, unknown: 0, partial: 0, list: 
            [ 
             { name: 'snoop', state: 'STARTED', up: 1, down: 0, unknown: 0, servers: { up: 1, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1' ]}}
             ]
          }
        });
        var appOnCluster = new AppOnCluster({name: 'snoop', cluster: cluster});
        var observer = new AppOnClusterObserver();

        appOnCluster.subscribe(observer);

        // Trigger a Server Event to indicate that the application is now stopped (with alerts)
        cluster._handleChangeEvent({type: 'cluster', id: 'cluster1', apps: {changed: [{name: 'snoop', state: 'STOPPING'}] },
          alerts: { count: 1, unknown: [], app: [ { name: 'cluster1,snoop', servers: ['localhost,/wlp/usr,server1'] } ] } });

        assert.equal(appOnCluster.state,                         'STOPPING',   'AppOnCluster.state did not have the correct updated value');
        assert.equal(appOnCluster.alerts.count,                  1,           'AppOnCluster.alerts.count should now be 1');
        assert.equal(appOnCluster.alerts.app.length,             1,           'AppOnCluster.alerts.app.length should now be length 1');
        assert.equal(appOnCluster.alerts.app[0].name,            'cluster1,snoop', 'AppOnCluster.alerts.app[0].name should now be snoop');
        assert.equal(appOnCluster.alerts.app[0].servers.length,  1,           'AppOnCluster.alerts.app[0].servers.length should now be length 1');
        assert.equal(appOnCluster.alerts.app[0].servers[0],      'localhost,/wlp/usr,server1', 'AppOnCluster.alerts.app[0].servers[0] should now be server1');
        assert.equal(observer.newState,                          'STOPPING',   'AppOnClusterObserver should not have a value for the new value for state');
        assert.equal(observer.oldState,                          'STARTED',   'AppOnClusterObserver should not have a value for the old value for state');
        assert.equal(observer.newAlerts.count,                   1,           'AppOnClusterObserver should have 1 count as the new alerts object');
        assert.equal(observer.oldAlerts.count,                   0,           'AppOnClusterObserver should have 0 count as the old alerts object');
      });
      
    });
  }
});
