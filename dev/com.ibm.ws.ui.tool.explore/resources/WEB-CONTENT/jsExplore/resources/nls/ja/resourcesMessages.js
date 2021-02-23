/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
define({
    ERROR : "エラー",
    ERROR_STATUS : "{0} エラー: {1}", //responseStatusCode, response.text
    ERROR_URL_REQUEST_NO_STATUS : "{0} の要求中にエラーが発生しました。", // url
    ERROR_URL_REQUEST : "{1} の要求中に {0} エラーが発生しました。", //responseStatusCode, url
    ERROR_SERVER_UNAVAILABLE : "サーバーが指定時間内に応答しませんでした。",
    ERROR_APP_NOT_AVAILABLE : "アプリケーション {0} はディレクトリー {3} にあるホスト {2} 上のサーバー {1} で使用できなくなりました。", //appName, serverName, hostName, userdirName
    ERROR_APPLICATION_OPERATION : "ディレクトリー {4} にあるホスト {3} 上のサーバー {2} のアプリケーション {1} を {0} しようとした際にエラーが発生しました。", //operation, appName, serverName, hostName, userdirName
    CLUSTER_STATUS : "クラスター {0} は {1} です。", //clusterName, clusterStatus
    CLUSTER_UNAVAILABLE : "クラスター {0} 使用できなくなりました。", //clusterName
    STOP_FAILED_DURING_RESTART : "再始動時に停止を正常に完了できませんでした。  発生したエラー: {0}", //errMsg
    ERROR_CLUSTER_OPERATION : "クラスター {1} を {0} しようとした際にエラーが発生しました。", //operation, clusterName
    SERVER_NONEXISTANT : "サーバー {0} は存在しません。", // serverName
    ERROR_SERVER_OPERATION : "ディレクトリー {3} にあるホスト {2} 上のサーバー {1} を {0} しようとした際にエラーが発生しました。", //operation, serverName, hostName, userdirName
    ERROR_SERVER_OPERATION_SET_MAINTENANCE_MODE : "エラーのため、ディレクトリー {2} 内のホスト {1} 上のサーバー {0} の保守モードが設定されませんでした。", //serverName, hostName, userdirName
    ERROR_SERVER_OPERATION_UNSET_MAINTENANCE_MODE : "ディレクトリー {2} 内のホスト{1} 上のサーバー {0} の保守モードを設定解除しようとしましたが、エラーのため完了しませんでした。", //serverName, hostName, userdirName
    ERROR_HOST_OPERATION_SET_MAINTENANCE_MODE : "エラーのため、ホスト {0} の保守モードが設定されませんでした。", // hostName
    ERROR_HOST_OPERATION_UNSET_MAINTENANCE_MODE : "ホスト {0} の保守モードを設定解除しようとしましたが、エラーのため完了しませんでした。", // hostName
    // Dialog messages on operation errors
    SERVER_START_ERROR: "サーバー開始中のエラー。",
    SERVER_START_CLEAN_ERROR: "--clean を指定してサーバー開始中のエラー。",
    SERVER_STOP_ERROR: "サーバー停止中のエラー。",
    SERVER_RESTART_ERROR: "サーバー再始動中のエラー。",
    // Used by standalone stop operation
    STANDALONE_STOP_NO_MBEAN : 'サーバーが停止しませんでした。 サーバーの停止に必要な API が使用できませんでした。',
    STANDALONE_STOP_CANT_DETERMINE_MBEAN : 'サーバーが停止しませんでした。 サーバーの停止に必要な API を判別できませんでした。',
    STANDALONE_STOP_FAILED : 'サーバー停止操作は正常に完了しませんでした。 詳しくは、サーバー・ログをチェックしてください。',
    STANDALONE_STOP_SUCCESS : 'サーバーは正常に停止しました。',
});

