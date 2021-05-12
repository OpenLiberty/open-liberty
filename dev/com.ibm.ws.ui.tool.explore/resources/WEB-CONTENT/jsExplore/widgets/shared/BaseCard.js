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
define(['jsExplore/widgets/shared/ScalingPolicyIcon',
        "dojo/_base/declare", 
        "dojo/_base/lang", 
        "dojo/has",
        "jsExplore/widgets/shared/StateIcon",
        "dojox/string/BidiComplex",
        "dojo/i18n!../../nls/explorerMessages",
        "dojo/dom",
        "dojo/dom-construct",
        "dijit/layout/BorderContainer",
        "dijit/layout/ContentPane",
        "dijit/registry",
        "dojo/on",
        "jsExplore/resources/Observer",
        'jsExplore/resources/utils',
        'dojo/dom-class',
        'dojox/gfx',
        'dojo/dom-style',
        'dojo/_base/fx',
        'dojo/fx',
        'jsShared/utils/imgUtils'
        ], 
        function(ScalingPolicyIcon, 
            declare, 
            lang, 
            has, 
            StateIcon, 
            BidiComplex,
            i18n,
            dom,
            domConstruct,
            BorderContainer,
            ContentPane,
            registry,
            on,
            Observer,
            utils,
            domClass,
            gfx, domStyle, fxBase, fx, imgUtils
        ) {

  return declare('BaseCard', [ Observer ], {
    id : "",
    label : "",
    resource: "",
    serverListProcessor : null,
    appListProcessor : null,
    statusIcon: '',
    onClick : "",
    "class" : "cardInner", 

    // fields for the template
    templateString : "",
    // textDir is a default value only. If a field contains a string, call utils.getStringTextDirection(string) instead
    textDir: "ltr",
    cardLabelTitle : "",
    cardLabelId : "",
    cardLabelDir : "",
    cardIconDivId : "",
    cardIcon : "",
    cardUserDirDivId : "",
    cardUserDirTitleId : "",
    cardUserDirTitle : "",
    cardUserDirDisplayTitle : "",
    cardUserDirClass : "cardUserDir",
    cardStatusIconId : "",
    cardStatusIconImg : "",
    cardStatusIconTitle : "",
    cardStatusLabelId : "",
    cardStatusLabelTitle : "",
    cardBottomAppDivId : "",
    cardBottomAppImgId : "",
    cardBottomAppTitle : "",
    cardBottomAppDir : "",
    cardBottomAppIconTitle : i18n.APPLICATION,
    cardBottomClusterDivId : "",
    cardBottomClusterClass : "",
    cardBottomClusterImgId : "",
    cardBottomClusterTitle : "",
    cardBottomClusterTitleId : "",
    cardBottomClusterDir : "ltr",   // default to ltr so RPT scanner will pass - dir can't be blank even when not displayed
    cardBottomClusterIcon : "",
    cardBottomClusterIconTitle : i18n.CLUSTER,
    cardBottomHostDivId : "",
    cardBottomHostImgId : "",
    cardBottomHostImgAlt : "",
    cardBottomHostTitleId : "",
    cardBottomHostTitle : "",
    cardBottomHostDir : "",
    cardBottomHostIconTitle : i18n.HOST,
    cardBottomServerDivId : "",
    cardBottomServerImgAlt : "",
    cardBottomServerTitleId : "",
    cardBottomServerTitle : "",
    cardBottomServerDir : "ltr",  // default for RPT scanner
    cardBottomServerIcon : "",
    cardBottomServerIconTitle : i18n.SERVER,

    /**
     * Subscribe this observer resource
     */
    _init: function() {
      if (this.resource) {
        this.resource.subscribe(this);
      }
      this.cardBottomClusterTitleId = this.id + "clusterTitle";  // default for RPT scanner
      this.cardBottomServerTitleId = this.id + "serverTitle";  // default for RPT scanner
    },

    /**
     * Unsubsribe this observer resource 
     */
    destroy: function() {
      this.destroyDescendants();
      if (this.resource) {
        this.resource.unsubscribe(this);   
      }
    }, 

    startup : function() {
      this.inherited(arguments);
    },

    /*
     * Setup the resource icon and its ID in the card based on whether scaling is involved.
     */
    _setCardResourceIcon: function(isSetAttachPoint) {
      if (!this.cardIcon) {
        this.cardIcon = new ScalingPolicyIcon({
          parentId : this.id,
          resource : this.resource
        });  
      }
      if (isSetAttachPoint) {
        this.iconCp.set("content", this.cardIcon.getHTML());
      } else {
        this.cardIconDivId = this.cardIcon.id + "-ContentPane";
      }
    },

    /*
     * Setup the resource name in the card including the text direction.
     */
    _setCardLabel: function() {
      this.cardLabelId = this.id + "labelCp";
      this.cardLabelTitle = this.label.replace(/"/g, "&quot;");
      this.cardLabelDir = utils.getStringTextDirection(this.cardLabelTitle);
    },

    /*
     * Setup the user directory in the card including the css for text overflow and
     * wrapping.
     */
    _setUserDir: function(userDir) {
      this.cardUserDirDivId = this.id + "userDir";
      this.cardUserDirTitleId = this.cardUserDirDivId + "Title";
      this.cardUserDirTitle = userDir.replace(/"/g, "&quot;");
      if (utils.getBidiTextDirectionSetting() !== "ltr") {
        this.cardUserDirDisplayTitle = BidiComplex.createDisplayString(userDir, "FILE_PATH");
      } else {
        this.cardUserDirDisplayTitle = userDir;
      }
      this.cardUserDirClass = "cardUserDir";           
      var userDirLength = this._getStringPixel(userDir);    
      // max width of the server card top is 162px
      if (userDirLength < 162) {          
        this.cardUserDirClass = "cardUserDirNoTruncate";    
      }     
    },

    _getStringPixel: function(text) {
      // there's no known way of getting a pixel length from a string in js
      // the work around by putting div with the specified string in html
      // then retrieved the id to calculate
      // the pixel and delete the div 
      var div = document.createElement('div');
      div.setAttribute('id', "userDirWidth");      
      div.innerHTML = text;
      document.body.appendChild(div);

      var t = document.getElementById("userDirWidth");
      var width = (t.clientWidth);

      div.parentNode.removeChild(div);

      return width;
    },

    /*
     * Setup the resource state icon and label in the card.
     */
    _setResourceState: function() {
      this.statusIcon = new StateIcon({
        parentId: this.id,
        resource: this.resource,
        size: "14",
        cardState: true
      });

      //this.cardStatusIconId = this.id + "-stateIcon";
      this.cardStatusIconId = this.statusIcon.id;
      this.cardStatusIconImg = this.statusIcon.getIconImage();
      this.cardStatusIconTitle = this.statusIcon.getIconLabel(); 
      this.cardStatusLabelId = this.cardStatusIconId + "-stateLabel";
      this.cardStatusLabelTitle = this.cardStatusIconTitle;
    },

    /*
     * Setup the application data related to the resource.
     */
    _setAppData: function() {
      this.cardBottomAppDivId = this.id + "appList";
      this.cardBottomAppTitleId = this.cardBottomAppDivId + "Title";
      this.cardBottomAppImgId = this.cardBottomAppDivId + "Img";
      var appData = "";

      var listedStat = null;
      if (this.appListProcessor) {
        listedStat = this.appListProcessor(this.resource);
      }
      if (listedStat.template) {
        appData = lang.replace(listedStat.template, listedStat.inserts);
      } else {
        appData = listedStat.label + ": " + listedStat.resources;
      }
      this.cardBottomAppDir = utils.getStringTextDirection(appData);
      this.cardBottomAppTitle = appData;
    },

    /*
     * Setup the cluster data related to the resource.
     */
    _setClusterData: function(resource) {
      var resourceForCluster;
      // Either use the passed in resource, or use our default value
      if (resource) {
        resourceForCluster = resource;
      } else {
        resourceForCluster = this.resource;
      }

      // Always set up the IDs, incase it becomes a cluster member later
      this.cardBottomClusterDivId = this.id + "cluster";
      this.cardBottomClusterImgId = this.cardBottomClusterDivId + "Img";
      this.cardBottomClusterTitleId = this.cardBottomClusterDivId + "Title";

      // If we have a cluster, set it, otherwise hide the field
      if (resourceForCluster.cluster) {
        this.cardBottomClusterTitle = resourceForCluster.cluster;
        this.cardBottomClusterDir = utils.getStringTextDirection(resourceForCluster.cluster);
      } else {
        this.cardBottomClusterClass = "cardFieldDisplayNone";
      }
    },

    /*
     * Setup the host data related to the resource.
     */
    _setHostData: function(resource) {
      this.cardBottomHostDivId = this.id + "host";
      this.cardBottomHostImgId = this.cardBottomHostDivId + "Img";
      this.cardBottomHostTitleId = this.cardBottomHostDivId + "Title";
      if (resource) {     
        this.cardBottomHostTitle = resource.host;  
      } else {
        this.cardBottomHostTitle = this.resource.host;  
      }
      this.cardBottomHostDir = utils.getStringTextDirection(this.cardBottomHostTitle);
    },

    /*
     * Setup the server data related to the resource.
     */
    _setServerData: function() {
      if (this.serverListProcessor) {
        this.serverList = this.serverListProcessor(this.resource);

        this.cardBottomServerDir = this.textDir;  // use default ltr for the server
        
        // use the server list template to populate the up/total count
        this.cardBottomServerDivId = this.id + "serverLabelContent";
        this.cardBottomServerTitleId = this.id + "serverTitle";
        this.total = this.serverList.up + this.serverList.down + this.serverList.unknown;
        if (this.serverList.template) {
          // The server template should always contain the parts we need, this is less free form than the listedStat
          this.cardBottomServerTitle = lang.replace(this.serverList.template, [ this.serverList.up, this.total ]);
        } else {
          console.log("ERROR: serverList must specify a template.");
        }
        
      }
    },

    _updateState: function(state) {
      this.statusIcon.updateStateIcon(state);
    },

    _updateUserDir: function(userDir) {
      var userDirTitleDom = dom.byId(this.cardUserDirTitleId);
      if (userDirTitleDom) {
        userDirTitleDom.title = userDir.replace(/"/g, "&quot;");
        if (utils.getBidiTextDirectionSetting() !== "ltr") {
          userDirTitleDom.innerHTML = BidiComplex.createDisplayString(userDir, "FILE_PATH");
        } else {
          userDirTitleDom.innerHTML = userDir;
        }

        var userDirDom = dom.byId(this.cardUserDirDivId);
        if (userDirDom) {
          var userDirClass = "cardUserDir";           
          var userDirLength = this._getStringPixel(userDir);    
          // max width of the server card top is 162px
          if (userDirLength < 162) {          
            userDirClass = "cardUserDirNoTruncate";    
          }  
          userDirDom.baseClass = userDirClass;
        }
      }
    },

    _updateServerData: function(resource) {
      if (this.serverListProcessor) {
        this.serverList = this.serverListProcessor(resource);
      }
      var me = this; 
      if (this.serverList) {

        var serverTitleDom = dom.byId(me.cardBottomServerTitleId);
        if (serverTitleDom) {
          me.total = me.serverList.up + me.serverList.down + me.serverList.unknown;
          if (me.serverList.template) {
            var serverTitle = lang.replace(me.serverList.template, [ me.serverList.up, me.total ]);
            serverTitleDom.innerHTML = serverTitle;
            serverTitleDom.title = serverTitle;
            serverTitleDom.dir = utils.getStringTextDirection(serverTitle);
          }
        }
      }
    },

    _updateAppData: function(resource) {
      var listedStat = null;
      if (this.appListProcessor) {
        listedStat = this.appListProcessor(resource);
      }

      if (listedStat) {
        this.listedStat = listedStat;
        var appTitleDom = dom.byId(this.cardBottomAppTitleId);
        if (appTitleDom) {
          var appData = "";

          if (listedStat.template) {
            appData = lang.replace(listedStat.template, listedStat.inserts);
          } else {
            appData = listedStat.label + ": " + listedStat.resources;
          }
          appTitleDom.innerHTML = appData;
          appTitleDom.title = appData;
          appTitleDom.dir = utils.getStringTextDirection(appData);
        }
      }
    },

    _updateClusterData: function(cluster) {
      var clusterDom = dom.byId(this.cardBottomClusterDivId);
      if (!clusterDom) {
        // For whatever reason we can be called pre-maturely. If we don't have the dom yet don't try to update it
        return;
      }
      if (cluster) {
        var clusterDataDom = dom.byId(this.cardBottomClusterTitleId);
        clusterDataDom.innerHTML = cluster;
        clusterDataDom.title = cluster;  
        clusterDataDom.dir = utils.getStringTextDirection(cluster);

        var clusterImgDom = dom.byId(this.cardBottomClusterImgId);
//        clusterImgDom.alt = cluster;          

        // reset class to remove cardFieldDisplayNone just in case
        domClass.remove(clusterDom, 'cardFieldDisplayNone');
      } else {
        // reset class to add cardFieldDisplayNone
        domClass.add(clusterDom, 'cardFieldDisplayNone');
      }
    },

    _updateHostData: function(host) {
      var HostTitleDom = dom.byId(this.cardBottomHostTitleId);
      if (HostTitleDom) {
        HostTitleDom.innerHTML = host;
        HostTitleDom.title = host;
        HostTitleDom.dir = utils.getStringTextDirection(host);
      }
      var HostImgDom = dom.byId(this.cardBottomHostImgId);
      if (HostImgDom) {
//        HostImgDom.alt = host;
      }      
    },

    // The onStateChange method is not needed because the StateIcon already establishes an Observer

    onClusterChange: function(newCluster) {
      this._updateClusterData(newCluster);
    },

    /**
     * Used by ApplicationCard to re-draw number of instances up out of total
     */
    onTallyChange: function(newTally) {
      this._updateAppData(newTally);
    },

    onUserdirChange: function(newUserdir) {
      this._updateUserDir(newUserDir);
    },

    /**
     * Used by ClusterCard to update the Applications bottom element
     */
    onAppsListChange: function(newAppList, oldAppList) {
      // using this.resource as it has been updated with the newAppList
      // and appListProcessor expects resource as input
      this._updateAppData(this.resource);
    },

    /**
     * Used by HostCard, RuntimeCard and ClusterCard to redraw server list data
     */
    onServersTallyChange: function() {
      this._updateServerData(this.resource);
    },
    
    /**
     * When the resource this card is for is destroyed, do cleanup.
     */
    onDestroyed: function() {
      if (this.resource) {
        this.resource.unsubscribe(this);   
      }
      this.destroy();
    }

  });
});
