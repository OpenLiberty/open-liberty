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
 * Objects and logic to view a resource collection.
 *
 * This code responds to the type of resource collection being viewed.
 */
define([ 'dojo/_base/lang', 'dojo/_base/window', 'dojo/keys', 'dojo/on', 'dojo/has', 'dojo/store/Memory', 
         'dojo/dom', 'dojo/dom-class', 'dojo/_base/array', 'dojo/query', 'dijit/registry', 
         'dijit/layout/BorderContainer', 'dijit/layout/ContentPane', 'dijit/layout/StackContainer',
         'dojo/i18n!../nls/explorerMessages', 'dojo/i18n!jsExplore/resources/nls/resourcesMessages', 
         'js/common/platform', 'js/widgets/MessageDialog', 'jsExplore/views/viewFactory', 
         'jsExplore/views/shared/allX', 'jsExplore/resources/utils', 'jsExplore/widgets/ObjectViewCards',
         'jsExplore/widgets/ObjectViewHeaderPane', 'jsExplore/widgets/ResourcePane', 'jsExplore/widgets/SideTabPane',
         'jsExplore/utils/ID', 'jsExplore/utils/constants',
         'dojox/string/BidiComplex','dijit/focus', 'jsShared/utils/imgUtils' ], 
         function(lang, window, keys, on, has, Memory, dom, domClass, array, query, registry, 
             BorderContainer, ContentPane, StackContainer, i18n, i18nResources, platform, MessageDialog, 
             viewFactory, allX, utils, ObjectViewCards, ObjectViewHeaderPane, ResourcePane, SideTabPane,
             ID, constants, BidiComplex,focusUtil, imgUtils) {

  'use strict';

  function getNameFromTuple(tuple) {
    return tuple.substring(tuple.lastIndexOf(',')+1);
  }

  return {
    /**
     * Creates a view for the given resource.
     * 
     * The contents and behaviour of the view will be dictated by the resource type.
     */
    openObjectView : function(resource, defaultSideTab) {
      resource.id = resource.id.replace(/ /g,ID.getUnderscore());
      var viewId = ID.dashDelimit(ID.getObjectView(), resource.id);
      var breadcrumbWidget = registry.byId(ID.getBreadcrumbContainer());
      var view = registry.byId(viewId);
      var createResource = true;
      if (view) {
        if (!view.resource.isDestroyed) {
          breadcrumbWidget.selectChild(viewId);
          // Default is Overview button
          var sideTabbutton = registry.byId(resource.id + ID.getSideTabOverviewButtonUpper());
          if (defaultSideTab) {
            if (defaultSideTab.indexOf('/serverConfig') === 0) {
              sideTabbutton = registry.byId(resource.id + ID.getSideTabConfigButtonUpper());
            } else {
              sideTabbutton = registry.byId(resource.id + ID.getSideTabUpper() + defaultSideTab + ID.getButtonUpper());
            }
          }
          sideTabbutton.onClick(defaultSideTab);
          createResource = false;
        } else {
          view.destroyRecursive();
        }
      } 
      if (createResource) {
        var resourcePage = createResourcePage(viewId, resource, defaultSideTab);
        breadcrumbWidget.addChild(resourcePage);
      } else {
        var mainStackContainer = registry.byId(resource.id + "-StackContainer");
        // Reset any collection views and collapse overview details
        array.forEach(mainStackContainer.getChildren(), function(stackPane){
          if (stackPane.id.indexOf("-MainContentPane") > -1) {
            // if main pane, look for objectViewHeaderPane to collapse the details
            array.forEach(stackPane.getChildren(), function(childPane){
              if (childPane instanceof ObjectViewHeaderPane && childPane.toggleButton) {
                childPane.toggleButton.set('checked', false);
              }
            });
          } else if (stackPane.id.indexOf("Config-ContentPane") === -1 &&
              stackPane.id.indexOf("Stats-ContentPane") === -1) {
            // else look for filterBar as a child of the pane
            array.forEach(stackPane.getChildren(), function(childPane){
              if (childPane instanceof FilterBar) {
                // set the currentFilter to total
                childPane.set("currentFilter", "Total");
                // reset to make sure multiselect is closed
                if (childPane.clicked) {
                  // multiselect bar is open so close it
                  // Hack the domClass and clicked because I can't figure out how to get to the editButton.onClick() method
                  // this.filterBar.editButton.onClick();
                  domClass.remove(childPane.editButton.domNode, "editButtonsClicked");
                  childPane.clicked = !childPane.clicked;
                  childPane.processEditButton();
                }
              }
            });
          }
        });
      }

      focusUtil.focus(dom.byId(resource.id + ID.getSideTabOverviewButtonUpper()));
      var breadcrumbPane = registry.byId(ID.getBreadcrumbPane());
      /**
       * see if it makes sense to use this widget from where it's created above.
       */
      breadcrumbPane.setBreadcrumb(resource);

    }
  };

  /**
   * Creates the correct resource page for the given resource. This sets up the shared structure of a given resource page.
   */
  function createResourcePage(viewId, resource, defaultSideTab) {
    var resourcePage = new ContentPane({
      id : viewId,
      headerTitle : resource.name,
      title : resource.name,
      content : ' ',
      baseClass : 'topResourceContentPane',
      resource : resource
    });

    resource.subscribe(resourcePage);
    resourcePage.onDestroyed = function() {
      var resId = resourcePage.resource.id;

      //check for when a resourcePage is destroyed to display alert dialog if necessary
      if (resourcePage.resource.isDestroyed) {
        resource.unsubscribe(resourcePage);
        var breadcrumbWidget = registry.byId(ID.getBreadcrumbContainer());
        var selectedView = breadcrumbWidget.selectedChildWidget;

        // check if resource is current page (dashboard has no associated resource)
        if (selectedView.resource && (resId == selectedView.resource.id)){
          //create Error Dialog box and display
          var errorMessageDialog = new MessageDialog({
            title : i18nResources.ERROR,
            messageText : i18n.VIEWED_RESOURCE_REMOVED,
            messageDialogIcon : imgUtils.getSVGSmallName('status-alert-gray'),
            id: ID.underscoreDelimit(resId, ID.getRemovedResourceErrorUpper()),
            okButtonLabel: i18n.GO_TO_DASHBOARD,
            okFunction : function() {
              // Go to dashboard
              var breadcrumbWidget = registry.byId(ID.getBreadcrumbContainer());
              var mainDashboard = registry.byId(ID.getMainDashboard());
              breadcrumbWidget.selectChild(mainDashboard);
              resourcePage.destroyRecursive();
            }
          });
          errorMessageDialog.placeAt(document.body);
          errorMessageDialog.startup();
          errorMessageDialog.show();
        }
      }
    };

    var objectBorderContainer = new BorderContainer({
      id : ID.dashDelimit(resource.id, ID.getBorderContainerUpper()),
      design : (platform.isPhone() ? 'headline' : 'sidebar'),
      gutters : false,
      doLayout : false
    });
    resourcePage.addChild(objectBorderContainer);

    // Create the SideTabPane, it will be populated later based on
    // the resource type
    var sideTabPane = new SideTabPane([ resource, defaultSideTab ]);
    objectBorderContainer.addChild(sideTabPane);

    var mainStackContainer = new StackContainer({
      id : ID.dashDelimit(resource.id, ID.getStackContainerUpper()),
      region : 'center'
    });
    objectBorderContainer.addChild(mainStackContainer);

    var mainContentPane = new ContentPane({
      id : ID.dashDelimit(resource.id, ID.getMainContentPaneUpper()),
      label : resource.name,
      doLayout : false,
      style : 'width: 100%; height: 100%; overflow: auto;',
      baseClass : 'scrollableContentPane'
    });
    mainStackContainer.addChild(mainContentPane);

    // Add in the object view header pane
    var ovhp = new ObjectViewHeaderPane([ resource ]); // This MUST be an array
    mainContentPane.addChild(ovhp);

    var type = resource.type;
    switch (type) {
    case 'appOnCluster':
      populateApplicationSideTabPane(resource, sideTabPane);
      populateApplicationContentPane(resource, mainContentPane, sideTabPane);
      break;
    case 'appOnServer':
      populateAppOnServerSideTabPane(resource, sideTabPane);
      populateAppOnServerContentPane(resource, mainContentPane, sideTabPane);
      break;
    case 'cluster':
      populateClusterSideTabPane(resource, sideTabPane);
      populateClusterContentPane(resource, mainContentPane, sideTabPane);
      break;
    case 'host':
      populateHostSideTabPane(resource, sideTabPane);
      populateHostContentPane(resource, mainContentPane, sideTabPane);
      break;
    case 'server':
    case 'standaloneServer':
      populateServerSideTabPane(resource, sideTabPane);
      populateServerContentPane(resource, mainContentPane, sideTabPane); // Need to pass in sideTabPane here
      // for binding graph re-view
      break;
    case 'runtime':
      populateRuntimeSideTabPane(resource, sideTabPane);
      populateRuntimeContentPane(resource, mainContentPane, sideTabPane);
      break;
    default:
      console.error('objectView.createResourcePage called for an unknown resource type: ' + type);
    }

    return resourcePage;
  }

  /** Create application content * */
  function populateApplicationSideTabPane(application, sideTabPane) {
    var populateAppInstTabPane = function(pane, resourceName) {
      application.getInstances().then(function(appInstances) {
        viewFactory.openView(appInstances, null, pane);
      });
    };
    sideTabPane.addSideTabButton('Instances', populateAppInstTabPane);
  }

  function populateApplicationContentPane(application, mainContentPane) {
    populateAppInst(application, mainContentPane);
  }

  // build app resource section
  function populateAppInst(application, mainContentPane) {
    var appInstPane = ResourcePane.createResourcePane([ "AppInst", application.id, application, i18n.INSTANCES, "App", "bottomContentPane", "center" ]);
    mainContentPane.addChild(appInstPane);
    application.getInstances().then(function(appInstances) {
    });
  }

  /** BEGIN APP ON SERVER * */
  function populateAppOnServerSideTabPane(appOnServer, sideTabPane) {
    // Don't add monitor button if app is running on nodeJS server
    if (constants.RUNTIME_NODEJS !== appOnServer.server.runtimeType) {  
      sideTabPane.addSideTabButton('Stats');
    }
  }

  function populateAppOnServerContentPane(appOnServer, mainContentPane) {
    // Nothing to do! AppOnServer has no content yet
  }

  /** Create cluster content * */
  function populateClusterSideTabPane(cluster, sideTabPane) {
    cluster.getServers().then(function(servers) {
      var populateServerTabPane = function(pane, resourceName, resource, filter) {
        viewFactory.openView(servers, filter, pane);
      };
      sideTabPane.addSideTabButton('Servers', populateServerTabPane);
      // To ensure side tab buttons are added in the correct order (servers first), get apps after getting servers
      cluster.getApps().then(function(apps) {
        var populateAppTabPane = function(pane, resourceName, resource, filter) {
          viewFactory.openView(apps, filter, pane);
        };
        sideTabPane.addSideTabButton('Apps', populateAppTabPane);
      });
    });

  }

  function populateClusterContentPane(cluster, mainContentPane, sideTabPane) {
    populateClusterContent(cluster, mainContentPane, sideTabPane);
  }

  /**
   * Build the cluster resource section.
   * This is done in a nested way so that the content panes are always added in the right order.
   */
  function populateClusterContent(cluster, mainContentPane, sideTabPane) {
    cluster.getServers().then(function(servers) {
      var clusterServerPane = ResourcePane.createResourcePane([ "Cluster", cluster.name, servers, i18n.SERVERS, "Server", 
                                                                "middleContentPane", "top", sideTabPane ]);
      mainContentPane.addChild(clusterServerPane);

      cluster.getApps().then(function(appsOnCluster) {
        var clusterApplicationPane = ResourcePane.createResourcePane([ "Cluster", cluster.name, appsOnCluster, i18n.APPLICATIONS, "App", 
                                                                       "bottomContentPane", "center", sideTabPane ]);
        mainContentPane.addChild(clusterApplicationPane);
      });
    });
  }

  function populateHostSideTabPane(host, sideTabPane) {
    host.getServers().then(function(servers) {
      // always put the button on the sideTab even when there are none for dynamic additions
      //if (servers.list.length != 0) {
      var populateServerTabPane = function(pane, resourceName, resource, filter) {
        viewFactory.openView(servers, filter, pane);
      };
      sideTabPane.addSideTabButton('Servers', populateServerTabPane);
      //}
      // To ensure side tab buttons are added in the correct order (servers first), get runtimes after getting servers
      host.getRuntimes().then(function(runtimesOnHost) {
        // always put the button on the sideTab even when there are none for dynamic additions
        //if (runtimesOnHost.list.length != 0) {
        var populateRuntimesTabPane = function(pane, resoureName, resource, filter) {
          viewFactory.openView(runtimesOnHost, filter, pane);
        };
        sideTabPane.addSideTabButton('Runtimes', populateRuntimesTabPane);
        //}
      }, function(err) {
        // Error in Host.getRuntimes()
        console.error('Caught error while obtaining the Host\'s runtimes. Error: ', err);
      });
    });
  }

  function populateHostContentPane(host, mainContentPane, sideTabPane) {
    populateHostPane(host, mainContentPane, sideTabPane);
  }

  function populateHostPane(host, mainContentPane, sideTabPane) {
    var hostPane = new ContentPane({
      id : ID.dashDelimit(host.id, ID.getContainerPaneUpper()),
      baseClass : "bottomContentPane",
      region : "center"
    });
    mainContentPane.addChild(hostPane);
    host.getServers().then(function(serversOnHost) {
      var hostServerPane = ResourcePane.createResourcePane([ "Host", host.name, serversOnHost, i18n.SERVERS, "Server",
                                                             "bottomContentPane", "center", sideTabPane ]);
      hostPane.addChild(hostServerPane);
    });
  }

  /** BEGIN SERVER * */
  function populateServerSideTabPane(server, sideTabPane) {
    var populateAppTabPane = function(pane, resourceName, resource, filter) {
      server.getApps().then(function(appsOnServer) {
        viewFactory.openView(appsOnServer, filter, pane);
      });
    };
    sideTabPane.addSideTabButton('Apps', populateAppTabPane);
    sideTabPane.addSideTabButton('Stats');
    if (constants.RUNTIME_NODEJS !== server.runtimeType && constants.CONTAINER_DOCKER !== server.containerType){
      sideTabPane.addSideTabButton('Config');
    }
  }

  function populateServerContentPane(server, mainContentPane, sideTabPane) {
    if (constants.RUNTIME_NODEJS !== server.runtimeType) {
      // For Node.js servers, the server process handles a single application.
      // The Admin center presentation is one object that is both the server and the app.
      // Therefore, there will not be an Applications section for a Node.js server.
      populateServerApplications(server, mainContentPane, sideTabPane);
    } else {
      // Remove the bottom border of the OVHP for a Liberty server so the panel 
      // does not look like it is missing something.
      var ovhp = query(".objectViewHeaderPane", mainContentPane.domNode);
      domClass.add(ovhp[0], "objectViewHeaderPaneSolo");
    }
  }

  // build the AppOnServer resource pane
  function populateServerApplications(server, mainContentPane, sideTabPane) {
    server.getApps().then(function(appsOnServer) {
      var applicationPane = ResourcePane.createResourcePane([ "Server", server.id, appsOnServer, i18n.APPLICATIONS, "App", "bottomContentPane", "center", sideTabPane ]);
      mainContentPane.addChild(applicationPane);
      applicationPane.sideTabPane = sideTabPane; 
    });
  }

  /** BEGIN RUNTIME * */
  function populateRuntimeSideTabPane(runtime, sideTabPane) {
    runtime.getServers().then(function(servers) {
      if (servers.list.length != 0) {
        var populateServerTabPane = function(pane, resourceName, resource, filter) {
          viewFactory.openView(servers, filter, pane);
        };
        sideTabPane.addSideTabButton('Servers', populateServerTabPane);
      }
    });
  }

  function populateRuntimeContentPane(runtime, mainContentPane, sideTabPane) {
    populateRuntimePane(runtime, mainContentPane, sideTabPane);
  }

  function populateRuntimePane(runtime, mainContentPane, sideTabPane) {
    var runtimePane = new ContentPane({
      id : ID.dashDelimit(runtime.id, ID.getContainerPaneUpper()),
      baseClass : "bottomContentPane",
      region : "center"
    });
    mainContentPane.addChild(runtimePane);
    runtime.getServers().then(function(serversOnRuntime) {
      var runtimeServerPane = ResourcePane.createResourcePane([ "Runtime", runtime.id, serversOnRuntime, i18n.SERVERS, "Server", 
                                                                "bottomContentPane", "center", sideTabPane ]);
      runtimePane.addChild(runtimeServerPane);
    });
  }

});