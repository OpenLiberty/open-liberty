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
define(['dojox/widget/Standby',
        'dijit/registry',
        'dojo/i18n!jsBatch/nls/javaBatchMessages',
        'dojo/dom-attr',
        'jsBatch/utils/ID'
        ], 
function(
    Standby,
    registry,
    i18n,
    domAttr,
    ID) {

  'use strict';

  /*
   * View to show when the a view is in process of loading
   */
  
  return {
    viewId: ID.LOADING_VIEW,
    init : init
  };
  
  function init() {
    var standby = new Standby({
      target : ID.LOADING_VIEW_STANDBY,
      image : 'imagesShared/search-loading-T.gif',
      color : '#FFFFFF'// the gif has a white background, so don't use default
      });
    standby.startup();
    // Looks like this widget needs to live somewhere in the document
    document.body.appendChild(standby.domNode);
    standby.show();
    
    /*
     * Setup a11y for this view
     */
    var loadingView = registry.byId(ID.LOADING_VIEW);
    if (loadingView) {    
      //  ...label has to be on the tab panel the standby is loaded into.
      var parentNode = loadingView.domNode.parentNode;
      domAttr.set(parentNode, "title", i18n.LOADING_VIEW_TITLE);
      domAttr.set(parentNode, "aria-label", i18n.LOADING_VIEW);
    }
  }
});