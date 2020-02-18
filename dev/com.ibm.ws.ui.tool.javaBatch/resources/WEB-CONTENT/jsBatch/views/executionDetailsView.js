/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
define(['dojo/_base/lang', 'dojo/Deferred', 'dijit/registry', 'dojo/on',
        'dijit/layout/BorderContainer', 'dijit/layout/ContentPane',
        'jsBatch/utils/restUtils',
        'jsBatch/utils/utils',
        'jsShared/utils/toolData',
        'jsShared/utils/userConfig',
        'dojo/i18n!jsBatch/nls/javaBatchMessages',
        'jsBatch/utils/ID',
        'dojo/json',
        'jsBatch/widgets/ExecutionOverviewPane',
        'jsBatch/widgets/StepsDetailGrid',
        'dojo/domReady!'],

function(lang, Deferred, registry, on,
         BorderContainer, ContentPane,
         restUtils, utils, toolData, userConfig, i18n, ID, JSON,
         ExecutionOverviewPane, StepsDetailGrid) {

  'use strict';

  /**
   * Controls the view for the Execution Details page.
   *
   * This page displays information on a specific execution of a job instance.  The information
   * includes data on the execution, the job instance associated with the execution, and details
   * about the execution's steps and partitions.
   *
   */

  return {
    viewId: ID.EXECUTION_DETAIL_PANE,

    // Page part references
    eop: null,                  // ExecutionOverviewPane
    stepsDetailGrid: null,      // StepsDetailGrid

    // Data
    executionId: null,
    jobInstanceId: null,
    executionDetails: null,
    jobInstanceDetails: null,
    stepsDetails: null,

    updateView : function(parms) {
      // Re-initialize the data fields....
      this.executionId = this.jobInstanceId = this.executionDetails = this.jobInstanceDetails = this.stepsDetails = null;
      var view = registry.byId(this.viewId);

      console.log("Attempting to view execution " + parms.executionId);
      this.executionId = parms.executionId;
      this.executionDetails = parms.executionDetails;
      this.jobInstanceDetails = parms.jobInstanceDetails;
      this.stepsDetails = parms.stepsDetails;

      var me = this;

      var updateBreadCrumb = function(){
        breadcrumbContainer.getChildren().forEach(lang.hitch(this, function(child){
          if(child.id === me.viewId){
            breadcrumbContainer._showChild(child);
          } else {
            breadcrumbContainer._hideChild(child);
          }
          child.resize();

        var executionDetailView = registry.byId(me.viewId);

          on(window, 'resize', function() {
            executionDetailView.resize();
          });

        }));
      };

      var breadcrumbContainer = registry.byId(ID.BREADCRUMB_CONTAINER);
      if (view) {
        console.log("View already exists. Reset for execution: " + parms.executionId);
          this.eop.updateData(this.executionId, this.executionDetails, this.jobInstanceDetails);
          this.stepsDetailGrid.updateTable(this.executionId, this.stepsDetails);
          updateBreadCrumb();
      } else {
        // Initiate the persistence configuration
        userConfig.init("com.ibm.websphere.appserver.adminCenter.tool.javaBatch");
        userConfig.load(lang.hitch(this, function(response){
          var executionDetailPage = this.initPage(this.executionId, null, null, null, response);
          breadcrumbContainer.addChild(executionDetailPage);
          updateBreadCrumb();
        }), lang.hitch(this, function(err){
          console.log("Unable to load any persistence data for the execution details grid.");
          var executionDetailPage = this.initPage(this.executionId, null, null, null, null);
          breadcrumbContainer.addChild(executionDetailPage);
          updateBreadCrumb();
        }));
      }

      breadcrumbContainer.getChildren().forEach(lang.hitch(this, function(child){
        if(child.id === this.viewId){
          breadcrumbContainer._showChild(child);
        } else {
          breadcrumbContainer._hideChild(child);
        }
        child.resize();
      }));
    },

    initPage : function(executionId, executionDetails, jobInstanceDetails, stepsDetails, persistedData) {

      var executionDetailCP = new ContentPane({
        id: this.viewId,
        executionId: executionId,
        title: "Execution Details", //provides aria-label upon init, which does not get updated even if changed in updateView
        content: ' ',
        baseClass: 'topDetailContentPane',
        'class': 'executionDetailsSizeInfo'
      });

      var executionBorderContainer = new BorderContainer({
        id: ID.EXECUTION_BORDER_CONTAINER
      });

      var mainDetailContentPane = new ContentPane({
        id: ID.EXECUTION_MAIN_CONTENT_PANE,
        label: "Execution " + executionId + " details",
        region: 'top',
        doLayout: false,
        baseClass: 'detailsContentPane',
        'class': 'scrollableContentPane',
        splitter: true
      });
      executionBorderContainer.addChild(mainDetailContentPane);

      this.eop = new ExecutionOverviewPane({executionId: this.executionId,
                                           executionDetails: this.executionDetails,
                                           jobInstanceDetails: this.jobInstanceDetails
                                          });
      this.eop.placeAt(mainDetailContentPane);

      var stepsDetailContentPane = new ContentPane({
        id: ID.EXECUTION_STEP_CONTENT_PANE,
        label: "Steps details for execution " + executionId,
        region: 'center',
        doLayout: false,
        baseClass: 'detailsContentPane',
        'class': 'scrollableContentPane',
        splitter: true
      });

      this.stepsDetailGrid = new StepsDetailGrid({executionId: this.executionId,
                                                 stepsDetails: this.stepsDetails,
                                                 persistedData: persistedData
                                                });
      executionBorderContainer.addChild(stepsDetailContentPane);
      this.stepsDetailGrid.stepsDetailGrid.placeAt(stepsDetailContentPane);
      this.stepsDetailGrid.stepsDetailGrid.startup();

      executionDetailCP.addChild(executionBorderContainer);

      return executionDetailCP;

    },

    closeView : function() {
    }
  };

 });
