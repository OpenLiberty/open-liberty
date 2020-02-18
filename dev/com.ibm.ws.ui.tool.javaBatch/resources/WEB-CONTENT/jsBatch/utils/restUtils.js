/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/**
 * This utility is to help make HTTP requests.  In this file, there are many things we need to
 * map between the terminology in Admin Center and the REST APIs.  In some cases, the Admin Center
 * will try to give a visually pleasing wording to a backend REST API parameter.  Since those transformation
 * mostly happen here in this file, the Admin Center code is be coded against the set Admin Center terminology
 * rather than against the REST API parameters.
 *
 */
define([ 'dojo/request/xhr',
         'dojo/request',
         'dojo/json',
         'dojo/Deferred',
         'jsShared/utils/toolData',
         'jsShared/utils/userConfig',
         'dojo/_base/lang',
         'dojo/io-query',
         'jsBatch/utils/hashUtils'], function(xhr, request, json, Deferred, toolData, userConfig, lang, ioQuery, hashUtils) {
  "use strict";
  return {
    get: __get,
    getWithParms: __getWithParms,
    put: __put,
    deleteWithParms: __deleteWithParms,

    getJobInstancesQuerySize: __getJobInstancesQuerySize,
    putJobInstancesQuerySize: __putJobInstancesQuerySize,
    JOB_INSTANCES_DEFAULT_QUERY_SIZE: jobInstancesDefaultQuerySize,

    BASE_URI : base_uri,
    URL : url,
    v4JobInstances : v4JobInstances,

    JOB_EXECUTIONS_URL_QUERY: jobExecutionsURL,
    JOB_INSTANCE_DETAIL_QUERY: jobInstanceDetailURL,
    EXECUTION_DETAIL_QUERY: executionDetailURL,
    EXECUTION_STEPS_DETAIL_QUERY: executionStepsDetailURL,
    JOB_INSTANCE_LOGS_QUERY: jobInstanceLogsURL,
    JOB_EXECUTION_LOGS_QUERY: jobExecutionLogsURL,

    JOB_INSTANCE_RESTART: jobInstanceRestartURL,
    JOB_INSTANCE_RESTART_PARMS: jobInstanceRestartParmsURL,
    JOB_INSTANCE_STOP: jobInstanceStopURL,

    JOB_INSTANCE_DELETE: jobInstancePurgeURL,

    getPersistedData : getPersistedData
  };

  /**
   * We need this special method because we have the following states of
   * ALL, QUEUED, ENDED.  These three states are not supported by the
   * REST APIs so we have to simulate these states in all of the UI code.
   * When the UI code uses these states and makes the REST call,
   * we will transform those three special states into their actual
   * states before calling the REST APIs.  The reason the transformation code
   * is in this code is so that the tool's codebase can behave as if
   * instance states query param ALL, QUEUED, ENDED are supported at the REST API
   * level.  In reality, this rest utility file will just fake the three states.
   */
  function __handleInstanceStates(URL) {
    // NOOP this method if there instance state parameter is missing
    var startOfQueryParams = URL.indexOf('?');
    if(URL.indexOf('instanceState=') === -1) {
      return URL;
    }

    startOfQueryParams++;  // We don't want to include the ? char

    var query = URL.substring(startOfQueryParams);
    var allQueryObject = ioQuery.queryToObject(query);

    var instanceStates = allQueryObject.instanceState.split(',');
    for(var i = 0; i < instanceStates.length; i++) {
      // Uppercase all of the instanceStates elements to be case insensitive.
      instanceStates[i] = instanceStates[i].toUpperCase();
    }

    var allStates = ["SUBMITTED","JMS_QUEUED","JMS_CONSUMED","DISPATCHED","FAILED","STOPPED","COMPLETED","ABANDONED"];
    var queuedStates = ["SUBMITTED","JMS_QUEUED","JMS_CONSUMED"];
    var endedStates = ["FAILED","STOPPED","COMPLETED","ABANDONED"];

    // Add the final instance states into newInstanceState that will be used in the REST API call.
    var newInstanceState = [];

    var allIndex = instanceStates.indexOf("ALL");
    if(allIndex > -1) {
      instanceStates.splice(allIndex, 1);
      newInstanceState = newInstanceState.concat(allStates);
    }

    var queuedIndex = instanceStates.indexOf("QUEUED");
    if(queuedIndex > -1) {
      instanceStates.splice(queuedIndex, 1);
      newInstanceState = newInstanceState.concat(queuedStates);
    }

    var endedIndex = instanceStates.indexOf("ENDED");
    if(endedIndex > -1) {
      instanceStates.splice(endedIndex, 1);
      newInstanceState = newInstanceState.concat(endedStates);
    }

    // Make sure we include user specified states that were not ALL, QUEUED, ENDED.
    newInstanceState = newInstanceState.concat(instanceStates);

    allQueryObject.instanceState = newInstanceState.toString();

    // Note: The REST API can handle duplicate states in the query string.
    //       For example, "instanceState=DISPATCHED,DISPATCHED" looks to be harmless
    return URL.substring(0 ,startOfQueryParams) + ioQuery.objectToQuery(allQueryObject);
  }

  /**
   * This method is for changing query string 'lastUpdate=' and 'lastUpdateRange=' to 'lastUpdatedTime='.
   * 'lastUpdate=' and 'lastUpdateRange=' is a query string specifically for Admin Center URLs
   * 'lastUpdatedTime=' is the REST API's query string.
   *
   * When using 'lastUpdate=' or 'lastUpdateRange=' in Admin Center, the REST call will use 'lastUpdatedTime='
   *
   * @param URL - URL that must have query strings
   */
  function __handleLastUpdateOrLastUpdateRange(queryString) {
    if(! queryString) {
      return queryString;
    }
    var isLastUpdate = queryString.indexOf('lastUpdate=') > -1;
    var isLastUpdateRange = queryString.indexOf('lastUpdateRange=') > -1;
    if(! isLastUpdate && ! isLastUpdateRange) {
      return queryString;
    }
    var newQueryString;
    if(isLastUpdate) {
      newQueryString = __replaceQueryStringKey(queryString, "lastUpdate", "lastUpdatedTime");
    } else if(isLastUpdateRange) {
      newQueryString = __replaceQueryStringKey(queryString, "lastUpdateRange", "lastUpdatedTime");
    }
    return newQueryString;
  }

  function __changeLastUpdateToLastUpdatedTime(oldQuery) {
    if(! oldQuery) {
      return oldQuery;
    }

    if(oldQuery.indexOf("?") === 0) {
      // Remove the ?.  Add it back in the new query string
      oldQuery = oldQuery.substring(1);
    }

    var newQuery = "?";
    var params = oldQuery.split("&");
    for(var i = 0; i < params.length; i++) {
      if(i !== 0) {
        // We need to add back the & because we striped them through split.
        // Make sure to add only when we are adding 2 or more query params
        newQuery += "&";
      }

      var singleParam = params[i];
      var containsLastUpdate = singleParam.indexOf('lastUpdate=') !== -1;
      if(containsLastUpdate) {
        var lastUpdateKeyAndValue = singleParam.split("=");
        newQuery += ('lastUpdatedTime=' + lastUpdateKeyAndValue[1]); // index 1 is the value
      } else {
        newQuery += singleParam;
      }
    }
    return newQuery;
  }

  /**
   * This method takes in a query string.  It will look for a parameter key
   * and replace it with a specified new key.
   *
   * @param queryString - full query string
   * @param targetKey - key to be replaced
   * @param newKey - key to replace targetKey
   * @param enableUpperCaseValue - optional parameter to upper case the value of the targetKey
   */
  function __replaceQueryStringKey(queryString, targetKey, newKey, enableUpperCaseValue) {
    if(! targetKey || ! newKey || ! queryString) {
      // NOOP since parameters are falsey
      return queryString;
    }

    if(queryString.indexOf("?") === 0) {
      // Remove the ?.  Add it back in the new query string
      queryString = queryString.substring(1);
    }

    var newQuery = "?";
    var params = queryString.split("&");
    for(var i = 0; i < params.length; i++) {
      if(i !== 0) {
        // We need to add back the & because we striped them through split.
        // Make sure to add only when we are adding 2 or more query params
        newQuery += "&";
      }

      var keyindex = 0;
      var valueindex = 1;
      var singleParam = params[i];
      var targetKeyAndValue = singleParam.split("=");
      var containsTargetKey = targetKeyAndValue[keyindex].indexOf(targetKey) !== -1;
      if(containsTargetKey) {
        var value = targetKeyAndValue[valueindex];
        if(enableUpperCaseValue) {
          value = value.toUpperCase();
        }
        newQuery += (newKey + '=' + value);
      } else {
        newQuery += singleParam;
      }
    }
    return newQuery;
  }

  /**
   * This method is to map admin center's query string "jesJobName=" and "jesJobId=" to
   * REST API's query string "jobParameter=jesJobName={name}" and "jobParameter=jesJobId={id}"
   *
   * @param queryString - query string used for the REST APIs
   */
  function __handleJesJobNameAndJesJobId(queryString) {
    if(! queryString) {
      return queryString;
    }
    var newQueryString = queryString;
    var hasJesJobName = newQueryString.indexOf('jesJobName=') !== -1;
    var hasJesJobId = newQueryString.indexOf('jesJobId=') !== -1;
    if(! hasJesJobName && ! hasJesJobId) {
      return newQueryString;
    }

    var requireUpperCaseValue = true;
    if(hasJesJobName) {
      newQueryString = __replaceQueryStringKey(newQueryString, "jesJobName", "jobParameter.com.ibm.ws.batch.submitter.jobName", requireUpperCaseValue);
    }
    if(hasJesJobId) {
      newQueryString = __replaceQueryStringKey(newQueryString, "jesJobId", "jobParameter.com.ibm.ws.batch.submitter.jobId", requireUpperCaseValue);
    }

    return newQueryString;
  }

   function _JOB_INSTANCES_PERSIST_KEY() {
     return 'jobInstancesKey';
   }

  /**
   * Invoked to set the query size for the Job Instance grid on the dashboard view and save it on the server side.
   *
   * @param value     An object containing everything we want to persist for this grid.
   *                  Currently, this would only be the selected query size for the
   *                  Job Instances grid on the landing page (or dashboard).
   *                          value = { querySize: xx }
   */
   function __putJobInstancesQuerySize(value) {
    if (value) {
      var data = {querySize: value};
      userConfig.save(_JOB_INSTANCES_PERSIST_KEY(), json.stringify(data), function(result){
        console.log("Saved job instances data:", value);
      });
    } else {
      userConfig.save(_JOB_INSTANCES_PERSIST_KEY(), null, function(){
        console.log("Failed to save job instances data");
      });
    }
   }

  /**
   * Invoked to get the query size for the Job Instance grid on the dashboard view from the server side.
   *
   * @returns   The JSON object that was persisted for this grid.  Currently it only
   *            holds the query size:
   *                  value = { querySize: xx }
   *            Null is returned if no value has been saved.
   */
   function __getJobInstancesPersistData() {
     var deferred = new Deferred();
     userConfig.init("com.ibm.websphere.appserver.adminCenter.tool.javaBatch");

     userConfig.load(function(result){
       if(result && result[_JOB_INSTANCES_PERSIST_KEY()]){
         var value = result[_JOB_INSTANCES_PERSIST_KEY()];
         deferred.resolve(json.parse(value));
       }
       else{
         console.log("Loaded the persisted data but there is no data for job instances.");
         deferred.resolve(null);
       }
     }, function(err){
       console.log("No job instances data found");
       deferred.resolve(null);
     });
     return deferred;
   }

  /**
   * Reads the persisted data for the Job Instances grid on the dashboard
   * and returns the querySize value, if defined.  Otherwise, returns null.
   */
   function __getJobInstancesQuerySize() {
     var deferred = new Deferred();
     __getJobInstancesPersistData().then(function(data){
       var querySize = null;
       if (data && lang.isObject(data)) {
         if (data.querySize) {
           querySize = parseInt(data.querySize, 10);
           if (isNaN(querySize)) {
             querySize = null;
           }
         }
       }
       deferred.resolve(querySize);
     });
     return deferred;
   }

   /**
    * @param URL - String with numbered substitution values, starting at 0.
    * @param parameterValues - Array of string values to insert into the URL string.
    */
   function __getWithParms(URL, parameterValues) {
     var url = lang.replace(URL, parameterValues);
     return __get(url);
   }

  /**
   * This method will prepare the GET URL.  There is not a direct one-to-one mapping
   * of query strings used by admin center URLs and REST API URls.  This method is
   * to handle the mapping, if needed.
   *
   * @param URL - URL without the query string
   * @param queryString - optional.  Can start with or without ? character
   */
   function __get(URL, queryString) {
     if(! queryString) {
       // Make a GET call without query strings
       return __invokeGet(URL);
     } else {
       // Make a GET call with query strings

       // Special transformation that maps Admin Center query params to REST API query params
       queryString = __handleLastUpdateOrLastUpdateRange(queryString);
       queryString = __handleJesJobNameAndJesJobId(queryString);

       return __invokeGet(URL + queryString);
     }
   }

    /**
     * Function used to get the tool data for the authorized user.
     *
     * @param URL - Request to execute
     *
     * @returns return deferred.
     */
    function __invokeGet(URL) {
      var options = {
        handleAs : "text",
        withCredentials : true // Used in CORS environment
      };

      // FIXME: This needs to be rewritten to work only with a query string rather
      // than a full URL
      URL = __handleInstanceStates(URL);

      __convertJesValuesToUpperCase();

      var promise = request.get(URL, options);

      var deferred = new Deferred(function cancelXHRDeferred(reason) {
        promise.cancel(reason);
      });

      promise.response.then(function(response) {
        deferred.resolve(response, true);
      }, function(err) {
        deferred.reject(err, true);
      }, function(evt) {
        deferred.progress(evt, true);
      });
      return deferred;
    }

   /**
     * @param URL - String with numbered substitution values, starting at 0.
     * @param parameterValues - Array of string values to insert into the URL string.
     * @param dataValue - Data to submit with put request; null if no data.
     */
    function __put(URL, parameterValues, dataValue) {
      var url = lang.replace(URL, parameterValues);
      return __invokePut(url, dataValue);
    }

    /**
     * Function used to execute an action for the authorized user.
     *
     * @param URL - Request to execute
     * @param dataValue - data to submit with PUT request
     *
     * @returns return deferred.
     */
    function __invokePut(URL, dataValue) {
      if (!dataValue) {
        dataValue = "";
      }

      var options = {
          handleAs : 'json',
          headers : {
            'Content-type' : 'application/json'
          },
          withCredentials: true,   // Used in CORS environment
          data: dataValue
      };

      var promise = request.put(URL, options);
      var deferred = new Deferred(function cancelXHRDeferred(reason) {
        promise.cancel(reason);
      });

      promise.response.then(function(response) {
        deferred.resolve(response, true);
      }, function(err) {
        deferred.reject(err, true);
      }, function(evt) {
        deferred.progress(evt, true);
      });
      return deferred;
    }

    /**
     * @param URL - String with numbered substitution values, starting at 0.
     * @param parameterValues - Array of string values to insert into the URL string.
     */
    function __deleteWithParms(URL, parameterValues) {
      var url = lang.replace(URL, parameterValues);
      return __delete(url);
    }

    /**
     * Function used to execute an action for the authorized user.
     *
     * @returns return deferred.
     */
    function __delete(URL) {
      var options = {
        handleAs : "application/json",
        withCredentials : true // Used in CORS environment
      };

      var promise = request.del(URL, options);

      var deferred = new Deferred(function cancelXHRDeferred(reason) {
        promise.cancel(reason);
      });

      promise.response.then(function(response) {
        deferred.resolve(response, true);
      }, function(err) {
        deferred.reject(err, true);
      }, function(evt) {
        deferred.progress(evt, true);
      });
      return deferred;
    }

    /**
    * Return persisted data from server side.  If no data exists return null.
    */
    function getPersistedData() {
      var deferred = new Deferred();
      // Initiate the persistence configuration
      userConfig.init("com.ibm.websphere.appserver.adminCenter.tool.javaBatch");
      userConfig.load(
        function(response) {
          deferred.resolve(response);
        }, function(error) {
          deferred.resolve(null);
        }
      );
      return deferred;
    } // end of getPersistedData

    /**
     * A method to enforce that the values for jes job name and jes job id are
     * displayed in upper case characters.
     */
    function __convertJesValuesToUpperCase() {
      var originalHash = hashUtils.getQueryParams();
      if(! originalHash) {
        return;
      }

      var all_parameters = originalHash.split('&');
      var newBrowserQueryString = '?';
      for(var i = 0; i < all_parameters.length; i++) {
        var currentKeyAndValue = all_parameters[i].split('=');
        var currentKey = currentKeyAndValue[0];
        var currentValue = currentKeyAndValue[1];

        if(newBrowserQueryString !== '?') {
          // More than one query parameter, so make sure to add back
          // the & character that was removed from the .split('&')
          newBrowserQueryString += '&';
        }

        if(currentKey === 'jesJobName' || currentKey === 'jesJobId') {
          currentValue = currentValue.toUpperCase();
        }

        newBrowserQueryString += (currentKey + "=" + currentValue);

      } // end of for loop

      hashUtils.replaceQueryParams(newBrowserQueryString);
    }
});

