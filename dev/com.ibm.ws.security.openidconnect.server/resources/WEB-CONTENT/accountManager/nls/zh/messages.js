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
    "ADD_NEW": "添加新项",
    "CANCEL": "取消",
    "CLEAR": "清除搜索输入",
    "CLICK_TO_SORT": "单击以对列排序",
    "CLOSE": "关闭",
    "COPY_TO_CLIPBOARD": "复制到剪贴板",
    "COPIED_TO_CLIPBOARD": "已复制到剪贴板",
    "DELETE": "删除",
    "DONE": "完成",
    "EDIT": "编辑",
    "GENERATE": "生成",
    "LOADING": "正在加载",
    "LOGOUT": "注销",
    "NEXT_PAGE": "下一页",
    "NO_RESULTS_FOUND": "找不到任何结果",
    "PAGES": "第 {0} 页（共 {1} 页）",   // {0} - current page number; {1} - total pages
    "PAGE_SELECT": "选择要查看的页码",
    "PREVIOUS_PAGE": "上一页",
    "PROCESSING": "处理",
    "REGENERATE": "重新生成",
    "REGISTER": "注册",
    "TRY_AGAIN": "请重试...",
    "UPDATE": "更新",

    // Common Column Names
    "EXPIRES_COL": "到期日期",
    "ISSUED_COL": "颁发日期",
    "NAME_COL": "名称",
    "TYPE_COL": "类型",

    // Account Manager
    // 'app-token' and 'app-password' are keywords - don't translate
    // name - the user defined name given the app-password or app-token
    "ACCT_MGR_TITLE": "管理个人令牌",
    "ACCT_MGR_DESC": "创建、删除和重新生成应用程序密码及应用程序令牌。",
    "ADD_NEW_AUTHENTICATION": "添加新的应用程序密码或应用程序令牌",
    "NAME_IDENTIFIER": "名称：{0}",
    "ADD_NEW_TITLE": "注册新的认证",
    "NOT_GENERATED_PLACEHOLDER": "未生成",
    "REGENERATE_APP_PASSWORD": "重新生成应用程序密码",
    "REGENERATE_PW_WARNING": "此操作将覆盖当前应用程序密码。",
    "REGENERATE_PW_PLACEHOLDER": "先前在 {0} 生成的密码",        // 0 - date
    "REGENERATE_APP_TOKEN": "重新生成应用程序令牌",
    "REGENERATE_TOKEN_WARNING": "此操作将覆盖当前应用程序令牌。",
    "REGENERATE_TOKEN_PLACEHOLDER": "先前在 {0} 生成的令牌",        // 0 - date
    "DELETE_PW": "删除此应用程序密码",
    "DELETE_TOKEN": "删除此应用程序令牌",
    "DELETE_WARNING_PW": "此操作将移除当前分配的应用程序密码。",
    "DELETE_WARNING_TOKEN": "此操作将移除当前分配的应用程序令牌。",
    "REGENERATE_ARIA": "重新生成对应 {1} 的 {0}",     // 0 - 'app-password' or 'app-token'; 1 - name
    "DELETE_ARIA": "删除名为 {1} 的 {0}",       // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_GENERATE_FAIL": "生成 {0} 时出错", // 0 - 'App-Password' or 'App-Token'
    "GENERIC_GENERATE_FAIL_MSG": "删除名称为 {1} 的新 {0} 时发生了错误。",  // 0 - 'app-password' or 'app-token'; 1 - name
    "ERR_NAME": "该名称已与 {0} 相关联，或者该名称太长。", // 0 - 'app-password' or 'app-token'
    "GENERIC_DELETE_FAIL": "删除 {0} 时出错",     // 0 - 'App-Password' or 'App-Token'
    "GENERIC_DELETE_FAIL_MSG": "删除名为 {1} 的 {0} 时发生了错误。",  // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_REGENERATE_FAIL": "重新生成 {0} 时出错",  // 0 - 'App-Password' or 'App-Token'
    "GENERIC_REGENERATE_FAIL_MSG": "重新生成名为 {1} 的 {0} 时发生了错误。",  // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_REGENERATE_FAIL_CREATE_MSG": "重新生成名为 {1} 的 {0} 时发生了错误。{0} 已被删除，但无法重新创建。", // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_FETCH_FAIL": "检索认证时出错",
    "GENERIC_FETCH_FAIL_MSG": "无法获取应用程序密码或应用程序令牌的当前列表。",
    "GENERIC_NOT_CONFIGURED": "未配置客户机",
    "GENERIC_NOT_CONFIGURED_MSG": "未配置 appPasswordAllowed 和 appTokenAllowed 客户机属性。无法检索数据。",
    "APP_PASSWORD_NOT_CONFIGURED": "未配置 appPasswordAllowed 客户机属性。",  // 'appPasswordAllowed' is a config option. Do not translate.
    "APP_TOKEN_NOT_CONFIGURED": "未配置 appTokenAllowed 客户机属性。"         // 'appTokenAllowed' is a config option.  Do not translate.
};
