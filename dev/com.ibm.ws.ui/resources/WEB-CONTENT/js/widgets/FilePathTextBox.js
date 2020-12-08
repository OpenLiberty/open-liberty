/* jshint strict: false */
define(["dojo/_base/declare",
        "js/widgets/TextBox", 
        "dojox/string/BidiComplex",
        "dojo/has",
        "dojo/_base/lang",
        "dojo/domReady!"],
        function(declare,
                TextBox,
                BidiComplex,
                has,
                lang
                ) {
    return declare("js.widgets.FilePathTextBox", TextBox,{
        postCreate: function(){
            console.log("FilePathTextBox has bidi:" + has("adminCenter-bidi"));
            if (has("adminCenter-bidi")){
                BidiComplex.attachInput(this.domNode, "FILE_PATH");
                // need to set value to "" because attachInput makes placeholder disappear
                if (this.value.length === 0){
                    this.set("value", "");
                }
            }
            this.inherited(arguments);
        }

    });
});
