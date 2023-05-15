/*******************************************************************************
 * Copyright (c) 2016, 2022 IBM Corporation and others.
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

/* jshint strict: false */
define([
        "js/toolbox/toolbox",
        "js/common/platform",
        "js/widgets/MessageDialog",
        "dojo/_base/declare", 
        "dojo/_base/window",
        "dojo/_base/lang",
        "dojo/_base/json",
        "dojo/json",
        "dojo/on",
        "dojo/dom",
        "dojo/has",
        "dojox/mobile/View",
        "dijit/registry",
        "dijit/form/Button",
        "dojo/store/Memory",
        "dijit/_TemplatedMixin",
        "dijit/_WidgetsInTemplateMixin",
        "dojo/text!./templates/LibertyIdentity.html",
        "dojo/i18n!./nls/widgetsMessages"
        ], function(
                toolbox,
                platform,
                MessageDialog,
                declare,
                win,
                lang,
                json,
                DJSON,
                on,
                dom,
                has,
                _WidgetBase,
                registry,
                Button,
                Memory,
                _TemplatedMixin,
                _WidgetsInTemplateMixin,
                template,
                i18n){

    return declare("LibertyIdentity", [ _WidgetBase, _TemplatedMixin, _WidgetsInTemplateMixin ], {
        constructor : function() {
            console.log("identityString");
            //var data = this.getStore();//.get(1);
            //console.log("AAA data " + data);   
        },

        templateString:template,
        id : 'identityContentContainer',
        transitionBackTo: 'toolboxContainer',
        identityTitle: i18n.IDENTITY_TITLE,
        identityDesc: i18n.IDENTITY_DESC,
        identityCustomStr: i18n.IDENTITY_CUSTOM_STRING,
        identityCustomStrMaxChars: i18n.IDENTITY_CUSTOM_STRING_TRUNCATE,
        identityColor: i18n.IDENTITY_COLOR,
        textDirection: "ltr",
        userName: '',
        userRole: '',
        identityString: '',
        identityStringColor: '#FFFFFF',
        identityMaxChars: 27,
        store: {},

        postCreate : function() {
            console.log("postCreate is called");
            var me = this;
            this.inherited(arguments);
            this.set("aria-label", i18n.IDENTITY_TITLE);
            registry.byId("identity_headerWidget").set("secondaryTitle", i18n.IDENTITY_TITLE);
            var test = json.fromJson(BIDI_PREFS_STRING);
            console.log("AAA test " + test);
            console.log("direction " + toolbox.getToolbox().PREFERENCE_BIDI_TEXT_DIRECTION);
            var textDir = json.fromJson(BIDI_PREFS_STRING)[toolbox.getToolbox().PREFERENCE_BIDI_TEXT_DIRECTION];
            this.textDirection = textDir;
            //if (textDir === toolbox.getToolbox().PREFERENCE_BIDI_TEXT_DIRECTION_RTL){
            //if (!this.userPrefs[toolbox.getToolbox().PREFERENCE_BIDI_ENABLED]){
                // disable the radio buttons and set color for text
            //    var textColorD = platform.isPhone()? "#d4d4d4" : "#787878";
            //    this.setRadioButtonCSS(true, textColorD);
            //} else {
            //    var textColorE = platform.isPhone()? "#ffffff" : "#413d3d";
                // enable the radio buttons and set color for text
            //    this.setRadioButtonCSS(false, textColorE);
            //}
            
            //var data = this.store; //getStore()//.get(1);
            //console.log("AAA data " + data);
            //if (data) {
                //data.query({id:1});
            //    console.log("custom string " + data.input);
            //    console.log("color " + data.color);
            //}

            // create Save, Cancel button
            var saveButton = new Button({
                label: i18n.IDENTITY_SAVE,
                onClick: function(){
                    console.log("saveButton click ");
                    // Do something:
                    //dom.byId("result1").innerHTML += "Thank you! ";
                    me.onClickSaveButton();
                }
            }, "identitySaveButton");

            var cancelButton = new Button({
                label: i18n.IDENTITY_CANCEL,
                onClick: function(){
                    console.log("cancelButton click ");
                    // Do something:
                    me.onClickCancelButton();    
                }
            }, "identityCancelButton");
        },

        // startup is called after widget rendrer and mounted to dom
        startup: function() {
            console.log("startup is called");
            // remove aria-checked attribute that is put there by dojo
            //this.removeAriaChecked();
        },

        getStore: function() {
            return this.store;
        },

        changeSelectedOption: function(evt) {
            console.log("changeSelectedOption " + evt);
            //var keyPushed = evt.charOrCode;
            //if (!keyPushed) {
            //  return;
            //}
            //if (keyPushed === keys.ENTER) {
            this.setSelectedOption(evt);
            //}
        },

        setSelectedOption: function(evt) {
            var selectedOption = dijit.byId("selectIdentityOption").attr('displayedValue');
            console.log("selectedOption " + selectedOption);
            if (evt === "custom") {
                // enable input field
                var customInput = dom.byId("custom_input");
                //var LTRDom = dom.byId("textDirectionLTR");
                if (customInput !== undefined) {
                    console.log("remove disabled attr from " + customInput);
                    customInput.removeAttribute("disabled");
                }               
                var customInputMaxChars = dom.byId("custom_input_max_char");
                if (customInputMaxChars !== undefined) {
                    console.log("remove disabled attr from " + customInputMaxChars);
                    customInputMaxChars.removeAttribute("disabled");
                }
                var customColor = dom.byId("custom_color");
                if (customColor !== undefined) {
                    console.log("remove disabled attr from " + customColor);
                    customColor.removeAttribute("disabled");
                }
            } else if (evt === "none") {
                var customInput = dom.byId("custom_input");
                if (customInput !== undefined) {
                    console.log("add disabled attr to " + customInput);
                    customInput.setAttribute("disabled", "");
                }
                var customInputMaxChars = dom.byId("custom_input_max_char");
                if (customInputMaxChars !== undefined) {
                    console.log("add disabled attr to " + customInputMaxChars);
                    customInputMaxChars.setAttribute("disabled", "");
                }
                var customColor = dom.byId("custom_color");
                if (customColor !== undefined) {
                    console.log("remove disabled attr from " + customColor);
                    customColor.setAttribute("disabled", "");
                }
            }
        },

        onClickSaveButton: function() {
            console.log("onClickSaveButton ");
            var customInput = dom.byId("custom_input");
            if (customInput !== undefined && this.identityString !== customInput) {
                console.log("set custom_input " + customInput.value);
                this.identityString = customInput;
            }               
            var customInputMaxChars = dom.byId("custom_input_max_char");
            if (customInputMaxChars !== undefined && this.identityMaxChars !== customInputMaxChars) {
                console.log("set customInputMaxChars " + customInputMaxChars.value);
                this.identityMaxChars = customInputMaxChars;
            }
            var customColor = dom.byId("custom_color");
            if (customColor !== undefined && this.identityStringColor != customColor) {
                console.log("set customColor " + customColor.value);
                this.identityStringColor = customColor;
            }
            // persisted data?
            //var identityData = [
            //    {id:1, input: this.identityString, color: this.identityStringColor, maxchar: this.identityMaxChars}
            //];
            //this.store = new Memory({data: identityData});
        },
        
        onClickCancelButton: function() {
            console.log("onClickCancelButton ");
        },

        setColor: function(textColor) {
            console.log("change text color:" + textColor);
            this.identityColor = textColor;
        },

        setIdentity: function(identityString) {
            console.log("change text:" + identityString);
            this.identityString = identityString;
        },

        setMaxCharIdentityInput: function(max_char) {
            console.log("max char:" + max_char);
            this.identityCustomStrMaxChars = max_char;
        },

        setTransitionBackTo : function(viewId) {
            this.transitionBackTo = viewId;
            registry.byId("identity_headerWidget").createToolHeading(this.label, "identityContainer", this.transitionBackTo);
            // now register with the onclick of the homebutton in case they change bidi settings and we need to reload
            //registry.byId("homeButtonprefsContainer").on("click", lang.hitch(this, function(e){
            //    if (this.changed){
            //        window.location.reload(true);
            //    }
            //}));
        },
        
        setLabelColor: function(buttonWidget, color) {
          console.log("buttonWidget: ", buttonWidget);
          var labelDOM = buttonWidget.domNode.nextElementSibling;
          console.log("label DOM: ", labelDOM);
          // fix for IE
          if (labelDOM !== null && labelDOM !== undefined) {
            labelDOM.style.color = color;
            if (platform.isPhone()) {
              //console.log("set padding");
              labelDOM.style.paddingLeft = '10px';
            }
          }
        }
        
    });

});