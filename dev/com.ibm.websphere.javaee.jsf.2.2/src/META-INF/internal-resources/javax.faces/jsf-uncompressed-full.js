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
 * Runtime/Startup class
 * this is the central class which initializes all base mechanisms
 * used by the rest of the system such as
 * a) namespacing system
 * b) browser detection
 * c) loose configuration coupling
 * d) utils methods to fetch the implementation
 * e) ajaxed script loading
 * f) global eval (because it is used internally)
 * g) Structural base patterns as singleton, delegate and inheritance
 *
 * Note this class is self contained and must!!! be loaded
 * as absolute first class before going into anything else
 *
 *
 */
/** @namespace myfaces._impl.core._Runtime*/

myfaces._impl.core = (myfaces._impl.core) ? myfaces._impl.core : {};
//now this is the only time we have to do this cascaded and manually
//for the rest of the classes our reserveNamespace function will do the trick
//Note, this class uses the classical closure approach (to save code)
//it cannot be inherited by our inheritance mechanism, but must be delegated
//if you want to derive from it
//closures and prototype inheritance do not mix, closures and delegation however do
/**
 * @ignore
 */
if (!myfaces._impl.core._Runtime) {
    /**
     * @memberOf myfaces._impl.core
     * @namespace
     * @name _Runtime
     */
    myfaces._impl.core._Runtime = new function() {
        //the rest of the namespaces can be handled by our namespace feature
        //helper to avoid unneeded hitches
        /**
         * @borrows myfaces._impl.core._Runtime as _T
         */
        var _T = this;

        //namespace idx to speed things up by hitting eval way less
        this._reservedNMS = {};
        this._registeredSingletons = {};
        this._registeredClasses = [];
        /**
         * replacement counter for plugin classes
         */
        this._classReplacementCnt = 0;

        /**
         * global eval on scripts
         * @param {String} code
         * @name myfaces._impl.core._Runtime.globalEval
         * @function
         */
        _T.globalEval = function(code) {
            return myfaces._impl.core._EvalHandlers.globalEval(code);
        };

        /**
         * applies an object to a namespace
         * basically does what bla.my.name.space = obj does
         * note we cannot use var myNameSpace = fetchNamespace("my.name.space")
         * myNameSpace = obj because the result of fetch is already the object
         * which the namespace points to, hence this function
         *
         * @param {String} nms the namespace to be assigned to
         * @param {Object} obj the  object to be assigned
         * @name myfaces._impl.core._Runtime.applyToGlobalNamespace
         * @function
         */
        _T.applyToGlobalNamespace = function(nms, obj) {
            var splitted = nms.split(/\./);
            if (splitted.length == 1) {
                window[nms] = obj;
                return;
            }
            var parent = splitted.slice(0, splitted.length - 1);
            var child = splitted[splitted.length - 1];
            var parentNamespace = _T.fetchNamespace(parent.join("."));
            parentNamespace[child] = obj;
        };

        /**
         * fetches the object the namespace points to
         * @param {String} nms the namespace which has to be fetched
         * @return the object the namespace points to or null if nothing is found
         */
        this.fetchNamespace = function(nms) {
            if ('undefined' == typeof nms || null == nms || !_T._reservedNMS[nms]) {
                return null;
            }

            var ret = null;
            try {
                //blackberries have problems as well in older non webkit versions
                if (!_T.browser.isIE) {
                    //in ie 6 and 7 we get an error entry despite the suppression
                    ret = _T.globalEval("window." + nms);
                }
                //namespace could point to numeric or boolean hence full
                //save check

            } catch (e) {/*wanted*/
            }
            //ie fallback for some ie versions path because it cannot eval namespaces
            //ie in any version does not like that particularily
            //we do it the hard way now
            if ('undefined' != typeof ret && null != ret) {
                return ret;
            }
            return _T._manuallyResolveNMS(nms);

        };

        _T._manuallyResolveNMS = function(nms) {
             //ie fallback for some ie versions path because it cannot eval namespaces
            //ie in any version does not like that particularily
            //we do it the hard way now

            nms = nms.split(/\./);
            var ret = window;
            var len = nms.length;

            for (var cnt = 0; cnt < len; cnt++) {
                ret = ret[nms[cnt]];
                if ('undefined' == typeof ret || null == ret) {
                    return null;
                }
            }
            return ret;
        };

        /**
         * Backported from dojo
         * a failsafe string determination method
         * (since in javascript String != "" typeof alone fails!)
         * @param {Object} it  the object to be checked for being a string
         * @return {boolean} true in case of being a string false otherwise
         */
        this.isString = function(/*anything*/ it) {
            //	summary:
            //		Return true if it is a String
            return !!arguments.length && it != null && (typeof it == "string" || it instanceof String); // Boolean
        };

        /**
         * reserves a namespace in the specific scope
         *
         * usage:
         * if(_T.reserve("org.apache.myfaces.MyUtils")) {
         *      org.apache.myfaces.MyUtils = function() {
         *      }
         * }
         *
         * reserves a namespace and if the namespace is new the function itself is reserved
         *
         *
         *
         * or:
         * _T.reserve("org.apache.myfaces.MyUtils", function() { .. });
         *
         * reserves a namespace and if not already registered directly applies the function the namespace
         *
         * note for now the reserved namespaces reside as global maps justl like jsf.js but
         * we also use a speedup index which is kept internally to reduce the number of evals or loops to walk through those
         * namespaces (eval is a heavy operation and loops even only for namespace resolution introduce (O)2 runtime
         * complexity while a simple map lookup is (O)log n with additional speedup from the engine.
         *
         *
         * @param {String} nms
         * @returns {boolean} true if it was not provided
         * false otherwise for further action
         */
        this.reserveNamespace = function(nms, obj) {

            if (!_T.isString(nms)) {
                throw Error("Namespace must be a string with . as delimiter");
            }
            if (_T._reservedNMS[nms] || null != _T.fetchNamespace(nms)) {
                return false;
            }

            var entries = nms.split(/\./);
            var currNms = window;

            var tmpNmsName = [];
            var  UDEF = "undefined";
            for (var cnt = 0; cnt < entries.length; cnt++) {
                var subNamespace = entries[cnt];
                tmpNmsName.push(subNamespace);
                if (UDEF == typeof currNms[subNamespace]) {
                    currNms[subNamespace] = {};
                }
                if (cnt == entries.length - 1 && UDEF != typeof obj) {
                    currNms[subNamespace] = obj;
                } else {
                    currNms = currNms[subNamespace];
                }
                _T._reservedNMS[tmpNmsName.join(".")] = true;
            }
            return true;
        };

        /**
         * iterates over all registered singletons in the namespace
         * @param operator a closure which applies a certain function
         * on the namespace singleton
         */
        this.iterateSingletons = function(operator) {
            var singletons = _T._registeredSingletons;
            for(var key in singletons) {
                var nms = _T.fetchNamespace(key);
                operator(nms);
            }
        };
        /**
         * iterates over all registered singletons in the namespace
         * @param operator a closure which applies a certain function
         * on the namespace singleton
         */
        this.iterateClasses = function(operator) {
            var classes = _T._registeredClasses;
            for(var cnt  = 0; cnt < classes.length; cnt++) {
                operator(classes[cnt], cnt);
            }
        };

        /**
         * check if an element exists in the root
         * also allows to check for subelements
         * usage
         * _T.exists(rootElem,"my.name.space")
         * @param {Object} root the root element
         * @param {String} subNms the namespace
         */
        this.exists = function(root, subNms) {
            if (!root) {
                return false;
            }
            //special case locally reserved namespace
            if (root == window && _T._reservedNMS[subNms]) {
                return true;
            }

            //initial condition root set element not set or null
            //equals to element exists
            if (!subNms) {
                return true;
            }
            var UDEF = "undefined";
            try {
                //special condition subnamespace exists as full blown key with . instead of function map
                if (UDEF != typeof root[subNms]) {
                    return true;
                }

                //crossported from the dojo toolkit
                // summary: determine if an object supports a given method
                // description: useful for longer api chains where you have to test each object in the chain
                var p = subNms.split(".");
                var len = p.length;
                for (var i = 0; i < len; i++) {
                    //the original dojo code here was false because
                    //they were testing against ! which bombs out on exists
                    //which has a value set to false
                    // (TODO send in a bugreport to the Dojo people)

                    if (UDEF == typeof root[p[i]]) {
                        return false;
                    } // Boolean
                    root = root[p[i]];
                }
                return true; // Boolean

            } catch (e) {
                //ie (again) has a special handling for some object attributes here which automatically throw an unspecified error if not existent
                return false;
            }
        };



        /**
         * fetches a global config entry
         * @param {String} configName the name of the configuration entry
         * @param {Object} defaultValue
         *
         * @return either the config entry or if none is given the default value
         */
        this.getGlobalConfig = function(configName, defaultValue) {
            /**
             * note we could use exists but this is an heavy operation, since the config name usually
             * given this function here is called very often
             * is a single entry without . in between we can do the lighter shortcut
             */
            return (myfaces["config"] && 'undefined' != typeof myfaces.config[configName] ) ?
                    myfaces.config[configName]
                    :
                    defaultValue;
        };

        /**
         * gets the local or global options with local ones having higher priority
         * if no local or global one was found then the default value is given back
         *
         * @param {String} configName the name of the configuration entry
         * @param {String} localOptions the local options root for the configuration myfaces as default marker is added implicitely
         *
         * @param {Object} defaultValue
         *
         * @return either the config entry or if none is given the default value
         */
        this.getLocalOrGlobalConfig = function(localOptions, configName, defaultValue) {
            /*use(myfaces._impl._util)*/
            var _local = !!localOptions;
            var _localResult;
            var MYFACES = "myfaces";

            if (_local) {
                //note we also do not use exist here due to performance improvement reasons
                //not for now we loose the subnamespace capabilities but we do not use them anyway
                //this code will give us a performance improvement of 2-3%
                _localResult = (localOptions[MYFACES]) ? localOptions[MYFACES][configName] : undefined;
                _local = "undefined" != typeof _localResult;
            }

            return (!_local) ? _T.getGlobalConfig(configName, defaultValue) : _localResult;
        };

        /**
         * determines the xhr level which either can be
         * 1 for classical level1
         * 1.5 for mozillas send as binary implementation
         * 2 for xhr level 2
         */
        this.getXHRLvl = function() {
            if (!_T.XHR_LEVEL) {
                _T.getXHRObject();
            }
            return _T.XHR_LEVEL;
        };

        /**
         * encapsulated xhr object which tracks down various implementations
         * of the xhr object in a browser independent fashion
         * (ie pre 7 used to have non standard implementations because
         * the xhr object standard came after IE had implemented it first
         * newer ie versions adhere to the standard and all other new browsers do anyway)
         *
         * @return the xhr object according to the browser type
         */
        this.getXHRObject = function() {
            var _ret = new XMLHttpRequest();
            //we now check the xhr level
            //sendAsBinary = 1.5 which means mozilla only
            //upload attribute present == level2
            var XHR_LEVEL = "XHR_LEVEL";
            if (!_T[XHR_LEVEL]) {
                var _e = _T.exists;
                _T[XHR_LEVEL] = (_e(_ret, "sendAsBinary")) ? 1.5 : 1;
                _T[XHR_LEVEL] = (_e(_ret, "upload") && 'undefined' != typeof FormData) ? 2 : _T.XHR_LEVEL;
            }
            return _ret;
        };

        /**
         * loads a script and executes it under a global scope
         * @param {String} src  the source of the script
         * @param {String} type the type of the script
         * @param {Boolean} defer  defer true or false, same as the javascript tag defer param
         * @param {String} charSet the charset under which the script has to be loaded
         * @param {Boolean} async tells whether the script can be asynchronously loaded or not, currently
         * not used
         */
        this.loadScriptEval = function(src, type, defer, charSet, async) {
            var xhr = _T.getXHRObject();
            xhr.open("GET", src, false);

            if (charSet) {
                xhr.setRequestHeader("Content-Type", "application/x-javascript; charset:" + charSet);
            }

            xhr.send(null);

            //since we are synchronous we do it after not with onReadyStateChange

            if (xhr.readyState == 4) {
                if (xhr.status == 200) {
                    //defer also means we have to process after the ajax response
                    //has been processed
                    //we can achieve that with a small timeout, the timeout
                    //triggers after the processing is done!
                    if (!defer) {
                        //we moved the sourceurl notation to # instead of @ because ie does not cover it correctly
                        //newer browsers understand # including ie since windows 8.1
                        //see http://updates.html5rocks.com/2013/06/sourceMappingURL-and-sourceURL-syntax-changed
                        _T.globalEval(xhr.responseText.replace("\n", "\r\n") + "\r\n//# sourceURL=" + src);
                    } else {
                        //TODO not ideal we maybe ought to move to something else here
                        //but since it is not in use yet, it is ok
                        setTimeout(function() {
                            _T.globalEval(xhr.responseText.replace("\n", "\r\n") + "\r\n//# sourceURL=" + src);
                        }, 1);
                    }
                } else {
                    throw Error(xhr.responseText);
                }
            } else {
                throw Error("Loading of script " + src + " failed ");
            }

        };

        /**
         * load script functionality which utilizes the browser internal
         * script loading capabilities
         *
         * @param {String} src  the source of the script
         * @param {String} type the type of the script
         * @param {Boolean} defer  defer true or false, same as the javascript tag defer param
         * @param {String} charSet the charset under which the script has to be loaded
         */
        this.loadScriptByBrowser = function(src, type, defer, charSet, async) {
            //if a head is already present then it is safer to simply
            //use the body, some browsers prevent head alterations
            //after the first initial rendering

            //ok this is nasty we have to do a head modification for ie pre 8
            //the rest can be finely served with body
            var position = "head";
            var UDEF = "undefined";
            try {
                var holder = document.getElementsByTagName(position)[0];
                if (UDEF == typeof holder || null == holder) {
                    holder = document.createElement(position);
                    var html = document.getElementsByTagName("html");
                    html.appendChild(holder);
                }
                var script = document.createElement("script");

                script.type = type || "text/javascript";
                script.src = src;
                if (charSet) {
                    script.charset = charSet;
                }
                if (defer) {
                    script.defer = defer;
                }
                /*html5 capable browsers can deal with script.async for
                 * proper head loading*/
                if (UDEF != typeof script.async) {
                    script.async = async;
                }
                holder.appendChild(script);

            } catch (e) {
                //in case of a loading error we retry via eval
                return false;
            }

            return true;
        };

        this.loadScript = function(src, type, defer, charSet, async) {
            //the chrome engine has a nasty javascript bug which prevents
            //a correct order of scripts being loaded
            //if you use script source on the head, we  have to revert
            //to xhr+ globalEval for those
            var b = _T.browser;
            if (!b.isFF && !b.isWebkit && !b.isOpera >= 10) {
                _T.loadScriptEval(src, type, defer, charSet);
            } else {
                //only firefox keeps the order, sorry ie...
                _T.loadScriptByBrowser(src, type, defer, charSet, async);
            }
        };

        //Base Patterns, Inheritance, Delegation and Singleton



        /*
         * prototype based delegation inheritance
         *
         * implements prototype delegaton inheritance dest <- a
         *
         * usage
         * <pre>
         *  var newClass = _T.extends( function (var1, var2) {
         *                                          _T._callSuper("constructor", var1,var2);
         *                                     };
         *                                  ,origClass);
         *
         *       newClass.prototype.myMethod = function(arg1) {
         *              _T._callSuper("myMethod", arg1,"hello world");
         *       ....
         *
         * other option
         *
         * myfaces._impl._core._Runtime.extends("myNamespace.newClass", parent, {
         *                              init: function() {constructor...},
         *                              method1: function(f1, f2) {},
         *                              method2: function(f1, f2,f3) {
         *                                  _T._callSuper("method2", F1,"hello world");
         *                              }
         *              });
         * </p>
         * @param {function|String} newCls either a unnamed function which can be assigned later or a namespace
         * @param {function} extendCls the function class to be extended
         * @param {Object} protoFuncs (Map) an optional map of prototype functions which in case of overwriting a base function get an inherited method
         *
         * To explain further
         * prototype functions:
         * <pre>
         *  newClass.prototype.<prototypeFunction>
         * namspace function
         *  newCls.<namespaceFunction> = function() {...}
         *  </pre>
         */

        this.extendClass = function(newCls, extendCls, protoFuncs, nmsFuncs) {

            if (!_T.isString(newCls)) {
                throw Error("new class namespace must be of type String");
            }
            var className = newCls;

            if (_T._reservedNMS[newCls]) {
                return _T.fetchNamespace(newCls);
            }
            var constr = "constructor_";
            var parClassRef = "_mfClazz";
            if(!protoFuncs[constr]) {
              protoFuncs[constr] =  (extendCls[parClassRef]  || (extendCls.prototype && extendCls.prototype[parClassRef])) ?
                      function() {this._callSuper("constructor_");}: function() {};
              var assigned = true;
            }

            if ('function' != typeof newCls) {
                newCls = _reserveClsNms(newCls, protoFuncs);
                if (!newCls) return null;
            }
            //if the type information is known we use that one
            //with this info we can inherit from objects also
            //instead of only from classes
            //sort of like   this.extendClass(newCls, extendObj._mfClazz...

            if (extendCls[parClassRef]) {
                extendCls = extendCls[parClassRef];
            }

            if ('undefined' != typeof extendCls && null != extendCls) {
                //first we have to get rid of the constructor calling problem
                //problem
                var tmpFunc = function() {
                };
                tmpFunc.prototype = extendCls.prototype;

                var newClazz = newCls;
                newClazz.prototype = new tmpFunc();
                tmpFunc = null;
                var clzProto = newClazz.prototype;
                clzProto.constructor = newCls;
                clzProto._parentCls = extendCls.prototype;
                //in case of overrides the namespace is altered with mfclazz
                //we want the final namespace
                clzProto._nameSpace = className.replace(/(\._mfClazz)+$/,"");
                /**
                 * @ignore
                 */
                clzProto._callSuper = function(methodName) {
                    var passThrough = (arguments.length == 1) ? [] : Array.prototype.slice.call(arguments, 1);
                    var accDescLevel = "_mfClsDescLvl";
                    //we store the descension level of each method under a mapped
                    //name to avoid name clashes
                    //to avoid name clashes with internal methods of array
                    //if we don't do this we trap the callSuper in an endless
                    //loop after descending one level
                    var _mappedName = ["_",methodName,"_mf_r"].join("");
                    this[accDescLevel] = this[accDescLevel] || new Array();
                    var descLevel = this[accDescLevel];
                    //we have to detect the descension level
                    //we now check if we are in a super descension for the current method already
                    //if not we are on this level
                    var _oldDescLevel = this[accDescLevel][_mappedName] || this;
                    //we now step one level down
                    var _parentCls = _oldDescLevel._parentCls;
                    var ret = null;
                    try {
                        //we now store the level position as new descension level for callSuper
                        descLevel[_mappedName] = _parentCls;
                        //and call the code on this
                        if(!_parentCls[methodName]) {
                            throw Error("Method _callSuper('"+ methodName+"')  called from "+className+" Method does not exist ");
                        }
                        ret = _parentCls[methodName].apply(this, passThrough);
                    } finally {
                        descLevel[_mappedName] = _oldDescLevel;
                    }
                    if('undefined' != typeof ret) {
                        return ret;
                    }
                };
                //reference to its own type
                clzProto[parClassRef] = newCls;
                _T._registeredClasses.push(clzProto);
            }

            //we now map the function map in
            _T._applyFuncs(newCls, protoFuncs, true);
            //we could add inherited but that would make debugging harder
            //see http://www.ruzee.com/blog/2008/12/javascript-inheritance-via-prototypes-and-closures on how to do it

            _T._applyFuncs(newCls, nmsFuncs, false);

            return newCls;
        };



        /**
         * Extends a class and puts a singleton instance at the reserved namespace instead
         * of its original class
         *
         * @param {function|String} newCls either a unnamed function which can be assigned later or a namespace
         * @param {function} extendsCls the function class to be extended
         * @param {Object} protoFuncs (Map) an optional map of prototype functions which in case of overwriting a base function get an inherited method
         */
        this.singletonExtendClass = function(newCls, extendsCls, protoFuncs, nmsFuncs) {
            _T._registeredSingletons[newCls] = true;
            return _T._makeSingleton(_T.extendClass, newCls, extendsCls, protoFuncs, nmsFuncs);
        };



        //since the object is self contained and only
        //can be delegated we can work with real private
        //functions here, the other parts of the
        //system have to emulate them via _ prefixes
        this._makeSingleton = function(ooFunc, newCls, delegateObj, protoFuncs, nmsFuncs) {
            if (_T._reservedNMS[newCls]) {
                return _T._reservedNMS[newCls];
            }

            var clazz = ooFunc(newCls + "._mfClazz", delegateObj, protoFuncs, nmsFuncs);
            if (clazz != null) {
                _T.applyToGlobalNamespace(newCls, new clazz());
            }
            return _T.fetchNamespace(newCls)["_mfClazz"] = clazz;
        };

        //internal class namespace reservation depending on the type (string or function)
        var _reserveClsNms = function(newCls, protoFuncs) {
            var constr = null;
            var UDEF = "undefined";
            if (UDEF != typeof protoFuncs && null != protoFuncs) {
                constr = (UDEF != typeof null != protoFuncs['constructor_'] && null != protoFuncs['constructor_']) ? protoFuncs['constructor_'] : function() {
                };
            } else {
                constr = function() {
                };
            }

            if (!_T.reserveNamespace(newCls, constr)) {
                return null;
            }
            newCls = _T.fetchNamespace(newCls);
            return newCls;
        };

        this._applyFuncs = function (newCls, funcs, proto) {
            if (funcs) {
                for (var key in funcs) {
                    //constructor already passed, callSuper already assigned
                    if ('undefined' == typeof key || null == key || key == "_callSuper") {
                        continue;
                    }
                    if (!proto)
                        newCls[key] = funcs[key];
                    else
                        newCls.prototype[key] = funcs[key];
                }
            }
        };

        /**
         * general type assertion routine
         *
         * @param probe the probe to be checked for the correct type
         * @param theType the type to be checked for
         */
        this.assertType = function(probe, theType) {
            return _T.isString(theType) ? probe == typeof theType : probe instanceof theType;
        };

        /**
         * onload wrapper for chaining the onload cleanly
         * @param func the function which should be added to the load
         * chain (note we cannot rely on return values here, hence jsf.util.chain will fail)
         */
        this.addOnLoad = function(target, func) {
            var oldonload = (target) ? target.onload : null;
            target.onload = (!oldonload) ? func : function() {
                try {
                    oldonload();
                } catch (e) {
                    throw e;
                } finally {
                    func();
                }
            };
        };

        /**
         * returns the internationalisation setting
         * for the given browser so that
         * we can i18n our messages
         *
         * @returns a map with following entires:
         * <ul>
         *      <li>language: the lowercase language iso code</li>
         *      <li>variant: the uppercase variant iso code</li>
         * </ul>
         * null is returned if the browser fails to determine the language settings
         */
        this.getLanguage = function(lOverride) {
            var deflt = {language: "en", variant: "UK"}; //default language and variant
            try {
                var lang = lOverride || navigator.language || navigator.browserLanguage;
                if (!lang || lang.length < 2) return deflt;
                return {
                    language: lang.substr(0, 2),
                    variant: (lang.length >= 5) ? lang.substr(3, 5) : null
                };
            } catch(e) {
                return deflt;
            }
        };

        //implemented in extruntime
        this.singletonDelegateObj = function()  {};

        /**
        * browser detection code
        * cross ported from dojo 1.2
        *
        * dojos browser detection code is very sophisticated
        * hence we port it over it allows a very fine grained detection of
        * browsers including the version number
        * this however only can work out if the user
        * does not alter the user agent, which they normally dont!
        *
        * the exception is the ie detection which relies on specific quirks in ie
        */
       var n = navigator;
       var dua = n.userAgent,
               dav = n.appVersion,
               tv = parseFloat(dav);
       var _T = this;
       _T.browser = {};
       myfaces._impl.core._EvalHandlers.browser = _T.browser;
       var d = _T.browser;

       if (dua.indexOf("Opera") >= 0) {
           _T.isOpera = tv;
       }
       if (dua.indexOf("AdobeAIR") >= 0) {
           d.isAIR = 1;
       }
       if (dua.indexOf("BlackBerry") >= 0) {
           d.isBlackBerry = tv;
       }
       d.isKhtml = (dav.indexOf("Konqueror") >= 0) ? tv : 0;
       d.isWebKit = parseFloat(dua.split("WebKit/")[1]) || undefined;
       d.isChrome = parseFloat(dua.split("Chrome/")[1]) || undefined;

       // safari detection derived from:
       //		http://developer.apple.com/internet/safari/faq.html#anchor2
       //		http://developer.apple.com/internet/safari/uamatrix.html
       var index = Math.max(dav.indexOf("WebKit"), dav.indexOf("Safari"), 0);
       if (index && !d.isChrome) {
           // try to grab the explicit Safari version first. If we don't get
           // one, look for less than 419.3 as the indication that we're on something
           // "Safari 2-ish".
           d.isSafari = parseFloat(dav.split("Version/")[1]);
           if (!d.isSafari || parseFloat(dav.substr(index + 7)) <= 419.3) {
               d.isSafari = 2;
           }
       }

       //>>excludeStart("webkitMobile", kwArgs.webkitMobile);

       if (dua.indexOf("Gecko") >= 0 && !d.isKhtml && !d.isWebKit) {
           d.isMozilla = d.isMoz = tv;
       }
       if (d.isMoz) {
           //We really need to get away from _T. Consider a sane isGecko approach for the future.
           d.isFF = parseFloat(dua.split("Firefox/")[1] || dua.split("Minefield/")[1] || dua.split("Shiretoko/")[1]) || undefined;
       }

       if (document.all && !d.isOpera && !d.isBlackBerry) {
           d.isIE = parseFloat(dav.split("MSIE ")[1]) || undefined;
           d.isIEMobile = parseFloat(dua.split("IEMobile")[1]);
           //In cases where the page has an HTTP header or META tag with
           //X-UA-Compatible, then it is in emulation mode, for a previous
           //version. Make sure isIE reflects the desired version.
           //document.documentMode of 5 means quirks mode.

           /** @namespace document.documentMode */
           if (d.isIE >= 8 && document.documentMode != 5) {
               d.isIE = document.documentMode;
           }
       }
    };
}


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
/*we cannot privatize with a global function hence we store the values away for the init part*/
(function() {
    var target = window || document.body;
    var impl = "myfaces._impl.";
    var params = {_PFX_UTIL: impl + "_util.",
            _PFX_CORE:impl + "core.",
            _PFX_XHR: impl + "xhrCore.",
            _PFX_I18N: impl + "i18n."};
    if ('undefined' != typeof target.myfaces) {
        //some mobile browsers do not have a window object
        var _RT = myfaces._impl.core._Runtime;
        params._MF_CLS = _RT.extendClass;
        params._MF_SINGLTN = _RT.singletonExtendClass;
    } else {
        params._MF_CLS = false;
        params._MF_SINGLTN = false;
        target.myfaces = {};
    }
    target.myfaces._implTemp = {};
    for (var key in params) {
            target.myfaces._implTemp[key] = target[key];
            target[key] = params[key];
    }
})();

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
 * @memberOf myfaces._impl
 * @namespace
 * @name i18n
 */

/**
 * System messages base version    <p />
 * (note the base version is basically the en_US) version
 * of all messages
 *
 * @class
 * @name Messages
 * @memberOf myfaces._impl.i18n
 */
_MF_CLS && _MF_CLS(_PFX_I18N+"Messages", Object,
/**
 * @lends myfaces._impl.i18n.Messages.prototype
 */
{

    MSG_TEST:               "Testmessage",

    /*Messages*/
    /** @constant */
    MSG_DEV_MODE:           "Note, this message is only sent, because project stage is development and no " +
                            "other error listeners are registered.",
    /** @constant */
    MSG_AFFECTED_CLASS:     "Affected Class:",
    /** @constant */
    MSG_AFFECTED_METHOD:    "Affected Method:",
    /** @constant */
    MSG_ERROR_NAME:         "Error Name:",
    /** @constant */
    MSG_ERROR_MESSAGE:      "Error Message:",
    /** @constant */
    MSG_SERVER_ERROR_NAME:  "Server Error Name:",

    /** @constant */
    MSG_ERROR_DESC:         "Error Description:",
    /** @constant */
    MSG_ERROR_NO:           "Error Number:",
    /** @constant */
    MSG_ERROR_LINENO:       "Error Line Number:",

    /*Errors and messages*/
    /** @constant */
    ERR_FORM:               "Sourceform could not be determined, either because element is not attached to a form or we have multiple forms with named elements of the same identifier or name, stopping the ajax processing",
    /** @constant */
    ERR_VIEWSTATE:          "jsf.viewState: param value not of type form!",
    /** @constant */
    ERR_TRANSPORT:          "Transport type {0} does not exist",
    /** @constant */
    ERR_EVT_PASS:           "an event must be passed down (either a an event object null or undefined) ",
    /** @constant */
    ERR_CONSTRUCT:          "Parts of the response couldn't be retrieved when constructing the event data: {0} ",
    /** @constant */
    ERR_MALFORMEDXML:       "The server response could not be parsed, the server has returned with a response which is not xml !",
    /** @constant */
    ERR_SOURCE_FUNC:        "source cannot be a function (probably source and event were not defined or set to null",
    /** @constant */
    ERR_EV_OR_UNKNOWN:      "An event object or unknown must be passed as second parameter",
    /** @constant */
    ERR_SOURCE_NOSTR:       "source cannot be a string",
    /** @constant */
    ERR_SOURCE_DEF_NULL:    "source must be defined or null",

    //_Lang.js
    /** @constant */
    ERR_MUST_STRING:        "{0}: {1} namespace must be of type String",
    /** @constant */
    ERR_REF_OR_ID:          "{0}: {1} a reference node or identifier must be provided",
    /** @constant */
    ERR_PARAM_GENERIC:      "{0}: parameter {1} must be of type {2}",
    /** @constant */
    ERR_PARAM_STR:          "{0}: {1} param must be of type string",
    /** @constant */
    ERR_PARAM_STR_RE:       "{0}: {1} param must be of type string or a regular expression",
    /** @constant */
    ERR_PARAM_MIXMAPS:      "{0}: both a source as well as a destination map must be provided",
    /** @constant */
    ERR_MUST_BE_PROVIDED:   "{0}: an {1} and a {2} must be provided",
    /** @constant */
    ERR_MUST_BE_PROVIDED1:  "{0}: {1} must be set",

    /** @constant */
    ERR_REPLACE_EL:         "replaceElements called while evalNodes is not an array",

    /** @constant */
    ERR_EMPTY_RESPONSE:     "{0}: The response cannot be null or empty!",
    /** @constant */
    ERR_ITEM_ID_NOTFOUND:   "{0}: item with identifier {1} could not be found",
    /** @constant */
    ERR_PPR_IDREQ:          "{0}: Error in PPR Insert, id must be present",
    /** @constant */
    ERR_PPR_INSERTBEFID:    "{0}: Error in PPR Insert, before id or after id must be present",
    /** @constant */
    ERR_PPR_INSERTBEFID_1:  "{0}: Error in PPR Insert, before  node of id {1} does not exist in document",
    /** @constant */
    ERR_PPR_INSERTBEFID_2:  "{0}: Error in PPR Insert, after  node of id {1} does not exist in document",

    /** @constant */
    ERR_PPR_DELID:          "{0}: Error in delete, id not in xml markup",
    /** @constant */
    ERR_PPR_UNKNOWNCID:     "{0}:  Unknown Html-Component-ID: {1}",

    /** @constant */
    ERR_NO_VIEWROOTATTR:    "{0}: Changing of ViewRoot attributes is not supported",
    /** @constant */
    ERR_NO_HEADATTR:        "{0}: Changing of Head attributes is not supported",
    /** @constant */
    ERR_RED_URL:            "{0}: Redirect without url",

    /** @constant */
    ERR_REQ_FAILED_UNKNOWN: "Request failed with unknown status",

    /** @constant */
    ERR_REQU_FAILED: "Request failed with status {0} and reason {1}",

    /** @constant */
    UNKNOWN: "UNKNOWN",

    ERR_NO_MULTIPART_FORM: "The form with the id {0} has an input file element, but is not a multipart form"
});


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
 * System messages german version
 * (note the base version is basically the en_US) version
 * of all messages
 * <p />
 * We use inheritance to overide the default messages with our
 * german one, variants can derive from the german one (like
 * suisse which do not have the emphasized s)
 * <p />
 * By using inheritance we can be sure that we fall back to the default one
 * automatically and that our variants only have to override the parts
 * which have changed from the baseline
 *
 * @class
 * @name Messages_de
 * @extends myfaces._impl.i18n.Messages
 * @memberOf myfaces._impl.i18n
 */

_MF_CLS && _MF_CLS(_PFX_I18N + "Messages_de", myfaces._impl.i18n.Messages,
        /** @lends myfaces._impl.i18n.Messages_de.prototype */
        {

            MSG_TEST:               "Testnachricht",

            /*Messages*/
            MSG_DEV_MODE:           "Sie sehen diese Nachricht, da sie sich gerade im Entwicklungsmodus befinden " +
                    "und sie keine Fehlerbehandlungsfunktionen registriert haben.",

            MSG_AFFECTED_CLASS:     "Klasse:",
            MSG_AFFECTED_METHOD:    "Methode:",

            MSG_ERROR_NAME:         "Fehler Name:",
            MSG_ERROR_MESSAGE:      "Nachricht:",
            MSG_SERVER_ERROR_NAME:  "Server Fehler Name:",

            MSG_ERROR_DESC:         "Fehlerbeschreibung:",
            MSG_ERROR_NO:           "Fehlernummer:",
            MSG_ERROR_LINENO:       "Zeilennummer:",

            /*Errors and messages*/
            ERR_FORM:                "Das Quellformular konnte nicht gefunden werden. " +
                    "Mögliche Gründe: Sie haben entweder kein formular definiert, oder es kommen mehrere Formulare vor, " +
                    "die alle das auslösende Element mit demselben Namen besitzen. " +
                    "Die Weitere Ajax Ausführung wird gestoppt.",

            ERR_VIEWSTATE:          "jsf.viewState: der Parameter ist not vom Typ form!",

            ERR_TRANSPORT:          "Transport typ {0} existiert nicht",
            ERR_EVT_PASS:           "Ein Event Objekt muss übergeben werden (entweder ein event Objekt oder null oder undefined)",
            ERR_CONSTRUCT:          "Teile des response konnten nicht ermittelt werden während die Event Daten bearbeitet wurden: {0} ",
            ERR_MALFORMEDXML:       "Es gab zwar eine Antwort des Servers, jedoch war diese nicht im erwarteten XML Format. Der Server hat kein valides XML gesendet! Bearbeitung abgebrochen.",
            ERR_SOURCE_FUNC:        "source darf keine Funktion sein",
            ERR_EV_OR_UNKNOWN:      "Ein Ereignis Objekt oder UNKNOWN muss als 2. Parameter übergeben werden",
            ERR_SOURCE_NOSTR:       "source darf kein String sein",
            ERR_SOURCE_DEF_NULL:    "source muss entweder definiert oder null sein",

            //_Lang.js
            ERR_MUST_STRING:        "{0}: {1} namespace muss vom Typ String sein",
            ERR_REF_OR_ID:          "{0}: {1} Ein Referenzknoten oder id muss übergeben werden",
            ERR_PARAM_GENERIC:      "{0}: Paramter {1} muss vom Typ {2} sein",
            ERR_PARAM_STR:          "{0}: Parameter {1} muss vom Typ String sein",
            ERR_PARAM_STR_RE:       "{0}: Parameter {1} muss entweder ein String oder ein Regulärer Ausdruck sein",
            ERR_PARAM_MIXMAPS:      "{0}: both a source as well as a destination map must be provided",
            ERR_MUST_BE_PROVIDED:   "{0}: ein {1} und ein {2} müssen übergeben werden",
            ERR_MUST_BE_PROVIDED1:  "{0}: {1} muss gesetzt sein",

            ERR_REPLACE_EL:         "replaceElements aufgerufen während evalNodes nicht ein Array ist",
            ERR_EMPTY_RESPONSE:     "{0}: Die Antwort darf nicht null oder leer sein!",
            ERR_ITEM_ID_NOTFOUND:   "{0}: Element mit ID {1} konnte nicht gefunden werden",
            ERR_PPR_IDREQ:          "{0}: Fehler im PPR Insert, ID muss gesetzt sein",
            ERR_PPR_INSERTBEFID:    "{0}: Fehler im PPR Insert, before ID oder after ID muss gesetzt sein",
            ERR_PPR_INSERTBEFID_1:  "{0}: Fehler im PPR Insert, before  Knoten mit ID {1} Existiert nicht",
            ERR_PPR_INSERTBEFID_2:  "{0}: Fehler im PPR Insert, after  Knoten mit ID {1} Existiert nicht",

            ERR_PPR_DELID:          "{0}: Fehler im PPR delete, id ist nicht im xml Markup vorhanden",
            ERR_PPR_UNKNOWNCID:     "{0}: Unbekannte Html-Komponenten-ID: {1}",
            ERR_NO_VIEWROOTATTR:    "{0}: Änderung von ViewRoot Attributen ist nicht erlaubt",
            ERR_NO_HEADATTR:        "{0}: Änderung von Head Attributen ist nicht erlaubt",
            ERR_RED_URL:            "{0}: Redirect ohne URL",

            ERR_REQ_FAILED_UNKNOWN: "Anfrage mit unbekanntem Status fehlgeschlagen",
            ERR_REQU_FAILED: "Anfrage mit Status {0} and Ursache {1} fehlgeschlagen",
            UNKNOWN: "Unbekannt",
            ERR_NO_MULTIPART_FORM: "Das Form Element mit der ID {0} hat ein Fileupload Feld aber ist kein Multipart Form"

        });


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
 * System messages dutch version
 *
 * @class
 * @name Messages_nl
 * @extends myfaces._impl.i18n.Messages
 * @memberOf myfaces._impl.i18n
 */
