/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
define([], function() {
  
var ID = {
    
    /*
     * CONVENTIONS FOR THIS FILE 
     * 
     * Adding variables:
     *   Variable name should be the same as variable string (unless the string starts with a special character or number)
     *   All variable names are written in all caps.
     *   All variable strings (except for those that will always be in all caps) should be written with the first letter uppercase, and all the rest lowercase. ("Explore")
     *   ALL CAPS: make a variable name with _CAPS at the end, and make the string all caps as well
     *   
     * Helper functions:
     *   dashDelimit() : takes in an arbitrary number of arguments, and returns a string with dashes in between each.
     *      dashDelimit("hi", "bob", ID.getExplore()) returns "hi-bob-explore"
     *   commaDelimit() : same as dashDelimit(), but with commas
     *   underscoreDelimit() and dumbDelimit() : please don't use. Use dashes.
     *   camel() : used inside ID.js to convert strings to camelCase. Takes an arbitrary number of arguments and returns a camelCase string.
     *      camel(ID.getApp(), ID.getInstances()) returns "appInstances"
     *      camel() also makes lowercase the first letter after a dash or underscore
     *   getResourceOnResource(): takes two variables, puts the second in parenthesis
     *      getResourceOnResource(ID.getApp(), ID.getServer()): "app(server)" 
     *      
     * Regular functions:
     *   getAnIdString(): gets the string in camelcase ("anIdString")
     *   getAnIdStringUpper(): gets the string with each word capitalized ("AnIdString")
     *   getANIDSTRING(): gets a fully capital string (assuming all-caps variable used)
     *   
     * Note that all files that call an ID function must include "jsExplore/utils/ID" and "ID" in the dojo heading. 
     * To call an ID function within the ID file, use this.getFunctionName(), and ID.getFunctionName() in any other file.
     * The functions and variables are roughly ordered by use and by alphabet, with emphasis on "roughly." 
     * 
     * Please add any function you need!
     */
    
    //TOOLS
    BREADCRUMB : "Breadcrumb",
    PANE : "Pane",
    DASH : "-",
    ID : "Id",
    SEARCH : "Search",
    CONDITION : "Condition",
    DIV : "Div", //should remove
    SEARCH_MAIN_BOX : "search-main-box",

    
    getSearchConditionDiv : function(){
      return this.camel(this.SEARCH + this.CONDITION + this.DIV);
    },
    
    
    camel : function(phrase){
      var segments = phrase.split(this.DASH);
      for(var i = 0; i < segments.length; ++i){
        segments[i] = segments[i].substring(0,1).toLowerCase() + segments[i].substring(1);
      }
      return segments.join(this.DASH);
    },
    
    getBreadcrumbPane : function(){
      return this.camel(this.BREADCRUMB + this.PANE + this.DASH + this.ID);
    }
};
return ID;
});