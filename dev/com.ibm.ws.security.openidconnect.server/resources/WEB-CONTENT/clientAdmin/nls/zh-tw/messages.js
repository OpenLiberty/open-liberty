/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
var messages = {
    // Common Strings
    "ADD_NEW": "新增",
    "CANCEL": "取消",
    "CLEAR_SEARCH": "清除搜尋輸入",
    "CLEAR_FILTER": "清除過濾器",
    "CLICK_TO_SORT": "按一下以排序直欄",
    "CLOSE": "關閉",
    "COPY_TO_CLIPBOARD": "複製到剪貼簿",
    "COPIED_TO_CLIPBOARD": "已複製到剪貼簿",
    "DELETE": "刪除",
    "DONE": "完成",
    "EDIT": "編輯",
    "FALSE": "False",
    "GENERATE": "產生",
    "LOADING": "正在載入",
    "LOGOUT": "登出",
    "NEXT_PAGE": "下一頁",
    "NO_RESULTS_FOUND": "找不到結果",
    "PAGES": "{0} / {1} 頁",   // {0} - current page number; {1} - total pages
    "PAGE_SELECT": "選取要檢視的頁碼",
    "PREVIOUS_PAGE": "上一頁",
    "PROCESSING": "處理中",
    "REGENERATE": "重新產生",
    "REGISTER": "登錄",
    "TABLE_FIELD_SORT_ASC": "表格依據 {0} 升冪排序。",   // {0} - column name (ie. 'Name', 'Client ID')
    "TABLE_FIELD_SORT_DESC": "表格依據 {0} 降冪排序。", // {0} - column name (ie. 'Name', 'Client ID')
    "TRUE": "True",
    "TRY_AGAIN": "重試...",
    "UPDATE": "更新",

    // Common Column Names
    "CLIENT_NAME_COL": "用戶端名稱",
    "EXPIRES_COL": "到期日",
    "ISSUED_COL": "核發日期",
    "NAME_COL": "名稱",
    "TYPE_COL": "類型",

    // Client Admin
    "CLIENT_ADMIN_TITLE": "管理 OAuth 用戶端",
    "CLIENT_ADMIN_DESC": "使用此工具來新增及編輯用戶端，以及產生用戶端密碼。",
    "CLIENT_ADMIN_SEARCH_PLACEHOLDER": "依 OAuth 用戶端名稱來過濾",
    "ADD_NEW_CLIENT": "新增 OAuth 用戶端。",
    "CLIENT_NAME": "用戶端名稱",
    "CLIENT_ID": "用戶端 ID",
    "EDIT_ARIA": "編輯 {0} OAuth 用戶端",      // {0} - name
    "DELETE_ARIA": "刪除 {0} OAuth 用戶端",  // {0} - name
    "CLIENT_SECRET": "用戶端密碼",
    "GRANT_TYPES": "授權類型",
    "SCOPE": "範圍",
    "PREAUTHORIZED_SCOPE": "預先授權的範圍（選用）",
    "REDIRECT_URLS": "重新導向 URL（選用）",
    "CLIENT_SECRET_CHECKBOX": "重新產生用戶端密碼",
    "NONE_SELECTED": "全未選",
    "MODAL_EDIT_TITLE": "編輯 OAuth 用戶端",
    "MODAL_REGISTER_TITLE": "登錄新的 OAuth 用戶端",
    "MODAL_SECRET_REGISTER_TITLE": "已儲存 OAuth 登錄",
    "MODAL_SECRET_UPDATED_TITLE": "已更新 OAuth 登錄",
    "MODAL_DELETE_CLIENT_TITLE": "刪除此 OAuth 用戶端",
    "RESET_GRANT_TYPE": "清除所有選取的授權類型。",
    "SELECT_ONE_GRANT_TYPE": "選取至少一個授權類型",
    "SPACE_HELPER_TEXT": "以空格區隔值",
    "REDIRECT_URL_HELPER_TEXT": "以空格區隔絕對重新導向 URL",
    "DELETE_OAUTH_CLIENT_DESC": "此作業會將已登錄的用戶端從用戶端登錄服務中刪除。",
    "REGISTRATION_SAVED": "已產生及指派「用戶端 ID」和「用戶端密碼」。",
    "REGISTRATION_UPDATED": "已產生新的「用戶端密碼」，並已指派給這個用戶端。",
    "COPY_CLIENT_ID": "將用戶端 ID 複製到剪貼簿",
    "COPY_CLIENT_SECRET": "將用戶端密碼複製到剪貼簿",
    "REGISTRATION_UPDATED_NOSECRET": "已更新 {0} OAuth 用戶端。",                 // {0} - client name
    "ERR_MULTISELECT_GRANT_TYPES": "必須至少選取一個授權類型。",
    "ERR_REDIRECT_URIS": "值必須是絕對 URI。",
    "GENERIC_REGISTER_FAIL": "登錄 OAuth 用戶端時發生錯誤",
    "GENERIC_UPDATE_FAIL": "更新 OAuth 用戶端時發生錯誤",
    "GENERIC_DELETE_FAIL": "刪除 OAuth 用戶端時發生錯誤",
    "GENERIC_MISSING_CLIENT": "擷取 OAuth 用戶端時發生錯誤",
    "GENERIC_REGISTER_FAIL_MSG": "登錄 {0} OAuth 用戶端時發生錯誤。",  // {0} - client name
    "GENERIC_UPDATE_FAIL_MSG": "更新 {0} OAuth 用戶端時發生錯誤。",       // {0} - client name
    "GENERIC_DELETE_FAIL_MSG": "刪除 {0} OAuth 用戶端時發生錯誤。",       // {0} - client name
    "GENERIC_MISSING_CLIENT_MSG": "找不到 ID 為 {1} 的 OAuth 用戶端 {0}。",     // {0} - client name; {1} - an ID
    "GENERIC_RETRIEVAL_FAIL_MSG": "擷取 {0} OAuth 用戶端的相關資訊時發生錯誤。", // {0} - client name
    "GENERIC_GET_CLIENTS_FAIL": "擷取 OAuth 用戶端時發生錯誤",
    "GENERIC_GET_CLIENTS_FAIL_MSG": "擷取 OAuth 用戶端清單時發生錯誤。",

    "RESET_SELECTION": "清除所有選取的{0}",     // {0} - field name (ie 'Grant types')
    "NUMBER_SELECTED": "選取的{0}數目",     // {0} - field name
    "OPEN_LIST": "開啟{0}清單。",                   // {0} - field name
    "CLOSE_LIST": "關閉{0}清單。",                 // {0} - field name
    "ENTER_PLACEHOLDER": "輸入值",
    "ADD_VALUE": "新增元素",
    "REMOVE_VALUE": "移除元素",
    "REGENERATE_CLIENT_SECRET": "'*' 保留現有值。空白值產生新的 client_secret。非空白參數值會以新指定的值置換現有的值。",
    "ALL_OPTIONAL": "所有欄位皆為選用"
};
