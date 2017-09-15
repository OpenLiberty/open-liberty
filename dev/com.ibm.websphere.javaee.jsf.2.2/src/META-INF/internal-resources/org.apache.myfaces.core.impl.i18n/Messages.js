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
 * @memberOf myfaces._impl
 * @namespace
 * @name i18n
 */

/**
 * System messages base version    <p />
 * (note the base version is basically the en_US) version
 * of all messages
 *
 * @class
 * @name Messages
 * @memberOf myfaces._impl.i18n
 */
_MF_CLS && _MF_CLS(_PFX_I18N+"Messages", Object,
/**
 * @lends myfaces._impl.i18n.Messages.prototype
 */
{

    MSG_TEST:               "Testmessage",

    /*Messages*/
    /** @constant */
    MSG_DEV_MODE:           "Note, this message is only sent, because project stage is development and no " +
                            "other error listeners are registered.",
    /** @constant */
    MSG_AFFECTED_CLASS:     "Affected Class:",
    /** @constant */
    MSG_AFFECTED_METHOD:    "Affected Method:",
    /** @constant */
    MSG_ERROR_NAME:         "Error Name:",
    /** @constant */
    MSG_ERROR_MESSAGE:      "Error Message:",
    /** @constant */
    MSG_SERVER_ERROR_NAME:  "Server Error Name:",

    /** @constant */
    MSG_ERROR_DESC:         "Error Description:",
    /** @constant */
    MSG_ERROR_NO:           "Error Number:",
    /** @constant */
    MSG_ERROR_LINENO:       "Error Line Number:",

    /*Errors and messages*/
    /** @constant */
    ERR_FORM:               "Sourceform could not be determined, either because element is not attached to a form or we have multiple forms with named elements of the same identifier or name, stopping the ajax processing",
    /** @constant */
    ERR_VIEWSTATE:          "jsf.viewState: param value not of type form!",
    /** @constant */
    ERR_TRANSPORT:          "Transport type {0} does not exist",
    /** @constant */
    ERR_EVT_PASS:           "an event must be passed down (either a an event object null or undefined) ",
    /** @constant */
    ERR_CONSTRUCT:          "Parts of the response couldn't be retrieved when constructing the event data: {0} ",
    /** @constant */
    ERR_MALFORMEDXML:       "The server response could not be parsed, the server has returned with a response which is not xml !",
    /** @constant */
    ERR_SOURCE_FUNC:        "source cannot be a function (probably source and event were not defined or set to null",
    /** @constant */
    ERR_EV_OR_UNKNOWN:      "An event object or unknown must be passed as second parameter",
    /** @constant */
    ERR_SOURCE_NOSTR:       "source cannot be a string",
    /** @constant */
    ERR_SOURCE_DEF_NULL:    "source must be defined or null",

    //_Lang.js
    /** @constant */
    ERR_MUST_STRING:        "{0}: {1} namespace must be of type String",
    /** @constant */
    ERR_REF_OR_ID:          "{0}: {1} a reference node or identifier must be provided",
    /** @constant */
    ERR_PARAM_GENERIC:      "{0}: parameter {1} must be of type {2}",
    /** @constant */
    ERR_PARAM_STR:          "{0}: {1} param must be of type string",
    /** @constant */
    ERR_PARAM_STR_RE:       "{0}: {1} param must be of type string or a regular expression",
    /** @constant */
    ERR_PARAM_MIXMAPS:      "{0}: both a source as well as a destination map must be provided",
    /** @constant */
    ERR_MUST_BE_PROVIDED:   "{0}: an {1} and a {2} must be provided",
    /** @constant */
    ERR_MUST_BE_PROVIDED1:  "{0}: {1} must be set",

    /** @constant */
    ERR_REPLACE_EL:         "replaceElements called while evalNodes is not an array",

    /** @constant */
    ERR_EMPTY_RESPONSE:     "{0}: The response cannot be null or empty!",
    /** @constant */
    ERR_ITEM_ID_NOTFOUND:   "{0}: item with identifier {1} could not be found",
    /** @constant */
    ERR_PPR_IDREQ:          "{0}: Error in PPR Insert, id must be present",
    /** @constant */
    ERR_PPR_INSERTBEFID:    "{0}: Error in PPR Insert, before id or after id must be present",
    /** @constant */
    ERR_PPR_INSERTBEFID_1:  "{0}: Error in PPR Insert, before  node of id {1} does not exist in document",
    /** @constant */
    ERR_PPR_INSERTBEFID_2:  "{0}: Error in PPR Insert, after  node of id {1} does not exist in document",

    /** @constant */
    ERR_PPR_DELID:          "{0}: Error in delete, id not in xml markup",
    /** @constant */
    ERR_PPR_UNKNOWNCID:     "{0}:  Unknown Html-Component-ID: {1}",

    /** @constant */
    ERR_NO_VIEWROOTATTR:    "{0}: Changing of ViewRoot attributes is not supported",
    /** @constant */
    ERR_NO_HEADATTR:        "{0}: Changing of Head attributes is not supported",
    /** @constant */
    ERR_RED_URL:            "{0}: Redirect without url",

    /** @constant */
    ERR_REQ_FAILED_UNKNOWN: "Request failed with unknown status",

    /** @constant */
    ERR_REQU_FAILED: "Request failed with status {0} and reason {1}",

    /** @constant */
    UNKNOWN: "UNKNOWN",

    ERR_NO_MULTIPART_FORM: "The form with the id {0} has an input file element, but is not a multipart form"
});

