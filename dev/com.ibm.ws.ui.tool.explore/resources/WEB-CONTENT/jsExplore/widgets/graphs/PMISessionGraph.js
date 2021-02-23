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
        "dojo/text!./templates/PMISessionGraph.html"
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

    var ACTIVE_COUNT = "ActiveCount";

    var PMISessionGraph = declare("PMISessionGraph", [ ExploreGraph, _TemplatedMixin, _WidgetsInTemplateMixin ], {
      constructor : function(params) {          // [resource, pane, "ActiveCount", resp]
          this.graphedStat = params[3];
          this.allSessionNames = params[4];
          this.allDataKeys = this.allSessionNames;
          this.sessionNames = [];
          if (params[5]) {
            this.sessionNames = params[5];
            this.instance = params[6];
          }
          this.dataKeys = this.sessionNames;
          this.clearChartData();
          this.title = i18n.STATS_SESSION_TITLE;
          this.yAxisTitle = i18n.STATS_SESSION_Y_ACTIVE;
          this.hasLegend = true;
          this.configButtonLabel = i18n.STATS_SESSION_CONFIG_LABEL;
          this.configButtonLabelNum = i18n.STATS_SESSION_CONFIG_LABEL_NUM;
          this.configButtonLabelOne = i18n.STATS_SESSION_CONFIG_LABEL_NUM_1;
          this.configAtLeastOne = i18n.STATS_SESSION_CONFIG_SELECT_ONE;
          this.displayLegend = "true";
        },

        graphKey : "SessionStats",
        graphedStat : ACTIVE_COUNT,
        sessionNames : null,
        allSessionNames : null,
        instance : null,
        activeCountValue : 0,
        liveCountValue : 0,
        createCountValue : 0,
        invalidatedCountValue : 0,
        invalidatedByTimeoutCountValue : 0,
        liveCountLabel : i18n.STATS_SESSION_LIVE_LABEL,
        createCountLabel : i18n.STATS_SESSION_CREATE_LABEL,
        invalidatedCountLabel : i18n.STATS_SESSION_INV_LABEL,
        invalidatedByTimeoutCountLabel : i18n.STATS_SESSION_INV_TIME_LABEL,
        templateString : template,

        postMixInProperties : function() {
          var prefix = 'server';
//          if (this.resource.type == 'appOnServer') {
//            prefix = 'app';
//          }
          if (this.instance) {
            this.id = prefix + this.server.id + ID.dashDelimit(ID.getSessionStatsUpper(), this.instance);
            this.chartId = "SessionStats-" + this.instance;
          } else {
            this.id = prefix + this.server.id + ID.getSessionStatsUpper();
            this.chartId = "SessionStats";
          }
          this.idConfigListSelect = this.id + ID.getConfigListSelect();
          this.idConfigList = this.id + ID.getConfigList();
          this.chartNodeId = this.chartId + ID.getChartNode();
          this.legendNodeId = this.chartId + ID.getLegendNode();
          this.idLiveMsgDetailNode = ID.underscoreDelimit(this.id, ID.getLiveMsgDetailNode()); 
        },

        postCreate : function() {
          this.inherited(arguments);
          this._setupConfigList(this.sessionNames, this.allSessionNames);
        },
        
        clearChartData: function() {
          this.chartData = [];
        },

        updateResourceList : function(resourceList) {
          this.sessionNames = resourceList;
          this.dataKeys = this.sessionNames;
        },
        
        generateHistoricalData: function() {
          this.clearChartData();
          this.intervalHandle = setInterval(lang.hitch(this, function() {
            // for each session
            var me = this;
            var newData = {};
            newData["time"] = new Date().getTime();
            for (var i = 0; i < this.sessionNames.length; i++) {
              // set up a function to pass in the loop variable
              (function(i) {
                var session = me.sessionNames[i];

                pmiStats.getSessionStats(me.server, session).then(function(response) {
                  var responseObj = null;
                  try {
                    responseObj = JSON.parse(response);
                  } catch(e) {
                    // TODO: could be that the mbean no longer exists
                    //console.error(e, response);
                    return;
                  }
                  
                  // Check to see if the liveCountNode exists or not. When the User has navigated away from the stats pane, the divs
                  // can get removed, and our repeating MBean call can try and write data to a non-existent div.
                  // We are only checking one of the divs and we'll assume that if it is missing, all the other ones have been 
                  // removed as well.
                  me.activeCountValue = responseObj.ActiveCount;
                  if (this.liveCountNode) {
                    me.liveCountValue = responseObj.LiveCount;
                    me.createCountValue = responseObj.CreateCount;
                    me.invalidatedCountValue = responseObj.InvalidatedCount;
                    me.invalidatedByTimeoutCountValue = responseObj.InvalidatedCountbyTimeout;
                
                    me.liveCountNode.innerHTML = lang.replace(me.liveCountLabel, [me.liveCountValue]);
                    me.createCountNode.innerHTML = lang.replace(me.createCountLabel, [me.createCountValue]);
                    me.invalidatedCountNode.innerHTML = lang.replace(me.invalidatedCountLabel, [me.invalidatedCountValue]);
                    me.invalidatedByTimeoutCountNode.innerHTML = lang.replace(me.invalidatedByTimeoutCountLabel, [me.invalidatedByTimeoutCountValue]);
                  };
 
                  newData[session] = me.activeCountValue;
                  if (Object.keys(newData).length === me.sessionNames.length + 1) {
                    me.d3addMultiDataPointAndRender(newData, me.chartData);
                  }
                });
              })(i);  // end loop function
            }  // end for loop
          }), this.interval);
        }

    });
    return PMISessionGraph;

});