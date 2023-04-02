/*******************************************************************************
 * Copyright (c) 2015, 2022 IBM Corporation and others.
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

var featureListCustomization = (function() {
    "use strict";
    
    var applyEditorFormCustomization = function(element) {
        
        var featureList = editor.getFeatureList();
        var readOnly = editor.isFileReadOnly();
        
        if(featureList !== null && featureList !== undefined) {
            var inputGroup = $("<div class=\"input-group\"></div>");
            var inputGroupButton = $("<span class=\"input-group-btn\"></span>");
            var buttonDisabledField = (readOnly) ? "tabindex=\"-1\" disabled=\"disabled\" aria-disabled=\"true\"" : ""; // Disable select button in read-only mode
            var button = $("<a role=\"button\"" + buttonDisabledField + "class=\"btn btn-default editorFormFieldButton featureListSelectButton\" draggable=\"false\" href=\"#\">" + editorMessages.SELECT + "</a>");
    
            var target = $("#editorFormControl div[style^='position'][style*='relative']");
            button.data("inputControl", $("input", target));
            var targetParent = target.parent();
            
            
            inputGroupButton.append(button);
            target.append(inputGroupButton);
            inputGroup.append(target);
            targetParent.append(inputGroup);
        }

    };
    
    return {        
        applyEditorFormCustomization: applyEditorFormCustomization
    };

})();
