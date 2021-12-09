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

var fileChangedDuringEditingDialog = (function() {
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

        invokeCallbackFunction: invokeCallbackFunction

    };

})();


$(document).ready(function() {
    "use strict";

    $("#dialogFileChangedDuringEditing").on("show.bs.modal", function(event) {

        // Update file name
        var fileName = $("#navigationBarTitle").text();
        $("#dialogFileChangedDuringEditingText").text(stringUtils.formatString(editorMessages.FILE_CHANGED_DURING_EDITING_DIALOG_MESSAGE, [fileName]));

        // Update button states
        var otherButtons = $("#dialogFileChangedDuringEditing .dialog-close-link, #dialogFileChangedDuringEditingCancelButton");
        otherButtons.removeAttr("disabled");

        // Update button label
        $("#dialogFileChangedDuringEditingOverwriteButton").text(editorMessages.OVERWRITE).removeClass("loading");

        // Center dialog
        core.centerDialog(this);
    });


    $("#dialogFileChangedDuringEditingOverwriteButton").on("click", function(event) {
        event.preventDefault();

        // Block UI
        core.setUIBlocked(true);

        var button = $(this);

        // Update button label
        button.html("<img src=\"img/config-tool-dialog-progress-bgred-D.gif\">" + editorMessages.OVERWRITING).addClass("loading");

        // Disable close without saving and cancel buttons
        var otherButtons = $("#dialogFileChangedDuringEditing .dialog-close-link, #dialogFileChangedDuringEditingCancelButton");
        otherButtons.attr("disabled", "disabled");

        editor.save().done(function() {
            fileChangedDuringEditingDialog.invokeCallbackFunction();
        }).fail(function() {

            // Show error message
            $("#dialogErrorSavingFile").modal("show");

        }).always(function() {
            $("#dialogFileChangedDuringEditing").modal("hide");

            // Unblock UI
            core.setUIBlocked(false);
        });
    });


    $("#dialogFileChangedDuringEditingCancelButton").on("click", function(event) {
        event.preventDefault();
        $("#dialogFileChangedDuringEditing").modal("hide");
    });

});
