/* jshint strict: false */
define([
        "js/catalog/catalog",
        "js/common/imgUtils",
        "js/widgets/ConfirmDialog",
        "js/widgets/MessageDialog",
        "js/widgets/LibertyTool",
        "js/toolbox/toolHash",
        "dijit/registry",
        "dojo/dom-construct",
        "dojo/_base/declare", 
        "dojo/_base/window",
        "dojo/_base/lang",
        "dojo/_base/array",
        "dojox/mobile/IconContainer",
        "dojo/store/Memory",
        "dojo/query",
        "dojo/on",
        "dojo/i18n!./nls/widgetsMessages"
        ], function(catalog, 
            imgUtils,
            ConfirmDialog,
            MessageDialog,
            LibertyTool,
            toolHash,
            registry,
            domConstruct,
            declare,
            win,
            lang,
            array,
            IconContainer, 
            Memory, 
            query,
            on,
            i18n){

  return declare("LibertyToolbox", [ IconContainer ], {

    id : 'catalogIconContainer',
    transition : 'below',
    iconBase : imgUtils.getIcon('profile'),
    editable : true,
    iconStore : new Memory(),
    action : "",
    iconItemPaneProps : {closeIconRole:"button", closeIconTitle:i18n.TOOLBOX_BUTTON_CANCEL},
    tag: "div",

    postCreate : function() {
      this.inherited(arguments);
      this.set("aria-label", i18n.TOOLCATALOG_TITLE);
      registry.byId("catalog_headerWidget").set("secondaryTitle", i18n.TOOLCATALOG_TITLE);

      // override _EditableIconMixin properties now

      this.deleteIconClicked = function(e) {

        // This is actually not deleting - it's adding the tool
        // but it's getting called for + and checks
        // item should return a LibertyTool object
        var item = registry.getEnclosingWidget(e.target);

        // if it's deletable, then it has the +
        // so we really do want to do the add confirmation

        if (item.deletable) {

          // override _EditableIconMixin properties now       
          if (registry.byId('catalogAddToolId')) {
            registry.byId('catalogAddToolId').destroy();
          }

          // display confirmation
          var addToolDialog = new ConfirmDialog({
            id: "catalogAddToolId",
            title : lang.replace(i18n.TOOLCATALOG_ADDTOOL_TITLE, [ item.label ]),
            confirmMessage : lang.replace(i18n.TOOLCATALOG_ADDTOOL_MESSAGE, [ item.label ]),
            confirmButtonLabel : i18n.TOOLCATALOG_BUTTON_ADD,
            okFunction : function() {
              // add to the user's toolbox
              catalog.getCatalog().getTool(item.id.substring(8)).then(
                  function(tool) {
                    registry.byId("toolIconContainer").addTool(tool, true);
                    // switch icon from green plus to checkmark and fix behavior
                    item.set("deletable", false); // still has green plus
                    item.set("clickable", true);
                    query(".mblIconItemDeleteIcon", item.id).forEach(function(node) {
                      domConstruct.destroy(node);
                    });
                    item.deleteIconNode = null;
                    item.setAddedIcon();
                  },
                  function(err) {
                    console.error("LibertyCatalog.getCatalog getTool error: ", err);
                    // display error
                    var errorMessageDialog = new MessageDialog({
                      title : i18n.LIBERTY_UI_ERROR_MESSAGE_TITLE,
                      messageText: lang.replace(i18n.LIBERTY_UI_CATALOG_GET_TOOL_ERROR, [item.id.substring(8), err.response.data.message])
                    });
                    errorMessageDialog.placeAt(win.body());
                    errorMessageDialog.startup();
                    errorMessageDialog.show();
                  });
            }
          });  //add tool dialog
          addToolDialog.placeAt(win.body());
          addToolDialog.startup();
          addToolDialog.show();
          return false;
        } else {
          //Details           // this was really the checkmark - should launch 
          //            this.set("selected", false);
          //            toolLauncher.openTool('catalogContainer', this);
          //            return true;
          //
          return false;
        }
      };
      this._onTouchStart = function(e){
        //console.log("LibertyCatalog._onTouchStart, " + e.target);
      };

      this._onTouchEnd = function(e){
        //console.log("LibertyCatalog._onTouchEnd, " + e.target);
      };
    },

    startup : function() {
      this.inherited(arguments);
      this.paneContainerWidget.set("role", "region");
      this.paneContainerWidget.set("aria-label", i18n.TOOLCATALOG_TITLE + " pane"); // never displayed so ok to hardcode
    },

    buildCatalog : function(action, param) {
      var me = this;
      me.action = action;
      me.destroyDescendants();
      catalog.getCatalog().getTools().then(
          function(tools) {
            var highlightFunction = function(timeout) {
            };
            for ( var i in tools) {
              if (tools.hasOwnProperty(i)) {
                var tool = tools[i];
                var deletable = true;
                var clickable = false;
                // If action is add, we are coming in from the toolbox. We need to get the current
                // tools in the toolbox because they can not be added again.
                // "param" is an array of tool IDs that are currently in the toolbox
                if (action && action === "add" && param) {
                  if (array.indexOf(param, tool.id) !== -1) {
                    deletable = false;
                    clickable = true;
                  } 
                }

                var toolIcon = new LibertyTool({
                  icon : imgUtils.getToolIcon(tool),
                  label : tool.name,
                  url : tool.url,
                  id : "catalog-" + tool.id,
                  hashId : toolHash.getName(tool.featureShortName),
                  href : tool.url,
                  isURLTool: tool.type === "bookmark",
                  clickable : clickable,
                  deletable : deletable,
                  deleteIconTitle: lang.replace(i18n.TOOL_ADD_TITLE, [ tool.name ])
                });
                if (action && action === "add" && !deletable) {
                  // set the checkmark since it is already in the toolbox
                  toolIcon.setAddedIcon();                          
                }
                me.iconStore.put(toolIcon);
                toolIcon.placeAt(me);
              }
            }

            query(".mblIconItem", "catalogContainer").forEach(function(node){
              on(node,"click", function(event){       
                var toolIcon = registry.byId(node.id);
                if (toolIcon.clickable){  //Details had this commented out
                  console.log("clicked on:" + toolIcon.label);                 
                  //Details                 toolIcon.onClick(event);
                  toolIcon.onClick(event);
                }      //Details had this commented out
              });
              query(".mblImageIcon", node).forEach(function(img){
                img.alt = registry.byId(node.id).label;
              });
            });
            if (action === "add" && !me.isEditing) {
              // put catalog into add mode
              me.set("deleteIconForEdit", "mblDomButtonGreenCirclePlus");
              console.log("starting edit in catalog");
              me.startEdit();
            }
          }, function(err) {
            console.error("LibertyCatalog.buildCatalog getCatalog error: ", err);
            // display error
            var errorMessageDialog = new MessageDialog({
              title : i18n.LIBERTY_UI_ERROR_MESSAGE_TITLE,
              messageText : lang.replace(i18n.LIBERTY_UI_CATALOG_GET_ERROR, [ err.response.data.message ])
            });
            errorMessageDialog.placeAt(win.body());
            errorMessageDialog.startup();
            errorMessageDialog.show();
          });
    },

    filter : function(filterString) {
      // destroy old container
      var me = this;
      me.destroyDescendants();

    }

  });

});