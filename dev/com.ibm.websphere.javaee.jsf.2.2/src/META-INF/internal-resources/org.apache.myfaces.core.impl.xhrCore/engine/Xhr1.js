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
 * @name Xhr1
 * @memberOf myfaces._impl.xhrCore.engine
 * @extends myfaces._impl.xhrCore.engine.BaseRequest
 * @description
 *
 * wrapper for an xhr level1 object with all its differences
 * it emulates the xhr level2 api which is way simpler than the level1 api
 */

_MF_CLS(_PFX_XHR + "engine.Xhr1", myfaces._impl.xhrCore.engine.BaseRequest, /** @lends myfaces._impl.xhrCore.engine.Xhr1.prototype */ {

    _xhrObject: null,
    _timeoutTimer: null,

    constructor_: function(params) {
        //the constructor is empty due to the original xhr object not having anything

        this._callSuper("constructor_", params);
        this._initDefaultFinalizableFields();

        this._XHRConst = myfaces._impl.xhrCore.engine.XhrConst;
        this._Lang.applyArgs(this, params);
    },

    // void open(DOMString method, DOMString url, boolean async);
    open: function(method, url, async) {

        var xhr = this._xhrObject;
        xhr.onreadystatechange = this._Lang.hitch(this, this.onreadystatechange);
        this.method = method || this.method;
        this.url = url || this.url;
        this.async = ('undefined' != typeof async) ? async : this.async;
        xhr.open(this.method, this.url, this.async);
    },

    send: function(formData) {

        var myevt = {};

        this._addProgressAttributes(myevt, 20, 100);
        this.onloadstart(myevt);
        this.onprogress(myevt);
        this._startTimeout();
        this._xhrObject.send(formData);
    },

    setRequestHeader: function(key, value) {
        this._xhrObject.setRequestHeader(key, value);
    },

    abort: function() {

        this._xhrObject.abort();
        this.onabort({});
    },

    _addProgressAttributes: function(evt, percent, total) {
        //http://www.w3.org/TR/progress-events/#progressevent
        evt.lengthComputable = true;
        evt.loaded = percent;
        evt.total = total;

    },

    onreadystatechange: function(evt) {
        var myevt = evt || {};
        //we have to simulate the attributes as well
        var xhr = this._xhrObject;
        var XHRConst = this._XHRConst;
        try {
        this.readyState = xhr.readyState;
        this.status = ""+xhr.status;
        } catch(e) {
            //IE 6 has an internal error
        }

        switch (this.readyState) {

            case  XHRConst.READY_STATE_OPENED:
                this._addProgressAttributes(myevt, 10, 100);

                this.onprogress(myevt);
                break;

            case XHRConst.READY_STATE_HEADERS_RECEIVED:
                this._addProgressAttributes(myevt, 25, 100);

                this.onprogress(myevt);
                break;

            case XHRConst.READY_STATE_LOADING:
                if (this._loadingCalled) break;
                this._loadingCalled = true;
                this._addProgressAttributes(myevt, 50, 100);

                this.onprogress(myevt);
                break;

            case XHRConst.READY_STATE_DONE:
                this._addProgressAttributes(myevt, 100, 100);
                //xhr level1 does not have timeout handler
                if (this._timeoutTimer) {
                    //normally the timeout should not cause anything anymore
                    //but just to make sure
                    window.clearTimeout(this._timeoutTimer);
                    this._timeoutTimer = null;
                }
                this._transferRequestValues();
                this.onprogress(myevt);
                try {
                    var status = xhr.status;
                    if (status >= XHRConst.STATUS_OK_MINOR && status < XHRConst.STATUS_OK_MAJOR) {
                        this.onload(myevt);
                    } else {
                        myevt.type = "error";
                        this.onerror(myevt);
                    }
                } finally {
					//remove for xhr level2 support
                    this.onloadend(myevt);
                }
        }
    },

    _transferRequestValues: function() {
        //this._Lang.mixMaps(this, this._xhrObject, true, null,
        //        {responseText:1,responseXML:1,status:1,statusText:1,response:1});
        var UDEF = "undefined";
        var xhr = this._xhrObject;

        this.responseText = (UDEF != typeof xhr.responseText)?xhr.responseText: null;
        this.responseXML = (UDEF != typeof xhr.responseXML)?xhr.responseXML: null;
        this.status = (UDEF != typeof xhr.status)?xhr.status: null;
        this.statusText = (UDEF != typeof xhr.statusText)?xhr.statusText: null;
        this.response = (UDEF != typeof xhr.response)?xhr.response: null;
    },

    _startTimeout: function() {
        if (this.timeout == 0) return;

        var xhr = this._xhrObject;
        //some browsers have timeouts in their xhr level 1.x objects implemented
        //we leverage them whenever they exist
        try {
            if ('undefined' != typeof xhr.timeout) {
                xhr.timeout = this.timeout;
                xhr.ontimeout = this.ontimeout;
                return;
            }
        } catch (e) {
            //firefox 12 has a bug here
        }


        this._timeoutTimer = setTimeout(this._Lang.hitch(this, function() {
            if (xhr.readyState != this._XHRConst.READY_STATE_DONE) {

                xhr.onreadystatechange = function() {
                };
                clearTimeout(this._timeoutTimer);
                xhr.abort();
                this.ontimeout({});
            }
        }), this.timeout);
    },

    getXHRObject: function() {
        return this._xhrObject;
    }


});