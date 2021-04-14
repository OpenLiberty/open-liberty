/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
define(['dojo/_base/array', 'dojo/_base/declare', 'dojo/_base/lang',
        'dojo/store/Memory',
        'dojo/aspect', 'dojo/dom-class', 'dojo/dom-style', 'dojo/keys', 'dojo/on', 'dojo/query',
        'dijit/_WidgetBase',
        'dijit/form/Button',
        'dijit/registry',
        'dojox/string/BidiComplex',
        'gridx/Grid',
        'gridx/core/model/cache/Sync',
        'gridx/core/model/extensions/Sort',
        'gridx/modules/CellWidget',
        'gridx/modules/ColumnResizer',
        'gridx/modules/Focus',
        'jsShared/grid/modules/HiddenColumns',
        'jsShared/grid/modules/GridStatePersist',
        'jsBatch/widgets/grid/modules/ExecutionGridColumnSelectionHeaderMenu',
        'jsShared/utils/imgUtils',
        'jsShared/utils/utils',
        'jsBatch/utils/ID',
        'jsBatch/utils/linkToExploreUtils',
        'jsBatch/utils/restUtils',
        'jsBatch/utils/utils',
        'jsBatch/utils/viewToHash',
        'jsBatch/widgets/TextWithImage',
        'dojo/i18n!jsBatch/nls/javaBatchMessages'
        ],
function( array, declare, lang, MemoryStore,
          aspect, domClass, domStyle, keys, on, query,
          _WidgetBase, Button, registry,
          BidiComplex,
          Grid, Cache, Sort,
          CellWidget, ColumnResizer, Focus, HiddenColumns,
          GridStatePersist, ExecutionGridColumnSelectionHeaderMenu,
          imgUtils, sharedUtils, ID, linkUtils, restUtils, utils, viewToHash,
          TextWithImage,
          i18n){

  'use strict';

  var exectionGrid = declare("executionGrid", [_WidgetBase], {
    executionStoreData : null,
    executionGrid : null,
    persistedData : null,
    instanceId : null,


    constructor : function(persistedData, instanceId) {
      this.persistedData = persistedData;
      this.instanceId = instanceId;
    },

    postCreate : function(){
      this.executionStoreData = new MemoryStore({
        idProperty: ID.EXECUTION_ID,
        data: [ ]
      });

      /* Additional Grid Structure properties added to support ColumnSelectionHeaderMenu:
       * hide -   true indicates the column should be hidden.
       * hideable - true indicates this column can be selected/unselected to be hidden;
       *          false indicates the column is ALWAYS displayed on the grid.
       * columnSelectionMenu - true indicates this column should hold the gear icon
       */
      var executionInstanceLayout = [ {
        id: ID.EXECUTION_ID,
        field: 'executionId',
        name: i18n.EXECUTION_ID,
        width: '5%',
        widgetsInCell : true,
        decorator : function() {
          return "<div class='javaBatchGridTitleText'><span data-dojo-attach-point='executionId'></span></div>";
        },
        setCellValue : function(idVal, storeVal, cellWidget) {
          var titleMsg = lang.replace(i18n.VIEW_EXECUTION_DETAILS, [idVal]);
          this.executionId.innerHTML = '<a title="' + titleMsg + '">' + idVal + '</a>';
          if (cellWidget.executionId._cnnt) {
            // Cell Widgets are reused among different rows.  Remove previously
            // connected events to avoid memory leak.
            cellWidget.executionId._cnnt.remove();
          }
          this.executionId._cnnt = on(this.executionId, 'click', lang.hitch(cellWidget.cell, function() {
            viewToHash.updateView("jobexecutions/" + this.row.id);
            return true;
          }));
        },
        hideable: false
      }, {
        id: ID.BATCH_STATUS,
        field: 'batchStatus',
        name: i18n.BATCH_STATUS,
        width: '8%',
        widgetsInCell: true,
        decorator: function() {
          return "<div class='javaBatchGridTitleText'><span data-dojo-attach-point='state'></span></div>";
        },
        setCellValue: function(stateVal, storeVal, cellWidget) {
          // Create the ID for the State field for test purposes
          var columnId = cellWidget.cell.column.id;
          var rowId = cellWidget.cell.row.id;
          var cellDataId = "exeutionGrid-" + columnId + '-' + rowId;
          var cellValue = new TextWithImage ({
              type: 'status',
              value: stateVal,
              divId: cellDataId
            });
            this.state.innerHTML = cellValue.domNode.innerHTML;
        },
        hideable: true
      }, {
        id: ID.EXIT_STATUS,
        field: 'exitStatus',
        name: i18n.EXIT_STATUS,
        width: '10%',
        widgetsInCell: true,
        decorator: function() {
          return "<div class='javaBatchGridTitleText'><span data-dojo-attach-point='status'></span></div>";
        },
        setCellValue: function(status) {
          this.status.innerHTML = "<span title='" + status + "' dir='" + sharedUtils.getStringTextDirection(status) + "'>" + status + '</span>';
        },
        hideable: true
      }, {
        id: ID.CREATE_TIME,
        field: 'createTime',
        name: i18n.CREATE_TIME,
        width: '8%',
        widgetsInCell: true,
        decorator: function() {
          return "<div class='javaBatchGridTitleText'><span data-dojo-attach-point='dateTime'></span></div>";
        },
        setCellValue: function(dateValue) {
          var htmlForDate = utils.formatDate(dateValue);
          this.dateTime.innerHTML = htmlForDate;
        },
        hide: true,
        hideable: true
      }, {
        id: ID.START_TIME,
        field: 'startTime',
        name: i18n.START_TIME,
        width: '8%',
        widgetsInCell: true,
        decorator: function() {
          return "<div class='javaBatchGridTitleText'><span data-dojo-attach-point='dateTime'></span></div>";
        },
        setCellValue: function(dateValue) {
          var htmlForDate = utils.formatDate(dateValue);
          this.dateTime.innerHTML = htmlForDate;
        },
        hide: true,
        hideable: true
     }, {
        id: ID.END_TIME,
        field: 'endTime',
        name: i18n.END_TIME,
        width: '8%',
        widgetsInCell: true,
        decorator: function() {
          return "<div class='javaBatchGridTitleText'><span data-dojo-attach-point='dateTime'></span></div>";
        },
        setCellValue: function(dateValue) {
          var htmlForDate = utils.formatDate(dateValue);
          this.dateTime.innerHTML = htmlForDate;
        },
        hide: true,
        hideable: true
      }, {
        id: ID.LAST_UPDATE,
        field: 'lastUpdate',
        name: i18n.LAST_UPDATE,
        width: '8%',
        widgetsInCell: true,
        decorator: function() {
          return "<div class='javaBatchGridTitleText'><span data-dojo-attach-point='dateTime'></span></div>";
        },
        setCellValue: function(dateValue) {
          var htmlForDate = utils.formatDate(dateValue);
          this.dateTime.innerHTML = htmlForDate;
        },
        hideable: true
      }, {
          id : ID.HOST,
          field : 'host',
          name : i18n.HOST,
          width : '5%',
          widgetsInCell : true,
          decorator : function() {
            return "<div class='javaBatchGridTitleText'><span data-dojo-attach-point='host'></span></div>";
          },
          setCellValue : function(hostVal, storeVal, cellWidget) {
            this.host.innerHTML = "<span title='" + hostVal + "' dir='" + sharedUtils.getStringTextDirection(hostVal) + "'>" + hostVal + '</span>';

            if (linkUtils.hasExploreTool() && !linkUtils.hasStandaloneServer()) {
                // Continue with checks to set up a link to view details of this host in the explore tool.
                // Note: standalone server does not have a Host page to view its details.
                linkUtils.isHostInExploreTool(hostVal).then(lang.hitch(this, function(response) {
                    if (response === true) {                         
                        var titleMsg = lang.replace(i18n.LINK_EXPLORE_HOST, [hostVal]);
                        var toolRef = linkUtils.getLinkPrefixToExploreTool() +"/hosts/";
                        var hostHref = window.top.location.origin + toolRef + hostVal;
                        this.host.innerHTML = "<span title=\"" + titleMsg + "\" dir='" + sharedUtils.getStringTextDirection(hostVal) + "'><a target='_blank' rel='noreferrer' href='" + hostHref + "'>" + hostVal + "</a></span>";
                    }
                }));  
            } 
            
            if (this._onKeydownHandler) {
              // Remove previously connected events to avoid memory leak.
              this._onKeydownHandler.remove();
            }
            this._onKeydownHandler = on(this.host, "keydown", function(evt) {
              if (evt.keyCode === keys.ENTER || evt.keyCode === keys.SPACE) {
                evt.srcElement.click();
              }
            });
          },
          hide: true,
          hideable: true
      }, {
          id : ID.USER_DIR,
          field : 'userDir',
          name : i18n.SERVER_USER_DIRECTORY,
          width : '10%',
          widgetsInCell : true,
          decorator : function() {
            return "<div class='javaBatchGridTitleText'><span data-dojo-attach-point='userDir'></span></div>";
          },
          setCellValue : function(value) {
            // Handle server's user dir correctly for bidi
            var bidiUserDir = BidiComplex.createDisplayString(value, "FILE_PATH");
            this.userDir.innerHTML = bidiUserDir;
            this.userDir.title = bidiUserDir;
          },
          hide: true,
          hideable: true
      }, {
        id: ID.SERVER_NAME,
        field: 'serverName',
        name: i18n.SERVER_NAME,
        width: '8%',
        widgetsInCell: true,
        decorator: function() {
          return "<div class='javaBatchGridTitleText'><span data-dojo-attach-point='serverName'></span></div>";
        },
        setCellValue : function(serverVal, storeVal, cellWidget) {
          if (linkUtils.hasExploreTool()) {
             // Continue with checks to set up a link to view details of this server in the explore tool
             if (linkUtils.hasStandaloneServer()) {
                 var standaloneServerObj = linkUtils.getStandaloneServerInfo();
                 if (standaloneServerObj) {
                     if (serverVal === standaloneServerObj.name) {
                         var titleMsg = lang.replace(i18n.LINK_EXPLORE_SERVER, [serverVal]);
                         var toolRef = linkUtils.getLinkPrefixToExploreTool();
                         var serverHref = window.top.location.origin + toolRef;
                         this.serverName.innerHTML = "<span title=\"" + titleMsg + "\" dir='" + sharedUtils.getStringTextDirection(serverVal) + "'><a target='_blank' rel='noreferrer' href='" + serverHref + "'>" + serverVal + "</a></span>";
                     } else { 
                         // This is not the standalone server.  It is just a server, so just display the name without a link.
                         this.serverName.innerHTML = "<span title='" + serverVal + "' dir='" + sharedUtils.getStringTextDirection(serverVal) + "'>" + serverVal + '</span>';
                     }                                          
                 } else {
                     this.serverName.innerHTML = "<span title='" + serverVal + "' dir='" + sharedUtils.getStringTextDirection(serverVal) + "'>" + serverVal + '</span>';                     
                 }
             } else {
                 // Continue with checks to see if this server is in a collective
                 var rowData = cellWidget.cell.row.data();
                 var serverObj = {name: serverVal,        // serverObj = {name: server, userdir: userdir, host: hostname}
                                  userdir: rowData.userDir,
                                  host: rowData.host
                                 };   
                 this.serverName.innerHTML = "<span title='" + serverVal + "' dir='" + sharedUtils.getStringTextDirection(serverVal) + "'>" + serverVal + '</span>';
                 linkUtils.isServerInCollective(serverObj).then(lang.hitch(this, function(serverObj, response) {
                     if (response === true) {                         
                         var titleMsg = lang.replace(i18n.LINK_EXPLORE_SERVER, [serverObj.name]);
                         var toolRef = linkUtils.getLinkPrefixToExploreTool() +"/servers/";
                         var serverHref = window.top.location.origin + toolRef + serverObj.host + "," + serverObj.userdir + "," + serverObj.name;
                         this.serverName.innerHTML = "<span title=\"" + titleMsg + "\" dir='" + sharedUtils.getStringTextDirection(serverObj.name) + "'><a target='_blank' rel='noreferrer' href='" + serverHref + "'>" + serverObj.name + "</a></span>";
                     } 
                     // else do nothing since we already put in a non-linking value in statement above 'isServerInCollective'.
                 }, serverObj));  
             }
          } else {
             // No link to the explore tool.  Display the name without a link.
              this.serverName.innerHTML = "<span title='" + serverVal + "' dir='" + sharedUtils.getStringTextDirection(serverVal) + "'>" + serverVal + '</span>';
          }

          if (this._onKeydownHandler) {
            // Remove previously connected events to avoid memory leak.
            this._onKeydownHandler.remove();
          }
          this._onKeydownHandler = on(this.serverName, "keydown", function(evt) {
            if (evt.keyCode === keys.ENTER || evt.keyCode === keys.SPACE) {
              evt.srcElement.click();
            }
          });
        },
        hide: false,
        hideable: true
      }, {
        id: ID.JOB_PARAMETERS,
        field: 'jobParameters',
        name: i18n.JOB_PARAMETERS,
        width: '15%',
        widgetsInCell: true,
        decorator: function() {
          return "<div class='javaBatchGridTitleText'><span data-dojo-attach-point='parms'></span></div>";
        },
        setCellValue: function(jobParms) {
          var jobParmsString = JSON.stringify(jobParms, null, 1);
          this.parms.innerHTML = "<span title='" + jobParmsString + "'>" + jobParmsString + '</span>';
        },
        hide: true,
        hideable: true
      }, {                              // Always keep the view log column as the last column
        id: ID.LOG,
        field: 'executionId',           // We want to use the job execution id for viewToHash
        name: '',                       // No column title
        columnSelectionMenu: true,      // This column selected to hold gear icon
        width: '7%',
        widgetsInCell: true,
        decorator: function(s) {
          return "<div><span data-dojo-attach-point='viewLog'></span></div>";
        },
        getCellWidgetConnects: function(cellWidget, cell){
          // Return an array of connection arguments for this CellWidget.
          // With this method, CellWidget will take care of connecting/disconnecting
          // all events for you, so you don't have to worry about memory leaks.
          var executionId = cell.data();
          return [
            [cellWidget.logButton, 'onClick', function(evt) {
              viewToHash.updateView("joblogs/?jobexecution=" + executionId);
            }],
            [cellWidget.logButton, 'onKeyDown', function(evt) {
              if (evt.keyCode === keys.ENTER || evt.keyCode === keys.SPACE) {
                viewToHash.updateView("joblogs/?jobexecution=" + executionId);
              }
            }]
          ];
        },
        initializeCellWidget: function(cellWidget, cell) {
          var executionId = cell.data();
          var title = i18n.VIEW_LOG_FILE;
          var buttonId = ID.VIEW_LOG_BUTTON + "_execution_" + executionId;
          var button = registry.byId(buttonId);

          if (!button) {
            cellWidget.logButton = new Button({
              id: buttonId,
              label: title,
              showLabel: false,
              iconClass : 'executionViewlogIcon',
              baseClass : 'executionViewLogButton',
              postCreate: function(){
                this.iconNode.innerHTML = imgUtils.getSVGSmall('logfile', 'viewLog');
              }
            });
          } else {
            cellWidget.logButton = button;
          }
//         domConstruct.empty(this.viewLog);
          cellWidget.logButton.placeAt(cellWidget.viewLog);
          cellWidget.logButton.startup();
        },
        uninitializeCellWidget: function(cellWidget, cell) {
          if (cellWidget.logButton) {
            cellWidget.logButton.destroy();
          }
        },
        hideable: false                 // This column should never be hidden
      } ];

      var cacheClass = Cache;

      var existingGrid = registry.byId(this.instanceId + '-executionGrid');
      if (existingGrid) {
        existingGrid.destroyRecursive();
      }

      this.executionGrid = new Grid({
        id: this.instanceId + '-executionGrid',
        cacheClass: cacheClass,
        store: this.executionStoreData,
        structure: executionInstanceLayout,
        baseClass: 'javaBatchGrid',
        columnWidthAutoResize: true,
        tableIdentifier: i18n.EXECUTIONS_TABLE_IDENTIFIER,
        autoHeight: true,       // Needed on this table to accommodate exactly the number of rows to show
        modules: [
                   CellWidget,
                   {
                     moduleClass: ColumnResizer,
                     onResize: function(colId, newWidth, oldWidth) {
                       // Persist the new column size.
                       var t = this,
                           g = t.grid;
                       if (g.gridStatePersist) {
                         g.gridStatePersist.save();
                       }
                     }
                   },
                   ExecutionGridColumnSelectionHeaderMenu,
                   Focus,
                   HiddenColumns,
                   {
                     moduleClass: GridStatePersist,
                     persistedData: this.persistedData,
                     key: "executionGrid"
                   }
                 ],
        modelExtensions: [Sort],
        style: 'margin-left: 100px;'
      });

//      this.executionGrid.gridStatePersist.enabled=false;  // Leave for devTest-reinits the cache

      // The last column containing the column selection button does not have a name (label).
      // However, when traversing the table header with key strokes the spot where the label
      // would be is marked as a focusNode in the HeaderRegions of the grid and is highlighted.
      // Setting this unused node (class=.gridxSortNode) to display=none will remove it from
      // being focused on when traversing the gridx header with the keyboard.
      var columnSelectionHeaderDomNode = this.executionGrid.header.getHeaderNode(ID.LOG);
      var labelNode = query('.gridxSortNode', columnSelectionHeaderDomNode)[0];
      domStyle.set(labelNode, "display", "none");
      aspect.after(this.executionGrid.header, 'onRender', function(c){
        // As the user selects/deselects the columns to be displayed the header may be re-rendered.
        // Reset the last column's node that would hold the header label to display=none so that it
        // does not get highlighted when navigating the grid with keystrokes (arrow key and tabs).
        var t = this;
        var columnSelectionHeaderDomNode = t.getHeaderNode(ID.LOG);
        var labelNode = query('.gridxSortNode', columnSelectionHeaderDomNode)[0];
        domStyle.set(labelNode, "display", "none");
      });

    },

    __updateExecutionGrid : function(){
      var self = this;
      var json = null;
      restUtils.get(restUtils.BASE_URI + "/jobinstances/" + self.instanceId +"/jobexecutions/").then(function(response){
        json = JSON.parse(response.data);
        // self.executionStoreData.data = [];
        json.forEach(function(eachExecution){
          var executionJson = self.__processJSONExecutionGrid(eachExecution);
          self.executionStoreData.put(executionJson);
        });
        self.executionGrid.model.clearCache();
        self.executionGrid.setStore(self.executionStoreData);
        // This method is called when originally creating the grid AND
        // when a new execution row is added to an existing grid following
        // a successful 'Restart' request for the job instance.  When a
        // single row is added to the existing grid's data it is added to the
        // "end" or last index for the data in the memoryStore and would
        // appear at the bottom of the execution grid displayed. Therefore,
        // reset the model's sort on the first column so that the new row
        // added will appear at the top of the list as expected when we
        // do the body.refresh().
        self.executionGrid.model.sort([{colId: 'executionId', descending: true}]);
        self.executionGrid.body.refresh();
      },
      function(err){
        utils.errorMessageRestFail(i18n.REST_CALL_FAILED, err);
      });
    },

    __processJSONExecutionGrid : function(json){
      // Extract the server information from the server ID
      var serverId = json.serverId;
      var serverObj = utils.extractServerParts(serverId);  // Returns host, userDir, and serverName

      var processedJSON = {
        executionId: "" + json.executionId + "",
        batchStatus: json.batchStatus,
        exitStatus: json.exitStatus,
//      exitStatus: "SleepyBatchlet:i=15;stopRequested=false; This is a whole other string of stuff to put in here to see what it looks like.",
        createTime: json.createTime,
        startTime: json.startTime,
        endTime: json.endTime,
        lastUpdate: json.lastUpdatedTime,
        serverName: serverObj.serverName,
        userDir: serverObj.userDir,
        host: serverObj.host,
        jobParameters: json.jobParameters
//      jobParameters: { "prop1" : "prop1value", "prop2" : "prop2value"}
      };

      return processedJSON;

    }

  });

  return exectionGrid;

});
