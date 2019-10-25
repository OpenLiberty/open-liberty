# querystring-browser [![npm][npm-image]][npm-url]

[npm-image]: https://img.shields.io/npm/v/querystring-browser.svg
[npm-url]: https://www.npmjs.com/package/querystring-browser

Node.js v5 compatible querystring module for browsers.

## install

```
npm install -g querystring-browser
```

## usage

Require the module or make browserify rewrite querystring using a transform such as [aliasify](https://www.npmjs.com/package/aliasify):

```js
"aliasify": {
  "aliases": {
    "querystring": "querystring-browser"
  }
}
```

## Node.js

The source code comes straight from the core implementation at https://github.com/nodejs/node

## license

MIT / Node.js licence