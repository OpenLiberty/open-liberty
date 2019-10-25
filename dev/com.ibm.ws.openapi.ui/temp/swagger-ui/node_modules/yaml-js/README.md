yaml-js
===

[![NPM](https://nodei.co/npm/yaml-js.png)](https://nodei.co/npm/yaml-js/)

yaml-js is a YAML loader and dumper, ported pretty much line-for-line from
[PyYAML](http://pyyaml.org/).  The goal for the project is to maintain a reliable and
specification-complete YAML processor in pure Javascript, with CoffeeScript source code.  You can
try it out [here](http://connec.github.com/yaml-js/).

Current Status
---

The library is being actively maintained for issues, and rather less actively developed for new/improved features.

Loading is stable and well-used, and passes the [yaml-spec](https://github.com/connec/yaml-spec)
test suite, which fairly thoroughly covers the YAML 'core' schema (if you notice anything missing,
create an issue).

Dumping is present but very lightly tested (auto-tests only, no significant usage).  The output
should therefore be correct YAML, however formatting is currently entirely untested.

If you use the library and find any bugs, or have any suggestions, don't hesitate to create an
[issue](https://github.com/connec/yaml-js/issues).

How Do I Get It?
---

    npm install yaml-js

How Do I Use It?
---

```javascript
// Server (e.g. node.js)
var yaml = require('yaml-js');

// Browser
// <script src='yaml.min.js'></script>

// Loading
console.log(yaml.load(
  '---\n' +
  'phrase1:\n' +
  '  - hello\n' +
  '  - &world world\n' +
  'phrase2:\n' +
  '  - goodbye\n' +
  '  - *world\n' +
  'phrase3: >\n' +
  '  What is up\n' +
  '  in this place.'
));
// { phrase1: [ 'hello', 'world' ],
//   phrase2: [ 'goodbye', 'world' ],
//   phrase3: 'What is up in this place.' }

// Dumping
console.log(yaml.dump({
  phrase1: [ 'hello',   'world' ],
  phrase2: [ 'goodbye', 'world' ],
  phrase3: 'What is up in this place.'
}));
// phrase1: [hello, world]
// phrase2: [goodbye, world]
// phrase3: What is up in this place.
```

### API summary

| Method          | Description                                                                                     |
|-----------------|-------------------------------------------------------------------------------------------------|
| **`load`**      | Parse the first YAML document in a stream and produce the corresponding Javascript object.      |
| **`dump`**      | Serialize a Javascript object into a YAML stream.                                               |
| `load_all`      | Parse all YAML documents in a stream and produce the corresponing Javascript objects.           |
| `dump_all`      | Serialize a sequence of Javascript objects into a YAML stream.                                  |
| `scan`          | Scan a YAML stream and produce tokens.                                                          |
| `parse`         | Parse a YAML stream and produce events.                                                         |
| `compose`       | Parse the first YAML document in a stream and produce the corresponding representation tree.    |
| `compose_all`   | Parse all YAML documents in a stream and produce corresponding representation trees.            |
| `emit`          | Emit YAML parsing events into a stream.                                                         |
| `serialize`     | Serialize a representation tree into a YAML stream.                                             |
| `serialize_all` | Serialize a sequence of representation trees into a YAML stream.                                |

License
---

[WTFPL](http://sam.zoy.org/wtfpl/)
