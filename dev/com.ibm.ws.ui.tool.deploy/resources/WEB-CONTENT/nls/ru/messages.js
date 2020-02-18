var messages = {
//General
"DEPLOY_TOOL_TITLE": "Развернуть",
"SEARCH" : "Поиск",
"SEARCH_HOSTS" : "Найти хосты",
"EXPLORE_TOOL": "Инструмент Обзор",
"EXPLORE_TOOL_INSERT": "Попробуйте инструмент Обзор",
"EXPLORE_TOOL_ARIA": "Поиск хостов в инструменте Обзор, открывающемся в новой вкладке",

//Rule Selector Panel
"RULESELECT_EDIT" : "ИЗМЕНИТЬ",
"RULESELECT_CHANGE_SELECTION" : "ИЗМЕНИТЬ ВЫБРАННОЕ",
"RULESELECT_SERVER_DEFAULT" : "ТИПЫ СЕРВЕРА ПО УМОЛЧАНИЮ",
"RULESELECT_SERVER_CUSTOM" : "ПОЛЬЗОВАТЕЛЬСКИЕ ТИПЫ",
"RULESELECT_SERVER_CUSTOM_ARIA" : "Пользовательские типы серверов",
"RULESELECT_NEXT" : "ДАЛЕЕ",
"RULESELECT_SERVER_TYPE": "Тип сервера",
"RULESELECT_SELECT_ONE": "Выберите один вариант",
"RULESELECT_DEPLOY_TYPE" : "Правило развертывания",
"RULESELECT_SERVER_SUBHEADING": "Сервер",
"RULESELECT_CUSTOM_PACKAGE": "Пользовательский пакет",
"RULESELECT_RULE_DEFAULT" : "ПРАВИЛА ПО УМОЛЧАНИЮ",
"RULESELECT_RULE_CUSTOM" : "ПОЛЬЗОВАТЕЛЬСКИЕ ПРАВИЛА",
"RULESELECT_FOOTER" : "Выберите тип сервера и тип правила перед возвратом к форме развертывания.",
"RULESELECT_CONFIRM" : "ПОДТВЕРДИТЬ",
"RULESELECT_CUSTOM_INFO": "Можно определить свои правила с произвольными входными данными и параметрами развертывания.",
"RULESELECT_CUSTOM_INFO_LINK": "Подробнее",
"RULESELECT_BACK": "Назад",

//Rule Selector Aria
"RULESELECT_PANEL_ARIA" : "Панель выбора правил: {0}", //RULESELECT_OPEN or RULESELECT_CLOSED
"RULESELECT_OPEN" : "Открыть",
"RULESELECT_CLOSED" : "Закрыто",
"RULESELECT_SCROLL_UP": "Прокрутить вверх",
"RULESELECT_SCROLL_DOWN": "Прокрутить вниз",
"RULESELECT_EDIT_SERVER_ARIA" : "Изменить тип сервера, текущее значение: {0}", // Insert for SERVER TYPES
"RULESELECT_EDIT_RULE_ARIA" : "Изменить правило, текущее значение: {0}", //Insert for PACKAGE TYPES
"RULESELECT_NEXT_ARIA" : "Следующая панель",

//SERVER TYPES
"LIBERTY_SERVER" : "Сервер Liberty",
"NODEJS_SERVER" : "Сервер Node.js",

//PACKAGE TYPES
"APPLICATION_PACKAGE" : "Пакет приложения", //IMPORTANT: Ensure that the translation of this string is the same as the one for "DEPLOY_RULE_APPLICATION_PACKAGE_TYPE" in com.ibm.ws.collective.controller\resources\com\ibm\ws\collective\command\internal\resources\Messages.nlsprops
"SERVER_PACKAGE" : "Пакет сервера", //IMPORTANT: Ensure that the translation of this string is the same as the one for "DEPLOY_RULE_SERVER_PACKAGE_TYPE" in com.ibm.ws.collective.controller\resources\com\ibm\ws\collective\command\internal\resources\Messages.nlsprops
"DOCKER_CONTAINER" : "Контейнер Docker", //IMPORTANT: Ensure that the translation of this string is the same as the one for "DEPLOY_RULE_DOCKER_PACKAGE_TYPE" in com.ibm.ws.collective.controller\resources\com\ibm\ws\collective\command\internal\resources\Messages.nlsprops

//Deployment parameters
"DEPLOYMENT_PARAMETERS": "Параметры развертывания",
// "PARAMETERS_TOGGLE": "Toggle between upload and use a file located on the collective controller",
"GROUP_DEPLOYMENT_PARAMETERS": "Параметры развертывания ({0})",
"PARAMETERS_DESCRIPTION": "Набор параметров зависит от выбранных типов сервера и шаблона.",
"PARAMETERS_TOGGLE_CONTROLLER": "Использовать файл, расположенный в групповом контроллере",
"PARAMETERS_TOGGLE_UPLOAD": "Передать файл",
"SEARCH_IMAGES": "Найти образы",
"SEARCH_CLUSTERS": "Найти кластеры",
"CLEAR_FIELD_BUTTON_ARIA": "Очистить входное значение",

// file browser and upload
"SERVER_BROWSE_DESCRIPTION": "Передать файл пакета сервера",
"BROWSE_TITLE": "Передать {0}",
"STRONGLOOP_BROWSE": "Перенесите файл сюда с помощью мыши или {0}, чтобы указать имя файла", //BROWSE_INSERT
"BROWSE_INSERT" : "выберите его",
"BROWSE_ARIA": "выбрать файлы",
"FILE_UPLOAD_PREVIOUS" : "Использовать файл, расположенный в групповом контроллере",
"IS_UPLOADING": "Идет передача {0}...",
"CANCEL" : "ОТМЕНА",
"UPLOAD_SUCCESSFUL" : "{0} успешно передан.", // Package Name
"UPLOAD_FAILED" : "Передача не выполнена",
"RESET" : "сброс",
"FILE_UPLOAD_WRITEDIRS_EMPTY_ERROR" : "Не задан список каталогов записи.",
"FILE_UPLOAD_WRITEDIRS_PATH_ERROR" : "Указанный путь не должен входить в число каталогов записи.",
"PARAMETERS_FILE_ARIA" : "Параметры развертывания или {0}", // Upload Package name

// docker
"DOCKER_MISSING_REPOSITORY_ERROR": "Необходимо настроить хранилище Docker",
"DOCKER_EMPTY_IMAGE_ERROR": "В настроенном хранилище Docker не найден ни один образ",
"DOCKER_GENERIC_ERROR": "Не загружен ни один образ Docker. Убедитесь, что хранилище Docker настроено.",
"REFRESH": "Обновить",
"REFRESH_ARIA": "Обновить образы Docker",
"PARAMETERS_DOCKER_ARIA": "Параметры развертывания или поиск образов Docker",
"DOCKER_IMAGES_ARIA" : "Список образов Docker",
"LOCAL_IMAGE": "имя локального образа",
"DOCKER_INVALID_CONTAINER__NAME_ERROR" : "Имя контейнера должно быть в формате [a-zA-Z0-9][a-zA-Z0-9_.-]*",

//Host selection
"ONE_SELECTED_HOST" : "Выбрано хостов: {0}", //quantity
"N_SELECTED_HOSTS": "Выбрано хостов: {0}", //quantity
"SELECT_HOSTS_MESSAGE": "Выберите хост в списке доступных хостов. Хост можно найти по имени или тегам.",
"ONE_HOST" : "Результатов: {0}", //quantity
"N_HOSTS": "Результатов: {0}", //quantity
"SELECT_HOSTS_FOOTER": "Нужно задать более сложное условие поиска? {0}", //EXPLORE_TOOL_INSERT
"NAME": "ИМЯ",
"NAME_FILTER": "Отфильтровать хосты по имени", // Used for aria-label
"TAG": "ТЕГ",
"TAG_FILTER": "Отфильтровать хосты по тегу",
"ALL_HOSTS_LIST_ARIA" : "Список со всеми хостами",
"SELECTED_HOSTS_LIST_ARIA": "Список с выбранными хостами",

//Security Details
"SECURITY_DETAILS": "Сведения о защите",
"SECURITY_DETAILS_FOR_GROUP": "Сведения о защите для группы {0}",  // {0} is the translated group name
"SECURITY_DETAILS_MESSAGE": "Для обеспечения защиты сервера требуется указать дополнительные идентификационные данные.",
"SECURITY_CREATE_PASSWORD" : "Создать пароль",
"KEYSTORE_PASSWORD_MESSAGE": "Укажите пароль для защиты создаваемых файлов хранилища ключей, содержащих идентификационные данные для сервера.",
"PASSWORD_MESSAGE": "Укажите пароль для защиты создаваемых файлов, содержащих идентификационные данные для сервера.",
"KEYSTORE_PASSWORD": "Пароль хранилища ключей",
"CONFIRM_KEYSTORE_PASSWORD": "Подтвердите пароль хранилища ключей",
"PASSWORDS_DONT_MATCH": "Пароли не совпадают",
"GROUP_GENERIC_PASSWORD": "{0} ({1})", // {0} is the display label for the initial password field
                                       // {1} is the group name if the password belongs to a group
"CONFIRM_GENERIC_PASSWORD": "Подтвердите {0}", // {0} is the display label for the initial password field
"CONFIRM_GROUP_GENERIC_PASSWORD": "Подтвердите {0} ({1})", // {0} is the display label for the initial password field
                                                       // {1} is the group name if the password belongs to a group

//Deploy
"REVIEW_AND_DEPLOY": "Проверить и развернуть",
"REVIEW_AND_DEPLOY_MESSAGE" : "Все поля {0} перед развертыванием.", //REVIEW_AND_DEPLOY_INCOMPLETE_FIELDS
"REVIEW_AND_DEPLOY_INCOMPLETE_FIELDS": "должны быть заполнены",
"READY_FOR_DEPLOYMENT": "готов к развертыванию.",
"READY_FOR_DEPLOYMENT_CAPS": "Все готово к развертыванию.",
"READY_TO_DEPLOY": "Форма заполнена. {0}", //READY_FOR_DEPLOYMENT_CAPS
"READY_TO_DEPLOY_SERVER": "Форма заполнена. Пакет сервера {0}", //READY_FOR_DEPLOYMENT
"READY_TO_DEPLOY_DOCKER": "Форма заполнена. Контейнер Docker {0}", //READY_FOR_DEPLOYMENT
"DEPLOY": "РАЗВЕРНУТЬ",

"DEPLOY_UPLOADING" : "Дождитесь завершения передачи пакета сервера...",
"DEPLOY_FILE_UPLOADING" : "Завершение передачи файла...",
"UPLOADING": "Передача...",
"DEPLOY_UPLOADING_MESSAGE" : "Не закрывайте это окно, пока не начнется процесс развертывания.",
"DEPLOY_AFTER_UPLOADED_MESSAGE" : "После завершения передачи {0} вы сможете следить здесь за ходом развертывания.", // Package name
"DEPLOY_FILE_UPLOAD_PERCENT" : "{0} - {1}% выполнено", // Package name, number
"DEPLOY_WATCH_FOR_UPDATES": "Следите здесь за обновлениями или закройте это окно - операция продолжит выполняться в фоновом режиме.",
"DEPLOY_CHECK_STATUS": "Состояние развертывания можно проверить в любой момент, щелкнув на значке Фоновые задачи в правом верхнем углу этого окна.",
"DEPLOY_IN_PROGRESS": "Выполняется развертывание.",
"DEPLOY_VIEW_BG_TASKS": "Показать фоновые задачи",
"DEPLOYMENT_PROGRESS": "Состояние развертывания",
"DEPLOYING_IMAGE": "{0} на {1} хостах", // Package name (e.g.,  someServerName.zip, aLibertyDockerImage), number of hosts
"VIEW_DEPLOYED_SERVERS": "Показать успешно развернутые серверы",
"DEPLOY_PERCENTAGE": "ВЫПОЛНЕНО {0}%", //quantity
"DEPLOYMENT_COMPLETE_TITLE" : "Развертывание выполнено.",
"DEPLOYMENT_COMPLETE_WITH_ERRORS_TITLE" : "Развертывание выполнено, но с ошибками.",
"DEPLOYMENT_COMPLETE_MESSAGE" : "Вы можете изучить подробные сведения об ошибках, просмотреть развернутые серверы или запустить другую операцию развертывания.",
"DEPLOYING": "Идет развертывание...",
"DEPLOYMENT_FAILED": "Не удалось выполнить развертывание.",
"RETURN_DEPLOY": "Вернитесь к форме развертывания и запустите операцию снова",
"REUTRN_DEPLOY_HEADER": "Повторить попытку",

//Footer
"FOOTER": "Нужно развернуть что-то еще?",
"FOOTER_BUTTON_MESSAGE" : "Начните другое развертывание",

//Error stuff
"ERROR_TITLE": "Сводка ошибок",
"ERROR_VIEW_DETAILS" : "Показать сведения об ошибке",
"ONE_ERROR_ONE_HOST": "Возникла одна ошибка на одном хосте",
"ONE_ERROR_MULTIPLE_HOST": "Возникла одна ошибка на нескольких хостах",
"MULTIPLE_ERROR_ONE_HOST": "Возникло несколько ошибок на одном хосте",
"MULTIPLE_ERROR_MULTIPLE_HOST": "Возникло несколько ошибок на нескольких хостах",
"INITIALIZATION_ERROR_MESSAGE": "Не удалось получить информацию о хостах или правилах развертывания с сервера",
"TRANSLATIONS_ERROR_MESSAGE" : "Не удалось получить доступ к экспортированным строкам",
"MISSING_HOST": "Выберите по крайней мере один хост в списке",
"INVALID_CHARACTERS" : "Поле не должно содержать специальные символы из набора '()$%&'",
"INVALID_DOCKER_IMAGE" : "Образ не найден",
"ERROR_HOSTS" : "{0} и еще {1}" // if there are more than three hosts reporting error, the message for errors from 6 hosts would be like:
                                      // host1.com, host2.com, host3.com and 3 others
};
