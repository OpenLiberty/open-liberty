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
/* jshint strict: false */
define([ "js/common/platform", "dojo/_base/declare", "dojo/dom", "dojo/dom-class", "dijit/form/Button", "dijit/layout/ContentPane", "dijit/registry",
    "dojo/i18n!jsExplore/nls/explorerMessages", "jsExplore/utils/ID"], function(platform, declare, dom, domClass, Button, ContentPane, registry, i18n, ID) {

  var ConfigureGraphsButton = declare("ConfigureGraphsButton", [ Button ], {
    constructor : function(params) { // resource, buttonType(Save, Cancel)
      this.resource = params[0];
      this.buttonType = params[1];
      if (params[2]) {
        this.dialog = params[2];
      }
      if (params[3]) {
        this.perspective = params[3];
      }
      this.id = this.perspective + this.resource.id + ID.getConfigureGraphsUpper() + this.buttonType + ID.getButtonUpper();
      this.baseClass = platform.getDeviceCSSPrefix() + "configureGraphs" + this.buttonType + "Button";
    },

    id : '',
    title : '', // set based on buttonType
    "aria-label" : '', // set same as title
    
    view : 'single',
    dialog : null,
    resource : null,
    perspective : '',
    buttonType : '', // Done, Save, Cancel

    postCreate : function() {
      this.set('class', 'configureGraphsButton');
      if (this.buttonType === "Save") {
        this.set("title", i18n.STATS_SHOW_HIDE_CONFIRM);
        this.set("label", i18n.STATS_SHOW_HIDE_CONFIRM);
      } else if (this.buttonType === "Done") {
        this.set("title", i18n.STATS_SHOW_HIDE_DONE);
        this.set("label", i18n.STATS_SHOW_HIDE_DONE);
      } else if (this.buttonType === "Cancel") {
        this.set("title", i18n.STATS_SHOW_HIDE_CANCEL);
        this.set("label", i18n.STATS_SHOW_HIDE_CANCEL);
      }
    },

    onClick : function() {

      this.resetEditPanes();

      // If Save button, persist the data
      // else if Cancel, rebuild from previous saved data
      if ((this.buttonType === "Save" || this.buttonType == "Done") && this.dialog) {
        this.dialog.saveGraphData();
      } else if (this.buttonType === "Cancel" && this.dialog) {
        this.dialog.resetGraphs();
      }
    },

    resetEditPanes: function() {
      // Tell graphContainer to leave edit mode
      var resourceGraphsContainer = registry.byId(this.perspective + this.resource.id + ID.dashDelimit(ID.getStatsUpper(), ID.getContentPaneUpper()));
      if (resourceGraphsContainer) {
        resourceGraphsContainer.endEdit();
      }

      // Get rid of the button's parent pane and remove the dialog 
      // from view when either Save or Cancel is clicked and restore 
      // the view as it was before
      if (this.dialog && this.dialog.domNode && this.dialog.style !== "display:none") {
        this.dialog.set("style", "display:none");
      }
      var saveCancelBarPane = registry.byId(this.perspective + ID.underscoreDelimit(this.resource.id, ID.getSaveCancelBar()));
      if (saveCancelBarPane) {
        saveCancelBarPane.set("style", "display:none");
      }
      // redisplay the pane with the edit button
      dom.byId(this.perspective + this.resource.id + ID.getShowHideGraphsButtonPane()).style.display = "block";
    }

  });
  return ConfigureGraphsButton;

});