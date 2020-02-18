require({cache:{
'url:js/toolbox/templates/addBookmark.html':"<div>\r\n\t<div data-dojo-attach-point=\"addBookmarkContent\" >\r\n\t\t<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"2\">\r\n\t\t\t<tr>\r\n\t\t\t\t<td><input data-dojo-attach-point=\"bookmarkName\"/><div id=\"bookmarkNameErrorMessageDiv\"></div></td>\r\n\t\t\t</tr>\r\n\t\t\t<tr>\r\n\t\t\t\t<td><input data-dojo-attach-point=\"bookmarkURL\"/><div id=\"bookmarkURLErrorMessageDiv\"  style=\"margin-bottom:15px\"></div></td>\r\n\t\t\t</tr>\r\n\t\t</table>\r\n\t\t<button data-dojo-attach-point=\"addButton\"></button>\r\n\t\t</div>\r\n</div>"}});
define("js/toolbox/addBookmark", 
    ["js/catalog/urlUtils",
     "js/common/platform",
     "js/toolbox/toolbox",
     "js/widgets/URLTextBox",
     "dojo/_base/declare",
     "dijit/_WidgetBase", 
     "dijit/registry",
     "dijit/_TemplatedMixin",
     "dojo/text!./templates/addBookmark.html", 
     "dojo/i18n!./nls/toolboxMessages", 
     "js/widgets/TextBox",
     "dojox/validate", 
     "dojox/string/BidiComplex",
     "dijit/form/Button", 
     "dojo/_base/lang",
     "dojo/has"],
        function(utils,
                 platform,
                 toolbox,
                 URLTextBox,
                 declare, 
                 WidgetBase,
                 registry,
                 TemplatedMixin,
                 template, 
                 i18n,
                 TextBox, 
                 validate, 
                 BidiComplex,
                 Button, 
                 lang,
                 has) 
{
    "use strict";
    var ignoreInaccessibleURL = false;
    var ignoreInvalidURL = false;
    var previousURL = "";
    
    function processURL(url) {
        if (url && url.search("://") < 0) {
            url = "http://"+url;
        }
        if (has("adminCenter-bidi")){
            url = BidiComplex.stripSpecialCharacters(url);
        }
        return url;
    }
    
    return declare([WidgetBase, TemplatedMixin], {
        templateString: template,

        postCreate : function custom_addBookmark_postCreate(){
            var me = this;
            var vboxName = new TextBox ({
                placeHolder: i18n.TOOL_NAME,
                required : true,
                id: "bookmarkName",
                missingMessage: i18n.TOOL_NAME_MISSING
            }, this.bookmarkName).startup();
            registry.byId("bookmarkName").set("aria-label", i18n.TOOL_NAME);

            // shorter for smartphones
            var width = platform.isPhone() ? "200px" : "320px";
            var vboxURL = new URLTextBox ({
                placeHolder: i18n.TOOL_URL,
                required : true,
                regExp: ".+",
                id: "bookmarkURL",
                missingMessage: i18n.TOOL_URL_MISSING,
                invalidMessage: i18n.TOOL_URL_INVALID,
                // Define a validator() which will only be invoked when the URL field value
                // is about to be submitted, not on a per keystroke instance.
                // See 160975
                urlValidator: function(){
                 var value = this.textbox.value;
                  if (has("adminCenter-bidi")){
                      value = BidiComplex.stripSpecialCharacters(value);
                  }
                  if (value.substr(0, 4) !== "http") {
                      value = "http://".concat(value);
                  }
                  var valid = utils.isValidUrl(value);
                  return valid;
              }
            }, this.bookmarkURL).startup();
            registry.byId("bookmarkURL").set("aria-label", i18n.TOOL_URL);

            var buttonAddBookmark = new Button({
                label: i18n.ADD_BUTTON,
                id: "addBookmarkButtonId",
                onClick: lang.hitch (this, function(){
                    var nameField = registry.byId("bookmarkName");
                    var urlField = registry.byId("bookmarkURL");

                    var url = processURL(urlField.get("value"));
                   
                    if (  nameField.isValid() === false )
                    {
                        nameField.focus();
                        return false;
                    }

                    // isValid() invokes the validator method defined for the 
                    // TextBox field.
                    // On submission, switch validator() to be the method that
                    // will validate the URL value from the original, generic 
                    // validator() provided by our custom _ValidationTextBoxMixin.js.
                    var originalValidator = urlField.validator;
                    urlField.validator = urlField.urlValidator;
                    if ( processURL(registry.byId("bookmarkURL").value) !== previousURL )
                    {
                        ignoreInvalidURL = false;
                    }
                    if (  urlField.isValid() === false && ignoreInvalidURL === false)
                    {
                        urlField.focus();
                        // focus() invokes validate() which will again test if
                        // if the value in the urlField isValid() and will post
                        // any error or warning messages if needed.
                        // (focus->validate()->isValid()->validator())
                        // Afterwards, switch the validator() method back to point
                        // to the generic validator() as we await further keystrokes
                        // as we only want to validate the actual URL value on 
                        // dialog submission and not on a per keystroke basis (see 160975).
                        urlField.validator = originalValidator;
                        previousURL = processURL(urlField.get("value"));
                        ignoreInvalidURL = true;
                        return false;
                    }
                    else if(ignoreInvalidURL === true)
                    {
                      me.addBookmark();
                    }
                    // Since this module is never destroyed but just hidden when user
                    // closes it, we must reset the URL field's ("Bookmark Address")
                    // validator() function back to the original, otherwise, the tooltip error
                    // "The URL is invalid. Enter a valid URL" will appear on a per 
                    // keystroke basis
                    urlField.validator = originalValidator;
                    // check if the url is accessible
                    utils.isUrlAccessible(url).then( function(value) {
                        if ( url !== previousURL )
                        {
                            ignoreInaccessibleURL = false;
                        }
                        if ( value === true || ignoreInaccessibleURL === true)
                        {
                            me.addBookmark();
                        }
                        else
                        {
                            me.displayError("bookmarkURL", i18n.TOOL_URL_INACCESSIBLE );
                            previousURL = processURL(urlField.get("value"));
                            ignoreInaccessibleURL = true;
                        }
                    }, function(err) {
                        if ( registry.byId("bookmarkURL").value !== previousURL )
                        {
                            ignoreInaccessibleURL = false;
                        }
                        if ( ignoreInaccessibleURL === true)
                        {
                            me.addBookmark();
                        }
                        else
                        {
                            me.displayError("bookmarkURL", i18n.TOOL_URL_INACCESSIBLE );
                            previousURL = processURL(urlField.get("value"));
                            ignoreInaccessibleURL = true;
                        }
                    });
                })
            }, this.addButton).startup();
        },

        displayError: function(fieldID, newMessage)
        {
            var field = registry.byId(fieldID);
            var originalValidator = field.validator;
            var oldMessage = field.get("invalidMessage");
            field.set("invalidMessage",newMessage);
            field.validator = function() {return false;};
            field.validate();
            field.focus();
            field.validator = originalValidator;
            field.set("invalidMessage", oldMessage);
        },

        addBookmark: function() {
            var name = registry.byId("bookmarkName").get("value");
            var url = processURL(registry.byId("bookmarkURL").get("value"));
            var me = this;
            var bookmarkProps = {
                    "id": name,
                    "type": "bookmark",
                    "name": name,
                    "url": decodeURI(url),
                    "icon": "images/tools/defaultBookmark_142x142.png"
            }; 
            toolbox.getToolbox().addBookmark(bookmarkProps).then(  function(tool) {
                // add tool to container
                registry.byId("toolIconContainer").addTool(tool);
                registry.byId("toolIconContainer").endEdit();
                registry.byId("toolIconContainer").startEdit();
                registry.byId('addBookmarkDialogId').hide();
                var nameField = registry.byId("bookmarkName");
                var urlField = registry.byId("bookmarkURL");
                nameField.reset();
                urlField.reset(); 
                ignoreInaccessibleURL = false;
                previousURL = "";
            }, function(err) {
                console.error("addBookmark.addBookmark error: ", err.message);
                if ( err.response.status === 409 )
                {
                    me.displayError("bookmarkName", i18n.TOOL_DUPLICATE );
                }
                else if ( err.response.status === 400)
                {
                          me.displayError("bookmarkName", lang.replace(i18n.TOOL_BADREQUEST, [err.response.data.message]));
                } else {
                    // some unexpected error so display err.message
                    me.displayError("bookmarkName", lang.replace(i18n.TOOL_BADREQUEST, [err.message]));
                }
                   
            });
        }
    });
});
