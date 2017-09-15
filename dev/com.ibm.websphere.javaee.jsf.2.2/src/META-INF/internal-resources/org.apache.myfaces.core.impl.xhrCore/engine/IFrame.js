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
 * @name IFrame
 * @memberOf myfaces._impl.xhrCore.engine
 * @extends myfaces._impl.xhrCore.engine.BaseRequest
 * @description
 *
 * wrapper for an iframe transport object with all its differences
 * it emulates the xhr level2 api
 */
_MF_CLS(_PFX_XHR + "engine.IFrame", myfaces._impl.xhrCore.engine.BaseRequest,
        /** @lends myfaces._impl.xhrCore.engine.IFrame.prototype */
        {

            _finalized: false,

            /*the target frame responsible for the communication*/
            _frame: null,
            //_requestHeader: null,
            _aborted: false,

            CLS_NAME: "myfaces._impl.xhrCore._IFrameRequest",
            _FRAME_ID: "_mf_comm_frm",

            /**
             * constructor which shifts the arguments
             * to the protected properties of this clas
             *
             * @param arguments
             */
            constructor_: function (arguments) {
                //we fetch in the standard arguments

                this._callSuper("constructor", arguments);

                this._initDefaultFinalizableFields();
                //this._requestHeader = {};

                this._XHRConst = myfaces._impl.xhrCore.engine.XhrConst;

                this._Lang.applyArgs(this, arguments);
                this.readyState = this._XHRConst.READY_STATE_UNSENT;
                this._startTimeout();
            },

            setRequestHeader: function (key, value) {
                //this._requestHeader[key] = value;
            },

            open: function (method, url, async) {

                this.readyState = this._XHRConst.READY_STATE_OPENED;

                var _RT = myfaces._impl.core._Runtime;
                var _Lang = this._Lang;

                this._frame = this._createTransportFrame();

                //we append an onload handler to the frame
                //to cover the starting and loading events,
                //timeouts cannot be covered in a cross browser way

                //we point our onload handler to the frame, we do not use addOnLoad
                //because the frame should not have other onload handlers in place
                if (!_RT.browser.isIE || this._Dom.isDomCompliant()) {
                    this._frame.onload = _Lang.hitch(this, this._callback);
                } else {
                    //ie has a bug, onload is not settable outside of innerHTML on iframes
                    this._frame.onload_IE = _Lang.hitch(this, this._callback);
                }

                this.method = method || this.method;
                this.url = url || this.url;
                this.async = ('undefined' != typeof async) ? async : this.async;

                var myevt = {};
                this._addProgressAttributes(myevt, 10, 100);
                this.onprogress(myevt);
            },

            send: function (formData) {

                var myevt = {};
                this._addProgressAttributes(myevt, 20, 100);
                this.onloadstart(myevt);
                this.onprogress(myevt);

                var formDataForm = formData.form,
                        oldTarget = formDataForm.target,
                        oldMethod = formDataForm.method,
                        oldAction = formDataForm.action;

                try {
                    //_t._initAjaxParams();
                    //for (var key in this._requestHeader) {
                    //    formData.append(key, this._requestHeader[key]);
                    //}

                    formDataForm.target = this._frame.name;
                    formDataForm.method = this.method;
                    if (this.url) {
                        formDataForm.action = this.url;
                    }
                    this.readyState = this._XHRConst.READY_STATE_LOADING;
                    this.onreadystatechange(myevt);
                    formDataForm.submit();
                } finally {
                    formDataForm.action = oldAction;
                    formDataForm.target = oldTarget;
                    formDataForm.method = oldMethod;

                    formData._finalize();
                    //alert("finalizing");
                    this._finalized = true;
                }
            },

            /*we can implement it, but it will work only on browsers
             * which have asynchronous iframe loading*/
            abort: function () {
                this._aborted = true;
                this.onabort({});
            },

            _addProgressAttributes: function (evt, percent, total) {
                //http://www.w3.org/TR/progress-events/#progressevent
                evt.lengthComputable = true;
                evt.loaded = percent;
                evt.total = total;

            },

            _callback: function () {
                //------------------------------------
                // we are asynchronous which means we
                // have to check wether our code
                // is finalized or not
                //------------------------------------
                if (this._aborted) return;
                if (this._timeoutTimer) {
                    clearTimeout(this._timeoutTimer);
                }
                if (!this._finalized) {
                    setTimeout(this._Lang.hitch(this, this._callback), 10);
                    return;
                }
                //aborted no further processing

                try {
                    var myevt = {};

                    this._addProgressAttributes(myevt, 100, 100);
                    //this.readyState = this._XHRConst.READY_STATE_DONE;
                    //this.onreadystatechange(myevt);

                    this.responseText = this._getFrameText();
                    this.responseXML = this._getFrameXml();
                    this.readyState = this._XHRConst.READY_STATE_DONE;

                    //TODO status and statusText

                    this.onreadystatechange(myevt);
                    this.onloadend();

                    if (!this._Lang.isXMLParseError(this.responseXML)) {
                        this.status = 201;
                        this.onload();
                    } else {
                        this.status = 0;
                        //we simulate the request for our xhr call
                        this.onerror();
                    }

                } finally {
                    this._frame = null;
                }
            },

            /**
             * returns the frame text in a browser independend manner
             */
            _getFrameDocument: function () {

                //we cover various browsers here, because almost all browsers keep the document in a different
                //position
                return this._frame.contentWindow.document || this._frame.contentDocument || this._frame.document;
            },

            _getFrameText: function () {
                var framedoc = this._getFrameDocument();
                //also ie keeps the body in framedoc.body the rest in documentElement
                var body = framedoc.body || framedoc.documentElement;
                return  body.innerHTML;
            },

            _clearFrame: function () {

                var framedoc = this._getFrameDocument();
                var body = framedoc.documentElement || framedoc.body;
                //ie8 in 7 mode chokes on the innerHTML method
                //direct dom removal is less flakey and works
                //over all browsers, but is slower
                if (myfaces._impl.core._Runtime.browser.isIE) {
                    this._Dom._removeChildNodes(body, false);
                } else {
                    body.innerHTML = "";
                }
            },

            /**
             * returns the processed xml from the frame
             */
            _getFrameXml: function () {
                var framedoc = this._getFrameDocument();
                //same situation here, the xml is hosted either in xmlDocument or
                //is located directly under the frame document
                return  framedoc.XMLDocument || framedoc;
            },

            _createTransportFrame: function () {

                var _FRM_ID = this._FRAME_ID;
                var frame = document.getElementById(_FRM_ID);
                if (frame) return frame;
                //normally this code should not be called
                //but just to be sure

                if (this._Dom.isDomCompliant()) {
                    frame = this._Dom.createElement('iframe', {
                        "src": "about:blank",
                        "id": _FRM_ID,
                        "name": _FRM_ID,
                        "type": "content",
                        "collapsed": "true",
                        "style": "display:none"
                    });

                    //probably the ie method would work on all browsers
                    //but this code is the safe bet it works on all standards
                    //compliant browsers in a clean manner

                    document.body.appendChild(frame);
                } else { //Now to the non compliant browsers
                    var node = this._Dom.createElement("div", {
                        "style": "display:none"
                    });

                    //we are dealing with two well known iframe ie bugs here
                    //first the iframe has to be set via innerHTML to be present
                    //secondly the onload handler is immutable on ie, we have to
                    //use a dummy onload handler in this case and call this one
                    //from the onload handler
                    node.innerHTML = "<iframe id='" + _FRM_ID + "' name='" + _FRM_ID + "' style='display:none;' src='about:blank' type='content' onload='this.onload_IE();'  ></iframe>";

                    //avoid the ie open tag problem
                    var body = document.body;
                    if (body.firstChild) {
                        body.insertBefore(node, document.body.firstChild);
                    } else {
                        body.appendChild(node);
                    }
                }

                //helps to for the onload handlers and innerhtml to be in sync again
                return document.getElementById(_FRM_ID);
            },

            _startTimeout: function () {

                if (this.timeout == 0) return;
                this._timeoutTimer = setTimeout(this._Lang.hitch(this, function () {
                    if (this._xhrObject.readyState != this._XHRConst.READY_STATE_DONE) {

                        this._aborted = true;
                        clearTimeout(this._timeoutTimer);
                        //we cannot abort an iframe request
                        this.ontimeout({});
                        this._timeoutTimer = null;
                    }
                }), this.timeout);
            }
        });
