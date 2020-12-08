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
      ACCOUNTING_STRING : "アカウンティング・ストリング",
      SEARCH_RESOURCE_TYPE_ALL : "すべて",
      SEARCH : "検索",
      JAVA_BATCH_SEARCH_BOX_LABEL : "「Add Search Criteria」ボタンを選択して検索基準を入力してから、値を指定してください",
      SUBMITTED : "サブミット済み",
      JMS_QUEUED : "JMS キュー済み",
      JMS_CONSUMED : "JMS コンシューム済み",
      JOB_PARAMETER : "ジョブ・パラメーター",
      DISPATCHED : "ディスパッチ済み",
      FAILED : "失敗",
      STOPPED : "停止済み",
      COMPLETED : "完了",
      ABANDONED : "中止",
      STARTED : "開始済み",
      STARTING : "開始中",
      STOPPING : "停止中",
      REFRESH : "最新表示",
      INSTANCE_STATE : "インスタンスの状態",
      APPLICATION_NAME : "アプリケーション名",
      APPLICATION: "アプリケーション",
      INSTANCE_ID : "インスタンス ID",
      LAST_UPDATE : "前回の更新",
      LAST_UPDATE_RANGE : "前回の更新範囲",
      LAST_UPDATED_TIME : "前回の更新時刻",
      DASHBOARD_VIEW : "ダッシュボード・ビュー",
      HOMEPAGE : "ホーム・ページ",
      JOBLOGS : "ジョブ・ログ",
      QUEUED : "キュー済み",
      ENDED : "終了",
      ERROR : "エラー",
      CLOSE : "閉じる",
      WARNING : "警告",
      GO_TO_DASHBOARD: "ダッシュボードに進む",
      DASHBOARD : "ダッシュボード",
      BATCH_JOB_NAME: "バッチ・ジョブ名",
      SUBMITTER: "サブミット担当者",
      BATCH_STATUS: "バッチ状況",
      EXECUTION_ID: "ジョブ実行 ID",
      EXIT_STATUS: "終了状況",
      CREATE_TIME: "作成時刻",
      START_TIME: "開始時刻",
      END_TIME: "終了時刻",
      SERVER: "サーバー",
      SERVER_NAME: "サーバー名",
      SERVER_USER_DIRECTORY: "ユーザー・ディレクトリー",
      SERVERS_USER_DIRECTORY: "サーバーのユーザー・ディレクトリー",
      HOST: "ホスト",
      NAME: "名前",
      JOB_PARAMETERS: "ジョブ・パラメーター",
      JES_JOB_NAME: "JES ジョブ名",
      JES_JOB_ID: "JES ジョブ ID",
      ACTIONS: "アクション",
      VIEW_LOG_FILE: "ログ・ファイルの表示",
      STEP_NAME: "ステップ名",
      ID: "ID",
      PARTITION_ID: "パーティション {0}",                               // Partition ID number
      VIEW_EXECUTION_DETAILS: "ジョブ実行 {0} の詳細の表示",    // Job Execution ID number
      PARENT_DETAILS: "親情報の詳細",
      TIMES: "時刻",      // Heading on section referencing create, start, and end timestamps
      STATUS: "状況",
      SEARCH_ON: "{1} {0} でのフィルタリングを選択",                    // {0} Value, {1} Column name
      SEARCH_PILL_INPUT : "検索基準を入力してください。",
      BREADCRUMB_JOB_INSTANCE : "ジョブ・インスタンス {0}",                // Job Instance ID
      BREADCRUMB_JOB_EXECUTION : "ジョブ実行 {0}",              // Job Execution ID
      BREADCRUMB_JOB_LOG : "ジョブ・ログ {0}",
      BATCH_SEARCH_CRITERIA_INVALID : "検索基準が無効です。",
      ERROR_CANNOT_HAVE_MULTIPLE_SEARCHBY : "検索基準には {0} パラメーターによる複数のフィルターを指定することはできません。", // {0} will be another translated message key like SUBMITTER and BATCH_JOB_NAME

      INSTANCES_TABLE_IDENTIFIER: "ジョブ・インスタンス・テーブル",
      EXECUTIONS_TABLE_IDENTIFIER: "ジョグ実行テーブル",
      STEPS_DETAILS_TABLE_IDENTIFIER: "ステップ詳細テーブル",
      LOADING_VIEW : "ページでは現在情報をロード中です",
      LOADING_VIEW_TITLE : "ビューをロード中",
      LOADING_GRID : "検索結果がサーバーから返されるのを待機中",
      PAGENUMBER : "ページ番号",
      SELECT_QUERY_SIZE: "照会サイズを選択",
      LINK_EXPLORE_HOST: "Explore ツールでホスト {0} 上の詳細を表示する場合に選択してください。",      // Host name
      LINK_EXPLORE_SERVER: "Explore ツールでサーバー {0} 上の詳細を表示する場合に選択してください。",  // Server name

      //ACTIONS
      RESTART: "再始動",
      STOP: "停止",
      PURGE: "パージ",
      OK_BUTTON_LABEL: "OK",
      INSTANCE_ACTIONS_BUTTON_LABEL: "ジョブ・インスタンス {0} に対するアクション",     // Job Instance ID number
      INSTANCE_ACTIONS_MENU_LABEL:  "ジョブ・インスタンスのアクション・メニュー",

      RESTART_INSTANCE_MESSAGE: "ジョブ・インスタンス {0} に関連した直近のジョブ実行を再始動しますか?",     // Job Instance ID number
      STOP_INSTANCE_MESSAGE: "ジョブ・インスタンス {0} に関連した直近のジョブ実行を停止しますか?",           // Job Instance ID number
      PURGE_INSTANCE_MESSAGE: "ジョブ・インスタンス {0} に関連したすべてのデータベース項目およびジョブ・ログをパージしますか?",     // Job Instance ID number
      PURGE_JOBSTORE_ONLY: "ジョブ・ストアのみをパージする",

      RESTART_INST_ERROR_MESSAGE: "再始動要求は失敗しました。",
      STOP_INST_ERROR_MESSAGE: "停止要求は失敗しました。",
      PURGE_INST_ERROR_MESSAGE: "パージ要求は失敗しました。",
      ACTION_REQUEST_ERROR_MESSAGE: "アクション要求が失敗しました。状況コード: {0}。  URL: {1}",  // Status Code number, URL string

      // RESTART JOB WITH PARAMETERS DIALOG
      REUSE_PARMS_TOGGLE_LABEL: "直前の実行からのパラメーターを再利用する",
      JOB_PARAMETERS_EMPTY: "'{0}' が選択されていない場合、このエリアを使用してジョブ・パラメーターを入力してください。",   // 0 - Checkbox label - REUSE_PARMS_TOGGLE_LABEL
      JOB_PARAMETER_NAME: "パラメーター名",
      JOB_PARAMETER_VALUE: "パラメーター値",
      PARM_NAME_COLUMN_HEADER: "パラメーター",
      PARM_VALUE_COLUMN_HEADER: "値",
      PARM_ADD_ICON_TITLE: "パラメーターの追加",
      PARM_REMOVE_ICON_TITLE: "パラメーターの削除",
      PARMS_ENTRY_ERROR: "パラメーター名が必要です。",
      JOB_PARAMETER_CREATE: "このジョブ・インスタンスの次回の実行にパラメーターを追加するには、{0} を選択してください。",  // 0 - Button label
      JOB_PARAMETER_CREATE_BUTTON: "テーブル・ヘッダーにパラメーター・ボタンを追加してください。",

      //JOB LOGS PAGE MESSAGES
      JOBLOGS_LOG_CONTENT : "ジョブ・ログ・コンテンツ",
      FILE_DOWNLOAD : "ファイルのダウンロード",
      DOWNLOAD_DIALOG_DESCRIPTION : "ログ・ファイルをダウンロードしますか?",
      INCLUDE_ALL_LOGS : "ジョブ実行のすべてのログ・ファイルを含める",
      LOGS_NAVIGATION_BAR : "ジョブ・ログのナビゲーション・バー",
      DOWNLOAD : "ダウンロード",
      LOG_TOP : "ログの先頭",
      LOG_END : "ログの末尾",
      PREVIOUS_PAGE : "前のページ",
      NEXT_PAGE : "次のページ",
      DOWNLOAD_ARIA : "ファイルのダウンロード",

      //Error messages for popups
      REST_CALL_FAILED : "データを取り出す呼び出しが失敗しました。",
      NO_JOB_EXECUTION_URL : "ジョブの実行回数が URL に提供されなかったか、あるいはこのインスタンスには表示するジョブの実行ログがありません。",
      NO_VIEW : "URL エラー: そのようなビューは存在しません。",
      WRONG_TOOL_ID : "URL の照会ストリングは、ツール ID {0} ではなく、{1} で始まっていました。",   // {0} and {1} are both Strings
      URL_NO_LOGS : "URL エラー: ログは存在しません。",
      NOT_A_NUMBER : "URL エラー: {0} は数値でなければなりません。",                                                // {0} is a field name
      PARAMETER_REPETITION : "URL エラー: {0} をパラメーターで使用できるのは 1 回限りです。",                   // {0} is a field name
      URL_PAGE_PARAMS_ERROR : "URL エラー: ページ・パラメーターは範囲外です。",
      INVALID_PARAMETER : "URL エラー: {0} は有効なパラメーターではありません。",                                       // {0} is a String
      URL_MULTIPLE_ATTRIBUTES : "URL エラー: URL には、ジョブ実行またはジョブ・インスタンスのいずれかを含めることができます。その両方を含めることはできません。",
      MISSING_EXECUTION_ID_PARAM : "必要な実行 ID パラメーターが欠落しています。",
      PERSISTENCE_CONFIGURATION_REQUIRED : "Java Batch ツールを使用するには、Java バッチ・パーシスタント・データベース構成が必要です。",
      IGNORED_SEARCH_CRITERIA : "フィルター基準 {0} は結果で無視されます。",

      GRIDX_SUMMARY_TEXT : "最新の ${0} ジョブ・インスタンスを表示しています"

});

