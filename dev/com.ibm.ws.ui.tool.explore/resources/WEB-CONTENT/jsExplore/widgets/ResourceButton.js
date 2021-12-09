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
        "jsExplore/resources/utils",
        "dojo/_base/declare", 
        "dojo/has",
        "dijit/registry",
        "dijit/form/Button",
        "jsShared/utils/imgUtils"
        ], function(
                platform,
                utils,
                declare,
                has,
                registry,
                Button,
                imgUtils
           ){

	var viewPrefix = 'single';
	function __getId(params) {
        var id = viewPrefix + params[0] + params[3] + "Button" + params[1] + params[2];
        return id;
  }
	/**
   * Construct a ResourceButton widget.
   */
	 return {
	   createResourceButton: function(params){
	     var id = __getId(params);
	     resourceButtonWidget = registry.byId(id);
	     if (resourceButtonWidget) {
	       resourceButtonWidget.destroy();
	     }	

	     var ResourceButton = declare("ResourceButton", [ Button ], {
	       constructor : function(params) {          //resourceType, resourceName, buttonResourceName, buttonResourceType
	         this.resourceType = params[0];
	         this.resourceName = params[1];
	         this.buttonResourceName = params[2];
	         this.buttonResourceType = params[3];
	         var spanOpen = "<span dir='" + utils.getBidiTextDirectionSetting() + "'>";
	         var spanClose = "</span>";
	         this.id = __getId(params);
	         this.label = spanOpen + this.buttonResourceName + spanClose;
	       },
	       
         id : '',
         baseClass : "relatedResourceButton",
         label : '',
         resourceType : '',
         resourceName : '',
         buttonResourceName : '',
         buttonResourceType : ''
       });
	     
       return new ResourceButton(params);
     } 

   };
});