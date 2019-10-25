(function() {
  var ScalarAnalysis, YAMLError, events, util,
    extend = function(child, parent) { for (var key in parent) { if (hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; },
    hasProp = {}.hasOwnProperty,
    indexOf = [].indexOf || function(item) { for (var i = 0, l = this.length; i < l; i++) { if (i in this && this[i] === item) return i; } return -1; };

  events = require('./events');

  util = require('./util');

  YAMLError = require('./errors').YAMLError;

  this.EmitterError = (function(superClass) {
    extend(EmitterError, superClass);

    function EmitterError() {
      return EmitterError.__super__.constructor.apply(this, arguments);
    }

    return EmitterError;

  })(YAMLError);


  /*
  Emitter expects events obeying the following grammar:
  
  stream   ::= STREAM-START document* STREAM-END
  document ::= DOCUMENT-START node DOCUMENT-END
  node     ::= SCALA | sequence | mapping
  sequence ::= SEQUENCE-START node* SEQUENCE-END
  mapping  ::= MAPPING-START (node node)* MAPPING-END
   */

  this.Emitter = (function() {
    var C_WHITESPACE, DEFAULT_TAG_PREFIXES, ESCAPE_REPLACEMENTS;

    C_WHITESPACE = '\0 \t\r\n\x85\u2028\u2029';

    DEFAULT_TAG_PREFIXES = {
      '!': '!',
      'tag:yaml.org,2002:': '!!'
    };

    ESCAPE_REPLACEMENTS = {
      '\0': '0',
      '\x07': 'a',
      '\x08': 'b',
      '\x09': 't',
      '\x0A': 'n',
      '\x0B': 'v',
      '\x0C': 'f',
      '\x0D': 'r',
      '\x1B': 'e',
      '"': '"',
      '\\': '\\',
      '\x85': 'N',
      '\xA0': '_',
      '\u2028': 'L',
      '\u2029': 'P'
    };

    function Emitter(stream, options) {
      var ref;
      this.stream = stream;
      this.encoding = null;
      this.states = [];
      this.state = this.expect_stream_start;
      this.events = [];
      this.event = null;
      this.indents = [];
      this.indent = null;
      this.flow_level = 0;
      this.root_context = false;
      this.sequence_context = false;
      this.mapping_context = false;
      this.simple_key_context = false;
      this.line = 0;
      this.column = 0;
      this.whitespace = true;
      this.indentation = true;
      this.open_ended = false;
      this.canonical = options.canonical, this.allow_unicode = options.allow_unicode;
      if (this.canonical == null) {
        this.canonical = false;
      }
      if (this.allow_unicode == null) {
        this.allow_unicode = true;
      }
      this.best_indent = 1 < options.indent && options.indent < 10 ? options.indent : 2;
      this.best_width = options.width > this.indent * 2 ? options.width : 80;
      this.best_line_break = (ref = options.line_break) === '\r' || ref === '\n' || ref === '\r\n' ? options.line_break : '\n';
      this.tag_prefixes = null;
      this.prepared_anchor = null;
      this.prepared_tag = null;
      this.analysis = null;
      this.style = null;
    }


    /*
    Reset the state attributes (to clear self-references)
     */

    Emitter.prototype.dispose = function() {
      this.states = [];
      return this.state = null;
    };

    Emitter.prototype.emit = function(event) {
      var results;
      this.events.push(event);
      results = [];
      while (!this.need_more_events()) {
        this.event = this.events.shift();
        this.state();
        results.push(this.event = null);
      }
      return results;
    };


    /*
    In some cases, we wait for a few next events before emitting.
     */

    Emitter.prototype.need_more_events = function() {
      var event;
      if (this.events.length === 0) {
        return true;
      }
      event = this.events[0];
      if (event instanceof events.DocumentStartEvent) {
        return this.need_events(1);
      } else if (event instanceof events.SequenceStartEvent) {
        return this.need_events(2);
      } else if (event instanceof events.MappingStartEvent) {
        return this.need_events(3);
      } else {
        return false;
      }
    };

    Emitter.prototype.need_events = function(count) {
      var event, i, len, level, ref;
      level = 0;
      ref = this.events.slice(1);
      for (i = 0, len = ref.length; i < len; i++) {
        event = ref[i];
        if (event instanceof events.DocumentStartEvent || event instanceof events.CollectionStartEvent) {
          level++;
        } else if (event instanceof events.DocumentEndEvent || event instanceof events.CollectionEndEvent) {
          level--;
        } else if (event instanceof events.StreamEndEvent) {
          level = -1;
        }
        if (level < 0) {
          return false;
        }
      }
      return this.events.length < count + 1;
    };

    Emitter.prototype.increase_indent = function(options) {
      if (options == null) {
        options = {};
      }
      this.indents.push(this.indent);
      if (this.indent == null) {
        return this.indent = options.flow ? this.best_indent : 0;
      } else if (!options.indentless) {
        return this.indent += this.best_indent;
      }
    };

    Emitter.prototype.expect_stream_start = function() {
      if (this.event instanceof events.StreamStartEvent) {
        if (this.event.encoding && !('encoding' in this.stream)) {
          this.encoding = this.event.encoding;
        }
        this.write_stream_start();
        return this.state = this.expect_first_document_start;
      } else {
        return this.error('expected StreamStartEvent, but got', this.event);
      }
    };

    Emitter.prototype.expect_nothing = function() {
      return this.error('expected nothing, but got', this.event);
    };

    Emitter.prototype.expect_first_document_start = function() {
      return this.expect_document_start(true);
    };

    Emitter.prototype.expect_document_start = function(first) {
      var explicit, handle, i, k, len, prefix, ref;
      if (first == null) {
        first = false;
      }
      if (this.event instanceof events.DocumentStartEvent) {
        if ((this.event.version || this.event.tags) && this.open_ended) {
          this.write_indicator('...', true);
          this.write_indent();
        }
        if (this.event.version) {
          this.write_version_directive(this.prepare_version(this.event.version));
        }
        this.tag_prefixes = util.clone(DEFAULT_TAG_PREFIXES);
        if (this.event.tags) {
          ref = ((function() {
            var ref, results;
            ref = this.event.tags;
            results = [];
            for (k in ref) {
              if (!hasProp.call(ref, k)) continue;
              results.push(k);
            }
            return results;
          }).call(this)).sort();
          for (i = 0, len = ref.length; i < len; i++) {
            handle = ref[i];
            prefix = this.event.tags[handle];
            this.tag_prefixes[prefix] = handle;
            this.write_tag_directive(this.prepare_tag_handle(handle), this.prepare_tag_prefix(prefix));
          }
        }
        explicit = !first || this.event.explicit || this.canonical || this.event.version || this.event.tags || this.check_empty_document();
        if (explicit) {
          this.write_indent();
          this.write_indicator('---', true);
          if (this.canonical) {
            this.write_indent();
          }
        }
        return this.state = this.expect_document_root;
      } else if (this.event instanceof events.StreamEndEvent) {
        if (this.open_ended) {
          this.write_indicator('...', true);
          this.write_indent();
        }
        this.write_stream_end();
        return this.state = this.expect_nothing;
      } else {
        return this.error('expected DocumentStartEvent, but got', this.event);
      }
    };

    Emitter.prototype.expect_document_end = function() {
      if (this.event instanceof events.DocumentEndEvent) {
        this.write_indent();
        if (this.event.explicit) {
          this.write_indicator('...', true);
          this.write_indent();
        }
        this.flush_stream();
        return this.state = this.expect_document_start;
      } else {
        return this.error('expected DocumentEndEvent, but got', this.event);
      }
    };

    Emitter.prototype.expect_document_root = function() {
      this.states.push(this.expect_document_end);
      return this.expect_node({
        root: true
      });
    };

    Emitter.prototype.expect_node = function(expect) {
      if (expect == null) {
        expect = {};
      }
      this.root_context = !!expect.root;
      this.sequence_context = !!expect.sequence;
      this.mapping_context = !!expect.mapping;
      this.simple_key_context = !!expect.simple_key;
      if (this.event instanceof events.AliasEvent) {
        return this.expect_alias();
      } else if (this.event instanceof events.ScalarEvent || this.event instanceof events.CollectionStartEvent) {
        this.process_anchor('&');
        this.process_tag();
        if (this.event instanceof events.ScalarEvent) {
          return this.expect_scalar();
        } else if (this.event instanceof events.SequenceStartEvent) {
          if (this.flow_level || this.canonical || this.event.flow_style || this.check_empty_sequence()) {
            return this.expect_flow_sequence();
          } else {
            return this.expect_block_sequence();
          }
        } else if (this.event instanceof events.MappingStartEvent) {
          if (this.flow_level || this.canonical || this.event.flow_style || this.check_empty_mapping()) {
            return this.expect_flow_mapping();
          } else {
            return this.expect_block_mapping();
          }
        }
      } else {
        return this.error('expected NodeEvent, but got', this.event);
      }
    };

    Emitter.prototype.expect_alias = function() {
      if (!this.event.anchor) {
        this.error('anchor is not specified for alias');
      }
      this.process_anchor('*');
      return this.state = this.states.pop();
    };

    Emitter.prototype.expect_scalar = function() {
      this.increase_indent({
        flow: true
      });
      this.process_scalar();
      this.indent = this.indents.pop();
      return this.state = this.states.pop();
    };

    Emitter.prototype.expect_flow_sequence = function() {
      this.write_indicator('[', true, {
        whitespace: true
      });
      this.flow_level++;
      this.increase_indent({
        flow: true
      });
      return this.state = this.expect_first_flow_sequence_item;
    };

    Emitter.prototype.expect_first_flow_sequence_item = function() {
      if (this.event instanceof events.SequenceEndEvent) {
        this.indent = this.indents.pop();
        this.flow_level--;
        this.write_indicator(']', false);
        return this.state = this.states.pop();
      } else {
        if (this.canonical || this.column > this.best_width) {
          this.write_indent();
        }
        this.states.push(this.expect_flow_sequence_item);
        return this.expect_node({
          sequence: true
        });
      }
    };

    Emitter.prototype.expect_flow_sequence_item = function() {
      if (this.event instanceof events.SequenceEndEvent) {
        this.indent = this.indents.pop();
        this.flow_level--;
        if (this.canonical) {
          this.write_indicator(',', false);
          this.write_indent();
        }
        this.write_indicator(']', false);
        return this.state = this.states.pop();
      } else {
        this.write_indicator(',', false);
        if (this.canonical || this.column > this.best_width) {
          this.write_indent();
        }
        this.states.push(this.expect_flow_sequence_item);
        return this.expect_node({
          sequence: true
        });
      }
    };

    Emitter.prototype.expect_flow_mapping = function() {
      this.write_indicator('{', true, {
        whitespace: true
      });
      this.flow_level++;
      this.increase_indent({
        flow: true
      });
      return this.state = this.expect_first_flow_mapping_key;
    };

    Emitter.prototype.expect_first_flow_mapping_key = function() {
      if (this.event instanceof events.MappingEndEvent) {
        this.indent = this.indents.pop();
        this.flow_level--;
        this.write_indicator('}', false);
        return this.state = this.states.pop();
      } else {
        if (this.canonical || this.column > this.best_width) {
          this.write_indent();
        }
        if (!this.canonical && this.check_simple_key()) {
          this.states.push(this.expect_flow_mapping_simple_value);
          return this.expect_node({
            mapping: true,
            simple_key: true
          });
        } else {
          this.write_indicator('?', true);
          this.states.push(this.expect_flow_mapping_value);
          return this.expect_node({
            mapping: true
          });
        }
      }
    };

    Emitter.prototype.expect_flow_mapping_key = function() {
      if (this.event instanceof events.MappingEndEvent) {
        this.indent = this.indents.pop();
        this.flow_level--;
        if (this.canonical) {
          this.write_indicator(',', false);
          this.write_indent();
        }
        this.write_indicator('}', false);
        return this.state = this.states.pop();
      } else {
        this.write_indicator(',', false);
        if (this.canonical || this.column > this.best_width) {
          this.write_indent();
        }
        if (!this.canonical && this.check_simple_key()) {
          this.states.push(this.expect_flow_mapping_simple_value);
          return this.expect_node({
            mapping: true,
            simple_key: true
          });
        } else {
          this.write_indicator('?', true);
          this.states.push(this.expect_flow_mapping_value);
          return this.expect_node({
            mapping: true
          });
        }
      }
    };

    Emitter.prototype.expect_flow_mapping_simple_value = function() {
      this.write_indicator(':', false);
      this.states.push(this.expect_flow_mapping_key);
      return this.expect_node({
        mapping: true
      });
    };

    Emitter.prototype.expect_flow_mapping_value = function() {
      if (this.canonical || this.column > this.best_width) {
        this.write_indent();
      }
      this.write_indicator(':', true);
      this.states.push(this.expect_flow_mapping_key);
      return this.expect_node({
        mapping: true
      });
    };

    Emitter.prototype.expect_block_sequence = function() {
      var indentless;
      indentless = this.mapping_context && !this.indentation;
      this.increase_indent({
        indentless: indentless
      });
      return this.state = this.expect_first_block_sequence_item;
    };

    Emitter.prototype.expect_first_block_sequence_item = function() {
      return this.expect_block_sequence_item(true);
    };

    Emitter.prototype.expect_block_sequence_item = function(first) {
      if (first == null) {
        first = false;
      }
      if (!first && this.event instanceof events.SequenceEndEvent) {
        this.indent = this.indents.pop();
        return this.state = this.states.pop();
      } else {
        this.write_indent();
        this.write_indicator('-', true, {
          indentation: true
        });
        this.states.push(this.expect_block_sequence_item);
        return this.expect_node({
          sequence: true
        });
      }
    };

    Emitter.prototype.expect_block_mapping = function() {
      this.increase_indent();
      return this.state = this.expect_first_block_mapping_key;
    };

    Emitter.prototype.expect_first_block_mapping_key = function() {
      return this.expect_block_mapping_key(true);
    };

    Emitter.prototype.expect_block_mapping_key = function(first) {
      if (first == null) {
        first = false;
      }
      if (!first && this.event instanceof events.MappingEndEvent) {
        this.indent = this.indents.pop();
        return this.state = this.states.pop();
      } else {
        this.write_indent();
        if (this.check_simple_key()) {
          this.states.push(this.expect_block_mapping_simple_value);
          return this.expect_node({
            mapping: true,
            simple_key: true
          });
        } else {
          this.write_indicator('?', true, {
            indentation: true
          });
          this.states.push(this.expect_block_mapping_value);
          return this.expect_node({
            mapping: true
          });
        }
      }
    };

    Emitter.prototype.expect_block_mapping_simple_value = function() {
      this.write_indicator(':', false);
      this.states.push(this.expect_block_mapping_key);
      return this.expect_node({
        mapping: true
      });
    };

    Emitter.prototype.expect_block_mapping_value = function() {
      this.write_indent();
      this.write_indicator(':', true, {
        indentation: true
      });
      this.states.push(this.expect_block_mapping_key);
      return this.expect_node({
        mapping: true
      });
    };

    Emitter.prototype.check_empty_document = function() {
      var event;
      if (!(this.event instanceof events.DocumentStartEvent) || this.events.length === 0) {
        return false;
      }
      event = this.events[0];
      return event instanceof events.ScalarEvent && (event.anchor == null) && (event.tag == null) && event.implicit && event.value === '';
    };

    Emitter.prototype.check_empty_sequence = function() {
      return this.event instanceof events.SequenceStartEvent && this.events[0] instanceof events.SequenceEndEvent;
    };

    Emitter.prototype.check_empty_mapping = function() {
      return this.event instanceof events.MappingStartEvent && this.events[0] instanceof events.MappingEndEvent;
    };

    Emitter.prototype.check_simple_key = function() {
      var length;
      length = 0;
      if (this.event instanceof events.NodeEvent && (this.event.anchor != null)) {
        if (this.prepared_anchor == null) {
          this.prepared_anchor = this.prepare_anchor(this.event.anchor);
        }
        length += this.prepared_anchor.length;
      }
      if ((this.event.tag != null) && (this.event instanceof events.ScalarEvent || this.event instanceof events.CollectionStartEvent)) {
        if (this.prepared_tag == null) {
          this.prepared_tag = this.prepare_tag(this.event.tag);
        }
        length += this.prepared_tag.length;
      }
      if (this.event instanceof events.ScalarEvent) {
        if (this.analysis == null) {
          this.analysis = this.analyze_scalar(this.event.value);
        }
        length += this.analysis.scalar.length;
      }
      return length < 128 && (this.event instanceof events.AliasEvent || (this.event instanceof events.ScalarEvent && !this.analysis.empty && !this.analysis.multiline) || this.check_empty_sequence() || this.check_empty_mapping());
    };

    Emitter.prototype.process_anchor = function(indicator) {
      if (this.event.anchor == null) {
        this.prepared_anchor = null;
        return;
      }
      if (this.prepared_anchor == null) {
        this.prepared_anchor = this.prepare_anchor(this.event.anchor);
      }
      if (this.prepared_anchor) {
        this.write_indicator("" + indicator + this.prepared_anchor, true);
      }
      return this.prepared_anchor = null;
    };

    Emitter.prototype.process_tag = function() {
      var tag;
      tag = this.event.tag;
      if (this.event instanceof events.ScalarEvent) {
        if (this.style == null) {
          this.style = this.choose_scalar_style();
        }
        if ((!this.canonical || (tag == null)) && ((this.style === '' && this.event.implicit[0]) || (this.style !== '' && this.event.implicit[1]))) {
          this.prepared_tag = null;
          return;
        }
        if (this.event.implicit[0] && (tag == null)) {
          tag = '!';
          this.prepared_tag = null;
        }
      } else if ((!this.canonical || (tag == null)) && this.event.implicit) {
        this.prepared_tag = null;
        return;
      }
      if (tag == null) {
        this.error('tag is not specified');
      }
      if (this.prepared_tag == null) {
        this.prepared_tag = this.prepare_tag(tag);
      }
      this.write_indicator(this.prepared_tag, true);
      return this.prepared_tag = null;
    };

    Emitter.prototype.process_scalar = function() {
      var split;
      if (this.analysis == null) {
        this.analysis = this.analyze_scalar(this.event.value);
      }
      if (this.style == null) {
        this.style = this.choose_scalar_style();
      }
      split = !this.simple_key_context;
      switch (this.style) {
        case '"':
          this.write_double_quoted(this.analysis.scalar, split);
          break;
        case "'":
          this.write_single_quoted(this.analysis.scalar, split);
          break;
        case '>':
          this.write_folded(this.analysis.scalar);
          break;
        case '|':
          this.write_literal(this.analysis.scalar);
          break;
        default:
          this.write_plain(this.analysis.scalar, split);
      }
      this.analysis = null;
      return this.style = null;
    };

    Emitter.prototype.choose_scalar_style = function() {
      var ref;
      if (this.analysis == null) {
        this.analysis = this.analyze_scalar(this.event.value);
      }
      if (this.event.style === '"' || this.canonical) {
        return '"';
      }
      if (!this.event.style && this.event.implicit[0] && !(this.simple_key_context && (this.analysis.empty || this.analysis.multiline)) && ((this.flow_level && this.analysis.allow_flow_plain) || (!this.flow_level && this.analysis.allow_block_plain))) {
        return '';
      }
      if (this.event.style && (ref = this.event.style, indexOf.call('|>', ref) >= 0) && !this.flow_level && !this.simple_key_context && this.analysis.allow_block) {
        return this.event.style;
      }
      if ((!this.event.style || this.event.style === "'") && this.analysis.allow_single_quoted && !(this.simple_key_context && this.analysis.multiline)) {
        return "'";
      }
      return '"';
    };

    Emitter.prototype.prepare_version = function(arg) {
      var major, minor, version;
      major = arg[0], minor = arg[1];
      version = major + "." + minor;
      if (major === 1) {
        return version;
      } else {
        return this.error('unsupported YAML version', version);
      }
    };

    Emitter.prototype.prepare_tag_handle = function(handle) {
      var char, i, len, ref;
      if (!handle) {
        this.error('tag handle must not be empty');
      }
      if (handle[0] !== '!' || handle.slice(-1) !== '!') {
        this.error("tag handle must start and end with '!':", handle);
      }
      ref = handle.slice(1, -1);
      for (i = 0, len = ref.length; i < len; i++) {
        char = ref[i];
        if (!(('0' <= char && char <= '9') || ('A' <= char && char <= 'Z') || ('a' <= char && char <= 'z') || indexOf.call('-_', char) >= 0)) {
          this.error("invalid character '" + char + "' in the tag handle:", handle);
        }
      }
      return handle;
    };

    Emitter.prototype.prepare_tag_prefix = function(prefix) {
      var char, chunks, end, start;
      if (!prefix) {
        this.error('tag prefix must not be empty');
      }
      chunks = [];
      start = 0;
      end = +(prefix[0] === '!');
      while (end < prefix.length) {
        char = prefix[end];
        if (('0' <= char && char <= '9') || ('A' <= char && char <= 'Z') || ('a' <= char && char <= 'z') || indexOf.call('-;/?!:@&=+$,_.~*\'()[]', char) >= 0) {
          end++;
        } else {
          if (start < end) {
            chunks.push(prefix.slice(start, end));
          }
          start = end = end + 1;
          chunks.push(char);
        }
      }
      if (start < end) {
        chunks.push(prefix.slice(start, end));
      }
      return chunks.join('');
    };

    Emitter.prototype.prepare_tag = function(tag) {
      var char, chunks, end, handle, i, k, len, prefix, ref, start, suffix, suffix_text;
      if (!tag) {
        this.error('tag must not be empty');
      }
      if (tag === '!') {
        return tag;
      }
      handle = null;
      suffix = tag;
      ref = ((function() {
        var ref, results;
        ref = this.tag_prefixes;
        results = [];
        for (k in ref) {
          if (!hasProp.call(ref, k)) continue;
          results.push(k);
        }
        return results;
      }).call(this)).sort();
      for (i = 0, len = ref.length; i < len; i++) {
        prefix = ref[i];
        if (tag.indexOf(prefix) === 0 && (prefix === '!' || prefix.length < tag.length)) {
          handle = this.tag_prefixes[prefix];
          suffix = tag.slice(prefix.length);
        }
      }
      chunks = [];
      start = end = 0;
      while (end < suffix.length) {
        char = suffix[end];
        if (('0' <= char && char <= '9') || ('A' <= char && char <= 'Z') || ('a' <= char && char <= 'z') || indexOf.call('-;/?!:@&=+$,_.~*\'()[]', char) >= 0 || (char === '!' && handle !== '!')) {
          end++;
        } else {
          if (start < end) {
            chunks.push(suffix.slice(start, end));
          }
          start = end = end + 1;
          chunks.push(char);
        }
      }
      if (start < end) {
        chunks.push(suffix.slice(start, end));
      }
      suffix_text = chunks.join('');
      if (handle) {
        return "" + handle + suffix_text;
      } else {
        return "!<" + suffix_text + ">";
      }
    };

    Emitter.prototype.prepare_anchor = function(anchor) {
      var char, i, len;
      if (!anchor) {
        this.error('anchor must not be empty');
      }
      for (i = 0, len = anchor.length; i < len; i++) {
        char = anchor[i];
        if (!(('0' <= char && char <= '9') || ('A' <= char && char <= 'Z') || ('a' <= char && char <= 'z') || indexOf.call('-_', char) >= 0)) {
          this.error("invalid character '" + char + "' in the anchor:", anchor);
        }
      }
      return anchor;
    };

    Emitter.prototype.analyze_scalar = function(scalar) {
      var allow_block, allow_block_plain, allow_double_quoted, allow_flow_plain, allow_single_quoted, block_indicators, break_space, char, flow_indicators, followed_by_whitespace, i, index, leading_break, leading_space, len, line_breaks, preceded_by_whitespace, previous_break, previous_space, ref, ref1, space_break, special_characters, trailing_break, trailing_space, unicode_characters;
      if (!scalar) {
        new ScalarAnalysis(scalar, true, false, false, true, true, true, false);
      }
      block_indicators = false;
      flow_indicators = false;
      line_breaks = false;
      special_characters = false;
      unicode_characters = false;
      leading_space = false;
      leading_break = false;
      trailing_space = false;
      trailing_break = false;
      break_space = false;
      space_break = false;
      if (scalar.indexOf('---') === 0 || scalar.indexOf('...') === 0) {
        block_indicators = true;
        flow_indicators = true;
      }
      preceded_by_whitespace = true;
      followed_by_whitespace = scalar.length === 1 || (ref = scalar[1], indexOf.call('\0 \t\r\n\x85\u2028\u2029', ref) >= 0);
      previous_space = false;
      previous_break = false;
      index = 0;
      for (index = i = 0, len = scalar.length; i < len; index = ++i) {
        char = scalar[index];
        if (index === 0) {
          if (indexOf.call('#,[]{}&*!|>\'"%@`', char) >= 0 || (char === '-' && followed_by_whitespace)) {
            flow_indicators = true;
            block_indicators = true;
          } else if (indexOf.call('?:', char) >= 0) {
            flow_indicators = true;
            if (followed_by_whitespace) {
              block_indicators = true;
            }
          }
        } else {
          if (indexOf.call(',?[]{}', char) >= 0) {
            flow_indicators = true;
          } else if (char === ':') {
            flow_indicators = true;
            if (followed_by_whitespace) {
              block_indicators = true;
            }
          } else if (char === '#' && preceded_by_whitespace) {
            flow_indicators = true;
            block_indicators = true;
          }
        }
        if (indexOf.call('\n\x85\u2028\u2029', char) >= 0) {
          line_breaks = true;
        }
        if (!(char === '\n' || ('\x20' <= char && char <= '\x7e'))) {
          if (char !== '\uFEFF' && (char === '\x85' || ('\xA0' <= char && char <= '\uD7FF') || ('\uE000' <= char && char <= '\uFFFD'))) {
            unicode_characters = true;
            if (!this.allow_unicode) {
              special_characters = true;
            }
          } else {
            special_characters = true;
          }
        }
        if (char === ' ') {
          if (index === 0) {
            leading_space = true;
          }
          if (index === scalar.length - 1) {
            trailing_space = true;
          }
          if (previous_break) {
            break_space = true;
          }
          previous_break = false;
          previous_space = true;
        } else if (indexOf.call('\n\x85\u2028\u2029', char) >= 0) {
          if (index === 0) {
            leading_break = true;
          }
          if (index === scalar.length - 1) {
            trailing_break = true;
          }
          if (previous_space) {
            space_break = true;
          }
          previous_break = true;
          previous_space = false;
        } else {
          previous_break = false;
          previous_space = false;
        }
        preceded_by_whitespace = indexOf.call(C_WHITESPACE, char) >= 0;
        followed_by_whitespace = index + 2 >= scalar.length || (ref1 = scalar[index + 2], indexOf.call(C_WHITESPACE, ref1) >= 0);
      }
      allow_flow_plain = true;
      allow_block_plain = true;
      allow_single_quoted = true;
      allow_double_quoted = true;
      allow_block = true;
      if (leading_space || leading_break || trailing_space || trailing_break) {
        allow_flow_plain = allow_block_plain = false;
      }
      if (trailing_space) {
        allow_block = false;
      }
      if (break_space) {
        allow_flow_plain = allow_block_plain = allow_single_quoted = false;
      }
      if (space_break || special_characters) {
        allow_flow_plain = allow_block_plain = allow_single_quoted = allow_block = false;
      }
      if (line_breaks) {
        allow_flow_plain = allow_block_plain = false;
      }
      if (flow_indicators) {
        allow_flow_plain = false;
      }
      if (block_indicators) {
        allow_block_plain = false;
      }
      return new ScalarAnalysis(scalar, false, line_breaks, allow_flow_plain, allow_block_plain, allow_single_quoted, allow_double_quoted, allow_block);
    };


    /*
    Write BOM if needed.
     */

    Emitter.prototype.write_stream_start = function() {
      if (this.encoding && this.encoding.indexOf('utf-16') === 0) {
        return this.stream.write('\uFEFF', this.encoding);
      }
    };

    Emitter.prototype.write_stream_end = function() {
      return this.flush_stream();
    };

    Emitter.prototype.write_indicator = function(indicator, need_whitespace, options) {
      var data;
      if (options == null) {
        options = {};
      }
      data = this.whitespace || !need_whitespace ? indicator : ' ' + indicator;
      this.whitespace = !!options.whitespace;
      this.indentation && (this.indentation = !!options.indentation);
      this.column += data.length;
      this.open_ended = false;
      return this.stream.write(data, this.encoding);
    };

    Emitter.prototype.write_indent = function() {
      var data, indent, ref;
      indent = (ref = this.indent) != null ? ref : 0;
      if (!this.indentation || this.column > indent || (this.column === indent && !this.whitespace)) {
        this.write_line_break();
      }
      if (this.column < indent) {
        this.whitespace = true;
        data = new Array(indent - this.column + 1).join(' ');
        this.column = indent;
        return this.stream.write(data, this.encoding);
      }
    };

    Emitter.prototype.write_line_break = function(data) {
      this.whitespace = true;
      this.indentation = true;
      this.line += 1;
      this.column = 0;
      return this.stream.write(data != null ? data : this.best_line_break, this.encoding);
    };

    Emitter.prototype.write_version_directive = function(version_text) {
      this.stream.write("%YAML " + version_text, this.encoding);
      return this.write_line_break();
    };

    Emitter.prototype.write_tag_directive = function(handle_text, prefix_text) {
      this.stream.write("%TAG " + handle_text + " " + prefix_text, this.encoding);
      return this.write_line_break();
    };

    Emitter.prototype.write_single_quoted = function(text, split) {
      var br, breaks, char, data, end, i, len, ref, spaces, start;
      if (split == null) {
        split = true;
      }
      this.write_indicator("'", true);
      spaces = false;
      breaks = false;
      start = end = 0;
      while (end <= text.length) {
        char = text[end];
        if (spaces) {
          if ((char == null) || char !== ' ') {
            if (start + 1 === end && this.column > this.best_width && split && start !== 0 && end !== text.length) {
              this.write_indent();
            } else {
              data = text.slice(start, end);
              this.column += data.length;
              this.stream.write(data, this.encoding);
            }
            start = end;
          }
        } else if (breaks) {
          if ((char == null) || indexOf.call('\n\x85\u2028\u2029', char) < 0) {
            if (text[start] === '\n') {
              this.write_line_break();
            }
            ref = text.slice(start, end);
            for (i = 0, len = ref.length; i < len; i++) {
              br = ref[i];
              if (br === '\n') {
                this.write_line_break();
              } else {
                this.write_line_break(br);
              }
            }
            this.write_indent();
            start = end;
          }
        } else if (((char == null) || indexOf.call(' \n\x85\u2028\u2029', char) >= 0 || char === "'") && start < end) {
          data = text.slice(start, end);
          this.column += data.length;
          this.stream.write(data, this.encoding);
          start = end;
        }
        if (char === "'") {
          this.column += 2;
          this.stream.write("''", this.encoding);
          start = end + 1;
        }
        if (char != null) {
          spaces = char === ' ';
          breaks = indexOf.call('\n\x85\u2028\u2029', char) >= 0;
        }
        end++;
      }
      return this.write_indicator("'", false);
    };

    Emitter.prototype.write_double_quoted = function(text, split) {
      var char, data, end, start;
      if (split == null) {
        split = true;
      }
      this.write_indicator('"', true);
      start = end = 0;
      while (end <= text.length) {
        char = text[end];
        if ((char == null) || indexOf.call('"\\\x85\u2028\u2029\uFEFF', char) >= 0 || !(('\x20' <= char && char <= '\x7E') || (this.allow_unicode && (('\xA0' <= char && char <= '\uD7FF') || ('\uE000' <= char && char <= '\uFFFD'))))) {
          if (start < end) {
            data = text.slice(start, end);
            this.column += data.length;
            this.stream.write(data, this.encoding);
            start = end;
          }
          if (char != null) {
            data = char in ESCAPE_REPLACEMENTS ? '\\' + ESCAPE_REPLACEMENTS[char] : char <= '\xFF' ? "\\x" + (util.pad_left(util.to_hex(char), '0', 2)) : char <= '\uFFFF' ? "\\u" + (util.pad_left(util.to_hex(char), '0', 4)) : "\\U" + (util.pad_left(util.to_hex(char), '0', 16));
            this.column += data.length;
            this.stream.write(data, this.encoding);
            start = end + 1;
          }
        }
        if (split && (0 < end && end < text.length - 1) && (char === ' ' || start >= end) && this.column + (end - start) > this.best_width) {
          data = text.slice(start, end) + "\\";
          if (start < end) {
            start = end;
          }
          this.column += data.length;
          this.stream.write(data, this.encoding);
          this.write_indent();
          this.whitespace = false;
          this.indentation = false;
          if (text[start] === ' ') {
            data = '\\';
            this.column += data.length;
            this.stream.write(data, this.encoding);
          }
        }
        end++;
      }
      return this.write_indicator('"', false);
    };

    Emitter.prototype.write_folded = function(text) {
      var br, breaks, char, data, end, hints, i, leading_space, len, ref, results, spaces, start;
      hints = this.determine_block_hints(text);
      this.write_indicator(">" + hints, true);
      if (hints.slice(-1) === '+') {
        this.open_ended = true;
      }
      this.write_line_break();
      leading_space = true;
      breaks = true;
      spaces = false;
      start = end = 0;
      results = [];
      while (end <= text.length) {
        char = text[end];
        if (breaks) {
          if ((char == null) || indexOf.call('\n\x85\u2028\u2029', char) < 0) {
            if (!leading_space && (char != null) && char !== ' ' && text[start] === '\n') {
              this.write_line_break();
            }
            leading_space = char === ' ';
            ref = text.slice(start, end);
            for (i = 0, len = ref.length; i < len; i++) {
              br = ref[i];
              if (br === '\n') {
                this.write_line_break();
              } else {
                this.write_line_break(br);
              }
            }
            if (char != null) {
              this.write_indent();
            }
            start = end;
          }
        } else if (spaces) {
          if (char !== ' ') {
            if (start + 1 === end && this.column > this.best_width) {
              this.write_indent();
            } else {
              data = text.slice(start, end);
              this.column += data.length;
              this.stream.write(data, this.encoding);
            }
            start = end;
          }
        } else if ((char == null) || indexOf.call(' \n\x85\u2028\u2029', char) >= 0) {
          data = text.slice(start, end);
          this.column += data.length;
          this.stream.write(data, this.encoding);
          if (char == null) {
            this.write_line_break();
          }
          start = end;
        }
        if (char != null) {
          breaks = indexOf.call('\n\x85\u2028\u2029', char) >= 0;
          spaces = char === ' ';
        }
        results.push(end++);
      }
      return results;
    };

    Emitter.prototype.write_literal = function(text) {
      var br, breaks, char, data, end, hints, i, len, ref, results, start;
      hints = this.determine_block_hints(text);
      this.write_indicator("|" + hints, true);
      if (hints.slice(-1) === '+') {
        this.open_ended = true;
      }
      this.write_line_break();
      breaks = true;
      start = end = 0;
      results = [];
      while (end <= text.length) {
        char = text[end];
        if (breaks) {
          if ((char == null) || indexOf.call('\n\x85\u2028\u2029', char) < 0) {
            ref = text.slice(start, end);
            for (i = 0, len = ref.length; i < len; i++) {
              br = ref[i];
              if (br === '\n') {
                this.write_line_break();
              } else {
                this.write_line_break(br);
              }
            }
            if (char != null) {
              this.write_indent();
            }
            start = end;
          }
        } else {
          if ((char == null) || indexOf.call('\n\x85\u2028\u2029', char) >= 0) {
            data = text.slice(start, end);
            this.stream.write(data, this.encoding);
            if (char == null) {
              this.write_line_break();
            }
            start = end;
          }
        }
        if (char != null) {
          breaks = indexOf.call('\n\x85\u2028\u2029', char) >= 0;
        }
        results.push(end++);
      }
      return results;
    };

    Emitter.prototype.write_plain = function(text, split) {
      var br, breaks, char, data, end, i, len, ref, results, spaces, start;
      if (split == null) {
        split = true;
      }
      if (!text) {
        return;
      }
      if (this.root_context) {
        this.open_ended = true;
      }
      if (!this.whitespace) {
        data = ' ';
        this.column += data.length;
        this.stream.write(data, this.encoding);
      }
      this.whitespace = false;
      this.indentation = false;
      spaces = false;
      breaks = false;
      start = end = 0;
      results = [];
      while (end <= text.length) {
        char = text[end];
        if (spaces) {
          if (char !== ' ') {
            if (start + 1 === end && this.column > this.best_width && split) {
              this.write_indent();
              this.whitespace = false;
              this.indentation = false;
            } else {
              data = text.slice(start, end);
              this.column += data.length;
              this.stream.write(data, this.encoding);
            }
            start = end;
          }
        } else if (breaks) {
          if (indexOf.call('\n\x85\u2028\u2029', char) < 0) {
            if (text[start] === '\n') {
              this.write_line_break();
            }
            ref = text.slice(start, end);
            for (i = 0, len = ref.length; i < len; i++) {
              br = ref[i];
              if (br === '\n') {
                this.write_line_break();
              } else {
                this.write_line_break(br);
              }
            }
            this.write_indent();
            this.whitespace = false;
            this.indentation = false;
            start = end;
          }
        } else {
          if ((char == null) || indexOf.call(' \n\x85\u2028\u2029', char) >= 0) {
            data = text.slice(start, end);
            this.column += data.length;
            this.stream.write(data, this.encoding);
            start = end;
          }
        }
        if (char != null) {
          spaces = char === ' ';
          breaks = indexOf.call('\n\x85\u2028\u2029', char) >= 0;
        }
        results.push(end++);
      }
      return results;
    };

    Emitter.prototype.determine_block_hints = function(text) {
      var first, hints, i, last, penultimate;
      hints = '';
      first = text[0], i = text.length - 2, penultimate = text[i++], last = text[i++];
      if (indexOf.call(' \n\x85\u2028\u2029', first) >= 0) {
        hints += this.best_indent;
      }
      if (indexOf.call('\n\x85\u2028\u2029', last) < 0) {
        hints += '-';
      } else if (text.length === 1 || indexOf.call('\n\x85\u2028\u2029', penultimate) >= 0) {
        hints += '+';
      }
      return hints;
    };

    Emitter.prototype.flush_stream = function() {
      var base;
      return typeof (base = this.stream).flush === "function" ? base.flush() : void 0;
    };


    /*
    Helper for common error pattern.
     */

    Emitter.prototype.error = function(message, context) {
      var ref, ref1;
      if (context) {
        context = (ref = context != null ? (ref1 = context.constructor) != null ? ref1.name : void 0 : void 0) != null ? ref : util.inspect(context);
      }
      throw new exports.EmitterError("" + message + (context ? " " + context : ''));
    };

    return Emitter;

  })();

  ScalarAnalysis = (function() {
    function ScalarAnalysis(scalar1, empty, multiline, allow_flow_plain1, allow_block_plain1, allow_single_quoted1, allow_double_quoted1, allow_block1) {
      this.scalar = scalar1;
      this.empty = empty;
      this.multiline = multiline;
      this.allow_flow_plain = allow_flow_plain1;
      this.allow_block_plain = allow_block_plain1;
      this.allow_single_quoted = allow_single_quoted1;
      this.allow_double_quoted = allow_double_quoted1;
      this.allow_block = allow_block1;
    }

    return ScalarAnalysis;

  })();

}).call(this);
