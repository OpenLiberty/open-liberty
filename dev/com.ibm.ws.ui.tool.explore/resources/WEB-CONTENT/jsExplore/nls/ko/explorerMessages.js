/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
define({
    EXPLORER : "탐색기",
    EXPLORE : "탐색",
    DASHBOARD : "대시보드",
    DASHBOARD_VIEW_ALL_APPS : "모든 애플리케이션 보기",
    DASHBOARD_VIEW_ALL_SERVERS : "모든 서버 보기",
    DASHBOARD_VIEW_ALL_CLUSTERS : "모든 클러스터 보기",
    DASHBOARD_VIEW_ALL_HOSTS : "모든 호스트 보기",
    DASHBOARD_VIEW_ALL_RUNTIMES : "모든 런타임 보기",
    SEARCH : "검색",
    SEARCH_RECENT : "최신 검색",
    SEARCH_RESOURCES : "자원 검색",
    SEARCH_RESULTS : "검색 결과",
    SEARCH_NO_RESULTS : "결과 없음",
    SEARCH_NO_MATCHES : "일치 항목 없음",
    SEARCH_TEXT_INVALID : "검색 텍스트에 올바르지 않은 문자가 있음",
    SEARCH_CRITERIA_INVALID : "검색 기준이 올바르지 않습니다.",
    SEARCH_CRITERIA_INVALID_COMBO :"{1}이(과) 함께 지정된 경우 {0}은(는) 올바르지 않습니다.",
    SEARCH_CRITERIA_INVALID_DUPLICATE : "{0}은(는) 한 번만 지정하십시오.",
    SEARCH_TEXT_MISSING : "검색 텍스트는 필수임",
    SEARCH_UNSUPPORT_TYPE_APPONSERVER : "서버에서 애플리케이션 태그 검색은 지원되지 않습니다.",
    SEARCH_UNSUPPORT_TYPE_APPONCLUSTER : "클러스터에서 애플리케이션 태그 검색은 지원되지 않습니다.",
    SEARCH_UNSUPPORT : "검색 기준은 지원되지 않습니다.",
    SEARCH_SWITCH_VIEW : "보기 전환",
    FILTERS : "필터",
    DEPLOY_SERVER_PACKAGE : "서버 패키지 배치",
    MEMBER_OF : "다음의 멤버임 -",
    N_CLUSTERS: "{0} 클러스터 ...",

    INSTANCE : "인스턴스",
    INSTANCES : "인스턴스",
    APPLICATION : "응용프로그램",
    APPLICATIONS : "애플리케이션",
    SERVER : "서버",
    SERVERS : "서버",
    CLUSTER : "클러스터",
    CLUSTERS : "클러스터",
    CLUSTER_NAME : "클러스터 이름: ",
    CLUSTER_STATUS : "클러스터 상태: ",
    APPLICATION_NAME : "애플리케이션 이름: ",
    APPLICATION_STATE : "애플리케이션 상태: ",
    HOST : "호스트",
    HOSTS : "호스트",
    RUNTIME : "런타임",
    RUNTIMES : "런타임",
    PATH : "경로",
    CONTROLLER : "제어기",
    CONTROLLERS : "제어기",
    OVERVIEW : "개요",
    CONFIGURE : "구성",

    SEARCH_RESOURCE_TYPE: "유형", // Search by resource types
    SEARCH_RESOURCE_STATE: "상태", // Search by resource types
    SEARCH_RESOURCE_TYPE_ALL: "모두", // Search all resource types
    SEARCH_RESOURCE_NAME: "이름", // Search by resource name
    SEARCH_RESOURCE_TAG: "태그", // Search by resource tag
    SEARCH_RESOURCE_CONTAINER: "컨테이너", // Search by container type
    SEARCH_RESOURCE_CONTAINER_DOCKER: "Docker", // Search by container type Docker
    SEARCH_RESOURCE_CONTAINER_NONE: "없음", // Search by container type none
    SEARCH_RESOURCE_RUNTIMETYPE: "런타임 유형", // Search by runtime type
    SEARCH_RESOURCE_OWNER: "소유자", // Search by owner
    SEARCH_RESOURCE_CONTACT: "문의처", // Search by contact
    SEARCH_RESOURCE_NOTE: "참고", // Search by note

    GRID_HEADER_USERDIR : "사용자 디렉토리",
    GRID_HEADER_NAME : "이름",
    GRID_LOCATION_NAME : "위치",
    GRID_ACTIONS : "그리드 조치",
    GRID_ACTIONS_LABEL : "{0} 그리드 조치",  // name of the grid
    APPONSERVER_LOCATION_NAME : "{1}의 {0}({2})", // server on host (/path)

    STATS : "모니터",
    STATS_ALL : "모두",
    STATS_VALUE : "값: {0}",
    CONNECTION_IN_USE_STATS : "{0} 사용 중 = {1} 관리 - {2} 미사용",
    CONNECTION_IN_USE_STATS_VALUE : "값: {0} 사용 중 = {1} 관리 - {2} 미사용",
    DATA_SOURCE : "데이터 소스: {0}",
    STATS_DISPLAY_LEGEND : "범례 표시",
    STATS_HIDE_LEGEND : "범례 숨기기",
    STATS_VIEW_DATA : "차트 데이터 보기",
    STATS_VIEW_DATA_TIMESTAMP : "시간소인",
    STATS_ACTION_MENU : "{0} 조치 메뉴",
    STATS_SHOW_HIDE : "자원 메트릭 추가",
    STATS_SHOW_HIDE_SUMMARY : "요약을 위한 메트릭 추가",
    STATS_SHOW_HIDE_TRAFFIC : "트래픽을 위한 메트릭 추가",
    STATS_SHOW_HIDE_PERFORMANCE : "성능을 위한 메트릭 추가",
    STATS_SHOW_HIDE_AVAILABILITY : "가용성을 위한 메트릭 추가",
    STATS_SHOW_HIDE_ALERT : "경보를 위한 메트릭 추가",
    STATS_SHOW_HIDE_LIST_BUTTON : "자원 메트릭 목록 표시 또는 숨기기",
    STATS_SHOW_HIDE_BUTTON_TITLE : "차트 편집",
    STATS_SHOW_HIDE_CONFIRM : "저장",
    STATS_SHOW_HIDE_CANCEL : "취소",
    STATS_SHOW_HIDE_DONE : "완료",
    STATS_DELETE_GRAPH : "차트 삭제",
    STATS_ADD_CHART_LABEL : "보려는 차트 추가",
    STATS_JVM_TITLE : "JVM",
    STATS_JVM_BUTTON_LABEL : "보려는 모든 JVM 차트 추가",
    STATS_HEAP_TITLE : "사용된 힙 메모리",
    STATS_HEAP_USED : "사용됨: {0}MB",
    STATS_HEAP_COMMITTED : "커미트됨: {0}MB",
    STATS_HEAP_MAX : "최대: {0}MB",
    STATS_HEAP_X_TIME : "시간",
    STATS_HEAP_Y_MB : "사용된 MB",
    STATS_HEAP_Y_MB_LABEL : "{0}MB",
    STATS_CLASSES_TITLE : "로드된 클래스",
    STATS_CLASSES_LOADED : "로드됨: {0}",
    STATS_CLASSES_UNLOADED : "로드 해제됨: {0}",
    STATS_CLASSES_TOTAL : "총계: {0}",
    STATS_CLASSES_Y_TOTAL : "로드된 클래스",
    STATS_PROCESSCPU_TITLE : "CPU 사용량",
    STATS_PROCESSCPU_USAGE : "CPU 사용량: {0}%",
    STATS_PROCESSCPU_Y_PERCENT : "CPU 백분율",
    STATS_PROCESSCPU_Y_PCT_LABEL : "{0}%",
    STATS_THREADS_TITLE : "활성 JVM 스레드",
    STATS_LIVE_MSG_INIT : "라이브 데이터 표시 중",
    STATS_LIVE_MSG :"이 차트에는 히스토리 데이터가 없습니다. 여기에는 최근 10분 동안의 데이터가 계속 표시됩니다.",
    STATS_THREADS_ACTIVE : "라이브: {0}",
    STATS_THREADS_PEAK : "최대: {0}",
    STATS_THREADS_TOTAL : "총계: {0}",
    STATS_THREADS_Y_THREADS : "스레드",
    STATS_TP_POOL_SIZE : "풀 크기",
    STATS_JAXWS_TITLE : "JAX-WS 웹 서비스",
    STATS_JAXWS_BUTTON_LABEL : "보려는 모든 JAX-WS 웹 서비스 차트 추가",
    STATS_JW_AVG_RESP_TIME : "평균 응답 시간",
    STATS_JW_AVG_INVCOUNT : "평균 호출 개수",
    STATS_JW_TOTAL_FAULTS : "런타임 결함 총계",
    STATS_LA_RESOURCE_CONFIG_LABEL : "자원 선택...",
    STATS_LA_RESOURCE_CONFIG_LABEL_NUM : "{0}개 자원",
    STATS_LA_RESOURCE_CONFIG_LABEL_NUM_1 : "1개 자원",
    STATS_LA_RESOURCE_CONFIG_SELECT_ONE : "하나 이상의 자원을 선택해야 합니다.",
    STATS_LA_RESOURCE_CONFIG_NO_DATA : "선택된 시간 범위에 대해 사용 가능한 데이터가 없습니다.",
    STATS_ACCESS_LOG_TITLE : "액세스 로그",
    STATS_ACCESS_LOG_BUTTON_LABEL : "보려는 모든 액세스 로그 차트 추가",
    STATS_ACCESS_LOG_GRAPH : "액세스 로그 메시지 개수",
    STATS_ACCESS_LOG_SUMMARY : "액세스 로그 요약",
    STATS_ACCESS_LOG_TABLE : "액세스 로그 메시지 목록",
    STATS_MESSAGES_TITLE : "메시지 및 추적",
    STATS_MESSAGES_BUTTON_LABEL : "보려는 모든 메시지 및 추적 차트 추가",
    STATS_MESSAGES_GRAPH : "로그 메시지 개수",
    STATS_MESSAGES_TABLE : "로그 메시지 목록",
    STATS_FFDC_GRAPH : "FFDC 개수",
    STATS_FFDC_TABLE : "FFDC 목록",
    STATS_TRACE_LOG_GRAPH : "추적 메시지 개수",
    STATS_TRACE_LOG_TABLE : "추적 메시지 목록",
    STATS_THREAD_POOL_TITLE : "스레드 풀",
    STATS_THREAD_POOL_BUTTON_LABEL : "보려는 모든 스레드 풀 차트 추가",
    STATS_THREADPOOL_TITLE : "활성 Liberty 스레드",
    STATS_THREADPOOL_SIZE : "풀 크기: {0}",
    STATS_THREADPOOL_ACTIVE : "활성: {0}",
    STATS_THREADPOOL_TOTAL : "총계: {0}",
    STATS_THREADPOOL_Y_ACTIVE : "활성 스레드",
    STATS_SESSION_MGMT_TITLE : "세션",
    STATS_SESSION_MGMT_BUTTON_LABEL : "보려는 모든 세션 차트 추가",
    STATS_SESSION_CONFIG_LABEL : "세션 선택...",
    STATS_SESSION_CONFIG_LABEL_NUM  : "{0}개 세션",
    STATS_SESSION_CONFIG_LABEL_NUM_1  : "1개 세션",
    STATS_SESSION_CONFIG_SELECT_ONE : "하나 이상의 세션을 선택해야 합니다.",
    STATS_SESSION_TITLE : "활성 세션",
    STATS_SESSION_Y_ACTIVE : "활성 세션",
    STATS_SESSION_LIVE_LABEL : "라이브 개수: {0}",
    STATS_SESSION_CREATE_LABEL : "작성 개수: {0}",
    STATS_SESSION_INV_LABEL : "무효화된 개수: {0}",
    STATS_SESSION_INV_TIME_LABEL : "제한시간에 의해 무효화된 개수: {0}",
    STATS_WEBCONTAINER_TITLE : "웹 애플리케이션",
    STATS_WEBCONTAINER_BUTTON_LABEL : "보려는 모든 웹 애플리케이션 차트 추가",
    STATS_SERVLET_CONFIG_LABEL : "서블릿 선택...",
    STATS_SERVLET_CONFIG_LABEL_NUM : "{0}개 서블릿",
    STATS_SERVLET_CONFIG_LABEL_NUM_1 : "1개 서블릿",
    STATS_SERVLET_CONFIG_SELECT_ONE : "하나 이상의 서블릿을 선택해야 합니다.",
    STATS_SERVLET_REQUEST_COUNT_TITLE : "요청 개수",
    STATS_SERVLET_REQUEST_COUNT_Y_AXIS : "요청 개수",
    STATS_SERVLET_RESPONSE_COUNT_TITLE : "응답 개수",
    STATS_SERVLET_RESPONSE_COUNT_Y_AXIS : "응답 개수",
    STATS_SERVLET_RESPONSE_MEAN_TITLE : "평균 응답 시간(ns)",
    STATS_SERVLET_RESPONSE_MEAN_Y_AXIS : "응답 시간(ns)",
    STATS_CONN_POOL_TITLE : "연결 풀",
    STATS_CONN_POOL_BUTTON_LABEL : "보려는 모든 연결 풀 차트 추가",
    STATS_CONN_POOL_CONFIG_LABEL : "데이터 소스 선택...",
    STATS_CONN_POOL_CONFIG_LABEL_NUM : "{0}개 데이터 소스",
    STATS_CONN_POOL_CONFIG_LABEL_NUM_1 : "1개 데이터 소스",
    STATS_CONN_POOL_CONFIG_SELECT_ONE : "하나 이상의 데이터 소스를 선택해야 합니다.",
    STATS_CONNECT_IN_USE_TITLE : "사용 중인 연결",
    STATS_CONNECT_USED_COUNT_Y_AXIS : "연결",
    STATS_CONNECT_IN_USE_LABEL : "사용 중: {0}",
    STATS_CONNECT_USED_USED_LABEL : "사용됨: {0}",
    STATS_CONNECT_USED_FREE_LABEL : "사용 가능: {0}",
    STATS_CONNECT_USED_CREATE_LABEL : "작성됨: {0}",
    STATS_CONNECT_USED_DESTROY_LABEL : "영구 삭제됨: {0}",
    STATS_CONNECT_WAIT_TIME_TITLE : "평균 대기 시간(ms)",
    STATS_CONNECT_WAIT_TIME_Y_AXIS : "대기 시간(ms)",
    STATS_TIME_ALL : "모두",
    STATS_TIME_1YEAR : "1년",
    STATS_TIME_1MONTH : "1개월",
    STATS_TIME_1WEEK : "1주",
    STATS_TIME_1DAY : "1일",
    STATS_TIME_1HOUR : "1시간",
    STATS_TIME_10MINUTES : "10분",
    STATS_TIME_5MINUTES : "5분",
    STATS_TIME_1MINUTE : "1분",
    STATS_PERSPECTIVE_SUMMARY : "요약",
    STATS_PERSPECTIVE_TRAFFIC : "트래픽",
    STATS_PERSPECTIVE_TRAFFIC_JVM : "JVM 트래픽",
    STATS_PERSPECTIVE_TRAFFIC_CONN : "연결 트래픽",
    STATS_PERSPECTIVE_TRAFFIC_LAACCESS : "액세스 로그 트래픽",
    STATS_PERSPECTIVE_PROBLEM : "문제점",
    STATS_PERSPECTIVE_PERFORMANCE : "성능",
    STATS_PERSPECTIVE_PERFORMANCE_JVM : "JVM 성능",
    STATS_PERSPECTIVE_PERFORMANCE_CONN : "연결 성능",
    STATS_PERSPECTIVE_ALERT : "경보 분석",
    STATS_PERSPECTIVE_ALERT_LAACCESS : "액세스 로그 경보",
    STATS_PERSPECTIVE_ALERT_LAMSGS : "메시지 및 추적 로그 경보",
    STATS_PERSPECTIVE_AVAILABILITY : "가용성",

    STATS_DISPLAY_TIME_LAST_MINUTE_LABEL : "최근 1분",
    STATS_DISPLAY_TIME_LAST_5_MINUTES_LABEL : "최근 5분",
    STATS_DISPLAY_TIME_LAST_10_MINUTES_LABEL : "최근 10분",
    STATS_DISPLAY_TIME_LAST_HOUR_LABEL : "최근 1시간",
    STATS_DISPLAY_TIME_LAST_DAY_LABEL : "최근 1일",
    STATS_DISPLAY_TIME_LAST_WEEK_LABEL : "최근 1주",
    STATS_DISPLAY_TIME_LAST_MONTH_LABEL : "최근 1개월",
    STATS_DISPLAY_TIME_LAST_YEAR_LABEL : "최근 1년",

    STATS_DISPLAY_CUSTOM_TIME_LAST_SECOND_LABEL : "최근 {0}초",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MINUTE_LABEL : "최근 {0}분",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MINUTE_AND_SECOND_LABEL : "최근 {0}분 {1}초",
    STATS_DISPLAY_CUSTOM_TIME_LAST_HOUR_LABEL : "최근 {0}시간",
    STATS_DISPLAY_CUSTOM_TIME_LAST_HOUR_AND_MINUTE_LABEL : "최근 {0}시간 {1}분",
    STATS_DISPLAY_CUSTOM_TIME_LAST_DAY_LABEL : "최근 {0}일",
    STATS_DISPLAY_CUSTOM_TIME_LAST_DAY_AND_HOUR_LABEL : "최근 {0}일 {1}시간",
    STATS_DISPLAY_CUSTOM_TIME_LAST_WEEK_LABEL : "최근 {0}주",
    STATS_DISPLAY_CUSTOM_TIME_LAST_WEEK_AND_DAY_LABEL : "최근 {0}주 {1}일",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MONTH_LABEL : "최근 {0}개월",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MONTH_AND_DAY_LABEL : "최근 {0}개월 {1}일",
    STATS_DISPLAY_CUSTOM_TIME_LAST_YEAR_LABEL : "최근 {0}년",
    STATS_DISPLAY_CUSTOM_TIME_LAST_YEAR_AND_MONTH_LABEL : "최근 {0}년 {1}개월",

    STATS_LIVE_UPDATE_LABEL: "라이브 업데이트",
    STATS_TIME_SELECTOR_NOW_LABEL: "지금",

    LOG_ANALYTICS_LOG_MESSAGE_TITLE: "로그 메시지",

    AUTOSCALED_APPLICATION : "자동 스케일된 애플리케이션",
    AUTOSCALED_SERVER : "자동 스케일된 서버",
    AUTOSCALED_CLUSTER : "자동 스케일된 클러스터",
    AUTOSCALED_POLICY : "자동 스케일링 정책",
    AUTOSCALED_POLICY_DISABLED : "자동 스케일링 정책이 사용 안함으로 설정됨",
    AUTOSCALED_NOACTIONS : "자동 스케일된 자원에 대해 조치를 사용할 수 없음",

    START : "시작",
    START_CLEAN : "시작 ---정리",
    STARTING : "시작 중",
    STARTED : "시작됨",
    RUNNING : "실행 중",
    NUM_RUNNING: "{0}개 실행 중",
    PARTIALLY_STARTED : "부분적으로 시작됨",
    PARTIALLY_RUNNING : "부분적으로 실행 중",
    NOT_STARTED : "시작되지 않음",
    STOP : "중지",
    STOPPING : "중지 중",
    STOPPED : "중지",
    NUM_STOPPED : "{0}개 중지됨",
    NOT_RUNNING : "실행 중이 아님",
    RESTART : "다시 시작",
    RESTARTING : "다시 시작 중",
    RESTARTED : "다시 시작됨",
    ALERT : "경보",
    ALERTS : "경보",
    UNKNOWN : "알 수 없음",
    NUM_UNKNOWN : "{0}개 알 수 없음",
    SELECT : "선택",
    SELECTED : "선택됨",
    SELECT_ALL : "모두 선택",
    SELECT_NONE : "선택 안함",
    DESELECT: "선택 취소",
    DESELECT_ALL : "모두 선택 취소",
    TOTAL : "총계",
    UTILIZATION : "{0}% 이상의 이용률", // percent

    ELLIPSIS_ARIA: "더 많은 옵션을 보려면 펼치십시오.",
    EXPAND : "펼치기",
    COLLAPSE: "접기",

    ALL : "모두",
    ALL_APPS : "모든 애플리케이션",
    ALL_SERVERS : "모든 서버",
    ALL_CLUSTERS : "모든 클러스터",
    ALL_HOSTS : "모든 호스트",
    ALL_APP_INSTANCES : "모든 애플리케이션 인스턴스",
    ALL_RUNTIMES : "모든 런타임",

    ALL_APPS_RUNNING : "모든 애플리케이션이 실행 중",
    ALL_SERVER_RUNNING : "모든 서버가 실행 중",
    ALL_CLUSTERS_RUNNING : "모든 클러스터가 실행 중",
    ALL_APPS_STOPPED : "모든 애플리케이션이 중지됨",
    ALL_SERVER_STOPPED : "모든 서버가 중지됨",
    ALL_CLUSTERS_STOPPED : "모든 클러스터가 중지됨",
    ALL_SERVERS_UNKNOWN : "모든 서버를 알 수 없음",
    SOME_APPS_RUNNING : "일부 애플리케이션이 실행 중",
    SOME_SERVERS_RUNNING : "일부 서버가 실행 중",
    SOME_CLUSTERS_RUNNING : "일부 클러스터가 실행 중",
    NO_APPS_RUNNING : "실행 중인 애플리케이션이 없음",
    NO_SERVERS_RUNNING : "실행 중인 서버가 없음",
    NO_CLUSTERS_RUNNING : "실행 중인 클러스터가 없음",

    HOST_WITH_ALL_SERVERS_RUNNING: "모든 서버가 실행 중인 호스트", // not used anymore since 4Q
    HOST_WITH_SOME_SERVERS_RUNNING: "일부 서버가 실행 중인 호스트",
    HOST_WITH_NO_SERVERS_RUNNING: "실행 중인 서버가 없는 호스트", // not used anymore since 4Q
    HOST_WITH_ALL_SERVERS_STOPPED: "모든 서버가 중지된 호스트",
    HOST_WITH_SERVERS_RUNNING: "서버가 실행 중인 호스트",

    RUNTIME_WITH_SOME_SERVERS_RUNNING: "일부 서버가 실행 중인 런타임",
    RUNTIME_WITH_ALL_SERVERS_STOPPED: "모든 서버가 중지된 런타임",
    RUNTIME_WITH_SERVERS_RUNNING: "서버가 실행 중인 런타임",

    START_ALL_APPS : "모든 애플리케이션을 시작하시겠습니까?",
    START_ALL_INSTANCES : "모든 애플리케이션 인스턴스를 시작하시겠습니까?",
    START_ALL_SERVERS : "모든 서버를 시작하시겠습니까?",
    START_ALL_CLUSTERS : "모든 클러스터를 시작하시겠습니까?",
    STOP_ALL_APPS : "모든 애플리케이션을 중지하시겠습니까?",
    STOPE_ALL_INSTANCES : "모든 애플리케이션 인스턴스를 중지하시겠습니까?",
    STOP_ALL_SERVERS : "모든 서버를 중지하시겠습니까?",
    STOP_ALL_CLUSTERS : "모든 클러스터를 중지하시겠습니까?",
    RESTART_ALL_APPS : "모든 애플리케이션을 다시 시작하시겠습니까?",
    RESTART_ALL_INSTANCES : "모든 애플리케이션 인스턴스를 다시 시작하시겠습니까?",
    RESTART_ALL_SERVERS : "모든 서버를 다시 시작하시겠습니까?",
    RESTART_ALL_CLUSTERS : "모든 클러스터를 다시 시작하시겠습니까?",

    START_INSTANCE : "애플리케이션 인스턴스를 시작하시겠습니까?",
    STOP_INSTANCE : "애플리케이션 인스턴스를 중지하시겠습니까?",
    RESTART_INSTANCE : "애플리케이션 인스턴스를 다시 시작하시겠습니까?",

    START_SERVER : "{0} 서버를 시작하시겠습니까?",
    STOP_SERVER : "{0} 서버를 중지하시겠습니까?",
    RESTART_SERVER : "{0} 서버를 다시 시작하시겠습니까?",

    START_ALL_INSTS_OF_APP : "{0}의 모든 인스턴스를 시작하시겠습니까?", // application name
    START_APP_ON_SERVER : "{1}에서 {0}을(를) 시작하시겠습니까?", // app name, server name
    START_ALL_APPS_WITHIN : "{0} 내의 모든 애플리케이션을 시작하시겠습니까?", // resource
    START_ALL_APP_INSTS_WITHIN : "{0} 내의 모든 애플리케이션 인스턴스를 시작하시겠습니까?", // resource
    START_ALL_SERVERS_WITHIN : "{0} 내의 모든 서버를 시작하시겠습니까?", // resource
    STOP_ALL_INSTS_OF_APP : "{0}의 모든 인스턴스를 중지하시겠습니까?", // application name
    STOP_APP_ON_SERVER : "{1}에서 {0}을(를) 중지하시겠습니까?", // app name, server name
    STOP_ALL_APPS_WITHIN : "{0} 내의 모든 애플리케이션을 중지하시겠습니까?", // resource
    STOP_ALL_APP_INSTS_WITHIN : "{0} 내의 모든 애플리케이션 인스턴스를 중지하시겠습니까?", // resource
    STOP_ALL_SERVERS_WITHIN : "{0} 내의 모든 서버를 중지하시겠습니까?", // resource
    RESTART_ALL_INSTS_OF_APP : "{0}의 모든 인스턴스를 다시 시작하시겠습니까?", // application name
    RESTART_APP_ON_SERVER : "{1}에서 {0}을(를) 다시 시작하시겠습니까?", // app name, server name
    RESTART_ALL_APPS_WITHIN : "{0} 내의 모든 애플리케이션을 다시 시작하시겠습니까?", // resource
    RESTART_ALL_APP_INSTS_WITHIN : "{0} 내의 모든 애플리케이션 인스턴스를 다시 시작하시겠습니까?", // resource
    RESTART_ALL_SERVERS_WITHIN : "{0} 내의 실행 중인 모든 서버를 다시 시작하시겠습니까?", // resource

    START_SELECTED_APPS : "선택된 애플리케이션의 모든 인스턴스를 시작하시겠습니까?",
    START_SELECTED_INSTANCES : "선택된 애플리케이션 인스턴스를 시작하시겠습니까?",
    START_SELECTED_SERVERS : "선택된 서버를 시작하시겠습니까?",
    START_SELECTED_SERVERS_LABEL : "선택된 서버 시작",
    START_SELECTED_CLUSTERS : "선택된 클러스터를 시작하시겠습니까?",
    START_CLEAN_SELECTED_SERVERS : "선택된 서버를 시작 --정리하시겠습니까?",
    START_CLEAN_SELECTED_CLUSTERS : "선택된 클러스터를 시작 --정리하시겠습니까?",
    STOP_SELECTED_APPS : "선택된 애플리케이션의 모든 인스턴스를 중지하시겠습니까?",
    STOP_SELECTED_INSTANCES : "선택된 애플리케이션 인스턴스를 중지하시겠습니까?",
    STOP_SELECTED_SERVERS : "선택된 서버를 중지하시겠습니까?",
    STOP_SELECTED_CLUSTERS : "선택된 클러스터를 중지하시겠습니까?",
    RESTART_SELECTED_APPS : "선택된 애플리케이션의 모든 인스턴스를 다시 시작하시겠습니까?",
    RESTART_SELECTED_INSTANCES : "선택된 애플리케이션 인스턴스를 다시 시작하시겠습니까?",
    RESTART_SELECTED_SERVERS : "선택된 서버를 다시 시작하시겠습니까?",
    RESTART_SELECTED_CLUSTERS : "선택된 클러스터를 다시 시작하시겠습니까?",

    START_SERVERS_ON_HOSTS : "선택된 호스트의 모든 서버를 시작하시겠습니까?",
    STOP_SERVERS_ON_HOSTS : "선택된 호스트의 모든 서버를 중지하시겠습니까?",
    RESTART_SERVERS_ON_HOSTS : "선택된 호스트에서 실행 중인 모든 서버를 다시 시작하시겠습니까?",

    SELECT_APPS_TO_START : "시작할 중지된 애플리케이션을 선택하십시오.",
    SELECT_APPS_TO_STOP : "중지할 시작된 애플리케이션을 선택하십시오.",
    SELECT_APPS_TO_RESTART : "다시 시작할 시작된 애플리케이션을 선택하십시오.",
    SELECT_INSTANCES_TO_START : "시작할 중지된 애플리케이션 인스턴스를 선택하십시오.",
    SELECT_INSTANCES_TO_STOP : "중지할 시작된 애플리케이션 인스턴스를 선택하십시오.",
    SELECT_INSTANCES_TO_RESTART : "다시 시작할 시작된 애플리케이션 인스턴스를 선택하십시오.",
    SELECT_SERVERS_TO_START : "시작할 중지된 서버를 선택하십시오.",
    SELECT_SERVERS_TO_STOP : "중지할 시작된 서버를 선택하십시오.",
    SELECT_SERVERS_TO_RESTART : "다시 시작할 시작된 서버를 선택하십시오.",
    SELECT_CLUSTERS_TO_START : "시작할 중지된 클러스터를 선택하십시오.",
    SELECT_CLUSTERS_TO_STOP : "중지할 시작된 클러스터를 선택하십시오.",
    SELECT_CLUSTERS_TO_RESTART : "다시 시작할 시작된 클러스터를 선택하십시오.",

    STATUS : "상태",
    STATE : "상태:",
    NAME : "이름:",
    DIRECTORY : "디렉토리",
    INFORMATION : "정보",
    DETAILS : "세부사항",
    ACTIONS : "조치",
    CLOSE : "닫기",
    HIDE : "숨기기",
    SHOW_ACTIONS : "조치 표시",
    SHOW_SERVER_ACTIONS_LABEL : "서버 {0} 조치",
    SHOW_APP_ACTIONS_LABEL : "애플리케이션 {0} 조치",
    SHOW_CLUSTER_ACTIONS_LABEL : "클러스터 {0} 조치",
    SHOW_HOST_ACTIONS_LABEL : "호스트 {0} 조치",
    SHOW_RUNTIME_ACTIONS_LABEL : "런타임 {0} 조치",
    SHOW_SERVER_ACTIONS_MENU_LABEL : "서버 {0} 조치 메뉴",
    SHOW_APP_ACTIONS_MENU_LABEL : "애플리케이션 {0} 조치 메뉴",
    SHOW_CLUSTER_ACTIONS_MENU_LABEL : "클러스터 {0} 조치 메뉴",
    SHOW_HOST_ACTIONS_MENU_LABEL : "호스트 {0} 조치 메뉴",
    SHOW_RUNTIME_ACTIONS_MENU_LABEL : "런타임 {0} 조치 메뉴",
    SHOW_RUNTIMEONHOST_ACTIONS_MENU_LABEL : "호스트의 런타임 {0} 조치 메뉴",
    SHOW_COLLECTION_MENU_LABEL : "콜렉션 {0} 상태 조치 메뉴",  // menu object id
    SHOW_SEARCH_MENU_LABEL : "검색 {0} 상태 조치 메뉴",  // menu object id


    // A bit odd to have sentence casing without punctuation?
    UNKNOWN_STATE : "{0}: 알 수 없는 상태", // resourceName
    UNKNOWN_STATE_APPS : "{0}개 애플리케이션이 알 수 없는 상태에 있음", // quantity
    UNKNOWN_STATE_APP_INSTANCES : "{0}개 인스턴스가 알 수 없는 상태에 있음", // quantity
    UNKNOWN_STATE_SERVERS : "{0}개 서버가 알 수 없는 상태에 있음", // quantity
    UNKNOWN_STATE_CLUSTERS : "{0}개 클러스터가 알 수 없는 상태에 있음", // quantity

    INSTANCES_NOT_RUNNING : "{0}개 애플리케이션 인스턴스가 실행 중이 아님", // quantity
    APPS_NOT_RUNNING : "{0}개 애플리케이션이 실행 중이 아님", // quantity
    SERVERS_NOT_RUNNING : "{0}개 서버가 실행 중이 아님", // quantity
    CLUSTERS_NOT_RUNNING : "{0}개 클러스터가 실행 중이 아님", // quantity

    APP_STOPPED_ON_SERVER : "실행 중인 서버 {1}에서 {0}이(가) 중지됨", // appName, serverName(s)
    APPS_STOPPED_ON_SERVERS : "실행 중인 서버 {1}에서 {0}개 애플리케이션이 중지됨", // quantity, serverName(s)
    APPS_STOPPED_ON_SERVER : "실행 중인 서버에서 {0}개 애플리케이션이 중지됨", // quantity
    NUMBER_RESOURCES : "{0}개 자원", // quantity
    NUMBER_APPS : "{0}개 애플리케이션", // quantity
    NUMBER_SERVERS : "{0}개 서버", // quantity
    NUMBER_CLUSTERS : "{0}개 클러스터", // quantity
    NUMBER_HOSTS : "{0}개 호스트", // quantity
    NUMBER_RUNTIMES : "{0}개 런타임", // quantity
    SERVERS_INSERT : "서버",
    INSERT_STOPPED_ON_INSERT : "실행 중인 {1}에서 {0}개가 중지됨", // NUMBER_APPS, SERVERS_INSERT

    APPSERVER_STOPPED_ON_SERVER : "{0}이(가) 실행 중인 서버 {1}에서 중지됨", //appName, serverName
    APPCLUSTER_STOPPED_ON_SERVER : "클러스터 {1}의 {0}이(가) 실행 중인 서버 {2}에서 중지됨 ",  //appName, clusterName, serverName(s)

    INSTANCES_STOPPED_ON_SERVERS : "실행 중인 서버에서 {0}개 애플리케이션 인스턴스가 중지됨", // quantity
    INSTANCE_STOPPED_ON_SERVERS : "{0}: 애플리케이션 인스턴스가 실행 중이 아님", // serverNames

    NOT_ALL_APPS_RUNNING : "{0}: 일부 애플리케이션이 실행 중이지 않음", // serverName[]
    NO_APPS_RUNNING : "{0}: 실행 중인 애플리케이션이 없음", // serverName[]
    NOT_ALL_APPS_RUNNING_SERVERS : "일부 애플리케이션이 실행 중이지 않은 {0}개 서버", // quantity
    NO_APPS_RUNNING_SERVERS : "실행 중인 애플리케이션이 없는 {0}개 서버", // quantity

    COUNT_OF_APPS_SELECTED : "{0}개 애플리케이션이 선택됨",
    RATIO_RUNNING : "{0} 실행 중", // ratio ex. 1/2

    RESOURCES_SELECTED : "{0}개 선택됨",

    NO_HOSTS_SELECTED : "호스트가 선택되지 않음",
    NO_DEPLOY_RESOURCE : "설치를 배치할 자원이 없음",
    NO_TOPOLOGY : "{0}이(가) 없습니다.",
    COUNT_OF_APPS_STARTED  : "{0}개 애플리케이션이 시작됨",

    APPS_LIST : "{0}개 애플리케이션",
    EMPTY_MESSAGE : "{0}",  // used to build a list of comma separated resource names
    APPS_INSTANCES_RUNNING_OF_TOTAL : "{0}/{1}개 인스턴스 실행 중",
    HOSTS_SERVERS_RUNNING_OF_TOTAL : "{0}/{1}개 서버 실행 중",
    RESOURCE_ON_RESOURCE : "{1}의 {0}", // resource name, resource name
    RESOURCE_ON_SERVER_RESOURCE : "서버 {1}의 {0}", // resource name, resource name
    RESOURCE_ON_CLUSTER_RESOURCE : "클러스터 {1}의 {0}", // resource name, resource name

    RESTART_DISABLED_FOR_ADMIN_CENTER: "이 서버가 관리 센터를 호스팅하는 중이므로 이 서버에 대한 다시 시작이 비활성화되어 있습니다.",
    ACTION_DISABLED_FOR_USER: "사용자에게 권한이 없으므로 이 자원에 대해 조치를 사용할 수 없습니다.",

    RESTART_AC_TITLE: "관리 센터를 다시 시작하지 않음",
    RESTART_AC_DESCRIPTION: "{0}이(가) 관리 센터를 제공합니다. 관리 센터는 자체적으로 다시 시작되지 않습니다.",
    RESTART_AC_MESSAGE: "선택된 다른 모든 서버는 다시 시작됩니다.",
    RESTART_AC_CLUSTER_MESSAGE: "선택된 다른 모든 클러스터가 다시 시작됩니다.",

    STOP_AC_TITLE: "관리 센터 중지",
    STOP_AC_DESCRIPTION: "{0} 서버는 관리 센터를 실행하는 집합체 제어기입니다. 제어기를 중지하면 Liberty 집합체 관리 조작에 영향을 미칠 수 있으며 관리 센터를 사용하지 못할 수 있습니다.",
    STOP_AC_MESSAGE: "이 제어기를 중지하시겠습니까?",
    STOP_STANDALONE_DESCRIPTION: "{0} 서버가 관리 센터를 실행합니다. 이 서버를 중지하면 관리 서버를 사용할 수 없게 됩니다.",
    STOP_STANDALONE_MESSAGE: "이 서버를 중지하시겠습니까?",

    STOP_CONTROLLER_TITLE: "제어기 중지",
    STOP_CONTROLLER_DESCRIPTION: "{0} 서버는 집합체 제어기입니다. 이 서버를 중지하면 Liberty 집합체 조작에 영향을 미칠 수 있습니다.",
    STOP_CONTROLLER_MESSAGE: "이 제어기를 중지하시겠습니까?",

    STOP_AC_CLUSTER_TITLE: "{0} 클러스터 중지",
    STOP_AC_CLUSTER_DESCRIPTION: "{0} 클러스터에는 관리 센터를 실행하는 집합체 제어기가 포함되어 있습니다. 이 클러스터를 중지하면 Liberty 집합체 관리 조작에 영향을 미치고 관리 서버를 사용하지 못할 수 있습니다.",
    STOP_AC_CLUSTER_MESSAGE: "이 클러스터를 중지하시겠습니까?",

    INVALID_URL: "페이지가 존재하지 않습니다.",
    INVALID_APPLICATION: "{0} 애플리케이션이 더 이상 집합체에 없습니다.", // application name
    INVALID_SERVER: "{0} 서버가 더 이상 집합체에 없습니다.", // server name
    INVALID_CLUSTER: "{0} 클러스터가 더 이상 집합체에 없습니다.", // cluster name
    INVALID_HOST: "{0} 호스트가 더 이상 집합체에 없습니다.", // host name
    INVALID_RUNTIME: "{0} 런타임이 더 이상 집합체에 없습니다.", // runtime name
    INVALID_INSTANCE: "애플리케이션 인스턴스 {0}이(가) 더 이상 집합체에 없습니다.", // application instance name
    GO_TO_DASHBOARD: "대시보드로 이동",
    VIEWED_RESOURCE_REMOVED: "잠깐! 자원이 제거되었거나 더 이상 사용 가능하지 않습니다.",

    OK_DEFAULT_BUTTON: "확인",
    CONNECTION_FAILED_MESSAGE: "서버에 대한 연결이 유실되었습니다. 환경에 대한 동적 변경사항이 더 이상 페이지에 표시되지 않습니다. 페이지를 새로 고쳐서 연결 및 동적 업데이트를 복원하십시오.",
    ERROR_MESSAGE: "연결 인터럽트됨",

    // Used by standalone stop message dialog
    STANDALONE_STOP_TITLE : '서버 중지',

    // Tags
    RELATED_RESOURCES: "관련 자원",
    TAGS : "태그",
    TAG_BUTTON_LABEL : "태그 {0}",  // tag value
    TAGS_LABEL : "쉼표, 공백, Enter 또는 Tab으로 구분된 태그를 입력하십시오.",
    OWNER : "소유자",
    OWNER_BUTTON_LABEL : "소유자 {0}",  // owner value
    CONTACTS : "문의",
    CONTACT_BUTTON_LABEL : "문의처 {0}",  // contact value
    PORTS : "포트",
    CONTEXT_ROOT : "컨텍스트 루트",
    HTTP : "HTTP",
    HTTPS : "HTTPS",
    MORE : "계속",  // alt text for the ... button
    MORE_BUTTON_MENU : "{0} 메뉴 계속", // alt text for the menu
    NOTES: "참고",
    NOTE_LABEL : "참고 {0}",  // note value
    SET_ATTRIBUTES: "태그 및 메타데이터",
    SETATTR_RUNTIME_NAME: "{1}의 {0}",  // runtime, host
    SAVE: "저장",
    TAGINVALIDCHARS: "'/', '<' 및 '>' 문자는 올바르지 않습니다.",
    ERROR_GET_TAGS_METADATA: "제품이 자원에 대한 현재 태그 및 메타데이터를 가져올 수 없습니다.",
    ERROR_SET_TAGS_METADATA: "오류로 인해 제품이 태그 및 메타데이터를 설정하지 못했습니다.",
    METADATA_WILLBE_INHERITED: "메타데이터는 애플리케이션에서 설정되고 클러스터에 있는 모든 인스턴스에서 공유됩니다.",
    ERROR_ALT: "오류",

    // Graph Warning Messages
    GRAPH_SERVER_NOT_STARTED: "이 서버가 중지되었기 때문에 이 서버에 대한 현재 통계를 사용할 수 없습니다. 해당 서버를 시작하여 모니터링을 시작하십시오.",
    GRAPH_SERVER_HOSTING_APP_NOT_STARTED: "연관된 서버가 중지되었기 때문에 이 애플리케이션에 대한 현재 통계를 사용할 수 없습니다. 해당 서버를 시작하여 이 애플리케이션 모니터링을 시작하십시오.",
    GRAPH_FEATURES_NOT_CONFIGURED: "아직 항목이 없습니다! 편집 아이콘을 선택한 후 메트릭을 추가하여 이 자원을 모니터하십시오.",
    NO_GRAPHS_AVAILABLE: "추가할 수 있는 메트릭이 없습니다. 추가 모니터링 기능을 설치하여 더 많은 메트릭을 사용할 수 있게 하십시오. ",
    NO_APPS_GRAPHS_AVAILABLE: "추가할 수 있는 메트릭이 없습니다. 추가 모니터링 기능을 설치하여 더 많은 메트릭을 사용할 수 있게 하십시오. 애플리케이션이 사용 중인지도 확인하십시오.",
    GRAPH_CONFIG_NOT_SAVED_TITLE : "저장되지 않은 변경사항",
    GRAPH_CONFIG_NOT_SAVED_DESCR : "저장되지 않은 변경사항이 있습니다. 다른 페이지로 이동하면 해당 변경사항이 유실됩니다.",
    GRAPH_CONFIG_NOT_SAVED_MSG : "변경사항을 저장하시겠습니까?",

    NO_CPU_STATS_AVAILABLE : "이 서버에 대한 CPU 사용량 통계를 사용할 수 없습니다.",

    // Server Config
    CONFIG_NOT_AVAILABLE: "이 보기를 사용하려면 서버 구성 도구를 설치하십시오.",
    SAVE_BEFORE_CLOSING_DIALOG_MESSAGE: "닫기 전에 {0}에 변경사항을 저장하시겠습니까?",
    SAVE: "저장",
    DONT_SAVE: "저장 안함",

    // Maintenance mode
    ENABLE_MAINTENANCE_MODE: "유지보수 모드 사용",
    DISABLE_MAINTENANCE_MODE: "유지보수 모드 사용 안함",
    ENABLE_MAINTENANCE_MODE_DIALOG_TITLE: "유지보수 모드 사용",
    DISABLE_MAINTENANCE_MODE_DIALOG_TITLE: "유지보수 모드 사용 안함",
    ENABLE_MAINTENANCE_MODE_HOST_DESCRIPTION: "호스트 및 모든 해당 서버({0}개 서버)에서 유지보수 모드 사용",
    ENABLE_MAINTENANCE_MODE_HOSTS_DESCRIPTION: "호스트 및 모든 해당 서버({0}개 서버)에서 유지보수 모드 사용",
    ENABLE_MAINTENANCE_MODE_SERVER_DESCRIPTION: "서버에서 유지보수 모드 사용",
    ENABLE_MAINTENANCE_MODE_SERVERS_DESCRIPTION: "서버에서 유지보수 모드 사용",
    DISABLE_MAINTENANCE_MODE_HOST_DESCRIPTION: "호스트 및 모든 해당 서버({0}개 서버)에서 유지보수 모드 사용 안함",
    DISABLE_MAINTENANCE_MODE_SERVER_DESCRIPTION: "서버에서 유지보수 모드 사용 안함",
    BREAK_AFFINITY_LABEL: "활성 세션과의 연관관계 중단",
    ENABLE_MAINTENANCE_MODE_DIALOG_ENABLE_BUTTON_LABEL: "사용 가능",
    DISABLE_MAINTENANCE_MODE_DIALOG_ENABLE_BUTTON_LABEL: "사용 안함",
    MAINTENANCE_MODE: "유지보수 모드",
    ENABLING_MAINTENANCE_MODE: "유지보수 모드 사용",
    MAINTENANCE_MODE_ENABLED: "유지보수 모드 사용 설정됨",
    MAINTENANCE_MODE_ALTERNATE_SERVERS_NOT_STARTED: "대체 서버가 시작되지 않아 유지보수 모드가 사용으로 설정되지 않았습니다.",
    MAINTENANCE_MODE_SELECT_FORCE_MESSAGE: "대체 서버를 시작하지 않고 유지보수 모드를 사용하려면 강제 실행을 선택하십시오. 강제 실행을 수행하면 자동 스케일링 정책이 중단될 수 있습니다.",
    MAINTENANCE_MODE_FAILED: "유지보수 모드를 사용으로 설정할 수 없습니다.",
    MAINTENANCE_MODE_FORCE_LABEL: "강제 실행",
    MAINTENANCE_MODE_CANCEL_LABEL: "취소",
    MAINTENANCE_MODE_SERVERS_IN_MAINTENANCE_MODE: "{0}개 서버가 현재 유지보수 모드에 있습니다.",
    MAINTENANCE_MODE_ENABLING_MAINTENANCE_MODE_ON_ALL_HOST_SERVERS_EQUAL_OR_LESS_THEN_10: "모든 호스트 서버에서 유지보수 모드를 사용으로 설정합니다.",
    MAINTENANCE_MODE_ENABLING_MAINTENANCE_MODE_ON_ALL_HOST_SERVERS_MORE_THAN_10: "모든 호스트 서버에서 유지보수 모드를 사용으로 설정합니다. 상태에 대한 서버 보기를 표시합니다.",

    SERVER_API_DOCMENTATION: "서버 API 정의 보기",

    // objectView title
    TITLE_FOR_CLUSTER: "클러스터 {0}", // cluster name
    TITLE_FOR_HOST: "호스트 {0}", // host name

    // objectView descriptor
    COLLECTIVE_CONTROLLER_DESCRIPTOR: "집합체 제어기",
    LIBERTY_SERVER : "Liberty 서버",
    NODEJS_SERVER : "Node.js 서버",
    CONTAINER : "컨테이너",
    CONTAINER_LIBERTY : "Liberty",
    CONTAINER_DOCKER : "Docker",
    CONTAINER_NODEJS : "Node.js",
    LIBERTY_IN_DOCKER_DESCRIPTOR : "Docker 컨테이너의 Liberty 서버",
    NODEJS_IN_DOCKER_DESCRIPTOR : "Docker 컨테이너의 Node.js 서버",
    RUNTIME_LIBERTY : "Liberty 런타임",
    RUNTIME_NODEJS : "Node.js 런타임",
    RUNTIME_DOCKER : "Docker 컨테이너의 런타임"

});
