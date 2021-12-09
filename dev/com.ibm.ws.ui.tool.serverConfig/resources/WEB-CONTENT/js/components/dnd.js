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
    
    var dragTarget = null;
    var mouseDown = false;
    var timeStamp = null;
    var dragInProgress = false;
    var unapplicableTreeNodes = null;
    var dragPointMove = false;
    
    
    $("#editorTree").on("mousedown touchstart", ".editorTreeNode .editorTreeDragPoint", function(event) {
        // Obtain grabbed element
        dragTarget = $(event.currentTarget).parent();
        
        // Update mouseDown status
        mouseDown = true;
        
        // Instantly start drag-and-drop
        initiateDrag();
        dragPointMove = true;
        dragInProgress = true;
        
    });

    
    $("#editorTree").on("mousedown touchstart", ".editorTreeNode", function(event) {
        
        if(!editor.isFileReadOnly() && !$(event.target).hasClass("editorTreeNodeExpandButton")) {            
            event.preventDefault();
            
            // Obtain grabbed element
            dragTarget = $(event.currentTarget);
            
            // Handle focus outline
            if("WebkitAppearance" in document.documentElement.style) {
                $("#editorTree").addClass("noOutline");
            }
            
            // Set focus
            dragTarget.focus();
            
            // Update mouseDown status
            mouseDown = true;
            
            // Set time stamp
            timeStamp = $.now();
        }
    });
    
    
    $("#editorTree").on("keydown", function() {
        
        // Handle focus outline
        if("WebkitAppearance" in document.documentElement.style) {
            $("#editorTree").removeClass("noOutline");
        }
        
    });
    
    
    $(window).on("mousemove", function(event) {
        if(dragInProgress) {
            // Obtain mouse coordinates
            var x = event.clientX;
            var y = event.clientY;
            
            handleMove(x, y);
        } else if(mouseDown && $.now() - timeStamp > 100) {
            initiateDrag();
            dragInProgress = true;
        }
    }); 
    

    $(window).on("mouseup touchend", function() {
        if(dragInProgress) {
            dragInProgress = false;
            finalizeDrag();
        }
        mouseDown = false;
    });
    
    
    var initiateDrag = function() {
        
        // Prevent hover effect on tree
        $("#editorTree").addClass("noHover");
        
        var parent = dragTarget.data("element").parentNode;
        
        dragTarget.parent().addClass("dragTarget");
        
        unapplicableTreeNodes = $("#editorTree .editorTreeNode").filter(function() {
            var current = $(this);
            return current.closest(".dragTarget").length === 0 && current.data("element").parentNode !== parent;
        });
        
        unapplicableTreeNodes.addClass("invalidDragTarget");
    };
    

    var handleMove = function(x, y) {
        // Obtain element in touch coordinates
        var target = $(document.elementFromPoint(x, y)).closest(".editorTreeNode");
        if(target.length && dragTarget.get(0) !== target.get(0)) {
            
            var dragTargetElement = dragTarget.data("element");
            var targetElement = target.data("element");
            
            if(dragTargetElement.parentNode === targetElement.parentNode) {
                handleSwap(dragTarget.get(0).parentNode, target.get(0).parentNode);
            }
        }
    };
    
    
    var handleSwap = function(dragTarget, target) {
        
        // Obtain location of elements
        var dragTargetIndex = $(dragTarget).index();
        var targetIndex = $(target).index();
        
        // Perform swap according to location of elements
        if(dragTargetIndex < targetIndex) {
            dragTarget.parentNode.insertBefore(dragTarget, target.nextSibling);
        } else if(dragTargetIndex > targetIndex) {
            dragTarget.parentNode.insertBefore(dragTarget, target);
        }
    };

    
    var finalizeDrag = function() {
        
        // Restore hover effect on tree
        $("#editorTree").removeClass("noHover");
        
        // Perform swap on document
        performSwapOnDocument(dragTarget.data("element"), dragTarget.parent().index());
        
        dragTarget.parent().removeClass("dragTarget");
        
        unapplicableTreeNodes.removeClass("invalidDragTarget");
        
        if(dragPointMove) {
            dragPointMove = false;
        } else {            
            dragTarget.focus();
        }
    };
    
    
    var performSwapOnDocument = function(element, index) {
        var currentIndex = xmlUtils.getElementIndex(element);
        if(currentIndex !== index) {

            if(element.previousSibling.nodeType === 3) {
                var precedingText = element.previousSibling.nodeValue;
                var newLineIndex = precedingText.lastIndexOf("\n");
                element.previousSibling.nodeValue = precedingText.substring(newLineIndex === -1? 0 : newLineIndex + 1);
            }
            
            // Calculate offset depending on element location
            var offset = 1;
            if(currentIndex > index) {
                offset = 0;
            }
            
            // Obtain target location using offset
            var target = xmlUtils.getChildElementAtIndex(element.parentNode, index + offset);
            if(target === null || target === undefined && element.parentNode.lastChild.nodeType !== 1) {
                target = element.parentNode.lastChild;
            }
            
            // Move element
            element.parentNode.insertBefore(element, target);
            
            // Calculate indentation
            var indentation = "";
            for(var i = 0; i < xmlUtils.getElementNestLevel(element.parentNode); i++) {
                indentation = indentation + settings.xml_indentation;
            }
            
            // Apply formatting using indentation
            element.parentNode.insertBefore(element.ownerDocument.createTextNode("\n" + indentation), offset === 1? element : element.nextSibling);
        
            // Mark document as dirty
            editor.markDocumentAsDirty();
        }
    };
    
    
    $("#editorTree").on("keydown", function(event) {
        // Check that the control key is pressed
        if(event.ctrlKey) {
            var target = $("#editorTree .editorTreeNode.active");
            if(event.keyCode === 38) {
                // Move up (if possible)
                var previous = target.parent().prev().children(".editorTreeNode").first();
                if(previous.length) {
                    // Swap elements
                    handleSwap(target.get(0).parentNode, previous.get(0).parentNode);
                    
                    // Perform swap on document
                    performSwapOnDocument(target.data("element"), target.parent().index());
                    
                    // Focus element
                    target.focus();
                }
            } else if(event.keyCode === 40) {
                // Move down (if possible)
                var next = target.parent().next().children(".editorTreeNode").first();
                if(next.length) {
                    // Swap elements
                    handleSwap(target.get(0).parentNode, next.get(0).parentNode);
                    
                    // Perform swap on document
                    performSwapOnDocument(target.data("element"), target.parent().index());
                    
                    // Focus element
                    target.focus();
                }
            }
        }
    });
    
    
    // Cursors for dnd
    var a = document.createElement("a");
    var grabCursor = "grab";
    var grabbingCursor = "grabbing";
    a.style.cursor = grabCursor;
    if(a.style.cursor !== "grab") {
        a.style.cursor = "-webkit-grab";
        if(a.style.cursor === "-webkit-grab") {
            grabCursor = "-webkit-grab";
            grabbingCursor = "-webkit-grabbing";
        } else {
            grabCursor = "move";
            grabbingCursor = "move";
        }
    }
    $(    "<style type='text/css'>" + 
        "    #editorTree .editorTreeDragPoint {" +
        "        cursor: " + grabCursor + ";" +
        "    }" +
        "    #editorTree .dragTarget .editorTreeNode, #editorTree .dragTarget .editorTreeNode .editorTreeDragPoint {" +
        "        cursor: " + grabbingCursor + ";" +
        "    }" +
        "</style>").appendTo("head");

    $(document).on("dragstart", function() {
         return false;
    });
    
});
