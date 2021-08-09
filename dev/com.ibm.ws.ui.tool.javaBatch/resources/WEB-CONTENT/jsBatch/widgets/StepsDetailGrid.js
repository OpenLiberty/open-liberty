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

define(['dojo/_base/array', 'dojo/_base/declare',
        'dojo/aspect', 'dojo/dom-class', 'dojo/dom-style', 'dojo/json', 'dojo/query',
        'dojo/store/Memory', 
        'dijit/_WidgetBase',
        'gridx/Grid', 'gridx/core/model/cache/Sync', 
        'gridx/modules/CellWidget', 'gridx/modules/ColumnResizer', 'gridx/modules/Focus',
        'jsShared/grid/modules/HiddenColumns', 'jsShared/grid/modules/ColumnSelectionHeaderMenu',
        'jsShared/grid/modules/GridStatePersist',
        'jsShared/utils/utils',
        'jsBatch/utils/ID',
        'jsBatch/utils/utils',
        'jsBatch/widgets/TextWithImage',
        'dojo/i18n!jsBatch/nls/javaBatchMessages'
       ],
function( array, declare,
          aspect, domClass, domStyle, JSON, query, MemoryStore,
          WidgetBase,
          Grid, Cache,
          CellWidget, ColumnResizer, Focus, 
          HiddenColumns, ColumnSelectionHeaderMenu, GridStatePersist,
          sharedUtils, ID, utils,
          TextWithImage,
          i18n
        ) {
  "use strict";
  
  /** 
   * Creates the grid detailing the steps and partitions associated with a particular execution of a batch job.
   * 
   * This grid is displayed at the bottom 1/2 of the Execution Details view.
   * 
   * Creation relies on the following parameters
   *    executionId: execution identifier
   *    stepsDetails: JSON object containing the values to be displayed about the steps and partitions
   *                  associated with the execution.
   */
  
  return declare("StepsDetailGrid", [ WidgetBase ], {
    stepsStoreData: null,
    stepsDetailGrid: null,
    
    // Values set from query response data, passed in as parameters.
    executionId: null,
    stepsData: null,
  
    constructor: function(args) {
      this.executionId = args.executionId;
      this.stepsData = args.stepsDetails;   
      this.persistedData = args.persistedData;
    },
    
    postCreate: function() {
      this._buildStepsDetailGrid(this._getStepsDetailLayout());
      
      this.__updateStepsDetailData();
    },
    
    _buildStepsDetailGrid: function(layout) {
      var t = this;

      t.stepsStoreData = new MemoryStore({
        idProperty: ID.STEP_EXECUTION_ID,
        data: [  ]
      });
      
      var cacheClass = Cache;

      // Instantiate the grid
      t.stepsDetailGrid = new Grid({
        id: ID.STEP_DETAIL_GRID,
        cacheClass: cacheClass,
        store: t.stepsStoreData,
        structure: layout,
        baseClass: 'javaBatchGrid',
        columnWidthAutoResize: true,
        tableIdentifier: i18n.STEPS_DETAILS_TABLE_IDENTIFIER,
        //    autoHeight: true,    ... not needed here so the table scrolls, not the containing div
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
                   ColumnSelectionHeaderMenu,
                   Focus,
                   HiddenColumns,
                   {
                     moduleClass: GridStatePersist,
                     persistedData: this.persistedData,
                     key: "stepsDetailGrid"
                   }
                 ],
        style: "height: 100%;"
      });
      
//    t.stepsDetailGrid.gridStatePersist.enabled=false;  // Leave for devTest-reinits the cache

      // The last column containing the column selection button does not have a name (label).
      // However, when traversing the table header with key strokes the spot where the label
      // would be is marked as a focusNode in the HeaderRegions of the grid and is highlighted.
      // Setting this unused node (class=.gridxSortNode) to display=none will remove it from
      // being focused on when traversing the gridx header with the keyboard.
      var columnSelectionHeaderDomNode = this.stepsDetailGrid.header.getHeaderNode(ID.COLUMN_SELECT);
      var labelNode = query('.gridxSortNode', columnSelectionHeaderDomNode)[0];
      domStyle.set(labelNode, "display", "none");
      aspect.after(this.stepsDetailGrid.header, 'onRender', function(c){
        // As the user selects/deselects the columns to be displayed the header may be re-rendered.
        // Reset the last column's node that would hold the header label to display=none so that it 
        // does not get highlighted when navigating the grid with keystrokes (arrow key and tabs).
        var t = this;
        var columnSelectionHeaderDomNode = t.getHeaderNode(ID.COLUMN_SELECT);
        var labelNode = query('.gridxSortNode', columnSelectionHeaderDomNode)[0];
        domStyle.set(labelNode, "display", "none");
      });
    },
    
    _getStepsDetailLayout: function() {
      var t = this;
      
      if (!t.viewLayout) {
        /* Additional Grid Structure properties added to support ColumnSelectionHeaderMenu:
         * hide -   true indicates the column should be hidden. 
         * hideable - true indicates this column can be selected/unselected to be hidden;
         *          false indicates the column is ALWAYS displayed on the grid.  
         * columnSelectionMenu - true indicates this column should hold the gear icon                 
         */
        t.viewLayout = [ {
          id: ID.STEP_NAME,
          field: 'stepName',
          name: i18n.STEP_NAME,
          width: '10%',
          widgetsInCell : true,
          decorator : function() {
            return "<div class='javaBatchGridTitleText'><span data-dojo-attach-point='stepName'></span></div>";
          },
          setCellValue : function(name) {
            this.stepName.innerHTML = "<span title='" + name + "' dir='" + sharedUtils.getStringTextDirection(name) + "'>" + name + '</span>';
          },
          hideable: true
        }, {
          id: ID.STEP_EXECUTION_ID,
          field: 'stepExecutionId',
          name: i18n.ID,
          width: '10%',
          widgetsInCell : true,
          decorator : function() {
            return "<div class='javaBatchGridTitleText'><span data-dojo-attach-point='idValue'></span></div>";
          },
          setCellValue : function(id, storeVal, cellWidget) {
            // This column holds both the Step Execution ID for rows containing step information
            //                    and the Partition Number for rows containing partition information.
            // It is also the idProperty for the store so it had to have a unique value per row.   
            // Therefore, the id of the partition rows were suffixed with "_part_<partionNumber>" so each
            // row in the table could maintain a unique ID.
            // To format the data, remove the suffix on partition rows.
            var idValue = id + '';
            if (idValue.indexOf("_part_") === -1) {              
              // This entry is for a step
              this.idValue.innerHTML = "<span title='" + idValue + "'>" + idValue + '</span>';              
            } else {
              // This entry is for a partition
              var columnId = cellWidget.cell.column.id;
              var stepId = idValue.substring(idValue.lastIndexOf("_") + 1);
              var partitionId = idValue.substring(0, idValue.indexOf("_part_"));
              var cellDataId = "stepsDetailGrid-" + columnId + '-'+ stepId + '-partition-' + partitionId;
              var cellValue = new TextWithImage ({
                  type: 'id',
                  value: partitionId,
                  divId: cellDataId
                });
              this.idValue.innerHTML = cellValue.domNode.innerHTML;
            }
          },
          hideable: true
        }, {
          id: ID.BATCH_STATUS,
          field: 'batchStatus',
          name: i18n.BATCH_STATUS,
          width: '15%',
          widgetsInCell: true,
          decorator: function() {
            return "<div class='javaBatchGridTitleText'><span data-dojo-attach-point='state'></span></div>";
          },
          setCellValue: function(stateVal, storeVal, cellWidget) {
            // Create the ID for the State field for test purposes
            var columnId = cellWidget.cell.column.id;
            var rowId = cellWidget.cell.row.id;
            var cellDataId = "stepsDetailGrid-" + columnId + '-' + rowId;
            var cellValue = new TextWithImage ({
                type: 'status',
                value: stateVal,
                divId: cellDataId
              });
              this.state.innerHTML = cellValue.domNode.innerHTML;
          }, 
          hideable: true
        }, {
          id: ID.START_TIME,
          field: 'startTime',
          name: i18n.START_TIME,
          width: '15%',
          widgetsInCell: true,
          decorator: function() {
            return "<div class='javaBatchGridTitleText'><span data-dojo-attach-point='dateTime'></span></div>";
          },
          setCellValue: function(dateValue) {
              this.dateTime.innerHTML = utils.formatDate(dateValue);
          },
          hideable: true
        }, {
          id: ID.END_TIME,
          field: 'endTime',
          name: i18n.END_TIME,
          width: '15%',
          widgetsInCell: true,
          decorator: function() {
            return "<div class='javaBatchGridTitleText'><span data-dojo-attach-point='dateTime'></span></div>";
          },
          setCellValue: function(dateValue) {
              this.dateTime.innerHTML = utils.formatDate(dateValue);
          },
          hideable: true
        }, {
          id: ID.EXIT_STATUS,
          field: 'exitStatus',
          name: i18n.EXIT_STATUS,
          width: '30%',
          widgetsInCell: true,
          decorator: function() {
            return "<div class='javaBatchGridTitleText'><span data-dojo-attach-point='status'></span></div>";
          },
          setCellValue: function(status) {
            this.status.innerHTML = "<span title='" + status + "' dir='" + sharedUtils.getStringTextDirection(status) + "'>" + status + '</span>';
          },
          hideable: true
        }, {                              // Always keep the column selection column as the last column
          id: ID.COLUMN_SELECT,
          field: 'colSelect',
          name: '',                       // No column title
          columnSelectionMenu: true,      // This column selected to hold gear icon
          width: '5%',
          hideable: false                 // This column should never be hidden
        } ];
      }
      
      return t.viewLayout;
    },
    
    /** 
     * This method sets the default hidden columns for the grid.  
     * 
     * Two new properties were added for each column in the grid structure:
     * hideable - true indicates this column can be selected/unselected to be hidden;
     *            false indicates the column is ALWAYS displayed on the grid.           
     * hide - true indicates the column should be initially hidden. 
     */
    __hideDefaultColumns : function() {
      var t = this;
      
      // set initial hidden columns
      for (var i = 0; i < t.stepsDetailGrid.structure.length; i++) {
        // if a hideable column and 'hide' is initialized to true, hide the column
        if (t.stepsDetailGrid.structure[i].hideable && t.stepsDetailGrid.structure[i].hide) {
          t.stepsDetailGrid.hiddenColumns.add(t.stepsDetailGrid.structure[i].id);
        }
      }

      if (t.stepsDetailGrid.gridStatePersist) {         // If we are persisting the grid state
        t.stepsDetailGrid.gridStatePersist.save();      // save off the current hidden columns.
      }
    },

    __updateStepsDetailData: function() {
      var t = this;      
      t.stepsStoreData.data = [];
      
      this.stepsData.forEach(function(eachStep) {
        // The query response currently returns an array of mixed elements.  It contains step 
        // elements, however, the last element of the array is a links array with pointers 
        // back to the job instance and execution rather than a step element.   Therefore, 
        // look that the element being processed is a step element by checking if it contains 
        // 'stepName' before processing.
        if (eachStep.stepName) {            
          var stepJson = t.__processJSONStepsDetailGrid(eachStep);
          t.stepsStoreData.data.push(stepJson);
          if (stepJson.partitions && stepJson.partitions.length > 0) {
            var stepExecutionId = stepJson.stepExecutionId;
            stepJson.partitions.forEach(function(eachPartition) {
              var partitionJson = t.__processJSONPartitionDetail(eachPartition, stepExecutionId);
              t.stepsStoreData.data.push(partitionJson);
            }); 
          }
        }
      });
      
      t.stepsDetailGrid.model.clearCache();
      t.stepsDetailGrid.setStore(t.stepsStoreData);
      t.stepsDetailGrid.body.refresh();
    },
    
    __processJSONStepsDetailGrid: function(json) {
      var processedJSON = {
          stepName: json.stepName,
          stepExecutionId: json.stepExecutionId,
          batchStatus: json.batchStatus,
          startTime: json.startTime,
          endTime: json.endTime,
          exitStatus: json.exitStatus,
          partitions: json.partitions
      };
      
      return processedJSON;      
    },
    
    __processJSONPartitionDetail: function(json, stepExecutionId) {
      var t = this;
      
      // NOTE: The stepExecutionId is the idProperty of the store. This table column displays both the
      //       stepExecutionId value of a row with step information and the partition number value
      //       of a row with partition information.    Each entry in the store must
      //       have a unique stepExecutionId value.  Since the partition numbers are only uniquely
      //       assigned within a step and there can be multiple steps in the grid, ensure the 
      //       stepExecutionId value is unique by appending the actual stepExecutionId value to
      //       the partitionNumber.
      var processedJSON = {
          stepName: "",
          stepExecutionId: json.partitionNumber + "_part_" + stepExecutionId,
          batchStatus: json.batchStatus,
          startTime: json.startTime,
          endTime: json.endTime,
          exitStatus: json.exitStatus
      };
      
      return processedJSON;
    },
    
    updateTable: function(executionId, stepsDetails) {
      var t = this;
      
      t.executionId = executionId;
      t.stepsData = stepsDetails;
      
      t.__updateStepsDetailData();
    }
    
    
  }); 

});