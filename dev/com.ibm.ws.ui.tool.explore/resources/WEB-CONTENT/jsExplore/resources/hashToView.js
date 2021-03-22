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
 * Collections and their hash: <code> 
 * /explore-1.0/servers - servers 
 * /explore-1.0/clusters - clusters 
 * /explore-1.0/applications - applications
 * /explore-1.0/hosts - hosts
 * 
 * Objects and their hash: 
 * /explore-1.0/#servers/serverTuple - server 
 * /explore-1.0/#clusters/clusterName - cluster
 * /explore-1.0/#applications/appName - application 
 * /explore-1.0/#servers/serverTuple/apps/appName - application instance
 * /explore-1.0/#hosts/hostName - host
 * /explore-1.0/#runtimes/runtime - runtime
 * 
 * SubViews (sub-collections) and their hash:
 * /explore-1.0/#servers/serverTuple/apps - apps 
 * /explore-1.0/#clusters/clusterName/servers - servers
 * /explore-1.0/#clusters/clusterName/apps - apps
 * /explore-1.0/#applications/appName/instances - instances 
 * /explore-1.0/#hosts/hostName/servers - servers
 * /explore-1.0/#hosts/hostName/runtimes - runtimes
 * /explore-1.0/#runtimes/runtime/servers - servers
 * </code>
 */
