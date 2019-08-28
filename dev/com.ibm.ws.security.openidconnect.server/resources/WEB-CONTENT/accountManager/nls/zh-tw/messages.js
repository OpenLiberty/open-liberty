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
    "CLEAR": "清除搜尋輸入",
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
    "EXPIRES_COL": "到期日",
    "ISSUED_COL": "核發日期",
    "NAME_COL": "名稱",
    "TYPE_COL": "類型",

    // Account Manager
    // 'app-token' and 'app-password' are keywords - don't translate
    // name - the user defined name given the app-password or app-token
    "ACCT_MGR_TITLE": "管理個人記號",
    "ACCT_MGR_DESC": "建立、刪除及重新產生 app-password 和 app-token。",
    "ADD_NEW_AUTHENTICATION": "新增 app-password 或 app-token。",
    "NAME_IDENTIFIER": "名稱：{0}",
    "ADD_NEW_TITLE": "登錄新鑑別",
    "NOT_GENERATED_PLACEHOLDER": "未產生",
    "AUTHENTICAION_GENERATED": "產生的鑑別",
    "GENERATED_APP_PASSWORD": "產生的 app-password",
    "GENERATED_APP_TOKEN": "產生的 app-token",
    "COPY_APP_PASSWORD": "將 app-password 複製到剪貼簿",
    "COPY_APP_TOKEN": "將 app-token 複製到剪貼簿",
    "REGENERATE_APP_PASSWORD": "重新產生 App-Password",
    "REGENERATE_PW_WARNING": "這個動作會改寫現行 app-password。",
    "REGENERATE_PW_PLACEHOLDER": "先前已在 {0}產生密碼",        // 0 - date
    "REGENERATE_APP_TOKEN": "重新產生 App-Token",
    "REGENERATE_TOKEN_WARNING": "這個動作會改寫現行 app-token。",
    "REGENERATE_TOKEN_PLACEHOLDER": "先前已在 {0}產生記號",        // 0 - date
    "DELETE_PW": "刪除這個 app-password",
    "DELETE_TOKEN": "刪除這個 app-token",
    "DELETE_WARNING_PW": "這個動作會移除目前指派的 app-password。",
    "DELETE_WARNING_TOKEN": "這個動作會移除目前指派的 app-token。",
    "REGENERATE_ARIA": "重新產生 {0} 給 {1}",     // 0 - 'app-password' or 'app-token'; 1 - name
    "DELETE_ARIA": "刪除名為 {1} 的 {0}",       // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_GENERATE_FAIL": "產生 {0} 時發生錯誤", // 0 - 'App-Password' or 'App-Token'
    "GENERIC_GENERATE_FAIL_MSG": "產生新的 {0}（名為 {1}）時發生錯誤。",  // 0 - 'app-password' or 'app-token'; 1 - name
    "ERR_NAME": "該名稱已與 {0} 相關聯，或者太長。", // 0 - 'app-password' or 'app-token'
    "GENERIC_DELETE_FAIL": "刪除 {0} 時發生錯誤",     // 0 - 'App-Password' or 'App-Token'
    "GENERIC_DELETE_FAIL_MSG": "刪除名為 {1} 的 {0} 時發生錯誤。",  // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_REGENERATE_FAIL": "重新產生 {0} 時發生錯誤",  // 0 - 'App-Password' or 'App-Token'
    "GENERIC_REGENERATE_FAIL_MSG": "重新產生名為 {1} 的 {0} 時發生錯誤。",  // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_REGENERATE_FAIL_CREATE_MSG": "重新產生名為 {1} 的 {0} 時發生錯誤。{0} 已刪除，但無法重建。", // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_FETCH_FAIL": "擷取鑑別時發生錯誤",
    "GENERIC_FETCH_FAIL_MSG": "無法取得 app-password 或 app-token 現行清單。",
    "GENERIC_NOT_CONFIGURED": "未配置用戶端",
    "GENERIC_NOT_CONFIGURED_MSG": "未配置 appPasswordAllowed 和 appTokenAllowed 用戶端屬性。無法擷取任何資料。",
    "APP_PASSWORD_NOT_CONFIGURED": "未配置 appPasswordAllowed 用戶端屬性。",  // 'appPasswordAllowed' is a config option. Do not translate.
    "APP_TOKEN_NOT_CONFIGURED": "未配置 appTokenAllowed 用戶端屬性。"         // 'appTokenAllowed' is a config option.  Do not translate.
};
