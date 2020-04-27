/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

var listUtils = (function() {
    "use strict";

    /*
     * Removes the previous selected highlight and highlights the new selected list item
     */
    var __highlightSelectedListItem = function(listSearchId, button){
        $("#" + listSearchId + "_listViewList .listViewListItemSelected").removeClass("listViewListItemSelected");
        if(button){
            button.addClass('listViewListItem listViewListItemSelected');
        }
    };
    
    /*
     * Scroll to the list item in the list
     */
    var __scrollToListItem = function(rootListId, listItemName){
        var list = $('#' + rootListId + "_listViewList"), 
        
        // Scroll to first item containing search query in the list
        scrollTo = $('#' + rootListId + "_listViewList a:contains(" + listItemName + ")");
        if(scrollTo.length !== 0){
            list.animate({
                scrollTop: scrollTo.offset().top - list.offset().top + list.scrollTop() - 11
            });
        }
    };
    
    /*
     * Filter the list when the user types in the search field or in the field on the left pane
     */
    var __renderList = function(rootListId, list, filterQuery) {
        if (list[rootListId]) {
            if (list[rootListId].localRepository === "true") {
                // not going to switch view
                return;
            } else {
                $('#' + rootListId + "_listViewList").show();
                $('#' + rootListId + "_listViewError").hide();

                // clean out existing selection
                var currentSelectedListItem =  $("#" + rootListId + " button.listViewListItemSelected");
                if (currentSelectedListItem.length > 0) {
                    //currentSelectedListItem.removeClass("clusterListItemSelected");
                    currentSelectedListItem.removeClass("listViewListItemSelected");
                    if (filterQuery !== list[rootListId].selectedListItem){
                        __setSelectedListItem("", list[rootListId]);
                    }
                }

                //Filter the clusters by their search query
                var filteredList = [];
                var allListItems = list[rootListId].listItems;

                var filter = (filterQuery) ? filterQuery.toLowerCase() : $('#' + rootListId + "_listSearchField").val().toLowerCase();
                var i;

                for(i = 0; i < allListItems.length; i++) {
                    var listItem = allListItems[i].toLowerCase();
                    if(listItem.indexOf(filter) !== -1) {
                        filteredList.push(allListItems[i]);
                    }
                }

                // Sort the filtered list
                filteredList.sort(function(a, b) {
                    return (a > b)? 1 : (a < b)? -1 : 0;
                });

                // Populate list 
                var currentList = $('#' + rootListId + "_listViewList");
                var selectedListItem = list[rootListId].selectedListItem;
                currentList.empty();
                var selected = false;

                for(i = 0; i < filteredList.length; i++) {
                    var row =  document.createElement('button');            
                    row.setAttribute('data-listName', filteredList[i]);
                    if (selectedListItem === filteredList[i] && selected === false) {  
                        selected = true;
                        row.setAttribute('class', 'listViewListItem listViewListItemSelected');
                    } else {
                        row.setAttribute('class', 'listViewListItem');
                    }

                    var listItemName = document.createElement('a');
                    // handle bidi
                    utils.setBidiTextDirection(listItemName, true);
                    // set hover over
                    listItemName.title = filteredList[i];
                    listItemName.innerHTML = filteredList[i];
                    row.appendChild(listItemName);

                    currentList.append(row);
                }

                // Scroll to the cluster if user clears their filter
                if(filter !== ""){
                    __scrollToListItem(rootListId, selectedListItem);
                }

                return selected;
            }
        } else {
            $('#' + rootListId + "_listViewList").hide();
            $('#' + rootListId + "_listViewError").show();
        }
    };
    
    var __checkIfListItemExists = function(rootListId, listItemName, list){
        // When there is no docker repository provided or an invalid docker repository is provided,
        // there is no list created to store the list items.
        if (list[rootListId] && list[rootListId].listItems && list[rootListId].listItems.length > 0) {
            var listItemMatches = $("#" + rootListId + "_listViewList a:contains(" + listItemName + ")");
        
            // Check for each list item selected by JQuery individually because contains could have extras
            for(var i=0; i< listItemMatches.length; i++){
                var listItem = listItemMatches[i].innerHTML;
                if(listItem === listItemName){
                    // Add selected css class to the row
                    var listItemRow = $(listItemMatches[i]).parent();
                    __highlightSelectedListItem(rootListId, listItemRow);
                    return true;
                }
            }
            return false;
        } else {
            return true;
        }
    };

    var __setSelectedListItem = function(selectedListItem, selectedList){
        selectedList.selectedListItem = selectedListItem;
    };
    
    var __isInputInFocus = function(listSearchId, id) {
        return  ($("#" + listSearchId).attr("focusId") === id);
    };
    
    var __removeInputInFocus = function(listSearchId) {
        $("#" + listSearchId).removeAttr("focusId");
    };
    
    var __getInputInFocus = function(listSearchId) {
        return $("#" + listSearchId).attr("focusId");
    };
    
    return {
        highlightSelectedListItem: __highlightSelectedListItem,
        scrollToListItem: __scrollToListItem,
        renderList: __renderList,
        checkIfListItemExists: __checkIfListItemExists,
        setSelectedListItem: __setSelectedListItem,
        isInputInFocus: __isInputInFocus,
        removeInputInFocus: __removeInputInFocus,
        getInputInFocus: __getInputInFocus
    };
})();
