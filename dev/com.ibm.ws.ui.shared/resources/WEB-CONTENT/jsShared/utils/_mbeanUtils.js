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
 * Various mbean methods used by the graph stats classes.
 * 
 * @author Tim Mitchell <tim_mitchell@uk.ibm.com>
 * 
 * @return {Object} Containing all the mbean methods
 */
define(
    [ 'dojo/_base/lang', 'dojo/Deferred', 'dojo/request' ],
    function(lang, Deferred, request) {
      
      var mbeanPrefixURL = "/IBMJMXConnectorREST/mbeans/";
      // This variable holds the list of features we have found in the format: 
      //  {featureName: <feature>, hostName: <hostName>, userDir: <user directory>, server: <serverName>, provisioned: <true/false>};
      var featureStatus = new Array();
      
      /**
       * This method is used to invoke a GET http method against a remote mbean and return the results. It allows a deferred object to be passed in, if a particular
       * method is required to amend the results before returning to the calling method. This method does an Asynchronous xhr call.
       * 
       * @param {Server}
       *          server - The server resource that we can use to find the host, user dir and servername to send the mbean request to.
       * @param {String}
       *          url - The relative URL of the mbean to invoke.
       * @param {boolean}
       *          sync - Whether the call should be synchronous (true) or asynchronous(false)
       * @param {Deferred}
       *          deferred - An optional deferred to use for the call. This is used if the calling method has needed to use the deferred in
       *          its methods.
       * @param {Function}
       *          successFunction - An optional function to run when the deferred is processed. This is required if the data from the
       *          response doesn't fit the default format and the success function needs to reformat the response.
       * @return {Promise} response - The result of the operation. If an error occurred, the result will contain not only the full err
       *         object, but also a errMsg property that can be used to display a prepared message regarding the result.
       */
      function __invokeGetMBeanOperation(server, url, sync, deferred, successFunction) {
        return __invokeMBeanOperation(server, url, sync, "GET", null, null, deferred, successFunction);
      };
      
      /**
       * This method is used to invoke a POST http method against a remote mbean and return the results. It allows a deferred object to be passed in, if a particular
       * method is required to amend the results before returning to the calling method. This method does an Asynchronous xhr call. 
       * 
       * @param {Server}
       *          server - The server resource that we can use to find the host, user dir and servername to send the mbean request to.
       * @param {String}
       *          url - The relative URL of the mbean to invoke.
       * @param {boolean}
       *          sync - Whether the call should be synchronous (true) or asynchronous(false)
       * @param {String}
       *          params - The parameters for the POST Http request
       * @param {String}
       *          signature - The signature for the parameter types
       * @param {Deferred}
       *          deferred - An optional deferred to use for the call. This is used if the calling method has needed to use the deferred in
       *          its methods.
       * @param {Function}
       *          successFunction - An optional function to run when the deferred is processed. This is required if the data from the
       *          response doesn't fit the default format and the success function needs to reformat the response.
       * @return {Promise} response - The result of the operation. If an error occurred, the result will contain not only the full err
       *         object, but also a errMsg property that can be used to display a prepared message regarding the result.
       */
      function __invokePostMBeanOperation(server, url, sync, params, signature, deferred, successFunction) {
        return __invokeMBeanOperation(server, url, sync, "POST", params, signature, deferred, successFunction);
      };
      
      /**
       * This method is used to call to a remote mbean and return the results. It allows a deferred object to be passed in, if a particular
       * method is required to amend the results before returning to the calling method.
       * 
       * @param {Server}
       *          server - The server resource that we can use to find the host, user dir and servername to send the mbean request to.
       * @param {String}
       *          url - The relative URL of the mbean to invoke.
       * @param {boolean}
       *          sync - Whether the call should be synchronous (true) or asynchronous(false)
       * @param {String}
       *          params - The parameters for the POST Http request
       * @param {String}
       *          signature - The signature for the parameter types
       * @param {Deferred}
       *          deferred - An optional deferred to use for the call. This is used if the calling method has needed to use the deferred in
       *          its methods.
       * @param {Function}
       *          successFunction - An optional function to run when the deferred is processed. This is required if the data from the
       *          response doesn't fit the default format and the success function needs to reformat the response.
       * @return {Promise} response - The result of the operation. If an error occurred, the result will contain not only the full err
       *         object, but also a errMsg property that can be used to display a prepared message regarding the result.
       */
      function __invokeMBeanOperation(server, url, sync, httpMethod, params, signature, deferred, successFunction) {
        var options = null;
        // If the MBean call is for a collective member, then we need to ensure we configure the routing
        // context.
        
        if (params === undefined) {
          params = '[]';
          signature = '[]';
        };
        
        var data = "";
        if (httpMethod === "POST")
            data = '{"params":' + params + ',"signature":' + signature + '}';
        
        if (server && server.type && server.type === 'server' && !server.isAdminCenterServer) {
          options = {
              handleAs : 'json',
              headers : {
                  'Content-type' : 'application/json',
                  'com.ibm.websphere.jmx.connector.rest.routing.hostName' : encodeURIComponent(server.host),
                  'com.ibm.websphere.jmx.connector.rest.routing.serverUserDir' : encodeURIComponent(server.userdir),
                  'com.ibm.websphere.jmx.connector.rest.routing.serverName' : encodeURIComponent(server.name)
              },
              sync: sync,
              preventCache: true,
              data: data
          };
        } else {
          // If not then we can just call the mbean on the local server.
          options = {
              handleAs : 'json',
              headers : {
                'Content-type' : 'application/json'
              },
              sync: sync,
              preventCache: true,
              data: data
          };
        }

        // If we have no deferred passed in, then create a new one.
        if (deferred === undefined)
          deferred = new Deferred();

        // This is the default success function. It takes the response, which is an array of key:value objects,
        // and turns this into a JSON object.
        defaultSuccessFunction = function(response) {
          var newResponse = "{";
          // Iterate over each object in the response and build up JSON key value pairs.
          for (var i = 0; i < response.length; i++) {
            if (i > 0)
              newResponse += ", ";
            newResponse += "\"" + (response[i].name) + "\": " + response[i].value.value;
          }
          newResponse += "}";
          // Resolve the deferred to pass back the new response.
          deferred.resolve(newResponse, true);
        };

        // Run the async get of the data from the mbean, using either the passed in success function or the default one.
        var httpPromise;
        if (httpMethod == "GET") {
          httpPromise = request.get(url, options);
        } else {
          httpPromise = request.post(url, options);
        }
          
        httpPromise.then((successFunction !== undefined) ? successFunction : defaultSuccessFunction, 
            function(err) {
              deferred.resolve(err, true); // Right now we're always resolving with the response... we could reject though!
            }
        );  

        return deferred;
      };
      
      /**
       * This method returns an array of mbeans that match a particular set of params. The user
       * passes a string of mbean parms that if all match, the mbean is added to the return list.
       * It allows you to supply a subset of the params of the mbean to get a match.
       * You can also supply params that end with * that says the param must start with the string prior to the *
       * 
       * so a passed in argument of WebSphere:type=ServletStats,name=App1*
       * 
       * would return mbeans for all of the servlets of App1, as the name would be App1.<servletName>
       * 
       * @param {Server}
       *          server - The server resource that we can use to find the host, user dir and servername to send the mbean request to.
       * @param {String}
       *          objectNameString - A String of mbean objectName params to match against objectNames in the system. A "*"
       *                             can be placed on the end of any of the params to do wildcard searching e.g.
       *                             WebSphere:type=Servlet*,name=App1*
       * @param {boolean}
       *          sync - whether the call should be synchronous (true) or asynchronous(false)
       * @return {Array} - An array of Strings containing the matching MBean ObjectNames.
       * 
       */
      function __getMBeans(server, objectNameString, sync) {
        
        var deferred = new Deferred();
        var successFunction = function(response) {
            deferred.resolve(response, true);
        };
        
        if (sync === null || sync === undefined)
          sync = false;
        // Break the passed in string into separate parameters, separating on commas.
        var mbeanParams = objectNameString.split(",");
        
        // Invoke the mbean operation to get all the mbeans in the system, and then process them in the returned promise.
        return this.invokeGetMBeanOperation(server, this.getMBeanPrefixURL(), sync, deferred, successFunction).then(function(response) {
          
            // The returned array containing all matching mbeans
            var mbeansFound = new Array();
            
            // Loop through each MBean Objectname in the response.
            for (var i = 0; i < response.length; i++) {
                // Split the current objectname into separate parms.
                var currMBeanParams = response[i].objectName.split(",");
                // This variable is used to work out whether we should continue to scan this object name. If we find that it doesn't
                // contain one of the users required parms, then we know we don't need to search through the rest of the parms.
                var matchedMBean = true;
                // Iterate over each of the user specified parms, as long as we are still matching.
                for (var j = 0; j < mbeanParams.length && matchedMBean; j++) {
                    // This boolean indicates when we have found the current user parm in the list of parms from the current response
                    // object name. We keep looping through each one until we have found a match. Once we match we don't need to go 
                    // through the rest of the parms.
                    var mbeanPropertyFound = false;
                    // Loop through all the parms on the response objectname until we find a match.
                    for (var k = 0; k < currMBeanParams.length && mbeanPropertyFound === false; k++) {
                        // If the passed mbean param ends with a wildcard * then we need to ensure that the current
                        // mbean property that we're checking starts with the param. 
                        // e.g. search parm is abc*, abcde matches and aabcd doesn't.
                        if (mbeanParams[j].indexOf("*", mbeanParams[j].length - 1) >= 0) {
                          if (currMBeanParams[k].indexOf(mbeanParams[j].substring(0, mbeanParams[j].length -1)) === 0)
                            mbeanPropertyFound = true;
                        // If we have no wildcard chars, then we need to do a standard equals check.
                        } else if (currMBeanParams[k] === mbeanParams[j]) {
                            mbeanPropertyFound = true;
                        }
                    }
                    
                    // If after we've finished checking the current object name we still don't have a match
                    // for the current property, then we know that this mbean shouldn't be added, and we can stop
                    // checking the rest of the user params.
                    if (mbeanPropertyFound === false)
                        matchedMBean = false;
                }
                
                // If after all the checking the Mbean does match, then add it to the list to return.
                if (matchedMBean)
                  mbeansFound.push(response[i].objectName);
            }
            
            return mbeansFound;
        }, 
        function(err) {
            deferred.resolve(err, true); // Right now we're always resolving with the response... we could reject though!
         });
    };
    
    function __isLibertyFeatureActive(server, libertyFeatureShortName) {
      
      var parms = "[{'value': 'com.ibm.wsspi.kernel.feature.LibertyFeature', 'type':'java.lang.String'}, {'value': '(ibm.featureName=" + libertyFeatureShortName + ")', 'type':'java.lang.String'}]";
      var signature = "['java.lang.String', 'java.lang.String']";
      var outerDeferred = new Deferred();
      
      // Check to see if we have already looked up the feature. If so get the provision status and return the result.
      // Otherwise, look up the OSGi service to see if the feature has been provisioned.
      var requestedFeature = null;
      for (var i = 0; i < featureStatus.length && requestedFeature === null; i++) {
        var previouslyFoundFeature = featureStatus[i];
        // If the featureName, the hostName, the userDir and server Name all match then use this to 
        // find out the previously stored state.
        if (previouslyFoundFeature.featureName === libertyFeatureShortName && 
            previouslyFoundFeature.hostName === server.host && 
            previouslyFoundFeature.userDir === server.userdir && 
            previouslyFoundFeature.serverName === server.name)
          requestedFeature = previouslyFoundFeature;
      }
      
      // If we haven't found a previous check of the feature, we need to do the lookup.
      if (requestedFeature === null) {
        
        this.getMBeans(server, 'osgi.core:type=serviceState,framework=org.eclipse.osgi', true).then(lang.hitch(this, function(response) {
          if (response.length > 0) {
            var innerDeferred = new Deferred();
            var successFunction = function(response) {
                innerDeferred.resolve(response, true);
            };
            this.invokePostMBeanOperation(server, this.getMBeanPrefixURL() + response[0] + "/operations/listServices", true, parms, 
                signature, innerDeferred, successFunction).then(function(response) {
                    var libertyFeatureFound;
                    if (response.value.length > 0)
                      libertyFeatureFound = true;
                    else 
                      libertyFeatureFound = false;
                    
                    // Create new entry for the feature.
                    var storeFeature = {featureName: libertyFeatureShortName, 
                                        provisioned: libertyFeatureFound, 
                                        hostName: server.host,
                                        serverName: server.name,
                                        userDir: server.userdir};
                    // Store this in the global array.
                    featureStatus.push(storeFeature);
                    
                    outerDeferred.resolve(libertyFeatureFound, true);
            });
          } else {
            console.log("ServiceStateMBean not found");
          }
        }));
      } else {
        // If we have found the feature before then resolve it to the value previously stored.
        outerDeferred.resolve(requestedFeature.provisioned, true);
      }
      
      return outerDeferred;
    };
      
    return {
        invokeGetMBeanOperation: __invokeGetMBeanOperation,
        invokePostMBeanOperation: __invokePostMBeanOperation,
        getMBeanPrefixURL: function(){ return mbeanPrefixURL;},
        // N.B. isLibertyFeatureActive is a number of synchronous XHR calls.
        isLibertyFeatureActive: __isLibertyFeatureActive,
        clearLibertyFeatureCache: function() {featureStatus = new Array();},
        getMBeans: __getMBeans
    };
      
});