define([ 'dojo/Deferred', 'dijit/registry', './resourceManager', 'jsExplore/resources/utils', 'dojo/hash', 'jsExplore/resources/hashUtils',
         'jsExplore/resources/viewToHash', 'jsExplore/views/viewFactory', 'js/common/platform', 'js/widgets/MessageDialog',
         'dojo/_base/lang', 'dojo/i18n!jsExplore/nls/explorerMessages', 'dojo/i18n!jsExplore/resources/nls/resourcesMessages', 'js/common/tr', 
         'dojo/dom-construct', 'jsExplore/utils/serverConfig', 'jsShared/utils/imgUtils', 'jsExplore/utils/ID' ],
         function(Deferred, registry, resourceManager, utils, hash, hashUtils, viewToHash, viewFactory, platform, MessageDialog, lang, i18n, 
             i18nResources, tr, domConstruct, serverConfig, imgUtils, ID) {

  function startsWith(string, prefix) {
    return ((typeof string === 'string') && (string.indexOf(prefix) === 0));
  }

  var hashParser = {
      /**
       * Initialize the hash parser with the initial value.
       */
      init : function(hash) {
        this.remaining = hash;
        this.runtimeSegment = null;
      },

      /**
       * Gets the next segment of the slash delimited string.
       * 
       * @param string
       *          The string to parse.
       * @return The next segment of the slash delimited string. Returns null if no slash, empty string if nothing after the slash, and
       *         non-empty string if there is anything after the slash (including subsequent slashes)
       */
      getNextSegment : function() {
        var current = this.remaining;
        if (current === null) {
          return null;
        }

        if (this.runtimeSegment) {
          // If this flag was set to true previously, then assume that everything that
          // follows the "runtimes/" string is the ID of the Runtime resource
          // Strip off trailing forward slash if present
          // subView and subResource segments are analyzed for later
          var rtSegment;
          var lastSlash = this.runtimeSegment.lastIndexOf('/');
          if (lastSlash > -1 && (lastSlash + 1 === this.runtimeSegment.length)) { 
            rtSegment = this.runtimeSegment.substring(0, this.runtimeSegment.length - 1);
          } else {
            rtSegment = this.runtimeSegment;
          }
          // Reset this property so next time this function called, flow doesn't go in here!
          this.runtimeSegment = null;
          this.remaining = this.remaining.substring(rtSegment.length);
          return rtSegment;
        } else {
          if (current.indexOf('runtimes/') === 0) {
            // Set this so that everything following it is considered a runtime (since it can contain forward slashes)
            this.runtimeSegment = current.substring(9, current.length);
          }

          var slashIndex = current.indexOf('/');
          if (slashIndex === -1) {
            // No more slashes, return null
            this.remaining = null;
            return current;
          } else {
            // If this is search, return entire hash
            if (current.indexOf('search/') === 0) {
              this.remaining = null;
              return current;
            }
            // Return everything up to the first slash
            var ret = current.substring(0, slashIndex);
            var firstComma = ret.indexOf(',');
            if (firstComma >= 0) {
              // This is the server tuple condition, we need to revise our substring logic
              var lastComma = current.lastIndexOf(',');
              slashIndex = current.indexOf('/', lastComma);
              if (slashIndex === -1) {
                // If we have no trailing slash, go to end of string
                slashIndex = current.length;
              }
              ret = current.substring(0, slashIndex);
            }

            // Save everything after the first slash (could be an empty string)
            this.remaining = current.substring(slashIndex + 1);
            return ret;
          }
        }
      }
  };

  /**
   * Returns the value wrapped in a deferred.
   */
  function returnAsDeferred(value) {
    var deferred = new Deferred();
    deferred.resolve(value, true);
    return deferred;
  }

  /**
   * Maps the URL hash components to the appropriate resource for /standaloneServer/...
   */
  function mapHashToStandaloneServer(collection, resource) {
    if (collection === 'apps') {
      if (resource === null) {
        return returnAsDeferred(collection);
      } else {
        return resource;
      }
    } else if (collection === 'monitor') {
      if (resource === null) {
        return returnAsDeferred(collection);
    } else {
        return returnAsDeferred(null);
      }
    } else if (collection === 'serverConfig') {
      if (resource === null) {
        return returnAsDeferred(collection);
    } else {
        return returnAsDeferred(null);
      }
    }
  }

  /**
   * Maps the URL hash components to the appropriate resource for /hosts/...
   */
  function mapHashToHosts(resource, subView, subResource, instanceView) {
    if (!resource) { // url: /hosts
      return resourceManager.getHosts();
    } else {
      if (!subView) { // url: /hosts/name
        return resourceManager.getHost(resource);
      } else {
        if (subView === 'apps') { // url: /hosts/name/apps
          return returnAsDeferred(null);
        } else if (subView === 'servers') { // url: /hosts/name/servers
          if (!subResource) { 
            return resourceManager.getServersOnHost(resource);
          } else {
            // We do not support hosts/myHost/servers/myServer right now
            return returnAsDeferred(null);
          }
        } else if (subView === 'runtimes') { // url: /hosts/name/runtimes
          if (!subResource) {
            return resourceManager.getRuntimesOnHost(resource);
          } else {
            // We do not support hosts/{host}/runtimes/{rt_id}
            return returnAsDeferred(null);                    
          }
        } else if (subView === 'monitor') { // url: // hosts/name/monitor
          return resourceManager.getHost(resource);
        } else {
          // Not 'apps', 'servers', or 'runtimes'
          return returnAsDeferred(null);
        }
      }
    }
  }

  /**
   * Maps the URL hash components to the appropriate resource for /runtimes/...
   */
  function mapHashToRuntimes(resource, subView, subResource, instanceView) {
    if (!resource) { // url: /runtimes
      return resourceManager.getRuntimes();
    } else {
      // A rather ugly hack, but it works for now
      var endsWith = function(str, suffix) {
        return str.indexOf(suffix, str.length - suffix.length) !== -1;
      };
      if (endsWith(resource, '/servers')) {
        resource = resource.substring(0, resource.indexOf('/servers'));
        subView = 'servers';
      }
      if (!subView) { // url: /runtimes/id
        return resourceManager.getRuntime(resource);
      } else {
        if (subView === 'servers') {
          if (!subResource) { // url: /runtimes/id/servers
            return resourceManager.getServersOnRuntime(resource);  
          } else {
            // Runtimes does not support 'subResources'
            return returnAsDeferred(null);    
          }
        } else {
          // Runtimes only supports 'servers' as a subView
          return returnAsDeferred(null);  
        }
      }
    }
  }

  /**
   * Maps the URL hash components to the appropriate resource for /servers/...
   */
  function mapHashToServers(resource, subView, subResource, instanceView) {
    if (!resource) { // url: /servers
      return resourceManager.getServers();
    } else {
      if (!subView) { // url: /servers/tuple
        return resourceManager.getServer(resource);
      } else {
        if (subView === 'apps') {
          if (!subResource) { // url: // servers/tuple/apps
            return resourceManager.getAppsOnServer(resource);
          } else { // url: // servers/tuple/apps/name
            return resourceManager.getAppOnServer(resource, subResource);
          }
        } else if (subView === 'monitor') { // url: // servers/tuple/monitor
          return resourceManager.getServer(resource);
        } else if (subView === 'serverConfig') { // url: // servers/tuple/serverConfig
          return resourceManager.getServer(resource);
        } else {
          // Not 'apps' or 'monitor' is unsupported
          return returnAsDeferred(null);
        }
      }
    }
  }

  /**
   * Maps the URL hash components to the appropriate resource for /clusters/...
   */
  function mapHashToClusters(resource, subView, subResource, instanceView) {
    if (!resource) { // url: /clusters
      return resourceManager.getClusters();
    } else {
      if (!subView) { // url: /clusters/name
        return resourceManager.getCluster(resource);
      } else {
        if (subView === 'apps') { // url: /clusters/name/apps
          if (!subResource) {
            return resourceManager.getAppsOnCluster(resource);
          } else {
            if (!instanceView) { // url: /clusters/name/apps/name
              return resourceManager.getApplication(resource + ',' + subResource);
            } else {
              if (instanceView === 'instances') { // url: /clusters/name/apps/name/instances
                return resourceManager.getAppInstancesByCluster(resource, subResource);
              } else {
                // We do not support anything other than instances right now
                return returnAsDeferred(null);
              }
            }
          }
        } else if (subView === 'servers') { // url: /clusters/name/servers
          if (!subResource) {
            return resourceManager.getServersOnCluster(resource);
          } else {
            // We do not support clusters/myCluster/servers/myServer right now
            return returnAsDeferred(null);
          }
        } else if (subView === 'monitor') { // url: // clusters/name/monitor
          return resourceManager.getCluster(resource);
        } else {
          // Neither 'apps' nor 'servers'
          return returnAsDeferred(null);
        }
      }
    }
  }

  /**
   * Maps the URL hash component for /applications/...
   */
  function mapHashToApplications(resource, subView, subResource, instanceView) {
    if (!resource) { // url: /applications
      return resourceManager.getApplications();
    } else {
      return returnAsDeferred(null);
    }
  }

  /**
   * Determines the resource type from the hash value. This is rather long and ugly, but it should do the trick...
   * 
   * @param hashParser
   *          hashParser which we'll use to continue to parse the hash into segments
   * @return Null if there is no collection segment, or a a Deferred if a collection segment is specified. The Deferred may resolve with a
   *         resource if such a resource exists, otherwise it will resolve with null if the resource could not be found.
   */
  function __findResource(hashParser) {
    var collection = hashParser.getNextSegment();
    var resource = hashParser.getNextSegment();
    var subView = hashParser.getNextSegment();
    var subResource = hashParser.getNextSegment();
    var instanceView = hashParser.getNextSegment();
    var shouldNotHave = hashParser.getNextSegment();

    if (!collection) {
      // No collection defined, just go to the dashboard (return null)
      // This is intentionally NOT a Deferred
      return null;
    }
    
    if (utils.isStandalone()) {
      return mapHashToStandaloneServer(collection, resource);
    } else if (!(collection === 'servers' && subView === 'serverConfig') && 
        !(collection === 'standaloneServer' && subView === 'serverConfig') &&
        (shouldNotHave || (instanceView && collection !== 'clusters' && subView !== 'apps'))) {
      // We have a path to something we just won't ever match to because its too long,
      // so return a Deferred resolved with null. (This is a 'no such resource' case).
      // ServerConfig is an exception since it can have indefinite '/' in its hash
      return returnAsDeferred(null);
    } else if (collection.indexOf('search') === 0) {
      return returnAsDeferred(collection);
    } else if (collection === 'applications') {
      return mapHashToApplications(resource, subView, subResource, instanceView);
    } else if (collection === 'clusters') {
      return mapHashToClusters(resource, subView, subResource, instanceView);
    } else if (collection === 'hosts') {
      return mapHashToHosts(resource, subView, subResource, instanceView);
    } else if (collection === 'servers') {
      return mapHashToServers(resource, subView, subResource, instanceView);
    } else if (collection === 'runtimes') {
      return mapHashToRuntimes(resource, subView, subResource, instanceView);
    }
    // No such resource
    return returnAsDeferred(null);
  }


  return {

    /**
     * Returns the resource for the given URL.
     * 
     * @param {String}
     *          hash - The hash portion of the URL to map to a resource, the tool ID should be included
     * @return {Promise} which will be resolved with the resource for the given URL, or a Promise which will be resolved with null if the
     *         resource is unknown. If the hash represents the dashboard (its just the tool ID), then null is returned (note the null is not
     *         wrapped in a Promise). If the hash represents the empty value, then an error is thrown (should never happen). If the hash
     *         represents another tool, then an error is thrown (should never happen).
     * @memberOf resources.hashToView
     */
    getResource : function(hash) {
      hash = decodeURI(hash);
      hashParser.init(hash);
      // Grab what should be the toolId segment of the hash
      var hashId = hashParser.getNextSegment();
      if (hashId !== hashUtils.getToolId()) {
        tr.throwMsg('The hash did not start with the tool ID ' + hashUtils.getToolId() + ', but instead ' + hashId);
      } else {
        return __findResource(hashParser);
      }
    },

    /**
     * Updates the view to the passed in hash
     * 
     * @param hash -
     *          The hash portion of the URL which determines which view to go to.
     * @memberOf resources.hashToView
     */
    updateView : function(hash) {
        var resource = this.getResource(hash);
        console.log('Resource from hash: ', resource);
        // Check if we're navigating away from config view with a dirty config and if we are then wait
        serverConfig.waitIfDirtyEditor().then(function() {
          var displayErrorDialog = function() {
          // Throw an error message for invalid URL
          var alertIcon = imgUtils.getSVGSmallName('status-alert-gray');
          
          var errorMessageDialog = new MessageDialog({
            title : i18nResources.ERROR,
            messageText : i18n.INVALID_URL,
            messageDialogIcon : alertIcon,
            okButtonLabel: i18n.GO_TO_DASHBOARD,
            okFunction : function() {
              if (utils.isStandalone()){
                resourceManager.getStandaloneServer().then(function(standaloneServer) {
                  viewFactory.openView(standaloneServer);
                });
              } else {
                viewToHash.updateHash();
                var breadcrumbWidget = registry.byId(ID.getBreadcrumbContainer());
                var mainDashboard = registry.byId(ID.getMainDashboard());
                breadcrumbWidget.selectChild(mainDashboard);
              }
            }
          });
          errorMessageDialog.placeAt(document.body);
          errorMessageDialog.startup();
          errorMessageDialog.show();
        };
  
        if (resource === null) {
          // Go to dashboard
          console.debug('Null resource from hash, so go to dashboard');
          if (utils.isStandalone()){
            // No dashboard for standalone server.   Go to server page.
            resourceManager.getStandaloneServer().then(function(standaloneServer) {
              viewFactory.openView(standaloneServer);
            });
          } else {
            var breadcrumbWidget = registry.byId(ID.getBreadcrumbContainer());
            var mainDashboard = registry.byId(ID.getMainDashboard());
            breadcrumbWidget.selectChild(mainDashboard);
          }
        } else if (utils.isStandalone()) {
          // Select the necessary side tab in standalone server view of explore
          resourceManager.getStandaloneServer().then(function(standaloneServer) {
            console.log(standaloneServer);
            if (hash === hashUtils.getToolId()) {       // url: /#explore
              viewFactory.openView(standaloneServer);
            } else if (hash === hashUtils.getToolId() + '/apps' || hash === hashUtils.getToolId() + '/apps/') {
                viewFactory.openView(standaloneServer, 'Apps');
            } else if (hash.indexOf(hashUtils.getToolId() + '/apps/') !== -1 ) {   // url: /#explore/apps/appName   
                // resource is the appName at this point
                if (hash.endsWith("/monitor") || hash.endsWith("/monitor/")) {
                  // display the stats of the appOnServer for the standalone server
                  resourceManager.getAppOnServer(standaloneServer, resource).then(function(appOnServerObj) {
                    viewFactory.openView(appOnServerObj, 'Stats');
                  });                 
                } else {  
                  // display the object view of the appOnServer for the standalone server
                  resourceManager.getAppOnServer(standaloneServer, resource).then(function(appOnServerObj) {
                    viewFactory.openView(appOnServerObj);
                  });
               }
            } else if (hash === hashUtils.getToolId() + '/monitor' || hash === hashUtils.getToolId() + '/monitor/') {
                viewFactory.openView(standaloneServer, 'Stats');
            } else if (hash.indexOf(hashUtils.getToolId() + '/serverConfig') === 0) {
              var configHash = hash.substring((hashUtils.getToolId()).length);
              viewFactory.openView(standaloneServer, configHash);
            } else {
              displayErrorDialog();
            }
          });
        } else {
          // Go to the resource based on the URL
          resource.then(function(view) {
            if (view !== null) {
              console.debug('View from hash: ', view);
              if ((view.type === 'server' || view.type === 'appOnServer' || view.type === 'cluster' || view.type === 'host') && (hash.indexOf('/monitor') === hash.length - 8)) {
                viewFactory.openView(view, 'Stats');
              } else if ((view.type === 'server') && (hash.indexOf(hashUtils.getToolId() + '/servers/' + view.id + '/serverConfig') === 0)) {
                var configHash = hash.substring((hashUtils.getToolId() + '/servers/' + view.id).length);
                viewFactory.openView(view, configHash);
              } else if (view.type === 'appInstancesByCluster') {
                resourceManager.getAppOnCluster(view.cluster, view.application.name).then(function(appOnCluster){
                  viewFactory.openView(appOnCluster, 'Instances');
                });
              } else if (!view.type && (view.toLowerCase() === 'search' || view.toLowerCase() === 'search/')) {
                viewFactory.openSearchView();
              } else if (!view.type && startsWith(view, 'search/?')) {
                viewFactory.openSearchView(view.substring(8));
              }
              else {
                console.debug('it is not a resource with special handling required, opening view as is');
                viewFactory.openView(view);
              }
            } else {
              displayErrorDialog();
            }
          }, function(err) {
            console.debug('Error obtaining resource; display error page and go to dashboard', err);
            displayErrorDialog();
          });
        }
      });
    }
  };

});