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
 * Test cases for AppOnServer
 */
define([ 
        "intern!tdd",
        "intern/chai!assert",
        "intern/dojo/declare",
        "resources/_objects/Server",
        "resources/_objects/Cluster",
        "resources/_derived/objects/AppOnServer",
        "resources/_derived/objects/AppOnCluster",
        "resources/Observer",
        "resources/resourceManager"
        ],
        function(tdd, assert, declare, Server, Cluster, AppOnServer, AppOnCluster, Observer, ResourceManager) {

  var AppOnServerObserver = declare([Observer], {
    id: 'testObserver',

    onStateChange: function(newState, oldState) {
      this.newState = newState;
      this.oldState = oldState;
    },

    onClusterChange: function(newCluster, oldCluster) {
      this.newCluster = newCluster;
      this.oldCluster = oldCluster;
    },

    onScalingPolicyChange: function(newScalingPolicy, oldScalingPolicy) {
      this.newScalingPolicy = newScalingPolicy;
      this.oldScalingPolicy = oldScalingPolicy;
    },

    onScalingPolicyEnabledChange: function(newScalingPolicyEnabled, oldScalingPolicyEnabled) {
      this.newScalingPolicyEnabled = newScalingPolicyEnabled;
      this.oldScalingPolicyEnabled = oldScalingPolicyEnabled;
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
    },

    onAlertsChange: function(newAlerts, oldAlerts) {
      this.newAlerts = newAlerts;
      this.oldAlerts = oldAlerts;
    }
  });

  with(assert) {

    /**
     * Defines the 'AppOnServer Object Tests' module test suite.
     */
    tdd.suite('AppOnServer Object Tests', function() {

      tdd.test('constructor - no initialization object', function() {
        try {
          new AppOnServer();
          assert.isTrue(false, 'AppOnServer was successfully created when it should have failed - an initialization object is required');
        } catch(error) {
          assert.equal(error, 'AppOnServer created without an initialization object', 'Error reported did not match expected error');
        }
      }),

      tdd.test('constructor - no name', function() {
        try {
          new AppOnServer({server: {/*...*/}});
          assert.isTrue(false, 'AppOnServer was successfully created when it should have failed - a name is required');
        } catch(error) {
          assert.equal(error, 'AppOnServer created without a name', 'Error reported did not match expected error');
        }
      }),

      tdd.test('constructor - no server', function() {
        try {
          new AppOnServer({name: 'snoop'});
          assert.isTrue(false, 'AppOnServer was successfully created when it should have failed - a server is required');
        } catch(error) {
          assert.equal(error, 'AppOnServer created without a server', 'Error reported did not match expected error');
        }
      }),

      tdd.test('constructor - server is not clustered', function() {
        var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', apps: {up: 1, down: 0, unknown: 0, list: [{name: 'snoop', state: 'STOPPED'}]}});
        var app = new AppOnServer({server: server, name: 'snoop'});

        assert.equal(app.id, 'snoop(localhost,/wlp/usr,server1)', 'AppOnServer.id did not have the correct initialized value');
        assert.equal(app.name,          'snoop',     'AppOnServer.name did not have the correct initialized value');
        assert.equal(app.state,         'STOPPED',   'AppOnServer.state did not have the correct initialized value');
        assert.equal(app.cluster,       null,        'AppOnServer.cluster did not have the correct initialized value');
        assert.equal(app.scalingPolicy, null,        'AppOnServer.scalingPolicy did not have the correct initialized value');
        assert.isFalse(app.scalingPolicyEnabled,     'AppOnServer.scalingPolicyEnabled did not have the correct initialized value');
      }),

      tdd.test('constructor - server is clustered with scalingPolicy', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED', scalingPolicy: 'default', scalingPolicyEnabled: true, 
          servers: { up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']},
          apps: { up: 1, down: 0, unknown: 0, partial: 0, list: 
            [ 
             { name: 'snoop', state: 'STARTED', up: 2, down: 0, unknown: 0, servers: { up: 2, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1']}} 
             ]
          }
        });
        var appOnCluster = new AppOnCluster({name: 'snoop', cluster: cluster});
        var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', cluster: 'cluster1', scalingPolicy: 'default', scalingPolicyEnabled: true, apps: {up: 1, down: 0, unknown: 0, list: [{name: 'snoop', state: 'STARTED'}]}});
        server.resourceManager = ResourceManager;
        var app = new AppOnServer({cluster: 'cluster1', server: server, name: 'snoop'});

        assert.equal(app.id, 'snoop(localhost,/wlp/usr,server1)', 'AppOnServer.id did not have the correct initialized value');
        assert.equal(app.name,     'snoop',     'AppOnServer.name did not have the correct initialized value');
        assert.equal(app.state,    'STARTED',   'AppOnServer.state did not have the correct initialized value');
        assert.equal(app.cluster,  'cluster1',  'AppOnServer.cluster did not have the correct initialized value');
        assert.equal(app.scalingPolicy, 'default',  'AppOnServer.scalingPolicy did not have the correct initialized value');
        assert.isTrue(app.scalingPolicyEnabled,     'AppOnServer.scalingPolicyEnabled did not have the correct initialized value');
      }),

      tdd.test('constructor - app not present in server apps list', function() {
        var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', apps: {up: 0, down: 0, unknown: 0, list: []}});
        var app = new AppOnServer({server: server, name: 'snoop'});

        assert.equal(app.id, 'snoop(localhost,/wlp/usr,server1)', 'AppOnServer.id did not have the correct initialized value');
        assert.equal(app.name,          'snoop',     'AppOnServer.name did not have the correct initialized value');
        assert.equal(app.state,         'UNKNOWN',   'AppOnServer.state did not have the correct initialized value');
        assert.equal(app.cluster,       null,        'AppOnServer.cluster did not have the correct initialized value');
        assert.equal(app.scalingPolicy, null,        'AppOnServer.scalingPolicy did not have the correct initialized value');
        assert.isFalse(app.scalingPolicyEnabled,     'AppOnServer.scalingPolicyEnabled did not have the correct initialized value');
      }),

      tdd.test('Server changes - changed application state', function() {
        var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', apps: {up: 1, down: 0, unknown: 0, list: [{name: 'snoop', state: 'STARTED'}]}});
        var app = new AppOnServer({server: server, name: 'snoop'});
        var observer = new AppOnServerObserver();

        app.subscribe(observer);

        // Initial sanity check
        assert.equal(app.state,         'STARTED',   'AppOnServer.state did not have the correct initialized value');

        // Simulate the app state change event
        server._handleChangeEvent({type: 'server', id: 'localhost,/wlp/usr,server1', apps: {up: 0, down: 1, unknown: 0, changed: [{name: 'snoop', state: 'STOPPED'}]}});

        assert.equal(app.state,         'STOPPED',   'AppOnServer.state did not have the correct updated value');
        assert.equal(app.cluster,       null,        'AppOnServer.cluster did not have the correct initialized value');
        assert.equal(app.scalingPolicy, null,        'AppOnServer.scalingPolicy did not have the correct initialized value');
        assert.isFalse(app.scalingPolicyEnabled,     'AppOnServer.scalingPolicyEnabled did not have the correct initialized value');

        assert.equal(observer.newState, 'STOPPED',   'AppOnServerObserver did not get the correct NEW value for state');
        assert.equal(observer.oldState, 'STARTED',   'AppOnServerObserver did not get the correct OLD value for state');
      }),


      tdd.test('Server changes - changed tags', function() {
        var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', apps: {up: 1, down: 0, unknown: 0, list: [{name: 'snoop', state: 'STARTED'}]}});
        var app = new AppOnServer({server: server, name: 'snoop'});
        var observer = new AppOnServerObserver();

        app.subscribe(observer);

        // Simulate the app tags change event
        server._handleChangeEvent({type: 'server', id: 'localhost,/wlp/usr,server1', apps: {up: 0, down: 1, unknown: 0, changed: [{name: 'snoop', state: 'STARTED', tags:['new']}]}});

        assert.equal(app.state,         'STARTED',   'AppOnServer.state did not have the correct updated value');
        assert.equal(app.cluster,       null,        'AppOnServer.cluster did not have the correct initialized value');
        assert.equal(app.scalingPolicy, null,        'AppOnServer.scalingPolicy did not have the correct initialized value');
        assert.isFalse(app.scalingPolicyEnabled,     'AppOnServer.scalingPolicyEnabled did not have the correct initialized value');

        assert.equal(JSON.stringify(observer.newTags), JSON.stringify(['new']),   'AppOnServerObserver did not get the correct NEW value for tags');
        assert.equal(observer.oldTags, null,   'AppOnServerObserver did not get the correct OLD value for tags');
      }),

      tdd.test('Server changes - changed owner', function() {
        var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', apps: {up: 1, down: 0, unknown: 0, list: [{name: 'snoop', state: 'STARTED',tags:['new'], owner:'Bob'}]}});
        var app = new AppOnServer({server: server, name: 'snoop'});
        var observer = new AppOnServerObserver();

        app.subscribe(observer);

        // Simulate the app owner change event
        server._handleChangeEvent({type: 'server', id: 'localhost,/wlp/usr,server1', apps: {up: 0, down: 1, unknown: 0, changed: [{name: 'snoop', state: 'STARTED', tags:['new'], owner:'Bill'}]}});

        assert.equal(app.state,         'STARTED',   'AppOnServer.state did not have the correct updated value');
        assert.equal(app.cluster,       null,        'AppOnServer.cluster did not have the correct initialized value');
        assert.equal(app.scalingPolicy, null,        'AppOnServer.scalingPolicy did not have the correct initialized value');
        assert.isFalse(app.scalingPolicyEnabled,     'AppOnServer.scalingPolicyEnabled did not have the correct initialized value');

        assert.equal(observer.newOwner, 'Bill',   'AppOnServerObserver did not get the correct NEW value for owner');
        assert.equal(observer.oldOwner, 'Bob',   'AppOnServerObserver did not get the correct OLD value for owner');
      }),

      tdd.test('Server changes - changed contacts', function() {
        var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', apps: {up: 1, down: 0, unknown: 0, list: [{name: 'snoop', state: 'STARTED',tags:['new'], owner:'Bob', contacts:null}]}});
        var app = new AppOnServer({server: server, name: 'snoop'});
        var observer = new AppOnServerObserver();

        app.subscribe(observer);

        // Simulate the app contacts change event
        server._handleChangeEvent({type: 'server', id: 'localhost,/wlp/usr,server1', apps: {up: 0, down: 1, unknown: 0, changed: [{name: 'snoop', state: 'STARTED', tags:['new'], owner:'Bob', contacts:['Wendy', 'Don']}]}});

        assert.equal(app.state,         'STARTED',   'AppOnServer.state did not have the correct updated value');
        assert.equal(app.cluster,       null,        'AppOnServer.cluster did not have the correct initialized value');
        assert.equal(app.scalingPolicy, null,        'AppOnServer.scalingPolicy did not have the correct initialized value');
        assert.isFalse(app.scalingPolicyEnabled,     'AppOnServer.scalingPolicyEnabled did not have the correct initialized value');

        assert.equal(JSON.stringify(observer.newContacts), JSON.stringify(['Wendy', 'Don']),   'AppOnServerObserver did not get the correct NEW value for contacts');
        assert.equal(observer.oldContacts, null,   'AppOnServerObserver did not get the correct OLD value for contacts');
      }),

      tdd.test('Server changes - changed note', function() {
        var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', apps: {up: 1, down: 0, unknown: 0, list: [{name: 'snoop', state: 'STARTED',tags:['new'], owner:'Bob', note:null}]}});
        var app = new AppOnServer({server: server, name: 'snoop'});
        var observer = new AppOnServerObserver();

        app.subscribe(observer);

        // Simulate the app note change event
        server._handleChangeEvent({type: 'server', id: 'localhost,/wlp/usr,server1', apps: {up: 0, down: 1, unknown: 0, changed: [{name: 'snoop', state: 'STARTED', tags:['new'], owner:'Bob', note:'Hotel California'}]}});

        assert.equal(app.state,         'STARTED',   'AppOnServer.state did not have the correct updated value');
        assert.equal(app.cluster,       null,        'AppOnServer.cluster did not have the correct initialized value');
        assert.equal(app.scalingPolicy, null,        'AppOnServer.scalingPolicy did not have the correct initialized value');
        assert.isFalse(app.scalingPolicyEnabled,     'AppOnServer.scalingPolicyEnabled did not have the correct initialized value');

        assert.equal(observer.newNote, 'Hotel California',   'AppOnServerObserver did not get the correct NEW value for note');
        assert.equal(observer.oldNote, null,   'AppOnServerObserver did not get the correct OLD value for note');
      }),

      tdd.test('Server changes - changed application state to INSTALLED (non-standard value)', function() {
        var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', apps: {up: 1, down: 0, unknown: 0, list: [{name: 'snoop', state: 'STARTED'}]}});
        var app = new AppOnServer({server: server, name: 'snoop'});
        var observer = new AppOnServerObserver();

        app.subscribe(observer);

        // Initial sanity check
        assert.equal('STARTED',   app.state,         'AppOnServer.state did not have the correct initialized value');

        // Simulate the app state change event
        server._handleChangeEvent({type: 'server', id: 'localhost,/wlp/usr,server1', apps: {up: 0, down: 1, unknown: 0, changed: [{name: 'snoop', state: 'INSTALLED'}]}});

        assert.equal(app.state,         'INSTALLED', 'AppOnServer.state did not have the correct updated value');
        assert.equal(app.cluster,       null,        'AppOnServer.cluster did not have the correct initialized value');
        assert.equal(app.scalingPolicy, null,        'AppOnServer.scalingPolicy did not have the correct initialized value');
        assert.isFalse(app.scalingPolicyEnabled,     'AppOnServer.scalingPolicyEnabled did not have the correct initialized value');

        assert.equal(observer.newState, 'INSTALLED', 'AppOnServerObserver did not get the correct NEW value for state');
        assert.equal(observer.oldState, 'STARTED',   'AppOnServerObserver did not get the correct OLD value for state');
      }),

      tdd.test('Server changes - added to cluster', function() {
        var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', apps: {up: 1, down: 0, unknown: 0, list: [{name: 'snoop', state: 'STARTED'}]}});
        var app = new AppOnServer({server: server, name: 'snoop'});
        var observer = new AppOnServerObserver();

        app.subscribe(observer);

        // Initial sanity check
        assert.equal(app.cluster,       null,        'AppOnServer.cluster did not have the correct initialized value - should have no cluster');

        // Simulate the cluster event
        server._handleChangeEvent({type: 'server', id: 'localhost,/wlp/usr,server1', cluster: 'cluster1' });

        assert.equal(app.state,         'STARTED',   'AppOnServer.state did not have the correct initialized value');
        assert.equal(app.cluster,       'cluster1',  'AppOnServer.cluster did not have the correct updated value');
        assert.equal(app.scalingPolicy, null,        'AppOnServer.cluster did not have the correct initialized value');
        assert.isFalse(app.scalingPolicyEnabled,     'AppOnServer.scalingPolicyEnabled did not have the correct initialized value');

        assert.equal(observer.newCluster, 'cluster1',  'AppOnServerObserver did not get the correct NEW value for cluster');
        assert.equal(observer.oldCluster, null,        'AppOnServerObserver did not get the correct OLD value for cluster');
      }),

      tdd.test('Server changes - changed cluster', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED', scalingPolicy: 'default', scalingPolicyEnabled: true, 
          servers: { up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']},
          apps: { up: 1, down: 0, unknown: 0, partial: 0, list: 
            [ 
             { name: 'snoop', state: 'STARTED', up: 2, down: 0, unknown: 0, servers: { up: 2, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1']}} 
             ]
          }
        });
        var appOnCluster = new AppOnCluster({name: 'snoop', cluster: cluster});
        var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', cluster: 'cluster1', apps: {up: 1, down: 0, unknown: 0, list: [{name: 'snoop', state: 'STARTED'}]}});
        server.resourceManager = ResourceManager;
        var app = new AppOnServer({server: server, name: 'snoop'});
        var observer = new AppOnServerObserver();

        app.subscribe(observer);

        // Initial sanity check
        assert.equal(app.cluster,       'cluster1',  'AppOnServer.cluster did not have the correct initialized value');

        // Simulate the cluster change event
        server._handleChangeEvent({type: 'server', id: 'localhost,/wlp/usr,server1', cluster: 'cluster2' });

        assert.equal(app.state,         'STARTED',   'AppOnServer.state did not have the correct initialized value');
        assert.equal(app.cluster,       'cluster2',  'AppOnServer.cluster did not have the correct updated value');
        assert.equal(app.scalingPolicy, null,        'AppOnServer.cluster did not have the correct initialized value');
        assert.isFalse(app.scalingPolicyEnabled,     'AppOnServer.scalingPolicyEnabled did not have the correct initialized value');

        assert.equal(observer.newCluster, 'cluster2',  'AppOnServerObserver did not get the correct NEW value for cluster');
        assert.equal(observer.oldCluster, 'cluster1',  'AppOnServerObserver did not get the correct OLD value for cluster');
      }),

      tdd.test('Server changes - remove cluster', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED', scalingPolicy: 'default', scalingPolicyEnabled: true, 
          servers: { up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']},
          apps: { up: 1, down: 0, unknown: 0, partial: 0, list: 
            [ 
             { name: 'snoop', state: 'STARTED', up: 2, down: 0, unknown: 0, servers: { up: 2, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1']}} 
             ]
          }
        });
        var appOnCluster = new AppOnCluster({name: 'snoop', cluster: cluster});
        var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', cluster: 'cluster1', apps: {up: 1, down: 0, unknown: 0, list: [{name: 'snoop', state: 'STARTED'}]}});
        server.resourceManager = ResourceManager;
        var app = new AppOnServer({server: server, name: 'snoop'});
        var observer = new AppOnServerObserver();

        app.subscribe(observer);

        // Initial sanity check
        assert.equal(app.cluster,       'cluster1',  'AppOnServer.cluster did not have the correct initialized value');

        // Simulate the cluster change event
        server._handleChangeEvent({type: 'server', id: 'localhost,/wlp/usr,server1', cluster: null });

        assert.equal(app.state,         'STARTED',   'AppOnServer.state did not have the correct initialized value');
        assert.equal(app.cluster,       null,        'AppOnServer.cluster did not have the correct updated value');
        assert.equal(app.scalingPolicy, null,        'AppOnServer.cluster did not have the correct initialized value');
        assert.isFalse(app.scalingPolicyEnabled,     'AppOnServer.scalingPolicyEnabled did not have the correct initialized value');

        assert.equal(observer.newCluster, null,        'AppOnServerObserver did not get the correct NEW value for cluster');
        assert.equal(observer.oldCluster, 'cluster1',  'AppOnServerObserver did not get the correct OLD value for cluster');
      }),

      tdd.test('Server changes - added to a scalingPolicy', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED', scalingPolicyEnabled: false, 
          servers: { up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']},
          apps: { up: 1, down: 0, unknown: 0, partial: 0, list: 
            [ 
             { name: 'snoop', state: 'STARTED', up: 2, down: 0, unknown: 0, servers: { up: 2, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1']}} 
             ]
          }
        });
        var appOnCluster = new AppOnCluster({name: 'snoop', cluster: cluster});
        var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', cluster: 'cluster1', apps: {up: 1, down: 0, unknown: 0, list: [{name: 'snoop', state: 'STARTED'}]}});
        server.resourceManager = ResourceManager;
        var app = new AppOnServer({server: server, name: 'snoop'});
        var observer = new AppOnServerObserver();

        app.subscribe(observer);

        // Initial sanity check
        assert.equal(app.scalingPolicy,  null,        'AppOnServer.scalingPolicy did not have the correct initialized value');

        // Simulate the scaling policy change event
        server._handleChangeEvent({type: 'server', id: 'localhost,/wlp/usr,server1', scalingPolicy: 'default' });

        assert.equal(app.state,         'STARTED',   'AppOnServer.state did not have the correct initialized value');
        assert.equal(app.cluster,       'cluster1',  'AppOnServer.cluster did not have the correct initialized value');
        assert.equal(app.scalingPolicy, 'default',   'AppOnServer.cluster did not have the correct updated value');
        assert.isFalse(app.scalingPolicyEnabled,     'AppOnServer.scalingPolicyEnabled did not have the correct initialized value');

        assert.equal(observer.newScalingPolicy, 'default', 'AppOnServerObserver did not get the correct NEW value for scalingPolicy');
        assert.equal(observer.oldScalingPolicy, null,      'AppOnServerObserver did not get the correct OLD value for scalingPolicy');
      }),

      tdd.test('Server changes - changed scalingPolicy', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED', scalingPolicy: 'default', scalingPolicyEnabled: true, 
          servers: { up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']},
          apps: { up: 1, down: 0, unknown: 0, partial: 0, list: 
            [ 
             { name: 'snoop', state: 'STARTED', up: 2, down: 0, unknown: 0, servers: { up: 2, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1']}} 
             ]
          }
        });
        var appOnCluster = new AppOnCluster({name: 'snoop', cluster: cluster});
        var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', cluster: 'cluster1', scalingPolicy: 'default', apps: {up: 1, down: 0, unknown: 0, list: [{name: 'snoop', state: 'STARTED'}]}});
        server.resourceManager = ResourceManager;
        var app = new AppOnServer({server: server, name: 'snoop'});
        var observer = new AppOnServerObserver();

        app.subscribe(observer);

        // Initial sanity check
        assert.equal(app.scalingPolicy,  'default',   'AppOnServer.scalingPolicy did not have the correct initialized value');

        // Simulate the scaling policy change event
        server._handleChangeEvent({type: 'server', id: 'localhost,/wlp/usr,server1', scalingPolicy: 'newPolicy' });

        assert.equal(app.state,         'STARTED',   'AppOnServer.state did not have the correct initialized value');
        assert.equal(app.cluster,       'cluster1',  'AppOnServer.cluster did not have the correct initialized value');
        assert.equal(app.scalingPolicy, 'newPolicy', 'AppOnServer.cluster did not have the correct updated value');
        assert.isFalse(app.scalingPolicyEnabled,     'AppOnServer.scalingPolicyEnabled did not have the correct initialized value');

        assert.equal(observer.newScalingPolicy, 'newPolicy', 'AppOnServerObserver did not get the correct NEW value for scalingPolicy');
        assert.equal(observer.oldScalingPolicy, 'default',   'AppOnServerObserver did not get the correct OLD value for scalingPolicy');
      }),

      tdd.test('Server changes - removed scalingPolicy', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED', scalingPolicy: 'default', scalingPolicyEnabled: true, 
          servers: { up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']},
          apps: { up: 1, down: 0, unknown: 0, partial: 0, list: 
            [ 
             { name: 'snoop', state: 'STARTED', up: 2, down: 0, unknown: 0, servers: { up: 2, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1']}} 
             ]
          }
        });
        var appOnCluster = new AppOnCluster({name: 'snoop', cluster: cluster});
        var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', cluster: 'cluster1', scalingPolicy: 'default', apps: {up: 1, down: 0, unknown: 0, list: [{name: 'snoop', state: 'STARTED'}]}});
        server.resourceManager = ResourceManager;
        var app = new AppOnServer({server: server, name: 'snoop'});
        var observer = new AppOnServerObserver();

        app.subscribe(observer);

        // Initial sanity check
        assert.equal(app.scalingPolicy,  'default',   'AppOnServer.scalingPolicy did not have the correct initialized value');

        // Simulate the scaling policy change event
        server._handleChangeEvent({type: 'server', id: 'localhost,/wlp/usr,server1', scalingPolicy: null });

        assert.equal(app.state,         'STARTED',   'AppOnServer.state did not have the correct initialized value');
        assert.equal(app.cluster,       'cluster1',  'AppOnServer.cluster did not have the correct initialized value');
        assert.equal(app.scalingPolicy, null,        'AppOnServer.cluster did not have the correct updated value');
        assert.isFalse(app.scalingPolicyEnabled,     'AppOnServer.scalingPolicyEnabled did not have the correct initialized value');

        assert.equal(observer.newScalingPolicy, null,        'AppOnServerObserver did not get the correct NEW value for scalingPolicy');
        assert.equal(observer.oldScalingPolicy, 'default',   'AppOnServerObserver did not get the correct OLD value for scalingPolicy');
      }),

      tdd.test('Server changes - changed scalingPolicyEnabled', function() {
        var cluster = new Cluster({id: 'cluster1', state: 'STARTED', scalingPolicy: 'default', scalingPolicyEnabled: true, 
          servers: { up: 1, down: 0, unknown: 0, list: ['localhost,/wlp/usr,server1']},
          apps: { up: 1, down: 0, unknown: 0, partial: 0, list: 
            [ 
             { name: 'snoop', state: 'STARTED', up: 2, down: 0, unknown: 0, servers: { up: 2, down: 0, unknown: 0, ids: [ 'localhost,/wlp/usr,server1']}} 
             ]
          }
        });
        var appOnCluster = new AppOnCluster({name: 'snoop', cluster: cluster});
        var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', cluster: 'cluster1', scalingPolicy: 'default', scalingPolicyEnabled: true, apps: {up: 1, down: 0, unknown: 0, list: [{name: 'snoop', state: 'STARTED'}]}});
        server.resourceManager = ResourceManager;
        var app = new AppOnServer({server: server, name: 'snoop'});
        var observer = new AppOnServerObserver();

        app.subscribe(observer);

        // Initial sanity check
        assert.equal(app.scalingPolicy, 'default',    'AppOnServer.scalingPolicy did not have the correct initialized value');
        assert.isTrue(app.scalingPolicyEnabled,       'AppOnServer.scalingPolicyEnabled did not have the correct initialized value');

        // Simulate the scaling policy change event
        server._handleChangeEvent({type: 'server', id: 'localhost,/wlp/usr,server1', scalingPolicyEnabled: false });

        assert.equal(app.state, 'STARTED',            'AppOnServer.state did not have the correct initialized value');
        assert.equal(app.cluster, 'cluster1',         'AppOnServer.cluster did not have the correct initialized value');
        assert.equal(app.scalingPolicy, 'default',    'AppOnServer.scalingPolicy did not have the correct initialized value');
        assert.isFalse(app.scalingPolicyEnabled,      'AppOnServer.scalingPolicyEnabled did not have the correct updated value');

        assert.isFalse(observer.newScalingPolicyEnabled, 'AppOnServerObserver did not get the correct NEW value for scalingPolicyEnabled');
        assert.isTrue(observer.oldScalingPolicyEnabled,  'AppOnServerObserver did not get the correct OLD value for scalingPolicyEnabled');
      }),

      tdd.test('Server changes - removed app destroys and unsubscribes', function() {
        var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', apps: {up: 1, down: 0, unknown: 0, list: [{name: 'snoop', state: 'STARTED'}]}});
        var app = new AppOnServer({server: server, name: 'snoop'});
        var observer = new AppOnServerObserver();

        app.subscribe(observer);

        // Server removes the app
        server._handleChangeEvent({type: 'server', id: 'localhost,/wlp/usr,server1', apps: {up: 0, down: 0, unknown: 0, removed: ['snoop']}});

        assert.equal(app.state,         'STARTED',   'Application.state did not have the correct initialized value');
        assert.equal(app.cluster,       null,        'AppOnServer.cluster did not have the correct initialized value');
        assert.equal(app.scalingPolicy, null,        'AppOnServer.scalingPolicy did not have the correct initialized value');
        assert.isTrue(app.isDestroyed,                 'AppOnServer.isDestroyed flag did not get set in response to a "removed" event');

        // Confirm the application is destroyed
        assert.isTrue(observer.destroyed,              'AppOnServerObserver.onDestroyed did not get called');

        // Send another event to the server, confirm no change is made to the App or Observer
        server._handleChangeEvent({type: 'server', id: 'localhost,/wlp/usr,server1', cluster: 'cluster2' });

        // Make sure the AppOnServer was unsubscribed and does not change in response to the previous Server event
        assert.equal(app.state,         'STARTED',   'Application.state did not have the correct initialized value. It should not change in response to a "removed" event state');
        assert.equal(app.cluster,       null,        'AppOnServer.cluster did not have the correct initialized value, it changed after the AppOnServer was destroyed');
        assert.equal(app.scalingPolicy, null,        'AppOnServer.scalingPolicy did not have the correct initialized value');

        assert.notOk(observer.newCluster, 'AppOnServerObserver should not have the cluster attribute changed after its been destroyed');
        assert.notOk(observer.oldCluster, 'AppOnServerObserver should not have the cluster attribute changed after its been destroyed');
      }),

      tdd.test('Server changes - server was removed from the collective', function() {
        var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', apps: {up: 1, down: 0, unknown: 0, list: [{name: 'snoop', state: 'STARTED'}]}});
        var app = new AppOnServer({server: server, name: 'snoop'});
        var observer = new AppOnServerObserver();

        app.subscribe(observer);

        // Server removes the app
        server._handleChangeEvent({type: 'server', id: 'localhost,/wlp/usr,server1', state: 'removed'});

        assert.equal(app.state,         'STARTED',   'AppOnServer.state did not have the correct initialized value. It should not change in response to a "removed" event state');
        assert.equal(app.cluster,       null,        'AppOnServer.cluster did not have the correct initialized value');
        assert.equal(app.scalingPolicy, null,        'AppOnServer.scalingPolicy did not have the correct initialized value');
        assert.isTrue(app.isDestroyed,                 'AppOnServer.isDestroyed flag did not get set in response to a "removed" event');

        // Confirm the application is destroyed
        assert.isTrue(observer.destroyed,              'AppOnServerObserver.onDestroyed did not get called');
      }),

      tdd.test('AppOnServer operation - state changes to starting', function() {
        var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STOPPED', apps: {up: 1, down: 0, unknown: 0, list: [{name: 'snoop', state: 'STOPPED'}]}});
        var app = new AppOnServer({server: server, name: 'snoop'});
        var observer = new AppOnServerObserver();

        app.subscribe(observer);

        // Trigger a State Event to indicate it is now starting
        app._handleChangeEvent({type: 'appOnServer', id: 'snoop(localhost,/wlp/usr,server1)', state: 'STARTING'});

        assert.equal(app.state,         'STARTING',   'AppOnServer.state did not have the correct updated value');
        assert.equal(observer.newState, 'STARTING',   'AppOnServerObserver did not have the correct new value for state');
        assert.equal(observer.oldState, 'STOPPED',    'AppOnServerObserver did not have the correct old value for state');
      }),

      tdd.test('AppOnServer operation - state changes to stopping', function() {
        var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', apps: {up: 1, down: 0, unknown: 0, list: [{name: 'snoop', state: 'STARTED'}]}});
        var app = new AppOnServer({server: server, name: 'snoop'});
        var observer = new AppOnServerObserver();

        app.subscribe(observer);

        // Trigger a State Event to indicate it is now starting
        app._handleChangeEvent({type: 'appOnServer', id: 'snoop(localhost,/wlp/usr,server1)', state: 'STOPPING'});

        assert.equal(app.state,         'STOPPING',   'AppOnServer.state did not have the correct updated value');
        assert.equal(observer.newState, 'STOPPING',   'AppOnServerObserver did not have the correct new value for state');
        assert.equal(observer.oldState, 'STARTED',    'AppOnServerObserver did not have the correct old value for state');
      }),

      tdd.test('AppOnServer operation - state changes to restarted', function() {
        var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', apps: {up: 1, down: 0, unknown: 0, list: [{name: 'snoop', state: 'STARTED'}]}});
        var app = new AppOnServer({server: server, name: 'snoop'});
        var observer = new AppOnServerObserver();

        app.subscribe(observer);

        // Trigger a State Event to indicate it is now starting
        app._handleChangeEvent({type: 'appOnServer', id: 'snoop(localhost,/wlp/usr,server1)', state: 'RESTARTED'});

        assert.equal(app.state,         'STARTED',   'AppOnServer.state did not have the correct updated value');
        assert.equal(observer.newState, 'STARTED',   'AppOnServerObserver did not have the correct new value for state');
        assert.equal(observer.oldState, 'STARTED',   'AppOnServerObserver did not have the correct old value for state');
      }),

      tdd.test('AppOnServer operation - state change is ignored', function() {
        var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', apps: {up: 1, down: 0, unknown: 0, list: [{name: 'snoop', state: 'UNSET'}]}});
        var app = new AppOnServer({server: server, name: 'snoop'});
        var observer = new AppOnServerObserver();

        app.subscribe(observer);

        // Trigger a State Event to indicate it is now starting
        app._handleChangeEvent({type: 'appOnServer', id: 'snoop(localhost,/wlp/usr,server1)', state: 'STOPPED'});
        app._handleChangeEvent({type: 'appOnServer', id: 'snoop(localhost,/wlp/usr,server1)', state: 'STARTED'});

        assert.equal(app.state,         'UNSET',   'AppOnServer.state did not have the correct updated value');
        assert.notOk(observer.newState, 'AppOnServerObserver should not have a value for the new value for state');
        assert.notOk(observer.oldState, 'AppOnServerObserver should not have a value for the old value for state');
      });

      tdd.test('AppOnServer update - Server gets a STOPPED app change', function() {
        var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', apps: {up: 1, down: 0, unknown: 0, list: [{name: 'snoop', state: 'STARTED'}]}});
        var app = new AppOnServer({server: server, name: 'snoop'});
        var observer = new AppOnServerObserver();

        app.subscribe(observer);

        // Trigger a Server Event to indicate that the application is now stopped (with alerts)
        server._handleChangeEvent({type: 'server', id: 'localhost,/wlp/usr,server1', apps: {changed: [{name: 'snoop', state: 'STOPPED'}] },
          alerts: { count: 1, unknown: [], app: [ { name: 'localhost,/wlp/usr,server1,snoop', servers: ['localhost,/wlp/usr,server1'] } ] } });

        assert.equal(app.state,                         'STOPPED',   'AppOnServer.state did not have the correct updated value');
        assert.equal(app.alerts.count,                  1,           'AppOnServer.alerts.count should now be 1');
        assert.equal(app.alerts.app.length,             1,           'AppOnServer.alerts.app.length should now be length 1');
        assert.equal(app.alerts.app[0].name,            'localhost,/wlp/usr,server1,snoop', 'AppOnServer.alerts.app[0].name should now be snoop');
        assert.equal(app.alerts.app[0].servers.length,  1,           'AppOnServer.alerts.app[0].servers.length should now be length 1');
        assert.equal(app.alerts.app[0].servers[0],      'localhost,/wlp/usr,server1', 'AppOnServer.alerts.app[0].servers[0] should now be server1');
        assert.equal(observer.newState,                 'STOPPED',   'AppOnServerObserver should not have a value for the new value for state');
        assert.equal(observer.oldState,                 'STARTED',   'AppOnServerObserver should not have a value for the old value for state');
        assert.equal(observer.newAlerts.count,           1,          'AppOnServerObserver should have 1 count as the new alerts object');
        assert.equal(observer.oldAlerts.count,           0,          'AppOnServerObserver should have 0 count as the old alerts object');
      });
      
      tdd.test('AppOnServer update - Server gets a STOPPING app change', function() {
        var server = new Server({id: 'localhost,/wlp/usr,server1', wlpInstallDir: '/wlp', state: 'STARTED', apps: {up: 1, down: 0, unknown: 0, list: [{name: 'snoop', state: 'STARTED'}]}});
        var app = new AppOnServer({server: server, name: 'snoop'});
        var observer = new AppOnServerObserver();

        app.subscribe(observer);

        // Trigger a Server Event to indicate that the application is now stopped (with alerts)
        server._handleChangeEvent({type: 'server', id: 'localhost,/wlp/usr,server1', apps: {changed: [{name: 'snoop', state: 'STOPPING'}] },
          alerts: { count: 1, unknown: [], app: [ { name: 'localhost,/wlp/usr,server1,snoop', servers: ['localhost,/wlp/usr,server1'] } ] } });

        assert.equal(app.state,                         'STOPPING',  'AppOnServer.state did not have the correct updated value');
        assert.equal(app.alerts.count,                  1,           'AppOnServer.alerts.count should now be 1');
        assert.equal(app.alerts.app.length,             1,           'AppOnServer.alerts.app.length should now be length 1');
        assert.equal(app.alerts.app[0].name,            'localhost,/wlp/usr,server1,snoop', 'AppOnServer.alerts.app[0].name should now be snoop');
        assert.equal(app.alerts.app[0].servers.length,  1,           'AppOnServer.alerts.app[0].servers.length should now be length 1');
        assert.equal(app.alerts.app[0].servers[0],      'localhost,/wlp/usr,server1', 'AppOnServer.alerts.app[0].servers[0] should now be server1');
        assert.equal(observer.newState,                 'STOPPING',  'AppOnServerObserver should not have a value for the new value for state');
        assert.equal(observer.oldState,                 'STARTED',   'AppOnServerObserver should not have a value for the old value for state');
        assert.equal(observer.newAlerts.count,           1,          'AppOnServerObserver should have 1 count as the new alerts object');
        assert.equal(observer.oldAlerts.count,           0,          'AppOnServerObserver should have 0 count as the old alerts object');
      });
      
    });
  }
});
