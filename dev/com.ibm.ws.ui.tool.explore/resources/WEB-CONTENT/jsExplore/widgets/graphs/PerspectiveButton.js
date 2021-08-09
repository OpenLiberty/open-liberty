/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/* jshint strict: false */
define([ "dojo/_base/declare", 
         "js/common/platform", 
         'jsExplore/widgets/graphs/GraphContainer', 
         "dijit/form/Button", 
         "dojo/i18n!jsExplore/nls/explorerMessages", 
         "dojo/text!jsExplore/widgets/graphs/templates/PerspectiveButton.html"
         ],
         function(declare, 
             platform,
             GraphContainer,
             Button, 
             i18n, 
             template) {

  var PerspectiveButton = declare("PerspectiveButton", [ Button ], {
    constructor : function(configObj) { // resource, perspective
      this.resource = configObj.resource;
      this.buttonPerspective = configObj.perspective;
      this.id = this.resource.id + "Perspective" + this.buttonPerspective + "Button";
      
      this.set("baseClass", platform.getDeviceCSSPrefix() + "perspectiveButton");
    },

    id : '',
    pane : null,
    baseClass : platform.getDeviceCSSPrefix() + "perspectiveButton",
    title : '', // set based on buttonViewType
    "aria-label" : '', // set same as title

    resource : null,
    buttonPerspective : '', // "Summary", "Problem", "Traffic", "Performance", "Alert", "Availability"
    templateString: template,

    postCreate : function() {
      this.set('class', 'sideTabLabel');
      if (this.buttonPerspective === "Problem") {
        this.set("title", i18n.STATS_PERSPECTIVE_PROBLEM);
        this.set("label", i18n.STATS_PERSPECTIVE_PROBLEM);
      } else if (this.buttonPerspective === "Traffic") {
        this.set("title", i18n.STATS_PERSPECTIVE_TRAFFIC);
        this.set("label", i18n.STATS_PERSPECTIVE_TRAFFIC);
      } else if (this.buttonPerspective === "Performance") {
        this.set("title", i18n.STATS_PERSPECTIVE_PERFORMANCE);
        this.set("label", i18n.STATS_PERSPECTIVE_PERFORMANCE);
      } else if (this.buttonPerspective === "Alert") {
        this.set("title", i18n.STATS_PERSPECTIVE_ALERT);
        this.set("label", i18n.STATS_PERSPECTIVE_ALERT);
      } else if (this.buttonPerspective === "Availability") {
        this.set("title", i18n.STATS_PERSPECTIVE_AVAILABILITY);
        this.set("label", i18n.STATS_PERSPECTIVE_AVAILABILITY);
      } else {
        // assume summary
        this.set("title", i18n.STATS_PERSPECTIVE_SUMMARY);
        this.set("label", i18n.STATS_PERSPECTIVE_SUMMARY);
      }
    },

    onClick : function(hash) {
      if (!this.pane) {
        this.pane = new GraphContainer({resource: this.resource, perspective: this.buttonPerspective});
        this.graphStack.addChild(this.pane);
      }
      this.graphStack.selectChild(this.pane);
    },

    // set iconclass when content is displayed/not displayed
    _setDisplayedAttr : function(value) {
      this.displayed = value;
      if (value) {
        this.set('class', 'sideTabSelectedLabel');
      } else {
        this.set('class', 'sideTabLabel');

      }
    }

  });
  return PerspectiveButton;

});