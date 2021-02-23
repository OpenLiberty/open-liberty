/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

var apiMsgUtils = (function() {
    "use strict";

    // TODO: The 3 find methods below have been largely copied and pasted from explore.
    // As such, Explore should be refactored to pull in this version
    /**
     * Finds the Error string in the input
     * 
     * @private
     * @param {string}
     *          input - The input string in which to search for an error
     * @returns {string|null} error - The first error string found in the input, starting with the Error ID and ending at the newline. If no error
     *          found in the input, returns null;
     */
    var __findErrorMsg = function(input) {
        var errMsg = null;
        var errStartIndex = input.search(/[A-Z]{5}\d{4}[E]:\s/);
        var errEndIndex = input.search(/\r\n|\r|\n/g);
        if (errStartIndex !== -1 && errEndIndex !== -1) {
            errMsg = input.substring(errStartIndex, errEndIndex);
        }
        return errMsg;
    };

    /**
     * Finds the Warning string in the input
     * 
     * @private
     * @param {string}
     *          input - The input string in which to search for a warning
     * @returns {string|null} warning - The first warning string found in the input, starting with the warning ID and ending at the newline. If no
     *          warning found in the input, returns null;
     */
    var __findWarningMsg = function(input) {
        var warnMsg = null;
        var errStartIndex = input.search(/[A-Z]{5}\d{4}[W]:\s/);
        var errEndIndex = input.search(/\r\n|\r|\n/g);
        if (errStartIndex !== -1 && errEndIndex !== -1) {
            warnMsg = input.substring(errStartIndex, errEndIndex);
        }
        return warnMsg;
    };

    /**
     * Gets the first line of the stack trace, which tends to have a meaningful message
     * 
     * @private
     * @param {string}
     *          input - The input string from which to get the first line after '"stackTrace": "'
     * @returns {string|null} firstLine - The first line of the stack trace, null otherwise
     */
    var __firstLineOfStackTrace = function(input) {
        var firstLine = null;
        var errStartIndex = input.search('"stackTrace": "');
        var errEndIndex = input.search(/\r\n|\r|\n/g);
        if (errStartIndex !== -1 && errEndIndex !== -1) {
            errStartIndex += '"stackTrace": "'.length;
            firstLine = input.substring(errStartIndex, errEndIndex);
        }
        return firstLine;
    };


  /**
   * Encode untrusted data by replacing the following characters with HTML entity
   * encoding values before inserting into the DOM.
   * 
   *      Characters replaced: &, <, >, ", ', #, and /
   * 
   * @param {string} dataString 
   */
   var __encodeData = function(dataString) {
    var chars = {'&': '&amp;',
                 '<': '&lt;',
                 '>': '&gt;',
                 '"': '&quot;',
                 "'": '&#039;',
                 '#': '&#035;',
                 '/': '&#x2F;'};
    return dataString.replace( /[&<>'"#/]/g, function(c) { return chars[c]; } );
  };

    return {
        findErrorMsg: __findErrorMsg,
        findWarningMsg: __findWarningMsg,
        firstLineOfStackTrace: __firstLineOfStackTrace,
        encodeData: __encodeData
    };

})();
