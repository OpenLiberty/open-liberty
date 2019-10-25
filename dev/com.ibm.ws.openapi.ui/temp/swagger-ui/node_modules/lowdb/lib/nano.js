'use strict';

var common = require('./common');

module.exports = function (source) {
  var opts = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : {};

  return common.init({}, '__state__', source, opts);
};