/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
define({
    EXPLORER : "Проводник",
    EXPLORE : "Обзор",
    DASHBOARD : "Сводная панель",
    DASHBOARD_VIEW_ALL_APPS : "Показать все приложения",
    DASHBOARD_VIEW_ALL_SERVERS : "Показать все серверы",
    DASHBOARD_VIEW_ALL_CLUSTERS : "Показать все кластеры",
    DASHBOARD_VIEW_ALL_HOSTS : "Показать все хосты",
    DASHBOARD_VIEW_ALL_RUNTIMES : "Показать все среды выполнения",
    SEARCH : "Поиск",
    SEARCH_RECENT : "Последние операции поиска",
    SEARCH_RESOURCES : "Ресурсы поиска",
    SEARCH_RESULTS : "Результаты поиска",
    SEARCH_NO_RESULTS : "Нет результатов",
    SEARCH_NO_MATCHES : "Нет совпадений",
    SEARCH_TEXT_INVALID : "Текст для поиска содержит недопустимые символы",
    SEARCH_CRITERIA_INVALID : "Критерии поиска недопустимы.",
    SEARCH_CRITERIA_INVALID_COMBO :"{0} нельзя указывать вместе с {1}.",
    SEARCH_CRITERIA_INVALID_DUPLICATE : "Укажите {0} только один раз.",
    SEARCH_TEXT_MISSING : "Необходимо указать текст для поиска",
    SEARCH_UNSUPPORT_TYPE_APPONSERVER : "Поиск тегов приложений на сервере не поддерживается.",
    SEARCH_UNSUPPORT_TYPE_APPONCLUSTER : "Поиск тегов приложений в кластере не поддерживается.",
    SEARCH_UNSUPPORT : "Критерии поиска не поддерживаются.",
    SEARCH_SWITCH_VIEW : "Переключить представление",
    FILTERS : "Фильтры",
    DEPLOY_SERVER_PACKAGE : "Развернуть пакет сервера",
    MEMBER_OF : "Элемент",
    N_CLUSTERS: "Кластеров: {0} ...",

    INSTANCE : "Экземпляр",
    INSTANCES : "Экземпляры",
    APPLICATION : "Приложение",
    APPLICATIONS : "Приложения",
    SERVER : "Сервер",
    SERVERS : "Серверы",
    CLUSTER : "Кластер",
    CLUSTERS : "Кластеры",
    CLUSTER_NAME : "Имя кластера: ",
    CLUSTER_STATUS : "Состояние кластера: ",
    APPLICATION_NAME : "Имя приложения: ",
    APPLICATION_STATE : "Состояние приложения: ",
    HOST : "Хост",
    HOSTS : "Хосты",
    RUNTIME : "Среда выполнения",
    RUNTIMES : "Среды выполнения",
    PATH : "Путь",
    CONTROLLER : "Контроллер",
    CONTROLLERS : "Контроллеры",
    OVERVIEW : "Обзор",
    CONFIGURE : "Настроить",

    SEARCH_RESOURCE_TYPE: "Тип", // Search by resource types
    SEARCH_RESOURCE_STATE: "Состояние", // Search by resource types
    SEARCH_RESOURCE_TYPE_ALL: "Все", // Search all resource types
    SEARCH_RESOURCE_NAME: "Имя", // Search by resource name
    SEARCH_RESOURCE_TAG: "Тег", // Search by resource tag
    SEARCH_RESOURCE_CONTAINER: "Контейнер", // Search by container type
    SEARCH_RESOURCE_CONTAINER_DOCKER: "Docker", // Search by container type Docker
    SEARCH_RESOURCE_CONTAINER_NONE: "Нет", // Search by container type none
    SEARCH_RESOURCE_RUNTIMETYPE: "Тип среды выполнения", // Search by runtime type
    SEARCH_RESOURCE_OWNER: "Владелец", // Search by owner
    SEARCH_RESOURCE_CONTACT: "Контакт", // Search by contact
    SEARCH_RESOURCE_NOTE: "Примечание", // Search by note

    GRID_HEADER_USERDIR : "Пользовательский каталог",
    GRID_HEADER_NAME : "Имя",
    GRID_LOCATION_NAME : "Расположение",
    GRID_ACTIONS : "Действия Grid",
    GRID_ACTIONS_LABEL : "{0} действий grid",  // name of the grid
    APPONSERVER_LOCATION_NAME : "{0} на {1} ({2})", // server on host (/path)

    STATS : "Монитор",
    STATS_ALL : "Все",
    STATS_VALUE : "Значение: {0}",
    CONNECTION_IN_USE_STATS : "{0} используется = {1} управляется - {2} свободно",
    CONNECTION_IN_USE_STATS_VALUE : "Значение: {0} используется = {1} управляется - {2} свободно",
    DATA_SOURCE : "Источник данных: {0}",
    STATS_DISPLAY_LEGEND : "Показать условные обозначения",
    STATS_HIDE_LEGEND : "Скрыть условные обозначения",
    STATS_VIEW_DATA : "Показать данные диаграммы",
    STATS_VIEW_DATA_TIMESTAMP : "Системное время",
    STATS_ACTION_MENU : "Меню действия {0}",
    STATS_SHOW_HIDE : "Добавить показатели ресурсов",
    STATS_SHOW_HIDE_SUMMARY : "Добавить показатели для сводки",
    STATS_SHOW_HIDE_TRAFFIC : "Добавить показатели потока данных",
    STATS_SHOW_HIDE_PERFORMANCE : "Добавить показатели производительности",
    STATS_SHOW_HIDE_AVAILABILITY : "Добавить показатели готовности",
    STATS_SHOW_HIDE_ALERT : "Добавить показатели для предупреждения",
    STATS_SHOW_HIDE_LIST_BUTTON : "Показать или скрыть список показателей ресурсов",
    STATS_SHOW_HIDE_BUTTON_TITLE : "Редактировать диаграммы",
    STATS_SHOW_HIDE_CONFIRM : "Сохранить",
    STATS_SHOW_HIDE_CANCEL : "Отмена",
    STATS_SHOW_HIDE_DONE : "Готово",
    STATS_DELETE_GRAPH : "Удалить диаграмму",
    STATS_ADD_CHART_LABEL : "Добавить диаграмму на панель",
    STATS_JVM_TITLE : "JVM",
    STATS_JVM_BUTTON_LABEL : "Добавить все диаграммы JVM на панель",
    STATS_HEAP_TITLE : "Использование памяти кучи",
    STATS_HEAP_USED : "Использовано: {0} МБ",
    STATS_HEAP_COMMITTED : "Фиксировано: {0} МБ",
    STATS_HEAP_MAX : "Макс.: {0} MB",
    STATS_HEAP_X_TIME : "Время",
    STATS_HEAP_Y_MB : "Использовано МБ",
    STATS_HEAP_Y_MB_LABEL : "{0} МБ",
    STATS_CLASSES_TITLE : "Загруженные классы",
    STATS_CLASSES_LOADED : "Загружено: {0}",
    STATS_CLASSES_UNLOADED : "Выгружено: {0}",
    STATS_CLASSES_TOTAL : "Всего: {0}",
    STATS_CLASSES_Y_TOTAL : "Загруженные классы",
    STATS_PROCESSCPU_TITLE : "Использование CPU",
    STATS_PROCESSCPU_USAGE : "Использование CPU: {0}%",
    STATS_PROCESSCPU_Y_PERCENT : "Процент CPU",
    STATS_PROCESSCPU_Y_PCT_LABEL : "{0}%",
    STATS_THREADS_TITLE : "Активные нити JVM",
    STATS_LIVE_MSG_INIT : "Показаны оперативные данные",
    STATS_LIVE_MSG :"Для этой диаграммы нет хронологических данных. Будут показаны данные за последние 10 минут.",
    STATS_THREADS_ACTIVE : "Текущее значение: {0}",
    STATS_THREADS_PEAK : "Пиковое значение: {0}",
    STATS_THREADS_TOTAL : "Всего: {0}",
    STATS_THREADS_Y_THREADS : "Нити",
    STATS_TP_POOL_SIZE : "Размер пула",
    STATS_JAXWS_TITLE : "Веб-службы JAX-WS",
    STATS_JAXWS_BUTTON_LABEL : "Добавить все диаграммы веб-служб JAX-WS на панель",
    STATS_JW_AVG_RESP_TIME : "Среднее время ответа",
    STATS_JW_AVG_INVCOUNT : "Среднее число вызовов",
    STATS_JW_TOTAL_FAULTS : "Общее число сбоев во время выполнения",
    STATS_LA_RESOURCE_CONFIG_LABEL : "Выбрать ресурсы...",
    STATS_LA_RESOURCE_CONFIG_LABEL_NUM : "{0} ресурсов",
    STATS_LA_RESOURCE_CONFIG_LABEL_NUM_1 : "1 ресурс",
    STATS_LA_RESOURCE_CONFIG_SELECT_ONE : "Необходимо выбрать хотя бы один ресурс.",
    STATS_LA_RESOURCE_CONFIG_NO_DATA : "Нет доступных данных для выбранного интервала времени.",
    STATS_ACCESS_LOG_TITLE : "Протокол доступа",
    STATS_ACCESS_LOG_BUTTON_LABEL : "Добавить все диаграммы Протокол доступа на панель",
    STATS_ACCESS_LOG_GRAPH : "Число сообщений в протоколе доступа",
    STATS_ACCESS_LOG_SUMMARY : "Сводка по протоколу доступа",
    STATS_ACCESS_LOG_TABLE : "Список сообщений в протоколе доступа",
    STATS_MESSAGES_TITLE : "Сообщения и трассировка",
    STATS_MESSAGES_BUTTON_LABEL : "Добавить все диаграммы Сообщения и трассировка на панель",
    STATS_MESSAGES_GRAPH : "Число сообщений в протоколе",
    STATS_MESSAGES_TABLE : "Список сообщений в протоколе",
    STATS_FFDC_GRAPH : "Число FFDC",
    STATS_FFDC_TABLE : "Список FFDC",
    STATS_TRACE_LOG_GRAPH : "Число сообщений трассировки",
    STATS_TRACE_LOG_TABLE : "Список сообщений трассировки",
    STATS_THREAD_POOL_TITLE : "Пул нитей",
    STATS_THREAD_POOL_BUTTON_LABEL : "Добавить все диаграммы Пул нитей на панель",
    STATS_THREADPOOL_TITLE : "Активные нити Liberty",
    STATS_THREADPOOL_SIZE : "Размер пула: {0}",
    STATS_THREADPOOL_ACTIVE : "Активно: {0}",
    STATS_THREADPOOL_TOTAL : "Всего: {0}",
    STATS_THREADPOOL_Y_ACTIVE : "Активные нити",
    STATS_SESSION_MGMT_TITLE : "Сеансы",
    STATS_SESSION_MGMT_BUTTON_LABEL : "Добавить все диаграммы Сеансы на панель",
    STATS_SESSION_CONFIG_LABEL : "Выбрать сеансы...",
    STATS_SESSION_CONFIG_LABEL_NUM  : "{0} сеансов",
    STATS_SESSION_CONFIG_LABEL_NUM_1  : "1 сеанс",
    STATS_SESSION_CONFIG_SELECT_ONE : "Вы должны выбрать хотя бы один сеанс.",
    STATS_SESSION_TITLE : "Активные сеансы",
    STATS_SESSION_Y_ACTIVE : "Активные сеансы",
    STATS_SESSION_LIVE_LABEL : "Число текущих: {0}",
    STATS_SESSION_CREATE_LABEL : "Число созданных: {0}",
    STATS_SESSION_INV_LABEL : "Число аннулированных: {0}",
    STATS_SESSION_INV_TIME_LABEL : "Число аннулированных по тайм-ауту: {0}",
    STATS_WEBCONTAINER_TITLE : "Веб-приложения",
    STATS_WEBCONTAINER_BUTTON_LABEL : "Добавить все диаграммы Веб-приложения на панель",
    STATS_SERVLET_CONFIG_LABEL : "Выбрать сервлеты...",
    STATS_SERVLET_CONFIG_LABEL_NUM : "{0} сервлетов",
    STATS_SERVLET_CONFIG_LABEL_NUM_1 : "1 сервлет",
    STATS_SERVLET_CONFIG_SELECT_ONE : "Вы должны выбрать хотя бы один сервлет.",
    STATS_SERVLET_REQUEST_COUNT_TITLE : "Число запросов",
    STATS_SERVLET_REQUEST_COUNT_Y_AXIS : "Число запросов",
    STATS_SERVLET_RESPONSE_COUNT_TITLE : "Число ответов",
    STATS_SERVLET_RESPONSE_COUNT_Y_AXIS : "Число ответов",
    STATS_SERVLET_RESPONSE_MEAN_TITLE : "Среднее время ответа (нс)",
    STATS_SERVLET_RESPONSE_MEAN_Y_AXIS : "Время ответа (нс)",
    STATS_CONN_POOL_TITLE : "Пул соединений",
    STATS_CONN_POOL_BUTTON_LABEL : "Добавить все диаграммы Пул соединений на панель",
    STATS_CONN_POOL_CONFIG_LABEL : "Выбрать источники данных...",
    STATS_CONN_POOL_CONFIG_LABEL_NUM : "{0} источников данных",
    STATS_CONN_POOL_CONFIG_LABEL_NUM_1 : "1 источник данных",
    STATS_CONN_POOL_CONFIG_SELECT_ONE : "Вы должны выбрать хотя бы один источник данных.",
    STATS_CONNECT_IN_USE_TITLE : "Используемые соединения",
    STATS_CONNECT_USED_COUNT_Y_AXIS : "Соединения",
    STATS_CONNECT_IN_USE_LABEL : "Используется: {0}",
    STATS_CONNECT_USED_USED_LABEL : "Использовано: {0}",
    STATS_CONNECT_USED_FREE_LABEL : "Свободно: {0}",
    STATS_CONNECT_USED_CREATE_LABEL : "Создано: {0}",
    STATS_CONNECT_USED_DESTROY_LABEL : "Закрыто: {0}",
    STATS_CONNECT_WAIT_TIME_TITLE : "Среднее время ожидания (мс)",
    STATS_CONNECT_WAIT_TIME_Y_AXIS : "Время ожидания (мс)",
    STATS_TIME_ALL : "Все",
    STATS_TIME_1YEAR : "1 г.",
    STATS_TIME_1MONTH : "1 мес.",
    STATS_TIME_1WEEK : "1 нед.",
    STATS_TIME_1DAY : "1 день",
    STATS_TIME_1HOUR : "1 ч",
    STATS_TIME_10MINUTES : "10 мин",
    STATS_TIME_5MINUTES : "5 мин",
    STATS_TIME_1MINUTE : "1 мин",
    STATS_PERSPECTIVE_SUMMARY : "Сводка",
    STATS_PERSPECTIVE_TRAFFIC : "Поток данных",
    STATS_PERSPECTIVE_TRAFFIC_JVM : "Поток данных JVM",
    STATS_PERSPECTIVE_TRAFFIC_CONN : "Поток данных соединений",
    STATS_PERSPECTIVE_TRAFFIC_LAACCESS : "Поток данных в протоколе доступа",
    STATS_PERSPECTIVE_PROBLEM : "Неполадка",
    STATS_PERSPECTIVE_PERFORMANCE : "Производительность",
    STATS_PERSPECTIVE_PERFORMANCE_JVM : "Производительность JVM",
    STATS_PERSPECTIVE_PERFORMANCE_CONN : "Производительность соединений",
    STATS_PERSPECTIVE_ALERT : "Анализ предупреждений",
    STATS_PERSPECTIVE_ALERT_LAACCESS : "Предупреждение в протоколе доступа",
    STATS_PERSPECTIVE_ALERT_LAMSGS : "Предупреждение в протоколе сообщений и трассировки",
    STATS_PERSPECTIVE_AVAILABILITY : "Готовность",

    STATS_DISPLAY_TIME_LAST_MINUTE_LABEL : "За последнюю минуту",
    STATS_DISPLAY_TIME_LAST_5_MINUTES_LABEL : "За последние 5 мин",
    STATS_DISPLAY_TIME_LAST_10_MINUTES_LABEL : "За последние 10 мин",
    STATS_DISPLAY_TIME_LAST_HOUR_LABEL : "За последний час",
    STATS_DISPLAY_TIME_LAST_DAY_LABEL : "За последний день",
    STATS_DISPLAY_TIME_LAST_WEEK_LABEL : "За последнюю неделю",
    STATS_DISPLAY_TIME_LAST_MONTH_LABEL : "За последний месяц",
    STATS_DISPLAY_TIME_LAST_YEAR_LABEL : "За последний год",

    STATS_DISPLAY_CUSTOM_TIME_LAST_SECOND_LABEL : "За последние {0} с",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MINUTE_LABEL : "За последние {0} мин",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MINUTE_AND_SECOND_LABEL : "За последние {0} мин {1} с",
    STATS_DISPLAY_CUSTOM_TIME_LAST_HOUR_LABEL : "За последние {0} ч",
    STATS_DISPLAY_CUSTOM_TIME_LAST_HOUR_AND_MINUTE_LABEL : "За последние {0} ч {1} м",
    STATS_DISPLAY_CUSTOM_TIME_LAST_DAY_LABEL : "За последние {0} дн.",
    STATS_DISPLAY_CUSTOM_TIME_LAST_DAY_AND_HOUR_LABEL : "За последние {0} дн. {1} ч",
    STATS_DISPLAY_CUSTOM_TIME_LAST_WEEK_LABEL : "За последние {0} нед.",
    STATS_DISPLAY_CUSTOM_TIME_LAST_WEEK_AND_DAY_LABEL : "За последние {0} нед. {1} дн.",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MONTH_LABEL : "За последние {0} мес.",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MONTH_AND_DAY_LABEL : "За последние {0} мес. {1} дн.",
    STATS_DISPLAY_CUSTOM_TIME_LAST_YEAR_LABEL : "За последние {0} лет",
    STATS_DISPLAY_CUSTOM_TIME_LAST_YEAR_AND_MONTH_LABEL : "За последние {0} лет {1} мес.",

    STATS_LIVE_UPDATE_LABEL: "Оперативное обновление",
    STATS_TIME_SELECTOR_NOW_LABEL: "Сейчас",

    LOG_ANALYTICS_LOG_MESSAGE_TITLE: "Сообщения протокола",

    AUTOSCALED_APPLICATION : "Автоматически масштабируемое приложение",
    AUTOSCALED_SERVER : "Автоматически масштабируемый сервер",
    AUTOSCALED_CLUSTER : "Автоматически масштабируемый кластер",
    AUTOSCALED_POLICY : "Стратегия автоматического масштабирования",
    AUTOSCALED_POLICY_DISABLED : "Стратегия автоматического масштабирования отключена",
    AUTOSCALED_NOACTIONS : "Действия недоступны для ресурсов с автоматическим масштабированием",

    START : "Запустить",
    START_CLEAN : "Запустить с параметром --clean",
    STARTING : "Запускается",
    STARTED : "Запущено",
    RUNNING : "Выполняется",
    NUM_RUNNING: "Выполняется: {0}",
    PARTIALLY_STARTED : "Частично запущено",
    PARTIALLY_RUNNING : "Частично работает",
    NOT_STARTED : "Не запущено",
    STOP : "Остановить",
    STOPPING : "Останавливается",
    STOPPED : "Остановлено",
    NUM_STOPPED : "Остановлено: {0}",
    NOT_RUNNING : "Не выполняется",
    RESTART : "Перезапустить",
    RESTARTING : "Перезапуск",
    RESTARTED : "Перезапущено",
    ALERT : "Предупреждение",
    ALERTS : "Предупреждения",
    UNKNOWN : "Неизвестно",
    NUM_UNKNOWN : "Неизвестно: {0}",
    SELECT : "Выбрать",
    SELECTED : "Выбрано",
    SELECT_ALL : "Выбрать все",
    SELECT_NONE : "Отменить выбор всех",
    DESELECT: "Отменить выбор",
    DESELECT_ALL : "Отменить выбор всех",
    TOTAL : "Всего",
    UTILIZATION : "Свыше {0}% использования", // percent

    ELLIPSIS_ARIA: "Развернуть для доступа к дополнительным опциям.",
    EXPAND : "Развернуть",
    COLLAPSE: "Свернуть",

    ALL : "Все",
    ALL_APPS : "Все приложения",
    ALL_SERVERS : "Все серверы",
    ALL_CLUSTERS : "Все кластеры",
    ALL_HOSTS : "Все хосты",
    ALL_APP_INSTANCES : "Все экземпляры приложений",
    ALL_RUNTIMES : "Все среды выполнения",

    ALL_APPS_RUNNING : "Выполняются все приложения",
    ALL_SERVER_RUNNING : "Работают все серверы",
    ALL_CLUSTERS_RUNNING : "Работают все кластеры",
    ALL_APPS_STOPPED : "Все приложения остановлены",
    ALL_SERVER_STOPPED : "Все серверы остановлены",
    ALL_CLUSTERS_STOPPED : "Все кластеры остановлены",
    ALL_SERVERS_UNKNOWN : "Состояние всех серверов неизвестно",
    SOME_APPS_RUNNING : "Выполняются некоторые приложения",
    SOME_SERVERS_RUNNING : "Работают некоторые серверы",
    SOME_CLUSTERS_RUNNING : "Работают некоторые кластеры",
    NO_APPS_RUNNING : "Ни одно приложение не выполняется",
    NO_SERVERS_RUNNING : "Ни один сервер не выполняется",
    NO_CLUSTERS_RUNNING : "Ни один кластер не выполняется",

    HOST_WITH_ALL_SERVERS_RUNNING: "Хосты со всеми запущенными серверами", // not used anymore since 4Q
    HOST_WITH_SOME_SERVERS_RUNNING: "Хосты с некоторыми запущенными серверами",
    HOST_WITH_NO_SERVERS_RUNNING: "Хосты без запущенных серверов", // not used anymore since 4Q
    HOST_WITH_ALL_SERVERS_STOPPED: "Хосты со всеми остановленными серверами",
    HOST_WITH_SERVERS_RUNNING: "Хосты с запущенными серверами",

    RUNTIME_WITH_SOME_SERVERS_RUNNING: "Среды выполнения с некоторыми запущенными серверами",
    RUNTIME_WITH_ALL_SERVERS_STOPPED: "Среды выполнения со всеми остановленными серверами",
    RUNTIME_WITH_SERVERS_RUNNING: "Среды выполнения с запущенными серверами",

    START_ALL_APPS : "Запустить все приложения?",
    START_ALL_INSTANCES : "Запустить все экземпляры приложений?",
    START_ALL_SERVERS : "Запустить все серверы?",
    START_ALL_CLUSTERS : "Запустить все кластеры?",
    STOP_ALL_APPS : "Остановить все приложения?",
    STOPE_ALL_INSTANCES : "Остановить все экземпляры приложений?",
    STOP_ALL_SERVERS : "Остановить все серверы?",
    STOP_ALL_CLUSTERS : "Остановить все кластеры?",
    RESTART_ALL_APPS : "Перезапустить все приложения?",
    RESTART_ALL_INSTANCES : "Перезапустить все экземпляры приложений?",
    RESTART_ALL_SERVERS : "Перезапустить все серверы?",
    RESTART_ALL_CLUSTERS : "Перезапустить все кластеры?",

    START_INSTANCE : "Запустить экземпляр приложения?",
    STOP_INSTANCE : "Остановить экземпляр приложения?",
    RESTART_INSTANCE : "Перезапустить экземпляр приложения?",

    START_SERVER : "Запустить сервер {0}?",
    STOP_SERVER : "Остановить сервер {0}?",
    RESTART_SERVER : "Перезапустить сервер {0}?",

    START_ALL_INSTS_OF_APP : "Запустить все экземпляры {0}?", // application name
    START_APP_ON_SERVER : "Запустить {0} на {1}?", // app name, server name
    START_ALL_APPS_WITHIN : "Запустить все приложения в {0}?", // resource
    START_ALL_APP_INSTS_WITHIN : "Запустить все экземпляры приложений в {0}?", // resource
    START_ALL_SERVERS_WITHIN : "Запустить все серверы в {0}?", // resource
    STOP_ALL_INSTS_OF_APP : "Остановить все экземпляры {0}?", // application name
    STOP_APP_ON_SERVER : "Остановить {0} на {1}?", // app name, server name
    STOP_ALL_APPS_WITHIN : "Остановить все приложения в {0}?", // resource
    STOP_ALL_APP_INSTS_WITHIN : "Остановить все экземпляры приложений в {0}?", // resource
    STOP_ALL_SERVERS_WITHIN : "Остановить все серверы в {0}?", // resource
    RESTART_ALL_INSTS_OF_APP : "Перезапустить все экземпляры {0}?", // application name
    RESTART_APP_ON_SERVER : "Перезапустить {0} на {1}?", // app name, server name
    RESTART_ALL_APPS_WITHIN : "Перезапустить все приложения в {0}?", // resource
    RESTART_ALL_APP_INSTS_WITHIN : "Перезапустить все экземпляры приложений в {0}?", // resource
    RESTART_ALL_SERVERS_WITHIN : "Перезапустить все выполняющиеся серверы в {0}?", // resource

    START_SELECTED_APPS : "Запустить все экземпляры выбранных приложений?",
    START_SELECTED_INSTANCES : "Запустить выбранные экземпляры приложений?",
    START_SELECTED_SERVERS : "Запустить выбранные серверы?",
    START_SELECTED_SERVERS_LABEL : "Запустить выбранные серверы",
    START_SELECTED_CLUSTERS : "Запустить выбранные кластеры?",
    START_CLEAN_SELECTED_SERVERS : "Запустить выбранные серверы с параметром --clean?",
    START_CLEAN_SELECTED_CLUSTERS : "Запустить выбранные кластеры с параметром --clean?",
    STOP_SELECTED_APPS : "Остановить все экземпляры выбранных приложений?",
    STOP_SELECTED_INSTANCES : "Остановить выбранные экземпляры приложений?",
    STOP_SELECTED_SERVERS : "Остановить выбранные серверы?",
    STOP_SELECTED_CLUSTERS : "Остановить выбранные кластеры?",
    RESTART_SELECTED_APPS : "Перезапустить все экземпляры выбранных приложений?",
    RESTART_SELECTED_INSTANCES : "Перезапустить выбранные экземпляры приложений?",
    RESTART_SELECTED_SERVERS : "Перезапустить выбранные серверы?",
    RESTART_SELECTED_CLUSTERS : "Перезапустить выбранные кластеры?",

    START_SERVERS_ON_HOSTS : "Запустить все серверы на выбранных хостах?",
    STOP_SERVERS_ON_HOSTS : "Остановить все серверы на выбранных хостах?",
    RESTART_SERVERS_ON_HOSTS : "Перезапустить все выполняющиеся серверы на выбранных хостах?",

    SELECT_APPS_TO_START : "Выберите остановленные приложения для запуска.",
    SELECT_APPS_TO_STOP : "Выберите запущенные приложения для остановки.",
    SELECT_APPS_TO_RESTART : "Выберите запущенные приложения для перезапуска.",
    SELECT_INSTANCES_TO_START : "Выберите остановленные экземпляры приложений для запуска.",
    SELECT_INSTANCES_TO_STOP : "Выберите запущенные экземпляры приложений для остановки.",
    SELECT_INSTANCES_TO_RESTART : "Выберите запущенные экземпляры приложений для перезапуска.",
    SELECT_SERVERS_TO_START : "Выберите остановленные серверы для запуска.",
    SELECT_SERVERS_TO_STOP : "Выберите запущенные серверы для остановки.",
    SELECT_SERVERS_TO_RESTART : "Выберите запущенные серверы для перезапуска.",
    SELECT_CLUSTERS_TO_START : "Выберите остановленные кластеры для запуска.",
    SELECT_CLUSTERS_TO_STOP : "Выберите запущенные кластеры для остановки.",
    SELECT_CLUSTERS_TO_RESTART : "Выберите запущенные кластеры для перезапуска.",

    STATUS : "Состояние",
    STATE : "Состояние:",
    NAME : "Имя:",
    DIRECTORY : "Каталог",
    INFORMATION : "Информация",
    DETAILS : "Сведения",
    ACTIONS : "Действия",
    CLOSE : "Закрыть",
    HIDE : "Скрыть",
    SHOW_ACTIONS : "Показать действия",
    SHOW_SERVER_ACTIONS_LABEL : "Действия {0} сервера",
    SHOW_APP_ACTIONS_LABEL : "Действия {0} приложения",
    SHOW_CLUSTER_ACTIONS_LABEL : "Действия {0} кластера",
    SHOW_HOST_ACTIONS_LABEL : "Действия {0} хоста",
    SHOW_RUNTIME_ACTIONS_LABEL : "Действия среды выполнения {0}",
    SHOW_SERVER_ACTIONS_MENU_LABEL : "Меню действий сервера {0}",
    SHOW_APP_ACTIONS_MENU_LABEL : "Меню действий приложения {0}",
    SHOW_CLUSTER_ACTIONS_MENU_LABEL : "Меню действий кластера {0}",
    SHOW_HOST_ACTIONS_MENU_LABEL : "Меню действий хоста {0}",
    SHOW_RUNTIME_ACTIONS_MENU_LABEL : "Меню действий среды выполнения {0}",
    SHOW_RUNTIMEONHOST_ACTIONS_MENU_LABEL : "Меню действий среды выполнения на хосте {0}",
    SHOW_COLLECTION_MENU_LABEL : "Меню действий состояний набора {0}",  // menu object id
    SHOW_SEARCH_MENU_LABEL : "Меню действий состояний поиска {0}",  // menu object id


    // A bit odd to have sentence casing without punctuation?
    UNKNOWN_STATE : "{0}: неизвестное состояние", // resourceName
    UNKNOWN_STATE_APPS : "{0} приложений в неизвестном состоянии", // quantity
    UNKNOWN_STATE_APP_INSTANCES : "{0} экземпляров приложений в неизвестном состоянии", // quantity
    UNKNOWN_STATE_SERVERS : "{0} серверов в неизвестном состоянии", // quantity
    UNKNOWN_STATE_CLUSTERS : "{0} кластеров в неизвестном состоянии", // quantity

    INSTANCES_NOT_RUNNING : "{0} экземпляров приложений не выполняется", // quantity
    APPS_NOT_RUNNING : "{0} приложений не выполняется", // quantity
    SERVERS_NOT_RUNNING : "{0} серверов не выполняется", // quantity
    CLUSTERS_NOT_RUNNING : "{0} кластеров не выполняется", // quantity

    APP_STOPPED_ON_SERVER : "Приложение {0} остановлено на выполняющихся серверах: {1}", // appName, serverName(s)
    APPS_STOPPED_ON_SERVERS : "{0} приложений остановлено на выполняющихся серверах: {1}", // quantity, serverName(s)
    APPS_STOPPED_ON_SERVER : "{0} приложений остановлено на выполняющихся серверах.", // quantity
    NUMBER_RESOURCES : "{0} ресурсов", // quantity
    NUMBER_APPS : "{0} приложений", // quantity
    NUMBER_SERVERS : "{0} серверов", // quantity
    NUMBER_CLUSTERS : "{0} кластеров", // quantity
    NUMBER_HOSTS : "{0} хостов", // quantity
    NUMBER_RUNTIMES : "{0} сред выполнения", // quantity
    SERVERS_INSERT : "серверах",
    INSERT_STOPPED_ON_INSERT : "{0} остановлено на выполняющихся {1}.", // NUMBER_APPS, SERVERS_INSERT

    APPSERVER_STOPPED_ON_SERVER : "Приложение {0} остановлено на выполняющемся сервере {1}", //appName, serverName
    APPCLUSTER_STOPPED_ON_SERVER : "Приложение {0} в кластере {1} остановлено на выполняющихся серверах: {2}",  //appName, clusterName, serverName(s)

    INSTANCES_STOPPED_ON_SERVERS : "{0} экземпляров приложений остановлено на выполняющихся серверах.", // quantity
    INSTANCE_STOPPED_ON_SERVERS : "{0}: экземпляр приложения не выполняется", // serverNames

    NOT_ALL_APPS_RUNNING : "{0}: не все приложения выполняются", // serverName[]
    NO_APPS_RUNNING : "{0}: приложения не выполняются", // serverName[]
    NOT_ALL_APPS_RUNNING_SERVERS : "{0} серверов, на которых не все приложения выполняются", // quantity
    NO_APPS_RUNNING_SERVERS : "{0} серверов, на которых все приложения не выполняются", // quantity

    COUNT_OF_APPS_SELECTED : "Приложений выбрано: {0}",
    RATIO_RUNNING : "Выполняются: {0}", // ratio ex. 1/2

    RESOURCES_SELECTED : "Выбрано: {0}",

    NO_HOSTS_SELECTED : "Хосты не выбраны",
    NO_DEPLOY_RESOURCE : "Нет ресурсов для развертывания",
    NO_TOPOLOGY : "Нет {0}.",
    COUNT_OF_APPS_STARTED  : "Приложений выполняются:{0}",

    APPS_LIST : "{0} приложений",
    EMPTY_MESSAGE : "{0}",  // used to build a list of comma separated resource names
    APPS_INSTANCES_RUNNING_OF_TOTAL : "Вып. экземпляров: {0}/{1}",
    HOSTS_SERVERS_RUNNING_OF_TOTAL : "Вып. серверов: {0}/{1}",
    RESOURCE_ON_RESOURCE : "{0} на {1}", // resource name, resource name
    RESOURCE_ON_SERVER_RESOURCE : "{0} на сервере {1}", // resource name, resource name
    RESOURCE_ON_CLUSTER_RESOURCE : "{0} в кластере {1}", // resource name, resource name

    RESTART_DISABLED_FOR_ADMIN_CENTER: "Перезапуск данного сервера запрещен, так как на нем размещен Admin Center",
    ACTION_DISABLED_FOR_USER: "Действия отключены для этого ресурса, поскольку у пользователя нет прав доступа",

    RESTART_AC_TITLE: "Нельзя перезапустить Admin Center",
    RESTART_AC_DESCRIPTION: "На {0} расположен Admin Center. Admin Center не может перезапустить сам себя.",
    RESTART_AC_MESSAGE: "Все остальные выбранные серверы будут перезапущены.",
    RESTART_AC_CLUSTER_MESSAGE: "Все остальные выбранные кластеры будут перезапущены.",

    STOP_AC_TITLE: "Остановить Admin Center",
    STOP_AC_DESCRIPTION: "Сервер {0} является групповым контроллером, на котором выполняется Admin Center. Его остановка может повлиять на операции группового управления Liberty и сделает недоступным Admin Center.",
    STOP_AC_MESSAGE: "Остановить этот контроллер?",
    STOP_STANDALONE_DESCRIPTION: "На сервере {0} выполняется Admin Center. Остановка сервера приведет к потере доступа к Admin Center.",
    STOP_STANDALONE_MESSAGE: "Остановить этот сервер?",

    STOP_CONTROLLER_TITLE: "Остановить контроллер",
    STOP_CONTROLLER_DESCRIPTION: "Сервер {0} является групповым контроллером. Его остановка может повлиять на групповые операции Liberty.",
    STOP_CONTROLLER_MESSAGE: "Остановить этот контроллер?",

    STOP_AC_CLUSTER_TITLE: "Остановить кластер {0}",
    STOP_AC_CLUSTER_DESCRIPTION: "Кластер {0} содержит групповой контроллер, на котором запущен Admin Center.  Прекращение его работы может повлиять на операции группового Liberty и сделать Admin Center недоступным.",
    STOP_AC_CLUSTER_MESSAGE: "Остановить этот кластер?",

    INVALID_URL: "Страница не существует.",
    INVALID_APPLICATION: "Приложение {0} больше не существует в группе.", // application name
    INVALID_SERVER: "Сервер {0} больше не существует в группе.", // server name
    INVALID_CLUSTER: "Кластер {0} больше не существует в группе.", // cluster name
    INVALID_HOST: "Хост {0} больше не существует в группе.", // host name
    INVALID_RUNTIME: "Среда выполнения {0} больше не существует в группе.", // runtime name
    INVALID_INSTANCE: "Экземпляр приложения {0} больше не существует в группе.", // application instance name
    GO_TO_DASHBOARD: "Перейти к сводной панели",
    VIEWED_RESOURCE_REMOVED: "Увы! Ресурс удален или больше не доступен.",

    OK_DEFAULT_BUTTON: "OK",
    CONNECTION_FAILED_MESSAGE: "Нет связи с сервером. На странице больше не будут показываться динамические изменения среды. Обновите страницу, чтобы восстановить соединение и динамические обновления.",
    ERROR_MESSAGE: "Соединение прервано",

    // Used by standalone stop message dialog
    STANDALONE_STOP_TITLE : 'Остановить сервер',

    // Tags
    RELATED_RESOURCES: "Связанные ресурсы",
    TAGS : "Теги",
    TAG_BUTTON_LABEL : "Тег {0}",  // tag value
    TAGS_LABEL : "Введите теги. В качестве разделителя можно использовать запятую, пробел, символ новой строки или символ табуляции.",
    OWNER : "Владелец",
    OWNER_BUTTON_LABEL : "Владелец {0}",  // owner value
    CONTACTS : "Контактные лица",
    CONTACT_BUTTON_LABEL : "Контакт {0}",  // contact value
    PORTS : "Порты",
    CONTEXT_ROOT : "Корневой контекст",
    HTTP : "HTTP",
    HTTPS : "HTTPS",
    MORE : "Больше",  // alt text for the ... button
    MORE_BUTTON_MENU : "Дополнительные меню: {0}", // alt text for the menu
    NOTES: "Примечания",
    NOTE_LABEL : "Примечание {0}",  // note value
    SET_ATTRIBUTES: "Теги и метаданные",
    SETATTR_RUNTIME_NAME: "{0} на {1}",  // runtime, host
    SAVE: "Сохранить",
    TAGINVALIDCHARS: "Символы '/', '<' и '>' недопустимы.",
    ERROR_GET_TAGS_METADATA: "Продукт не может получить текущие теги и метаданные для ресурса.",
    ERROR_SET_TAGS_METADATA: "Продукту не удалось указать теги и метаданные вследствие ошибки.",
    METADATA_WILLBE_INHERITED: "Метаданные задаются для приложения и совместно используются всеми экземплярами в кластере.",
    ERROR_ALT: "Ошибка",

    // Graph Warning Messages
    GRAPH_SERVER_NOT_STARTED: "Текущая статистика недоступна для этого сервера, поскольку он остановлен. Запустите сервер, чтобы начать его мониторинг.",
    GRAPH_SERVER_HOSTING_APP_NOT_STARTED: "Текущая статистика недоступна для этого приложения, поскольку связанный с ним сервер остановлен. Запустите сервер, чтобы начать мониторинг приложения.",
    GRAPH_FEATURES_NOT_CONFIGURED: "Пока нет данных! Запустите мониторинг данного ресурса. Для этого щелкните на значке Изменить и добавьте показатели.",
    NO_GRAPHS_AVAILABLE: "Нет доступных показателей для добавления. Попробуйте установить дополнительные функции мониторинга, чтобы расширить набор доступных показателей.",
    NO_APPS_GRAPHS_AVAILABLE: "Нет доступных показателей для добавления. Попробуйте установить дополнительные функции мониторинга, чтобы расширить набор доступных показателей. Убедитесь также, что приложение используется.",
    GRAPH_CONFIG_NOT_SAVED_TITLE : "Несохраненные изменения",
    GRAPH_CONFIG_NOT_SAVED_DESCR : "У вас есть несохраненные изменения. Если вы перейдете на другую страницу, то эти изменения будут утрачены.",
    GRAPH_CONFIG_NOT_SAVED_MSG : "Сохранить изменения?",

    NO_CPU_STATS_AVAILABLE : "Статистика использования CPU недоступна для этого сервера.",

    // Server Config
    CONFIG_NOT_AVAILABLE: "Чтобы включить это представление, установите инструмент настройки сервера.",
    SAVE_BEFORE_CLOSING_DIALOG_MESSAGE: "Сохранить изменения в {0} перед закрытием?",
    SAVE: "Сохранить",
    DONT_SAVE: "Не сохранять",

    // Maintenance mode
    ENABLE_MAINTENANCE_MODE: "Включить режим обслуживания",
    DISABLE_MAINTENANCE_MODE: "Выключить режим обслуживания",
    ENABLE_MAINTENANCE_MODE_DIALOG_TITLE: "Включить режим обслуживания",
    DISABLE_MAINTENANCE_MODE_DIALOG_TITLE: "Выключить режим обслуживания",
    ENABLE_MAINTENANCE_MODE_HOST_DESCRIPTION: "Включить режим обслуживания на хосте и всех его серверах ({0} шт.)",
    ENABLE_MAINTENANCE_MODE_HOSTS_DESCRIPTION: "Включить режим обслуживания на хостах и всех их серверах ({0} шт.)",
    ENABLE_MAINTENANCE_MODE_SERVER_DESCRIPTION: "Включить режим обслуживания на сервере",
    ENABLE_MAINTENANCE_MODE_SERVERS_DESCRIPTION: "Включить режим обслуживания на серверах",
    DISABLE_MAINTENANCE_MODE_HOST_DESCRIPTION: "Выключить режим обслуживания на хосте и всех его серверах ({0} шт.)",
    DISABLE_MAINTENANCE_MODE_SERVER_DESCRIPTION: "Выключить режим обслуживания на сервере",
    BREAK_AFFINITY_LABEL: "Разорвать привязку к активным сеансам",
    ENABLE_MAINTENANCE_MODE_DIALOG_ENABLE_BUTTON_LABEL: "Включить",
    DISABLE_MAINTENANCE_MODE_DIALOG_ENABLE_BUTTON_LABEL: "Выключить",
    MAINTENANCE_MODE: "Режим обслуживания",
    ENABLING_MAINTENANCE_MODE: "Включение режима обслуживания",
    MAINTENANCE_MODE_ENABLED: "Режим обслуживания включен",
    MAINTENANCE_MODE_ALTERNATE_SERVERS_NOT_STARTED: "Режим обслуживания не включен, поскольку альтернативные серверы не запущены.",
    MAINTENANCE_MODE_SELECT_FORCE_MESSAGE: "Выберите Принудительно, чтобы включить режим обслуживания без запуска альтернативных серверов. Принудительное включение может нарушить стратегии автоматического масштабирования.",
    MAINTENANCE_MODE_FAILED: "Режим обслуживания невозможно включить.",
    MAINTENANCE_MODE_FORCE_LABEL: "Принудительно",
    MAINTENANCE_MODE_CANCEL_LABEL: "Отмена",
    MAINTENANCE_MODE_SERVERS_IN_MAINTENANCE_MODE: "{0} серверов работают в режиме обслуживания.",
    MAINTENANCE_MODE_ENABLING_MAINTENANCE_MODE_ON_ALL_HOST_SERVERS_EQUAL_OR_LESS_THEN_10: "Включение режима обслуживания на всех серверах хоста.",
    MAINTENANCE_MODE_ENABLING_MAINTENANCE_MODE_ON_ALL_HOST_SERVERS_MORE_THAN_10: "Включение режима обслуживания на всех серверах хоста.  Показать состояние на панели Серверы.",

    SERVER_API_DOCMENTATION: "Показать определение API сервера",

    // objectView title
    TITLE_FOR_CLUSTER: "Кластер {0}", // cluster name
    TITLE_FOR_HOST: "Хост {0}", // host name

    // objectView descriptor
    COLLECTIVE_CONTROLLER_DESCRIPTOR: "Групповой контроллер",
    LIBERTY_SERVER : "Сервер Liberty",
    NODEJS_SERVER : "Сервер Node.js",
    CONTAINER : "Контейнер",
    CONTAINER_LIBERTY : "Liberty",
    CONTAINER_DOCKER : "Docker",
    CONTAINER_NODEJS : "Node.js",
    LIBERTY_IN_DOCKER_DESCRIPTOR : "Сервер Liberty в контейнере Docker",
    NODEJS_IN_DOCKER_DESCRIPTOR : "Сервер Node.js в контейнере Docker",
    RUNTIME_LIBERTY : "Среда выполнения Liberty",
    RUNTIME_NODEJS : "Среда выполнения Node.js",
    RUNTIME_DOCKER : "Среда выполнения в контейнере Docker"

});
