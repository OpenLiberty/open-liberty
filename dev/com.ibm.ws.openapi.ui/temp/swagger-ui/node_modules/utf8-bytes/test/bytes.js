var bytes = require('../');
var test = require('tape');

test('some canned examples', function (t) {
    t.deepEqual(bytes('[☉,☼]'), bufArray('[☉,☼]'));
    t.deepEqual(
        bytes('\uD834\uDF06'),
        [ 240, 157, 140, 134 ]
    );
    t.deepEqual(
        bytes('\uD835\uDC01'),
        [ 240, 157, 144, 129 ]
    );
    t.end();
});

test('all the code points', function (t) {
    t.plan(65536);
    for (var i = 0; i < 65536; i++) {
        var s = String.fromCharCode(i);
        t.deepEqual(bytes(s), bufArray(s));
    }
});

function bufArray (s) {
    return [].slice.call(Buffer(s));
}
