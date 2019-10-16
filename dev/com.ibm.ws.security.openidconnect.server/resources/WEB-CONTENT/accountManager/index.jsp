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
<link href="${pageContext.request.contextPath}/WEB-CONTENT/accountManager/css/accountManager.css" rel="stylesheet">
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<meta name="DC.Rights" content="© Copyright IBM Corp. 2019" />
<%
String accessToken = (String) request.getAttribute("ui_token");
String authHeader = (String) request.getAttribute("ui_authheader");
boolean isAppPasswordAllowed = false;
if (request.getAttribute("ui_app_pw_enabled") != null) {
  isAppPasswordAllowed =  Boolean.parseBoolean(request.getAttribute("ui_app_pw_enabled").toString());
}
boolean isAppTokenAllowed = false;
if (request.getAttribute("ui_app_tok_enabled") != null) {
  isAppTokenAllowed =  Boolean.parseBoolean(request.getAttribute("ui_app_tok_enabled").toString());
}
%>
<script type="text/javascript">
    window.globalAccessToken="<%=accessToken%>"
    window.globalAuthHeader="<%=authHeader%>"
    window.globalAppPasswordsAllowed="<%=isAppPasswordAllowed%>".toLowerCase() === 'true';
    window.globalAppTokensAllowed="<%=isAppTokenAllowed%>".toLowerCase() === 'true';
 </script>

<%
// Set security headers	
response.setHeader("X-XSS-Protection", "1");	
response.setHeader("X-Content-Type-Options", "nosniff");	
response.setHeader("X-Frame-Options", "SAMEORIGIN");
%>

<title id="acct_mgr_tool">Manage Personal Tokens</title>
</head>

<body>
<div id="am_loader" class="tool_processing_overlay tool_processing_overlay_transparent">
    <div class="tool_processing">
        <svg class="tool_processing_svg" viewBox="-75 -75 150 150">
            <title data-externalizedTitle="LOADING"></title>
            <circle class="tool_processing_stroke" cx="0" cy="0" r="37.5"></circle>
        </svg>
    </div>
