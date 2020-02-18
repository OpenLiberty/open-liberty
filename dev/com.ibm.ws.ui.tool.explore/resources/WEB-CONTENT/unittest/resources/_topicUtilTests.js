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
define([
        "intern!tdd",
        "intern/chai!assert",
        "resources/_topicUtil"
        ],

        function (tdd, assert, topicUtil) {

  var failureMsg = 'FAIL: Did not get back the expected topic string for the given input';

  /**
   * Defines the 'Topic Util Tests' module test suite.
   */
  tdd.suite("Topic Util Tests", function() {

    tdd.afterEach(function() {
      // ?
    });

    tdd.test("getTopicByType: standaloneServer", function() {
      assert.equal(topicUtil.getTopicByType('standaloneServer'), '/explore-1.0/standaloneServer', failureMsg);
    });

    tdd.test('getTopicByType - summay', function() {
      assert.equal(topicUtil.getTopicByType('summary'), '/explore-1.0/summary', failureMsg);
    }),

    tdd.test('getTopicByType - summary', function() {
      assert.equal(topicUtil.getTopicByType('summary'), '/explore-1.0/summary', failureMsg);
    }),

    tdd.test('getTopicByType - alerts', function() {
      assert.equal(topicUtil.getTopicByType('alerts'), '/explore-1.0/alerts', failureMsg);
    }),

    tdd.test("getTopicByType: servers", function() {
      assert.equal(topicUtil.getTopicByType('servers'), '/explore-1.0/servers', failureMsg);
    });

    tdd.test("getTopicByType: serversOnHost", function() {
      assert.equal(topicUtil.getTopicByType('serversOnHost', 'myHost'), '/explore-1.0/hosts/myHost/servers', failureMsg);
    });

    tdd.test("getTopicByType: serversOnCluster", function() {
      assert.equal(topicUtil.getTopicByType('serversOnCluster', 'myCluster'), '/explore-1.0/clusters/myCluster/servers', failureMsg);
    });

    tdd.test("getTopicByType: server", function() {
      assert.equal(topicUtil.getTopicByType('server', 'myHost,/wlp/usr,myServer'), '/explore-1.0/servers/myHost#/wlp/usr#myServer', failureMsg);
    });

    tdd.test("getTopicByType: clusters", function() {
      assert.equal(topicUtil.getTopicByType('clusters'), '/explore-1.0/clusters', failureMsg);
    });

    tdd.test("getTopicByType: cluster", function() {
      assert.equal(topicUtil.getTopicByType('cluster', 'myCluster'), '/explore-1.0/clusters/myCluster', failureMsg);
    });

    tdd.test("getTopicByType: hosts", function() {
      assert.equal(topicUtil.getTopicByType('hosts'), '/explore-1.0/hosts', failureMsg);
    });

    tdd.test('getTopicByType: host', function() {
      assert.equal(topicUtil.getTopicByType('host', 'myHost'), '/explore-1.0/hosts/myHost', failureMsg);
    }),

    tdd.test("getTopicByType: applications", function() {
      assert.equal(topicUtil.getTopicByType('applications'), '/explore-1.0/applications', failureMsg);
    });

    tdd.test("getTopicByType: appsOnServer", function() {
      assert.equal(topicUtil.getTopicByType('appsOnServer', 'myHost,/wlp/usr,myServer'), '/explore-1.0/servers/myHost#/wlp/usr#myServer/apps', failureMsg);
    });

    tdd.test("getTopicByType: appOnServer", function() {
      assert.equal(topicUtil.getTopicByType('appOnServer', 'myHost,/wlp/usr,myServer', 'snoop'), '/explore-1.0/servers/myHost#/wlp/usr#myServer/apps/snoop', failureMsg);
    });

    tdd.test("getTopicByType: appsOnCluster", function() {
      assert.equal(topicUtil.getTopicByType('appsOnCluster', 'myCluster'), '/explore-1.0/clusters/myCluster/apps', failureMsg);
    });

    tdd.test("getTopicByType: appOnCluster", function() {
      assert.equal(topicUtil.getTopicByType('appOnCluster', 'myCluster', 'snoop'), '/explore-1.0/clusters/myCluster/apps/snoop', failureMsg);
    });

    tdd.test("getTopicByType: unknown type", function() {
      try {
        topicUtil.getTopicByType('unknown');
        throw 'getTopicByType() for an unknown type should have thrown an error';
      } catch(error) {
        assert.ok("'unknown' for topic type should result in error");
      }
    });

    tdd.test("getTopic: standaloneServer", function() {
      var resource = { type: 'standaloneServer', id: 'standaloneServer' };
      assert.equal(topicUtil.getTopic(resource), '/explore-1.0/standaloneServer', failureMsg);
    });

    tdd.test("getTopic: servers", function() {
      var resource = { type: 'servers', id: 'servers' };
      assert.equal(topicUtil.getTopic(resource), '/explore-1.0/servers', failureMsg);
    });

    tdd.test("getTopic: serversOnHost", function() {
      var resource = { type: 'serversOnHost', id: 'serversOnHost(myHost)', host: { id: 'myHost' } };
      assert.equal(topicUtil.getTopic(resource), '/explore-1.0/hosts/myHost/servers', failureMsg);
    });

    tdd.test("getTopic: serversOnCluster", function() {
      var resource = { type: 'serversOnCluster', id: 'serversOnCluster(myCluster)', cluster: { id: 'myCluster' } };
      assert.equal(topicUtil.getTopic(resource), '/explore-1.0/clusters/myCluster/servers', failureMsg);
    });
    tdd.test("getTopic: server", function() {
      var resource = { type: 'server', id: 'myHost,/wlp/usr,myServer' };
      assert.equal(topicUtil.getTopic(resource), '/explore-1.0/servers/myHost#/wlp/usr#myServer', failureMsg);
    });

    tdd.test("getTopic: clusters", function() {
      var resource = { type: 'clusters', id: 'clusters' };
      assert.equal(topicUtil.getTopic(resource), '/explore-1.0/clusters', failureMsg);
    });

    tdd.test("getTopic: cluster", function() {
      var resource = { type: 'serversOnCluster', id: 'serversOnCluster(myCluster)', cluster: { id: 'myCluster' } };
      assert.equal(topicUtil.getTopic(resource), '/explore-1.0/clusters/myCluster/servers', failureMsg);
    });

    tdd.test("getTopic: hosts", function() {
      var resource = { type: 'hosts', id: 'hosts' };
      assert.equal(topicUtil.getTopic(resource), '/explore-1.0/hosts', failureMsg);
    });

    tdd.test('getTopic: host', function() {
      var resource = { type: 'host', id: 'myHost' };

      assert.equal(topicUtil.getTopic(resource), '/explore-1.0/hosts/myHost',failureMsg);
    }),

    tdd.test("getTopic: applications", function() {
      var resource = { type: 'applications', id: 'applications' };
      assert.equal(topicUtil.getTopic(resource), '/explore-1.0/applications', failureMsg);
    });

    tdd.test("getTopic: appsOnServer", function() {
      var resource = { type: 'appsOnServer', id: 'appsOnServer(myHost,/wlp/usr,myServer)', server: { id: 'myHost,/wlp/usr,myServer' } };
      assert.equal(topicUtil.getTopic(resource), '/explore-1.0/servers/myHost#/wlp/usr#myServer/apps', failureMsg);
    });

    tdd.test("getTopic: appOnServer", function() {
      var resource = { type: 'appOnServer', id: 'snoop(myHost,/wlp/usr,myServer)', name: 'snoop', server: { id: 'myHost,/wlp/usr,myServer' } };
      assert.equal(topicUtil.getTopic(resource), '/explore-1.0/servers/myHost#/wlp/usr#myServer/apps/snoop', failureMsg);
    });

    tdd.test("getTopic: appsOnCluster", function() {
      var resource = { type: 'appsOnCluster', id: 'appsOnCluster(myCluster)', cluster: { id: 'myCluster' } };
      assert.equal(topicUtil.getTopic(resource), '/explore-1.0/clusters/myCluster/apps', failureMsg);
    });

    tdd.test("getTopic: appOnCluster", function() {
      var resource = { type: 'appOnCluster', id: 'snoop(myCluster)', name: 'snoop', cluster: { id: 'myCluster' } };
      assert.equal(topicUtil.getTopic(resource), '/explore-1.0/clusters/myCluster/apps/snoop', failureMsg);
    });

    tdd.test("getTopic: Unknown type", function() {
      var resource = { type: 'unknown' };

      try {
        topicUtil.getTopic(resource);
        throw 'getTopic() for an unknown resource should have thrown an error';
      } catch(error) {
        assert.ok("'unknown' for resource type should result in error");
      }
    });

  });
});