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

    var tableId = "ca_table";
    var textDir = "";

    var __getTableRow = function(clientId) {
        var $tableRow = $("td").filter(function() {
            return $(this).text() == clientId;
        }).closest("tr");
        return $tableRow;
    };

    var __getTableRowClientNameCell = function(clientId) {
        var $tableRow = __getTableRow(clientId);
        return $tableRow.find("td").first();
    };

    var __getTableRowDeleteButton = function(clientId) {
        var $tableRow = __getTableRow(clientId);
        return $tableRow.find("td").find(".delete_client_button");
    };

    var __getTableRowEditButton = function(clientId) {
        var $tableRow = __getTableRow(clientId);
        return $tableRow.find("td").find(".edit_client_button");
    };

    /**
     * Populates the table with the initial data 
     * 
     * @param [*] array of clientData objects returned from the server 
     */
    var populateTable = function(data) {
        for (var i = 0; i < data.length; i++) {
            var dataList = data[i];
            if (dataList.hasOwnProperty('client_name')) {
                createTableRow(dataList);
            } else {
                console.error("CA table data " + i + " has no client_name");
            }
        }
    };

    /**
     * Creates the row of data and adds it to the table.
     * 
     * @param {*} clientData
     * @param String filterValue(optional) - Current filter value to match against row being 
     *                                       created. If not specified, assume row is a match
     *                                       against the current filter.
     */
    var createTableRow = function(clientData, filterValue) {
        var clientName = "<td tabindex='-1'>" + clientData.client_name + "</td>";
        var clientId = "<td tabindex='-1'>" + clientData.client_id + "</td>";
        var clientId_ID = clientData.client_id.replace(/\s/g, '__');
        var editAriaLabel = utils.formatString(messages.EDIT_ARIA, [utils.encodeData(clientData.client_name)]);
        var editButton = "<td><button id='edit_" + clientId_ID + "' class='tool_table_button edit_client_button' type='button' aria-label='" + editAriaLabel + "'>" + messages.EDIT + "</button></td>";
        var deleteAriaLabel = utils.formatString(messages.DELETE_ARIA, [utils.encodeData(clientData.client_name)]);
        var deleteButton = "<td><button id='delete_" + clientId_ID + "' class='tool_table_button delete_client_button' type='button' aria-label='" + deleteAriaLabel + "'>" + messages.DELETE + "</button></td>";

        // Create a table row and add the filter data attribute as the client name lowercased.
        // This will be used in filtering and sorting

        // insert bidi text direction to the table row
        var textDir = bidiUtils.getDOMBidiTextDirection(); 
        var tableRow = "<tr data-filter='" + utils.encodeData(clientData.client_name.toLowerCase()) + "' " + textDir + ">" + clientName + clientId + editButton  + deleteButton + "</tr>";

        var rowMatched = true;
        if (filterValue && filterValue !== "") {        // If no filter specified, row is a match.
            // Determine if this new row matches the filter Value
            if (clientData.client_name.toLowerCase().indexOf(filterValue.trim().toLowerCase()) === -1) {
                // Row is not a match for the current filter value
                rowMatched = false;
            }
        }

        tableUtils.addToTable(tableId, tableRow, rowMatched);

        __getTableRowEditButton(clientData.client_id).click(function(event) {
            utils.startProcessingSpinner('ca_loader');
            var $this = $(this);
            utils.saveFocus($this);     // Save which edit button was clicked
                                        // to return focus to it when edit completes
            event.preventDefault();
            apiUtils.getClient(clientData.client_id).done(function(response) {
                clientInputDialog.setupEditClientDialog(response);
            })
            .fail(function(errResponse) {
                var client_name = clientData.client_name;
                var clientId = clientData.client_id;
                // insert bidi text direction to the client name
                var errClientName = bidiUtils.getDOMSpanWithBidiTextDirection(client_name);
                console.log("update client failed for " + client_name);
                console.log("errResponse.status: " + errResponse.status + ":   errResponse.statusText: " + errResponse.statusText);
                if (errResponse.status === 404) {
                    if (errResponse.responseJSON && errResponse.responseJSON.error && errResponse.responseJSON.error === "invalid_client") {
                        // The OAuth client no longer exists....message the user and delete
                        // the row from the table.
                        console.log("client no longer exists, remove row");                        
                        table.deleteTableRow(clientData.client_id);

                        var missingTitle = messages.GENERIC_MISSING_CLIENT;
                        var missingDescription = utils.formatString(messages.GENERIC_MISSING_CLIENT_MSG, [errClientName, clientId]);
                        utils.showResultsDialog(true, missingTitle, missingDescription, false, false, true);
                        return;
                    }
                } 
                // Something else happended with the request.  Put up an error message...
                var errTitle = messages.GENERIC_MISSING_CLIENT;
                var errDescription = "";
                if (errResponse.responseJSON && errResponse.responseJSON.error_description) {
                    errDescription = errResponse.responseJSON.error_description;
                } else {
                    // Display a generic error message...
                    errDescription = utils.formatString(messages.GENERIC_RETRIEVAL_FAIL_MSG, [errClientName]);
                }
                utils.showResultsDialog(true, errTitle, errDescription, false, false, true); 
            });
        });

        __getTableRowDeleteButton(clientData.client_id).click(function(event) {
            utils.saveFocus($(this));   // Save which delete button was clicked
                                        // to return focus to it when delete completes
            event.preventDefault();
            $("#delete_client_modal_client_name").text(clientData.client_name);
            $(".tool_modal_delete_button").prop("client_id", clientData.client_id);
            $("#delete_client_modal").removeClass("hidden");
            $("#deleteClientCancel").get(0).focus(); // Set focus to non-destructive button
        });
    };

    /**
     * Updates the clientName field on the table following an OAuth client update action.
     * @param String clientId - unique identifier for this client.  Can NOT be changed.
     * @param String clientName - OAuth client name in update action
     */
    var updateTableRow = function(clientId, clientName) {
        var $clientNameTableRowCell = __getTableRowClientNameCell(clientId);
        if ($clientNameTableRowCell.length > 0 && $clientNameTableRowCell.text() !== clientName) {
            // Name was updated
            $clientNameTableRowCell.text(clientName);
            var editAriaLabel = utils.formatString(messages.EDIT_ARIA, [clientName]);
            var $editButton = __getTableRowEditButton(clientId);
            $editButton.attr('aria-label', editAriaLabel);
            var deleteAriaLabel = utils.formatString(messages.DELETE_ARIA, [clientName]);
            var $deleteButton = __getTableRowDeleteButton(clientId);
            $deleteButton.attr('aria-label', deleteAriaLabel);
            $clientNameTableRowCell.closest('tr').data('filter', clientName.toLowerCase());

            var direction = tableUtils.currentSortDirection('table_client_name_column');

            if (direction !== "none") {
                tableUtils.sortTableWithFilterName(tableId, direction === "ascending");
                tableUtils.switchPage(tableId, tableUtils.currentPage());
            }
        }
    };

    var deleteTableRow = function(clientId) {
        var $tableRowToBeDeleted = __getTableRow(clientId);

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
            if (id && id.indexOf(clientId) > 0) {
                utils.updateFocusAfterDelete(table.tableId, coords.row, coords.col);
            }
        }
    };

    return {
        tableId: tableId,
        createTableRow: createTableRow,
        deleteTableRow: deleteTableRow,
        updateTableRow: updateTableRow,
        populateTable: populateTable,
        getTableRowClientNameCell: __getTableRowClientNameCell
    };

})();