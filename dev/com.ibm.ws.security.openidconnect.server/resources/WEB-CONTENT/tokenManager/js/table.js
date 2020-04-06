/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

var table = (function() {
    "use strict";

    var tableId = "tm_table";
    var __noQuery = "noQuery";   // Classname marking row that explains you need to search for name
                                 // to see data in the table.

    var __getTableRow = function(authID) {
        var $table = $('#' + tableId + ' tbody');
        var $row = $table.find('input[authid="' + authID + '"]').closest('tr');
        return $row;
    };
    
    /**
     * The batch bar is the blue bar appearing over top of the table which does
     * a batch delete request for all selected rows in the table. 
     */
    var initBatchBar = function() {
        var $tableToolbar = $('.tool_table_toolbar');

        // Initialize number selected elements to 0
        $tableToolbar.find('#batch_selected_msg').text(utils.formatString(messages.ITEMS_SELECTED, [0]));

        // Initialize the batch action buttons
        $tableToolbar.find('#delete_batch_selection').on('click', function() {
            utils.saveFocus($(this));   // Save off button to return focus
                                        // to it when dialog is dismissed.

            __deleteManyAuthentications();
        });
    };

    /**
     * Selects or unselects all the checkboxes in the table and updates the selected
     * number message in the batch bar
     * 
     * @param Boolean selectAll - true: select all the checkboxes in the table; 
     *                            false: unselect all the checkboxes in the table.
     */
    var selectAllAction = function(selectAll) {
        var $table = $('#' + table.tableId + ' tbody');
        if (selectAll) {
            // Unselect all checkboxes in the table         
            $table.find('.tool_checkbox').prop('checked', true).attr('aria-checked', true);
        } else {
            // Select all checkboxes in the table           
            $table.find('.tool_checkbox').prop('checked', false).attr('aria-checked', false);
        }
        updateSelectedMessage();
    };

    /**
     * Updates that number selected message in the batch bar.
     */
    var updateSelectedMessage = function() {
        // Get number of selected checkboxes
        var $table = $('#' + table.tableId);
        var $rowsChecked = $table.find('td.table_column_checkbox input:checkbox:checked');

        // Update the message
        var $tableToolbar = $('.tool_table_toolbar');
        if ($rowsChecked.length === 1) {
            $tableToolbar.find('#batch_selected_msg').text(utils.formatString(messages.SINGLE_ITEM_SELECTED, [$rowsChecked.length]));
        } else {
            $tableToolbar.find('#batch_selected_msg').text(utils.formatString(messages.ITEMS_SELECTED, [$rowsChecked.length]));
        }  
    };

    /**
     * Converts the response object for an authentication to the data needed to display
     * in the table row.
     * This is needed because the field names returned are different between app-passwords
     * and app-tokens.
     * 
     * @param {*} responseObj - object returned from Rest API representing an authentication
     * @param String authType - 'app-password' or 'app-token'
     * 
     * @return {*} authData  of the form: 
     *              {
     *                  "authType": "app-password" OR "app-token",
     *                  "name": "yyyyyyy",
     *                  "authID": "0cc7389f863a47b69c004dd7d21e8a5b",
     *                  "client_name": "Big Application",
     *                  "created_at": "1553204416750",
     *                  "expires_at": "1552390482158"
     *                  "user": "zzzzz"
     *              }
     */
    var convertResponseForTable = function(responseObj, authType) {
        var authData = {  authType: authType,
                              name: responseObj.name,
                            authID: responseObj.app_id,  // ID give by OP to track this authentication
                        created_at: responseObj.created_at,
                        expires_at: responseObj.expires_at,
                              user: responseObj.user
        };

        if (responseObj.used_by) {
            authData.client_name = responseObj.used_by;
        } else {
            authData.client_name = "*";
        }

        return authData;
    };

    /**
     * Create the <tr> and <td> html elements to represent this authentication
     * 
     * @param {*} authData of the form: 
     *              {
     *                  "authType": "app-password" OR "app-token",
     *                  "name": "yyyyyyy",
     *                  "authID": "0cc7389f-863a-47b6-9c00-4dd7d21e8a5b",
     *                  "client_name": "Big Application",
     *                  "created_at": 1553204416750,
     *                  "expires_at": 1552390482158,
     *                  "user": "zzzzz"
     *              }
     * 
     * @returns String - HTML representing a table row
     */
    var __createTableRow = function(authData) {
        // insert bidi text direction to the table row
        var textDir = bidiUtils.getDOMBidiTextDirection(); 
        var selectionAriaLabel = utils.formatString(messages.SELECT_SPECIFIC, [authData.authType, authData.name]);
        var selectBox = "<td class='table_column_checkbox'>" +
                        "   <div class='tool_checkbox_wrapper'>" +
                        "       <input id='select_" + authData.authID + "' class='tool_checkbox' type='checkbox' role='checkbox' aria-checked='false' aria-label='" + selectionAriaLabel + "'>" +
                        "       <div class='tool_checkbox_label' title='" + selectionAriaLabel + "' aria-label='" + selectionAriaLabel + "'></div>" +
                        "   </div>" +
                        "</td>";
        var name = "<td class='authName'" + textDir + " tabindex='-1'>" + authData.name + "</td>";
        var clientName = "<td class='cientName'" + textDir + " tabindex='-1'>" + authData.client_name + "</td>";
        var type = "<td class='authType' tabindex='-1'>" + authData.authType + "</td>";

        // Date data
        var lang = globalization.getLanguageCode();
        var options = { month: 'long', day: 'numeric', year: 'numeric' };
        var issueDate = new Date(authData.created_at);
        var issuedOn = "<td class='authIssued' tabindex='-1'>" + issueDate.toLocaleDateString(lang, options) + "</td>";
        var expiresDate = new Date(authData.expires_at);
        var expiresOn = "<td class='authExpires' tabindex='-1'>" + expiresDate.toLocaleDateString(lang, options) + "</td>";

        // Action button
        var deleteAriaLabel = utils.formatString(messages.DELETE_ARIA, [authData.authType, authData.name]);
        var deleteButton = "<td><input id='delete_" + authData.authID + "' type='button' authID = " + authData.authID + " class='tool_table_button delete_auth_button' value='" + messages.DELETE + "' aria-label='" + deleteAriaLabel + "'></td>";

        // Create a table row and add the filter data attribute as the name lowecased.
        // This is used in sorting and filtering (when implemented).
        var tableRow = "<tr data-filter='" + utils.encodeData(authData.name.toLowerCase()) + "' data-authid='" + authData.authID + "' data-userid='" + authData.user + "'>" + selectBox + name + clientName + type + issuedOn + expiresOn + deleteButton + "</tr>";

        return tableRow;
    };

    /**
     * Add onClick handlers for the row actions
     * 
     * @param {*} authID - unique ID for this app-password/app-token
     */
    var __enableRowActions = function(authID) {
        var $table = $('#' + tableId + ' tbody');

        $(".delete_auth_button[authID='" + authID + "']").click(function(event) {
            event.preventDefault();
            var $this = $(this);
            var $row = $(this).closest('tr');

            utils.saveFocus($this);     // Save which delete button was clicked
                                        // to return focus to it when delete completes

            var authID = $this.attr('authID');
            var name = $row.find('td.authName').text();
            var type = $row.find('td.authType').text();
            var userID = $row.data('userid');

            __deleteAuthentication(authID, name, type, userID);                                      
        });

        // Checkbox to mark row for batch delete
        $('#select_' + authID).parent().on('click keydown', function(event) {
            var toggleCheckbox = true;

            if (event.type === "keydown") {
                var key = event.which || event.keyCode;
                if (key !== 0 && key !== 32) {      // Space key toggles a checkbox
                    toggleCheckbox = false;
                }
            }

            if (toggleCheckbox) {
                var $checkbox = $(this).find('input');
                var checked = $checkbox.prop('checked');

                // Toggle the checkbox value
                $checkbox.prop('checked', !checked).attr('aria-checked', !checked);
                checked = !checked;

                if (!checked) {
                    // If unchecked, unselect the 'select all' button since one checkbox is now not checked
                    $('#tm_select_all').prop('checked', false).attr('aria-checked', false);
                } else {
                    // If all checked, select the 'select all' button too!
                    var $rows = $table.find('tr');
                    var $rowsChecked = $table.find('td.table_column_checkbox input:checkbox:checked');
                    if ($rows.length === $rowsChecked.length) {
                    $('#tm_select_all').prop('checked', true).prop('aria-checked', true);
                    }
                }
                // Update the number selected message in the batch bar
                updateSelectedMessage();
            }
        });
    };

    /**
     * Add a row representing an authentication to the table.
     * 
     * @param {*} authData of the form: 
     *              {
     *                  "authType": "app-password" OR "app-token",
     *                  "name": "yyyyyyy",
     *                  "authID": "0cc7389f-863a-47b6-9c00-4dd7d21e8a5b",
     *                  "client_name": "Big Application", 
     *                  "created_at": 1553204416750,
     *                  "expires_at": 1552390482158,
     *                  "user": "zzzzz"
     *              }
     */
    var addTableRow = function(authData) {
        var tableRow = __createTableRow(authData);

        tableUtils.addToTable(tableId, tableRow, true);

        __enableRowActions(authData.authID);
    };

    /**
     * Displays the correct delete dialog (app-password -vs- app-token) to submit
     * the request.  This is for a single authentication deletion request from the
     * 'Delete' button on the button row.
     * 
     * @param String authID - unique ID for this app-password/app-token
     * @param String name - given name for this authentication (app-password/app-token)
     * @param String type - 'app-password' or 'app-token'
     * @param String userID - owner id of this authentication
     */
    var __deleteAuthentication = function(authID, name, type, userID) {
        // Make sure other dialogs are hidden
        $('.tool_modal_container').addClass('hidden');

        // Find the delete dialog in the html
        var $deleteDlg = $('.tool_modal_container.token_manager_delete');
        var confirmationTitle = utils.formatString(messages.DELETE_FOR_USERID, [utils.encodeData(name), userID]);
        $deleteDlg.find('.tool_modal_title').html(confirmationTitle);

        if (type === 'app-password') {
            $deleteDlg.find('.tool_modal_secondary_title').html(messages.DELETE_PW);
            $deleteDlg.find('.tool_modal_body_description').html(messages.DELETE_WARNING_PW);
        } else if (type === 'app-token') {
            $deleteDlg.find('.tool_modal_secondary_title').html(messages.DELETE_TOKEN);
            $deleteDlg.find('.tool_modal_body_description').html(messages.DELETE_WARNING_TOKEN);
        } else {
            console.log('Bad data for delete.  Type was set to ' + type);
            return;
        }

        // Remove any previous onclick handler for delete button
        $deleteDlg.find('.tool_modal_delete_button').off('click');
        $deleteDlg.find('.tool_modal_delete_button').on('click', function() {
            utils.startProcessingSpinner('delete_processing');
           __deletePWorToken(authID, name, type, userID);
        });

        $deleteDlg.removeClass('hidden');
        $deleteDlg.find('.tool_modal_cancel_button').get(0).focus(); // Set focus to non-destructive button
    };

    var deleteTableRow = function(authID) {
        var $tableRowToBeDeleted = __getTableRow(authID);

        if ($tableRowToBeDeleted.length > 0) {
            // About to delete a row. If we are deleting a row because of selecting
            // 'delete' from a row, focus will no longer be able to return
            // to the delete button on this row. Save off the coordinates (row, col)
            // of the delete button before the row is deleted.  
            var $tableFocusButton = utils.getTopFocusElement();
            var coords = null;
            if ($tableFocusButton.attr('id') !== 'delete_batch_selection') {
                coords = utils.getTableCellCoords($tableFocusButton);
            }

            $tableRowToBeDeleted.remove();
            // Layout the pages of the table again since one row is now missing
            tableUtils.switchPage(tableId, tableUtils.currentPage());
            // Update the number selected message in the batch bar in case we deleted a selected row
            updateSelectedMessage();

            if (coords !== null) {
                // Update the focus button selected from a row since the row was
                // just deleted.
                var id = $tableFocusButton.attr('id');
                if (id && id.indexOf(authID) > 0) {
                    utils.updateFocusAfterDelete(table.tableId, coords.row, coords.col);
                }
            }
        }
    };

    /**
     * Delete the app-password/app-token and remove the row from the table
     * 
     * @param String authID - unique ID for this app-password/app-token
     * @param String name - given name for this authentication (app-password/app-token)
     * @param String authType - 'app-password' or 'app-token'
     * @param String userID - owner id of this authentication
     */
    var __deletePWorToken = function(authID, name, authType, userID) {
        apiUtils.deleteAcctAppPasswordToken(authID, authType, userID).done(function() {
            deleteTableRow(authID);
            // Stop the processing spinner
            utils.stopProcessingSpinner();
            // Dismiss the dialog
            $(".tool_modal_container.token_manager_delete").addClass('hidden');
            utils.returnFocus();
        }).fail(function(errResponse) {
            console.log("delete failed for ID " + authID);
            console.log("errResponse.status: " + errResponse.status + ":   errResponse.statusText: " + errResponse.statusText);
            // The API returns success (200) if you attempt to delete an app-password/app-token
            // that is already deleted.
            // So, if something else happended with the request, put up the generic error message.
            var deleteTypeTitle = authType === 'app-password' ? 'App-Password' : 'App-Token';
            var errTitle = utils.formatString(messages.GENERIC_DELETE_FAIL, [deleteTypeTitle]);
            var errDescription = "";
            if (errResponse.responseJSON && errResponse.responseJSON.error_description) {
                errDescription = errResponse.responseJSON.error_description;
            } else {
                errDescription = utils.formatString(messages.GENERIC_DELETE_FAIL_MSG, [authType, utils.encodeData(name)]);
            }
            utils.showResultsDialog(true, errTitle, errDescription, true, true, false, __reshowDeleteDialog);
        });
    };

    /**
     * Delete all app-passwords and app-tokens for this user and remove all the rows from the table
     * 
     * @param String userID - owner of the authentications shown in the table.
     */
    var __deleteAllAuthentications = function(userID) {
        var $rows = $('#' + tableId + ' tbody').find('tr');

        //  Delete all the app-passwords for this user first...
        apiUtils.deleteAllAppPasswordsTokens(userID, "app-password").done(function() {
            // Remove all app-password rows from the table
            var $appPasswordRows = $rows.find('td.authType').filter(function(){ return this.textContent === 'app-password'; }).parent();
            $appPasswordRows.remove();

            // Switch to page 1
            tableUtils.switchPage(tableId, 1);

            // Update the number selected message in the batch bar
            updateSelectedMessage();

            // Now, delete all the app-tokens for this user
            apiUtils.deleteAllAppPasswordsTokens(userID, "app-token").done(function() {
                // Remove the rest of the rows
                $rows = $('#' + tableId + ' tbody').find('tr');
                $rows.remove();

                // Switch to page 1
                tableUtils.switchPage(tableId, 1);

                // Update the number selected message in the batch bar
                updateSelectedMessage();
                            
                // Stop the processing spinner
                utils.stopProcessingSpinner();
                // Dismiss the delete dialog
                $(".tool_modal_container.token_manager_delete").addClass('hidden');
                utils.returnFocus();
            }). fail(function(errResponse) {
                console.log("delete all app-tokens failed for " + userID);
                console.log("errResponse.status: " + errResponse.status + ":   errResponse.statusText: " + errResponse.statusText);
                // The API returns success (200) if an invalid user ID was submitted
                // So, if something else happended with the request, put up the generic error message.
                var errTitle = utils.formatString(messages.GENERIC_DELETE_FAIL, ["App-Tokens"]);
                var errDescription = "";
                if (errResponse.responseJSON && errResponse.responseJSON.error_description) {
                    errDescription = errResponse.responseJSON.error_description;
                } else {
                    errDescription = utils.formatString(messages.GENERIC_DELETE_ALL_FAIL_MSG, ["App-Tokens", userID]);
                }
                utils.showResultsDialog(true, errTitle, errDescription, true, true, false, __reshowDeleteDialog);
                });
        }).fail(function(errResponse) {
            console.log("delete all app-passwords failed for " + userID);
            console.log("errResponse.status: " + errResponse.status + ":   errResponse.statusText: " + errResponse.statusText);
            // The API returns success (200) if an invalid user ID was submitted
            // So, if something else happended with the request, put up the generic error message.
            var errTitle = utils.formatString(messages.GENERIC_DELETE_FAIL, ["App-Passwords"]);
            var errDescription = "";
            if (errResponse.responseJSON && errResponse.responseJSON.error_description) {
                errDescription = errResponse.responseJSON.error_description;
            } else {
                errDescription = utils.formatString(messages.GENERIC_DELETE_ALL_FAIL_MSG, ["App-Passwords", userID]);
            }
            utils.showResultsDialog(true, errTitle, errDescription, true, true, false, __reshowDeleteDialog);
        });
    };

    /**
     * Delete selected app-passwords and app-tokens for this user and remove all
     * associated rows from the table.
     * 
     * @param String userID - owner of the authentications shown in the table.
     */
    var __deleteSelectedAuthentications = function(userID) {
        // Get the selected rows
        var $table = $('#' + tableId + ' tbody');
        var $rows = $table.find('tr');
        var $rowsChecked = $rows.filter(':has(:checkbox:checked)');

        // Create a list of deferreds for requesting deletion of the selected authentications.
        var delDeferreds = [];
        $rowsChecked.each(function(row) {
            var authID = $(this).data('authid');
            var name = $(this).find('td.authName').text();
            var authType = $(this).find('td.authType').text();
            
            delDeferreds.push(apiUtils.deleteSelectedAppPasswordsTokens(authID, authType, name, userID));
        });

        // $.when executes a callback based on zero or more Thenable objects.  Pass all the deferreds
        // assembled above for each authentication deletion requested as an array to .when.  A
        // "Master" deferred object will be created to track the state of all deferreds passed in the
        // array.  The "Master" deferred resolves when all the deferreds in our delDeferreds
        // resolve, or fails as soon as ONE of the delDeferreds fails.  Therefore, any failures in
        // apiUtils.deleteSelectedAppPasswordsTokens() will be RESOLVED, not REJECTED, and an object
        // tracking which request failed will be returned with the response so a proper error message
        // can be returned.
        // Create a "Master" deferred to track the the state of all the deferreds it was passed...
        $.when.apply($, delDeferreds).then(function() {
            // The args passed to the done callback provide the resolved values for each of the
            // deferreds and matches the order the deferreds were passed to .when().  A deferred
            // resolved with no value will have an arg value of undefined.  Delete requests resolve
            // with no value when successful.  
            // If the delete request fails, apiUtils.deleteSelectedAppPasswordsTokens() resolves
            // (NOT rejects) with a small object indicating the name and type of authentication that
            // could not be deleted.

            // Turn the arguments list into an array
            var returnedRequests = Array.prototype.slice.call(arguments);
            var totalRequests = returnedRequests.length;

            // Filter through the arguments returned to pull out the name and type of all failed deletions...
            var failedDeletions = [];
            for (var i=0; i < totalRequests; i++) {
                if (returnedRequests[i] !== undefined) {
                    failedDeletions.push(__identifyAuthentication(returnedRequests[i]));
                }
            }
            var numFailures = failedDeletions.length;

            // Stop the processing spinner
            utils.stopProcessingSpinner();
            // Dismiss the dialog
            $(".tool_modal_container.token_manager_delete").addClass('hidden');
            if (numFailures > 0) {
                // Show dialog listing the identities of all authentications that failed to delete
                var errDescription = utils.formatString(messages.GENERIC_DELETE_FAIL_NOTYPES_MSG, [numFailures]);
                if (numFailures === 1) {
                    errDescription = messages.GENERIC_DELETE_FAIL_NOTYPES_ONE_MSG;
                }
                errDescription = errDescription  + 
                                 "<div style='margin-top: 20px;'>" + failedDeletions.join(', ') + "</div>";
                utils.showResultsDialog(true, messages.GENERIC_DELETE_FAIL_NOTYPES, errDescription, true, true, false, __deleteManyAuthentications);
            } else {
                utils.returnFocus();
            }
        }, function() {
            console.error("FAILED??");
        });
    };

    var __identifyAuthentication = function(authentication) {
        return (utils.formatString(messages.IDENTIFY_AUTH, [authentication.authType, utils.encodeData(authentication.name)]));   
    };

    var __reshowDeleteDialog = function() {
        // Close other dialogs
        $(".tool_modal_container").addClass('hidden');

        // Show the existing delete dialog
       $('#delete_authentication_modal').removeClass('hidden');
       $("#deleteAuthCancel").get(0).focus();    // Set focus to non-destructive button
    };

    /**
     * Displays the confirmation dialog for deleting more than one authentication
     */
    var __deleteManyAuthentications = function() {
        // Make sure other dialogs are hidden
        $('.tool_modal_container').addClass('hidden');

        // Determine the number of authentications selected
        var $table = $('#' + tableId + ' tbody');
        var $rows = $table.find('tr');
        var $rowsChecked = $rows.filter(':has(:checkbox:checked)');

        if ($rowsChecked.length > 0) {
            // Get the userID owning these authentications
            var userID = $rowsChecked.data('userid');

            // Find the delete dialog in the html
            var $deleteDlg = $('.tool_modal_container.token_manager_delete');

            if ($rowsChecked.length === $rows.length) {
                // Deleting ALL the users authentications
                var delAllTitle = utils.formatString(messages.DELETE_MANY_FOR, [userID]);
                $deleteDlg.find('.tool_modal_title').html(delAllTitle);
                $deleteDlg.find('.tool_modal_secondary_title').html(messages.DELETE_MANY);
                $deleteDlg.find('.tool_modal_body_description').html(utils.formatString(messages.DELETE_ALL_MESSAGE, [userID]));

                // Remove any previous onclick handler for delete button
                $deleteDlg.find('.tool_modal_delete_button').off('click');
                $deleteDlg.find('.tool_modal_delete_button').on('click', function() {
                    utils.startProcessingSpinner('delete_processing');
                    __deleteAllAuthentications(userID);
                });                    
            } else {
                // Delete selected from batch bar.  Delete the selected authentications.
                var delSomeTitle = utils.formatString(messages.DELETE_MANY_FOR, [userID]);
                $deleteDlg.find('.tool_modal_title').html(delSomeTitle);
                $deleteDlg.find('.tool_modal_secondary_title').html(messages.DELETE_MANY);
                if ($rowsChecked.length === 1) {
                    $deleteDlg.find('.tool_modal_body_description').html(messages.DELETE_ONE_MESSAGE);
                } else {

                    $deleteDlg.find('.tool_modal_body_description').html(utils.formatString(messages.DELETE_MANY_MESSAGE, [$rowsChecked.length]));
                }
    
                // Remove any previous onclick handler for delete button
                $deleteDlg.find('.tool_modal_delete_button').off('click');
                $deleteDlg.find('.tool_modal_delete_button').on('click', function() {
                    utils.startProcessingSpinner('delete_processing');
                    __deleteSelectedAuthentications(userID);
                });
            }

            $deleteDlg.removeClass('hidden');
            $deleteDlg.find('.tool_modal_cancel_button').get(0).focus(); // Set focus to non-destructive button
        } else {
            // No rows were selected for deletion ... show directional dialog
            utils.showResultsDialog(false, messages.DELETE_NONE, messages.DELETE_NONE_MESSAGE, false, false, true);           
        }

    };

    /**
      * Add the "No query" row to the table
      */
    var addNoQueryMessage = function() {
        if ($('#' + tableId + ' tbody tr.' + __noQuery).length === 0) {
            // Remove existing rows from the table
            var $table = $('#' + tableId + ' tbody');
            var $rows = $table.find('tr').remove();
 
            // Message isn't already showing.....so add one.
            var numTableCols = $('#' + tableId + ' th').length;   // No query message spans all columns in table
            var noQueryMessage = "<td colspan='" + numTableCols + "' tabindex='-1'>" + messages.NO_QUERY + "</td>";
            var tableRow = "<tr class='" + __noQuery + "'>" + noQueryMessage + "</tr>";
            var $addRow = $(tableRow);
            $table.prepend($addRow);
            tableUtils.initTableKeyTraversing(tableId, $addRow);
        }
        $('#tm_select_all').prop('checked', false).attr('aria-checked', false);
        $('#' + tableId + ' tbody tr.' + __noQuery).removeClass('rowHidden');
    };

    var removeNoQueryMessage = function() {
        // If the 'no query' message is displaying, remove the row
        $('#' + tableId + ' tbody tr.' + __noQuery).remove();
    };

    var getNoQueryRow = function() {
        return $('#' + tableId + ' tbody tr.' + __noQuery);
    };

    return {
        tableId: tableId,
        initBatchBar: initBatchBar,
        selectAllAction: selectAllAction,
        updateSelectedMessage: updateSelectedMessage,
        convertResponseForTable: convertResponseForTable,
        addTableRow: addTableRow,
        deleteTableRow: deleteTableRow,
        addNoQueryMessage: addNoQueryMessage,
        removeNoQueryMessage: removeNoQueryMessage,
        getNoQueryRow: getNoQueryRow
    };

})();    
