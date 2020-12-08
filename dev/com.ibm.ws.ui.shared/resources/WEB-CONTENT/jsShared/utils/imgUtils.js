!function() {
  var imgUtils = {};

  var svgResource = '#';
  
  imgUtils.normalizeName = function(name) {
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
  };

  imgUtils.getSVGName = function(name) {
    name = this.normalizeName(name);
    return svgResource + name;
  },
  imgUtils.getSVGSmallName = function(name) {
    name = this.normalizeName(name);
    return svgResource + name + '-small';
  },
  
  imgUtils.__getSVGObject = function(name, Class, size, ariaLabel) {
    name = imgUtils.normalizeName(name);
    var svgNS = "http://www.w3.org/2000/svg";
    var xlinkNS = "http://www.w3.org/1999/xlink";
    
    var svg = document.createElementNS(svgNS, "svg");

    if (Class) {
      svg.setAttribute('class', Class + '-Icon-SVG');
      //TODO: possibly remove - only 1 or 2 icons use this. search '-Icon-SVG' to check
    }
    svg.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:xlink", xlinkNS);
    svg.setAttribute('preserveAspectRatio', 'xMidYMid meet');
    svg.setAttribute('viewBox', '0 0 64 64');
    svg.setAttribute('role', 'img');
    if (ariaLabel) {
      svg.setAttribute('aria-label', ariaLabel);
      svg.setAttribute("alt", ariaLabel);
    }
    else{
      svg.setAttribute("alt", name);
    }
    if (size) {
      if (size === 'small') {
        svg.setAttribute('viewBox', '0 0 32 32');
        name = name + '-small';
      }
    }

    var icon = document.createElementNS(svgNS, 'use');
    icon.setAttributeNS(xlinkNS, "href", svgResource + name);
    svg.appendChild(icon);
    return svg;
  }

  imgUtils.__getSVGIcon = function(name, Class, size, title, ariaLabel) {
    // IE doesn't support outerHTML on SVG objects, so need div to grab SVG text
    var svgHolderSpan = document.createElement('span');
    var svg = this.__getSVGObject(name, Class, size, ariaLabel ? title : null);    
    svgHolderSpan.appendChild(svg);

    if (title) {
      svgHolderSpan.setAttribute("title", title);
      var svgTempHolderSpan = document.createElement('span');
      svgTempHolderSpan.appendChild(svgHolderSpan);
      return svgTempHolderSpan.innerHTML;
    } else {
      return svgHolderSpan.innerHTML;
    }
  };

  /**
   * Obtain the SVG icon in text format. The small icon may look visually different (usually thicker lines). No difference otherwise. The actual size when
   * used will look the same, as long as the SVG file is correct.
   * 
   * @method
   * @param {String}
   *          the name of the icon without any file extensions or platform modifiers. e.g. 'toolbox'
   * @param {String}
   *          Class - will come out as {Class}-Icon-SVG
   * @param {String}
   *          title - the title to assign to the parent <span> for tooltip on hover-over 
   * @return {String} the <svg> text, without the <span> if no title specified
   */
  imgUtils.getSVG = function(name, Class, title, ariaLabel) {
    return this.__getSVGIcon(name, Class, '', title, ariaLabel);
  };

  imgUtils.getSVGSmall = function(name, Class, title, ariaLabel) {
    return this.__getSVGIcon(name, Class, 'small', title, ariaLabel);
  };
  
  /**
   * Obtain SVG icon in Object format.
   * 
   * @method
   * @param {String}
   *          the name of the icon without any file extensions or platform modifiers. e.g. 'toolbox'
   * @param {String}
   *          Class - will come out as {Class}-Icon-SVG
   * @param {String}
   *          title - the title to assign to the parent <span> for tooltip on hover-over
   * @param {Boolean}
   *          ariaLabel - if set to true, the title will be also used as an aria label
   * 
   * @return {String} the <svg> text, without the <span> if no title specified
   */
  imgUtils.getSVGObject = function(name, Class, title, ariaLabel) {
    return this.__getSVGObject(name, Class, '', ariaLabel ? title : null);
  };

  imgUtils.getSVGSmallObject = function(name, Class, title, ariaLabel) {
    return this.__getSVGObject(name, Class, 'small', ariaLabel ? title : null);
  };
  
  // This code was lifted from the d3 library.  This logic allows pure javascript to be loaded using html <script> or by AMD loading
  if (typeof define === "function" && define.amd) {
    define(this.imgUtils = imgUtils); 
  } else if (typeof module === "object" && module.exports) { 
    module.exports = imgUtils; 
  } else { 
    this.imgUtils = imgUtils;
  }
}();