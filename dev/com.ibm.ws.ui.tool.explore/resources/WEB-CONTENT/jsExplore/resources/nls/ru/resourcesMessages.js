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
    ERROR : "Ошибка",
    ERROR_STATUS : "Ошибка {0}: {1}", //responseStatusCode, response.text
    ERROR_URL_REQUEST_NO_STATUS : "Произошла ошибка при запросе {0}.", // url
    ERROR_URL_REQUEST : "Произошла ошибка {0} при запросе {1}.", //responseStatusCode, url
    ERROR_SERVER_UNAVAILABLE : "Сервер не ответил за указанное время.",
    ERROR_APP_NOT_AVAILABLE : "Приложение {0} больше не доступно для сервера {1} на хосте {2} в каталоге {3}.", //appName, serverName, hostName, userdirName
    ERROR_APPLICATION_OPERATION : "Произошла ошибка при попытке {0} приложение {1} на сервере {2} на хосте {3} в каталоге {4}.", //operation, appName, serverName, hostName, userdirName
    CLUSTER_STATUS : "Кластер {0} находится в состоянии {1}.", //clusterName, clusterStatus
    CLUSTER_UNAVAILABLE : "Кластер {0} больше не доступен.", //clusterName
    STOP_FAILED_DURING_RESTART : "Остановка не завершена успешно в процессе перезапуска.  Ошибка: {0}", //errMsg
    ERROR_CLUSTER_OPERATION : "Произошла ошибка при попытке {0} кластер {1}.", //operation, clusterName
    SERVER_NONEXISTANT : "Сервер {0} не существует.", // serverName
    ERROR_SERVER_OPERATION : "Произошла ошибка при попытке {0} сервер {1} на хосте {2} в каталоге {3}.", //operation, serverName, hostName, userdirName
    ERROR_SERVER_OPERATION_SET_MAINTENANCE_MODE : "Режим обслуживания для сервера {0} на хосте {1} в каталоге {2} не задан из-за ошибки.", //serverName, hostName, userdirName
    ERROR_SERVER_OPERATION_UNSET_MAINTENANCE_MODE : "Попытка отменить задание режима обслуживания для сервера {0} на хосте {1} в каталоге {2} не выполнена из-за ошибки.", //serverName, hostName, userdirName
    ERROR_HOST_OPERATION_SET_MAINTENANCE_MODE : "Режим обслуживания для хоста {0} не задан из-за ошибки.", // hostName
    ERROR_HOST_OPERATION_UNSET_MAINTENANCE_MODE : "Попытка отмены задания режима обслуживания для хоста {0} не выполнена из-за ошибки.", // hostName
    // Dialog messages on operation errors
    SERVER_START_ERROR: "Произошла ошибка при запуске сервера.",
    SERVER_START_CLEAN_ERROR: "Произошла ошибка при запуске сервера с параметром --clean.",
    SERVER_STOP_ERROR: "Произошла ошибка при остановке сервера.",
    SERVER_RESTART_ERROR: "Произошла ошибка в ходе перезапуска сервера.",
    // Used by standalone stop operation
    STANDALONE_STOP_NO_MBEAN : 'Сервер не остановлен. Нет доступа к API, необходимого для остановки сервера.',
    STANDALONE_STOP_CANT_DETERMINE_MBEAN : 'Сервер не остановлен. Не удалось определить API, необходимый для остановки сервера.',
    STANDALONE_STOP_FAILED : 'Операция остановки сервера не была выполнена. См. протоколы сервера.',
    STANDALONE_STOP_SUCCESS : 'Сервер успешно остановлен.',
});

