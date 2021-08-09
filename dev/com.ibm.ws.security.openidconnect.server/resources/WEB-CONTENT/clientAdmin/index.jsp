<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<!--
    Copyright (c) 2019 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License v1.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-v10.html

    Contributors:
        IBM Corporation - initial API and implementation
-->
<html>
<head>
<link href="${pageContext.request.contextPath}/WEB-CONTENT/common/css/tool.css" rel="stylesheet">
<link href="${pageContext.request.contextPath}/WEB-CONTENT/clientAdmin/css/clientAdmin.css" rel="stylesheet">
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<meta name="DC.Rights" content="© Copyright IBM Corp. 2019" />
<%
// Set security headers	
response.setHeader("X-XSS-Protection", "1");	
response.setHeader("X-Content-Type-Options", "nosniff");	
response.setHeader("X-Frame-Options", "SAMEORIGIN");
%>
<title id="client_admin_tool">Manage OAuth Clients</title>
</head>

<body>
<div id="ca_loader" class="tool_processing_overlay tool_processing_overlay_transparent">
    <div class="tool_processing">
        <svg class="tool_processing_svg" viewBox="-75 -75 150 150">
            <title data-externalizedTitle="LOADING"></title>
            <circle class="tool_processing_stroke" cx="0" cy="0" r="37.5"></circle>
        </svg>
    </div>
