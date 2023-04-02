/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/* jshint strict: false */
define([ "dojo/_base/declare", "dijit/form/Button", "dojo/i18n!../nls/sharedMessages"], 
    function(declare, Button, i18n) {
  
  var BreadcrumbSeparator = declare("BreadcrumbSeparator", [ Button ], {
      id : '',
      baseClass : 'breadcrumbSeparatorButton',
      tabIndex : -2,
      iconClass : 'breadcrumbSeparator',
      
      // The "showLabel: false" throws an internal dojo error.  Using dijitOffScreen to hide the label instead.
      label : "<span class='dijitOffScreen'>" + i18n.BREADCRUMB_SEPARATOR_LABEL + "</span>"
  });
  return BreadcrumbSeparator;
});