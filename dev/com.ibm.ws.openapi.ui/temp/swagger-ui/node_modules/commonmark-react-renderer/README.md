# commonmark-react-renderer

[![npm version](http://img.shields.io/npm/v/commonmark-react-renderer.svg?style=flat-square)](http://browsenpm.org/package/commonmark-react-renderer)[![Build Status](http://img.shields.io/travis/rexxars/commonmark-react-renderer/master.svg?style=flat-square)](https://travis-ci.org/rexxars/commonmark-react-renderer)[![Coverage Status](http://img.shields.io/codeclimate/coverage/github/rexxars/commonmark-react-renderer.svg?style=flat-square)](https://codeclimate.com/github/rexxars/commonmark-react-renderer)[![Code Climate](http://img.shields.io/codeclimate/github/rexxars/commonmark-react-renderer.svg?style=flat-square)](https://codeclimate.com/github/rexxars/commonmark-react-renderer/)

Renderer for CommonMark which returns an array of React elements, ready to be used in a React component. See [react-markdown](https://github.com/rexxars/react-markdown/) for such a component.

## Installing

```
npm install --save commonmark-react-renderer
```

## Basic usage

```js
var CommonMark = require('commonmark');
var ReactRenderer = require('commonmark-react-renderer');

var parser = new CommonMark.Parser();
var renderer = new ReactRenderer();

var input = '# This is a header\n\nAnd this is a paragraph';
var ast = parser.parse(input);
var result = renderer.render(ast);

// `result`:
[
    <h1>This is a header</h1>,
    <p>And this is a paragraph</p>
]
```

## Options

Pass an object of options to the renderer constructor to configure it. Available options:

* `sourcePos` - *boolean* Setting to `true` will add `data-sourcepos` attributes to all elements, indicating where in the markdown source they were rendered from (default: `false`).
* `escapeHtml` - *boolean* Setting to `true` will escape HTML blocks, rendering plain text instead of inserting the blocks as raw HTML (default: `false`).
* `skipHtml` - *boolean* Setting to `true` will skip inlined and blocks of HTML (default: `false`).
* `softBreak` - *string* Setting to `br` will create `<br>` tags instead of newlines (default: `\n`).
* `allowedTypes` - *array* Defines which types of nodes should be allowed (rendered). (default: all types).
* `disallowedTypes` - *array* Defines which types of nodes should be disallowed (not rendered). (default: none).
* `unwrapDisallowed` - *boolean* Setting to `true` will try to extract/unwrap the children of disallowed nodes. For instance, if disallowing `Strong`, the default behaviour is to simply skip the text within the strong altogether, while the behaviour some might want is to simply have the text returned without the strong wrapping it. (default: `false`)
* `allowNode` - *function* Function execute if in order to determine if the node should be allowed. Ran prior to checking `allowedTypes`/`disallowedTypes`. Returning a truthy value will allow the node to be included. Note that if this function returns `true` and the type is not in `allowedTypes` (or specified as a `disallowedType`), it won't be included. The function will get a single object argument (`node`), which includes the following properties:
  * `type` - *string* The type of node - same ones accepted in `allowedTypes` and `disallowedTypes`
  * `renderer` - *string* The resolved renderer for this node
  * `props` - *object* Properties for this node
  * `children` - *array* Array of children
* `renderers` - *object* An object where the keys represent the node type and the value is a React component. The object is merged with the default renderers. The props passed to the component varies based on the type of node. See the `Type renderer options` section below for more details.
* `transformLinkUri` - *function|null* Function that gets called for each encountered link with a single argument - `uri`. The returned value is used in place of the original. The default link URI transformer acts as an XSS-filter, neutralizing things like `javascript:`, `vbscript:` and `file:` protocols. If you specify a custom function, this default filter won't be called, but you can access it as `require('commonmark-react-renderer').uriTransformer`. If you want to disable the default transformer, pass `null` to this option.
* `transformImageUri` - *function|null* Function that gets called for each encountered image with a single argument - `uri`. The returned value is used in place of the original.
* `linkTarget` - *string* A string to be used in the anchor tags `target` attribute e.g., `"_blank"`

## Type renderer options

### HtmlInline / HtmlBlock

**Note**: Inline HTML is [currently broken](https://github.com/rexxars/commonmark-react-renderer/issues/9)

* `isBlock` - *boolean* `true` if type is `HtmlBlock`, `false` otherwise
* `escapeHtml` - *boolean* Same as renderer option, see above
* `skipHtml` - *boolean* Same as renderer option, see above
* `literal` - *string* The HTML fragment

### CodeBlock

* `language` - *string* Language info tag, for instance \```js would set this to `js`. Undefined if the tag is not present in the source.
* `literal` - *string* The string value of the code block

### Code

* `literal` - *string* The string value of the inline code
* `inline` - *boolean* Always true. Present to allow reuse of the same renderer for both `CodeBlock` and `Code`.

### Heading

* `level` - *number* Heading level, from 1 to 6.
* `children` - *node* One or more child nodes for the heading

### Softbreak

* `softBreak` - *mixed* Depending on the `softBreak` setting of the actual renderer, either a given string or a React linebreak element

### Link

* `href` - *string* URL for the link
* `title` - *string* Title for the link, if any
* `children` - *node* One or more child nodes for the link

### Image

* `src` - *string* URL for the image
* `title` - *string* Title for the image, if any
* `alt` - *string* Alternative text for the image, if any

### List

* `start` - *number* Start index of the list
* `type` - *string* Type of list (`Bullet`/`Ordered`)
* `tight` - *boolean* Whether the list is tight or not (see [http://spec.commonmark.org/0.23/#lists](CommonMark spec) for more details)

### Common

* `nodeKey` - *string* A key that can be used by React for the `key` hint
* `children` - *node* Child nodes of the current node
* `literal` - *string* A literal representation of the node, where applicable
* `data-sourcepos` - *string* If `sourcePos` option is set, passed to all types and should be present in all the DOM-representations to signify the source position of this node

## Testing

```bash
git clone git@github.com:rexxars/commonmark-react-renderer.git
cd commonmark-react-renderer
npm install
npm test
```

## License

MIT-licensed. See LICENSE.
