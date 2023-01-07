/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

$(document).ready(function() {
    "use strict";

    $("#dialogErrorSavingFile").on("show.bs.modal", function(event) {
        // Center dialog
        core.centerDialog(this);
    });


    $("#dialogErrorSavingFile").on("keydown", function(event) {
        if(event.keyCode === 13) {
            event.preventDefault();
            $("#dialogErrorSavingFileReturnToEditorButton").trigger("click");
        }
    });


    $("#dialogErrorSavingFileReturnToEditorButton").on("click", function(event) {
        event.preventDefault();
        $("#dialogErrorSavingFile").modal("hide");
    });

});
