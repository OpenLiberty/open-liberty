/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
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
        'js/common/platform',
        'dojo/_base/declare',
        'dojo/_base/lang',
        'dojo/dom',
        'dojo/keys',
        'dojo/number',
        'd3/d3.min',
        'dijit/layout/ContentPane',
        'dijit/_TemplatedMixin',
        'dojo/text!./templates/DashboardPane.html',
        'dojo/i18n!jsExplore/nls/explorerMessages',
        'jsExplore/resources/Observer',
        'jsShared/utils/imgUtils'
        ], function(
                platform,
                declare,
                lang,
                dom,
                keys,
                number,
                d3,
                ContentPane,
                _TemplatedMixin,
                template,
                i18n, 
                Observer,
                imgUtils
            ){

    return declare('DashboardPane', [ ContentPane, _TemplatedMixin, Observer], {
        id : '-OverviewPane',
        view : 'dashboard-',
        collectionType : '',
        collection : null,
        templateString : template,
        dashboardIconId : '',
        dashboardResourceLabel : '',
        viewAllLabel : '',   // Button title/aria-label for left side
        totalLabel : i18n.TOTAL,
        dashboardGraphPaneId : '',
        dashboardResourceNumberId : '',
        dashboardResourceNumber : 0,
        stateLabels : {
            up : i18n.RUNNING,
            partial : i18n.PARTIALLY_RUNNING,
            down : i18n.STOPPED,
            unknown : i18n.UNKNOWN
        },
        dashboardResourceUnknownButtonNumberId : '',
        dashboardResourceRunningButtonNumberId : '',
        dashboardResourceStoppedButtonNumberId : '',

        constructor : function(params) {              // collectionType, label
            this.collectionType = params[0];          // Host, Application, Server, etc
            this.dashboardResourceLabel = params[1];  // "Hosts", "Applications", translated
            
            this.id = this.view + this.collectionType + '-DashboardPane';
            this.dashboardIconId = this.view + this.collectionType + '-Icon';
            this.dashboardResourceId = this.view + this.collectionType + '-OverviewPane';
            this.dashboardGraphPaneId = this.view + this.collectionType + '-GraphPane';
            this.dashboardResourceNumberId = this.view + this.collectionType + '-OverviewPane' + '-Number';
            this.dashboardResourceUnknownButtonNumberId = this.view + this.collectionType + '-Unknown-Count';
            this.dashboardResourceRunningButtonNumberId = this.view + this.collectionType + '-Running-Count';
            this.dashboardResourceStoppedButtonNumberId = this.view + this.collectionType + '-Stopped-Count';
            
            this.__setViewAllLabel();
            // initialize the labels for the states (like 'Running' vs. 'Started')
            this._setStateLabels();
            this.runningLabel = this.stateLabels.up;
            this.stoppedLabel = this.stateLabels.down;
            this.unknownLabel = this.stateLabels.unknown;
        },   
        
        /*
         * Unsubscribe this observer resource 
         */
        destroy: function() {
          this.inherited(arguments);

          if (this.resource) {
            this.resource.unsubscribe(this);   
          }
        }, 
        
        postCreate : function() {
            this.inherited(arguments);

            if(this.iconTypeNode){
              this.iconTypeNode.innerHTML = imgUtils.getSVG(this.collectionType.toLowerCase() + '-dashboard');
            
              this.runIcon.innerHTML = imgUtils.getSVG("status-running");
              this.stopIcon.innerHTML = imgUtils.getSVG("status-stopped");
              this.unknownIcon.innerHTML = imgUtils.getSVGSmall("unknown-white");
            }                        
        },
        
        /**
         * Sets the aria-label and title for the collection type of this pane.
         */
        __setViewAllLabel: function() {
          switch (this.collectionType) {
            case 'Application':
              this.viewAllLabel = i18n.DASHBOARD_VIEW_ALL_APPS;
              break;  
            case 'Server':
              this.viewAllLabel = i18n.DASHBOARD_VIEW_ALL_SERVERS;
              break;  
            case 'Cluster':
              this.viewAllLabel = i18n.DASHBOARD_VIEW_ALL_CLUSTERS;
              break;  
            case 'Host':
              this.viewAllLabel = i18n.DASHBOARD_VIEW_ALL_HOSTS;
              break;  
            case 'Runtime':
              this.viewAllLabel = i18n.DASHBOARD_VIEW_ALL_RUNTIMES;
              break;  
          }
        },
        
        /**
         * Computes the 'total' number of elements in this collection.
         * 
         * @return {number} The total number of elements in the collection
         */
        __computeTotal: function() {
          if (this.collection.type === 'summary') {
            if (this.collectionType === 'Application') {
              var apps = this.collection.applications;
              return apps.up + apps.down + apps.unknown + apps.partial;
            } else
            if (this.collectionType === 'Cluster') {
              var clusters = this.collection.clusters;
              return clusters.up + clusters.down + clusters.unknown + clusters.partial;
            } else
            if (this.collectionType === 'Server') {
              var servers = this.collection.servers;
              return servers.up + servers.down + servers.unknown;
            } else
            if (this.collectionType === 'Host') {
              var hosts = this.collection.hosts;
              return hosts.up + hosts.down + hosts.unknown + hosts.partial + hosts.empty;
            } else
            if (this.collectionType === 'Runtime') {
                var runtimes = this.collection.runtimes;
                return runtimes.up + runtimes.down + runtimes.unknown + runtimes.partial + runtimes.empty;
            }
          } else {
            return this.collection.list.length;  
          }
        },
        
        /**
         * Computes the 'running' number of elements in this collection.
         * 
         * @return {number} The total number of running elements in the collection
         */
        __computeRunning: function() {
          if (this.collection.type === 'summary') {
            if (this.collectionType === 'Application') {
              var apps = this.collection.applications;
              return apps.up + apps.partial;
            } else
            if (this.collectionType === 'Cluster') {
              var clusters = this.collection.clusters;
              return clusters.up + clusters.partial;
            } else
            if (this.collectionType === 'Server') {
              var servers = this.collection.servers;
              return servers.up;
            } else
            if (this.collectionType === 'Host') {
              var hosts = this.collection.hosts;
              return hosts.up + hosts.partial;
            } else
            if (this.collectionType === 'Runtime') {
              var runtimes = this.collection.runtimes;
              return runtimes.up + runtimes.partial;
            }
          } else {
            return this.collection.up + (this.collection.partial ? this.collection.partial : 0);  
          }
        },
        
        /**
         * Computes the 'stopped' number of elements in this collection.
         * 
         * @return {number} The total number of stopped elements in the collection
         */
        __computeStopped: function() {
          if (this.collection.type === 'summary') {
            if (this.collectionType === 'Application') {
              var apps = this.collection.applications;
              return apps.down;
            } else
            if (this.collectionType === 'Cluster') {
              var clusters = this.collection.clusters;
              return clusters.down;
            } else
            if (this.collectionType === 'Server') {
              var servers = this.collection.servers;
              return servers.down;
            } else
            if (this.collectionType === 'Host') {
              var hosts = this.collection.hosts;
              return hosts.down + hosts.empty;
            } else
            if (this.collectionType === 'Runtime') {
              var runtimes = this.collection.runtimes;
              return runtimes.down + runtimes.empty;
            }
          } else {
            return this.collection.down + (this.collection.empty ? this.collection.empty : 0);  
          }
        },
        
        /**
         * Computes the 'total' number of elements in this collection.
         * 
         * @return {number} The total number of unknown elements in the collection
         */
        __computeUnknown: function() {
          if (this.collection.type === 'summary') {
            if (this.collectionType === 'Application') {
              var apps = this.collection.applications;
              return apps.unknown;
            } else
            if (this.collectionType === 'Cluster') {
              var clusters = this.collection.clusters;
              return clusters.unknown;
            } else
            if (this.collectionType === 'Server') {
              var servers = this.collection.servers;
              return servers.unknown;
            } else
            if (this.collectionType === 'Host') {
              var hosts = this.collection.hosts;
              return hosts.unknown;
            } else
            if (this.collectionType === 'Runtime') {
              var runtimes = this.collection.runtimes;
              return runtimes.unknown;
            }
          } else {
            return this.collection.unknown;  
          }
        },
        
        _buildGraph : function() {
          var graphContainerId = this.dashboardGraphPaneId.replace( /(:|\.|\[|\]|,)/g, "\\$1" );
          d3.select("#"+graphContainerId+"svg").remove();
          var dataset = [
                         { count: this.__computeRunning(), type: 'running' }, 
                         { count: this.__computeStopped(), type: 'stopped' },
                         { count: this.__computeUnknown(), type: 'unknown' }
                        ];
          var percentUP = number.format(0, {type: 'percent'});
          if (this.__computeTotal() > 0) {
            percentUP = number.format(this.__computeRunning()/this.__computeTotal(), {type: 'percent'});
          }
 
          var width = 75;       
          var height = 75;      
          var innerRadius = 31; 
          var outerRadius = 37; 
          var midRadius = (outerRadius + innerRadius) / 2;

          var color = d3.scale.ordinal().range( ['#4b8400', '#9F1F25', '#d74108']);

          var svg = d3.select("#" + graphContainerId)
          .append('svg')
          .attr("id", graphContainerId + "svg")
          .attr('width', '100%')
          .attr('height', '100%')
          .attr('class', 'detailPercentageLabel')
          .append('g')
          .attr('transform', 'translate(' + (width / 2) + 
              ',' + (height / 2) + ')');

          var arc = d3.svg.arc()
          .innerRadius(function (d) {
            if (d.data.type === 'stopped') {
              return midRadius - 1;
            }
            return innerRadius;
          })
          .outerRadius(function (d) { 
            if (d.data.type === 'stopped') {
              return midRadius + 1;
            }
            return outerRadius;
          });

          var pie = d3.layout.pie()
          .value(function(d) { return d.count; })
          .sort(null);

          var path = svg.selectAll('path')
            .data(pie(dataset))
            .enter()
            .append('path')
            .attr('d', arc)
            .attr('fill', function(d, i) { 
              return color(i);
            });

          var legend = svg
            .append('g')
            .attr('transform', 'translate(0,5)');
          legend.append('text')
            .attr("text-anchor", "middle")
            .attr("fill", "#4b8400")
            .text(percentUP);
        },

        _setStateLabels : function() {
            // initialize the labels for the states (like 'Running' vs. 'Started')
        	  this.stateLabels.up = i18n.RUNNING;
            if(this.collectionType ==='Server') {
              this.stateLabels.up = i18n.RUNNING;
            } else if (this.collectionType === 'Host') {
              this.stateLabels.up = i18n.HOST_WITH_SERVERS_RUNNING;
            } else if (this.collectionType === 'Runtime') {
              this.stateLabels.up = i18n.RUNTIME_WITH_SERVERS_RUNNING;
            }

            this.stateLabels.partial = i18n.PARTIALLY_RUNNING;
            if (this.collectionType === 'Host') {
            	this.stateLabels.partial = i18n.HOST_WITH_SOME_SERVERS_RUNNING;
            } else if (this.collectionType === 'Runtime') {
              this.stateLabels.partial = i18n.RUNTIME_WITH_SOME_SERVERS_RUNNING;
            }

            this.stateLabels.down = i18n.STOPPED;
            if(this.collectionType ==='Host') {
            	this.stateLabels.down = i18n.HOST_WITH_ALL_SERVERS_STOPPED;
            } else if (this.collectionType === 'Runtime') {
              this.stateLabels.down = i18n.RUNTIME_WITH_ALL_SERVERS_STOPPED;
            }

            this.stateLabels.unknown = i18n.UNKNOWN;
            if(this.collectionType ==='Host') {
            	this.stateLabels.unknown = i18n.ALL_SERVERS_UNKNOWN;
            }
        },
        
        /**
         * Handles the behaviour when widget.set('collection', obj) gets called.
         * @param collection
         */
        _setCollectionAttr : function(collection) {
          this.collection = collection;

          if (collection) {
            this.collection.subscribe(this);
            this.dashboardResourceNumber = this.__computeTotal();

            if (this.stateNode) {
              this.stateNode.destroyDescendants();
            }
            this._updateCount();
            this._buildGraph();
          }
        },
        
        _updateCount: function() {
          this.dashboardResourceNumber = this.__computeTotal();
          var dashboardResourceNumberDom = dom.byId(this.dashboardResourceNumberId);
          if(dashboardResourceNumberDom){
            dashboardResourceNumberDom.innerHTML = this.dashboardResourceNumber;
          }  
          
          var dashboardResourceUnknownButtomDom = dom.byId(this.dashboardResourceUnknownButtonNumberId);
          if(dashboardResourceUnknownButtomDom){
            dashboardResourceUnknownButtomDom.innerHTML = this.__computeUnknown();
            if (this.__computeUnknown() === 0) {
              this.unknownNode.style.display = "none";
            } else {
              this.unknownNode.style.display = "block";
            }
          } 
          
          var dashboardResourceRunningButtomDom = dom.byId(this.dashboardResourceRunningButtonNumberId);
          if(dashboardResourceRunningButtomDom){
            dashboardResourceRunningButtomDom.innerHTML = this.__computeRunning();
          }  
          
          var dashboardResourceStoppedButtomDom = dom.byId(this.dashboardResourceStoppedButtonNumberId);
          if(dashboardResourceStoppedButtomDom){
            dashboardResourceStoppedButtomDom.innerHTML = this.__computeStopped();
          }  
        },
        
        _updateGraph: function() {
          // need to update pie chart
          this._buildGraph();
        },
        
        /**
         * Callback for when the collection's tally changes
         */
        onTallyChange: function() {
         this._updateCount();
         this._updateGraph();
        },
        
        /**
         * Callback for when the collection's list changes
         */
        onListChange: function() {
          this._updateCount();
          this._updateCount();
        },
        
        /**
         * Callback for when the Summary's applications tally changes.
         */
        onApplicationsTallyChange: function() {
          if (this.collectionType === 'Application') {
            this._updateCount();
            this._updateGraph();  
          }
        },
        
        /**
         * Callback for when the Summary's clusters tally changes.
         */
        onClustersTallyChange: function() {
          if (this.collectionType === 'Cluster') {
            this._updateCount();
            this._updateGraph();  
          }
        },
        
        /**
         * Callback for when the Summary's servers tally changes.
         */
        onServersTallyChange: function() {
          if (this.collectionType === 'Server') {
            this._updateCount();
            this._updateGraph();  
          }
        },
        
        /**
         * Callback for when the Summary's runtimes tally changes.
         */
        onRuntimesTallyChange: function() {
          if (this.collectionType === 'Runtime') {
            this._updateCount();
            this._updateGraph();  
          }
        },
        
        /**
         * Callback for when the Summary's hosts tally changes.
         */
        onHostsTallyChange: function() {
          if (this.collectionType === 'Host') {
            this._updateCount();
            this._updateGraph();  
          }
        },
        
        onUnknown: function() {
          this.onClick("Alert");
        },
        onRunning: function() {
          this.onClick("STARTED");
        },
        onStopped: function() {
          this.onClick("STOPPED");
        },

        _onKey: function(/*Event*/ e){
            // summary:
            //      Handler for when user hits a key
            // tags:
            //      private
            if (e.keyCode === keys.ENTER || e.keyCode === keys.SPACE) {
                // route it to the onclick method
                this.onClick("Total");
            }
        },

        _onClick: function(/*Event*/ e){
            // summary:
            //      Handler for when user clicks
            // tags:
            //      private
            // route it to the onclick method
            return this.onClick("Total");
        }
    });
    return DashboardPane;

});