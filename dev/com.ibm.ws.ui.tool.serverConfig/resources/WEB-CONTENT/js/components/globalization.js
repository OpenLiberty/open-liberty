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

var globalization = (function() {
    "use strict";
    
    var bidiEnabled = false;
    var bidiTextDirection = "ltr";
    
    var supportedLanguages = ["cs",
                              "de",
                              "en",
                              "es", 
                              "fr",
                              "hu",
                              "it",
                              "ja",
                              "ko",
                              "pl",
                              "pt-br",
                              "ro",
                              "ru",
                              "zh",
                              "zh-tw"
                             ];
    
    /**
     * Check if we support the browser language
     */
    var checkIfLanguageSupported = function(browserLanguage){
      var languageCode = "";
      
      if(supportedLanguages.indexOf(browserLanguage) !== -1) {
        languageCode = browserLanguage;
      } 
      else if(browserLanguage.length === 5) { 
        var code = browserLanguage.substring(0, 2);
        if(supportedLanguages.indexOf(code) !== -1) {
          languageCode = code;
        }
      }
      
      return languageCode;
    };
    
    /**
     * Get language code from the browser's list of languages
     */
    var getLanguageCode = function(){      
      // Obtain browser languages
      var browserLanguages = navigator.languages;
      var languageCode = "";
      var browserLanguage = "";
      
      // Loop through languages and check if supported 
      if(browserLanguages){
        for(var i=0; i<browserLanguages.length; i++){
          languageCode = checkIfLanguageSupported(browserLanguages[i].toLowerCase());
          if(languageCode){
            break;
          }
        }
      }
      else{
        browserLanguage = navigator.language ? navigator.language : navigator.userLanguage;
        languageCode = checkIfLanguageSupported(browserLanguage.toLowerCase());
      }
      // Default to English if no language found
      if (languageCode === ""){
        languageCode = "en";
      }
      
      return languageCode;
    };


    var retrieveExternalizedStrings = function() {
        
        var languageCode = getLanguageCode();
        
        var url = (languageCode !== "en") ? "nls/" + languageCode + "/messages.js" : "nls/messages.js"; //defaults to english if not set
            
        // Retrieve translations
        return $.ajax({
            url: url,
            dataType: "script",
            success: function() {
                replaceExternalizedStrings();
            }
        });
    };
    
    
    var retrieveBidiPreference = function() {
        return $.ajax({
            url: "/ibm/api/adminCenter/v1/toolbox/preferences",
            success: function(data) {
                if(data) {
                    bidiEnabled = data.bidiEnabled;
                    bidiTextDirection = data.bidiTextDirection;
                }
            }
        });
    };

    
    var dataTypeRequiresSpecialHandling = function(dataType) {
        return dataType === "location" || dataType === "dirLocation" || dataType === "fileLocation";
    };
    
    
    var replaceExternalizedStrings = function() {
        
        $("[data-externalizedString]").each(function() {
            var element = $(this);
            element.append(editorMessages[element.attr("data-externalizedString")]);
        });
        
        $("[data-externalizedPlaceholder]").each(function() {
            var element = $(this);
            element.attr("placeholder", editorMessages[element.attr("data-externalizedPlaceholder")]);
        });
        
        $("[data-externalizedStringTitle]").each(function() {
            var element = $(this);
            element.attr("title", editorMessages[element.attr("data-externalizedStringTitle")]);
        });
        
        $("[data-externalizedStringAlt]").each(function() {
            var element = $(this);
            element.attr("alt", editorMessages[element.attr("data-externalizedStringAlt")]);
        });

    };
    
    var obtainContextualDir = function(text) {
        // look for strong (directional) characters
        var fdc = /[A-Za-z\u05d0-\u065f\u066a-\u06ef\u06fa-\u07ff\ufb1d-\ufdff\ufe70-\ufefc]/.exec(text);
        // if found return the direction that defined by the character, else return "ltr"
        return fdc ? ( fdc[0] <= 'z' ? "ltr" : "rtl" ) : "ltr";
    };

    var _isBidiChar = function(c){
        return (c >= '\u0030' && c <= '\u0039') || (c > '\u00ff');
    };

    var _isLatinChar = function(c){
        return (c >= '\u0041' && c <= '\u005A') || (c >= '\u0061' && c <= '\u007A');
    };
    
    var _createBidiDisplayString = function(/*String*/str, /*String*/pattern){
        // summary:
        //        Create the display string by adding the Unicode direction Markers
        // pattern: Complex Expression Pattern type. One of "FILE_PATH", "URL", "EMAIL", "XPATH"

        str = _stripBidiSpecialCharacters(str);
        var segmentsPointers = _parseBidi(str, pattern);
        
        var buf = '\u202A'/*LRE*/ + str;
        var shift = 1;
        segmentsPointers.forEach(function(n){
            if(n !== null && n !== undefined){
                var preStr = buf.substring(0, n + shift);
                var postStr = buf.substring(n + shift, buf.length);
                buf = preStr + '\u200E'/*LRM*/ + postStr;
                shift++;
            }
        });
        return buf;
    };

    var _stripBidiSpecialCharacters = function(str){
        // summary:
        //        removes all Unicode directional markers from the string

        return str.replace(/[\u200E\u200F\u202A-\u202E]/g, ""); // String
    };

    var _parseBidi = function(/*String*/str, /*String*/pattern){
        var previous = -1, segmentsPointers = [];
        var delimiters = {
            FILE_PATH: "/\\:.",
            URL: "/:.?=&#",
            XPATH: "/\\:.<>=[]",
            EMAIL: "<>@.,;"
        }[pattern];

        switch(pattern){
            case "FILE_PATH":
            case "URL":
            case "XPATH":
                for (var i=0; i < str.length; i++) {
                    var ch = str.charAt(i);
                    if(delimiters.indexOf(ch) >= 0 && _isCharBeforeBiDiChar(str, i, previous)){
                        previous = i;
                        segmentsPointers.push(i);
                    }
                }
                break;
            case "EMAIL":
                /* jshint shadow:true */
                for (var i=0; i < str.length; i++) {
                    var ch = str.charAt(i);
                    if(ch === '\"'){
                        if(_isCharBeforeBiDiChar(str, i, previous)){
                            previous = i;
                            segmentsPointers.push(i);
                        }
                        i++;
                        var i1 = str.indexOf('\"', i);
                        if(i1 >= i){
                            i = i1;
                        }
                        if(_isCharBeforeBiDiChar(str, i, previous)){
                            previous = i;
                            segmentsPointers.push(i);
                        }
                    }
    
                    if(delimiters.indexOf(ch) >= 0 && _isCharBeforeBiDiChar(str, i, previous)){
                                previous = i;
                                segmentsPointers.push(i);
                    }
                }
                /* jshint shadow:false */
        }
        return segmentsPointers;
    };

    var _isCharBeforeBiDiChar = function(buffer, i, previous){
        while(i > 0){
            if(i === previous){
                return false;
            }
            i--;
            if(_isBidiChar(buffer.charAt(i))){
                return true;
            }
            if(_isLatinChar(buffer.charAt(i))){
                return false;
            }
        }
        return false;
    };
    
    return {
        retrieveExternalizedStrings: retrieveExternalizedStrings,
        retrieveBidiPreference: retrieveBidiPreference,
        obtainContextualDir: obtainContextualDir,
        dataTypeRequiresSpecialHandling: dataTypeRequiresSpecialHandling,
        createBidiDisplayString: _createBidiDisplayString,
        stripBidiSpecialCharacters: _stripBidiSpecialCharacters,
        getLanguageCode: getLanguageCode,
        isBidiEnabled: function() {
            return bidiEnabled;
        },
        getBidiTextDirection: function() {
            return bidiTextDirection;
        }
    };
    
})();