_MF_CLS && _MF_CLS(_PFX_I18N + "Messages_nl", myfaces._impl.i18n.Messages,
        /** @lends myfaces._impl.i18n.Messages_nl.prototype */
        {

            MSG_TEST:               "Testbericht",

            /*Messages*/
            MSG_DEV_MODE:           "Opmerking, dit bericht is enkel gestuurd omdat het project stadium develoment is en er geen " +
                    "andere listeners zijn geconfigureerd.",
            MSG_AFFECTED_CLASS:     "Betrokken Klasse:",
            MSG_AFFECTED_METHOD:    "Betrokken Methode:",

            MSG_ERROR_NAME:         "Naam foutbericht:",
            MSG_ERROR_MESSAGE:      "Naam foutbericht:",

            MSG_ERROR_DESC:         "Omschrijving fout:",
            MSG_ERROR_NO:           "Fout nummer:",
            MSG_ERROR_LINENO:       "Fout lijn nummer:",

            /*Errors and messages*/
            ERR_FORM:               "De doel form kon niet bepaald worden, ofwel omdat het element niet tot een form behoort, ofwel omdat er verschillende forms zijn met 'named element' met dezelfde identifier of naam, ajax verwerking is gestopt.",
            ERR_VIEWSTATE:          "jsf.viewState: param waarde is niet van het type form!",
            ERR_TRANSPORT:          "Transport type {0} bestaat niet",
            ERR_EVT_PASS:           "een event moet opgegegevn worden (ofwel een event object null of undefined) ",
            ERR_CONSTRUCT:          "Delen van het antwoord konden niet opgehaald worden bij het aanmaken van de event data: {0} ",
            ERR_MALFORMEDXML:       "Het antwoordt van de server kon niet ontleed worden, de server heeft een antwoord gegeven welke geen xml bevat!",
            ERR_SOURCE_FUNC:        "source kan geen functie zijn (waarschijnlijk zijn source en event niet gedefinieerd of kregen de waarde null)",
            ERR_EV_OR_UNKNOWN:      "Een event object of 'unknown' moet gespecifieerd worden als tweede parameter",
            ERR_SOURCE_NOSTR:       "source kan geen string zijn",
            ERR_SOURCE_DEF_NULL:    "source moet gedefinieerd zijn of null bevatten",

            //_Lang.js
            ERR_MUST_STRING:        "{0}: {1} namespace moet van het type String zijn",
            ERR_REF_OR_ID:          "{0}: {1} een referentie node of identifier moet opgegeven worden",
            ERR_PARAM_GENERIC:      "{0}: parameter {1} moet van het type {2} zijn",
            ERR_PARAM_STR:          "{0}: {1} parameter moet van het type string zijn",
            ERR_PARAM_STR_RE:       "{0}: {1} parameter moet van het type string zijn of een reguliere expressie",
            ERR_PARAM_MIXMAPS:      "{0}: zowel source als destination map moeten opgegeven zijn",
            ERR_MUST_BE_PROVIDED:   "{0}: een {1} en een {2} moeten opgegeven worden",
            ERR_MUST_BE_PROVIDED1:  "{0}: {1} moet gezet zijn",

            ERR_REPLACE_EL:         "replaceElements opgeroepen maar evalNodes is geen array",
            ERR_EMPTY_RESPONSE:     "{0}: Het antwoord kan geen null of leeg zijn!",
            ERR_ITEM_ID_NOTFOUND:   "{0}: item met identifier {1} kan niet gevonden worden",
            ERR_PPR_IDREQ:          "{0}: Fout in PPR Insert, id moet bestaan",
            ERR_PPR_INSERTBEFID:    "{0}: Fout in PPR Insert, before id of after id moet bestaan",
            ERR_PPR_INSERTBEFID_1:  "{0}: Fout in PPR Insert, before node van id {1} bestaat niet in het document",
            ERR_PPR_INSERTBEFID_2:  "{0}: Fout in PPR Insert, after node van id {1} bestaat niet in het document",

            ERR_PPR_DELID:          "{0}: Fout in delete, id is niet in de xml markup",
            ERR_PPR_UNKNOWNCID:     "{0}: Onbekende Html-Component-ID: {1}",
            ERR_NO_VIEWROOTATTR:    "{0}: Wijzigen van ViewRoot attributen is niet ondersteund",
            ERR_NO_HEADATTR:        "{0}: Wijzigen van Head attributen is niet ondersteund",
            ERR_RED_URL:            "{0}: Redirect zonder url",

            ERR_REQ_FAILED_UNKNOWN: "Request mislukt met onbekende status",
            ERR_REQU_FAILED:        "Request mislukt met status {0} en reden {1}",
            UNKNOWN:                "ONBEKEND"

        });

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
 * System messages spanish version version
 * (note the base version is basically the en_US) version
 * of all messages
 *
 * @class
 * @name Messages_es
 * @extends myfaces._impl.i18n.Messages
 * @memberOf myfaces._impl.i18n
 */

_MF_CLS && _MF_CLS(_PFX_I18N + "Messages_es", myfaces._impl.i18n.Messages,
        /** @lends myfaces._impl.i18n.Messages_es.prototype */
        {


            MSG_TEST:               "Mensajeprueba",

            /*Messages*/
            MSG_DEV_MODE:           "Aviso. Este mensaje solo se envia porque el 'Project Stage' es 'Development' y no hay otros 'listeners' de errores registrados.",
            MSG_AFFECTED_CLASS:     "Clase Afectada:",
            MSG_AFFECTED_METHOD:    "M�todo Afectado:",

            MSG_ERROR_NAME:         "Nombre del Error:",
            MSG_ERROR_MESSAGE:      "Mensaje del Error:",
            MSG_SERVER_ERROR_NAME:  "Mensaje de error de servidor:",

            MSG_ERROR_DESC:         "Descripci�n del Error:",
            MSG_ERROR_NO:           "N�mero de Error:",
            MSG_ERROR_LINENO:       "N�mero de L�nea del Error:",

            /*Errors and messages*/
            ERR_FORM:               "El formulario de origen no ha podido ser determinado, debido a que el elemento no forma parte de un formulario o hay diversos formularios con elementos usando el mismo nombre o identificador. Parando el procesamiento de Ajax.",
            ERR_VIEWSTATE:          "jsf.viewState: el valor del par�metro no es de tipo 'form'!",
            ERR_TRANSPORT:          "El tipo de transporte {0} no existe",
            ERR_EVT_PASS:           "un evento debe ser transmitido (sea null o no definido)",
            ERR_CONSTRUCT:          "Partes de la respuesta no pudieron ser recuperadas cuando construyendo los datos del evento: {0} ",
            ERR_MALFORMEDXML:       "La respuesta del servidor no ha podido ser interpretada. El servidor ha devuelto una respuesta que no es xml !",
            ERR_SOURCE_FUNC:        "el origen no puede ser una funci�n (probablemente 'source' y evento no han sido definidos o son 'null'",
            ERR_EV_OR_UNKNOWN:      "Un objeto de tipo evento o desconocido debe ser pasado como segundo par�metro",
            ERR_SOURCE_NOSTR:       "el origen no puede ser 'string'",
            ERR_SOURCE_DEF_NULL:    "el origen debe haber sido definido o ser 'null'",

            //_Lang.js
            ERR_MUST_STRING:        "{0}: {1} namespace debe ser de tipo String",
            ERR_REF_OR_ID:          "{0}: {1} una referencia a un nodo o identificador tiene que ser pasada",
            ERR_PARAM_GENERIC:      "{0}: el par�metro {1} tiene que ser de tipo {2}",
            ERR_PARAM_STR:          "{0}: el par�metro {1} tiene que ser de tipo string",
            ERR_PARAM_STR_RE:       "{0}: el par�metro {1} tiene que ser de tipo string o una expresi�n regular",
            ERR_PARAM_MIXMAPS:      "{0}: han de ser pasados tanto un origen como un destino",
            ERR_MUST_BE_PROVIDED:   "{0}: {1} y {2} deben ser pasados",
            ERR_MUST_BE_PROVIDED1:  "{0}: {1} debe estar definido",

            ERR_REPLACE_EL:         "replaceElements invocado mientras que evalNodes no es un an array",
            ERR_EMPTY_RESPONSE:     "{0}: �La respuesta no puede ser de tipo 'null' o vac�a!",
            ERR_ITEM_ID_NOTFOUND:   "{0}: el elemento con identificador {1} no ha sido encontrado",
            ERR_PPR_IDREQ:          "{0}: Error en PPR Insert, 'id' debe estar presente",
            ERR_PPR_INSERTBEFID:    "{0}: Error in PPR Insert, antes de 'id' o despu�s de 'id' deben estar presentes",
            ERR_PPR_INSERTBEFID_1:  "{0}: Error in PPR Insert, antes de nodo con id {1} no existe en el documento",
            ERR_PPR_INSERTBEFID_2:  "{0}: Error in PPR Insert, despu�s de nodo con id {1} no existe en el documento",

            ERR_PPR_DELID:          "{0}: Error durante borrado, id no presente en xml",
            ERR_PPR_UNKNOWNCID:     "{0}:  Desconocido Html-Component-ID: {1}",
            ERR_NO_VIEWROOTATTR:    "{0}: El cambio de atributos de ViewRoot attributes no es posible",
            ERR_NO_HEADATTR:        "{0}: El cambio de los atributos de Head attributes no es posible",
            ERR_RED_URL:            "{0}: Redirecci�n sin url",

            ERR_REQ_FAILED_UNKNOWN: "La petici�n ha fallado con estado desconocido",
            ERR_REQU_FAILED:        "La petici�n ha fallado con estado {0} y raz�n {1}",
            UNKNOWN:                "DESCONOCIDO"

        });



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
     * System messages french version version
     * (note the base version is basically the en) version
     * of all messages
     *
     * @class
     * @name Messages_fr
     * @extends myfaces._impl.i18n.Messages
     * @memberOf myfaces._impl.i18n
     */
_MF_CLS && _MF_CLS(_PFX_I18N + "Messages_fr", myfaces._impl.i18n.Messages,
            /** @lends myfaces._impl.i18n.Messages_fr.prototype */
            {
                MSG_TEST:               "MessageTest FR",

                /*Messages*/
                MSG_DEV_MODE:           "Note : ce message n'est envoyé que parce que le projet est au stade de développement et " +
                        "qu'aucun autre listener d'erreurs n'est enregistré.",
                MSG_AFFECTED_CLASS:     "Classe affectée : ",
                MSG_AFFECTED_METHOD:    "Méthode affectée : ",

                MSG_ERROR_NAME:         "Nom de l'erreur : ",
                MSG_ERROR_MESSAGE:      "Nom de l'erreur : ",

                MSG_ERROR_DESC:         "Description de l'erreur : ",
                MSG_ERROR_NO:           "Numéro de l'erreur : ",
                MSG_ERROR_LINENO:       "Erreur à la ligne : ",

                /*Errors and messages*/
                ERR_FORM:               "Le formulaire source n'a pas pu être déterminé, soit parce que l'élément n'est rattaché à aucun formulaire, soit parce qu'ils y a plusieurs formulaires contenant des éléments avec le même nom ou identifiant. Arrêt du traitement AJAX",
                ERR_VIEWSTATE:          "jsf.viewState: La valeur de 'param' n'est pas de type 'form' !",
                ERR_TRANSPORT:          "Le type de tansport {0} n'existe pas",
                ERR_EVT_PASS:           "Un évènement doit être transmis (soit un objet évènement, soit null ou undefined) ",
                ERR_CONSTRUCT:          "Des éléments de la réponse n'ont pu être récupérés lors de la construction des données de l'évènement : {0} ",
                ERR_MALFORMEDXML:       "La réponse du serveur n'a pas pu être analysée : le serveur n'a pas renvoyé une réponse en xml !",
                ERR_SOURCE_FUNC:        "La source ne peut pas être une fonction (Il est probable que 'source' et 'event' n'ont pas été définis ou mis à null",
                ERR_EV_OR_UNKNOWN:      "Le second paramètre doit être un objet évènement ou 'unknown' ",
                ERR_SOURCE_NOSTR:       "La source ne peut pas être de type String",
                ERR_SOURCE_DEF_NULL:    "La source doit être définie ou égale à null",

                //_Lang.js
                ERR_MUST_STRING:        "{0}: Le namespace {1} doit être de type String",
                ERR_REF_OR_ID:          "{0}: {1} un noeud de référence ou un identifiant doit être passé",
                ERR_PARAM_GENERIC:      "{0}: Le paramètre {1} doit être de type {2}",
                ERR_PARAM_STR:          "{0}: Le paramètre {1} doit être de type String",
                ERR_PARAM_STR_RE:       "{0}: Le paramètre {1} doit être de type String ou être une expression régulière",
                ERR_PARAM_MIXMAPS:      "{0}: Un Map de source et un Map de destination doivent être passés",
                ERR_MUST_BE_PROVIDED:   "{0}: un(e) {1} et un(e) {2} doivent être passés",
                ERR_MUST_BE_PROVIDED1:  "{0}: {1} doit être défini",

                ERR_REPLACE_EL:         "replaceElements a été appelé alors que evalNodes n'est pas un tableau",
                ERR_EMPTY_RESPONSE:     "{0}: La réponse ne peut pas être nulle ou vide !",
                ERR_ITEM_ID_NOTFOUND:   "{0}: l'élément portant l'identifiant {1} n'a pas pu être trouvé",
                ERR_PPR_IDREQ:          "{0}: Erreur lors de l'insertion PPR, l'id doit être présent",
                ERR_PPR_INSERTBEFID:    "{0}: Erreur lors de l'insertion PPR, 'before id' ou 'after id' doivent être présents",
                ERR_PPR_INSERTBEFID_1:  "{0}: Erreur lors de l'insertion PPR, le noeud before de l'id {1} n'existe pas dans le document",
                ERR_PPR_INSERTBEFID_2:  "{0}: Erreur lors de l'insertion PPR, le noeud after  de l'id {1} n'existe pas dans le document",

                ERR_PPR_DELID:          "{0}: Erreur lors de la suppression, l'id n'est pas présent dans le xml",
                ERR_PPR_UNKNOWNCID:     "{0}:  Html-Component-ID inconnu : {1}",
                ERR_NO_VIEWROOTATTR:    "{0}: Le changement d'attributs dans ViewRoot n'est pas supporté",
                ERR_NO_HEADATTR:        "{0}: Le changement d'attributs dans Head n'est pas supporté",
                ERR_RED_URL:            "{0}: Redirection sans url"
            });

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
 * System messages italian version version
 * (note the base version is basically the en_US) version
 * of all messages
 *
 * @class
 * @name Messages_it
 * @extends myfaces._impl.i18n.Messages
 * @memberOf myfaces._impl.i18n
 */
_MF_CLS && _MF_CLS(_PFX_I18N + "Messages_it", myfaces._impl.i18n.Messages,
        /** @lends myfaces._impl.i18n.Messages_it.prototype */
        {
            /*Messages*/
            MSG_DEV_MODE:           "Questo messaggio � stato inviato esclusivamente perch� il progetto � in development stage e nessun altro listener � stato registrato.",
            MSG_AFFECTED_CLASS:     "Classi coinvolte:",
            MSG_AFFECTED_METHOD:    "Metodi coinvolti:",

            MSG_ERROR_NAME:         "Nome dell'errore:",
            MSG_ERROR_MESSAGE:      "Nome dell'errore:",

            MSG_ERROR_DESC:         "Descrizione dell'errore:",
            MSG_ERROR_NO:           "Numero errore:",
            MSG_ERROR_LINENO:       "Numero di riga dell'errore:",

            /*Errors and messages*/
            ERR_FORM:               "Il Sourceform non puo' essere determinato a causa di una delle seguenti ragioni: l'elemento non e' agganciato ad un form oppure sono presenti pi� form con elementi con lo stesso nome, il che blocca l'elaborazione ajax",
            ERR_VIEWSTATE:          "jsf.viewState: il valore del parametro non � di tipo form!",
            ERR_TRANSPORT:          "Il transport type {0} non esiste",
            ERR_EVT_PASS:           "� necessario passare un evento (sono accettati anche gli event object null oppure undefined) ",
            ERR_CONSTRUCT:          "Durante la costruzione dell' event data: {0} non � stato possibile acquisire alcune parti della response ",
            ERR_MALFORMEDXML:       "Il formato della risposta del server non era xml, non � stato quindi possibile effettuarne il parsing!",
            ERR_SOURCE_FUNC:        "source non puo' essere una funzione (probabilmente source and event non erano stati definiti o sono null",
            ERR_EV_OR_UNKNOWN:      "Come secondo parametro bisogna passare un event object oppure unknown",
            ERR_SOURCE_NOSTR:       "source non pu� essere una stringa di testo",
            ERR_SOURCE_DEF_NULL:    "source deve essere definito oppure  null",

            //_Lang.js
            ERR_MUST_STRING:        "{0}: {1} namespace deve essere di tipo String",
            ERR_REF_OR_ID:          "{0}: {1} un reference node oppure un identificatore deve essere fornito",
            ERR_PARAM_GENERIC:      "{0}: il parametro {1} deve essere di tipo {2}",
            ERR_PARAM_STR:          "{0}: {1} parametro deve essere di tipo String",
            ERR_PARAM_STR_RE:       "{0}: {1} parametro deve essere di tipo String oppure una regular expression",
            ERR_PARAM_MIXMAPS:      "{0}: � necessario specificare sia  source che destination map",
            ERR_MUST_BE_PROVIDED:   "{0}: � necessario specificare sia {1} che {2} ",
            ERR_MUST_BE_PROVIDED1:  "{0}: {1} deve essere settato",

            ERR_REPLACE_EL:         "replaceElements chiamato metre evalNodes non � un array",
            ERR_EMPTY_RESPONSE:     "{0}: La response non puo' essere nulla o vuota!",
            ERR_ITEM_ID_NOTFOUND:   "{0}: non � stato trovato alcun item con identificativo {1}",
            ERR_PPR_IDREQ:          "{0}: Errore durante la PPR Insert, l' id deve essere specificato",
            ERR_PPR_INSERTBEFID:    "{0}: Errore durante la PPR Insert, before id o after id deve essere specificato",
            ERR_PPR_INSERTBEFID_1:  "{0}: Errore durante la PPR Insert, before node of id {1} non esiste nel document",
            ERR_PPR_INSERTBEFID_2:  "{0}: Errore durante la PPR Insert, after  node of id {1} non esiste nel in document",

            ERR_PPR_DELID:          "{0}: Errore durante la delete, l'id non e' nella forma di un markup xml",
            ERR_PPR_UNKNOWNCID:     "{0}:   Html-Component-ID: {1} sconosciuto",
            ERR_NO_VIEWROOTATTR:    "{0}: La modifica degli attributi del ViewRoot non � supportata",
            ERR_NO_HEADATTR:        "{0}: La modifica degli attributi di Head non � supportata",
            ERR_RED_URL:            "{0}: Redirect senza url"
        });


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
if (_MF_CLS) {
/**
 * System messages kyrillic/russian version
 *
 * @class
 * @name Messages_nl
 * @extends myfaces._impl.i18n.Messages
 * @memberOf myfaces._impl.i18n
 */
_MF_CLS && _MF_CLS(_PFX_I18N + "Messages_ru", myfaces._impl.i18n.Messages,
        /** myfaces._impl.i18n.Messages_ru.prototype */
        {

            MSG_TEST:               "ТестовоеСообщение",

            /*Messages*/
            MSG_DEV_MODE:           "Это сообщение выдано, потому что 'project stage' было присоено значение 'development', и никаких" +
                    "других error listeners зарегистрировано не было.",
            MSG_AFFECTED_CLASS:     "Задействованный класс:",
            MSG_AFFECTED_METHOD:    "Задействованный метод:",

            MSG_ERROR_NAME:         "Имя ошибки:",
            MSG_ERROR_MESSAGE:      "Имя ошибки:",

            MSG_ERROR_DESC:         "Описание ошибки:",
            MSG_ERROR_NO:           "Номер ошибки:",
            MSG_ERROR_LINENO:       "Номер строки ошибки:",

            /*Errors and messages*/
            ERR_FORM:               "Sourceform не найдена, потому что элемент не находится внутри <form>, либо были найдены элементы <form> с рдинаковым именем или идентификатором. Обработка ajax остановлена",
            ERR_VIEWSTATE:          "jsf.viewState: Параметру присвоено значение, не являющееся элементом <form>!",
            ERR_TRANSPORT:          "Несуществующий тип транспорта {0}",
            ERR_EVT_PASS:           "Параметр event необходим, и не может быть null или undefined",
            ERR_CONSTRUCT:          "Часть ответа не удалось прочитать при создании данных события: {0} ",
            ERR_MALFORMEDXML:       "Ответ сервера не может быть обработан, он не в формате xml !",
            ERR_SOURCE_FUNC:        "source не может быть функцией (возможно, для source и event не были даны значения",
            ERR_EV_OR_UNKNOWN:      "Объект event или unknown должен быть всторым параметром",
            ERR_SOURCE_NOSTR:       "source не может быть типа string",
            ERR_SOURCE_DEF_NULL:    "source должно быть присвоено значение или null",

            //_Lang.js
            ERR_MUST_STRING:        "{0}: {1} namespace должно быть типа String",
            ERR_REF_OR_ID:          "{0}: {1} a Ссылочный узел (reference node) или идентификатор необходимы",
            ERR_PARAM_GENERIC:      "{0}: параметр {1} должен быть типа {2}",
            ERR_PARAM_STR:          "{0}: {1} параметр должен быть типа string",
            ERR_PARAM_STR_RE:       "{0}: {1} параметр должен быть типа string string или regular expression",
            ERR_PARAM_MIXMAPS:      "{0}: source b destination map необходимы",
            ERR_MUST_BE_PROVIDED:   "{0}: {1} и {2} необходимы",
            ERR_MUST_BE_PROVIDED1:  "{0}: {1} должно быть присвоено значение",

            ERR_REPLACE_EL:         "replaceElements вызвана, с evalNodes, не являющимся массивом",
            ERR_EMPTY_RESPONSE:     "{0}: Ответ не может бвть null или пустым!",
            ERR_ITEM_ID_NOTFOUND:   "{0}: Элемент с идентификатором {1} не найден",
            ERR_PPR_IDREQ:          "{0}: Ошибка в PPR Insert, id необходим",
            ERR_PPR_INSERTBEFID:    "{0}: Ошибка в PPR Insert, before id или after id необходимы",
            ERR_PPR_INSERTBEFID_1:  "{0}: Ошибка в PPR Insert, before node c id {1} не найден в документе",
            ERR_PPR_INSERTBEFID_2:  "{0}: Ошибка в PPR Insert, after node с id {1} не найден в документе",

            ERR_PPR_DELID:          "{0}: Ошибка в удалении, id не найден в xml документе",
            ERR_PPR_UNKNOWNCID:     "{0}: Неопознанный Html-Component-ID: {1}",
            ERR_NO_VIEWROOTATTR:    "{0}: Изменение атрибутов ViewRoot не предусмотрено",
            ERR_NO_HEADATTR:        "{0}: Изменение атрибутов Head не предусмотрено",
            ERR_RED_URL:            "{0}: Перенаправление (Redirect) без url"

        });
}
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
if (_MF_CLS) {
    /**
     * System messages Simplified Chinese version
     *
     * @class
     * @name Messages_zh_CN
     * @extends myfaces._impl.i18n.Messages
     * @memberOf myfaces._impl.i18n
     */
_MF_CLS && _MF_CLS(_PFX_I18N+"Messages_zh_CN", myfaces._impl.i18n.Messages,
    /** @lends myfaces._impl.i18n.Messages_zh_CN.prototype */
    {

        MSG_TEST:               "测试信息",

        /*Messages*/
        /** @constant */
        MSG_DEV_MODE:           "请注意，此信息只在项目发展阶段，及没有注册错误监听器而发放。",
        /** @constant */
        MSG_AFFECTED_CLASS:     "受影响类别：",
        /** @constant */
        MSG_AFFECTED_METHOD:    "受影响方法：",
        /** @constant */
        MSG_ERROR_NAME:         "错误名称：",
        /** @constant */
        MSG_ERROR_MESSAGE:      "错误信息：",
        /** @constant */
        MSG_SERVER_ERROR_NAME:  "伺服器错误名称：",
        /** @constant */
        MSG_ERROR_DESC:         "错误说明：",
        /** @constant */
        MSG_ERROR_NO:           "错误号码：",
        /** @constant */
        MSG_ERROR_LINENO:       "错误行号：",

        /*Errors and messages*/
        /** @constant */
        ERR_FORM:               "不能判定源表单，要么没有连接元件到表单，要么有多个相同标识符或名称的表单，AJAX处理停止运作",
        /** @constant */
        ERR_VIEWSTATE:          "jsf.viewState：参数值不是表单类型！",
        /** @constant */
        ERR_TRANSPORT:          "不存在{0}传输类型",
        /** @constant */
        ERR_EVT_PASS:           "必须放弃事件（可能事件物件为空或未定义）",
        /** @constant */
        ERR_CONSTRUCT:          "构建事件数据时部分回应不能取得，原因是：{0}",
        /** @constant */
        ERR_MALFORMEDXML:       "无法解析伺服器的回应，伺服器返回的回应不是XML！",
        /** @constant */
        ERR_SOURCE_FUNC:        "来源不能是一个函数（可能来源和事件没有定义或设定为空）",
        /** @constant */
        ERR_EV_OR_UNKNOWN:      "事件物件或不明必须作为第二个参数传递",
        /** @constant */
        ERR_SOURCE_NOSTR:       "来源不能是字串",
        /** @constant */
        ERR_SOURCE_DEF_NULL:    "来源必须定义或为空",

        //_Lang.js
        /** @constant */
        ERR_MUST_STRING:        "{0}：{1} 名称空间必须是字串类型",
        /** @constant */
        ERR_REF_OR_ID:          "{0}：{1} 必须提供参考节点或标识符",
        /** @constant */
        ERR_PARAM_GENERIC:      "{0}：{1} 参数必须是 {2} 类型",
        /** @constant */
        ERR_PARAM_STR:          "{0}：{1} 参数必须是字串类型",
        /** @constant */
        ERR_PARAM_STR_RE:       "{0}：{1} 参数必须是字串类型或正规表达式",
        /** @constant */
        ERR_PARAM_MIXMAPS:      "{0}：必须提供来源及目标映射",
        /** @constant */
        ERR_MUST_BE_PROVIDED:   "{0}：必须提供 {1} 及 {2}",
        /** @constant */
        ERR_MUST_BE_PROVIDED1:  "{0}：必须设定 {1}",

        /** @constant */
        ERR_REPLACE_EL:         "调用replaceElements函数时evalNodes变量不是阵列类型",

        /** @constant */
        ERR_EMPTY_RESPONSE:     "{0}：回应不能为空的！",
        /** @constant */
        ERR_ITEM_ID_NOTFOUND:   "{0}：找不到有 {1} 标识符的项目",
        /** @constant */
        ERR_PPR_IDREQ:          "{0}：局部页面渲染嵌入错误，标识符必须存在",
        /** @constant */
        ERR_PPR_INSERTBEFID:    "{0}：局部页面渲染嵌入错误，前或后标识符必须存在",
        /** @constant */
        ERR_PPR_INSERTBEFID_1:  "{0}：局部页面渲染嵌入错误，前节点的标识符 {1} 不在文件内",
        /** @constant */
        ERR_PPR_INSERTBEFID_2:  "{0}：局部页面渲染嵌入错误，后节点的标识符 {1} 不在文件内",

        /** @constant */
        ERR_PPR_DELID:          "{0}：删除错误，标识符不在XML标记中",
        /** @constant */
        ERR_PPR_UNKNOWNCID:     "{0}：不明的HTML组件标识符：{1}",

        /** @constant */
        ERR_NO_VIEWROOTATTR:    "{0}：不支援改变ViewRoot属性",
        /** @constant */
        ERR_NO_HEADATTR:        "{0}：不支援改变Head的属性",
        /** @constant */
        ERR_RED_URL:            "{0}：没有重导向网址",

        /** @constant */
        ERR_REQ_FAILED_UNKNOWN: "请求失败，状态不明",

        /** @constant */
        ERR_REQU_FAILED: "请求失败，状态是 {0} 和原因是 {1}",

        /** @constant */
        UNKNOWN: "不明"
    });
}

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
 * System messages Traditional Chinese (Hong Kong) version
 *
 * @class
 * @name Messages_zh_HK
 * @extends myfaces._impl.i18n.Messages
 * @memberOf myfaces._impl.i18n
 */
