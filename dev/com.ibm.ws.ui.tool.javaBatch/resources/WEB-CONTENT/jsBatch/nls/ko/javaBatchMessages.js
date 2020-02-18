/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

define({
      ACCOUNTING_STRING : "어카운팅 문자열",
      SEARCH_RESOURCE_TYPE_ALL : "모두",
      SEARCH : "검색",
      JAVA_BATCH_SEARCH_BOX_LABEL : "검색 기준 추가 단추를 선택한 다음 값을 지정하여 검색 기준 입력",
      SUBMITTED : "제출",
      JMS_QUEUED : "JMS 큐에 대기",
      JMS_CONSUMED : "JMS 소비",
      JOB_PARAMETER : "작업 매개변수",
      DISPATCHED : "디스패치",
      FAILED : "실패",
      STOPPED : "중지",
      COMPLETED : "완료",
      ABANDONED : "중지",
      STARTED : "시작됨",
      STARTING : "시작 중",
      STOPPING : "중지 중",
      REFRESH : "새로 고치기",
      INSTANCE_STATE : "인스턴스 상태",
      APPLICATION_NAME : "애플리케이션 이름",
      APPLICATION: "애플리케이션",
      INSTANCE_ID : "인스턴스 ID",
      LAST_UPDATE : "마지막 업데이트 날짜",
      LAST_UPDATE_RANGE : "마지막 업데이트 날짜 범위",
      LAST_UPDATED_TIME : "마지막 업데이트 시간",
      DASHBOARD_VIEW : "대시보드 보기",
      HOMEPAGE : "홈 페이지",
      JOBLOGS : "작업 로그",
      QUEUED : "큐에 대기",
      ENDED : "종료",
      ERROR : "오류",
      CLOSE : "닫기",
      WARNING : "경고",
      GO_TO_DASHBOARD: "대시보드로 이동",
      DASHBOARD : "대시보드",
      BATCH_JOB_NAME: "일괄처리 작업 이름",
      SUBMITTER: "제출자",
      BATCH_STATUS: "일괄처리 상태",
      EXECUTION_ID: "작업 실행 ID",
      EXIT_STATUS: "종료 상태",
      CREATE_TIME: "작성 시간",
      START_TIME: "시작 시간",
      END_TIME: "종료 시간",
      SERVER: "서버",
      SERVER_NAME: "서버 이름",
      SERVER_USER_DIRECTORY: "사용자 디렉토리",
      SERVERS_USER_DIRECTORY: "서버의 사용자 디렉토리",
      HOST: "호스트",
      NAME: "이름",
      JOB_PARAMETERS: "작업 매개변수",
      JES_JOB_NAME: "JES 작업 이름",
      JES_JOB_ID: "JES 작업 ID",
      ACTIONS: "조치",
      VIEW_LOG_FILE: "로그 보기 파일",
      STEP_NAME: "단계 이름",
      ID: "ID",
      PARTITION_ID: "파티션 {0}",                               // Partition ID number
      VIEW_EXECUTION_DETAILS: "작업 실행 {0} 세부사항 보기",    // Job Execution ID number
      PARENT_DETAILS: "상위 정보 세부사항",
      TIMES: "시간",      // Heading on section referencing create, start, and end timestamps
      STATUS: "상태",
      SEARCH_ON: "{1} {0}에서 필터링하기 위해 선택",                    // {0} Value, {1} Column name
      SEARCH_PILL_INPUT : "검색 기준을 입력하십시오.",
      BREADCRUMB_JOB_INSTANCE : "작업 인스턴스 {0}",                // Job Instance ID
      BREADCRUMB_JOB_EXECUTION : "작업 실행 {0}",              // Job Execution ID
      BREADCRUMB_JOB_LOG : "작업 로그 {0}",
      BATCH_SEARCH_CRITERIA_INVALID : "검색 기준이 올바르지 않습니다.",
      ERROR_CANNOT_HAVE_MULTIPLE_SEARCHBY : "검색 기준은 {0} 매개변수별로 여러 필터를 사용할 수 없습니다.", // {0} will be another translated message key like SUBMITTER and BATCH_JOB_NAME

      INSTANCES_TABLE_IDENTIFIER: "작업 인스턴스 테이블",
      EXECUTIONS_TABLE_IDENTIFIER: "작업 실행 테이블",
      STEPS_DETAILS_TABLE_IDENTIFIER: "단계 세부사항 테이블",
      LOADING_VIEW : "페이지가 현재 정보를 로드하는 중",
      LOADING_VIEW_TITLE : "로딩 보기",
      LOADING_GRID : "서버에서 리턴할 검색 결과 대기 중",
      PAGENUMBER : "페이지 번호",
      SELECT_QUERY_SIZE: "조회 크기 선택",
      LINK_EXPLORE_HOST: "탐색 도구에서 {0} 호스트에 대한 세부사항을 보도록 선택하십시오. ",      // Host name
      LINK_EXPLORE_SERVER: "탐색 도구에서 {0} 서버에 대한 세부사항을 보도록 선택하십시오. ",  // Server name

      //ACTIONS
      RESTART: "다시 시작",
      STOP: "중지",
      PURGE: "제거",
      OK_BUTTON_LABEL: "확인",
      INSTANCE_ACTIONS_BUTTON_LABEL: "작업 인스턴스 {0}의 조치",     // Job Instance ID number
      INSTANCE_ACTIONS_MENU_LABEL:  "작업 인스턴스 조치 메뉴",

      RESTART_INSTANCE_MESSAGE: "작업 인스턴스 {0}과(와) 연관된 최신 작업 실행을 다시 시작하시겠습니까?",     // Job Instance ID number
      STOP_INSTANCE_MESSAGE: "작업 인스턴스 {0}과(와) 연관된 최신 작업 실행을 중지하시겠습니까?",           // Job Instance ID number
      PURGE_INSTANCE_MESSAGE: "모든 데이터베이스 항목 및 작업 인스턴스 {0}과(와) 연관된 작업 로그를 제거하시겠습니까?",     // Job Instance ID number
      PURGE_JOBSTORE_ONLY: "작업 저장소만 제거",

      RESTART_INST_ERROR_MESSAGE: "다시 시작 요청이 실패했습니다.",
      STOP_INST_ERROR_MESSAGE: "중지 요청이 실패했습니다.",
      PURGE_INST_ERROR_MESSAGE: "제거 요청이 실패했습니다.",
      ACTION_REQUEST_ERROR_MESSAGE: "조치 요청이 실패했습니다. 상태 코드: {0}. URL: {1}",  // Status Code number, URL string

      // RESTART JOB WITH PARAMETERS DIALOG
      REUSE_PARMS_TOGGLE_LABEL: "이전 실행에서 매개변수 재사용",
      JOB_PARAMETERS_EMPTY: "'{0}'이(가) 선택되는 경우 이 영역을 사용하여 작업 매개변수를 입력하십시오.",   // 0 - Checkbox label - REUSE_PARMS_TOGGLE_LABEL
      JOB_PARAMETER_NAME: "매개변수 이름",
      JOB_PARAMETER_VALUE: "매개변수 값",
      PARM_NAME_COLUMN_HEADER: "매개변수",
      PARM_VALUE_COLUMN_HEADER: "값",
      PARM_ADD_ICON_TITLE: "매개변수 추가",
      PARM_REMOVE_ICON_TITLE: "매개변수 제거",
      PARMS_ENTRY_ERROR: "매개변수 이름은 필수입니다.",
      JOB_PARAMETER_CREATE: "{0}을(를) 선택하여 매개변수를 작업 인스턴스의 다음 실행에 추가하십시오.",  // 0 - Button label
      JOB_PARAMETER_CREATE_BUTTON: "테이블 헤더의 매개변수 단추 추가",

      //JOB LOGS PAGE MESSAGES
      JOBLOGS_LOG_CONTENT : "작업 로그 컨텐츠",
      FILE_DOWNLOAD : "파일 다운로드",
      DOWNLOAD_DIALOG_DESCRIPTION : "로그 파일을 다운로드하시겠습니까?",
      INCLUDE_ALL_LOGS : "작업 실행의 모든 로그 파일 포함",
      LOGS_NAVIGATION_BAR : "작업 로그 탐색줄",
      DOWNLOAD : "다운로드",
      LOG_TOP : "로그의 맨 위",
      LOG_END : "로그의 끝",
      PREVIOUS_PAGE : "이전 페이지",
      NEXT_PAGE : "다음 페이지",
      DOWNLOAD_ARIA : "파일 다운로드",

      //Error messages for popups
      REST_CALL_FAILED : "데이터 페치 호출이 실패했습니다.",
      NO_JOB_EXECUTION_URL : "작업 실행 번호가 URL에 제공되지 않았거나 인스턴스에 표시할 작업 실행 로그가 없습니다.",
      NO_VIEW : "URL 오류: 해당 보기가 없음",
      WRONG_TOOL_ID : "URL의 조회 문자열이 도구 ID {0}(으)로 시작하는 대신 {1}(으)로 시작합니다.",   // {0} and {1} are both Strings
      URL_NO_LOGS : "URL 오류: 로그가 없음",
      NOT_A_NUMBER : "URL 오류: {0}은(는) 숫자여야 합니다. ",                                                // {0} is a field name
      PARAMETER_REPETITION : "URL 오류: {0}은(는) 매개변수에 한 번만 있을 수 있습니다. ",                   // {0} is a field name
      URL_PAGE_PARAMS_ERROR : "URL 오류: 페이지 매개변수가 범위 내에 없음",
      INVALID_PARAMETER : "URL 오류: {0}은(는) 올바른 매개변수가 아님",                                       // {0} is a String
      URL_MULTIPLE_ATTRIBUTES : "URL 오류: URL이 작업 실행 또는 작업 인스턴스 중 하나를 지정할 수는 있지만 둘 다 지정할 수는 없음",
      MISSING_EXECUTION_ID_PARAM : "필수 실행 ID 매개변수가 누락되었습니다.",
      PERSISTENCE_CONFIGURATION_REQUIRED : "Java 일괄처리 도구를 사용하려면 Java 일괄처리 지속적 데이터베이스 구성이 필요합니다.",
      IGNORED_SEARCH_CRITERIA : "다음 필터 기준은 결과에서 무시되었음: {0}",

      GRIDX_SUMMARY_TEXT : "최근 ${0}개의 작업 인스턴스 표시"

});

