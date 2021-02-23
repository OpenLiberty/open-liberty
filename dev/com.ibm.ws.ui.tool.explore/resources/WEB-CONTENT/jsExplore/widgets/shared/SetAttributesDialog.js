/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
define([
  "dojo/aspect",
  "dojo/dom",
  "dojo/dom-attr",
  "dojo/dom-style",
  "dojo/dom-class",
  "dojo/dom-construct",
  "dojo/dom-geometry", 
  "dojo/has",
  "dojo/keys",
  "dojo/on",
  "dojo/_base/array",
  "dojo/_base/declare",
  "dojo/_base/lang",
  "dojo/store/Memory",
  "dojo/request",
  "dojo/query",
  "dojo/window",
  
  "dijit/_WidgetBase", 
  "dijit/_TemplatedMixin",
  "dijit/_WidgetsInTemplateMixin",

  "dijit/form/Button",
  "dijit/form/ComboBox", 
  "dijit/InlineEditBox",
  "dijit/form/SimpleTextarea",
  "dijit/form/Textarea",
  "dijit/form/TextBox",
  "dijit/Tooltip",

  "dijit/Dialog",
  "dijit/focus",
  "dijit/registry",
  "dijit/layout/utils",

  "jsExplore/resources/utils",
  "jsExplore/resources/_util",
  "dojo/text!../templates/SetAttributesDialog.html",
  "dojo/i18n!../../nls/explorerMessages",
  "jsShared/utils/imgUtils",
  "jsShared/utils/apiMsgUtils",
  "dojo/domReady!"

  ], 
  
  // summary:
  //    A modal dialog for setting Attributes.
  // description:
  //    Pops up a modal dialog window for setting attributes for a resource.  The attributes
  //    include tags, owner, contacts, and notes.  The 'Save' action of the dialog will REPLACE
  //    all existing tags, owner, contacts, and notes with what is displayed on the dialog.

  function(aspect, dom, domAttr, domStyle, domClass, domConstruct, domGeometry,
           has, keys, on, array, declare, lang, Memory, request, query, winUtils,
           _WidgetBase, _TemplatedMixin, _WidgetsInTemplateMixin,
           Button, ComboBox, InlineEditBox, SimpleTextArea, Textarea, 
           TextBox, Tooltip, Dialog, focusUtil, registry, utils, resourceUtils, util,
           template, i18n, imgUtils, apiMsgUtils){
    
  return declare("SetAttributesDialog", [Dialog, _TemplatedMixin, _WidgetsInTemplateMixin], { 
    templateString: template,
    baseClass: "setAttrsDlg",
    resourceNameClass: "setAttrResourceName",     // Class used in dialog title.  Changes when
                                                  // the resource name used in the title contains
                                                  // strings of different sizes like for 
                                                  // 'APP on server x'.
    
    id: "SetAttributesDialog",
    BIDIType: "ltr",
    preferredWidth: 740,            // Preferred width of the dialog
    
    resource: null,                 // The resource that the attributes apply to
    name: "",                       // Resource name
    resourceType: "",               // Resource type....needed to process AppOnServer for server in a cluster as an AppOnCluster
    
    tagsStore: null,
    contactsStore: null,
    
    title: i18n.SET_ATTRIBUTES,
    tagsLabel: i18n.TAGS,
    ownerLabel: i18n.OWNER,
    contactsLabel: i18n.CONTACTS,
    notesLabel: i18n.NOTES,
    notesAriaLabel: i18n.NOTES,
    saveButtonLabel: i18n.SAVE,
    tagInvalidChars: i18n.TAGINVALIDCHARS,
    
    // Error Message field.
//unused    setAttrsDlgErrMsg: "",
    errorIconMessage: i18n.ERROR_ALT,
    
    // Fields to help with selecting multiple pills for an action, like delete.
    pillSelection: {widget: null},        // Widget is the pill widget currently selected with 
                                          // Shift/Ctrl + click.
    pillNumber: 0,                        // An incrementor to track the order pills are added 
                                          // to the dialog.
    
    // Input field types - NOT PII - used internally by the code to indicate the type of
    // input value.
    TYPE_TAG: 'tag',
    TYPE_OWNER: 'owner',
    TYPE_CONTACT: 'contact',
    
    constructor: function(args) {
      declare.safeMixin(this, args);
      
      if (this.resource.type === "appOnServer" && this.resource.cluster) {
        // If we are viewing an app on a server and that server belongs to a cluster then 
        // we are going to show the metadata set on the App On Cluster.  The dialog should
        // view as AppOnCluster.
        this.resourceType = "appOnCluster";
      } else {
        this.resourceType = this.resource.type;
      }

      this.name = this._getResourceName(this.resource);
      
      this.BIDIType = resourceUtils.getBidiTextDirectionSetting();
      
      this.pillSelection.widget = null;

    },
    
    postMixInProperties:function(){
      this.inherited(arguments);      
    },
       
    postCreate: function() {
      this.inherited(arguments);

      this.resourceIconNode.innerHTML = this._setResourceIcon(this.resource);
      if (this.resource.type === "appOnServer" && this.resource.cluster) {
        // If we are viewing an app on a server and that server belongs to a cluster then 
        // we are going to show the metadata set on the App On Cluster. Notify users.
        dom.byId("clusteredMetadataMsgContainer").innerHTML = i18n.METADATA_WILLBE_INHERITED;
      } else {
        dom.byId("clusteredMetadataMsgContainer").innerHTML = "";
      }
      
      this._getExistingTags();      
      this._getExistingContacts();
      
      // Add existing attributes to the dialog.
      this._setAttributePills(this.resource);
      
      // Add existing note to the dialog.
      if (this.resource.note) {
        this.notesAttrsPane.set('value', this.resource.note);
      }
      
      on(this.saveButton, "click", lang.hitch(this, this._save));
    },
       
    /** 
     * Invoked when the 'X' or ESC key is used to dismiss the dialog
     * because Dialog invokes hide after onCancel (see Dialog.js).
     */
    hide: function() {
      this.destroyRecursive();
    },
    
    /**
     * Adjusts Dialog as necessary to keep it visible when the browser window is resized.
     * The dialog itself is not re-sizable...The dialog resizes to stay within the browser
     * viewport.
     * 
     * Overrides resize() in dijit/Dialog.
     *  - The action button ('Save') on this dialog was placed so that it is not
     *    part of the containerNode.  Instead it comes beneath the containerNode
     *    and should always be visible.  This overridden resize() ensures that the
     *    action button is always visible and only adds scrolling to the containerNode.
     *  - The natural size of the contents of the dialog when no pills are defined 
     *    can be quite a bit smaller than the preferred width.  This overriden 
     *    resize() ensures that the dialog will be displayed as wide as it can be, 
     *    up to a maximum of the preferred width, when the browser window is resized. 
     * 
     * @param dim
     */
    resize: function(dim){
      if(this.domNode.style.display != "none") {

        if(!dim){
          if(this._alteredSize){
            // If we earlier adjusted the size of the dialog to maximize the space in the 
            // viewport, reset it to its natural size. Below,
            // the size of each component will be recalculated based on the size available in 
            // the viewport for the dialog.
            array.forEach([this.domNode, this.containerNode, this.titleBar, this.actionBarNode], function(node){
              domStyle.set(node, {
                position: "static",
                width: "auto",
                height: "auto"
              });
            });
            this.domNode.style.position = "absolute";
          }

          // If necessary, alter the size of the Dialog to fit in the viewport and have some 
          // space around it to visually indicate that it's a popup.  Attempt to size the 
          // dialog so that it is the maximum size it can be for the viewport size up to the 
          // preferred width set in this widget.
          var viewport = winUtils.getBox(this.ownerDocument);
          viewport.w *= this.maxRatio;
          viewport.h *= this.maxRatio;

          var bb = domGeometry.position(this.domNode);  // Gets the size of the dialog
                                                        // as it is currently laid out.
          
          if (!this.preferredWidth) this.preferredWidth = 740;
          
          if (bb.w >= viewport.w || bb.h >= viewport.h) {
            // Alter the size of the dialog.....
            var myWidth = viewport.w;
            if (bb.w <= myWidth) {
              viewport.w > this.preferredWidth ? myWidth = this.preferredWidth: myWidth = viewport.w;
            }
            dim = {
              w: myWidth,
              h: Math.min(bb.h, viewport.h)
            };
            this._alteredSize = true;
          }else{
            if (bb.w < viewport.w && bb.w != this.preferredWidth) {   
              // Maximize the width....bb width is often smaller than 740 especially if there are no pills.
              dim = {
                w: viewport.w < this.preferredWidth? viewport.w: this.preferredWidth,
                h: Math.min(bb.h, viewport.h)
              };
              this._alteredSize = true;
            } else {  // everything is fitted up to its max width for the current viewport!
              this._alteredSize = false;
            }
          }
        }

        if(dim){
          // Altering the size of the dialog....
          // Set this.domNode to specified size
          domGeometry.setMarginBox(this.domNode, dim);

          // And then size this.containerNode
          var contentDim = utils.marginBox2contentBox(this.domNode, dim),
              titleBarSize = {domNode: this.titleBar, region: "top"},
              centerSize = {domNode: this.containerNode, region: "center"},
              actionBarSize = {domNode: this.actionBarNode, region: "bottom"};
          utils.layoutChildren(this.domNode, contentDim,
                               [titleBarSize, actionBarSize, centerSize]);

          // Set this.containerNode to show a scrollbar if it's overflowing.
          this.containerNode.style.overflow = "auto";
          this._layoutChildren();   // send resize() event to all child widgets
        } else {
          this._layoutChildren();   // send resize() event to all child widgets
        }

        if(!has("touch") && !dim){
          // If the user has scrolled the viewport then reposition the Dialog.  But don't do it for touch
          // devices, because it will counteract when a keyboard pops up and then the browser auto-scrolls
          // the focused node into view.
          this._position();
        }
      }
    },
    
    close: function() {
      this.destroyRecursive();
    }, 
    
    startup: function(){
      this.inherited(arguments);
      // Display content
      if(this.containerNode.innerHTML){
        domStyle.set(this.contentWrapper, "display", "block");
      }
    },
    
    /**
     * Get the resource name as it will be displayed in the title of the dialog.
     * 
     * @param resource
     * 
     * @returns {String}  HTML encapsulating the resource name
     */
    _getResourceName: function(resource) {
      switch (this.resourceType) {
        case 'cluster':
        case 'host':
        case 'server':
        case 'standaloneServer':
          var spanOpen = '<span dir="' + resourceUtils.getStringTextDirection(resource.name) + '">';
          var spanClose = '</span>';
          return spanOpen + resource.name + spanClose;
          
        case 'runtime':
          this.resourceNameClass = "setAttrResourceOnResourceName";
          var runtime = resource;
          var spanOpen1 = '<span dir="' + resourceUtils.getStringTextDirection(runtime.name) + '" class="setAttrResourceName">';
          var spanOpen2 = '<span dir="' + resourceUtils.getStringTextDirection(runtime.host.id) + '" class="setAttrResourceOnResourceName">';
          var spanClose = '</span>';
          
          var content = '<span>'
              + lang.replace(i18n.SETATTR_RUNTIME_NAME, [ spanOpen1 + runtime.name + spanClose,
                                                           spanOpen2 + runtime.host.id + spanClose ]) + '</span>';
          return content;

        case 'appOnServer':
          this.resourceNameClass = "setAttrResourceOnResourceName";
          
          // Check if this is an application on a server belonging to a cluster.  If so, 
          // the dialog should display as an AppOnCluster.
          if (!resource.cluster) {            
            var appOnServer = resource;
            var spanOpen1 = '<span dir="' + resourceUtils.getStringTextDirection(appOnServer.name) + '" class="setAttrResourceName">';
            var spanOpen2 = '<span dir="' + resourceUtils.getStringTextDirection(appOnServer.server.name) + '" class="setAttrResourceOnResourceName">';
            var spanClose = '</span>';

            var content = '<span>'
                + lang.replace(i18n.RESOURCE_ON_SERVER_RESOURCE, [ spanOpen1 + appOnServer.name + spanClose,
                                                          spanOpen2 + appOnServer.server.name + spanClose ]) + '</span>';
            return content;
          }
          // else fall through to AppOnCluster....

        case 'appOnCluster':
          this.resourceNameClass = "setAttrResourceOnResourceName";
          
          var appOnCluster = resource;
          var clusterName = (typeof appOnCluster.cluster === 'string') ? appOnCluster.cluster: appOnCluster.cluster.name;
          var spanOpen1 = '<span dir="' + resourceUtils.getStringTextDirection(appOnCluster.name) + '" class="setAttrResourceName">';
          var spanOpen2 = '<span dir="' + resourceUtils.getStringTextDirection(clusterName) + '" class="setAttrResourceOnResourceName">';
          var spanClose = "</span>";

          var content = '<span>'
              + lang.replace(i18n.RESOURCE_ON_CLUSTER_RESOURCE, [ spanOpen1 + appOnCluster.name + spanClose,
                                                        spanOpen2 + clusterName + spanClose ]) + '</span>';
          return content;   

        default:
          console.error('SetAttributesDialog.getResourceName called for an unknown resource type: ' + this.resourceType);
      }
    },

    /**
     * Set the icon in the title of the dialog to represent the resource type.
     * 
     * @param resource
     * 
     * @returns {String}  HTML image tag
     */
    _setResourceIcon: function(resource) {
      var icon = "";

      switch (this.resourceType) {
        case 'appOnCluster':
        case 'appOnServer':
        case 'cluster':
        case 'host':
          icon = this.resourceType;
          break;
        case 'server':
          if(resource.isCollectiveController) {
            icon = "collectiveController";
          } else {
            icon = "server";
          }
          break;
        case 'standaloneServer':
          // Standalone servers doesn't have tag/metadata
          break;
        case 'runtime':
          icon = "runtime";            
          switch (resource.runtimeType) {
            case constants.RUNTIME_LIBERTY:
              icon = "runtimeLiberty";
              break;
            case constants.RUNTIME_NODEJS:
              icon = "node";
              break;
            default:
              console.error('Runtime resource with unknown runtimeType: ' + resource.runtimeType);
          }
          break;
  
        default:
          console.error('Tags&Metatdata dialog _setResourceIcon called for an unknown resource type: ' + this.resourceType);
      }
      
      if (resource.scalingPolicyEnabled) {
        icon = icon + "-autoscaled";
      } else {
        icon = icon + "-selected";
      }

      return imgUtils.getSVGSmall(icon);
    },

    /** 
     * Each attribute (value) for tags, owner, or contacts is represented as a 'pill'
     *  in the appropriate section.
     *  
     * @param resource  
     */
    _setAttributePills: function(resource) {
      // *****************
      // Tags section
      // *****************
      // Add the new tag field
      this.tagNewEditBox = this._createEditField(this.TYPE_TAG);
      this.tagAttrsPane.addChild(this.tagNewEditBox);
      this.attributeTagsIcon.innerHTML = imgUtils.getSVGSmall('metadata-tag');
            
      // After completing editing in the new Tag Edit field,
      // create a pill for the new value.
      aspect.after(this.tagNewEditBox, 
                   "onBlur", 
                   lang.hitch(this, function() {
                     var newTagWidget = registry.byId("newTagEditField");
                     var value = newTagWidget.get('value');
                     if (newTagWidget.isValid() && !newTagWidget._isEmpty(value)) {
                       newTagWidget.set('value', "");
                       // If a pill does not exist for this value, then create one
                       var match = false;
                       var attrNodeList = query('.attrButton', 'attributeTagsDiv');
                       for (var i=0; i<attrNodeList.length; i++) {
                         // Get the widget associated with this node...
                         var pill = registry.byNode(attrNodeList[i]);
                         if (pill.get('value') == value) {
                           match = true;
                           break;
                         }
                       }
                       if (!match) {
                         this._addPill(value, true, this.tagNewEditBox.domNode, this.TYPE_TAG);
                       }
                       
                       newTagWidget.focus();                      
                     }
                   }));
      
      if (resource.tags) {
        var tags = resource.tags;  
        for (var i=0; i<tags.length; i++) {
          this._addPill(tags[i], false, this.tagNewEditBox.domNode, this.TYPE_TAG);
        }
      }
      
      // If you click anywhere within the tagAttrsPane, put the cursor in the newTagEditField.
      on(this.tagAttrsPane, ".attributesEditArea:click", lang.hitch(this, function(e){
        if (e.target.tagName == "DIV") {
          focusUtil.focus(dom.byId("newTagEditField"));
        }
        // Remove any 'selected' indication on pills
        query(".selectedAttribute").forEach(function(selectedPillNode) {
          domClass.remove(selectedPillNode, "selectedAttribute");
        });
        // Reset the pillSelection
        this.pillSelection.widget = null;
      }));
      

      // *****************
      // Owner section
      // *****************
      // Add the new owner field.
      this.ownerNewEditBox = this._createEditField(this.TYPE_OWNER); 
      this.ownerAttrsPane.addChild(this.ownerNewEditBox);
      this.attributeOwnerIcon.innerHTML = imgUtils.getSVGSmall('metadata-user');

      // After completing editing in the new Owner Edit field, 
      // create a pill for the new value.
      aspect.after(this.ownerNewEditBox, 
          "onBlur", 
          lang.hitch(this, function() {
            var value = this.ownerNewEditBox.get('value');
            if (this.ownerNewEditBox.isValid() && !this.ownerNewEditBox._isEmpty(value)) {
              this.ownerNewEditBox.set('value', "");
              var newOwner = this._addPill(value, true, this.ownerNewEditBox.domNode, this.TYPE_OWNER);
              domStyle.set(this.ownerNewEditBox.domNode, { visibility: "hidden" });
              this.ownerNewEditBox.set('disabled', true);
              // For the Owner field, focus is set to owner pill just created since
              // the new owner edit field is now hidden.
              focusUtil.focus(newOwner.domNode);
            }
          }));        
      
      if (resource.owner) {
        this._addPill(resource.owner, false, this.ownerNewEditBox.domNode, this.TYPE_OWNER);
        domStyle.set(this.ownerNewEditBox.domNode, { visibility: "hidden" });
        this.ownerNewEditBox.set('disabled', true);
      } 
        
      // Clicking within the ownerAttrsPane....
      on(this.ownerAttrsPane, ".attributesEditArea:click", lang.hitch(this, function(e){
        if (e.target.tagName == "DIV") {
          var ownerEditField = registry.byId("newOwnerEditField").domNode;
          if (ownerEditField.style.visibility !== "hidden") {
            // If there is no owner pill, 
            // put the cursor in the ownerNewEditBox field (id = newOwnerEditField).
            focusUtil.focus(dom.byId("newOwnerEditField"));
          } else {
            // Put the focus on the owner pill
            var attrNodeList = query('.attrButton', 'attributeOwnerDiv');
            if (attrNodeList.length > 0) {
              var pill = registry.byNode(attrNodeList[0]);
              focusUtil.focus(pill.domNode);
            }            
          }
        }
        // Remove any 'selected' indication on pills
        query(".selectedAttribute").forEach(function(selectedPillNode) {
          domClass.remove(selectedPillNode, "selectedAttribute");
        });
        // Reset the pillSelection
        this.pillSelection.widget = null;
      }));


      // *****************
      // Contacts section
      // *****************
      // Add the new contact field
      this.contactNewEditBox = this._createEditField(this.TYPE_CONTACT);
      this.contactAttrsPane.addChild(this.contactNewEditBox);
      this.attributeContactsIcon.innerHTML = imgUtils.getSVGSmall('metadata-contacts');
      
      // After completing editing in the new Contact Edit field, 
      // create a pill for the new value.
      aspect.after(this.contactNewEditBox, 
                   "onBlur", 
                   lang.hitch(this, function() {
                     var newContactWidget = registry.byId("newContactEditField");
                     var value = newContactWidget.get('value');
                     if (newContactWidget.isValid() && !newContactWidget._isEmpty(value)) {
                       newContactWidget.set('value', "");
                       // if a pill does not exist for this value, then create one
                       var match = false;
                       var attrNodeList = query('.attrButton', 'attributeContactsDiv');
                       for (var i=0; i<attrNodeList.length; i++) {
                         // Get the widget associated with this node...
                         var pill = registry.byNode(attrNodeList[i]);
                         if (pill.get('value') == value) {
                           match = true;
                           break;
                         }
                       }
                       if (!match) {
                         this._addPill(value, true, this.contactNewEditBox.domNode, this.TYPE_CONTACT);
                       }
                       
                       newContactWidget.focus();                      
                     }
                   }));
      
      if (resource.contacts) {
        var contacts = resource.contacts;      
        for (var i=0; i<contacts.length; i++) {
          this._addPill(contacts[i], false, this.contactNewEditBox.domNode, this.TYPE_CONTACT);
        }
      }
      
      // If you click anywhere within the tagAttrsPane, put the cursor in the newTagEditField.
      on(this.contactAttrsPane, ".attributesEditArea:click", lang.hitch(this, function(e){
        if (e.target.tagName == "DIV") {
          focusUtil.focus(dom.byId("newContactEditField"));
        }
        // Remove any 'selected' indication on pills
        query(".selectedAttribute").forEach(function(selectedPillNode) {
          domClass.remove(selectedPillNode, "selectedAttribute");
        });
        // Reset the pillSelection
        this.pillSelection.widget = null;
      }));
      
      // *****************
      // Notes section
      // *****************
      this.attributeNotesIcon.innerHTML = imgUtils.getSVGSmall('metadata-notes');
      on(this.notesAttrsPane, "click", lang.hitch(this, function(e) {
        // Remove any 'selected' indication on pills
        query(".selectedAttribute").forEach(function(selectedPillNode) {
          domClass.remove(selectedPillNode, "selectedAttribute");
        });        
        // Reset the pillSelection
        this.pillSelection.widget = null;
     }));
    }, 
    
    /**
     * Get a list of all the unique tags across all resources.
     */
    _getExistingTags: function() {
      this.tagsStore = new Memory();
      
      var url = '/IBMJMXConnectorREST/mbeans/WebSphere:feature=collectiveController,name=AdminMetadataManager,type=AdminMetadataManager/attributes/Tags';
      var options = {
          handleAs : 'json',
          headers : {
            'Content-type' : 'application/json'
          },
          sync: true
      };

      request.get(url, options).then(lang.hitch(this, function(response) {
          // response.value is an array of strings (tags).  Add each to the 
          // dojo store to be used in the ComboBoxes for each tag.
          for (var i=0; i<response.value.length; i++) {
            this.tagsStore.put({name: response.value[i]});
          }
          
      }), lang.hitch(this, function(err) {
        console.log('Error ' + err.response.status + ' occurred when requesting ' + url + ': ', err);
        dom.byId("setAttrsDlgErrMsg").innerHTML =  i18n.ERROR_GET_TAGS_METADATA;
        // Show the message div
        domStyle.set(dom.byId("messageDiv"), "display", "inline");

      }));
    
    },
    
    /**
     * ** TEMPORARILY set the list of existing contacts to an empty set until 
     *    an API to retrieve all unique contacts across all resources has been created.
     */
    _getExistingContacts: function() {
      this.contactsStore = new Memory();
    },
    
    /**
     * Adds an edit field for defining a new tag, owner, or contact.
     * 
     * @param attrType    The type of pill: TYPE_TAG, TYPE_OWNER, or TYPE_CONTACT  
     * 
     * @return A dijit comboBox entry field.
     */
    _createEditField: function(attrType) {

      var fieldID = "";
      var store = this.contactsStore;
      var label = "";
      switch(attrType) {
        case this.TYPE_TAG:
          fieldID = "newTagEditField";
          store = this.tagsStore;
          label = i18n.TAGS_LABEL;
          break;
        case this.TYPE_OWNER:
          fieldID = "newOwnerEditField";
          label = i18n.OWNER;
          break;
        case this.TYPE_CONTACT:
          fieldID = "newContactEditField";
          label = i18n.CONTACTS;
          break;
      }
      
      var newEditBox = new ComboBox({
        id: fieldID,
        value: "", 
        "aria-label": label,
        store: store,
        attrType: attrType,
        searchAttr: "name", 
        "class": "newAttributeEditField attrComboBox",
        lowercase: attrType == this.TYPE_TAG? true: false,  // Tags are all lowercase
        ignoreCase: true,
        trim: true,
//        queryExpr: "*${0}*",
        textDir: this.BIDIType,
        scrollOnFocus: true,
        intermediateChanges: true,
        setTabIndex: true,
        onChange: function(newValue) {
          // On the first pass, set tabIndex=-1 for the hidden previous & next buttons or batchScan will fail with a false positive
          if (this.setTabIndex) {
            domAttr.set(this.dropDown.previousButton, "tabIndex", "-1");
            domAttr.set(this.dropDown.nextButton, "tabIndex", "-1");
            this.setTabIndex = false;
          }
          // The following is needed to get immediate feedback when an 
          // invalid character ('/', '<', '>') is typed in tag field.
          if (!this.validate()) {
            this.focus();
          }
        }, 
        onBlur: attrType == this.TYPE_TAG? function() {
          // Remove any spaces that may be in a string pasted into the
          // entry field.  Spaces are not allowed in tags.
          var val = this.get('value').replace(/\s/g, '');
          this.set('value', val);
          }: function() {},
        validator: this._validateTag,
        invalidMessage: this.tagInvalidChars,
        displayMessage: this._showInvalidMessage
      });
      
      on(newEditBox, "keypress", this._attrEntryNavigation);
      
      on(newEditBox, "keydown", 
         function(evt) {
           var charOrCode = evt.charCode || evt.keyCode;
           switch (charOrCode){
             case keys.ESCAPE:   // ESC registers on keydown, not keypress
               // Prevent exiting from dialog when ESC selected from entry field.
               // Just reset the entry field.
               evt.preventDefault();  
               evt.stopPropagation();
               this.reset();
             break;
          
             default: 
             break;
           }
         }
      );
            
      return newEditBox;
    }, 
    
    /**
     * Keypress event handler for the new value edit fields (the Comboboxes).
     * 
     * @param evt
     */
    _attrEntryNavigation: function(evt) {
      var charOrCode = evt.charCode || evt.keyCode;
      var nextFocus = null;

      switch (charOrCode){
        case keys.SPACE:
        case 44: //comma
          if (this.attrType == 'tag') {           
            this._abortQuery();
            if (this._opened) {
              this.set('value', this._lastInput);
              this.closeDropDown(this.focused);
            } 
            if (this.isValid()) {
              evt.preventDefault();
              evt.stopPropagation();
              if (!this._isEmpty(this.get('value'))) {              
                this.onBlur();
              }
            } else { // value was not valid when key was hit.  Don't process value.
              evt.preventDefault();
              evt.stopPropagation();
            }
          }
        break;
        
        case keys.ENTER:
        case keys.TAB:
          if (this.isValid()) {
            if (!this._isEmpty(this.get('value'))) {              
              evt.stopPropagation();
              evt.preventDefault();
              this.onBlur();
            }            
          } else { // value was not valid when key was hit.  Don't process value.
            evt.preventDefault();
            evt.stopPropagation();
          }
          break;
          
        case keys.LEFT_ARROW:
          // Determine the previous pill in this section.  If the new entry edit field
          // is the first field in the section, then let focus remain there.
          nextFocus = this.domNode.previousSibling;
          if (nextFocus && domClass.contains(nextFocus, "dijitOffScreen")) {
            // nextFocus is pointing to the currently off-screen editor for the 
            // previous pill.  Set nextFocus to this pill's onscreen InlineEditbox.
            nextFocus = nextFocus.previousSibling;
          } else if (!nextFocus) {
            nextFocus = this.domNode;
          } 
        case keys.RIGHT_ARROW:
          if (!nextFocus) {            
            // The new value edit fields are always the last in their container (section)
            // So, a right arrow here loops the focus back to the first pill in the 
            // section.
            nextFocus = this.domNode.parentElement.firstChild;
            if (nextFocus && domClass.contains(nextFocus, "dijitOffScreen")) {
              // nextFocus is pointing to the currently off-screen editor for the 
              // next pill.  Set nextFocus to this pill's onscreen InlineEditbox.
              nextFocus = nextFocus.nextSibling;
            }
          }
          if (nextFocus) {   // Reset focus
            var nextWidget = registry.byNode(nextFocus);
            if (nextWidget.focus) {
              nextWidget.focus();
            } else {                    
              focusUtil.focus(nextFocus);
            }
          }
          break;
          
        default:
          break;
      }
    },
    
    /**
     * Adds an attribute pill to the dialog.
     * 
     * @param value   Value of the attribute.
     * @param newTag  True if this attribute was newly created by the user and 
     *                therefore should receive a green "new" indication border.
     *                False if this pill represents a known attribute value for 
     *                this resource, as defined in admin-metadata.xml.
     * @param placementNode  domNode of where to place the new attribute pill. 
     * @param attrType       The type of pill: TYPE_TAG, TYPE_OWNER, or TYPE_CONTACT                                   
     */
    _addPill: function(value, newTag, placementNode, attrType) {
      // Hook up the correct store of pre-existing values.
      var store = this.contactsStore;  // Owner and Contact fields
      if (attrType == this.TYPE_TAG) {
        store = this.tagsStore;
      }
            
      if (value !== "") {
        var newIEB = new InlineEditBox({
          textDir: this.BIDIType,
          pillSelection: this.pillSelection,		// Points back to currently selected Pill on whole dialog.
          pillNumber: this.pillNumber,              // Indicates the order this pill field was added to dialog.
          attrType: attrType,
          title: value,
          editor: ComboBox,
          editorParams: {value: value, 
                         store: store, 
                         attrType: attrType,
                         searchAttr: "name", 
                         "class": "attrComboBox", 
                         "aria-label": value,
                         lowercase: attrType == this.TYPE_TAG? true: false,  // Tags are all lowercase
                         ignoreCase: true,
                         trim: true,
//                         queryExpr: "*${0}*",
                         textDir: this.BIDIType,
                         validator: this._validateTag,
                         invalidMessage: this.tagInvalidChars,
                         onBlur: function() {
                           if (this.attrType == 'tag') {                             
                             // Remove any spaces that may be in a string pasted into the
                             // edit entry field.  Spaces are not allowed in tags.
                             var val = this.get('value').replace(/\s/g, '');
                             this.set('value', val);
                           }
                         },
                         displayMessage: this._showInvalidMessage
                        },
            srcNodeRef: domConstruct.create("div",
                        {innerHTML: value,
                         "class": "attrButton",
                         style: "display: inline-block;"
                        }),
           nextToFocus: function() {
                         // Determine the next pill, or new attribute edit field, in 
                         // this pill's section.
                         var nextFocus = this.domNode.nextSibling;
                         if (nextFocus && domClass.contains(nextFocus, "dijitOffScreen")) {
                           // nextFocus is pointing to the currently off-screen editor for the 
                           // next pill.  Set nextFocus to this pill's onscreen InlineEditbox.
                           nextFocus = nextFocus.nextSibling;
                         } 
                         return nextFocus;
                        },
       previousToFocus: function() {
                         // Determine the previous pill in this section.
                         var nextFocus = this.domNode.previousSibling;
                         if (nextFocus && domClass.contains(nextFocus, "dijitOffScreen")) {
                           // nextFocus is pointing to the currently off-screen editor for the 
                           // previous pill.  Set nextFocus to this pill's onscreen InlineEditbox.
                           nextFocus = nextFocus.previousSibling;
                         }
                         return nextFocus;
                        },
        removeSelected: function(global) {
                         // Remove 'selected' indication from pills.
                         // If global, remove 'selected' indicator from all pills in all sections.
                         // If not global, remove 'selected' indicator from pills in this pill's section.
                         if (global) {
                           query(".selectedAttribute").forEach(function(selectedPillNode) {
                             domClass.remove(selectedPillNode, "selectedAttribute");
                           });                           
                         } else {
                           var containerId = this.domNode.parentNode.id;
                           query('.selectedAttribute', containerId).forEach(function(selectedPillNode) {
                             domClass.remove(selectedPillNode, "selectedAttribute");
                           });
                         }
                         // Reset the pill selection.
                         this.pillSelection.widget = null;
                        },
        deleteSelected: function() {
                         // Remove all pill's in this pill's section that were 'selected'.
                         var containerId = this.domNode.parentNode.id;
                         query('.selectedAttribute', containerId).forEach(function(selectedPillNode) {
                           registry.byId(selectedPillNode.id).destroyRecursive();
                         });                         
                        }               
        });
        
        this.pillNumber++;   // Increment for the next pill that will be created.
        
        if (newTag) {
          // Newly added attributes are bordered in green and appear with a green circle.
          domClass.add(newIEB.domNode, "newAttrButton");  
          // 177381 - The green dot should appear on the left side of the pill. 
          //          Check the dir attribute value (for BIDI) of the html div
          //          created as part of the InlineEditBox template to see which
          //          way the text is flowing for this newly created pill and 
          //          position the green dot accordingly.
          var direction = domAttr.get(newIEB.domNode, "dir");
          var position = "first";
          if (direction == "rtl") {
            position = "last";
          }
          domConstruct.place(domConstruct.create("div", {"class": "circleNew"}),
                             newIEB.domNode, position);
        }
      
        // Place the new pill at the end of the existing pills, before the 
        // edit node for the section.
        domConstruct.place(newIEB.domNode, placementNode, "before");
      
        // Set additional keyboard navigation on the edit widget associated
        // with the InlineEditBox.  However, the edit widget is not created 
        // until the first time that you click on the InlineEditBox.  So, 
        // provide a sort of hacky way to initialize the wrapperWidget that
        // wraps the edit widget.
        newIEB.edit();
        newIEB.cancel();
        if (attrType == this.TYPE_TAG) {    // For tags, space and comma indicate end of entry.          
          on(newIEB.wrapperWidget.editWidget, "keypress", 
             lang.hitch(newIEB.wrapperWidget, function(evt) {
               var charOrCode = evt.charCode || evt.keyCode;
               switch (charOrCode){
                 // Pressing the space or comma key while in the editor 
                 // associated with the InlineEditBox FOR A TAG should indicate 
                 // that editing is complete and 'save' off the new value in the 
                 // display widget for the InlineEditBox.
                 // InlineEditBox already catches ENTER and processes it as a 'save'.
                 case keys.SPACE:
                 case 44: //comma
                   this._onChange();   // This will fire _onBlur and then 'save'
                   if (this.isValid()) {
                     focusUtil.focus(this.inlineEditBox.displayNode);
                   }
                   break;

                 default:
                   break;
               }
             })
          );
        }
        
        // When within the editor of the InlineEditBox, and Enter (submit) is selected,
        // force the editor to show input errors.
        // Normally, the combobox editor widget would only display the error indication
        // after tabbing or stepping away from the widget via the mouse.  But now Enter 
        // works as a 'submit' for the InlineEditBox widget to update the pill with the 
        // new value if it is valid.  Therefore, if it is not valid, show the error msg
        // prior to leaving the combobox editor within the InlineEditBox.
        on(newIEB.wrapperWidget.editWidget, "keydown", 
            function(evt) {
              if (!this._destroyed) {                
                var charOrCode = evt.charCode || evt.keyCode;
                switch (charOrCode){
                  case keys.ENTER:
                    this.validate(false);
                    break;
                  case keys.ESCAPE:
                    this.state = "";  // Remove any error state since ESC === reset value.
                    break;
                }
             }
            });    

		// _onClick handler enhanced to first check if either Shift or CTRL was selected 
		// with the click.   If so, do not enter the editor but highlight the pill as a
		// selected attribute for an action, such as delete.
		//
		// Selected pills in a section will lose their selection indication if a user
		//  - re-selects the selected pill with CTRL + click
		//  - clicks away from the selected pill without CTRL or Shift
		//  - clicks on a pill with CTRL or Shift that is in a different section than the
        //    selected pill.
        aspect.around(newIEB, '_onClick', function(origMethod) {
          return function(evt){
            if(this.disabled){
              return;
            }
            
            if (evt) {
              if (evt.ctrlKey || evt.shiftKey || evt.metaKey) {                
                if (evt.ctrlKey) {    
                  // CTRL + click acts as a selection toggle for a pill.
                  if (domClass.contains(this.domNode, "selectedAttribute")) {
                    domClass.remove(this.domNode, "selectedAttribute");
                  } else {
                    domClass.add(this.domNode, "selectedAttribute");  
                  }
                  // mark this one as the currently selected pill.
                  this.pillSelection.widget = this;
                } else if (evt.shiftKey) {
                  // Shift can be used to select a single pill or act as 
                  // the beginning or ending of a range of pills to mark
                  // selected.
                  if (!this.pillSelection.widget) {
                    // mark this one as the currently selected pill
                    // so the next pill selected with Shift marks a
                    // range of pills selected.
                    this.pillSelection.widget = this;
                    domClass.add(this.domNode, "selectedAttribute");
                  } else {
                      // remove all other selected attributes in the container because this is the 
                      // way it works.   Then, add selectedAttribute to this.pillSelection and this pill
                      // to mark the beginning and end of a range of pills selected and then mark all
                      // pills between. 
                      query(".selectedAttribute", this.domNode.parentNode).forEach(function(selectedPillNode) {
                        domClass.remove(selectedPillNode, "selectedAttribute");
                      });  
                      domClass.add(this.pillSelection.widget.domNode, "selectedAttribute");
                      domClass.add(this.domNode, "selectedAttribute");
                      if (this.attrType == this.pillSelection.widget.attrType) {
                        // Determine the pills between this one and the selected one and mark
                        // all as selected.
                        if (this.pillNumber > this.pillSelection.widget.pillNumber) {
                          // Select all pills from pillSelection.widget to this pill - 
                          var nextPill =  this.pillSelection.widget.domNode.nextSibling;                        
                          if (nextPill && domClass.contains(nextPill, "dijitOffScreen")) {
                            // nextPill is pointing to the currently off-screen editor for the 
                            // next pill.  Set nextPill to this pill's onscreen InlineEditbox.
                            nextPill = nextPill.nextSibling;
                          }                            
                          while (!domClass.contains(nextPill, "newAttributeEditField")  &&
                                 nextPill !== this.domNode) {                            
                            domClass.add(nextPill, "selectedAttribute");
                            nextPill = nextPill.nextSibling;
                            if (nextPill && domClass.contains(nextPill, "dijitOffScreen")) {
                              // nextPill is pointing to the currently off-screen editor for the 
                              // pill.  Set nextPill to this pill's onscreen InlineEditbox.
                              nextPill = nextPill.nextSibling;
                            }                            
                          }                          
                        } else {
                          // Select all pills from this pill to pillSelection.widget -
                          var nextPill =  this.domNode.nextSibling;
                          if (nextPill && domClass.contains(nextPill, "dijitOffScreen")) {
                            // nextPill is pointing to the currently off-screen editor for the 
                            // pill.  Set nextPill to this pill's onscreen InlineEditbox.
                            nextPill = nextPill.nextSibling;
                          }                            
                          while (!domClass.contains(nextPill, "newAttributeEditField") &&
                              nextPill != this.pillSelection.widget.domNode) {                            
                            domClass.add(nextPill, "selectedAttribute");
                            nextPill=nextPill.nextSibling;
                            if (nextPill && domClass.contains(nextPill, "dijitOffScreen")) {
                              // nextPill is pointing to the currently off-screen editor for the 
                              // pill.  Set nextPill to this pill's onscreen InlineEditbox.
                              nextPill = nextPill.nextSibling;
                            }                            
                          }                          
                       }
                      } else {  // Selected pill in new section.....reset the pillSelection
                        this.pillSelection.widget = this;
                      }
                                        
                  }
                }
                
                // If there were any 'selected' pills in other sections than this 
                // pill's section, remove the 'selected' indication.
                var containers = ['attributeTagsDiv', 'attributeContactsDiv', 'attributeOwnerDiv'];
                var typeContainerIndex = 0;
                if (this.attrType == 'contact') {
                  typeContainerIndex = 1;
                } else if (this.attrType == 'owner') {
                  typeContainerIndex = 2;
                }
                for (var i = 0; i < containers.length; i++) {
                  if (typeContainerIndex != i) {                                
                    query(".selectedAttribute", containers[i]).forEach(function(selectedPillNode) {
                      domClass.remove(selectedPillNode, "selectedAttribute");
                    });
                  }
                }
                
                // Place focus on this pill's node, but don't enter the editor.
                focusUtil.focus(this.domNode);
                
                evt.stopPropagation();
                evt.preventDefault();
                
                return;
              } else {
                // Remove the 'selected' indication on any existing pills
                // since the CTRL, SHIFT, or META keys were no longer engaged.
                this.removeSelected(true);
              }
              
            }
            origMethod.apply(this, arguments);
          };
        }); 
        
        // Implements:
        // - Delete functionality for a pill - backspace or delete key.
        // - Left/Right Arrow functionality for a pill - loops through the
        //   pills in a given section (tags, contacts).
        on(newIEB, "keydown", function(evt) {
            var attrType = this.attrType;
            var charOrCode = evt.charCode || evt.keyCode;
            var nextFocus = null;
            
            switch (charOrCode) {
              case keys.LEFT_ARROW:
                if (attrType !== 'owner') {   
                  nextFocus = this.previousToFocus();
                  if (!nextFocus) {
                    // Left arrow key loops to last child, the new value entry
                    // field for the section, when you are on the first pill.
                    nextFocus = this.domNode.parentNode.lastChild;
                  }
                }
              case keys.RIGHT_ARROW:
                if (attrType !== 'owner') {                  
                  if (!nextFocus) {
                    nextFocus = this.nextToFocus();
                    // The last pill in a section is the new value entry field,
                    // a combobox whose keystrokes are managed by _attrEntryNavigation()
                    // and not this method.
                    // If focus reaches this field, that code is invoked to
                    // move navigation back into the pills using the right arrow key.
                  }
                  
                  if (evt.ctrlKey || evt.shiftKey || evt.metaKey) {
                    if (domClass.contains(this.domNode, "selectedAttribute")) {
                      domClass.remove(this.domNode, "selectedAttribute");
                    } else {
                      domClass.add(this.domNode, "selectedAttribute");  
                    }
                  } else {
                    // The ctrl, shift, or meta keys were not engaged so
                    // clear selection from all pills in the section.  
                    this.removeSelected(false);
                  }

                }
                evt.preventDefault();
                evt.stopPropagation();
                break;
                
              case keys.DOWN_ARROW:
                if (attrType === 'owner') {
                  // Stop shift to new owner edit field since it should be in hiding right now if
                  // an owner pill is being displayed.
                  evt.preventDefault();
                  evt.stopPropagation();
                }
                break;
                
              case keys.BACKSPACE:
                // Stop propagation of backspace keydown event or the underlay will
                // backup towards the dashboard!
                evt.preventDefault();  
                evt.stopPropagation();
                // Set focus to the previous pill.
                nextFocus = this.previousToFocus();
                while (nextFocus && domClass.contains(nextFocus, "selectedAttribute")) {
                  // If the previous pill was already marked 'selected'
                  // then loop backwards to find the 1st pill that was not selected
                  // for deletion.
                  var pillWidget = registry.byId(nextFocus.id);
                  nextFocus = pillWidget.previousToFocus();
                }               
                
                var section = this.domNode.parentNode;
                // Make sure this node is marked for deletion
                domClass.add(this.domNode, "selectedAttribute");  
                this.deleteSelected();
                if (!nextFocus) {
                  // First pill in section was marked for deletion.  Set focus
                  // to the first pill left in section after deletion.
                  var nextFocus = section.firstChild;
                  if (nextFocus && domClass.contains(nextFocus, "dijitOffScreen")) {
                    // nextFocus is pointing to the currently off-screen editor for the 
                    // first pill.  Set nextFocus to this pill's onscreen InlineEditbox.
                    nextFocus = nextFocus.nextSibling;
                  }                  
                }
                break;
                
              case keys.DELETE:
                // Set focus to the next pill in the section or the new
                // attriubte entry field if this was the last pill getting deleted.
                nextFocus = this.nextToFocus();                
                while (nextFocus && domClass.contains(nextFocus, "selectedAttribute")) {
                  // If the next pill was already marked 'selected'
                  // then loop forward to find the next pill that was not selected
                  // for deletion.
                  var pillWidget = registry.byId(nextFocus.id);
                  nextFocus = pillWidget.nextToFocus();
                }               
                
                // Make sure this node is marked for deletion
                domClass.add(this.domNode, "selectedAttribute");  
                this.deleteSelected();
                
                break;
              default: 
                break;
            }
            
            if (nextFocus) {   // Reset focus
              if (attrType == 'owner') {
                // 'Owner' is a single pill.  If deleted, then we
                // need to bring back the owner edit field into focus!
                var ownerEditWidget = registry.byId("newOwnerEditField");
                domStyle.set(ownerEditWidget.domNode, { visibility: "visible" });
                ownerEditWidget.set('disabled', false); 
                ownerEditWidget.focus();
              } else {
                var nextWidget = registry.byNode(nextFocus);
                if (nextWidget.focus) {
                  nextWidget.focus();
                } else {            
                  focusUtil.focus(nextFocus);
                }
              }
            }
          });

        // InlineEditBox will invoke its 'save' method when focus moves 
        // from its associated editor (combobox) only if the value set in the
        // editor differs from the InlineEditBox's current value.  'save' will
        // update the InlineEditBox's display node with the new value from the
        // associated editor.
        //
        // However, if the value of the editor was set to '', there is no
        // 'value' to be saved as a pill and therefore 'save' should not be
        // invoked.  In this case, destroy the existing pill associated with
        // this inlineEditBox and place the cursor appropriately.
        aspect.around(newIEB.wrapperWidget, "_onChange", function(origMethod){ 
          return function () {
            if (this.editWidget.get('value') == '') {
              if (this.inlineEditBox) {
                // Prior to deleting this pill, get some information....
                var attrType = this.inlineEditBox.attrType;
                // Determine the next pill, or new attribute edit field, to set
                // focus to following the pill delete.     
                var nextFocus = this.inlineEditBox.nextToFocus();

                this.inlineEditBox.destroyRecursive();

                if (attrType == 'owner') {
                  // If this was for the ONLY owner pill, re-instate its edit field.
                  var ownerEditWidget = registry.byId("newOwnerEditField");
                  domStyle.set(ownerEditWidget.domNode, { visibility: "visible" });
                  ownerEditWidget.set('disabled', false);
                  ownerEditWidget.focus();                  
                } else {
                  var nextWidget = registry.byNode(nextFocus);
                  if (nextWidget.focus) {
                    nextWidget.focus();
                  } else {                    
                    focusUtil.focus(nextFocus);
                  }
                }
              }
              return;
            } 
            return origMethod.apply(this, arguments);
          };
        });
        
        // After 'save' has updated the InlineEditBox's display node (pill
        // representing this tag), update the pill to show it is changed.          
        aspect.after(newIEB, "save",
            function() {
               var currentValue = this.get('value');    
               
               var match = false;
               if (this.attrType == 'tag' ||
                   this.attrType == 'contact') {
                 // Tags and Contacts can have multiple pills.  Check if a pill
                 // already exists for this value, then remove this pill so the
                 // value is only submitted once.
                 var typeContainer = 'attributeTagsDiv';
                 if (this.attrType == 'contact') {
                   typeContainer = 'attributeContactsDiv';
                 }
                 var attrNodeList = query('.attrButton', typeContainer);
                 var pill = null;
                 for (var i=0; i<attrNodeList.length; i++) {
                   // Get the widget associated with this node...
                   //     the inlineEditBox represented visually as a pill.
                   pill = registry.byNode(attrNodeList[i]);
                   // Check if there are any other pills with the same value
                   // as the pill that had just been updated.
                   if (pill != this && pill.get('value') == currentValue) {
                     match = true;
                     break;
                   }
                 }
               }

               if (match) {
                 // Another pill exists with this value. So, just delete
                 // this one so only one with this value is submitted.                 
                 this.destroyRecursive();  
                 
                 // Since there was a match, focus on the pill with the same value. 'defer'
                 // was needed so that the change of focus doesn't happen until after the 
                 // focus change from the editor to the InlineEditbox which invoked this 
                 // 'save' has occurred.
                 pill.defer(function(){ 
                     focusUtil.focus(pill.domNode);
                 });
               } else {                   
                 // All updated pills get a green border and a "new" icon in their pill.
                 domClass.add(this.displayNode, "newAttrButton");
                 this.set("title", this.get('value'));
                 // 177381 - A green dot should appear on the left side of the pill. 
                 //          Check the dir attribute value (for BIDI) of the html div
                 //          created as part of the InlineEditBox template to see which
                 //          way the text is flowing for this pill and position the 
                 //          green dot accordingly.
                 var direction = domAttr.get(this.displayNode, "dir");
                 var position = "first";
                 if (direction == "rtl") {
                   position = "last";
                 }
                 domConstruct.place(domConstruct.create("div", {"class": "circleNew"}),
                                    this.displayNode, position);
               }
                              
            }); 
        
        // Adding the new IEB to the DOM causes focus to be set to it.  However,  
        // design states that when a new attribute (pill) is added, focus should be 
        // returned to the new attribute entry field.  On Chrome, this is not happening
        // because focus for the new DOM element is taking precedence over specifically
        // assigning focus to the new attribute entry field. Therefore, for the FIRST 
        // focus to this pill caused by adding an IEB to the DOM, ignore it.
        var pos = aspect.around(newIEB.wrapperWidget, "focus", function(origMethod){ 
          return function () {
            // Do nothing!
            pos.remove();
          };
        });


        return newIEB;
      }
    },
    
    /**
     * Field input validation method.
     * 
     * @param value         Value to be validated.
     * @param constraints   (not currently used)
     * 
     * @returns  True if value is valid; False if value contains an invalid character.
     */
    _validateTag: function(value, constraints) {
      // Invalid characters are all ascii control characters and '/', '<', or '>'.
      // All Unicode chars above u+009F are allowed.        
      var INVALID_NAME_CHARS_REGEX = "[\\u0000-\\u0009\\u000B-\\u001F\\u002F\\u003C\\u003E\\u007F-\\u009F]";
      
      var regexp = new RegExp(INVALID_NAME_CHARS_REGEX);
      var isValid = ! regexp.test(value);
      return isValid;

    },
    
    /**
     * Displays an error message by the input field when field validation fails.
     * 
     * @param message
     */
    _showInvalidMessage: function(message) {
      var tooltipConnector, tooltipContainer;
      var tooltipDirection = "";
      if(message && this.focused) {
        Tooltip.show(message, this.domNode, this.tooltipPosition, !this.isLeftToRight());
        
        // Get the tooltip nodes to modify as an error.....
        query(".dijitTooltipConnector").forEach(function(node) {
          tooltipConnector = node;
        });
        query(".dijitTooltipRight").forEach(function(node) {
          tooltipDirection = "right";
        });
        if (tooltipDirection === "") {
          query(".dijitTooltipLeft").forEach(function(node) {
              tooltipDirection = "left";
          });
        }
        query(".dijitTooltipContainer").forEach(function(node) {
          tooltipContainer = node;
        });

        // Style it in red...
        domClass.add(tooltipContainer, "attributeTooltipError");
        if (tooltipDirection === "right") {
            domClass.add(tooltipConnector, "attributeTooltipConnectorRightError");
        } else if (tooltipDirection === "left") {
            domClass.add(tooltipConnector, "attributeTooltipConnectorLeftError");
        }     
        domClass.add(this.domNode, "attributeTooltipBorderError");
        
      } else {
        Tooltip.hide(this.domNode);
      }
    },
    
    /**
     * Saves the current values for tags, owner, contacts, and note.
     * 
     * This method gathers the values from the dialog fields and prepares them for 
     * submission.
     * Tags and Contacts can have multiple values.  They are submitted as an array of
     * double-quoted strings:  ["one", "two"]
     * Owner and Notes are submitted as a String value.
     * 
     * All fields are re-validated in case the user exited the field before fixing
     * erroneous input and an error message is displayed if needed.
     */
    _save: function() {
      var entryError = false;

      // Get Tag values....
      var tagValues = [];
      var attrNodeList = query('.attrButton', 'tagAttrsPane');
      for (var i=0; i<attrNodeList.length; i++) {
        // Get the widget associated with this node...
        var pill = registry.byNode(attrNodeList[i]);
        var value = pill.get('value');
        if (this._validateTag(value) &&
            pill.wrapperWidget.editWidget.state != "Error") {
          tagValues.push('"' + value + '"');    // API requires values submitted in quotes.
        } else {
          entryError = true;
          break;
        }
      }
      console.log("tagValues");
      console.log(tagValues);
      
      // Get Owner value....
      var ownerValue = "";
      attrNodeList = query('.attrButton', 'ownerAttrsPane');
      if (attrNodeList.length > 0) {   // Value was specified
        // Get the widget associated with this node...
        var pill = registry.byNode(attrNodeList[0]);
        var value = pill.get('value');
        if (this._validateTag(value) &&
            pill.wrapperWidget.editWidget.state != "Error") {
          ownerValue = value;
        } else {
          entryError = true;
        }
      }
      console.log("ownerValue");
      console.log(ownerValue);

      // Get Contact values....
      var contactValues = [];
      attrNodeList = query('.attrButton', 'contactAttrsPane');
      for (var i=0; i<attrNodeList.length; i++) {
        // Get the widget associated with this node...
        var pill = registry.byNode(attrNodeList[i]);
        var value = pill.get('value');
        if (this._validateTag(value) &&
            pill.wrapperWidget.editWidget.state != "Error") {
          contactValues.push('"' + value + '"');  // API requires values submitted in quotes
        } else {
          entryError = true;
          break;
        }
      }
      console.log("contactValues");
      console.log(contactValues);
      
      // Get the Note value....
      var noteValue = registry.byId("notesAttrsPane").get('value');
      if (this._validateTag(noteValue)) {
        domClass.remove(registry.byId("notesAttrsPane").domNode, "fieldError");
      } else {
        entryError = true;
        domClass.add(registry.byId("notesAttrsPane").domNode, "fieldError");
      } 
      console.log("noteValue");
      console.log(noteValue);
      
      console.log("entryError: " + entryError);
      
      if (!entryError) { 
        domStyle.set(dom.byId("messageDiv"), "display", "none");
        // Submit the values.
        this._submit(tagValues, ownerValue, contactValues, noteValue);
      } else {
        console.log('Entry error in one or more fields for Set Attributes dialog.');
        dom.byId("setAttrsDlgErrMsg").innerHTML =  this.tagInvalidChars;
        // Show the message div
        domStyle.set(dom.byId("messageDiv"), "display", "inline");
      }
    },
    
    /**
     * Issues AdminMetadataManagerMBean setAdminMetadata request.
     *    setAdminMetadata(String resourceType, 
     *                     String identity,
     *                     Map    metadata)
     *    This replaces the administrative metadata for the given resource.
     *    
     * @param tagValues - array of double quoted Strings - ["tagOne","tagTwo","tagThree"]
     * @param ownerValue - String
     * @param contactValues - array of double quoted Strings - ["Sue Daniels","Chris Stoop"]
     * @param noteValue - String
     */
    _submit: function(tagValues, ownerValue, contactValues, noteValue){   
      // Based on the type specified in the resource object, set the APIs resourceType and identity.
      var type = this.resource.type;
      var resourceType = "";
      var identity = "";
      switch (type) {
        case 'appOnCluster':
          resourceType = "application";
          var resourceID = this.resource.id;
          var applicationName = resourceID.substring(0, resourceID.indexOf('('));
          var clusterName = resourceID.substring(resourceID.indexOf('(')+1, resourceID.indexOf(')'));
          identity = clusterName + ',' + applicationName;      // <clusterName>,<applicationName>
          break;
        case 'appOnServer':
          resourceType = "application";
          var resourceID = this.resource.id;
          var applicationName = resourceID.substring(0, resourceID.indexOf('('));
          if (this.resource.cluster) {
            // This appOnServer represents an app on a server belonging to a cluster.
            // Therefore, submit the identity as <clusterName>,<applicationName>
            identity = this.resource.cluster + ',' + applicationName;   // <clusterName>,<applicationName>
          } else {
            // This is an app on a server.  The server does not belong to a cluster.
            var serverName = resourceID.substring(resourceID.indexOf('(')+1, resourceID.indexOf(')'));
            identity = serverName + ',' + applicationName;      // <hostName>,<userDir>,<serverName>,<applicationName>
          }
          break;
        case 'cluster':
          resourceType = "cluster";
          identity = this.resource.id;        // <clusterName>
          break;
        case 'server':
        case 'standaloneServer':
          resourceType = "server";
          identity = this.resource.id;        // <hostName>,<userDir>,<serverName>
          break;
        case 'host':    
          resourceType = "host";
          identity = this.resource.id;        // <hostName>
          break;
        case 'runtime': 
          resourceType = "runtime";
          identity = this.resource.id;        // <hostName>,<installDir>
          break;
        default:
          console.error('SetAttribuesDialog.submit called for an unknown resource type: ' + type);
      }
      
      var url = '/IBMJMXConnectorREST/mbeans/WebSphere:feature=collectiveController,name=AdminMetadataManager,type=AdminMetadataManager/operations/setAdminMetadata';
      var options = {
          handleAs : 'json',
          headers : {
            'Content-type' : 'application/json'
          },
          data: '{"params":[{"value":"' + resourceType + '","type":"java.lang.String"},' +
                           '{"value":"' + identity + '","type":"java.lang.String"},'     +
                           '{"value":{"tags":[' + tagValues + '],'         +
                                     '"owner":"' + ownerValue + '",'       +
                                     '"contacts":[' + contactValues + '],' +
                                     '"note":"' + noteValue + '"},'        +
                            '"type":{"className":"java.util.HashMap","simpleKey":true,' +
                                    '"entries":[{"key":"tags","keyType":"java.lang.String","value":"[Ljava.lang.String;"},'     +
                                               '{"key":"owner","keyType":"java.lang.String","value":"java.lang.String"},'       +
                                               '{"key":"contacts","keyType":"java.lang.String","value":"[Ljava.lang.String;"},' +
                                               '{"key":"note","keyType":"java.lang.String","value":"java.lang.String"}]}}'      +
                          '],"signature":["java.lang.String","java.lang.String","java.util.Map"]}',
          sync: true
      };

      request.post(url, options).then(lang.hitch(this, function(response) {
        console.log("it worked!");
        // Close the dialog.  Updated values should be reflected on the panel.
        this.close();
      }), lang.hitch(this, function(err) {
        console.log('Error ' + err.response.status + ' occurred when requesting ' + url + ': ', err);
        
        // Determine error message to display.....
        var errMsg;
        if (err && err.response && err.response.data && err.response.data.stackTrace) {
          var stackTrace = err.response.data.stackTrace;
          errMsg = apiMsgUtils.findErrorMsg(stackTrace);
          if (!errMsg) {
            errMsg = apiMsgUtils.firstLineOfStackTrace(stackTrace);
            if (!errMsg) {
              if (err.response.status) {
                errMsg = err.response.status + " " + i18n.ERROR_SET_TAGS_METADATA;
              } else {  //  Unable to determine what caused the failure, show generic error message...
                errMsg = i18n.ERROR_SET_TAGS_METADATA;
              }
            } 
          } 
        }
        
        // Display the error message.
        dom.byId("setAttrsDlgErrMsg").innerHTML =  errMsg;
        // Show the message div
        domStyle.set(dom.byId("messageDiv"), "display", "inline");

      }));
      
    },
    
    destroy: function() {
      this.inherited(arguments);   
      this.destroyRecursive(); 
    }   
    
  });
});