/*******************************************************************************
 * Copyright (c) 2016, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/**
 * Objects and logic to view a resource collection.
 * 
 * This code responds to the type of resource collection being viewed.
 */
define([ 'dojo/_base/declare', 'dojo/_base/lang', 'dojo/_base/window', 'dojo/on', 'dojo/store/Memory', 'dojo/store/Observable', 'dijit/form/ToggleButton',
    'dojo/dom', 'dojo/dom-class', 'dijit/registry', 'dijit/layout/ContentPane', 'dijit/form/DropDownButton', 'dijit/DropDownMenu', 'dijit/MenuItem',
    'dojo/i18n!../nls/explorerMessages', 'jsExplore/views/viewFactory', 'dijit/form/Button',"dijit/focus",
    'dijit/form/Select', 'dijit/form/TextBox', 'jsExplore/views/shared/gridFactory', 'dojo/io-query', 'jsExplore/resources/resourceManager',
    'dijit/TitlePane', 'jsExplore/widgets/SearchPane', 'jsExplore/resources/viewToHash', 'jsExplore/resources/hashUtils', 'jsShared/utils/imgUtils', 'jsExplore/utils/ID', 'dojo/domReady!'],

function(declare, lang, window, on, Memory, Observable, ToggleButton, dom, domClass, registry, ContentPane, DropDownButton, DropDownMenu, MenuItem, i18n, viewFactory,
    formButton, focusUtil, Select, TextBox, gridFactory, ioQuery, resourceManager, TitlePane, SearchPane, viewToHash, hashUtils, imgUtils, ID) {

  'use strict';

  var _InternalSearchViewObj = declare(null, {

      /**
       * This view's unique ID. This is set during construction, and is used as the basis for all all child IDs.
       * 
       * @see __getOrCreateViewHolder for what this value is set to.
       */
      id : null,
      type : "search", // "search", "collection"
      collectionResourceType : "application",  // default to application
      displayType : "list", // "list", "cards",
      
      // The persisted data object when read from the server. We store this so we don't have to re-read when we save. It might
      // be that we will have to re-read if the data is stale because something else has updated the data, but this should
      // prevent the need to read on the odd occasion.
      persistedData: {},

      /**
       * Construct the _InternalSearchViewObj with all of its fixed attributes. These attributes are key to the behavior of the view and do
       * not change after creation.
       */
      constructor : function(viewId, resourceCollection) {
        this.id = viewId;
        this.resourceCollection = resourceCollection;
        
        // Initiate the persistence configuration. Note: Can move to a different function later if need
        userConfig.init("com.ibm.websphere.appserver.adminCenter.tool.explore");
        userConfig.load(lang.hitch(this, function(response){
          if(response && response.searchGrids){
            this.persistedData = response.searchGrids;
          }          
        }));
        
      },

      /**
       * @see openCollectionView
       */
      buildOrSwitchToView : function(pane) {
        
        var breadcrumbWidget = registry.byId(ID.getBreadcrumbContainer());
        
        // Set breadcrumb to search
        registry.byId(ID.getBreadcrumbPane()).setSearchBreadCrumb(false);
        
        // If this view already exists, just switch. We are only called down this path
        // when launched from the dashboard. Access single resource view buttons only
        // runs through this logic once to create the view. Access via breadcrumb does
        // not run through this view.
        if (registry.byId(this.id)) {
          this.__resetSearchResults();
          breadcrumbWidget.selectChild(this.id);
        } else {
          this.__buildView(breadcrumbWidget, pane);
        }

      },

      hideCardContainer : function() {
        if (this.cardContainer) {
          domClass.add(this.cardContainer.id, "cardsPaneHidden");
          domClass.remove(this.cardContainer.id, "cardsPane");
        }
        // need to hide filterBar editbutton too
        var collectionId = this.id.substring(this.id.indexOf('-')+1);
        var filterBarId = ID.dashDelimit(ID.getCollectionView(), collectionId, ID.getFilterBarUpper());
        domClass.remove(filterBarId, "filterBar");
        domClass.add(filterBarId, "filterBarHidden");

        // now make sure the list is displayed
        if (this.listContainer) {
          this.listContainer.set("style", "display:block;");
        }
        if (this.buttonPane) {
          domClass.add(this.buttonPane.id, "searchButtonPane");
          domClass.remove(this.buttonPane.id, "searchButtonPaneHidden");
        }
      },

      hideListContainer : function() {
        if (this.listContainer) {
          this.listContainer.set("style", "display:none;");
        }
        if (this.buttonPane) {
          domClass.remove(this.buttonPane.id, "searchButtonPane");
          domClass.add(this.buttonPane.id, "searchButtonPaneHidden");
        }

        // now make sure the cards are displayed
        if (this.cardContainer) {
          domClass.add(this.cardContainer.id, "cardsPane");
          domClass.remove(this.cardContainer.id, "cardsPaneHidden");
        }
        // need to show filterBar editbutton too
        var collectionId = this.id.substring(this.id.indexOf('-')+1);
        var filterBarId = ID.dashDelimit(ID.getCollectionView(), collectionId, ID.getFilterBarUpper());
        domClass.remove(filterBarId, "filterBarHidden");
        domClass.add(filterBarId, "filterBar");
      },

      /**
       * Creates the view. If this view already has a parent, we'll use it.
       */
      __buildView : function(breadcrumbWidget, parentPane, resource) {
        // TODO: New initialization because we don't destroy properly... we probably should never destroy collection views!
        this.parentPane = null;

        if (parentPane) {
          this.parentPane = parentPane;
        } else {
          // If we were created without a parentPane, create one now and add it to the breadcrumb children.
          this.parentPane = this.__createParentPane();
          this.parentPane.resource = resource;
          breadcrumbWidget.addChild(this.parentPane);
        }

        this.__createViewContent();

      },

      /**
       * Creates this view's parent pane. This is only invoked when the object does not have a parent pane.
       */
      __createParentPane : function() {
        var title = i18n.SEARCH;

        return new ContentPane({
            id : this.id, // This is the 'viewId' and is used by breadcrumbWidget.selectChild(this.id)
            title : title,
            headerTitle : title,
            content : '',
            baseClass : 'topResourceContentPane',
            style: "background: white;"
        });

      },

      __buildSummaryPane : function() {
        var summaryPane = new ContentPane({
            id : ID.dashDelimit(this.id, ID.getSummaryPaneUpper()),
           // content : i18n.SEARCH_NO_RESULTS,
            doLayout : false,
            style: "margin: 5px;"
        });
        return summaryPane;
      },
      
      __buildErrorMsgPane : function() { 
        var errorMsgPane = new ContentPane({
          id : ID.dashDelimit(this.id, ID.getErrorMessagePaneUpper()),
          doLayout : false,
          "class": "invalidSearchErrorMsg"
         });
        return errorMsgPane;
      },

      __buildSearchPane : function() {
        if (registry.byId(ID.getSearchConditionDiv())) {
          registry.byId(ID.getSearchConditionDiv()).destroy();
        }
        var searchPane = new SearchPane({
            id : ID.getSearchConditionDiv(),
//            id : ID.dashDelimit(this.id, ID.getSearchConditionDiv()),
            role : "search"
        });
        if (this.type === "search") {
          searchPane.set("style", "width:100%;");
        }
        return searchPane;
      },

      /**
       * Populates the view with all of its content.
       */
      __createViewContent : function() {
          // This condition is not exercised as there is no collection search view type
          // and we're taking out dojox/mobile package.
//        if (this.type === "collection") {
//          // add a single pane for the specific collection to hold the buttons
//          // min-width is to allow space for the absolute position editButton from the collection filterBar
//          var holder = new ContentPane({
//            baseClass: "searchButtonHolderPane"
//          });
//          this.parentPane.addChild(holder);
//          this.buttonPane = new ContentPane({
//            // TODO: move to ID
//            id: ID.dashDelimit(this.id, "buttonPane"),
//            baseClass: "searchButtonPane"
//          });
//          holder.addChild(this.buttonPane);
//          // add the switch between list and cards button
//          var switchViewButton = new ToggleButton({
//            id: this.id + "switchViewButton",  // TODO: needs ID
//            label: '<span style="display:none;">' + i18n.SEARCH_SWITCH_VIEW + '</span>',
//            title: i18n.SEARCH_SWITCH_VIEW,
//            'class': 'searchViewSwitchView'
//          });
//          switchViewButton.set("role", "button");
//          switchViewButton.set("aria-label", i18n.SEARCH_SWITCH_VIEW);
//          holder.addChild(switchViewButton);
//          on(switchViewButton, "click", lang.hitch(this, function(evt) {   
//            evt.stopPropagation();
//            if (switchViewButton.checked) {
//              this.displayType = "list";
//              this.hideCardContainer();
//              this.__updateRows();
//            } else {
//              this.displayType = "cards";
//              this.hideListContainer();
//            }
//          }));
//          // create a pane to hold the card view
//          // create a pane to hold the list view
//        }
        this.parentPane.addChild(this.__buildSearchPane());
        this.parentPane.addChild(this.__buildErrorMsgPane());
        this.parentPane.addChild(this.__buildSummaryPane());
        
        // add all titles if this is a full search
        if (this.type === "search" || this.collectionResourceType === 'application') {
          this.__addTitlePane(ID.getApplications(), i18n.NUMBER_APPS);
        }
        if (this.type === "search" || this.collectionResourceType === 'server') {
          this.__addTitlePane(ID.getServers(), i18n.NUMBER_SERVERS);
        }
        if (this.type === "search" || this.collectionResourceType === 'cluster') {
          this.__addTitlePane(ID.getClusters(), i18n.NUMBER_CLUSTERS);
        }
        if (this.type === "search" || this.collectionResourceType === 'host') {
          this.__addTitlePane(ID.getHosts(), i18n.NUMBER_HOSTS);
        }
        if (this.type === "search" || this.collectionResourceType === 'runtime') {
          this.__addTitlePane(ID.getRuntimes(), i18n.NUMBER_RUNTIMES);
        }
      },

      __addTitlePane : function(id, title) {
        var titlePane = new TitlePane({
          id: ID.dashDelimit(this.id, id, ID.getTitlePaneUpper()),
          title: lang.replace(title, [ 0 ]),
          open: false
        });
        this.parentPane.addChild(titlePane);
        titlePane.titleNode.id = ID.dashDelimit(titlePane.id, ID.getLabelUpper());
        titlePane.domNode.style.display = 'none';
      },
      
      __setTitlePaneContent : function(titlePane, rowList, resourceType, grid, multiSelectButton, settingsButton) {
        if (this.type === "search" && titlePane.focusNode) {
          titlePane.focusNode.setAttribute("style", "display:inline-block;");
        }
        var container = new ContentPane ({
        });
        this.listContainer = container;

        var actionBar = registry.byId(ID.dashDelimit(titlePane.id, ID.getActionBar()));
        if (actionBar) {
          actionBar.destroy();
        }
        actionBar = new ActionBar({
          id : ID.dashDelimit(titlePane.id, ID.getActionBar()),
          resourceType: resourceType,
          grid: grid,
          actions: null,
          resetActions: null
        });
        grid.actionBar = actionBar;

        container.addChild(actionBar);
        container.addChild(grid);
        on(settingsButton, "click", function(e){
          e.stopPropagation();
          // TODO: anything else?
        });
        
        if (multiSelectButton) {
          on(multiSelectButton, "click", function(evt) {   
            evt.stopPropagation();
            if (globalIsAdmin === false) {
                console.log('Action bar is disabled for users without the Administrator role as they cannot take actions');
                return;
            }
            // Remove the background styling when un-toggling the multi select
            if(!this.checked){
              for (var i = 0; i < grid.rowCount(); i++) {
                gridFactory.setRowBackgroundColor(grid.row(i), false);
              }
            }

            // need to refresh the action for each row
            var columns = grid.columns();
            var actionColumn = -1;
            for (var i = 0; i < columns.length && actionColumn == -1; i++) {
              if (grid.column(i).id === "actions") {
                actionColumn = i;
              }
            }
            for (var i = 0; i < grid.rowCount(); i++) {
              grid.body.refreshCell(i,actionColumn);
            }

            if (!actionBar.processActionBar()) {
              // if action is reset like the collectiveView, then have to
              // remove the observers for selected resources and remove the selected row
              gridFactory.clearRowSelection(grid);
            }

            container.set("style", "height: auto");
            container.resize();
          });

          grid.connect(grid.select.row, 'onSelected', function(row, rowId) {
            actionBar.set("selectCount", grid.select.row.getSelected().length);
            actionBar.enableDisableActionBarActionButtons();
            var rowDataResource = grid.store.data[rowId].actions;
            actionBar.createSelectedResourceObserver(rowDataResource);
            gridFactory.setRowBackgroundColor(grid.row(rowId), true);
          });
          grid.connect(grid.select.row, 'onDeselected', function(row, rowId) {
            actionBar.set("selectCount", grid.select.row.getSelected().length);
            actionBar.enableDisableActionBarActionButtons();
            var rowDataResource = grid.store.data[rowId].actions;
            actionBar.removeSelectedResourceObserver(rowDataResource);
            gridFactory.setRowBackgroundColor(grid.row(rowId), false);
          });
        }
        if (this.type === "search") {
          titlePane.set("content", container.domNode);
        } else {
          container.set("style", "height: auto; display: block;");
          this.parentPane.addChild(container);
        }
        
        if (rowList.data.length > 0) {
          if (this.type === "search") {
            titlePane.set('open', true);
          }
          //if (rowList.data.length > 1) { // lavena: inline or none for 1 row
          if (multiSelectButton) {
            multiSelectButton.domNode.style.display = 'inline';
          }
          //} else { 
          //  multiSelectButton.domNode.style.display = 'none';
          //}
        } else {
          if (this.type === "search") {
            titlePane.set('open', false);
          }
          if (multiSelectButton) {
            multiSelectButton.domNode.style.display = 'none';
          }
        }
        if (this.type === "search") {
          titlePane.watch('open', function(param, oldValue, newValue){
            if (newValue != oldValue && settingsButton) {
              //if (newValue && rowList.data.length > 1) { // lavena: 1 or 0
              if (newValue && rowList.data.length > 0) {
                if(multiSelectButton)
                  { 
                    multiSelectButton.domNode.style.display = 'inline';
                  }
                settingsButton.domNode.style.display = 'inline';
              } else {
                if(multiSelectButton)
                { 
                  multiSelectButton.domNode.style.display = 'none';
                }
                settingsButton.domNode.style.display = 'none';
              }
            }
          });
        }
      },

      /**
       * Updates all of the cards for this view.
       */
      __updateRows : function(calledFromReset) {

        if (this.displayType === "list") {
          if (this.type === "search" || this.collectionResourceType === "applications" || this.collectionResourceType === "appsOnServer") {
            this.appGrid = this.__initGrid("applications", this.appGrid, gridFactory.getApplicationGrid, gridFactory.getApplicationData, this.appResourceCollection, 3, i18n.NUMBER_APPS);
          }
          if (this.type === "search" || this.collectionResourceType === "servers") {
            this.serverGrid = this.__initGrid("servers", this.serverGrid, gridFactory.getServerGrid, gridFactory.getServerData, this.serverResourceCollection, 4, i18n.NUMBER_SERVERS);
          }
          if (this.type === "search" || this.collectionResourceType === "clusters") {
            this.clusterGrid = this.__initGrid("clusters", this.clusterGrid, gridFactory.getClusterGrid, gridFactory.getClusterData, this.clusterResourceCollection, 5, i18n.NUMBER_CLUSTERS);
          }
          if (this.type === "search" || this.collectionResourceType === "hosts") {
            this.hostGrid = this.__initGrid("hosts", this.hostGrid, gridFactory.getHostGrid, gridFactory.getHostData, this.hostResourceCollection, 6, i18n.NUMBER_HOSTS);
          }
          if (this.type === "search" || this.collectionResourceType === "runtimes") {
            this.runtimeGrid = this.__initGrid("runtimes", this.runtimeGrid, gridFactory.getRuntimeGrid, gridFactory.getRuntimeData, this.runtimeResourceCollection, 7, i18n.NUMBER_RUNTIMES);
          }
          if (this.type === "search") {
            // Update total number of resources
            var totalResources = this.appResourceCollection.list.length
            + this.serverResourceCollection.list.length
            + this.clusterResourceCollection.list.length
            + this.hostResourceCollection.list.length
            + this.runtimeResourceCollection.list.length;
            var viewId = ID.getSearchView();
            if (this.type === 'collection') {
              viewId = ID.dashDelimit(ID.getSearchView(), this.collectionResourceType);
            }
            var summaryPane = registry.byId(ID.dashDelimit(viewId, ID.getSummaryPaneUpper()));
            if (summaryPane) {
              if (totalResources > 0 || calledFromReset) {
                /** 
                 *Until the summary pane is updated to show summary count, let's not display it 
                 **/
                summaryPane.set('content', '');            
                summaryPane.set("style", {
                  "display":"none"
                });

              } else {
                summaryPane.set("style", {
                  "display":"block"
                });
                summaryPane.set('class', "noResultsDiv");
                summaryPane.set('content', i18n.SEARCH_NO_RESULTS);            
              }
            }
          }

        } else {
          // card view currently only available with type "collection"
          // must be cards view
        }
      },

      /*
       * Initiate the grid for the resourceType. If the column order is persisted then use it.
       */
      __initGrid : function(resourceType, grid, getGrid, getData, collection, titleNodeIndex, titleNodeLabel) {
        if (!collection) {
          collection = this.resourceCollection;
        }
        if (!collection.hasOwnProperty('list')) {
          collection.list = [];
        }
        var titlePane = this.parentPane.getChildren()[titleNodeIndex];


        if (collection.list.length == 0) {
          if (this.type === "search") {
            titlePane.domNode.style.display = 'none';
          }
        } else {
          if (this.type === "search") {
            titlePane.domNode.style.display = 'block';
          }
          var gridMemory = this.__convertToObservableMemory(collection.list);
          if (grid) {
            if (grid.clearRowSelection) {
              grid.clearRowSelection();
              grid.actionBar.resetActionBar();
            }
            grid.model.clearCache();
            grid.model.setStore(getData(gridMemory));
            grid.body.refresh();
            // update title with the correct number
            if (this.type === "search") {
              this.__updateTitlePaneTitle(titlePane, lang.replace(titleNodeLabel, [ gridMemory.data.length ]), grid.id);
            }
          } else {
            grid = getGrid(gridMemory, this.persistedData);
            if (this.type === "search") {
              titlePane.set('title', grid.titleText);
            }

            var buttonPlace = titlePane.titleBarNode;
            var msbId = ID.dashDelimit(titlePane.id, ID.getMultiSelectListViewButton());
            if (this.type === "collection") {
              buttonPlace = this.buttonPane;
              msbId = ID.dashDelimit(ID.getSearchView(), resourceType, "SummaryPane", ID.getMultiSelectListViewButton());
            }

            var multiSelectButton = registry.byId(msbId);
            if (multiSelectButton) {
              multiSelectButton.destroy();
            }
            var settingsButton = registry.byId(grid.id + '-actionMenugridActions');
            if (settingsButton) {
              settingsButton.destroy();
            }

            // Build the action bar menu
            var actionBarMenu = this.__createActionBarMenu(grid.id);
            this.__createActionBarMenuItems(actionBarMenu, grid);
            var settingsButton = new DropDownButton({
              id: grid.id + '-actionMenugridActions',
              dropDown : actionBarMenu, 
              label : i18n.GRID_ACTIONS, 
              showLabel : false,
              iconClass : 'listViewSettingsIconClass',
              'baseClass' : 'listViewSettings'
            }).placeAt(buttonPlace);
            settingsButton.set("role", "button");
            settingsButton.iconNode.innerHTML = imgUtils.getSVGSmall('settings');

            // TODO: For now just check for runtime here and don't add the multiSelectButton. In the future when metadata can be added
            // to multiple resources at once, this restriction for the runtime grid will be removed.
            var multiSelectButton = null;
            if (!(resourceType === "runtimes")) {
              var multiSelectButtonLabel = (globalIsAdmin === false ? i18n.ACTION_DISABLED_FOR_USER : i18n.ACTIONS);
              multiSelectButton = new ToggleButton({
                id: msbId,
                label: '<span style="display:none;">' + multiSelectButtonLabel + '</span>',
                title: multiSelectButtonLabel,
                'class': 'listViewMultiSelect',
                'iconClass': (globalIsAdmin === false ? 'listViewMultiSelectDisabled' : 'listViewMultiSelectUnchecked'),
                disabled : (globalIsAdmin === false ? true : false),
                value: msbId, // workaround for batchscan false positive label requirement on hidden input tag
                onClick: function(evt) {
                    evt.stopPropagation();
                    if (globalIsAdmin === false) {
                        console.log('Action bar is disabled for users without the Administrator role as they cannot take actions');
                        return;
                    }
                    if (this.get("checked")) {
                        this.set("iconClass", "listViewMultiSelectChecked");
                    } else {
                        this.set("iconClass", "listViewMultiSelectUnchecked");
                    }
                }
              }).placeAt(buttonPlace);
              multiSelectButton.set("role", "button");
              multiSelectButton.set("aria-label", multiSelectButtonLabel);
              multiSelectButton.set("aria-disabled", (globalIsAdmin === false ? 'true' : 'false'));
              if (globalIsAdmin === false) {
                domClass.add(multiSelectButton.domNode, "listViewMultiSelectDisabled");
              }
            }
            this.__setTitlePaneContent(titlePane, gridMemory, resourceType, grid, multiSelectButton, settingsButton);
          }  
        }
        return grid;
      }, 

      __convertToObservableMemory : function(list) {
        var memory = new Observable(new Memory({}));
        for (var i = 0; i < list.length; i++) {
          memory.put(list[i]);
        }
        return memory;
      },
      
      __resetSearchResults : function() {
        registry.byId(ID.getSearchMainBox()).resetSearchBox();
        
        this.serverResourceCollection = {};
        this.appResourceCollection = {};
        this.clusterResourceCollection = {};
        this.hostResourceCollection = {};
        this.runtimeResourceCollection = {};
        this.__updateRows(true);
      },
      
      __updateTitlePaneTitle: function(titlePane, titleText, gridID) {
        var title = titlePane.get("title");
        title = title.substring(0, title.indexOf("\<span"));
        title = title + '<span style="margin-left: 6px;">' + titleText + '</span>';
        titlePane.set("title", title);
        var settingsButton = registry.byId(gridID + '-actionMenugridActions');
        if (settingsButton) {
          settingsButton.placeAt(titlePane.titleBarNode);
        }
        var multiSelectButton = registry.byId(ID.dashDelimit(titlePane.id, ID.getMultiSelectListViewButton()));
        if (multiSelectButton) {
          multiSelectButton.placeAt(titlePane.titleBarNode);
        }
      },
      
      __getPersistedData: function(){
        return this.persistedData;
      },
      
      __setPersistedData: function(_persistedData){
        this.persistedData = _persistedData;
      },
      
      __createActionBarMenu: function(id) {
        var actionBarMenuId = id + '-actionMenu';
        var actionBarMenu = registry.byId(actionBarMenuId);
        if (!actionBarMenu) {
          actionBarMenu = new DropDownMenu({
            id : actionBarMenuId,
            'aria-label' : lang.replace(i18n.GRID_ACTIONS_LABEL, [id]),
            leftClickToOpen : true,
            baseClass : 'actionMenu',
            style : 'display: none;',
            bindGrid: function(grid, col){
              // Attach the grid to the menu so it can do grid-related actions
              this.grid = grid;
              this.colId = col.id;
            }
          });
        }
        return actionBarMenu;
      },

      __createActionBarMenuItems: function(actionBarMenu, grid) {
        if (registry.byId(actionBarMenu.id + grid.structure[grid.hideableColumns[0]].field)) {
          // if the first menuItem has already been created, just return
          return;
        }        
        var self = this;
        for (var i = 0; i < grid.hideableColumns.length; i++) {
          actionBarMenu.addChild(new MenuItem({
            id : actionBarMenu.id + grid.structure[grid.hideableColumns[i]].field, 
            label : grid.structure[grid.hideableColumns[i]].name, 
            value : grid.structure[grid.hideableColumns[i]].field,
            grid : grid,
            baseClass : 'listViewHeaderMenuItem',
            iconClass : grid.structure[grid.hideableColumns[i]].display ? 'listViewHeaderMenuItem_checked' : 'listViewHeaderMenuItem_unchecked', 
            selected : grid.structure[grid.hideableColumns[i]].display,
            onClick : function(){              
              self.__headerMenuItemClick(this, self);
            }
          }));
        }
      },
      
     __headerMenuItemClick: function(me, searchView) {
        var persistedData = searchView.__getPersistedData();
        
        if (me.selected) {
          // unchecking the column
          me.selected = false;
          me.grid.hiddenColumns.add(me.value);
        } else {
          me.selected = true;
          me.grid.hiddenColumns.remove(me.value);
        }
        
        // Store the grid's visible columns and the order
        var gridData = {};
        var colsToPersist = [];    
        var cols = me.grid.columns();
        for(var i=0; i < cols.length; i++){
          colsToPersist.push(cols[i].id);
        }    
        gridData['columns'] = colsToPersist;
        
        // If this grid's columns are already persisted then update the data to not modify other resource's
        if(!persistedData){
          persistedData = {};      
        }        
        persistedData[me.grid.id] = gridData;
        searchView.__setPersistedData(persistedData);
        userConfig.save('searchGrids', persistedData);
        
        me.set('iconClass', me.selected ? 'listViewHeaderMenuItem_checked' : 'listViewHeaderMenuItem_unchecked');
    }
  });

  return {

      /**
       * Opens the view for the given resource collection. If the view for the resource collection has never been created, then the view
       * will be initialized.
       */
      openSearchView : function(queryParams, searchType, pane, viewId) {
        //console.error("resourceType:" + this.collectionResourceType);
        if (!viewId) {
          if (this.type === 'collection') {
            viewId = ID.dashDelimit(ID.getSearchView(), this.collectionResourceType);
          } else {
            viewId = ID.getSearchView();
          }          
        }
        //console.error("viewId: " + viewId);
        var viewHolder = this.__getOrCreateViewHolder(viewId, this.resourceCollection);
        var me = this;
        viewHolder.view.type = searchType;
        if (searchType === 'collection') {
          viewHolder.view.collectionResourceType = this.collectionResourceType;
          viewHolder.view.displayType = this.displayType;
        }
        viewHolder.view.buildOrSwitchToView(pane);
        focusUtil.focus(dom.byId(ID.getSearchTextBox()));
        if (queryParams){
          var queryObject = ioQuery.queryToObject(queryParams);
          // for search the type will still be application instead of appOnCluster/appOnServer
          if (queryObject.type === "appOnCluster" || queryObject.type === "appOnServer") {
            queryObject.type = "application";
          }
          var performSearch = false;
          var summaryPane = registry.byId(ID.dashDelimit(viewId, ID.getSummaryPaneUpper()));
          if (summaryPane) {
            if (queryObject.type === 'appOnServers') {
              summaryPane.set('content', i18n.SEARCH_UNSUPPORT_TYPE_APPONSERVER);
              summaryPane.set('class', "unsupportedSearchDiv");
              summaryPane.set("style", {"display":"block"});
            } else if (queryObject.type === 'appOnClusters') {
              summaryPane.set('content', i18n.SEARCH_UNSUPPORT_TYPE_APPONCLUSTER);
              summaryPane.set('class', "unsupportedSearchDiv");
              summaryPane.set("style", {"display":"block"});
            } else if (queryObject.type === undefined && 
                       queryObject.state === undefined && 
                       queryObject.name === undefined && 
                       queryObject.tag === undefined && 
                       queryObject.runtimeType === undefined && 
                       queryObject.container === undefined && 
                       queryObject.owner === undefined && 
                       queryObject.contact === undefined &&
                       queryObject.note === undefined){
              summaryPane.set('class', "unsupportedSearchDiv");
              summaryPane.set("style", {"display":"block"});
              summaryPane.set('content', i18n.SEARCH_UNSUPPORT);
            } else {
              performSearch = true;
            }
          }
          if (performSearch) {
            registry.byId(ID.getSearchMainBox()).populateSearchPillPane(queryObject);
            me.loading(true);
            resourceManager.getSearchResults(queryObject).then(function(resources) {
              console.debug('resources: ', resources);
              me.loading(false);
              me.refresh(resources.apps, resources.servers, resources.clusters, resources.hosts, resources.runtimes);
            });
          }
        } else {
          viewToHash.updateHash(hashUtils.getCurrentHash());
        }
      },

      /**
       * Opens the view for the given resource collection. If the view for the resource collection has never been created, then the view
       * will be initialized.
       */
      openCollectionView : function(resourceCollection, defaultSideTab, pane) {
        this.type = "collection";
        this.collectionResourceType = resourceCollection.id; //.substring(0, resourceCollection.id.length-1);
        this.resourceCollection = resourceCollection;
        this.displayType = "cards";
        var queryParams = 'type=' + this.collectionResourceType.substring(0, this.collectionResourceType.length-1);  // strip off 's'
        var viewId = ID.dashDelimit(ID.getSearchView(), this.collectionResourceType);
        if (pane) {
          // must be an object collection from sideTab so build a different queryParams
          this.collectionResourceType = resourceCollection.id.substring(0, resourceCollection.id.indexOf("("));
          viewId = ID.dashDelimit(ID.getSearchView(), resourceCollection.id);
          queryParams = null;
        }
        this.openSearchView(queryParams, 'collection', pane, viewId);
        var breadcrumbPane = registry.byId(ID.getBreadcrumbPane());
        breadcrumbPane.setBreadcrumb(resourceCollection);
        var viewHolder = this.__getOrCreateViewHolder(viewId);
        viewFactory.collectionView.openCollectionView(resourceCollection, defaultSideTab, viewHolder.view.parentPane, false);
        var cardContainerId = ID.dashDelimit(ID.getCollectionView(), resourceCollection.id) + ID.getCenterPane();
        viewHolder.view.cardContainer = registry.byId(cardContainerId);
        if (viewHolder.view.cardContainer && this.displayType === "list") {
          viewHolder.view.hideCardContainer();
        }
      },

      refresh : function(apps, servers, clusters, hosts, runtimes) {
        var viewId = ID.getSearchView();
        if (this.type === 'collection') {
          viewId = ID.dashDelimit(ID.getSearchView(), this.collectionResourceType);
        }
        var viewHolder = this.__getOrCreateViewHolder(viewId);
        viewHolder.view.appResourceCollection = apps;
        viewHolder.view.serverResourceCollection = servers;
        viewHolder.view.clusterResourceCollection = clusters;
        viewHolder.view.hostResourceCollection = hosts;
        viewHolder.view.runtimeResourceCollection = runtimes;
        viewHolder.view.__updateRows();
      },
      
      loading : function(load){
        var viewId = ID.getSearchView();
        if (this.type === 'collection') {
          viewId = ID.dashDelimit(ID.getSearchView(), this.collectionResourceType);
        }
        var summaryPane = registry.byId(ID.dashDelimit(viewId, ID.getSummaryPaneUpper()));
        if(summaryPane){      
            if(load){
              summaryPane.set("style", {
                "display":"block"
              });
              summaryPane.set('class', "loadingOverlayDiv");
              summaryPane.set('content', "");    
            }else{
              summaryPane.set("style", {
                "display":"none"
              });             
            }
        }
      },
      
      /**
       * dojo Memory, used to store created views.
       */
      __searchViews : new Memory({}),

      /**
       * Gets or creates a view for the given viewId.
       * 
       * @param viewId
       *          The unique viewId to obtain or create. The viewId implies scoping, e.g. 'collectionView-applications' is used for a new
       *          breadcrumb element which is for all applications in the collective. An ID like 'collectionView-appsOnServer(myServer)'
       *          implies a scoped view which will have a parentPane. We do not need to include the parentPane ID in the view as the
       *          resource collection IDs are already scoped.
       * @param
       */
      __getOrCreateViewHolder : function(viewId, resourceCollection) {
        var viewHolder = this.__searchViews.get(viewId);
        if (viewHolder === undefined) {
          viewHolder = {
              id : viewId,
              view : new _InternalSearchViewObj(viewId, resourceCollection)
          };
          this.__searchViews.put(viewHolder);
        }
        return viewHolder;
      },
      
      clearSearchResults : function() {
        var viewId = ID.getSearchView();
        if (this.type === 'collection') {
          viewId = ID.dashDelimit(ID.getSearchView(), this.collectionResourceType);
        }
        var viewHolder = this.__getOrCreateViewHolder(viewId);
        viewHolder.view.serverResourceCollection = {};
        viewHolder.view.appResourceCollection = {};
        viewHolder.view.clusterResourceCollection = {};
        viewHolder.view.hostResourceCollection = {};
        viewHolder.view.runtimeResourceCollection = {};
        viewHolder.view.__updateRows(true);
        this.clearErrorMessagePane();

      },
      
      clearErrorMessagePane : function() {
        var viewId = ID.getSearchView();
        if (this.type === 'collection') {
          viewId = ID.dashDelimit(ID.getSearchView(), this.collectionResourceType);
        }
          var errorMsgPane = registry.byId(ID.dashDelimit(viewId, ID.getErrorMessagePaneUpper()));
          if (errorMsgPane) {
            errorMsgPane.set('content', '');            
            errorMsgPane.set("style", {"display":"none"}); 
          }
      }    
  };    

});