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
    "REGENERATE": "Создать повторно",
    "REGISTER": "Зарегистрировать",
    "TABLE_FIELD_SORT_ASC": "Таблица отсортирована по {0} в порядке возрастания.",   // {0} - column name (ie. 'Name', 'Client ID')
    "TABLE_FIELD_SORT_DESC": "Таблица отсортирована по {0} в порядке убывания.", // {0} - column name (ie. 'Name', 'Client ID')
    "TRUE": "True",
    "TRY_AGAIN": "Повторить попытку...",
    "UPDATE": "Обновить",

    // Common Column Names
    "CLIENT_NAME_COL": "Имя клиента",
    "EXPIRES_COL": "Действует до",
    "ISSUED_COL": "Выдано",
    "NAME_COL": "Имя",
    "TYPE_COL": "Тип",

    // Client Admin
    "CLIENT_ADMIN_TITLE": "Управление клиентами OAuth",
    "CLIENT_ADMIN_DESC": "Этот инструмент предназначен для добавления и редактирования клиентов, а также обновления паролей клиентов.",
    "CLIENT_ADMIN_SEARCH_PLACEHOLDER": "Фильтр: имя клиента OAuth",
    "ADD_NEW_CLIENT": "Добавить новый клиент OAuth.",
    "CLIENT_NAME": "Имя клиента",
    "CLIENT_ID": "ИД клиента",
    "EDIT_ARIA": "Редактировать клиент OAuth {0}",      // {0} - name
    "DELETE_ARIA": "Удалить клиент OAuth {0}",  // {0} - name
    "CLIENT_SECRET": "Пароль клиента",
    "GRANT_TYPES": "Типы предоставления",
    "SCOPE": "Область",
    "PREAUTHORIZED_SCOPE": "Предварительно разрешенная область (необязательно)",
    "REDIRECT_URLS": "URL перенаправления (необязательно)",
    "CLIENT_SECRET_CHECKBOX": "Заново создать пароль клиента",
    "NONE_SELECTED": "Ничего не выбрано",
    "MODAL_EDIT_TITLE": "Редактировать клиент OAuth",
    "MODAL_REGISTER_TITLE": "Зарегистрировать новый клиент OAuth",
    "MODAL_SECRET_REGISTER_TITLE": "Регистрация OAuth сохранена",
    "MODAL_SECRET_UPDATED_TITLE": "Регистрация OAuth обновлена",
    "MODAL_DELETE_CLIENT_TITLE": "Удалить этот клиент OAuth",
    "RESET_GRANT_TYPE": "Очистить все выбранные типы предоставления.",
    "SELECT_ONE_GRANT_TYPE": "Выберите по крайней мере один тип предоставления",
    "SPACE_HELPER_TEXT": "Значения через пробел",
    "REDIRECT_URL_HELPER_TEXT": "Абсолютные URL перенаправления через пробел",
    "DELETE_OAUTH_CLIENT_DESC": "Эта операция удаляет требуемый клиент из службы регистрации клиентов.",
    "REGISTRATION_SAVED": "ИД клиента и пароль клиента созданы и присвоены.",
    "REGISTRATION_UPDATED": "Новый пароль клиента создан и присвоен этому клиенту.",
    "COPY_CLIENT_ID": "Скопировать ИД клиента в буфер обмена",
    "COPY_CLIENT_SECRET": "Скопировать пароль клиента в буфер обмена",
    "REGISTRATION_UPDATED_NOSECRET": "Клиент OAuth {0} обновлен.",                 // {0} - client name
    "ERR_MULTISELECT_GRANT_TYPES": "Необходимо выбрать по крайней мере один предоставления.",
    "ERR_REDIRECT_URIS": "Значения должны быть абсолютными URI.",
    "GENERIC_REGISTER_FAIL": "Ошибка при регистрации клиента OAuth",
    "GENERIC_UPDATE_FAIL": "Ошибка при обновлении клиента OAuth",
    "GENERIC_DELETE_FAIL": "Ошибка при удалении клиента OAuth",
    "GENERIC_MISSING_CLIENT": "Ошибка при извлечении клиента OAuth",
    "GENERIC_REGISTER_FAIL_MSG": "Произошла ошибка при регистрации клиента OAuth {0}.",  // {0} - client name
    "GENERIC_UPDATE_FAIL_MSG": "Произошла ошибка при обновлении клиента OAuth {0}.",       // {0} - client name
    "GENERIC_DELETE_FAIL_MSG": "Произошла ошибка при удалении клиента OAuth {0}.",       // {0} - client name
    "GENERIC_MISSING_CLIENT_MSG": "Клиент OAuth {0} с ИД {1} не найден.",     // {0} - client name; {1} - an ID
    "GENERIC_RETRIEVAL_FAIL_MSG": "Произошла ошибка при извлечении информации о клиенте OAuth {0}.", // {0} - client name
    "GENERIC_GET_CLIENTS_FAIL": "Ошибка при извлечении клиентов OAuth",
    "GENERIC_GET_CLIENTS_FAIL_MSG": "Произошла ошибка во время получения списка клиентов OAuth.",

    "RESET_SELECTION": "Очистить все выбранные {0}",     // {0} - field name (ie 'Grant types')
    "NUMBER_SELECTED": "Выбрано {0}",     // {0} - field name
    "OPEN_LIST": "Открыть список {0}.",                   // {0} - field name
    "CLOSE_LIST": "Закрыть список {0}.",                 // {0} - field name
    "ENTER_PLACEHOLDER": "Введите значение",
    "ADD_VALUE": "Добавить элемент",
    "REMOVE_VALUE": "Удалить элемент",
    "REGENERATE_CLIENT_SECRET": "'*' сохраняет существующее значение. Пустое значение приводит к созданию нового пароля клиента. Непустое значение переопределяет существующее значение.",
    "ALL_OPTIONAL": "Все поля необязательные"
};
