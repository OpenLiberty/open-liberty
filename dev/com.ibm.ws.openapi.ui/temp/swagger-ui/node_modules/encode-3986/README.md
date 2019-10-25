# encode-3986

Like [`encodeURIComponent`](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/encodeURIComponent), but [RFC 3986](https://tools.ietf.org/html/rfc3986) compliant.

## install

```sh
$ npm install encode-3986
```

## example

```js
const encode = require('encode-3986')

encode('I am a T-Rex!') // => 'I%20am%20a%20T-Rex%21'
```
