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
define("jsBatch/widgets/grid/modules/JobInstanceQuerySizer", [
  'dojo/_base/array', 'dojo/_base/declare', 'dojo/_base/lang', 
  'dojo/aspect', 'dojo/dom-class', 'dojo/keys', 'dojo/query', 
  'dojo/sniff', 'dojo/string', 'dojo/topic',
  'dijit/_FocusMixin', 'dijit/_TemplatedMixin', 'dijit/_WidgetBase',
  'jsBatch/utils/hashUtils',
  'jsBatch/utils/restUtils',
  'dojo/i18n!jsBatch/nls/javaBatchMessages'
], function(array, declare, lang,  
            aspect, domClass, keys, query, has, string, topic,
            _FocusMixin, _TemplatedMixin, _WidgetBase, 
            hashUtils, restUtils, i18n){

  'use strict';

  /**
   *  This is a gridx Bar plugin specifically for the Job Instances Grid.
   *  JobInstanceQuerySizer creates a toolbar with predefined query sizes 
   *  that can appear on the gridx footer...which is a gridx Bar.
   *  It identifies the maximum number of elements to query for and 
   *  display in the Job Instances table.
   *  
   *  The value selected is persisted, and then retrieved at query submission
   *  time to append to the query string for the Job Instances table as the pageSize. 
   *  See utility methods for persisting and retrieving the value selected in 
   *  restUtils.js.
   */  

  return declare([_WidgetBase, _TemplatedMixin, _FocusMixin], {
    templateString: '<div class="instanceQuerySizer" role="toolbar" aria-label="${jobSizerAriaLabel}" data-dojo-attach-event="onclick: _changeQuerySize, onmouseover: _onHover, onmouseout: _onHover"></div>',
    
    jobSizerAriaLabel: i18n.SELECT_QUERY_SIZE,
    
    grid: null,
    _tabIndex: -1,
    sizeSeparator: '|',
    sizes: [50, 100, 500, 1000],
    defaultValue: null,
    selectedQuerySize: null,

    constructor: function(args){
      declare.safeMixin(this, args);
      
      var t = this;
      lang.mixin(t, args.grid.nls);  // Makes grid's NLS available to me
      
      if(has('ie') || has('trident')){
        // IE does not support inline-block, so have to set tabIndex
        var gridTabIndex = args.grid.domNode.getAttribute('tabindex');
        t._tabIndex = gridTabIndex > 0 ? gridTabIndex : 0;
      }
    },
    
    postCreate: function(){
      var t = this;
      var g = t.grid;
      
      t.defaultValue = restUtils.JOB_INSTANCES_DEFAULT_QUERY_SIZE;
      
      t.domNode.setAttribute('tabIndex', t.grid.domNode.getAttribute('tabIndex'));
      t.connect(t, 'onFocus', '_onFocus');
      t.connect(t.domNode, 'onkeydown', '_onKey');
      
      // Determine the initial setting for the query size if persisted...
      restUtils.getJobInstancesQuerySize().then(function(response){
        t.selectedQuerySize = response;
        
        if (!t.selectedQuerySize) {
          t.selectedQuerySize = t.defaultValue;
          restUtils.putJobInstancesQuerySize(t.selectedQuerySize);
        }
        if (t.sizes.indexOf(t.selectedQuerySize) === -1) {
          // Size saved is not part of the selected sizes array available to the user
          t.selectedQuerySize = t.sizes[0];
          restUtils.putJobInstancesQuerySize(t.selectedQuerySize);
          // Refresh the grid data with the new page size..
          topic.publish("/dojo/hashchange", hashUtils.getCurrentHash());
        }
        
        t.refresh();
      });    
    },

    refresh: function(){
      var t = this,
          sb = [],
          tabIndex = t._tabIndex,
          separator = t.sizeSeparator,
          currentSize = t.selectedQuerySize;
      var substitute = string.substitute;
      
      for (var i = 0, len = t.sizes.length; i < len; ++i) {
        var querySize = t.sizes[i];

        sb.push('<span class="gridxPagerSizeSwitchBtn ',
                 currentSize === querySize ? 'gridxPagerSizeSwitchBtnActive' : '',  // indicate currently selected size
                '" querysize="', querySize,
                '" title="', substitute(t.pageSizeTitle, [querySize]),
                '" aria-label="', substitute(t.pageSizeTitle, [querySize]),
                '" tabindex="', tabIndex, '">', substitute(t.pageSize, [querySize]),
                '</span>',
                //Separate the "separator, so we can pop the last one.
                '<span class="gridxPagerSizeSwitchSeparator">' + separator + '</span>');
      }
      sb.pop();
      t.domNode.innerHTML = sb.join('');      
    },

    _onChange: function(size, oldSize){
      var dn = this.domNode,
          n = query('[querysize="' + size + '"]', dn)[0];
      
      if(n){
        domClass.add(n, 'gridxPagerSizeSwitchBtnActive');
      }
      n = query('[querysize="' + oldSize + '"]', dn)[0];
      if(n){
        domClass.remove(n, 'gridxPagerSizeSwitchBtnActive');
      }
    },

    _changeQuerySize: function(evt){
      var n = this._findNodeByEvent(evt, 'gridxPagerSizeSwitchBtn', 'instanceQuerySizer');
      if(n){
        var querySize = n.getAttribute('querysize');
        
        // Update the UI to point to the new value
        this._onChange(querySize, this.selectedQuerySize);
        this.selectedQuerySize = this.focusQuerySize = querySize;
        restUtils.putJobInstancesQuerySize(this.selectedQuerySize);
 
        // Refresh the grid with the new page size....
        // This will resubmit the current page query string with the new
        // persisted pageSize value.
        topic.publish("/dojo/hashchange", hashUtils.getCurrentHash());
      }
    },
  
    _onKey: function(evt){
      var t = this;
      var leftKey = t.grid.isLeftToRight() ? keys.LEFT_ARROW : keys.RIGHT_ARROW;
      var hasClass = domClass.contains;
      
      if(evt.keyCode === keys.LEFT_ARROW || evt.keyCode === keys.RIGHT_ARROW) {
        evt.stopPropagation();
        t._focusNextBtn(true, evt.keyCode === leftKey);
      } else if(evt.keyCode === keys.ENTER && hasClass(evt.target, 'gridxPagerSizeSwitchBtn') &&
                !hasClass(evt.target, 'gridxPagerSizeSwitchBtnActive')) {
        evt.stopPropagation();
        t._changeQuerySize(evt);
      }
    },
  
    _focus: function(nodes, node, isMove, isLeft, isFocusable){
      // Try to focus on node, but if node is not focusable, find the next focusable node in nodes 
      // along the given direction. If not found, try the other direction.
      // Return the node if successfully focused, null if not.
      var dir = isLeft ? -1 : 1;
      var i = node ? array.indexOf(nodes, node) + (isMove ? dir : 0) : (isLeft ? nodes.length - 1 : 0);
      var findNode = function(i, dir) {
            while(nodes[i] && !isFocusable(nodes[i])) {
              i += dir;
            }
            return nodes[i];
          };
          
      node = findNode(i, dir) || findNode(i - dir, -dir);
      
      if(node){
        node.focus();
      }
      return node;
    },

    _focusNextBtn: function(isMove, isLeft){
      var t = this,
          c = t.domNode,
          n = query('[querySize="' + t._focusQuerySize + '"]', c)[0];
      var hasClass = domClass.contains;
      
      n = t._focus(query('.gridxPagerSizeSwitchBtn', c), n, isMove, isLeft, function(node){
        return !hasClass(node, 'gridxPagerSizeSwitchBtnActive');
      });
      if(n){
        t._focusQuerySize = n.getAttribute('querysize');
      }
      return n;
    },
        
    _onFocus: function(){
      this._focusNextBtn();
    },
    
    _onHover: function(evt){
      this._toggleHover(evt, 'gridxPagerSizeSwitchBtn', 'instanceQuerySizer', 'gridxPagerSizeSwitchBtnHover');
    },

    _toggleHover: function(evt, targetCls, containerCls, hoverCls){
      var n = this._findNodeByEvent(evt, targetCls, containerCls);
      if(n){
        domClass.toggle(n, hoverCls, evt.type === 'mouseover');
      }
    },
    
    _findNodeByEvent: function(evt, targetClass, containerClass){
      var n = evt.target;
      var hasClass = domClass.contains;
      
      while(!hasClass(n, targetClass)) {
        if(hasClass(n, containerClass)) {
          return null;
        }
        n = n.parentNode;
      }
      return n;
    }

  });
});
