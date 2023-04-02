/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
define({
    ERROR : "エラー",
    ERROR_STATUS : "{0} エラー: {1}", //responseStatusCode, response.text
    ERROR_URL_REQUEST_NO_STATUS : "{0} の要求中にエラーが発生しました。", // url
    ERROR_URL_REQUEST : "{1} の要求中に {0} エラーが発生しました。", //responseStatusCode, url
    ERROR_SERVER_UNAVAILABLE : "割り当てられた時間内にサーバーが応答しませんでした。",
    ERROR_APP_NOT_AVAILABLE : "ディレクトリー {3} の ホスト {2} 上にあるサーバー {1} は、アプリケーション {0} を使用できなくなりました。", //appName, serverName, hostName, userdirName
    ERROR_APPLICATION_OPERATION : "ディレクトリー {4} のホスト {3} 上にあるサーバー {2} の {0} アプリケーション {1} への試行中にエラーが発生しました。", //operation, appName, serverName, hostName, userdirName
    CLUSTER_STATUS : "クラスター {0} は {1} です。", //clusterName, clusterStatus
    CLUSTER_UNAVAILABLE : "クラスター {0} は使用できなくなりました。", //clusterName
    STOP_FAILED_DURING_RESTART : "再始動中に、正しく停止を完了できませんでした。  発生したエラー: {0}", //errMsg
    ERROR_CLUSTER_OPERATION : "{0} クラスター {1} への試行中にエラーが発生しました。", //operation, clusterName
    SERVER_NONEXISTANT : "サーバー {0} が存在しません。", // serverName
    ERROR_SERVER_OPERATION : "ディレクトリー {3} のホスト {2} 上の {0} サーバー {1} への試行中にエラーが発生しました。", //operation, serverName, hostName, userdirName
    ERROR_SERVER_OPERATION_SET_MAINTENANCE_MODE : "エラーのため、ディレクトリー {2} 内のホスト {1} 上のサーバー {0} の保守モードが設定されませんでした。", //serverName, hostName, userdirName
    ERROR_SERVER_OPERATION_UNSET_MAINTENANCE_MODE : "ディレクトリー {2} 内のホスト{1} 上のサーバー {0} の保守モードを設定解除しようとしましたが、エラーのため完了しませんでした。", //serverName, hostName, userdirName
    ERROR_HOST_OPERATION_SET_MAINTENANCE_MODE : "エラーのため、ホスト {0} の保守モードが設定されませんでした。", // hostName
    ERROR_HOST_OPERATION_UNSET_MAINTENANCE_MODE : "ホスト {0} の保守モードを設定解除しようとしましたが、エラーのため完了しませんでした。", // hostName
    // Dialog messages on operation errors
    SERVER_START_ERROR: "サーバーの始動中にエラーが発生しました。",
    SERVER_START_CLEAN_ERROR: "server start --clean の実行中にエラーが発生しました。",
    SERVER_STOP_ERROR: "サーバーの停止中にエラーが発生しました。",
    SERVER_RESTART_ERROR: "サーバーの再始動中にエラーが発生しました。",
    // Used by standalone stop operation
    STANDALONE_STOP_NO_MBEAN : 'サーバーが停止しませんでした。サーバーの停止に必要な API が使用できませんでした。',
    STANDALONE_STOP_CANT_DETERMINE_MBEAN : 'サーバーが停止しませんでした。サーバーの停止に必要な API を判別できませんでした。',
    STANDALONE_STOP_FAILED : 'サーバー停止操作は正常に完了しませんでした。詳細については、サーバー・ログを確認してください。',
    STANDALONE_STOP_SUCCESS : 'サーバーは正常に停止しました。',
});
