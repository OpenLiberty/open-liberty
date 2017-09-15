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
 * System messages german version
 * (note the base version is basically the en_US) version
 * of all messages
 * <p />
 * We use inheritance to overide the default messages with our
 * german one, variants can derive from the german one (like
 * suisse which do not have the emphasized s)
 * <p />
 * By using inheritance we can be sure that we fall back to the default one
 * automatically and that our variants only have to override the parts
 * which have changed from the baseline
 *
 * @class
 * @name Messages_de
 * @extends myfaces._impl.i18n.Messages
 * @memberOf myfaces._impl.i18n
 */

_MF_CLS && _MF_CLS(_PFX_I18N + "Messages_de", myfaces._impl.i18n.Messages,
        /** @lends myfaces._impl.i18n.Messages_de.prototype */
        {

            MSG_TEST:               "Testnachricht",

            /*Messages*/
            MSG_DEV_MODE:           "Sie sehen diese Nachricht, da sie sich gerade im Entwicklungsmodus befinden " +
                    "und sie keine Fehlerbehandlungsfunktionen registriert haben.",

            MSG_AFFECTED_CLASS:     "Klasse:",
            MSG_AFFECTED_METHOD:    "Methode:",

            MSG_ERROR_NAME:         "Fehler Name:",
            MSG_ERROR_MESSAGE:      "Nachricht:",
            MSG_SERVER_ERROR_NAME:  "Server Fehler Name:",

            MSG_ERROR_DESC:         "Fehlerbeschreibung:",
            MSG_ERROR_NO:           "Fehlernummer:",
            MSG_ERROR_LINENO:       "Zeilennummer:",

            /*Errors and messages*/
            ERR_FORM:                "Das Quellformular konnte nicht gefunden werden. " +
                    "Mögliche Gründe: Sie haben entweder kein formular definiert, oder es kommen mehrere Formulare vor, " +
                    "die alle das auslösende Element mit demselben Namen besitzen. " +
                    "Die Weitere Ajax Ausführung wird gestoppt.",

            ERR_VIEWSTATE:          "jsf.viewState: der Parameter ist not vom Typ form!",

            ERR_TRANSPORT:          "Transport typ {0} existiert nicht",
            ERR_EVT_PASS:           "Ein Event Objekt muss übergeben werden (entweder ein event Objekt oder null oder undefined)",
            ERR_CONSTRUCT:          "Teile des response konnten nicht ermittelt werden während die Event Daten bearbeitet wurden: {0} ",
            ERR_MALFORMEDXML:       "Es gab zwar eine Antwort des Servers, jedoch war diese nicht im erwarteten XML Format. Der Server hat kein valides XML gesendet! Bearbeitung abgebrochen.",
            ERR_SOURCE_FUNC:        "source darf keine Funktion sein",
            ERR_EV_OR_UNKNOWN:      "Ein Ereignis Objekt oder UNKNOWN muss als 2. Parameter übergeben werden",
            ERR_SOURCE_NOSTR:       "source darf kein String sein",
            ERR_SOURCE_DEF_NULL:    "source muss entweder definiert oder null sein",

            //_Lang.js
            ERR_MUST_STRING:        "{0}: {1} namespace muss vom Typ String sein",
            ERR_REF_OR_ID:          "{0}: {1} Ein Referenzknoten oder id muss übergeben werden",
            ERR_PARAM_GENERIC:      "{0}: Paramter {1} muss vom Typ {2} sein",
            ERR_PARAM_STR:          "{0}: Parameter {1} muss vom Typ String sein",
            ERR_PARAM_STR_RE:       "{0}: Parameter {1} muss entweder ein String oder ein Regulärer Ausdruck sein",
            ERR_PARAM_MIXMAPS:      "{0}: both a source as well as a destination map must be provided",
            ERR_MUST_BE_PROVIDED:   "{0}: ein {1} und ein {2} müssen übergeben werden",
            ERR_MUST_BE_PROVIDED1:  "{0}: {1} muss gesetzt sein",

            ERR_REPLACE_EL:         "replaceElements aufgerufen während evalNodes nicht ein Array ist",
            ERR_EMPTY_RESPONSE:     "{0}: Die Antwort darf nicht null oder leer sein!",
            ERR_ITEM_ID_NOTFOUND:   "{0}: Element mit ID {1} konnte nicht gefunden werden",
            ERR_PPR_IDREQ:          "{0}: Fehler im PPR Insert, ID muss gesetzt sein",
            ERR_PPR_INSERTBEFID:    "{0}: Fehler im PPR Insert, before ID oder after ID muss gesetzt sein",
            ERR_PPR_INSERTBEFID_1:  "{0}: Fehler im PPR Insert, before  Knoten mit ID {1} Existiert nicht",
            ERR_PPR_INSERTBEFID_2:  "{0}: Fehler im PPR Insert, after  Knoten mit ID {1} Existiert nicht",

            ERR_PPR_DELID:          "{0}: Fehler im PPR delete, id ist nicht im xml Markup vorhanden",
            ERR_PPR_UNKNOWNCID:     "{0}: Unbekannte Html-Komponenten-ID: {1}",
            ERR_NO_VIEWROOTATTR:    "{0}: Änderung von ViewRoot Attributen ist nicht erlaubt",
            ERR_NO_HEADATTR:        "{0}: Änderung von Head Attributen ist nicht erlaubt",
            ERR_RED_URL:            "{0}: Redirect ohne URL",

            ERR_REQ_FAILED_UNKNOWN: "Anfrage mit unbekanntem Status fehlgeschlagen",
            ERR_REQU_FAILED: "Anfrage mit Status {0} and Ursache {1} fehlgeschlagen",
            UNKNOWN: "Unbekannt",
            ERR_NO_MULTIPART_FORM: "Das Form Element mit der ID {0} hat ein Fileupload Feld aber ist kein Multipart Form"

        });

