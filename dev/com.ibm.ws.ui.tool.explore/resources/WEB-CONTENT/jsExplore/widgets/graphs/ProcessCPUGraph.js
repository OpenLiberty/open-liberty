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
        "jsExplore/resources/stats/_jvmStats",
        "jsExplore/utils/ID",
        "dojo/_base/declare", 
        "dojo/_base/lang",
        "dojo/json",
        "dojo/dom-construct",
        "dojo/number",
        "./ExploreGraph",
        "dijit/_TemplatedMixin",
        "dijit/_WidgetsInTemplateMixin",
        "dojo/i18n!jsExplore/nls/explorerMessages",
        "dojo/text!./templates/ProcessCPUGraph.html"
        ], function(
                jvmStats,
                ID,
                declare,
                lang,
                JSON,
                domConstruct,
                number,
                ExploreGraph,
                _TemplatedMixin,
                _WidgetsInTemplateMixin,
                i18n,
                template
            ){

    var ProcessCPUGraph = declare("ProcessCPUGraph", [ ExploreGraph, _TemplatedMixin, _WidgetsInTemplateMixin ], {
        constructor : function(params) {          // resourceName
            this.chartId = this.server.name + ID.getProcessCPUStatsUpper();
            this.chartNodeId = this.chartId + ID.getChartNode();
            this.legendNodeId = this.chartId + ID.getLegendNode();
            this.idLiveMsgDetailNode = ID.underscoreDelimit(this.id, ID.getLiveMsgDetailNode()); 
            this.title = i18n.STATS_PROCESSCPU_TITLE;
            this.yAxisTitle = i18n.STATS_PROCESSCPU_Y_PERCENT;
        },

        graphKey : "ProcessCPUStats",
        loadValue : -1,
        loadLabel : i18n.STATS_PROCESSCPU_USAGE,
        templateString : template,

        postCreate : function() {
            this.inherited(arguments);
            this.yDomain = [0,100];
            this.yAxisTickSuffix = "%";
        },

        generateHistoricalData: function() {
          this.chartData = [];
          this.intervalHandle = setInterval(lang.hitch(this, function() {
            if (this.chartData.length >= this.numberOfSnapshots) {
              this.chartData.shift();
            }

            jvmStats.getCPUUsage(this.server).then(lang.hitch(this, function(response) {
              var responseObj = null;
              try {
                responseObj = JSON.parse(response);
              } catch(e) {
                console.error(response);
                return;
              }
              
              // Check to see if the loadNode exists or not. When the User has navigated away from the stats pane, the divs
              // can get removed, and our repeating MBean call can try and write data to a non-existent div.
              if (! responseObj.ProcessCpuLoad) {
                // If there hasn't been any data returned yet, it should be safe to stop polling.
                // Otherwise, just do nothing and let it come around again. Hopefully the next call will
                // return data.
                if (this.chartData.length < 2) {
                  this.stopPolling();
                  domConstruct.empty(this.laChartNode);
                  if (this.loadNode) {
                    this.loadNode.innerHTML ="";
                  }
                  var warningMsg = new GraphWarningMessage({}, this.laChartNode);
                  warningMsg.setTextMessage(i18n.NO_CPU_STATS_AVAILABLE);
                }
              } else {
                this.loadValue = (responseObj.ProcessCpuLoad * 100);
                
                if (this.loadValue < 0) {
                  this.loadValue = 0;
                }

                if (this.loadNode) {
                  this.loadNode.innerHTML = lang.replace(this.loadLabel, [number.format(this.loadValue, {
                    places: 1
                  })]);
                };

                this.addDataPointAndRender(this.loadValue);
              }
            }));
          }), this.interval);
        }

    });
    return ProcessCPUGraph;

});