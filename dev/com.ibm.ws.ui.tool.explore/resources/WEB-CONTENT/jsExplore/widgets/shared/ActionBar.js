/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
define([ "dojo/_base/declare", "dojo/_base/window", "dojo/_base/lang", "dojo/dom-construct", "dojo/dom", "dojo/on",
         "dijit/registry", "dijit/form/Button", "dijit/form/DropDownButton",
         "jsExplore/utils/featureDetector", "js/widgets/ConfirmDialog", "jsExplore/utils/constants", "jsShared/utils/imgUtils",
         "jsExplore/resources/resourceManager", "jsExplore/widgets/shared/ActionButtons", "jsExplore/resources/Observer",
         "jsExplore/widgets/BreakAffinityDialog", "dijit/DropDownMenu", "dijit/MenuItem", "dijit/layout/ContentPane",
         "dijit/layout/BorderContainer", "jsExplore/utils/ID", "js/common/platform", "jsExplore/widgets/FilterBar",
         "dojo/i18n!../../nls/explorerMessages", "dojo/Deferred", "dojo/request",
         "dojo/dom-class",
         "dojo/domReady!" ],
         function(declare, window, lang, domConstruct, dom, on,
                 registry, Button, DropDownButton, featureDetector, ConfirmDialog, constants,
                 imgUtils, resourceManager, ActionButtons, Observer, BreakAffinityDialog, DropDownMenu,
                 MenuItem, ContentPane, BorderContainer,
                 ID, platform, filterBar, i18n, Deferred, request, domClass
                 ) {

  return declare("ActionBar", [ BorderContainer, Observer ], {

    constructor : function() {
      // Define these resources inside the constructor so they become per-instance objects
      this.selectCount = 0;
      this.selectAllButton = null;
      this.selectNoneButton = null;
      this.currentFilter = null;
    },
    'class' : "actionBar",
    gutters: false,
    splitters: false,
    //doLayout : false,
    postCreate : function() {
      this.inherited(arguments);
      this.__buildActionBar();
    },

    processActionBar: function() {
      if (globalIsAdmin === false) {
        console.log('Action bar is disabled for users without the Administrator role as they cannot take actions');
        return;
      }
      var me = this;
      var curVal = me.get("selectMode");
      curVal = !curVal;
      me.set("selectMode", curVal);
      if (curVal) {
        me.domNode.style.display = "block";

        me.selectAllButton.set("disabled", false);
        me.selectNoneButton.set("disabled", false);
        me.selectAllButton.set("iconClass", "actionBarSelectAllIcon");
        me.selectAllButton.set("tabindex","0");
        me.selectAllButton.iconNode.innerHTML = imgUtils.getSVGSmall('selectAll');
        me.selectNoneButton.set("tabindex","-1");
        me.selectNoneButton.set("iconClass", "actionBarSelectNoneIconDisabled");
        me.selectNoneButton.iconNode.innerHTML = imgUtils.getSVGSmall('selectNone-disabled');
        domClass.add(me.selectNoneButton.id + "_label", "filterBarButtonLabelDisabled");
        domClass.remove(me.selectAllButton.id + "_label", "filterBarButtonLabelDisabled");
      } else {
        me.domNode.style.display = "none";
        me.resetActionBar();
      }

      me.resize();
      return curVal;
    },

    resetActionBar: function() {
      this.set("selectCount", 0);

      var disabled = true;
      if (this.get("selectMode")) {
        disabled = false;
      }

      this.selectAllButton.set("disabled", disabled);
      this.selectNoneButton.set("disabled", disabled);

      if (this.resetActions) {
        this.resetActions();
      }
    },

    setCurrentFilter : function(currFilter){
      this.set("currentFilter",currFilter);
    },

    enableDisableActionBarActionButtons: function() {
      if (this.grid) {
        this.__processGridActions();
      } else if (this.collectionView) {
        this.__processCollectionViewActions(this);
      }
    },

    /*
     * Enable or disable the action buttons when a selected resource changes its state or maintenance mode
     */
    createSelectedResourceObserver: function(resource) {
      resource.subscribe(this);
    },

    removeSelectedResourceObserver: function(resource) {
      resource.unsubscribe(this);
    },

    onStateChange: function(state) {
      this.enableDisableActionBarActionButtons();

    },

    onMaintenanceModeChange: function() {
      this.enableDisableActionBarActionButtons();
    },

    __buildActionBar: function() {
      var me = this;
      me.selectionPane = new ContentPane({
        id : this.id + "selectionPane",
        region : "left",
        title : me.resourceType,
        content : ""
      });
      domClass.add(me.selectionPane.domNode, "actionBarSelection");
      var regionLocation = platform.isPhone() ? "bottom" : "right";
      me.actionPane = new ContentPane({
        id : this.id + "actionPane",
        region : regionLocation,
        title : me.resourceType,
        content : ""
      });
      domClass.add(me.actionPane.domNode, "actionBarAction");

      me.selectedCount = new ContentPane({
        id : this.id + "selectedCount",
        content : lang.replace(i18n.RESOURCES_SELECTED, [ me.selectCount ])
      });
      domClass.add(me.selectedCount.domNode, "actionBarSelectionSelectedCount");


      me.watch("selectCount", function(name, oldValue, newValue) {
        me.selectedCount.set("content", lang.replace(i18n.RESOURCES_SELECTED, [ newValue ]));
        var totalObject = 0;
        if (me.resourceFilters) { // lavena: to be passed in from FilterBar.js
          totalObject = me.resourceFilters.get(me.get("currentFilter")).obj.number;
        } else if (me.grid){
          totalObject = me.grid.rowCount();
        }
        if (newValue === totalObject) {
          me.selectAllButton.set("iconClass", "actionBarSelectAllIconDisabled");
          me.selectAllButton.iconNode.innerHTML = imgUtils.getSVGSmall('selectAll-disabled');
          me.selectAllButton.set("tabindex","-1");
          me.selectNoneButton.set("iconClass", "actionBarSelectNoneIcon");
          me.selectNoneButton.iconNode.innerHTML = imgUtils.getSVGSmall('selectNone');
          me.selectNoneButton.set("tabindex","0");
          domClass.add(me.selectAllButton.id + "_label", "filterBarButtonLabelDisabled");
          domClass.remove(me.selectNoneButton.id + "_label", "filterBarButtonLabelDisabled");
        } else if (newValue == 0){
          me.selectAllButton.set("iconClass", "actionBarSelectAllIcon");
          me.selectAllButton.iconNode.innerHTML = imgUtils.getSVGSmall('selectAll');
          me.selectAllButton.set("tabindex","0");
          me.selectNoneButton.set("iconClass", "actionBarSelectNoneIconDisabled");
          me.selectNoneButton.iconNode.innerHTML = imgUtils.getSVGSmall('selectNone-disabled');
          me.selectNoneButton.set("tabindex","-1");
          domClass.add(me.selectNoneButton.id + "_label", "filterBarButtonLabelDisabled");
          domClass.remove(me.selectAllButton.id + "_label", "filterBarButtonLabelDisabled");
        } else {
          me.selectAllButton.set("iconClass", "actionBarSelectAllIcon");
          me.selectAllButton.iconNode.innerHTML = imgUtils.getSVGSmall('selectAll');
          me.selectAllButton.set("tabindex","0");
          me.selectNoneButton.set("iconClass", "actionBarSelectNoneIcon");
          me.selectNoneButton.iconNode.innerHTML = imgUtils.getSVGSmall('selectNone');
          me.selectNoneButton.set("tabindex","0");
          domClass.remove(me.selectNoneButton.id + "_label", "filterBarButtonLabelDisabled");
          domClass.remove(me.selectAllButton.id + "_label", "filterBarButtonLabelDisabled");
        }
      });

      me.selectionPane.addChild(me.selectedCount);
      me.addChild(me.selectionPane);

      me.__createSelectButtons();
      me.__createActions();
      // If the user has passed in actions, use those instead of the defaults
      if (me.actions && me.resourceType !== 'runtimesOnHost' && me.resourceType !== 'runtimes') {
        me.actions.forEach(function(singleActionPane) {
          me.actionPane.addChild(singleActionPane);
        });
        me.addChild(me.actionPane);
      }
      me.domNode.style.display = "none";
    },

    __createSelectButtons: function() {
      // Add the selection options in the bar, since they are always the same
      this.selectAllButton = this.__createSelectButton(false);
      this.selectionPane.addChild(this.selectAllButton);
      domClass.add(this.selectAllButton.domNode, "actionBarSelectionButton");
      domClass.add(this.selectAllButton.domNode, "actionBarSelectionMiddleButton");

      this.selectNoneButton = this.__createSelectButton(true);
      this.selectionPane.addChild(this.selectNoneButton);
      domClass.add(this.selectNoneButton.domNode, "actionBarSelectionButton");
      domClass.add(this.selectNoneButton.domNode, "actionBarSelectionRightButton");
    },

    __createSelectButton: function(isSelectNone) {
      var me = this;
      var selectButton = new Button({
        id : (function() {
          if (isSelectNone) {
            return me.id + "selectNoneButton";
          } else {
            return me.id + "selectAllButton";
          }
        }()),
        label : (function() {
          if (isSelectNone) {
            return i18n.SELECT_NONE;
          } else {
            return i18n.SELECT_ALL;
          }
        }()),
        iconClass : (function() {
          if (isSelectNone) {
            return "actionBarSelectNoneIconDisabled";
          } else {
            return "actionBarSelectAllIcon";
          }
        }()),
        showLabel : true,
        disabled : "disabled", // to prevent the tab to go there until wrench icon is clicked
        onClick : function() {
          if (me.grid) {
            if (isSelectNone) {
              me.grid.deSelectAllRows();
            } else {
              me.grid.selectAllRows();
            }
          } else if (me.collectionView) {
            me.collectionView.setCardSelection(!isSelectNone);
          }
          if (isSelectNone) {
            me.selectAllButton.set("iconClass", "actionBarSelectAllIcon");
            me.selectAllButton.iconNode.innerHTML = imgUtils.getSVGSmall('selectAll');
            me.selectAllButton.set("tabindex","0");
            me.selectNoneButton.set("tabindex","-1");
            me.selectNoneButton.set("iconClass", "actionBarSelectNoneIconDisabled");
            me.selectNoneButton.iconNode.innerHTML = imgUtils.getSVGSmall('selectNone-disabled');
            domClass.add(me.selectNoneButton.id + "_label", "filterBarButtonLabelDisabled");
            domClass.remove(me.selectAllButton.id + "_label", "filterBarButtonLabelDisabled");
          } else {
            me.selectAllButton.set("iconClass", "actionBarSelectAllIconDisabled");
            me.selectAllButton.iconNode.innerHTML = imgUtils.getSVGSmall('selectAll-disabled');
            me.selectAllButton.set("tabindex","-1");
            me.selectNoneButton.set("tabindex","0");
            me.selectNoneButton.set("iconClass", "actionBarSelectNoneIcon");
            me.selectNoneButton.iconNode.innerHTML = imgUtils.getSVGSmall('selectNone');
            domClass.add(me.selectAllButton.id + "_label", "filterBarButtonLabelDisabled");
            domClass.remove(me.selectNoneButton.id + "_label", "filterBarButtonLabelDisabled");
          }
        }
      });
      return selectButton;
    },

    __createActions: function() {
      // Holds the list of actions for this resource collection
      var me = this;
      var actionList = [];
      var resetActions = function() {
      }; // Intentional empty function
      var showLabel = !platform.isPhone(); // Hide labels on the phone

      // The panel is the root object for the drop down. The action is the button
      // that actually does the work we've requested, whereas the actionButton is
      // the ActionItem object displayed on the FilterBar (the one the drop down
      // hangs from)
      if (this.resourceType !== 'runtimesOnHost') {

        // Common behaviour for all event buttons
        var setEventActions = function(actionBarButton, actionButton) {
          on(actionBarButton.domNode, "keydown", function(evt) {
            if (evt.keyCode === 40) { // for down arrow key
              actionButton.focus();
            }
          });
          if (actionButton) {
            on(actionButton.domNode, "focus", function() {
              domClass.add(actionButton.domNode, "actionBarButtonFocused");
            });
            on(actionButton.domNode, "focusout", function() {
              domClass.remove(actionButton.domNode, "actionBarButtonFocused");
            });
          }
        };

        // The set of actions supported on the filter bar is based on the resource type
        // For example, we only build deploy action button for hosts when not on a phone
        if (this.resourceType === 'hosts') {
          if (!platform.isPhone()) {
            // Build the "DEPLOY" button
            var buttonId = ID.dashDelimit(this.id, ID.getActionDeployButton());
            var actionDeployButton = this.__createActionButton(buttonId, i18n.DEPLOY_SERVER_PACKAGE, 'deployActionIcon',
                   'deployActionDisabledIcon', showLabel, 'actionBarButton actionBarDeployButton',
                   function() {
                     // We should deploy to any visible
                     var breadcrumbWidget = registry.byId(ID.getBreadcrumbContainer());
                     var hostList = [];
                     if (me.collectionView) {
                       me.collectionView.cardList.query({
                         visible : true,
                         selected : true
                       }).forEach(function(card) {
                         hostList.push(card.id);
                       });
                     } else if (me.grid) {
                       for (var i = 0; i < me.grid.select.row.getSelected().length; i++) {
                         var id = me.grid.row(parseInt(me.grid.select.row.getSelected()[i])).id;
                         var resource = me.grid.store.query({"id":id})[0].actions;
                         hostList.push(resource.id);
                       }
                     }

                     if (hostList.length > 0) {
                       // The proxy page is where we'll put our content. We need to blast it from inside
                       // the iframe, so we need a global function in the main index page to allow that
                       var iframeNode = domConstruct.create('iframe', {
                         style : 'border: none; overflow: hidden;',
                         width : '100%',
                         height : '100%',
                         src : '../deploy-1.0/index.jsp?hostlist=' + hostList,
                         id : 'deployFrame',
                         title : 'deployFrame'
                       });

                       var refNode = registry.byId(ID.getHostProxyPage());
                       if (refNode) {
                         refNode.set('content', iframeNode);
                         breadcrumbWidget.selectChild(refNode);
                       } else {
                         refNode = new ContentPane({
                           id : ID.getHostProxyPage(),
                           title : i18n.DEPLOY_SERVER_PACKAGE,
                           'style' : 'height: 100%;',
                           content : iframeNode
                         });
                         breadcrumbWidget.addChild(refNode);
                       }
                       breadcrumbWidget.set('style', 'overflow: hidden;');

                       // We need to dynamically resize the pane because Firefox doesn't handle this very well
                       // add height property causes problem in webkit.  However without height, webkit would show scroll bar.  The fix
                       // is to not set height in webkit and hide iframe scrollbar in webkit.
                       iframeNode.onload = function() {
                         var isWebkit = 'WebkitAppearance' in document.documentElement.style;
                         if ( !isWebkit )
                           refNode.set('style', 'height: ' + iframeNode.contentDocument.body.scrollHeight + 'px');
                         refNode.resize();
                       };
                     } else {
                       // This is unlikely to occur since the button is disabled when no selection is made,
                       // but we can err on the side of caution here.
                       alert(i18n.NO_HOSTS_SELECTED);
                     }
                   }
            );
//            actionDeployButton.iconNode.innerHTML = imgUtils.getSVG('deploy');
            actionList.push(actionDeployButton);

            me.deployFeatureAvailable = featureDetector.isDeployAvailable();

            if (me.collectionView) {
              me.__observeCardList();
            }

            resetActions = function() {
              actionDeployButton.set('disabled', true);
              actionDeployButton.set('iconClass', 'deployActionDisabledIcon');
              if (actionMoreButton) {
                actionMoreButton.set('disabled', true);
                actionMoreButton.set('iconClass', 'moreActionDisabledIcon');
              }
            };
          }

          // Build the "More" button and drop down menu
          var actionMoreButton = me.__createActionMoreButton(this.id, "host", showLabel);
          actionList.push(actionMoreButton);
        } else {
          // Build the "START" button
          var getStartPanelContent = lang.hitch(this, function() {
            switch (this.resourceType) {
            case 'applications':
            case 'appOnServer':
            case 'appsOnCluster':
            case 'appsOnServer':
            case 'appInstancesByCluster':
              return i18n.START_SELECTED_APPS;
            case 'clusters':
              return i18n.START_SELECTED_CLUSTERS;
            case 'servers':
            case 'serversOnCluster':
            case 'serversOnHost':
            case 'serversOnRuntime':
              return i18n.START_SELECTED_SERVERS;
            default:
              console.error('Unable to determine start panel content for type: ' + type);
            }
          });

          var startButtonId = ID.dashDelimit(this.id, ID.getActionStartButton());
          // createActionDropDownButton is the prototype to display disabled button help
          //var actionStartButton = this.__createActionDropDownButton(startButtonId, i18n.START, 'startActionIcon',
          var actionStartButton = this.__createActionButton(startButtonId, i18n.START, 'startActionIcon',
                 'startActionDisabledIcon', showLabel, 'actionBarButton actionBarThreeButtonsButtonThree',
                  function() {
                    var confirmAndStart = function() {
                      var doStart = function(resource) {
                        resource.start();
                      };
                      if (me.grid) {
                        for (var i = 0; i < me.grid.select.row.getSelected().length; i++) {
                          var id = me.grid.row(parseInt(me.grid.select.row.getSelected()[i])).id;
                          var resource = me.grid.store.query({"id":id})[0].actions;
                          doStart(resource);
                        }
                      } else if (me.collectionView) {
                        me.collectionView.cardList.query({
                          visible : true,
                          selected : true
                        }).forEach(function(cardHolder) {
                            doStart(cardHolder.card.resource);
                        });
                      }
                    };
                    me.__confirmAction(startButtonId, confirmAndStart, getStartPanelContent(), ID.dashDelimit(this.id, ID.getStartAction()), i18n.START);
                  }
          );
          actionStartButton.iconNode.innerHTML = imgUtils.getSVGSmall('actionBar-start');
          actionList.push(actionStartButton);

          // Build the "RESTART" button
          var getRestartPanelContent = lang.hitch(this, function() {
            switch (this.resourceType) {
            case 'applications':
            case 'appsOnCluster':
            case 'appsOnServer':
            case 'appInstancesByCluster':
              return i18n.RESTART_SELECTED_APPS;
            case 'clusters':
              return i18n.RESTART_SELECTED_CLUSTERS;
            case 'servers':
            case 'serversOnCluster':
            case 'serversOnHost':
            case 'serversOnRuntime':
              return i18n.RESTART_SELECTED_SERVERS;
            default:
              console.error('Unable to determine stop panel content for type: ' + type);
            }
          });

          var restartButtonId = ID.dashDelimit(this.id, ID.getActionRestartButton());
          var actionRestartButton = this.__createActionButton(restartButtonId, i18n.RESTART, 'restartActionIcon',
                 'restartActionDisabledIcon', showLabel, 'actionBarButton actionBarThreeButtonsButtonThree',
                  function() {
                    var confirmAndRestart = function() {
                      var doRestart = function(resource) {
                        resource.restart();
                      };
                      var adminCenterServer = null;
                      var adminCenterCluster = null;
                      if (me.grid && me.resourceType === "servers") {
                        for (var i = 0; i < me.grid.select.row.getSelected().length; i++) {
                          var id = me.grid.row(parseInt(me.grid.select.row.getSelected()[i])).id;
                          var resource = me.grid.store.query({"id":id})[0].actions;
                          if (adminCenterServer === null) {
                            if (resource.isAdminCenterServer) {
                              adminCenterServer = resource;
                            } else {
                              doRestart(resource);
                            }
                          } else {
                            doRestart(resource);
                          }
                        }
                      } else if (me.collectionView && (me.resourceType === "servers" || me.resourceType.indexOf("serversOn") === 0)) {
                        me.collectionView.cardList.query({
                          visible : true,
                          selected : true
                        }).forEach(function(cardHolder) {
                          if (cardHolder.card.resource.isAdminCenterServer) {
                            // If the Admin Center server was selected, grab a reference but do not stop it, we need to initiate the stop AFTER all others have stopped
                            adminCenterServer = cardHolder.card.resource;
                          } else {
                            doRestart(cardHolder.card.resource);
                          }
                        });
                      } else if (me.grid && me.resourceType === "clusters") {
                        for (var i = 0; i < me.grid.select.row.getSelected().length; i++) {
                          var id = me.grid.row(parseInt(me.grid.select.row.getSelected()[i])).id;
                          var resource = me.grid.store.query({"id":id})[0].actions;
                          if (adminCenterCluster === null) {
                            resource.resourceManager.getServer(resource.servers.list).then(function(servers) {
                              for (var i = 0; i < servers.length; i++) {
                                if (servers[i].isAdminCenterServer) {
                                  adminCenterCluster = resource;
                                  break;
                                }
                              }
                              if (adminCenterCluster === null) {
                                doRestart(resource);
                              }
                            });
                          } else {
                            doRestart(resource);
                          }
                        }
                      } else if (me.collectionView && me.resourceType === "clusters") {
                        me.collectionView.cardList.query({
                          visible : true,
                          selected : true
                        }).forEach(function(cardHolder) {
                          var resource = cardHolder.card.resource;
                          if (adminCenterCluster === null) {
                            resource.resourceManager.getServer(resource.servers.list).then(function(servers) {
                              for (var i = 0; i < servers.length; i++) {
                                if (servers[i].isAdminCenterServer) {
                                  adminCenterCluster = resource;
                                  break;
                                }
                              }
                              if (adminCenterCluster === null) {
                                doRestart(resource);
                              }
                            });
                          } else {
                            doRestart(resource);
                          }
                        });
                      } else if (me.grid) {
                        for (var i = 0; i < me.grid.select.row.getSelected().length; i++) {
                          var id = me.grid.row(parseInt(me.grid.select.row.getSelected()[i])).id;
                          var resource = me.grid.store.query({"id":id})[0].actions;
                          doRestart(resource);
                        }
                      } else if (me.collectionView) {
                        me.collectionView.cardList.query({
                          visible : true,
                          selected : true
                        }).forEach(function(cardHolder) {
                          doRestart(cardHolder.card.resource);
                        });
                      }

                      // If a cluster hosting Admin Center server was selected, now that all other clusters have stopped, prompt
                      if (adminCenterServer || adminCenterCluster) {
                        var restartAdminCenterDialog = new ConfirmDialog({
                          id : ID.getWontRestartACServer(),
                          title : i18n.RESTART_AC_TITLE,
                          confirmDescriptionIcon : imgUtils.getSVGSmallName('status-alert'),
                          confirmDescription : lang.replace(i18n.RESTART_AC_DESCRIPTION, [ adminCenterServer ? adminCenterServer.name : adminCenterCluster.name]),
                          confirmMessage : (adminCenterServer ? i18n.RESTART_AC_MESSAGE : i18n.RESTART_AC_CLUSTER_MESSAGE),
                          okFunction : function() {
                          }
                        });
                        restartAdminCenterDialog.placeAt(window.body());
                        restartAdminCenterDialog.startup();
                        restartAdminCenterDialog.show();
                      }
                    };
                    me.__confirmAction(restartButtonId, confirmAndRestart, getRestartPanelContent(), ID.dashDelimit(this.id, ID.getRestartAction()), i18n.RESTART);
                  }
          );
          actionRestartButton.iconNode.innerHTML = imgUtils.getSVGSmall('actionBar-restart');
          actionList.push(actionRestartButton);

          // Build the "STOP" button
          var getStopPanelContent = lang.hitch(this, function() {
            switch (this.resourceType) {
            case 'applications':
            case 'appsOnCluster':
            case 'appsOnServer':
            case 'appInstancesByCluster':
              return i18n.STOP_SELECTED_APPS;
            case 'clusters':
              return i18n.STOP_SELECTED_CLUSTERS;
            case 'servers':
            case 'serversOnCluster':
            case 'serversOnHost':
            case 'serversOnRuntime':
              return i18n.STOP_SELECTED_SERVERS;
            default:
              console.error('Unable to determine stop panel content for type: ' + type);
            }
          });

          var stopButtonId = ID.dashDelimit(this.id, ID.getActionStopButton());
          var actionStopButton = this.__createActionButton(stopButtonId, i18n.STOP, 'stopActionIcon',
                 'stopActionDisabledIcon', showLabel, 'actionBarButton actionBarThreeButtonsButtonThree',
                  function() {
                    var confirmAndStop = function() {
                      var doStop = function(resource) {
                        resource.stop();
                      }
                      var confirmStop = function(resource) {
                        if (resource.isAdminCenterServer) {
                          ActionButtons.displayStopPopupDialog(resource, function() {
                            doStop(resource);
                          });
                        } else if (resource.type === "cluster") {
                          var isStop = false;
                          resource.servers.list.forEach(function(eachServer){
                            resource.resourceManager.getServer(eachServer).then(function(server){
                              isStop = server.isAdminCenterServer;
                              return isStop;
                            }).then(function(isStop){
                              if(isStop){
                                ActionButtons.displayClusterStopPopupDialog(resource, function() {
                                  doStop(resource);
                                });
                              } else {
                                doStop(resource);
                              }
                            });
                          });
                        } else {
                          doStop(resource);
                        }
                      };

                      if (me.grid) {
                        for (var i = 0; i < me.grid.select.row.getSelected().length; i++) {
                          var id = me.grid.row(parseInt(me.grid.select.row.getSelected()[i])).id;
                          var resource = me.grid.store.query({"id":id})[0].actions;
                          confirmStop(resource);
                        }
                      } else if (me.collectionView) {
                        me.collectionView.cardList.query({
                          visible : true,
                          selected : true
                        }).forEach(function(cardHolder) {
                          confirmStop(cardHolder.card.resource);
                        });
                      }
                    };
                    me.__confirmAction(stopButtonId, confirmAndStop, getStopPanelContent(), ID.dashDelimit(this.id, ID.getStopAction()), i18n.STOP);
                  }
          );
          actionStopButton.iconNode.innerHTML = imgUtils.getSVGSmall('actionBar-stop');
          actionList.push(actionStopButton);

          if (me.resourceType === 'servers' || me.resourceType === 'serversOnHost' || me.resourceType === 'serversOnRuntime' || me.resourceType === 'serversOnCluster') {
            // Build the "More" button and drop down menu
            var actionMoreButton = me.__createActionMoreButton(this.id, "server", showLabel); //skz todo add this to runtime and host views
            actionList.push(actionMoreButton);
            me.updateMMLabels();
          }

          if (me.resourceType === 'clusters') {
            // Build the "More" button and drop down menu
            var actionMoreButton = me.__createActionMoreButton(this.id, "cluster", showLabel);
            actionList.push(actionMoreButton);
          }

          if (me.collectionView) {
            me.__observeCardList();
          }

          resetActions = function() {
            actionStartButton.set('disabled', true);
            actionStartButton.set('iconClass', 'startActionDisabledIcon');
            actionStopButton.set('disabled', true);
            actionStopButton.set('iconClass', 'stopActionDisabledIcon');
            actionRestartButton.set('disabled', true);
            actionRestartButton.set('iconClass', 'restartActionDisabledIcon');
            if (actionMoreButton) {
              actionMoreButton.set('disabled', true);
              actionMoreButton.set('iconClass', 'moreActionDisabledIcon');
            }
          };
        }
      }

      this.actions = actionList;
      this.resetActions = resetActions;
      if (actionStartButton) {
        this.actionStartButton = actionStartButton;
      }
      if (actionStopButton) {
        this.actionStopButton = actionStopButton;
      }
      if (actionRestartButton) {
        this.actionRestartButton = actionRestartButton;
      }
      if (actionDeployButton) {
        this.actionDeployButton = actionDeployButton;
      }
      if (actionMoreButton) {
        this.actionMoreButton = actionMoreButton;
      }
    },

    /**
     * Enable or disable maintenance mode on the selected resources
     */
    __enableDisableMaintenanceModeAction: function(me, resourceType) {
      var me = this;
      var doMaintenanceMode = function(ids, maintenanceMode, resourceType, resources, optionalParams) {
        if ( maintenanceMode != constants.MAINTENANCE_MODE_IN_MAINTENANCE_MODE ) {
          me.__collectionOperation(ids, 'enterMaintenanceMode', resourceType, optionalParams).then(function(response) {
            // ignore response;
          });
        }
        else {
          me.__collectionOperation(ids, 'exitMaintenanceMode', resourceType, optionalParams).then(function(response) {
            // ignore response
          });
        }
      };

      this.__displayEnableDisableMaintenanceModePopupDialog(me, resourceType, function(ids, maintenanceMode, resourceType, resources) {
        if ( maintenanceMode === constants.MAINTENANCE_MODE_IN_MAINTENANCE_MODE ) {
          console.log ("submit disable maintenance mode.");
          doMaintenanceMode(ids, maintenanceMode, resourceType, resources);
        } else {
          var breakAffinityToggleButton = dom.byId(ID.getBreakAffinityToggleButton());
          console.log ("submit enable maintenance mode on host with break affinity set to " + breakAffinityToggleButton.checked);
          var params = {};
          params.maintainAffinity = !breakAffinityToggleButton.checked;
          params.force = false;
          doMaintenanceMode(ids, maintenanceMode, resourceType, resources, params);
        }
      });
    },

    /**
     * Constructs and displays the enable/disable maintenance mode dialog.
     */
    __displayEnableDisableMaintenanceModePopupDialog: function(me, resourceType, actionFunction) {
      if (registry.byId(ID.getAskEnableDisableMaintenanceMode())) {
        registry.byId(ID.getAskEnableDisableMaintenanceMode()).destroy();
      }
      // Run query to get list of ids
      var maintenanceMode = '';
      var ids = [];
      var serversInHosts = 0;
      var serversInMM = 0;
      var resources = [];
      if (me.collectionView) {
        me.collectionView.cardList.query({
          visible : true,
          selected : true
        }).forEach(function(cardHolder) {
          var resource = cardHolder.card.resource;
          maintenanceMode = cardHolder.card.resource.maintenanceMode;
          ids.push(resource.id);
          resources.push(resource);
          if ( resource.type === 'host' ) {
            serversInHosts += resource.servers.list.length;
            serversInMM += resource.servers.inMaintenanceMode;
          }
        });
      } else if (me.grid) {
        for (var i = 0; i < me.grid.select.row.getSelected().length; i++) {
          var id = me.grid.row(parseInt(me.grid.select.row.getSelected()[i])).id;
          var resource = me.grid.store.query({"id":id})[0].actions;
          maintenanceMode = resource.maintenanceMode;
          ids.push(resource.id);
          resources.push(resource);
          if (resource.type === "host") {
            serversInHosts += resource.servers.list.length;
            serversInMM += resource.servers.inMaintenanceMode;
          }
        }
      }

      // default is to enable mm on server
      var description = i18n.ENABLE_MAINTENANCE_MODE_SERVERS_DESCRIPTION;
      var title = i18n.ENABLE_MAINTENANCE_MODE_DIALOG_TITLE; // title is either enable or disable
      var hasOptions = true ; // break affinity option is only available for enable mm
      var question = i18n.BREAK_AFFINITY_LABEL;
      var btnLabel = i18n.ENABLE_MAINTENANCE_MODE_DIALOG_ENABLE_BUTTON_LABEL;
      if ( maintenanceMode === constants.MAINTENANCE_MODE_IN_MAINTENANCE_MODE) {
        // do not pop up dialog, do the action directly
        actionFunction(ids, maintenanceMode, resourceType, resources);
        return false;
      } else {
        // enable MM on host labels
        if ( 'host' === resourceType ) {
          description = lang.replace(i18n.ENABLE_MAINTENANCE_MODE_HOST_DESCRIPTION, [ serversInHosts ]) ;
          if (serversInMM > 0 ) description += "<br><br>" + lang.replace(i18n.MAINTENANCE_MODE_SERVERS_IN_MAINTENANCE_MODE, [serversInMM]);

        }
      }

      var confirmEnableMaintenanceDialog = new BreakAffinityDialog({
        id: ID.getAskEnableDisableMaintenanceMode(),
        title : title,
        confirmDescriptionIcon : "",
        confirmDescription : description,
        confirmMessage : '',
        question: question,
        parseOnLoad: false,
        hasOptions: hasOptions,
        helpLinkImg : imgUtils.getSVGSmallName('help'),
        helpLinkURL : constants.MAINTENANCE_MODE_HELP_LINK,
        confirmButtonLabel : btnLabel,
        okFunction : function() {
          actionFunction(ids, maintenanceMode, resourceType, resources);
        }
      });
      confirmEnableMaintenanceDialog.placeAt(window.body());
      confirmEnableMaintenanceDialog.startup();
      confirmEnableMaintenanceDialog.show();
      return false;
    },

    __startCleanAction: function(me, resourceType, buttonId) {

      var startCleanMesage;
      switch(resourceType){
        case 'cluster':
          startCleanMesage = i18n.START_CLEAN_SELECTED_CLUSTERS;
          break;
        case 'server':
          startCleanMesage = i18n.START_CLEAN_SELECTED_SERVERS;
          break;
        default:
          console.error('Unable to determine start --clean panel content for type: ' + resourceType);
          break;
      }

      var confirmAndStartClean = function(){
        var doStartClean = function(resource){
          resource.startClean();
        }

        if (me.grid) {
          for (var i = 0; i < me.grid.select.row.getSelected().length; i++) {
            var id = me.grid.row(parseInt(me.grid.select.row.getSelected()[i])).id;
            var resource = me.grid.store.query({"id":id})[0].actions;
            doStartClean(resource);
          }
        } else if (me.collectionView) {
          me.collectionView.cardList.query({
            visible : true,
            selected : true
          }).forEach(function(cardHolder) {
            var resource = cardHolder.card.resource;
            doStartClean(resource);
          });
        }
      };

      me.__confirmAction(buttonId, confirmAndStartClean, startCleanMesage, ID.dashDelimit(buttonId, ID.getStartCleanAction()), i18n.START_CLEAN);
    },

    /**
     * Submit enterMaintenanceMode or exitMaintenanceMode on selected resources
     *
     */
    __collectionOperation: function(ids, operation, resourceType, optionalParams) {
      // Fire off an 'acting' event
      if (operation === 'enterMaintenanceMode') {
        // do nothing
      } else if (operation === 'exitMaintenanceMode') {
        // do nothing
      } else {
        console.error('__hostOperation, unknown operation: ' + operation);
      }
      var first = ids.shift();
     //collective/v1/hosts/hostID/enterMaintenanceMode
      var  url = '/ibm/api/collective/v1/hosts/' + encodeURIComponent(first) + '/' + operation;
      if ( resourceType === 'server')
        url = '/ibm/api/collective/v1/servers/' + encodeURIComponent(first) + '/' + operation;
      var additionalResources = ids.join(constants.MAINTENANCE_MODE_REST_HEADER_LIST_SEPARATOR);
      var  options = {
            handleAs: 'json',
            headers: {
              'Content-type': 'application/json',
              'collective.additionalIDs': additionalResources
            },
            data: (optionalParams === null || optionalParams === undefined)? "" : JSON.stringify(optionalParams)
        };

      console.log("********************POST: URL = [" + url +"], data = [" + JSON.stringify(options) + "]" );
      var deferred = new Deferred();

      request.post(url, options).then(function(response) {
        var opSuccessful = false;
        console.log(response);
       // ignore failures for now
        deferred.resolve(response, true);
      },
      function(err) {
        console.log("Error " + err.response.status + " occurred when requesting " + url + ": ", err);

        // ignore failures for now
        deferred.resolve(err, true);
      });

      return deferred;
    },

    /**
     * Enables the start clean button only if there are resources stopped and none in the started state
     */
    enableDisableStartCleanButton : function (startCount, stopCount, unknownCount, autoScaledInMMCount) {
      var startCleanButton = registry.byId(ID.dashDelimit(this.id, ID.getStartCleanActionMenuItem()));
      if(startCleanButton){
        var disabled = true;
        if(startCount === 0 && autoScaledInMMCount === 0 && (stopCount > 0 || unknownCount > 0)){
          disabled = false;
        }
        startCleanButton.set('disabled', disabled);
      }
    },

    /**
     * Updates maintenance mode label
     */
    updateMMLabels : function () {
      //var collectionView = this;
      var mmActionMenuItem = registry.byId(ID.dashDelimit(this.id, ID.getMaintenanceActionMenuItem()));
      var inMM = 0;
      var notInMM = 0;
      if ( mmActionMenuItem ) {
        if (this.collectionView) {
          inMM = this.collectionView.getSelectedWithInMaintenanceModeCount();
          notInMM = this.collectionView.getSelectedWithNotInMaintenanceModeCount();
        } else if (this.grid) {
          inMM = this.grid.getSelectedWithInMaintenanceModeCount();
          notInMM = this.grid.getSelectedWithNotInMaintenanceModeCount();
        }

        if ( inMM > 0 && notInMM === 0 ){
          mmActionMenuItem.set("label", i18n.DISABLE_MAINTENANCE_MODE);
          mmActionMenuItem.set('disabled', false);
          mmActionMenuItem.set('class', 'dropDownActionButtons');
        }
        else if ( inMM === 0 && notInMM > 0 ) {
          mmActionMenuItem.set("label", i18n.ENABLE_MAINTENANCE_MODE);
          mmActionMenuItem.set('disabled', false);
          mmActionMenuItem.set('class', 'dropDownActionButtons');
        } else {
          mmActionMenuItem.set('disabled', true);
          mmActionMenuItem.set('class', 'dropDownActionButtonsDisabled');
        }
      }
    },

    __createActionButton: function(buttonId, buttonLabel, buttonEnabledIconClass, buttonDisabledIconClass, buttonShowLabel, buttonClasses, onClickAction) {
      var button = new Button({
        id : buttonId,
        label : buttonLabel,
        iconClass : buttonDisabledIconClass,
        showLabel : buttonShowLabel,
        'class' : buttonClasses,
        disabled : true,
        setEnabled: function() {
          this.set('disable', false);
          this.set('iconClass', buttonEnabledIconClass);
        },
        setDisabled: function() {
          this.set('disable', true);
          this.set('iconClass', buttonDisabledIconClass);
        },
        onClick: onClickAction
      });

      return button;
    },

    __createActionDropDownButton : function(buttonId, buttonLabel, buttonEnabledIconClass, buttonDisabledIconClass, buttonShowLabel, buttonClasses, onClickAction) {
      var actionDropDownMenu = new DropDownMenu({
        id : ID.dashDelimit(buttonId, "dropDownMenu"),
        name : lang.replace(i18n.MORE_BUTTON_MENU, [buttonId]),
        'aria-labelledby' : ID.dashDelimit(buttonId, "dropDown")
      });

      var actionDropDownMenuItem = new MenuItem({
        baseClass: 'dropDownActionButtons',
        id: ID.dashDelimit(buttonId, "dropDownMenuItem"),
        label: "Select running application to stop" //i18n.ENABLE_MAINTENANCE_MODE,
      });
      actionDropDownMenu.addChild(actionDropDownMenuItem);

      var actionDropDownButton = new DropDownButton({
        id : ID.dashDelimit(buttonId, "dropDownButton"),
        label : buttonLabel,
        iconClass : buttonDisabledIconClass,
        showLabel : buttonShowLabel,
        //'class' : buttonClasses,
        setEnabled: function() {
          this.set('disable', false);
          this.set('iconClass', buttonEnabledIconClass);
        },
        setDisabled: function() {
          this.set('disable', true);
          this.set('iconClass', buttonDisabledIconClass);
        },
        dropDown : actionDropDownMenu,
        baseClass : 'actionBarButton dijitButton'
      });
      actionDropDownMenu.on("open", function(evt) {
        var icon = actionDropDownButton.get("iconClass");
        if (icon.indexOf("DisabledIcon") > 0) {
          actionDropDownMenuItem.domNode.style.display = "";
        } else {
          actionDropDownMenuItem.domNode.style.display = "none";
          onClickAction();
        }
      });
      return actionDropDownButton;
    },

    __createActionMoreButton: function(buttonRootId, resourceType, showLabel) {
      var moreActionDropDownMenu = new DropDownMenu({
        id : ID.dashDelimit(buttonRootId, ID.getMoreActionDropDownMenu()),
        name : lang.replace(i18n.MORE_BUTTON_MENU, [buttonRootId]),
        'class' : 'moreActionDropDownMenu',
        'aria-labelledby' : ID.dashDelimit(buttonRootId, ID.getActionMoreButton())
      });

      if(resourceType === 'server' || resourceType === 'host'){
        var maintenanceModeMenuItem = new MenuItem({
          baseClass: 'dropDownActionButtons',
          id: ID.dashDelimit(buttonRootId, ID.getMaintenanceActionMenuItem()),
          label: i18n.ENABLE_MAINTENANCE_MODE,
          onClick: lang.hitch(this, function(){
            var actionMoreButton = registry.byId(ID.dashDelimit(buttonRootId, ID.getActionMoreButton()));
            actionMoreButton.closeDropDown();
            this.__enableDisableMaintenanceModeAction(this, resourceType);
          })
        });
        moreActionDropDownMenu.addChild(maintenanceModeMenuItem);
      }

      if(resourceType === 'server' || resourceType === 'cluster'){
        var startCleanMenuItem = new MenuItem({
          baseClass: 'dropDownActionButtons',
          id: ID.dashDelimit(buttonRootId, ID.getStartCleanActionMenuItem()),
          label: i18n.START_CLEAN,
          disabled: false,
          onClick: lang.hitch(this, function(){
            var actionMoreButton = registry.byId(ID.dashDelimit(buttonRootId, ID.getActionMoreButton()));
            actionMoreButton.closeDropDown();
            this.__startCleanAction(this, resourceType, ID.dashDelimit(buttonRootId, ID.getStartCleanActionMenuItem()));
          })
        })

        moreActionDropDownMenu.addChild(startCleanMenuItem);
      }


      var actionMoreButton = new DropDownButton({
        id : ID.dashDelimit(buttonRootId, ID.getActionMoreButton()),
        label : i18n.MORE,
        iconClass : 'moreActionDisabledIcon',
        showLabel : showLabel,
        dropDown : moreActionDropDownMenu,
        baseClass : 'actionBarButton dijitButton',
        disabled : true
      });

      return actionMoreButton;
    },

    __confirmAction: function(buttonId, operation, confirmationPaneContent, confirmButtonId, confirmationButtonLabel) {
        var confirmationDialog = new ConfirmDialog({
          id : ID.dashDelimit(buttonId, ID.getConfirmationPopup()),
          baseClass: "confirmDialog acDialog acDialogNoTitle",
          title : '',
          confirmDescriptionIcon : imgUtils.getSVGSmallName('status-alert'),
          confirmDescription : confirmationPaneContent,
          confirmMessage : '',
          //confirmButtonId : ID.dashDelimit(this.id, ID.getStopAction()),
          confirmButtonId : confirmButtonId,
          confirmButtonLabel : confirmationButtonLabel,
          redButton : true,
          displayXButton : true,
          okFunction : function() {
            operation();
          }
        });
        confirmationDialog.placeAt(window.body());
        confirmationDialog.startup();
        confirmationDialog.show();
    },

    __observeCardList: function() {
      if (this.collectionView) {
        this.collectionView.cardList.query({
          visible : true,
          selected : true
        }).observe(lang.hitch(this, function() {
          this.__processCollectionViewActions();
        }
        ));
      }
    },

    __processGridActions: function() {
      var stopCount = 0;
      var startCount = 0;
      var partialCount = 0;
      var unknownCount = 0;
      var autoScaledNotInMMCount = 0;
      var nodeJSAppOnClusterCount = 0;
      var nodeJSAppOnServerCount = 0;
      var scalingPolicyEnabledCount = 0;

      var selectedRows = this.grid.select.row.getSelected();
      var totalSelectedCount = selectedRows.length;
      for (var i = 0; i < selectedRows.length; i++) {
        var id = this.grid.row(parseInt(selectedRows[i])).id;
        var resource = this.grid.store.query({"id":id})[0].actions;

        //console.log("------------------ resource in collecting counts", resource);
        var resourceState = resource.state;
        if (resourceState === "STARTED") {
          startCount++;
        } else if ("STOPPED" === resourceState) {
          stopCount++;
        } else if ("PARTIALLY_STARTED" === resourceState) {
          partialCount++;
        } else if ("UNKNOWN" === resourceState) {
          unknownCount++;
        }

        if("STOPPED" === resourceState && resource.scalingPolicyEnabled && resource.maintenanceMode === constants.MAINTENANCE_MODE_NOT_IN_MAINTENANCE_MODE){
          autoScaledNotInMMCount++;
        }

        if (resource.type === "appOnServer" && resource.parentResource.runtimeType && resource.parentResource.runtimeType === constants.RUNTIME_NODEJS) {
          nodeJSAppOnServerCount++;
        } else if (resource.type === "appOnCluster") {
          resourceManager.getServer(resource.servers.list).then(function(servers) {
            // this is a deferred call and could have issue with not setting up the buttons enablement/disablement correctly
            for (var i = 0; i < servers.length; i++) {
              if (constants.RUNTIME_NODEJS === servers[i].runtimeType) {
                nodeJSAppOnClusterCount++;
              }
            }
          });
        }
        if (resource.scalingPolicyEnabled) {
          scalingPolicyEnabledCount++;
        }
      }
      this.__enableDisableActionButtons(totalSelectedCount, startCount, stopCount, partialCount, unknownCount, nodeJSAppOnClusterCount,
          nodeJSAppOnServerCount, scalingPolicyEnabledCount);
      this.enableDisableStartCleanButton(startCount, stopCount, unknownCount, autoScaledNotInMMCount);
      this.updateMMLabels();
    },

    __processCollectionViewActions: function() {
      var collectionView = this.collectionView;
      if (this.get("selectMode")) {
        this.updateMMLabels();
        var totalSelectedCount = collectionView.getSelectedCount();
        this.set('selectCount', totalSelectedCount);
        var startedCount = collectionView.getSelectedWithStartedCount();
        var stoppedCount = collectionView.getSelectedWithStoppedCount();
        var partialCount = collectionView.getSelectedWithPartialCount();
        var unknownCount = collectionView.getSelectedWithUnknownCount();
        var autoScaledNotInMMCount = collectionView.getSelectedAutoScaledNotInMaintenanceModeCount();
        var nodeJSAppOnClusterCount = collectionView.getSelectedWithNodeJSAppOnClusterCount();
        var nodeJSAppOnServerCount = collectionView.getSelectedWithNodeJSAppOnServerCount();
        var scalingPolicyEnabledCount = collectionView.getSelectedWithScalingPolicyEnabledCount();
        this.__enableDisableActionButtons(totalSelectedCount, startedCount, stoppedCount, partialCount, unknownCount, autoScaledNotInMMCount, nodeJSAppOnClusterCount,
            nodeJSAppOnServerCount, scalingPolicyEnabledCount);
        this.enableDisableStartCleanButton(startedCount, stoppedCount, unknownCount, autoScaledNotInMMCount);
      }
    },

    __enableDisableActionButtons: function(totalSelectedCount, startCount, stopCount, partialCount, unknownCount, autoScaledNotInMMCount, nodeJSAppOnClusterCount,
        nodeJSAppOnServerCount, scalingPolicyEnabledCount) {
      //console.log("---------- count: " + totalSelectedCount + " " + startCount + " " + stopCount + " " + partialCount + " " + nodeJSAppOnClusterCount +
      //    " " + nodeJSAppOnServerCount + " " + scalingPolicyEnabledCount);

      if ((nodeJSAppOnServerCount > 0) || (nodeJSAppOnClusterCount > 0) || (scalingPolicyEnabledCount > 0) || (startCount > 0 && stopCount > 0) || (autoScaledNotInMMCount > 0)) {
        // When we have selected things that are both started and stopped or an autoscaled resource not in maintenance mode, disable all actions
        if (this.actionStartButton) {
          this.actionStartButton.set('disabled', true);
          this.actionStartButton.set('iconClass', 'startActionDisabledIcon');
        }
        if (this.actionStopButton) {
          this.actionStopButton.set('disabled', true);
          this.actionStopButton.set('iconClass', 'stopActionDisabledIcon');
        }
        if (this.actionRestartButton) {
          this.actionRestartButton.set('disabled', true);
          this.actionRestartButton.set('iconClass', 'restartActionDisabledIcon');
        }
      } else {
        if (startCount > 0) {
          // When only started resources are selected, disable start
          if (this.actionStartButton) {
            this.actionStartButton.set('disabled', true);
            this.actionStartButton.set('iconClass', 'startActionDisabledIcon');
          }
          if (this.actionStopButton) {
            this.actionStopButton.set('disabled', false);
            this.actionStopButton.set('iconClass', 'stopActionIcon');
          }
          if (this.actionRestartButton) {
            this.actionRestartButton.set('disabled', false);
            this.actionRestartButton.set('iconClass', 'restartActionIcon');
          }
        } else if (stopCount > 0 || unknownCount > 0) {
          // When only stopped or unknown resources are selected, disable stop and restart
          if (this.actionStartButton) {
            this.actionStartButton.set('disabled', false);
            this.actionStartButton.set('iconClass', 'startActionIcon');
          }
          if (this.actionStopButton) {
            this.actionStopButton.set('disabled', true);
            this.actionStopButton.set('iconClass', 'stopActionDisabledIcon');
          }
          if (this.actionRestartButton) {
            this.actionRestartButton.set('disabled', true);
            this.actionRestartButton.set('iconClass', 'restartActionDisabledIcon');
          }
        } else if (partialCount > 0) {
          // This check for partialCount must be placed after the started and stopped checks.
          // The idea is that we allow the most restrictive conditions be honored first.
          // Restrictive conditions being, disable Start, Stop, Restart buttons.

          // Partially started cards are selected, don't disable anything
          if (this.actionStartButton) {
            this.actionStartButton.set('disabled', false);
            this.actionStartButton.set('iconClass', 'startActionIcon');
          }
          if (this.actionStopButton) {
            this.actionStopButton.set('disabled', false);
            this.actionStopButton.set('iconClass', 'stopActionIcon');
          }
          if (this.actionRestartButton) {
            this.actionRestartButton.set('disabled', false);
            this.actionRestartButton.set('iconClass', 'restartActionIcon');
          }
        } else {
          // When no resources are selected, disable all actions
          if (this.actionStartButton) {
            this.actionStartButton.set('disabled', true);
            this.actionStartButton.set('iconClass', 'startActionDisabledIcon');
          }
          if (this.actionStopButton) {
            this.actionStopButton.set('disabled', true);
            this.actionStopButton.set('iconClass', 'stopActionDisabledIcon');
          }
          if (this.actionRestartButton) {
            this.actionRestartButton.set('disabled', true);
            this.actionRestartButton.set('iconClass', 'restartActionDisabledIcon');
          }
        }
      }
      if (totalSelectedCount > 0) {
        if (this.deployFeatureAvailable) {
          if (this.actionDeployButton) {
            this.actionDeployButton.set('disabled', false);
            this.actionDeployButton.set('iconClass', 'deployActionIcon');
          }
        } else {
          if (this.actionDeployButton) {
            this.actionDeployButton.set('disabled', true);
            this.actionDeployButton.set('iconClass', 'deployActionDisabledIcon');
          }
        }
        if (this.actionMoreButton) {
          this.actionMoreButton.set('disabled', false);
          this.actionMoreButton.set('iconClass', 'moreActionIcon');
        }
      } else {
        if (this.actionDeployButton) {
          this.actionDeployButton.set('disabled', true);
          this.actionDeployButton.set('iconClass', 'deployActionDisabledIcon');
        }
        if (this.actionMoreButton) {
          this.actionMoreButton.set('disabled', true);
          this.actionMoreButton.set('iconClass', 'moreActionDisabledIcon');
        }
      }

    }

  });
});
