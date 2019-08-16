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

    // Client Admin
    "CLIENT_ADMIN_TITLE": "管理 OAuth 客户机",
    "CLIENT_ADMIN_DESC": "使用此工具来添加和编辑客户机以及重新生成客户机密钥。",
    "CLIENT_ADMIN_SEARCH_PLACEHOLDER": "针对 OAuth 客户机名称进行过滤",
    "ADD_NEW_CLIENT": "添加新的 OAuth 客户机。",
    "CLIENT_NAME": "客户机名称",
    "CLIENT_ID": "客户机标识",
    "EDIT_ARIA": "编辑 {0} OAuth 客户机",      // {0} - name
    "DELETE_ARIA": "删除 {0} OAuth 客户机",  // {0} - name
    "CLIENT_SECRET": "客户机密钥",
    "GRANT_TYPES": "授权类型",
    "SCOPE": "作用域",
    "PREAUTHORIZED_SCOPE": "预授权范围（可选）",
    "REDIRECT_URLS": "重定向 URL（可选）",
    "CLIENT_SECRET_CHECKBOX": "重新生成客户机密钥",
    "NONE_SELECTED": "未选择任何项",
    "MODAL_EDIT_TITLE": "编辑 OAuth 客户机",
    "MODAL_REGISTER_TITLE": "注册新的 OAuth 客户机",
    "MODAL_SECRET_REGISTER_TITLE": "已保存 OAuth 注册",
    "MODAL_SECRET_UPDATED_TITLE": "已更新 OAuth 注册",
    "MODAL_DELETE_CLIENT_TITLE": "删除此 OAuth 客户机",
    "RESET_GRANT_TYPE": "取消选中所有已选授权类型。",
    "SELECT_ONE_GRANT_TYPE": "选择至少一个授权类型",
    "SPACE_HELPER_TEXT": "空格分隔的值列表",
    "REDIRECT_URL_HELPER_TEXT": "空格分隔的绝对重定向 URL 列表",
    "DELETE_OAUTH_CLIENT_DESC": "此操作将已注册的客户机从客户机注册服务中删除。",
    "REGISTRATION_SAVED": "已生成并分配客户机标识和客户机密钥。",
    "REGISTRATION_UPDATED": "已针对此客户机生成并分配新的客户机密钥。",
    "COPY_CLIENT_ID": "将客户机标识复制到剪贴板",
    "COPY_CLIENT_SECRET": "将客户机密钥复制到剪贴板",
    "REGISTRATION_UPDATED_NOSECRET": "已更新 {0} OAuth 客户机。",                 // {0} - client name
    "ERR_MULTISELECT_GRANT_TYPES": "必须选择至少一个授权类型。",
    "ERR_REDIRECT_URIS": "值必须为绝对 URI。",
    "GENERIC_REGISTER_FAIL": "注册 OAuth 客户机时出错",
    "GENERIC_UPDATE_FAIL": "更新 OAuth 客户机时出错",
    "GENERIC_DELETE_FAIL": "删除 OAuth 客户机时出错",
    "GENERIC_MISSING_CLIENT": "检索 OAuth 客户机时出错",
    "GENERIC_REGISTER_FAIL_MSG": "注册 {0} OAuth 客户机时发生了错误。",  // {0} - client name
    "GENERIC_UPDATE_FAIL_MSG": "更新 {0} OAuth 客户机时发生了错误。",       // {0} - client name
    "GENERIC_DELETE_FAIL_MSG": "删除 {0} OAuth 客户机时发生了错误。",       // {0} - client name
    "GENERIC_MISSING_CLIENT_MSG": "找不到标识为 {1} 的 OAuth 客户机 {0}。",     // {0} - client name; {1} - an ID
    "GENERIC_RETRIEVAL_FAIL_MSG": "检索 {0} OAuth 客户机上的信息时发生了错误。", // {0} - client name
    "GENERIC_GET_CLIENTS_FAIL": "检索 OAuth 客户机时出错",
    "GENERIC_GET_CLIENTS_FAIL_MSG": "检索 OAuth 客户机列表时发生了错误。",

    "RESET_SELECTION": "取消选中所有已选 {0}",     // {0} - field name (ie 'Grant types')
    "NUMBER_SELECTED": "已选 {0} 的数目",     // {0} - field name
    "OPEN_LIST": "打开 {0} 列表。",                   // {0} - field name
    "CLOSE_LIST": "关闭 {0} 列表。",                 // {0} - field name
    "ENTER_PLACEHOLDER": "输入值",
    "ADD_VALUE": "添加元素",
    "REMOVE_VALUE": "移除元素",
    "REGENERATE_CLIENT_SECRET": "“*”将保留现有值。空白值将生成生成新的 client_secret。非空白参数值使用新指定的值覆盖现有值。",
    "ALL_OPTIONAL": "所有字段都是可选字段"
};
