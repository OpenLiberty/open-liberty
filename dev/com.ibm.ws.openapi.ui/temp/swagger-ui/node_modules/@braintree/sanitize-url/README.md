# sanitize-url

## Installation

```sh
npm install -S @braintree/sanitize-url
```

## Usage

```js
var sanitizeUrl = require('@braintree/sanitize-url').sanitizeUrl;

sanitizeUrl('http://example.com'); // 'http://example.com'
sanitizeUrl('javascript:alert(document.domain)'); // 'about:blank'
sanitizeUrl('jAvasCrIPT:alert(document.domain)'); // 'about:blank'
sanitizeUrl(decodeURIComponent('JaVaScRiP%0at:alert(document.domain)')); // 'about:blank'
```
