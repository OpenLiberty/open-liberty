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
 * 
 * * USAGE: catalog = new Catalog(hostURL); // e.g
 * https://kboo1519.torolab.ibm.com:9443
 * 
 * Example) var catalog = new Catalog("https://kboo1519.torolab.ibm.com:9443");
 * var tools = catalog.getTools(); for (var i=0; i<tools.length; i++) { var
 * tool = tools[i]; var toolName = tool.name; }
 * 
 * @returns {Catalog} The Catalog object
 */

define(["dojo/_base/declare",
        "dojo/request/xhr",
        "dojo/Deferred",
        "dojo/json",
        "dojo/_base/lang",
        "dojo/i18n!./nls/catalogMessages",
        'js/common/tr'
    ],
    function(declare, xhr, Deferred, JSON, lang, i18n, tr) {
    'use strict';

    var Catalog = declare("Catalog", null, {
        __url: "/ibm/api/adminCenter/v1/catalog",

        /**
         * Function used to get all the Tools in the catalog available to the user
         *
         * @param Optional. The set of fields to filter on.
         * @returns {Array} Array of Tool type
         */
        getTools: function(fields) {
            var url = (fields) ? this.__url+"?fields="+fields : this.__url;
            var options = { handleAs: "json" };
            var xhrDef = xhr.get(url, options);

            // Establish the Deferred to be returned.
            // This allows the caller to cancel the underlying XHR request.
            var deferred = new Deferred(function cancelXHRDeferred(reason) {
                xhrDef.cancel(reason);
            });

            xhrDef.then(function(catalog) {
                var toolObjs = [];
                var i = 0;
                if ( catalog.featureTools) {
                    for (i = 0; i < catalog.featureTools.length; i++) {
                        toolObjs.push(new Catalog.FeatureTool(catalog.featureTools[i]));
                    }
                }
                if (catalog.bookmarks) {
                    for (i = 0; i < catalog.bookmarks.length; i++) {
                        toolObjs.push(new Catalog.Bookmark(catalog.bookmarks[i]));
                    }
                }
                deferred.resolve(toolObjs, true);
            }, function(err) {
                deferred.reject(err, true);
            }, function(evt) {
                deferred.progress(evt, true);
            });

            return deferred;
        },

        /**
         * Function used to get a specific Tool with fields to narrow 
         * @param {String} id - ID of the Tool that is being requested
         * @param {String} fields - The fields passed to the server to filter what to return
         * @returns {Tool} The requested Tool or null if doesn't exist
         */
        getTool: function(id) {
            if (!id) {
                tr.throwMsg(i18n.ERROR_GETTOOL_NOID);
            }
            var url = this.__url;
            var options = { handleAs: "json" };
            var xhrDef = xhr.get(url, options);

            // Establish the Deferred to be returned.
            // This allows the caller to cancel the underlying XHR request.
            var deferred = new Deferred(function cancelXHRDeferred(reason) {
                xhrDef.cancel(reason);
            });

            xhrDef.then(function(catalog) {
                var i;
                // First try to resolve against the bookmarks
                for(i = 0; i < catalog.bookmarks.length; i++) {
                    var bookmark = catalog.bookmarks[i];
                    if (bookmark.id === id) {
                        deferred.resolve(new Catalog.Bookmark(bookmark), true);
                        return;
                    }
                }
                
                // Next try to resolve against the feature tools
                for(i = 0; i < catalog.featureTools.length; i++) {
                    var featureTool = catalog.featureTools[i];
                    if (featureTool.id === id) {
                        deferred.resolve(new Catalog.FeatureTool(featureTool), true);
                        return;
                    }
                }
                
                // Lastly, reject
                deferred.reject(lang.replace(i18n.ERROR_GETTOOL_NOTOOL, [ id ]));
            }, function(err) {
                deferred.reject(err, true);
            }, function(evt) {
                deferred.progress(evt, true);
            });

            return deferred;
        },

        /**
         * Function to store a Tool on the server. The Tool must be
         * sent to the Liberty server via REST.
         * @param {Bookmark} bookmark - the Bookmark to be persisted in the server's catalog
         * @returns {}
         */
        addBookmark: function(bookmarkProps) {
            if (!bookmarkProps) {
              tr.throwMsg(i18n.ERROR_ADDBOOKMARK_NOPROPS);
            }
            var url = this.__url+"/bookmarks";
            var options = { handleAs: "json", headers: {"Content-type":"application/json"}, data: JSON.stringify(bookmarkProps) };
            var xhrDef = xhr.post(url, options);

            // Establish the Deferred to be returned.
            // This allows the caller to cancel the underlying XHR request.
            var deferred = new Deferred(function cancelXHRDeferred(reason) {
                xhrDef.cancel(reason);
            });

            xhrDef.then(function handleAppState(tool) {
                if (tool && tool.type && tool.type === "featureTool") {
                    deferred.resolve(new Catalog.FeatureTool(tool), true);
                } else {
                    deferred.resolve(new Catalog.Bookmark(tool), true);    
                }
            }, function(err) {
                deferred.reject(err, true);
            }, function(evt) {
                deferred.progress(evt, true);
            });

            return deferred;
        },

        /**
         * Function used to delete a specific bookmark on the server. The Tool must be
         * sent to the Liberty server via REST
         * @param {String} id - ID of the Tool that is being delete in the server's catalog
         * @returns {Response} The response 
         */
        deleteBookmark: function(id) {
            if (!id) {
              tr.throwMsg(i18n.ERROR_DELETETOOL_NOID);
            }
            var url = this.__url + "/bookmarks/" + id;
            var options = { handleAs: "json" };
            var xhrDef = xhr.del(url, options);

            // Establish the Deferred to be returned.
            // This allows the caller to cancel the underlying XHR request.
            var deferred = new Deferred(function cancelXHRDeferred(reason) {
                xhrDef.cancel(reason);
            });

            xhrDef.then(function(tool) {
                if (tool && tool.type && tool.type === "featureTool") {
                    deferred.resolve(new Catalog.FeatureTool(tool), true);
                } else {
                    deferred.resolve(new Catalog.Bookmark(tool), true);    
                }
            }, function(err) {
                deferred.reject(err, true);
            }, function(evt) {
                deferred.progress(evt, true);
            });

            return deferred;
        }
    });

    /**
     * Define the representation of a Catalog's Bookmark.
     * USAGE: var tool = new catalog.Tool(properties); // properties is a parsed result from
     * JSON
     * 
     * @returns {Tool} The Tool Object
     */
    Catalog.Bookmark = declare("Catalog.Bookmark", null, {
        id: null,
        type: null,
        name: null,
        url: null,
        icon: null,
        description: null,

        /**
         * Constructor.
         *
         * @param args Initialization object: {name:"appName"}
         */
        constructor: function(args){
            this.id = args.id;
            this.type = args.type;
            this.name = args.name;
            this.url = args.url;
            this.icon = args.icon;
            this.description = args.description;
        }
    });

    /**
     * Define the representation of a Catalog's Feature Tool.
     * USAGE: var tool = new catalog.FeatureTool(properties); // properties is a parsed result from
     * JSON
     * 
     * @returns {Tool} The Tool Object
     */
    Catalog.FeatureTool = declare("Catalog.FeatureTool", null, {
        id: null,
        type: null,
        featureName: null,
        featureVersion: null,
        featureShortName: null,
        name: null,
        url: null,
        icon: null,
        description: null,

        /**
         * Constructor.
         *
         * @param args Initialization object: {name:"appName"}
         */
        constructor: function(args){
            this.id = args.id;
            this.type = args.type;
            this.featureName = args.featureName;
            this.featureVersion = args.featureVersion;
            this.featureShortName = args.featureShortName;
            this.name = args.name;
            this.url = args.url;
            this.icon = args.icon;
            this.description = args.description;
        }
    });

    var catalogInstance = new Catalog();
    return {
        getCatalog: function() {
            return catalogInstance;
        }
    };
});
