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
 * Shared logging and serviceability module. Any custom logging for Admin Center, such as FFDC,
 * conditional debugging, etc should be added here.
 */
define([], function() {

  'use strict';

  // Begin module initialization
  // We will determine if we're debugging once and only once but for
  // most cases this is sufficient
  var debugEnabled = false;

  var field = 'debug';
  var url = window.location.href;
  if(url.indexOf('?' + field + '=') !== -1) {
    debugEnabled = true;
  } else if(url.indexOf('&' + field + '=') !== -1) {
    debugEnabled = true;
  }
  
  if (debugEnabled) {
    console.log('Detailed debugging is enabled!');
  } else {
    console.log('Detailed debugging is not enabled. To enable, specify query parameter debug=true');  
  }
  // End module initialization

  return {

    /**
     * Throws the exception message and captures serviceability information.
     * 
     * This common method should be used over the native throw method to ensure proper logging
     * of the error condition.
     * 
     * @param {string} msg The exception message to throw
     */
    throwMsg: function(msg) {
      console.error(msg);
      throw msg;
    },

    /**
     * Logs an exception caught in a try / catch block. No assumption is made about what type
     * of object the exception is, and this method will do the best it can to log the exception
     * in a meaningful way.
     * 
     * This common method should always be used when processing a try / catch, especially when
     * the try / catch is an unexpected error path.
     * 
     * If arguments are specified after the exception, they will be logged to provide additional context.
     * 
     * @param {string} msg The message which should explain what action was occuring at the time the exception was caught 
     * @param {object} e The exception caught in the try / catch block
     */
    ffdc: function(msg, e) {
      console.error('FFDC Capture - Message: ' + msg);
      console.error('----------------------------------');
      console.error('Exception details:');
      console.error('toString: ' + e.toString());
      console.error('object: ' + e);
      console.error('JSON: ' + JSON.stringify(e));
      console.error('----------------------------------');
      if (arguments.length > 2) {
        console.error('Additional call context:');
        for (var i = 2; i < arguments.length; i++) {
          var contextObj = arguments[i];
          console.error('Context Object: ' + (i - 2));
          console.error('toString: ' + contextObj.toString());
          console.error('object: ' + contextObj);
          console.error('JSON: ' + JSON.stringify(contextObj));
          console.error('----------------------------------');
        }
      }
      console.error('End FFDC Capture');
    },

    /**
     * Logs a detailed console.log message. The message will only be logged if
     * the debug query parameter is specified.
     * 
     * @param {string} msg The message to log
     */
    debug: function(msg) {
      if (debugEnabled) {
        console.log(msg);  
      }
    }

  };

});