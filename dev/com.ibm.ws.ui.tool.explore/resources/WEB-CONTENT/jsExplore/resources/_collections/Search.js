/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/**
 * The class doesn't represent any resource, thus it does not subscribe to any topics nor receive any events.
 */
define([ 'dojo/_base/declare', 'dojo/promise/all', 'dojo/Deferred', "dijit/registry", '../_util', "../viewToHash", "../hashUtils", 'dojo/io-query' ],
    function(declare, all, Deferred, registry, util, viewToHash, hashUtils, ioQuery) {
  
  return declare('Search', [], {
    populated: false,
    hashQueryParams: null,
    queryParams: null,
    

    /**
     * Populate the initial collection state.
     * 
     * @return {Deferred} The returned Deferred will be resolved when the update is complete.
     */
    populate: function(options) {
      this.apps = {};
      this.servers = {};
      this.clusters = {};
      this.hosts = {};
      this.runtimes = {};
      this.apps.list = new Array();
      this.servers.list = new Array();
      this.clusters.list = new Array();
      this.hosts.list = new Array();
      this.runtimes.list = new Array();
      var updateMe = function(me, resources) {
        var deferreds = [];
        if (resources.applications) {
          for (var i = 0; i < resources.applications.list.length; i++) {
            var res = resources.applications.list[i];
            var deferred = new Deferred();
            deferreds.push(deferred);
            me.resourceManager.getApplication(res.id).then(function(app) {
              me.apps.list.push(app);
              var def = deferreds.pop();
              if (def) {
                def.resolve();
              } else {
                console.warning(app, ' was pushed into Search.apps.list, but all the deferreds have already been resolved.');
              }
            }, function(err) {
              console.error('Search.populate application error: ', err);
            });
          }
        }
        
        if (resources.servers) {
          for (var i = 0; i < resources.servers.list.length; i++) {
            var res = resources.servers.list[i];
            var deferred = new Deferred();
            deferreds.push(deferred);
            me.resourceManager.getServer(res.id).then(function(server) {
                me.servers.list.push(server);
                var def = deferreds.pop();
                if (def) {
                  def.resolve();
                } else {
                  console.warning(server, ' was pushed into Search.servers.list, but all the deferreds have already been resolved.');
                }
            }, function(err) {
              console.error('Search.populate server error: ', err);
            });
          }
        }
        
        if (resources.hosts) {
          for (var i = 0; i < resources.hosts.list.length; i++) {
            var res = resources.hosts.list[i];
            var deferred = new Deferred();
            deferreds.push(deferred);
            me.resourceManager.getHost(res.id).then(function(host) {
              me.hosts.list.push(host);
              var def = deferreds.pop();
              if (def) {
                def.resolve();
              } else {
                console.warning(host, ' was pushed into Search.hosts.list, but all the deferreds have already been resolved.');
              }
            }, function(err) {
              console.error('Search.populate host error: ', err);
            });
          };
        }
        
        if (resources.clusters) {
          for (var i = 0; i < resources.clusters.list.length; i++) {
            var res = resources.clusters.list[i];
            var deferred = new Deferred();
            deferreds.push(deferred);
            me.resourceManager.getCluster(res.id).then(function(cluster) {
              me.clusters.list.push(cluster);
              var def = deferreds.pop();
              if (def) {
                def.resolve();
              } else {
                console.warning(cluster, ' was pushed into Search.clusters.list, but all the deferreds have already been resolved.');
              }
            }, function(err) {
              console.error('Search.populate cluster error: ', err);
            });
          };
        };
        
        if (resources.runtimes) {
          for (var i = 0; i < resources.runtimes.list.length; i++) {
            var res = resources.runtimes.list[i];
            var deferred = new Deferred();
            deferreds.push(deferred);
            me.resourceManager.getRuntime(res.id).then(function(runtime) {
              me.runtimes.list.push(runtime);
              var def = deferreds.pop();
              if (def) {
                def.resolve();
              } else {
                console.warning(runtime, ' was pushed into Search.runtimes.list, but all the deferreds have already been resolved.');
              }
            }, function(err) {
              console.error('Search.populate runtime error: ', err);
            });
          };
        };
        var promises = new all(deferreds);  
        return promises;
      };

      this.populated = false;
      this.queryParams = [];
      this.hashQueryParams = null;
      
      var currentHash = hashUtils.getCurrentHash();
      if (currentHash.indexOf(hashUtils.getToolId() + '/search/?') === 0) {
        this.hashQueryParams = ioQuery.queryToObject(currentHash.substring((hashUtils.getToolId() + '/search/?').length));
      }
      // To ease our checks, make everything (even single key-value pair) arrays; consider doing the same for the keys
      for (var property in this.hashQueryParams) {
        if (this.hashQueryParams.hasOwnProperty(property)) {
          if (typeof this.hashQueryParams[property] === 'string') {
            this.hashQueryParams[property] = [this.hashQueryParams[property]];
          }
        }
      }
      
      for (var option in options) {
        var value = options[option];
        this._addQueryParam(options[option], option);
      }
      
      var queryString = '';
      for (var i = 0; i < this.queryParams.length; i++) {
        if (i == 0) {
          queryString += '?';
          } else {
          queryString += '&';
          }
        queryString += this.queryParams[i];
          }
      if (this.hashQueryParams === null) {
        viewToHash.updateHash(viewToHash.getHash() + "/search/" + queryString);
      } else {
        viewToHash.updateHash(hashUtils.getCurrentHash());
      }
      return util.doPopulate('/ibm/api/collective/v1/search' + queryString, this, updateMe);
    },

    /**
     * @param value The search criteria (ex. All, Running, Unknown, Host, etc ...)
     * @param key String with the search key (type, state, name, tag, etc...)
     */
    _addQueryParam: function(value, key) {
      /* 
       * Only update the URL if it's functionally different (ignore order and incorrect keys).  
       * Set 'hashQueryParams = null' to indicate new URL and skip processing URL for the remainder of key-value pairs.
       * The flow is the same for each of the 4 supported keys: type, state, name, tag
       * If a key is a string (single item), add it to queryParams and check if the URL currently has only one of that key and the value matches
       * If a key is a non-empty array, add each value to queryParams and check if the URL currently has exactly the same set of key-value pairs
       * If a key is an empty array, check if the current URL doesn't have that key or if it does that it is an empty string
     */
      if (value) {
        if (typeof value === 'string') {
          this.queryParams.push("" + key + "=" + encodeURIComponent(value));
          if (this.hashQueryParams && this.hashQueryParams.hasOwnProperty(key) && this.hashQueryParams[key].length === 1 && this.hashQueryParams[key][0] === value){
            this.hashQueryParams[key] = [];
          } else {
            this.hashQueryParams = null;
          }
        } else if (value[0] !== '') {
          if (!(this.hashQueryParams && this.hashQueryParams.hasOwnProperty(key) && this.hashQueryParams[key].length === value.length)) {
            this.hashQueryParams = null;
          }
          for (var i = 0; i < value.length; i++) {
            this.queryParams.push("" + key + "=" + encodeURIComponent(value[i]));
            if (this.hashQueryParams) {
              if (typeof this.hashQueryParams[key] !== 'string') {
                for (var j = 0; j < this.hashQueryParams[key].length; j++) {
                  if (this.hashQueryParams[key][j] === value[i]) {
                    this.hashQueryParams[key].splice(j, 1);
                    break;
                  }
                }
              } else if (this.hashQueryParams[key] !== value[i]) {
                this.hashQueryParams = null;
              }
            }
          }
          if (this.hashQueryParams && this.hashQueryParams[key] && this.hashQueryParams[key].length !== 0) {
            this.hashQueryParams = null;
        }
        } else if (value[0] === '' && (this.hashQueryParams && this.hashQueryParams[key] && this.hashQueryParams[key][0] !== '')) {
          this.hashQueryParams = null;
          }
      }
    }
  });
});