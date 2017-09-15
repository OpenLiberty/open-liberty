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
 * @name BaseRequest
 * @memberOf myfaces._impl.xhrCore.engine
 * @extends myfaces._impl.core.Object
 * @description
 * Abstract Base for all classes which simulate the xhr level2 object
 * with a different transport
 *
 * <h3>Every class inheriting the interface must expose following methods and attributes</h3>
 *
 * <ul>
 *      <li>open(method, url, async)</li>
 *      <li>send(formData)</li>
 *      <li>setRequestHeader(key, value)</li>
 *      <li>abort()</li>
 *      <li>onloadstart()</li>
 *      <li>onprogress()</li>
 *      <li>onabort()</li>
 *      <li>onerror()</li>
 *      <li>onload()</li>
 *      <li>ontimeout()</li>
 *      <li>onloadend()</li>
 *      <li>onreadystatechange()</li>
 * </ul>
 * <h3>following attributes are supported</h3>
 * <ul>
 *      <li>async</li>
 *      <li>url</li>
 *      <li>method</li>
 *      <li>timeout</li>
 *      <li>response</li>
 *      <li>responseText</li>
 *      <li>responseXML</li>
 *      <li>status</li>
 *      <li>statusText</li>
 * </ul>
 */
_MF_CLS(_PFX_XHR + "engine.BaseRequest", _MF_OBJECT, /** @lends myfaces._impl.xhrCore.engine.BaseRequest.prototype */ {
    /*standard attributes*/

    /**
     * timeout attribute with a timeout for the request in miliseconds
     */
    timeout:0,
    /**
     * readonly ready stte attribute
     */
    readyState:0,
    /**
     * send method, allowed values POST and GET
     */
    method:"POST",
    /**
     * the url for the call
     */
    url:null,
    /**
     * asynchronous request, if set to true then the request happens
     * asynchronously, if possible.
     */
    async:true,
    /**
     * read only response object, containing the response as json/dom representation
     */
    response:null,
    /**
     * read only plain text representation of the response
     */
    responseText:null,
    /**
     * xml dom readonly representation of the response
     */
    responseXML:null,
    /**
     * readonly status code of the response
     */
    status:null,
    /**
     * readonly status text of the response
     */
    statusText:null,

    constructor_:function (params) {
        this._callSuper("constructor_", params);
        this._initDefaultFinalizableFields();

        this._XHRConst = myfaces._impl.xhrCore.engine.XhrConst;
        this._Lang.applyArgs(this, params);
    },

    //open send, abort etc... abstract
    /**
     * opens the transport element
     * @param {String} method transport method allowed values <i>POST</i> and <i>GET</i>
     * @param {String} url optional url
     * @param {Boolean} async optional param asynchronous transmission if set to true
     */
    open:function (method, url, async) {
        this._implementThis();
    },
    /**
     * send method
     * @param {Object} formData data to be posted within the request
     */
    send:function (formData) {
        this._implementThis();
    },
    /**
     * appends a key value pair to the request header if possible
     * @param {String} key the key of the request header entry
     * @param {String} value  the value for the key
     */
    setRequestHeader:function (key, value) {
        this._implementThis();
    },
    /**
     * aborts the transmission
     */
    abort:function () {
        this._implementThis();
    },

    //empty implementations for the callback handlers
    /**
     * callback once the transmission has started
     * @param evt
     */
    onloadstart:function (evt) {
    },
    onprogress:function (evt) {
    },
    onabort:function (evt) {
    },
    onerror:function (evt) {
    },
    onload:function (evt) {
    },
    ontimeout:function (evt) {
    },
    onloadend:function (evt) {
    },
    onreadystatechange:function (evt) {
    },

    _implementThis:function () {
        throw Error("the function needs to be implemented");
    }
});