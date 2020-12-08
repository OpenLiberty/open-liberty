define(["dojo/parser",
        "dojo/query",
        "dojo/i18n!js/toolbox/nls/toolboxMessages",
        "dojo/i18n!js/widgets/nls/widgetsMessages", 
        "dojox/mobile/compat",
        "dojo/_base/kernel",
        "dojox/mobile/deviceTheme",
        "dojox/mobile/ScrollableView",
        "dojo/ready",
        "dijit/registry",
        "js/toolbox/addBookmark",
        "dijit/Dialog",
        "js/common/platform",
        "js/widgets/LibertyHeader",
        "js/widgets/LibertyToolbox",
        "js/widgets/TextBox",
        "dojo/NodeList-manipulate",
        "dojo/domReady!"],
        function(Parser,
            query,
            i18n,
            i18nL,
            mCompat, // not used in code, but tools show up horizontal when this and the following two strings are deleted
            kernel,
            mDeviceTheme,
            mScrollableView,
            ready,
            registry,
            addBookmark,
            Dialog,
            platform) {
  'use strict';

  /**
   * Check if the element is visible to the user
   * @param {*} element 
   * @return true if element is visible, false otherwise
   */
  function isVisible(element) {
    if(! element) {
      return false;
    }
    return element.offsetParent !== null;
  }

  /**
   * Returns the first element that is visible
   * @param {Array} manyElements 
   * @return element that is visible, otherwise null
   */
  function getFirstVisibleElement(manyElements) {
    if(! manyElements || manyElements.length === 0) {
      return null;
    }

    for(var x = 0; x < manyElements.length; x++) {
      var element = manyElements[x];
      if(isVisible(element)) {
        return element;
      }
    }
    return null;
  }

  /**
   * Put focus on the first visible element in an array of elements that could
   * be visible or not visible.
   * @param {Array} manyElements - a list of elements
   */
  function focusFirstVisibleElement(manyElements) {
    var firstVisibleElement = getFirstVisibleElement(manyElements);
    if(firstVisibleElement) {
      firstVisibleElement.focus();
    }
  }

  function handlejQueryTool() {
    var toolMain = document.getElementById("toolIFrame").contentWindow.document;

    var allButtons = toolMain.querySelectorAll('[role="button"]');
    if (allButtons.length > 0) {
      focusFirstVisibleElement(allButtons);
    } else {
      var errorContainer = toolMain.getElementById("toolErrorFlexContainer");
      if (errorContainer) {
        var errText = toolMain.getElementById("errorText");
        errText.focus();
      }
    }

  }

  function handleJavaBatch() {
    var toolMain = document.getElementById("toolIFrame").contentWindow.document;

    // Need to handle views:
    // 1.  Dashboard view
    // 2.  Execution details view
    // 3.  Job logs view
    // 4.  Loading page view

    var dashboardView = toolMain.getElementById("dashboardView"); // only one element
    var executionDetailsView = toolMain.getElementById("execution-detailsPane"); // only one element
    var jogLogsView = toolMain.getElementById("jobLogs-Page"); // only one element
    var loadingView = toolMain.getElementById("loadingView"); // only one element

    if(isVisible(dashboardView)) {
      var first;
      var searchPills = dashboardView.getElementsByClassName("searchPill");
      if(searchPills.length > 0) {
        first = searchPills[0]; // Pick top left search pill
      } else {
        // No search pills, put focus on the input box
        first = toolMain.getElementById("search-text-box");
      }
      first.focus();

    } else if(isVisible(executionDetailsView)) {
      var f = toolMain.getElementsByClassName("executionIdentitySection");
      f[0].focus(); // Only one element expected

    } else if(isVisible(jogLogsView)) {
      var logs = toolMain.getElementById("jobLogs-Page-contentPane");
      logs.firstChild.focus();

    } else if(isVisible(loadingView)) {
      console.log("Nothing to focus on in loading view.");
    } else {
      // This should never happen!
      console.error('Unknown java batch tool page.');
      console.error('Unable to determine how to skip to the content of this page.');
    }
  }

  function handleBackgroundTasks() {
    var backgroundTaskView = document.getElementById("bgTasksContainer");
    if(isVisible(backgroundTaskView)) {
      var lines = backgroundTaskView.getElementsByClassName("clickableLine");
      if(lines.length > 0) {
        // focus first background task
        focusFirstVisibleElement(lines);
      }
    }
  }

  function handleToolbox() {
  
    // Need to handle views:
    // 1.  Main toolbox view
    // 2.  Preference view
    // 3.  Background view
    
    var toolMain = document.getElementById("toolIconContainer");
    if(isVisible(toolMain)) {
      var tools = toolMain.querySelectorAll('[role="button"]');
      // place focus first admin center tool
      focusFirstVisibleElement(tools);
      return;
    }

    var preferenceView = document.getElementById("prefsContentContainer");
    var isPreferenceView = isVisible(preferenceView);
    if(isPreferenceView) {
      var prefView = document.getElementById("prefsContentContainer");
      var prefViewButtons = prefView.querySelectorAll('[role="button"]');
      // place focus on first button in the preference view
      focusFirstVisibleElement(prefViewButtons);
      return;
    }
  }

  function handleExplore() {
    var toolMain = document.getElementById("toolIFrame").contentWindow.document;

    // Need to handle views:
    //  1.  Dashboard
    //  2.  Object view
    //  3.  Search view
    //  4.  Collection view

    // *** Note:
    // Explore tool can have many objectView and collectionView specific to the content of the page.
    // Explore also caches previous object and collection views for performance, so there can be multiple
    // objectView and collectionView in the DOM.  This means the query has to be a "id that starts with *View-". 
    // Search view is reused and unique so we can just search for the searchView id
     
    var dashboard = toolMain.getElementById("mainDashboard");
    var allObjectView = toolMain.querySelectorAll('[id^="objectView-"]'); // can have many object views
    var allCollectionView = toolMain.querySelectorAll('[id^="collectionView-"]'); // can have many collection views
    var searchView = toolMain.getElementById("searchView"); // only one search view

    // We have to check if the view is visible since we cache recently visited pages
    // by hiding them in the DOM
    var isObjectView = getFirstVisibleElement(allObjectView);
    var isCollectionView = getFirstVisibleElement(allCollectionView);

    if(isVisible(dashboard)) {
      // Viewing dashboard
      var dashboardButtons = dashboard.querySelectorAll('[type="button"]');
      // Place focus on the first explore dashboard button
      focusFirstVisibleElement(dashboardButtons);

    } else if(isVisible(searchView)) {
      // Viewing search
      toolMain.getElementById("search-text-box").focus();

    } else if(isVisible(isObjectView)) {
      // Viewing a single resource
      // There can be 1 MainContentPane and many ContentPane in the DOM, but only one is visible
      var allContentPanes = toolMain.querySelectorAll('[id$="ContentPane"]');
      var visibleContentPane = getFirstVisibleElement(allContentPanes);

      if(! visibleContentPane) {
        return;
      }

      if(visibleContentPane.firstChild && visibleContentPane.firstChild.nodeName === "IFRAME") {
        // We are viewing server config embedded in explore tool
        visibleContentPane = visibleContentPane.firstChild.contentDocument.body;
      }

      var buttons = visibleContentPane.querySelectorAll('[role="button"]');
      focusFirstVisibleElement(buttons);

    } else if(isVisible(isCollectionView)) {
      // Viewing a collection of resources
      var mainContentPane = toolMain.querySelector('[id^="collectionView-"]');
      var collectionViewButtons = mainContentPane.querySelectorAll('[role="button"]');
      focusFirstVisibleElement(collectionViewButtons);
    } else {
      // This should never happen!
      console.error('Unknown explore tool page.  Unable to determine how to skip to the content of this page.');
    }
  }

  function skipToContent(e) {
    var currentAdminCenterTool = window.location.hash;
    if(currentAdminCenterTool === "") {
      handleToolbox();
    } else if(currentAdminCenterTool.indexOf("#backgroundTasks") > -1 ) {
      handleBackgroundTasks();
    } else if(currentAdminCenterTool.indexOf("#javaBatch") > -1 ) {
      handleJavaBatch();
    } else if(currentAdminCenterTool.indexOf("#explore") > -1) {
      handleExplore();
    } else {
      // Deploy and Server Config
      handlejQueryTool();
    }
    // Prevent the default <a> behavior
    e.preventDefault();
  }

  function handleSkipToContentButton() {
    // Add custom logic to the "skip to content" button
    var skipToContentButton = document.getElementsByClassName("skipToContent"); // Should ALWAYS exist

    // There are multiple instances of the Toolbar.  This means there are multiple skip to content buttons!
    // Attach the onclick logic to all skip to content button instances
    for(var k = 0; k < skipToContentButton.length; k++) {
      skipToContentButton[k].title = i18n.TOOLBOX_SKIP_TO_CONTENT;
      skipToContentButton[k].text = i18n.TOOLBOX_SKIP_TO_CONTENT;
      skipToContentButton[k].setAttribute("aria-label", i18n.TOOLBOX_SKIP_TO_CONTENT);
      skipToContentButton[k].onclick = skipToContent;
    }
  }

  return {

    initialize: function() {
      Parser.parse(); // handle declarative widgets

      // set translated tab text - Admin Center
      document.getElementById("toolbox_tab_title").innerHTML = i18nL.LIBERTY_HEADER_TITLE;

      // set lang on the <html> tag
      console.log("locale:" + kernel.locale);
      document.documentElement.setAttribute("lang", kernel.locale);

      ready(function(){

        if (platform.isDesktop()) {
          query("head").append('<link rel="stylesheet" type="text/css" href="css/desktop.css">');
        }
        
        // deal with widgets here

        var addBookmarkDialog = new Dialog({
          title: i18n.TOOLBOX_ADDURL_TITLE,
          content: addBookmark()
        }, "addBookmarkDialogId");
        addBookmarkDialog.onCancel = function(){
          registry.byId('addBookmarkDialogId').hide();
          registry.byId("bookmarkName").reset();
          registry.byId("bookmarkURL").reset();
        };

        handleSkipToContentButton();

      });
    }
  };
});
