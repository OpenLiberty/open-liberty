/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
define([
        "dijit/layout/BorderContainer", 
        "dijit/layout/ContentPane",
        "dijit/_TemplatedMixin",
        "dijit/_WidgetsInTemplateMixin",
        "dojo/text!./templates/ResourceFilter.html",
        "dojo/dom-style",
        "dojo/parser",
        "dojo/_base/declare", 
        "js/common/platform", 
        "dojo/_base/lang",
        "dojo/i18n!jsExplore/nls/explorerMessages",
        "jsShared/utils/imgUtils", 
        "dojo/domReady!" ], function(
            BorderContainer, 
            ContentPane,
            _TemplatedMixin,
            _WidgetsInTemplateMixin,
            template,
            domStyle, 
            parser,
            declare, 
            platform, 
            lang, 
            i18n, 
            imgUtils
        ) {
  
  /* Create a ResourceFilter widget */
  return declare("ResourceFilter", [ContentPane, _TemplatedMixin, _WidgetsInTemplateMixin], {
    constructor : function(){
      status = this.status;
      number = this.number;
    },
    templateString: template,
    iconId: this.icon,
    aria: this.number + " " + this.status,
    postCreate : function() {
      this.inherited(arguments);
      this.buildResourcesView();
    },
    buildResourcesView : function() {
      this.iconNode.innerHTML = imgUtils.getSVGSmall(this.icon);
    },
    //TODO: move CSS into separate file
    deselect : function() {
      this.set("style", "background-color: transparent; padding-bottom: 5px; border-bottom: 0px solid #C3C3C3;");
      this.selected = false;
    },
    select : function() {
      var me = this;
      if (platform.isPhone()) {
        me.set("style", "background-color: #EDEDED; border-bottom: 1px solid #4178BE;");
      } else {
        me.set("style", "background-color: #EDEDED; padding-bottom: 0px; border-bottom: 5px solid #4178BE;");
      }
      me.selected = true;
    },
    changeCount : function(count) {
      this.number = count;
      this.numberNode.textContent = count;
      // In order to get JAWS to read the information, we need to give it a role
      this.set("role", "button");
      // Also set the aria-label since the alert button for some reason says "Explore"
      this.set("aria-label", this.number + " " + this.status.replace(/"/g, "&quot;"));
    },
    startup : function() {
      this.inherited(arguments);
    }
  });
});