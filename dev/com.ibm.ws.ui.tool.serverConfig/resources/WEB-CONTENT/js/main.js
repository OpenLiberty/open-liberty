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

goog.require("core");

$(document).ready(function() {
    "use strict";
    
    var initializeUI = function() {
        // Malformed XML document error message
        var sourcePaneLink = "<a id=\"editorSwitchToSourceLink\" href=\"#\">" + editorMessages.SOURCE_PANE + "</a>";
        $("#editorDesignViewErrorMessage").append(stringUtils.formatString(editorMessages.MALFORMED_XML, [sourcePaneLink]));
    };

    
    globalization.retrieveExternalizedStrings().done(function() {
    
        // Show progress
        core.showControlById("progress", true);
        
        // Initialize user interface
        initializeUI();

        // Check connection to server
        login.authenticate();

    }).fail(function() {
        // Display error message if externalized strings cannot be obtained
        core.renderMessage("Unable to access translation file.", "danger");
    });
    
});
