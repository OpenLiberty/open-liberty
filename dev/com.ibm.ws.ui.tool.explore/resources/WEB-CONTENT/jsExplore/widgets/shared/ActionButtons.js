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
define([ 'dojo/_base/lang', 'dojo/_base/window', 'dojo/aspect', 'dojo/dom-geometry', 'dojo/dom-style', 'dojo/dom-attr',
         'dojo/i18n!../../nls/explorerMessages', 'dojo/window', 'dijit/form/Button', 'dijit/form/DropDownButton', 'dojo/on', 'dojo/has',
         'dojo/query', 'dojo/dom-class', 'dijit/layout/ContentPane', 'dijit/registry', 'dijit/TooltipDialog', 'dojo/dom-construct',
         'js/common/platform', 'js/widgets/YesNoDialog', 'jsExplore/widgets/shared/SetAttributesDialog',
         'js/widgets/ConfirmDialog', "dojo/dom",
         'jsExplore/widgets/BreakAffinityDialog', "jsExplore/utils/constants",
         'jsExplore/widgets/shared/StateIcon', 'dojo/i18n!js/widgets/nls/widgetsMessages', 'jsExplore/resources/utils',
         'dojox/layout/TableContainer', 'jsExplore/utils/featureDetector', 'js/widgets/MessageDialog', 'dojo/i18n!jsExplore/resources/nls/resourcesMessages',
         'jsShared/utils/imgUtils', 'jsExplore/utils/ID'],
         function(lang, win, aspect, domGeometry, domStyle, domAttr, i18n, dojoWindow, Button, DropDownButton, on, has, query, domClass,
             ContentPane, registry, TooltipDialog, domConstruct, platform, YesNoDialog, SetAttributesDialog, ConfirmDialog, dom, BreakAffinityDialog, constants, StateIcon, widgetI18N, utils,
             TableContainer, featureDetector, MessageDialog, i18nResources, imgUtils, ID) {

  var startLabel = i18n.START;
  if (platform.isPhone()) {
    startLabel = '';
  }
  var startCleanLabel = i18n.START_CLEAN;
  if (platform.isPhone()) {
    startCleanLabel = '';
  }
  var stopLabel = i18n.STOP;
  if (platform.isPhone()) {
    stopLabel = '';
  }
  var restartLabel = i18n.RESTART;
  if (platform.isPhone()) {
    restartLabel = '';
  }
  var deployLabel = i18n.DEPLOY_SERVER_PACKAGE;
  if (platform.isPhone()) {
    deployLabel = "";
  }
  var setAttrLabel = i18n.SET_ATTRIBUTES;
  if (platform.isPhone()) {
    setAttrLabel = "";
  }
  var enableMaintenanceModeLabel = i18n.DISABLE_MAINTENANCE_MODE;
  if (platform.isPhone()) {
    enableMaintenanceModeLabel = "";
  }

  var disableMaintenanceModeLabel = i18n.ENABLE_MAINTENANCE_MODE;
  if (platform.isPhone()) {
    disableMaintenanceModeLabel = "";
  }
  // Shared constants
  var restartActionIcon = __createImgTag("actionMenu-restart", restartLabel);
  var restartActionDisabledIcon = __createImgTag("actionMenu-restart-disabled", restartLabel);
  
  var stopActionIcon = __createImgTag("actionMenu-stop", stopLabel);
  var stopActionDisabledIcon = __createImgTag("actionMenu-stop-disabled", stopLabel);
  
  var startActionIcon = __createImgTag("actionMenu-start", startLabel);
  var startActionDisabledIcon = __createImgTag("actionMenu-start-disabled", startLabel);
  
  var startCleanActionIcon = __createImgTag("actionMenu-start", startCleanLabel);
  var startCleanActionDisabledIcon = __createImgTag("actionMenu-start-disabled", startCleanLabel);
  
  var ellipsisIcon = __createImgTag("actionMenu-expandEllipsis", "");
  
  function __createImgTag(icon, label) {
    var className = "dropDownActionButtonIcon";
// Disable bidi support as Admin Center does not support mirroring    
//    if (has("adminCenter-bidi")) {
//      var bidiType = has("adminCenter-bidi-type");
//      if (bidiType === "rtl") {
//        className = className + " dropDownActionButtonIconRtl";
//      } 
//    }
    var spanOpen = "<span>";
    var spanClose = "</span>";
    return spanOpen + imgUtils.getSVGSmall(icon, icon) + label + spanClose;
  }
  
  // Creates a Content Pane that wraps around the resource button and an ellipsis button (that enables more options) to show the two buttons in the same row
  function createExpandActionButton(assignedId, resource, actionId, isActionable, enabled) {
      var cp = new ContentPane({
        id : assignedId + "expandButton-contentPane",
        region : "center",
        baseClass : "actionMenu-expandButton-contentPane",
        content : ""
      });
      
     var resourceButton = registry.byId(assignedId);
     if(resourceButton){
       resourceButton.placeAt(cp);
     }      
     
     var actionButton = registry.byId(actionId);
     if(actionButton){
       actionButton.domNode.classList.add('expandedButtonHidden');  
     }
          
     var hasMoreOptions = __hasAdvancedOptions(resource);
     
     if (hasMoreOptions){
       var expandButton = registry.byId(assignedId + '-expandButton');
       if (expandButton) {
         expandButton.destroy();
       }
 
       var expandButton = new Button({
         id : assignedId + '-expandButton',
         value: assignedId + '-expandButton', // workaround for batchscan false positive label requirement on hidden input tag
         title: i18n.EXPAND,
         label: ellipsisIcon + "<span class='hideForAccessibility'>" + i18n.EXPAND + "</span>", // Span important to pass batch scan since the ellipsis svg has no text itself
         showLabel : true,
         "aria-label" : i18n.ELLIPSIS_ARIA,
         baseClass : 'expandButton',
         tabindex : (enabled ? '0' : '-1'),
         checked : false,
         disabled : false,
         onClick : function() {           
           if(actionButton.domNode.classList.contains('expandedButtonHidden')){
             this.set('title', i18n.COLLAPSE);
             actionButton.domNode.classList.remove('expandedButtonHidden');             
           }
           else{
             this.set('title', i18n.EXPAND);
             actionButton.domNode.classList.add('expandedButtonHidden');             
           }         
         }
       });
       
       expandButton.placeAt(cp);
     }
  
     return cp;
  }
  
  // maintenance mode labels
  var enableMaintenanceModeLabel = i18n.ENABLE_MAINTENANCE_MODE;
  var disableMaintenanceModeLabel = i18n.DISABLE_MAINTENANCE_MODE;
  /**
   * Common method to create an 'ActionButton'. The basic format for Start, Stop and Restart buttons is extremely common, only the
   * values are different.
   */
  function __createActionButtonCommon(name, assignedId, resource, isExpandedButton, assignedAction, isActionable, enabledIcon, disabledIcon) {
    var dir = "ltr";
    
    var nlsLabel = "";
    if (name === "Start") {
      nlsLabel = startLabel;
    } else if (name === "Start --clean") {
      nlsLabel = startCleanLabel;
    } else if (name === "Stop") {
      nlsLabel = stopLabel;
    } else if (name === "Restart") {
      nlsLabel = restartLabel;
    } else if (name === "Deploy") {
      nlsLabel = deployLabel;
    }
    var b = new Button({
      id : assignedId,
      value: assignedId, // workaround for batchscan false positive label requirement on hidden input tag
      title: nlsLabel,
      dir : dir,
      resource : resource,
      "aria-label" : nlsLabel,
      label : (function() {
        if (isActionable(resource, this.id)) {
          return enabledIcon;
        } else {
          return disabledIcon;
        }
      }()),
      baseClass : (function() {
        if (isActionable(resource, this.id)) {
          return 'dropDownActionButtons';
        } else {
          return 'dropDownActionButtonsDisabled';
        }
      }()),
      disabled : (function() {
        if (isActionable(resource, this.id)) {
          return false;
        } else {
          return true;
        }
      }()),
      updateEnabled : function() {
        if (isActionable(resource, this.id)) {
          this.setEnabled();
        } else {
          this.setDisabled();
        }
      },
      setEnabled : function() {
        this.set('disabled', false);
        //domClass.remove(this.domNode.id, 'dropDownActionButtonsDisabled');
        domClass.remove(this.domNode, 'dropDownActionButtonsDisabled');
        this.set('class', 'dropDownActionButtons');
        this.set('label', enabledIcon);
      },
      setDisabled : function() {
        this.set('disabled', true);
        //domClass.remove(this.domNode.id, 'dropDownActionButtons');
        domClass.remove(this.domNode, 'dropDownActionButtons');
        this.set('class', 'dropDownActionButtonsDisabled');
        this.set('label', disabledIcon);
      },
      display : (isExpandedButton) ? 'none' : 'block',
      onClick : assignedAction
    }, name);
    
    if(isExpandedButton){
      domClass.add(b.domNode, "expandedButton");
    }

    // Bind in the focus behaviours
    on(b.domNode, "focus", function() {
      domClass.add(b.domNode, "dropDownActionButtonFocused");
    });
    on(b.domNode, "focusout", function() {
      domClass.remove(b.domNode, "dropDownActionButtonFocused");
    });    

    return b;
  }

  /**
   *  Method to create the Tags & Metadata button
   */
  function __createTagsAndMetadataButton(rootId, resource) {
    var dir = "ltr";
    
    var setAttrButton = new Button({
      id : rootId + '-setAttrButton',
      value: rootId + '-setAttrButton', // workaround for batchscan false positive label requirement on hidden input tag
      dir : dir,
      resource : resource,
      label : setAttrLabel,
      "aria-label": setAttrLabel,
      baseClass : 'dropDownActionButtons',
      disabled : false,
      onClick : function(me) {
        var bDialog = new SetAttributesDialog({
          id: "setAttrsDlg",
          resource: resource,
          onHide: lang.hitch(this, setTimeout(function() {
                    if (!registry.byId("setAttrsDlg").open)
                      registry.byId("setAttrsDlg").destroyRecursive();
                   }, 500))
        }); 
        bDialog.placeAt(win.body());
        bDialog.startup();
        bDialog.show();          
      }
    });

    // Bind in the focus behaviours
    on(setAttrButton.domNode, "focus", function() {
      domClass.add(setAttrButton.domNode, "dropDownActionButtonFocused");
    });
    on(setAttrButton.domNode, "focusout", function() {
      domClass.remove(setAttrButton.domNode, "dropDownActionButtonFocused");
    });

    return setAttrButton;
  }
  
  /**
   * Method to create the set/unset maintenance mode button
   * values are different.
   */
  function __createEnableDisableMaintenanceModeButton(name, assignedId, resource, assignedAction, isActionable, enableMMLabel, disableMMLabel) {
    var dir = "ltr";
// Disable bidi support as Admin Center does not support mirroring    
//    if (has("adminCenter-bidi")) {
//      var bidiType = has("adminCenter-bidi-type");
//      if (bidiType === "rtl") {
//        dir = bidiType;
//      } 
//    }

    var b = new Button({
      id : assignedId,
      value: assignedId, // workaround for batchscan false positive label requirement on hidden input tag
      dir : dir,
      resource : resource,
      "aria-label": (function() {
        if ( resource.maintenanceMode === null || resource.maintenanceMode === constants.MAINTENANCE_MODE_NOT_IN_MAINTENANCE_MODE) {
          return enableMMLabel;
        } else {
          return disableMMLabel;
        }
      }()),
      label : (function() {
        if ( resource.maintenanceMode === null || resource.maintenanceMode === constants.MAINTENANCE_MODE_NOT_IN_MAINTENANCE_MODE) {
            return enableMMLabel;
          }
        else return disableMMLabel;
      }()),
      baseClass : (function() {
        if (isActionable(resource)) {
          return 'dropDownActionButtons';
        } else {
          return 'dropDownActionButtonsDisabled';
        }
      }()),
      disabled : (function() {
        if (isActionable(resource)) {
          return false;
        } else {
          return true;
        }
      }()),
      updateEnabled : function() {
        if (isActionable(resource)) {
          this.setEnabled();
        } else {
          this.setDisabled();
        }
        if ( resource.maintenanceMode === null || resource.maintenanceMode === constants.MAINTENANCE_MODE_NOT_IN_MAINTENANCE_MODE) {
          this.set('label',  enableMMLabel);
        }
        else  this.set('label',  disableMMLabel);
      },
      setEnabled : function() {
        this.set('disabled', false);
        this.set('class', 'dropDownActionButtons');
      },
      setDisabled : function() {
        this.set('disabled', true);
        this.set('class', 'dropDownActionButtonsDisabled');
      },
      onClick : assignedAction
    }, name);

    // Bind in the focus behaviours
    on(b.domNode, "focus", function() {
      domClass.add(b.domNode, "dropDownActionButtonFocused");
    });
    on(b.domNode, "focusout", function() {
      domClass.remove(b.domNode, "dropDownActionButtonFocused");
    });

    return b;
  }

  /**
   * Determines if the resource is considered startable.
   * Anything that isn't considered fully started is startable.  
   * If the resource's parent is not startable, then the resource is not startable.
   */
  function isStartable(resource, id) {
    var parent = resource.parentResource;
    if(parent) {
      // Example:  Parent resource means that when viewing an application, 
      // the app's parent would be a server or cluster.
      var parentStartable = __isParentResourceStartable(parent, id);
      if(parentStartable) {
        // Parent is startable, now check the resource itself is startable
        return __isResourceStartable(resource, id);
      } else {
        return false;
      }
    }
    
    // No parent resource or cluster check logic.  Only check the resource individually.
    return __isResourceStartable(resource, id);
  } // isStartable end
  
  function __isParentNodeJS(parentResource, id) {
    var isParentNodeJS = false;
    if (parentResource && parentResource.type === "server" && constants.RUNTIME_NODEJS === parentResource.runtimeType) {
      isParentNodeJS = true;
    } else if (parentResource && parentResource.type === "cluster" && parentResource.servers && typeof id !== "undefined" && id !== null) {
      // check each server in cluster; if at least 1 server is nodeJS, return true & disable the buttons.
      parentResource.resourceManager.getServer(parentResource.servers.list).then(function(servers) {
        for (var i = 0; i < servers.length; i++) {
          if (constants.RUNTIME_NODEJS === servers[i].runtimeType) {
            isParentNodeJS = true;
            baseID = id.substring(0, id.lastIndexOf('-'));
            var actionButtonIDs = [baseID + '-startButton', baseID + '-stopButton', baseID + '-restartButton'];
            for (var j = 0; j < actionButtonIDs.length; j++){
              var buttonId = actionButtonIDs[j];
              var button = registry.byId(buttonId);
              if (typeof button !== "undefined" && button !== null) {
                button.setDisabled();
              }
            }
            break;
          }
        }
      });
    }
    return isParentNodeJS;
  } // __isParentNodeJS end
  
  /**
   * Checks if resource contains the server hosting this Admin Center
   */
  function __hasAdminCenterServer(resource) {
    var hasAdminCenterServer = false;
    if (resource.type === "cluster") {
      resource.resourceManager.getServer(resource.servers.list).then(function(servers) {
        for (var i = 0; i < servers.length; i++) {
          if (servers[i].isAdminCenterServer) {
            hasAdminCenterServer = true;
            break;
          }
        }
      });
    }
    return hasAdminCenterServer;
  } 
  
  function __isParentResourceStartable(parentResource, id) {
    var isParentStartable = false;
    var isParentNodeJS = __isParentNodeJS(parentResource, id);
    // If this resource has a parent, the parent's state dictates if the current resource is startable.
    // Example: An stopped app on a stopped cluster.  The app should not be startable due to the stopped cluster.
     if(parentResource.state === 'STOPPED' || parentResource.scalingPolicyEnabled || isParentNodeJS) {
       isParentStartable = false;
     } else {
       isParentStartable = true;
     }
     return isParentStartable;
  } // __isParentResourceStartable end
  
  function __isResourceStartable(resource, id) {
    return !(resource.state === 'STARTED' || resource.state === 'STARTING' || resource.state === 'STOPPING' || resource.scalingPolicyEnabled || __isParentNodeJS(resource.parentResource, id));
  } // __isResourceStartable end

  /**
   * Construct a start Button widget.
   */
  function __createStartButton(assignedId, resource, assignedAction) {
    return __createActionButtonCommon('Start', assignedId, resource, false, assignedAction, isStartable, startActionIcon,
        startActionDisabledIcon);
  }
  
  /**
   * Construct a start clean Button widget.
   */
  function __createStartCleanButton(assignedId, resource, assignedAction) {
    return __createActionButtonCommon('Start --clean', assignedId, resource, true, assignedAction, isStartable, startCleanActionIcon,
        startCleanActionDisabledIcon);
  }

  /**
   * Determines if the resource is considered stoppable. Anything that is not fully stopped is stoppable.
   */
  function isStoppable(resource, id) {
    return !(resource.state === 'STOPPED' || resource.state === 'STARTING' || resource.state === 'STOPPING' || resource.state === 'INSTALLED' || resource.scalingPolicyEnabled || __isParentNodeJS(resource.parentResource, id));
  }

  /**
   * Construct a stop Button widget.
   */
  function __createStopButton(assignedId, resource, assignedAction) {
    return __createActionButtonCommon('Stop', assignedId, resource, false, assignedAction, isStoppable, stopActionIcon, stopActionDisabledIcon);
  }

  /**
   * Determines if the resource is considered restartable. Anything that isn't fully stopped OR that is the AdminCenter server is
   * restartable.
   */
  function isRestartable(resource, id) {
    return !(resource.state === 'STOPPED' || resource.state === 'STARTING' || resource.state === 'STOPPING' || resource.isAdminCenterServer || __hasAdminCenterServer(resource) || resource.scalingPolicyEnabled || __isParentNodeJS(resource.parentResource, id));
  }

  /**
   * Construct a restart Button widget.
   */
  function __createRestartButton(assignedId, resource, assignedAction) {
    return __createActionButtonCommon('Restart', assignedId, resource, false, assignedAction, isRestartable, restartActionIcon,
        restartActionDisabledIcon);
  }

  /**
   * Construct a maintenance mode Button widget.
   */
  function __createMaintenanceModeButton(assignedId, resource, assignedAction) {
    return __createEnableDisableMaintenanceModeButton('MaintenanceMode', assignedId, resource, assignedAction, isMaintenanceModeNotInProgress, enableMaintenanceModeLabel, disableMaintenanceModeLabel);
  }
  
  /**
   * Determines if the resource is in maintenance mode
   */
  function isMaintenanceModeNotInProgress(resource) {
    if (resource.maintenanceMode !== "alternateServerStarting" )
      return true;
    else 
      return false;
  }
  /**
   * Determines if the Deploy tool is available in the environment.
   */
  function supportsDeploy(resource) {
    return featureDetector.isDeployAvailable();
  }
  
  /**
   * Determines if an action button should have an ellipsis
   */
  function __hasAdvancedOptions(resource) {
    return (resource.type === "server" || resource.type === "cluster");
    //&&  (action === "start" || action === "restart");
  }

  /**
   * Construct a deploy Button widget.
   */
  function __createDeployButton(assignedId, resource, actionBarDialog, dropDownParent) {
    var assignedAction = function() {
      var doDeploy = function(resource) {
        // We should deploy to any visible
        var hostList = [];
        hostList.push(resource.id);
        // The proxy page is where we'll put our content. We need to blast it from inside
        // the iframe, so we need a global function in the main index page to allow that
        var iframeNode = domConstruct.create("iframe", {
          style : "border: none; overflow: hidden;",
          width : "100%",
          height : "100%",
          src : "../deploy-1.0/index.jsp?hostlist=" + hostList,
          id : "deployFrame"
        });

        var breadCrumbContainer = registry.byId("breadcrumbContainer-id");
        var refNode = registry.byId(resource.type + "HostProxyPage");
        if (refNode) {
          refNode.set("content", iframeNode);
          breadCrumbContainer.selectChild(refNode);
        } else {
          refNode = new ContentPane({
            id : resource.type + "HostProxyPage",
            title : i18n.DEPLOY_SERVER_PACKAGE,
            "style" : "height: 100%;",
            content : iframeNode
          });
          breadCrumbContainer.addChild(refNode);
        }
        breadCrumbContainer.set("style", "overflow: hidden;");

        // We need to dynamically resize the pane because Firefox doesn't handle this very well
        // add height property causes problem in webkit.  However without height, webkit would show scroll bar.  The fix
        // is to not set height in webkit and hide iframe scrollbar in webkit.
        iframeNode.onload = function() {
          var isWebkit = 'WebkitAppearance' in document.documentElement.style;
          if ( !isWebkit )
            refNode.set("style", "height: " + iframeNode.contentDocument.body.scrollHeight + "px");
          refNode.resize();
        };
      };

      dropDownParent.closeDropDown();
      __clearConfirmationPane(actionBarDialog);
      doDeploy(resource);
    };
    return __createActionButtonCommon('Deploy', assignedId, resource, false, assignedAction, supportsDeploy, deployLabel, deployLabel);
  }

  /**
   * Constructs and displays the stop Admin Center dialog. This should be used to replace the confirmation button for a stop action.
   */
  function __displayStopPopupDialog(resource, stopFunction) {
    var name = resource.name;
    var isController = resource.isAdminCenterServer && !(resource.type === 'standaloneServer');
    var spanOpen = __getSpanOpen(name);
    var spanClose = "</span>";
    var widgetId = 'askStopACServer';
    utils.destroyWidgetIfExists(widgetId);
    var stopDialog = new YesNoDialog({
      id : widgetId,
      title : i18n.STOP_AC_TITLE,
      descriptionIcon : imgUtils.getSVGSmallName('status-alert'),
      description : lang.replace((isController ? i18n.STOP_AC_DESCRIPTION : i18n.STOP_STANDALONE_DESCRIPTION), [ spanOpen + name + spanClose ]),
      message : (isController ? i18n.STOP_AC_MESSAGE : i18n.STOP_STANDALONE_MESSAGE),
      yesFunction : function() {
        stopFunction();
      },
      noFunction : function() {
      }
    });
    stopDialog.placeAt(win.body());
    stopDialog.startup();
    stopDialog.show();
  }
  
  /**
   * Constructs and displays the enable/disable maintenance mode dialog.
   */
  function __displayEnableDisableMaintenanceModePopupDialog(resource, actionFunction) {
    if (registry.byId('askEnableDisableMaintenanceMode')) {
      registry.byId('askEnableDisableMaintenanceMode').destroy();
    }

    // default is to enable mm on server
    var description = i18n.ENABLE_MAINTENANCE_MODE_SERVER_DESCRIPTION;
    var title = i18n.ENABLE_MAINTENANCE_MODE_DIALOG_TITLE; // title is either enable or disable
    var hasOptions = true ; // break affinity option is only available for enable mm 
    var question = i18n.BREAK_AFFINITY_LABEL;
    var btnLabel = i18n.ENABLE_MAINTENANCE_MODE_DIALOG_ENABLE_BUTTON_LABEL;
    if ( resource.maintenanceMode === constants.MAINTENANCE_MODE_IN_MAINTENANCE_MODE) {
      // do not pop up dialog, do the action directly
      actionFunction();
      return false;
    } else {
      // enable MM on host labels
      if ( 'host' === resource.type ) {
        var serversInHost = resource.servers.list.length;
        var serversInMM = resource.servers.inMaintenanceMode;
        description = lang.replace(i18n.ENABLE_MAINTENANCE_MODE_HOST_DESCRIPTION, [ serversInHost ]) ;
        if ( serversInMM > 0 ) description += "<br><br>" + lang.replace(i18n.MAINTENANCE_MODE_SERVERS_IN_MAINTENANCE_MODE, [serversInMM]);
        
      }
    }
    
    var confirmEnableMaintenanceDialog = new BreakAffinityDialog({
      id: "askEnableDisableMaintenanceMode",
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
        actionFunction();
      }
    });
    confirmEnableMaintenanceDialog.placeAt(win.body());
    confirmEnableMaintenanceDialog.startup();
    confirmEnableMaintenanceDialog.show();
    return false;
  }
  
  /**
   * Constructs and displays the stop Admin Center dialog. This should be used to replace the confirmation button for a stop action.
   */
  function __displayClusterStopPopupDialog(resource, stopFunction) {
    var name = resource.name;
    var isController = resource.isAdminCenterServer && !(resource.type === 'standaloneServer');
    var spanOpen = __getSpanOpen(name);
    var spanClose = "</span>";
    var widgetId = 'askStopACServer';
    utils.destroyWidgetIfExists(widgetId);
    var stopDialog = new YesNoDialog({
      id : widgetId,
      title : lang.replace(i18n.STOP_AC_CLUSTER_TITLE, [ spanOpen + name + spanClose ]),
      descriptionIcon : imgUtils.getSVGSmallName('status-alert'),
      description : lang.replace((isController ? i18n.STOP_AC_CLUSTER_DESCRIPTION : i18n.STOP_AC_CLUSTER_DESCRIPTION), [ spanOpen + name + spanClose ]),
      message : (isController ? i18n.STOP_AC_CLUSTER_MESSAGE : i18n.STOP_AC_CLUSTER_MESSAGE),
      yesFunction : function() {
        stopFunction();
      },
      noFunction : function() {
      }
    });
    stopDialog.placeAt(win.body());
    stopDialog.startup();
    stopDialog.show();
  }

  function __displayConfirmationPopupDialog(resource, title, question, actionFunction) {
    var name = resource.name;
    var widgetId = 'actionConfirmationPopup';
    utils.destroyWidgetIfExists(widgetId);
    var confirmationDialog = new YesNoDialog({
      id : widgetId,
      title : title,
      descriptionIcon : imgUtils.getSVGSmallName('status-alert'),
      description : name,
      message : question,
      yesFunction : function() {
        actionFunction();
      },
      noFunction : function() {
      }
    });
    confirmationDialog.placeAt(win.body());
    confirmationDialog.startup();
    confirmationDialog.show();
  }

  
  /**
   * Determines if the action menu should be prevented.
   */
  function disableActionsInMenu(resource) {
    // appInst: { state: X, server: { state: Y } }
    return resource.server && resource.server.state === 'STOPPED';
  }

  /**
   * Determine the correct message based on the resource type
   */
  function getStartConfirmationMessage(resource) {
    var spanOpen = __getSpanOpen(resource.name);
    var spanClose = "</span>";
    // TODO: Make these checks method calls (resource.isCluster() or resourceUtil.isCluster(resource)) something like that
    var confMessage = i18n.START_INSTANCE;
    if (resource.type === 'cluster' || resource.type === 'host') {
      confMessage = lang.replace(i18n.START_ALL_SERVERS_WITHIN, [ spanOpen + resource.name + spanClose ]);
    } else if (resource.type == 'application') {
      confMessage = lang.replace(i18n.START_ALL_INSTS_OF_APP, [ spanOpen + resource.name + spanClose ]);
    } else if (resource.type == 'server') {
      confMessage = lang.replace(i18n.START_SERVER, [ spanOpen + resource.name + spanClose ]);
    } else if (resource.type === "applicationInst") {
      if (resource.server) {
        // in a singleServer pane
        confMessage = lang.replace(i18n.START_APP_ON_SERVER, [ spanOpen + resource.name + spanClose,
                                                               __getSpanOpen(resource.server.name) + resource.server.name + spanClose ]);
      } else if (resource.cluster) {
        // in a singleCluster pane
        confMessage = lang.replace(i18n.START_ALL_INSTS_OF_APP, [ spanOpen + resource.name + spanClose ]);
      }
    }
    return confMessage;
  }

  /**
   * Determine the correct message based on the resource type
   */
  function getStopConfirmationMessage(resource) {
    var spanOpen = __getSpanOpen(resource.name);
    var spanClose = "</span>";
    var confMessage = i18n.STOP_INSTANCE;
    if (resource.type === "cluster" || resource.type === "host") {
      confMessage = lang.replace(i18n.STOP_ALL_SERVERS_WITHIN, [ spanOpen + resource.name + spanClose ]);
    } else if (resource.type === "application") {
      confMessage = lang.replace(i18n.STOP_ALL_INSTS_OF_APP, [ spanOpen + resource.name + spanClose ]);
    } else if (resource.type === "server") {
      confMessage = lang.replace(i18n.STOP_SERVER, [ spanOpen + resource.name + spanClose ]);
    }
    return confMessage;
  }

  /**
   * Determine the correct message based on the resource type
   */
  function getRestartConfirmationMessage(resource) {
    var spanOpen = __getSpanOpen(resource.name);
    var spanClose = "</span>";
    var confMessage = i18n.RESTART_INSTANCE;
    if (resource.type == "cluster" || resource.type == "host") {
      confMessage = lang.replace(i18n.RESTART_ALL_SERVERS_WITHIN, [ spanOpen + resource.name + spanClose ]);
    } else if (resource.type == "application") {
      confMessage = lang.replace(i18n.RESTART_ALL_INSTS_OF_APP, [ spanOpen + resource.name + spanClose ]);
    } else if (resource.type == "server") {
      confMessage = lang.replace(i18n.RESTART_SERVER, [ spanOpen + resource.name + spanClose ]);
    }
    return confMessage;
  }

  /**
   * Performs clean up of the ActionBar confirmation pane.
   */
  function __clearConfirmationPane(buttonParent) {
    if (buttonParent.confirmationPane) {
      buttonParent.removeChild(buttonParent.confirmationPane);
      buttonParent.confirmationPane.destroy();
      buttonParent.removeChild(buttonParent.confirmationButton);
      buttonParent.confirmationButton.destroy();
      buttonParent.resize();
    }
  }

  /**
   * Display the popup confirmation dialog for the actions
   */
  function __confirmedAction(actionButtonId, resource, operation, getConfirmationMessage, confirmationButtonLabel, isRedButton) {
    var confirmationPaneContent = null;
    if (typeof (getConfirmationMessage) == "function") {
      confirmationPaneContent = getConfirmationMessage(resource);
    } else {
      confirmationPaneContent = getConfirmationMessage;
    }
    var confirmationDialog = registry.byId(actionButtonId + '-actionConfirmationPopup');
    if(confirmationDialog){
      confirmationDialog.destroy();
    }
    confirmationDialog = new ConfirmDialog({
      id : actionButtonId + '-actionConfirmationPopup',
      baseClass: "confirmDialog acDialog acDialogNoTitle",
      title : '',
      confirmDescriptionIcon : imgUtils.getSVGSmallName('status-alert'),
      confirmDescription : confirmationPaneContent,
      confirmMessage : '',
      confirmButtonId : actionButtonId + '-confirmationButton',
      confirmButtonLabel : confirmationButtonLabel,
      displayXButton : true,
      redButton : isRedButton,
      okFunction : function() {
        operation(resource);
      }
    });
    confirmationDialog.placeAt(win.body());
    confirmationDialog.startup();
    confirmationDialog.show();
  }
  
  /**
   * Creates a pop-up with the failed operation message.
   */
  function showFailedOperationDialog(resource, operation, errMsg) {
    // Is there a way to determine if message comes from JRE/JVM? Prob not
    if (errMsg !== undefined && errMsg.indexOf && (errMsg.indexOf('Class JavaLaunchHelper is implemented in both ') > -1) && (errMsg.indexOf('Which one is undefined') > -1)) {
      // This is a known bug on JDK 8 for Mac: http://bugs.java.com/bugdatabase/view_bug.do?bug_id=8022291
      // The message does not come from anything in Liberty and there's nothing we can do to suppress it from going
      // to System.err. It appears consistently in that specific environment. Log but don't display message in a dialogue
      // because it is just noise and has no bearing on the server
      console.log("Error from JRE:", errMsg, "; will not display an error dialog because this does not come from the Liberty server");
      return;
    }
    switch (operation) {
    case 'start': 
      errMsg = i18nResources.SERVER_START_ERROR + "<br><br>" + errMsg;
      break;
    case 'start --clean':
      errMsg = i18nResources.SERVER_START_CLEAN_ERROR + "<br><br>" + errMsg;
      break;
    case 'stop':
      errMsg = i18nResources.SERVER_STOP_ERROR + "<br><br>" + errMsg;
      break;
    case 'restart': 
      errMsg = i18nResources.SERVER_RESTART_ERROR + "<br><br>" + errMsg;
      break;     
    }
    var failedDialog = new ConfirmDialog({
      id : resource.id + '-' + operation + '-failed',
      title : i18nResources.ERROR,
      confirmDescriptionIcon : imgUtils.getSVGSmallName('status-alert'),
      confirmDescription : errMsg,
      confirmMessage : '',
      confirmButtonLabel : widgetI18N.TOOLBOX_BUTTON_OK,
      displayXButton : false,
      okFunction : function() {
      }
    });
    failedDialog.placeAt(win.body());
    failedDialog.startup();
    failedDialog.show();
   }

  /**
   * Creates the ActionBarDropDown.
   * 
   * The ActionBarDropDown needs to be response to changes in the state of the resource which it is managing.
   */
  function __createActionBarDropDown(resource, parentId, dropDownParent, message) {
    var myRootId = parentId + '-actionMenu';

    // Build the action bar dropDown (even though its a TooltipDialog)!
    var actionBarDialog = new TooltipDialog({
      id : myRootId,
      baseClass : 'actionMenu',
      name : message,
      "aria-labelledby" : myRootId,
      "aria-label" : message,
      style : 'display: inline;',
      content : '',
      resource : resource
      // The resource these actions will operate on
    });
    
    if (resource.type === 'host') {
      
      var tableContainer = __createActionTable (actionBarDialog);
      
      var deployButton = __createDeployButton(myRootId + '-deployButton', resource, actionBarDialog, dropDownParent);
      tableContainer.addChild(deployButton);
      
      // Tags and Metadata button
      var tagsMetadataButton = __createTagsAndMetadataButton(myRootId, resource);
      tableContainer.addChild(tagsMetadataButton);
      
      var mmbHost = __createMaintenanceModeButton(myRootId + '-maintenanceModeButton', resource, function(me) {
      var doMaintenanceMode = function(resource, breakAffinity, force) {
        if ( resource.maintenanceMode != constants.MAINTENANCE_MODE_IN_MAINTENANCE_MODE ) {
            resource.setMaintenanceMode(breakAffinity, force).then(function(response) {
              if ( response.errMsg ) {
                showFailedOperationDialog(resource, 'setMaintenanceMode', response.errMsg);
              } else if ( response.showForce && response.showForce === true ) {
                var widgetId = 'askForceDialog';
                utils.destroyWidgetIfExists(widgetId);
                var forceDialog = new YesNoDialog({
                  id : widgetId,
                  title : i18n.MAINTENANCE_MODE_FAILED,
                  descriptionIcon : imgUtils.getSVGSmallName('status-alert'),
                  yesButtonText: i18n.MAINTENANCE_MODE_FORCE_LABEL,
                  noButtonText: i18n.MAINTENANCE_MODE_CANCEL_LABEL,
                  description : i18n.MAINTENANCE_MODE_ALTERNATE_SERVERS_NOT_STARTED,
                  message : i18n.MAINTENANCE_MODE_SELECT_FORCE_MESSAGE,
                  yesFunction : function() {
                    resource.setMaintenanceMode(breakAffinity, true).then(function(response) {
                      if ( response.errMsg ) {
                        showFailedOperationDialog(resource, 'setMaintenanceMode', response.errMsg);
                      } else {
                        //TODO animation on obj view
                      }
                    });
                  },
                  noFunction : function() {
                  }
                });
                forceDialog.placeAt(win.body());
                forceDialog.startup();
                forceDialog.show();
              } else {
                //TODO show animation if it is on Obj view
              }
            });
          }
          else {
            resource.unsetMaintenanceMode().then(function(response) {
              if ( response.errMsg ) {
                  showFailedOperationDialog(resource, 'unsetMaintenanceMode', response.errMsg);
              }
            });
          }
            
        };

        /** Construct the confirmation dialog. Stop is only done if confirmed **/
        __clearConfirmationPane(actionBarDialog);

        // TODO: We still need to clear the action bar faster than we do right now
        __displayEnableDisableMaintenanceModePopupDialog(resource, function() {
          if ( resource.maintenanceMode === constants.MAINTENANCE_MODE_IN_MAINTENANCE_MODE ) {
            console.log ("submit disable maintenance mode on host.");
            dropDownParent.closeDropDown();
            __clearConfirmationPane(actionBarDialog);
            doMaintenanceMode(resource);
          } else {
            dropDownParent.closeDropDown();
            __clearConfirmationPane(actionBarDialog);
            var breakAffinityToggleButton = dom.byId(ID.getBreakAffinityToggleButton());
            console.log ("submit enable maintenance mode on host with break affinity set to " + breakAffinityToggleButton.checked);
            doMaintenanceMode(resource, breakAffinityToggleButton.checked, false);            
          }
        });
      });
      tableContainer.addChild(mmbHost);
   // Bind the buttons and no action pane into the action bar in an organized manner
      actionBarDialog.buttons = {};
      actionBarDialog.buttons.mmbHost = mmbHost;
      
      // When the ActionBar is opened, ensure the available options are correct and up to date
      actionBarDialog.on('open', function(evt) {
        // Remove confirmation panels
        __clearConfirmationPane(actionBarDialog);

        this.buttons.mmbHost.updateEnabled();
        
      });
      
    } else if (resource.type === 'runtime') {
      
      var tableContainer = __createActionTable (actionBarDialog);
      
      // Tags and Metadata button
      var tagsMetadataButton = __createTagsAndMetadataButton(myRootId, resource);
      tableContainer.addChild(tagsMetadataButton);
      
    } else if (resource.type === 'standaloneServer') {
      var stopStandalone = __createStopButton(myRootId + '-stopButton', resource, function(me) {
        var doStop = function(resource) {
          resource.stop().then(function(successMsg) {
            displayStopMessage(successMsg, true);
          }, function(errMsg) {
            displayStopMessage(errMsg);
          });
        };

        /** Construct the confirmation dialog. Stop is only done if confirmed **/
        __clearConfirmationPane(actionBarDialog);

        // TODO: We still need to clear the action bar faster than we do right now
        __displayStopPopupDialog(resource, function() {
          dropDownParent.closeDropDown();
          __clearConfirmationPane(actionBarDialog);
          doStop(resource);
        });
      });
      actionBarDialog.addChild(stopStandalone);
            
      // Bind the buttons and no action pane into the action bar in an organized manner
      actionBarDialog.buttons = {};
      actionBarDialog.buttons.stopButton = stopStandalone;

      // When the ActionBar is opened, ensure the available options are correct and up to date
      actionBarDialog.on('open', function(evt) {
        // Remove confirmation panels
        __clearConfirmationPane(actionBarDialog);

      });
    } else {
      var tableContainer = __createActionTable (actionBarDialog);
      
      var startButton = __createStartButton(myRootId + '-startButton', resource, function() {
        var doStart = function(resource) {
          resource.start().then(function(response) {
            if (response !== null && response.errMsg) {
              showFailedOperationDialog(resource, 'start', response.errMsg);
            }
          });
        };
        __confirmedAction(myRootId, resource, doStart, getStartConfirmationMessage, i18n.START, false);
      });

      if(__hasAdvancedOptions(resource)){
        var startCleanButton = __createStartCleanButton(myRootId + '-startCleanButton', resource, function() {
          var doStartClean = function(resource) {
            resource.startClean().then(function(response) {
              if (response !== null && response.errMsg) {
                showFailedOperationDialog(resource, 'start --clean', response.errMsg);
              }
            });
          };
          __confirmedAction(myRootId, resource, doStartClean, getStartConfirmationMessage /* todo change */, i18n.START_CLEAN, false);
        });
        
        // Wrap start button in a content pane with an ellipsis to expand to see start-clean button
        var startExpandButton = createExpandActionButton(myRootId + '-startButton', resource, myRootId + '-startCleanButton', isStartable, startButton.isDisabled);
      }    
      
      var stopButton = __createStopButton(myRootId + '-stopButton', resource, function(me) {
        var doStop = function(resource) {
          resource.stop().then(function(response) {
            if ( response.errMsg ) {
              var showMsg = true;
              if (resource.isAdminCenterServer) {
                var msg = response.errMsg;
                if (msg.lastIndexOf("CWWKX7202E", 0) === 0 ) {
                  showMsg = false;
                }
              }
              if ( resource.type == 'server' && response.message ) {
                // If the Server.stop() function cannot connect to the MBean because
                // the server is in the midst of stopping, don't display the error dialog
                // It *should* be safe to ignore the message here, when a stop for Server resource
                // was issued
                var msg = response.message;
                var msgToCompare = "Unable to load /IBMJMXConnectorREST/mbeans/WebSphere:feature=collectiveController,"+
                                   "type=ServerCommands,name=ServerCommands/operations/stopServer status: 0";
                if (msg.indexOf(msgToCompare) > -1) {
                  showMsg = false;
                }
              }

              if ( showMsg )
                showFailedOperationDialog(resource, 'stop', response.errMsg);
            }
          });
        };

        /** Construct the confirmation dialog. Stop is only done if confirmed **/
        if (resource.isAdminCenterServer) {
          __clearConfirmationPane(actionBarDialog);

          // TODO: We still need to clear the action bar faster than we do right now
          __displayStopPopupDialog(resource, function() {
            dropDownParent.closeDropDown();
            __clearConfirmationPane(actionBarDialog);
            doStop(resource);
          });
        }
        else if(resource.type === 'cluster')
        {
          var isStop = false;
          //console.error("resource.servers",resource.servers,resource);
          resource.servers.list.forEach(function(eachServer){
            resource.resourceManager.getServer(eachServer).then(function(server){
              isStop = server.isAdminCenterServer;
              return isStop;
            }).then(function(isStop){
              if(isStop){
                __clearConfirmationPane(actionBarDialog);
                __displayClusterStopPopupDialog(resource, function() {
                  dropDownParent.closeDropDown();
                  __clearConfirmationPane(actionBarDialog);
                  doStop(resource);});
              } else {
                __confirmedAction(myRootId, resource, doStop, getStopConfirmationMessage, i18n.STOP, true);
              }
            });});
         } else {
           __confirmedAction(myRootId, resource, doStop, getStopConfirmationMessage, i18n.STOP, true);
        }
      });

      var restartButton = __createRestartButton(myRootId + '-restartButton', resource, function(me) {
        var doRestart = function(resource) {
          resource.restart().then(function(response) {
            if (response.errMsg) {
              showFailedOperationDialog(resource, 'restart', response.errMsg);
            }
          });
        };
        __confirmedAction(myRootId, resource, doRestart, getRestartConfirmationMessage, i18n.RESTART, false);
        });

      // Add buttons in correct order
      if(__hasAdvancedOptions(resource)){
        tableContainer.addChild(startExpandButton);
        tableContainer.addChild(startCleanButton);
      }
      else{
        tableContainer.addChild(startButton);
      }      
      tableContainer.addChild(restartButton);
      tableContainer.addChild(stopButton);
      
      if (resource.type !== 'application' &&
          !(resource.type === 'appOnServer' && resource.parentResource.type === 'standaloneServer')){ 
        var tagsMetadataButton = __createTagsAndMetadataButton(myRootId, resource);
        tableContainer.addChild(tagsMetadataButton);
      }

      if (resource.type === 'server' ) {
        var mmbServer = __createMaintenanceModeButton(myRootId + '-maintenanceModeButton', resource, function(me) {
          var doMaintenanceMode = function(resource, breakAffinity, force) {
            if ( resource.maintenanceMode != constants.MAINTENANCE_MODE_IN_MAINTENANCE_MODE ) {
              resource.setMaintenanceMode(breakAffinity, force).then(function(response) {
                if ( response.errMsg ) {
                  showFailedOperationDialog(resource, 'setMaintenanceMode', response.errMsg);
                } else if ( response.showForce && response.showForce === true ) {
                  // TODO show force
                  var widgetId = 'askForceDialog';
                  utils.destroyWidgetIfExists(widgetId);
                  var forceDialog = new YesNoDialog({
                    id : widgetId,
                    title : i18n.MAINTENANCE_MODE_FAILED,
                    descriptionIcon : imgUtils.getSVGSmallName('status-alert'),
                    yesButtonText: i18n.MAINTENANCE_MODE_FORCE_LABEL,
                    noButtonText: i18n.MAINTENANCE_MODE_CANCEL_LABEL,
                    description : i18n.MAINTENANCE_MODE_ALTERNATE_SERVERS_NOT_STARTED,
                    message : i18n.MAINTENANCE_MODE_SELECT_FORCE_MESSAGE,
                    yesFunction : function() {
                      resource.setMaintenanceMode(breakAffinity, true).then(function(response) {
                        if ( response.errMsg ) {
                          showFailedOperationDialog(resource, 'setMaintenanceMode', response.errMsg);
                        }
                      });
                    },
                    noFunction : function() {
                    }
                  });
                  forceDialog.placeAt(win.body());
                  forceDialog.startup();
                  forceDialog.show();

                }
              });
            }
            else {
              resource.unsetMaintenanceMode().then(function(response) {
                if ( response.errMsg ) {
                    showFailedOperationDialog(resource, 'unsetMaintenanceMode', response.errMsg);
                }
              });
            }
              
          };

          /** Construct the confirmation dialog. Stop is only done if confirmed **/
          __clearConfirmationPane(actionBarDialog);

          // TODO: We still need to clear the action bar faster than we do right now
          __displayEnableDisableMaintenanceModePopupDialog(resource, function() {
            if ( resource.maintenanceMode === constants.MAINTENANCE_MODE_IN_MAINTENANCE_MODE ) {
              dropDownParent.closeDropDown();
              __clearConfirmationPane(actionBarDialog);
              console.log ("submit disable maintenance mode on server.");
              doMaintenanceMode(resource);
            }
            else {
              dropDownParent.closeDropDown();
              __clearConfirmationPane(actionBarDialog);
              var breakAffinityToggleButton = dom.byId(ID.getBreakAffinityToggleButton());
              console.log ("submit enable maintenance mode on server with break affinity set to " + breakAffinityToggleButton.checked);
              doMaintenanceMode(resource, breakAffinityToggleButton.checked, false);
            }
          });
        });
        tableContainer.addChild(mmbServer);
      }
        
      // Bind the buttons and no action pane into the action bar in an organized manner
      actionBarDialog.buttons = {};
      actionBarDialog.buttons.startButton = startButton;
      if(__hasAdvancedOptions(resource)){
        actionBarDialog.buttons.startCleanButton = startCleanButton;
      }      
      actionBarDialog.buttons.stopButton = stopButton;
      actionBarDialog.buttons.restartButton = restartButton;
      actionBarDialog.buttons.mmbServer = mmbServer;

      // When the ActionBar is opened, ensure the available options are correct and up to date
      actionBarDialog.on('open', function(evt) {
        //show action button as selected
        showHoverIcon(parentId, true);
        
        // Remove confirmation panels
        __clearConfirmationPane(actionBarDialog);

        // Check to see if the actions should always be disabled
        var preventActions = disableActionsInMenu(this.resource);
        if (preventActions) {
          this.buttons.startButton.setDisabled();
          if(__hasAdvancedOptions(resource)){
            this.buttons.startCleanButton.setDisabled();
          }          
          this.buttons.stopButton.setDisabled();
          this.buttons.restartButton.setDisabled();
        } else {
          this.buttons.startButton.updateEnabled();
          if(__hasAdvancedOptions(resource)){
            this.buttons.startCleanButton.updateEnabled();
          }          
          this.buttons.stopButton.updateEnabled();
          this.buttons.restartButton.updateEnabled();
        }
        if ( 'server' === this.resource.type ) {
          this.buttons.mmbServer.updateEnabled();
        }
      });

      // When the ActionBar is closed, we may need to reset the positional information of the 
      // dialog and the "V" connector.
      actionBarDialog.on('close', function(evt) {
        //Change action icon back to original 
        this.closing = true;
        showHoverIcon(parentId, false);
        this.closing = false;
        
        // Remove the class that adds a margin to force the dialog above the button.
        domClass.remove(this.domNode.id, "dropDownPositionAboveButton");

        // Reset the position style property and top properties if they are set.
        domStyle.set(this.domNode.id, "position", (this.originalPosition) ? this.originalPosition : "");
        domStyle.set(this.domNode.id, "top", (this.originalTop) ? this.originalTop : "");
        domStyle.set(this.domNode.id, "width", (this.originalWidth) ? this.originalWidth : "");
        
        // Reset the advanced options buttons
        if(__hasAdvancedOptions(resource)){
          this.buttons.startCleanButton.domNode.classList.add('expandedButtonHidden'); 
        }

        // Query the connector again, and reset its style values.
        query(".dijitTooltipConnector", this.domNode.id).forEach(function(node) {
          console.log("Connector found");

          domStyle.set(node, "position", (node.originalPosition) ? node.originalPosition : "");
          domStyle.set(node, "left", (node.originalLeft) ? node.originalLeft : "");
        });

        // Set the dialogOpen to false, so that we will do the re-positioning if we
        // need to put the dialog above the button, the next time we display it.
        this.dialogOpen = false;
      });
    }

    return actionBarDialog;
  }

  /**
   * Common logic to create either an ActionButton (the 'cog') or a StateButton ('running/stopped' button). The only fundamental
   * difference between these two buttons is the label, which controls the icon used. This object will include a StateIcon if we are
   * constructing a StateButton. The StateIcon will be used as the label of the StateButton. Other than this label difference, the
   * behaviour of displaying the dropdown and the available buttons is exactly the same.
   * 
   * @param isActionButton
   *          Boolean. True indicates the button should be a 'cog', false indicates it should be a StateIcon.
   */
  function __createActionOrStateButton(isActionButton, resource, parentId, baseClass, iconSize) {
    var thisButtonId = isActionButton ? parentId + '-actionButton' : parentId + '-stateButton';

    var existingButton = registry.byId(thisButtonId);
    if (existingButton) {
      existingButton.destroy();
    }
    var message = "";
    var menuMessage = "";
    if(resource.type === "cluster")
    {
     message = lang.replace(i18n.SHOW_CLUSTER_ACTIONS_LABEL, [ resource.name ]);
    }
    else if(resource.type === "server")
    {
     message = lang.replace(i18n.SHOW_SERVER_ACTIONS_LABEL, [ resource.name ]);
    }
    else if(resource.type === "application" || resource.type === "appOnServer" || resource.type === "appOnCluster")
    {
     var insert = resource.name;
     if (resource.type === "appOnServer") {
       insert = lang.replace(i18n.RESOURCE_ON_SERVER_RESOURCE, [resource.type + " " + resource.name, resource.server.name]);
     } else if (resource.type === "appOnCluster") {
       insert = lang.replace(i18n.RESOURCE_ON_CLUSTER_RESOURCE, [resource.type + " " + resource.name, resource.cluster.name]);
     }
     message = lang.replace(i18n.SHOW_APP_ACTIONS_LABEL, [ resource.name ]);
    }
    else if(resource.type === "host")
    {
     message = lang.replace(i18n.SHOW_HOST_ACTIONS_LABEL, [ resource.name ]);
    }
    else if(resource.type === "runtime")
    {
     message = lang.replace(i18n.SHOW_RUNTIME_ACTIONS_LABEL, [ resource.name ]);
    }
    if (thisButtonId.indexOf("collection") === 0) {
        menuMessage = lang.replace(i18n.SHOW_COLLECTION_MENU_LABEL, [thisButtonId]);
    } else {
        menuMessage = lang.replace(i18n.SHOW_SEARCH_MENU_LABEL, [thisButtonId]);
    }
    if (globalIsAdmin === false) {
        message = i18n.ACTION_DISABLED_FOR_USER;
        menuMessage = i18n.ACTION_DISABLED_FOR_USER;
    }
    // Build the button
    var asButton = new DropDownButton({
      id : thisButtonId,
      disabled : (globalIsAdmin === false ? true : false),
      "aria-disabled": (globalIsAdmin === false ? true : false),
      "aria-label": message,
      "aria-haspopup" : true,
      title : message,
      value : thisButtonId,
      iconClass : '',
      baseClass : (baseClass ? baseClass : 'dropDownButton')
    }, (isActionButton) ? 'ActionButton' : 'StateButton');
   
    on(asButton, "mouseover", function() {
      showHoverIcon(asButton.id, true);
    });
    on(asButton, "mouseout", function() {
      showHoverIcon(asButton.id, false);
    });

    // Add an aspect to allow us to fire before the openDropDown method, and gives us the opportunity
    // to affect whether the dropdown is above or below the button.
    aspect.before(asButton, "toggleDropDown", function() {
      // Bind in the action bar drop down, but only once the dropdown button is clicked
      if (asButton.dropDown === undefined || asButton.dropDown === null) {
        asButton.dropDown = __createActionBarDropDown(resource, thisButtonId, asButton, menuMessage);
      }
      
      // get the dimentions of the page. Also get the position of the state button.
      var documentwindow = dojoWindow.getBox();
      var dialogPosition = domGeometry.position(this.domNode);

      // We need to check that we have enough space to display the full dialog in the window, both below and
      // above the card. If we don't have enough room in either, then we must display below because the user 
      // can't do anything to get that displayed. They could resize the screen if it is off the bottom.
      // We shouldn't do this for action buttons as they are always at the top of the screen and should
      // always be down the page.
      // If we can't then we add a css class, that puts a margin-top to the dialog that will
      // force the dropdown to put the dialog above the button.
      // If we can fit it in, try and remove the css class, in case it is still set.
      var displayAbove = false;
      // If we don't have space at the top of the screen just display below.
      if (dialogPosition.y < 300) {
        displayAbove = false;
        // If we don't have 300 pixels below the card then display above.
      } else if (documentwindow.h - (dialogPosition.y + dialogPosition.h) < 300) {
        displayAbove = true;
        // Otherwise display below.    
      } else {
        displayAbove = false;
      }

      if (displayAbove) {
        console.log("Configuring the dropdown above the button");
        domClass.add(asButton.dropDown.domNode, "dropDownPositionAboveButton");
      } else {
        domClass.remove(asButton.dropDown.domNode, "dropDownPositionAboveButton");
      }
      __clearConfirmationPane(asButton.dropDown);
    });

    // Set the label based on whether or not this is an ActionButton
    if (isActionButton) {
      if (globalIsAdmin) {
        asButton.set('label', imgUtils.getSVGSmall('menu-action', 'actionMenu'));
      } else {
        asButton.set('label', imgUtils.getSVGSmall('menu-action-disabled', 'actionMenu'));
      }
    } else {
      // Build and bind the StateIcon used as the label for this button
      if (iconSize) {
        asButton.stateIcon = new StateIcon({
          parentId : thisButtonId,
          resource : resource,
          alt : message,
          size: iconSize
        });
      } else {
        asButton.stateIcon = new StateIcon({
          parentId : thisButtonId,
          resource : resource,
          alt : message
        });
      }


      // Override the destroy method to invoke StateIcon.destroy
      asButton.origDestroy = asButton.destroy;
      asButton.destroy = function() {
        console.log('Custom destroy for StateButton: ' + this.id);
        this.stateIcon.destroy();
        this.origDestroy();
      };

      // Set the state label to be the StateIcon's HTML element
      asButton.set('label', asButton.stateIcon.getHTML());
    }

    return asButton;
  }
  
 
  /**
   * Creates an action DropDownButton.
   */
  function __createActionButton(resource, parentId, baseClass) {
    return __createActionOrStateButton(true, resource, parentId, baseClass);
  }

  /**
   * Creates a state DropDownButton.
   */
  function __createStateButton(resource, parentId, baseClass, iconSize) {
    return __createActionOrStateButton(false, resource, parentId, baseClass, iconSize);
  }

  /**
   * Creates a <span> with the correct text direction. Used everywhere so consolidate code into one place.
   * @param name the string that the span will go around
   */
  function __getSpanOpen(name) {
    return '<span dir="' + utils.getStringTextDirection(name) + '">';
  }
  return {
    createStartButton : __createStartButton,
    createStartCleanButton : __createStartCleanButton,
    createStopButton : __createStopButton,
    createRestartButton : __createRestartButton,
    displayStopPopupDialog : __displayStopPopupDialog,
    displayClusterStopPopupDialog : __displayClusterStopPopupDialog,
    createActionButton : __createActionButton,
    createStateButton : __createStateButton,
    createMaintenanceModeButton : __createMaintenanceModeButton
  };

  function __createActionTable (actionBarDialog) {
    var tableContainer = new TableContainer({
      cols : 1,
      showLabels : false,
      'class' : "tableContainerDropDownActionButton"
    });
    tableContainer.layout = function() {
      TableContainer.prototype.layout.apply(this);
      // should ALWAYS have a table but check first just in case
      if (this.table) {
        domAttr.set(this.table, "role", "presentation");
      }
    };

    actionBarDialog.addChild(tableContainer);
	return tableContainer;  
  }

  function displayStopMessage(messageContents, isSuccess) {
    var alertIcon = imgUtils.getSVGSmallName('status-alert-gray');
   
    var messageDialog = new MessageDialog({
      title : i18n.STANDALONE_STOP_TITLE,
      messageText : messageContents,
      messageDialogIcon : (isSuccess ? '' : alertIcon),
      okButtonLabel: widgetI18N.TOOLBOX_BUTTON_OK,
      okFunction : function() {
        // Do nothing!
      }
    });
    messageDialog.placeAt(document.body);
    messageDialog.startup();
    messageDialog.show();
  }

  /**
   * Shows proper icon on hover or select
   * 
   * parentId - String
   * show - boolean
   */
  function showHoverIcon(buttonId, show){
      var actionButton = document.getElementById(buttonId + "-stateIcon");
      if (globalIsAdmin === false) {
        show = false;
      }
      
      if (actionButton !=null) {
        var img = actionButton.getElementsByTagName('img')[0];
        var stoppingCheck = img["title"]+"";
        if (show && !(stoppingCheck===i18n.STOPPING) && !(stoppingCheck===i18n.STARTING)) {
          img.src = 'imagesShared/card-action-hover-T.png';
        }else if (stoppingCheck===i18n.STOPPING) {
          var dropdown = registry.byId(buttonId + "-actionMenu");
          if (!(dropdown && dropdown.focused)) {
            img.src = 'images/status-stopping-T.gif';
          }
        }else if(stoppingCheck===i18n.STARTING) {
          var dropdown = registry.byId(buttonId + "-actionMenu");
          if (!(dropdown && dropdown.focused)) {
            img.src = 'images/status-starting-T.gif';
          }
        } else {
          var dropdown = registry.byId(buttonId + "-actionMenu");
          // dropdown.closing was added for the case when the menu was displayed
          // and then the user selected a different card's actionButton to 
          // display a new menu.  At this time, dropdown.focused is still set for
          // the first actionButton's dropdown menu since closing has not completed.
          if (!(dropdown && dropdown.focused) || dropdown.closing) {
            img.src = 'imagesShared/card-action-T.png';
          }
        }
      }
  }

});