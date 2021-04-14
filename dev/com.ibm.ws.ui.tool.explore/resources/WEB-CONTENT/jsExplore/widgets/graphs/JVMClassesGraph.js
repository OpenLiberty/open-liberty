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
        "dojo/text!./templates/JVMClassesGraph.html"
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

    var JVMClassesGraph = declare("JVMClassesGraph", [ ExploreGraph, _TemplatedMixin, _WidgetsInTemplateMixin ], {
        constructor : function(params) {          // resourceName
            this.chartId = this.server.name + ID.getClassesStatsUpper();
            this.chartNodeId = this.chartId + ID.getChartNode();
            this.legendNodeId = this.chartId + ID.getLegendNode();
            this.idLiveMsgDetailNode = ID.underscoreDelimit(this.id, ID.getLiveMsgDetailNode()); 
            this.title = i18n.STATS_CLASSES_TITLE;
            this.yAxisTitle = i18n.STATS_CLASSES_Y_TOTAL;
        },
        
        graphKey : "ClassesStats",
        loadedValue : 0,
        loadedLabel : i18n.STATS_CLASSES_LOADED,
        unloadedValue : 0,
        unloadedLabel : i18n.STATS_CLASSES_UNLOADED,
        totalValue : 0,
        totalLabel : i18n.STATS_CLASSES_TOTAL,
        templateString : template,

        postCreate : function() {
          this.inherited(arguments);
        },

        generateHistoricalData: function() {
          this.chartData = [];
          this.intervalHandle = setInterval(lang.hitch(this, function() {
            if (this.chartData.length >= this.numberOfSnapshots) {
              this.chartData.shift();
            }

            jvmStats.getLoadedClasses(this.server).then(lang.hitch(this, function(response) {
              var responseObj = null;
              try {
                responseObj = JSON.parse(response);
              } catch(e) {
                console.error(response);
                return;
              }
              // Check to see if the loadedNode exists or not. When the User has navigated away from the stats pane, the divs
              // can get removed, and our repeating MBean call can try and write data to a non-existent div.
              if (this.loadedNode) {
                this.loadedValue = responseObj.LoadedClassCount;
                this.loadedNode.innerHTML = lang.replace(this.loadedLabel, [number.format(this.loadedValue, {
                  places: 0
                })]);
              };
              
              // Check to see if the unloadedNode exists or not. When the User has navigated away from the stats pane, the divs
              // can get removed, and our repeating MBean call can try and write data to a non-existent div.
              if (this.unloadedNode) {
                this.unloadedValue = responseObj.UnloadedClassCount;
                this.unloadedNode.innerHTML = lang.replace(this.unloadedLabel, [number.format(this.unloadedValue, {
                  places: 0
                })]);
              };
              
              
              // Check to see if the totalNode exists or not. When the User has navigated away from the stats pane, the divs
              // can get removed, and our repeating MBean call can try and write data to a non-existent div.
              if (this.totalNode) {
                this.totalValue = responseObj.TotalLoadedClassCount;
                this.totalNode.innerHTML = lang.replace(this.totalLabel, [number.format(this.totalValue, {
                  places: 0
                })]);
              };

              this.addDataPointAndRender(this.loadedValue);
            }));
          }), this.interval);
        }

    });
    return JVMClassesGraph;

});