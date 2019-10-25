# XML, But Prettier

[![Build Status][travis-image]][travis-url]
[![NPM version][npm-image]][npm-url]

_This module is a fork of [jonathanp/xml-beautifier][upstream-github]._

This module beautifies XML documents by putting each tag and text node on their own line and correctly indenting everything.

Can be used e.g. if you're using [React as a static page generator][react] and (for some reason) need the generated HTML to be more human-readable.


## Install

```
$ npm install --save xml-beautifier
```


## Usage

The module's function signature is `xmlButPrettier(xml:String, options:Object)`.

```js
import xmlButPrettier from 'xml-but-prettier';

const xml = xmlButPrettier('<div><span>foo</span></div>');
console.log(xml); // => will output correctly indented elements
```

#### Options
- `indentor`: a custom string to use for indenting things
- `textNodesOnSameLine`: compresses text nodes onto the same line as their containing tags

## License

MIT.

[upstream-github]: https://github.com/jonathanp/xml-beautifier
[npm-url]: https://npmjs.org/package/xml-but-prettier
[npm-image]: https://badge.fury.io/js/xml-but-prettier.svg
[travis-image]: https://travis-ci.org/shockey/xml-but-prettier.svg
[travis-url]: https://travis-ci.org/shockey/xml-but-prettier
[react]: https://facebook.github.io/react/docs/top-level-api.html#reactdomserver.rendertostaticmarkup
