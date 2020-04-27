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
/* jshint strict: false */
define([
        'dojo/_base/lang',
        'dojo/json',
        'dojo/request/xhr',
        'jsExplore/resources/stats/_mbeanUtils',
        'dojo/NodeList-manipulate'
        ], function(
            lang,
            JSON,
            xhr,
            mbeanUtils
        ){
  
  var __xhrOptions= {
      handleAs : "text",
      preventCache : true,
      sync : false,
      headers : {
        "Content-type" : " text/html; charset=UTF-8"
      }
  };
  var logAnalyticsEnabled = null;
  var logAnalyticsURL = null;
  
  /**
   * This method returns a boolean indicating whether or not the Log Analytics are enabled or not.     * 
   * By passing null as the server to mbeanUtils, the call will be made to the controller which is where the 
   * AdminCenter is running.
   */
  var __isLogAnalyticsEnabled = function(resource) {
// DISABLE_ANALYTICS
    return false;
//    if (logAnalyticsEnabled !== null) {
//      return logAnalyticsEnabled;
//    }
//    logAnalyticsEnabled = false;
//    if (resource.type === 'server' || resource.type === 'standaloneServer') {
//      mbeanUtils.isLibertyFeatureActive(resource, "analyticsCollector-1.0").then(function(response) {
//        if (response === true) {
//          logAnalyticsEnabled = true;
//        } 
//      });
//    } else if (resource.type === 'cluster' || resource.type === 'host') {
//      // TODO: hard code for now
//      logAnalyticsEnabled = true;
//    }
//    console.log("logAnalyticsEnabled: " + logAnalyticsEnabled);
//    return logAnalyticsEnabled;
  };

  /**
   * This method returns all the pipes that are in the Analytics Server
   * It returns a Deferred object that when complete returns an array of all the available pipes. 
   */
  var __getAllAvailablePipes = function() {
    // TODO: need to add server name(s) to options
    return xhr.get(__getLogAnalyticsURL(), __xhrOptions).then(lang.hitch(this, function(response) {
    // TODO: exact format of return with server name needs to be defined
//      var responseArray = JSON.parse('[{"id":"access_count"}, {"id":"messages_count"}, {"id":"ffdc_count"}, {"id":"trace_count"}]');
      var responseArray = JSON.parse(response);
      return responseArray.pipes;
    }), function(err) {
      console.log(err); 
    });
  };

  /**
   * This method returns all the keys for the specified pipe
   * It returns a Deferred object that when complete returns an array of all the keys. 
   */
  var __getPipeKeys = function(pipe) {
    return xhr.get(__getLogAnalyticsURL() + pipe + "?init=true", __xhrOptions).then(lang.hitch(this, function(response) {
      var keys = [];
      try {
        var responseJSON = JSON.parse(response);
        // TODO: not sure whether we want axes or labels
        if (responseJSON.axes && responseJSON.axes[1]) {
          console.log("yaxis values: " + responseJSON.axes[1]);
          keys = responseJSON.axes[1];
        }
      } catch (e) {
        // just let it fall through and return []
      }
      return keys;
    }), function(err) {
      console.log(err); 
    });
  };

  /**
   * This method returns the url to the analytics engine.
   */
  var __getLogAnalyticsURL = function() {
    if (logAnalyticsURL !== null) {
      return logAnalyticsURL;
    }
    // TODO: Hard code for now
    // TODO: if it isn't set yet, might make sense to add the logic to __isLogAnalyticsEnabled and just call that here
    logAnalyticsURL = "/ibm/api/analytics/v1/insights/";
    return logAnalyticsURL;
  }

  return {
    isLogAnalyticsEnabled: __isLogAnalyticsEnabled,
    getAllAvailablePipes: __getAllAvailablePipes,
    getPipeKeys: __getPipeKeys,
    getLogAnalyticsURL : __getLogAnalyticsURL,
    getLogAnalyticsXHROptions: function() { return __xhrOptions;}
  };
});