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
/* jshint strict: false */
define([ 'dojo/_base/declare', 'dojo/_base/event', 'dojo/_base/lang', 'dojo/dom',
         'dojo/dom-construct', 'dojo/dom-style',
         'dojo/keys', 'dojo/on', 'dojo/query', 'dijit/registry',
         'dijit/_AttachMixin', 'dijit/_WidgetBase', 'dijit/_TemplatedMixin', 'dijit/_WidgetsInTemplateMixin',

         'dijit/a11y',  // ???????

         'dijit/form/Button', 'jsBatch/widgets/AutoTextarea',
         'gridx/Grid', 'gridx/core/model/cache/Sync',
         'gridx/modules/CellWidget', 'gridx/modules/Edit', 'gridx/modules/HeaderRegions', 'gridx/modules/SingleSort',
         'dojo/store/Memory',
         'js/widgets/ConfirmDialog',
         'jsBatch/utils/ID',
         'jsShared/utils/imgUtils',
         'dojo/i18n!jsBatch/nls/javaBatchMessages'
       ],
function(declare, event, lang, dom,
         domConstruct, domStyle,
         keys, on, query, registry,
         _AttachMixin, _WidgetBase, _TemplatedMixin, _WidgetsInTemplateMixin,
         a11y,
         Button, AutoTextarea,
         Grid, Cache,
         CellWidget, Edit, HeaderRegions, SingleSort,
         MemoryStore,
         ConfirmDialog,
         ID, imgUtils, i18n) {

  return declare([ ConfirmDialog, _AttachMixin], {
    // This dialog extends js/widgets/ConfirmDialog.  It uses the options node
    // to display an input area for job parameters that can be submitted with
    // a job instance Restart request.

    parm_column_header: i18n.PARM_NAME_COLUMN_HEADER,
    value_column_header: i18n.PARM_VALUE_COLUMN_HEADER,
    parameter_name_placeholder: i18n.JOB_PARAMETER_NAME,
    parameter_value_placeholder: i18n.JOB_PARAMETER_VALUE,
    noname_error_string: i18n.PARMS_ENTRY_ERROR,
    parm_remove: i18n.PARM_REMOVE_ICON_TITLE,

    constructor: function(args) {
      this.inherited(arguments);
      this.hasOptions = true;         // Indicate this dialog needs an options section.
      this.overrideWidth = true;      // Indicate that the confirm dialog needs to be
                                      // wider than the standard size.
      this.overrideWidthPx = 600;     // Provide the new width value.

      // Convert the job parameters for grid data.
      this.originalParameters = []; var i=0;
      for (var key in args.jobParameters) {
        if (args.jobParameters.hasOwnProperty(key)) {    // Not from prototype chain
          this.originalParameters[i] = {parmId: i,
                                        parmName: key,
                                        parmValue: args.jobParameters[key]
                                       };
          i++;
       }
      }

      this.nextIndex = i;            // Track the index for adding new parameters.

    },

    buildRendering: function() {
      this.inherited(arguments);

      // Create the input area for the job parameters on the confirmation dialog.
      var targetNode = query("div[data-dojo-attach-point='optionsNode']", this.contentNode.domNode)[0];
      var optionsNode = new (declare([_WidgetBase, _TemplatedMixin, _WidgetsInTemplateMixin], {
        templateString: '<div>' +
                        '  <input role="checkbox" type="checkbox" id=${reuseParmsToggleID} checked aria-checked="true"></input>' +
                        '  <label for=${reuseParmsToggleID}>${reuseParmsToggleLabel}</label>' +
                        '  <div id=${parmGridDivID} style="height:300px; width: 100%; margin-top: 10px;" data-dojo-attach-point=${parmGridDivID}> ' +
                        '  </div>' +
                        '  <div id="errMessageDiv" class="entryErrMessage">' +
                        '    <div id="entryErrorIcon" class="entryErrIcon"></div>' +
                        '    <span id="restartJobDlgErrMsg"></span>' +
                        '  </div>' +
                        '</div>',
        reuseParmsToggleID: ID.REUSE_PARMS_TOGGLE_ID,
        reuseParmsToggleLabel: i18n.REUSE_PARMS_TOGGLE_LABEL,
        parmGridDivID: ID.PARM_GRID_DIV_ID
      }))();
      this._attachTemplateNodes(optionsNode.domNode);
      optionsNode.startup();
      domConstruct.place(optionsNode.domNode, targetNode);
    },

    postCreate: function() {
      this.inherited(arguments);
      var t = this;

      // The parent class, js/widgets/ConfirmDialog, will always close and
      // destroy the dialog after the "OK" (or action) button is selected.
      // However, since we can enter parameters on this confirmation
      // dialog, we need to first validate the inputed value in the entry
      // field and then close the dialog if the inputed value is correct.
      // Therefore, we need to change the event processing code...
      t.okSignal.remove();  // Remove default ConfirmDialog processing
      var contents = t.contentNode;
      t.okSignal = on(contents.okButton, "click", function() {
        if (t.okFunction) {
          t.okFunction();
        } else {
          // Nothing to process.....close and destroy the dialog.
          t.okSignal.remove();
          t.destroyRecursive();
        }
      });

      // DESIGN NOTE:   The dialog will originally display with a checkbox and
      //                a display-only grid showing the current job parameters.
      //                When the reuse parameters checkbox is un-selected,
      //                indicating that the user wishes to update their job
      //                parameters, the display-only grid is hidden and another
      //                editable grid is displayed.  If the checkbox is reselected,
      //                the editable grid is hidden, the original parameters are
      //                restored to the grid store, and the read-only grid is
      //                displayed.

      // Create a copy of the original parameters.
      var data = lang.clone(t.originalParameters);

      // Set up the grid store
      var cacheClass = Cache;
      t.jobParmStore = new MemoryStore({
        idProperty: 'parmId',
        data: data
      });

      // Create the Display-only parameter grid
      var columns = [
                      {id: ID.PARM_GRID_NAME,
                       field: 'parmName',
                       name: t.parm_column_header,
                       width: '40%'
                      },
                      {id: ID.PARM_GRID_VALUE,
                       field: 'parmValue',
                       name: t.value_column_header,
                       width: '60%'
                      }
                    ];

      t.grid = new Grid({
        id: ID.PARM_GRID_ID,
        cacheClass: cacheClass,
        store: t.jobParmStore,
        structure: columns,
        baseClass: 'restartParmsGrid',
        columnWidthAutoResize: true,
        modules: [
           {
             moduleClass: SingleSort,
             initialOrder: {colId: ID.PARM_GRID_NAME, descending: false}
           }
        ],
        bodyEmptyInfo: lang.replace(i18n.JOB_PARAMETERS_EMPTY, [i18n.REUSE_PARMS_TOGGLE_LABEL])
      });

      // Toggle the reuse parameters checkbox....
      dom.byId(ID.REUSE_PARMS_TOGGLE_ID).onclick = function(e) {
        if (e.target.checked) {
          //  **** DON'T ALLOW EDITS IN THE TABLE
          e.target.setAttribute('aria-checked', 'true');

          if (t.grid) {
            // Reset to the initial parameters when dialog was created.
            t.grid.model.clearCache();
            var oldParms = lang.clone(t.originalParameters);
            t.grid.model.store.setData(oldParms);
            t.grid.body.refresh();

            // Restore the original sort order of the grid as on entry
            // to the dialog.
            t.grid.sort.sort(ID.PARM_GRID_NAME, false);

            // Hide the editing grid and display the read-only grid.
            var editableGrid = dom.byId(ID.EDIT_PARM_GRID_ID);
            if (editableGrid) {
              domStyle.set(dom.byId(ID.EDIT_PARM_GRID_ID), "display", "none");
              t.editingGrid.destroy();
              t.editingGrid = null;
            }
            domStyle.set(dom.byId(ID.PARM_GRID_ID), "display", "block");

          }

          // Hide any error message from the editable grid if it was displayed.
          domStyle.set(dom.byId("errMessageDiv"), "display", "none");

        } else {
          // *** ALLOW EDITING IN THE TABLE
          e.target.setAttribute('aria-checked', 'false');

          // Reset variables used to manage the editing Grid
          var origDataCopy = lang.clone(t.originalParameters);
          var editingParmStore = new MemoryStore({
              idProperty: 'parmId',
              data: origDataCopy
          });
          t.nextIndex = t.originalParameters.length;

          var addParmSVG = imgUtils.getSVGSmall('add-new', 'newParm', i18n.PARM_ADD_ICON_TITLE, i18n.JOB_PARAMETER_CREATE_BUTTON);
          var addParmSVGInsert = '<span class="parmGridAddSVGInMsg">' + addParmSVG + '</span>';
          var emptyInfoMsg = "<div>" + lang.replace(i18n.JOB_PARAMETER_CREATE, [addParmSVGInsert]) + "</div>";
          
          var columns = [
                         {id: ID.PARM_GRID_NAME,
                          field: 'parmName',
                          name: t.parm_column_header,
                          width: '40%',
                          editable: true,
                          editorArgs: {
                            props: 'placeholder:"' + t.parameter_name_placeholder + '", "aria-Label":"' + t.parameter_name_placeholder + '"'
                          },
                          alwaysEditing: true
                         },
                         {id: ID.PARM_GRID_VALUE,
                          field: 'parmValue',
                          name: t.value_column_header,
                          width: '55%',
                          editable: true,
                          editor: AutoTextarea,
                          editorArgs: {
                            props: 'placeholder:"' + t.parameter_value_placeholder + '", renderAsHtml: false, style: "padding: 5px 10px 4px 10px; font-size: 14px;", "aria-Label":"' + t.parameter_value_placeholder + '"'
                          },
                          editorIgnoresEnter: true,
                          alwaysEditing: true
                         },
                         {id: ID.PARM_GRID_ACTIONS,
                          field: 'parmId',
                          name: '',
                          width: '5%',
                          sortable: false,
                          allowEventBubble: true,
                          widgetsInCell: true,
                          style: 'vertical-align: middle',
                          decorator: function(s) {
                            return "<div style='text-align: center;'><span data-dojo-attach-point='action'></span></div>";
                          },
                          getCellWidgetConnects: function(cellWidget, cell) {
                            // Return an array of connection arguments for this CellWidget.
                            // With this method, CellWidget will take care of connecting/disconnecting
                            // all events for you, so you don't have to worry about memory leaks.
                            var parmId = cell.data();
                            return [
                              [cellWidget.removeButton, 'onClick', function(evt) {
                                t.removeParmField(parmId);
                              }],
                              [cellWidget.removeButton, 'onKeyDown', function(evt) {
                                if (evt.keyCode === keys.ENTER || evt.keyCode === keys.SPACE) {
                                  t.removeParmField(parmId);
                                } else if (evt.keyCode === keys.ESCAPE) {
                                    // Don't exit the dialog! ....instead stop the event and 
                                    // put focus back on the cell instead of the button itself.
                                    t.editingGrid.focus.stopEvent(evt);
                                    t.editingGrid.focus.focusArea('body');
                                }
                              }]
                            ];
                          },
                          initializeCellWidget: function (cellWidget, cell) {
                            var parmId = cell.data();
                            var title = t.parm_remove;
                            var buttonId = "parmGridRemove-" + parmId;
                            var button = registry.byId(buttonId);

                            if (!button) {
                              cellWidget.removeButton = new Button({
                                id: buttonId,
                                label: title,
                                showLabel: false,
                                iconClass: 'parmGridActionsIcon',
                                baseClass: 'parmGridRemoveActionButton',
                                postCreate: function() {
                                  this.iconNode.innerHTML = imgUtils.getSVGSmall('remove', 'removeParm');
                                }
                              });
                            } else {
                              cellWidget.removeButton = button;
                            }
                            // Remove any remove button that was previously attached to action
                            domConstruct.empty(cellWidget.action);
                            cellWidget.removeButton.placeAt(cellWidget.action);
                            cellWidget.removeButton.startup();
                          },
                          uninitializeCellWidget: function(cellWidget, cell) {
                            var buttonId = "parmGridRemove-" + cell.data();
                            var button = registry.byId(buttonId);
                            if (button) {
                              button.destroy();
                            }
                          }
                         }
                       ];

          t.editingGrid = new Grid({
            id: ID.EDIT_PARM_GRID_ID,
            cacheClass: cacheClass,
            store: editingParmStore,
            structure: columns,
            baseClass: 'restartParmsGrid',
            columnWidthAutoResize: true,
            modules: [
              CellWidget,
              {
                moduleClass: Edit,
                _doBlur: t._doBlur
              },
              {
                moduleClass: SingleSort,
                initialOrder: {colId: ID.PARM_GRID_NAME, descending: false}
              },
              {
                moduleClass: HeaderRegions,
                destroy: function() {
                  var t = this;
                  var _arguments = arguments;
                  _arguments.callee.nom = "destroy";
                  t.inherited(_arguments);
                  // Destroy the 'add parm' button added to the header region
                  var addParmButton = registry.byId(ID.PARM_GRID_ADD_BUTTON);
                  if (addParmButton) {
                    addParmButton.destroyRecursive();
                  }
                }
              }
            ],
            style: "border: 2px solid #7CC7FF",
            bodyEmptyInfo: emptyInfoMsg,
            markEmptyEntries: t.markEmptyEntries
          });

          // Add the 'Add parameter' action button in the header
          t.editingGrid.headerRegions.add(function(col){

            if (col.id === ID.PARM_GRID_ACTIONS) {
              // Create the '+' button
              var addParmButton = new Button({
                id: ID.PARM_GRID_ADD_BUTTON,
                label: i18n.PARM_ADD_ICON_TITLE,
                showLabel: false,
                iconClass : 'parmGridActionsIcon',
                baseClass : 'parmGridAddActionButton',
                postCreate: function(){
                  this.iconNode.innerHTML = imgUtils.getSVGSmall('add-new', 'newParm');  // ADD TITLE AND ARIA-LABEL??
                },
                onClick: lang.hitch(t, t.addNewParmFields)
              });

              t.editingGrid.on("headerCellKeyDown", lang.hitch(t.editingGrid, function(evt) {
                if (evt.keyCode === keys.ENTER || evt.keyCode === keys.SPACE) {
                  if (evt.columnId === ID.PARM_GRID_ACTIONS) {
                    // Event was to add a new parameter field to the editing grid
                    t.editingGrid.store.add({parmId: t.nextIndex++,
                                             parmName: "",
                                             parmValue: ""
                                            });
                    evt.stopPropagation();
                  }
                }
              }));

              return addParmButton.domNode;
            }
          }, 0, 1, 0);

          t.editingGrid.headerRegions.refresh();

          // The last column containing the 'Add Parameter' action button does not have a name (label).
          // However, when traversing the table header with key strokes the spot where the label
          // would be is marked as a focusNode in the HeaderRegions of the grid and is highlighted.
          // Setting this unused node (class=.gridxSortNode) to display=none will remove it from
          // being focused on when traversing the gridx header with the keyboard.
          var columnSelectionHeaderDomNode = t.editingGrid.header.getHeaderNode(ID.PARM_GRID_ACTIONS);
          var labelNode = query('.gridxSortNode', columnSelectionHeaderDomNode)[0];
          domStyle.set(labelNode, "display", "none");

          // This was needed so that when using the mouse to single click between fields focus is
          // not erroneously reset to a previous field and input is added to the wrong field.
          // The onCellClick event seems not necessary anymore and is causing the disappearance of 
          // the parameter value 
//          t.editingGrid.edit.aspect(t.editingGrid, 'onCellClick', function(evt){
//              t.editingGrid.edit._onUIBegin(evt);
//          });
          
          t.connect(t.editingGrid.body, 'onAfterRow', function(row) {
//              console.log("In my onafterrow with " );
              var rowData = row.rawData();
//              console.log(rowData);
//              if (rowData.parmName === "" && rowData.parmValue !== ""){
//                  console.log("message should be posted");
//              }
              this.editingGrid.markEmptyEntries(rowData, row);
          });
          
          t.connect(t.editingGrid.edit, 'onApply', function(cell, applySuccess) {
//              console.log("in my onApply");
//              console.log(cell.row.rawData());
              var rowData = cell.row.rawData();
//              if (rowData.parmName ==="" && rowData.parmValue !== "") {
//                  console.log("message should be posted");
//              }
              this.editingGrid.markEmptyEntries(rowData, cell.row);
          });
          
          // Hide the read-only grid and display the editing grid.
          domStyle.set(dom.byId(ID.PARM_GRID_ID), "display", "none");
          t.editingGrid.placeAt(t.parmGridDiv, "first");

        }
      };

      // Add a tooltip for the Job Parameters table
//      var jpTooltip = new Tooltip ({
//        connectId: [ID.JOB_PARMS_EDIT_AREA],
//        label: lang.replace(i18n.JOB_PARAMETERS_TOOLTIP, [i18n.REUSE_PARMS_TOGGLE_LABEL])
//      });

    },

    // This is copied from the gridx Edit module.   We needed to update to change the 
    // tabbing behavior.   With the Edit module, the TAB will go between displayed editors
    // only.   We needed to also include the 'Remove Parameter' button (- sign) in the 
    // last column of each row in the tabbing sequence.
    _doBlur: function(evt, step){
      var t = this,
          g = t.grid,
          view = g.view,
          body = g.body;
      if (t._editing && step) {
        var cellNode = g.body.getCellNode({
          rowId: t._focusCellRow,
          colId: t._focusCellCol
        });
        // Get navigable elements within a cell in case there is more than 1.
        var elems = a11y._getTabNavigable(cellNode);
        if(evt && ((!elems.first && !elems.last) || evt.target === (step < 0 ? elems.first : elems.last))) {
          var rowIndex, colIndex, dir, checker;
          if (evt.columnId === 'value') {
//            g.focus.stopEvent(evt);   THIS DOES NOT WORK HERE
//            console.log("doBlur: Blurred on value column....pass focus through to actions button");
            rowIndex = view.getRowInfo({parentId: t.model.parentId(t._focusCellRow),
                                            rowIndex: t.model.idToIndex(t._focusCellRow)
                                           }).visualIndex,
                colIndex = g._columnsById[t._focusCellCol].index,
                dir = step > 0 ? 1 : -1,
                checker = function(r, c) {
                  return g.navigableCell._isNavigable(g._columns[c].id);
                };
                body._nextCell(rowIndex, colIndex, dir, checker).then(function(obj) {
//                  console.log("doBlur: body._nextCell to determine next cell");
//                  console.log("doBlur: then invoke ApplyAll for current change");
                  t._applyAll();
                  t._focusCellCol = g._columns[obj.c].id;
                  var rowInfo = view.getRowInfo({visualIndex: obj.r});
                  t._focusCellRow = t.model.indexToId(rowInfo.rowIndex, rowInfo.parentId);
                  //This breaks encapsulation a little....
                  body._focusCellCol = obj.c;
                  body._focusCellRow = obj.r;
//                  t.begin(t._focusCellRow, t._focusCellCol);
                });
          } else {
//            console.log("doBlur: Blurred on name column.  Stop event and move to value column.");
            g.focus.stopEvent(evt);
            rowIndex = view.getRowInfo({parentId: t.model.parentId(t._focusCellRow),
                                            rowIndex: t.model.idToIndex(t._focusCellRow)
                                           }).visualIndex,
                colIndex = g._columnsById[t._focusCellCol].index,
                dir = step > 0 ? 1 : -1,
                checker = function(r, c) {
                  return g._columns[c].editable;
                };
//            console.log("doBlur: body._nextCell to determine next cell");
            body._nextCell(rowIndex, colIndex, dir, checker).then(function(obj) {
//              console.log("doBlur: then invoke ApplyAll for current change and Begin for next editable cell");
              t._applyAll();
              t._focusCellCol = g._columns[obj.c].id;
              var rowInfo = view.getRowInfo({visualIndex: obj.r});
              t._focusCellRow = t.model.indexToId(rowInfo.rowIndex, rowInfo.parentId);
              //This breaks encapsulation a little....
              body._focusCellCol = obj.c;
              body._focusCellRow = obj.r;
//              console.log("Invoke Begin for editor in next cell " + t._focusCellCol);
              t.begin(t._focusCellRow, t._focusCellCol);
            });
          }
        }
        return false;
      }
      return true;
    },

    addNewParmFields: function(e) {
      var t = this;            // The dialog
      // Event was to add a new parameter field to the editing grid
      var newEntryRowId = t.editingGrid.store.add({parmId: t.nextIndex++,
                               parmName: "",
                               parmValue: ""
                              });
    },

    removeParmField: function(rowId) {
      var t = this;
      t.editingGrid.store.remove(rowId);
    },

    show: function() {
      this.inherited(arguments);
      // Place the grid on the dialog AFTER the dialog has been sized
      // so the grid can display with its data.  Prior to this point,
      // the size of the dialog was unknown (zero) and the grid data
      // did not show since the data in the grid will only show if the
      // grid has a size. Since the grid size is a percentage of
      // its container size and the container size was zero (0), prior
      // to this point it had not yet been given a size.
      this.grid.placeAt(this.parmGridDiv);
    },
    
    markEmptyEntries: function(rowData, row) {
      var blankParmName = false;
      var rowNode = row.node();

      if (rowData.parmName === "") {
        if (rowData.parmValue === "") {
          // Blanking out the row is allowed.
          domStyle.set(rowNode, "border", "none");
        } else {
          // Error if parameter Name is blank but the parameter value is not.
          domStyle.set(rowNode, "border", "1px solid #a91024");
          blankParmName = true;
        }
      } else {
        // Value in parameter Name field now, so make sure it is not marked as error
        domStyle.set(rowNode, "border", "none");  
      }
      
      // See if we need to post the error message on the blank parm name...
      // message should be posted if error is on this row or any other row.
      if (!blankParmName) {
        var rows = row.grid.rows();
        for (var i=0; i < rows.length; i++) {
            if (rows[i].id === row.id) {     // We already processed this row...
              continue;
            }
            rowData = rows[i].rawData();
            if (rowData.parmName === ""  &&  rowData.parmValue !== "") {
                blankParmName = true;
                break;
            }
        }          
      }
      
      if (blankParmName) {          
        var errorIcon = dom.byId("entryErrorIcon");
        errorIcon.innerHTML = imgUtils.getSVGSmall("submission-error", "submission-error");
        var errorMessage = dom.byId("restartJobDlgErrMsg");
        errorMessage.innerHTML = i18n.PARMS_ENTRY_ERROR;
        domStyle.set(dom.byId("errMessageDiv"), "display", "block");
      } else {
        // Hide the error message
        domStyle.set(dom.byId("errMessageDiv"), "display", "none");
      }
    },
      
    /**
     * This method is called by the okFunction() common to js/widgets/ConfirmDialog.
     * It will validate the input in the jobParameter input field.   If valid, it
     * will forward the Restart request to the action method identified.  If not,
     * it will post a generic error message on the dialog to inform the user.
     *
     * An entry that is not valid is one in which the parameter name is blank.
     * Currently, this restriction is not held by the API, but is being enforced by
     * the UI since it doesn't make sense to submit a parameter that you can not
     * reference.
     *
     * @param type        Job Instance = "inst"  or  Job Execution = "execution"
     * @param elementId   ID of the Job instance or execution on which to perform the action.
     * @param actionOperation  Function to perform if action is confirmed and input
     *                         is valid.
     */
    processRestart: function(type, jobId, actionOperation){
      var t = this;

      var reuseParmsCheckbox = dom.byId(ID.REUSE_PARMS_TOGGLE_ID);
      if (reuseParmsCheckbox.checked || !t.editingGrid) {
        // Re-using the parameters from the previous execution.

        // Destroy the dialog to allow processing to continue.
        t.okSignal.remove();
        t.destroyRecursive();

        // Forward the request to the processing method with JobParms set to NULL
        // to indicate that they do not need to be updated.
        actionOperation(type, jobId, null);

      } else {
        var submitRequest = true;

        // Convert all the values from the table into a JSON object.
        var parmRows = t.editingGrid.rows();
        var jobParameterObj = {};
        for (var i=0; i<parmRows.length; i++) {
          var data = parmRows[i].data();       // Get the data for the row
          if (data.parmName === "" && data.value === "") {
            continue;                          // Spare row added for new parms
          } else {
            if (data.parmName !== "") {
              // Valid key/value pair
              jobParameterObj[data.parmName] = data.value;
            } else {
              // The parameter name value is blank.
              // Flag this as an error.
              console.log("Job Parameter name missing for row " + i);
              submitRequest = false;

              t.editingGrid.row(i).node().style.border = "1px solid #a91024";
            }
          }
        }

        if (submitRequest) {
          // Destroy the dialog to allow processing to continue.
          t.okSignal.remove();
          t.destroyRecursive();

          actionOperation(type, jobId, jobParameterObj);
        } else {
          var errorIcon = dom.byId("entryErrorIcon");
          errorIcon.innerHTML = imgUtils.getSVGSmall("submission-error", "submission-error");
          var errorMessage = dom.byId("restartJobDlgErrMsg");
          errorMessage.innerHTML = i18n.PARMS_ENTRY_ERROR;
          domStyle.set(dom.byId("errMessageDiv"), "display", "block");
          // Stay on the dialog
        }

      }
    }

  });
});
