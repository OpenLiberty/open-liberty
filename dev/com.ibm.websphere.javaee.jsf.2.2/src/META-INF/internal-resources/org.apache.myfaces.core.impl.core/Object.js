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
