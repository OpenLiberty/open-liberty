utfstring
===

UTF-safe string operations for Javascript.

## What is this thing?

Javascript strings work great for holding text in English and other Latin-based languages, but they fall short when it comes to languages in Unicode's [astral plane](https://en.wikipedia.org/wiki/Plane_(Unicode)).

Consider this Javascript code. What number does `len` contain?

```javascript
var str = '𤔣';
var len = str.length;
```
If you said `1`, you're clearly a hopeful idealist. In fact, `len` contains `2`. To explain why this is, we need to understand a few things about the Unicode standard.

### Some History

Unicode isn't all that complicated. It's just a huge series of numbers, called "codepoints," one for each logical character. Unicode includes character sets for ideographic languages like Chinese, nearly [1,800 emojis](http://unicode.org/emoji/charts/full-emoji-list.html), and characters for scripts like Cherokee, Amharic, Greek, and Georgian (just to name a few). There are literally hundreds of thousands of characters specified in the Unicode standard.

#### Encoding

Encoding is the process of converting Unicode codepoints into binary data that can be written or transmitted by a computer system. Javascript strings are encoded in UTF-16, meaning every character takes up 16 bits, or 2 bytes (there are 8 bits per byte). The problem is that not every Unicode character can be encoded in 2 bytes, since 2<sup>16</sup> is only 65536 - not nearly enough space to represent each of the hundreds of thousands of Unicode characters.

#### Javascript's Solution

To mitigate this problem, Javascript (as well as other languages and platforms that use UTF-16 encoding) makes use of what are called "surrogate pairs." Surrogate pairs are two encoded characters that represent a single logical character. Together they are 4 bytes wide and can represent every Unicode character (2<sup>32</sup> = 4,294,967,296).

Unfortunately, that's where the good news ends. Javascript still counts each group of two bytes as a character, meaning any character made up of a surrogate pair looks like two logical characters to Javascript instead of just one. That's why `len` contains `2` in the example above.

Javascript's inability to correctly count surrogate pairs means a bunch of its string operations aren't safe to perform on foreign characters. This includes such favorites as `indexOf`, `slice`, and `substr`.

This library contains a number of UTF-safe string operations, including the ones I just mentioned. These operations respect surrogate pairs to ensure you're not caught off guard.

## Installation

UtfString is designed to be used in node.js or in the browser.

In node:

```javascript
var UtfString = require('utfstring');
```

In the browser, `UtfString` will be available on `window`.

## Usage

UtfString currently supports the following string operations:

* `charAt(String str, Integer index)` - Returns the character at the given index.

* `charCodeAt(String str, Integer index)` - Returns the Unicode codepoint at the given index.

* `fromCharCode(Integer codepoint)` - Returns the string for the given Unicode codepoint.

* `indexOf(String str, String searchValue, [Integer start])` - Finds the first instance of the search value within the string. Starts at an optional offset.

* `lastIndexOf(Str string, string searchValue, [Integer start])` - Finds the last instance of the search value within the string. Starts searching backwards at an optional offset, which can be negative.

* `slice(String str, Integer start, Integer finish)` - Returns the characters between the two given indices.

* `substr(String str, Integer start, Integer length)` - Returns the characters starting at the given start index up to the start index plus the given length. Also aliased as `substring`.

* `length(String str)` - Returns the number of logical characters in the given string.

* `stringToCodePoints(String str)` - Converts a string into an array of codepoints.

* `codePointsToString(Array arr)` - Converts an array of codepoints into a string.

* `stringToBytes(String str)` - Converts a string into an array of UTF-16 bytes.

* `bytesToString(Array arr)` - Converts an array of UTF-16 bytes into a string.

* `stringToCharArray(String str)` - Converts the given string into an array of invidivual logical characters. Note that each entry in the returned array may be more than one UTF-16 character.

* `findByteIndex(String str, Integer charIndex)` - Finds the byte index for the given character index. Note: a "byte index" is really a "JavaScript string index", not a true byte offset. Use this function to convert a UTF character boundary to a JavaScript string index.

* `findCharIndex(String str, Integer byteIndex)` - Finds the character index for the given byte index. Note: a "byte index" is really a "JavaSciprt string index", not a true byte offset. Use this function to convert a JavaScript string index to (the closest) UTF character boundary.

## Regional Indicators

Certain characters in the Unicode standard are meant to be combined by display systems, but are represented by multiple code points. A good example are the so-called regional indicators. By themselves, regional indicators u1F1EB (regional indicator symbol letter F) and u1F1F7 (regional indicator symbol letter R) don't mean much, but combined they form the French flag: 🇫🇷.

Since regional indicators are semantically individual Unicode code points and because utfstring is a dependency of other Unicode-aware libraries, it doesn't make sense for utfstring to treat two regional indicators as a single character by default. That said, it can be useful to treat them as such from a display or layout perspective. In order to support both scenarios, two implementations are necessary. The first and default implementation is available via the instructions above. For visual grapheme clustering such as the grouping of regional indicators, use the `visual` property on `UtfString`. Display-aware versions of all the functions described above are available. The difference can be seen by way of the `length` function:

```javascript
UtfString.visual.length("🇫🇷");  // 1
UtfString.length("🇫🇷");         // 2
```

## Running Tests

Tests are written in Jasmine and can be executed via [jasmine-node](https://github.com/mhevery/jasmine-node):

1. `npm install -g jasmine-node`
2. `jasmine-node spec`

## Authors

Written and maintained by Cameron C. Dutro ([@camertron](https://github.com/camertron)).

## License

Copyright 2016 Cameron Dutro, licensed under the MIT license.
