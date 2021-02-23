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
define([ 'dijit/registry', 'dijit/layout/ContentPane', 'dojo/keys', 'dojo/on', 'dojo/_base/window', 'jsExplore/utils/ID' ], 
    function(registry, ContentPane, keys, on, window, ID) {

  /**
   * allView - the allServers, allApplications, etc view
   * resourceCollection - the collection for the view
   * updateCards - function to update the cards, i.e. updateCards(me, resourceCollection)
   * breadcrumDOM - dom.byId("breadcrumbContainer-id")
   * breadcrumWidget - registry.byId("breadcrumbContainer-id")
   * requiredBreadcrumChildID - 'resourcePageALL_SERVERS'
   */
  function __initializeCardChunking(allView, resourceCollection, updateCards, breadcrumDOM, breadcrumWidget, requiredBreadcrumChildID) {

    allView.scrollOffset = 0;
    allView.built_cards = 0;
    allView.visible_cards = 0;

    console.log("clientHeight:" + breadcrumDOM.clientHeight);

    allView.BUILD_CHUNKS = (~~(((breadcrumDOM.clientHeight-100)/150)*(breadcrumDOM.clientWidth/250))) * 2 + 10;
    console.log("initial buildchunks: " + allView.BUILD_CHUNKS);

    allView.max_built_cards = allView.BUILD_CHUNKS;
    console.log("initial maxBuiltCards:" + allView.max_built_cards);

    on(breadcrumWidget, "scroll", function(evt) {
      if (breadcrumWidget.selectedChildWidget.id === requiredBreadcrumChildID) {
        // if scrollHeight-scrollTop < clientHeight*2 meaning less than 2 screens of data
        console.log("compare " + (evt.target.scrollHeight-evt.target.scrollTop-100) + " to " + evt.target.clientHeight*2);
        if (evt.target.scrollTop > allView.scrollOffset && ((evt.target.scrollHeight-evt.target.scrollTop-100) <= evt.target.clientHeight*2)) {
          // scrolled down so build the next set
          allView.scrollOffset = evt.target.scrollTop;
          allView.max_built_cards += allView.BUILD_CHUNKS;
          console.log("new maxBuiltCards:" + allView.max_built_cards);
          updateCards(allView, resourceCollection);
        }
      }
    });

    on(window.global, 'resize', function(evt) {
      var newChunks = (~~(((breadcrumDOM.clientHeight-100)/150)*(breadcrumDOM.clientWidth/250))) * 2;
      newChunks = newChunks > 50 ? 50 : newChunks;
      var chunksDisplayed = ~~(allView.max_built_cards/allView.BUILD_CHUNKS);
      if (newChunks > allView.BUILD_CHUNKS) {
        // add some cards
        allView.max_built_cards = chunksDisplayed * newChunks;
        console.log("new maxBuiltCards:" + allView.max_built_cards);
        allView.scrollOffset = breadcrumDOM.scrollTop;
        updateCards(allView, resourceCollection);
      } else {
        // chunk size went down
        if (allView.built_cards < chunksDisplayed*newChunks) {
          allView.max_built_cards = chunksDisplayed * newChunks;
          console.log("new maxBuiltCards:" + allView.max_built_cards);
        }
      }
      allView.BUILD_CHUNKS = newChunks;
      console.log("resized window so recalculate buildchunks:" + allView.BUILD_CHUNKS);
    });
  }
  
  /**
   * Common create card logic for allX resource views.
   */
  function __commonUpdateCard(owningView, idTemplate, classes, cardResource, openView, appListProcessor, serverListProcessor) {
    var cardObj = owningView.cardList.get(cardResource.id);
    if (cardObj === undefined) {
    var notMaxBuilt = owningView.built_cards < owningView.max_built_cards;
    var notFilterBuilt = owningView.filterBar.resourceIsInCurrentFilter(cardResource, owningView.filterBar.get("currentFilter")) && owningView.filterBar.get("currentFilter") !== "Total";
    // figure out chunking for filtered views
    if (notFilterBuilt) {
      if (owningView.visible_cards >= owningView.max_built_cards) {
      notFilterBuilt = false;
      }
    }
      if (notMaxBuilt || notFilterBuilt) {
        owningView.built_cards++;   
        cardObj = __createCard(owningView, idTemplate, classes, cardResource, openView, appListProcessor, serverListProcessor);
      }
    }
    if (cardObj) {
      // Update the held state

      cardObj.state = cardResource.state;
      cardObj.maintenanceMode = cardResource.maintenanceMode;
      var prevVis = cardObj.visible;
      if (owningView.filterBar.resourceIsInCurrentFilter(cardResource, owningView.filterBar.get("currentFilter"))) {
        cardObj.visible = true;
        owningView.visible_cards++;
      } else {
        cardObj.visible = false;
        owningView.visible_cards--;
      }

      // Put the card in the list (update/create), this may seem weird but its used by the observables to notify a change
      owningView.cardList.put(cardObj);
      if (prevVis != cardObj.visible) {
        // TODO: The deeply nested nature of this update is very ugly. We need to consider re-writing cards entirely
        cardObj.card.card.update(); // Force a re-draw now that we're supposed to be visible
      }
    }
  }

  function __createCard(owningView, idTemplate, classes, cardResource, openView, appListProcessor, serverListProcessor) {
    var toggleState = function(newValue) {
      var res = owningView.cardList.get(cardResource.id);
      if (newValue) {
        res.selected = newValue;
      } else {
        res.selected = !res.selected;
      }
      // Put the card in the list (update/create), this may seem weird but its used by the observables to notify a change
      owningView.cardList.put(res);
      res.card.setSelected(res.selected);
      if (owningView.filterBar && owningView.filterBar.actionBar) {
        if (res.selected) {
          owningView.filterBar.actionBar.createSelectedResourceObserver(cardResource);
        } else {
          owningView.filterBar.actionBar.removeSelectedResourceObserver(cardResource);
        }
      }
    };

    var onCardSelect = function() {
      if (owningView.filterBar.actionBar.get("selectMode")) {
        toggleState();
      } else {
        openView();
      }
    };
    
    var onCardStateChange = function() {
      __commonUpdateCard(owningView, idTemplate, classes, cardResource, openView, appListProcessor, serverListProcessor);
    };
    
    var card = new CardBorderContainer({
      id: ID.dashDelimit(idTemplate, ID.getCard(), cardResource.id),
      doLayout : false,
      label: cardResource.name,
      resource: cardResource,
      "class": classes,
      onclick: function() {
        onCardSelect();
      },
      onkeydown: function(evt) {
        if (evt.keyCode === keys.ENTER || evt.keyCode === keys.SPACE) {
          onCardSelect();
        }
      },
      toggleState: toggleState,
      appListProcessor: appListProcessor,
      serverListProcessor: serverListProcessor,
      stackPane: owningView.stackPane
    });
    // This update implies that the card has had its value changed (probably via a call to the backend). So we need to update our
    // stats
    var cardObj = {
        id : cardResource.id,
        card : card,
        state : cardResource.state,
        onStateChange: function(state) {
          // This could be a card for a stateless object
          if (state !== cardObj.state) {
            cardObj.state = state;
            // need to update the visibility of the card with the current filter when its state changes
            onCardStateChange();
          }
        },
        destroy: function(){
          cardResource.unsubscribe(this);
        },
        onDestroyed: function() {
          this.destroy();
          console.log("cardObj id ", cardObj.id);
          owningView.removeCard(cardObj.id);
        }
    };
    cardResource.subscribe(cardObj);
    return cardObj;
  }

  return {

    initializeCardChunking: __initializeCardChunking,
    
    commonUpdateCard: __commonUpdateCard,

    createContentPane: function(paneTitle, id, title) {
      return new ContentPane({
        id : ID.getResourcePage() + id,
        title : paneTitle,
        headerTitle : title,
        content : ' ',
        baseClass : 'topResourceContentPane'
      });
    },

    /**
     * Gets or creates a tab.
     */
    getOrCreateTab: function(tabList, tabId, TabObject) {
      var currentTab = tabList.get(tabId);
      if (currentTab == undefined) {
        currentTab = {
            id : tabId,
            tab : new TabObject()
        };
        tabList.put(currentTab);
      }
      return currentTab;
    },

    setEventActions: function(actionBarButton, actionButton) {
      on(actionBarButton.domNode, "keydown", function(evt) {
        if (evt.keyCode === 40) { // for down arrow key
          actionButton.focus();
        }
      });
      on(actionButton.domNode, "focus", function() {
        domClass.add(actionButton.domNode, "actionBarButtonFocused");
      });
      on(actionButton.domNode, "focusout", function() {
        domClass.remove(actionButton.domNode, "actionBarButtonFocused");
      });
    }

  };
});