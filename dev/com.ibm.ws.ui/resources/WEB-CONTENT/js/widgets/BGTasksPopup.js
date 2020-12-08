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
         "dojo/dom",
         "dojo/_base/declare", 
         "dojo/i18n!./nls/widgetsMessages",
         "dojo/window",
         "dojo/dom-attr"
         ], 
         function(_WidgetBase, dom, declare,
             i18n, window, domAttr) {
  'use strict';


  // main class to get the background tasks and show them in the tree view
  var BGTasksPopup = declare("BGTasksPopup", [_WidgetBase], {
    divID: "backgroundTaskPopupDiv", // this is the div id on the toolbox.jsp
    constructor: function() {
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
      return i18n.BGTASKS_DEPLOYMENT_INSTALLATION_POPUP;
    },

    toggleButton: function(totalInProgress, total) {
      // If there are more than 0 background tasks, un-hide the button div
      var btnDiv = dom.byId("bgTasksDetailsViewButtonDiv");
      var div = dom.byId('backgroundTaskPopupDialogContentDiv');
      var divNone = dom.byId('backgroundTaskPopupDialogNoneTaskDiv');
      var spanTitle = dom.byId('backgroundTaskPopupDialogTitleDiv');
      if ( total ) {
          btnDiv.hidden=false;
          div.hidden=false;
          spanTitle.hidden=false;
          divNone.hidden=true;
          var btn = dom.byId("bgTasksDetailsViewButton");
          if (btn) {
              btn.focus();
          }
      }
      else {
          btnDiv.hidden=true;
          div.hidden=true;
          spanTitle.hidden=true;
          divNone.hidden=false;
      }
    },
    
    showBGTasks: function(store) {
      var content = "";
      var rootNode = store.get(-1);
      for (var i=0; i < rootNode.children.length; i++) {
        content = content + rootNode.children[i].popupLabel;
      }
      var divNone = dom.byId('backgroundTaskPopupDialogNoneTaskDiv');
      divNone.hidden = true;
      var div = dom.byId('backgroundTaskPopupDialogContentDiv');
      div.hidden = false;
      div.innerHTML = content; 
      var winHeight = window.getBox().h;
      var rowHeight = 90;
      if (winHeight < rootNode.children.length * rowHeight + 158) {
        var popupHeight = Math.max(winHeight - 156, rowHeight);
        div.setAttribute("style","height:" + popupHeight + "px; overflow-y:auto;");
      } else {
        div.setAttribute("style", "height:" + rootNode.children.length * rowHeight + "px;");
      }
    },

    showNone: function() {
      var divNone = dom.byId('backgroundTaskPopupDialogNoneTaskDiv');
      divNone.hidden = false;
    }
  });
  return BGTasksPopup;
});
