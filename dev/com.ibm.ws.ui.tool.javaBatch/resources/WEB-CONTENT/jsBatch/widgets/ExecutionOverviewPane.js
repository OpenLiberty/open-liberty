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
/* jshint strict: false */
define([ 'dojo/_base/declare', 'dojo/_base/lang', 'dojo/dom', 'dojo/dom-construct',
         'dijit/_WidgetBase', 'dijit/_TemplatedMixin', 'dijit/_WidgetsInTemplateMixin',
         'dijit/registry', 'dijit/form/Button',
         'dojox/string/BidiComplex',
         'jsBatch/utils/ID',
         'jsBatch/utils/linkToExploreUtils',
         'jsBatch/utils/utils',
         'jsBatch/utils/viewToHash',
         'jsBatch/widgets/ExecutionTitlePane',
         'jsBatch/widgets/TextWithImage',
         'jsShared/utils/imgUtils',
         'jsShared/utils/utils',
         'dojo/text!./templates/ExecutionOverviewPane.html', 
         'dojo/i18n!jsBatch/nls/javaBatchMessages',
         'dojo/domReady!'
    ], 
function(declare, lang, dom, domConstruct, 
         WidgetBase, _TemplatedMixin, _WidgetsInTemplateMixin, 
         registry, Button, BidiComplex,
         ID, linkUtils, utils, viewToHash,
         ExecutionTitlePane, TextWithImage, 
         imgUtils, sharedUtils, 
         template, i18n) {

  /**
   * Represents the details of a specific execution.  This Widget does not query
   * for the information; rather, the information to be displayed about an 
   * execution must be submitted via parameters on instance creation.  The 
   * values for the fields can then later be updated via updateData(...).
   * 
   * It is displayed on the top 1/2 of the Execution Details View.
   * 
   * Creation relies on the following parameters
   *    executionId: execution identifier
   *    executionDetails: JSON object containing the values to be displayed about the execution
   *    jobInstanceDetails: JSON object containing the values to be displayed about the Job Instance
   *                        associated with the execution.
   */
  
  return declare("ExecutionOverviewPane", [ WidgetBase, _TemplatedMixin, _WidgetsInTemplateMixin ], {
    id: ID.EOP,
    widgetsInTemplate: true,
    templateString: template,

    // Template variables
    executionId: 'setMe',       // the execution ID value
    parentInfoSection: i18n.PARENT_DETAILS,
    batchJobNameLabel: i18n.BATCH_JOB_NAME,
    applicationLabel: i18n.APPLICATION,
    instanceStateLabel: i18n.INSTANCE_STATE,
    submitterLabel: i18n.SUBMITTER,
    executionIdLabel: i18n.EXECUTION_ID,
    batchStatusLabel: i18n.BATCH_STATUS,
    lastUpdateLabel: i18n.LAST_UPDATE,
    createTimeLabel: i18n.CREATE_TIME,
    startTimeLabel: i18n.START_TIME,
    endTimeLabel: i18n.END_TIME,
    serverSection: i18n.SERVER,
    nameLabel: i18n.NAME,
    hostLabel: i18n.HOST,
    userDirLabel: i18n.SERVERS_USER_DIRECTORY,
    exitStatusLabel: i18n.EXIT_STATUS,
    jobParametersLabel: i18n.JOB_PARAMETERS,
    viewLogFileLabel: i18n.VIEW_LOG_FILE,
    timesSectionLabel: i18n.TIMES,
    statusSectionLabel: i18n.STATUS,
    
    // Values set from query response data, passed in as parameters.
    // See 'executionDetails' and 'jobInstanceDetails' input variables.
    // Execution data values
    jobInstanceId: "",
    batchStatus: "",
    lastUpdate: "",
    createTime: "",
    startTime: "",
    endTime: "",
    server: "",
    host: "",
    userDir: "",
    serverName: "",
    exitStatus: "",
    jobParameters: "",
    // Job Instance data values
    batchJobName: "",
    application: "",
    instanceState: "",
    submitter: "",
    executionJobLogLink: "",
    
    constructor: function(args) {
      if (args.executionId && args.executionDetails && args.jobInstanceDetails) {
        this.executionId = args.executionId;
        
        this.__processExecutionDetailsJSON(args.executionDetails);
        this.__processJobInstanceDetailsJSON(args.jobInstanceDetails);

      } else {
        console.error("Execution details is unknown");        
      }     
    },
    
    postCreate: function() {
      this.inherited(arguments);

      // Process the data for the page
      this.__updateExecutionDetailsFields();
      this.__updateJobInstanceDetailsFields();
      
    },   
 
    __processExecutionDetailsJSON: function(json) {
      var t = this;
      
      t.jobInstanceId = json.instanceId;
      t.batchStatus = json.batchStatus;
      t.lastUpdate = json.lastUpdatedTime;
      t.createTime = json.createTime;
      t.startTime = json.startTime;
      t.endTime = json.endTime;
      t.server = json.serverId;
      t.exitStatus = json.exitStatus;
      var jobParameters = JSON.stringify(json.jobParameters, null, 1);
      t.jobParameters = jobParameters.replace(/:/g, ' : ');
      
      // The serverId value is a concatenation of the Host + User Directory + ServerName.
      // Separate these into 3 fields to be used in the 'Server' section of the 
      // ExecutionOverviewPane.
      var serverObj = utils.extractServerParts(t.server);
      t.host = serverObj.host;
      t.userDir = serverObj.userDir;
      t.serverName = serverObj.serverName;
            
      var links = json._links;
      t.executionJobLogLink = "";
      if (links) {     
        for (var i=0; i<links.length; i++) {
          if (links[i].rel === 'job logs') {
            t.executionJobLogLink = links[i].href + "?type=text";
            break;
          }
        }
      }

    },
    
    __updateExecutionDetailsFields: function() {
      var t = this;
      
      t.executionID.innerHTML = t.executionId;
      
      // Create the widget to display the Batch Status
      var batchStatusWidget = registry.byId(ID.EXECUTION_BATCH_STATUS);
      if (batchStatusWidget) {
        batchStatusWidget.destroyRecursive();
      }
      var statusDisplayValue = new TextWithImage ({
        type: 'status',
        value: t.batchStatus,
        id: ID.EXECUTION_BATCH_STATUS,
        divId: "execution-batchStatus"
      });
      statusDisplayValue.placeAt(t.executionStatus);
     
      t.executionLastUpdate.innerHTML =  utils.formatDate(t.lastUpdate);
      t.executionCreateTime.innerHTML = utils.formatDate(t.createTime);
      t.executionStartTime.innerHTML = utils.formatDate(t.startTime);
      t.executionEndTime.innerHTML = utils.formatDate(t.endTime);
      
      t.serverServerName.innerHTML = "<span dir='" + sharedUtils.getStringTextDirection(t.serverName) + "'>" + t.serverName + '</span>';
      t.__setLinkForServerNameToExplore();
      t.serverHost.innerHTML = "<span dir='" + sharedUtils.getStringTextDirection(t.host) + "'>" + t.host + '</span>';
      t.__setLinkForHostNameToExplore();
      t.serverUserDir.innerHTML = BidiComplex.createDisplayString(t.userDir, "FILE_PATH");
      
      t.executionExitStatus.innerHTML = t.exitStatus;
      t.executionJobParamters.innerHTML = t.jobParameters;
      
      // Create the View Log button for the execution
      var viewLogButtonWidget = registry.byId(ID.VIEW_LOG_BUTTON);
      if (viewLogButtonWidget) {
        viewLogButtonWidget.destroyRecursive();
      }
      var executionLogViewButton = new Button({
        id: ID.VIEW_LOG_BUTTON,
        label: t.viewLogFileLabel,
        iconClass : 'executionViewlogIcon',
        baseClass : 'executionViewLogButton',
        postCreate: function(){
          this.iconNode.innerHTML = imgUtils.getSVG('logfile');
        },
        onClick: function() {
          viewToHash.updateView("joblogs/?jobexecution=" + t.executionId);
        }
      });
      executionLogViewButton.placeAt(t.executionViewLogButton);
    },
    
    __processJobInstanceDetailsJSON: function(json) {
      var t = this;
      
      t.batchJobName = json.jobName;
      var appName = json.appName;
      if (json.appName.indexOf('#') > -1) {
        appName = json.appName.substr(0, json.appName.indexOf('#'));
      } 
      t.application = appName;
      t.instanceState = json.instanceState;
      t.submitter = json.submitter;
      
    },
    
    __updateJobInstanceDetailsFields: function() {
      var t = this;
      
      t.jobInstanceBatchJobName.innerHTML = "<span class='valueBold' dir='" + sharedUtils.getStringTextDirection(t.batchJobName) + "'>" + t.batchJobName + '</span>';
      t.jobInstanceApplication.innerHTML = "<span dir='" + sharedUtils.getStringTextDirection(t.application) + "'>" + t.application + '</span>';
       
      // Create the widget to display the Job Instance State
      var instanceStateWidget = registry.byId("jobInstance-instanceState");
      if (instanceStateWidget) {
        instanceStateWidget.destroyRecursive();
      }
      var statusDisplayValue = new TextWithImage ({
        type: 'status',
        value: t.instanceState,
        id: ID.JOB_INSTANCE_INSTANCE_STATE,
        divId: "jobInstance-instanceState"
      });
      statusDisplayValue.placeAt(t.jobInstanceInstanceState);
      
      t.jobInstanceSubmitter.innerHTML = "<span dir='" + sharedUtils.getStringTextDirection(t.submitter) + "'>" + t.submitter + '</span>';
    },
    
    updateData: function(executionId, executionDetails, jobInstanceDetails) {
      var t = this;
      
      if (executionId && executionDetails && jobInstanceDetails) {
        t.executionId = executionId;
        
        t.__processExecutionDetailsJSON(executionDetails);
        t.__processJobInstanceDetailsJSON(jobInstanceDetails);
        
        t.__updateExecutionDetailsFields();
        t.__updateJobInstanceDetailsFields();        
      } else {
        console.error("Data was not sufficient to update the view.");
        t.reinitialize();
        t.__updateExecutionDetailsFields();
        t.__updateJobInstanceDetailsFields();
      }
      
    },
    
    reinitialize: function() {
      var t = this;
      
      // Execution data values
      t.executionId = "";
      t.jobInstanceId = "";
      t.batchStatus = "";
      t.lastUpdate = "";
      t.createTime = "";
      t.startTime = "";
      t.endTime = "";
      t.server = "";
      t.host = "";
      t.userDir = "";
      t.serverName = "";
      t.exitStatus = "";
      t.jobParameters = "";
      
      // Job Instance data values
      t.batchJobName = "";
      t.application = "";
      t.instanceState = "";
      t.submitter = "";
      t.executionJobLogLink = "";

    },
    
    __setLinkForServerNameToExplore: function() {
        var t = this;
        if (linkUtils.hasExploreTool()) {
            // Continue with checks to set up a link to view details of this server in the explore tool
            if (linkUtils.hasStandaloneServer()) {
                var standaloneServerObj = linkUtils.getStandaloneServerInfo();
                if (standaloneServerObj) {
                    if (t.serverName === standaloneServerObj.name) {
                        var titleMsg = lang.replace(i18n.LINK_EXPLORE_SERVER, [t.serverName]);
                        var toolRef = linkUtils.getLinkPrefixToExploreTool();
                        var serverHref = window.top.location.origin + toolRef;
                        t.serverServerName.innerHTML = "<span title=\"" + titleMsg + "\" dir='" + sharedUtils.getStringTextDirection(t.serverName) + "'><a target='_blank' rel='noreferrer' href='" + serverHref + "'>" + t.serverName + "</a></span>";
                    }                                          
                }
            } else {
                // Continue with checks to see if this server is in a collective
                var serverObj = {name: t.serverName,        // serverObj = {name: server, userdir: userdir, host: hostname}
                                 userdir: t.userDir,
                                 host: t.host
                                };   
                linkUtils.isServerInCollective(serverObj).then(lang.hitch(this, function(serverObj, response) {
                    if (response === true) {                         
                        var titleMsg = lang.replace(i18n.LINK_EXPLORE_SERVER, [serverObj.name]);
                        var toolRef = linkUtils.getLinkPrefixToExploreTool() +"/servers/";
                        var serverHref = window.top.location.origin + toolRef + serverObj.host + "," + serverObj.userdir + "," + serverObj.name;
                        this.serverServerName.innerHTML = "<span title=\"" + titleMsg + "\" dir='" + sharedUtils.getStringTextDirection(serverObj.name) + "'><a target='_blank' rel='noreferrer' href='" + serverHref + "'>" + serverObj.name + "</a></span>";
                    } 
                    // else do nothing since we already put in a non-linking value in statement above 'isServerInCollective'.
                }, serverObj));  
            }
         }
    }, 
    
    __setLinkForHostNameToExplore: function() {
        var t = this;
        if (linkUtils.hasExploreTool() && !linkUtils.hasStandaloneServer()) {
            // Continue with checks to set up a link to view details of this host in the explore tool
            linkUtils.isHostInExploreTool(t.host).then(lang.hitch(this, function(response) {
                if (response === true) {                         
                    var titleMsg = lang.replace(i18n.LINK_EXPLORE_HOST, [this.host]);
                    var toolRef = linkUtils.getLinkPrefixToExploreTool() +"/hosts/";
                    var hostHref = window.top.location.origin + toolRef + this.host;
                    this.serverHost.innerHTML = "<span title=\"" + titleMsg + "\" dir='" + sharedUtils.getStringTextDirection(this.host) + "'><a target='_blank' rel='noreferrer' href='" + hostHref + "'>" + this.host + "</a></span>";
                } 
             }));  
        } 

    }
    
  });
  
});
