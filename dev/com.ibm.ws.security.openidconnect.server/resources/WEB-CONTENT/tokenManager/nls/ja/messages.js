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
    "CLEAR_SEARCH": "検索入力をクリア",
    "CLEAR_FILTER": "フィルターをクリア",
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
    "TABLE_BATCH_BAR": "テーブル・アクション・バー",
    "TABLE_FIELD_SORT_ASC": "表は {0} によって昇順でソートされます。",   // {0} - column name (ie. 'Name', 'Client ID')
    "TABLE_FIELD_SORT_DESC": "表は {0} によって降順でソートされます。", // {0} - column name (ie. 'Name', 'Client ID')
    "TRUE": "True",
    "TRY_AGAIN": "再試行...",
    "UPDATE": "更新",

    // Common Column Names
    "CLIENT_NAME_COL": "クライアント名",
    "EXPIRES_COL": "有効期限",
    "ISSUED_COL": "発行日",
    "NAME_COL": "名前",
    "TYPE_COL": "タイプ",

    // Token Manager
    // 'app-token' and 'app-password' are keywords - don't translate
    // name - user defined name given to the app-password/app-token; user id - user's login name
    "TOKEN_MGR_TITLE": "トークンの削除",
    "TOKEN_MGR_DESC": "指定されたユーザーの app-password と app-token を削除します。",
    "TOKEN_MGR_SEARCH_PLACEHOLDER": "ユーザー ID の入力",
    "TABLE_FILLED_WITH": "{1} に属している {0} 認証を表示するように表が更新されました。",  // 0 - number of entries in table; 1 - user id
    "DELETE_SELECTED": "選択した app-password と app-token を削除します。",
    "DELETE_ARIA": "名前が {1} の {0} の削除",         // 0 - 'app-password' or 'app-token'; 1 - name
    "DELETE_PW": "この app-password の削除",
    "DELETE_TOKEN": "この app-token の削除",
    "DELETE_FOR_USERID": "{1} の {0}",                // 0 - name; 1 - user id
    "DELETE_WARNING_PW": "このアクションにより、現在割り当てられている app-password が削除されます。",
    "DELETE_WARNING_TOKEN": "このアクションにより、現在割り当てられている app-token が削除されます。",
    "DELETE_MANY": "App-Password/App-Token の削除",
    "DELETE_MANY_FOR": "{0} に割り当て済み",              // 0 - user id
    "DELETE_ONE_MESSAGE": "このアクションにより、選択した app-password/app-token が削除されます。",
    "DELETE_MANY_MESSAGE": "このアクションにより、選択した {0} 個の app-password/app-token が削除されます。",  // 0 - number
    "DELETE_ALL_MESSAGE": "このアクションにより、{0} に属しているすべての app-password/app-token が削除されます。", // 0 - user id
    "DELETE_NONE": "削除対象を選択",
    "DELETE_NONE_MESSAGE": "削除する必要がある app-password または app-token を示すチェック・ボックスを選択してください。",
    "SINGLE_ITEM_SELECTED": "1 項目が選択されました",
    "ITEMS_SELECTED": "{0} 項目が選択されました",            // 0 - number
    "SELECT_ALL_AUTHS": "このユーザーのすべての app-password と app-token を選択します。",
    "SELECT_SPECIFIC": "名前が {1} の {0} を削除対象に選択します。",  // 0 - 'app-password' or 'app-token; 1 - name
    "NO_QUERY": "何かをお探しですか? app-password と app-token を表示するには、ユーザー IDを入力してください。",
    "GENERIC_FETCH_FAIL": "{0} の取得エラー",      // 0 - 'App-Passwords' or 'App-Tokens'
    "GENERIC_FETCH_FAIL_MSG": "{1} に属している {0} のリストを取得できません。", // 0 - 'app-passwords' or 'app-tokens; 1 - user id
    "GENERIC_DELETE_FAIL": "{0} の削除エラー",       // 0 - 'App-Password' or 'App-Token'
    "GENERIC_DELETE_FAIL_MSG": "名前が {1} の {0} の削除中にエラーが発生しました。",  // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_DELETE_ALL_FAIL_MSG": "{1} の {0} の削除中にエラーが発生しました。",     // 0 - 'app-passwords' or 'app-tokens'; 1 - user id
    "GENERIC_DELETE_FAIL_NOTYPES": "削除エラー",
    "GENERIC_DELETE_FAIL_NOTYPES_ONE_MSG": "次の app-password または app-token の削除中にエラーが発生しました。",
    "GENERIC_DELETE_FAIL_NOTYPES_MSG": "次の {0} 個の app-password と app-token の削除中にエラーが発生しました。",  // 0 - number
    "IDENTIFY_AUTH": "{0} {1}",   // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_FETCH_ALL_FAIL": "認証の取得エラー",
    "GENERIC_FETCH_ALL_FAIL_MSG": "{0} に属している app-password と app-token のリストを取得できません。",   // 0 - user id
    "GENERIC_NOT_CONFIGURED": "クライアントが未構成",
    "GENERIC_NOT_CONFIGURED_MSG": "appPasswordAllowed と appTokenAllowed のクライアント属性が構成されていません。  データを取得できません。"
};
