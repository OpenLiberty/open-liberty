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
define([ "js/common/platform", 'dojo/_base/lang', "dojo/_base/declare", 'dijit/registry', "jsShared/breadcrumb/BreadcrumbPane",
         "jsExplore/widgets/ExploreBreadcrumbButton", 'dijit/form/Button', 'jsExplore/views/viewFactory', 'jsExplore/resources/utils',
         'dojo/Deferred', 'dojo/i18n!../nls/explorerMessages'],
    function(platform, lang, declare, registry, BreadcrumbPane, ExploreBreadcrumbButton, Button, viewFactory, utils, Deferred, i18n) {

  var ExlporeBreadcrumbPane = declare("ExlporeBreadcrumbPane", [ BreadcrumbPane ],
      {
    constructor : function(params) {
      this.textDir = utils.getBidiTextDirectionSetting();
    },

    dashboardButton : null,

    postCreate : function() {
      /* Set the initial BreadcrumbPane with the dashboard button already existing */
      this.addDashboardButton();
    },

    setBreadcrumb : function(resource) {
      var type = resource.type;
      if (!(type.indexOf('appInstances') !== -1 || type.indexOf('appsOn') !== -1 || type.indexOf('serversOn') !== -1 || type
          .indexOf('runtimesOn') !== -1)) {
        /* The above types are views accessed by clicking a side tab button - do nothing, leave breadcrumb the same */

        this.resetBreadcrumbPane();
        this.resource = resource;
        switch (type) {
          case 'appOnServer':
            this.addAppInstBreadcrumb();
            break;
          case 'server':
            this.addServerBreadcrumb(this.resource, this.resource, true);
            break;
          case 'standaloneServer':
            this.addStandaloneServerBreadcrumb(this.resource);
            break;
          case 'runtime':
            this.addRuntimeBreadcrumb();
            break;
          case 'appOnCluster':
            this.addAppOnClusterBreadcrumb();
            break;
          default:
            this.addChild(new ExploreBreadcrumbButton([ this.resource, type, this.resource.name ]));
            this.__addSeparators();

        }

      }
    },
    
    setSearchBreadCrumb : function(showResultsButton){
      this.resetBreadcrumbPane(true); 
      this.addChild(new ExploreBreadcrumbButton([ null, "search", i18n.SEARCH]));
      if(showResultsButton){
        this.addChild(new ExploreBreadcrumbButton([ null, "searchResults", i18n.SEARCH_RESULTS]));       
      }
      this.__addSeparators();
      
      
    },

    addDashboardButton : function(onSearchPane) {
      // Do not add Dashboard Button if the resource is the standalone server.
      if (!utils.isStandalone()) {
        this.resource = registry.byId('mainDashboard');
        if(onSearchPane)
          this.dashboardButton = new ExploreBreadcrumbButton([ this.resource, "mainDashboard-search" ]);
        else
          this.dashboardButton = new ExploreBreadcrumbButton([ this.resource, "mainDashboard" ]);
        this.dashboardButton.set("displayed", true);
        this.dashboardButton.pane = this; // GRAPH_REDRAW_CHANGE Bind the pane into the dashboardButton button //<prhodes> confirm
        // this is
        // needed for breadcrumb
        this.addChild(this.dashboardButton);
      }
    },

    addRuntimeBreadcrumb : function() {
      this.addChild(new ExploreBreadcrumbButton([ this.resource.host, "host", this.resource.host.name ]));
      this.addChild(new ExploreBreadcrumbButton([ this.resource, "runtime", this.resource.path ]));
      this.__addSeparators();
    },

    addServerBreadcrumb : function(resource, server, isServerBreadcrumb) {
      var deferred = new Deferred();
      var me = this;
      this.__getResource("host", server.host, server).then(function(host) {
        if (!registry.byId('breadcrumbController-objectView-' + server.host.replace(/ /g,"_"))) {
          me.addChild(new ExploreBreadcrumbButton([ host, "host", server.host ]));
        }

        me.__getResource("runtime", host, server).then(function(runtime) {
          me.addChild(new ExploreBreadcrumbButton([ runtime, "runtime", runtime.name ]));
          me.addChild(new ExploreBreadcrumbButton([ server, "server", server.name, server.id ]));
          
          if(isServerBreadcrumb){
              me.__addSeparators();
          }
          deferred.resolve(true);
        });
        
      });
      return deferred;
    },

    addStandaloneServerBreadcrumb : function(resource) {
      this.addChild(new ExploreBreadcrumbButton([ resource, "server", resource.name, resource.id ]));
    },
    
    addAppInstBreadcrumb : function() {
      var me = this;
      if (!utils.isStandalone()) {
        this.addServerBreadcrumb(this.resource, this.resource.server).then(function(value) {        
          me.addChild(new ExploreBreadcrumbButton([ me.resource, "appOnServer", me.resource.name ]));
          me.__addSeparators();
        });        
      } else {
        this.addStandaloneServerBreadcrumb(this.resource.server);
        this.addChild(new ExploreBreadcrumbButton([ me.resource, "appOnServer", me.resource.name ]));
        this.__addSeparators();
      }
      
    },

    addAppOnClusterBreadcrumb : function() {
      console.log(this.resource);
      this.addChild(new ExploreBreadcrumbButton([ this.resource.cluster, "cluster", this.resource.cluster.name ]));

      this.addChild(new ExploreBreadcrumbButton([ this.resource, "appOnCluster", this.resource.name, null, null ]));
      this.__addSeparators();
    },

    /**
     * Performs clean up of the breadcrumb pane.
     */
    resetBreadcrumbPane : function(onSearchView) {
      /**
       * 2 options 1. use this widget's destroyDescendants that removes and destroys. drawback, will have to add back the dashboard
       * button each time. 2. use remove and then destroy each. drawback, will need to recursively do this for longer breadcrumbs...
       * could be slower performance than the widget's function in #1.
       */

      this.destroyDescendants();
      this.addDashboardButton(onSearchView);
    },

    __getResource : function(type, host, server) {
      switch (type) {
        case 'host':
          var host = server.getHost(host);
          return host;
          break;
        case 'runtime':
          var runtime = host.getRuntimeForServer(server);
          return runtime;
          break;
        default:
          console.error('viewFactory.openView called for an unknown resource type: ' + type);
      }

    },

    __addSeparators : function() {
      var buttons = this.getChildren();
      var numButtons = buttons.length;
      
      if (!utils.isStandalone()) {       
        if (numButtons == 2) {
          buttons[1].set("class", "lastBreadCrumb");
        }

        if (numButtons > 2) {
          for (var i = 2; i < numButtons; i++) {
            var currentIndex = this.getIndexOfChild(buttons[i]);
            var sepButton = new ExploreBreadcrumbButton([ this.resource, "separator" + i ]);
            this.addChild(sepButton, currentIndex);

            if (i == (numButtons - 1)) { //last, selected button                
              sepButton.set("iconClass", "breadcrumbSeparatorSelected");
              buttons[i].set("class", "lastBreadCrumb");
            }
          }
        }     
      } else if (numButtons > 1) {
        // Standalone severs do not have the dashboard breadcrumb button, so 
        // separators are needed if there are more than 1 breadcrumb (instead
        // of more than 2 as in the else statement above)
        for (var i = 1; i < numButtons; i++) {
          var currentIndex = this.getIndexOfChild(buttons[i]);
          var sepButton = new ExploreBreadcrumbButton([ this.resource, "separator" + i ]);
          this.addChild(sepButton, currentIndex);
          
          if (i == (numButtons-1)) {
            sepButton.set("iconClass", "breadcrumbSeparatorSelected");
            buttons[i].set("class", "lastBreadCrumb");
          }
        }
      }
    }
    
    });
  return ExlporeBreadcrumbPane;

});