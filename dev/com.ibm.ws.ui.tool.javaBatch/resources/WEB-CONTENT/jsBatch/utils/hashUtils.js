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

define([ 'dojo/Deferred', 'dojo/hash', 'jsBatch/utils/ID' ], function hashUtils(Deferred, hash, ID) {
  "use strict";
  window.location.hash = window.top.location.hash;

  var toolId;
  
  function startsWith(string, prefix) {
    return ((typeof string === 'string') && (string.indexOf(prefix) === 0));
  }

  return {
    
    getToolId : function() {
      toolId = ID.JAVA_BATCH;
      return toolId;
    },

    /**
     * Returns the current hash. Unfortunately, cannot trust hash() because tools opened through the toolbox open in an iframe, and dojo's
     * hash() is then limited to that iframe. TODO: consider copying dojo's hash implementation, but make it work with window.top instead?
     * 
     * @returns {String} currentHash - The current hash as seen in the URL.
     * @memberOf resources.hashUtils
     * @function
     * @public
     */
    getCurrentHash : function() {
      // It seems hash() returns an empty string when in an iframe, so instead we have to use core js
      // to get at the hash since the toolbox opens tools in an iframe.  I don't like this as it doesn't
      // seem safe...worth investigating not opening tools in iframes?
      if (window.top.location.hash) {
        return window.top.location.hash.substring(1);
      } else {
        return toolId;
      }
    },
    
    /**
     * Returns just the string of query parameters in the current hash.
     * 
     * @returns  Null if none are set; otherwise the string of query parameters
     *           in the URL following the '?'
     */
    getQueryParams : function() {
      var queryParams = null;
      var hash = this.getCurrentHash();
      if(hash.indexOf('?') > -1) {
        queryParams = hash.substring(hash.indexOf('?') + 1);  // Don't return the '?'
      }
      return queryParams;
    },

    /**
     * Returns a "HashMap" of query parameters and their values.
     * If no parameter is passed into this method, get the current 
     * query string, otherwise convert the parameter.
     * 
     * @returns Object  Returns an object containing the query parameters
     *                  in the current hash and their values.
     */
    getQueryParamHash : function(queryParamsString) {
      if(! queryParamsString) {
        queryParamsString = this.getQueryParams();   // Returns null if there are none
      }

      var queryParamsHashMap = {};

      if (queryParamsString) {        
        // Split up the query parameters
        var queryParams = queryParamsString.split("&");

        for(var i = 0; i < queryParams.length; i++) {
          var param = queryParams[i];
          param = param.split("=");
          var paramKey = param[0];
          var paramValue = param[1];
          if(queryParamsHashMap[paramKey]) {
            queryParamsHashMap[paramKey] += "," + paramValue;
          } else {
            queryParamsHashMap[paramKey] = paramValue; 
          }
        }
      }

      return queryParamsHashMap;
    },

    /**
    * Returns a query string where only the values are encoded.
    * For example: 
    * ?submitter=#&jobName=bob turns into ?submitter=%23&jobName=bob
    */
    encodeQueryParameterValuesOnly : function(queryString) {
      if(! queryString) {
        return queryString;
      }

      var newEncodedQueryString = "";

      var keyAndValues = queryString.split("&");
      for(var b = 0; b < keyAndValues.length; b++) {
        if(b > 0) {
          newEncodedQueryString += "&";
        }
        var temp = keyAndValues[b];
        temp = temp.split("=");
        newEncodedQueryString += temp[0] + '=' + encodeURIComponent(temp[1]);
      }

      return newEncodedQueryString;
    },
    
    /**
     * Converts a column id value to its query parameter string.
     * 
     * @param   id       Column id
     * @returns String   String representing this column as a query parameter.  
     */
    getColumnIdQueryParam: function(id) {
      switch (id) {
        case 'instanceId': 
          return ID.JOB_INSTANCE;
        case 'JESJobName':
          return 'jesJobName';
        case 'JESJobId':
          return 'jesJobId';
        default:
          return id;
      }
    },

    /**
    * This method will handle when a query string needs to be a list of comma seperated values
    * or a series of repeated key=value pairs 
    */
    convertListToMultipleKeys : function(filterType, filterValueList) {
      if(filterValueList.indexOf(",") === -1) {
        return filterType + "=" + filterValueList;
      }

      var queryString = "";
      var values = filterValueList.split(",");
      for(var i = 0; i < values.length; i++) {
        if(queryString.length > 0) {
          queryString += "&";
        }
        queryString += filterType + "=" + values[i];
      }
      return queryString;
    },
    
    /**
     * Replaces the query string in the browser's url with a new query string
     * @param newQueryParams
     */
    replaceQueryParams : function(newQueryParams) {
      if(newQueryParams[0] !== '?') {
        newQueryParams = '?' + newQueryParams;
      }
      
      var originalHash = window.top.location.hash;
      var questionMarkIndex = originalHash.indexOf('?');
      if(questionMarkIndex === -1) {
        window.top.location.hash = originalHash + newQueryParams;
        return;
      }
      
      var newUrl = window.top.location.protocol + "//" + window.top.location.host + window.top.location.pathname + originalHash.substring(0, questionMarkIndex) + newQueryParams;
      window.top.location.replace(newUrl);
    },
    
    /**
     * Remove the hash from the browser's URL
     */
    removeHash : function() {
      // Remove the hash from the browser URL to take users back to the toolbox
      var index =  window.top.location.href.indexOf("#");
      if (index !== -1) {
        var newUrl = window.top.location.href.substring(0, index);
        window.top.location.replace(newUrl);
      }
    }
    
  };
});