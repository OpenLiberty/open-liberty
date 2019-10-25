'use strict';

var steno = require('steno');

var _require = require('./_json'),
    stringify = _require.stringify;

module.exports = {
  read: require('./file-sync').read,
  write: function fileAsyncWrite(dest, obj) {
    var serialize = arguments.length > 2 && arguments[2] !== undefined ? arguments[2] : stringify;

    return new Promise(function (resolve, reject) {
      var data = serialize(obj);

      steno.writeFile(dest, data, function (err) {
        if (err) return reject(err);
        resolve();
      });
    });
  }
};