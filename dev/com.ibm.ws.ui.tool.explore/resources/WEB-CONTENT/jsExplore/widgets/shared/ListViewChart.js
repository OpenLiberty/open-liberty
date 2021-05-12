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
 * A ListViewChart object is a representation of the current state of a stateful resource.
 * 
 * N.B. The word state and status are used pretty interchangeably outside of the Admin Center.
 *      Outside of the Admin Center, a server and a cluster have a status. The word 'status'
 *      is used by the wlp/bin/server command and is used in the ServerCommands and ClusterManager
 *      MBeans. Applications however have a 'state', as per their application MBean. In order
 *      to keep ourselves sane in our code base, we have opted to use the word 'state' everywhere,
 *      changing API responses that return 'status' to be 'state'.
 */
define([ 'dojo/_base/declare', 'dojo/dom', 'dojo/dom-construct', 'dojo/on', 'dojo/i18n!../../nls/explorerMessages', 
         'dojo/_base/lang', 
         'dojox/gfx',
         'dojo/dom-style',
         'dojo/_base/fx',
         'dojo/aspect',
         'dojo/fx',
         'js/common/tr'],
    function(declare, dom, domc, on, i18n, lang, gfx, domStyle, fxBase, aspect, fx, tr) {


  /**
   * Defines the StateIcon. The StateIcon is a resource-state aware entity which will auto-update
   * its icon based on state change events. While resources will update themselves, this establishes
   * its own state change event listener so that the icon is updated without need for external
   * calls.
   */
  return declare('ListViewChart', [], {

    constructor: function(configObj) {
      if (!configObj) {
        tr.throwMsg('Programming Error: No configuration object passed into the constructor. Go read the JSDoc!');
      }
      this.resource = configObj.resource;
      this.domElementLabel = configObj.domElementLabel;
      this.domElementGraphic = configObj.domElementGraphic;
      this.id = configObj.resource.id+'-chart';
      this.barHeight = configObj.height; // height is usually fixed
      this.animation = configObj.animation;

      this.resource.subscribe(this);
    },

    
    destroy: function() {
      this.resource.unsubscribe(this);
      domc.destroy(this.id);
    },

    onTallyChange: function() {
      this.updateLabel();
      this.updateChart();
    },
        
    getLabel: function() {
      var total = this.resource.up + this.resource.down + this.resource.unknown;
      switch(this.resource.type) {
      case 'appOnCluster':
        return lang.replace(i18n.APPS_INSTANCES_RUNNING_OF_TOTAL, [ this.resource.up, total ]);
        break;
      case 'appOnServer':
        return "";
        break;
      case 'serversOnCluster':
      case 'serversOnHost':
      case 'serversOnRuntime':
        return lang.replace(i18n.HOSTS_SERVERS_RUNNING_OF_TOTAL, [ this.resource.up, total ]);
        break;
      };
    },
    
    __gfxStackableBar : function() {
      domc.empty(this.domElementGraphic);
      var total = this.resource.up + this.resource.down + this.resource.unknown;
      if ( total > 0 ) {
        var surface = gfx.createSurface(this.domElementGraphic, "100%", this.barHeight);
        
        
        var upPixel = this.resource.up / total ;
        var downPixel = this.resource.down / total;
       
        var upPect = Math.ceil ( upPixel * 100 );
        if ( upPect < 0 ) upPect = 0;
        if ( upPect  >100 ) upPect = 100;
        var downPect = Math.ceil ( downPixel * 100 );
        if ( downPect < 0 ) downPect = 0;
        if ( downPect  >100 ) downPect = 100;
        var unknownPect = 100 - upPect - downPect; 
        if ( unknownPect < 0 ) unknownPect = 0;
        if ( unknownPect  >100 ) unknownPect = 100;
        
        console.log("three values = " + upPect + ", " + downPect + ", " + unknownPect);
        surface.createRect({ x: 0, y: 0, width: upPect + "%", height: this.barHeight }).setFill([38,156,108,1]);//"#269C6C"
        surface.createRect({ x: upPect + "%", y: 0, width: downPect + "%", height: this.barHeight }).setFill([105,105,105,1]);//"#696969");
        surface.createRect({ x: upPect + downPect + "%", y: 0, width: unknownPect + "%", height: this.barHeight }).setFill([221,115,28,1]);//"#DD731C");
        domStyle.set(this.domElementGraphic, "opacity", "0");
        var fadeArgs = {
            duration: 2000,
            node: this.domElementGraphic
          };
        fxBase.fadeIn(fadeArgs).play();
      }
    },
    
    /*
     * update the stackable bar
     */
    updateChart: function() {
        this.__gfxStackableBar();
    },
    
    /*
     * update the state label
     */
    updateLabel: function() {
      if (this.domElementLabel) {
        this.domElementLabel.innerHTML = this.getLabel();
      } 
    }
    

  });
});