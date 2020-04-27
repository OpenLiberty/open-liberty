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
define([ 'dojo/_base/array', 'dojo/_base/declare', 'dojo/_base/lang',
         'dojo/aspect', 'dojo/dom-class', 'dojo/dom-construct', 'dojo/dom-style','dojo/json',
         'dojo/keys', 'dojo/on', 'dojo/query',
         'dojo/store/Memory',
         'dijit/_WidgetBase',
         'dijit/form/Button',
         'dijit/registry',
         'gridx/Grid',
         'gridx/core/model/cache/Sync',
         'gridx/modules/Bar', 'gridx/modules/CellWidget',
         'gridx/modules/ColumnResizer', 'gridx/modules/Dod',
         'gridx/modules/Focus', 'jsShared/grid/modules/HiddenColumns',
         'gridx/support/Summary',
         'jsBatch/widgets/grid/modules/JobInstanceQuerySizer',
         'jsShared/grid/modules/ColumnSelectionHeaderMenu',
         'jsShared/grid/modules/GridStatePersist',
         "jsShared/utils/toolData",
         "jsShared/utils/userConfig",
         'jsShared/utils/imgUtils',
         'jsShared/utils/utils',
         'jsBatch/utils/hashUtils',
         'jsBatch/utils/utils',
         'jsBatch/utils/restUtils',
         'jsBatch/utils/ID',
         'jsBatch/utils/viewToHash',
         'jsBatch/widgets/ActionButtons',
         'jsBatch/widgets/ExecutionGrid',
         'jsBatch/widgets/TextWithImage',
         'dojo/i18n!jsBatch/nls/javaBatchMessages',
         'gridx/modules/VirtualVScroller',
         'dojo/domReady!' ],
function(array, declare, lang,
         aspect, domClass, domConstruct, domStyle, JSON, keys, on, query,
         MemoryStore,
         _WidgetBase, Button, registry,
         Grid, Cache,
         Bar, CellWidget, ColumnResizer, Dod, Focus,
         HiddenColumns, Summary, JobInstanceQuerySizer,
         ColumnSelectionHeaderMenu, GridStatePersist,
         toolData, userConfig, imgUtils, sharedUtils, hashUtils, utils, restUtils, ID, viewToHash,
         ActionButtons, ExecutionGrid, TextWithImage, i18n, VirtualVScroller) {

  'use strict';

  /**
   * Defines the main grid found on the Java Batch Tool Landing Page.   It contains a listing
   * of job instances.
   */

  var javaBatchInstanceGrid = declare("JavaBatchInstanceGrid",[ _WidgetBase ], {
    jobStoreData : null,
    jobInstanceGrid : null,
    executionGridObjects : null,   // Will contain a listing of ExecutionGrids which have been
                                   // displayed by selecting a job instance twistie (first column).
                                   // The job instance ID of the job instance selected is the key
                                   // value within this list.

    constructor : function(persistedData){
      this.executionGridObjects = {};
      this.persistedData = persistedData;
    },

    postCreate : function() {
      var self = this;

      this.jobStoreData = new MemoryStore({
        idProperty: ID.INSTANCE_ID,
        data: [  ]
      });

      /* Additional Grid Structure properties added to support ColumnSelectionHeaderMenu:
       * hide -   true indicates the column should be hidden.
       * hideable - true indicates this column can be selected/unselected to be hidden;
       *          false indicates the column is ALWAYS displayed on the grid.
       * columnSelectionMenu - true indicates this column should hold the gear icon
       */
      var jobInstanceLayout = [ {
        id: ID.EXECUTIONS_DROPDOWN,   // unique id
        name: '',                   // column header
        width: '3%',
        hideable: false
      },{
        id: ID.JOB_NAME,
        field: 'jobName',
        name: i18n.BATCH_JOB_NAME,
        width: '10%',
        widgetsInCell: true,
        columnType: i18n.BATCH_JOB_NAME,
        filterFunction: self.__filter,
        decorator: function() {
          return "<div class='javaBatchGridTitleText'><span data-dojo-attach-point='jobName'></span></div>";
        },
        setCellValue : function(name, storeVal, cellWidget) {
          if(! name) {
             // Do not add <a> if we do not have a name to turn into a clickable link.  
             // It is a batchScan violations when there is a link with no text (text being the name)
            this.jobName.innerHTML = "";
            return;
          }
          var titleMsg = lang.replace(i18n.SEARCH_ON, [name, cellWidget.cell.column.columnType]);
          this.jobName.innerHTML = "<a title='" + titleMsg + "' dir='" + sharedUtils.getStringTextDirection(name) + "'>" + name + '</a>';
          if (cellWidget.jobName._cnnt) {
            // Cell Widgets are reused among different rows.  Remove previously
            // connected events to avoid memory leak.
            cellWidget.jobName._cnnt.remove();
          }
          this.jobName._cnnt = on(this.jobName, 'click', lang.hitch(cellWidget.cell, function() {
              var filterType = this.column.id;
              var filterValue = name;
              this.column.filterFunction(filterType, filterValue);
              return true;
          }));
        },
        hideable: true
      }, {
        id: ID.INSTANCE_ID,
        field: 'instanceId',
        name: i18n.INSTANCE_ID,
        width: '6%',
        widgetsInCell : true,
        columnType: i18n.INSTANCE_ID,
        filterFunction: self.__filter,
        decorator : function() {
          return "<div class='javaBatchGridTitleText'><span data-dojo-attach-point='instId'></span></div>";
        },
        setCellValue : function(idVal, storeVal, cellWidget) {
          var titleMsg = lang.replace(i18n.SEARCH_ON, [idVal, cellWidget.cell.column.columnType]);
          this.instId.innerHTML = '<a title="' + titleMsg + '">' + idVal + '</a>';

          if (cellWidget.instId._cnnt) {
            // Cell Widgets are reused among different rows.  Remove previously
            // connected events to avoid memory leak.
            cellWidget.instId._cnnt.remove();
          }
          this.instId._cnnt = on(this.instId, 'click', lang.hitch(cellWidget.cell, function() {
               var filterType = this.column.id;
               var filterValue = idVal;
               this.column.filterFunction(filterType, filterValue);
               return true;
          }));
        },
        hideable: true
      },{
        id: ID.JES_JOB_NAME,
        field: 'JESJobName',
        name: i18n.JES_JOB_NAME,
        width: '10%',
        widgetsInCell: true,
        columnType: i18n.JES_JOB_NAME,
        filterFunction: self.__filter,
        decorator: function() {
          return "<div class='javaBatchGridTitleText'><span data-dojo-attach-point='name'></span></div>";
        },
        setCellValue : function(name, storeVal, cellWidget) {
          if (name) {
            var titleMsg = lang.replace(i18n.SEARCH_ON, [name, cellWidget.cell.column.columnType]);
            this.name.innerHTML = "<span dir='" + sharedUtils.getStringTextDirection(name) + "'><a title='" + titleMsg + "'>" + name + "</a></span>";

            if (cellWidget.name._cnnt) {
              // Cell Widgets are reused among different rows.  Remove previously
              // connected events to avoid memory leak.
              cellWidget.name._cnnt.remove();
            }
            this.name._cnnt = on(this.name, 'click', lang.hitch(cellWidget.cell, function() {
                 var filterType = this.column.id;
                 var filterValue = name;
                 this.column.filterFunction(filterType, filterValue);
                 return true;
            }));
          }
        },
        hide: true,
        hideable: true
      },{
        id: ID.JES_JOB_ID,
        field: 'JESJobId',
        name: i18n.JES_JOB_ID,
        width: '6%',
        widgetsInCell: true,
        columnType: i18n.JES_JOB_ID,
        filterFunction: self.__filter,
        decorator: function() {
          return "<div class='javaBatchGridTitleText'><span data-dojo-attach-point='jjId'></span></div>";
        },
        setCellValue : function(idVal, storeVal, cellWidget) {
          if (idVal) {
            var titleMsg = lang.replace(i18n.SEARCH_ON, [idVal, cellWidget.cell.column.columnType]);
            this.jjId.innerHTML = "<span dir='" + sharedUtils.getStringTextDirection(idVal) + "'><a title='" + titleMsg + "'>" + idVal + "</a></span>";

            if (cellWidget.jjId._cnnt) {
              // Cell Widgets are reused among different rows.  Remove previously
              // connected events to avoid memory leak.
              cellWidget.jjId._cnnt.remove();
            }
            this.jjId._cnnt = on(this.jjId, 'click', lang.hitch(cellWidget.cell, function() {
                 var filterType = this.column.id;
                 var filterValue = idVal;
                 this.column.filterFunction(filterType, filterValue);
                 return true;
            }));
          }
        },
        hide: true,
        hideable: true
      }, {
        id: ID.APP_NAME,
        field: 'appName',
        name: i18n.APPLICATION_NAME,
        width: '18%',
        widgetsInCell: true,
        columnType: i18n.APPLICATION_NAME,
        filterFunction: self.__filter,
/**     formatter: function(data) {
          // Application Name is the 'application' piece of the AMC trio (application#module#component)
          data.appName = data.appName.substr(0, data.appName.indexOf('#'));
          return data.appName;
        }, **/
        decorator: function() {
          return "<div class='javaBatchGridTitleText'><span data-dojo-attach-point='applicationName'></span></div>";
        },
        setCellValue : function(fullAppName, storeVal, cellWidget) {
          if (fullAppName) {
            var shortAppName = fullAppName;
            // Application Name is the 'application' piece of the AMC trio (application#module#component)
            if (shortAppName.indexOf('#') > -1) {
              shortAppName = shortAppName.substr(0, shortAppName.indexOf('#'));
            }
            var titleMsg = lang.replace(i18n.SEARCH_ON, [shortAppName, cellWidget.cell.column.columnType]);
            this.applicationName.innerHTML =
                "<span dir='" + sharedUtils.getStringTextDirection(shortAppName) + "'><a title='" + titleMsg + "'>" + shortAppName + "</a></span>";
            if (cellWidget.applicationName._cnnt) {
              // Cell Widgets are reused among different rows.  Remove previously
              // connected events to avoid memory leak.
              cellWidget.applicationName._cnnt.remove();
            }
            this.applicationName._cnnt = on(this.applicationName, 'click', lang.hitch(cellWidget.cell, function() {
                 var filterType = this.column.id;
                 var filterValue = shortAppName;
                 this.column.filterFunction(filterType, filterValue);
                 return true;
            }));
          }
        },
        hideable: true
      }, {
        id: ID.SUBMITTER,
        field: 'submitter',
        name: i18n.SUBMITTER,
        width: '12%',
        widgetsInCell: true,
        columnType: i18n.SUBMITTER,
        filterFunction: self.__filter,
        decorator: function() {
          return "<div class='javaBatchGridTitleText'><span data-dojo-attach-point='submitter'></span></div>";
        },
        setCellValue : function(submitter, storeVal, cellWidget) {
          if (submitter) {
            var titleMsg = lang.replace(i18n.SEARCH_ON, [submitter, cellWidget.cell.column.columnType]);
            this.submitter.innerHTML = "<span dir='" + sharedUtils.getStringTextDirection(submitter) + "'><a title='" + titleMsg + "'>" + submitter + "</a></span>";

            if (cellWidget.submitter._cnnt) {
              // Cell Widgets are reused among different rows.  Remove previously
              // connected events to avoid memory leak.
              cellWidget.submitter._cnnt.remove();
            }
            this.submitter._cnnt = on(this.submitter, 'click', lang.hitch(cellWidget.cell, function() {
                 var filterType = this.column.id;
                 var filterValue = submitter;
                 this.column.filterFunction(filterType, filterValue);
                 return true;
            }));
          }
        },
        hideable: true
      },
      /**
       * Last Update column
       * dateValue is the time in server time, with timezone indicator
       */
      {
        id: ID.LAST_UPDATED_TIME,
        field: 'lastUpdatedTime',
        name: i18n.LAST_UPDATE,
        width: '14%',
        widgetsInCell: true,
        columnType: i18n.LAST_UPDATE,
        filterFunction: self.__filter,
        decorator: function() {
          return "<div class='javaBatchGridTitleText'><span data-dojo-attach-point='dateTime'></span></div>";
        },
        setCellValue: function(dateValue, storeVal, cellWidget) {
          var formattedDateTime = utils.formatDate(dateValue, 1);
          this.dateTime.innerHTML = formattedDateTime;

          if (cellWidget.dateTime._cnnt) {
            // Cell Widgets are reused among different rows.  Remove previously
            // connected events to avoid memory leak.
            cellWidget.dateTime._cnnt.remove();
          }

          var date = utils.getDate(dateValue);
          this.dateTime._cnnt = on(this.dateTime, 'click', lang.hitch(cellWidget.cell, function() {
               var filterType = ID.LAST_UPDATE; // column.id is lastUpdatedTime; need lastUpdate
               var filterValue = date;
               this.column.filterFunction(filterType, filterValue);
               return true;
          }));
        },
        hideable: true
      }, {
        id: ID.INSTANCE_STATE,
        field: 'instanceState',
        name: i18n.INSTANCE_STATE,
        width: '12%',
        widgetsInCell: true,
        columnType: i18n.INSTANCE_STATE,
        filterFunction: self.__filter,
        decorator: function() {
          return "<div class='javaBatchGridTitleText'><span data-dojo-attach-point='state'></span></div>";
        },
        setCellValue : function(stateVal, storeVal, cellWidget) {
          var stateValConverted = utils.stateStatusToLabel(stateVal);
          var titleMsg = lang.replace(i18n.SEARCH_ON, [stateValConverted, cellWidget.cell.column.columnType]);

          // Create the "icon + stateText" widget
          var columnId = cellWidget.cell.column.id;
          var rowId = cellWidget.cell.row.id;
          var cellDataId = "jobInstanceGrid-" + columnId + '-' + rowId;

          var cellValue = new TextWithImage ({
              type: 'state',
              value: stateVal,
              divId: cellDataId,
              title: titleMsg
          });

          this.state.innerHTML = '<a title="' + titleMsg + '">' + cellValue.domNode.innerHTML + '</a>';
          if (cellWidget.state._cnnt) {
            // Cell Widgets are reused among different rows.  Remove previously
            // connected events to avoid memory leak.
            cellWidget.state._cnnt.remove();
          }
          this.state._cnnt = on(this.state, 'click', lang.hitch(cellWidget.cell, function() {
               var filterType = this.column.id;
               var filterValue = stateVal;
               this.column.filterFunction(filterType, filterValue);
               return true;
          }));
        },
        hideable: true
      }, {
        id: ID.ACTIONS,
        field: 'actions',
        name: i18n.ACTIONS,
        width: '5%',
        widgetsInCell: true,
        decorator: function(s) {
          return "<div class='listViewAction'><span data-dojo-attach-point='action'></span></div>";
        },
        setCellValue: function(idVal, storeVal, cellWidget) {
          var actionButton = ActionButtons.createActionDropDownButton('inst', idVal);
          domConstruct.empty(this.action);
          actionButton.placeAt(this.action);
          actionButton.startup();
        },
        hideable: true
      }, {                              // Always keep the view log column as the last column
        id: ID.LOG,
        field: 'instanceId',
        name: '',                       // No column title
        columnSelectionMenu: true,      // This column selected to hold gear icon
        width: '4%',
        widgetsInCell: true,
        decorator: function(s) {
          return "<div><span data-dojo-attach-point='viewLog'></span></div>";
        },
        getCellWidgetConnects: function(cellWidget, cell){
          // Return an array of connection arguments for this CellWidget.
          // With this method, CellWidget will take care of connecting/disconnecting
          // all events for you, so you don't have to worry about memory leaks.
          var instanceId = cell.data();
          return [
            [cellWidget.logButton, 'onClick', function(evt) {
              viewToHash.updateView("joblogs/?jobinstance=" + instanceId);
            }],
            [cellWidget.logButton, 'onKeyDown', function(evt) {
              if (evt.keyCode === keys.ENTER || evt.keyCode === keys.SPACE) {
                viewToHash.updateView("joblogs/?jobinstance=" + instanceId);
              }
            }]
          ];
        },
        initializeCellWidget: function(cellWidget, cell) {
          var instanceId = cell.data();
          var title = i18n.VIEW_LOG_FILE;
          var buttonId = ID.VIEW_LOG_BUTTON + "_inst_" + instanceId;
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
          // remove any log file button that has previously attached to viewLog
          domConstruct.empty(cellWidget.viewLog);
          cellWidget.logButton.placeAt(cellWidget.viewLog);
          cellWidget.logButton.startup();
        },
        uninitializeCellWidget: function(cellWidget, cell) {
          var buttonId = ID.VIEW_LOG_BUTTON + "_inst_" + cell.data();
          var button = registry.byId(buttonId);
          if (button) {
            button.destroy();
          }
        },

        hideable: false                 // This column should never be hidden
      }];

      var cacheClass = Cache;

      this.jobInstanceGrid = new Grid({
        id: ID.JOBINSTANCE_GRID,
        cacheClass: cacheClass,
        store: this.jobStoreData,
        structure: jobInstanceLayout,
        baseClass: 'javaBatchGrid',
        tableIdentifier: i18n.INSTANCES_TABLE_IDENTIFIER,
        executionGrids: this.executionGridObjects,
        columnWidthAutoResize: true,
        //    autoHeight: true,    ... not needed here so the table scrolls, not the containing div
        barBottom: [
                    {pluginClass: Summary, message: i18n.GRIDX_SUMMARY_TEXT},
                    JobInstanceQuerySizer
                   ],
        modules: [ Bar,
                   VirtualVScroller,
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
                   ColumnSelectionHeaderMenu,
                   Focus,
                   HiddenColumns,
                   {
                     moduleClass: GridStatePersist,
                     persistedData: this.persistedData,
                     key: "jobInstanceGrid"
                   },
                   {
                     moduleClass: Dod,
                     defaultShow: false,   // true to automatically have details expand on grid show
                     useAnimation: true,   // use sliding animation when row is expanded/collapsed
                     showExpando: true,    // show expando icon; otherwise expand/collapse programmatically
                     _onModelSet: function(id, index, row) {
                      // Don't do anything with the DOD.  We handle that internally.
                      },
                     detailProvider: lang.hitch(this, this.__showExecutionGrid)
                   } ],
        style: "height: 100%",
        updateInstanceRow: function(rowId) {
          self.__updateInstanceRow(rowId);
        }
      });

//      this.jobInstanceGrid.gridStatePersist.enabled=false;     // Leave for devTest-reinits the cache

      // Each time we expand the DOD for the job instance, update the
      // the job execution grid and job instance row.
      this.jobInstanceGrid.dod.onShow = lang.hitch(this, function(row) {
        console.log('row shown: ' + row.id);
        if (this.executionGridObjects[row.id]) {
          // Update the instance row with newest data
          this.__updateInstanceRow(row.id);

          this.executionGridObjects[row.id].__updateExecutionGrid();
        } else {
          console.error("ExecutionGrid not present for execution id " + row.id);
        }
      });
      this.jobInstanceGrid.dod.onHide = lang.hitch(this, function(row) {
        console.log('row hidden: ' + row.id);
      });

      this.jobInstanceGrid.on('cellKeyDown', lang.hitch(this, function(evt) {
        if (evt.keyCode === keys.SPACE  && evt.columnIndex === 0) {
          this.jobInstanceGrid.dod.toggle(parseInt(evt.rowId, 10));
        } else if (evt.keyCode === keys.TAB && evt.columnIndex === 0){
          var rowId = parseInt(evt.rowId, 10);
          if (this.jobInstanceGrid.dod.isShown(rowId)) {
            // When TABbing in the FIRST column, the one with the twistie,
            // if the executionGrid is showing for this Job Instance, then
            // put focus on the executionGrid.  If it is not showing, then
            // TAB will work as usual....which will take you out of the body
            // of the Job Instances table and onto the table footer.
            //
            // ESCAPE will take you from the executionGrid back to the
            // first column cell of the associated Job Instance row.
            var associatedExecutionGrid = this.executionGridObjects[rowId];
            if (associatedExecutionGrid) {
              associatedExecutionGrid.executionGrid.focus.focusArea('header');
              // Stop the event from processing further...
              evt.stopPropagation();
            }
          }
        }
      }));

      // The last column containing the column selection button does not have a name (label).
      // However, when traversing the table header with key strokes the spot where the label
      // would be is marked as a focusNode in the HeaderRegions of the grid and is highlighted.
      // Setting this unused node (class=.gridxSortNode) to display=none will remove it from
      // being focused on when traversing the gridx header with the keyboard.
      var columnSelectionHeaderDomNode = this.jobInstanceGrid.header.getHeaderNode(ID.LOG);
      var labelNode = query('.gridxSortNode', columnSelectionHeaderDomNode)[0];
      domStyle.set(labelNode, "display", "none");
      aspect.after(this.jobInstanceGrid.header, 'onRender', function(c){
        // As the user selects/deselects the columns to be displayed the header may be re-rendered.
        // Reset the last column's node that would hold the header label to display=none so that it
        // does not get highlighted when navigating the grid with keystrokes (arrow key and tabs).
        var t = this;
        var columnSelectionHeaderDomNode = t.getHeaderNode(ID.LOG);
        var labelNode = query('.gridxSortNode', columnSelectionHeaderDomNode)[0];
        domStyle.set(labelNode, "display", "none");
      });

      aspect.after(this.jobInstanceGrid, 'resize', lang.hitch(this, function(){
        // After the instance grid is resized, resize all the execution grids contained
        // within to fit their new space.
        // Start by resizing the intance grid's DOD spaces which house the execution grids...
        this.jobInstanceGrid.dod._onColumnResize();

        // Then relayout the execution grids within the DODs so that vertical
        // adjustments can be made if needed.
        var eGrids = Object.keys(this.executionGridObjects);
        for (var i = 0; i < eGrids.length; i++) {
          this.executionGridObjects[eGrids[i]].executionGrid.hLayout.reLayout();
        }
      }));

    },

    __updateTable : function(response){
      var self = this;
      var json = JSON.parse(response);
      // Parse the response into a JSON object
      this.jobStoreData.setData([]); //reset dataStore before repopulating
      json.forEach(function(eachInstance){
        var instanceJson = self.__processJSONInstanceGrid(eachInstance);
        self.jobStoreData.put(instanceJson, {id:instanceJson.instanceId}); // push directly to dataStore so it can keep track of IDs
      });
      self.jobInstanceGrid.model.clearCache();
      self.jobInstanceGrid.setStore(self.jobStoreData);
      self.jobInstanceGrid.body.refresh();
    },

    /**
      * Update instance row with the latest data.
      *
      * @param row  row number of the instance
      */
    __updateInstanceRow : function(rowId){
      restUtils.getWithParms(restUtils.JOB_INSTANCE_DETAIL_QUERY,[rowId]).then(lang.hitch(this, function(response){
        var newData = response.data;
        var json = JSON.parse(newData);
        var instanceJson = this.__processJSONInstanceGrid(json);

        this.jobStoreData.put(instanceJson, {id:rowId});
      }));
    },

    __processJSONInstanceGrid : function(json){
      // Until the Batch Runtime is able to return a Job Instance State
      // of 'Stopping', examine the batchStatus value for the Job Instance
      // and set this Job Instance's instanceState value to 'STOPPING'
      // if the batchStatus is 'STOPPING'.  This allows the UI to provide
      // immediate feedback on the grid for a successful Stop request.
      var instanceState = json.batchStatus === "STOPPING"? "STOPPING": json.instanceState;

      var processedJSON = {
          jobName: json.jobName,
          instanceId: "" + json.instanceId,
          JESJobName: json.JESJobName || "",
          JESJobId: json.JESJobId || "",
          appName: json.appName,
          submitter: json.submitter,
          lastUpdatedTime: json.lastUpdatedTime,
          instanceState: instanceState,
          actions: "" + json.instanceId
      };

      return processedJSON;

    },

    __showExecutionGrid : function(jobInstanceGrid, rowId, detailNode, rendered) {
      var me = this;
      var createGrid = function(data){
        console.log("ID of row to expand: " + rowId);
        var executionGrid = new ExecutionGrid(data, rowId);
        executionGrid.executionGrid.placeAt(detailNode);
        executionGrid.executionGrid.startup();
        me.executionGridObjects[rowId] = executionGrid;
        rendered.callback();
        return rendered;
      };
      userConfig.load(lang.hitch(this, function(data){
        // Persisted data loaded
        createGrid(data);
      }),
      lang.hitch(this, function(){
        // Persisted data not found
        createGrid(null);
      }));
    },

    /**
     * Add selected value to the page's query parameters by updating the URL.
     * Method will know how to handle when the query parameter is in a list form
     * like jobInstanceId=1,2,3... and when the parameter should be repeated key=value pairs
     * like submitter=alice&submitter=bob&...
     *
     * @param filterType   columnId of value selected
     * @param filterValue  value selected
     */
    __filter : function(filterType, filterValue) {
      console.log("Filter on " + filterType + " with " + filterValue);

      // These query strings need to be in list format, ie foo=1,2,3,4
      var filterTypeNeedingListFormat = ["jobInstanceId", "instanceState"];

      var newParam = hashUtils.getColumnIdQueryParam(filterType);
      var currentQueryParms = hashUtils.getQueryParams();  // returns null if none are set

      if(! currentQueryParms) {
        // Filtering only on this new value
        currentQueryParms = newParam + "=" + filterValue;
        viewToHash.updateView("?" + currentQueryParms);
        return;
      }

      // There are existing query parameters
      // Add the newly selected filter to the existing URL query parameters
      var existingQueryParamHash = hashUtils.getQueryParamHash();
      if (existingQueryParamHash[newParam]) {
        // The current query parameters already contain a value for this
        // filterType.  Add another value to this parameter.
        var currentValue = existingQueryParamHash[newParam];
        var newValue = currentValue + ',' + filterValue;
        existingQueryParamHash[newParam] = newValue;
      } else {
        // This is a new query parameter for the current URL
        existingQueryParamHash[newParam] = filterValue;
      }

      // Re-assemble the query parameters from the hash
      currentQueryParms = "";
      for (var param in existingQueryParamHash) {
        if(existingQueryParamHash.hasOwnProperty(param)) { // To comply with JSHint
          if (currentQueryParms.length > 0) {
            currentQueryParms += "&";
          }
          var covertToRepeatedKeyValuePairs =
            filterTypeNeedingListFormat.indexOf(param) === -1;

          if(covertToRepeatedKeyValuePairs) {
            currentQueryParms += hashUtils.convertListToMultipleKeys(param, existingQueryParamHash[param]);
          } else {
            currentQueryParms += param + "=" + existingQueryParamHash[param];
          }
        }
      }
      viewToHash.updateView("?" + currentQueryParms);
    } // end of __filter method

  });

  return javaBatchInstanceGrid;

});
