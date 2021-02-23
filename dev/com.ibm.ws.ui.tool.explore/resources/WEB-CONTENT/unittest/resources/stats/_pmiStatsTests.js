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
     "dojo/json",
     "resources/stats/_pmiStats"
       ],
       
    function(tdd, assert, json, pmiStats) {

    // Responses for the servers.
    var connectionPoolResponse = [{"name":"CreateCount","value":{"value":"1","type":"java.lang.Long"}},
                {"name":"DestroyCount","value":{"value":"0","type":"java.lang.Long"}},
                {"name":"ManagedConnectionCount","value":{"value":"1","type":"java.lang.Long"}},
                {"name":"WaitTime","value":{"value":"0.0","type":"java.lang.Double"}},
                {"name":"ConnectionHandleCount","value":{"value":"0","type":"java.lang.Long"}},
                {"name":"FreeConnectionCount","value":{"value":"1","type":"java.lang.Long"}}];
    
    var threadPoolResponse =  [{"name":"PoolSize","value":{"value":"61","type":"java.lang.Integer"}},
                             {"name":"ActiveThreads","value":{"value":"3","type":"java.lang.Integer"}}];
  
    var sessionStatsResponse =  [{"name":"ActiveCount","value":{"value":"0","type":"java.lang.Long"}},
                                 {"name":"LiveCount","value":{"value":"1","type":"java.lang.Long"}},
                                 {"name":"CreateCount","value":{"value":"1","type":"java.lang.Long"}},
                                 {"name":"InvalidatedCountbyTimeout","value":{"value":"0","type":"java.lang.Long"}},
                                 {"name":"InvalidatedCount","value":{"value":"0","type":"java.lang.Long"}}];

    var jaxwsServerResponse = [{"name":"NumInvocations","value":{"value":"1","type":"java.lang.Integer"}},
                              {"name":"NumCheckedApplicationFaults","value":{"value":"0","type":"java.lang.Integer"}},
                              {"name":"NumLogicalRuntimeFaults","value":{"value":"0","type":"java.lang.Integer"}},
                              {"name":"NumRuntimeFaults","value":{"value":"0","type":"java.lang.Integer"}},
                              {"name":"NumUnCheckedApplicationFaults","value":{"value":"0","type":"java.lang.Integer"}},
                              {"name":"AvgResponseTime","value":{"value":"152769","type":"java.lang.Integer"}},
                              {"name":"MaxResponseTime","value":{"value":"152769","type":"java.lang.Long"}},
                              {"name":"MinResponseTime","value":{"value":"152769","type":"java.lang.Long"}},
                              {"name":"TotalHandlingTime","value":{"value":"152769","type":"java.lang.Long"}}];
    
    var jaxwsClientResponse = [{"name":"NumInvocations","value":{"value":"1","type":"java.lang.Integer"}},
                               {"name":"NumCheckedApplicationFaults","value":{"value":"0","type":"java.lang.Integer"}},
                               {"name":"NumLogicalRuntimeFaults","value":{"value":"0","type":"java.lang.Integer"}},
                               {"name":"NumRuntimeFaults","value":{"value":"0","type":"java.lang.Integer"}},
                               {"name":"NumUnCheckedApplicationFaults","value":{"value":"0","type":"java.lang.Integer"}},
                               {"name":"AvgResponseTime","value":{"value":"806793","type":"java.lang.Integer"}},
                               {"name":"MaxResponseTime","value":{"value":"806793","type":"java.lang.Long"}},
                               {"name":"MinResponseTime","value":{"value":"806793","type":"java.lang.Long"}},
                               {"name":"TotalHandlingTime","value":{"value":"806793","type":"java.lang.Long"}}];
  
    var servletStatsResponse =  [{"name":"RequestCountDetails",
                                  "value": {"value":{"currentValue":"24",
                                  "description":"This shows number of requests to a servlet",
                                  "reading":{"count":"24", "timestamp":"1417688091760", "unit":"ns"},
                                  "unit":"ns"},
                                  "type":{"className":"javax.management.openmbean.CompositeDataSupport",
                                  "openType":"0"},
                                  "openTypes":[{"openTypeClass":"javax.management.openmbean.CompositeType",
                                        "className":"javax.management.openmbean.CompositeData",
                                        "typeName":"com.ibm.websphere.monitor.meters.Counter",
                                        "description":"com.ibm.websphere.monitor.meters.Counter",
                                        "items":[{"key":"currentValue", "description":"currentValue", "type":"1"},
                                                 {"key":"description", "description":"description", "type":"2"},
                                                 {"key":"reading", "description": "reading", "type":"3"},
                                                 {"key":"unit", "description":"unit","type":"2"}]},
                                        "java.lang.Long","java.lang.String",
                                        {"openTypeClass":"javax.management.openmbean.CompositeType",
                                         "className":"javax.management.openmbean.CompositeData",
                                         "typeName":"com.ibm.websphere.monitor.meters.CounterReading",
                                         "description":"com.ibm.websphere.monitor.meters.CounterReading",
                                         "items":[{"key":"count","description":"count","type":"1"},
                                                  {"key":"timestamp","description":"timestamp","type":"1"},
                                                  {"key":"unit","description":"unit","type":"2"}]
                                         }]
                                  }},{"name":"ResponseTimeDetails",
                                      "value":{"value":{"count":"13",
                                                        "description":"Average Response Time for servlet",
                                                        "maximumValue":"807544824",
                                                        "mean":"1.2898803015384616E8",
                                                      "minimumValue":"2363151",
                                                      "reading":{"count":"13",
                                                                 "maximumValue":"807544824",
                                                                 "mean":"1.2898803015384616E8",
                                                                 "minimumValue":"2363151",
                                                                 "standardDeviation":"2.4270193756603527E8",
                                                                 "timestamp":"1417688091760",
                                                                 "total":"1.676844392E9",
                                                                 "unit":"ns",
                                                                 "variance":"5.890423049830768E16"},
                                                       "standardDeviation":"2.4644741716053835E8",
                                                       "total":"1.676844392E9",
                                                       "unit":"ns",
                                                       "variance":"5.0836644513616104E16"},
                                                       "type":{"className":"javax.management.openmbean.CompositeDataSupport",
                                                               "openType":"0"},
                                                       "openTypes":[{"openTypeClass":"javax.management.openmbean.CompositeType",
                                                                     "className":"javax.management.openmbean.CompositeData",
                                                                     "typeName":"com.ibm.websphere.monitor.meters.StatisticsMeter",
                                                                     "description":"com.ibm.websphere.monitor.meters.StatisticsMeter",
                                                                     "items":[{"key":"count", "description":"count","type":"1"},
                                                                              {"key":"description","description":"description","type":"2"},
                                                                              {"key":"maximumValue","description":"maximumValue","type":"1"},
                                                                              {"key":"mean","description":"mean","type":"3"},
                                                                              {"key":"minimumValue","description":"minimumValue","type":"1"},
                                                                              {"key":"reading","description":"reading","type":"4"},
                                                                              {"key":"standardDeviation","description":"standardDeviation","type":"3"},
                                                                              {"key":"total","description":"total","type":"3"},
                                                                              {"key":"unit","description":"unit","type":"2"},
                                                                              {"key":"variance","description":"variance","type":"3"}]},
                                                                       "java.lang.Long","java.lang.String","java.lang.Double",
                                                                       {"openTypeClass":"javax.management.openmbean.CompositeType",
                                                                        "className":"javax.management.openmbean.CompositeData",
                                                                        "typeName":"com.ibm.websphere.monitor.meters.StatisticsReading",
                                                                        "description":"com.ibm.websphere.monitor.meters.StatisticsReading",
                                                                        "items":[{"key":"count","description":"count","type":"1"},
                                                                                 {"key":"maximumValue","description":"maximumValue","type":"1"},
                                                                                 {"key":"mean","description":"mean","type":"3"},
                                                                                 {"key":"minimumValue","description":"minimumValue","type":"1"},
                                                                                 {"key":"standardDeviation","description":"standardDeviation","type":"3"},
                                                                                 {"key":"timestamp","description":"timestamp","type":"1"},
                                                                                 {"key":"total","description":"total","type":"3"},
                                                                                 {"key":"unit","description":"unit","type":"2"},
                                                                                 {"key":"variance","description":"variance","type":"3"}]}]}}];

    var availMBeansForServletStatsResp = [{"objectName":"WebSphere:type=ServletStats,name=testApp.HelloWorldClientServlet","className":"com.ibm.ws.webcontainer.monitor.ServletStats",
                                  "URL":"/IBMJMXConnectorREST/mbeans/WebSphere%3Aname%3DtestApp.HelloWorldClientServlet%2Ctype%3DServletStats"}];
    
    var multipleMBeansForServletStatsResp = [{"objectName":"WebSphere:type=ServletStats,name=testApp.HelloWorldClientServlet","className":"com.ibm.ws.webcontainer.monitor.ServletStats1",
        "URL":"/IBMJMXConnectorREST/mbeans/WebSphere%3Aname%3DtestApp.HelloWorldClientServlet%2Ctype%3DServletStats"},
        {"objectName":"WebSphere:type=ServletStats,name=testApp.NewServlet","className":"com.ibm.ws.webcontainer.monitor.ServletStats2",
        "URL":"/IBMJMXConnectorREST/mbeans/WebSphere%3Aname%3DtestApp.NewServlet%2Ctype%3DServletStats"},
        {"objectName":"WebSphere:type=ServletStats,name=testApp.CompletelyDifferentServlet","className":"com.ibm.ws.webcontainer.monitor.ServletStats3",
         "URL":"/IBMJMXConnectorREST/mbeans/WebSphere%3Aname%3DtestApp.CompletelyDifferentServlet%2Ctype%3DServletStats"}];
    
    var availMBeansForSessionStatsResp = [{"objectName":"WebSphere:type=SessionStats,name=default_host%2Fibm%2Fapi",
      "className":"com.ibm.ws.monitors.helper.SessionStats","URL":"/IBMJMXConnectorREST/mbeans/WebSphere%3Atype%3DSessionStats%2Cname%3Ddefault_host%2Fibm%2Fapi"}];
    
    var availMBeansForConnPoolStatsResp = [{"objectName":"WebSphere:type=ConnectionPoolStats,name=jdbc/TradeDataSource",
      "className":"com.ibm.ws.connectionpool.monitor.ConnectionPoolStats","URL":"/IBMJMXConnectorREST/mbeans/WebSphere%3Aname%3Djdbc%2FTradeDataSource%2Ctype%3DConnectionPoolStats"}];
    
    var multipleMBeansForConnPoolStatsResp = [{"objectName":"WebSphere:type=ConnectionPoolStats,name=jdbc/TradeDataSource",
      "className":"com.ibm.ws.connectionpool.monitor.ConnectionPoolStats","URL":"/IBMJMXConnectorREST/mbeans/WebSphere%3Aname%3Djdbc%2FTradeDataSource%2Ctype%3DConnectionPoolStats"},
      {"objectName":"WebSphere:type=ConnectionPoolStats,name=jdbc/HelloKittyDataSource",
       "className":"com.ibm.ws.connectionpool.monitor.ConnectionPoolStats","URL":"/IBMJMXConnectorREST/mbeans/WebSphere%3Aname%3Djdbc%2FHelloKittyDataSource%2Ctype%3DConnectionPoolStats"},
      {"objectName":"WebSphere:type=ConnectionPoolStats,name=jdbc/shoppingcartdb",
       "className":"com.ibm.ws.connectionpool.monitor.ConnectionPoolStats","URL":"/IBMJMXConnectorREST/mbeans/WebSphere%3Aname%3Djdbc%2Fshoppingcartdb%2Ctype%3DConnectionPoolStats"}];
    
    var availMBeansForThreadPoolStatsResp = [{"objectName":"WebSphere:type=ThreadPoolStats,name=Default Executor",
      "className":"com.ibm.ws.monitors.helper.ThreadPoolStats","URL":"/IBMJMXConnectorREST/mbeans/WebSphere%3Aname%3DDefault+Executor%2Ctype%3DThreadPoolStats"}];

  with(assert) {
    
    /**
     * Defines the 'PMI Stats Tests' module test suite.
     */
    tdd.suite('PMI Stats Tests', function() {
      
      var server;
      var serverResource;
      var mbeanURLPrefix;

      tdd.beforeEach(function() {
        // Mock the admin center server since it is not available in a unittest
        server = sinon.fakeServer.create();
        
        // Mock a server resource
        serverResource = {
            fullName: "localhost,C:/libertyHome/usr/servers/mockServer,mockServer",
            host: "localhost",
            userdir: "C:/libertyHome/usr/servers/mockServer",
            type: "standaloneServer"
        };
        
        mbeanURLPrefix = '\/IBMJMXConnectorREST\/mbeans/';
      });
      
      tdd.afterEach(function() {
        server.restore();
      });
  
      tdd.test('ConnectionPool Test UnEncoded', function() {
  
           var dfd = this.async(1000);
           pmiStats.getConnectionPoolStats(serverResource, "jdbc/TradeDataSource").then(dfd.callback(function(response) {
               var jsonResp = JSON.parse(response);
               assert.equal(jsonResp.CreateCount, 1);
               assert.equal(jsonResp.DestroyCount, 0);
               assert.equal(jsonResp.ManagedConnectionCount, 1);
               assert.equal(jsonResp.ConnectionHandleCount, 0);
               assert.equal(jsonResp.FreeConnectionCount, 1);
           }), dfd.reject.bind(dfd));
           
           // If the server gets the relevant Mbean URL it will return the corresponding response.
           server.respondWith("GET", /\/IBMJMXConnectorREST\/mbeans\/WebSphere:type=ConnectionPoolStats,name=jdbc%2FTradeDataSource\/.*/, function(xhr) {
             xhr.respond(200, { "Content-Type": "application/json"}, JSON.stringify(connectionPoolResponse));
           });
           
           // Trigger the server to respond with our required JSON response.
           server.respond();
         });
         
         tdd.test('ConnectionPool Test Encoded', function() {
  
           var dfd = this.async(1000);
           pmiStats.getConnectionPoolStats(serverResource, "jdbc%2FTradeDataSource").then(dfd.callback(function(response) {
               var jsonResp = JSON.parse(response);
                 assert.equal(1, jsonResp.CreateCount);
                 assert.equal(0, jsonResp.DestroyCount);
                 assert.equal(1, jsonResp.ManagedConnectionCount);
                 assert.equal(0, jsonResp.ConnectionHandleCount);
                 assert.equal(1, jsonResp.FreeConnectionCount);
           }), dfd.reject.bind(dfd));
           
           // If the server gets the relevant Mbean URL it will return the corresponding response.
           server.respondWith("GET", /\/IBMJMXConnectorREST\/mbeans\/WebSphere:type=ConnectionPoolStats,name=jdbc%2FTradeDataSource\/.*/, function(xhr) {
             xhr.respond(200, { "Content-Type": "application/json"}, JSON.stringify(connectionPoolResponse));
           });
           
           // Trigger the server to respond with our required JSON response.
           server.respond();
         });
  
         tdd.test('getSessionStats Test UnEncoded', function() {
  
           var dfd = this.async(1000);
           pmiStats.getSessionStats(serverResource, "default_host/ibm/api").then(dfd.callback(function(response) {
             var jsonResp = JSON.parse(response);
             assert.equal(jsonResp.ActiveCount, 0);
             assert.equal(jsonResp.LiveCount, 1);
             assert.equal(jsonResp.CreateCount, 1);
             assert.equal(jsonResp.InvalidatedCountbyTimeout, 0);
             assert.equal(jsonResp.InvalidatedCount, 0);
           }), dfd.reject.bind(dfd));
  
           // If the server gets the relevant Mbean URL it will return the corresponding response.
           server.respondWith("GET", /\/IBMJMXConnectorREST\/mbeans\/WebSphere:type=SessionStats,name=default_host%2Fibm%2Fapi\/.*/, function(xhr) {
             xhr.respond(200, { "Content-Type": "application/json"}, JSON.stringify(sessionStatsResponse));
           });
       
           // Trigger the server to respond with our required JSON response.
           server.respond();
         });
         
         tdd.test('getSessionStats Test Encoded', function() {

           var dfd = this.async(1000);
           pmiStats.getSessionStats(serverResource, "default_host%2Fibm%2Fapi").then(dfd.callback(function(response) {
             var jsonResp = JSON.parse(response);
               assert.equal(jsonResp.ActiveCount, 0);
               assert.equal(jsonResp.LiveCount, 1);
               assert.equal(jsonResp.CreateCount, 1);
               assert.equal(jsonResp.InvalidatedCountbyTimeout, 0);
               assert.equal(jsonResp.InvalidatedCount, 0);
           }), dfd.reject.bind(dfd));
         
           // If the server gets the relevant Mbean URL it will return the corresponding response.
           server.respondWith("GET", /\/IBMJMXConnectorREST\/mbeans\/WebSphere:type=SessionStats,name=default_host%2Fibm%2Fapi\/.*/, function(xhr) {
             xhr.respond(200, { "Content-Type": "application/json"}, JSON.stringify(sessionStatsResponse));
           });
           
           // Trigger the server to respond with our required JSON response.
           server.respond();
         });

         tdd.test('getThreadPool Stats Test UnEncoded', function() {
           
           var dfd = this.async(1000);
           pmiStats.getThreadPoolStats(serverResource, "Default Executor").then(dfd.callback(function(response) {
               var jsonResp = JSON.parse(response);
               assert.equal(jsonResp.PoolSize, 61);
               assert.equal(jsonResp.ActiveThreads, 3);
           }), dfd.reject.bind(dfd));
           
           // If the server gets the relevant Mbean URL it will return the corresponding response.
           server.respondWith("GET", /\/IBMJMXConnectorREST\/mbeans\/WebSphere:type=ThreadPoolStats,name=Default%20Executor\/.*/, function(xhr) {
             xhr.respond(200, { "Content-Type": "application/json"}, JSON.stringify(threadPoolResponse));
           });
           
           // Trigger the server to respond to with our required JSON response.
           server.respond();
         });
         
         tdd.test('getThreadPool Stats Test Encoded', function() {
           
           var dfd = this.async(1000);
           pmiStats.getThreadPoolStats(serverResource, "Default%20Executor").then(dfd.callback(function(response) {
               var jsonResp = JSON.parse(response);
               assert.equal(jsonResp.PoolSize, 61);
               assert.equal(jsonResp.ActiveThreads, 3);
           }), dfd.reject.bind(dfd));
           
           // If the server gets the relevant Mbean URL it will return the corresponding response.
           server.respondWith("GET", /\/IBMJMXConnectorREST\/mbeans\/WebSphere:type=ThreadPoolStats,name=Default%20Executor\/.*/, function(xhr) {
             xhr.respond(200, { "Content-Type": "application/json"}, JSON.stringify(threadPoolResponse));
           });
           
           // Trigger the server to respond to with our required JSON response.
           server.respond();
         });

         tdd.test('getServletStats Test', function() {
           
           var dfd = this.async(1000);
           pmiStats.getServletStats(serverResource, "testApp.TestServlet").then(dfd.callback(function(response) {
               var jsonResp = JSON.parse(response);
                 assert.equal(jsonResp.RequestCount, 24);
                 assert.equal(jsonResp.RequestLastTimestamp, 1417688091760);
                 assert.equal(jsonResp.RequestUnit, "ns");
                 assert.equal(jsonResp.ResponseCount, 13);
                 assert.equal(jsonResp.ResponseMax, 807544824);
                 assert.equal(jsonResp.ResponseMean, 1.2898803015384616E8);
                 assert.equal(jsonResp.ResponseMin, 2363151);
                 assert.equal(jsonResp.ResponseStdDev, 2.4270193756603527E8);
                 assert.equal(jsonResp.ResponseLastTimestamp, 1417688091760);
                 assert.equal(jsonResp.ResponseTotal, 1.676844392E9);
                 assert.equal(jsonResp.ResponseUnit, "ns");
                 assert.equal(jsonResp.ResponseVariance, 5.890423049830768E16);
           }), dfd.reject.bind(dfd));
           
           // If the server gets the relevant Mbean URL it will return the corresponding response.
           server.respondWith("GET", /\/IBMJMXConnectorREST\/mbeans\/WebSphere:type=ServletStats,name=testApp.TestServlet\/.*/, function(xhr) {
             xhr.respond(200, { "Content-Type": "application/json"}, JSON.stringify(servletStatsResponse));
           });
           
           // Trigger the server to respond to with our required JSON response.
           server.respond();
         });
         
         tdd.test('getJAXWS Performance Server Test', function() {
           
           var dfd = this.async(1000);
           
           pmiStats.getJAXWSStats(server, true, "HelloWorldWeb-Server-Bus", "\"{http://lab1.ws.ibm.com/}HelloWorldService\"", "\"HelloWorldPort\"", "\"sayHello\"").then(dfd.callback(function(response) {
               var jsonResp = JSON.parse(response);
                 assert.equal(jsonResp.NumInvocations, 1);
                 assert.equal(jsonResp.NumCheckedApplicationFaults, 0);
                 assert.equal(jsonResp.NumRuntimeFaults, 0);
                 assert.equal(jsonResp.NumUnCheckedApplicationFaults, 0);
                 assert.equal(jsonResp.AvgResponseTime, 152769);
                 assert.equal(jsonResp.MaxResponseTime, 152769);
                 assert.equal(jsonResp.MinResponseTime, 152769);
                 assert.equal(jsonResp.TotalHandlingTime, 152769);
           }), dfd.reject.bind(dfd));
           
           // If the server gets the relevant Mbean URL it will return the corresponding response.
           server.respondWith("GET", /\/IBMJMXConnectorREST\/mbeans\/org.apache.cxf:type=Performance.Counter.Server%2Cbus.id%3DHelloWorldWeb-Server-Bus%2Cport%3D%22HelloWorldPort%22%2Cservice%3D%22%7Bhttp%3A%2F%2Flab1.ws.ibm.com%2F%7DHelloWorldService%22%2Coperation%3D%22sayHello%22\/attributes/, function(xhr) {
             xhr.respond(200, { "Content-Type": "application/json"}, JSON.stringify(jaxwsServerResponse));
           });
           
           // Trigger the server to respond to with our required JSON response.
           server.respond();
         });
         
         tdd.test('getJAXWS Performance Client Test', function() {
             
             var dfd = this.async(1000);
             
             pmiStats.getJAXWSStats(server, false, "HelloWorldClientWeb_jaxws-Client-Bus", "\"{http://lab1.ws.ibm.com/}HelloWorldService\"", "\"HelloWorldPort\"", "\"sayHello\"").then(dfd.callback(function(response) {
                 var jsonResp = JSON.parse(response);
                 assert.equal(jsonResp.NumInvocations, 1);
                 assert.equal(jsonResp.NumCheckedApplicationFaults0);
                 assert.equal(jsonResp.NumRuntimeFaults, 0);
                 assert.equal(jsonResp.NumUnCheckedApplicationFaults, 0);
                 assert.equal(jsonResp.AvgResponseTime, 806793);
                 assert.equal(jsonResp.MaxResponseTime, 806793);
                 assert.equal(jsonResp.MinResponseTime, 806793);
                 assert.equal(jsonResp.TotalHandlingTime, 806793);
             }), dfd.reject.bind(dfd));
             
             // If the server gets the relevant Mbean URL it will return the corresponding response.
             
             server.respondWith("GET", /\/IBMJMXConnectorREST\/mbeans\/org.apache.cxf:type=Performance.Counter.Client%2Cbus.id%3DHelloWorldClientWeb_jaxws-Client-Bus%2Cport%3D%22HelloWorldPort%22%2Cservice%3D%22%7Bhttp%3A%2F%2Flab1.ws.ibm.com%2F%7DHelloWorldService%22%2Coperation%3D%22sayHello%22\/attributes/, function(xhr) {
               xhr.respond(200, { "Content-Type": "application/json"}, JSON.stringify(jaxwsClientResponse));
             });
             
             // Trigger the server to respond to with our required JSON response.
             server.respond();
           }),
         
         tdd.test('getServletsForApp Test', function() {
           
           var dfd = this.async(1000);
           pmiStats.getServletsForApp("testApp", serverResource).then(dfd.callback(function(response) {
              assert.equal(response[0], "testApp.HelloWorldClientServlet");
           }), dfd.reject.bind(dfd));
           
           // getServletsForApp() calls _mbeanUtils.getMBeans(), so URL is to match that call 
           server.respondWith("GET", this.mbeanURLPrefix, function(xhr) {
             xhr.respond(200, { "Content-Type": "application/json"}, JSON.stringify(availMBeansForServletStatsResp));
           });
           
           // Trigger the server to respond to with our required JSON response.
           server.respond();

           return dfd;
         });
         
         tdd.test('getServletsForApp Test: Multiple Servlets', function() {
           
           var expectedArray = [ "testApp.HelloWorldClientServlet", "testApp.NewServlet", "testApp.CompletelyDifferentServlet"];
           var dfd = this.async(1000);
           pmiStats.getServletsForApp("testApp", serverResource).then(dfd.callback(function(response) {
               for (var i = 0; i < response.length; i++) {
                 assert.equal(response[i], expectedArray[i]); 
               }
           }), dfd.reject.bind(dfd));
           
           // getServletsForApp() calls _mbeanUtils.getMBeans(), so URL is to match that call 
           server.respondWith("GET", this.mbeanURLPrefix, function(xhr) {
             xhr.respond(200, { "Content-Type": "application/json"}, JSON.stringify(multipleMBeansForServletStatsResp));
           });
           
           // Trigger the server to respond to with our required JSON response.
           server.respond();
         }),
         
         tdd.test('getSessionsForApp Test', function() {
           
           var dfd = this.async(1000);
           pmiStats.getSessionsForApp("testApp", serverResource).then(dfd.callback(function(response) {
              assert.equal(response[0], "default_host%2Fibm%2Fapi");
           }), dfd.reject.bind(dfd));
           
           // getSessionsForApp() calls _mbeanUtils.getMBeans(), so URL is to match that call 
           server.respondWith("GET", this.mbeanURLPrefix, function(xhr) {
             xhr.respond(200, { "Content-Type": "application/json"}, JSON.stringify(availMBeansForSessionStatsResp));
           });
           
           // Trigger the server to respond to with our required JSON response.
           server.respond();
         });
         
         tdd.test('getServerSessions Test', function() {
           
           var dfd = this.async(1000);
           pmiStats.getServerSessions(serverResource).then(dfd.callback(function(response) {
              assert.equal(response[0], "default_host%2Fibm%2Fapi");
           }), dfd.reject.bind(dfd));
           
           // getSessionsForApp() calls _mbeanUtils.getMBeans(), so URL is to match that call 
           server.respondWith("GET", this.mbeanURLPrefix, function(xhr) {
             xhr.respond(200, { "Content-Type": "application/json"}, JSON.stringify(availMBeansForSessionStatsResp));
           });
           
           // Trigger the server to respond to with our required JSON response.
           server.respond();
         });
         
         tdd.test('getDataSourcesForServer Test', function() {
           
           var dfd = this.async(1000);
           pmiStats.getDataSourcesForServer(serverResource).then(dfd.callback(function(response) {
               assert.equal(response[0], "jdbc/TradeDataSource"); 
           }), dfd.reject.bind(dfd));
           
           // getDataSourcesForServer() calls _mbeanUtils.getMBeans(), so URL is to match that call 
           server.respondWith("GET", this.mbeanURLPrefix, function(xhr) {
             xhr.respond(200, { "Content-Type": "application/json"}, JSON.stringify(availMBeansForConnPoolStatsResp));
           });
           
           // Trigger the server to respond to with our required JSON response.
           server.respond();
         }),
         
         tdd.test('getDataSourcesForServer Test: Multiple Datasources', function() {

           var expectedArray = [ "jdbc/TradeDataSource", "jdbc/HelloKittyDataSource", "jdbc/shoppingcartdb"];
           var dfd = this.async(1000);
           pmiStats.getDataSourcesForServer(serverResource).then(dfd.callback(function(response) {
               for (var i = 0; i < response.length; i++) {
                 assert.equal(response[i], expectedArray[i]); 
               }
           }), dfd.reject.bind(dfd));
           
           // getDataSourcesForServer() calls _mbeanUtils.getMBeans(), so URL is to match that call 
           server.respondWith("GET", this.mbeanURLPrefix, function(xhr) {
             xhr.respond(200, { "Content-Type": "application/json"}, JSON.stringify(multipleMBeansForConnPoolStatsResp));
           });
           
           // Trigger the server to respond to with our required JSON response.
           server.respond();
         }),
         
         tdd.test('getThreadPoolExecutorsForServer Test', function() {
           
           var dfd = this.async(1000);
           pmiStats.getThreadPoolExecutorsForServer(serverResource).then(dfd.callback(function(response) {
               for (var i = 0; i < response.length; i++) {
                 assert.equal(response[0], "Default Executor"); 
               }
           }), dfd.reject.bind(dfd));
           
           // getThreadPoolExecutorsForServer() calls _mbeanUtils.getMBeans(), so URL is to match that call 
           server.respondWith("GET", this.mbeanURLPrefix, function(xhr) {
             xhr.respond(200, { "Content-Type": "application/json"}, JSON.stringify(availMBeansForThreadPoolStatsResp));
           });
           
           // Trigger the server to respond to with our required JSON response.
           server.respond();
         });
    });
  }
});
//@ sourceURL=_pmiStatsTest.js
