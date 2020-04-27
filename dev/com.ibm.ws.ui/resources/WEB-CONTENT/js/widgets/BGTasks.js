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
                    registry.byId("bgTasksTreeContainer").getRoot("/ibm/api/collective/v1/deployment", "json");
                    console.log("Run getRoot method");
                    registry.byId("toolboxContainer").performTransition("bgTasksContainer", 1, "slide");
                    console.log("Slide in the view");
                }
            })); 

Another way to launch it:

                    var _dURL = "/ibm/api/collective/v1/deployment";
                    var tsk = new Tasks("tasksDiv");
                    tsk.getRoot(_dURL, "json" );


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
define([ "dijit/_WidgetBase",
         "dojo/_base/declare",
         "dojo/_base/window",
         "dojo/dom",
         "dijit/registry",
         "dojo/i18n!./nls/widgetsMessages",
         "dojo/_base/lang", 
         "dijit/Dialog",
         'js/widgets/BGDetailsDialog',
         "dojo/dom-construct",
         "dojo/fx",
         "dojo/dom-style",
         "dojo/on"
         ], 
    function(_WidgetBase, declare, win, dom, registry, i18n, lang, Dialog, BGDetailsDialog, domConstruct, fx, domStyle, on ) {
    'use strict';


    var onClickDetails = function(store, stepNodeId, storeIdPrefix) {
        //var id = parseInt( stepNodeId.substring(storeIdPrefix.length) , 10 );
        var id = stepNodeId;
        console.log("------------- id on onClickDetails", id);
        var rootNode = store.get(-1);
        var item = null;
        for (var i=0; i < rootNode.children.length && item === null; i++) {
             if ( id === rootNode.children[i].id) {
                 item =  rootNode.children[i];
             }
             else {
                 for (var j=0; j < rootNode.children[i].children.length && item === null; j++) {
                     if ( id === rootNode.children[i].children[j].id) {
                         item =  rootNode.children[i].children[j];
                     }
                     else {
                         
                         for (var k=0; k < rootNode.children[i].children[j].children.length && item === null; k++) {
                             if ( id === rootNode.children[i].children[j].children[k].id) {
                                 item =  rootNode.children[i].children[j].children[k];
                             }
                        }
                     }
                }
             }
        }
        console.log("bgTasks.js" + registry.byId("bgTaskInfoDialog"));
        
        var infoDialog = registry.byId("bgTaskInfoDialog");
        if ( undefined === infoDialog ) {
              infoDialog = new BGDetailsDialog({
              result: item.result,
              stdOut: item.stdOut,
              stdErr: item.stdErr,
              exception: item.exception,
              deployedArtifactName: item.deployedArtifactName,
              deployedUserDir: item.deployedUserDir
          });
          
        }
        
        infoDialog.placeAt(win.body());
        infoDialog.startup();
        infoDialog.show();
        
    };
    
    var onClickHost = function(hostNodeId, hostIdPrefix, iconPrefix){
        
        var id = hostNodeId.substring(hostIdPrefix.length);
        console.log("hostNodeId=" + hostNodeId);
        console.log("id=" + id);
        var targetNode = dom.byId (id);
        var isVisible = (domStyle.get(targetNode, "display") !== "none");
        if ( isVisible ) {
            fx.wipeOut({ node: targetNode }).play();
            dom.byId(iconPrefix + hostNodeId).innerHTML = '<img class="centerImg" src="images/upIcon.png" alt="' + i18n.BGTASKS_EXPAND + '" title="' + i18n.BGTASKS_EXPAND + '">';
        }
        else {
            dom.byId(iconPrefix + hostNodeId).innerHTML = '<img class="centerImg" src="images/downIcon.png" alt="' + i18n.BGTASKS_COLLAPSE + '" title="' + i18n.BGTASKS_COLLAPSE + '">';
            fx.wipeIn({ node: targetNode }).play();
        }
    };

    var onClickTask = function(taskNodeId, taskIdPrefix){
        var id = taskNodeId.substring(taskIdPrefix.length);
        console.log("taskNodeId=" + taskNodeId);
        console.log("id=" + id);
        var targetNode = dom.byId (id);
        var isVisible = (domStyle.get(targetNode, "display") !== "none");
        if ( isVisible ) {
            fx.wipeOut({ node: targetNode }).play();
        }
        else {
            fx.wipeIn({ node: targetNode }).play();        
        }
    };
     // main class to get the background tasks and show them in the tree view
    var BGTasks = declare("BGTasks", [_WidgetBase], {
        divID: "bgTasksTreeView", // this is the div id on the toolbox.jsp
        postCreate: function() {
          this.set("aria-label", i18n.BGTASKS_DEPLOYMENT_INSTALLATION);
        },
        
        getStatusDisplay:function(){
             var display = {
                    'IN_PROGRESS': i18n.BGTASKS_STATUS_RUNNING,
                    'ERROR': i18n.BGTASKS_STATUS_FAILED,
                    'FINISHED': i18n.BGTASKS_STATUS_SUCCEEDED,
                    "PARTIAL_SUCCESS": i18n.BGTASKS_STATUS_WARNING
                };
            return display;
        },
        
        getTitle: function() {
            return i18n.BGTASKS_DEPLOYMENT_INSTALLATION;
        },
        
        showBGTasks: function(store) {
            var taskIdPrefix = "task-";
            var taskDetailPrefix = "taskDetail-";
            var hostIdPrefix = "host-";
            var iconPrefix = "icon-";
            var hostDetialPrefix = "hostDetail-";
            var storeIdPrefix = "storeId-";
            var iconMap = {
                    'IN_PROGRESS': '<img class="centerImg" src="images/status-icon--in-progress--T.png" title="'+ i18n.BGTASKS_STATUS_RUNNING +'" alt="'+ i18n.BGTASKS_STATUS_RUNNING +'">',
                    'ERROR': '<img class="centerImg" src="images/status-icon--error--T.png" title="'+ i18n.BGTASKS_STATUS_FAILED +'" alt="'+ i18n.BGTASKS_STATUS_FAILED +'">',
                    'FINISHED': '<img class="centerImg" src="images/status-icon--success--T.png" title="'+ i18n.BGTASKS_STATUS_SUCCEEDED +'" alt="'+ i18n.BGTASKS_STATUS_SUCCEEDED +'">',
                    'PARTIAL_SUCCESS': '<img class="centerImg" src="images/status-icon--warning--T.png" title="'+ i18n.BGTASKS_STATUS_WARNING +'" alt="'+ i18n.BGTASKS_STATUS_WARNING +'">'
            };
            domConstruct.empty(this.divID);
            var rootNode = store.get(-1);
            console.log("rootNode", rootNode);
            for (var i=0; i < rootNode.children.length; i++) {
                var taskDetailNodeId = taskDetailPrefix + rootNode.children[i].id;
                var taskNodeId = taskIdPrefix + taskDetailNodeId+"-"+i;
                
                taskDetailNodeId = taskDetailPrefix + rootNode.children[i].id;
                var hostsNode = domConstruct.toDom('<div id="' + taskDetailNodeId+"-"+i +'"  style="display:none" class="arrowDivBox"></div>');
                
                //loop through each host
                for ( var j=0; j< rootNode.children[i].children.length; j++ ) {
                    var parentHostNode = domConstruct.toDom("<div></div>");
                    var hostNode = domConstruct.toDom('<div tabindex=\"0\" class="clickableLine bgLinePadding" style="display: inline-block;vertical-align: middle;width: calc( 100% - 30px);" id="' +
                            hostIdPrefix + hostDetialPrefix + rootNode.children[i].children[j].label + "-" + i + "-" + j + '">' +
                            '<span class="childStatusIcon" id="' + iconPrefix + hostIdPrefix + hostDetialPrefix + rootNode.children[i].children[j].label  + "-"  + i + "-" + j +
                            '">' +
                            //'<img class="centerImg" src="images/upIcon.png" alt="' + i18n.BGTASKS_EXPAND + '" title="' + i18n.BGTASKS_EXPAND + '">' +
                            '</span><span class="childStatusIcon">' +
                            iconMap[rootNode.children[i].children[j].status] + '</span>' +
                            '<span class="description" style="width: calc( 100% - 83px ) !important;">' +
                            rootNode.children[i].children[j].label + '</span>' +
                            '</div>');
                    domConstruct.place(parentHostNode, hostsNode);
                    domConstruct.place(hostNode, parentHostNode);       
                    
                    // details for each host
//                    var detailNode = domConstruct.toDom('<div style="display:none;margin-top:-10px" id="' + hostDetialPrefix + rootNode.children[i].children[j].label  + "-"  + i + "-" + j + '"></div>');
//                    for ( var k=0; k< rootNode.children[i].children[j].children.length; k++) {
//                        var stepNode = domConstruct.toDom('<div id="' + storeIdPrefix + rootNode.children[i].children[j].children[k].id + '"><div tabindex=\"0\" class="bgLinePadding"><span class="childStatusIcon"></span><span class="childStatusIcon"></span>' +
//                                rootNode.children[i].children[j].children[k].label +
//                                '</div></div>');
//                        domConstruct.place(stepNode, detailNode);
//                        
//                        on(stepNode, "click", lang.hitch(this, onClickDetails, store, stepNode.id, storeIdPrefix));
//
//                    }
//                    domConstruct.place(detailNode, parentHostNode);
//                    
//                    on(hostNode, "click", lang.hitch(this, onClickHost, hostNode.id, hostIdPrefix, iconPrefix));
                    on(hostNode, "click", lang.hitch(this, onClickDetails, store, rootNode.children[i].children[j].children[0].id, storeIdPrefix));
                }
                var parentTaskNode = domConstruct.toDom("<div></div>");
                var overAllStatus = rootNode.children[i].status;   
                var taskLevelIcon = iconMap.FINISHED;
                if (overAllStatus === "IN_PROGRESS") {
                  taskLevelIcon = iconMap.IN_PROGRESS;
                } else if (overAllStatus === "ERROR") {
                  taskLevelIcon = iconMap.ERROR;
                } else if (overAllStatus === "PARTIAL_SUCCESS") {
                  taskLevelIcon = iconMap.PARTIAL_SUCCESS;
                }

                var taskNode = domConstruct.toDom('<div tabindex=\"0\" class="clickableLine bgLinePadding" id="' +taskNodeId+ '">' +
                        '<span class="rootStatusIcon">' +
                        taskLevelIcon + '</span>' +
                        rootNode.children[i].label +
                        '</div>');
                
                domConstruct.place(parentTaskNode, this.divID);
                domConstruct.place(taskNode, parentTaskNode);
                domConstruct.place(hostsNode, parentTaskNode);
                
                on(taskNode, "click", lang.hitch(this, onClickTask, taskNode.id, taskIdPrefix));
            }
        }        
    });
    return BGTasks;
});
