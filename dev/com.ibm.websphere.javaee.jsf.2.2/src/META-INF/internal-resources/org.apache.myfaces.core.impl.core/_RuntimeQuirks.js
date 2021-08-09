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
if (!document.querySelectorAll || !window.XMLHttpRequest) {

    //initial browser detection, we encapsule it in a closure
    //to drop all temporary variables from ram as soon as possible
    //we run into the quirks fallback if XMLHttpRequest is not enabled
    (function() {
        var _T  = myfaces._impl.core._Runtime;

        _T.getXHRObject = function() {
            //since this is a global object ie hates it if we do not check for undefined
            if (window.XMLHttpRequest) {
                var _ret = new XMLHttpRequest();
                //we now check the xhr level
                //sendAsBinary = 1.5 which means mozilla only
                //upload attribute present == level2

                if (!_T.XHR_LEVEL) {
                    var _e = _T.exists;
                    _T.XHR_LEVEL = (_e(_ret, "sendAsBinary")) ? 1.5 : 1;
                    _T.XHR_LEVEL = (_e(_ret, "upload") && 'undefined' != typeof FormData) ? 2 : _T.XHR_LEVEL;
                }
                return _ret;
            }
            //IE
            try {
                _T.XHR_LEVEL = 1;
                return new ActiveXObject("Msxml2.XMLHTTP");
            } catch (e) {

            }
            return new ActiveXObject('Microsoft.XMLHTTP');
        };


    })();
}