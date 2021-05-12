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
 * This Javascript file contains all of the utility functions needed around platform detection.
 * <p>
 * Currently, that amounts to detecting if we're on a phone.
 * 
 * @module common/platform
 */
define(['dojo/has'], function(has) {
  'use strict';

  return {
    
    /**
     * Determines if the current platform is a phone.
     * 
     * @method
     * @return {boolean} true if the platform should be treated as a phone, false otherwise.
     */
    isPhone: function() {
      // Kind of an insufficient check - we'll probably want to improve this
      var isIOS = has('ios');
      var isAndroid =  has('android');
      var isBB = has('bb');
      var deviceWidth = has('device-width');
//      console.log("ios: " + isIOS + ", android: " + isAndroid + ",blackberry: " + isBB);
//      console.log("device-width: " + deviceWidth);
      return ((isIOS || isAndroid || isBB) && deviceWidth <= 480);
    },

    /**
     * Determines if the current platform is a desktop/laptop computer.
     * 
     * @method
     * @return {boolean} true if the platform should be treated as a phone, false otherwise.
     */
    isDesktop: function() {
      // Kind of an insufficient check - we'll probably want to improve this
      var isIOS = has('ios');
      var isAndroid =  has('android');
      var isBB = has('bb');
      return (!isIOS && !isAndroid && !isBB);
    },

    getDeviceCSSPrefix: function() {
        if ( this.isPhone() )  {
            return " mobileExplore ";
        }
        else {
            return "";
        }
    }
  };

});
