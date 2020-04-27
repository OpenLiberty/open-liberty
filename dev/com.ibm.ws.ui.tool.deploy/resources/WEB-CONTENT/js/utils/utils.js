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

var utils = (function(){
    "use strict";

    var __formatString = function(value, args) {
        for (var i = 0; i < args.length; i++) {
            var regexp = new RegExp('\\{'+i+'\\}', 'gi');
            try{
                value = value.replace(regexp, args[i]);
            }
            catch(err){
                console.error("Translation message is not available: ", err);
            }
        }
        return value;
    };

    var __replaceSpaces = function(phrase){
      var segments = phrase.split(" ");
      for(var i = 0; i < segments.length; ++i){
        segments[i] = segments[i].substring(0,1).toUpperCase() + segments[i].substring(1);
      }
      return segments.join("");
    };

    /**
     * Encode untrusted data by replacing the following characters with HTML entity
     * encoding values before inserting into the DOM.
     * 
     *      Characters replaced: &, <, >, ", ', #, and /
     * 
     * @param String dataString 
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

    var __checkIfToolboxExists = function(){
        var deferred = new $.Deferred();

        var ajaxPromise = $.ajax({
            url: "/adminCenter/feature",
            cache: false,
            success: function(response) {
                deferred.resolve(true);
            },
            error: function(response) {
                deferred.reject(false);
            }
        });

        return deferred;
    };

    var __checkIfExploreToolExists = function(){
        var deferred = new $.Deferred();

        var ajaxPromise = $.ajax({
            url: "/ibm/adminCenter/explore-1.0/feature",
            cache: false,
            success: function(response) {
                deferred.resolve(true);
            },
            error: function(response) {
                deferred.reject(false);
            }
        });

        return deferred;
    };

    var __setBidiTextDirection = function(element, isDomElement) {
        if (globalization.isBidiEnabled()) {
            var dirValue = globalization.getBidiTextDirection();
            if (dirValue !== "") {
                if (isDomElement) {
                    element.dir = dirValue;
                    //element.title = dirValue;
                } else {
                    element.attr("dir", dirValue);
                }
            }
        }
    };

    var __disableFormWithOverlay = function(){
        $("#contents, #review").addClass("underlay");
        this.__disableForm();
    };

    // Focus on the input and adjust the screen according to the ruleselect
    var __focusInvalidInput = function(input){
        $(input).focus();
        var yPosition = $(window).scrollTop();
        var ruleSelectorHeight = $("#ruleSelect").height();
        $(window).scrollTop(yPosition-(ruleSelectorHeight + 50));
    };

    var __disableForm = function(panel) {
        this.regions = $(".parameters, #hostSelection, #security, #review");
        this.inputs = $(".parameters input:not(.hide), .parameters button:not(.hide), .parameters a, #backgroundTasksButton, #searchHost, #hostSelectionContent button, #security input, #deployButton");
        this.links = $(".dockerImagesErrorLink a, #hostSearchFooter a, #showMissingParameters, #fileUploadBrowseMessage, #deployButton");

        this.regions.prop("tabindex", "-1");

        // Disable all inputs and deploy button and disable tabbing
        this.inputs.prop("disabled", "true");
        this.inputs.prop("tabindex", "-1");
        this.inputs.prop("cursor", "default");

        // Disable links and tabbing
        this.links.addClass("disabledLink");
        this.links.prop("tabindex", "-1");
        this.links.prop("cursor", "default");

        if (panel === 'ruleSelect') {
            $("#review").prop("tabindex", "-1");
            $("#footerButton").prop("tabindex", "-1");
            $("#footerButton").prop("disabled", "true");
            $('html body').css('overflow', 'hidden');
        }
    };

    var __enableForm = function() {
        this.regions = $(".parameters, #hostSelection, #security, #review");
        this.inputs = $(".parameters input:not(.hide), .parameters button:not(.hide), .parameters a, #backgroundTasksButton, #searchHost, #hostSelectionContent button, #security input, #deployButton");
        this.links = $(".dockerImagesErrorLink a, #hostSearchFooter a, #showMissingParameters, #fileUploadBrowseMessage, #deployButton");

        this.regions.prop("tabindex", "0");

        this.inputs.prop("disabled", false);
        this.inputs.prop("tabindex", "0");
        this.inputs.removeProp("cursor");

        this.links.removeClass("disabledLink");
        this.links.prop("tabindex", "0");
        this.links.removeProp("cursor");

        $("#contents, #review").removeClass("underlay");

        $("#review").prop("tabindex", "0");
        $("#footerButton").prop("tabindex", "0");
        $("#footerButton").removeProp("disabled");

        $('html body').css('overflow', 'visible');

        // need to enable or disable the deploy button
        validateUtils.validate();
    };

    /* Disables opening of the template when the modal is in view */
    var __disableTemplate = function() {
        this.templateLinks = $("#server-selection, #deploy-selection, #edit-selection");
        this.templateLinks.prop("tabindex", "-1");
        this.templateLinks.addClass("disabledLink");
    };

    var __enableTemplate = function() {
        this.templateLinks = $("#server-selection, #deploy-selection, #edit-selection");
        this.templateLinks.prop("tabindex", "0");
        this.templateLinks.removeClass("disabledLink");
    };

    return {
        formatString: __formatString,
        replaceSpaces: __replaceSpaces,
        encodeData: __encodeData,
        checkIfToolboxExists: __checkIfToolboxExists,
        checkIfExploreToolExists: __checkIfExploreToolExists,
        setBidiTextDirection: __setBidiTextDirection,
        focusInvalidInput: __focusInvalidInput,
        disableForm: __disableForm,
        disableFormWithOverlay: __disableFormWithOverlay,
        enableForm: __enableForm,
        disableTemplate: __disableTemplate,
        enableTemplate: __enableTemplate
    };
})();
