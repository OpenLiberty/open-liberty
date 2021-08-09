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


