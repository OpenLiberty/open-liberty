/**
 * The toolLauncher has the logic to know how to open a tool.
 * 
 * A tool can be opened from a particular view. Which view is important to be known so that the visual transition to the tool can be done
 * properly.
 */
//dojo and dijit are global!
define([ 'dojo/i18n!js/widgets/nls/widgetsMessages', 'dijit/registry', 'dojo/dom', 'dojo/query' ], function(i18n, registry, dom, query) {
  'use strict';

  /**
   * Sets the tool header to the appropriate value for the opened tool.
   * 
   * @param from
   *          The view from which the tool was opened
   * @param tool
   *          The tool which was opened
   */
  function setToolboxHeader(from, tool) {
    var tool_headerWidget = registry.byId('tool_headerWidget');
    tool_headerWidget.createToolHeading(tool.label, 'toolContainer', from);
    tool_headerWidget.set('secondaryTitle', tool.label);

    // set the title to be displayed in hover over
    var secondaryTitle_container = query('#tool_headerWidget .idxHeaderSecondaryTitle')[0];
    secondaryTitle_container.setAttribute("title", tool.label);

    // store the hashId for home button to use if the returnToViewId is toolContainer
    tool_headerWidget.set('hashId', tool.hashId);
  }

  /**
   * Opens the iframe which will contain the tool. If we're in devAdminCenter, open the dev version of the tool.
   * 
   * @param from
   *          The view from which the tool was opened
   * @param tool
   *          The tool which was opened
   * @param noAnimation
   *          Indicates whether or not the transition should be animated. Opening a tool directly via a URL is an example of where animation
   *          is not desired.
   */
  function openIFrame(from, tool, noAnimation) {
    var toolUrl = tool.url;
    console.debug(toolUrl);
    if ((toolUrl.indexOf("/oidc/endpoint/") === -1) && (window.location.pathname.indexOf('/devAdminCenter/') > -1)) {
      // if this is devAdminCenter, open the devTool
      var toolName = toolUrl.substring(toolUrl.lastIndexOf('/') + 1);
      toolUrl = '/dev' + toolName.charAt(0).toUpperCase() + toolName.substring(1, toolName.indexOf('-'));
    }
    console.info('Opening tool ' + tool.label + ' via URL: ', toolUrl);
    if (!window.globalIsAdmin) {	
      if (tool.id.indexOf("com.ibm.websphere.appserver.adminCenter.tool.deploy") >= 0) {
        // set toolUrl to empty so that it doesn't trigger the GET call to deploy
        toolUrl = "";
      }
    }
    dom.byId('toolContentContainer').innerHTML = '<iframe id="toolIFrame" src="' + toolUrl + 
        '" height="100%" width="100%" style="border: 0" title="' + i18n.TOOL_FRAME_TITLE + '"></iframe>';
    registry.byId(from).performTransition('toolContainer', 1, (noAnimation ? 'none' : 'slide'));

    if (toolUrl.indexOf("/oidc/endpoint/") >= 0) {
      oidcIframeOnload();
    }

    if (tool.id.indexOf("com.ibm.websphere.appserver.adminCenter.tool.deploy") >= 0) {
      deployIframeOnload();
	}
  }

  /**
   * Remap the non-translated error returned from the oidc rest API to a translated message if the error code is access_denied. 
   * Otherwise, return a generic translated message.
   * 
   * Note: this call may not be needed once security team translates their messages.
   */
  function oidcIframeOnload() {
    var toolIframe = document.getElementById('toolIFrame');
    var errorDesc;
    toolIframe.onload = function () {
      var contentDocument = toolIframe.contentDocument || toolIframe.contentWindow.document;
      if (contentDocument) {
        var contentBody = contentDocument.body;
        if (contentBody) {
          if (contentBody.innerText.indexOf('"error"') !== -1 || contentBody.innerText.indexOf('"error_description"') !== -1) {
            var jsonErrorBody = JSON.parse(contentBody.innerText);
            if (jsonErrorBody.error === "access_denied") {
              errorDesc = i18n.TOOL_OIDC_ACCESS_DENIED;
            } else {
              errorDesc = i18n.TOOL_OIDC_GENERIC_ERROR;
            }
            // Note: The current src is still pointing to oidc/endpoint/<provider>. Has to change the url back to adminCenter path.
            toolIframe.src = "../../../adminCenter/html/toolError.html";
            // listen to onload of the changed src to set the error text
            document.getElementById('toolIFrame').onload = function (event) {
              // add the error message
              var iframeObject = event.target;
              contentDocument = iframeObject.contentDocument || toolIframe.contentWindow.document;
              var errorTextElement = contentDocument.getElementById('errorText');
              if (errorDesc) {
                errorTextElement.innerText = errorDesc;
              }
            };
          }
        }
      }
    };
  }
  
  /**
   * Display an error message for access_denied if the user does not have Admin role when trying to access deploy tool
   */
  function deployIframeOnload() {
	// display error message if the user is not in admin role
	if (!window.globalIsAdmin) {
      var toolIframe = document.getElementById('toolIFrame');
      var errorDesc = i18n.TOOL_OIDC_ACCESS_DENIED;
      var contentDocument = toolIframe.contentDocument || toolIframe.contentWindow.document;
      if (contentDocument) {
        var contentBody = contentDocument.body;
        if (contentBody) {
            // Note: The current src is still pointing to "". Has to change the url back to adminCenter path.
            toolIframe.src = "../../../adminCenter/html/toolError.html";
            // listen to onload of the changed src to set the error text
            document.getElementById('toolIFrame').onload = function (event) {
              // add the error message
              var iframeObject = event.target;
              contentDocument = iframeObject.contentDocument || toolIframe.contentWindow.document;
              var errorTextElement = contentDocument.getElementById('errorText');
              if (errorDesc) {
                errorTextElement.innerText = errorDesc;
              }
            };
        }
      }
	}
  }

  return {
    /**
     * Opens a tool within the context of the toolbox.
     * 
     * @param from
     *          The view from which the tool was opened
     * @param tool
     *          The tool to be opened
     * @param noAnimation
     *          Indicates whether or not the transition to the tool should be animated. Opening a tool directly via a URL is an example of
     *          where animation is not desired.
     */
    openTool : function(from, tool, noAnimation) {
      console.log('Opening tool: ' + tool.id, tool.url);
      if (tool.isURLTool) {
        window.location.assign(tool.url);
      } else {
        setToolboxHeader(from, tool);
        openIFrame(from, tool, noAnimation);
      }
    }
  };

});
