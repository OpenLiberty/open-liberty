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

var customizationManager = (function() {
    "use strict";
    
    var reset = function() {
        includesCustomization.reset();
    };
    
    var applyEditorFormCustomization = function(element) {
        var elementPath = xmlUtils.getElementPath(element);
        switch(elementPath.toString()) {
        case "server,include":
            includesCustomization.applyEditorFormCustomization(element);
            break;
        case "server,featureManager,feature":
            featureListCustomization.applyEditorFormCustomization(element);
            break;
        }
    };
    
    return {
        reset: reset,
        applyEditorFormCustomization: applyEditorFormCustomization
    };

})();
