# Please upgrade Node [![Build Status](https://travis-ci.org/typicode/please-upgrade-node.svg?branch=master)](https://travis-ci.org/typicode/please-upgrade-node) [![npm](https://img.shields.io/npm/v/please-upgrade-node.svg)](https://www.npmjs.com/package/please-upgrade-node)

> __Be friendly, be cool__ :sunglasses: show a message to your users to upgrade Node instead of a stacktrace 

It's common for new Node users to miss the `npm` engines warning when installing a CLI. This package displays a beginner-friendly message if their Node version is below the one expected and it's just a [__few lines__](index.js) of JS code.

Example with [hotel](https://github.com/typicode/hotel) CLI:

```
$ node -v
0.12
$ hotel
hotel requires at least version 4 of Node, please upgrade
```

## Usage

Install:

```sh
npm install please-upgrade-node

yarn add please-upgrade-node
```

Require `please-upgrade-node` before anything else:

```js
// bin.js
require('please-upgrade-node')(
  require('./package.json')
)

// The rest of your code...
```

In your `package.json`, define the required Node version:

```json
{ 
  "name": "super-cli",
  "bin": "./bin.js",
  "engines": { "node": ">=6" }
}
```

Please note that `>=` is the only operator supported by `please-upgrade-node` (e.g. `>=4`, `>=4.0`, `>=4.0.0`). Now if your users install your `super-cli` project with an older Node version, they'll see:

```sh
$ npm install -g super-cli
# [...]
npm WARN engine super-cli@1.0.0: : wanted: {"node":">=6"} (current: {"node":"4.8.3","npm":"2.15.11"})
# [...]
```

```sh
$ super-cli
super-cli requires at least version 6 of Node, please upgrade
```

## Caveat

Make sure when requiring `please-upgrade-node` to not use syntax that is only supported in recent versions of Node.

For example, if you use `const` instead of `var` and don't transpile it, `please-upgrade-node` won't work with Node `0.12`:

```js
const pkg = require('./package.json') // ← Will fail and exit here with Node 0.12,
                                      // because const isn't supported.
require('please-upgrade-node')(pkg)   // No upgrade message will be displayed :(
```

## See also

* [pkg-ok](https://github.com/typicode/pkg-ok) - :ok_hand: Prevents publishing a module with bad paths
* [husky](https://github.com/typicode/husky) - :dog: Git hooks made easy

Thanks to [zeit/serve](https://github.com/zeit/serve) for inspiring the error message.

## License

MIT - [Typicode :cactus:](https://github.com/typicode) - [Patreon](https://patreon.com/typicode)
