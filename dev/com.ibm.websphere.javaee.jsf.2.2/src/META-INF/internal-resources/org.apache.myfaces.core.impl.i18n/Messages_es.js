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


/**
 * System messages spanish version version
 * (note the base version is basically the en_US) version
 * of all messages
 *
 * @class
 * @name Messages_es
 * @extends myfaces._impl.i18n.Messages
 * @memberOf myfaces._impl.i18n
 */

_MF_CLS && _MF_CLS(_PFX_I18N + "Messages_es", myfaces._impl.i18n.Messages,
        /** @lends myfaces._impl.i18n.Messages_es.prototype */
        {


            MSG_TEST:               "Mensajeprueba",

            /*Messages*/
            MSG_DEV_MODE:           "Aviso. Este mensaje solo se envia porque el 'Project Stage' es 'Development' y no hay otros 'listeners' de errores registrados.",
            MSG_AFFECTED_CLASS:     "Clase Afectada:",
            MSG_AFFECTED_METHOD:    "M�todo Afectado:",

            MSG_ERROR_NAME:         "Nombre del Error:",
            MSG_ERROR_MESSAGE:      "Mensaje del Error:",
            MSG_SERVER_ERROR_NAME:  "Mensaje de error de servidor:",

            MSG_ERROR_DESC:         "Descripci�n del Error:",
            MSG_ERROR_NO:           "N�mero de Error:",
            MSG_ERROR_LINENO:       "N�mero de L�nea del Error:",

            /*Errors and messages*/
            ERR_FORM:               "El formulario de origen no ha podido ser determinado, debido a que el elemento no forma parte de un formulario o hay diversos formularios con elementos usando el mismo nombre o identificador. Parando el procesamiento de Ajax.",
            ERR_VIEWSTATE:          "jsf.viewState: el valor del par�metro no es de tipo 'form'!",
            ERR_TRANSPORT:          "El tipo de transporte {0} no existe",
            ERR_EVT_PASS:           "un evento debe ser transmitido (sea null o no definido)",
            ERR_CONSTRUCT:          "Partes de la respuesta no pudieron ser recuperadas cuando construyendo los datos del evento: {0} ",
            ERR_MALFORMEDXML:       "La respuesta del servidor no ha podido ser interpretada. El servidor ha devuelto una respuesta que no es xml !",
            ERR_SOURCE_FUNC:        "el origen no puede ser una funci�n (probablemente 'source' y evento no han sido definidos o son 'null'",
            ERR_EV_OR_UNKNOWN:      "Un objeto de tipo evento o desconocido debe ser pasado como segundo par�metro",
            ERR_SOURCE_NOSTR:       "el origen no puede ser 'string'",
            ERR_SOURCE_DEF_NULL:    "el origen debe haber sido definido o ser 'null'",

            //_Lang.js
            ERR_MUST_STRING:        "{0}: {1} namespace debe ser de tipo String",
            ERR_REF_OR_ID:          "{0}: {1} una referencia a un nodo o identificador tiene que ser pasada",
            ERR_PARAM_GENERIC:      "{0}: el par�metro {1} tiene que ser de tipo {2}",
            ERR_PARAM_STR:          "{0}: el par�metro {1} tiene que ser de tipo string",
            ERR_PARAM_STR_RE:       "{0}: el par�metro {1} tiene que ser de tipo string o una expresi�n regular",
            ERR_PARAM_MIXMAPS:      "{0}: han de ser pasados tanto un origen como un destino",
            ERR_MUST_BE_PROVIDED:   "{0}: {1} y {2} deben ser pasados",
            ERR_MUST_BE_PROVIDED1:  "{0}: {1} debe estar definido",

            ERR_REPLACE_EL:         "replaceElements invocado mientras que evalNodes no es un an array",
            ERR_EMPTY_RESPONSE:     "{0}: �La respuesta no puede ser de tipo 'null' o vac�a!",
            ERR_ITEM_ID_NOTFOUND:   "{0}: el elemento con identificador {1} no ha sido encontrado",
            ERR_PPR_IDREQ:          "{0}: Error en PPR Insert, 'id' debe estar presente",
            ERR_PPR_INSERTBEFID:    "{0}: Error in PPR Insert, antes de 'id' o despu�s de 'id' deben estar presentes",
            ERR_PPR_INSERTBEFID_1:  "{0}: Error in PPR Insert, antes de nodo con id {1} no existe en el documento",
            ERR_PPR_INSERTBEFID_2:  "{0}: Error in PPR Insert, despu�s de nodo con id {1} no existe en el documento",

            ERR_PPR_DELID:          "{0}: Error durante borrado, id no presente en xml",
            ERR_PPR_UNKNOWNCID:     "{0}:  Desconocido Html-Component-ID: {1}",
            ERR_NO_VIEWROOTATTR:    "{0}: El cambio de atributos de ViewRoot attributes no es posible",
            ERR_NO_HEADATTR:        "{0}: El cambio de los atributos de Head attributes no es posible",
            ERR_RED_URL:            "{0}: Redirecci�n sin url",

            ERR_REQ_FAILED_UNKNOWN: "La petici�n ha fallado con estado desconocido",
            ERR_REQU_FAILED:        "La petici�n ha fallado con estado {0} y raz�n {1}",
            UNKNOWN:                "DESCONOCIDO"

        });


