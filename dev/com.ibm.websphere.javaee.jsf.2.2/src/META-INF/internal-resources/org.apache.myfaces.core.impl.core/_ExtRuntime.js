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
 * Runtime Extension which externalizes code not used in the core but in the extension classes
 */

/**
 * A dojo like require to load scripts dynamically, note
 * to use this mechanism you have to set your global config param
 * myfacesScriptRoot to the root of your script files (aka under normal circumstances
 * resources/scripts)
 *
 * @param {String} nms the subnamespace to be required
 */
(function() {

    var _T = myfaces._impl.core._Runtime;

    _T.require = function (nms) {
        //namespace exists
        if (_T.exists(nms)) return;
        var rootPath = _T.getGlobalConfig("myfacesScriptRoot", "");
        this.loadScriptEval(rootPath + "/" + nms.replace(/\./g, "/") + ".js");
    };

    /**
     * delegation pattern
     * usage:
     * this.delegateObject("my.name.space", delegate,
     * {
     *  constructor_ :function(bla, bla1) {
     *      _T._callDelegate("constructor", bla1);
     *  },
     *  myFunc: function(yyy) {
     *      DoSomething;
     *      _T._callDelegate("someOtherFunc", yyyy);
     *  }, null
     * });
     *
     * or
     * usage var newClass = this.delegateObject(
     * function (var1, var2) {
     *  _T._callDelegate("constructor", var1,var2);
     * };
     * ,delegateObject);
     * newClass.prototype.myMethod = function(arg1) {
     *      _T._callDelegate("myMethod", arg1,"hello world");
     *
     *
     * @param {String} newCls the new class name to be generated
     * @param {Object} delegateObj the delegation object
     * @param {Object} protoFuncs the prototype functions which should be attached
     * @param {Object} nmsFuncs the namespace functions which should be attached to the namespace
     */
    _T.delegateObj = function (newCls, delegateObj, protoFuncs, nmsFuncs) {
        if (!_T.isString(newCls)) {
            throw Error("new class namespace must be of type String");
        }

        if ('function' != typeof newCls) {
            newCls = _T._reserveClsNms(newCls, protoFuncs);
            if (!newCls) return null;
        }

        //central delegation mapping core
        var proto = newCls.prototype;

        //the trick here is to isolate the entries to bind the
        //keys in a private scope see
        //http://www.ruzee.com/blog/2008/12/javascript-inheritance-via-prototypes-and-closures
        for (var key in delegateObj) (function (key, delFn) {
            //The isolation is needed otherwise the last _key assigend would be picked
            //up internally
            if (key && typeof delFn == "function") {
                proto[key] = function (/*arguments*/) {
                    return delFn.apply(delegateObj, arguments);
                };
            }
        })(key, delegateObj[key]);

        proto._delegateObj = delegateObj;
        proto.constructor = newCls;

        proto._callDelegate = function (methodName) {
            var passThrough = (arguments.length == 1) ? [] : Array.prototype.slice.call(arguments, 1);
            var ret = this._delegateObj[methodName].apply(this._delegateObj, passThrough);
            if ('undefined' != ret) return ret;
        };

        //we now map the function map in
        _T._applyFuncs(newCls, protoFuncs, true);
        _T._applyFuncs(newCls, nmsFuncs, false);

        return newCls;
    };


    /**
     * convenience method which basically replaces an existing class
     * with a new one under the same namespace, note all old functionality will be
     * presereced by pushing the original class into an new nampespace
     *
     * @param classNms the namespace for the class, must already be existing
     * @param protoFuncs the new prototype functions which are plugins for the old ones
     * @param overWrite if set to true replaces the old funcs entirely otherwise just does an implicit
     * inheritance with super being remapped
     *
     * TODO do not use this function yet it needs some refinement, it will be interesting later
     * anyway, does not work yet
     */
    _T.pluginClass = function (classNms, protoFuncs, overWrite) {
        var oldClass = _T.fetchNamespace(classNms);
        if (!oldClass) throw new Error("The class namespace " + classNms + " is not existent");

        if (!overWrite) {
            var preserveNMS = classNms + "." + ("" + _T._classReplacementCnt++);
            _T.reserveNamespace(preserveNMS, oldClass);

            return _T.extendClass(classNms, preserveNMS, protoFuncs);
        } else {
            if (protoFuncs.constructor_) {
                newCls.prototype.constructor = protoFuncs.constructor_;
            }
            _T._applyFuncs(oldClass, protoFuncs, true);
        }
    };

    /**
     * delegation pattern which attached singleton generation
     *
     * @param newCls the new namespace object to be generated as singletoin
     * @param delegateObj the object which has to be delegated
     * @param protoFuncs the prototype functions which are attached on prototype level
     * @param nmsFuncs the functions which are attached on the classes namespace level
     */
    _T.singletonDelegateObj = function (newCls, delegateObj, protoFuncs, nmsFuncs) {
        if (_T._reservedNMS[newCls]) {
            return;
        }
        return _T._makeSingleton(_T.delegateObj, newCls, delegateObj, protoFuncs, nmsFuncs);
    };


    _T.loadScript = function (src, type, defer, charSet, async) {
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

      //internal class namespace reservation depending on the type (string or function)
      _T._reserveClsNms = function(newCls, protoFuncs) {
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

})();