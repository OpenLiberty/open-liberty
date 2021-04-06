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
        "dojox/form/CheckedMultiSelect",
        "dojo/i18n!jsExplore/nls/explorerMessages",
        "jsExplore/utils/ID",
        "dojo/text!./templates/PMIServletStatsGraph.html"
        ], function(
                pmiStats,
                declare,
                lang,
                JSON,
                ExploreGraph,
                _TemplatedMixin,
                _WidgetsInTemplateMixin,
                CheckedMultiSelect,
                i18n,
                ID,
                template
            ){

    var REQUEST_COUNT = "RequestCount";
    var RESPONSE_COUNT = "ResponseCount";
    var RESPONSE_MEAN = "ResponseMean";   // use for average response time

    var PMIServletStatsGraph = declare("PMIServletStatsGraph", [ ExploreGraph, _TemplatedMixin, _WidgetsInTemplateMixin ], {
        constructor : function(params) {          // resourceName
            this.appName = params[3];
            this.graphedStat = params[4];
            this.allServlets = params[5];
            this.allDataKeys = this.allServlets;
            this.servlets = [];
            if (params[6]) {
              this.servlets = params[6];
              this.instance = params[7];
            }
            this.dataKeys = this.servlets;
            this.clearChartData();
            if (this.graphedStat === REQUEST_COUNT) {
              this.title = i18n.STATS_SERVLET_REQUEST_COUNT_TITLE;
              this.yAxisTitle = i18n.STATS_SERVLET_REQUEST_COUNT_Y_AXIS;
            } else if (this.graphedStat === RESPONSE_MEAN) {
              this.title = i18n.STATS_SERVLET_RESPONSE_MEAN_TITLE;
              this.yAxisTitle = i18n.STATS_SERVLET_RESPONSE_MEAN_Y_AXIS;
            } else {
              this.title = i18n.STATS_SERVLET_RESPONSE_COUNT_TITLE;
              this.yAxisTitle = i18n.STATS_SERVLET_RESPONSE_COUNT_Y_AXIS;
            }
            this.hasLegend = true;
            this.configButtonLabel = i18n.STATS_SERVLET_CONFIG_LABEL;
            this.configButtonLabelNum = i18n.STATS_SERVLET_CONFIG_LABEL_NUM;
            this.configButtonLabelOne = i18n.STATS_SERVLET_CONFIG_LABEL_NUM_1;
            this.configAtLeastOne = i18n.STATS_SERVLET_CONFIG_SELECT_ONE;
            this.displayLegend = "true";
        },

        graphKey : "ServletStats",
        appName : null,
        graphedStat : RESPONSE_COUNT,
        servlets : null,
        allServlets : null,
        instance : null,
        templateString : template,

        postMixInProperties : function() {
          if (this.instance) {
            this.id = ID.getApp() + ID.getResourceOnResource(this.appName, this.server.id) + this.graphedStat + ID.dashDelimit(ID.getServletStatsUpper(), this.instance);
            this.chartId = this.graphedStat + "ServletStats-" + this.instance;
          } else {
            this.id = ID.getApp() + ID.getResourceOnResource(this.appName, this.server.id) + this.graphedStat + ID.getServletStatsUpper();
            this.chartId = this.graphedStat + "ServletStats";
          }
          this.idConfigListSelect = this.id + ID.getConfigListSelect();
          this.idConfigList = this.id + ID.getConfigList();
          this.chartNodeId = this.chartId + ID.getChartNode();
          this.legendNodeId = this.chartId + ID.getLegendNode();
        },

        postCreate : function() {
          this.inherited(arguments);
          this._setupConfigList(this.servlets, this.allServlets);
        },

        _prepChart : function() {
          this.inherited(arguments);
          if (this.graphedStat === RESPONSE_MEAN) {
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
          this.servlets = resourceList;
          this.dataKeys = resourceList;
        },

        generateHistoricalData: function() {

          this.clearChartData();
          this.intervalHandle = setInterval(lang.hitch(this, function() {
            // for each servlet
            var me = this;
            var newData = {};
            newData["time"] = new Date().getTime();
            for (var i = 0; i < this.servlets.length; i++) {
              // set up a function to pass in the loop variable
              (function(i) {
                var servlet = me.servlets[i];
                pmiStats.getServletStats(me.server, servlet).then(function(response) {
                  var responseObj = null;
                  try {
                    responseObj = JSON.parse(response);
                  } catch(e) {
                    // TODO: could be that the mbean no longer exists
                    //console.error(e, response);
                    return;
                  }
                  var servlet = me.servlets[i];
                  var countValue = responseObj.ResponseCount;
                  if (me.graphedStat === REQUEST_COUNT) {
                    countValue = responseObj.RequestCount;
                  } else if (me.graphedStat === RESPONSE_MEAN) {
                    countValue = responseObj.ResponseMean;
                  }

                  newData[servlet] = countValue;
                  if (Object.keys(newData).length === me.servlets.length + 1) {
                    me.d3addMultiDataPointAndRender(newData, me.chartData);
                  }
                });
              })(i);  // end loop function
            }  // end for loop
          }), this.interval);
        }
    });

    return PMIServletStatsGraph;

});
