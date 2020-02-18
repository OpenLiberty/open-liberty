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
 * This hashUtil module is intended to ensure that all of our URL hash construction and interpretation is normalized and common.
 * 
 * Collections and their hash: <code> 
 * /explore-1.0/servers - servers 
 * /explore-1.0/clusters - clusters 
 * /explore-1.0/applications - applications
 * /explore-1.0/hosts - hosts
 * 
 * Objects and their hash: 
 * /explore-1.0/servers/serverTuple - server 
 * /explore-1.0/clusters/clusterName - cluster
 * /explore-1.0/clusters/clusterName/apps/appName - clustered application 
 * /explore-1.0/servers/serverTuple/apps/appName - application instance
 * /explore-1.0/hosts/hostName - host</code>
 * 
 * Any hash returned by this module URL encodes the server tuple. There is support for interpreting a hash value which contains an unencoded
 * tuple.
 * 
 * @author Michal Broz <mbroz@us.ibm.com>
 */

define([ 'dojo/Deferred', 'dojo/hash', 'jsExplore/utils/ID' ], function hashUtils(Deferred, hash, ID) {

  // This sets the iframe's hash to be that of the top window's (which contains the URL).
  // Moved this out of maindashboard-init because it actually gets resolved here first due to dojo's loader.
  window.location.hash = window.top.location.hash;

  var toolId;

  return {

    /**
     * Extracts the toolID from the hash portion of the URL. If no hash is present, default to 'explore.'
     * 
     * @returns {String} toolID
     * @memberOf resources.hashUtils
     * @function
     * @public
     */
    getToolId : function() {
      if (toolId) {
        return toolId;
      } else {
        var toolHash = hash();
        var slashPos = toolHash.indexOf('/');
        if (slashPos > 0) {
          toolId = toolHash.substring(0, slashPos);
        } else if (toolHash) {
          toolId = toolHash;
        } else {
          toolId = ID.getExplore();
        }
        return toolId;
      }
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
     * For ease of unittesting this module!
     */
    __setCurrentHash : function(mockedHash) {
    	window.top.location.hash = mockedHash;
    }
  };
});