_MF_CLS && _MF_CLS(_PFX_I18N + "Messages_zh_HK", myfaces._impl.i18n.Messages,
        /** @lends myfaces._impl.i18n.Messages_zh_HK.prototype */
        {

            MSG_TEST:               "測試信息",

            /*Messages*/
            /** @constant */
            MSG_DEV_MODE:           "請注意，此信息只在項目發展階段，及沒有註冊錯誤監聽器而發放。",
            /** @constant */
            MSG_AFFECTED_CLASS:     "受影響類別：",
            /** @constant */
            MSG_AFFECTED_METHOD:    "受影響方法：",
            /** @constant */
            MSG_ERROR_NAME:         "錯誤名稱：",
            /** @constant */
            MSG_ERROR_MESSAGE:      "錯誤信息：",
            /** @constant */
            MSG_SERVER_ERROR_NAME:  "伺服器錯誤名稱：",
            /** @constant */
            MSG_ERROR_DESC:         "錯誤說明：",
            /** @constant */
            MSG_ERROR_NO:           "錯誤號碼：",
            /** @constant */
            MSG_ERROR_LINENO:       "錯誤行號：",

            /*Errors and messages*/
            /** @constant */
            ERR_FORM:               "不能判定源表單，要麼沒有連接元件到表單，要麼有多個相同標識符或名稱的表單，AJAX處理停止運作",
            /** @constant */
            ERR_VIEWSTATE:          "jsf.viewState：參數值不是表單類型！",
            /** @constant */
            ERR_TRANSPORT:          "不存在{0}傳輸類型",
            /** @constant */
            ERR_EVT_PASS:           "必須放棄事件（可能事件物件為空或未定義）",
            /** @constant */
            ERR_CONSTRUCT:          "構建事件數據時部分回應不能取得，原因是：{0}",
            /** @constant */
            ERR_MALFORMEDXML:       "無法解析伺服器的回應，伺服器返回的回應不是XML！",
            /** @constant */
            ERR_SOURCE_FUNC:        "來源不能是一個函數（可能來源和事件沒有定義或設定為空）",
            /** @constant */
            ERR_EV_OR_UNKNOWN:      "事件物件或不明必須作為第二個參數傳遞",
            /** @constant */
            ERR_SOURCE_NOSTR:       "來源不能是字串",
            /** @constant */
            ERR_SOURCE_DEF_NULL:    "來源必須定義或為空",

            //_Lang.js
            /** @constant */
            ERR_MUST_STRING:        "{0}：{1} 名稱空間必須是字串類型",
            /** @constant */
            ERR_REF_OR_ID:          "{0}：{1} 必須提供參考節點或標識符",
            /** @constant */
            ERR_PARAM_GENERIC:      "{0}：{1} 參數必須是 {2} 類型",
            /** @constant */
            ERR_PARAM_STR:          "{0}：{1} 參數必須是字串類型",
            /** @constant */
            ERR_PARAM_STR_RE:       "{0}：{1} 參數必須是字串類型或正規表達式",
            /** @constant */
            ERR_PARAM_MIXMAPS:      "{0}：必須提供來源及目標映射",
            /** @constant */
            ERR_MUST_BE_PROVIDED:   "{0}：必須提供 {1} 及 {2}",
            /** @constant */
            ERR_MUST_BE_PROVIDED1:  "{0}：必須設定 {1}",

            /** @constant */
            ERR_REPLACE_EL:         "調用replaceElements函數時evalNodes變量不是陣列類型",

            /** @constant */
            ERR_EMPTY_RESPONSE:     "{0}：回應不能為空的！",
            /** @constant */
            ERR_ITEM_ID_NOTFOUND:   "{0}：找不到有 {1} 標識符的項目",
            /** @constant */
            ERR_PPR_IDREQ:          "{0}：局部頁面渲染嵌入錯誤，標識符必須存在",
            /** @constant */
            ERR_PPR_INSERTBEFID:    "{0}：局部頁面渲染嵌入錯誤，前或後標識符必須存在",
            /** @constant */
            ERR_PPR_INSERTBEFID_1:  "{0}：局部頁面渲染嵌入錯誤，前節點的標識符 {1} 不在文件內",
            /** @constant */
            ERR_PPR_INSERTBEFID_2:  "{0}：局部頁面渲染嵌入錯誤，後節點的標識符 {1} 不在文件內",

            /** @constant */
            ERR_PPR_DELID:          "{0}：刪除錯誤，標識符不在XML標記中",
            /** @constant */
            ERR_PPR_UNKNOWNCID:     "{0}：不明的HTML組件標識符：{1}",

            /** @constant */
            ERR_NO_VIEWROOTATTR:    "{0}：不支援改變ViewRoot屬性",
            /** @constant */
            ERR_NO_HEADATTR:        "{0}：不支援改變Head的屬性",
            /** @constant */
            ERR_RED_URL:            "{0}：沒有重導向網址",

            /** @constant */
            ERR_REQ_FAILED_UNKNOWN: "請求失敗，狀態不明",

            /** @constant */
            ERR_REQU_FAILED: "請求失敗，狀態是 {0} 和原因是 {1}",

            /** @constant */
            UNKNOWN: "不明"
        });


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
if (_MF_CLS) {
    /**
     * System messages Traditional Chinese (Taiwan) version
     *
     * @class
     * @name Messages_zh_TW
     * @extends myfaces._impl.i18n.Messages
     * @memberOf myfaces._impl.i18n
     */
_MF_CLS &&  _MF_CLS(_PFX_I18N + "Messages_zh_TW", myfaces._impl.i18n.Messages,
            /** @lends myfaces._impl.i18n.Messages_zh_TW.prototype */
            {

                MSG_TEST:               "測試信息",

                /*Messages*/
                /** @constant */
                MSG_DEV_MODE:           "請注意，此信息只在項目發展階段，及沒有註冊錯誤監聽器而發放。",
                /** @constant */
                MSG_AFFECTED_CLASS:     "受影響類別：",
                /** @constant */
                MSG_AFFECTED_METHOD:    "受影響方法：",
                /** @constant */
                MSG_ERROR_NAME:         "錯誤名稱：",
                /** @constant */
                MSG_ERROR_MESSAGE:      "錯誤信息：",
                /** @constant */
                MSG_SERVER_ERROR_NAME:  "伺服器錯誤名稱：",
                /** @constant */
                MSG_ERROR_DESC:         "錯誤說明：",
                /** @constant */
                MSG_ERROR_NO:           "錯誤號碼：",
                /** @constant */
                MSG_ERROR_LINENO:       "錯誤行號：",

                /*Errors and messages*/
                /** @constant */
                ERR_FORM:               "不能判定源表單，要麼沒有連接元件到表單，要麼有多個相同標識符或名稱的表單，AJAX處理停止運作",
                /** @constant */
                ERR_VIEWSTATE:          "jsf.viewState：參數值不是表單類型！",
                /** @constant */
                ERR_TRANSPORT:          "不存在{0}傳輸類型",
                /** @constant */
                ERR_EVT_PASS:           "必須放棄事件（可能事件物件為空或未定義）",
                /** @constant */
                ERR_CONSTRUCT:          "構建事件數據時部分回應不能取得，原因是：{0}",
                /** @constant */
                ERR_MALFORMEDXML:       "無法解析伺服器的回應，伺服器返回的回應不是XML！",
                /** @constant */
                ERR_SOURCE_FUNC:        "來源不能是一個函數（可能來源和事件沒有定義或設定為空）",
                /** @constant */
                ERR_EV_OR_UNKNOWN:      "事件物件或不明必須作為第二個參數傳遞",
                /** @constant */
                ERR_SOURCE_NOSTR:       "來源不能是字串",
                /** @constant */
                ERR_SOURCE_DEF_NULL:    "來源必須定義或為空",

                //_Lang.js
                /** @constant */
                ERR_MUST_STRING:        "{0}：{1} 名稱空間必須是字串類型",
                /** @constant */
                ERR_REF_OR_ID:          "{0}：{1} 必須提供參考節點或標識符",
                /** @constant */
                ERR_PARAM_GENERIC:      "{0}：{1} 參數必須是 {2} 類型",
                /** @constant */
                ERR_PARAM_STR:          "{0}：{1} 參數必須是字串類型",
                /** @constant */
                ERR_PARAM_STR_RE:       "{0}：{1} 參數必須是字串類型或正規表達式",
                /** @constant */
                ERR_PARAM_MIXMAPS:      "{0}：必須提供來源及目標映射",
                /** @constant */
                ERR_MUST_BE_PROVIDED:   "{0}：必須提供 {1} 及 {2}",
                /** @constant */
                ERR_MUST_BE_PROVIDED1:  "{0}：必須設定 {1}",

                /** @constant */
                ERR_REPLACE_EL:         "調用replaceElements函數時evalNodes變量不是陣列類型",

                /** @constant */
                ERR_EMPTY_RESPONSE:     "{0}：回應不能為空的！",
                /** @constant */
                ERR_ITEM_ID_NOTFOUND:   "{0}：找不到有 {1} 標識符的項目",
                /** @constant */
                ERR_PPR_IDREQ:          "{0}：局部頁面渲染嵌入錯誤，標識符必須存在",
                /** @constant */
                ERR_PPR_INSERTBEFID:    "{0}：局部頁面渲染嵌入錯誤，前或後標識符必須存在",
                /** @constant */
                ERR_PPR_INSERTBEFID_1:  "{0}：局部頁面渲染嵌入錯誤，前節點的標識符 {1} 不在文件內",
                /** @constant */
                ERR_PPR_INSERTBEFID_2:  "{0}：局部頁面渲染嵌入錯誤，後節點的標識符 {1} 不在文件內",

                /** @constant */
                ERR_PPR_DELID:          "{0}：刪除錯誤，標識符不在XML標記中",
                /** @constant */
                ERR_PPR_UNKNOWNCID:     "{0}：不明的HTML組件標識符：{1}",

                /** @constant */
                ERR_NO_VIEWROOTATTR:    "{0}：不支援改變ViewRoot屬性",
                /** @constant */
                ERR_NO_HEADATTR:        "{0}：不支援改變Head的屬性",
                /** @constant */
                ERR_RED_URL:            "{0}：沒有重導向網址",

                /** @constant */
                ERR_REQ_FAILED_UNKNOWN: "請求失敗，狀態不明",

                /** @constant */
                ERR_REQU_FAILED: "請求失敗，狀態是 {0} 和原因是 {1}",

                /** @constant */
                UNKNOWN: "不明"
            });
}

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
/*
 theoretically we could save some code
 by
 defining the parent object as
 var parent = new Object();
 parent.prototype = new myfaces._impl.core._Runtime();
 extendClass(function () {
 }, parent , {
 But for now we are not doing it the little bit of saved
 space is not worth the loss of readability
 */
/**
 * @memberOf myfaces._impl
 * @namespace
 * @name _util
 */
/**
 * @class
 * @name _Lang
 * @memberOf myfaces._impl._util
 * @extends myfaces._impl.core._Runtime
 * @namespace
 * @description Object singleton for Language related methods, this object singleton
 * decorates the namespace myfaces._impl.core._Runtime and adds a bunch of new methods to
 * what _Runtime provided
 * */
_MF_SINGLTN(_PFX_UTIL + "_Lang", Object, /** @lends myfaces._impl._util._Lang.prototype */ {
    _processedExceptions:{},
    _installedLocale:null,
    _RT:myfaces._impl.core._Runtime,
    /**
     * returns a given localized message upon a given key
     * basic java log like templating functionality is included
     *
     * @param {String} key the key for the message
     * @param {String} defaultMessage optional default message if none was found
     *
     * Additionally you can pass additional arguments, which are used
     * in the same way java log templates use the params
     *
     * @param key
     */
    getMessage:function (key, defaultMessage /*,vararg templateParams*/) {
        if (!this._installedLocale) {
            //we first try to install language and variant, if that one fails
            //we try to install the language only, and if that one fails
            //we install the base messages
            this.initLocale();
        }
        var msg = this._installedLocale[key] || defaultMessage || key + " - undefined message";
        //we now make a simple templating replace of {0}, {1} etc... with their corresponding
        //arguments
        for (var cnt = 2; cnt < arguments.length; cnt++) {
            msg = msg.replace(new RegExp(["\\{", cnt - 2, "\\}"].join(""), "g"), new String(arguments[cnt]));
        }
        return msg;
    },
    /**
     * (re)inits the currently installed
     * messages so that after loading the main scripts
     * a new locale can be installed optionally
     * to our i18n subsystem
     *
     * @param newLocale locale override
     */
    initLocale:function (newLocale) {
        if (newLocale) {
            this._installedLocale = new newLocale();
            return;
        }
        var language_Variant = this._RT.getLanguage(this._RT.getGlobalConfig("locale")),
                langStr = language_Variant ? language_Variant.language : "",
                variantStr = language_Variant ? [language_Variant.language, "_", language_Variant.variant || ""].join("") : "",
                i18nRoot = myfaces._impl.i18n, i18nHolder = i18nRoot["Messages_" + variantStr] || i18nRoot["Messages_" + langStr] || i18nRoot["Messages"];
        this._installedLocale = new i18nHolder();
    },
    assertType:function (probe, theType) {
        return this._RT.assertType(probe, theType);
    },
    exists:function (nms, theType) {
        return this._RT.exists(nms, theType);
    },
    fetchNamespace:function (namespace) {
        this._assertStr(namespace, "fetchNamespace", "namespace");
        return this._RT.fetchNamespace(namespace);
    },
    reserveNamespace:function (namespace) {
        this._assertStr(namespace, "reserveNamespace", "namespace");
        return this._RT.reserveNamespace(namespace);
    },
    globalEval:function (code) {
        this._assertStr(code, "globalEval", "code");
        return  this._RT.globalEval(code);
    },
    /**
     * determines the correct event depending
     * on the browsers state
     *
     * @param evt incoming event object (note not all browsers
     * have this)
     *
     * @return an event object no matter what is incoming
     */
    getEvent:function (evt) {
        evt = (!evt) ? window.event || {} : evt;
        return evt;
    },
    /**
     * cross port from the dojo lib
     * browser save event resolution
     * @param evt the event object
     * (with a fallback for ie events if none is present)
     */
    getEventTarget:function (evt) {
        //ie6 and 7 fallback
        evt = this.getEvent(evt);
        /**
         * evt source is defined in the jsf events
         * seems like some component authors use our code
         * so we add it here see also
         * https://issues.apache.org/jira/browse/MYFACES-2458
         * not entirely a bug but makes sense to add this
         * behavior. I dont use it that way but nevertheless it
         * does not break anything so why not
         * */
        var t = evt.srcElement || evt.target || evt.source || null;
        while ((t) && (t.nodeType != 1)) {
            t = t.parentNode;
        }
        return t;
    },

    /**
     * equalsIgnoreCase, case insensitive comparison of two strings
     *
     * @param source
     * @param destination
     */
    equalsIgnoreCase:function (source, destination) {
        //either both are not set or null
        if (!source && !destination) {
            return true;
        }
        //source or dest is set while the other is not
        if (!source || !destination) return false;
        //in any other case we do a strong string comparison
        return source.toLowerCase() === destination.toLowerCase();
    },

    /**
     * Save document.getElementById (this code was ported over from dojo)
     * the idea is that either a string or domNode can be passed
     * @param {Object} reference the reference which has to be byIded
     */
    byId:function (/*object*/ reference) {
        if (!reference) {
            throw this.makeException(new Error(), null, null, this._nameSpace, "byId", this.getMessage("ERR_REF_OR_ID", null, "_Lang.byId", "reference"));
        }
        return (this.isString(reference)) ? document.getElementById(reference) : reference;
    },

    /**
     * String to array function performs a string to array transformation
     * @param {String} it the string which has to be changed into an array
     * @param {RegExp} splitter our splitter reglar expression
     * @return an array of the splitted string
     */
    strToArray:function (/*string*/ it, /*regexp*/ splitter) {
        //	summary:
        //		Return true if it is a String
        this._assertStr(it, "strToArray", "it");
        if (!splitter) {
            throw this.makeException(new Error(), null, null, this._nameSpace, "strToArray", this.getMessage("ERR_PARAM_STR_RE", null, "myfaces._impl._util._Lang.strToArray", "splitter"));
        }
        var retArr = it.split(splitter);
        var len = retArr.length;
        for (var cnt = 0; cnt < len; cnt++) {
            retArr[cnt] = this.trim(retArr[cnt]);
        }
        return retArr;
    },
    _assertStr:function (it, functionName, paramName) {
        if (!this.isString(it)) {
            throw this.makeException(new Error(), null, null, this._nameSpace, arguments.caller.toString(), this.getMessage("ERR_PARAM_STR", null, "myfaces._impl._util._Lang." + functionName, paramName));
        }
    },
    /**
     * hyperfast trim
     * http://blog.stevenlevithan.com/archives/faster-trim-javascript
     * crossported from dojo
     */
    trim:function (/*string*/ str) {
        this._assertStr(str, "trim", "str");
        str = str.replace(/^\s\s*/, '');
        var ws = /\s/, i = str.length;

        while (ws.test(str.charAt(--i))) {
            //do nothing
        }
        return str.slice(0, i + 1);
    },
    /**
     * Backported from dojo
     * a failsafe string determination method
     * (since in javascript String != "" typeof alone fails!)
     * @param it {|Object|} the object to be checked for being a string
     * @return true in case of being a string false otherwise
     */
    isString:function (/*anything*/ it) {
        //	summary:
        //		Return true if it is a String
        return !!arguments.length && it != null && (typeof it == "string" || it instanceof String); // Boolean
    },
    /**
     * hitch backported from dojo
     * hitch allows to assign a function to a dedicated scope
     * this is helpful in situations when function reassignments
     * can happen
     * (notably happens often in lazy xhr code)
     *
     * @param {Function} scope of the function to be executed in
     * @param {Function} method to be executed, the method must be of type function
     *
     * @return whatever the executed method returns
     */
    hitch:function (scope, method) {
        return !scope ? method : function () {
            return method.apply(scope, arguments || []);
        }; // Function
    },
    /**
     * Helper function to merge two maps
     * into one
     * @param {Object} dest the destination map
     * @param {Object} src the source map
     * @param {boolean} overwrite if set to true the destination is overwritten if the keys exist in both maps
     **/
    mixMaps:function (dest, src, overwrite, blockFilter, whitelistFilter) {
        if (!dest || !src) {
            throw this.makeException(new Error(), null, null, this._nameSpace, "mixMaps", this.getMessage("ERR_PARAM_MIXMAPS", null, "_Lang.mixMaps"));
        }
        var _undef = "undefined";
        for (var key in src) {
            if (!src.hasOwnProperty(key)) continue;
            if (blockFilter && blockFilter[key]) {
                continue;
            }
            if (whitelistFilter && !whitelistFilter[key]) {
                continue;
            }
            if (!overwrite) {
                /**
                 *we use exists instead of booleans because we cannot rely
                 *on all values being non boolean, we would need an elvis
                 *operator in javascript to shorten this :-(
                 */
                dest[key] = (_undef != typeof dest[key]) ? dest[key] : src[key];
            } else {
                dest[key] = (_undef != typeof src[key]) ? src[key] : dest[key];
            }
        }
        return dest;
    },
    /**
     * checks if an array contains an element
     * @param {Array} arr   array
     * @param {String} str string to check for
     */
    contains:function (arr, str) {
        if (!arr || !str) {
            throw this.makeException(new Error(), null, null, this._nameSpace, "contains", this.getMessage("ERR_MUST_BE_PROVIDED", null, "_Lang.contains", "arr {array}", "str {string}"));
        }
        return this.arrIndexOf(arr, str) != -1;
    },
    arrToMap:function (arr, offset) {
        var ret = new Array(arr.length);
        var len = arr.length;
        offset = (offset) ? offset : 0;
        for (var cnt = 0; cnt < len; cnt++) {
            ret[arr[cnt]] = cnt + offset;
        }
        return ret;
    },
    objToArray:function (obj, offset, pack) {
        if (!obj) {
            return null;
        }
        //since offset is numeric we cannot use the shortcut due to 0 being false
        //special condition array delivered no offset no pack
        if (obj instanceof Array && !offset && !pack)  return obj;
        var finalOffset = ('undefined' != typeof offset || null != offset) ? offset : 0;
        var finalPack = pack || [];
        try {
            return finalPack.concat(Array.prototype.slice.call(obj, finalOffset));
        } catch (e) {
            //ie8 (again as only browser) delivers for css 3 selectors a non convertible object
            //we have to do it the hard way
            //ie8 seems generally a little bit strange in its behavior some
            //objects break the function is everything methodology of javascript
            //and do not implement apply call, or are pseudo arrays which cannot
            //be sliced
            for (var cnt = finalOffset; cnt < obj.length; cnt++) {
                finalPack.push(obj[cnt]);
            }
            return finalPack;
        }
    },
    /**
     * foreach implementation utilizing the
     * ECMAScript wherever possible
     * with added functionality
     *
     * @param arr the array to filter
     * @param func the closure to apply the function to, with the syntax defined by the ecmascript functionality
     * function (element<,key, array>)
     * <p />
     * optional params
     * <p />
     * <ul>
     *      <li>param startPos (optional) the starting position </li>
     *      <li>param scope (optional) the scope to apply the closure to  </li>
     * </ul>
     */
    arrForEach:function (arr, func /*startPos, scope*/) {
        if (!arr || !arr.length) return;
        var startPos = Number(arguments[2]) || 0;
        var thisObj = arguments[3];
        //check for an existing foreach mapping on array prototypes
        //IE9 still does not pass array objects as result for dom ops
        arr = this.objToArray(arr);
        (startPos) ? arr.slice(startPos).forEach(func, thisObj) : arr.forEach(func, thisObj);
    },
    /**
     * foreach implementation utilizing the
     * ECMAScript wherever possible
     * with added functionality
     *
     * @param arr the array to filter
     * @param func the closure to apply the function to, with the syntax defined by the ecmascript functionality
     * function (element<,key, array>)
     * <p />
     * additional params
     * <ul>
     *  <li> startPos (optional) the starting position</li>
     *  <li> scope (optional) the scope to apply the closure to</li>
     * </ul>
     */
    arrFilter:function (arr, func /*startPos, scope*/) {
        if (!arr || !arr.length) return [];
        arr = this.objToArray(arr);
        return ((startPos) ? arr.slice(startPos).filter(func, thisObj) : arr.filter(func, thisObj));
    },
    /**
     * adds a EcmaScript optimized indexOf to our mix,
     * checks for the presence of an indexOf functionality
     * and applies it, otherwise uses a fallback to the hold
     * loop method to determine the index
     *
     * @param arr the array
     * @param element the index to search for
     */
    arrIndexOf:function (arr, element /*fromIndex*/) {
        if (!arr || !arr.length) return -1;
        var pos = Number(arguments[2]) || 0;
        arr = this.objToArray(arr);
        return arr.indexOf(element, pos);
    },
    /**
     * helper to automatically apply a delivered arguments map or array
     * to its destination which has a field "_"<key> and a full field
     *
     * @param dest the destination object
     * @param args the arguments array or map
     * @param argNames the argument names to be transferred
     */
    applyArgs:function (dest, args, argNames) {
        var UDEF = 'undefined';
        if (argNames) {
            for (var cnt = 0; cnt < args.length; cnt++) {
                //dest can be null or 0 hence no shortcut
                if (UDEF != typeof dest["_" + argNames[cnt]]) {
                    dest["_" + argNames[cnt]] = args[cnt];
                }
                if (UDEF != typeof dest[ argNames[cnt]]) {
                    dest[argNames[cnt]] = args[cnt];
                }
            }
        } else {
            for (var key in args) {
                if (!args.hasOwnProperty(key)) continue;
                if (UDEF != typeof dest["_" + key]) {
                    dest["_" + key] = args[key];
                }
                if (UDEF != typeof dest[key]) {
                    dest[key] = args[key];
                }
            }
        }
    },

    /**
     * transforms a key value pair into a string
     * @param key the key
     * @param val the value
     * @param delimiter the delimiter
     */
    keyValToStr:function (key, val, delimiter) {
        var ret = [], pushRet = this.hitch(ret, ret.push);
        pushRet(key);
        pushRet(val);
        delimiter = delimiter || "\n";
        pushRet(delimiter);
        return ret.join("");
    },
    parseXML:function (txt) {
        try {
            var parser = new DOMParser();
            return parser.parseFromString(txt, "text/xml");
        } catch (e) {
            //undefined internal parser error
            return null;
        }
    },
    serializeXML:function (xmlNode, escape) {
        if (!escape) {
            if (xmlNode.data) return xmlNode.data; //CDATA block has raw data
            if (xmlNode.textContent) return xmlNode.textContent; //textNode has textContent
        }
        return (new XMLSerializer()).serializeToString(xmlNode);
    },
    serializeChilds:function (xmlNode) {
        var buffer = [];
        if (!xmlNode.childNodes) return "";
        for (var cnt = 0; cnt < xmlNode.childNodes.length; cnt++) {
            buffer.push(this.serializeXML(xmlNode.childNodes[cnt]));
        }
        return buffer.join("");
    },
    isXMLParseError:function (xmlContent) {
        //no xml content
        if (xmlContent == null) return true;
        var findParseError = function (node) {
            if (!node || !node.childNodes) return false;
            for (var cnt = 0; cnt < node.childNodes.length; cnt++) {
                var childNode = node.childNodes[cnt];
                if (childNode.tagName && childNode.tagName == "parsererror") return true;
            }
            return false;
        };
        return !xmlContent ||
                (this.exists(xmlContent, "parseError.errorCode") && xmlContent.parseError.errorCode != 0) ||
                findParseError(xmlContent);
    },
    /**
     * fetches the error message from the xml content
     * in a browser independent way
     *
     * @param xmlContent
     * @return a map with the following structure {errorMessage: the error Message, sourceText: the text with the error}
     */
    fetchXMLErrorMessage:function (text, xmlContent) {
        var _t = this;
        var findParseError = function (node) {
            if (!node || !node.childNodes) return false;
            for (var cnt = 0; cnt < node.childNodes.length; cnt++) {
                var childNode = node.childNodes[cnt];
                if (childNode.tagName && childNode.tagName == "parsererror") {
                    var errorMessage = _t.serializeXML(childNode.childNodes[0]);
                    //we now have to determine the row and column position
                    var lastLine = errorMessage.split("\n");
                    lastLine = lastLine[lastLine.length-1];
                    var positions = lastLine.match(/[^0-9]*([0-9]+)[^0-9]*([0-9]+)[^0-9]*/);

                    var ret = {
                        errorMessage: errorMessage,
                        sourceText: _t.serializeXML(childNode.childNodes[1].childNodes[0])
                    }
                    if(positions) {
                        ret.line = Math.max(0, parseInt(positions[1])-1);
                        ret.linePos = Math.max(0, parseInt(positions[2])-1);
                    }
                    return ret;
                }
            }
            return null;
        };
        var ret = null;
        if (!xmlContent) {
            //chrome does not deliver any further data
            ret =  (this.trim(text || "").length > 0)? {errorMessage:"Illegal response",sourceText:""} : {errorMessage:"Empty Response",sourceText:""};
        } else if (this.exists(xmlContent, "parseError.errorCode") && xmlContent.parseError.errorCode != 0) {
            ret =   {
                errorMessage:xmlContent.parseError.reason,
                line:Math.max(0, parseInt(xmlContent.parseError.line)-1),
                linePos:Math.max(0,parseInt(xmlContent.parseError.linepos) -1),
                sourceText:xmlContent.parseError.srcText
            };
        } else {
            ret = findParseError(xmlContent);
        }
        //we have a line number we now can format the source accordingly
        if(ret && 'undefined' != typeof ret.line) {
            var source = ret.sourceText ||"";
            source = source.split("\n");
            if(source.length-1 < ret.line) return ret;
            source = source[ret.line];
            var secondLine = [];
            var lineLen = (ret.linePos - 2);
            for(var cnt = 0; cnt < lineLen; cnt++) {
                secondLine.push(" ");
            }
            secondLine.push("^^");
            ret.sourceText = source;
            ret.visualError = secondLine;
        }
        return ret;
    },

    /**
     * creates a neutral form data wrapper over an existing form Data element
     * the wrapper delegates following methods, append
     * and adds makeFinal as finalizing method which returns the final
     * send representation of the element
     *
     * @param formData an array
     */
    createFormDataDecorator:function (formData) {
        //we simulate the dom level 2 form element here
        var _newCls = null;
        var bufInstance = null;
        if (!this.FormDataDecoratorArray) {
            this.FormDataDecoratorArray = function (theFormData) {
                this._valBuf = theFormData;
                this._idx = {};
            };
            _newCls = this.FormDataDecoratorArray;
            _newCls.prototype.append = function (key, val) {
                this._valBuf.push([encodeURIComponent(key), encodeURIComponent(val)].join("="));
                this._idx[key] = true;
            };
            _newCls.prototype.hasKey = function (key) {
                return !!this._idx[key];
            };
            _newCls.prototype.makeFinal = function () {
                return this._valBuf.join("&");
            };
        }
        if (!this.FormDataDecoratorString) {
            this.FormDataDecoratorString = function (theFormData) {
                this._preprocessedData = theFormData;
                this._valBuf = [];
                this._idx = {};
            };
            _newCls = this.FormDataDecoratorString;
            _newCls.prototype.append = function (key, val) {
                this._valBuf.push([encodeURIComponent(key), encodeURIComponent(val)].join("="));
                this._idx[key] = true;
            };
            //for now we check only for keys which are added subsequently otherwise we do not perform any checks
            _newCls.prototype.hasKey = function (key) {
                return !!this._idx[key];
            };
            _newCls.prototype.makeFinal = function () {
                if (this._preprocessedData != "") {
                    return this._preprocessedData + "&" + this._valBuf.join("&")
                } else {
                    return this._valBuf.join("&");
                }
            };
        }
        if (!this.FormDataDecoratorOther) {
            this.FormDataDecoratorOther = function (theFormData) {
                this._valBuf = theFormData;
                this._idx = {};
            };
            _newCls = this.FormDataDecoratorOther;
            _newCls.prototype.append = function (key, val) {
                this._valBuf.append(key, val);
                this._idx[key] = true;
            };
            _newCls.prototype.hasKey = function (key) {
                return !!this._idx[key];
            };
            _newCls.prototype.makeFinal = function () {
                return this._valBuf;
            };
        }
        if (formData instanceof Array) {
            bufInstance = new this.FormDataDecoratorArray(formData);
        } else if (this.isString(formData)) {
            bufInstance = new this.FormDataDecoratorString(formData);
        } else {
            bufInstance = new this.FormDataDecoratorOther(formData);
        }
        return bufInstance;
    },
    /**
     * define a property mechanism which is browser neutral
     * we cannot use the existing setter and getter mechanisms
     * for now because old browsers do not support them
     * in the long run we probably can switch over
     * or make a code split between legacy and new
     *
     *
     * @param obj
     * @param name
     * @param value
     */
    attr:function (obj, name, value) {
        var findAccessor = function (theObj, theName) {
            return (theObj["_" + theName]) ? "_" + theName : ( (theObj[theName]) ? theName : null)
        };
        var applyAttr = function (theObj, theName, value, isFunc) {
            if (value) {
                if (isFunc) {
                    theObj[theName](value);
                } else {
                    theObj[theName] = value;
                }
                return null;
            }
            return (isFunc) ? theObj[theName]() : theObj[theName];
        };
        try {
            var finalAttr = findAccessor(obj, name);
            //simple attibute no setter and getter overrides
            if (finalAttr) {
                return applyAttr(obj, finalAttr, value);
            }
            //lets check for setter and getter overrides
            var found = false;
            var prefix = (value) ? "set" : "get";
            finalAttr = [prefix, name.substr(0, 1).toUpperCase(), name.substr(1)].join("");
            finalAttr = findAccessor(obj, finalAttr);
            if (finalAttr) {
                return applyAttr(obj, finalAttr, value, true);
            }

            throw this.makeException(new Error(), null, null, this._nameSpace, "contains", "property " + name + " not found");
        } finally {
            findAccessor = null;
            applyAttr = null;
        }
    },

    /**
     * creates an exeption with additional internal parameters
     * for extra information
     *
     * @param {String} title the exception title
     * @param {String} name  the exception name
     * @param {String} callerCls the caller class
     * @param {String} callFunc the caller function
     * @param {String} message the message for the exception
     */
    makeException:function (error, title, name, callerCls, callFunc, message) {
        error.name = name || "clientError";
        error.title = title || "";
        error.message = message || "";
        error._mfInternal = {};
        error._mfInternal.name = name || "clientError";
        error._mfInternal.title = title || "clientError";
        error._mfInternal.caller = callerCls || this._nameSpace;
        error._mfInternal.callFunc = callFunc || ("" + arguments.caller.toString());
        return error;
    }
});

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


/*
 theoretically we could save some code
 by
 defining the parent object as
 var parent = new Object();
 parent.prototype = new myfaces._impl.core._Runtime();
 extendClass(function () {
 }, parent , {
 But for now we are not doing it the little bit of saved
 space is not worth the loss of readability
 */

//html5 ecmascript 3 compliant browser, no quirks mode needed
if(!Array.prototype.forEach  && _MF_SINGLTN) {
/**
 * @memberOf myfaces._impl
 * @namespace
 * @name _util
 */

/**
 * @class
 * @name _Lang
 * @memberOf myfaces._impl._util
 * @extends myfaces._impl.core._Runtime
 * @namespace
 * @description Object singleton for Language related methods, this object singleton
 * decorates the namespace myfaces._impl.core._Runtime and adds a bunch of new methods to
 * what _Runtime provided
 * <p>This class provides the proper fallbacks for ie8- and Firefox 3.6-</p>
 * */
_MF_SINGLTN(_PFX_UTIL+"_LangQuirks", myfaces._impl._util._Lang, {

    constructor_: function() {
        this._callSuper("constructor_");
        var _RT = this._RT;
        var _T = this;
        //we only apply lazy if the jsf part is loaded already
        //otherwise we are at the correct position
        if(myfaces._impl.core.Impl) {
            _RT.iterateClasses(function(proto) {
                if(proto._Lang) proto._Lang = _T;
            });
        }

        myfaces._impl._util._Lang = _T;
    },

    /**
     * foreach implementation utilizing the
     * ECMAScript wherever possible
     * with added functionality
     *
     * @param arr the array to filter
     * @param func the closure to apply the function to, with the syntax defined by the ecmascript functionality
     * function (element<,key, array>)
     * <p />
     * optional params
     * <p />
     * <ul>
     *      <li>param startPos (optional) the starting position </li>
     *      <li>param scope (optional) the scope to apply the closure to  </li>
     * </ul>
     */
    arrForEach: function(arr, func /*startPos, scope*/) {
        if (!arr || !arr.length) return;
        try {
            var startPos = Number(arguments[2]) || 0;
            var thisObj = arguments[3];

            //check for an existing foreach mapping on array prototypes
            //IE9 still does not pass array objects as result for dom ops
            if (Array.prototype.forEach && arr.forEach) {
                (startPos) ? arr.slice(startPos).forEach(func, thisObj) : arr.forEach(func, thisObj);
            } else {
                startPos = (startPos < 0) ? Math.ceil(startPos) : Math.floor(startPos);
                if (typeof func != "function") {
                    throw new TypeError();
                }
                for (var cnt = 0; cnt < arr.length; cnt++) {
                    if (thisObj) {
                        func.call(thisObj, arr[cnt], cnt, arr);
                    } else {
                        func(arr[cnt], cnt, arr);
                    }
                }
            }
        } finally {
            func = null;
        }
    },

    /**
     * foreach implementation utilizing the
     * ECMAScript wherever possible
     * with added functionality
     *
     * @param arr the array to filter
     * @param func the closure to apply the function to, with the syntax defined by the ecmascript functionality
     * function (element<,key, array>)
     * <p />
     * additional params
     * <ul>
     *  <li> startPos (optional) the starting position</li>
     *  <li> scope (optional) the scope to apply the closure to</li>
     * </ul>
     */
    arrFilter: function(arr, func /*startPos, scope*/) {
        if (!arr || !arr.length) return [];
        try {
            var startPos = Number(arguments[2]) || 0;
            var thisObj = arguments[3];

            //check for an existing foreach mapping on array prototypes
            if (Array.prototype.filter) {
                return ((startPos) ? arr.slice(startPos).filter(func, thisObj) : arr.filter(func, thisObj));
            } else {
                if (typeof func != "function") {
                    throw new TypeError();
                }
                var ret = [];
                startPos = (startPos < 0) ? Math.ceil(startPos) : Math.floor(startPos);

                for (var cnt = startPos; cnt < arr.length; cnt++) {
                    var elem = null;
                    if (thisObj) {
                        elem = arr[cnt];
                        if (func.call(thisObj, elem, cnt, arr)) ret.push(elem);
                    } else {
                        elem = arr[cnt];
                        if (func(arr[cnt], cnt, arr)) ret.push(elem);
                    }
                }
            }
        } finally {
            func = null;
        }
    },

    /**
     * adds a EcmaScript optimized indexOf to our mix,
     * checks for the presence of an indexOf functionality
     * and applies it, otherwise uses a fallback to the hold
     * loop method to determine the index
     *
     * @param arr the array
     * @param element the index to search for
     */
    arrIndexOf: function(arr, element /*fromIndex*/) {
        if (!arr || !arr.length) return -1;
        var pos = Number(arguments[2]) || 0;

        if (Array.prototype.indexOf) {
            return arr.indexOf(element, pos);
        }
        //var cnt = this._space;
        var len = arr.length;
        pos = (pos < 0) ? Math.ceil(pos) : Math.floor(pos);

        //if negative then it is taken from as offset from the length of the array
        if (pos < 0) {
            pos += len;
        }
        while (pos < len && arr[pos] !== element) {
            pos++;
        }
        return (pos < len) ? pos : -1;
    },

    parseXML: function(txt) {
        try {
            var xmlDoc = null;
            if (window.DOMParser) {
                xmlDoc = this._callSuper("parseXML", txt);
            }
            else // Internet Explorer
            {
                xmlDoc = new ActiveXObject("Microsoft.XMLDOM");
                xmlDoc.async = "false";
                xmlDoc.loadXML(txt);
            }
            return xmlDoc;
        } catch (e) {
            //undefined internal parser error
            return null;
        }
    },

    serializeXML: function(xmlNode, escape) {
        if (xmlNode.xml) return xmlNode.xml; //IE
        return this._callSuper("serializeXML", xmlNode, escape);
    },

     /**
     * Concatenates an array to a string
     * @param {Array} arr the array to be concatenated
     * @param {String} delimiter the concatenation delimiter if none is set \n is used
     *
     * @return the concatenated array, one special behavior to enable j4fry compatibility has been added
     * if no delimiter is used the [entryNumber]+entry is generated for a single entry
     * TODO check if this is still needed it is somewhat outside of the scope of the function
     * and functionality wise dirty
     */
    arrToString : function(/*String or array*/ arr, /*string*/ delimiter) {
        if (!arr) {
            throw this._Lang.makeException(new Error(), null, null, this._nameSpace,"arrToString",  this.getMessage("ERR_MUST_BE_PROVIDED1",null, "arr {array}"));
        }
        if (this.isString(arr)) {
            return arr;
        }

        delimiter = delimiter || "\n";
        return arr.join(delimiter);
    }
});

}

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
 Base class which provides several helper functions over all objects
 */
_MF_CLS(_PFX_CORE+"Object", Object, {



    constructor_: function() {
        this._resettableContent = {};
        //to make those singleton references
        //overridable in the instance we have
        //to load them into the prototype instead
        //of the instance
        var proto = this._mfClazz.prototype;
        var impl = myfaces._impl;
        if(!proto._RT) {
            proto._RT  =  impl.core._Runtime;
            proto._Lang = impl._util._Lang;
            proto._Dom =  impl._util._Dom;
        }
    },

    /*optional functionality can be provided
     * for ie6 but is turned off by default*/
    _initDefaultFinalizableFields: function() {
        var isIE = this._RT.browser.isIE;
        if(!isIE || isIE > 7) return;
        for (var key in this) {
            //per default we reset everything which is not preinitalized
            if (null == this[key] && key != "_resettableContent" && key.indexOf("_mf") != 0 && key.indexOf("_") == 0) {
                this._resettableContent[key] = true;
            }
        }
    },

    /**
     * ie6 cleanup
     * This method disposes all properties manually in case of ie6
     * hence reduces the chance of running into a gc problem tremendously
     * on other browsers this method does nothing
     */
    _finalize: function() {
        try {
            if (this._isGCed || !this._RT.browser.isIE || !this._resettableContent) {
                //no ie, no broken garbage collector
                return;
            }

            for (var key in this._resettableContent) {
                if (this._RT.exists(this[key], "_finalize")) {
                    this[key]._finalize();
                }
                delete this[key];
            }
        } finally {
            this._isGCed = true;
        }
    },

    attr: function(name, value) {
       return this._Lang.attr(this, name, value);
    },

    getImpl: function() {
        this._Impl = this._Impl || this._RT.getGlobalConfig("jsfAjaxImpl", myfaces._impl.core.Impl);
        return this._Impl;
    },

    applyArgs: function(args) {
        this._Lang.applyArgs(this, args);
    },

    updateSingletons: function(key) {
        var _T = this;
        _T._RT.iterateSingletons(function(namespace) {
            if(namespace[key]) namespace[key] = _T;
        });
    }

});

