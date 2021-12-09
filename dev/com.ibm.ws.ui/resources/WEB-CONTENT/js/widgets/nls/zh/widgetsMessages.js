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
    LIBERTY_HEADER_TITLE: "Liberty Admin Center",
    LIBERTY_HEADER_PROFILE: "首选项",
    LIBERTY_HEADER_LOGOUT: "注销",
    LIBERTY_HEADER_LOGOUT_USERNAME: "注销 {0}",
    TOOLBOX_BANNER_LABEL: "{0} 条幅",  // TOOLBOX_TITLE
    TOOLBOX_TITLE: "工具箱",
    TOOLBOX_TITLE_LOADING_TOOL: "正在加载工具...",
    TOOLBOX_TITLE_EDIT: "编辑工具箱",
    TOOLBOX_EDIT: "编辑",
    TOOLBOX_DONE: "完成",
    TOOLBOX_SEARCH: "过滤",
    TOOLBOX_CLEAR_SEARCH: "清除过滤条件",
    TOOLBOX_END_SEARCH: "结束过滤",
    TOOLBOX_ADD_TOOL: "添加工具",
    TOOLBOX_ADD_CATALOG_TOOL: "添加工具",
    TOOLBOX_ADD_BOOKMARK: "添加书签",
    TOOLBOX_REMOVE_TITLE: "移除工具 {0}",
    TOOLBOX_REMOVE_TITLE_NO_TOOL_NAME: "移除工具",
    TOOLBOX_REMOVE_MESSAGE: "确定要移除 {0} 吗？",
    TOOLBOX_BUTTON_REMOVE: "移除",
    TOOLBOX_BUTTON_OK: "确定",
    TOOLBOX_BUTTON_GO_TO: "转至工具箱",
    TOOLBOX_BUTTON_CANCEL: "取消",
    TOOLBOX_BUTTON_BGTASK: "后台任务",
    TOOLBOX_BUTTON_BACK: "返回",
    TOOLBOX_BUTTON_USER: "用户",
    TOOLBOX_ADDTOOL_ERROR_MESSAGE: "添加工具 {0} 时发生错误：{1}",
    TOOLBOX_REMOVETOOL_ERROR_MESSAGE: "移除工具 {0} 时发生错误：{1}",
    TOOLBOX_GET_ERROR_MESSAGE: "在检索工具箱中的工具时发生错误：{0}",
    TOOLCATALOG_TITLE: "工具目录",
    TOOLCATALOG_ADDTOOL_TITLE: "添加工具",
    TOOLCATALOG_ADDTOOL_MESSAGE: "确定要将工具 {0} 添加至工具箱吗？",
    TOOLCATALOG_BUTTON_ADD: "添加",
    TOOL_FRAME_TITLE: "工具框架",
    TOOL_DELETE_TITLE: "删除 {0}",
    TOOL_ADD_TITLE: "添加 {0}",
    TOOL_ADDED_TITLE: "已添加 {0}",
    TOOL_LAUNCH_ERROR_MESSAGE_TITLE: "未找到工具",
    TOOL_LAUNCH_ERROR_MESSAGE: "所请求工具未启动，因为该工具不在目录中。",
    LIBERTY_UI_ERROR_MESSAGE_TITLE: "错误",
    LIBERTY_UI_WARNING_MESSAGE_TITLE: "警告",
    LIBERTY_UI_INFO_MESSAGE_TITLE: "信息",
    LIBERTY_UI_CATALOG_GET_ERROR: "获取目录时发生错误：{0}",
    LIBERTY_UI_CATALOG_GET_TOOL_ERROR: "从目录获取工具 {0} 时发生错误：{1}",
    PREFERENCES_TITLE: "首选项",
    PREFERENCES_SECTION_TITLE: "首选项",
    PREFERENCES_ENABLE_BIDI: "启用双向支持",
    PREFERENCES_BIDI_TEXTDIR: "文本方向",
    PREFERENCES_BIDI_TEXTDIR_LTR: "左到右",
    PREFERENCES_BIDI_TEXTDIR_RTL: "右到左",
    PREFERENCES_BIDI_TEXTDIR_CONTEXTUAL: "上下文",
    PREFERENCES_SET_ERROR_MESSAGE: "在设置工具箱中的用户首选项时发生错误：{0}",
    BGTASKS_PAGE_LABEL: "后台任务",
    BGTASKS_DEPLOYMENT_INSTALLATION_POPUP: "部署安装 {0}", // {0} is the token number associated with the deployment
    BGTASKS_DEPLOYMENT_INSTALLATION: "部署安装 {0} - {1}", // {0} is the token number associated with the deployment, {1} is the server name
    BGTASKS_STATUS_RUNNING: "正在运行",
    BGTASKS_STATUS_FAILED: "已失败",
    BGTASKS_STATUS_SUCCEEDED: "已完成", 
    BGTASKS_STATUS_WARNING: "已部分成功",
    BGTASKS_STATUS_PENDING: "正在暂 挂",
    BGTASKS_INFO_DIALOG_TITLE: "详细信息",
    //BGTASKS_INFO_DIALOG_DESC: "Description:",
    BGTASKS_INFO_DIALOG_STDOUT: "标准输出：",
    BGTASKS_INFO_DIALOG_STDERR: "标准错误：",
    BGTASKS_INFO_DIALOG_EXCEPTION: "异常：",
    //BGTASKS_INFO_DIALOG_RETURN_CODE: "Return code:",
    BGTASKS_INFO_DIALOG_RESULT: "结果：",
    BGTASKS_INFO_DIALOG_DEPLOYED_ARTIFACT_NAME: "服务器名称：",
    BGTASKS_INFO_DIALOG_DEPLOYED_USER_DIR: "用户目录：",
    BGTASKS_POPUP_RUNNING_TASK_TITLE: "活动后台任务",
    BGTASKS_POPUP_RUNNING_TASK_NONE: "无",
    BGTASKS_POPUP_RUNNING_TASK_NO_ACTIVE_TASK: "没有活动的后台任务",
    BGTASKS_DISPLAY_BUTTON: "任务详细信息和历史记录",
    BGTASKS_EXPAND: "展开此区段",
    BGTASKS_COLLAPSE: "折叠此区段",
    PROFILE_MENU_HELP_TITLE: "帮助",
    DETAILS_DESCRIPTION: "说明",
    DETAILS_OVERVIEW: "概述",
    DETAILS_OTHERVERSIONS: "其他版本",
    DETAILS_VERSION: "版本：{0}",
    DETAILS_UPDATED: "已更新：{0}",
    DETAILS_NOTOPTIMIZED: "没有为当前设备优化。",
    DETAILS_ADDBUTTON: "添加至我的工具箱",
    DETAILS_OPEN: "打开",
    DETAILS_CATEGORY: "类别 {0}",
    DETAILS_ADDCONFIRM: "工具 {0} 已成功添加至工具箱。",
    CONFIRM_DIALOG_HELP: "帮助",
    YES_BUTTON_LABEL: "{0} 是",  // insert is dialog title
    NO_BUTTON_LABEL: "{0} 否",  // insert is dialog title

    YES: "是",
    NO: "否",

    TOOL_OIDC_ACCESS_DENIED: "该用户所具有的角色无权完成此请求。",
    TOOL_OIDC_GENERIC_ERROR: "发生了错误。请查看日志中的该错误以了解更多信息。",
    TOOL_DISABLE: "该用户无权使用此工具。只有具有管理员角色的用户才有权使用此工具。" 
});
