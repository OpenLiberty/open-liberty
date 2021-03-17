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
        "dojo/_base/declare",
        "dojo/_base/lang",
        "dojo/json",
        "./ExploreGraph",
        "dijit/_TemplatedMixin",
        "dijit/_WidgetsInTemplateMixin",
        "dojo/i18n!jsExplore/nls/explorerMessages",
        "jsExplore/utils/ID",
        "d3/d3.min",
        "dojo/text!./templates/ConnectionPoolStatsGraph.html",
        "dijit/Tooltip"
        ], function(
                pmiStats,
                declare,
                lang,
                JSON,
                ExploreGraph,
                _TemplatedMixin,
                _WidgetsInTemplateMixin,
                i18n,
                ID,
                d3,
                template,
                Tooltip
            ){

    var USED_COUNT = "ManagedConnectionCount";
    var WAIT_TIME = "WaitTime";
    var FREE_COUNT = "FreeConnectionCount";

    var ConnectionPoolStatsGraph = declare("ConnectionPoolStatsGraph", [ ExploreGraph, _TemplatedMixin, _WidgetsInTemplateMixin ], {
        constructor : function(params) {          // resourceName
            this.graphedStat = params[3];
            this.allConnections = params[4]; 
            this.allDataKeys = this.allConnections;
            this.connections = [];
            if (params[5]) {
              this.connections = params[5];
              this.instance = params[6];
            }
            this.dataKeys = this.connections;
            this.clearChartData();
            if (this.graphedStat === USED_COUNT) {
              this.title = i18n.STATS_CONNECT_IN_USE_TITLE;
              this.yAxisTitle = i18n.STATS_CONNECT_USED_COUNT_Y_AXIS;
            } else {
              this.title = i18n.STATS_CONNECT_WAIT_TIME_TITLE;
              this.yAxisTitle = i18n.STATS_CONNECT_WAIT_TIME_Y_AXIS;
            }
            this.hasLegend = true;
            this.configButtonLabel = i18n.STATS_CONN_POOL_CONFIG_LABEL;
            this.configButtonLabelNum = i18n.STATS_CONN_POOL_CONFIG_LABEL_NUM;
            this.configButtonLabelOne = i18n.STATS_CONN_POOL_CONFIG_LABEL_NUM_1;
            this.configAtLeastOne = i18n.STATS_CONN_POOL_CONFIG_SELECT_ONE;
            this.displayLegend = "true";
            
            // Need to keep track of the latest numbers for each connection
            this.initCountTracking();
        },

        graphKey : "ConnectionStats",
        graphedStat : WAIT_TIME,
        connections : null,
        allConnections : null,
        instance : null,
        templateString : template,

        // { 
        //   jdbc/batch: { ManagedConnectionCount : 0,
        //                 FreeConnectionCount : 0
        //               }
        // }
        allConnectionCounts : {}, // To keep track of the latest set of connection data for summarizing


        postMixInProperties : function() {
          if (this.instance) {
            this.id = ID.getServer() + this.server.id + this.graphedStat + ID.dashDelimit(ID.getConnectionStatsUpper(), this.instance);
            this.chartId = this.graphedStat + "ConnectionStats-" + this.instance;
          } else {
            this.id = ID.getServer() + this.server.id + this.graphedStat + ID.getConnectionStatsUpper();
            this.chartId = this.graphedStat + "ConnectionStats";
          }
          this.idConfigListSelect = this.id + ID.getConfigListSelect();
          this.idConfigList = this.id + ID.getConfigList();
          this.legendNodeId = this.chartId + ID.getLegendNode();
          this.chartNodeId = this.chartId + ID.getChartNode();
          this.idLiveMsgDetailNode = ID.underscoreDelimit(this.id, ID.getLiveMsgDetailNode());
        },

        postCreate : function() {
          var me = this;
          this.inherited(arguments);
          this._setupConfigList(this.connections, this.allConnections);
          if(this.graphedStat === USED_COUNT) {
            this.chart.buildCustomizeTooltip = function(d,i) {
              // Override the default tooltip from parent Chart with a custom one
              content = me.buildCustomizeInUseTooltip(d,i);
            }
          }
        },

        _prepChart : function() {
          this.inherited(arguments);
          if (this.graphedStat === WAIT_TIME) {
              this.chart.data(this.chartData)
              .vAxis({"label":"",
                      "position":"out",
                      "ticks":5,
                      "tickformat": function (d) {
                                      var formatD = d3.format("d");
                                      var formatE = d3.format(".1e");
                                      return ((d/1000000 > 1) ? formatE(d) : formatD(d));
                                    },
                      "tickDisplayAngle":0,"domain": this.yDomain})
          }
        },

        clearChartData: function() {
          this.chartData = [];
        },

        updateResourceList : function(resourceList) {
          this.connections = resourceList;
          this.dataKeys = this.connections;
          // Need add/remove any connections that we were keeping track of for Counts
          this.initCountTracking();
        },

        initCountTracking : function() {
          for(var i = 0; i < this.connections.length; i++) {
            var connectionName = this.connections[i];
            this.allConnectionCounts[connectionName] = {"ManagedConnectionCount" : 0, "FreeConnectionCount" : 0, "CreateCount" : 0, "DestroyCount" : 0}; 
          }
        },

        /**
        * Return an Object that has sum of all of the connections being graphed.
        */
        getSummarizedCounts : function() {
          var summary = {"sumOfManagedConnectionCount": 0, "sumOfFreeConnectionCount": 0, "sumOfCreateCount" : 0,"sumOfDestroyCount" : 0};
          for(var i = 0; i < this.connections.length; i++) {
            var singleConnectionCounts = this.allConnectionCounts[this.connections[i]];
            summary.sumOfManagedConnectionCount += singleConnectionCounts.ManagedConnectionCount;
            summary.sumOfFreeConnectionCount += singleConnectionCounts.FreeConnectionCount;
            summary.sumOfCreateCount += singleConnectionCounts.CreateCount;
            summary.sumOfDestroyCount += singleConnectionCounts.DestroyCount;
          }
          return summary;
        },

      /**
        * Build a custom tooltip when hovering over the plotted line in the graph
        * @param {Number} d - from D3.js, the current datum
        * @param {Number} i - from D3.js, the current index
        */
        buildCustomizeInUseTooltip : function(d, i) {
          var chart = this.chart;
          var chartConfig = chart.config;

          // Get connection name and the connection's data from the point on the graph
          // index d and index i are expected tp be related to each other
          var connectionName = chartConfig.ykeys[d];
          var singleChartData = this.chartData[i];

          // If no data exists for the connection, assume the value is zero
          var freeCount = singleChartData["FreeConnectionCount"][connectionName] || 0;
          var managedCount = singleChartData["ManagedConnectionCount"][connectionName] || 0;

          var dateFormat = chart.globalizeDateFormat("", chartConfig.showUTC);
          var displayDate = new Date(chartConfig.x[i]);
          displayDate = dateFormat(displayDate);
          var formatter = d3.format(".n"); // whole number format
          var msg = lang.replace(i18n.CONNECTION_IN_USE_STATS_VALUE, [formatter(d), formatter(managedCount), formatter(freeCount)]);
          var dataSourceLabel = lang.replace(i18n.DATA_SOURCE, [connectionName]);
          var toolTip = dataSourceLabel + '</br>' + displayDate + '<br/>' + msg;
          return toolTip;
        },

        // Show a tooltip when hovering over the "In Use" summary in the graph title
        showInUseSummaryTooltip: function() {
          if(this.graphedStat === USED_COUNT) {
            var sumOfAllConnectionCounts = this.getSummarizedCounts();
            var free = sumOfAllConnectionCounts.sumOfFreeConnectionCount;
            var managed = sumOfAllConnectionCounts.sumOfManagedConnectionCount;
            var inUse = managed - free;
            var msg = lang.replace(i18n.CONNECTION_IN_USE_STATS, [inUse, managed, free]);
            Tooltip.show(msg, this.usedCountNode);
          }
        },

        // Hide the "In Use" summary tooltip
        hideInUseSummaryTooltip : function() {
          if(this.graphedStat === USED_COUNT) {
            Tooltip.hide(this.usedCountNode);
          }
        },

        generateHistoricalData: function() {
          this.clearChartData();
          this.intervalHandle = setInterval(lang.hitch(this, function() {
            // for each connection
            var me = this;
            var newData = {};
            newData["time"] = new Date().getTime();
            for (var i = 0; i < this.connections.length; i++) {
              // set up a function to pass in the loop variable
              (function(i) {
                var connection = me.connections[i];
                pmiStats.getConnectionPoolStats(me.server, connection).then(function(response) {
                  var responseObj = null;
                  try {
                    responseObj = JSON.parse(response);
                  } catch(e) {
                    // TODO: could be that the mbean no longer exists
                    //console.error(e, response);
                    return;
                  }
                  var countValue = responseObj.ResponseCount;
                  if (me.graphedStat === USED_COUNT) {
                    countValue = responseObj.ManagedConnectionCount - responseObj.FreeConnectionCount;
                  } else {
                    countValue = responseObj.WaitTime;
                  }

                  // If the graph Stat is for Used connection, check to see if the usedCountNode exists or not. When the User has navigated away
                  // from the stats pane, the divs can get removed, and our repeating MBean call can try and write data to a non-existent div.
                  // We don't bother to check all divs. If the usedCountNode is not there, then assume the others aren't as well.
                  if (me.graphedStat === USED_COUNT && me.usedCountNode) {
                    // Fill in summary information

                    // First update all the latest counts from the connections being displayed on the graph
                    me.allConnectionCounts[connection].ManagedConnectionCount = responseObj.ManagedConnectionCount;
                    me.allConnectionCounts[connection].FreeConnectionCount = responseObj.FreeConnectionCount;
                    me.allConnectionCounts[connection].CreateCount = responseObj.CreateCount;
                    me.allConnectionCounts[connection].DestroyCount = responseObj.DestroyCount;

                    // Get a summary view of all the counts across all the connections displayed in the graph
                    var allSummaryCounts = me.getSummarizedCounts();

                    // 
                    var inUseCount = allSummaryCounts.sumOfManagedConnectionCount - allSummaryCounts.sumOfFreeConnectionCount;
                    me.usedCountNode.innerHTML = lang.replace(i18n.STATS_CONNECT_IN_USE_LABEL, [inUseCount]);
                    me.freeCountNode.innerHTML = lang.replace(i18n.STATS_CONNECT_USED_FREE_LABEL, [allSummaryCounts.sumOfFreeConnectionCount]);
                    me.createdCountNode.innerHTML = lang.replace(i18n.STATS_CONNECT_USED_CREATE_LABEL, [allSummaryCounts.sumOfCreateCount]);
                    me.destroyedCountNode.innerHTML = lang.replace(i18n.STATS_CONNECT_USED_DESTROY_LABEL, [allSummaryCounts.sumOfDestroyCount]);

                    // Add these pieces of data along with the data that is graphed.  When inspecting a datapoint on the graph, you
                    // should see these pieces of data also.
                    newData["ManagedConnectionCount"] = {};
                    newData["ManagedConnectionCount"][connection] = responseObj.ManagedConnectionCount;
                    newData["FreeConnectionCount"] = {};
                    newData["FreeConnectionCount"][connection] = responseObj.FreeConnectionCount
                  };

                  newData[connection] = countValue;
                  me.d3addMultiDataPointAndRender(newData, me.chartData);
                });
              })(i);  // end loop function
            }  // end for loop
          }), this.interval);
        }

    });

    return ConnectionPoolStatsGraph;

});
