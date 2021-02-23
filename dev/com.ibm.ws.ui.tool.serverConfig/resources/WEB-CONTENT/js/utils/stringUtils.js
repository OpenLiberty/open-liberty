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

var stringUtils = (function() {
    "use strict";
    
    var capitalizeString = function(value) {
        return value.charAt(0).toUpperCase() + value.slice(1).toLowerCase();
    };
    
    
    var formatString = function(value, args) {
        for (var i = 0; i < args.length; i++) {
            var regexp = new RegExp('\\{'+i+'\\}', 'gi');
            value = value.replace(regexp, args[i]);
        }
        return value;
    };
    
    
    var getLineCount = function (value) {
        return value.split(/\r\n|\r|\n/).length;
    };
    
    
    var getQueryStringParameter = function(name) {
        var queryString = window.location.search.substring(1);
        var parameters = queryString.split("&");
        for (var i = 0; i < parameters.length; i++) {
            var parameterName = parameters[i].split("=");
            if (parameterName[0] === name) {
                return parameterName[1];
            }
        }
        return null;
    };    
    
    return {
        capitalizeString: capitalizeString,
        formatString: formatString,
        getLineCount: getLineCount,
        getQueryStringParameter: getQueryStringParameter
    };
    
})();
