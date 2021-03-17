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

    $("#dialogEnumerationSelect").on("show.bs.modal", function(event) {

        var listContainer = $("#dialogEnumerationSelectListContainer");

        // Clear list
        listContainer.empty();

        // Obtain dialog
        var dialog = $(event.currentTarget);

        // Obtain attribute
        var attribute = dialog.data("attribute");

        // Obtain input control
        var inputControl = dialog.data("inputControl");

        // Store input control for value update
        $("#dialogEnumerationSelect").data("inputControl", inputControl);

        // Obtain attribute label (default to capitalized attribute name if no label is specified)
        var attributeLabel = schemaUtils.getLabel(attribute);
        if(attributeLabel === null || attributeLabel === undefined) {
            attributeLabel = stringUtils.capitalizeString(attribute.getAttribute("name"));
        }

        // Obtain possible values
        var possibleValues = schemaUtils.getPossibleValues(attribute);

        // Obtain default value
        var defaultValue = schemaUtils.getDefaultValue(attribute);

        // Update dialog title
        $("#dialogEnumerationSelectTitle").text(attributeLabel);

        // Iterate through possible values
        var valueSelected = false;
        possibleValues.forEach(function(possibleValue) {

            // Obtain value
            var value = possibleValue[0];

            // Obtain description
            var description = possibleValue[1];

            // Create list item
            var listItem = $("<div role=\"radio\" aria-checked=\"false\" class=\"enumerationOption noSelect\" data-value=\"" + value + "\"></div>");

            // Create suffix if value is default
            var defaultValueSuffix = "";
            if(value === defaultValue) {
                defaultValueSuffix = "&nbsp;&nbsp;" + editorMessages.DEFAULT_SUFFIX;
            }

            // Add value
            var listItemValue = $("<span class=\"enumerationOptionValue\">" + value + defaultValueSuffix + "</span>");
            listItem.append(listItemValue);

            // Mark selection if value is present
            if(value === inputControl.value) {
                listItem.addClass("active");
                listItem.attr("tabindex", 0);
                valueSelected = true;
                listItem.attr("aria-checked", true);
            }

            // Skip the description if it matches the value
            if(value !== description) {
                var listItemDescription = $("<span class=\"enumerationOptionDescription\">" + description + "</span>");
                listItem.append(listItemDescription);
            }

            // Add item to list
            listContainer.append(listItem);

        });

        // If none of the values is selected, disable "OK" button
        if(!valueSelected) {
            $("#dialogEnumerationSelectOKButton").attr("disabled", "disabled").attr("tabindex", -1);
            $("#dialogEnumerationSelectListContainer .enumerationOption:first").attr("tabindex", 0);
        } else {
            $("#dialogEnumerationSelectOKButton").removeAttr("disabled").removeAttr("tabindex");
        }

        // Reduce size of dialog for boolean values
        var dataType = schemaUtils.getDataType(attribute);
        if(dataType === "xsd:boolean" || dataType === "booleanType") {
            $("#dialogEnumerationSelect .modal-dialog").addClass("modal-boolean");
        } else {
            $("#dialogEnumerationSelect .modal-dialog").removeClass("modal-boolean");
        }

        // Center dialog
        core.centerDialog(this);
    });


    $("#dialogEnumerationSelectListContainer").on("mousedown", ".enumerationOption", function(event) {

        // Obtain selected item
        var selectedListItem = $(event.currentTarget);

        if("WebkitAppearance" in document.documentElement.style) {
            selectedListItem.css("outline", "none");
        }

        if(!selectedListItem.hasClass("active")) {
            $("#dialogEnumerationSelectListContainer .enumerationOption.active").removeAttr("tabindex");
            selectedListItem.attr("tabindex", "0");
            selectedListItem.focus();
        }

    });


    $("#dialogEnumerationSelectListContainer").on("click", ".enumerationOption", function(event) {
        event.preventDefault();

        var eventTarget = $(event.currentTarget);
        if(!eventTarget.hasClass("active")) {
            $("#dialogEnumerationSelectListContainer .enumerationOption").removeClass("active").attr("aria-checked", false);
            //work in progress have problems with how it works with and w/o JAWS
            eventTarget.addClass("active");
            eventTarget.attr("aria-checked", true);
        }

        // Enable "OK" button when a selection is made
        $("#dialogEnumerationSelectOKButton").removeAttr("disabled").removeAttr("tabindex");
    });


    $("#dialogEnumerationSelectListContainer").on("dblclick", function(event) {
        $("#dialogEnumerationSelectOKButton").trigger("click");
    });


    $("#dialogEnumerationSelectOKButton").on("click", function(event) {
        event.preventDefault();

        // Obtain selected value
        var value = $("#dialogEnumerationSelectListContainer .enumerationOption.active").data("value");

        // Obtain input control
        var inputControl = $("#dialogEnumerationSelect").data("inputControl");

        // Close dialog
        $("#dialogEnumerationSelect").modal("hide");

        // Update input control with new value
        if(inputControl.value !== value) {
            inputControl.value = value;

            // Trigger change
            $(inputControl).trigger("input");
        }
    });


    $("#dialogEnumerationSelect").on("keydown", function(event) {
        if(event.keyCode === 13) {
            var eventTarget = $(event.target);
            if(eventTarget.hasClass("enumerationOption") && eventTarget.hasClass("active")) {
                event.preventDefault();
                $("#dialogEnumerationSelectOKButton").trigger("click");
            }
        }
    });


    $("#dialogEnumerationSelect").on("keydown", function(event) {

        var selectedItem = $("#dialogEnumerationSelectListContainer .enumerationOption.active");
        if(!selectedItem.length) {
            selectedItem = $("#dialogEnumerationSelectListContainer .enumerationOption:focus");
        }

        var targetItem = null;

        if("WebkitAppearance" in document.documentElement.style) {
            selectedItem.css("outline", "");
        }

        switch(event.keyCode) {
        case 32:
            targetItem = selectedItem;
            break;
        case 36:
            targetItem = $("#dialogEnumerationSelectListContainer .enumerationOption:first");
            break;
        case 35:
            targetItem = $("#dialogEnumerationSelectListContainer .enumerationOption:last");
            break;
        case 40:
            if(selectedItem.length) {
                targetItem = selectedItem.next();
            } else {
                targetItem = $("#dialogEnumerationSelectListContainer .enumerationOption:first");
            }
            break;
        case 38:
            if(selectedItem.length) {
                targetItem = selectedItem.prev();
            } else {
                targetItem = $("#dialogEnumerationSelectListContainer .enumerationOption:last");
            }
            break;
        }

        if(targetItem !== null && targetItem !== undefined && targetItem.length) {
            event.preventDefault();
            $("#dialogEnumerationSelectListContainer .enumerationOption").removeClass("active").removeAttr("tabindex");
            targetItem.click().attr("tabindex", 0).focus();
        }
    });


    $("#editorForm").on("click", ".dialogEnumerationSelectButton", function(event) {
        event.preventDefault();

        // Obtain target button
        var targetButton = $(event.target);

        // Obtain dialog
        var dialogEnumerationSelect = $("#dialogEnumerationSelect");

        // Pass information
        dialogEnumerationSelect.data("attribute", targetButton.data("attribute"));
        dialogEnumerationSelect.data("inputControl", targetButton.data("inputControl"));
        dialogEnumerationSelect.data("selectButton", targetButton);

        // Open dialog
        dialogEnumerationSelect.modal("show");
    });


    $("#dialogEnumerationSelect").on("hidden.bs.modal", function() {
        var selectButton = $("#dialogEnumerationSelect").data("selectButton");

        if("WebkitAppearance" in document.documentElement.style) {
            selectButton.css("outline", "none");
        }

        selectButton.focus();
    });


    $("#dialogEnumerationSelect").on("shown.bs.modal", function(event) {
        $("#dialogEnumerationSelect .enumerationOption.active").focus();
    });


    $("#editorForm").on("keydown", ".dialogEnumerationSelectButton", function(event) {
        if(event.keyCode === 9) {
            if("WebkitAppearance" in document.documentElement.style) {
                $(event.currentTarget).css("outline", "");
            }
        }
    });

});
