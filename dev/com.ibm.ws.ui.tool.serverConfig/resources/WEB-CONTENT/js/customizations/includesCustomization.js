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

var includesCustomization = (function() {
    "use strict";

    var resolveDeferred = null;

    var reset = function() {
        if(resolveDeferred !== null && resolveDeferred !== undefined) {
            resolveDeferred.abort();
        }
    };

    var applyEditorFormCustomization = function(element) {

        // Container for link and messages
        var includeCustomizationContainer = $("<div id=\"includeCustomizationContainer\"></div>");

        // Link to open include file
        var link = $("<a href=\"#\" id=\"includeCustomizationLink\" class=\"hidden\"></a>");

        // Message (for loading indicator and status)
        var message = $("<div id=\"includeCustomizationMessage\" class=\"hidden\">" +
                        "    <img>" +
                        "    <span></span>" +
                        "</div>");

        // Add link and message to container
        includeCustomizationContainer.append(link);
        includeCustomizationContainer.append(message);


        // Associate element
        includeCustomizationContainer.data("element", element);

        // Add container to form
        $("label[for='attribute_location']").after(includeCustomizationContainer);

        // Resolve
        refresh();

    };

    var refresh = function() {

        var includeCustomizationLink = $("#includeCustomizationLink");

        // Obtain include element
        var includeCustomizationContainer = $("#includeCustomizationContainer");
        includeCustomizationContainer.data("refreshNeeded", false);
        var element = includeCustomizationContainer.data("element");

        // Obtain location attribute
        var location = element.getAttribute("location");

        // Check locationa attribute value
        if(location !== null && location !== undefined && location.length > 0) {

            // Disable attribute field
            var attributeControl = $("#attribute_location");
            attributeControl.attr("disabled", "disabled");

            // Show resolving message
            $("#includeCustomizationMessage>img").attr("src","img/config-tool-home-progress-bgwhite-D.gif");
            $("#includeCustomizationMessage>img").attr("alt","");
            $("#includeCustomizationMessage>span").empty();
            core.showControlById("includeCustomizationMessage", true);

            // Obtain context location from parent file
            var contextLocation = fileUtils.getPathFromFilePath(editor.getFileResolvedPath());

            // Attempt to resolve file
            resolveDeferred = fileUtils.resolveIncludeLocation(contextLocation, location).done(function(result) {

                // Apply variables to path
                var resolvedLocationWithVariables = fileUtils.applyVariablesToFilePath(result.fileName);

                // Hide message
                core.hideControlById("includeCustomizationMessage");

                // Setup and show hyperlink
                includeCustomizationLink.text(editorMessages.OPEN_FILE);
                includeCustomizationLink.attr("href", core.getFileURL(resolvedLocationWithVariables));
                includeCustomizationLink.data("filePath", resolvedLocationWithVariables);
                includeCustomizationLink.removeAttr("disabled");
                core.showControlById("includeCustomizationLink");
                attributeControl.removeAttr("disabled");

            }).fail(function() {

                // Attempt to resolve include file directory
                var locationPath = fileUtils.getPathFromFilePath(location);
                resolveDeferred = fileUtils.resolveIncludeLocation(contextLocation, locationPath).done(function(result) {

                    // File directory resolved and writable, provide option to create file
                    includeCustomizationLink.text(editorMessages.CREATE_FILE);
                    if(fileUtils.isPathWritable(result.fileName)) {
                        includeCustomizationLink.removeAttr("disabled");
                    } else {
                        includeCustomizationLink.attr("disabled", "disabled");
                    }

                    // Indicate file not found
                    $("#includeCustomizationMessage>img").attr("src", "img/warning-D.png");
                    $("#includeCustomizationMessage>span").text(editorMessages.FILE_NOT_FOUND);

                    var resolvedLocationWithVariables = fileUtils.applyVariablesToFilePath(result.fileName);
                    includeCustomizationLink.data("filePath", resolvedLocationWithVariables + "/" + fileUtils.getFileFromFilePath(location));

                }).fail(function() {

                    includeCustomizationLink.attr("disabled", "disabled");

                    // Include file directory cannot be resolved, indicate include as non accessible
                    $("#includeCustomizationMessage>img").attr("src","img/warning-D.png");
                    $("#includeCustomizationMessage>span").text(editorMessages.CANNOT_ACCESS_FILE);

                    includeCustomizationLink.text(editorMessages.OPEN_FILE);

                }).always(function() {
                    core.showControlById("includeCustomizationLink");
                    attributeControl.removeAttr("disabled");
                });

            });
        } else {

            // If no include location is provided, show disabled link
            includeCustomizationLink.text(editorMessages.OPEN_FILE);
            includeCustomizationLink.attr("disabled", "disabled");
            core.showControlById("includeCustomizationLink");

        }
    };


    var createFile = function(filePath) {
        var includeCustomizationLink = $("#includeCustomizationLink");

        // Update link and message
        includeCustomizationLink.attr("disabled", "disabled");
        $("#includeCustomizationMessage>img").attr("src","img/config-tool-home-progress-bgwhite-D.gif");
        $("#includeCustomizationMessage>img").attr("alt","");
        $("#includeCustomizationMessage>span").text(editorMessages.CREATING_FILE);

        // Disable attribute field
        var attributeControl = $("#attribute_location");
        attributeControl.attr("disabled", "disabled");

        // Create file
        return $.ajax({
            url: "/IBMJMXConnectorREST/file/" + encodeURIComponent(filePath),
            type: "POST",
            beforeSend: core.collectiveRoutingRequired()? core.applyJmxRoutingHeaders: null,
            data: "<server>\n</server>",
            success: function() {
                includeCustomizationLink.attr("href", core.getFileURL(filePath));
                $("#includeCustomizationMessage>img").attr("src","img/status-success-16-DT.png");
                $("#includeCustomizationMessage>span").text(editorMessages.SUCCESSFULLY_CREATED_FILE);
                includeCustomizationLink.text(editorMessages.OPEN_FILE);
                includeCustomizationLink.removeAttr("disabled");
            },
            error: function() {
                $("#includeCustomizationMessage>img").attr("src","img/warning-D.png");
                $("#includeCustomizationMessage>span").text(editorMessages.COULD_NOT_CREATE_FILE);
            },
            complete: function() {
                attributeControl.removeAttr("disabled");
            }
        });
    };


    $(document).ready(function() {

        $("#editorForm").on("click", "#includeCustomizationLink", function(event) {
            var currentTarget = $(event.currentTarget);
            if(currentTarget.text() === editorMessages.CREATE_FILE) {
                event.preventDefault();
                createFile(currentTarget.data("filePath"));
            } else if((event.which === 0 || event.which === 1) && !event.ctrlKey && !event.shiftKey) {
                event.preventDefault();
                if(editor.isDocumentDirty()) {
                    saveBeforeClosingDialog.setCallbackFunction(function(){
                        core.setFile(currentTarget.data("filePath"));
                    });
                    $("#dialogSaveBeforeClosing").modal("show");
                } else {
                    core.setFile(currentTarget.data("filePath"));
                }
            }
        });

        $("#editorForm").on("input", "#attribute_location", function(event) {
            var includeCustomizationContainer = $("#includeCustomizationContainer");
            if(includeCustomizationContainer.length === 1) {
                includeCustomizationContainer.data("refreshNeeded", true);
                $("#includeCustomizationLink").attr("disabled", "disabled");
                core.showControlById("includeCustomizationLink");
                core.hideControlById("includeCustomizationMessage");
            }
        });

        $("#editorForm").on("blur", "#attribute_location", function(event) {
            var includeCustomizationContainer = $("#includeCustomizationContainer");
            if(includeCustomizationContainer.length === 1 && includeCustomizationContainer.data("refreshNeeded")) {
                refresh();
            }
        });

        $("#editorForm").on("keydown", "#attribute_location", function(event) {
            if(event.keyCode === 13) {
                var includeCustomizationContainer = $("#includeCustomizationContainer");
                if(includeCustomizationContainer.length === 1 && includeCustomizationContainer.data("refreshNeeded")) {
                    refresh();
                }
            }
        });

        $("#editorForm").on("click", "#includeLocationRefresh", function(event) {
            event.preventDefault();
            refresh();
        });

    });

    return {
        reset: reset,
        applyEditorFormCustomization: applyEditorFormCustomization
    };

})();
