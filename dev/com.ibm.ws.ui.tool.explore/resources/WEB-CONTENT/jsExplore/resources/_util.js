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
 * Various internal utility methods used by the resource classes.
 * 
 * @author Michael Thompson <mcthomps@us.ibm.com>
 * @module resources/_util
 * 
 * @return {Object} Containing all the utils methods
 */
define([ 'dojo/Deferred', 'dojo/promise/Promise', 'dojo/_base/lang', 'dojo/request', 'dojo/promise/all', './_topicUtil', 'dojo/i18n!./nls/resourcesMessages', 'dojo/i18n!../nls/explorerMessages', "jsExplore/resources/stats/_mbeanUtils", "jsExplore/utils/ID"],
    function(Deferred, Promise, lang, request, all, topicUtil, resourcesMessages, i18n, mbeanUtils, ID) {

  /* Global to hold our computed value */
  var acServer = null;

  return {
    /**
     * Finds the Error string in the input
     * 
     * @method
     * @param {String}
     *          input - The input string in which to search for an error
     * @returns {String} error - The first error string found in the input, starting with the Error ID and ending at the newline. If no
     *          error found in the input, returns null;
     */
    findErrorMsg : __findErrorMsg,

    /**
     * Finds the Warning string in the input
     * 
     * @method
     * @param {String}
     *          input - The input string in which to search for a warning
     * @returns {String} warning - The first warning string found in the input, starting with the warning ID and ending at the newline. If
     *          no warning found in the input, returns null;
     */
    findWarningMsg : __findWarningMsg,

    /**
     * Gets the first line of the stack trace, which tends to have a meaningful message
     * 
     * @method
     * @param {String}
     *          input - The input string from which to get the first line
     * @returns {String} firstLine - The first line of the stack trace, null otherwise
     */
    firstLineOfStackTrace : __firstLineOfStackTrace,

    /**
     * Helper method to populate an unpopulated resource.
     */
    doPopulate: __doPopulate,

    /**
     * I love this function name.
     */
    noopPop: function(resource) {
      var deferred = new Deferred();
      deferred.resolve(resource, true);
      return deferred;
    },

    populateStandaloneServer: __populateStandaloneServer,

    sendStateNotification: __sendStateNotification,

    appOperation: __appOperation,

    getAdminCenterServer: __getAdminCenterServer,

    getFromListByName: __getFromListByName,

    getFromListByNames: __getFromListByNames,
    returnValueAsDeferred: __returnValueAsDeferred,
    genericPopulateAndLookup: __genericPopulateAndLookup,
    genericPopulateAndReturnValue: __genericPopulateAndReturnValue,
    getCollectionPartialCount: __getCollectionPartialCount,
    getResourceDisplayedState: __getResourceDisplayedState,
    getResourceDisplayedLabel: __getResourceDisplayedLabel
  };

  /**
   * Finds the Error string in the input
   * 
   * @private
   * @param {String}
   *          input - The input string in which to search for an error
   * @returns {String} error - The first error string found in the input, starting with the Error ID and ending at the newline. If no error
   *          found in the input, returns null;
   */
  function __findErrorMsg(input) {
    var errMsg = null;
    var errStartIndex = input.search(/[A-Z]{5}\d{4}[E]:\s/);
    var errEndIndex = input.search(/\r\n|\r|\n/g);
    if (errStartIndex != -1 && errEndIndex != -1) {
      errMsg = input.substring(errStartIndex, errEndIndex);
    }
    return errMsg;
  }
  ;

  /**
   * Finds the Warning string in the input
   * 
   * @private
   * @param {String}
   *          input - The input string in which to search for a warning
   * @returns {String} warning - The first warning string found in the input, starting with the warning ID and ending at the newline. If no
   *          warning found in the input, returns null;
   */
  function __findWarningMsg(input) {
    var warnMsg = null;
    var errStartIndex = input.search(/[A-Z]{5}\d{4}[W]:\s/);
    var errEndIndex = input.search(/\r\n|\r|\n/g);
    if (errStartIndex != -1 && errEndIndex != -1) {
      warnMsg = input.substring(errStartIndex, errEndIndex);
    }
    return warnMsg;
  }

  /**
   * Gets the first line of the stack trace, which tends to have a meaningful message
   * 
   * @private
   * @param {String}
   *          input - The input string from which to get the first line
   * @returns {String} firstLine - The first line of the stack trace, null otherwise
   */
  function __firstLineOfStackTrace(stackTrace) {
    var firstLine = null;
    var errStartIndex = stackTrace.search('"stackTrace": "');
    if (errStartIndex != -1) {
      errStartIndex += '"stackTrace": "'.length;
    } else {
      errStartIndex = 0;
    }
    var errEndIndex = stackTrace.search(/\r\n|\r|\n/g);
    if (errEndIndex != -1) {
      firstLine = stackTrace.substring(errStartIndex, errEndIndex);
    }
    return firstLine;
  }

  function __processError(url, error) {
    var errMsg;
    if (error.response && error.response.text) {
      errMsg = error.response.text;
    } else if (error.response && error.response.status) {
      errMsg = lang.replace(resourcesMessages.ERROR_URL_REQUEST, [ error.response.status, url ]);
    } else {
      errMsg = lang.replace(resourcesMessages.ERROR_URL_REQUEST_NO_STATUS, [ url ]);
    }
    error.errMsg = errMsg;
    return error;
  };

  function __doPopulate(url, objToUpdate, updateObjFn) {
    var deferred = new Deferred();

    // If we've been populated, immediately resolve the deferred
    if (objToUpdate.populated) {
      deferred.resolve(objToUpdate, true);
    } else {
      // take care of case when populate is called twice during hashtoView scenario so as to
      // prevent the collection list getting populated twice when the second call comes in before
      // the first call is finished.
      if (!objToUpdate.populating) {
        objToUpdate.populating = deferred;
        request(url, { handleAs : 'json', preventCache : true }).then(function(payload) {
          try {
            var result = updateObjFn(objToUpdate, payload);
            if (result instanceof Promise){
              result.then(function(){
                objToUpdate.populated = true;
                objToUpdate.populating = undefined;
                deferred.resolve(objToUpdate, true);
              });
            } else {
              objToUpdate.populated = true;
              objToUpdate.populating = undefined;
              deferred.resolve(objToUpdate, true);
            }
          } catch(err) {
            console.error('Error while populating from ' + url + ' - the updateObjFn threw an exception.', err);
            deferred.reject(err, true);
          }
        }, function(err) {
          __processError(url, err);
          objToUpdate.populating = undefined;
          deferred.reject(err, true);
        });
      } else {
        return objToUpdate.populating;
      }
    }

    return deferred;
  }

  function __populateStandaloneServer(objToUpdate, updateObjFn, skipPopulatedCheck) {
    var deferred = new Deferred();

    if (objToUpdate.populated && !skipPopulatedCheck) {
      // If this has already been populated, return the object to update as there is no work to be done
      deferred.resolve(objToUpdate, true);
    } else {
      if (objToUpdate.populating) {
        // If the object is currently populating, return the stored deferred which is handling that job
        return objToUpdate.populating;
      } else {
        // If the object is not yet populating, store the deferred which we will use to populate it
        objToUpdate.populating = deferred;

        __getStandaloneServerInfo().then(function(serverInfo) {
          __addStandaloneClusterInfo(serverInfo).then(function(serverInfo) {
            __addStandaloneAppInfo(serverInfo).then(function(serverInfo) {
              try {
                updateObjFn(objToUpdate, serverInfo);

                // Indicate that the resource is now populated.
                objToUpdate.populated = true;
                objToUpdate.populating = undefined;

                deferred.resolve(objToUpdate, true);
              } catch(err) {
                console.error('Error while populating standalone server resource - the updateObjFn threw an exception.', err);
                objToUpdate.populating = undefined;
                deferred.reject(err, true);
              }
            }, function(err) {
              objToUpdate.populating = undefined;
              deferred.reject(err, true);
            });
          }, function(err) {
            objToUpdate.populating = undefined;
            deferred.reject(err, true);
          });
        }, function(err) {
          objToUpdate.populating = undefined;
          deferred.reject(err, true);
        });
      }
    }

    return deferred;
  }

  function __getStandaloneServerInfo() {
    var deferred = new Deferred();

    /* Response: [{"name":"Name","value":{"value":"uiDev","type":"java.lang.String"}},{"name":"DefaultHostname","value":{"value":"localhost","type":"java.lang.String"}},{"name":"UserDirectory","value":{"value":"C:/sandbox/workspace.libertyDev/build.image/wlp/usr/","type":"java.lang.String"}}] */
    var url = '/IBMJMXConnectorREST/mbeans/WebSphere%3Afeature%3Dkernel%2Cname%3DServerInfo/attributes';
    var options = { handleAs : 'json', preventCache : true };
    request.get(url, options).then(function(attributes) {
      var hostname = "TBD";
      var userdir = "TBD";
      var server = "TBD";

      for(var i = 0; i < attributes.length; i++) {
        var attrObj = attributes[i];
        if (attrObj.name === "Name") {
          server = attrObj.value.value;
        } else if (attrObj.name === "UserDirectory") {
          userdir = attrObj.value.value.substr(0, attrObj.value.value.length-1); // Need to drop trailing slash
        } else if (attrObj.name === "DefaultHostname") {
          hostname = attrObj.value.value;
        } else {
          console.log("WebSphere:name=ServerIdentity had an unrecognized attribute: " + attrObj.name);
        };
      }

      var serverInfo = {
          name: server,
          userdir: userdir,
          host: hostname
      };

      deferred.resolve(serverInfo, true);
    }, function(err) {
      __processError(url, err);
      deferred.reject(err, true);
    });

    return deferred;
  }

  function __addStandaloneClusterInfo(serverInfoToUpdate) {
    var deferred = new Deferred();
    
    // Since mbeanUtils.isLibertyFeatureActive does not return deferred.reject, no need to handle error scenario
    mbeanUtils.isLibertyFeatureActive(serverInfoToUpdate, "clusterMember-1.0").then(function(resultBool){

      if(resultBool){

        /* Response: { "value": "defaultCluster", "type": "java.lang.String" } */
        var url = '/IBMJMXConnectorREST/mbeans/WebSphere%3Afeature%3DclusterMember%2Cname%3DClusterMember%2Ctype%3DClusterMember/attributes/Name';
        var options = { handleAs : 'json', preventCache : true };
        request.get(url, options).then(function(payload) {
          serverInfoToUpdate.cluster = payload.value;
          deferred.resolve(serverInfoToUpdate, true);
        }, function(err) {
          __processError(url, err);
          deferred.reject(err, true);
        });
      }else{
        deferred.resolve(serverInfoToUpdate, true);
      }

    });
    return deferred;
  }

  /**
   * Properly extract the application name out of the Object Name.
   * 
   * The Object Name could be either: "WebSphere:service=com.ibm.websphere.application.ApplicationMBean,name=jsApp" OR
   * "WebSphere:name=jsApp,service=com.ibm.websphere.application.ApplicationMBean"
   * 
   * @method
   * @private
   * @param {String}
   *          objectName - The name of the object from which to get the app name
   * @return {String} The application name embedded within the Object Name
   */
  function getAppNameFromObjectName(objectName) {
    var name = objectName.substring(objectName.indexOf("name=") + 5);
    return name.split(",")[0];
  }

  function __addStandaloneAppInfo(serverInfoToUpdate) {
    var deferred = new Deferred();

    /* Response: ... */
    var url = '/IBMJMXConnectorREST/mbeans?objectName=WebSphere:service=com.ibm.websphere.application.ApplicationMBean,name=*';
    var options = { handleAs : 'json', preventCache : true };
    request.get(url, options).then(function(objectNames) {
      var appPromises = new Array();
      var appNames = new Array();
      // For each objectName, kick off a GET
      for (var i in objectNames) {
        var appName = getAppNameFromObjectName(objectNames[i].objectName);
        appNames[i] = appName;
        var appStateUrl = "/IBMJMXConnectorREST/mbeans/WebSphere:service=com.ibm.websphere.application.ApplicationMBean,name=" + appName + "/attributes/State";
        appPromises[i] = request.get(appStateUrl, options).then(function(response) {
          var app = {
              state : ((response.value === "STARTED") ? "STARTED" : "STOPPED")
          };
          return app;
        });
      }
      // With promises all, all have to be resolved successfully to be able to tally the count.
      // Otherwise, if one is rejected, we have to bubble up the reject so as to allow a new deferred
      // to be initiated or the error to be surfaced after 3 tries.
      return all(appPromises).then(function(allApps) {
          var apps = {};
          var totalUp = 0;
          var totalDown = 0;
          var list = new Array();
          for ( var j in appNames) {
              allApps[j].name = appNames[j];
              list[j] = allApps[j];
              if (allApps[j].state === "STARTED") {
                  totalUp++;
              } else {
                  totalDown++;
              }
          }
          apps.up = totalUp;
          apps.down = totalDown;
          apps.unknown = 0;
          apps.list = list;
          serverInfoToUpdate.apps = apps;
          deferred.resolve(serverInfoToUpdate, true);
      }, function(err) {
          // no need to call processError as err.message is descriptive enough
          deferred.reject(err, true);
      });
    }, function(err) {
      __processError(url, err);
      deferred.reject(err, true);
    });

    return deferred;
  }

  /**
   * Send state change notification events for the server.
   */
  function __sendStateNotification(resource, state, origin) {
    topicUtil.publish(resource.__myTopic, {
      id: resource.id,
      type: resource.type,
      state: state,
      origin: origin
    });
  }

  /**
   * Determines the appropriate error message for the given app operation.
   */
  function getAppOperationErrorMessage(appResource, operation, err) {
    var errMsg;

    if (err && err.response && err.response.data && err.response.data.stackTrace) {
      var stackTrace = err.response.data.stackTrace;
      errMsg = __findErrorMsg(stackTrace);
      if (errMsg) {
        return errMsg;
      } else if (err.response.status === 400
          && err.response.data.stackTrace.search("javax.management.InstanceNotFoundException:") != -1) {
        return lang.replace(resourcesMessages.ERROR_APP_NOT_AVAILABLE, [ appResource.name, appResource.server.name, appResource.server.host, appResource.server.userdir ]);
      }
      errMsg = __firstLineOfStackTrace(stackTrace);
      if (errMsg) {
        return errMsg;
      }
    }

    if (err.response && err.response.status) {
      return err.response.status + " "
      + lang.replace(resourcesMessages.ERROR_APPLICATION_OPERATION, [ operation, appResource.name, appResource.server.name, appResource.server.host,
                                                                      appResource.server.userdir ]);
    }
    // Unable to determine what caused the operation to fail. Return errMsg regarding the failure. I don't think this path is
    // reachable since that would mean the response did not include a status code....which I don't think allowed...but just in case
    errMsg = lang.replace(resourcesMessages.ERROR_APPLICATION_OPERATION, [ operation, appResource.name, appResource.server.name, appResource.server.host,
                                                                           appResource.server.userdir ]);
    return errMsg;
  }
  /**
   * Implementation for applications.start(), applications.stop(), applications.restart().
   * 
   * @private
   * @param {String}
   *          appResource - The application to stop.
   * @param {String}
   *          operation - The mode, either 'start', 'stop' or 'restart'
   * @return {Promise} response - The result of the operation. If an error occurred, the result will contain not only the full err object,
   *         but also a errMsg property that can be used to display a prepared message regarding the result.
   */
  function __appOperation(appResource, operation) {
    var prevState = appResource.state;
    // Fire off an 'acting' event
    if (operation === 'stop') {
      __sendStateNotification(appResource, 'STOPPING', '_util.js:419');
    } else if (operation === 'start' || operation === 'restart') {
      __sendStateNotification(appResource, 'STARTING', '_util.js:421');
    } else {
      console.error('__appOperation, unknown operation: ' + operation);
    }
    var server = appResource.server; // Safe dereference, since AppOnServer is constructed with its Server object 
    var url = null;
    var options = null;
    if (server && server.type !== 'standaloneServer') {
      // debugging for appOnCluster partial stop problem  
      var appOnCluster = false;
      if (appResource.cluster) {
        appOnCluster = true;
      }
      if (appOnCluster) {
          var comment = document.createComment(" " + operation + "-" + appResource.name + "-" + server.name + "-" + Date.now() + " -- " + appResource.cluster);
          document.head.appendChild(comment);
      }
      // end debugging codes

      // controller routing
      url = '/IBMJMXConnectorREST/router/mbeans/WebSphere:service=com.ibm.websphere.application.ApplicationMBean,name=' + appResource.name + '/operations/' + operation;
      options = {
          handleAs : 'json',
          headers : {
            'Content-type' : 'application/json',
            'com.ibm.websphere.jmx.connector.rest.routing.hostName' : server.host,
            'com.ibm.websphere.jmx.connector.rest.routing.serverUserDir' : server.userdir,
            'com.ibm.websphere.jmx.connector.rest.routing.serverName' : server.name
          },
          data : '{"params":[],"signature":[]}'
      };
    } else {
      // standalone
      // Returns Object {value: null, type: null}
      url = '/IBMJMXConnectorREST/mbeans/WebSphere:service=com.ibm.websphere.application.ApplicationMBean,name=' + appResource.name + '/operations/' + operation;
      options = {
          handleAs : 'json',
          headers : {
            'Content-type' : 'application/json'
          },
          data : '{"params":[],"signature":[]}'
      };
    }

    var deferred = new Deferred();

    request.post(url, options).then(function(response) {
      // We used to send the final state as an event, but we do not do this anymore to ensure all affected groups are updated.
      // Do not add the state change notifications back!
      if (operation === 'stop') {
        response.nowState = "STOPPED";
      } else if (operation === 'start' || operation === 'restart') {
        response.nowState = "STARTED";
      } else {
        console.error('__serverOperation, unknown operation: ' + operation);
      }
      response.prevState = prevState;
      if (operation === 'restart') {
        __sendStateNotification(appResource, 'RESTARTED', '_util.js:468');
      }
      deferred.resolve(response, true);
    }, function(err) {
      topicUtil.publish(appResource.__myTopic, {
        state: 'UNKNOWN',
        origin: '_util.js:455'
      });

      var errMsg = getAppOperationErrorMessage(appResource, operation, err);
      err.errMsg = errMsg;

      deferred.resolve(err, true); // Right now we're always resolving with the response... we could reject though!
    });

    return deferred;
  }

  function __getAdminCenterServer() {
    if (acServer) {
      return acServer;
    }
    var url = "/IBMJMXConnectorREST/mbeans/WebSphere%3Afeature%3Dkernel%2Cname%3DServerInfo/attributes";
    var options = {
        handleAs : "json",
        preventCache : true,
        sync: true
    };
    request.get(url, options).then(
        //[{"name":"Name","value":{"value":"uiDev","type":"java.lang.String"}},{"name":"DefaultHostname","value":{"value":"localhost","type":"java.lang.String"}},{"name":"UserDirectory","value":{"value":"C:/sandbox/workspace.libertyDev/build.image/wlp/usr/","type":"java.lang.String"}}]
        function(attributes) {
          var hostname = "TBD";
          var userdir = "TBD";
          var server = "TBD";
          var installdir = "TBD";
          var i;

          for(i = 0; i < attributes.length; i++) {
            var attrObj = attributes[i];
            if (attrObj.name === "Name") {
              server = attrObj.value.value;
            } else if (attrObj.name === "UserDirectory") {
              userdir = attrObj.value.value.substr(0, attrObj.value.value.length-1); // Need to drop trailing slash
            } else if (attrObj.name === "DefaultHostname") {
              hostname = attrObj.value.value;
            } else if (attrObj.name === "InstallDirectory"){
              installdir = attrObj.value.value;
            } else {
              console.log("WebSphere:name=ServerIdentity had an unrecognized attribute: " + attrObj.name);
            }
          }
          var serverObj = {
              id : ID.commaDelimit(hostname, userdir, server),
              name : server,
              userdir: userdir,
              host : hostname
          };
          acServer = serverObj;
          return serverObj;
        }, function(error) {
          var errMsg;
          if (error.response && error.response.text) {
            errMsg = error.response.text;
          } else if (error.response && error.response.status) {
            errMsg = lang.replace(resourcesMessages.ERROR_URL_REQUEST, [ error.response.status, url ]);
          } else {
            errMsg = lang.replace(resourcesMessages.ERROR_URL_REQUEST_NO_STATUS, [ url ]);
          }
          error.errMsg = errMsg;
          return error;
        });
    return acServer;
  }

  function __getFromListByName(collection, id, notFoundMsg) {
    var deferred = new Deferred();
    collection.populate().then(function(updatedCollection) {
      for(var i = 0; i < updatedCollection.list.length; i++) {
        var element = updatedCollection.list[i];
        if (element.id === id) {
          deferred.resolve(element, true);
          return;
        }
      }
      deferred.reject(notFoundMsg, true);
    }, function(err) {
      deferred.reject(err, true);
    });
    return deferred;
  }

  function __getFromListByNames(collection, ids, notFoundMsg) {
    var deferred = new Deferred();
    collection.populate().then(function(updatedCollection) {
      var matches = [];
      for (var n = 0; n < ids.length; n++){
        var id = ids[n];
        for(var i = 0; i < updatedCollection.list.length; i++) {
          var element = updatedCollection.list[i];
          if (element.id === id) {
            matches.push(element);
            break;
          }
        }
      }
      if (matches.length != ids.length) {
        deferred.reject(notFoundMsg, true);
      } else {
        deferred.resolve(matches, true);
      }
    }, function(err) {
      deferred.reject(err, true);
    });
    return deferred;
  }

  function __returnValueAsDeferred(value) {
    var deferred = new Deferred();
    deferred.resolve(value, true);
    return deferred;
  }

  function __genericPopulateAndReturnValue(obj, getValue) {
    if (obj.isPopulated) {
      return util.returnValueAsDeferred(getValue(obj));
    } else {
      // Do populate and return value
      var deferred = new Deferred();
      obj.populate().then(function(populatedObj) {
        deferred.resolve(getValue(populatedObj), true);
      });
      return deferred;
    }
  }


  function __genericPopulateAndLookup(obj, lookupFn, getLookupName) {
    if (obj.isPopulated) {
      return lookupFn(getLookupName(ojb));
    } else {
      // Do populate and lookup
      var deferred = new Deferred();
      obj.populate().then(function(populatedObj) {
        if (getLookupName(populatedObj)) {
          lookupFn(getLookupName(populatedObj)).then(function(resolvedObj) {
            deferred.resolve(resolvedObj, true);  
          });
        } else {
          deferred.resolve(null, true);
        }
      });
      return deferred;
    }
  };

  function __getCollectionPartialCount(collection) {
    var resourcePartialCount = 0;
    collection.list.forEach(function(resource) {
      if (resource.state == "PARTIALLY STARTED" || resource.state == "PARTIALLY_STARTED") {
        if (resource.scalingPolicyEnabled) {
          console.log("do not increase partial count");
        } else {
          console.log("increase partial count");
          resourcePartialCount++;
        }
      }
    });

    console.log("__getCollectionPartialCount returning count: ", resourcePartialCount);
    return resourcePartialCount;
  };

  function __getResourceDisplayedState(resource) {
    var state = resource.state;
    if (resource.scalingPolicyEnabled && (resource.state == "PARTIALLY_STARTED" || resource.state == "PARTIALLY STARTED")) {
      state = "STARTED";
    }

    console.log("__getResourceDisplayedState returning state: ", state);
    return state;
  };

  function __getResourceDisplayedLabel(resource) {
    var state = __getResourceDisplayedState(resource);
    var label;
    switch (state) {
    case "STARTED": 
      label = i18n.RUNNING; 
      break;
    case "PARTIALLY STARTED":
    case "PARTIALLY_STARTED":    
      label = i18n.PARTIALLY_RUNNING;
      break;
    case "STARTING": 
      label = i18n.STARTING;
      break;
    case "STOPPED": 
      label = i18n.STOPPED;
      break;
    case "STOPPING": 
      label = i18n.STOPPING;
      break;
    case 'UNKNOWN':
    case 'NOT_FOUND': 
      label = i18n.UNKNOWN;
    }

    console.log("__getResourceDisplayedLabel returning label ", label);
    return label;
  };
});
