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
    LIBERTY_HEADER_TITLE: "Liberty 관리 센터",
    LIBERTY_HEADER_PROFILE: "환경 설정",
    LIBERTY_HEADER_LOGOUT: "로그아웃",
    LIBERTY_HEADER_LOGOUT_USERNAME: "{0} 로그아웃",
    TOOLBOX_BANNER_LABEL: "{0} 배너",  // TOOLBOX_TITLE
    TOOLBOX_TITLE: "도구 상자",
    TOOLBOX_TITLE_LOADING_TOOL: "도구 로드 중...",
    TOOLBOX_TITLE_EDIT: "도구 상자 편집",
    TOOLBOX_EDIT: "편집",
    TOOLBOX_DONE: "완료",
    TOOLBOX_SEARCH: "필터",
    TOOLBOX_CLEAR_SEARCH: "필터 기준 지우기",
    TOOLBOX_END_SEARCH: "필터 종료",
    TOOLBOX_ADD_TOOL: "도구 추가",
    TOOLBOX_ADD_CATALOG_TOOL: "도구 추가",
    TOOLBOX_ADD_BOOKMARK: "책갈피 추가",
    TOOLBOX_REMOVE_TITLE: "{0} 도구 제거",
    TOOLBOX_REMOVE_TITLE_NO_TOOL_NAME: "도구 제거",
    TOOLBOX_REMOVE_MESSAGE: "{0}을(를) 제거하시겠습니까?",
    TOOLBOX_BUTTON_REMOVE: "제거",
    TOOLBOX_BUTTON_OK: "확인",
    TOOLBOX_BUTTON_GO_TO: "도구 상자로 이동",
    TOOLBOX_BUTTON_CANCEL: "취소",
    TOOLBOX_BUTTON_BGTASK: "백그라운드 태스크",
    TOOLBOX_BUTTON_BACK: "이전",
    TOOLBOX_BUTTON_USER: "사용자",
    TOOLBOX_ADDTOOL_ERROR_MESSAGE: "{0} 도구를 추가하는 중에 오류 발생: {1}",
    TOOLBOX_REMOVETOOL_ERROR_MESSAGE: "{0} 도구를 제거하는 중에 오류 발생: {1}",
    TOOLBOX_GET_ERROR_MESSAGE: "도구 상자에서 도구를 검색하는 중에 오류 발생: {0}",
    TOOLCATALOG_TITLE: "도구 카탈로그",
    TOOLCATALOG_ADDTOOL_TITLE: "도구 추가",
    TOOLCATALOG_ADDTOOL_MESSAGE: "{0} 도구를 도구 상자에 추가하시겠습니까?",
    TOOLCATALOG_BUTTON_ADD: "추가",
    TOOL_FRAME_TITLE: "도구 프레임",
    TOOL_DELETE_TITLE: "{0} 삭제",
    TOOL_ADD_TITLE: "{0} 추가",
    TOOL_ADDED_TITLE: "{0}이(가) 이미 추가됨",
    TOOL_LAUNCH_ERROR_MESSAGE_TITLE: "도구를 찾을 수 없음",
    TOOL_LAUNCH_ERROR_MESSAGE: "카탈로그에 도구가 없으므로 요청된 도구를 실행할 수 없습니다.",
    LIBERTY_UI_ERROR_MESSAGE_TITLE: "오류",
    LIBERTY_UI_WARNING_MESSAGE_TITLE: "경고",
    LIBERTY_UI_INFO_MESSAGE_TITLE: "정보",
    LIBERTY_UI_CATALOG_GET_ERROR: "카탈로그를 가져오는 중에 오류 발생: {0}",
    LIBERTY_UI_CATALOG_GET_TOOL_ERROR: "카탈로그에서 {0} 도구를 가져오는 중에 오류 발생: {1}",
    PREFERENCES_TITLE: "환경 설정",
    PREFERENCES_SECTION_TITLE: "환경 설정",
    PREFERENCES_ENABLE_BIDI: "양방향 지원 사용",
    PREFERENCES_BIDI_TEXTDIR: "텍스트 방향",
    PREFERENCES_BIDI_TEXTDIR_LTR: "왼쪽에서 오른쪽으로",
    PREFERENCES_BIDI_TEXTDIR_RTL: "오른쪽에서 왼쪽으로",
    PREFERENCES_BIDI_TEXTDIR_CONTEXTUAL: "컨텍스트에 따라",
    PREFERENCES_SET_ERROR_MESSAGE: "도구 상자에서 사용자 환경 설정을 설정하는 중에 오류 발생: {0}",
    BGTASKS_PAGE_LABEL: "백그라운드 태스크",
    BGTASKS_DEPLOYMENT_INSTALLATION_POPUP: "설치 배치 {0}", // {0} is the token number associated with the deployment
    BGTASKS_DEPLOYMENT_INSTALLATION: "설치 배치 {0} - {1}", // {0} is the token number associated with the deployment, {1} is the server name
    BGTASKS_STATUS_RUNNING: "실행 중",
    BGTASKS_STATUS_FAILED: "실패",
    BGTASKS_STATUS_SUCCEEDED: "완료됨", 
    BGTASKS_STATUS_WARNING: "일부 성공",
    BGTASKS_STATUS_PENDING: "보류 중",
    BGTASKS_INFO_DIALOG_TITLE: "세부사항",
    //BGTASKS_INFO_DIALOG_DESC: "Description:",
    BGTASKS_INFO_DIALOG_STDOUT: "표준 출력:",
    BGTASKS_INFO_DIALOG_STDERR: "표준 오류:",
    BGTASKS_INFO_DIALOG_EXCEPTION: "예외:",
    //BGTASKS_INFO_DIALOG_RETURN_CODE: "Return code:",
    BGTASKS_INFO_DIALOG_RESULT: "결과:",
    BGTASKS_INFO_DIALOG_DEPLOYED_ARTIFACT_NAME: "서버 이름:",
    BGTASKS_INFO_DIALOG_DEPLOYED_USER_DIR: "사용자 디렉토리:",
    BGTASKS_POPUP_RUNNING_TASK_TITLE: "활성 백그라운드 태스크",
    BGTASKS_POPUP_RUNNING_TASK_NONE: "없음",
    BGTASKS_POPUP_RUNNING_TASK_NO_ACTIVE_TASK: "활성 백그라운드 태스크 없음",
    BGTASKS_DISPLAY_BUTTON: "태스크 세부사항 및 히스토리",
    BGTASKS_EXPAND: "섹션 펼치기",
    BGTASKS_COLLAPSE: "섹션 접기",
    PROFILE_MENU_HELP_TITLE: "도움말",
    DETAILS_DESCRIPTION: "설명",
    DETAILS_OVERVIEW: "개요",
    DETAILS_OTHERVERSIONS: "기타 버전",
    DETAILS_VERSION: "버전: {0}",
    DETAILS_UPDATED: "업데이트됨: {0}",
    DETAILS_NOTOPTIMIZED: "현재 디바이스에 최적화되지 않았습니다.",
    DETAILS_ADDBUTTON: "내 도구 상자에 추가",
    DETAILS_OPEN: "열기",
    DETAILS_CATEGORY: "카테고리 {0}",
    DETAILS_ADDCONFIRM: "{0} 도구가 도구 상자에 추가되었습니다.",
    CONFIRM_DIALOG_HELP: "도움말",
    YES_BUTTON_LABEL: "{0} 예",  // insert is dialog title
    NO_BUTTON_LABEL: "{0} 아니오",  // insert is dialog title

    YES: "예",
    NO: "아니오",

    TOOL_OIDC_ACCESS_DENIED: "사용자가 이 요청을 완료할 수 있는 권한이 있는 역할에 없습니다.",
    TOOL_OIDC_GENERIC_ERROR: "오류가 발생했습니다. 자세한 정보는 로그의 오류를 검토하십시오.",
    TOOL_DISABLE: "사용자에게 이 도구를 사용할 권한이 없습니다. 관리자 역할의 사용자만 이 도구를 사용할 권한이 있습니다." 
});
