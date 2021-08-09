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
    "ADD_NEW": "新規追加",
    "CANCEL": "キャンセル",
    "CLEAR": "検索入力をクリア",
    "CLICK_TO_SORT": "クリックで列をソート",
    "CLOSE": "閉じる",
    "COPY_TO_CLIPBOARD": "クリップボードにコピー",
    "COPIED_TO_CLIPBOARD": "クリップボードにコピーされました",
    "DELETE": "削除",
    "DONE": "完了",
    "EDIT": "編集",
    "FALSE": "False",
    "GENERATE": "生成",
    "LOADING": "ロード中",
    "LOGOUT": "ログアウト",
    "NEXT_PAGE": "次のページ",
    "NO_RESULTS_FOUND": "結果は検出されませんでした",
    "PAGES": "{0}/{1} ページ",   // {0} - current page number; {1} - total pages
    "PAGE_SELECT": "表示するページ番号の選択",
    "PREVIOUS_PAGE": "前のページ",
    "PROCESSING": "処理中",
    "REGENERATE": "再生成",
    "REGISTER": "登録",
    "TABLE_FIELD_SORT_ASC": "表は {0} によって昇順でソートされます。",   // {0} - column name (ie. 'Name', 'Client ID')
    "TABLE_FIELD_SORT_DESC": "表は {0} によって降順でソートされます。", // {0} - column name (ie. 'Name', 'Client ID')
    "TRUE": "True",
    "TRY_AGAIN": "再試行...",
    "UPDATE": "更新",

    // Common Column Names
    "EXPIRES_COL": "有効期限",
    "ISSUED_COL": "発行日",
    "NAME_COL": "名前",
    "TYPE_COL": "タイプ",

    // Account Manager
    // 'app-token' and 'app-password' are keywords - don't translate
    // name - the user defined name given the app-password or app-token
    "ACCT_MGR_TITLE": "個人用トークンの管理",
    "ACCT_MGR_DESC": "app-password と app-token を作成、削除および再生成します。",
    "ADD_NEW_AUTHENTICATION": "新規 app-password または app-token を追加します。",
    "NAME_IDENTIFIER": "名前: {0}",
    "ADD_NEW_TITLE": "新規認証の登録",
    "NOT_GENERATED_PLACEHOLDER": "生成されません",
    "AUTHENTICAION_GENERATED": "生成された認証",
    "GENERATED_APP_PASSWORD": "生成された app-password",
    "GENERATED_APP_TOKEN": "生成された app-token",
    "COPY_APP_PASSWORD": "app-password をクリップボードにコピー",
    "COPY_APP_TOKEN": "app-token をクリップボードにコピー",
    "REGENERATE_APP_PASSWORD": "App-Password の再生成",
    "REGENERATE_PW_WARNING": "このアクションにより、現在の app-password が上書きされます。",
    "REGENERATE_PW_PLACEHOLDER": "以前 ({0}) に生成されたパスワード",        // 0 - date
    "REGENERATE_APP_TOKEN": "App-Token の再生成",
    "REGENERATE_TOKEN_WARNING": "このアクションにより、現在の app-token が上書きされます。",
    "REGENERATE_TOKEN_PLACEHOLDER": "以前 ({0}) に生成されたトークン",        // 0 - date
    "DELETE_PW": "この app-password の削除",
    "DELETE_TOKEN": "この app-token の削除",
    "DELETE_WARNING_PW": "このアクションにより、現在割り当てられている app-password が削除されます。",
    "DELETE_WARNING_TOKEN": "このアクションにより、現在割り当てられている app-token が削除されます。",
    "REGENERATE_ARIA": "{1} の {0} の再生成",     // 0 - 'app-password' or 'app-token'; 1 - name
    "DELETE_ARIA": "名前が {1} の {0} の削除",       // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_GENERATE_FAIL": "{0} の生成エラー", // 0 - 'App-Password' or 'App-Token'
    "GENERIC_GENERATE_FAIL_MSG": "名前が {1} の新規 {0} の生成中にエラーが発生しました。",  // 0 - 'app-password' or 'app-token'; 1 - name
    "ERR_NAME": "この名前は既に {0} に関連付けられているか、長すぎます。", // 0 - 'app-password' or 'app-token'
    "GENERIC_DELETE_FAIL": "{0} の削除エラー",     // 0 - 'App-Password' or 'App-Token'
    "GENERIC_DELETE_FAIL_MSG": "名前が {1} の {0} の削除中にエラーが発生しました。",  // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_REGENERATE_FAIL": "{0} の再生成エラー",  // 0 - 'App-Password' or 'App-Token'
    "GENERIC_REGENERATE_FAIL_MSG": "名前が {1} の {0} の再生成中にエラーが発生しました。",  // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_REGENERATE_FAIL_CREATE_MSG": "名前が {1} の {0} の再生成中にエラーが発生しました。 {0} は削除されましたが、再作成できませんでした。", // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_FETCH_FAIL": "認証の取得エラー",
    "GENERIC_FETCH_FAIL_MSG": "app-password または app-token の現行リストを取得できません。",
    "GENERIC_NOT_CONFIGURED": "クライアントが未構成",
    "GENERIC_NOT_CONFIGURED_MSG": "appPasswordAllowed と appTokenAllowed のクライアント属性が構成されていません。  データを取得できません。",
    "APP_PASSWORD_NOT_CONFIGURED": "appPasswordAllowed クライアント属性が構成されていません。",  // 'appPasswordAllowed' is a config option. Do not translate.
    "APP_TOKEN_NOT_CONFIGURED": "appTokenAllowed クライアント属性が構成されていません。"         // 'appTokenAllowed' is a config option.  Do not translate.
};
