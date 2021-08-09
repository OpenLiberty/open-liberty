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

var clientAdmin = (function() {
    "use strict";

    var __reshowDeleteDialog = function() {
         // Close other dialogs
         $(".tool_modal_container").addClass('hidden');
 
         // Show the existing delete Dialog
        $('#delete_client_modal').removeClass('hidden');
        $("#deleteClientCancel").get(0).focus();    // Set focus to non-destructive button
    };

    var __initClientAdmin = function () {
        globalization.retrieveExternalizedStrings('clientAdmin').done(function () {
            // Disable the Add New client button until all the fields are pulled in.
            $('#add_new_client').prop('disabled', true);

            apiUtils.getAllClients().done(function (response) {
               clientInputDialog.init(); 
               
               var data = response.data;

                if (data.length === 0) {
                    // Post a no data message in the table
                    tableUtils.addNoDataMessage(table.tableId);
                } else {
                    table.populateTable(data);
                }

                tableUtils.updateTablePagingInfo(data.length);

                utils.stopProcessingSpinner();
            }).fail(function (errResponse) {
                console.log("Failed to retrieve OAuth client list");
                console.log("errResponse.status: " + errResponse.status + ":   errResponse.statusText: " + errResponse.statusText);
                utils.showResultsDialog(true, messages.GENERIC_GET_CLIENTS_FAIL, messages.GENERIC_GET_CLIENTS_FAIL_MSG, false, false, true);
                tableUtils.switchPage(table.tableId, 1);
            });

            // Add registration client onclick event
            $("#add_new_client").click(function() {
                utils.saveFocus($(this));   // Save off button to return focus
                                            // to it when dialog is dismissed.
                clientInputDialog.setupNewClientDialog();
            });

            var $headerRow = $('#' + table.tableId).find('tr').first();
            tableUtils.initTableKeyTraversing(table.tableId, $headerRow);
            tableUtils.initTableSorting('table_client_name_column');
            tableUtils.initTableFilter('filter_client_name', 'clear_client_name_filter');
            tableUtils.initTablePaging();

            $(".tool_modal_delete_button").click(function() {
                utils.startProcessingSpinner('delete_processing');
                var clientId = $(this).prop("client_id");
                var client_name = table.getTableRowClientNameCell(clientId).text();
                var $requestingButton = utils.getTopFocusElement();  // Delete button selected from table

                apiUtils.deleteClient(clientId).done(function (response) {
                    // delete row from the table
                    table.deleteTableRow(clientId);
                    utils.stopProcessingSpinner();
                    $("#delete_client_modal").addClass('hidden');
                    utils.returnFocus();
                })
                .fail(function (errResponse) {
                    console.log("delete client failed for " + client_name);
                    console.log("errResponse.status: " + errResponse.status + ":   errResponse.statusText: " + errResponse.statusText);
                    if (errResponse.status === 404) {
                        if (errResponse.responseJSON && errResponse.responseJSON.error && errResponse.responseJSON.error === "invalid_client") {
                            // Deleting an OAuth client that no longer exists....just delete
                            // the row from the table.
                            console.log("client no longer exists, remove row");
                            table.deleteTableRow(clientId);
                            utils.stopProcessingSpinner();
                            $("#delete_client_modal").addClass('hidden');
                            utils.returnFocus();
                            return;
                        }
                    }
                    // Something else happended with the request.  Put up the generic error message.
                    var errTitle = messages.GENERIC_DELETE_FAIL;
                    var errClientName = bidiUtils.getDOMSpanWithBidiTextDirection(client_name);
                    var errDescription = "";
                    if (errResponse.responseJSON && errResponse.responseJSON.error_description) {
                        errDescription = errResponse.responseJSON.error_description;
                    } else {
                        errDescription = utils.formatString(messages.GENERIC_DELETE_FAIL_MSG, [errClientName]);
                    }
                    utils.showResultsDialog(true, errTitle, errDescription, true, true, false, __reshowDeleteDialog);
                });
            });

            utils.initModalDialogKeystrokes();

            if (globalization.isBidiEnabled()) {
                bidiUtils.setupToolBidi();
            }
        });
    };

    $(document).ready(function() {
        if (self !== top) {
            // Since the content is displayed in an iframe, assume it's thru admin center. Look for bidi preference.
            globalization.retrieveBidiPreference().always(function() {
                $('.tool_container').removeClass('hide');                
                __initClientAdmin();
            });
        } else {
            utils.initLogout();
            $('.tool_container').removeClass('hide');
            __initClientAdmin();
        }
    });
    
})();
