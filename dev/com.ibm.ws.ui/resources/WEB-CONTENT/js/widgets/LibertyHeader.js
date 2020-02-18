/* jshint strict: false */
define([
        "js/toolbox/toolbox",
        "js/common/platform",
        "js/common/imgUtils",
        "dojo/_base/declare",
        "dojo/dom",
        "idx/app/Header",
        "idx/widget/Menu",
        "dijit/DropDownMenu",
        "dijit/MenuItem",
        "dijit/MenuSeparator",
        "dijit/form/Button",
        "dojo/_base/array",
        "dijit/form/DropDownButton",
        "idx/form/buttons", //iButtons string not expressly used, but still necessary
        "dojo/dom-construct",
        "dojo/query",
        "dijit/registry",
        "dojo/i18n!./nls/widgetsMessages",
        "dojo/has",
        "dojo/_base/lang",
        "dojo/_base/json",
        "dijit/TooltipDialog",
        "dijit/popup",
        "js/toolbox/backgroundTasks",
        "js/toolbox/links",
        "dojo/dom-class",
        "js/widgets/BGTasksPopup",
        'js/toolbox/toolHash'
        ], function(toolbox, platform, imgUtils, declare, dom, AppHeader, Menu, DropDownMenu, MenuItem, MenuSeparator, Button, array, DropDownButton, 
            iButtons, DomConstruct, query, registry, i18n, has, lang, json, TooltipDialog,
            popup, backgroundTasks, links, domClass, BGTasksPopup, toolHash) {

  /**
   * Builds a hidden form to trigger the logout. This isn't super elegant,
   * but its more flexible than hard-coding up some stuff in HTML!
   * 
   * Here's what the HTML would look like though:
   * <form id="doLogout" style="display:none" method="POST" action="ibm_security_logout">
   * <input type="hidden" name="logoutExitPage" value="/login.jsp">
   * <input type="submit" name="logout" value="Logout">
   * </form>
   */
  function logout() {
    // Build a hidden logout form
    var inputPage = document.createElement("input");
    inputPage.type="hidden";
    inputPage.name="logoutExitPage";
    inputPage.value="/login.jsp";

    var inputSubmit = document.createElement("input");
    inputSubmit.type="submit";
    inputSubmit.name="logout";
    inputSubmit.value="Logout";

    var logoutForm = document.createElement("form");
    logoutForm.id="doLogout";
    logoutForm.style.cssText="display:none";
    logoutForm.method="POST";
    logoutForm.action="ibm_security_logout";
    logoutForm.appendChild(inputPage);
    logoutForm.appendChild(inputSubmit);

    // Remove any tool iframes to prevent calls
    var iframe = dom.byId("toolIFrame");
    if (iframe) {
      DomConstruct.destroy("toolIFrame");
    }

    // Add Logout to DOM and submit
    document.body.appendChild(logoutForm);
    document.getElementById('doLogout').submit();
  }

  function filterSearchFunction(data) {
    registry.byId("toolIconContainer").filter(data);
  }

  return declare("LibertyHeader", [AppHeader], {
    constructor: function() {
    },

    id: 'toolBox_headerWidget',
    containerId: '',
    userName: '',
    primaryTitle: i18n.LIBERTY_HEADER_TITLE, 
    primaryBannerType: 'thin',
    layoutType: 'variable',

    secondaryBannerType: 'blue',
    secondaryTitle: i18n.TOOLBOX_TITLE,

    postMixInProperties: function(){
      this.inherited(arguments);

      // construct user menu bar
      var userMenu = new Menu({id:"user_actionWidget" + this.id});
      userMenu.set("aria-label", this.id);
      userMenu.addChild(new MenuItem({label:i18n.LIBERTY_HEADER_PROFILE, onClick: lang.hitch(this, function(){
        // need to let prefs container know where to transition back to for back button
        console.log("going to prefs from container:" + this.containerId);
        registry.byId("prefsContentContainer").setTransitionBackTo(this.containerId);
        registry.byId(this.containerId).performTransition("prefsContainer", 1, "slide");
      })})); 
      userMenu.addChild(new MenuItem({label:i18n.LIBERTY_HEADER_LOGOUT, onClick: logout}));
      userMenu.addChild(new MenuItem({label:this.userName, iconClass:"headerIcon profileIcon"}), 0); 
      var user = {
          displayName: this.userName,
          displayImage: imgUtils.getIcon('profile'),
          actions: userMenu
      };
      this.set("user", user);
      if (this.id === "toolBox_headerWidget"){
        var secondarySearch = {
            onChange: function(data){
              filterSearchFunction(data);
            }
        };
        this.set("secondarySearch", secondarySearch);
      }

    },

    postCreate: function(){
      console.log("view id:" + this.id);
      // Always create skip to content button for every view
      var skipToContentButtonStr = "<a class='skipToContent' role='button' tabindex='1' aria-label='Skip to content' title='Skip to content'>Skip to content</a>";
      var skipToContentButton = DomConstruct.toDom(skipToContentButtonStr);
      DomConstruct.place(skipToContentButton, this.domNode, "first"); // Make sure this button is first on the UI
    },

    _fixSearchNode: function(){
      // need to hard code the id of the search input field for test
      // starting at the secondary banner node, find the text input field
      console.log("fixing searchNode");
      query(".idxHeaderSearchContainer input", this.trailingSecondaryBannerNode).forEach(function(node){
        if (node.type === "text") {
          node.id = "toolboxSearchText";
          // set text direction if bidi is enabled
          if (has("adminCenter-bidi")){
            // get user preference for textDir and set it (rtl, ltr, auto)
            var textDir = json.fromJson(BIDI_PREFS_STRING)[toolbox.getToolbox().PREFERENCE_BIDI_TEXT_DIRECTION];
            console.log("setting Search textDirection:" + textDir);
            if (textDir === toolbox.getToolbox().PREFERENCE_BIDI_TEXT_DIRECTION_CONTEXTUAL){
              node.setAttribute("dir", "auto");
            } else if (textDir === toolbox.getToolbox().PREFERENCE_BIDI_TEXT_DIRECTION_RTL){
              node.setAttribute("dir", "rtl");
            } else {
              node.setAttribute("dir", "ltr");
            }
          }
        }
      });
    },

    _createSecondarySearch: function(){
      registry.byId("toolBox_headerWidget").set("secondarySearch", {onChange: function(data){filterSearchFunction(data);}});
      this._fixSearchNode();
      // clear any search results since creating a brand new search field ... reset to full toolbox
      // the search in the header banner is not used anymore
      //filterSearchFunction("");
    },

    /**
     * Setter to handle re-rendering the widget if needed.
     * @private
     */
    _setSecondaryTitleAttr: function(value) {
      this.inherited(arguments);
      // need to add "banner" to make the aria-label unique as it is used for the toolbox as well
      this.set("aria-label", lang.replace(i18n.TOOLBOX_BANNER_LABEL, [value]));
    },

    createModifyDiv: function(){
      // if it is already there, just ignore this call
      if (dom.byId("modifyButtonWidgetDiv")) {
        return;
      }

      // if the edit div is there, need to delete the search which will also delete the edit div
      if (dom.byId("editButtonWidgetDiv")) {
        // first remove search since it will clear the trailingSecondaryBannerNode
        this.set("secondarySearch", null);
        // switch title to Edit my toolbox
        this.set("secondaryTitle", i18n.TOOLBOX_TITLE_EDIT);
      }

      // create the div that holds the add button.
      // if the device is a smartphone, don't add the inline style for width
      // since we want the CSS media query style to do that 
      var domString = "<div id='editHeaderWidgetTrailingDiv' class='leftSecondaryHeader' style='display:inline-block;";
      // 45px and 70px are the icon sizes
      var suffix = platform.isPhone() ? "height:45px;float:left'></div>" : "width:70px;height:70px;float:left'></div>";
      domString = domString.concat(suffix);
      console.log("domString = " + domString);
      var trailingHeaderDiv = DomConstruct.toDom(domString);
      // for desktop, put the + at the left of the header.
      // There is css using media to move the leadingSecondaryHeader to the right of the icon.
      if (platform.isPhone()) {
        DomConstruct.place(trailingHeaderDiv, this.trailingSecondaryBannerNode);
      } else {
        DomConstruct.place(trailingHeaderDiv, this.secondaryBannerNode);
      }

      // add icon is always on the left 
      var modifyButtonStr = "<div id='modifyButtonWidgetDiv' style='display:inline-block;";
      suffix = platform.isPhone() ? "'></div>" : "width:70px;height:70px;float:left'></div>"; 
      modifyButtonStr = modifyButtonStr.concat(suffix);
      var modifyButtonDiv = DomConstruct.toDom(modifyButtonStr);
      DomConstruct.place(modifyButtonDiv, trailingHeaderDiv);

      // create a widget to anchor on the right to place the done icon
      domString = "<div id='editHeaderWidgetRightTrailingDiv' style='display:inline-block;";
      // 70px is the icon height and width. 210px is because we have 3 icons.
      suffix = platform.isPhone() ? "'></div>" : "height:70px;width:210px;position:absoulte;float:right;'></div>";
      domString = domString.concat(suffix);
      var rightTrailingHeaderDiv = DomConstruct.toDom(domString);
      DomConstruct.place(rightTrailingHeaderDiv, this.trailingSecondaryBannerNode);

      // done icon is always on the right 
      var doneButtonDiv = DomConstruct.toDom("<div id='doneButtonWidgetDiv' style='display:inline-block;float:right'></div>");
      DomConstruct.place(doneButtonDiv, rightTrailingHeaderDiv);
      var me = this;

      // construct add menu
      var addToolMenu;
      if (platform.isPhone()) {
        addToolMenu = new DropDownMenu({
          id: "toolboxAddToolMenu",
          style: "display: none;"
        });
      } else { 
        addToolMenu = new DropDownMenu({
          id: "toolboxAddToolMenu"
        });
      }

      addToolMenu.set("aria-label", i18n.TOOLBOX_ADD_TOOL);
      addToolMenu.addChild(new MenuItem({
        label:i18n.TOOLBOX_ADD_CATALOG_TOOL,
        id: "toolboxAddCatalogToolMenu",
        onClick: function(){
          // launch the catalog in add mode
          registry.byId("catalog_headerWidget").createToolHeading("not used", "catalogContainer", "toolboxContainer");
          registry.byId("catalogIconContainer").buildCatalog("add", registry.byId("toolIconContainer").getToolIDs());
          registry.byId("toolboxContainer").performTransition("catalogContainer", 1, "slide");
        }
      })); 
      addToolMenu.addChild(new MenuSeparator());
      addToolMenu.addChild(new MenuItem({
        label:i18n.TOOLBOX_ADD_BOOKMARK,
        id: "toolboxAddBookmarkMenu",
        onClick: function(){
          registry.byId('addBookmarkDialogId').show();
        }
      }));

      var addButtonIconClass = platform.isPhone() ? 'headerIcon plusIcon' : 'desktop headerIcon plusIcon'; 
      console.log("add button iconclass: ", addButtonIconClass);
      var addToolButton = registry.byId("addToolButtonWidget");
      if (!addToolButton) {
        addToolButton = new DropDownButton({
          id: 'addToolButtonWidget',
          name: 'addToolButtonWidget',
          iconClass: addButtonIconClass,
          showLabel: false,
          label: i18n.TOOLBOX_ADD_TOOL,
          placement: 'special',
          displayMode: 'iconOnly',
          style: 'margin:0',
          tabindex: '1',
          dropDown: addToolMenu
        });
      }
      addToolButton.placeAt(modifyButtonDiv);
      addToolMenu.on('open', function(evt) {
        query(".dijitPopup").forEach(function(node) {
          console.log("------------- dijitPopup node: ", node);
          domClass.add(node, "addDropDownPopup");
        });
      });

      domClass.add(addToolMenu.domNode, "addDropDownMenu");

      var doneButtonIconClass = platform.isPhone() ? 'headerIcon doneIcon' : 'desktop headerIcon doneIcon'; 
      var doneButton = registry.byId("doneButtonWidget");
      if (!doneButton) {
        doneButton = new Button({
          id: 'doneButtonWidget',
          widget: 'doneButtonWidget',
          iconClass: doneButtonIconClass,
          displayMode: 'iconOnly',
          label: i18n.TOOLBOX_DONE,
          showLabel: false,
          placement: 'special',
          tabindex: '4',
          style: 'margin:0',
          onClick: function(){
            registry.byId("toolBox_headerWidget").performDone();
          }
        });
      }
      // if this is a smartphone, add the button to the 
      // #editHeaderWidgetTrailingDiv div so it flushes to the right
      if (platform.isPhone()) {
        doneButton.placeAt(trailingHeaderDiv); 
      } else {
        // default behavior
        doneButton.placeAt(doneButtonDiv);
      }
      console.log("pencil button is clicked");
    },

    createEditDiv: function(){
      // create the div that holds the edit pencil button
      // if the device is a smartphone, don't add the inline style for width
      // since we want the CSS media query style to do that 
      var domString = "<div id='editHeaderWidgetTrailingDiv' class='leftSecondaryHeader' style='display:inline-block;";           
      // 45px and 70px are the icon sizes
      var suffix = platform.isPhone() ? "height:45px'></div>" : "width:70px;height:70px'></div>";
      domString = domString.concat(suffix);
      console.log("domString = " + domString);
      var trailingHeaderDiv = DomConstruct.toDom(domString);
      // for desktop, put the edit at the left of the header.
      // There is css using media to move the leadingSecondaryHeader to the right of the icon.
      if (platform.isPhone()) {
        DomConstruct.place(trailingHeaderDiv, this.trailingSecondaryBannerNode);
      } else {
        DomConstruct.place(trailingHeaderDiv, this.secondaryBannerNode);
      }

      var flush = "left";  
      console.log("flush = " + flush);

      var heightandwidth = platform.isPhone() ? "" : ";width:70px;height:70px"; 
      console.log("height and width: ", heightandwidth);

      var editButtonDiv = DomConstruct.toDom("<div id='editButtonWidgetDiv' style='display:inline-block;float:"+flush+heightandwidth+"'></div>");
      DomConstruct.place(editButtonDiv, trailingHeaderDiv);
      var me = this;

      var button_icon_Class = platform.isPhone() ? 'headerIcon pencilEditIcon' : 'desktop headerIcon pencilEditIcon'; 
      console.log("button iconClass: ", button_icon_Class);
      var editButton = new Button({
        id: 'editButtonWidget',
        iconClass: button_icon_Class,
        displayMode: 'iconOnly',
        label: i18n.TOOLBOX_EDIT,
        placement: 'special',
        style: 'border:none;margin:0',
        tabindex: '1',
        onClick: function(){
          // going into edit mode
          // create the modify div containing add and done buttons
          me.createModifyDiv();
          // clear out the filter pane
          registry.byId("toolIconContainer").refreshSearchPane('none', 'none');
          registry.byId("toolIconContainer").filter('');
          registry.byId("toolIconContainer").startEdit();
          registry.byId("addToolButtonWidget").focus();
        }
      });
      editButton.placeAt(editButtonDiv);

      if (platform.isPhone()) {
        me.createProfileButton("toolboxContainer", trailingHeaderDiv);
      }

    },

    createEditTrailingFloatRightDiv: function(divId) {
      if (platform.isPhone()) {
        return;
      }

      // If already there, remove it first. Not sure if just leaving it works so clean up and create a new one
      if (dom.byId(divId)){
        DomConstruct.destroy(divId);
      }
      // create a widget to anchor on the right to place other icons
      // 70px is the icon height and width. 210px is because we have 3 icons.
      var rightDivString = "<div id=" + divId + " style='display:inline-block;" +
      "height:55px;width:210px;position:absoulte;float:right;'></div>";  

      var rightTrailingHeaderDiv = DomConstruct.toDom(rightDivString);
      DomConstruct.place(rightTrailingHeaderDiv, this.trailingSecondaryBannerNode);
    },

    // toolTitle does not appear to be used
    createToolHeading: function(toolTitle, toolViewId, returnToViewId){

      // create the div that holds the home button
      console.log("createToolHeading:" + toolViewId + " return to " + returnToViewId);
      var leadingHeaderDiv = null;
      var homeButtonDiv = null;
      var leadingHeaderDivId = toolViewId + "HeaderLeadingDiv";
      if (dom.byId(leadingHeaderDivId)) {
        leadingHeaderDiv = dom.byId(leadingHeaderDivId);
        if (registry.byId('homeButton' + toolViewId)){
          registry.byId('homeButton' + toolViewId).destroy();
        }
        homeButtonDiv = dom.byId("homeButtonDiv" + toolViewId);
      } else {
        // 70px is the icon height and width.
        leadingHeaderDiv = DomConstruct.toDom("<div id='" + leadingHeaderDivId + "' style='display:inline-block;height:55px;width:70px;position:absolute;'></div>");
        console.log("placing leadingHeaderDiv at id:" + this.id);
        var bannerNodeToUse = platform.isPhone() ?  this.trailingSecondaryBannerNode : this.secondaryBannerNode;  
        var position = platform.isPhone() ?  "after" : "first"; 
        DomConstruct.place(leadingHeaderDiv, bannerNodeToUse, position);
        homeButtonDiv = DomConstruct.toDom("<div id='homeButtonDiv" + toolViewId + "' style='display:inline-block'></div>");
        DomConstruct.place(homeButtonDiv, leadingHeaderDiv);
      }

      var buttonLabel = i18n.TOOLBOX_TITLE;
      if (this.containerId !== "prefsContainer") {   //adding this because right now preferences always returns to toolbox - ugly but that's how it is           
        if (returnToViewId === "catalogContainer") {
          console.log('Turning off setting label as Tool Catalog, need to turn back on soon');
          // TODO: Re-enable this?? This may need to be re-enabled if we support going back into the Catalog view
//          buttonLabel = i18n.TOOLCATALOG_TITLE;
        } else {              
          if (returnToViewId === "toolContainer") {
            buttonLabel = toolTitle;
          } else {
            if (returnToViewId === "prefsContainer") {
              buttonLabel = i18n.LIBERTY_HEADER_PROFILE;
            }
          }
        }
      }

      var mobile_icon_class = 'headerIcon toolboxIcon';     
      var dt_icon_class = 'desktop headerIcon toolboxIcon';
      if ( this.get("tooboxIconClass_mobile") ) {
        mobile_icon_class = this.get("tooboxIconClass_mobile");
      }
      if ( this.get("tooboxIconClass_dt") ) {
        dt_icon_class = this.get("tooboxIconClass_dt");
      }
      var button_icon_Class = platform.isPhone() ?  mobile_icon_class : dt_icon_class;
      console.log("button_icon_Class = " + button_icon_Class);
      console.log("homeButton: " + 'homeButton' + toolViewId);
      var homeButton = new Button({
        id: 'homeButton' + toolViewId,
        iconClass: button_icon_Class,
        showLabel: false,
        label: buttonLabel,
        placement: 'special',
        displayMode: 'iconOnly',
        tabindex: '1',
        style: 'margin:0', // Forces the margin for the 'go back to Toolbox' button to be zero
        onClick: function(){
          // if we were in the tool, but we got there from the details view
          // we need to set the return to the toolbox and set a flag so we know 
          //Details                   var openFromDetails = false;
          //                   if ((returnToViewId === "toolDetailContainer") && (toolViewId === "toolContainer")) {
          //                     returnToViewId = "toolboxContainer";
          //                     openFromDetails = true;
          //                   }
          // going back to returnViewId

          if ( 'prefsContainer' === toolViewId ) {
            returnToViewId = 'toolboxContainer';
          }
          console.log("clicked home button, toolViewId=" + toolViewId + ", returnToViewId=" + returnToViewId);

          if(returnToViewId === "catalogContainer" || returnToViewId === "bgTasksContainer"){
            returnToViewId = "toolboxContainer";
            console.log("changing returnToViewId to " + returnToViewId);
          }
          
          // when click on home button, remove #toolId from url
          // should not clear the hash if not going back to toolboxContainer
          if (returnToViewId === "toolboxContainer") {
            toolHash.clear();
          } 

          registry.byId(toolViewId).performTransition(returnToViewId, -1, "slide", this, function(){
            var returnHeaderWidget = null;

            if (returnToViewId === "toolDetailContainer"){
              returnHeaderWidget = registry.byId("details_headerWidget");
              var detailsHeaderRightDiv = "deatailsHeaderWidgetTrailingDiv";
              returnHeaderWidget.createEditTrailingFloatRightDiv(detailsHeaderRightDiv);
              console.log("create Profile button on detailsHeader");
              returnHeaderWidget.createProfileDesktopButton("toolDetailContainer",  detailsHeaderRightDiv);
              // add the back button 
              returnHeaderWidget.createBackButton("toolDetailContainer",leadingHeaderDiv);
            } else if (returnToViewId === "prefsContainer"){
              returnHeaderWidget =  registry.byId('prefs_headerWidget');
              var prefsHeaderRightDiv = "prefsHeaderWidgetTrailingDiv";
              returnHeaderWidget.createEditTrailingFloatRightDiv(prefsHeaderRightDiv);
              console.log("create bgTasks button on prefsHeader");
              returnHeaderWidget.createBgTaskButton("prefsBgTaskButton", "prefsBgTaskButtonDiv", prefsHeaderRightDiv);
            } else if (returnToViewId === "toolboxContainer"){
              console.log("return to toolboxContainer");
              returnHeaderWidget = registry.byId("toolBox_headerWidget");
              // delay it until at the end
              //registry.byId("toolIconContainer").endEdit();
              if (toolViewId === "catalogContainer" || toolViewId === "toolContainer"){
                registry.byId("catalogIconContainer").endEdit();
              }
            } else if (returnToViewId === "catalogContainer"){
              returnHeaderWidget = registry.byId("catalog_headerWidget");

              var catalogHeaderRightDiv = "catalogHeaderWidgetTrailingDiv";
              returnHeaderWidget.createEditTrailingFloatRightDiv(catalogHeaderRightDiv);
              console.log("create bgTasks/profileMenu button on catalogHeader");
              returnHeaderWidget.createBgTaskButton("catalogBgTaskButton", "catalogBgTaskButtonDiv", catalogHeaderRightDiv);
              returnHeaderWidget.createProfileDesktopButton("_catalogContainer", catalogHeaderRightDiv);

              registry.byId("catalogIconContainer").startEdit();

            } else if (returnToViewId === "toolContainer"){
              returnHeaderWidget = registry.byId("tool_headerWidget");
              
              // retrieve the hashId set by toolLauncher and update the hash
              var toolHashId = returnHeaderWidget.get("hashId");
              var toolHashIdObject = { hashId: toolHashId};
              toolHash.set(toolHashIdObject);
              
              var toolHeaderRightDiv = "toolHeaderWidgetTrailingDiv";
              returnHeaderWidget.createEditTrailingFloatRightDiv(toolHeaderRightDiv);
              console.log("create bgTasks/profileMenu button on toolHeader");
              returnHeaderWidget.createBgTaskButton("toolBgTaskButton", "toolBgTaskButtonDiv", toolHeaderRightDiv);
              returnHeaderWidget.createProfileDesktopButton("_toolContainer",  toolHeaderRightDiv);    
            }
            //Details                   if (returnToViewId === "toolboxContainer" && (openFromDetails ||  (toolViewId === "catalogContainer" || toolViewId === "toolDetailContainer"))) {
            if (returnToViewId === "toolboxContainer") {

              // if coming back from catalog or details, must have been in edit mode last time we were in the toolbox
              // so, change title back, reset to having edit button, and recreate secondary search
              // remove the edit buttons from the registry if they are there (add tool from catalog ends up here)
              if (registry.byId("addToolButtonWidget")){
                registry.byId("addToolButtonWidget").destroy();
              }
              if (registry.byId("doneButtonWidget")){
                registry.byId("doneButtonWidget").destroy();
              }

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
        }
      });
      homeButton.placeAt(homeButtonDiv);

      if (platform.isPhone()) {
        var me = this;
        me.createProfileButton(toolViewId, this.trailingSecondaryBannerNode);
      } else {
        console.log("toolViewId: " + toolViewId);  
        if ("bgTasks_headerWidget" === this.id && "bgTasksContainer" === this.containerId) {
          console.log("C. bgTasks");
          var bgTasksDetailsHeaderRightDiv = "bgTasksDetailsViewHeaderWidgetTrailingDiv";
          this.createEditTrailingFloatRightDiv(bgTasksDetailsHeaderRightDiv);
          this.createBgTaskButton("bgTasksDetailsbgTaskButton", "bgTasksDetailsbgTaskButtonDiv", bgTasksDetailsHeaderRightDiv);
          registry.byId("bgTasksDetailsbgTaskButton").setDisabled(true);
          var nl = query("span[widgetid='bgTasksDetailsbgTaskButton']");
          if ( nl && nl[0] ) {
            domClass.add(nl[0], "dijitDropDownButtonHover dijitHover");    
          }
          domClass.add(registry.byId("bgTasksDetailsbgTaskButton"), "dijitDropDownButtonHover dijitHover");
          this.createProfileDesktopButton("_bgTasksContainer", bgTasksDetailsHeaderRightDiv);

        }
        else if ("catalog_headerWidget" === this.id && "catalogContainer" === this.containerId) {
          console.log("D. catalog"); 
          var catalogHeaderRightDiv = "catalogHeaderWidgetTrailingDiv";
          if (!registry.byId(catalogHeaderRightDiv)) {
            this.createEditTrailingFloatRightDiv(catalogHeaderRightDiv);
            console.log("create bgTasks/profileMenu button on catalogHeader");
            this.createBgTaskButton("catalogBgTaskButton", "catalogBgTaskButtonDiv", catalogHeaderRightDiv);
            this.createProfileDesktopButton("_catalogContainer", catalogHeaderRightDiv);    
          }                 
        } else if ("tool_headerWidget" === this.id && "toolContainer" === this.containerId) {
          console.log("E. tool");                       
          var toolHeaderRightDiv = "toolHeaderWidgetTrailingDiv";
          if (!registry.byId(toolHeaderRightDiv)) {                               
            this.createEditTrailingFloatRightDiv(toolHeaderRightDiv);
            console.log("create bgTasks/profileMenu button on toolHeader");
            this.createBgTaskButton("toolBgTaskButton", "toolBgTaskButtonDiv", toolHeaderRightDiv);
            this.createProfileDesktopButton("_toolContainer",  toolHeaderRightDiv);    
          } 
        } else if ("prefs_headerWidget" === this.id && "prefsContainer" === this.containerId) {
          console.log("F. preferences");
          var prefsHeaderRightDiv = "prefsHeaderWidgetTrailingDiv";
          this.createEditTrailingFloatRightDiv(prefsHeaderRightDiv);
          console.log("create bgTasks button on prefsHeader");
          this.createBgTaskButton("prefsBgTaskButton", "prefsBgTaskButtonDiv", prefsHeaderRightDiv);
        } 
        else if ("details_headerWidget" === this.id && "toolDetailContainer" === this.containerId) {
          console.log("G.details");
          var detailsHeaderRightDiv = "deatailsHeaderWidgetTrailingDiv";
          this.createEditTrailingFloatRightDiv(detailsHeaderRightDiv);
          console.log("create Profile button on detailsHeader");
          this.createProfileDesktopButton("toolDetailContainer",  detailsHeaderRightDiv);
          // add the back button 
          this.createBackButton("toolDetailContainer",leadingHeaderDiv);
        } 
      }

    },

    createBackButton: function(toolViewId, parentDiv){
      var mobile_icon_class = 'headerIcon backIcon';
      if (registry.byId('backButton' + toolViewId)) {
        registry.byId('backButton' + toolViewId).destroy();
        DomConstruct.destroy("backButtonDiv" + toolViewId);
        console.log("after destroy:" + dom.byId("backButtonDiv" + toolViewId));
      }
      var dt_icon_class = 'desktop headerIcon backIcon';
      if ( this.get("tooboxIconClass_mobile") ) {
        mobile_icon_class = this.get("tooboxIconClass_mobile");
      }
      if ( this.get("backIconClass_dt") ) {
        dt_icon_class = this.get("backIconClass_dt");
      }
      var button_icon_Class = platform.isPhone() ?  mobile_icon_class : dt_icon_class;
      console.log("backButton: " + 'backButton' + toolViewId);
      var backButton = new Button({
        id: 'backButton' + toolViewId,
        iconClass: button_icon_Class,
        showLabel: false,
        label: i18n.TOOLBOX_BUTTON_BACK,
        placement: 'special',
        displayMode: 'iconOnly',
        tabindex: '1',
        style: 'margin:0', // Forces the margin for the 'go back to Toolbox' button to be zero
        onClick: function(){
          console.log("clicked back button");
          registry.byId("toolDetailContainer").performTransition("catalogContainer", -1, "slide");
        }});
      var backButtonDiv = DomConstruct.toDom("<div id='backButtonDiv" + toolViewId + "'  style='display:inline-block'></div>");
      DomConstruct.place(backButtonDiv, parentDiv, "last");
      backButton.placeAt(backButtonDiv);

    },

    /**
     * Exposed for unit test.
     * @param window Seam to override the real Window
     * @param toolId Optional param a tool's menu bar
     * @returns {___module_dijit_MenuItem}
     */
    __createHelpMenuItem: function(window, toolId) {
      return new MenuItem({
        label: i18n.PROFILE_MENU_HELP_TITLE,
        id: "helpMenuItem" + (toolId ? toolId : ""),
        onClick: function() {
          // Direct user to Knowledge Center.
          var win = window.open(links.KNOWLEDGE_CENTER_URL, '_blank', 'noreferrer');
          win.focus();
        }
      });
    },
    
    createProfileButton: function(toolViewId, parentDiv) {
      if (!platform.isPhone()) {
        console.log("Not in a phone environment!");
        return;
      }

      console.log("In createProfileButton() for parentDiv " + parentDiv + " with toolViewId " + toolViewId);
      // If the button exists, destroy it. Do we also need to destroy the Menu
      // and its child MenuItems if they exist?          
      if (registry.byId('profileButtonWidget' + toolViewId)) {
        registry.byId('profileButtonWidget' + toolViewId).destroy();
        DomConstruct.destroy('profileButtonWidgetDiv' + toolViewId);
      }

      var profileButtonDiv = DomConstruct.toDom("<div id='profileButtonWidgetDiv" + toolViewId + "' style='display:inline-block;float:right;'></div>");
      DomConstruct.place(profileButtonDiv, parentDiv);

      console.log("Knowledge Center URL = " + links.KNOWLEDGE_CENTER_URL);

      // Construct the profile menu
      var profileMenu = new DropDownMenu({
        id: "profileMenu" + toolViewId,
        style: "display: none; width: 100%;"   // the div that contains this must have width: 100%
      });
      profileMenu.set("aria-label", this.userName);
      var dir = "ltr";
      if (has("adminCenter-bidi")){
        // get user preference for textDir
        var textDir = json.fromJson(BIDI_PREFS_STRING)[toolbox.getToolbox().PREFERENCE_BIDI_TEXT_DIRECTION];
        if (textDir === toolbox.getToolbox().PREFERENCE_BIDI_TEXT_DIRECTION_CONTEXTUAL){
          dir = "auto";
        } else if (textDir === toolbox.getToolbox().PREFERENCE_BIDI_TEXT_DIRECTION_RTL){
          dir = "rtl";
        }
      }
      profileMenu.addChild(new MenuItem({
        // TODO: We need to truncate or fade away user name if too long, 
        // rather than let it scroll off the screen
        label: lang.replace(i18n.LIBERTY_HEADER_LOGOUT_USERNAME, ["<span dir='" + dir + "'>" + this.userName + "</span>"]),
        id: "userNameLogoutMenuItem" + toolViewId,
        iconClass: 'headerIcon profileIcon',
        onClick: logout,
        'aria-label' : lang.replace(i18n.LIBERTY_HEADER_LOGOUT_USERNAME, [this.userName])
      }));

      // If we are in the toolbox container, add the Search menu item
      var previouslyClicked = false;
      if (toolViewId === "toolboxContainer") {
        profileMenu.addChild(new MenuItem({
          label: i18n.TOOLBOX_SEARCH,
          id: "searchMenuItem" + toolViewId,
          onClick: function() {
            var searchBoxDisplayBlockDom = dom.byId("secondarySearchBoxHeadingDiv");
            if (!previouslyClicked) {
              // Display the filter box (it is not displayed upon toolbox load)
              console.log("Will display the filter box");
              searchBoxDisplayBlockDom.style.display = "block";
              previouslyClicked = true;
            } else {
              console.log("Hiding the filter box");
              searchBoxDisplayBlockDom.style.display = "none";
              previouslyClicked = false;
            }
          }
        }));
      } else {
        console.log("Not in the toolbox, can't add Search!");
      }

      if (toolViewId !== "prefsContainer") {
        profileMenu.addChild(new MenuItem({
          label:i18n.PREFERENCES_TITLE,
          id: "preferencesMenuItem" + toolViewId,
          onClick: lang.hitch(this, function() {
            // Transition to the 'Preferences' container
            console.log("Preferences menu item clicked, going to prefs from container:" + this.containerId);
            registry.byId("prefsContentContainer").setTransitionBackTo(this.containerId);
            registry.byId(this.containerId).performTransition("prefsContainer", 1, "slide");
          })
        }));
      }
      profileMenu.addChild(this.__createHelpMenuItem(window, toolViewId));

      if (toolViewId !== "bgTasksContainer") {     
        var bgContent =  
        '<span id="backgroundTaskPopupDialogTitleDiv" hidden><b>' + i18n.BGTASKS_POPUP_RUNNING_TASK_TITLE + '</b></span>' +
        '<div id="backgroundTaskPopupDialogContentDiv" hidden>' +
        '<hr><span class="backgroundTaskPopupText" style="display:inline-block">' + i18n.BGTASKS_POPUP_RUNNING_TASK_NONE + '</span>' +  
        '</div>' + 
        // By default, the button div should not be visible as there are likely no background tasks
        '<div id="bgTasksDetailsViewButtonDiv" class="tasks-details-button-bar" hidden><p>' + 
        '<button id="bgTasksDetailsViewButton" class="mblButton blue-btn" type="submit" onClick="require([\'dijit/registry\', \'js/toolbox/toolHash\'], function(registry, toolHash) { var hashIdObj = { hashId: \'backgroundTasks\' }; toolHash.set(hashIdObj); registry.byId(\'tool_headerWidget\').launchBgTask(); });" >' +
        i18n.BGTASKS_DISPLAY_BUTTON + 
        '</button>' + 
        '</div>' + 
        '<div id="backgroundTaskPopupDialogNoneTaskDiv" hidden><span class="nobackgroundTaskPopupText">' + i18n.BGTASKS_POPUP_RUNNING_TASK_NO_ACTIVE_TASK + '</span></div>';
        profileMenu.addChild(new MenuItem({
          label: bgContent,
          id: "backgroundTasks" + toolViewId
        }));
      }
      // add css class to the drop down menu
      domClass.add(profileMenu.domNode, "profileMobileDropDownMenu");

      var profileButton = registry.byId("profileButtonWidget" + toolViewId);
      if (!profileButton) {
        profileButton = new DropDownButton({
          id: 'profileButtonWidget' + toolViewId,
          iconClass: 'headerIcon menuListIcon',
          displayMode: 'iconOnly',
          label: i18n.TOOLBOX_BUTTON_USER,
          placement: 'special',
          tabindex: '3',
          style: 'margin:0',
          dropDown: profileMenu,
          onClick: function() {
            registry.byId('tool_headerWidget').updateBgTaskPopupContent();
          }
        });
      }
      profileButton.placeAt(profileButtonDiv);
    },

    // bgtaskButtonDiv, editHeaderWidgetTrailingDiv
    createBgTaskButton: function(buttonId, buttonDiv, parentDiv){
      // Smart phones do not have a separate background task button
      if (platform.isPhone()) {
        return;
      }
      // create the div that holds the button
      console.log("createBgTaskButton buttonDiv=" + buttonDiv + " parentDiv=" + parentDiv);
      var bgTaskButtonDiv = dom.byId(buttonDiv);
      if ( bgTaskButtonDiv ) {
        DomConstruct.destroy(bgTaskButtonDiv);
      }
      bgTaskButtonDiv = DomConstruct.toDom("<div id='" + buttonDiv + "' style='float:right'></div>"); 
      DomConstruct.place(bgTaskButtonDiv, dom.byId(parentDiv));

      var bgTaskButton = registry.byId(buttonId);
      if (bgTaskButton) {
        console.log(buttonId + ' exists, destroy it');
        bgTaskButton.destroy();
      }

      if (dom.byId("backgroundTaskPopupDialogDiv")){
        DomConstruct.destroy("backgroundTaskPopupDialogDiv");
        console.log('backgroundTaskPopupDialogDiv exists, destroy it');
      }
      var ttDialog = registry.byId('backgroundTaskPopupDialog');
      if ( ttDialog ) {
        ttDialog.destroy();
        console.log('backgroundTaskPopupDialog exists, destroy it');
      }

      var bgTasksDetailsViewButton = registry.byId('bgTasksDetailsViewButton');
      if (bgTasksDetailsViewButton) {
        bgTasksDetailsViewButton.destroy();
      }
      
      var content = '<div id="backgroundTaskPopupDialogDiv">' +  
      '<span class="backgroundTaskPopupText" id="backgroundTaskPopupDialogTitleDiv" hidden>' + i18n.BGTASKS_POPUP_RUNNING_TASK_TITLE + '</span>' +
      '<div id="backgroundTaskPopupDialogContentDiv" hidden>' +
      '<hr><span id="backgroundTaskPopupDialogActiveRunningTask" class="backgroundTaskPopupText" style="display:inline-block">' + 
      i18n.BGTASKS_POPUP_RUNNING_TASK_NONE + 
      '</span></div>' + 
      // By default, the button div should not be visible as there are likely no background tasks
      '<div id="bgTasksDetailsViewButtonDiv" class="button-bar" hidden>' + 
      '<button data-dojo-type="dijit/form/Button" id="bgTasksDetailsViewButton" data-dojo-props="baseClass: \'blue-btn mblButton buttonBackground\'" type="button" ' + 
      'tabindex="-1"' + 
      '>' + 
      i18n.BGTASKS_DISPLAY_BUTTON + 
      '<script type="dojo/on" data-dojo-event="click" data-dojo-args="evt">' +
      'require([\'dijit/registry\', \'js/toolbox/toolHash\'], function(registry, toolHash) { var hashIdObj = { hashId: \'backgroundTasks\' }; toolHash.set(hashIdObj); registry.byId(\'tool_headerWidget\').launchBgTask(); });' +
      '</script>' +
      '</button>' +
      '</div></div>' + 
      '<div id="backgroundTaskPopupDialogNoneTaskDiv" style="height: 34px; padding-top: 9px; text-align: center" hidden><span class="nobackgroundTaskPopupText">' + i18n.BGTASKS_POPUP_RUNNING_TASK_NO_ACTIVE_TASK + '</span></div>';

      ttDialog = new TooltipDialog ({
        id: "backgroundTaskPopupDialog",
        baseClass: "backgroundTaskPopupDialog",
        content: content,
        "aria-labelledby": buttonId,
        onFocus: function() {
          registry.byId('tool_headerWidget').updateBgTaskPopupContent();
        }
      });
      console.log ("ttDialog=" + ttDialog);
      bgTaskButton = new DropDownButton({
        id: buttonId,
        iconClass: 'desktop headerIcon backgroundTaskIcon',
        label: i18n.TOOLBOX_BUTTON_BGTASK,
        showLabel: false,
        displayMode: 'iconOnly',
        placement: 'special',
        dropDown: ttDialog,
        maxHeight: 0,
        tabindex: '4'
      });
      bgTaskButton.startup();
      bgTaskButton.placeAt(bgTaskButtonDiv);
      console.log("div:" + bgTaskButtonDiv.id);
      
      //dom.byId("backgroundTaskPopupDialog").setAttribute(
      //        "style", "height: 52px; padding-top: 18px");
    },


    updateBgTaskPopupContent: function() {
      // first param is the call back function, once all ansync calls are finished, the showBGTasks tree will be displayed
      var bgTaskPopup = new BGTasksPopup();
      backgroundTasks.setParams(bgTaskPopup.showBGTasks, 
          bgTaskPopup.showNone,
          bgTaskPopup.getStatusDisplay(), 
          bgTaskPopup.getTitle(),
          320);
      var _dURLAll = "/ibm/api/collective/v1/deployment";
      backgroundTasks.getActiveTaskCount(_dURLAll, "json", bgTaskPopup.toggleButton);
    },

    launchBgTask: function() {
      popup.close(registry.byId('ttDialog'));

      // finds the visible view
      var mainContainerChildNodes = dom.byId('mainContainer').childNodes;
      console.log("find divs = " + mainContainerChildNodes);
      // the loop always use the last div, because div 'idx/app/A11yPrologue' is always visible and it is always the first element returned
      var returnToViewId = "toolboxContainer";
      array.forEach(mainContainerChildNodes, function(w) {
        if ( w.nodeType === 1) {
          var visibility = (w.offsetHeight !== 0 ) ? true:false;
          console.log("find div " + w.id + ", visibility is " + visibility);
          if ( visibility === true ) {
            returnToViewId = w.id;
          }
        }
      });

      registry.byId("bgTasks_headerWidget").set("tooboxIconClass_mobile", "headerIcon backIcon");
      registry.byId("bgTasks_headerWidget").set("tooboxIconClass_dt", "desktop headerIcon backIcon");
      console.log("*********** container id = " + this.containerId);
      registry.byId("bgTasks_headerWidget").createToolHeading(this.secondaryTitle, "bgTasksContainer", returnToViewId);
      registry.byId("bgTasks_headerWidget").set("tooboxIconClass_mobile", "");
      registry.byId("bgTasks_headerWidget").set("tooboxIconClass_dt", "");
      registry.byId("bgTasks_headerWidget").set("secondaryTitle", i18n.BGTASKS_PAGE_LABEL);
      console.log("Created background task heading");

      var _dURL = "/ibm/api/collective/v1/deployment";  
      // first param is the call back function, once all ansync calls are finished, the showBGTasks tree will be displayed
      backgroundTasks.setParams(registry.byId("bgTasksTreeContainer").showBGTasks, 
          null,
          registry.byId("bgTasksTreeContainer").getStatusDisplay(), 
          registry.byId("bgTasksTreeContainer").getTitle() );
      console.log("Run getRoot method");
      backgroundTasks.getRoot(_dURL, "json" );
      registry.byId(returnToViewId).performTransition("bgTasksContainer", 1, "slide");
      console.log("Slide in the view");
    },

    createFilterButton: function(rightTrailingHeaderDiv) {
      // Smart phone do not have the filter button
      if (platform.isPhone()) {
        return;
      }

      // float the filter icon to the right of the existing icons 
      var filterButtonDiv = DomConstruct.toDom("<div id='filterButtonWidgetDiv' style='display:inline-block;float:right;height:70px;'></div>");
      DomConstruct.place(filterButtonDiv, rightTrailingHeaderDiv);

      var filterButton = registry.byId("filterButtonWidget");
      if (!filterButton) {
        filterButton = new Button({
          id: 'filterButtonWidget',
          widget: 'filterButtonWidget',
          iconClass: 'desktop headerIcon filterIcon',
          displayMode: 'iconOnly',
          label: i18n.TOOLBOX_SEARCH,
          showLabel: false,
          placement: 'special',
          tabindex: '2',
          onClick: function(){
            registry.byId("toolIconContainer").refreshSearchPane('block', 'none');
            registry.byId("secondarySearchBoxInput").focus();
            console.log("filter button is clicked");
          }
        });
      }

      filterButton.placeAt(filterButtonDiv);

      // if filter pane is there, disable the filter icon. This could happen when done is clicked
      // to end the edit initiated while doing filter
      var filterDisplayBlockDom = dom.byId("secondarySearchBoxHeadingDiv"); 
      console.log("filterDisplayBlockDom: ", filterDisplayBlockDom);
      if (filterDisplayBlockDom !== null && filterDisplayBlockDom !== undefined) {
        var displayStyle = filterDisplayBlockDom.style.display;
        console.log("filter box display block dom: ", displayStyle);
        if (displayStyle === "block") {
          var filterButtonDom = dom.byId("filterButtonWidget");
          filterButtonDom.style.display = 'none';
        }
      }
    },

    createProfileDesktopButton: function(viewId, parentDiv) {
      if (platform.isPhone()) {
        return;
      }

      console.log("createProfileDesktopButton() parentDiv " + parentDiv + ", viewId=" + viewId);

      // If the button exists, destroy it. Do we also need to destroy the Menu
      // and its child MenuItems if they exist?          
      if (registry.byId('profileButtonWidget' + viewId)) {
        console.log("destroy widget profileButtonWidget"+viewId);
        registry.byId('profileButtonWidget' + viewId).destroy();
        DomConstruct.destroy('profileButtonWidgetDiv' + viewId);
      }

      // create profile button to the right of existing icons
      var profileButtonDiv = DomConstruct.toDom("<div id='profileButtonWidgetDiv" + viewId + "' style='display:inline-block;float:right'></div>");
      DomConstruct.place(profileButtonDiv, parentDiv);

      console.log("Knowledge Center URL = " + links.KNOWLEDGE_CENTER_URL);

      var profileMenu = registry.byId("toolboxProfileMenu");
      console.log("profileDropDownMenu exists? " + profileMenu); 
      if ( profileMenu ) {
        profileMenu.destroyRecursive();
      }

      // Construct the profile menu
      console.log("create profileDropDownMenu"); 
      profileMenu = new DropDownMenu({
        id: "toolboxProfileMenu"
      });

      profileMenu.set("aria-label", this.userName);
      var dir = "ltr";
      if (has("adminCenter-bidi")){
        // get user preference for textDir
        var textDir = json.fromJson(BIDI_PREFS_STRING)[toolbox.getToolbox().PREFERENCE_BIDI_TEXT_DIRECTION];
        if (textDir === toolbox.getToolbox().PREFERENCE_BIDI_TEXT_DIRECTION_CONTEXTUAL){
          dir = "auto";
        } else if (textDir === toolbox.getToolbox().PREFERENCE_BIDI_TEXT_DIRECTION_RTL){
          dir = "rtl";
        }
      }
      profileMenu.addChild(new MenuItem({
        // TODO: We need to truncate or fade away user name if too long, 
        // rather than let it scroll off the screen
        label: lang.replace(i18n.LIBERTY_HEADER_LOGOUT_USERNAME, ["<span dir='" + dir + "'>" + this.userName + "</span>"]),
        id: "userNameLogoutMenuItem",
        onClick: logout,
        'aria-label' : lang.replace(i18n.LIBERTY_HEADER_LOGOUT_USERNAME, [this.userName])
      }));

      profileMenu.addChild(new MenuSeparator());
      profileMenu.addChild(new MenuItem({
        label:i18n.PREFERENCES_TITLE,
        id: "preferencesMenuItem",
        onClick: lang.hitch(this, function() {
          // Transition to the 'Preferences' container
          console.log("Preferences menu item clicked, going to prefs from container:" + this.containerId);
          registry.byId("prefsContentContainer").setTransitionBackTo(this.containerId);
          registry.byId(this.containerId).performTransition("prefsContainer", 1, "slide");
        })
      }));

      profileMenu.addChild(new MenuSeparator());
      profileMenu.addChild(this.__createHelpMenuItem(window));

      var profileButtonId = 'profileButtonWidget' + viewId;
      var profileButton = registry.byId(profileButtonId);
      console.log(profileButtonId + " exists? ", profileButton);
      if (!profileButton) {
        console.log("create profileButtonWidget" + viewId);
        profileButton = new DropDownButton({
          id: profileButtonId, 
          widget: profileButtonId,
          iconClass: 'desktop headerIcon profileIcon',
          displayMode: 'iconOnly',
          label: i18n.TOOLBOX_BUTTON_USER,
          showLabel: false,
          placement: 'special',
          tabindex: '3',
          dropDown: profileMenu
        });
      }
      profileButton.placeAt(profileButtonDiv);

      domClass.add(profileMenu.domNode, "profileMenu");
      profileMenu.on('open', function(evt) {
        query(".dijitPopup").forEach(function(node) {
          if ( !domClass.contains(node, "profileMenuPopup") ) { 
            domClass.add(node, "profileMenuPopup");
          }
        });
      });
      console.log("end create profile menu");
    },

    performDone: function() {
      console.log("in performDone");
      registry.byId('toolIconContainer').refreshToolOrdering();
      // clicked Done button to end edit mode
      // first recreate the secondary search
      this._createSecondarySearch();
      // now create the div containing the edit pencil button
      this.createEditDiv();
      this.createEditTrailingFloatRightDiv("editHeaderWidgetRightTrailingDiv");
      this.createBgTaskButton("bgTaskButton", "bgTaskButtonDiv", "editHeaderWidgetRightTrailingDiv");
      this.createProfileDesktopButton("_toolboxContainer", "editHeaderWidgetRightTrailingDiv");
      this.createFilterButton("editHeaderWidgetRightTrailingDiv");

      // switch title back to My toolbox
      this.set("secondaryTitle", i18n.TOOLBOX_TITLE);
      registry.byId("toolIconContainer").endEdit();
      console.log("done button is clicked");
    }

  });

});