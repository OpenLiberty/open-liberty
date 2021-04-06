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
     "resources/stats/_mbeanUtils"
       ],
   
   function (tdd, assert, json, mbeanUtils) {
    
    var mbeansResponse = [{"objectName":"WebSphere:type=ServletStats1,name=App1.Servlet1","className":"com.ibm.ws.webcontainer.monitor.ServletStats1","URL":"/IBMJMXConnectorREST/mbeans/WebSphere%3Aname%3DApp1.Servlet1%2Ctype%3DServletStats"},
                          {"objectName":"WebSphere:type=ServletStats2,name=App1.Servlet2","className":"com.ibm.ws.webcontainer.monitor.ServletStats2","URL":"/IBMJMXConnectorREST/mbeans/WebSphere%3Aname%3DApp1.Servlet2%2Ctype%3DServletStats"},
                          {"objectName":"WebSphere:type=ServletStats1,name=App1.Servlet3","className":"com.ibm.ws.webcontainer.monitor.ServletStats3","URL":"/IBMJMXConnectorREST/mbeans/WebSphere%3Aname%3DApp1.Servlet3%2Ctype%3DServletStats"},
                          {"objectName":"WebSphere:type=ServletStats22,name=App2.Servlet1","className":"com.ibm.ws.webcontainer.monitor.ServletStats1","URL":"/IBMJMXConnectorREST/mbeans/WebSphere%3Aname%3DApp2.Servlet1%2Ctype%3DServletStats"},
                          {"objectName":"WebSphere:type=ServletStats11,name=App2.Servlet2","className":"com.ibm.ws.webcontainer.monitor.ServletStats2","URL":"/IBMJMXConnectorREST/mbeans/WebSphere%3Aname%3DApp2.Servlet2%2Ctype%3DServletStats"},
                          {"objectName":"WebSphere:type=ServletStats2,name=App2.Servlet3","className":"com.ibm.ws.webcontainer.monitor.ServletStats3","URL":"/IBMJMXConnectorREST/mbeans/WebSphere%3Aname%3DApp2.Servlet3%2Ctype%3DServletStats"},
                          
                          {"objectName":"WebSphere:type=ThreadPoolStats,name=Default Executor","className":"com.ibm.ws.monitors.helper.ThreadPoolStats","URL":"/IBMJMXConnectorREST/mbeans/WebSphere%3Aname%3DDefault+Executor%2Ctype%3DThreadPoolStats"},
                          
                          {"objectName":"WebSphere:type=SessionStats,name=default_host%2Fibm%2Fapi","className":"com.ibm.ws.monitors.helper.SessionStats","URL":"/IBMJMXConnectorREST/mbeans/WebSphere%3Atype%3DSessionStats%2Cname%3Ddefault_host%2Fibm%2Fapi"},
                          {"objectName":"osgi.core:type=serviceState,version=1.7,framework=org.eclipse.osgi,uuid=404ff712-4685-0014-1b4e-8cb5ed8b9d1e","className":"org.apache.aries.jmx.framework.ServiceState","URL":"/IBMJMXConnectorREST/mbeans/osgi.core%3Aframework%3Dorg.eclipse.osgi%2Ctype%3DserviceState%2Cuuid%3D404ff712-4685-0014-1b4e-8cb5ed8b9d1e%2Cversion%3D1.7"}];

    with(assert) {
      
      /**
       * Defines the 'MBean Utility Tests' module test suite.
       */
      tdd.suite('MBean Utility Tests', function() {
        
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
                 userdir: "C:/libertyHome/usr/servers/mockServer"
             };
             
             mbeanURLPrefix = '\/IBMJMXConnectorREST\/mbeans/';
           });
           
           tdd.afterEach(function() {
             server.restore();
           });
    
           /* This tests that the getMbeans wildcard searching returns the correct collection of mbeans. In this test
            * we get all type=ServletStats* and all name=App1.*.
            */
           tdd.test('FindApp1 Servlets', function() {
    
             var dfd = this.async(1000);
             mbeanUtils.getMBeans(server, "WebSphere:type=ServletStats*,name=App1.*").then(dfd.callback(function(response) {
                 assert.ok(response.length === 3, 
                     "There should have been 3 mbeans found, but " + response.length + " were returned.");
                 
                 var servlet1Found = false;
                 var servlet2Found = false;
                 var servlet3Found = false;
                 
                 // If we've got the right number of mbeans, we need to ensure we have the correct ones.
                 // Loop through each one and store which ones we find. We then check that each of the expected ones have
                 // be found.
                 for (var responseCount = 0; responseCount < response.length; responseCount++) {
                     if (response[responseCount] === "WebSphere:type=ServletStats1,name=App1.Servlet1")
                         servlet1Found = true;
                     else if (response[responseCount] === "WebSphere:type=ServletStats2,name=App1.Servlet2")
                         servlet2Found = true;
                     else if (response[responseCount] === "WebSphere:type=ServletStats1,name=App1.Servlet3")
                         servlet3Found = true;
                 }
                 
                 assert.ok(servlet1Found, 
                        "Unable to find mbean WebSphere:type=ServletStats1,name=App1.Servlet1");
                 assert.ok(servlet2Found, 
                        "Unable to find mbean WebSphere:type=ServletStats2,name=App1.Servlet2");
                 assert.ok(servlet3Found, 
                         "Unable to find mbean WebSphere:type=ServletStats1,name=App1.Servlet3");                     
             }), dfd.reject.bind(dfd));

             
             server.respondWith("GET", this.mbeanURLPrefix, function(xhr) {
               xhr.respond(200, { "Content-Type": "application/json"}, JSON.stringify(mbeansResponse));
             });
             
             // Trigger the server to respond to with our required JSON response.
             server.respond();
           }),
           
           /* This tests that the getMbeans wildcard searching returns the correct collection of mbeans. In this test
            * we get all type=ServletStats* and all name=App2.*.
            */
           tdd.test('FindApp2 Servlets', function() {
    
             var dfd = this.async(1000);
             mbeanUtils.getMBeans(server, "WebSphere:type=ServletStats*,name=App2.*").then(dfd.callback(function(response) {
                 
                   assert.ok(response.length === 3, 
                       "There should have been 3 mbeans found, but " + response.length + " were returned.");
                   
                   var servlet1Found = false;
                   var servlet2Found = false;
                   var servlet3Found = false;
                   
                   // If we've got the right number of mbeans, we need to ensure we have the correct ones.
                   // Loop through each one and store which ones we find. We then check that each of the expected ones have
                   // be found.
                   for (var responseCount = 0; responseCount < response.length; responseCount++) {
                       if (response[responseCount] === "WebSphere:type=ServletStats22,name=App2.Servlet1")
                           servlet1Found = true;
                       else if (response[responseCount] === "WebSphere:type=ServletStats11,name=App2.Servlet2")
                           servlet2Found = true;
                       else if (response[responseCount] === "WebSphere:type=ServletStats2,name=App2.Servlet3")
                           servlet3Found = true;
                   }
                   
                   assert.ok(servlet1Found, 
                          "Unable to find mbean WebSphere:type=ServletStats22,name=App2.Servlet1");
                   assert.ok(servlet2Found, 
                          "Unable to find mbean WebSphere:type=ServletStats11,name=App2.Servlet2");
                   assert.ok(servlet3Found, 
                           "Unable to find mbean WebSphere:type=ServletStats2,name=App2.Servlet3"); 
             }), dfd.reject.bind(dfd));             
             
             server.respondWith("GET", this.mbeanURLPrefix, function(xhr) {
               xhr.respond(200, { "Content-Type": "application/json"}, JSON.stringify(mbeansResponse));
             });
             
             // Trigger the server to respond to with our required JSON response.
             server.respond();
           }),
           
           /* This tests that the getMbeans wildcard searching returns the correct collection of mbeans. In this test
            * we get all type=ServletStats2* for App1. This should return the ServletStats2.
            */
           tdd.test('Find All App1* ServletStats2*', function() {
    
             var dfd = this.async(1000);
             mbeanUtils.getMBeans(server, "WebSphere:type=ServletStats2*,name=App1*").then(dfd.callback(function(response) {
                   assert.ok(response.length === 1, 
                       "There should have been 1 mbeans found, but " + response.length + " were returned.");
                   assert.equal(response[0], "WebSphere:type=ServletStats2,name=App1.Servlet2",
                          "We should have found mbean WebSphere:type=ServletStats2,name=App1.Servlet, but was " + response[0]);
              }), dfd.reject.bind(dfd));
             
             
             server.respondWith("GET", this.mbeanURLPrefix, function(xhr) {
               xhr.respond(200, { "Content-Type": "application/json"}, JSON.stringify(mbeansResponse));
             });
             
             // Trigger the server to respond to with our required JSON response.
             server.respond();
           }),
           
           /* This tests that the getMbeans with non wildcard searching but only partial objectname properties still returns the correct 
            * collection of mbeans. In this test we get all type=ServletStats1. This should return the App1.Servlet1 and App1.Servlet3.
            */
           tdd.test('Find All ServletStats1', function() {
    
             var dfd = this.async(1000);
             mbeanUtils.getMBeans(server, "WebSphere:type=ServletStats1").then(dfd.callback(function(response) {
                   assert.ok(response.length === 2, 
                       "There should have been 2 mbeans found, but " + response.length + " were returned.");
                   
                   var servlet1Found = false;
                   var servlet3Found = false;
                   
                   // If we've got the right number of mbeans, we need to ensure we have the correct ones.
                   // Loop through each one and store which ones we find. We then check that each of the expected ones have
                   // be found.
                   for (var responseCount = 0; responseCount < response.length; responseCount++) {
                       if (response[responseCount] === "WebSphere:type=ServletStats1,name=App1.Servlet1")
                           servlet1Found = true;
                       else if (response[responseCount] === "WebSphere:type=ServletStats1,name=App1.Servlet3")
                           servlet3Found = true;
                   }
                   
                   assert.ok(servlet1Found, 
                          "Unable to find mbean WebSphere:type=ServletStats1,name=App1.Servlet1");
                   assert.ok(servlet3Found, 
                           "Unable to find mbean WebSphere:type=ServletStats1,name=App1.Servlet3"); 
             }), dfd.reject.bind(dfd));             
             
             server.respondWith("GET", this.mbeanURLPrefix, function(xhr) {
               xhr.respond(200, { "Content-Type": "application/json"}, JSON.stringify(mbeansResponse));
             });
             
             // Trigger the server to respond to with our required JSON response.
             server.respond();
           }),
           
           /* This tests that the getMbeans wildcard searching returns the correct collection of mbeans. In this test
            * we get all type=ServletStats2* for App2. This should return the ServletStats2 and the ServletStats22.
            */
           tdd.test('Find All App2* ServletStats2*', function() {
    
               var dfd = this.async(1000);
               mbeanUtils.getMBeans(server, "WebSphere:type=ServletStats2*,name=App2*").then(dfd.callback(function(response) {
                     assert.ok(response.length === 2, 
                         "There should have been 2 mbeans found, but " + response.length + " were returned.");
                     
                     var servlet22Found = false;
                     var servlet2Found = false;
                     
                     // If we've got the right number of mbeans, we need to ensure we have the correct ones.
                     // Loop through each one and store which ones we find. We then check that each of the expected ones have
                     // be found.
                     for (var responseCount = 0; responseCount < response.length; responseCount++) {
                         if (response[responseCount] === "WebSphere:type=ServletStats22,name=App2.Servlet1")
                             servlet22Found = true;
                         else if (response[responseCount] === "WebSphere:type=ServletStats2,name=App2.Servlet3")
                             servlet2Found = true;
                     }
                     
                     assert.ok(servlet22Found, 
                            "Unable to find mbean WebSphere:type=ServletStats22,name=App2.Servlet1");
                     assert.ok(servlet2Found, 
                            "Unable to find mbean WebSphere:type=ServletStats2,name=App2.Servlet2");
               }), dfd.reject.bind(dfd));
               
               server.respondWith("GET", this.mbeanURLPrefix, function(xhr) {
                 xhr.respond(200, { "Content-Type": "application/json"}, JSON.stringify(mbeansResponse));
               });
               
               // Trigger the server to respond to with our required JSON response.
               server.respond();
             });
      });
    }
});
//@ sourceURL=unittests/resources/stats_mbeanUtilsTest.js