(function() {
    /*some mobile browsers do not have a window object*/
    var target = window ||document;
    var _RT = myfaces._impl.core._Runtime;
    _RT._MF_OBJECT = target._MF_OBJECT;

     target._MF_OBJECT = myfaces._impl.core.Object;
})();

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
 * @name _Queue
 * @memberOf myfaces._impl._util
 * @description Queue implementation used by our runtime system
 * improved version of
 * @see <a href="http://safalra.com/web-design/javascript/queues/Queue.js">http://safalra.com/web-design/javascript/queues/Queue.js</a>
 */
_MF_CLS(_PFX_UTIL+"_Queue", _MF_OBJECT,
  /**
   * @lends myfaces._impl._util._Queue.prototype
   */
{
    //faster queue by http://safalra.com/web-design/javascript/queues/Queue.js
    //license public domain
    //The trick is to simply reduce the number of slice and slice ops to a bare minimum.

    _q : null,
    _space : 0,
    _size: -1,

    /**
     * Standard constructor
     */
    constructor_: function() {
        this._callSuper("constructor_");
        this._q = [];
    },

    /**
     * @return the length of the queue as integer
     */
    length: function() {
        // return the number of elements in the queue
        return this._q.length - this._space;

    },

    /**
     * @return true if the current queue is empty false otherwise
     */
    isEmpty: function() {
        // return true if the queue is empty, and false otherwise
        return (this._q.length == 0);
    },

    /**
     * Sets the current queue to a new size, all overflow elements at the end are stripped
     * automatically
     *
     * @param {int} newSize as numeric value
     */
    setQueueSize: function(newSize) {
        this._size = newSize;
        this._readjust();
    },

    /**
     * adds a listener to the queue
     *
     * @param element the listener to be added
     */
    enqueue : function(/*function*/element) {
        this._q.push(element);
        //qeuesize is bigger than the limit we drop one element so that we are
        //back in line

        this._readjust();
    },

    _readjust: function() {
        var size = this._size;
        while (size && size > -1 && this.length() > size) {
            this.dequeue();
        }
    },

    /**
     * removes a listener form the queue
     *
     * @param element the listener to be removed
     */
    remove : function(/*function*/element) {
        /*find element in queue*/
        var index = this.indexOf(element);
        /*found*/
        if (index != -1) {
            this._q.splice(index, 1);
        }
    },

    /**
     * dequeues the last element in the queue
     * @return {Object} element which is dequeued
     */
    dequeue: function() {
        // initialise the element to return to be undefined
        var element = null;

        // check whether the queue is empty
        var qLen = this._q.length;
        var queue = this._q;
        
        if (qLen) {

            // fetch the oldest element in the queue
            element = queue[this._space];

            // update the amount of space and check whether a shift should occur
            //added here a max limit of 30
            //now bit shift left is a tad faster than multiplication on most vms and does the same
            //unless we run into a bit skipping which is impossible in our usecases here
            if ((++this._space) << 1 >= qLen) {

                // set the queue equal to the non-empty portion of the queue
                this._q = queue.slice(this._space);

                // reset the amount of space at the front of the queue
                this._space = 0;

            }

        }

        // return the removed element
        return element;
    },

    /**
     * simple foreach
     *
     * @param closure a closure which processes the element
     * @code
     *   queue.each(function(element) {
     *      //do something with the element
     *   });
     */
    each: function(closure) {
        this._Lang.arrForEach(this._q, closure, this._space);
    },

    /**
     * Simple filter
     *
     * @param closure a closure which returns true or false depending
     * whether the filter has triggered
     *
     * @return an array of filtered queue entries
     */
    arrFilter: function(closure) {
        return this._Lang.arrFilter(this._q, closure, this._space);
    },

    /**
     * @param element
     * @return the current index of the element in the queue or -1 if it is not found
     */
    indexOf: function(element) {
        return this._Lang.arrIndexOf(this._q, element);
    },

    /**
     * resets the queue to initial empty state
     */
    cleanup: function() {
        this._q = [];
        this._space = 0;
    }
});


/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
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
/*
 * a classical listener queue pattern
 */



/**
 * @class
 * @name _ListenerQueue
 * @extends myfaces._impl._util._Queue
 * @memberOf myfaces._impl._util
 * @description Implementation of the listener queue for jsf.js
 * <p>It is based upon our high performance queue and adds dedicated
 * methods for listener based closures to the mix </p>
 * */
_MF_CLS(_PFX_UTIL+"_ListenerQueue", myfaces._impl._util._Queue,
/**
 * @lends myfaces._impl._util._ListenerQueue.prototype
 */
{
    /**
     * listener type safety assertion function
     *
     * @param {function} listener must be of type function otherwise an error is raised
     */
    _assertListener : function( listener) {
        if ("function" != typeof (listener)) {
            var msg = myfaces._impl._util._Lang.getMessage("ERR_PARAM_GENERIC",null,"_ListenerQueue", arguments.caller.toString(),"function" );
            throw this._Lang.makeException(new Error(), null, null, this._nameSpace,arguments.caller.toString(),  msg);
        }
    },

    /**
     * adds a listener to the queue
     *
     * @param {function} listener the listener to be added
     */
    enqueue : function(listener) {
        this._assertListener(listener);
        this._callSuper("enqueue", listener);
    },

    /**
     * removes a listener form the queue
     *
     * @param {function} listener the listener to be removed
     */
    remove : function(listener) {
        this._assertListener(listener);
        this._callSuper("remove", listener);
    },

    /**
     * generic broadcast with a number of arguments being passed down
     * @param {Object} argument the arguments passed down which are broadcast
     */
    broadcastEvent : function(argument) {
        var _args = myfaces._impl._util._Lang.objToArray(arguments);

        var broadCastFunc = function(element) {
            element.apply(null, _args);
        };
        try {
            this.each(broadCastFunc);
        } finally {
            broadCastFunc = null;
        }
    }
});
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
 * @name _Dom
 * @memberOf myfaces._impl._util
 * @extends myfaces._impl.core._Runtime
 * @description Object singleton collection of dom helper routines
 * (which in later incarnations will
 * get browser specific speed optimizations)
 *
 * Since we have to be as tight as possible
 * we will focus with our dom routines to only
 * the parts which our impl uses.
 * A jquery like query API would be nice
 * but this would increase up our codebase significantly
 *
 * <p>This class provides the proper fallbacks for ie8- and Firefox 3.6-</p>
 */
