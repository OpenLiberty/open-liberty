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
 * Test cases for viewFactory
 */
define([ 
        "intern!tdd",
        "intern/chai!assert",
        "views/viewFactory"
        ], 

        function(tdd, assert, viewFactory) {

  var mockCollectionView = {
      setResource: 'not set!',
      setDefaultFilter: 'not set!',
      setPane: 'not set!',
      openCollectionView: function(resource, defaultFilter, pane) {
        this.setResource = resource;
        this.setDefaultFilter = defaultFilter;
        this.setPane = pane;
      }
  };

  var mockObjectView = {
      setResource: 'not set!',
      openObjectView: function(resource) {
        this.setResource = resource;
      }
  };

  /**
   * Defines the 'viewFactory' module test suite.
   */
  tdd.suite('View Factory Tests', function() {

    var mockServer = sinon.fakeServer.create();

    tdd.beforeEach(function() {
      viewFactory.collectionView = mockCollectionView;
      viewFactory.objectView = mockObjectView;
      mockServer = sinon.fakeServer.create();
    });

    tdd.afterEach(function() {
      mockServer.restore();
    });

    /**
     * Test calling viewFactory before its ready. This does not really do a lot
     * because we don't have a way to test console.error happened (as far as I know).
     */
    tdd.test('ViewFactory - invocation before ready ', function() {
      viewFactory.collectionView = null;
      viewFactory.objectView = null;

      viewFactory.openView('ignored');
      // Nothing happens!

      viewFactory.openView('ignored');
      // Nothing happens!
    }),

    /**
     * Test opening a view for the applications collection.
     */
    tdd.test('ViewFactory - open applications', function() {
      var resource = { type: 'applications' };
      var defaultFilter = 'Alert';
      var pane = 'parentPane';

      viewFactory.openView(resource, defaultFilter, pane);

      assert.equal(resource, mockCollectionView.setResource, 'The view was not called with the correct resource');
      assert.equal(defaultFilter, mockCollectionView.setDefaultFilter, 'The view was not called with the correct default filter');
      assert.equal(pane, mockCollectionView.setPane, 'The view was not called with the correct pane');
    }),

    /**
     * Test opening a view for the appsOnCluster collection.
     */
    tdd.test('ViewFactory - open appsOnCluster', function() {
      var resource = { type: 'appsOnCluster', cluster: 'mockCluster', name: 'appsOnCluster_ABC' };
      var defaultFilter = 'Alert';
      var pane = 'parentPane';

      viewFactory.openView(resource, defaultFilter, pane);

      assert.equal(resource, mockCollectionView.setResource, 'The view was not called with the correct resource');
      assert.equal(defaultFilter, mockCollectionView.setDefaultFilter, 'The view was not called with the correct default filter');
      assert.equal(pane, mockCollectionView.setPane, 'The view was not called with the correct pane');
    }),

    /**
     * Test opening a view for the appsOnServer collection.
     */
    tdd.test('ViewFactory - open appsOnServer', function() {
      var resource = { type: 'appsOnServer', server: 'mockServer', name: 'appsOnServerName_123' };
      var defaultFilter = 'Alert';
      var pane = 'parentPane';

      viewFactory.openView(resource, defaultFilter, pane);

      assert.equal(resource, mockCollectionView.setResource, 'The view was not called with the correct resource');
      assert.equal(defaultFilter, mockCollectionView.setDefaultFilter, 'The view was not called with the correct default filter');
      assert.equal(pane, mockCollectionView.setPane, 'The view was not called with the correct pane');
    }),

    /**
     * Test opening a view for the clusters collection.
     */
    tdd.test('ViewFactory - open clusters', function() {
      var resource = { type: 'clusters' };
      var defaultFilter = null; // Change the type for coverage
      var pane = null; // Change the type for coverage

      viewFactory.openView(resource, defaultFilter, pane);

      assert.equal(resource, mockCollectionView.setResource, 'The view was not called with the correct resource');
      assert.equal(defaultFilter, mockCollectionView.setDefaultFilter, 'The view was not called with the correct default filter');
      assert.equal(pane, mockCollectionView.setPane, 'The view was not called with the correct pane');
    }),

    /**
     * Test opening a view for the hosts collection.
     */
    tdd.test('ViewFactory - open hosts', function() {
      var resource = { type: 'hosts' };
      var defaultFilter = 'Alert';
      var pane = 'parentPane';

      viewFactory.openView(resource, defaultFilter, pane);

      assert.equal(resource, mockCollectionView.setResource, 'The view was not called with the correct resource');
      assert.equal(defaultFilter, mockCollectionView.setDefaultFilter, 'The view was not called with the correct default filter');
      assert.equal(pane, mockCollectionView.setPane, 'The view was not called with the correct pane');
    }),

    /**
     * Test opening a view for the servers collection.
     */
    tdd.test('ViewFactory - open servers', function() {
      var resource = { type: 'servers' };
      var defaultFilter = 'Alert';
      var pane = 'parentPane';

      viewFactory.openView(resource, defaultFilter, pane);

      assert.equal(resource, mockCollectionView.setResource, 'The view was not called with the correct resource');
      assert.equal(defaultFilter, mockCollectionView.setDefaultFilter, 'The view was not called with the correct default filter');
      assert.equal(pane, mockCollectionView.setPane, 'The view was not called with the correct pane');
    }),

    /**
     * Test opening a view for the serversOnCluster collection.
     */
    tdd.test('ViewFactory - open serversOnCluster', function() {
      var resource = { type: 'serversOnCluster', cluster: 'mockCluster', name: 'serversOnClusterName_ABC' };
      var defaultFilter = 'Alert';
      var pane = 'parentPane';

      viewFactory.openView(resource, defaultFilter, pane);

      assert.equal(resource, mockCollectionView.setResource, 'The view was not called with the correct resource');
      assert.equal(defaultFilter, mockCollectionView.setDefaultFilter, 'The view was not called with the correct default filter');
      assert.equal(pane, mockCollectionView.setPane, 'The view was not called with the correct pane');
    }),

    /**
     * Test opening a view for the serversOnHost collection.
     */
    tdd.test('ViewFactory - open serversOnHost', function() {
      var resource = { type: 'serversOnHost', host: 'mockHost' };
      var defaultFilter = 'Alert';
      var pane = 'parentPane';

      viewFactory.openView(resource, defaultFilter, pane);

      assert.equal(resource, mockCollectionView.setResource, 'The view was not called with the correct resource');
      assert.equal(defaultFilter, mockCollectionView.setDefaultFilter, 'The view was not called with the correct default filter');
      assert.equal(pane, mockCollectionView.setPane, 'The view was not called with the correct pane');
    }),

    /**
     * Test opening a view for the appOnCluster object.
     */
    tdd.test('ViewFactory - open appOnCluster', function() {
      var resource = { type: 'appOnCluster', cluster: 'mockCluster', name: 'appOnClusterName_123' };

      viewFactory.openView(resource);

      assert.equal(resource, mockObjectView.setResource, 'The view was not called with the correct resource');
    }),

    /**
     * Test opening a view for the appOnServer object.
     */
    tdd.test('ViewFactory - open appOnServer', function() {
      var resource = { type: 'appOnServer', server: 'mockServer', name: 'appOnServer_WCR' };

      viewFactory.openView(resource);

      assert.equal(resource, mockObjectView.setResource, 'The view was not called with the correct resource');
    }),

    /**
     * Test opening a view for the cluster object.
     */
    tdd.test('ViewFactory - open cluster', function() {
      var resource = { type: 'cluster' };

      viewFactory.openView(resource);

      assert.equal(resource, mockObjectView.setResource, 'The view was not called with the correct resource');
    }),

    /**
     * Test opening a view for the host object.
     */
    tdd.test('ViewFactory - open host', function() {
      var resource = { type: 'host' };

      viewFactory.openView(resource);

      assert.equal(resource, mockObjectView.setResource, 'The view was not called with the correct resource');
    }),

    /**
     * Test opening a view for the server object.
     */
    tdd.test('ViewFactory - open server', function() {
      var resource = { type: 'server' };

      viewFactory.openView(resource);

      assert.equal(resource, mockObjectView.setResource, 'The view was not called with the correct resource');
    }),

    /**
     * Test opening a view for the standaloneServer object.
     */
    tdd.test('ViewFactory - open standaloneServer', function() {
      var resource = { type: 'standaloneServer' };

      viewFactory.openView(resource);

      assert.equal(resource, mockObjectView.setResource, 'The view was not called with the correct resource');
    }),

    /**
     * Test opening a view for the single server object's Monitor view
     * Extra flag does nothing in a unittest; just verify that the passed-in
     * server resource is the expected one
     */
    tdd.test('ViewFactory - open server\'s monitor view', function() {
      var resource = { type: 'server' };

      viewFactory.openView(resource, "Stats");

      assert.equal(resource, mockObjectView.setResource, 'The view was not called with the correct resource');
    }),

    /**
     * Test opening a view for an unknown type. This does not really do a lot
     * because we don't have a way to test console.error happened (as far as I know).
     */
    tdd.test('ViewFactory - open unknown type', function() {
      viewFactory.collectionView = null;
      viewFactory.objectView = null;

      var resource = { type: 'unknown' };

      viewFactory.openView(resource);
      // Nothing happens!
    });
  });
});
