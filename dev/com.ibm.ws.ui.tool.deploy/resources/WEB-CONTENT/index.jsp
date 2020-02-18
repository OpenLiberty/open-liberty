<%--
    Copyright (c) 2014 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License v1.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-v10.html

    Contributors:
        IBM Corporation - initial API and implementation
 --%>
<html>
    <head>
        <link href="css/deploy.css" rel="stylesheet">
    <title data-externalizedString="DEPLOY_TOOL_TITLE"></title>
    <meta charset="utf-8" />
    <%@ include file="img/Icons.svg" %>
    <%
    boolean isAdmin = request.isUserInRole("Administrator");
    %>
    <script type="text/javascript">
        globalIsAdmin=<%=isAdmin%>
    </script>

    <%
    // Set security headers	
    response.setHeader("X-XSS-Protection", "1");	
    response.setHeader("X-Content-Type-Options", "nosniff");	
    response.setHeader("X-Frame-Options", "SAMEORIGIN");
    %>
  </head>
    <body>
        <section id="ruleSelect" class="slider" role="region" aria-label="Select Rules">
            <div class="slider--submission-bar">
                <div class="submission--gradient"></div>
                <div class="button button--selection">
                    <a href="#" id="submit-done-button" class="button--done" data-externalizedString="RULESELECT_CONFIRM" data-externalizedStringTitle="RULESELECT_CONFIRM" role="button">
                        <svg width="16px" height="10px" viewBox="121 15 16 10" version="1.1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink">
                            <polyline stroke="#D7D7D7" stroke-width="2" fill="none" points="122.339209 16.7295457 129.09661 23.7295457 135.854011 16.7295457"></polyline>
                        </svg>
                    </a>
                </div>
            </div>
            <div class="slider--wrapper">
                <div class="js--nav">
                    <p class="nav--selections">
                        <a href="#" id="server-selection" class="js--nav__server" data-type="server" role="button" data-externalizedString="RULESELECT_SERVER_TYPE"></a>
                        <span style="font-weight: bold">|</span>
                        <a href="#" id="deploy-selection" class="js--nav__deploy" data-type="deploy" role="button" data-externalizedString="RULESELECT_DEPLOY_TYPE"></a>
                    </p>
                    <a href="#" id="edit-selection" class="nav--edit-selections js--edit-selections" data-externalizedString="RULESELECT_CHANGE_SELECTION" role="button" data-externalizedStringTitle="RULESELECT_CHANGE_SELECTION">
                        <svg width="16px" height="10px" viewBox="121 15 16 10" version="1.1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink">
                            <polyline  stroke="#D7D7D7" stroke-width="2" fill="none" points="122.339209 16.7295457 129.09661 23.7295457 135.854011 16.7295457"></polyline>
                        </svg>
                    </a>
                </div>

                <ul class="slider--list">
                     <li class="slider--list-item server list-item--arrow">
                        <div class="server--list-types positioning" tabindex="-1">
                            <div class="list-types--default" id="deploy-server-list">
                                <div class="list-types--headers">
                                    <p data-externalizedString="RULESELECT_SERVER_DEFAULT" data-externalizedStringTitle="RULESELECT_SERVER_DEFAULT"></p>
                                </div>
                                <div class="list-types--groupings">
                                    <ul id=default-runtime-list-items>
                                    </ul>
                                </div>
                            </div>
                            <div class="list-types--custom">
                                <div class="list-types--headers">
                                    <p data-externalizedString="RULESELECT_SERVER_CUSTOM" data-externalizedStringTitle="RULESELECT_SERVER_CUSTOM"></p>
                                </div>
                                <div class="list-types--groupings">
                                    <ul id="custom-runtime-list-items">

                                    </ul>
                                </div>
                            </div>
                        </div>
                    </li>
                    <li class="slider--list-item server bg--lighter">
                        <div class="initial--overlay">
                            <div class="positioning" role="application" data-externalizedAriaLabel="RULESELECT_EDIT_SERVER_ARIA">
                                <div class="positioning--wrapper">
                                    <div class="choosing--headline">
                                        <div class="headline-top" data-externalizedString="RULESELECT_SERVER_TYPE"></div>
                                        <div class="headline-bottom" data-externalizedString="RULESELECT_SELECT_ONE"></div>
                                    </div>
                                    <div id="choose-server-selected" class="choosing--selected server--selected">
                                        <div class="headline-top--selected"></div>
                                        <div class="headline-bottom--selected" data-externalizedString="RULESELECT_SERVER_SUBHEADING" data-externalizedStringTitle="RULESELECT_SERVER_SUBHEADING"></div>
                                    </div>
                                    <div class="choosing--edit" id="choose-server-edit" data-type="server" role="presentation">
                                        <div class="headline-edit" data-externalizedString="RULESELECT_EDIT"></div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </li>

                    <li class="slider--list-item deploy bg--lighter">
                        <div class="button--back" role="button" data-externalizedAriaLabel="RULESELECT_BACK" data-externalizedStringTitle="RULESELECT_BACK">
                            <div class="svg--wrapper">
                                <svg width="27px" height="46px" viewBox="-2401 8179 27 46" version="1.1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink">
                                    <polyline  stroke="#345D2A" stroke-width="4" fill="none" transform="translate(-2387.382012, 8201.500000) rotate(90.000000) translate(2387.382012, -8201.500000) " points="-2407.88201 8190.88201 -2387.38201 8212.11799 -2366.88201 8190.88201"></polyline>
                                </svg>
                            </div>
                        </div>
                        <div class="initial--overlay">
                            <div class="positioning" role="application" data-externalizedAriaLabel="RULESELECT_EDIT_RULE_ARIA">
                                <div class="positioning--wrapper">
                                    <div class="choosing--headline">
                                        <div class="headline-top" data-externalizedString="RULESELECT_DEPLOY_TYPE" data-externalizedStringTitle="RULESELECT_DEPLOY_TYPE"></div>
                                        <div class="headline-bottom" data-externalizedString="RULESELECT_SELECT_ONE" data-externalizedStringTitle="RULESELECT_SELECT_ONE"></div>
                                    </div>
                                    <div class="choosing--selected deploy--selected">
                                        <div class="headline-top--selected"></div>
                                    </div>
                                    <div class="choosing--edit" id="choose-deploy-edit" data-type="deploy" role="presentation">
                                        <div class="headline-edit" data-externalizedString="RULESELECT_EDIT"></div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </li>
                    <li class="slider--list-item deploy list-item--arrow">
                        <div class="scroll">
                            <button class="js--scroll js--scroll-top" id="rs-scroll-up" aria-labelledby="rs-scroll-up-label" tabindex="-1">
                                <svg width="24px" height="15px" viewBox="1612 129 24 15" version="1.1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink">
                                    <title data-externalizedTitle="RULESELECT_SCROLL_UP" style="color: #FFFFFF">Scroll Up</title>
                                    <polyline  stroke="#AEAEAE" stroke-width="3" fill="none" points="1614 132 1624.09661 142.459091 1634.19322 132"></polyline>
                                </svg>
                                                                <label class="hide" id="rs-scroll-up-label" for="rs-scroll-up" data-externalizedLabel="RULESELECT_SCROLL_UP"></label>
                            </button>
                            <button class="js--scroll js--scroll-bottom" id="rs-scroll-down" aria-labelledby="rs-scroll-down-label" tabindex="-1">
                                <svg width="24px" height="15px" viewBox="1612 167 24 15" version="1.1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink">
                                    <title data-externalizedTitle="RULESELECT_SCROLL_DOWN" style="color: #FFFFFF">Scroll Down</title>
                                    <polyline  stroke="#AEAEAE" stroke-width="3" fill="none" points="1634.19322 169 1624.09661 179.459091 1614 169"></polyline>
                                </svg>
                                <label class="hide" id="rs-scroll-down-label" for="rs-scroll-down" data-externalizedLabel="RULESELECT_SCROLL_DOWN"></label>
                            </button>
                        </div>
                        <div class="deploy--list-types positioning" tabindex="-1">
                            <div class="list-types--default" id="deploy-package-list">
                                <div class="list-types--headers">
                                    <p data-externalizedString="RULESELECT_RULE_DEFAULT" data-externalizedStringTitle="RULESELECT_RULE_DEFAULT"></p>
                                </div>
                                <div class="list-types--groupings">
                                    <ul id="default-container-list-items">

                                    </ul>
                                </div>
                            </div>
                            <div class="list-types--custom">
                                <div class="list-types--headers">
                                    <p data-externalizedString="RULESELECT_RULE_CUSTOM" data-externalizedStringTitle="RULESELECT_RULE_CUSTOM"></p>
                                </div>
                                <div class="list-types--groupings">
                                    <ul id="custom-container-list-items">

                                    </ul>
                                </div>
                            </div>
                        </div>
                    </li>
                </ul>
            </div>
        </section>

        <div class="form--overlay js--form--overlay">
            <div class="form--overlay-verbiage">
                <span data-externalizedString="RULESELECT_FOOTER" data-externalizedStringTitle="RULESELECT_FOOTER"></span>
            </div>
        </div>
        <div id="contents" role="main" class="hidden form">

            <!-- Deployment parameters -->
        <section id="parameters" class="parameters card hidden">
            <div id="parametersLeftPane" class="parametersLeftPane" aria-labelledBy="parameterBanner">
                <header id="parameterBanner" class="parameterBanner"
                    data-externalizedString="DEPLOYMENT_PARAMETERS"></header>
                <article id="parametersDescription"
                    class="parametersDescription"
                    data-externalizedString="PARAMETERS_DESCRIPTION"
                    data-externalizedAriaLabel="PARAMETERS_DESCRIPTION"></article>
                <span id="parameterToggle" class="parameterToggle hidden" tabindex="-1">
                  <button id="parameterToggleController" class="parameterToggleController" data-externalizedString="PARAMETERS_TOGGLE_CONTROLLER" data-externalizedAriaLabel="PARAMETERS_TOGGLE_CONTROLLER" data-externalizedStringTitle="PARAMETERS_TOGGLE_CONTROLLER" role='button' tabindex="0"></button><button id="parameterToggleUpload" class="parameterToggleUpload" data-externalizedString="PARAMETERS_TOGGLE_UPLOAD" data-externalizedStringTitle="PARAMETERS_TOGGLE_UPLOAD" data-externalizedAriaLabel="PARAMETERS_TOGGLE_UPLOAD" role='button' tabindex="0"></button>
                </span>
                <div id="inputVariablesContainer"
                    class="inputVariablesContainer"></div>
            </div>

            <div id="parametersRightPane"
                class="parametersRightPane">
            </div>
        </section>

        <div id="listSearch" class="listSearch hide hidden">
          <span id="listSearchIconSpan"
              class="listSearchIconSpan">
              <div id="listSearchIcon"
                  class="listSearchIcon"></div>
              <label id="listSearchFieldLabel" class="hide" data-externalizedLabel="SEARCH_IMAGES"></label>
              <input type="text"
              id="listSearchField"
              class="listSearchField hide"
              data-externalizedPlaceholder="SEARCH_IMAGES" aria-labelledby="listSearchFieldLabel">
          </span>
          <div id="listView" class="listView" role="presentation">
              <div id="listViewList"
                  class="listViewList" aria-label="Docker Images or Cluster List"></div>
              <div id="listViewError" class="listViewError hidden">
                  <div id="listViewErrorIcon" class="listViewErrorIcon"></div>
                  <div id="listViewErrorMessage" class="listViewErrorMessage"></div>
                  <div id="listViewErrorLink" class="listViewErrorLink"></div>
                  <div id="listViewErrorFooter" class="listViewErrorFooter"></div>
              </div>
          </div>
        </div>


        <div id="serverBrowseAndUpload" class="serverBrowseAndUpload hide hidden">
          <header id="serverBrowseTitle" class="serverBrowseTitle" role="presentation"></header>
              <!-- data-externalizedString="SERVER_BROWSE_DESCRIPTION" -->

          <!-- <article id="serverBrowseDescription" data-externalizedString="SERVER_BROWSE_DESCRIPTION"></article> -->
          <div id="serverFileBrowser" class="serverFileBrowser">
              <div id="fileBrowseStatusIcon"
                  class="fileBrowseStatusIcon"></div>
              <div id="fileBrowserMessages"
                  class="fileBrowserMessages">
                  <div id="fileUploadSuccess"
                      class="fileUploadSuccess">
                      <span id="fileUploadSuccessMessage"
                          class="fileUploadSuccessMessage"></span>
                  </div>
                  <div>
                      <div id="fileBrowseMessage" class="fileBrowseMessage">
                          <label id="browseLabel" class="hide" data-externalizedLabel="BROWSE_ARIA"></label>
                      </div>
                      <input type="file" id="browse" class="browse" style="display: none" role="presentation">
                  </div>
              </div>
          </div>
          <div id="fileUploadFooter" class="fileUploadFooter">
              <span id="fileUploadResetButton"
                  class="fileUploadResetButton" role="button" data-externalizedAriaLabel="RESET"></span>
          </div>
      </div>

        <!-- Host selection -->
            <section id="hostSelection" class="card" role="region" tabindex="0" data-externalizedAriaLabel="SELECTED_HOSTS_LIST_ARIA">
	          <div id="hostSelectionContent">
	            <div id="hostSelectionContentLeft">
	              <header id="hostSelectionBanner"></header>
	              <div id="selectedHostsMessage" data-externalizedString="SELECT_HOSTS_MESSAGE"></div>
	              <div id="selectedHosts">
	                <div id="selectedHostsList" role="application" data-externalizedAriaLabel="SELECTED_HOSTS_LIST_ARIA"></div>
	              </div>
	            </div>
	            <div id="hostSelectionContentRight">
	              <div id="searchHostBox">
	                  <label id="searchHostLabel" class="hide" for="searchHost" data-externalizedLabel="SEARCH_HOSTS"></label>
	                <input id="searchHost" type="text" data-externalizedPlaceholder="SEARCH_HOSTS" autocomplete="off">
	                <div id="searchHostIconContainer">
	                    <div id="searchHostIcon"></div>
	                </div>
	                <span id="searchHostFilter" tabindex="-1">
	                  <button id="searchNameFilter" data-externalizedString="NAME" data-externalizedAriaLabel="NAME_FILTER" role='button' tabindex="0"></button><button id="searchTagFilter" data-externalizedString="TAG" data-externalizedAriaLabel="TAG_FILTER" role='button' tabindex="0"></button>
	                </span>
	              </div>
	              <div id="hostListCount"></div>
	              <div id="allHosts">
	                <div id="allHostsList" role="application" data-externalizedAriaLabel="ALL_HOSTS_LIST_ARIA"></div>
	              </div>
	              <footer id="hostSearchFooter">
	              </footer>
	            </div>
	          </div>
            </section>

        <!-- Security card -->
            <div id="security" class="card hidden" tabindex="0">
                <div id="securityTopPane">
                    <div id="securityTopPaneTitle" data-externalizedString="SECURITY_DETAILS"></div>
                    <div id="securityTopPaneMessage" data-externalizedString="SECURITY_DETAILS_MESSAGE"></div>
                </div>
                <div id="securityBottomPane">
                    <div id="securityBottomPaneTitle" data-externalizedString="SECURITY_CREATE_PASSWORD"></div>
                    <div id="securityBottomPaneMessage" data-externalizedString="PASSWORD_MESSAGE"></div>
                    <div style="display:table; width: 100%;">
                        <div id="securityInputContainer" style="display:table-cell; width: 50%;">
                        </div>
                        <div id="securityInputConfirmContainer" style="display:table-cell; width: 50%;">
                        </div>
                    </div>
                </div>
            </div>

            </div>
        <!--  </div> -->

            <!-- Review and deploy -->
            <section id="review" tabindex="0" data-externalizedAriaLabel="REVIEW_AND_DEPLOY" role="region">
                <div id="reviewContainer">
                    <div id="reviewBanner" data-externalizedString="REVIEW_AND_DEPLOY"></div>
                    <table id="reviewTable" cellspacing="0" role="presentation">
                        <tr>
                            <td style="vertical-align: top">
                                <span id="reviewContent"></span>
                                <div id="reviewComplete" class="hidden"></div>
                            </td>
                            <td id="deployButton" tabindex="-1" disabled="disabled" role='button' aria-disabled="true">
                            <div  id="deployButtonContent" data-externalizedString="DEPLOY" data-externalizedAriaLabel="DEPLOY"></div>
                            <div id="deployButtonArrowContainer">
                                <div id="deployButtonArrow"></div>
                            </div>
                            </td>
                        </tr>
                    </table>
                </div>
            </section>

        <!--  This extra div is required to keep the modal and footer together after they deploy -->
        <div id="deployModalContainer">
            <section id="deployModal" class="deployModal hidden" role="region" aria-label="Deploy Modal">
                <div id="deploymentTopPane">
                    <div id="deploymentTopPaneLeft">
                        <div id="deploymentProgressCircleContainer">
                            <div id="deployProgressCircle" class="hidden"></div>
                            <div id="deployProgressCircleText" class="hidden" data-externalizedString="DEPLOYING"></div>
                        </div>
                        <div id="deployProgressCount"></div>
                </div>
                <div id="deploymentTopPaneRight">
                    <div id="deploymentMessage" role="status">
                        <div id="deploymentMessageTitle" class="deploymentMessageTitle" role="presentation"></div>
                        <div id="deploymentMessageContentWrapper">
                            <div id="deploymentMessageContent" role="presentation"></div>
                        </div>
                        <div id="deploymentMessageButtonHeader" class="hidden"></div>
                        <span id="deployMessageButton" style="display: none" role="button" data-externalizedAriaLabel="VIEW_DEPLOYED_SERVERS" tabindex="-1">
                            <span id="deployMessageButtonContent"></span>
                            <span id="deployMessageButtonArrow"></span>
                        </span>
                    </div>
                </div>
            </div>

            <div id="deployErrors" role="region" data-externalizedAriaLabel="ERROR_VIEW_DETAILS"> <!--  Aria label will be changed in javascript; This is to pass RPT -->
                <div id="deployErrorsTitle" role="presentation"></div>
                <div id="yellowRectangle"></div>
                <div id="deployErrorsSummaryBottomDiv">
                    <div id="deployErrorsMessageAndHosts">
		                <div id="deployErrorsHosts" class="errorHosts" role="presentation"></div>
		                <div id="backgroundTasksButton" role="button" tabindex="-1" data-externalizedAriaLabel="ERROR_VIEW_DETAILS">
		                    <div id="backgroundTasksButtonContent" data-externalizedString="ERROR_VIEW_DETAILS"></div>
		                    <div id="backgroundTasksButtonArrow"></div>
		                </div>
                    </div>
                    <!-- <div id="deployErrorsButtonDiv">
                        <button id="displayErrorsButton" data-externalizedString="ERROR_VIEW_DETAILS" role="button"></button>
                    </div>     -->
                </div>
            </div>
            <div id="deployErrorsFooter"></div>
            <!-- End of deployModalContainer -->


            </section>
            <div id="footerContainer">
                <div id="footer" role="region" data-externalizedAriaLabel="FOOTER_BUTTON_MESSAGE">
                    <div id="footerMessage" data-externalizedString="FOOTER"></div>
                    <div id="footerButton" role='button' tabindex="0" data-externalizedAriaLabel="FOOTER_BUTTON_MESSAGE">
                        <div id="footerButtonMessage" data-externalizedString="FOOTER_BUTTON_MESSAGE"></div>
                        <div id="footerButtonPlus">+</div>
                    </div>
                </div>
            </div>
        </div>




        <!-- Error messages -->
        <div id="translationsErrorMessage" class="errorMessage hidden" data-externalizedString="TRANSLATIONS_ERROR_MESSAGE"></div>
        <div id="initializationErrorMessage" class="errorMessage hidden" data-externalizedString="INITIALIZATION_ERROR_MESSAGE"></div>

    <script src="lib/jquery.min.js"></script>
    <script src="lib/velocity.min.js"></script>
    <script src="lib/dotdotdot.min.js"></script>
    <script src="js/prod.min.js"></script>
</body>
</html>
