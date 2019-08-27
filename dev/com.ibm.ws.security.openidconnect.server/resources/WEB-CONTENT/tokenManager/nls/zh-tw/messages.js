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
    "TABLE_BATCH_BAR": "表格動作列",
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

    // Token Manager
    // 'app-token' and 'app-password' are keywords - don't translate
    // name - user defined name given to the app-password/app-token; user id - user's login name
    "TOKEN_MGR_TITLE": "刪除記號",
    "TOKEN_MGR_DESC": "刪除指定使用者的 app-password 和 app-token。",
    "TOKEN_MGR_SEARCH_PLACEHOLDER": "輸入使用者 ID",
    "TABLE_FILLED_WITH": "已更新表格以顯示屬於 {1} 的 {0} 個鑑別。",  // 0 - number of entries in table; 1 - user id
    "DELETE_SELECTED": "刪除選取的 app-password 和 app-token。",
    "DELETE_ARIA": "刪除名為 {1} 的 {0}",         // 0 - 'app-password' or 'app-token'; 1 - name
    "DELETE_PW": "刪除這個 app-password",
    "DELETE_TOKEN": "刪除這個 app-token",
    "DELETE_FOR_USERID": "{1} 的 {0}",                // 0 - name; 1 - user id
    "DELETE_WARNING_PW": "這個動作會移除目前指派的 app-password。",
    "DELETE_WARNING_TOKEN": "這個動作會移除目前指派的 app-token。",
    "DELETE_MANY": "刪除 App-Password/App-Token",
    "DELETE_MANY_FOR": "指派給 {0}",              // 0 - user id
    "DELETE_ONE_MESSAGE": "這個動作會刪除選取的 app-password/app-token。",
    "DELETE_MANY_MESSAGE": "這個動作會刪除 {0} 個選取的 app-password/app-token。",  // 0 - number
    "DELETE_ALL_MESSAGE": "這個動作會刪除屬於 {0} 的所有 app-password/app-token。", // 0 - user id
    "DELETE_NONE": "選取以進行刪除",
    "DELETE_NONE_MESSAGE": "選取勾選框，指出應刪除哪些 app-password 或 app-token。",
    "SINGLE_ITEM_SELECTED": "選取了 1 個項目",
    "ITEMS_SELECTED": "選取了 {0} 個項目",            // 0 - number
    "SELECT_ALL_AUTHS": "選取這個使用者的所有 app-password 和 app-token。",
    "SELECT_SPECIFIC": "選取 {0}（名為 {1}）來刪除。",  // 0 - 'app-password' or 'app-token; 1 - name
    "NO_QUERY": "正在尋找嗎？請輸入使用者 ID，以檢視其 app-password 和 app-token。",
    "GENERIC_FETCH_FAIL": "擷取 {0} 時發生錯誤",      // 0 - 'App-Passwords' or 'App-Tokens'
    "GENERIC_FETCH_FAIL_MSG": "無法取得屬於 {1} 的 {0} 清單。", // 0 - 'app-passwords' or 'app-tokens; 1 - user id
    "GENERIC_DELETE_FAIL": "刪除 {0} 時發生錯誤",       // 0 - 'App-Password' or 'App-Token'
    "GENERIC_DELETE_FAIL_MSG": "刪除名為 {1} 的 {0} 時發生錯誤。",  // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_DELETE_ALL_FAIL_MSG": "刪除 {1} 的 {0} 時發生錯誤。",     // 0 - 'app-passwords' or 'app-tokens'; 1 - user id
    "GENERIC_DELETE_FAIL_NOTYPES": "刪除時發生錯誤",
    "GENERIC_DELETE_FAIL_NOTYPES_ONE_MSG": "刪除下列 app-password 或 app-token 時發生錯誤：",
    "GENERIC_DELETE_FAIL_NOTYPES_MSG": "刪除下列 {0} 個 app-password 和 app-token 時發生錯誤：",  // 0 - number
    "IDENTIFY_AUTH": "{0} {1}",   // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_FETCH_ALL_FAIL": "擷取鑑別時發生錯誤",
    "GENERIC_FETCH_ALL_FAIL_MSG": "無法取得屬於 {0} 的 app-password 和 app-token 清單。",   // 0 - user id
    "GENERIC_NOT_CONFIGURED": "未配置用戶端",
    "GENERIC_NOT_CONFIGURED_MSG": "未配置 appPasswordAllowed 和 appTokenAllowed 用戶端屬性。無法擷取任何資料。"
};
