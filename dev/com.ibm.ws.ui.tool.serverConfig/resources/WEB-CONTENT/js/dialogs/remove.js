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

    var skipRemoveButtonFocusRestore = false;

    $("#dialogRemoveElement").on("show.bs.modal", function(event) {
        // Center dialog
        core.centerDialog(this);

        skipRemoveButtonFocusRestore = false;
    });


    $("#dialogRemoveElement").on("keydown", function(event) {
        if(event.keyCode === 13) {
            event.preventDefault();
            $("#dialogRemoveElementOKButton").trigger("click");
        }
    });


    $("#dialogRemoveElementOKButton").on("click", function(event) {
        // Obtain selected element
        var selectedElement = editor.getSelectedElement();

        // Obtain selected element parent
        var parentNode = selectedElement.parentNode;

        // Remove preceding text
        var sibling = selectedElement.previousSibling;
        while(sibling !== null && sibling !== undefined && sibling.nodeType === 3) {
            sibling = sibling.previousSibling;
            parentNode.removeChild(selectedElement.previousSibling);
        }

        // Remove selected element
        parentNode.removeChild(selectedElement);

        skipRemoveButtonFocusRestore = true;

        // Close dialog
        event.preventDefault();
        $("#dialogRemoveElement").modal("hide");

        // Update UI
        editorTree.removeTreeNode(selectedElement);

        // Mark document as dirty
        editor.markDocumentAsDirty();
    });


    $("#dialogRemoveElement").on("hidden.bs.modal", function() {
        if(!skipRemoveButtonFocusRestore) {
            var removeButton = $("#removeButton");

            if("WebkitAppearance" in document.documentElement.style) {
                removeButton.css("outline", "none");
            }

            removeButton.focus();
        }
    });

    $("#editorForm").on("keydown", "#removeButton", function(event) {
        if(event.keyCode === 9) {
            if("WebkitAppearance" in document.documentElement.style) {
                $(event.currentTarget).css("outline", "");
            }
        }
    });

});
