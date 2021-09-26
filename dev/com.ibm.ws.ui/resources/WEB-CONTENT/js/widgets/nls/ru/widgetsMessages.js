/*******************************************************************************
 * Copyright (c) 2016, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

define({
    LIBERTY_HEADER_TITLE: "Liberty Admin Center",
    LIBERTY_HEADER_PROFILE: "Параметры",
    LIBERTY_HEADER_LOGOUT: "Выход",
    LIBERTY_HEADER_LOGOUT_USERNAME: "Выход ({0})",
    TOOLBOX_BANNER_LABEL: "Информационная строка {0}",  // TOOLBOX_TITLE
    TOOLBOX_TITLE: "Панель инструментов",
    TOOLBOX_TITLE_LOADING_TOOL: "Загрузка инструмента...",
    TOOLBOX_TITLE_EDIT: "Изменить панель инструментов",
    TOOLBOX_EDIT: "Изменить",
    TOOLBOX_DONE: "Готово",
    TOOLBOX_SEARCH: "Фильтр",
    TOOLBOX_CLEAR_SEARCH: "Очистить критерии фильтрации",
    TOOLBOX_END_SEARCH: "Закрыть фильтр",
    TOOLBOX_ADD_TOOL: "Добавить инструмент",
    TOOLBOX_ADD_CATALOG_TOOL: "Добавить инструмент",
    TOOLBOX_ADD_BOOKMARK: "Добавить закладку",
    TOOLBOX_REMOVE_TITLE: "Удалить инструмент {0}",
    TOOLBOX_REMOVE_TITLE_NO_TOOL_NAME: "Удалить инструмент",
    TOOLBOX_REMOVE_MESSAGE: "Вы действительно хотите удалить это: {0}?",
    TOOLBOX_BUTTON_REMOVE: "Удалить",
    TOOLBOX_BUTTON_OK: "OK",
    TOOLBOX_BUTTON_GO_TO: "Перейти к набору инструментов",
    TOOLBOX_BUTTON_CANCEL: "Отмена",
    TOOLBOX_BUTTON_BGTASK: "Фоновые задачи",
    TOOLBOX_BUTTON_BACK: "Назад",
    TOOLBOX_BUTTON_USER: "Пользователь",
    TOOLBOX_ADDTOOL_ERROR_MESSAGE: "Произошла ошибка при добавлении инструмента {0}: {1}",
    TOOLBOX_REMOVETOOL_ERROR_MESSAGE: "Произошла ошибка при удалении инструмента {0}: {1}",
    TOOLBOX_GET_ERROR_MESSAGE: "Произошла ошибка при извлечении инструментов в панель инструментов: {0}",
    TOOLCATALOG_TITLE: "Каталог инструментов",
    TOOLCATALOG_ADDTOOL_TITLE: "Добавить инструмент",
    TOOLCATALOG_ADDTOOL_MESSAGE: "Вы действительно хотите добавить инструмент {0} на панель инструментов?",
    TOOLCATALOG_BUTTON_ADD: "Добавить",
    TOOL_FRAME_TITLE: "Рамка инструмента",
    TOOL_DELETE_TITLE: "Удалить {0}",
    TOOL_ADD_TITLE: "Добавить {0}",
    TOOL_ADDED_TITLE: "Объект {0} уже добавлен",
    TOOL_LAUNCH_ERROR_MESSAGE_TITLE: "Инструмент не найден",
    TOOL_LAUNCH_ERROR_MESSAGE: "Запрошенный инструмент не запущен, поскольку он отсутствует в каталоге.",
    LIBERTY_UI_ERROR_MESSAGE_TITLE: "Ошибка",
    LIBERTY_UI_WARNING_MESSAGE_TITLE: "Предупреждение",
    LIBERTY_UI_INFO_MESSAGE_TITLE: "Информация",
    LIBERTY_UI_CATALOG_GET_ERROR: "Произошла ошибка при получении каталога: {0}",
    LIBERTY_UI_CATALOG_GET_TOOL_ERROR: "Произошла ошибка при получении инструмента {0} из каталога: {1}",
    PREFERENCES_TITLE: "Параметры",
    PREFERENCES_SECTION_TITLE: "Параметры",
    PREFERENCES_ENABLE_BIDI: "Включить поддержку двунаправленного текста",
    PREFERENCES_BIDI_TEXTDIR: "Направление текста",
    PREFERENCES_BIDI_TEXTDIR_LTR: "Слева направо",
    PREFERENCES_BIDI_TEXTDIR_RTL: "Справа налево",
    PREFERENCES_BIDI_TEXTDIR_CONTEXTUAL: "В зависимости от контекста",
    PREFERENCES_SET_ERROR_MESSAGE: "Произошла ошибка при установке параметров пользователя в панели инструментов: {0}",
    BGTASKS_PAGE_LABEL: "Фоновые задачи",
    BGTASKS_DEPLOYMENT_INSTALLATION_POPUP: "Развернуть установку {0}", // {0} is the token number associated with the deployment
    BGTASKS_DEPLOYMENT_INSTALLATION: "Развернуть установку {0} - {1}", // {0} is the token number associated with the deployment, {1} is the server name
    BGTASKS_STATUS_RUNNING: "Выполняется",
    BGTASKS_STATUS_FAILED: "Сбой",
    BGTASKS_STATUS_SUCCEEDED: "Завершено", 
    BGTASKS_STATUS_WARNING: "Выполнено частично",
    BGTASKS_STATUS_PENDING: "Ожидание",
    BGTASKS_INFO_DIALOG_TITLE: "Сведения",
    //BGTASKS_INFO_DIALOG_DESC: "Description:",
    BGTASKS_INFO_DIALOG_STDOUT: "Стандартный вывод:",
    BGTASKS_INFO_DIALOG_STDERR: "Стандартная ошибка:",
    BGTASKS_INFO_DIALOG_EXCEPTION: "Исключительная ситуация:",
    //BGTASKS_INFO_DIALOG_RETURN_CODE: "Return code:",
    BGTASKS_INFO_DIALOG_RESULT: "Результат:",
    BGTASKS_INFO_DIALOG_DEPLOYED_ARTIFACT_NAME: "Имя сервера:",
    BGTASKS_INFO_DIALOG_DEPLOYED_USER_DIR: "Пользовательский каталог:",
    BGTASKS_POPUP_RUNNING_TASK_TITLE: "Активные фоновые задачи",
    BGTASKS_POPUP_RUNNING_TASK_NONE: "Нет",
    BGTASKS_POPUP_RUNNING_TASK_NO_ACTIVE_TASK: "Нет активных фоновых задач",
    BGTASKS_DISPLAY_BUTTON: "Хронология и сведения задачи",
    BGTASKS_EXPAND: "Развернуть раздел",
    BGTASKS_COLLAPSE: "Свернуть раздел",
    PROFILE_MENU_HELP_TITLE: "Справка",
    DETAILS_DESCRIPTION: "Описание",
    DETAILS_OVERVIEW: "Обзор",
    DETAILS_OTHERVERSIONS: "Другие версии",
    DETAILS_VERSION: "Версия: {0}",
    DETAILS_UPDATED: "Обновлено: {0}",
    DETAILS_NOTOPTIMIZED: "Не оптимизировано для текущего устройства.",
    DETAILS_ADDBUTTON: "Добавить в мой комплект инструментов",
    DETAILS_OPEN: "Открыть",
    DETAILS_CATEGORY: "Категория: {0}",
    DETAILS_ADDCONFIRM: "Инструмент {0} успешно добавлен в комплект.",
    CONFIRM_DIALOG_HELP: "Справка",
    YES_BUTTON_LABEL: "{0} да",  // insert is dialog title
    NO_BUTTON_LABEL: "{0} нет",  // insert is dialog title

    YES: "Да",
    NO: "Нет",

    TOOL_OIDC_ACCESS_DENIED: "Пользователю не присвоена роль, обладающая правами на выполнение этого запроса.",
    TOOL_OIDC_GENERIC_ERROR: "Возникла ошибка. См. информацию об ошибке в протоколе.",
    TOOL_DISABLE: "Пользователь не обладает правами на использование этого инструмента. Для работы с этим инструментом требуется роль Администратор." 
});
