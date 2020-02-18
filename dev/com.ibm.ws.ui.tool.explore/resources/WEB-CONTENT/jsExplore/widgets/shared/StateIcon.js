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
 * A StateIcon object is a representation of the current state of a stateful resource.
 * 
 * N.B. The word state and status are used pretty interchangeably outside of the Admin Center.
 *      Outside of the Admin Center, a server and a cluster have a status. The word 'status'
 *      is used by the wlp/bin/server command and is used in the ServerCommands and ClusterManager
 *      MBeans. Applications however have a 'state', as per their application MBean. In order
 *      to keep ourselves sane in our code base, we have opted to use the word 'state' everywhere,
 *      changing API responses that return 'status' to be 'state'.
 */
define([ 'dojo/_base/declare', 'dojo/dom', 'dojo/dom-construct', 'dojo/on', 'dojo/i18n!../../nls/explorerMessages', 'js/common/platform',
         'dojo/_base/lang', 'jsExplore/resources/_util', 'js/common/tr', 'jsShared/utils/imgUtils' ],
    function(declare, dom, domc, on, i18n, platform, lang, util, tr, imgUtils) {


  /**
   * Defines the StateIcon. The StateIcon is a resource-state aware entity which will auto-update
   * its icon based on state change events. While resources will update themselves, this establishes
   * its own state change event listener so that the icon is updated without need for external
   * calls.
   * These images should probably be moved to css.
   */
  var htmlTemplate = '<img src="images/status{0}{1}{2}.{5}" height="{3}" width="{3}" style="vertical-align:middle;" alt="{4}" title="{4}">';
  var labelHtmlTemplate = '<span class="{0}">{1}</span>';
  var actionHtmlTemplate = '<img src="imagesShared/card-action{0}.png" height="{2}" width="{2}" style="vertical-align:top;" alt="{3}" title="{1}">';
  var labelCardHtmlTemplate = '<span title="{0}">{0}</span>';

  return declare('StateIcon', [], {
    size: "32",
    label: "",
    // cardState is the little icon state inside the card
    // if true, then it gets the actual state
    // if false, then it gets the gear when started/stopped/unknown
    isCardState: false,	
    platformString: "-T",
    platformIconSeparator: "-",
    constructor: function(configObj) {
      if (!configObj) {
        tr.throwMsg('Programming Error: No configuration object passed into the constructor. Go read the JSDoc!');
      }
      this.resource = configObj.resource;
      this.id = configObj.parentId+'-stateIcon';
      this.state = this.resource.state; // This should be kept in sync with the resoure's state via events. If its not, then we are likely dealing with a bug.
      if (configObj.size) {
        this.size = configObj.size;
      }
      if (configObj.cardState) {
        this.isCardState = configObj.cardState;
      }
      if (configObj.showLabel) {
        this.showLabel = configObj.showLabel;
      }
      if (configObj.labelClass) {
        this.labelClass = configObj.labelClass;
      } else {
        this.labelClass = 'stateIconLabel'; // Default class for the label
      }
      if (configObj.alt) {
        this.alt = configObj.alt;
      }

      this.resource.subscribe(this);
    },

    onStateChange: function(state) {
      this.updateStateIcon(state);
    },

    destroy: function() {
      this.resource.unsubscribe(this);
      domc.destroy(this.id);
    },
    
    onDestroyed: function() {
      this.destroy();
    },

    getIconImage: function() {
      switch(this.state){
      case 'STARTED':
        return imgUtils.getSVGName('status-running');
      case 'STARTING':
        if (this.size === '14') {
          return imgUtils.getSVGName('status-starting');
        } else{ 
          return "images/status-starting-T.gif";
        }
      case 'STOPPED':
      case 'INSTALLED':
        return imgUtils.getSVGName('status-stopped');
      case 'STOPPING': 
        if (this.size === '14') {
          return imgUtils.getSVGName('status-stopped');
        } else { 
          return "images/status-stopping-T.gif";
        }
      case 'PARTIALLY_STARTED':
      case 'PARTIALLY STARTED':
        if (util.getResourceDisplayedState(this.resource) == "STARTED") {
          return imgUtils.getSVGName('status-running');
        } else {
          return imgUtils.getSVGName('status-some-running');
        }
      case 'UNKNOWN':
      case 'NOT_FOUND':
        return imgUtils.getSVGName('unknown');
      default:
        return imgUtils.getSVGName('unknown');
      }
    },

    getIconLabel: function() {
      switch(this.state){
      case 'STARTED':
        return i18n.RUNNING;
      case 'STARTING':
        return i18n.STARTING;
      case 'STOPPED':
      case 'INSTALLED':
        return i18n.STOPPED;
      case 'STOPPING': 
        return i18n.STOPPING;
      case 'PARTIALLY_STARTED':
      case 'PARTIALLY STARTED':
        return util.getResourceDisplayedLabel(this.resource);
      case 'UNKNOWN':
      case 'NOT_FOUND':
        return i18n.UNKNOWN;
      default:
        return i18n.UNKNOWN;
      }
    },

    /*
     * function to update the state icon image and label
     */
    updateStateIcon: function(newState) {
      this.state = newState;
      this.updateIcon();
      this.updateLabel();  
    },

    /*
     * update the icon image
     */
    updateIcon: function() {
      var icon = this.__getStateIcon(this.state);
      if (icon) {
        // TODO: For some reason, we're not getting cleaned up properly...
        // The DOM ID is gone but our listener was not removed?!
        var domElement = dom.byId(this.id);
        if (domElement) {
          domElement.innerHTML = icon;
        }
      }
    },

    /*
     * update the state label
     */
    updateLabel: function() {
      // update the state label 
      if (this.isCardState) {
        var labelDomElement = dom.byId(this.id + '-stateLabel');
        if (labelDomElement) {
          labelDomElement.innerHTML = this.__getCardLabel();
        }
      }
    },

    getHTML: function(className) {
      var span = document.createElement('span');
        if(className) {
          span.setAttribute('class', className);
        }
        span.setAttribute('style', 'white-space: nowrap');
        span.setAttribute('id', this.id);
        span.innerHTML = this.__getStateIcon(this.state);
      return span.outerHTML;
    },

    __getStartedIcon: function () {
      return imgUtils.getSVG('status-running');
    },

    __getStartingIcon: function() {
      var extension = 'gif';
      if (this.size === '14') {
        return imgUtils.getSVG('status-starting');
      }
      return lang.replace(htmlTemplate, [this.platformIconSeparator, 'starting', this.platformString, this.size, i18n.STARTING, extension]);
    },

    __getStoppedIcon: function() {
        return imgUtils.getSVG('status-stopped');
    },

    __getStoppingIcon: function() {
      var extension = 'gif';
      if (this.size === '14') {
        return imgUtils.getSVG('status-stopping');
      }
      return lang.replace(htmlTemplate, [this.platformIconSeparator, 'stopping', this.platformString, this.size, i18n.STOPPING, extension]);
    },    

    //TODO: check the alt/title text for some of these
    __getPartiallyStartedIcon: function() {
      // with the new design, partially running should return as running for a dynamic cluster or app belonging to a dynamic cluster
      if (util.getResourceDisplayedState(this.resource) == "STARTED") {
        return this.__getStartedIcon();
      } else {
        return imgUtils.getSVG('status-some-running');
      }
    },

    __getUnknownIcon: function() {
//      return lang.replace(htmlTemplate, [this.platformIconSeparator, 'unknown', this.platformString, this.size, i18n.UNKNOWN, 'png']);
      return imgUtils.getSVGSmall('unknown');
    },

    __getActionIcon: function() {
      var title = (globalIsAdmin === false ? i18n.ACTION_DISABLED_FOR_USER : i18n.ACTIONS);
      if (this.alt) {
        return lang.replace(actionHtmlTemplate, [this.platformString, title, this.size, this.alt]);
      }
      return lang.replace(actionHtmlTemplate, [this.platformString, title, this.size, title]);
    },

    __getLabel: function() {
      return lang.replace(labelHtmlTemplate, [this.labelClass, this.label]);
    },

    __getCardLabel: function() {
      if (this.isCardState) {
        return lang.replace(labelCardHtmlTemplate, [this.label]);
      }
    },

    /**
     * Gets the appropriate state icon for the given resource state.
     * TODO: Is this the kind of method that should like in the prototype of the StateIcon class?
     */
    __getStateIcon: function(state){
      switch(state){
      case 'STARTED':
        this.label = i18n.RUNNING;
        if (this.isCardState) {
          if (this.showLabel) {
            return this.__getStartedIcon() + this.__getLabel();
          } else {
            return this.__getStartedIcon();  
          }
        } else {
          return this.__getActionIcon();
        }
      case 'STARTING':
        this.label = i18n.STARTING;
        if (this.showLabel) {
          return this.__getStartingIcon() + this.__getLabel();
        } else {
          return this.__getStartingIcon();  
        }
      case 'STOPPED':
      case 'INSTALLED':
        this.label = i18n.STOPPED;
        if (this.isCardState) {
          if (this.showLabel) {
            return this.__getStoppedIcon() + this.__getLabel();
          } else {
            return this.__getStoppedIcon();  
          }
        } else {
          return this.__getActionIcon();
        }
      case 'STOPPING':
        this.label = i18n.STOPPING;
        if (this.showLabel) {
          return this.__getStoppingIcon() + this.__getLabel();
        } else {
          return this.__getStoppingIcon();  
        }
      case 'PARTIALLY_STARTED':
      case 'PARTIALLY STARTED':
        this.label = util.getResourceDisplayedLabel(this.resource);
        if (this.isCardState) {
          if (this.showLabel) {
            return this.__getPartiallyStartedIcon() + this.__getLabel();
          } else {
            return this.__getPartiallyStartedIcon();  
          }
        } else {
          return this.__getActionIcon();
        }
      case 'UNKNOWN':
      case 'NOT_FOUND': // TODO: Should we push this into the Server's state?
        this.label = i18n.UNKNOWN;
        if (this.isCardState) {
          if (this.showLabel) {
            return this.__getUnknownIcon() + this.__getLabel();
          } else {
            return this.__getUnknownIcon();  
          }
        } else {
          return this.__getActionIcon();
        }
      default:
        // Issue the unknown icons for any other state we don't know about.
        this.label = i18n.UNKNOWN;
      if (this.isCardState) {
        if (this.showLabel) {
          return this.__getUnknownIcon() + this.__getLabel();
        } else {
          return this.__getUnknownIcon();  
        }
      } else {
        return this.__getActionIcon();
      }
      }
    }

  });
});