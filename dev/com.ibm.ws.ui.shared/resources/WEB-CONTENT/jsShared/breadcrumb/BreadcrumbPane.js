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
define(["dojo/_base/declare","dijit/layout/ContentPane"],
    function(declare, ContentPane) {

    var BreadcrumbPane = declare("BreadcrumbPane", [ ContentPane ],
    {
      id : '',
      baseClass : "breadcrumbPane", /* <prhodes> will this vary by device size and type? */
      region : "top",
      resource : null,
      
      setBreadcrumb : function(resource) {
        // Needs to be implemented by extending widgets
      },
      
      resetBreadcrumbPane : function() {
        // Needs to be implemented by extending widgets
      },
      
      __addSeparators : function() {
        // Needs to be implemented by extending widgets
      }
      
    });
    return BreadcrumbPane;
});