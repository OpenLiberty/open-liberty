/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

define({
      ACCOUNTING_STRING : "Учетная строка",
      SEARCH_RESOURCE_TYPE_ALL : "Все",
      SEARCH : "Поиск",
      JAVA_BATCH_SEARCH_BOX_LABEL : "Введите критерии поиска. Для этого нажмите кнопку Добавить критерий поиска и укажите значение",
      SUBMITTED : "Отправлено",
      JMS_QUEUED : "В очереди JMS",
      JMS_CONSUMED : "Использовано JMS",
      JOB_PARAMETER : "Параметр задания",
      DISPATCHED : "Передано",
      FAILED : "Сбой",
      STOPPED : "Остановлено",
      COMPLETED : "Завершено",
      ABANDONED : "Аннулировано",
      STARTED : "Запущено",
      STARTING : "Запускается",
      STOPPING : "Останавливается",
      REFRESH : "Обновить",
      INSTANCE_STATE : "Состояние экземпляра",
      APPLICATION_NAME : "Имя приложения",
      APPLICATION: "Приложение",
      INSTANCE_ID : "ИД экземпляра",
      LAST_UPDATE : "Последнее обновление",
      LAST_UPDATE_RANGE : "Диапазон последних обновлений",
      LAST_UPDATED_TIME : "Время последнего обновления",
      DASHBOARD_VIEW : "Представление сводной панели",
      HOMEPAGE : "Домашняя страница",
      JOBLOGS : "Протоколы заданий",
      QUEUED : "В очереди",
      ENDED : "Завершено",
      ERROR : "Ошибка",
      CLOSE : "Закрыть",
      WARNING : "Предупреждение",
      GO_TO_DASHBOARD: "Перейти к сводной панели",
      DASHBOARD : "Сводная панель",
      BATCH_JOB_NAME: "Имя пакетного задания",
      SUBMITTER: "Отправитель",
      BATCH_STATUS: "Состояние пакета",
      EXECUTION_ID: "ИД выполнения задания",
      EXIT_STATUS: "Состояние выхода",
      CREATE_TIME: "Время создания",
      START_TIME: "Время запуска",
      END_TIME: "Время завершения",
      SERVER: "Сервер",
      SERVER_NAME: "Имя сервера",
      SERVER_USER_DIRECTORY: "Пользовательский каталог",
      SERVERS_USER_DIRECTORY: "Пользовательский каталог сервера",
      HOST: "Хост",
      NAME: "Имя",
      JOB_PARAMETERS: "Параметры задания",
      JES_JOB_NAME: "Имя задания JES",
      JES_JOB_ID: "ИД задания JES",
      ACTIONS: "Действия",
      VIEW_LOG_FILE: "Просмотреть файл протокола",
      STEP_NAME: "Имя шага",
      ID: "ИД",
      PARTITION_ID: "Раздел {0}",                               // Partition ID number
      VIEW_EXECUTION_DETAILS: "Просмотреть сведения о выполнении задания {0}",    // Job Execution ID number
      PARENT_DETAILS: "Сведения о родительском объекте",
      TIMES: "Время",      // Heading on section referencing create, start, and end timestamps
      STATUS: "Состояние",
      SEARCH_ON: "Выберите для фильтрации по {1} {0}",                    // {0} Value, {1} Column name
      SEARCH_PILL_INPUT : "Укажите критерии поиска.",
      BREADCRUMB_JOB_INSTANCE : "Экземпляр задания {0}",                // Job Instance ID
      BREADCRUMB_JOB_EXECUTION : "Выполнение задания {0}",              // Job Execution ID
      BREADCRUMB_JOB_LOG : "Протокол задания {0}",
      BATCH_SEARCH_CRITERIA_INVALID : "Критерии поиска недопустимы.",
      ERROR_CANNOT_HAVE_MULTIPLE_SEARCHBY : "В критериях поиска нельзя задавать несколько фильтров по параметрам {0}.", // {0} will be another translated message key like SUBMITTER and BATCH_JOB_NAME

      INSTANCES_TABLE_IDENTIFIER: "Таблица экземпляров задания",
      EXECUTIONS_TABLE_IDENTIFIER: "Таблица выполнений задания",
      STEPS_DETAILS_TABLE_IDENTIFIER: "Таблица со сведениями о шагах",
      LOADING_VIEW : "На странице загружается информация",
      LOADING_VIEW_TITLE : "Представление загрузки",
      LOADING_GRID : "Ожидание результатов поиска для возврата с сервера",
      PAGENUMBER : "Номер страницы",
      SELECT_QUERY_SIZE: "Выбрать размер запроса",
      LINK_EXPLORE_HOST: "Выберите для просмотра сведений о хосте {0} в инструменте Обзор.",      // Host name
      LINK_EXPLORE_SERVER: "Выберите для просмотра сведений о сервере {0} в инструменте Обзор.",  // Server name

      //ACTIONS
      RESTART: "Перезапустить",
      STOP: "Остановить",
      PURGE: "Очистить",
      OK_BUTTON_LABEL: "OK",
      INSTANCE_ACTIONS_BUTTON_LABEL: "Действия для экземпляра задания {0}",     // Job Instance ID number
      INSTANCE_ACTIONS_MENU_LABEL:  "Меню действий экземпляров задания",

      RESTART_INSTANCE_MESSAGE: "Перезапустить последнее выполнение задания, связанное с экземпляром задания {0}?",     // Job Instance ID number
      STOP_INSTANCE_MESSAGE: "Остановить последнее выполнение задания, связанное с экземпляром задания {0}?",           // Job Instance ID number
      PURGE_INSTANCE_MESSAGE: "Очистить все записи базы данных и протоколы заданий, связанные с экземпляром задания {0}?",     // Job Instance ID number
      PURGE_JOBSTORE_ONLY: "Очистить только хранилище заданий",

      RESTART_INST_ERROR_MESSAGE: "Запрос на перезапуск не выполнен.",
      STOP_INST_ERROR_MESSAGE: "Запрос на остановку не выполнен.",
      PURGE_INST_ERROR_MESSAGE: "Запрос на очистку не выполнен.",
      ACTION_REQUEST_ERROR_MESSAGE: "Запрос действия не выполнен, код состояния: {0}.  URL: {1}",  // Status Code number, URL string

      // RESTART JOB WITH PARAMETERS DIALOG
      REUSE_PARMS_TOGGLE_LABEL: "Использовать параметры из предыдущего вызова",
      JOB_PARAMETERS_EMPTY: "Если не включен переключатель '{0}', укажите здесь параметры задания.",   // 0 - Checkbox label - REUSE_PARMS_TOGGLE_LABEL
      JOB_PARAMETER_NAME: "Имя параметра",
      JOB_PARAMETER_VALUE: "Значение параметра",
      PARM_NAME_COLUMN_HEADER: "Параметр",
      PARM_VALUE_COLUMN_HEADER: "Значение",
      PARM_ADD_ICON_TITLE: "Добавить параметр",
      PARM_REMOVE_ICON_TITLE: "Удалить параметр",
      PARMS_ENTRY_ERROR: "Требуется имя параметра.",
      JOB_PARAMETER_CREATE: "Нажмите \"{0}\", чтобы добавить параметры в следующий вызов этого экземпляра задания.",  // 0 - Button label
      JOB_PARAMETER_CREATE_BUTTON: "Добавить кнопку параметров в заголовок таблицы.",

      //JOB LOGS PAGE MESSAGES
      JOBLOGS_LOG_CONTENT : "Содержимое протокола задания",
      FILE_DOWNLOAD : "Загрузка файла",
      DOWNLOAD_DIALOG_DESCRIPTION : "Загрузить файл протокола?",
      INCLUDE_ALL_LOGS : "Включить все файлы протокола для выполнения задания",
      LOGS_NAVIGATION_BAR : "Панель навигации по протоколам заданий",
      DOWNLOAD : "Загрузить",
      LOG_TOP : "Начало списка протоколов",
      LOG_END : "Конец списка протоколов",
      PREVIOUS_PAGE : "Предыдущая страница",
      NEXT_PAGE : "Следующая страница",
      DOWNLOAD_ARIA : "Загрузить файл",

      //Error messages for popups
      REST_CALL_FAILED : "Ошибка вызова для извлечения данных.",
      NO_JOB_EXECUTION_URL : "В URL не указан номер выполнения задания, или в экземпляре нет протоколов выполнения заданий для отображения.",
      NO_VIEW : "Ошибка URL: такое представление не существует.",
      WRONG_TOOL_ID : "Строка запроса URL начинается не с ИД инструмента {0}, а с {1}.",   // {0} and {1} are both Strings
      URL_NO_LOGS : "Ошибка URL: протоколы не существуют.",
      NOT_A_NUMBER : "Ошибка URL: {0} не число.",                                                // {0} is a field name
      PARAMETER_REPETITION : "Ошибка URL: {0} может встречаться только один раз в параметрах.",                   // {0} is a field name
      URL_PAGE_PARAMS_ERROR : "Ошибка URL: параметр страницы лежит за пределами допустимого диапазона.",
      INVALID_PARAMETER : "Ошибка URL: параметр {0} недопустим.",                                       // {0} is a String
      URL_MULTIPLE_ATTRIBUTES : "Ошибка URL: в URL может быть либо выполнение задания, либо экземпляр задания.",
      MISSING_EXECUTION_ID_PARAM : "Отсутствует обязательный параметр ИД выполнения.",
      PERSISTENCE_CONFIGURATION_REQUIRED : "Для использования пакетного инструмента Java требуется конфигурация постоянной базы данных пакетов Java.",
      IGNORED_SEARCH_CRITERIA : "В результатах были проигнорированы следующие критерии фильтров: {0}",

      GRIDX_SUMMARY_TEXT : "Показаны недавние экземпляры задания (${0} шт.)"

});

