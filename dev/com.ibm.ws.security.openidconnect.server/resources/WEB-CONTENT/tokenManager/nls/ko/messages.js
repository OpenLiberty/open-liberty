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
    "CLEAR_SEARCH": "검색 입력 지우기",
    "CLEAR_FILTER": "필터 지우기",
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
    "TABLE_BATCH_BAR": "테이블 조치 표시줄",
    "TABLE_FIELD_SORT_ASC": "테이블은 {0}별로 오름차순으로 정렬됩니다.",   // {0} - column name (ie. 'Name', 'Client ID')
    "TABLE_FIELD_SORT_DESC": "테이블은 {0}별로 내림차순으로 정렬됩니다.", // {0} - column name (ie. 'Name', 'Client ID')
    "TRUE": "True",
    "TRY_AGAIN": "다시 시도...",
    "UPDATE": "업데이트",

    // Common Column Names
    "CLIENT_NAME_COL": "클라이언트 이름",
    "EXPIRES_COL": "만기 날짜",
    "ISSUED_COL": "발행 날짜",
    "NAME_COL": "이름",
    "TYPE_COL": "유형",

    // Token Manager
    // 'app-token' and 'app-password' are keywords - don't translate
    // name - user defined name given to the app-password/app-token; user id - user's login name
    "TOKEN_MGR_TITLE": "토큰 삭제",
    "TOKEN_MGR_DESC": "지정된 사용자의 app-password 및 app-token을 삭제합니다.",
    "TOKEN_MGR_SEARCH_PLACEHOLDER": "사용자 ID 입력",
    "TABLE_FILLED_WITH": "{1}에 속하는 {0} 인증을 표시하도록 테이블을 업데이트했습니다. ",  // 0 - number of entries in table; 1 - user id
    "DELETE_SELECTED": "선택한 app-passwords 및 app-tokens 삭제",
    "DELETE_ARIA": "이름이 {1}인 {0} 삭제",         // 0 - 'app-password' or 'app-token'; 1 - name
    "DELETE_PW": "이 app-password 삭제",
    "DELETE_TOKEN": "이 app-token 삭제",
    "DELETE_FOR_USERID": "{1}에 대한 {0}",                // 0 - name; 1 - user id
    "DELETE_WARNING_PW": "이 조치는 현재 지정된 app-password를 제거합니다.",
    "DELETE_WARNING_TOKEN": "이 조치는 현재 지정된 app-token을 제거합니다.",
    "DELETE_MANY": "App-Password/App-Token 삭제",
    "DELETE_MANY_FOR": "{0}에 지정됨",              // 0 - user id
    "DELETE_ONE_MESSAGE": "이 조치는 선택한 app-password/app-token을 삭제합니다. ",
    "DELETE_MANY_MESSAGE": "이 조치는 {0}개의 선택된 app-password/app-token을 삭제합니다.",  // 0 - number
    "DELETE_ALL_MESSAGE": "이 조치는 {0}에 속한 모든 app-password/app-token을 삭제합니다.", // 0 - user id
    "DELETE_NONE": "삭제하도록 선택",
    "DELETE_NONE_MESSAGE": "삭제할 app-password 또는 app-token을 표시하려면 선택란을 선택하십시오.",
    "SINGLE_ITEM_SELECTED": "1개 항목이 선택됨",
    "ITEMS_SELECTED": "{0}개 항목이 선택됨",            // 0 - number
    "SELECT_ALL_AUTHS": "이 사용자에 대한 모든 app-password 및 app-token 선택",
    "SELECT_SPECIFIC": "삭제할 이름이 {1}인 {0}을(를) 선택하십시오.",  // 0 - 'app-password' or 'app-token; 1 - name
    "NO_QUERY": "무언가를 찾으려고 하십니까? app-password 및 app-token을 보려면 사용자 ID를 입력하십시오.",
    "GENERIC_FETCH_FAIL": "{0}을(를) 검색하는 중에 오류 발생",      // 0 - 'App-Passwords' or 'App-Tokens'
    "GENERIC_FETCH_FAIL_MSG": "{1}에 속한 {0}의 목록을 가져올 수 없습니다.", // 0 - 'app-passwords' or 'app-tokens; 1 - user id
    "GENERIC_DELETE_FAIL": "{0}을(를) 삭제하는 중에 오류 발생",       // 0 - 'App-Password' or 'App-Token'
    "GENERIC_DELETE_FAIL_MSG": "이름이 {1}인 {0}을(를) 삭제하는 중에 오류가 발생했습니다.",  // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_DELETE_ALL_FAIL_MSG": "{1}에 대한 {0}을(를) 삭제하는 중에 오류가 발생했습니다.",     // 0 - 'app-passwords' or 'app-tokens'; 1 - user id
    "GENERIC_DELETE_FAIL_NOTYPES": "삭제하는 중에 오류 발생",
    "GENERIC_DELETE_FAIL_NOTYPES_ONE_MSG": "다음 app-password 또는 app-token을 삭제하는 중에 오류 발생:",
    "GENERIC_DELETE_FAIL_NOTYPES_MSG": "다음 {0} app-password 및 app-token을 삭제하는 중에 오류 발생:",  // 0 - number
    "IDENTIFY_AUTH": "{0} {1}",   // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_FETCH_ALL_FAIL": "인증을 검색하는 중에 오류 발생",
    "GENERIC_FETCH_ALL_FAIL_MSG": "{0}에 속한 app-password 및 app-token의 목록을 가져올 수 없습니다.",   // 0 - user id
    "GENERIC_NOT_CONFIGURED": "클라이언트가 구성되지 않음",
    "GENERIC_NOT_CONFIGURED_MSG": "appPasswordAllowed 및 appTokenAllowed 클라이언트 속성이 구성되지 않았습니다. 데이터를 검색할 수 없습니다."
};
