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

var editor = (function() {
    "use strict";

    // File to edit
    var filePath = null;
    var fileResolvedPath = null;
    var fileContent = null;
    var fileDocumentContent = null;
    var fileReadOnly = false;
    var documentDirty = false;

    // Configuration schema
    var schemaDocumentContent = null;
    var schemaDocumentRootElement = null;

    // Feature list
    var featureList = null;

    // Validator list
    var validatorList = null;

    // Dirty flags
    var editorDesignViewDirty = false;
    var editorSourceViewDirty = false;


    var openFileForEditing = function(file, schemaTextContent, featureListContent, validatorListContent) {

        // Set file for edit
        filePath = file.path;
        fileResolvedPath = file.resolvedPath;
        fileContent = file.content;
        fileReadOnly = file.isReadOnly || !window.globalIsAdmin;
        documentDirty = false;

        featureList = featureListContent;
        validatorList = validatorListContent;

        // Set schema for edit (when available)
        if(schemaTextContent !== null && schemaTextContent !== undefined) {
            schemaDocumentContent = parse(schemaTextContent);
            if(schemaDocumentContent !== null && schemaDocumentContent !== undefined) {
                schemaDocumentRootElement = schemaUtils.getRootElementByName(schemaDocumentContent, "server");
            } else {
                schemaDocumentRootElement = null;
            }
        } else {
            schemaDocumentContent = null;
            schemaDocumentRootElement = null;
        }

        // Initialize design and source views
        parseConfigurationFile(file.content);

        // Disable save button
        $("#navbarEditorButtonsSave").attr("disabled", "disabled").attr("tabindex", -1).attr("aria-disabled", true);

        // Wait for Orion editor to finish loading
        source.initializeSource(fileContent, fileReadOnly).done(function() {

            // Update show line number preference
            source.orionEditor.editor.setLineNumberRulerVisible(settings.showLineNumbers);

            if(fileReadOnly) {
                core.showControlById("navbarEditorReadOnlyMessage");
                core.hideControlById("navbarEditorButtonsSave");
            } else {
                core.hideControlById("navbarEditorReadOnlyMessage");
                core.showControlById("navbarEditorButtonsSave");
            }

            // Switch to design or source view depending on preference
            if(settings.defaultToDesignView) {
                switchToDesignView();
            } else {
                switchToSourceView();
            }

        });
    };


    var parseConfigurationFile = function(textContent) {
        fileDocumentContent = parse(textContent);
        if(fileDocumentContent !== null && fileDocumentContent !== undefined) {
            core.hideControlById("editorDesignViewErrorMessage");
            editorTree.renderEditorTree(fileDocumentContent.documentElement);
            core.showControlById("editorDesignContent");
        } else {
            core.hideControlById("editorDesignContent");
            core.showControlById("editorDesignViewErrorMessage");
        }
    };


    var serializeConfigurationFile = function() {
        var serializer = new XMLSerializer();
        var xmlDocumentAsText = serializer.serializeToString(fileDocumentContent);

        // Ensure the XML declaration hasn't been dropped
        var xmlDeclaration = xmlUtils.getDocumentDeclaration(fileContent);
        if(xmlDeclaration !== null && xmlDeclaration !== undefined) {
            if(!(xmlDocumentAsText.length > xmlDeclaration.length && xmlDocumentAsText.substring(0, xmlDeclaration.length) === xmlDeclaration)) {
                xmlDocumentAsText = xmlDeclaration + "\n" + xmlDocumentAsText;
            }
        }
        return xmlDocumentAsText;
    };


    var parse = function(textContent) {
        try {
            return $.parseXML(textContent);
        } catch(error) {
            return null;
        }
    };


    var markDocumentAsDirty = function() {
        if($("#editorNavigationDesignLink").hasClass("active")) {
            editorDesignViewDirty = true;
        } else {
            editorSourceViewDirty = true;
        }
        documentDirty = true;
        $("#navbarEditorButtonsSave").removeAttr("disabled").removeAttr("aria-disabled").attr("tabindex", 0);
    };


    var switchToDesignView = function() {

        if(editorSourceViewDirty) {
            parseConfigurationFile(source.orionEditor.editor.getText());
            source.orionEditor.editor.setDirty(false);
            editorSourceViewDirty = false;
        }

        // Update navigation links state
        $("#editorNavigationDesignLink").addClass("active");
        $("#editorNavigationSourceLink").removeClass("active");
        if(!fileReadOnly) {
            core.hideControlById("contentAssistHint");
        }

        // Deactivate content assist
        if(source.orionEditor.editor.getContentAssist().isActive()) {
            source.orionEditor.editor.getContentAssist().deactivate();
        }

        // Hide source view
        core.hideControlById("editorSourceView");

        // Show design view
        core.showControlById("editorDesignView");

        // Persist preference
        settings.setDefaultToDesignView(true);

    };


    var switchToSourceView = function() {
        var xmlDocumentAsText = null;

        if(editorDesignViewDirty) {
            // If working with a valid XML, serialize it for source editing
            if(fileDocumentContent !== null && fileDocumentContent !== undefined) {
                xmlDocumentAsText = serializeConfigurationFile();
            }
            editorDesignViewDirty = false;
        }

        if(xmlDocumentAsText !== null && xmlDocumentAsText !== undefined) {
            source.orionEditor.updateText(xmlDocumentAsText);
        }

        // Update navigation links state
        $("#editorNavigationDesignLink").removeClass("active");
        $("#editorNavigationSourceLink").addClass("active");
        if(!fileReadOnly) {
            core.showControlById("contentAssistHint");
        } else {
            core.hideControlById("contentAssistHint");
        }

        // Hide design view
        core.hideControlById("editorDesignView");

        // Show source view
        core.showControlById("editorSourceView");

        // Update sizing on source editor
        source.orionEditor.editor.getTextView().setCaretOffset(source.orionEditor.editor.getText().length);
        source.orionEditor.editor.getTextView().setCaretOffset(0);
        source.orionEditor.editor.getTextView().setSelection(0,0);
        source.orionEditor.editor.resize();

        // Persist preference
        settings.setDefaultToDesignView(false);

    };


    var save = function() {

        // Obtain content to save
        var contentToSave = null;
        if($("#editorDesignView").is(":visible")) {
            contentToSave = serializeConfigurationFile();
        } else if($("#editorSourceView").is(":visible")) {
            contentToSave = source.orionEditor.editor.getText();
        }
        // if bidi, make sure there aren't control characters
        if(globalization.isBidiEnabled()) {
            contentToSave = globalization.stripBidiSpecialCharacters(contentToSave);
        }

        return $.ajax({
            url: "/IBMJMXConnectorREST/file/" + encodeURIComponent(filePath),
            type: "POST",
            data: contentToSave,
            beforeSend: core.applyJmxRoutingHeaders,
            success: function() {
                source.orionEditor.editor.setDirty(false);
                fileContent = contentToSave;
            }
        });

    };


    var fileChangedDuringEditing = function() {
        var deferred = new $.Deferred();

        fileUtils.retrieveFileContent(filePath).done(function(serverContent) {
            if(serverContent !== fileContent) {
                deferred.resolve();
            } else {
                deferred.reject();
            }
        }).fail(function() {
            deferred.reject();
        });

        return deferred;
    };


    $(document).ready(function() {

        // Handle design view tab
        $("#editorNavigationDesignLink").on("click", function(event) {
            event.preventDefault();
            if(!$(event.currentTarget).hasClass("active")) {
                switchToDesignView();
            }
        });


        // Handle source view tab
        $("#editorNavigationSourceLink").on("click", function(event) {
            event.preventDefault();
            if(!$(event.currentTarget).hasClass("active")) {
                switchToSourceView();
            }
        });


        // Handle hyperlink from malformed xml error message
        $("#editorDesignView").on("click", "#editorSwitchToSourceLink", function(event) {
            event.preventDefault();
            switchToSourceView();
        });


        // Handle save button
        $("#navbarEditorButtonsSave").on("click", function(event) {
            event.preventDefault();

            // Deactivate content assist
            if(source.orionEditor.editor.getContentAssist().isActive()) {
                source.orionEditor.editor.getContentAssist().deactivate();
            }

            // Block UI
            core.setUIBlocked(true);

            // Show saving message
            core.showControlById("navbarEditorSavingMessage", true);

            fileChangedDuringEditing().done(function() {

                // Unblock UI
                core.setUIBlocked(false);

                // Action after successful save
                fileChangedDuringEditingDialog.setCallbackFunction(function() {

                    // Disable save button
                    $("#navbarEditorButtonsSave").attr("disabled", "disabled").attr("tabindex", -1).attr("aria-disabled", true);

                    // Show changes saved message
                    core.showControlById("navbarEditorChangesSavedMessage", true);
                    window.setTimeout(function() {
                        core.hideControlById("navbarEditorChangesSavedMessage", true);
                    }, 3000);

                });

                // Hide saving message
                core.hideControlById("navbarEditorSavingMessage", false);

                // Show overwrite dialog
                $("#dialogFileChangedDuringEditing").modal("show");

            }).fail(function() {

                save().done(function() {

                    // Clear document dirty
                    documentDirty = false;

                    // Disable save button
                    $("#navbarEditorButtonsSave").attr("disabled", "disabled").attr("tabindex", -1).attr("aria-disabled", true).blur();

                    // Show changes saved message
                    core.showControlById("navbarEditorChangesSavedMessage", true);
                    window.setTimeout(function() {
                        core.hideControlById("navbarEditorChangesSavedMessage", true);
                    }, 3000);
                }).fail(function() {
                    // Show error message
                    $("#dialogErrorSavingFile").modal("show");
                }).always(function() {

                    // Unblock UI
                    core.setUIBlocked(false);

                    // Hide saving message
                    core.hideControlById("navbarEditorSavingMessage", false);
                });

            });

        });


        // Handle close button
        $("#navbarEditorButtonsClose").on("click", function(event) {
            event.preventDefault();

            // Deactivate content assist
            if(source.orionEditor.editor.getContentAssist().isActive()) {
                source.orionEditor.editor.getContentAssist().deactivate();
            }

            if($("#navbarEditorButtonsSave").attr("disabled") !== "disabled") {
                saveBeforeClosingDialog.setCallbackFunction(function(){
                    core.clearFile();
                });
                $("#dialogSaveBeforeClosing").modal("show");
            } else {
                core.clearFile();
            }
        });


        // Handle closing the browser window with unsaved changes
        $(window).on("beforeunload", function() {
            if($("#editor").is(":visible") && documentDirty) {
                return editorMessages.UNSAVED_CHANGES_MESSAGE;
            }
        });

        // Handle tooltip for content assist hint
        $("#contentAssistHint").on("mouseenter", function(event) {
            var label = event.currentTarget;
            if(label.offsetWidth < label.scrollWidth) {
                label.title = label.innerHTML;
            } else {
                $(label).removeAttr("title");
            }
        });

    });


    return {
        openFileForEditing: openFileForEditing,

        save: save,

        markDocumentAsDirty: markDocumentAsDirty,

        isDocumentDirty: function() {
            return documentDirty;
        },

        getSelectedElement: function() {
            return $("#editorTree .active").data("element");
        },

        getSchemaDocumentRootElement: function() {
            return schemaDocumentRootElement;
        },

        isFileReadOnly: function() {
            return fileReadOnly;
        },

        getFileResolvedPath: function() {
            return fileResolvedPath;
        },

        getFeatureList: function() {
            return featureList;
        },

        getValidatorList: function() {
            return validatorList;
        },

        switchToSourceView: switchToSourceView,

        fileChangedDuringEditing: fileChangedDuringEditing
    };

})();