_MF_SINGLTN(_PFX_UTIL + "_Dom", Object, /** @lends myfaces._impl._util._Dom.prototype */ {

    /*table elements which are used in various parts */
    TABLE_ELEMS:  {
        "thead": 1,
        "tbody": 1,
        "tr": 1,
        "th": 1,
        "td": 1,
        "tfoot" : 1
    },

    _Lang:  myfaces._impl._util._Lang,
    _RT:    myfaces._impl.core._Runtime,
    _dummyPlaceHolder:null,

    /**
     * standard constructor
     */
    constructor_: function() {
    },

    runCss: function(item/*, xmlData*/) {

        var  UDEF = "undefined",
                _RT = this._RT,
                _Lang = this._Lang,
                applyStyle = function(item, style) {
                    var newSS = document.createElement("style");

                    newSS.setAttribute("rel", item.getAttribute("rel") || "stylesheet");
                    newSS.setAttribute("type", item.getAttribute("type") || "text/css");
                    document.getElementsByTagName("head")[0].appendChild(newSS);
                    //ie merrily again goes its own way
                    if (window.attachEvent && !_RT.isOpera && UDEF != typeof newSS.styleSheet && UDEF != newSS.styleSheet.cssText) newSS.styleSheet.cssText = style;
                    else newSS.appendChild(document.createTextNode(style));
                },

                execCss = function(item) {
                    var equalsIgnoreCase = _Lang.equalsIgnoreCase;
                    var tagName = item.tagName;
                    if (tagName && equalsIgnoreCase(tagName, "link") && equalsIgnoreCase(item.getAttribute("type"), "text/css")) {
                        applyStyle(item, "@import url('" + item.getAttribute("href") + "');");
                    } else if (tagName && equalsIgnoreCase(tagName, "style") && equalsIgnoreCase(item.getAttribute("type"), "text/css")) {
                        var innerText = [];
                        //compliant browsers know child nodes
                        var childNodes = item.childNodes;
                        if (childNodes) {
                            var len = childNodes.length;
                            for (var cnt = 0; cnt < len; cnt++) {
                                innerText.push(childNodes[cnt].innerHTML || childNodes[cnt].data);
                            }
                            //non compliant ones innerHTML
                        } else if (item.innerHTML) {
                            innerText.push(item.innerHTML);
                        }

                        applyStyle(item, innerText.join(""));
                    }
                };

        try {
            var scriptElements = this.findByTagNames(item, {"link":1,"style":1}, true);
            if (scriptElements == null) return;
            for (var cnt = 0; cnt < scriptElements.length; cnt++) {
                execCss(scriptElements[cnt]);
            }

        } finally {
            //the usual ie6 fix code
            //the IE6 garbage collector is broken
            //nulling closures helps somewhat to reduce
            //mem leaks, which are impossible to avoid
            //at this browser
            execCss = null;
            applyStyle = null;
        }
    },


    /**
     * Run through the given Html item and execute the inline scripts
     * (IE doesn't do this by itself)
     * @param {Node} item
     */
    runScripts: function(item, xmlData) {
        var _Lang = this._Lang,
            _RT = this._RT,
            finalScripts = [],
            execScrpt = function(item) {
                var tagName = item.tagName;
                var type = item.type || "";
                //script type javascript has to be handled by eval, other types
                //must be handled by the browser
                if (tagName && _Lang.equalsIgnoreCase(tagName, "script") &&
                        (type === "" ||
                        _Lang.equalsIgnoreCase(type,"text/javascript") ||
                        _Lang.equalsIgnoreCase(type,"javascript") ||
                        _Lang.equalsIgnoreCase(type,"text/ecmascript") ||
                        _Lang.equalsIgnoreCase(type,"ecmascript"))) {

                    var src = item.getAttribute('src');
                    if ('undefined' != typeof src
                            && null != src
                            && src.length > 0
                            ) {
                        //we have to move this into an inner if because chrome otherwise chokes
                        //due to changing the and order instead of relying on left to right
                        //if jsf.js is already registered we do not replace it anymore
                        if ((src.indexOf("ln=scripts") == -1 && src.indexOf("ln=javax.faces") == -1) || (src.indexOf("/jsf.js") == -1
                                && src.indexOf("/jsf-uncompressed.js") == -1)) {
                            if (finalScripts.length) {
                                //script source means we have to eval the existing
                                //scripts before running the include
                                _RT.globalEval(finalScripts.join("\n"));

                                finalScripts = [];
                            }
                            _RT.loadScriptEval(src, item.getAttribute('type'), false, "UTF-8", false);
                        }

                    } else {
                        // embedded script auto eval
                        var test = (!xmlData) ? item.text : _Lang.serializeChilds(item);
                        var go = true;
                        while (go) {
                            go = false;
                            if (test.substring(0, 1) == " ") {
                                test = test.substring(1);
                                go = true;
                            }
                            if (test.substring(0, 4) == "<!--") {
                                test = test.substring(4);
                                go = true;
                            }
                            if (test.substring(0, 11) == "//<![CDATA[") {
                                test = test.substring(11);
                                go = true;
                            }
                        }
                        // we have to run the script under a global context
                        //we store the script for less calls to eval
                        finalScripts.push(test);

                    }
                }
            };
        try {
            var scriptElements = this.findByTagName(item, "script", true);
            if (scriptElements == null) return;
            for (var cnt = 0; cnt < scriptElements.length; cnt++) {
                execScrpt(scriptElements[cnt]);
            }
            if (finalScripts.length) {
                _RT.globalEval(finalScripts.join("\n"));
            }
        } catch (e) {
            //we are now in accordance with the rest of the system of showing errors only in development mode
            //the default error output is alert we always can override it with
            //window.myfaces = window.myfaces || {};
            //myfaces.config =  myfaces.config || {};
            //myfaces.config.defaultErrorOutput = console.error;
            if(jsf.getProjectStage() === "Development") {
                var defaultErrorOutput = myfaces._impl.core._Runtime.getGlobalConfig("defaultErrorOutput", alert);
                defaultErrorOutput("Error in evaluated javascript:"+ (e.message || e.description || e));
            }
        } finally {
            //the usual ie6 fix code
            //the IE6 garbage collector is broken
            //nulling closures helps somewhat to reduce
            //mem leaks, which are impossible to avoid
            //at this browser
            execScrpt = null;
        }
    },


    /**
     * determines to fetch a node
     * from its id or name, the name case
     * only works if the element is unique in its name
     * @param {String} elem
     */
    byIdOrName: function(elem) {
        if (!elem) return null;
        if (!this._Lang.isString(elem)) return elem;

        var ret = this.byId(elem);
        if (ret) return ret;
        //we try the unique name fallback
        var items = document.getElementsByName(elem);
        return ((items.length == 1) ? items[0] : null);
    },

    /**
     * node id or name, determines the valid form identifier of a node
     * depending on its uniqueness
     *
     * Usually the id is chosen for an elem, but if the id does not
     * exist we try a name fallback. If the passed element has a unique
     * name we can use that one as subsequent identifier.
     *
     *
     * @param {String} elem
     */
    nodeIdOrName: function(elem) {
        if (elem) {
            //just to make sure that the pas

            elem = this.byId(elem);
            if (!elem) return null;
            //detached element handling, we also store the element name
            //to get a fallback option in case the identifier is not determinable
            // anymore, in case of a framework induced detachment the element.name should
            // be shared if the identifier is not determinable anymore
            //the downside of this method is the element name must be unique
            //which in case of jsf it is
            var elementId = elem.id || elem.name;
            if ((elem.id == null || elem.id == '') && elem.name) {
                elementId = elem.name;

                //last check for uniqueness
                if (this.getElementsByName(elementId).length > 1) {
                    //no unique element name so we need to perform
                    //a return null to let the caller deal with this issue
                    return null;
                }
            }
            return elementId;
        }
        return null;
    },

    deleteItems: function(items) {
        if (! items || ! items.length) return;
        for (var cnt = 0; cnt < items.length; cnt++) {
            this.deleteItem(items[cnt]);
        }
    },

    /**
     * Simple delete on an existing item
     */
    deleteItem: function(itemIdToReplace) {
        var item = this.byId(itemIdToReplace);
        if (!item) {
            throw this._Lang.makeException(new Error(),null, null, this._nameSpace, "deleteItem",  "_Dom.deleteItem  Unknown Html-Component-ID: " + itemIdToReplace);
        }

        this._removeNode(item, false);
    },

    /**
     * creates a node upon a given node name
     * @param nodeName {String} the node name to be created
     * @param attrs {Array} a set of attributes to be set
     */
    createElement: function(nodeName, attrs) {
        var ret = document.createElement(nodeName);
        if (attrs) {
            for (var key in attrs) {
                if(!attrs.hasOwnProperty(key)) continue;
                this.setAttribute(ret, key, attrs[key]);
            }
        }
        return ret;
    },

    /**
     * Checks whether the browser is dom compliant.
     * Dom compliant means that it performs the basic dom operations safely
     * without leaking and also is able to perform a native setAttribute
     * operation without freaking out
     *
     *
     * Not dom compliant browsers are all microsoft browsers in quirks mode
     * and ie6 and ie7 to some degree in standards mode
     * and pretty much every browser who cannot create ranges
     * (older mobile browsers etc...)
     *
     * We dont do a full browser detection here because it probably is safer
     * to test for existing features to make an assumption about the
     * browsers capabilities
     */
    isDomCompliant: function() {
        return true;
    },

    /**
     * proper insert before which takes tables into consideration as well as
     * browser deficiencies
     * @param item the node to insert before
     * @param markup the markup to be inserted
     */
    insertBefore: function(item, markup) {
        this._assertStdParams(item, markup, "insertBefore");

        markup = this._Lang.trim(markup);
        if (markup === "") return null;

        var evalNodes = this._buildEvalNodes(item, markup),
                currentRef = item,
                parentNode = item.parentNode,
                ret = [];
        for (var cnt = evalNodes.length - 1; cnt >= 0; cnt--) {
            currentRef = parentNode.insertBefore(evalNodes[cnt], currentRef);
            ret.push(currentRef);
        }
        ret = ret.reverse();
        this._eval(ret);
        return ret;
    },

    /**
     * proper insert before which takes tables into consideration as well as
     * browser deficiencies
     * @param item the node to insert before
     * @param markup the markup to be inserted
     */
    insertAfter: function(item, markup) {
        this._assertStdParams(item, markup, "insertAfter");
        markup = this._Lang.trim(markup);
        if (markup === "") return null;

        var evalNodes = this._buildEvalNodes(item, markup),
                currentRef = item,
                parentNode = item.parentNode,
                ret = [];

        for (var cnt = 0; cnt < evalNodes.length; cnt++) {
            if (currentRef.nextSibling) {
                //Winmobile 6 has problems with this strategy, but it is not really fixable
                currentRef = parentNode.insertBefore(evalNodes[cnt], currentRef.nextSibling);
            } else {
                currentRef = parentNode.appendChild(evalNodes[cnt]);
            }
            ret.push(currentRef);
        }
        this._eval(ret);
        return ret;
    },

    propertyToAttribute: function(name) {
        if (name === 'className') {
            return 'class';
        } else if (name === 'xmllang') {
            return 'xml:lang';
        } else {
            return name.toLowerCase();
        }
    },

    isFunctionNative: function(func) {
        return /^\s*function[^{]+{\s*\[native code\]\s*}\s*$/.test(String(func));
    },

    detectAttributes: function(element) {
        //test if 'hasAttribute' method is present and its native code is intact
        //for example, Prototype can add its own implementation if missing
        if (element.hasAttribute && this.isFunctionNative(element.hasAttribute)) {
            return function(name) {
                return element.hasAttribute(name);
            }
        } else {
            try {
                //when accessing .getAttribute method without arguments does not throw an error then the method is not available
                element.getAttribute;

                var html = element.outerHTML;
                var startTag = html.match(/^<[^>]*>/)[0];
                return function(name) {
                    return startTag.indexOf(name + '=') > -1;
                }
            } catch (ex) {
                return function(name) {
                    return element.getAttribute(name);
                }
            }
        }
    },

    /**
     * copy all attributes from one element to another - except id
     * @param target element to copy attributes to
     * @param source element to copy attributes from
     * @ignore
     */
    cloneAttributes: function(target, source) {

        // enumerate core element attributes - without 'dir' as special case
        var coreElementProperties = ['className', 'title', 'lang', 'xmllang'];
        // enumerate additional input element attributes
        var inputElementProperties = [
            'name', 'value', 'size', 'maxLength', 'src', 'alt', 'useMap', 'tabIndex', 'accessKey', 'accept', 'type'
        ];
        // enumerate additional boolean input attributes
        var inputElementBooleanProperties = [
            'checked', 'disabled', 'readOnly'
        ];

        // Enumerate all the names of the event listeners
        var listenerNames =
            [ 'onclick', 'ondblclick', 'onmousedown', 'onmousemove', 'onmouseout',
                'onmouseover', 'onmouseup', 'onkeydown', 'onkeypress', 'onkeyup',
                'onhelp', 'onblur', 'onfocus', 'onchange', 'onload', 'onunload', 'onabort',
                'onreset', 'onselect', 'onsubmit'
            ];

        var sourceAttributeDetector = this.detectAttributes(source);
        var targetAttributeDetector = this.detectAttributes(target);

        var isInputElement = target.nodeName.toLowerCase() === 'input';
        var propertyNames = isInputElement ? coreElementProperties.concat(inputElementProperties) : coreElementProperties;
        var isXML = !source.ownerDocument.contentType || source.ownerDocument.contentType == 'text/xml';
        for (var iIndex = 0, iLength = propertyNames.length; iIndex < iLength; iIndex++) {
            var propertyName = propertyNames[iIndex];
            var attributeName = this.propertyToAttribute(propertyName);
            if (sourceAttributeDetector(attributeName)) {

                //With IE 7 (quirks or standard mode) and IE 8/9 (quirks mode only),
                //you cannot get the attribute using 'class'. You must use 'className'
                //which is the same value you use to get the indexed property. The only
                //reliable way to detect this (without trying to evaluate the browser
                //mode and version) is to compare the two return values using 'className'
                //to see if they exactly the same.  If they are, then use the property
                //name when using getAttribute.
                if( attributeName == 'class'){
                    if( this._RT.browser.isIE && (source.getAttribute(propertyName) === source[propertyName]) ){
                        attributeName = propertyName;
                    }
                }

                var newValue = isXML ? source.getAttribute(attributeName) : source[propertyName];
                var oldValue = target[propertyName];
                if (oldValue != newValue) {
                    target[propertyName] = newValue;
                }
            } else {
                target.removeAttribute(attributeName);
                if (attributeName == "value") {
                    target[propertyName] = '';
                }
            }
        }

        var booleanPropertyNames = isInputElement ? inputElementBooleanProperties : [];
        for (var jIndex = 0, jLength = booleanPropertyNames.length; jIndex < jLength; jIndex++) {
            var booleanPropertyName = booleanPropertyNames[jIndex];
            var newBooleanValue = source[booleanPropertyName];
            var oldBooleanValue = target[booleanPropertyName];
            if (oldBooleanValue != newBooleanValue) {
                target[booleanPropertyName] = newBooleanValue;
            }
        }

        //'style' attribute special case
        if (sourceAttributeDetector('style')) {
            var newStyle;
            var oldStyle;
            if (this._RT.browser.isIE) {
                newStyle = source.style.cssText;
                oldStyle = target.style.cssText;
                if (newStyle != oldStyle) {
                    target.style.cssText = newStyle;
                }
            } else {
                newStyle = source.getAttribute('style');
                oldStyle = target.getAttribute('style');
                if (newStyle != oldStyle) {
                    target.setAttribute('style', newStyle);
                }
            }
        } else if (targetAttributeDetector('style')){
            target.removeAttribute('style');
        }

        // Special case for 'dir' attribute
        if (!this._RT.browser.isIE && source.dir != target.dir) {
            if (sourceAttributeDetector('dir')) {
                target.dir = source.dir;
            } else if (targetAttributeDetector('dir')) {
                target.dir = '';
            }
        }

        for (var lIndex = 0, lLength = listenerNames.length; lIndex < lLength; lIndex++) {
            var name = listenerNames[lIndex];
            target[name] = source[name] ? source[name] : null;
            if (source[name]) {
                source[name] = null;
            }
        }

        //clone HTML5 data-* attributes
        try{
            var targetDataset = target.dataset;
            var sourceDataset = source.dataset;
            if (targetDataset || sourceDataset) {
                //cleanup the dataset
                for (var tp in targetDataset) {
                    delete targetDataset[tp];
                }
                //copy dataset's properties
                for (var sp in sourceDataset) {
                    targetDataset[sp] = sourceDataset[sp];
                }
            }
        } catch (ex) {
            //most probably dataset properties are not supported
        }
    },
    //from
    // http://blog.vishalon.net/index.php/javascript-getting-and-setting-caret-position-in-textarea/
    getCaretPosition:function (ctrl) {
        var caretPos = 0;

        try {

            // other browsers make it simpler by simply having a selection start element
            if (ctrl.selectionStart || ctrl.selectionStart == '0')
                caretPos = ctrl.selectionStart;
            // ie 5 quirks mode as second option because
            // this option is flakey in conjunction with text areas
            // TODO move this into the quirks class
            else if (document.selection) {
                ctrl.focus();
                var selection = document.selection.createRange();
                //the selection now is start zero
                selection.moveStart('character', -ctrl.value.length);
                //the caretposition is the selection start
                caretPos = selection.text.length;
            }
        } catch (e) {
            //now this is ugly, but not supported input types throw errors for selectionStart
            //this way we are future proof by having not to define every selection enabled
            //input in an if (which will be a lot in the near future with html5)
        }
        return caretPos;
    },

    setCaretPosition:function (ctrl, pos) {

        if (ctrl.createTextRange) {
            var range = ctrl.createTextRange();
            range.collapse(true);
            range.moveEnd('character', pos);
            range.moveStart('character', pos);
            range.select();
        }
        //IE quirks mode again, TODO move this into the quirks class
        else if (ctrl.setSelectionRange) {
            ctrl.focus();
            //the selection range is our caret position
            ctrl.setSelectionRange(pos, pos);
        }
    },

    /**
     * outerHTML replacement which works cross browserlike
     * but still is speed optimized
     *
     * @param item the item to be replaced
     * @param markup the markup for the replacement
     * @param preserveFocus, tries to preserve the focus within the outerhtml operation
     * if set to true a focus preservation algorithm based on document.activeElement is
     * used to preserve the focus at the exactly same location as it was
     *
     */
    outerHTML : function(item, markup, preserveFocus) {
        this._assertStdParams(item, markup, "outerHTML");
        // we can work on a single element in a cross browser fashion
        // regarding the focus thanks to the
        // icefaces team for providing the code
        if (item.nodeName.toLowerCase() === 'input') {
            var replacingInput = this._buildEvalNodes(item, markup)[0];
            this.cloneAttributes(item, replacingInput);
            return item;
        } else {
            markup = this._Lang.trim(markup);
            if (markup !== "") {
                var ret = null;

                var focusElementId = null;
                var caretPosition = 0;
                if (preserveFocus && 'undefined' != typeof document.activeElement) {
                    focusElementId = (document.activeElement) ? document.activeElement.id : null;
                    caretPosition = this.getCaretPosition(document.activeElement);
                }
                // we try to determine the browsers compatibility
                // level to standards dom level 2 via various methods
                if (this.isDomCompliant()) {
                    ret = this._outerHTMLCompliant(item, markup);
                } else {
                    //call into abstract method
                    ret = this._outerHTMLNonCompliant(item, markup);
                }
                if (focusElementId) {
                    var newFocusElement = this.byId(focusElementId);
                    if (newFocusElement && newFocusElement.nodeName.toLowerCase() === 'input') {
                        //just in case the replacement element is not focusable anymore
                        if ("undefined" != typeof newFocusElement.focus) {
                            newFocusElement.focus();
                        }
                    }
                    if (newFocusElement && caretPosition) {
                        //zero caret position is set automatically on focus
                        this.setCaretPosition(newFocusElement, caretPosition);
                    }
                }

                // and remove the old item
                //first we have to save the node newly insert for easier access in our eval part
                this._eval(ret);
                return ret;
            }
            // and remove the old item, in case of an empty newtag and do nothing else
            this._removeNode(item, false);
            return null;
        }
    },

    isFunctionNative: function(func) {
        return /^\s*function[^{]+{\s*\[native code\]\s*}\s*$/.test(String(func));
    },

    detectAttributes: function(element) {
        //test if 'hasAttribute' method is present and its native code is intact
        //for example, Prototype can add its own implementation if missing
        if (element.hasAttribute && this.isFunctionNative(element.hasAttribute)) {
            return function(name) {
                return element.hasAttribute(name);
            }
        } else {
            try {
                //when accessing .getAttribute method without arguments does not throw an error then the method is not available
                element.getAttribute;

                var html = element.outerHTML;
                var startTag = html.match(/^<[^>]*>/)[0];
                return function(name) {
                    return startTag.indexOf(name + '=') > -1;
                }
            } catch (ex) {
                return function(name) {
                    return element.getAttribute(name);
                }
            }
        }
    },

    /**
     * detaches a set of nodes from their parent elements
     * in a browser independend manner
     * @param {Object} items the items which need to be detached
     * @return {Array} an array of nodes with the detached dom nodes
     */
    detach: function(items) {
        var ret = [];
        if ('undefined' != typeof items.nodeType) {
            if (items.parentNode) {
                ret.push(items.parentNode.removeChild(items));
            } else {
                ret.push(items);
            }
            return ret;
        }
        //all ies treat node lists not as arrays so we have to take
        //an intermediate step
        var nodeArr = this._Lang.objToArray(items);
        for (var cnt = 0; cnt < nodeArr.length; cnt++) {
            ret.push(nodeArr[cnt].parentNode.removeChild(nodeArr[cnt]));
        }
        return ret;
    },

    _outerHTMLCompliant: function(item, markup) {
        //table element replacements like thead, tbody etc... have to be treated differently
        var evalNodes = this._buildEvalNodes(item, markup);

        if (evalNodes.length == 1) {
            var ret = evalNodes[0];
            item.parentNode.replaceChild(ret, item);
            return ret;
        } else {
            return this.replaceElements(item, evalNodes);
        }
    },

    /**
     * checks if the provided element is a subelement of a table element
     * @param item
     */
    _isTableElement: function(item) {
        return !!this.TABLE_ELEMS[(item.nodeName || item.tagName).toLowerCase()];
    },

    /**
     * non ie browsers do not have problems with embedded scripts or any other construct
     * we simply can use an innerHTML in a placeholder
     *
     * @param markup the markup to be used
     */
    _buildNodesCompliant: function(markup) {
        var dummyPlaceHolder = this.getDummyPlaceHolder(); //document.createElement("div");
        dummyPlaceHolder.innerHTML = markup;
        return this._Lang.objToArray(dummyPlaceHolder.childNodes);
    },




    /**
     * builds up a correct dom subtree
     * if the markup is part of table nodes
     * The usecase for this is to allow subtable rendering
     * like single rows thead or tbody
     *
     * @param item
     * @param markup
     */
    _buildTableNodes: function(item, markup) {
        var itemNodeName = (item.nodeName || item.tagName).toLowerCase();

        var tmpNodeName = itemNodeName;
        var depth = 0;
        while (tmpNodeName != "table") {
            item = item.parentNode;
            tmpNodeName = (item.nodeName || item.tagName).toLowerCase();
            depth++;
        }

        var dummyPlaceHolder = this.getDummyPlaceHolder();
        if (itemNodeName == "td") {
            dummyPlaceHolder.innerHTML = "<table><tbody><tr>" + markup + "</tr></tbody></table>";
        } else {
            dummyPlaceHolder.innerHTML = "<table>" + markup + "</table>";
        }

        for (var cnt = 0; cnt < depth; cnt++) {
            dummyPlaceHolder = dummyPlaceHolder.childNodes[0];
        }

        return this.detach(dummyPlaceHolder.childNodes);
    },

    _removeChildNodes: function(node /*, breakEventsOpen */) {
        if (!node) return;
        node.innerHTML = "";
    },



    _removeNode: function(node /*, breakEventsOpen*/) {
        if (!node) return;
        var parentNode = node.parentNode;
        if (parentNode) //if the node has a parent
            parentNode.removeChild(node);
    },


    /**
     * build up the nodes from html markup in a browser independend way
     * so that it also works with table nodes
     *
     * @param item the parent item upon the nodes need to be processed upon after building
     * @param markup the markup to be built up
     */
    _buildEvalNodes: function(item, markup) {
        var evalNodes = null;
        if (this._isTableElement(item)) {
            evalNodes = this._buildTableNodes(item, markup);
        } else {
            var nonIEQuirks = (!this._RT.browser.isIE || this._RT.browser.isIE > 8);
            //ie8 has a special problem it still has the swallow scripts and other
            //elements bug, but it is mostly dom compliant so we have to give it a special
            //treatment, IE9 finally fixes that issue finally after 10 years
            evalNodes = (this.isDomCompliant() &&  nonIEQuirks) ?
                    this._buildNodesCompliant(markup) :
                    //ie8 or quirks mode browsers
                    this._buildNodesNonCompliant(markup);
        }
        return evalNodes;
    },

    /**
     * we have lots of methods with just an item and a markup as params
     * this method builds an assertion for those methods to reduce code
     *
     * @param item  the item to be tested
     * @param markup the markup
     * @param caller caller function
     * @param {optional} params array of assertion param names
     */
    _assertStdParams: function(item, markup, caller, params) {
        //internal error
        if (!caller) {
            throw this._Lang.makeException(new Error(), null, null, this._nameSpace, "_assertStdParams",  "Caller must be set for assertion");
        }
        var _Lang = this._Lang,
                ERR_PROV = "ERR_MUST_BE_PROVIDED1",
                DOM = "myfaces._impl._util._Dom.",
                finalParams = params || ["item", "markup"];

        if (!item || !markup) {
            _Lang.makeException(new Error(), null, null,DOM, ""+caller,  _Lang.getMessage(ERR_PROV, null, DOM +"."+ caller, (!item) ? finalParams[0] : finalParams[1]));
            //throw Error(_Lang.getMessage(ERR_PROV, null, DOM + caller, (!item) ? params[0] : params[1]));
        }
    },

    /**
     * internal eval handler used by various functions
     * @param _nodeArr
     */
    _eval: function(_nodeArr) {
        if (this.isManualScriptEval()) {
            var isArr = _nodeArr instanceof Array;
            if (isArr && _nodeArr.length) {
                for (var cnt = 0; cnt < _nodeArr.length; cnt++) {
                    this.runScripts(_nodeArr[cnt]);
                }
            } else if (!isArr) {
                this.runScripts(_nodeArr);
            }
        }
    },

    /**
     * for performance reasons we work with replaceElement and replaceElements here
     * after measuring performance it has shown that passing down an array instead
     * of a single node makes replaceElement twice as slow, however
     * a single node case is the 95% case
     *
     * @param item
     * @param evalNode
     */
    replaceElement: function(item, evalNode) {
        //browsers with defect garbage collection
        item.parentNode.insertBefore(evalNode, item);
        this._removeNode(item, false);
    },


    /**
     * replaces an element with another element or a set of elements
     *
     * @param item the item to be replaced
     *
     * @param evalNodes the elements
     */
    replaceElements: function (item, evalNodes) {
        var evalNodesDefined = evalNodes && 'undefined' != typeof evalNodes.length;
        if (!evalNodesDefined) {
            throw this._Lang.makeException(new Error(), null, null, this._nameSpace, "replaceElements",  this._Lang.getMessage("ERR_REPLACE_EL"));
        }

        var parentNode = item.parentNode,

                sibling = item.nextSibling,
                resultArr = this._Lang.objToArray(evalNodes);

        for (var cnt = 0; cnt < resultArr.length; cnt++) {
            if (cnt == 0) {
                this.replaceElement(item, resultArr[cnt]);
            } else {
                if (sibling) {
                    parentNode.insertBefore(resultArr[cnt], sibling);
                } else {
                    parentNode.appendChild(resultArr[cnt]);
                }
            }
        }
        return resultArr;
    },

    /**
     * optimized search for an array of tag names
     * deep scan will always be performed.
     * @param fragment the fragment which should be searched for
     * @param tagNames an map indx of tag names which have to be found
     *
     */
    findByTagNames: function(fragment, tagNames) {
        this._assertStdParams(fragment, tagNames, "findByTagNames", ["fragment", "tagNames"]);

        var nodeType = fragment.nodeType;
        if (nodeType != 1 && nodeType != 9 && nodeType != 11) return null;

        //we can use the shortcut
        if (fragment.querySelectorAll) {
            var query = [];
            for (var key in tagNames) {
                if(!tagNames.hasOwnProperty(key)) continue;
                query.push(key);
            }
            var res = [];
            if (fragment.tagName && tagNames[fragment.tagName.toLowerCase()]) {
                res.push(fragment);
            }
            return res.concat(this._Lang.objToArray(fragment.querySelectorAll(query.join(", "))));
        }

        //now the filter function checks case insensitively for the tag names needed
        var filter = function(node) {
            return node.tagName && tagNames[node.tagName.toLowerCase()];
        };

        //now we run an optimized find all on it
        try {
            return this.findAll(fragment, filter, true);
        } finally {
            //the usual IE6 is broken, fix code
            filter = null;
        }
    },

    /**
     * determines the number of nodes according to their tagType
     *
     * @param {Node} fragment (Node or fragment) the fragment to be investigated
     * @param {String} tagName the tag name (lowercase)
     * (the normal usecase is false, which means if the element is found only its
     * adjacent elements will be scanned, due to the recursive descension
     * this should work out with elements with different nesting depths but not being
     * parent and child to each other
     *
     * @return the child elements as array or null if nothing is found
     *
     */
    findByTagName : function(fragment, tagName) {
        this._assertStdParams(fragment, tagName, "findByTagName", ["fragment", "tagName"]);
        var _Lang = this._Lang,
                nodeType = fragment.nodeType;
        if (nodeType != 1 && nodeType != 9 && nodeType != 11) return null;

        //remapping to save a few bytes

        var ret = _Lang.objToArray(fragment.getElementsByTagName(tagName));
        if (fragment.tagName && _Lang.equalsIgnoreCase(fragment.tagName, tagName)) ret.unshift(fragment);
        return ret;
    },

    findByName : function(fragment, name) {
        this._assertStdParams(fragment, name, "findByName", ["fragment", "name"]);

        var nodeType = fragment.nodeType;
        if (nodeType != 1 && nodeType != 9 && nodeType != 11) return null;

        var ret = this._Lang.objToArray(fragment.getElementsByName(name));
        if (fragment.name == name) ret.unshift(fragment);
        return ret;
    },

    /**
     * a filtered findAll for subdom treewalking
     * (which uses browser optimizations wherever possible)
     *
     * @param {|Node|} rootNode the rootNode so start the scan
     * @param filter filter closure with the syntax {boolean} filter({Node} node)
     * @param deepScan if set to true or not set at all a deep scan is performed (for form scans it does not make much sense to deeply scan)
     */
    findAll : function(rootNode, filter, deepScan) {
        this._Lang.assertType(filter, "function");
        deepScan = !!deepScan;

        if (document.createTreeWalker && NodeFilter) {
            return this._iteratorSearchAll(rootNode, filter, deepScan);
        } else {
            //will not be called in dom level3 compliant browsers
            return this._recursionSearchAll(rootNode, filter, deepScan);
        }
    },

    /**
     * the faster dom iterator based search, works on all newer browsers
     * except ie8 which already have implemented the dom iterator functions
     * of html 5 (which is pretty all standard compliant browsers)
     *
     * The advantage of this method is a faster tree iteration compared
     * to the normal recursive tree walking.
     *
     * @param rootNode the root node to be iterated over
     * @param filter the iteration filter
     * @param deepScan if set to true a deep scan is performed
     */
    _iteratorSearchAll: function(rootNode, filter, deepScan) {
        var retVal = [];
        //Works on firefox and webkit, opera and ie have to use the slower fallback mechanis
        //we have a tree walker in place this allows for an optimized deep scan
        if (filter(rootNode)) {

            retVal.push(rootNode);
            if (!deepScan) {
                return retVal;
            }
        }
        //we use the reject mechanism to prevent a deep scan reject means any
        //child elements will be omitted from the scan
        var FILTER_ACCEPT = NodeFilter.FILTER_ACCEPT,
                FILTER_SKIP = NodeFilter.FILTER_SKIP,
                FILTER_REJECT = NodeFilter.FILTER_REJECT;

        var walkerFilter = function (node) {
            var retCode = (filter(node)) ? FILTER_ACCEPT : FILTER_SKIP;
            retCode = (!deepScan && retCode == FILTER_ACCEPT) ? FILTER_REJECT : retCode;
            if (retCode == FILTER_ACCEPT || retCode == FILTER_REJECT) {
                retVal.push(node);
            }
            return retCode;
        };

        var treeWalker = document.createTreeWalker(rootNode, NodeFilter.SHOW_ELEMENT, walkerFilter, false);
        //noinspection StatementWithEmptyBodyJS
        while (treeWalker.nextNode());
        return retVal;
    },

    /**
     * bugfixing for ie6 which does not cope properly with setAttribute
     */
    setAttribute : function(node, attr, val) {
        this._assertStdParams(node, attr, "setAttribute", ["fragment", "name"]);
        if (!node.setAttribute) {
            return;
        }

        if (attr === 'disabled') {
            node.disabled = val === 'disabled' || val === 'true';
        } else if (attr === 'checked') {
            node.checked = val === 'checked' || val === 'on' || val === 'true';
        } else if (attr == 'readonly') {
            node.readOnly = val === 'readonly' || val === 'true';
        } else {
            node.setAttribute(attr, val);
        }
    },

    /**
     * fuzzy form detection which tries to determine the form
     * an item has been detached.
     *
     * The problem is some Javascript libraries simply try to
     * detach controls by reusing the names
     * of the detached input controls. Most of the times,
     * the name is unique in a jsf scenario, due to the inherent form mapping.
     * One way or the other, we will try to fix that by
     * identifying the proper form over the name
     *
     * We do it in several ways, in case of no form null is returned
     * in case of multiple forms we check all elements with a given name (which we determine
     * out of a name or id of the detached element) and then iterate over them
     * to find whether they are in a form or not.
     *
     * If only one element within a form and a given identifier found then we can pull out
     * and move on
     *
     * We cannot do much further because in case of two identical named elements
     * all checks must fail and the first elements form is served.
     *
     * Note, this method is only triggered in case of the issuer or an ajax request
     * is a detached element, otherwise already existing code has served the correct form.
     *
     * This method was added because of
     * https://issues.apache.org/jira/browse/MYFACES-2599
     * to support the integration of existing ajax libraries which do heavy dom manipulation on the
     * controls side (Dojos Dijit library for instance).
     *
     * @param {Node} elem - element as source, can be detached, undefined or null
     *
     * @return either null or a form node if it could be determined
     *
     * TODO move this into extended and replace it with a simpler algorithm
     */
    fuzzyFormDetection : function(elem) {
        var forms = document.forms, _Lang = this._Lang;

        if (!forms || !forms.length) {
            return null;
        }

        // This will not work well on portlet case, because we cannot be sure
        // the returned form is right one.
        //we can cover that case by simply adding one of our config params
        //the default is the weaker, but more correct portlet code
        //you can override it with myfaces_config.no_portlet_env = true globally
        else if (1 == forms.length && this._RT.getGlobalConfig("no_portlet_env", false)) {
            return forms[0];
        }

        //before going into the more complicated stuff we try the simple approach
        var finalElem = this.byId(elem);
        var fetchForm = _Lang.hitch(this, function(elem) {
            //element of type form then we are already
            //at form level for the issuing element
            //https://issues.apache.org/jira/browse/MYFACES-2793

            return (_Lang.equalsIgnoreCase(elem.tagName, "form")) ? elem :
                    ( this.html5FormDetection(elem) || this.getParent(elem, "form"));
        });

        if (finalElem) {
            var elemForm = fetchForm(finalElem);
            if (elemForm) return elemForm;
        }

        /**
         * name check
         */
        var foundElements = [];
        var name = (_Lang.isString(elem)) ? elem : elem.name;
        //id detection did not work
        if (!name) return null;
        /**
         * the lesser chance is the elements which have the same name
         * (which is the more likely case in case of a brute dom replacement)
         */
        var nameElems = document.getElementsByName(name);
        if (nameElems) {
            for (var cnt = 0; cnt < nameElems.length && foundElements.length < 2; cnt++) {
                // we already have covered the identifier case hence we only can deal with names,
                var foundForm = fetchForm(nameElems[cnt]);
                if (foundForm) {
                    foundElements.push(foundForm);
                }
            }
        }

        return (1 == foundElements.length ) ? foundElements[0] : null;
    },

    html5FormDetection: function(/*item*/) {
        return null;
    },


    /**
     * gets a parent of an item with a given tagname
     * @param {Node} item - child element
     * @param {String} tagName - TagName of parent element
     */
    getParent : function(item, tagName) {

        if (!item) {
            throw this._Lang.makeException(new Error(), null, null, this._nameSpace, "getParent",
                    this._Lang.getMessage("ERR_MUST_BE_PROVIDED1", null, "_Dom.getParent", "item {DomNode}"));
        }

        var _Lang = this._Lang;
        var searchClosure = function(parentItem) {
            return parentItem && parentItem.tagName
                    && _Lang.equalsIgnoreCase(parentItem.tagName, tagName);
        };
        try {
            return this.getFilteredParent(item, searchClosure);
        } finally {
            searchClosure = null;
            _Lang = null;
        }
    },

    /**
     * A parent walker which uses
     * a filter closure for filtering
     *
     * @param {Node} item the root item to ascend from
     * @param {function} filter the filter closure
     */
    getFilteredParent : function(item, filter) {
        this._assertStdParams(item, filter, "getFilteredParent", ["item", "filter"]);

        //search parent tag parentName
        var parentItem = (item.parentNode) ? item.parentNode : null;

        while (parentItem && !filter(parentItem)) {
            parentItem = parentItem.parentNode;
        }
        return (parentItem) ? parentItem : null;
    },

    /**
     * cross ported from dojo
     * fetches an attribute from a node
     *
     * @param {String} node the node
     * @param {String} attr the attribute
     * @return the attributes value or null
     */
    getAttribute : function(/* HTMLElement */node, /* string */attr) {
        return node.getAttribute(attr);
    },

    /**
     * checks whether the given node has an attribute attached
     *
     * @param {String|Object} node the node to search for
     * @param {String} attr the attribute to search for
     * @true if the attribute was found
     */
    hasAttribute : function(/* HTMLElement */node, /* string */attr) {
        //	summary
        //	Determines whether or not the specified node carries a value for the attribute in question.
        return this.getAttribute(node, attr) ? true : false;	//	boolean
    },

    /**
     * concatenation routine which concats all childnodes of a node which
     * contains a set of CDATA blocks to one big string
     * @param {Node} node the node to concat its blocks for
     */
    concatCDATABlocks : function(/*Node*/ node) {
        var cDataBlock = [];
        // response may contain several blocks
        for (var i = 0; i < node.childNodes.length; i++) {
            cDataBlock.push(node.childNodes[i].data);
        }
        return cDataBlock.join('');
    },

    //all modern browsers evaluate the scripts
    //manually this is a w3d recommendation
    isManualScriptEval: function() {
        return true;
    },

    /**
     * jsf2.2
     * checks if there is a fileupload element within
     * the executes list
     *
     * @param executes the executes list
     * @return {Boolean} true if there is a fileupload element
     */
    isMultipartCandidate:function (executes) {
        if (this._Lang.isString(executes)) {
            executes = this._Lang.strToArray(executes, /\s+/);
        }

        for (var cnt = 0, len = executes.length; cnt < len ; cnt ++) {
            var element = this.byId(executes[cnt]);
            var inputs = this.findByTagName(element, "input", true);
            for (var cnt2 = 0, len2 = inputs.length; cnt2 < len2 ; cnt2++) {
                if (this.getAttribute(inputs[cnt2], "type") == "file") return true;
            }
        }
        return false;
    },

    insertFirst: function(newNode) {
        var body = document.body;
        if (body.childNodes.length > 0) {
            body.insertBefore(newNode, body.firstChild);
        } else {
            body.appendChild(newNode);
        }
    },

    byId: function(id) {
        return this._Lang.byId(id);
    },

    getDummyPlaceHolder: function() {
        this._dummyPlaceHolder = this._dummyPlaceHolder ||this.createElement("div");
        return this._dummyPlaceHolder;
    },

    getNamedElementFromForm: function(form, elementId) {
        return form[elementId];
    }
});



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
if (_MF_SINGLTN) {
    _MF_SINGLTN(_PFX_UTIL + "_DomExperimental", myfaces._impl._util._Dom, /** @lends myfaces._impl._util._Dom.prototype */ {
        constructor_:function () {
            this._callSuper("constructor_");
            myfaces._impl._util._Dom = this;
        },

       html5FormDetection:function (item) {
            var browser = this._RT.browser;
            //ie shortcut, not really needed but speeds things up
            if (browser.isIEMobile && browser.isIEMobile <= 8) {
                return null;
            }
            var elemForm = this.getAttribute(item, "form");
            return (elemForm) ? this.byId(elemForm) : null;
        },

        getNamedElementFromForm: function(form, elementId) {
            return form[elementId];
        }
    });
}

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
if (_MF_SINGLTN) {
    /***
     Dom.js version for non html5 browsers
     */
    _MF_SINGLTN(_PFX_UTIL + "DomQuirks", myfaces._impl._util._Dom, /** @lends myfaces._impl._util._DomQuirks.prototype */ {

        IE_QUIRKS_EVENTS:{
            "onabort":true,
            "onload":true,
            "onunload":true,
            "onchange":true,
            "onsubmit":true,
            "onreset":true,
            "onselect":true,
            "onblur":true,
            "onfocus":true,
            "onkeydown":true,
            "onkeypress":true,
            "onkeyup":true,
            "onclick":true,
            "ondblclick":true,
            "onmousedown":true,
            "onmousemove":true,
            "onmouseout":true,
            "onmouseover":true,
            "onmouseup":true
        },

        constructor_:function () {

            var b = myfaces._impl.core._Runtime.browser;

            if (b.isIEMobile && b.isIE <= 6) {
                //winmobile hates add onLoad, and checks on the construct
                //it does not eval scripts anyway
                myfaces.config = myfaces.config || {};
                myfaces.config._autoeval = false;
                return;
            } else {
                //for whatever reason autoeval is set here on Firefox 3.5 only
                // could be a firebug problem, by setting setters
                //and getters no assignment was revealed.
                if ('undefined' != typeof myfaces.config && 'undefined' != typeof myfaces.config._autoeval) {
                    delete myfaces.config._autoeval;
                }
            }
            this._callSuper("constructor_");
            myfaces._impl._util._Dom = this;

            var _Lang = this._Lang;
            //by w3c spec the script eval is turned off it is only
            //on on legacy browsers
            var scriptEval = _Lang.hitch(this, this.isManualScriptEval);

            if (window) {
                this._RT.addOnLoad(window, scriptEval);
            }
            //safety fallback if the window onload handler is overwritten and not chained
            if (document.body) {
                this._RT.addOnLoad(document.body, scriptEval);
            }

            //we register ourselves into the existing classes lazily
            var _T = this;
            if (myfaces._impl.core.Impl) {
                this._RT.iterateClasses(function (proto) {
                    if (proto._Dom) proto._Dom = _T;
                });
            }
        },

        /**
         * Checks whether the browser is dom compliant.
         * Dom compliant means that it performs the basic dom operations safely
         * without leaking and also is able to perform a native setAttribute
         * operation without freaking out
         *
         *
         * Not dom compliant browsers are all microsoft browsers in quirks mode
         * and ie6 and ie7 to some degree in standards mode
         * and pretty much every browser who cannot create ranges
         * (older mobile browsers etc...)
         *
         * We dont do a full browser detection here because it probably is safer
         * to test for existing features to make an assumption about the
         * browsers capabilities
         */
        isDomCompliant:function () {
            if ('undefined' == typeof this._isCompliantBrowser) {
                this._isCompliantBrowser = !!((window.Range
                        && typeof Range.prototype.createContextualFragment == 'function')
                                                            //createContextualFragment hints to a no quirks browser but we need more fallbacks
                        || document.querySelectoryAll       //query selector all hints to html5 capabilities
                        || document.createTreeWalker);      //treewalker is either firefox 3.5+ or ie9 standards mode
            }
            return this._isCompliantBrowser;
        },

        /**
         * now to the evil browsers
         * of what we are dealing with is various bugs
         * first a simple replaceElement leaks memory
         * secondly embedded scripts can be swallowed upon
         * innerHTML, we probably could also use direct outerHTML
         * but then we would run into the script swallow bug
         *
         * the entire mess is called IE6 and IE7
         *
         * @param item
         * @param markup
         */
        _outerHTMLNonCompliant:function (item, markup) {

            var b = this._RT.browser;

            try {
                //check for a subtable rendering case

                var evalNodes = this._buildEvalNodes(item, markup);

                if (evalNodes.length == 1) {
                    var ret = evalNodes[0];
                    this.replaceElement(item, evalNodes[0]);
                    return ret;
                } else {
                    return this.replaceElements(item, evalNodes);
                }

            } finally {

                var dummyPlaceHolder = this.getDummyPlaceHolder();
                //now that Microsoft has finally given
                //ie a working gc in 8 we can skip the costly operation
                if (b.isIE && b.isIE < 8) {
                    this._removeChildNodes(dummyPlaceHolder, false);
                }
                dummyPlaceHolder.innerHTML = "";
            }

        },

        replaceElement:function (item, evalNode) {

            var _Browser = this._RT.browser;
            if (!_Browser.isIE || _Browser.isIE >= 8) {
                //standards conform no leaking browser
                item.parentNode.replaceChild(evalNode, item);
            } else {
                this._callSuper("replaceElement", item, evalNode);
            }
        },

        /**
         * builds the ie nodes properly in a placeholder
         * and bypasses a non script insert bug that way
         * @param markup the marku code
         */
        _buildNodesNonCompliant:function (markup) {

            //now to the non w3c compliant browsers
            //http://blogs.perl.org/users/clinton_gormley/2010/02/forcing-ie-to-accept-script-tags-in-innerhtml.html
            //we have to cope with deficiencies between ie and its simulations in this case
            var probe = this.getDummyPlaceHolder();//document.createElement("div");

            probe.innerHTML = "<table><tbody><tr><td><div></div></td></tr></tbody></table>";

            //we have customers using html unit, this has a bug in the table resolution
            //hence we determine the depth dynamically
            var depth = this._determineDepth(probe);

            this._removeChildNodes(probe, false);
            probe.innerHTML = "";

            var dummyPlaceHolder = this.getDummyPlaceHolder();//document.createElement("div");

            //fortunately a table element also works which is less critical than form elements regarding
            //the inner content
            dummyPlaceHolder.innerHTML = "<table><tbody><tr><td>" + markup + "</td></tr></tbody></table>";
            var evalNodes = dummyPlaceHolder;

            for (var cnt = 0; cnt < depth; cnt++) {
                evalNodes = evalNodes.childNodes[0];
            }
            var ret = (evalNodes.parentNode) ? this.detach(evalNodes.parentNode.childNodes) : null;

            if ('undefined' == typeof evalNodes || null == evalNodes) {
                //fallback for htmlunit which should be good enough
                //to run the tests, maybe we have to wrap it as well
                dummyPlaceHolder.innerHTML = "<div>" + markup + "</div>";
                //note this is triggered only in htmlunit no other browser
                //so we are save here
                //TODO Fix this (probaby ret is needed here, check the history)
                evalNodes = this.detach(dummyPlaceHolder.childNodes[0].childNodes);
            }

            this._removeChildNodes(dummyPlaceHolder, false);
            //ie fix any version, ie does not return true javascript arrays so we have to perform
            //a cross conversion
            return ret;

        },

        _determineDepth:function (probe) {
            var depth = 0;
            var newProbe = probe;
            for (; newProbe &&
                           newProbe.childNodes &&
                           newProbe.childNodes.length &&
                           newProbe.nodeType == 1; depth++) {
                newProbe = newProbe.childNodes[0];
            }
            return depth;
        },

        //now to another nasty issue:
        //for ie we have to walk recursively over all nodes:
        //http://msdn.microsoft.com/en-us/library/bb250448%28VS.85%29.aspx
        //http://weblogs.java.net/blog/driscoll/archive/2009/11/13/ie-memory-management-and-you
        //http://home.orange.nl/jsrosman/
        //http://www.quirksmode.org/blog/archives/2005/10/memory_leaks_li.html
        //http://www.josh-davis.org/node/7
        _removeNode:function (node, breakEventsOpen) {
            if (!node) return;
            var b = this._RT.browser;
            if (this.isDomCompliant()) {
                //recursive descension only needed for old ie versions
                //all newer browsers cleanup the garbage just fine without it
                //thank you
                if ('undefined' != typeof node.parentNode && null != node.parentNode) //if the node has a parent
                    node.parentNode.removeChild(node);
                return;
            }

            //now to the browsers with non working garbage collection
            this._removeChildNodes(node, breakEventsOpen);

            try {
                //outer HTML setting is only possible in earlier IE versions all modern browsers throw an exception here
                //again to speed things up we precheck first
                if (!this._isTableElement(node)) {
                    //we do not do a table structure innnerhtml on table elements except td
                    //htmlunit rightfully complains that we should not do it
                    node.innerHTML = "";
                }
                if (b.isIE && 'undefined' != typeof node.outerHTML) {//ie8+ check done earlier we skip it here
                    node.outerHTML = '';
                } else {
                    node = this.detach(node)[0];
                }
                if (!b.isIEMobile) {
                    delete node;
                }
            } catch (e) {
                //on some elements we might not have covered by our table check on the outerHTML
                // can fail we skip those in favor of stability
                try {
                    // both innerHTML and outerHTML fails when <tr> is the node, but in that case
                    // we need to force node removal, otherwise it will be on the tree (IE 7 IE 6)
                    this.detach(node);
                    if (!b.isIEMobile) {
                        delete node;
                    }
                } catch (e1) {
                }
            }
        },

        /**
         * recursive delete child nodes
         * node, this method only makes sense in the context of IE6 + 7 hence
         * it is not exposed to the public API, modern browsers
         * can garbage collect the nodes just fine by doing the standard removeNode method
         * from the dom API!
         *
         * @param node  the node from which the childnodes have to be deletd
         * @param breakEventsOpen if set to true a standard events breaking is performed
         */
        _removeChildNodes:function (node, breakEventsOpen) {
            if (!node) return;

            //node types which cannot be cleared up by normal means
            var disallowedNodes = this.TABLE_ELEMS;

            //for now we do not enable it due to speed reasons
            //normally the framework has to do some event detection
            //which we cannot do yet, I will dig for options
            //to enable it in a speedly manner
            //ie7 fixes this area anyway
            //this.breakEvents(node);

            var b = this._RT.browser;
            if (breakEventsOpen) {
                this.breakEvents(node);
            }

            for (var cnt = node.childNodes.length - 1; cnt >= 0; cnt -= 1) {
                var childNode = node.childNodes[cnt];
                //we cannot use our generic recursive tree walking due to the needed head recursion
                //to clean it up bottom up, the tail recursion we were using in the search either would use more time
                //because we had to walk and then clean bottom up, so we are going for a direct head recusion here
                if ('undefined' != typeof childNode.childNodes && node.childNodes.length)
                    this._removeChildNodes(childNode);
                try {
                    var nodeName = (childNode.nodeName || childNode.tagName) ? (childNode.nodeName || childNode.tagName).toLowerCase() : null;
                    //ie chokes on clearing out table inner elements, this is also covered by our empty
                    //catch block, but to speed things up it makes more sense to precheck that
                    if (!disallowedNodes[nodeName]) {
                        //outer HTML setting is only possible in earlier IE versions all modern browsers throw an exception here
                        //again to speed things up we precheck first
                        if (!this._isTableElement(childNode)) {    //table elements cannot be deleted
                            childNode.innerHTML = "";
                        }
                        if (b.isIE && b.isIE < 8 && 'undefined' != typeof childNode.outerHTML) {
                            childNode.outerHTML = '';
                        } else {
                            node.removeChild(childNode);
                        }
                        if (!b.isIEMobile) {
                            delete childNode;
                        }
                    }
                } catch (e) {
                    //on some elements the outerHTML can fail we skip those in favor
                    //of stability

                }
            }
        },
        /**
         * cross ported from dojo
         * fetches an attribute from a node
         *
         * @param {String} node the node
         * @param {String} attr the attribute
         * @return the attributes value or null
         */
        getAttribute:function (/* HTMLElement */node, /* string */attr) {
            //	summary
            //	Returns the value of attribute attr from node.
            node = this.byId(node);
            // FIXME: need to add support for attr-specific accessors
            if ((!node) || (!node.getAttribute)) {
                // if(attr !== 'nwType'){
                //	alert("getAttr of '" + attr + "' with bad node");
                // }
                return null;
            }
            var ta = typeof attr == 'string' ? attr : new String(attr);

            // first try the approach most likely to succeed
            var v = node.getAttribute(ta.toUpperCase());
            if ((v) && (typeof v == 'string') && (v != "")) {
                return v;	//	string
            }

            // try returning the attributes value, if we couldn't get it as a string
            if (v && v.value) {
                return v.value;	//	string
            }

            // this should work on Opera 7, but it's a little on the crashy side
            if ((node.getAttributeNode) && (node.getAttributeNode(ta))) {
                return (node.getAttributeNode(ta)).value;	//	string
            } else if (node.getAttribute(ta)) {
                return node.getAttribute(ta);	//	string
            } else if (node.getAttribute(ta.toLowerCase())) {
                return node.getAttribute(ta.toLowerCase());	//	string
            }
            return null;	//	string
        },

        /**
         * bugfixing for ie6 which does not cope properly with setAttribute
         */
        setAttribute:function (node, attr, val) {

            this._assertStdParams(node, attr, "setAttribute");

            //quirks mode and ie7 mode has the attributes problems ie8 standards mode behaves like
            //a good citizen
            var _Browser = this._RT.browser;
            //in case of ie > ie7 we have to check for a quirks mode setting
            if (!_Browser.isIE || _Browser.isIE > 7 && this.isDomCompliant()) {
                this._callSuper("setAttribute", node, attr, val);
                return;
            }

            /*
             Now to the broken browsers IE6+.... ie7 and ie8 quirks mode

             we deal mainly with three problems here
             class and for are not handled correctly
             styles are arrays and cannot be set directly
             and javascript events cannot be set via setAttribute as well!

             or in original words of quirksmode.org ... this is a mess!

             Btw. thank you Microsoft for providing all necessary tools for free
             for being able to debug this entire mess in the ie rendering engine out
             (which is the Microsoft ie vms, developers toolbar, Visual Web Developer 2008 express
             and the ie8 8 developers toolset!)

             also thank you http://www.quirksmode.org/
             dojotoolkit.org and   //http://delete.me.uk/2004/09/ieproto.html
             for additional information on this mess!

             The lowest common denominator tested within this code
             is IE6, older browsers for now are legacy!
             */
            attr = attr.toLowerCase();

            if (attr === "class") {
                //setAttribute does not work for winmobile browsers
                //firect calls work
                node.className = val;
            } else if (attr === "name") {
                //the ie debugger fails to assign the name via setAttr
                //in quirks mode
                node[attr] = val;
            } else if (attr === "for") {
                if (!_Browser.isIEMobile || _Browser.isIEMobile >= 7) {
                    node.setAttribute("htmlFor", val);
                } else {
                    node.htmlFor = val;
                }
            } else if (attr === "style") {
                node.style.cssText = val;
            } else {
                //check if the attribute is an event, since this applies only
                //to quirks mode of ie anyway we can live with the standard html4/xhtml
                //ie supported events
                if (this.IE_QUIRKS_EVENTS[attr]) {
                    if (this._Lang.isString(attr)) {
                        var c = document.body.appendChild(document.createElement('span'));
                        try {
                            c.innerHTML = '<span ' + attr + '="' + val + '"/>';
                            node[attr] = c.firstChild[attr];
                        } finally {
                            document.body.removeChild(c);
                        }
                    }
                } else {
                    //unknown cases we try to catch them via standard setAttributes
                    if (!_Browser.isIEMobile || _Browser.isIEMobile >= 7) {
                        node.setAttribute(attr, val);
                    } else {
                        node[attr] = val;
                    }
                }
            }
        },

        getDummyPlaceHolder:function () {
            this._callSuper("getDummyPlaceHolder");

            //ieMobile in its 6.1-- incarnation cannot handle innerHTML detached objects so we have
            //to attach the dummy placeholder, we try to avoid it for
            //better browsers so that we do not have unecessary dom operations
            if (this._RT.browser.isIEMobile && created) {
                this.insertFirst(this._dummyPlaceHolder);

                this.setAttribute(this._dummyPlaceHolder, "style", "display: none");

            }

            return this._dummyPlaceHolder;
        },

        /**
         * classical recursive way which definitely will work on all browsers
         * including the IE6
         *
         * @param rootNode the root node
         * @param filter the filter to be applied to
         * @param deepScan if set to true a deep scan is performed
         */
        _recursionSearchAll:function (rootNode, filter, deepScan) {
            var ret = [];
            //fix the value to prevent undefined errors

            if (filter(rootNode)) {
                ret.push(rootNode);
                if (!deepScan) return ret;
            }

            //
            if (!rootNode.childNodes) {
                return ret;
            }

            //subfragment usecases

            var retLen = ret.length;
            var childLen = rootNode.childNodes.length;
            for (var cnt = 0; (deepScan || retLen == 0) && cnt < childLen; cnt++) {
                ret = ret.concat(this._recursionSearchAll(rootNode.childNodes[cnt], filter, deepScan));
            }
            return ret;
        },

        /**
         * break the standard events from an existing dom node
         * (note this method is not yet used, but can be used
         * by framework authors to get rid of ie circular event references)
         *
         * another way probably would be to check all attributes of a node
         * for a function and if one is present break it by nulling it
         * I have to do some further investigation on this.
         *
         * The final fix is to move away from ie6 at all which is the root cause of
         * this.
         *
         * @param node the node which has to be broken off its events
         */
        breakEvents:function (node) {
            if (!node) return;
            var evtArr = this.IE_QUIRKS_EVENTS;
            for (var key in evtArr) {
                if (!evtArr.hasOwnProperty(key)) continue;
                if (key != "onunload" && node[key]) {
                    node[key] = null;
                }
            }
        },

        isManualScriptEval:function () {

            if (!this._Lang.exists(myfaces, "config._autoeval")) {

                //now we rely on the document being processed if called for the first time
                var evalDiv = document.createElement("div");
                this._Lang.reserveNamespace("myfaces.config._autoeval");
                //null not swallowed
                myfaces.config._autoeval = false;

                var markup = "<script type='text/javascript'> myfaces.config._autoeval = true; </script>";
                //now we rely on the same replacement mechanisms as outerhtml because
                //some browsers have different behavior of embedded scripts in the contextualfragment
                //or innerhtml case (opera for instance), this way we make sure the
                //eval detection is covered correctly
                this.setAttribute(evalDiv, "style", "display:none");

                //it is less critical in some browsers (old ie versions)
                //to append as first element than as last
                //it should not make any difference layoutwise since we are on display none anyway.
                this.insertFirst(evalDiv);

                //we remap it into a real boolean value
                if (this.isDomCompliant()) {
                    this._outerHTMLCompliant(evalDiv, markup);
                } else {
                    //will not be called placeholder for quirks class
                    this._outerHTMLNonCompliant(evalDiv, markup);
                }


            }

            return  !myfaces.config._autoeval;
        },

        getNamedElementFromForm:function (form, elementName) {
			var browser = this._RT.browser;
            if(browser.isIE && browser.isIE < 8) {
                if(!form.elements) return null;
                for(var cnt = 0, l = form.elements.length; cnt < l; cnt ++) {
                    var element = form.elements[cnt];
                    if(element.name == elementName) {
                        return element;
                    }
                }
                return null;
            } else {
                return form[elementName];
            }
        }

    });

}

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
 * @name _HtmlStripper
 * @memberOf myfaces._impl._util
 * @extends myfaces._impl.core._Runtime
 * @description
 *  Fallback routine if the browser embedded xml parser fails on the document
 *  This fallback is not failsafe but should give enough cover to handle all cases
 */

/** @namespace myfaces._impl._util._HtmlStripper */
_MF_CLS(_PFX_UTIL + "_HtmlStripper", _MF_OBJECT, /** @lends myfaces._impl._util._HtmlStripper.prototype */ {

    /**
     * main parse routine parses the document for a given tag name
     *
     *
     * @param theString  the markup string to be parsed
     * @param tagNameStart the tag name to be parsed for
     */
    parse : function(theString, tagNameStart) {

        var BEGIN_TAG = "html",
                _tagStart = -1,
                _tagEnd = -1,
                _contentStart = -1,
                _contentEnd = -1,
                _tokenPos = 0,
                _tokenForward = 1,
                tagNameStart = (!tagNameStart) ? BEGIN_TAG : tagNameStart;

        var proposedTagStartPos = theString.indexOf("<" + tagNameStart);
        var _T = this;

        //we use closures instead of private methods to improve the compressability

        var isValidPositionCombination = function(pos1, pos2, pos3, pos4) {
            return pos1 <= pos2 && pos3 <= pos4;
        };

        /**
         * trace for a forward comment
         *
         * @param theStr the string to be checked
         * @param tagPos the tag position from which the check onwards has to be perfomed
         * @return true in case a comment is found
         */
        var checkForwardForComment = function(theStr, tagPos) {
            var toCheck = theStr.substring(tagPos),
                    indexOf = _T._Lang.hitch(toCheck, toCheck.indexOf),
                    firstBeginComment = indexOf("<!--"),
                    firstEndComment = indexOf("-->"),

                    firstBeginCDATA = indexOf("<[CDATA["),
                    firstEndCDATA = indexOf("]]>");

            if (isValidPositionCombination(firstBeginComment, firstEndComment, firstBeginCDATA, firstEndCDATA)) {
                return true;
            }

            return firstBeginComment <= firstEndComment && firstBeginCDATA <= firstEndCDATA;
        };

        /**
         * check backwards for a comment
         *
         * @param theStr the check string
         * @param tagPos the tag position from which the check should be performed
         * @return true in case a comment is found
         */
        var checkBackForComment = function(theStr, tagPos) {
            var toCheck = theStr.substring(tagPos),
                    indexOf = _T._Lang.hitch(toCheck, toCheck.indexOf),
                    lastBeginComment = indexOf("<!--"),
                    lastEndComment = indexOf("-->"),
                    lastBeginCDATA = indexOf("<[CDATA["),
                    lastEndCDATA = indexOf("]]>");

            if (isValidPositionCombination(lastBeginComment, lastEndComment, lastBeginCDATA, lastEndCDATA)) {
                //TODO we have to handle the embedded cases, for now we leave them out
                return true;
            }
        };

        //no need for ll parsing a handful of indexofs instead of slower regepx suffices
        var theSubStr = this._Lang.hitch(theString, theString.substring);
        while (_contentStart == -1 && proposedTagStartPos != -1) {
            if (checkBackForComment(theString, proposedTagStartPos)) {
                _tagStart = proposedTagStartPos;
                _contentStart = proposedTagStartPos + theSubStr(proposedTagStartPos).indexOf(">") + 1;
            }
            proposedTagStartPos = theSubStr(proposedTagStartPos + tagNameStart.length + 2).indexOf("<" + tagNameStart);
        }

        var proposedEndTagPos = theString.lastIndexOf("</" + tagNameStart);
        while (_contentEnd == -1 && proposedEndTagPos > 0) {
            if (checkForwardForComment(theString, proposedEndTagPos)) {
                _tagEnd = proposedEndTagPos;
                _contentEnd = proposedEndTagPos;
            }
            proposedTagStartPos = theSubStr(proposedTagStartPos - tagNameStart.length - 2).lastIndexOf("</" + tagNameStart);
        }
        if (_contentStart != -1 && _contentEnd != -1) {
            return theSubStr(_contentStart, _contentEnd);
        }
        return null;
    }
});




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
 * legacy code to enable various aspects
 * of myfaces, used to be rendered inline
 * for jsf 2.0 we can externalized it into its own custom resource
 */

(!window.myfaces) ? window.myfaces = {} : null;
if (!myfaces.oam) {
    myfaces.oam = new function() {

        /**
         * sets a hidden input field
         * @param formname the formName
         * @param name the hidden field
         * @param value the value to be rendered
         */
        this.setHiddenInput = function(formname, name, value) {
            var form = document.forms[formname];
            if (typeof form == 'undefined') {
                form = document.getElementById(formname);
            }
    
            if (typeof form.elements[name] != 'undefined' && (form.elements[name].nodeName == 'INPUT' || form.elements[name].nodeName == 'input')) {
                form.elements[name].value = value;
            }
            else {
                var newInput = document.createElement('input');
                newInput.setAttribute('type', 'hidden');
                newInput.setAttribute('id', name);
                newInput.setAttribute('name', name);
                newInput.setAttribute('value', value);
                form.appendChild(newInput);
            }
        };
        
        /**
         * clears a hidden input field
         *
         * @param formname formName for the input
         * @param name the name of the input field
         * @param value the value to be cleared
         */
        this.clearHiddenInput = function(formname, name, value) {
            var form = document.forms[formname];
    
            if (typeof form == 'undefined') {
                form = document.getElementById(formname);
            }
    
            var hInput = form.elements[name];
            if (typeof hInput != 'undefined') {
                form.removeChild(hInput);
            }
        };
        
        /**
         * does special form submit remapping
         * remaps the issuing command link into something
         * the decode of the command link on the server can understand
         *
         * @param formName
         * @param linkId
         * @param target
         * @param params
         */
        this.submitForm = function(formName, linkId, target, params) {
    
            var clearFn = 'clearFormHiddenParams_' + formName.replace(/-/g, '\$:').replace(/:/g, '_');
            if (typeof window[clearFn] == 'function') {
                window[clearFn](formName);
            }
    
            var form = document.forms[formName];
            if (typeof form == 'undefined') {
                form = document.getElementById(formName);
            }
    
            //autoscroll code
            if (myfaces.core.config.autoScroll && typeof window.getScrolling != 'undefined') {
                myfaces.oam.setHiddenInput(formName, 'autoScroll', getScrolling());
            }
    
            if (myfaces.core.config.ieAutoSave) {
                var agentString = navigator.userAgent.toLowerCase();
                var version = navigator.appVersion;
                if (agentString.indexOf('msie') != -1) {
                    if (!(agentString.indexOf('ppc') != -1 && agentString.indexOf('windows ce') != -1 && version >= 4.0)) {
                        window.external.AutoCompleteSaveForm(form);
                    }
                }
            }
    
            var oldTarget = form.target;
            if (target != null) {
                form.target = target;
            }
            if ((typeof params != 'undefined') && params != null) {
                for (var i = 0, param; (param = params[i]); i++) {
                    myfaces.oam.setHiddenInput(formName, param[0], param[1]);
                }
    
            }
    
            myfaces.oam.setHiddenInput(formName, formName + ':' + '_idcl', linkId);
    
            if (form.onsubmit) {
                var result = form.onsubmit();
                if ((typeof result == 'undefined') || result) {
                    try {
                        form.submit();
                    }
                    catch(e) {
                    }
                }
    
            }
            else {
                try {
                    form.submit();
                }
                catch(e) {
                }
            }
    
            form.target = oldTarget;
            if ((typeof params != 'undefined') && params != null) {
    
                for (var i = 0, param; (param = params[i]); i++) {
                    myfaces.oam.clearHiddenInput(formName, param[0], param[1]);
                }
    
            }
    
            myfaces.oam.clearHiddenInput(formName, formName + ':' + '_idcl', linkId);
            return false;
        };
    }
}

//reserve a cofig namespace for impl related stuff
(!myfaces.core) ? myfaces.core = {} : null;
(!myfaces.core.config) ? myfaces.core.config = {} : null;


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
 * @name _AjaxUtils
 * @memberOf myfaces._impl.xhrCore
 * @description
 *
 * A set of helper routines which are utilized within our Ajax subsystem and nowhere else
 *
 * TODO move this into a singleton, the current structure is
 * still a j4fry legacy we need to get rid of it in the long run
 */
_MF_SINGLTN(_PFX_XHR+"_AjaxUtils", _MF_OBJECT,
/** @lends myfaces._impl.xhrCore._AjaxUtils.prototype */
{


    /**
     * determines fields to submit
     * @param {Object} targetBuf - the target form buffer receiving the data
     * @param {Node} parentItem - form element item is nested in
     * @param {Array} partialIds - ids fo PPS
     */
    encodeSubmittableFields : function(targetBuf,
                                       parentItem, partialIds) {
            if (!parentItem) throw "NO_PARITEM";
            if (partialIds ) {
                this.encodePartialSubmit(parentItem, false, partialIds, targetBuf);
            } else {
                // add all nodes
                var eLen = parentItem.elements.length;
                for (var e = 0; e < eLen; e++) {
                    this.encodeElement(parentItem.elements[e], targetBuf);
                } // end of for (formElements)
            }

    },

     /**
     * appends the issuing item if not given already
     * @param item
     * @param targetBuf
     */
    appendIssuingItem: function (item, targetBuf) {
        // if triggered by a Button send it along
        if (item && item.type && item.type.toLowerCase() == "submit") {
            targetBuf.append(item.name, item.value);
        }
    },


    /**
     * encodes a single input element for submission
     *
     * @param {Node} element - to be encoded
     * @param {} targetBuf - a target array buffer receiving the encoded strings
     */
    encodeElement : function(element, targetBuf) {

        //browser behavior no element name no encoding (normal submit fails in that case)
        //https://issues.apache.org/jira/browse/MYFACES-2847
        if (!element.name) {
            return;
        }

        var _RT = this._RT;
        var name = element.name;
        var tagName = element.tagName.toLowerCase();
        var elemType = element.type;
        if (elemType != null) {
            elemType = elemType.toLowerCase();
        }

        // routine for all elements
        // rules:
        // - process only inputs, textareas and selects
        // - elements muest have attribute "name"
        // - elements must not be disabled
        if (((tagName == "input" || tagName == "textarea" || tagName == "select") &&
                (name != null && name != "")) && !element.disabled) {

            // routine for select elements
            // rules:
            // - if select-one and value-Attribute exist => "name=value"
            // (also if value empty => "name=")
            // - if select-one and value-Attribute don't exist =>
            // "name=DisplayValue"
            // - if select multi and multple selected => "name=value1&name=value2"
            // - if select and selectedIndex=-1 don't submit
            if (tagName == "select") {
                // selectedIndex must be >= 0 sein to be submittet
                if (element.selectedIndex >= 0) {
                    var uLen = element.options.length;
                    for (var u = 0; u < uLen; u++) {
                        // find all selected options
                        //var subBuf = [];
                        if (element.options[u].selected) {
                            var elementOption = element.options[u];
                            targetBuf.append(name, (elementOption.getAttribute("value") != null) ?
                                    elementOption.value : elementOption.text);
                        }
                    }
                }
            }

            // routine for remaining elements
            // rules:
            // - don't submit no selects (processed above), buttons, reset buttons, submit buttons,
            // - submit checkboxes and radio inputs only if checked
            if ((tagName != "select" && elemType != "button"
                    && elemType != "reset" && elemType != "submit" && elemType != "image")
                    && ((elemType != "checkbox" && elemType != "radio") || element.checked)) {
                if ('undefined' != typeof element.files && element.files != null && _RT.getXHRLvl() >= 2 && element.files.length) {
                    //xhr level2
                    targetBuf.append(name, element.files[0]);
                } else {
                    targetBuf.append(name, element.value);
                }
            }

        }
    }
});
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
/** @namespace myfaces._impl.xhrCore._AjaxRequestQueue */
_MF_CLS(_PFX_XHR + "_AjaxRequestQueue", myfaces._impl._util._Queue, /** @lends  myfaces._impl.xhrCore._AjaxRequestQueue.prototype */ {

    /**
     * a pointer towards the currently processed
     * request in our queue
     */
    _curReq : null,

    /**
     * delay request, then call enqueue
     * @param {Object} request (myfaces._impl.xhrCore._AjaxRequest) request to send
     */
    enqueue : function(request) {

        if (this._curReq == null) {
            this._curReq = request;
            this._curReq.send();
        } else {
            this._callSuper("enqueue", request);
            if (request._queueSize != this._size) {
                this.setQueueSize(request._queueSize);
            }
        }

    },

    /**
     * process queue, send request, if exists
     */
    processQueue: function() {
        this._curReq = this.dequeue();
        if (this._curReq) {
            this._curReq.send();
        }
    },

    /**
     * cleanup queue
     */
    cleanup: function() {
        this._curReq = null;
        this._callSuper("cleanup");
    }
});


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
 * @memberOf myfaces._impl.xhrCore
 * @namespace
 * @name engine
 */

/**
 * @class
 * @name XhrConst
 * @memberOf myfaces._impl.xhrCore.engine
 */
_MF_SINGLTN(_PFX_XHR+"engine.XhrConst", Object,
        /** @lends myfaces._impl.xhrCore.engine.XhrConst.prototype */
        {
            READY_STATE_UNSENT:     0,
            READY_STATE_OPENED:     1,
            READY_STATE_HEADERS_RECEIVED: 2,
            READY_STATE_LOADING:    3,
            READY_STATE_DONE:       4,

            STATUS_OK_MINOR:        200,
            STATUS_OK_MAJOR:        300,

            constructor_: function() {
            }
        });
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
 * @name IFrame
 * @memberOf myfaces._impl.xhrCore.engine
 * @extends myfaces._impl.xhrCore.engine.BaseRequest
 * @description
 *
 * wrapper for an iframe transport object with all its differences
 * it emulates the xhr level2 api
 */
_MF_CLS(_PFX_XHR + "engine.IFrame", myfaces._impl.xhrCore.engine.BaseRequest,
        /** @lends myfaces._impl.xhrCore.engine.IFrame.prototype */
        {

            _finalized: false,

            /*the target frame responsible for the communication*/
            _frame: null,
            //_requestHeader: null,
            _aborted: false,

            CLS_NAME: "myfaces._impl.xhrCore._IFrameRequest",
            _FRAME_ID: "_mf_comm_frm",

            /**
             * constructor which shifts the arguments
             * to the protected properties of this clas
             *
             * @param arguments
             */
            constructor_: function (arguments) {
                //we fetch in the standard arguments

                this._callSuper("constructor", arguments);

                this._initDefaultFinalizableFields();
                //this._requestHeader = {};

                this._XHRConst = myfaces._impl.xhrCore.engine.XhrConst;

                this._Lang.applyArgs(this, arguments);
                this.readyState = this._XHRConst.READY_STATE_UNSENT;
                this._startTimeout();
            },

            setRequestHeader: function (key, value) {
                //this._requestHeader[key] = value;
            },

            open: function (method, url, async) {

                this.readyState = this._XHRConst.READY_STATE_OPENED;

                var _RT = myfaces._impl.core._Runtime;
                var _Lang = this._Lang;

                this._frame = this._createTransportFrame();

                //we append an onload handler to the frame
                //to cover the starting and loading events,
                //timeouts cannot be covered in a cross browser way

                //we point our onload handler to the frame, we do not use addOnLoad
                //because the frame should not have other onload handlers in place
                if (!_RT.browser.isIE || this._Dom.isDomCompliant()) {
                    this._frame.onload = _Lang.hitch(this, this._callback);
                } else {
                    //ie has a bug, onload is not settable outside of innerHTML on iframes
                    this._frame.onload_IE = _Lang.hitch(this, this._callback);
                }

                this.method = method || this.method;
                this.url = url || this.url;
                this.async = ('undefined' != typeof async) ? async : this.async;

                var myevt = {};
                this._addProgressAttributes(myevt, 10, 100);
                this.onprogress(myevt);
            },

            send: function (formData) {

                var myevt = {};
                this._addProgressAttributes(myevt, 20, 100);
                this.onloadstart(myevt);
                this.onprogress(myevt);

                var formDataForm = formData.form,
                        oldTarget = formDataForm.target,
                        oldMethod = formDataForm.method,
                        oldAction = formDataForm.action;

                try {
                    //_t._initAjaxParams();
                    //for (var key in this._requestHeader) {
                    //    formData.append(key, this._requestHeader[key]);
                    //}

                    formDataForm.target = this._frame.name;
                    formDataForm.method = this.method;
                    if (this.url) {
                        formDataForm.action = this.url;
                    }
                    this.readyState = this._XHRConst.READY_STATE_LOADING;
                    this.onreadystatechange(myevt);
                    formDataForm.submit();
                } finally {
                    formDataForm.action = oldAction;
                    formDataForm.target = oldTarget;
                    formDataForm.method = oldMethod;

                    formData._finalize();
                    //alert("finalizing");
                    this._finalized = true;
                }
            },

            /*we can implement it, but it will work only on browsers
             * which have asynchronous iframe loading*/
            abort: function () {
                this._aborted = true;
                this.onabort({});
            },

            _addProgressAttributes: function (evt, percent, total) {
                //http://www.w3.org/TR/progress-events/#progressevent
                evt.lengthComputable = true;
                evt.loaded = percent;
                evt.total = total;

            },

            _callback: function () {
                //------------------------------------
                // we are asynchronous which means we
                // have to check wether our code
                // is finalized or not
                //------------------------------------
                if (this._aborted) return;
                if (this._timeoutTimer) {
                    clearTimeout(this._timeoutTimer);
                }
                if (!this._finalized) {
                    setTimeout(this._Lang.hitch(this, this._callback), 10);
                    return;
                }
                //aborted no further processing

                try {
                    var myevt = {};

                    this._addProgressAttributes(myevt, 100, 100);
                    //this.readyState = this._XHRConst.READY_STATE_DONE;
                    //this.onreadystatechange(myevt);

                    this.responseText = this._getFrameText();
                    this.responseXML = this._getFrameXml();
                    this.readyState = this._XHRConst.READY_STATE_DONE;

                    //TODO status and statusText

                    this.onreadystatechange(myevt);
                    this.onloadend();

                    if (!this._Lang.isXMLParseError(this.responseXML)) {
                        this.status = 201;
                        this.onload();
                    } else {
                        this.status = 0;
                        //we simulate the request for our xhr call
                        this.onerror();
                    }

                } finally {
                    this._frame = null;
                }
            },

            /**
             * returns the frame text in a browser independend manner
             */
            _getFrameDocument: function () {

                //we cover various browsers here, because almost all browsers keep the document in a different
                //position
                return this._frame.contentWindow.document || this._frame.contentDocument || this._frame.document;
            },

            _getFrameText: function () {
                var framedoc = this._getFrameDocument();
                //also ie keeps the body in framedoc.body the rest in documentElement
                var body = framedoc.body || framedoc.documentElement;
                return  body.innerHTML;
            },

            _clearFrame: function () {

                var framedoc = this._getFrameDocument();
                var body = framedoc.documentElement || framedoc.body;
                //ie8 in 7 mode chokes on the innerHTML method
                //direct dom removal is less flakey and works
                //over all browsers, but is slower
                if (myfaces._impl.core._Runtime.browser.isIE) {
                    this._Dom._removeChildNodes(body, false);
                } else {
                    body.innerHTML = "";
                }
            },

            /**
             * returns the processed xml from the frame
             */
            _getFrameXml: function () {
                var framedoc = this._getFrameDocument();
                //same situation here, the xml is hosted either in xmlDocument or
                //is located directly under the frame document
                return  framedoc.XMLDocument || framedoc;
            },

            _createTransportFrame: function () {

                var _FRM_ID = this._FRAME_ID;
                var frame = document.getElementById(_FRM_ID);
                if (frame) return frame;
                //normally this code should not be called
                //but just to be sure

                if (this._Dom.isDomCompliant()) {
                    frame = this._Dom.createElement('iframe', {
                        "src": "about:blank",
                        "id": _FRM_ID,
                        "name": _FRM_ID,
                        "type": "content",
                        "collapsed": "true",
                        "style": "display:none"
                    });

                    //probably the ie method would work on all browsers
                    //but this code is the safe bet it works on all standards
                    //compliant browsers in a clean manner

                    document.body.appendChild(frame);
                } else { //Now to the non compliant browsers
                    var node = this._Dom.createElement("div", {
                        "style": "display:none"
                    });

                    //we are dealing with two well known iframe ie bugs here
                    //first the iframe has to be set via innerHTML to be present
                    //secondly the onload handler is immutable on ie, we have to
                    //use a dummy onload handler in this case and call this one
                    //from the onload handler
                    node.innerHTML = "<iframe id='" + _FRM_ID + "' name='" + _FRM_ID + "' style='display:none;' src='about:blank' type='content' onload='this.onload_IE();'  ></iframe>";

                    //avoid the ie open tag problem
                    var body = document.body;
                    if (body.firstChild) {
                        body.insertBefore(node, document.body.firstChild);
                    } else {
                        body.appendChild(node);
                    }
                }

                //helps to for the onload handlers and innerhtml to be in sync again
                return document.getElementById(_FRM_ID);
            },

            _startTimeout: function () {

                if (this.timeout == 0) return;
                this._timeoutTimer = setTimeout(this._Lang.hitch(this, function () {
                    if (this._xhrObject.readyState != this._XHRConst.READY_STATE_DONE) {

                        this._aborted = true;
                        clearTimeout(this._timeoutTimer);
                        //we cannot abort an iframe request
                        this.ontimeout({});
                        this._timeoutTimer = null;
                    }
                }), this.timeout);
            }
        });

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
 * An implementation of an xhr request object
 * with partial page submit functionality, and jsf
 * ppr request and timeout handling capabilities
 *
 * Author: Werner Punz (latest modification by $Author: ganeshpuri $)
 * Version: $Revision: 1.4 $ $Date: 2009/05/31 09:16:44 $
 */

/**
 * @class
 * @name _AjaxRequest
 * @memberOf myfaces._impl.xhrCore
 * @extends myfaces._impl.core.Object
 */
_MF_CLS(_PFX_XHR + "_AjaxRequest", _MF_OBJECT, /** @lends myfaces._impl.xhrCore._AjaxRequest.prototype */ {

    _contentType:"application/x-www-form-urlencoded",
    /** source element issuing the request */
    _source:null,
    /** context passed down from the caller */
    _context:null,
    /** source form issuing the request */
    _sourceForm:null,
    /** passthrough parameters */
    _passThrough:null,

    /** queue control */
    _timeout:null,
    /** enqueuing delay */
    //_delay:null,
    /** queue size */
    _queueSize:-1,

    /**
     back reference to the xhr queue,
     only set if the object really is queued
     */
    _xhrQueue:null,

    /** pps an array of identifiers which should be part of the submit, the form is ignored */
    _partialIdsArray:null,

    /** xhr object, internal param */
    _xhr:null,

    /** predefined method */
    _ajaxType:"POST",

    //CONSTANTS
    ENCODED_URL:"javax.faces.encodedURL",
    /*
     * constants used internally
     */
    _CONTENT_TYPE:"Content-Type",
    _HEAD_FACES_REQ:"Faces-Request",
    _VAL_AJAX:"partial/ajax",
    _XHR_CONST:myfaces._impl.xhrCore.engine.XhrConst,

    // _exception: null,
    // _requestParameters: null,
    /**
     * Constructor
     * <p />
     * note there is a load of common properties
     * inherited by the base class which define the corner
     * parameters and the general internal behavior
     * like _onError etc...
     * @param {Object} args an arguments map which an override any of the given protected
     * instance variables, by a simple name value pair combination
     */
    constructor_:function (args) {

        try {
            this._callSuper("constructor_", args);

            this._initDefaultFinalizableFields();
            delete this._resettableContent["_xhrQueue"];

            this.applyArgs(args);

            /*namespace remapping for readability*/
            //we fetch in the standard arguments
            //and apply them to our protected attributes
            //we do not gc the entry hence it is not defined on top
            var xhrCore = myfaces._impl.xhrCore;
            this._AJAXUTIL = xhrCore._AjaxUtils;

        } catch (e) {
            //_onError
            this._stdErrorHandler(this._xhr, this._context, e);
        }
    },

    /**
     * Sends an Ajax request
     */
    send:function () {

        var _Lang = this._Lang;
        var _RT = this._RT;
        var _Dom = this._Dom;
        try {

            var scopeThis = _Lang.hitch(this, function (functionName) {
                return _Lang.hitch(this, this[functionName]);
            });
            this._xhr = _Lang.mixMaps(this._getTransport(), {
                onprogress:scopeThis("onprogress"),
                ontimeout:scopeThis("ontimeout"),
                //remove for xhr level2 support (chrome has problems with it)
                //for chrome we have to emulate the onloadend by calling it explicitely
                //and leave the onload out
                //onloadend:  scopeThis("ondone"),
                onload:scopeThis("onsuccess"),
                onerror:scopeThis("onerror")

            }, true);

            this._applyClientWindowId();
            var xhr = this._xhr,
                    sourceForm = this._sourceForm,
                    targetURL = (typeof sourceForm.elements[this.ENCODED_URL] == 'undefined') ?
                            sourceForm.action :
                            sourceForm.elements[this.ENCODED_URL].value,
                    formData = this.getFormData();

            for (var key in this._passThrough) {
                if (!this._passThrough.hasOwnProperty(key)) continue;
                formData.append(key, this._passThrough[key]);
            }

            xhr.open(this._ajaxType, targetURL +
                    ((this._ajaxType == "GET") ? "?" + this._formDataToURI(formData) : "")
                    , true);

            xhr.timeout = this._timeout || 0;

            this._applyContentType(xhr);
            xhr.setRequestHeader(this._HEAD_FACES_REQ, this._VAL_AJAX);

            //some webkit based mobile browsers do not follow the w3c spec of
            // setting the accept headers automatically
            if (this._RT.browser.isWebKit) {
                xhr.setRequestHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            }
            this._sendEvent("BEGIN");
            //Check if it is a custom form data object
            //if yes we use makefinal for the final handling
            if (formData && formData.makeFinal) {
                formData = formData.makeFinal()
            }
            xhr.send((this._ajaxType != "GET") ? formData : null);

        } catch (e) {
            //_onError//_onError
            e = (e._mfInternal) ? e : this._Lang.makeException(new Error(), "sendError", "sendError", this._nameSpace, "send", e.message);
            this._stdErrorHandler(this._xhr, this._context, e);
        } finally {
            //no finally possible since the iframe uses real asynchronousity
        }
    },

    _applyClientWindowId:function () {
        var clientWindow = this._Dom.getNamedElementFromForm(this._sourceForm, "javax.faces.ClientWindow");
        //pass through if exists already set by _Impl
        if ('undefined' != typeof this._context._mfInternal._clientWindow) {
            this._context._mfInternal._clientWindowOld = clientWindow.value;
            clientWindow.value = this._context._mfInternal._clientWindow;
        } else {
            if(clientWindow) {
                this._context._mfInternal._clientWindowDisabled = !! clientWindow.disabled;
                clientWindow.disabled = true;
            }
        }
    },

    _restoreClientWindowId:function () {
        //we have to reset the client window back to its original state

        var clientWindow = this._Dom.getNamedElementFromForm(this._sourceForm, "javax.faces.ClientWindow");
        if(!clientWindow) {
            return;
        }
        if ('undefined' != typeof this._context._mfInternal._clientWindowOld) {
            clientWindow.value =  this._context._mfInternal._clientWindow;
        }
        if('undefined' != typeof this._context._mfInternal._clientWindowDisabled) {
            //we reset it to the old value
            clientWindow.disabled = this._context._mfInternal._clientWindowDisabled;
        }
    },

    /**
     * applies the content type, this needs to be done only for xhr
     * level1
     * @param xhr
     * @private
     */
    _applyContentType:function (xhr) {
        var contentType = this._contentType + "; charset=utf-8";
        xhr.setRequestHeader(this._CONTENT_TYPE, contentType);
    },

    ondone:function () {
        this._requestDone();
    },

    onsuccess:function (/*evt*/) {
        this._restoreClientWindowId();
        var context = this._context;
        var xhr = this._xhr;
        try {
            this._sendEvent("COMPLETE");
            //now we have to reroute into our official api
            //because users might want to decorate it, we will split it apart afterwards

            context._mfInternal = context._mfInternal || {};
            jsf.ajax.response((xhr.getXHRObject) ? xhr.getXHRObject() : xhr, context);

        } catch (e) {
            this._stdErrorHandler(this._xhr, this._context, e);

            //add for xhr level2 support
        } finally {
            //W3C spec onloadend must be called no matter if success or not
            this.ondone();
        }
    },

    onerror:function (/*evt*/) {
        this._restoreClientWindowId();
        //TODO improve the error code detection here regarding server errors etc...
        //and push it into our general error handling subframework
        var context = this._context;
        var xhr = this._xhr;
        var _Lang = this._Lang;

        var errorText = "";
        this._sendEvent("COMPLETE");
        try {
            var UNKNOWN = _Lang.getMessage("UNKNOWN");
            //status can be 0 and statusText can be ""
            var status = ('undefined' != xhr.status && null != xhr.status) ? xhr.status : UNKNOWN;
            var statusText = ('undefined' != xhr.statusText && null != xhr.statusText) ? xhr.statusText : UNKNOWN;
            errorText = _Lang.getMessage("ERR_REQU_FAILED", null, status, statusText);

        } catch (e) {
            errorText = _Lang.getMessage("ERR_REQ_FAILED_UNKNOWN", null);
        } finally {
            try {
                var _Impl = this.attr("impl");
                _Impl.sendError(xhr, context, _Impl.HTTPERROR,
                        _Impl.HTTPERROR, errorText, "", "myfaces._impl.xhrCore._AjaxRequest", "onerror");
            } finally {
                //add for xhr level2 support
                //since chrome does not call properly the onloadend we have to do it manually
                //to eliminate xhr level1 for the compile profile modern
                //W3C spec onloadend must be called no matter if success or not
                this.ondone();
            }
        }
        //_onError
    },

    onprogress:function (/*evt*/) {
        //do nothing for now
    },

    ontimeout:function (/*evt*/) {
        try {
            this._restoreClientWindowId();
            //we issue an event not an error here before killing the xhr process
            this._sendEvent("TIMEOUT_EVENT");
            //timeout done we process the next in the queue
        } finally {
            this._requestDone();
        }
    },

    _formDataToURI:function (formData) {
        if (formData && formData.makeFinal) {
            formData = formData.makeFinal()
        }
        return formData;
    },

    _getTransport:function () {

        var xhr = this._RT.getXHRObject();
        //the current xhr level2 timeout w3c spec is not implemented by the browsers yet
        //we have to do a fallback to our custom routines

        //add for xhr level2 support
        //Chrome fails in the current builds, on our loadend, we disable the xhr
        //level2 optimisations for now
        if (/*('undefined' == typeof this._timeout || null == this._timeout) &&*/ this._RT.getXHRLvl() >= 2) {
            //no timeout we can skip the emulation layer
            return xhr;
        }
        return new myfaces._impl.xhrCore.engine.Xhr1({xhrObject:xhr});
    },

    //----------------- backported from the base request --------------------------------
    //non abstract ones
    /**
     * Spec. 13.3.1
     * Collect and encode input elements.
     * Additionally the hidden element javax.faces.ViewState
     *
     *
     * @return  an element of formDataWrapper
     * which keeps the final Send Representation of the
     */
    getFormData:function () {
        var _AJAXUTIL = this._AJAXUTIL, myfacesOptions = this._context.myfaces;
        return this._Lang.createFormDataDecorator(jsf.getViewState(this._sourceForm));
    },

    /**
     * Client error handlers which also in the long run route into our error queue
     * but also are able to deliver more meaningful messages
     * note, in case of an error all subsequent xhr requests are dropped
     * to get a clean state on things
     *
     * @param request the xhr request object
     * @param context the context holding all values for further processing
     * @param exception the embedded exception
     */
    _stdErrorHandler:function (request, context, exception) {
        var xhrQueue = this._xhrQueue;
        try {
            this.attr("impl").stdErrorHandler(request, context, exception);
        } finally {
            if (xhrQueue) {
                xhrQueue.cleanup();
            }
        }
    },

    _sendEvent:function (evtType) {
        var _Impl = this.attr("impl");
        _Impl.sendEvent(this._xhr, this._context, _Impl[evtType]);
    },

    _requestDone:function () {
        var queue = this._xhrQueue;
        if (queue) {
            queue.processQueue();
        }
        //ie6 helper cleanup
        delete this._context.source;
        this._finalize();
    },

    //cleanup
    _finalize:function () {
        if (this._xhr.readyState == this._XHR_CONST.READY_STATE_DONE) {
            this._callSuper("_finalize");
        }
    }
});


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

    constructor_:function (arguments) {
        this._callSuper("constructor_", arguments);
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

    constructor_:function (arguments) {
        this._callSuper("constructor_", arguments);
        //TODO xhr level2 can deal with real props

    },

    _getTransport:function () {
        return new XMLHttpRequest();
    }
});





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
 * @name _AjaxResponse
 * @memberOf myfaces._impl.xhrCore
 * @extends myfaces._impl.core.Object
 * @description
 * This singleton is responsible for handling the standardized xml ajax response
 * Note: since the semantic processing can be handled about 90% in a functional
 * style we make this class stateless. Every state information is stored
 * temporarily in the context.
 *
 * The singleton approach also improves performance
 * due to less object gc compared to the old instance approach.
 *
 */
_MF_SINGLTN(_PFX_XHR + "_AjaxResponse", _MF_OBJECT, /** @lends myfaces._impl.xhrCore._AjaxResponse.prototype */ {

    /*partial response types*/
    RESP_PARTIAL:"partial-response",
    RESP_TYPE_ERROR:"error",
    RESP_TYPE_REDIRECT:"redirect",
    RESP_TYPE_CHANGES:"changes",

    /*partial commands*/
    CMD_CHANGES:"changes",
    CMD_UPDATE:"update",
    CMD_DELETE:"delete",
    CMD_INSERT:"insert",
    CMD_EVAL:"eval",
    CMD_ERROR:"error",
    CMD_ATTRIBUTES:"attributes",
    CMD_EXTENSION:"extension",
    CMD_REDIRECT:"redirect",

    /*other constants*/
    P_VIEWSTATE:"javax.faces.ViewState",
    P_CLIENTWINDOW: "javax.faces.ClientWindow",
    P_VIEWROOT:"javax.faces.ViewRoot",
    P_VIEWHEAD:"javax.faces.ViewHead",
    P_VIEWBODY:"javax.faces.ViewBody",

    /**
     * uses response to start Html element replacement
     *
     * @param {Object} request (xhrRequest) - xhr request object
     * @param {Object} context (Map) - AJAX context
     *
     * A special handling has to be added to the update cycle
     * according to the JSDoc specs if the CDATA block contains html tags the outer rim must be stripped
     * if the CDATA block contains a head section the document head must be replaced
     * and if the CDATA block contains a body section the document body must be replaced!
     *
     */
    processResponse:function (request, context) {
        //mfinternal handling, note, the mfinternal is only optional
        //according to the spec
        context._mfInternal = context._mfInternal || {};
        var mfInternal = context._mfInternal;

        //the temporary data is hosted here
        mfInternal._updateElems = [];
        mfInternal._updateForms = [];
        mfInternal.appliedViewState = null;
        mfInternal.appliedClientWindow = null;

        try {
            var _Impl = this.attr("impl"), _Lang = this._Lang;
            // TODO:
            // Solution from
            // http://www.codingforums.com/archive/index.php/t-47018.html
            // to solve IE error 1072896658 when a Java server sends iso88591
            // istead of ISO-8859-1

            if (!request || !_Lang.exists(request, "responseXML")) {
                throw this.makeException(new Error(), _Impl.EMPTY_RESPONSE, _Impl.EMPTY_RESPONSE, this._nameSpace, "processResponse", "");
            }
            //check for a parseError under certain browsers

            var xmlContent = request.responseXML;
            //ie6+ keeps the parsing response under xmlContent.parserError
            //while the rest of the world keeps it as element under the first node
            var xmlErr = _Lang.fetchXMLErrorMessage(request.responseText || request.response, xmlContent)
            if (xmlErr) {
                throw this._raiseError(new Error(), xmlErr.errorMessage + "\n" + xmlErr.sourceText + "\n" + xmlErr.visualError + "\n", "processResponse");
            }
            var partials = xmlContent.childNodes[0];
            if ('undefined' == typeof partials || partials == null) {
                throw this._raiseError(new Error(), "No child nodes for response", "processResponse");

            } else {
                if (partials.tagName != this.RESP_PARTIAL) {
                    // IE 8 sees XML Header as first sibling ...
                    partials = partials.nextSibling;
                    if (!partials || partials.tagName != this.RESP_PARTIAL) {
                        throw this._raiseError(new Error(), "Partial response not set", "processResponse");
                    }
                }
            }

            var childNodesLength = partials.childNodes.length;

            for (var loop = 0; loop < childNodesLength; loop++) {
                var childNode = partials.childNodes[loop];
                var tagName = childNode.tagName;
                /**
                 * <eval>
                 *      <![CDATA[javascript]]>
                 * </eval>
                 */

                //this ought to be enough for eval
                //however the run scripts still makes sense
                //in the update and insert area for components
                //which do not use the response writer properly
                //we might add this one as custom option in update and
                //insert!
                if (tagName == this.CMD_ERROR) {
                    this.processError(request, context, childNode);
                } else if (tagName == this.CMD_REDIRECT) {
                    this.processRedirect(request, context, childNode);
                } else if (tagName == this.CMD_CHANGES) {
                    this.processChanges(request, context, childNode);
                }
            }

            //fixup missing viewStates due to spec deficiencies
            if(mfInternal.appliedViewState) {
                this.fixViewStates(context);
            }
            if(mfInternal.appliedClientWindow) {
                this.fixClientWindows(context);
            }

            //spec jsdoc, the success event must be sent from response
            _Impl.sendEvent(request, context, _Impl["SUCCESS"]);

        } finally {
            delete mfInternal._updateElems;
            delete mfInternal._updateForms;
            delete mfInternal.appliedViewState;
            delete mfInternal.appliedClientWindow;
        }
    },

    /**
     * fixes the viewstates in the current page
     *
     * @param context
     */
    fixViewStates:function (context) {
        var _Lang = this._Lang;
        var mfInternal = context._mfInternal;

        if (null == mfInternal.appliedViewState) {
            return;
        }

        //if we set our no portlet env we safely can update all forms with
        //the new viewstate
        if (this._RT.getLocalOrGlobalConfig(context, "no_portlet_env", false)) {
            for (var cnt = document.forms.length - 1; cnt >= 0; cnt--) {
                this._setVSTCWForm(context, document.forms[cnt], mfInternal.appliedViewState, this.P_VIEWSTATE);
            }
            return;
        }

        // Now update the forms that were not replaced but forced to be updated, because contains child ajax tags
        // we should only update forms with view state hidden field. If by some reason, the form was set to be
        // updated but the form was replaced, it does not have hidden view state, so later in changeTrace processing the
        // view state is updated.

        //set the viewstates of all outer forms parents of our updated elements

        _Lang.arrForEach(mfInternal._updateForms, function (elem) {
            this._setVSTCWForm(context, elem, mfInternal.appliedViewState, this.P_VIEWSTATE);
        }, 0, this);

        //set the viewstate of all forms within our updated elements
        _Lang.arrForEach(mfInternal._updateElems, function (elem) {
            this._setVSTCWInnerForms(context, elem, mfInternal.appliedViewState, this.P_VIEWSTATE);
        }, 0, this);
    },

    fixClientWindows:function (context, theForm) {
        var _Lang = this._Lang;
        var mfInternal = context._mfInternal;

        if (null == mfInternal.appliedClientWindow) {
            return;
        }
         //if we set our no portlet env we safely can update all forms with
        //the new viewstate
        if (this._RT.getLocalOrGlobalConfig(context, "no_portlet_env", false)) {
            for (var cnt = document.forms.length - 1; cnt >= 0; cnt--) {
                this._setVSTCWForm(context, document.forms[cnt], mfInternal.appliedClientWindow, this.P_CLIENTWINDOW);
            }
            return;
        }
        //set the client window of all outer form of updated elements

        _Lang.arrForEach(mfInternal._updateForms, function (elem) {
            this._setVSTCWForm(context, elem, mfInternal.appliedClientWindow, this.P_CLIENTWINDOW);
        }, 0, this);

        //set the client window of all forms within our updated elements
        _Lang.arrForEach(mfInternal._updateElems, function (elem) {
            this._setVSTCWInnerForms(context, elem, mfInternal.appliedClientWindow, this.P_CLIENTWINDOW);
        }, 0, this);
    },

    /**
     * sets the viewstate element in a given form
     *
     * @param theForm the form to which the element has to be set to
     * @param context the current request context
     */
    _setVSTCWForm:function (context, theForm, value, identifier) {
        theForm = this._Lang.byId(theForm);
        var mfInternal = context._mfInternal;

        if (!theForm) return;

        //in IE7 looking up form elements with complex names (such as 'javax.faces.ViewState') fails in certain cases
        //iterate through the form elements to find the element, instead
        var fieldToApply = this._Dom.getNamedElementFromForm(theForm, identifier);

        if (fieldToApply) {
            this._Dom.setAttribute(fieldToApply, "value", value);
        } else if (!fieldToApply) {
            var element = this._Dom.getDummyPlaceHolder();
            //spec error, two elements with the same id should not be there, TODO recheck the space if the name does not suffice alone
            element.innerHTML = ["<input type='hidden'", "id='", identifier+jsf.separatorchar+Math.random() , "' name='", identifier , "' value='" , value , "' />"].join("");
            //now we go to proper dom handling after having to deal with another ie screwup
            try {
                theForm.appendChild(element.childNodes[0]);
            } finally {
                element.innerHTML = "";
            }
        }
    },

    _setVSTCWInnerForms:function (context, elem, value, identifier) {

        var _Lang = this._Lang, _Dom = this._Dom;
        elem = _Dom.byIdOrName(elem);
        //elem not found for whatever reason
        //https://issues.apache.org/jira/browse/MYFACES-3544
        if (!elem) return;

        var replacedForms = _Dom.findByTagName(elem, "form", false);

        var applyVST = _Lang.hitch(this, function (elem) {
            this._setVSTCWForm(context, elem, value, identifier);
        });

        try {
            _Lang.arrForEach(replacedForms, applyVST, 0, this);
        } finally {
            applyVST = null;
        }
    },

    /**
     * processes an incoming error from the response
     * which is hosted under the &lt;error&gt; tag
     * @param request the current request
     * @param context the contect object
     * @param node the node in the xml hosting the error message
     */
    processError:function (request, context, node) {
        /**
         * <error>
         *      <error-name>String</error-name>
         *      <error-message><![CDATA[message]]></error-message>
         * <error>
         */
        var errorName = node.firstChild.textContent || node.firstChild.text || "",
                errorMessage = node.childNodes[1].firstChild.data || "";

        this.attr("impl").sendError(request, context, this.attr("impl").SERVER_ERROR, errorName, errorMessage, "myfaces._impl.xhrCore._AjaxResponse", "processError");
    },

    /**
     * processes an incoming xml redirect directive from the ajax response
     * @param request the request object
     * @param context the context
     * @param node the node hosting the redirect data
     */
    processRedirect:function (request, context, node) {
        /**
         * <redirect url="url to redirect" />
         */
        var _Lang = this._Lang;
        var redirectUrl = node.getAttribute("url");
        if (!redirectUrl) {
            throw this._raiseError(new Error(), _Lang.getMessage("ERR_RED_URL", null, "_AjaxResponse.processRedirect"), "processRedirect");
        }
        redirectUrl = _Lang.trim(redirectUrl);
        if (redirectUrl == "") {
            return false;
        }
        window.location = redirectUrl;
        return true;
    },

    /**
     * main entry point for processing the changes
     * it deals with the &lt;changes&gt; node of the
     * response
     *
     * @param request the xhr request object
     * @param context the context map
     * @param node the changes node to be processed
     */
    processChanges:function (request, context, node) {
        var changes = node.childNodes;
        var _Lang = this._Lang;
        //note we need to trace the changes which could affect our insert update or delete
        //se that we can realign our ViewStates afterwards
        //the realignment must happen post change processing

        for (var i = 0; i < changes.length; i++) {

            switch (changes[i].tagName) {

                case this.CMD_UPDATE:
                    this.processUpdate(request, context, changes[i]);
                    break;
                case this.CMD_EVAL:
                    _Lang.globalEval(changes[i].firstChild.data);
                    break;
                case this.CMD_INSERT:
                    this.processInsert(request, context, changes[i]);
                    break;
                case this.CMD_DELETE:
                    this.processDelete(request, context, changes[i]);
                    break;
                case this.CMD_ATTRIBUTES:
                    this.processAttributes(request, context, changes[i]);
                    break;
                case this.CMD_EXTENSION:
                    break;
                default:
                    throw this._raiseError(new Error(), "_AjaxResponse.processChanges: Illegal Command Issued", "processChanges");
            }
        }

        return true;
    },

    /**
     * First sub-step process a pending update tag
     *
     * @param request the xhr request object
     * @param context the context map
     * @param node the changes node to be processed
     */
    processUpdate:function (request, context, node) {
        if ( (node.getAttribute('id').indexOf(this.P_VIEWSTATE) != -1) || (node.getAttribute('id').indexOf(this.P_CLIENTWINDOW) != -1) ) {
            //update the submitting forms viewstate to the new value
            // The source form has to be pulled out of the CURRENT document first because the context object
            // may refer to an invalid document if an update of the entire body has occurred before this point.
            var mfInternal = context._mfInternal,
                    fuzzyFormDetection = this._Lang.hitch(this._Dom, this._Dom.fuzzyFormDetection);
            var elemId = (mfInternal._mfSourceControlId) ? mfInternal._mfSourceControlId :
                    ((context.source) ? context.source.id : null);

            //theoretically a source of null can be given, then our form detection fails for
            //the source element case and hence updateviewstate is skipped for the source
            //form, but still render targets still can get the viewstate
            var sourceForm = (mfInternal && mfInternal["_mfSourceFormId"] &&
                    document.forms[mfInternal["_mfSourceFormId"]]) ?
                    document.forms[mfInternal["_mfSourceFormId"]] : ((elemId) ? fuzzyFormDetection(elemId) : null);

            if(node.getAttribute('id').indexOf(this.P_VIEWSTATE) != -1) {
                mfInternal.appliedViewState = this._Dom.concatCDATABlocks(node);//node.firstChild.nodeValue;
            } else if(node.getAttribute('id').indexOf(this.P_CLIENTWINDOW) != -1) {
                mfInternal.appliedClientWindow = node.firstChild.nodeValue;
            }
            //source form could not be determined either over the form identifer or the element
            //we now skip this phase and just add everything we need for the fixup code

            if (!sourceForm) {
                //no source form found is not an error because
                //we might be able to recover one way or the other
                return true;
            }

            mfInternal._updateForms.push(sourceForm.id);

        }
        else {
            // response may contain several blocks
            var cDataBlock = this._Dom.concatCDATABlocks(node),
                    resultNode = null,
                    pushOpRes = this._Lang.hitch(this, this._pushOperationResult);

            switch (node.getAttribute('id')) {
                case this.P_VIEWROOT:

                    cDataBlock = cDataBlock.substring(cDataBlock.indexOf("<html"));

                    var parsedData = this._replaceHead(request, context, cDataBlock);

                    resultNode = ('undefined' != typeof parsedData && null != parsedData) ? this._replaceBody(request, context, cDataBlock, parsedData) : this._replaceBody(request, context, cDataBlock);
                    if (resultNode) {
                        pushOpRes(context, resultNode);
                    }
                    break;
                case this.P_VIEWHEAD:
                    //we cannot replace the head, almost no browser allows this, some of them throw errors
                    //others simply ignore it or replace it and destroy the dom that way!
                    this._replaceHead(request, context, cDataBlock);

                    break;
                case this.P_VIEWBODY:
                    //we assume the cdata block is our body including the tag
                    resultNode = this._replaceBody(request, context, cDataBlock);
                    if (resultNode) {
                        pushOpRes(context, resultNode);
                    }
                    break;

                default:
                    resultNode = this.replaceHtmlItem(request, context, node.getAttribute('id'), cDataBlock);
                    if (resultNode) {
                        pushOpRes(context, resultNode);
                    }
                    break;
            }
        }

        return true;
    },

    _pushOperationResult:function (context, resultNode) {
        var mfInternal = context._mfInternal;
        var pushSubnode = this._Lang.hitch(this, function (currNode) {
            var parentForm = this._Dom.getParent(currNode, "form");
            //if possible we work over the ids
            //so that elements later replaced are referenced
            //at the latest possibility
            if (null != parentForm) {
                mfInternal._updateForms.push(parentForm.id || parentForm);
            }
            else {
                mfInternal._updateElems.push(currNode.id || currNode);
            }
        });
        var isArr = 'undefined' != typeof resultNode.length && 'undefined' == typeof resultNode.nodeType;
        if (isArr && resultNode.length) {
            for (var cnt = 0; cnt < resultNode.length; cnt++) {
                pushSubnode(resultNode[cnt]);
            }
        } else if (!isArr) {
            pushSubnode(resultNode);
        }

    },

    /**
     * replaces a current head theoretically,
     * pratically only the scripts are evaled anew since nothing else
     * can be changed.
     *
     * @param request the current request
     * @param context the ajax context
     * @param newData the data to be processed
     *
     * @return an xml representation of the page for further processing if possible
     */
    _replaceHead:function (request, context, newData) {

        var _Lang = this._Lang,
                _Dom = this._Dom,
                isWebkit = this._RT.browser.isWebKit,
        //we have to work around an xml parsing bug in Webkit
        //see https://issues.apache.org/jira/browse/MYFACES-3061
                doc = (!isWebkit) ? _Lang.parseXML(newData) : null,
                newHead = null;

        if (!isWebkit && _Lang.isXMLParseError(doc)) {
            doc = _Lang.parseXML(newData.replace(/<!\-\-[\s\n]*<!\-\-/g, "<!--").replace(/\/\/-->[\s\n]*\/\/-->/g, "//-->"));
        }

        if (isWebkit || _Lang.isXMLParseError(doc)) {
            //the standard xml parser failed we retry with the stripper
            var parser = new (this._RT.getGlobalConfig("updateParser", myfaces._impl._util._HtmlStripper))();
            var headData = parser.parse(newData, "head");
            //We cannot avoid it here, but we have reduced the parsing now down to the bare minimum
            //for further processing
            newHead = _Lang.parseXML("<head>" + headData + "</head>");
            //last and slowest option create a new head element and let the browser
            //do its slow job
            if (_Lang.isXMLParseError(newHead)) {
                try {
                    newHead = _Dom.createElement("head");
                    newHead.innerHTML = headData;
                } catch (e) {
                    //we give up no further fallbacks
                    throw this._raiseError(new Error(), "Error head replacement failed reason:" + e.toString(), "_replaceHead");
                }
            }
        } else {
            //parser worked we go on
            newHead = doc.getElementsByTagName("head")[0];
        }

        var oldTags = _Dom.findByTagNames(document.getElementsByTagName("head")[0], {"link":true, "style":true});
        _Dom.runCss(newHead, true);
        _Dom.deleteItems(oldTags);

        //var oldTags = _Dom.findByTagNames(document.getElementsByTagName("head")[0], {"script": true});
        //_Dom.deleteScripts(oldTags);
        _Dom.runScripts(newHead, true);

        return doc;
    },

    /**
     * special method to handle the body dom manipulation,
     * replacing the entire body does not work fully by simply adding a second body
     * and by creating a range instead we have to work around that by dom creating a second
     * body and then filling it properly!
     *
     * @param {Object} request our request object
     * @param {Object} context (Map) the response context
     * @param {String} newData the markup which replaces the old dom node!
     * @param {Node} parsedData (optional) preparsed XML representation data of the current document
     */
    _replaceBody:function (request, context, newData /*varargs*/) {
        var _RT = this._RT,
                _Dom = this._Dom,
                _Lang = this._Lang,

                oldBody = document.getElementsByTagName("body")[0],
                placeHolder = document.createElement("div"),
                isWebkit = _RT.browser.isWebKit;

        placeHolder.id = "myfaces_bodyplaceholder";

        _Dom._removeChildNodes(oldBody);
        oldBody.innerHTML = "";
        oldBody.appendChild(placeHolder);

        var bodyData, doc = null, parser;

        //we have to work around an xml parsing bug in Webkit
        //see https://issues.apache.org/jira/browse/MYFACES-3061
        if (!isWebkit) {
            doc = (arguments.length > 3) ? arguments[3] : _Lang.parseXML(newData);
        }

        if (!isWebkit && _Lang.isXMLParseError(doc)) {
            doc = _Lang.parseXML(newData.replace(/<!\-\-[\s\n]*<!\-\-/g, "<!--").replace(/\/\/-->[\s\n]*\/\/-->/g, "//-->"));
        }

        if (isWebkit || _Lang.isXMLParseError(doc)) {
            //the standard xml parser failed we retry with the stripper

            parser = new (_RT.getGlobalConfig("updateParser", myfaces._impl._util._HtmlStripper))();

            bodyData = parser.parse(newData, "body");
        } else {
            //parser worked we go on
            var newBodyData = doc.getElementsByTagName("body")[0];

            //speedwise we serialize back into the code
            //for code reduction, speedwise we will take a small hit
            //there which we will clean up in the future, but for now
            //this is ok, I guess, since replace body only is a small subcase
            //bodyData = _Lang.serializeChilds(newBodyData);
            var browser = _RT.browser;
            if (!browser.isIEMobile || browser.isIEMobile >= 7) {
                //TODO check what is failing there
                for (var cnt = 0; cnt < newBodyData.attributes.length; cnt++) {
                    var value = newBodyData.attributes[cnt].value;
                    if (value)
                        _Dom.setAttribute(oldBody, newBodyData.attributes[cnt].name, value);
                }
            }
        }
        //we cannot serialize here, due to escape problems
        //we must parse, this is somewhat unsafe but should be safe enough
        parser = new (_RT.getGlobalConfig("updateParser", myfaces._impl._util._HtmlStripper))();
        bodyData = parser.parse(newData, "body");

        var returnedElement = this.replaceHtmlItem(request, context, placeHolder, bodyData);

        if (returnedElement) {
            this._pushOperationResult(context, returnedElement);
        }
        return returnedElement;
    },

    /**
     * Replaces HTML elements through others and handle errors if the occur in the replacement part
     *
     * @param {Object} request (xhrRequest)
     * @param {Object} context (Map)
     * @param {Object} itemIdToReplace (String|Node) - ID of the element to replace
     * @param {String} markup - the new tag
     */
    replaceHtmlItem:function (request, context, itemIdToReplace, markup) {
        var _Lang = this._Lang, _Dom = this._Dom;

        var item = (!_Lang.isString(itemIdToReplace)) ? itemIdToReplace :
                _Dom.byIdOrName(itemIdToReplace);

        if (!item) {
            throw this._raiseError(new Error(), _Lang.getMessage("ERR_ITEM_ID_NOTFOUND", null, "_AjaxResponse.replaceHtmlItem", (itemIdToReplace) ? itemIdToReplace.toString() : "undefined"), "replaceHtmlItem");
        }
        return _Dom.outerHTML(item, markup, this._RT.getLocalOrGlobalConfig(context, "preserveFocus", false));
    },

    /**
     * xml insert command handler
     *
     * @param request the ajax request element
     * @param context the context element holding the data
     * @param node the xml node holding the insert data
     * @return true upon successful completion, false otherwise
     *
     **/
    processInsert:function (request, context, node) {
        /*remapping global namespaces for speed and readability reasons*/
        var _Dom = this._Dom,
                _Lang = this._Lang,
        //determine which path to go:
                insertData = this._parseInsertData(request, context, node);

        if (!insertData) return false;

        var opNode = _Dom.byIdOrName(insertData.opId);
        if (!opNode) {
            throw this._raiseError(new Error(), _Lang.getMessage("ERR_PPR_INSERTBEFID_1", null, "_AjaxResponse.processInsert", insertData.opId), "processInsert");
        }

        //call insertBefore or insertAfter in our dom routines
        var replacementFragment = _Dom[insertData.insertType](opNode, insertData.cDataBlock);
        if (replacementFragment) {
            this._pushOperationResult(context, replacementFragment);
        }
        return true;
    },

    /**
     * determines the corner data from the insert tag parsing process
     *
     *
     * @param request request
     * @param context context
     * @param node the current node pointing to the insert tag
     * @return false if the parsing failed, otherwise a map with follwing attributes
     * <ul>
     *     <li>inserType - a ponter to a constant which maps the direct function name for the insert operation </li>
     *     <li>opId - the before or after id </li>
     *     <li>cDataBlock - the html cdata block which needs replacement </li>
     * </ul>
     *
     * TODO we have to find a mechanism to replace the direct sendError calls with a javascript exception
     * which we then can use for cleaner error code handling
     */
    _parseInsertData:function (request, context, node) {
        var _Lang = this._Lang,
                _Dom = this._Dom,
                concatCDATA = _Dom.concatCDATABlocks,

                INSERT_TYPE_BEFORE = "insertBefore",
                INSERT_TYPE_AFTER = "insertAfter",

                id = node.getAttribute("id"),
                beforeId = node.getAttribute("before"),
                afterId = node.getAttribute("after"),
                ret = {};

        //now we have to make a distinction between two different parsing paths
        //due to a spec malalignment
        //a <insert id="... beforeId|AfterId ="...
        //b <insert><before id="..., <insert> <after id="....
        //see https://issues.apache.org/jira/browse/MYFACES-3318
        //simple id, case1
        if (id && beforeId && !afterId) {
            ret.insertType = INSERT_TYPE_BEFORE;
            ret.opId = beforeId;
            ret.cDataBlock = concatCDATA(node);

            //<insert id=".. afterId="..
        } else if (id && !beforeId && afterId) {
            ret.insertType = INSERT_TYPE_AFTER;
            ret.opId = afterId;
            ret.cDataBlock = concatCDATA(node);

            //<insert><before id="... <insert><after id="...
        } else if (!id) {
            var opType = node.childNodes[0].tagName;

            if (opType != "before" && opType != "after") {
                throw this._raiseError(new Error(), _Lang.getMessage("ERR_PPR_INSERTBEFID"), "_parseInsertData");
            }
            opType = opType.toLowerCase();
            var beforeAfterId = node.childNodes[0].getAttribute("id");
            ret.insertType = (opType == "before") ? INSERT_TYPE_BEFORE : INSERT_TYPE_AFTER;
            ret.opId = beforeAfterId;
            ret.cDataBlock = concatCDATA(node.childNodes[0]);
        } else {
            throw this._raiseError(new Error(), [_Lang.getMessage("ERR_PPR_IDREQ"),
                                                 "\n ",
                                                 _Lang.getMessage("ERR_PPR_INSERTBEFID")].join(""), "_parseInsertData");
        }
        ret.opId = _Lang.trim(ret.opId);
        return ret;
    },

    processDelete:function (request, context, node) {

        var _Lang = this._Lang,
                _Dom = this._Dom,
                deleteId = node.getAttribute('id');

        if (!deleteId) {
            throw this._raiseError(new Error(), _Lang.getMessage("ERR_PPR_UNKNOWNCID", null, "_AjaxResponse.processDelete", ""), "processDelete");
        }

        var item = _Dom.byIdOrName(deleteId);
        if (!item) {
            throw this._raiseError(new Error(), _Lang.getMessage("ERR_PPR_UNKNOWNCID", null, "_AjaxResponse.processDelete", deleteId), "processDelete");
        }

        var parentForm = this._Dom.getParent(item, "form");
        if (null != parentForm) {
            context._mfInternal._updateForms.push(parentForm);
        }
        _Dom.deleteItem(item);

        return true;
    },

    processAttributes:function (request, context, node) {
        //we now route into our attributes function to bypass
        //IE quirks mode incompatibilities to the biggest possible extent
        //most browsers just have to do a setAttributes but IE
        //behaves as usual not like the official standard
        //myfaces._impl._util.this._Dom.setAttribute(domNode, attribute, value;

        var _Lang = this._Lang,
        //<attributes id="id of element"> <attribute name="attribute name" value="attribute value" />* </attributes>
                elemId = node.getAttribute('id');

        if (!elemId) {
            throw this._raiseError(new Error(), "Error in attributes, id not in xml markup", "processAttributes");
        }
        var childNodes = node.childNodes;

        if (!childNodes) {
            return false;
        }
        for (var loop2 = 0; loop2 < childNodes.length; loop2++) {
            var attributesNode = childNodes[loop2],
                    attrName = attributesNode.getAttribute("name"),
                    attrValue = attributesNode.getAttribute("value");

            if (!attrName) {
                continue;
            }

            attrName = _Lang.trim(attrName);
            /*no value means reset*/
            //value can be of boolean value hence full check
            if ('undefined' == typeof attrValue || null == attrValue) {
                attrValue = "";
            }

            switch (elemId) {
                case this.P_VIEWROOT:
                    throw  this._raiseError(new Error(), _Lang.getMessage("ERR_NO_VIEWROOTATTR", null, "_AjaxResponse.processAttributes"), "processAttributes");

                case this.P_VIEWHEAD:
                    throw  this._raiseError(new Error(), _Lang.getMessage("ERR_NO_HEADATTR", null, "_AjaxResponse.processAttributes"), "processAttributes");

                case this.P_VIEWBODY:
                    var element = document.getElementsByTagName("body")[0];
                    this._Dom.setAttribute(element, attrName, attrValue);
                    break;

                default:
                    this._Dom.setAttribute(document.getElementById(elemId), attrName, attrValue);
                    break;
            }
        }
        return true;
    },

    /**
     * internal helper which raises an error in the
     * format we need for further processing
     *
     * @param message the message
     * @param title the title of the error (optional)
     * @param name the name of the error (optional)
     */
    _raiseError:function (error, message, caller, title, name) {
        var _Impl = this.attr("impl");
        var finalTitle = title || _Impl.MALFORMEDXML;
        var finalName = name || _Impl.MALFORMEDXML;
        var finalMessage = message || "";

        return this._Lang.makeException(error, finalTitle, finalName, this._nameSpace, caller || ( (arguments.caller) ? arguments.caller.toString() : "_raiseError"), finalMessage);
    }
});

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
 * @name _Transports
 * @memberOf myfaces._impl.xhrCore
 * @description
 *
 * The xhr core adapter
 * which provides the transport mechanisms to the calling
 * objects, and controls the queue behavior, the error handling
 * and partial page submit functionality among other things
 * <p />
 * The idea behind this is to make the ajax request object as barebones
 * as possible and shift the extra functionality like queuing
 * parameter handling etc... to this class so that our transports become more easily
 * pluggable. This should keep the path open to iframe submits and other transport layers
 * <p />
 * the call to the corresponding transport just should be a
 * transport.xhrQueuedPost <br />
 * or transport.xhrPost,transport.xhrGet  etc... in the future
 * <p />
 * Note we have taken a pattern lesson or two from the dojo toolkit and its excellent handling
 * of transports by our patterns here (which is mainly a centralized transport singleton which routes
 * to different transport implementations and the auto passing of parameters into their
 * corresponding protected attributes on class level in the transports themselves)
 */
_MF_SINGLTN(_PFX_XHR + "_Transports", _MF_OBJECT,
        /** @lends myfaces._impl.xhrCore._Transports.prototype */ {

    _PAR_ERRORLEVEL:"errorlevel",
    _PAR_QUEUESIZE:"queuesize",
    _PAR_PPS:"pps",
    _PAR_TIMEOUT:"timeout",
    _PAR_DELAY:"delay",


    /**
     * a singleton queue
     * note the structure of our inheritance
     * is that that _queue is attached to prototype
     * and hence the pointer to the request qeue
     * is shared over all instances
     *
     * if you need to have it per instance for complex objects
     * you have to initialize in the constructor
     *
     * (This is the same limitation dojo class inheritance
     * where our inheritance pattern is derived from has)
     */
    _q: new myfaces._impl.xhrCore._AjaxRequestQueue(),

    /**
     * xhr post with enqueuing as defined by the jsf 2.0 specification
     *
     * mapped options already have the exec and view properly in place
     * myfaces specifics can be found under mappedOptions.myFaces
     * @param {Node} source the source of this call
     * @param {Node} sourceForm the html form which is the source of this call
     * @param {Object} context (Map) the internal pass through context
     * @param {Object} passThrgh (Map) values to be passed through
     **/
    xhrQueuedPost : function(source, sourceForm, context, passThrgh) {
        context._mfInternal.xhrOp = "xhrQueuedPost";
        this._q.enqueue(
                new (this._getAjaxReqClass(context))(this._getArguments(source, sourceForm, context, passThrgh)));
    },

    /**
      * iframe queued post
      *
      * mapped options already have the exec and view properly in place
      * myfaces specifics can be found under mappedOptions.myFaces
      * @param {Node} source the source of this call
      * @param {Node} sourceForm the html form which is the source of this call
      * @param {Object} context (Map) the internal pass through context
      * @param {Object} passThrgh (Map) values to be passed through
      **/
     multipartQueuedPost : function(source, sourceForm, context, passThrgh) {
         context._mfInternal.xhrOp = "multipartQueuedPost";
         var args = this._getArguments(source, sourceForm, context, passThrgh);
         // note in get the timeout is not working delay however is and queue size as well
         // since there are no cross browser ways to resolve a timeout on xhr level
         this._q.enqueue(
                 new (this._getMultipartReqClass(context))(args));
     },


    /**
     * creates the arguments map and
     * fetches the config params in a proper way in to
     * deal with them in a flat way (from the nested context way)
     *
     * @param source the source of the request
     * @param sourceForm the sourceform
     * @param context   the context holding all values
     * @param passThrgh the passThrough values to be blended into the response
     */
    _getArguments: function(source, sourceForm, context, passThrgh) {
        var _RT = myfaces._impl.core._Runtime,
        /** @ignore */
             _Lang = myfaces._impl._util._Lang,
             applyCfg = _Lang.hitch(this, this._applyConfig),
            //RT does not have this references, hence no hitch needed
             getCfg = _RT.getLocalOrGlobalConfig,


            ret = {
                "source": source,
                "sourceForm": sourceForm,
                "context": context,
                "passThrough": passThrgh,
                "xhrQueue": this._q
            };

        //we now mix in the config settings which might either be set globally
        //or pushed in under the context myfaces.<contextValue> into the current request
        applyCfg(ret, context, "alarmThreshold", this._PAR_ERRORLEVEL);
        applyCfg(ret, context, "queueSize", this._PAR_QUEUESIZE);
        //TODO timeout probably not needed anymore
        applyCfg(ret, context, "timeout", this._PAR_TIMEOUT);
        //applyCfg(ret, context, "delay", this._PAR_DELAY);

        //now partial page submit needs a different treatment
        //since pps == execute strings
        if (getCfg(context, this._PAR_PPS, false)
                && _Lang.exists(passThrgh, myfaces._impl.core.Impl.P_EXECUTE)
                && passThrgh[myfaces._impl.core.Impl.P_EXECUTE].length > 0) {
            ret['partialIdsArray'] = passThrgh[myfaces._impl.core.Impl.P_EXECUTE].split(" ");
        }
        return ret;
    },

    /**
     * helper method to apply a config setting to our varargs param list
     *
     * @param destination the destination map to receive the setting
     * @param context the current context
     * @param destParm the destination param of the destination map
     * @param srcParm the source param which is the key to our config setting
     */
    _applyConfig: function(destination, context, destParm, srcParm) {
        var _RT = myfaces._impl.core._Runtime;
        /** @ignore */
        var _getConfig = _RT.getLocalOrGlobalConfig;
        if (_getConfig(context, srcParm, null) != null) {
            destination[destParm] = _getConfig(context, srcParm, null);
        }
    },

    /**
     * centralized transport switching helper
     * for the multipart submit case
     *
     * @param context the context which is passed down
     */
    _getMultipartReqClass: function(context) {
       if (this._RT.getXHRLvl() >= 2) {
            return myfaces._impl.xhrCore._MultipartAjaxRequestLevel2;
       } else {
            return myfaces._impl.xhrCore._IFrameRequest;
       }
    },


    _getAjaxReqClass: function(context) {
        // var _RT = myfaces._impl.core._Runtime;
        if(this._RT.getXHRLvl() < 2) {
           return myfaces._impl.xhrCore._AjaxRequest;
        } else {
           return myfaces._impl.xhrCore._AjaxRequestLevel2;
        }
    }

});

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
 * @name _Transports
 * @memberOf myfaces._impl.xhrCore
 * @description
 *
 * The xhr core adapter
 * which provides the transport mechanisms to the calling
 * objects, and controls the queue behavior, the error handling
 * and partial page submit functionality among other things
 * <p />
 * The idea behind this is to make the ajax request object as barebones
 * as possible and shift the extra functionality like queuing
 * parameter handling etc... to this class so that our transports become more easily
 * pluggable. This should keep the path open to iframe submits and other transport layers
 * <p />
 * the call to the corresponding transport just should be a
 * transport.xhrQueuedPost <br />
 * or transport.xhrPost,transport.xhrGet  etc... in the future
 * <p />
 * Note we have taken a pattern lesson or two from the dojo toolkit and its excellent handling
 * of transports by our patterns here (which is mainly a centralized transport singleton which routes
 * to different transport implementations and the auto passing of parameters into their
 * corresponding protected attributes on class level in the transports themselves)
 */
_MF_SINGLTN(_PFX_XHR+"_ExtTransports" , myfaces._impl.xhrCore._Transports,
     /** @lends myfaces._impl.xhrCore._Transports.prototype */ {


    constructor_: function() {
        this._callSuper("constructor_");
        myfaces._impl.xhrCore._Transports = this;
        //we change the transport to our new implementation
        this.updateSingletons("transport", this);
    },

    /**
     * a simple not enqueued xhr post
     *
     * mapped options already have the exec and view properly in place
     * myfaces specifics can be found under mappedOptions.myFaces
     * @param {Node} source the source of this call
     * @param {Node} sourceForm the html form which is the source of this call
     * @param {Object} context (Map) the internal pass through context
     * @param {Object} passThrgh (Map) values to be passed through
     **/
    xhrPost : function(source, sourceForm, context, passThrgh) {
        context._mfInternal.xhrOp = "xhrPost";
        var args = this._getArguments(source, sourceForm, context, passThrgh);
         delete args.xhrQueue;
        (new (this._getAjaxReqClass(context))(args)).send();
    },


    /**
     * xhr get without enqueuing
     *
     * mapped options already have the exec and view properly in place
     * myfaces specifics can be found under mappedOptions.myFaces
     * @param {Node} source the source of this call
     * @param {Node} sourceForm the html form which is the source of this call
     * @param {Object} context (Map) the internal pass through context
     * @param {Object} passThrgh (Map) values to be passed through
     **/
    xhrGet : function(source, sourceForm, context, passThrgh) {
        context._mfInternal.xhrOp = "xhrGet";
        var args = this._getArguments(source, sourceForm, context, passThrgh);
        // note in get the timeout is not working delay however is and queue size as well
        // since there are no cross browser ways to resolve a timeout on xhr level
        // we have to live with it
        args.ajaxType = "GET";
        delete args.xhrQueue;
        (new (this._getAjaxReqClass(context))(args)).send();
    },

    /**
     * xhr get which takes the existing queue into consideration to by synchronized
     * to previous queued post requests
     *
     * mapped options already have the exec and view properly in place
     * myfaces specifics can be found under mappedOptions.myFaces
     * @param {Node} source the source of this call
     * @param {Node} sourceForm the html form which is the source of this call
     * @param {Object} context (Map) the internal pass through context
     * @param {Object} passThrgh (Map) values to be passed through
     **/
    xhrQueuedGet : function(source, sourceForm, context, passThrgh) {
        context._mfInternal.xhrOp = "xhrQueuedGet";
        var args = this._getArguments(source, sourceForm, context, passThrgh);
        // note in get the timeout is not working delay however is and queue size as well
        // since there are no cross browser ways to resolve a timeout on xhr level
        // we have to live with it
        args.ajaxType = "GET";
        this._q.enqueue(
                new (this._getAjaxReqClass(context))(args));
    },


    /**
     * iframe post without queueing
     *
     * mapped options already have the exec and view properly in place
     * myfaces specifics can be found under mappedOptions.myFaces
     * @param {Node} source the source of this call
     * @param {Node} sourceForm the html form which is the source of this call
     * @param {Object} context (Map) the internal pass through context
     * @param {Object} passThrgh (Map) values to be passed through
     **/
    multipartPost : function(source, sourceForm, context, passThrgh) {
        context._mfInternal.xhrOp = "multipartPost";
        var args = this._getArguments(source, sourceForm, context, passThrgh);
        // note in get the timeout is not working delay however is and queue size as well
        // since there are no cross browser ways to resolve a timeout on xhr level
        // we have to live with it
        delete args.xhrQueue;
        (new (this._getMultipartReqClass(context))(args)).send();
    },


    /**
     * iframe get without queueing
     *
     * mapped options already have the exec and view properly in place
     * myfaces specifics can be found under mappedOptions.myFaces
     * @param {Node} source the source of this call
     * @param {Node} sourceForm the html form which is the source of this call
     * @param {Object} context (Map) the internal pass through context
     * @param {Object} passThrgh (Map) values to be passed through
     **/
    multipartGet : function(source, sourceForm, context, passThrgh) {
        context._mfInternal.xhrOp = "multiPartGet";
        var args = this._getArguments(source, sourceForm, context, passThrgh);
        // note in get the timeout is not working delay however is and queue size as well
        // since there are no cross browser ways to resolve a timeout on xhr level
        // we have to live with it
        args.ajaxType = "GET";
        delete args.xhrQueue;
        (new (this._getMultipartReqClass(context))(args)).send();
    },

    /**
     * iframe queued http get
     *
     * mapped options already have the exec and view properly in place
     * myfaces specifics can be found under mappedOptions.myFaces
     * @param {Node} source the source of this call
     * @param {Node} sourceForm the html form which is the source of this call
     * @param {Object} context (Map) the internal pass through context
     * @param {Object} passThrgh (Map) values to be passed through
     **/
    multipartQueuedGet : function(source, sourceForm, context, passThrgh) {
        context._mfInternal.xhrOp = "multipartQueuedGet";
        var args = this._getArguments(source, sourceForm, context, passThrgh);
        // note in get the timeout is not working delay however is and queue size as well
        // since there are no cross browser ways to resolve a timeout on xhr level
        args.ajaxType = "GET";
        this._q.enqueue(
                new (this._getMultipartReqClass(context))(args));
    }
});

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
 * @name Impl
 * @memberOf myfaces._impl.core
 * @description Implementation singleton which implements all interface method
 * defined by our jsf.js API
 * */
_MF_SINGLTN(_PFX_CORE + "Impl", _MF_OBJECT, /**  @lends myfaces._impl.core.Impl.prototype */ {

    //third option myfaces._impl.xhrCoreAjax which will be the new core impl for now
    _transport:myfaces._impl.core._Runtime.getGlobalConfig("transport", myfaces._impl.xhrCore._Transports),

    /**
     * external event listener queue!
     */
    _evtListeners:new (myfaces._impl.core._Runtime.getGlobalConfig("eventListenerQueue", myfaces._impl._util._ListenerQueue))(),

    /**
     * external error listener queue!
     */
    _errListeners:new (myfaces._impl.core._Runtime.getGlobalConfig("errorListenerQueue", myfaces._impl._util._ListenerQueue))(),

    /*CONSTANTS*/

    /*internal identifiers for options*/
    IDENT_ALL:"@all",
    IDENT_NONE:"@none",
    IDENT_THIS:"@this",
    IDENT_FORM:"@form",

    /*
     * [STATIC] constants
     */

    P_PARTIAL_SOURCE:"javax.faces.source",
    P_VIEWSTATE:"javax.faces.ViewState",
    P_CLIENTWINDOW:"javax.faces.ClientWindow",
    P_AJAX:"javax.faces.partial.ajax",
    P_EXECUTE:"javax.faces.partial.execute",
    P_RENDER:"javax.faces.partial.render",
    P_EVT:"javax.faces.partial.event",
    P_WINDOW_ID:"javax.faces.ClientWindow",
    P_RESET_VALUES:"javax.faces.partial.resetValues",

    /* message types */
    ERROR:"error",
    EVENT:"event",

    /* event emitting stages */
    BEGIN:"begin",
    COMPLETE:"complete",
    SUCCESS:"success",

    /*ajax errors spec 14.4.2*/
    HTTPERROR:"httpError",
    EMPTY_RESPONSE:"emptyResponse",
    MALFORMEDXML:"malformedXML",
    SERVER_ERROR:"serverError",
    CLIENT_ERROR:"clientError",
    TIMEOUT_EVENT:"timeout",

    /*error reporting threshold*/
    _threshold:"ERROR",

    /*blockfilter for the passthrough filtering, the attributes given here
     * will not be transmitted from the options into the passthrough*/
    _BLOCKFILTER:{onerror:1, onevent:1, render:1, execute:1, myfaces:1, delay:1, resetValues:1},

    /**
     * collect and encode data for a given form element (must be of type form)
     * find the javax.faces.ViewState element and encode its value as well!
     * return a concatenated string of the encoded values!
     *
     * @throws Error in case of the given element not being of type form!
     * https://issues.apache.org/jira/browse/MYFACES-2110
     */
    getViewState:function (form) {
        /**
         *  typecheck assert!, we opt for strong typing here
         *  because it makes it easier to detect bugs
         */
        if (form) {
            form = this._Lang.byId(form);
        }

        if (!form
                || !form.nodeName
                || form.nodeName.toLowerCase() != "form") {
            throw new Error(this._Lang.getMessage("ERR_VIEWSTATE"));
        }

        var ajaxUtils = myfaces._impl.xhrCore._AjaxUtils;

        var ret = this._Lang.createFormDataDecorator([]);
        ajaxUtils.encodeSubmittableFields(ret, form, null);

        return ret.makeFinal();
    },

    /**
     * this function has to send the ajax requests
     *
     * following request conditions must be met:
     * <ul>
     *  <li> the request must be sent asynchronously! </li>
     *  <li> the request must be a POST!!! request </li>
     *  <li> the request url must be the form action attribute </li>
     *  <li> all requests must be queued with a client side request queue to ensure the request ordering!</li>
     * </ul>
     *
     * @param {String|Node} elem any dom element no matter being it html or jsf, from which the event is emitted
     * @param {|Event|} event any javascript event supported by that object
     * @param {|Object|} options  map of options being pushed into the ajax cycle
     *
     *
     * a) transformArguments out of the function
     * b) passThrough handling with a map copy with a filter map block map
     */
    request:function (elem, event, options) {
        if (this._delayTimeout) {
            clearTimeout(this._delayTimeout);
            delete this._delayTimeout;
        }
        /*namespace remap for our local function context we mix the entire function namespace into
         *a local function variable so that we do not have to write the entire namespace
         *all the time
         **/
        var _Lang = this._Lang,
                _Dom = this._Dom;
        /*assert if the onerror is set and once if it is set it must be of type function*/
        _Lang.assertType(options.onerror, "function");
        /*assert if the onevent is set and once if it is set it must be of type function*/
        _Lang.assertType(options.onevent, "function");

        //options not set we define a default one with nothing
        options = options || {};

        /**
         * we cross reference statically hence the mapping here
         * the entire mapping between the functions is stateless
         */
        //null definitely means no event passed down so we skip the ie specific checks
        if ('undefined' == typeof event) {
            event = window.event || null;
        }

        //improve the error messages if an empty elem is passed
        if (!elem) {
            throw _Lang.makeException(new Error(), "ArgNotSet", null, this._nameSpace, "request", _Lang.getMessage("ERR_MUST_BE_PROVIDED1", "{0}: source  must be provided", "jsf.ajax.request", "source element id"));
        }
        var oldElem = elem;
        elem = _Dom.byIdOrName(elem);
        if (!elem) {
            throw _Lang.makeException(new Error(), "Notfound", null, this._nameSpace, "request", _Lang.getMessage("ERR_PPR_UNKNOWNCID", "{0}: Node with id {1} could not be found from source", this._nameSpace + ".request", oldElem));
        }

        var elementId = _Dom.nodeIdOrName(elem);

        /*
         * We make a copy of our options because
         * we should not touch the incoming params!
         * this copy is also the pass through parameters
         * which are sent down our request
         */
        var passThrgh = _Lang.mixMaps({}, options, true, this._BLOCKFILTER);

        if (event) {
            passThrgh[this.P_EVT] = event.type;
        }

        /**
         * ajax pass through context with the source
         * onevent and onerror
         */
        var context = {
            source:elem,
            onevent:options.onevent,
            onerror:options.onerror,

            //TODO move the myfaces part into the _mfInternal part
            myfaces:options.myfaces,
            _mfInternal:{}
        };
        //additional meta information to speed things up, note internal non jsf
        //pass through options are stored under _mfInternal in the context
        var mfInternal = context._mfInternal;

        /**
         * fetch the parent form
         *
         * note we also add an override possibility here
         * so that people can use dummy forms and work
         * with detached objects
         */
        var form = (options.myfaces && options.myfaces.form) ?
                _Lang.byId(options.myfaces.form) :
                this._getForm(elem, event);

        /**
         * JSF2.2 client window must be part of the issuing form so it is encoded
         * automatically in the request
         */
        //we set the client window before encoding by a call to jsf.getClientWindow
        var clientWindow = jsf.getClientWindow(form);
        //in case someone decorates the getClientWindow we reset the value from
        //what we are getting
        if ('undefined' != typeof clientWindow && null != clientWindow) {
            var formElem = _Dom.getNamedElementFromForm(form, this.P_CLIENTWINDOW);
            if (formElem) {
                //we store the value for later processing during the ajax phase
                //job so that we do not get double values
                context._mfInternal._clientWindow = jsf.getClientWindow(form);
            } else {
                passThrgh[this.P_CLIENTWINDOW] = jsf.getClientWindow(form);
            }
        } /*  spec proposal
        else {
            var formElem = _Dom.getNamedElementFromForm(form, this.P_CLIENTWINDOW);
            if (formElem) {
                context._mfInternal._clientWindow = "undefined";
            } else {
                passThrgh[this.P_CLIENTWINDOW] = "undefined";
            }
        }
        */

        /**
         * binding contract the javax.faces.source must be set
         */
        passThrgh[this.P_PARTIAL_SOURCE] = elementId;

        /**
         * javax.faces.partial.ajax must be set to true
         */
        passThrgh[this.P_AJAX] = true;

        /**
         * if resetValues is set to true
         * then we have to set javax.faces.resetValues as well
         * as pass through parameter
         * the value has to be explicitly true, according to
         * the specs jsdoc
         */
        if(options.resetValues === true) {
            passThrgh[this.P_RESET_VALUES] = true;
        }

        if (options.execute) {
            /*the options must be a blank delimited list of strings*/
            /*compliance with Mojarra which automatically adds @this to an execute
             * the spec rev 2.0a however states, if none is issued nothing at all should be sent down
             */
            options.execute = (options.execute.indexOf("@this") == -1) ? options.execute : options.execute;

            this._transformList(passThrgh, this.P_EXECUTE, options.execute, form, elementId);
        } else {
            passThrgh[this.P_EXECUTE] = elementId;
        }

        if (options.render) {
            this._transformList(passThrgh, this.P_RENDER, options.render, form, elementId);
        }

        /**
         * multiple transports upcoming jsf 2.x feature currently allowed
         * default (no value) xhrQueuedPost
         *
         * xhrQueuedPost
         * xhrPost
         * xhrGet
         * xhrQueuedGet
         * iframePost
         * iframeQueuedPost
         *
         */
        var transportType = this._getTransportType(context, passThrgh, form);

        mfInternal["_mfSourceFormId"] = form.id;
        mfInternal["_mfSourceControlId"] = elementId;
        mfInternal["_mfTransportType"] = transportType;

        //mojarra compatibility, mojarra is sending the form id as well
        //this is not documented behavior but can be determined by running
        //mojarra under blackbox conditions
        //i assume it does the same as our formId_submit=1 so leaving it out
        //wont hurt but for the sake of compatibility we are going to add it
        passThrgh[form.id] = form.id;

        /* jsf2.2 only: options.delay || */
        var delayTimeout = options.delay || this._RT.getLocalOrGlobalConfig(context, "delay", false);
        if (delayTimeout) {
            if (this._delayTimeout) {
                clearTimeout(this._delayTimeout);
            }
            this._delayTimeout = setTimeout(_Lang.hitch(this, function () {
                this._transport[transportType](elem, form, context, passThrgh);
                this._delayTimeout = null;
            }), parseInt(delayTimeout));
        } else {
            this._transport[transportType](elem, form, context, passThrgh);
        }
    },

    /**
     * fetches the form in an unprecise manner depending
     * on an element or event target
     *
     * @param elem
     * @param event
     */
    _getForm:function (elem, event) {
        var _Dom = this._Dom;
        var _Lang = this._Lang;
        var form = _Dom.fuzzyFormDetection(elem);

        if (!form && event) {
            //in case of no form is given we retry over the issuing event
            form = _Dom.fuzzyFormDetection(_Lang.getEventTarget(event));
            if (!form) {
                throw _Lang.makeException(new Error(), null, null, this._nameSpace, "_getForm", _Lang.getMessage("ERR_FORM"));
            }
        } else if (!form) {
            throw _Lang.makeException(new Error(), null, null, this._nameSpace, "_getForm", _Lang.getMessage("ERR_FORM"));

        }
        return form;
    },

    /**
     * determines the transport type to be called
     * for the ajax call
     *
     * @param context the context
     * @param passThrgh  pass through values
     * @param form the form which issues the request
     */
    _getTransportType:function (context, passThrgh, form) {
        /**
         * if execute or render exist
         * we have to pass them down as a blank delimited string representation
         * of an array of ids!
         */
        //for now we turn off the transport auto selection, to enable 2.0 backwards compatibility
        //on protocol level, the file upload only can be turned on if the auto selection is set to true
        var getConfig = this._RT.getLocalOrGlobalConfig,
                _Lang = this._Lang,
                _Dom = this._Dom;

        var transportAutoSelection = getConfig(context, "transportAutoSelection", true);
        /*var isMultipart = (transportAutoSelection && _Dom.getAttribute(form, "enctype") == "multipart/form-data") ?
         _Dom.isMultipartCandidate((!getConfig(context, "pps",false))? form : passThrgh[this.P_EXECUTE]) :
         false;
         **/
        if (!transportAutoSelection) {
            return getConfig(context, "transportType", "xhrQueuedPost");
        }
        var multiPartCandidate = _Dom.isMultipartCandidate((!getConfig(context, "pps", false)) ?
                form : passThrgh[this.P_EXECUTE]);
        var multipartForm = (_Dom.getAttribute(form, "enctype") || "").toLowerCase() == "multipart/form-data";
        //spec section jsdoc, if we have a multipart candidate in our execute (aka fileupload)
        //and the form is not multipart then we have to raise an error
        if (multiPartCandidate && !multipartForm) {
            throw _Lang.makeException(new Error(), null, null, this._nameSpace, "_getTransportType", _Lang.getMessage("ERR_NO_MULTIPART_FORM", "No Multipart form", form.id));
        }
        var isMultipart = multiPartCandidate && multipartForm;
        /**
         * multiple transports upcoming jsf 2.2 feature currently allowed
         * default (no value) xhrQueuedPost
         *
         * xhrQueuedPost
         * xhrPost
         * xhrGet
         * xhrQueuedGet
         * iframePost
         * iframeQueuedPost
         *
         */
        var transportType = (!isMultipart) ?
                getConfig(context, "transportType", "xhrQueuedPost") :
                getConfig(context, "transportType", "multipartQueuedPost");
        if (!this._transport[transportType]) {
            //throw new Error("Transport type " + transportType + " does not exist");
            throw new Error(_Lang.getMessage("ERR_TRANSPORT", null, transportType));
        }
        return transportType;

    },

    /**
     * transforms the list to the expected one
     * with the proper none all form and this handling
     * (note we also could use a simple string replace but then
     * we would have had double entries under some circumstances)
     *
     * @param passThrgh
     * @param target
     * @param srcStr
     * @param form
     * @param elementId
     */
    _transformList:function (passThrgh, target, srcStr, form, elementId) {
        var _Lang = this._Lang;
        //this is probably the fastest transformation method
        //it uses an array and an index to position all elements correctly
        //the offset variable is there to prevent 0 which results in a javascript
        //false
        srcStr = this._Lang.trim(srcStr);
        var offset = 1,
                vals = (srcStr) ? srcStr.split(/\s+/) : [],
                idIdx = (vals.length) ? _Lang.arrToMap(vals, offset) : {},

        //helpers to improve speed and compression
                none = idIdx[this.IDENT_NONE],
                all = idIdx[this.IDENT_ALL],
                theThis = idIdx[this.IDENT_THIS],
                theForm = idIdx[this.IDENT_FORM];

        if (none) {
            //in case of none nothing is returned
            if ('undefined' != typeof passThrgh.target) {
                delete passThrgh.target;
            }
            return passThrgh;
        }
        if (all) {
            //in case of all only one value is returned
            passThrgh[target] = this.IDENT_ALL;
            return passThrgh;
        }

        if (theForm) {
            //the form is replaced with the proper id but the other
            //values are not touched
            vals[theForm - offset] = form.id;
        }
        if (theThis && !idIdx[elementId]) {
            //in case of this, the element id is set
            vals[theThis - offset] = elementId;
        }

        //the final list must be blank separated
        passThrgh[target] = vals.join(" ");
        return passThrgh;
    },

    addOnError:function (/*function*/errorListener) {
        /*error handling already done in the assert of the queue*/
        this._errListeners.enqueue(errorListener);
    },

    addOnEvent:function (/*function*/eventListener) {
        /*error handling already done in the assert of the queue*/
        this._evtListeners.enqueue(eventListener);
    },

    /**
     * implementation triggering the error chain
     *
     * @param {Object} request the request object which comes from the xhr cycle
     * @param {Object} context (Map) the context object being pushed over the xhr cycle keeping additional metadata
     * @param {String} name the error name
     * @param {String} serverErrorName the server error name in case of a server error
     * @param {String} serverErrorMessage the server error message in case of a server error
     * @param {String} caller optional caller reference for extended error messages
     * @param {String} callFunc optional caller Function reference for extended error messages
     *
     *  handles the errors, in case of an onError exists within the context the onError is called as local error handler
     *  the registered error handlers in the queue receiv an error message to be dealt with
     *  and if the projectStage is at development an alert box is displayed
     *
     *  note: we have additional functionality here, via the global config myfaces.config.defaultErrorOutput a function can be provided
     *  which changes the default output behavior from alert to something else
     *
     *
     */
    sendError:function sendError(/*Object*/request, /*Object*/ context, /*String*/ name, /*String*/ serverErrorName, /*String*/ serverErrorMessage, caller, callFunc) {
        var _Lang = myfaces._impl._util._Lang;
        var UNKNOWN = _Lang.getMessage("UNKNOWN");

        var eventData = {};
        //we keep this in a closure because we might reuse it for our serverErrorMessage
        var malFormedMessage = function () {
            return (name && name === myfaces._impl.core.Impl.MALFORMEDXML) ? _Lang.getMessage("ERR_MALFORMEDXML") : "";
        };

        //by setting unknown values to unknown we can handle cases
        //better where a simulated context is pushed into the system
        eventData.type = this.ERROR;

        eventData.status = name || UNKNOWN;
        eventData.serverErrorName = serverErrorName || UNKNOWN;
        eventData.serverErrorMessage = serverErrorMessage || UNKNOWN;

        try {
            eventData.source = context.source || UNKNOWN;
            eventData.responseCode = request.status || UNKNOWN;
            eventData.responseText = request.responseText || UNKNOWN;
            eventData.responseXML = request.responseXML || UNKNOWN;
        } catch (e) {
            // silently ignore: user can find out by examining the event data
        }
        //extended error message only in dev mode
        if (jsf.getProjectStage() === "Development") {
            eventData.serverErrorMessage = eventData.serverErrorMessage || "";
            eventData.serverErrorMessage = (caller) ? eventData.serverErrorMessage + "\nCalling class: " + caller : eventData.serverErrorMessage;
            eventData.serverErrorMessage = (callFunc) ? eventData.serverErrorMessage + "\n Calling function: " + callFunc : eventData.serverErrorMessage;
        }

        /**/
        if (context["onerror"]) {
            context.onerror(eventData);
        }

        /*now we serve the queue as well*/
        this._errListeners.broadcastEvent(eventData);

        if (jsf.getProjectStage() === "Development" && this._errListeners.length() == 0 && !context["onerror"]) {
            var DIVIDER = "--------------------------------------------------------",
                    defaultErrorOutput = myfaces._impl.core._Runtime.getGlobalConfig("defaultErrorOutput", alert),
                    finalMessage = [],
            //we remap the function to achieve a better compressability
                    pushMsg = _Lang.hitch(finalMessage, finalMessage.push);

            (serverErrorMessage) ? pushMsg(_Lang.getMessage("MSG_ERROR_MESSAGE") + " " + serverErrorMessage + "\n") : null;

            pushMsg(DIVIDER);

            (caller) ? pushMsg("Calling class:" + caller) : null;
            (callFunc) ? pushMsg("Calling function:" + callFunc) : null;
            (name) ? pushMsg(_Lang.getMessage("MSG_ERROR_NAME") + " " + name) : null;
            (serverErrorName && name != serverErrorName) ? pushMsg("Server error name: " + serverErrorName) : null;

            pushMsg(malFormedMessage());
            pushMsg(DIVIDER);
            pushMsg(_Lang.getMessage("MSG_DEV_MODE"));
            defaultErrorOutput(finalMessage.join("\n"));
        }
    },

    /**
     * sends an event
     */
    sendEvent:function sendEvent(/*Object*/request, /*Object*/ context, /*event name*/ name) {
        var _Lang = myfaces._impl._util._Lang;
        var eventData = {};
        var UNKNOWN = _Lang.getMessage("UNKNOWN");

        eventData.type = this.EVENT;

        eventData.status = name;
        eventData.source = context.source;

        if (name !== this.BEGIN) {

            try {
                //we bypass a problem with ie here, ie throws an exception if no status is given on the xhr object instead of just passing a value
                var getValue = function (value, key) {
                    try {
                        return value[key]
                    } catch (e) {
                        return UNKNOWN;
                    }
                };

                eventData.responseCode = getValue(request, "status");
                eventData.responseText = getValue(request, "responseText");
                eventData.responseXML = getValue(request, "responseXML");

            } catch (e) {
                var impl = myfaces._impl.core._Runtime.getGlobalConfig("jsfAjaxImpl", myfaces._impl.core.Impl);
                impl.sendError(request, context, this.CLIENT_ERROR, "ErrorRetrievingResponse",
                        _Lang.getMessage("ERR_CONSTRUCT", e.toString()));

                //client errors are not swallowed
                throw e;
            }

        }

        /**/
        if (context.onevent) {
            /*calling null to preserve the original scope*/
            context.onevent.call(null, eventData);
        }

        /*now we serve the queue as well*/
        this._evtListeners.broadcastEvent(eventData);
    },

    /**
     * Spec. 13.3.3
     * Examining the response markup and updating the DOM tree
     * @param {XMLHttpRequest} request - the ajax request
     * @param {Object} context - the ajax context
     */
    response:function (request, context) {
        this._RT.getLocalOrGlobalConfig(context, "responseHandler", myfaces._impl.xhrCore._AjaxResponse).processResponse(request, context);
    },

    /**
     * fetches the separator char from the given script tags
     *
     * @return {char} the separator char for the given script tags
     */
    getSeparatorChar:function () {
        if (this._separator) {
            return this.separatorchar;
        }
        var SEPARATOR_CHAR = "separatorchar",
                found = false,
                getConfig = myfaces._impl.core._Runtime.getGlobalConfig,
                scriptTags = document.getElementsByTagName("script");
        for (var i = 0; i < scriptTags.length && !found; i++) {
            if (scriptTags[i].src.search(/\/javax\.faces\.resource.*\/jsf\.js.*separator/) != -1) {
                found = true;
                var result = scriptTags[i].src.match(/separator=([^&;]*)/);
                this._separator = decodeURIComponent(result[1]);
            }
        }
        this._separator = getConfig(SEPARATOR_CHAR, this._separator || ":");
        return this._separator;
    },

    /**
     * @return the project stage also emitted by the server:
     * it cannot be cached and must be delivered over the server
     * The value for it comes from the request parameter of the jsf.js script called "stage".
     */
    getProjectStage:function () {
        //since impl is a singleton we only have to do it once at first access

        if (!this._projectStage) {
            var PRJ_STAGE = "projectStage",
                    STG_PROD = "Production",

                    scriptTags = document.getElementsByTagName("script"),
                    getConfig = myfaces._impl.core._Runtime.getGlobalConfig,
                    projectStage = null,
                    found = false,
                    allowedProjectStages = {STG_PROD:1, "Development":1, "SystemTest":1, "UnitTest":1};

            /* run through all script tags and try to find the one that includes jsf.js */
            for (var i = 0; i < scriptTags.length && !found; i++) {
                if (scriptTags[i].src.search(/\/javax\.faces\.resource\/jsf\.js.*ln=javax\.faces/) != -1) {
                    var result = scriptTags[i].src.match(/stage=([^&;]*)/);
                    found = true;
                    if (result) {
                        // we found stage=XXX
                        // return only valid values of ProjectStage
                        projectStage = (allowedProjectStages[result[1]]) ? result[1] : null;

                    }
                    else {
                        //we found the script, but there was no stage parameter -- Production
                        //(we also add an override here for testing purposes, the default, however is Production)
                        projectStage = getConfig(PRJ_STAGE, STG_PROD);
                    }
                }
            }
            /* we could not find anything valid --> return the default value */
            this._projectStage = getConfig(PRJ_STAGE, projectStage || STG_PROD);
        }
        return this._projectStage;
    },

    /**
     * implementation of the external chain function
     * moved into the impl
     *
     *  @param {Object} source the source which also becomes
     * the scope for the calling function (unspecified side behavior)
     * the spec states here that the source can be any arbitrary code block.
     * Which means it either is a javascript function directly passed or a code block
     * which has to be evaluated separately.
     *
     * After revisiting the code additional testing against components showed that
     * the this parameter is only targeted at the component triggering the eval
     * (event) if a string code block is passed. This is behavior we have to resemble
     * in our function here as well, I guess.
     *
     * @param {Event} event the event object being passed down into the the chain as event origin
     *   the spec is contradicting here, it on one hand defines event, and on the other
     *   it says it is optional, after asking, it meant that event must be passed down
     *   but can be undefined
     */
    chain:function (source, event) {
        var len = arguments.length;
        var _Lang = this._Lang;
        var throwErr = function (msgKey) {
            throw Error("jsf.util.chain: " + _Lang.getMessage(msgKey));
        };
        /**
         * generic error condition checker which raises
         * an exception if the condition is met
         * @param assertion
         * @param message
         */
        var errorCondition = function (assertion, message) {
            if (assertion === true) throwErr(message);
        };
        var FUNC = 'function';
        var ISSTR = _Lang.isString;

        //the spec is contradicting here, it on one hand defines event, and on the other
        //it says it is optional, I have cleared this up now
        //the spec meant the param must be passed down, but can be 'undefined'

        errorCondition(len < 2, "ERR_EV_OR_UNKNOWN");
        errorCondition(len < 3 && (FUNC == typeof event || ISSTR(event)), "ERR_EVT_PASS");
        if (len < 3) {
            //nothing to be done here, move along
            return true;
        }
        //now we fetch from what is given from the parameter list
        //we cannot work with splice here in any performant way so we do it the hard way
        //arguments only are give if not set to undefined even null values!

        //assertions source either null or set as dom element:
        errorCondition('undefined' == typeof source, "ERR_SOURCE_DEF_NULL");
        errorCondition(FUNC == typeof source, "ERR_SOURCE_FUNC");
        errorCondition(ISSTR(source), "ERR_SOURCE_NOSTR");

        //assertion if event is a function or a string we already are in our function elements
        //since event either is undefined, null or a valid event object
        errorCondition(FUNC == typeof event || ISSTR(event), "ERR_EV_OR_UNKNOWN");

        for (var cnt = 2; cnt < len; cnt++) {
            //we do not change the scope of the incoming functions
            //but we reuse the argument array capabilities of apply
            var ret;

            if (FUNC == typeof arguments[cnt]) {
                ret = arguments[cnt].call(source, event);
            } else {
                //either a function or a string can be passed in case of a string we have to wrap it into another function
                ret = new Function("event", arguments[cnt]).call(source, event);
            }
            //now if one function returns false in between we stop the execution of the cycle
            //here, note we do a strong comparison here to avoid constructs like 'false' or null triggering
            if (ret === false /*undefined check implicitly done here by using a strong compare*/) {
                return false;
            }
        }
        return true;
    },

    /**
     * error handler behavior called internally
     * and only into the impl it takes care of the
     * internal message transformation to a myfaces internal error
     * and then uses the standard send error mechanisms
     * also a double error logging prevention is done as well
     *
     * @param request the request currently being processed
     * @param context the context affected by this error
     * @param exception the exception being thrown
     */
    stdErrorHandler:function (request, context, exception) {
        //newer browsers do not allow to hold additional values on native objects like exceptions
        //we hence capsule it into the request, which is gced automatically
        //on ie as well, since the stdErrorHandler usually is called between requests
        //this is a valid approach
        if (this._threshold == "ERROR") {
            var mfInternal = exception._mfInternal || {};

            var finalMsg = [];
            finalMsg.push(exception.message);
            this.sendError(request, context,
                    mfInternal.title || this.CLIENT_ERROR, mfInternal.name || exception.name, finalMsg.join("\n"), mfInternal.caller, mfInternal.callFunc);
        }
    },

    /**
     * @return the client window id of the current window, if one is given
     */
    getClientWindow:function (node) {
        var fetchWindowIdFromForms = this._Lang.hitch(this, function (forms) {
            var result_idx = {};
            var result;
            var foundCnt = 0;
            for (var cnt = forms.length - 1; cnt >= 0; cnt--) {

                var currentForm = forms[cnt];
                var winIdElement = this._Dom.getNamedElementFromForm(currentForm, this.P_WINDOW_ID);
                var windowId = (winIdElement) ? winIdElement.value : null;

                if (windowId) {
                    if (foundCnt > 0 && "undefined" == typeof result_idx[windowId]) throw Error("Multiple different windowIds found in document");
                    result = windowId;
                    result_idx[windowId] = true;
                    foundCnt++;
                }
            }
            return result;
        });

        var fetchWindowIdFromURL = function () {
            var href = window.location.href, windowId = "jfwid";
            var regex = new RegExp("[\\?&]" + windowId + "=([^&#\\;]*)");
            var results = regex.exec(href);
            //initial trial over the url and a regexp
            if (results != null) return results[1];
            return null;
        };

        //byId ($)
        var finalNode = (node) ? this._Dom.byId(node) : document.body;

        var forms = this._Dom.findByTagName(finalNode, "form");
        var result = fetchWindowIdFromForms(forms);
        return (null != result) ? result : fetchWindowIdFromURL();
    }
});



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
/*last file loaded, must restore the state of affairs*/
(function() {
    //some mobile browsers do not have a window object
    var target = window || document;

    var resetAbbreviation = function (name) {
        var _temp = target.myfaces._implTemp;
        (!!_temp[name]) ?
                target[name] = _temp[name] : null;
    },
            resetArr = ["_MF_CLS",
                        "_MF_SINGLTN",
                        "_MF_OBJECT",
                        "_PFX_UTIL",
                        "_PFX_XHR",
                        "_PFX_CORE",
                        "_PFX_I18N"];
    for (var cnt = resetArr.length - 1; cnt >= 0; cnt--) {
        resetAbbreviation(resetArr[cnt]);
    }
})();



/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
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
 *MyFaces core javascripting libraries
 *
 *  Those are the central public API functions in the JSF2
 *  Ajax API! They handle the entire form submit and ajax send
 *  and resolve cycle!
 */

/**
 * reserve the root namespace
 */
if ('undefined' != typeof OpenAjax && ('undefined' == typeof jsf || null == typeof jsf)) {
    OpenAjax.hub.registerLibrary("jsf", "www.sun.com", "1.0", null);
}
//just in case openajax has failed (testing environment)
/**
* @ignore
*/
if (!window.jsf) {
	/**
	* @namespace jsf
	*/
    var jsf = new function() {
        /*
         * Version of the implementation for the jsf.js.
         * <p />
         * as specified within the jsf specifications jsf.html:
         * <ul>
         * <li>left two digits major release number</li>
         * <li>middle two digits minor spec release number</li>
         * <li>right two digits bug release number</li>
         * </ul>
		 * @constant
         */
        this.specversion = 220000;
        /**
         * Implementation version as specified within the jsf specification.
         * <p />
         * A number increased with every implementation version
         * and reset by moving to a new spec release number
         *
		 * @constant
         */
        this.implversion = 0;

        /**
         * SeparatorChar as defined by UINamingContainer.getNamingContainerSeparatorChar()
         * @type {Char}
         */
        this.separatorchar = getSeparatorChar();

        /**
         * This method is responsible for the return of a given project stage as defined
         * by the jsf specification.
         * <p/>
         * Valid return values are:
         * <ul>
         *     <li>&quot;Production&quot;</li>
         *     <li>&quot;Development&quot;</li>
         *     <li>&quot;SystemTest&quot;</li>
         *     <li>&quot;UnitTest&quot;</li>
         * </li>
         *
         * @return {String} the current project state emitted by the server side method:
         * <i>javax.faces.application.Application.getProjectStage()</i>
         */
        this.getProjectStage = function() {
            var impl = myfaces._impl.core._Runtime.getGlobalConfig("jsfAjaxImpl", myfaces._impl.core.Impl);
            return impl.getProjectStage();
        };

        /**
         * collect and encode data for a given form element (must be of type form)
         * find the javax.faces.ViewState element and encode its value as well!
         * return a concatenated string of the encoded values!
         *
         * @throws an exception in case of the given element not being of type form!
         * https://issues.apache.org/jira/browse/MYFACES-2110
         */
        this.getViewState = function(formElement) {
            /*we are not allowed to add the impl on a global scope so we have to inline the code*/
            var impl = myfaces._impl.core._Runtime.getGlobalConfig("jsfAjaxImpl", myfaces._impl.core.Impl);
            return impl.getViewState(formElement);
        };

        /**
         * returns the window identifier for the given node / window
         * @param {optional String | DomNode}  the node for which the client identifier has to be determined
         * @return the window identifier or null if none is found
         */
        this.getClientWindow = function() {
            /*we are not allowed to add the impl on a global scope so we have to inline the code*/
            var impl = myfaces._impl.core._Runtime.getGlobalConfig("jsfAjaxImpl", myfaces._impl.core.Impl);
            return (arguments.length)? impl.getClientWindow(arguments[0]) : impl.getClientWindow();
        }

        //private helper functions
        function getSeparatorChar() {
            var impl = myfaces._impl.core._Runtime.getGlobalConfig("jsfAjaxImpl", myfaces._impl.core.Impl);
            return impl.getSeparatorChar();
        }

    };
	//jsdoc helper to avoid warnings, we map later 
	window.jsf = jsf;
}

/**
 * just to make sure no questions arise, I simply prefer here a weak
 * typeless comparison just in case some frameworks try to interfere
 * by overriding null or fiddeling around with undefined or typeof in some ways
 * it is safer in this case than the standard way of doing a strong comparison
 **/
if (!jsf.ajax) {
	/**
	* @namespace jsf.ajax
	*/
    jsf.ajax = new function() {


        /**
         * this function has to send the ajax requests
         *
         * following request conditions must be met:
         * <ul>
         *  <li> the request must be sent asynchronously! </li>
         *  <li> the request must be a POST!!! request </li>
         *  <li> the request url must be the form action attribute </li>
         *  <li> all requests must be queued with a client side request queue to ensure the request ordering!</li>
         * </ul>
         *
         * @param {String|Node} element: any dom element no matter being it html or jsf, from which the event is emitted
         * @param {EVENT} event: any javascript event supported by that object
         * @param {Map} options : map of options being pushed into the ajax cycle
         */
        this.request = function(element, event, options) {
            if (!options) {
                options = {};
            }
            /*we are not allowed to add the impl on a global scope so we have to inline the code*/
            var impl = myfaces._impl.core._Runtime.getGlobalConfig("jsfAjaxImpl", myfaces._impl.core.Impl);
            return impl.request(element, event, options);
        };

		/**
		* Adds an error handler to our global error queue.
		* the error handler must be of the format <i>function errorListener(&lt;errorData&gt;)</i>
		* with errorData being of following format:
		* <ul>
         *     <li> errorData.type : &quot;error&quot;</li>
         *     <li> errorData.status : the error status message</li>
         *     <li> errorData.serverErrorName : the server error name in case of a server error</li>
         *     <li> errorData.serverErrorMessage : the server error message in case of a server error</li>
         *     <li> errorData.source  : the issuing source element which triggered the request </li>
         *     <li> eventData.responseCode: the response code (aka http request response code, 401 etc...) </li>
         *     <li> eventData.responseText: the request response text </li>
         *     <li> eventData.responseXML: the request response xml </li>
        * </ul>
         *
         * @param {function} errorListener error handler must be of the format <i>function errorListener(&lt;errorData&gt;)</i>
		*/
        this.addOnError = function(/*function*/errorListener) {
            var impl = myfaces._impl.core._Runtime.getGlobalConfig("jsfAjaxImpl", myfaces._impl.core.Impl);
            return impl.addOnError(errorListener);
        };

        /**
         * Adds a global event listener to the ajax event queue. The event listener must be a function
         * of following format: <i>function eventListener(&lt;eventData&gt;)</i>
         *
         * @param {function} eventListener event must be of the format <i>function eventListener(&lt;eventData&gt;)</i>
         */
        this.addOnEvent = function(/*function*/eventListener) {
            var impl = myfaces._impl.core._Runtime.getGlobalConfig("jsfAjaxImpl", myfaces._impl.core.Impl);
            return impl.addOnEvent(eventListener);
        };

        /**
         * processes the ajax response if the ajax request completes successfully
         * @param request the ajax request!
         * @param context the ajax context!
         */
        this.response = function(/*xhr request object*/request, context) {
            var impl = myfaces._impl.core._Runtime.getGlobalConfig("jsfAjaxImpl", myfaces._impl.core.Impl);
            return impl.response(request, context);
        };
    }
}

if (!jsf.util) {
	/**
	* @namespace jsf.util
	*/
    jsf.util = new function() {

        /**
         * varargs function which executes a chain of code (functions or any other code)
         *
         * if any of the code returns false, the execution
         * is terminated prematurely skipping the rest of the code!
         *
         * @param {DomNode} source, the callee object
         * @param {Event} event, the event object of the callee event triggering this function
         * @param {optional} functions to be chained, if any of those return false the chain is broken
         */
        this.chain = function(source, event) {
            var impl = myfaces._impl.core._Runtime.getGlobalConfig("jsfAjaxImpl", myfaces._impl.core.Impl);
            return impl.chain.apply(impl, arguments);
        };
    }
}



