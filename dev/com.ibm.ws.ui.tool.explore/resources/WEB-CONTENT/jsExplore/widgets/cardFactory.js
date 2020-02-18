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
 * cardFactory creates a card based on the resource type
 */
define([ "jsExplore/widgets/AppOnClusterCard",
         "jsExplore/widgets/AppOnServerCard",
         "jsExplore/widgets/ClusterCard",
         "jsExplore/widgets/HostCard",
         "jsExplore/widgets/RuntimeOnHostCard",
         "jsExplore/widgets/ServerCard"], 
         function(
             AppOnClusterCard,
             AppOnServerCard,
             ClusterCard,
             HostCard,
             RuntimeOnHostCard,
             ServerCard 
             ) {

  return {
    getCard : function(resource, id, label, serverListProcessor, appListProcessor, onclick) {
      var card = null;
      switch (resource.type) {
      case "appOnCluster":
        card = new AppOnClusterCard({
          id : id + "newCard",
          label : label,
          serverListProcessor : serverListProcessor,
          appListProcessor : appListProcessor,
          resource: resource,
          onClick : onclick,
          tabindex : 0,
          role : "button",
          'aria-label' : label
        }, "card");
        break;
      case "appOnServer":
        card = new AppOnServerCard({
          id : id + "newCard",
          label : label,
          serverListProcessor : serverListProcessor,
          appListProcessor : appListProcessor,
          resource: resource,
          onClick : onclick,
          tabindex : 0,
          role : "button",
          'aria-label' : label
        }, "card");
        break;
      case "cluster":
        card = new ClusterCard({
          id : id + "newCard",
          label : label,
          serverListProcessor : serverListProcessor,
          appListProcessor : appListProcessor,
          resource: resource,
          onClick : onclick,
          tabindex : 0,
          role : "button",
          'aria-label' : label
        }, "card");
        break;
      case "host":
        card = new HostCard({
          id : id + "newCard",
          label : label,
          serverListProcessor : serverListProcessor,
          appListProcessor : appListProcessor,
          resource: resource,
          onClick : onclick,
          tabindex : 0,
          role : "button",
          'aria-label' : label
        }, "card");
        break;
      case "runtime":
        card = new RuntimeOnHostCard({
          id : id + "newCard",
          label : label,
          serverListProcessor : serverListProcessor,
          appListProcessor : appListProcessor,
          resource: resource,
          onClick : onclick,
          tabindex : 0,
          role : "button",
          'aria-label' : label
        }, "card");
        break;
      case "server":
        card = new ServerCard({
          id : id + "newCard",
          label : label,
          serverListProcessor : serverListProcessor,
          appListProcessor : appListProcessor,
          resource: resource,
          onClick : onclick,
          tabindex : 0,
          role : "button",
          'aria-label' : label
        }, "card");
        break;
      default:
        console.error('cardFactory.getCard called for an unknown resource type: ' + resource.type);
      }
      return card;
    }

  };

});