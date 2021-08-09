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
define([
    'dojo/_base/declare',
    'dojo/_base/lang',
    'dijit/_WidgetBase', 
    'dijit/_TemplatedMixin',
    'jsBatch/utils/utils',
    'jsShared/utils/imgUtils',
    'dojo/i18n!jsBatch/nls/javaBatchMessages',
    'dojo/text!./templates/TextWithImage.html'
    ], 
function(declare, lang, WidgetBase, _TemplatedMixin,
         utils, imgUtils, i18n, template) {
  "use strict";

  /**
   * Creates a widget representing an icon with a value.
   * 
   * The widget appears as
   *      <svg> text-value
   *  
   * Attributes for TextWithImage are as follows:
   * @class
   * @typedef {Object} TextWithImage
   * @property {string} divId           - id to give the div containing the widget img
   * @property {string} type            - 'state', 'status', 'id'
   * @property {string} value           - value to display
   *        
   * Example: 
   *      new TextWithImage ({
   *         type: 'state',                     // 'state' or 'status' or 'id'
   *         value: 'COMPLETED',                // value to display 
   *         divId: 'jobInstance-state-2'       // id value
   *       });
   *  
   */
  
  var textWithImage = declare("TextWithImage", [ WidgetBase, _TemplatedMixin ], {
    templateString : template,
    imgTitle: "",
    valueLabel: "",
    type: 'state',   // default to 'state'
    value: "",
    imgClass: "titleImage",
    textClass: "titleText",

    constructor: function(args) {
      declare.safeMixin(this, args);
      
      this.imgId = this.divId + "-image";
      
    },
  
    postMixInProperties : function() {
      switch (this.type) {
        case 'state':
          this.imgTitle = utils.stateStatusToLabel(this.value);
          this.valueLabel = this.imgTitle;
          break;
        case 'status':
          this.imgTitle = utils.stateStatusToLabel(this.value);
          this.valueLabel = this.imgTitle;
          break;
        case 'id':
          this.imgTitle = lang.replace(i18n.PARTITION_ID, [this.value]);
          this.valueLabel = this.value;
          break;
        default:
          console.error("Type of value is not known: " + this.type);
          this.imgTitle = this.value;
      }
      
      if (this.title) {  // Override if needed
        this.imgTitle = this.title;
      }
    },
    
    postCreate: function() {
      // images are set in postCreate because SVGs need to be set here
      switch (this.type) {
        case 'state':
          if (this.value === 'STOPPING') {
            // 'STOPPING' is shown as an animated .gif.  Only the gif id is returned.
            this.iconNode.innerHTML = "<img src='" + utils.statusToImage(this.value) + 
                                      "' class='" + this.imgClass + "'>";            
          } else {
            this.iconNode.innerHTML = utils.stateToImage(this.value);
          }
          break;
        case 'status':
          if (this.value === 'STARTING' || this.value === 'STOPPING') {
            // These status' are shown as animated .gifs.  Only the gif id is returned.
            this.iconNode.innerHTML = "<img src='" + utils.statusToImage(this.value) + 
                                      "' class='" + this.imgClass + "'>";            
          } else {
            this.iconNode.innerHTML = utils.statusToImage(this.value);
          }
          break;
        case 'id':
            this.iconNode.innerHTML = imgUtils.getSVGSmall('partitioned-step');
          break;
        default:
          console.error("Type of value is not 'state' or 'status': " + this.type);
          this.iconNode.innerHTML = "";
      }
    }    
    
  });

  return textWithImage;
});
