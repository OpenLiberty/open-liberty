/* jshint strict: false */
define([ "dojo/_base/declare", "dojo/_base/lang", "dojo/dom-class", "dijit/_Widget", "dijit/_TemplatedMixin",
    "dijit/_WidgetsInTemplateMixin", "dijit/Dialog", "dojo/text!./templates/confirmDialog.html", "dojo/i18n!./nls/widgetsMessages",
    "dojo/on", "dojo/query", "dojo/dom", "js/common/utils", "dojo/dom-style", "dojo/dom-attr"], function(declare, lang, domClass, Widget, TemplatedMixin, WidgetsInTemplateMixin, Dialog,
    template, i18n, on, query, dom, utils, domStyle, domAttr) {
  // summary:
  //      A modal confirmation dialog Widget.
  // description:
  //      Pops up a modal confirmation dialog window.
  // example:
  //  var removeToolDialog = new ConfirmDialog({ 
  //      title: lang.replace(i18n.TOOLBOX_REMOVE_TITLE, [item.label]), 
  //      confirmMessage: lang.replace(i18n.TOOLBOX_REMOVE_MESSAGE, [item.label]),
  //      confirmButtonLabel: i18n.TOOLBOX_BUTTON_REMOVE
  //      okFunction: function(){
  //          // handle the action
  //      }
  //  });
  //  removeToolDialog.placeAt(win.body());
  //  removeToolDialog.startup();
  //  removeToolDialog.show();

  return declare("ConfirmDialog", [ Dialog ], {
      constructor : function() {
      },
      baseClass : "confirmDialog acDialog",
      content : "default",
      confirmDescriptionIcon : '',
      confirmDescription : '',
      confirmMessage : "default",
      helpLinkURL : '',
      helpLinkImg : '',
      helpLinkAriaLabel : i18n.CONFIRM_DIALOG_HELP,
      contentNode : [],
      hasOptions : false,
      displayXButton : true,

      confirmButtonId : 'confirmOkButton',
      confirmButtonLabel : i18n.TOOLBOX_BUTTON_OK,
      cancelButtonLabel : i18n.TOOLBOX_BUTTON_CANCEL,
      redButton : false,
      okFunction : undefined,
      confirmHelpLabel : i18n.CONFIRM_DIALOG_HELP,

      // called when the close button is clicked
      hide : function() {
        this.inherited(arguments).then(lang.hitch(this, function() {
          this.destroyRecursive();
        }));
      },

      buildRendering : function() {
        this.inherited(arguments);
        var me = this;
        var contents = new (declare([ Widget, TemplatedMixin, WidgetsInTemplateMixin ], {
            templateString : template,

            confirmDescriptionIcon : me.get("confirmDescriptionIcon"),
            confirmDescription : me.get("confirmDescription"),
            confirmMessage : me.get("confirmMessage"),
            confirmHelpLabel : me.get("confirmHelpLabel"),

            confirmButtonId : me.get("confirmButtonId"),
            confirmButtonLabel : me.get("confirmButtonLabel"),
            cancelButtonLabel : me.get("cancelButtonLabel"),

            helpLinkURL : me.get("helpLinkURL"),
            helpLinkImg : me.get("helpLinkImg"),
            helpLinkAriaLabel : me.get("helpLinkAriaLabel"),

            confirmButtonAriaLabel : this.title
        }))();
        this.contentNode = contents;
      },
      toggleBreakAffinity : function(evt) {
        // evt is true or false
        if (evt) {
          alert("true1");
        } else {
          alert("false1");
        }
      },

      postCreate : function() {
        this.inherited(arguments);

        domClass.add(this.containerNode, "acDialogContentPane");
        domClass.add(this.titleBar, "acDialogTitleBar");
        domClass.add(this.titleNode, "acDialog_title");
        if (!this.displayXButton) {
          domClass.add(this.domNode, 'disableX');
        }
        
        // For accessibility, 'heading' must have 'aria-level' with it; current version of dojo seems to only have 'level'
        var level = domAttr.get(this.titleNode, "level");
        domAttr.set(this.titleNode, "aria-level", level);

        var me = this;
        // build the content from the template and inputs
        var contents = this.contentNode;
        contents.startup();

        this.set("content", contents);
        var pnode = dom.byId(me.get("id"));
        // cannot use dom.byId directly because we have connectionLostDialog created and it is hiding.
        // need to use query and dom.byId to limit the scope
        // set css style button to be red if button label is Remove
        if ((i18n.TOOLBOX_BUTTON_REMOVE === me.confirmButtonLabel) || (this.redButton === true)) {
          var cnode = query(".confirmOkButton", pnode)[0];
          cnode.setAttribute("style", "background-color:#A90F22;");
        }

        var lnode = query(".confirmDescriptionIcon", pnode)[0].parentNode;
        var mnode = query(".confirmDescription", pnode)[0];
        var style = domStyle.get(this.titleNode);
        var titleWidthinPx = utils.getTextWidth(me.title, style.fontSize, style.fontWeight, style.fontFamily);
        // console.log("Detected dialog title width is " + titleWidthinPx + "px");
        var dialogWidth = 370;
        if (this.overrideWidth && this.overrideWidthPx){
          dialogWidth = this.overrideWidthPx;
        }
        if ( titleWidthinPx + 80 > dialogWidth) {
          // requirement is to have 40px white space between the title and the dialog on both left and right side
          dialogWidth = titleWidthinPx + 80;
        }
                  
        pnode.style.width = dialogWidth + "px";
        //var mainWidth=dialog content width; in default mode, the dialog width is 370 px, icon div width is 77px, so the reminder is 370-77 = 263;
        var mainWidth = dialogWidth - 77; 
        // console.log("Calculated mainWidth is " + mainWidth);

        // change the left div width if no icon is specified
        // default confirm description icon div is 77 px, 30px + image width 32 px + 15 px, so it is 77 px
        // if no description icon, the we change it to 30 px.  The remainder is defaulted 370 - 30 - 30 = 310;       
        if ("" === me.confirmDescriptionIcon) {
          //console.log("set confirmation dialog left div width to 30px, and the main div width to 263+47 = 310px");
          domStyle.set(lnode, "width", "30px");
          //console.log("remove confirmation dialog icon image since it is empty");
          lnode.innerHTML = "&nbsp;";
          //mainWidth = 310; in default 370 px dialog, if no description icon, the main div width is 310 (370-30-30);
          //                                                                   30 for both left and right "margin"    
          mainWidth = dialogWidth - 60;
          domStyle.set(mnode, "width", mainWidth + "px");
        }
        else {
          mainWidth = mainWidth - 30;   // 30px reserved as "margin" on right
          domStyle.set(mnode, "width", mainWidth + "px");
        }
        
        // the help link icon, if help link is not specified, hide the help link div
        var mnodeSubDivNode = mnode.children[0];
        if ("" === me.helpLinkURL || "" === me.helpLinkImg || "" === me.helpLinkAriaLabel) {
          var mnodeHelpDivNode = mnodeSubDivNode.children[1];
          domStyle.set(mnodeHelpDivNode, "display", "none");
        } else {
          // make room for the help link icon within the mnode.
          var mnodeDescDivNode = mnodeSubDivNode.children[0];  
          mainWidth = mainWidth - 18 - 15;  // 18 is icon width, 15 is left padding for icon.  
          domStyle.set(mnodeDescDivNode, "width", mainWidth + "px");
        }

        // hide the options div if no options node
        if (false === this.hasOptions) {
          var mnodeOptionsNode = mnode.children[1];
          mnodeOptionsNode.setAttribute("style", "display:none;");
        }

        me.okSignal = on(contents.okButton, "click", function() {
          // remove listener after first event
          me.okSignal.remove();
          // call ok function
          if (me.okFunction) {
            me.okFunction();
          }
          me.destroyRecursive();
        });
      }
  });
});
