/* jshint strict: false */
define([
        "js/catalog/catalog",
        "js/common/platform",
        "dojo/_base/declare", 
        "dojo/_base/window",
        "dojo/_base/lang",
        "dojo/query",
        "dijit/registry",
        "dojo/dom-construct",
        "dojox/mobile/View",
        "dijit/_TemplatedMixin",
        "dijit/_WidgetsInTemplateMixin",
        "dojo/text!./templates/ToolDetails.html",
        "dojo/i18n!./nls/widgetsMessages",
        'js/toolbox/toolLauncher'
        ], function(
            catalog,
            platform,
            declare,
            win,
            lang,
            query,
            registry,
            domConstruct,
            _WidgetBase,
            _TemplatedMixin,
            _WidgetsInTemplateMixin,
            template,
            i18n,
            toolLauncher){

  return declare("ToolDetails", [ _WidgetBase, _TemplatedMixin, _WidgetsInTemplateMixin ], {
    constructor : function() {

    },

    templateString:template,
    id : 'toolDetailContainer',
    transitionBackTo: 'catalogContainer',
    description: "",
    appname : "",
    version : "",
    updated : "remove",
    category : "remove", 
    toolObject : null,
    notoptimized : i18n.DETAILS_NOTOPTIMIZED,
    descriptiontitle : i18n.DETAILS_DESCRIPTION,
    overview : i18n.DETAILS_OVERVIEW,
    otherversion : i18n.DETAILS_OTHERVERSIONS,
    buttonText: "",
    supportedFieldIcon: "",
    supportedFieldText: "",

    postCreate : function() {
      this.inherited(arguments);
      this.set("aria-label", "Tool Details");  // TODO: replace with i18n translated string
    },


    setup : function(toolId){
      var me = this; 
      console.log ("setup");

      catalog.getCatalog().getTool(toolId.id.substring(8)).then(lang.hitch(toolId, 
          function(tool) {

        if (tool.featureVersion) {
          me.nameField.innerHTML = tool.name + tool.featureVersion;
          me.versionField.innerHTML= lang.replace(i18n.DETAILS_VERSION, [tool.featureVersion]);
        } else {
          me.nameField.innerHTML = tool.name;
          // don't display the version field
          me.versionField.style.display = "none";
        }
        if (tool.update) {
          me.updatedField.innerHTML = lang.replace(i18n.DETAILS_UPDATED, [ tool.update]);
        } else {
          me.updatedField.style.display = "none";
        }
        if (tool.category) {
          me.categoryField.innerHTML = lang.replace(i18n.DETAILS_CATEGORY, [tool.category]);
        } else {
          me.categoryField.style.display = "none";
        }
        me.descriptionField.innerHTML= tool.description;
        me.iconField.src = tool.icon;
        me.toolObject = tool;
        if (toolId.deletable) {
          if (platform.isPhone()) {
            me.buttonText.innerHTML = i18n.DETAILS_ADD; 
          } else {
            me.buttonText.innerHTML = i18n.DETAILS_ADDBUTTON;
          }
        } else {
          me.buttonText.innerHTML = i18n.DETAILS_OPEN;
        }

        //Right now these are not supported
        me.supportedFieldIcon.style.display = "none";
        me.supportedFieldText.style.display = "none";
      })); 

    },


    setTransitionBackTo : function(viewId) {
      this.transitionBackTo = viewId;
      registry.byId("prefs_headerWidget").createToolHeading(this.label, "toolDetailsContainer", this.transitionBackTo);
    },

    detailsToolButton : function() {

      var item = registry.byId("catalog-" + this.toolObject.id);
      // open the tool and return to toolbox
      if (!item.deletable) {
        // set the icon to not selected
        item.set("selected", false);

        toolLauncher.openTool('toolDetailContainer', item);
      } else {
        // add the tool and return to catalog
        registry.byId("toolIconContainer").addTool(this.toolObject,true);
        registry.byId("toolDetailContainer").performTransition("catalogContainer", 1, "slide");

        // switch icon from green plus to checkmark and fix behavior
        item.set("deletable", false); // still has green plus
        item.set("clickable", true);
        query(".mblIconItemDeleteIcon", item.id).forEach(function(node) {
          domConstruct.destroy(node);

        });
        item.deleteIconNode = null;
        item.setAddedIcon();  
      }
    }






  });

});