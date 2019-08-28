/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
var messages = {
    // Common Strings
    "ADD_NEW": "Добавить",
    "CANCEL": "Отмена",
    "CLEAR_SEARCH": "Очистить строку поиска",
    "CLEAR_FILTER": "Очистить фильтр",
    "CLICK_TO_SORT": "Щелкните для сортировки по значениям столбца",
    "CLOSE": "Закрыть",
    "COPY_TO_CLIPBOARD": "Скопировать в буфер обмена",
    "COPIED_TO_CLIPBOARD": "Скопировано в буфер обмена",
    "DELETE": "Удалить",
    "DONE": "Готово",
    "EDIT": "Изменить",
    "FALSE": "False",
    "GENERATE": "Сгенерировать",
    "LOADING": "Загрузка",
    "LOGOUT": "Выход из системы",
    "NEXT_PAGE": "Следующая страница",
    "NO_RESULTS_FOUND": "Результаты не найдены",
    "PAGES": "Стр. {0} из {1}",   // {0} - current page number; {1} - total pages
    "PAGE_SELECT": "Выберите номер страницы для просмотра",
    "PREVIOUS_PAGE": "Предыдущая страница",
    "PROCESSING": "Обработка",
    "REGENERATE": "Обновить",
    "REGISTER": "Зарегистрировать",
    "TABLE_BATCH_BAR": "Панель действий для таблицы",
    "TABLE_FIELD_SORT_ASC": "Таблица отсортирована по {0} в порядке возрастания.",   // {0} - column name (ie. 'Name', 'Client ID')
    "TABLE_FIELD_SORT_DESC": "Таблица отсортирована по {0} в порядке убывания. ", // {0} - column name (ie. 'Name', 'Client ID')
    "TRUE": "True",
    "TRY_AGAIN": "Повторить попытку...",
    "UPDATE": "Обновить",

    // Common Column Names
    "CLIENT_NAME_COL": "Имя клиента",
    "EXPIRES_COL": "Действует до",
    "ISSUED_COL": "Выдано",
    "NAME_COL": "Имя",
    "TYPE_COL": "Тип",

    // Token Manager
    // 'app-token' and 'app-password' are keywords - don't translate
    // name - user defined name given to the app-password/app-token; user id - user's login name
    "TOKEN_MGR_TITLE": "Удалить маркеры",
    "TOKEN_MGR_DESC": "Удалить элементы app-password и app-token для указанного пользователя.",
    "TOKEN_MGR_SEARCH_PLACEHOLDER": "Введите ИД пользователя",
    "TABLE_FILLED_WITH": "Таблица обновлена и содержит идентификации {0}, принадлежащие {1}.",  // 0 - number of entries in table; 1 - user id
    "DELETE_SELECTED": "Удалить выбранные элементы app-password и app-token.",
    "DELETE_ARIA": "Удалить {0} с именем {1}",         // 0 - 'app-password' or 'app-token'; 1 - name
    "DELETE_PW": "Удалить этот элемент app-password",
    "DELETE_TOKEN": "Удалить этот элемент app-token",
    "DELETE_FOR_USERID": "{0} для {1}",                // 0 - name; 1 - user id
    "DELETE_WARNING_PW": "Это действие удалит текущий элемент app-password.",
    "DELETE_WARNING_TOKEN": "Это действие удалит текущий элемент app-token.",
    "DELETE_MANY": "Удалить App-Password/App-Token",
    "DELETE_MANY_FOR": "Присвоено {0}",              // 0 - user id
    "DELETE_ONE_MESSAGE": "Это действие удалит выбранный элемент app-password/app-token.",
    "DELETE_MANY_MESSAGE": "Это действие удалит {0} выбранных элементов app-password/app-token.",  // 0 - number
    "DELETE_ALL_MESSAGE": "Это действие удалит все элементы app-password/app-token, принадлежащие {0}.", // 0 - user id
    "DELETE_NONE": "Выберите элементы для удаления",
    "DELETE_NONE_MESSAGE": "Отметьте элементы app-password или app-token, которые необходимо удалить.",
    "SINGLE_ITEM_SELECTED": "Выбрано элементов: 1",
    "ITEMS_SELECTED": "Выбрано элементов: {0}",            // 0 - number
    "SELECT_ALL_AUTHS": "Выбрать все элементы app-password и app-token для этого пользователя.",
    "SELECT_SPECIFIC": "Выбрать {0} с именем {1} для удаления.",  // 0 - 'app-password' or 'app-token; 1 - name
    "NO_QUERY": "Требуется что-то найти? Введите ИД пользователя, чтобы просмотреть элементы app-password и app-token.",
    "GENERIC_FETCH_FAIL": "Ошибка при извлечении {0}",      // 0 - 'App-Passwords' or 'App-Tokens'
    "GENERIC_FETCH_FAIL_MSG": "Не удалось получить список {0}, принадлежащих {1}.", // 0 - 'app-passwords' or 'app-tokens; 1 - user id
    "GENERIC_DELETE_FAIL": "Ошибка при удалении {0}",       // 0 - 'App-Password' or 'App-Token'
    "GENERIC_DELETE_FAIL_MSG": "Произошла ошибка при удалении {0} с именем {1}.",  // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_DELETE_ALL_FAIL_MSG": "Произошла ошибка при удалении {0} для {1}.",     // 0 - 'app-passwords' or 'app-tokens'; 1 - user id
    "GENERIC_DELETE_FAIL_NOTYPES": "Ошибка при удалении",
    "GENERIC_DELETE_FAIL_NOTYPES_ONE_MSG": "Произошла ошибка при удалении следующего элемента app-password или app-token:",
    "GENERIC_DELETE_FAIL_NOTYPES_MSG": "Произошла ошибка при удалении следующих {0} элементов app-password и app-token:",  // 0 - number
    "IDENTIFY_AUTH": "{0} {1}",   // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_FETCH_ALL_FAIL": "Ошибка при извлечении идентификационных данных",
    "GENERIC_FETCH_ALL_FAIL_MSG": "Не удалось получить список элементов app-password и app-token, принадлежащих {0}.",   // 0 - user id
    "GENERIC_NOT_CONFIGURED": "Клиент не настроен",
    "GENERIC_NOT_CONFIGURED_MSG": "Атрибуты клиента appPasswordAllowed и appTokenAllowed не настроены.  Нет данных для извлечения."
};
