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
 * Various resource utilities. This class is mostly a hold-over from previous implementations, but isStandalone() is still important and
 * used.
 * 
 * @author Michal Broz <mbroz@us.ibm.com>
 * @module resources/utils
 * 
 * @return {Object} Containing all the utils methods
 */
define([ "jsExplore/resources/stats/_mbeanUtils", "dojo/request/xhr", "dojo/has", "dijit/_Widget", "dijit/registry" ],
/**
 * @exports resources/utils
 */
function(mbeanUtils, xhr, has, Widget, registry) {
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
      getBidiTextDirectionSetting : __getBidiTextDirectionSetting,
      getStringTextDirection : __getStringTextDirection,
      getScrollBarWidth : __getScrollBarWidth,
      getScrollBarHeight : __getScrollBarHeight,
      destroyWidgetIfExists : __destroyWidgetIfExists
  };

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
   * Check for the availability of one of the collective mbeans that we need like
   * WebSphere:feature=collectiveController,type=CollectiveRepository,name=CollectiveRepository
   * 
   * @private
   * @returns {boolean} isStandalone - True if the server is a standalone server. False otherwise
   */
  function __isStandalone() {
    if (global.isStandalone == undefined) {

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
  
  function __destroyWidgetIfExists(id){
    try {
      var results = registry.byId(id);
      results.destroy();
     }
     catch(e)
     {
         // no widget
     }
  }
  
});
