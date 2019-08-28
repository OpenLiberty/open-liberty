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

    // Client Admin
    "CLIENT_ADMIN_TITLE": "OAuth クライアントの管理",
    "CLIENT_ADMIN_DESC": "このツールは、クライアントの追加と編集、およびクライアント秘密鍵の再生成に使用します。",
    "CLIENT_ADMIN_SEARCH_PLACEHOLDER": "OAuth クライアント名でのフィルター",
    "ADD_NEW_CLIENT": "新規 OAuth クライアントを追加します。",
    "CLIENT_NAME": "クライアント名",
    "CLIENT_ID": "クライアント ID",
    "EDIT_ARIA": "{0} OAuth クライアントの編集",      // {0} - name
    "DELETE_ARIA": "{0} OAuth クライアントの削除",  // {0} - name
    "CLIENT_SECRET": "クライアント秘密鍵",
    "GRANT_TYPES": "認可タイプ",
    "SCOPE": "有効範囲",
    "PREAUTHORIZED_SCOPE": "事前許可済み有効範囲 (オプション)",
    "REDIRECT_URLS": "リダイレクト URL (オプション)",
    "CLIENT_SECRET_CHECKBOX": "クライアント秘密鍵の再生成",
    "NONE_SELECTED": "選択なし",
    "MODAL_EDIT_TITLE": "OAuth クライアントの編集",
    "MODAL_REGISTER_TITLE": "新規 OAuth クライアントの登録",
    "MODAL_SECRET_REGISTER_TITLE": "保存された OAuth 登録",
    "MODAL_SECRET_UPDATED_TITLE": "更新された OAuth 登録",
    "MODAL_DELETE_CLIENT_TITLE": "この OAuth クライアントの削除",
    "RESET_GRANT_TYPE": "選択したすべての認可タイプをクリアします",
    "SELECT_ONE_GRANT_TYPE": "少なくとも 1 つの認可タイプを選択してください",
    "SPACE_HELPER_TEXT": "スペースで区切った値",
    "REDIRECT_URL_HELPER_TEXT": "スペースで区切った絶対リダイレクト URL",
    "DELETE_OAUTH_CLIENT_DESC": "この操作により、登録済みのクライアントはクライアント登録サービスから削除されます。",
    "REGISTRATION_SAVED": "クライアント ID とクライアント秘密鍵が生成され、割り当てられました。",
    "REGISTRATION_UPDATED": "このクライアント用に新規クライアント秘密鍵が生成され、割り当てられました。",
    "COPY_CLIENT_ID": "クライアント ID をクリップボードにコピー",
    "COPY_CLIENT_SECRET": "クライアント秘密鍵をクリップボードにコピー",
    "REGISTRATION_UPDATED_NOSECRET": "{0} OAuth クライアントが更新されました。",                 // {0} - client name
    "ERR_MULTISELECT_GRANT_TYPES": "少なくとも 1 つの認可タイプを選択する必要があります。",
    "ERR_REDIRECT_URIS": "値は絶対 URI でなければなりません。",
    "GENERIC_REGISTER_FAIL": "OAuth クライアントの登録エラー",
    "GENERIC_UPDATE_FAIL": "OAuth クライアントの更新エラー",
    "GENERIC_DELETE_FAIL": "OAuth クライアントの削除エラー",
    "GENERIC_MISSING_CLIENT": "OAuth クライアントの取得エラー",
    "GENERIC_REGISTER_FAIL_MSG": "{0} OAuth クライアントの登録中にエラーが発生しました。",  // {0} - client name
    "GENERIC_UPDATE_FAIL_MSG": "{0} OAuth クライアントの更新中にエラーが発生しました。",       // {0} - client name
    "GENERIC_DELETE_FAIL_MSG": "{0} OAuth クライアントの削除中にエラーが発生しました。",       // {0} - client name
    "GENERIC_MISSING_CLIENT_MSG": "ID {1} の OAuth クライアント {0} が見つかりませんでした。",     // {0} - client name; {1} - an ID
    "GENERIC_RETRIEVAL_FAIL_MSG": "{0} OAuth クライアントの情報の取得中にエラーが発生しました。", // {0} - client name
    "GENERIC_GET_CLIENTS_FAIL": "OAuth クライアントの取得エラー",
    "GENERIC_GET_CLIENTS_FAIL_MSG": "OAuth クライアントのリストの取得中にエラーが発生しました。",

    "RESET_SELECTION": "選択したすべての {0} をクリア",     // {0} - field name (ie 'Grant types')
    "NUMBER_SELECTED": "選択されている {0} の数",     // {0} - field name
    "OPEN_LIST": "{0} リストをオープンします。",                   // {0} - field name
    "CLOSE_LIST": "{0} リストをクローズします。",                 // {0} - field name
    "ENTER_PLACEHOLDER": "値の入力",
    "ADD_VALUE": "エレメントの追加",
    "REMOVE_VALUE": "エレメントの削除",
    "REGENERATE_CLIENT_SECRET": "「*」は既存の値を保持します。ブランク値の場合は、新規 client_secret を生成します。ブランク以外のパラメーター値の場合は、新しく指定された値で既存の値をオーバーライドします。",
    "ALL_OPTIONAL": "すべてのフィールドはオプションです"
};
