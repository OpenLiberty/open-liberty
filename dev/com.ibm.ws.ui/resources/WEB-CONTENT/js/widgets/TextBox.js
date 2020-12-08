/* jshint strict: false */
define(["dojo/_base/declare",
        "dojo/_base/lang",
        "js/toolbox/toolbox",
        "js/widgets/_ValidationTextBoxMixin",
        "dojox/mobile/TextBox", 
        "dojo/has",
        "dojo/domReady!"],
        function(declare,
                lang,
                toolbox,
                ValidationTextBoxMixin,
                mTextBox,
                has
                ) {
    var TextBox = declare("js.widgets.TextBox", [mTextBox, ValidationTextBoxMixin],{
        postCreate: function(){
            if (has("adminCenter-bidi")){
                // get user preference for textDir and set it (rtl, ltr, auto)
                toolbox.getToolbox().getPreferences().then(lang.hitch(this, function(prefs) {
                    var textDir = prefs[toolbox.getToolbox().PREFERENCE_BIDI_TEXT_DIRECTION];
                    if (textDir === toolbox.getToolbox().PREFERENCE_BIDI_TEXT_DIRECTION_CONTEXTUAL){
                        this.set("textDir", "auto");
                    } else if (textDir === toolbox.getToolbox().PREFERENCE_BIDI_TEXT_DIRECTION_RTL){
                        this.set("textDir", "rtl");
                    } else {
                        this.set("textDir", "ltr");
                    }
                }));
            }
            this.inherited(arguments);
        }

    });
    return TextBox;
});
