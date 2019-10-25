# standard-loader

webpack loader for linting your code with [feross/standard](https://github.com/feross/standard)

[![Build Status](https://travis-ci.org/timoxley/standard-loader.png?branch=master)](https://travis-ci.org/timoxley/standard-loader)
[![Standard - JavaScript Style Guide](https://img.shields.io/badge/code_style-standard-brightgreen.svg)](http://standardjs.com/)

[![NPM](https://nodei.co/npm/standard-loader.png?downloads=true)](https://nodei.co/npm/standard-loader/)
[![NPM](https://nodei.co/npm-dl/standard-loader.png?months=3&height=2)](https://nodei.co/npm/standard-loader/)

## Installation

* Install the desired version of `standard` alongside `standard-loader`.

```
npm install --save-dev standard-loader standard
```

## Usage

### Webpack 1

Webpack 1.x is no longer supported as of [`standard-loader` version `6.0.0`](https://github.com/timoxley/standard-loader/compare/5.0.0...6.0.0).  PRs for Webpack 1.x support will be accepted on the [5.x branch](https://github.com/timoxley/standard-loader/tree/5.x).

### Webpack 2

```js
// webpack.config.js
const webpack = require('webpack')

const config = {
  // ...
  module: {
    rules: [
      {
        // set up standard-loader as a preloader
        enforce: 'pre',
        test: /\.jsx?$/,
        loader: 'standard-loader',
        exclude: /(node_modules|bower_components)/,
        options: {
          // Emit errors instead of warnings (default = false)
          error: false
          // enable snazzy output (default = true)
          snazzy: true
          // other config options to be passed through to standard e.g.
          parser: 'babel-eslint'
        }
      },
      // other loaders...
    ]
  }
}

module.exports = config
```


### Example Input

```js
//code not conforming to standard style

module.exports = function(a,b) {
    console.log( a, b);
}

```

### Example Output

```
> webpack
Hash: 757e85681387cfef939a
Version: webpack 1.9.11
Time: 1152ms
   Asset     Size  Chunks             Chunk Names
build.js  2.01 kB       0  [emitted]  bundle
    + 1 hidden modules

WARNING in ./index.js
standard: Use JavaScript Standard Style (https://github.com/feross/standard)

<text>:1:0: Expected space or tab after // in comment.
<text>:3:25: Missing space before function parentheses.
<text>:3:27: A space is required after ','.
<text>:4:2: Expected indentation of 2 characters.
<text>:4:15: There should be no spaces inside this paren.
<text>:4:23: Extra semicolon.
<text>:7:0: Multiple blank lines not allowed.
```

## Licence

ISC
