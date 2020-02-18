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

goog.provide("settings");

var settings = (function() {
    "use strict";

    var PREFERENCE_KEY_ENHANCE_LABELS = "com.ibm.serverConfig.preferences.showEnhancedLabels";
    var PREFERENCE_KEY_SHOW_DESCRIPTIONS = "com.ibm.serverConfig.preferences.showDescriptions";
    var PREFERENCE_KEY_SHOW_LINE_NUMBERS = "com.ibm.serverConfig.preferences.showLineNumbers";
    var PREFERENCE_KEY_DEFAULT_TO_DESIGN_VIEW = "com.ibm.serverConfig.preferences.defaultToDesignView";

    $(document).ready(function() {

        // Flag for settings UI visibility handling
        var settingsShowing = false;

        // InitializePreferences
        var enhanceLabels = localStorage.getItem(PREFERENCE_KEY_ENHANCE_LABELS);
        if(enhanceLabels !== null && enhanceLabels !== undefined) {
            settings.enhanceLabels = enhanceLabels === "true";
        }
        var showDescriptions = localStorage.getItem(PREFERENCE_KEY_SHOW_DESCRIPTIONS);
        if(showDescriptions !== null && showDescriptions !== undefined) {
            settings.showDescriptions = showDescriptions === "true";
        }
        var showLineNumbers = localStorage.getItem(PREFERENCE_KEY_SHOW_LINE_NUMBERS);
        if(showLineNumbers !== null && showLineNumbers !== undefined) {
            settings.showLineNumbers = showLineNumbers === "true";
        }
        var defaultToDesignView = localStorage.getItem(PREFERENCE_KEY_DEFAULT_TO_DESIGN_VIEW);
        if(defaultToDesignView !== null && defaultToDesignView !== undefined) {
            settings.defaultToDesignView = defaultToDesignView === "true";
        }

        // Initialize popovers
        var popOverWidget = $("#settingsButtonLink").popover({
            viewport: "#mainContainer",
            placement: "bottom",
            container: "body",
            animation: false,
            html: true,
            trigger: "manual",
            content: function() {
                return "<div id=\"settings\">" +
                "<span class=\"settingsTitle\">" + editorMessages.DESIGN + "</span>" +
                "<a href=\"#\" id=\"settingsShowEnhancedLabels\" class=\"editorSetting clearfix\" role=\"button\" aria-pressed=\"" + (settings.enhanceLabels? "true" : "false") + "\">" + editorMessages.ENHANCED_LABELS + "<span id=\"settingsShowEnhancedLabelsToggle\" class=\"pull-right configurationEditorToggle " + (settings.enhanceLabels? "configurationEditorToggleOn" : "configurationEditorToggleOff") + "\"></span></a>" +
                "<a href=\"#\" id=\"settingsShowDescriptions\" class=\"editorSetting clearfix\" role=\"button\" aria-pressed=\"" + (settings.showDescriptions? "true" : "false") + "\">" + editorMessages.FIELD_DESCRIPTIONS + "<span id=\"settingsShowDescriptionsToggle\" class=\"pull-right configurationEditorToggle " + (settings.showDescriptions? "configurationEditorToggleOn" : "configurationEditorToggleOff") + "\"></span></a>" +
                "<span class=\"settingsTitle\">" + editorMessages.SOURCE + "</span>" +
                "<a href=\"#\" id=\"settingsShowLineNumbers\" class=\"editorSetting clearfix\" role=\"button\" aria-pressed=\"" + (settings.showLineNumbers? "true" : "false") + "\">" + editorMessages.LINE_NUMBERS + "<span id=\"settingsShowLineNumbersToggle\" class=\"pull-right configurationEditorToggle " + (settings.showLineNumbers? "configurationEditorToggleOn" : "configurationEditorToggleOff") + "\"></span></a>" +
                "</div>";
            }
        });


        $("#settingsButtonLink").on("click", function(event) {
            event.preventDefault();
            if(!settingsShowing) {

                // Deactivate content assist
                if(source.orionEditor.editor.getContentAssist().isActive()) {
                    source.orionEditor.editor.getContentAssist().deactivate();
                }

                popOverWidget.popover("show");
                settingsShowing = true;
                $(this).attr("aria-expanded", "true");
            } else {
                popOverWidget.popover("hide");
                settingsShowing = false;
                $(this).attr("aria-expanded", "false");
            }
        });


        $("#settingsButtonLink").on("keydown", function(event) {
            if(settingsShowing && event.keyCode === 9) {
                event.preventDefault();
                if(event.shiftKey) {
                    $("#settingsShowLineNumbers").focus();
                } else {
                    $("#settingsShowEnhancedLabels").focus();
                }
            }
        });


        $("html").on("keydown", function (event) {
            if(settingsShowing) {
                var settingsButtonLink = $("#settingsButtonLink");
                var settingsShowEnhancedLabels = $("#settingsShowEnhancedLabels");
                var settingsShowDescriptions = $("#settingsShowDescriptions");
                var settingsShowLineNumbers = $("#settingsShowLineNumbers");
                if(event.keyCode === 27 || (!(event.keyCode === 9 || event.keyCode === 16 || event.keyCode === 17 || event.keyCode === 18) && !(settingsButtonLink.is(":focus") || settingsShowEnhancedLabels.is(":focus") || settingsShowDescriptions.is(":focus") || settingsShowLineNumbers.is(":focus")))) {
                    popOverWidget.popover("hide");
                    settingsShowing = false;
                    settingsButtonLink.attr("aria-expanded", "false");
                } else if(event.keyCode === 9 && event.shiftKey && settingsShowEnhancedLabels.is(":focus")) {
                    event.preventDefault();
                    $("#settingsButtonLink").focus();
                } else if(event.keyCode === 9 && !event.shiftKey && settingsShowLineNumbers.is(":focus")) {
                    event.preventDefault();
                    $("#settingsButtonLink").focus();
                }
            }
        });


        $("body").on("mousedown touchstart", function(event) {
            if(settingsShowing) {
              var clickOnSettingsButton = $(event.target).closest("#settingsButtonLink").length > 0;
                if($(event.target).parents(".popover").length === 0 && !clickOnSettingsButton) {
                  popOverWidget.popover("hide");
                    settingsShowing = false;
                    $("#settingsButtonLink").attr("aria-expanded", "false");
                }
            }
        });


        $(window).on("resize", function() {
            if(settingsShowing) {
              popOverWidget.popover("hide");
                settingsShowing = false;
                $("#settingsButtonLink").attr("aria-expanded", "false");
            }
        });


        $("body").on("click touchstart", "#settingsShowEnhancedLabels", function(event) {
            event.preventDefault();

            // Toggle value
            settings.enhanceLabels = !settings.enhanceLabels;

            // Persist preference
            localStorage.setItem(PREFERENCE_KEY_ENHANCE_LABELS, settings.enhanceLabels);

            // Animate switch
            $("#settingsShowEnhancedLabelsToggle").toggleClass("configurationEditorToggleOn configurationEditorToggleOff");
            $("#settingsShowEnhancedLabels").attr("aria-pressed", settings.enhanceLabels? "true" : "false");

            // Update UI
            var selectedElement = editor.getSelectedElement();
            if($("#editorDesignView").is(":visible")) {
                setTimeout(function() {
                    editorTree.updateTreeLabels();
                    editorForm.renderEditorForm(selectedElement);
                }, 330);
            }
        });


        $("body").on("click touchstart", "#settingsShowDescriptions", function(event) {
            event.preventDefault();

            // Toggle value
            settings.showDescriptions = !settings.showDescriptions;

            // Persist preference
            localStorage.setItem(PREFERENCE_KEY_SHOW_DESCRIPTIONS, settings.showDescriptions);

            // Animate switch
            $("#settingsShowDescriptionsToggle").toggleClass("configurationEditorToggleOn configurationEditorToggleOff");
            $("#settingsShowDescriptions").attr("aria-pressed", settings.showDescriptions? "true" : "false");

            // Update UI
            var selectedElement = editor.getSelectedElement();
            if($("#editorDesignView").is(":visible")) {
                setTimeout(function() {
                    editorForm.renderEditorForm(selectedElement);
                }, 330);
            }
        });


        $("body").on("click touchstart", "#settingsShowLineNumbers", function(event) {
            event.preventDefault();

            // Toggle value
            settings.showLineNumbers = !settings.showLineNumbers;

            // Persist preference
            localStorage.setItem(PREFERENCE_KEY_SHOW_LINE_NUMBERS, settings.showLineNumbers);

            // Animate switch
            $("#settingsShowLineNumbersToggle").toggleClass("configurationEditorToggleOn configurationEditorToggleOff");
            $("#settingsShowLineNumbers").attr("aria-pressed", settings.showLineNumbers? "true" : "false");

            // Update UI
            setTimeout(function() {
                source.orionEditor.editor.setLineNumberRulerVisible(settings.showLineNumbers);
            }, 330);

        });

    });

    var setDefaultToDesignView = function(value) {
        settings.defaultToDesignView = value;

        // Persist preference
        localStorage.setItem(PREFERENCE_KEY_DEFAULT_TO_DESIGN_VIEW, value);
    };

    return {

        enhanceLabels: true,
        showDescriptions: true,
        showLineNumbers: true,
        defaultToDesignView: true,
        setDefaultToDesignView: setDefaultToDesignView,
        xml_indentation: "   "

    };

})();
