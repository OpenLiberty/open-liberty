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
    ERROR : "錯誤",
    ERROR_STATUS : "{0} 錯誤：{1}", //responseStatusCode, response.text
    ERROR_URL_REQUEST_NO_STATUS : "要求 {0} 時發生錯誤。", // url
    ERROR_URL_REQUEST : "要求 {1} 時發生 {0} 錯誤。", //responseStatusCode, url
    ERROR_SERVER_UNAVAILABLE : "伺服器未在規定的時間內回應。",
    ERROR_APP_NOT_AVAILABLE : "對 {3} 目錄中之主機 {2} 上的伺服器 {1} 而言，應用程式 {0} 不再是可用的。", //appName, serverName, hostName, userdirName
    ERROR_APPLICATION_OPERATION : "嘗試對應用程式 {1} 執行 {0} 時發生錯誤，該應用程式位於 {4} 目錄中之主機 {3} 的伺服器 {2} 上。", //operation, appName, serverName, hostName, userdirName
    CLUSTER_STATUS : "叢集 {0} 是 {1}。", //clusterName, clusterStatus
    CLUSTER_UNAVAILABLE : "叢集 {0} 不再是可用的。", //clusterName
    STOP_FAILED_DURING_RESTART : "在重新啟動期間，未能順利完成停止。錯誤是：{0}", //errMsg
    ERROR_CLUSTER_OPERATION : "嘗試對叢集 {1} 執行 {0} 時發生錯誤。", //operation, clusterName
    SERVER_NONEXISTANT : "伺服器 {0} 不存在。", // serverName
    ERROR_SERVER_OPERATION : "嘗試對目錄 {3} 中之主機 {2} 上的伺服器 {1} 執行 {0} 時發生錯誤。", //operation, serverName, hostName, userdirName
    ERROR_SERVER_OPERATION_SET_MAINTENANCE_MODE : "由於發生錯誤，未設定目錄 {2} 中主機 {1} 上伺服器 {0} 的維護模式。", //serverName, hostName, userdirName
    ERROR_SERVER_OPERATION_UNSET_MAINTENANCE_MODE : "由於發生錯誤，未完成嘗試取消設定目錄 {2} 中主機 {1} 上伺服器 {0} 的維護模式。", //serverName, hostName, userdirName
    ERROR_HOST_OPERATION_SET_MAINTENANCE_MODE : "由於發生錯誤，未設定主機 {0} 的維護模式。", // hostName
    ERROR_HOST_OPERATION_UNSET_MAINTENANCE_MODE : "由於發生錯誤，未完成嘗試取消設定主機 {0} 的維護模式。", // hostName
    // Dialog messages on operation errors
    SERVER_START_ERROR: "在伺服器啟動期間發生錯誤。",
    SERVER_START_CLEAN_ERROR: "伺服器啟動 --clean 期間發生錯誤。",
    SERVER_STOP_ERROR: "在伺服器停止期間發生錯誤。",
    SERVER_RESTART_ERROR: "在伺服器重新啟動期間發生錯誤。",
    // Used by standalone stop operation
    STANDALONE_STOP_NO_MBEAN : '伺服器未停止。停止伺服器所需的 API 無法使用。',
    STANDALONE_STOP_CANT_DETERMINE_MBEAN : '伺服器未停止。無法判斷停止伺服器所需的 API。',
    STANDALONE_STOP_FAILED : '伺服器停止作業未順利完成。請檢查伺服器日誌，以取得詳細資料。',
    STANDALONE_STOP_SUCCESS : '伺服器已順利停止。',
});

