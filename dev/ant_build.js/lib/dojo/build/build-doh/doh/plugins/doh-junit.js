define("doh/plugins/doh-junit", ["dojo/main", "doh/runner", "dojo/_base/declare"], function(dojo, doh, declare) {

    var debugOn = false;

    if (debugOn) console.log("Declaring DOH-JUNIT");
    var anonymousDohJUnit = declare(null, {
        // Internal fields
        _doh: null,
        _oXmlOutput: [],
        _oXml: {},
        _hashes: {},
        _totalTime: 0,
        
        /**
         * @param Provide DOH: {_doh: doh}
         */
        constructor: function(args) {
            declare.safeMixin(this, args);
        },

        /** Adds a system message to the XML output */
        pushSystemMessage: function(args){

            this._oXmlOutput.push({
                type: 'system-out',
                hash: args.hash,
                value: args.message
            });
        },

        /** Adds a system error to the XML output */
        pushSystemError: function(args){

            this._oXmlOutput.push({
                type: 'system-err',
                hash: args.hash,
                value: args.message
            });

        },

        /** Addsa a test failure to the XML output */
        pushFailure: function(args){
            args.type = args.type || 'junit.framework.AssertionFailedError';
            this._tabFailures[args.testsuite]++;
            this._oXmlOutput.push({
                type: 'failure',
                hash: args.hash,
                value: args.trace,
                attributes: {
                    type: args.type,
                    message: args.message
                }
            });

        },

        /** Adds a test error to the XML output */
        pushError: function(args){
            this._tabErrors[args.testsuite]++;
            this._oXmlOutput.push({
                type: 'error',
                hash: args.hash,
                value: args.value,
                attributes: {
                    message: args.attributes["stack"]
                }
            });

        },

        /** Starts the test suites block */
        openTestSuites: function(){
            this._oXmlOutput.push({
                type: 'testsuites'
            });
        },

        /** Starts a test suite block */
        openTestSuite: function(args){
            var _date = new Date();
            _date = _date.getFullYear()+'-'+_date.getMonth()+'-'+_date.getDay()+'T'+_date.getHours()+':'+_date.getMinutes()+':'+_date.getSeconds();

            //reset error counter
            args.id = args.id || 0;
            args.errors = args.errors || this._doh._errorCount;
            args.time = args.time || '0.0';
            args.tests = args.tests || this._doh._testCount;
            args.failures = args.failures || this._doh._failureCount;
            args.timestamp = args.timestamp || _date;
            args.hostname = args.hostname || 'localhost';
            this._tabErrors[args.name] = 0;
            this._tabFailures[args.name] = 0;

            this._oXmlOutput.push({
                type: 'testsuite',
                attributes: {
                    'name':args.name,
                    'errors':args.errors,
                    'failures':args.failures,
                    'tests':args.tests,
                    'time':args.time,
                    'timestamp':args.timestamp,
                    'hostname':args.hostname
                }
            });
        },

        /** Starts a test case block */
        openTestCase: function(args){
            args.time = args.time || '0.0';

            this._oXmlOutput.push({
                type: 'testcase',
                attributes: {
                    'classname': args.classname,
                    'name': args.name,
                    'time': args.time
                }
            });

        },

        /** Creates the final XML representation */
        outputXML: function(){
            var comment = [];
            var testsuites = [];
            var testsuite = [];
            var testcase = [];
            var failure = [];
            var error = [];
            var systemOut = [];
            var systemErr = [];
            var output = [];
            output.push('<?xml version="1.0" encoding="UTF-8" ?>');
            output.push('<!-- ******************************* -->');
            output.push('<!-- The Dojo Unit Test Harness, $Rev: 23869 $ (Patched version) -->');
            output.push('<!-- Copyright (c) 2011, The Dojo Foundation, All Rights Reserved -->');
            output.push('<!-- Date: '+(new Date())+'              -->');
            output.push('<!-- '+this._doh._testCount+" test"+(this._doh._testCount>1?'s':'')+" to run in "+this._doh._groupCount+" group"+(this._doh._groupCount>2?'s':'') +' -->');
            output.push('<!-- ******************************* -->');

            var indent = function(n){
                var _tab = [];
                for(var i=0;i<n;i++){
                    _tab.push('\t');
                }
                return _tab.join('');
            };
            var getAttr = function(o){
                var attr = [];
                if(o.attributes){
                    o = o.attributes;
                    for (var p in o) {
                        attr.push( p + '="' + o[p] + '"');
                    }
                }
                return attr.join(' ');
            };
            var getObjectFromHash = function(o, hash){
                for (var i = o.length - 1; i >= 0; i--) {
                    if(o[i].hash && o[i].hash===hash){
                        return o[i].data;
                    }
                };
                return null;
            };

            // filter by type
            for(var i=0; i<this._oXmlOutput.length; i++){
                var o = this._oXmlOutput[i];
                var type = o.type;

                switch(type){
                case 'testsuites':  testsuites.push(o); break;
                case 'testsuite':   testsuite.push(o); break;
                case 'testcase':    testcase.push(o); break;
                case 'failure':     failure.push({ _name: 'failure', hash: o.hash, data: o}); break;
                case 'error':       error.push({ _name: 'error', hash: o.hash, data: o});console.log("PUSHED ERROR"); break;
                case 'system-out':  systemOut.push({ _name: 'system-out', hash: o.hash, data: o}); break;
                case 'system-err':  systemErr.push({ _name: 'system-err', hash: o.hash, data: o}); break;
                }

            };

            for(var i=0; i<testsuites.length; i++){
                output.push('<testsuites>');

                for(var j=0; j<testsuite.length; j++){
                    output.push(indent(1)+'<testsuite '+getAttr(testsuite[j])+'>');

                    var _hash = 'Not Set!';
                    for(var k=0; k<testcase.length; k++){

                        if(testcase[k].attributes.classname===testsuite[j].attributes.name){
                            output.push(indent(2)+'<testcase '+getAttr(testcase[k])+'>');

                            _hash = this.generateHash(testcase[k].attributes['classname'], testcase[k].attributes['name']);
                            var f, e;
                            if(failure.length > 0){
                                f = getObjectFromHash(failure, _hash);
                                if(f){
                                    var failureAttr = getAttr(f).replace(/>/g,"&gt;");
                                    failureAttr = failureAttr.replace(/</g,"&lt;");
                                    output.push(indent(3)+'<failure '+  failureAttr + '>');
                                    output.push(indent(4)+f.value);
                                    output.push(indent(3)+'</failure>');
                                }
                            }
                            if(error.length > 0){
                                e = getObjectFromHash(error, _hash);
                                if(e){
                                    
                                    var errorAttr = getAttr(e).replace(/>/g,"&gt;");
                                    errorAttr = errorAttr.replace(/</g,"&lt;");
                                    output.push(indent(3)+'<error '+ errorAttr + '>');
                                    output.push(indent(4)+e.value);
                                    output.push(indent(3)+'</error>');
                                }
                            }

                            output.push(indent(2)+'</testcase>');
                        }

                    }

                    var so = getObjectFromHash(systemOut, _hash);
                    if(so){
                        output.push(indent(2)+'<system-out '+getAttr(so)+'>');
                        output.push(indent(3)+so.value);
                        output.push(indent(2)+'</system-out>');
                    }

                    var se = getObjectFromHash(systemErr, _hash);
                    if(se){
                        output.push(indent(2)+'<system-err '+getAttr(so)+'>');
                        output.push(indent(3)+so.value);
                        output.push(indent(2)+'</system-err>');
                    }

                    output.push(indent(1)+'</testsuite>');
                }

                output.push('</testsuites>');
            }

            return output.join('\n');
            output = [];
        },

        /** ??? */
        updateAttribute: function(args){

            for(var i=this._oXmlOutput.length-1; i>=0; i--){

                if(args.type===this._oXmlOutput[i].type
                        && this._oXmlOutput[i].attributes
                        && args.groupName===this._oXmlOutput[i].attributes.name
                ){
                    this._oXmlOutput[i].attributes[ args.name ] = args.value;
                    return true; 
                }
            }
            return false;
        },

        /** Generates a hash based on all of the arguments */
        generateHash: function(){
            var args = [];
            var _h = '';
            for (var i = arguments.length - 1; i >= 0; i--) {
                args.push(arguments[i]);
            };
            _h = args.join('########');

            this._hashes[_h] = _h;
            return _h;
        },

        /** ??? */
        formatTime: function(n){
            n = +n;
            if(n===0){
                return '0.0';
            }

            switch(true){
            case n<1000: //<1s
                return '0.'+n;
            case n<60000: //<1m
                return (n/100)/10;
            }
        },

        // holly crap !!!!!!!!!!!!!!
        // Neither Dojo.Deferred() nor setTimeout() are available is this context
        // so busy wait 
        WTF: function(callback){

            if(window && window.setTimeout) {
                window.setTimeout(callback, 1000);
            }
            else {
                // I'm aware this is so ugly! Please submit fixes :)
                var i=0;while( i<this._groupCounter*10000000 ){i++;};
                (typeof callback == 'function') && callback();
            }

        }
    });

    // Store doh to dohJUnit for reference
    var dohJUnit = new anonymousDohJUnit({_doh: doh});

    if (debugOn) console.log("DOH-JUNIT declared!", dohJUnit);

    if (debugOn) console.log("DOH before wrapping:");
    if (debugOn) console.log(doh);

    /** ======== BEGIN OVERRIDES =========== */
    function wrapMethod(obj, mName, fOverride) {
        var orig = obj[mName];
        obj[mName] = function() {
            fOverride.apply(doh, arguments);
            if (orig) {
                orig.apply(doh, arguments);
            } else {
                console.error("Wrapped a function "+mName+" which did not exist!!");
            }
        };
    }

    if (debugOn) console.log("Wrapping DOH with DOH-JUNIT");

    wrapMethod(doh, "_init", function() {
        if (debugOn) console.log("_init for DOH-JUNIT");
        dohJUnit._currentGroup = null;
        dohJUnit._currentTest = null;
        dohJUnit._tabErrors = {};
        dohJUnit._tabFailures = {};
        dohJUnit.openTestSuites();
    });

    wrapMethod(doh, "_groupStarted", function(groupName) {
        if (debugOn) console.log("_groupStarted for DOH-JUNIT", groupName);
        dohJUnit._groupTotalTime = 0;
        if(dohJUnit._groupCounter){
            dohJUnit._groupCounter++;
        }
        else {
            dohJUnit._groupCounter = 1;
        }
    });

    wrapMethod(doh, "_groupFinished", function(groupName, success) {
        if (debugOn) console.log("_groupFinished for DOH-JUNIT", groupName, success);
        dohJUnit._totalTime += dohJUnit._groupTotalTime;
        dohJUnit.updateAttribute({
            type: 'testsuite',
            name: 'time',
            groupName: groupName,
            value: dohJUnit.formatTime(dohJUnit._groupTotalTime)
        });

        dohJUnit.updateAttribute({
            type: 'testsuite',
            name: 'errors',
            groupName: groupName,
            value:  dohJUnit._tabErrors[groupName]
        });

        dohJUnit.updateAttribute({
            type: 'testsuite',
            name: 'failures',
            groupName: groupName,
            value: dohJUnit._tabFailures[groupName]
        });

    });

    wrapMethod(doh, "_testStarted", function(groupName, fixture){
        if (debugOn) console.log("_testStarted for DOH-JUNIT", groupName, fixture);
        dohJUnit.openTestCase({
            classname: groupName,
            name: fixture.name,
            time: fixture.endTime
        });
    });

    wrapMethod(doh, "_testFinished", function(groupName, fixture, err){
        if (debugOn) console.log("_testFinished for DOH-JUNIT", groupName, fixture, err);
        var _timeDiff = fixture.endTime-fixture.startTime;
        var testElapsedTime = dohJUnit.formatTime(_timeDiff);

        dohJUnit.updateAttribute({
            type: 'testcase',
            name: 'time',
            groupName: groupName,
            value: testElapsedTime
        });
    });

    wrapMethod(doh, "_printTestFinished", function() {
        if (debugOn) console.log("_printTestFinished for DOH-JUNIT"); 
    });

    wrapMethod(doh, "testRegistered", function(groupName, fixture){
        if (debugOn) console.log("_testRegistered for DOH-JUNIT", groupName, fixture);
    });

    wrapMethod(doh, "_setupGroupForRun", function(/*String*/ groupName, /*Integer*/ idx){
        if (debugOn) console.log("_setupGroupForRun for DOH-JUNIT", groupName, idx);
        var tg = this._groups[groupName];
        dohJUnit.openTestSuite({
            name: groupName,
            tests: tg.length
        });
    });

    wrapMethod(doh, "_handleFailure", function(groupName, fixture, e){
        if (debugOn) console.log("_handleFailure for DOH-JUNIT", groupName, fixture, e);
        var out = "";
        console.log(groupName);
        console.log(fixture);
        console.log(e);
        if(e instanceof this._AssertFailure){
            if(e["fileName"]){ out += e.fileName + ':'; }
            if(e["lineNumber"]){ out += e.lineNumber + ' '; }
            out += e+": "+e.message;

            dohJUnit.pushFailure({
                hash: dohJUnit.generateHash(groupName, fixture.name),
                testsuite : groupName,
                testcase : fixture.name,
                name: fixture.name,
                message: doh._AssertFailure.prototype.name,
                trace: out
            });

        }else{
            dohJUnit.pushError({
                type: 'error',
                testsuite : groupName,
                testcase : fixture.name, 
                hash: dohJUnit.generateHash(groupName, fixture.name),
                value: e,
                attributes: {
                    stack: e.stack
                }
            });

        }

        if(e){
            if(e['rhinoException']){
                dohJUnit.pushSystemError({
                    hash: dohJUnit.generateHash(groupName, fixture.name),
                    message: e.rhinoException.getStackTrace().getMessage()
                });
            }else if(e['javaException']){
                dohJUnit.pushSystemError({
                    hash: dohJUnit.generateHash(groupName, fixture.name),
                    message: e.javaException.getStackTrace().getMessage()
                });
            }
        }
    });

    wrapMethod(doh, "_onEnd", function(){
        if (debugOn) console.log("_onEnd for DOH-JUNIT");
    });

    wrapMethod(doh, "_report", function(){
        if (debugOn) console.log("_report for DOH-JUNIT");
        dohJUnit.WTF(function(){
            var result = dohJUnit.outputXML();

            // check if this module is running inside a browser
            if(window && window.document) {
                var node = document.getElementById("xml-report");
                if (node.innerText) {
                    node.innerText = result;
                }
                else {
                    node.textContent = result;
                }
            }
            else {
                // TODO: write this out to an XML file
                // you might want to save the result into a *.xml file.
                // but for now we just output the xml content.
                this.debug(result);
            }

        });
    });

    return doh;
});
