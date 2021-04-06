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
    LIBERTY_HEADER_TITLE: "Liberty Admin Center",
    LIBERTY_HEADER_PROFILE: "設定",
    LIBERTY_HEADER_LOGOUT: "ログアウト",
    LIBERTY_HEADER_LOGOUT_USERNAME: "ログアウト: {0}",
    TOOLBOX_BANNER_LABEL: "{0} バナー",  // TOOLBOX_TITLE
    TOOLBOX_TITLE: "ツールボックス",
    TOOLBOX_TITLE_LOADING_TOOL: "ツールのロード中 ...",
    TOOLBOX_TITLE_EDIT: "ツールボックスの編集",
    TOOLBOX_EDIT: "編集",
    TOOLBOX_DONE: "完了",
    TOOLBOX_SEARCH: "フィルター",
    TOOLBOX_CLEAR_SEARCH: "フィルター基準をクリア",
    TOOLBOX_END_SEARCH: "フィルター処理を終了",
    TOOLBOX_ADD_TOOL: "ツールの追加",
    TOOLBOX_ADD_CATALOG_TOOL: "ツールの追加",
    TOOLBOX_ADD_BOOKMARK: "ブックマークの追加",
    TOOLBOX_REMOVE_TITLE: "ツール {0} の削除",
    TOOLBOX_REMOVE_TITLE_NO_TOOL_NAME: "ツールの削除",
    TOOLBOX_REMOVE_MESSAGE: "{0} を削除しますか？",
    TOOLBOX_BUTTON_REMOVE: "削除",
    TOOLBOX_BUTTON_OK: "OK",
    TOOLBOX_BUTTON_GO_TO: "ツールボックスに進む",
    TOOLBOX_BUTTON_CANCEL: "キャンセル",
    TOOLBOX_BUTTON_BGTASK: "バックグラウンド・タスク",
    TOOLBOX_BUTTON_BACK: "戻る",
    TOOLBOX_BUTTON_USER: "ユーザー",
    TOOLBOX_ADDTOOL_ERROR_MESSAGE: "ツール {0} の追加中にエラーが発生しました: {1}",
    TOOLBOX_REMOVETOOL_ERROR_MESSAGE: "ツール {0} の削除中にエラーが発生しました: {1}",
    TOOLBOX_GET_ERROR_MESSAGE: "ツールボックス {0} のツールを取得中にエラーが発生しました",
    TOOLCATALOG_TITLE: "ツール・カタログ",
    TOOLCATALOG_ADDTOOL_TITLE: "ツールの追加",
    TOOLCATALOG_ADDTOOL_MESSAGE: "ツールボックスにツール {0} を追加しますか？",
    TOOLCATALOG_BUTTON_ADD: "追加",
    TOOL_FRAME_TITLE: "ツール・フレーム",
    TOOL_DELETE_TITLE: "{0} の削除",
    TOOL_ADD_TITLE: "{0} の追加",
    TOOL_ADDED_TITLE: "{0} は既に追加されています",
    TOOL_LAUNCH_ERROR_MESSAGE_TITLE: "ツールが見つかりません",
    TOOL_LAUNCH_ERROR_MESSAGE: "ツールがカタログ内にないため要求されたツールが起動しませんでした。",
    LIBERTY_UI_ERROR_MESSAGE_TITLE: "エラー",
    LIBERTY_UI_WARNING_MESSAGE_TITLE: "警告",
    LIBERTY_UI_INFO_MESSAGE_TITLE: "通知",
    LIBERTY_UI_CATALOG_GET_ERROR: "カタログ {0} の取得中にエラーが発生しました",
    LIBERTY_UI_CATALOG_GET_TOOL_ERROR: "カタログ {1} からツール {0} を取得中にエラーが発生しました",
    PREFERENCES_TITLE: "設定",
    PREFERENCES_SECTION_TITLE: "設定",
    PREFERENCES_ENABLE_BIDI: "双方向言語サポートを有効にする",
    PREFERENCES_BIDI_TEXTDIR: "テキストの方向",
    PREFERENCES_BIDI_TEXTDIR_LTR: "左から右",
    PREFERENCES_BIDI_TEXTDIR_RTL: "右から左",
    PREFERENCES_BIDI_TEXTDIR_CONTEXTUAL: "コンテキスト",
    PREFERENCES_SET_ERROR_MESSAGE: "ツールボックス {0} にユーザー・プリファレンスを設定中にエラーが発生しました",
    BGTASKS_PAGE_LABEL: "バックグラウンド・タスク",
    BGTASKS_DEPLOYMENT_INSTALLATION_POPUP: "インストールのデプロイ {0}", // {0} is the token number associated with the deployment
    BGTASKS_DEPLOYMENT_INSTALLATION: "インストールのデプロイ {0} - {1}", // {0} is the token number associated with the deployment, {1} is the server name
    BGTASKS_STATUS_RUNNING: "実行中",
    BGTASKS_STATUS_FAILED: "失敗",
    BGTASKS_STATUS_SUCCEEDED: "終了", 
    BGTASKS_STATUS_WARNING: "部分的に成功",
    BGTASKS_STATUS_PENDING: "処理待ち",
    BGTASKS_INFO_DIALOG_TITLE: "詳細",
    //BGTASKS_INFO_DIALOG_DESC: "Description:",
    BGTASKS_INFO_DIALOG_STDOUT: "標準出力:",
    BGTASKS_INFO_DIALOG_STDERR: "標準エラー:",
    BGTASKS_INFO_DIALOG_EXCEPTION: "例外:",
    //BGTASKS_INFO_DIALOG_RETURN_CODE: "Return code:",
    BGTASKS_INFO_DIALOG_RESULT: "結果:",
    BGTASKS_INFO_DIALOG_DEPLOYED_ARTIFACT_NAME: "サーバー名:",
    BGTASKS_INFO_DIALOG_DEPLOYED_USER_DIR: "ユーザー・ディレクトリー:",
    BGTASKS_POPUP_RUNNING_TASK_TITLE: "アクティブなバックグラウンド・タスク",
    BGTASKS_POPUP_RUNNING_TASK_NONE: "なし",
    BGTASKS_POPUP_RUNNING_TASK_NO_ACTIVE_TASK: "アクティブなバックグラウンド・タスクがありません",
    BGTASKS_DISPLAY_BUTTON: "タスクの詳細とヒストリー",
    BGTASKS_EXPAND: "セクションの展開",
    BGTASKS_COLLAPSE: "セクションの省略",
    PROFILE_MENU_HELP_TITLE: "ヘルプ",
    DETAILS_DESCRIPTION: "説明",
    DETAILS_OVERVIEW: "概説",
    DETAILS_OTHERVERSIONS: "その他のバージョン",
    DETAILS_VERSION: "バージョン: {0}",
    DETAILS_UPDATED: "更新: {0}",
    DETAILS_NOTOPTIMIZED: "現行装置に最適化されていません。",
    DETAILS_ADDBUTTON: "「マイ・ツールボックス」に追加する",
    DETAILS_OPEN: "開く",
    DETAILS_CATEGORY: "カテゴリー {0}",
    DETAILS_ADDCONFIRM: "ツール {0} が正常にツールボックスに追加されました。",
    CONFIRM_DIALOG_HELP: "ヘルプ",
    YES_BUTTON_LABEL: "{0} はい",  // insert is dialog title
    NO_BUTTON_LABEL: "{0} いいえ",  // insert is dialog title

    YES: "Yes",
    NO: "No",

    TOOL_OIDC_ACCESS_DENIED: "ユーザーはこの要求を完了するためのアクセス権があるロールに属していません。",
    TOOL_OIDC_GENERIC_ERROR: "エラーが発生しました。 詳しくは、ログに記述されているエラーを確認してください。",
    TOOL_DISABLE: "ユーザーはこのツールを使用するための許可を持っていません。 管理者役割のユーザーのみがこのツールを使用する許可を持ちます。" 
});
