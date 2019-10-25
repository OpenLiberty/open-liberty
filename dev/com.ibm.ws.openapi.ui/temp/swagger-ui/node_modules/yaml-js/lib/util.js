
/*
A small class to stand-in for a stream when you simply want to write to a string.
 */

(function() {
  var ref, ref1, ref2,
    slice = [].slice,
    hasProp = {}.hasOwnProperty;

  this.StringStream = (function() {
    function StringStream() {
      this.string = '';
    }

    StringStream.prototype.write = function(chunk) {
      return this.string += chunk;
    };

    return StringStream;

  })();

  this.clone = (function(_this) {
    return function(obj) {
      return _this.extend({}, obj);
    };
  })(this);

  this.extend = function() {
    var destination, i, k, len, source, sources, v;
    destination = arguments[0], sources = 2 <= arguments.length ? slice.call(arguments, 1) : [];
    for (i = 0, len = sources.length; i < len; i++) {
      source = sources[i];
      for (k in source) {
        v = source[k];
        destination[k] = v;
      }
    }
    return destination;
  };

  this.is_empty = function(obj) {
    var key;
    if (Array.isArray(obj) || typeof obj === 'string') {
      return obj.length === 0;
    }
    for (key in obj) {
      if (!hasProp.call(obj, key)) continue;
      return false;
    }
    return true;
  };

  this.inspect = (ref = (ref1 = (ref2 = require('util')) != null ? ref2.inspect : void 0) != null ? ref1 : global.inspect) != null ? ref : function(a) {
    return "" + a;
  };

  this.pad_left = function(str, char, length) {
    str = String(str);
    if (str.length >= length) {
      return str;
    } else if (str.length + 1 === length) {
      return "" + char + str;
    } else {
      return "" + (new Array(length - str.length + 1).join(char)) + str;
    }
  };

  this.to_hex = function(num) {
    if (typeof num === 'string') {
      num = num.charCodeAt(0);
    }
    return num.toString(16);
  };

}).call(this);
