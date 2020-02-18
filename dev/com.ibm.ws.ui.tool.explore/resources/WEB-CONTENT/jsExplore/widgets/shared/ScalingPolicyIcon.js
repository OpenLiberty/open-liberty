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
define([ 'dojo/_base/declare',
         'dojo/dom',
         'dojo/dom-construct',
         'dojo/on',
         'dojo/i18n!../../nls/explorerMessages',
         'js/common/platform',
         'js/common/tr',
         'jsShared/utils/imgUtils'],
    function(
        declare,
        dom,
        domc,
        on,
        i18n,
        platform,
        tr,
        imgUtils) {

  // The default Icons for resources without a scaling policy
  function __getHostIcon() {
    return __getImageTag("host", i18n.HOST);
  }

  function __getClusterIcon() {
    return __getImageTag("cluster", i18n.CLUSTER);
  }

  function __getServerIcon() {
    return __getImageTag("server", i18n.SERVER);
  }

  function __getCollectiveControllerServerIcon() {
    return __getImageTag("collectiveController", i18n.COLLECTIVE_CONTROLLER_DESCRIPTOR);
  }
  
  function __getAppOnClusterIcon() {
    return __getImageTag("appOnCluster", i18n.APPLICATION);
  }

  function __getInstanceIcon() {
    return __getImageTag("instance", i18n.APPLICATION);
  }

  function __getRuntimeIcon() {
    return __getImageTag("runtime", i18n.RUNTIME);
  }

  function __getImageTag(resource, title) {
    return imgUtils.getSVGSmall(resource + '-selected', null, title);
  }  

  // The icons for resources with a scaling policy
  function __getClusterIconWithPolicy() {
    return __getImageTagAutoscaled("cluster", i18n.AUTOSCALED_CLUSTER);
  }

  function __getServerIconWithPolicy() {
    return __getImageTagAutoscaled("server", i18n.AUTOSCALED_SERVER);
  }
  
  // This is not possible; can't have autoscaled controllers
  function __getCollectiveControllerServerIconWithPolicy() {
    return __getImageTagAutoscaled("collectiveController", i18n.COLLECTIVE_CONTROLLER_DESCRIPTOR);
  }

  function __getAppOnClusterIconWithPolicy() {
    return __getImageTagAutoscaled("appOnCluster", i18n.AUTOSCALED_APPLICATION);
  }

  function __getInstanceIconWithPolicy() {
    return __getImageTagAutoscaled("instance", i18n.AUTOSCALED_APPLICATION);
  }

  function __getImageTagAutoscaled(resource, title) {
    return imgUtils.getSVGSmall(resource + '-autoscaled', null, title);
  }  

  /**
   * Gets the appropriate card icon for the given resource.
   * If the resource has an enabled scaling policy, then the scaled icon is returned.
   * If the resource does not have an enabled scaling policy, then the non-scaled icon is returned.
   */
  function __getScalingPolicyIcon(resource) {
    var type = resource.type;
    if (resource.scalingPolicyEnabled) {
      switch (type) {
      case 'appOnCluster':
        return __getAppOnClusterIconWithPolicy();
      case 'appOnServer':
        return __getInstanceIconWithPolicy();
      case 'cluster':
        return __getClusterIconWithPolicy();
      case 'server':
      case 'standaloneServer':
        if (resource.isCollectiveController) {
          return __getCollectiveControllerServerIconWithPolicy();
        } else {
          return __getServerIconWithPolicy();
        }
      default:
        console.error('_getScalingPolicyIcon called for an unknown resource type: ' + type);
      }
    } else {
      switch (type) {
      case 'appOnCluster':
        return __getAppOnClusterIcon();
      case 'appOnServer':
        return __getInstanceIcon();
      case 'cluster':
        return __getClusterIcon();
      case 'host':
        return __getHostIcon();
      case 'server':
      case 'standaloneServer':
        if (resource.isCollectiveController) {
          return __getCollectiveControllerServerIcon();
        } else {
          return __getServerIcon();
        }
      case 'runtime':
        return __getRuntimeIcon();
      default:
        console.error('_getScalingPolicyIcon called for an unknown resource type: ' + type);
      }
    }

  }

  /**
   * Defines the ScalingPolicyIcon. The ScalingPolicyIcon is a resource-policy aware entity which will auto-update
   * its icon based on whether or not if the resource has a scaling policy. While resources will update themselves, 
   * this establishes its own change event listener so that the icon is updated without need for external calls.
   */
  return declare('ScalingPolicyIcon', [], {
    constructor: function(configObj) {
      if (!configObj) {
        tr.throwMsg('Programming Error: No configuration object passed into the constructor. Go read the JSDoc!');
      }
      this.resource = configObj.resource;
      this.id = configObj.parentId+'-cardIcon';
      this.resource.subscribe(this);
    },

    onScalingPolicyChange: function() {
      this.__updateScalingPolicyIcon();
    },
    
    onScalingPolicyEnabledChange: function() {
      this.__updateScalingPolicyIcon();
    },

    destroy: function() {
      this.resource.unsubscribe(this);
      domc.destroy(this.id);
    },
    
    onDestroyed: function() {
      this.destroy();
    },

    __updateScalingPolicyIcon: function() {
      var icon = __getScalingPolicyIcon(this.resource);
      if (icon) {
        // TODO: For some reason, we're not getting cleaned up properly...
        // The DOM ID is gone but our listener was not removed?!
        var domElement = dom.byId(this.id);
        if (domElement) {
          domElement.innerHTML = icon;
        }
      }
    },

    getHTML: function() {
      var html = '<span id="'+this.id+'">'+__getScalingPolicyIcon(this.resource)+'</span>';
      return html;
    }

  });
});