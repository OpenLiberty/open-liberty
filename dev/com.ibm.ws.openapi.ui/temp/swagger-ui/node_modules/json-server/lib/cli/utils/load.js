'use strict';

var path = require('path');
var request = require('request');
var low = require('lowdb');
var fileAsync = require('lowdb/lib/storages/file-async');
var is = require('./is');

module.exports = function (source, cb) {
  if (is.URL(source)) {
    // Load remote data
    var opts = {
      url: source,
      json: true
    };

    request(opts, function (err, response) {
      if (err) return cb(err);
      cb(null, response.body);
    });
  } else if (is.JS(source)) {
    // Clear cache
    var filename = path.resolve(source);
    delete require.cache[filename];
    var dataFn = require(filename);

    if (typeof dataFn !== 'function') {
      throw new Error('The database is a JavaScript file but the export is not a function.');
    }

    // Run dataFn to generate data
    var data = dataFn();
    cb(null, data);
  } else if (is.JSON(source)) {
    // Load JSON using lowdb
    var _data = low(source, { storage: fileAsync }).getState();
    cb(null, _data);
  } else {
    throw new Error(`Unsupported source ${source}`);
  }
};