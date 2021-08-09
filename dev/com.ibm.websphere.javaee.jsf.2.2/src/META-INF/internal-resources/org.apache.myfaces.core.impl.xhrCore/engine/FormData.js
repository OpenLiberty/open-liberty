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
 * @name FormData
 * @memberOf myfaces._impl.xhrCore.engine
 * @description
 *
 * html5 formdata object emulation for the iframe
 */
_MF_CLS(_PFX_XHR+"engine.FormData", Object,
        /** @lends myfaces._impl.xhrCore.engine.FormData.prototype */
        {
            form: null,
            viewstate: null,
            _appendedParams: {},

            constructor_: function(form) {
                this.form = form;
            },

            append: function(key, value) {
                this._appendedParams[key] = true;
                if (this.form) {
                    this._appendHiddenValue(key, value);
                }
            },

            _finalize: function() {
                this._removeAppendedParams();
            },

            _appendHiddenValue: function(key, value) {
                if ('undefined' == typeof value) {
                    return;
                }
                var _Dom = myfaces._impl._util._Dom;
                var input = _Dom.createElement("input", {
                    "type": "hidden", "name": key, "style": "display:none", "value": value
                });

                this.form.appendChild(input);
            },

            _removeAppendedParams: function() {
                if (!this.form) return;
                for (var cnt = this.form.elements.length - 1; cnt >= 0; cnt--) {
                    var elem = this.form.elements[cnt];
                    if (this._appendedParams[elem.name] && elem.type == "hidden") {
                        elem.parentNode.removeChild(elem);
                        delete elem;
                    }
                }
                this._appendedParams = {};
            }

        });