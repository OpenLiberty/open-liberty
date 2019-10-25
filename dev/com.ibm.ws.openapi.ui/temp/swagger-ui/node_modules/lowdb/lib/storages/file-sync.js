'use strict';

var fs = require('graceful-fs');

var _require = require('./_json'),
    parse = _require.parse,
    stringify = _require.stringify;

module.exports = {
  read: function fileSyncRead(source) {
    var deserialize = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : parse;

    if (fs.existsSync(source)) {
      // Read database
      var data = fs.readFileSync(source, 'utf-8').trim() || '{}';

      try {
        return deserialize(data);
      } catch (e) {
        if (e instanceof SyntaxError) {
          e.message = 'Malformed JSON in file: ' + source + '\n' + e.message;
        }
        throw e;
      }
    } else {
      // Initialize empty database
      fs.writeFileSync(source, '{}');
      return {};
    }
  },
  write: function fileSyncWrite(dest, obj) {
    var serialize = arguments.length > 2 && arguments[2] !== undefined ? arguments[2] : stringify;

    var data = serialize(obj);
    fs.writeFileSync(dest, data);
  }
};