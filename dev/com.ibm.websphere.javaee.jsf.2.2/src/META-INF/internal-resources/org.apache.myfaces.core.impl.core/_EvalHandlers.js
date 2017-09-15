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
 * @namespace
 * @name window
 * @description Eval routines, depending on the browser.
 * <p/>
 * The problem solved in this class is the problem on how to perform
 * a global eval on multiple browsers. Some browsers auto eval themselves
 * they do not need to be called
 * <li>Some work with a window.eval.call(window,... </li>
 * <li>Others use simply execScript <li>
 * <li>Some others work only with the head appendix method
 * head.appendChild(&lt;script...., head.removeChild(&lt;script </li>
 * <p/>
 * Note: The code here already is precompressed because the compressor
 * fails on it, the deficits in readability will be covered by more comments
 *
 */


if (!window.myfaces) {
    /**
     * @namespace
     * @name myfaces
     */
    var myfaces = new function() {
    };
    window.myfaces = myfaces;
}

/**
 * @memberOf myfaces
 * @namespace
 * @name _impl
 */
myfaces._impl = (myfaces._impl) ? myfaces._impl : {};
/**
 * @memberOf myfaces._impl
 * @namespace
 * @name core
 */
myfaces._impl.core = (myfaces._impl.core) ? myfaces._impl.core :{};

if (!myfaces._impl.core._EvalHandlers) {
    /**
     * @memberOf myfaces._impl.core
     * @namespace
     * @name _EvalHandlers
     */
    myfaces._impl.core._EvalHandlers = new function() {
        //the rest of the namespaces can be handled by our namespace feature
        //helper to avoid unneeded hitches
        /**
         * @borrows myfaces._impl.core._Runtime as _T
         */
        var _T = this;

        /*cascaded eval methods depending upon the browser*/

        /**
         * @function
         * @param code

         *
         * evals a script globally using exec script (ie6 fallback)
         * @param {String} code the code which has to be evaluated
         * @borrows myfaces._impl.core._Runtime as _T
         *
         * TODO eval if we cannot replace this method with the head appendix
         * method which is faster for ie this also would reduce our code
         * by a few bytes
         */
        _T._evalExecScript = function(code) {
            //execScript definitely only for IE otherwise we might have a custom
            //window extension with undefined behavior on our necks
            //window.execScript does not return anything
            //on htmlunit it return "null object"
            //_r == ret
            var _r = window.execScript(code);
            if ('undefined' != typeof _r && _r == "null" /*htmlunit bug*/) {
                return null;
            }
            return _r;
        };

        /**
         * flakey head appendix method which does not work in the correct
         * order or at all for all modern browsers
         * but seems to be the only method which works on blackberry correctly
         * hence we are going to use it as fallback
         *
         * @param {String} code the code part to be evaled
         * @borrows myfaces._impl.core._Runtime as _T
         */
        _T._evalHeadAppendix = function(code) {
            //_l == location
            var _l = document.getElementsByTagName("head")[0] || document.documentElement;
            //_p == placeHolder
            var _p = document.createElement("script");
            _p.type = "text/javascript";
            _p.text = code;
            _l.insertBefore(_p, _l.firstChild);
            _l.removeChild(_p);
            return null;
        };

        /**
         * @name myfaces._impl.core._Runtime._standardGlobalEval
         * @private
         * @param {String} code
         */
        _T._standardGlobalEval = function(code) {
            //fix which works in a cross browser way
            //we used to scope an anonymous function
            //but I think this is better
            //the reason is some Firefox versions
            // apply a wrong scope
            //if we call eval by not scoping
            //_U == "undefined"
            var _U = "undefined";
            var gEval = function () {
                //_r == retVal;
                var _r = window.eval.call(window, code);
                if (_U == typeof _r) return null;
                return _r;
            };
            var _r = gEval();
            if (_U == typeof _r) return null;
            return _r;
        };

        /**
         * global eval on scripts
         * @param {String} c (code abbreviated since the compression does not work here)
         * @name myfaces._impl.core._Runtime.globalEval
         * @function
         */
        _T.globalEval = function(c) {
            //TODO add a config param which allows to evaluate global scripts even if the call
            //is embedded in an iframe
            //We lazy init the eval type upon the browsers
            //capabilities   
            var _e = "_evalType";
            var _w = window;
            var _b = myfaces._impl.core._Runtime.browser;
            //central routine to determine the eval method
            if (!_T[_e]) {
                //execScript supported
                _T[_e] = _w.execScript ? "_evalExecScript" : null;

                //in case of no support we go to the standard global eval  window.eval.call(window,
                // with Firefox fixes for scoping
                _T[_e] = _T[_e] ||(( _w.eval && (!_b.isBlackBerry ||_b.isBlackBerry >= 6)) ? "_standardGlobalEval" : null);

                //this one routes into the hed appendix method
                _T[_e] = _T[_e] ||((_w.eval ) ? "_evalHeadAppendix" : null);
            }
            if (_T[_e]) {
                //we now execute the eval method
                return _T[_T[_e]](c);
            }
            //we probably have covered all browsers, but this is a safety net which might be triggered
            //by some foreign browser which is not covered by the above cases
            eval.call(window, c);
            return null;
        };

    };
}