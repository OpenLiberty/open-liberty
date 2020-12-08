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
 * viewFactory gets us around the circular dependency collectionView and objectView.
 */
define([ 'jsExplore/resources/viewToHash', 'jsExplore/resources/hashUtils', 'dijit/registry', 'jsExplore/utils/ID' ], function(viewToHash, hashUtils, registry, ID) {

  return {
    collectionView : null,
    objectView : null,
    searchView : null,

    openSearchView : function (queryParams) {
      console.debug('queryParams: ' + queryParams);
      if (!this.searchView) {
        console.error('viewFactory.openView called before searchView was set');
      } else {
        this.searchView.openSearchView(queryParams, 'search');
      }
    },
    
    openView : function(resource, defaultSideTab, pane) {
      if (!this.collectionView || !this.objectView) {
        console.error('viewFactory.openView called before collectionView and objectView were set');
      } else {
        viewToHash.updateHash(resource, defaultSideTab);
        
        var breadcrumbStackContainer = registry.byId(ID.getBreadcrumbStackContainer());
        var breadcrumbAndSearchPane = registry.byId(ID.getBreadcrumbAndSearchDiv());
        if (breadcrumbStackContainer  && breadcrumbAndSearchPane) {
          breadcrumbStackContainer.selectChild(breadcrumbAndSearchPane);
        }

        var type = resource.type;
        switch (type) {
        case 'appInstancesByCluster':
        case 'appsOnCluster':
        case 'appsOnServer':
        case 'serversOnCluster':
        case 'serversOnRuntime':
        case 'serversOnHost':
        case 'runtimesOnHost':
        	if (!pane){
        		this.objectView.openObjectView(resource.parentResource, resource.viewType);
        		break;
        	}
        case 'applications':
        case 'clusters':
        case 'hosts':
        case 'servers':
        case 'runtimes':
          this.collectionView.openCollectionView(resource, defaultSideTab, pane);
          break;
        case 'appOnCluster':
        case 'appOnServer':
        case 'cluster':
        case 'host':
        case 'runtime' :
        case 'server':
        case 'standaloneServer':
          this.objectView.openObjectView(resource, defaultSideTab);
          break;
        default:
          console.error('viewFactory.openView called for an unknown resource type: ' + type);
        }
      }
    }

  };

});