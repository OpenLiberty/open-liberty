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