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
define(['dojo/_base/declare', 'dojo/_base/lang', 'dojo/dom', 'dojo/has', 'jsExplore/utils/constants', 'jsExplore/utils/ID',
        'dijit/_WidgetBase', 'dijit/_TemplatedMixin', 'dijit/_WidgetsInTemplateMixin', 'dojo/_base/fx', 'dojo/fx', 'dojo/keys',
        'dijit/registry', 'dijit/layout/ContentPane', 'dojox/string/BidiComplex', 'jsExplore/views/viewFactory', 'dojo/on',
        'jsExplore/widgets/shared/ActionButtons', 'jsExplore/widgets/ResourceButton', 'jsExplore/widgets/TagButton',
        'dojo/text!./templates/ObjectViewHeaderPane.html', 'dojo/i18n!../nls/explorerMessages', 'dijit/form/ToggleButton',
        'jsExplore/resources/Observer', 'jsExplore/resources/utils', 'jsShared/utils/imgUtils', 'dojo/dom-geometry', 'dojo/domReady!'
        ], function(declare, lang, dom, has, constants, ID,
            WidgetBase, _TemplatedMixin, _WidgetsInTemplateMixin, fx, coreFx, keys,
            registry, ContentPane, BidiComplex, viewFactory, on,
            ActionButtons, ResourceButton, TagButton,
            template, i18n, ToggleButton,
            Observer, utils, imgUtils, domGeom) {

  return declare([ WidgetBase, _TemplatedMixin, _WidgetsInTemplateMixin, Observer ], {
    id: 'OVHP', // Will be set by constructor
    widgetsInTemplate: true,
    region: 'top', // Set the region, required by the template parent
    templateString: template,

    resource: null, // The resource that this view is for

    // Template variables
    resourceId: 'setMe', // the resource.id value
    resourceIconClass: 'setMe', // avatorServerIcon, avatorHostIcon, etc
    resourceName: 'setMe', // resource.name
    resourceNameClass: 'setMe', // css class
    resourceNote: '', //Optional
    resourceDescriptor: '', //Optional.
    resourceNameQualifier1: '', // Optional.
    resourceNameQualifier2: '', // Optional.
    hostMaintenanceModeNotificationTitle: '',
    textDir: "ltr",
    serverApiIcon: '', // 'setMe'
    serverApiMessage: i18n.SERVER_API_DOCMENTATION,

    constructor : function(params) {
      this.textDir = utils.getBidiTextDirectionSetting();
      var resource = params[0];
      this.resource = resource;
      this.resourceId = resource.id;
      this.resourceIconClass = getResourceIconClass(resource);
      if ( resource.type === 'host' ) {
        if (resource.servers.list.length > 10 )
          this.hostMaintenanceModeNotificationTitle = i18n.MAINTENANCE_MODE_ENABLING_MAINTENANCE_MODE_ON_ALL_HOST_SERVERS_MORE_THAN_10;
        else
          this.hostMaintenanceModeNotificationTitle = i18n.MAINTENANCE_MODE_ENABLING_MAINTENANCE_MODE_ON_ALL_HOST_SERVERS_EQUAL_OR_LESS_THEN_10;
      }
      this.resourceName = this.getResourceName(resource);
      this.id = 'OVHP-' + resource.type + '-' + resource.id;

      // Set the additional name qualifiers for server, runtime and standaloneServer
      if (resource.type === 'server') {
        this.resourceDescriptor = this._determineServerDescriptor(resource);
        if (utils.getBidiTextDirectionSetting() !== "ltr") {
          this.resourceNameQualifier1 = BidiComplex.createDisplayString(resource.userdir, 'FILE_PATH');
        } else {
          this.resourceNameQualifier1 = resource.userdir;
        }
      } else if (resource.type === 'standaloneServer') {
        this.resourceDescriptor = this._determineServerDescriptor(resource);
        if (utils.getBidiTextDirectionSetting() !== "ltr") {
          this.resourceNameQualifier1 = BidiComplex.createDisplayString(resource.userdir, 'FILE_PATH');
        } else {
          this.resourceNameQualifier1 = resource.userdir;
        }
        this.resourceNameQualifier2 = resource.host;
      } else if (resource.type === 'runtime') {
        if (resource.containerType && resource.containerType === constants.CONTAINER_DOCKER) {
          this.resourceDescriptor = i18n.RUNTIME_DOCKER;
        }
        if (utils.getBidiTextDirectionSetting() !== "ltr") {
          this.resourceNameQualifier1 = BidiComplex.createDisplayString(resource.path, 'FILE_PATH');
        } else {
          this.resourceNameQualifier1 = resource.path;
        }
      }

      if (resource.type === 'appOnCluster' || resource.type === 'appOnServer') {
        this.resourceNameClass = "resourceAppOnResource";
      } else {
        this.resourceNameClass = "resourceName";
      }
      this.resource.subscribe(this);
    },

    postCreate: function() {
      this.iconNode.innerHTML = imgUtils.getSVG(getResourceIcon(this.resource));

      /* Set the StateIcon if needed */
      if ((this.resource.type !== 'host') && (this.resource.type !== 'standaloneServer') && (this.resource.type !== 'runtime')) {
        this.stateIcon = getStateIcon(this.resource);
        this.stateDiv.set('content', this.stateIcon.getHTML('objectViewStateIcon'));
      } else {
        this.stateDiv.domNode.style.display = 'none';
      }

      setAutoScaledIcon(this.autoScaledLabel, this.resource);
      setMaintenanceModeIcon(this.maintenanceModeState, this.resource);

      /* Set action button */
      this.actionButton = ActionButtons.createActionButton(this.resource, this.resource.id, 'editButtons');
      this.actionButtonParent.addChild(this.actionButton);

      var resource = this.resource;
      var relatedResourceContainer = this.relatedResourceContainer;
      var memberOfButton = this.memberOfButton;
      var relatedResourceIcon = this.relatedResourceIcon;
      setClusterButton(resource, relatedResourceContainer, relatedResourceIcon, memberOfButton);

      var me = this;
      this.toggleButton = new ToggleButton({
        id: this.id + '-ResourceToggle',
        baseClass: 'toggleButton',
        iconClass: 'metaDataIcon',
        showLabel: true,
        checked: false,
        onChange: function(val) {
          if (val) {
            me.allTagContainer.domNode.style.display = 'block';
            // Compact tags pane into single row
            compactTagAndContactPane(me.resource);
            me.toggleButton.iconNode.innerHTML = imgUtils.getSVG("triangleDown");
          } else {
            me.allTagContainer.domNode.style.display = 'none';
            me.toggleButton.iconNode.innerHTML = imgUtils.getSVG("triangleSideways");
          }
        },
        label : i18n.SET_ATTRIBUTES
      });
      this.toggleButton.iconNode.innerHTML = imgUtils.getSVG("triangleSideways");
      this.toggleButtonParent.addChild(this.toggleButton);

      setTagButtons(this.resource, this.tagPane, this.tagIcon, this.ownerTagPane, this.ownerIcon, this.contactTagPane, this.contactIcon, this.serverPortPane, this.portIcon, this.resourceNotePane, this.notesIcon, this.toggleButton);

      // Hide tags and metadata on initial display
      this.allTagContainer.domNode.style.display = 'none';

     populateAlertContent(this.resource, this.alertPaneContainer);
    },

    startup: function() {
      this.inherited(arguments);
      // startup is when this object is attached to the DOM.
      setServerApiButton(this.resource, this.apiDefIcon);
    },

    destroy: function() {
      this.inherited(arguments);
      this.resource.unsubscribe(this);

      if (this.stateIcon) {
        this.stateIcon.destroy();
      }
      if (this.actionButton) {
        this.actionButton.destroy();
      }
      if (this.allTagContainer) {
        this.allTagContainer.destroy();
      }
      if (this.tagPane) {
        this.tagPane.destroyRecursive();
      }
      if (this.ownerTagPane) {
        this.ownerTagPane.destroyRecursive();
      }
      if (this.contactTagPane) {
        this.contactTagPane.destroyRecursive();
      }
      if (this.serverPortPane) {
        this.serverPortPane.destroy();
      }
      if (this.resourceNotePane) {
        this.resourceNotePane.destroy();
      }
      if (this.toggleButton) {
        this.toggleButton.destroy();
      }
    },

    /**
     * Retrieve the resource name.
     *
     * @return The content to place in for the resource name.
     */
    getResourceName : function(resource) {
      var type = resource.type;
      var resourceTitle = null;
      switch (type) {
      case 'cluster':
        resourceTitle = i18n.TITLE_FOR_CLUSTER;
        break;
      case 'host':
        resourceTitle = i18n.TITLE_FOR_HOST;
        break;
      }

      switch (type) {
      case 'cluster':
      case 'host':
      case 'server':
      case 'standaloneServer':
      case 'runtime':
        var spanOpen = '<span dir="' + this.getTextDir(resource.name) + '" class="resourceName">';
        var spanClose = '</span>';
        if (resourceTitle) {
          return '<span class="resourceName">' + lang.replace(resourceTitle, [spanOpen + resource.name + spanClose]) + spanClose;
        } else {
          return spanOpen + resource.name + spanClose;
        }
      case 'appOnServer':
        var appOnServer = resource;
        var spanOpen1 = '<span dir="' + this.getTextDir(appOnServer.name) + '" class="resourceAppName" style="top:0;left:0">';
        var spanOpen2 = '<span dir="' + this.getTextDir(appOnServer.server.name) + '" class="resourceAppOnResource" style="top:0;left:0">';
        var spanClose = '</span>';

        var content = '<span class="resourceAppOnResource">'
            + lang.replace(i18n.RESOURCE_ON_SERVER_RESOURCE, [ spanOpen1 + appOnServer.name + spanClose,
                                                        spanOpen2 + appOnServer.server.name + spanClose ]) + '</span>';
        return content;
      case 'appOnCluster':
        var appOnCluster = resource;
        var spanOpen1 = '<span dir="' + this.getTextDir(appOnCluster.name) + '" class="resourceAppName" style="top:0;left:0">';
        var spanOpen2 = '<span dir="' + this.getTextDir(appOnCluster.cluster.name) + '" class="resourceAppOnResource" style="top:0;left:0">';
        var spanClose = "</span>";

        var content = '<span class="resourceAppOnResource">'
            + lang.replace(i18n.RESOURCE_ON_CLUSTER_RESOURCE, [ spanOpen1 + appOnCluster.name + spanClose,
                                                        spanOpen2 + appOnCluster.cluster.name + spanClose ]) + '</span>';
        return content;
      default:
        console.error('ObjectViewHeaderPane.getResourceName called for an unknown resource type: ' + type);
      }
    },

    /**
     * Create the overview descriptor for the provided server.
     *
     * @param resource
     *
     * @returns <String>  The resource descriptor.
     */
    _determineServerDescriptor: function(resource) {
      var descriptor = "";
      if (resource.type === 'server') {
        if (resource.isCollectiveController) {
          descriptor = i18n.COLLECTIVE_CONTROLLER_DESCRIPTOR;
        } else {
          if (resource.containerType && resource.containerType === constants.CONTAINER_DOCKER) {
            descriptor = i18n.LIBERTY_IN_DOCKER_DESCRIPTOR;
            if (resource.runtimeType && resource.runtimeType === constants.RUNTIME_NODEJS){
                       // Node.js in Docker container
              descriptor = i18n.NODEJS_IN_DOCKER_DESCRIPTOR;
            }
          } else {
            if (resource.runtimeType && resource.runtimeType === constants.RUNTIME_NODEJS) {
              descriptor = i18n.NODEJS_SERVER;
            } else {
              descriptor = i18n.LIBERTY_SERVER;
            }
          }
        }

      }
      return descriptor;
    },

    onClusterChange: function() {
      setClusterButton(this.resource, this.relatedResourceContainer, this.relatedResourceIcon, this.memberOfButton);
    },

    /**
     * onChange listener for maintenance mode change
     */
    onMaintenanceModeChange: function() {
      setMaintenanceModeIcon(this.maintenanceModeState, this.resource);
      fadeInOutMaintenanceModeNotificationPane(this.resource);
    },

    /**
     * onChange listener for scaling policy name change
     */
    onScalingPolicyChange: function() {
      setAutoScaledIcon(this.autoScaledLabel, this.resource);
    },

    /**
     * onChange listener for scaling policy enabled name change
     */
    onScalingPolicyEnabledChange: function() {
      setAutoScaledIcon(this.autoScaledLabel, this.resource);
    },

    onTagsChange: function() {
      setTagButtons(this.resource, this.tagPane, this.tagIcon, this.ownerTagPane, this.ownerIcon, this.contactTagPane, this.contactIcon, this.serverPortPane, this.portIcon, this.resourceNotePane, this.notesIcon, this.toggleButton);
    },

    onOwnerChange: function() {
      setTagButtons(this.resource, this.tagPane, this.tagIcon, this.ownerTagPane, this.ownerIcon, this.contactTagPane, this.contactIcon, this.serverPortPane, this.portIcon, this.resourceNotePane, this.notesIcon, this.toggleButton);
    },

    onContactsChange: function() {
      setTagButtons(this.resource, this.tagPane, this.tagIcon, this.ownerTagPane, this.ownerIcon, this.contactTagPane, this.contactIcon, this.serverPortPane, this.portIcon, this.resourceNotePane, this.notesIcon, this.toggleButton);
    },

    onPortsChange: function() {
      setTagButtons(this.resource, this.tagPane, this.tagIcon, this.ownerTagPane, this.ownerIcon, this.contactTagPane, this.contactIcon, this.serverPortPane, this.portIcon, this.resourceNotePane, this.notesIcon, this.toggleButton);
    },

    onNoteChange: function() {
      setTagButtons(this.resource, this.tagPane, this.tagIcon, this.ownerTagPane, this.ownerIcon, this.contactTagPane, this.contactIcon, this.serverPortPane, this.portIcon, this.resourceNotePane, this.notesIcon, this.toggleButton);
    },

    onApiDiscoveryChange: function() {
      setServerApiButton(this.resource, this.apiDefIcon);
    }
  });

  /**
   * Retrieve the correct icon for the resource type.
   *
   * @return The resource icon
   */
  function getResourceIcon(resource) {
    var type = resource.type;
    switch (type) {
    case 'appOnCluster':
      return 'appOnCluster';
    case 'appOnServer':
      return 'instance';

    case 'cluster':
    case 'host':
      return type;
    case 'runtime':
      switch (resource.runtimeType) {
        case constants.RUNTIME_LIBERTY:
          return 'runtimeLiberty';

        case constants.RUNTIME_NODEJS:
          return 'node';

        default:
          console.error('Runtime resource with unknown runtimeType: ' + resource.runtimeType);
          return 'runtime';
      }

    case 'server':
    case 'standaloneServer':
      if (resource.isCollectiveController) {
        return 'collectiveController';
      } else {
        return 'server';
      }
    default:
      console.error('ObjectViewHeaderPane.getResourceIcon called for an unknown resource type: ' + type);
    }
  }

  /**
   * Retrieve the correct icon class for the resource type.
   *
   * @return The class name for the resource icon
   */
  function getResourceIconClass(resource) {
    var type = resource.type;
    switch (type) {
    case 'appOnCluster':
      return 'avatorAppOnClusterIcon';
    case 'appOnServer':
      return 'avatorInstanceIcon';
    case 'cluster':
      return 'avatorClusterIcon';
    case 'host':
      return 'avatorHostIcon';
    case 'server':
    case 'standaloneServer':
      if (resource.isCollectiveController) {
        return 'avatorCollectiveControllerIcon';
      } else {
        return 'avatorServerIcon';
      }
    case 'runtime':
      switch (resource.runtimeType) {
        case constants.RUNTIME_LIBERTY:
          return 'avatorRuntimeIcon';

        case constants.RUNTIME_NODEJS:
          return 'avatorNodejsIcon';

        default:
          console.error('Runtime resource with unknown runtimeType: ' + resource.runtimeType);
          return 'avatorRuntimeBasicIcon';
      }

    default:
      console.error('ObjectViewHeaderPane.getResourceIconClass called for an unknown resource type: ' + type);
    }
  }

  /**
   * Construct a StateIcon the resource name.
   *
   * @return The content to place in for the resource name.
   */
  function getStateIcon(resource) {
    return new StateIcon({
      parentId: resource.id,
      resource: resource,
      size: "20",
      cardState: true,
      showLabel: true
    });
  }

  /**
   * Fade in the notification then fade out the notification
   */
  function fadeInOutMaintenanceModeNotificationPane (resource) {
    var resourceId = resource.id + "-HostMaintenanceModeNotificationContainer";

    var node = dom.byId( resourceId );
    if ( node && resource.type === 'host' && resource.maintenanceMode === constants.MAINTENANCE_MODE_IN_MAINTENANCE_MODE) {
      coreFx.chain([
        fx.fadeIn({
          node: resourceId,
          duration: 750, // default 350ms seems too fast
          delay: 750, // delay 750 ms
          beforeBegin: function(){
            node.style.display = "block";
          }
        }),
      fx.fadeOut({
        node: resourceId,
        duration: 750, // default 350ms seems too fast
        delay: 10000, // delay 10 seconds

        onEnd: function(){
          node.style.display = "none";
        }
      })]).play();
    }
  }
  /**
   * Sets the maintenance mode widget content to the appropriate HTML.
   * If the resource is not server nor a host, the content is empty and the parent div will be hidden.
   * If the resource is host and server: if the maintenance mode is disabled, the div will be hiddeen;
   *        if the maintenance mode is enabled, the div will be displayed and the content will be
   *        set to appropriate HTML.
   * The maintenanceMode attribute could be one of these:
   *    inMaintenanceMode
   *    notInMaintenanceMode
   *    alternateServerStarting
   *    alternateServerUnavailable
   */
  function setMaintenanceModeIcon(maintenanceModeState, resource) {
    if ( resource.type === 'server' || resource.type === 'host' || resource.type === 'appOnServer') {
      maintenanceModeState.domNode.style.display = "inline-block";
      // if maintenance mode is disabled then do not display maintenance mode
      if ( resource.maintenanceMode === null || resource.maintenanceMode === constants.MAINTENANCE_MODE_NOT_IN_MAINTENANCE_MODE || resource.maintenanceMode === constants.MAINTENANCE_MODE_ALTERNATE_SERVER_UNAVAILABLE ) { // not in maintenance mode
        maintenanceModeState.domNode.style.display = "none";
      } else if ( resource.maintenanceMode === constants.MAINTENANCE_MODE_IN_MAINTENANCE_MODE || resource.maintenanceMode === constants.MAINTENANCE_MODE_ALTERNATE_SERVER_STARTING ){
        //maintenance mode enabled
        var maintenanceModeText = i18n.ENABLING_MAINTENANCE_MODE;
        var mmEnabledIcon = '<img src="images/enabling-maintenance-mode-20-DT.gif" style="vertical-align: middle;" height="20" width="20" alt="' + maintenanceModeText + '">';
        if ( resource.maintenanceMode === constants.MAINTENANCE_MODE_IN_MAINTENANCE_MODE) {
          // set the enabled content
          var maintenanceModeText = i18n.MAINTENANCE_MODE;
          var mmEnabledIcon = imgUtils.getSVGSmall('maintenanceMode');
        }
        var html = '<span class="maintenanceModeLabel">'+ maintenanceModeText + '</span>';
        maintenanceModeState.set('content', mmEnabledIcon + html);
      }
    } else {
      maintenanceModeState.domNode.style.display = "none";
    }
  }
  /**
   * Set the autoScaledLabel widget content to the appropriate HTML.
   * If the resource has no scaling policy, then the contents is empty.
   * If the resource has an enabled scaling policy, the contents is an enabled icon and text.
   * If the resource has a disabled scaling policy, then the contents are a disabled icon and text.
   */
  function setAutoScaledIcon(autoScaledLabel, resource) {
    if (resource.scalingPolicy) {
      // Assume its disabled
      var policyText = i18n.AUTOSCALED_POLICY_DISABLED;
      var policyIcon = imgUtils.getSVGSmall('autoscaling-disabled');
      var state = 'autoscaleDisabled';
      // Check if really enabled
      if (resource.scalingPolicyEnabled) {
        policyText = i18n.AUTOSCALED_POLICY;
        policyIcon = imgUtils.getSVGSmall('autoscaling-enabled');
        state = 'autoscaleEnabled';
      }

      var html = policyIcon + '<span class="scalingPolicyLabel ' + state + '">'+ policyText + '</span>';
      autoScaledLabel.set('content', html);
      autoScaledLabel.domNode.style.display = "inline-block";
    } else {
      autoScaledLabel.set('content', '');
      autoScaledLabel.domNode.style.display = "none";
    }
  }

  function setClusterButton(resource, relatedResourceContainer, iconPane, memberOfButton) {
    // Destroy any and all descendants. This will clear the old cluster button(s).
    memberOfButton.destroyDescendants();

    /* Set memberOf section if needed */
    var type = resource.type;
    switch (type) {
    case 'appOnServer':
      setAppOnServerClusterButton(resource, relatedResourceContainer, iconPane, memberOfButton);
      break;
    case 'server':
    case 'standaloneServer':
      setServerClusterButton(resource, relatedResourceContainer, iconPane, memberOfButton);
      break;
    case 'cluster':
    case 'host':
    case 'runtime':
    case 'appOnCluster':
      hideClusterButton(relatedResourceContainer);
      break;
    default:
      console.error('ObjectViewHeaderPane.createResourcePage called for an unknown resource type: ' + type);
    }
  }

  function setServerApiButton(resource, apiDefIcon) {
    // 1.  Only allow button for server views
    //
    var type = resource.type;
    if(type !== ID.getServer() && type !== ID.getStandaloneServer()) {
      // Keep the server api button the default hidden styling
      return;
    }

    // 2. Get the server api button so we can manipulate the attributes/styling
    //
    var id = ID.dashDelimit(resource.id, ID.SERVERAPICONTAINER);
    var serverApiButton = document.getElementById(id);

    // 3.  Fail safe the method if server api button is missing
    //
    if(typeof(serverApiButton) === "undefined" || serverApiButton === null ) {
      console.error('ObjectViewHeaderPane.setServerApiButton serverApiButton not found: '
          + serverApiButton);
      return; // Quiet return. Fail safe. This should not happen. Ever.
    }

    // 4.  Determine if to show server api button and what button styling based on
    // server's state and presence of apiDiscovery feature on server
    //
    var explorerURL = resource.explorerURL;
    if (explorerURL) {
      if (resource.state === ID.getSTARTED() || resource.type === 'standaloneServer') {
        // If server running and apiDiscovery enabled, enable button
        // If a standAlone server, the feature is enabled since we don't track the state of
        // a standAlone server.  It is assumed to be up.
        enableServerApiButton(serverApiButton, apiDefIcon, explorerURL);
      } else {
        // If server not started and apiDiscovery enabled, grey out button
        disableServerApiButton(serverApiButton, apiDefIcon);
      }
    } else {
      // If no apiDiscovery, don't display.
      hideServerApiButton(serverApiButton);
    }
  }

  function disableServerApiButton(element, apiDefIcon) {
    // Set image
    apiDefIcon.innerHTML = imgUtils.getSVGSmall('apiDefDisabled');

    // Disable link
    var link = element.getElementsByTagName("a")[0];
    link.style.display = "none";

    // Enable span
    var span = element.getElementsByTagName("span")[0];
    span.style.display = "";

    element.style.display = "inline-block";
  }

  function enableServerApiButton(element, apiDefIcon, explorerURL) {
    // Set image
    apiDefIcon.innerHTML = imgUtils.getSVGSmall('apiDefEnabled');

    // Disable span
    var span = element.getElementsByTagName("span")[0];
    span.style.display = "none";

    // Enable link
    var link = element.getElementsByTagName("a")[0];
    link.href = explorerURL;
    link.style.display = "";

    element.style.display = "inline-block";

    // I have no idea why, but none of our links in Admin Center are working when dojo is used.
    // I tried to track it down to see if something was intercepting the event, but couldn't find anything.
    // So instead, I'm intercepting this specific event to manually case the correct behavior.
    on(link, "keydown", function(evt) {
      if (evt.keyCode === keys.ENTER || evt.keyCode === keys.SPACE) {
        link.click();
      }
    });
  }

  function hideServerApiButton(element) {
    element.style.display = "none";
  }

  function setAppOnServerClusterButton(appOnServer, relatedResourceContainer, iconPane, memberOfButton) {
    appOnServer.getServer().then(function(server) {
      if (server.cluster) {
        server.getCluster().then(function(cluster) {
          var clusterButton;
          if (cluster.scalingPolicy) {
            // Set icon to scalingPolicy cluster one.
            iconPane.innerHTML = imgUtils.getSVGSmall("cluster-autoscaled-OVHP");
          } else {
            // Set icon to normal cluster one.
            iconPane.innerHTML = imgUtils.getSVG("cluster-dashboard");
          }
          clusterButton = ResourceButton.createResourceButton([ 'AppInst', appOnServer.id, cluster.name, 'Cluster']);

          clusterButton.onClick = function() {
            viewFactory.openView(cluster);
          };

          memberOfButton.addChild(clusterButton);
          showClusterButton(relatedResourceContainer);
        });
      } else {
        hideClusterButton(relatedResourceContainer);
      }
    });
  }

  function setServerClusterButton(server, relatedResourceContainer, iconPane, memberOfButton) {
    if (server.cluster) {
      var clusterName = server.cluster;
      var clusterButton;
      if (server.scalingPolicy) {
        // Set icon to scalingPolicy cluster one.
        iconPane.innerHTML = imgUtils.getSVGSmall("cluster-autoscaled-OVHP");
      } else {
        // Set icon to normal cluster one.
        iconPane.innerHTML = imgUtils.getSVG("cluster-dashboard");
      }
      clusterButton = ResourceButton.createResourceButton([ 'Server', server.id, clusterName, 'Cluster']);

      if (server.type === 'server') {   // When would this not be a server?  Already checked if server.cluster.
        server.getCluster().then(function(clusterObj) {
          clusterButton.onClick = function() {
            viewFactory.openView(clusterObj);
          };
        });
      }

      memberOfButton.addChild(clusterButton);
      showClusterButton(relatedResourceContainer);
    } else {
      hideClusterButton(relatedResourceContainer);
    }
  }

  /**
   * Hides the relatedResourceContainer contentPane in the OVHP
   *
   * @param relatedResourceContainter:  this.relatedResourceContainer in the OVHP
   */
  function hideClusterButton(relatedResourceContainer) {
    relatedResourceContainer.domNode.style.display = "none";
  }

  /**
   * Show the relatedResourceContainer contentPane in the OVHP
   *
   * @param relatedResourceContainter:  this.relatedResourceContainer in the OVHP
   */
  function showClusterButton(relatedResourceContainer) {
    relatedResourceContainer.domNode.style.display = "inline-block";
  }

  /**
   * Format the ports from JSON into a string for the OVHP
   *
   * the `.filter(function (val) {return val;})` removes null values
   * the '.sort(function (a, b){return a-b;})' sorts the port values in natural order
   *
   * @param {*} ports JSON object containing array of httpsPorts and httpPorts
   */
  function formatPorts(resource, ports) {
    if (ports.httpPorts) {
      var httpText = "<span id='" + resource.id + "-httpPorts' class='httpPorts'><b>HTTP</b>: " + ports.httpPorts.sort(function (a, b){return a-b;}).filter(function (val) {return val;}).join(', ') + "</span>";
    }    
    if (ports.httpsPorts) {
      var httpsText = "<span id='" + resource.id + "-httpsPorts'><b>HTTPS</b>: " + ports.httpsPorts.sort(function (a, b){return a-b;}).filter(function (val) {return val;}).join(', ') + "</span>";
    }

    return httpText + httpsText;
  }

  function setTagButtons(resource, tagPane, tagIcon, ownerTagPane, ownerIcon, contactTagPane, contactIcon, serverPortsPane, portIcon, resourceNotePane, notesIcon, toggleButton) {
    // Destroy any and all descendants. This will clear the old tag button(s).
    tagPane.destroyDescendants();
    ownerTagPane.destroyDescendants();
    contactTagPane.destroyDescendants();
    resourceNotePane.destroyDescendants();

    if (resource.tags) {
      var tags = resource.tags;
      if (tags.length > 0) {
        tagIcon.set('content', imgUtils.getSVGSmall('metadata-tag'));
      }
      for (var i=0; i < tags.length; i++) {
        tagPane.addChild(TagButton.createTagButton(['objectView', resource.type, resource.id, 'tag', tags[i]]));
      }
      tagPane.addChild(TagButton.createTagButton(['objectView', resource.type, resource.id, 'expand-tag', 'more']));
    }
    if (resource.owner) {
      ownerIcon.set('content', imgUtils.getSVGSmall('metadata-user'));
      ownerTagPane.addChild(TagButton.createTagButton(['objectView', resource.type, resource.id, 'owner', resource.owner]));
    }
    if (resource.contacts) {
      var contactTags = resource.contacts;
      if (contactTags.length > 0) {
        contactIcon.set('content', imgUtils.getSVGSmall('metadata-contacts'));
      }
      for (var i=0; i < contactTags.length; i++) {
        contactTagPane.addChild(TagButton.createTagButton(['objectView', resource.type, resource.id, 'contact', contactTags[i]]));
      }
      contactTagPane.addChild(TagButton.createTagButton(['objectView', resource.type, resource.id, 'expand-contact', 'more']));
    }
    if (resource.ports) {
      var ports = resource.ports;
        portIcon.set('content', imgUtils.getSVGSmall('metadata-port'));
        var list = formatPorts(resource, ports);
        serverPortsPane.set('content', list);
        serverPortsPane.set('aria-label', lang.replace(i18n.PORTS, [ports]));
    }
    if (resource.note) {
      var exp = /(\bhttps?:\/\/[-A-Z0-9+&@#\/%?=~_|!:,.;]*[-A-Z0-9+&@#\/%=~_|])/i;
      var note = resource.note;
      notesIcon.set('content', imgUtils.getSVGSmall('metadata-notes'));
      resourceNotePane.set('content', "<span dir='" + utils.getStringTextDirection(note) + "'>" + note.replace(exp,"<a href='$1' target=_blank' rel='noreferrer'>$1</a>") + "</span>");
      resourceNotePane.set('aria-label', lang.replace(i18n.NOTE_LABEL, [note]));
      resourceNotePane.domNode.style.display = 'inline-block';
      // Per Michal...Remove click functionality from Note displayed in OVHP.
//      resourceNotePane.on("click", function() {
//        viewFactory.openSearchView('note=~eq~' + resource.note);
//      });
    } else {
      resourceNotePane.set('content', '');
      resourceNotePane.domNode.style.display = 'none';
    }

    if (((resource.tags) && (resource.tags.length > 0))
        || ((resource.owner) && (resource.owner.length > 0))
        || ((resource.contacts) && (resource.contacts.length > 0))
        || ((resource.note) && (resource.note.length > 0))) {
      // show toggle button
      toggleButton.domNode.style.display = 'inline-block';
    } else {
      // hide toggle button
      toggleButton.domNode.style.display = 'none';
    }

    // Compact tags pane into two rows
    compactTagAndContactPane(resource);
  }

  function compactTagAndContactPane(resource) {
    // Compact tags pane into single row
    var nameNode = dom.byId(resource.id + '-ResourceName');
    if (nameNode) {
      var width = domGeom.getContentBox(nameNode).w;
      var tagPane = registry.byId(resource.id + '-TagPane');
      if (tagPane) {
        var tagContainerNode = dom.byId(resource.id + '-TagContainer');
        var tagPaneWidth = domGeom.getContentBox(tagPane.domNode).w;
        if (tagPane.hasChildren()) {
          if (tagContainerNode) {
            tagContainerNode.style.display = 'inline-block';
          }
          compactTagPaneDouble(tagPane, tagPaneWidth);
        } else {
          // No tags, hide the div
          if (tagContainerNode) {
            tagContainerNode.style.display = 'none';
          }
        }
      }
      var ownerContainerWidth = 0;
      var ownerTagPane = registry.byId(resource.id + "-OwnerTagPane");
      if (ownerTagPane) {
        var ownerContainerNode = dom.byId(resource.id + '-OwnerContainer');
        if (ownerTagPane.hasChildren()) {
          if (ownerContainerNode) {
            ownerContainerNode.style.display = 'inline-block';
          }
        } else {
          // No tags, hide the div
          if (ownerContainerNode) {
            ownerContainerNode.style.display = 'none';
          }
        }
        ownerContainerWidth = domGeom.getContentBox(ownerContainerNode).w;
      }
      var contactContainerWidth = 0;
      var contactTagPane = registry.byId(resource.id + "-ContactTagPane");
      if (contactTagPane) {
        var contactContainerNode = dom.byId(resource.id + '-ContactContainer');
        if (contactTagPane.hasChildren()) {
          if (contactContainerNode) {
            contactContainerNode.style.display = 'inline-block';
          }
          contactContainerWidth = domGeom.getContentBox(contactContainerNode).w;
          //Owner and Contacts fit on the same line
          if(width >= ownerContainerWidth + contactContainerWidth){
            compactTagPane(contactTagPane, width - ownerContainerWidth);
          }
          else{
            var contactTagPaneWidth = domGeom.getContentBox(contactTagPane.domNode).w;
            compactTagPaneDouble(contactTagPane, contactTagPaneWidth);
          }
        } else {
          // No tags, hide the div
          if (contactContainerNode) {
            contactContainerNode.style.display = 'none';
          }
        }
      }
    }
    //Hide the note icon if there is no note
    var notePane = dom.byId(resource.id + '-NoteContainer');
    if(notePane){
      var note = dom.byId(resource.id + '-ResourceNote');
      if(note.textContent.trim() === ''){
        notePane.style.display = 'none';
      }
      else{
        notePane.style.display = 'inline-block';
      }
    }
  }

  /**
   * If we have more than one rows of tags, display the first row and hide second row and onwards. Add an expand button at the end of
   * the first row.
   */
  function compactTagPane(tagPane, paneWidth) {
    if (paneWidth <= 0) {
      return;
    }
    var tagPaneWidth = paneWidth;
    var children = tagPane.getChildren();

    var tagWidth = 0;
    var expandTag = children[children.length-1].domNode;
    if (expandTag.clientWidth == 0 ) {
      children[children.length-1].domNode.style.display = 'inline-block';
    }
    var expandTagWidth = expandTag.clientWidth + 17; //17 is the border plus right margin of the expand tag
    var tagName = "";
    var tagType = "";
    var anyTagHidden = false;
    for (var i=0; i < children.length; i++) {
      tagWidth = children[i].domNode.clientWidth;
      tagName = children[i].tagName;
      tagType = children[i].tagType;
      if (tagType.indexOf('expand') == 0) {
        if (anyTagHidden) {
          children[i].domNode.style.display = 'inline-block';
        } else {
          children[i].domNode.style.display = 'none';
        }
      }
      if ((tagPaneWidth > tagWidth + 17 + expandTagWidth)
          || ((tagPaneWidth > tagWidth) && (i == children.length - 2) && !anyTagHidden)) {
        // save room for the expand tag except when it is the second last tag (last tag is the expand tag)
        tagPaneWidth = tagPaneWidth - tagWidth -17;
      } else {
        if (tagType.indexOf('expand') !== 0) {
          children[i].domNode.style.display = 'none';
          anyTagHidden = true;
        }
      }
    }
  }

  /**
   * If we have more than two rows of tags, display the first two rows and hide the rest. Add an expand button at the end of the first
   * row.
   */
  function compactTagPaneDouble(tagPane, paneWidth) {
    if (paneWidth <= 0) {
      return;
    }
    var tagPaneWidth = paneWidth;
    var firstRowFull = false;
    var anyTagHidden = false;

    var children = tagPane.getChildren();
    var tagWidth = 0;
    var expandTag = children[children.length-1].domNode;
    if (expandTag.clientWidth == 0 ) {
      children[children.length-1].domNode.style.display = 'inline-block';
    }
    var expandTagWidth = expandTag.clientWidth + 17; //17 is the border plus right margin of the expand tag
    var tagName = "";
    var tagType = "";
    for (var i = 0; i < children.length; i++) {
      //Set the node to display first if it was hidden so that clientWidth is non zero
      if(children[i].domNode.clientWidth === 0){
        children[i].domNode.style.display = 'inline-block';
      }
      tagWidth = children[i].domNode.clientWidth;
      tagName = children[i].tagName;
      tagType = children[i].tagType;

      // Check if tag is the ellipses tag
      if (tagType.indexOf('expand') == 0) {
        // Show the ellipses if any of the tags are hidden
        if (anyTagHidden) {
          children[i].domNode.style.display = 'inline-block';
        } else {
          // Hide ellipses when all tags are shown
          children[i].domNode.style.display = 'none';
        }
      }
      if ((!firstRowFull && (tagPaneWidth >= tagWidth + 17)) ||
          ( firstRowFull && (tagPaneWidth >= tagWidth + 17 + expandTagWidth)) ||
          ((tagPaneWidth > tagWidth) && (i == children.length - 2) && !anyTagHidden) ){
         tagPaneWidth = tagPaneWidth - tagWidth - 17;
      }

      else {
        if (tagType.indexOf('expand') !== 0) {
          // If the tag expands past the first row, then reset the width for the tags on the 2nd row minus the tag width and the spacing after it
          if (!firstRowFull) {
            firstRowFull = true;
            tagPaneWidth = paneWidth - tagWidth - 17;
          } else {
            children[i].domNode.style.display = 'none';
            anyTagHidden = true;
          }
        }
      }
    }
  }

  /**
   * Common method to create an AlertPane and bind in a single resource.
   */
  function populateAlertContent(resource, alertPaneContainer) {
    var alertPane = new AlertPane({
      id : ID.dashDelimit(ID.getObjectView(), resource.id, ID.getAlertPaneUpper())
    }, alertPaneContainer);
    alertPane.addResource(resource);
  }

});
