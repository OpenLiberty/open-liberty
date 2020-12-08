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
define([ "js/common/platform", "dojo/_base/declare", "dojo/has", "dijit/registry", 
         "dijit/form/Button", "dojo/on", "dojo/i18n!../nls/explorerMessages", "dojo/_base/lang",
         "jsExplore/views/viewFactory", "jsExplore/resources/utils", "dojo/dom-class"], 
         function(platform, declare, has, registry, Button, on, i18n, lang, viewFactory, utils, domClass) {

  function __getId(params) {
    var id = params[0] + '-' + params[1] + params[2] + "Tag-" + params[3] + "-" +  params[4];
    return id.replace(/ /g,"_");
  }
  /**
   * Construct a TagButton widget.
   */
  return {
    createTagButton : function(params) {
      var id = __getId(params);
      var tagButtonWidget = registry.byId(id);
      if (tagButtonWidget) {
        tagButtonWidget.destroy();
      }

      var TagButton = declare("TagButton", [ Button ], {
        constructor : function(params) {  // parent View ('objectView' or 'searchView', resource.type, resource.id, tagType, TagName
          this.resourceType = params[1];
          this.resourceId = params[2];
          this.tagType = params[3];
          this.tagName = params[4];
          // setting textDir on the widget will add the correct "dir" to the html even for IE and contextual
          this.textDir = utils.getBidiTextDirectionSetting();
          this.id = __getId(params);
          
          // default to a tag label
          var labelMsg = i18n.TAG_BUTTON_LABEL;
          if (this.tagType === "owner") {
            labelMsg = i18n.OWNER_BUTTON_LABEL;
          } else if (this.tagType === "contact") {
            labelMsg = i18n.CONTACT_BUTTON_LABEL;
          }
          this.label = '<span aria-label="' + lang.replace(labelMsg , [this.tagName]) + '">' + this.tagName + '</span>';
          
          if (params[0] == 'searchView') {
            // A tag on a search view table list has less room to be displayed so reset its styling.
            this.baseClass = 'searchViewMetaDataButton';
          }
        },

        id : '',
        baseClass : "metaDataButton",
        label : '',
        tagType : '',
        tagName : '',

        onClick : function() {
          if (this.tagType === 'tag') {
            viewFactory.openSearchView('type=' + this.resourceType + '&' + this.tagType + '=~eq~' + this.tagName);
          } else if (this.tagType === 'owner' || this.tagType === 'contact') {
              viewFactory.openSearchView(this.tagType + '=~eq~' + this.tagName);
          } else if (this.tagType === 'expand-tag') {
            var tagPane = registry.byId(this.resourceId + "-TagPane");
            var children = tagPane.getChildren();
            for (var i=0; i < children.length; i++) {
              children[i].domNode.style.display = 'inline-block';
            }
            this.domNode.style.display = 'none';
          } else if (this.tagType === 'expand-contact') {
            var contactTagPane = registry.byId(this.resourceId + "-ContactTagPane");
            var children = contactTagPane.getChildren();
            for (var i=0; i < children.length; i++) {
              children[i].domNode.style.display = 'inline-block';
            }
            this.domNode.style.display = 'none';
          }
        }
      });
      return new TagButton(params);
    }
  };
});