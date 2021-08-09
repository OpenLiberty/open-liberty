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
 * Test cases for Server side Persistence of Metrics data
 */
define([ 
        "intern!tdd",
        "intern/chai!assert",
        "dojo/json",
        "widgets/graphs/ShowGraphsDialog"
         ],
         
         function(tdd, assert, JSON, ShowGraphsDialog) {

  /**
   * Local variable in which to store created ShowGraphsDialog instances. They will be destroyed
   * after the test executes.
   */
  // The ShowGraphsDialog var
  var showGraphsDialog = null;
  // The app resource ID we're going to use
  var appResourceID = "app1(localhost,C:/libertyHome/usr/servers/mockServer,mockServer)";
  // The Server resource ID we're going to use.
  var serverResourceID = "localhost,C:/libertyHome/usr/servers/mockServer,mockServer";
  // A list of server graphs that should be displayed by default.
  var allDefaultServerGraphs = ["HeapStats", "ClassesStats", "ThreadStats", "ProcessCPUStats"];
  // A list of app graphs that should be displayed by default.
  var allDefaultAppGraphs = ["ResponseMeanServletStats", "RequestCountServletStats"];
  // A list of graphs that should be displayed by default.
  var allDefaultGraphs = allDefaultServerGraphs.concat(allDefaultAppGraphs);
  
  /*
   * This function goes through the graph data in the showGraphDialog and checks that the supplied list of graphs
   * are configured to display. We configure the defaultDisplay=true to ensure that the graphs get built and displayed.
   * 
   */
  function checkDefaultGraphs(showGraphsDialog, graphsToDisplay) {
    // Iterate over each section in the graphdata. 
    for (var section in showGraphsDialog.graphData) {
      var graphs = showGraphsDialog.graphData[section].graphs;
      // Iterate over each graph in the current section
      for (var graphCount = 0; graphCount < graphs.length; graphCount++) {
        // If we have the current graphType listed in the supplied list of graphs that should be displayed, then check that the 
        // defaultDisplay is set to true, otherwise ensure it is set to false.
        var graphDisplayed = graphs[graphCount].defaultDisplay ? true : false;
        if (graphsToDisplay.indexOf(graphs[graphCount].graphType) >= 0) { 
          assert.ok(graphDisplayed, graphs[graphCount].graphType + " should be configured to display by default and it isn't");
        } else {
          assert.notOk(graphDisplayed, graphs[graphCount].graphType + " should NOT be configured to display by default but is");
        };
      };
    };
  };
  
  /*
   * This function checks that the supplied graphs are in their required states.
   * The graphsToCheck is an array of graphs, and the displayed var is either true or false.
   * 
   */
  function checkSelectedGraphs(showGraphsDialog, graphsToCheck, section, displayed) {
    // Iterate over each section in the graphdata. 
    for (var graphCount = 0; graphCount < graphsToCheck.length; graphCount++) {
      // If we have the current graphType listed in the supplied list of graphs that should be displayed, then check that the 
      // defaultDisplay is set to true, otherwise ensure it is set to false.
      var currGraph = showGraphsDialog.__getGraph(section, graphsToCheck[graphCount]);
      var graphDisplayed = currGraph.defaultDisplay ? true : false;
      
      if (displayed) { 
        assert.ok(graphDisplayed, currGraph.graphType + " should be configured to display by default and it isn't");
      } else {
        assert.notOk(graphDisplayed, currGraph.graphType + " should NOT be configured to display by default but is");
      };
    };
  };
  /*
   * This function goes through the list of graphs and configures their displayGraph variable. This is set when the graph is built in the 
   * showGraphsDialog, but in this unittest, we don't go through this code path, so we have to manually set the values.
   * These are used when we save the config, as we only write out graphs that have displayGraph=true.
   * 
   */
  function displaySelectedGraphs(showGraphsDialog, graphsToDisplay) {
    // Iterate over each section in the graphdata. 
    for (var section in showGraphsDialog.graphData) {
      var graphs = showGraphsDialog.graphData[section].graphs;
      // Iterate over each graph in the current section
      for (var graphCount = 0; graphCount < graphs.length; graphCount++) {
        // If we have the current graphType listed in the supplied list of graphs that should be displayed, then configure
        // the displayGraph to true, otherwise set it to false.
        if (graphsToDisplay.indexOf(graphs[graphCount].graphType) >= 0) { 
          graphs[graphCount].displayGraph = true;
        } else {
          graphs[graphCount].displayGraph = false;
        };
      };
    };
  };
  
  /*
   * Check for a list of the servlets listed in the graphs data field.
   * This method accepts letters that map to GraphingServlet.com.ibm.ws.ui.graph.servlet.TestServlet_ e.g.
   * A maps to GraphingServlet.com.ibm.ws.ui.graph.servlet.TestServletA
   * 
   */
  function checkServletDataField(data, servletLetters) {
    
      assert.ok(data.length === servletLetters.length, 
          "ResponseMeanServletStats graph data field should have " + servletLetters.length + " entries and there are only " + data.length);
      
      for (var servletLetter = 0; servletLetter < servletLetters.length; servletLetters++) {
        var currServletIdent = "GraphingServlet.com.ibm.ws.ui.graph.servlet.TestServlet" + servletLetters[servletLetter];
        assert.ok(data.indexOf(currServletIdent) >= 0, "ResponseMeanServletStats graph data does not contain " + currServletIdent);
      }
  }
  
  /* This function asserts that the JVM HeapStats graph and CPUUsage graph are configured correctly. This is used by a number of the tests,
   * and saves duplicating checking.
   */ 
  function checkHeapStatsAndCPUUsageGraphs(JVMMetricsData) {
    // Check the type of JVM graph. We have to check that the 1st one we check is either HeapStats or ProcessCPU, and once we know which
    // one we can make sure the other one is definitely for the other type.
    
    var heapStatsData;
    var cpuStatsData;
    if (JVMMetricsData.graphs[0].graphType === "HeapStats") {
      heapStatsData = JVMMetricsData.graphs[0];
      cpuStatsData = JVMMetricsData.graphs[1];
    } else {
      heapStatsData = JVMMetricsData.graphs[1];
      cpuStatsData = JVMMetricsData.graphs[0];
    }
    assert.ok(heapStatsData.graphType === "HeapStats", "The graph type should have been HeapStats but was " + heapStatsData.graphType);
    assert.ok(heapStatsData.graphInstances.length === 1, "The number of graph instances for HeapStats should have been 1,  but was "
        + heapStatsData.graphInstances.length);
    assert.ok(heapStatsData.graphInstances[0].order === 2, "The graph order for HeapStats should have been 2 but was " + 
        heapStatsData.graphInstances[0].order);
    assert.ok(heapStatsData.graphInstances[0].width === 50, "The graph width for HeapStats should have been 50 but was " + 
        heapStatsData.graphInstances[0].width);
    assert.ok(heapStatsData.graphInstances[0].height === 50, "The graph height for HeapStats should have been 50 but was " + 
        heapStatsData.graphInstances[0].height);
        
    assert.ok(cpuStatsData.graphType === "ProcessCPUStats", "The graph type should have been ProcessCPUStats but was " + cpuStatsData.graphType);
    assert.ok(cpuStatsData.graphInstances.length === 1, "The number of graph instances for ProcessCPUStats should have been 1,  but was "
        + cpuStatsData.graphInstances.length);
    assert.ok(cpuStatsData.graphInstances[0].order === 1, "The graph order for ProcessCPUStats should have been 1 but was " + 
        cpuStatsData.graphInstances[0].order);
    assert.ok(cpuStatsData.graphInstances[0].width === 100, "The graph width for ProcessCPUStats should have been 100 but was " + 
        cpuStatsData.graphInstances[0].width);
    assert.ok(cpuStatsData.graphInstances[0].height === 200, "The graph height for ProcessCPUStats should have been 200 but was " + 
        cpuStatsData.graphInstances[0].height);
  }
  
  /*
   * A set of functions to build up the server JSON response for previously stored JSON, and
   * a few commonly used variables that hold basic JSON responses.
   */
  function buildMetricsJSON(resources) {
    return {"metrics": resources};
  }
  
  function buildMetricsResourceJSON(resourceId, graphs) {
    return {"resource": resourceId,
            "graphs": graphs
           };
  };
    
  function buildMetricsGraphJSON(section, type, instances) {
      return {"graphSection": section,
              "graphType": type, 
              "graphInstances": instances
             };
  };
  function buildMetricsGraphInstanceJSON(order, width, height, data) {
    var instance = {"order": order,
                    "width": width,
                    "height": height};
    if (data)
      instance.data = data;
    
    return instance;
  };
  
  
  var buildSearchAndTaggingJSON = function(searchValue) {
    return {"searchAndTagging": {"search1": searchValue}};
  };
  
  var appGraphResource = buildMetricsResourceJSON(appResourceID, [
                                buildMetricsGraphJSON("Servlet", "ResponseMeanServletStats", [
                                       buildMetricsGraphInstanceJSON(1, 100, 100, ["app1.com.ibm.ws.test.ServletA"])
                                ])
                         ]);
  
  var serverGraphResource = buildMetricsResourceJSON(serverResourceID, [
                                   buildMetricsGraphJSON("JVM", "HeapStats", [
                                          buildMetricsGraphInstanceJSON(1, 100, 100)
                                   ])
                            ]);
  
  var serverGraphJSON = buildMetricsJSON([serverGraphResource]);
  
  var appGraphJSON = buildMetricsJSON([appGraphResource]);
                                          
  var appServerGraphJSON = buildMetricsJSON([appGraphResource, serverGraphResource]);
  
  with(assert) {
    
    /**
     * Defines the 'viewFactory' module test suite.
     */
    tdd.suite('Metrics Persistence Tests', function() {
  
      var server;
      var error;

      tdd.beforeEach(function() {
        // Mock the admin center server since it is not available in a unittest
        server = sinon.fakeServer.create();
        error = "";
      });
      
        tdd.afterEach(function() {
          if (showGraphsDialog) {
            showGraphsDialog.destroy();
            showGraphsDialog = null;
             server.restore();
          }
        });
        
        /*
         * This is a very basic test that ensures that if we have no persisted file, we configure the defaults graphs to display.
         * 
         */
        tdd.test('Metrics Persistence - Check correct default graphs created', function() { 
          
          // Create a new instance of the ShowGraphsDialog for the server resource ID.
          showGraphsDialog = new ShowGraphsDialog({resource: {id: serverResourceID}});
          // Configure the Async callback.
          var dfd = this.async(1000);
          // Even though the getGraphData is called during the creation of the ShowGraphsDialog, we need to call it again
          // as this is an async process and we can't guarantee that we'll catch the one in the initial creation. 
          showGraphsDialog.__getGraphData().then(dfd.callback(function(response) {
            // Call the checking function to ensure all the required graphs have been displayed.
            checkDefaultGraphs(showGraphsDialog, allDefaultGraphs);
          }), dfd.reject.bind(dfd));
          
          // The Server HTTP Get function that returns a 404 indicating that no persistence file exists.
          server.respondWith("GET", /\/ibm\/api\/adminCenter\/v1\/tooldata\/com.ibm.websphere.appserver.adminCenter.tool.explore/, function(xhr) {
            xhr.respond(404, { "Content-Type": "application/json"}, "");
          });
          
          // Trigger the server to respond with our required JSON response.
          server.respond();
        });
        
        /*
         * This test ensures that we create the default graphs if we have a persistence file, but it doesn't contain
         * any metrics element. This is a server resource test.
         * 
         */
        tdd.test('Metrics Persistence - Check saved graphs from persisted file with no metrics', function() {
          
          // Create a new instance of the ShowGraphsDialog for the server resource ID.
          showGraphsDialog = new ShowGraphsDialog({resource: {id: serverResourceID}});
          // Configure the Async callback.
          var dfd = this.async(1000);
          // Even though the getGraphData is called during the creation of the ShowGraphsDialog, we need to call it again
          // as this is an async process and we can't guarantee that we'll catch the one in the initial creation. 
          showGraphsDialog.__getGraphData().then(dfd.callback(function(response) {
            // Call the checking function to ensure all the required graphs have been displayed.
            checkDefaultGraphs(showGraphsDialog, allDefaultGraphs);
          }), dfd.reject.bind(dfd));
          
          // The Server HTTP Get function that returns a JSON response with no metrics element.
          server.respondWith("GET", /\/ibm\/api\/adminCenter\/v1\/tooldata\/com.ibm.websphere.appserver.adminCenter.tool.explore/, function(xhr) {
            xhr.respond(200, { "Content-Type": "application/json"}, JSON.stringify(buildSearchAndTaggingJSON("abcd")));
          });
          
          // Trigger the server to respond with our required JSON response.
          server.respond();
        });
        
        /*
         * This test ensures that we create the default graphs if we have a persistence file, and it contains
         * a metrics element, but doesn't have graph data for this Server resource.
         * 
         */
        tdd.test('Metrics Persistence - Check saved Server graphs from persisted file with metrics but no matching resource', function() {
          // Create a new instance of the ShowGraphsDialog for the server resource ID.
          showGraphsDialog = new ShowGraphsDialog({resource: {id: serverResourceID}});
          // Configure the Async callback.
          var dfd = this.async(1000);
          // Even though the getGraphData is called during the creation of the ShowGraphsDialog, we need to call it again
          // as this is an async process and we can't guarantee that we'll catch the one in the initial creation. 
          showGraphsDialog.__getGraphData().then(dfd.callback(function(response) {
            // Call the checking function to ensure all the required graphs have been displayed.
            checkSelectedGraphs(showGraphsDialog, allDefaultServerGraphs, "JVM", true);
          }), dfd.reject.bind(dfd));
          
          // The Server HTTP Get function that returns a JSON response for an application metrics resource element only.
          server.respondWith("GET", /\/ibm\/api\/adminCenter\/v1\/tooldata\/com.ibm.websphere.appserver.adminCenter.tool.explore/, function(xhr) {
            xhr.respond(200, { "Content-Type": "application/json"}, JSON.stringify(appGraphJSON));
          });
          
          // Trigger the server to respond with our required JSON response.
          server.respond();
        });
        
        /*
         * This test ensures that we create the previously persisted graphs, if we have a persistence file, and it contains
         * a metrics element with graph data for this Server resource.
         * 
         */
        tdd.test('Metrics Persistence - Check saved Server graphs from persisted file with matching resource', function() {
          // Create a new instance of the ShowGraphsDialog for the server resource ID.
          showGraphsDialog = new ShowGraphsDialog({resource: {id: serverResourceID}});
          // Configure the Async callback.
          var dfd = this.async(1000);
          // Even though the getGraphData is called during the creation of the ShowGraphsDialog, we need to call it again
          // as this is an async process and we can't guarantee that we'll catch the one in the initial creation. 
          showGraphsDialog.__getGraphData().then(dfd.callback(function(response) {
            // Call the checking function to ensure that the previously saved graph has been displayed.
            checkSelectedGraphs(showGraphsDialog, ["HeapStats"], "JVM", true);
          }), dfd.reject.bind(dfd));
          
          // The Server HTTP Get function that returns a JSON response with both application and server metrics resource elements.
          server.respondWith("GET", /\/ibm\/api\/adminCenter\/v1\/tooldata\/com.ibm.websphere.appserver.adminCenter.tool.explore/, function(xhr) {
            xhr.respond(200, { "Content-Type": "application/json"}, JSON.stringify(appServerGraphJSON));
          });
          
          // Trigger the server to respond with our required JSON response.
          server.respond();
        });
        
        /*
         * This test ensures that we create the default graphs if we have a persistence file, and it contains
         * a metrics element, but doesn't have graph data for this Application resource.
         * 
         */
        tdd.test('Metrics Persistence - Check saved App graphs from persisted file with metrics but no matching resource', function() {
          // Create a new instance of the ShowGraphsDialog for the app resource ID.
          showGraphsDialog = new ShowGraphsDialog({resource: {id: appResourceID}});
          // Configure the Async callback.
          var dfd = this.async(1000);
          // Even though the getGraphData is called during the creation of the ShowGraphsDialog, we need to call it again
          // as this is an async process and we can't guarantee that we'll catch the one in the initial creation. 
          showGraphsDialog.__getGraphData().then(dfd.callback(function(response) {
            // Call the checking function to ensure all the required graphs have been displayed.
            checkSelectedGraphs(showGraphsDialog, allDefaultAppGraphs, "Servlet", true);
          }), dfd.reject.bind(dfd));
          
          // The Server HTTP Get function that returns a JSON response for a server metrics resource element only.
          server.respondWith("GET", /\/ibm\/api\/adminCenter\/v1\/tooldata\/com.ibm.websphere.appserver.adminCenter.tool.explore/, function(xhr) {
            xhr.respond(200, { "Content-Type": "application/json"}, JSON.stringify(serverGraphJSON));
          });
          
          // Trigger the server to respond with our required JSON response.
          server.respond();
        });
        
        /*
         * This test ensures that we create the previously persisted graphs, if we have a persistence file, and it contains
         * a metrics element with graph data for this application resource.
         * 
         */
        tdd.test('Metrics Persistence - Check saved App graphs from persisted file with matching resource', function() {
          // Create a new instance of the ShowGraphsDialog for the server resource ID.
          showGraphsDialog = new ShowGraphsDialog({resource: {id: appResourceID}});
          // Configure the Async callback.
          var dfd = this.async(1000);
          // Even though the getGraphData is called during the creation of the ShowGraphsDialog, we need to call it again
          // as this is an async process and we can't guarantee that we'll catch the one in the initial creation. 
          showGraphsDialog.__getGraphData().then(dfd.callback(function(response) {
            // Call the checking function to ensure all the required graphs have been displayed.
            checkSelectedGraphs(showGraphsDialog, ["ResponseMeanServletStats"], "Servlet", true);
          }), dfd.reject.bind(dfd));
          
          // The Server HTTP Get function that returns a JSON response for an application metrics resource element only.
          server.respondWith("GET", /\/ibm\/api\/adminCenter\/v1\/tooldata\/com.ibm.websphere.appserver.adminCenter.tool.explore/, function(xhr) {
            xhr.respond(200, { "Content-Type": "application/json"}, JSON.stringify(appServerGraphJSON));
          });
          
          // Trigger the server to respond with our required JSON response.
          server.respond();
        });
        
        /*
         * This test ensures that we can write out the server graphs correctly when there is no persistence file.
         * 
         */
        tdd.test('Metrics Persistence - Check saved server graphs written to new persistence file', function() { 
          // Create a new instance of the ShowGraphsDialog for the server resource ID.
          showGraphsDialog = new ShowGraphsDialog({resource: {id: serverResourceID}});
          // Configure the Async callback. We want to have 2 calls to the callback so we can check in the Server Put and in the getGraphData.
          var dfd = this.async(1000, 2);
          // Even though the getGraphData is called during the creation of the ShowGraphsDialog, we need to call it again
          // as this is an async process and we can't guarantee that we'll catch the one in the initial creation. 
          showGraphsDialog.__getGraphData().then(dfd.callback(function(response) {
            // Call the checking function to ensure all the required graphs have been displayed.
            checkDefaultGraphs(showGraphsDialog, allDefaultGraphs);
            
            // Now we're going to make a change and switch off a couple of the JVM graphs, leaving just 2 that we'll save.
            displaySelectedGraphs(showGraphsDialog, ["HeapStats", "ProcessCPUStats"]);
            
            // Now set up some specific values on the graphs that we can check in the POST.
            var cpuStatsGraph = showGraphsDialog.__getGraph("JVM", "ProcessCPUStats");
            var heapStatsGraph = showGraphsDialog.__getGraph("JVM", "HeapStats");
            
            cpuStatsGraph.order = 1;
            cpuStatsGraph.width = 100;
            cpuStatsGraph.height = 200;
            
            heapStatsGraph.order = 2;
            heapStatsGraph.width = 50;
            heapStatsGraph.height = 50;
            
            // Finally save the config.
            showGraphsDialog.saveGraphData();
          }));
            
          // The Server HTTP Get function that returns a 404 indicating that no persistence file exists.
          server.respondWith("GET", /\/ibm\/api\/adminCenter\/v1\/tooldata\/com.ibm.websphere.appserver.adminCenter.tool.explore/, function(xhr) {
            xhr.respond(404, { "Content-Type": "application/json"}, "");
          });
          
          // The Server HTTP Put function that will fail if invoked. In this scenario we should not be doing a PUT of the data.
          server.respondWith("PUT", /\/ibm\/api\/adminCenter\/v1\/tooldata\/com.ibm.websphere.appserver.adminCenter.tool.explore/, dfd.callback(function(xhr) {
            assert.ok(0, "This PUT method should never be invoked.");
          }));
          // The Server HTTP Post function that receives the JSON of the persisted data. We can use the response to check that the correct graphs have been saved.
          server.respondWith("POST", /\/ibm\/api\/adminCenter\/v1\/tooldata\/com.ibm.websphere.appserver.adminCenter.tool.explore/, dfd.callback(function(xhr) {
            
            // Convert the response to an Object.
            var persistenceData = JSON.parse(xhr.requestBody);
            
            // Do a number of asserts that indicate that the correct graphs have been identified and their values are correct.
            assert.ok(persistenceData.metrics, "No metrics element found in Post request");
            assert.ok(persistenceData.metrics.length === 1, "There should be only one resource listed in the metrics element, but there were " +
                persistenceData.metrics.length);
            assert.ok(persistenceData.metrics[0].resource === serverResourceID, "The resource ID should have been " + serverResourceID + 
                " but was " + persistenceData.metrics[0].resource);
            assert.ok(persistenceData.metrics[0].graphs.length === 2, "The number of graphs in the persistedData should have been 2 but was " + 
                persistenceData.metrics[0].graphs.length);
            
            assert.ok(persistenceData.metrics[0].graphs[0].graphSection === "JVM", "The graph section should have been JVM but was " + 
                persistenceData.metrics[0].graphs[0].graphSection);
            
            
            // Call the helper function that asserts the JVM graphs have been successfully written.
            checkHeapStatsAndCPUUsageGraphs(persistenceData.metrics[0]);
            
            xhr.respond(200, { "Content-Type": "application/json"}, "");
          }));
          
          // Trigger the server to respond with our required JSON response.
          server.respond();
        });
        
        /*
         * This test ensures that we can write out the server graphs correctly when there is an existing persistence file, but with no metrics data.
         * 
         */
        tdd.test('Metrics Persistence - Check saved server graphs written to existing persistence file', function() {
          // Create a new instance of the ShowGraphsDialog for the server resource ID.
          showGraphsDialog = new ShowGraphsDialog({resource: {id: serverResourceID}});
          // Configure the Async callback. We want to have 2 calls to the callback so we can check in the Server Put and in the getGraphData.
          var dfd = this.async(1000, 2);
          // Even though the getGraphData is called during the creation of the ShowGraphsDialog, we need to call it again
          // as this is an async process and we can't guarantee that we'll catch the one in the initial creation. 
          showGraphsDialog.__getGraphData().then(dfd.callback(function(response) {
            // Just check that the default graphs have been loaded.
            checkDefaultGraphs(showGraphsDialog, allDefaultGraphs);
            // Now we're going to make a change and switch off a couple of the JVM graphs, leaving just 2 that we'll save.
            displaySelectedGraphs(showGraphsDialog, ["HeapStats", "ProcessCPUStats"]);
            
            // Now set up some specific values on the graphs that we can check in the POST.
            var cpuStatsGraph = showGraphsDialog.__getGraph("JVM", "ProcessCPUStats");
            var heapStatsGraph = showGraphsDialog.__getGraph("JVM", "HeapStats");
            
            cpuStatsGraph.order = 1;
            cpuStatsGraph.width = 100;
            cpuStatsGraph.height = 200;
            
            heapStatsGraph.order = 2;
            heapStatsGraph.width = 50;
            heapStatsGraph.height = 50;
            
            // Finally save the config.
            showGraphsDialog.saveGraphData();
          }));
          
          // The Server HTTP Get function that returns a persistence file but with no metrics.
          server.respondWith("GET", /\/ibm\/api\/adminCenter\/v1\/tooldata\/com.ibm.websphere.appserver.adminCenter.tool.explore/, function(xhr) {
            xhr.respond(200, { "Content-Type": "application/json"}, JSON.stringify(buildSearchAndTaggingJSON("abcd")));
          });
          
          // The Server HTTP Post function that will fail if invoked. In this scenario we should not be doing a POST of the data.
          server.respondWith("POST", /\/ibm\/api\/adminCenter\/v1\/tooldata\/com.ibm.websphere.appserver.adminCenter.tool.explore/, dfd.callback(function(xhr) {
            assert.ok(0, "This POST method should never be invoked.");
          }));
          
          // The Server HTTP Put function that receives the JSON of the persisted data. We can use the response to check that the correct graphs have been saved.
          server.respondWith("PUT", /\/ibm\/api\/adminCenter\/v1\/tooldata\/com.ibm.websphere.appserver.adminCenter.tool.explore/, dfd.callback(function(xhr) {
            
            var persistenceData = JSON.parse(xhr.requestBody);
            // Ensure that the original search data in the persistence file still exists, as well as the metrics data.
            assert.ok(persistenceData.searchAndTagging, "No searchAndTagging element found in Post request");
            
            assert.ok(persistenceData.searchAndTagging.search1 === "abcd", "No search1 field with value abcd found in persistence data:" + persistenceData);
            
            assert.ok(persistenceData.metrics, "No metrics element found in Post request");
            assert.ok(persistenceData.metrics.length === 1, "There should be only one resource listed in the metrics element, but there were " +
                persistenceData.metrics.length);
            assert.ok(persistenceData.metrics[0].resource === serverResourceID, "The resource ID should have been " + serverResourceID + 
                " but was " + persistenceData.metrics[0].resource);
            assert.ok(persistenceData.metrics[0].graphs.length === 2, "The number of graphs in the persistedData should have been 2 but was " + 
                persistenceData.metrics[0].graphs.length);
            
            assert.ok(persistenceData.metrics[0].graphs[0].graphSection === "JVM", "The graph section should have been JVM but was " + 
                persistenceData.metrics[0].graphs[0].graphSection);
            
            // Call the helper function that asserts the JVM graphs have been successfully written.
            checkHeapStatsAndCPUUsageGraphs(persistenceData.metrics[0]);
            
            xhr.respond(200, { "Content-Type": "application/json"}, "");
          }));
          
          // Trigger the server to respond with our required JSON response.
          server.respond();
        });
        
        /*
         * This test ensures that we can write out the server graphs correctly when there is an existing persistence file, with metrics data, but not for this resource.
         * 
         */
        tdd.test('Metrics Persistence - Check saved server graphs written to existing persistence file with other resource', function() {
          
          // Create a new instance of the ShowGraphsDialog for the server resource ID.
          showGraphsDialog = new ShowGraphsDialog({resource: {id: serverResourceID}});
          // Configure the Async callback. We want to have 2 calls to the callback so we can check in the Server Put and in the getGraphData.
          var dfd = this.async(1000, 2);
          // Even though the getGraphData is called during the creation of the ShowGraphsDialog, we need to call it again
          // as this is an async process and we can't guarantee that we'll catch the one in the initial creation. 
          showGraphsDialog.__getGraphData().then(dfd.callback(function(response) {
            // Just check that the default graphs have been loaded.
            checkSelectedGraphs(showGraphsDialog, allDefaultServerGraphs, "JVM", true);
            // Make sure that we switch off some of the JVM stats graphs and we need to check that we store these correctly.
            displaySelectedGraphs(showGraphsDialog, ["HeapStats", "ProcessCPUStats"]);
            
            // Now set up some specific values on the graphs that we can check in the POST.
            var cpuStatsGraph = showGraphsDialog.__getGraph("JVM", "ProcessCPUStats");
            var heapStatsGraph = showGraphsDialog.__getGraph("JVM", "HeapStats");
            
            cpuStatsGraph.order = 1;
            cpuStatsGraph.width = 100;
            cpuStatsGraph.height = 200;
            
            heapStatsGraph.order = 2;
            heapStatsGraph.width = 50;
            heapStatsGraph.height = 50;
            
            showGraphsDialog.saveGraphData();
          }));
          
          // The Server HTTP Get function that returns a persistence file but with just app resource metrics.
          server.respondWith("GET", /\/ibm\/api\/adminCenter\/v1\/tooldata\/com.ibm.websphere.appserver.adminCenter.tool.explore/, function(xhr) {
            xhr.respond(200, { "Content-Type": "application/json"}, JSON.stringify(appGraphJSON));
          });
          
          // The Server HTTP Post function that will fail if invoked. In this scenario we should not be doing a POST of the data.
          server.respondWith("POST", /\/ibm\/api\/adminCenter\/v1\/tooldata\/com.ibm.websphere.appserver.adminCenter.tool.explore/, dfd.callback(function(xhr) {
            assert.ok(0, "This POST method should never be invoked.");
          }));
          
          // The Server HTTP Put function that receives the JSON of the persisted data. We can use the response to check that the correct graphs have been saved.
          server.respondWith("PUT", /\/ibm\/api\/adminCenter\/v1\/tooldata\/com.ibm.websphere.appserver.adminCenter.tool.explore/, dfd.callback(function(xhr) {
            
            var persistenceData = JSON.parse(xhr.requestBody);
            
            assert.ok(persistenceData.metrics, "No metrics element found in Post request");
            assert.ok(persistenceData.metrics.length === 2, "There should be 2 resources listed in the metrics element, but there were " +
                persistenceData.metrics.length);
            
            var appResource;
            var serverResource;
            if (persistenceData.metrics[0].resource === serverResourceID) {
              serverResource = persistenceData.metrics[0];
              appResource = persistenceData.metrics[1];
            } else {
              serverResource = persistenceData.metrics[1];
              appResource = persistenceData.metrics[0];
            }
            
            assert.ok(serverResource.resource === serverResourceID, "The resource ID should have been " + serverResourceID + 
                " but was " + serverResource.resource);
            assert.ok(appResource.resource === appResourceID, "The resource ID should have been " + appResourceID + 
                " but was " + appResource.resource);
            
            assert.ok(serverResource.graphs.length === 2, "The number of graphs in the server resource should have been 2 but was " + 
                serverResource.graphs.length);
            assert.ok(appResource.graphs.length === 1, "The number of graphs in the app resource should have been 1 but was " + 
                appResource.graphs.length);
            
            assert.ok(serverResource.graphs[0].graphSection === "JVM", "The graph section should have been JVM but was " + 
                serverResource.graphs[0].graphSection);
            assert.ok(appResource.graphs[0].graphSection === "Servlet", "The graph section should have been Servlet but was " + 
                appResource.graphs[0].graphSection);
            
            // Call the helper function that asserts the JVM graphs have been successfully written.
            checkHeapStatsAndCPUUsageGraphs(serverResource);
            
            // Check the type of Servlet graph. We have to check that the 1st one we check is either HeapStats or ProcessCPU, and once we know which
            // one we can make sure the other one is definitely for the other type.
            assert.ok(appResource.graphs[0].graphType === "ResponseMeanServletStats", "The graph type should have been ResponseMeanServletStats but was " 
                + appResource.graphs[0].graphType);
            assert.ok(appResource.graphs[0].graphInstances.length === 1, "The number of graph instances for Servlet should have been 1,  but was "
                + appResource.graphs[0].graphInstances.length);
                
            assert.ok(appResource.graphs[0].graphInstances[0].order === 1, "The graph order for Servlet should have been 1 but was " + 
                appResource.graphs[0].graphInstances[0].order);
            assert.ok(appResource.graphs[0].graphInstances[0].width === 100, "The graph width for Servlet should have been 100 but was " + 
                appResource.graphs[0].graphInstances[0].width);
            assert.ok(appResource.graphs[0].graphInstances[0].height === 100, "The graph height for Servlet should have been 100 but was " + 
                appResource.graphs[0].graphInstances[0].height);
            assert.ok(appResource.graphs[0].graphInstances[0].data.length === 1, "The number of data elements for Servlet should have been 1,  but was "
                + appResource.graphs[0].graphInstances[0].data.length);
            assert.ok(appResource.graphs[0].graphInstances[0].data[0] === "app1.com.ibm.ws.test.ServletA", 
                "The graph data should have been app1.com.ibm.ws.test.ServletA but was " + appResource.graphs[0].graphInstances[0].data[0]);
            
            xhr.respond(200, { "Content-Type": "application/json"}, "");
          }));
          
          // Trigger the server to respond with our required JSON response.
          server.respond();
        });
        
        /*
         * This test ensures that we can write out the server graphs correctly when there is an existing persistence file, with metrics data, and saved config 
         * for this resource.
         * 
         */
        tdd.test('Metrics Persistence - Check saved server graphs written to existing persistence file with existing resource', function() {
          
          // Create a new instance of the ShowGraphsDialog for the server resource ID.
          showGraphsDialog = new ShowGraphsDialog({resource: {id: serverResourceID}});
          // Configure the Async callback. We want to have 2 calls to the callback so we can check in the Server Put and in the getGraphData.
          var dfd = this.async(1000, 2);
          
          // Even though the getGraphData is called during the creation of the ShowGraphsDialog, we need to call it again
          // as this is an async process and we can't guarantee that we'll catch the one in the initial creation. 
          showGraphsDialog.__getGraphData().then(dfd.callback(function(response) {
            
            // Check that the graphs from the persisted file have been configured to display.
            checkSelectedGraphs(showGraphsDialog, ["HeapStats", "ProcessCPUStats"], "JVM", true);
            // Check that the other JVM graphs from are not configured to display.
            checkSelectedGraphs(showGraphsDialog, ["ThreadStats", "ClassesStats"], "JVM", false);
            // Make sure that we switch off the selected JVM stats graphs and select different ones, and then we need to check that we store 
            // these correctly.
            displaySelectedGraphs(showGraphsDialog, ["ThreadStats", "ClassesStats"]);
            
            // Now set up some specific values on the graphs that we can check in the POST.
            var threadStatsGraph = showGraphsDialog.__getGraph("JVM", "ThreadStats");
            var classesStatsGraph = showGraphsDialog.__getGraph("JVM", "ClassesStats");
            
            threadStatsGraph.order = 3;
            threadStatsGraph.width = 300;
            threadStatsGraph.height = 300;
            
            classesStatsGraph.order = 6;
            classesStatsGraph.width = 80;
            classesStatsGraph.height = 80;
            
            showGraphsDialog.saveGraphData();
          }));
          
          // The Server HTTP Get function that returns a persistence file but with just server resource metrics.
          server.respondWith("GET", /\/ibm\/api\/adminCenter\/v1\/tooldata\/com.ibm.websphere.appserver.adminCenter.tool.explore/, function(xhr) {
            var graphJSON = buildMetricsJSON([
                                   buildMetricsResourceJSON(serverResourceID, [
                                          buildMetricsGraphJSON("JVM", "HeapStats", [
                                                 buildMetricsGraphInstanceJSON(1, 100, 100)
                                          ]),
                                          buildMetricsGraphJSON("JVM", "ProcessCPUStats", [
                                                 buildMetricsGraphInstanceJSON(2, 100, 100)
                                          ])
                                   ])
                            ]);
            xhr.respond(200, { "Content-Type": "application/json"}, JSON.stringify(graphJSON));
          });
          
          // The Server HTTP Post function that will fail if invoked. In this scenario we should not be doing a POST of the data.
          server.respondWith("POST", /\/ibm\/api\/adminCenter\/v1\/tooldata\/com.ibm.websphere.appserver.adminCenter.tool.explore/, dfd.callback(function(xhr) {
            assert.ok(0, "This POST method should never be invoked.");
          }));
          
          // The Server HTTP Put function that receives the JSON of the persisted data. We can use the response to check that the correct graphs have been saved.
          server.respondWith("PUT", /\/ibm\/api\/adminCenter\/v1\/tooldata\/com.ibm.websphere.appserver.adminCenter.tool.explore/, dfd.callback(function(xhr) {
            
            var persistenceData = JSON.parse(xhr.requestBody);
            assert.ok(persistenceData.metrics, "No metrics element found in Post request");
            assert.ok(persistenceData.metrics.length === 1, "There should be only one resource listed in the metrics element, but there were " +
                persistenceData.metrics.length);
            assert.ok(persistenceData.metrics[0].resource === serverResourceID, "The resource ID should have been " + serverResourceID + 
                " but was " + persistenceData.metrics[0].resource);
            assert.ok(persistenceData.metrics[0].graphs.length === 2, "The number of graphs in the persistedData should have been 2 but was " + 
                persistenceData.metrics[0].graphs.length);
            
            assert.ok(persistenceData.metrics[0].graphs[0].graphSection === "JVM", "The graph section should have been JVM but was " + 
                persistenceData.metrics[0].graphs[0].graphSection);
            // Check the type of JVM graph. We have to check that the 1st one we check is either HeapStats or ProcessCPU, and once we know which
            // one we can make sure the other one is definitely for the other type.
            
            var threadStatsData;
            var classesStatsData;
            if (persistenceData.metrics[0].graphs[0].graphType === "ThreadStats") {
              threadStatsData = persistenceData.metrics[0].graphs[0];
              classesStatsData = persistenceData.metrics[0].graphs[1];
            } else {
              threadStatsData = persistenceData.metrics[0].graphs[1];
              classesStatsData = persistenceData.metrics[0].graphs[0];
            }
            assert.ok(threadStatsData.graphType === "ThreadStats", "The graph type should have been ThreadStats but was " + threadStatsData.graphType);
            assert.ok(threadStatsData.graphInstances.length === 1, "The number of graph instances for ThreadStats should have been 1,  but was "
                + threadStatsData.graphInstances.length);
            assert.ok(threadStatsData.graphInstances[0].order === 3, "The graph order for ThreadStats should have been 3 but was " + 
                threadStatsData.graphInstances[0].order);
            assert.ok(threadStatsData.graphInstances[0].width === 300, "The graph width for ThreadStats should have been 300 but was " + 
                threadStatsData.graphInstances[0].width);
            assert.ok(threadStatsData.graphInstances[0].height === 300, "The graph height for ThreadStats should have been 300 but was " + 
                threadStatsData.graphInstances[0].height);
                
            assert.ok(classesStatsData.graphType === "ClassesStats", "The graph type should have been ClassesStats but was " + classesStatsData.graphType);
            assert.ok(classesStatsData.graphInstances.length === 1, "The number of graph instances for ClassesStats should have been 1,  but was "
                + classesStatsData.graphInstances.length);
            assert.ok(classesStatsData.graphInstances[0].order === 6, "The graph order for ClassesStats should have been 6 but was " + 
                classesStatsData.graphInstances[0].order);
            assert.ok(classesStatsData.graphInstances[0].width === 80, "The graph width for ClassesStats should have been 80 but was " + 
                classesStatsData.graphInstances[0].width);
            assert.ok(classesStatsData.graphInstances[0].height === 80, "The graph height for ClassesStats should have been 80 but was " + 
                classesStatsData.graphInstances[0].height);
            
            xhr.respond(200, { "Content-Type": "application/json"}, "");
          }));
          
          // Trigger the server to respond with our required JSON response.
          server.respond();
        });
        
        /*
         * This test ensures that we can cope with a persistence file being written after we have attempted to read it but found it isn't there.
         * So the flow is:
         * 
         *  1) ShowGraphsDialog attempts to read the file, but it doesn't exist. We use default graphs
         *  2) In the meantime another process writes the persistence file.
         *  3) The user attempts to save the graphs, and we try and post the data. But we find that the file now exists, and so re-read it
         *     merge our data and then put the merged data.
         * 
         */
        tdd.test('Metrics Persistence - Check saved graphs from no persisted file at Get, but file exists at write', function() { 
          var dataRead = false;  
          var post_HTTP_409 = false;
          
          // Create a new instance of the ShowGraphsDialog for the server resource ID.
          showGraphsDialog = new ShowGraphsDialog({resource: {id: serverResourceID}});
          // Configure the Async callback. We want to have 2 calls to the callback so we can check in the Server Put and in the getGraphData.
          var dfd = this.async(1000, 2);
          // Even though the getGraphData is called during the creation of the ShowGraphsDialog, we need to call it again
          // as this is an async process and we can't guarantee that we'll catch the one in the initial creation. 
          showGraphsDialog.__getGraphData().then(dfd.callback(function(response) {
            
            // Just check that the default graphs have been loaded.
            checkDefaultGraphs(showGraphsDialog, allDefaultGraphs);
            
            dataRead = true;
            // Make sure that we switch off some of the JVM stats graphs and we need to check that we store these correctly.
            displaySelectedGraphs(showGraphsDialog, ["HeapStats", "ProcessCPUStats"]);
            
            // Now set up some specific values on the graphs that we can check in the POST.
            var cpuStatsGraph = showGraphsDialog.__getGraph("JVM", "ProcessCPUStats");
            var heapStatsGraph = showGraphsDialog.__getGraph("JVM", "HeapStats");
            
            cpuStatsGraph.order = 1;
            cpuStatsGraph.width = 100;
            cpuStatsGraph.height = 200;
            
            heapStatsGraph.order = 2;
            heapStatsGraph.width = 50;
            heapStatsGraph.height = 50;
            
            showGraphsDialog.saveGraphData();
          }));
            
          // The Server HTTP Get function that returns a 404 initially when the data is read, and when the read variable is set, will return data, mimicing another process
          // writing out the file after we have attempted to read the file.
          server.respondWith("GET", /\/ibm\/api\/adminCenter\/v1\/tooldata\/com.ibm.websphere.appserver.adminCenter.tool.explore/, function(xhr) {
            
            if (dataRead) {
              xhr.respond(200, { "Content-Type": "application/json"}, JSON.stringify(buildSearchAndTaggingJSON("abcd")));
            } else {
              xhr.respond(404, { "Content-Type": "application/json"}, "");
            } 
          });
          
          // The Server HTTP Post function that will return an HTTP 409 indicating that a file exists, and the data can not be saved
          server.respondWith("POST", /\/ibm\/api\/adminCenter\/v1\/tooldata\/com.ibm.websphere.appserver.adminCenter.tool.explore/, function(xhr) {
            post_HTTP_409 = true;
            xhr.respond(409, { "Content-Type": "application/json"}, "");
          });
                    
          // The Server HTTP Get function that check that we've hit an HTTP 409 from the post, and then confirms that the data passed through is correct,
          // including checking that the data that was saved after the initial get is still contained in the persisted data.
          server.respondWith("PUT", /\/ibm\/api\/adminCenter\/v1\/tooldata\/com.ibm.websphere.appserver.adminCenter.tool.explore/, dfd.callback(function(xhr) {
            // Ensure that we have attempted to do a POST, and that we received a 409.
            assert.ok(post_HTTP_409, "We didn't attempt a Post which should have returned HTTP 409");
            
            var persistenceData = JSON.parse(xhr.requestBody);
            assert.ok(persistenceData.searchAndTagging, "No searchAndTagging element found in Post request");
            
            assert.ok(persistenceData.searchAndTagging.search1 === "abcd", "No search1 field with value abcd found in persistence data:" + persistenceData);
            
            assert.ok(persistenceData.metrics, "No metrics element found in Post request");
            assert.ok(persistenceData.metrics.length === 1, "There should be only one resource listed in the metrics element, but there were " +
                persistenceData.metrics.length);
            assert.ok(persistenceData.metrics[0].resource === serverResourceID, "The resource ID should have been " + serverResourceID + 
                " but was " + persistenceData.metrics[0].resource);
            assert.ok(persistenceData.metrics[0].graphs.length === 2, "The number of graphs in the persistedData should have been 2 but was " + 
                persistenceData.metrics[0].graphs.length);
            
            assert.ok(persistenceData.metrics[0].graphs[0].graphSection === "JVM", "The graph section should have been JVM but was " + 
                persistenceData.metrics[0].graphs[0].graphSection);

            // Call the helper function that asserts the JVM graphs have been successfully written.
            checkHeapStatsAndCPUUsageGraphs(persistenceData.metrics[0]);
                
            xhr.respond(200, { "Content-Type": "application/json"}, "");
          }));
          
          server.respond();
        });
        
        tdd.test('Metrics Persistence - Check saved graphs from persisted file at Get, but data stale at write', function() {
          var dataRead = false;  
          var put_HTTP_412 = false;
          // Create a new instance of the ShowGraphsDialog for the server resource ID.
          showGraphsDialog = new ShowGraphsDialog({resource: {id: serverResourceID}});
          // Configure the Async callback. We want to have 2 calls to the callback so we can check in the Server Put and in the getGraphData.
          var dfd = this.async(1000, 2);
          // Even though the getGraphData is called during the creation of the ShowGraphsDialog, we need to call it again
          // as this is an async process and we can't guarantee that we'll catch the one in the initial creation. 
          showGraphsDialog.__getGraphData().then(dfd.callback(function(response) {
            
            // Just check that the default graphs have been loaded.
            checkDefaultGraphs(showGraphsDialog, allDefaultGraphs);
                        
            dataRead = true;
            // Select HeapStats, and then we need to check that we store these correctly.
            displaySelectedGraphs(showGraphsDialog, ["HeapStats"]);
            
            // Now set up some specific values on the graphs that we can check in the POST.
            var heapStatsGraph = showGraphsDialog.__getGraph("JVM", "HeapStats");
            
            heapStatsGraph.order = 1;
            heapStatsGraph.width = 100;
            heapStatsGraph.height = 100;
            
            showGraphsDialog.saveGraphData();
          }));
            
          // The Server HTTP Get function that returns initial persisted data, which is changed when the Graphs are saved, causing the 
          // HTTP 412 stale data.
          server.respondWith("GET", /\/ibm\/api\/adminCenter\/v1\/tooldata\/com.ibm.websphere.appserver.adminCenter.tool.explore/, function(xhr) {
            if (! dataRead) {
              xhr.respond(200, { "Content-Type": "application/json"}, JSON.stringify(buildSearchAndTaggingJSON("abcd")));
            } else {
              xhr.respond(200, { "Content-Type": "application/json"}, JSON.stringify(buildSearchAndTaggingJSON("zyxw")));
            } 
          });
          
          // The Server HTTP Post function that will fail if invoked. In this scenario we should not be doing a POST of the data.
          server.respondWith("POST", /\/ibm\/api\/adminCenter\/v1\/tooldata\/com.ibm.websphere.appserver.adminCenter.tool.explore/, dfd.callback(function(xhr) {
            assert.ok(0, "This POST method should never be invoked.");
          }));
          
          // The Server HTTP Put function that intially throws an HTTP 412 which indicates that we have stale data, and then on the 2nd
          // invocation, confirms that the data passed through is correct.
          server.respondWith("PUT", /\/ibm\/api\/adminCenter\/v1\/tooldata\/com.ibm.websphere.appserver.adminCenter.tool.explore/, function(xhr) {
            if (put_HTTP_412 === false) {
              put_HTTP_412 = true;
              xhr.respond(412, { "Content-Type": "application/json"}, "");
            } else {
              // We have to create the async callback here because we have 2 paths into the put. We also have to invoke the function immediately
              // via the function (){}(); format, in order to trigger the checks.
              dfd.callback(function() {
            	
                assert.ok(put_HTTP_412, "We didn't attempt an initial put which should have returned HTTP 412");
                
                var persistenceData = JSON.parse(xhr.requestBody);
                assert.ok(persistenceData.searchAndTagging, "No searchAndTagging element found in Post request");
                
                assert.ok(persistenceData.searchAndTagging.search1 === "zyxw", "No search1 field with value zyxw found in persistence data:" + persistenceData);
                
                assert.ok(persistenceData.metrics, "No metrics element found in Post request");
                assert.ok(persistenceData.metrics.length === 1, "There should be only one resource listed in the metrics element, but there were " +
                    persistenceData.metrics.length);
                assert.ok(persistenceData.metrics[0].resource === serverResourceID, "The resource ID should have been " + serverResourceID + 
                    " but was " + persistenceData.metrics[0].resource);
                assert.ok(persistenceData.metrics[0].graphs.length === 1, "The number of graphs in the persistedData should have been 1 but was " + 
                    persistenceData.metrics[0].graphs.length);
                
                assert.ok(persistenceData.metrics[0].graphs[0].graphSection === "JVM", "The graph section should have been JVM but was " + 
                    persistenceData.metrics[0].graphs[0].graphSection);
                // Check the type of JVM graph. We have to check that the 1st one we check is either HeapStats or ProcessCPU, and once we know which
                // one we can make sure the other one is definitely for the other type.
                assert.ok(persistenceData.metrics[0].graphs[0].graphType === "HeapStats", "The graph type should have been HeapStats but was " + persistenceData.metrics[0].graphs[0].graphType);
                assert.ok(persistenceData.metrics[0].graphs[0].graphInstances.length === 1, "The number of graph instances for HeapStats should have been 1,  but was "
                    + persistenceData.metrics[0].graphs[0].graphInstances.length);
              })();
                  
              xhr.respond(200, { "Content-Type": "application/json"}, "");
            }
          });
          
          server.respond();
        });
        
        /*
         * This test ensures that a large complicated persistence file can be loaded, altered and saved.
         * 
         * 
         */
        tdd.test('Metrics Persistence - Test Multi Graph, multi-instance save and re-read.', function() {
          // Create a new instance of the ShowGraphsDialog for the server resource ID.
          showGraphsDialog = new ShowGraphsDialog({resource: {id: appResourceID}});
          // Configure the Async callback. We want to have 2 calls to the callback so we can check in the Server Put and in the getGraphData.
          var dfd = this.async(1000, 2);
          // Even though the getGraphData is called during the creation of the ShowGraphsDialog, we need to call it again
          // as this is an async process and we can't guarantee that we'll catch the one in the initial creation. 
          showGraphsDialog.__getGraphData().then(dfd.callback(function(response) {
            
            // Just check that the selected graphs have been loaded.
            checkDefaultGraphs(showGraphsDialog, ["ResponseMeanServletStats", "RequestCountServletStats"]);
            
            var responseStatsGraph = showGraphsDialog.__getGraph("Servlet", "ResponseMeanServletStats");
            var requestStatsGraph = showGraphsDialog.__getGraph("Servlet", "RequestCountServletStats");
            
            assert.ok(responseStatsGraph.graphInstances.length === 2, "There should be 2 instances of ResponseMeanServletStats graph, " +
                "but there are " + responseStatsGraph.graphInstances.length);
            if (responseStatsGraph.graphInstances[0].order === 1) {
              checkServletDataField(responseStatsGraph.graphInstances[0].data, ["A", "B", "C", "D", "E", "F"]);
              assert.ok(responseStatsGraph.graphInstances[0].width === 100, "ResponseMeanServletStats graph width is not 100, but is " + responseStatsGraph.graphInstances[0].width);
            } else {
              checkServletDataField(responseStatsGraph.graphInstances[1].data, ["A", "B"]);
              assert.ok(responseStatsGraph.graphInstances[1].width === 100, "ResponseMeanServletStats graph width is not 100, but is " + responseStatsGraph.graphInstances[1].width);
            }
            
            assert.ok(requestStatsGraph.graphInstances.length === 1, "There should be 1 instances of RequestMeanServletStats graph, " +
                "but there are " + requestStatsGraph.graphInstances.length);
            if (requestStatsGraph.graphInstances[0].order === 2) {
              checkServletDataField(requestStatsGraph.graphInstances[0].data, ["A", "B", "C", "D", "E", "F"]);
              assert.ok(requestStatsGraph.graphInstances[0].width === 200, "RequestCountServletStats graph width is not 200, but is " + requestStatsGraph.graphInstances[0].width);
            }
            
            // Remove the responseStatsGraph instance
            responseStatsGraph.graphInstances = new Array();
            
            // Add new requestStatsGraph instance
            requestStatsGraph.graphInstances.push({instance: 3, order: 25, width: 50, height: 50, data:["GraphingServlet.com.ibm.ws.ui.graph.servlet.TestServletE"]});
            requestStatsGraph.graphInstancesIndex++;
            
            showGraphsDialog.saveGraphData();
          }));
            
          // The Server HTTP Get function that builds up the initial persisted data.
          server.respondWith("GET", /\/ibm\/api\/adminCenter\/v1\/tooldata\/com.ibm.websphere.appserver.adminCenter.tool.explore/, function(xhr) {
            var existingData = buildSearchAndTaggingJSON("abcd");
            var graphJSON = buildMetricsJSON([buildMetricsResourceJSON(serverResourceID, [
                                                     buildMetricsGraphJSON("JVM", "HeapStats", [
                                                            buildMetricsGraphInstanceJSON(1, 100, 100)
                                                     ]),
                                                     buildMetricsGraphJSON("JVM", "ClassesStats", [
                                                            buildMetricsGraphInstanceJSON(2, 200, 200)
                                                     ]),
                                                     buildMetricsGraphJSON("JVM", "ProcessCPUStats", [
                                                            buildMetricsGraphInstanceJSON(3, 300, 300)
                                                     ])
                                             ]), buildMetricsResourceJSON(appResourceID, [
                                                       buildMetricsGraphJSON("Servlet", "ResponseMeanServletStats", [
                                                              buildMetricsGraphInstanceJSON(1, 100, 100, ["GraphingServlet.com.ibm.ws.ui.graph.servlet.TestServletD","GraphingServlet.com.ibm.ws.ui.graph.servlet.TestServletA","GraphingServlet.com.ibm.ws.ui.graph.servlet.TestServletF","GraphingServlet.com.ibm.ws.ui.graph.servlet.TestServletB","GraphingServlet.com.ibm.ws.ui.graph.servlet.TestServletE","GraphingServlet.com.ibm.ws.ui.graph.servlet.TestServletC"]),
                                                              buildMetricsGraphInstanceJSON(2, 200, 200, ["GraphingServlet.com.ibm.ws.ui.graph.servlet.TestServletA","GraphingServlet.com.ibm.ws.ui.graph.servlet.TestServletB"])
                                                       ]),
                                                       buildMetricsGraphJSON("Servlet", "RequestCountServletStats", [
                                                              buildMetricsGraphInstanceJSON(3, 300, 300, ["GraphingServlet.com.ibm.ws.ui.graph.servlet.TestServletD","GraphingServlet.com.ibm.ws.ui.graph.servlet.TestServletA","GraphingServlet.com.ibm.ws.ui.graph.servlet.TestServletF","GraphingServlet.com.ibm.ws.ui.graph.servlet.TestServletB","GraphingServlet.com.ibm.ws.ui.graph.servlet.TestServletE","GraphingServlet.com.ibm.ws.ui.graph.servlet.TestServletC"])       
                                                       ])
                                              ])
                              ]);
            existingData.metrics = graphJSON.metrics;
            
            xhr.respond(200, { "Content-Type": "application/json"}, JSON.stringify(existingData));
          });
          
          // The Server HTTP Post function that will fail if invoked. In this scenario we should not be doing a POST of the data.
          server.respondWith("POST", /\/ibm\/api\/adminCenter\/v1\/tooldata\/com.ibm.websphere.appserver.adminCenter.tool.explore/, dfd.callback(function(xhr) {
            assert.ok(0, "This POST method should never be invoked.");
          }));
          
          // The Server HTTP Put function that validates the updated data.
          server.respondWith("PUT", /\/ibm\/api\/adminCenter\/v1\/tooldata\/com.ibm.websphere.appserver.adminCenter.tool.explore/,  dfd.callback(function(xhr) {
            
            var persistenceData = JSON.parse(xhr.requestBody);
            assert.ok(persistenceData.searchAndTagging, "No searchAndTagging element found in Post request");
            
            assert.ok(persistenceData.searchAndTagging.search1 === "abcd", "No search1 field with value abcd found in persistence data:" + persistenceData);
            
            assert.ok(persistenceData.metrics, "No metrics element found in Post request");
            assert.ok(persistenceData.metrics.length === 2, "There should be only 2 resources listed in the metrics element, but there were " +
                persistenceData.metrics.length);
            
            var appResource;
            
            if (persistenceData.metrics[0].resource === appResourceID) {
              appResource = persistenceData.metrics[0];
            } else {
              appResource = persistenceData.metrics[1];
            }
            
            assert.ok(appResource.resource === appResourceID, "The resource ID should have been " + appResourceID + 
                " but was " + appResource.resource);
            
            assert.ok(appResource.graphs.length === 1, "The number of graphs in the app resource should have been 1 but was " + 
                appResource.graphs.length);
            
            assert.ok(appResource.graphs[0].graphSection === "Servlet", "The graph section should have been Servlet but was " + 
                appResource.graphs[0].graphSection);
            
            // Check the type of Servlet graph. We have to check that the 1st one we check is either HeapStats or ProcessCPU, and once we know which
            // one we can make sure the other one is definitely for the other type.
            assert.ok(appResource.graphs[0].graphType === "RequestCountServletStats", "The graph type should have been RequestCountServletStats but was " 
                + appResource.graphs[0].graphType);
            assert.ok(appResource.graphs[0].graphInstances.length === 2, "The number of graph instances for Servlet should have been 2,  but was "
                + appResource.graphs[0].graphInstances.length);
            
            for (var instanceCount = 0; instanceCount < appResource.graphs[0].graphInstances.length; instanceCount++) {
              var currInstance = appResource.graphs[0].graphInstances[instanceCount];
              if (currInstance.order === 3) {
                assert.ok(currInstance.width === 300, "The graph width for Servlet should have been 300 but was " + currInstance.width);
                assert.ok(currInstance.height === 300, "The graph height for Servlet should have been 300 but was " + currInstance.height);
                checkServletDataField(currInstance.data, ["A", "B", "C", "D", "E", "F"]);
              } else {
                assert.ok(currInstance.order === 25, "The graph order for Servlet should have been 25 but was " + currInstance.order);
                assert.ok(currInstance.width === 50, "The graph width for Servlet should have been 50 but was " + currInstance.width);
                assert.ok(currInstance.height === 50, "The graph height for Servlet should have been 50 but was " + currInstance.height);
                checkServletDataField(currInstance.data, ["E"]);
              }
            }
                  
            xhr.respond(200, { "Content-Type": "application/json"}, "");
          }));
          
          server.respond();
        });
      });
    }
});
