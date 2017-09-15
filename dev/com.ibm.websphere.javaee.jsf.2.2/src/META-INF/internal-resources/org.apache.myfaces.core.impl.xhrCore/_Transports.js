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
