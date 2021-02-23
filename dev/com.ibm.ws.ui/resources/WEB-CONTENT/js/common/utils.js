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
 * This Javascript file contains common used utility functions.
 * 
 * 
 * @module common/utils
 */
define(['dojo/has'], function(has) {
  'use strict';

  return {
    /**
     * Determines the width of a giving string with specified font.
     * 
     * @method
     * @return {number} the string width in pixels.
     */
  getTextWidth : function(text, fontSize, weight, family) {
      var testDiv = document.createElement('lDiv');
      document.body.appendChild(testDiv);
      if (weight !== null) {
        testDiv.fontWeight = weight;
      }
      else {
        testDiv.fontWeight = "normal";
      }
      
      if (family !== null) {
        testDiv.fontFamily = family;
      }
      testDiv.style.fontSize = fontSize;
      testDiv.style.position = "absolute";
      testDiv.style.left = -10000;
      testDiv.style.top = -10000;
      testDiv.innerHTML = text;
  
      var result = testDiv.clientWidth;
      document.body.removeChild(testDiv);
      testDiv = null;
  
      return result;
    }
  };

});
