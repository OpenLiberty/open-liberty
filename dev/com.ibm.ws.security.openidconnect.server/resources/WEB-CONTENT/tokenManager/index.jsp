<!--
    Copyright (c) 2019 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License v1.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-v10.html

    Contributors:
        IBM Corporation - initial API and implementation
-->
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<link href="${pageContext.request.contextPath}/WEB-CONTENT/common/css/tool.css" rel="stylesheet">
<link href="${pageContext.request.contextPath}/WEB-CONTENT/tokenManager/css/tokenManager.css" rel="stylesheet">
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<meta name="DC.Rights" content="© Copyright IBM Corp. 2019" />
<%
String accessToken = (String) request.getAttribute("ui_token");
String authHeader = (String) request.getAttribute("ui_authheader");
%>
<script type="text/javascript">
    window.globalAccessToken="<%=accessToken%>"
    window.globalAuthHeader="<%=authHeader%>"
</script> 

<%
// Set security headers	
response.setHeader("X-XSS-Protection", "1");	
response.setHeader("X-Content-Type-Options", "nosniff");	
response.setHeader("X-Frame-Options", "SAMEORIGIN");
%>

<title id="token_mgr_tool">Delete Tokens</title>
</head>

<body>
<div id="tm_loader" class="tool_processing_overlay tool_processing_overlay_transparent hidden">
    <div class="tool_processing">
        <svg class="tool_processing_svg" viewBox="-75 -75 150 150">
            <title data-externalizedTitle="PROCESSING"></title>
            <circle class="tool_processing_stroke" cx="0" cy="0" r="37.5"></circle>
        </svg>
    </div>
</div>
<div class="tool_container hide" role="main">
    <div id="tm_logout_div" class="tool_logout_div hidden">
        <button id="tm_logout" class="tool_logout_button" type="button" data-externalizedString="LOGOUT"></button>
    </div>
    <h2 id="tm_title" class="tool_title" data-externalizedString="TOKEN_MGR_TITLE"></h2>
    <div class="tool_desc_block">
        <div id="tm_desc" class="tool_desc" data-externalizedString="TOKEN_MGR_DESC"></div>
    </div>
    <div id="tool_search" role="search" class="tool_filter_div">
      <input id="search_userid" class="tool_filter_input" type="text" data-externalizedAriaLabel="TOKEN_MGR_SEARCH_PLACEHOLDER" data-externalizedPlaceholder="TOKEN_MGR_SEARCH_PLACEHOLDER">
      <img src='../../WEB-CONTENT/common/images/magnifierGlass.svg' class='tool_search_img' alt="" aria-hidden=true>
      <button id="clear_userid_search" class="tool_filter_clear" type="button" tabindex="0">
          <img src='../../WEB-CONTENT/common/images/clearSearch.svg' data-externalizedStringAlt="CLEAR_SEARCH" data-externalizedStringTitle="CLEAR_SEARCH">
      </button>
    </div>

    <div class="tool_table_toolbar">
        <div class="tool_table_batch tool_table_batch_active" data-externalizedAriaLabel="TABLE_BATCH_BAR" data-active="false" tabindex="-1">
            <div class="tool_table_batch_action_list">
                <button id="delete_batch_selection" class="batch_bar_button batch_bar_delete" type="button" data-externalizedString="DELETE" data-externalizedStringTitle="DELETE_SELECTED">
                    <img src="../../WEB-CONTENT/common/images/trash_can.svg" alt="" aria-hidden=true data-externalizedStringAlt="DELETE_SELECTED" data-externalizedStringTitle="DELETE_SELECTED">
                </button> 
