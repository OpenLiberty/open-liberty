/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
define([ "dojo/_base/declare", "dojo/_base/window", "dojo/_base/lang", "dojo/parser", "dojo/dom-attr", "dojo/dom-construct", "dojo/dom", "dojo/on",
         "dojo/Stateful", "dojo/store/Memory", "dojo/store/Observable", "dijit/registry", "dijit/form/Button", "dijit/layout/ContentPane",
         "dijit/layout/BorderContainer", "jsExplore/widgets/ResourceFilter", "js/common/platform", "dojo/i18n!../nls/explorerMessages",
         "dojo/dom-class", "dojo/keys", "dijit/focus", "js/common/imgUtils", 'jsExplore/widgets/shared/ActionBar', 'jsExplore/utils/ID', "dojo/domReady!" ], 
         function(declare, window, lang, parser, domAttr, domConstruct, dom, on, Stateful, Memory,
             Observable, registry, Button, ContentPane, BorderContainer, ResourceFilter, 
             platform, i18n, domClass, keys, focusUtil, imgUtils, ActionBar, ID ) {

  return declare("FilterBar", [ ContentPane ], {

    constructor : function() {
      // Define these resources inside the constructor so they become per-instance objects
      this.resourceFilters = new Memory({});
      this.actionHandlers = [];
      this.selectCount = 0;
      this.selectAllButton = null;
      this.selectNoneButton = null;
    },
    baseClass : "filterBar",
    doLayout : false,
    postCreate : function() {
      this.inherited(arguments);
      this.buildTabBar();

    },

    buildTabBar : function() {
      var me = this;
      if (!me.queryFn) {
        me.queryFn = {};
      }
      me.watch("currentFilter", function(name, oldValue, newValue) {
        me.resourceFilters.query().forEach(function(singleFilter) {
          if (singleFilter.id == newValue) {
            singleFilter.obj.select();
          } else {
            singleFilter.obj.deselect();
          }
          
          me.actionBar.setCurrentFilter(newValue);
        });
      });
      me.filterBar = new ContentPane({
        id : this.id + "filterBar"
      });
      me.addChild(me.filterBar);
      domClass.add(me.filterBar.domNode, "filterBar");
      me.page.own(me.filterBar);
      me.actionBar = new ActionBar({
        id : ID.dashDelimit(this.id, ID.getActionBar()),
        //page : this.parentPane,
        //resourceType : this.resourceCollection.type,
        resourceType: this.resourceType,
        //actions : this.actionList,
        actions: null,
        //parent : this,
        parent: this,
        resourceFilters: this.resourceFilters,
        collectionView: this.parent,
        //resetActions : this.resetActions
        resetActions: null
      });

      me.page.own(me.actionBar);
      me.filterPane = new ContentPane({
        id : this.id + "filterPane",
        title : me.resourceType,
        content : "",
        doLayout : false,
        style: platform.isPhone() ? "width: 100%" : ''
      });
      me.filterBar.addChild(me.filterPane);
      me.page.own(me.filterPane);

      // no edit button for runtimes
      if (this.resourceType !== 'runtimesOnHost' && this.resourceType !== 'runtimes') {
        me.editButton = new ContentPane({
          id : this.id + "editButton",
          title : me.resourceType,
          content : (globalIsAdmin === true ? imgUtils.getSVGSmall('menu-action') : imgUtils.getSVGSmall('menu-action-disabled')),
          baseClass : "editButtonsAll",
          tabindex : '0',
          role: 'button',
          'aria-label': (globalIsAdmin === false ? i18n.ACTION_DISABLED_FOR_USER : i18n.ACTIONS),
          'aria-pressed': 'false',
          'aria-disabled': (globalIsAdmin === true ? 'false' : 'true'),
          clicked : false,
          onClick: function(){
            if (globalIsAdmin === false) {
              console.log('Action bar is disabled for users without the Administrator role as they cannot take actions');
              return;
            }
            if(!me.clicked){
              domClass.add(me.editButton.domNode, "editButtonsClicked");
              me.clicked = !me.clicked;
              me.editButton.setAttribute('aria-pressed', 'true');
            }
            else
            {
              domClass.remove(me.editButton.domNode, "editButtonsClicked");
              me.clicked = !me.clicked;
              me.editButton.setAttribute('aria-pressed', 'false');
            } 
          }
        //style: "max-height: 90px"
        });
        domAttr.set(me.editButton.domNode, "title", (globalIsAdmin === false ? i18n.ACTION_DISABLED_FOR_USER : i18n.ACTIONS));
        me.page.own(me.editButton);
        me.actionBar.watch("selectMode", function(name, oldValue, newValue) {
          me.actionHandlers.forEach(function(actionHandler) {
            actionHandler.set("available", newValue);
          });
        });
      }
      // If the user has passed in actions, use those instead of the defaults
      if (me.actionBar.actions && me.resourceType !== 'runtimesOnHost' && me.resourceType !== 'runtimes') {
        var actionImg = (globalIsAdmin === true ? imgUtils.getSVGSmall('menu-action') : imgUtils.getSVGSmall('menu-action-disabled'));
        me.editButton.set("content", actionImg);
        if (globalIsAdmin) {
          // Admins can process actions from the filter bar....
          me.editButtonClickHandler = on(me.editButton, "click", function() {
            me.actionBar.processActionBar();
          });            
        } else {
          // non-Admins (reader role) will not be allowed to access the menu bar action items
          domClass.add(me.editButton.domNode, "editButtonsDisabled");
        }

        //var isWebKit = /WebKit/.test(navigator.userAgent);
        on(me.editButton, "keydown", function(evt) {
          if (evt.keyCode === keys.ENTER || evt.keyCode === keys.SPACE) {
            // a workaround for browser that fires both keydown and click events when a key is entered
            if (me.editButtonClickHandler !== undefined) {
              me.editButtonClickHandler.remove();
              me.editButtonClickHandler = undefined;
            }
            me.actionBar.processActionBar();
          }
        });

        on(me.editButton.domNode, "focus", function() {
          domClass.add(me.editButton.domNode, "editButtonsFocused");
        });
        on(me.editButton.domNode, "focusout", function() {
          domClass.remove(me.editButton.domNode, "editButtonsFocused");
          if (me.editButtonClickHandler === undefined) {
            me.editButtonClickHandler = on(me.editButton.domNode, "click", function() {
              me.actionBar.processActionBar();
            });
          }
        });
        on(me.editButton.domNode, "mouseover", function() {
          if (me.editButtonClickHandler === undefined) {
            me.editButtonClickHandler = on(me.editButton.domNode, "click", function() {
              me.actionBar.processActionBar();
            });
          }
        });

        me.addChild(me.editButton);
        me.addChild(me.actionBar);
      } else {
        me.addChild(me.actionBar);
      }
      me.set("style", "height: auto");
      me.resize();
    },

    addFilter : function(filter) {
      var me = this;
      //See if the filter exists
      var resourceFilter = null;
      var countValue = (filter.count == undefined) ? 0 : filter.count;
      var resourceFilterObj = this.resourceFilters.get(filter.id);
      if (resourceFilterObj == undefined) {
        resourceFilter = new ResourceFilter({
          id : this.id + filter.id + "resourceFilter",
          number : countValue,
          status : filter.label,
          icon : filter.icon,
          baseClass : "statusContentPaneDeselected",
          tabindex : '0',
          selected : false,
          alt : filter.label
        });
        me.page.own(resourceFilter);
        if (!filter.hideIfZero || filter.count > 0) {
          this.filterPane.addChild(resourceFilter);
        }
        resourceFilterObj = {
            id : filter.id,
            obj : resourceFilter,
            filter : filter
        };
        this.resourceFilters.put(resourceFilterObj);
        on(resourceFilter, "click", function() {
          // Update the valid cardList
          me.set("currentFilter", filter.id);
        });
        on(resourceFilter, "keydown", function(evt) {
          if (evt.keyCode === keys.ENTER || evt.keyCode === keys.SPACE) {
            // Update the valid cardList
            me.set("currentFilter", filter.id);
          }
        });
        on(resourceFilter, "focus", function() {
          //resourceFilter.set("style", "background-color: #ffffff;");
          //resourceFilter.set("style", "border: 2px solid ")
          domClass.add(resourceFilter.domNode, "statusContentPaneFocused");
        });
        on(resourceFilter, "focusout", function() {
          //resourceFilter.set("style", "background-color: #F8F8F7;");
          domClass.remove(resourceFilter.domNode, "statusContentPaneFocused");
        });
        if (this.currentFilter == null) {
          this.set("currentFilter", filter.id);
          resourceFilter.select();
        }
      }
      resourceFilter = resourceFilterObj.obj;
      resourceFilter.changeCount(countValue);
    },

    updateFilterValues : function(resourceCollection, resources) {
      var me = this;

      me.resourceFilters.query().forEach(function(filter) {
        var newCount = 0;
        if (resourceCollection.hasOwnProperty('up')) {
          // If resource collections keep tallies, use them as its less expensive to iterate through
          if (filter.id === 'Total') {
            newCount = resourceCollection.up + resourceCollection.down + resourceCollection.unknown;
            if (resourceCollection.partial) {
              newCount += resourceCollection.partial;
            }
            if (resourceCollection.empty) {
              newCount += resourceCollection.empty;
            }
          } else if (filter.id === 'STARTED') {
            newCount = resourceCollection.up;
            if (resourceCollection.partial) {
              newCount += resourceCollection.partial;
            }
          } else if (filter.id === 'PARTIALLY_STARTED') {
            // We need to check each resource here. If the resource has a scaling policy, we don't actually include it in the partial count
            // This kind of sucks because our performance is lame then
            resources.forEach(function(resource) {
              // Always add this to the Total
              // Add this if there are alerts
              // Add it to a related filter
              if (me.resourceIsInCurrentFilter(resource, filter.id)) {
                newCount++;
              }
            });
          } else if (filter.id === 'STOPPED') {
            newCount = resourceCollection.down;
            if (resourceCollection.empty) {
              newCount += resourceCollection.empty;
            }
          } else if (filter.id === 'Alert') {
            // For resource collections that don't keep tallies, query each item in the list
            resources.forEach(function(resource) {
              // Always add this to the Total
              // Add this if there are alerts
              // Add it to a related filter
              if (me.resourceIsInCurrentFilter(resource, filter.id)) {
                newCount++;
              }
            });
          }
        } else {
          // For resource collections that don't keep tallies, query each item in the list
          resourceCollection.list.forEach(function(resource) {
            // Always add this to the Total
            // Add this if there are alerts
            // Add it to a related filter
            if (me.resourceIsInCurrentFilter(resource, filter.id)) {
              newCount++;
            }
          });
        }

        filter.obj.changeCount(newCount);
        if (filter.filter.hideIfZero) {
          // This filter is only visible 
          if (filter.obj.number < 1 && me.filterPane.getIndexOfChild(filter.obj) != -1) {
            // This is displayed but shouldn't be
            me.filterPane.removeChild(filter.obj);
            // Only go to total if this is the Alert filter and it is now empty
            if (me.get("currentFilter") === "Alert") {
              me.set("currentFilter", "Total");
            }
          } else if (filter.obj.number > 0 && me.filterPane.getIndexOfChild(filter.obj) < 0) {
            // This is not displayed but should be
            me.filterPane.addChild(filter.obj);
            filter.obj.resize();
          }
        }
      });
      // Need to resize the filterPane in case a previously invisible button is now visible.
      // And for some reason the height has to be set to auto just before the call and not when
      // the filterPane is created.
      me.filterPane.set("style", "height: auto");
      me.filterPane.resize();
    },

    resourceIsInCurrentFilter : function(resource, filterValue) {
      //console.log("resource to check to include in filter or not: ", resource);
      //console.log("filterValue: " + filterValue + "; resourceType: " + this.resourceType);
      if (filterValue == "Total") {
        return true;
      } else if (filterValue == "Alert") {
        // Does the resource have any alerts?
        return (resource.alerts ? resource.alerts.count > 0 : false);
      } else if (resource.state == "PARTIALLY STARTED" || resource.state == "PARTIALLY_STARTED") {
        // partially started filter is included for apps, clusters, and hosts, not servers 
        // Note: cluster has PARTIALLY STARTED state whereas app and host have PARTIALLY_STARTED state
        if (filterValue == "STARTED") {
          // with the new design, include partially to running filter
          return true;
        } else if (filterValue == "PARTIALLY_STARTED") {
          // The design states that a resource with an enabled scaling policy (e.g. a dynamic cluster)
          // is not considered partially running. It is either running, or not.
          if (resource.scalingPolicyEnabled) {
            return false;
          } else {
            // If the resource is PARTIALLY_RUNNING but does not have an enabled scaling policy, then
            // treat it as partially running
            return true;
          }
        } else { // for other filter values
          return false;
        }
      } else {
        var belongsInStoppedView = resource.state === "STOPPING" || resource.state === "STOPPED" || resource.state === "STARTING";
        if(filterValue == "STOPPED" && belongsInStoppedView) {
          // We consider all the states in variable belongsInStoppedView as +1 to down resource count.  
          // This check will make the filtering of resources behave the same way. See State.java isDown()
          // for reference.
          return true;
        }
        return resource.state == filterValue;
      }
    },

    focusCurrentFilter : function() {
      focusUtil.focus(dom.byId(this.id + this.get("currentFilter") + "resourceFilter"));
    }

  });
});
