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
        "dijit/_TemplatedMixin",
        "dijit/_WidgetsInTemplateMixin",
        "dojo/text!./templates/LibertyPrefs.html",
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
                _TemplatedMixin,
                _WidgetsInTemplateMixin,
                template,
                i18n){

    return declare("LibertyPrefs", [ _WidgetBase, _TemplatedMixin, _WidgetsInTemplateMixin ], {
        constructor : function() {
            console.log("bidiPrefsString:" + BIDI_PREFS_STRING);
            this.userPrefs = json.fromJson(BIDI_PREFS_STRING);
            this.originalUserPrefs = json.fromJson(BIDI_PREFS_STRING);
            console.log("bidi:" + this.userPrefs[toolbox.getToolbox().PREFERENCE_BIDI_ENABLED] + " and textDir:" + this.userPrefs[toolbox.getToolbox().PREFERENCE_BIDI_TEXT_DIRECTION]);
            if (this.userPrefs[toolbox.getToolbox().PREFERENCE_BIDI_ENABLED]){
                this.enableBidiChecked = "checked";
            }
            if (this.userPrefs[toolbox.getToolbox().PREFERENCE_BIDI_TEXT_DIRECTION]) {
                this.textDirection = this.userPrefs[toolbox.getToolbox().PREFERENCE_BIDI_TEXT_DIRECTION];
                if (this.textDirection === toolbox.getToolbox().PREFERENCE_BIDI_TEXT_DIRECTION_RTL){
                    this.rtlTextDirectionChecked = "checked aria-checked=true";
                } else if (this.textDirection === toolbox.getToolbox().PREFERENCE_BIDI_TEXT_DIRECTION_CONTEXTUAL){
                    this.conTextDirectionChecked = "checked aria-checked=true";
                } else {
                    // default to ltr
                    this.ltrTextDirectionChecked = "checked aria-checked=true";
                }
            } else {
                // default to ltr
                this.ltrTextDirectionChecked = "checked aria-checked=true";
            }
        },

        templateString:template,
        id : 'prefsContentContainer',
        transitionBackTo: 'toolboxContainer',
        prefsTitle: i18n.PREFERENCES_SECTION_TITLE,
        enableBidiLabel: i18n.PREFERENCES_ENABLE_BIDI,
        textDirectionLabel: i18n.PREFERENCES_BIDI_TEXTDIR,
        textDirectionLTR: i18n.PREFERENCES_BIDI_TEXTDIR_LTR,
        textDirectionRTL: i18n.PREFERENCES_BIDI_TEXTDIR_RTL,
        textDirectionContextual: i18n.PREFERENCES_BIDI_TEXTDIR_CONTEXTUAL,
        userName: '',
        userRole: '',
        userPrefs: {},
        originalUserPrefs: {},
        changed: false,
        bidiEnabled: false,
        textDirection: "ltr",
        enableBidiChecked: "",
        ltrTextDirectionChecked: "aria-checked=false",
        rtlTextDirectionChecked: "aria-checked=false",
        conTextDirectionChecked: "aria-checked=false",

        postCreate : function() {
            this.inherited(arguments);
            this.set("aria-label", i18n.PREFERENCES_SECTION_TITLE);
            registry.byId("prefs_headerWidget").set("secondaryTitle", i18n.PREFERENCES_TITLE);
            if (!this.userPrefs[toolbox.getToolbox().PREFERENCE_BIDI_ENABLED]){
                // disable the radio buttons and set color for text
                var textColorD = platform.isPhone()? "#d4d4d4" : "#787878";
                this.setRadioButtonCSS(true, textColorD);
            } else {
                var textColorE = platform.isPhone()? "#ffffff" : "#413d3d";
                // enable the radio buttons and set color for text
                this.setRadioButtonCSS(false, textColorE);
            }

        },

        changeBidi : function(evt){
            // evt is true or false
            if (evt) {
                console.log("enabling bidi");
                this.userPrefs[toolbox.getToolbox().PREFERENCE_BIDI_ENABLED] = true;
                // enable the radio buttons and set color for text
                var textColor = platform.isPhone()? "#ffffff" : "#413d3d";
                this.setRadioButtonCSS(false, textColor);
                // set adminCenter-bidi in config though this does not seem to work
                has.add("adminCenter-bidi", true);
                console.log("has bidi should be true:" + has("adminCenter-bidi"));
            } else {
                console.log("disabling bidi");
                this.userPrefs[toolbox.getToolbox().PREFERENCE_BIDI_ENABLED] = false;
                // disable the radio buttons and set color for text
                var textColor2 = platform.isPhone()? "#d4d4d4" : "#787878";
                this.setRadioButtonCSS(true, textColor2);
                // unset adminCenter-bidi in config though this does not seem to work
                has.add("adminCenter-bidi", false);
                console.log("has bidi should be false:" + has("adminCenter-bidi"));
            }
            this.setPrefs();
        },

        setTextDirLTR: function(evt){
            if (evt) {
                this.setTextDir(toolbox.getToolbox().PREFERENCE_BIDI_TEXT_DIRECTION_LTR);
            }
        },

        setTextDirRTL: function(evt){
            if (evt) {
                this.setTextDir(toolbox.getToolbox().PREFERENCE_BIDI_TEXT_DIRECTION_RTL);
            }
        },

        setTextDirContextual: function(evt){
            if (evt) {
                this.setTextDir(toolbox.getToolbox().PREFERENCE_BIDI_TEXT_DIRECTION_CONTEXTUAL);
            }
        },

        setTextDir: function(textDirection){
            console.log("change textDir:" + textDirection + ", prefs:" + this.userPrefs);
            this.textDirection = textDirection;
            this.userPrefs[toolbox.getToolbox().PREFERENCE_BIDI_TEXT_DIRECTION] = textDirection;
            this.setPrefs();
         },

         setPrefs: function(){
             console.log("setting prefs:" + this.userPrefs[toolbox.getToolbox().PREFERENCE_BIDI_ENABLED] + "-" + this.userPrefs[toolbox.getToolbox().PREFERENCE_BIDI_TEXT_DIRECTION]);
             toolbox.getToolbox().updatePreferences(this.userPrefs).then(lang.hitch(this, function() {
                 this.changed = DJSON.stringify(this.userPrefs) !== DJSON.stringify(this.originalUserPrefs);
                 console.log("success so nothing else to do, settings changed:" + this.changed);
             }, function(err) {
                 // handle addTool errors
                 console.error("Error setting preferences in the toolbox: ", err.message);
                 // display error
                 var errorMessageDialog = new MessageDialog({ 
                     title: i18n.LIBERTY_UI_ERROR_MESSAGE_TITLE,
                     messageText: lang.replace(i18n.PREFERENCES_SET_ERROR_MESSAGE, [err.message])
                 });
                 errorMessageDialog.placeAt(win.body());
                 errorMessageDialog.startup();
                 errorMessageDialog.show();
             }));
          },

        setTransitionBackTo : function(viewId) {
            this.transitionBackTo = viewId;
            registry.byId("prefs_headerWidget").createToolHeading(this.label, "prefsContainer", this.transitionBackTo);
            // now register with the onclick of the homebutton in case they change bidi settings and we need to reload
            registry.byId("homeButtonprefsContainer").on("click", lang.hitch(this, function(e){
                if (this.changed){
                    window.location.reload(true);
                }
            }));
        },
        
        setRadioButtonCSS: function(isDisabled, color) {
          console.log('------------------ in setRadioButtonCss isDisable: ', isDisabled);
          //console.log('------------------ in setRadioButtonCss color: ', color);
          var LTRWidget = registry.byId("textDirectionLTR");
          LTRWidget.set("disabled", isDisabled);
          var RTLWidget = registry.byId("textDirectionRTL");
          RTLWidget.set("disabled", isDisabled);
          var contextualWidget = registry.byId("textDirectionContextual");
          contextualWidget.set("disabled", isDisabled);
          this.setLabelColor(LTRWidget, color);
          this.setLabelColor(RTLWidget, color);
          this.setLabelColor(contextualWidget, color);
          dom.byId("textDirectionLabel").style.color = color;
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