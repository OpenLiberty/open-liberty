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
    EXPLORER : "浏览器",
    EXPLORE : "浏览",
    DASHBOARD : "仪表板",
    DASHBOARD_VIEW_ALL_APPS : "查看所有应用程序",
    DASHBOARD_VIEW_ALL_SERVERS : "查看所有服务器",
    DASHBOARD_VIEW_ALL_CLUSTERS : "查看所有集群",
    DASHBOARD_VIEW_ALL_HOSTS : "查看所有主机",
    DASHBOARD_VIEW_ALL_RUNTIMES : "查看所有运行时",
    SEARCH : "搜索",
    SEARCH_RECENT : "最近搜索项",
    SEARCH_RESOURCES : "搜索资源",
    SEARCH_RESULTS : "搜索结果",
    SEARCH_NO_RESULTS : "无结果",
    SEARCH_NO_MATCHES : "无匹配项",
    SEARCH_TEXT_INVALID : "搜索文本包括无效字符",
    SEARCH_CRITERIA_INVALID : "搜索条件无效。",
    SEARCH_CRITERIA_INVALID_COMBO :"当与 {1} 一起指定时，{0} 无效。",
    SEARCH_CRITERIA_INVALID_DUPLICATE : "仅指定一次 {0}。",
    SEARCH_TEXT_MISSING : "需要搜索文本",
    SEARCH_UNSUPPORT_TYPE_APPONSERVER : "不支持在服务器上搜索应用程序标签。",
    SEARCH_UNSUPPORT_TYPE_APPONCLUSTER : "不支持在集群上搜索应用程序标签。",
    SEARCH_UNSUPPORT : "不支持搜索条件。",
    SEARCH_SWITCH_VIEW : "切换视图",
    FILTERS : "过滤器",
    DEPLOY_SERVER_PACKAGE : "部署服务器包",
    MEMBER_OF : "所属对象",
    N_CLUSTERS: "{0} 个集群 ...",

    INSTANCE : "实例",
    INSTANCES : "实例",
    APPLICATION : "应用程序",
    APPLICATIONS : "应用程序",
    SERVER : "服务器",
    SERVERS : "服务器",
    CLUSTER : "集群",
    CLUSTERS : "集群",
    CLUSTER_NAME : "集群名称：",
    CLUSTER_STATUS : "集群状态：",
    APPLICATION_NAME : "应用程序名称：",
    APPLICATION_STATE : "应用程序状态：",
    HOST : "主机",
    HOSTS : "主机",
    RUNTIME : "运行时",
    RUNTIMES : "运行时",
    PATH : "路径",
    CONTROLLER : "控制器",
    CONTROLLERS : "控制器",
    OVERVIEW : "概述",
    CONFIGURE : "配置",

    SEARCH_RESOURCE_TYPE: "类型", // Search by resource types
    SEARCH_RESOURCE_STATE: "省/自治区/直辖市", // Search by resource types
    SEARCH_RESOURCE_TYPE_ALL: "全部", // Search all resource types
    SEARCH_RESOURCE_NAME: "名称", // Search by resource name
    SEARCH_RESOURCE_TAG: "标签", // Search by resource tag
    SEARCH_RESOURCE_CONTAINER: "容器", // Search by container type
    SEARCH_RESOURCE_CONTAINER_DOCKER: "Docker", // Search by container type Docker
    SEARCH_RESOURCE_CONTAINER_NONE: "无", // Search by container type none
    SEARCH_RESOURCE_RUNTIMETYPE: "运行时类型", // Search by runtime type
    SEARCH_RESOURCE_OWNER: "所有者", // Search by owner
    SEARCH_RESOURCE_CONTACT: "联系人", // Search by contact
    SEARCH_RESOURCE_NOTE: "注释", // Search by note

    GRID_HEADER_USERDIR : "用户目录",
    GRID_HEADER_NAME : "名称",
    GRID_LOCATION_NAME : "地区",
    GRID_ACTIONS : "网格操作",
    GRID_ACTIONS_LABEL : "{0} 网格操作",  // name of the grid
    APPONSERVER_LOCATION_NAME : "{0} 位于 {1} ({2})", // server on host (/path)

    STATS : "监视员",
    STATS_ALL : "全部",
    STATS_VALUE : "值：{0}",
    CONNECTION_IN_USE_STATS : "{0} 个正在使用 = {1} 个受管 - {2} 个可用",
    CONNECTION_IN_USE_STATS_VALUE : "值：{0} 个正在使用 = {1} 个受管 - {2} 个可用",
    DATA_SOURCE : "数据源：{0}",
    STATS_DISPLAY_LEGEND : "显示图注",
    STATS_HIDE_LEGEND : "隐藏图注",
    STATS_VIEW_DATA : "查看图表数据",
    STATS_VIEW_DATA_TIMESTAMP : "时间戳记",
    STATS_ACTION_MENU : "{0} 操作菜单",
    STATS_SHOW_HIDE : "添加资源度量",
    STATS_SHOW_HIDE_SUMMARY : "添加摘要的度量",
    STATS_SHOW_HIDE_TRAFFIC : "添加流量的度量",
    STATS_SHOW_HIDE_PERFORMANCE : "添加性能的度量",
    STATS_SHOW_HIDE_AVAILABILITY : "添加可用性的度量",
    STATS_SHOW_HIDE_ALERT : "添加警报的度量",
    STATS_SHOW_HIDE_LIST_BUTTON : "显示或者隐藏资源度量列表",
    STATS_SHOW_HIDE_BUTTON_TITLE : "编辑图表",
    STATS_SHOW_HIDE_CONFIRM : "保存",
    STATS_SHOW_HIDE_CANCEL : "取消",
    STATS_SHOW_HIDE_DONE : "完成",
    STATS_DELETE_GRAPH : "删除图表",
    STATS_ADD_CHART_LABEL : "向视图添加图表",
    STATS_JVM_TITLE : "JVM",
    STATS_JVM_BUTTON_LABEL : "向视图添加所有 JVM 图表",
    STATS_HEAP_TITLE : "已使用堆内存",
    STATS_HEAP_USED : "已用：{0} MB",
    STATS_HEAP_COMMITTED : "已落实：{0} MB",
    STATS_HEAP_MAX : "最大：{0} MB",
    STATS_HEAP_X_TIME : "时间",
    STATS_HEAP_Y_MB : "已使用容量 (MB)",
    STATS_HEAP_Y_MB_LABEL : "{0} MB",
    STATS_CLASSES_TITLE : "装入类数",
    STATS_CLASSES_LOADED : "已装入：{0}",
    STATS_CLASSES_UNLOADED : "已卸装：{0}",
    STATS_CLASSES_TOTAL : "总计：{0}",
    STATS_CLASSES_Y_TOTAL : "装入类数",
    STATS_PROCESSCPU_TITLE : "CPU 使用率",
    STATS_PROCESSCPU_USAGE : "CPU 使用率：{0}%",
    STATS_PROCESSCPU_Y_PERCENT : "CPU 百分比",
    STATS_PROCESSCPU_Y_PCT_LABEL : "{0}%",
    STATS_THREADS_TITLE : "活动 JVM 线程数",
    STATS_LIVE_MSG_INIT : "显示实时数据",
    STATS_LIVE_MSG :"此图表没有历史记录数据。它会继续显示最近十分钟的数据。",
    STATS_THREADS_ACTIVE : "实时：{0}",
    STATS_THREADS_PEAK : "峰值：{0}",
    STATS_THREADS_TOTAL : "总计：{0}",
    STATS_THREADS_Y_THREADS : "线程数",
    STATS_TP_POOL_SIZE : "池大小",
    STATS_JAXWS_TITLE : "JAX-WS Web 服务",
    STATS_JAXWS_BUTTON_LABEL : "向视图添加所有 JAX-WS Web Service 图表",
    STATS_JW_AVG_RESP_TIME : "平均响应时间",
    STATS_JW_AVG_INVCOUNT : "平均调用计数",
    STATS_JW_TOTAL_FAULTS : "总计运行时故障",
    STATS_LA_RESOURCE_CONFIG_LABEL : "选择资源...",
    STATS_LA_RESOURCE_CONFIG_LABEL_NUM : "{0} 个资源",
    STATS_LA_RESOURCE_CONFIG_LABEL_NUM_1 : "1 个资源",
    STATS_LA_RESOURCE_CONFIG_SELECT_ONE : "您必须至少选择一个资源。",
    STATS_LA_RESOURCE_CONFIG_NO_DATA : "所选时间范围没有可用数据。",
    STATS_ACCESS_LOG_TITLE : "访问日志",
    STATS_ACCESS_LOG_BUTTON_LABEL : "向视图添加所有访问日志图表",
    STATS_ACCESS_LOG_GRAPH : "访问日志消息计数",
    STATS_ACCESS_LOG_SUMMARY : "访问日志摘要",
    STATS_ACCESS_LOG_TABLE : "访问日志消息列表",
    STATS_MESSAGES_TITLE : "消息和跟踪",
    STATS_MESSAGES_BUTTON_LABEL : "向视图添加所有消息和跟踪图表",
    STATS_MESSAGES_GRAPH : "日志消息计数",
    STATS_MESSAGES_TABLE : "日志消息列表",
    STATS_FFDC_GRAPH : "FFDC 计数",
    STATS_FFDC_TABLE : "FFDC 列表",
    STATS_TRACE_LOG_GRAPH : "跟踪消息计数",
    STATS_TRACE_LOG_TABLE : "跟踪消息列表",
    STATS_THREAD_POOL_TITLE : "线程池",
    STATS_THREAD_POOL_BUTTON_LABEL : "向视图添加所有线程池图表",
    STATS_THREADPOOL_TITLE : "活动 Liberty 线程",
    STATS_THREADPOOL_SIZE : "池大小：{0}",
    STATS_THREADPOOL_ACTIVE : "活动：{0}",
    STATS_THREADPOOL_TOTAL : "总计：{0}",
    STATS_THREADPOOL_Y_ACTIVE : "活动线程数",
    STATS_SESSION_MGMT_TITLE : "会话数",
    STATS_SESSION_MGMT_BUTTON_LABEL : "向视图添加所有会话图表",
    STATS_SESSION_CONFIG_LABEL : "选择会话...",
    STATS_SESSION_CONFIG_LABEL_NUM  : "{0} 个会话",
    STATS_SESSION_CONFIG_LABEL_NUM_1  : "1 个会话",
    STATS_SESSION_CONFIG_SELECT_ONE : "您必须至少选择一个会话。",
    STATS_SESSION_TITLE : "活动会话数",
    STATS_SESSION_Y_ACTIVE : "活动会话数",
    STATS_SESSION_LIVE_LABEL : "实时计数：{0}",
    STATS_SESSION_CREATE_LABEL : "创建计数：{0}",
    STATS_SESSION_INV_LABEL : "失效计数：{0}",
    STATS_SESSION_INV_TIME_LABEL : "超时的失效计数：{0}",
    STATS_WEBCONTAINER_TITLE : "Web 应用程序",
    STATS_WEBCONTAINER_BUTTON_LABEL : "向视图添加所有 Web 应用程序图表",
    STATS_SERVLET_CONFIG_LABEL : "选择 servlet...",
    STATS_SERVLET_CONFIG_LABEL_NUM : "{0} 个 servlet",
    STATS_SERVLET_CONFIG_LABEL_NUM_1 : "1 个 servlet",
    STATS_SERVLET_CONFIG_SELECT_ONE : "您必须至少选择一个 servlet。",
    STATS_SERVLET_REQUEST_COUNT_TITLE : "请求计数",
    STATS_SERVLET_REQUEST_COUNT_Y_AXIS : "请求计数",
    STATS_SERVLET_RESPONSE_COUNT_TITLE : "响应计数",
    STATS_SERVLET_RESPONSE_COUNT_Y_AXIS : "响应计数",
    STATS_SERVLET_RESPONSE_MEAN_TITLE : "平均响应时间 (ns)",
    STATS_SERVLET_RESPONSE_MEAN_Y_AXIS : "响应时间 (ns)",
    STATS_CONN_POOL_TITLE : "连接池",
    STATS_CONN_POOL_BUTTON_LABEL : "向视图添加所有连接池图表",
    STATS_CONN_POOL_CONFIG_LABEL : "选择数据源...",
    STATS_CONN_POOL_CONFIG_LABEL_NUM : "{0} 个数据源",
    STATS_CONN_POOL_CONFIG_LABEL_NUM_1 : "1 个数据源",
    STATS_CONN_POOL_CONFIG_SELECT_ONE : "您必须至少选择一个数据源。",
    STATS_CONNECT_IN_USE_TITLE : "正在使用连接",
    STATS_CONNECT_USED_COUNT_Y_AXIS : "连接数",
    STATS_CONNECT_IN_USE_LABEL : "正在使用：{0}",
    STATS_CONNECT_USED_USED_LABEL : "已使用：{0}",
    STATS_CONNECT_USED_FREE_LABEL : "可用：{0}",
    STATS_CONNECT_USED_CREATE_LABEL : "已创建：{0}",
    STATS_CONNECT_USED_DESTROY_LABEL : "已毁坏：{0}",
    STATS_CONNECT_WAIT_TIME_TITLE : "平均等待时间 (ms)",
    STATS_CONNECT_WAIT_TIME_Y_AXIS : "等待时间（毫秒）",
    STATS_TIME_ALL : "全部",
    STATS_TIME_1YEAR : "1 年",
    STATS_TIME_1MONTH : "1 个月",
    STATS_TIME_1WEEK : "1 星期",
    STATS_TIME_1DAY : "1 天",
    STATS_TIME_1HOUR : "1 小时",
    STATS_TIME_10MINUTES : "10 分钟",
    STATS_TIME_5MINUTES : "5 分钟",
    STATS_TIME_1MINUTE : "1 分钟",
    STATS_PERSPECTIVE_SUMMARY : "摘要",
    STATS_PERSPECTIVE_TRAFFIC : "流量",
    STATS_PERSPECTIVE_TRAFFIC_JVM : "JVM 流量",
    STATS_PERSPECTIVE_TRAFFIC_CONN : "连接流量",
    STATS_PERSPECTIVE_TRAFFIC_LAACCESS : "访问日志流量",
    STATS_PERSPECTIVE_PROBLEM : "问题",
    STATS_PERSPECTIVE_PERFORMANCE : "性能",
    STATS_PERSPECTIVE_PERFORMANCE_JVM : "JVM 性能",
    STATS_PERSPECTIVE_PERFORMANCE_CONN : "连接性能",
    STATS_PERSPECTIVE_ALERT : "警报分析",
    STATS_PERSPECTIVE_ALERT_LAACCESS : "访问日志警报",
    STATS_PERSPECTIVE_ALERT_LAMSGS : "消息和跟踪日志警报",
    STATS_PERSPECTIVE_AVAILABILITY : "可用性",

    STATS_DISPLAY_TIME_LAST_MINUTE_LABEL : "最近 1 分钟",
    STATS_DISPLAY_TIME_LAST_5_MINUTES_LABEL : "最近 5 分钟",
    STATS_DISPLAY_TIME_LAST_10_MINUTES_LABEL : "最近 10 分钟",
    STATS_DISPLAY_TIME_LAST_HOUR_LABEL : "最近 1 小时",
    STATS_DISPLAY_TIME_LAST_DAY_LABEL : "昨天",
    STATS_DISPLAY_TIME_LAST_WEEK_LABEL : "上周",
    STATS_DISPLAY_TIME_LAST_MONTH_LABEL : "上个月",
    STATS_DISPLAY_TIME_LAST_YEAR_LABEL : "去年",

    STATS_DISPLAY_CUSTOM_TIME_LAST_SECOND_LABEL : "最近 {0} 秒",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MINUTE_LABEL : "最近 {0} 分钟",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MINUTE_AND_SECOND_LABEL : "最近 {0} 分钟 {1} 秒",
    STATS_DISPLAY_CUSTOM_TIME_LAST_HOUR_LABEL : "最近 {0} 小时",
    STATS_DISPLAY_CUSTOM_TIME_LAST_HOUR_AND_MINUTE_LABEL : "最近 {0} 小时 {1} 分钟",
    STATS_DISPLAY_CUSTOM_TIME_LAST_DAY_LABEL : "最近 {0} 天",
    STATS_DISPLAY_CUSTOM_TIME_LAST_DAY_AND_HOUR_LABEL : "最近 {0} 天 {1} 小时",
    STATS_DISPLAY_CUSTOM_TIME_LAST_WEEK_LABEL : "最近 {0} 周",
    STATS_DISPLAY_CUSTOM_TIME_LAST_WEEK_AND_DAY_LABEL : "最近 {0} 周 {1} 天",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MONTH_LABEL : "最近 {0} 个月",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MONTH_AND_DAY_LABEL : "最近 {0} 个月 {1} 天",
    STATS_DISPLAY_CUSTOM_TIME_LAST_YEAR_LABEL : "最近 {0} 年",
    STATS_DISPLAY_CUSTOM_TIME_LAST_YEAR_AND_MONTH_LABEL : "最近 {0} 年 {1} 个月",

    STATS_LIVE_UPDATE_LABEL: "实时更新",
    STATS_TIME_SELECTOR_NOW_LABEL: "现在",

    LOG_ANALYTICS_LOG_MESSAGE_TITLE: "日志消息",

    AUTOSCALED_APPLICATION : "自动扩展的应用程序",
    AUTOSCALED_SERVER : "自动扩展的服务器",
    AUTOSCALED_CLUSTER : "自动扩展的集群",
    AUTOSCALED_POLICY : "自动扩展策略",
    AUTOSCALED_POLICY_DISABLED : "已禁用自动扩展策略",
    AUTOSCALED_NOACTIONS : "不可对自动扩展的资源执行操作",

    START : "开始",
    START_CLEAN : "启动 --clean",
    STARTING : "正在启动",
    STARTED : "已启动",
    RUNNING : "正在运行",
    NUM_RUNNING: "{0} 正在运行",
    PARTIALLY_STARTED : "已部分启动",
    PARTIALLY_RUNNING : "部分正在运行",
    NOT_STARTED : "未启动",
    STOP : "停止",
    STOPPING : "正在停止",
    STOPPED : "已停止",
    NUM_STOPPED : "{0} 已停止",
    NOT_RUNNING : "未在运行",
    RESTART : "重新启动",
    RESTARTING : "正在重新启动",
    RESTARTED : "已重新启动",
    ALERT : "警报",
    ALERTS : "警报",
    UNKNOWN : "未知",
    NUM_UNKNOWN : "{0} 未知",
    SELECT : "选择",
    SELECTED : "已选择",
    SELECT_ALL : "全部选中",
    SELECT_NONE : "全部不选",
    DESELECT: "取消选择",
    DESELECT_ALL : "全部取消选择",
    TOTAL : "总计",
    UTILIZATION : "超过 {0}% 利用率", // percent

    ELLIPSIS_ARIA: "展开更多选项。",
    EXPAND : "展开",
    COLLAPSE: "折叠",

    ALL : "全部",
    ALL_APPS : "所有应用程序",
    ALL_SERVERS : "所有服务器",
    ALL_CLUSTERS : "所有集群",
    ALL_HOSTS : "所有主机",
    ALL_APP_INSTANCES : "所有应用程序实例",
    ALL_RUNTIMES : "所有运行时",

    ALL_APPS_RUNNING : "所有应用程序正在运行",
    ALL_SERVER_RUNNING : "所有服务器正在运行",
    ALL_CLUSTERS_RUNNING : "所有集群正在运行",
    ALL_APPS_STOPPED : "所有应用程序已停止",
    ALL_SERVER_STOPPED : "所有服务器已停止",
    ALL_CLUSTERS_STOPPED : "所有集群已停止",
    ALL_SERVERS_UNKNOWN : "所有服务器都未知",
    SOME_APPS_RUNNING : "一些应用程序正在运行",
    SOME_SERVERS_RUNNING : "一些服务器正在运行",
    SOME_CLUSTERS_RUNNING : "一些集群正在运行",
    NO_APPS_RUNNING : "没有应用程序正在运行",
    NO_SERVERS_RUNNING : "没有服务器正在运行",
    NO_CLUSTERS_RUNNING : "没有集群正在运行",

    HOST_WITH_ALL_SERVERS_RUNNING: "所有服务器正在运行的主机", // not used anymore since 4Q
    HOST_WITH_SOME_SERVERS_RUNNING: "一些服务器正在运行的主机",
    HOST_WITH_NO_SERVERS_RUNNING: "没有服务器正在运行的主机", // not used anymore since 4Q
    HOST_WITH_ALL_SERVERS_STOPPED: "所有服务器都已停止的主机",
    HOST_WITH_SERVERS_RUNNING: "服务器正在运行的主机",

    RUNTIME_WITH_SOME_SERVERS_RUNNING: "一些服务器正在运行的运行时",
    RUNTIME_WITH_ALL_SERVERS_STOPPED: "所有服务器都已停止的运行时",
    RUNTIME_WITH_SERVERS_RUNNING: "服务器正在运行的运行时",

    START_ALL_APPS : "要启动所有应用程序吗？",
    START_ALL_INSTANCES : "要启动所有应用程序实例吗？",
    START_ALL_SERVERS : "要启动所有服务器吗？",
    START_ALL_CLUSTERS : "要启动所有集群吗？",
    STOP_ALL_APPS : "要停止所有应用程序吗？",
    STOPE_ALL_INSTANCES : "要停止所有应用程序实例吗？",
    STOP_ALL_SERVERS : "要停止所有服务器吗？",
    STOP_ALL_CLUSTERS : "要停止所有集群吗？",
    RESTART_ALL_APPS : "要重新启动所有应用程序吗？",
    RESTART_ALL_INSTANCES : "要重新启动所有应用程序实例吗？",
    RESTART_ALL_SERVERS : "要重新启动所有服务器吗？",
    RESTART_ALL_CLUSTERS : "要重新启动所有集群吗？",

    START_INSTANCE : "要启动应用程序实例吗？",
    STOP_INSTANCE : "要停止应用程序实例吗？",
    RESTART_INSTANCE : "要重新启动应用程序实例吗？",

    START_SERVER : "要启动服务器 {0} 吗？",
    STOP_SERVER : "要停止服务器 {0} 吗？",
    RESTART_SERVER : "要重新启动服务器 {0} 吗？",

    START_ALL_INSTS_OF_APP : "要启动 {0} 的所有实例吗？", // application name
    START_APP_ON_SERVER : "要在 {1} 上启动 {0} 吗？", // app name, server name
    START_ALL_APPS_WITHIN : "要在 {0} 内启动所有应用程序吗？", // resource
    START_ALL_APP_INSTS_WITHIN : "要在 {0} 内启动所有应用程序实例吗？", // resource
    START_ALL_SERVERS_WITHIN : "要在 {0} 内启动所有服务器吗？", // resource
    STOP_ALL_INSTS_OF_APP : "要停止 {0} 的所有实例吗？", // application name
    STOP_APP_ON_SERVER : "要在 {1} 停止 {0} 吗？", // app name, server name
    STOP_ALL_APPS_WITHIN : "要在 {0} 内停止所有应用程序吗？", // resource
    STOP_ALL_APP_INSTS_WITHIN : "要在 {0} 内停止所有应用程序实例吗？", // resource
    STOP_ALL_SERVERS_WITHIN : "要在 {0} 内停止所有服务器吗？", // resource
    RESTART_ALL_INSTS_OF_APP : "要重新启动 {0} 的所有实例吗？", // application name
    RESTART_APP_ON_SERVER : "要在 {1} 上重新启动 {0} 吗？", // app name, server name
    RESTART_ALL_APPS_WITHIN : "要在 {0} 内重新启动所有应用程序吗？", // resource
    RESTART_ALL_APP_INSTS_WITHIN : "要在 {0} 内重新启动所有应用程序实例吗？", // resource
    RESTART_ALL_SERVERS_WITHIN : "要在 {0} 内重新启动所有正在运行的服务器吗？", // resource

    START_SELECTED_APPS : "要启动所选应用程序的所有实例吗？",
    START_SELECTED_INSTANCES : "要启动所选应用程序实例吗？",
    START_SELECTED_SERVERS : "要启动所选服务器吗？",
    START_SELECTED_SERVERS_LABEL : "启动所选服务器",
    START_SELECTED_CLUSTERS : "要启动所选集群吗？",
    START_CLEAN_SELECTED_SERVERS : "要带 --clean 参数启动所选服务器吗？",
    START_CLEAN_SELECTED_CLUSTERS : "要带 --clean 参数启动所选集群吗？",
    STOP_SELECTED_APPS : "要停止所选应用程序的所有实例吗？",
    STOP_SELECTED_INSTANCES : "要停止所选应用程序实例吗？",
    STOP_SELECTED_SERVERS : "要停止所选服务器吗？",
    STOP_SELECTED_CLUSTERS : "要停止所选集群吗？",
    RESTART_SELECTED_APPS : "要重新启动所选应用程序的所有实例吗？",
    RESTART_SELECTED_INSTANCES : "要重新启动所选应用程序实例吗？",
    RESTART_SELECTED_SERVERS : "要重新启动所选服务器吗？",
    RESTART_SELECTED_CLUSTERS : "要重新启动所选集群吗？",

    START_SERVERS_ON_HOSTS : "要在所选主机上启动所有服务器吗？",
    STOP_SERVERS_ON_HOSTS : "要在所选主机上停止所有服务器吗？",
    RESTART_SERVERS_ON_HOSTS : "要在所选主机上重新启动所有正在运行的服务器吗？",

    SELECT_APPS_TO_START : "请选择要启动的已停止应用程序。",
    SELECT_APPS_TO_STOP : "请选择要停止的已启动应用程序。",
    SELECT_APPS_TO_RESTART : "请选择要重新启动的已启动应用程序。",
    SELECT_INSTANCES_TO_START : "请选择要启动的已停止应用程序实例。",
    SELECT_INSTANCES_TO_STOP : "请选择要停止的已启动应用程序实例。",
    SELECT_INSTANCES_TO_RESTART : "请选择要重新启动的已启动应用程序实例。",
    SELECT_SERVERS_TO_START : "请选择要启动的已停止服务器。",
    SELECT_SERVERS_TO_STOP : "请选择要停止的已启动服务器。",
    SELECT_SERVERS_TO_RESTART : "请选择要重新启动的已启动服务器。",
    SELECT_CLUSTERS_TO_START : "请选择要启动的已停止集群。",
    SELECT_CLUSTERS_TO_STOP : "请选择要停止的已启动集群。",
    SELECT_CLUSTERS_TO_RESTART : "请选择要重新启动的已启动集群。",

    STATUS : "状态",
    STATE : "状态：",
    NAME : "名称：",
    DIRECTORY : "目录",
    INFORMATION : "信息",
    DETAILS : "详细信息",
    ACTIONS : "操作",
    CLOSE : "关闭",
    HIDE : "隐藏",
    SHOW_ACTIONS : "显示操作",
    SHOW_SERVER_ACTIONS_LABEL : "服务器 {0} 操作",
    SHOW_APP_ACTIONS_LABEL : "应用程序 {0} 操作",
    SHOW_CLUSTER_ACTIONS_LABEL : "集群 {0} 操作",
    SHOW_HOST_ACTIONS_LABEL : "主机 {0} 操作",
    SHOW_RUNTIME_ACTIONS_LABEL : "运行时 {0} 操作",
    SHOW_SERVER_ACTIONS_MENU_LABEL : "服务器 {0} 操作菜单",
    SHOW_APP_ACTIONS_MENU_LABEL : "应用程序 {0} 操作菜单",
    SHOW_CLUSTER_ACTIONS_MENU_LABEL : "集群 {0} 操作菜单",
    SHOW_HOST_ACTIONS_MENU_LABEL : "主机 {0} 操作菜单",
    SHOW_RUNTIME_ACTIONS_MENU_LABEL : "运行时 {0} 操作菜单",
    SHOW_RUNTIMEONHOST_ACTIONS_MENU_LABEL : "主机上的运行时 {0} 操作菜单",
    SHOW_COLLECTION_MENU_LABEL : "集合 {0} 状态操作菜单",  // menu object id
    SHOW_SEARCH_MENU_LABEL : "搜索 {0} 状态操作菜单",  // menu object id


    // A bit odd to have sentence casing without punctuation?
    UNKNOWN_STATE : "{0}：未知状态", // resourceName
    UNKNOWN_STATE_APPS : "{0} 个应用程序处于未知状态", // quantity
    UNKNOWN_STATE_APP_INSTANCES : "{0} 个应用程序实例处于未知状态", // quantity
    UNKNOWN_STATE_SERVERS : "{0} 个服务器处于未知状态", // quantity
    UNKNOWN_STATE_CLUSTERS : "{0} 个集群处于未知状态", // quantity

    INSTANCES_NOT_RUNNING : "{0} 个应用程序实例未在运行", // quantity
    APPS_NOT_RUNNING : "{0} 个应用程序未在运行", // quantity
    SERVERS_NOT_RUNNING : "{0} 个服务器未在运行", // quantity
    CLUSTERS_NOT_RUNNING : "{0} 个集群未在运行", // quantity

    APP_STOPPED_ON_SERVER : "{0} 已在以下处于运行状态的服务器上停止：{1}", // appName, serverName(s)
    APPS_STOPPED_ON_SERVERS : "{0} 个应用程序已在以下处于运行状态的服务器上停止：{1}", // quantity, serverName(s)
    APPS_STOPPED_ON_SERVER : "{0} 个应用程序已在处于运行状态的服务器上停止。", // quantity
    NUMBER_RESOURCES : "{0} 个资源", // quantity
    NUMBER_APPS : "{0} 个应用程序", // quantity
    NUMBER_SERVERS : "{0} 个服务器", // quantity
    NUMBER_CLUSTERS : "{0} 个集群", // quantity
    NUMBER_HOSTS : "{0} 个主机", // quantity
    NUMBER_RUNTIMES : "{0} 运行时", // quantity
    SERVERS_INSERT : "服务器",
    INSERT_STOPPED_ON_INSERT : "{0} 已在处于运行状态的 {1} 上停止。", // NUMBER_APPS, SERVERS_INSERT

    APPSERVER_STOPPED_ON_SERVER : "{0} 已在处于运行状态的服务器 {1} 上停止", //appName, serverName
    APPCLUSTER_STOPPED_ON_SERVER : "集群 {1} 上的 {0} 已在以下处于运行状态的服务器上停止：{2}",  //appName, clusterName, serverName(s)

    INSTANCES_STOPPED_ON_SERVERS : "{0} 个应用程序实例已在处于运行状态的服务器上停止。", // quantity
    INSTANCE_STOPPED_ON_SERVERS : "{0}：应用程序实例未在运行", // serverNames

    NOT_ALL_APPS_RUNNING : "{0}：并非所有应用程序都在运行", // serverName[]
    NO_APPS_RUNNING : "{0}：没有应用程序正在运行", // serverName[]
    NOT_ALL_APPS_RUNNING_SERVERS : "{0} 个服务器上并非所有应用程序都在运行", // quantity
    NO_APPS_RUNNING_SERVERS : "{0} 个服务器上没有应用程序在运行", // quantity

    COUNT_OF_APPS_SELECTED : "已选择 {0} 个应用程序",
    RATIO_RUNNING : "{0} 正在运行", // ratio ex. 1/2

    RESOURCES_SELECTED : "{0} 已选择",

    NO_HOSTS_SELECTED : "未选择任何主机",
    NO_DEPLOY_RESOURCE : "没有资源用于部署安装",
    NO_TOPOLOGY : "不存在 {0}。",
    COUNT_OF_APPS_STARTED  : "{0} 个应用程序已启动",

    APPS_LIST : "{0} 个应用程序",
    EMPTY_MESSAGE : "{0}",  // used to build a list of comma separated resource names
    APPS_INSTANCES_RUNNING_OF_TOTAL : "{0}/{1} 个实例正在运行",
    HOSTS_SERVERS_RUNNING_OF_TOTAL : "{0}/{1} 个服务器正在运行",
    RESOURCE_ON_RESOURCE : "{1} 上的 {0}", // resource name, resource name
    RESOURCE_ON_SERVER_RESOURCE : "服务器 {1} 上的 {0}", // resource name, resource name
    RESOURCE_ON_CLUSTER_RESOURCE : "集群 {1} 上的 {0}", // resource name, resource name

    RESTART_DISABLED_FOR_ADMIN_CENTER: "已禁用此服务器的重新启动，因为其正在托管 Admin Center",
    ACTION_DISABLED_FOR_USER: "禁止对此资源执行操作，因为用户未经授权",

    RESTART_AC_TITLE: "不允许重新启动 Admin Center",
    RESTART_AC_DESCRIPTION: "{0} 正在提供 Admin Center。Admin Center 不能重新启动其自身。",
    RESTART_AC_MESSAGE: "所有其他所选择的服务器将会重新启动。",
    RESTART_AC_CLUSTER_MESSAGE: "将重新启动选择的所有其他集群。",

    STOP_AC_TITLE: "停止 Admin Center",
    STOP_AC_DESCRIPTION: "服务器 {0} 是运行 Admin Center 的 Collective Controller。停止该服务器可能影响 Liberty 集合体管理操作并使 Admin Center 不可用。",
    STOP_AC_MESSAGE: "要停止此控制器吗？",
    STOP_STANDALONE_DESCRIPTION: "服务器 {0} 运行 Admin Center。停止它会导致 Admin Center 不可用。",
    STOP_STANDALONE_MESSAGE: "要停止此服务器吗？",

    STOP_CONTROLLER_TITLE: "停止控制器",
    STOP_CONTROLLER_DESCRIPTION: "服务器 {0} 是 Collective Controller。停止它可能会影响 Liberty 集合体操作。",
    STOP_CONTROLLER_MESSAGE: "要停止此控制器吗？",

    STOP_AC_CLUSTER_TITLE: "停止集群 {0}",
    STOP_AC_CLUSTER_DESCRIPTION: "集群 {0} 包含运行 Admin Center 的 Collective Controller。停止它可能会影响 Liberty 集合体管理操作并使 Admin Center 不可用。",
    STOP_AC_CLUSTER_MESSAGE: "要停止此集群吗？",

    INVALID_URL: "页面不存在。",
    INVALID_APPLICATION: "应用程序 {0} 不再存在于此集合体中。", // application name
    INVALID_SERVER: "服务器 {0} 不再存在于此集合体中。", // server name
    INVALID_CLUSTER: "集群 {0} 不再存在于此集合体中。", // cluster name
    INVALID_HOST: "主机 {0} 不再存在于此集合体中。", // host name
    INVALID_RUNTIME: "运行时 {0} 不再存在于此集合体中。", // runtime name
    INVALID_INSTANCE: "应用程序实例 {0} 不再存在于此集合体中。", // application instance name
    GO_TO_DASHBOARD: "转至仪表板",
    VIEWED_RESOURCE_REMOVED: "糟糕！资源已移除或者不再可用。",

    OK_DEFAULT_BUTTON: "确定",
    CONNECTION_FAILED_MESSAGE: "与服务器的连接已丢失。页面不会再显示对环境的动态更改。请刷新页面以复原连接和动态更新。",
    ERROR_MESSAGE: "连接已中断",

    // Used by standalone stop message dialog
    STANDALONE_STOP_TITLE : '停止服务器',

    // Tags
    RELATED_RESOURCES: "相关资源",
    TAGS : "标签",
    TAG_BUTTON_LABEL : "标签 {0}",  // tag value
    TAGS_LABEL : "请输入使用逗号、空格、回车或跳格分隔的标签。",
    OWNER : "所有者",
    OWNER_BUTTON_LABEL : "所有者 {0}",  // owner value
    CONTACTS : "联系人",
    CONTACT_BUTTON_LABEL : "联系人 {0}",  // contact value
    PORTS : "端口",
    CONTEXT_ROOT : "上下文根",
    HTTP : "HTTP",
    HTTPS : "HTTPS",
    MORE : "更多",  // alt text for the ... button
    MORE_BUTTON_MENU : "{0} 更多菜单", // alt text for the menu
    NOTES: "注",
    NOTE_LABEL : "注释 {0}",  // note value
    SET_ATTRIBUTES: "标签和元数据",
    SETATTR_RUNTIME_NAME: "{1} 上的 {0}",  // runtime, host
    SAVE: "保存",
    TAGINVALIDCHARS: "字符“/”、“<”和“>”无效。",
    ERROR_GET_TAGS_METADATA: "该产品无法获取资源的当前标签和元数据。",
    ERROR_SET_TAGS_METADATA: "错误导致该产品不能设置标签和元数据。",
    METADATA_WILLBE_INHERITED: "已在应用程序上设置元数据，并在集群中的所有实例间进行共享。",
    ERROR_ALT: "错误",

    // Graph Warning Messages
    GRAPH_SERVER_NOT_STARTED: "此服务器的当前统计信息不可用，因为其已经停止。启动服务器以开始对其进行监视。",
    GRAPH_SERVER_HOSTING_APP_NOT_STARTED: "此应用程序的当前统计信息不可用，因为其相关服务器已经停止。启动服务器以开始监视此应用程序。",
    GRAPH_FEATURES_NOT_CONFIGURED: "尚无任何信息！通过选择“编辑”图标并添加度量来监视此资源。",
    NO_GRAPHS_AVAILABLE: "没有可用度量可供添加。请尝试安装另外的监视功能以增加更多可用度量。",
    NO_APPS_GRAPHS_AVAILABLE: "没有可用度量可供添加。请尝试安装另外的监视功能以增加更多可用度量。此外，确保正使用应用程序。",
    GRAPH_CONFIG_NOT_SAVED_TITLE : "未保存更改",
    GRAPH_CONFIG_NOT_SAVED_DESCR : "您具有未保存的更改。如果您移动到另一页面，那么更改将丢失。",
    GRAPH_CONFIG_NOT_SAVED_MSG : "要保存您的更改吗？",

    NO_CPU_STATS_AVAILABLE : "此服务器的 CPU 使用率统计信息不可用。",

    // Server Config
    CONFIG_NOT_AVAILABLE: "要启用此视图，请安装服务器配置工具。",
    SAVE_BEFORE_CLOSING_DIALOG_MESSAGE: "关闭之前要保存对 {0} 的更改吗？",
    SAVE: "保存",
    DONT_SAVE: "不要保存",

    // Maintenance mode
    ENABLE_MAINTENANCE_MODE: "启用维护方式",
    DISABLE_MAINTENANCE_MODE: "禁用维护方式",
    ENABLE_MAINTENANCE_MODE_DIALOG_TITLE: "启用维护方式",
    DISABLE_MAINTENANCE_MODE_DIALOG_TITLE: "禁用维护方式",
    ENABLE_MAINTENANCE_MODE_HOST_DESCRIPTION: "在主机及其所有服务器（{0} 个服务器）上启用维护方式",
    ENABLE_MAINTENANCE_MODE_HOSTS_DESCRIPTION: "在主机及其所有服务器（{0} 个服务器）上启用维护方式",
    ENABLE_MAINTENANCE_MODE_SERVER_DESCRIPTION: "在服务器上启用维护方式",
    ENABLE_MAINTENANCE_MODE_SERVERS_DESCRIPTION: "在服务器上启用维护方式",
    DISABLE_MAINTENANCE_MODE_HOST_DESCRIPTION: "在主机及其所有服务器（{0} 个服务器）上禁用维护方式",
    DISABLE_MAINTENANCE_MODE_SERVER_DESCRIPTION: "在服务器上禁用维护方式",
    BREAK_AFFINITY_LABEL: "中断与活动会话的亲缘关系",
    ENABLE_MAINTENANCE_MODE_DIALOG_ENABLE_BUTTON_LABEL: "启用",
    DISABLE_MAINTENANCE_MODE_DIALOG_ENABLE_BUTTON_LABEL: "禁用",
    MAINTENANCE_MODE: "维护方式",
    ENABLING_MAINTENANCE_MODE: "启用维护方式",
    MAINTENANCE_MODE_ENABLED: "已启用维护方式",
    MAINTENANCE_MODE_ALTERNATE_SERVERS_NOT_STARTED: "未启用维护方式，因为备用服务器未启动。",
    MAINTENANCE_MODE_SELECT_FORCE_MESSAGE: "选择“强制实施”以启用维护方式，而不启动备用服务器。强制实施可能会中断自动扩展策略。",
    MAINTENANCE_MODE_FAILED: "无法启用维护方式。",
    MAINTENANCE_MODE_FORCE_LABEL: "强制实施",
    MAINTENANCE_MODE_CANCEL_LABEL: "取消",
    MAINTENANCE_MODE_SERVERS_IN_MAINTENANCE_MODE: "{0} 个服务器当前处于维护方式。",
    MAINTENANCE_MODE_ENABLING_MAINTENANCE_MODE_ON_ALL_HOST_SERVERS_EQUAL_OR_LESS_THEN_10: "在所有主机服务器上启用维护方式。",
    MAINTENANCE_MODE_ENABLING_MAINTENANCE_MODE_ON_ALL_HOST_SERVERS_MORE_THAN_10: "在所有主机服务器上启用维护方式。显示“服务器”视图以查看状态。",

    SERVER_API_DOCMENTATION: "查看服务器 API 定义",

    // objectView title
    TITLE_FOR_CLUSTER: "集群 {0}", // cluster name
    TITLE_FOR_HOST: "主机 {0}", // host name

    // objectView descriptor
    COLLECTIVE_CONTROLLER_DESCRIPTOR: "集合体控制器",
    LIBERTY_SERVER : "Liberty 服务器",
    NODEJS_SERVER : "Node.js 服务器",
    CONTAINER : "容器",
    CONTAINER_LIBERTY : "Liberty",
    CONTAINER_DOCKER : "Docker",
    CONTAINER_NODEJS : "Node.js",
    LIBERTY_IN_DOCKER_DESCRIPTOR : "Docker 容器中的 Liberty 服务器",
    NODEJS_IN_DOCKER_DESCRIPTOR : "Docker 容器中的 Node.js 服务器",
    RUNTIME_LIBERTY : "Liberty 运行时",
    RUNTIME_NODEJS : "Node.js 运行时",
    RUNTIME_DOCKER : "Docker 容器中的运行时"

});
