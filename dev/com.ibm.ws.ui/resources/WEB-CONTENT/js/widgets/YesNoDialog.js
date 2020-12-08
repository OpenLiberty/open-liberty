/* jshint strict: false */
define(["dojo/_base/declare",
        "dojo/dom-class",
        "dijit/_Widget", 
        "dijit/_TemplatedMixin",
        "dijit/_WidgetsInTemplateMixin",
        "dijit/Dialog",
        "dojo/text!./templates/YesNoDialog.html", 
        "dojo/i18n!./nls/widgetsMessages",
        "dojo/_base/lang",
        "dojo/on",
        "dojo/domReady!"],
        function(
            declare,
            domClass,
            Widget,
            TemplatedMixin,
            WidgetsInTemplateMixin,
            Dialog,
            template,
            i18n,
            lang,
            on) {
  // summary:
  //		A modal Yes / No dialog Widget. No is the default tab order.
  // description:
  //		Pops up a modal confirmation dialog window.
  // example:
  //	var stopDialog = new YesNoDialog({ 
  //  title: i18n.STOP_AC_TITLE,
  //  descriptionIcon: 'images/message-alert-T.png',
  //  description: '',
  //  message: lang.replace(i18n.STOP_AC_MESSAGE, [ thisResource.name ]),
  //  yesFunction: function() {
  //    // handle the action
  //  },
  //  noFunction: function() {
  //    // handle the action
  //  }
  //	});
  //	stopDialog.placeAt(win.body());
  //	stopDialog.startup();
  //	stopDialog.show();

  return declare("YesNoDialog", [Dialog], {
    constructor: function() {
    },

    baseClass: "yesNoDialog acDialog",
    title: 'default',
    descriptionIcon: '',
    description: 'default',
    message: 'default',

    destructiveAction: 'yes',
    destructiveActionClass: 'yesNoDestructiveAction',
    defaultActionClass: 'yesNoDefaultAction',

    yesButtonText: i18n.YES,
    yesButtonAriaLabel: undefined,
    yesFunction: undefined,
    yesButtonClass: 'yesNoYesButton',
    yesButtonTabIndex: 2,

    noButtonText: i18n.NO,
    noButtonAriaLabel: undefined,
    noFunction: undefined,
    noButtonClass: 'yesNoNoButton',
    noButtonTabIndex: 1,

    postCreate: function() {
      if (!this.yesButtonAriaLabel) {
        this.yesButtonAriaLabel = lang.replace(i18n.YES_BUTTON_LABEL,[this.title]);
      }
      if (!this.noButtonAriaLabel) {
        this.noButtonAriaLabel = lang.replace(i18n.NO_BUTTON_LABEL,[this.title]);
      }
      this.inherited(arguments);

      domClass.add(this.containerNode, "acDialogContentPane");
      domClass.add(this.titleBar, "acDialogTitleBar");
      domClass.add(this.titleNode, "acDialog_title");

      var me = this;
      // build the content from the template and inputs
      console.log("me: ", this);
      console.log("id: ", this.get("id"));
      var yesButtonClass = me.get("yesButtonClass");
      var noButtonClass = me.get("noButtonClass");
      if (me.destructiveAction === 'yes') {
        yesButtonClass = yesButtonClass + ' ' + me.destructiveActionClass;
        noButtonClass = noButtonClass + ' ' + me.defaultActionClass;
      } else {
        yesButtonClass = yesButtonClass + ' ' + me.defaultActionClass;
        noButtonClass = noButtonClass + ' ' + me.destructiveActionClass;
        me.yesButtonTabIndex = 1;
        me.noButtonTabIndex = 2;
      }
      var contents = new (declare([Widget, TemplatedMixin, WidgetsInTemplateMixin], {
        templateString: template,

        descriptionIcon: me.get("descriptionIcon"),
        description: me.get("description"),
        message: me.get("message"),
        destructiveAction : me.get("destructiveAction"),

        yesButtonText: me.get("yesButtonText"),
        yesButtonAriaLabel: me.get("yesButtonAriaLabel"),
        yesButtonClass: yesButtonClass,
        yesButtonTabIndex: me.get("yesButtonTabIndex"),

        noButtonText: me.get("noButtonText"),
        noButtonAriaLabel: me.get("noButtonAriaLabel"),
        noButtonClass: noButtonClass,
        noButtonTabIndex: me.get("noButtonTabIndex"),

        confirmButtonAriaLabel: this.title
      }))();
      contents.startup();

      this.set("content", contents);

      var removeListeners = function() {
        me.yesSignal.remove();
        me.noSignal.remove();
      };

      me.yesSignal = on(contents.yesButton, "click", function(){
        // remove listener after first event
        removeListeners();

        // call ok function
        if (me.yesFunction) {
          try {
            me.yesFunction();
          } catch(e) {
            console.log('Failed to invoke the yesFunction', e);
          }
        }

        // Clean up the dialog DOM
        me.destroyRecursive();
      });

      me.noSignal = on(contents.noButton, "click", function(){
        // remove listener after first event
        removeListeners();

        // call ok function
        if (me.noFunction) {
          try {
            me.noFunction();
          } catch(e) {
            console.log('Failed to invoke the noFunction', e);
          }
        }

        // Clean up the dialog DOM
        me.destroyRecursive();
      });
    }
  });
});
