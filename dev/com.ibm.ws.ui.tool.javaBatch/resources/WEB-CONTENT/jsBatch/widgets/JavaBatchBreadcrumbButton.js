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
define([ 'dojo/_base/declare', 'jsBatch/utils/viewToHash', 'jsShared/utils/imgUtils', 'jsShared/breadcrumb/BreadcrumbButton',
    'jsBatch/utils/ID', 'dojo/dom-class' ], function(declare, viewToHash, imgUtils, BreadcrumbButton, ID, domClass) {

  'use strict';
  var JavaBatchBreadcrumbButton = declare('JavaBatchBreadcrumbButton', [ BreadcrumbButton ], {

     id : '',
     baseClass : '',
     svgIconId : '', // optional, SVG icon's ID
     svgIconClass : '', // optional, styling for the SVG icon
      postCreate : function() {
        domClass.add(this.iconNode, this.svgIconClass);
        this.iconNode.innerHTML = imgUtils.getSVGSmall(this.svgIconId);
      },
      onFocus : function() {
        domClass.add(this.domNode, "borderFocused");
      },
      onBlur : function() {
        domClass.remove(this.domNode, "borderFocused");
      }
  });
  return JavaBatchBreadcrumbButton;
});