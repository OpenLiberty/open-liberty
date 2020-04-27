var messages = {
//General
"DEPLOY_TOOL_TITLE": "部署",
"SEARCH" : "搜索",
"SEARCH_HOSTS" : "搜索主机",
"EXPLORE_TOOL": "导出工具",
"EXPLORE_TOOL_INSERT": "尝试浏览工具",
"EXPLORE_TOOL_ARIA": "在新标签中使用浏览工具搜索主机",

//Rule Selector Panel
"RULESELECT_EDIT" : "编辑",
"RULESELECT_CHANGE_SELECTION" : "编辑选项",
"RULESELECT_SERVER_DEFAULT" : "缺省服务器类型",
"RULESELECT_SERVER_CUSTOM" : "定制类型",
"RULESELECT_SERVER_CUSTOM_ARIA" : "定制服务器类型",
"RULESELECT_NEXT" : "NEXT",
"RULESELECT_SERVER_TYPE": "服务器类型",
"RULESELECT_SELECT_ONE": "选择其中一项",
"RULESELECT_DEPLOY_TYPE" : "部署规则",
"RULESELECT_SERVER_SUBHEADING": "服务器",
"RULESELECT_CUSTOM_PACKAGE": "定制包",
"RULESELECT_RULE_DEFAULT" : "缺省规则",
"RULESELECT_RULE_CUSTOM" : "定制规则",
"RULESELECT_FOOTER" : "选择服务器类型和规则类型，然后返回到 Deploy 表单。",
"RULESELECT_CONFIRM" : "CONFIRM",
"RULESELECT_CUSTOM_INFO": "您可以使用定制输入和部署行为定义您自己的规则。",
"RULESELECT_CUSTOM_INFO_LINK": "了解更多",
"RULESELECT_BACK": "返回",

//Rule Selector Aria
"RULESELECT_PANEL_ARIA" : "规则选择面板 {0}", //RULESELECT_OPEN or RULESELECT_CLOSED
"RULESELECT_OPEN" : "打开",
"RULESELECT_CLOSED" : "Closed ",
"RULESELECT_SCROLL_UP": "向上滚动",
"RULESELECT_SCROLL_DOWN": "向下滚动",
"RULESELECT_EDIT_SERVER_ARIA" : "编辑服务器类型，当前选择 {0}", // Insert for SERVER TYPES
"RULESELECT_EDIT_RULE_ARIA" : "编辑规则，当前选择 {0}", //Insert for PACKAGE TYPES
"RULESELECT_NEXT_ARIA" : "下一个面板",

//SERVER TYPES
"LIBERTY_SERVER" : "Liberty 服务器",
"NODEJS_SERVER" : "Node.js 服务器",

//PACKAGE TYPES
"APPLICATION_PACKAGE" : "应用程序包", //IMPORTANT: Ensure that the translation of this string is the same as the one for "DEPLOY_RULE_APPLICATION_PACKAGE_TYPE" in com.ibm.ws.collective.controller\resources\com\ibm\ws\collective\command\internal\resources\Messages.nlsprops
"SERVER_PACKAGE" : "服务器包", //IMPORTANT: Ensure that the translation of this string is the same as the one for "DEPLOY_RULE_SERVER_PACKAGE_TYPE" in com.ibm.ws.collective.controller\resources\com\ibm\ws\collective\command\internal\resources\Messages.nlsprops
"DOCKER_CONTAINER" : "Docker 容器", //IMPORTANT: Ensure that the translation of this string is the same as the one for "DEPLOY_RULE_DOCKER_PACKAGE_TYPE" in com.ibm.ws.collective.controller\resources\com\ibm\ws\collective\command\internal\resources\Messages.nlsprops

//Deployment parameters
"DEPLOYMENT_PARAMETERS": "部署参数",
// "PARAMETERS_TOGGLE": "Toggle between upload and use a file located on the collective controller",
"GROUP_DEPLOYMENT_PARAMETERS": "部署参数 ({0})",
"PARAMETERS_DESCRIPTION": "详细信息基于选择的服务器和模板类型。",
"PARAMETERS_TOGGLE_CONTROLLER": "使用位于集合体控制器上的文件",
"PARAMETERS_TOGGLE_UPLOAD": "上载文件",
"SEARCH_IMAGES": "搜索映像",
"SEARCH_CLUSTERS": "搜索集群",
"CLEAR_FIELD_BUTTON_ARIA": "清除输入值",

// file browser and upload
"SERVER_BROWSE_DESCRIPTION": "上载服务器包文件",
"BROWSE_TITLE": "上载{0}",
"STRONGLOOP_BROWSE": "拖动此处文件或 {0} 以提供文件名", //BROWSE_INSERT
"BROWSE_INSERT" : "浏览 (browse)",
"BROWSE_ARIA": "浏览文件",
"FILE_UPLOAD_PREVIOUS" : "使用位于集合体控制器上的文件",
"IS_UPLOADING": "正在上载{0} ...",
"CANCEL" : "CANCEL",
"UPLOAD_SUCCESSFUL" : "{0} 上载成功！", // Package Name
"UPLOAD_FAILED" : "上载失败",
"RESET" : "复位",
"FILE_UPLOAD_WRITEDIRS_EMPTY_ERROR" : "写目录列表为空。",
"FILE_UPLOAD_WRITEDIRS_PATH_ERROR" : "指定的路径必须位于写目录列表。",
"PARAMETERS_FILE_ARIA" : "部署参数或 {0}", // Upload Package name

// docker
"DOCKER_MISSING_REPOSITORY_ERROR": "必须配置 Docker 存储库",
"DOCKER_EMPTY_IMAGE_ERROR": "在配置的 Docker 存储库未找到映像",
"DOCKER_GENERIC_ERROR": "未载入 Docker 映像。确保已配置 Docker 存储库。",
"REFRESH": "刷新",
"REFRESH_ARIA": "刷新 Docker 映像",
"PARAMETERS_DOCKER_ARIA": "部署参数或搜索 Docker 映像",
"DOCKER_IMAGES_ARIA" : "Docker 映像列表",
"LOCAL_IMAGE": "本地映像名称",
"DOCKER_INVALID_CONTAINER__NAME_ERROR" : "容器名称必须符合格式 [a-zA-Z0-9][a-zA-Z0-9_.-]*",

//Host selection
"ONE_SELECTED_HOST" : "{0} 个已选主机", //quantity
"N_SELECTED_HOSTS": "{0} 个已选主机", //quantity
"SELECT_HOSTS_MESSAGE": "从可选主机列表中进行选择。您可以按照名称或标签搜索主机。",
"ONE_HOST" : "{0} 个结果", //quantity
"N_HOSTS": "{0} 个结果", //quantity
"SELECT_HOSTS_FOOTER": "是否需要更复杂的搜索？{0}", //EXPLORE_TOOL_INSERT
"NAME": "NAME",
"NAME_FILTER": "根据名称过滤主机", // Used for aria-label
"TAG": "TAG",
"TAG_FILTER": "根据标记过滤主机",
"ALL_HOSTS_LIST_ARIA" : "所有主机列表",
"SELECTED_HOSTS_LIST_ARIA": "所选主机列表",

//Security Details
"SECURITY_DETAILS": "安全性详细信息",
"SECURITY_DETAILS_FOR_GROUP": "{0} 的安全详细信息",  // {0} is the translated group name
"SECURITY_DETAILS_MESSAGE": "需要创建其他凭证以确保服务器安全。",
"SECURITY_CREATE_PASSWORD" : "创建密码",
"KEYSTORE_PASSWORD_MESSAGE": "指定密码以保护包含服务器认证凭证的新生成密钥库文件。",
"PASSWORD_MESSAGE": "指定密码以保护包含服务器认证凭证的新生成文件。",
"KEYSTORE_PASSWORD": "密钥库密码",
"CONFIRM_KEYSTORE_PASSWORD": "确认密钥库密码",
"PASSWORDS_DONT_MATCH": "密码不匹配",
"GROUP_GENERIC_PASSWORD": "{0}（{1}）", // {0} is the display label for the initial password field
                                       // {1} is the group name if the password belongs to a group
"CONFIRM_GENERIC_PASSWORD": "确认 {0}", // {0} is the display label for the initial password field
"CONFIRM_GROUP_GENERIC_PASSWORD": "确认{0} ({1})", // {0} is the display label for the initial password field
                                                       // {1} is the group name if the password belongs to a group

//Deploy
"REVIEW_AND_DEPLOY": "复查并部署",
"REVIEW_AND_DEPLOY_MESSAGE" : "部署前所有字段都{0}。", //REVIEW_AND_DEPLOY_INCOMPLETE_FIELDS
"REVIEW_AND_DEPLOY_INCOMPLETE_FIELDS": "必须完成",
"READY_FOR_DEPLOYMENT": "准备部署。",
"READY_FOR_DEPLOYMENT_CAPS": "准备部署。",
"READY_TO_DEPLOY": "表单已完成。{0}", //READY_FOR_DEPLOYMENT_CAPS
"READY_TO_DEPLOY_SERVER": "表单已完成。服务器包是 {0}", //READY_FOR_DEPLOYMENT
"READY_TO_DEPLOY_DOCKER": "表单已完成。Docker 容器是 {0}", //READY_FOR_DEPLOYMENT
"DEPLOY": "DEPLOY",

"DEPLOY_UPLOADING" : "请允许服务器包完成上载...",
"DEPLOY_FILE_UPLOADING" : "文件上载完成...",
"UPLOADING": "正在上载...",
"DEPLOY_UPLOADING_MESSAGE" : "在开始部署进程之前，请保持此窗口打开。",
"DEPLOY_AFTER_UPLOADED_MESSAGE" : "{0} 上载完成后，您可以在此处监视您的部署进度。", // Package name
"DEPLOY_FILE_UPLOAD_PERCENT" : "{0} - {1}% 已完成", // Package name, number
"DEPLOY_WATCH_FOR_UPDATES": "在此处查看更新，或关闭此窗口使其后台运行！",
"DEPLOY_CHECK_STATUS": "可以通过单击此屏幕右上角的“后台任务”按钮随时查看部署状态。",
"DEPLOY_IN_PROGRESS": "部署正在进行！",
"DEPLOY_VIEW_BG_TASKS": "查看后台任务",
"DEPLOYMENT_PROGRESS": "部署进度",
"DEPLOYING_IMAGE": "{0} 至 {1} 个主机", // Package name (e.g.,  someServerName.zip, aLibertyDockerImage), number of hosts
"VIEW_DEPLOYED_SERVERS": "查看已成功部署的服务器",
"DEPLOY_PERCENTAGE": "完成 {0}%", //quantity
"DEPLOYMENT_COMPLETE_TITLE" : "部署完成!",
"DEPLOYMENT_COMPLETE_WITH_ERRORS_TITLE" : "您的部署已完成，但有一些错误。",
"DEPLOYMENT_COMPLETE_MESSAGE" : "您可以更详细地研究错误信息，检查最新部署的服务器或开始其他部署。",
"DEPLOYING": "正在部署...",
"DEPLOYMENT_FAILED": "部署失败。",
"RETURN_DEPLOY": "返回部署表单，然后重新提交",
"REUTRN_DEPLOY_HEADER": "重试",

//Footer
"FOOTER": "要部署更多？",
"FOOTER_BUTTON_MESSAGE" : "开始其他部署",

//Error stuff
"ERROR_TITLE": "错误摘要",
"ERROR_VIEW_DETAILS" : "查看错误详细信息",
"ONE_ERROR_ONE_HOST": "一个主机上存在一个错误",
"ONE_ERROR_MULTIPLE_HOST": "多个主机上存在同一个错误",
"MULTIPLE_ERROR_ONE_HOST": "一个主机上存在多个错误",
"MULTIPLE_ERROR_MULTIPLE_HOST": "多个主机上存在多个错误",
"INITIALIZATION_ERROR_MESSAGE": "无法访问主机，或部署服务器的规则信息",
"TRANSLATIONS_ERROR_MESSAGE" : "无法访问外部化字符串",
"MISSING_HOST": "请至少从列表中选择一个主机",
"INVALID_CHARACTERS" : "字段不能包含特殊字符，例如 '()$%&'",
"INVALID_DOCKER_IMAGE" : "未找到映像",
"ERROR_HOSTS" : "{0} 和 {1} 其他主机" // if there are more than three hosts reporting error, the message for errors from 6 hosts would be like:
                                      // host1.com, host2.com, host3.com and 3 others
};
