/*
 * This this the example of the object store model:
 * [
 *  {
        id: 0,
        label: "Deployment task 1 - a.zip",
        status: "failed",
        progress: 0;
        children:
        [
            {
                id: 1,
                label: "localhost",
                status: "failed",
                children:
                [
                    {
                        id: 2,
                        label: "<span style='white-space:normal'><span style='width:50%; text-overflow: ellipsis'>Transferring and extracting archive</span><div class='timestamp'>02/20/2014  5:21PM</div></span>",
                        status: "failed",
                    },
                    {
                        id: 3,
                        label: "<span style='white-space:normal'>Join servers to the collective<div class='timestamp'>02/20/2014  5:24PM</div></span>",
                        status: "inProgress",
                    }
                ],
            },
        ]
    }
  ] 
*/

/*
This is how I enable it in the LibertyHeader.js:

find the section to add the bookmark drop down, then add the following:

            
            //Temporary entry point to background tasks page - start
             
                addToolMenu.addChild(new MenuItem({
                label: i18n.BGTASKS_PAGE_LABEL,
                id: "toolboxShowBGTasksMenu",
                onClick: function(){
                    // launch the catalog in add mode
                    registry.byId("catalog_headerWidget").createToolHeading("not used", "bgTasksContainer", "toolboxContainer");
                    console.log("Created tool heading");
                    var _dURL = "/ibm/api/collective/v1/deployment";
                    // first param is the call back function, once all ansync calls are finished, the showBGTasks tree will be displayed
                    backgroundTasks.setParams(registry.byId("bgTasksTreeContainer").showBGTasks,registry.byId("bgTasksTreeContainer").getStatusDisplay(),registry.byId("bgTasksTreeContainer").getTitle() );
                    console.log("Run getRoot method");
                    backgroundTasks.getRoot(_dURL, "json" );
                    registry.byId("toolboxContainer").performTransition("bgTasksContainer", 1, "slide");
                    console.log("Slide in the view");
                }
            })); 


This widget requires the deploymentResult.css, which should be added into the toolbox.jsp.

The following sections are required in toolbox.jsp:

I put this after catalogContainer:

         <div id='bgTasksContainer' data-dojo-type="dojox.mobile.View" data-dojo-props=" 
             style: 'width: 100%; height: 100%; margin: auto;'">
          <div data-dojo-type="js/widgets/LibertyHeader" id="bgTasks_headerWidget" containerId="bgTasksContainer" userName="<%=userId%>"> 
          </div>  <!-- end of header -->
             <div data-dojo-type="js/widgets/BGTasks" id="bgTasksTreeContainer"></div>
             <div id="bgTasksTreeView"></div> 
          </div>  <!-- end of contentPane -->

*/
define([ "dojo/_base/declare", 
         "dojo/request/xhr", 
         "dojo/Deferred", 
         "dojo/_base/lang",
         "dojo/store/Memory",
         "dojo/date/locale"
         ], 
    function(declare, xhr, Deferred, lang, Memory, locale) {
    'use strict';

    // maps to the rest api status code
    var msgFailed = "ERROR";
    var msgSucceeded = "FINISHED";
    var msgInProgress = "IN_PROGRESS";
    var msgPartialSuccess = "PARTIAL_SUCCESS";
    
    // status order function, determine which status shows first
    
    var status_order = function(a){
        if ( a === msgInProgress) {
            return 3;
        }
        else if ( a === msgFailed) {
            return 2;
        }
        else if ( a === msgPartialSuccess) {
          return 1;
      }
        else if ( a === msgSucceeded) {
            return 0;
        }
    };

    // sort function, it will sort the array using field1 first, if field1 is the same, the check field2
    var sort_by = function(field1, field2, reverse1, reverse2, primer1, primer2){
        var key1 = function (x) {return primer1 ? primer1(x[field1]) : x[field1];};
        var key2 = function (x) {return primer2 ? primer2(x[field2]) : x[field2];};

        return function (a,b) {
           var A = key1(a), B = key1(b);
           if ( A<B ) {
               if ( reverse1 ){
                   return 1;
               }
               else {
                   return -1;
               }
           }
           if ( A>B ) {
               if ( reverse1 ){
                   return -1;
               }
               else {
                   return 1;
               }
           }
           else {
               var AA = key2(a), BB = key2(b);
               if ( AA<BB ) {
                   if ( reverse2 ){
                       return 1;
                   }
                   else {
                       return -1;
                   }
               }
               if ( AA>BB ) {
                   if ( reverse2 ){
                       return -1;
                   }
                   else {
                       return 1;
                   }
               }
               else {
                   return 0;
               }
           }
        };
     };

     // get upper case function
     var upper_case = function(a){
         return a.toUpperCase();
     }; 

     // main class to get the background tasks and show them in the tree view
    var BackgroundTasks = declare("BackgroundTasks", null, {
        totalObjects : 0, // holds how many tasks in the treeMap
        total: 0, // holds how many in total including failed xhr call

        treeMap: [], // holds the tasks 
        statusDisplay: [], // array to quickly locate the translated labels
        store: {},
        divID: "bgTasksTreeView", // this is the div id on the toolbox.jsp
        progressBarWidthinPixes: 2.16, // in hundreds, 2.16 means progress bar width from edge to edge is 216 pixes.
        isProcessFinished: false,
        deployURI: "/deploy",
        statusURI: "/status",
        resultsURI: "/results",

        callback: null,
        callbackNone: null,
        constructor: function(){
            
        },
        setParams: function(cbk, cbkNone, statusDisplayObj, title, barPixels) {
            this.callback = cbk;
            this.callbackNone = cbkNone;
            this.statusDisplay = statusDisplayObj;
            this.deploymentInstallation = title;
            if(typeof(barPixels)==='undefined') {
                this.progressBarWidthinPixes = 2.16;
            }
            else {
                this.progressBarWidthinPixes = barPixels/100;
            }
        },
        
        resetTasks: function() {
            this.treeMap = [];
            this.isProcessFinished = true;
        },
        // adds a single task to the map
        addTask: function(taskID, taskStatus) {
            var length = this.treeMap.length;
            var obj = {};
            obj.id = taskID;
            obj.taskStatus = taskStatus;
            obj.hosts = [];
            obj.totalHosts = -1;
            this.treeMap.push(obj);
            return length;
        },
        // sets the total number of hosts for the specified task
        setTotalHosts: function (index, total) {
            this.treeMap[index].totalHosts = total;  
        },
        
        // adds a host to the specific task
        addHost: function (index, hostName, hostStatus){
            var obj = {};
            obj.hostName = hostName;
            obj.hostStatus = hostStatus;
            
            var length = this.treeMap[index].hosts.length;
            this.treeMap[index].hosts.push(obj);
            return length;
        },
        
        // adds the host details (steps etc) to the specified task
        addHostDetails: function (index, hostIndex, data){
            this.treeMap[index].hosts[hostIndex].details = data;
        },
        
        // sets the task name
        setTaskName: function(index, name) {
            this.treeMap[index].name = name;
        },
        
        // helper method for unit test
        setTotalObjects: function (a) {
            this.totalObjects = a;
        },
        
        // helper method for unit test
        getStore: function(){
            return this.store;
        },

        // the main function to convert the restapi data to the memory store model for tree view display.
        // this method will be called multiple time, however it won't do anything if the all hosts details are not loaded
        parseData: function() {
            // in case of lighting fast network speed, this method can be called many times after all ansync calls are returned,
            // thus this make sure the tree are not registered more than once.
            
            if ( this.isProcessFinished === true ) {
                console.log("already processed, quit");
                return;
            }
                
            // make sure all hosts are loaded
            if ( this.totalObjects >=1 ) {
                
                this.store = new Memory({
                    data: [
                           {
                               id: -1,
                               label: "root",
                               children: []
                           }
                         ],
                         
                         getChildren: function(object){
                               return object.children || [];
                        }
                        
                });

                // starting id, id is required for memory store.
                var id = 10;
                
                 // make sure all calls are finished
                console.log("checking.....", this.treeMap);
                for (var i=0; i<this.totalObjects;i++) {
                    if ( this.treeMap[i].totalHosts < 1 ||
                            this.treeMap[i].hasOwnProperty('taskStatus') === false ||
                            this.treeMap[i].hasOwnProperty('name') === false) {
                        console.log(" totalHosts < 1, taskStatus is not set or name property is not set, quit loop");
                        return;
                    }
                    if (this.treeMap[i].hasOwnProperty('hosts') === false) {
                        console.log(" no hosts property, quit loop");
                        return;
                    }
                    if (this.treeMap[i].hosts.length !== this.treeMap[i].totalHosts) {
                        console.log(" hosts are not fully loaded, quit loop");
                        return;
                    }
                     
                    for (var b=0; b<this.treeMap[i].totalHosts;b++) {
                        if (this.treeMap[i].hosts[b].hasOwnProperty('details') === false) {
                            console.log(" no details property, quit loop");
                            return;
                        }
                        
                    }
                }
                var tasksArray = [];
                // we now should have all data ready for us to display the tree.
                for (i=0; i<this.totalObjects;i++) {
                    var totalRunning = 0;
                    var totalSucceeded = 0;
                    var totalFailed = 0;
                    var task = {};
                    task.id = ++id; // assign as root
                    task.children = [];

                    for (var j=0; j<this.treeMap[i].totalHosts;j++) {
                        var host = {};
                        host.id = ++id;
                        host.children = [];
                        host.label = this.treeMap[i].hosts[j].hostName;
                        host.status = this.treeMap[i].hosts[j].hostStatus;
                        if ( host.status === msgInProgress) {
                            totalRunning++;
                        }
                        else if ( host.status === msgFailed) {
                            totalFailed++;
                        }
                        else {
                            totalSucceeded++;
                        }

                        var hostDetail = this.treeMap[i].hosts[j].details;
                        var detail = {};
                        detail.id = ++id;
                        
                        var msg = "";
                        if (hostDetail.status === "ERROR") {
                          if (hostDetail.stdErr !== undefined && hostDetail.stdErr !== "") {
                            msg = hostDetail.stdErr;
                          } else {
                            msg = hostDetail.result;
                          }
                        } else if (hostDetail.status === "FINISHED") {
                          msg = hostDetail.result;
                        }
                        detail.label = "<span class='normalWhiteSpace'><span class='description'>" + msg + " " + "</span></span>";
                        detail.status = hostDetail.status;
                        // initialize all properties to empty string as detail dialog will choke on undefined
                        detail.result = "";
                        // since result could be 0, has to check for undefined
                        if (hostDetail.result !== undefined) {
                          detail.result = hostDetail.result;
                        }
                        detail.stdOut = "";
                        if (hostDetail.stdout) {
                          detail.stdOut = hostDetail.stdout;
                        }
                        detail.stdErr = "";
                        if (hostDetail.stderr) {
                          detail.stdErr = hostDetail.stderr;
                        }
                        detail.exception = "";
                        if (hostDetail.exception) {
                          if (hostDetail.exception.localizedMessage) {
                            detail.exception = hostDetail.exception.localizedMessage;
                            detail.label = "<span class='normalWhiteSpace'><span class='description'>" + detail.exception + " " + "</span></span>";
                          } else if (hostDetail.exception.message) {
                            detail.exception = hostDetail.exception.message;
                            detail.label = "<span class='normalWhiteSpace'><span class='description'>" + detail.exception + " " + "</span></span>";
                          }
                        }
                        detail.deployedArtifactName = "";
                        if (hostDetail.deployedArtifactName) {
                          detail.deployedArtifactName = hostDetail.deployedArtifactName;
                        }
                        detail.deployedUserDir = "";
                        if (hostDetail.deployedUserDir) {
                          detail.deployedUserDir = hostDetail.deployedUserDir;
                        }
                        host.children.push(detail);
                        task.children.push(host);
                    }
                   
                    // get percentage
                    var r = Math.round(totalRunning / this.treeMap[i].totalHosts * 100);
                    var f = Math.round(totalFailed / this.treeMap[i].totalHosts * 100);
                    var s = Math.round(totalSucceeded / this.treeMap[i].totalHosts * 100);
                    var c = 100 - r;
                    
                    // total width of the progress bar is 216 pixels, the bar will be displayed in the following order: Succeeded, warning, failed, pending and running.
                    // margin is always 2pixels.
                    // also each bar has 1 pixel border at each side, so total 2 pixels
                    var succeeded = "";
                    var failed = "";
                    var running = "";
                    
                    var numOfMargins=-1;
                    if ( s !== 0 ) {
                        numOfMargins ++;
                    }
                    if ( f !== 0 ) {
                        numOfMargins ++;
                    }
                    if ( r !== 0 ) {
                        numOfMargins ++;
                    }
                    
                    var totalPixels = this.progressBarWidthinPixes - (0.04 * numOfMargins); // in hundreds
                    var sWidth = Math.round(s*totalPixels);
                    var fWidth = Math.round(f*totalPixels);
                    var rWidth = Math.round(r*totalPixels);
                    var pCSSOverride = "";
                    var pWidthOverride = 0;
                    var rWidthOverride = 0;

                    if (r!== 0) {
                        rWidthOverride = 2;
                    }
                        
                    if ( s!== 0) {
                      sWidth = totalPixels * 100 - ( fWidth + rWidth);
                        succeeded = "<img src='images/colors/green.png' width='" + sWidth + "' height='18' class='statusBarSingleBlock' title='" + this.statusDisplay.FINISHED +": "+ Math.round(s) +"%' alt='" + this.statusDisplay.FINISHED +": "+ Math.round(s) +"%'>";
                    }
                                         
                    if ( f!== 0) {
                      fWidth = totalPixels * 100 - ( sWidth + rWidth);
                        failed = "<img src='images/colors/red.png' width='" + fWidth + "' height='18' class='statusBarSingleBlock' title='" + this.statusDisplay.ERROR +": "+ Math.round(f) +"%' alt='" + this.statusDisplay.ERROR +": "+ Math.round(f) +"%'>";
                    }
                    
                    if ( r!== 0) {
                        rWidth = totalPixels * 100 - ( fWidth + sWidth) ;
                        rWidthOverride = rWidth + rWidthOverride; 
                        running = "<img src='images/colors/transparent.png' width='" + rWidthOverride + "' height='18' class='repeatingGradientPending statusBarSingleBlock' title='" + this.statusDisplay.IN_PROGRESS +": "+ Math.round(r) +"%' alt='" + this.statusDisplay.IN_PROGRESS +": "+ Math.round(r) +"%'>";
                    }
                    
                    // create title to use for sorting
                    task.title = lang.replace(this.deploymentInstallation, [this.treeMap[i].id, this.treeMap[i].name]);
                    //task.title = deploymentInstallation + " - " + this.treeMap[i].name;
                    task.label = "<span class='normalWhiteSpaceAlignMiddle'><span class='deployInstallationTitle'>" + task.title + "</span><span class='container'>" +
                    "<span class='status'>" + this.statusDisplay[this.treeMap[i].taskStatus] + "</span> <span class='progressbar'>"+  succeeded + failed + running + "</span> <span class='percentage'>" + c +"%" + "</span></span></span>";                    
                    task.popupLabel = "<hr><table class='backgroundTaskPopupTable'><tr><td><span class='filename backgroundTaskPopupTableText'>" + task.title + "</span>" +  
                    "<tr><td><span class='progressbarPopup'>"+  succeeded + failed + running + "</span></td></tr>" +
                    "</span><span class='percentage backgroundTaskPopupTableText'>" + c +"%" + "</span></td></tr></table>";
                    
                    task.status = this.treeMap[i].taskStatus;
                    tasksArray.push(task);
                }
                
                // still need to sort by the title as each result comes back async
                tasksArray.sort(sort_by('title', 'status', true, false, upper_case, upper_case));
                for ( i=0; i<tasksArray.length;i++){
                    this.store.get(-1).children.push(tasksArray[i]);
                }
                
                this.resetTasks();
                
                if (this.callback && typeof(this.callback) === "function") {
                    this.callback(this.store);
                }
            }
                
        },

        getHosts : function (results, index) {
          var totalHosts = 0;
          for (var i = 0; i < results.length; i++) {
            var result = results[i];
            if (result.hasOwnProperty("host")) {
              totalHosts++;
              var hostIndex = this.addHost(index, result.host, result.status);
              this.addHostDetails(index, hostIndex, result);
            }
            if (result.hasOwnProperty("deployedArtifactName")) {
              this.setTaskName(index, result.deployedArtifactName); 
            }
          }
          this.setTotalHosts(index, totalHosts);
        },
        
        getHostsFromStatus: function(status, index) {
          var totalHosts = 0;
          for (var property in status) {
            if (status.hasOwnProperty(property)) {
              var hostStatus = status[property];
              var host = property;
              totalHosts++;
              var hostIndex = this.addHost(index, host, hostStatus);
              var hostFakeResult = {
                  status: hostStatus,
                  result: -1
              };
              this.addHostDetails(index, hostIndex, hostFakeResult);
            }
          }
          this.setTotalHosts(index, totalHosts);
          this.setTaskName(index, "fakeName"); // won't be used but has to set it to pass parseData verification
        },        

        // Calculate the overall task status.
        // If a host is still in progress, the entire deployment is flagged as in progress.
        // If there are error and finished (but no in progress) status for the hosts, the entire deployment
        //    is flagged as partial success.
        // Otherwise, the deployment will tag as failed or succeeded.
        getOverAllDeploymentStatus: function(finished, error, inProgress) {
            var overAllStatus = msgSucceeded;
            if (inProgress > 0) {
              overAllStatus = msgInProgress;
            } else if (error > 0 && finished === 0) {
              overAllStatus = msgFailed;
            } else if (error > 0 && finished > 0) {
              overAllStatus = msgPartialSuccess;
            }
            return overAllStatus;
        },
        
        // ansync call to get the background tasks       
        getDeploymentStatus: function(rootURL, token, expectedTotal, cbk, parentDeferred) {
          var xhrArgs = {handleAs: 'json'};
          var xhrDef = xhr.get(rootURL + "/" + token + this.statusURI, xhrArgs);

          // Establish the Deferred to be returned.
          // This allows the caller to cancel the underlying XHR request.
          var deferred = new Deferred(function cancelXHRDeferred(reason) {
            xhrDef.cancel(reason);
          });

          xhrDef.then(lang.hitch(this, function(data) {
//            // testing
//            if (token === 3) {
//              data.status["host3"] = "FINISHED";
//              data.status["host2"] = "IN_PROGRESS";
//              console.log("--------- fake host3", data);
//            }
            // end testing

            this.total++;
            var inProgressCount = 0;
            var errorCount = 0;
            var finishedCount = 0;

            if (data.hasOwnProperty("status")) {
              var status = data.status;
             
              for (var property in status) {
                if (status.hasOwnProperty(property)) {
                  var hostStatus = status[property];
                  if (hostStatus === "IN_PROGRESS") {
                    inProgressCount++;
                  } else if (hostStatus === "ERROR") {
                    errorCount++;
                  } else if (hostStatus === "FINISHED") {                  
                    finishedCount++;
                  }
                }
              }

              var overAllStatus = this.getOverAllDeploymentStatus(finishedCount, errorCount, inProgressCount);
              if (overAllStatus === msgInProgress) {
                this.totalObjects++;
                var index = this.addTask(token, overAllStatus);  
                this.getHostsFromStatus(status, index);
              }
             
              deferred.resolve(overAllStatus, true);

              if (expectedTotal === this.total) {
                // all are accounted for
                this.parseData();
                parentDeferred.resolve(expectedTotal, true);
                if (cbk && typeof(cbk) === "function") {
                  cbk(this.totalObjects, this.total);
                }
              }
            }
          }), function(err) {
            this.total++;
            deferred.reject(err, true);
          }, function(evt) {
            deferred.progress(evt, true);
          });
          return deferred;
        },
      
        getActiveTaskCount: function(inURL, inType, cbk ) {
            this.total = 0;
            this.totalObjects = 0;
            this.isProcessFinished = false;
            var xhrArgs = {handleAs: inType};
            var xhrDef = xhr.get(inURL + this.deployURI, xhrArgs);

            // Establish the Deferred to be returned.
            // This allows the caller to cancel the underlying XHR request.
            var deferred = new Deferred(function cancelXHRDeferred(reason) {
              xhrDef.cancel(reason);
            });

            xhrDef.then(lang.hitch(this, function(data) {
                if (data.hasOwnProperty("tokens")) {
                  if (data.tokens.length === 0) {
                      if (this.callbackNone && typeof(this.callbackNone) === "function") {
                          this.callbackNone();
                      }
                      deferred.resolve(this.total, true);
                  }
                  else {                        
                      for (var i = 0; i < data.tokens.length; i++) {
                        // the compiler will not let me have a function within a for loop. Has to put all logic in getDeploymentStatus.
                        this.getDeploymentStatus(inURL, data.tokens[i], data.tokens.length, cbk, deferred);
                      }
                  } 
                }
                
            }), function(err) {
              deferred.reject(err, true);
            }, function(evt) {
              deferred.progress(evt, true);
            });
            return deferred;
        },
        
        getDeploymentResult: function(rootURL, token, expectedTotal, parentDeferred) {
          var xhrArgs = {handleAs: 'json'};
          var xhrDef = xhr.get(rootURL + "/" + token + this.resultsURI, xhrArgs);

          // Establish the Deferred to be returned.
          // This allows the caller to cancel the underlying XHR request.
          var deferred = new Deferred(function cancelXHRDeferred(reason) {
            xhrDef.cancel(reason);
          });

          xhrDef.then(lang.hitch(this, function(data) {
            var deployStatus = "FINISHED";
            this.total++;
            var inProgressCount = 0;
            var errorCount = 0;
            var finishedCount = 0;

            if (data.hasOwnProperty("results")) {
              var results = data.results;
              // go thru each host result to figure out the overall status
              for (var i = 0; i < results.length; i++) {
                var result = results[i];
                // testing
//                if (result.host === "host3") {
//                  result.status = "FINISHED";
//                  console.log("--------- fake host3 in deploymentresult", result);
//                }
//                if (result.host === "host2") {
//                  result.status = "IN_PROGRESS";
//                }
                // end testing

                if (result.status === "IN_PROGRESS") {
                  inProgressCount++;
                } else if (result.status === "ERROR") {
                  errorCount++;
                } else if (result.status === "FINISHED") {                  
                  finishedCount++;
                }                  
              }

              var overAllStatus = this.getOverAllDeploymentStatus(finishedCount, errorCount, inProgressCount);
              var index = this.addTask(token, overAllStatus);  
              this.getHosts(results, index);
              this.totalObjects++;

              deferred.resolve(true);
              if (expectedTotal === this.total) {
                // we're done getting all the results including failed xhr calls
                this.parseData();

                parentDeferred.resolve(this.totalObjects, true);
              }
            }
          }), function(err) {
            this.total++;
            deferred.reject(err, true);
          }, function(evt) {
            deferred.progress(evt, true);
          });
          return deferred;
        },
             
        getRoot:function(inURL, inType) {
            this.totalObjects = 0;
            this.total = 0;
            var xhrArgs = {handleAs: inType};
            var xhrDef = xhr.get(inURL + this.deployURI, xhrArgs);
            this.isProcessFinished = false;

            // Establish the Deferred to be returned.
            // This allows the caller to cancel the underlying XHR request.
            var deferred = new Deferred(function cancelXHRDeferred(reason) {
              xhrDef.cancel(reason);
            });

            xhrDef.then(lang.hitch(this, function(data) {
                if (data.hasOwnProperty("tokens")) {
                  if (data.tokens.length === 0) {
                      if (this.callbackNone && typeof(this.callbackNone) === "function") {
                          this.callbackNone();
                      }
                  } else {
                    // do it in reverse order so as to have the newest deploy results first
                    for (var i = data.tokens.length -1; i >= 0; i--) {
                      // the compiler will not let me have a function within a for loop. Has to put all logic in getDeploymentResult.
                      this.getDeploymentResult(inURL, data.tokens[i], data.tokens.length, deferred);
                    }
                  }
                }
                
            }), function(err) {
              deferred.reject(err, true);
            }, function(evt) {
              deferred.progress(evt, true);
            });
            return deferred;
        }
        
    });

    return new BackgroundTasks();
});
