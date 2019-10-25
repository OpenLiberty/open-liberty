'use strict';

var flow = require('lodash/fp/flow');
var get = require('lodash/get');
var set = require('lodash/set');
var common = require('./common');

module.exports = function (source) {
  var opts = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : {};

  function db(path, defaultValue) {
    function getValue(funcs) {
      var result = get(db.getState(), path, defaultValue);
      return flow(funcs)(result);
    }

    getValue.write = function () {
      var result = getValue.apply(undefined, arguments);
      set(db.getState(), path, result);
      return db.write(source, result);
    };

    return getValue;
  }

  return common.init(db, '__state__', source, opts);
};