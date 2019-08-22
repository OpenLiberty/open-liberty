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
    "ADD_NEW": "새로 추가",
    "CANCEL": "취소",
    "CLEAR": "검색 입력 지우기",
    "CLICK_TO_SORT": "열을 정렬하려면 클릭",
    "CLOSE": "닫기",
    "COPY_TO_CLIPBOARD": "클립보드에 복사",
    "COPIED_TO_CLIPBOARD": "클립보드에 복사됨",
    "DELETE": "삭제",
    "DONE": "완료",
    "EDIT": "편집",
    "FALSE": "False",
    "GENERATE": "생성",
    "LOADING": "로드 중",
    "LOGOUT": "로그아웃",
    "NEXT_PAGE": "다음 페이지",
    "NO_RESULTS_FOUND": "결과를 찾을 수 없음",
    "PAGES": "{0}/{1}페이지",   // {0} - current page number; {1} - total pages
    "PAGE_SELECT": "보려는 페이지 번호 선택",
    "PREVIOUS_PAGE": "이전 페이지",
    "PROCESSING": "처리 중",
    "REGENERATE": "재생성",
    "REGISTER": "등록",
    "TABLE_FIELD_SORT_ASC": "테이블은 {0}별로 오름차순으로 정렬됩니다.",   // {0} - column name (ie. 'Name', 'Client ID')
    "TABLE_FIELD_SORT_DESC": "테이블은 {0}별로 내림차순으로 정렬됩니다.", // {0} - column name (ie. 'Name', 'Client ID')
    "TRUE": "True",
    "TRY_AGAIN": "다시 시도...",
    "UPDATE": "업데이트",

    // Common Column Names
    "EXPIRES_COL": "만기 날짜",
    "ISSUED_COL": "발행 날짜",
    "NAME_COL": "이름",
    "TYPE_COL": "유형",

    // Account Manager
    // 'app-token' and 'app-password' are keywords - don't translate
    // name - the user defined name given the app-password or app-token
    "ACCT_MGR_TITLE": "개인 토큰 관리",
    "ACCT_MGR_DESC": "app-password 및 app-token을 작성, 삭제 및 재생성합니다.",
    "ADD_NEW_AUTHENTICATION": "새 app-password 또는 app-token 추가",
    "NAME_IDENTIFIER": "이름: {0}",
    "ADD_NEW_TITLE": "새 인증 등록",
    "NOT_GENERATED_PLACEHOLDER": "생성되지 않음",
    "AUTHENTICAION_GENERATED": "인증이 생성됨",
    "GENERATED_APP_PASSWORD": "app-password가 생성됨",
    "GENERATED_APP_TOKEN": "app-token이 생성됨",
    "COPY_APP_PASSWORD": "클립보드로 app-password 복사",
    "COPY_APP_TOKEN": "클립보드로 app-token 복사",
    "REGENERATE_APP_PASSWORD": "App-Password 재생성",
    "REGENERATE_PW_WARNING": "이 조치는 현재 app-password를 겹쳐씁니다.",
    "REGENERATE_PW_PLACEHOLDER": "{0}에 이전에 생성된 비밀번호",        // 0 - date
    "REGENERATE_APP_TOKEN": "App-Token 재생성",
    "REGENERATE_TOKEN_WARNING": "이 조치는 현재 app-token을 겹쳐씁니다.",
    "REGENERATE_TOKEN_PLACEHOLDER": "{0}에 이전에 생성된 토큰",        // 0 - date
    "DELETE_PW": "이 app-password 삭제",
    "DELETE_TOKEN": "이 app-token 삭제",
    "DELETE_WARNING_PW": "이 조치는 현재 지정된 app-password를 제거합니다.",
    "DELETE_WARNING_TOKEN": "이 조치는 현재 지정된 app-token을 제거합니다.",
    "REGENERATE_ARIA": "{1}에 대한 {0} 재생성",     // 0 - 'app-password' or 'app-token'; 1 - name
    "DELETE_ARIA": "이름이 {1}인 {0} 삭제",       // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_GENERATE_FAIL": "{0}을(를) 생성하는 중에 오류 발생", // 0 - 'App-Password' or 'App-Token'
    "GENERIC_GENERATE_FAIL_MSG": "이름이 {1}인 새 {0}을(를) 생성하는 중에 오류가 발생했습니다.",  // 0 - 'app-password' or 'app-token'; 1 - name
    "ERR_NAME": "이름이 이미 {0}에 연관되어 있거나 너무 깁니다.", // 0 - 'app-password' or 'app-token'
    "GENERIC_DELETE_FAIL": "{0}을(를) 삭제하는 중에 오류 발생",     // 0 - 'App-Password' or 'App-Token'
    "GENERIC_DELETE_FAIL_MSG": "이름이 {1}인 {0}을(를) 삭제하는 중에 오류가 발생했습니다.",  // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_REGENERATE_FAIL": "{0}을(를) 재생성하는 중에 오류 발생",  // 0 - 'App-Password' or 'App-Token'
    "GENERIC_REGENERATE_FAIL_MSG": "이름이 {1}인 {0}을(를) 재생성하는 중에 오류가 발생했습니다.",  // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_REGENERATE_FAIL_CREATE_MSG": "이름이 {1}인 {0}을(를) 재생성하는 중에 오류가 발생했습니다. {0}을(를) 삭제했지만 다시 작성할 수 없습니다.", // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_FETCH_FAIL": "인증을 검색하는 중에 오류 발생",
    "GENERIC_FETCH_FAIL_MSG": "app-password 또는 app-token의 현재 목록을 가져올 수 없습니다.",
    "GENERIC_NOT_CONFIGURED": "클라이언트가 구성되지 않음",
    "GENERIC_NOT_CONFIGURED_MSG": "appPasswordAllowed 및 appTokenAllowed 클라이언트 속성이 구성되지 않았습니다. 데이터를 검색할 수 없습니다.",
    "APP_PASSWORD_NOT_CONFIGURED": "appPasswordAllowed 클라이언트 속성이 구성되지 않았습니다.",  // 'appPasswordAllowed' is a config option. Do not translate.
    "APP_TOKEN_NOT_CONFIGURED": "appTokenAllowed 클라이언트 속성이 구성되지 않았습니다."         // 'appTokenAllowed' is a config option.  Do not translate.
};
