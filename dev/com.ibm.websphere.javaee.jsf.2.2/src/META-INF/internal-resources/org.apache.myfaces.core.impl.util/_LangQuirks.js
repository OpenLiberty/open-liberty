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
