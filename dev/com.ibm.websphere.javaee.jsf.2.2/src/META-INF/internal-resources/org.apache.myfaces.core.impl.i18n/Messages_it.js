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
 * System messages italian version version
 * (note the base version is basically the en_US) version
 * of all messages
 *
 * @class
 * @name Messages_it
 * @extends myfaces._impl.i18n.Messages
 * @memberOf myfaces._impl.i18n
 */
_MF_CLS && _MF_CLS(_PFX_I18N + "Messages_it", myfaces._impl.i18n.Messages,
        /** @lends myfaces._impl.i18n.Messages_it.prototype */
        {
            /*Messages*/
            MSG_DEV_MODE:           "Questo messaggio � stato inviato esclusivamente perch� il progetto � in development stage e nessun altro listener � stato registrato.",
            MSG_AFFECTED_CLASS:     "Classi coinvolte:",
            MSG_AFFECTED_METHOD:    "Metodi coinvolti:",

            MSG_ERROR_NAME:         "Nome dell'errore:",
            MSG_ERROR_MESSAGE:      "Nome dell'errore:",

            MSG_ERROR_DESC:         "Descrizione dell'errore:",
            MSG_ERROR_NO:           "Numero errore:",
            MSG_ERROR_LINENO:       "Numero di riga dell'errore:",

            /*Errors and messages*/
            ERR_FORM:               "Il Sourceform non puo' essere determinato a causa di una delle seguenti ragioni: l'elemento non e' agganciato ad un form oppure sono presenti pi� form con elementi con lo stesso nome, il che blocca l'elaborazione ajax",
            ERR_VIEWSTATE:          "jsf.viewState: il valore del parametro non � di tipo form!",
            ERR_TRANSPORT:          "Il transport type {0} non esiste",
            ERR_EVT_PASS:           "� necessario passare un evento (sono accettati anche gli event object null oppure undefined) ",
            ERR_CONSTRUCT:          "Durante la costruzione dell' event data: {0} non � stato possibile acquisire alcune parti della response ",
            ERR_MALFORMEDXML:       "Il formato della risposta del server non era xml, non � stato quindi possibile effettuarne il parsing!",
            ERR_SOURCE_FUNC:        "source non puo' essere una funzione (probabilmente source and event non erano stati definiti o sono null",
            ERR_EV_OR_UNKNOWN:      "Come secondo parametro bisogna passare un event object oppure unknown",
            ERR_SOURCE_NOSTR:       "source non pu� essere una stringa di testo",
            ERR_SOURCE_DEF_NULL:    "source deve essere definito oppure  null",

            //_Lang.js
            ERR_MUST_STRING:        "{0}: {1} namespace deve essere di tipo String",
            ERR_REF_OR_ID:          "{0}: {1} un reference node oppure un identificatore deve essere fornito",
            ERR_PARAM_GENERIC:      "{0}: il parametro {1} deve essere di tipo {2}",
            ERR_PARAM_STR:          "{0}: {1} parametro deve essere di tipo String",
            ERR_PARAM_STR_RE:       "{0}: {1} parametro deve essere di tipo String oppure una regular expression",
            ERR_PARAM_MIXMAPS:      "{0}: � necessario specificare sia  source che destination map",
            ERR_MUST_BE_PROVIDED:   "{0}: � necessario specificare sia {1} che {2} ",
            ERR_MUST_BE_PROVIDED1:  "{0}: {1} deve essere settato",

            ERR_REPLACE_EL:         "replaceElements chiamato metre evalNodes non � un array",
            ERR_EMPTY_RESPONSE:     "{0}: La response non puo' essere nulla o vuota!",
            ERR_ITEM_ID_NOTFOUND:   "{0}: non � stato trovato alcun item con identificativo {1}",
            ERR_PPR_IDREQ:          "{0}: Errore durante la PPR Insert, l' id deve essere specificato",
            ERR_PPR_INSERTBEFID:    "{0}: Errore durante la PPR Insert, before id o after id deve essere specificato",
            ERR_PPR_INSERTBEFID_1:  "{0}: Errore durante la PPR Insert, before node of id {1} non esiste nel document",
            ERR_PPR_INSERTBEFID_2:  "{0}: Errore durante la PPR Insert, after  node of id {1} non esiste nel in document",

            ERR_PPR_DELID:          "{0}: Errore durante la delete, l'id non e' nella forma di un markup xml",
            ERR_PPR_UNKNOWNCID:     "{0}:   Html-Component-ID: {1} sconosciuto",
            ERR_NO_VIEWROOTATTR:    "{0}: La modifica degli attributi del ViewRoot non � supportata",
            ERR_NO_HEADATTR:        "{0}: La modifica degli attributi di Head non � supportata",
            ERR_RED_URL:            "{0}: Redirect senza url"
        });

