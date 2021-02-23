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
define([ "dojo/_base/declare", "jsExplore/utils/constants", "dojox/mobile/Button", "dojo/dom", "dijit/layout/ContentPane", "dijit/_TemplatedMixin", "dijit/_WidgetsInTemplateMixin",
         "jsExplore/widgets/cardFactory", "js/common/platform", "dojo/dom-class", "dijit/registry",
         "dojo/_base/lang", "dojo/i18n!../nls/explorerMessages", "dojo/on", "dojo/keys", "jsExplore/widgets/shared/ActionButtons", "jsExplore/resources/Observer",
         "jsExplore/resources/utils", "jsExplore/resources/resourceManager", "jsShared/utils/imgUtils", "dojo/domReady!" ],
         function(declare, constants, Button, dom, ContentPane, _TemplatedMixin, _WidgetsInTemplateMixin,
             cardFactory, platform, domClass, registry, lang, i18n, on, keys, ActionButtons, Observer, utils, resourceManager, imgUtils) {

  return declare('CardBorderContainer', [ContentPane], {
    actionButton : "",
    id : null,
    splitters : false,
    baseClass : null,
    open : false,
    label : "",
    onkeydown : "",
    postCreate : function() {
      this.inherited(arguments);
      this.resource.subscribe(this);
      this.newCard();
    },
    destroy: function() {
      this.inherited(arguments);
      if (this.resource) {
        this.resource.unsubscribe(this);   
      }
    }, 
    startup : function() {
      this.inherited(arguments);
    },
    createNewCard : function(resource, onclick, serverListProcessor, appListProcessor) {
      var card = cardFactory.getCard(resource, this.id, this.label, serverListProcessor, appListProcessor, onclick); 
      return card;
    },

    setSelected : function(select) {
      var me = this;
      var idTemplate = this.id + "selectedPane";
      if (select) {
        dom.byId(me.id+'newCard').setAttribute('aria-pressed','true');
        var selectedPane = "";
        var checkMark = "";
        if (registry.byId(idTemplate)) {
          selectedPane = registry.byId(idTemplate);
          checkMark = registry.byId(idTemplate + "checkMark");
        } else {
          selectedPane = new ContentPane(
              {
                id : idTemplate,
                region : "center",
                baseClass : "cardSelectedPane",
                style : 'height: ' + this.cp.containerNode.clientHeight + 'px;',
                content : ""
              });
          checkMark = new ContentPane({
            id : idTemplate + "checkMark",
            content : imgUtils.getSVGSmall('complete'),
            baseClass : 'cardSelectedPaneCheck'
          });
        }
        checkMark.startup = function() {};
        selectedPane.addChild(checkMark);
        on(selectedPane, "click", function() {
          me.toggleState(false);
        });
        me.addChild(selectedPane);
      } else {
        dom.byId(me.id+'newCard').setAttribute('aria-pressed',"false");
        if (registry.byId(idTemplate)) {
          me.removeChild(registry.byId(idTemplate));
          registry.byId(idTemplate).destroy();
        }
      }
    },
    newCard : function() {
      var me = this;
      me.cp = new ContentPane({
        id : me.id + "contentPaneInner",
        region : "center",
        baseClass : "contentPaneInner",
        content : ""
      });
      on(me.cp.domNode, "focus", function() {
        domClass.add(me.cp.domNode, "contentPaneInnerFocused");
        me.keydownHandler = on(me.cp, "keydown", me.onkeydown);
      });
      on(me.cp.domNode, "focusout", function() {
        domClass.remove(me.cp.domNode, "contentPaneInnerFocused");
        if (me.keydownHandler !== undefined) {
          me.keydownHandler.remove();
          me.keydownHandler = undefined;
        }
      });
      me.buildResource(me.cp, me.resource);
      me.addChild(me.cp);
    },
    buildResource : function(cp) {
      var me = this;
      cp.destroyDescendants(false);
      cp.set("class", "contentPaneInner");
      
      if(me.resource.alerts && me.resource.alerts.count>0){
        me.updateAlerts(cp, this.resource.alerts);
      }
      if ( me.resource.maintenanceMode ) {
        this.updateMaintenanceModeAlert(cp);
      }

      me.card = me.createNewCard( this.resource, me.onclick, me.serverListProcessor, me.appListProcessor);
      cp.addChild(me.card, 0);
      if (registry.byId(me.id + "alertDialog"))
      {
        registry.byId(me.id + "alertDialog").destroy();
      }
      this.actionButton = ActionButtons.createStateButton(this.resource, me.id);
      this.actionButton.placeAt(cp);
    },
    onDestroyed: function() {
      this.destroy();
    },
    onAlertsChange: function(newAlerts) {
      this.updateAlerts(this.cp, newAlerts);
    },   
    onStateChange: function(newState) {
      this.actionButton = ActionButtons.createStateButton(this.resource, this.id);
      this.actionButton.placeAt(this.cp);
    },
    onMaintenanceModeChange: function() {
      this.updateMaintenanceModeAlert(this.cp);
    },
    updateMaintenanceModeAlert: function(cp) {
      var me = this;
      var type = this.resource.type;
      if ( type === "server" || type === "host" || type === "appOnServer") {
        var hasExistingAlert = false;
        var styleString = "right: 0px;";
        if (registry.byId(me.id + "alertIcon")) {
          hasExistingAlert = true;
          styleString = "right: 24px;";
        }
        
        var localAlerts = this.resource.maintenanceMode;
        if (localAlerts && (localAlerts === constants.MAINTENANCE_MODE_ALTERNATE_SERVER_STARTING || localAlerts === constants.MAINTENANCE_MODE_IN_MAINTENANCE_MODE)) {
          if (registry.byId(me.id + "mmAlertIcon")) {
            registry.byId(me.id + "mmAlertIcon").destroy();
          }
          var label = i18n.ENABLING_MAINTENANCE_MODE;
          var imgSrc = "enabling-maintenance-mode-21-DT.gif";
          if ( localAlerts === constants.MAINTENANCE_MODE_IN_MAINTENANCE_MODE) {
            label = i18n.MAINTENANCE_MODE_ENABLED;
            imgSrc = "maintenance-mode-21-DT.png";
          }
            // sets the orange border since we are either in MM or MM in progresss
            cp.set("class", "contentPaneInnerAlert");
            var mmAlertIcon = new ContentPane({
              id : me.id + "mmAlertIcon",
              content : '<img src="images/'+ imgSrc +'" height="21" width="21" title="' + label + '" alt="' + label + '">',
              baseClass : "maintenanceMode",
              style: styleString,
              tabindex: '0'
            }, "mmAlertIcon");
            mmAlertIcon.placeAt(cp);
            on(mmAlertIcon.domNode, "focus", function(evt) {
              domClass.add(mmAlertIcon.domNode, "maintenanceModeFocused");
            });
            on(mmAlertIcon.domNode, "focusout", function(evt) {
              domClass.remove(mmAlertIcon.domNode, "maintenanceModeFocused");
            });
          
        } else {
          if ( hasExistingAlert == false ) {
            // destroy any existing alert if no real alerts
            if (registry.byId(me.alertCpId)) {
              cp.removeChild(registry.byId(me.alertCpId));
              registry.byId(me.alertCpId).destroy();
            }           
            if (me.alertFocusHandler) {
              me.alertFocusHandler.remove();
              me.alertFocusHandler = undefined;
            }
            cp.set("class", "contentPaneInner");
          }
          if (registry.byId(me.id + "mmAlertIcon")) {
            registry.byId(me.id + "mmAlertIcon").destroy();
          }
        }  
      }
      else {
        console.log('cardBorderContainer.updateMaintenanceModeAlert called for an unknown resource type: ' + type);
      }             
    },
    updateAlerts: function(cp, newAlerts) {
      var me = this;
      var localAlerts = me.alertContent;
      if (newAlerts) {
        var type = this.resource.type;
        switch (type) {
        case 'appOnServer':
          localAlerts = me.appOnServerAlertProcessor(newAlerts);
          break;
        case 'cluster':
        case 'host':
        case 'runtime':
          localAlerts = me.hostOrRuntimeorClusterAlertProcessor(newAlerts);
          break;
        case 'appOnCluster':
          localAlerts = me.appOnClusterAlertProcessor(newAlerts);
          break;
        case 'server':
        case 'standaloneServer':
          localAlerts = me.serverAlertProcessor(newAlerts);
          break;
        default:
          console.error('cardBorderContainer.updateAlerts called for an unknown resource type: ' + type);
        }
      }
      this.alertString = localAlerts;
      if (localAlerts && localAlerts != "") {
        if (registry.byId(me.id + "alertIcon")) {
          // update existing alert content
          var alertCp = registry.byId(me.alertCpId);
          if (alertCp) {
            alertCp.set("content", localAlerts);
          }
        } else {
          cp.set("class", "contentPaneInnerAlert");
          var alertIcon = new Button({
            id : me.id + "alertIcon",
            label : '<img src="images/card-badge-alert-T-S.png" height="21" width="21" title="' + i18n.ALERT + '" alt="' + i18n.ALERT + '">',
            baseClass : "alertExclamation",
            onClick : function(evt) { // onClick will handle both key and click with button
              me.createAlertBox(cp);
              var alertCpDOM = dom.byId(me.alertCpId);
              if (alertCpDOM) {
                // now force the focus on the alert pane
                alertCpDOM.focus();
              }
            }
          }, "alertIcon");
          alertIcon.placeAt(cp);
          on(alertIcon.domNode, "focus", function(evt) {
            domClass.add(alertIcon.domNode, "alertExclamationFocused");
            if (me.keydownHandler !== undefined) { // just in case as I know it is not necessary
              me.keydownHandler.remove();
            }
          });
          on(alertIcon.domNode, "focusout", function(evt) {
            domClass.remove(alertIcon.domNode, "alertExclamationFocused");
          });
        }
      } else {
        // destroy any existing alert
        if (registry.byId(me.alertCpId)) {
          cp.removeChild(registry.byId(me.alertCpId));
          registry.byId(me.alertCpId).destroy();
        }           
        if (me.alertFocusHandler) {
          me.alertFocusHandler.remove();
          me.alertFocusHandler = undefined;
        }
        if (registry.byId(me.id + "alertIcon")) {
          registry.byId(me.id + "alertIcon").destroy();
        }

        cp.set("class", "contentPaneInner");
        // now updates maintenanceMode alert position
        this.updateMaintenanceModeAlert(cp);
      }
    },
    serverAlertProcessor: function(alerts) {
      var alertString = "";
      if (alerts && alerts.count > 0) {
        if (alerts.app && alerts.app.length > 0) {
          if (alerts.app.length < 3) {
            var appNames = [];          
            for ( var a in alerts.app) {
              var spanClose = '</span>';
              var appName = resourceManager.getAppNameFromId(alerts.app[a].name);
              var spanOpen1 = '<span dir="' + utils.getStringTextDirection(appName) + '" >';
              var insert1 = spanOpen1 + appName + spanClose;
              var clusterOrServerName = resourceManager.getClusterOrServerName(alerts.app[a].name);
              var insert2 = "";
              if (alerts.app[a].servers.length == 1 && clusterOrServerName === alerts.app[a].servers[0]) {
                // appOnServer              
                clusterOrServerName = clusterOrServerName.substring(clusterOrServerName.lastIndexOf(',') + 1);
                var spanOpen2 = '<span dir="' + utils.getStringTextDirection(clusterOrServerName) + '" >';
                insert2 = spanOpen2 + clusterOrServerName + spanClose;
              } else {  
                // appOnCluster              
                var spanOpen2 = '<span dir="' + utils.getStringTextDirection(clusterOrServerName) + '" >';
                insert2 = spanOpen2 + clusterOrServerName + spanClose;
              }
              var appDisplayName = lang.replace(i18n.RESOURCE_ON_RESOURCE, [ insert1, insert2]);
              
              appNames.push(appDisplayName);
            }
            alertString += lang.replace(i18n.INSTANCE_STOPPED_ON_SERVERS, [ appNames ]) + "<br>";
          } else {
            alertString += lang.replace(i18n.APPS_NOT_RUNNING, [ alerts.app.length ]) + "<br>";
          }
        }
        if (alerts.unknown && alerts.unknown.length > 0) {
          if(alerts.unknown[0].type === "server")
            alertString += lang.replace(i18n.UNKNOWN_STATE, [ alerts.unknown[0].id.substring(alerts.unknown[0].id.lastIndexOf(',') + 1) ]) + "<br>";
          if(alerts.unknown[0].type === "appOnServer" || alerts.unknown[0].type === "appOnCluster")
            alertString += lang.replace(i18n.UNKNOWN_STATE, [ alerts.unknown[0].id ]);
        }
      }
      return alertString;
    },

    appOnClusterAlertProcessor: function(alerts) {
      var alertString = "";
      if (alerts && alerts.count > 0) {
        if (alerts.app && alerts.app.length > 0) {
          var spanClose = '</span>';
          // The alert.app.name field is of the form serverTuple|clusterName,appName.
          // Pull out the app name to display.
          var appName = resourceManager.getAppNameFromId(alerts.app[0].name);
          var spanOpen1 = '<span dir="' + utils.getStringTextDirection(appName) + '" >';
          var insert1 = spanOpen1 + appName + spanClose;
          var clusterName = resourceManager.getClusterOrServerName(alerts.app[0].name);
          var spanOpen2 = '<span dir="' + utils.getStringTextDirection(clusterName) + '" >';
          var insert2 = spanOpen2 + clusterName + spanClose;
          var appOnClusterDisplayName = lang.replace(i18n.RESOURCE_ON_RESOURCE, [ insert1, insert2]);
          alertString += lang.replace(i18n.INSTANCE_STOPPED_ON_SERVERS, [ appOnClusterDisplayName ]) + "<br>";
        }
        if (alerts.unknown && alerts.unknown.length > 0) {
          alertString += lang.replace(i18n.UNKNOWN_STATE, [ alerts.unknown[0].id ]);
        }
      }
      return alertString;
    },

    appOnServerAlertProcessor: function(alerts) {
      var alertString = "";
      if (alerts && alerts.count > 0) {
        var spanClose = '</span>';
        if (alerts.app && alerts.app.length > 0) {
          // The alert.app.name field is of the form serverTuple|clusterName,appName.
          // Pull out the app name to display.
          var appName = resourceManager.getAppNameFromId(alerts.app[0].name);
          var spanOpen1 = '<span dir="' + utils.getStringTextDirection(appName) + '" >';
          var insert1 = spanOpen1 + appName + spanClose;
          var insert2name = alerts.app[0].servers[0].substring(alerts.app[0].servers[0].lastIndexOf(',') + 1);
          var spanOpen2 = '<span dir="' + utils.getStringTextDirection(insert2name) + '" >';
          var insert2 = spanOpen2 + insert2name + spanClose;
          var appOnServerDisplayName = lang.replace(i18n.RESOURCE_ON_RESOURCE, [ insert1, insert2]);
          alertString += lang.replace(i18n.INSTANCE_STOPPED_ON_SERVERS, [ appOnServerDisplayName ]) + "<br>";
        }
        if (alerts.unknown && alerts.unknown.length > 0) {
          var spanOpen = '<span dir="' + utils.getStringTextDirection(appOnServer.name) + '" >';
          alertString += lang.replace(i18n.UNKNOWN_STATE, [ spanOpen + appOnServer.name + spanClose ]);
        }
      }
      return alertString;
    },

    hostOrRuntimeorClusterAlertProcessor: function(alerts){
      var alertString = '';
      if (alerts && alerts.count > 0) {
        var uniqueFunction = function (value, index, self) { 
          return self.indexOf(value) === index;
        };
        if (alerts.app && alerts.app.length > 0) {
          var serverNames = [];
          for ( var a in alerts.app) {
            for ( var j in alerts.app[a].servers) {
              serverNames.push(alerts.app[a].servers[j].substring(alerts.app[a].servers[j].lastIndexOf(',') + 1));
            }
          }
          var uniqueServerNames = serverNames.filter( uniqueFunction );
          if (uniqueServerNames.length < 3) {
            alertString += lang.replace(i18n.NOT_ALL_APPS_RUNNING, [ uniqueServerNames ]) + "<br>";
          } else {
            alertString += lang.replace(i18n.NOT_ALL_APPS_RUNNING_SERVERS, [ uniqueServerNames.length ]) + "<br>";
          }
        }
        if (alerts.unknown && alerts.unknown.length > 0) {
          var serverNames = [];
          for ( var s in alerts.unknown) {
            serverNames.push(alerts.unknown[s].id.substring(alerts.unknown[s].id.lastIndexOf(',') + 1));
          }
          var uniqueServerNames = serverNames.filter( uniqueFunction );
          if (uniqueServerNames.length < 3) {
            alertString += lang.replace(i18n.UNKNOWN_STATE, [ uniqueServerNames ]) + "<br>";
          } else {
            alertString += lang.replace(i18n.UNKNOWN_STATE_SERVERS, [ uniqueServerNames.length ]) + "<br>";
          }
        }
      }
      return alertString;
    },
    createAlertBox : function(cp) {
      var me = this;
      var originalOpen = me.open;
      var alertByAnotherCard = false;
      if (me.open) {
        // check if the alert is already opened by another card
        var alertCpWidget = registry.byId(me.alertCpId);
        if (alertCpWidget) {
          // since alert content is shared by all cards,
          // handle the cases that after the initial open of this alert,
          // - your card closes the alert content
          // - another card opens and closes the alert content 
          // - another card opens the alert 
          var cssClass = alertCpWidget.get("class");
          if (cssClass.indexOf("cardFieldDisplayNone") > 0) {
            alertByAnotherCard = true;
          } else {
            var alertCpParent = alertCpWidget.getParent().id;
            var alertIconParent = registry.byId(me.id + "alertIcon").getParent().id;
            if (alertCpParent !== alertIconParent) {
              // handle the case that another card opens the alert after you
              alertByAnotherCard = true;
            } else {         
              // normal case that you opened the initial alert
              alertCpWidget.set("class",  "alertContentPane cardFieldDisplayNone");
            }
          }
        }
        me.open = !me.open;
      } 
      if (!originalOpen || alertByAnotherCard) {
        if (!me.alertCpId) {
          // create an alertCp for each view. For collection view, the id will be
          // collection-resourceType-card-alertCp. For object view, the id will be
          // id-Card-alertCp
//        var index = me.id.indexOf("-card-");
//        if (index < 0) {
//        index = me.id.indexOf("-Card-");
//        }
//        me.alertCpId = me.id.substring(0, index + 6) + "alertCp";
          // with existing tests, need to make one alertCp for all cards
          me.alertCpId = "alertCp";
        }
        if (registry.byId(me.alertCpId)) {
          var alertCpWidget = registry.byId(me.alertCpId);
          if (alertCpWidget) {
            alertCpWidget.set("class",  "alertContentPane");
            alertCpWidget.set("content", this.alertString);
            alertCpWidget.placeAt(cp);
            me.open = !me.open;
          }
        } else {
          var alertCp = new ContentPane({
            id : me.alertCpId, 
            baseClass : "alertContentPane",
            content : this.alertString,
            tabindex : '0'
          });
          alertCp.placeAt(cp);

          me.open = !me.open;
          //var alertCpDOM = dom.byId(me.id + "alertCp");
          var alertCpDOM = dom.byId(me.alertCpId);
          if (alertCpDOM) {
            // FF would not generate a focusout event when the alert pane
            // is destroyed. Hence, need to remove existing handlers for the
            // previous alert pane.
            // Chrome would generate a focusout event when the alert pane is
            // destroyed.
            if (me.alertFocusHandler) {
              me.alertFocusHandler.remove();
            }
            me.alertFocusHandler = on(alertCpDOM, "focus", function(evt) {
              if (me.alertKeydownHandler) {
                me.alertKeydownHandler.remove();
              }
              me.alertKeydownHandler = on(alertCpDOM, "keydown", function(evt) {
                if (evt.keyCode === keys.ENTER || evt.keyCode === keys.SPACE) {
                  me.alertBox(cp, this.resource, alertString);
                }
              });
            });
            on(alertCpDOM, "focusout", function() {
              if (me.alertKeydownHandler) {
                me.alertKeydownHandler.remove();
                me.alertKeydownHandler = undefined;
              }
              if (me.alertFocusHandler) {
                me.alertFocusHandler.remove();
                me.alertFocusHandler = undefined;
              }
            });
          }
        }
      }
    }
  });
});