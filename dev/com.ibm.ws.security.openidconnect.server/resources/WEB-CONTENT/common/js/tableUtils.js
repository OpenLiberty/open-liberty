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

var tableUtils = (function() {
    "use strict";

    var __pageSize = 20;   // Page displays 20 items/page
    var __noResults = 'noResults';   // Classname marking row that shows no results

    /**
     * Common keyboard initialization for the sort column associated with the
     * filter value.
     * 
     * @param String Column name that will be sorted
     */
    var initTableSorting = function(sortColumnName) {
        // Sort column button on Client Name <th>
        $('#' + sortColumnName).on('click keydown', (function(event) {
            var sortTable = true;
            if (event.type === "keydown") {
                var key = event.which || event.keyCode;
                if (key !== 13 &&     // Enter key pressed
                    key !== 32 ) {    // Space key pressed
                    sortTable = false;
                }
            }
            if (sortTable) {
                event.preventDefault();
                tableUtils.toggleTableSort(sortColumnName);
            }
        }));
        //          Show sort icon only on button hover or focus.
        $(".table_sort_column").hover(function() {
            // mouseenter
            $(this).find('span').css('visibility', 'visible');
        }, function() {
            // mouseleave
            $(this).find('span').css('visibility', 'hidden');
        });
        $(".table_sort_column").focus(function() {
            $(this).find('span').css('visibility', 'visible');
        });
        $(".table_sort_column").blur(function() {
            $(this).find('span').css('visibility', 'hidden');
        });
    };

    /**
     * Common keyboard initialization for the filter field.
     * 
     * @param String filterInputField - name of the input field for the filter
     * @param String filterClearButton - name of the 'x' button to clear the filter
     */
    var initTableFilter = function(filterInputField, filterClearButton) {
        // Filter field
        $('#' + filterInputField).keyup(function(event) {       // Filter field
            var keycode = (event.keyCode ? event.keyCode : event.which);
            if(keycode == '9') {   // Tab key
                return;
            }
            var searchValue = $(this).val();
            tableUtils.filterRows(table.tableId, searchValue);
        });
        //         Clear filter input when requested
        $('#' + filterClearButton).click(function() {      // 'x' in filter field
            var $searchInputField = $(this).siblings(".tool_filter_input");
            $searchInputField.val("").focus().trigger({ type : 'keyup', which : 13 });
        });
    };

    /**
     * Common keyboard initialization for the paging buttons
     */
    var initTablePaging = function() {
        // Paging selection on table footer
        $('select.tool_table_page_select_input').on('change', function() {
            var pageSelected = $(this).find(":selected").val();
            tableUtils.switchPage(table.tableId, parseInt(pageSelected));
        });
        // Paging forward and backward buttons
        $('.tool_table_pagination_button').on('click', function() {
            // Get the current page from the value of the select box
            var currentPage = tableUtils.currentPage();
            if ($(this).hasClass('tool_table_pagination_button_backward') && currentPage > 1) {
                 currentPage -= 1;
            } else {
                currentPage += 1;
            }
            tableUtils.switchPage(table.tableId, currentPage);
        });
    };

    /**
     * Add row to the table.
     * 
     * @param String tableId - ID of the application table
     * @param String rowToAdd - html String of row to add
     * @param Boolean rowMatched - true if the row being added to the table matches the current filter
     *                             false if it is not a match
     *              NOTE: the accountAdmin and tokenAdmin applications did not implement
     *                    filtering (yet) so they will always have rowMatched = true.  
     *                    Setting default to true below.
     */
    var addToTable = function(tableId, rowToAdd, rowMatched) {
        var $table = $('#' + tableId + ' tbody');
        var $addRow = $(rowToAdd);

        if ($addRow.hasClass(__noResults)) {
            $table.prepend($addRow);    // Add 'No results' message as FIRST row in table.
            return;
        }

        var $rows = $table.find('tr:not(".rowHidden")');
        if ($rows.length >= __pageSize) {
            $addRow.addClass("rowHidden");
        } else if ($rows.length === 1 && $table.find('tr.' + __noResults).length > 0) {
            // Remove the no results row so we can append the first client to the table
            removeNoDataMessage(tableId);
        }

        if (rowMatched ||
            rowMatched === undefined) {     // For the accountAdmin and tokenAdmin apps
                                            // mark ALL rows as 'rowMatched'
            $addRow.addClass("rowMatched");
        } else {
            $addRow.addClass("rowHidden");
        }

        $table.append($addRow);

        // Set up keystroke traversal on the row
        initTableKeyTraversing(tableId, $addRow);
    };

    /**
     * Determines the data that should be displayed on the indicated page number
     * and update the table footer with the selected page information.
     * 
     * @param String tableId - ID of the application table
     * @param int pageNumber - page number to display
     */
    var switchPage = function(tableId, pageNumber) {
        var $table = $('#' + tableId + ' tbody');

        var $eligibleRows = $table.find('tr.rowMatched');
        if ($eligibleRows.length > 0) {
            // Validate the requested pageNumber
            var numPages = Math.ceil($eligibleRows.length/__pageSize);
            if (pageNumber > numPages) {
                pageNumber = numPages;  // Select the last page
            }

            var startShowing = (pageNumber - 1) * __pageSize;
            if ($eligibleRows.length > startShowing) {
                if (startShowing >= __pageSize) {
                    // Hide the rows before the one we should start showing
                    $eligibleRows.slice(0, startShowing).addClass("rowHidden");
                }
                // Hide the rows following the page of the ones we should be showing
                $eligibleRows.slice(startShowing + __pageSize).addClass("rowHidden");
                // Show the rows on the page we are going to
                $eligibleRows.slice(startShowing, startShowing + __pageSize).removeClass("rowHidden");
            } else {
                $eligibleRows.removeClass("rowHidden");
            }
        } else {
            addNoDataMessage(tableId);
            pageNumber = 1;  // Only one page to show...the one with the No Data message
        }

        // Update the paging info in the table footer
        updateTablePagingInfo($eligibleRows.length, pageNumber);
    };

    /**
     * Returns the currently selected page number
     */
    var currentPage = function() {
        return parseInt($('.tool_table_pagination .tool_table_page_select_input').val());
    };

    /**
     * Updates the footer with the current paging information for the table.
     * 
     * @param int numElements - total number of elements to display in the table
     * @param int page (optional) - page number loaded.  If null, page 1 is assumed
     */
    var updateTablePagingInfo = function(numElements, page) {
        if (page===undefined || page===null) {
            page = 1;
        }

        var numPages = numElements===0 ? 1: Math.ceil(numElements/__pageSize);

        var pagesMessage = utils.formatString(messages.PAGES, [page, numPages]);
        $('.tool_table_pagination .tool_table_page_number_label').html(pagesMessage);

        var $pageSelect = $('.tool_table_pagination .tool_table_page_select_input');
        $pageSelect.find('option').remove();

        var pageOptions = [];
        for (var i = 0; i<numPages; i++) {
            pageOptions[i] = "<option class='select-option' value='" + (i + 1) + "'>" + (i + 1) + "</option>";
        }
        $pageSelect.get(0).innerHTML = pageOptions.join('');
        $pageSelect.prop("selectedIndex", page-1);

        if (page > 1) {
            $('.tool_table_pagination .tool_table_pagination_button_backward').prop("disabled", false);
        } else {
            $('.tool_table_pagination .tool_table_pagination_button_backward').prop("disabled", true);
        }

        if (page < numPages) {
            $('.tool_table_pagination .tool_table_pagination_button_forward').prop("disabled", false);
        } else {
            $('.tool_table_pagination .tool_table_pagination_button_forward').prop("disabled", true);
        }

    };

    /**
     * Find rows based on the complete filter value.  Display the matched rows
     * beginning on page one.
     * 
     * This Filter is based on the tr's being created with a data-filter value
     * that is lowercased.
     * 
     * @param String tableId - ID of the application table
     * @param string filterValue - value to filter on
     */
    var filterRows = function(tableId, filterValue) {
        var filter = filterValue.trim().toLowerCase();  // Stored the name in <tr> element
                                                        // lower-cased for a case insensitive search
        filter = utils.escapeString(filter);                                                        

        var $table = $('#' + tableId + ' tbody');

        if (filter === "") {
            // Remove 'No results' msg row if it exists
            $table.find("tr." + __noResults).remove();
            // Display all rows in the table
            $table.find('tr').addClass('rowMatched');
            $('.tool_filter_clear').css({"visibility": "hidden"});
        } else {
            // Display the clear search button
            $('.tool_filter_clear').css({"visibility": "visible"});

            // Mark the rows that match the filter
            $table.find('tr').removeClass('rowMatched');
            $table.find("tr[data-filter*='" + filter + "']").addClass('rowMatched');
            // Hide the rows that do not match the filter
            $table.find("tr:not('.rowMatched')").addClass('rowHidden');
        }

        // Switch to page one to show just the newly filtered values
        switchPage(tableId, 1);
    };

    /**
     * OnClick handler for the filter column.  Toggles the sort direction.
     * 
     * If sort has not yet been set, default to ascending.
     * 
     * @param String columnId - Id of the column to be sorted for the particular
     *                          application table.
     */
    var toggleTableSort = function(columnId) {
        var direction = currentSortDirection(columnId);

        // Switch the icon in the column to show the direction of the sort
        var $sortColumn = $('#' + columnId);
        var $sortImgSpan = $sortColumn.find("span");
        if (direction === "ascending") {
            // Swap direction to descending
            direction = "descending";
            $sortColumn.attr("aria-sort", direction);
            $sortImgSpan.data("sortdir", direction);
            $sortImgSpan.find('img').attr('src', '../../WEB-CONTENT/common/images/caretSortDown.svg');
        } else {
            // Swap direction to ascending;  If sort had not yet been selected,
            // initialize to ascending.
            direction = "ascending";
            $sortColumn.attr("aria-sort", direction);
            $sortImgSpan.data("sortdir", direction);
            $sortImgSpan.find('img').attr('src', '../../WEB-CONTENT/common/images/caretSortUp.svg');
        }

        var tableId = $sortColumn.closest('table').prop('id');
        sortTableWithFilterName(tableId, direction === "ascending");

        var screenReaderMsg;
        if (direction === "ascending") {
            screenReaderMsg = utils.formatString(messages.TABLE_FIELD_SORT_ASC, [$sortColumn.prop('innerText')]);
        } else {
            screenReaderMsg = utils.formatString(messages.TABLE_FIELD_SORT_DESC, [$sortColumn.prop('innerText')]);
        }
        $('#accessibleLiveRegion').text(screenReaderMsg);
        setTimeout(function() {
            $('#accessibleLiveRegion').html('');
        }, 2000);

        // Switch to page 1 following a new sort request
        switchPage(tableId, 1);
    };

    /**
     * Determines the current sort direction.
     * @returns String "ascending" if currently ascending
     *                 "descending" if currently descending
     *                 "none" if sort direction has not yet been selected
     * 
     * @param String columnId - Id of the column to be sorted
     */
    var currentSortDirection = function(columnId) {
        var $imgSpan = $('#' + columnId).find("span");
        return($imgSpan.data("sortdir"));
    };

    /**
     * Sort the rows of the table according to the filter value in requested order.
     * The sort is case-insensitive because the value used for sorting was made
     * lower-case when the row was created  (See the tr's data-filter value).
     * 
     * @param  boolean sortAscending - true to sort table rows in ascending order  (A -> Z)
     *                                 false to sort table rows in descending order (Z -> A)
     */
    var sortTableWithFilterName = function(tableId, sortAscending) {
        //Sort Table rows according to desired direction
        var $table = $('#' + tableId + ' tbody');
        var $trArray = $table.find('tr');
        __order($trArray, sortAscending, function(row) {
            return ($(row).data('filter'));   // NOTE: <tr> data-filter attribute is already in lower-case
        });

        // Re-attach the rows to the table in sorted order
        $trArray.each(function(i) {
            $table.get(0).appendChild(this);
        });
    };

    /**
     * Reorders an Array of the rows in the table in ascending or descending order
     *  
     * @param [tr] rows - Array of table row (<tr>) html elements to sort
     * @param boolean asc - True to sort in ascending order; false for descending
     * @param Function fn - Function that returns the value to be used in the sort for the row
     */
    var __order = function(rows, asc, fn) {
        // Using an Array sort to sort the array of rows
        //      ... If the result is negative a is sorted before b.
        //          If the result is positive b is sorted before a.
        //          If the result is 0 no change is done with the sort order of the two values.
        if (asc) {   // Sort in ascending order
            rows.sort(function(a, b) {     
                a = fn(a);
                b = fn(b);
                if (a == b) return 0;
                return a > b ? 1: -1;
            });
        } else {     // Sort in descending order
            rows.sort(function(a, b) {
                a = fn(a);
                b = fn(b);
                if (a == b) return 0;
                return a < b ? 1: -1;
            });
        }
/* TODO:  Look into javascript localeCompare() https://www.w3schools.com/jsref/jsref_localecompare.asp 
                                               https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/String/localeCompare
                                               https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Collator
*/        
     };

     /**
      * Add the "No results" row to the table
      * 
      * @param Sring tableId - ID of the appliation table
      */
    var addNoDataMessage = function(tableId) {
        if ($('#' + tableId + ' tbody tr.' + __noResults).length === 0) {
            // Message isn't already showing.....so add one.
            var numTableCols = $('#' + tableId + ' th').length;   // No results message spans all columns in table
            var noResultsMessage = "<td colspan='" + numTableCols + "' tabindex='-1'>" + messages.NO_RESULTS_FOUND + "</td>";
            var tableRow = "<tr class='" + __noResults + "'>" + noResultsMessage + "</tr>";
    
            addToTable(tableId, tableRow);
            initTableKeyTraversing(tableId, $('#' + tableId + ' tbody tr.' + __noResults));
        }
        $('#' + tableId + ' tbody tr.' + __noResults).removeClass('rowHidden');
    };

    var removeNoDataMessage = function(tableId) {       
        // If the 'no results' message is displaying, remove the row
        $('#' + tableId + ' tbody tr.' + __noResults).remove();
    };

    /**
     * Sets up key traversing wtihin the main application tables
     * 
     * @param String tableId - ID of the table
     * @param {*} $row - JQuery object representing a row in the table
     */
    var initTableKeyTraversing = function(tableId, $row) {
        var $table = $('#' + tableId);

        var $cells = $row.find('td');
        var headerRow = false;
        if ($cells.length === 0) {
            // Working with the table header row
            $cells = $row.find('th');
            headerRow = true;
        }

        $cells.on('keydown', function(event) {
            var key = event.which || event.keyCode;
            var ctrlPressed = event.ctrlKey;

            var $this = $(this);           // Event cell
            var cellIndex = $this.index();

            if (ctrlPressed) {
                var $visibleRows = $table.find("tbody tr:not('.rowHidden')");
                // if (headerRow) {
                //     $visibleRows = $table.find("thead tr");
                // }
                switch(key) {
                    case 36:        // home
                        // Moves focus to the first cell in the first row.
                        event.preventDefault();
                        var $firstCellFirstRow = $visibleRows.find('td').first();
                        if ($firstCellFirstRow.has('button, input').length) {
                            $firstCellFirstRow.children().first().focus();
                        } else {
                            $firstCellFirstRow.focus();
                        }                       
                        break;
                    
                    case 35:        // end
                        // Moves focus to the last cell in the last row.
                        event.preventDefault();
                        var $lastCellLastRow = $visibleRows.find('td').last();
                        if ($lastCellLastRow.has('button, input').length) {
                            $lastCellLastRow.children().last().focus();
                        } else {
                            $lastCellLastRow.focus();
                        }
                        break;
                }
            } else {    // Ctrl key was not pressed
                switch(key) {
                    case 40:        // arrow down
                        // Moves focus one cell down. If focus is on the bottom
                        // cell in the column, focus does not move.
                        event.preventDefault();
                        var $cellsBelow = $this.closest('tr').next('tr:not(".rowHidden")').children();
                        if (headerRow) {
                            // At header row.  Move into the tbody rows.
                            $cellsBelow = $this.closest('thead').siblings('tbody').find('tr:not(".rowHidden")').first().children();
                        }
                        var $cellBelow = $cellsBelow.eq(cellIndex);
                        if ($cellBelow.length === 0) {
                            // Row contains a message like '__noResults' so only one spanning multiple columns
                            $cellBelow = $cellsBelow.eq(0);
                        }
                        if ($cellBelow.length) {
                            if ($cellBelow.has('button, input').length) {
                                $cellBelow.children().first().focus();
                            } else {
                                $cellBelow.focus();
                            }
                        }  // else at bottom row of the table
                        break;
                    
                    case 38:        // arrow up
                        // Moves focus one cell Up. If focus is on the top cell
                        // in the column, focus moves to associated header cell.
                        var $cellAbove = $this.closest('tr').prev('tr:not(".rowHidden")').children().eq(cellIndex);
                        if ($cellAbove.length === 0) {
                            // At top tbody row.  Place focus in associated header cell.
                            $cellAbove = $table.find('thead tr').children().eq(cellIndex);
                        }
                        if ($cellAbove.has('button, input').length) {
                            $cellAbove.children().first().focus();
                        } else {
                            $cellAbove.focus();
                        }
                        break;
    
                    case 37:        // left arrow
                        // Moves focus one cell to the left. If focus is on the
                        // left-most cell in the row, focus does not move.
                        event.preventDefault();
                        var $prevTD = $(this).prev('td, th');
                        if ($prevTD.has('button, input').length) {
                            $prevTD.children().first().focus();
                        } else {
                            $prevTD.focus();
                        }
    
                        break;
    
                    case 39:        // right arrow
                        //  Moves focus one cell to the right. If focus is on the
                        //  right-most cell in the row, focus does not move.
                        event.preventDefault();
                        var $nextTD = $(this).next('td, th');
                        if ($nextTD.has('button, input').length) {
                            $nextTD.children().first().focus();
                        } else {
                            $nextTD.focus();
                        }
                        break;
    
                    case 36:        // home
                        // Moves focus to the first cell in the row.
                        event.preventDefault();
                        var $firstCell = $cells.first();
                        if ($firstCell.has('button, input').length) {
                            $firstCell.children().first().focus();
                        } else {
                            $firstCell.focus();
                        }
                        break;
                        
                    case 35:        // end
                        // Moves focus to the last cell in the row.
                        event.preventDefault();
                        var $lastCell = $cells.last();
                        if ($lastCell.has('button, input').length) {
                            $lastCell.children().first().focus();
                        } else {
                            $lastCell.focus();
                        }
                        break;
                }    
            }
            
        });

    };

    return {
        initTableSorting: initTableSorting,
        initTableFilter: initTableFilter,
        initTablePaging: initTablePaging,
        addToTable: addToTable,
        switchPage: switchPage,
        currentPage: currentPage,
        updateTablePagingInfo: updateTablePagingInfo,
        filterRows: filterRows,
        toggleTableSort: toggleTableSort,
        currentSortDirection: currentSortDirection,
        sortTableWithFilterName: sortTableWithFilterName,
        addNoDataMessage: addNoDataMessage,
        removeNoDataMessage: removeNoDataMessage,
        initTableKeyTraversing: initTableKeyTraversing
    };

})();