</div>
<div class="tool_container hide" role="main">
    <div id="ca_logout_div" class="tool_logout_div hidden">
        <button id="ca_logout" class="tool_logout_button" data-externalizedString="LOGOUT" type="button"></button>
    </div>
    <h2 id="ca_title" class="tool_title" data-externalizedString="CLIENT_ADMIN_TITLE"></h2>
    <div class="tool_desc_block">
        <div id="ca_desc" class="tool_desc" data-externalizedString="CLIENT_ADMIN_DESC"></div>
        <div class="tool_starter">
            <div id="tool_search" role="search" class="tool_filter_div">
                <input id="filter_client_name" class="tool_filter_input" type="text" data-externalizedAriaLabel="CLIENT_ADMIN_SEARCH_PLACEHOLDER" data-externalizedPlaceholder="CLIENT_ADMIN_SEARCH_PLACEHOLDER">
                <img src="../../WEB-CONTENT/common/images/filter.svg" class="tool_filter_img" alt="" aria-hidden=true>
                <button id="clear_client_name_filter" class="tool_filter_clear" tabindex="0" type="button">
                    <img src="../../WEB-CONTENT/common/images/clearSearch.svg" data-externalizedStringAlt="CLEAR_FILTER" data-externalizedStringTitle="CLEAR_FILTER">
                </button>
            </div>
            <button id="add_new_client" class="tool_desc_block_button" data-externalizedString="ADD_NEW" type="button" data-externalizedStringTitle="ADD_NEW_CLIENT">
                    <img src="../../WEB-CONTENT/common/images/addNew.svg" alt="" aria-hidden=true>
            </button>
        </div>
    </div>

    <table id="ca_table" role="grid" class="tool_table" aria-labelledby="ca_desc" aria-describedby="ca_desc">
        <thead>
            <tr role="row">
                <th id="table_client_name_column" class="table_sort_column" type="button" tabIndex="0" data-externalizedString="CLIENT_NAME"  role="columnheader" aria-sort="none" scope="col">
                    <span id="table_client_name_sort" class="table_sort_direction" data-sortDir="none">
                        <img src='../../WEB-CONTENT/common/images/caretSortUnknown.svg' data-externalizedStringAlt="CLICK_TO_SORT" data-externalizedStringTitle="CLICK_TO_SORT">
                     </span>
                </th>
                <th id="table_client_id_column" data-externalizedString="CLIENT_ID" scope="col" tabindex='-1'></th>
                <th class="table_button_column" scope="col" tabindex='-1'></th>
                <th class="table_button_column" scope="col" tabindex='-1'></th>
            </tr>
        </thead>
        <tbody>
        </tbody>
    </table>
    <div class="tool_table_pagination">
        <div class="tool_table_pagination_right">
            <div id="ca_table_page_number_label" class="tool_table_page_number_label"></div>
            <button id="ca_table_page_backward" class="tool_table_pagination_button tool_table_pagination_button_backward" tabIndex="0" disabled type="button">
                <img src="../../WEB-CONTENT/common/images/chevronLeft.svg" data-externalizedStringAlt="PREVIOUS_PAGE" data-externalizedStringTitle="PREVIOUS_PAGE">
            </button>
            <select id="ca_table_page_number_select" class="tool_table_page_select_input select-input" data-externalizedAriaLabel="PAGE_SELECT" tabIndex="0">
            </select>
            <button id="ca_table_page_forward" class="tool_table_pagination_button tool_table_pagination_button_forward" tabIndex="0" disabled type="button">
                <img src="../../WEB-CONTENT/common/images/chevronRight.svg" data-externalizedStringAlt="NEXT_PAGE" data-externalizedStringTitle="NEXT_PAGE">
            </button>    
        </div>
    </div>

    <div id="add_edit_client_modal" class="tool_modal_container hidden" tabindex="-1" role="dialog" aria-labelledby="add_edit_client_modal_title" aria-modal="true">
        <div class="tool_modal">
            <div id="add_edit_processing" class="tool_processing_overlay tool_processing_overlay_modal hidden">
                <div class="tool_processing">
                    <svg class="tool_processing_svg" viewBox="-75 -75 150 150">
                        <title data-externalizedTitle="PROCESSING"></title>
                        <circle class="tool_processing_stroke" cx="0" cy="0" r="37.5"></circle>
                    </svg>
                </div>
            </div>        
            <div class="tool_modal_header">
                <button id="add_edit_client_modal_x_close" class="tool_modal_close tool_modal_x_close" type="button" data-externalizedAriaLabel="CLOSE">
                    <img src="../../WEB-CONTENT/common/images/modalClose.svg" data-externalizedStringAlt="CLOSE" data-externalizedStringTitle="CLOSE" type="button">
                </button>
                <div id="add_edit_client_modal_title" class="tool_modal_title"></div>
                <div class="tool_modal_secondary_title tool_modal_information" data-externalizedString="ALL_OPTIONAL"></div>
            </div>
            <div class="tool_modal_body">                
            </div>        
            <div class="tool_modal_button_container">
                <div class="tool_modal_button_right_frame">
                    <button id="addEditCancel" class="tool_modal_close tool_modal_action_button tool_modal_cancel_button" type="button" data-externalizedString="CANCEL"></button>
                    <button id="addEditUpdate" class="tool_modal_action_button tool_modal_update_button" type="button" data-externalizedString="UPDATE"></button>
                    <button id="addEditRegister" class="tool_modal_action_button tool_modal_register_button" type="button" data-externalizedString="REGISTER"></button>
                </div>
            </div>
        </div>            
    </div>

    <div id="client_secret_modal" class="tool_modal_container hidden" tabindex="-1" role="dialog" aria-labelledby="client_secret_modal_title" aria-describedby="client_secret_modal_description" aria-modal="true">
        <div class="tool_modal_center_view">
            <div class="tool_modal_header">
                <button id="client_secret_modal_x_close" class="tool_modal_close tool_modal_x_close" type="button" data-externalizedAriaLabel="CLOSE">
                    <img src="../../WEB-CONTENT/common/images/modalClose.svg" data-externalizedStringAlt="CLOSE" data-externalizedStringTitle="CLOSE">
                </button>
                <div id="client_secret_modal_title" class="tool_modal_title"></div>
            </div>
            <div class="tool_modal_body">
                <div id="client_secret_modal_description" class="tool_modal_body_description"></div>
                <div class="tool_modal_body_field">
                    <label class="tool_modal_body_field_label" for="gend_client_id" data-externalizedLabel="CLIENT_ID"></label>
                    <div class="tool_modal_body_auth_value readonly">
                        <input id="gend_client_id" type="text" class="tool_modal_body_field_auth_value readonly" readonly tabIndex="0"></input>
                        <button id="gend_client_id_copy" class='tool_modal_field_copy_button' type="button" tabindex="0">
                            <img src='../../WEB-CONTENT/common/images/copy_button.svg' data-externalizedStringAlt="COPY_CLIENT_ID" data-externalizedStringTitle="COPY_CLIENT_ID">
                        </button>
                    </div>    
                </div>
                <div class="tool_modal_body_field">
                    <label class="tool_modal_body_field_label" for="gend_client_secret" data-externalizedLabel="CLIENT_SECRET"></label>
                    <div class="tool_modal_body_auth_value readonly">
                        <input id="gend_client_secret" type="text" class="tool_modal_body_field_auth_value readonly" readonly tabIndex="0"></input>
                        <button id="gend_client_secret_copy" class='tool_modal_field_copy_button' type="button" tabindex="0">
                            <img src='../../WEB-CONTENT/common/images/copy_button.svg' data-externalizedStringAlt="COPY_CLIENT_SECRET" data-externalizedStringTitle="COPY_CLIENT_SECRET">
                        </button>
                    </div>
                </div>
            </div>
            <div class="tool_modal_button_container">
                <div class="tool_modal_button_right_frame">
                    <button id="clientSecretDone" class="tool_modal_close tool_modal_action_button" type="button" data-externalizedString="DONE"></button>
               </div>
            </div>                
        </div>
        <div id="copied_confirmation" data-externalizedString="COPIED_TO_CLIPBOARD" aria-hidden=true></div>
    </div>
    
    <div id="delete_client_modal" class="tool_modal_container tool_modal_alert hidden" tabindex="-1" role="alertdialog" aria-labelledby="delete_client_modal_title" aria-describedby="delete_client_modal_description" aria-modal="true">
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
                <div id="delete_client_modal_title" class="tool_modal_secondary_title" data-externalizedString="MODAL_DELETE_CLIENT_TITLE"></div>
                <div id="delete_client_modal_client_name" class="tool_modal_title"></div>
            </div>
            <div class="tool_modal_body">
                <div id="delete_client_modal_description" class="tool_modal_body_description" data-externalizedString="DELETE_OAUTH_CLIENT_DESC"></div>
            </div>
            <div class="tool_modal_button_container">
                <div class="tool_modal_button_right_frame">
                    <button id="deleteClientCancel" class="tool_modal_close tool_modal_action_button tool_modal_cancel_button" type="button" data-externalizedString="CANCEL"></button>
                    <button id="deleteClientDelete" class="tool_modal_action_button tool_modal_delete_button" type="button" data-externalizedString="DELETE"></button>
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
<script src="${pageContext.request.contextPath}/WEB-CONTENT/clientAdmin/js/apiUtils.js"></script>
<script src="${pageContext.request.contextPath}/WEB-CONTENT/clientAdmin/js/table.js"></script>
<script src="${pageContext.request.contextPath}/WEB-CONTENT/clientAdmin/js/clientInputDialog.js"></script>
<script src="${pageContext.request.contextPath}/WEB-CONTENT/clientAdmin/js/clientAdmin.js"></script>
<script src="${pageContext.request.contextPath}/WEB-CONTENT/clientAdmin/nls/messages.js"></script>

</body>
</html>
