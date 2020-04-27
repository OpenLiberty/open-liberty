/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
define({
    EXPLORER : "瀏覽器",
    EXPLORE : "探索",
    DASHBOARD : "儀表板",
    DASHBOARD_VIEW_ALL_APPS : "檢視所有應用程式",
    DASHBOARD_VIEW_ALL_SERVERS : "檢視所有伺服器",
    DASHBOARD_VIEW_ALL_CLUSTERS : "檢視所有叢集",
    DASHBOARD_VIEW_ALL_HOSTS : "檢視所有主機",
    DASHBOARD_VIEW_ALL_RUNTIMES : "檢視所有執行時期",
    SEARCH : "搜尋",
    SEARCH_RECENT : "最近的搜尋",
    SEARCH_RESOURCES : "搜尋資源",
    SEARCH_RESULTS : "搜尋結果",
    SEARCH_NO_RESULTS : "無結果",
    SEARCH_NO_MATCHES : "無相符項",
    SEARCH_TEXT_INVALID : "搜尋文字包含無效的字元",
    SEARCH_CRITERIA_INVALID : "搜尋準則無效。",
    SEARCH_CRITERIA_INVALID_COMBO :"{0} 與 {1} 一起指定時無效。",
    SEARCH_CRITERIA_INVALID_DUPLICATE : "{0} 只能指定一次。",
    SEARCH_TEXT_MISSING : "必須提供搜尋文字",
    SEARCH_UNSUPPORT_TYPE_APPONSERVER : "不支援搜尋伺服器上的應用程式標籤。",
    SEARCH_UNSUPPORT_TYPE_APPONCLUSTER : "不支援搜尋叢集上的應用程式標籤。",
    SEARCH_UNSUPPORT : "不支援搜尋準則。",
    SEARCH_SWITCH_VIEW : "切換視圖",
    FILTERS : "過濾器",
    DEPLOY_SERVER_PACKAGE : "部署伺服器套件",
    MEMBER_OF : "成員隸屬",
    N_CLUSTERS: "{0} 個叢集 ...",

    INSTANCE : "實例",
    INSTANCES : "實例",
    APPLICATION : "應用程式",
    APPLICATIONS : "應用程式",
    SERVER : "伺服器",
    SERVERS : "伺服器",
    CLUSTER : "叢集",
    CLUSTERS : "叢集",
    CLUSTER_NAME : "叢集名稱：",
    CLUSTER_STATUS : "叢集狀態：",
    APPLICATION_NAME : "應用程式名稱：",
    APPLICATION_STATE : "應用程式狀態：",
    HOST : "主機",
    HOSTS : "主機",
    RUNTIME : "執行時期",
    RUNTIMES : "執行時期",
    PATH : "路徑",
    CONTROLLER : "控制器",
    CONTROLLERS : "控制器",
    OVERVIEW : "概觀",
    CONFIGURE : "配置",

    SEARCH_RESOURCE_TYPE: "類型", // Search by resource types
    SEARCH_RESOURCE_STATE: "狀態", // Search by resource types
    SEARCH_RESOURCE_TYPE_ALL: "全部", // Search all resource types
    SEARCH_RESOURCE_NAME: "名稱", // Search by resource name
    SEARCH_RESOURCE_TAG: "標籤", // Search by resource tag
    SEARCH_RESOURCE_CONTAINER: "儲存器", // Search by container type
    SEARCH_RESOURCE_CONTAINER_DOCKER: "Docker", // Search by container type Docker
    SEARCH_RESOURCE_CONTAINER_NONE: "無", // Search by container type none
    SEARCH_RESOURCE_RUNTIMETYPE: "執行時期類型", // Search by runtime type
    SEARCH_RESOURCE_OWNER: "擁有者", // Search by owner
    SEARCH_RESOURCE_CONTACT: "聯絡人", // Search by contact
    SEARCH_RESOURCE_NOTE: "附註", // Search by note

    GRID_HEADER_USERDIR : "使用者目錄",
    GRID_HEADER_NAME : "名稱",
    GRID_LOCATION_NAME : "位置",
    GRID_ACTIONS : "網格動作",
    GRID_ACTIONS_LABEL : "{0} 網格動作",  // name of the grid
    APPONSERVER_LOCATION_NAME : "{1} 上的 {0} ({2})", // server on host (/path)

    STATS : "監視",
    STATS_ALL : "全部",
    STATS_VALUE : "值：{0}",
    CONNECTION_IN_USE_STATS : "{0} 使用中 = {1} 受管理的 - {2} 可用的",
    CONNECTION_IN_USE_STATS_VALUE : "值：{0} 使用中 = {1} 受管理的 - {2} 可用的",
    DATA_SOURCE : "資料來源：{0}",
    STATS_DISPLAY_LEGEND : "顯示圖註",
    STATS_HIDE_LEGEND : "隱藏圖註",
    STATS_VIEW_DATA : "檢視圖表資料",
    STATS_VIEW_DATA_TIMESTAMP : "時間戳記",
    STATS_ACTION_MENU : "{0} 動作功能表",
    STATS_SHOW_HIDE : "新增資源度量",
    STATS_SHOW_HIDE_SUMMARY : "新增摘要度量",
    STATS_SHOW_HIDE_TRAFFIC : "新增資料流量度量",
    STATS_SHOW_HIDE_PERFORMANCE : "新增效能度量",
    STATS_SHOW_HIDE_AVAILABILITY : "新增可用性度量",
    STATS_SHOW_HIDE_ALERT : "新增警示度量",
    STATS_SHOW_HIDE_LIST_BUTTON : "顯示或隱藏資源度量清單",
    STATS_SHOW_HIDE_BUTTON_TITLE : "編輯圖表",
    STATS_SHOW_HIDE_CONFIRM : "儲存",
    STATS_SHOW_HIDE_CANCEL : "取消",
    STATS_SHOW_HIDE_DONE : "完成",
    STATS_DELETE_GRAPH : "刪除圖表",
    STATS_ADD_CHART_LABEL : "新增圖表至視圖",
    STATS_JVM_TITLE : "JVM",
    STATS_JVM_BUTTON_LABEL : "新增所有 JVM 圖表至視圖",
    STATS_HEAP_TITLE : "已用的資料堆記憶體",
    STATS_HEAP_USED : "已使用：{0} MB",
    STATS_HEAP_COMMITTED : "已確定：{0} MB",
    STATS_HEAP_MAX : "上限：{0} MB",
    STATS_HEAP_X_TIME : "時間",
    STATS_HEAP_Y_MB : "已用的 MB",
    STATS_HEAP_Y_MB_LABEL : "{0} MB",
    STATS_CLASSES_TITLE : "已載入的類別",
    STATS_CLASSES_LOADED : "已載入：{0}",
    STATS_CLASSES_UNLOADED : "已卸載：{0}",
    STATS_CLASSES_TOTAL : "總計：{0}",
    STATS_CLASSES_Y_TOTAL : "已載入的類別",
    STATS_PROCESSCPU_TITLE : "CPU 使用率",
    STATS_PROCESSCPU_USAGE : "CPU 使用率：{0}%",
    STATS_PROCESSCPU_Y_PERCENT : "CPU 百分比",
    STATS_PROCESSCPU_Y_PCT_LABEL : "{0}%",
    STATS_THREADS_TITLE : "作用中的 JVM 執行緒",
    STATS_LIVE_MSG_INIT : "顯示現用資料",
    STATS_LIVE_MSG :"此圖表無歷程資料。將繼續顯示最近 10 分鐘的資料。",
    STATS_THREADS_ACTIVE : "現用：{0}",
    STATS_THREADS_PEAK : "尖峰：{0}",
    STATS_THREADS_TOTAL : "總計：{0}",
    STATS_THREADS_Y_THREADS : "執行緒",
    STATS_TP_POOL_SIZE : "儲存區大小",
    STATS_JAXWS_TITLE : "JAX-WS Web 服務",
    STATS_JAXWS_BUTTON_LABEL : "新增所有「JAX-WS Web 服務」圖表至視圖",
    STATS_JW_AVG_RESP_TIME : "平均回應時間",
    STATS_JW_AVG_INVCOUNT : "平均呼叫計數",
    STATS_JW_TOTAL_FAULTS : "執行時期錯誤數總計",
    STATS_LA_RESOURCE_CONFIG_LABEL : "選取資源...",
    STATS_LA_RESOURCE_CONFIG_LABEL_NUM : "{0} 個資源",
    STATS_LA_RESOURCE_CONFIG_LABEL_NUM_1 : "1 個資源",
    STATS_LA_RESOURCE_CONFIG_SELECT_ONE : "您必須至少選取一個資源。",
    STATS_LA_RESOURCE_CONFIG_NO_DATA : "選取的時間範圍沒有可用的資料。",
    STATS_ACCESS_LOG_TITLE : "存取日誌",
    STATS_ACCESS_LOG_BUTTON_LABEL : "新增所有「存取日誌」圖表至視圖",
    STATS_ACCESS_LOG_GRAPH : "存取日誌訊息計數",
    STATS_ACCESS_LOG_SUMMARY : "存取日誌摘要",
    STATS_ACCESS_LOG_TABLE : "存取日誌訊息清單",
    STATS_MESSAGES_TITLE : "訊息和追蹤",
    STATS_MESSAGES_BUTTON_LABEL : "新增所有「訊息和追蹤」圖表至視圖",
    STATS_MESSAGES_GRAPH : "日誌訊息計數",
    STATS_MESSAGES_TABLE : "日誌訊息清單",
    STATS_FFDC_GRAPH : "FFDC 計數",
    STATS_FFDC_TABLE : "FFDC 清單",
    STATS_TRACE_LOG_GRAPH : "追蹤訊息計數",
    STATS_TRACE_LOG_TABLE : "追蹤訊息清單",
    STATS_THREAD_POOL_TITLE : "執行緒儲存區",
    STATS_THREAD_POOL_BUTTON_LABEL : "新增所有「執行緒儲存區」圖表至視圖",
    STATS_THREADPOOL_TITLE : "作用中的 Liberty 執行緒",
    STATS_THREADPOOL_SIZE : "儲存區大小：{0}",
    STATS_THREADPOOL_ACTIVE : "作用中：{0}",
    STATS_THREADPOOL_TOTAL : "總計：{0}",
    STATS_THREADPOOL_Y_ACTIVE : "作用中執行緒",
    STATS_SESSION_MGMT_TITLE : "階段作業",
    STATS_SESSION_MGMT_BUTTON_LABEL : "新增所有「階段作業」圖表至視圖",
    STATS_SESSION_CONFIG_LABEL : "選取階段作業...",
    STATS_SESSION_CONFIG_LABEL_NUM  : "{0} 個階段作業",
    STATS_SESSION_CONFIG_LABEL_NUM_1  : "1 個階段作業",
    STATS_SESSION_CONFIG_SELECT_ONE : "您必須至少選取一個階段作業。",
    STATS_SESSION_TITLE : "作用中的階段作業",
    STATS_SESSION_Y_ACTIVE : "作用中的階段作業",
    STATS_SESSION_LIVE_LABEL : "現用計數：{0}",
    STATS_SESSION_CREATE_LABEL : "建立計數：{0}",
    STATS_SESSION_INV_LABEL : "失效計數：{0}",
    STATS_SESSION_INV_TIME_LABEL : "失效計數（依逾時）：{0}",
    STATS_WEBCONTAINER_TITLE : "Web 應用程式",
    STATS_WEBCONTAINER_BUTTON_LABEL : "新增所有「Web 應用程式」圖表至視圖",
    STATS_SERVLET_CONFIG_LABEL : "選取 Servlet...",
    STATS_SERVLET_CONFIG_LABEL_NUM : "{0} 個 Servlet",
    STATS_SERVLET_CONFIG_LABEL_NUM_1 : "1 個 Servlet",
    STATS_SERVLET_CONFIG_SELECT_ONE : "您必須至少選取一個 Servlet。",
    STATS_SERVLET_REQUEST_COUNT_TITLE : "要求計數",
    STATS_SERVLET_REQUEST_COUNT_Y_AXIS : "要求計數",
    STATS_SERVLET_RESPONSE_COUNT_TITLE : "回應計數",
    STATS_SERVLET_RESPONSE_COUNT_Y_AXIS : "回應計數",
    STATS_SERVLET_RESPONSE_MEAN_TITLE : "平均回應時間（奈秒）",
    STATS_SERVLET_RESPONSE_MEAN_Y_AXIS : "回應時間（奈秒）",
    STATS_CONN_POOL_TITLE : "連線儲存區",
    STATS_CONN_POOL_BUTTON_LABEL : "新增所有「連線儲存區」圖表至視圖",
    STATS_CONN_POOL_CONFIG_LABEL : "選取資料來源...",
    STATS_CONN_POOL_CONFIG_LABEL_NUM : "{0} 個資料來源",
    STATS_CONN_POOL_CONFIG_LABEL_NUM_1 : "1 個資料來源",
    STATS_CONN_POOL_CONFIG_SELECT_ONE : "您必須至少選取一個資料來源。",
    STATS_CONNECT_IN_USE_TITLE : "使用中的連線",
    STATS_CONNECT_USED_COUNT_Y_AXIS : "連線",
    STATS_CONNECT_IN_USE_LABEL : "使用中：{0}",
    STATS_CONNECT_USED_USED_LABEL : "已用：{0}",
    STATS_CONNECT_USED_FREE_LABEL : "可用：{0}",
    STATS_CONNECT_USED_CREATE_LABEL : "已建立：{0}",
    STATS_CONNECT_USED_DESTROY_LABEL : "已毀損：{0}",
    STATS_CONNECT_WAIT_TIME_TITLE : "平均等待時間（毫秒）",
    STATS_CONNECT_WAIT_TIME_Y_AXIS : "等待時間（毫秒）",
    STATS_TIME_ALL : "全部",
    STATS_TIME_1YEAR : "1 年",
    STATS_TIME_1MONTH : "1 個月",
    STATS_TIME_1WEEK : "1 週",
    STATS_TIME_1DAY : "1 天",
    STATS_TIME_1HOUR : "1 小時",
    STATS_TIME_10MINUTES : "10 分鐘",
    STATS_TIME_5MINUTES : "5 分鐘",
    STATS_TIME_1MINUTE : "1 分鐘",
    STATS_PERSPECTIVE_SUMMARY : "摘要",
    STATS_PERSPECTIVE_TRAFFIC : "資料流量",
    STATS_PERSPECTIVE_TRAFFIC_JVM : "JVM 資料流量",
    STATS_PERSPECTIVE_TRAFFIC_CONN : "連線資料流量",
    STATS_PERSPECTIVE_TRAFFIC_LAACCESS : "存取日誌資料流量",
    STATS_PERSPECTIVE_PROBLEM : "問題",
    STATS_PERSPECTIVE_PERFORMANCE : "效能",
    STATS_PERSPECTIVE_PERFORMANCE_JVM : "JVM 效能",
    STATS_PERSPECTIVE_PERFORMANCE_CONN : "連線效能",
    STATS_PERSPECTIVE_ALERT : "警示分析",
    STATS_PERSPECTIVE_ALERT_LAACCESS : "存取日誌警示",
    STATS_PERSPECTIVE_ALERT_LAMSGS : "訊息和追蹤日誌警示",
    STATS_PERSPECTIVE_AVAILABILITY : "可用性",

    STATS_DISPLAY_TIME_LAST_MINUTE_LABEL : "前 1 分鐘",
    STATS_DISPLAY_TIME_LAST_5_MINUTES_LABEL : "前 5 分鐘",
    STATS_DISPLAY_TIME_LAST_10_MINUTES_LABEL : "前 10 分鐘",
    STATS_DISPLAY_TIME_LAST_HOUR_LABEL : "前一小時",
    STATS_DISPLAY_TIME_LAST_DAY_LABEL : "前一天",
    STATS_DISPLAY_TIME_LAST_WEEK_LABEL : "上週",
    STATS_DISPLAY_TIME_LAST_MONTH_LABEL : "上個月",
    STATS_DISPLAY_TIME_LAST_YEAR_LABEL : "去年",

    STATS_DISPLAY_CUSTOM_TIME_LAST_SECOND_LABEL : "前 {0} 秒",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MINUTE_LABEL : "前 {0} 分鐘",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MINUTE_AND_SECOND_LABEL : "前 {0} 分 {1} 秒",
    STATS_DISPLAY_CUSTOM_TIME_LAST_HOUR_LABEL : "前 {0} 小時",
    STATS_DISPLAY_CUSTOM_TIME_LAST_HOUR_AND_MINUTE_LABEL : "前 {0} 小時 {1} 分鐘",
    STATS_DISPLAY_CUSTOM_TIME_LAST_DAY_LABEL : "前 {0} 天",
    STATS_DISPLAY_CUSTOM_TIME_LAST_DAY_AND_HOUR_LABEL : "前 {0} 天 {1} 小時",
    STATS_DISPLAY_CUSTOM_TIME_LAST_WEEK_LABEL : "前 {0} 週",
    STATS_DISPLAY_CUSTOM_TIME_LAST_WEEK_AND_DAY_LABEL : "前 {0} 週 {1} 天",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MONTH_LABEL : "前 {0} 個月",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MONTH_AND_DAY_LABEL : "前 {0} 個月 {1} 天",
    STATS_DISPLAY_CUSTOM_TIME_LAST_YEAR_LABEL : "前 {0} 年",
    STATS_DISPLAY_CUSTOM_TIME_LAST_YEAR_AND_MONTH_LABEL : "前 {0} 年 {1} 個月",

    STATS_LIVE_UPDATE_LABEL: "即時更新中",
    STATS_TIME_SELECTOR_NOW_LABEL: "現在",

    LOG_ANALYTICS_LOG_MESSAGE_TITLE: "日誌訊息",

    AUTOSCALED_APPLICATION : "自動調整的應用程式",
    AUTOSCALED_SERVER : "自動調整的伺服器",
    AUTOSCALED_CLUSTER : "自動調整的叢集",
    AUTOSCALED_POLICY : "自動調整原則",
    AUTOSCALED_POLICY_DISABLED : "自動調整原則已停用",
    AUTOSCALED_NOACTIONS : "自動調整的資源無法使用動作",

    START : "啟動",
    START_CLEAN : "啟動 --clean",
    STARTING : "啟動中",
    STARTED : "已啟動",
    RUNNING : "執行中",
    NUM_RUNNING: "{0} 執行中",
    PARTIALLY_STARTED : "局部啟動",
    PARTIALLY_RUNNING : "部分執行中",
    NOT_STARTED : "未啟動",
    STOP : "停止",
    STOPPING : "停止中",
    STOPPED : "已停止",
    NUM_STOPPED : "{0} 已停止",
    NOT_RUNNING : "不在執行中",
    RESTART : "重新啟動",
    RESTARTING : "重新啟動中",
    RESTARTED : "已重新啟動",
    ALERT : "警示",
    ALERTS : "警示",
    UNKNOWN : "不明",
    NUM_UNKNOWN : "{0} 個不明",
    SELECT : "選取",
    SELECTED : "已選取",
    SELECT_ALL : "全選",
    SELECT_NONE : "全不選",
    DESELECT: "取消選取",
    DESELECT_ALL : "取消全選",
    TOTAL : "總計",
    UTILIZATION : "使用率超過 {0}%", // percent

    ELLIPSIS_ARIA: "展開以顯示其他選項。",
    EXPAND : "展開",
    COLLAPSE: "收合",

    ALL : "全部",
    ALL_APPS : "所有應用程式",
    ALL_SERVERS : "所有伺服器",
    ALL_CLUSTERS : "所有叢集",
    ALL_HOSTS : "所有主機",
    ALL_APP_INSTANCES : "所有應用程式實例",
    ALL_RUNTIMES : "所有執行時期",

    ALL_APPS_RUNNING : "所有應用程式執行中",
    ALL_SERVER_RUNNING : "所有伺服器執行中",
    ALL_CLUSTERS_RUNNING : "所有叢集執行中",
    ALL_APPS_STOPPED : "所有應用程式都已停止",
    ALL_SERVER_STOPPED : "所有伺服器都已停止",
    ALL_CLUSTERS_STOPPED : "所有叢集都已停止",
    ALL_SERVERS_UNKNOWN : "所有伺服器都不明",
    SOME_APPS_RUNNING : "部分應用程式執行中",
    SOME_SERVERS_RUNNING : "部分伺服器執行中",
    SOME_CLUSTERS_RUNNING : "部分叢集執行中",
    NO_APPS_RUNNING : "無應用程式執行中",
    NO_SERVERS_RUNNING : "無伺服器執行中",
    NO_CLUSTERS_RUNNING : "無叢集執行中",

    HOST_WITH_ALL_SERVERS_RUNNING: "所有伺服器都在執行中的主機", // not used anymore since 4Q
    HOST_WITH_SOME_SERVERS_RUNNING: "部分伺服器在執行中的主機",
    HOST_WITH_NO_SERVERS_RUNNING: "沒有伺服器在執行中的主機", // not used anymore since 4Q
    HOST_WITH_ALL_SERVERS_STOPPED: "所有伺服器都已停止的主機",
    HOST_WITH_SERVERS_RUNNING: "伺服器在執行中的主機",

    RUNTIME_WITH_SOME_SERVERS_RUNNING: "部分伺服器正在執行的執行時期",
    RUNTIME_WITH_ALL_SERVERS_STOPPED: "所有伺服器都已停止的執行時期",
    RUNTIME_WITH_SERVERS_RUNNING: "有伺服器正在執行的執行時期",

    START_ALL_APPS : "要啟動所有應用程式嗎？",
    START_ALL_INSTANCES : "要啟動所有應用程式實例嗎？",
    START_ALL_SERVERS : "要啟動所有伺服器嗎？",
    START_ALL_CLUSTERS : "要啟動所有叢集嗎？",
    STOP_ALL_APPS : "要停止所有應用程式嗎？",
    STOPE_ALL_INSTANCES : "要停止所有應用程式實例嗎？",
    STOP_ALL_SERVERS : "要停止所有伺服器嗎？",
    STOP_ALL_CLUSTERS : "要停止所有叢集嗎？",
    RESTART_ALL_APPS : "要重新啟動所有應用程式嗎？",
    RESTART_ALL_INSTANCES : "要重新啟動所有應用程式實例嗎？",
    RESTART_ALL_SERVERS : "要重新啟動所有伺服器嗎？",
    RESTART_ALL_CLUSTERS : "要重新啟動所有叢集嗎？",

    START_INSTANCE : "要啟動應用程式實例嗎？",
    STOP_INSTANCE : "要停止應用程式實例嗎？",
    RESTART_INSTANCE : "要重新啟動應用程式實例嗎？",

    START_SERVER : "要啟動伺服器 {0} 嗎？",
    STOP_SERVER : "要停止伺服器 {0} 嗎？",
    RESTART_SERVER : "要重新啟動伺服器 {0} 嗎？",

    START_ALL_INSTS_OF_APP : "要啟動 {0} 的所有實例嗎？", // application name
    START_APP_ON_SERVER : "要啟動 {1} 上的 {0} 嗎？", // app name, server name
    START_ALL_APPS_WITHIN : "要啟動 {0} 內的所有應用程式嗎？", // resource
    START_ALL_APP_INSTS_WITHIN : "要啟動 {0} 內的所有應用程式實例嗎？", // resource
    START_ALL_SERVERS_WITHIN : "要啟動 {0} 內的所有伺服器嗎？", // resource
    STOP_ALL_INSTS_OF_APP : "要停止 {0} 的所有實例嗎？", // application name
    STOP_APP_ON_SERVER : "要停止 {1} 上的 {0} 嗎？", // app name, server name
    STOP_ALL_APPS_WITHIN : "要停止 {0} 內的所有應用程式嗎？", // resource
    STOP_ALL_APP_INSTS_WITHIN : "要停止 {0} 內的所有應用程式實例嗎？", // resource
    STOP_ALL_SERVERS_WITHIN : "要停止 {0} 內的所有伺服器嗎？", // resource
    RESTART_ALL_INSTS_OF_APP : "要重新啟動 {0} 的所有實例嗎？", // application name
    RESTART_APP_ON_SERVER : "要重新啟動 {1} 上的 {0} 嗎？", // app name, server name
    RESTART_ALL_APPS_WITHIN : "要重新啟動 {0} 內的所有應用程式嗎？", // resource
    RESTART_ALL_APP_INSTS_WITHIN : "要重新啟動 {0} 內的所有應用程式實例嗎？", // resource
    RESTART_ALL_SERVERS_WITHIN : "要重新啟動 {0} 內所有正在執行的伺服器嗎？", // resource

    START_SELECTED_APPS : "要啟動所選應用程式的所有實例嗎？",
    START_SELECTED_INSTANCES : "要啟動選取的應用程式實例嗎？",
    START_SELECTED_SERVERS : "要啟動選取的伺服器嗎？",
    START_SELECTED_SERVERS_LABEL : "啟動選取的伺服器",
    START_SELECTED_CLUSTERS : "要啟動選取的叢集嗎？",
    START_CLEAN_SELECTED_SERVERS : "啟動 -- 要清理選取的伺服器嗎？",
    START_CLEAN_SELECTED_CLUSTERS : "啟動 -- 要清理選取的叢集嗎？",
    STOP_SELECTED_APPS : "要停止所選應用程式的所有實例嗎？",
    STOP_SELECTED_INSTANCES : "要停止選取的應用程式實例嗎？",
    STOP_SELECTED_SERVERS : "要停止選取的伺服器嗎？",
    STOP_SELECTED_CLUSTERS : "要停止選取的叢集嗎？",
    RESTART_SELECTED_APPS : "要重新啟動所選應用程式的所有實例嗎？",
    RESTART_SELECTED_INSTANCES : "要重新啟動選取的應用程式實例嗎？",
    RESTART_SELECTED_SERVERS : "要重新啟動選取的伺服器嗎？",
    RESTART_SELECTED_CLUSTERS : "要重新啟動選取的叢集嗎？",

    START_SERVERS_ON_HOSTS : "要啟動所選主機上的所有伺服器嗎？",
    STOP_SERVERS_ON_HOSTS : "要停止所選主機上的所有伺服器嗎？",
    RESTART_SERVERS_ON_HOSTS : "要重新啟動所選主機上所有正在執行的伺服器嗎？",

    SELECT_APPS_TO_START : "選取已停止的應用程式來啟動。",
    SELECT_APPS_TO_STOP : "選取已啟動的應用程式來停止。",
    SELECT_APPS_TO_RESTART : "選取已啟動的應用程式來重新啟動。",
    SELECT_INSTANCES_TO_START : "選取已停止的應用程式實例來啟動。",
    SELECT_INSTANCES_TO_STOP : "選取已啟動的應用程式實例來停止。",
    SELECT_INSTANCES_TO_RESTART : "選取已啟動的應用程式實例來重新啟動。",
    SELECT_SERVERS_TO_START : "選取已停止的伺服器來啟動。",
    SELECT_SERVERS_TO_STOP : "選取已啟動的伺服器來停止。",
    SELECT_SERVERS_TO_RESTART : "選取已啟動的應伺服器來重新啟動。",
    SELECT_CLUSTERS_TO_START : "選取已停止的叢集來啟動。",
    SELECT_CLUSTERS_TO_STOP : "選取已啟動的叢集來停止。",
    SELECT_CLUSTERS_TO_RESTART : "選取已啟動的叢集來重新啟動。",

    STATUS : "狀態",
    STATE : "狀態：",
    NAME : "名稱：",
    DIRECTORY : "目錄",
    INFORMATION : "資訊",
    DETAILS : "詳細資料",
    ACTIONS : "動作",
    CLOSE : "關閉",
    HIDE : "隱藏",
    SHOW_ACTIONS : "顯示動作",
    SHOW_SERVER_ACTIONS_LABEL : "{0} 伺服器動作",
    SHOW_APP_ACTIONS_LABEL : "{0} 應用程式動作",
    SHOW_CLUSTER_ACTIONS_LABEL : "{0} 叢集動作",
    SHOW_HOST_ACTIONS_LABEL : "{0} 主機動作",
    SHOW_RUNTIME_ACTIONS_LABEL : "執行時期 {0} 動作",
    SHOW_SERVER_ACTIONS_MENU_LABEL : "伺服器 {0} 動作功能表",
    SHOW_APP_ACTIONS_MENU_LABEL : "應用程式 {0} 動作功能表",
    SHOW_CLUSTER_ACTIONS_MENU_LABEL : "叢集 {0} 動作功能表",
    SHOW_HOST_ACTIONS_MENU_LABEL : "主機 {0} 動作功能表",
    SHOW_RUNTIME_ACTIONS_MENU_LABEL : "執行時期 {0} 動作功能表",
    SHOW_RUNTIMEONHOST_ACTIONS_MENU_LABEL : "主機 {0} 上之執行時期的動作功能表",
    SHOW_COLLECTION_MENU_LABEL : "集合 {0} 狀態動作功能表",  // menu object id
    SHOW_SEARCH_MENU_LABEL : "搜尋 {0} 狀態動作功能表",  // menu object id


    // A bit odd to have sentence casing without punctuation?
    UNKNOWN_STATE : "{0}：不明狀態", // resourceName
    UNKNOWN_STATE_APPS : "{0} 個應用程式處於不明狀態", // quantity
    UNKNOWN_STATE_APP_INSTANCES : "{0} 個應用程式實例處於不明狀態", // quantity
    UNKNOWN_STATE_SERVERS : "{0} 部伺服器處於不明狀態", // quantity
    UNKNOWN_STATE_CLUSTERS : "{0} 個叢集處於不明狀態", // quantity

    INSTANCES_NOT_RUNNING : "{0} 個應用程式實例不在執行中", // quantity
    APPS_NOT_RUNNING : "{0} 個應用程式不在執行中", // quantity
    SERVERS_NOT_RUNNING : "{0} 部伺服器不在執行中", // quantity
    CLUSTERS_NOT_RUNNING : "{0} 個叢集不在執行中", // quantity

    APP_STOPPED_ON_SERVER : "{0} 已在正在執行的伺服器 {1} 上停止", // appName, serverName(s)
    APPS_STOPPED_ON_SERVERS : "{0} 個應用程式已在正在執行的伺服器 {1} 上停止", // quantity, serverName(s)
    APPS_STOPPED_ON_SERVER : "{0} 個應用程式已在正在執行的伺服器上停止。", // quantity
    NUMBER_RESOURCES : "{0} 個資源", // quantity
    NUMBER_APPS : "{0} 個應用程式", // quantity
    NUMBER_SERVERS : "{0} 部伺服器", // quantity
    NUMBER_CLUSTERS : "{0} 個叢集", // quantity
    NUMBER_HOSTS : "{0} 部主機", // quantity
    NUMBER_RUNTIMES : "{0} 個執行時期", // quantity
    SERVERS_INSERT : "伺服器",
    INSERT_STOPPED_ON_INSERT : "{0} 已在正在執行的 {1} 上停止。", // NUMBER_APPS, SERVERS_INSERT

    APPSERVER_STOPPED_ON_SERVER : "{0} 已在正在執行的伺服器 {1} 上停止", //appName, serverName
    APPCLUSTER_STOPPED_ON_SERVER : "叢集 {1} 中的 {0} 已在正在執行的伺服器 {2} 上停止",  //appName, clusterName, serverName(s)

    INSTANCES_STOPPED_ON_SERVERS : "{0} 個應用程式實例已在正在執行的伺服器上停止。", // quantity
    INSTANCE_STOPPED_ON_SERVERS : "{0}：應用程式實例不在執行中", // serverNames

    NOT_ALL_APPS_RUNNING : "{0}：並非所有應用程式都在執行中", // serverName[]
    NO_APPS_RUNNING : "{0}：無應用程式執行中", // serverName[]
    NOT_ALL_APPS_RUNNING_SERVERS : "並非所有應用程式都在執行中的伺服器有 {0} 部", // quantity
    NO_APPS_RUNNING_SERVERS : "沒有應用程式正在執行的伺服器有 {0} 部", // quantity

    COUNT_OF_APPS_SELECTED : "選取了 {0} 個應用程式",
    RATIO_RUNNING : "{0} 正在執行", // ratio ex. 1/2

    RESOURCES_SELECTED : "已選取 {0} 個",

    NO_HOSTS_SELECTED : "未選取任何主機",
    NO_DEPLOY_RESOURCE : "無資源可供部署安裝",
    NO_TOPOLOGY : "沒有任何 {0}。",
    COUNT_OF_APPS_STARTED  : "{0} 個應用程式已啟動",

    APPS_LIST : "{0} 個應用程式",
    EMPTY_MESSAGE : "{0}",  // used to build a list of comma separated resource names
    APPS_INSTANCES_RUNNING_OF_TOTAL : "{0}/{1} 個實例執行中",
    HOSTS_SERVERS_RUNNING_OF_TOTAL : "{0}/{1} 部伺服器執行中",
    RESOURCE_ON_RESOURCE : "{0} 位於 {1}", // resource name, resource name
    RESOURCE_ON_SERVER_RESOURCE : "{0} 位於伺服器 {1}", // resource name, resource name
    RESOURCE_ON_CLUSTER_RESOURCE : "{0} 位於叢集 {1}", // resource name, resource name

    RESTART_DISABLED_FOR_ADMIN_CENTER: "此伺服器已停用重新啟動，因為它正在代管「管理中心」",
    ACTION_DISABLED_FOR_USER: "動作在這個資源上停用，因為使用者未獲授權",

    RESTART_AC_TITLE: "無法重新啟動管理中心",
    RESTART_AC_DESCRIPTION: "{0} 負責提供「管理中心」。「管理中心」無法自行重新啟動。",
    RESTART_AC_MESSAGE: "將重新啟動其他所有選取的伺服器。",
    RESTART_AC_CLUSTER_MESSAGE: "將重新啟動其他所有選取的叢集。",

    STOP_AC_TITLE: "停止管理中心",
    STOP_AC_DESCRIPTION: "伺服器 {0} 是執行「管理中心」的 Collective Controller。停止它可能影響 Liberty 群體管理作業，並造成無法使用「管理中心」。",
    STOP_AC_MESSAGE: "您想要停止此控制器嗎？",
    STOP_STANDALONE_DESCRIPTION: "伺服器 {0} 執行「管理中心」。如果停止，會導致「管理中心」無法使用。",
    STOP_STANDALONE_MESSAGE: "您想停止這部伺服器嗎？",

    STOP_CONTROLLER_TITLE: "停止控制器",
    STOP_CONTROLLER_DESCRIPTION: "伺服器 {0} 是 Collective Controller。如果停止，可能影響 Liberty 群體作業。",
    STOP_CONTROLLER_MESSAGE: "您想要停止此控制器嗎？",

    STOP_AC_CLUSTER_TITLE: "停止叢集 {0}",
    STOP_AC_CLUSTER_DESCRIPTION: "叢集 {0} 含有執行「管理中心」的群體控制器。停止它可能會影響 Liberty 群體管理作業並且使「管理中心」無法使用。",
    STOP_AC_CLUSTER_MESSAGE: "您要停止此叢集嗎？",

    INVALID_URL: "頁面不存在。",
    INVALID_APPLICATION: "應用程式 {0} 已不存在於群體中。", // application name
    INVALID_SERVER: "伺服器 {0} 已不存在於群體中。", // server name
    INVALID_CLUSTER: "叢集 {0} 已不存在於群體中。", // cluster name
    INVALID_HOST: "主機 {0} 已不存在於群體中。", // host name
    INVALID_RUNTIME: "執行時期 {0} 已不存在於群體中。", // runtime name
    INVALID_INSTANCE: "應用程式實例 {0} 已不存在於群體中。", // application instance name
    GO_TO_DASHBOARD: "跳至儀表板",
    VIEWED_RESOURCE_REMOVED: "糟糕！資源已移除或無法再使用。",

    OK_DEFAULT_BUTTON: "確定",
    CONNECTION_FAILED_MESSAGE: "已遺失伺服器的連線。頁面不再顯示環境的動態變更。請重新整理頁面，以還原連線和動態更新。",
    ERROR_MESSAGE: "連線已中斷",

    // Used by standalone stop message dialog
    STANDALONE_STOP_TITLE : '停止伺服器',

    // Tags
    RELATED_RESOURCES: "相關的資源",
    TAGS : "標籤",
    TAG_BUTTON_LABEL : "標籤 {0}",  // tag value
    TAGS_LABEL : "請輸入一些標籤，並以逗點、空格、Enter 鍵或 Tab 鍵區隔。",
    OWNER : "擁有者",
    OWNER_BUTTON_LABEL : "擁有者 {0}",  // owner value
    CONTACTS : "聯絡人",
    CONTACT_BUTTON_LABEL : "聯絡人 {0}",  // contact value
    PORTS : "埠",
    CONTEXT_ROOT : "環境定義根目錄",
    HTTP : "HTTP",
    HTTPS : "HTTPS",
    MORE : "其他",  // alt text for the ... button
    MORE_BUTTON_MENU : "{0} 其他功能表", // alt text for the menu
    NOTES: "注意事項",
    NOTE_LABEL : "附註 {0}",  // note value
    SET_ATTRIBUTES: "標籤和中繼資料",
    SETATTR_RUNTIME_NAME: "{0} 位於 {1}",  // runtime, host
    SAVE: "儲存",
    TAGINVALIDCHARS: "下列字元無效：'/'、'<' 和 '>'。",
    ERROR_GET_TAGS_METADATA: "產品無法取得資源的現行標籤和中繼資料。",
    ERROR_SET_TAGS_METADATA: "發生錯誤，使產品無法設定標籤和中繼資料。",
    METADATA_WILLBE_INHERITED: "Meta 資料設定在應用程式中，且供所有在叢集中的實例共用。",
    ERROR_ALT: "錯誤",

    // Graph Warning Messages
    GRAPH_SERVER_NOT_STARTED: "這部伺服器的現行統計資料無法使用，因為它已停止。請啟動伺服器，開始監視它。",
    GRAPH_SERVER_HOSTING_APP_NOT_STARTED: "這個應用程式的現行統計資料無法使用，因為其相關聯的伺服器已停止。請啟動伺服器，開始監視這個應用程式。",
    GRAPH_FEATURES_NOT_CONFIGURED: "這裡尚無任何項目！請選取「編輯」圖示，並新增度量，來監視此資源。",
    NO_GRAPHS_AVAILABLE: "沒有可用的度量可新增。請嘗試安裝其他的監視特性，以提供更多的度量。",
    NO_APPS_GRAPHS_AVAILABLE: "沒有可用的度量可新增。請嘗試安裝其他的監視特性，以提供更多的度量。此外，請確定正在使用該應用程式。",
    GRAPH_CONFIG_NOT_SAVED_TITLE : "未儲存的變更",
    GRAPH_CONFIG_NOT_SAVED_DESCR : "您有未儲存的變更。如果您移至另一頁面，就會遺失變更。",
    GRAPH_CONFIG_NOT_SAVED_MSG : "您要儲存變更嗎？",

    NO_CPU_STATS_AVAILABLE : "這部伺服器的「CPU 使用率」統計資料無法使用。",

    // Server Config
    CONFIG_NOT_AVAILABLE: "如果要啟用這個視圖，請安裝「伺服器配置」工具。",
    SAVE_BEFORE_CLOSING_DIALOG_MESSAGE: "關閉之前要先儲存 {0} 的變更嗎？",
    SAVE: "儲存",
    DONT_SAVE: "不儲存",

    // Maintenance mode
    ENABLE_MAINTENANCE_MODE: "啟用維護模式",
    DISABLE_MAINTENANCE_MODE: "停用維護模式",
    ENABLE_MAINTENANCE_MODE_DIALOG_TITLE: "啟用維護模式",
    DISABLE_MAINTENANCE_MODE_DIALOG_TITLE: "停用維護模式",
    ENABLE_MAINTENANCE_MODE_HOST_DESCRIPTION: "在該主機和其所有伺服器（{0} 部伺服器）上啟用維護模式",
    ENABLE_MAINTENANCE_MODE_HOSTS_DESCRIPTION: "在主機和其所有伺服器（{0} 部伺服器）上啟用維護模式",
    ENABLE_MAINTENANCE_MODE_SERVER_DESCRIPTION: "在該伺服器上啟用維護模式",
    ENABLE_MAINTENANCE_MODE_SERVERS_DESCRIPTION: "在伺服器上啟用維護模式",
    DISABLE_MAINTENANCE_MODE_HOST_DESCRIPTION: "在該主機和其所有伺服器（{0} 部伺服器）上停用維護模式",
    DISABLE_MAINTENANCE_MODE_SERVER_DESCRIPTION: "在伺服器上停用維護模式",
    BREAK_AFFINITY_LABEL: "中斷與作用中階段作業的親緣性",
    ENABLE_MAINTENANCE_MODE_DIALOG_ENABLE_BUTTON_LABEL: "啟用",
    DISABLE_MAINTENANCE_MODE_DIALOG_ENABLE_BUTTON_LABEL: "停用",
    MAINTENANCE_MODE: "維護模式",
    ENABLING_MAINTENANCE_MODE: "正在啟用維護模式",
    MAINTENANCE_MODE_ENABLED: "維護模式已啟用",
    MAINTENANCE_MODE_ALTERNATE_SERVERS_NOT_STARTED: "維護模式未啟用，因為替代伺服器未啟動。",
    MAINTENANCE_MODE_SELECT_FORCE_MESSAGE: "選取「強制」以啟用維護模式，且不啟動替代伺服器。「強制」可能會中斷自動調整原則。",
    MAINTENANCE_MODE_FAILED: "無法啟用維護模式",
    MAINTENANCE_MODE_FORCE_LABEL: "強制",
    MAINTENANCE_MODE_CANCEL_LABEL: "取消",
    MAINTENANCE_MODE_SERVERS_IN_MAINTENANCE_MODE: "有 {0} 部伺服器目前處於維護模式。",
    MAINTENANCE_MODE_ENABLING_MAINTENANCE_MODE_ON_ALL_HOST_SERVERS_EQUAL_OR_LESS_THEN_10: "正在在主機的所有伺服器上啟用維護模式。",
    MAINTENANCE_MODE_ENABLING_MAINTENANCE_MODE_ON_ALL_HOST_SERVERS_MORE_THAN_10: "正在在主機的所有伺服器上啟用維護模式。顯示「伺服器」視圖，以查看狀態。",

    SERVER_API_DOCMENTATION: "檢視伺服器 API 定義",

    // objectView title
    TITLE_FOR_CLUSTER: "叢集 {0}", // cluster name
    TITLE_FOR_HOST: "主機 {0}", // host name

    // objectView descriptor
    COLLECTIVE_CONTROLLER_DESCRIPTOR: "群體控制器",
    LIBERTY_SERVER : "Liberty 伺服器",
    NODEJS_SERVER : "Node.js 伺服器",
    CONTAINER : "儲存器",
    CONTAINER_LIBERTY : "Liberty",
    CONTAINER_DOCKER : "Docker",
    CONTAINER_NODEJS : "Node.js",
    LIBERTY_IN_DOCKER_DESCRIPTOR : "Docker 儲存器中的 Liberty 伺服器",
    NODEJS_IN_DOCKER_DESCRIPTOR : "Docker 儲存器中的 Node.js 伺服器",
    RUNTIME_LIBERTY : "Liberty 執行時期",
    RUNTIME_NODEJS : "Node.js 執行時期",
    RUNTIME_DOCKER : "Docker 儲存器中的執行時期"

});
