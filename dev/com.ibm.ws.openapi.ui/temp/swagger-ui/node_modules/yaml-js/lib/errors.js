(function() {
  var indexOf = [].indexOf || function(item) { for (var i = 0, l = this.length; i < l; i++) { if (i in this && this[i] === item) return i; } return -1; },
    extend = function(child, parent) { for (var key in parent) { if (hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; },
    hasProp = {}.hasOwnProperty;

  this.Mark = (function() {
    function Mark(line, column, buffer, pointer) {
      this.line = line;
      this.column = column;
      this.buffer = buffer;
      this.pointer = pointer;
    }

    Mark.prototype.get_snippet = function(indent, max_length) {
      var break_chars, end, head, ref, ref1, start, tail;
      if (indent == null) {
        indent = 4;
      }
      if (max_length == null) {
        max_length = 75;
      }
      if (this.buffer == null) {
        return null;
      }
      break_chars = '\x00\r\n\x85\u2028\u2029';
      head = '';
      start = this.pointer;
      while (start > 0 && (ref = this.buffer[start - 1], indexOf.call(break_chars, ref) < 0)) {
        start--;
        if (this.pointer - start > max_length / 2 - 1) {
          head = ' ... ';
          start += 5;
          break;
        }
      }
      tail = '';
      end = this.pointer;
      while (end < this.buffer.length && (ref1 = this.buffer[end], indexOf.call(break_chars, ref1) < 0)) {
        end++;
        if (end - this.pointer > max_length / 2 - 1) {
          tail = ' ... ';
          end -= 5;
          break;
        }
      }
      return "" + ((new Array(indent)).join(' ')) + head + this.buffer.slice(start, end) + tail + "\n" + ((new Array(indent + this.pointer - start + head.length)).join(' ')) + "^";
    };

    Mark.prototype.toString = function() {
      var snippet, where;
      snippet = this.get_snippet();
      where = "  on line " + (this.line + 1) + ", column " + (this.column + 1);
      if (snippet) {
        return where;
      } else {
        return where + ":\n" + snippet;
      }
    };

    return Mark;

  })();

  this.YAMLError = (function(superClass) {
    extend(YAMLError, superClass);

    function YAMLError(message) {
      this.message = message;
      YAMLError.__super__.constructor.call(this);
      this.stack = this.toString() + '\n' + (new Error).stack.split('\n').slice(1).join('\n');
    }

    YAMLError.prototype.toString = function() {
      return this.message;
    };

    return YAMLError;

  })(Error);

  this.MarkedYAMLError = (function(superClass) {
    extend(MarkedYAMLError, superClass);

    function MarkedYAMLError(context, context_mark, problem, problem_mark, note) {
      this.context = context;
      this.context_mark = context_mark;
      this.problem = problem;
      this.problem_mark = problem_mark;
      this.note = note;
      MarkedYAMLError.__super__.constructor.call(this);
    }

    MarkedYAMLError.prototype.toString = function() {
      var lines;
      lines = [];
      if (this.context != null) {
        lines.push(this.context);
      }
      if ((this.context_mark != null) && ((this.problem == null) || (this.problem_mark == null) || this.context_mark.line !== this.problem_mark.line || this.context_mark.column !== this.problem_mark.column)) {
        lines.push(this.context_mark.toString());
      }
      if (this.problem != null) {
        lines.push(this.problem);
      }
      if (this.problem_mark != null) {
        lines.push(this.problem_mark.toString());
      }
      if (this.note != null) {
        lines.push(this.note);
      }
      return lines.join('\n');
    };

    return MarkedYAMLError;

  })(this.YAMLError);

}).call(this);
