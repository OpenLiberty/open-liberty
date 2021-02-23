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
require([ 'jsBatch/views/executionDetailsView',
          'jsBatch/views/jobLogsView',
          'jsBatch/views/loadingView',
          'jsBatch/utils/hashToView', 
          'dojo/topic', 
          'dijit/registry',
          'jsBatch/utils/viewToHash',
          'jsBatch/widgets/JavaBatchBreadcrumbPane',
          'jsShared/utils/imgUtils',
          'jsBatch/utils/hashUtils',
          'jsBatch/utils/linkToExploreUtils',
          'dijit/form/Button',
          'jsShared/utils/ID',
          'dojo/i18n!jsBatch/nls/javaBatchMessages',
          'dojo/domReady!' ], 
          function(executionDetailsView,
              jobLogsView,
              loadingView,
              hashToView,
              topic,
              registry,
              viewToHash,
              JavaBatchBreadcrumbPane,
              imgUtils,
              hashUtils,
              linkUtils,
              Button,
              sharedID,
              i18n) {
  
  'use strict';
  
  buildBreadCrumb();
  
  loadingView.init();
  
  topic.subscribe('/dojo/hashchange', function(changedHash) {
    if (changedHash !== '') {
      // Only deal with encoding/decoding logic if the url has a query string
      if(changedHash.indexOf('?') > -1) {
        // We want to make sure we do not double encode any existing encoded 
        // query string values.  If an user decides to add a non-encoded special
        // character to the URL containing already encoded special characters, we 
        // end up with a mix of encoded and decoded values.
        // If we first decode, this should normalize the hash to be all characters
        // that need to be encoded.  We can call encode safely without worry we
        // encoded the percent sign from already encoded characters.
        changedHash = decodeURIComponent(changedHash);
        var queryString = changedHash.substring(changedHash.indexOf('?'));
        var newQueryString = hashUtils.encodeQueryParameterValuesOnly(queryString);
        window.top.location.hash = changedHash.substring(0, changedHash.indexOf('?')) + newQueryString;
        changedHash = changedHash.substring(0, changedHash.indexOf('?')) + newQueryString;
      }
      viewToHash.lastUpdateHash = changedHash;
      hashToView.updateView(changedHash);
    }
  });
  
  linkUtils.getExploreInfo().then(function() {      
      //This shouldn't be done anywhere else.  It is done here just because on first load
      //we need to open the right page
      topic.publish("/dojo/hashchange", hashUtils.getCurrentHash());
  });
    
  function buildBreadCrumb() {
    var breadcrumbPane = new JavaBatchBreadcrumbPane( 
        {     
          id : sharedID.getBreadcrumbPane()
        });     
    var breadcrumbDiv = registry.byId('breadcrumbDiv');
    breadcrumbPane.placeAt(breadcrumbDiv);
  
    var refreshButton = new Button({
      title: i18n.REFRESH,
      label: i18n.REFRESH,
      baseClass: '',
      iconClass: 'searchButtonIcon',
      showLabel: false,
      "style" : "background-color:#2C363B",
      postCreate : function(){
        // use imageutils and use i18n file to change the alt attribute
        this.iconNode.innerHTML = imgUtils.getSVG('refresh');  
      },
      onFocus : function(){
        this.set("style", "background-color:none");
      },
      onBlur : function(){
        this.set("style", "background-color:#2C363B");
      },      
      onClick: function() {
        topic.publish("/dojo/hashchange", hashUtils.getCurrentHash());
      }
    }, "searchButton").startup();
  }
  
});
