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
 */
_MF_SINGLTN(_PFX_XHR + "_AjaxResponse", _MF_OBJECT, /** @lends myfaces._impl.xhrCore._AjaxResponse.prototype */ {

    /*partial response types*/
    RESP_PARTIAL:"partial-response",
    RESP_TYPE_ERROR:"error",
    RESP_TYPE_REDIRECT:"redirect",
    RESP_TYPE_CHANGES:"changes",

    /*partial commands*/
    CMD_CHANGES:"changes",
    CMD_UPDATE:"update",
    CMD_DELETE:"delete",
    CMD_INSERT:"insert",
    CMD_EVAL:"eval",
    CMD_ERROR:"error",
    CMD_ATTRIBUTES:"attributes",
    CMD_EXTENSION:"extension",
    CMD_REDIRECT:"redirect",

    /*other constants*/
    P_VIEWSTATE:"javax.faces.ViewState",
    P_CLIENTWINDOW: "javax.faces.ClientWindow",
    P_VIEWROOT:"javax.faces.ViewRoot",
    P_VIEWHEAD:"javax.faces.ViewHead",
    P_VIEWBODY:"javax.faces.ViewBody",
    P_RESOURCE:"javax.faces.Resource",

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
    processResponse:function (request, context) {
        //mfinternal handling, note, the mfinternal is only optional
        //according to the spec
        context._mfInternal = context._mfInternal || {};
        var mfInternal = context._mfInternal;

        //the temporary data is hosted here
        mfInternal._updateElems = [];
        mfInternal._updateForms = [];
        mfInternal.appliedViewState = null;
        mfInternal.appliedClientWindow = null;

        try {
            var _Impl = this.attr("impl"), _Lang = this._Lang;
            // TODO:
            // Solution from
            // http://www.codingforums.com/archive/index.php/t-47018.html
            // to solve IE error 1072896658 when a Java server sends iso88591
            // istead of ISO-8859-1

            if (!request || !_Lang.exists(request, "responseXML")) {
                throw this.makeException(new Error(), _Impl.EMPTY_RESPONSE, _Impl.EMPTY_RESPONSE, this._nameSpace, "processResponse", "");
            }
            //check for a parseError under certain browsers

            var xmlContent = request.responseXML;
            //ie6+ keeps the parsing response under xmlContent.parserError
            //while the rest of the world keeps it as element under the first node
            var xmlErr = _Lang.fetchXMLErrorMessage(request.responseText || request.response, xmlContent)
            if (xmlErr) {
                throw this._raiseError(new Error(), xmlErr.errorMessage + "\n" + xmlErr.sourceText + "\n" + xmlErr.visualError + "\n", "processResponse");
            }
            var partials = xmlContent.childNodes[0];
            if ('undefined' == typeof partials || partials == null) {
                throw this._raiseError(new Error(), "No child nodes for response", "processResponse");

            } else {
                if (partials.tagName != this.RESP_PARTIAL) {
                    // IE 8 sees XML Header as first sibling ...
                    partials = partials.nextSibling;
                    if (!partials || partials.tagName != this.RESP_PARTIAL) {
                        throw this._raiseError(new Error(), "Partial response not set", "processResponse");
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
                if (tagName == this.CMD_ERROR) {
                    this.processError(request, context, childNode);
                } else if (tagName == this.CMD_REDIRECT) {
                    this.processRedirect(request, context, childNode);
                } else if (tagName == this.CMD_CHANGES) {
                    this.processChanges(request, context, childNode);
                }
            }

            //fixup missing viewStates due to spec deficiencies
            if(mfInternal.appliedViewState) {
                this.fixViewStates(context);
            }
            if(mfInternal.appliedClientWindow) {
                this.fixClientWindows(context);
            }

            //spec jsdoc, the success event must be sent from response
            _Impl.sendEvent(request, context, _Impl["SUCCESS"]);

        } finally {
            delete mfInternal._updateElems;
            delete mfInternal._updateForms;
            delete mfInternal.appliedViewState;
            delete mfInternal.appliedClientWindow;
        }
    },

    /**
     * fixes the viewstates in the current page
     *
     * @param context
     */
    fixViewStates:function (context) {
        var _Lang = this._Lang;
        var mfInternal = context._mfInternal;

        if (null == mfInternal.appliedViewState) {
            return;
        }

        //if we set our no portlet env we safely can update all forms with
        //the new viewstate
        if (this._RT.getLocalOrGlobalConfig(context, "no_portlet_env", false)) {
            for (var cnt = document.forms.length - 1; cnt >= 0; cnt--) {
                this._setVSTCWForm(context, document.forms[cnt], mfInternal.appliedViewState, this.P_VIEWSTATE);
            }
            return;
        }

        var updatedForm = this._getUpdatedForm(context, mfInternal._updateForms);
        if (updatedForm != null) {
            var baseViewStateField = this._Dom.getNamedElementFromForm(updatedForm, this.P_VIEWSTATE);
            var viewStateId = baseViewStateField.id;
            var viewStatePrefix = viewStateId.substring(0,
                viewStateId.indexOf(this.P_VIEWSTATE)+this.P_VIEWSTATE.length);
            var viewStateFields = document.getElementsByName(this.P_VIEWSTATE);
            for (var cnt = viewStateFields.length - 1; cnt >= 0; cnt--) {
                if (viewStateFields[cnt].id.startsWith(viewStatePrefix)) {
                    this._setVSTCWForm(context, viewStateFields[cnt].form, mfInternal.appliedViewState, this.P_VIEWSTATE);
                }
            }
        }else{
            // Now update the forms that were not replaced but forced to be updated, because contains child ajax tags
            // we should only update forms with view state hidden field. If by some reason, the form was set to be
            // updated but the form was replaced, it does not have hidden view state, so later in changeTrace processing the
            // view state is updated.

            //set the viewstates of all outer forms parents of our updated elements
            _Lang.arrForEach(mfInternal._updateForms, function (elem) {
                this._setVSTCWForm(context, elem, mfInternal.appliedViewState, this.P_VIEWSTATE);
            }, 0, this);

            //set the viewstate of all forms within our updated elements
            _Lang.arrForEach(mfInternal._updateElems, function (elem) {
                this._setVSTCWInnerForms(context, elem, mfInternal.appliedViewState, this.P_VIEWSTATE);
            }, 0, this);
        }
    },

    fixClientWindows:function (context, theForm) {
        var _Lang = this._Lang;
        var mfInternal = context._mfInternal;

        if (null == mfInternal.appliedClientWindow) {
            return;
        }
         //if we set our no portlet env we safely can update all forms with
        //the new viewstate
        if (this._RT.getLocalOrGlobalConfig(context, "no_portlet_env", false)) {
            for (var cnt = document.forms.length - 1; cnt >= 0; cnt--) {
                this._setVSTCWForm(context, document.forms[cnt], mfInternal.appliedClientWindow, this.P_CLIENTWINDOW);
            }
            return;
        }
        
        var updatedForm = this._getUpdatedForm(context, mfInternal._updateForms);
        if (updatedForm != null) {
            var baseCWField = this._Dom.getNamedElementFromForm(updatedForm, this.P_CLIENTWINDOW);
            var cwId = baseCWField.id;
            var cwPrefix = cwId.substring(0,
                cwId.indexOf(this.P_CLIENTWINDOW)+this.P_CLIENTWINDOW.length);
            var cwFields = document.getElementsByName(this.P_CLIENTWINDOW);
            for (var cnt = cwFields.length - 1; cnt >= 0; cnt--) {
                if (cwFields[cnt].id.startsWith(cwPrefix)) {
                    this._setVSTCWForm(context, cwFields[cnt].form, mfInternal.appliedClientWindow, this.P_CLIENTWINDOW);
                }
            }
        } else{
            //set the client window of all outer form of updated elements

            _Lang.arrForEach(mfInternal._updateForms, function (elem) {
                this._setVSTCWForm(context, elem, mfInternal.appliedClientWindow, this.P_CLIENTWINDOW);
            }, 0, this);

            //set the client window of all forms within our updated elements
            _Lang.arrForEach(mfInternal._updateElems, function (elem) {
                this._setVSTCWInnerForms(context, elem, mfInternal.appliedClientWindow, this.P_CLIENTWINDOW);
            }, 0, this);
        }
    },


    _getUpdatedForm:function(context, updateForms) {
        if (updateForms != null) {
            for (var i = 0; i < updateForms.length; i++) {
                var elem = this._Lang.byId(updateForms[i]);
                if (!elem){
                    continue;
                }else{
                    return elem;
                }
            }
        }
    },
    
    /**
     * sets the viewstate element in a given form
     *
     * @param theForm the form to which the element has to be set to
     * @param context the current request context
     */
    _setVSTCWForm:function (context, theForm, value, identifier) {
        if (typeof theForm === 'string' || theForm instanceof String) {
            theForm = this._Lang.byId(theForm);
        }
        var mfInternal = context._mfInternal;

        if (!theForm) return;

        //in IE7 looking up form elements with complex names (such as 'javax.faces.ViewState') fails in certain cases
        //iterate through the form elements to find the element, instead
        var fieldToApply = this._Dom.getNamedElementFromForm(theForm, identifier);

        if (fieldToApply) {
            this._Dom.setAttribute(fieldToApply, "value", value);
        } else if (!fieldToApply) {
            var element = this._Dom.getDummyPlaceHolder();
            //spec error, two elements with the same id should not be there, TODO recheck the space if the name does not suffice alone
            element.innerHTML = ["<input type='hidden'", "id='", identifier+jsf.separatorchar+Math.random() , "' name='", identifier , "' value='" , value , "' />"].join("");
            //now we go to proper dom handling after having to deal with another ie screwup
            try {
                theForm.appendChild(element.childNodes[0]);
            } finally {
                element.innerHTML = "";
            }
        }
    },

    _setVSTCWInnerForms:function (context, elem, value, identifier) {

        var _Lang = this._Lang, _Dom = this._Dom;
        elem = _Dom.byIdOrName(elem);
        //elem not found for whatever reason
        //https://issues.apache.org/jira/browse/MYFACES-3544
        if (!elem) return;

        var replacedForms = _Dom.findByTagName(elem, "form", false);

        var applyVST = _Lang.hitch(this, function (elem) {
            this._setVSTCWForm(context, elem, value, identifier);
        });

        try {
            _Lang.arrForEach(replacedForms, applyVST, 0, this);
        } finally {
            applyVST = null;
        }
    },

    /**
     * processes an incoming error from the response
     * which is hosted under the &lt;error&gt; tag
     * @param request the current request
     * @param context the contect object
     * @param node the node in the xml hosting the error message
     */
    processError:function (request, context, node) {
        /**
         * <error>
         *      <error-name>String</error-name>
         *      <error-message><![CDATA[message]]></error-message>
         * <error>
         */
        var errorName = node.firstChild.textContent || node.firstChild.text || "",
                errorMessage = node.childNodes[1].firstChild.data || "";

        this.attr("impl").sendError(request, context, this.attr("impl").SERVER_ERROR, errorName, errorMessage, "myfaces._impl.xhrCore._AjaxResponse", "processError");
    },

    /**
     * processes an incoming xml redirect directive from the ajax response
     * @param request the request object
     * @param context the context
     * @param node the node hosting the redirect data
     */
    processRedirect:function (request, context, node) {
        /**
         * <redirect url="url to redirect" />
         */
        var _Lang = this._Lang;
        var redirectUrl = node.getAttribute("url");
        if (!redirectUrl) {
            throw this._raiseError(new Error(), _Lang.getMessage("ERR_RED_URL", null, "_AjaxResponse.processRedirect"), "processRedirect");
        }
        redirectUrl = _Lang.trim(redirectUrl);
        if (redirectUrl == "") {
            return false;
        }
        window.location = redirectUrl;
        return true;
    },

    /**
     * main entry point for processing the changes
     * it deals with the &lt;changes&gt; node of the
     * response
     *
     * @param request the xhr request object
     * @param context the context map
     * @param node the changes node to be processed
     */
    processChanges:function (request, context, node) {
        var changes = node.childNodes;
        var _Lang = this._Lang;
        //note we need to trace the changes which could affect our insert update or delete
        //se that we can realign our ViewStates afterwards
        //the realignment must happen post change processing

        for (var i = 0; i < changes.length; i++) {

            switch (changes[i].tagName) {

                case this.CMD_UPDATE:
                    this.processUpdate(request, context, changes[i]);
                    break;
                case this.CMD_EVAL:
                    _Lang.globalEval(changes[i].firstChild.data);
                    break;
                case this.CMD_INSERT:
                    this.processInsert(request, context, changes[i]);
                    break;
                case this.CMD_DELETE:
                    this.processDelete(request, context, changes[i]);
                    break;
                case this.CMD_ATTRIBUTES:
                    this.processAttributes(request, context, changes[i]);
                    break;
                case this.CMD_EXTENSION:
                    break;
                default:
                    throw this._raiseError(new Error(), "_AjaxResponse.processChanges: Illegal Command Issued", "processChanges");
            }
        }

        return true;
    },

    /**
     * First sub-step process a pending update tag
     *
     * @param request the xhr request object
     * @param context the context map
     * @param node the changes node to be processed
     */
    processUpdate:function (request, context, node) {
        if ( (node.getAttribute('id').indexOf(this.P_VIEWSTATE) != -1) || (node.getAttribute('id').indexOf(this.P_CLIENTWINDOW) != -1) ) {
            //update the submitting forms viewstate to the new value
            // The source form has to be pulled out of the CURRENT document first because the context object
            // may refer to an invalid document if an update of the entire body has occurred before this point.
            var mfInternal = context._mfInternal,
                    fuzzyFormDetection = this._Lang.hitch(this._Dom, this._Dom.fuzzyFormDetection);
            var elemId = (mfInternal._mfSourceControlId) ? mfInternal._mfSourceControlId :
                    ((context.source) ? context.source.id : null);

            //theoretically a source of null can be given, then our form detection fails for
            //the source element case and hence updateviewstate is skipped for the source
            //form, but still render targets still can get the viewstate
            var sourceForm = (mfInternal && mfInternal["_mfSourceFormId"] &&
                    document.forms[mfInternal["_mfSourceFormId"]]) ?
                    document.forms[mfInternal["_mfSourceFormId"]] : ((elemId) ? fuzzyFormDetection(elemId) : null);

            if(node.getAttribute('id').indexOf(this.P_VIEWSTATE) != -1) {
                mfInternal.appliedViewState = this._Dom.concatCDATABlocks(node);//node.firstChild.nodeValue;
            } else if(node.getAttribute('id').indexOf(this.P_CLIENTWINDOW) != -1) {
                mfInternal.appliedClientWindow = node.firstChild.nodeValue;
            }
            //source form could not be determined either over the form identifer or the element
            //we now skip this phase and just add everything we need for the fixup code

            if (!sourceForm) {
                //no source form found is not an error because
                //we might be able to recover one way or the other
                return true;
            }

            mfInternal._updateForms.push(sourceForm.id);

        }
        else {
            // response may contain several blocks
            var cDataBlock = this._Dom.concatCDATABlocks(node),
                    resultNode = null,
                    pushOpRes = this._Lang.hitch(this, this._pushOperationResult);

            switch (node.getAttribute('id')) {
                case this.P_VIEWROOT:

                    cDataBlock = cDataBlock.substring(cDataBlock.indexOf("<html"));

                    var parsedData = this._replaceHead(request, context, cDataBlock);

                    resultNode = ('undefined' != typeof parsedData && null != parsedData) ? this._replaceBody(request, context, cDataBlock, parsedData) : this._replaceBody(request, context, cDataBlock);
                    if (resultNode) {
                        pushOpRes(context, resultNode);
                    }
                    break;
                case this.P_VIEWHEAD:
                    //we cannot replace the head, almost no browser allows this, some of them throw errors
                    //others simply ignore it or replace it and destroy the dom that way!
                    this._replaceHead(request, context, cDataBlock);

                    break;
                case this.P_VIEWBODY:
                    //we assume the cdata block is our body including the tag
                    resultNode = this._replaceBody(request, context, cDataBlock);
                    if (resultNode) {
                        pushOpRes(context, resultNode);
                    }
                    break;
                case this.P_RESOURCE:
                    
                    this._addResourceToHead(request,context,cDataBlock);
                    break;
                default:
                    resultNode = this.replaceHtmlItem(request, context, node.getAttribute('id'), cDataBlock);
                    if (resultNode) {
                        pushOpRes(context, resultNode);
                    }
                    break;
            }
        }

        return true;
    },

    _pushOperationResult:function (context, resultNode) {
        var mfInternal = context._mfInternal;
        var pushSubnode = this._Lang.hitch(this, function (currNode) {
            var parentForm = this._Dom.getParent(currNode, "form");
            //if possible we work over the ids
            //so that elements later replaced are referenced
            //at the latest possibility
            if (null != parentForm) {
                mfInternal._updateForms.push(parentForm.id || parentForm);
            }
            else {
                mfInternal._updateElems.push(currNode.id || currNode);
            }
        });
        var isArr = 'undefined' != typeof resultNode.length && 'undefined' == typeof resultNode.nodeType;
        if (isArr && resultNode.length) {
            for (var cnt = 0; cnt < resultNode.length; cnt++) {
                pushSubnode(resultNode[cnt]);
            }
        } else if (!isArr) {
            pushSubnode(resultNode);
        }

    },

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
    _replaceHead:function (request, context, newData) {

        var _Lang = this._Lang,
                _Dom = this._Dom,
                isWebkit = this._RT.browser.isWebKit,
        //we have to work around an xml parsing bug in Webkit
        //see https://issues.apache.org/jira/browse/MYFACES-3061
                doc = (!isWebkit) ? _Lang.parseXML(newData) : null,
                newHead = null;

        if (!isWebkit && _Lang.isXMLParseError(doc)) {
            doc = _Lang.parseXML(newData.replace(/<!\-\-[\s\n]*<!\-\-/g, "<!--").replace(/\/\/-->[\s\n]*\/\/-->/g, "//-->"));
        }

        if (isWebkit || _Lang.isXMLParseError(doc)) {
            //the standard xml parser failed we retry with the stripper
            var parser = new (this._RT.getGlobalConfig("updateParser", myfaces._impl._util._HtmlStripper))();
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
                    throw this._raiseError(new Error(), "Error head replacement failed reason:" + e.toString(), "_replaceHead");
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
    },
    
    _addResourceToHead:function (request, context, newData) {
        var lastHeadChildTag = document.getElementsByTagName("head")[0].lastChild;

        var replacementFragment = this._Dom.insertAfter(lastHeadChildTag, newData);
        if (replacementFragment) {
            this._pushOperationResult(context, replacementFragment);
        }
    },

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
    _replaceBody:function (request, context, newData /*varargs*/) {
        var _RT = this._RT,
                _Dom = this._Dom,
                _Lang = this._Lang,

                oldBody = document.getElementsByTagName("body")[0],
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

        var returnedElement = this.replaceHtmlItem(request, context, placeHolder, bodyData);

        if (returnedElement) {
            this._pushOperationResult(context, returnedElement);
        }
        return returnedElement;
    },

    /**
     * Replaces HTML elements through others and handle errors if the occur in the replacement part
     *
     * @param {Object} request (xhrRequest)
     * @param {Object} context (Map)
     * @param {Object} itemIdToReplace (String|Node) - ID of the element to replace
     * @param {String} markup - the new tag
     */
    replaceHtmlItem:function (request, context, itemIdToReplace, markup) {
        var _Lang = this._Lang, _Dom = this._Dom;

        var item = (!_Lang.isString(itemIdToReplace)) ? itemIdToReplace :
                _Dom.byIdOrName(itemIdToReplace);

        if (!item) {
            throw this._raiseError(new Error(), _Lang.getMessage("ERR_ITEM_ID_NOTFOUND", null, "_AjaxResponse.replaceHtmlItem", (itemIdToReplace) ? itemIdToReplace.toString() : "undefined"), "replaceHtmlItem");
        }
        return _Dom.outerHTML(item, markup, this._RT.getLocalOrGlobalConfig(context, "preserveFocus", false));
    },

    /**
     * xml insert command handler
     *
     * @param request the ajax request element
     * @param context the context element holding the data
     * @param node the xml node holding the insert data
     * @return true upon successful completion, false otherwise
     *
     **/
    processInsert:function (request, context, node) {
        /*remapping global namespaces for speed and readability reasons*/
        var _Dom = this._Dom,
                _Lang = this._Lang,
        //determine which path to go:
                insertData = this._parseInsertData(request, context, node);

        if (!insertData) return false;

        var opNode = _Dom.byIdOrName(insertData.opId);
        if (!opNode) {
            throw this._raiseError(new Error(), _Lang.getMessage("ERR_PPR_INSERTBEFID_1", null, "_AjaxResponse.processInsert", insertData.opId), "processInsert");
        }

        //call insertBefore or insertAfter in our dom routines
        var replacementFragment = _Dom[insertData.insertType](opNode, insertData.cDataBlock);
        if (replacementFragment) {
            this._pushOperationResult(context, replacementFragment);
        }
        return true;
    },

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
    _parseInsertData:function (request, context, node) {
        var _Lang = this._Lang,
                _Dom = this._Dom,
                concatCDATA = _Dom.concatCDATABlocks,

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
                throw this._raiseError(new Error(), _Lang.getMessage("ERR_PPR_INSERTBEFID"), "_parseInsertData");
            }
            opType = opType.toLowerCase();
            var beforeAfterId = node.childNodes[0].getAttribute("id");
            ret.insertType = (opType == "before") ? INSERT_TYPE_BEFORE : INSERT_TYPE_AFTER;
            ret.opId = beforeAfterId;
            ret.cDataBlock = concatCDATA(node.childNodes[0]);
        } else {
            throw this._raiseError(new Error(), [_Lang.getMessage("ERR_PPR_IDREQ"),
                                                 "\n ",
                                                 _Lang.getMessage("ERR_PPR_INSERTBEFID")].join(""), "_parseInsertData");
        }
        ret.opId = _Lang.trim(ret.opId);
        return ret;
    },

    processDelete:function (request, context, node) {

        var _Lang = this._Lang,
                _Dom = this._Dom,
                deleteId = node.getAttribute('id');

        if (!deleteId) {
            throw this._raiseError(new Error(), _Lang.getMessage("ERR_PPR_UNKNOWNCID", null, "_AjaxResponse.processDelete", ""), "processDelete");
        }

        var item = _Dom.byIdOrName(deleteId);
        if (!item) {
            throw this._raiseError(new Error(), _Lang.getMessage("ERR_PPR_UNKNOWNCID", null, "_AjaxResponse.processDelete", deleteId), "processDelete");
        }

        var parentForm = this._Dom.getParent(item, "form");
        if (null != parentForm) {
            context._mfInternal._updateForms.push(parentForm);
        }
        _Dom.deleteItem(item);

        return true;
    },

    processAttributes:function (request, context, node) {
        //we now route into our attributes function to bypass
        //IE quirks mode incompatibilities to the biggest possible extent
        //most browsers just have to do a setAttributes but IE
        //behaves as usual not like the official standard
        //myfaces._impl._util.this._Dom.setAttribute(domNode, attribute, value;

        var _Lang = this._Lang,
        //<attributes id="id of element"> <attribute name="attribute name" value="attribute value" />* </attributes>
                elemId = node.getAttribute('id');

        if (!elemId) {
            throw this._raiseError(new Error(), "Error in attributes, id not in xml markup", "processAttributes");
        }
        var childNodes = node.childNodes;

        if (!childNodes) {
            return false;
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
                case this.P_VIEWROOT:
                    throw  this._raiseError(new Error(), _Lang.getMessage("ERR_NO_VIEWROOTATTR", null, "_AjaxResponse.processAttributes"), "processAttributes");

                case this.P_VIEWHEAD:
                    throw  this._raiseError(new Error(), _Lang.getMessage("ERR_NO_HEADATTR", null, "_AjaxResponse.processAttributes"), "processAttributes");

                case this.P_VIEWBODY:
                    var element = document.getElementsByTagName("body")[0];
                    this._Dom.setAttribute(element, attrName, attrValue);
                    break;

                default:
                    this._Dom.setAttribute(document.getElementById(elemId), attrName, attrValue);
                    break;
            }
        }
        return true;
    },

    /**
     * internal helper which raises an error in the
     * format we need for further processing
     *
     * @param message the message
     * @param title the title of the error (optional)
     * @param name the name of the error (optional)
     */
    _raiseError:function (error, message, caller, title, name) {
        var _Impl = this.attr("impl");
        var finalTitle = title || _Impl.MALFORMEDXML;
        var finalName = name || _Impl.MALFORMEDXML;
        var finalMessage = message || "";

        return this._Lang.makeException(error, finalTitle, finalName, this._nameSpace, caller || ( (arguments.caller) ? arguments.caller.toString() : "_raiseError"), finalMessage);
    }
});
