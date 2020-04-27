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

goog.require("inputType");
goog.provide("listType");

var list = (function(){
    "use strict";

    var ListType = function(inputVariable) {
        string.stringType.call(this, inputVariable);
        this.listInputId = this.id;
        this.listSearchId = idUtils.getListId(inputVariable);
        this.waitStatus = {
                loading: 1,
                done: 2
        };
        this.loadingStatus = this.waitStatus.loading;

        // look for existing list object belonging to this card
        this.listElement = $("#" + this.listSearchId);
        if (this.listElement.length === 0) {
            // create listElement for this card
            this.listElement = __createListSectionClone(this.listSearchId);
        }

        var rightPaneId = idUtils.getCardRightPaneId(inputVariable);
        $("#" + rightPaneId).append(this.listElement);
        // ToDo: to be updated by the extension
        $("#" + rightPaneId).closest(".parameters").attr("aria-label", messages.PARAMETERS_DOCKER_ARIA);

        __hideRightPanelElements(this.inputVariable);
        $('#' + this.listSearchId).removeClass("hidden");

        // handle bidi for search image
        utils.setBidiTextDirection($("#" + this.listSearchId + "_listSearchField"));
    };

    // Clone the list section and change the id's to match the input group's list section
    var __createListSectionClone = function(id) {
        var newListSearchElement = $("#listSearch").clone().attr("id", id).removeClass("hide");

        newListSearchElement.find("#listSearchIconSpan").attr("id", id + "_listSearchIconSpan");
        newListSearchElement.find("#listSearchIcon").attr("id", id + "_listSearchIcon");
        newListSearchElement.find("#listSearchFieldLabel").attr("id", id + "_listSearchFieldLabel");
        newListSearchElement.find("#listSearchField").attr("aria-labelledby", id + "_listSearchFieldLabel");
        newListSearchElement.find("#listSearchField").removeClass("hide");
        newListSearchElement.find("#listSearchField").attr("id", id + "_listSearchField");
        newListSearchElement.find("#listView").attr("id", id + "_listView");
        newListSearchElement.find("#listViewList").attr("role", "application");
        newListSearchElement.find("#listViewList").attr("id", id + "_listViewList");
        newListSearchElement.find("#listViewError").attr("id", id + "_listViewError");
        newListSearchElement.find("#listViewErrorLink").attr("id", id + "_listViewErrorLink");
        newListSearchElement.find("#listViewErrorIcon").attr("id", id + "_listViewErrorIcon");
        newListSearchElement.find("#listViewErrorMessage").attr("id", id + "_listViewErrorMessage");
        newListSearchElement.find("#listViewErrorFooter").attr("id", id + "_listViewErrorFooter");

        return newListSearchElement;
    };

    ListType.prototype = $.extend ({
        displayLoading: function() {
            if (this.loadingStatus === this.waitStatus.loading) {
                loadingUtils.displayLoader(this.listSearchId);
            }
        },

        setLoadingStatus: function(newStatus) {
            this.loadingStatus = newStatus;
        },

        isLoadingDone: function() {
            return (this.loadingStatus === this.waitStatus.done);
        }
    }, string.stringType.prototype);

    ListType.prototype.addInputListener = function() {
        string.stringType.prototype.addInputListener.call(this);
        __setSearchFieldListener(this);
    };

    var __hideRightPanelElements = function(inputVariable) {
        var fileBrowserForCard = $('#' + idUtils.getBrowseAndUploadId(inputVariable));
        if (fileBrowserForCard.length > 0) {
            fileBrowserForCard.addClass("hidden");
        }
    };

    var __setSearchFieldListener = function(me) {
        // Listens to the search field above the list
        $("#" + me.listSearchId + "_listSearchField").attr('oninput', '').unbind("input");
        $("#" + me.listSearchId + "_listSearchField").on("input", function() {
            // once search string is inputed, clear out previous imageName
            $("#" + me.id).val("");
            if (me.inputVariable.type === "dockerImage") {
                dockerUtils.renderDockerImages(me.listSearchId, $(this).val());
            } else {
                clusterUtils.renderClusters(me.listSearchId, $(this).val());
            }
        });

        $("#" + me.listSearchId + "_listView").attr('onclick', '').unbind("click");
        $("#" + me.listSearchId + "_listView").on("click", ".listViewListItem", function(event) {
            event.preventDefault();
            var name = event.currentTarget.getAttribute("data-ListName");
            var input = $("#" + me.listInputId)[0];
            // trigger the change event manually
            $("#" + me.listInputId).val(name).trigger("change");
            validateUtils.validateInputField(input, false);

            if (me.inputVariable.type === "dockerImage") {
                dockerUtils.setSelectedImage(name, me.listSearchId);
            } else {
                console.log("calling clusterUtils");
                clusterUtils.setSelectedCluster(name, me.listSearchId);
            }
            listUtils.highlightSelectedListItem(me.listSearchId, $(this));
            me.clearFieldButton.show();

            // Checks if the form is complete and shows the deploy button if so
            validateUtils.validate();
        });

        $("#" + me.listSearchId + "_listView").attr('onkeydown', '').unbind("keydown");
        $("#" + me.listSearchId + "_listView").on("keydown", ".listViewListItem", function(e) {
            // Shift+tab, send focus to the search bar
            if(e.shiftKey && e.keyCode === 9){
                e.preventDefault();
                $(this).closest(".parameters").find(".listSearchField").focus();
            }
            // Tab key, skip to the next section
            else if(e.which === 9){
                e.preventDefault();
                if($(this).closest(".parameters").nextAll(".card:visible")[0]){
                    $(this).closest(".parameters").nextAll(".card:visible")[0].focus();
                } else{
                    $("#review").focus();
                }
            }
            // Down key or right key
            else if(e.which === 39 || e.which === 40){
                e.preventDefault();
                // Focus next item if not the last item
                if($(this).nextAll(".listViewListItem")[0]){
                    $(this).nextAll(".listViewListItem")[0].focus();
                }
            }
            // Up key or left key
            else if(e.which === 37 || e.which === 38){
                e.preventDefault();
                // Focus previous item if not the first item
                if($(this).prevAll(".listViewListItem")[0]){
                    $(this).prevAll(".listViewListItem")[0].focus();
                }
            }
        });
    };

    return {
        /*
         * This input type is not to be used directly. It is used as a base type for
         * docker and cluster types.
         */
        listType: ListType,
        hideRightPanelElements: __hideRightPanelElements,
        setSearchFieldListener: __setSearchFieldListener
    };

})();
