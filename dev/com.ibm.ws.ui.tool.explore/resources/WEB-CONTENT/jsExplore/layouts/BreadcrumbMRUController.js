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
define([ "dojo/_base/declare", // dojo_delcare
"dojo/dom-class", // dojo_class
"dojo/dom-geometry", // dojo_geo
"dojo/_base/lang", // dojo_lang
"dijit/registry", // dijit_registry
"dojo/_base/array", // dojo_array
"dojo/dom-construct", // dojo_construct
"dojo/dom-attr", "dojo/dom-style",// dojo_domattr,
'js/common/tr', // tr
"dojo/query", "dojo/dom-class", "dijit/layout/StackController", // dijit_layout_StackController
"dojo/i18n!jsExplore/nls/explorerMessages", "dojo/dom-construct", "dijit/_Container", "dijit/layout/StackContainer",
    "dojo/keys", "dojo/has", "dijit/focus", "dijit/registry", "dojo/_base/array", 'dojo/hash', 'jsExplore/resources/viewToHash', 'jsExplore/utils/ID',
    'jsExplore/views/viewFactory' ], function(dojo_declare, dojo_class, dojo_geo, dojo_lang, dijit_registry, dojo_array, dojo_construct,
    dojo_domattr, domStyle, tr, query, domClass, dijit_layout_StackController, i18n, domConstruct, _Container, StackContainer, keys,
    has, focus, registry, array, hash, viewToHash, ID, viewFactory) {
  /**
   * @name idx.layout.BreadcrumbController
   * @class Bread crumb controller
   * @augments dijit.layout.StackControler
   */
  var BreadcrumbMRUController = dojo_declare("js.layouts.BreadcrumbMRUController", dijit_layout_StackController,
  /** @lends idx.layout.BreadcrumbController# */
  {

    baseClass : "breadcrumbController",
    updateHash: true,

    /**
     * @name idx.layout.BreadcrumbController
     * @class A stack controller that behaves like a breadcrumb trail. When children are added to the associated StackContainer, it adds
     *        another breadcrumb. When a breadcrumb is clicked, it removes all breadcrumbs to the right of it, along with their associated
     *        screens.
     * @function
     * @augments dijit.layout.StackController
     * 
     */
    constructor : function(args, node) {

    },

    /**
     * postCreate
     */
    postCreate : function() {
      this.inherited(arguments);
      dojo_domattr.set(this.domNode, "id", this.id);
    },

    /**
     * String used to separate the breadcrumb links, only actived in high contract mode.
     * 
     * @type String
     * @default >
     */
    breadcrumbSeparator : "&gt", // &gt;

    keepFirstChild : false,


    buildRendering : function() {
      this.inherited(arguments);
      dojo_class.add(this.domNode, this.idxBaseClass);
    },

    /**
     * Override - Called after StackContainer has finished initializing
     * 
     * @private
     */
    onStartup : function(/* Object */info) {
      dojo_array.forEach(info.children, this.onAddChild, this);
    },

    resize : function(changeSize, resultSize) {
      this.inherited(arguments);
      if (changeSize)
        dojo_geo.getMarginBox(this.domNode, changeSize);
    },

    /**
     * Override
     * 
     * @private
     */
    onAddChild : function(/* dijit._Widget */page, /* Integer? */insertIndex) {
      try {
        dijit_registry.byId(this.containerId).selectChild(page);
      } catch (e) {
        tr.ffdc('Error occurred while selecting page.', e, page);
      }
    },

    /**
     * Override
     * 
     * @private
     */
    onRemoveChild : function(/* dijit._Widget */page) {
        if (this._tabPage === page) {
          this._tabPage = null;
        }
      this.inherited(arguments);
    },

    /**
     * Override
     * 
     * @private
     */
    onSelectChild : function(/* dijit._Widget */page) {
      console.debug('Breadcrumb onSelectChild page', page.headerTitle);

      // if the child is selected due to the re-ordering, should not remove and
      // re-add the child element as it affects the existing ordering of the elements in
      // the breadcrumb container
      if (!page || !this.selectionConfirmed(page) || !this.updateHash) {
        return;
      }

      // remove all breadcrumbs to the right of selection
      var container = dijit_registry.byId(this.containerId);
      var pageIndex = container.getIndexOfChild(page);
      var children = container.getChildren();

      // if the child is not the latest one, remove it and add it to the front
      // set
      if ((pageIndex != (children.length - 1)) && !(pageIndex == 0)) {
        var existingChild = children[pageIndex];
        // Should really try to move instead of the below destroy and recreate
        
        // When manipulating the ordering of the elements in the breadcrumb container, we should
        // not update the hash with the new breadcrumb design as it  
        // - affects the backward and forward operations
        // - may trigger a hash to view operation (an issue that surfaces when running hashToResource test thru FF
        this.updateHash = false;
        container.removeChild(existingChild);
        container.addChild(page);
        this.updateHash = true;
//        console.debug('Added the following page to the breadcrumb: ', page);
        // Update the URL when navigating to a resource via breadcrumbs
//        console.debug('Update Hash from breadcrumb for ', page.headerTitle);
//        if (page.lastHash){
//          console.debug('updating hash from breadcrumb through lastHash');
//        	viewToHash.updateHash(page.lastHash);
//        } else {
//        	viewToHash.updateHash(page.resource);
//        }
      }

      // This is the logic to update the dashboard URL only when navigating to the dashboard
      if (pageIndex == 0 && children.length > 1) {
        console.debug('Update Hash from breadcrumb for dashboard? ', page.headerTitle);
        var breadcrumbPane = registry.byId(ID.getBreadcrumbPane());
        breadcrumbPane.resetBreadcrumbPane();

        if (this.updateHash) {
          viewToHash.updateHash();
        }
      }
      this._tabPage = page;
    },

    /**
     * Before moving to the user's selected link (removing all breadcrumbs to the right), users can implement this method in order to
     * prevent the click from happening. For example, to show a confirmation dialog if changes that were made have not been saved.
     * 
     * @param {dijit._Widget}
     *          page The page in the container which the end-user wants to return to
     * @returns {boolean} Whether the breadcrumb move should take place.
     */
    selectionConfirmed : function(/* dijit._Widget */page) {
      return true;
    },

    onkeydown : function(/* Event */e, /* Boolean? */fromContainer) {
      // summary:
      // Handle keystrokes on the page list, for advancing to next/previous button
      // and closing the current page if the page is closable.
      // tags:
      // private
      if (this.disabled || e.altKey) {
        return;
      }
      var forward = null;
      if (e.ctrlKey || !e._djpage) {
        switch (e.keyCode) {
        case keys.LEFT_ARROW:
        case keys.UP_ARROW:
          if (!e._djpage) {
            forward = false;
          }
          break;
        case keys.PAGE_UP:
          if (e.ctrlKey) {
            forward = false;
          }
          break;
        case keys.RIGHT_ARROW:
        case keys.DOWN_ARROW:
          if (!e._djpage) {
            forward = true;
          }
          break;
        case keys.PAGE_DOWN:
          if (e.ctrlKey) {
            forward = true;
          }
          break;
        case keys.ENTER:
        case keys.SPACE:
          this.onButtonClick(this._tabPage);
          e.stopPropagation();
          e.preventDefault();
          break;
        default:
          this.inherited(arguments);
          break;
        }
        // handle next/previous page navigation (left/right arrow, etc.)
        if (forward !== null) {
          this.onArrowKeys(this.adjacent(forward).page);
          e.stopPropagation();
          e.preventDefault();
        }
      }
    },

    onArrowKeys : function(/* dijit/_WidgetBase */page) {
      // summary:
      // Called whenever one of my child buttons is navigated thru an arrow key
      // tags:
      // private

      // For TabContainer where the tabs are <span>, need to set focus explicitly when left/right arrow
      focus.focus(button.focusNode);

      this._tabPage = page;
    },

    adjacent : function(/* Boolean */forward) {
      // summary:
      // Helper for onkeydown to find next/previous button
      // tags:
      // private
      if (!this.isLeftToRight() && (!this.tabPosition || /top|bottom/.test(this.tabPosition))) {
        forward = !forward;
      }
      // find currently focused button in children array
      var children = this.getChildren();
      var page = this._currentChild;
      if (this._tabPage) {
        page = this._tabPage;
      }
 //     var idx = array.indexOf(children, this.pane2button(page.id)), current = children[idx];

      // Pick next/previous non-disabled button to focus on. If we get back to the original button it means
      // that all buttons must be disabled, so return current child to avoid an infinite loop.
      var child;
      do {
        idx = (idx + (forward ? 1 : children.length - 1)) % children.length;
        child = children[idx];
      } while (child.disabled && child != current);

      return child; // dijit/_WidgetBase
    }
  });

//  BreadcrumbMRUController.Breadcrumb = Breadcrumb;
  return BreadcrumbMRUController;

});
