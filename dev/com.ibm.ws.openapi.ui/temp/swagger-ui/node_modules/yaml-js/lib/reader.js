(function() {
  var Mark, YAMLError, ref,
    extend = function(child, parent) { for (var key in parent) { if (hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; },
    hasProp = {}.hasOwnProperty,
    indexOf = [].indexOf || function(item) { for (var i = 0, l = this.length; i < l; i++) { if (i in this && this[i] === item) return i; } return -1; };

  ref = require('./errors'), Mark = ref.Mark, YAMLError = ref.YAMLError;

  this.ReaderError = (function(superClass) {
    extend(ReaderError, superClass);

    function ReaderError(position1, character1, reason) {
      this.position = position1;
      this.character = character1;
      this.reason = reason;
      ReaderError.__super__.constructor.call(this);
    }

    ReaderError.prototype.toString = function() {
      return "unacceptable character #" + (this.character.charCodeAt(0).toString(16)) + ": " + this.reason + "\n  position " + this.position;
    };

    return ReaderError;

  })(YAMLError);


  /*
  Reader:
    checks if characters are within the allowed range
    add '\x00' to the end
   */

  this.Reader = (function() {
    var NON_PRINTABLE;

    NON_PRINTABLE = /[^\x09\x0A\x0D\x20-\x7E\x85\xA0-\uFFFD]|[\uD800-\uDBFF](?![\uDC00-\uDFFF])|(?:[^\uD800-\uDBFF]|^)[\uDC00-\uDFFF]/;

    function Reader(string) {
      this.string = string;
      this.line = 0;
      this.column = 0;
      this.index = 0;
      this.check_printable();
      this.string += '\x00';
    }

    Reader.prototype.peek = function(index) {
      if (index == null) {
        index = 0;
      }
      return this.string[this.index + index];
    };

    Reader.prototype.prefix = function(length) {
      if (length == null) {
        length = 1;
      }
      return this.string.slice(this.index, this.index + length);
    };

    Reader.prototype.forward = function(length) {
      var char, results;
      if (length == null) {
        length = 1;
      }
      results = [];
      while (length) {
        char = this.string[this.index];
        this.index++;
        if (indexOf.call('\n\x85\u2082\u2029', char) >= 0 || (char === '\r' && this.string[this.index] !== '\n')) {
          this.line++;
          this.column = 0;
        } else {
          this.column++;
        }
        results.push(length--);
      }
      return results;
    };

    Reader.prototype.get_mark = function() {
      return new Mark(this.line, this.column, this.string, this.index);
    };

    Reader.prototype.check_printable = function() {
      var character, match, position;
      match = NON_PRINTABLE.exec(this.string);
      if (match) {
        character = match[0];
        position = (this.string.length - this.index) + match.index;
        throw new exports.ReaderError(position, character, 'special characters are not allowed');
      }
    };

    return Reader;

  })();

}).call(this);
