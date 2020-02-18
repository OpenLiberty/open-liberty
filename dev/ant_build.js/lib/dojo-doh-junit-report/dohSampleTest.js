define(['doh/runner'], function(doh) {
    doh.register('Sample Tests Useful for Testing The DOH-JUNIT plugin',
            [
             function willPass() {
                 doh.assertTrue(true);
             },
             function willFail() {
                 doh.assertFalse(true);
             },
             function willError() {
                 throw 'error';
             }
             ]);
});