</div>
<div class="tool_container hide" role="main">
    <div id="am_logout_div" class="tool_logout_div hidden">
        <button id="am_logout" class="tool_logout_button" type="button" data-externalizedString="LOGOUT"></button>
    </div>
    <h2 id="am_title" class="tool_title" data-externalizedString="ACCT_MGR_TITLE"></h2>
    <div class="tool_desc_block">
        <div id="am_desc" class="tool_desc" data-externalizedString="ACCT_MGR_DESC"></div>
        <button id="add_new_authentication" class="tool_desc_block_button" type="button" data-externalizedString="ADD_NEW" data-externalizedStringTitle="ADD_NEW_AUTHENTICATION">
                <img src="../../WEB-CONTENT/common/images/addNew.svg" alt="" aria-hidden=true>
        </button>
    </div>

    <table id="am_table" role="grid" class="tool_table" aria-labelledby="am_desc" aria-describedby="am_desc">
        <thead>
            <tr role="row">
                <th id="table_name_column" class="table_sort_column" type="button" tabIndex="0" data-externalizedString="NAME_COL" role="columnheader" aria-sort="none" scope="col">
                    <span id="table_name_sort" class="table_sort_direction" data-sortDir="none">
                        <img src='../../WEB-CONTENT/common/images/caretSortUnknown.svg' data-externalizedStringAlt="CLICK_TO_SORT" data-externalizedStringTitle="CLICK_TO_SORT">
                    </span>
                </th>
                <th id="table_type_column" data-externalizedString="TYPE_COL" scope="col" tabindex='-1'></th>
                <th id="table_issued_column" data-externalizedString="ISSUED_COL" scope="col" tabindex='-1'></th>
                <th id="table_expires_column" data-externalizedString="EXPIRES_COL" scope="col" tabindex='-1'></th>
                <th class="table_button_column" scope="col" tabindex='-1'></th>
                <th class="table_button_column" scope="col" tabindex='-1'></th>
            </tr>
        </thead>
        <tbody>
        </tbody>
    </table>
    <div class="tool_table_pagination">
        <div class="tool_table_pagination_right">
            <div id="am_table_page_number_label" class="tool_table_page_number_label"></div>
            <button id="am_table_page_backward" class="tool_table_pagination_button tool_table_pagination_button_backward" type="button" tabIndex="0" disabled>
                <img src="../../WEB-CONTENT/common/images/chevronLeft.svg" data-externalizedStringAlt="PREVIOUS_PAGE" data-externalizedStringTitle="PREVIOUS_PAGE">
            </button>
            <select id="am_table_page_number_select" class="tool_table_page_select_input select-input" data-externalizedAriaLabel="PAGE_SELECT" tabIndex="0">
            </select>
            <button id="am_table_page_forward" class="tool_table_pagination_button tool_table_pagination_button_forward" type="button" tabIndex="0" disabled>
                <img src="../../WEB-CONTENT/common/images/chevronRight.svg" data-externalizedStringAlt="NEXT_PAGE" data-externalizedStringTitle="NEXT_PAGE">
            </button>    
        </div>
    </div>
    
    <div id="add_regen_authentication_modal" class="tool_modal_container ss_authenticate hidden" tabindex="-1" role="dialog" aria-labelledby="add_regen_authentication_modal_title" aria-modal="true">
        <div class="tool_modal">
            <div id="add_regen_processing" class="tool_processing_overlay tool_processing_overlay_modal hidden">
                <div class="tool_processing">
                    <svg class="tool_processing_svg" viewBox="-75 -75 150 150">
                        <title data-externalizedTitle="PROCESSING"></title>
                        <circle class="tool_processing_stroke" cx="0" cy="0" r="37.5"></circle>
                    </svg>
                </div>
            </div>            
            <div class="tool_modal_header">
                <button id="add_regen_authentication_modal_x_close" class="tool_modal_close tool_modal_x_close" type="button" data-externalizedAriaLabel="CLOSE">
                    <img src="../../WEB-CONTENT/common/images/modalClose.svg" data-externalizedStringAlt="CLOSE" data-externalizedStringTitle="CLOSE">
                </button>
                <div id="add_regen_authentication_modal_title" class="tool_modal_title"></div>
            </div>
            <div class="tool_modal_body">
                <div class="tool_modal_body_field hidden">
                    <label class="tool_modal_body_field_label" for="name" data-externalizedLabel="NAME_COL"></label>
                    <input id="name" type="text" class="tool_modal_body_field_input">
                </div>
                <div class="tool_modal_body_info hidden">
                    <div class="tool_modal_body_info_item" style="margin-bottom: 13px;">
                        <span class="tool_modal_body_info_label"></span>
                    </div>
                    <div class="tool_modal_body_description"></div>
                </div>
                <div id='authType'>
                    <fieldset class='tool_modal_radio_button_fieldset'>
                    <legend class="tool_modal_body_field_label" data-externalizedString="TYPE_COL"></legend>
                    <input id="rb_app_password" class="tool_modal_radio_button" type="radio" role="radio" aria-checked="false" name="authType" value="app-password"></input>
                    <label for="rb_app_password" class="tool_modal_radio_button_label">
                        <span class="tool_modal_radio_button_appearance"></span>
                        app-password
                    </label>
                    <input id="rb_app_token" class="tool_modal_radio_button" type="radio" role="radio" aria-checked="false" name="authType" value="app-token"></input>
                    <label for="rb_app_token" class="tool_modal_radio_button_label">
                        <span class="tool_modal_radio_button_appearance"></span>
                        app-token
                    </label>
                    </fieldset>
                </div>
                <div class="tool_modal_body_auth_value authValueDiv readonly">
                    <input id="auth_value" type="text" class="tool_modal_body_field_auth_value readonly" readonly tabIndex="0"></input>
                    <button id="auth_value_copy" class='tool_modal_field_copy_button' type="button" tabindex="0">
                        <img src='../../WEB-CONTENT/common/images/copy_button.svg' data-externalizedStringAlt="COPY_TO_CLIPBOARD" data-externalizedStringTitle="COPY_TO_CLIPBOARD">
                    </button>
                </div>
            </div>
            <div class="tool_modal_button_container">
                <div class="tool_modal_button_right_frame">
                    <button id="auth_cancel" class="tool_modal_close tool_modal_action_button tool_modal_cancel_button" type="button" data-externalizedString="CANCEL"></button>
                    <button id="auth_generate" class="tool_modal_action_button tool_modal_generate_button" type="button" data-externalizedString="GENERATE"></button>
                    <button id="auth_done" class="tool_modal_close tool_modal_action_button tool_modal_done_button" type="button" data-externalizedString="DONE"></button>
                </div>
            </div>
        </div>
        <div id="copied_confirmation" data-externalizedString="COPIED_TO_CLIPBOARD" aria-hidden=true></div>
    </div>

    <div id="delete_authentication_modal" class="tool_modal_container tool_modal_alert ss_delete hidden" tabindex="-1" role="alertdialog" aria-labelledby="delete_authentication_modal_title" aria-describedby="delete_authentication_modal_description" aria-modal="true">
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
<script src="${pageContext.request.contextPath}/WEB-CONTENT/accountManager/js/apiUtils.js"></script>
<script src="${pageContext.request.contextPath}/WEB-CONTENT/accountManager/js/table.js"></script>
<script src="${pageContext.request.contextPath}/WEB-CONTENT/accountManager/js/accountManager.js"></script>
<script src="${pageContext.request.contextPath}/WEB-CONTENT/accountManager/nls/messages.js"></script>
</body>
</html>
