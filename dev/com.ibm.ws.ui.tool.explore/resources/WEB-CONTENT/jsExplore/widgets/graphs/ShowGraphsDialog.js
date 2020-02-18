/* jshint strict: false */
define(["jsShared/utils/toolData",
        "jsShared/utils/userConfig",
        "jsExplore/resources/stats/_pmiStats",
        "jsExplore/utils/ID",
        "jsExplore/resources/utils",
        "jsExplore/utils/constants",
        "jsExplore/widgets/graphs/ConnectionPoolStatsGraph",
        "jsExplore/widgets/graphs/JVMClassesGraph",
        "jsExplore/widgets/graphs/JVMHeapGraph",
        "jsExplore/widgets/graphs/JVMThreadGraph",
        "jsExplore/widgets/graphs/PMIServletStatsGraph",
        "jsExplore/widgets/graphs/PMISessionGraph",
        "jsExplore/widgets/graphs/PMIThreadPoolGraph",
        "jsExplore/widgets/graphs/ProcessCPUGraph",
//        "jsExplore/widgets/graphs/logAnalytics/MessagesTableGraph",
//        "jsExplore/widgets/graphs/logAnalytics/LogAnalyticsGraph",
        "jsExplore/widgets/graphs/GraphWarningMessage",
//        'jsExplore/widgets/graphs/logAnalytics/charts/BarChart',
//        "jsExplore/resources/stats/_logAnalyticsUtils",
        "dojo/_base/declare",
        "dojo/_base/lang",
        "dojo/_base/fx",
        "dojo/dom",
        "dojo/Deferred",
        "dojo/json",
        "dojo/query",
        "dijit/_Widget", 
        "dijit/_TemplatedMixin",
        "dijit/_WidgetsInTemplateMixin",
        "dijit/layout/ContentPane",
        "dijit/registry",
        "dojo/text!./templates/ShowGraphsDialog.html", 
        "dojo/i18n!jsExplore/nls/explorerMessages",
        "dojo/on",
        "dojo/dom-class",
        "dojo/dom-style",
        "dojo/domReady!"],
        function(
            tooldata,
            userConfig,
            pmiStats,
            ID,
            utils,
            constants,
            ConnectionPoolStatsGraph,
            JVMClassesGraph,
            JVMHeapGraph,
            JVMThreadGraph,
            PMIServletStatsGraph,
            PMISessionGraph,
            PMIThreadPoolGraph,
            ProcessCPUGraph,
//            MessagesTableGraph,
//            LogAnalyticsGraph,
            GraphWarningMessage,
//            BarChart,
//            logAnalyticsUtils,
            declare,
            lang,
            fx,
            dom,
            Deferred,
            JSON,
            query,
            Widget,
            TemplatedMixin,
            WidgetsInTemplateMixin,
            ContentPane,
            registry,
            template,
            i18n,
            on,
            domClass,
            domStyle) {
  // summary:
  //    A contentPane Widget that has a list of graph checkboxes.
  // description:
  //    Displays a contentPane.

  return declare("ShowGraphsDialog", [ContentPane, TemplatedMixin, WidgetsInTemplateMixin], {
    constructor: function(params) {
      if (params.perspective) {
        this.perspective = params.perspective;
      }
      this.id = this.perspective + ID.getShowGraphsDialogId();
      switch (this.perspective) {
        case "Summary":
          this.subTitle = i18n.STATS_SHOW_HIDE_SUMMARY;
          break;
        case "Traffic":
          this.subTitle = i18n.STATS_SHOW_HIDE_TRAFFIC;
          break;
        case "Performance":
          this.subTitle = i18n.STATS_SHOW_HIDE_PERFORMANCE;
          break;
        case "Alert":
          this.subTitle = i18n.STATS_SHOW_HIDE_ALERT;
          break;
      };
    },

    templateString: template,
    resource : null,
    nodeJSResource : false,
    type : "",
    id : ID.getShowGraphsDialogId(),
    idPrefix : "",
    perspective : "",
    paneWarningId: null,
    graphWarningMsg: null,
    graphWarningMsgID: null,
    graphWarningPaneID: null,
    graphWarningMsgText: i18n.GRAPH_FEATURES_NOT_CONFIGURED,
    
    showGraphsDialogButtonId : null,
    showHideButtonId : null,
    showGraphsDialogContainerId : null,
    jvmParentId : null,
    jvmHeadingId : null,
    addAllJVMGraphButtonId : null,
    addMemoryGraphButtonId : null,
    addClassesGraphButtonId : null,
    addThreadsGraphButtonId : null,
    addCPUGraphButtonId : null,
    threadPoolParentId : null,
    addAllThreadPoolGraphButtonId : null,
    addActiveThreadsGraphButtonId : null,
    connPoolParentId : null,
    addAllConnGraphButtonId : null,
    addUsedConnGraphButtonId : null,
    addAvgWaitGraphButtonId : null,
    sessionMgmtParentId : null,
    addAllSessionMgmtGraphButtonId : null,
    addActiveSessionsGraphButtonId : null,
    accessLogParentId : null,
    addAllAccessLogGraphButtonId : null,
    accessLogGraphOptionId : null,
    addAccessLogGraphGraphButtonId : null,
    accessLogSummaryOptionId : null,
    addAccessLogSummaryGraphButtonId : null,
    messagesParentId : null,
    addAllMessagesGraphButtonId : null,
    messagesGraphOptionId : null,
    addMessagesGraphGraphButtonId : null,
    messagesTableOptionId : null,
    addMessagesTableGraphButtonId : null,
    FFDCGraphOptionId : null,
    addFFDCGraphGraphButtonId : null,
    FFDCTableOptionId : null,
    addFFDCTableGraphButtonId : null,
    traceLogGraphOptionId : null,
    addTraceLogGraphGraphButtonId : null,
    traceLogTableOptionId : null,
    addTraceLogTableGraphButtonId : null,
    wcParentId : null,
    addAllWebContainerGraphButtonId : null,
    addAvgRespGraphButtonId : null,
    addReqCountGraphButtonId : null,
    
    modified : false,
    // The utility to write persisted data to the Server.
    toolData: null,
    // The persistenceURL that identifies which feature to write the 
    persistenceFeatureName: "com.ibm.websphere.appserver.adminCenter.tool.explore",
    // This is used to determine whether the next write should be attempted with a PUT or a POST.
    // If we find data on the get we'll set this variable to be true. If not then we'll set it to false.
    // When we come to write it out, false means we initially attempt with a POST, true means a PUT.
    persistenceFileExists: false,
    // The persisted data object when read from the server. We store this so we don't have to re-read when we save. It might
    // be that we will have to re-read if the data is stale because something else has updated the data, but this should
    // prevent the need to read on the odd occasion.
    persistedData: {},
    

    subTitle : i18n.STATS_SHOW_HIDE,
    showHideButtonLabel : i18n.STATS_SHOW_HIDE_LIST_BUTTON,
    addButtonLabel : i18n.STATS_ADD_CHART_LABEL,
    confirmButtonLabel : i18n.STATS_SHOW_HIDE_CONFIRM,
    jvmOptionsLabel : i18n.STATS_JVM_TITLE,
    jvmOptionsButtonLabel : i18n.STATS_JVM_BUTTON_LABEL,
    heapGraphLabel : i18n.STATS_HEAP_TITLE,
    classesGraphLabel : i18n.STATS_CLASSES_TITLE,
    cpuGraphLabel : i18n.STATS_PROCESSCPU_TITLE,
    threadsGraphLabel : i18n.STATS_THREADS_TITLE,
    webContainerLabel : i18n.STATS_WEBCONTAINER_TITLE,
    webContainerButtonLabel : i18n.STATS_WEBCONTAINER_BUTTON_LABEL,
    wcReqCountLabel : i18n.STATS_SERVLET_REQUEST_COUNT_TITLE,
    wcAvgRespLabel : i18n.STATS_SERVLET_RESPONSE_MEAN_TITLE,
    threadPoolLabel : i18n.STATS_THREAD_POOL_TITLE,
    threadPoolButtonLabel : i18n.STATS_THREAD_POOL_BUTTON_LABEL,
    tpActiveLabel : i18n.STATS_THREADPOOL_TITLE,
    tpSizeLabel : i18n.STATS_TP_POOL_SIZE,
    sessionMgmtLabel : i18n.STATS_SESSION_MGMT_TITLE,
    sessionMgmtButtonLabel : i18n.STATS_SESSION_MGMT_BUTTON_LABEL,
    smActiveLabel : i18n.STATS_SESSION_TITLE,
    connPoolLabel : i18n.STATS_CONN_POOL_TITLE,
    connPoolButtonLabel : i18n.STATS_CONN_POOL_BUTTON_LABEL,
    usedConnCountLabel : i18n.STATS_CONNECT_IN_USE_TITLE,
    connAvgWaitLabel : i18n.STATS_CONNECT_WAIT_TIME_TITLE,
    jaxwsLabel : i18n.STATS_JAXWS_TITLE,
    jaxwsButtonLabel : i18n.STATS_JAXWS_BUTTON_LABEL,
    jaxwsAvgRespLabel : i18n.STATS_JW_AVG_RESP_TIME,
    jaxwsAvgInvLabel : i18n.STATS_JW_AVG_INVCOUNT,
    jaxwsTotalFaultsLabel : i18n.STATS_JW_TOTAL_FAULTS,
    accessLogLabel: i18n.STATS_ACCESS_LOG_TITLE,
    accessLogButtonLabel: i18n.STATS_ACCESS_LOG_BUTTON_LABEL,
    accessLogGraphLabel : i18n.STATS_ACCESS_LOG_GRAPH,
    accessLogSummaryLabel : i18n.STATS_ACCESS_LOG_TABLE,
    messagesLabel : i18n.STATS_MESSAGES_TITLE,
    messagesButtonLabel : i18n.STATS_MESSAGES_BUTTON_LABEL,
    messagesGraphLabel : i18n.STATS_MESSAGES_GRAPH,
    messagesTableLabel : i18n.STATS_MESSAGES_TABLE,
    FFDCGraphLabel : i18n.STATS_FFDC_GRAPH,
    FFDCTableLabel : i18n.STATS_FFDC_TABLE,
    traceLogGraphLabel : i18n.STATS_TRACE_LOG_GRAPH,
    traceLogTableLabel : i18n.STATS_TRACE_LOG_TABLE,

    postMixInProperties : function() {
      //put ids together
      this.showGraphsDialogButtonId = this.perspective + ID.underscoreDelimit(ID.getShowGraphsDialog(), ID.getButton());
      this.showHideButtonId = this.perspective + ID.getShowHideButton();
      this.showGraphsDialogContainerId = this.perspective + ID.underscoreDelimit(ID.getShowGraphsDialog(), ID.getContainer());
      this.jvmParentId = ID.getJvmParent();
      this.jvmHeadingId = ID.getJvmHeading();
      this.addAllJVMGraphButtonId = this.perspective + ID.getAddAllJVMGraphButton();
      this.addMemoryGraphButtonId = this.perspective + ID.getAddMemoryGraphButton();
      this.addClassesGraphButtonId = this.perspective + ID.getAddClassesGraphButton();
      this.addThreadsGraphButtonId = this.perspective + ID.getAddThreadsGraphButton();
      this.addCPUGraphButtonId = this.perspective + ID.getAddCPUGraphButton();
      this.threadPoolParentId = ID.getThreadPoolParent();
      this.addAllThreadPoolGraphButtonId = this.perspective + ID.getAddAllThreadPoolGraphButton();
      this.addActiveThreadsGraphButtonId = this.perspective + ID.getAddActiveThreadsGraphButton();
      this.connPoolParentId = ID.getConnPoolParent();
      this.addAllConnGraphButtonId = this.perspective + ID.getAddAllConnGraphButton();
      this.addUsedConnGraphButtonId = this.perspective + ID.getAddUsedConnGraphButton();
      this.addAvgWaitGraphButtonId = this.perspective + ID.getAddAvgWaitGraphButton();
      this.sessionMgmtParentId = ID.getSessionMgmtParent();
      this.addAllSessionMgmtGraphButtonId = this.perspective + ID.getAddAllSessionMgmtGraphButton();
      this.addActiveSessionsGraphButtonId = this.perspective + ID.getAddActiveSessionsGraphButton();
      this.accessLogParentId = ID.getAccessLogParent();
      this.addAllAccessLogGraphButtonId = this.perspective + ID.getAddAllAccessLogGraphButton();
      this.accessLogGraphOptionId = this.perspective + ID.getAccessLogGraphOption();
      this.addAccessLogGraphGraphButtonId = this.perspective + ID.getAddAccessLogGraphGraphButton();
      this.accessLogSummaryOptionId = this.perspective + ID.getAccessLogSummaryOption();
      this.addAccessLogSummaryGraphButtonId = this.perspective + ID.getAddAccessLogSummaryGraphButton();
      this.messagesParentId = ID.getMessagesParent();
      this.addAllMessagesGraphButtonId = this.perspective + ID.getAddAllMessagesGraphButton();
      this.messagesGraphOptionId = this.perspective + ID.getMessagesGraphOption();
      this.addMessagesGraphGraphButtonId = this.perspective + ID.getAddMessagesGraphGraphButton();
      this.messagesTableOptionId = this.perspective + ID.getMessagesTableOption();
      this.addMessagesTableGraphButtonId = this.perspective + ID.getAddMessagesTableGraphButton();
      this.FFDCGraphOptionId = this.perspective + ID.getFFDCGraphOption();
      this.addFFDCGraphGraphButtonId = this.perspective + ID.getAddFFDCGraphGraphButton();
      this.FFDCTableOptionId = this.perspective + ID.getFFDCTableOption();
      this.addFFDCTableGraphButtonId = this.perspective + ID.getAddFFDCTableGraphButton();
      this.traceLogGraphOptionId = this.perspective + ID.getTraceLogGraphOption();
      this.addTraceLogGraphGraphButtonId = this.perspective + ID.getAddTraceLogGraphGraphButton();
      this.traceLogTableOptionId = this.perspective + ID.getTraceLogTableOption();
      this.addTraceLogTableGraphButtonId = this.perspective + ID.getAddTraceLogTableGraphButton();
      this.wcParentId = ID.getWcParent();
      this.addAllWebContainerGraphButtonId = this.perspective + ID.getAddAllWebContainerGraphButton();
      this.addAvgRespGraphButtonId = this.perspective + ID.getAddAvgRespGraphButton();
      this.addReqCountGraphButtonId =this.perspective +  ID.getAddReqCountGraphButton();
      // Create the warning pane id
      this.graphWarningPanelID = this.perspective + ID.underscoreDelimit(this.resource.id, ID.getGraphWarningMessage(), ID.getAppInstStatsSelectorUpper());
      this.paneWarningId = this.perspective + ID.underscoreDelimit(this.resource.id, ID.getGraphWarningMessage(), ID.getAppInstStatsPaneUpper());
      if (this.resource.type == 'server' || this.resource.type === 'standaloneServer') {
        this.graphWarningPanelID = this.perspective + ID.underscoreDelimit(this.resource.id, ID.getGraphWarningMessage(), ID.getServerStatsSelectorUpper());
        this.paneWarningId = this.perspective + ID.underscoreDelimit(this.resource.id, ID.getGraphWarningMessage(), ID.getServerStatsPaneUpper());
      }

    },
    
    postCreate: function() {
      // resource.name is not unique, need to use resource.id
      this.idPrefix = (this.resource.type === 'server' || this.resource.type === 'standaloneServer' ) ? ID.getServer() + this.resource.id : ID.getApp() + this.resource.id;
      this.inherited(arguments);
      if (this.resource.type === 'server' && this.resource.runtimeType && this.resource.runtimeType === constants.RUNTIME_NODEJS) {  
        this.nodeJSResource = true;
      }
      this.set("style", "overflow:visible;");

      this.graphData = 
      {
          JVM:
          {
            name: "JVM",
            buttonNode: this.addAllJVMGraphButton,
            textNode: this.addAllJVMGraphOptionNode,
            perspectives : ["Traffic", "Performance"],
            perspectivesTitles : [i18n.STATS_PERSPECTIVE_TRAFFIC_JVM, i18n.STATS_PERSPECTIVE_PERFORMANCE_JVM],
            multipleGraphs: false,
            graphs:
              [{
                  id : this.idPrefix + ID.getHeapStatsUpper(),
                perspectives : ["Traffic", "Performance"],
                showGraphsDialog : this,
                multipleGraphs: false,
                defaultDisplay: true,
                graphSection : "JVM",
                graphType : "HeapStats",
                graphClass : JVMHeapGraph,
                buildGraph: this.__buildSimpleGraph,
                buttonNode: this.addMemoryGraphButton,
                tableNode: this.heapGraphOptionTable,
                textNode: this.heapGraphOptionNode
              },
              {
                  id : this.idPrefix + ID.getClassesStatsUpper(),
                perspectives : ["Traffic"],
                showGraphsDialog : this,
                multipleGraphs: false,
                defaultDisplay: true,
                graphSection : "JVM",
                graphType : "ClassesStats",
                graphClass : JVMClassesGraph,
                buildGraph: this.__buildSimpleGraph,
                buttonNode: this.addClassesGraphButton,
                tableNode: this.classesGraphOptionTable,
                textNode: this.classesGraphOptionNode
              },
              {
                  id : this.idPrefix + ID.getThreadStatsUpper(),
                perspectives : ["Traffic"],
                showGraphsDialog : this,
                multipleGraphs: false,
                defaultDisplay: true,
                graphSection : "JVM",
                graphType : "ThreadStats",
                graphClass : JVMThreadGraph,
                buildGraph: this.__buildSimpleGraph,
                buttonNode: this.addThreadsGraphButton,
                tableNode: this.threadsGraphOptionTable,
                textNode: this.threadsGraphOptionNode
              },
              {
                  id : this.idPrefix + ID.getProcessCPUStatsUpper(),
                perspectives : ["Traffic", "Performance"],
                showGraphsDialog : this,
                multipleGraphs: false,
                defaultDisplay: true,
                graphSection : "JVM",
                graphType : "ProcessCPUStats",
                graphClass : ProcessCPUGraph,
                buildGraph: this.__buildSimpleGraph,
                buttonNode: this.addCPUGraphButton,
                tableNode: this.cpuGraphOptionTable,
                textNode: this.cpuGraphOptionNode
              }]
          },

          Session:
          {
            name: "Session",
            buttonNode: this.addAllSessionMgmtGraphButton,
            textNode: this.addAllSessionMgmtGraphOptionNode,
            perspectives : ["Traffic"],
            multipleGraphs: true,
            graphs:
              [{
                  id : this.idPrefix + ID.getSessionStatsUpper(),
                perspectives : ["Traffic"],
                showGraphsDialog : this,
                multipleGraphs: true,
                graphInstances: [],
                graphInstancesIndex: 0,  // incremented when graphs are added and used for the id (note not decremented when removed)
                graphSection : "Session",
                graphType : "SessionStats",
                graphClass : PMISessionGraph,
                graphParams : ["ActiveCount"],
                buildGraph: this.__buildDeferredGraph,
                deferredFunction: pmiStats.getServerSessions,
                buttonNode: this.addActiveSessionsGraphButton,
                tableNode: this.activeSessionsOptionTable,
                textNode: this.activeSessionsOptionNode
              }]
          },

          Servlet:
          {
            name: "Servlet",
            perspectives : [],
            buttonNode: this.addAllWebContainerGraphButton,
            textNode: this.addAllWebContainerGraphOptionNode,
            multipleGraphs: true,
            graphs:
              [{
                  id : this.idPrefix + ID.getResponseMeanServletStatsUpper(),
                perspectives : [],
                showGraphsDialog : this,
                multipleGraphs: true,
                defaultDisplay: true,
                graphInstances: [],
                graphInstancesIndex: 0,  // incremented when graphs are added and used for the id (note not decremented when removed)
                graphSection : "Servlet",
                graphType : "ResponseMeanServletStats",
                graphClass : PMIServletStatsGraph,
                graphParams : [this.resource.name, "ResponseMean"],
                buildGraph: this.__buildDeferredGraph,
                deferredFunction: pmiStats.getServletsForApp,
                buttonNode: this.addAvgRespGraphButton,
                tableNode: this.avgRespOptionTable,
                textNode: this.avgRespOptionNode
              },
              {
                  id : this.idPrefix + ID.getRequestCountServletStatsUpper(),
                perspectives : [],
                showGraphsDialog : this,
                multipleGraphs: true,
                defaultDisplay: true,
                graphInstances: [],
                graphInstancesIndex: 0,
                graphSection : "Servlet",
                graphType : "RequestCountServletStats",
                graphClass : PMIServletStatsGraph,
                graphParams : [this.resource.name, "RequestCount"],
                buildGraph: this.__buildDeferredGraph,
                deferredFunction: pmiStats.getServletsForApp,
                buttonNode: this.addReqCountGraphButton,
                tableNode: this.reqCountOptionTable,
                textNode: this.reqCountOptionNode
              }]
          },

          ThreadPool:
          {
            name: "ThreadPool",
            buttonNode: this.addAllThreadPoolGraphButton,
            textNode: this.addAllThreadPoolGraphOptionNode,
            perspectives : ["Traffic"],
            multipleGraphs: false,
            graphs:
              [{
                  id : this.idPrefix + ID.getThreadPoolStatsUpper(),
                perspectives : ["Traffic"],
                showGraphsDialog : this,
                multipleGraphs: false,
                graphSection : "ThreadPool",
                graphType : "ThreadPoolStats",
                graphClass : PMIThreadPoolGraph,
                buildGraph: this.__buildSimpleGraph,
                buttonNode: this.addActiveThreadsGraphButton,
                tableNode: this.activeThreadsOptionTable,
                textNode: this.activeThreadsOptionNode
              }]
          },

          Connection:
          {
            name: "Connection",
            buttonNode: this.addAllConnGraphButton,
            textNode: this.addAllConnGraphOptionNode,
            perspectives : ["Traffic", "Performance"],
            perspectivesTitles : [i18n.STATS_PERSPECTIVE_TRAFFIC_CONN, i18n.STATS_PERSPECTIVE_PERFORMANCE_CONN],
            multipleGraphs: true,
            graphs:
              [{
                  id : this.idPrefix + ID.getManagedConnectionCountConnectionStatsUpper(),
                perspectives : ["Traffic"],
                showGraphsDialog : this,
                multipleGraphs: true,
                graphInstances: [],
                graphInstancesIndex: 0,  // incremented when graphs are added and used for the id (note not decremented when removed)
                graphSection : "Connection",
                graphType : "ManagedConnectionCountConnectionStats",
                graphClass : ConnectionPoolStatsGraph,
                graphParams : ["ManagedConnectionCount"],
                buildGraph: this.__buildDeferredGraph,
                deferredFunction: pmiStats.getDataSourcesForServer,
                buttonNode: this.addUsedConnGraphButton,
                tableNode: this.usedConnOptionTable,
                textNode: this.usedConnOptionNode
              },
              {
                  id : this.idPrefix + ID.getWaitTimeConnectionStatsUpper(),
                perspectives : ["Performance"],
                showGraphsDialog : this,
                multipleGraphs: true,
                graphInstances: [],
                graphInstancesIndex: 0,  // incremented when graphs are added and used for the id (note not decremented when removed)
                graphSection : "Connection",
                graphType : "WaitTimeConnectionStats",
                graphClass : ConnectionPoolStatsGraph,
                graphParams : ["WaitTime"],
                buildGraph: this.__buildDeferredGraph,
                deferredFunction: pmiStats.getDataSourcesForServer,
                buttonNode: this.addAvgWaitGraphButton,
                tableNode: this.avgWaitOptionTable,
                textNode: this.avgWaitOptionNode
              }]
// DISABLE_ANALYTICS
//          },
//
//          LAAccessLog:
//          {
//            name: "LAAccessLog",
//            buttonNode: this.addAllAccessLogGraphButton,
//            textNode: this.addAllAccessLogGraphOptionNode,
//            perspectives : ["Traffic", "Alert"],
//            perspectivesTitles : [i18n.STATS_PERSPECTIVE_TRAFFIC_LAACCESS, i18n.STATS_PERSPECTIVE_ALERT_LAACCESS],
//            multipleGraphs: false,
//            graphs:
//              [{
//                id : this.idPrefix + "AccessLogGraph",
//                perspectives : ["Traffic", "Alert"],
//                showGraphsDialog : this,
//                multipleGraphs: false,
//                graphSection : "LAAccessLog",
//                graphType : "AccessLogGraph",
//                graphClass : LogAnalyticsGraph,
//                chartType : BarChart,
//                buildGraph: this.__buildLogAnalyticGraph,
//                buttonNode: this.addAccessLogGraphGraphButton,
//                tableNode: this.accessLogGraphOptionTable,
//                textNode: this.accessLogGraphOptionNode,
//                title: i18n.STATS_ACCESS_LOG_GRAPH,
//                pipeName: "access_count",
//                eventType: "access_log"
//              }
//              // Was not ready for Aug. 2015 Beta
//            ,{
//              id : this.idPrefix + "AccessLogSummary",
//              perspectives : ["Traffic", "Alert"],
//              showGraphsDialog : this,
//              multipleGraphs: false,
//              graphSection : "LAAccessLog",
//              graphType : "AccessLogSummary",
//              graphClass : MessagesTableGraph,
//              buildGraph: this.__buildLogAnalyticGraph,
//              buttonNode: this.addAccessLogSummaryGraphButton,
//              tableNode: this.accessLogSummaryOptionTable,
//              textNode: this.accessLogSummaryOptionNode,
//              title: i18n.STATS_ACCESS_LOG_TABLE,
//              pipeName: "access_table",
//              eventType: "access_log"
//            }
//            ]
//          },
//
//          LAMsgsTr:
//          {
//            name: "LAMsgsTr",
//            buttonNode: this.addAllMessagesGraphButton,
//            textNode: this.addAllMessagesGraphOptionNode,
//            perspectives : ["Alert"],
//            perspectivesTitles : [i18n.STATS_PERSPECTIVE_ALERT_LAMSGS],
//            multipleGraphs: false,
//            graphs:
//              [{
//                id : this.idPrefix + "MessagesGraph",
//                perspectives : ["Alert"],
//                showGraphsDialog : this,
//                multipleGraphs: false,
//                graphInstances: [],
//                graphInstancesIndex: 0,  // incremented when graphs are added and used for the id (note not decremented when removed)
//                graphSection : "LAMsgsTr",
//                graphType : "MessagesGraph",
//                graphClass : LogAnalyticsGraph,
//                chartType : BarChart,
//                buildGraph: this.__buildLogAnalyticGraph,
//                buttonNode: this.addMessagesGraphGraphButton,
//                tableNode: this.messagesGraphOptionTable,
//                textNode: this.messagesGraphOptionNode,
//                title: i18n.STATS_MESSAGES_GRAPH,
//                pipeName: "messages_count", //"messages_histogram", //"accessLogCount",
//                eventType: "liberty_message"
//              },
//              {
//                id : this.idPrefix + "MessagesTable",
//                perspectives : ["Alert"],
//                showGraphsDialog : this,
//                multipleGraphs: false,
//                graphSection : "LAMsgsTr",
//                graphType : "MessagesTable",
//                graphClass : MessagesTableGraph,
//                buildGraph: this.__buildLogAnalyticGraph,
//                buttonNode: this.addMessagesTableGraphButton,
//                tableNode: this.messagesTableOptionTable,
//                textNode: this.messagesTableOptionNode,
//                title: i18n.STATS_MESSAGES_TABLE,
//                pipeName: "messages_table",
//                eventType: "liberty_message"
//              },
//              {
//                id : this.idPrefix + "FFDCGraph",
//                perspectives : ["Alert"],
//                showGraphsDialog : this,
//                multipleGraphs: false,
//                graphSection : "LAMsgsTr",
//                graphType : "FFDCGraph",
//                graphClass : LogAnalyticsGraph,
//                chartType : BarChart,
//                buildGraph: this.__buildLogAnalyticGraph,
//                buttonNode: this.addFFDCGraphGraphButton,
//                tableNode: this.FFDCGraphOptionTable,
//                textNode: this.FFDCGraphOptionNode,
//                title: i18n.STATS_FFDC_GRAPH,
//                pipeName: "ffdc_count",
//                eventType: "liberty_ffdc"
//              },
//              {
//                id : this.idPrefix + "FFDCTable",
//                perspectives : ["Alert"],
//                showGraphsDialog : this,
//                multipleGraphs: false,
//                graphSection : "LAMsgsTr",
//                graphType : "FFDCTable",
//                graphClass : MessagesTableGraph,
//                buildGraph: this.__buildLogAnalyticGraph,
//                buttonNode: this.addFFDCTableGraphButton,
//                tableNode: this.FFDCTableOptionTable,
//                textNode: this.FFDCTableOptionNode,
//                title: i18n.STATS_FFDC_TABLE,
//                pipeName: "ffdc_table",
//                eventType: "liberty_ffdc"
//              },
//              {
//                id : this.idPrefix + "TraceLogGraph",
//                perspectives : ["Alert"],
//                showGraphsDialog : this,
//                multipleGraphs: false,
//                graphSection : "LAMsgsTr",
//                graphType : "TraceLogGraph",
//                graphClass : LogAnalyticsGraph,
//                chartType : BarChart,
//                buildGraph: this.__buildLogAnalyticGraph,
//                buttonNode: this.addTraceLogGraphGraphButton,
//                tableNode: this.traceLogGraphOptionTable,
//                textNode: this.traceLogGraphOptionNode,
//                title: i18n.STATS_TRACE_LOG_GRAPH,
//                pipeName: "trace_count",
//                eventType: "liberty_trace"
//              }
//              // Was not ready for Aug. 2015 Beta
//            ,{
//              id : this.idPrefix + "TraceLogTable",
//              perspectives : ["Alert"],
//              showGraphsDialog : this,
//              multipleGraphs: false,
//              graphSection : "LAMsgsTr",
//              graphType : "TraceLogTable",
//              graphClass : MessagesTableGraph,
//              buildGraph: this.__buildLogAnalyticGraph,
//              buttonNode: this.addTraceLogTableGraphButton,
//              tableNode: this.traceLogTableOptionTable,
//              textNode: this.traceLogTableOptionNode,
//              title: i18n.STATS_TRACE_LOG_TABLE,
//              pipeName: "trace_table",
//              eventType: "liberty_trace"
//            }
//            ]
          }
      };
      
      userConfig.init("com.ibm.websphere.appserver.adminCenter.tool.explore");

//      // Create the persistence utility object that helps read/write out the persisted data.
//      this.toolData = tooldata.createToolData(this.persistenceFeatureName);

      // get the user config. Once this has been loaded, continue populating the graphs.
      this.__getGraphData().then(lang.hitch(this, function() {

        // Work out which type of graph we have.
        var graphType = this.resource.type;

        // If we're about to show the AppInstance graphs, but the PMI feature isn't available or the server is unavailable
        // then replace the display with the graph warning message.
        if (graphType === "appOnServer" && this.resource.server) {
          // If the server isn't started then we can't display any graphs
          if (this.resource.server.state !== "STARTED" && this.resource.server.type !== "standaloneServer") {
            this.graphWarningMsgText = i18n.GRAPH_SERVER_HOSTING_APP_NOT_STARTED;
            // If there are no graphs to display then display the relevant warning message.
          } else {
            this.graphWarningMsgText = i18n.NO_APPS_GRAPHS_AVAILABLE;
          };
          // For the server,  we only need to issue a message if the server is stopped, or if there are no selected graphs
        } else if (graphType === "server") {
          if (this.resource && this.resource.state !== "STARTED" && this.resource.type !== "standaloneServer") {
            this.graphWarningMsgText = i18n.GRAPH_SERVER_NOT_STARTED;
          };
        }
        this.graphWarningMsg.setTextMessage(i18n.NO_GRAPHS_AVAILABLE);
        registry.byId(this.paneWarningId).setTextMessage(this.graphWarningMsgText);
        // Initially configure the warning message to be displayed. When we find a graph to display, we disable the warning msg.
        domClass.replace(this.graphWarningMsg.domNode, "displayGraph", "hideGraph");

        this.checkForAvailableOptions(true);

        // for each section, set the onClick for the section and each graph in the section
        for (var section in this.graphData) {
          if (this.graphData.hasOwnProperty(section)) {
            // for the section, set the onClick
            this.own(
                on(this.graphData[section].buttonNode, "click", lang.hitch(this, function(section){
                  this.toggleSection(section.name);
                }, this.graphData[section])),
                on(this.graphData[section].buttonNode, "mouseover", lang.hitch(this, function(section) {
                  this.showHoverIcon(this.graphData[section.name].buttonNode, true);
                }, this.graphData[section])),
                on(this.graphData[section].buttonNode, "mouseout", lang.hitch(this, function(section) {
                  this.showHoverIcon(this.graphData[section.name].buttonNode, false);
                }, this.graphData[section])),
                on(this.graphData[section].buttonNode, "focus", lang.hitch(this, function(section) {
                  this.showHoverIcon(this.graphData[section.name].buttonNode, true);
                }, this.graphData[section])),
                on(this.graphData[section].buttonNode, "focusout", lang.hitch(this, function(section) {
                  this.showHoverIcon(this.graphData[section.name].buttonNode, false);
                }, this.graphData[section]))
            );
            // for each graph in the section, set the onClick
            this.graphData[section].graphs.forEach(lang.hitch(this, function(currGraph){
              this.own(
                  on(currGraph.buttonNode, "click", lang.hitch(this, function(){
                    this.toggleGraph(currGraph.id, true);
                  }, currGraph)),
                  on(currGraph.buttonNode, "mouseover", lang.hitch(this, function(){
                    this.showHoverIcon(currGraph.buttonNode, true);
                  }, currGraph)),
                  on(currGraph.buttonNode, "mouseout", lang.hitch(this, function(){
                    this.showHoverIcon(currGraph.buttonNode, false);
                  }, currGraph)),
                  on(currGraph.buttonNode, "focus", lang.hitch(this, function(){
                    this.showHoverIcon(currGraph.buttonNode, true);
                  }, currGraph)),
                  on(currGraph.buttonNode, "focusout", lang.hitch(this, function(){
                    this.showHoverIcon(currGraph.buttonNode, false);
                  }, currGraph))
              );
            }));
          };
        }

        var browserScrollBarWidth = utils.getScrollBarWidth();

        // Using dijit/Destroyable's "own" method ensures that event handlers are unregistered when the widget is destroyed
        this.own(
            on(this.showHideButton, "click", lang.hitch(this, function(){  
              var divToHide = dom.byId(this.perspective + ID.underscoreDelimit(ID.getShowGraphsDialog(), ID.getContainer()));
              if (domStyle.get(divToHide, "width") != 0) {
                fx.animateProperty({
                  node: this.perspective + "showGraphsDialog_container",
                  properties: {
                    width: 0
                  }
                }).play();
                fx.animateProperty({
                  node: this.perspective + "showGraphsDialog_button",
                  properties: {
                    right: browserScrollBarWidth
                  }
                }).play();
                // set button icon - need to start it from the widget level
                //domClass.replace(this.perspective + 'showHideButton', "showGraphs_openButton", "showGraphs_closeButton");
                var showHideButton = registry.byId(this.perspective + "showHideButton");
                if (showHideButton) {
                    dojo.removeClass(showHideButton.domNode, "showGraphs_closeButton");
                    dojo.addClass(showHideButton.domNode, "showGraphs_openButton");
                    showHideButton.set('iconClass', 'showGraphs_openButtonIcon');
                }
                // For some reason, leaving max-height makes the showHideButton disabled
                domStyle.set(this.perspective + 'showGraphsDialog_container', 'max-height', '');
              } else {
                fx.animateProperty({
                  node: this.perspective + "showGraphsDialog_container",
                  properties: {
                    width: 290,
                    right: browserScrollBarWidth
                  }
                }).play();
                fx.animateProperty({
                  node: this.perspective + "showGraphsDialog_button",
                  properties: {
                    right: 290 + browserScrollBarWidth
                  }
                }).play();
                // set button icon - need to start it from the widget level
                //domClass.replace(this.perspective + 'showHideButton', "showGraphs_closeButton", "showGraphs_openButton");
                var showHideButton = registry.byId(this.perspective + "showHideButton");
                if (showHideButton) {
                    dojo.removeClass(showHideButton.domNode, "showGraphs_openButton");
                    dojo.addClass(showHideButton.domNode, "showGraphs_closeButton");
                    showHideButton.set('iconClass', 'showGraphs_closeButtonIcon');
                }
                domStyle.set(this.perspective + 'showGraphsDialog_container', 'max-height', 'calc(100% - 107px)');
              }
            }))

        );
      }));
    },

    /**
     * Makes sure the selection list is not collapsed to the side
     */
    show : function() {
      var divToHide = dom.byId(this.perspective + ID.underscoreDelimit(ID.getShowGraphsDialog(), ID.getContainer()));
      if (domStyle.get(divToHide, "width") === 0) {
        var scrollBarWidth = utils.getScrollBarWidth();
        var containerWidth = 290;
        domStyle.set(this.perspective + 'showGraphsDialog_container', 'width', containerWidth+'px');
        domStyle.set(this.perspective + 'showGraphsDialog_button', 'right', containerWidth+scrollBarWidth+'px');
        domClass.replace(this.perspective + 'showHideButton', 'showGraphs_closeButton', 'showGraphs_openButton');
      }
    },

    /**
     * Displays all the graphs in the section
     */
    toggleSection : function(section) {
      this.graphData[section].graphs.forEach(lang.hitch(this, function(graphData){
        if (graphData.multipleGraphs) {
          // add an instance of the graph
          this.toggleGraph(graphData.id, true);
        } else {
          // if the graph isn't displayed, display it
          if (! graphData.displayGraph) {
            this.toggleGraph(graphData.id, true);
          }
        }
      }));
      // if this section does not have any graphs that can be added multiple times,
      // then set this section greyed since all graphs are added
      if (!this.graphData[section].multipleGraphs) {
        this.graphData[section].buttonNode.set("disabled", true);
        domClass.toggle(this.graphData[section].buttonNode.domNode, "disabled_button", true);
        domClass.toggle(this.graphData[section].textNode, "disabled", true);
        this.graphData[section].buttonNode.set('iconClass', 'disableGraphButtonIcon');
      }
    },
    
    showHoverIcon: function(buttonNode, showHover) {
        if (!buttonNode.get('disabled')) {
            if (showHover) {
                buttonNode.set('iconClass', 'hoverEnableGraphButtonIcon');
            } else {
                buttonNode.set('iconClass', 'enableGraphButtonIcon');
            }
        }
    },

    /**
     * Checks the graphs in the section. If all are displayed and the section has no multiple graphs, 
     * then grey the section otherwise it is available
     */
    __checkSectionGraphsDisplayed : function(section) {
      var allDisplayed = true; 
      this.graphData[section].graphs.forEach(lang.hitch(this, function(graphData){
        // if the graph isn't displayed, allDisplayed is false
        if (this.__graphInPerspective(graphData) && !graphData.displayGraph) {
          allDisplayed = false;
        }
      }));
      if (allDisplayed  && !this.graphData[section].multipleGraphs) {
        domClass.toggle(this.graphData[section].buttonNode.domNode, "disabled_button", true);
        domClass.toggle(this.graphData[section].textNode, "disabled", true);
        this.graphData[section].buttonNode.set("disabled", true);
        this.graphData[section].buttonNode.set('iconClass', 'disableGraphButtonIcon');
      } else {
        domClass.toggle(this.graphData[section].buttonNode.domNode, "disabled_button", false);
        domClass.toggle(this.graphData[section].textNode, "disabled", false);
        this.graphData[section].buttonNode.set("disabled", false);
        this.graphData[section].buttonNode.set('iconClass', 'enableGraphButtonIcon');
      }
    },

    /**
     * Displays/hides the graph and updates the dialog option.
     * If the graph is currently displayed, it is hidden and vice versa.
     */
    toggleGraph : function(graphId, stateChanged) {
      for (var section in this.graphData) {
        if (this.graphData.hasOwnProperty(section)) {
          this.graphData[section].graphs.forEach(lang.hitch(this, function(graphData){
            // find the stanza for the graph
            if (graphId.indexOf(graphData.graphType) > -1) {
                if (stateChanged) {
                  this.modified = true;
                }
                if (graphData.multipleGraphs) {
                  // if the graphId equals the base section id, then this is adding a new instance
                  if (graphId === graphData.id) {
                    // this is adding a new graph
                    var statGraph = graphData.buildGraph(graphData, true);
                    if (statGraph) {
                      this.__enableGraph(statGraph);
                    }
                  } else {
                    // deleting. Need to really delete since adding a new one will reset to all "parts"
                    // remove from graphData
                    this.__deleteMultiGraphData(graphId, graphData);
                    // destroy graph
                    registry.byId(graphId).stopPolling();
                    registry.byId(graphId).destroyRecursive();
                  }
                } else {
                  if (graphData.displayGraph) {
                    graphData.displayGraph = false;
                  } else {
                    graphData.displayGraph = true;
                  }

                  var graph = this.__displayDefaultGraph(graphData);
                  // may not be defined if it was from a deferred call
                  if (graph) {
                    if (graphData.displayGraph) {
                      graph.startEdit();
                      // tell the graph it is new
                      graph.setNew(true);
                    } else { 
                      graph.endEdit();
                    }
                  }
                }
            }
          }));
        }
      }
    },

    /**
     * Determines which options are applicable to display/enable
     * for the specified resource 
     */
    checkForAvailableOptions : function(buildGraphs) {
      // first show the warningMessage
      this.__showWarningMessage();
      // These options only apply to a server
      if (this.resource.type === 'server' || this.resource.type === 'standaloneServer') {
        if (this.__graphInPerspective(this.graphData.JVM)) {
          this.__checkForJVMOptions(buildGraphs, this.resource);
        }
        if (this.__graphInPerspective(this.graphData.ThreadPool)) {
          this.__checkForMBeanOptions(buildGraphs, this.resource, pmiStats.getThreadPoolExecutorsForServer, "ThreadPool", this.threadPoolParentNode);
        }
        if (this.__graphInPerspective(this.graphData.Connection)) {
          this.__checkForMBeanOptions(buildGraphs, this.resource, pmiStats.getDataSourcesForServer, "Connection", this.connPoolParentNode);
        }
// DISABLE_ANALYTICS
//        if (this.__graphInPerspective(this.graphData.LAAccessLog) || this.__graphInPerspective(this.graphData.LAMsgsTr)) {
//          this.__checkForLogAnalyticsOptions(buildGraphs, this.resource);
//        }
        if (this.__graphInPerspective(this.graphData.Session)) {
          this.__checkForMBeanOptions(buildGraphs, this.resource, pmiStats.getServerSessions, "Session", this.sessionMgmtParentNode);
        }
      }
      
      // These options only apply to a cluster
// DISABLE_ANALYTICS
//      else if (this.resource.type === 'cluster') {
//        if (this.__graphInPerspective(this.graphData.LAAccessLog) || this.__graphInPerspective(this.graphData.LAMsgsTr)) {
//          this.__checkForLogAnalyticsOptions(buildGraphs, this.resource);
//        }
//      }
//
//      // These options only apply to a host
//      else if (this.resource.type === 'host') {
//        if (this.__graphInPerspective(this.graphData.LAAccessLog) || this.__graphInPerspective(this.graphData.LAMsgsTr)) {
//          this.__checkForLogAnalyticsOptions(buildGraphs, this.resource);
//        }
//      }

      // These options only apply to an application
      else if (this.resource.type === 'appOnServer') {
        this.__checkForWCOptions(buildGraphs, this.resource, this.resource.server);
//        this.__checkForSessionMgmtOptions(this.resource, this.resource.server);
//        this.__checkForJAXWSOptions(this.resource, this.resource.server);
      }
    },

    /**
     * Rereads the persisted config and reloads the graphs
     */
    resetGraphs : function() {
      if (this.modified) {
        // delete any multigraphs to prevent any duplicate id issues
        this.__destroyMultiGraphs();
        // get the user config. Once this has been loaded, continue populating the graphs.
        userConfig.load(lang.hitch(this, function() {
          this.modified = false;
          if(! this.persistenceFileExists) {
            // There is an inital case where we reset the graphs 
            // but we do not have a saved user configuration yet.
            this.__rebuildAllGraphs();
          }
          // build the graphs
          this.checkForAvailableOptions(true);
        }), function(err) {
          console.log("empty data", err);
        });
      }
    },
    
    /**
     * This method will restore all of the graphData back to the original settings defined in postCreate().
     * This will disable all visible graphs
     */
    __rebuildAllGraphs : function() {
      // Assuming the graphData contains exactly what is defined in postCreate.  postCreate defines the
      // "default" values for graphData.
      var keys = Object.keys(this.graphData);
      keys.forEach(lang.hitch(this, function(section){
        this.graphData[section].graphs.forEach(lang.hitch(this, function(graphData){
          var statGraph = registry.byId(graphData.id);
          if(statGraph) {
            this.__disableGraph(statGraph);
            this.__enableDisableOption(graphData);
          }
        }));
      }));
    },

    /**
     * Hides JVM options if resource is not a server 
     */
    __checkForJVMOptions : function(buildGraphs, resource) {
      if ((resource.type === 'server' && resource.state === "STARTED") || resource.type === 'standaloneServer') {
        // For Node.js, remove the JVM heading
        if (resource.runtimeType === 'Node.js') {
          domClass.replace(this.jvmHeading, "categorySeparatorHidden", "categorySeparator");
        }
        this.__hideWarningMessage();
        domClass.replace(this.jvmParentNode, "displayGraph", "hideGraph");
        if (buildGraphs) {
          this.__displayDefaultSectionGraphs("JVM");
        }
      } else {
        domClass.replace(this.jvmParentNode, "hideGraph", "displayGraph");
      }
      // Since we know the JVM section does not support multiple graphs, check to see if the section button
      // should be disabled.
      this.__checkSectionGraphsDisplayed("JVM");
    },

    /**
     * Hide the section options if the MBean is
     * not available or not applicable to resource
     */
    __checkForMBeanOptions : function(buildGraphs, resource, deferredGetFunction, section, parentNode) {
      // Check if the response contains the MBean.'
      // If so, display them. The default is that these options are hidden
      if ((resource.type === 'server' || resource.type === 'standaloneServer') && pmiStats.isPMIEnabled(resource)) {
        deferredGetFunction(resource).then(lang.hitch (this, function(resp) {
          if (resp !== null && resp.length > 0) {
            this.__hideWarningMessage();
            // Display the graph.
            domClass.replace(parentNode, "displayGraph", "hideGraph");
            if (buildGraphs) {
              this.__displayDefaultSectionGraphs(section);
            }
          } else {
            // hide the section
            // need to get rid of any graphs
            domClass.replace(parentNode, "hideGraph", "displayGraph");
            if (buildGraphs) {
              this.__clearSectionGraphs(section);
            }
          }
        }));
      } else {
        // hide the section
        domClass.replace(parentNode, "hideGraph", "displayGraph");
        // need to get rid of any graphs
        if (buildGraphs) {
          this.__clearSectionGraphs(section);
        }
      }
    },
    
    /**
     * Hides Log Analytics (LA) options. 
     * 
     * Enable LA if the resource is a server and logAnalyticsUtils.isLogAnalyticsEnabled() returns true
     * 
     */
// DISABLE_ANALYTICS
//    __checkForLogAnalyticsOptions : function(buildGraphs, resource) {
//      
//      // TODO Need to add code to ensure that the log analytics should be displayed.
//      var timeSelector = registry.byId(this.perspective + resource.id + "-TimeSelector");
//      // If log analytics feature is enabled, assume at least 1 graph is available
//      // TODO: handle cluster and host
//      if ( logAnalyticsUtils.isLogAnalyticsEnabled(resource) ) { //&& (resource.type === 'server' || resource.type === 'standaloneServer')) {
//        this.__hideWarningMessage();
//        // TODO list the pipes and ensure that these are available
//        // get the list of graphs/pipes from the REST api
//        logAnalyticsUtils.getAllAvailablePipes().then(lang.hitch(this, function(pipes) {
//          // for each pipe, enable the section parent node and node
//          var msgsSectionEnabled = false;
//          var accessSectionEnabled = false;
//          // forEach returned pipe, switch on the id (for now hard code for the ones we know
//          pipes.forEach(lang.hitch(this, function(p) {
//            // TODO: The following should probably be a function to get rid of all the duplicate code
//            // TODO: Also don't like the hard coded index into graphs but there is no label currently
//            switch (p.id) {
//              case 'access_count':
//                if (this.__graphInPerspective(this.graphData.LAAccessLog.graphs[0])) {
//                  accessSectionEnabled = true;
//                  domClass.replace(this.perspective + "accessLogGraphOption", "", "hideGraph");
//                  domClass.replace(this.accessLogParentNode, "displayGraph", "hideGraph");
//                }
//                break;
//              case 'access_table':
//                if (this.__graphInPerspective(this.graphData.LAAccessLog.graphs[1])) {
//                  accessSectionEnabled = true;
//                  domClass.replace(this.perspective + "accessLogSummaryOption", "", "hideGraph");
//                  domClass.replace(this.accessLogParentNode, "displayGraph", "hideGraph");
//                }
//                break;
//              case 'messages_count':
//                if (this.__graphInPerspective(this.graphData.LAMsgsTr.graphs[0])) {
//                  msgsSectionEnabled = true;
//                  domClass.replace(this.perspective + "messagesGraphOption", "", "hideGraph");
//                  domClass.replace(this.messagesParentNode, "displayGraph", "hideGraph");
//                }
//                break;
//              case 'messages_table':
//                if (this.__graphInPerspective(this.graphData.LAMsgsTr.graphs[1])) {
//                  msgsSectionEnabled = true;
//                  domClass.replace(this.perspective + "messagesTableOption", "", "hideGraph");
//                  domClass.replace(this.messagesParentNode, "displayGraph", "hideGraph");
//                }
//                break;
//              case 'ffdc_count':
//                if (this.__graphInPerspective(this.graphData.LAMsgsTr.graphs[2])) {
//                  msgsSectionEnabled = true;
//                  domClass.replace(this.perspective + "FFDCGraphOption", "", "hideGraph");
//                  domClass.replace(this.messagesParentNode, "displayGraph", "hideGraph");
//                }
//                break;
//              case 'ffdc_table':
//                if (this.__graphInPerspective(this.graphData.LAMsgsTr.graphs[3])) {
//                  msgsSectionEnabled = true;
//                  domClass.replace(this.perspective + "FFDCTableOption", "", "hideGraph");
//                  domClass.replace(this.messagesParentNode, "displayGraph", "hideGraph");
//                }
//                break;
//              case 'trace_count':
//                if (this.__graphInPerspective(this.graphData.LAMsgsTr.graphs[4])) {
//                  msgsSectionEnabled = true;
//                  domClass.replace(this.perspective + "traceLogGraphOption", "", "hideGraph");
//                  domClass.replace(this.messagesParentNode, "displayGraph", "hideGraph");
//                }
//                break;
//              case 'trace_table':
//                if (this.__graphInPerspective(this.graphData.LAMsgsTr.graphs[5])) {
//                  msgsSectionEnabled = true;
//                  domClass.replace(this.perspective + "traceLogTableOption", "", "hideGraph");
//                  domClass.replace(this.messagesParentNode, "displayGraph", "hideGraph");
//                }
//                break;
//              default :
//                console.error("unsupported analytics graph: " + p.id);
//                break;
//            }
//          }));
//          
//          if (buildGraphs) {
//            if (accessSectionEnabled) {
//              this.__displayDefaultSectionGraphs("LAAccessLog");
//            }
//            if (msgsSectionEnabled) {
//              this.__displayDefaultSectionGraphs("LAMsgsTr");
//            }
//            // show the timeSlider if it is there
//            if (timeSelector) {
//              domClass.replace(timeSelector.domNode, "graphTimeSelectorDisplay", "graphTimeSelectorHide");
//            }
//          }
//        }));
//      } else {
//        // if there is a timeSlider, hide it too
//        if (timeSelector) {
//          domClass.replace(timeSelector.domNode, "graphTimeSelectorHide", "graphTimeSelectorDisplay");
//        }
//        console.log("Log analytics not available at the moment");
//      }
//    },

    /**
     * Hides web container options if ServletStats MBean not 
     * available or not applicable to resource
     */
    __checkForWCOptions : function(buildGraphs, resource, server) {
      // Check if the response contains the "ServletStats" MBean.
      // If so, display the "Web Container" options.
      if (resource.type === 'appOnServer' && pmiStats.isPMIEnabled(server)) {
        // The prevailing thinking is that if this is empty, then ServletStats not available
        // and therefore we will not display web container options.
        pmiStats.getServletsForApp(resource.name, server).then(lang.hitch(this, function(resp) {
          
          if (resp != null && resp.length > 0) {
            console.log("Will display web container options!");
            this.__hideWarningMessage();
            domClass.replace(this.wcParentNode, "displayGraph", "hideGraph");
            if (buildGraphs) {
              this.__displayDefaultSectionGraphs("Servlet");
            }
          } else {
            // hide the section
            if (this.wcParentNode) {
              domClass.replace(this.wcParentNode, "hideGraph", "displayGraph");
            }
            // need to get rid of any graphs
            if (buildGraphs) {
              this.__clearSectionGraphs("Servlet");
            }
          }
        }));
      } else {
        // hide the section
        domClass.replace(this.wcParentNode, "hideGraph", "displayGraph");
        // need to get rid of any graphs
        if (buildGraphs) {
          this.__clearSectionGraphs("Servlet");
        }
      }
    },

    /**
     * Hides JAX-WS web services options if JAX-WS PMI MBean 
     * not available or not applicable to resource
     */
    __checkForJAXWSOptions : function(buildGraphs, resource, server) {
      // Check if the response contains the "ServletStats" MBean.
      // If so, display the "JAX-WS Web Services" options
      if (resource.type === 'appOnServer' && pmiStats.isPMIEnabled(server)) {
      //this.__hideWarningMessage();
        console.log("JAX-WS Stats MBean not implemented yet so disabling whether the resource has these endpoints or not :P");
      }
    },

    /**
     * If any graphs in the section are displayed, remove them
     */
    __clearSectionGraphs : function(section) {
      this.graphData[section].graphs.forEach(lang.hitch(this, function(graphData){
        // if the graph is displayed, toggle it
        if (graphData.displayGraph) {
          this.toggleGraph(graphData.id, false);
        }
      }));
    },

    /**
     * Checks whether the graph is displayed and enables or disables the associated dialog item
     */
    __enableDisableOption : function(graphData) {

      var graph = registry.byId(this.perspective + graphData.id);
      if (!graphData.multipleGraphs && graph && domClass.contains(graph.domNode, "displayGraph")) {
        domClass.toggle(graphData.buttonNode.domNode, "disabled_button", true);
        domClass.toggle(graphData.textNode, "disabled", true);
        graphData.buttonNode.set("iconClass", "disableGraphButtonIcon");
        graphData.buttonNode.set("disabled", true);
        graphData.displayGraph = true;
      } else {
        domClass.toggle(graphData.buttonNode.domNode, "disabled_button", false);
        domClass.toggle(graphData.textNode, "disabled", false);
        graphData.buttonNode.set("iconClass", "enableGraphButtonIcon");
        graphData.buttonNode.set("disabled", false);
        graphData.displayGraph = false;
      }
      this.__checkSectionGraphsDisplayed(graphData.graphSection);
    },

    /**
     * Build and display a default graph
     * @param graphData stanza of graphData for the specific graph
     */
    __displayDefaultGraph : function(graphData) {
      var statGraph = registry.byId(this.perspective + graphData.id);
      if (this.__graphInPerspective(graphData)) {
        if (graphData.displayGraph) {
          if(! statGraph) {
            // if the graph does not exist, need to create it
            statGraph = graphData.buildGraph(graphData, false);
          }
          if (statGraph) {
            //  make the graph visible
            this.__enableGraph(statGraph);
          }
        } else if (statGraph){
          // if the graph exists, set it to not displayed
          this.__disableGraph(statGraph);
        }
        if (statGraph) {
          this.__enableDisableOption(graphData);
        }
      } else {
        // make sure the option is not in the list
        domClass.replace(graphData.tableNode, "hideGraph", "showGraphsOptionTable");
      }
      return statGraph;
    },
    
    __enableGraph: function(graph) {
      domClass.replace(graph.domNode, "displayGraph", "hideGraph");
      graph.startPolling();
    },
    
    __disableGraph: function(graph) {
      // if the graph exists, set it to not displayed
      domClass.replace(graph.domNode, "hideGraph", "displayGraph");
      graph.stopPolling();
    },

    /**
     * Calls displayDefaultGraph for each graph in the given section
     * @param section String matching graphSection in the graphData stanzas
     */
    __displayDefaultSectionGraphs : function(section) {
      this.graphData[section].graphs.forEach(lang.hitch(this, function(graphData){

        // if this is the JVM section and this is a nodeJS server, only display heap and CPU
        if (section === "JVM" && this.nodeJSResource && !(graphData.graphType === "HeapStats" || graphData.graphType === "ProcessCPUStats")) {
          // make sure the option is not in the list
          domClass.replace(graphData.tableNode, "hideGraph", "showGraphsOptionTable");
        } else {
          // If the graphData.defaultDisplay is set, set the flag to display the graph to true. The defaultDisplay is set for graphs that should
          // be displayed the 1st time you go to a resource metric view, and also for each graph configured in a previous user session, has been loaded.
          if (graphData.defaultDisplay) {
            graphData.displayGraph = "true";
          }
        
          this.__displayDefaultGraph(graphData);
        }
      }));
    },

    /**
     * Build a graph that does not require a deferred.
     * @param graphData stanza of graphData for the specific graph
     * @returns {graphClass} the object found/created
     */
    __buildSimpleGraph : function(graphData) {
      //console.error("this", this);
      // "this" is a graphData object
      // TODO: this is not the right id, needs perspective
      var thisObj = this.showGraphsDialog; //registry.byId(this.perspective + "showGraphsDialogId");
      //console.error("thisObj " + this.perspective + "showGraphsDialogId", thisObj);
      var graphsPane = registry.byId(thisObj.perspective + ID.dashDelimit(thisObj.resource.id, ID.getGraphsPaneUpper()));
      // TODO: no perspective on this id
      var statGraph = registry.byId(graphData.id);
      if (!statGraph) {
        statGraph = new graphData.graphClass([thisObj.resource, graphsPane, thisObj.perspective]);
      }
      return statGraph;
    },

    /**
     * Build a graph that involves calling a deferred function.
     * If the graph is built, then the button and option parameters must be passed in.
     * For now, this method handles building graphs that have 1 extra parameter
     * @param graphData stanza of graphData for the specific graph
     * @returns {graphClass}
     */
    __buildDeferredGraph : function(graphData, createNew) {
      // TODO: this is not the right id, needs perspective
      var thisObj = this.showGraphsDialog; //registry.byId(this.perspective + "showGraphsDialogId");
      var graphsPane = registry.byId(thisObj.perspective + ID.dashDelimit(thisObj.resource.id, ID.getGraphsPaneUpper()));
      // TODO: no perspective on this id
      var statGraph = registry.byId(graphData.id);
      var param1 = thisObj.resource;
      var param2 = null;
      var server = thisObj.resource;
      if (thisObj.resource.type === 'appOnServer') {
        param1 = thisObj.resource.name;
        param2 = thisObj.resource.server;
        server = thisObj.resource.server;
      }
      if (!statGraph) {
        // since this is deferred, assume they really want it to be visible
        graphData.deferredFunction(param1, param2).then(lang.hitch(thisObj, function(resp) {
          if (resp != null && resp.length > 0) {
            if (graphData.multipleGraphs) {
              if (createNew || graphData.graphInstances.length === 0) {
                // if this is to create a new instance of a multiInstance graph
                // if not in edit mode, then must be default so select all (pass all resp options)
                var options = resp;
                // if the dialog is displayed, we are in edit mode
                if (thisObj.get("style") === "display:block") {
                  options = [];
                }
                graphData.graphInstancesIndex++;
                if (graphData.graphParams.length === 1) {
                  statGraph = new graphData.graphClass([server, graphsPane, thisObj.perspective, graphData.graphParams[0], resp, options, graphData.graphInstancesIndex]);
                } else {
                  // just use 2 parameters
                  statGraph = new graphData.graphClass([server, graphsPane, thisObj.perspective, graphData.graphParams[0], graphData.graphParams[1], resp, options, graphData.graphInstancesIndex]);
                }
                // add an instance
                graphData.graphInstances.push({id: statGraph.id, instance:graphData.graphInstancesIndex, data: options});
              } else {
                // this is to create an instance for previously configured instances of a multiInstance graph
                graphData.graphInstances.forEach(lang.hitch(this, function(graphInstance) {
                  if (graphData.graphParams.length === 1) {
                    statGraph = new graphData.graphClass([server, graphsPane, thisObj.perspective, graphData.graphParams[0], resp, graphInstance.data, graphInstance.instance]);
                  } else {
                    // just use 2 parameters
                    statGraph = new graphData.graphClass([server, graphsPane, thisObj.perspective, graphData.graphParams[0], graphData.graphParams[1], resp, graphInstance.data, graphInstance.instance]);
                  }
                  graphInstance.id = statGraph.id;
                  thisObj.__enableGraph(statGraph);
                  thisObj.__enableDisableOption(graphData);
                  // if the dialog is displayed, we are in edit mode
                  if (thisObj.get("style") === "display:block") {
                    statGraph.startEdit();
                    // tell the graph it is new
                    statGraph.setNew(true);
                  }
                }));
              }
            } else if (graphData.graphParams.length === 1) {
              statGraph = new graphData.graphClass([server, graphsPane, thisObj.perspective, graphData.graphParams[0], resp]);
            } else {
              // just use 2 parameters
              statGraph = new graphData.graphClass([server, graphsPane, thisObj.perspective, graphData.graphParams[0], graphData.graphParams[1], resp]);
            }
            thisObj.__enableGraph(statGraph);
            thisObj.__enableDisableOption(graphData);
            // if the dialog is displayed, we are in edit mode
            if (thisObj.get("style") === "display:block") {
              statGraph.startEdit();
              // tell the graph it is new
              statGraph.setNew(true);
            }
          }
        }));
      }
      return statGraph;
    },
    
    /**
     * Build a logAnalytics graph
     * If the graph is built, then the button and option parameters must be passed in.
     * For now, this method handles building graphs that have 1 extra parameter
     * @param graphData stanza of graphData for the specific graph
     * @returns {graphClass}
     */
// DISABLE_ANALYTICS
//    __buildLogAnalyticGraph : function(graphData, createNew) {
//      // TODO: this is not the right id, needs perspective
//      var thisObj = this.showGraphsDialog; //registry.byId(this.perspective + "showGraphsDialogId");
//      var graphsPane = registry.byId(thisObj.perspective + thisObj.resource.id + "-GraphsPane");
//      // TODO: not perspective on this id
//      var statGraph = registry.byId(graphData.id);
//      if (!statGraph) {
//        if (graphData.multipleGraphs) {
//          if (createNew || graphData.graphInstances.length === 0) {
//            // if this is to create a new instance of a multiInstance graph
//            // if not in edit mode, then must be default so select all (pass all resp options)
//            // TODO resp was something like a list of servlets before from a deferred call
//            var options = []; //resp;
//            // if the dialog is displayed, we are in edit mode
////            if (thisObj.get("style") === "display:block") {
////              options = [];
////            }
//            graphData.graphInstancesIndex++;
//            statGraph = new graphData.graphClass({id: (thisObj.perspective + graphData.id.substring(this.id.lastIndexOf(",")+1) + graphData.graphInstancesIndex), 
//              title: graphData.title, 
//              parentPane: graphsPane, 
//              pipeName: graphData.pipeName, 
//              resource: thisObj.resource, 
//              perspective: thisObj.perspective,
//              chartType: graphData.chartType,
//              eventType: graphData.eventType});
//            // add an instance
//            graphData.graphInstances.push({id: statGraph.id, instance:graphData.graphInstancesIndex, data: options});
//            thisObj.__enableGraph(statGraph);
//            // if the dialog is displayed, we are in edit mode
//            if (thisObj.get("style") === "display:block") {
//              statGraph.startEdit();
//              // tell the graph it is new
//              statGraph.setNew(true);
//            }
//          } else {
//            // this is to create an instance for previously configured instances of a multiInstance graph
//            graphData.graphInstances.forEach(lang.hitch(this, function(graphInstance) {
//              // TODO: need to use the id without :,/ because d3.select seems to fail if the id has extra chars
//              statGraph = new graphData.graphClass({id: (thisObj.perspective + graphData.id.substring(this.id.lastIndexOf(",")+1) + graphInstance.instance), 
//                title: graphData.title, 
//                parentPane: graphsPane, 
//                pipeName: graphData.pipeName, 
//                resource: thisObj.resource,
//                perspective: thisObj.perspective,
//                chartType: graphData.chartType,
//                keys: graphInstance.data,
//                eventType: graphData.eventType});
//              graphInstance.id = statGraph.id;
//              thisObj.__enableGraph(statGraph);
//            }));
//          }
//        } else {
//          // single instance graph
//          statGraph = new graphData.graphClass({id: thisObj.perspective + graphData.id.substring(this.id.lastIndexOf(",")+1), 
//                                    title: graphData.title, 
//                                    parentPane: graphsPane, 
//                                    pipeName: graphData.pipeName, 
//                                    resource: thisObj.resource, 
//                                    perspective: thisObj.perspective,
//                                    chartType: graphData.chartType,
//                                    eventType: graphData.eventType});
//          graphData.id = graphData.id.substring(this.id.lastIndexOf(",")+1);
//          thisObj.__enableGraph(statGraph);
//          thisObj.__enableDisableOption(graphData);
//          if (thisObj.get("style") === "display:block") {
//            statGraph.startEdit();
//            // tell the graph it is new
//            statGraph.setNew(true);
//          }
//        }
//      };
//      return null;
//    },
    
    /**
     * This hides the graphWarning message
     */
    __hideWarningMessage : function() {
      if (this.graphWarningMsg) {
        domClass.replace(this.graphWarningMsg.domNode, "hideGraph", "displayGraph");
        domClass.replace(registry.byId(this.paneWarningId).domNode, "hideGraph", "displayGraph");
      }
    },
    
    /**
     * This shows the graphWarning message
     */
    __showWarningMessage : function() {
      domClass.replace(this.graphWarningMsg.domNode, "displayGraph", "hideGraph");
      domClass.replace(registry.byId(this.paneWarningId).domNode, "displayGraph", "hideGraph");
    },

    /**
     * Read the persisted user config. This returns a deferred object that gets the data from the server and populates the 
     * GraphData, defaulting any values that are required. This is a wrapper around the userConfig file to get the data from the server
     * and parse it.
     */
    __getGraphData : function() {
      
      var deferred = new Deferred();
        
      // Return the deferred from persistence file helper class, which is getting the current data.
      userConfig.load(lang.hitch(this, function(response) {
        // Parse the response into a JSON object
        this.persistedData = response;
        // Set the persistenceFileExists to true, as this influences how we write data back out.
        this.persistenceFileExists = true;
        // Check to see if there is any saved metrics data in the persisted response. If there is
        // we need to update the graphData in memory model.
        if (this.persistedData !== "" && this.persistedData.metrics) {
          // If we do have data then process this.
            var graphData = this.persistedData.metrics;
            // Look for persisted data for this resource id.
            for (var resourceCount = 0; resourceCount < graphData.length; resourceCount++) {
              if (graphData[resourceCount].resource === this.resource.id) {

                // If we find an entry for this resource ID, iterate over all graphs and set all defaultDisplay 
                // to be false, so that only the graphs saved in the persisted data are displayed.
                for (var section in this.graphData) {
                  var graphs = this.graphData[section].graphs;
                  for (var graphCount = 0; graphCount < graphs.length; graphCount++) {
                    graphs[graphCount].defaultDisplay = false;
                    graphs[graphCount].displayGraph = false;
                  }
                }
                // Now load the persisted Data into the graph data.
                if (this.perspective && this.perspective.length > 0) {
                  for (var perspective in graphData[resourceCount]) {
                    if (perspective === this.perspective) {
                      this.__persistedDataToGraphData(graphData[resourceCount][this.perspective].graphs);
                    }
                  }
                } else {
                  this.__persistedDataToGraphData(graphData[resourceCount].graphs);
                }
              }
            }
        }
        deferred.resolve();
      }), lang.hitch(this, function(err) {
        // If we get a HTTP 404 it means that the persistence file doesn't exist, and we need to set the variable to false.
        this.persistenceFileExists = false;
        deferred.resolve();
      }));
      
      return deferred;
    },
    
    /**
     * Wrapper around the userConfig save function
     * This method saves the currently displayed graphs to the server side persistence file.
     */
    saveGraphData : function() {
      
      var deferred = new Deferred();
      
      // If we have persisted data stored, we should attempt to get it, incase it has been created since we initially found it didn't exist.
      var persistedDataEmpty = true;
      for(var key in this.persistedData){
        if(this.persistedData.hasOwnProperty(key)){
          persistedDataEmpty = false;
          break;
        }
      }
      
      var me = this;
      
      var save = function(){
        // Convert our graph data to the persisted JSON format
        var currentGraphConfig = me.__graphDataToPersistedData();
        
        me.__updatePersistedData(currentGraphConfig);
        
        // If the userConfig is successful on saving, then update modified flag to false
        userConfig.save("metrics", me.persistedData.metrics, function(){
          me.modified = false;
          deferred.resolve();
        });
      
      };
      
      // Get persisted data and update our copy
      if (persistedDataEmpty) {
        this.__getGraphData().then(lang.hitch(this, function(){
          save();
        }));
      }
      else{
        save();
      }
      
      return deferred;
      
    },
    
    /*
     * This method creates the new JSON object to return to the server. This is done in a separate function because when we write out the
     * data we may send back stale data, which will mean that we need to re-get the up to date data and then re-calculate the JSON to send back.
     * 
     */
    __updatePersistedData: function(graphConfig) {

      // If we don't have any metrics listed in our persisted object, we need to create it.
      if (! this.persistedData.metrics) {
        this.persistedData.metrics = new Array();
      }
      // We need to find an existing entry for our resource. 
      var resourceFound = false;
      var resourcePersistedData = null;
      var metrics = this.persistedData.metrics;
      // Iterate over all the resource elements looking for a match for this current resource.
      for (var resourceCount = 0; resourceCount < metrics.length && ! resourceFound; resourceCount++) {
        if (metrics[resourceCount].resource === this.resource.id) {
          resourcePersistedData = metrics[resourceCount];
          resourceFound = true;
        }
      }
  
      // If we don't find a match, then create a new entry in the metrics array. If we do find one, replace the graph info in that resource object.
      if (resourcePersistedData === null) {
        if (this.perspective && this.perspective.length > 0) {
          var persistedPerspective = new Object();
          persistedPerspective.push({"graphs": graphConfig});
          metrics.push({"resource": this.resource.id});
          metrics[this.perspective] = persistedPerspective;
        } else {
          // no perspective so do it the old way
          metrics.push({"resource": this.resource.id, "graphs": graphConfig});
        }
      } else {
        if (this.perspective && this.perspective.length > 0) {
          if (!resourcePersistedData[this.perspective]) {
            resourcePersistedData[this.perspective] = new Object();
          }
          resourcePersistedData[this.perspective].graphs = graphConfig;
        } else {
          // no perspective so do it the old way
          resourcePersistedData.graphs = graphConfig;
        }
      }
    },

    /**
     * Convert the persisted data into the graph data model. The data passed will be all graphs for this resource id.
     *  
     */
    __persistedDataToGraphData : function(persistedResourceData) {
      // check that we have some data, and that there is a persistedData.graphs value in it.
      if (persistedResourceData && persistedResourceData != null) {
        // Iterate over each graph type in the persisted data.
        for (var resourceGraphCount = 0; resourceGraphCount < persistedResourceData.length; resourceGraphCount++) {
         
          // Find the section, type and list of instances for the current graph
          var graphSection = persistedResourceData[resourceGraphCount].graphSection;
          var graphType = persistedResourceData[resourceGraphCount].graphType;
          var graphInstances = persistedResourceData[resourceGraphCount].graphInstances;
          // Check that the graph section is defined. This could happen if the user had a beta version
          // and saved any of the analytics graphs. Those sections are commented out while analytics is disabled.
          if (this.graphData[graphSection]) {
            // Check to see whether this section had multiple graph capabilities
            var multipleGraphs = this.graphData[graphSection].multipleGraphs;

            // Get the in memory model graph that maps to this persisted version. We'll update this with the values.
            var graphDataGraph = this.__getGraph(graphSection, graphType);

            if (multipleGraphs) {
              // Always create a new array as we don't want existing values loaded in there.
              graphDataGraph.graphInstances = new Array();
              graphDataGraph.graphInstancesIndex = 0;
            }

            // Iterate over all the graph instances. We store non-multi graph instances in the graph instances var, e.g. JVM graphs,
            // but obviously there will only be 1 instance listed.
            for (var instanceCount = 0; instanceCount < graphInstances.length; instanceCount++) {

              // Get the current instance 
              var currInstance = graphInstances[instanceCount];

              if (graphDataGraph !== null) {
                // If we have graph size data, then set this. Otherwise use 0 for width and height
                var graphWidth = 0;
                if (currInstance.width)
                  graphWidth = currInstance.width;

                var graphHeight = 0;
                if (currInstance.height)
                  graphHeight = currInstance.height;

                // If we have graph order value, then set this. Otherwise use 0.
                var graphOrder = 0;
                if (currInstance.order)
                  graphOrder = currInstance.order;

                // If this is for the multi graphs then we need to add instances. Otherwise we just load info on to the graph object itself.
                if (multipleGraphs) { 

                  // Set the defaultDisplay to true, and increase the instancesIndex.
                  graphDataGraph.defaultDisplay = true;
                  graphDataGraph.graphInstancesIndex++;
                  // If we have selection data, then set this. Otherwise use empty array
                  var graphData = new Array();
                  if (currInstance.data)
                    graphData = currInstance.data;

                  // Push the new instance into the array
                  graphDataGraph.graphInstances.push({instance: graphDataGraph.graphInstancesIndex, data: graphData, width: graphWidth, height: graphHeight, order: graphOrder});

                  // for non-multi instance graphs, just update the graph with the values. 
                } else {
                  graphDataGraph.defaultDisplay = true;
                  graphDataGraph.width = graphWidth;
                  graphDataGraph.height = graphHeight;
                  graphDataGraph.order = graphOrder;
                }
              }
            }
          }
        }
      }
    },
    
    /*
     * This method finds the graph object for the particular section and type.
     * It returns the graph or null if it can't find one.
     */
    __getGraph: function(section, graphType) {
      var foundGraph = null;
      // Iterate over all graphs in the required section looking for a match for the required type.
      for (var graphIter = 0; graphIter < this.graphData[section].graphs.length && foundGraph === null; graphIter++) {
        if (this.graphData[section].graphs[graphIter].graphType === graphType)
          foundGraph = this.graphData[section].graphs[graphIter];
      }
      // Return the graph or null if we haven't found one.
      return foundGraph;
    },

    /**
     * Creates the persistedData from the graphData that we'll send out to the persistence file.
     */
    __graphDataToPersistedData: function() {
      
      // Start with a blank array.
      var newPersistedData = new Array();
      // Iterate over each section in the graphData
      for (var section in this.graphData) {
        if (this.graphData.hasOwnProperty(section)) {
          // For each graph in the section, build up the data for the single instance or 
          // multiple instances.
          this.graphData[section].graphs.forEach(lang.hitch(this, function(currGraph) {
            
            // Create a new array to hold the instances
            var persistedInstances = new Array();
            
            if (this.graphData[section].multipleGraphs && currGraph.multipleGraphs) {
              for (var instances = 0; instances < currGraph.graphInstances.length; instances++) {
                // If the graph is being displayed then add this to the persistedData
                var currInstance = currGraph.graphInstances[instances];
                persistedInstances.push({"order": (currInstance.order) ? currInstance.order : 0,
                                         "width": (currInstance.width) ? currInstance.width : 0,
                                         "height": (currInstance.height) ? currInstance.height : 0,
                                         "data": (currInstance.data) ? currInstance.data : ""});
              };
            } else {
              if (currGraph.displayGraph) {
                persistedInstances.push({"order": (currGraph.order) ? currGraph.order : 0,
                                         "width": (currGraph.width) ? currGraph.width : 0,
                                         "height": (currGraph.height) ? currGraph.height : 0});
                }
            }
            
            // If we have any instances defined, then we need to wrap this in a outer graph object.
            if (persistedInstances.length > 0) {
              newPersistedData.push({"graphSection": section,
                                     "graphType":currGraph.graphType,
                                     "graphInstances": persistedInstances});
            }
          }));
        };
      }
      
      // Once we have built the persisted data, return the new object
      return newPersistedData;
    },

    /**
     * Delete any instances of multi graphs
     */
    __destroyMultiGraphs : function() {
      for (var section in this.graphData) {
        if (this.graphData.hasOwnProperty(section) && this.graphData[section].multipleGraphs) {
          // for each graph in the section, set any multiGraph data
          this.graphData[section].graphs.forEach(lang.hitch(this, function(currGraph){
            if (currGraph.multipleGraphs) {
              if (currGraph.graphInstances && currGraph.graphInstances.length > 0) {
                for (var i = 0; i < currGraph.graphInstances.length; i++) {
                  if (registry.byId(currGraph.graphInstances[i].id)) {
                    registry.byId(currGraph.graphInstances[i].id).stopPolling();
                    registry.byId(currGraph.graphInstances[i].id).destroyRecursive();
                  }
                }
                currGraph.graphInstances = [];
                currGraph.graphInstancesIndex = 0;
              }
            }
          }));
        }
      }
    },

    /**
     * Delete the instance data for a multiGraph
     */
    __deleteMultiGraphData : function(graphId, graphData) {
      for (var i = (graphData.graphInstances.length - 1); i >= 0; i--) {
        if (graphData.graphInstances[i].id === graphId) {
          graphData.graphInstances.splice(i,1);
          break;
        }
      }
      if (graphData.graphInstances.length == 0) {
        graphData.graphInstancesIndex = 0;  // reset only when there are no graphs left
      }
    },

    /**
     * Set the data for the graph in the "right" place
     * Set the modified flag if in edit mode
     */
    updateGraphData : function(graphId, data, editing) {
      // find the right stanza, get the instance, use it to index into the cookie data
      for (var section in this.graphData) {
        if (this.graphData.hasOwnProperty(section) && this.graphData[section].multipleGraphs) {
          this.graphData[section].graphs.forEach(lang.hitch(this, function(currGraph){
            // find the stanza for the graph
            if (currGraph.multipleGraphs) {
              currGraph.graphInstances.forEach(lang.hitch(this, function(inst){
                if (inst.id === graphId) {
                  inst.data = data;
                  if (editing) {
                    this.modified = true;
                  }
                }
              }));
            }
          }));
        }
      }
    },
    
    startup : function() {
      this.resize(); // per dojo documentation, resize should be called by startup
    },
    
    resize : function() {
      this.__movePanelsOffTheScrollBar();
    },
    
    /**
     * Move all the the panels docked to the right side of the browser off the scrollbar.  
     * We don't want to cover the browser's right vertical scrollbar.
     */
    __movePanelsOffTheScrollBar : function () {
      var element = dom.byId(this.perspective + 'showGraphsDialog_container');
      if(element === null) {
        // Do not do anything if the elements are missing
        return;
      }
      
      // Assume that if showGraphsDialog_container exists, then showGraphsDialog_button exists
      
      var rightPadding = domStyle.get(this.perspective + 'showGraphsDialog_container', 'right');
      if(rightPadding === '0px') {
        var scrollBarWidth = utils.getScrollBarWidth();
        var currentButtonRight = domStyle.get(this.perspective + 'showGraphsDialog_button', 'right');
        currentButtonRight = parseInt(currentButtonRight, 10);  // strip off the px at the end
        domStyle.set(this.perspective + 'showGraphsDialog_container', 'right', scrollBarWidth+'px');
        domStyle.set(this.perspective + 'showGraphsDialog_button', 'right', currentButtonRight+scrollBarWidth+'px');
      }
    },

    /**
     * Figure out if the graph or section should be included in the current perspective
     * @param graphData
     * @returns {Boolean}
     */
    __graphInPerspective : function(graphData) {
      var inPerspective = false;
      if (this.perspective === null || this.perspective === '' || this.perspective === 'all') {
        inPerspective = true
      } else if (!graphData.perspectives){
        // the graph has no perspective list
        inPerspective = false;
      } else if (graphData.perspectives.indexOf(this.perspective) > -1) {
        inPerspective = true;
      }
      return inPerspective;
    },
    
    /**
     * Cycle through the graphs that are visible on the UI,
     * turn on each graph's polling
     */
    turnOnGraphs : function () {
      this.__togglePollingOfAllActiveGraphs("on")
    },
    
    /**
     * Cycle through the graphs that are visible on the UI, 
     * turn off each graph's polling
     */
    turnOffGraphs : function () {
      this.__togglePollingOfAllActiveGraphs("off")
    },
    
    /**
     * Go through all the possible choices of graphs and 
     * turn polling off on existing graphs
     * @param action - "on" or "off"
     */
    __togglePollingOfAllActiveGraphs : function(action) {
      var keys = Object.keys(this.graphData);
      keys.forEach(lang.hitch(this, function(section) {
        this.graphData[section].graphs.forEach(lang.hitch(this, function(graphData) {
          var statGraph = registry.byId(graphData.id);
          if (statGraph) {
            if (action === "off") {
              statGraph.stopPolling();
            } else if (action === "on") {
              statGraph.startPolling();
            }
          }
        }));
      }));
    }
    
    
    
  });
});
