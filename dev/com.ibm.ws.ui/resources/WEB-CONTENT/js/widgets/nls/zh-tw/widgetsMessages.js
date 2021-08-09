/*******************************************************************************
 * Copyright (c) 2016, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

define({
    LIBERTY_HEADER_TITLE: "Liberty 管理中心",
    LIBERTY_HEADER_PROFILE: "喜好設定",
    LIBERTY_HEADER_LOGOUT: "登出",
    LIBERTY_HEADER_LOGOUT_USERNAME: "登出 {0}",
    TOOLBOX_BANNER_LABEL: "{0} 橫幅",  // TOOLBOX_TITLE
    TOOLBOX_TITLE: "工具箱",
    TOOLBOX_TITLE_LOADING_TOOL: "正在載入工具...",
    TOOLBOX_TITLE_EDIT: "編輯工具箱",
    TOOLBOX_EDIT: "編輯",
    TOOLBOX_DONE: "完成",
    TOOLBOX_SEARCH: "過濾器",
    TOOLBOX_CLEAR_SEARCH: "清除過濾器準則",
    TOOLBOX_END_SEARCH: "關閉過濾器",
    TOOLBOX_ADD_TOOL: "新增工具",
    TOOLBOX_ADD_CATALOG_TOOL: "新增工具",
    TOOLBOX_ADD_BOOKMARK: "新增書籤",
    TOOLBOX_REMOVE_TITLE: "移除工具 {0}",
    TOOLBOX_REMOVE_TITLE_NO_TOOL_NAME: "移除工具",
    TOOLBOX_REMOVE_MESSAGE: "您確定要移除 {0} 嗎？",
    TOOLBOX_BUTTON_REMOVE: "移除",
    TOOLBOX_BUTTON_OK: "確定",
    TOOLBOX_BUTTON_GO_TO: "跳至工具箱",
    TOOLBOX_BUTTON_CANCEL: "取消",
    TOOLBOX_BUTTON_BGTASK: "背景作業",
    TOOLBOX_BUTTON_BACK: "返回",
    TOOLBOX_BUTTON_USER: "使用者",
    TOOLBOX_ADDTOOL_ERROR_MESSAGE: "新增工具 {0} 時發生錯誤：{1}",
    TOOLBOX_REMOVETOOL_ERROR_MESSAGE: "移除工具 {0} 時發生錯誤：{1}",
    TOOLBOX_GET_ERROR_MESSAGE: "擷取工具箱中的工具時發生錯誤：{0}",
    TOOLCATALOG_TITLE: "工具型錄",
    TOOLCATALOG_ADDTOOL_TITLE: "新增工具",
    TOOLCATALOG_ADDTOOL_MESSAGE: "您確定要將工具 {0} 新增至工具箱嗎？",
    TOOLCATALOG_BUTTON_ADD: "新增",
    TOOL_FRAME_TITLE: "工具頁框",
    TOOL_DELETE_TITLE: "刪除 {0}",
    TOOL_ADD_TITLE: "新增 {0}",
    TOOL_ADDED_TITLE: "已新增 {0}",
    TOOL_LAUNCH_ERROR_MESSAGE_TITLE: "找不到工具",
    TOOL_LAUNCH_ERROR_MESSAGE: "所要求的工具並未啟動，因該工具未編列在型錄中。",
    LIBERTY_UI_ERROR_MESSAGE_TITLE: "錯誤",
    LIBERTY_UI_WARNING_MESSAGE_TITLE: "警告",
    LIBERTY_UI_INFO_MESSAGE_TITLE: "資訊",
    LIBERTY_UI_CATALOG_GET_ERROR: "取得型錄時發生錯誤：{0}",
    LIBERTY_UI_CATALOG_GET_TOOL_ERROR: "從型錄取得工具 {0} 時發生錯誤：{1}",
    PREFERENCES_TITLE: "喜好設定",
    PREFERENCES_SECTION_TITLE: "喜好設定",
    PREFERENCES_ENABLE_BIDI: "啟用雙向支援",
    PREFERENCES_BIDI_TEXTDIR: "文字方向",
    PREFERENCES_BIDI_TEXTDIR_LTR: "從左到右",
    PREFERENCES_BIDI_TEXTDIR_RTL: "從右到左",
    PREFERENCES_BIDI_TEXTDIR_CONTEXTUAL: "環境定義",
    PREFERENCES_SET_ERROR_MESSAGE: "在工具箱中設定使用者喜好設定時發生錯誤：{0}",
    BGTASKS_PAGE_LABEL: "背景作業",
    BGTASKS_DEPLOYMENT_INSTALLATION_POPUP: "部署安裝 {0}", // {0} is the token number associated with the deployment
    BGTASKS_DEPLOYMENT_INSTALLATION: "部署安裝 {0} - {1}", // {0} is the token number associated with the deployment, {1} is the server name
    BGTASKS_STATUS_RUNNING: "執行中",
    BGTASKS_STATUS_FAILED: "失敗",
    BGTASKS_STATUS_SUCCEEDED: "已完成", 
    BGTASKS_STATUS_WARNING: "局部成功",
    BGTASKS_STATUS_PENDING: "擱置",
    BGTASKS_INFO_DIALOG_TITLE: "詳細資料",
    //BGTASKS_INFO_DIALOG_DESC: "Description:",
    BGTASKS_INFO_DIALOG_STDOUT: "標準輸出：",
    BGTASKS_INFO_DIALOG_STDERR: "標準錯誤：",
    BGTASKS_INFO_DIALOG_EXCEPTION: "異常狀況：",
    //BGTASKS_INFO_DIALOG_RETURN_CODE: "Return code:",
    BGTASKS_INFO_DIALOG_RESULT: "結果：",
    BGTASKS_INFO_DIALOG_DEPLOYED_ARTIFACT_NAME: "伺服器名稱：",
    BGTASKS_INFO_DIALOG_DEPLOYED_USER_DIR: "使用者目錄：",
    BGTASKS_POPUP_RUNNING_TASK_TITLE: "作用中的背景作業",
    BGTASKS_POPUP_RUNNING_TASK_NONE: "無",
    BGTASKS_POPUP_RUNNING_TASK_NO_ACTIVE_TASK: "無作用中的背景作業",
    BGTASKS_DISPLAY_BUTTON: "作業詳細資料和歷程",
    BGTASKS_EXPAND: "展開區段",
    BGTASKS_COLLAPSE: "收合區段",
    PROFILE_MENU_HELP_TITLE: "說明",
    DETAILS_DESCRIPTION: "說明",
    DETAILS_OVERVIEW: "概觀",
    DETAILS_OTHERVERSIONS: "其他版本",
    DETAILS_VERSION: "版本：{0}",
    DETAILS_UPDATED: "已更新：{0}",
    DETAILS_NOTOPTIMIZED: "未針對現行裝置最佳化。",
    DETAILS_ADDBUTTON: "新增至我的工具箱",
    DETAILS_OPEN: "開啟",
    DETAILS_CATEGORY: "種類 {0}",
    DETAILS_ADDCONFIRM: "已順利將工具 {0} 新增至工具箱。",
    CONFIRM_DIALOG_HELP: "說明",
    YES_BUTTON_LABEL: "{0} 是",  // insert is dialog title
    NO_BUTTON_LABEL: "{0} 否",  // insert is dialog title

    YES: "是",
    NO: "否",

    TOOL_OIDC_ACCESS_DENIED: "使用者不具備有權完成此要求的角色。",
    TOOL_OIDC_GENERIC_ERROR: "發生錯誤。請檢閱日誌中的錯誤，以取得相關資訊。",
    TOOL_DISABLE: "使用者無權使用此工具。只有具備「管理者」角色的使用者才有權使用此工具。" 
});
