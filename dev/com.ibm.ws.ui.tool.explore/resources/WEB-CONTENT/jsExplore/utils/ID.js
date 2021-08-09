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
define(["jsShared/utils/ID","dojo/_base/lang","dojo/domReady!"], function(sharedID,lang) {
  
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
    
    DEPLOY : "Deploy",
    EXPLORE : "Explore",
    
    //VIEWS - search, dashboard, collection, object
    
    COLLECTION : "Collection",
    DASHBOARD : "Dashboard",
    OBJECT : "Object",
    OVERVIEW : "Overview",
    SUMMARY : "Summary",
    VIEW : "View",

    //RESOURCES
    
    APPLICATION : "Application",
    APPLICATIONS : "Applications",
    APP : "App",
    APPS : "Apps",
    CLUSTER : "Cluster",
    CLUSTERS : "Clusters",
    CLUSTERED : "Clustered",
    HOST : "Host",
    HOSTS : "Hosts",
    RESOURCE : "Resource",
    RUNTIME : "Runtime",
    RUNTIMES : "Runtimes",
    SERVER : "Server",
    SERVERS : "Servers",
    
    //PARTS AND PIECES
    
    ALERT : "Alert",
    ALERTS : "Alerts",
    API : "Api",
    BAR : "Bar",
    BORDER: "Border",
    BOX : "Box",
    BUTTON : "Button",
    BUTTONS : "Buttons",
    CARD : "Card",
    CARDS : "Cards",
    CHART: "Chart",
    CONTAINER : "Container",
    DIALOG : "Dialog",
    DLG : "Dlg",
    FIELD : "Field",
    FILTER : "Filter",
    GRAPH : "Graph",
    GRAPHS : "Graphs",
    GRAPHIC : "Graphic",
    GRID : "Grid",
    ICON : "Icon",
    IMG : "Img",
    ITEM : "Item",
    LABEL : "Label",
    LIST : "List",
    MENU : "Menu",
    NAME : "Name",
    NODE : "Node",
    NUMBER : "Number",
    PAGE : "Page",
    PANEL : "Panel",
    PARENT : "Parent",
    HEADING : "Heading",
    PILL : "Pill",
    POPUP : "Popup",
    SLIDER : "Slider",
    STACK : "Stack",
    TAB :"Tab",
    TABLE : "Table",
    TAG : "Tag",
    TAGS : "Tags",
    TEXT : "Text",
    TITLE : "Title",
    TOGGLE : "Toggle",
    TOOL : "Tool",
    TOOLTIP : "Tooltip",
    TYPE : "Type",
    USERDIR : "UserDir",
    
    //DESCRIPTIONS
    
    ABOVE : "Above",
    ACCESS : "Access",
    ACTIONS : "Actions",
    ACTION : "Action",
    ACTIVE : "Active",
    AFFINITY : "Affinity",
    ALL : "All",
    ATTR : "Attr",
    ATTRS : "Attrs",
    ATTRIBUTE : "Attribute",
    ATTRIBUTES : "Attributes",
    AVG : "Avg",
    CANCEL : "Cancel",
    CENTER : "Center",
    CLASSES : "Classes",
    CLEAN : "Clean",
    CLOSE : "Close",
    COMBO : "Combo",
    CONFIG : "Config",
    CONFIGURATION : "Configuration",
    CONFIRMATION : "Confirmation",
    CONN : "Conn",
    CONNECTION : "Connection",
    CONTACT : "Contact",
    CONTACTS : "Contacts",
    CONTENT : "Content",
    CONTROLLER : "Controller",
    COUNT : "Count",
    CPU : "CPU",
    DELETE : "Delete",
    DETAIL : "Detail",
    DIRTY : "Dirty",
    DISPLAY : "Display",
    DROPDOWN : "DropDown",
    EDIT : "Edit",
    END :"End",
    EXPANDED : "Expanded",
    FADING : "Fading",
    FFDC : "ffdc",
    FFDC_CAPS : "FFDC",
    HEAP : "Heap",
    INST : "Inst",
    INSTANCES : "Instances",
    INNER : "Inner",
    INPUT : "Input",
    JAXWS : "Jaxws",
    LEFT : "Left",
    LEGEND : "Legend",
    LIVE : "Live",
    LOG : "Log",
    MAIN : "Main",
    MAINTENANCE : "Maintenance",
    MANAGED : "Managed",
    MEAN : "Mean",
    MEMORY : "Memory",
    MESSAGE: "Message",
    MESSAGES : "Messages",
    METADATA : "Metadata",
    MGMT_CAPS : "MGMT",
    MINIMISE : "Minimise",
    MINIMISED : "Minimised",
    MODE : "Mode",
    MORE : "More",
    MSG : "Msg",
    MULTI : "Multi",
    NEW : "New",
    NONE : "None",
    NOTE : "Note",
    NOTES : "Notes",
    NOTIFICATION : "Notification",
    OBSERVER : "Observer",
    OWNER : "Owner",
    OVERLAY : "Overlay",
    POOL : "Pool",
    PORT : "Port",
    POSITION : "Position",
    PROCESS : "Process",
    PROXY : "Proxy",
    QUALIFIER : "Qualifier",
    REMOVED : "Removed",
    REQUEST : "Request",
    REQ : "Req",
    RESPONSE : "Response",
    RESP : "Resp",
    RIGHT : "Right",
    ROW : "Row",
    SAVE : "Save",
    SELECT : "Select",
    SELECTED : "Selected",
    SELECTION : "Selection",
    SERVLET : "Servlet",
    SESSION : "Session",
    SESSIONS : "Sessions",
    SIDE : "Side",
    SINGLE : "Single",
    STANDALONE : "Standalone",
    START : "Start",
    STATE : "State",
    STATS : "Stats",
    STATUS : "Status",
    SUB : "Sub",
    THREAD : "Thread",
    THREADS : "Threads",
    TOTAL : "Total",
    TRACE : "Trace",
    UNSAVED : "Unsaved",
    UPDATING : "Updating",
    URL : "Url",
    USED : "Used",
    VALUE : "Value",
    WAIT : "Wait",
    WARNING : "Warning",
    WC : "Wc",
    WIDTH : "Width",
    
    //STATES
    ERROR : "Error",
    ERR : "Err",
    FAILED : "Failed",
    PARTIALLY_STARTED_CAPS : "PARTIALLY_STARTED",
    RESTART : "Restart",
    RUNNING : "Running",
    START : "Start",
    STARTED: "Started",
    STARTED_CAPS : "STARTED",
    STOP : "Stop",
    STOPPED : "Stopped",
    STOPPED_CAPS : "STOPPED",
    UNKNOWN : "Unknown",
    UNKNOWN_CAPS : "UNKNOWN",
    
    //OTHER
    
    SVG: "Svg", //graph stuff
    SVGG : "SvgG",
    LEGENDSVG : "Legendsvg",
    TIME: "Time",
    SELECTOR : "Selector",
    
    ADD :"Add",
    AND : "And",
    ASK : "Ask",
    BREAK : "Break",
    CONFIGURE : "Configure",
    FORCE : "Force",
    SET : "Set",
    WONT : "Wont",
    
    SHOW : "Show",
    HIDE : "Hide",
    
    NO : "No",
    YES : "Yes",
    
    MM : "Mm",
    JVM : "Jvm",
    JVM_CAPS : "JVM",
    
    ENABLE : "Enable",
    DISABLE : "Disable",
    
    OVHP_CAPS : "OVHP",
    
    CHECKMARK : "CheckMark",
    
    AC_CAPS : "AC",
    CP : "Cp",
    
    //Connectors
    
    TO : "To",
    ON : "On",
    BY : "By",
    FOR : "For",
    UNDERSCORE : "_",
    OPENPAREN : "(",
    CLOSEPAREN : ")",
    
    //Times 
    
    Y : "y",
    Mo : "mo",
    W : "w",
    D : "d",
    H : "h",
    M : "m",
    
    //Numbers
    
    TEN : "10",
    FIVE : "5",
    ONE : "1",
    TWO : "2",
    
//    // ObjectViewHeaderPane (Not sure how we are grouping ids)
//    
    SERVERAPICONTAINER : "ServerApiContainer",
//    ServerApiContainerImg : "ServerApiContainerImg",
//    ServerApiContainerUrlEnabled : "ServerApiContainerUrlEnabled",
//    ServerApiContainerUrlDisabled : "ServerApiContainerUrlDisabled",
    
    //HELPER functions
    
    getId : function(){
      return this.camel(this.ID);
    },
    
    //JUST RESOURCES

    getApplication : function(){
      return this.camel(this.APPLICATION);
    },
    
    getApplications : function(){
      return this.camel(this.APPLICATIONS);
    },
    
    getApp : function(){
      return this.camel(this.APP);
    },

    getApps : function(){
      return this.camel(this.APPS);
    },
    
    getAppInstStatsSelectorUpper : function(){
      return this.APP + this.INST + this.STATS + this.SELECTOR;
    },
    
    getInstances : function(){
      return this.camel(this.INSTANCES);
    },

    getAppInstancesUpper : function(){
      return this.APP + this.INSTANCES;
    },
    
    getAppInstances : function(){
      return this.camel(this.APP + this.INSTANCES);
    },//to be deleted
    
    getAppList : function(){
      return this.camel(this.APP + this.LIST);
    },

    getCluster : function(){
      return this.camel(this.CLUSTER);
    },
    
    getClusters : function(){
      return this.camel(this.CLUSTERS);
    },
    
    getClusterAppMetadataMsgContainerUpper : function(){
      return this.CLUSTER + this.APP + this.METADATA + this.MSG + this.CONTAINER;
    },
    
    getClusteredMetadataMsgContainer : function(){
      return this.camel(this.CLUSTERED + this.METADATA + this.MSG + this.CONTAINER);
    },
    
    getConfigureGraphsUpper : function(){
      return this.CONFIGURE + this.GRAPHS;
    },
    
    getConfigureGraphsCancelButtonUpper : function(){
      return this.CONFIGURE + this.GRAPHS + this.CANCEL + this.BUTTON;
    },
    
    getConfigureGraphsSaveButtonUpper : function(){
      return this.CONFIGURE + this.GRAPHS + this.SAVE + this.BUTTON;
    },
    
    getHost : function(){
      return this.camel(this.HOST);
    },

    getHosts : function(){
      return this.camel(this.HOSTS);
    },
    
    getServer : function(){
      return this.camel(this.SERVER);
    },
    
    getServers : function(){
      return this.camel(this.SERVERS);
    },
    
    getRuntime : function(){
      return this.camel(this.RUNTIME);
    },
    
    getRuntimeUpper : function(){
      return this.RUNTIME;
    },
    
    getRuntimes : function(){
      return this.camel(this.RUNTIMES);
    },
    
    getRuntimesUpper : function(){
      return this.RUNTIMES;
    },
    
    getStandaloneServer : function(){
      return this.camel(this.STANDALONE + this.SERVER);
    },
 
    //RESOURCE AND LOCATION
    getMultiSelectListViewButton: function() {
      return this.camel(this.MULTI + this.SELECT + this.LIST + this.VIEW + this.BUTTON);
    },
    
    getRowSelectButton: function() {
      return this.camel(this.ROW + this.SELECT + this.BUTTON);
    },
    
    getApplicationGrid : function(){
      return this.camel(this.APPLICATION + this.GRID);
    },
    
    getAppInstStatsPaneUpper : function(){
      return this.APP + this.INST + this.STATS + this.PANE;
    },

    getClusterGrid : function(){
      return this.camel(this.CLUSTER + this.GRID);
    },
    
    getHostGrid : function(){
      return this.camel(this.HOST + this.GRID);
    },
    
    getHostMaintenanceModeNotificationCenterUpper : function(){
      return this.HOST + this.MAINTENANCE + this.MODE + this.NOTIFICATION + this.CENTER;
    },
    
    getHostMaintenanceModeNotificationContainerUpper : function(){
      return this.HOST + this.MAINTENANCE + this.MODE + this.NOTIFICATION + this.CONTAINER;
    },
    
    getServerApiContainerUpper : function(){
      return this.SERVER + this.API + this.CONTAINER;
    },

    getServerApiContainerImgUpper : function(){
      return this.SERVER + this.API + this.CONTAINER + this.IMG;
    },

    getServerApiContainerUrlDisabledUpper : function(){
      return this.SERVER + this.API + this.CONTAINER + this.URL + this.DISABLED;
    },

    getServerApiContainerUrlEnabledUpper : function(){
      return this.SERVER + this.API + this.CONTAINER + this.URL + this.ENABLED;
    },
    
    getServerGrid : function(){
      return this.camel(this.SERVER + this.GRID);
    },

    getApplicationObserverUpper : function(){
      return this.APPLICATION + this.OBSERVER;
    },

    getServerObserverUpper: function(){
      return this.SERVER + this.OBSERVER;
    },
    
    getServersList : function(){
      return this.camel(this.SERVERS + this.LIST);
    },
    
    getServerStatsPaneUpper : function(){
      return this.SERVER + this.STATS + this.PANE;
    },
    
    getHostProxyPage : function(){
      return this.camel(this.HOST + this.PROXY + this.PAGE);
    },
    
    getRuntimeGrid : function(){
      return this.camel(this.RUNTIME + this.GRID);
    },
    
    //RESOURCE ON RESOURCE
    
    getResourceOnResource : function(big, little){
      return big + this.OPENPAREN + little + this.CLOSEPAREN;
    },

    getAppInstancesByCluster : function(){
      return this.camel(this.APP + this.INSTANCES + this.BY + this.CLUSTER);
    },
    
    getAppInstancesByClusterUpper : function(){
      return this.APP + this.INSTANCES + this.BY + this.CLUSTER;
    },
    
    getAppOnClusterUpper : function(){
      return this.APP + this.ON + this.CLUSTER;
    },
    
    getAppsOnClusterUpper : function(){
      return this.APPS + this.ON + this.CLUSTER;
    },
    
    getAppOnCluster : function(){
      return this.camel(this.APP + this.ON + this.CLUSTER);
    },
    
    getAppsOnCluster : function(){
      return this.camel(this.APPS + this.ON + this.CLUSTER);
    },

    getAppsOnServerUpper : function(){
      return this.APPS + this.ON + this.SERVER;
    },
    
    getAppsOnServer : function(){
      return this.camel(this.APPS + this.ON + this.SERVER);
    },
    
    getRuntimesOnHostUpper : function(){
      return this.RUNTIMES + this.ON + this.HOST;
    },
    
    getRuntimesOnHost : function(){
      return this.camel(this.RUNTIMES + this.ON + this.HOST);
    },
    
    getServersOnClusterUpper : function(){
      return this.SERVERS + this.ON + this.CLUSTER;
    },
    
    getServersOnCluster : function(){
      return this.camel(this.SERVERS + this.ON + this.CLUSTER);
    },
    
    getServersOnHostUpper : function(){
      return this.SERVERS + this.ON + this.HOST;
    },
    
    getServersOnHost : function(){
      return this.camel(this.SERVERS + this.ON + this.HOST);
    },
    
    getServersOnRuntimeUpper : function(){
      return this.SERVERS + this.ON + this.RUNTIME;
    },
    
    getServersOnRuntime : function(){
      return this.camel(this.SERVERS + this.ON + this.RUNTIME);
    },

    //BREADCRUMB   
    
    getBreadcrumbAndSearchDiv : function (){
      return this.camel(this.BREADCRUMB + this.AND + this.SEARCH + this.DIV);
    },
    
    getBreadcrumbContainer : function(){
      return this.camel(this.BREADCRUMB + this.CONTAINER + this.DASH + this.ID);
    },
    
    getBreadcrumbController : function(){
      return this.camel(this.BREADCRUMB + this.CONTROLLER);
    },
    
    getBreadcrumbDiv : function(){
      return this.camel(this.BREADCRUMB + this.DIV);
    },
    
    getBreadcrumbStackContainer : function(){
      return this.camel(this.BREADCRUMB + this.STACK + this.CONTAINER + this.DASH + this.ID);
    },
    
    //LOCATIONS
    
    getActions : function(){
      return this.camel(this.ACTIONS);
    },
    
    getActionBar : function(){
      return this.camel(this.ACTION + this.BAR);
    },
    
    getActionButton : function(){
      return this.camel(this.ACTION + this.BUTTON);
    },
    
    getActionButtonUpper : function(){
      return this.ACTION + this.BUTTON;
    },
    
    getActionConfirmationPopup : function(){
      return this.camel(this.ACTION + this.CONFIRMATION + this.POPUP);
    },
    
    getActionMenu : function(){
      return this.camel(this.ACTION + this.MENU);
    },
    
    getActionMoreButton : function(){
      return this.camel(this.ACTION + this.MORE + this.BUTTON);
    },
    
    getActionPane : function(){
      return this.camel(this.ACTION + this.PANE);
    },

    getAlertDialog : function(){
      return this.camel(this.ALERT + this.DIALOG);
    },
    
    getAlertIcon : function(){
      return this.camel(this.ALERT + this.ICON);
    },
    
    getAlertPaneUpper : function(){
      return this.ALERT + this.PANE;
    },

    getBorderContainerUpper : function(){
      return this.BORDER + this.CONTAINER;
    },
    
    getBox : function(){
      return this.camel(this.BOX);
    },
    
    getBreakAffinityToggleButton : function(){
      return this.camel(this.BREAK + this.AFFINITY + this.TOGGLE + this.BUTTON);
    },
    
    getButton : function(){
      return this.camel(this.BUTTON);
    },
    
    getButtonUpper : function(){
      return this.BUTTON;
    },

    getCard : function(){
      return this.camel(this.CARD);
    },
    
    getCardUpper : function(){
      return this.CARD;
    },
    
    getCards : function(){
      return this.camel(this.CARDS);
    },

    getCardIcon : function(){
      return this.camel(this.CARD + this.ICON);
    },
    
    getCenterPane : function(){
      return this.camel(this.CENTER + this.PANE);
    },
    
    getChart : function(){
      return this.camel(this.CHART);
    },
    
    getChartNode : function(){
      return this.camel(this.CHART + this.NODE);
    },
    
    getCollectionView : function(){
      return this.camel(this.COLLECTION + this.VIEW);
    },
    
    getComboBox : function(){
      return this.camel(this.COMBO + this.BOX)
    },
    
    getConfigList : function(){
      return this.camel(this.CONFIG + this.LIST);
    },
    
    getConfigListSelect : function(){
      return this.camel(this.CONFIG + this.LIST + this.SELECT);
    },
    
    getConfirmationPopup : function(){
      return this.camel(this.CONFIRMATION + this.POPUP);
    },
    
    getConnectionStatsUpper : function(){
      return this.CONNECTION + this.STATS;
    },
    
    getContainer : function(){
      return this.camel(this.CONTAINER);
    },
    
    getContainerPaneUpper : function(){
      return this.CONTAINER + this.PANE;
    },
    
    getContactAttrsPane : function(){
      return this.camel(this.CONTACT + this.ATTRS + this.PANE);
    },
    
    getContactContainerUpper : function(){
      return this.CONTACT + this.CONTAINER;
    },
    
    getContactsIcon : function(){
      return this.camel(this.CONTACTS + this.ICON);
    },
    
    getContactTagPaneUpper : function(){
      return this.CONTACT + this.TAG + this.PANE;
    },

    getContentPaneInner : function(){
      return this.camel(this.CONTENT + this.PANE + this.INNER);
    },
    
    getContentPaneUpper : function(){
      return this.CONTENT + this.PANE;
    },
    
    getDashboard : function(){
      return this.camel(this.DASHBOARD);
    },
    
    getDashboardPaneUpper : function(){
      return this.DASHBOARD + this.PANE;
    },
    
    getDeleteGraphButton : function(){
      return this.camel(this.DELETE + this.GRAPH + this.BUTTON);
    },
    
    getDeployButton : function(){
      return this.camel(this.DEPLOY + this.BUTTON);
    },
    
    getDirtyConfigDialog : function(){
      return this.camel(this.DIRTY + this.CONFIG + this.DIALOG);
    },
    
    getDropDownMenu : function(){
      return this.camel(this.DROPDOWN + this.MENU);
    },
    
    getEditButton : function(){
      return this.camel(this.EDIT + this.BUTTON);
    },
    
    getEndTimeDisplay : function(){
      return this.camel(this.END + this.TIME + this.DISPLAY);
    },

    getEndTimeSlider : function(){
      return this.camel(this.END + this.TIME + this.SLIDER);
    },

    getErrorMessagePaneUpper : function(){
      return this.ERROR + this.MESSAGE + this.PANE;
    },
    
    getExpandedView : function(){
      return this.camel(this.EXPANDED + this.VIEW);
    },
    
    getExploreContainerForConfigTool : function(){
      return this.camel(this.EXPLORE + this.CONTAINER + this.FOR + this.CONFIG + this.TOOL);
    },
    
    getFilterBar : function(){
      return this.camel(this.FILTER + this.BAR);
    },
    
    getFilterBarUpper : function(){
      return this.FILTER + this.BAR;
    },
    
    getFilterPane : function(){
      return this.camel(this.FILTER + this.PANE);
    },
    
    getGraph : function(){
      return this.camel(this.GRAPH);
    },
    
    getGraphWarningMessage : function(){
      return this.camel(this.GRAPH + this.WARNING + this.MESSAGE);
    },
    
    getGraphsPaneUpper : function(){
      return this.GRAPHS + this.PANE;
    },
    
    getIconUpper : function(){
      return this.ICON;
    },
    
    getInput : function(){
      return this.camel(this.INPUT);
    },
    
    getGraphic : function(){
      return this.camel(this.GRAPHIC);
    },

    getLabel : function(){
      return this.camel(this.LABEL);
    },
    
    getMain : function(){
      return this.camel(this.MAIN);
    },
    
    getMainContentPaneUpper : function(){
      return this.MAIN + this.CONTENT + this.PANE;
    },

    getMainDashboard : function(){
      return this.camel(this.MAIN + this.DASHBOARD);
    },
    
    getMaintenanceActionMenuItem : function(){
      return this.camel(this.MAINTENANCE + this.ACTION + this.MENU + this.ITEM);
    },
    
    getMaintenanceModeButton : function(){
      return this.camel(this.MAINTENANCE + this.MODE + this.BUTTON);
    },
    
    getStartCleanActionMenuItem : function(){
      return this.camel(this.START + this.CLEAN + this.ACTION + this.MENU + this.ITEM);
    },
    
    getMenuItem : function(){
      return this.camel(this.MENU + this.ITEM);
    },
    
    getMessagesFilter : function(){
      return this.camel(this.MESSAGES + this.FILTER);
    },
    
    getMessagesGraphUpper : function(){
      return this.MESSAGES + this.GRAPH;
    },
    
    getMessageGrid : function(){
      return this.camel(this.MESSAGE + this.GRID);
    },

    getMessagesTableUpper : function(){
      return this.MESSAGES + this.TABLE;
    },
    
    getMinimiseButton : function(){
      return this.camel(this.MINIMISE + this.BUTTON);
    },
    
    getMinimisedView : function(){
      return this.camel(this.MINIMISED + this.VIEW);
    },
    
    getNewCard : function(){
      return this.camel(this.NEW + this.CARD);
    },
    
    getNewContactEditField : function(){
      return this.camel(this.NEW + this.CONTACT + this.EDIT + this.FIELD);
    },
    
    getNewTagEditField : function(){
      return this.camel(this.NEW + this.TAG + this.EDIT + this.FIELD);
    },
    
    getNewOwnerEditField : function(){
      return this.camel(this.NEW + this.OWNER + this.EDIT + this.FIELD);
    },
    
    getNumberUpper : function(){
      return this.NUMBER;
    },

    getObjectView : function(){
      return this.camel(this.OBJECT + this.VIEW);
    },
    
    getOVHP : function(){
      return this.OVHP_CAPS;
    },
    
    getOwnerAttrsPane : function(){
      return this.camel(this.OWNER + this.ATTRS + this.PANE);
    },
    
    getOwnerContainerUpper : function(){
      return this.OWNER + this.CONTAINER;
    },
    
    getOwnerIcon : function(){
      return this.camel(this.OWNER + this.ICON);
    },
    
    getOwnerTagPaneUpper : function(){
      return this.OWNER + this.TAG + this.PANE;
    },
    
    getPane : function(){
      return this.camel(this.PANE);
    },
    
    getPaneUpper : function(){
      return this.PANE;
    },
    
    getProcessCPUStatsUpper : function(){
      return this.PROCESS + this.CPU + this.STATS;
    },
    
    getResourceFilter : function(){
      return this.camel(this.RESOURCE + this.FILTER);
    },
    
    getResourceName : function(){
      return this.camel(this.RESOURCE + this.NAME);
    },
    
    getResourceNameUpper : function(){
      return this.RESOURCE + this.NAME;
    },
    
    getResourceNameQualifier1Upper : function(){
      return this.RESOURCE + this.NAME + this.QUALIFIER + this.ONE;
    },
    
    getResourceNameQualifier2Upper : function(){
      return this.RESOURCE + this.NAME + this.QUALIFIER + this.TWO;
    },
    
    getResourceNoteUpper : function(){
      return this.RESOURCE + this.NOTE;
    },

    getResourcePage : function(){
      return this.camel(this.RESOURCE + this.PAGE);
    },
    
    getResourceState : function(){
      return this.camel(this.RESOURCE + this.STATE);
    },
    
    getResourceTag : function(){
      return this.camel(this.RESOURCE + this.TAG);
    },
    
    getResourceType : function(){
      return this.camel(this.RESOURCE + this.TYPE);
    },

    getResourceTypeSelect : function(){
      return this.camel(this.RESOURCE + this.TYPE + this.SELECT);
    },
    
    getResourceToggleUpper : function(){
      return this.RESOURCE + this.TOGGLE;
    },
    
    getRestartButton : function(){
      return this.camel(this.RESTART + this.BUTTON);
    },
    
    getRight : function(){
      return this.camel(this.RIGHT);
    },
    
    getSearch : function(){
      return this.camel(this.SEARCH);
    },
    
    getSearchDiv: function() {
      return this.camel(this.SEARCH + this.DIV);
    },
    
    getSearchBoxAddPill : function(){
      return this.camel(this.SEARCH + this.BOX + this.ADD + this.PILL);
    },
    
    getSearchBoxButtonsDiv : function(){
      return this.camel(this.SEARCH + this.BOX + this.BUTTONS + this.DIV);
    },
    
    getSearchBoxClear : function(){
      return this.camel(this.SEARCH + this.BOX + this.CLEAR);
    },
    
    getSearchBoxSearch : function(){
      return this.camel(this.SEARCH + this.BOX + this.SEARCH);
    },
    
    getSearchButton : function(){
      return this.camel(this.SEARCH + this.BUTTON);
    },
    
    getSearchFieldContentPanel : function(){
      return this.camel(this.SEARCH + this.FIELD + this.CONTENT + this.PANEL);
    },
    
    getSearchFieldContentPane : function(){
      return this.camel(this.SEARCH + this.FIELD + this.CONTENT + this.PANE);
    },
    
    getSearchFieldIconContentPanel : function(){
      return this.camel(this.SEARCH + this.FIELD + this.ICON + this.CONTENT + this.PANEL);
    },
    
    getSearchIconContentPane : function(){
      return this.camel(this.SEARCH + this.ICON + this.CONTENT + this.PANE);
    },
    
    getSearchMainBox : function(){
      return this.dashDelimit(this.getSearch(), this.getMain(), this.getBox());
    },
    
    getSearchPane : function(){
      return this.camel(this.SEARCH + this.PANE);
    },
    
    getSearchPill : function(){
      return this.camel(this.SEARCH + this.PILL);
    },
    
    getSearchStackContainer : function(){
      return this.camel(this.SEARCH + this.STACK + this.CONTAINER);
    },
    
    getSearchTextBox : function(){
      return this.dashDelimit(this.getSearch(), this.getText(), this.getBox());
    },

    getSearchView : function(){
      return this.camel(this.SEARCH + this.VIEW);
    },
    
    getSelect : function(){
      return this.camel(this.SELECT);
    },
    
    getSelectAllButton : function(){
      return this.camel(this.SELECT + this.ALL + this.BUTTON);
    },
    
    getSelectNoneButton : function(){
      return this.camel(this.SELECT + this.NONE + this.BUTTON);
    },
    
    getSelectedCount : function(){
      return this.camel(this.SELECTED + this.COUNT);
    },
    
    getSelectedPane : function(){
      return this.camel(this.SELECTED + this.PANE);
    },
    
    getSelectionPane : function(){
      return this.camel(this.SELECTION + this.PANE);
    },
    
    getServerPortContainerUpper : function(){
      return this.PORT + this.CONTAINER;
    },
    
    getServerPortIcon : function(){
      return this.camel(this.PORT + this.ICON);
    },
    
    getServerPortUpper : function(){
      return this.SERVER + this.PORT;
    },
    
    getServletStatsUpper : function(){
      return this.SERVLET + this.STATS;
    },
    
    getSideTabUpper : function(){
      return this.SIDE + this.TAB;
    },
    
    getSideTabConfigButtonUpper : function(){
      return this.SIDE + this.TAB + this.CONFIG + this.BUTTON;
    },
    
    getSideTabOverviewButtonUpper : function(){
      return this.getSideTabUpper() + this.OVERVIEW + this.BUTTON;
    },
    
    getSideTabStatsButtonUpper : function(){
      return this.getSideTabUpper() + this.STATS + this.BUTTON;
    },
    
    getSideTabPaneUpper : function(){
      return this.getSideTabUpper() + this.PANE;
    },

    getStackContainerUpper : function(){
      return this.STACK + this.CONTAINER;
    },
    
    getStartButton : function(){
      return this.camel(this.START + this.BUTTON);
    },
    
    getStartTimeDisplay : function(){
      return this.camel(this.START + this.TIME + this.DISPLAY);
    },
    
    getStartTimeSlider : function(){
      return this.camel(this.START + this.TIME + this.SLIDER);
    },
    
    getState : function(){
      return this.camel(this.STATE);
    },
    
    getStateButton : function(){
      return this.camel(this.STATE + this.BUTTON);
    },
    
    getStateIcon : function(){
      return this.camel(this.STATE + this.ICON);
    },

    getStateLabel : function(){
      return this.camel(this.STATE + this.LABEL);
    },
    
    getStateLabelUpper : function(){
      return this.STATE + this.LABEL;
    },
    
    getStatePaneUpper : function(){
      return this.STATE + this.PANE;
    },
    
    getStateNumberUpper : function(){
      return this.STATE + this.NUMBER;
    },
    
    getStatsUpper : function(){
      return this.STATS;
    },
    
    getStopButton : function(){
      return this.camel(this.STOP + this.BUTTON);
    },
    
    getSubLabelStatePaneUpper : function(){
      return this.SUB + this.LABEL + this.STATE + this.PANE;
    },
    
    getSummary : function(){
      return this.camel(this.SUMMARY);
    },
    
    getSummaryDisplay : function(){
      return this.camel(this.SUMMARY + this.DISPLAY);
    },
    
    getSummaryPaneUpper : function(){
      return this.SUMMARY + this.PANE;
    },
    
    getTextBox : function(){
      return this.camel(this.TEXT + this.BOX);
    },
    
    getTimeSelectorUpper : function(){
      return this.TIME + this.SELECTOR;
    },
    
    getTimeSliderView : function(){
      return this.camel(this.TIME + this.SLIDER + this.VIEW);
    },
    
    getTitlePaneUpper : function(){
      return this.TITLE + this.PANE;
    },
    
    getTooltipDialogUpper : function(){
      return this.TOOLTIP + this.DIALOG;
    },
    
    getType : function(){
      return this.camel(this.TYPE);
    },
    
    getOverviewPaneUpper : function(){
      return this.OVERVIEW + this.PANE;
    },
    
    getUnsavedConfigYesNoDialog : function(){
      return this.camel(this.UNSAVED + this.CONFIG + this.YES + this.NO + this.DIALOG);
    },
    
    //DESCRIPTIONS

    getAll : function(){
      return this.camel(this.ALL);
    },
    
    getAllUpper : function(){
      return this.ALL;
    },
    
    getAllTagContainerUpper : function(){
      return this.ALL + this.TAG + this.CONTAINER;
    },
    
    getActionButtonTooltipDialogUpper : function(){
      return this.ACTION + this.BUTTON + this.TOOLTIP + this.DIALOG;
    },

    getAlert : function(){
      return this.camel(this.ALERT);
    },
    
    getAlertCp : function(){
      return this.camel(this.ALERT + this.CP);
    },
    
    getAlertUpper : function(){
      return this.ALERT;
    },

    getAlerts : function(){
      return this.camel(this.ALERTS);
    },
    
    getMmAlertIcon : function(){
      return this.camel(this.MM + this.ALERT + this.ICON);
    },
    
    getAttributeContactsDiv : function(){
      return this.camel(this.ATTRIBUTE + this.CONTACTS + this.DIV);
    },
    
    getAttributeNotesDiv : function(){
      return this.camel(this.ATTRIBUTE + this.NOTES + this.DIV);
    },
    
    getAttributeOwnerDiv : function(){
      return this.camel(this.ATTRIBUTE + this.OWNER + this.DIV);
    },
    
    getAttributeTagsDiv : function(){
      return this.camel(this.ATTRIBUTE + this.TAGS + this.DIV);
    },
    
    getClassesStatsUpper : function(){
      return this.CLASSES + this.STATS;
    },
    
    getClose : function(){
      return this.camel(this.CLOSE);
    },
    
    getCountUpper : function(){
      return this.COUNT;
    },
    
    getDiv : function(){
      return this.camel(this.DIV);
    },
    
    getDropDownPositionAboveButton : function(){
      return this.camel(this.DROPDOWN + this.POSITION + this.ABOVE + this.BUTTON);
    },
    
    getExplore : function(){
      return this.camel(this.EXPLORE);
    },
    
    getFadingOverlay : function(){
      return this.camel(this.FADING = this.OVERLAY);
    },
    
    getHeapStatsUpper : function(){
      return this.HEAP + this.STATS;
    },
    
    getImgUpper : function() {
      return this.IMG;
    },
    
    getLabelCp : function(){
      return this.camel(this.LABEL + this.CP);
    },
    
    getLabelUpper : function(){
      return this.LABEL;
    },
    
    getLeft : function(){
      return this.camel(this.LEFT);
    },
    
    getLiveMsgNode : function(){
      return this.camel(this.LIVE + this.MSG + this.NODE);
    },
    
    getLiveMsgDetailNode : function(){
      return this.camel(this.LIVE + this.MSG + this.DETAIL + this.NODE);
    },
    
    getLiveUpdatingView : function(){
      return this.camel(this.LIVE + this.UPDATING + this.VIEW);
    },
    
    getManagedConnectionCountConnectionStatsUpper : function(){
      return this.MANAGED + this.CONNECTION + this.COUNT + this.CONNECTION + this.STATS;
    },
    
    getMessageDiv : function(){
      return this.camel(this.MESSAGE + this.DIV);
    },
    
    getMoreActionDropDownMenu : function(){
      return this.camel(this.MORE + this.ACTION + this.DROPDOWN + this.MENU);
    },

    getName : function(){
      return this.camel(this.NAME);
    },
    
    getRequestCountServletStatsUpper : function(){
      return this.REQUEST + this.COUNT + this.SERVLET + this.STATS;
    },
    
    getResponseMeanServletStatsUpper : function(){
      return this.RESPONSE + this.MEAN + this.SERVLET + this.STATS;
    },
    
    getSearchBy : function(){
      return this.camel(this.SEARCH + this.BY);
    },
    
    getSessionStatsUpper : function(){
      return this.SESSION + this.STATS;
    },
    
    getSingleLegend : function(){
      return this.camel(this.SINGLE + this.LEGEND);
    },
    
    getStatus : function(){
      return this.camel(this.STATUS);
    },
    
    getTagAttrsPane : function(){
      return this.camel(this.TAG + this.ATTRS + this.PANE);
    },
    
    getTagPaneUpper : function(){
      return this.TAG + this.PANE;
    },
    
    getTagContainerUpper : function(){
      return this.TAG + this.CONTAINER;
    },
    
    getTag : function(){
      return this.camel(this.TAG);
    },
    
    getTagUpper : function(){
      return this.TAG;
    },
    
    getTags : function(){
      return this.camel(this.TAGS);
    },    
    
    getTagsIcon : function(){
      return this.camel(this.TAGS + this.ICON);
    },
    
    getText : function(){
      return this.camel(this.TEXT);
    },
    
    getThreadPoolStatsUpper : function(){
      return this.THREAD + this.POOL + this.STATS;
    },
    
    getThreadStatsUpper : function(){
      return this.THREAD + this.STATS;
    },
    
    getTitle : function(){
      return this.camel(this.TITLE);
    },
    
    getTitleUpper : function(){
      return this.TITLE;
    },

    getTotal : function(){
      return this.camel(this.TOTAL);
    },
    
    getTotalUpper : function(){
      return this.TOTAL;
    },
    
    getUserDir : function(){
      return this.camel(this.USERDIR);
    },

    getUserDirNoCaps : function() {
      return this.USERDIR.toLowerCase();
    },
    
    getUserDirWidth : function(){
      return this.camel(this.USERDIR + this.WIDTH);
    },
    
    getValue : function(){
      return this.camel(this.VALUE);
    },
    
    getWaitTimeConnectionStatsUpper : function(){
      return this.WAIT + this.TIME + this.CONNECTION + this.STATS;
    },
    
    //STATES
    
    getActionDeployButton : function(){
      return this.camel(this.ACTION + this.DEPLOY + this.BUTTON);
    },

    getActionRestartButton : function(){
      return this.camel(this.ACTION + this.RESTART + this.BUTTON);
    },
    
    getActionStartButton : function(){
      return this.camel(this.ACTION + this.START + this.BUTTON);
    },
    
    getActionStopButton : function(){
      return this.camel(this.ACTION + this.STOP + this.BUTTON);
    },
    
    getFailed : function(){
        return this.camel(this.FAILED);
    },
    
    getPARTIALLYSTARTED : function(){
      return this.PARTIALLY_STARTED_CAPS;
    },
    
    getRestartAction : function(){
      return this.camel(this.RESTART + this.ACTION);
    },
    
    getRunningUpper : function(){
      return this.RUNNING;
    },
    
    getSTARTED : function(){
      return this.STARTED_CAPS;
    },
    
    getStartedUpper : function(){
      return this.STARTED;
    },
    
    getStartAction : function(){
      return this.camel(this.START + this.ACTION);
    },
    
    getStartCleanAction: function(){
      return this.camel(this.START + this.CLEAN + this.ACTION);
    },
    
    getSTOPPED : function(){
      return this.STOPPED_CAPS;
    },
    
    getStoppedUpper : function(){
      return this.STOPPED;
    },
    
    getStopAction : function(){
      return this.camel(this.STOP + this.ACTION);
    },

    getUNKNOWN : function(){
      return this.UNKNOWN_CAPS;
    },
    
    getUnknownUpper : function(){
      return this.UNKNOWN;
    },
    
    //CONFIRMATION
    
    getConfirmationButton : function(){
      return this.camel(this.CONFIRMATION + this.BUTTON);
    },
    
    getConfirmationPane : function(){
      return this.camel(this.CONFIRMATION + this.PANE);
    },
    
    getConfirmationRestartPanel : function(){
      return this.camel(this.CONFIRMATION + this.RESTART + this.PANEL);
    },
    
    getConfirmationStartPanel : function(){
      return this.camel(this.CONFIRMATION + this.START + this.PANEL);
    },
    
    getConfirmationStopPanel : function(){
      return this.camel(this.CONFIRMATION + this.STOP + this.PANEL);
    },
    
    //UPDATES
   
    getConnectionToServerFailed : function(){
      this.dashDelimit(this.CONNECTION, this.TO, this.SERVER, this.FAILED);
    },

    getRemovedResourceErrorUpper : function(){
      return this.REMOVED + this.RESOURCE + this.ERROR;
    },
    
    //OTHER
    
    getCheckMark : function(){
      return this.camel(this.CHECKMARK);
    },
    
    getDisplayHideLegendUpper : function(){
      return this.DISPLAY + this.HIDE + this.LEGEND;
    },
    
    getNotesAttrsPaneDiv: function(){
      return this.camel(this.NOTES + this.ATTRS + this.PANE + this.DIV);
    },
    
    getNotesAttrsPane : function(){
      return this.camel(this.NOTES + this.ATTRS + this.PANE);
    },
    
    getNotesIcon : function(){
      return this.camel(this.NOTES + this.ICON);
    },
    
    getSaveButton : function(){
      return this.camel(this.SAVE + this.BUTTON);
    },
    
    getSaveCancelBar : function(){
      return this.camel(this.SAVE + this.CANCEL + this.BAR);
    },
    
    getSaveCancelButtonArea : function(){
      return this.camel(this.SAVE + this.CANCEL + this.BUTTON + this.AREA);
    },
    
    getServerStatsSelectorUpper : function(){
      return this.SERVER + this.STATS + this.SELECTOR;
    },
    
    getSetAttrButton : function(){
      return this.camel(this.SET + this.ATTR + this.BUTTON);
    },
    
    getSetAttrResourceName : function(){
      return this.camel(this.SET + this.ATTR + this.RESOURCE + this.NAME);
    },
    
    getSetAttrsDlg : function(){
      return this.camel(this.SET + this.ATTRS + this.DLG);
    },
    
    getSetAttrsDlgErrMsg : function(){
      return this.camel(this.SET + this.ATTRS + this.DLG + this.ERR + this.MSG);
    },
    
    getSetAttrsDlgMsg : function(){
      return this.camel(this.SET + this.ATTRS + this.DLG + this.MSG);
    },
    
    getSetAttributesDialogUpper : function(){
      return this.SET + this.ATTRIBUTES + this.DIALOG;
    },
    
    getShowGraphsDialog : function(){
      return this.camel(this.SHOW + this.GRAPHS + this.DIALOG);
    },
    
    getShowGraphsDialogId : function(){
      return this.camel(this.SHOW + this.GRAPHS + this.DIALOG + this.ID);
    },
    
    getShowHideGraphsButton : function(){
      return this.camel(this.SHOW + this.HIDE + this.GRAPHS + this.BUTTON);
    },
    
    getShowHideGraphsButtonPane : function(){
      return this.camel(this.SHOW + this.HIDE + this.GRAPHS + this.BUTTON + this.PANE);
    },

    getSvg : function(){
      return this.camel(this.SVG);
    },
    
    getSvgG : function(){
      return this.camel(this.SVGG);
    },
    
    getLegendsvg : function(){
      return this.camel(this.LEGENDSVG);
    },
    
    getLegendNode : function(){
      return this.camel(this.LEGEND + this.NODE);
    },
    
    getWontRestartACServer : function(){
      return this.camel(this.WONT + this.RESTART + this.AC_CAPS + this.SERVER);
    },
    
    getAskEnableDisableMaintenanceMode : function(){
      return this.camel(this.ASK + this.ENABLE + this.DISABLE + this.MAINTENANCE + this.MODE);
    },
    
    getAskForceDialog : function(){
      return this.camel(this.ASK + this.FORCE + this.DIALOG);
    },
    
    getAskStopACServer : function(){
      return this.camel(this.ASK + this.STOP + this.AC_CAPS + this.SERVER);
    },
    
    tbd : function(){
      return "toBeSet";
    },
    
    setMe : function(){
      return "setMe";
    },
        
    //LINKERS
    
    commaDelimit : function(){
      var args = [];
      for (var i = 0; i < arguments.length; ++i){
        args[i] = arguments[i];
      }
      return args.join();
    },
    
    dashDelimit : function(){
      var args = [];
      for (var i = 0; i < arguments.length; ++i){
        args[i] = arguments[i];
      }
      return args.join(this.DASH);
    },
    
    getDash : function(){
      return this.DASH;
    },
    
    getComma : function(){
      return ",";
    },
    
    getUnderscore : function(){
      return this.UNDERSCORE;
    },
    
    underscoreDelimit : function(){
      var args = [];
      for (var i = 0; i < arguments.length; ++i){
        args[i] = arguments[i];
      }
      return args.join(this.UNDERSCORE);
    },
    
    dumbDelimit : function(arg1, arg2, arg3){
      return arg1 + this.UNDERSCORE + arg2 + this.DASH + arg3;
    },
    
    //COUNTS & CONTENTS
    
    getAccessLogCount : function(){
      return this.camel(this.ACCESS + this.LOG + this.COUNT);
    },
    
    getAccessLogContent : function(){
      return this.camel(this.ACCESS + this.LOG + this.CONTENT);
    },
    
    getAccessLogGraphUpper : function(){
      return this.ACCESS + this.LOG + this.GRAPH;
    },
    
    getAccessLogSummaryUpper : function(){
      return this.ACCESS + this.LOG + this.SUMMARY;
    },
    
    getLogMessageCount : function(){
      return this.camel(this.LOG + this.MESSAGE + this.COUNT);
    },
    
    getLogMessageContent : function(){
      return this.camel(this.LOG + this.MESSAGE + this.CONTENT);
    },
    
    getFfdcCount : function(){
      return this.camel(this.FFDC + this.COUNT);
    },
    
    getFfdcContent : function(){
      return this.camel(this.FFDC + this.CONTENT);
    },
    
    getFFDCGraph : function(){
      return this.FFDC_CAPS + this.GRAPH;
    },

    getFFDCTable : function(){
      return this.FFDC_CAPS + this.Table;
    },
    
    getTraceCount : function(){
      return this.camel(this.TRACE + this.COUNT);
    },
    
    getTraceContent : function(){
      return this.camel(this.TRACE + this.CONTENT);
    },
    
    getTraceLogGraphUpper : function(){
      return this.TRACE + this.LOG + this.GRAPH;
    },
    
    getTraceLogTableUpper : function(){
      return this.TRACE + this.LOG + this.TABLE;
    },
    
    //NUMBERS & TIMES
    
    get1y : function(){
      return this.ONE + this.Y;
    },
    
    get1mo : function(){
      return this.ONE + this.MO;
    },
    
    get1w : function(){
      return this.ONE + this.W;
    },
    
    get1d : function(){
      return this.ONE + this.D;
    },
    
    get1h : function(){
      return this.ONE + this.H;
    },
    
    get10m : function(){
      return this.TEN + this.M;
    },
    
    get5m : function(){
      return this.FIVE + this.M;
    },

    get1m : function(){
      return this.ONE + this.M;
    },
    
    
    
    getShowHideButton : function(){
      return this.camel(this.SHOW + this.HIDE + this.BUTTON);
    },
    
    getJvmParent : function(){
      return this.camel(this.JVM + this.PARENT);
    },
    
    getJvmHeading : function(){
      return this.camel(this.JVM + this.HEADING);
    },
    
    getAddAllJVMGraphButton : function(){
      return this.camel(this.ADD + this.ALL + this.JVM_CAPS + this.GRAPH + this.BUTTON);
    },
    
    getAddMemoryGraphButton : function(){
      return this.camel(this.ADD + this.MEMORY + this.GRAPH + this.BUTTON);
    },
    
    getAddClassesGraphButton : function(){
      return this.camel(this.ADD + this.CLASSES + this.GRAPH + this.BUTTON);
    },
    
    getAddThreadsGraphButton : function(){
      return this.camel(this.ADD + this.THREADS + this.GRAPH + this.BUTTON);
    },
    
    getAddCPUGraphButton : function(){
      return this.camel(this.ADD + this.CPU + this.GRAPH + this.BUTTON);
    },
    
    getThreadPoolParent : function(){
      return this.camel(this.THREAD + this.POOL + this.PARENT);
    },
    
    getAddAllThreadPoolGraphButton : function(){
      return this.camel(this.ADD + this.ALL + this.THREAD + this.POOL + this.GRAPH + this.BUTTON);
    },
    
    getAddActiveThreadsGraphButton : function(){
      return this.camel(this.ADD + this.ACTIVE  + this.THREADS + this.GRAPH + this.BUTTON);
    },
    
    getConnPoolParent : function(){
      return this.camel(this.CONN + this.POOL + this.PARENT);
    },
    
    getAddAllConnGraphButton : function(){
      return this.camel(this.ADD + this.ALL + this.CONN + this.GRAPH + this.BUTTON);
    },
    
    getAddUsedConnGraphButton : function(){
      return this.camel(this.ADD + this.USED + this.CONN + this.GRAPH + this.BUTTON);
    },
    
    getAddAvgWaitGraphButton : function(){
      return this.camel(this.ADD + this.AVG + this.WAIT + this.GRAPH + this.BUTTON);
    },
    
    getSessionMgmtParent : function(){
      return this.camel(this.SESSION + this.MGMT_CAPS + this.PARENT);
    },
    
    getAddAllSessionMgmtGraphButton : function(){
      return this.camel(this.ADD + this.ALL + this.SESSION + this.MGMT_CAPS + this.GRAPH + this.BUTTON);
    },
    
    getAddActiveSessionsGraphButton : function(){
      return this.camel(this.ADD + this.ACTIVE + this.SESSIONS + this.GRAPH + this.BUTTON);
    },
    
    getAccessLogParent : function(){
      return this.camel(this.ACCESS + this.LOG + this.PARENT);
    },
    
    getAddAllAccessLogGraphButton : function(){
      return this.camel(this.ADD + this.ALL + this.ACCESS + this.LOG + this.GRAPH + this.BUTTON);
    },
    
    getAccessLogGraphOption : function(){
      return this.camel(this.ACCESS + this.LOG + this.GRAPH + this.OPTION);
    },
    
    getAddAccessLogGraphGraphButton : function(){
      return this.camel(this.ADD + this.ACCESS + this.LOG + this.GRAPH + this.GRAPH + this.BUTTON);
    },
    
    getAccessLogSummaryOption : function(){
      return this.camel(this.ACCESS + this.LOG + this.SUMMARY + this.OPTION);
    },
    
    getAddAccessLogSummaryGraphButton : function(){
      return this.camel(this.ADD + this.ACCESS + this.LOG + this.SUMMARY + this.GRAPH + this.BUTTON);
    },
    
    getMessagesParent : function(){
      return this.camel(this.MESSAGES + this.PARENT);
    },
    
    getAddAllMessagesGraphButton : function(){
      return this.camel(this.ADD + this.ALL + this.MESSAGES + this.GRAPH + this.BUTTON);
    },
    
    getMessagesGraphOption : function(){
      return this.camel(this.MESSAGES + this.GRAPH + this.OPTION);
    },
    
    getAddMessagesGraphGraphButton : function(){
      return this.camel(this.ADD + this.MESSAGES + this.GRAPH + this.GRAPH + this.BUTTON);
    },
    
    getMessagesTableOption : function(){
      return this.camel(this.MESSAGES + this.TABLE + this.OPTION);
    },
    
    getAddMessagesTableGraphButton : function(){
      return this.camel(this.ADD + this.MESSAGES + this.TABLE + this.GRAPH + this.BUTTON);
    },
    
    getFFDCGraphOption : function(){
      return this.camel(this.FFDC_CAPS + this.GRAPH + this.OPTION);
    },
    
    getAddFFDCGraphGraphButton : function(){
      return this.camel(this.ADD + this.FFDC_CAPS + this.GRAPH + this.GRAPH + this.BUTTON);
    },
    
    getFFDCTableOption : function(){
      return this.camel(this.FFDC_CAPS + this.TABLE + this.OPTION);
    },
    
    getAddFFDCTableGraphButton : function(){
      return this.camel(this.ADD + this.FFDC_CAPS + this.TABLE + this.GRAPH + this.BUTTON);
    },
    
    getTraceLogGraphOption : function(){
      return this.camel(this.TRACE + this.LOC + this.GRAPH + this.OPTION);
    },
    
    getAddTraceLogGraphGraphButton : function(){
      return this.camel(this.ADD + this.TRACE + this.LOG + this.GRAPH + this.GRAPH + this.BUTTON);
    },
    
    getTraceLogTableOption : function(){
      return this.camel(this.TRACE + this.LOG + this.TABLE + this.OPTION);
    },
    
    getAddTraceLogTableGraphButton : function(){
      return this.camel(this.ADD + this.TRACE + this.LOG + this.TABLE + this.GRAPH + this.BUTTON);
    },
    
    getWcParent : function(){
      return this.camel(this.WC + this.PARENT);
    },
    
    getAddAllWebContainerGraphButton : function(){
      return this.camel(this.ADD + this.ALL + this.WEB + this.CONTAINER + this.GRAPH + this.BUTTON);
    },
    
    getAddAvgRespGraphButton : function(){
      return this.camel(this.ADD + this.AVG + this.RESP + this.GRAPH + this.BUTTON);
    },
    
    getAddReqCountGraphButton : function(){
      return this.camel(this.ADD + this.REQ + this.COUNT + this.GRAPH + this.BUTTON);
    },
    
    getJaxwsParent : function(){
      return this.camel(this.JAXWS + this.PARENT);
    },
    
    getRecentSearchHistoryBanner : function() {
      return "recentSearchHistoryBanner";
    }
    
};
ID = lang.mixin(ID, sharedID); //overwrites the duplicate members from the second object

return ID;
});