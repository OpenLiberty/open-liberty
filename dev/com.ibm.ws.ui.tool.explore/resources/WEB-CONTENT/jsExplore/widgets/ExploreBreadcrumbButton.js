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
define([ "js/common/platform", 
         "dojo/_base/declare",
         "dojo/dom-class",
         "dojo/dom-attr",
         "dojo/on",
         'dijit/registry',
         "jsShared/breadcrumb/BreadcrumbButton", 
         "dijit/layout/ContentPane", 
         "dojo/i18n!jsExplore/nls/explorerMessages", 
         'jsExplore/views/viewFactory',
         "jsExplore/resources/viewToHash",
         "jsExplore/resources/utils",
         "jsExplore/utils/constants",
         "jsExplore/utils/serverConfig",
         "jsShared/utils/imgUtils"], 
		function(platform, 
				declare,
				domClass,
				domAttr,
				on,
				registry,
				BreadcrumbButton, 
				ContentPane, 
				i18n, 
				viewFactory,
				viewToHash,
				utils,
				constants,
				serverConfig,
				imgUtils) {

  var ExploreBreadcrumbButton = declare("ExploreBreadcrumbButton", [ BreadcrumbButton ], {
    constructor : function(params) { // resource, buttonViewType(Overview, Application, Server, etc.), title, id override, special label (with span)
      this.resource = params[0];
      this.buttonViewType = params[1];
      if (params[2]) {
        this.title = params[2];
        this.id = "breadcrumbController-objectView-"+this.title.replace(/ /g,"_");
      } else {
        this.id = "breadcrumbController-collectionView-" + this.buttonViewType.replace(/ /g,"_");
      }
      if (params[3]) {
        this.id = "breadcrumbController-objectView-" + params[3].replace(/ /g,"_");
      }
      if (params[4]) {
        this.labelSameAsTitle = false;
        this.specialLabel = params[4];
      }

      if (this.buttonViewType == 'mainDashboard' || this.buttonViewType == 'mainDashboard-search') {
        this.set("baseClass", "breadcrumbDashboard");
        // Note: Adding and removing the background color because it messes with the box-shadow for focus
        on(this, "focus", function(){
          if (!domClass.contains(this.iconNode,  "borderFocused")) {
              domClass.add(this.iconNode, "borderFocused");      
          }
        });       
        on(this, "blur", function(){
          if (domClass.contains(this.iconNode,  "borderFocused")) {
            domClass.remove(this.iconNode, "borderFocused");
          }
        });      
        if (this.buttonViewType == 'mainDashboard') {
          on(this, "click", closeIframe); // Inline function in index.jsp
        }
        this.id = "breadcrumbController-mainDashboard";
      } else if (this.buttonViewType == 'search'||this.buttonViewType == 'searchResults') {
        this.set("baseClass", "breadcrumbTextOnly");
        on(this, "focus", function(){
            if (!domClass.contains(this.domNode,  "borderFocused"))
              domClass.add(this.domNode, "borderFocused");         
        });       
        on(this, "blur", function(){
          if (domClass.contains(this.domNode,  "borderFocused"))
            domClass.remove(this.domNode, "borderFocused");
        });
      } else if (this.buttonViewType.indexOf('separator') !== -1) {
        this.set("baseClass", "breadcrumbSeparatorButton");  
      } else {
        this.set("baseClass", platform.getDeviceCSSPrefix() + "breadcrumb");
        on(this, "focus", function(){
            if (!domClass.contains(this.domNode,  "borderFocused"))
              domClass.add(this.domNode, "borderFocused");         
        });       
        on(this, "blur", function(){
          if (domClass.contains(this.domNode,  "borderFocused"))
            domClass.remove(this.domNode, "borderFocused");
        });              
      }
      this.value = this.id;
    },

    postCreate : function() {
      /*Set the title and label for all of the object views. 
       * The collection views will overwrite this with a generic value*/
      if (this.labelSameAsTitle && utils.getStringTextDirection(this.title) === "rtl") {
        this.title = "\u202B" + this.title + "\u202C";
      }
      this.set("title", this.title);
      if (this.labelSameAsTitle) {
        this.set("label", this.title);
      } else {
        this.set("label", this.specialLabel);
      }

      var type = this.buttonViewType;
      /*the separator IDs consist of 'separator' and a #. This will */
      if (type.indexOf('separator') !== -1) {
        type = 'separator';
      }
      console.log(type);
      switch (type) {
      /**Breadcrumb buttons to show specific resource name. Titles and labels will not be constants  */
      case 'host':
        this.set("iconClass", "breadcrumbHosts");
        this.iconNode.innerHTML = imgUtils.getSVGSmall('host-white', null, i18n.HOST, true);
        break;
      case 'runtime':
        this.set("title", this.resource.name);
        this.set("label", this.resource.name);
        var runtimeIcon = 'runtime-white';
        switch (this.resource.runtimeType) {
          case constants.RUNTIME_LIBERTY:
            this.set("iconClass", "breadcrumbRuntimes breadcrumbLiberty");
            runtimeIcon = 'runtimeLiberty-white'; //TODO: set to liberty runtime SVG icon when it exists
            break;
          
          case constants.RUNTIME_NODEJS:
            this.set("iconClass", "breadcrumbRuntimes breadcrumbNodejs");
            runtimeIcon = 'node-hex';
            break;
            
          default:
            this.set("iconClass", "breadcrumbRuntimes");
            this.set("title", i18n.RUNTIME);
            this.set("label", i18n.RUNTIME);
            runtimeIcon = 'runtime-white';
            console.error('Runtime breadcrumb button for an unknown runtime resource type: ' + this.resource.runtimeType);
        }
        this.iconNode.innerHTML = imgUtils.getSVGSmall(runtimeIcon);
        break;
      case 'cluster':
        this.set("iconClass", "breadcrumbClusters");
        this.iconNode.innerHTML = imgUtils.getSVGSmall('cluster-white', null, i18n.CLUSTER, true);
        break;
      case 'appOnCluster':
        this.set("iconClass", "breadcrumbApps");
        this.iconNode.innerHTML = imgUtils.getSVGSmall('app-white', null, i18n.APPLICATION, true);
        break;
      case 'appOnServer':
        this.set("iconClass", "breadcrumbInstances");
        this.iconNode.innerHTML = imgUtils.getSVGSmall('app-white', null, i18n.APPLICATION, true);
        break;
      case 'server':
        this.set("iconClass", "breadcrumbServers");
        if(this.resource.isCollectiveController) {
          this.iconNode.innerHTML = imgUtils.getSVGSmall('collectiveController-white', null, i18n.COLLECTIVE_CONTROLLER_DESCRIPTOR, true);
        } else {
          this.iconNode.innerHTML = imgUtils.getSVGSmall('server-white', null, i18n.SERVER, true);
        }
        break;
      case 'applications':
        this.set("title", i18n.APPLICATIONS);
        this.set("label", i18n.APPLICATIONS);
        this.set("iconClass", "breadcrumbApps");
        this.iconNode.innerHTML = imgUtils.getSVGSmall('app-white');
        break;
      case 'clusters':
        this.set("title", i18n.CLUSTERS);
        this.set("label", i18n.CLUSTERS);
        this.set("iconClass", "breadcrumbClusters");
        this.iconNode.innerHTML = imgUtils.getSVGSmall('cluster-white');
        break;
      case 'hosts':
        this.set("title", i18n.HOSTS);
        this.set("label", i18n.HOSTS);
        this.set("iconClass", "breadcrumbHosts");
        this.iconNode.innerHTML = imgUtils.getSVGSmall('host-white');
        break;
      case 'runtimesOnHost': //this may need to be move to the "do nothing" section; will need to revisit once runtimes are showing
      case 'runtimes':
        this.set("title", i18n.RUNTIMES);
        this.set("label", i18n.RUNTIMES);
        this.set("iconClass", "breadcrumbRuntimes");
        this.iconNode.innerHTML = imgUtils.getSVGSmall('runtime-white');
        break;
      case 'servers':
        this.set("title", i18n.SERVERS);
        this.set("label", i18n.SERVERS);
        this.set("iconClass", "breadcrumbServers");
        this.iconNode.innerHTML = imgUtils.getSVGSmall('server-white');
        break;

      /**Views accessed by clicking a side tab button - do nothing, leave breadcrumb the same **/
      case 'appsOnCluster':
      case 'appsOnServer':
      case 'serversOnCluster':
      case 'serversOnHost':
      case 'serversOnRuntime':
      case 'appInstancesByCluster':
      case 'search':
      case 'searchResults':
        break;
      case 'separator':
        this.set("iconClass", "breadcrumbSeparator");
        this.set("tabIndex", -2);
        break;
      default:
        // assume dashboard
        this.set("title", i18n.DASHBOARD);
        this.set("iconClass", "breadcrumbDashboard");
        this.set("aria-label", i18n.DASHBOARD);
        // Note: Adding and removing the background color because it messes with the box-shadow for focus
        this.set("style", "background-color:#2C363B");
        this.iconNode.innerHTML = imgUtils.getSVGSmall('dashboard');
      }

    },

    startup : function() {
      this.inherited(arguments);
      // need to set specifically on the label because setting textDir on the button itself mirrors the icon as well
      if (this.labelSameAsTitle) {
        domAttr.set(this.id + "_label", "dir", utils.getStringTextDirection(this.get("title")));
      }
    },

    onClick : function() {
      // Before anything, check if we're navigating away from config view with a dirty/altered config
      // Wait until the dirtyConfig dialog has been resolved before continuing.
      //TODO: currently separators are clickable and will go to default case. they should not be clickable
      var me = this;
      serverConfig.waitIfDirtyEditor().then(function() {
        /**look up and create resource for host, runtime, or server **/
        var type = me.buttonViewType;
        switch (type) {
        case 'mainDashboard':
        case 'mainDashboard-search':
          registry.byId('breadcrumbContainer-id').selectChild("mainDashboard");
          var breadcrumbStackContainer = registry.byId("breadcrumbStackContainer-id");
          var breadcrumbAndSearchPane = registry.byId("breadcrumbAndSearchDiv");
          if (breadcrumbStackContainer  && breadcrumbAndSearchPane) {
            breadcrumbStackContainer.selectChild(breadcrumbAndSearchPane);
          }
          break;
        case 'applications':
        case 'appsOnCluster':
        case 'appsOnServer':
        case 'appInstances':
        case 'clusters':
        case 'hosts':
        case 'runtimesOnHost':
        case 'servers':
        case 'serversOnCluster':
        case 'serversOnHost':
        case 'serversOnRuntime':
        case 'appInstancesByCluster':
        case 'search':
        case 'searchResults':  
          break;
        case 'appOnCluster':
        case 'appOnServer':
        case 'cluster':
        case 'host':
        case 'runtime':
        case 'server':
        case 'standaloneServer':
          viewFactory.openView(me.resource);
          break;
        default:
          console.error('viewFactory.openView called for an unknown resource type: ' + type);
        }
      });
    }
  });
  return ExploreBreadcrumbButton;

});