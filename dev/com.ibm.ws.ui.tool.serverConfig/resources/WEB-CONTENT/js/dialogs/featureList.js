/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

$(document).ready(function() {
    "use strict";

    $("#dialogSelectFeature").on("show.bs.modal", function(event) {

        // Obtain dialog
        var dialogSelectFeature = $("#dialogSelectFeature");

        // Obtain input control
        var inputControl = dialogSelectFeature.data("inputControl");

        // Obtain current value
        var currentValue = inputControl.val();

        // Clear dialog body
        $("#dialogSelectFeatureListContainer").empty();

        // Obtain feature list
        var featureList = editor.getFeatureList();

        var featureMatch = false;

        // Add features to dialog
        for(var property in featureList) {
            if(featureList.hasOwnProperty(property)){
                var featureObject = featureList[property];

                var item = $("<div role=\"radio\" aria-checked=\"false\" data-feature-name=\"" + property + "\" class=\"dialogSelectFeatureListItem noSelect\">" + property + "</div>");

                if(currentValue.length > 0 && currentValue === property) {
                    featureMatch = true;
                    item.addClass("active");
                    item.attr("tabindex", "0");
                    $("#dialogSelectFeatureDescription").val(featureObject.description);
                }

                $("#dialogSelectFeatureListContainer").append(item);
            }
        }

        // Clear search
        $("#dialogSelectFeatureSearch").val("");

        // Update feature description
        if(!featureMatch) {
            $("#dialogSelectFeatureDescription").val(editorMessages.SELECT_FEATURE_TO_VIEW_DESCRIPTION);

            // Disable OK button
            $("#dialogSelectFeatureOKButton").attr("disabled", "disabled").attr("tabindex", -1);

        } else {

            // Enable "OK" button
            $("#dialogSelectFeatureOKButton").removeAttr("disabled").removeAttr("tabindex");

        }

        // Handle bidi
        if(globalization.isBidiEnabled()) {
            var dirValue = globalization.getBidiTextDirection();
            if(dirValue !== "contextual") {
                $("#dialogSelectFeatureSearch").attr("dir", dirValue);
            }
        }

        // Center dialog
        core.centerDialog(this);
    });


    $("#dialogSelectFeatureSearch").on("input", function(event) {

        // Handle bidi
        if(globalization.isBidiEnabled() && globalization.getBidiTextDirection() === "contextual") {
            $(this).attr("dir", globalization.obtainContextualDir(event.currentTarget.value));
        }

        // Clear dialog body
        $("#dialogSelectFeatureListContainer").empty();

        // Obtain filter
        var filter = event.currentTarget.value.toLowerCase();

        // Obtain feature list
        var featureList = editor.getFeatureList();

        // Add features to dialog
        for(var property in featureList) {
            if(property.toLowerCase().indexOf(filter) !== -1) {
                var item = $("<div role=\"radio\" aria-checked=\"false\" data-feature-name=\"" + property + "\" class=\"dialogSelectFeatureListItem noSelect\">" + property + "</div>");
                $("#dialogSelectFeatureListContainer").append(item);
            }
        }

        var firstItem = $("#dialogSelectFeatureListContainer .dialogSelectFeatureListItem:first");
        if(firstItem.length === 1) {
            firstItem.addClass("active");
            firstItem.attr("tabindex", "0");

            // Update description
            var featureName = firstItem.data("feature-name");
            $("#dialogSelectFeatureDescription").val(featureList[featureName].description);

            // Enable "OK" button
            $("#dialogSelectFeatureOKButton").removeAttr("disabled").removeAttr("tabindex");
        } else {

            $("#dialogSelectFeatureDescription").val(editorMessages.NO_MATCHES_FOUND);

            // Disable OK button
            $("#dialogSelectFeatureOKButton").attr("disabled", "disabled").attr("tabindex", -1);
        }

    });


    $("#dialogSelectFeature").on("keydown", function(event) {
        if(event.keyCode === 13) {
            var eventTarget = $(event.target);
            if(eventTarget.hasClass("dialogSelectFeatureListItem") && eventTarget.hasClass("active")) {
                event.preventDefault();
                $("#dialogSelectFeatureOKButton").trigger("click");
            }
        }
    });


    $("#dialogSelectFeatureSearch").on("keydown", function(event) {
        if(event.keyCode === 40) {
            event.preventDefault();
            var selectedOption = $("#dialogSelectFeatureListContainer .dialogSelectFeatureListItem.active");
            if(selectedOption.length) {
                selectedOption.focus();
            } else {
                var firstOption = $("#dialogSelectFeatureListContainer .dialogSelectFeatureListItem:first");
                firstOption.click().attr("tabindex", "0").focus();
            }
        } else if(event.keyCode === 13 && $("#dialogSelectFeatureListContainer .active").length === 1) {
            event.preventDefault();
            $("#dialogSelectFeatureOKButton").trigger("click");
        }
    });


    $("#dialogSelectFeatureListContainer").on("keydown", function(event) {

        var selectedItem = $("#dialogSelectFeatureListContainer .dialogSelectFeatureListItem.active");
        if(!selectedItem.length) {
            selectedItem = $("#dialogSelectFeatureListContainer .dialogSelectFeatureListItem:focus");
        }

        if("WebkitAppearance" in document.documentElement.style) {
            selectedItem.css("outline", "");
        }

        if(selectedItem.length) {

            var targetItem = null;

            switch(event.keyCode) {
            case 32:
                targetItem = selectedItem;
                break;
            case 36:
                targetItem = $("#dialogSelectFeatureListContainer .dialogSelectFeatureListItem:first");
                break;
            case 35:
                targetItem = $("#dialogSelectFeatureListContainer .dialogSelectFeatureListItem:last");
                break;
            case 40:
                targetItem = selectedItem.next();
                break;
            case 38:
                targetItem = selectedItem.prev();
                break;
            }

            if(targetItem !== null && targetItem !== undefined && targetItem.length) {
                event.preventDefault();
                $("#dialogSelectFeatureListContainer .dialogSelectFeatureListItem").removeClass("active").removeAttr("tabindex").attr("aria-checked", false);
                targetItem.click().attr("tabindex", 0).focus();
                targetItem.attr("aria-checked", true);
            }
        }

    });


    $("#dialogSelectFeatureListContainer").on("click", ".dialogSelectFeatureListItem", function(event) {
        // Clear current selection
        $("#dialogSelectFeatureListContainer .active").removeClass("active");

        // Obtain selected item
        var selectedListItem = $(event.currentTarget);

        // Mark selected item
        selectedListItem.addClass("active");
        selectedListItem.attr("aria-checked",true);

        // Obtain feature list
        var featureList = editor.getFeatureList();

        // Update description
        var description = featureList[selectedListItem.data("feature-name")].description;
        $("#dialogSelectFeatureDescription").val(description);

        // Enable "OK" button when a selection is made
        $("#dialogSelectFeatureOKButton").removeAttr("disabled").removeAttr("tabindex");

    });



    $("#dialogSelectFeatureListContainer").on("dblclick", function(event) {
        $("#dialogSelectFeatureOKButton").trigger("click");
    });


    $("#dialogSelectFeatureListContainer").on("mousedown", ".dialogSelectFeatureListItem", function(event) {

        // Obtain selected item
        var selectedListItem = $(event.currentTarget);

        if("WebkitAppearance" in document.documentElement.style) {
            selectedListItem.css("outline", "none");
        }

        if(!selectedListItem.hasClass("active")) {
            $("#dialogSelectFeatureListContainer .dialogSelectFeatureListItem").removeAttr("tabindex");
            selectedListItem.attr("tabindex", "0");
            selectedListItem.focus();
        }
    });


    $("#dialogSelectFeatureOKButton").on("click", function(event) {

        // Obtain dialog
        var dialogSelectFeature = $("#dialogSelectFeature");

        // Obtain input control
        var inputControl = dialogSelectFeature.data("inputControl");

        var currentValue = inputControl.val();

        // Obtain selected value
        var value = $("#dialogSelectFeatureListContainer .active").data("feature-name");

        if(currentValue !== value) {

            // Update input control value
            inputControl.val(value);

            // Trigger change
            $(inputControl).trigger("input");

            // Mark document as dirty
            editor.markDocumentAsDirty();

        }

        // Close dialog
        event.preventDefault();
        $("#dialogSelectFeature").modal("hide");

        // Focus select button
        dialogSelectFeature.data("selectButton").focus();

    });


    $("#dialogSelectFeature").on("shown.bs.modal", function(event) {
        var selectedFeature = $("#dialogSelectFeature .dialogSelectFeatureListItem.active");
        if(selectedFeature.length === 1) {
            selectedFeature.focus();
        } else {
            $("#dialogSelectFeatureSearch").focus();
        }
    });


    $("#editorForm").on("click", ".featureListSelectButton", function(event) {
        event.preventDefault();

        // Obtain target button
        var targetButton = $(event.target);

        // Obtain dialog
        var dialogSelectFeature = $("#dialogSelectFeature");

        // Pass information
        dialogSelectFeature.data("inputControl", targetButton.data("inputControl"));
        dialogSelectFeature.data("selectButton", targetButton);

        // Open dialog
        dialogSelectFeature.modal("show");
    });

});
