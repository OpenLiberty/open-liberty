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
 * This class is for http based unit tests
 *
 * note this class is only for testing pages
 * and is not added to the final build
 * we use it internally for testing pages
 * which test the various functions of the framework
 */
/** @namespace myfaces._impl._util._UnitTest*/
_MF_SINGLTN(_PFX_UTIL+"_UnitTest", _MF_OBJECT, {



    /**
     * Simple assert true
     *
     * @param message the assertion message
     * @param assertionOutcome the assertion outcome (true or false)
     */
    assertTrue: function(message, assertionOutcome) {
        var _Logger = myfaces._impl._util._Logging;

        if (!assertionOutcome) {
            _Logger.logError(message, "assertionOutcome:", assertionOutcome);
            throw Error(message, assertionOutcome);
        }
        _Logger.logInfo(message, "assertionOutcome:", assertionOutcome);
    },
    
    /**
     * Simple assert false
     *
     * @param message the assertion message
     * @param assertionOutcome the assertion outcome (true or false)
     */
    assertFalse: function(message, assertionOutcome) {
        var _Logger = myfaces._impl._util._Logging;

        if (assertionOutcome) {
            _Logger.logError(message, "assertionOutcome:", assertionOutcome);
            throw Error(message, assertionOutcome);
        }
        _Logger.logInfo(message, "assertionOutcome:", assertionOutcome);
    }
});