var base_uri = '/ibm/api/batch';
var base_uri_v2 = base_uri + '/v2';
var base_uri_v4 = base_uri + '/v4';
var v4JobInstances = base_uri_v4 + '/jobinstances';

var url = base_uri_v2 + '/jobinstances';
var jobExecutionsURL = base_uri + '/jobinstances/{0}/jobexecutions';   // 0 - job instance ID
var executionDetailURL = base_uri + "/jobexecutions/{0}";     // 0 - execution ID
var jobInstanceDetailURL = base_uri + "/jobinstances/{0}";    // 0 - job instance ID
var executionStepsDetailURL = base_uri + "/jobexecutions/{0}/stepexecutions";  // 0 - execution ID
var jobInstanceLogsURL = base_uri + "/jobinstances/{0}/joblogs"; // 0 - jobinstance ID
var jobExecutionLogsURL = base_uri + "/jobexecutions/{0}/joblogs"; // 0 - jobexecution ID
var jobInstanceRestartURL = base_uri + "/jobinstances/{0}" + "?action=restart&reusePreviousParams=true"; // 0 - job instance ID
var jobInstanceRestartParmsURL = base_uri + "/jobinstances/{0}" + "?action=restart&reusePreviousParams=false"; // 0 - job instance ID
var jobInstanceStopURL = jobInstanceDetailURL + "?action=stop";
var jobInstancePurgeURL = base_uri_v2 + "/jobinstances?jobInstanceId={0}&purgeJobStoreOnly={1}";  //0 - jobinstance ID
                                                                                                  //1 - true/false

var jobInstancesDefaultQuerySize = 100;
