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
    "CLEAR_SEARCH": "清除搜索输入",
    "CLEAR_FILTER": "清除过滤器",
    "CLICK_TO_SORT": "单击以对列排序",
    "CLOSE": "关闭",
    "COPY_TO_CLIPBOARD": "复制到剪贴板",
    "COPIED_TO_CLIPBOARD": "已复制到剪贴板",
    "DELETE": "删除",
    "DONE": "完成",
    "EDIT": "编辑",
    "FALSE": "False",
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
    "TABLE_BATCH_BAR": "表操作栏",
    "TABLE_FIELD_SORT_ASC": "表按 {0} 以升序进行排序。",   // {0} - column name (ie. 'Name', 'Client ID')
    "TABLE_FIELD_SORT_DESC": "表按 {0} 以降序进行排序。", // {0} - column name (ie. 'Name', 'Client ID')
    "TRUE": "True",
    "TRY_AGAIN": "请重试...",
    "UPDATE": "更新",

    // Common Column Names
    "CLIENT_NAME_COL": "客户机名称",
    "EXPIRES_COL": "到期日期",
    "ISSUED_COL": "颁发日期",
    "NAME_COL": "名称",
    "TYPE_COL": "类型",

    // Token Manager
    // 'app-token' and 'app-password' are keywords - don't translate
    // name - user defined name given to the app-password/app-token; user id - user's login name
    "TOKEN_MGR_TITLE": "删除令牌",
    "TOKEN_MGR_DESC": "删除所指定用户的 app-password 和 app-token。",
    "TOKEN_MGR_SEARCH_PLACEHOLDER": "输入用户标识",
    "TABLE_FILLED_WITH": "表已更新，将显示属于 {1} 的 {0} 项认证。",  // 0 - number of entries in table; 1 - user id
    "DELETE_SELECTED": "删除所选 app-password 和 app-token。",
    "DELETE_ARIA": "删除名为 {1} 的 {0}",         // 0 - 'app-password' or 'app-token'; 1 - name
    "DELETE_PW": "删除此 app-password",
    "DELETE_TOKEN": "删除此 app-token",
    "DELETE_FOR_USERID": "对应 {1} 的 {0}",                // 0 - name; 1 - user id
    "DELETE_WARNING_PW": "此操作将移除当前分配的 app-password。",
    "DELETE_WARNING_TOKEN": "此操作将移除当前分配的 app-token。",
    "DELETE_MANY": "删除 App-Password/App-Token",
    "DELETE_MANY_FOR": "已分配给 {0}",              // 0 - user id
    "DELETE_ONE_MESSAGE": "此操作将删除所选 app-password/app-token。",
    "DELETE_MANY_MESSAGE": "此操作将删除 {0} 个所选 app-password/app-token。",  // 0 - number
    "DELETE_ALL_MESSAGE": "此操作将删除属于 {0} 的所有 app-password/app-token。", // 0 - user id
    "DELETE_NONE": "选择要删除的项",
    "DELETE_NONE_MESSAGE": "选中复选框以指示应删除的 app-password 或 app-token。",
    "SINGLE_ITEM_SELECTED": "已选择 1 项",
    "ITEMS_SELECTED": "已选择 {0} 项",            // 0 - number
    "SELECT_ALL_AUTHS": "选择此用户的所有 app-password 和 app-token。",
    "SELECT_SPECIFIC": "选择删除名为 {1} 的 {0}。",  // 0 - 'app-password' or 'app-token; 1 - name
    "NO_QUERY": "想查找某些内容？请输入用户标识以查看其 app-password 和 app-token。",
    "GENERIC_FETCH_FAIL": "检索 {0} 时出错",      // 0 - 'App-Passwords' or 'App-Tokens'
    "GENERIC_FETCH_FAIL_MSG": "无法获取属于 {1} 的 {0} 列表。", // 0 - 'app-passwords' or 'app-tokens; 1 - user id
    "GENERIC_DELETE_FAIL": "删除 {0} 时出错",       // 0 - 'App-Password' or 'App-Token'
    "GENERIC_DELETE_FAIL_MSG": "删除名为 {1} 的 {0} 时发生了错误。",  // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_DELETE_ALL_FAIL_MSG": "删除对应 {1} 的 {0} 时发生了错误。",     // 0 - 'app-passwords' or 'app-tokens'; 1 - user id
    "GENERIC_DELETE_FAIL_NOTYPES": "删除时出错",
    "GENERIC_DELETE_FAIL_NOTYPES_ONE_MSG": "删除以下 app-password 或 app-token 时发生了错误。",
    "GENERIC_DELETE_FAIL_NOTYPES_MSG": "删除以下 {0} 个 app-password 和 app-token 时发生了错误。",  // 0 - number
    "IDENTIFY_AUTH": "{0} {1}",   // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_FETCH_ALL_FAIL": "检索认证时出错",
    "GENERIC_FETCH_ALL_FAIL_MSG": "无法获取属于 {0} 的 app-password 和 app-token 列表。",   // 0 - user id
    "GENERIC_NOT_CONFIGURED": "未配置客户机",
    "GENERIC_NOT_CONFIGURED_MSG": "未配置 appPasswordAllowed 和 appTokenAllowed 客户机属性。无法检索数据。"
};
