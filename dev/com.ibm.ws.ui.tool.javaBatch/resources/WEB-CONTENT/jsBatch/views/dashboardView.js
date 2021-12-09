/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
define([ 'dojo/_base/lang', 'dojo/parser', 'dojo/json', 'dojo/topic', 'dojo/on',
         'dijit/registry', 
         'dijit/layout/ContentPane', 'dijit/layout/BorderContainer', 'dijit/layout/StackContainer',
         'dijit/form/Button',
         'jsBatch/widgets/search/JavaBatchSearchBox',        
         'jsBatch/widgets/JavaBatchInstanceGrid',
         'jsShared/utils/toolData',
         'jsShared/utils/userConfig',
         'dojo/Deferred',
         'jsBatch/utils/hashUtils',
         'jsBatch/utils/viewToHash',
         'jsShared/utils/imgUtils',
         'jsBatch/utils/utils',
         'jsShared/utils/ID',
         'jsBatch/utils/ID',
         'dojo/i18n!jsBatch/nls/javaBatchMessages',
         'dojox/widget/Standby',
         'dojo/domReady!' ], 
function(lang, parser, JSON, topic, on,
         registry, ContentPane, BorderContainer, StackContainer, Button,  
         JavaBatchSearchBox, JavaBatchInstanceGrid, toolData, userConfig, 
         Deferred, hashUtils, viewToHash, imgUtils, utils, ID, batchID, i18n, Standby) {

  'use strict';
  parser.parse();

  /*
   * View of the main landing page for the Java Batch Tool
   *  
   */

  // The panels that exist on the dashboard view
  var instanceGrid = null;
  var searchErrorPane = null;
  var searchBox = null;
  var loadingGridPane = null;

  return {
    viewId: batchID.DASHBOARD_VIEW,

    initPage: function() {
      var dashboardView = new ContentPane({
        id: batchID.DASHBOARD_VIEW,
        title: i18n.DASHBOARD_VIEW,
        content: ' ',
        baseClass: 'topDetailContentPane'
      });

      createDashboardView(dashboardView);
      return dashboardView;
    }, // end of initPage
    
    closeView : function() {
      
    },

    // Will update the dashboard view with new data
    // @param queryParams - parameters to update the search pills and URL query string
    // @param tableJson - data to update the grid.  If null, the loading animation is shown on the grid
    updateView : function(queryParams, tableJson) {
      var breadcrumbContainer = registry.byId(batchID.BREADCRUMB_CONTAINER);
      var dashboardView = registry.byId(this.viewId);
      var me = this;
      
      var updateBreadCrumbs = function(){
        breadcrumbContainer.getChildren().forEach(lang.hitch(me, function(child){
          if(child.id === me.viewId){
            breadcrumbContainer._showChild(child);
          } else {
            breadcrumbContainer._hideChild(child);
          }
          child.resize();
        }));

        if(!tableJson) {
          loadingGridPane.show();
        } else {
          loadingGridPane.hide();
          instanceGrid.__updateTable(tableJson);
        }

        if(queryParams) {
          searchBox.updateSearchPills(queryParams);
        }
        
        on(window, 'resize', function() { 
          dashboardView.resize();
        });
      };
      
      if (! dashboardView) {
        dashboardView = this.initPage();
        breadcrumbContainer.addChild(dashboardView);
        updateBreadCrumbs();
      } else {
        updateBreadCrumbs();
      }
      
     
    }

  }; // end of return

  // Creates all the panels for the dashboard view
  function createDashboardView(dashboardView) {
   
    var borderContainer = new BorderContainer( {
      style: "height: 100%; width: 100%;"
    });
    
    var contentPaneTop = new ContentPane({
      region: "top",
      style: "height: 75px;",
      splitter: true
    });

    var searchPane = createSearchBox();
    contentPaneTop.addChild(searchPane);
    borderContainer.addChild(contentPaneTop);
    
    var contentPaneCenter = new ContentPane({
      id : batchID.CENTER_CONTENT_PANE,
      region: "center",
      splitter: true
    });

    instanceGrid = new JavaBatchInstanceGrid(dashboardView.persistedData);
    borderContainer.addChild(contentPaneCenter);
    instanceGrid.jobInstanceGrid.placeAt(contentPaneCenter);
    instanceGrid.jobInstanceGrid.startup();

    loadingGridPane = createLoadingAnimationPane();
    loadingGridPane.placeAt(contentPaneCenter, "first");

    searchErrorPane = createSearchErrorPane();
    borderContainer.addChild(searchErrorPane);
    
    borderContainer.placeAt(dashboardView);
    borderContainer.startup();
    borderContainer.resize();
  } // end of createDashboardView
  
  function createSearchBox() {
    if (registry.byId(ID.getSearchConditionDiv())) {
      registry.byId(ID.getSearchConditionDiv()).destroy();
    }
    
    // Legacy markup.  The search box needs to be attached to a search pane
    // for the logic that controls how many rows of pills are shown on screen
    // when adding lots of pills to the search query.
    var searchPane = new ContentPane({
      // FIXME: This ID seems wrong, but using it because of legacy code
      id : ID.getSearchConditionDiv(), 
      role : "search"
    });
    
    searchBox = new JavaBatchSearchBox();
    searchBox.startup();
    
    searchPane.addChild(searchBox);
    return searchPane;
  } // end of createSearchBox

  function createLoadingAnimationPane() {
    // Note 1 - Start
    // In order to keep the styling of Standby at "top: 0px",
    // I had to target the main container (which is weird)
    // When targeting the job instance grid, the Standby widget
    // was doing calculations that factored in the target's size.
    // This caused the Standby widget to have a top of greater than 0 px.
    // You would think that targeting a DOM element would mean the Standby 
    // widget would overlay completely over the target, but it doesn't.
    // Note 1 - End
    var standby = new Standby({
      id : batchID.GRID_LOADING_PANE,
      target: batchID.MAIN_CONTAINER, // See Note 1
      image : 'imagesShared/search-loading-T.gif',
      imageText : i18n.LOADING_GRID,
      text : i18n.LOADING_GRID,
      tabindex : 0,
      zIndex : 2, // Becaues target is main_container, have to force z-index from auto to 2
      color : '#FFFFFF'// the gif has a white background, so don't use default
    });
    document.body.appendChild(standby.domNode);
    standby.startup();
    return standby;
  }

  function createSearchErrorPane() {
    var pane = new ContentPane({
      id : batchID.SEARCH_COMBINATION_ERROR_PANEL,
      "style" : "display: none;",
      tabindex : 0,
      region : "center"
    });

    var searchErrorPane = new ContentPane({
      id : batchID.SEARCH_COMBINATION_ERROR_PANEL_MSG,
      "class" : "invalidSearchErrorMsg",
      content : "" // Programatically populate this with a NLS message
    });

    var searchErrorMessagePane = new ContentPane({
      content : i18n.BATCH_SEARCH_CRITERIA_INVALID,
      "class" : "invalidSearchDiv",
      doLayout : false
    });

    pane.addChild(searchErrorPane);
    pane.addChild(searchErrorMessagePane);

    return pane;
  }

});