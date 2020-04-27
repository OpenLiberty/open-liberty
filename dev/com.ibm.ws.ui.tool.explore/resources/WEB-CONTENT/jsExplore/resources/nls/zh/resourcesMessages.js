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
    ERROR : "错误",
    ERROR_STATUS : "{0} 错误：{1}", //responseStatusCode, response.text
    ERROR_URL_REQUEST_NO_STATUS : "请求 {0} 时发生错误。", // url
    ERROR_URL_REQUEST : "请求 {1} 时发生 {0} 错误。", //responseStatusCode, url
    ERROR_SERVER_UNAVAILABLE : "在规定的时间内服务器没有响应。",
    ERROR_APP_NOT_AVAILABLE : "对于服务器 {1}，主机 {2} 上目录 {3} 内的应用程序 {0} 不再可用。", //appName, serverName, hostName, userdirName
    ERROR_APPLICATION_OPERATION : "尝试在服务器 {2} 主机 {3} 目录 {4} 上 {0} 应用程序 {1} 时发生错误。", //operation, appName, serverName, hostName, userdirName
    CLUSTER_STATUS : "集群 {0} 是 {1}。", //clusterName, clusterStatus
    CLUSTER_UNAVAILABLE : "集群 {0} 不再可用。", //clusterName
    STOP_FAILED_DURING_RESTART : "重新启动期间未成功完成停止。错误为：{0}", //errMsg
    ERROR_CLUSTER_OPERATION : "尝试 {0} 集群 {1} 时发生错误。", //operation, clusterName
    SERVER_NONEXISTANT : "服务器 {0} 不存在。", // serverName
    ERROR_SERVER_OPERATION : "尝试在主机 {2} 目录 {3} {0} 服务器 {1} 时发生错误。", //operation, serverName, hostName, userdirName
    ERROR_SERVER_OPERATION_SET_MAINTENANCE_MODE : "由于出现错误，没有为目录 {2} 中主机 {1} 上的服务器 {0} 设置维护方式。", //serverName, hostName, userdirName
    ERROR_SERVER_OPERATION_UNSET_MAINTENANCE_MODE : "由于出现错误，为目录 {2} 中主机 {1} 上的服务器 {0} 取消设置维护方式的尝试未完成。", //serverName, hostName, userdirName
    ERROR_HOST_OPERATION_SET_MAINTENANCE_MODE : "由于出现错误，主机 {0} 的维护方式未设置。", // hostName
    ERROR_HOST_OPERATION_UNSET_MAINTENANCE_MODE : "由于出现错误，为主机 {0} 取消设置维护方式的尝试未完成。", // hostName
    // Dialog messages on operation errors
    SERVER_START_ERROR: "服务器启动期间发生错误",
    SERVER_START_CLEAN_ERROR: "服务器启动 --clean 期间发生错误。",
    SERVER_STOP_ERROR: "服务器停止期间发生错误",
    SERVER_RESTART_ERROR: "服务器重新启动期间发生错误",
    // Used by standalone stop operation
    STANDALONE_STOP_NO_MBEAN : '服务器未停止。停止服务器所需的 API 不可用。',
    STANDALONE_STOP_CANT_DETERMINE_MBEAN : '服务器未停止。无法确定停止服务器所需的 API。',
    STANDALONE_STOP_FAILED : '服务器停止操作未成功完成。请检查服务器日志以获取详细信息。',
    STANDALONE_STOP_SUCCESS : '服务器已成功停止。',
});

