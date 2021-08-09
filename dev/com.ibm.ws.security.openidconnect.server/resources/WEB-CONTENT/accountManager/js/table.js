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

    var tableId = "am_table";

    var __getTableRowById = function(authID) {
        var $table = $('#' + tableId + ' tbody');
        var $row = $table.find('input[authid="' + authID + '"]').closest('tr');
        return $row;
    };

    var __getTableRowByName = function(name, authType) {
        var $table = $('#' + tableId + ' tbody');
        var $row = $table.find('tr[data-filter="' + utils.escapeString(name.toLowerCase())+ '"]');
        if ($row.length > 1) {
            // Two rows with the same name.   Find requested one based on authType.
            var $correctAuthType = $row.find('td.authType').filter(function(){ return this.textContent === authType; });
            $row = $correctAuthType.parent();
        }
        return $row;
    };

    /**
     * Converts the response object for an authentication to the data needed to display
     * in the table row.
     * This is needed because the field names returned are different between app-password
     * and app-token.
     * 
     * @param {*} responseObj - object returned from Rest API representing an authentication
     * @param String authType - 'app-password' or 'app-token'
     * @param String createdName - name given to authentication (for auth creation scenario)
     * 
     * @return {*} authData  of the form: 
     *              {
     *                  "authType": "app-password" OR "app-token",
     *                  "name": "yyyyyyy",
     *                  "authID": "0cc7389f863a47b69c004dd7d21e8a5b",
     *                  "created_at": "1553204416750"
     *                  "expires_at": "1552390482158"
     *              }
     */
    var convertResponseForTable = function(responseObj, authType, createdName) {
        var authData = {  authType: authType,
                            authID: responseObj.app_id,  // ID given by OP to track this authentication
                        created_at: responseObj.created_at,
                        expires_at: responseObj.expires_at
        };

        // User friendly name given by user to identify a client application that uses this authentication
        if (responseObj.name) {
            authData.name = responseObj.name;
        } else {   // Just being created....assign name provided by user
            authData.name = createdName.trim();
        }

        if (typeof(responseObj.created_at) === "string") {
            // convert to integer
            responseObj.created_at = parseInt(responseObj.created_at);
        }
        authData.created_at = responseObj.created_at;

        if (typeof(responseObj.expires_at) === "string") {
            // convert to integer
            responseObj.expires_at = parseInt(responseObj.expires_at);
        }
        authData.expires_at = responseObj.expires_at;

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
     *                  "created_at": 1553204416750,
     *                  "expires_at": 1552390482158
     *              }
     * 
     * @returns String - HTML representing a table row
     */
    var __createTableRow = function(authData) {
        var name = "<td class='authName' tabindex='-1'>" + authData.name + "</td>";
        var type = "<td class='authType' tabindex='-1'>" + authData.authType + "</td>";

        // Date data
        var lang = globalization.getLanguageCode(); // Use English format for unsupported language as not all browsers are able to 
                                                    // render the Arabic date correctly
        var options = { month: 'long', day: 'numeric', year: 'numeric' };
        var issueDate = new Date(authData.created_at);
        var issuedOn = "<td class='authIssued' tabindex='-1'>" + issueDate.toLocaleDateString(lang, options) + "</td>";
        var expiresDate = new Date(authData.expires_at);
        var expiresOn = "<td class='authExpires' tabindex='-1'>" + expiresDate.toLocaleDateString(lang, options) + "</td>";

        // Action buttons
        var regenerateAriaLabel = utils.formatString(messages.REGENERATE_ARIA, [authData.authType, authData.name]);
        var regenerateButton = "<td><input id='regenerate_" + authData.authID + "' type='button' authID = " + authData.authID;
        if ((authData.authType === 'app-password' && window.globalAppPasswordsAllowed) ||
            (authData.authType === 'app-token' && window.globalAppTokensAllowed)) {
                regenerateButton += " class='tool_table_button regenerate_auth_button' value='" + messages.REGENERATE + "' aria-label='" + regenerateAriaLabel + "'></td>";
        } else {
            // Disable the 'Regenerate' button if the OP was not defined with the appPasswordAllowed 
            // or appTokenAllowed flags.
                regenerateButton += " class='tool_table_button regenerate_auth_button' disabled value='" + messages.REGENERATE + "' aria-label='" + regenerateAriaLabel + "'></td>";
        }
        var deleteAriaLabel = utils.formatString(messages.DELETE_ARIA, [authData.authType, authData.name]);
        var deleteButton = "<td><input id='delete_" + authData.authID + "' type='button' authID = " + authData.authID + " class='tool_table_button delete_auth_button' value='" + messages.DELETE + "' aria-label='" + deleteAriaLabel + "'></td>";

        // insert bidi text direction to the table row
        var textDir = bidiUtils.getDOMBidiTextDirection(); 
        // Create a table row and add the filter data attribute as the name lowecased.
        // This is used in sorting and filtering (when implemented).
        var tableRow = "<tr data-filter='" + authData.name.toLowerCase() + "' " + textDir + ">" + name + type + issuedOn + expiresOn + regenerateButton + deleteButton + "</tr>";

        return tableRow;
    };

    /**
     * Add onClick handlers for the row actions
     * 
     * @param {*} authID - unique ID for this app-password or app-token
     */
    var __enableRowActions = function(authID) {
        $(".regenerate_auth_button[authID='" + authID + "']").click(function(event) {
            event.preventDefault();
            var $this = $(this);
            var $row = $(this).closest('tr');

            utils.saveFocus($this);     // Save which regenerate button was clicked
                                        // to return focus to it when regenerate completes

            var authID = $this.attr('authID');
            var name = $row.find('td.authName').text();
            var type = $row.find('td.authType').text();
            var issueDate = $row.find('td.authIssued').text();

            __regenerateAuthMechanism(authID, name, type, issueDate);                        
        });

        $(".delete_auth_button[authID='" + authID + "']").click(function(event) {
            event.preventDefault();
            var $this = $(this);
            var $row = $(this).closest('tr');

            utils.saveFocus($this);     // Save which delete button was clicked
                                        // to return focus to it when delete completes

            var authID = $this.attr('authID');
            var name = $row.find('td.authName').text();
            var type = $row.find('td.authType').text();

            __deleteAuthentication(authID, name, type);                                        
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
     *                  "created_at": 1553204416750,
     *                  "expires_at": 1552390482158
     *              }
     */
    var addTableRow = function(authData) {
        var tableRow = __createTableRow(authData);

        tableUtils.addToTable(tableId, tableRow, true);

        __enableRowActions(authData.authID);
    };

    var deleteTableRow = function(authID) {
        var $tableRowToBeDeleted = __getTableRowById(authID);

        if ($tableRowToBeDeleted.length > 0) {
            // About to delete a row. Focus will no longer be able to return
            // to the buttons on this row, so they will need to be reset.
            // Save off the coordinates (row, col) of the focus button before
            // the row is deleted.
            var $tableFocusButton = utils.getTopFocusElement();
            var coords = utils.getTableCellCoords($tableFocusButton);

            $tableRowToBeDeleted.remove();
            // Layout the pages of the table again since one row is now missing
            tableUtils.switchPage(tableId, tableUtils.currentPage());

            // Update the focus button if it was in the row that was just
            // deleted.
            var id = $tableFocusButton.attr('id');
            if (id && id.indexOf(authID) > 0) {
                utils.updateFocusAfterDelete(table.tableId, coords.row, coords.col);
            }
        }
    };

    /**
     * Displays the correct regenerate dialog (app-password -vs- app-token) to submit
     * the request.
     * 
     * @param String authID - unique ID for this app-password or app-token
     * @param String name - given name for this authentication (app-password/app-token)
     * @param String type - 'app-password' or 'app-token'
     * @param String issueDate - date app-password/app-token was issued
     */
    var __regenerateAuthMechanism = function(authID, name, type, issueDate) {
        // Make sure other dialogs are hidden
        $('.tool_modal_container').addClass('hidden');

        // Find the regenerate dialog in the html
        var $regenerateDlg = $('.tool_modal_container.ss_authenticate');
        var $dialogInfo = $regenerateDlg.find('.tool_modal_body_info');
        // insert bidi text direction to the name
        var dirTextName = bidiUtils.getDOMBidiTextDirection(name);
        var nameLabel = utils.formatString(messages.NAME_IDENTIFIER, ["<span class='tool_modal_body_info_value' " + dirTextName + ">" + utils.encodeData(name) + "</span>"]);
        $dialogInfo.find('.tool_modal_body_info_label').html(nameLabel);
        var $authType = $regenerateDlg.find('#authType');
        var $authValue = $regenerateDlg.find('.authValueDiv');

        if (type === 'app-password') {
            $regenerateDlg.find('.tool_modal_title').html(messages.REGENERATE_APP_PASSWORD);
            $dialogInfo.find('.tool_modal_body_description').html(messages.REGENERATE_PW_WARNING);

            $authType.find("#rb_app_password").prop("disabled", false).prop("checked", true).attr('aria-checked', true);
            $authType.find("#rb_app_token").prop("disabled", true).prop("checked", false).attr('aria-checked', false);

            var passwordMsg = utils.formatString(messages.REGENERATE_PW_PLACEHOLDER, [issueDate]);
            $authValue.find("#auth_value").val("").attr({"placeholder": passwordMsg, "aria-label": messages.GENERATED_APP_PASSWORD});
            $authValue.find('.tool_modal_field_copy_button>img').prop('title', messages.COPY_APP_PASSWORD).prop('alt', messages.COPY_APP_PASSWORD);

        } else if (type === 'app-token') {
            $regenerateDlg.find('.tool_modal_title').html(messages.REGENERATE_APP_TOKEN);
            $dialogInfo.find('.tool_modal_body_description').html(messages.REGENERATE_TOKEN_WARNING);
            
            $authType.find("#rb_app_password").prop("disabled", true).prop("checked", false).attr('aria-checked', false);
            $authType.find("#rb_app_token").prop("disabled", false).prop("checked", true).attr('aria-checked', true);

            var tokenMsg = utils.formatString(messages.REGENERATE_TOKEN_PLACEHOLDER, [issueDate]);
            $authValue.find("#auth_value").val("").attr({"placeholder": tokenMsg, "aria-label": messages.GENERATED_APP_TOKEN});
            $authValue.find('.tool_modal_field_copy_button>img').prop('title', messages.COPY_APP_TOKEN).prop('alt', messages.COPY_APP_TOKEN);

        } else {
            console.log('Bad data.  Type was set to ' + type);
            return;
        }

        $authValue.find('.tool_modal_field_copy_button').prop("disabled", true);

        // Enable the correct actions
        $regenerateDlg.find('.tool_modal_cancel_button').removeClass('hidden');   
        $regenerateDlg.find('.tool_modal_generate_button').prop('disabled', false).off('click');
        $regenerateDlg.find('.tool_modal_generate_button').on('click', function() {
            utils.startProcessingSpinner('add_regen_processing');
            regeneratePWorToken(authID, name, type); 
        }).removeClass('hidden');
        $regenerateDlg.find('.tool_modal_done_button').addClass('hidden');

        $('.tool_modal_body_field').addClass('hidden');
        $dialogInfo.removeClass('hidden');

        $regenerateDlg.removeClass('hidden');
        $regenerateDlg.find('.tool_modal_cancel_button').get(0).focus();  // Set focus to non-destructive button
    };

    /**
     * Regenerate the app-password or app-token.  This requires deleting the current one
     * and recreating one with the same name and type.
     * 
     * @param String authID - unique ID for this app-password or app-token
     * @param String name  - given name for this authentication (app-password/app-token)
     * @param String authType - 'app-password' or 'app-token'
     */
    var regeneratePWorToken = function(authID, name, authType) {
        // Delete the existing authentication and create an identical one.
        // The new one will be assigned a new authID and app-pw or app-token.
        apiUtils.deleteAcctAppPasswordToken(authID, authType).done(function() {
            // Create a new app-password or token with the same name.
            apiUtils.addAcctAppPasswordToken(name, authType).done(function (response) {       
                var authenticationValue;
                if (authType === "app-password") {
                    authenticationValue = response.app_password;
                } else {
                    authenticationValue = response.app_token;
                }
               
                // Display returned app-password or app-token on dialog for user to copy
                var $addNewDlg = $('.tool_modal_container.ss_authenticate');
                var $authValue = $addNewDlg.find('.authValueDiv');
                $authValue.find("#auth_value").val(authenticationValue);
                $authValue.find(".tool_modal_field_copy_button").prop("disabled", false);

                // Switch out the buttons so only 'Done' is showing.
                $addNewDlg.find('.tool_modal_cancel_button').addClass('hidden');
                $addNewDlg.find('.tool_modal_generate_button').addClass('hidden');
                $addNewDlg.find('.tool_modal_done_button').removeClass('hidden');
                // Set focus to the copy button
                $("#auth_value_copy").get(0).focus();

                // Update the row in the table with the returned information
                var authData = convertResponseForTable(response, authType, utils.encodeData(name));
                var tableRow = __createTableRow(authData);   // Create new table row with new values
                var $currentTableRow = __getTableRowByName(name, authType);
                $currentTableRow.replaceWith(tableRow);      // In place replacement
                // Get the replaced row in the table.
                $currentTableRow = __getTableRowByName(name, authType);
                $currentTableRow.addClass("rowMatched");
                // Row elements were replaced ... so reset the key traversing event handlers.
                tableUtils.initTableKeyTraversing(tableId, $currentTableRow);
                __enableRowActions(authData.authID);

                // Row was replaced so switch out the return focus button to the
                // 'Regenerate' button on this new row.
                utils.clearFocus();
                utils.saveFocus($currentTableRow.find('.regenerate_auth_button'));

                utils.stopProcessingSpinner();
            }).fail(function(errResponse) {
                // A failure here indicates that the deletion of the currently existing
                // row was successful, so it no longer exists.  Delete it from the table
                // to minimize confusion (it won't be there any longer if they refresh the page).
                deleteTableRow(authID);

                console.log("Regenerate failed for ID" + authID);
                console.log("errResponse.status: " + errResponse.status + ":   errResponse.statusText: " + errResponse.statusText);
                // The API returns success (200) if you attempt to delete an authentication
                // that is already deleted.
                // So, if something else happended with the request, put up the generic error message.
                var regenerateTypeTitle = authType === 'app-password' ? 'App-Password' : 'App-Token';
                var errTitle = utils.formatString(messages.GENERIC_REGENERATE_FAIL, [regenerateTypeTitle]);
                var errDescription = "";
                if (errResponse.responseJSON && errResponse.responseJSON.error_description) {
                    errDelDescription = errResponse.responseJSON.error_description;
                } else {
                    errDescription = utils.formatString(messages.GENERIC_REGENERATE_FAIL_CREATE_MSG, [authType, utils.encodeData(name)]);
                }    
                utils.showResultsDialog(true, errTitle, errDescription, true, true, false, reshowAddRegenDialog);
            });            
        }).fail(function(errResponse) {
            console.log("delete failed for ID" + authID);
            console.log("errResponse.status: " + errResponse.status + ":   errResponse.statusText: " + errResponse.statusText);
            // The API returns success (200) if you attempt to delete an authentication
            // that is already deleted.
            // So, if something else happended with the request, put up the generic error message.
            var regenDelTitle = authType === 'app-password' ? 'App-Password' : 'App-Token';
            var errDelTitle = utils.formatString(messages.GENERIC_REGENERATE_FAIL, [regenDelTitle]);
            var errDelDescription = "";
            if (errResponse.responseJSON && errResponse.responseJSON.error_description) {
                errDelDescription = errResponse.responseJSON.error_description;
            } else {
                // Display a generic error message...
                errDelDescription = utils.formatString(messages.GENERIC_REGENERATE_FAIL_MSG, [authType, utils.encodeData(name)]);
            }
            utils.showResultsDialog(true, errDelTitle, errDelDescription, true, true, false, reshowAddRegenDialog);
        });
    };

    /**
     * Displays the correct delete dialog (app-password -vs- app-token) to submit
     * the request.
     * 
     * @param String authID - unique ID for this app-password or app-token
     * @param String name - given name for this authentication (app-password/app-token)
     * @param String type - 'app-password' or 'app-token'
     */
    var __deleteAuthentication = function(authID, name, type) {
        // Make sure other dialogs are hidden
        $('.tool_modal_container').addClass('hidden');

        // Find the delete dialog in the html
        var $deleteDlg = $('.tool_modal_container.ss_delete');
        $deleteDlg.find('.tool_modal_title').html(utils.encodeData(name));

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
                __deletePWorToken(authID, name, type);
        });

        $deleteDlg.removeClass('hidden');
        $deleteDlg.find('.tool_modal_cancel_button').get(0).focus(); // Set focus to non-destructive button
    };

    /**
     * Deletes the authentication and removes the row from the table
     * 
     * @param String authID - unique ID for this app-password or app-token
     * @param String name - given name for this authentication (app-password/app-token)
     * @param String authType - 'app-password' or 'app-token'
     */
    var __deletePWorToken = function(authID, name, authType) {
        apiUtils.deleteAcctAppPasswordToken(authID, authType).done(function() {
            deleteTableRow(authID);
            utils.stopProcessingSpinner();
            // Dismiss the dialog
            $(".tool_modal_container.ss_delete").addClass('hidden');
            utils.returnFocus();
        }).fail(function(errResponse) {
            console.log("delete failed for ID" + authID);
            console.log("errResponse.status: " + errResponse.status + ":   errResponse.statusText: " + errResponse.statusText);
            // The API returns success (200) if you attempt to delete an authentication 
            // that is already deleted.
            // So, if something else happended with the request, put up the generic error message.
            var deleteTypeTitle = authType === 'app-password' ? 'App-Password' : 'App-Token';
            var errDescription = "";
            var errTitle = utils.formatString(messages.GENERIC_DELETE_FAIL, [deleteTypeTitle]);
            if (errResponse.responseJSON && errResponse.responseJSON.error_description) {
                errDescription = errResponse.responseJSON.error_description;
            } else {
                errDescription = utils.formatString(messages.GENERIC_DELETE_FAIL_MSG, [authType, utils.encodeData(name)]);
            }
            utils.showResultsDialog(true, errTitle, errDescription, true, true, false, __reshowDeleteDialog);
        });
    };

    var __reshowDeleteDialog = function() {
         // Close other dialogs
         $(".tool_modal_container").addClass('hidden');
 
         // Show the existing delete dialog
        $('#delete_authentication_modal').removeClass('hidden');
        $("#deleteAuthCancel").get(0).focus();    // Set focus to non-destructive button
    };

    var reshowAddRegenDialog = function() {
        // Close other dialogs
        $(".tool_modal_container").addClass('hidden');

        // Show the existing add/regenerate dialog
       $('#add_regen_authentication_modal').removeClass('hidden');
       $("#auth_cancel").get(0).focus();
    };

    return {
        tableId: tableId,
        convertResponseForTable: convertResponseForTable,
        addTableRow: addTableRow,
        deleteTableRow: deleteTableRow,
        reshowAddRegenDialog: reshowAddRegenDialog
    };

})();