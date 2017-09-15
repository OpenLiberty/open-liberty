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
 * @name _IFrameRequest
 * @memberOf myfaces._impl.xhrCore
 * @extends myfaces._impl.xhrCore._AjaxRequest
 * @description specialized implementation of the jsf js ajax request class
 * which utilizes an iframe transport for communications to the server
 */
_MF_CLS(_PFX_XHR+"_IFrameRequest", myfaces._impl.xhrCore._AjaxRequest,
        /** @lends myfaces._impl.xhrCore._IFrameRequest.prototype */
        {

    /**
     * @constant
     * @description request marker that the request is an iframe based request
     */
    //JX_PART_IFRAME: "javax.faces.partial.iframe",
    /**
     * @constant
     * @description request marker that the request is an apache myfaces iframe request based request
     */
    MF_PART_IFRAME: "javax.faces.transport.iframe",

    MF_PART_FACES_REQUEST: "javax.faces.request",


    constructor_: function(arguments) {
        this._callSuper("constructor_", arguments);
    },

    getFormData: function() {
        var ret = new myfaces._impl.xhrCore.engine.FormData(this._sourceForm);
        //marker that this is an ajax iframe request
        //ret.append(this.JX_PART_IFRAME, "true");
        ret.append(this.MF_PART_IFRAME, "true");
        ret.append(this.MF_PART_FACES_REQUEST, "partial/ajax");

        var viewState = decodeURIComponent(jsf.getViewState(this._sourceForm));
        viewState = viewState.split("&");

        //we append all viewstate values which are not part of the original form
        //just in case getViewState is decorated
        for(var cnt = 0, len = viewState.length; cnt < len; cnt++) {
            var currViewState = viewState[cnt];
            var keyVal = currViewState.split("=");
            var name = keyVal[0];
            if(!this._Dom.getNamedElementFromForm(this._sourceForm, name)) {
                ret.append(name, keyVal[1]);
            }
        }

        return ret;
    },

    _formDataToURI: function(/*formData*/) {
        //http get alwyays sends the form data
        return "";
    },

    _getTransport: function() {
        return new myfaces._impl.xhrCore.engine.IFrame();
    }

});