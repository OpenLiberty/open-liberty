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
    ERROR : "错误",
    ERROR_STATUS : "{0} 错误：{1}", //responseStatusCode, response.text
    ERROR_URL_REQUEST_NO_STATUS : "在请求 {0} 时发生错误。", // url
    ERROR_URL_REQUEST : "{0} 在请求 {1} 时发生错误。", //responseStatusCode, url
    ERROR_SERVER_UNAVAILABLE : "服务器未在所分配时间内响应。",
    ERROR_APP_NOT_AVAILABLE : "对于主机 {2} 上目录 {3} 中的服务器 {1}，应用程序 {0} 不再可用。", //appName, serverName, hostName, userdirName
    ERROR_APPLICATION_OPERATION : "尝试对主机 {3} 上目录 {4} 中的服务器 {2} 上应用程序 {1} 执行操作 {0} 时发生错误。", //operation, appName, serverName, hostName, userdirName
    CLUSTER_STATUS : "集群 {0} 为 {1}。", //clusterName, clusterStatus
    CLUSTER_UNAVAILABLE : "集群 {0} 不再可用。", //clusterName
    STOP_FAILED_DURING_RESTART : "在重新启动期间，停止未成功完成。错误为：{0}", //errMsg
    ERROR_CLUSTER_OPERATION : "尝试对集群 {1} 执行操作 {0} 时发生错误。", //operation, clusterName
    SERVER_NONEXISTANT : "服务器 {0} 不存在。", // serverName
    ERROR_SERVER_OPERATION : "尝试对主机 {2} 上目录 {3} 中的服务器 {1} 执行操作 {0} 时发生错误。", //operation, serverName, hostName, userdirName
    ERROR_SERVER_OPERATION_SET_MAINTENANCE_MODE : "由于出现错误，没有为目录 {2} 中主机 {1} 上的服务器 {0} 设置维护方式。", //serverName, hostName, userdirName
    ERROR_SERVER_OPERATION_UNSET_MAINTENANCE_MODE : "由于出现错误，尝试为目录 {2} 中主机 {1} 上的服务器 {0} 取消设置维护方式未完成。", //serverName, hostName, userdirName
    ERROR_HOST_OPERATION_SET_MAINTENANCE_MODE : "由于出现错误，未设置主机 {0} 的维护方式。", // hostName
    ERROR_HOST_OPERATION_UNSET_MAINTENANCE_MODE : "由于出现错误，尝试为主机 {0} 取消设置维护方式未完成。", // hostName
    // Dialog messages on operation errors
    SERVER_START_ERROR: "服务器启动期间发生错误",
    SERVER_START_CLEAN_ERROR: "服务器启动 --clean 期间发生错误。",
    SERVER_STOP_ERROR: "服务器停止期间发生错误",
    SERVER_RESTART_ERROR: "服务器重新启动期间发生错误",
    // Used by standalone stop operation
    STANDALONE_STOP_NO_MBEAN : '服务器未停止。停止服务器所需要的 API 不可用。',
    STANDALONE_STOP_CANT_DETERMINE_MBEAN : '服务器未停止。未能确定停止服务器所需要的 API。',
    STANDALONE_STOP_FAILED : '未成功完成服务器停止操作。请查看服务器日志以了解详细信息。',
    STANDALONE_STOP_SUCCESS : '服务器已成功停止。',
});
