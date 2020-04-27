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
    EXPLORER : "Explorer",
    EXPLORE : "Explore",
    DASHBOARD : "ダッシュボード",
    DASHBOARD_VIEW_ALL_APPS : "すべてのアプリケーションの表示",
    DASHBOARD_VIEW_ALL_SERVERS : "すべてのサーバーの表示",
    DASHBOARD_VIEW_ALL_CLUSTERS : "すべてのクラスターの表示",
    DASHBOARD_VIEW_ALL_HOSTS : "すべてのホストの表示",
    DASHBOARD_VIEW_ALL_RUNTIMES : "すべてのランタイムの表示",
    SEARCH : "検索",
    SEARCH_RECENT : "最近の検索",
    SEARCH_RESOURCES : "検索リソース",
    SEARCH_RESULTS : "検索結果",
    SEARCH_NO_RESULTS : "結果はありません",
    SEARCH_NO_MATCHES : "一致はありません",
    SEARCH_TEXT_INVALID : "検索テキストに無効文字が含まれています",
    SEARCH_CRITERIA_INVALID : "検索基準が無効です。",
    SEARCH_CRITERIA_INVALID_COMBO :"{0} は、{1} と一緒に指定された場合無効です。",
    SEARCH_CRITERIA_INVALID_DUPLICATE : "{0} は一度だけ指定してください。",
    SEARCH_TEXT_MISSING : "検索テキストが必要です",
    SEARCH_UNSUPPORT_TYPE_APPONSERVER : "サーバーでのアプリケーション・タグの検索はサポートされていません。",
    SEARCH_UNSUPPORT_TYPE_APPONCLUSTER : "クラスターでのアプリケーション・タグの検索はサポートされていません。",
    SEARCH_UNSUPPORT : "検索基準はサポートされていません。",
    SEARCH_SWITCH_VIEW : "表示の切り替え",
    FILTERS : "フィルター",
    DEPLOY_SERVER_PACKAGE : "サーバー・パッケージのデプロイ",
    MEMBER_OF : "メンバーの所属先",
    N_CLUSTERS: "{0} クラスター ...",

    INSTANCE : "インスタンス",
    INSTANCES : "インスタンス",
    APPLICATION : "アプリケーション",
    APPLICATIONS : "アプリケーション",
    SERVER : "サーバー",
    SERVERS : "サーバー",
    CLUSTER : "クラスター",
    CLUSTERS : "クラスター",
    CLUSTER_NAME : "クラスター名: ",
    CLUSTER_STATUS : "クラスター状況: ",
    APPLICATION_NAME : "アプリケーション名: ",
    APPLICATION_STATE : "アプリケーション状態: ",
    HOST : "ホスト",
    HOSTS : "ホスト",
    RUNTIME : "ランタイム",
    RUNTIMES : "ランタイム",
    PATH : "パス",
    CONTROLLER : "コントローラー",
    CONTROLLERS : "コントローラー",
    OVERVIEW : "概説",
    CONFIGURE : "構成",

    SEARCH_RESOURCE_TYPE: "タイプ", // Search by resource types
    SEARCH_RESOURCE_STATE: "都道府県", // Search by resource types
    SEARCH_RESOURCE_TYPE_ALL: "すべて", // Search all resource types
    SEARCH_RESOURCE_NAME: "名前", // Search by resource name
    SEARCH_RESOURCE_TAG: "タグ", // Search by resource tag
    SEARCH_RESOURCE_CONTAINER: "コンテナー", // Search by container type
    SEARCH_RESOURCE_CONTAINER_DOCKER: "Docker", // Search by container type Docker
    SEARCH_RESOURCE_CONTAINER_NONE: "なし", // Search by container type none
    SEARCH_RESOURCE_RUNTIMETYPE: "ランタイム・タイプ", // Search by runtime type
    SEARCH_RESOURCE_OWNER: "所有者", // Search by owner
    SEARCH_RESOURCE_CONTACT: "連絡先", // Search by contact
    SEARCH_RESOURCE_NOTE: "注記", // Search by note

    GRID_HEADER_USERDIR : "ユーザー・ディレクトリー",
    GRID_HEADER_NAME : "名前",
    GRID_LOCATION_NAME : "ロケーション",
    GRID_ACTIONS : "グリッド・アクション",
    GRID_ACTIONS_LABEL : "{0} グリッド・アクション",  // name of the grid
    APPONSERVER_LOCATION_NAME : "{1} 上の {0} ({2})", // server on host (/path)

    STATS : "モニター",
    STATS_ALL : "すべて",
    STATS_VALUE : "値: {0}",
    CONNECTION_IN_USE_STATS : "使用中 {0} = 管理対象 {1} - 空き {2}",
    CONNECTION_IN_USE_STATS_VALUE : "値: {0} 使用中 = {1} 管理 - {2} 空き",
    DATA_SOURCE : "データ・ソース: {0}",
    STATS_DISPLAY_LEGEND : "凡例の表示",
    STATS_HIDE_LEGEND : "凡例の非表示",
    STATS_VIEW_DATA : "グラフ・データの表示",
    STATS_VIEW_DATA_TIMESTAMP : "タイム・スタンプ",
    STATS_ACTION_MENU : "{0} アクション・メニュー",
    STATS_SHOW_HIDE : "リソース・メトリックの追加",
    STATS_SHOW_HIDE_SUMMARY : "要約用メトリックの追加",
    STATS_SHOW_HIDE_TRAFFIC : "トラフィック用メトリックの追加",
    STATS_SHOW_HIDE_PERFORMANCE : "パフォーマンス用メトリックの追加",
    STATS_SHOW_HIDE_AVAILABILITY : "可用性用メトリックの追加",
    STATS_SHOW_HIDE_ALERT : "アラート用メトリックの追加",
    STATS_SHOW_HIDE_LIST_BUTTON : "リソース・メトリック・リストを表示または非表示にする",
    STATS_SHOW_HIDE_BUTTON_TITLE : "グラフの編集",
    STATS_SHOW_HIDE_CONFIRM : "保存(S)",
    STATS_SHOW_HIDE_CANCEL : "キャンセル",
    STATS_SHOW_HIDE_DONE : "完了",
    STATS_DELETE_GRAPH : "グラフの削除",
    STATS_ADD_CHART_LABEL : "グラフを追加して表示する",
    STATS_JVM_TITLE : "JVM",
    STATS_JVM_BUTTON_LABEL : "JVM のすべてのグラフを追加して表示する",
    STATS_HEAP_TITLE : "使用ヒープ・メモリー",
    STATS_HEAP_USED : "使用量: {0} MB",
    STATS_HEAP_COMMITTED : "コミット済み: {0} MB",
    STATS_HEAP_MAX : "最大: {0} MB",
    STATS_HEAP_X_TIME : "時間",
    STATS_HEAP_Y_MB : "使用中 MB",
    STATS_HEAP_Y_MB_LABEL : "{0} MB",
    STATS_CLASSES_TITLE : "ロードされたクラス",
    STATS_CLASSES_LOADED : "ロード済み: {0}",
    STATS_CLASSES_UNLOADED : "アンロード済み: {0}",
    STATS_CLASSES_TOTAL : "合計: {0}",
    STATS_CLASSES_Y_TOTAL : "ロードされたクラス",
    STATS_PROCESSCPU_TITLE : "CPU 使用率",
    STATS_PROCESSCPU_USAGE : "CPU 使用率: {0}%",
    STATS_PROCESSCPU_Y_PERCENT : "CPU 率",
    STATS_PROCESSCPU_Y_PCT_LABEL : "{0}%",
    STATS_THREADS_TITLE : "アクティブ JVM スレッド",
    STATS_LIVE_MSG_INIT : "ライブ・データを表示しています",
    STATS_LIVE_MSG :"このグラフには履歴データがありません。 直近の 10 分間のデータを表示します。",
    STATS_THREADS_ACTIVE : "ライブ: {0}",
    STATS_THREADS_PEAK : "ピーク: {0}",
    STATS_THREADS_TOTAL : "合計: {0}",
    STATS_THREADS_Y_THREADS : "スレッド",
    STATS_TP_POOL_SIZE : "プール・サイズ",
    STATS_JAXWS_TITLE : "JAX-WS Web サービス",
    STATS_JAXWS_BUTTON_LABEL : "JAX-WS Web サービスのすべてのグラフを追加して表示する",
    STATS_JW_AVG_RESP_TIME : "平均応答時間",
    STATS_JW_AVG_INVCOUNT : "平均呼び出しカウント",
    STATS_JW_TOTAL_FAULTS : "ランタイム・フォールトの合計",
    STATS_LA_RESOURCE_CONFIG_LABEL : "リソースの選択...",
    STATS_LA_RESOURCE_CONFIG_LABEL_NUM : "{0} リソース",
    STATS_LA_RESOURCE_CONFIG_LABEL_NUM_1 : "1 リソース",
    STATS_LA_RESOURCE_CONFIG_SELECT_ONE : "少なくとも 1 つのリソースを選択する必要があります。",
    STATS_LA_RESOURCE_CONFIG_NO_DATA : "選択された時刻範囲にデータはありません。",
    STATS_ACCESS_LOG_TITLE : "アクセス・ログ",
    STATS_ACCESS_LOG_BUTTON_LABEL : "アクセス・ログのすべてのグラフを追加して表示する",
    STATS_ACCESS_LOG_GRAPH : "アクセス・ログ・メッセージ・カウント",
    STATS_ACCESS_LOG_SUMMARY : "アクセス・ログ・サマリー",
    STATS_ACCESS_LOG_TABLE : "アクセス・ログ・メッセージ・リスト",
    STATS_MESSAGES_TITLE : "メッセージおよびトレース",
    STATS_MESSAGES_BUTTON_LABEL : "メッセージおよびトレースのすべてのグラフを追加して表示する",
    STATS_MESSAGES_GRAPH : "ログ・メッセージ・カウント",
    STATS_MESSAGES_TABLE : "ログ・メッセージ・リスト",
    STATS_FFDC_GRAPH : "FFDC カウント",
    STATS_FFDC_TABLE : "FFDC リスト",
    STATS_TRACE_LOG_GRAPH : "トレース・メッセージ・カウント",
    STATS_TRACE_LOG_TABLE : "トレース・メッセージ・リスト",
    STATS_THREAD_POOL_TITLE : "スレッド・プール",
    STATS_THREAD_POOL_BUTTON_LABEL : "スレッド・プールのすべてのグラフを追加して表示する",
    STATS_THREADPOOL_TITLE : "アクティブ Liberty スレッド",
    STATS_THREADPOOL_SIZE : "プール・サイズ: {0}",
    STATS_THREADPOOL_ACTIVE : "アクティブ: {0}",
    STATS_THREADPOOL_TOTAL : "合計: {0}",
    STATS_THREADPOOL_Y_ACTIVE : "アクティブ・スレッド",
    STATS_SESSION_MGMT_TITLE : "セッション",
    STATS_SESSION_MGMT_BUTTON_LABEL : "セッションのすべてのグラフを追加して表示する",
    STATS_SESSION_CONFIG_LABEL : "セッションの選択...",
    STATS_SESSION_CONFIG_LABEL_NUM  : "{0} セッション",
    STATS_SESSION_CONFIG_LABEL_NUM_1  : "1 セッション",
    STATS_SESSION_CONFIG_SELECT_ONE : "少なくとも 1 つのセッションを選択する必要があります。",
    STATS_SESSION_TITLE : "アクティブ・セッション",
    STATS_SESSION_Y_ACTIVE : "アクティブ・セッション",
    STATS_SESSION_LIVE_LABEL : "ライブ・カウント: {0}",
    STATS_SESSION_CREATE_LABEL : "作成カウント: {0}",
    STATS_SESSION_INV_LABEL : "無効化されたカウント: {0}",
    STATS_SESSION_INV_TIME_LABEL : "タイムアウトにより無効化されたカウント: {0}",
    STATS_WEBCONTAINER_TITLE : "Web アプリケーション",
    STATS_WEBCONTAINER_BUTTON_LABEL : "Web アプリケーションのすべてのグラフを追加して表示する",
    STATS_SERVLET_CONFIG_LABEL : "サーブレットの選択...",
    STATS_SERVLET_CONFIG_LABEL_NUM : "{0} サーブレット",
    STATS_SERVLET_CONFIG_LABEL_NUM_1 : "1 サーブレット",
    STATS_SERVLET_CONFIG_SELECT_ONE : "少なくとも 1 つのサーブレットを選択する必要があります。",
    STATS_SERVLET_REQUEST_COUNT_TITLE : "要求カウント",
    STATS_SERVLET_REQUEST_COUNT_Y_AXIS : "要求カウント",
    STATS_SERVLET_RESPONSE_COUNT_TITLE : "応答カウント",
    STATS_SERVLET_RESPONSE_COUNT_Y_AXIS : "応答カウント",
    STATS_SERVLET_RESPONSE_MEAN_TITLE : "平均応答時間 (ns)",
    STATS_SERVLET_RESPONSE_MEAN_Y_AXIS : "応答時間 (ns)",
    STATS_CONN_POOL_TITLE : "接続プール",
    STATS_CONN_POOL_BUTTON_LABEL : "接続プールのすべてのグラフを追加して表示する",
    STATS_CONN_POOL_CONFIG_LABEL : "データ・ソースの選択...",
    STATS_CONN_POOL_CONFIG_LABEL_NUM : "{0} データ・ソース",
    STATS_CONN_POOL_CONFIG_LABEL_NUM_1 : "1 データ・ソース",
    STATS_CONN_POOL_CONFIG_SELECT_ONE : "少なくとも 1 つのデータ・ソースを選択する必要があります。",
    STATS_CONNECT_IN_USE_TITLE : "使用中の接続",
    STATS_CONNECT_USED_COUNT_Y_AXIS : "接続",
    STATS_CONNECT_IN_USE_LABEL : "使用中: {0}",
    STATS_CONNECT_USED_USED_LABEL : "使用: {0}",
    STATS_CONNECT_USED_FREE_LABEL : "空き: {0}",
    STATS_CONNECT_USED_CREATE_LABEL : "作成: {0}",
    STATS_CONNECT_USED_DESTROY_LABEL : "破棄: {0}",
    STATS_CONNECT_WAIT_TIME_TITLE : "平均待ち時間 (ms)",
    STATS_CONNECT_WAIT_TIME_Y_AXIS : "待機時間 (ms)",
    STATS_TIME_ALL : "すべて",
    STATS_TIME_1YEAR : "1 年",
    STATS_TIME_1MONTH : "1 月",
    STATS_TIME_1WEEK : "1 週",
    STATS_TIME_1DAY : "1 日",
    STATS_TIME_1HOUR : "1 時間",
    STATS_TIME_10MINUTES : "10 分",
    STATS_TIME_5MINUTES : "5 分",
    STATS_TIME_1MINUTE : "1 分",
    STATS_PERSPECTIVE_SUMMARY : "要約",
    STATS_PERSPECTIVE_TRAFFIC : "トラフィック",
    STATS_PERSPECTIVE_TRAFFIC_JVM : "JVM トラフィック",
    STATS_PERSPECTIVE_TRAFFIC_CONN : "接続トラフィック",
    STATS_PERSPECTIVE_TRAFFIC_LAACCESS : "アクセス・ログ・トラフィック",
    STATS_PERSPECTIVE_PROBLEM : "問題",
    STATS_PERSPECTIVE_PERFORMANCE : "パフォーマンス",
    STATS_PERSPECTIVE_PERFORMANCE_JVM : "JVM パフォーマンス",
    STATS_PERSPECTIVE_PERFORMANCE_CONN : "接続パフォーマンス",
    STATS_PERSPECTIVE_ALERT : "アラート分析",
    STATS_PERSPECTIVE_ALERT_LAACCESS : "アクセス・ログ・アラート",
    STATS_PERSPECTIVE_ALERT_LAMSGS : "メッセージおよびトレース・ログ・アラート",
    STATS_PERSPECTIVE_AVAILABILITY : "可用性",

    STATS_DISPLAY_TIME_LAST_MINUTE_LABEL : "過去 1 分",
    STATS_DISPLAY_TIME_LAST_5_MINUTES_LABEL : "過去 5 分",
    STATS_DISPLAY_TIME_LAST_10_MINUTES_LABEL : "過去 10 分",
    STATS_DISPLAY_TIME_LAST_HOUR_LABEL : "過去 1 時間",
    STATS_DISPLAY_TIME_LAST_DAY_LABEL : "過去 1 日",
    STATS_DISPLAY_TIME_LAST_WEEK_LABEL : "過去 1 週",
    STATS_DISPLAY_TIME_LAST_MONTH_LABEL : "過去 1 カ月",
    STATS_DISPLAY_TIME_LAST_YEAR_LABEL : "過去 1 年",

    STATS_DISPLAY_CUSTOM_TIME_LAST_SECOND_LABEL : "過去 {0} 秒",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MINUTE_LABEL : "過去 {0} 分",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MINUTE_AND_SECOND_LABEL : "過去 {0} 分 {1} 秒",
    STATS_DISPLAY_CUSTOM_TIME_LAST_HOUR_LABEL : "過去 {0} 時間",
    STATS_DISPLAY_CUSTOM_TIME_LAST_HOUR_AND_MINUTE_LABEL : "過去 {0} 時間 {1} 分",
    STATS_DISPLAY_CUSTOM_TIME_LAST_DAY_LABEL : "過去 {0} 日",
    STATS_DISPLAY_CUSTOM_TIME_LAST_DAY_AND_HOUR_LABEL : "過去 {0} 日 {1} 時間",
    STATS_DISPLAY_CUSTOM_TIME_LAST_WEEK_LABEL : "過去 {0} 週",
    STATS_DISPLAY_CUSTOM_TIME_LAST_WEEK_AND_DAY_LABEL : "過去 {0} 週 {1} 日",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MONTH_LABEL : "過去 {0} カ月",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MONTH_AND_DAY_LABEL : "過去 {0} カ月 {1} 日",
    STATS_DISPLAY_CUSTOM_TIME_LAST_YEAR_LABEL : "過去 {0} 年",
    STATS_DISPLAY_CUSTOM_TIME_LAST_YEAR_AND_MONTH_LABEL : "過去 {0} 年 {1} カ月",

    STATS_LIVE_UPDATE_LABEL: "ライブ更新",
    STATS_TIME_SELECTOR_NOW_LABEL: "現在",

    LOG_ANALYTICS_LOG_MESSAGE_TITLE: "ログ・メッセージ",

    AUTOSCALED_APPLICATION : "自動スケール・アプリケーション",
    AUTOSCALED_SERVER : "自動スケール・サーバー",
    AUTOSCALED_CLUSTER : "自動スケール・クラスター",
    AUTOSCALED_POLICY : "自動スケーリング・ポリシー",
    AUTOSCALED_POLICY_DISABLED : "自動スケーリング・ポリシーは無効にされています",
    AUTOSCALED_NOACTIONS : "自動スケールされたリソースのアクションは使用不可",

    START : "開始",
    START_CLEAN : "Start --clean",
    STARTING : "始動中",
    STARTED : "始動済み",
    RUNNING : "実行中",
    NUM_RUNNING: "{0} 個が実行中",
    PARTIALLY_STARTED : "部分的に開始中",
    PARTIALLY_RUNNING : "部分的に実行中",
    NOT_STARTED : "開始されていません",
    STOP : "停止",
    STOPPING : "停止中",
    STOPPED : "停止",
    NUM_STOPPED : "{0} 個が停止済み",
    NOT_RUNNING : "実行されていません",
    RESTART : "再始動",
    RESTARTING : "再始動中",
    RESTARTED : "再始動されました",
    ALERT : "アラート",
    ALERTS : "アラート",
    UNKNOWN : "不明",
    NUM_UNKNOWN : "{0} 個が不明",
    SELECT : "選択",
    SELECTED : "選択済み",
    SELECT_ALL : "すべて選択",
    SELECT_NONE : "何も選択しない",
    DESELECT: "選択解除",
    DESELECT_ALL : "すべて選択解除",
    TOTAL : "合計",
    UTILIZATION : "使用率が {0}% を超える", // percent

    ELLIPSIS_ARIA: "展開すると、詳細が表示されます。",
    EXPAND : "展開",
    COLLAPSE: "省略表示",

    ALL : "すべて",
    ALL_APPS : "すべてのアプリケーション",
    ALL_SERVERS : "すべてのサーバー",
    ALL_CLUSTERS : "すべてのクラスター",
    ALL_HOSTS : "すべてのホスト",
    ALL_APP_INSTANCES : "すべてのアプリケーション・インスタンス",
    ALL_RUNTIMES : "すべてのランタイム",

    ALL_APPS_RUNNING : "すべてのアプリケーションが実行中",
    ALL_SERVER_RUNNING : "すべてのサーバーが稼働中",
    ALL_CLUSTERS_RUNNING : "すべてのクラスターが稼働中",
    ALL_APPS_STOPPED : "すべてのアプリケーションが停止中",
    ALL_SERVER_STOPPED : "すべてのサーバーが停止中",
    ALL_CLUSTERS_STOPPED : "すべてのクラスターが停止中",
    ALL_SERVERS_UNKNOWN : "すべてのサーバーが不明",
    SOME_APPS_RUNNING : "一部のアプリケーションが実行中",
    SOME_SERVERS_RUNNING : "一部のサーバーが稼働中",
    SOME_CLUSTERS_RUNNING : "一部のクラスターが稼働中",
    NO_APPS_RUNNING : "実行中のアプリケーションはありません",
    NO_SERVERS_RUNNING : "稼働中のサーバーはありません",
    NO_CLUSTERS_RUNNING : "稼働中のクラスターはありません",

    HOST_WITH_ALL_SERVERS_RUNNING: "すべてのサーバーが稼働中のホスト", // not used anymore since 4Q
    HOST_WITH_SOME_SERVERS_RUNNING: "一部のサーバーが稼働中のホスト",
    HOST_WITH_NO_SERVERS_RUNNING: "サーバーが稼働していないホスト", // not used anymore since 4Q
    HOST_WITH_ALL_SERVERS_STOPPED: "すべてのサーバーが停止中のホスト",
    HOST_WITH_SERVERS_RUNNING: "サーバーが実行中のホスト",

    RUNTIME_WITH_SOME_SERVERS_RUNNING: "一部のサーバーが実行中のランタイム",
    RUNTIME_WITH_ALL_SERVERS_STOPPED: "すべてのサーバーが停止中のランタイム",
    RUNTIME_WITH_SERVERS_RUNNING: "サーバーが実行中のランタイム",

    START_ALL_APPS : "すべてのアプリケーションを開始しますか?",
    START_ALL_INSTANCES : "すべてのアプリケーション・インスタンスを開始しますか?",
    START_ALL_SERVERS : "すべてのサーバーを始動しますか?",
    START_ALL_CLUSTERS : "すべてのクラスターを始動しますか?",
    STOP_ALL_APPS : "すべてのアプリケーションを停止しますか?",
    STOPE_ALL_INSTANCES : "すべてのアプリケーション・インスタンスを停止しますか?",
    STOP_ALL_SERVERS : "すべてのサーバーを停止しますか?",
    STOP_ALL_CLUSTERS : "すべてのクラスターを停止しますか?",
    RESTART_ALL_APPS : "すべてのアプリケーションを再開しますか?",
    RESTART_ALL_INSTANCES : "すべてのアプリケーション・インスタンスを再開しますか?",
    RESTART_ALL_SERVERS : "すべてのサーバーを再始動しますか?",
    RESTART_ALL_CLUSTERS : "すべてのクラスターを再始動しますか?",

    START_INSTANCE : "アプリケーション・インスタンスを開始しますか?",
    STOP_INSTANCE : "アプリケーション・インスタンスを停止しますか?",
    RESTART_INSTANCE : "アプリケーション・インスタンスを再開しますか?",

    START_SERVER : "サーバー {0} を始動しますか?",
    STOP_SERVER : "サーバー {0} を停止しますか?",
    RESTART_SERVER : "サーバー {0} を再始動しますか?",

    START_ALL_INSTS_OF_APP : "{0} のすべてのインスタンスを開始しますか?", // application name
    START_APP_ON_SERVER : "{1} で {0} を開始しますか?", // app name, server name
    START_ALL_APPS_WITHIN : "{0} 内のすべてのアプリケーションを開始しますか?", // resource
    START_ALL_APP_INSTS_WITHIN : "{0} 内のすべてのアプリケーション・インスタンスを開始しますか?", // resource
    START_ALL_SERVERS_WITHIN : "{0} 内のすべてのサーバーを始動しますか?", // resource
    STOP_ALL_INSTS_OF_APP : "{0} のすべてのインスタンスを停止しますか?", // application name
    STOP_APP_ON_SERVER : "{1} で {0} を停止しますか?", // app name, server name
    STOP_ALL_APPS_WITHIN : "{0} 内のすべてのアプリケーションを停止しますか?", // resource
    STOP_ALL_APP_INSTS_WITHIN : "{0} 内のすべてのアプリケーション・インスタンスを停止しますか?", // resource
    STOP_ALL_SERVERS_WITHIN : "{0} 内のすべてのサーバーを停止しますか?", // resource
    RESTART_ALL_INSTS_OF_APP : "{0} のすべてのインスタンスを再開しますか?", // application name
    RESTART_APP_ON_SERVER : "{1} で {0} を再開しますか?", // app name, server name
    RESTART_ALL_APPS_WITHIN : "{0} 内のすべてのアプリケーションを再開しますか?", // resource
    RESTART_ALL_APP_INSTS_WITHIN : "{0} 内のすべてのアプリケーション・インスタンスを再開しますか?", // resource
    RESTART_ALL_SERVERS_WITHIN : "{0} 内のすべての実行中のサーバーを再始動しますか?", // resource

    START_SELECTED_APPS : "選択されたアプリケーションのすべてのインスタンスを開始しますか?",
    START_SELECTED_INSTANCES : "選択されたアプリケーション・インスタンスを開始しますか?",
    START_SELECTED_SERVERS : "選択されたサーバーを始動しますか?",
    START_SELECTED_SERVERS_LABEL : "選択されたサーバーを始動します",
    START_SELECTED_CLUSTERS : "選択されたクラスターを始動しますか?",
    START_CLEAN_SELECTED_SERVERS : "選択されたサーバーに「Start --clean」を使用しますか?",
    START_CLEAN_SELECTED_CLUSTERS : "選択されたクラスターに「Start --clean」を使用しますか?",
    STOP_SELECTED_APPS : "選択されたアプリケーションのすべてのインスタンスを停止しますか?",
    STOP_SELECTED_INSTANCES : "選択されたアプリケーション・インスタンスを停止しますか?",
    STOP_SELECTED_SERVERS : "選択されたサーバーを停止しますか?",
    STOP_SELECTED_CLUSTERS : "選択されたクラスターを停止しますか?",
    RESTART_SELECTED_APPS : "選択されたアプリケーションのすべてのインスタンスを再開しますか?",
    RESTART_SELECTED_INSTANCES : "選択されたアプリケーション・インスタンスを再開しますか?",
    RESTART_SELECTED_SERVERS : "選択されたサーバーを再始動しますか?",
    RESTART_SELECTED_CLUSTERS : "選択されたクラスターを再始動しますか?",

    START_SERVERS_ON_HOSTS : "選択されたホスト上のすべてのサーバーを始動しますか?",
    STOP_SERVERS_ON_HOSTS : "選択されたホスト上のすべてのサーバーを停止しますか?",
    RESTART_SERVERS_ON_HOSTS : "選択されたホスト上で実行中のすべてのサーバーを再始動しますか?",

    SELECT_APPS_TO_START : "停止中のアプリケーションから開始するものを選択してください。",
    SELECT_APPS_TO_STOP : "開始されたアプリケーションから停止するものを選択してください。",
    SELECT_APPS_TO_RESTART : "開始されたアプリケーションから再開するものを選択してください。",
    SELECT_INSTANCES_TO_START : "停止中のアプリケーション・インスタンスから開始するものを選択してください。",
    SELECT_INSTANCES_TO_STOP : "開始されたアプリケーション・インスタンスから停止するものを選択してください。",
    SELECT_INSTANCES_TO_RESTART : "開始されたアプリケーション・インスタンスから再開するものを選択してください。",
    SELECT_SERVERS_TO_START : "停止中のサーバーから始動するものを選択してください。",
    SELECT_SERVERS_TO_STOP : "始動されたサーバーから停止するものを選択してください。",
    SELECT_SERVERS_TO_RESTART : "始動されたサーバーから再始動するものを選択してください。",
    SELECT_CLUSTERS_TO_START : "停止中のクラスターから始動するものを選択してください。",
    SELECT_CLUSTERS_TO_STOP : "始動されたクラスターから停止するものを選択してください。",
    SELECT_CLUSTERS_TO_RESTART : "始動されたクラスターから再始動するものを選択してください。",

    STATUS : "状況",
    STATE : "状態:",
    NAME : "名前:",
    DIRECTORY : "ディレクトリー",
    INFORMATION : "通知",
    DETAILS : "詳細",
    ACTIONS : "アクション",
    CLOSE : "閉じる",
    HIDE : "非表示",
    SHOW_ACTIONS : "アクションを表示",
    SHOW_SERVER_ACTIONS_LABEL : "サーバー {0} アクション",
    SHOW_APP_ACTIONS_LABEL : "アプリケーション {0} アクション",
    SHOW_CLUSTER_ACTIONS_LABEL : "クラスター {0} アクション",
    SHOW_HOST_ACTIONS_LABEL : "ホスト {0} アクション",
    SHOW_RUNTIME_ACTIONS_LABEL : "ランタイム {0} アクション",
    SHOW_SERVER_ACTIONS_MENU_LABEL : "サーバー {0} アクション・メニュー",
    SHOW_APP_ACTIONS_MENU_LABEL : "アプリケーション {0} アクション・メニュー",
    SHOW_CLUSTER_ACTIONS_MENU_LABEL : "クラスター {0} アクション・メニュー",
    SHOW_HOST_ACTIONS_MENU_LABEL : "ホスト {0} アクション・メニュー",
    SHOW_RUNTIME_ACTIONS_MENU_LABEL : "ランタイム {0} アクション・メニュー",
    SHOW_RUNTIMEONHOST_ACTIONS_MENU_LABEL : "ホスト上のランタイム {0} アクション・メニュー",
    SHOW_COLLECTION_MENU_LABEL : "コレクション {0} 状態アクション・メニュー",  // menu object id
    SHOW_SEARCH_MENU_LABEL : "検索 {0} 状態アクション・メニュー",  // menu object id


    // A bit odd to have sentence casing without punctuation?
    UNKNOWN_STATE : "{0}: 不明な状態", // resourceName
    UNKNOWN_STATE_APPS : "{0} アプリケーションが不明な状態です", // quantity
    UNKNOWN_STATE_APP_INSTANCES : "{0} アプリケーション・インスタンスが不明な状態です", // quantity
    UNKNOWN_STATE_SERVERS : "{0} サーバーが不明な状態です", // quantity
    UNKNOWN_STATE_CLUSTERS : "{0} クラスターが不明な状態です", // quantity

    INSTANCES_NOT_RUNNING : "{0} アプリケーション・インスタンスが実行されていません", // quantity
    APPS_NOT_RUNNING : "{0} アプリケーションが実行されていません", // quantity
    SERVERS_NOT_RUNNING : "{0} サーバーが稼働していません", // quantity
    CLUSTERS_NOT_RUNNING : "{0} クラスターが稼働していません", // quantity

    APP_STOPPED_ON_SERVER : "稼働中のサーバー {1} で {0} が停止しました", // appName, serverName(s)
    APPS_STOPPED_ON_SERVERS : "稼働中のサーバー {1} で {0} アプリケーションが停止しました", // quantity, serverName(s)
    APPS_STOPPED_ON_SERVER : "稼働中のサーバーで {0} アプリケーションが停止しました。", // quantity
    NUMBER_RESOURCES : "{0} リソース", // quantity
    NUMBER_APPS : "{0} アプリケーション", // quantity
    NUMBER_SERVERS : "{0} サーバー", // quantity
    NUMBER_CLUSTERS : "{0} クラスター", // quantity
    NUMBER_HOSTS : "{0} ホスト", // quantity
    NUMBER_RUNTIMES : "{0} ランタイム", // quantity
    SERVERS_INSERT : "サーバー",
    INSERT_STOPPED_ON_INSERT : "稼働中の {1} で {0} が停止しました。", // NUMBER_APPS, SERVERS_INSERT

    APPSERVER_STOPPED_ON_SERVER : "稼働中のサーバー {1} で {0} が停止しました", //appName, serverName
    APPCLUSTER_STOPPED_ON_SERVER : "稼働中のサーバー: {2} でクラスター {1} 上の {0} が停止しました",  //appName, clusterName, serverName(s)

    INSTANCES_STOPPED_ON_SERVERS : "稼働中のサーバーで {0} アプリケーション・インスタンスが停止しました。", // quantity
    INSTANCE_STOPPED_ON_SERVERS : "{0}: アプリケーション・インスタンスが実行されていません", // serverNames

    NOT_ALL_APPS_RUNNING : "{0}: 一部のアプリケーションが実行されていません", // serverName[]
    NO_APPS_RUNNING : "{0}: アプリケーションが実行されていません", // serverName[]
    NOT_ALL_APPS_RUNNING_SERVERS : "{0} サーバーは一部のアプリケーションが実行されていません", // quantity
    NO_APPS_RUNNING_SERVERS : "{0} サーバーは実行されているアプリケーションがありません", // quantity

    COUNT_OF_APPS_SELECTED : "{0} アプリケーションが選択されました",
    RATIO_RUNNING : "{0} が実行中", // ratio ex. 1/2

    RESOURCES_SELECTED : "選択済み: {0}",

    NO_HOSTS_SELECTED : "ホストが選択されていません",
    NO_DEPLOY_RESOURCE : "インストールをデプロイするリソースがありません",
    NO_TOPOLOGY : "{0} がありません。",
    COUNT_OF_APPS_STARTED  : "{0} アプリケーションが開始されました",

    APPS_LIST : "{0} アプリケーション",
    EMPTY_MESSAGE : "{0}",  // used to build a list of comma separated resource names
    APPS_INSTANCES_RUNNING_OF_TOTAL : "{0}/{1} のインスタンスが実行中",
    HOSTS_SERVERS_RUNNING_OF_TOTAL : "{0}/{1} のサーバーが実行中",
    RESOURCE_ON_RESOURCE : "{1} 上の {0}", // resource name, resource name
    RESOURCE_ON_SERVER_RESOURCE : "サーバー {1} 上の {0}", // resource name, resource name
    RESOURCE_ON_CLUSTER_RESOURCE : "クラスター {1} 上の {0}", // resource name, resource name

    RESTART_DISABLED_FOR_ADMIN_CENTER: "このサーバーは Admin Center をホストしているため、再始動できません",
    ACTION_DISABLED_FOR_USER: "ユーザーに権限がないため、このリソースのアクションは無効です。",

    RESTART_AC_TITLE: "Admin Center の再開不可",
    RESTART_AC_DESCRIPTION: "{0} が Admin Center を提供しています。 Admin Center は自身を再開できません。",
    RESTART_AC_MESSAGE: "他の選択されたサーバーはすべて再始動されます。",
    RESTART_AC_CLUSTER_MESSAGE: "他のすべての選択されたクラスターは再始動します。",

    STOP_AC_TITLE: "Admin Center の停止",
    STOP_AC_DESCRIPTION: "サーバー {0} は Admin Center を実行する集合コントローラーです。 それを停止すると、Liberty 集合管理操作に影響を与え、Admin Center が使用不可になる可能性があります。",
    STOP_AC_MESSAGE: "このコントローラーを停止しますか?",
    STOP_STANDALONE_DESCRIPTION: "サーバー {0} は Admin Center を実行します。 これを停止すると Admin Center が使用不可になります。",
    STOP_STANDALONE_MESSAGE: "このサーバーを停止しますか?",

    STOP_CONTROLLER_TITLE: "コントローラーの停止",
    STOP_CONTROLLER_DESCRIPTION: "サーバー {0} は集合コントローラーです。 これを停止すると Liberty 集合操作に影響を与える場合があります。",
    STOP_CONTROLLER_MESSAGE: "このコントローラーを停止しますか?",

    STOP_AC_CLUSTER_TITLE: "クラスター {0} の停止",
    STOP_AC_CLUSTER_DESCRIPTION: "クラスター {0} は、Admin Center を実行する集合コントローラーを含みます。  これを停止すると、Liberty 集合管理操作に影響を与え、Admin Center が使用不可になる場合があります。",
    STOP_AC_CLUSTER_MESSAGE: "このクラスターを停止しますか?",

    INVALID_URL: "ページは存在しません。",
    INVALID_APPLICATION: "アプリケーション {0} は既に集合内に存在していません。", // application name
    INVALID_SERVER: "サーバー {0} は既に集合内に存在していません。", // server name
    INVALID_CLUSTER: "クラスター {0} は既に集合内に存在していません。", // cluster name
    INVALID_HOST: "ホスト {0} は既に集合内に存在していません。", // host name
    INVALID_RUNTIME: "ランタイム {0} は既に集合内に存在していません。", // runtime name
    INVALID_INSTANCE: "アプリケーション・インスタンス {0} は既に集合内に存在していません。", // application instance name
    GO_TO_DASHBOARD: "ダッシュボードに進む",
    VIEWED_RESOURCE_REMOVED: "リソースは削除されたか、使用可能ではなくなりました。",

    OK_DEFAULT_BUTTON: "OK",
    CONNECTION_FAILED_MESSAGE: "サーバーへの接続が失われました。 環境に対する動的変更は、ページに示されなくなります。 ページをリフレッシュして、接続と動的更新を復元してください。",
    ERROR_MESSAGE: "接続が中断されました",

    // Used by standalone stop message dialog
    STANDALONE_STOP_TITLE : 'サーバーを停止する',

    // Tags
    RELATED_RESOURCES: "関連リソース",
    TAGS : "タグ",
    TAG_BUTTON_LABEL : "タグ {0}",  // tag value
    TAGS_LABEL : "コンマ、スペース、Enter、またはタブで区切って、タグを入力します。",
    OWNER : "所有者",
    OWNER_BUTTON_LABEL : "所有者 {0}",  // owner value
    CONTACTS : "連絡先",
    CONTACT_BUTTON_LABEL : "連絡先 {0}",  // contact value
    PORTS : "ポート",
    CONTEXT_ROOT : "コンテキスト・ルート",
    HTTP : "HTTP",
    HTTPS : "HTTPS",
    MORE : "詳細",  // alt text for the ... button
    MORE_BUTTON_MENU : "{0} 詳細メニュー", // alt text for the menu
    NOTES: "注意事項",
    NOTE_LABEL : "注記 {0}",  // note value
    SET_ATTRIBUTES: "タグおよびメタデータ",
    SETATTR_RUNTIME_NAME: "{1} 上の {0}",  // runtime, host
    SAVE: "保存(S)",
    TAGINVALIDCHARS: "「/」、「<」、および「>」文字は無効です。",
    ERROR_GET_TAGS_METADATA: "製品は、リソースの現行のタグおよびメタデータを取得できません。",
    ERROR_SET_TAGS_METADATA: "エラーにより、製品はタグおよびメタデータを設定できませんでした。",
    METADATA_WILLBE_INHERITED: "メタデータはアプリケーション上に設定され、クラスター内のすべてのインスタンスにより共有されます。",
    ERROR_ALT: "エラー",

    // Graph Warning Messages
    GRAPH_SERVER_NOT_STARTED: "このサーバーは停止しているため、その最新統計情報を入手できません。 サーバーを始動して、サーバーのモニターを開始してください。",
    GRAPH_SERVER_HOSTING_APP_NOT_STARTED: "このアプリケーションの関連サーバーが停止しているため、このアプリケーションの最新統計情報を入手できません。 サーバーを始動して、このアプリケーションのモニターを開始してください。",
    GRAPH_FEATURES_NOT_CONFIGURED: "まだ何も指定されていません。 このリソースをモニターするには、「編集」アイコンをクリックしてメトリックを追加してください。",
    NO_GRAPHS_AVAILABLE: "追加できるメトリックがありません。 より多くのメトリックを使用可能にするには、追加モニター機能のインストールを試行してください。",
    NO_APPS_GRAPHS_AVAILABLE: "追加できるメトリックがありません。 より多くのメトリックを使用可能にするには、追加モニター機能のインストールを試行してください。また、アプリケーションが使用中であることを確認してください。",
    GRAPH_CONFIG_NOT_SAVED_TITLE : "保存されていない変更",
    GRAPH_CONFIG_NOT_SAVED_DESCR : "保存されていない変更があります。 別のページに移動すると変更内容は失われます。",
    GRAPH_CONFIG_NOT_SAVED_MSG : "変更を保存しますか?",

    NO_CPU_STATS_AVAILABLE : "このサーバーの CPU 使用率の統計は使用できません。",

    // Server Config
    CONFIG_NOT_AVAILABLE: "このビューを有効にするには、サーバー構成ツールをインストールしてください。",
    SAVE_BEFORE_CLOSING_DIALOG_MESSAGE: "閉じる前に変更を {0} に保存しますか?",
    SAVE: "保存(S)",
    DONT_SAVE: "保存しない",

    // Maintenance mode
    ENABLE_MAINTENANCE_MODE: "保守モードの有効化",
    DISABLE_MAINTENANCE_MODE: "保守モードの無効化",
    ENABLE_MAINTENANCE_MODE_DIALOG_TITLE: "保守モードの有効化",
    DISABLE_MAINTENANCE_MODE_DIALOG_TITLE: "保守モードの無効化",
    ENABLE_MAINTENANCE_MODE_HOST_DESCRIPTION: "ホストおよびそのすべてのサーバー ({0} サーバー) で保守モードを有効にします",
    ENABLE_MAINTENANCE_MODE_HOSTS_DESCRIPTION: "複数のホストおよびそのすべてのサーバー ({0} サーバー) で保守モードを有効にします",
    ENABLE_MAINTENANCE_MODE_SERVER_DESCRIPTION: "サーバーで保守モードを有効にします",
    ENABLE_MAINTENANCE_MODE_SERVERS_DESCRIPTION: "複数のサーバーで保守モードを有効にします",
    DISABLE_MAINTENANCE_MODE_HOST_DESCRIPTION: "ホストおよびそのすべてのサーバー ({0} サーバー) で保守モードを無効にします",
    DISABLE_MAINTENANCE_MODE_SERVER_DESCRIPTION: "サーバーで保守モードを無効にします",
    BREAK_AFFINITY_LABEL: "アクティブ・セッションとのアフィニティーを切断する",
    ENABLE_MAINTENANCE_MODE_DIALOG_ENABLE_BUTTON_LABEL: "有効化",
    DISABLE_MAINTENANCE_MODE_DIALOG_ENABLE_BUTTON_LABEL: "無効化",
    MAINTENANCE_MODE: "保守モード",
    ENABLING_MAINTENANCE_MODE: "保守モードを有効にしています",
    MAINTENANCE_MODE_ENABLED: "保守モードが有効になりました",
    MAINTENANCE_MODE_ALTERNATE_SERVERS_NOT_STARTED: "代替サーバーが始動しなかったため、保守モードは有効になりませんでした。",
    MAINTENANCE_MODE_SELECT_FORCE_MESSAGE: "代替サーバーを始動せずに保守モードを有効にするには、「強制」を選択してください。 強制により、自動スケーリング・ポリシーが中断する可能性があります。",
    MAINTENANCE_MODE_FAILED: "保守モードを有効にすることができません。",
    MAINTENANCE_MODE_FORCE_LABEL: "強制",
    MAINTENANCE_MODE_CANCEL_LABEL: "キャンセル",
    MAINTENANCE_MODE_SERVERS_IN_MAINTENANCE_MODE: "現在、{0} サーバーが保守モードです。",
    MAINTENANCE_MODE_ENABLING_MAINTENANCE_MODE_ON_ALL_HOST_SERVERS_EQUAL_OR_LESS_THEN_10: "すべてのホスト・サーバーで保守モードを有効にしています。",
    MAINTENANCE_MODE_ENABLING_MAINTENANCE_MODE_ON_ALL_HOST_SERVERS_MORE_THAN_10: "すべてのホスト・サーバーで保守モードを有効にしています。  状況については、「サーバー」ビューを表示してください。",

    SERVER_API_DOCMENTATION: "サーバー API 定義の表示",

    // objectView title
    TITLE_FOR_CLUSTER: "クラスター {0}", // cluster name
    TITLE_FOR_HOST: "ホスト {0}", // host name

    // objectView descriptor
    COLLECTIVE_CONTROLLER_DESCRIPTOR: "集合コントローラー",
    LIBERTY_SERVER : "Liberty サーバー",
    NODEJS_SERVER : "Node.js サーバー",
    CONTAINER : "コンテナー",
    CONTAINER_LIBERTY : "Liberty",
    CONTAINER_DOCKER : "Docker",
    CONTAINER_NODEJS : "Node.js",
    LIBERTY_IN_DOCKER_DESCRIPTOR : "Docker コンテナー内の Liberty サーバー",
    NODEJS_IN_DOCKER_DESCRIPTOR : "Docker コンテナー内の Node.js サーバー",
    RUNTIME_LIBERTY : "Liberty ランタイム",
    RUNTIME_NODEJS : "Node.js ランタイム",
    RUNTIME_DOCKER : "Docker コンテナー内のランタイム"

});
