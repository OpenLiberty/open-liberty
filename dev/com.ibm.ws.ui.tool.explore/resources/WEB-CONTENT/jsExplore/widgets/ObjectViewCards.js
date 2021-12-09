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
/* 
 * create a server type card using template
 */
define(["dojo/_base/declare", 
        "dojo/_base/lang", 
        'dijit/_WidgetBase', 
        'dijit/_TemplatedMixin', 
        'dijit/_WidgetsInTemplateMixin',
        'dojo/text!./templates/ObjectViewCards.html', 
        'dojo/store/Memory',
        'dojo/has',
        'dojo/keys',
        "dojox/string/BidiComplex",
        "dojo/i18n!../nls/explorerMessages",
        "dojo/dom",
        "dojo/dom-construct",
        "dijit/layout/BorderContainer",
        "dijit/layout/ContentPane",
        "dijit/registry",
        'jsExplore/resources/utils', 
        "dojo/on",
        'jsExplore/views/viewFactory',
        'jsExplore/resources/Observer'
        ], 
        function(declare, 
            lang, 
            WidgetBase,
            _TemplatedMixin,
            _WidgetsInTemplateMixin,
            template,
            Memory,
            has,
            keys,
            BidiComplex,
            i18n,
            dom,
            domConstruct,
            BorderContainer,
            ContentPane,
            registry,
            utils,
            on,
            viewFactory,
            Observer
        ) {

  return declare('ObjectViewCards', [ WidgetBase, _TemplatedMixin, _WidgetsInTemplateMixin, Observer ], {
    id : 'objectView-cards',
    collection: null,
    widgetsInTemplate: true,
    region: 'top', // Set the region, required by the template parent
    templateString: template,
    resourceLabelPaneType: '',
    resourceIcon: '',
    parentResource: '',
    cardMemory: null,

    /**
     * Subscribes this widget as an observer of the collection
     */
    constructor: function(params) {
      var collection = params[0];
      this.collection = collection;
      this.id= 'objectView-cards' + this.collection.id;
      this.resourceLabelPaneType = params[1];
      this.resourceIcon = params[2];
      if(params[3]){
        this.parentResource = params[3]; // if Application object view the parentResource is the same as the resource
      } else {
        this.parentResource=collection;
      }
      this.parentsResourceId = this.parentResource.id;
      this.cardMemory = new Memory();
      this.collection.subscribe(this);
    },

    /**
     * Common method to re-draw the cards based on the collection type
     */
    __redrawCards: function() {
      var type = this.collection.type;
      if (type === 'appsOnServer' || type === 'appInstancesByCluster') {
        this.__updateCards(this.collection, this.__addApplicationInstCard);
      } else if(type === 'appsOnCluster') {
        this.__updateCards(this.collection, this.__addAppOnClusterCard);
      } else if(type === 'serversOnHost' || type === 'serversOnRuntime' ) {
        this.__updateCards(this.collection, this.__addServerCard);
      }
    },

    /**
     * Triggered when the widget is ready to render.
     */
    postCreate: function() {
      this.__redrawCards();
    },

    /**
     * Unsubsribe this observer collection 
     */
    destroy: function() {
      this.inherited(arguments);
      if (this.collection) {
        this.collection.unsubscribe(this);   
      }
    }, 
    
    onAppsListChange: function() {
      console.log("onAppsListChange event received!");
      this.__redrawCards();
    },
    
    onServersListChange: function() {
      console.log("onServersListChange event received!");
      this.__redrawCards();
    },

    /**
     * Adds a card to object view, and stores a reference to it in the the cardMemory.
     * 
     * @param resource
     * @param card
     */
    __addCard: function(resource, card) {
      this.cardPane.addChild(card);
      card.resize();

      this.cardMemory.put({
        id : resource.id,
        card : card,
        display : true
      });
    },

    /**
     * Adds an ApplicationInstance card to the object view.
     * This method will also create a cardMemoryObject for this card and store it in the cardMemory.
     * 
     * @param singleServer
     */
    __addApplicationInstCard: function(appOnServer) {
      var onClickFn = function(){};
      if (!utils.isStandalone()) {
        onClickFn = function() {
          viewFactory.openView(appOnServer);
        };
      }
      var onKeyDownFn = function() {};
      if (!utils.isStandalone()) {
        onKeyDownFn = function(evt) {
          if (evt.keyCode === keys.ENTER || evt.keyCode === keys.SPACE) {
            viewFactory.openView(appOnServer);
          }
        };
      }

      var card = new CardBorderContainer({
        id : this.parentsResourceId + '-Card-' + appOnServer.id,
        label : appOnServer.name,
        server : appOnServer.server,
        resource : appOnServer,
        resourceType : 'Application Inst',

        onclick : onClickFn,
        onkeydown : onKeyDownFn,
        baseClass : 'cardBorderContainerSingleAppInst'
      });

      this.__addCard(appOnServer, card);
    },

    /**
     * Adds an AppOnCluster card to the object view.
     * This method will also create a cardMemoryObject for this card and store it in the cardMemory.
     * 
     * @param appOnCluster resource
     */
    __addAppOnClusterCard: function(appOnCluster) {
      var card = new CardBorderContainer({
        id : this.parentsResourceId + '-Card-' + appOnCluster.id,
        label : appOnCluster.name,
        resource : appOnCluster,
        cluster : appOnCluster.cluster,
        resourceType : 'AppOnCluster',
        onclick : function() {
          viewFactory.openView(appOnCluster);
        },
        onkeydown : function(evt) {
          if (evt.keyCode === keys.ENTER || evt.keyCode === keys.SPACE) {
            viewFactory.openView(appOnCluster);
          }
        },
        appListProcessor: function(application) {
          var totalInstances = application.up + application.down + application.unknown;
          return {
            template : i18n.APPS_INSTANCES_RUNNING_OF_TOTAL,
            inserts : [application.up, totalInstances]
          };
        },
        baseClass : 'cardBorderContainerClusterApp'
      });

      this.__addCard(appOnCluster, card);
    },

    /**
     * Adds a Server card to the object view.
     * This method will also create a cardMemoryObject for this card and store it in the cardMemory.
     * 
     * @param singleServer
     */
    __addServerCard: function(singleServer) {
      var card = new CardBorderContainer({
        id : this.parentsResourceId + '-Card-' + singleServer.id,
        label : singleServer.name,
        resource : singleServer,
        resourceType : 'Server',
        onclick : function() {
          viewFactory.openView(singleServer);
        },
        onkeydown : function(evt) {
          if (evt.keyCode === keys.ENTER || evt.keyCode === keys.SPACE) {
            viewFactory.openView(singleServer);
          }
        },
        appListProcessor : function() {
          var nameList = [];
          singleServer.apps.list.forEach(function(app) {
            nameList.push(app.name);
          });
          // Test the length of the array. If its too small or too big change the array to a
          // single element thats the length (i.e. the number of resources)
          return {
            template : (nameList.length > 2 || nameList.length < 1 ? i18n.APPS_LIST : i18n.EMPTY_MESSAGE),
            inserts : (nameList.length > 2 || nameList.length < 1 ? [ nameList.length ] : nameList)
          };
        },
        baseClass : 'cardBorderContainerServer'
      }); // end of new card

      this.__addCard(singleServer, card);
    },

    /**
     * Common logic which knows how to, given a resource collection, create and remove cards.
     * 
     * @param collection
     * @param createCardMethod
     */
    __updateCards: function(collection, createCardMethod) {
      var cardMemory = this.cardMemory;

      // Mark all cards as not displayed.
      cardMemory.query().forEach(function(cardMemoryObject) {
        cardMemoryObject.display = false;
      });

      // Mark all cards for all current resources as displayed
      // Any cards which do not yet exist will be created
      for(var i = 0; i < collection.list.length; i++) {
        var resource = collection.list[i];
        var cardMemoryObject = cardMemory.get(resource.id);
        if (!cardMemoryObject) {
          createCardMethod.call(this, resource);
          cardMemoryObject = this.cardPane.get(resource.id);
        }
        if (cardMemoryObject) {
          cardMemoryObject.display = true;
          cardMemory.put(cardMemoryObject);
        }
      }

      // Delete all cards which are no longer displayed
      cardMemory.query({
        display : false
      }).forEach(function(cardMemoryObject) {
        cardMemory.remove(cardMemoryObject.id);
        cardMemoryObject.card.destroy();
      });
    },

    /**
     * Triggers when the underlying collection's list changes. This is the trigger to re-draw the cards.
     */
    onListChange: function() {
      // deprecated.  Try using onAppsListChange and onServersListChange.
    }

  });
});
