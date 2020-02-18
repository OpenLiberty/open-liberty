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
 * This Javascript file contains utility functions for resolving images and icons at the correct size.
 * <p>
 * This module is intended to be re-used by the various tools and included in their built DOJO.
 * @module common/imgUtils
 */
define(['js/common/platform'], function(platform) {
  'use strict';
  
  var svgResource = '#';

  return {

    /**
     * Obtain the correct icon URL for the tool. By default, tool icon URLs returned by the REST API
     * are for tablet / desktop (142px). This method will interpret the Tool object and return the
     * appropriate URL for the appropriately sized icon based on the current platform.
     * <p>
     * The icon URL for a tool is determined based on a few factors:
     * <ol>
     * <li>The type of the tool. Feature tool icons are obtained via REST API where as bookmarks are fixed paths</li>
     * <li>Special case bookmarks, such as 'wasdev.net' have custom bookmarks which must be handled.</li>
     * </ol>
     * 
     * @method
     * @param {Object} the Tool object returned from the toolbox / catalog REST API
     * @return {String} the URL for the correctly sized tool icon
     */
    getToolIcon: function(tool) {
      if (platform.isPhone()) {
        // This stuff is hard-coded. It would be better to have the tool return
        // a detailed set of information about their sizes. For 2Q14, this is
        // sufficient though.
        if (tool.type === 'featureTool') {
          return tool.icon + '?size=78';
        } else if (tool.id === 'wasdev.net') {
          return 'images/tools/wasdev_78x78.png';
        } else if (tool.type === 'bookmark') {
          return 'images/tools/defaultBookmark_78x78.png';
        }
      } else {
        return tool.icon;
      }
    },

    /**
     * Obtain the correct URL for the icon based on the current platform.
     * 
     * @method
     * @param {String} the name of the icon without any file extensions or platform modifiers. e.g. 'toolbox'
     * @param {String} optional - explicitly force the image type. This should only be necessary for animated images like GIFs.
     * @return {String} the image URL, appropriate for the platform. e.g. 'toolbox-S.png'
     */
    getIcon: function(name, type) {
      var extension = '.png';
      if (type) {
        extension = '.' + type;
      }
      if (platform.isPhone()) {
        return 'images/' + name + '-S' + extension;
      } else if (platform.isDesktop()) {
        return 'images/' + name + '-D' + extension;
      } else {
        return 'images/' + name + '-T' + extension;
      }
    },
    
    normalizeName: function(name) {
      var normalizedName = name.charAt(0).toLowerCase() + name.slice(1); //lowercase the first letter only to keep camelCase
      
      normalizedName = normalizedName.replace('application','app');
      normalizedName = normalizedName.replace('appinst','appOnServer');
      normalizedName = normalizedName.replace('instances','app'); // change to appOnServer if we decide to use the icon for left nav bar
      normalizedName = normalizedName.replace('instance','appOnServer');
      
      //selectively remove plurals. 
      //  could probably do a regex replace for string ending in s, but there are possible corner cases, like 'status'?
      switch(normalizedName) {
      case 'apps':
      case 'clusters':
      case 'hosts':
      case 'servers':
      case 'runtimes':
        normalizedName = normalizedName.slice(0, -1);
        break;
      }
      return normalizedName;
    },
    
    getSVGName: function(name) {
      name = this.normalizeName(name);
      return svgResource + name;
    },
    getSVGSmallName: function(name) {
      name = this.normalizeName(name);
      return svgResource + name + '-small';
    },
        
    getSVGIcon: function(name, Class, size) {
      name = this.normalizeName(name);
      var svgNS = "http://www.w3.org/2000/svg";
      var xlinkNS = "http://www.w3.org/1999/xlink";
    //IE doesn't support outerHTML on SVG objects, so need div to grab SVG text 
      var svgDiv = document.createElement('div'); 
      
        var svg = document.createElementNS(svgNS, "svg");
        
        if (Class) {
          svg.setAttribute('class',  Class + '-Icon-SVG');
        }
        svg.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:xlink", xlinkNS);
        svg.setAttribute('preserveAspectRatio', 'xMidYMid meet');
        svg.setAttribute('viewBox', '0 0 64 64');
                
        if (size){
          if (size === 'small') {
            svg.setAttribute('viewBox', '0 0 32 32');
            name = name + '-small';
          }
        }
        
        var icon = document.createElementNS(svgNS, 'use');
        icon.setAttributeNS(xlinkNS, "href", svgResource + name);
        svg.appendChild(icon);
        svgDiv.appendChild(svg);
      
      return svgDiv.innerHTML;
    },
    
    /**
     * Obtain the SVG icon
     * The small icon may look visually different (usually thicker lines). No difference otherwise. 
     * The actual size when used will look the same, as long as the SVG file is correct. 
     * @method
     * @param {String} the name of the icon without any file extensions or platform modifiers. e.g. 'toolbox'
     * @param {String} Class - will come out as {Class}-Icon-SVG
     * 
     * @return {String} the <svg> text, without the <div>
     */
    getSVG: function(name, Class) {
      return this.getSVGIcon(name, Class, '');
    },
    
    getSVGSmall: function(name, Class) {
      return this.getSVGIcon(name, Class, 'small');
    }
     
  };
});
