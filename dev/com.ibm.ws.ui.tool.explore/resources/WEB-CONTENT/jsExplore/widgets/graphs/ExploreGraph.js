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
        "js/common/platform",
        "jsExplore/widgets/MenuItemNoIcon",
        'jsExplore/widgets/graphs/logAnalytics/charts/LineChart',
        "dojo/_base/declare",
        "dojo/_base/lang",
        "dojo/dom-style",
        "dojo/dom-class",
        "dojo/dom-construct",
        "dojo/dom-attr",
        "dojo/on",
        "dijit/layout/ContentPane",
        "dijit/TooltipDialog",
        "dijit/Dialog",
        "dijit/form/DropDownButton",
        "dijit/DropDownMenu",
        "dijit/MenuItem",
        "dijit/registry",
        "dijit/form/Button",
        "dojo/i18n!jsExplore/nls/explorerMessages",
        'jsExplore/widgets/graphs/TimeSelector',
        "jsExplore/resources/stats/_logAnalyticsUtils",
        "d3/d3.min",
        "jsExplore/utils/ID"
        ], function(
                platform,
                MenuItemNoIcon,
                LineChart,
                declare,
                lang,
                domStyle,
                domClass,
                domConstruct,
                domAttr,
                on,
                ContentPane,
                TooltipDialog,
                Dialog,
                DropDownButton,
                DropDownMenu,
                MenuItem,
                registry,
                Button,
                i18n,
                TimeSelector,
                logAnalyticsUtils,
                d3,
                ID
            ){

    var ExploreGraph = declare("ExploreGraph", [ ContentPane ], {
        constructor : function(params) {
          this.server = params[0];
          this.parentPane = params[1];
          this.perspective = params[2];
          this.chartData = [];
          this.lineColors = ['#4178BE', '#df3b4d', '#008a52', '#f19027', '#7f1c7d', '#838329', '#c42684', '#00b0da', '#ffcf04', '#007670'];
          this.initLiveMsgText = i18n.STATS_LIVE_MSG_INIT;
          this.liveMsgText = i18n.STATS_LIVE_MSG;
          this.closeLabel = i18n.CLOSE;
          // Set default based on server. Override this in postMixInProperties if necessary
          if (this.server) {
            this.id = this.perspective + "server" + this.server.id + this.graphKey;
          }
        },

        title : "",
        // The size of the title text
        titleFontSize : platform.isPhone() ? "10px" : "16px",
        // The unique ID for this graph
        id : "",
        graphType : "",
        server : null,
        parentPane : null,
        interval : 2000,
        numberOfSnapshots : 50,
        chartWidth: 411,
        chartHeight: 246,
        chartData : null,
        allDataKeys: ["y"],
        dataKeys: ["y"],
        chart : null,
        legend : null,
        singleLegend : null,
        yDomain : null,   // used for y-axis to specify a hard-coded start/end for the axis to render
        upperLimit : 0,
        yAxisTitle : "",
        yAxisTickSuffix : "",
        displayActionButton : true,
        displayLegend : "false",
        hasLegend : false,
        hasChartData : true,
        actionButton : null,
        actionMenu : null,
        actionBarDialog: null,
        deleteButton : null,
        legendMenuItem : null,
        legendMenuItemRemoved : false,
        // The handle of the polling interval, so we can shut it down when the graph is not displayed.
        intervalHandle: null,
        editing: false,
        // for the dropdown config list options
        configButtonLabel : null,
        configButtonLabelNum : null,  // has numeric insert
        configButtonLabelOne : null,  // what to use when only 1 is selected
        configButtonCurrentLabel : null,  // actual current value
        configAllOptions : null,
        configSelectedOptions : null,
        configPreviousSelectedOptions : null,
        configAtLeastOne : null,
        configAtLeastOnePane : null,
        // The Time Selector widget that the user uses to select the time range for the graph
        timeSelector: null,
        // The resource that the graph is for e.g. server object
        resource: null,

        postCreate : function() {
          this.inherited(arguments);
          if (!this.resource) {
            // default to the server
            this.resource = this.server;
          }
          if (this.graphType !== "LA") {
            this.parentPane.addChild(this);
          }

          // Create the action button and menu
          if (this.displayActionButton) {
            this._createActionButton();
          }

          // Create the delete button for edit mode
          this._createDeleteButton();

          // Create the chart within it's "holding" node
          this.timeSelector = registry.byId(this.perspective + ID.dashDelimit(this.resource.id, ID.getTimeSelectorUpper()));
          if (this.graphType !== "LA") {
            this._createChart();
            // if there is a timeSelector and it is displayed, only then display the overlay
            // TODO: display is none in the single perspective for some reason (probably timing thing)
            if (this.timeSelector && logAnalyticsUtils.isLogAnalyticsEnabled(this.resource)) { // && domStyle.get(this.timeSelector.domNode, "display") !== "none") {
              this._liveStatusOverlay();
            } else {
              // hide the overlay
              var varLiveMsgNode=document.getElementById(this.id+'_liveMsgNode');
              if (varLiveMsgNode) {
                varLiveMsgNode.style.display='none';
              }
            }
          }

        },

        /**
         * Shows the status message about live update on non-analytics graphs.
         */
        _liveStatusOverlay:function(){
          var resourceId=this.id;
          if (this.timeSelector && this.timeSelector !== null) {
            this.timeSelector.watch("selectedRange", lang.hitch(this, function(name, oldValue, value) {
              var varLiveMsgNodeId=resourceId+'_liveMsgNode';
              var varLiveMsgDetailNodeId=resourceId+'_liveMsgDetailNode';
              var varLiveMsgNode=document.getElementById(varLiveMsgNodeId);
              var varLiveMsgDetailNode=document.getElementById(varLiveMsgDetailNodeId);
              if(varLiveMsgNode && varLiveMsgDetailNode){
                varLiveMsgDetailNode.style.display='none';
                varLiveMsgNode.style.display='block';
              }
            }));
          }
        },
        /**
         * startEdit - switch the action button to delete
         */
        startEdit : function() {
          this.editing = true;
          if (this.actionButton){
            this.actionButton.set("style", "display:none");
          }
          // Set the deleteButton visible
          this.deleteButton.set("style", "display:block");
          // check for the configList
          if (this.configList && this.configList.options && this.configList.options.length > 0) {
            this.configList.set("style", "display:inline-block");
          }
        },

        _xDomain: function (chartData) {
          if (chartData[0]) {
            if (chartData[0].x) {
              return [chartData[0].x, this._calculateToXAxisValue(chartData)];
            }
            if (chartData[0].time) {
              return [chartData[0].time, this._calculateToXAxisValue(chartData)];
            }
          }
          return [null, null];
        },

        /**
         * endEdit - switch the action button to action
         */
        endEdit : function() {
          this.editing = false;
          if (this.actionButton && this.actionMenu.getChildren().length !== 0){
            this.actionButton.set("style", "display:block");
          }
          // just take off the deleteButton
          this.deleteButton.set("style", "display:none");
          this.setNew(false);
          // check for the configList
          if (this.configList) {
            this.configList.set("style", "display:none");
          }
        },

        /**
         * Set whether this is a newly added graph or not
         */
        setNew : function(newGraph) {
          if (newGraph) {
            domClass.toggle(this.domNode, "new", true);
          } else {
            domClass.toggle(this.domNode, "new", false);
          }
        },

        _createChart : function() {
            this.startPolling();
            this._createD3Chart();
        },

        _createD3Chart : function() {

          if (!this.chart) {
            this.chart = new LineChart();
            }

        },

        addDataPointAndRender : function(data) {
          // Check that the data we're about to display is a valid value.
          if (! (isNaN(data))) {
            this.chartData[this.chartData.length] = { "time" : new Date().getTime(), y : data};
            // The charts seem to need more than one data point to render properly, so only create the chart when we have 2 or more
            // datapoints.
            if (this.chartData.length > 1) {
              this._renderChart();
            }
          }
        },

        d3addMultiDataPointAndRender : function(data, chartData) {
          chartData[chartData.length] = data;
          if ( this.chartData.length >= this.numberOfSnapshots) {
            this.chartData.shift();
            }
          this._renderChart();
        },

        _prepChart : function() {
          // legendStyle should be position:"right","width":100,"height":220 for > 1 and position:"bottom","width":880,"height":50 if only 1
          var legendPosition = "right", legendWidth = 100, legendHeight = 220;
          if (this.dataKeys.length < 2) {
            legendPosition = "bottom";
            legendWidth = 440;
            legendHeight = 50;
            this._removeRightSideLegend();
          } else {
            this._addLegendMenuItem();
          }
          var keys = ["time", this.dataKeys];
          var xDomain = this._xDomain(this.chartData);
          var tickSuffix = this.yAxisTickSuffix;

          this.chart.data(this.chartData)
          .keys(keys)
          .title(this.title)
          .chartStyle({"width":415,"height":258, "showUTC":false,"timezoneOffsetMillis":0, "dataPoints":true,"lineWidth":1})
          .legendStyle({"position":legendPosition,"width":legendWidth,"height":legendHeight, "displayLegend":this.displayLegend})
          .hAxis({"label":"","position":"out","ticks":4,"tickformat":"","tickDisplayAngle":0, "isDate":"true", "startValue" : xDomain[0], "endValue" : xDomain[1], "hasSelect":"false"})
          .vAxis({"label":"","position":"out","ticks":5,"tickformat":function (d){var format = d3.format("d"); return (format(d) + tickSuffix);},"tickDisplayAngle":0,"domain": this.yDomain})
          .colors(d3.scale.ordinal().domain(this.allDataKeys).range(this.lineColors))
          .tooltip({"trigger":"mouseover"})
          .responsiveWidth(0);
        },

        _renderChart : function() {
          if (this.chartData.length > 1) {
            this._prepChart();
            this.chart.render(this.laChartNode, this.laSrChartNode, this.legendNode, this.singleLegendNode); //'#sr_chart_line_boo'); //'#chart_line_boo','#sr_chart_line_boo');
          }
        },

        /* This method works out what the Graph's XAxis To value is. If we have the full set of snapshot values
         * the returned value is the x value of the last element. If we don't have the full number of snapshots, we
         * take the last element's xvalue, and add the remaining number snapshot elements calculated at the interval
         * value.
         * e.g if we have number of snapshots of 50, and an interval type of 2000 ms and we have 25 snaphots, with the last
         * entry having a x value of 111111, then the xAxis value returned would be 111111 + ((50 -25) * 2000)
         */
        _calculateToXAxisValue: function(chartData) {
          var xAxisValue = 0;
          var chartDataLength = chartData.length;
          // If we have the maximum number of data elements for the chartdata, then the xaxis value
          // is the x value of the last element.
          if (chartDataLength >= this.numberOfSnapshots) {
            if (chartData[chartDataLength -1].x) {
            xAxisValue = chartData[chartDataLength -1].x;
            } else if (chartData[chartDataLength -1].time) {
              xAxisValue = chartData[chartDataLength -1].time;
            }
          } else if(chartDataLength === 0) {
            xAxisValue += this.numberOfSnapshots * (this.interval);
          } else {
            // if we don't have a full array, we need to take the x value of the last entry
            // and add the default interval values for the rest of the snapshot places
            if (chartData[chartDataLength -1].x) {
            xAxisValue = chartData[chartDataLength -1].x;
            } else if (chartData[chartDataLength -1].time) {
              xAxisValue = chartData[chartDataLength -1].time;
            }
            xAxisValue += (this.numberOfSnapshots - chartDataLength) * this.interval;
          }

          return xAxisValue;
        },


        _createActionButton : function() {

          this.actionMenu = new DropDownMenu({
            'aria-labelledby': this.id + ID.getActionButtonTooltipDialogUpper(),
            baseClass: "statusGraphActionMenu"
          });

          if (this.hasLegend) {
            var itemLabel = i18n.STATS_HIDE_LEGEND;
            var itemIconClass = "statusGraphLegendHideIcon";
            // if legend is not displayed, then menu option is to display it
            if (domStyle.get(this.legendNode, "display") === "none") {
              itemLabel = i18n.STATS_DISPLAY_LEGEND;
              itemIconClass = "statusGraphLegendShowIcon";
            }
            // TODO: Haven't figured out why this is left over after destroy in ShowGraphsDialog.__destroyMultiGraphs()
            // so if there is one, delete it and see if that works for now
            // Scenario: edit, add/remove a servlet, cancel
            if (registry.byId(this.id + "DisplayHideLegend")) {
              registry.byId(this.id + "DisplayHideLegend").destroy();
            }
            this.legendMenuItem = new MenuItem({
                label: itemLabel,
                id: this.id + ID.getDisplayHideLegendUpper(),
                iconClass: itemIconClass,
                baseClass: "statusGraphActionMenuItems",
                onClick: lang.hitch(this, function(){
                  // switch the display of the legend and the menuItem label
                  if (domStyle.get(this.legendNode, "display") === "none") {
                    domStyle.set(this.legendNode, "display", "inline-block");
                    this.legendMenuItem.set("label", i18n.STATS_HIDE_LEGEND);
                    this.legendMenuItem.set("iconClass", "statusGraphLegendHideIcon");
                  } else {
                    domStyle.set(this.legendNode, "display", "none");
                    this.legendMenuItem.set("label", i18n.STATS_DISPLAY_LEGEND);
                    this.legendMenuItem.set("iconClass", "statusGraphLegendShowIcon");
                  }
                  this.actionButton.closeDropDown();
                })
            });
            this.actionMenu.addChild(this.legendMenuItem);
          }

          if (this.hasChartData) {
            this.showDataMenuItem = new MenuItemNoIcon({
              label: i18n.STATS_VIEW_DATA,
              baseClass: "statusGraphActionMenuItems",
              onClick: lang.hitch(this, function(){
                this.actionButton.closeDropDown();
                var dataDialog = new Dialog({
                  title: this.title,
                  onCancel: function(){this.hide();},
                  onHide: function(){this.destroyRecursive();},
                  content: ""
                }, "dataDialogId");
                domAttr.set(dataDialog.closeButtonNode, "tabindex", "0");
                dataDialog.set("class", "graphDataDialog");
                domClass.add(dataDialog.titleBar, "graphDataDialog");
                domClass.add(dataDialog.containerNode, "graphDataDialog");
                dataDialog.set("content", this._createDataTable());
                dataDialog.startup();
                dataDialog.show();
              })
            });
            this.actionMenu.addChild(this.showDataMenuItem);
          }

          this.actionMenu.startup();

          // Build the container for the dropDownMenu.  Put menu in a TooltipDialog
          // so that we can have the little connector to the DropDownButton (V) no matter how
          // the menu is positioned (above, left, right, or below) relative to the DropDownButton.
          // build the drop down menu options
          this.actionBarDialog = new TooltipDialog({
            id : this.id + ID.getActionButtonTooltipDialogUpper(),
            "aria-labelledby" : this.id + ID.getActionButtonTooltipDialogUpper(),
            "aria-label" : lang.replace(i18n.STATS_ACTION_MENU, [this.id]),
            baseClass : 'actionMenu',
            style : 'display: inline;',
            content : this.actionMenu
          });

          // TODO: Should add a string instead of constructing the alt string
          this.actionButton = new DropDownButton({
            id : this.id + ID.getActionButtonUpper(),
            label : '<img src="imagesShared/card-action-T.png" alt="' + this.title + " " +i18n.ACTIONS +'" title="' +i18n.ACTIONS +'">',
            dropDown : this.actionBarDialog,
            "aria-haspopup" : true,
            baseClass : 'statusGraphActionButton'
          });
          this.actionButtonNode.addChild(this.actionButton);

          on(this.actionButton, "mouseover", lang.hitch(this, function() {
            this._showHoverIcon(this.actionButton.id, true);
          }));
          on(this.actionButton, "mouseout", lang.hitch(this, function() {
            this._showHoverIcon(this.actionButton.id, false);
          }));

          on(this.actionBarDialog, "open", lang.hitch(this, function(evt) {
            this._showHoverIcon(this.actionButton.id, true);
          }));
          on(this.actionBarDialog, "close", lang.hitch(this, function(evt) {
            //Change action icon back to original
            this.actionBarDialog.closing = true;
            this._showHoverIcon(this.actionButton.id, false);
            this.actionBarDialog.closing = false;
          }));

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
              this.chart.config.showUTC,
              this.yAxisTitle);
        },

        /**
         * To show or hide the hover/selected icon for the graph's action drop down.
         * @param buttonId  String
         * @param show      Boolean.  True to show the hover/selected icon; False
         *                            to show the original action icon.
         */
        _showHoverIcon: function(buttonId, show) {
          var actionButton = document.getElementById(buttonId);

          if(actionButton !=null){
            var img = actionButton.getElementsByTagName('img')[0];
            if (show) {
              img.src = 'imagesShared/card-action-hover-T.png';
            } else {
              var dropdown = registry.byId(buttonId + ID.getTooltipDialogUpper());
              if (!(dropdown && dropdown.focused) || dropdown.closing) {
                img.src = 'imagesShared/card-action-T.png';
              }
            }
          }
        },

        /**
         * _removeRightSideLegend removes the legend from the expanded right side of the graph
         * Collapse right side legend if open
         * Remove legend menu item
         * Remove action button if no remaining actions
         */
        _removeRightSideLegend : function() {
          // If the legend is open, close/hide it
          if (domStyle.get(this.legendNode, "display") !== "none") {
            domStyle.set(this.legendNode, "display", "none");
            this.legendMenuItem.set("label", i18n.STATS_DISPLAY_LEGEND);
            this.legendMenuItem.set("iconClass", "statusGraphLegendShowIcon");
          }
          // If the legend menu item has not been removed, remove it
          if (this.legendMenuItem && !this.legendMenuItemRemoved) {
            this.actionMenu.removeChild(this.legendMenuItem);
            this.legendMenuItemRemoved = true;
          }
          // If there are no children left, hide the action button
          if (this.actionMenu && this.actionMenu.getChildren().length == 0) {
            this.actionButton.set("style", "display:none");
          }
        },

        _addLegendMenuItem : function() {
          if (this.legendMenuItemRemoved) {
            this.actionMenu.addChild(this.legendMenuItem, 0);
            this.legendMenuItem.set("label", i18n.STATS_DISPLAY_LEGEND);
            this.legendMenuItem.set("iconClass", "statusGraphLegendShowIcon");
            this.legendMenuItemRemoved = false;
          }
        },

        _createDeleteButton : function() {
          var deleteButtonId = ID.underscoreDelimit(this.id, ID.getDeleteGraphButton());
          // TODO: Add a message for the alt instead of constructing it. Past the translation cut-off.
          this.deleteButton = new Button({
            id : deleteButtonId,
            value: deleteButtonId,
            label : '<img src="images/remove-DT.png" alt="' +i18n.STATS_DELETE_GRAPH +'" title="' +i18n.STATS_DELETE_GRAPH +'">',
            "aria-label" : i18n.STATS_DELETE_GRAPH + " " + this.title,
            baseClass : "statusGraphDeleteButton",
            "style" : "display:none",
            onClick : lang.hitch(this, function(){
              registry.byId(this.perspective + ID.getShowGraphsDialogId()).toggleGraph(this.id, true);
            })
          });
          this.actionButtonNode.addChild(this.deleteButton);
        },

        startPolling: function() {
          if (this.intervalHandle === null) {
            // start the interval to gather and render the data
            this.generateHistoricalData();
          }
        },

        stopPolling: function() {
          if (this.intervalHandle !== null) {
            clearInterval(this.intervalHandle);
            this.intervalHandle = null;
          }
        },

        /**
         * Create the configList
         */
        _setupConfigList : function(selections, allSelections) {
          this.configAllOptions = allSelections;
          this.configSelectedOptions = selections;
          domClass.add(this.configList.dropDownMenu.domNode, "claro");
          // set up the drop down list of options to select
          // first add "all"
          var allSelected = false;
          if (this.configSelectedOptions.length === this.configAllOptions.length) {
            allSelected = true;
          }
          this.configList.addOption({value: "STATS_ALL", label: i18n.STATS_ALL, selected: allSelected});
          var optionSelected = false;
          for (var i = 0; i < this.configAllOptions.length; i++) {
            if (!allSelected) {
              if (this.configSelectedOptions.indexOf(this.configAllOptions[i]) > -1) {
                optionSelected = true;
              } else {
                optionSelected = false;
              }
            } else {
              // select it since all are selected
              optionSelected = true;
            }
            this.configList.addOption({value: this.configAllOptions[i], label: this.configAllOptions[i], selected: optionSelected, onClick : function(){console.error("click!");}});
          }
          this.configList.onChange = lang.hitch(this, function(selectedOptions) {
            // if this action is unchecking all, deselect all the others
            var all = selectedOptions.indexOf("STATS_ALL") !== -1;
            // if all was previously selected, this must be de-selecting it
            if (this.configPreviousSelectedOptions && this.configPreviousSelectedOptions.indexOf("STATS_ALL") !== -1
                  && !all && selectedOptions.length === this.configAllOptions.length) {
              // deselect all options
              this.configList.getOptions().forEach(function(option) {
                option.selected = false;
              });
            }
            this._updateResourceList(selectedOptions);
            // call updateSelection to set the checkboxes since they may have been changed
            this.configList._updateSelection();
            // now set the button label
            this.configList.dropDownButton.set("label", this.configButtonCurrentLabel);
            // and save the list for the next time
            this.configPreviousSelectedOptions = selectedOptions;
          });
          this.configList.dropDownButton.set("label", this.configButtonLabel);

          // enable clicking anywhere on the button to display/hide the dropdown rather than
          // restricting it on the arrow only
          var me = this;
          this.configList.on('click', function(evt){
            me.configList.dropDownButton.toggleDropDown();
          });

          // display "must select something" message initially
          // construct and display
          this.configAtLeastOnePane = new ContentPane({
            content: '<div class="statsUnavailableMessageIconDialog"></div><div>' + this.configAtLeastOne + '</div>',
            baseClass: "configureGraphsSelectOptions"
          });
          this.addChild(this.configAtLeastOnePane);
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
            domConstruct.destroy(this.laChartNode.id + "svg");
            // get rid of the bottom legend
            domConstruct.destroy(this.laChartNode.id  + "legendsvg");
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
          this.clearChartData();
          this._renderChart();
        }
    });

    return ExploreGraph;

});
