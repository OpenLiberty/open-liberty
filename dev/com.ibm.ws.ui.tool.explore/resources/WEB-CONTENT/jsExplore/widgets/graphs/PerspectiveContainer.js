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
define([
        "dojo/_base/declare", 
        "dijit/layout/ContentPane",
        "dijit/_TemplatedMixin",
        "dijit/_WidgetsInTemplateMixin",
        'jsExplore/widgets/graphs/PerspectiveButton', 
        "dojo/i18n!jsExplore/nls/explorerMessages",
        "dojo/text!./templates/PerspectiveContainer.html"
        ], function(
                declare,
                ContentPane,
                _TemplatedMixin,
                _WidgetsInTemplateMixin,
                PerspectiveButton,
                i18n,
                template
            ){

    var PerspectiveContainer = declare("PerspectiveContainer", [ ContentPane, _TemplatedMixin, _WidgetsInTemplateMixin ], {

        templateString: template,
        id : "",
        resource : null,
        resourceId : "",

        postMixInProperties : function() {
          this.id = this.resource.id + "Stats-PerspectivePane";
          this.resourceId = this.resource.id;
        },

        postCreate : function() {
          this.inherited(arguments);

          // add the perspective buttons
          var pButton = new PerspectiveButton({
            graphStack: this.graphStack,
            resource: this.resource,
            perspective: "Summary"
          });
          this.toolbar.addChild(pButton);
          pButton.onClick();
          pButton = new PerspectiveButton({
            graphStack: this.graphStack,
            resource: this.resource,
            perspective: "Problem"
          });
          this.toolbar.addChild(pButton);
          pButton = new PerspectiveButton({
            graphStack: this.graphStack,
            resource: this.resource,
            perspective: "Traffic"
          });
          this.toolbar.addChild(pButton);
          pButton = new PerspectiveButton({
            graphStack: this.graphStack,
            resource: this.resource,
            perspective: "Performance"
          });
          this.toolbar.addChild(pButton);
          pButton = new PerspectiveButton({
            graphStack: this.graphStack,
            resource: this.resource,
            perspective: "Alert"
          });
          this.toolbar.addChild(pButton);
          pButton = new PerspectiveButton({
            graphStack: this.graphStack,
            resource: this.resource,
            perspective: "Availability"
          });
          this.toolbar.addChild(pButton);
        }
    });

    return PerspectiveContainer;

});
