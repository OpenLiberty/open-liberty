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
     * System messages french version version
     * (note the base version is basically the en) version
     * of all messages
     *
     * @class
     * @name Messages_fr
     * @extends myfaces._impl.i18n.Messages
     * @memberOf myfaces._impl.i18n
     */
_MF_CLS && _MF_CLS(_PFX_I18N + "Messages_fr", myfaces._impl.i18n.Messages,
            /** @lends myfaces._impl.i18n.Messages_fr.prototype */
            {
                MSG_TEST:               "MessageTest FR",

                /*Messages*/
                MSG_DEV_MODE:           "Note : ce message n'est envoyé que parce que le projet est au stade de développement et " +
                        "qu'aucun autre listener d'erreurs n'est enregistré.",
                MSG_AFFECTED_CLASS:     "Classe affectée : ",
                MSG_AFFECTED_METHOD:    "Méthode affectée : ",

                MSG_ERROR_NAME:         "Nom de l'erreur : ",
                MSG_ERROR_MESSAGE:      "Nom de l'erreur : ",

                MSG_ERROR_DESC:         "Description de l'erreur : ",
                MSG_ERROR_NO:           "Numéro de l'erreur : ",
                MSG_ERROR_LINENO:       "Erreur à la ligne : ",

                /*Errors and messages*/
                ERR_FORM:               "Le formulaire source n'a pas pu être déterminé, soit parce que l'élément n'est rattaché à aucun formulaire, soit parce qu'ils y a plusieurs formulaires contenant des éléments avec le même nom ou identifiant. Arrêt du traitement AJAX",
                ERR_VIEWSTATE:          "jsf.viewState: La valeur de 'param' n'est pas de type 'form' !",
                ERR_TRANSPORT:          "Le type de tansport {0} n'existe pas",
                ERR_EVT_PASS:           "Un évènement doit être transmis (soit un objet évènement, soit null ou undefined) ",
                ERR_CONSTRUCT:          "Des éléments de la réponse n'ont pu être récupérés lors de la construction des données de l'évènement : {0} ",
                ERR_MALFORMEDXML:       "La réponse du serveur n'a pas pu être analysée : le serveur n'a pas renvoyé une réponse en xml !",
                ERR_SOURCE_FUNC:        "La source ne peut pas être une fonction (Il est probable que 'source' et 'event' n'ont pas été définis ou mis à null",
                ERR_EV_OR_UNKNOWN:      "Le second paramètre doit être un objet évènement ou 'unknown' ",
                ERR_SOURCE_NOSTR:       "La source ne peut pas être de type String",
                ERR_SOURCE_DEF_NULL:    "La source doit être définie ou égale à null",

                //_Lang.js
                ERR_MUST_STRING:        "{0}: Le namespace {1} doit être de type String",
                ERR_REF_OR_ID:          "{0}: {1} un noeud de référence ou un identifiant doit être passé",
                ERR_PARAM_GENERIC:      "{0}: Le paramètre {1} doit être de type {2}",
                ERR_PARAM_STR:          "{0}: Le paramètre {1} doit être de type String",
                ERR_PARAM_STR_RE:       "{0}: Le paramètre {1} doit être de type String ou être une expression régulière",
                ERR_PARAM_MIXMAPS:      "{0}: Un Map de source et un Map de destination doivent être passés",
                ERR_MUST_BE_PROVIDED:   "{0}: un(e) {1} et un(e) {2} doivent être passés",
                ERR_MUST_BE_PROVIDED1:  "{0}: {1} doit être défini",

                ERR_REPLACE_EL:         "replaceElements a été appelé alors que evalNodes n'est pas un tableau",
                ERR_EMPTY_RESPONSE:     "{0}: La réponse ne peut pas être nulle ou vide !",
                ERR_ITEM_ID_NOTFOUND:   "{0}: l'élément portant l'identifiant {1} n'a pas pu être trouvé",
                ERR_PPR_IDREQ:          "{0}: Erreur lors de l'insertion PPR, l'id doit être présent",
                ERR_PPR_INSERTBEFID:    "{0}: Erreur lors de l'insertion PPR, 'before id' ou 'after id' doivent être présents",
                ERR_PPR_INSERTBEFID_1:  "{0}: Erreur lors de l'insertion PPR, le noeud before de l'id {1} n'existe pas dans le document",
                ERR_PPR_INSERTBEFID_2:  "{0}: Erreur lors de l'insertion PPR, le noeud after  de l'id {1} n'existe pas dans le document",

                ERR_PPR_DELID:          "{0}: Erreur lors de la suppression, l'id n'est pas présent dans le xml",
                ERR_PPR_UNKNOWNCID:     "{0}:  Html-Component-ID inconnu : {1}",
                ERR_NO_VIEWROOTATTR:    "{0}: Le changement d'attributs dans ViewRoot n'est pas supporté",
                ERR_NO_HEADATTR:        "{0}: Le changement d'attributs dans Head n'est pas supporté",
                ERR_RED_URL:            "{0}: Redirection sans url"
            });
