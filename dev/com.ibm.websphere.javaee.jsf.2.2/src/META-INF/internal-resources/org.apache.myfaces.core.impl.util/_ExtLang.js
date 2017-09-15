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
_MF_SINGLTN && _MF_SINGLTN(_PFX_UTIL + "_ExtLang", myfaces._impl._util._Lang, {

    _RT:myfaces._impl.core._Runtime,

    constructor_:function () {
        this._callSuper("constructor_");
        var _T = this;
        //we only apply lazy if the jsf part is loaded already
        //otherwise we are at the correct position
        if (myfaces._impl.core.Impl) {
            this._RT.iterateClasses(function (proto) {
                if (proto._Lang) {
                    proto._Lang = _T;
                }
            });
        }

        myfaces._impl._util._Lang = _T;
    },

    /**
     * consume event in a browser independend manner
     * @param event the event which should not be propagated anymore
     */
    consumeEvent:function (event) {
        //w3c model vs ie model again
        event = event || window.event;
        (event.stopPropagation) ? event.stopPropagation() : event.cancelBubble = true;
    },
    /**
     * escapes a strings special chars (crossported from dojo 1.3+)
     *
     * @param str the string
     *
     * @param except a set of exceptions
     */
    escapeString:function (/*String*/str, /*String?*/except) {
        //	summary:
        //		Adds escape sequences for special characters in regular expressions
        // except:
        //		a String with special characters to be left unescaped
        return str.replace(/([\.$?*|:{}\(\)\[\]\\\/\+^])/g, function (ch) {
            if (except && except.indexOf(ch) != -1) {
                return ch;
            }
            return "\\" + ch;
        }); // String
    },
    /**
     * Helper function to provide a trim with a given splitter regular expression
     * @param {String} it the string to be trimmed
     * @param {RegExp} splitter the splitter regular expressiion
     *
     * FIXME is this still used?
     */
    trimStringInternal:function (it, splitter) {
        return this.strToArray(it, splitter).join(splitter);
    },
    /**
     * creates a standardized error message which can be reused by the system
     *
     * @param sourceClass the source class issuing the exception
     * @param func the function issuing the exception
     * @param error the error object itself (optional)
     */
    createErrorMsg:function (sourceClass, func, error) {
        var ret = [];
        var keyValToStr = this.hitch(this, this.keyValToStr),
                getMsg = this.hitch(this, this.getMessage),
                pushRet = this.hitch(ret, ret.push);
        pushRet(keyValToStr(getMsg("MSG_AFFECTED_CLASS"), sourceClass));
        pushRet(keyValToStr(getMsg("MSG_AFFECTED_METHOD"), func));
        /*we push the values into separate vars to improve the compression*/
        var errName = error.name;
        var errMsg = error.message;
        var errDesc = error.description;
        var errNum = error.number;
        var errLineNo = error.lineNumber;
        if (error) {
            var _UDEF = "undefined";
            pushRet(keyValToStr(getMsg("MSG_ERROR_NAME"), errName ? errName : _UDEF));
            pushRet(keyValToStr(getMsg("MSG_ERROR_MESSAGE"), errMsg ? errMsg : _UDEF));
            pushRet(keyValToStr(getMsg("MSG_ERROR_DESC"), errDesc ? errDesc : _UDEF));
            pushRet(keyValToStr(getMsg("MSG_ERROR_NO"), _UDEF != typeof errNum ? errNum : _UDEF));
            pushRet(keyValToStr(getMsg("MSG_ERROR_LINENO"), _UDEF != typeof errLineNo ? errLineNo : _UDEF));
        }
        return ret.join("");
    }
});