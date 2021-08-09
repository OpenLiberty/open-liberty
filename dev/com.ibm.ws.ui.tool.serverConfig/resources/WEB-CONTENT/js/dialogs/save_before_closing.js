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

var saveBeforeClosingDialog = (function() {
    "use strict";

    var callbackFunction = null;

    var invokeCallbackFunction = function() {
        if(callbackFunction !== null && callbackFunction !== undefined) {
            callbackFunction();
        }
    };

    return {

        setCallbackFunction: function(newFunction) {
            callbackFunction = newFunction;
        },

        getCallbackFunction: function() {
            return callbackFunction;
        },

        invokeCallbackFunction: invokeCallbackFunction

    };

})();


$(document).ready(function() {
    "use strict";

    $("#dialogSaveBeforeClosing").on("show.bs.modal", function(event) {

        // Update file name
        var fileName = $("#navigationBarTitle").text();
        $("#dialogSaveBeforeClosingText").text(stringUtils.formatString(editorMessages.SAVE_BEFORE_CLOSING_DIALOG_MESSAGE, [fileName]));

        // Update button states
        var otherButtons = $("#dialogSaveBeforeClosing .dialog-close-link, #dialogSaveBeforeClosingDontSaveButton");
        otherButtons.removeAttr("disabled");

        // Update button label
        $("#dialogSaveBeforeClosingSaveButton").text(editorMessages.SAVE).removeClass("loading");

        // Center dialog
        core.centerDialog(this);
    });


    $("#dialogSaveBeforeClosingSaveButton").on("click", function(event) {
        event.preventDefault();

        var button = $(this);

        // Update button label
        button.html("<img src=\"img/config-tool-home-progress-bgblue-D.gif\" alt=\"\">" + editorMessages.SAVING).addClass("loading");

        // Disable close without saving and cancel buttons
        var otherButtons = $("#dialogSaveBeforeClosing .dialog-close-link, #dialogSaveBeforeClosingDontSaveButton");
        otherButtons.attr("disabled", "disabled");

        // Block UI
        core.setUIBlocked(true);

        editor.fileChangedDuringEditing().done(function() {

            // Pass callback function
            var callbackFunction = saveBeforeClosingDialog.getCallbackFunction();
            fileChangedDuringEditingDialog.setCallbackFunction(callbackFunction);

            $("#dialogSaveBeforeClosing").modal("hide");

            // Show overwrite dialog
            $("#dialogFileChangedDuringEditing").modal("show");

        }).fail(function() {

            editor.save().done(function() {
                saveBeforeClosingDialog.invokeCallbackFunction();
            }).fail(function() {

                // Show error message
                $("#dialogErrorSavingFile").modal("show");

            }).always(function() {
                $("#dialogSaveBeforeClosing").modal("hide");

            });

        }).always(function() {

            // Unblock UI
            core.setUIBlocked(false);

        });


    });


    $("#dialogSaveBeforeClosingDontSaveButton").on("click", function(event) {
        event.preventDefault();
        $("#dialogSaveBeforeClosing").modal("hide");
        saveBeforeClosingDialog.invokeCallbackFunction();
    });


});
