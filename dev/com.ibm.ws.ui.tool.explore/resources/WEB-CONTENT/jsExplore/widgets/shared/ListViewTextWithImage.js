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
    "dojo/_base/lang",
    "dojox/string/BidiComplex",
    "dojo/i18n!../../nls/explorerMessages",
    "dijit/_WidgetBase", 
    "dijit/_TemplatedMixin",
    "dijit/_WidgetsInTemplateMixin", 
    'jsExplore/resources/utils',
    "dojo/text!./../templates/ListViewTextWithImage.html",
    'js/common/tr',
    "jsShared/utils/imgUtils"
    ], 
    function(
    declare, 
    lang,
    BidiComplex,
    i18n,
    WidgetBase, 
    _TemplatedMixin, 
    _WidgetsInTemplateMixin, 
    utils,
    template,
    tr,
    imgUtils
    ) {

  var listViewTextWithImage = declare("ListViewTextWithImage", [ WidgetBase, _TemplatedMixin, _WidgetsInTemplateMixin ], {
    templateString : template,
    listViewImg: "setMe",
    listViewImgTitle: "setMe",
    listViewTextTitle: "setMe",
    listViewTextDir: "setMe",

    postMixInProperties : function() {
      //listViewImg is no longer used - images are set in postCreate because SVGs need to be set there
      if (this.resource) {
        if (this.resource.type === "server") {
          if (this.resource.isCollectiveController) {
            this.listViewImgTitle = this.resource.name;
            this.listViewTextTitle = this.resource.name;
            this.listViewTextDir = utils.getStringTextDirection(this.resource.name);
            this.listViewLink = "#explore/servers/" + this.resource.id;
          }
        } else if (this.resource.type === "appOnCluster") {

          var name = this.resource.cluster.id;
          
          this.listViewImgTitle = name;
          this.listViewTextTitle = name;
          this.listViewTextDir = utils.getStringTextDirection(name);
          this.listViewLink = "#explore/clusters/" + name;
        } else if (this.resource.type === "appOnServer") {
          
          var name = this.resource.server.id;
          
          var locationName = this.__getLocationAppOnServer(name);

          this.listViewImgTitle = locationName;
          this.listViewTextTitle = locationName;
          this.listViewTextDir = utils.getStringTextDirection(locationName);
          this.listViewLink = "#explore/servers/" + name;          
        }
      } else {
        tr.throwMsg('ListViewTextWithImg created without a resource');
      }
    },
    
    postCreate: function() {
      if (this.resource.type === "server") {
        if (this.resource.isCollectiveController) {
          this.iconNode.innerHTML = imgUtils.getSVGSmall('collectiveController', null, i18n.COLLECTIVE_CONTROLLER_DESCRIPTOR);
          if (this.resource.scalingPolicyEnabled) {
            this.iconNode.innerHTMl = imgUtils.getSVGSmall('collectiveController-autoscaled', null, COLLECTIVE_CONTROLLER_DESCRIPTOR);
          }
        }
      } else if (this.resource.type === "appOnCluster") {
        this.iconNode.innerHTML = imgUtils.getSVGSmall('cluster', null, i18n.CLUSTER);
      } else if (this.resource.type === "appOnServer") {
        this.iconNode.innerHTML = imgUtils.getSVGSmall('server', null, i18n.SERVER);
      }
    },
    
    __getLocationAppOnServer: function(name) {
        var locationName = name;
        
        var firstIndex = name.indexOf(",");
        var lastIndex = name.lastIndexOf(",");
        
        if (firstIndex > 0 && lastIndex > 0) {
            var hostName = name.substring(0, firstIndex);       
        
            var path = name.substring(firstIndex + 1, lastIndex);
            var isBidi = utils.getBidiTextDirectionSetting();
            var bidiPath = path;
            // call bidi only if it's enable
            if (isBidi !== "ltr") {
                bidiPath = BidiComplex.createDisplayString(path, "FILE_PATH");
            }
  
            var serverName = name.substring(lastIndex + 1);
        
            locationName = lang.replace(i18n.APPONSERVER_LOCATION_NAME, [ serverName, hostName, bidiPath]);
        }
   
        return locationName;
  }
    
  });
  return listViewTextWithImage;
});
