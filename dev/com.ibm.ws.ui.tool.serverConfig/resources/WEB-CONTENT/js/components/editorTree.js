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

var editorTree = (function() {
    "use strict";

    var renderEditorTree = function(documentElement) {
        var editorTree = $("#editorTree");
        editorTree.empty();
        if(documentElement !== null && documentElement !== undefined) {

            // Add tree root
            renderElement(documentElement, editorTree);

            // Expand root children
            var treeRoot = $(".editorTreeNode:first", editorTree);
            toggleTreeNode(treeRoot);

            // Select tree root
            selectTreeNode(treeRoot);

        }
    };


    var renderElement = function(element, container) {
        // Obtain element label (defaults to capitalized element name if no label is specified)
        var elementLabel = element.nodeName;
        var elementSuffix = "";

        // Obtain element information
        var elementPath = xmlUtils.getElementPath(element);
        var schemaElement = schemaUtils.getElementFromPath(editor.getSchemaDocumentRootElement(), elementPath);
        if(schemaElement !== null && schemaElement !== undefined) {
            var elementDeclaration = schemaUtils.resolveElementDeclaration(schemaElement);
            if(elementDeclaration !== null && elementDeclaration !== undefined) {

                elementLabel = getElementLabel(element, schemaElement, elementDeclaration);

                // Obtain suffix (when applicable)
                elementSuffix = getElementSuffix(element, elementDeclaration);
                elementSuffix = apiMsgUtils.encodeData(elementSuffix);
            }
        }

        // Check if element has children
        var elementHasChildren = xmlUtils.elementHasChildren(element);

        // Determine if element is root
        var elementIsRoot = element.ownerDocument.documentElement === element;

        // Create tree node control
        var treeRoleAndExpandedState = "role=\"treeitem\"";
        if(elementIsRoot) {
            treeRoleAndExpandedState += " aria-expanded=\"true\"";
            treeRoleAndExpandedState += " aria-selected=\"true\"";
        } else if(elementHasChildren) {
            treeRoleAndExpandedState += " aria-expanded=\"false\"";
            treeRoleAndExpandedState += " aria-selected=\"false\"";
        }
        var treeNodeControl = $("<div class=\"editorTreeNode\" " + treeRoleAndExpandedState + " aria-label=\"" + elementLabel + " " + elementSuffix + "\"></div>");

        // Create expansion button control
        var expandButtonControl = $("<div class=\"editorTreeNodeExpandButton\"><span class=\"sr-only\">" + editorMessages.EXPAND_COLLAPSE + "</span></div>");
        treeNodeControl.append(expandButtonControl);
        if(elementIsRoot) {
            expandButtonControl.addClass("hidden");
        } else if(!elementHasChildren) {
            expandButtonControl.addClass("invisible");
        }

        // Create label control
        var labelControl = $("<div class=\"editorTreeNodeLabel\">" + elementLabel + "</div>");
        if(elementIsRoot) {
            labelControl.addClass("editorTreeNodeRoot");
            labelControl.attr("id", "editorTreeNodeRootLabel");
        }
        treeNodeControl.append(labelControl);

        // Create suffix control
        var suffixControl = $("<div class=\"editorTreeNodeSuffix\">" + elementSuffix + "</div>");

        // Handle bidi
        if(globalization.isBidiEnabled()) {
            if(!globalization.dataTypeRequiresSpecialHandling(elementSuffix)) {
                var dirValue = globalization.getBidiTextDirection();
                if(dirValue === "contextual") {
                    dirValue = globalization.obtainContextualDir(elementSuffix);
                }
                suffixControl.attr("dir", dirValue);
            }
        }

        treeNodeControl.append(suffixControl);

        // Create drag point
        if(!editor.isFileReadOnly()) {
            var dragPoint = $("<div class=\"editorTreeDragPoint\"></div>");
            treeNodeControl.append(dragPoint);
        }

        // Associate element with tree node
        treeNodeControl.data("element", element);

        // Add tree node to tree
        var treeNodeContainer = $("<div class=\"editorTreeNodeContainer\"></div>");
        treeNodeContainer.append(treeNodeControl);

        // Apply indentation
        var nestLevel = xmlUtils.getElementNestLevel(element);
        if(nestLevel > 2) {
            treeNodeControl.css("padding-left", (18 * (nestLevel - 1)) + "px");
        } else {
            treeNodeControl.addClass("topLevelTreeNode");
        }

        // Add tree node to container
        container.append(treeNodeContainer);
    };


    var getElementLabel = function(element, schemaElement, elementDeclaration) {
        var elementLabel = element.nodeName;
        if(settings.enhanceLabels) {
            elementLabel = schemaUtils.getLabel(schemaElement);
            if(elementLabel === null || elementLabel === undefined) {
                elementLabel = schemaUtils.getLabel(elementDeclaration);
            }
            if(elementLabel === null || elementLabel === undefined) {
                elementLabel = stringUtils.capitalizeString(element.nodeName);
            }
        }
        return elementLabel;
    };


    var getElementSuffix = function(element, elementDeclaration) {
        var suffix = "";
        if(schemaUtils.elementContainsText(elementDeclaration) && element.textContent !== null && element.textContent !== undefined) {
            suffix = element.textContent;
        } else if(element.getAttribute("id") !== null && element.getAttribute("id") !== undefined) {
            suffix = element.getAttribute("id");
        } else if(element.getAttribute("name") !== null && element.getAttribute("name") !== undefined) {
            suffix = element.getAttribute("name");
        } else if(element.getAttribute("location") !== null && element.getAttribute("location") !== undefined) {
            suffix = element.getAttribute("location");
        }
        return suffix;
    };


    var updateTreeNodeSuffix = function() {
        var element = $("#editorTree .active").data("element");
        var elementPath = xmlUtils.getElementPath(element);
        var schemaElement = schemaUtils.getElementFromPath(editor.getSchemaDocumentRootElement(), elementPath);
        if(schemaElement !== null && schemaElement !== undefined) {
            var elementDeclaration = schemaUtils.resolveElementDeclaration(schemaElement);
            if(elementDeclaration !== null && elementDeclaration !== undefined) {
                var suffix = getElementSuffix(element, elementDeclaration);
                var suffixControl = $("#editorTree .active .editorTreeNodeSuffix");
                suffixControl.text(suffix);

                // Handle bidi
                if(globalization.isBidiEnabled()) {
                    if(!globalization.dataTypeRequiresSpecialHandling(suffix)) {
                        var dirValue = globalization.getBidiTextDirection();
                        if(dirValue === "contextual") {
                            dirValue = globalization.obtainContextualDir(suffix);
                        }
                        suffixControl.attr("dir", dirValue);
                    }
                }
            }
        }
    };


    var toggleTreeNode = function(treeNode) {
        var treeNodeContainer = treeNode.parent();
        var treeNodeChildren = treeNodeContainer.children(".editorTreeNodeChildren");
        if(!treeNode.hasClass("expanded")) {
            treeNode.attr("aria-expanded", "true");
            if(treeNodeChildren.length) {
                // Children already loaded, show them
                treeNodeChildren.removeClass("hidden");
            } else {
                // Load children
                treeNodeChildren = $("<div class=\"editorTreeNodeChildren\" role=\"group\"></div>");
                var element = treeNode.data("element");
                $(element).children().each(function(index, childElement) {
                    renderElement(childElement, treeNodeChildren);
                });
                treeNodeContainer.append(treeNodeChildren);
            }
        } else {
            treeNode.attr("aria-expanded", "false");
            treeNodeChildren.addClass("hidden");
            var selectedTreeNode = $(".editorTreeNode.active", treeNodeChildren);
            if(selectedTreeNode.length) {
                if("WebkitAppearance" in document.documentElement.style) {
                    treeNode.css("outline", "none");
                }
                selectTreeNode(treeNode);
                treeNode.focus();
            }
        }
        treeNode.toggleClass("expanded");
    };


    var addTreeNode = function(element) {

        // Locate tree node associated with the parent element
        var parent = element.parentNode;
        var parentTreeNode = $("#editorTree .editorTreeNode").filter(function() {
            return $(this).data("element") === parent;
        });

        if(parentTreeNode.length) {

            var treeNodeContainer = parentTreeNode.parent();
            var treeNodeChildren = treeNodeContainer.children(".editorTreeNodeChildren");

            if(treeNodeChildren.length) {
                // Append new element
                renderElement(element, treeNodeChildren);
                treeNodeChildren.removeClass("hidden");
            } else {
                // Load children (which includes the new element)
                treeNodeChildren = $("<div class=\"editorTreeNodeChildren\"></div>");
                element = parentTreeNode.data("element");
                $(element).children().each(function(index, childElement) {
                    renderElement(childElement, treeNodeChildren);
                });
                treeNodeContainer.append(treeNodeChildren);
            }

            // Ensure the parent is expanded
            parentTreeNode.addClass("expanded");
            parentTreeNode.attr("aria-expanded", "true");

            // Ensure the expansion icon of the parent is visible
            $(".editorTreeNodeExpandButton", parentTreeNode).removeClass("invisible");

            // Select new tree node
            var addedTreeNode = $(".editorTreeNode:last", treeNodeChildren);
            selectTreeNode(addedTreeNode);

            // Ensure new tree node is visible
            var editorTree = $("#editorTree");
            var offset = addedTreeNode.offset().top - editorTree.scrollTop();
            if(offset > editorTree.innerHeight()){
                editorTree.animate({scrollTop: offset}, 300);
            }

        }
    };


    var removeTreeNode = function(element) {
        var treeNode = $("#editorTree .editorTreeNode").filter(function() {
            return $(this).data("element") === element;
        });

        if(treeNode.length) {
            // Obtain tree node container
            var treeNodeContainer = treeNode.parent();

            // Obtain parent tree node
            var parentTreeNode = $(".editorTreeNode:first", treeNodeContainer.parent().closest(".editorTreeNodeContainer"));

            // If no siblings are left remove expand button from parent
            if(treeNodeContainer.siblings().length === 0) {
                $(".editorTreeNodeExpandButton", parentTreeNode).addClass("invisible");
            }

            // Remove tree node
            treeNodeContainer.remove();

            // Change selection to parent
            selectTreeNode(parentTreeNode);

            // Ensure parent tree node is visible
            var editorTree = $("#editorTree");
            var offset = parentTreeNode.offset().top;
            if(offset < 0) {
                editorTree.animate({scrollTop: offset}, 300);
            }

        }
    };

    var selectTreeNode = function(treeNode) {
        $("#editorTree .editorTreeNode.active").removeClass("active").removeAttr("tabindex").attr("aria-selected",false);
        treeNode.addClass("active").attr("tabindex", 0).attr("aria-selected",true);
        editorForm.renderEditorForm(treeNode.data("element"));
        $("#editorFormContainer").scrollTop(0);
    };


    var updateTreeLabels = function() {
        $("#editorTree .editorTreeNode").each(function(index, object) {
            var element = $(object).data("element");
            var treeNodeLabel = $(".editorTreeNodeLabel", object);
            var elementPath = xmlUtils.getElementPath(element);
            var schemaElement = schemaUtils.getElementFromPath(editor.getSchemaDocumentRootElement(), elementPath);
            if(schemaElement !== null && schemaElement !== undefined) {
                var elementDeclaration = schemaUtils.resolveElementDeclaration(schemaElement);
                if(elementDeclaration !== null && elementDeclaration !== undefined) {
                    var elementLabel = getElementLabel(element, schemaElement, elementDeclaration);
                    treeNodeLabel.text(elementLabel);
                }
            }
        });
    };


    $(document).ready(function() {

        $("#editorTree").on("mousedown", ".editorTreeNode", function(event) {
            var target = $(event.target);
            if(target.hasClass("editorTreeNodeExpandButton")) {
                event.preventDefault();
                toggleTreeNode(target.parent());
            } else {
                var treeNode = $(event.currentTarget);
                if("WebkitAppearance" in document.documentElement.style) {
                    treeNode.css("outline", "none");
                }
                if(!treeNode.hasClass("active")) {
                    $("#editorTree .editorTreeNode.active").removeAttr("tabindex");
                    treeNode.attr("tabindex", "0");
                    treeNode.focus();
                }
            }
        });

        // Prevent default hyperlink action
        $("#editorTree").on("click", ".editorTreeNode", function(event) {
            if(!$(event.target).hasClass("editorTreeNodeExpandButton")) {
                var treeNode = $(event.currentTarget);
                if(!treeNode.hasClass("active")) {
                    selectTreeNode(treeNode);
                }
            }
        });

        // Handle tooltips for clipped tree node suffixes
        $("#editorTree").on("mouseenter", ".editorTreeNodeLabel, .editorTreeNodeSuffix", function(event) {
            var suffix = event.currentTarget;
            if(suffix.offsetWidth < suffix.scrollWidth) {
                suffix.title = suffix.innerText;
            } else {
                suffix.title = "";
            }
        });


        // Handle keyboard navigation
        $("#editorTree").on("keydown", ".editorTreeNode", function(event) {

            // Only show outline in webkit browsers when navigating with keyboard
            if("WebkitAppearance" in document.documentElement.style) {
                $(event.target).css("outline", "");
            }

            if(!event.ctrlKey) {
                var selectedItem = $("#editorTree .editorTreeNode.active");
                var targetItem = null;
                var currentAncestor;

                switch(event.keyCode) {
                case 36:
                    targetItem = $("#editorTree .editorTreeNode:first");
                    break;
                case 35:
                    targetItem = $("#editorTree .editorTreeNode:last");
                    break;
                case 39:
                    if(xmlUtils.elementHasChildren(selectedItem.data("element"))) {
                        if(!selectedItem.hasClass("expanded")) {
                            toggleTreeNode(selectedItem);
                        }
                    }
                    break;

                case 40:
                    if(selectedItem.length) {
                        currentAncestor = selectedItem.parent();
                        targetItem = $(".editorTreeNodeChildren:first", currentAncestor).find(".editorTreeNode:visible:first");
                        while(!targetItem.length && currentAncestor.closest("#editorTree").length) {
                            targetItem = currentAncestor.next().find(".editorTreeNode:first");
                            currentAncestor = currentAncestor.parent().parent();
                        }
                    } else {
                        targetItem = $("#editorTree .editorTreeNode:first");
                    }
                    break;
                case 37:
                    if(selectedItem.hasClass("expanded") && selectedItem.parent().parent().attr("id") !== "editorTree") {
                        toggleTreeNode(selectedItem);
                    } else {
                        targetItem = $(".editorTreeNode:first", selectedItem.parent().parent().parent());
                    }
                    break;
                case 38:
                    if(selectedItem.length) {
                        currentAncestor = selectedItem.parent();
                        targetItem = currentAncestor.prev().find(".editorTreeNode:visible:last");
                        if(!targetItem.length) {
                            targetItem = $(".editorTreeNode:visible:first", currentAncestor.parent().parent());
                        }
                    } else {
                        targetItem = $("#editorTree .editorTreeNode:last");
                    }
                    break;
                }

                if(targetItem !== null && targetItem !== undefined && targetItem.length) {
                    event.preventDefault();
                    selectTreeNode(targetItem);
                    targetItem.focus();
                }
            }
        });

    });


    return {
        renderEditorTree: renderEditorTree,
        addTreeNode: addTreeNode,
        removeTreeNode: removeTreeNode,
        updateTreeNodeSuffix: updateTreeNodeSuffix,
        updateTreeLabels: updateTreeLabels
    };

})();
