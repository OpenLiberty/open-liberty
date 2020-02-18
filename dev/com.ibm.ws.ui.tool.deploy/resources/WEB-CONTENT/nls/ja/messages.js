var messages = {
//General
"DEPLOY_TOOL_TITLE": "デプロイ",
"SEARCH" : "検索",
"SEARCH_HOSTS" : "ホストの検索",
"EXPLORE_TOOL": "探索ツール",
"EXPLORE_TOOL_INSERT": "探索ツールの試行",
"EXPLORE_TOOL_ARIA": "新規タブでの探索ツールによるホストの検索",

//Rule Selector Panel
"RULESELECT_EDIT" : "編集",
"RULESELECT_CHANGE_SELECTION" : "選択項目の編集",
"RULESELECT_SERVER_DEFAULT" : "デフォルト・サーバー・タイプ",
"RULESELECT_SERVER_CUSTOM" : "カスタム・タイプ",
"RULESELECT_SERVER_CUSTOM_ARIA" : "カスタム・サーバー・タイプ",
"RULESELECT_NEXT" : "次へ",
"RULESELECT_SERVER_TYPE": "サーバー・タイプ",
"RULESELECT_SELECT_ONE": "1 つ選択",
"RULESELECT_DEPLOY_TYPE" : "デプロイ・ルール",
"RULESELECT_SERVER_SUBHEADING": "サーバー",
"RULESELECT_CUSTOM_PACKAGE": "カスタム・パッケージ",
"RULESELECT_RULE_DEFAULT" : "デフォルト・ルール",
"RULESELECT_RULE_CUSTOM" : "カスタム・ルール",
"RULESELECT_FOOTER" : "デプロイ・フォームに戻る前にサーバー・タイプとルール・タイプを選択してください。",
"RULESELECT_CONFIRM" : "確認",
"RULESELECT_CUSTOM_INFO": "カスタマイズされた入力およびデプロイメント動作を使用して独自のルールを定義できます。",
"RULESELECT_CUSTOM_INFO_LINK": "詳細表示",
"RULESELECT_BACK": "戻る",

//Rule Selector Aria
"RULESELECT_PANEL_ARIA" : "ルール選択パネル {0}", //RULESELECT_OPEN or RULESELECT_CLOSED
"RULESELECT_OPEN" : "開く",
"RULESELECT_CLOSED" : "クローズ済み",
"RULESELECT_SCROLL_UP": "スクロールアップ",
"RULESELECT_SCROLL_DOWN": "スクロールダウン",
"RULESELECT_EDIT_SERVER_ARIA" : "サーバー・タイプの編集。現在の選択 {0}", // Insert for SERVER TYPES
"RULESELECT_EDIT_RULE_ARIA" : "ルールの編集。現在の選択 {0}", //Insert for PACKAGE TYPES
"RULESELECT_NEXT_ARIA" : "次のパネル",

//SERVER TYPES
"LIBERTY_SERVER" : "Liberty サーバー",
"NODEJS_SERVER" : "Node.js サーバー",

//PACKAGE TYPES
"APPLICATION_PACKAGE" : "アプリケーション・パッケージ", //IMPORTANT: Ensure that the translation of this string is the same as the one for "DEPLOY_RULE_APPLICATION_PACKAGE_TYPE" in com.ibm.ws.collective.controller\resources\com\ibm\ws\collective\command\internal\resources\Messages.nlsprops
"SERVER_PACKAGE" : "サーバー・パッケージ", //IMPORTANT: Ensure that the translation of this string is the same as the one for "DEPLOY_RULE_SERVER_PACKAGE_TYPE" in com.ibm.ws.collective.controller\resources\com\ibm\ws\collective\command\internal\resources\Messages.nlsprops
"DOCKER_CONTAINER" : "Docker コンテナー", //IMPORTANT: Ensure that the translation of this string is the same as the one for "DEPLOY_RULE_DOCKER_PACKAGE_TYPE" in com.ibm.ws.collective.controller\resources\com\ibm\ws\collective\command\internal\resources\Messages.nlsprops

//Deployment parameters
"DEPLOYMENT_PARAMETERS": "デプロイメント・パラメーター",
// "PARAMETERS_TOGGLE": "Toggle between upload and use a file located on the collective controller",
"GROUP_DEPLOYMENT_PARAMETERS": "デプロイメント・パラメーター ({0})",
"PARAMETERS_DESCRIPTION": "詳細は、選択されたサーバーおよびテンプレート・タイプに基づきます。",
"PARAMETERS_TOGGLE_CONTROLLER": "集合コントローラー上にあるファイルを使用",
"PARAMETERS_TOGGLE_UPLOAD": "ファイルのアップロード",
"SEARCH_IMAGES": "イメージの検索",
"SEARCH_CLUSTERS": "クラスターの検索",
"CLEAR_FIELD_BUTTON_ARIA": "入力値のクリア",

// file browser and upload
"SERVER_BROWSE_DESCRIPTION": "サーバー・パッケージ・ファイルのアップロード",
"BROWSE_TITLE": "{0} のアップロード",
"STRONGLOOP_BROWSE": "ファイルをここにドラッグするか {0} して、ファイル名を指定してください", //BROWSE_INSERT
"BROWSE_INSERT" : "参照",
"BROWSE_ARIA": "ファイルの参照",
"FILE_UPLOAD_PREVIOUS" : "集合コントローラー上にあるファイルを使用",
"IS_UPLOADING": "{0} のアップロード中...",
"CANCEL" : "キャンセル",
"UPLOAD_SUCCESSFUL" : "{0} が正常にアップロードされました。", // Package Name
"UPLOAD_FAILED" : "アップロードに失敗しました",
"RESET" : "リセット",
"FILE_UPLOAD_WRITEDIRS_EMPTY_ERROR" : "書き込みディレクトリー・リストが空です。",
"FILE_UPLOAD_WRITEDIRS_PATH_ERROR" : "指定するパスは、書き込みディレクトリー・リストに含まれている必要があります。",
"PARAMETERS_FILE_ARIA" : "デプロイメント・パラメーターまたは {0}", // Upload Package name

// docker
"DOCKER_MISSING_REPOSITORY_ERROR": "Docker リポジトリーを構成する必要があります",
"DOCKER_EMPTY_IMAGE_ERROR": "構成済みの Docker リポジトリーでイメージが見つかりませんでした",
"DOCKER_GENERIC_ERROR": "Docker イメージがロードされませんでした。 構成済みの Docker リポジトリーがあることを確認してください。",
"REFRESH": "最新表示",
"REFRESH_ARIA": "Docker イメージの最新表示",
"PARAMETERS_DOCKER_ARIA": "デプロイメント・パラメーターまたは Docker イメージの検索",
"DOCKER_IMAGES_ARIA" : "Docker イメージのリスト",
"LOCAL_IMAGE": "ローカル・イメージ名",
"DOCKER_INVALID_CONTAINER__NAME_ERROR" : "コンテナー名はフォーマット [a-zA-Z0-9][a-zA-Z0-9_.-]* に一致していなければなりません",

//Host selection
"ONE_SELECTED_HOST" : "{0} 個のホストが選択されています", //quantity
"N_SELECTED_HOSTS": "{0} 個のホストが選択されています", //quantity
"SELECT_HOSTS_MESSAGE": "使用可能なホストのリストから選択してください。 名前またはタグでホストを検索できます。",
"ONE_HOST" : "{0} 個の結果", //quantity
"N_HOSTS": "{0} 個の結果", //quantity
"SELECT_HOSTS_FOOTER": "複雑な検索が必要な場合: {0}", //EXPLORE_TOOL_INSERT
"NAME": "名前",
"NAME_FILTER": "名前によるホストのフィルター", // Used for aria-label
"TAG": "タグ",
"TAG_FILTER": "タグによるホストのフィルター",
"ALL_HOSTS_LIST_ARIA" : "すべてのホストのリスト",
"SELECTED_HOSTS_LIST_ARIA": "選択されたホストのリスト",

//Security Details
"SECURITY_DETAILS": "セキュリティーの詳細",
"SECURITY_DETAILS_FOR_GROUP": "{0} のセキュリティーの詳細",  // {0} is the translated group name
"SECURITY_DETAILS_MESSAGE": "サーバー・セキュリティー用の追加の資格情報が必要です。",
"SECURITY_CREATE_PASSWORD" : "パスワードの作成",
"KEYSTORE_PASSWORD_MESSAGE": "新しく生成された鍵ストア・ファイルにはサーバーの認証資格情報が含まれているため、パスワードを指定して保護してください。",
"PASSWORD_MESSAGE": "サーバー認証資格情報を含む新しく生成したファイルを保護するパスワードを指定してください。",
"KEYSTORE_PASSWORD": "鍵ストア・パスワード",
"CONFIRM_KEYSTORE_PASSWORD": "鍵ストア・パスワードの確認",
"PASSWORDS_DONT_MATCH": "パスワードが一致しません",
"GROUP_GENERIC_PASSWORD": "{0} ({1})", // {0} is the display label for the initial password field
                                       // {1} is the group name if the password belongs to a group
"CONFIRM_GENERIC_PASSWORD": "{0} の確認", // {0} is the display label for the initial password field
"CONFIRM_GROUP_GENERIC_PASSWORD": "{0} ({1}) の確認", // {0} is the display label for the initial password field
                                                       // {1} is the group name if the password belongs to a group

//Deploy
"REVIEW_AND_DEPLOY": "確認およびデプロイ",
"REVIEW_AND_DEPLOY_MESSAGE" : "デプロイメントの前にすべてのフィールドに{0}。", //REVIEW_AND_DEPLOY_INCOMPLETE_FIELDS
"REVIEW_AND_DEPLOY_INCOMPLETE_FIELDS": "入力する必要があります",
"READY_FOR_DEPLOYMENT": "デプロイメントの準備ができています。",
"READY_FOR_DEPLOYMENT_CAPS": "デプロイメントの準備ができています。",
"READY_TO_DEPLOY": "フォームの入力が完了しました。 {0}", //READY_FOR_DEPLOYMENT_CAPS
"READY_TO_DEPLOY_SERVER": "フォームの入力が完了しました。 サーバー・パッケージは {0}", //READY_FOR_DEPLOYMENT
"READY_TO_DEPLOY_DOCKER": "フォームの入力が完了しました。 Docker コンテナーは {0}", //READY_FOR_DEPLOYMENT
"DEPLOY": "デプロイ",

"DEPLOY_UPLOADING" : "サーバー・パッケージのアップロードが完了するまでお待ちください...",
"DEPLOY_FILE_UPLOADING" : "ファイルのアップロードを完了しています...",
"UPLOADING": "アップロードしています...",
"DEPLOY_UPLOADING_MESSAGE" : "デプロイメント・プロセスが開始するまで、このウィンドウは開いたままにしてください。",
"DEPLOY_AFTER_UPLOADED_MESSAGE" : "{0} のアップロードが完了した後に、ここでデプロイメントの進行をモニターできます。", // Package name
"DEPLOY_FILE_UPLOAD_PERCENT" : "{0} - {1}% 完了", // Package name, number
"DEPLOY_WATCH_FOR_UPDATES": "ここで更新を監視するか、このウィンドウを閉じて更新をバックグラウンドで実行させてください。",
"DEPLOY_CHECK_STATUS": "デプロイメントの状況は、いつでも、画面の右上隅にある「バックグラウンド・タスク」アイコンをクリックして確認できます。",
"DEPLOY_IN_PROGRESS": "デプロイメントは進行中です。",
"DEPLOY_VIEW_BG_TASKS": "バックグラウンド・タスクの表示",
"DEPLOYMENT_PROGRESS": "デプロイメント進行状況",
"DEPLOYING_IMAGE": "{0} を {1} 個のホストへ", // Package name (e.g.,  someServerName.zip, aLibertyDockerImage), number of hosts
"VIEW_DEPLOYED_SERVERS": "正常にデプロイされたサーバーの表示",
"DEPLOY_PERCENTAGE": "{0}% 完了", //quantity
"DEPLOYMENT_COMPLETE_TITLE" : "デプロイメントは完了しました。",
"DEPLOYMENT_COMPLETE_WITH_ERRORS_TITLE" : "デプロイメントは完了しましたが、エラーがいくつかあります。",
"DEPLOYMENT_COMPLETE_MESSAGE" : "エラーをさらに詳細に調査したり、新たにデプロイされたサーバーについて確認したり、別のデプロイメントを開始したりすることができます。",
"DEPLOYING": "デプロイ中...",
"DEPLOYMENT_FAILED": "デプロイメントは失敗しました。",
"RETURN_DEPLOY": "デプロイ・フォームに戻って再サブミットしてください",
"REUTRN_DEPLOY_HEADER": "再試行",

//Footer
"FOOTER": "さらにデプロイする場合:",
"FOOTER_BUTTON_MESSAGE" : "別のデプロイメントを開始",

//Error stuff
"ERROR_TITLE": "エラーの要約",
"ERROR_VIEW_DETAILS" : "エラー詳細の表示",
"ONE_ERROR_ONE_HOST": "1 つのホストで 1 つのエラーが発生しました",
"ONE_ERROR_MULTIPLE_HOST": "複数のホストで 1 つのエラーが発生しました",
"MULTIPLE_ERROR_ONE_HOST": "1 つのホストで複数のエラーが発生しました",
"MULTIPLE_ERROR_MULTIPLE_HOST": "複数のホストで複数のエラーが発生しました",
"INITIALIZATION_ERROR_MESSAGE": "サーバー上のホストまたはデプロイ・ルールの情報にアクセスできません",
"TRANSLATIONS_ERROR_MESSAGE" : "外部化ストリングにアクセスできませんでした",
"MISSING_HOST": "リストから少なくとも 1 つのホストを選択してください",
"INVALID_CHARACTERS" : "「()$%&」のような特殊文字をフィールドに含めることはできません",
"INVALID_DOCKER_IMAGE" : "イメージが見つかりませんでした",
"ERROR_HOSTS" : "{0}、およびその他 {1} 個" // if there are more than three hosts reporting error, the message for errors from 6 hosts would be like:
                                      // host1.com, host2.com, host3.com and 3 others
};
