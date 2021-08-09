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
 * debugging replacement for lang which adds logging functionality
 * which is not yet present in the core
 * this is a full replacement class for myfaces._impl._util._Lang
 * and replaces the object entirely with
 * a delegated implementation which adds the new methods to Lang
 *
 * We use this class to move some debugging related
 * lang functions out of the core, which never will
 * be utilized directly in the core
 * but will be used externally by extension frameworks
 * and by unit tests
 */
/** @namespace myfaces._impl._util._Logging */
_MF_SINGLTN(_PFX_UTIL+"_Logging", _MF_OBJECT, {

    /**
     * we use a map switch instread of a log level
     * slightly slower but more flexible
     */
    _ERR: "error",
    _INF: "info",
    _DEB: "debug",
    _LOG: "log",
    _WRN: "warn",

    _Lang: myfaces._impl._util._Lang,

    constructor_: function() {
        this._callSuper("constructor_");

        //due to the delegation pattern we do not have access to the browser
        this._browser = myfaces._impl.core._Runtime.browser;

        this.logLevels = {};
        this.stackTraceLevels = {};
        var logLevels = this.logLevels;
        var traceLevels = this.stackTraceLevels;
        var levels = [this._ERR, this._INF, this._DEB, this._LOG, this._WRN];
        for(var cnt = 0; cnt < levels.length; cnt++) {
            var elem = levels[cnt];
            logLevels[elem] = true;
            traceLevels[elem] = false;
        }
        traceLevels[this._ERR] = true;
    },

    /**
     * Simple simple logging only triggering at
     * firebug compatible logging consoles
     *
     * note: ;; means the code will be stripped
     * from the production code by the build system
     */
    _log: function(logType /*+arguments*/, args) {

        var argsString = this._Lang.objToArray(arguments[1]).join("|");
        var c = window.console;
        

        if (c && c[logType]) {
            c[logType](argsString);
            if(this.stackTraceLevels[logType] && c.trace) {
                c.trace();
            }
        }
        var logHolder = document.getElementById("myfaces.logging");

        if (logHolder) {
            var elem = document.createElement("div");
            var b = this._browser;
            if (!b.isIE || b.isIE > 7) {
                //w3 compliant class setting
                elem.setAttribute("class", "consoleLog " + logType);
            } else {
                //ie quirks compliant class setting
                elem.className ="consoleLog " + logType;
            }
            logHolder.appendChild(elem);
            var argsArr = this._Lang.objToArray(arguments[1]);
            var argsStr = [];

            for(var cnt = 0; cnt < argsArr.length; cnt++) {
                argsStr.push("<div class='args args_"+cnt+"'>");
                argsStr.push(argsArr[cnt]);
                argsStr.push("</div>");
            }
            elem.innerHTML = "<div class='args argsinfo'>"+logType.toUpperCase() + "</div>" + argsStr.join("");
            logHolder.scrollTop = logHolder.scrollHeight;
        }
    },

    logError: function(/*varargs*/) {
        if (!this.logLevels[this._ERR]) return;
        this._log(this._ERR, arguments);
    }
    ,
    logWarn: function(/*varargs*/) {
        if (!this.logLevels[this._WRN]) return;
        this._log(this._WRN, arguments);
    }
    ,
    logInfo: function(/*varargs*/) {
        if (!this.logLevels[this._INF]) return;
        this._log(this._INF, arguments);
    }
    ,
    logDebug: function(/*varargs*/) {
        //Level 1 == debug
        if (!this.logLevels[this._DEB]) return;
        this._log(this._DEB, arguments);
    }
    ,
    logTrace: function(/*varargs*/) {
        if (!this.logLevels[this._LOG]) return;
        this._log("log", arguments);
    }

});

