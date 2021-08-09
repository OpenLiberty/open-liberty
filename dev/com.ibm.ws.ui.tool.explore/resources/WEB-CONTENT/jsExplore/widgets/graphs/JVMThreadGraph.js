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
        "dojo/_base/declare", 
        "dojo/_base/lang",
        "dojo/json",
        "dojo/number",
        "./ExploreGraph",
        "dijit/_TemplatedMixin",
        "dijit/_WidgetsInTemplateMixin",
        "dojo/i18n!jsExplore/nls/explorerMessages",
        "jsExplore/utils/ID",
        "dojo/text!./templates/JVMThreadGraph.html"
        ], function(
                jvmStats,
                declare,
                lang,
                JSON,
                number,
                ExploreGraph,
                _TemplatedMixin,
                _WidgetsInTemplateMixin,
                i18n,
                ID,
                template
            ){

    var JVMThreadGraph = declare("JVMThreadGraph", [ ExploreGraph, _TemplatedMixin, _WidgetsInTemplateMixin ], {
        constructor : function(params) {          // resourceName
            this.chartId = this.server.name + ID.getThreadStatsUpper();
            this.chartNodeId = this.chartId + ID.getChartNode();
            this.legendNodeId = this.chartId + ID.getLegendNode();
            this.idLiveMsgDetailNode = ID.underscoreDelimit(this.id, ID.getLiveMsgDetailNode());         
            this.title = i18n.STATS_THREADS_TITLE;
            this.yAxisTitle = i18n.STATS_THREADS_Y_THREADS;
        },

        graphKey : "ThreadStats",
        activeValue : 0,
        activeLabel : i18n.STATS_THREADS_ACTIVE,
        peakValue : 0,
        peakLabel : i18n.STATS_THREADS_PEAK,
        totalValue : 0,
        totalLabel : i18n.STATS_THREADS_TOTAL,
        templateString : template,

        generateHistoricalData: function() {
          this.chartData = [];
          this.intervalHandle = setInterval(lang.hitch(this, function() {
            if (this.chartData.length >= this.numberOfSnapshots) {
              this.chartData.shift();
            }

            jvmStats.getThreads(this.server).then(lang.hitch(this, function(response) {
              var responseObj = null;
              try {
                responseObj = JSON.parse(response);
              } catch(e) {
                console.error(response);
                return;
              }
              
              // Check to see if the activeNode exists or not. When the User has navigated away from the stats pane, the divs
              // can get removed, and our repeating MBean call can try and write data to a non-existent div.
              if (this.activeNode) {
                this.activeValue = responseObj.ThreadCount;
                this.activeNode.innerHTML = lang.replace(this.activeLabel, [number.format(this.activeValue, {
                  places: 0
                })]);
              };
              
              // Check to see if the peakNode exists or not. When the User has navigated away from the stats pane, the divs
              // can get removed, and our repeating MBean call can try and write data to a non-existent div.
              if (this.peakNode) {
                this.peakValue = responseObj.PeakThreadCount;
                this.peakNode.innerHTML = lang.replace(this.peakLabel, [number.format(this.peakValue, {
                  places: 0
                })]);
              };
              
              // Check to see if the totalNode exists or not. When the User has navigated away from the stats pane, the divs
              // can get removed, and our repeating MBean call can try and write data to a non-existent div.
              if (this.totalNode) {
                this.totalValue = responseObj.TotalStartedThreadCount;
                this.totalNode.innerHTML = lang.replace(this.totalLabel, [number.format(this.totalValue, {
                  places: 0
                })]);
              };

              this.addDataPointAndRender(this.activeValue);
            }));
          }), this.interval);
        }

    });
    return JVMThreadGraph;

});