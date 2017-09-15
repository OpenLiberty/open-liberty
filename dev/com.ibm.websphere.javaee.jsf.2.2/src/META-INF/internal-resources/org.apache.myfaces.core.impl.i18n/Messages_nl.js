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
 * System messages dutch version
 *
 * @class
 * @name Messages_nl
 * @extends myfaces._impl.i18n.Messages
 * @memberOf myfaces._impl.i18n
 */
_MF_CLS && _MF_CLS(_PFX_I18N + "Messages_nl", myfaces._impl.i18n.Messages,
        /** @lends myfaces._impl.i18n.Messages_nl.prototype */
        {

            MSG_TEST:               "Testbericht",

            /*Messages*/
            MSG_DEV_MODE:           "Opmerking, dit bericht is enkel gestuurd omdat het project stadium develoment is en er geen " +
                    "andere listeners zijn geconfigureerd.",
            MSG_AFFECTED_CLASS:     "Betrokken Klasse:",
            MSG_AFFECTED_METHOD:    "Betrokken Methode:",

            MSG_ERROR_NAME:         "Naam foutbericht:",
            MSG_ERROR_MESSAGE:      "Naam foutbericht:",

            MSG_ERROR_DESC:         "Omschrijving fout:",
            MSG_ERROR_NO:           "Fout nummer:",
            MSG_ERROR_LINENO:       "Fout lijn nummer:",

            /*Errors and messages*/
            ERR_FORM:               "De doel form kon niet bepaald worden, ofwel omdat het element niet tot een form behoort, ofwel omdat er verschillende forms zijn met 'named element' met dezelfde identifier of naam, ajax verwerking is gestopt.",
            ERR_VIEWSTATE:          "jsf.viewState: param waarde is niet van het type form!",
            ERR_TRANSPORT:          "Transport type {0} bestaat niet",
            ERR_EVT_PASS:           "een event moet opgegegevn worden (ofwel een event object null of undefined) ",
            ERR_CONSTRUCT:          "Delen van het antwoord konden niet opgehaald worden bij het aanmaken van de event data: {0} ",
            ERR_MALFORMEDXML:       "Het antwoordt van de server kon niet ontleed worden, de server heeft een antwoord gegeven welke geen xml bevat!",
            ERR_SOURCE_FUNC:        "source kan geen functie zijn (waarschijnlijk zijn source en event niet gedefinieerd of kregen de waarde null)",
            ERR_EV_OR_UNKNOWN:      "Een event object of 'unknown' moet gespecifieerd worden als tweede parameter",
            ERR_SOURCE_NOSTR:       "source kan geen string zijn",
            ERR_SOURCE_DEF_NULL:    "source moet gedefinieerd zijn of null bevatten",

            //_Lang.js
            ERR_MUST_STRING:        "{0}: {1} namespace moet van het type String zijn",
            ERR_REF_OR_ID:          "{0}: {1} een referentie node of identifier moet opgegeven worden",
            ERR_PARAM_GENERIC:      "{0}: parameter {1} moet van het type {2} zijn",
            ERR_PARAM_STR:          "{0}: {1} parameter moet van het type string zijn",
            ERR_PARAM_STR_RE:       "{0}: {1} parameter moet van het type string zijn of een reguliere expressie",
            ERR_PARAM_MIXMAPS:      "{0}: zowel source als destination map moeten opgegeven zijn",
            ERR_MUST_BE_PROVIDED:   "{0}: een {1} en een {2} moeten opgegeven worden",
            ERR_MUST_BE_PROVIDED1:  "{0}: {1} moet gezet zijn",

            ERR_REPLACE_EL:         "replaceElements opgeroepen maar evalNodes is geen array",
            ERR_EMPTY_RESPONSE:     "{0}: Het antwoord kan geen null of leeg zijn!",
            ERR_ITEM_ID_NOTFOUND:   "{0}: item met identifier {1} kan niet gevonden worden",
            ERR_PPR_IDREQ:          "{0}: Fout in PPR Insert, id moet bestaan",
            ERR_PPR_INSERTBEFID:    "{0}: Fout in PPR Insert, before id of after id moet bestaan",
            ERR_PPR_INSERTBEFID_1:  "{0}: Fout in PPR Insert, before node van id {1} bestaat niet in het document",
            ERR_PPR_INSERTBEFID_2:  "{0}: Fout in PPR Insert, after node van id {1} bestaat niet in het document",

            ERR_PPR_DELID:          "{0}: Fout in delete, id is niet in de xml markup",
            ERR_PPR_UNKNOWNCID:     "{0}: Onbekende Html-Component-ID: {1}",
            ERR_NO_VIEWROOTATTR:    "{0}: Wijzigen van ViewRoot attributen is niet ondersteund",
            ERR_NO_HEADATTR:        "{0}: Wijzigen van Head attributen is niet ondersteund",
            ERR_RED_URL:            "{0}: Redirect zonder url",

            ERR_REQ_FAILED_UNKNOWN: "Request mislukt met onbekende status",
            ERR_REQU_FAILED:        "Request mislukt met status {0} en reden {1}",
            UNKNOWN:                "ONBEKEND"

        });
