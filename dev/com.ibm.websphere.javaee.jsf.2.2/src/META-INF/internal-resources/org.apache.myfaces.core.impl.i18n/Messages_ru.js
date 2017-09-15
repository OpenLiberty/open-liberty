/* Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
if (_MF_CLS) {
/**
 * System messages kyrillic/russian version
 *
 * @class
 * @name Messages_nl
 * @extends myfaces._impl.i18n.Messages
 * @memberOf myfaces._impl.i18n
 */
_MF_CLS && _MF_CLS(_PFX_I18N + "Messages_ru", myfaces._impl.i18n.Messages,
        /** myfaces._impl.i18n.Messages_ru.prototype */
        {

            MSG_TEST:               "ТестовоеСообщение",

            /*Messages*/
            MSG_DEV_MODE:           "Это сообщение выдано, потому что 'project stage' было присоено значение 'development', и никаких" +
                    "других error listeners зарегистрировано не было.",
            MSG_AFFECTED_CLASS:     "Задействованный класс:",
            MSG_AFFECTED_METHOD:    "Задействованный метод:",

            MSG_ERROR_NAME:         "Имя ошибки:",
            MSG_ERROR_MESSAGE:      "Имя ошибки:",

            MSG_ERROR_DESC:         "Описание ошибки:",
            MSG_ERROR_NO:           "Номер ошибки:",
            MSG_ERROR_LINENO:       "Номер строки ошибки:",

            /*Errors and messages*/
            ERR_FORM:               "Sourceform не найдена, потому что элемент не находится внутри <form>, либо были найдены элементы <form> с рдинаковым именем или идентификатором. Обработка ajax остановлена",
            ERR_VIEWSTATE:          "jsf.viewState: Параметру присвоено значение, не являющееся элементом <form>!",
            ERR_TRANSPORT:          "Несуществующий тип транспорта {0}",
            ERR_EVT_PASS:           "Параметр event необходим, и не может быть null или undefined",
            ERR_CONSTRUCT:          "Часть ответа не удалось прочитать при создании данных события: {0} ",
            ERR_MALFORMEDXML:       "Ответ сервера не может быть обработан, он не в формате xml !",
            ERR_SOURCE_FUNC:        "source не может быть функцией (возможно, для source и event не были даны значения",
            ERR_EV_OR_UNKNOWN:      "Объект event или unknown должен быть всторым параметром",
            ERR_SOURCE_NOSTR:       "source не может быть типа string",
            ERR_SOURCE_DEF_NULL:    "source должно быть присвоено значение или null",

            //_Lang.js
            ERR_MUST_STRING:        "{0}: {1} namespace должно быть типа String",
            ERR_REF_OR_ID:          "{0}: {1} a Ссылочный узел (reference node) или идентификатор необходимы",
            ERR_PARAM_GENERIC:      "{0}: параметр {1} должен быть типа {2}",
            ERR_PARAM_STR:          "{0}: {1} параметр должен быть типа string",
            ERR_PARAM_STR_RE:       "{0}: {1} параметр должен быть типа string string или regular expression",
            ERR_PARAM_MIXMAPS:      "{0}: source b destination map необходимы",
            ERR_MUST_BE_PROVIDED:   "{0}: {1} и {2} необходимы",
            ERR_MUST_BE_PROVIDED1:  "{0}: {1} должно быть присвоено значение",

            ERR_REPLACE_EL:         "replaceElements вызвана, с evalNodes, не являющимся массивом",
            ERR_EMPTY_RESPONSE:     "{0}: Ответ не может бвть null или пустым!",
            ERR_ITEM_ID_NOTFOUND:   "{0}: Элемент с идентификатором {1} не найден",
            ERR_PPR_IDREQ:          "{0}: Ошибка в PPR Insert, id необходим",
            ERR_PPR_INSERTBEFID:    "{0}: Ошибка в PPR Insert, before id или after id необходимы",
            ERR_PPR_INSERTBEFID_1:  "{0}: Ошибка в PPR Insert, before node c id {1} не найден в документе",
            ERR_PPR_INSERTBEFID_2:  "{0}: Ошибка в PPR Insert, after node с id {1} не найден в документе",

            ERR_PPR_DELID:          "{0}: Ошибка в удалении, id не найден в xml документе",
            ERR_PPR_UNKNOWNCID:     "{0}: Неопознанный Html-Component-ID: {1}",
            ERR_NO_VIEWROOTATTR:    "{0}: Изменение атрибутов ViewRoot не предусмотрено",
            ERR_NO_HEADATTR:        "{0}: Изменение атрибутов Head не предусмотрено",
            ERR_RED_URL:            "{0}: Перенаправление (Redirect) без url"

        });
}