/* jshint strict: false */
define([
  "dojo/_base/declare",
  "dojo/_base/lang",
  "dijit/_Widget", 
  "dijit/_WidgetBase",
  "dijit/_TemplatedMixin",
  "js/widgets/ConfirmDialog",
  "dojox/mobile/ToggleButton",
  "dojo/query",
  "dojo/dom-construct",
  "dijit/_WidgetsInTemplateMixin",
  "dojo/text!./templates/BreakAffinity.html"
], function(declare, lang, _Widget, _WidgetBase, _TemplatedMixin, ConfirmDialog, ToggleButton, query, domConstruct, _WidgetsInTemplateMixin, template) {
  return declare([ConfirmDialog, _TemplatedMixin, _WidgetsInTemplateMixin], {
    // This dialog extends the ConfimDialog.  It has a options node.    
    constructor: function(args){
    },

    buildRendering: function(){
      var me = this;
      this.inherited(arguments);
      var targetNode = query("div[data-dojo-attach-point='optionsNode']", this.contentNode.domNode)[0];
      var optionsNode = new (declare([_Widget, _TemplatedMixin, _WidgetsInTemplateMixin], {
        templateString: template,
        question: me.get("question")
      }))();
      optionsNode.startup();
      domConstruct.place(optionsNode.domNode, targetNode);
    },
    
    postCreate: function(){ 
      this.inherited(arguments);
    }
  });
});
