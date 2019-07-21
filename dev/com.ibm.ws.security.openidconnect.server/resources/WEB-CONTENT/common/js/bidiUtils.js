/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

var bidiUtils = (function() {
    "use strict";

    var __getBidiTextDirection = function() {
        if (globalization.isBidiEnabled()) {
            var dir = globalization.getBidiTextDirection();
            if (dir === "contextual") {
                dir = "auto";
            }
            return dir;
        } else {
            return "";
        }
    }

    var __setBidiTextDirection = function(element) {
        var dirValue = __getBidiTextDirection();
        if (dirValue !== "") {
            element.attr('dir', dirValue);
        }
    };

    /**
     * Return the DOM bidi direction string, eg. dir="rtl"
     */
    var getDOMBidiTextDirection = function() {
        var domTextDir = "";
        var dirValue = __getBidiTextDirection();
        if (dirValue !== "") {
            domTextDir = "dir='" + dirValue + "'";
        }
        return domTextDir;
    }

    /**
     * Set bidi text direction for input type fields
     */
    var setupToolBidi = function() {
        $(".tool_filter_input, .tool_modal_title, input.tool_modal_body_field_input, input.tool_modal_body_field_auth_value").each(function() {
            __setBidiTextDirection($(this));
        });
    };

    /**
     * Enclose the name passed in with a span that has dir in it, eg.
     * <span dir="rtl">name</span>
     * 
     * Otherwise return name as is.
     * @param name name to display with bidi direction support
     */
    var getDOMSpanWithBidiTextDirection = function(name) {
        // insert bidi text direction to the name
        var domElement = name;
        var dirText = bidiUtils.getDOMBidiTextDirection();
        if (dirText !== "") {
            domElement= "<span " + dirText + ">" + name + "</span>";
        }
        return domElement;
    };

    // not used at this point to add unicode control character as URL is not special handling
    var displayBidiString = function(element) {
        var type = "STRING";
        if (element.attr('valueType')) {
            type = element.attr('valueType');
        }
        if (globalization.dataTypeRequiresSpecialHandling(type) && globalization.isBidiEnabled()) {
            if (element.val()) {
                var bidiDisplayString = globalization.createBidiDisplayString(element.val(), type);
                element.val(bidiDisplayString);
            }
        }
    };

    return {
        setupToolBidi: setupToolBidi,
        getDOMBidiTextDirection: getDOMBidiTextDirection,
        getDOMSpanWithBidiTextDirection: getDOMSpanWithBidiTextDirection,
        displayBidiString: displayBidiString
    };

})();