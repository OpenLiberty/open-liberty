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
/* jshint strict: false */
define([
        "js/common/platform",
        "dojo/_base/declare", 
        "dijit/layout/ContentPane",
        "jsExplore/widgets/TagButton"
        ], function(
            platform,
            declare,
            ContentPane,
            TagButton
        ){

  var TagPane = declare("TagPane", [ ContentPane ], {
    constructor : function(params) { 
      this.resource = params[0];
      this.tagType = params[1];
      this.resource.subscribe(this);
    },

    resource : null,
    tagType : null,

    postCreate: function() {
      this.populateTags(this.resource, this.tagType);
    },
    
    /*
     * Unsubscribe this observer resource 
     */
    destroy: function() {
      this.inherited(arguments);
      if (this.resource) {
        this.resource.unsubscribe(this);
      }
    }, 


    populateTags : function(resource, tagType) {
      var tags = null;
      if (tagType) {
        switch (tagType) {
        case 'tag':
          tags = resource.tags;
          break;
        case 'owner':
          tags = resource.owner;
          break;
        case 'contact':
          tags = resource.contacts;
          break;
        }
      }

      if (tags) {
        for (var i=0; i < tags.length; i++) {
          this.addChild(TagButton.createTagButton(['searchView', resource.type, resource.id, tagType, tags[i]]));
        }
      }
    },

    onTagsChange: function() {
      if (this.tagType === 'tag') {
        this.destroyDescendants();
        this.populateTags(this.resource, this.tagType);
      }
    },
    
    onOwnerChange: function() {
      if (this.tagType === 'owner') {
        this.destroyDescendants();
        this.populateTags(this.resource, this.tagType);
      }
    },
    
    onContactsChange: function() {
      if (this.tagType === 'contact') {
        this.destroyDescendants();
        this.populateTags(this.resource, this.tagType);
      }
    }

  });
  return TagPane;

});