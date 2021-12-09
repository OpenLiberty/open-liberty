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

    var skipAddChildButtonFocusRestore = false;

    $("#dialogAddChildElement").on("show.bs.modal", function(event) {

        // Obtain selected element information
        var selectedElement = editor.getSelectedElement();
        var elementPath = xmlUtils.getElementPath(selectedElement);
        var schemaElement = schemaUtils.getElementFromPath(editor.getSchemaDocumentRootElement(), elementPath);
        var elementDeclaration = schemaUtils.resolveElementDeclaration(schemaElement);

        // Reset button focus restore
        skipAddChildButtonFocusRestore = false;

        // Obtain child elements
        var childElements = schemaUtils.getChildElements(elementDeclaration);

        // Process child elements
        var processedChildElements = [];
        childElements.forEach(function(childElement) {

            // Obtain child element label (defaults to capitalized element name if no label is specified)
            var childElementLabel = null;
            if(settings.enhanceLabels) {
                childElementLabel = schemaUtils.getLabel(childElement);
                if(childElementLabel === null || childElementLabel === undefined) {

                    // Obtain child element information
                    var childElementDeclaration = schemaUtils.resolveElementDeclaration(childElement);

                    if(childElementDeclaration !== null && childElementDeclaration !== undefined) {
                        childElementLabel = schemaUtils.getLabel(childElementDeclaration);
                    }
                }
                if(childElementLabel === null ||childElementLabel === undefined) {
                    childElementLabel = stringUtils.capitalizeString(childElement.getAttribute("name"));
                }
            } else {
                childElementLabel = childElement.getAttribute("name");
            }

            // Create processed child element
            var processedChildElement = {
                    name: childElement.getAttribute("name"),
                    label: childElementLabel
            };

            // Add processed child element to list
            processedChildElements.push(processedChildElement);
        });

        // Sort elements by label or name
        if(settings.enhanceLabels) {
            processedChildElements.sort(function(a, b) {
                var valueA = a.label.toLowerCase();
                var valueB = b.label.toLowerCase();
                return (valueA > valueB)? 1 : (valueA < valueB)? -1 : 0;
            });
        } else {
            processedChildElements.sort(function(a, b) {
                var valueA = a.name.toLowerCase();
                var valueB = b.name.toLowerCase();
                return (valueA > valueB)? 1 : (valueA < valueB)? -1 : 0;
            });
        }

        // Store processed child elements
        $("#dialogAddChildElement").data("processedChildElements", processedChildElements);

        // Store filtered processed child elements (initialized with processedChildElements)
        $("#dialogAddChildElement").data("filteredProcessedChildElements", processedChildElements.slice());

        // Clear search input
        var dialogAddChildElementSearch = $("#dialogAddChildElementSearch");
        dialogAddChildElementSearch.val("");

        // Update child elements List
        updateAddChildElementDialogList();

        // Disable OK button
        $("#dialogAddChildElementOKButton").attr("disabled", "disabled").attr("tabindex", -1);

        // Handle bidi
        if(globalization.isBidiEnabled()) {
            var dirValue = globalization.getBidiTextDirection();
            if(dirValue !== "contextual") {
                dialogAddChildElementSearch.attr("dir", dirValue);
            }
        }

        // Center dialog
        core.centerDialog(this);
    });


    $("#dialogAddChildElementSearch").on("input", function(event) {

        // Handle bidi
        if(globalization.isBidiEnabled() && globalization.getBidiTextDirection() === "contextual") {
            $(this).attr("dir", globalization.obtainContextualDir(event.currentTarget.value));
        }

        var filter = event.currentTarget.value.toLowerCase();
        var processedChildElements = $("#dialogAddChildElement").data("processedChildElements");
        var filteredProcessedChildElements = [];
        for(var i = 0; i < processedChildElements.length; i++) {
            if(processedChildElements[i].label.toLowerCase().indexOf(filter) !== -1) {
                filteredProcessedChildElements.push(processedChildElements[i]);
            }
        }
        // Store filtered processed child elements (initialized with processedChildElements)
        $("#dialogAddChildElement").data("filteredProcessedChildElements", filteredProcessedChildElements);

        // Update dialog
        updateAddChildElementDialogList();

        if(filteredProcessedChildElements.length > 0) {
            // Auto select first filtered entry
            if(filter.length > 0) {
                $("#dialogAddChildElementListContainer .dialogAddChildElementListItem:first").attr("tabindex", 0).trigger("click");
            }
        } else {
            // If the filtered list is empty, show "no matches found" message and disable "ok" button
            $("#dialogAddChildElementDescription").val(editorMessages.NO_MATCHES_FOUND);
            $("#dialogAddChildElementOKButton").attr("disabled", "disabled").attr("tabindex", -1);
        }
    });


    $("#dialogAddChildElement").on("keydown", function(event) {
        if(event.keyCode === 13) {
            var eventTarget = $(event.target);
            if(eventTarget.hasClass("dialogAddChildElementListItem") && eventTarget.hasClass("active")) {
                event.preventDefault();
                $("#dialogAddChildElementOKButton").trigger("click");
            }
        }
    });


    $("#dialogAddChildElementSearch").on("keydown", function(event) {
        if(event.keyCode === 40) {
            event.preventDefault();
            var selectedOption = $("#dialogAddChildElementListContainer .dialogAddChildElementListItem.active");
            if(selectedOption.length) {
                selectedOption.focus();
            } else {
                var firstOption = $("#dialogAddChildElementListContainer .dialogAddChildElementListItem:first");
                firstOption.click().attr("tabindex", "0").focus();
            }
        } else if(event.keyCode === 13 && $("#dialogAddChildElementListContainer .active").length === 1) {
            event.preventDefault();
            $("#dialogAddChildElementOKButton").trigger("click");
        }
    });


    $("#dialogAddChildElementListContainer").on("keydown", function(event) {

        var selectedItem = $("#dialogAddChildElementListContainer .dialogAddChildElementListItem.active");
        if(!selectedItem.length) {
            selectedItem = $("#dialogAddChildElementListContainer .dialogAddChildElementListItem:focus");
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
                targetItem = $("#dialogAddChildElementListContainer .dialogAddChildElementListItem:first");
                break;
            case 35:
                targetItem = $("#dialogAddChildElementListContainer .dialogAddChildElementListItem:last");
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
                $("#dialogAddChildElementListContainer .dialogAddChildElementListItem").removeClass("active").removeAttr("tabindex").attr("aria-checked", false);
                targetItem.click().attr("tabindex", 0).focus();
                targetItem.attr("aria-checked", true);
            }
        }

    });


    function updateAddChildElementDialogList() {
        // Clear dialog body
        $("#dialogAddChildElementListContainer").empty();

        // Obtain stored processed child elements
        var filteredProcessedChildElements = $("#dialogAddChildElement").data("filteredProcessedChildElements");

        // Update List
        for(var i = 0; i < filteredProcessedChildElements.length; i++) {
            $("#dialogAddChildElementListContainer").append("<div role=\"radio\" aria-checked=\"false\" data-element-name=\"" + filteredProcessedChildElements[i].name + "\" class=\"dialogAddChildElementListItem noSelect\">" + filteredProcessedChildElements[i].label + "</div>");
        }

        // Clear description
        $("#dialogAddChildElementDescription").val(editorMessages.SELECT_ELEMENT_TO_VIEW_DESCRIPTION);
    }


    $("#dialogAddChildElementListContainer").on("dblclick", function(event) {
        $("#dialogAddChildElementOKButton").trigger("click");
    });

    $("#dialogAddChildElementListContainer").on("mousedown", ".dialogAddChildElementListItem", function(event) {

        // Obtain selected item
        var selectedListItem = $(event.currentTarget);

        if("WebkitAppearance" in document.documentElement.style) {
            selectedListItem.css("outline", "none");
        }

        if(!selectedListItem.hasClass("active")) {
            $("#dialogAddChildElementListContainer .dialogAddChildElementListItem").removeAttr("tabindex");
            selectedListItem.attr("tabindex", "0");
            selectedListItem.focus();
        }


    });

    $("#dialogAddChildElementListContainer").on("click", ".dialogAddChildElementListItem", function(event) {
        event.preventDefault();

        // Obtain selected item
        var selectedListItem = $(event.currentTarget);

        if(!selectedListItem.hasClass("active")) {

            // Clear current selection
            $("#dialogAddChildElementListContainer .active").removeClass("active");

            // Mark selected item
            selectedListItem.addClass("active");
            selectedListItem.attr("aria-checked",true);

        }

        // Obtain element name
        var elementName = selectedListItem.data("element-name");

        // Obtain selected element
        var selectedElement = editor.getSelectedElement();
        var elementPath = xmlUtils.getElementPath(selectedElement);
        elementPath.push(elementName);

        // Update description
        var schemaElement = schemaUtils.getElementFromPath(editor.getSchemaDocumentRootElement(), elementPath);
        var description = schemaUtils.getDocumentation(schemaElement);
        if(description === null || description === undefined) {
            var elementDeclaration = schemaUtils.resolveElementDeclaration(schemaElement);
            if(elementDeclaration !== null && elementDeclaration !== undefined) {
                description = schemaUtils.getDocumentation(elementDeclaration);
            }
        }
        if(description !== null && description !== undefined) {
            $("#dialogAddChildElementDescription").val(description);
        } else {
            $("#dialogAddChildElementDescription").val("");
        }

        // Enable "OK" button when a selection is made
        $("#dialogAddChildElementOKButton").removeAttr("disabled").removeAttr("tabindex");
    });


    $("#dialogAddChildElementOKButton").on("click", function(event) {

        if ($(this).attr("disabled") !== "disabled") {
            // Skip add child button focus restore
            skipAddChildButtonFocusRestore = true;

            // Obtain selected value
            var value = $("#dialogAddChildElementListContainer .active").data("element-name");

            // Obtain selected element
            var selectedElement = editor.getSelectedElement();

            // Create new element
            var newElement = selectedElement.ownerDocument.createElement(value);

            // Remove text towards the end of the parent
            var child = selectedElement.lastChild;
            while(child !== null && child !== undefined && child.nodeType === 3) {
                child = child.previousSibling;
                selectedElement.removeChild(selectedElement.lastChild);
            }

            // Append line return and indentation
            selectedElement.appendChild(selectedElement.ownerDocument.createTextNode("\n"));
            var indentation = "";
            for(var i = 0; i < xmlUtils.getElementNestLevel(selectedElement); i++) {
                indentation = indentation + settings.xml_indentation;
            }
            selectedElement.appendChild(selectedElement.ownerDocument.createTextNode(indentation));

            // Attach new element as child of selected element
            selectedElement.appendChild(newElement);

            // Append line return after new element
            selectedElement.appendChild(selectedElement.ownerDocument.createTextNode("\n"));

            // Add indentation to parent closing tag
            var endTagIndentation = indentation.substring(0, indentation.length / 2);
            selectedElement.appendChild(selectedElement.ownerDocument.createTextNode(endTagIndentation));

            // Close dialog
            event.preventDefault();
            $("#dialogAddChildElement").modal("hide");

            // Update editor UI
            editorTree.addTreeNode(newElement);

            // Mark document as dirty
            editor.markDocumentAsDirty();

            // Focus first control in form panel
            var firstControl = $("#editorForm input:first");
            if (firstControl.length) {
                firstControl.focus();
            } else {
                var addChildButton = $("#addChildButton");
                if (addChildButton.attr("disabled") !== "disabled") {

                    if("WebkitAppearance" in document.documentElement.style) {
                        addChildButton.css("outline", "none");
                    }

                    addChildButton.focus();
                }
            }
        } else {
            // disable button on onClick should do nothing
            event.preventDefault();
        }
    });


    $("#dialogAddChildElement").on("shown.bs.modal", function(event) {
        $("#dialogAddChildElementListContainer").scrollTop(0);
        var options = $("#dialogAddChildElement").data("processedChildElements").length;

        if(options > 0) {
            $("#dialogAddChildElementListContainer .dialogAddChildElementListItem:first").attr("tabindex", 0);
        }

        if(options === 1) {
            var firstOption = $("#dialogAddChildElementListContainer .dialogAddChildElementListItem");

            if("WebkitAppearance" in document.documentElement.style) {
                firstOption.css("outline", "none");
            }

            firstOption.attr("tabindex", 0).click().focus();
            firstOption.attr("aria-checked",true);
        } else {
            $("#dialogAddChildElementSearch").focus();
        }
    });


    $("#dialogAddChildElement").on("hidden.bs.modal", function() {
        if(!skipAddChildButtonFocusRestore) {
            var addChildButton = $("#addChildButton");

            if("WebkitAppearance" in document.documentElement.style) {
                addChildButton.css("outline", "none");
            }

            addChildButton.focus();
        }
    });

    $("#editorForm").on("keydown", "#addChildButton", function(event) {
        if(event.keyCode === 9) {
            if("WebkitAppearance" in document.documentElement.style) {
                $(event.currentTarget).css("outline", "");
            }
        }
    });

});
