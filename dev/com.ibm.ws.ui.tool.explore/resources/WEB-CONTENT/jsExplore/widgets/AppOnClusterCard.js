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
define([
    "dojo/_base/declare", 
    "dojo/dom",
    "dijit/_WidgetBase", 
    "dijit/_TemplatedMixin",
    "dijit/_WidgetsInTemplateMixin",
    "jsExplore/widgets/shared/BaseCard",
    "jsExplore/resources/utils",
    "dojo/text!./templates/AppOnClusterCard.html",
    "dojo/i18n!../nls/explorerMessages",
    "jsShared/utils/imgUtils"
    ], 
    function(
    declare, 
    dom,
    WidgetBase, 
    _TemplatedMixin, 
    _WidgetsInTemplateMixin, 
    BaseCard,
    utils,
    template, 
    i18n,
    imgUtils
    ) {

  var Card = declare("AppOnClusterCard", [ BaseCard, WidgetBase, _TemplatedMixin, _WidgetsInTemplateMixin ], {
    templateString : template,
    cardBottomAppIcon : imgUtils.getSVGSmallName('app'),
    

    postMixInProperties : function() {  
      this._setCardResourceIcon(false);
      this._setCardLabel();
      this._setResourceState();
      this._setAppData();
      this._setClusterData();
      console.log("postMixInProperties");
      this._init();
    },

    postCreate : function() {
      this.inherited(arguments);
      this._setCardResourceIcon(true);
      this.connect(this.domNode, "onclick", this.onClick);
    },
    
    _getClusterNames: function(clusters) {
      var clusterData = "";
      if (clusters.list.length === 1) {
        clusterData = clusters.list[0];
      } else if (clusters.list.length > 1) {
        clusterData = clusters.list.length + " " + i18n.CLUSTERS;
      }
      return clusterData;
    },
      
    // override the default behavior
    _setClusterData: function() {
      console.log("------------------- resource: ", this.resource);
      console.log("------------------- cluster: ", this.resource.cluster);
      console.log("------------------- cluster name: ", this.resource.cluster.name);
      if (this.resource.cluster !== null) {
        this.cardBottomClusterDivId = this.id + "cluster";
        this.cardBottomClusterTitleId = this.cardBottomClusterDivId + "Title";
        this.cardBottomClusterTitle = this.resource.cluster.name;
        this.cardBottomClusterImgId = this.cardBottomClusterDivId + "Img";
        this.cardBottomClusterDir = utils.getStringTextDirection(this.cardBottomClusterTitle);
      } else {
        this.cardBottomClusterClass = "cardFieldDisplayNone";
        this.cardBottomClusterDir = this.textDir;
      }
      this.cardBottomClusterIcon = imgUtils.getSVGSmallName('cluster');
    },
    
    update: function() {
      this._updateState(this.resource.state);
      this._updateAppData(this.resource);
      this._updateClusterData(this.resource.clusters);
    },
    
    _updateClusterData: function(clusters) {
      var clusterDom = dom.byId(this.cardBottomClusterDivId);
      if (clusters && clusters.list.length > 0) {
        var clusterDataDom = dom.byId(this.cardBottomClusterTitleId);
        var clusterNames = '';
        if (clusterDataDom) {
          clusterNames = this._getClusterNames(clusters);
          clusterDataDom.innerHTML = clusterNames;
          clusterDataDom.title = clusterNames;
          clusterDataDom.dir = utils.getStringTextDirection(clusterNames);
        }
        var clusterImgDom = dom.byId(this.cardBottomClusterImgId);
        if (clusterImgDom) {
          clusterImgDom.alt = clusterNames;
        }
       
        // reset class to remove cardFieldDisplayNone just in case
        if (clusterDom) {
          clusterDom.baseClass = "cardBottomListedStat";
        }
      } else {
        // reset class to add cardFieldDisplayNone
        if (clusterDom) {
          clusterDom.baseClass = "cardFieldDisplayNone";
        }
      }
    },
    
    onClusterChange: function(newCluster) {
      console.log("onClusterChange: " + newCluster + " for " + this.resource.id);
      this._updateClusterData(newCluster);
    }
    
  });
  return Card;
});
