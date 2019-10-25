# standard-json

[![npm][npm-image]][npm-url]
[![travis][travis-image]][travis-url]
[![downloads][downloads-image]][downloads-url]
 
[npm-image]: https://img.shields.io/npm/v/standard-json.svg?style=flat-square
[npm-url]: https://www.npmjs.com/package/standard-json
[travis-image]: https://img.shields.io/travis/standard/standard-json.svg?style=flat-square
[travis-url]: https://travis-ci.org/standard/standard-json
[downloads-image]: https://img.shields.io/npm/dm/standard-json.svg?style=flat
[downloads-url]: https://npmjs.org/package/standard-json

Format JavaScript Standard Style output to a JSON array!

## Install

```
# use the CLI
npm install --global standard-json

# or use programatically
npm install --save standard-json
```

## CLI Usage

```bash
standard | standard-json
# exit code will be 1 if any errors are found

standard --verbose | standard-json
# the ruleId attribute will be filled in if --verbose is passed

# works with anything based on standard-engine!
semistandard | standard-json
doublestandard | standard-json

```

## Example output JSON (formatted)
Typical `standard` output:
```bash
standard: Use JavaScript Standard Style (https://github.com/feross/standard)
  /home/icmpdev/code/standard-json/bin.js:19:21: Missing space before function parentheses.
  /home/icmpdev/code/standard-json/index.js:6:35: Strings must use singlequote.
  /home/icmpdev/code/standard-json/index.js:6:51: Extra semicolon.
  /home/icmpdev/code/standard-json/index.js:17:5: Keyword "if" must be followed by whitespace.
  /home/icmpdev/code/standard-json/index.js:22:28: Extra semicolon.
```

After running through standard-json:
```json
[
  {
    "filePath": "/home/flet/code/something-great/bin.js",
    "messages": [
      {
        "line": "19",
        "column": "21",
        "message": "Missing space before function parentheses."
      }
    ]
  },
  {
    "filePath": "/home/flet/code/something-great/index.js",
    "messages": [
      {
        "line": "6",
        "column": "35",
        "message": "Strings must use singlequote."
      },
      {
        "line": "6",
        "column": "51",
        "message": "Extra semicolon."
      },
      {
        "line": "17",
        "column": "5",
        "message": "Keyword \"if\" must be followed by whitespace."
      },
      {
        "line": "22",
        "column": "28",
        "message": "Extra semicolon."
      }
    ]
  }
]
```

## API Usage

```js

var txt = someStandardThing() // produces Standard Style output

var standardJson = require('standard-json')

var output = standardJson(txt)
// output will be an array of errors or an empty array.

var output = standardJson(txt, {noisey: true})
// output will be an array of errors or an empty array.
// The banner will be `console.error`'d (plus any other non-parsable lines)

```

## Inspiration

This package was inspired by:
- [snazzy](https://github.com/standard/snazzy)
- https://github.com/standard/standard/issues/222


## Contributing

Contributions welcome! Please read the [contributing guidelines](CONTRIBUTING.md) first.

## License

[ISC](LICENSE.md)
