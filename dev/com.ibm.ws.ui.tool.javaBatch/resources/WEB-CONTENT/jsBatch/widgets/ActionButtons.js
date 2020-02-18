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

define([ 'dojo/_base/lang', 'dojo/_base/window', 
         'dojo/aspect', 'dojo/dom', 'dojo/on',
         'dojo/dom-attr', 'dojo/dom-class',
         'dojox/layout/TableContainer', 'dojox/mobile/Button',
         'dijit/registry',
         'dijit/form/DropDownButton', 'dijit/TooltipDialog', 
         'js/widgets/ConfirmDialog',
         'js/widgets/MessageDialog',
         'jsBatch/utils/restUtils', 'jsBatch/utils/ID',
         'jsBatch/widgets/PurgeDialog', 'jsBatch/widgets/RestartDialog',
         'jsShared/utils/imgUtils',
         'dojo/i18n!jsBatch/nls/javaBatchMessages'
       ],
function(lang, win, aspect, dom, on,
         domAttr, domClass,
         TableContainer, Button,
         registry,
         DropDownButton, TooltipDialog,
         ConfirmDialog, MessageDialog,
         restUtils, ID, 
         PurgeDialog, RestartDialog,
         imgUtils,
         i18n) {

  'use strict';

  var restartLabel = i18n.RESTART;
  var stopLabel = i18n.STOP;
  var purgeLabel = i18n.PURGE;
  
  var restartActionIcon = __createImgTag("actionMenu-restart", restartLabel);
//  var restartActionDisabledIcon = __createImgTag("actionMenu-restart-disabled", restartLabel);

  var stopActionIcon = __createImgTag("actionMenu-stop", stopLabel);
//  var stopActionDisabledIcon = __createImgTag("actionMenu-stop-disabled", stopLabel);
  
  var purgeActionIcon = __createImgTag("actionMenu-purge", purgeLabel);

  function __createImgTag(icon, label) {
    var spanOpen = "<span>";
    var spanClose = "</span>";
    return spanOpen + imgUtils.getSVGSmall(icon, icon) + label + spanClose;
  }

  /** 
   * Create the Action Button that will open a dropDownMenu when selected.  The
   * menu will contain a set of actions relevant to the selected element.
   * 
   * @param type         Job Instance = "inst"  or  Job Execution = "execution"
   * @param elementId    Instance or Execution ID
   * 
   * @returns  DropDownButton
   */
  function _createActionDropDownButton(type, elementId) {
    var thisButtonId = ID.ACTION_BUTTON + '-' + type + '-' + elementId;

    var existingButton = registry.byId(thisButtonId);
    if (existingButton) {
      existingButton.destroy();
    }
    
    var message = "";
    var menuMessage = "";
    
    if (type === "inst") {              // Job Instance
      message = lang.replace(i18n.INSTANCE_ACTIONS_BUTTON_LABEL, [elementId]);
      menuMessage = i18n.INSTANCE_ACTIONS_MENU_LABEL;
    } 
//    else if (type == "execution") {   // Job Execution
//      message = lang.replace(i18n.EXECUTION_ACTIONS_BUTTON_LABEL, [elementId]);
//      menuMessage = i18n.EXECUTION_ACTIONS_MENU_LABEL
//    }
    
    var actionButton = new DropDownButton({
      id: thisButtonId,
      "aria-label": message,
      "aria-haspopup": true,
      title: message,          // Hover over button text displayed
      iconClass: 'listViewActionDropDownIcon',
      baseClass: 'listViewActionDropDown',
      label: message,
      showLabel: false
    });
   
    on(actionButton, "mouseover", function() {
      _showHoverIcon(actionButton, true);
    });
    on(actionButton, "mouseout", function() {
      _showHoverIcon(actionButton, false);
    });
    
    // Add an aspect to allow us to fire before the openDropDown method
    aspect.before(actionButton, "toggleDropDown", function() {
      // Bind in the action bar dropdown, but only once the dropdown button is clicked
      if (actionButton.dropDown === undefined || actionButton.dropDown === null) {
        actionButton.dropDown = _createActionsMenuDropDown(type, elementId, actionButton, menuMessage);
      }
    });

    return actionButton;
  }
  
  /**
   * Shows correct Actions Button icon on hover or select
   * 
   * @param actionButton - The dropdown action Button
   * show - boolean  True: Set hover icon styling on Button
   */
    function _showHoverIcon(actionButton, show) {
      
      if (actionButton !== null) {
        if (show) {
          actionButton.set("iconClass", "listViewActionDropDownHoverIcon");
        } else {
          var dropdown = registry.byId(actionButton.id + "-actionMenu");
          // dropdown.closing was added for the case when the menu was displayed
          // and then the user selected a different actionButton to 
          // display a new menu.  At this time, dropdown.focused is still set for
          // the first actionButton's dropdown menu since closing has not completed.
          if (!(dropdown && dropdown.focused) || dropdown.closing) {
            actionButton.set("iconClass", "listViewActionDropDownIcon");
          }
        }
      }
  }
  
  /**
   * Creates the dropdown menu displayed after selecting the Actions Button for the
   * associated element (Job Instance).  The menu will contain a list
   * of actions that can be performed on the associated element.
   * 
   * @param type            Job Instance = "inst"  or  Job Execution = "execution"
   * @param elementId       Job Instance or Execution id
   * @param dropDownParent  The parent button connected with this dropdown. 
   *                        When selected it will display the dropdown menu.
   * @param message         Accessibility message to provide for the dropdown menu       
   * 
   * @return A TooltipDialog that contains the actions menu for the element.               
   */
  function _createActionsMenuDropDown(type, elementId, dropDownParent, message) {
    var myRootId = dropDownParent.id + '-actionMenu';

    // Create the ToolTipDialog which will hold the actions dropDown menu.  
    var actionMenuDialog = new TooltipDialog({
      id: myRootId,
      baseClass : 'actionMenu',
      title: message,   
      "aria-labelledby": myRootId,
      "aria-label": message,
      style: 'display: inline;',
      content: ''
    });
    
    var tableContainer = _createActionTable(actionMenuDialog);
    actionMenuDialog.addChild(tableContainer);
    
    // RESTART action
    var restartButton = _createActionButton('RESTART', myRootId, type, elementId, function(me) {
      var doRestart = function(type, elementId, jobParms) {
        console.log("Restart " + type + " with ID " + elementId);        
        if (jobParms) {
          var jobParmsValue = JSON.stringify({jobParameters: jobParms});
          restUtils.put(restUtils.JOB_INSTANCE_RESTART_PARMS, [elementId], jobParmsValue).then(function(response){
            // Update the grid row and associated execution grid, if expanded.
            var rowId = elementId;
            console.log("Restart of " + type + " " + elementId + " successful.  Update the row to show new status.");
            var jobInstanceGrid = registry.byId(ID.JOBINSTANCE_GRID);
            jobInstanceGrid.updateInstanceRow(rowId);
            
            if (jobInstanceGrid.executionGrids[elementId]) {
              // The Execution Grid for this Job Instance was showing. Update it
              // to show the new execution.
              var executionGrid = jobInstanceGrid.executionGrids[elementId];
              executionGrid.__updateExecutionGrid();
            }
          },
          function(err) {
            showFailedActionDialog('RESTART', type, elementId, err);
          });
        } else {
          // Issue Restart re-using previous parms
          restUtils.put(restUtils.JOB_INSTANCE_RESTART,[elementId]).then(function(response){
            // Update the grid row and associated execution grid, if expanded.
            var rowId = elementId;
            console.log("Restart of " + type + " " + elementId + " successful.  Update the row to show new status.");
            var jobInstanceGrid = registry.byId(ID.JOBINSTANCE_GRID);
            jobInstanceGrid.updateInstanceRow(rowId);
            
            if (jobInstanceGrid.executionGrids[elementId]) {
              // The Execution Grid for this Job Instance was showing. Update it
              // to show the new execution.
              var executionGrid = jobInstanceGrid.executionGrids[elementId];
              executionGrid.__updateExecutionGrid();
            }
          },
          function(err) {
            showFailedActionDialog('RESTART', type, elementId, err);
          });
        }
               
      };
      /** Construct the confirmation dialog. Restart is only done if confirmed **/
      _confirmActionDialog('RESTART', myRootId, type, elementId, doRestart);
    });

    // STOP action
    var stopButton = _createActionButton('STOP', myRootId, type, elementId, function(me) {
      var doStop = function(type, elementId) {
        console.log("Stop " + type + " with ID " + elementId);
        restUtils.put(restUtils.JOB_INSTANCE_STOP,[elementId]).then(function(response){
          // Update the grid row and associated execution grid, if expanded.
          var rowId = elementId;
          console.log("Stop request for " + type + " " + elementId + " successful.  Update the row to show new status.");
          var jobInstanceGrid = registry.byId(ID.JOBINSTANCE_GRID);
          jobInstanceGrid.updateInstanceRow(rowId);                  

          if (jobInstanceGrid.executionGrids[elementId]) {
            // The Execution Grid for this Job Instance was showing. Update it
            // to show the new execution status.
            var executionGrid = jobInstanceGrid.executionGrids[elementId];
            executionGrid.__updateExecutionGrid();
          }
        },
        function(err) {
          showFailedActionDialog('STOP', type, elementId, err);
        });
      };

      /** Construct the confirmation dialog. Stop is only done if confirmed **/
      _confirmActionDialog('STOP', myRootId, type, elementId, doStop);
    });

    // PURGE action
    var purgeButton = _createActionButton('PURGE', myRootId, type, elementId, function(me) {
      var doPurge = function(type, elementId) {
        console.log("Purge " + type + " with ID " + elementId);
        
        var jobStoreOnlyButton = dom.byId(ID.JOB_STORE_ONLY_TOGGLE_ID);
        var jobStoreOnlySetting = false;
        if (jobStoreOnlyButton) {
          jobStoreOnlySetting = jobStoreOnlyButton.checked;
        }
        
        restUtils.deleteWithParms(restUtils.JOB_INSTANCE_DELETE,[elementId, jobStoreOnlySetting]).then(function(response){
          // Update the grid row and associated execution grid, if expanded.
          var rowId = elementId;
          // With the V1 delete API, removing a job that is currently 
          // executing (dispatched) will fail with a status code of 400
          // and fall into the failed path below.  The message simply 
          // says 'Delete request not supported'.
          
          // With the V2 delete API, removing a job that is currently
          // executing (dispatched) will actually return 200, but the
          // response will have a purgeStatus field.  I have seen this
          // field with values of 'COMPLETED' and 'FAILED', even though
          // the API doc indicates other values may be returned.
          // There is also a message field that provided an explanation.
          // I'm not sure if the message will always be there or if 
          // the NOT_LOCAL will be a problem for us.
          
          if (response && response.data) {
              var trueResponse = JSON.parse(response.data);
              
              if (trueResponse instanceof Array  && trueResponse.length > 0) {
                  trueResponse = trueResponse[0];
                  
                  if (trueResponse.purgeStatus && trueResponse.purgeStatus !== 'COMPLETED') {
                      // If not completed, then an error occurred.  Post the message...
                      showFailedActionDialog('PURGE', type, elementId, trueResponse.message);
                      
                      return;
                  }
              }
              //*** Note that occasionally the API will return nothing in the
              // response ( [] ).   So you need to check for that too and assume
              // that means successful.

              
          } // else, assume purge request worked...
           
          console.log("Purge request for " + type + " " + elementId + " successful.");
          var jobInstanceGrid = registry.byId(ID.JOBINSTANCE_GRID);
                    
          jobInstanceGrid.store.remove(rowId);          
        },
        function(err) {
          showFailedActionDialog('PURGE', type, elementId, err);
        });
      };

      /** Construct the confirmation dialog. Purge is only done if confirmed **/
      _confirmActionDialog('PURGE', myRootId, type, elementId, doPurge);
    });

    // Add buttons in correct order
    tableContainer.addChild(restartButton);
    tableContainer.addChild(stopButton);
    tableContainer.addChild(purgeButton);
          
    // Bind the buttons into the dialog
    actionMenuDialog.buttons = {};
    actionMenuDialog.buttons.restartButton = restartButton;
    actionMenuDialog.buttons.stopButton = stopButton;
    actionMenuDialog.buttons.purgeButton = purgeButton;

    // When the ActionBar is opened, show the action Button as selected.
    actionMenuDialog.on('open', function(evt) {
      _showHoverIcon(dropDownParent, true);
    });

    // When the ActionBar is closed, we may need to reset the positional information of the 
    // dialog and the "V" connector.
    actionMenuDialog.on('close', function(evt) {
      //Change action icon back to original 
      this.closing = true;
      _showHoverIcon(dropDownParent, false);
      this.closing = false;
      
      // Set the dialogOpen to false, so that we will do the re-positioning if we
      // need to put the dialog above the button the next time we display it.
      this.dialogOpen = false;
    });

    return actionMenuDialog;
  }

  /**
   * Create a TableContainer to hold the dropdown menu items in 
   * a Table element.
   * 
   * @returns TableContainer
   */
  function _createActionTable() {
    
    var tableContainer = new TableContainer({
      cols : 1,
      showLabels : false,
      'class' : "tableContainerDropDownActionButton"
    });
    
    tableContainer.layout = function() {
      TableContainer.prototype.layout.apply(this);
      // should ALWAYS have a table but check first just in case
      if (this.table) {
        domAttr.set(this.table, "role", "presentation");
      }
    };

    return tableContainer;  
  }
  
  /**
   * Common method to create an 'ActionButton' (ie. 'STOP', 'RESTART', 'PURGE')
   * 
   * @param action      Activity to be performed.   'STOP', 'RESTART', 'PURGE'
   * @param rootId      Root of ID to give to action button created.  For ID creation.
   * @param type        Job Instance = "inst"  or  Job Execution = "execution"
   * @param elementId   ID of the Job instance or execution on which to perform the action.
   * @param assignedAction  Function to perform when selected
   * 
   * @return Button representing an action.
   */
  function _createActionButton(action, rootId, type, elementId, assignedAction) {
    // Validate the activity...
    var assignedId, enabledIcon, titleval;
    switch (action) {
      case 'RESTART':
        assignedId = rootId + '-restartButton';
        enabledIcon = restartActionIcon;
        titleval = restartLabel;
//        disabledIcon = restartActionDisabledIcon;
        break;
      case 'STOP':
        assignedId = rootId + '-stopButton';
        enabledIcon = stopActionIcon;
        titleval = stopLabel;
//        disabledIcon = stopActionDisabledIcon;
        break;   
      case 'PURGE':
        assignedId = rootId + '-purgeButton';
        enabledIcon = purgeActionIcon;
        titleval = purgeLabel;
        break;
      default:  
        console.error("Unknown action, " + action + ", for element, " + type + elementId + ".  Do not add to action menu.");
        return;
    }

    var actionButton = new Button({
      id: assignedId,      
      label: enabledIcon,
      title: titleval,
      baseClass: 'dropDownActionButtons',
      onClick: assignedAction,
      "aria-label": titleval
    }, name);

    // Bind in the focus behaviors
    on(actionButton.domNode, "focus", function() {
      domClass.add(actionButton.domNode, "dropDownActionButtonFocused");
    });
    on(actionButton.domNode, "focusout", function() {
      domClass.remove(actionButton.domNode, "dropDownActionButtonFocused");
    });    

    return actionButton;
  }
 
  /**
   * Display the popup confirmation dialog for the action selected
   * 
   * @param action      Activity to be performed.   'STOP', 'RESTART', 'PURGE'
   * @param rootId      Root of ID to give to action dialog created.  For ID creation.
   * @param type        Job Instance = "inst"  or  Job Execution = "execution"
   * @param elementId   ID of the Job instance or execution on which to perform the action.
   * @param actionOperation  Function to perform if action is confirmed
   */
  function _confirmActionDialog(action, rootId, type, elementId, actionOperation) {
    var confirmationPaneContentMsg = _getConfirmationMessage(action, type, elementId);
    var confirmationButtonLabel = _getConfirmationButtonLabel(action);
    var dialogId = rootId + '-actionConfirmationPopup';

    var confirmationDialog = registry.byId(dialogId);
    if(confirmationDialog){
      confirmationDialog.destroy();
    }
    
    if (action === 'PURGE') {
      confirmationDialog = new PurgeDialog({
        id : dialogId,
        baseClass: "confirmDialog acDialog acDialogNoTitle",
        title : '',
        confirmDescriptionIcon : imgUtils.getSVGSmallName('status-alert'),
        confirmDescription : confirmationPaneContentMsg,
        confirmMessage : '',
        confirmButtonId : rootId + '-confirmationButton',
        confirmButtonLabel : confirmationButtonLabel,
        displayXButton : true,
        redButton : true,
        okFunction : function() {
          actionOperation(type, elementId);
        }
      });
    } else if (action === 'RESTART') {
      if (type === "inst") {
        // We need to display the parameters used in the last execution of this job instance.
        // Query for the executions associated with the instance.  
        restUtils.getWithParms(restUtils.JOB_EXECUTIONS_URL_QUERY,[elementId]).then(function(response){
          // An array of executions is returned.   The one at index [0] represents the 
          // most recent execution of this job.
          var json = JSON.parse(response.data);
          var mostRecentExecution = json[0];

          confirmationDialog = new RestartDialog({
            id : dialogId,
            baseClass: "confirmDialog acDialog acDialogNoTitle",
            title : '',
            confirmDescription : confirmationPaneContentMsg,
            confirmMessage : '',
            confirmButtonId : rootId + '-confirmationButton',
            confirmButtonLabel : confirmationButtonLabel,
            displayXButton : true,
            redButton : action === 'STOP'? true: false,
            okFunction : function() {
              this.processRestart(type, elementId, actionOperation);
            },
            jobParameters: mostRecentExecution.jobParameters
          });      
          confirmationDialog.placeAt(win.body());
          confirmationDialog.startup();
          confirmationDialog.show();
        },
        function(err) {
          console.error("Unable to retrieve parameters used in last execution for job " + elementId);
          // Show generic confirmation dialog which will process the restart  
          // reusing the parameters used in the last execution of the job.
          confirmationDialog = new ConfirmDialog({
            id : dialogId,
            baseClass: "confirmDialog acDialog acDialogNoTitle",
            title : '',
            confirmDescriptionIcon : imgUtils.getSVGSmallName('status-alert'),
            confirmDescription : confirmationPaneContentMsg,
            confirmMessage : '',
            confirmButtonId : rootId + '-confirmationButton',
            confirmButtonLabel : confirmationButtonLabel,
            displayXButton : true,
            redButton : false,
            okFunction : function() {
              actionOperation(type, elementId);
            }
          });          
          confirmationDialog.placeAt(win.body());
          confirmationDialog.startup();
          confirmationDialog.show();        
        });
      }
      return;
    } else {
      // Action is 'STOP'
      confirmationDialog = new ConfirmDialog({
        id : dialogId,
        baseClass: "confirmDialog acDialog acDialogNoTitle",
        title : '',
        confirmDescriptionIcon : imgUtils.getSVGSmallName('status-alert'),
        confirmDescription : confirmationPaneContentMsg,
        confirmMessage : '',
        confirmButtonId : rootId + '-confirmationButton',
        confirmButtonLabel : confirmationButtonLabel,
        displayXButton : true,
        redButton : true,
        okFunction : function() {
          actionOperation(type, elementId);
        }
      });
    }

    confirmationDialog.placeAt(win.body());
    confirmationDialog.startup();
    confirmationDialog.show();
  }

  /**
   * Returns the confirmation message for the selected action.
   * 
   * @param action      Activity to be performed.   'STOP', 'RESTART', 'PURGE'
   * @param type        Job Instance = "inst"  or  Job Execution = "execution"
   * @param elementId   ID of the Job instance or execution on which to perform the action.
   * @returns   String  Translated confirmation message
   */
  function _getConfirmationMessage(action, type, elementId) {
    var confirmMessage = '';
    if (type === 'inst') {       // Confirmation message for Job Instance
      switch (action) {
        case 'RESTART':
          confirmMessage = lang.replace(i18n.RESTART_INSTANCE_MESSAGE, [elementId]);
          break;
        case 'STOP':
          confirmMessage = lang.replace(i18n.STOP_INSTANCE_MESSAGE, [elementId]);
          break;
        case 'PURGE':
          confirmMessage = lang.replace(i18n.PURGE_INSTANCE_MESSAGE, [elementId]);
          break;
        default:
          console.error("No confirmation message assigned. Unrecognized action, " + action + ".");
      }
    }
    return confirmMessage;
  }
  
  /**
   * Return the translated button label for the selected action.
   * The label is used on the confirmation dialog for the action.
   * 
   * @param    action   Activity to be performed.   'STOP', 'RESTART', 'PURGE'
   * @returns  String   Button label
   */
  function _getConfirmationButtonLabel(action) {
    switch (action) {
      case 'RESTART':
        return restartLabel;
      case 'STOP':
        return stopLabel;
      case 'PURGE':
        return purgeLabel;
      default:
        console.error("No button label assigned.  Unrecognized action, " + action + ".");
        return "";
    }
  }
  
  /**
   * Creates a pop-up with the failed operation message.
   * 
   * @param action      Activity performed.   'STOP', 'RESTART', 'PURGE'
   * @param type        Job Instance = "inst"  or  Job Execution = "execution"
   * @param elementId   ID of the Job instance or execution on which the action was performed.
   * @param err         Response returned from the failed rest call.
   */
  function showFailedActionDialog(action, type, elementId, err) {
    var errorMsg = "";
    if (err && err.response) {
      if (err.response.text) {
        errorMsg = err.response.text;
      } else {
        if (err.response.data && typeof err.response.data === 'string') {
          errorMsg = err.response.data;
        } else {
          // Display an error showing the status code.
          var statusCode = err.response.status;
          errorMsg = lang.replace(i18n.ACTION_REQUEST_ERROR_MESSAGE, [statusCode, err.response.url]);
        }
      }      
    } else {
      // Assume 'err' is the error text to display...
      errorMsg = err;
    }
    
    var msg = "";
    switch (action) {
      case 'RESTART': 
        msg = i18n.RESTART_INST_ERROR_MESSAGE;
        break;     
      case 'STOP':
        msg = i18n.STOP_INST_ERROR_MESSAGE;
        break;
      case 'PURGE':
        msg = i18n.PURGE_INST_ERROR_MESSAGE;
        break;
    }

    var failedDialog = new MessageDialog({
      id : ID.ERROR_DIALOG,
      title : i18n.ERROR,
      messageText : msg,
      additionalText : errorMsg,
      messageDialogIcon : imgUtils.getSVGSmallName('status-alert'),
      okButtonLabel: i18n.OK_BUTTON_LABEL,
      okFunction : function() {
        // Do nothing.
      }   
    });

    failedDialog.placeAt(win.body());
    failedDialog.startup();
    failedDialog.show();
  }

  return {
    createActionDropDownButton: _createActionDropDownButton
  };

});