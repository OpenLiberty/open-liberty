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
/* jshint strict: false */
define([
        "js/common/platform",
        "dojo/_base/declare", 
        "dijit/layout/ContentPane",
        "jsExplore/widgets/SideTabButton"
        ], function(
            platform,
            declare,
            ContentPane,
            SideTabButton
        ){

  var SideTabPane = declare("SideTabPane", [ ContentPane ], {
    constructor : function(params) { 
      this.resource = params[0];
      this.defaultSideTab = params[1];
      this.id = this.resource.id + "SideTabPane";
    },

//    sideTabButtons : ['Overview', 'Apps', 'Servers', 'Runtimes', 'Instances', 'Stats', 'Config'],
    id : '',
    baseClass : platform.getDeviceCSSPrefix() + "sideTab",
    region : platform.isPhone() ? "bottom" : "left",
    resource : null,
    sideTabButtons : [],

    postCreate : function() {
      // add the overview button - it is there for all sideTabPanes
      this.addSideTabButton('Overview');
      // It is very weird that the sideTabButtons property stores every sideTabButton created for every objectView created.
      // To work around this mystery, has to set the properties of the latest sideTabButton in the array, not the first element
      // in the array.
      this.sideTabButtons[this.sideTabButtons.length-1].set("displayed", true);
      this.sideTabButtons[this.sideTabButtons.length-1].pane = this; // GRAPH_REDRAW_CHANGE Bind the pane into the overview button
      this.set("role", "navigation");
      this.set("aria-label", this.resource.id);
    },
    
    addSideTabButton : function(buttonType, populateTabPane) {
      var sideTabButton = new SideTabButton({
        resource: this.resource,
        type: buttonType,
        populateFunction: populateTabPane
      });
      this.sideTabButtons.push(sideTabButton);
      this.addChild(sideTabButton);
      if (this.defaultSideTab && (this.defaultSideTab === buttonType || (this.defaultSideTab.indexOf('/serverConfig') === 0 && buttonType === 'Config'))){
        sideTabButton.onClick(this.defaultSideTab);
      }
    },
    
    /**
     * Return the button for the given buttonType
     * @param buttonType
     */
    getSideTabButton : function(buttonType) {
      var button = null;
      for (var i = 0; i < this.sideTabButtons.length; i++) {
        if (this.sideTabButtons[i].type === buttonType) {
          button = this.sideTabButtons[i];
        }
      }
      return button;
    },
    
    buttonClicked : function(button) {
      for (var i = 0; i < this.sideTabButtons.length; i++) {
        // Since all the buttons including those that are destroyed are still kept in the array, has to skip those buttons that
        // are destroyed.
        if (this.sideTabButtons[i].domNode) {
          if (this.sideTabButtons[i] === button) {
            this.sideTabButtons[i].set("displayed", true);
          } else {
            this.sideTabButtons[i].set("displayed", false);
          }
        }
      }
    }
  });
  return SideTabPane;

});