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