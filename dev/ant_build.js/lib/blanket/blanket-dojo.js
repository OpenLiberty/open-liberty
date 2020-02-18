/**
 * 
 * Adapters must implement the following calls:

    When the test runner starts it must call blanket.setupCoverage();
    When the test module or suite starts it can call blanket.onModuleStart(); (optional)
    When a test starts it must call blanket.onTestStart();
    When a test ends it must call blanket.onTestDone(<total>, <num passed>)
    When all the tests are done it must call blanket.onTestsDone();

Finally, the adapter must call the following function:

blanket.beforeStartTestRunner({
    callback: function(){ command to start test runner }
});
 * 
 */

(function() {
    function wrapMethod(obj, mName, fOverride) {
        var orig = obj[mName];
        obj[mName] = function() {
            fOverride.apply(doh, arguments);
            console.log(arguments);
            orig.apply(doh, arguments);
        };
    }
    
    console.log("blanket-dojo.js initiatlized");

    blanket.beforeStartTestRunner({
        callback: function(){
            console.log("ADAPTER blanket.beforeStartTestRunner invoked"); 
        }
    });
})();
