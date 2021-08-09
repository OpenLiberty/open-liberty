/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/* jshint strict: false */
define([
        "js/common/platform",
        "js/widgets/YesNoDialog",
        "jsExplore/widgets/graphs/ConfigureGraphsButton",
        "jsExplore/widgets/graphs/ShowGraphsDialog",
        "./TimeSelector",
        "dojo/_base/declare", 
        "dojo/_base/lang",
        "dojo/_base/window",
        "dojo/dom",
        "dojo/dom-construct",
        "dojo/dom-style",
        "dojo/on",
        "dijit/layout/ContentPane",
        "dijit/_TemplatedMixin",
        "dijit/_WidgetsInTemplateMixin",
        "dijit/registry",
        "dijit/form/Button",
        "dojo/i18n!jsExplore/nls/explorerMessages",
        "jsExplore/utils/ID",
        "jsExplore/resources/utils",
        "jsShared/utils/imgUtils",
        "dojo/text!./templates/GraphContainer.html"
        ], function(
                platform,
                YesNoDialog,
                ConfigureGraphsButton,
                ShowGraphsDialog,
                TimeSelector,
                declare,
                lang,
                win,
                dom,
                domConstruct,
                domStyle,
                on,
                ContentPane,
                _TemplatedMixin,
                _WidgetsInTemplateMixin,
                registry,
                Button,
                i18n,
                ID,
                utils,
                imgUtils,
                template
            ){

    var GraphContainer = declare("GraphContainer", [ ContentPane, _TemplatedMixin, _WidgetsInTemplateMixin ], {

        templateString: template,
        id : "",
        warningId : "",
        label : i18n.STATS,
        resource : null,
        resourceId : "",
        resourceIdStatsContentPane : "",
        resourceIdShowHideGraphsButtonPane : "",
        resourceIdSaveCancelBar : "",
        resourceIdSaveCancelButtonArea : "",
        resourceIdGraphsPane : "",
        style : 'overflow:visible; display:block; height:100%; width:100%; position:relative;',
        timeSelector : null,
        isShowing : false,
        isEditing : false,

        postMixInProperties : function() {
          this.id = this.perspective + this.resource.id + ID.dashDelimit(ID.getStatsUpper(), ID.getContentPaneUpper());
          this.resourceId = this.perspective + this.resource.id;
          this.warningId = ID.underscoreDelimit(this.resourceId, ID.getGraphWarningMessage(), ID.getAppInstStatsPaneUpper());
          if (this.resource.type == 'server' || this.resource.type === 'standaloneServer') {
            this.warningId = ID.underscoreDelimit(this.resourceId, ID.getGraphWarningMessage(), ID.getServerStatsPaneUpper());
          }
          this.resourceIdStatsContentPane = ID.dashDelimit(this.resourceId + ID.getStatsUpper(), ID.getContentPaneUpper());
          this.resourceIdShowHideGraphsButtonPane = this.resourceId + ID.getShowHideGraphsButtonPane();
          this.resourceIdSaveCancelBar = ID.underscoreDelimit(this.resourceId, ID.getSaveCancelBar());
          this.resourceIdSaveCancelButtonArea = ID.underscoreDelimit(this.resourceId, ID.getSaveCancelButtonArea());
          this.resourceIdGraphsPane = ID.dashDelimit(this.resourceId, ID.getGraphsPaneUpper());
        },

        postCreate : function() {
          this.inherited(arguments);

// DISABLE_ANALYTICS
//          if (!this.timeSelector && (this.resource.type === 'server' || this.resource.type === 'standaloneServer' || this.resource.type === 'cluster' || this.resource.type === 'host')) {
//            // Always create and let showGraphsDialog figure out when to display it
//            this.timeSelector = new TimeSelector({resource: this.resource, perspective: this.perspective});
//            this.addChild(this.timeSelector);
//          }

          var resourceType = "server";
          if (this.resource.type === "appOnServer") {
            resourceType = "app";
          }
          this.__createEditButtonPane(this.resource, resourceType);

          on(registry.byId(ID.getBreadcrumbController()), "selectChild", lang.hitch(this, function(page){
            // Make sure not using href to load monitor tab directly (this triggers a breadcrumb click after 3 onShow's)
            if (page.id !== ID.dashDelimit(ID.getObjectView(), this.resource.id)) {
              this.__checkExit();
              this.isShowing = false;
            }
          }));

        },

        onShow : function() {
          this.isShowing = true;
          this.startPollingOfAllActiveGraphs();
        },

        // only called on switching tabs
        onHide : function() {
          this.__checkExit();
          this.isShowing = false;
          this.stopPollingOfAllActiveGraphs();
        },

        // Go through all the displayed graphs and put them in edit mode (display the minus icon for delete)
        startEdit : function() {
          this.isEditing = true;
          this.graphsPane.getChildren().forEach(function(graph){
            if (graph.id.indexOf(ID.getUnderscore()+ID.getGraphWarningMessage()+ID.getUnderscore()) === -1) {
              graph.startEdit();
            }
          });
        },

        // Go through all the displayed graphs and end edit mode (display the action button)
        endEdit : function() {
          this.isEditing = false;
          this.graphsPane.getChildren().forEach(function(graph){
            if (graph.id.indexOf(ID.getUnderscore()+ID.getGraphWarningMessage()+ID.getUnderscore()) === -1) {
              graph.endEdit();
            }
          });
        },
        
        startPollingOfAllActiveGraphs : function() {
          var showHideDialog = registry.byId(this.perspective + ID.getShowGraphsDialogId());
          showHideDialog.turnOnGraphs();
        },
        
        stopPollingOfAllActiveGraphs : function() {
          var showHideDialog = registry.byId(this.perspective + ID.getShowGraphsDialogId());
          showHideDialog.turnOffGraphs();
        },

        // TODO: This needs to be put in the template. At least the logic is now in the right place.
        __createEditButtonPane : function(resource, type) {
          var showHideDialog = this.__createSelectionDialog(resource, type);
//          var doneButton = new ConfigureGraphsButton([resource, "Done", showHideDialog]);
          var saveButton = new ConfigureGraphsButton([resource, "Save", showHideDialog, this.perspective]);
          domConstruct.place(saveButton.domNode, this.saveCancelButtonArea);
          var cancelButton = new ConfigureGraphsButton([resource, "Cancel", showHideDialog, this.perspective]);
          domConstruct.place(cancelButton.domNode, this.saveCancelButtonArea);
          this.addChild(showHideDialog, 2);

          var editButtonId = this.perspective + resource.id + ID.getShowHideGraphsButton();
          var editButton = new Button({
            id : editButtonId,
            value: editButtonId,
            title : i18n.STATS_SHOW_HIDE_BUTTON_TITLE,
            label : '<span style="display:none;">' + i18n.STATS_SHOW_HIDE_BUTTON_TITLE + '</span>',
            "aria-label" : i18n.STATS_SHOW_HIDE_BUTTON_TITLE,
            "class": "showGraphsButton",
            iconClass : "showGraphsButtonIcon",
            label : "<span style='visibility:hidden;'>" + i18n.STATS_SHOW_HIDE_BUTTON_TITLE + "</span>",
            onClick : lang.hitch(this, function(){
              //var parentPane = registry.byId(this.perspective + resource.id + "Stats-ContentPane");
              // first hide the pane with the button
              dom.byId(this.perspective + resource.id + ID.getShowHideGraphsButtonPane()).style.display = "none";

              // check for the dialog
              var showHideDialog = this.__createSelectionDialog(resource, type);
              showHideDialog.checkForAvailableOptions(false);   // don't rebuild the graphs

              // set the saveCancelBar and graph selection pane to display
              this.saveCancelBar.set("style", "display:block");
              registry.byId(this.perspective + ID.getShowGraphsDialogId()).set("style", "display:block");
              
              // place focus on 'Cancel'
              var cancelButton = registry.byId(this.perspective + resource.id + ID.getConfigureGraphsCancelButtonUpper());
              if (cancelButton && cancelButton.focus) {
                cancelButton.focus();
              }

              // start edit mode
              this.startEdit();
            })
          });
          this.editBar.addChild(editButton);
        },

        __createSelectionDialog : function(resource, type) {
          // check for the dialog
          // If it is already there from a previous resource, delete it and close the edit
          var showHideDialog = registry.byId(this.perspective + ID.getShowGraphsDialogId());
          if (showHideDialog && showHideDialog.resource.id !== resource.id) {
//            var graphButton = registry.byId(showHideDialog.resource.id + "ConfigureGraphsCancelButton");
//            if (graphButton) {
//              graphButton.onClick();
//            } else {
//              // just in case, look for the graphsContainer and tell it to endEdit
//              var resourceGraphsContainer = registry.byId(showHideDialog.resource.id + "Stats-ContentPane");
//              if (resourceGraphsContainer) {
//                resourceGraphsContainer.endEdit();
//              }
//            }
            showHideDialog.destroyRecursive();
            showHideDialog = null;
          }
          if (!showHideDialog) {
            showHideDialog = new ShowGraphsDialog({
              type : type,
              resource : resource,
              perspective : this.perspective,
              style : 'display:none;',
              doLayout: false
            });
            this.addChild(showHideDialog, 2);
            var graphButton = registry.byId(this.perspective + resource.id + ID.getConfigureGraphsSaveButtonUpper());
            if (graphButton) {
              graphButton.dialog = showHideDialog;
            }
            graphButton = registry.byId(this.perspective + resource.id + ID.getConfigureGraphsCancelButtonUpper());
            if (graphButton) {
              graphButton.dialog = showHideDialog;
            }
          } else {
            // already existed so make sure it is expanded
            showHideDialog.show();
          }
          return showHideDialog;
        },

        __checkExit : function() {
          if (this.isShowing) {
            var showHideDialog = registry.byId(this.perspective + ID.getShowGraphsDialogId());
            // if there are unsaved changes, display the dialog i18n.GRAPH_CONFIG_NOT_SAVED
            if (showHideDialog.modified) {
              var widgetId = ID.getUnsavedConfigYesNoDialog();
              utils.destroyWidgetIfExists(widgetId);
              var stopDialog = new YesNoDialog({ 
                id: widgetId,
                title: i18n.GRAPH_CONFIG_NOT_SAVED_TITLE,
                descriptionIcon: imgUtils.getSVGSmallName('status-alert'),
                description: i18n.GRAPH_CONFIG_NOT_SAVED_DESCR,
                message: i18n.GRAPH_CONFIG_NOT_SAVED_MSG,
                destructiveAction: 'no',
                yesFunction: lang.hitch(this, function() {
                  // click the save button
                  var saveButton = registry.byId(this.perspective + this.resource.id + ID.getConfigureGraphsSaveButtonUpper());
                  saveButton.onClick();
                }),
                noFunction: lang.hitch(this, function() {
                  // click the cancel button
                  var cancelButton = registry.byId(this.perspective + this.resource.id + ID.getConfigureGraphsCancelButtonUpper());
                  cancelButton.onClick();
                })
              });
              stopDialog.placeAt(win.body());
              stopDialog.startup();
              stopDialog.show();
            } else {
              // close it in case it was open
              var cancelButton = registry.byId(this.perspective + this.resource.id + ID.getConfigureGraphsCancelButtonUpper());
              cancelButton.resetEditPanes();
            }
          }
        }
    });

    return GraphContainer;

});
