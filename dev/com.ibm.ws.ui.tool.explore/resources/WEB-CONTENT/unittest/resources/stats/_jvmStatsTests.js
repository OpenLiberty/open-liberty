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
     "resources/stats/_jvmStats"
       ],
   
   function (tdd, assert, jvmStats) {
  
      with(assert) {
   
          tdd.suite("jvmStats Test Suite", function() {
   
            var server;
            var mbeanURLPrefix;

            tdd.beforeEach(function() {
              // Mock the admin center server since it is not available in a unittest
              server = sinon.fakeServer.create();
              
              mbeanURLPrefix = '\/IBMJMXConnectorREST\/mbeans/';
            });
            
            tdd.afterEach(function() {
              server.restore();
            });
            
            tdd.test("getHeapMemoryUsage Test", function() {
              
              var dfd = this.async(1000);
              jvmStats.getHeapMemoryUsage(this.serverResponse).then(dfd.callback(function(response) {
                var jsonResp = JSON.parse(response);
                assert.equal(jsonResp.Used, 33762400);
                assert.equal(jsonResp.Committed, 46333952);
                assert.equal(jsonResp.Max, 536870912);
              }), dfd.reject.bind(dfd));
              // ^ above taken from github Intern tutorial. Not sure what the 
              // proper code is when there's an error...
              /*}), dfd.rejectOnError(function(err) {
                console.log("error!", err);
              }));*/
              
              // Return a mock response from mock server
              var response = {"value":{"committed":"46333952","init":"4194304","max":"536870912","used":"33762400"},
                  "type":{"className":"javax.management.openmbean.CompositeDataSupport","openType":"0"},
                  "openTypes":[{"openTypeClass":"javax.management.openmbean.CompositeType","className":"javax.management.openmbean.CompositeData",
                    "typeName":"java.lang.management.MemoryUsage","description":"java.lang.management.MemoryUsage",
                    "items":[{"key":"committed","description":"committed","type":"1"},{"key":"init","description":"init","type":"1"},
                             {"key":"max","description":"max","type":"1"},{"key":"used","description":"used","type":"1"}]},"java.lang.Long"]};
               
              server.respondWith("GET", /\/IBMJMXConnectorREST\/mbeans\/java.lang:type=Memory\/attributes\/HeapMemoryUsage.*/, function(xhr) {
                xhr.respond(200, { "Content-Type": "application/json"}, JSON.stringify(response));
              });
              
              // Trigger the server to respond to with our required JSON response.
              server.respond();
            });

            tdd.test("getThreads Test", function() {
              
              var dfd = this.async(1000);
              jvmStats.getThreads(this.serverResponse).then(dfd.callback(function(response) {
                var jsonResp = JSON.parse(response);
                assert.equal(jsonResp.ThreadCount, 59);
                assert.equal(jsonResp.PeakThreadCount, 59);
                assert.equal(jsonResp.TotalStartedThreadCount, 80);
              }), dfd.reject.bind(dfd));

              var response = [{"name":"ThreadCount","value":{"value":"59","type":"java.lang.Integer"}},
                              {"name":"PeakThreadCount","value":{"value":"59","type":"java.lang.Integer"}},
                              {"name":"TotalStartedThreadCount","value":{"value":"80","type":"java.lang.Long"}}];

              // If the server gets the relevant Mbean URL it will return the corresponding response.
              server.respondWith("GET", /\/IBMJMXConnectorREST\/mbeans\/java.lang:type=Threading\/.*/, function(xhr) {
                    xhr.respond(200, { "Content-Type": "application/json"}, JSON.stringify(response));
                  });
              
              // Trigger the server to respond with our required JSON response.
              server.respond();
            });

            tdd.test("getLoadedClasses Test", function() {
              
              var dfd = this.async(1000);
              jvmStats.getLoadedClasses(this.serverResponse).then(dfd.callback(function(response) {
                var jsonResp = JSON.parse(response);
                assert.equal(jsonResp.LoadedClassCount, 7992);
                assert.equal(jsonResp.UnloadedClassCount, 0);
                assert.equal(jsonResp.TotalLoadedClassCount, 7992);
              }), dfd.reject.bind(dfd));

              // Return a mock response from mock server
              var response = [{"name":"LoadedClassCount","value":{"value":"7992","type":"java.lang.Integer"}},
                              {"name":"UnloadedClassCount","value":{"value":"0","type":"java.lang.Long"}},
                              {"name":"TotalLoadedClassCount","value":{"value":"7992","type":"java.lang.Long"}}];

              // If the server gets the relevant Mbean URL it will return the corresponding response.
              server.respondWith("GET", /\/IBMJMXConnectorREST\/mbeans\/java.lang:type=ClassLoading\/.*/, function(xhr) {
                    xhr.respond(200, { "Content-Type": "application/json"}, JSON.stringify(response));
                  });
              
              // Trigger the server to respond with our required JSON response.
              server.respond();
            });
            
            tdd.test("getCPUUsage Test", function() {
              
              var dfd = this.async(1000);
              jvmStats.getCPUUsage(this.serverResponse).then(dfd.callback(function(response) {
                var jsonResp = JSON.parse(response);
                assert.equal(jsonResp.ProcessCpuLoad, 0.13330497370727432);
              }), dfd.reject.bind(dfd));

              // Return a mock response from mock server
              var response = [{"name":"ProcessCpuLoad","value":{"value":"0.13330497370727432","type":"java.lang.Double"}}];

              // If the server gets the relevant Mbean URL it will return the corresponding response.
              server.respondWith("GET", /\/IBMJMXConnectorREST\/mbeans\/java.lang:type=OperatingSystem\/.*/, function(xhr) {
                    xhr.respond(200, { "Content-Type": "application/json"}, JSON.stringify(response));
                  });
              
              // Trigger the server to respond with our required JSON response.
              server.respond();
            });
            
            /*
             * This test tests the SystemCPULoad request as this property is sometimes the attribute on the operatingSystem mbean.
             */ 
            tdd.test("getSystemCPUUsage Test", function() {
              
              var dfd = this.async(1000);
              jvmStats.getCPUUsage(this.serverResponse).then(dfd.callback(function(response) {
                var jsonResp = JSON.parse(response);
                assert.equal(jsonResp.SystemCpuLoad, 0.13330497370727432);
              }), dfd.reject.bind(dfd));

              // Return a mock response from mock server
              var response = [{"name":"SystemCpuLoad","value":{"value":"0.13330497370727432","type":"java.lang.Double"}}];

              // If the server gets the relevant Mbean URL it will return the corresponding response.
              server.respondWith("GET", /\/IBMJMXConnectorREST\/mbeans\/java.lang:type=OperatingSystem\/.*/, function(xhr) {
                    xhr.respond(200, { "Content-Type": "application/json"}, JSON.stringify(response));
                  });
              
              // Trigger the server to respond with our required JSON response.
              server.respond();
            });
            
            tdd.test("getHeapMemoryUsage Error Test", function() {
              
              var dfd = this.async(1000);
              jvmStats.getHeapMemoryUsage(this.serverResponse).then(dfd.callback(function(response) {
                assert.equal(response.response.status, 500);
                assert.equal(response.response.text, JSON.stringify(errMsg));
              }), dfd.reject.bind(dfd));

              // Return a mock error message from mock server. I get a SyntaxError
              // if I make this a plain text message. Something in Dojo expects JSON only
              var errMsg = {'Error': 'An error occurred on the fake server trying to process HeapMemoryUsage stats request!'};

              server.respondWith("GET", /\/IBMJMXConnectorREST\/mbeans\/java.lang:type=Memory\/attributes\/HeapMemoryUsage.*/, function(xhr) {
                xhr.respond(500, { "Content-Type": "application/json" }, JSON.stringify(errMsg));
              });
              
              // Trigger the server to respond with our required JSON response.
              server.respond();
            });

            tdd.test("getThreads Error Test", function() {
              
              var dfd = this.async(1000);
              jvmStats.getThreads(this.serverResponse).then(dfd.callback(function(response) {
                assert.equal(response.response.status, 500);
                assert.equal(JSON.stringify(errMsg), response.response.text);
              }), dfd.reject.bind(dfd));

              // Return a mock error message from mock server.
              var errMsg = {'Error': 'An error occurred on the fake server trying to process Threading stats request!'};

              server.respondWith("GET", /\/IBMJMXConnectorREST\/mbeans\/java.lang:type=Threading\/.*/, function(xhr) {
                    xhr.respond(500, { "Content-Type": "application/json"}, JSON.stringify(errMsg));
                  });
              
              // Trigger the server to respond with our required JSON response.
              server.respond();
            });

            tdd.test("getLoadedClasses Error Test", function() {
              
              var dfd = this.async(1000);
              jvmStats.getLoadedClasses(this.serverResponse).then(dfd.callback(function(response) {
                assert.equal(response.response.status, 500);
                assert.equal(JSON.stringify(errMsg), response.response.text);
              }), dfd.reject.bind(dfd));

              // Return a mock error message from mock server.
              var errMsg = {'Error': 'An error occurred on the fake server trying to process ClassLoading stats request!'};

              server.respondWith("GET", /\/IBMJMXConnectorREST\/mbeans\/java.lang:type=ClassLoading\/.*/, function(xhr) {  
                    xhr.respond(500, { "Content-Type": "application/json"}, JSON.stringify(errMsg));
                  });
              
              // Trigger the server to respond with our required JSON response.
              server.respond();
            });
            
            tdd.test("getCPUUsage Error Test", function() {
              
              var dfd = this.async(1000);
              jvmStats.getCPUUsage(this.serverResponse).then(dfd.callback(function(response) {
                assert.equal(response.response.status, 500);
                assert.equal(response.response.text, JSON.stringify(errMsg));
              }), dfd.reject.bind(dfd));

              // Return a mock error message from mock server.
              var errMsg = {'Error': 'An error occurred on the fake server trying to process CPU load stats request!'};

              server.respondWith("GET", /\/IBMJMXConnectorREST\/mbeans\/java.lang:type=OperatingSystem\/.*/, function(xhr) {
                    xhr.respond(500, { "Content-Type": "application/json"}, JSON.stringify(errMsg));
                  });
              
              // Trigger the server to respond with our required JSON response.
              server.respond();
            });
          });
      }
  });