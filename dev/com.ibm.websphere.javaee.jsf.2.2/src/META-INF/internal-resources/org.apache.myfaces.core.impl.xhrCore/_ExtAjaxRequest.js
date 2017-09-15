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
 * Extendend functionality
 * like issuing element outside of a form
 * and partial page submit
 *
 * Author: Werner Punz (latest modification by $Author: ganeshpuri $)
 * Version: $Revision: 1.4 $ $Date: 2009/05/31 09:16:44 $
 */

//partial extension for the ajax request
myfaces._impl.xhrCore._AjaxRequest = _MF_CLS(_PFX_XHR + "_ExtAjaxRequest", myfaces._impl.xhrCore._AjaxRequest , /** @lends myfaces._impl.xhrCore._ExtAjaxRequest.prototype */ {
    constructor_: function(args) {
        this._callSuper("constructor_", args);
    },

    /**
     * Spec. 13.3.1
     * Collect and encode input elements.
     * Additionally the hidden element javax.faces.ViewState
     * Enhancement partial page submit
     *
     * @return  an element of formDataWrapper
     * which keeps the final Send Representation of the
     */
    getFormData : function() {
        var _AJAXUTIL = this._AJAXUTIL, myfacesOptions = this._context.myfaces, ret = null;

        //now this is less performant but we have to call it to allow viewstate decoration
        if (!this._partialIdsArray || !this._partialIdsArray.length) {
            ret = this._callSuper("getFormData");
            //just in case the source item is outside of the form
            //only if the form override is set we have to append the issuing item
            //otherwise it is an element of the parent form
            if (this._source && myfacesOptions && myfacesOptions.form)
                _AJAXUTIL.appendIssuingItem(this._source, ret);
        } else {
            ret = this._Lang.createFormDataDecorator(new Array());
            _AJAXUTIL.encodeSubmittableFields(ret, this._sourceForm, this._partialIdsArray);
            if (this._source && myfacesOptions && myfacesOptions.form)
                _AJAXUTIL.appendIssuingItem(this._source, ret);

        }
        return ret;
    }

});
