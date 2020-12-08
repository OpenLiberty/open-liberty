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
 * This JavaScript file contains all of the logic to capture, retrieve and clear the URL hash
 * to and from a cookie.
 */
define(['dojo/cookie', 'dojo/hash', 'dojo/topic'], function(cookie, hash, topic) {
  'use strict';

  var listenerHandle = null;

  /**
   * Creates and sets a cookie to the browser with the hash value.
   * The cookie is only set if there is a hash value. Lack of a hash
   * should not override the hash cookie if set. To clear the hash
   * cookie, an explicit invocation to clearHashCookie() should be made.
   * 
   * @param hashValue The hash value to store in the cookie.
   */
  function setHashCookie(hashValue) {
    if (hashValue) {
      cookie('adminCenterHash', hashValue, {secure: true });
    }
  }

  /**
   * Gets the value stored in the hash cookie. If the hash cookie is not present,
   * this will return null / undefined.
   * 
   * @return The value stored in the hash cookie. May be null / undefined.
   */
  function getHashCookieValue() {
    return cookie('adminCenterHash');
  }

  /**
   * Removes the hash cookie from the browser.
   */
  function clearHashCookie() {
    cookie('adminCenterHash', '', {secure: true, expires: -1 });
  }

  /**
   * Captures the current hash value and stores it to the hash cookie.
   * This method also establishes a hash change listener, on the off-change that
   * prior to logging in the hash value is changed. If the hash is changed, the
   * cookie is updated.
   */
  function __captureHashCookie() {
    setHashCookie(hash());

    // If we've not been called before, then establish a listener. Otherwise,
    // don't set it up again.
    if (!listenerHandle) {
      listenerHandle = topic.subscribe("/dojo/hashchange", function(changedHash){
        setHashCookie(changedHash);
      });
    }
  }

  /**
   * Stops capturing the hash. This is clean up which should be called when capturing
   * is no longer necessary.
   */
  function __stopCapturing() {
    if (listenerHandle) {
      listenerHandle.remove();
      listenerHandle = null;
    }
  }

  /**
   * Restores the hash from the cookie. This method invalidates the cookie so that it can
   * not be reused.
   */
  function __restoreHashFromCookie() {
    var cookieHashValue = getHashCookieValue();
    if (cookieHashValue) {
      hash(cookieHashValue);
    }
    clearHashCookie();
  }

  return {
    captureHashCookie: __captureHashCookie,
    stopCapturing: __stopCapturing,
    restoreHashFromCookie: __restoreHashFromCookie
  };

});
