/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
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
