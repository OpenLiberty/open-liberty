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
define([ 'dojo/Deferred', 
         "js/common/platform", 
         "dojo/_base/declare", 
         "dijit/form/Button", 
         "dijit/layout/ContentPane", 
         "dijit/registry",
         "dojo/i18n!jsExplore/nls/explorerMessages", 
         "jsExplore/resources/viewToHash", 
//         "jsExplore/resources/stats/_logAnalyticsUtils",
         "dojo/text!jsExplore/widgets/templates/SideTabButton.html", 
         'jsExplore/widgets/graphs/GraphContainer', 
         'jsExplore/widgets/graphs/PerspectiveContainer', 
         'dojo/dom-construct', 
         'jsExplore/resources/utils', 
         'jsExplore/utils/featureDetector',
         'jsExplore/utils/ID',
         'dojo/_base/lang', 
         'dojo/dom', 
         'jsExplore/utils/serverConfig',
         'jsShared/utils/imgUtils' ],
         function(Deferred, 
             platform, 
             declare, 
             Button, 
             ContentPane, 
             registry, 
             i18n, 
             viewToHash, 
//             logAnalyticsUtils,
             template, 
             GraphContainer, 
             PerspectiveContainer,
             domConstruct, 
             utils, 
             featureDetector, 
             ID,
             lang, 
             dom, 
             serverConfig,
             imgUtils) {

  var SideTabButton = declare("SideTabButton", [ Button ], {
    constructor : function(configObj) { // resource, buttonViewType(Overview, Application, Server), populateTabPane
      this.resource = configObj.resource;
      this.buttonViewType = configObj.type;
      this.populateTabPane = configObj.populateFunction;
      this.id = this.resource.id + "SideTab" + this.buttonViewType + "Button";
      
      // Construct the icon class based on the buttonViewType except for Apps.
      // If Apps is passed in, determine whether it is appsOnServer on appsOnCluster
      // by inspecting the resource.type and then set the Apps icon accordingly.
      this.baseIconClass = 'sideTab' + this.buttonViewType;
      if (this.buttonViewType === "Apps") {
        if (this.resource.type === "cluster") {
          this.baseIconClass = 'sideTab' + "AppsOnCluster";
        } else if (this.resource.type === "server") {
          this.baseIconClass = 'sideTab' + "Instances";
        }
      }
      
      this.set("baseClass", platform.getDeviceCSSPrefix() + "sideTabButton");
      console.debug('buttonID: ', this.id);
    },

    id : '',
    baseClass : platform.getDeviceCSSPrefix() + "sideTabButtonOne",
    baseIconClass: '', 
    iconClass : '', // set based on buttonViewType
    title : '', // set based on buttonViewType
    "aria-label" : '', // set same as title

    view : 'single',
    resource : null,
    buttonViewType : '', // "Overview", "Servers", "Apps", "Instances", "Runtimes", "Stats", "Config"
    buttonPanes : null,
    populateTabPane : null,
    displayed : false,
    templateString: template,
    filter: null,

    postCreate : function() {
      this.set('class', 'sideTabLabel');
      this.set("iconClass", this.baseIconClass);
      if (this.buttonViewType === "Apps") {
        this.set("title", i18n.APPLICATIONS);
        this.set("label", i18n.APPLICATIONS);
      } else if (this.buttonViewType === "Instances") {
        this.set("title", i18n.INSTANCES);
        this.set("label", i18n.INSTANCES);
      } else if (this.buttonViewType === "Servers") {
        this.set("title", i18n.SERVERS);
        this.set("label", i18n.SERVERS);
      } else if (this.buttonViewType === "Runtimes") {
        this.set("title", i18n.RUNTIMES);
        this.set("label", i18n.RUNTIMES);
      } else if (this.buttonViewType === "Stats") {
        this.set("title", i18n.STATS);
        this.set("label", i18n.STATS);
      } else if (this.buttonViewType === "Config") {
        this.set("title", i18n.CONFIGURE);
        this.set("label", i18n.CONFIGURE);
      } else {
        // assume overview
        this.set("title", i18n.OVERVIEW);
        this.set("label", i18n.OVERVIEW);
      }
      this.buttonPanes = new Array();
	  var icon = this._getIcon(this.buttonViewType);
      this.iconNode.innerHTML = imgUtils.getSVG(this._getIcon(this.buttonViewType));

    },

    onClick : function(hash) {
      // Before anything, check if we're navigating away from config view with a dirty/altered config
      // Wait until the dirtyConfig dialog has been resolved before continuing.
      var me = this;
      serverConfig.waitIfDirtyEditor().then(function() {
        hash = hash && (typeof hash === 'string') ? hash : '';
        if (me.get("displayed") && me.buttonViewType !== "Overview" && hash.indexOf('/serverConfig') !== 0) {
          return;
        }
        // enable icon
        me._setDisplayedAttr(true);
        // tell parent so it can disable the other buttons
        me.getParent().buttonClicked(me);
  
        // now figure out what to display
        // TODO: Change how we pass these things around. Looking it up by ID is very brittle
        var viewFinder = me.resource.type;
        if (viewFinder === 'Instance') {
          viewFinder = 'Application';
        }
        // TODO: Change how we pass these things around. Looking it up by ID is very brittle
        var mainStackContainer = registry.byId(me.resource.id + "-StackContainer");
        if (me.buttonViewType === "Overview") {
          // TODO: Change how we pass these things around. Looking it up by ID is very brittle
          viewToHash.updateHash(me.resource);
          mainStackContainer.selectChild(me.resource.id + "-MainContentPane");
          // GRAPH_REDRAW_CHANGE If we have a pane and a pane chart, re-render it!
          console.log('Overview clicked');
//          if (me.pane && me.pane.chart) {
//            me.pane.chart.fullRender();
//            me.pane.chart.resize('100%', 30);
//          }
        } else {
          // TODO: Change how we pass these things around. Looking it up by ID is very brittle
          var paneId = me.resource.id + me.buttonViewType + "-ContentPane";
          if (me.buttonPanes[me.buttonViewType]) {
            paneId = me.buttonPanes[me.buttonViewType].paneId;
          }
          var existingPane = registry.byId(paneId);
          if (existingPane && me.buttonViewType !== "Config") {
            // If this pane already exists, update the hash. Considered moving this logic to the top, but that would cause it to fire twice on
            // creation: first for the topic and second for this
            console.debug(me.resource);
            if (me.buttonViewType === "Apps") {
              me.resource.getApps().then(function(appsOnSomething) {
                viewToHash.updateHash(appsOnSomething);
              });
            } else if (me.buttonViewType === "Servers") {
              me.resource.getServers().then(function(serversOnSomething) {
                viewToHash.updateHash(serversOnSomething);
              });
            } else if (me.buttonViewType === "Instances") {
              me.resource.getInstances().then(function(appInstances) {
                viewToHash.updateHash(appInstances);
              });
            } else if (me.buttonViewType === "Runtimes") {
              me.resource.getRuntimes().then(function(runtimes) {
                viewToHash.updateHash(runtimes);
              });
            } else if (me.buttonViewType === "Stats") {
              viewToHash.updateHash(me.resource, "Stats");
            } else if (me.buttonViewType === "Config") {
              viewToHash.updateHash(me.resource, "Config");
            }
            mainStackContainer.selectChild(paneId);
            // if there is a filter, then need to apply it
            if (me.filter) {
              var filterBar = registry.byId(existingPane.containerNode.children[0].id);
              if (filterBar) {
                filterBar.set('currentFilter', me.filter);
              }
            }
          } else {
            // need to construct it
            var pane = null;
            // The code below should be moved to a new place so that we have common code around opening tool iFrames within Explore
            var configHash = '';
            if (me.buttonViewType === "Stats") {
//              if (me.resource.type === "server" && logAnalyticsUtils.isLogAnalyticsEnabled(me.resource)) {
//                pane = new PerspectiveContainer({resource: me.resource});
//              } else {
                // if analytics is not enabled, do it the old way
                pane = new GraphContainer({resource: me.resource, perspective: ""});
//              }
            } else if (me.buttonViewType === "Config") {
              if (existingPane) {
                mainStackContainer.removeChild(registry.byId(paneId));
                existingPane.destroy();
              }
              if (hash.indexOf('/serverConfig') === 0) {
                configHash = hash.substring(('/serverConfig').length);
              }
              var url;
              var serverConfigUrl;
              if (window.top.location.pathname.indexOf('/devAdminCenter/') > -1 || window.top.location.pathname.indexOf('/devExplore/') > -1) {
                // if this is devAdminCenter or devExplore, open the devServerConfig
                serverConfigUrl = '/devServerConfig/#serverConfig';
              } else {
                serverConfigUrl = '/ibm/adminCenter/serverConfig-1.0/#serverConfig';
              }
              if (utils.isStandalone()){
                url = window.top.location.origin + serverConfigUrl + configHash;
              } else {
                url = window.top.location.origin + serverConfigUrl + '/' + me.resource.id + configHash;
              }
              
              console.debug('Opening serverConfig with URL: ' + url);
              if (featureDetector.isConfigAvailable()){
                // TODO: Move iFrame creation to a common place
                var configIFrame = domConstruct.create('iframe', {
                  id : 'exploreContainerForConfigTool',
                  title : i18n.CONFIGURE,
                  style : 'border: none; overflow: auto;',
                  width : '100%',
                  height : '100%',
                  "class": "configIFrameHideScrollbar",
                  src : url
                });
                pane = new ContentPane({
                  id : paneId,
                  label : i18n.CONFIGURE,
                  region : "top",
                  style : 'overflow:hidden',
                  content : configIFrame
                });
              } else {
                pane = new ContentPane({
                  content: '<div class="configUnavailableMessageContainer"><div class="configUnavailableMessage"><div class="configUnavailableMessageIcon"></div><div class="configUnavailableMessageLabel">' + i18n.CONFIG_NOT_AVAILABLE + '</div></div></div>'
                });
              }
            } else {
              pane = new ContentPane({
                id : paneId,
                label : me.resource.name,
                region : "top",
                style : 'overflow:auto',
                content : ''
              });
            }
            mainStackContainer.addChild(pane);
            mainStackContainer.selectChild(pane);
            if (me.populateTabPane) {
              me.populateTabPane(pane, me.resource.name, me.resource, me.filter);
            }
            me.buttonPanes[me.buttonViewType] = new Object();
            me.buttonPanes[me.buttonViewType].paneId = pane.id;
            // In objectView.populateServerSideTabPane(), there's no calling of viewFactory.openView() 
            // for statistics; it's not a resource, and it's certainly not available on the Server object. 
            // viewFactory.openView() is where the calling of viewToHash.updateHash() would occur.
            // So for now, do it here (feels a bit hackish, though)
            if (me.buttonViewType === "Stats") {
              viewToHash.updateHash(me.resource, "Stats");
            } 
            else if (me.buttonViewType === "Config" && configHash === '') {
              viewToHash.updateHash(me.resource, "Config");
            }
          }
        }
      });
    },

    // set iconclass when content is displayed/not displayed
    _setDisplayedAttr : function(value) {
      this.displayed = value;
      if (value) {
        this.set('iconClass', this.baseIconClass + 'Selected');
        this.set('class', 'sideTabSelectedLabel');
        this.iconNode.innerHTML = imgUtils.getSVG(imgUtils.normalizeName(this.buttonViewType) + '-selected');
      } else {
        this.set('iconClass', this.baseIconClass);
        this.set('class', 'sideTabLabel');
        var icon = this._getIcon(this.buttonViewType);
        this.iconNode.innerHTML = imgUtils.getSVG(this._getIcon(this.buttonViewType));
      }
    },
    
    _getIcon : function(icon) {
    	var newIcon = imgUtils.normalizeName(icon);
	  	switch (newIcon) {
	  		case 'app':
	  		case 'instance':
	  		case 'server':
	  		case 'runtime':
	  		case 'appOnServer':
	  			newIcon = newIcon + '-nav';
	  			break;
	  		default:
		  		break;
	  	}
	  	return newIcon;
    }

  });
  return SideTabButton;

});