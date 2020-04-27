/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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
        'dojo/dom',
        'dojo/date/locale',
        'dojo/number',
        'dojo/dom-construct',
        'dojo/_base/declare',
        'd3/d3.min',
        "dojo/i18n!jsExplore/nls/explorerMessages",
        "jsExplore/utils/ID"
        ], function(
            lang,
            dom,
            dateLocale,
            numberLocale,
            domConstruct,
            declare,
            d3,
            i18n,
            ID
            ){

    var Chart = declare("Chart", [], {
        constructor : function(params) {
          this.config = {
              data  : [],
              keys  : null,
              legendStyle : {},
              chartStyle  : {},
              colors  : d3.scale.category20(),
              hAxis   : null,
              vAxis   : null,
              tooltip : {},
              tooltip_showAllLegends : false,
              dataInterval : null,
              responsiveWidth : 0,
              clickAction : null
            };
        },

        config : null,
        container : null,
        sronlyContainer : null,
        legendNode : null,
        singleLegendNode : null,
        zoomingIn : false,
        selectedStartTime : null,
        selectedStartXValue : null,

        postCreate : function() {
          this.inherited(arguments);
        },

        render : function(container, sronlyContainer, legendNode, singleLegendNode) {
          this.container = container;
          this.sronlyContainer = sronlyContainer;
          this.legendNode = legendNode;
          this.singleLegendNode = singleLegendNode;
          this.drawChart(container);
          // add sr-only table for accessibility
          if (sronlyContainer != null && this.config.data) {
            d3.select(sronlyContainer).html(
                this.get_sr_table_template(this.config.data,
                    this.config.keys,
                    this.config.title,
                    true,
                    this.config.showUTC));
          }
        },

        data : function(value) {
          if (!arguments.length) {
            return this.config.data;
          }
          this.config.data = value;
          return this;
        },

        keys : function(value) {
          if (!arguments.length) {
            return this.config.keys;
          }
          this.config.keys = value;
          return this;
        },

        legendStyle : function(value) {
          if (!arguments.length) {
            return this.config.legendStyle;
          }
          this.config.legendStyle = value;
          return this;
        },

        chartStyle : function(value) {
          if (!arguments.length) {
            return this.config.chartStyle;
          }
          this.config.chartStyle = value;
          return this;
        },

        hAxis : function(value) {
          if (!arguments.length) {
            return this.config.hAxis;
          }
          this.config.hAxis = value;
          return this;
        },

        vAxis : function(value) {
          if (!arguments.length) {
            return this.config.vAxis;
          }
          this.config.vAxis = value;
          return this;
        },

        colors : function(value) {
          if (!arguments.length) {
            return this.config.colors;
          }
          this.config.colors = value;
          return this;
        },

        tooltip : function(value) {
          if (!arguments.length) {
            return this.config.tooltip;
          }
          this.config.tooltip = value;
          return this;
        },

        title : function(value) {
          if (!arguments.length) {
            return this.config.title;
          }
          this.config.title = value;
          return this;
        },

        clickAction : function(value) {
          if (!arguments.length) {
            return this.config.clickAction;
          }
          this.config.clickAction = value;
          return this;
        },

        responsiveWidth : function(value) {
          if (!arguments.length) {
            return this.config.responsiveWidth;
          }
          this.config.responsiveWidth = value;
          return this;
        },

        timeSelector : function(value) {
          if (!arguments.length) {
            return this.config.timeSelector;
          }
          this.config.timeSelector = value;
          return this;
        },

        /**
         * _renderAxes : Use the config information to render the x and y axes
         * @param panel panel to add the axis
         */
        _renderAxes : function(panel) { //actualChartWidth, actualChartHeight) {
          var margin = {
              left: 5,
              right: 5,
              top: 10,
              bottom: 0
          };

          var xAxisPadding = 0;
          if (this.config.hAxis !== null) {
            if (this.config.hAxis['position'] == 'out') {
              xAxisPadding += 25;
            } else if (this.config.hAxis['position'] == 'in') {
              xAxisPadding += 10;
            }
            if(this.config.hAxis['label'] !== '') {
              xAxisPadding += 20;
            }
            margin.left += 5;
            margin.right += 10;
          }

          var yAxisPadding = 0;
          if (this.config.vAxis !== null) {
            if (this.config.vAxis['position'] == 'out') {
              yAxisPadding += 40;
            } else if (this.config.vAxis['position'] == 'in') {
              yAxisPadding += 5;
            }
            if(this.config.vAxis['label'] !== '') {
              yAxisPadding += 20;
            }
          }

          var actualChartWidth = this.config.width - yAxisPadding - margin.left - margin.right;
          var actualChartHeight = this.config.height;
//          if (this.config.legendStyle.position === "right") {
            actualChartHeight = this.config.height - xAxisPadding - margin.top - margin.bottom;
//          }

          if (this.config.hAxis != null) {
            this.config.hAxis['labelOffset'] = xAxisPadding-5;
            if(this.config.hAxis['ticks']=='default') {
              this.config.hAxis['ticks'] = ((actualChartWidth / 100) * 1);
            }

            var tickformat = this.config.hAxis['tickformat'];
            if(tickformat==null || tickformat=='default' || tickformat=='') {
              this.config.hAxis['tickformat'] = this.globalizeTimeScaleFormat(this.config.showUTC);
            }

            // this.config has hAxis and vAxis passed in
            this.config.xAxis = {};
            this.config.xAxis.scale = this.config.xScale;
            this.config.xAxis.axisStyle = this.config.hAxis;
            this.config.xAxis.chartWidth = actualChartWidth;
            this.config.xAxis.chartHeight = actualChartHeight;
            this.config.xAxis.makeTicksResponsive = false;
            this.config.xAxis.showUTC = this.config.showUTC;
            this._createAxis(panel, "x", this.config.xAxis);
          }

          if (this.config.vAxis != null) {
            this.config.vAxis['labelOffset'] = yAxisPadding-10;
            if(this.config.vAxis['ticks']=='default') {
              this.config.vAxis['ticks'] = ((actualChartHeight / 100) * 3) + 1;
            }
            var tickformat = this.config.vAxis['tickformat'];
            if(tickformat==null || tickformat=='default' || tickformat=='') {
              this.config.vAxis['tickformat'] = d3.format("d");
            }
            this.config.yAxis = {};
            this.config.yAxis.scale = this.config.yScale;
            this.config.yAxis.axisStyle = this.config.vAxis;
            this.config.yAxis.chartWidth = actualChartWidth;
            this.config.yAxis.chartHeight = actualChartHeight;
            this.config.yAxis.makeTicksResponsive = false;
            this.config.yAxis.showUTC = this.config.showUTC;
            this._createAxis(panel, "y", this.config.yAxis);
          }
        },

        /**
         * _createAxis : create an axis
         * @param mainpanel panel to add the axis
         * @param axis "x" or "y"
         * @param axisConfig all the config info
         *        axisConfig.axisStyle : "label", "position", ticks", "tickformat", "tickDisplayAngle", "isDate"
         */
        _createAxis : function(mainpanel, axis, axisConfig) {

          axisConfig.className = axis;
          axisConfig.addlClassName = "";
          if (axis === 'y') {
            // 'in'=='right' /  'out=='left"
            axisConfig.addlClassName = "yaxis";
            axisConfig.axisPosition = (axisConfig.axisStyle['position'] == 'in') ? "right" : "left";
            axisConfig.hasGridLines = true;
            axisConfig.labelOffset = 5;
            axisConfig.d3axis_x = 0; //axisConfig.chartWidth;
            axisConfig.d3axis_y = 0;
            axisConfig.calcuReqHeightNLabelPosition = function() {
              if (axisConfig.axisStyle.label!= null && axisConfig.axisStyle.label !== '') {
                axisConfig.label_x = -22; //-(axisConfig.chartHeight/2);
                axisConfig.label_y = -axisConfig.labelOffset;
//                axisConfig.label_transform = "rotate(-90)";
              }
              axisConfig.requiredHeight = axisConfig.chartHeight;
            };
            axisConfig.addGridNewLine = function(d,i) {
              var g = d3.select(this);
              var line = g.append("line");
              line.attr("x2",0)
                .attr("y2",0)
                .attr("x1",axisConfig.chartWidth);
            };
          } else {
            // default to x axis
            // 'in'=='top' /  'out=='bottom"
            axisConfig.axisPosition = (axisConfig.axisStyle['position'] == 'in') ? "top" : "bottom";
            axisConfig.hasGridLines = false;
            axisConfig.labelOffset = 40;
            axisConfig.d3axis_x = 0;
            axisConfig.d3axis_y = axisConfig.chartHeight;
            axisConfig.calcuReqHeightNLabelPosition = function() {
              if (axisConfig.axisStyle.label!= null && axisConfig.axisStyle.label !== '') {
                axisConfig.label_x = axisConfig.chartWidth / 2;
                axisConfig.label_y = axisConfig.chartHeight + axisConfig.labelOffset;
                axisConfig.label_transform = "";
              }
              axisConfig.requiredHeight = axisConfig.labelOffset;

              if(axisConfig.axisStyle['position'] == 'in') {
                axisConfig.requiredHeight = 0;
              }
            };
            axisConfig.addGridNewLine = function(d,i) {
              var g = d3.select(this);
              var line = g.append("line");
              line.attr("y2",0)
                .attr("x2",0)
                .attr("y1",-axisConfig.chartHeight);
            };
            if (!axisConfig.axisStyle.hasSelect || axisConfig.axisStyle.hasSelect === "true") {
              this._addAxisSelect(mainpanel);
            }
          }

          // D3 AXIS
          var d3Axis = d3.svg.axis()
            .scale(axisConfig.scale) //attrs.scale)
            .orient(axisConfig.axisPosition);
          d3Axis.ticks(axisConfig.axisStyle.ticks);
          if(axisConfig.axisStyle['tickformat']!=null) { d3Axis.tickFormat(axisConfig.axisStyle['tickformat']); }

          // ADD AXIS TO SVG
          var axisSvg = mainpanel.append("g")
            .attr("class",axisConfig.className+" axis " + axisConfig.addlClassName)
            .attr("transform", "translate("+ axisConfig.d3axis_x + ","+ axisConfig.d3axis_y + ")")
            .call(d3Axis);
          if(axisConfig.axisStyle.tickDisplayAngle!=null) {
            axisSvg.selectAll('text').attr('transform', 'rotate('+ axisConfig.axisStyle.tickDisplayAngle  + ')');
          }
          // add grid lines
          if(axisConfig.hasGridLines) {
            axisSvg.selectAll('.tick.major').each(axisConfig.addGridNewLine);
          }

          // calculate height & Label Position
          axisConfig.calcuReqHeightNLabelPosition();

          // ADD AXIS LABEL - specify a label to better describe what the axis represents
          if (axisConfig.axisStyle.label!= null && axisConfig.axisStyle.label !== '') {
            var labelText = mainpanel.append('g')
              .attr("class",axisConfig.className+" label "+axisConfig.addlClassName)
              .append('text')
              .attr('transform', axisConfig.label_transform)
              .attr("text-anchor","middle")
              .attr('x', axisConfig.label_x)
              .attr('y', axisConfig.label_y)
              .text(axisConfig.axisStyle.label);
            if(axisConfig.labelFontStyle != null) {
              labelText.style('font', axisConfig.labelFontStyle);
            }
          }
        },

        _addAxisSelect : function(mainpanel) {
          var chart = this;
          var focus = mainpanel.append("g")
            .style("display", "none");
          // add the line
          focus.append("line")
            .attr("class", "x")
            .style("stroke", "blue")
            .attr("y1", 0)
            .attr("y2", 350);
          mainpanel.append("rect")
            .attr("class", "rect")
            .attr("width", chart.config.width)
            .attr("height", chart.config.height)
            .style("fill", "none")
            .style("pointer-events", "all")
            .on("mouseover", function() { focus.style("display", null); })
            .on("mouseout", function() { focus.style("display", "none"); })
            .on("mousedown", mousedown)
            .on("mouseup", mouseup)
            .on("mousemove", mousemove);

          function mousedown() {
            // save the start value
            // the date where the mouse is
            var newX = d3.mouse(this)[0];
            chart.selectedStartTime = chart.config.xScale.invert(newX);
            chart.selectedStartXValue = newX;
            chart.zooming = true;
            // put a line where the cursor is
            focus.append("line")
              .attr("class", "xStart")
              .style("stroke", "blue")
              .attr("y2", 0)
              .attr("x2", newX)
              .attr("x1", newX)
              .attr("y1", chart.config.height);

            focus.append("rect")
              .attr({
                "class" : "selection",
                x       : newX,
                y       : 0,
                width   : 0,
                height  : chart.config.height
              })
              .style("fill", "blue")
              .style("opacity", 0.3);
          }

          function mouseup()  {
            // mouse up so set the range on the time selector
            chart.zooming = false;
            // remove the startLine and rectangle
            focus.select(".xStart").remove();
            focus.select("rect.selection").remove();
            var newX = d3.mouse(this)[0];
            // if they just clicked and didn't drag, return
            if (chart.selectedStartXValue === newX) {
              chart.selectedStartXValue = null;
              return;
            }
            // the date where the mouse is
            chart.selectedEndTime = chart.config.xScale.invert(newX);
            if (chart.config.timeSelector) {
              var start = new Date(chart.selectedStartTime);
              var end = new Date(chart.selectedEndTime);
              console.log("zoom in chart dateRange:" + [start,end]);
              if (start > end) {
                // start is later than end, swap them
                var temp = start;
                start = end;
                end = temp;
              }
              chart.config.timeSelector.setStartAndEndDate(start, end);
            }
            chart.selectedStartXValue = null;
          }

          function mousemove() {
            focus.select(".x")
                .attr("x2", d3.mouse(this)[0])
                .attr("x1", d3.mouse(this)[0]);
            // if there is a first line, then set the area
            if (focus.select(".xStart")[0][0]) {
              var s = focus.select( "rect.selection");
              if (!s.empty()) {
                var newX = d3.mouse(this)[0];
                var d = {
                        x       : chart.selectedStartXValue,
                        y       : 0,
                        width   : 0,
                        height  : chart.config.height
                };
                if (newX < d.x) {
                  d.width = d.x - newX;
                  d.x = newX;
                } else {
                  d.width = newX - d.x;
                }
                s.attr( d);
              }
            }
          }
        },

        /**
         * CHART LEGENDS
         *
         * @attr data
         * @attr colors
         * @attr layoutStyle
         * @attr x
         * @attr y
         * @attr width
         * @attr height
         * @attr dotSize
         * @attr dotType
         * @attr fontStyle
         * @attr padding
         * @attr chartWidthOffset
         * @attr chartHeightOffset
         *
         */
        _chart_legends : function() {
          var config = {
            "data" : [],
            "actualNames" : null,
            "colors" : d3.scale.category20(),
            "x" : 0,
            "y" : 0,
            "width" : 50,
            "height" : 100,
            "fontStyle" : "10px Verdana",
            "dotSize" : 5,
            "dotType" : "circle",  /* circle,square */
            "padding" : 10,
            "chartWidthOffset" : 8,
            "chartHeightOffset" : 8,
            "layoutStyle" : "left2right@width" /* left2right@width,left2righ@height,top2bottom@height,top2bottom@width */
          };
          var config_keys = Object.keys(config);

          var thisPane = function(parentPanel) {
            var mainPanel = (parentPanel instanceof String)
              ? d3.select(parentPanel)
              : parentPanel;
              _drawLegends(mainPanel);
          };
          thisPane.attr = lang.hitch(this, function(name,value) {
            switch (arguments.length) {
            case 0:
              return null;
            case 1:
              return config[name];
            default:
              if(config_keys.indexOf(name)==-1) { console.log("[u$.chart_legends]","WARNING","Invalid attribute :",name); }
              config[name]=this.defaultValue(value, config[name]);
              break;
            }
            return thisPane;
          });
          thisPane.getLegends = function() {
            return config.legends;
          };

          /**
           * _drawLegends - render legends
           */
          _drawLegends = lang.hitch(this, function(mainpanel) {
            // if position is "right", get the legendNode and create an svg as the parent/mainpanel
            if (dom.byId(this.container.id  + ID.getLegendsvg())) {
              domConstruct.destroy(this.container.id  + ID.getLegendsvg());
            }
            var legendContainer = null;
            var width = 300;
            var height = 35;
            if (this.config.legendStyle.position === "right") {
              legendContainer = d3.select(this.legendNode);
              var maxLength = 0;
              config.data.forEach(function(eachVal){
                if(eachVal.length>maxLength)
                  maxLength = eachVal.length;
              });
              maxLength *= 7;
              width = maxLength + 40;
              height = config.data.length*19;
            } else {
              width = this.config.chartStyle.width; // = 800;
              legendContainer = d3.select(this.singleLegendNode);
            }
            mainpanel = legendContainer
            .append("svg")
            .attr("id", this.container.id + ID.getLegendsvg())
            .attr("height",height)
            .attr("width",width);
            var legendpanel = mainpanel.append("g");
            var entry = legendpanel.selectAll("g")
              .data(config.data)
              .enter()
              .append("g")
              .attr("class",function(d,i) {
                // add common class 'legend',
                // and add custom class eg 'entry0', useful for selecting individual entry
                return "legend entry"+i; });

            config.legends = entry;

            config.hasNamedColors = this.isOrdinalScaleWithNames(config.colors);
            if(config.hasNamedColors && config.actualNames==null) {
              config.actualNames = config.data;
            }

            var legendDotRadius = config.dotSize;
            var chartWidthOffset = config.chartWidthOffset;
            var chartHeightOffset = config.chartHeightOffset;
            var startAtX = config.x;
            var startAtY = config.y;
            var colPadding = config.padding;
            var rowPadding = config.padding;

            var calc = {};
            calc.getLegendWidth = function(noOfChars) {
              return (noOfChars*chartWidthOffset)+(legendDotRadius*2)+colPadding;
            };

            var legendMax=d3.max(config.data.map(function(d,i){ return d.length; }));
            var maxColWidth = calc.getLegendWidth(legendMax);
            var maxRowHeight = chartHeightOffset+rowPadding;
            // console.log("maxColWidth="+maxColWidth,"maxRowHeight="+maxRowHeight)

            var len = config.data.length;
            if(config.layoutStyle.indexOf("@width")!=-1)  {
              // width-based - no of columns will be calculated based on width supplied
              calc.noOfCols = Math.floor(config.width / maxColWidth);
              if(calc.noOfCols>len) { calc.noOfCols=len;}
              if(calc.noOfCols<=0) { calc.noOfCols=1; }

              calc.noOfRows = Math.ceil(len/calc.noOfCols);
              // console.log("WIDTH-BASED")
            }
            else { // "@height"
              // height-based - no of columns will be calculated based on height supplied
              calc.noOfRows = Math.floor(config.height / (maxRowHeight));
              if(calc.noOfRows>len) { calc.noOfRows=len; }
              if(calc.noOfRows<1) { calc.noOfRows=1; }

              calc.noOfCols = Math.ceil(len/calc.noOfRows);
              // console.log("HEIGHT-BASED")
            }

            if(config.layoutStyle.indexOf("left2right")!=-1) {
              // left-to-right
              calc.getColNo = function(i) { return Math.floor(i%calc.noOfCols); };
              calc.getRowNo = function(i) { return Math.floor(i/calc.noOfCols); };
              // console.log("left-to-right")
            }
            else {
              // top-to-bottom
              calc.getColNo = function(i) { return Math.floor(i/calc.noOfRows); };
              calc.getRowNo = function(i) { return Math.floor(i%calc.noOfRows); };
              // console.log("top-to-bottom")
            }

            // calculate max-string-length for each column
            var listPerColWidth = [];
            config.data.forEach(function(d,i){
              var colNo = calc.getColNo(i);
              var dlen = d.length;

              if(listPerColWidth[colNo]==undefined || listPerColWidth[colNo]<dlen) {
                listPerColWidth[colNo] = dlen;
              }
            });

            // calculate column X-postion, based on previous column
            var colPositions=[];
            for(var i=0;i<calc.noOfCols;i++) {
              var x;
              if(i==0) {
                x = startAtX;
              }
              else {
                var w0 = listPerColWidth[i-1];  // get max-width of pervious column
                var x0 = colPositions[i-1];    // get x-position of pervious column
                x = x0 + calc.getLegendWidth(w0);
              }
              colPositions[i] = x;
            }

            // totalWidth = lastColPosition - firstColPosition + lastColWidth
//            config["requiredTotalWidth"] = colPositions[calc.noOfCols-1]-colPositions[0]+calc.getLegendWidth(listPerColWidth[calc.noOfCols-1]);
            // totalHeight = (maxRowHeight * No-of-Rows) + padding
//            config["requiredTotalHeight"] = (calc.noOfRows * maxRowHeight) + config.padding;

            var xcall = lang.hitch(this, function(d,i) {
              var colNo = calc.getColNo(i);
              if (this.config.legendStyle['position'] == 'right') {
                colNo = 0;
              }
              var x = colPositions[colNo];
              return x;
            });
            var ycall = lang.hitch(this, function(d,i) {
              var rowNo = calc.getRowNo(i);
              if (this.config.legendStyle['position'] == 'right') {
                rowNo = i;
                return  startAtY + (rowNo * maxRowHeight);
              }
              return 10;
            });

            if (!this.config.legendStyle.displayLegend || this.config.legendStyle.displayLegend === "true") {
            // text
            entry.append("text")
              .attr("x", "18px" )
              .attr("y", ycall)
              .attr("dy", ".35em")
              .attr("text-anchor", "start")
              .text(function(d, i) {
                  return d; })
              .style('font',config.fontStyle);
            // dot
            if(config.dotType=='square') {
              var dot = entry.append("rect")
              .attr("x", function(d,i) {
                return xcall(d,i) - legendDotRadius; } )
              .attr("y", function(d,i) {
                return ycall(d,i) - legendDotRadius; } )
              .attr("width", legendDotRadius*2)
              .attr("height",legendDotRadius*2)
              .attr("fill", function(d, i) {
                return (config.hasNamedColors) ?
                    config.colors(config.actualNames[i]) : config.colors(i); });
            }
            else {
              entry.append("circle")
                .attr("cx", "10px")
                .attr("cy", ycall)
                .attr("r",legendDotRadius)
                .attr("fill", function(d, i) {
                  // console.log("circle fill d=",d,i,config.hasNamedColors,(config.hasNamedColors)?config.actualNames[i]:"N/A",(config.hasNamedColors) ? config.colors(config.actualNames[i]) : config.colors(i))
                  return (config.hasNamedColors) ?
                      config.colors(config.actualNames[i]) : config.colors(i);  });
            }
            }
          });
          return thisPane;
        },

        /**
         * _customToolTip
         * @param tooltipId
         * @param width
         * @param height
         * @returns {}
         */
        _customTooltip : function(tooltipId, width, height) {
          var ttDiv = d3.select("body")
            .append("div")
            .attr("class", "chart-tooltip")
            .attr("id", tooltipId)
            .style("opacity", 1);
          // if(width) {  ttDiv.style("width", width); }
          // if(height) { ttDiv.style("height", height); }

          hideTooltip();

          function showTooltip(content, event) {
            ttDiv.html(content);
            ttDiv.style("display", "");
            updatePosition(event);
          }

          function hideTooltip() {
            ttDiv.style("display", "none");
          }

          function updatePosition(event) {
            var tdiv = document.getElementById(tooltipId);
            var xOffset = 20;
            var yOffset = 10;

            var ttw = tdiv.clientWidth;
            var tth = tdiv.clientHeight;
            var wscrY = window.scrollY || window.pageYOffset;
            var wscrX = window.scrollX || window.pageXOffset;
            var curX = tdiv ? event.clientX + wscrX : event.pageX;
            var curY = tdiv ? event.clientY + wscrY : event.pageY;
            var ttleft = ((curX - wscrX + xOffset*2 + ttw) > window.outerWidth) ? curX - ttw - xOffset*2 : curX + xOffset;
            if (ttleft < wscrX + xOffset) {
              ttleft = wscrX + xOffset;
            }
            var tttop = ((curY - wscrY + yOffset*2 + tth) > window.outerHeight) ? curY - tth - yOffset*2 : curY + yOffset;
            if (tttop < wscrY + yOffset) {
              tttop = curY + yOffset;
            }
            ttDiv.style('top', tttop + 'px').style('left', ttleft + 'px');
          }

          return {
            showTooltip: showTooltip,
            hideTooltip: hideTooltip,
            updatePosition: updatePosition
          };
        },

        /**
         * parseInt
         * @param text
         * @param defaultValue
         * @returns
         */
        parseInt : function(text, defaultValue) {
          if(text == null || text==='' || text==='default') {
            return (arguments.length>1) ? defaultValue : 0;
          }
          else {
            var num = parseInt(text.trim?text.trim():text);
            return isNaN(num) ? (arguments.length>1) ? defaultValue : 0 : num;
          }
        },

        /**
         *
         * @param value
         * @param defaultValue
         * @returns
         */
        parseBool : function(value, defaultValue) {
          if(value != null) {
            if(typeof value === "string")  {
              return (['true','on','yes','y','t'].indexOf(value.toLowerCase()) != -1);
            }
            else {
              return (value) ? true : false;
            }
          }
          else {
            return (arguments.length>1 && (defaultValue)) ? true : false;
          }
        },

        /**
         *
         * @param scale
         * @returns {Boolean}
         */
        isOrdinalScaleWithNames : function(scale) {
          var domain = scale.domain();
          return (domain.length > 0 && (typeof domain[0] === "string"));
        },

        /**
         *
         * @param value
         * @param defaultValue
         * @returns
         */
        defaultValue : function(value, defaultValue) {
          if(value == null || value==='' || value==='default') {
            return (arguments.length>1) ? defaultValue : '';
          }
          else {
            return value;
          }
        },

        /**
         * globalizeDateFormat
         * @param format ignored
         * @param _showUTC ignored
         * @returns {Function}
         */
        globalizeDateFormat : function(format,_showUTC) {
          return function(d,i) {
            return dateLocale.format(new Date(d), { formatLength: "medium" });
          };
        },

        /**
         * globalizeTimeScaleFormat
         * @param _showUTC ignored
         * @returns
         */
        globalizeTimeScaleFormat : function(_showUTC) {
          var _format = function(dt) {
            var bundle = dateLocale._getGregorianBundle();

            if(dt.getMilliseconds()) {
              //return d3.time.format(".%L")(dt);
              return dateLocale.format(new Date(dt),{
                selector: "time",
                formatLength: "long"
                //timePattern: ".SSS"
              });
            }
            else if(dt.getSeconds()) {
              //return d3.time.format(":%S")(dt);
              return dateLocale.format(new Date(dt),{
                selector: "time",
                formatLength: "medium"
//                timePattern: "s"
              });
            }
            else if(dt.getMinutes()) {
              //return d3.time.format("%I:%M")(dt);
              return dateLocale.format(new Date(dt),{
                selector: "time",
                formatLength: "short"
//                timePattern: bundle["timeFormat-short"]
              });
            }
            else if(dt.getHours()) {
//                return d3.time.format("%I %p")(dt);
                return dateLocale.format(new Date(dt),{
                  selector: "time",
                  formatLength: "short"
//                  datePattern: bundle["dateFormatItem-H"]
                });
            }
            else if(dt.getDay() && dt.getDate() != 1) {
              //return d3.locale.timeFormat("%a %d")(dt);
              // day of the week
              return dateLocale.format(new Date(dt),{
                selector: "date",
                datePattern: "EEEE"
              });
            }
            else if(dt.getDate() != 1) {
              //return d3.time.format("%b %d")(dt);
              // month and day
              return dateLocale.format(new Date(dt),{
                selector: "date",
                datePattern: bundle["dateFormatItem-MMMd"]
              });
            }
            else if(dt.getMonth()) {
//                return d3.time.format("%B")(dt);
              return dateLocale.format(new Date(dt),{
                selector: "date",
                datePattern: "MMM"
              });
            }
            else { // YEAR
//                return d3.time.format("%Y")(dt);
              return dateLocale.format(new Date(dt),{
                selector: "date",
                datePattern: "yyy"
              });
            }
          };

          return function(dt0,idx) {
            if(dt0 == null) {
              return null;
            }

            return _format(dt0);
          };
        },

        /**
         * globalizeNumberFormat
         * @returns
         */
        globalizeNumberFormat : function() {
          var _format = function(dt) {
            return numberLocale.format(dt);
          };

          return function(dt0,idx) {
            if(dt0 == null) {
              return null;
            }

            return _format(dt0);
          };
        },

        /**
         *
         */
        get_sr_table_template : function(list, keys, title, isTimeline, showUTC, yAxisTitle) {
          // if pie chart data
          if(typeof keys[1] === "string") {
            keys = [keys[0], [keys[1]]];
          }

          var colnos = 1;
          var firstCol = keys[0];
          if (firstCol === 'time' || firstCol === 'x') {
            // change to translated timeStamp
            firstCol = i18n.STATS_VIEW_DATA_TIMESTAMP;
          }
          var key_name_cols = "<th scope=\"col\" class='graphDataTH'>"+firstCol+"</th>";
          var colnames = keys[1];
          colnames.forEach(function(name,i) {
            var newHeader = name;
            if (name === "y" && yAxisTitle) {
              newHeader = yAxisTitle;
            }
            key_name_cols += "<th scope=\"col\" class='graphDataTH'>"+newHeader+"</th>";
            colnos += 1;
          });

          if (title == null) {
            title = "Chart Data";
          }

          // create data table for accessibility to be used by screen readers
          var sr_table_template = "\<table summary='" + title + "' tabindex=0 class='graphDataTable'> \<thead> \<tr>"+key_name_cols+"</tr> \</thead>";

          var data_rows = "";
          list.forEach(function(d,i){
            var data_cols = "";
            colnames.forEach(function(name,i) {
              if (d[name] || d[name] === 0) { // Zero is falsy, but we want to display 0 so allow this case to be true
                data_cols += "<td class='graphDataTD'>"+d[name]+"</td>";
              } else {
                data_cols += "<td class='graphDataTD'></td>";
              }
            });
            var col0val;
            if (isTimeline === true) {
              // TODO: fix this
              //tm = time.ctime(time.mktime(time.gmtime(int(d[keys[0]])/1000)))
              var tm;
              if(showUTC) {
                tm = (new Date(d[keys[0]])).toUTCString();
              }
              else {
                if(dojo.locale === 'ar' || dojo.locale.indexOf('ar-') > -1){
                  // Arabic requires special logic for formatting its dates, because Dojo doesn't display correctly with the default formatting or Gregorian bundle formats
                  // We have to append two date patterns since the day of the month doesn't appear to the right of the month name even if the datePattern is rearranged.
                  var timestamp = dateLocale.format(new Date(d[keys[0]]),{
                    selector: "date",
                    formatLength: "medium",
                    datePattern: "MMMM ,yyyy HH:mm a"
                  });
                  var day = dateLocale.format(new Date(d[keys[0]]),{
                    selector: "date",
                    formatLength: "medium",
                    datePattern: "dd"
                  });
                  // Add unicode characters to allow concatenating rtl strings
                  tm = "\u200e" + timestamp + "\u200e " + day;
                }
                else{
                  tm = dateLocale.format(new Date(d[keys[0]]), {formatLength: "medium"});
                }
              }
              col0val = tm;
            }
            else {
              col0val = d[keys[0]];
            }
            data_rows += "<tr><th scope=\"row\" class='graphDataTD'>"+col0val+"</th>"+data_cols+"</tr>";
          });

          sr_table_template = sr_table_template +
          "<tbody>"+data_rows+"</tbody> \</table>";

          return sr_table_template;
        }

    });

    return Chart;

});
