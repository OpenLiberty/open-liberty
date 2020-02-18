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
 * /explore-1.0/servers/serverTuple - server 
 * /explore-1.0/clusters/clusterName - cluster
 * /explore-1.0/clusters/clusterName/apps/appName - clustered application 
 * /explore-1.0/servers/serverTuple/apps/appName - application instance
 * /explore-1.0/hosts/hostName - host</code>
 */
define([ 'dojo/Deferred', 'dijit/registry', 'jsExplore/resources/utils', 'jsExplore/utils/ID', 'dojo/hash', 'jsExplore/resources/hashUtils', 'js/common/platform', 'js/widgets/YesNoDialog',
         'dojo/_base/lang', 'dojo/i18n!jsExplore/nls/explorerMessages', 'dojo/i18n!jsExplore/resources/nls/resourcesMessages', 'dojo/dom-construct', 'jsShared/utils/imgUtils' ],
    function(Deferred, registry, utils, ID, hash, hashUtils, platform, YesNoDialog, lang, i18n, i18nResources, domConstruct, imgUtils) {

  /**
   * Builds the topic string.
   * 
   * @param topicStr
   *          The topic string from which the hash is generated
   * @return The built hash string
   */
  function __buildHash(topicStr) {
    // IF we want to encode the '/' and ',' of the server tuple, we would want to use encodeURIComponent() instead
    var t = encodeURI(hashUtils.getToolId() + '/' + topicStr);
    console.log('Constructed resource hash [' + t + ']');
    return t;
  }

  /**
   * Obtain the hash string for the type and arguments.
   * 
   * @param {String}
   *          type - The Object/Collection for which to obtain the hash. Ex: server, servers, serversOnHost
   * @return {String} hash - The hash for the resource. Null if incorrect type.
   */
  function __getHash(type, x, y, z) {
    switch (type) {
    case 'standaloneServer':
      return __buildHash(x);
    case 'servers':
      return __buildHash('servers');
    case 'serversOnHost':
      return __buildHash('hosts/' + x + '/servers');
    case 'serversOnCluster':
      return __buildHash('clusters/' + x + '/servers');
    case 'serversOnRuntime':
      return __buildHash('runtimes/' + x + '/servers');
    case 'server':
      var tuple = x;
      if (x && y && z) {
        // All 3 args, likely a broken up tuple
        var host = x;
        var userdir = y;
        var server = z;
        tuple = host + ',' + userdir + ',' + server;
      }
      return __buildHash('servers/' + tuple);
    case 'clusters':
      return __buildHash('clusters');
    case 'cluster':
      return __buildHash('clusters/' + x);
    case 'hosts':
      return __buildHash('hosts');
    case 'host':
      return __buildHash('hosts/' + x);
    case 'runtimesOnHost':
      return __buildHash('hosts/' + x + '/runtimes');
    case 'runtimes':
      return __buildHash('runtimes');
    case 'runtime':
      return __buildHash('runtimes/' + x);
    case 'applications':
      return __buildHash('applications');
    case 'appsOnServer':
      return __buildHash('servers/' + x + '/apps');
    case 'appOnServer':
      return __buildHash('servers/' + x + '/apps/' + y);
    case 'appsOnCluster':
      return __buildHash('clusters/' + x + '/apps');
    case 'appOnCluster':
      return __buildHash('clusters/' + x + '/apps/' + y);
    case 'appInstancesByCluster':
      return __buildHash('clusters/' + x + '/apps/' + y + '/instances');
    default:
      return null;
    }
  }

  return {
    /**
     * Returns the URL for a given resource.
     * 
     * @param resource
     *          The resource for which a URL will be computed
     * @return The hash portion of the URL for the given resource, tool ID is included. If the resource is not valid, null will be
     *         returned. If the resource is null, hashUtils.getToolId() will be returned (Dashboard)
     * @memberOf resources.viewToHash
     * @function
     * @public
     */
    getHash : function(resource) {
      if (resource == null) {
        return hashUtils.getToolId();
      } else if (utils.isStandalone()) {
        if (resource.type === 'appsOnServer') {
          return __getHash('standaloneServer', 'apps');
        } else if (resource.type === 'appOnServer') {
          return __getHash('standaloneServer', 'apps/' +  resource.name);
        } else {
          return hashUtils.getToolId();
        }
      } else {
        if (resource.type === 'appOnServer' || resource.type === 'appsOnServer') {
          return __getHash(resource.type, resource.server.id, resource.name);
        } else if (resource.type === 'appOnCluster' || resource.type === 'appsOnCluster' || resource.type === 'serversOnCluster') {
          return __getHash(resource.type, resource.cluster.id, resource.name);
        } else if (resource.type === 'appInstancesByCluster') {
          return __getHash(resource.type, resource.cluster.id, resource.application.name);
        } else if (resource.type === 'serversOnHost' || resource.type === 'runtimesOnHost') {
          return __getHash(resource.type, resource.host.id);
        } else if (resource.type === 'serversOnRuntime') {
          return __getHash(resource.type, resource.runtime.id);
        } else if (resource.type === 'runtime') {
          return __getHash(resource.type, resource.id);
        } else {
          return __getHash(resource.type, resource.id);
        }
      }
      
    },

    /**
     * Updates the hash portion of the URL. Note that we cannot rely on hash() to make the update because, when in an iframe (which is
     * how tools are opened through toolbox), it will only update the iframe's hash and not the url.
     * 
     * @param resource
     *          The resource for which a URL will be updated. If null, then update with the dashboard url (hashUtils.getToolId())
     * @memberOf resources.viewToHash
     * @function
     * @public
     */
    updateHash : function(resource, defaultSideTab) {
      //Before anything, check if we're navigating away from config view with a dirty/altered config
      var configIframe = document.getElementById(ID.getExploreContainerForConfigTool());
      if (configIframe) {
        var configWin = configIframe.contentWindow;
        if (configWin && configWin.editor && configWin.editor.isDocumentDirty && configWin.editor.isDocumentDirty() && !document.getElementById(ID.getDirtyConfigDialog())) {
          var alertIcon = imgUtils.getSVGSmallName('status-alert-gray');
          
          // We could get the filename from the hash, but this is how serverConfig obtains it.  We should obtain the name 
          // in the same manner as serverConfig, but we should do it through an 'API' similar to isDocumentDirty()
          var filename = configWin.$("#navigationBarTitle").text();
          var widgetId = ID.getDirtyConfigDialog();
          utils.destroyWidgetIfExists(widgetId);
          var dirtyConfigDialog = new YesNoDialog({
            id : widgetId,
            title : i18n.CLOSE,
            message : '',
            description : lang.replace(i18n.SAVE_BEFORE_CLOSING_DIALOG_MESSAGE, [filename]),
            descriptionIcon : alertIcon,
            destructiveAction : "no",
            yesButtonText: i18n.SAVE,
            noButtonText: i18n.DONT_SAVE,
            yesFunction : function() {
              configWin.editor.save();
            },
            noFunction : function() {
              // Destroy the view so we don't get prompted about dirty config again
              domConstruct.destroy(configIframe);
            }
          });
          dirtyConfigDialog.placeAt(document.body);
          dirtyConfigDialog.startup();
          dirtyConfigDialog.show();
          }
        }
      var newHash;
      if (typeof resource === "string"){
        newHash = resource;
      } else {
        newHash = this.getHash(resource);
      }

      if (defaultSideTab === "Instances") {
        newHash = newHash + '/instances';
      } else if (defaultSideTab === "Stats") {
        newHash = newHash + '/monitor';
      } else if (defaultSideTab === "Config") {
        newHash = newHash + "/serverConfig";
      } else if (defaultSideTab && defaultSideTab.indexOf("/serverConfig") === 0) {
        // We need to encode both the liberty environment variable as well as the file name, but not the surrounding %{}
        if (defaultSideTab.indexOf("/serverConfig/${") === 0) {
          if (defaultSideTab.lastIndexOf("}/") != -1) {
            var envVar = (defaultSideTab.substring(("/serverConfig/${").length, defaultSideTab.lastIndexOf("}/")));
            envVar = encodeURI(envVar);
            if ((defaultSideTab.lastIndexOf("}/") + 3) < defaultSideTab.length){
              var configFile = defaultSideTab.substring(defaultSideTab.lastIndexOf("}/") + 2);
              configFile = encodeURI(configFile);
              newHash = newHash + "/serverConfig/${" + envVar + "}/" + configFile;
            } else {
              console.warn("Unexpected: No configFile seems to follow the config env var.");
              newHash = newHash + "/serverConfig/${" + envVar + "}/";
            }
          } else if (defaultSideTab.lastIndexOf("}") != -1) {
            console.warn("Unexpected: No forward slash '/' found after the closing bracket '}' of '/serverConfig/${'");
            var envVar = (defaultSideTab.substring(("/serverConfig/${").length, defaultSideTab.lastIndexOf("}")));
            envVar = encodeURI(envVar);
            newHash = newHash + "/serverConfig/${" + envVar + "}";
          } else {
            console.warn("Unexpected: No closing bracket '}' could be found after '/serverConfig/${'");
            newHash = newHash + defaultSideTab;
          }
        } else {
          newHash = newHash + defaultSideTab;
        }
      } else if (defaultSideTab === "Apps") {
        newHash = newHash + '/apps';
      }

      if (newHash !== hashUtils.getCurrentHash()) {
        window.top.location.hash = "#" + newHash;
      }
      this.lastUpdateHash = newHash;
    },

    /**
     * This variable is used to store the result of the last updateHash call. We do this in order to avoid calling updateHash() from the
     * hashChange listener when we just updated the hash. This is because hashchange listener also catches hash() calls. This is
     * primarily for a performance improvement.
     * 
     * @returns {String} lashUpdatedHash
     * @memberOf resources.viewToHash
     * @function
     * @public
     */
    lastUpdateHash : hashUtils.getToolId()
  };

});