'use strict';

var _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; };

var isPromise = require('is-promise');
var memory = require('./storages/memory');
var defaultStorage = require('./storages/file-sync');

var init = function init(db, key, source) {
  var _ref = arguments.length > 3 && arguments[3] !== undefined ? arguments[3] : {},
      _ref$storage = _ref.storage,
      storage = _ref$storage === undefined ? defaultStorage : _ref$storage,
      _ref$format = _ref.format,
      format = _ref$format === undefined ? {} : _ref$format;

  db.source = source;

  // Set storage
  // In-memory only if no source is provided
  db.storage = _extends({}, memory, db.source && storage);

  db.read = function () {
    var s = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : source;

    var r = db.storage.read(s, format.deserialize);

    return isPromise(r) ? r.then(db.plant) : db.plant(r);
  };

  db.write = function () {
    var dest = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : source;

    var value = (arguments.length <= 1 ? 0 : arguments.length - 1) ? arguments.length <= 1 ? undefined : arguments[1] : db.getState();

    var w = db.storage.write(dest, db.getState(), format.serialize);
    return isPromise(w) ? w.then(function () {
      return value;
    }) : value;
  };

  db.plant = function (state) {
    db[key] = state;
    return db;
  };

  db.getState = function () {
    return db[key];
  };

  db.setState = function (state) {
    db.plant(state);
    return db.write();
  };

  return db.read();
};

module.exports = {
  init: init
};