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
 * like form data with file uploads.
 * This is a replacement for the iframe request which we used until now
 * The iframe method works on older browsers but most likely will
 * be cut off in future browsers, because there is an alternative
 * in form of FormData.
 */
_MF_CLS(_PFX_XHR+"_FormDataRequest", myfaces._impl.xhrCore._AjaxRequest, {
    _AJAXUTIL: myfaces._impl.xhrCore._AjaxUtils,

    constructor_: function(arguments) {
        this._callSuper("constructor_", arguments);
    },

    /**
     * Spec. 13.3.1
     * Collect and encode input elements.
     * Additionally, the hidden element javax.faces.ViewState
     * Enhancement partial page submit
     *
     * @return  an element of formDataWrapper
     * which keeps the final Send Representation of the
     */
    getFormData : function() {
        var _AJAXUTIL = this._AJAXUTIL, myfacesOptions = this._context.myfaces, ret = null;


        //now this is less performant but we have to call it to allow viewstate decoration
        if (!this._partialIdsArray || !this._partialIdsArray.length) {
            ret = new FormData();
            _AJAXUTIL.encodeSubmittableFields(ret, this._sourceForm);
            //just in case the source item is outside of the form
            //only if the form override is set we have to append the issuing item
            //otherwise it is an element of the parent form
            // Backported MYFACES-4606
            if (this._source && !this._isBehaviorEvent()) {
                _AJAXUTIL.appendIssuingItem(this._source, ret);
            }
        } else {
            ret = new FormData();
            _AJAXUTIL.encodeSubmittableFields(ret, this._sourceForm, this._partialIdsArray);
            // Backported MYFACES-4606
            if (this._source && !this._isBehaviorEvent()) {
                _AJAXUTIL.appendIssuingItem(this._source, ret);
            }
        }

        return ret;
    },

    _getTransport: function() {
        return new XMLHttpRequest();
    },

    _applyContentType: function(xhr) {
        //by overriding we let the form data and xhr object
        //figure it out themselves (auto multipart)
    }

});