<%--            <button id="cancel_batch_selection" class="batch_bar_button batch_bar_cancel" type="button" data-externalizedString="CANCEL"></button> --%>
            </div>
            <div class="tool_table_batch_summary">
                <span id="batch_selected_msg" class="batch_bar_summary"></span>
            </div>
        </div>
    </div>
    <table id="tm_table" role="grid" class="tool_table" aria-labelledby="tm_desc" aria-describedby="tm_desc">
        <thead>
            <tr role="row">
                <th id="table_select_column" class="table_column_checkbox" scope="col">
                    <div class="tool_checkbox_wrapper">
                        <input id="tm_select_all" class="tool_checkbox" type="checkbox" role="checkbox" aria-checked="false" data-externalizedAriaLabel="SELECT_ALL_AUTHS">
                        <div class="tool_checkbox_label" data-externalizedAriaLabel="SELECT_ALL_AUTHS" data-externalizedStringTitle="SELECT_ALL_AUTHS"></div>
                    </div>
                </th>
                <th id="table_name_column" class="table_sort_column" type="button" tabIndex="0" data-externalizedString="NAME_COL" role="columnheader" aria-sort="none" scope="col">
                    <span id="table_name_sort" class="table_sort_direction" data-SortDir="none">
                        <img src='../../WEB-CONTENT/common/images/caretSortUnknown.svg' data-externalizedStringAlt="CLICK_TO_SORT" data-externalizedStringTitle="CLICK_TO_SORT">
                    </span>
                </th>
                <th id="table_oauth_client_name_column" data-externalizedString="CLIENT_NAME_COL" scope="col" tabindex='-1'></th>
                <th id="table_type_column" data-externalizedString="TYPE_COL" scope="col" tabindex='-1'></th>
                <th id="table_issued_column" data-externalizedString="ISSUED_COL" scope="col" tabindex='-1'></th>
                <th id="table_expires_column" data-externalizedString="EXPIRES_COL" scope="col" tabindex='-1'></th>
                <th class="table_button_column" scope="col" tabindex='-1'></th>
            </tr>
        </thead>
        <tbody>
            <tr class="noQuery">
                <td colspan="7" data-externalizedString="NO_QUERY" tabindex='-1'></td>
            </tr>
        </tbody>
    </table>
    <div class="tool_table_pagination">
        <div class="tool_table_pagination_right">
            <div id="tm_table_page_number_label" class="tool_table_page_number_label"></div>
                <button id="tm_table_page_backward" class="tool_table_pagination_button tool_table_pagination_button_backward" type="button" tabIndex="0" disabled>
                    <img src="../../WEB-CONTENT/common/images/chevronLeft.svg" data-externalizedStringAlt="PREVIOUS_PAGE" data-externalizedStringTitle="PREVIOUS_PAGE">
                </button>
                <select id="tm_table_page_number_select" class="tool_table_page_select_input select-input" data-externalizedAriaLabel="PAGE_SELECT" tabIndex="0">
                </select>
                <button id="tm_table_page_forward" class="tool_table_pagination_button tool_table_pagination_button_forward" type="button" tabIndex="0" disabled>
                    <img src="../../WEB-CONTENT/common/images/chevronRight.svg" data-externalizedStringAlt="NEXT_PAGE" data-externalizedStringTitle="NEXT_PAGE">
                </button>    
            </div>
        </div>
    </div>
    
    <div id="delete_authentication_modal" class="tool_modal_container tool_modal_alert token_manager_delete hidden" tabindex="-1" role="alertdialog" aria-labelledby="delete_authentication_modal_title" aria-describedby="delete_authentication_modal_description" aria-modal="true">
        <div class="tool_modal_center_view">
            <div id="delete_processing" class="tool_processing_overlay tool_processing_overlay_modal hidden">
                <div class="tool_processing">
                    <svg class="tool_processing_svg" viewBox="-75 -75 150 150">
                        <title data-externalizedTitle="PROCESSING"></title>
                        <circle class="tool_processing_stroke" cx="0" cy="0" r="37.5"></circle>
                    </svg>
                </div>
            </div>            
            <div class="tool_modal_header">
                <button id="delete_modal_x_close" class="tool_modal_close tool_modal_x_close" type="button" data-externalizedAriaLabel="CLOSE">
                    <img src="../../WEB-CONTENT/common/images/modalClose.svg" data-externalizedStringAlt="CLOSE" data-externalizedStringTitle="CLOSE">
                </button>        
                <div id="delete_authentication_modal_title" class="tool_modal_secondary_title"></div>
                <div id="delete_authentication_name" class="tool_modal_title"></div>
            </div>
            <div class="tool_modal_body">
                <div id="delete_authentication_modal_description" class="tool_modal_body_description"></div>
            </div>
            <div class="tool_modal_button_container">
                <div class="tool_modal_button_right_frame">
                    <button id="deleteAuthCancel" class="tool_modal_close tool_modal_action_button tool_modal_cancel_button" type="button" data-externalizedString="CANCEL"></button>
                    <button id="deleteAuthDelete" class="tool_modal_action_button tool_modal_delete_button" type="button" data-externalizedString="DELETE"></button>
                </div>
            </div>
        </div>
    </div>
    
    <div id="results_modal" class="tool_modal_container hidden" tabindex="-1" role="dialog" aria-labelledby="results_modal_title" aria-describedby="results_modal_description" aria-modal="true">
        <div class="tool_modal_center_view">          
            <div class="tool_modal_header">
                <button id="results_modal_x_close" class="tool_modal_close tool_modal_x_close" type="button" data-externalizedAriaLabel="CLOSE">
                    <img src="../../WEB-CONTENT/common/images/modalClose.svg" data-externalizedStringAlt="CLOSE" data-externalizedStringTitle="CLOSE">
                </button>
                <div id="results_modal_title" class="tool_modal_title"></div>
            </div>
            <div class="tool_modal_body">
                <div id="results_modal_description" class="tool_modal_body_description"></div>
            </div>
            <div class="tool_modal_button_container">
                <div class="tool_modal_button_right_frame">
                    <button id="resultsCancel" class="tool_modal_close tool_modal_action_button tool_modal_cancel_button" type="button" data-externalizedString="CANCEL"></button>
                    <button id="resultsTryAgain" class="tool_modal_action_button tool_modal_try_again_button" type="button" data-externalizedString="TRY_AGAIN"></button>
                    <button id="resultsDone" class="tool_modal_close tool_modal_action_button tool_modal_done_button" type="button" data-externalizedString="DONE"></button>
                </div>
            </div>                
        </div>
    </div>

    <div id="accessibleLiveRegion" class="visuallyhidden" aria-live="polite"></div>

</div>

<script src="${pageContext.request.contextPath}/WEB-CONTENT/common/lib/jquery/jquery.min.js"></script>
<script src="${pageContext.request.contextPath}/WEB-CONTENT/common/js/globalization.js"></script>
<script src="${pageContext.request.contextPath}/WEB-CONTENT/common/js/utils.js"></script>
<script src="${pageContext.request.contextPath}/WEB-CONTENT/common/js/tableUtils.js"></script>
<script src="${pageContext.request.contextPath}/WEB-CONTENT/common/js/bidiUtils.js"></script> 
<script src="${pageContext.request.contextPath}/WEB-CONTENT/tokenManager/js/apiUtils.js"></script>
<script src="${pageContext.request.contextPath}/WEB-CONTENT/tokenManager/js/table.js"></script>
<script src="${pageContext.request.contextPath}/WEB-CONTENT/tokenManager/js/tokenManager.js"></script>
<script src="${pageContext.request.contextPath}/WEB-CONTENT/tokenManager/nls/messages.js"></script>
</body>
</html>
