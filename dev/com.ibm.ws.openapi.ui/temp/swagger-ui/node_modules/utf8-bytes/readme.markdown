# utf8-bytes

return an array of all the bytes in a unicode string

[![build status](https://secure.travis-ci.org/substack/utf8-bytes.png)](http://travis-ci.org/substack/utf8-bytes)

This module is like `Buffer(str).toJSON()`, but without using `Buffer`.

# example

``` js
var bytes = require('utf8-bytes');
console.log(bytes('[☉,☼]'));
```

output:

```
$ node example/utf8-bytes.js
[ 91, 226, 152, 137, 44, 226, 152, 188, 93 ]
```

# methods

``` js
var bytes = require('utf8-bytes')
```

## bytes(str)

Return an array of integers from 0 through 255, inclusive, representing the
bytes in the unicode string `str`.

# install

With [npm](https://npmjs.org) do:

```
npm install utf8-bytes
```

# license

MIT
