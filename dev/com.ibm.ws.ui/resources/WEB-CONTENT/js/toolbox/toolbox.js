/**
 * 
 * USAGE: toolbox = new Toolbox(hostURL); // e.g https://kboo1519.torolab.ibm.com:9443
 * 
 * Example var toolbox = new Toolbox("https://kboo1519.torolab.ibm.com:9443"); var tools = toolbox.getTools(); for (var i=0; i<tools.length;
 * i++) { var tool = tools[i]; var toolName = tool.name; }
 * 
 * @module toolbox/toolbox
 * @returns {Toolbox} The Toolbox object
 */

define(["dojo/_base/declare", "dojo/request/xhr", "dojo/Deferred", "dojo/json", 'js/common/tr' ], function(declare, xhr, Deferred, JSON, tr) {
  'use strict';

  /**
   * @class Toolbox
   */
  var Toolbox = declare("Toolbox", null,
  /**
   * @lends Toolbox.ToolEntrys.prototype
   */
  {
    __url : "/ibm/api/adminCenter/v1/toolbox",

    /**
     * Function used to get all the Tools in the toolbox available to the user
     * 
     * @param Optional.
     *          The set of fields to filter on.
     * @returns {Array} Array of Tool type
     */
    getToolEntries : function() {
      var url = this.__url + "/toolEntries";
      var options = {
        handleAs : "json"
      };
      var xhrDef = xhr.get(url, options);

      // Establish the Deferred to be returned.
      // This allows the caller to cancel the underlying XHR request.
      var deferred = new Deferred(function cancelXHRDeferred(reason) {
        xhrDef.cancel(reason);
      });

      xhrDef.then(function(toolEntries) {
        var toolObjs = [];
        for ( var i = 0; i < toolEntries.length; i++) {
          toolObjs.push(new Toolbox.ToolEntry(toolEntries[i]));
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
     * 
     * @param {String}
     *          id - ID of the Tool that is being requested
     * @param {String}
     *          fields - The fields passed to the server to filter what to return
     * @returns {Tool} The requested Tool or null if doesn't exist
     */
    getTool : function(idOrEntry, fields) {
      if (!idOrEntry) {
        tr.throwMsg("Requested a tool but did not provide an ID!");
      }
      var id = idOrEntry;
      if (idOrEntry instanceof Toolbox.ToolEntry) {
        id = idOrEntry.id;
      }
      var url = this.__url + "/toolEntries/" + id + ((fields) ? "?fields=" + fields : "");
      var options = {
        handleAs : "json"
      };
      var xhrDef = xhr.get(url, options);

      // Establish the Deferred to be returned.
      // This allows the caller to cancel the underlying XHR request.
      var deferred = new Deferred(function cancelXHRDeferred(reason) {
        xhrDef.cancel(reason);
      });

      xhrDef.then(function(tool) {
        deferred.resolve(tool, true);
      }, function(err) {
        deferred.reject(err, true);
      }, function(evt) {
        deferred.progress(evt, true);
      });

      return deferred;
    },

    /**
     * Function to store a ToolEntry on the server. The ToolEntry must be sent to the Liberty server via REST.
     * 
     * @param {ToolEntry}
     *          tool - the ToolEntry to be persisted in the server's catalog
     * @returns {}
     */
    addToolEntry : function(toolProps) {
      if (!toolProps) {
        tr.throwMsg("No tool properties specified!");
      }
      var url = this.__url + "/toolEntries";
      var toAdd = new Toolbox.ToolEntry(toolProps);
      console.log("Adding Entry", JSON.stringify(toAdd));
      var options = {
        handleAs : "json",
        headers : {
          "Content-type" : "application/json"
        },
        data : JSON.stringify(toAdd)
      };
      var xhrDef = xhr.post(url, options);

      // Establish the Deferred to be returned.
      // This allows the caller to cancel the underlying XHR request.
      var deferred = new Deferred(function cancelXHRDeferred(reason) {
        xhrDef.cancel(reason);
      });

      xhrDef.then(function handleAppState(tool) {
        deferred.resolve(toolProps, true);
      }, function(err) {
        deferred.reject(err, true);
      }, function(evt) {
        deferred.progress(evt, true);
      });

      return deferred;
    },

    /**
     * Function to store a Bookmark on the server. The Bookmark must be sent to the Liberty server via REST.
     * 
     * @param {Tool}
     *          tool - the Tool to be persisted in the server's catalog
     * @returns {}
     */
    addBookmark : function(bookmarkProps) {
      if (!bookmarkProps) {
        tr.throwMsg("No bookmark properties specified!");
      }
      var url = this.__url + "/bookmarks";
      var toAdd = new Toolbox.Bookmark(bookmarkProps);
      var options = {
        handleAs : "json",
        headers : {
          "Content-type" : "application/json"
        },
        data : JSON.stringify(toAdd)
      };
      var xhrDef = xhr.post(url, options);

      // Establish the Deferred to be returned.
      // This allows the caller to cancel the underlying XHR request.
      var deferred = new Deferred(function cancelXHRDeferred(reason) {
        xhrDef.cancel(reason);
      });

      xhrDef.then(function handleAppState(tool) {
        deferred.resolve(bookmarkProps, true);
      }, function(err) {
        deferred.reject(err, true);
      }, function(evt) {
        deferred.progress(evt, true);
      });

      return deferred;
    },

    /**
     * Function used to delete a specific tool on the server. The Tool must be sent to the Liberty server via REST
     * 
     * @param {String}
     *          id - ID of the Tool that is being delete in the server's toolbox
     * @returns {Response} The response
     */
    deleteTool : function(id) {
      if (!id) {
        tr.throwMsg("Delete a tool but did not provide an ID!");
      }
      var url = this.__url + "/toolEntries/" + id;
      var options = {
        handleAs : "json"
      };
      var xhrDef = xhr.del(url, options);

      // Establish the Deferred to be returned.
      // This allows the caller to cancel the underlying XHR request.
      var deferred = new Deferred(function cancelXHRDeferred(reason) {
        xhrDef.cancel(reason);
      });

      xhrDef.then(function(tool) {
        deferred.resolve(tool, true);
      }, function(err) {
        deferred.reject(err, true);
      }, function(evt) {
        deferred.progress(evt, true);
      });

      return deferred;
        },

        /**
         * Function used to get the user preferences in the toolbox
         * 
         * @returns {Map} Map of preferences
         */
        getPreferences : function() {
            var url = this.__url + "/preferences";
            var options = {
                    handleAs : "json"
            };
            var xhrDef = xhr.get(url, options);

            // Establish the Deferred to be returned.
            // This allows the caller to cancel the underlying XHR request.
            var deferred = new Deferred(function cancelXHRDeferred(reason) {
                xhrDef.cancel(reason);
            });

            xhrDef.then(function(preferences) {
                deferred.resolve(preferences, true);
            }, function(err) {
                deferred.reject(err, true);
            }, function(evt) {
                deferred.progress(evt, true);
            });

            return deferred;
        },

        /**
         * Function to update the user preferences in the toolbox.
         * 
         * @param {Map} prefs - the preferences to set in the toolbox
         * @returns {}
         */
        updatePreferences: function(prefs) {
            if (!prefs) {
              tr.throwMsg("No preferences properties specified!");
            }
            var url = this.__url+"/preferences";
            var options = { handleAs: "json", headers: {"Content-type":"application/json"}, data: JSON.stringify(prefs) };
            var xhrDef = xhr.put(url, options);

            // Establish the Deferred to be returned.
            // This allows the caller to cancel the underlying XHR request.
            var deferred = new Deferred(function cancelXHRDeferred(reason) {
                xhrDef.cancel(reason);
            });

            xhrDef.then(function handleAppState(tool) {
                deferred.resolve(prefs, true);
            }, function(err) {
                deferred.reject(err, true);
            }, function(evt) {
                deferred.progress(evt, true);
            });

            return deferred;
        },
        
        /**
         * Function used to update all the Tools in the toolbox
         * 
         * @param The list of ToolEntry.
         * @returns {Response} The response
         */
        updateToolEntries : function(toolEntries) {
            if (!toolEntries) {
              tr.throwMsg("No tool specified!");
            }
            console.log("in updateToolEntries: ", toolEntries);
            console.log("a stringify version", JSON.stringify(toolEntries));
            var url = this.__url + "/toolEntries"; 
            var options = {
                    handleAs : "json", headers: {"Content-type":"application/json"}, data: JSON.stringify(toolEntries)
            };
            var xhrDef = xhr.put(url, options);

            // Establish the Deferred to be returned.
            // This allows the caller to cancel the underlying XHR request.
            var deferred = new Deferred(function cancelXHRDeferred(reason) {
                xhrDef.cancel(reason);
            });

            xhrDef.then(function(response) {
                deferred.resolve(response, true);
            }, function(err) {
                deferred.reject(err, true);
            }, function(evt) {
                deferred.progress(evt, true);
            });

            return deferred;
        },
        
        createToolEntry: function(entryProps) {
            if (entryProps.id !== undefined && entryProps.type !== undefined) {
                var toolEntry = new Toolbox.ToolEntry(entryProps);
                return toolEntry;
            }  
    }
  });

  /**
   * Define the representation of a Toolbox's Tool. USAGE: var tool = new toolbox.ToolEntry(properties); // properties is a parsed result
   * from JSON
   * 
   * @class Toolbox.ToolEntry
   * @returns {Tool} The Tool Object
   */
  Toolbox.ToolEntry = declare("Toolbox.ToolEntry", null,
  /**
   * @lends Toolbox.ToolEntrys.prototype
   */
  {
    id : null,
    type : null,

    /**
     * Constructor.
     * 
     * @param args -
     *          Initialization object: {{id:String,type:String}}
     */
    constructor : function(args) {
      this.id = args.id;
      this.type = args.type;
    }
  });

  /**
   * Define the representation of a Toolbox's Tool. USAGE: var tool = new toolbox.Bookmark(properties); // properties is a parsed result
   * from JSON
   * 
   * @returns {Tool} The Tool Object
   */
  Toolbox.Bookmark = declare("Toolbox.Bookmark", null,
  /**
   * @lends Toolbox.Bookmark.prototype
   */
  {
    name : null,
    url : null,
    icon : null,

    /**
     * Constructor.
     * 
     * @param args
     *          Initialization object: {name:"appName"}
     */
    constructor : function(args) {
      this.name = args.name;
      this.url = args.url;
      this.icon = args.icon;
    }
  });

  var toolboxInstance = new Toolbox();

    toolboxInstance.PREFERENCE_BIDI_ENABLED = "bidiEnabled";
    toolboxInstance.PREFERENCE_BIDI_TEXT_DIRECTION = "bidiTextDirection";
    toolboxInstance.PREFERENCE_BIDI_TEXT_DIRECTION_LTR = "ltr";
    toolboxInstance.PREFERENCE_BIDI_TEXT_DIRECTION_RTL = "rtl";
    toolboxInstance.PREFERENCE_BIDI_TEXT_DIRECTION_CONTEXTUAL = "contextual";

  return {
    getToolbox : function() {
      return toolboxInstance;
    }
  };
});
