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
define([], function() {
'use strict';
var ID = {
    
    MAIN_CONTAINER : 'mainContainer',

    JAVA_BATCH :'javaBatch',
    BREADCRUMB_CONTAINER : 'breadcrumbContainer-id',
    LOADING_VIEW : 'loadingView',
    JOB_INSTANCE : 'jobInstanceId',
    APPLICATION_NAME : 'applicationName',
    INSTANCE_STATE : 'instanceState',
    LOADING_VIEW_STANDBY : 'loadingViewStandBy',
    CONTENT_PANE : 'contentPane',
    CENTER_CONTENT_PANE : 'javaBatch-contentPaneCenter',
    JAVA_BATCH_DASHBOARD_BUTTON : 'javaBatchDashboardButton',
    JAVA_BATCH_BREADCRUMB_BUTTON: 'javaBatch-breadcrumbButton',
    
    //Execution Details Page
    EXECUTION_DETAIL_PANE : 'execution-detailsPane',
    EXECUTION_BORDER_CONTAINER : 'execution-borderContainer',
    EXECUTION_MAIN_CONTENT_PANE : 'execution-mainContentPane',
    EXECUTION_STEP_CONTENT_PANE: 'execution-stepsContentPane',
    EXECUTION_ID : 'executionId',
    BATCH_STATUS : 'batchStatus',
    EXIT_STATUS : 'exitStatus',
    CREATE_TIME : 'createTime',
    START_TIME : 'startTime',
    END_TIME : 'endTime',
    LAST_UPDATE : 'lastUpdate',
    LAST_UPDATED_TIME: 'lastUpdatedTime',
    SERVER: 'server',
    SERVER_NAME: 'serverName',
    USER_DIR: 'userDir',
    HOST: 'host',
    JOB_PARAMETERS : 'jobParameters',
    ACTIONS: 'actions',
    LOG : 'log',
    EOP : 'EOP',
    EXECUTION_BATCH_STATUS : 'execution-batchStatus',
    VIEW_LOG_BUTTON : 'viewLogButton',
    JOB_INSTANCE_INSTANCE_STATE : 'jobInstance-instanceState',
    
    
    //Job Logs Page
    JOB_LOGS_PAGE : 'jobLogs-Page',
    DOWNLOAD_BOX : 'download_box',
    PAGE_NUMBER_BOX : 'pageNumber-Box',
    PAGE_NUMBER_TOTAL: 'pageNumber-Total',
    NEXT_BUTTON : 'next-button',
    PREVIOUS_BUTTON : 'previous-button',
    BOTTOM_BUTTON : 'bottom-button',
    TOP_BUTTON : 'top-button',
    DOWNLOAD_BUTTON : 'download-button',
    PAGE_NAVIGATION_BAR : 'pageNavigationBar',
    
    //Dashboard View
    DASHBOARD_VIEW : 'dashboardView',
    INSTANCE_ID : 'instanceId',
    EXECUTIONS_DROPDOWN : 'executionsDropDown',
    JOB_NAME : 'jobName',
    APP_NAME : 'appName',
    SUBMITTER : 'submitter',
    JOBINSTANCE_GRID : 'jobInstanceGrid',
    EXECUTION_GRID: 'executionGrid',
    STEP_EXECUTION_ID : "stepExecutionId",
    STEP_DETAIL_GRID : "stepsDetailGrid",
    STEP_NAME : 'stepName',
    COLUMN_SELECT : 'colSelect',
    JES_JOB_NAME: 'JESJobName',
    JES_JOB_ID: 'JESJobId',
    ACTION_BUTTON: 'actionButton',
    SEARCH_COMBINATION_ERROR_PANEL : 'search-combination-error-panel',
    SEARCH_COMBINATION_ERROR_PANEL_MSG : 'search-combination-error-panel-msg',
    JOB_STORE_ONLY_TOGGLE_ID : 'jobStoreOnlyToggle',
    GRID_LOADING_PANE : 'javaBatch-grid-loading-pane',
    
    //Search Pills
    SEARCH_PILL : "search-searchPill",
    SEARCH_PILL_PANE : "search-searchPill-pane",
    SEARCH_TEXT_BOX : "search-text-box",
    
    ERROR_DIALOG : "javaBatch-error-dialog-popup",
    ERROR_DIALOG_FOR_IN_MEMORY_DB : "javaBatch-inmemory-error-dialog-popup",
    ERROR_DIALOG_FOR_IGNORED_SEARCH_CRITERIA : "javaBatch-search-criteria-error-dialog-popup",
    ERROR_DIALOG_FOR_AUTHORIZATION: "javaBatch-authorization-error-dialog-popup",
    BREADCRUMB_JOBINSTANCE : 'breadcrumb-jobinstance',
    BREADCRUMB_JOBEXECUTION : 'breadcrumb-jobexecution',
    BREADCRUMB_JOBLOG : 'breadcrumb-joblog',
    
    // Job Instance Restart Dialog
    REUSE_PARMS_TOGGLE_ID: 'reuseParmsToggle',
    PARM_GRID_DIV_ID: 'parmGridDiv',
    PARM_GRID_ID: 'parmGrid',
    EDIT_PARM_GRID_ID: 'editingParmGrid',
    PARM_GRID_NAME: 'parmName',
    PARM_GRID_VALUE: 'value',
    PARM_GRID_ACTIONS: 'parmGridActions',
    PARM_GRID_ADD_BUTTON: 'parmGridAddParm',
    
    camel : function(phrase){
      var segments = phrase.split(this.DASH);
      for(var i = 0; i < segments.length; ++i){
        segments[i] = segments[i].substring(0,1).toLowerCase() + segments[i].substring(1);
      }
      return segments.join(this.DASH);
    },
    
    getJavaBatch : function(){
      return this.camel(this.JAVA_BATCH);
    },
    
    getJoblogsContentPane : function(){
      return this.camel(this.JOB_LOGS_PAGE+'-'+this.CONTENT_PANE);
    },
    
    getJoblogsNavigationBar : function(){
      return this.camel(this.JOB_LOGS_PAGE+'-'+this.PAGE_NAVIGATION_BAR);
    }
};
return ID;
});