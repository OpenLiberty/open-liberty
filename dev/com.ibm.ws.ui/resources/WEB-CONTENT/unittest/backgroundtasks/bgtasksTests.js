/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
     "js/toolbox/backgroundTasks"
       ],
       
   function (tdd, assert, backgroundTasks) {
     
      with(assert) {
        
        /**
         * Defines the 'toolbox' module test suite.
         */
        tdd.suite("backgroundTasks Tests", function() {

          var server;

           tdd.beforeEach(function() {
              // Mock the admin center server since it is not available in a unittest
              server = sinon.fakeServer.create();
           });
            
           tdd.afterEach(function() {
              server.restore();
           });

           tdd.test("BackgroundTasks - constructor", function() {
               var bgts = new BackgroundTasks();
               assert.isTrue(bgts instanceof BackgroundTasks, "Unable to construct BackgroundTask");
           });
           
  
//           tdd.test("BackgroundTasks.getRoot - test getRoot return 3 tasks", function() {
//               var dfd = this.async(1000);
//               var stub = sinon.stub(backgroundTasks, "addTask");
//               var stub1 = sinon.stub(backgroundTasks, "getHosts");
//               var stub2 = sinon.stub(backgroundTasks, "parseData");
//               var stub3 = sinon.stub(backgroundTasks, "resetTasks");
//               
//               backgroundTasks.getRoot("/status", "json").then(dfd.callback(function(data) {
//                  assert.equal(data, 3, "The total tasks should be 3 but returned " + data);
//               }), function(err) {
//                 dfd.reject(err);
//               });
//  
//               server.respondWith("GET", "/status/deploy",
//                       [200, { "Content-Type": "application/json" },
//                        '{ "tokens" : [1, 2, 3] }'
//                        ]);
//               server.respondWith("GET", "/status/1/results",
//                   [200, { "Content-Type": "application/json" },
//                    '{"results":[{"host":"localhost","status":"ERROR","result":"2","deployedArtifactName":"errServer"}, {"host":"host2","status":"ERROR","result":"999","stdout":"errServer","deployedArtifactName":"errServer","deployedUserDir":""}])']);
//               server.respondWith("GET", "/status/2/results",
//                   [200, { "Content-Type": "application/json" },
//                    '{"results":[{"host":"localhost","status":"FINISHED","result":"0","deployedArtifactName":"server1", "deployedUserDir":"c:\nodejs\server\wlpn"}]}']);
//               server.respondWith("GET", "/status/3/results",
//                   [200, { "Content-Type": "application/json" },
//                    '{"results":[{"host":"localhost","status":"IN_PROGRESS","result":"0","deployedArtifactName":"server2"}' +
//                    '{"host":"host2","status":"FINISHED","result":"0","stdout":"This deploy was successful.", "stderr":"No error is found","deployedArtifactName":"server2","deployedUserDir":"/home/wlpn"}])']);
//               server.respond();
//               stub.restore();
//               stub1.restore();
//               stub2.restore();
//               stub3.restore();
//               return dfd;
//           });
//  

           tdd.test("BackgroundTasks.getOverAllDeploymentStatus - test getOverAllDeploymentStatus", function() {
             var status1 = backgroundTasks.getOverAllDeploymentStatus(1, 0, 1);
             assert.equal("IN_PROGRESS", status1, "Expected to get IN_PROGRESS but return " + status1);
             var status2 = backgroundTasks.getOverAllDeploymentStatus(2, 0, 0);
             assert.equal("FINISHED", status2, "Expected to get FINISHED but return " + status2);
             var status3 = backgroundTasks.getOverAllDeploymentStatus(2, 2, 0);
             assert.equal("PARTIAL_SUCCESS", status3, "Expected to get PARTIAL_SUCCESS but return " + status3);
             var status4 = backgroundTasks.getOverAllDeploymentStatus(0, 1, 0);
             assert.equal("ERROR", status4, "Expected to get ERROR but return " + status4);
           }),
                     
           tdd.test("BackgroundTasks.parseData - test parseData", function() {
               
             var hostDetails11 = {"status": "IN_PROGRESS","result": 0,"host":"localhost","deployedArtifactName":"server1"};
             var hostDetails12 = {"status": "ERROR","result": 2,"host":"host12","stdOut": "","stdErr": "", "exception":{"message": "Host provided for the deployment was empty or null", "localizedMessage": "Host provided for the deployment was empty or null"}, "deployedArtifactName":"server1"};
             var hostDetails21 = {"status": "FINISHED","result": 0,"host":"host21","stdOut": "Deployment is done","stdErr": "No error is found","deployedArtifactName":"server2","deployedUserDir":"/home/wlp"};
             var hostDetails31 = {"status": "FINISHED","result": 0,"host":"host31","stdOut": "Deployment is done","stdErr": "No error is found","deployedArtifactName":"server3","deployedUserDir":"/home/wlp"};
             var hostDetails32 = {"status": "ERROR","result": 999,"host":"host32","stdOut": "Deployment fails unexpectedly.","stdErr": "Security chain error","deployedArtifactName":"server3","deployedUserDir":"/home/wlp"};
               
             var callback = function(){};
               var display = {
                       'IN_PROGRESS': 'Running',
                       'ERROR': 'Failed',
                       'FINISHED': 'Finished',
                       'PARTIAL_SUCCESS': 'Partial Succeeded'
                   };
  
               var label1 = "<span class='normalWhiteSpaceAlignMiddle'><span class='deployInstallationTitle'>Deploy Installation 1 - server1</span><span class='container'><span class='status'>Running</span> <span class='progressbar'><img src='images/colors/red.png' width='106' height='18' class='statusBarSingleBlock' title='Failed: 50%' alt='Failed: 50%'><img src='images/colors/transparent.png' width='108' height='18' class='repeatingGradientPending statusBarSingleBlock' title='Running: 50%' alt='Running: 50%'></span> <span class='percentage'>50%</span></span></span>";
               var label2 = "<span class='normalWhiteSpaceAlignMiddle'><span class='deployInstallationTitle'>Deploy Installation 2 - server2</span><span class='container'><span class='status'>Finished</span> <span class='progressbar'><img src='images/colors/green.png' width='216' height='18' class='statusBarSingleBlock' title='Finished: 100%' alt='Finished: 100%'></span> <span class='percentage'>100%</span></span></span>"; 
               var label3 = "<span class='normalWhiteSpaceAlignMiddle'><span class='deployInstallationTitle'>Deploy Installation 3 - server3</span><span class='container'><span class='status'>Partial Succeeded</span> <span class='progressbar'><img src='images/colors/green.png' width='106' height='18' class='statusBarSingleBlock' title='Finished: 50%' alt='Finished: 50%'><img src='images/colors/red.png' width='106' height='18' class='statusBarSingleBlock' title='Failed: 50%' alt='Failed: 50%'></span> <span class='percentage'>100%</span></span></span>";
               backgroundTasks.setParams(callback, null, display, "Deploy Installation {0} - {1}" );
               
               // adds a single task to the map
               backgroundTasks.setTotalObjects(3);
               backgroundTasks.addTask(1, "IN_PROGRESS");
               backgroundTasks.addTask(2, "FINISHED");
               backgroundTasks.addTask(3, "PARTIAL_SUCCESS");
  
            // sets the total number of hosts for the specified task
               backgroundTasks.setTotalHosts(0, 2);
               
            // sets the task name
               backgroundTasks.setTaskName(0, "server1");               
  
               backgroundTasks.addHost (0, "localhost", "IN_PROGRESS");
               backgroundTasks.addHost (0, "host11", "ERROR");
               backgroundTasks.addHostDetails(0, 0, hostDetails11);
               backgroundTasks.addHostDetails(0, 1, hostDetails12);
               
            // sets the total number of hosts for the specified task
               backgroundTasks.setTotalHosts(1, 1);
  
            // sets the task name
               backgroundTasks.setTaskName(1, "server2");  
               backgroundTasks.addHost (1, "host21", "FINISHED");
               backgroundTasks.addHostDetails(1, 0, hostDetails21);
               
               backgroundTasks.setTotalHosts(2, 2);
               backgroundTasks.setTaskName(2, "server3");  
               backgroundTasks.addHost (2, "host31", "FINISHED");
               backgroundTasks.addHost (2, "host32", "ERROR");
               backgroundTasks.addHostDetails(2, 0, hostDetails31);
               backgroundTasks.addHostDetails(2, 1, hostDetails32);
               
               backgroundTasks.parseData();
               var store = backgroundTasks.getStore();
               var item1 = store.get(-1);
               assert.isTrue(store.data instanceof Array, "returned store.data is not an array");
               assert.equal(store.data.length, 1, "returned store.data.length should be 1, but returned " + store.data.length);
               assert.equal(item1.label, "root", "root element label should be root but returned " + item1.label);
               
               var tasks = store.getChildren(item1);
               assert.isTrue(tasks instanceof Array, "returned tasks is not an array");
               assert.equal(tasks.length, 3, "returned tasks.length should be 3, but returned " + tasks.length);
               assert.equal(tasks[0].label, label3, "task 1 label should be \"" + label3 + "\" but returned " + tasks[0].label);
               assert.equal(tasks[1].label, label2, "task 2 label should be \"" + label2 + "\" but returned " + tasks[1].label);
               assert.equal(tasks[2].label, label1, "task 3 label should be \"" + label1 + "\" but returned " + tasks[1].label);
  
           });
        });
      }
});
