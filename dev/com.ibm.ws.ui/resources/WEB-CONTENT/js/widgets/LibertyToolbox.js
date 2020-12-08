/* jshint strict: false */
define(["js/toolbox/toolbox",
        "js/catalog/catalog",
        "js/common/imgUtils",
        "js/common/platform",
        "js/widgets/LibertyTool",
        "js/widgets/ConfirmDialog",
        "js/widgets/MessageDialog",
        "dojo/_base/declare",
        "dojox/mobile/IconItem",
        "dojox/mobile/IconContainer",
        "dojo/store/Memory",
        "dojo/parser",
        "dojo/query",
        "dijit/registry",
        "dojo/touch",
        "dojo/_base/window",
        "dojo/_base/lang",
        "dojo/_base/json",
        "dojo/_base/array",
        "dojo/i18n!./nls/widgetsMessages",
        "dojo/dom-construct",
        "dojox/mobile/TextBox",
        "dojo/on",
        "dojo/has",
        "dojo/dom",
        "dojo/keys",
        "dojo/topic",
        'js/toolbox/toolHash',
        'js/toolbox/toolLauncher',
        'dojo/hash',
        'dojo/dom-class'
        ], function(
            toolbox,
            catalog,
            imgUtils,
            platform,
            LibertyTool,
            ConfirmDialog,
            MessageDialog,
            declare,
            IconItem,
            IconContainer,
            Memory,
            parser,
            query,
            registry,
            touch,
            win,
            lang,
            json,
            array,
            i18n,
            DomConstruct,
            TextBox,
            on,
            has,
            dom,
            keys,
            topic,
            toolHash,
            toolLauncher,
            hash, 
            domClass) {

  return declare("LibertyToolbox", [ IconContainer ], {

    id : 'toolIconContainer',
    transition : 'below',
    iconBase : imgUtils.getIcon('profile'),
    editable : true,
    pressedIconOpacity: 1.0,
    iconStore : new Memory({
      idProperty : "id"
    }),
    toolIndexEntries : [],
    IE11: false,
    iconItemPaneProps : {closeIconRole:"button", closeIconTitle:i18n.TOOLBOX_BUTTON_CANCEL},
    tag: "div",

    postCreate : function() {
      this.inherited(arguments);
      this.set("aria-label", i18n.TOOLBOX_TITLE);
      var me = this;

      if (toolHash.isSet()) {
        // if hash is provided in the initial bring up, change the secondary title to "Loading tool..."
        // Otherwise, when we are rendered, the word 'Toolbox' will be displayed for an instant
        registry.byId("toolBox_headerWidget").set("secondaryTitle", i18n.TOOLBOX_TITLE_LOADING_TOOL);

        // if hash is provided in the initial bring up, do not display the tool icon container
        // Otherwise, when we are rendered, the container will be displayed for an instant
        dom.byId("toolIconContainer").style.display = "none";
      } else {
        registry.byId("toolBox_headerWidget").set("secondaryTitle", i18n.TOOLBOX_TITLE);
      }
      registry.byId("toolBox_headerWidget")._fixSearchNode();
      registry.byId("toolBox_headerWidget").createEditDiv();
      registry.byId("toolBox_headerWidget").createEditTrailingFloatRightDiv("editHeaderWidgetRightTrailingDiv");
      registry.byId("toolBox_headerWidget").createBgTaskButton("bgTaskButton", "bgTaskButtonDiv", "editHeaderWidgetRightTrailingDiv");
      registry.byId("toolBox_headerWidget").createProfileDesktopButton("_toolboxContainer", "editHeaderWidgetRightTrailingDiv");
      registry.byId("toolBox_headerWidget").createFilterButton("editHeaderWidgetRightTrailingDiv");
      if(registry.byId("tool_headerWidget"))
      { 
        registry.byId("tool_headerWidget").createBgTaskButton("toolBgTaskButton", "toolBgTaskButtonDiv", "toolHeaderWidgetTrailingDiv");
      }
      // override _EditableIconMixin properties now
      this.set("deleteIconForEdit", "mblDomButtonRedCircleMinus");
      // This may no longer be necessary since we are disabling long click but leaving it here for now
      this.set("onStartEdit", function() {
        // since user can click/hold to start editing, we need to make sure header is in right state
        registry.byId("toolBox_headerWidget").createModifyDiv();
      });

      var trident = !!navigator.userAgent.match(/Trident\/7.0/);
      var net = !!navigator.userAgent.match(/.NET4.0/);
      this.IE11 = trident && net;
      console.log("--------------------------------- IE11: ", this.IE11);
      // catch and handle confirmation on delete

      this.deleteIconClicked = function(e) {
        // item should return an IconItem object
        var item = registry.getEnclosingWidget(e.target);
        // display confirmation
        console.log("confirmDialog is called");

        if (registry.byId('toolboxRemoveToolId')) {
          registry.byId('toolboxRemoveToolId').destroy();
        }

        var removeToolDialog = new ConfirmDialog({
          id: "toolboxRemoveToolId",
          title : i18n.TOOLBOX_REMOVE_TITLE_NO_TOOL_NAME,
          confirmDescriptionIcon: imgUtils.getSVGSmallName('status-alert-gray'),
          confirmDescription: lang.replace(i18n.TOOLBOX_REMOVE_MESSAGE, [ item.label ]),
          confirmMessage : '',
          //confirmMessage : lang.replace(i18n.TOOLBOX_REMOVE_MESSAGE, [ item.label ]),
          confirmButtonLabel : i18n.TOOLBOX_BUTTON_REMOVE,
          okFunction : function() {
            // remove item from list and rebuild container
            me.removeTool(item);
          }
        });
        removeToolDialog.placeAt(win.body());
        removeToolDialog.startup();
        removeToolDialog.show();
        return false;
      };
      // override onTouchStart to disable long click going into edit mode
      this._onTouchStart = function(e) {
        // tags:
        // private
        if (!this._blankItem) {
          this._blankItem = new IconItem();
          this._blankItem.domNode.style.visibility = "hidden";
          this._blankItem._onClick = function() {
          };
        }
        var item = this._movingItem = registry.getEnclosingWidget(e.target);
        var iconPressed = false;
        var n;
        for (n = e.target; n !== item.domNode; n = n.parentNode) {
          if (n === item.iconNode) {
            iconPressed = true;
            break;
          }
        }
        if (!iconPressed) {
          return;
        }

        if (!this._conn) {
          this._conn = [ this.connect(this.domNode, touch.move, "_onTouchMove"), this.connect(win.doc, touch.release, "_onTouchEnd") ];
        }
        this._touchStartPosX = e.touches ? e.touches[0].pageX : e.pageX;
        this._touchStartPosY = e.touches ? e.touches[0].pageY : e.pageY;
        if (this.isEditing) {
          this._onDragStart(e);
          /*
           * }else{ // set timer to detect long press this._pressTimer = this.defer(function(){ this.startEdit(); this._onDragStart(e); },
           * 1000);
           */
        }
      };

      this.buildToolbox();
      this.buildSearchBox();

      // If we have a tool ID in the URL, we need to launch it
      this.launchToolFromURL();

      var previousHash = toolHash.get();
      topic.subscribe("/dojo/hashchange", function(newHash) {
        if (!toolHash.wasSet() && toolHash.hasChanged(previousHash, newHash)) {
          me.loadPage(newHash, previousHash);
        }
        previousHash = newHash;
      });
    },

    startup : function() {
      this.inherited(arguments);
      this.paneContainerWidget.set("role", "region");
      this.paneContainerWidget.set("aria-label", i18n.TOOLBOX_TITLE + " pane"); // never displayed so ok to hardcode
    },

    buildToolbox : function() {
      var me = this;
      toolbox.getToolbox().getToolEntries().then(function(toolEntries) {
        for(var i=0;i<toolEntries.length;i++){
          DomConstruct.place('<div class="mblIconItem" tabindex="-1" id="'+toolEntries[i][Object.keys(toolEntries[i])[0]]+'" widgetid="'+toolEntries[i][Object.keys(toolEntries[i])[0]]+'" style="height:220px; width:192px; top: 0px; left: 0px;"></div>','toolIconContainer','last');
        }
        return toolEntries;
      }).then(function(toolEntries) {
        var toolsLeft = toolEntries.length;
        if ( toolsLeft > 0 ) {
          var editBtn = registry.byId("editButtonWidget");
          if ( editBtn ) {
            editBtn.setDisabled(true);
            editBtn.tabIndex = 1;
          }
        }
          
        var addTheIcon = function(tool) {
          // getToolEntries returns an ordered list of toolEntries.
          // For each toolEntry returned from getToolEntries, a getTool
          // call is made to the server side to get the tool details.
          // Since the call to getTool is async, the tool is not
          // guaranteed to come back in the correct order. Hence an
          // order index is built for each tool to guarantee the
          // display order.
          var toolIndex = 0;
          for ( var index in toolEntries) {
            if (toolEntries[index].id === tool.id) {
              console.log("matching id for " + tool.id + " with index " + index);
              toolIndex = index;
              break;
            }
          }
          me._addToolIcon(tool, toolIndex, true);
          // get the tool dom from the memory store and place it in the container
          var toolIcon = me.iconStore.get(tool.id);
          var toolIconIndex = me.toolIndexEntries[tool.id];
          var totalTools = me.iconStore.data.length;

          DomConstruct.place(toolIcon.domNode, tool.id, "replace");
          
          
          if ( --toolsLeft === 0 ) {
            var editBtn = registry.byId("editButtonWidget");
            registry.byId("toolIconContainer").filter('');
            if ( editBtn ) {
              editBtn.setDisabled(false);
            }
          }
        };

        for ( var i = 0; i < toolEntries.length; i++) {
          console.log("tool entry " + i + " for " + toolEntries[i].id);

          // create a place holder for each tool to preserve the ordering
          var toolIcon = new LibertyTool({
            id: toolEntries[i].id
          });
          // do not place the tool in the container, defect 160837
          me.iconStore.put(toolIcon);

          if (toolEntries.hasOwnProperty(i)) {
            toolbox.getToolbox().getTool(toolEntries[i]).then(addTheIcon);
          }
        }
        query(".mblIconItem", "toolboxContainer").forEach(function(node) {
          query(".mblImageIcon", node).forEach(function(img) {
            img.alt = registry.byId(node.id).label;
          });
        });
      }, function(err) {
        console.error("Error getting tools from the toolbox: ", err.message);
        // display error
        var errorMessageDialog = new MessageDialog({
          title : i18n.LIBERTY_UI_ERROR_MESSAGE_TITLE,
          messageText : lang.replace(i18n.TOOLBOX_GET_ERROR_MESSAGE, [ err.message ])
        });
        errorMessageDialog.placeAt(win.body());
        errorMessageDialog.startup();
        errorMessageDialog.show();
      });
    },

    buildSearchBox: function() {
      var me = this;
      console.log("in buildSearchBox");

      // build secondary search box with a heading pane
      var searchImageRight = "26px";  // tablet
      if (platform.isPhone()) {
        searchImageRight = "17px";
      } else if (platform.isDesktop()) {
        searchImageRight = "24px";
      } 
      if (has("adminCenter-bidi")){
        var textDir = json.fromJson(BIDI_PREFS_STRING)[toolbox.getToolbox().PREFERENCE_BIDI_TEXT_DIRECTION];
        if (textDir === toolbox.getToolbox().PREFERENCE_BIDI_TEXT_DIRECTION_RTL){
          if (platform.isPhone()) {
            searchImageRight = "calc(50% + 95px)";
          } else if (platform.isDesktop()) {
            searchImageRight = "273px";
          } else {
            searchImageRight = "264px";
          }
        }
      }
      // For smartphone...
      // * Don't display the enclosing dojox.mobile.Heading--UNTIL
      // user clicks in filter box
      // * Don't display #searchBoxInputClear or #cancelFiltering icons
      // upon initial load. These are displayed when user clicks in box
      // * Do display the filter icon image on the left of box.
      // Style for filter icon is in mobile.css
      var secondarySearchBoxHeadingStr  =  platform.isPhone() ? "<div id='secondarySearchBoxHeadingDiv' role='search' class='searchBoxHeading' data-dojo-type='dojox/mobile/Heading' fixed='top' >" :
        "<div id='secondarySearchBoxHeadingDiv' role='search' aria-labelledby='secondarySearchBoxInput' class='searchBoxHeading' data-dojo-type='dojox/mobile/Heading'> ";
      
      secondarySearchBoxHeadingStr = secondarySearchBoxHeadingStr.concat(
          '<div id="searchBoxDiv" class="searchBoxPane"> ' +
          '<label for="secondarySearchBoxInput" class="dijitOffScreen">' + i18n.TOOLBOX_SEARCH + '</label>' +
          '<input id="secondarySearchBoxInput" tabindex="0" class="textBox searchBoxInput" data-dojo-type="js/widgets/TextBox" type="text" ' +
          'data-dojo-props="intermediateChanges:true" widgetid="secondarySearchBoxInput"></input> ' +
          '<a id="deleteSearchInputHref" class="deleteSearchInputHref" tabindex="0"><img class="deleteSearchInputImg" id="searchBoxInputClear" title="'+i18n.TOOLBOX_CLEAR_SEARCH+'" style="right:' +
          searchImageRight + ';' + '"' + ' src="'+imgUtils.getIcon('delete')+'" ' +
          'alt="' + i18n.TOOLBOX_CLEAR_SEARCH + '" role="button" ' +
          '></img></a></div>');

      var secondarySearchBoxHeadingDom = DomConstruct.toDom(secondarySearchBoxHeadingStr);
      console.log("secondarySearchBoxHeadingDom ", secondarySearchBoxHeadingDom);

      var cancelSearchStr = '<div style="float:right;width:auto;">' +
      '<a id="cancelFilterHref" tabindex="0" class="cancelFilterHref"><img class="cancelFilterImg" id="cancelFiltering" title="'+i18n.TOOLBOX_END_SEARCH+'" src="'+imgUtils.getIcon('cancel')+'" ' +
      'alt="' + i18n.TOOLBOX_END_SEARCH + '" ' +
      '></img></a>';
      var cancelSearchDom = DomConstruct.toDom(cancelSearchStr);
      console.log('cancelSearchDom: ', cancelSearchDom);
      DomConstruct.place(cancelSearchDom, secondarySearchBoxHeadingDom);
      var toolboxContainerNode = dom.byId("toolboxContainer");
      // For mobile, put in an empty div so that the fixed "Toolbox"
      // header does not conceal it
      if (platform.isPhone()) {
        var emptyDividerStr = '<div class="header-blank"></div>';
        var emptyDividerDom = DomConstruct.toDom(emptyDividerStr);
        DomConstruct.place(emptyDividerDom, toolboxContainerNode, 2);
        DomConstruct.place(secondarySearchBoxHeadingDom, toolboxContainerNode, 3);
        secondarySearchBoxHeadingDom.style.display = "none";
      } else {
        DomConstruct.place(secondarySearchBoxHeadingDom, toolboxContainerNode, 2);
        // For desktop, do not display the secondary search box until later;
        secondarySearchBoxHeadingDom.style.display = "none";
      }
      console.log("done building secondary search box");
      console.log("calling parser instaniate");
      parser.instantiate([dom.byId("secondarySearchBoxInput")]);

      // register events
      var secondarySearchBoxInputWidget = registry.byId("secondarySearchBoxInput");
      on(secondarySearchBoxInputWidget, "change", function() {
        console.log("value of input: ", secondarySearchBoxInputWidget.get("value"));
        me.filter(secondarySearchBoxInputWidget.get("value"));
      });

      var deleteSearchInputHrefDOM = dom.byId("deleteSearchInputHref");
      // for accessibility to detect ENTER key. Has to listen to the a-tag element, not the img element.
      on(deleteSearchInputHrefDOM, "keydown", function(evt) {
        console.log("keydown is pressed");
        if (evt.keyCode === keys.ENTER) {
          console.log("enter is pressed");
          registry.byId('secondarySearchBoxInput').set('value', '');
        }
      });
      on(deleteSearchInputHrefDOM, "click", function() {
        console.log("click is pressed");
        registry.byId('secondarySearchBoxInput').set('value', '');
      });

      var cancelFilteringDOM = dom.byId("cancelFilterHref");
      // for accessibility to detect ENTER key. Has to listen to the a-tag element, not the img element.
      on(cancelFilteringDOM, "keydown", function(evt) {
        console.log("key down to cancel filter");
        if (evt.keyCode === keys.ENTER) {
          console.log("enter is pressed");
          me.refreshSearchPane('none', 'block');
        }
      });
      on(cancelFilteringDOM, "click", function(evt) {
        me.refreshSearchPane('none', 'block');
      });

    },

    // function to control the display style of the filter pane and the filter icon on the header
    refreshSearchPane: function(filterPaneDisplayStyle, filterIconDisplayStyle) {
      console.log("filterPaneStyle: ", filterPaneDisplayStyle);
      console.log("filterIconStyle: ", filterIconDisplayStyle);
      var searchBoxDisplayBlockDom = dom.byId("secondarySearchBoxHeadingDiv");
      console.log("search box display block dom: ", searchBoxDisplayBlockDom);
      searchBoxDisplayBlockDom.style.display = filterPaneDisplayStyle;
      var secondarySearchBoxInputWidget = registry.byId("secondarySearchBoxInput");
      secondarySearchBoxInputWidget.set("value", "");

      var filterIconButtonDom = dom.byId("filterButtonWidget");
      console.log("filterIconButtonDom: ", filterIconButtonDom);
      if ((filterIconButtonDom === undefined) || (filterIconButtonDom === null)) {
        // this path is entered when edit is entered while doing search.
        // For now, just ignore the icon style.
        if (filterIconDisplayStyle === 'none') {
          console.log("no filter icon is found");
        }
      } else {
        filterIconButtonDom.style.display = filterIconDisplayStyle;
        var filterIconButtonWidget = registry.byId("filterButtonWidget");
        // need to take care of the small box created by the widget domNode
        console.log("filterIconButtonWidget: ", filterIconButtonWidget);
        filterIconButtonWidget.domNode.style.display = filterIconDisplayStyle;
      }
    },

    getToolIDs : function() {
      var toolList = [];
      for ( var i = 0; i < this.iconStore.data.length; i++) {
        toolList[i] = this.iconStore.data[i].id;
      }
      return toolList;
    },

    // add a Catalog entry icon to the container for the tool and optionally to the toolbox
    addTool : function(tool, addToToolbox) {
      var me = this;
      if (addToToolbox) {
        toolbox.getToolbox().addToolEntry(tool).then(lang.hitch(this, function(tool) {
          this._addToolIcon(tool, this.iconStore.data.length, false);
        }), function(err) {
          // handle addTool errors
          console.error("Error adding tool " + tool.id + " to the toolbox:", err.response.data.message);
          // display error
          var errorMessageDialog = new MessageDialog({
            title : i18n.LIBERTY_UI_ERROR_MESSAGE_TITLE,
            messageText : lang.replace(i18n.TOOLBOX_ADDTOOL_ERROR_MESSAGE, [ tool.id, err.response.data.message ])
          });
          errorMessageDialog.placeAt(win.body());
          errorMessageDialog.startup();
          errorMessageDialog.show();
        });
      } else {
        me._addToolIcon(tool, this.iconStore.data.length, false);
      }
    },

    _addToolIcon : function(tool, toolIndex, startup) {
      var toolIcon = registry.byId(tool.id);
      if (toolIcon === undefined) {
        var toolTextDir = "";
        if (has("adminCenter-bidi")){
          var textDir = json.fromJson(BIDI_PREFS_STRING)[toolbox.getToolbox().PREFERENCE_BIDI_TEXT_DIRECTION];
          if (textDir === toolbox.getToolbox().PREFERENCE_BIDI_TEXT_DIRECTION_RTL){
            toolTextDir = "rtl";
          } else if (textDir === toolbox.getToolbox().PREFERENCE_BIDI_TEXT_DIRECTION_CONTEXTUAL){
            toolTextDir = "auto";
          }
        }

        // should be a regular add scenario path to add a brand new tool
        toolIcon = new LibertyTool({
          icon : imgUtils.getToolIcon(tool),
          label : tool.name,
          id : tool.id,
          hashId : toolHash.getName(tool.featureShortName),
          href : tool.url,
          url : tool.url,
          isURLTool: tool.type === "bookmark",
          textDir: toolTextDir,
          type : tool.type
        });
        console.log("created new toolIcon with id ", tool.id);
      } else {
        // a startup scenario where we already have a place holder for the tool
        console.log("existing toolIcon with id ", toolIcon.id);
        toolIcon.set("icon", imgUtils.getToolIcon(tool));
        toolIcon.set("label", tool.name);
        toolIcon.set("url", tool.url);
        toolIcon.set("hashId", toolHash.getName(tool.featureShortName));
        toolIcon.set("href", tool.url);
        toolIcon.set("type", tool.type);
        toolIcon.set("isURLTool", tool.type === "bookmark");
        toolIcon.set("deleteIconTitle", lang.replace(i18n.TOOL_DELETE_TITLE, [tool.name]));
      }
      toolIcon.set("alt", tool.name);
      // set the title for mblIconAreaTitle
      toolIcon.labelNode.setAttribute("title", tool.name);

      // The toolIndexEntries is built with two indices:
      // - array with number index and toolid as the array value
      // - array with toolid as the property/string index and the ordering as
      // the value
      // eg.
      // toolIndexEntries[0] = "Simple-Clock"
      // toolIndexEntries[1] = "Weather-Tool"
      // toolIndexEntries[2] = "Trivia-Game"
      // toolIndexEntries["Simple-Clock"] = 0
      // toolIndexEntries["Weather-Tool"] = 1
      // toolIndexEntries["Trivia-Game"] = 3
      //
      // When an ordered list is required, the array with number index is used.
      // When the order for a particular tool is needed, the array with string
      // index is used.
      this.toolIndexEntries[tool.id] = toolIndex;
      this.toolIndexEntries[toolIndex] = tool.id;

      this.iconStore.put(toolIcon);
      if (!startup) {
        console.log("placing tool into the container directly");
        toolIcon.placeAt(this);
      }
    },

    goToToolbox : function(previousHash){
        var toolViewId = 'toolContainer';
        // with background as a hash, need to make sure that the transition from widget is set correctly
        if (previousHash === "backgroundTasks") {
          toolViewId = "bgTasksContainer";
        }
        var returnToViewId = 'toolboxContainer';
        if ( 'prefsContainer' === toolViewId ) {
          returnToViewId = 'toolboxContainer';
        }
        console.log("clicked browser back button, toolViewId=" + toolViewId + ", returnToViewId=" + returnToViewId);

        // when click on home button, remove #toolId from url
        //toolHash.clear();

        registry.byId(toolViewId).performTransition(returnToViewId, -1, "slide", this,function(){
          if (returnToViewId === "toolboxContainer") {

            if (registry.byId("addToolButtonWidget")){
              registry.byId("addToolButtonWidget").destroy();
            }
            if (registry.byId("doneButtonWidget")){
              registry.byId("doneButtonWidget").destroy();
            }
            var returnHeaderWidget = registry.byId("toolBox_headerWidget");
            // change the title back
            returnHeaderWidget.set("secondaryTitle", i18n.TOOLBOX_TITLE);
            // add the secondary search
            returnHeaderWidget._createSecondarySearch();
            // add the edit pencil div
            returnHeaderWidget.createEditDiv();                   
            returnHeaderWidget.createEditTrailingFloatRightDiv("editHeaderWidgetRightTrailingDiv");
            returnHeaderWidget.createBgTaskButton("bgTaskButton", "bgTaskButtonDiv", "editHeaderWidgetRightTrailingDiv");
            returnHeaderWidget.createProfileDesktopButton("_toolboxContainer", "editHeaderWidgetRightTrailingDiv");
            returnHeaderWidget.createFilterButton("editHeaderWidgetRightTrailingDiv");
            // now end the edit for toolContainer so as to refresh the icon correctly
            // with no deleteIcon there
            console.log("========= calling toolcontainer end edit");
            registry.byId("toolIconContainer").endEdit();
            console.log("coming back from end edit");

            // display the toolbox header and icon container
            dom.byId("toolBox_headerWidget").style.display = "block";
            dom.byId("toolIconContainer").style.display = "block";
          }
        });
          
    },

    loadPage: function(page, previousHash) {
      console.log(">>>>loadPage: ", page);
      console.log("previousHash: ", previousHash);
      // if hash is empty reload the AdminCenter url
      // else go to the tool
      if (page === "") {
        var currentUrl = document.URL;
        console.log("currentURL: ", currentUrl);
        try{
          var index =  window.location.href.indexOf("#");
          var newUrl = "";
          if(index > 0){
            newUrl = window.location.href.substring(0, index);
            }
          else{
            newUrl = window.location.href;
            //window.history.back();
            //window.location = newUrl;
            this.goToToolbox(previousHash);
          }
          //window.history.replaceState({}, i18n.LOGIN_TITLE, newUrl);
          //window.location = newUrl;
          } catch(err){
            console.error("Caught error", err, ". Using me.loadPage");
            window.location = currentUrl;
          }
      } else {
        this.launchToolFromURL(previousHash);
        //window.location.reload(true);
      }
    },

    launchToolFromURL: function(previousHash) {
      if (toolHash.isSet()) {
        var hashID = toolHash.get();
        console.log("launchToolFromURL hashID ", hashID);
      
        if (hashID === "backgroundTasks") {
          // going thru the tool catalog is important as it makes the widgets for other containers available
          catalog.getCatalog().getTools().then(function(tools) {

            for(var i = 0; i < tools.length; i++) {
              var tool = tools[i];
              var libertyTool = registry.byId("launchCatalog-" + tool.id);

              if(!libertyTool){              
                libertyTool = new LibertyTool({
                  label : tool.name,
                  url : tool.url,
                  id : "launchCatalog-" + tool.id,
                  hashId : toolHash.getName(tool.featureShortName),
                  href : tool.url,
                  isURLTool: tool.type === "bookmark"
                });
              }
            }

            console.log("--------------- calling launchBgTask");
            registry.byId('tool_headerWidget').launchBgTask(previousHash);
          });

        } else {

          catalog.getCatalog().getTools().then(function(tools) {
            var launched = false;
            for(var i = 0; i < tools.length; i++) {
              var tool = tools[i];
              if (toolHash.getName(tool.featureShortName) === hashID || tool.featureShortName === hashID) {

                var libertyTool = registry.byId("launchCatalog-" + tool.id);

                if(!libertyTool){              
                  libertyTool = new LibertyTool({
                    label : tool.name,
                    url : tool.url,
                    id : "launchCatalog-" + tool.id,
                    hashId : toolHash.getName(tool.featureShortName),
                    href : tool.url,
                    isURLTool: tool.type === "bookmark"
                  });
                }
                console.log("libertyTool from catalog: ", libertyTool);
                launched = true;
                var fromContainer = "toolboxContainer";
                if (previousHash === "backgroundTasks") {
                  fromContainer = "bgTasksContainer";
                }
                toolLauncher.openTool(fromContainer, libertyTool, true);
                break;
              }
            }

            if (!launched) {
              console.log("ERROR tool not found from catalog");

              var alertIcon = imgUtils.getSVGSmallName('status-alert-gray');

              var errorMessageDialog = new MessageDialog({
                title : i18n.TOOL_LAUNCH_ERROR_MESSAGE_TITLE,
                messageDialogIcon: alertIcon,
                //messageText : lang.replace(i18n.TOOL_LAUNCH_ERROR_MESSAGE, [toolID])
                messageText: i18n.TOOL_LAUNCH_ERROR_MESSAGE,
                okFunction: function() {
                  // when click ok button of message dialog, remove #toolId from url if it's there
                  toolHash.erase();
                }
              });

              errorMessageDialog.placeAt(win.body());
              errorMessageDialog.startup();
              errorMessageDialog.show();
            }
          });
        }
      }	else {
        console.log(">> hash is empty - no need to launch tool");
      }
    },

    filter : function(filterString) {
      console.log("in filter");

      // if textdirection is contextual, check to see if we need to move the clearSearch image
      if (has("adminCenter-bidi")){
        var textDir = json.fromJson(BIDI_PREFS_STRING)[toolbox.getToolbox().PREFERENCE_BIDI_TEXT_DIRECTION];
        var searchImage = dom.byId("searchBoxInputClear");
        if (((textDir === toolbox.getToolbox().PREFERENCE_BIDI_TEXT_DIRECTION_CONTEXTUAL && dom.byId("secondarySearchBoxInput").dir === "rtl")) ||
            (textDir === toolbox.getToolbox().PREFERENCE_BIDI_TEXT_DIRECTION_RTL)){
          searchImage.style.setProperty("right", "263px");
          if (platform.isPhone()) {
            searchImage.style.setProperty("right", "244px");
          } else if (platform.isDesktop()) {
            searchImage.style.setProperty("right", "273px");
          }
        } else {
          // make sure it is on the right
          searchImage.style.setProperty("right", "21px");
          if (platform.isPhone()) {
            searchImage.style.setProperty("right", "14px");
          } else if (platform.isDesktop()) {
            searchImage.style.setProperty("right", "24px");
          }
        }
      }

      // destroy old container
      var me = this;
      for ( var i = 0, len = this.getChildren().length; i < len; i++) {
        this.removeChild(0);
      }

      // re-fill toolbox container
      var toolEntryQueryResult = this.iconStore.query(function(object) {
        if (filterString) {
          return object.label.toLowerCase().indexOf(filterString.toLowerCase()) >= 0;
        } else {
          return true;
        }
      });

      var orderedToolEntryQueryResult = [];
      // The query result could be from a search or a regular display.
      // That is why we cannot just traverse the ordering array sequentially.
      // We have to build the ordering based on the query.
      toolEntryQueryResult.forEach(function(toolIcon) {
        var toolId = toolIcon.id;
        var toolIndex = me.toolIndexEntries[toolId];
        orderedToolEntryQueryResult[toolIndex] = toolId;
      });
      console.log("tool entry query result after ordering: ", orderedToolEntryQueryResult);
      orderedToolEntryQueryResult.forEach(function(toolId) {
        var toolIcon = me.iconStore.get(toolId);
        if (toolId !== undefined) {
          toolIcon.placeAt(me);
        }
      });
    },

    removeTool : function(tool) {
      // remove the tool
      var me = this;
      toolbox.getToolbox().deleteTool(tool.id).then(function() {
        // remove tool from store and remove dijit
        me.iconStore.remove(tool.id);
        registry.byId(tool.id).destroy();

        // need to update both set of indices:
        // - array with number index and toolid as the array value
        // - array with toolid as the property/string index and ordering as
        // the value
        var toolIndex = me.toolIndexEntries[tool.id];
        delete me.toolIndexEntries[tool.id];
        me.toolIndexEntries.splice(toolIndex, 1);
        me.RebuildToolOrderIndex();
      }, function(err) {
        // handle removeTool errors
        console.error("Error removing tool " + tool.id + " to the toolbox:", err.response.data.message);
        // display error
        var errorMessageDialog = new MessageDialog({
          title : i18n.LIBERTY_UI_ERROR_MESSAGE_TITLE,
          messageText : lang.replace(i18n.TOOLBOX_REMOVETOOL_ERROR_MESSAGE, [ tool.id, err.response.data.message ])
        });
        errorMessageDialog.placeAt(win.body());
        errorMessageDialog.startup();
        errorMessageDialog.show();
      });
    },

    RebuildToolOrderIndex : function() {
      // This function is called whenever a delete is performed so as to
      // refresh the ordering.
      for ( var i = 0; i < this.toolIndexEntries.length; i++) {
        var toolId = this.toolIndexEntries[i];
        this.toolIndexEntries[toolId] = i;
      }
      console.log("after rebuilding ", this.toolIndexEntries);
    },

    buildOrderedToolEntries : function() {
      // This function is called to build the ordered ToolEntries. This is to
      // persist the tool re-ordering in the JSON file.
      var orderedToolEntries = [];
      for ( var i = 0; i < this.toolIndexEntries.length; i++) {
        var toolEntryProp = {};
        toolEntryProp.id = this.toolIndexEntries[i];
        var toolIcon = this.iconStore.get(toolEntryProp.id);
        toolEntryProp.type = toolIcon.type;
        orderedToolEntries[i] = toolbox.getToolbox().createToolEntry(toolEntryProp);
        console
        .log("buildOrderedToolEntries: orderedToolEntries[" + i + "]=" + orderedToolEntries[i]);
      }
      return orderedToolEntries;
    },

    refreshToolOrdering : function() {
      console.log("in refreshToolOrdering");
      // This function is called when a "done" is clicked after an edit.
      // It needs to take care of two types of tool re-ordering:
      // 1. reorder from regular edit
      // 2. reorder from an edit resulted from a search. The re-ordering from this mode
      // will result in swapping the ordering of the tools in their original ordering
      // in the entire tool entry set.
      // eg.
      // - entire tool entires are tool A, tool B, tool C, tool D, tool E
      // - search resulted in tool entries tool B, tool D, tool E
      // - edit performed to reorder to tool E, tool B, tool D
      // - the merged ordering at the end: tool A, tool E, tool C, tool B, tool D
      var children = this.getChildren();
      var editWithSearch = false;
      var toolOrderWithSearch = [];
      if (children.length !== this.iconStore.data.length) {
        console.log("done from search mode");
        editWithSearch = true;
        for ( var i = 0, len = children.length; i < len; i++) {
          var child = children[i];
          var childIconTool = this.iconStore.get(child.id);
          if (childIconTool !== undefined) {
            toolOrderWithSearch[i] = this.toolIndexEntries[child.id];
          }
        }
        console.log("orig tool order with search ", toolOrderWithSearch);
        toolOrderWithSearch.sort();
        console.log("after sort, tool order with search ", toolOrderWithSearch);
      } else {
        console.log("done from reg mode");
      }

      var hasToolReordered = false;
      for ( var j = 0, length = children.length; j < length; j++) {
        var aChild = children[j];
        var iconTool = this.iconStore.get(aChild.id);
        var iconToolOrder = this.toolIndexEntries[aChild.id];
        console.log("icon id is " + iconTool.id + "; icon order is " + j + "; store order is " + iconToolOrder);
        if (iconTool !== undefined) {
          if (!editWithSearch && (j.toString() !== iconToolOrder)) {
            this.toolIndexEntries[aChild.id] = j;
            this.toolIndexEntries[j] = aChild.id;
            hasToolReordered = true;
            console.log("done is not from search: reorder index to ", j);
          } else if (editWithSearch && (toolOrderWithSearch[j] !== iconToolOrder)) {
            this.toolIndexEntries[aChild.id] = toolOrderWithSearch[j];
            this.toolIndexEntries[toolOrderWithSearch[j]] = aChild.id;
            hasToolReordered = true;
            console.log("from search done: reorder index to ", toolOrderWithSearch[j]);
          }
        }
      }

      // add call to persist here
      if (hasToolReordered) {
        console.log("after reording: ", this.toolIndexEntries);
        toolbox.getToolbox().updateToolEntries(this.buildOrderedToolEntries());
      }

    }

  });

});
