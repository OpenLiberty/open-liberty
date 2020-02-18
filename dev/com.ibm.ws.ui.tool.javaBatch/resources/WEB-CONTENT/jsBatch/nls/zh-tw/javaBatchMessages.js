/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

define({
      ACCOUNTING_STRING : "帳戶字串",
      SEARCH_RESOURCE_TYPE_ALL : "全部",
      SEARCH : "搜尋",
      JAVA_BATCH_SEARCH_BOX_LABEL : "輸入搜尋準則，作法是選取「新增搜尋準則」按鈕，然後指定一值",
      SUBMITTED : "已提交",
      JMS_QUEUED : "已排入 JMS 佇列",
      JMS_CONSUMED : "JMS 已耗用",
      JOB_PARAMETER : "工作參數",
      DISPATCHED : "已分派",
      FAILED : "已失敗",
      STOPPED : "已停止",
      COMPLETED : "已完成",
      ABANDONED : "已放棄",
      STARTED : "已啟動",
      STARTING : "啟動中",
      STOPPING : "停止中",
      REFRESH : "重新整理",
      INSTANCE_STATE : "實例狀態",
      APPLICATION_NAME : "應用程式名稱",
      APPLICATION: "應用程式",
      INSTANCE_ID : "實例 ID",
      LAST_UPDATE : "最後更新時間",
      LAST_UPDATE_RANGE : "前次更新範圍",
      LAST_UPDATED_TIME : "最後更新時間",
      DASHBOARD_VIEW : "儀表板視圖",
      HOMEPAGE : "首頁",
      JOBLOGS : "工作日誌",
      QUEUED : "已排入佇列",
      ENDED : "已結束",
      ERROR : "錯誤",
      CLOSE : "關閉",
      WARNING : "警告",
      GO_TO_DASHBOARD: "移至儀表板",
      DASHBOARD : "儀表板",
      BATCH_JOB_NAME: "批次工作名稱",
      SUBMITTER: "提交者",
      BATCH_STATUS: "批次狀態",
      EXECUTION_ID: "工作執行 ID",
      EXIT_STATUS: "結束狀態",
      CREATE_TIME: "建立時間",
      START_TIME: "開始時間",
      END_TIME: "結束時間",
      SERVER: "伺服器",
      SERVER_NAME: "伺服器名稱",
      SERVER_USER_DIRECTORY: "使用者目錄",
      SERVERS_USER_DIRECTORY: "伺服器的使用者目錄",
      HOST: "主機",
      NAME: "名稱",
      JOB_PARAMETERS: "工作參數",
      JES_JOB_NAME: "JES 工作名稱",
      JES_JOB_ID: "JES 工作 ID",
      ACTIONS: "動作",
      VIEW_LOG_FILE: "檢視日誌檔",
      STEP_NAME: "步驟名稱",
      ID: "ID",
      PARTITION_ID: "分割區 {0}",                               // Partition ID number
      VIEW_EXECUTION_DETAILS: "檢視工作執行 {0} 的詳細資料",    // Job Execution ID number
      PARENT_DETAILS: "母項資訊詳細資料",
      TIMES: "時間",      // Heading on section referencing create, start, and end timestamps
      STATUS: "狀態",
      SEARCH_ON: "選取以便在 {1} {0} 中過濾",                    // {0} Value, {1} Column name
      SEARCH_PILL_INPUT : "輸入搜尋準則。",
      BREADCRUMB_JOB_INSTANCE : "工作實例 {0}",                // Job Instance ID
      BREADCRUMB_JOB_EXECUTION : "工作執行 {0}",              // Job Execution ID
      BREADCRUMB_JOB_LOG : "工作日誌 {0}",
      BATCH_SEARCH_CRITERIA_INVALID : "搜尋準則無效。",
      ERROR_CANNOT_HAVE_MULTIPLE_SEARCHBY : "搜尋準則不能包含具有「{0}」多個參數的過濾器。", // {0} will be another translated message key like SUBMITTER and BATCH_JOB_NAME

      INSTANCES_TABLE_IDENTIFIER: "工作實例表格",
      EXECUTIONS_TABLE_IDENTIFIER: "工作執行表格",
      STEPS_DETAILS_TABLE_IDENTIFIER: "步驟明細表",
      LOADING_VIEW : "頁面目前正在載入資訊",
      LOADING_VIEW_TITLE : "載入視圖",
      LOADING_GRID : "等待從伺服器傳回搜尋結果",
      PAGENUMBER : "頁碼",
      SELECT_QUERY_SIZE: "選取查詢大小",
      LINK_EXPLORE_HOST: "選取，以在「探索」工具中檢視主機 {0} 上的詳細資料。",      // Host name
      LINK_EXPLORE_SERVER: "選取，以在「探索」工具中檢視伺服器 {0} 上的詳細資料。",  // Server name

      //ACTIONS
      RESTART: "重新啟動",
      STOP: "停止",
      PURGE: "清除",
      OK_BUTTON_LABEL: "確定",
      INSTANCE_ACTIONS_BUTTON_LABEL: "工作實例 {0} 的動作",     // Job Instance ID number
      INSTANCE_ACTIONS_MENU_LABEL:  "工作實例的動作功能表",

      RESTART_INSTANCE_MESSAGE: "您想重新啟動與工作實例 {0} 相關聯之最近一次的工作執行嗎？",     // Job Instance ID number
      STOP_INSTANCE_MESSAGE: "您想停止與工作實例 {0} 相關聯之最近一次的工作執行嗎？",           // Job Instance ID number
      PURGE_INSTANCE_MESSAGE: "您想清除與工作實例 {0} 相關聯的所有資料庫項目和工作日誌嗎？",     // Job Instance ID number
      PURGE_JOBSTORE_ONLY: "僅清除工作儲存庫",

      RESTART_INST_ERROR_MESSAGE: "重新啟動要求失敗。",
      STOP_INST_ERROR_MESSAGE: "停止要求失敗。",
      PURGE_INST_ERROR_MESSAGE: "清除要求失敗。",
      ACTION_REQUEST_ERROR_MESSAGE: "動作要求失敗，狀態碼：{0}。URL：{1}",  // Status Code number, URL string

      // RESTART JOB WITH PARAMETERS DIALOG
      REUSE_PARMS_TOGGLE_LABEL: "重複使用前次執行中的參數",
      JOB_PARAMETERS_EMPTY: "當未選取 '{0}' 時，請使用此區域來輸入工作參數。",   // 0 - Checkbox label - REUSE_PARMS_TOGGLE_LABEL
      JOB_PARAMETER_NAME: "參數名稱",
      JOB_PARAMETER_VALUE: "參數值",
      PARM_NAME_COLUMN_HEADER: "參數",
      PARM_VALUE_COLUMN_HEADER: "值",
      PARM_ADD_ICON_TITLE: "新增參數",
      PARM_REMOVE_ICON_TITLE: "移除參數",
      PARMS_ENTRY_ERROR: "需要參數名稱。",
      JOB_PARAMETER_CREATE: "選取 {0} 來新增參數，以便在下次執行這個工作實例時使用。",  // 0 - Button label
      JOB_PARAMETER_CREATE_BUTTON: "表格標頭中的新增參數按鈕。",

      //JOB LOGS PAGE MESSAGES
      JOBLOGS_LOG_CONTENT : "工作日誌內容",
      FILE_DOWNLOAD : "檔案下載",
      DOWNLOAD_DIALOG_DESCRIPTION : "您想下載日誌檔嗎？",
      INCLUDE_ALL_LOGS : "包含該項工作執行的所有日誌檔",
      LOGS_NAVIGATION_BAR : "工作日誌導覽列",
      DOWNLOAD : "下載",
      LOG_TOP : "日誌頂端",
      LOG_END : "日誌尾端",
      PREVIOUS_PAGE : "上一頁",
      NEXT_PAGE : "下一頁",
      DOWNLOAD_ARIA : "下載檔案",

      //Error messages for popups
      REST_CALL_FAILED : "為提取資料所發出的呼叫失敗。",
      NO_JOB_EXECUTION_URL : "URL 中未提供任何工作執行號碼，或是實例沒有任何工作執行日誌可顯示",
      NO_VIEW : "URL 錯誤：不存在這類視圖。",
      WRONG_TOOL_ID : "URL 查詢字串的開頭不是工具 ID {0}，而是 {1}。",   // {0} and {1} are both Strings
      URL_NO_LOGS : "URL 錯誤：日誌不存在。",
      NOT_A_NUMBER : "URL 錯誤：{0} 必須是數字。",                                                // {0} is a field name
      PARAMETER_REPETITION : "URL 錯誤：{0} 在參數中只能存在一次。",                   // {0} is a field name
      URL_PAGE_PARAMS_ERROR : "URL 錯誤：頁面參數超出範圍。",
      INVALID_PARAMETER : "URL 錯誤：{0} 不是有效的參數。",                                       // {0} is a String
      URL_MULTIPLE_ATTRIBUTES : "URL 錯誤：URL 可以指定工作執行或工作實例，但不能同時指定兩者。",
      MISSING_EXECUTION_ID_PARAM : "遺漏必要的執行 ID 參數。",
      PERSISTENCE_CONFIGURATION_REQUIRED : "需要配置 Java 批次持續性資料庫才能使用「Java 批次」工具。",
      IGNORED_SEARCH_CRITERIA : "結果中會忽略下列的過濾準則：{0}",

      GRIDX_SUMMARY_TEXT : "顯示最新的 ${0} 工作實例"

});

