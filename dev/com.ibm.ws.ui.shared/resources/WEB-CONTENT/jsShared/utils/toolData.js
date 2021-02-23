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
var toolData = (function() {
    "use strict";
    
    var __baseurl = "/ibm/api/adminCenter/v1/tooldata";
    var __toolName = "";
    var __url = "";
    var etag = "";
    var tooldataAsString = "";
    var tooldata = {};
    
    /**
     * Constructor
     */
    var createToolData = function(toolName) {
        if (!toolName) { 
            this._throwError('Tooldata created without tool name.'); 
        }
        __toolName = toolName;
        __url = __baseurl + "/" + __toolName;
        
        return this;
    };
    
    /**
     * Resets tool data and etag.
     */
    var _clearData = function () {
        tooldataAsString = "";
        tooldata = {};
        etag = "";
    };
    
    /**
     * Sets the tool data object and tool data string, also save etag for update (PUT) use.
     */
    var _setData = function (response, eTag) {
        tooldataAsString = response;
        tooldata = JSON.parse(response); 
        etag = eTag;       
    };
    
    /**
     * Gets the tooldata as string.
     */
    var getTooldataAsString = function () {
        return tooldataAsString;
    };
    
    /**
     * Gets the tooldata as javascript object.
     */
    var getTooldataAsObject = function () {
        return tooldata;
    };
    
    /**
     * Function used to get the tooldata for the authorized user.
     * 
     * @returns return deferred.
     */
    var get = function(resolve, reject) { 
    	
    		var req = new XMLHttpRequest();
            req.onload = function() {
              if(req.status === 200 && req.responseText){
                _setData(req.responseText, req.getResponseHeader('ETag')); 
                resolve(req.responseText);
              }
              else{
                reject(req);
              }
            };
            req.onerror = function() {
            	console.error("error get() ", req.status);
            	reject(req);
            };

            req.open("GET", __url);
            req.send();
    };

    /**
     * Function used to delete the tooldata for the authorized user.
     * 
     * @return return deferred.
     */
    var del = function(resolve, reject) {
      
    		var req = new XMLHttpRequest();
        req.onload = function() {
        	if(req.responseText){
        		_clearData(req.responseText);
        	}            	
        	if(resolve){
        	  resolve(req.responseText);
        	}
        };
        req.onerror = function() {
        	console.log("fail del() ", req.statusText);
        	if(reject){
        	  reject();
        	}
        };
  
        req.open("DELETE", __url);
        req.send();
    };

    /**
     * Function used to post the tooldata for the authorized user.
     * 
     * @return return deferred.
     */

    var post = function(object, resolve, reject) {
    	
    	var objectString = JSON.stringify(object);
    	
    		var req = new XMLHttpRequest();
        req.onload = function() {
        	if(req.responseText){
        		_setData(req.responseText, req.getResponseHeader('ETag'));
        		if(resolve){
              resolve(req.responseText);
            }
        	}            	
        	else{
        	  if(reject){
        	    reject(req);
        	  }
        	}
        	
        };
        req.onerror = function() {
        	console.log("fail post() ", req.statusText);
        	if(reject){
        	  reject();
        	}
        };

        req.open("POST", __url);
        req.setRequestHeader('Content-Type', 'text/plain');
        req.send(objectString);
    };

    /**
     * Function used to update the tooldata for the authorized user.
     * 
     * @return return deferred.
     */

    var put = function(object, resolve, reject) {

        var objectString = JSON.stringify(object);
        
    		var req = new XMLHttpRequest();
        req.onload = function() {
        	if(req.status == 200 && req.responseText){
        		_setData(req.responseText, req.getResponseHeader('ETag'));
        		if(resolve){
        		  resolve(req.responseText);
        		}
        	}            	
        	else{
        	  reject(req);
        	}
        };
        req.onerror = function() {
        	console.log("fail put() ", req.statusText);
        	if(reject){
        	  reject();
        	}
        };
        
        req.open("PUT", __url, true);
        req.setRequestHeader('Content-Type', 'text/plain');
        req.setRequestHeader('If-Match', etag);
        req.send(objectString);
    };

    return {
        createToolData: createToolData,
        getTooldataAsString: getTooldataAsString,
        getTooldataAsObject: getTooldataAsObject,
        get: get,
        del: del,
        put: put,
        post: post
    };
    
})();

//This code was lifted from the d3 library.  This logic allows pure javascript to be loaded using html <script> or by AMD loading
if (typeof define === "function" && define.amd) {
  define(this.toolData = toolData); 
} else if (typeof module === "object" && module.exports) { 
  module.exports = toolData; 
}