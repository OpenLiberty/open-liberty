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
define([ 'dojo/_base/declare', 'dojo/_base/lang', 'jsShared/breadcrumb/BreadcrumbPane', 
  'jsBatch/widgets/JavaBatchBreadcrumbButton', 'dojo/i18n!jsBatch/nls/javaBatchMessages', 'jsBatch/utils/ID', 
  'jsShared/utils/utils', 'jsShared/breadcrumb/BreadcrumbSeparator', 'dojo/aspect',
  'dojo/dom-class', 'jsBatch/utils/viewToHash'],
    function(declare, lang, BreadcrumbPane, 
        JavaBatchBreadcrumbButton, i18n, ID, 
        utils, BreadcrumbSeparator, aspect,
        domClass, viewToHash) {

      'use strict';
      
      var JavaBatchBreadcrumbPane = declare('JavaBatchBreadcrumbPane', [ BreadcrumbPane ], {
          constructor : function(params) {
            // TODO: How is this textDir used?  Explore doesn't use this variable
            this.textDir = utils.getBidiTextDirectionSetting();
          },

          postCreate : function() {
            this.addDashboardButton();
          },
          
          addDashboardButton : function() {
            var dashboardButton = new JavaBatchBreadcrumbButton({
              id : ID.JAVA_BATCH_DASHBOARD_BUTTON,
              baseClass : 'breadcrumbDashboard',
              svgIconId : 'dashboard',
              svgIconClass : 'javaBatchBreadcrumbButton',
              title : i18n.DASHBOARD,
              label : i18n.DASHBOARD,
              showLabel : false,
              onClick : function() {
                // Go back to dashboard
                viewToHash.updateView('');
              }
            });
            this.addChild(dashboardButton);
          },
          
          addExecutionBreadcrumb : function(id) {
            var msg = lang.replace(i18n.BREADCRUMB_JOB_EXECUTION, [id]);
            var jobExecution = new JavaBatchBreadcrumbButton({
              id : ID.BREADCRUMB_JOBEXECUTION + id,
              baseClass : 'breadcrumbTextOnly', // Change to breadcrumb, if icon is needed
              // Below is how you add icons to breadcrumb, if ever needed
              //svgIconId : 'jobinst',
              //svgIconClass : 'javaBatchBreadcrumbButton',
              title : msg,
              label : msg,
              onClick : function() {
                viewToHash.updateView('jobexecutions/' + id);
              }
            });
            
            this.addChild(jobExecution);
          },
          
          addJobInstanceBreadcrumb : function(id) {
            var msg = lang.replace(i18n.BREADCRUMB_JOB_INSTANCE, [id]);
            var jobInstancesButton = new JavaBatchBreadcrumbButton({
              id : ID.BREADCRUMB_JOBINSTANCE + id,
              baseClass : 'breadcrumbTextOnly', // Change to breadcrumb, if icon is needed
              // Below is how you add icons to breadcrumb, if ever needed
              //svgIconId : 'jobinst',
              //svgIconClass : 'javaBatchBreadcrumbButton',
              title : msg,
              label : msg,
              onClick : function() {
                viewToHash.updateView('?jobInstanceId=' + id);
              }
            });
            this.addChild(jobInstancesButton);
          },
          
          addJobLogBreadcrumb : function(executionId, logFileName, page) {
            var msg = lang.replace(i18n.BREADCRUMB_JOB_LOG, [logFileName]);
            var jobInstancesButton = new JavaBatchBreadcrumbButton({
              id : ID.BREADCRUMB_JOBLOG + logFileName,
              baseClass : 'breadcrumbTextOnly', // Change to breadcrumb, if icon is needed
              // Below is how you add icons to breadcrumb, if ever needed
              //svgIconId : 'jobinst',
              //svgIconClass : 'javaBatchBreadcrumbButton',
              title : msg,
              label : msg,
              onClick : function() {
                viewToHash.updateView('joblogs/?jobexecution=' + executionId + '&page=' + page);
              }
            });
            this.addChild(jobInstancesButton);
          },
          
          setBreadcrumb : function(jobInstanceId, jobExecutionId, jobLogId, jobLogPageNumber) {
            this.resetBreadcrumbPane();
            this.addJobInstanceBreadcrumb(jobInstanceId);
            this.addSeparator();
            this.addExecutionBreadcrumb(jobExecutionId);
            
            if(jobLogId && jobLogPageNumber) {
              this.addSeparator();
              this.addJobLogBreadcrumb(jobExecutionId, jobLogId, jobLogPageNumber);
            }
            
            // Style the last breadcrumb differently from the other breadcrumb buttons
            var breadcrumbChildren = this.getChildren();
            var lastButton = breadcrumbChildren[breadcrumbChildren.length - 1];
            domClass.add(lastButton.domNode, "lastBreadCrumb");
            
            // Style the last separater to be two-toned due to the last breadcrumb button
            // being styled differently
            var lastSeparator = breadcrumbChildren[breadcrumbChildren.length - 2];
            lastSeparator.set("iconClass", "breadcrumbSeparatorSelected");
          },
          
          resetBreadcrumbPane : function() {
            this.destroyDescendants();
            this.addDashboardButton();
          },
          
          addSeparator : function() {
            var separator = new BreadcrumbSeparator();

            // There is a lot of inherited styling that are specifcally to make
            // the breadcrumb separator to work in explore tool.  The dependencies are 
            // overwhelming to untangle, so instead adding a class here that dictates 
            // and simplfies the styling of the breadcrumb separator for java batch tool only.
            domClass.add(separator.domNode, "javaBatchBreadcrumbSeparator");
            this.addChild(separator);
          }
          
      });
      return JavaBatchBreadcrumbPane;
    });