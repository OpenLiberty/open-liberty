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
    ERROR : "오류",
    ERROR_STATUS : "{0} 오류: {1}", //responseStatusCode, response.text
    ERROR_URL_REQUEST_NO_STATUS : "{0} 요청 중에 오류가 발생했습니다.", // url
    ERROR_URL_REQUEST : "{1} 요청 중에 {0} 오류가 발생했습니다.", //responseStatusCode, url
    ERROR_SERVER_UNAVAILABLE : "할당된 시간에 서버가 응답하지 않았습니다.",
    ERROR_APP_NOT_AVAILABLE : "{0} 애플리케이션을 {3} 디렉토리에 있는 {2} 호스트의 {1} 서버에 대해 더 이상 사용할 수 없습니다. ", //appName, serverName, hostName, userdirName
    ERROR_APPLICATION_OPERATION : "{4} 디렉토리에 있는 {3} 호스트의 {2} 서버에서 {1} 애플리케이션을 {0}하려고 시도하는 중에 오류가 발생했습니다.", //operation, appName, serverName, hostName, userdirName
    CLUSTER_STATUS : "{0} 클러스터가 {1}입니다.", //clusterName, clusterStatus
    CLUSTER_UNAVAILABLE : "{0} 클러스터를 더 이상 사용할 수 없습니다.", //clusterName
    STOP_FAILED_DURING_RESTART : "다시 시작 중에 중지가 완료되지 않았습니다. 오류는 다음과 같습니다. {0}", //errMsg
    ERROR_CLUSTER_OPERATION : "{1} 클러스터 {0} 중에 오류가 발생했습니다.", //operation, clusterName
    SERVER_NONEXISTANT : "{0} 서버가 없습니다.", // serverName
    ERROR_SERVER_OPERATION : "{3} 디렉토리의 {2} 호스트에서 {1} 서버를 {0}하려고 시도하는 중에 오류가 발생했습니다.", //operation, serverName, hostName, userdirName
    ERROR_SERVER_OPERATION_SET_MAINTENANCE_MODE : "오류로 인해 디렉토리 {2}의 호스트 {1}에 있는 {0} 서버의 유지보수 모드가 설정되지 않았습니다.", //serverName, hostName, userdirName
    ERROR_SERVER_OPERATION_UNSET_MAINTENANCE_MODE : "오류로 인해 디렉토리 {2}의 호스트 {1}에 있는 {0} 서버의 유지보수 모드 설정 해제가 완료되지 않았습니다.", //serverName, hostName, userdirName
    ERROR_HOST_OPERATION_SET_MAINTENANCE_MODE : "오류로 인해 호스트 {0}의 유지보수 모드가 설정되지 않았습니다.", // hostName
    ERROR_HOST_OPERATION_UNSET_MAINTENANCE_MODE : "오류로 인해 호스트 {0}의 유지보수 모드 설정 해제가 완료되지 않았습니다.", // hostName
    // Dialog messages on operation errors
    SERVER_START_ERROR: "서버 시작 중 오류가 발생했습니다.",
    SERVER_START_CLEAN_ERROR: "서버 시작 --clean 중 오류가 발생했습니다.",
    SERVER_STOP_ERROR: "서버 중지 중 오류가 발생했습니다.",
    SERVER_RESTART_ERROR: "서버 재시작 중 오류가 발생했습니다.",
    // Used by standalone stop operation
    STANDALONE_STOP_NO_MBEAN : '서버가 중지되지 않았습니다. 서버를 중지하는 데 필요한 API를 사용할 수 없습니다.',
    STANDALONE_STOP_CANT_DETERMINE_MBEAN : '서버가 중지되지 않았습니다. 서버를 중지하는 데 필요한 API를 판별할 수 없습니다.',
    STANDALONE_STOP_FAILED : '서버 중지 조작이 완료되지 않았습니다. 세부사항은 서버 로그를 확인하십시오.',
    STANDALONE_STOP_SUCCESS : '서버가 중지되었습니다.',
});

