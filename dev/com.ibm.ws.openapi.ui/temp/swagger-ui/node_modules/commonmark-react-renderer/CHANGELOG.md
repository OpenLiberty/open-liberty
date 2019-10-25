# Change Log

All notable changes will be documented in this file.

## 4.3.3 - 2017-05-28

- Expose all of codeinfo to CodeBlock renderer (Tauren Mills)
- Include className in core props (Espen Hovlandsdal)

## 4.3.2 - 2016-12-08

- Add `linkTarget` prop to optionally add a `target` attribute on links (James Simpson)
- Fix typo in readme (Chase Ricketts)
- Allow using Commonmark 0.27 (Lukas Geiger)

## 4.3.1 - 2016-08-23

- Update dependencies to latest versions (Espen Hovlandsdal)

## 4.3.0 - 2016-08-23

- Enable Commonmark.js 0.26 compatibility (Espen Hovlandsdal)

## 4.2.4 - 2016-07-09

- Fix bug where nodes would not be rendered due to duplicate keys (Espen Hovlandsdal)

## 4.2.3 - 2016-07-09

- Fix regression in passing props to `Code`-nodes (Espen Hovlandsdal)

## 4.2.2 - 2016-07-09

### Changes

- Give `Code` renderers an `inline` property that is always true, allowing reuse of renderer for `CodeBlock` and `Code` (Espen Hovlandsdal)

## 4.2.1 - 2016-07-09

### Changes

- Fix bug where lists, codeblocks and headings would not get passed `sourcepos` prop (Espen Hovlandsdal)

## 4.2.0 - 2016-07-09

### Changes

- Plain DOM-node renderers are now given only their respective props. Fixes warnings when using React >= 15.2 (Espen Hovlandsdal)

### Added

- New `transformImageUri` option allows you to transform URIs for images. (Petri Lehtinen)

## 4.1.4 - 2016-04-27

### Changes

- Fix image alt text when it includes special characters (Ramsay Stirling II)

## 4.1.3 - 2016-04-26

### Changes

- Pass `nodeKey` as prop to complex renderers. Fixes warning in React >= 15 (Espen Hovlandsdal)

## 4.1.2 - 2016-03-12

### Changes

- Also join sibling nodes within paragraphs and similar (Espen Hovlandsdal)

## 4.1.1 - 2016-03-12

### Changes

- Join sibling text nodes into one text node (Espen Hovlandsdal)

## 4.0.1 - 2016-02-21

### Changes

- Use strings as renderers in simple cases (Glen Mailer)
- Set keys on lists and code blocks (Guillaume Plique)

## 4.0.0 - 2016-02-21

### Changes

- **Breaking change**: Inline HTML nodes are now wrapped in a `<span>`, block HTML nodes in `<div>`. This is necessary to properly support custom renderers.

## 3.0.2 - 2016-02-21

### Changes

- The default URI transformer no longer applies double URI-encoding.

## 3.0.1 - 2016-02-21

### Added

- The default URI transformer is now exposed on the `uriTransformer` property of the renderer, allowing it to be reused.
- Documentation for `transformLinkUri`-option.

## 3.0.0 - 2016-02-21

### Changes

- **Breaking change**: The renderer now requires Node 0.14 or higher. This is because the renderer uses stateless components internally.
- **Breaking change**: `allowNode` now receives different properties in the options argument. See `README.md` for more details.
- **Breaking change**: CommonMark has changed some type names. `Html` is now `HtmlInline`, `Header` is now `Heading` and `HorizontalRule` is now `ThematicBreak`. This affects the `allowedTypes` and `disallowedTypes` options.
- **Breaking change**: A bug in the `allowedTypes`/`disallowedTypes` and `allowNode` options made them only applicable to certain types. In this version, all types are filtered, as expected.
- **Breaking change**: Link URIs are now filtered through an XSS-filter by default, prefixing "dangerous" protocols such as `javascript:` with `x-` (eg: `javascript:alert('foo')` turns into `x-javascript:alert('foo')`). This can be overridden with the `transformLinkUri`-option. Pass `null` to disable the feature or a custom function to replace the built-in behaviour.

### Added

- New `renderers` option allows you to customize which React component should be used for rendering given types. See `README.md` for more details. (Espen Hovlandsdal / Guillaume Plique)
- New `unwrapDisallowed` option allows you to select if the contents of a disallowed node should be "unwrapped" (placed into the disallowed node position). For instance, setting this option to true and disallowing a link would still render the text of the link, instead of the whole link node and all it's children disappearing. (Espen Hovlandsdal)
- New `transformLinkUri` option allows you to transform URIs in links. By default, an XSS-filter is used, but you could also use this for use cases like transforming absolute to relative URLs, or similar. (Espen Hovlandsdal)

## 2.2.2 - 2016-01-22

### Added

- Provide index-based keys to generated elements to silent warnings from React (Guillaume Plique)

## 2.2.1 - 2016-01-22

### Changed

- Upgrade commonmark to latest version (Guillaume Plique)

## 2.2.0 - 2015-12-11

### Added

- Allow passing `allowNode` - a function which determines if a given node should be allowed (Espen Hovlandsdal)

## 2.1.0 - 2015-11-20

### Added

- Add support for specifying which types should be allowed - `allowTypes`/`disallowedTypes` (Espen Hovlandsdal)

## 2.0.2 - 2015-11-19

### Added

- Add support for hard linebreaks (marlonbaeten)

## 2.0.1 - 2015-10-22

### Changed

- Peer dependency for React was (incorrectly) set to >= 0.14.0, when 0.13.3 was supported.
