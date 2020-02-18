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
      ACCOUNTING_STRING : "记帐字符串",
      SEARCH_RESOURCE_TYPE_ALL : "全部",
      SEARCH : "搜索",
      JAVA_BATCH_SEARCH_BOX_LABEL : "选择“添加搜索条件”按钮，输入搜索条件然后指定一个值。",
      SUBMITTED : "已提交",
      JMS_QUEUED : "已排队的 JMS",
      JMS_CONSUMED : "已使用的 JMS",
      JOB_PARAMETER : "作业参数",
      DISPATCHED : "已分派",
      FAILED : "已失败",
      STOPPED : "已停止",
      COMPLETED : "已完成",
      ABANDONED : "已放弃",
      STARTED : "已启动",
      STARTING : "正在启动",
      STOPPING : "正在停止",
      REFRESH : "刷新",
      INSTANCE_STATE : "实例状态",
      APPLICATION_NAME : "应用程序名称",
      APPLICATION: "应用程序",
      INSTANCE_ID : "实例标识",
      LAST_UPDATE : "最近一次更新时间",
      LAST_UPDATE_RANGE : "最近一次更新范围",
      LAST_UPDATED_TIME : "最近一次更新时间",
      DASHBOARD_VIEW : "仪表板视图",
      HOMEPAGE : "主页",
      JOBLOGS : "作业日志",
      QUEUED : "已排队",
      ENDED : "已结束",
      ERROR : "错误",
      CLOSE : "关闭",
      WARNING : "警告",
      GO_TO_DASHBOARD: "转至仪表板",
      DASHBOARD : "仪表板",
      BATCH_JOB_NAME: "批处理作业名称",
      SUBMITTER: "提交者",
      BATCH_STATUS: "批处理状态",
      EXECUTION_ID: "作业执行标识",
      EXIT_STATUS: "退出状态",
      CREATE_TIME: "创建时间",
      START_TIME: "开始时间",
      END_TIME: "结束时间",
      SERVER: "服务器",
      SERVER_NAME: "服务器名称",
      SERVER_USER_DIRECTORY: "用户目录",
      SERVERS_USER_DIRECTORY: "服务器的用户目录",
      HOST: "主机",
      NAME: "名称",
      JOB_PARAMETERS: "作业参数",
      JES_JOB_NAME: "JES 作业名",
      JES_JOB_ID: "JES 作业标识",
      ACTIONS: "操作",
      VIEW_LOG_FILE: "查看日志文件",
      STEP_NAME: "步骤名称",
      ID: "标识",
      PARTITION_ID: "分区 {0}",                               // Partition ID number
      VIEW_EXECUTION_DETAILS: "查看作业执行 {0} 详细信息",    // Job Execution ID number
      PARENT_DETAILS: "父代详细信息",
      TIMES: "时间",      // Heading on section referencing create, start, and end timestamps
      STATUS: "状态",
      SEARCH_ON: "选择以过滤 {1} {0}",                    // {0} Value, {1} Column name
      SEARCH_PILL_INPUT : "输入搜索条件。",
      BREADCRUMB_JOB_INSTANCE : "作业实例 {0}",                // Job Instance ID
      BREADCRUMB_JOB_EXECUTION : "作业执行 {0}",              // Job Execution ID
      BREADCRUMB_JOB_LOG : "作业日志 {0}",
      BATCH_SEARCH_CRITERIA_INVALID : "搜索条件无效。",
      ERROR_CANNOT_HAVE_MULTIPLE_SEARCHBY : "搜索条件不能包含具有“{0}”多个参数的过滤器。", // {0} will be another translated message key like SUBMITTER and BATCH_JOB_NAME

      INSTANCES_TABLE_IDENTIFIER: "作业实例表",
      EXECUTIONS_TABLE_IDENTIFIER: "作业执行表",
      STEPS_DETAILS_TABLE_IDENTIFIER: "步骤详细信息表",
      LOADING_VIEW : "页面当前正在加载信息",
      LOADING_VIEW_TITLE : "正在加载视图",
      LOADING_GRID : "等待要从服务器返回的搜索结果",
      PAGENUMBER : "页码",
      SELECT_QUERY_SIZE: "选择查询大小",
      LINK_EXPLORE_HOST: "选择以在浏览器工具中查看主机 {0} 的详细信息。",      // Host name
      LINK_EXPLORE_SERVER: "选择以在浏览器工具中查看服务器 {0} 的详细信息。",  // Server name

      //ACTIONS
      RESTART: "重新启动",
      STOP: "停止",
      PURGE: "清除",
      OK_BUTTON_LABEL: "确定",
      INSTANCE_ACTIONS_BUTTON_LABEL: "作业实例 {0} 的操作",     // Job Instance ID number
      INSTANCE_ACTIONS_MENU_LABEL:  "作业实例操作菜单",

      RESTART_INSTANCE_MESSAGE: "要重新启动与作业实例 {0} 关联的最新作业执行吗？",     // Job Instance ID number
      STOP_INSTANCE_MESSAGE: "要停止与作业实例 {0} 关联的最新作业执行吗？",           // Job Instance ID number
      PURGE_INSTANCE_MESSAGE: "要清除与作业实例 {0} 关联的所有数据库条目和作业日志吗？",     // Job Instance ID number
      PURGE_JOBSTORE_ONLY: "仅清除作业存储",

      RESTART_INST_ERROR_MESSAGE: "重新启动请求失败。",
      STOP_INST_ERROR_MESSAGE: "停止请求失败。",
      PURGE_INST_ERROR_MESSAGE: "清除请求已失败。",
      ACTION_REQUEST_ERROR_MESSAGE: "操作请求失败，状态码为 {0}。URL：{1}",  // Status Code number, URL string

      // RESTART JOB WITH PARAMETERS DIALOG
      REUSE_PARMS_TOGGLE_LABEL: "复用先前执行中的参数",
      JOB_PARAMETERS_EMPTY: "未选择“{0}”时，请使用此区域来输入作业参数。",   // 0 - Checkbox label - REUSE_PARMS_TOGGLE_LABEL
      JOB_PARAMETER_NAME: "参数名称",
      JOB_PARAMETER_VALUE: "参数值",
      PARM_NAME_COLUMN_HEADER: "参数",
      PARM_VALUE_COLUMN_HEADER: "值",
      PARM_ADD_ICON_TITLE: "添加参数",
      PARM_REMOVE_ICON_TITLE: "移除参数",
      PARMS_ENTRY_ERROR: "参数名称是必需的。",
      JOB_PARAMETER_CREATE: "选择 {0} 以将参数添加至此作业实例的下一次执行过程。",  // 0 - Button label
      JOB_PARAMETER_CREATE_BUTTON: "表头中的添加参数按钮。",

      //JOB LOGS PAGE MESSAGES
      JOBLOGS_LOG_CONTENT : "作业日志内容",
      FILE_DOWNLOAD : "文件下载",
      DOWNLOAD_DIALOG_DESCRIPTION : "要下载此日志文件吗？",
      INCLUDE_ALL_LOGS : "包含作业执行的所有日志文件",
      LOGS_NAVIGATION_BAR : "作业日志导航栏",
      DOWNLOAD : "下载",
      LOG_TOP : "日志的顶部",
      LOG_END : "日志的底部",
      PREVIOUS_PAGE : "上一页",
      NEXT_PAGE : "下一页",
      DOWNLOAD_ARIA : "下载文件",

      //Error messages for popups
      REST_CALL_FAILED : "对访存数据的调用失败。",
      NO_JOB_EXECUTION_URL : "URL 中未提供任何作业执行号码，或者此实例未显示任何作业执行日志。",
      NO_VIEW : "URL 错误：不存在此类视图。",
      WRONG_TOOL_ID : "URL 的查询字符串不是以工具标识 {0} 而是以 {1} 开头。",   // {0} and {1} are both Strings
      URL_NO_LOGS : "URL 错误：不存在任何日志。",
      NOT_A_NUMBER : "URL 错误：{0} 必须为数字。",                                                // {0} is a field name
      PARAMETER_REPETITION : "URL 错误：{0} 在参数中只能出现一次。",                   // {0} is a field name
      URL_PAGE_PARAMS_ERROR : "URL 错误：页面参数超出范围。",
      INVALID_PARAMETER : "URL 错误：{0} 不是有效的参数。",                                       // {0} is a String
      URL_MULTIPLE_ATTRIBUTES : "URL 错误：URL 可以指定作业执行或作业实例，但不同时指定两者。",
      MISSING_EXECUTION_ID_PARAM : "缺少需要的执行标识参数。",
      PERSISTENCE_CONFIGURATION_REQUIRED : "在使用 Java 批处理工具之前，需要配置 Java 批处理持久性数据库。",
      IGNORED_SEARCH_CRITERIA : "在结果中将忽略以下过滤条件：{0}",

      GRIDX_SUMMARY_TEXT : "显示最新的 ${0} 个作业实例"

});

