define("doh/plugins/simpleWrapper", ["doh/runner"], function(doh) {
    function wrapMethod(obj, mName, fOverride) {
        var orig = obj[mName];
        obj[mName] = function() {
            fOverride.apply(doh, arguments);
            orig.apply(doh, arguments);
        };
    }
    
    console.log("Wrapping with blanket");
	console.log(doh);
    
    // _groupFinished
    // _groupStarted
    var registered = false;
    wrapMethod(doh, "run", function() {
        doh.debug("DEBUG - run is called for each test execution");
    });
    
    wrapMethod(doh, "_init", function() {
	    console.log("DEBUG: invoked on init");
	    console.log(doh);
	    console.log(arguments);
	    blanket.setupCoverage();
    });
	
    wrapMethod(doh, "_groupStarted", function() {
	    console.log("DEBUG: invoked on group start");
	    console.log(doh);
	    console.log(arguments);
	    blanket.onModuleStart();
	});
	
    wrapMethod(doh, "_testStarted", function() {
	    console.log("DEBUG: invoked on test start");
	    console.log(doh);
	    console.log(arguments);
	    blanket.onTestStart();
	});
    
    wrapMethod(doh, "_testFinished", function(name, ignored, pass) {
        console.log("DEBUG: invoked on test end");
        console.log(doh);
	    console.log(arguments);
        blanket.onTestDone(1, pass);
    });
    
    wrapMethod(doh, "_onEnd", function() {
        console.log("DEBUG: invoked on execution end");
        console.log(doh);
	    console.log(arguments);
        blanket.onTestsDone();
    });
    
    wrapMethod(doh, "run", function() { console.log("New DOH run");});
    blanket.beforeStartTestRunner({
        callback: function(){
            console.log("PLUGIN blanket.beforeStartTestRunner invoked"); 
            doh.run();
        }
    });
});
