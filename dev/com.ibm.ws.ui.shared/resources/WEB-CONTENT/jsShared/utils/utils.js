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
/*****************************************************************************
  * module:
  *   jsShared/utils/utils
  * summary:
  *   A Util class that contains a list of common tool utility functions
  *
  * @return {Object} Containing all the utils methods
  *
  ****************************************************************************/
define([ "jsShared/utils/_mbeanUtils", 
         "dojo/Deferred", "dojo/dom-construct", "dojo/has", 
         "dojo/request", "dojo/request/xhr",  
         "dijit/_Widget"],
/**
 * @exports resources/utils
 */
function(mbeanUtils, Deferred, domConstruct, has, 
         request, xhr, Widget) {
  var global = this;
  var __baseTextDirection = null;
  var __textDirectionWidget = null;
  var __scrollBarWidth = null;
  var __scrollBarHeight = null;

  return {

      /**
       * Check for the availability of one of the collective mbeans that we need like
       * WebSphere:feature=collectiveController,type=CollectiveRepository,name=CollectiveRepository
       *
       * @method
       * @returns {boolean} isStandalone - True if the server is a standalone server. False otherwise
       */
      isStandalone : __isStandalone,
      getStandaloneServerInfo: __getStandaloneServerInfo,
      checkIfToolboxExists: __checkIfToolboxExists,
      checkIfExploreToolExists: __checkIfExploreToolExists,
      getBidiTextDirectionSetting : __getBidiTextDirectionSetting,
      getStringTextDirection : __getStringTextDirection,
      getScrollBarWidth : __getScrollBarWidth,
      getScrollBarHeight : __getScrollBarHeight,
      domToString : __domToString,
      toCamelCase : __toCamelCase,
      camelToSnake : __camelToSnake
  };

  /**
   * Check for the availability of one of the collective mbeans that we need like
   * WebSphere:feature=collectiveController,type=CollectiveRepository,name=CollectiveRepository
   *
   * Synchronous call.
   * 
   * @returns {boolean} isStandalone - True if the server is a standalone server. False otherwise
   */
  function __isStandalone() {
    if (global.isStandalone === undefined) {

      mbeanUtils.getMBeans(null, "WebSphere:feature=collectiveController,type=CollectiveRepository,name=CollectiveRepository", true).then(
          function(response) {
            if (response.length > 0) {
              global.isStandalone = false;
            } else {
              global.isStandalone = true;
            }
          }, function(err) {
            global.isStandalone = false;
          });

    }
    return global.isStandalone;
  }
  
  /**
   * To get the server identity information (name, user directory, and host) of the Standalone Server.
   * 
   * @returns  Deferred
   *           When resolved, returns a serverObj = {name: server, userdir: userdir, host: hostname }
   *           for the standalone server.
   *           
   *  Note: from jsExplore/resources/_util.js           
   */
  function __getStandaloneServerInfo() {
    var deferred = new Deferred();

    /* Response: [{"name":"Name","value":{"value":"uiDev","type":"java.lang.String"}},{"name":"DefaultHostname","value":{"value":"localhost","type":"java.lang.String"}},{"name":"UserDirectory","value":{"value":"C:/sandbox/workspace.libertyDev/build.image/wlp/usr/","type":"java.lang.String"}}] */
    var url = '/IBMJMXConnectorREST/mbeans/WebSphere%3Afeature%3Dkernel%2Cname%3DServerInfo/attributes';
    var options = { handleAs : 'json', preventCache : true };
    request.get(url, options).then(function(attributes) {
      var hostname = "TBD";
      var userdir = "TBD";
      var server = "TBD";

      for(var i = 0; i < attributes.length; i++) {
          var attrObj = attributes[i];
          if (attrObj.name === "Name") {
            server = attrObj.value.value;
          } else if (attrObj.name === "UserDirectory") {
            userdir = attrObj.value.value.substr(0, attrObj.value.value.length-1); // Need to drop trailing slash
          } else if (attrObj.name === "DefaultHostname") {
            hostname = attrObj.value.value;
          } else {
            console.log("WebSphere:name=ServerIdentity had an unrecognized attribute: " + attrObj.name);
          };
      }

      var serverInfo = {
            name: server,
            userdir: userdir,
            host: hostname
      };

      deferred.resolve(serverInfo, true);
    }, function(err) {
      __processError(url, err);
      deferred.reject(err, true);
    });

    return deferred;
  }
  
  /**
   * 
   * @param url
   * @param error
   * 
   * @returns  Updates error object with errMsg field.
   * 
   *  Note: from jsExplore/resources/_util.js
   */
  function __processError(url, error) {
      var errMsg;
      if (error.response && error.response.text) {
        errMsg = error.response.text;
      } else if (error.response && error.response.status) {
        errMsg = lang.replace(resourcesMessages.ERROR_URL_REQUEST, [ error.response.status, url ]);
      } else {
        errMsg = lang.replace(resourcesMessages.ERROR_URL_REQUEST_NO_STATUS, [ url ]);
      }
      error.errMsg = errMsg;
      return error;
    };

  /**
   * 
   * @returns  Deferred
   *           When resolved, indicates if Toolbox exists - 
   *           True if the Toolbox exists; False otherwise.
   */
  function __checkIfToolboxExists() {
    var deferred = new Deferred();
      
    var url = '/adminCenter/feature';
    request.get(url).then(function(response) {
       deferred.resolve(true);
    }, function(err) {
       deferred.resolve(false);
    });

    return deferred;
  }

  /**
   * 
   * @returns  Deferred  
   *           When resolved, indicates if the Explore Tool exists - 
   *           True if the Explore Tool exists; False otherwise.
   */
  function __checkIfExploreToolExists() {
    var deferred = new Deferred();

    var url = "/ibm/adminCenter/explore-1.0/feature";
    request.get(url).then(function(response) {
       deferred.resolve(true);
    }, function(err) {
       deferred.resolve(false);
    });

    return deferred;
  }
  
 function __getBidiTextDirectionSetting() {
    if (!__baseTextDirection) {
      __baseTextDirection = "ltr";
      if (has("adminCenter-bidi")) {
        var bidiType = has("adminCenter-bidi-type");
        if (bidiType == "rtl") {
          __baseTextDirection = "rtl";
        } else if (bidiType == "contextual") {
          __baseTextDirection = "auto";
        }
      }
    }
    return __baseTextDirection;
  }

  /**
   * Determine the text direction for a string based on the current Bidi Text Direction user preference.
   *
   * If the Bidi Text Direction is set to "auto", the text direction "ltr" or "rtl" is determined by the first character of the text string.
   *
   * @param {string}
   *          text User inputted text value.
   * @return {string} The direction of the text, "ltr" or "rtl".
   *
   */
  function __getStringTextDirection(text) {
    var bidiType = __getBidiTextDirectionSetting();
    if (bidiType == "auto") {
      // Workaround: When the bidi setting in preferences is
      // contextual, which translates to a bidiType of
      // 'auto', then the first typed character is used
      // to determine the direction, "ltr" or "rtl",
      // of the text.
      // When the first character is in a language
      // such as Arabic or Hebrew, orientation and typing
      // direction becomes "rtl", with right alignment
      // of text; otherwise, it is "ltr".
      // Create a dummy widget to let dojo determine if
      // the text direction is ltr (Latin characters)
      // or rtl (Bidi characters) for the inputted text.
      // The value returned can be used to set the html dir
      // value for non-widget user text on the screen.
      if (!__textDirectionWidget) {
        __textDirectionWidget = new Widget({
          textDir : bidiType
        });
      }
      bidiType = __textDirectionWidget.getTextDir(text);
    }
    return bidiType;
  }

  /**
   * This method will calculate the width of the browser's vertical scroll bar
   * It will also calculate the height and save it for later reference
   *
   * @returns {Number}
   */
  function __getScrollBarWidth() {
    if (!__scrollBarWidth) {
      var outer = document.createElement("div");
      outer.style.visibility = "hidden";
      outer.style.width = "100px";
      outer.style.height = "50px";

      document.body.appendChild(outer);

      var totalWidthIncludingScrollBar = outer.offsetWidth;
      var totalHeightIncludingScrollBar = outer.offsetHeight;
      outer.style.overflow = "scroll"; // force the scrollbar to appear

      // Create an inner div inside the outer div
      // This will allow us to isolate the outer div's scrollbar
      var inner = document.createElement("div");
      inner.style.width = "100%";
      inner.style.height = "100%";
      outer.appendChild(inner);

      var widthWithoutScrollBar = inner.offsetWidth;
      var heightWithoutScrollBar = inner.offsetHeight;

      // Clean up by removing both divs
      outer.parentNode.removeChild(outer);
      __scrollBarWidth = totalWidthIncludingScrollBar - widthWithoutScrollBar;
      __scrollBarHeight = totalHeightIncludingScrollBar - heightWithoutScrollBar;
    }
    return __scrollBarWidth;
  }

  /**
   * This method will return the height of the browser's horizontal scroll bar
   *
   * @returns {Number}
   */
  function __getScrollBarHeight() {
    if (!__scrollBarHeight) {
      __getScrollBarWidth()
    }
    return __scrollBarHeight;
  }

  function __domToString(node) {
    /*
     * Was not able to find a way to convert a DOM node into a raw String.
     * innerHTML will strip off the parent element that we need to preserve.
     * So the hack is to place the DOM node into an empty div and grab the innerHTML.
     */
    var emptyDiv = domConstruct.create("div");
    domConstruct.place(node, emptyDiv);
    return emptyDiv.innerHTML;
  } // end of __domToString

  /**
   * Converts spaced phrases to camelCase
   * @param phrase
   * @returns camelCase phrase
   */
  function __toCamelCase (phrase) {
    var segments = phrase.split(' ');
    for(var i = 0; i < segments.length; ++i){
      var segment = segments[i];
      segment = segment.toLowerCase();
      if (i === 0) {
    	   segments[i] = segment;
      } else {
    	   segments[i] = segment.substring(0,1).toUpperCase() + segment.substring(1);
      }
    }
    return segments.join('');
  }

  /**
   * Converts a camelCase into snake_case
   * @param string
   * @returns a string in snake_case (uses underscores)
   */
  function __camelToSnake (phrase) {
    var upperCase = new RegExp('[a-z](?=[A-Z])', 'g');	//match lowercase letters only if followed by uppercase letter, without including the letter
    return phrase.replace(upperCase, '$&_');	//in the last match, replace upperCase with _ (underscore)
  }
});
