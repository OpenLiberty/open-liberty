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
        'dojo/_base/declare', 
        'jsExplore/utils/ID',
        './Chart',
        'd3/d3.min'
        ], function(
            lang,
            declare,
            ID,
            Chart,
            d3
        ){
  return declare("BarChart", [Chart], {
    tooltipObjects: [],
    constructor : function(params) {
      this.config.colors = d3.scale.category20();
      this.config.barWidth = 10;
      this.config.barWidthMin = 3;
      this.config.barWidthMax = 20;
    },

//    postCreate : function(params) {
//      this.inherited(arguments);
//    },

    drawChart : function(chartContainer) {
      this.config.uniqueid = chartContainer.id;
      this.config.xkey = this.config.keys[0];
      this.config.ykeys = (typeof this.config.keys[1] === "string") ? [this.config.keys[1]] : this.config.keys[1];
      this.config.width = parseInt(this.config.chartStyle['width']);
      this.config.height = parseInt(this.config.chartStyle['height']);
      if (this.config.tooltip != null && !this.config.customTooltip) {
        this.config.customTooltip = this._customTooltip(this.config.uniqueid+"_tooltip",  this.config.tooltip['width'], this.config.tooltip['height']);
      }
      this.config.dataInterval = this.config.chartStyle['dataInterval'];
      var legendWidth = 0;
      var legendHeight = 0;
      if (this.config.legendStyle !== null) {
        legendWidth = parseInt(this.config.legendStyle['width']);
        legendHeight = parseInt(this.config.legendStyle['height']);
      }

      this.config.timezoneOffsetMillis = this.parseInt(this.config.chartStyle['timezoneOffsetMillis']);
      this.config.showUTC = this.parseBool(this.config.chartStyle.showUTC);

      var responsiveWidth = parseInt(this.config.responsiveWidth);
      if (responsiveWidth > 0) {
        var totalWidth;
        if (this.config.legendStyle==null || this.config.legendStyle['position'] == 'right') {
          totalWidth = this.config.width + legendWidth;
        }
        else {
          totalWidth = (this.config.width > legendWidth)
          ? this.config.width 
              : legendWidth;
        }
        if(responsiveWidth < totalWidth) {
          if(responsiveWidth < this.config.width) {
            this.config.width = responsiveWidth;

            if (this.config.legendStyle !== null) {
              if (this.config.legendStyle['position'] == 'right') {
                this.config.legendStyle['position'] = 'bottom';
                legendWidth = this.config.width;
                legendHeight = 10;  // providing reduced height, as it will be readjusted
              }
              else {
                if(this.config.width < legendWidth) {
                  legendWidth = this.config.width;
                }
              }
            }
          }
          else {
            if (this.config.legendStyle !== null) {
              if (this.config.legendStyle['position'] == 'right') {
                var legendLen = (this.config.ykeys[0].length*10)+5;
                var diff = responsiveWidth - this.config.width;
                if(diff > legendLen) {
                  legendWidth = diff;
                }
                else {
                  this.config.legendStyle['position'] = 'bottom';
                  legendWidth = this.config.width;
                  legendHeight = 10;   // providing reduced height, as it will be readjusted
                }
              }
              else {
                legendWidth = responsiveWidth;
              }
            }
          }
        }
      }

      this.config.hasNamedColors = this.isOrdinalScaleWithNames(this.config.colors);

      // ---------------------------------
      // if no data yet
      if (!this.config.data) {
        return;
      }
      var yearInMS = 1000*60*60*24*365; // approximate milliseconds in a year
      var allDates=[], ymax=0, ymin=0, minTimeDiff=yearInMS;
      this.config.dataLayers = this.config.ykeys.map(function(){return [];});
      if (this.config.dataLayers.length === 0) {
        return;
      }
      this.config.data.forEach(lang.hitch(this, function(d,i) {
        // Note: the time value should always be either long or long-string value
        //       '+' will convert long-string to numeric value, 
        //       but if the time value is date-string - it will create unintended problem.
        var tmillis =  (+d[this.config.xkey]) + this.config.timezoneOffsetMillis;
        var time = new Date(tmillis);
        allDates[i] = time;
        if(i>0) {
          var diff = allDates[i].getTime()-allDates[i-1].getTime();
          if(diff < 0) { diff *= -1; }
          if(diff > 0 && diff < minTimeDiff) { minTimeDiff = diff; }
        }
        var ytotal = 0;
        this.config.ykeys.forEach(lang.hitch(this, function(k,ki){
          var yval = d[k];
          var yvalue = yval==null ? 0 : +yval;
          ytotal += yvalue;
          this.config.dataLayers[ki][i] = {
              "x" : time,
              "y" : yvalue,
              "i" : ki 
          };
        }));
        if(ytotal < ymin) { ymin = ytotal; }
        if(ytotal > ymax) { ymax = ytotal; }
      }));
      var startTime = d3.min( allDates );
      var endTime = d3.max( allDates );

      this.config.stackedData = d3.layout.stack()(this.config.dataLayers);
      // ----------------------------------
      this.config.legends = this.config.ykeys;
      var margin = {
          left: 5,
          right: 5,
          top: 10,
          bottom: 0
      };
//    var panelOffsetX = margin.top,  
//    panelOffsetY = margin.left,
      var	chartXTopPadding = 0,
      chartYLeftPadding = 0;
      if(this.config.legendStyle != null) {
        if(this.config.legendStyle['position']=='left') {
          chartYLeftPadding += legendWidth;
        }
        else if(this.config.legendStyle['position']=='top') {
          chartXTopPadding += legendHeight;
        }
      }

      var xAxisPadding = 0;
      if (this.config.hAxis !== null) {
        if (this.config.hAxis['position'] == 'out')
          xAxisPadding += 25;
        else if (this.config.hAxis['position'] == 'in')
          xAxisPadding += 10;
        if(this.config.hAxis['label'] !== '')
          xAxisPadding += 20;

        // user supplied start time
        if(this.config.hAxis['startValue']!=null) {
          var val = +this.config.hAxis['startValue'];
          if(startTime && val > 0 && val < startTime.getTime()) {
            startTime = new Date(val);
          }
        }

        // user supplied end time
        if(this.config.hAxis['endValue']!=null) {
          var val = +this.config.hAxis['endValue'];
          if(endTime && val > 0 && val > endTime.getTime()) {
            endTime = new Date(val);
          }
        }

        margin.left += 5;
        margin.right += 10;
      }

      var yAxisPadding = 0;
      if (this.config.vAxis !== null) {
        if (this.config.vAxis['position'] == 'out')
          yAxisPadding += 40;
        else if (this.config.vAxis['position'] == 'in')
          yAxisPadding += 5;
        if(this.config.vAxis['label'] !== '')
          yAxisPadding += 20;
      }

      var actualChartWidth = this.config.width - yAxisPadding - margin.left - margin.right;
      var actualChartHeight = this.config.height;
//      if (this.config.legendStyle.position === "right") {
        actualChartHeight = this.config.height - xAxisPadding - margin.top - margin.bottom;
//      }
      // startValue should atleast as big as interval
      var interval = null; //,minTime = UTILS.hour;
// can't find any place dataInterval is being set      
//      if(this.config.dataInterval != null) {
//        interval = eval("UTILS."+this.config.dataInterval);
//        var diff = endTime.getTime()-startTime.getTime();
//        if(diff<interval) {
//          startTime = new Date(endTime.getTime()-interval);
//        }
//      }
//      else {
        // if only one datapoint, then x-axis time-range is at least an hour,
        if(endTime && startTime && endTime.getTime()==startTime.getTime()) {
          startTime = new Date(startTime.getTime()-(1000*60)); //UTILS.minute);
        }
//      }

      if(this.config.showUTC) {
        this.config.xScale = d3.time.scale.utc();
      }
      else {
        this.config.xScale = d3.time.scale();
      }
      this.config.xScale
      .domain([startTime, endTime])
      .rangeRound([0, actualChartWidth]);

      this.config.barWidth = this.normalizedBarWidth(startTime,minTimeDiff,this.config.xScale,interval);

      // for space at the start of xAxis
      var xBarOffset = this.config.barWidth * 20/100;
      // console.log("barWidth="+this.config.barWidth,"xBarOffset="+xBarOffset);
      if(xBarOffset < 3) { xBarOffset = 3; }
      if(xBarOffset > 10) { xBarOffset = 10; }
      var newStartTime = this.config.xScale.invert(-xBarOffset);
      // for space at the end of xAxis
      var newEndTime = this.config.xScale.invert(actualChartWidth+this.config.barWidth+xBarOffset);
      this.config.xScale.domain([newStartTime, newEndTime]);

      this.config.yScale = d3.scale.linear()
      .domain([ymin, ymax])
      .range([actualChartHeight,0]);

      var totalWidth,totalHeight;
      //if (this.config.legendStyle==null || this.config.legendStyle['position'] == 'right') {  // default 'right'
        totalWidth = this.config.width; // + legendWidth;
        totalHeight = this.config.height;
//        totalHeight = (this.config.height > legendHeight) 
//        ? this.config.height 
//            : legendHeight;
      //}
      //else {
      //  totalWidth = (this.config.width > legendWidth)
      //  ? this.config.width 
      //      : legendWidth;
      //  totalHeight = this.config.height; // + legendHeight;
      //}
       //console.log("totalWidth=",totalWidth,"totalHeight=",totalHeight)
       //console.log("  this.config.width=", this.config.width,"actualChartWidth=",actualChartWidth,"responsiveWidth=",responsiveWidth)

      var container = d3.select(chartContainer);
      cid = chartContainer.id.replace( /(:|\.|\[|\]|,)/g, "\\$1" );
      d3.select("#"+cid+ID.getSvg()).remove();
      // first check to see if the svg has already been created
//    console.error("looking for " + "#" + chartContainer.id);
      var mainpanel = d3.select("#" + chartContainer.id);
      var panel = null;
//    console.error("main? ", mainpanel[0][0]);
//    if (!mainpanel[0][0]) {
      mainpanel = container
      .append("svg")
      .attr("id", chartContainer.id + ID.getSvg())
      .attr("class", "barchart");
      //	.attr("width", totalWidth)
      //	.attr("height", totalHeight)
      panel = mainpanel.append("g")
      .attr("id", chartContainer.id + ID.getSvgG());
      // .attr("transform","translate("+ (margin.left+yAxisPadding) + "," + (margin.top+chartXTopPadding)  + ")" );
//    } else {
//    panel = d3.select("#" + chartContainer.id + "svgG");
//    panel = mainpanel.append("g");
//    }

      // render the x and y axes
      this._renderAxes(panel);

      // stacked bars group
      var spanel = panel.selectAll('g.category')
      .data(this.config.stackedData)
      .enter()
      .append("g")
      .attr("class", "category")
      .style("fill", lang.hitch(this, function(d, i) { 
        return this.config.hasNamedColors ? 
            this.config.colors(this.config.ykeys[i]) : this.config.colors(i); }));

      var me = this;
      spanel.each(function(d,i) {
        var category = d3.select(this);
        me._drawBar(d,i,category);
      });
      // stacked bars group

      if(this.config.clickAction != null) {
        spanel.style("cursor","pointer");
      }

      if(this.config.legendStyle != null) {
        var actualLegendWidth,actualLegendHeight;
        if (this.config.legendStyle['position'] == 'right') {
          actualLegendWidth = legendWidth - margin.right;
          actualLegendHeight = legendHeight - margin.top;
        }
        else if (this.config.legendStyle['position'] == 'top') {
          actualLegendWidth = legendWidth - margin.left;
          if(actualLegendWidth > actualChartWidth) {
            actualLegendWidth -= yAxisPadding;
          }
          actualLegendHeight = legendHeight - margin.top;
        }
        else {
          actualLegendWidth = legendWidth - margin.left;
          if(actualLegendWidth > actualChartWidth) {
            actualLegendWidth -= yAxisPadding;
          }
          actualLegendHeight = legendHeight - margin.bottom;
        }

        var chart_legends = this._chart_legends()
        .attr("data",this.config.legends)
        .attr("colors",this.config.colors)
        .attr("width",actualLegendWidth)
        .attr("height",actualLegendHeight)
        .attr("dotType","circle");
        if (this.config.legendStyle['position'] == 'right') {
          chart_legends
          .attr("x",(margin.left + actualChartWidth + yAxisPadding + margin.right*2))
          .attr("y",margin.top);
        }
        else if (this.config.legendStyle['position'] == 'top') {
          chart_legends
          .attr("x",(margin.left + yAxisPadding))
          .attr("y",(margin.top));
        }
        else {
          chart_legends
          .attr("x",(margin.left + yAxisPadding))
          .attr("y",(margin.top + actualChartHeight + xAxisPadding + margin.bottom*2));
        }
        chart_legends(mainpanel);

        // re-adjust total chart width/height for responsiveness 
        // eg. if right-positioned legend is move to bottom
//        var newLegendWidth = chart_legends.attr("requiredTotalWidth");
//        var newLegendHeight = chart_legends.attr("requiredTotalHeight");
//        if (this.config.legendStyle['position'] == 'right') {
//          if(newLegendWidth > legendWidth) { totalWidth = totalWidth -legendWidth + newLegendWidth; }
//          if(newLegendHeight > totalHeight) { totalHeight = newLegendHeight; }
//        }
//        else {
//          if(newLegendWidth > totalWidth) { totalWidth = newLegendWidth; }
//          if(newLegendHeight > legendHeight) { totalHeight = totalHeight - legendHeight + newLegendHeight; }
//        }

//        if (this.config.legendStyle['position'] == 'top') {
//          chartXTopPadding = chartXTopPadding - legendHeight + newLegendHeight;
//        }
      }

      mainpanel
      .attr("width", totalWidth)
      .attr("height", totalHeight);
      panel
      .attr("transform","translate("+ (margin.left+yAxisPadding) + "," + (margin.top+chartXTopPadding)  + ")" );
    },


    _drawBar : function(p,q,parent) {
      // each rect bar in the stacked bar group
      var bar = parent.selectAll("rect")
      .data(p)
      .enter().append('rect')
      .attr("class","bar")
      .attr('x', lang.hitch(this, function(d) {
        return this.config.xScale(d.x); }))
        .attr('y', lang.hitch(this, function(d) {
          return this.config.yScale(d.y0 + d.y); }))
          // .attr('width', this.config.xScale.rangeBand())
          .attr('width', this.config.barWidth)
          .attr('height', lang.hitch(this, function(d) {
            return (this.config.yScale(d.y0) - this.config.yScale(d.y0 + d.y));}));

      if(this.config.tooltip != null) {
        var dateFormat = this.globalizeDateFormat("", this.config.showUTC);
        this.config.tooltip_showAllLegends = this.parseBool(this.config.tooltip['showAllLegends'], this.config.tooltip_showAllLegends);

        bar.on("mouseover",lang.hitch(this, function(d,i) {
          if (this.zooming) {
            return;
          }
          var date = d.x;
          var displayDate = dateFormat(date); 
          var content = displayDate;
          if(this.config.tooltip_showAllLegends) {
            var d = this.config.data[i];
            for (var i = this.config.ykeys.length; i > -1; i--) {
              var k = this.config.ykeys[i];
              if(+d[k]>0) {
                var color = this.config.colors( this.config.hasNamedColors ? k : i );
                content += "<br/><span style='color:"+color+";'>" + k + " : <b>" + d[k] + "</b></span>" ;
              }
            }
          }
          else {
            content += "<br/>" + this.config.ykeys[d.i] + " : <b>" + d.y  + "</b>" ;
          }
          this.config.customTooltip.showTooltip(content, (d3.event || window.event));
          this.tooltipObjects.push(this.config.customTooltip);
        }))
        .on("mouseout",lang.hitch(this, function(d,i) {
          this.config.customTooltip.hideTooltip();
          this.tooltipObjects.forEach(function(eachToolTip){
            eachToolTip.hideTooltip();
          });
          this.tooltipObjects = [];
        }));
      }

// TODO: currently clicking on the bar has no additional behavior
//      if(this.config.clickAction != null) {
//        bar.on("click",lang.hitch(this, function(d,i) {
//          this.config.customTooltip.hideTooltip();
//          var actionType = this.config.clickAction['actionType'];
//          var actionTarget = this.config.clickAction['actionTarget'];
//          var actionUrl = this.config.clickAction['actionUrl'];
//          var actionUrlParams = 'data='+d.x+','+ d.y +'&keys='+this.config.xkey+','+this.config.legends[q];
//          if (actionType == 'callback') {
//            eval( actionTarget+"(d.x,d.y,this.config.legends[q],i)" );
//          } else {
//            UTILS.executeClickAction(actionType, actionTarget, actionUrl, actionUrlParams);
//          }
//          if(this.config.tooltip != null) {
//            this.config.customTooltip.hideTooltip();
//          }
//          return;
//        }));
//      }
    },

    // The idea here is to figure out the minimum time between 2 data points. Then, based on
    // that difference, to calculate the width of the bars so that they won't overlap.
    // minTimeDiff: the minimum time between 2 data points
    // interval: seems to always be null
    normalizedBarWidth : function(startTime,minTimeDiff,xScale,interval) {
      if (!startTime) {
        return this.config.barWidthMin;
      }
      var timeDiff = (interval!=null && minTimeDiff > interval) ? interval : minTimeDiff;
      var dt1 = new Date(startTime.getTime()+timeDiff);
      var barWidth = (xScale(dt1)-xScale(startTime))*65/100;
      return (barWidth < this.config.barWidthMin) ? this.config.barWidthMin : (barWidth > this.config.barWidthMax) ? this.config.barWidthMax : barWidth;  
    }
  });
	
});