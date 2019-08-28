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

var tokenMgr = (function() {
    "use strict";

    var __initTableSort = false;

    var __initTableSearch = function(searchInputField, searchClearButton) {
        // Search field - when data entered, show the 'x' to clear the search input field
        $('#' + searchInputField).keyup(function() {       // Search field
            var searchValue = $(this).val().trim();
            if (searchValue === "") {
                // Hide the clear search button
                $('.tool_filter_clear').css({"visibility": "hidden"});
            } else {
                // Display the clear search button
                $('.tool_filter_clear').css({"visibility": "visible"});
            }            
        })
        // Search field - when enter pressed, contact the OP to get the authentications for the user
        .keydown(function(event) {
            var keycode = (event.keyCode ? event.keyCode : event.which);
            if(keycode == '13'){
                var searchValue = $(this).val().trim();
                if (searchValue !== "") {
                    getAuthentications(searchValue);
                } else {
                    table.addNoQueryMessage();
                }
            }
        });

        // Clear search input when requested
        $('#' + searchClearButton).click(function() {         // 'x' in search field
            var $searchInputField = $(this).siblings(".tool_filter_input");
            $searchInputField.val("").focus().trigger({ type : 'keyup', which : 13 });
        });
    };

    var getAuthentications = function(userID) {
        // Add the loading spinner and disable further functions on the table
        utils.startProcessingSpinner('tm_loader');

        // Remove all rows from the table to prepare for the new results and reset the 'select all' checkbox
        $('#' + table.tableId + ' tbody').find('tr').remove();
        if (!__initTableSort) {
            // Don't initialize table sorting until after the __noQuery message
            // is removed (after the first query) or we may end up with both
            // __noQuery and __noResults at the same time!
            __initTableSort = true;
            tableUtils.initTableSorting('table_name_column');
        }
        // Unselect 'select all' checkbox and all the rows
        $('#tm_select_all').prop('checked', false).attr('aria-checked', false);

        var getAuthRequests = [];
        getAuthRequests.push(apiUtils.getAccountAppPasswords(userID));
        getAuthRequests.push(apiUtils.getAccountAppTokens(userID));

        var app_passwords = [], app_tokens = [];

        $.when.apply($, getAuthRequests).then(function() {
            // The args passed to the done callback provide the resolved
            // values for each of the deferreds passed to $.when() and they 
            // match the order the deferreds were passed to .when(). A deferred
            // resolved with no value will have an arg value of undefined.                
                
            // Turn the arguments list into an array
            var returnedRequests = Array.prototype.slice.call(arguments);
            var firstResponse = returnedRequests[0];
            if (firstResponse) {
                app_passwords = firstResponse["app-passwords"];
            }

            var secondResponse = returnedRequests[1];
            if (secondResponse) {
                app_tokens = secondResponse["app-tokens"];
            }

            for (var i = 0; i < app_tokens.length; i++) {
                var tokenauthData = app_tokens[i];
                table.addTableRow(table.convertResponseForTable(tokenauthData, "app-token"));
            }
            for (var j = 0; j < app_passwords.length; j++) {
                var pwauthData = app_passwords[j];
                table.addTableRow(table.convertResponseForTable(pwauthData, "app-password"));
            }

            var numAuthentications = app_tokens.length + app_passwords.length;
            if (numAuthentications === 0) {
                // Post a no data message in the table
                tableUtils.addNoDataMessage(table.tableId);
            }

            var screenReaderMsg = utils.formatString(messages.TABLE_FILLED_WITH, [numAuthentications, userID]);
            $('#accessibleLiveRegion').text(screenReaderMsg);
            setTimeout(function() {
                $('#accessibleLiveRegion').html('');
            }, 2000);

            // Fix up the paging results based on this new query and put user on page 1
            tableUtils.switchPage(table.tableId, 1);
            // Remove the loader
            utils.stopProcessingSpinner();
        }, function() {
            console.log("GET account passwords/token failed to retrieve all app-passwords or app-tokens");
            var errMsg = utils.formatString(messages.GENERIC_FETCH_ALL_FAIL_MSG, [utils.encodeData(userID)]);
            utils.showResultsDialog(true, messages.GENERIC_FETCH_ALL_FAIL, errMsg, false, false, true);

            // Reset the table contents to 'no results' message
            tableUtils.addNoDataMessage(table.tableId);
        });
        
    };

    var __initTokenManager = function() {
        globalization.retrieveExternalizedStrings('tokenManager').done(function () {

            tableUtils.updateTablePagingInfo(0);
            tableUtils.initTablePaging();
            __initTableSearch('search_userid', 'clear_userid_search');
            var $headerRow = $('#' + table.tableId).find('tr').first();
            tableUtils.initTableKeyTraversing(table.tableId, $headerRow);
            tableUtils.initTableKeyTraversing(table.tableId, table.getNoQueryRow());
            table.initBatchBar();
            // FF was not re-initializing the following fields even after refresh,
            // so making sure they initialized here.
            $('#search_userid').val("");
            $('#tm_select_all').prop('checked', false).attr('aria-checked', false);

            // 'Select all' checkbox in table header
            $('#tm_select_all').parent().on('click keydown', function(event) {
                var toggleCheckbox = true;
    
                if (event.type === "keydown") {
                    var key = event.which || event.keyCode;
                    if (key !== 0 && key !== 32) {      // Space key toggles a checkbox
                        toggleCheckbox = false;
                    }
                }
    
                if (toggleCheckbox) {
                    var $checkbox = $(this).find('input');

                    if ($checkbox.prop("checked")) {
                        $checkbox.prop('checked', false).attr('aria-checked', false);
                    } else {
                        $checkbox.prop('checked', true).attr('aria-checked', true);
                    }
                    // Similarly, check/uncheck all the checkboxes in the table
                    table.selectAllAction($checkbox.prop("checked"));
                }
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
                __initTokenManager();
            });
        } else {
            utils.initLogout();
            $('.tool_container').removeClass('hide');
            __initTokenManager();
        }
    });
})();
