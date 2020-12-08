/* jshint strict: false */
define(["dojo/_base/declare",
        "dojo/_base/lang", 
        "dijit/_Widget", 
        "dijit/_TemplatedMixin",
        "dijit/_WidgetsInTemplateMixin",
        "dijit/Dialog",
        "dojo/text!./templates/messageDialog.html", 
        "dojo/i18n!./nls/widgetsMessages",
        "dojo/on",
        "dojo/dom-style",
        "dojo/domReady!"],
        function(declare,
            lang,
            Widget,
            TemplatedMixin,
            WidgetsInTemplateMixin,
            Dialog,
            template,
            i18n,
            on,
            domStyle) {
  // summary:
  //    A modal message dialog Widget.
  // description:
  //    Pops up a modal message dialog window.
  // example:
  //  var msgDialog = new MessageDialog({
  //      type: "info", "warn", "error"(default)
  //    title: i18n.TOOLBOX_REMOVE_TITLE, 
  //    message: lang.replace(i18n.TOOLBOX_REMOVE_MESSAGE, [item.label]),
  //    okButtonLabel: i18n.TOOLBOX_BUTTON_OK
  //    okFunction: function(){
  //      // handle the action if any is necessary
  //    }
  //  });
  //  msgDialog.placeAt(win.body());
  //  msgDialog.startup();
  //  msgDialog.show();

  return declare("MessageDialog", [Dialog], {
    constructor: function() {
    },

    content: "default",
    messageText: "default",
    additionalText: "",
    okButtonLabel: i18n.TOOLBOX_BUTTON_GO_TO,
    messageDialogIcon: '',
    displayXButton : true,
    type: "error",
    okFunction: undefined,
    displayOkButton : true,

    // called when the close button is clicked
    hide: function() {
      this.inherited(arguments).then(lang.hitch(this, function(){
        this.destroyRecursive();
      }));
    },

    postCreate: function(){
      this.inherited(arguments);

      var me = this;

      // build the content from the template and inputs
      var iconTitle = '';
      // if there is no icon, then there should not be any title for the icon
      if (me.get("messageDialogIcon").length !== 0) {
        iconTitle = '<title>' + me.get("messageText") + '</title>';
      }
      
      var contents = new (declare([Widget, TemplatedMixin, WidgetsInTemplateMixin], {
        templateString: template,
        messageText: me.get("messageText"),
        additionalText: me.get("additionalText"),
        okButtonLabel: me.get("okButtonLabel"),
        messageDialogIcon: me.get("messageDialogIcon"),
        messageDialogIconTitle: iconTitle
      }))();
      contents.startup();

      this.set("content", contents);
      if (!this.displayXButton) {
        this.set("class", "messageDialog disableX");
      } else {
        this.set("class", "messageDialog");
      }
      
      if(! this.displayOkButton) {
        domStyle.set(contents.okButton, 'display', 'none');
      } else {
        var okSignal = on(contents.okButton, "click", function(){
          // remove listener after first event
          okSignal.remove();
          // call ok function
          if (me.okFunction) {
            me.okFunction();
          }
          me.destroyRecursive();
        });
      }
    }

  });
});
