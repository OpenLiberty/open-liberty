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
        "dojo/text!./templates/JVMHeapGraph.html"
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

    var JVMHeapGraph = declare("JVMHeapGraph", [ ExploreGraph, _TemplatedMixin, _WidgetsInTemplateMixin ], {
        constructor : function(params) {          // resourceName
        	this.chartId = this.server.name + ID.getHeapStatsUpper();
            this.chartNodeId = this.chartId + ID.getChartNode();
            this.legendNodeId = this.chartId + ID.getLegendNode();
            this.idLiveMsgDetailNode = ID.underscoreDelimit(this.id, ID.getLiveMsgDetailNode()); 
            this.title = i18n.STATS_HEAP_TITLE;
            this.yAxisTitle = i18n.STATS_HEAP_Y_MB;
        },

        graphKey : "HeapStats",
        usedValue : 0,
        usedLabel : i18n.STATS_HEAP_USED,
        committedValue : 0,
        committedLabel : i18n.STATS_HEAP_COMMITTED,
        maxValue : 0,
        maxLabel : i18n.STATS_HEAP_MAX,
        templateString : template,

        generateHistoricalData: function() {
          this.chartData = [];
          this.intervalHandle = setInterval(lang.hitch(this, function() {
            if (this.chartData.length >= this.numberOfSnapshots) {
              this.chartData.shift();
            }

            jvmStats.getHeapMemoryUsage(this.server).then(lang.hitch(this, function(response) {
              var responseObj = null;
              try {
                responseObj = JSON.parse(response);
              } catch(e) {
                console.error(response);
                return;
              }
              
              // Check to see if the usedNode exists or not. When the User has navigated away from the stats pane, the divs
              // can get removed, and our repeating MBean call can try and write data to a non-existent div.
              if (this.usedNode) {
                // to convert from bits to Mb for display values, divide by 1024*1024*8
                this.usedValue = responseObj.Used/1048576;
                this.usedNode.innerHTML = lang.replace(this.usedLabel, [number.format(this.usedValue, {
                  places: 1
                })]);
              };
              
              // Check to see if the committedValue exists or not. When the User has navigated away from the stats pane, the divs
              // can get removed, and our repeating MBean call can try and write data to a non-existent div.
              if (this.committedNode) {
                this.committedValue = responseObj.Committed/1048576;
                this.committedNode.innerHTML = lang.replace(this.committedLabel, [number.format(this.committedValue, {
                  places: 1
                })]);
                if(this.committedValue === null || this.committedValue === "null" || this.committedValue === ""){
                  this.committedNode.style.visibility = "hidden";
                }
                else{
                  this.committedNode.style.visibility = "visible";
                }
              };
              
              // Check to see if the maxNode exists or not. When the User has navigated away from the stats pane, the divs
              // can get removed, and our repeating MBean call can try and write data to a non-existent div.
              if (this.maxNode) {
                this.maxValue = responseObj.Max/1048576;
                this.maxNode.innerHTML = lang.replace(this.maxLabel, [number.format(this.maxValue, {
                  places: 1
                })]);
                if(this.maxValue === null || this.maxValue === "null" || this.maxValue === ""){
                  this.maxNode.style.visibility = "hidden";
                }
                else{
                  this.maxNode.style.visibility = "visible";
                }
              };

              this.addDataPointAndRender(this.usedValue);
            }));
          }), this.interval);
        }

    });
    return JVMHeapGraph;

});