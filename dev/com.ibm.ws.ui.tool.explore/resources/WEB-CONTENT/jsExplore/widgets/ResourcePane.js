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
        "jsExplore/widgets/DashboardPane",
        "dojo/_base/declare",
        "dojo/_base/lang",
        "dijit/registry",
        "dijit/layout/ContentPane",
        "dijit/_TemplatedMixin",
        "dijit/_WidgetsInTemplateMixin",
        "dojo/text!./templates/ResourcePane.html",
        "dojo/i18n!jsExplore/nls/explorerMessages",
        "jsShared/utils/imgUtils"
        ], function(
                DashboardPane,
                declare,
                lang,
                registry,
                ContentPane,
                _TemplatedMixin,
                _WidgetsInTemplateMixin,
                template,
                i18n,
                imgUtils
            ){

	var viewPrefix = "objectView-";
	function __getId(params) {
        var id = viewPrefix + params[0] + params[3] + "Pane" + params[1]; //id : objectView-resourceTypelabelPaneresourceName
        return id;
    }
	  /**
     * Construct a ResourcePane widget.
     */
    return {
        /**
         * 
         * @param   resourceType    Resource Type of the resource associated with this ResourcePane
         * @param   resourceName    Name of the resource associated with this ResourcePane
         * @param   collection      Collection object to gather state info and subscribe for updates
         * @param   label           Translated label for pane ("Instances", "Servers", "Applications")
         * @param   paneType        Resource Type that this ResourcePane will display info about
         * @param   paneClass       Mostly "bottomContentPane" when there is only one ResourcePane on the
         *                          page...."middleContentPane" if more than one like for Clusters
         * @param   region          Mostly "center"...."top" for top ResourcePane when there is more than 
         *                          one ResourcePane on the page like for Clusters.
         * @param   sideTab         object
         *       
         * Example: "Cluster", "defaultCluster", serversObj, i18n.SERVERS, "Server", "middleContentPane", "top", sideTabPane
         */
        createResourcePane: function(params) {
        	var id = __getId(params);
          if (registry.byId(id)) {
            return registry.byId(id);
          } else {
           	var ResourcePane = declare("ResourcePane", [ DashboardPane, _TemplatedMixin, _WidgetsInTemplateMixin], {
              constructor : function(params) {     //resourceType, resourceName, collection, label, paneType, paneClass, region, sideTab
                this.view = viewPrefix;
                this.resourceType = params[0];
                this.resourceName = params[1];
                this.collection = params[2]; // Hack!
                this.collection.subscribe(this);
                this.resourceLabel = params[3];
                this.paneType = params[4];

                if (params[5]) {
                  this.resourcePaneClass = params[5];
                }
                if (params[6]) {
                  this.region = params[6];
                }
                if (params[7]) {
                  this.sideTabPane = params[7];
                }

                this.id = __getId(params);
                // TODO: Set these ids in ID.js
                this.resourceIconId = this.view + this.resourceName + "-" + this.resourceType + this.paneType + "-Icon";  
                this.resourceId = this.view + this.resourceName + "-" + this.resourceType + this.paneType + "-Count";
                this.resourceCountId = this.resourceId + "Number";
                this.resourceStatePaneId = this.view + this.resourceType + this.paneType + "SubLabelStatePane" + this.resourceName;
                this.resourceStateUnknownNumber = this.view + this.resourceName + "-" + this.resourceType + this.paneType + "-Unknown-Count" + "-StateNumber";
                this.resourceStateRunningNumber = this.view + this.resourceName + "-" + this.resourceType + this.paneType + "-Running-Count" + "-StateNumber";
                this.resourceStateStoppedNumber = this.view + this.resourceName + "-" + this.resourceType + this.paneType + "-Stopped-Count" + "-StateNumber";
                
                this.resourceNumber = this.collection.up + (this.collection.partial ? this.collection.partial : 0 ) + this.collection.down + (this.collection.unknown ? this.collection.unknown : 0);
           
                // initialize the labels for the states (like "Running" vs. "Started")
                this._setStateLabels();
                
             },
             
             paneType : '',
             resourceName : '',
             resourceNumber : 0,
             resourceLabel : '',
             resourceId : '',
             templateString : template,
             region : 'center',
             resourcePaneClass : 'bottomContentPane',
             resourceIconId : '',
             resourceStatePaneId : '',
             resourceStateUnknownNumber : '',
             resourceStateRunningNumber : '',
             resourceStateStoppedNumber : '',
             sideTabPane : null,
             
             postCreate : function() {
               this.inherited(arguments);
               
               this.iconNode.innerHTML = imgUtils.getSVG(this.paneType);
               this.runIcon.innerHTML = imgUtils.getSVG("status-running");
               this.stopIcon.innerHTML = imgUtils.getSVG("status-stopped");
               this.unknownIcon.innerHTML = imgUtils.getSVG("unknown");
               
               this._buildStates();
             },
             
             /*
              * Unsubscribe this observer resource 
              */
             destroy: function() {
               this.inherited(arguments);
               if (this.collection) {
                 this.collection.unsubscribe(this);   
               }
             }, 
                    
             startup : function() {
               this.inherited(arguments);

               this._buildStates();                     
             }, 
              
             _buildStates: function() {
               
               var unknown = this.__computeUnknown();
               if (unknown > 0) {
                 this.unknownPane.domNode.style.display = "block";
                 this.unknownNumberNode.innerHTML = unknown;
               } else {
                 this.unknownPane.domNode.style.display = 'none';
               }

               this.runningNumberNode.innerHTML = this.__computeRunning();
               this.stoppedNumberNode.innerHTML = this.__computeStopped();
             },
                    
             _updateGraph: function() {
               this._set("resourceNumber", this.collection.up + (this.collection.partial ? this.collection.partial : 0 ) + this.collection.down + (this.collection.unknown ? this.collection.unknown : 0));
               this.numberNode.textContent = this.resourceNumber;
               
               this._buildStates();
             },
                    
             onTallyChange: function() {
               this._updateGraph();
             },

             _setCollectionAttr : function() {
               // Override DashboardPane._setCollectionAttr as we do not want to take those actions in this class 
             },

             _setStateLabels : function() {
               this.stateLabels.up = i18n.RUNNING;
               this.stateLabels.partial = i18n.PARTIALLY_RUNNING;
               this.stateLabels.down = i18n.STOPPED;
               this.stateLabels.unknown = i18n.UNKNOWN;
             }
           	
           	});
            return new ResourcePane(params);
          }
   	    }
    };
});