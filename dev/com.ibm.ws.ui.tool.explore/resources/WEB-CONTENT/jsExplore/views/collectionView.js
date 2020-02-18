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
/**
 * Objects and logic to view a resource collection.
 * 
 * This code responds to the type of resource collection being viewed.
 */
define([ 'dojo/_base/declare', 'dojo/_base/lang', 'dojo/_base/window', 'dojo/Deferred', 'dojo/keys', 'dojo/on', 'dojo/store/Memory',

         'dojo/store/Observable', 'dojo/dom-construct', 'dojo/dom', 'dojo/dom-class', 'dijit/registry',

         'dijit/layout/ContentPane', 'dojo/i18n!../nls/explorerMessages', 'js/common/platform',

         'js/widgets/ConfirmDialog', 'jsExplore/resources/utils', 'jsExplore/views/viewFactory', 'jsExplore/views/shared/allX',

         'jsExplore/widgets/CardBorderContainer', 'jsExplore/widgets/FilterBar', 'jsExplore/widgets/shared/ActionButtons',

         'jsExplore/utils/featureDetector', 'jsExplore/resources/resourceManager', 'dijit/DropDownMenu',
         
         'jsExplore/widgets/BreakAffinityDialog', 'dojo/request',
         
         'dijit/MenuItem', 'dojo/_base/window', 'jsExplore/utils/constants', 'jsExplore/utils/ID', 'jsShared/utils/imgUtils'],

         function(declare, lang, window, Deferred, keys, on, Memory, Observable, domConstruct, dom, domClass, registry, ContentPane,

             i18n, platform, ConfirmDialog, utils, viewFactory, allX, CardBorderContainer, FilterBar, ActionButtons,

             featureDetector, resourceManager, DropDownMenu, BreakAffinityDialog, request, MenuItem, win, constants, ID, imgUtils) {

  'use strict';

  // Constants used to control default behaviours
  var MINIMUM_CHUNK_COUNT = 50;
  var IDLE_TIMEOUT = 500; //miliseconds
  var _idleMiliSecondsCounter = 0;

  function getNameFromTuple(tuple) {
    return tuple.substring(tuple.lastIndexOf(',')+1);
  }

  var _InternalCollectionViewObj = declare(null, {

    /**
     * This view's unique ID. This is set during construction, and is used as the basis for all all child IDs.
     * 
     * @see __getOrCreateViewHolder for what this value is set to.
     */
    id : null,

    /**
     * Construct the _InternalCollectionViewObj with all of its fixed attributes. These attributes are key to the behavior of the view and
     * do not change after creation.
     */
    constructor : function(viewId, resourceCollection, displayFilters) {
      this.id = viewId;
      this.resourceCollection = resourceCollection;
      this.resourceCollection.subscribe(this);
      this.resources = [];
      this.displayFilters = displayFilters;
    },

    destroy: function() {
      this.resourceCollection.unsubscribe(this);
      if ( this.resources ) {
        for (var r = 0; r < this.resources.length; r++) {
          var resource = resources[r];
          if ( resource.type === 'host' || resource.type === 'server' ) {
            resource.unsubscribe(this);
          }
        }
      }
    },

    onMaintenanceModeChange: function() {
      this.__updateCards();
    },
    /**
     * Refresh the filter counts on change.
     */
    onTallyChange: function(newTally, oldTally) {
      
      if (this.filterBar) {
        // TODO: Change this to push in each card as its added
        // Need to re-draw the cards because something changed state
        this.__updateCards();
      }
    },

    /**
     * Update the server cards when server changes are detected
     */
    onServersListChange: function() {
      console.log("collectionView got an onServersListChange event!");
      this.__updateCards();
    },
    
    /**
     * Update the application cards when application changes are detected
     */
    onAppsListChange: function() {
      console.log("collectionView got an onAppsListChange event!");
      this.__updateCards();
    },

    /**
     * Update the cards when new elements are added
     */
    onListChange: function(newList, oldList, added, removed) {
      // deprecated.  Try using onAppsListChange and onServersListChange.
    },
    
    /**
     * Update the Runtime On Hosts cards when runtimes list changes are detected
     */
    onRuntimesListChange: function() {
      console.log("collectionView got an onRuntimesListChange event!");
      this.__updateCards();
    },

    /**
     * @see openCollectionView
     */
    buildOrSwitchToView : function(defaultFilter, parentPane, resource) {
      var breadcrumbWidget = registry.byId(ID.getBreadcrumbContainer());

      // If this view already exists, just switch. We are only called down this path
      // when launched from the dashboard. Access single resource view buttons only
      // runs through this logic once to create the view. Access via breadcrumb does
      // not run through this view.
      if (registry.byId(this.id)) {
        breadcrumbWidget.selectChild(this.id);
        // reset to make sure multiselect is closed
        if (this.filterBar.clicked) {
          // multiselect bar is open so close it
          // Hack the domClass and clicked because I can't figure out how to get to the editButton.onClick() method
          // this.filterBar.editButton.onClick();
          domClass.remove(this.filterBar.editButton.domNode, "editButtonsClicked");
          this.filterBar.clicked = !this.filterBar.clicked;
          //this.filterBar.processEditButton();
          this.filterBar.actionBar.processActionBar();
        }
      } else {
        this.__buildView(breadcrumbWidget, parentPane, resource);
      }

      if (defaultFilter) {
        this.filterBar.set('currentFilter', defaultFilter);
      }
    },
    
//    updateMMLabels: function() {
//      this.__updateMMLabels();
//    },

    /**
     * Creates the view. If this view already has a parent, we'll use it.
     */
    __buildView : function(breadcrumbWidget, parentPane, resource) {
      // TODO: New initialization because we don't destroy properly... we probably should never destroy collection views!
      this.parentPane = null;
      this.filters = new Observable(new Memory({}));
      this.cardList = new Observable(new Memory({}));

      if (parentPane) {
        this.parentPane = parentPane;
      } else {
        // If we were created without a parentPane, create one now and add it to the breadcrumb children.
        this.parentPane = this.__createParentPane();
        this.parentPane.resource = resource;
        breadcrumbWidget.addChild(this.parentPane);
      }

      this.__initializeCardChunking(dom.byId(ID.getBreadcrumbContainer()), breadcrumbWidget);
      this.__createViewContent();
    },

    /**
     * Creates this view's parent pane. This is only invoked when the object does not have a parent pane.
     */
    __createParentPane : function() {
      var title = 'INVALID_TYPE';

      var type = this.resourceCollection.type;
      switch (type) {
      case 'applications':
        title = i18n.ALL_APPS;
        break;
      case 'clusters':
        title = i18n.ALL_CLUSTERS;
        break;
      case 'hosts':
        title = i18n.ALL_HOSTS;
        break;
      case 'servers':
        title = i18n.ALL_SERVERS;
        break;
      case 'runtimesOnHost':
      case 'runtimes':
        title = i18n.RUNTIMES;
        break;
      default:
        console.error('__createParentPane can not determine title for resource type: ' + type);
      }

      return new ContentPane({
        id : this.id, // This is the 'viewId' and is used by breadcrumbWidget.selectChild(this.id)
        title : title,
        headerTitle : title,
        content : '',
        baseClass : 'topResourceContentPane'
      });
    },

    /**
     * Initializes the logic we use to limit the amount of cards we render. This uses some object state, as well as event bindings. 
     * TODO: How does this play with collection views which are not 'all' views?
     */
    __initializeCardChunking : function(breadcrumbDOM, breadcrumbWidget) {
      var debug = false;
      if (debug) {
        console.log("clientHeight:" + breadcrumbDOM.clientHeight);
      }

      this.scrollOffset = 0;
      this.built_cards = 0;
      this.visible_cards = 0;
      this.max_built_cards = this.BUILD_CHUNKS = (~~(((breadcrumbDOM.clientHeight - 100) / 150) * (breadcrumbDOM.clientWidth / 250))) * 2 + 10;
      if (debug) {
        console.log("initial buildchunks & maxBuiltCards: " + this.BUILD_CHUNKS);
      }

      /*on(breadcrumbWidget, "scroll", lang.hitch(this, function(evt) {
        // This logic should only run when we're the active breadcrumb selection
        if (breadcrumbWidget.selectedChildWidget.id === this.id) {
          // if scrollHeight-scrollTop < clientHeight*2 meaning less than 2 screens of data
          if (debug) {
            console.log("compare " + (evt.target.scrollHeight - evt.target.scrollTop - 100) + " to " + evt.target.clientHeight * 2);
          }
          if (evt.target.scrollTop > this.scrollOffset
              && ((evt.target.scrollHeight - evt.target.scrollTop - 100) <= evt.target.clientHeight * 2)) {
            // scrolled down so build the next set
            this.scrollOffset = evt.target.scrollTop;
            this.max_built_cards += this.BUILD_CHUNKS;
            if (debug) {
              console.log("new maxBuiltCards:" + this.max_built_cards);
            }
            this.__updateCards();
          }
        }
      }));*/
      
      document.onmousemove = function() {
          _idleMiliSecondsCounter = 0;

      };
      document.onkeypress = function() {
        _idleMiliSecondsCounter = -100;
      };
      document.onclick = function() {
        _idleMiliSecondsCounter = -100;
    };
      var maxBuiltCards = this.resourceCollection.list.length;
      var maxCards = this.resourceCollection.list.length;
      var CheckIdleTime = lang.hitch(this, function() {
        _idleMiliSecondsCounter += 100;
        if (_idleMiliSecondsCounter >= IDLE_TIMEOUT ) {
          this.BUILD_CHUNKS = 10;
          this.max_built_cards += this.BUILD_CHUNKS;
          if (debug) {
            console.log("new maxBuiltCards:" + this.max_built_cards);
          }
          maxBuiltCards = this.max_built_cards;
          this.__updateCards();
        }
      });
      var refreshId = setInterval(function() {
        if (maxCards < maxBuiltCards) {
          clearInterval(refreshId);
        }else{
          CheckIdleTime();
        }
      }, 10);

      on(window.global, 'resize', lang.hitch(this, function(evt) {
        var newChunks = (~~(((breadcrumbDOM.clientHeight - 100) / 150) * (breadcrumbDOM.clientWidth / 250))) * 2;
        // do not remove this --- otherwise cards will disappear when the browser is resized to a less than ~559px defect 194902
        if(newChunks <= 2){
          newChunks = 2;
        }
        newChunks = newChunks > MINIMUM_CHUNK_COUNT ? MINIMUM_CHUNK_COUNT : newChunks;
        var chunksDisplayed = ~~(this.max_built_cards / this.BUILD_CHUNKS);
        if (newChunks > this.BUILD_CHUNKS) {
          // add some cards
          this.max_built_cards = chunksDisplayed * newChunks;
          if (debug) {
            console.log("new maxBuiltCards:" + this.max_built_cards);
          }
          this.scrollOffset = breadcrumbDOM.scrollTop;
          this.__updateCards();
        } else {
          // chunk size went down
          if (this.built_cards < chunksDisplayed * newChunks) {
            this.max_built_cards = chunksDisplayed * newChunks;
            if (debug) {
              console.log("new maxBuiltCards:" + this.max_built_cards);
            }
          }
        }
        this.BUILD_CHUNKS = newChunks;
        if (debug) {
          console.log("resized window so recalculate buildchunks:" + this.BUILD_CHUNKS);
        }
      }));
    },

    /**
     * Populates the view with all of its content.
     */
    __createViewContent : function() {

      var collectionView = this;

      var showLabel = !platform.isPhone(); // Hide labels on the phone

      // Create the FilterBar
      this.filterBar = new FilterBar({
        id : ID.dashDelimit(this.id, ID.getFilterBarUpper()),
        page : this.parentPane,
        resourceType : this.resourceCollection.type, // TODO: This is really used for the title... so I don't get why its called 'resourceType'
        actions : null,
        parent : this,
        resetActions : null
      });
      this.parentPane.addChild(this.filterBar);

      var getResourceIcon = function(resourceCollection) {
        var type = resourceCollection.type;
        switch (type) {
        case 'applications':
          return 'app';
        case 'appsOnCluster':
          return 'appOnCluster'; //nav-appOnCluster-selected
        case 'appsOnServer':
        case 'appInstancesByCluster':
          return 'appOnServer'; //nav-instance-selected
        case 'clusters':
          return 'cluster';
        case 'hosts':
          return 'host';
        case 'servers':
        case 'serversOnCluster':
        case 'serversOnHost':
        case 'serversOnRuntime':
          return 'server';
        case 'runtimesOnHost':
        case 'runtimes':
        case 'runtime':
          return 'runtime';
        default:
          console.error('getResourceIcon called for an unknown resource type: ' + type);
        }
      };

      // Compute our icons for use in the FilterBar
        //note: filterbar uses SMALL versions of these
      var resourceIcon = getResourceIcon(this.resourceCollection);
      var runningIcon = "status-running";
      var someRunningIcon = "status-some-running";
      var stopIcon = "status-stopped";
      var alertIcon = "status-alert";

      var getFilterResourceName = function(resourceCollection) {
        var type = resourceCollection.type;
        switch (type) {
        case 'applications':
        case 'appsOnCluster':
        case 'appsOnServer':
        case 'appInstancesByCluster':
          return i18n.APPLICATIONS;
        case 'clusters':
          return i18n.CLUSTERS;
        case 'hosts':
          return i18n.HOSTS;
        case 'servers':
        case 'serversOnCluster':
        case 'serversOnHost':
        case 'serversOnRuntime':
          return i18n.SERVERS;
        case 'runtimesOnHost':
        case 'runtime':
        case 'runtimes':
          return i18n.RUNTIMES;
        default:
          console.error('getFilterResourceName called for an unknown resource type: ' + type);
        }
      };

      // The list of filters applicable to this resource
      var startedLabel = null;
      var partialLabel = null;
      var stoppedLabel = null;
      switch (this.resourceCollection.type) {
      case 'hosts':
        startedLabel = i18n.HOST_WITH_SERVERS_RUNNING;
        partialLabel = i18n.HOST_WITH_SOME_SERVERS_RUNNING;
        stoppedLabel = i18n.HOST_WITH_ALL_SERVERS_STOPPED;
        break;
      case 'runtimesOnHost':
      case 'runtimes':
        startedLabel = i18n.RUNTIME_WITH_SERVERS_RUNNING;
        partialLabel = i18n.RUNTIME_WITH_SOME_SERVERS_RUNNING;
        stoppedLabel = i18n.RUNTIME_WITH_ALL_SERVERS_STOPPED;
        break;
      default:
        startedLabel = i18n.RUNNING;
        partialLabel = i18n.PARTIALLY_RUNNING;
        stoppedLabel = i18n.STOPPED;
      };
      this.filterBar.addFilter({
        id : ID.getTotalUpper(),
        icon : resourceIcon + '-selected',
        label : getFilterResourceName(this.resourceCollection)
      });
      this.filterBar.addFilter({
        id : ID.getSTARTED(),
        icon : runningIcon,
        label : startedLabel
      });
      // Partially started does not apply to servers nor appsOnServer
      if (!(this.resourceCollection.type === 'servers' || this.resourceCollection.type === 'appsOnServer')) {
        this.filterBar.addFilter({
          id : ID.getPARTIALLYSTARTED(),
          icon : someRunningIcon,
          label : partialLabel,
          hideIfZero: true
        });
      }
      this.filterBar.addFilter({
        id : ID.getSTOPPED(),
        icon : stopIcon,
        label : stoppedLabel
      });
      if (this.resourceCollection.type !== 'runtimesOnHost') {
        this.filterBar.addFilter({
          id : ID.getAlertUpper(),
          icon : alertIcon,
          label : i18n.ALERT,
          hideIfZero : true
        });
      }

      /* TODO: Enable searching!
       * var searchPane = new ContentPane({ // style : "", content : "", doLayout : false }, "searchPane");
       * registry.byId(resourcePage).addChild(searchPane); if (!platform.isPhone()) { search.searchTab(false, searchPane, "servers"); }
       */
      var centerPane = new ContentPane({
        id : this.id + ID.getCenterPane(),
        doLayout : false,
        baseClass: 'cardsPane'
      });
      this.parentPane.addChild(centerPane);

      // Triggers when the current filter changes (and updates the visible cards)
      this.filterBar.watch('currentFilter', function() {
        collectionView.__updateCards();
      });

      // Triggers when selection mode is enabled and adds selected cards to the cardList
      this.filterBar.actionBar.watch('selectMode', function(name, oldValue, newValue) {
        // manage austoscaled resources
        
        // If we've been opened, newValue===true
        if (newValue) {
          // Toggle the 'no actions for auto scaled resources' message based on the displayed cards
          var collectionContainsAutoscaled = false;
          collectionView.cardList.query({
            visible : true
          }).forEach(function(cardHolder) {
            var resource = cardHolder.card.resource;
            dom.byId(cardHolder.card.id+'newCard').setAttribute('aria-pressed','false');
            if (resource.scalingPolicyEnabled) {
              collectionContainsAutoscaled = true;
            }
          });
        }


        if (oldValue && !newValue) {
          collectionView.cardList.query({
            visible : true
            //selected : true     if removing this causes errors in future - add it back and just add a second query to remove aria pressed for selected = false
          }).forEach(function(cardHolder) {
            // Update the card holder. The cardList.put triggers other things to react to the change.
            cardHolder.selected = false;
            cardHolder.card.setSelected(false);
            dom.byId(cardHolder.card.id+'newCard').removeAttribute('aria-pressed');
            collectionView.filterBar.actionBar.removeSelectedResourceObserver(cardHolder.card.resource);
            collectionView.cardList.put(cardHolder);
          });
        }
      });

      this.cardList.query({
        visible : true
      }, {}).observe(function(cardHolder, removedFrom, insertedInto) {
        // This appears to be only called on add/remove to/from view for filtering so DON'T destroy
        if (removedFrom >= 0) {
          centerPane.removeChild(cardHolder.card);
        } else {
          centerPane.addChild(cardHolder.card, insertedInto);
        }
//      centerPane.resize();
      });

      this.__updateCards();
      // Set the initial filter counts
      this.filterBar.updateFilterValues(this.resourceCollection, this.resources);
      this.filterBar.resize();

    },
    
    /**
     * various methods to get counts on the selected cards
     */
    getSelectedCount : function() {
      var totalSelectedCount = this.cardList.query({
        visible : true,
        selected : true
      }, {
        count : 0
      }).total;
      return totalSelectedCount;
    },
    
    getSelectedWithStartedCount : function() {
      var startedCount = this.cardList.query({
        visible : true,
        selected : true,
        state : 'STARTED'
      }, {
        count : 0
      }).total;
      return startedCount;
    },
    
    getSelectedWithStoppedCount : function() {
      var stoppedCount = this.cardList.query({
        visible : true,
        selected : true,
        state : 'STOPPED'
      }, {
        count : 0
      }).total;
      return stoppedCount;
    },

    getSelectedWithPartialCount : function() {
      var partialCount = this.cardList.query({
        visible : true,
        selected : true,
        state : 'PARTIALLY_STARTED'
      }, {
        count : 0
      }).total;
      return partialCount;
    },
    
    getSelectedWithUnknownCount : function() {
      var unknownCount = this.cardList.query({
        visible : true,
        selected : true,
        state : 'UNKNOWN'
      }, {
        count : 0
      }).total;
      return unknownCount;
    },
    
    /*
     * If any autoscaled resources are stopped and not in maintenance mode, disable start and start --clean actions
     */
    getSelectedAutoScaledNotInMaintenanceModeCount : function() {
      var autoScaledNotInMMCount = 0;
        this.cardList.query({
        visible : true,
        selected : true,
        state : 'STOPPED',
        maintenanceMode : constants.MAINTENANCE_MODE_NOT_IN_MAINTENANCE_MODE
      }).forEach(function(cardHolder) {
        if(cardHolder.card.resource && cardHolder.card.resource.scalingPolicyEnabled){
          autoScaledNotInMMCount++;
        }
      });
      return autoScaledNotInMMCount;
    },

    getSelectedWithNodeJSAppOnClusterCount : function() {
      var nodeJSAppOnClusterCount = this.cardList.query(function(cardHolder) {
        var nodeJSAppOnCluster = false;
        if (cardHolder.visible && cardHolder.selected && cardHolder.card.resource.type === "appOnCluster") {
          resourceManager.getServer(cardHolder.card.resource.servers.list).then(function(servers) {
            for (var i = 0; i < servers.length; i++) {
              if (constants.RUNTIME_NODEJS === servers[i].runtimeType) {
                nodeJSAppOnCluster = true;
                return nodeJSAppOnCluster;
              }
            }
            return false;
          });
        }
        return nodeJSAppOnCluster;
      }, {
        count : 0
      }).total;
      return nodeJSAppOnClusterCount;
    },

    getSelectedWithNodeJSAppOnServerCount : function() {
      var nodeJSAppOnServerCount = this.cardList.query(function(cardHolder) {
        return cardHolder.visible && cardHolder.selected && cardHolder.card.resource.type === "appOnServer" && cardHolder.card.resource.parentResource.runtimeType && cardHolder.card.resource.parentResource.runtimeType === constants.RUNTIME_NODEJS;
      }, {
        count : 0
      }).total;
      return nodeJSAppOnServerCount;
    },

    getSelectedWithScalingPolicyEnabledCount : function() {
      var scalingPolicyEnabledCount = this.cardList.query(function(cardHolder) {
        return cardHolder.visible && cardHolder.selected && cardHolder.card.resource.scalingPolicyEnabled;
      }, {
        count : 0
      }).total;
      return scalingPolicyEnabledCount;
    },

    getSelectedWithInMaintenanceModeCount : function() {
      var inMM = this.cardList.query({
        visible : true,
        selected : true,
        maintenanceMode : constants.MAINTENANCE_MODE_IN_MAINTENANCE_MODE
      }, {
        count : 0
      }).total;
      return inMM;
    },

    getSelectedWithNotInMaintenanceModeCount : function() {
      var notInMM = this.cardList.query({
        visible : true,
        selected : true,
        maintenanceMode : constants.MAINTENANCE_MODE_NOT_IN_MAINTENANCE_MODE
      }, {
        count : 0
      }).total;
      return notInMM;
    },
    
    setCardSelection : function(isSelect) {
      var me = this;
      this.cardList.query({
        visible : true
      }).forEach(function(card) {
        card.card.setSelected(isSelect);
        card.selected = isSelect;
        me.cardList.put(card);
      });
    },
    
    removeCard : function(id) {
      var collectionView = this;
      collectionView.cardList.query().forEach(function(cardObj){
        console.log("cardObjId ", cardObj);
        if (cardObj.id === id) {
          console.log("removing ", cardObj.id);
          collectionView.cardList.remove(cardObj.id);
        }
      });      
    },
    
    /**
     * Updates all of the cards for this view.
     */
    __updateCards : function() {
      var collectionView = this;
      
      // Figure out which cards need to be removed
      collectionView.cardList.query().forEach(function(cardObj){
        cardObj.found = false;
      });

      // TODO: This is kind of bleh. It would be better to move this into the resourceColleciton itself... (maybe?)
      var getFn;
      if (this.resourceCollection.type === 'applications') {
        getFn = resourceManager.getApplication;
      } else if (this.resourceCollection.type === 'clusters') {
        getFn = resourceManager.getCluster;
      } else if (this.resourceCollection.type === 'hosts') {
        getFn = resourceManager.getHost;
      } else if (this.resourceCollection.type === 'servers') {
        getFn = resourceManager.getServer;
      } else if (this.resourceCollection.type === 'runtimes') {
        getFn = resourceManager.getRuntime;
      } else {
        getFn = function(resource) {
          var deferred = new Deferred();
          deferred.resolve(resource, true);
          return deferred;
        };
      }

      // Select a subset of the resource collection to render
      var toRender = this.resourceCollection.list.slice(0, this.max_built_cards);
      getFn(toRender).then(function(resources) {
        for (var r = 0; r < resources.length; r++) {
          var resource = resources[r];
          // First call to cardList.get determines if it exists, if not
          var cardObjExists = collectionView.cardList.get(resource.id);
          // Always update the card - this will force it to show if it exists or create it if it doesn't
          collectionView.__updateCard(resource);
          if (cardObjExists) {
            cardObjExists.found = true;
          } else {
            // The card object now exists, so mark it found and also store the resource into the view's resources list
            var cardObj = collectionView.cardList.get(resource.id);
            cardObj.found = true;
            collectionView.resources.push(resource);
            //listen to maintenance mode change
            if ( "host" === resource.type || "server" === resource.type )
              resource.subscribe(collectionView);
          }
        }
        
        collectionView.cardList.query().forEach(function(cardObj){
          if (!cardObj.found) {
            console.log("removing card ", cardObj);
            collectionView.cardList.remove(cardObj.id);

            // Remove the resource from the list of resources
            for (var i = (collectionView.resources.length - 1); i >= 0; i--) {
              if (collectionView.resources[i].id === cardObj.id) {
                var removedResource = collectionView.resources.splice(i, 1);
                if ( "host" === removedResource.type || "server" === removedResource.type )
                  removedResource.unsubscribe(collectionView);
              }
            }

            cardObj.card.destroy();
            collectionView.built_cards--;
          }
        });
        //collectionView.__updateMMLabels();
        collectionView.filterBar.actionBar.updateMMLabels();
        collectionView.filterBar.updateFilterValues(collectionView.resourceCollection, collectionView.resources);
        collectionView.filterBar.resize();
      });
    },

    /**
     * Creates a card for a given resource object. The resource object is part of the collection's list.
     */
    __updateCard : function(resource) {
      var type = resource.type;
      switch (type) {
      case 'appOnCluster':
      case 'appOnServer':
        this.__updateApplicationCard(resource);
        break;
      case 'cluster':
        this.__updateClusterCard(resource);
        break;
      case 'host':
        this.__updateHostCard(resource);
        break;
      case 'server':
        this.__updateServerCard(resource);
        break;
      case 'runtime':
        this.__updateRuntimeCard(resource);
        break;
      default:
        console.error('__updateCard called for an unknown resource type: ' + type);
      }
    },

    __updateApplicationCard : function(application) {
      var collectionView = this;

      var appClass = 'cardBorderContainerGraph';
      if (utils.isStandalone()) {
        appClass = 'cardBorderContainerSingleAppInst';
      } else if (application.type == 'appOnCluster') {
        appClass = 'cardBorderContainerClusterApp';
      } else {    // appOnServer
        appClass = 'cardBorderContainerSingleAppInst';
      }
      var classes = appClass + ' cardboardContainerMobileWidth'; // Handle the 'single server' case

      var openSingleAppView = function() {
          viewFactory.openView(application);
      };

      var appListProcessor = null;

      var appListProcessor = function(application) {
        // If the user has passed us an app (appOnCluster card shows X/N running
        var totalInstances = application.up + application.down + application.unknown;
        return {
          template : i18n.APPS_INSTANCES_RUNNING_OF_TOTAL,
          inserts : [application.up, totalInstances]
        };
      };
      
      allX.commonUpdateCard(collectionView, collectionView.id, classes, application, openSingleAppView, appListProcessor,
          null);
    },

    __updateClusterCard : function(cluster) {
      var collectionView = this;

      // Need to call commonUpdateCard from inside getServers() so we have resolved the cluster's servers
      var classes = 'cardBorderContainerCluster cardboardContainerMobileWidth';

      var openSingleClusterView = function() {
        viewFactory.openView(cluster);
      };

      var appListProcessor = function(cluster) {
        return {
          template : ((cluster.apps.list.length > 2 || cluster.apps.list.length < 1) ? i18n.APPS_LIST : i18n.EMPTY_MESSAGE),
          inserts : ((cluster.apps.list.length > 2 || cluster.apps.list.length < 1) ? [ cluster.apps.list.length ] : [cluster.apps.list[0].name + (cluster.apps.list.length == 2 ? ', '+cluster.apps.list[1].name : '')])
        };
      };

      var serverListProcessor = function(cluster) {
        return {
          template : i18n.HOSTS_SERVERS_RUNNING_OF_TOTAL,
          unknown : cluster.servers.unknown,
          up : cluster.servers.up,
          down : cluster.servers.down
        };
      };

      allX.commonUpdateCard(collectionView, collectionView.id, classes, cluster, openSingleClusterView,
          appListProcessor, serverListProcessor);
    },

    __updateHostCard : function(host) {
      var collectionView = this;

      var classes = 'cardBorderContainerHost cardboardContainerMobileWidth';

      var openSingleHostView = function() {
        viewFactory.openView(host);
      };

      var appListProcessor = null;

      var serverListProcessor = function(host) {
        // TODO: Does this need to be called with host.getServers() ??
        var returnedObj = {
            template : i18n.HOSTS_SERVERS_RUNNING_OF_TOTAL,
            unknown : host.servers.unknown,
            up : host.servers.up,
            down : host.servers.down
        };
        return returnedObj;
      };

      allX.commonUpdateCard(collectionView, collectionView.id, classes, host, openSingleHostView, appListProcessor,
          serverListProcessor);
    },

    __updateServerCard : function(server) {
      var collectionView = this;
      var classes = 'cardBorderContainerServer cardboardContainerMobileWidth';

      var openSingleServerView = function() {
        viewFactory.openView(server);
      };

      var appListProcessor = function(server) {
        // Grab all of the names of this server's applications.
        return {
          template : ((server.apps.list.length > 2 || server.apps.list.length < 1) ? i18n.APPS_LIST : i18n.EMPTY_MESSAGE),
          inserts : ((server.apps.list.length > 2 || server.apps.list.length < 1) ? [ server.apps.list.length ] : [server.apps.list[0].name + (server.apps.list.length == 2 ? ', ' + server.apps.list[1].name : '')])
        };
      };
      allX.commonUpdateCard(collectionView, collectionView.id, classes, server, openSingleServerView, appListProcessor);
    },

    __updateRuntimeCard : function(runtime) {
      var collectionView = this;

      var classes = 'cardBorderContainerRuntime cardboardContainerMobileWidth';

      var openRuntimeView = function() {
        viewFactory.openView(runtime);
      };

      var appListProcessor = null;

      var serverListProcessor = null;
      runtime.getServers().then(function(servers){
        // Always create the graph processor. Magic in Card.js around line 300 will take
        // it out when necessary
        serverListProcessor = function(runtime) {
          var returnedObj = {
              template : i18n.HOSTS_SERVERS_RUNNING_OF_TOTAL,
              unknown : servers.unknown,
              up : servers.up,
              down : servers.down
          };
          return returnedObj;
        };
        allX.commonUpdateCard(collectionView, collectionView.id, classes, runtime, openRuntimeView, appListProcessor,
            serverListProcessor);
      });
    }
  });

  return {

    /**
     * Opens the view for the given resource collection. If the view for the resource collection has never been created, then the view will
     * be initialized.
     * 
     * @param resourceCollection
     *          Used to create the contents and create a unique instance based on the collection's ID. The id ensures uniqueness, e.g.
     *          applications, appsOnServer(tuple), appsOnCluster(clusterName).
     * @param defaultFilter
     *          If set, then the view is launched with the specified filter applied. Otherwise, the default filter is 'everything'.
     * @param parentPane
     *          The parentPane is only used during view creation. This is used to create a 'sub-collectionView' of a single resource view.
     *          If this value is set, then the created view is not added to the breadcrumb but is added to the parentParent.
     * @see __getOrCreateViewHolder for more on the view ID and resource collection ID.
     */
    openCollectionView : function(resourceCollection, defaultFilter, parentPane, filter) {
      if (!defaultFilter) {
        // set the default
        defaultFilter = "Total";
      }
      var displayFilters = true;
      if (filter === false) {
        displayFilters = false;
      }
      var viewId = ID.dashDelimit(ID.getCollectionView(), resourceCollection.id);
      var viewHolder = this.__getOrCreateViewHolder(viewId, resourceCollection, parentPane, displayFilters);
      viewHolder.view.buildOrSwitchToView(defaultFilter, parentPane, resourceCollection);
      var breadcrumbPane = registry.byId(ID.getBreadcrumbPane()); /**see if it makes sense to use this widget from where it's created above. */
      breadcrumbPane.setBreadcrumb(resourceCollection);
      if (displayFilters) {
        viewHolder.view.filterBar.focusCurrentFilter();
      } else {
        domClass.add(registry.byId(viewHolder.view.filterBar.id + 'filterBar').domNode, "filterBarHidden");
        domClass.remove(registry.byId(viewHolder.view.filterBar.id + 'filterBar').domNode, "filterBar");
        registry.byId(viewHolder.view.filterBar.id).set("style", "min-height:0px;border-bottom:0px;");
      }
    },

    /**
     * dojo Memory, used to store created views.
     */
    __collectionViews : new Memory({}),

    /**
     * Gets or creates a view for the given viewId.
     * 
     * @param viewId
     *          The unique viewId to obtain or create. The viewId implies scoping, e.g. 'collectionView-applications' is used for a new
     *          breadcrumb element which is for all applications in the collective. An ID like 'collectionView-appsOnServer(myServer)'
     *          implies a scoped view which will have a parentPane. We do not need to include the parentPane ID in the view as the resource
     *          collection IDs are already scoped.
     * @param
     */
    __getOrCreateViewHolder : function(viewId, resourceCollection, parentPane, displayFilters) {
      var viewHolder = this.__collectionViews.get(viewId);
      if (viewHolder === undefined) {
        viewHolder = {
            id : viewId,
            view : new _InternalCollectionViewObj(viewId, resourceCollection, displayFilters)
        };
        this.__collectionViews.put(viewHolder);
      }
      return viewHolder;
    }
  };

});