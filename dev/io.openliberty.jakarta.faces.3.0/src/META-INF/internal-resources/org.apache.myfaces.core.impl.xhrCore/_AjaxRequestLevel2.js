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
 * this method is used only for pure multipart form parts
 * otherwise the normal method is used
 * IT is a specialized request which uses the form data
 * element for the handling of forms
 */
_MF_CLS(_PFX_XHR + "_MultipartAjaxRequestLevel2", myfaces._impl.xhrCore._AjaxRequest, {

    _sourceForm:null,

    constructor_:function (args) {
        this._callSuper("constructor_", args);
        //TODO xhr level2 can deal with real props

    },

    getFormData:function () {
        var ret;
        //in case of a multipart form post we savely can use the FormData object
        if (this._context._mfInternal.xhrOp === "multipartQueuedPost") {
            ret = new FormData(this._sourceForm);
            this._AJAXUTIL.appendIssuingItem(this._source, ret);
        } else {
            //we switch back to the encode submittable fields system
            this._AJAXUTIL.encodeSubmittableFields(ret, this._sourceForm, null);
            this._AJAXUTIL.appendIssuingItem(this._source, ret);
        }
        return ret;
    },

    /**
     * applies the content type, this needs to be done only for xhr
     * level1
     * @param xhr
     * @private
     */
    _applyContentType:function (xhr) {
        //content type is not set in case of xhr level2 because
        //the form data object does it itself
    },

    _formDataToURI:function (formData) {
        //in xhr level2 form data takes care of the http get parametrisation
        return "";
    },

    _getTransport:function () {
        return new XMLHttpRequest();
    }
});

/**
 * for normal requests we basically use
 * only the xhr level2 object but
 */
_MF_CLS(_PFX_XHR + "_AjaxRequestLevel2", myfaces._impl.xhrCore._AjaxRequest, {

    _sourceForm:null,

    constructor_:function (args) {
        this._callSuper("constructor_", args);
        //TODO xhr level2 can deal with real props

    },

    _getTransport:function () {
        return new XMLHttpRequest();
    }
});




