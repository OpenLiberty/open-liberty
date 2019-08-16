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

var utils = (function() {
    "use strict";

    /**
     * Message substitution
     * @param {String} value - String with value {}s to replace
     * @param {Array} args - an array containing strings to replace {i}
     */
    var formatString = function(value, args) {     
        for (var i = 0; i < args.length; i++) {
            var regexp = new RegExp('\\{'+i+'\\}', 'gi');
            value = value.replace(regexp, args[i]);
        }
        return value;
    };

    /**
     * Onclick handler for copy to clipboard button.
     * 
     * @param target - DOM button clicked
     * @param callback - function to call when copy is successful
     */
    var copyToClipboard = function(target, callback) {
        var copyValue = $(target).prev().val().trim();

        // IE
        if (window.clipboardData) {
            window.clipboardData.setData("Text", copyValue);
        } else {
            // Create a temporary element for copying the text.
            var temp = $('<textarea>');
            temp.css ({
                position: "absolute",
                left:     "-1000px",
                top:      "-1000px"
            });
            temp.text(copyValue);
            $("body").append(temp);
            temp.select();

            if (document.execCommand('copy')) {
                callback();
            } else {
                alert('Copy failed. Copy the value manually.');
            }

            // Remove temporary element
            temp.remove();
        }
    };

    /**
     * Show the generic results dialog (laid out within each index.jsp).
     * 
     * @param Boolean errDialog - true if dialog displays an error; 
     *                            false if dialog displays result message.
     * @param {*} title - dialog title
     * @param {*} description - dialog message in the body
     * @param {*} showCancel - true to show Cancel button; false otherwise
     * @param {*} showTryAgain - true to show 'Try again...' button; false otherwise
     * @param {*} showDone - true to show Done button; false otherwise
     * @param {*} actionCallback - callback for 'Try again...' button
     */
    var showResultsDialog = function(errDialog, title, description, showCancel, showTryAgain, showDone, actionCallback) {
        var $dialog = $('#results_modal');

        if (errDialog) {
            // Add red error indications to the dialog title and buttons
            $dialog.addClass('tool_modal_alert');
            $dialog.attr('role', 'alertdialog');
        } else {
            $dialog.removeClass('tool_modal_alert');
            $dialog.attr('role', 'dialog');
        }

        // Set Results dialog title
        $dialog.find('.tool_modal_title').html(title);

        // Set Results dialog description
        $dialog.find('.tool_modal_body_description').html(description);

        // Show/Hide the correct buttons       
        if (showCancel) {
            $dialog.find('.tool_modal_cancel_button').removeClass('hidden');
        } else {
            $dialog.find('.tool_modal_cancel_button').addClass('hidden');
        }
        var $tryAgainButton = $dialog.find('.tool_modal_try_again_button');
        $tryAgainButton.off('click');  // Remove any previous onClick handler for this button
        if (showTryAgain) {
            $tryAgainButton.removeClass('hidden');
            if (actionCallback) {
                $tryAgainButton.on('click', function() {
                    actionCallback();
                });
            }
        } else {
            $tryAgainButton.addClass('hidden');
        }
        if (showDone) {
            $dialog.find('.tool_modal_done_button').removeClass('hidden');
        } else {
            $dialog.find('.tool_modal_done_button').addClass('hidden');
        }

        // Stop the processing spinner
        stopProcessingSpinner();
        // Close other modals
        $(".tool_modal_container").addClass('hidden');
        // Show the Results modal dialog
        $dialog.removeClass('hidden');

        // Set focus on correct button
        if (showTryAgain) {
            // Focus 'Try again...' button
            $dialog.find('.tool_modal_try_again_button').get(0).focus();
        } else {
            // Focus on the 'Done' button
            $dialog.find('.tool_modal_done_button').get(0).focus();
        }        
    };

    /**
     * Cleans up the error indicators on the fields within a dialog.
     */
    var cleanUpErrorFields = function() {
        $('.tool_field_error').removeClass('tool_field_error');
        $('.tool_err_img').remove();
        $('.tool_err_msg').remove();
    };

    /**
     * Show the processing spinner until the task completes
     */
    var startProcessingSpinner = function(spinnerName) {
        $('#' + spinnerName).find('.tool_processing').removeClass('tool_processing_stop');
        $('#' + spinnerName).removeClass('hidden');
    };

    /** 
     * Hide the processing spinner on completion of a task
     */
    var stopProcessingSpinner = function() {
        $('.tool_processing_overlay').find('.tool_processing').addClass('tool_processing_stop');
        $('.tool_processing_overlay').addClass('hidden');
    };

    /**
     * When accessing the application through the Admin Center, hide the Logout
     * button.
     */
    var hideLogout = function() {
        $('.tool_logout_div').addClass('hidden');
    };

    /** 
     * OnClick handler for the 'Logout' button that appears when the application is 
     * NOT part of the AdminCenter.
     */
    var initLogout = function() {
        $('.tool_logout_div').removeClass('hidden');
        $('.tool_logout_button').on('click', function(){
            startProcessingSpinner('ca_loader');

            logoutUser().done(function (response) {
                $('.tool_container').html(response);
                stopProcessingSpinner();
            }).fail(function(errResponse) {
                $('.tool_container').html(errResponse);
                stopProcessingSpinner();
            });
        });
    };

    var logoutUser = function() {
        var toolLocation = window.location.href;
        var urlLogout = toolLocation.substring(0, toolLocation.lastIndexOf('/')) + "/logout";

        var deferred = new $.Deferred();
    
        $.ajax({
            url: urlLogout,
            success: function(response) {
                deferred.resolve(response);
            },
            error: function(jqXHR) {
                // Ajax request failed.      
                deferred.reject(jqXHR);
            }
        });
        
        return deferred;
    };

    /**
     * 1) Set up closing the dialog with the buttons
     * 2) Set up ESC in the dialog to close the dialog
     * 3) Set up tabbing within a modal dialog.  
     * 
     * Tabbing: 
     * From the X-close button at the upper right of a modal dialog, forward tabbing
     * should cycle down through the input fields to the button container at the bottom
     * of all modals. At the last enabled non-hidden button, we should loop back to the
     * X-close button.
     * Backward tabbing should do the cycle in reverse.
     */
    var initModalDialogKeystrokes = function() {
        // Close the dialog with the buttons
        $(".tool_modal_close").click(function() {
            $(".tool_modal_container").addClass('hidden');
            returnFocus();
        });

        // ESC in a dialog will close the dialog
        $(document).keydown(function(event) {
            var key = event.which || event.keyCode;
            if (key === 27) {   // ESC key
                $(".tool_modal_container").addClass('hidden');
                returnFocus();
            }
        });

        // Tabbing backwards cycle
        $('.tool_modal_x_close').on('keydown', function(event) {
            var key = event.which || event.keyCode;

            if (event.shiftKey && key === 9) {  // Shift + tab key ... reverse tab
                event.preventDefault();
                // Place the focus to the last enabled non-hidden button in the dialog
                var $dlg = $(this).closest('.tool_modal_container');
                $dlg.find('.tool_modal_button_container').find('.tool_modal_action_button:enabled:not(".hidden")').last().focus();
            }
        });

        // Tabbing forward cycle
        $('.tool_modal_action_button').on('keydown', function(event) {
            var key = event.which || event.keyCode;

            if (!event.shiftKey && key === 9) {                    // Tab key
                // If this is the last button then Tab should cycle user back to 
                // the X-close button at the top right of the dialog.
                // Get the last enabled non-hidden button on this dialog.
                var $buttonContainer = $(this).closest('.tool_modal_button_container');
                var $lastButton = $buttonContainer.find('.tool_modal_action_button:enabled:not(".hidden")').last();
                var $thisButton = $(this);
                if ($thisButton.is($lastButton)) {
                    // Tab pressed while focus was on the last button in button container.
                    // Cycle back to the X-close button.
                    event.preventDefault();
                    var $dlg = $buttonContainer.closest('.tool_modal_container');
                    $dlg.find('.tool_modal_x_close').focus();
                }
            }
        });
    };

    /**
     * The following will be used to set and restore the focus to the main application
     * panel after all modal dialogs have been dismissed.  Focus should be returned to
     * the button that requested the action of the dialog ('Add new', 'Delete', 'Edit',
     * 'Regenerate').
     * 
     * When a deletion occurs, the row of the table is no longer present to return 
     * focus to the 'Delete' button for that row.  In those cases, focus will be 
     * returned to the 'Delete' button of the next row, or the previous row if there
     * are no next rows.
     */
    var spotStack = [];    // Saves the focus element on the main app page that focus
                           // should be returned to after all dialogs are dismissed.
    /**
     * @param {*} $element - JQuery element of the button or cell to return focus to
     *                       when an action completes.
     */                           
    var saveFocus = function($element) {
        spotStack.push($element);
    };
    var returnFocus = function() {
        var $element = spotStack.pop();
        if ($element !== undefined) {
            $element.focus();   // Return focus to element on top of stack
        }
    };
    var clearFocus = function() {
        spotStack = [];
    };
    var getTopFocusElement = function() {
        if (spotStack.length) {
            return spotStack[spotStack.length-1];
        } else {
            return null;
        }
    };
    /**
     * Update the focus element where focus should be returned to
     * after a row has been deleted since the 'Delete' button on
     * the row that was deleted no longer exists.
     * 
     * @param String tableId - ID of the table
     * @param int row - row index of the visible rows of the row that was deleted
     * @param int col - column index of the button seleted in the row that caused
     *                  the deletion
     */
    var updateFocusAfterDelete = function(tableId, row, col) {
        var $table = $('#' + tableId + ' tbody');

        var $visibleRows = $table.find("tr:not('.rowHidden')");
        var $tr = $visibleRows.eq(row);
        if ($tr.length === 0) {
            $tr = $visibleRows.eq(row-1);
        }

        var $cell = $tr.find('td').eq(col);
        if ($cell.length === 0) {
            // This occurs when the 'No results found' row is the
            // only row in the table. 'No results found' row has
            // only 1 cell that spans all columns.
            $cell = $tr.find('td').eq(0);
        }

        // Clear current focus spot stack since we are replacing 
        // the return focus element.
        clearFocus();

        // Set stack to new focus element
        if ($cell.has('button, input').length) {
            saveFocus($cell.children().last());
        } else {
            saveFocus($cell);
        }
    };
    var getTableCellCoords = function($tableButton) {
        var $colCell = $tableButton.closest('td');
        var col = $colCell.index();

        // With paging and filtering, we cannot rely on the index
        // of the event.target's closest 'tr's index value.
        // Look at the rows that are visible to match the row
        // containing the cell and save that row index value
        // as the row coordinate.
        var $eventRow = $colCell.closest('tr');
        var $visibleRows = $colCell.closest('tbody').find('tr:not(".rowHidden")');
        for (var row = 0; row < $visibleRows.length; row++) {
            if ($($visibleRows[row]).is($eventRow)) {
                break;
            }
        }
        var coords = {row: row,
                      col: col};
        return coords;
    };

    /**
     * Encode untrusted data by replacing the following characters with HTML entity
     * encoding values before inserting into the DOM.
     * 
     *      Characters replaced: &, <, >, ", ', #, and /
     * 
     * @param String dataString 
     */
    var encodeData = function(dataString) {
        var chars = {'&': '&amp;',
                     '<': '&lt;',
                     '>': '&gt;',
                     '"': '&quot;',
                     "'": '&#039;',
                     '#': '&#035;',
                     '/': '&#x2F;'};
        return dataString.replace( /[&<>'"#/]/g, function(c) { return chars[c]; } );
    };

    /**
     * Escape the dataString so it is as it appears in the HTML.
     * 
     * @param String dataString
     */
    var escapeString = function(dataString) {
        return dataString.replace(/([ #;&,.+*~\':"!^$[\]()=>|\/@])/g,'\\$1');
    };

    return {
        formatString: formatString,
        copyToClipboard: copyToClipboard,
        showResultsDialog: showResultsDialog,
        cleanUpErrorFields: cleanUpErrorFields,
        startProcessingSpinner: startProcessingSpinner,
        stopProcessingSpinner: stopProcessingSpinner,
        hideLogout: hideLogout,
        initLogout: initLogout,
        logoutUser: logoutUser,
        initModalDialogKeystrokes: initModalDialogKeystrokes,
        spotStack: spotStack,
        saveFocus: saveFocus,
        returnFocus: returnFocus,
        clearFocus: clearFocus,
        getTopFocusElement: getTopFocusElement,
        updateFocusAfterDelete: updateFocusAfterDelete,
        getTableCellCoords: getTableCellCoords,
        encodeData: encodeData,
        escapeString: escapeString
    };

})();