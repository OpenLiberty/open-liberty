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
 * This Javascript file contains a number of Utility functions that should be
 * useful to a number of different areas of the UI javascript runtime.
 */

define(['dojo/request/xhr',
        'dojo/Deferred',
        'dojox/validate/web',
        'dojo/json',
        'dojo/i18n!./nls/catalogMessages',
        'js/common/tr'
     ], function(xhr, Deferred, web, JSON, i18n, tr) {
    'use strict';

    return {

        /**
         * Function to determine whether tool URL uses either 
         * HTTP or HTTPS scheme. Rejects anything else.
         * 
         * @param (String) url: The URL to validate
         * @returns (Boolean) Whether URL uses HTTP or HTTPS 
         */
        isValidUrl: function(url) {

            // RFC 1738 describes URLs as using all sorts of schemes besides
            // http or https, but the UI will only allow web URLs. Use the 
            // built-in Dojo isUrl() to do this validation work for us.
            // Note that URLs don't need to explicitly start with http:// or https://
            // Allowable URL examples:
            // - http://nytimes.com
            // - https://washingtonpost.com
            // - www.wallstreetjournal.com
            // - google.com
            // Not valid:
            // - file:///C:/wendy/helloworld.txt
            // - mailto:wraschke@us.ibm.com
            // - //yahoo.com
            //console.log("validate:" + web.isUrl("http://he.wikipedia.org/wiki/%D7%9E%D7%9C%D7%97_%28%D7%9B%D7%99%D7%9E%D7%99%D7%94%29"));
            if (url)  {
                try{
                  // decode first in case they pasted something already encoded and then encode it
                  var decodeUrl = decodeURI(url);
                  // truncate anything starting with '#'
                  var hashIndex = decodeUrl.indexOf("#");
                  if (hashIndex !== -1) {
                    decodeUrl = decodeURI(decodeUrl.substring(0, hashIndex));
                  }
                  var valid = web.isUrl(encodeURI(decodeUrl), {allowLocal: true});
                  if (valid) {
                      console.log("url " + url + " is valid");
                      return true;
                  }
                } catch (e){
                    return false;
                }
            }
            return false;
        },

        /**
         * Function to determine if new tool URL is accessible
         * @param (String) url: The URL to access
         * @returns (Boolean) true if status code 200, false otherwise
         */
        isUrlAccessible: function(targetURL) {
            if (!targetURL) {
              tr.throwMsg(i18n.ERROR_URLACCESS_NOURL);
            }
            var url = '/ibm/api/adminCenter/v1/utils/url/getStatus?url=' + decodeURI(targetURL);
            var options = { handleAs: 'json', timeout: 2000 };
            var xhrDef = xhr.get(url, options);

            // Establish the Deferred to be returned.
            // This allows the caller to cancel the underlying XHR request.
            var deferred = new Deferred(function cancelXHRDeferred(reason) {
                xhrDef.cancel(reason);
            });

            xhrDef.then(function(response) {
                if (response.status === 200) {
                    deferred.resolve(true, true);
                } else {
                    deferred.resolve(false, true);
                }
            }, function(err) {
                deferred.reject(err, true);
            }, function(evt) {
                deferred.progress(evt, true);
            });

            return deferred;
        },

        /**
         * Function used to analyze the metadata from a tool URL.
         * 
         * @returns An object properties containing the Tool fields that have been found in the URL, such
         *          as name, and description.
         */
        analyzeURL: function(targetURL) {
            if (!targetURL) {
              tr.throwMsg(i18n.ERROR_URLANALYZE_NOURL);
            }
            var url = '/ibm/api/adminCenter/v1/utils/url/getTool?url=' + targetURL;
            var options = { handleAs: 'json' };
            var xhrDef = xhr.get(url, options);

            // Establish the Deferred to be returned.
            // This allows the caller to cancel the underlying XHR request.
            var deferred = new Deferred(function cancelXHRDeferred(reason) {
                xhrDef.cancel(reason);
            });

            xhrDef.then(function(toolStatus) {
                console.log('Was the URL to convert to a tool reachable? ' + toolStatus.urlReachable);
                deferred.resolve(toolStatus.tool, true);
            }, function(err) {
                deferred.reject(err, true);
            }, function(evt) {
                deferred.progress(evt, true);
            });

            return deferred;
        }

    };
});