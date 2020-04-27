var messages = {
//General
"DEPLOY_TOOL_TITLE": "배치",
"SEARCH" : "검색",
"SEARCH_HOSTS" : "호스트 검색",
"EXPLORE_TOOL": "탐색 도구",
"EXPLORE_TOOL_INSERT": "탐색 도구 시도",
"EXPLORE_TOOL_ARIA": "새 탭의 탐색 도구에서 호스트 검색",

//Rule Selector Panel
"RULESELECT_EDIT" : "편집",
"RULESELECT_CHANGE_SELECTION" : "편집 선택",
"RULESELECT_SERVER_DEFAULT" : "기본 서버 유형",
"RULESELECT_SERVER_CUSTOM" : "사용자 정의 유형",
"RULESELECT_SERVER_CUSTOM_ARIA" : "사용자 정의 서버 유형",
"RULESELECT_NEXT" : "다음",
"RULESELECT_SERVER_TYPE": "서버 유형",
"RULESELECT_SELECT_ONE": "하나 선택",
"RULESELECT_DEPLOY_TYPE" : "배치 규칙",
"RULESELECT_SERVER_SUBHEADING": "서버",
"RULESELECT_CUSTOM_PACKAGE": "사용자 정의 패키지",
"RULESELECT_RULE_DEFAULT" : "기본 규칙",
"RULESELECT_RULE_CUSTOM" : "사용자 정의 규칙",
"RULESELECT_FOOTER" : "배치 유형으로 돌아가기 전에 서버 유형 및 규칙 유형을 선택하십시오.",
"RULESELECT_CONFIRM" : "확인",
"RULESELECT_CUSTOM_INFO": "사용자 정의된 입력 및 배치 동작으로 고유한 규칙을 정의할 수 있습니다.",
"RULESELECT_CUSTOM_INFO_LINK": "자세히 보기",
"RULESELECT_BACK": "이전",

//Rule Selector Aria
"RULESELECT_PANEL_ARIA" : "규칙 선택 패널 {0}", //RULESELECT_OPEN or RULESELECT_CLOSED
"RULESELECT_OPEN" : "열기",
"RULESELECT_CLOSED" : "닫힘",
"RULESELECT_SCROLL_UP": "위로 스크롤",
"RULESELECT_SCROLL_DOWN": "아래로 스크롤",
"RULESELECT_EDIT_SERVER_ARIA" : "서버 유형 편집, 현재 선택사항 {0}", // Insert for SERVER TYPES
"RULESELECT_EDIT_RULE_ARIA" : "규칙 편집, 현재 선택사항 {0}", //Insert for PACKAGE TYPES
"RULESELECT_NEXT_ARIA" : "다음 패널",

//SERVER TYPES
"LIBERTY_SERVER" : "Liberty 서버",
"NODEJS_SERVER" : "Node.js 서버",

//PACKAGE TYPES
"APPLICATION_PACKAGE" : "애플리케이션 패키지", //IMPORTANT: Ensure that the translation of this string is the same as the one for "DEPLOY_RULE_APPLICATION_PACKAGE_TYPE" in com.ibm.ws.collective.controller\resources\com\ibm\ws\collective\command\internal\resources\Messages.nlsprops
"SERVER_PACKAGE" : "서버 패키지", //IMPORTANT: Ensure that the translation of this string is the same as the one for "DEPLOY_RULE_SERVER_PACKAGE_TYPE" in com.ibm.ws.collective.controller\resources\com\ibm\ws\collective\command\internal\resources\Messages.nlsprops
"DOCKER_CONTAINER" : "Docker 컨테이너", //IMPORTANT: Ensure that the translation of this string is the same as the one for "DEPLOY_RULE_DOCKER_PACKAGE_TYPE" in com.ibm.ws.collective.controller\resources\com\ibm\ws\collective\command\internal\resources\Messages.nlsprops

//Deployment parameters
"DEPLOYMENT_PARAMETERS": "배치 매개변수",
// "PARAMETERS_TOGGLE": "Toggle between upload and use a file located on the collective controller",
"GROUP_DEPLOYMENT_PARAMETERS": "배치 매개변수({0})",
"PARAMETERS_DESCRIPTION": "세부사항은 선택된 서버 및 템플리트 유형에 따라 달라집니다.",
"PARAMETERS_TOGGLE_CONTROLLER": "집합체 제어기에 위치한 파일 사용",
"PARAMETERS_TOGGLE_UPLOAD": "파일 업로드",
"SEARCH_IMAGES": "이미지 검색",
"SEARCH_CLUSTERS": "클러스터 검색",
"CLEAR_FIELD_BUTTON_ARIA": "입력 값 지우기",

// file browser and upload
"SERVER_BROWSE_DESCRIPTION": "서버 패키지 파일 업로드",
"BROWSE_TITLE": "{0} 업로드",
"STRONGLOOP_BROWSE": "파일을 여기에 끌어오거나 {0}를 수행하여 파일 이름 제공", //BROWSE_INSERT
"BROWSE_INSERT" : "찾아보기",
"BROWSE_ARIA": "파일 찾아보기",
"FILE_UPLOAD_PREVIOUS" : "집합체 제어기에 위치한 파일 사용",
"IS_UPLOADING": "{0} 업로드 중...",
"CANCEL" : "취소",
"UPLOAD_SUCCESSFUL" : "{0}이(가) 업로드되었습니다!", // Package Name
"UPLOAD_FAILED" : "업로드 실패",
"RESET" : "재설정",
"FILE_UPLOAD_WRITEDIRS_EMPTY_ERROR" : "쓰기 디렉토리 목록이 비어 있습니다.",
"FILE_UPLOAD_WRITEDIRS_PATH_ERROR" : "지정된 경로가 쓰기 디렉토리 목록에 있어야 합니다.",
"PARAMETERS_FILE_ARIA" : "배치 매개변수 또는 {0}", // Upload Package name

// docker
"DOCKER_MISSING_REPOSITORY_ERROR": "Docker 저장소를 구성해야 함",
"DOCKER_EMPTY_IMAGE_ERROR": "구성된 Docker 저장소에 이미지가 없음",
"DOCKER_GENERIC_ERROR": "Docker 이미지가 로드되지 않았습니다. 구성된 Docker 저장소가 있는지 확인하십시오.",
"REFRESH": "새로 고치기",
"REFRESH_ARIA": "Docker 이미지 새로 고치기",
"PARAMETERS_DOCKER_ARIA": "배치 매개변수 또는 Docker 이미지 검색",
"DOCKER_IMAGES_ARIA" : "Docker 이미지 목록",
"LOCAL_IMAGE": "로컬 이미지 이름",
"DOCKER_INVALID_CONTAINER__NAME_ERROR" : "컨테이너 이름은 [a-zA-Z0-9][a-zA-Z0-9_.-]* 형식과 일치해야 함",

//Host selection
"ONE_SELECTED_HOST" : "{0}개의 선택된 호스트", //quantity
"N_SELECTED_HOSTS": "{0}개의 선택된 호스트", //quantity
"SELECT_HOSTS_MESSAGE": "사용 가능한 호스트의 목록에서 선택하십시오. 이름 또는 태그로 호스트를 검색할 수 있습니다.",
"ONE_HOST" : "{0}개의 결과", //quantity
"N_HOSTS": "{0}개의 결과", //quantity
"SELECT_HOSTS_FOOTER": "좀 더 복잡한 검색이 필요하십니까? {0}", //EXPLORE_TOOL_INSERT
"NAME": "이름",
"NAME_FILTER": "이름별로 호스트 필터링", // Used for aria-label
"TAG": "태그",
"TAG_FILTER": "태그별로 호스트 필터링",
"ALL_HOSTS_LIST_ARIA" : "모든 호스트 목록",
"SELECTED_HOSTS_LIST_ARIA": "선택된 호스트 목록",

//Security Details
"SECURITY_DETAILS": "보안 세부사항",
"SECURITY_DETAILS_FOR_GROUP": "{0}에 대한 보안 세부사항",  // {0} is the translated group name
"SECURITY_DETAILS_MESSAGE": "서버 보안에 필요한 추가 신임 정보입니다.",
"SECURITY_CREATE_PASSWORD" : "비밀번호 작성",
"KEYSTORE_PASSWORD_MESSAGE": "비밀번호를 지정하여 새로 생성된 키 저장소 파일(서버 인증 신임 정보 포함)을 보호하십시오.",
"PASSWORD_MESSAGE": "비밀번호를 지정하여 새로 생성된 파일(서버 인증 신임 정보 포함)을 보호하십시오.",
"KEYSTORE_PASSWORD": "키 저장소 비밀번호",
"CONFIRM_KEYSTORE_PASSWORD": "키 저장소 비밀번호 확인",
"PASSWORDS_DONT_MATCH": "비밀번호가 일치하지 않음",
"GROUP_GENERIC_PASSWORD": "{0}({1})", // {0} is the display label for the initial password field
                                       // {1} is the group name if the password belongs to a group
"CONFIRM_GENERIC_PASSWORD": "{0} 확인", // {0} is the display label for the initial password field
"CONFIRM_GROUP_GENERIC_PASSWORD": "{0}({1}) 확인", // {0} is the display label for the initial password field
                                                       // {1} is the group name if the password belongs to a group

//Deploy
"REVIEW_AND_DEPLOY": "검토 및 배치",
"REVIEW_AND_DEPLOY_MESSAGE" : "배치 전 모든 필드가 {0}.", //REVIEW_AND_DEPLOY_INCOMPLETE_FIELDS
"REVIEW_AND_DEPLOY_INCOMPLETE_FIELDS": "완료되어야 합니다",
"READY_FOR_DEPLOYMENT": "배치할 준비가 되었습니다.",
"READY_FOR_DEPLOYMENT_CAPS": "배치할 준비가 되었습니다.",
"READY_TO_DEPLOY": "양식이 완성되었습니다. {0}", //READY_FOR_DEPLOYMENT_CAPS
"READY_TO_DEPLOY_SERVER": "양식이 완성되었습니다. 서버 패키지가 {0}", //READY_FOR_DEPLOYMENT
"READY_TO_DEPLOY_DOCKER": "양식이 완성되었습니다. Docker Container가 {0}", //READY_FOR_DEPLOYMENT
"DEPLOY": "배치",

"DEPLOY_UPLOADING" : "서버 패키지가 업로드를 완료하도록 허용하십시오...",
"DEPLOY_FILE_UPLOADING" : "파일 업로드 완료 중...",
"UPLOADING": "업로드 중...",
"DEPLOY_UPLOADING_MESSAGE" : "배치 프로세스가 시작될 때까지 이 창을 열어 두십시오.",
"DEPLOY_AFTER_UPLOADED_MESSAGE" : "{0}에서 업로드를 완료한 후 여기서 배치 진행상태를 모니터할 수 있습니다.", // Package name
"DEPLOY_FILE_UPLOAD_PERCENT" : "{0} - {1}% 완료", // Package name, number
"DEPLOY_WATCH_FOR_UPDATES": "여기서 업데이트를 확인하거나 이 창을 닫고 백그라운드에서 실행하십시오!",
"DEPLOY_CHECK_STATUS": "이 화면의 오른쪽 상단 모서리에 있는 백그라운드 태스크 아이콘을 클릭하여 언제든지 배치 상태를 확인할 수 있습니다.",
"DEPLOY_IN_PROGRESS": "배치가 진행 중입니다!",
"DEPLOY_VIEW_BG_TASKS": "백그라운드 태스크 보기",
"DEPLOYMENT_PROGRESS": "배치 진행상태",
"DEPLOYING_IMAGE": "{0} - {1}개의 호스트", // Package name (e.g.,  someServerName.zip, aLibertyDockerImage), number of hosts
"VIEW_DEPLOYED_SERVERS": "배치된 서버 보기",
"DEPLOY_PERCENTAGE": "{0}% 완료", //quantity
"DEPLOYMENT_COMPLETE_TITLE" : "배치가 완료되었습니다!",
"DEPLOYMENT_COMPLETE_WITH_ERRORS_TITLE" : "배치가 완료되었으나 일부 오류가 있습니다.",
"DEPLOYMENT_COMPLETE_MESSAGE" : "오류를 좀 더 자세하게 조사할 수 있습니다. 새로 배치된 서버를 확인하거나 다른 배치를 시작하십시오.",
"DEPLOYING": "배치 중...",
"DEPLOYMENT_FAILED": "배치에 실패했습니다.",
"RETURN_DEPLOY": "배치 양식으로 돌아가서 다시 제출",
"REUTRN_DEPLOY_HEADER": "다시 시도",

//Footer
"FOOTER": "추가로 배치하시겠습니까?",
"FOOTER_BUTTON_MESSAGE" : "다른 배치 시작",

//Error stuff
"ERROR_TITLE": "오류 요약",
"ERROR_VIEW_DETAILS" : "오류 세부사항 보기",
"ONE_ERROR_ONE_HOST": "하나의 호스트에 하나의 오류가 발생함",
"ONE_ERROR_MULTIPLE_HOST": "여러 호스트에 하나의 오류가 발생함",
"MULTIPLE_ERROR_ONE_HOST": "하나의 호스트에 여러 오류가 발생함",
"MULTIPLE_ERROR_MULTIPLE_HOST": "여러 호스트에 여러 오류가 발생함",
"INITIALIZATION_ERROR_MESSAGE": "호스트에 액세스하거나 서버에 규칙 정보를 배치할 수 없음",
"TRANSLATIONS_ERROR_MESSAGE" : "구체화된 문자열에 액세스할 수 없음",
"MISSING_HOST": "목록에서 하나 이상의 호스트 선택",
"INVALID_CHARACTERS" : "필드에 '()$%&'와 같은 특수 문자를 사용할 수 없음",
"INVALID_DOCKER_IMAGE" : "이미지가 없음",
"ERROR_HOSTS" : "{0} 및 기타 {1}개" // if there are more than three hosts reporting error, the message for errors from 6 hosts would be like:
                                      // host1.com, host2.com, host3.com and 3 others
};
