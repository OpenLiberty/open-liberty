var messages = {
//General
"DEPLOY_TOOL_TITLE": "部署",
"SEARCH" : "搜尋",
"SEARCH_HOSTS" : "搜尋主機",
"EXPLORE_TOOL": "探索工具",
"EXPLORE_TOOL_INSERT": "嘗試探索工具",
"EXPLORE_TOOL_ARIA": "在「探索工具」中的新標籤中搜尋主機",

//Rule Selector Panel
"RULESELECT_EDIT" : "編輯",
"RULESELECT_CHANGE_SELECTION" : "編輯選項",
"RULESELECT_SERVER_DEFAULT" : "預設伺服器類型",
"RULESELECT_SERVER_CUSTOM" : "自訂類型",
"RULESELECT_SERVER_CUSTOM_ARIA" : "自訂伺服器類型",
"RULESELECT_NEXT" : "NEXT",
"RULESELECT_SERVER_TYPE": "伺服器類型",
"RULESELECT_SELECT_ONE": "請選取一種",
"RULESELECT_DEPLOY_TYPE" : "部署規則",
"RULESELECT_SERVER_SUBHEADING": "伺服器",
"RULESELECT_CUSTOM_PACKAGE": "自訂套件",
"RULESELECT_RULE_DEFAULT" : "預設規則",
"RULESELECT_RULE_CUSTOM" : "自訂規則",
"RULESELECT_FOOTER" : "選擇伺服器類型和規則類型，然後回到「部署表單」。",
"RULESELECT_CONFIRM" : "CONFIRM",
"RULESELECT_CUSTOM_INFO": "您可以使用自訂的輸入和部署行為，來定義您自己的規則。",
"RULESELECT_CUSTOM_INFO_LINK": "進一步瞭解",
"RULESELECT_BACK": "返回",

//Rule Selector Aria
"RULESELECT_PANEL_ARIA" : "規則選擇畫面 {0}", //RULESELECT_OPEN or RULESELECT_CLOSED
"RULESELECT_OPEN" : "開啟",
"RULESELECT_CLOSED" : "Closed",
"RULESELECT_SCROLL_UP": "向上捲動",
"RULESELECT_SCROLL_DOWN": "向下捲動",
"RULESELECT_EDIT_SERVER_ARIA" : "編輯伺服器，現行選擇 {0}", // Insert for SERVER TYPES
"RULESELECT_EDIT_RULE_ARIA" : "編輯規則，現行選擇 {0}", //Insert for PACKAGE TYPES
"RULESELECT_NEXT_ARIA" : "下一畫面",

//SERVER TYPES
"LIBERTY_SERVER" : "Liberty 伺服器",
"NODEJS_SERVER" : "Node.js 伺服器",

//PACKAGE TYPES
"APPLICATION_PACKAGE" : "應用程式套件", //IMPORTANT: Ensure that the translation of this string is the same as the one for "DEPLOY_RULE_APPLICATION_PACKAGE_TYPE" in com.ibm.ws.collective.controller\resources\com\ibm\ws\collective\command\internal\resources\Messages.nlsprops
"SERVER_PACKAGE" : "伺服器套件", //IMPORTANT: Ensure that the translation of this string is the same as the one for "DEPLOY_RULE_SERVER_PACKAGE_TYPE" in com.ibm.ws.collective.controller\resources\com\ibm\ws\collective\command\internal\resources\Messages.nlsprops
"DOCKER_CONTAINER" : "Docker 儲存器", //IMPORTANT: Ensure that the translation of this string is the same as the one for "DEPLOY_RULE_DOCKER_PACKAGE_TYPE" in com.ibm.ws.collective.controller\resources\com\ibm\ws\collective\command\internal\resources\Messages.nlsprops

//Deployment parameters
"DEPLOYMENT_PARAMETERS": "部署參數",
// "PARAMETERS_TOGGLE": "Toggle between upload and use a file located on the collective controller",
"GROUP_DEPLOYMENT_PARAMETERS": "部署參數 ({0})",
"PARAMETERS_DESCRIPTION": "詳細資料是以選取的伺服器和範本類型為基礎。",
"PARAMETERS_TOGGLE_CONTROLLER": "使用位於群體控制器上的檔案",
"PARAMETERS_TOGGLE_UPLOAD": "Upload a file",
"SEARCH_IMAGES": "搜尋映像檔",
"SEARCH_CLUSTERS": "搜尋叢集",
"CLEAR_FIELD_BUTTON_ARIA": "清除輸入值",

// file browser and upload
"SERVER_BROWSE_DESCRIPTION": "上傳伺服器套件檔",
"BROWSE_TITLE": "上傳{0}",
"STRONGLOOP_BROWSE": "將檔案拖曳到這裡或 {0} 以提供檔名", //BROWSE_INSERT
"BROWSE_INSERT" : "瀏覽 (browse)",
"BROWSE_ARIA": "瀏覽檔案",
"FILE_UPLOAD_PREVIOUS" : "使用位於群體控制器上的檔案",
"IS_UPLOADING": "{0} 上傳中...",
"CANCEL" : "CANCEL",
"UPLOAD_SUCCESSFUL" : "{0} 上傳成功！", // Package Name
"UPLOAD_FAILED" : "上傳失敗",
"RESET" : "reset",
"FILE_UPLOAD_WRITEDIRS_EMPTY_ERROR" : "寫入目錄清單是空的。",
"FILE_UPLOAD_WRITEDIRS_PATH_ERROR" : "指定的路徑必須位於寫入目錄清單中。",
"PARAMETERS_FILE_ARIA" : "部署參數或 {0}", // Upload Package name

// docker
"DOCKER_MISSING_REPOSITORY_ERROR": "您必須配置 Docker 儲存庫",
"DOCKER_EMPTY_IMAGE_ERROR": "在所配置的 Docker 儲存庫中找不到映像檔",
"DOCKER_GENERIC_ERROR": "未載入任何 Docker 映像檔。請確保您已配置 Docker 儲存庫。",
"REFRESH": "重新整理",
"REFRESH_ARIA": "重新整理 Docker 映像檔",
"PARAMETERS_DOCKER_ARIA": "部署參數或搜尋 Docker 映像檔",
"DOCKER_IMAGES_ARIA" : "Docker 映像檔清單",
"LOCAL_IMAGE": "本端映像檔名稱",
"DOCKER_INVALID_CONTAINER__NAME_ERROR" : "儲存器名稱必須符合格式 [a-zA-Z0-9][a-zA-Z0-9_.-]*",

//Host selection
"ONE_SELECTED_HOST" : "{0} 部選取的主機", //quantity
"N_SELECTED_HOSTS": "{0} 部選取的主機", //quantity
"SELECT_HOSTS_MESSAGE": "請從可用的主機清單中選取。您可以依名稱或標籤來搜尋主機。",
"ONE_HOST" : "{0} 筆結果", //quantity
"N_HOSTS": "{0} 筆結果", //quantity
"SELECT_HOSTS_FOOTER": "需要進行更複雜的搜尋嗎？{0}", //EXPLORE_TOOL_INSERT
"NAME": "NAME",
"NAME_FILTER": "依名稱來過濾主機", // Used for aria-label
"TAG": "TAG",
"TAG_FILTER": "依標籤來過濾主機",
"ALL_HOSTS_LIST_ARIA" : "所有主機清單",
"SELECTED_HOSTS_LIST_ARIA": "選取的主機清單",

//Security Details
"SECURITY_DETAILS": "安全詳細資料",
"SECURITY_DETAILS_FOR_GROUP": "{0} 安全詳細資料",  // {0} is the translated group name
"SECURITY_DETAILS_MESSAGE": "需要其他認證來維護伺服器安全。",
"SECURITY_CREATE_PASSWORD" : "建立密碼",
"KEYSTORE_PASSWORD_MESSAGE": "指定密碼，以保護含有伺服器鑑別認證之新產生的金鑰儲存庫檔。",
"PASSWORD_MESSAGE": "指定密碼，以保護含有伺服器鑑別認證之新產生的檔案。",
"KEYSTORE_PASSWORD": "金鑰儲存庫密碼",
"CONFIRM_KEYSTORE_PASSWORD": "確認金鑰儲存庫密碼",
"PASSWORDS_DONT_MATCH": "密碼不相符",
"GROUP_GENERIC_PASSWORD": "{0} ({1})", // {0} is the display label for the initial password field
                                       // {1} is the group name if the password belongs to a group
"CONFIRM_GENERIC_PASSWORD": "確認{0}", // {0} is the display label for the initial password field
"CONFIRM_GROUP_GENERIC_PASSWORD": "確認 {0} ({1})", // {0} is the display label for the initial password field
                                                       // {1} is the group name if the password belongs to a group

//Deploy
"REVIEW_AND_DEPLOY": "檢閱和部署",
"REVIEW_AND_DEPLOY_MESSAGE" : "部署之前{0}所有欄位。", //REVIEW_AND_DEPLOY_INCOMPLETE_FIELDS
"REVIEW_AND_DEPLOY_INCOMPLETE_FIELDS": "必須完成",
"READY_FOR_DEPLOYMENT": "已備妥可供部署。",
"READY_FOR_DEPLOYMENT_CAPS": "已備妥可供部署。",
"READY_TO_DEPLOY": "表單已完成。{0}", //READY_FOR_DEPLOYMENT_CAPS
"READY_TO_DEPLOY_SERVER": "表單已完成。伺服器套件為 {0}", //READY_FOR_DEPLOYMENT
"READY_TO_DEPLOY_DOCKER": "表單已完成。Docker 儲存器為 {0}", //READY_FOR_DEPLOYMENT
"DEPLOY": "部署",

"DEPLOY_UPLOADING" : "請容許伺服器套件完成上傳...",
"DEPLOY_FILE_UPLOADING" : "正在完成檔案上傳...",
"UPLOADING": "正在上傳...",
"DEPLOY_UPLOADING_MESSAGE" : "維持此視窗開啟，直到部署程序開始。",
"DEPLOY_AFTER_UPLOADED_MESSAGE" : "在 {0} 完成上傳之後，您可以在這裡監視部署進度。", // Package name
"DEPLOY_FILE_UPLOAD_PERCENT" : "{0} - {1}% 完成", // Package name, number
"DEPLOY_WATCH_FOR_UPDATES": "在這裡留意更新項目，或關閉此視窗，讓它在背景執行！",
"DEPLOY_CHECK_STATUS": "您可以按一下此畫面右上角的「背景作業」圖示，隨時檢查部署的狀態。",
"DEPLOY_IN_PROGRESS": "部署進行中！",
"DEPLOY_VIEW_BG_TASKS": "檢視背景作業",
"DEPLOYMENT_PROGRESS": "部署進度",
"DEPLOYING_IMAGE": "{0} 至 {1} 主機", // Package name (e.g.,  someServerName.zip, aLibertyDockerImage), number of hosts
"VIEW_DEPLOYED_SERVERS": "檢視已順利部署的伺服器",
"DEPLOY_PERCENTAGE": "{0}% 完成", //quantity
"DEPLOYMENT_COMPLETE_TITLE" : "部署完成！",
"DEPLOYMENT_COMPLETE_WITH_ERRORS_TITLE" : "您的部署已完成，但有一些錯誤。",
"DEPLOYMENT_COMPLETE_MESSAGE" : "您可以更仔細地調查錯誤，檢查新部署的伺服器，或啟動另一項部署。",
"DEPLOYING": "正在部署...",
"DEPLOYMENT_FAILED": "部署失敗。",
"RETURN_DEPLOY": "回到部署表單，並重新提交",
"REUTRN_DEPLOY_HEADER": "重試",

//Footer
"FOOTER": "還要進行其他部署嗎？",
"FOOTER_BUTTON_MESSAGE" : "啟動另一項部署",

//Error stuff
"ERROR_TITLE": "錯誤摘要",
"ERROR_VIEW_DETAILS" : "檢視錯誤詳細資料",
"ONE_ERROR_ONE_HOST": "有一部主機發生一個錯誤",
"ONE_ERROR_MULTIPLE_HOST": "有多部主機發生一個錯誤",
"MULTIPLE_ERROR_ONE_HOST": "有一部主機發生多個錯誤",
"MULTIPLE_ERROR_MULTIPLE_HOST": "有多部主機發生多個錯誤",
"INITIALIZATION_ERROR_MESSAGE": "無法存取伺服器上的主機或部署規則資訊",
"TRANSLATIONS_ERROR_MESSAGE" : "無法存取所提出的字串",
"MISSING_HOST": "請從清單中選取至少一部主機",
"INVALID_CHARACTERS" : "欄位不能含有特殊字元，例如 '()$%&'",
"INVALID_DOCKER_IMAGE" : "找不到映像檔",
"ERROR_HOSTS" : "{0} 和其他 {1} 部" // if there are more than three hosts reporting error, the message for errors from 6 hosts would be like:
                                      // host1.com, host2.com, host3.com and 3 others
};
