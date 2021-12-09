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
        'dojo/i18n!jsExplore/nls/explorerMessages',
        './Chart',
        'd3/d3.min'
        ], function(
            lang,
            declare,
            ID,
            i18n,
            Chart,
            d3
        ){
  return declare("LineChart", [Chart], {
    tooltipObjects: [],
    constructor : function(params) {
      this.config.colors = d3.scale.ordinal().range(["#4178BE", "#df3b4d", "#008a52", "#f19027", "#7f1c7d", "#838329", "#c42684", "#00b0da", "#ffcf04", "#007670"]);
      this.config.isDate = false;
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
        this.config.customTooltip = this._customTooltip(this.config.uniqueid+"_tooltip", this.config.tooltip['width'], this.config.tooltip['height']);
      }

      if(this.config.hAxis) { this.config.isDate = this.parseBool(this.config.hAxis['isDate']); }

      this.config.dataPoints = this.parseBool(this.config.chartStyle['dataPoints']);
      this.config.lineWidth = this.parseInt(this.config.chartStyle['lineWidth']);

      this.config.hasNamedColors = this.isOrdinalScaleWithNames(this.config.colors);
      
      this.config.timezoneOffsetMillis = this.parseInt(this.config.chartStyle['timezoneOffsetMillis']);
      this.config.showUTC = this.parseBool(this.config.chartStyle.showUTC);

      var legendWidth = 0;
      var legendHeight = 0;
      if (this.config.legendStyle !== null) {
        legendWidth = parseInt(this.config.legendStyle['width']);
        legendHeight = parseInt(this.config.legendStyle['height']);
      }
      
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

      // TODO: seems like maybe first time throws an exception that data is undefined
      // ---------------------------------
      // if no data yet
      if (!this.config.data || this.config.data.length === 0) {
        return;
      }

      this.config.x = this.config.data.map(lang.hitch(this, function(d) {
        if(this.config.isDate) {
          return (+d[this.config.xkey]) + this.config.timezoneOffsetMillis;
        }
        else {
          return d[this.config.xkey];
        }
      }));
      this.config.y = this.config.data.map(lang.hitch(this, function(d) {
        return this.config.ykeys.map(function(k) {
          var val = d[k];
          return (val==null) ? 0 : +val;
        });
      }));
      this.config.y1 = this.config.ykeys.map(lang.hitch(this, function(k) {
        return this.config.data.map(function(d) {
          var val = d[k];
          return (val==null) ? 0 : +val;
        });
      }));
      this.config.legends = this.config.ykeys;
      
      var ymin=9999999;
      var ymax=-9999;
      if (this.config.vAxis.domain) {
        ymin = this.config.vAxis.domain[0];
        ymax = this.config.vAxis.domain[1];
      } else {
      for(var i=0; i < this.config.y.length; i++) {
        var pvmin = d3.min(this.config.y[i]);
        if (pvmin < ymin) ymin=pvmin;
        var pvmax = d3.max(this.config.y[i], function(d) {return +d;} );
        if (pvmax > ymax) ymax=pvmax;
      }
      }
      // Handle the case of a flat line not rendering any y-axis
      if (ymin === ymax) {
        if (ymin >= 5) {
          ymin -= 5;
        } else {
          ymin = 0;
        }
        ymax = ymin + 5;
      }

      var margin = {
        left: 5,
        right: 5,
        top: 10,
        bottom: 5
      };
      var chartXTopPadding = 0,
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
          xAxisPadding += 20;
        else if (this.config.hAxis['position'] == 'in')
          xAxisPadding += 10;
        if(this.config.hAxis['label'] !== '')
          xAxisPadding += 20;

        margin.left += 5;
        margin.right += 15;
      }

      var yAxisPadding = 0;
      if (this.config.vAxis !== null) {
        var legendLen = 0; //(this.config.ykeys[this.config.ykeys.length-1].length/2)+5;
        margin.right += legendLen;
        
        if (this.config.vAxis['position'] == 'out')
          yAxisPadding += 40;
        else if (this.config.vAxis['position'] == 'in')
          yAxisPadding += 5;
        if(this.config.vAxis['label'] !== '')
          yAxisPadding += 20;
      }

//      var xAxisOffset = 0;
      if(!this.config.isDate && this.config.hAxis != null) {
        if(this.config.vAxis == null) {
          // add offset for beginning of x-axis, for the first tick label
          yAxisPadding += ((""+this.config.x[0]).length * 3);
        }
        // offset at ending of x-axis, for the last tick label
        xAxisPadding += ((""+this.config.x[this.config.x.length-1]).length * 2);
      }

      if(this.config.dataPoints) {
        this.config.dotRadius = (this.config.lineWidth + 3);
        if(this.config.vAxis == null) {
          yAxisPadding += this.config.dotRadius + 3;
        }
        if(this.config.hAxis == null) {
          xAxisPadding += this.config.dotRadius;
          if (this.config.legendStyle==null || this.config.legendStyle['position'] != 'bottom') {
            xAxisPadding += 3;
          }
        }
      }

      var actualChartWidth = this.config.width - yAxisPadding - margin.left - margin.right;
      var actualChartHeight = this.config.height - xAxisPadding - margin.top - margin.bottom;

      /* The scale is a time scale if isDate == true else its an ordinal scale */
      if(!this.config.isDate) {
        //ordinal scale
        this.config.xScale = d3.scale.ordinal()
          .domain(this.config.x)
          .rangePoints([0, actualChartWidth]);
      }
      else {
        //timeScale - shows epoch time as meaningful values
        if(this.config.showUTC) {
          this.config.xScale = d3.time.scale.utc();
        }
        else {
          this.config.xScale = d3.time.scale();
        }
        var xDomain = [d3.min(this.config.x),d3.max(this.config.x)];
        // user supplied start time
        if(this.config.hAxis['startValue']!=null) {
          var val = +this.config.hAxis['startValue'];
          xDomain[0] = val;
        }

        // user supplied end time
        if(this.config.hAxis['endValue']!=null) {
          var val = +this.config.hAxis['endValue'];
          xDomain[1] = val;
        }
        this.config.xScale
          .domain(xDomain)
          .range([0, actualChartWidth]);
      }
      this.config.yScale = d3.scale.linear()
        .domain([ymin, ymax])
        .range([actualChartHeight, 0]);
      this.config.m = this.config.y[0].length;

      var container = d3.select(chartContainer);
      cid = chartContainer.id.replace( /(:|\.|\[|\]|,)/g, "\\$1" );
      d3.select("#"+cid+ID.getSvg()).remove();
      
//      var totalWidth,totalHeight;
//      if (this.config.legendStyle==null || this.config.legendStyle['position'] == 'right') {  // default 'right'
//        totalWidth = this.config.width + legendWidth;
//        totalHeight = (this.config.height > legendHeight) 
//              ? this.config.height 
//              : legendHeight;
//      }
//      else {
//        totalWidth = (this.config.width > legendWidth)
//              ? this.config.width 
//              : legendWidth;
//        totalHeight = this.config.height + legendHeight;
//      }
      var totalWidth,totalHeight;
        totalWidth = this.config.width; // + legendWidth;
        totalHeight = this.config.height;
      // console.log("totalWidth=",totalWidth,"totalHeight=",totalHeight)
      
      var mainpanel = container
        .append("svg")
        .attr("id", chartContainer.id + ID.getSvg())
        .attr("class", "linechart");
      // .attr("width", totalWidth )
      // .attr("height", totalHeight)

      var panel = mainpanel.append("g");
      //  .attr("transform","translate("+ (margin.left+yAxisPadding) + "," + (margin.top+chartXTopPadding)  + ")" );

      var spanel= panel.append("g")
        //.attr("transform", function(d,i) { return "translate(" + xScale(x[i]) + ",0)"; });
        .attr("transform", function(d,i) {
            return "translate(0,0)"; })
        .attr("class","chartdisplay");

      var me = this;
      var line = d3.svg.line()
        .x(function(d,i) {
          return me.config.xScale(me.config.x[i]);})
        .y(function(d,i) {
          return me.config.yScale(d); })
        // .interpolate("cardinal-closed");
        .interpolate(this.config.chartStyle['interpolate']);

      var paths = spanel.selectAll("g.path")
        .data(this.config.y1)
        .enter()
        .append("g")
        .attr("class","path")
        .each(function(d,i) {
          var path = d3.select(this);
          me.dot(d,i,path);
        });

      paths.append("svg:path")
        .attr("d", function(d,i) {
            return line(d,i);})
        .attr("class","svgpath")
        .style("stroke-width", this.config.lineWidth)
        .style("stroke", function(d,i) {
            return (me.config.hasNamedColors) ? 
                me.config.colors(me.config.ykeys[i]) : me.config.colors(i);})
        .style("fill","none");


      if(this.config.clickAction != null) {
        mainpanel.style("cursor","pointer");
      }

      // render the x and y axes
      this._renderAxes(panel);

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
            .attr("height",actualLegendHeight);
        if (this.config.legendStyle['position'] == 'right') {
          chart_legends
            .attr("x",(margin.left + actualChartWidth + yAxisPadding + margin.right))
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
        var newLegendWidth = chart_legends.attr("requiredTotalWidth");
        var newLegendHeight = chart_legends.attr("requiredTotalHeight");
        if (this.config.legendStyle['position'] == 'right') {
          if(newLegendWidth > legendWidth) { totalWidth = totalWidth -legendWidth + newLegendWidth; }
          if(newLegendHeight > totalHeight) { totalHeight = newLegendHeight; }
        }
        else {
          if(newLegendWidth > totalWidth) { totalWidth = newLegendWidth; }
          if(newLegendHeight > legendHeight) { totalHeight = totalHeight - legendHeight + newLegendHeight; }
        }

        if (this.config.legendStyle['position'] == 'top') {
          chartXTopPadding = chartXTopPadding - legendHeight + newLegendHeight;
        }
      }

      mainpanel
        .attr("width", totalWidth)
        .attr("height", totalHeight);
      panel
        .attr("transform","translate("+ (margin.left+yAxisPadding) + "," + (margin.top+chartXTopPadding)  + ")" );
    },

    dot : function(p,q,path) {
      var me = this;
      var point = path.selectAll("g.point")
        .data(function(d) {
            return d;})
        .enter().append("g").attr("class","point");
      
      point.append("svg:circle")
        .attr("cx",function(d,i) {
            return me.config.xScale(me.config.x[i]);})
        .attr("cy",function(d,i) {
            return me.config.yScale(d);})
        .attr("r", this.config.dotRadius)
        .attr("fill", function(d,i) {
            return (me.config.hasNamedColors) ? me.config.colors(me.config.ykeys[q]) : me.config.colors(q); })
        .attr("fill-opacity",function(d,i) {
              return 0; });

      if(this.config.tooltip != null) {

        point.on("mouseover",lang.hitch(this, function(d,i) {
          this.config.tooltip_showAllLegends = this.parseBool(this.config.tooltip['showAllLegends'], this.config.tooltip_showAllLegends);
          if (this.zooming) {
            return;
          }
          me.buildCustomizeTooltip(d, i);
          this.config.customTooltip.showTooltip(content, (d3.event || window.event));
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
//        point.on("click",function(d,i) {
//          var actionType = me.config.clickAction['actionType'];
//          var actionTarget = me.config.clickAction['actionTarget'];
//          var actionUrl = me.config.clickAction['actionUrl'];
//
//          var actionUrlParams = 'data='+me.config.x[i]+','+me.config.legend[q]+','+d+'&keys='+me.config.keys.toString();
//          if (actionType == 'callback') {
//            eval( actionTarget+"(this.config.x[i], this.config.legend[q], d)" );
//          } else {
//            UTILS.executeClickAction(actionType, actionTarget, actionUrl, actionUrlParams);
//          }
//          return;
//        });
//      }
    }, // end of dot

    buildCustomizeTooltip : function(d, i) {
      var dateFormat = this.globalizeDateFormat("", this.config.showUTC);
      var displayDate = new Date(this.config.x[i]);
      displayDate = dateFormat(displayDate);
      var formatter = d3.format(".3n");
      var val = lang.replace(i18n.STATS_VALUE, [formatter(d)]);
      content = displayDate + '<br/>' + val;
    }

  });
	
});