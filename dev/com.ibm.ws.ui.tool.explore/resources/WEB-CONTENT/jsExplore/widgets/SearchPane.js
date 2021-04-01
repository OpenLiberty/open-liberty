/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
define([ "js/common/platform", "dojo/_base/lang", "dojo/_base/declare", "dijit/registry", 
         "dojo/dom-style", "dijit/layout/ContentPane", "dijit/form/Select", "dojox/mobile/ComboBox", "dijit/focus", "dojo/dom",
         "dojo/store/Memory", "dijit/form/TextBox", "dijit/form/Button",
         "jsExplore/views/viewFactory", 'jsExplore/resources/resourceManager', "dojo/dom-construct", "dojo/i18n!../nls/explorerMessages",
         "jsExplore/widgets/ExploreSearchBox"], 
    function(platform, lang, declare, registry, 
             domStyle, ContentPane, Select, mComboBox, focusUtil, dom,
             Memory, TextBox, formButton,
             viewFactory, resourceManager, domConstruct, i18n, ExploreSearchBox) {

  var SearchPane = declare("SearchPane", [ ContentPane ], {

      //id : 'search-mainSearch-contentPane',
      baseClass : "searchPane",

      postCreate : function() {
        this.__buildSearchPane();
      },

      __buildSearchPane : function() {
        
          var exploreSearchBoxObj = new ExploreSearchBox();
          this.addChild(exploreSearchBoxObj);
        
      },

      /**
       * Performs clean up of the breadcrumb pane.
       */
      resetSearchPane : function() {
        this.destroyDecendants();
        this.__buildSearchPane();
      }

  });
  return SearchPane;

});