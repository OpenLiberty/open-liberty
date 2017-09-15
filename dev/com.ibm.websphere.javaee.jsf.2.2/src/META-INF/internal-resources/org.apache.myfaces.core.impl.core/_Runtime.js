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

