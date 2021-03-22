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
define([
        "jsExplore/resources/stats/_pmiStats",
        "jsExplore/utils/ID",
        "dojo/_base/declare", 
        "dojo/_base/lang",
        "dojo/json",
        "./ExploreGraph",
        "dijit/_TemplatedMixin",
        "dijit/_WidgetsInTemplateMixin",
        "dojo/i18n!jsExplore/nls/explorerMessages",
        "dojo/text!./templates/PMIThreadPoolGraph.html"
        ], function(
                pmiStats,
                ID,
                declare,
                lang,
                JSON,
                ExploreGraph,
                _TemplatedMixin,
                _WidgetsInTemplateMixin,
                i18n,
                template
            ){

    var PMIThreadPoolGraph = declare("PMIThreadPoolGraph", [ ExploreGraph, _TemplatedMixin, _WidgetsInTemplateMixin ], {
        constructor : function(params) {          // resourceName
            this.chartId = this.server.name + ID.getThreadPoolStatsUpper();
            this.title = i18n.STATS_THREADPOOL_TITLE;
            this.yAxisTitle = i18n.STATS_THREADPOOL_Y_ACTIVE;
            this.chartNodeId = this.chartId + ID.getChartNode();
            this.legendNodeId = this.chartId + ID.getLegendNode();
            this.idLiveMsgDetailNode = ID.underscoreDelimit(this.id, ID.getLiveMsgDetailNode()); 
        },
        
        graphKey : "ThreadPoolStats",
        poolSizeValue : 0,
        poolSizeLabel : i18n.STATS_THREADPOOL_SIZE,
        activeThreadsValue : 0,
        templateString : template,

        generateHistoricalData: function() {
          this.chartData = [];
          this.intervalHandle = setInterval(lang.hitch(this, function() {
            if (this.chartData.length >= this.numberOfSnapshots) {
              this.chartData.shift();
            }

            pmiStats.getThreadPoolStats(this.server, "Default Executor").then(lang.hitch(this, function(response) {
              var responseObj = null;
              try {
                responseObj = JSON.parse(response);
              } catch(e) {
                console.error(response);
                return;
              }
              
              // Check to see if the poolSizeNode exists or not. When the User has navigated away from the stats pane, the divs
              // can get removed, and our repeating MBean call can try and write data to a non-existent div.
              if (this.poolSizeNode) {
                this.poolSizeNode = responseObj.PoolSize;
                this.poolSizeNode.innerHTML = lang.replace(this.poolSizeLabel, [this.poolSizeValue]);
              };
              this.activeThreadsValue = responseObj.ActiveThreads;

              this.addDataPointAndRender(this.activeThreadsValue);
            }));
          }), this.interval);
        }

    });
    return PMIThreadPoolGraph;

});