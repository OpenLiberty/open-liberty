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

    // Client Admin
    "CLIENT_ADMIN_TITLE": "OAuth 클라이언트 관리",
    "CLIENT_ADMIN_DESC": "이 도구를 사용하여 클라이언트를 추가 및 편집하고 클라이언트 시크릿을 재생성하십시오.",
    "CLIENT_ADMIN_SEARCH_PLACEHOLDER": "OAuth 클라이언트 이름에 대한 필터링",
    "ADD_NEW_CLIENT": "새 OAuth 클라이언트 추가",
    "CLIENT_NAME": "클라이언트 이름",
    "CLIENT_ID": "클라이언트 ID",
    "EDIT_ARIA": "{0} OAuth 클라이언트 편집",      // {0} - name
    "DELETE_ARIA": "{0} OAuth 클라이언트 삭제",  // {0} - name
    "CLIENT_SECRET": "클라이언트 시크릿",
    "GRANT_TYPES": "권한 부여 유형",
    "SCOPE": "범위",
    "PREAUTHORIZED_SCOPE": "사전 권한 부여된 범위(선택사항)",
    "REDIRECT_URLS": "경로 재지정 URL(선택사항)",
    "CLIENT_SECRET_CHECKBOX": "클라이언트 시크릿 재생성",
    "NONE_SELECTED": "선택사항 없음",
    "MODAL_EDIT_TITLE": "OAuth 클라이언트 편집",
    "MODAL_REGISTER_TITLE": "새 OAuth 클라이언트 등록",
    "MODAL_SECRET_REGISTER_TITLE": "OAuth 등록이 저장됨",
    "MODAL_SECRET_UPDATED_TITLE": "OAuth 등록이 업데이트됨",
    "MODAL_DELETE_CLIENT_TITLE": "이 OAuth 클라이언트 삭제",
    "RESET_GRANT_TYPE": "선택한 모든 권한 부여 유형 지우기",
    "SELECT_ONE_GRANT_TYPE": "하나 이상의 권한 부여 유형 선택",
    "SPACE_HELPER_TEXT": "공백으로 구분된 값",
    "REDIRECT_URL_HELPER_TEXT": "공백으로 구분된 절대 경로 재지정 URL",
    "DELETE_OAUTH_CLIENT_DESC": "이 조작은 클라이언트 등록 서비스에서 등록된 클라이언트를 삭제합니다.",
    "REGISTRATION_SAVED": "클라이언트 ID 및 클라이언트 시크릿이 생성되고 지정되었습니다.",
    "REGISTRATION_UPDATED": "이 클라이언트에 대한 새 클라이언트 시크릿이 생성되고 지정되었습니다.",
    "COPY_CLIENT_ID": "클립보드로 클라이언트 ID 복사",
    "COPY_CLIENT_SECRET": "클립보드로 클라이언트 시크릿 복사",
    "REGISTRATION_UPDATED_NOSECRET": "{0} OAuth 클라이언트가 업데이트되었습니다.",                 // {0} - client name
    "ERR_MULTISELECT_GRANT_TYPES": "하나 이상의 권한 부여 유형을 선택해야 합니다.",
    "ERR_REDIRECT_URIS": "값은 절대 URI여야 합니다.",
    "GENERIC_REGISTER_FAIL": "OAuth 클라이언트를 등록하는 중에 오류 발생",
    "GENERIC_UPDATE_FAIL": "OAuth 클라이언트를 업데이트하는 중에 오류 발생",
    "GENERIC_DELETE_FAIL": "OAuth 클라이언트를 삭제하는 중에 오류 발생",
    "GENERIC_MISSING_CLIENT": "OAuth 클라이언트를 검색하는 중에 오류 발생",
    "GENERIC_REGISTER_FAIL_MSG": "{0} OAuth 클라이언트를 등록하는 중에 오류가 발생했습니다.",  // {0} - client name
    "GENERIC_UPDATE_FAIL_MSG": "{0} OAuth 클라이언트를 업데이트하는 중에 오류가 발생했습니다.",       // {0} - client name
    "GENERIC_DELETE_FAIL_MSG": "{0} OAuth 클라이언트를 삭제하는 중에 오류가 발생했습니다.",       // {0} - client name
    "GENERIC_MISSING_CLIENT_MSG": "ID가 {1}인 OAuth 클라이언트 {0}을(를) 찾을 수 없습니다.",     // {0} - client name; {1} - an ID
    "GENERIC_RETRIEVAL_FAIL_MSG": "{0} OAuth 클라이언트에 대한 정보를 검색하는 중에 오류가 발생했습니다.", // {0} - client name
    "GENERIC_GET_CLIENTS_FAIL": "OAuth 클라이언트를 검색하는 중에 오류 발생",
    "GENERIC_GET_CLIENTS_FAIL_MSG": "OAuth 클라이언트의 목록을 검색하는 중에 오류가 발생했습니다.",

    "RESET_SELECTION": "선택한 모든 {0} 지우기 ",     // {0} - field name (ie 'Grant types')
    "NUMBER_SELECTED": "선택한 {0}의 수",     // {0} - field name
    "OPEN_LIST": "{0} 목록을 엽니다.",                   // {0} - field name
    "CLOSE_LIST": "{0} 목록을 닫습니다.",                 // {0} - field name
    "ENTER_PLACEHOLDER": "값 입력",
    "ADD_VALUE": "요소 추가",
    "REMOVE_VALUE": "요소 제거",
    "REGENERATE_CLIENT_SECRET": "'*'는 기존 값을 유지합니다. 공백 값은 새 client_secret을 생성합니다. 비어 있지 않은 매개변수 값은 기존 값을 새로 지정되는 값으로 대체합니다. ",
    "ALL_OPTIONAL": "모든 필드는 선택사항임"
};
