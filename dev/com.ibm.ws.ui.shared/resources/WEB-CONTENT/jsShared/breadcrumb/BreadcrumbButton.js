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
define([ "dojo/_base/declare","dijit/form/Button"], 
    function(declare, Button) {
  var BreadcrumbButton = declare("BreadcrumbButton", [ Button ], {
    id : '',
    baseClass : '',// platform.getDeviceCSSPrefix() + "breadcrumb",  //<prhodes> platform may not apply since there's only 1 breadcrumb class
    iconClass : '', // set based on buttonViewType
    title : '', // set based on buttonViewType
    "label" : '', // set same as title
    labelSameAsTitle : true,
    specialLabel : '',
    view : 'single',
    resource : null,
    buttonViewType : '', // "Overview", "Server", "Application", "Instance", etc.
    selected : true,
    showLabel: true
  });
  return BreadcrumbButton;
});