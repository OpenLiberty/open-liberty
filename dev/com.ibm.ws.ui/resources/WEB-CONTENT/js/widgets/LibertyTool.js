/* jshint strict: false */
define(["dojo/_base/declare", 
        "dojo/_base/lang",
        "dojo/_base/json",
        "dojo/dom-class",
        "dojo/dom-construct",
        "dojo/has",
        "dojox/mobile/IconItem",
        "dijit/registry",
        "dojox/mobile/iconUtils",
        "dojo/i18n!./nls/widgetsMessages",
        "js/toolbox/toolbox",
        "js/common/imgUtils",
        'js/toolbox/toolHash',
        'js/toolbox/toolLauncher'],
        function(declare, 
            lang,
            json,
            domClass,
            domConstruct,
            has,
            IconItem, 
            registry,
            iconUtils,
            i18n,
            toolbox,
            imgUtils,
            toolHash,
            toolLauncher) {

  function isRightClick(e) {
    return e.button && e.button === 2;
  }

  return declare("LibertyTool", [IconItem], {

    clickable: true,
    deletable: true,
    // deleteIcon is overridden by _setDeleteIconAttr codes below
    deleteIcon: "mblDomButtonRedCircleMinus",
    deleteIconRole: "button",
    urlTarget: "toolContentContainer",
    lazy: true,
    // removing the tabIndex so that it won't try to tab to the giant icon just the icon itself
    tabIndex: "-1",
    tag: "div",
    disabledTool: false,

    postCreate: function(){
      this.inherited(arguments);

      // Check if tool should be disabled
      if (!window.globalIsAdmin && this.id.indexOf("com.ibm.websphere.appserver.adminCenter.tool.deploy") !== -1) {
        this.disabledTool = true;
      }

      if (has("adminCenter-bidi")){
        var textDir = json.fromJson(BIDI_PREFS_STRING)[toolbox.getToolbox().PREFERENCE_BIDI_TEXT_DIRECTION];
        if (textDir === toolbox.getToolbox().PREFERENCE_BIDI_TEXT_DIRECTION_RTL){
          this.textDir = "rtl";
        } else if (textDir === toolbox.getToolbox().PREFERENCE_BIDI_TEXT_DIRECTION_CONTEXTUAL){
          this.textDir = "auto";
        }
      }
      if (!this.get("deleteIconTitle")){
        // fix for IE
        var catalogContainer = registry.byId("catalogContainer");
        if (catalogContainer !== null && catalogContainer !== undefined) {
          if (catalogContainer.isVisible()) {
            this.set("deleteIconTitle", lang.replace(i18n.TOOL_ADD_TITLE, [ this.label ]));
          } else {
            this.set("deleteIconTitle", lang.replace(i18n.TOOL_DELETE_TITLE, [ this.label ]));
          }
        }
      }

      // Disable tool if requested,
      // For example, the Deploy Tool is disabled for users with Reader role
      if (this.disabledTool) {
        this.setToolDisabled();
      }

      /* This is the logic which runs when the tool is clicked. This may to launch the tool in the right conditions */
      this.onClick = lang.hitch(this, function(e){

        // if reader role, disable onclick on deploy tool
        if (this.disabledTool === true) {
           // do nothing
           return false;
        }

        // TODO: I really have no idea why this check is here...
        if ( this.hasOwnProperty("label") === false ) {
          return;
        }
        if (isRightClick(e)) {
          // right click so do nothing;
          console.log("onClick: right click so don't launch the tool");
          return false;
        }

        var returnTo = "toolboxContainer";
        // fix for IE
        var returnContainer = registry.byId("catalogContainer");
        if (returnContainer !== null && returnContainer !== undefined) {
          if (returnContainer.isVisible()) {
            returnTo = "catalogContainer";
          }
        }

        // We do not want to set the hash for a URL tool, if we do, then when you hit back
        // the hash will be in the history, and you'll end up in an endless loop of redirects
        if (!this.isURLTool) {
          // set the icon to not selected
          this.set("selected", false); 

          // append the url with /#toolboxId
          toolHash.set(this);
        }

        toolLauncher.openTool(returnTo, this);

        //Details        }
        return false;
      });
    },

    // override highlight so it doesn't wiggle in edit mode
    highlight: function(timeout) {},

    _setDeleteIconAttr: function(icon){
      console.log("in _setDeleteIconAttr");
      // override from IconItem for accessibility
      if(!this.getParent()){ return; } // icon may be invalid because inheritParams is not called yet

      // if in catalog, need the addTool icon
      if (registry.byId("catalogContainer").isVisible()){
        if (this.deletable) {
          icon = imgUtils.getIcon('addTool');
        } else {
          icon = imgUtils.getIcon('addedTool');
          this.set("deleteIconTitle", lang.replace(i18n.TOOL_ADDED_TITLE, [ this.label ]));
        }
      } else {
        icon = imgUtils.getIcon('remove');
        icon = this.deletable ? icon : "";
      }

      // need to handle the following cases:
      // - when this method is called in edit mode to set the deleteIcon
      // - when this method is called to get rid of the deleteIcon when getting out of edit mode
      // - when this method is called when a new tool is added during edit mode
      var doneButtonDOM = document.getElementById('doneButtonWidget');
      console.log("doneButtonDOM: ", doneButtonDOM);
      if (doneButtonDOM !== null) {
        // in edit mode, should continue
        console.log("in edit mode, should continue to create the deleteIcon");
      } else {
        if ((this.deleteIconNode !== undefined) && (this.deleteIconNode !== null)) {
          console.log("removing deleteIcon");
          domConstruct.destroy(this.deleteIconNode);
          domClass.add(this.iconDivNode, "mblNoIcon");
          this.deleteIconNode = null;
        }
        return null;
      }

      // call my version of setIcon
      this.deleteIconNode = this.setIcon(icon, this.deleteIconPos, this.deleteIconNode, 
          this.deleteIconTitle || this.alt, this.iconDivNode);
      if(this.deleteIconNode){
        domClass.add(this.deleteIconNode, "mblIconItemDeleteIcon");
        if(this.deleteIconRole){
          this.deleteIconNode.setAttribute("role", this.deleteIconRole);
        }
      }
    },

    setAddedIcon: function(){
      console.log("setAddedIcon");
      this.set("deleteIconTitle", this.label);
      if(!this.getParent()){ return; } // icon may be invalid because inheritParams is not called yet
      var icon = imgUtils.getIcon('addedTool');
      this.deleteIconNode = this.setIcon(icon, this.deleteIconPos, this.deleteIconNode, 
          this.deleteIconTitle || this.alt, this.iconDivNode);
      if(this.deleteIconNode){
        domClass.add(this.deleteIconNode, "mblIconItemDeleteIcon");
        if(this.deleteIconRole){
          this.deleteIconNode.setAttribute("role", this.deleteIconRole);
        }
      }
    },

    _setIconAttr: function(icon){
      // copied from _ItemBase to call the setIcon here instead of iconUtils for accessibiltiy
      // tags:
      //      private
      if(!this._isOnLine){
        // record the value to be able to reapply it (see the code in the startup method)
        this._pendingIcon = icon;  
        return; 
      } // icon may be invalid because inheritParams is not called yet
      this._set("icon", icon);
      this.iconNode = this.setIcon(icon, this.iconPos, this.iconNode, this.alt, this.iconParentNode, this.refNode, this.position);
    },

    setIcon: function(/*String*/icon, /*String*/iconPos, /*DomNode*/iconNode, /*String?*/alt, /*DomNode*/parent, /*DomNode?*/refNode, /*String?*/pos){
      // override iconUtils.setIcon for accessibility
      console.log("++++++ icon=" + icon + " iconPos:" + iconPos + " iconNode=" + iconNode + " alt=" + alt + " parent=" + parent + " reNode=" + refNode + " pos=" + pos);
      if(!parent || !icon && !iconNode){ return null; }

      if(icon && icon !== "none"){ // create or update an icon
        if(!iconUtils.iconWrapper && icon.indexOf("mblDomButton") !== 0 && !iconPos){ // image
          if(iconNode && iconNode.tagName === "DIV"){
            domConstruct.destroy(iconNode);
            iconNode = null;
          }
          console.log("creating icon with alt:" + alt);
          iconNode = iconUtils.createIcon(icon, null, iconNode, alt, parent, refNode, pos);
          // !!! added alt parameter and role
          iconNode.setAttribute("role", "button");
          iconNode.setAttribute("alt", alt);
          // setting this tabindex allows the badge icons to be tabbed to
          iconNode.setAttribute("tabindex", 0);
          console.log("iconNode=", iconNode);
          domClass.add(iconNode, "mblImageIcon");
          // update css cursor pointer if user is not admin role
          this.updateIconCssCursor(icon, iconNode);
        }else{ // sprite or DOM button
          if(iconNode && iconNode.tagName === "IMG"){
            domConstruct.destroy(iconNode);
            iconNode = null;
          }
          if (iconNode){domConstruct.empty(iconNode);}
          if(!iconNode){
            iconNode = domConstruct.create("div", null, refNode || parent, pos);
          }
          // !!! added alt parameter and role
          var node = iconUtils.createIcon(icon, iconPos, null, alt, iconNode);
          node.setAttribute("role", "button");
          if(alt){
            iconNode.title = alt;
          }
        }
        domClass.remove(parent, "mblNoIcon");
        return iconNode;
      }else{ // clear the icon
        domConstruct.destroy(iconNode);
        domClass.add(parent, "mblNoIcon");
        return null;
      }
    },

    updateIconCssCursor: function(icon, iconNode) {
      // update css cursor if user is not admin role for disable tools
      if (!window.globalIsAdmin) {
        if (icon.endsWith("com.ibm.websphere.appserver.adminCenter.tool.deploy-1.0")) {
          domClass.add(iconNode, "mblImageIconCursor");
        }
      }
    },

    setToolDisabled: function() {
      if (this.id.indexOf("com.ibm.websphere.appserver.adminCenter.tool.deploy") !== -1) {
        // Display message that user has no permission to access the tool because of their role when hover over
        this.set("title", i18n.TOOL_DISABLE);
        this.set("aria-label", i18n.TOOL_DISABLE);
        this.set("aria-disabled", "true");
        // gray out the icon tool
        domClass.add(this.domNode, "mblImageIconDisable");
      }
    }
  });

});