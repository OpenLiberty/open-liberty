'use strict';

var lodash = require('lodash');
var common = require('./common');

module.exports = function (source) {
  var opts = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : {};

  // Create a fresh copy of lodash
  var _ = lodash.runInContext();
  var db = _.chain({});

  // Expose _ for mixins
  db._ = _;

  // Add write function to lodash
  // Calls save before returning result
  _.prototype.write = _.wrap(_.prototype.value, function (func) {
    var dest = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : source;

    var funcRes = func.apply(this);
    return db.write(dest, funcRes);
  });

  return common.init(db, '__wrapped__', source, opts);
};