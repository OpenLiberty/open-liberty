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
define([ 'dojo/parser', 'dijit/registry', 'dijit/layout/ContentPane', 'dojo/i18n!jsExplore/nls/explorerMessages', 'dojo/query',
         /*'js/common/platform',*/ 'jsExplore/resources/utils', 'jsExplore/views/collectionView', 'jsExplore/views/objectView', 
         'jsExplore/views/viewFactory', 'jsExplore/views/searchView', 'jsExplore/widgets/DashboardPane', 
         'jsExplore/widgets/AlertPane', 'jsExplore/resources/resourceManager', 'jsExplore/resources/_notifications/changeDetection', 'dojo/topic', 'jsExplore/resources/hashToView', 
         'jsExplore/utils/ID', 'jsExplore/resources/viewToHash', 'dojo/hash', 'jsExplore/resources/hashUtils', 'dijit/form/Button', 
         // disable deviceTheme for now as it breaks monitor menu 'dojox/mobile/deviceTheme', 
         'jsExplore/widgets/ExploreBreadcrumbPane', 'dojo/dom', 'jsExplore/utils/serverConfig', 'jsShared/utils/imgUtils', 
         'dojo/NodeList-manipulate', 'dojo/domReady!', 
         'jsExplore/layouts/BreadcrumbMRUController' /* Not used directly, but required to be loaded */
         ], function(parser, registry, ContentPane, i18n, query,
             /*platform,*/ utils, collectionView, objectView, 
             viewFactory, searchView, DashboardPane, 
             AlertPane, resourceManager, changeDetection, topic, hashToView, 
             ID, viewToHash, hash, hashUtils, Button, //deviceTheme, 
             ExploreBreadcrumbPane, dom, serverConfig, imgUtils) {

  'use strict';
  parser.parse();

  /*
   * View of the main dashboard: appsButton, clustersButton, serversButton, hostsButton ID information: ids are set as such : view +
   * resource + widget; i.e. dashboardservericon if not resource specific then: view + widget; i.e. dashboardContentPane
   * 
   * If only one server then will go into standAlone server case - show singleServer view
   */

  return {
    initPage : function() {
      // I hate this hack, but I somehow screwed up load order
      viewFactory.collectionView = collectionView;
      viewFactory.objectView = objectView;
      viewFactory.searchView = searchView;

      // I'm disabling this check as we no longer do special rendering for phone/tablet,
      // and this slows down the rendering of explore since it has to wait and fetch an extra file.
//      if (platform.isDesktop()) {
//        query("head").append('<link rel="stylesheet" type="text/css" href="css/desktop.css">');
//      }

      var breadcrumbPane = new ExploreBreadcrumbPane( 
          {     
            id : ID.getBreadcrumbPane()
          });     
      var breadcrumbDiv = registry.byId('breadcrumbDiv');
      breadcrumbPane.placeAt(breadcrumbDiv);

      var mainDashboard = registry.byId(ID.getMainDashboard());

      // Start the ResourceManager and the change detection
      changeDetection.start();

      // Check if in StandAlone server case
      var isStandalone = utils.isStandalone();
      if (isStandalone) {
        createSingleServerView(mainDashboard);
      } else {
        // Note: Adding and removing the background color because it messes with the box-shadow for focus
        new Button({
          title: i18n.SEARCH,
          label: i18n.SEARCH,
          baseClass: '',
          iconClass: 'searchButtonIcon',
          showLabel: false,
          "style" : "background-color:#2C363B",
          postCreate : function(){
            this.iconNode.innerHTML = imgUtils.getSVG('search');
          },
          onFocus : function(){
            this.set("style", "background-color:none");
          },
          onBlur : function(){
            this.set("style", "background-color:#2C363B");
          },      
          onClick: function() {
            // Before anything, check if we're navigating away from config view with a dirty/altered config
            // Wait until the dirtyConfig dialog has been resolved before continuing.
            serverConfig.waitIfDirtyEditor().then(function() {
              viewFactory.openSearchView();
              viewToHash.updateHash(viewToHash.getHash() + "/search");
            });
          }
        }, "searchButton");

        createCollectiveDashboard(mainDashboard);
      }

      // Upon initial load, check the hash to see if we need to go to a resource
      var currentHash = hashUtils.getCurrentHash();
      if (window.top.location.hash.indexOf('#' + hashUtils.getToolId()) !== 0) {
        // If the current hash doesn't have the toolId, add it.
        window.top.location.hash = '#' + hashUtils.getToolId();
      } else if (viewToHash.getHash() !== currentHash) {
        hashToView.updateView(currentHash);
        // Explicitly set lastUpdateHash to avoid double errorMessageDialogs when navigating to non-existent URL on initial load
        viewToHash.lastUpdateHash = currentHash;
      }

      // Subscribe to hash changes so we can deal with them appropriately.
      // TODO: Consider using this to be the sole call to viewFactory (all views opened from this)
      topic.subscribe("/dojo/hashchange", function(changedHash) {
        if (changedHash !== '' && changedHash !== viewToHash.lastUpdateHash) {
          hashToView.updateView(changedHash);
        }
      });
    }
  };

  function createSingleServerView(mainDashboard) {
    var searchDiv = dom.byId(ID.getSearchDiv());
    searchDiv.style.display="none";
    
    var searchButton = dom.byId(ID.getSearchButton());
    searchButton.innerHTML = i18n.SEARCH;
    
    resourceManager.getStandaloneServer().then(function(standaloneServer) {
      viewFactory.openView(standaloneServer);
    });
  }

  function createCollectiveDashboard(mainDashboard) {
    // Create the main dashboard pane
    var dashboardPane = new ContentPane({
      id : ID.dashDelimit(ID.getDashboard(), ID.getContentPaneUpper()),
      baseClass : 'dashboardPaneContent',
      content : ''
    }, 'dashboardPane');
    mainDashboard.addChild(dashboardPane);
    
    // Create the alert pane
    createAlertsPane(dashboardPane);

    // Order here matters, it dictates the layout
    appsOverview(dashboardPane);
    serversOverview(dashboardPane);
    clustersOverview(dashboardPane);
    hostsOverview(dashboardPane);
    runtimesOverview(dashboardPane);
  }

  /**
   * Create the alerts pane which is part of the dashboard. Nothing is initially displayed, so the pane is not visible.
   */
  function createAlertsPane(dashboardPane) {
    var alertPane = new AlertPane({
      id : ID.dashDelimit(ID.getDashboard(), ID.getAlertPaneUpper()),
      dashboardDisplay : true
    });
    dashboardPane.addChild(alertPane);
    
    resourceManager.getAlerts().then(function(alerts) {
      alertPane.addAlerts(alerts);
    });
  }

  /**
   * Do the common pane creation.
   */
  function buildCommonOverview(dashboardPane, type, name, getCollection) {
    // Build the initial pane (populate after we draw something quick)
    var pane = new DashboardPane([ type, name ]);
    dashboardPane.addChild(pane);

    // Populate the summary and then update the pane
    resourceManager.getSummary().then(function(summary) {
      pane.set('collection', summary);
    });

  // Even though we are using the summary to render the pane, we still need the right collection for the collection view
    getCollection.then(function(collection) {
      pane.onClick = function(filter) {
        viewFactory.openView(collection, filter);
      };
    });
  }

  // /////////////////
  // APPS OVERVIEW PANE
  function appsOverview(dashboardPane) {
    // Build, but do not populate
    var getApps = resourceManager.getApplications();

    buildCommonOverview(dashboardPane, "Application", i18n.APPLICATIONS, getApps);
  }

  // /////////////////
  // CLUSTER OVERVIEW PANE
  function clustersOverview(dashboardPane) {
    // Build, but do not populate
    var getClusters = resourceManager.getClusters();

    buildCommonOverview(dashboardPane, "Cluster", i18n.CLUSTERS, getClusters);
  }

  // /////////////////
  // SERVER OVERVIEW PANE
  function serversOverview(dashboardPane) {
    // Build, but do not populate
    var getServers = resourceManager.getServers();

    buildCommonOverview(dashboardPane, "Server", i18n.SERVERS, getServers);
  }

  // /////////////////
  // HOSTS OVERVIEW PANE
  function hostsOverview(dashboardPane) {
    // Build, but do not populate 
    var getHosts = resourceManager.getHosts();

    buildCommonOverview(dashboardPane, "Host", i18n.HOSTS, getHosts);
  }

  // /////////////////
  // RUNTIMES OVERVIEW PANE
  function runtimesOverview(dashboardPane) {
    // Build, but do not populate 
    var getRuntimes = resourceManager.getRuntimes();

    buildCommonOverview(dashboardPane, "Runtime", i18n.RUNTIMES, getRuntimes);
  }

});