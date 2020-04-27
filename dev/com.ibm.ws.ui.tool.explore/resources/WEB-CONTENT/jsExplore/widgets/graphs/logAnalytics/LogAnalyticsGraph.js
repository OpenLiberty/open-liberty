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
        'dojo/_base/lang',
        'dojo/_base/declare', 
        'dojo/request/xhr',
        "dojo/text!./templates/LogAnalyticsGraph.html",
        'dojo/i18n!jsExplore/nls/explorerMessages',
        "dojo/dom-construct",
        'dijit/registry',
        'dijit/_TemplatedMixin', 
        "dijit/_WidgetsInTemplateMixin",
        'dojox/layout/ContentPane',
        "dojox/form/CheckedMultiSelect",
        'js/common/platform',
        'jsExplore/widgets/graphs/ExploreGraph',
        'jsExplore/widgets/graphs/TimeSelector',
        './charts/LineChart',
        'jsExplore/resources/stats/_logAnalyticsUtils',
        'jsExplore/utils/ID'
        
        ], function(
                lang,
                declare,
                xhr,
                template,
                i18n,
                domConstruct,
                registry,
                _TemplatedMixin, 
                _WidgetsInTemplateMixin,
                ContentPane,
                CheckedMultiSelect,
                platform,
                ExploreGraph,
                TimeSelector,
                LineChart,
                logAnalyticsUtils,
                ID
            ){
    return declare("LogAnalyticsGraph", [ExploreGraph, _TemplatedMixin, _WidgetsInTemplateMixin], {
    
      // The name of the Analytics pipe to call.
      pipeName: null,
      // The graph template
      templateString : template,
      // The query to pass to the pipe. This is used to narrow down the search.
      searchString: "*",
      // The resource that the graph is for e.g. server object
      resource: null,
      // The Time Selector widget that the user uses to select the time range for the graph
      timeSelector: null,
      // The type of message we want to display. Different messages have different eventtypes, e.g. Access logs, FFDC, console.
      eventType: "libertyMessage",
      // The interval for the polling mechanism - default 2 secs.
      interval: 20000,
      // The handle for the polling function.      
      intervalHandle: null,
      // The number of intervals to fetch so at most this many time values of data
      numIntervals : 30,
      // This variable determines how many records are pulled back in each request.
      maxRecords: 0,
      // This variable determines which was the last record we have received.
      startRecord: 0,
      // This variable is used to determine the time intervals for the bar graphs. It is based on the time time selection
      // range.
      timeInterval: 0,
      displayActionButton : false,
      dataKeys : null,
      allDataKeys : null,
      // time range in ms
      startTime : 0,
      to : 0,
      fetchedStartTime : -1,
      fetchedEndTime : -1,
      // chart type class
      chartType : null,
      data : [],
      configNoData : i18n.STATS_LA_RESOURCE_CONFIG_NO_DATA,
      configNoDataPane : null,

      constructor: function(params) {
        this.title = params.title,
        this.parentPane = params.parentPane;
        this.pipeName= params.pipeName;
        this.resource = params.resource;
        this.perspective = params.perspective;
        this.graphType = "LA";
        if (params.chartType) {
          this.chartType = params.chartType;
        } else {
          this.chartType = LineChart;
        }

        if (params.keys) {
          this.dataKeys = params.keys;
        }
        this.configButtonLabel = i18n.STATS_LA_RESOURCE_CONFIG_LABEL;
        this.configButtonLabelNum = i18n.STATS_LA_RESOURCE_CONFIG_LABEL_NUM;
        this.configButtonLabelOne = i18n.STATS_LA_RESOURCE_CONFIG_LABEL_NUM_1;
        this.configAtLeastOne = i18n.STATS_LA_RESOURCE_CONFIG_SELECT_ONE;

        // Add in the timeselector widget so we can query the time range.
        this.timeSelector = registry.byId(this.perspective + ID.dashDelimit(this.resource.id, ID.getTimeSelectorUpper()));
        
        // Default the start/end times in case we don't have a time Selector.
        // If we have a timeSelector then get the range from this widget.
        if (this.timeSelector) {
          var timeSelectorRange = this.timeSelector.getSelectedRange();
          this.startTime = timeSelectorRange[0];
          this.endTime = timeSelectorRange[1];
//Computation of Time interval based on the Time selection
          this.numIntervals=this.getNumberOfInterval();
          this.timeInterval=this.__calculateTimeInterval();
        }
//        console.error("id:" + this.id);
      },
      
      postCreate: function() {
        
        this.inherited(arguments);
          logAnalyticsUtils.getPipeKeys(this.pipeName).then(lang.hitch(this, function(keys) {
            this.parentPane.addChild(this);

            // Set the watch on the selectedRange object in the TimeSelector. This will mean that when the user pressed the button we'll 
            // update the graphs immediately. 
            if (this.timeSelector) {
              this.timeSelector.watch("selectedRange", lang.hitch(this, function(name, oldValue, value) {
                // Remove any previous values in the fetched from and to, so that we go and get the full set of data from the 
                // pipe, now we have a new range.
                this.fetchedStartTime = -1;
                this.fetchedEndTime = -1;
                var timeSelectorRange = this.timeSelector.getSelectedRange();
                this.startTime = timeSelectorRange[0];
                this.endTime = timeSelectorRange[1];
//                Computation of time interval based on time selection
                this.numIntervals=this.getNumberOfInterval();
                // calculate the timeInterval now we have set the this.endTime and this.startTime.
                this.timeInterval=this.__calculateTimeInterval();

                if(this.initTableFlag){// To refresh table 
                  this.initTable();  
                }
                this.processPipe(this.id, this.chartNode, this.startTime, this.endTime, true);
              }));
              
              // This watch is to see when the live Update mode in the TimeSelector is enabled or disabled. If it is disabled, we need 
              // to switch off polling, and if it is enabled need to switch on polling.
              this.timeSelector.watch("liveUpdating", lang.hitch(this, function(name, oldValue, value) {
                if (value) {
                  this.startPolling();
                } else {
                  this.stopPolling();
                }
              }));
            }

            this.allDataKeys = keys;
            if (this.allDataKeys.length <= 1 && !(this instanceof MessagesTableGraph)) {
              // this is a chart with only one bar like "count" so add the actionButton and chartData menu item
              this.hasChartData = true;
              this._createActionButton();
            }
            if (!this.dataKeys) {
              this.dataKeys = this.allDataKeys;
            }
            if (this.laChartNode && this.allDataKeys && this.allDataKeys.length > 1) {
              this._setupConfigList(this.dataKeys, this.allDataKeys);
            }

            // Configure the automatic polling for the graphs. For now we just re-download the graph each time.
            // TODO introduce an update mechanism to add new records to the table - This needs to be done in the pipe, and embedded in the HTML that is
            //      downloaded.
            // Initially generate the graph, and then set up the polling mechanism.
            this.processPipe(this.id, this.chartNode, this.startTime, this.endTime, true);
            this.startPolling();

            // Timing issue could have resulted in startEdit being called before the graph was added
            // to the container. So check here when it is added.
            var graphContainer = registry.byId(this.perspective + this.resource.id + ID.dashDelimit(ID.getStatsUpper(), ID.getContentPaneUpper()));
            if (graphContainer.isEditing) {
              this.startEdit();
            }
          }));
      },

      /**
       * endEdit - override parent to add legendMenuItem back if it was removed
       */
      endEdit: function() {
        if (this.dataKeys && this.dataKeys.length > 1 && this.legendMenuItem && this.legendMenuItemRemoved) {
          this.actionMenu.addChild(this.legendMenuItem, 0);
          this.legendMenuItemRemoved = false;
        }
        this.inherited(arguments);
      },

      _renderChart: function(newWidth) {
        var keys = ["time", this.dataKeys];
        if (!this.chart) {
          this.chart = new this.chartType();
          this.chart.timeSelector(this.timeSelector);
        }

        // legendStyle should be position:"right","width":100,"height":220 for > 1 and position:"bottom","width":880,"height":50 if only 1
        var legendPosition = "right", legendWidth = 100, legendHeight = 220;
        if (this.dataKeys.length < 2) {
          legendPosition = "bottom";
          legendWidth = 880;
          legendHeight = 50;
          this._removeRightSideLegend();
        } else if (!this.legendMenuItem) {
          // need to add the action button
          this.hasLegend = true;
          this.hasChartData = true;
          this._createActionButton();
          // since the actionButton is created after startEdit was first called, we need to call it again
          // to set visibility of the actionButton and configList
          if (this.editing) {
            this.startEdit();
          }
        }
        
        var listOfColors = this.__getColorList();

        this.chart.data(this.data)
        .keys(keys)
        .title(this.title)
        .chartStyle({"width":880,"height":257, "showUTC":false,"timezoneOffsetMillis":0, "dataPoints":true,"lineWidth":2})
        .legendStyle({"position":legendPosition,"width":legendWidth,"height":legendHeight})
        .hAxis({"label":"","position":"out","ticks":7,"tickformat":"","tickDisplayAngle":0, "isDate":"true", "startValue" : this.startTime, "endValue" : this.endTime})
        .vAxis({"label":"","position":"out","ticks":5,"tickformat":"","tickDisplayAngle":0})
        .colors(d3.scale.ordinal().domain(this.allDataKeys).range(listOfColors))
        .tooltip({"trigger":"mouseover"})
        .responsiveWidth(newWidth);
        this.chart.render(this.laChartNode, this.laSrChartNode, this.legendNode, this.singleLegendNode); //'#sr_chart_line_boo'); //'#chart_line_boo','#sr_chart_line_boo');
      },

      stopPolling: function() {
        if (this.intervalHandle !== null) {
          clearInterval(this.intervalHandle);
          this.intervalHandle = null;
        }
      },
      
      startPolling: function() {
        if (this.intervalHandle === null)
          this.intervalHandle = setInterval(lang.hitch(this, this.processPipe), this.interval, this.id, this.chartNode, this.startTime, this.endTime, false);
      },
      
      // note timeSelector is not used ... method _getDeltaTimeRange uses this.timeSelector
      /**
       * This method is where the network call is made to get the records from the pipe. It calls out to processResponse to allow other
       * extended classes to override how the data is parsed.
       * 
       */
      processPipe: function(id, chartNode, startTime, endTime, init) {
        var graphContentPaneID  = id + ID.getChartNode();
        // Get the existing graph Content pane. If it doesn't exist, then create it.
        var chartNodeGraph = registry.byId(graphContentPaneID);
        if (! chartNodeGraph) {
          chartNodeGraph = new ContentPane({id: graphContentPaneID}, chartNode);
        }

        // If we have a previous fetched from value, then we need to use that to only get new records.
        // We always go back 1 min from the last read timestamp, in order for late arriving messages to be processed in the Analytics
        // engine.
        if (this.fetchedStartTime > 0)
          this.fetchedStartTime -= 60000;
        
        // Add the query details to the 
        var urlParams = ""; //"server=" + this.resource.id; // + "&startTime=" + startTime + "&endTime=" + endTime;
        if (this.searchString && this.searchString !== "*") {
          if (urlParams) {
            urlParams += "&";
          } 
          urlParams += "search=" + this.searchString;
        }
        
        if (this.maxRecords > 0) {
//          urlParams += "&startRecord=" + this.startRecord + "&endRecord=" + (this.startRecord + this.maxRecords);
        }
//      Time interval calculation based on time selection.  
        this.timeInterval=this.__calculateTimeInterval();
        if (init) {// First time refresh
          if (urlParams) {
            urlParams += "&";
          } 
          urlParams += "init=true"+"&startTime=" + this.startTime + "&endTime=" + this.endTime+"&timeInterval=" + this.timeInterval;
        }else{// Auto refresh
          this.startTime+=this.interval;
          this.endTime+=this.interval;
          if (urlParams) {
            urlParams += "&";
          } 
          urlParams += "startTime=" + this.startTime + "&endTime=" + this.endTime+"&timeInterval=" + this.timeInterval;
        }
        
        // TODO: this will no longer be a local url
        xhr.get(logAnalyticsUtils.getLogAnalyticsURL() + this.pipeName + "?" + urlParams, logAnalyticsUtils.getLogAnalyticsXHROptions()).then(lang.hitch(this, function(response) {
          // response could be "no data" message
          this.fetchedStartTime = this.startTime;
          this.fetchedEndTime = this.endTime;

          try {
            var parsedResponse = JSON.parse(response);
          
            // Configure the startRecord to point to the next record to read
            if (this.maxRecords > 0) {
              this.startRecord = this.startRecord + parsedResponse.data.length + 1;
            }
          
            // Call out to the function that processes the response.
            this.processResponse(parsedResponse);
          } catch (e) {
            // assumption is it was a message that there is no data
//            if (this.data && this.data.length) {
//              // means only the updated time slice had no new data so render the chart
//              this._renderChart(0);
//            } else {
            if (!response === "No Records found that match search") {
              console.error("Error fetching data for pipe " + this.pipeName + ":" + e);
              console.error("Response:" + response);
            }
            this.data = [];
              if (!this.configNoDataPane) {
                this.configNoDataPane = new ContentPane({
                  content: '<div class="statsUnavailableMessageIconDialog"></div><div>' + this.configNoData + '</div>',
                  baseClass: "configureGraphsSelectOptions"
                });
                this.addChild(this.configNoDataPane);
              }
              this.configNoDataPane.set("style", "display:block");
              this.set("style", "height:360px;");
              if(this.initTableFlag){// To refresh table TODO : as of now hiding the table, actually data should be removed.
                this.initTable();  
              }
              
              // If we have an laChartNode, then do some more processing. 
              if (this.laChartNode) {
                // get rid of the chart
                domConstruct.destroy(this.laChartNode.id + ID.getSvgG());
                // get rid of the bottom legend
                domConstruct.destroy(this.laChartNode.id  + ID.getLegendsvg());
              }
            }
          
        }), lang.hitch(this, function(err) {
          this.processFailure(err); 
        }));
      },
      
      /**
       * This method is called when the response has come back from the pipe. It allows other sub classes to override the processing
       * behaviour of the repsonse.
       */
      processResponse: function(response) {
        // just stick the new data on the array
        this.data = response.data; //this.data.concat(parseResponse[3].data);
        if (this.dataKeys.length !== 0) {
          if (this.configNoDataPane) {
            this.configNoDataPane.set("style", "display:none");
          }
          this._renderChart(0);
        }
      },
      
      /**
       * This method is called when the XHR call to the pipe fails. It allows this to be overridden if required.
       */
      processFailure: function(err) {
        console.log(err);
      },

      /**
       * _getDeltaTimeRange: figures out what the actual time range should be used on the next fetch.
       * @returns {Array} [0] is fromTime, [1] is toTime, both will be -1 when there is nothing new to fetch
       *                  [2] boolean indicates when nothing new to fetch if the chart needs to be rebuilt because of truncation
       */
      _getDeltaTimeRange : function() {
        var rebuild = false;// Moved to front for accommidating the below if condition.
        // If we have a timeSelector then get the range from this widget.
        if (this.timeSelector) {
          var timeSelectorRange = this.timeSelector.getSelectedRange();
          this.startTime = timeSelectorRange[0];
          this.endTime = timeSelectorRange[1];
          rebuild=true;// Auto refresh 
        }
        var newStartTime = -1;
        var newEndTime = -1;
        
        if (this.fetchedStartTime === -1) {
          // haven't fetched anything yet so use the whole from-to range
          newStartTime = this.startTime;
          newEndTime = this.endTime;
        } else if (this.fetchedStartTime !== this.startTime || this.fetchedEndTime !== this.endTime){
          // figure out what to fetch
          // if from is before fetchedStartTime
          if (this.startTime < this.fetchedStartTime) {
            //   if to is after fetchedEndTime
            if (this.endTime > this.fetchedEndTime) {
              //     just get the whole range again
              this.data = [];
              newStartTime = this.startTime;
              newEndTime = this.endTime;
            } else {
              //   else
              //     to could be before fetchedEndTime so need to truncate data
              //     need to prepend the data
              newStartTime = this.startTime;
              newEndTime = this.fetchedStartTime;
              if (this.endTime < this.fetchedEndTime) {
                // need to truncate
                this._truncateData(this.endTime, false);
                this.fetchedEndTime = this.endTime;
                rebuild = true;
              }
            }
          // else
          } else {
            //   from is = or after fetched From
            if (this.startTime !== this.fetchedStartTime) {
              //   if from is after fetchedEndTime
              //     get the whole range
              if (this.startTime > this.fetchedEndTime) {
                this.data = [];
                newStartTime = this.startTime;
                newEndTime = this.endTime;
              } else {
                //   from is after fetchedStartTime so need to truncate data
                this._truncateData(this.startTime, true);
                this.fetchedStartTime = this.startTime;
                rebuild = true;
              }
            }
            if (this.endTime > this.fetchedEndTime) {
              //   append new data
              newStartTime = this.fetchedEndTime;
              newEndTime = this.endTime;
            } else if (this.endTime !== this.fetchedEndTime) {
              // need to truncate the data
              this._truncateData(this.endTime, false);
              this.fetchedEndTime = this.endTime;
              rebuild = true;
            }
          }
        }
        return [newStartTime, newEndTime, rebuild];
      },

      /**
       * truncate data before/after a certain timestamp
       * @param timestamp the timestamp to truncate including the timestamp
       * @param before boolean, true if old (before timestamp) truncation, false if after/newer
       */
      _truncateData : function(timestamp, before)
      {
        if (!this.data || this.data.length === 0) {
          return;
        }
        for (var i = this.data.length-1; i--;) {
          if ((this.data[i].time <= timestamp && before) ||
              (this.data[i].time >= timestamp && !before)) {
            this.data.splice(i,1);
          }
        }
        // check for removing the last item
        if (this.data.length === 1) {
          if ((this.data[0].time <= timestamp && before) ||
              (this.data[0].time >= timestamp && !before)) {
            this.data = [];
          }
        }
      },

      /**
       * update the list of resources based on the configList selections
       */
      updateResourceList : function(resourceList) {
        this.dataKeys = resourceList;
      },


      /**
       * update the list of resources based on the configList selections
       */
      _updateResourceList : function(selectedOptions) {
        // if nothing is selected, display the message
        // need to gray any axis when the message is displayed
        if (selectedOptions.length == 0) {
          this.configAtLeastOnePane.set("style", "display:block");
          // get rid of the chart
          domConstruct.destroy(this.laChartNode.id + ID.getSvgG());
          // get rid of the bottom legend
          domConstruct.destroy(this.laChartNode.id  + ID.getLegendsvg());
        } else {
          this.configAtLeastOnePane.set("style", "display:none");
        }
        this.configSelectedOptions = [];
        this.configButtonCurrentLabel = this.configButtonLabel;
        // if not all selected, deselect all
        var all = selectedOptions.indexOf("STATS_ALL") != -1;
        // if all was previously selected, this must NOT be selecting it
        if (this.configPreviousSelectedOptions && this.configPreviousSelectedOptions.indexOf("STATS_ALL") !== -1
              && selectedOptions.length !== (this.configAllOptions.length+1)) {
          all = false;
          // deselect all option
          this.configList.getOptions("STATS_ALL").selected = false;
        }
        if (all) {
          this.configSelectedOptions = this.configAllOptions;
          this.configButtonCurrentLabel = i18n.STATS_ALL;
          // select all the options
          this.configList.getOptions().forEach(function(option) {
            option.selected = true;
          });
        } else {
          // this.configSelectedOptions = selectedOptions;
          // Still need to go through the list of options to see which are selected to remove the series so the legend updates
          this.configAllOptions.forEach(lang.hitch(this, function(option) {
            if (selectedOptions.indexOf(option) != -1) {
              // add to list
              this.configSelectedOptions[this.configSelectedOptions.length] = option;
            }
          }));
          // set the label based on number selected
          if (selectedOptions.length === 1) {
            this.configButtonCurrentLabel = this.configButtonLabelOne; // TODO: selectedOptions[0];
          } else if (selectedOptions.length > 1){
            this.configButtonCurrentLabel = lang.replace(this.configButtonLabelNum, [selectedOptions.length]);
          }
        }
        // need to set the new list in the graphData section
        registry.byId(this.perspective + ID.getShowGraphsDialogId()).updateGraphData(this.id, this.configSelectedOptions, this.editing);
        this.updateResourceList(this.configSelectedOptions);
        if (this.laChartNode) {
          this._renderChart(0);
        }
      },
      /**
       * This function returns the number of bars should be displayed on the graph.
       */
      getNumberOfInterval: function() {
        var tempSelectedTimeRangeInMillis=this.endTime - this.startTime;
        if(tempSelectedTimeRangeInMillis<86400000){//less then 1 day
          return 30;
        }else if(tempSelectedTimeRangeInMillis<604800017){//1 day to 1 week
          return 24;
        }else if(tempSelectedTimeRangeInMillis<2629800000){//1 week to 1 month
          return 7;
        }else if(tempSelectedTimeRangeInMillis<3155760001 ){//1 month to 1 yr
          return 12;
        }else {// more then 1 yr
          return 30;
        }
      },

      __calculateTimeInterval: function() {
     // Rouding the value to the nearest integer
        return Math.round((this.endTime - this.startTime) / this.numIntervals); 
      },


      /**
       * Create the <table> string representing the data for the graph
       * @returns {String}
       */
      _createDataTable : function() {
        return this.chart.get_sr_table_template(this.chart.config.data,
            this.chart.config.keys,
            this.chart.config.title,
            true,
            this.chart.config.showUTC);
      },

      /**
       * This function returns a list of colors based on the analytics pipeName API
       */
      __getColorList: function() {
        // default colors
        var listOfColors = 
          ["#d9182d","#d9182d","#f19027","#00b0da","#00b0da","#00b0da","#00b0da","#7f1c7d","#4178BE","#a91024"];
        
        if(this.pipeName) {
          if(this.pipeName === "access_count") {
            listOfColors = ["#00b0da", "#008a52", "#7f1c7d", "#f19027", "#d9182d"];
          }
        }
        return listOfColors;
      }
      
    });
});