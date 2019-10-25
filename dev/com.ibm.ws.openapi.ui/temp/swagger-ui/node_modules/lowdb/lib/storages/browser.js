'use strict';

/* global localStorage */

module.exports = {
  read: function browserRead(source) {
    var deserialize = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : JSON.parse;

    var data = localStorage.getItem(source);
    if (data) {
      return deserialize(data);
    } else {
      localStorage.setItem(source, '{}');
      return {};
    }
  },
  write: function browserWrite(dest, obj) {
    var serialize = arguments.length > 2 && arguments[2] !== undefined ? arguments[2] : JSON.stringify;

    localStorage.setItem(dest, serialize(obj));
  }
};