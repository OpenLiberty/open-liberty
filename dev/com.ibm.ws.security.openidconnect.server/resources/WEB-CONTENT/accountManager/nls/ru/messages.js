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
    "CLEAR": "Очистить строку поиска",
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
    "TABLE_FIELD_SORT_ASC": "Таблица отсортирована по {0} в порядке возрастания.",   // {0} - column name (ie. 'Name', 'Client ID')
    "TABLE_FIELD_SORT_DESC": "Таблица отсортирована по {0} в порядке убывания.", // {0} - column name (ie. 'Name', 'Client ID')
    "TRUE": "True",
    "TRY_AGAIN": "Повторить попытку...",
    "UPDATE": "Обновить",

    // Common Column Names
    "EXPIRES_COL": "Действует до",
    "ISSUED_COL": "Выдано",
    "NAME_COL": "Имя",
    "TYPE_COL": "Тип",

    // Account Manager
    // 'app-token' and 'app-password' are keywords - don't translate
    // name - the user defined name given the app-password or app-token
    "ACCT_MGR_TITLE": "Управление личными маркерами",
    "ACCT_MGR_DESC": "Создание, удаление и обновление элементов app-password и app-token.",
    "ADD_NEW_AUTHENTICATION": "Добавить новый элемент app-password или app-token.",
    "NAME_IDENTIFIER": "Имя: {0}",
    "ADD_NEW_TITLE": "Зарегистрировать новые идентификационные данные",
    "NOT_GENERATED_PLACEHOLDER": "Не сгенерировано",
    "AUTHENTICAION_GENERATED": "Созданы идентификационные данные",
    "GENERATED_APP_PASSWORD": "Создан элемент app-password",
    "GENERATED_APP_TOKEN": "Создан элемент app-token",
    "COPY_APP_PASSWORD": "Скопировать элемент app-password в буфер обмена",
    "COPY_APP_TOKEN": "Скопировать элемент app-token в буфер обмена",
    "REGENERATE_APP_PASSWORD": "Обновить App-Password",
    "REGENERATE_PW_WARNING": "Это действие заменит текущий элемент app-password.",
    "REGENERATE_PW_PLACEHOLDER": "Дата создания предыдущего пароля: {0}",        // 0 - date
    "REGENERATE_APP_TOKEN": "Обновить App-Token",
    "REGENERATE_TOKEN_WARNING": "Это действие заменит текущий элемент app-token.",
    "REGENERATE_TOKEN_PLACEHOLDER": "Дата создания предыдущего маркера: {0}",        // 0 - date
    "DELETE_PW": "Удалить этот элемент app-password",
    "DELETE_TOKEN": "Удалить этот элемент app-token",
    "DELETE_WARNING_PW": "Это действие удалит текущий элемент app-password.",
    "DELETE_WARNING_TOKEN": "Это действие удалит текущий элемент app-token.",
    "REGENERATE_ARIA": "Обновить {0} для {1}",     // 0 - 'app-password' or 'app-token'; 1 - name
    "DELETE_ARIA": "Удалить {0} с именем {1}",       // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_GENERATE_FAIL": "Ошибка при создании {0}", // 0 - 'App-Password' or 'App-Token'
    "GENERIC_GENERATE_FAIL_MSG": "Произошла ошибка при создании нового {0} с именем {1}.",  // 0 - 'app-password' or 'app-token'; 1 - name
    "ERR_NAME": "Имя уже связано с {0} или слишком длинное.", // 0 - 'app-password' or 'app-token'
    "GENERIC_DELETE_FAIL": "Ошибка при удалении {0}",     // 0 - 'App-Password' or 'App-Token'
    "GENERIC_DELETE_FAIL_MSG": "Произошла ошибка при удалении {0} с именем {1}.",  // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_REGENERATE_FAIL": "Ошибка при повторном создании {0}",  // 0 - 'App-Password' or 'App-Token'
    "GENERIC_REGENERATE_FAIL_MSG": "Произошла ошибка при повторном создании {0} с именем {1}.",  // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_REGENERATE_FAIL_CREATE_MSG": "Произошла ошибка при обновлении {0} с именем {1}. Не удалось заново создать {0} после удаления.", // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_FETCH_FAIL": "Ошибка при извлечении идентификационных данных",
    "GENERIC_FETCH_FAIL_MSG": "Не удалось получить текущий список элементов app-password или app-token.",
    "GENERIC_NOT_CONFIGURED": "Клиент не настроен",
    "GENERIC_NOT_CONFIGURED_MSG": "Атрибуты клиента appPasswordAllowed и appTokenAllowed не настроены.  Нет данных для извлечения.",
    "APP_PASSWORD_NOT_CONFIGURED": "Атрибут клиента appPasswordAllowed не настроен.",  // 'appPasswordAllowed' is a config option. Do not translate.
    "APP_TOKEN_NOT_CONFIGURED": "Атрибут клиента appTokenAllowed не настроен."         // 'appTokenAllowed' is a config option.  Do not translate.
};
