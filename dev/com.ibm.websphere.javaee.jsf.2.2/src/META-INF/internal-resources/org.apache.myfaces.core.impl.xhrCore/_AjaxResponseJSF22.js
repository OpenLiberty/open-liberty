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
 * @class
 * @name _AjaxResponse
 * @memberOf myfaces._impl.xhrCore
 * @extends myfaces._impl.core.Object
 * @description
 * This singleton is responsible for handling the standardized xml ajax response
 * Note: since the semantic processing can be handled about 90% in a functional
 * style we make this class stateless. Every state information is stored
 * temporarily in the context.
 *
 * The singleton approach also improves performance
 * due to less object gc compared to the old instance approach.
 *
 * This is a preparation to the JSF 2.2 specification, which implements the latest spec
 * proposals in the area,
 * the new viestate syntax is
 * &lt;<update id="<naming container id>:javax.faces.ViewState"> ... </update>
 *
 * with the naming container id being the update viewroot, if none is present
 * then the viewstate updates all forms in the page.
 * The render targets are of no usage regarding the viewstate determination anymore
 *
 * We use a classical javascript private method pattern here to improve compressability over
 * our old singleton approach, this makes sense since only one of our methods is exposed
 */
if (!myfaces._impl.core._Runtime.fetchNamespace(_PFX_XHR + "_AjaxResponseJSF22")) {

    myfaces._impl.core._Runtime.reserveNamespace(_PFX_XHR + "_AjaxResponseJSF22", new function () {
        /** @lends myfaces._impl.xhrCore._AjaxResponseJSF22.prototype */
        /*partial response types*/
        var RESP_PARTIAL = "partial-response";

        /*partial commands*/
        var CMD_CHANGES = "changes",
                CMD_UPDATE = "update",
                CMD_DELETE = "delete",
                CMD_INSERT = "insert",
                CMD_EVAL = "eval",
                CMD_ERROR = "error",
                CMD_ATTRIBUTES = "attributes",
                CMD_EXTENSION = "extension",
                CMD_REDIRECT = "redirect";

        /*other constants*/
        var P_VIEWSTATE = "javax.faces.ViewState",
        //client window handler for the client id
                P_CLIENTWINDOW = "javax.faces.ClientWindow",
                P_VIEWROOT = "javax.faces.ViewRoot",
                P_VIEWHEAD = "javax.faces.ViewHead",
                P_VIEWBODY = "javax.faces.ViewBody",
                _RT = myfaces._impl.core._Runtime,
                _Lang = myfaces._impl._util._Lang,
                _Dom = myfaces._impl._util._Dom;

        /**
         * uses response to start Html element replacement
         *
         * @param {Object} request (xhrRequest) - xhr request object
         * @param {Object} context (Map) - AJAX context
         *
         * A special handling has to be added to the update cycle
         * according to the JSDoc specs if the CDATA block contains html tags the outer rim must be stripped
         * if the CDATA block contains a head section the document head must be replaced
         * and if the CDATA block contains a body section the document body must be replaced!
         *
         */
        this.processResponse = function (request, context) {
            //mfinternal handling, note, the mfinternal is only optional
            //according to the spec
            context._mfInternal = context._mfInternal || {};
            var mfInternal = context._mfInternal;

            //the temporary data is hosted here
            mfInternal._updateElems = [];
            mfInternal._viewStateForms = [];
            mfInternal._clientWindowForms = [];
            //mfInternal.newValue = null;

            try {

                // TODO:
                // Solution from
                // http://www.codingforums.com/archive/index.php/t-47018.html
                // to solve IE error 1072896658 when a Java server sends iso88591
                // istead of ISO-8859-1

                if (!request || !_Lang.exists(request, "responseXML")) {
                    throw _Lang.makeException(new Error(), _getImpl().EMPTY_RESPONSE, _getImpl().EMPTY_RESPONSE, _nameSpace, "processResponse", "");
                }
                //check for a parseError under certain browsers

                var xmlContent = request.responseXML;
                //ie6+ keeps the parsing response under xmlContent.parserError
                //while the rest of the world keeps it as element under the first node
                var xmlErr = _Lang.fetchXMLErrorMessage(request.responseText || request.response, xmlContent);
                if (xmlErr) {
                    throw _raiseError(new Error(), xmlErr.errorMessage + "\n" + xmlErr.sourceText + "\n" + xmlErr.visualError + "\n", "processResponse");
                }
                var partials = xmlContent.childNodes[0];
                if ('undefined' == typeof partials || partials == null) {
                    throw _raiseError(new Error(), "No child nodes for response", "processResponse");

                } else {
                    if (partials.tagName != RESP_PARTIAL) {
                        // IE 8 sees XML Header as first sibling ...
                        partials = partials.nextSibling;
                        if (!partials || partials.tagName != RESP_PARTIAL) {
                            throw _raiseError(new Error(), "Partial response not set", "processResponse");
                        }
                    }
                }

                var childNodesLength = partials.childNodes.length;

                for (var loop = 0; loop < childNodesLength; loop++) {
                    var childNode = partials.childNodes[loop];
                    var tagName = childNode.tagName;
                    /**
                     * <eval>
                     *      <![CDATA[javascript]]>
                     * </eval>
                     */

                    //this ought to be enough for eval
                    //however the run scripts still makes sense
                    //in the update and insert area for components
                    //which do not use the response writer properly
                    //we might add this one as custom option in update and
                    //insert!
                    if (tagName == CMD_ERROR) {
                        _processError(request, context, childNode);
                    } else if (tagName == CMD_REDIRECT) {
                        _processRedirect(request, context, childNode);
                    } else if (tagName == CMD_CHANGES) {
                        _processChanges(request, context, childNode);
                    }
                }

                //fixup missing viewStates due to spec deficiencies
                _fixViewStates(context);

                //spec jsdoc, the success event must be sent from response
                _getImpl().sendEvent(request, context, _getImpl()["SUCCESS"]);

            } finally {
                delete mfInternal._updateElems;
                delete mfInternal._viewStateForms;
                delete mfInternal._clientWindowForms;
                //delete mfInternal.newValue;
            }
        };

        /**
         * fixes the viewstates in the current page
         *
         * @param context
         */
        var _fixViewStates = function (context) {
            var mfInternal = context._mfInternal;


            // Now update the forms that were not replaced but forced to be updated, because contains child ajax tags
            // we should only update forms with view state hidden field. If by some reason, the form was set to be
            // updated but the form was replaced, it does not have hidden view state, so later in changeTrace processing the
            // view state is updated.

            //set the viewstates of all outer forms parents of our updated elements
            if (mfInternal._viewStateForms && mfInternal._viewStateForms.length) {
                _Lang.arrForEach(mfInternal._viewStateForms, function (elem) {
                    _updateFormField(P_VIEWSTATE, context, elem);
                }, 0, this);
            }
            if(mfInternal._clientWindowForms && mfInternal._clientWindowForms.length) {
                _Lang.arrForEach(mfInternal._clientWindowForms, function (elem) {
                    _updateFormField(P_CLIENTWINDOW,context, elem);
                }, 0, this);
            }
        };

        /**
         * sets the viewstate element in a given form
         *
         * @param the fieldIdentifier to update either, P_VIEWSTATE or P_CLIENTWINDOW
         * @param theFormData the form to which the element has to be set to
         * @param context the current request context
         */
        var _updateFormField = function (fieldIdentifier,context, theFormData) {
            var form = _Lang.byId(theFormData.form);

            if (!theFormData) return;

            var fieldToUpdate = (form.elements) ? form.elements[fieldIdentifier] : null;

            if (fieldToUpdate) {
                _Dom.setAttribute(fieldToUpdate, "value", theFormData.newValue);
            } else if (!fieldToUpdate) {
                var element = _Dom.getDummyPlaceHolder();

                element.innerHTML = ["<input type='hidden'", "id='", theFormData.id, "' name='", fieldIdentifier , "' value='" , theFormData.newValue , "' />"].join("");
                //now we go to proper dom handling after having to deal with another ie screwup
                try {
                    form.appendChild(element.childNodes[0]);
                } finally {
                    //squelch
                    element.innerHTML = "";
                }
            }
        };



        /**
         * processes an incoming error from the response
         * which is hosted under the &lt;error&gt; tag
         * @param request the current request
         * @param context the contect object
         * @param node the node in the xml hosting the error message
         */
        var _processError = function (request, context, node) {
            /**
             * <error>
             *      <error-name>String</error-name>
             *      <error-message><![CDATA[message]]></error-message>
             * <error>
             */
            var errorName = node.firstChild.textContent || node.firstChild.text || "",
                    errorMessage = node.childNodes[1].firstChild.data || "";

            _getImpl().sendError(request, context, _getImpl().SERVER_ERROR, errorName, errorMessage, "myfaces._impl.xhrCore._AjaxResponse", "_processError");
        };

        var _getImpl = function () {
            return _RT.getGlobalConfig("jsfAjaxImpl", myfaces._impl.core.Impl);
        };

        /**
         * processes an incoming xml redirect directive from the ajax response
         * @param request the request object
         * @param context the context
         * @param node the node hosting the redirect data
         */
        var _processRedirect = function (request, context, node) {
            /**
             * <redirect url="url to redirect" />
             */
            var redirectUrl = node.getAttribute("url");
            if (!redirectUrl) {
                throw _raiseError(new Error(), _Lang.getMessage("ERR_RED_URL", null, "_AjaxResponse.processRedirect"), "_processRedirect");
            }
            redirectUrl = _Lang.trim(redirectUrl);
            if (redirectUrl == "") {
                return false;
            }
            window.location = redirectUrl;
            return true;
        };

        /**
         * main entry point for processing the changes
         * it deals with the &lt;changes&gt; node of the
         * response
         *
         * @param request the xhr request object
         * @param context the context map
         * @param node the changes node to be processed
         */
        var _processChanges = function (request, context, node) {
            var changes = node.childNodes;
            //note we need to trace the changes which could affect our insert update or delete
            //se that we can realign our ViewStates afterwards
            //the realignment must happen post change processing

            for (var i = 0; i < changes.length; i++) {

                switch (changes[i].tagName) {

                    case CMD_UPDATE:
                        _processUpdate(request, context, changes[i]);
                        break;
                    case CMD_EVAL:
                        _Lang.globalEval(changes[i].firstChild.data);
                        break;
                    case CMD_INSERT:
                        _processInsert(request, context, changes[i]);
                        break;
                    case CMD_DELETE:
                        _processDelete(request, context, changes[i]);
                        break;
                    case CMD_ATTRIBUTES:
                        _processAttributes(request, context, changes[i]);
                        break;
                    case CMD_EXTENSION:
                        break;
                    default:
                        throw _raiseError(new Error(), "_AjaxResponse.processChanges: Illegal Command Issued", "_processChanges");
                }
            }

            return true;
        };

        /**
         * First sub-step process a pending update tag
         *
         * @param request the xhr request object
         * @param context the context map
         * @param node the changes node to be processed
         */
        var _processUpdate = function (request, context, node) {
            var id = node.getAttribute('id');
            var viewStateIdx = id.indexOf(P_VIEWSTATE);
            var clientWindowIdx = id.indexOf(P_CLIENTWINDOW);
            var mfInternal = context._mfInternal;

            /**
             * applies either the viewstate or the clientWindowId
             * to the correct forms
             *
             * @param idx the idx identifier to search for, it is either P_VIEWSTATE or P_CLIENTIDs idx position
             * @param target target array receiving the results for further procession
             */
            var applyViewStWindowId = function(idx, target) {
                var viewRootEndIdx = Math.max(0, idx - 1);

                var viewRoot = (viewRootEndIdx) ? id.substr(0, viewRootEndIdx) : null;

                //update the submitting forms viewstate to the new value
                // The source form has to be pulled out of the CURRENT document first because the context object
                // may refer to an invalid document if an update of the entire body has occurred before this point.


                var rootElem = (viewRoot) ? document.getElementById(viewRoot) : document.body;
                var forms = _Dom.findByTagName(rootElem, "form");

                if (forms) {
                    for (var cnt = forms.length - 1; cnt >= 0; cnt--) {
                        target.push({
                            form:forms[cnt].id,
                            newValue:node.firstChild.nodeValue,
                            id:id +jsf.separatorchar + cnt
                        });
                    }
                }
            }


            if (viewStateIdx != -1) {
                applyViewStWindowId(viewStateIdx, mfInternal._viewStateForms);

            } else if (clientWindowIdx != -1) {
                applyViewStWindowId(clientWindowIdx, mfInternal._clientWindowForms);
            }
            else {
                // response may contain several blocks
                var cDataBlock = _Dom.concatCDATABlocks(node);

                switch (node.getAttribute('id')) {
                    case P_VIEWROOT:

                        cDataBlock = cDataBlock.substring(cDataBlock.indexOf("<html"));

                        var parsedData = _replaceHead(request, context, cDataBlock);

                        ('undefined' != typeof parsedData && null != parsedData) ? _replaceBody(request, context, cDataBlock, parsedData) : _replaceBody(request, context, cDataBlock);

                        break;
                    case P_VIEWHEAD:
                        //we cannot replace the head, almost no browser allows this, some of them throw errors
                        //others simply ignore it or replace it and destroy the dom that way!
                        _replaceHead(request, context, cDataBlock);

                        break;
                    case P_VIEWBODY:
                        //we assume the cdata block is our body including the tag
                        _replaceBody(request, context, cDataBlock);

                        break;

                    default:
                        _replaceHtmlItem(request, context, node.getAttribute('id'), cDataBlock);

                        break;
                }
            }

            return true;
        };

        /**
         * replaces a current head theoretically,
         * pratically only the scripts are evaled anew since nothing else
         * can be changed.
         *
         * @param request the current request
         * @param context the ajax context
         * @param newData the data to be processed
         *
         * @return an xml representation of the page for further processing if possible
         */
        var _replaceHead = function (request, context, newData) {

            var isWebkit = _RT.browser.isWebKit,
            //we have to work around an xml parsing bug in Webkit
            //see https://issues.apache.org/jira/browse/MYFACES-3061
                    doc = (!isWebkit) ? _Lang.parseXML(newData) : null,
                    newHead = null;

            if (!isWebkit && _Lang.isXMLParseError(doc)) {
                doc = _Lang.parseXML(newData.replace(/<!\-\-[\s\n]*<!\-\-/g, "<!--").replace(/\/\/-->[\s\n]*\/\/-->/g, "//-->"));
            }

            if (isWebkit || _Lang.isXMLParseError(doc)) {
                //the standard xml parser failed we retry with the stripper
                var parser = new (_RT.getGlobalConfig("updateParser", myfaces._impl._util._HtmlStripper))();
                var headData = parser.parse(newData, "head");
                //We cannot avoid it here, but we have reduced the parsing now down to the bare minimum
                //for further processing
                newHead = _Lang.parseXML("<head>" + headData + "</head>");
                //last and slowest option create a new head element and let the browser
                //do its slow job
                if (_Lang.isXMLParseError(newHead)) {
                    try {
                        newHead = _Dom.createElement("head");
                        newHead.innerHTML = headData;
                    } catch (e) {
                        //we give up no further fallbacks
                        throw _raiseError(new Error(), "Error head replacement failed reason:" + e.toString(), "_replaceHead");
                    }
                }
            } else {
                //parser worked we go on
                newHead = doc.getElementsByTagName("head")[0];
            }

            var oldTags = _Dom.findByTagNames(document.getElementsByTagName("head")[0], {"link":true, "style":true});
            _Dom.runCss(newHead, true);
            _Dom.deleteItems(oldTags);

            //var oldTags = _Dom.findByTagNames(document.getElementsByTagName("head")[0], {"script": true});
            //_Dom.deleteScripts(oldTags);
            _Dom.runScripts(newHead, true);

            return doc;
        };

        /**
         * special method to handle the body dom manipulation,
         * replacing the entire body does not work fully by simply adding a second body
         * and by creating a range instead we have to work around that by dom creating a second
         * body and then filling it properly!
         *
         * @param {Object} request our request object
         * @param {Object} context (Map) the response context
         * @param {String} newData the markup which replaces the old dom node!
         * @param {Node} parsedData (optional) preparsed XML representation data of the current document
         */
        var _replaceBody = function (request, context, newData /*varargs parsedData*/) {
            var oldBody = document.getElementsByTagName("body")[0],
                    placeHolder = document.createElement("div"),
                    isWebkit = _RT.browser.isWebKit;

            placeHolder.id = "myfaces_bodyplaceholder";

            _Dom._removeChildNodes(oldBody);
            oldBody.innerHTML = "";
            oldBody.appendChild(placeHolder);

            var bodyData, doc = null, parser;

            //we have to work around an xml parsing bug in Webkit
            //see https://issues.apache.org/jira/browse/MYFACES-3061
            if (!isWebkit) {
                doc = (arguments.length > 3) ? arguments[3] : _Lang.parseXML(newData);
            }

            if (!isWebkit && _Lang.isXMLParseError(doc)) {
                doc = _Lang.parseXML(newData.replace(/<!\-\-[\s\n]*<!\-\-/g, "<!--").replace(/\/\/-->[\s\n]*\/\/-->/g, "//-->"));
            }

            if (isWebkit || _Lang.isXMLParseError(doc)) {
                //the standard xml parser failed we retry with the stripper

                parser = new (_RT.getGlobalConfig("updateParser", myfaces._impl._util._HtmlStripper))();

                bodyData = parser.parse(newData, "body");
            } else {
                //parser worked we go on
                var newBodyData = doc.getElementsByTagName("body")[0];

                //speedwise we serialize back into the code
                //for code reduction, speedwise we will take a small hit
                //there which we will clean up in the future, but for now
                //this is ok, I guess, since replace body only is a small subcase
                //bodyData = _Lang.serializeChilds(newBodyData);
                var browser = _RT.browser;
                if (!browser.isIEMobile || browser.isIEMobile >= 7) {
                    //TODO check what is failing there
                    for (var cnt = 0; cnt < newBodyData.attributes.length; cnt++) {
                        var value = newBodyData.attributes[cnt].value;
                        if (value)
                            _Dom.setAttribute(oldBody, newBodyData.attributes[cnt].name, value);
                    }
                }
            }
            //we cannot serialize here, due to escape problems
            //we must parse, this is somewhat unsafe but should be safe enough
            parser = new (_RT.getGlobalConfig("updateParser", myfaces._impl._util._HtmlStripper))();
            bodyData = parser.parse(newData, "body");

            _replaceHtmlItem(request, context, placeHolder, bodyData);
        };

        /**
         * Replaces HTML elements through others and handle errors if the occur in the replacement part
         *
         * @param {Object} request (xhrRequest)
         * @param {Object} context (Map)
         * @param {Object} itemIdToReplace (String|Node) - ID of the element to replace
         * @param {String} markup - the new tag
         */
        var _replaceHtmlItem = function (request, context, itemIdToReplace, markup) {
            var item = (!_Lang.isString(itemIdToReplace)) ? itemIdToReplace :
                    _Dom.byIdOrName(itemIdToReplace);

            if (!item) {
                throw _raiseError(_Lang.getMessage("ERR_ITEM_ID_NOTFOUND", null, "_AjaxResponse.replaceHtmlItem", (itemIdToReplace) ? itemIdToReplace.toString() : "undefined"));
            }
            return _Dom.outerHTML(item, markup);
        };

        /**
         * xml insert command handler
         *
         * @param request the ajax request element
         * @param context the context element holding the data
         * @param node the xml node holding the insert data
         * @return true upon successful completion, false otherwise
         *
         **/
        var _processInsert = function (request, context, node) {
            /*remapping global namespaces for speed and readability reasons*/
            var insertData = _parseInsertData(request, context, node);

            if (insertData) {
                var opNode = _Dom.byIdOrName(insertData.opId);
                if (!opNode) {
                    throw _raiseError(new Error(), _Lang.getMessage("ERR_PPR_INSERTBEFID_1", null, "_AjaxResponse.processInsert", insertData.opId), "_processInsert");
                }
                //call insertBefore or insertAfter in our dom routines
                _Dom[insertData.insertType](opNode, insertData.cDataBlock);
            }
        };

        /**
         * determines the corner data from the insert tag parsing process
         *
         *
         * @param request request
         * @param context context
         * @param node the current node pointing to the insert tag
         * @return false if the parsing failed, otherwise a map with follwing attributes
         * <ul>
         *     <li>inserType - a ponter to a constant which maps the direct function name for the insert operation </li>
         *     <li>opId - the before or after id </li>
         *     <li>cDataBlock - the html cdata block which needs replacement </li>
         * </ul>
         *
         * TODO we have to find a mechanism to replace the direct sendError calls with a javascript exception
         * which we then can use for cleaner error code handling
         */
        var _parseInsertData = function (request, context, node) {
            var concatCDATA = _Dom.concatCDATABlocks,

                    INSERT_TYPE_BEFORE = "insertBefore",
                    INSERT_TYPE_AFTER = "insertAfter",

                    id = node.getAttribute("id"),
                    beforeId = node.getAttribute("before"),
                    afterId = node.getAttribute("after"),
                    ret = {};

            //now we have to make a distinction between two different parsing paths
            //due to a spec malalignment
            //a <insert id="... beforeId|AfterId ="...
            //b <insert><before id="..., <insert> <after id="....
            //see https://issues.apache.org/jira/browse/MYFACES-3318
            //simple id, case1
            if (id && beforeId && !afterId) {
                ret.insertType = INSERT_TYPE_BEFORE;
                ret.opId = beforeId;
                ret.cDataBlock = concatCDATA(node);

                //<insert id=".. afterId="..
            } else if (id && !beforeId && afterId) {
                ret.insertType = INSERT_TYPE_AFTER;
                ret.opId = afterId;
                ret.cDataBlock = concatCDATA(node);

                //<insert><before id="... <insert><after id="...
            } else if (!id) {
                var opType = node.childNodes[0].tagName;

                if (opType != "before" && opType != "after") {
                    throw _raiseError(new Error(), _Lang.getMessage("ERR_PPR_INSERTBEFID"), "_parseInsertData");
                }
                opType = opType.toLowerCase();
                var beforeAfterId = node.childNodes[0].getAttribute("id");
                ret.insertType = (opType == "before") ? INSERT_TYPE_BEFORE : INSERT_TYPE_AFTER;
                ret.opId = beforeAfterId;
                ret.cDataBlock = concatCDATA(node.childNodes[0]);
            } else {
                throw _raiseError(new Error(), [_Lang.getMessage("ERR_PPR_IDREQ"),
                                                "\n ",
                                                _Lang.getMessage("ERR_PPR_INSERTBEFID")].join(""), "_parseInsertData");
            }
            ret.opId = _Lang.trim(ret.opId);
            return ret;
        };

        var _processDelete = function (request, context, node) {

            var deleteId = node.getAttribute('id');

            if (!deleteId) {
                throw _raiseError(new Error(), _Lang.getMessage("ERR_PPR_UNKNOWNCID", null, "_AjaxResponse.processDelete", ""), "_processDelete");
            }

            var item = _Dom.byIdOrName(deleteId);
            if (!item) {
                throw _raiseError(new Error(), _Lang.getMessage("ERR_PPR_UNKNOWNCID", null, "_AjaxResponse.processDelete", deleteId), "_processDelete");
            }

            var parentForm = _Dom.getParent(item, "form");
            if (null != parentForm) {
                context._mfInternal._viewStateForms.push(parentForm);
            }
            _Dom.deleteItem(item);
        };

        var _processAttributes = function (request, context, node) {
            //we now route into our attributes function to bypass
            //IE quirks mode incompatibilities to the biggest possible extent
            //most browsers just have to do a setAttributes but IE
            //behaves as usual not like the official standard
            //myfaces._impl._util._Dom.setAttribute(domNode, attribute, value;

            var elemId = node.getAttribute('id');

            if (!elemId) {
                throw _raiseError(new Error(), "Error in attributes, id not in xml markup", "_processAttributes");
            }
            var childNodes = node.childNodes;

            if (!childNodes) {
                return;
            }
            for (var loop2 = 0; loop2 < childNodes.length; loop2++) {
                var attributesNode = childNodes[loop2],
                        attrName = attributesNode.getAttribute("name"),
                        attrValue = attributesNode.getAttribute("value");

                if (!attrName) {
                    continue;
                }

                attrName = _Lang.trim(attrName);
                /*no value means reset*/
                //value can be of boolean value hence full check
                if ('undefined' == typeof attrValue || null == attrValue) {
                    attrValue = "";
                }

                switch (elemId) {
                    case P_VIEWROOT:
                        throw  _raiseError(new Error(), _Lang.getMessage("ERR_NO_VIEWROOTATTR", null, "_AjaxResponse.processAttributes"), "_processAttributes");

                    case P_VIEWHEAD:
                        throw  _raiseError(new Error(), _Lang.getMessage("ERR_NO_HEADATTR", null, "_AjaxResponse.processAttributes"), "_processAttributes");

                    case P_VIEWBODY:
                        var element = document.getElementsByTagName("body")[0];
                        _Dom.setAttribute(element, attrName, attrValue);
                        break;

                    default:
                        _Dom.setAttribute(document.getElementById(elemId), attrName, attrValue);
                        break;
                }
            }
        };

        /**
         * internal helper which raises an error in the
         * format we need for further processing
         *
         * @param message the message
         * @param title the title of the error (optional)
         * @param name the name of the error (optional)
         */
        var _raiseError = function (error, message, caller, title, name) {
            var _Impl = _getImpl(),
                    finalTitle = title || _Impl.MALFORMEDXML,
                    finalName = name || _Impl.MALFORMEDXML,
                    finalMessage = message || "";

            return _Lang.makeException(error, finalTitle, finalName, "myfaces._impl.xhrCore._AjaxResponse", caller || ( (arguments.callee.caller) ? arguments.callee.caller.toString() : "_raiseError"), finalMessage);
        };

    });

}