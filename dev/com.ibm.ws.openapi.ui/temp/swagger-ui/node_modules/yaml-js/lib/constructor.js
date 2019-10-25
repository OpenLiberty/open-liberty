(function() {
  var MarkedYAMLError, nodes, util,
    extend = function(child, parent) { for (var key in parent) { if (hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; },
    hasProp = {}.hasOwnProperty,
    indexOf = [].indexOf || function(item) { for (var i = 0, l = this.length; i < l; i++) { if (i in this && this[i] === item) return i; } return -1; };

  MarkedYAMLError = require('./errors').MarkedYAMLError;

  nodes = require('./nodes');

  util = require('./util');


  /*
  Thrown for errors encountered during construction.
   */

  this.ConstructorError = (function(superClass) {
    extend(ConstructorError, superClass);

    function ConstructorError() {
      return ConstructorError.__super__.constructor.apply(this, arguments);
    }

    return ConstructorError;

  })(MarkedYAMLError);


  /*
  The constructor class handles the construction of Javascript objects from representation trees
  ({Node}s).
  
  This uses the methods from {Composer} to process the representation stream, and provides a similar
  stream-like interface to Javascript objects via {BaseConstructor#check_node},
  {BaseConstructor#get_node}, and {BaseConstructor#get_single_node}.
   */

  this.BaseConstructor = (function() {

    /*
    @property {Object} A map from a YAML tag to a constructor function for data with that tag.
    @private
     */
    BaseConstructor.prototype.yaml_constructors = {};


    /*
    @property {Object} A map from a YAML tag prefix to a constructor function for data with that tag
                       prefix.
    @private
     */

    BaseConstructor.prototype.yaml_multi_constructors = {};


    /*
    Add a constructor function for a specific tag.
    
    The constructor will be used to turn {Node Nodes} with the given tag into a Javascript object.
    
    @param tag {String} The tag for which the constructor should apply.
    @param constructor {Function<Node,any>} A function that turns a {Node} with the given tag into a
      Javascript object.
    @return {Function<Node,Any>} Returns the supplied `constructor`.
     */

    BaseConstructor.add_constructor = function(tag, constructor) {
      if (!this.prototype.hasOwnProperty('yaml_constructors')) {
        this.prototype.yaml_constructors = util.extend({}, this.prototype.yaml_constructors);
      }
      return this.prototype.yaml_constructors[tag] = constructor;
    };


    /*
    Add a constructor function for a tag prefix.
    
    The constructor will be used to turn {Node Nodes} with the given tag prefix into a Javascript
    object.
    
    @param tag_prefix {String} The tag prefix for which the constructor should apply.
    @param multi_constructor {Function<Node,any>} A function that turns a {Node} with the given tag
      prefix into a Javascript object.
    @return {Function<Node,Any>} Returns the supplied `multi_constructor`.
     */

    BaseConstructor.add_multi_constructor = function(tag_prefix, multi_constructor) {
      if (!this.prototype.hasOwnProperty('yaml_multi_constructors')) {
        this.prototype.yaml_multi_constructors = util.extend({}, this.prototype.yaml_multi_constructors);
      }
      return this.prototype.yaml_multi_constructors[tag_prefix] = multi_constructor;
    };


    /*
    Construct a new `Constructor` instance.
     */

    function BaseConstructor() {
      this.constructed_objects = {};
      this.constructing_nodes = [];
      this.deferred_constructors = [];
    }


    /*
    Checks if a document can be constructed from the representation stream.
    
    So long as the representation stream hasn't ended, another document can be constructed.
    
    @return {Boolean} True if a document can be constructed, false otherwise.
     */

    BaseConstructor.prototype.check_data = function() {
      return this.check_node();
    };


    /*
    Construct a document from the remaining representation stream.
    
    {Constructor#check_data} must be called before calling this method.
    
    @return {any} The next document in the stream. Returns `undefined` if the stream has ended.
     */

    BaseConstructor.prototype.get_data = function() {
      if (this.check_node()) {
        return this.construct_document(this.get_node());
      }
    };


    /*
    Construct a single document from the entire representation stream.
    
    @throw {ComposerError} if there's more than one document is in the stream.
    
    @return {Node} The single document in the stream.
     */

    BaseConstructor.prototype.get_single_data = function() {
      var node;
      node = this.get_single_node();
      if (node != null) {
        return this.construct_document(node);
      }
      return null;
    };


    /*
    Construct a document node
    
    @private
     */

    BaseConstructor.prototype.construct_document = function(node) {
      var data;
      data = this.construct_object(node);
      while (!util.is_empty(this.deferred_constructors)) {
        this.deferred_constructors.pop()();
      }
      return data;
    };

    BaseConstructor.prototype.defer = function(f) {
      return this.deferred_constructors.push(f);
    };

    BaseConstructor.prototype.construct_object = function(node) {
      var constructor, object, ref, tag_prefix, tag_suffix;
      if (node.unique_id in this.constructed_objects) {
        return this.constructed_objects[node.unique_id];
      }
      if (ref = node.unique_id, indexOf.call(this.constructing_nodes, ref) >= 0) {
        throw new exports.ConstructorError(null, null, 'found unconstructable recursive node', node.start_mark);
      }
      this.constructing_nodes.push(node.unique_id);
      constructor = null;
      tag_suffix = null;
      if (node.tag in this.yaml_constructors) {
        constructor = this.yaml_constructors[node.tag];
      } else {
        for (tag_prefix in this.yaml_multi_constructors) {
          if (node.tag.indexOf(tag_prefix === 0)) {
            tag_suffix = node.tag.slice(tag_prefix.length);
            constructor = this.yaml_multi_constructors[tag_prefix];
            break;
          }
        }
        if (constructor == null) {
          if (null in this.yaml_multi_constructors) {
            tag_suffix = node.tag;
            constructor = this.yaml_multi_constructors[null];
          } else if (null in this.yaml_constructors) {
            constructor = this.yaml_constructors[null];
          } else if (node instanceof nodes.ScalarNode) {
            constructor = this.construct_scalar;
          } else if (node instanceof nodes.SequenceNode) {
            constructor = this.construct_sequence;
          } else if (node instanceof nodes.MappingNode) {
            constructor = this.construct_mapping;
          }
        }
      }
      object = constructor.call(this, tag_suffix != null ? tag_suffix : node, node);
      this.constructed_objects[node.unique_id] = object;
      this.constructing_nodes.pop();
      return object;
    };

    BaseConstructor.prototype.construct_scalar = function(node) {
      if (!(node instanceof nodes.ScalarNode)) {
        throw new exports.ConstructorError(null, null, "expected a scalar node but found " + node.id, node.start_mark);
      }
      return node.value;
    };

    BaseConstructor.prototype.construct_sequence = function(node) {
      var child, i, len, ref, results;
      if (!(node instanceof nodes.SequenceNode)) {
        throw new exports.ConstructorError(null, null, "expected a sequence node but found " + node.id, node.start_mark);
      }
      ref = node.value;
      results = [];
      for (i = 0, len = ref.length; i < len; i++) {
        child = ref[i];
        results.push(this.construct_object(child));
      }
      return results;
    };

    BaseConstructor.prototype.construct_mapping = function(node) {
      var i, key, key_node, len, mapping, ref, ref1, value, value_node;
      if (!(node instanceof nodes.MappingNode)) {
        throw new ConstructorError(null, null, "expected a mapping node but found " + node.id, node.start_mark);
      }
      mapping = {};
      ref = node.value;
      for (i = 0, len = ref.length; i < len; i++) {
        ref1 = ref[i], key_node = ref1[0], value_node = ref1[1];
        key = this.construct_object(key_node);
        if (typeof key === 'object') {
          throw new exports.ConstructorError('while constructing a mapping', node.start_mark, 'found unhashable key', key_node.start_mark);
        }
        value = this.construct_object(value_node);
        mapping[key] = value;
      }
      return mapping;
    };

    BaseConstructor.prototype.construct_pairs = function(node) {
      var i, key, key_node, len, pairs, ref, ref1, value, value_node;
      if (!(node instanceof nodes.MappingNode)) {
        throw new exports.ConstructorError(null, null, "expected a mapping node but found " + node.id, node.start_mark);
      }
      pairs = [];
      ref = node.value;
      for (i = 0, len = ref.length; i < len; i++) {
        ref1 = ref[i], key_node = ref1[0], value_node = ref1[1];
        key = this.construct_object(key_node);
        value = this.construct_object(value_node);
        pairs.push([key, value]);
      }
      return pairs;
    };

    return BaseConstructor;

  })();

  this.Constructor = (function(superClass) {
    var BOOL_VALUES, TIMESTAMP_PARTS, TIMESTAMP_REGEX;

    extend(Constructor, superClass);

    function Constructor() {
      return Constructor.__super__.constructor.apply(this, arguments);
    }

    BOOL_VALUES = {
      on: true,
      off: false,
      "true": true,
      "false": false,
      yes: true,
      no: false
    };

    TIMESTAMP_REGEX = /^([0-9][0-9][0-9][0-9])-([0-9][0-9]?)-([0-9][0-9]?)(?:(?:[Tt]|[\x20\t]+)([0-9][0-9]?):([0-9][0-9]):([0-9][0-9])(?:\.([0-9]*))?(?:[\x20\t]*(Z|([-+])([0-9][0-9]?)(?::([0-9][0-9]))?))?)?$/;

    TIMESTAMP_PARTS = {
      year: 1,
      month: 2,
      day: 3,
      hour: 4,
      minute: 5,
      second: 6,
      fraction: 7,
      tz: 8,
      tz_sign: 9,
      tz_hour: 10,
      tz_minute: 11
    };

    Constructor.prototype.construct_scalar = function(node) {
      var i, key_node, len, ref, ref1, value_node;
      if (node instanceof nodes.MappingNode) {
        ref = node.value;
        for (i = 0, len = ref.length; i < len; i++) {
          ref1 = ref[i], key_node = ref1[0], value_node = ref1[1];
          if (key_node.tag === 'tag:yaml.org,2002:value') {
            return this.construct_scalar(value_node);
          }
        }
      }
      return Constructor.__super__.construct_scalar.call(this, node);
    };

    Constructor.prototype.flatten_mapping = function(node) {
      var i, index, j, key_node, len, len1, merge, ref, ref1, submerge, subnode, value, value_node;
      merge = [];
      index = 0;
      while (index < node.value.length) {
        ref = node.value[index], key_node = ref[0], value_node = ref[1];
        if (key_node.tag === 'tag:yaml.org,2002:merge') {
          node.value.splice(index, 1);
          if (value_node instanceof nodes.MappingNode) {
            this.flatten_mapping(value_node);
            merge = merge.concat(value_node.value);
          } else if (value_node instanceof nodes.SequenceNode) {
            submerge = [];
            ref1 = value_node.value;
            for (i = 0, len = ref1.length; i < len; i++) {
              subnode = ref1[i];
              if (!(subnode instanceof nodes.MappingNode)) {
                throw new exports.ConstructorError('while constructing a mapping', node.start_mark, "expected a mapping for merging, but found " + subnode.id, subnode.start_mark);
              }
              this.flatten_mapping(subnode);
              submerge.push(subnode.value);
            }
            submerge.reverse();
            for (j = 0, len1 = submerge.length; j < len1; j++) {
              value = submerge[j];
              merge = merge.concat(value);
            }
          } else {
            throw new exports.ConstructorError('while constructing a mapping', node.start_mark, "expected a mapping or list of mappings for merging but found " + value_node.id, value_node.start_mark);
          }
        } else if (key_node.tag === 'tag:yaml.org,2002:value') {
          key_node.tag = 'tag:yaml.org,2002:str';
          index++;
        } else {
          index++;
        }
      }
      if (merge.length) {
        return node.value = merge.concat(node.value);
      }
    };

    Constructor.prototype.construct_mapping = function(node) {
      if (node instanceof nodes.MappingNode) {
        this.flatten_mapping(node);
      }
      return Constructor.__super__.construct_mapping.call(this, node);
    };

    Constructor.prototype.construct_yaml_null = function(node) {
      this.construct_scalar(node);
      return null;
    };

    Constructor.prototype.construct_yaml_bool = function(node) {
      var value;
      value = this.construct_scalar(node);
      return BOOL_VALUES[value.toLowerCase()];
    };

    Constructor.prototype.construct_yaml_int = function(node) {
      var base, digit, digits, i, len, part, ref, sign, value;
      value = this.construct_scalar(node);
      value = value.replace(/_/g, '');
      sign = value[0] === '-' ? -1 : 1;
      if (ref = value[0], indexOf.call('+-', ref) >= 0) {
        value = value.slice(1);
      }
      if (value === '0') {
        return 0;
      } else if (value.indexOf('0b') === 0) {
        return sign * parseInt(value.slice(2), 2);
      } else if (value.indexOf('0x') === 0) {
        return sign * parseInt(value.slice(2), 16);
      } else if (value.indexOf('0o') === 0) {
        return sign * parseInt(value.slice(2), 8);
      } else if (value[0] === '0') {
        return sign * parseInt(value, 8);
      } else if (indexOf.call(value, ':') >= 0) {
        digits = (function() {
          var i, len, ref1, results;
          ref1 = value.split(/:/g);
          results = [];
          for (i = 0, len = ref1.length; i < len; i++) {
            part = ref1[i];
            results.push(parseInt(part));
          }
          return results;
        })();
        digits.reverse();
        base = 1;
        value = 0;
        for (i = 0, len = digits.length; i < len; i++) {
          digit = digits[i];
          value += digit * base;
          base *= 60;
        }
        return sign * value;
      } else {
        return sign * parseInt(value);
      }
    };

    Constructor.prototype.construct_yaml_float = function(node) {
      var base, digit, digits, i, len, part, ref, sign, value;
      value = this.construct_scalar(node);
      value = value.replace(/_/g, '').toLowerCase();
      sign = value[0] === '-' ? -1 : 1;
      if (ref = value[0], indexOf.call('+-', ref) >= 0) {
        value = value.slice(1);
      }
      if (value === '.inf') {
        return sign * 2e308;
      } else if (value === '.nan') {
        return 0/0;
      } else if (indexOf.call(value, ':') >= 0) {
        digits = (function() {
          var i, len, ref1, results;
          ref1 = value.split(/:/g);
          results = [];
          for (i = 0, len = ref1.length; i < len; i++) {
            part = ref1[i];
            results.push(parseFloat(part));
          }
          return results;
        })();
        digits.reverse();
        base = 1;
        value = 0.0;
        for (i = 0, len = digits.length; i < len; i++) {
          digit = digits[i];
          value += digit * base;
          base *= 60;
        }
        return sign * value;
      } else {
        return sign * parseFloat(value);
      }
    };

    Constructor.prototype.construct_yaml_binary = function(node) {
      var error, value;
      value = this.construct_scalar(node);
      try {
        if (typeof window !== "undefined" && window !== null) {
          return atob(value);
        }
        return new Buffer(value, 'base64').toString('ascii');
      } catch (error1) {
        error = error1;
        throw new exports.ConstructorError(null, null, "failed to decode base64 data: " + error, node.start_mark);
      }
    };

    Constructor.prototype.construct_yaml_timestamp = function(node) {
      var date, day, fraction, hour, index, key, match, millisecond, minute, month, second, tz_hour, tz_minute, tz_sign, value, values, year;
      value = this.construct_scalar(node);
      match = node.value.match(TIMESTAMP_REGEX);
      values = {};
      for (key in TIMESTAMP_PARTS) {
        index = TIMESTAMP_PARTS[key];
        values[key] = match[index];
      }
      year = parseInt(values.year);
      month = parseInt(values.month) - 1;
      day = parseInt(values.day);
      if (!values.hour) {
        return new Date(Date.UTC(year, month, day));
      }
      hour = parseInt(values.hour);
      minute = parseInt(values.minute);
      second = parseInt(values.second);
      millisecond = 0;
      if (values.fraction) {
        fraction = values.fraction.slice(0, 6);
        while (fraction.length < 6) {
          fraction += '0';
        }
        fraction = parseInt(fraction);
        millisecond = Math.round(fraction / 1000);
      }
      if (values.tz_sign) {
        tz_sign = values.tz_sign === '-' ? 1 : -1;
        if (tz_hour = parseInt(values.tz_hour)) {
          hour += tz_sign * tz_hour;
        }
        if (tz_minute = parseInt(values.tz_minute)) {
          minute += tz_sign * tz_minute;
        }
      }
      date = new Date(Date.UTC(year, month, day, hour, minute, second, millisecond));
      return date;
    };

    Constructor.prototype.construct_yaml_pair_list = function(type, node) {
      var list;
      list = [];
      if (!(node instanceof nodes.SequenceNode)) {
        throw new exports.ConstructorError("while constructing " + type, node.start_mark, "expected a sequence but found " + node.id, node.start_mark);
      }
      this.defer((function(_this) {
        return function() {
          var i, key, key_node, len, ref, ref1, results, subnode, value, value_node;
          ref = node.value;
          results = [];
          for (i = 0, len = ref.length; i < len; i++) {
            subnode = ref[i];
            if (!(subnode instanceof nodes.MappingNode)) {
              throw new exports.ConstructorError("while constructing " + type, node.start_mark, "expected a mapping of length 1 but found " + subnode.id, subnode.start_mark);
            }
            if (subnode.value.length !== 1) {
              throw new exports.ConstructorError("while constructing " + type, node.start_mark, "expected a mapping of length 1 but found " + subnode.id, subnode.start_mark);
            }
            ref1 = subnode.value[0], key_node = ref1[0], value_node = ref1[1];
            key = _this.construct_object(key_node);
            value = _this.construct_object(value_node);
            results.push(list.push([key, value]));
          }
          return results;
        };
      })(this));
      return list;
    };

    Constructor.prototype.construct_yaml_omap = function(node) {
      return this.construct_yaml_pair_list('an ordered map', node);
    };

    Constructor.prototype.construct_yaml_pairs = function(node) {
      return this.construct_yaml_pair_list('pairs', node);
    };

    Constructor.prototype.construct_yaml_set = function(node) {
      var data;
      data = [];
      this.defer((function(_this) {
        return function() {
          var item, results;
          results = [];
          for (item in _this.construct_mapping(node)) {
            results.push(data.push(item));
          }
          return results;
        };
      })(this));
      return data;
    };

    Constructor.prototype.construct_yaml_str = function(node) {
      return this.construct_scalar(node);
    };

    Constructor.prototype.construct_yaml_seq = function(node) {
      var data;
      data = [];
      this.defer((function(_this) {
        return function() {
          var i, item, len, ref, results;
          ref = _this.construct_sequence(node);
          results = [];
          for (i = 0, len = ref.length; i < len; i++) {
            item = ref[i];
            results.push(data.push(item));
          }
          return results;
        };
      })(this));
      return data;
    };

    Constructor.prototype.construct_yaml_map = function(node) {
      var data;
      data = {};
      this.defer((function(_this) {
        return function() {
          var key, ref, results, value;
          ref = _this.construct_mapping(node);
          results = [];
          for (key in ref) {
            value = ref[key];
            results.push(data[key] = value);
          }
          return results;
        };
      })(this));
      return data;
    };

    Constructor.prototype.construct_yaml_object = function(node, klass) {
      var data;
      data = new klass;
      this.defer((function(_this) {
        return function() {
          var key, ref, results, value;
          ref = _this.construct_mapping(node, true);
          results = [];
          for (key in ref) {
            value = ref[key];
            results.push(data[key] = value);
          }
          return results;
        };
      })(this));
      return data;
    };

    Constructor.prototype.construct_undefined = function(node) {
      throw new exports.ConstructorError(null, null, "could not determine a constructor for the tag " + node.tag, node.start_mark);
    };

    return Constructor;

  })(this.BaseConstructor);

  this.Constructor.add_constructor('tag:yaml.org,2002:null', this.Constructor.prototype.construct_yaml_null);

  this.Constructor.add_constructor('tag:yaml.org,2002:bool', this.Constructor.prototype.construct_yaml_bool);

  this.Constructor.add_constructor('tag:yaml.org,2002:int', this.Constructor.prototype.construct_yaml_int);

  this.Constructor.add_constructor('tag:yaml.org,2002:float', this.Constructor.prototype.construct_yaml_float);

  this.Constructor.add_constructor('tag:yaml.org,2002:binary', this.Constructor.prototype.construct_yaml_binary);

  this.Constructor.add_constructor('tag:yaml.org,2002:timestamp', this.Constructor.prototype.construct_yaml_timestamp);

  this.Constructor.add_constructor('tag:yaml.org,2002:omap', this.Constructor.prototype.construct_yaml_omap);

  this.Constructor.add_constructor('tag:yaml.org,2002:pairs', this.Constructor.prototype.construct_yaml_pairs);

  this.Constructor.add_constructor('tag:yaml.org,2002:set', this.Constructor.prototype.construct_yaml_set);

  this.Constructor.add_constructor('tag:yaml.org,2002:str', this.Constructor.prototype.construct_yaml_str);

  this.Constructor.add_constructor('tag:yaml.org,2002:seq', this.Constructor.prototype.construct_yaml_seq);

  this.Constructor.add_constructor('tag:yaml.org,2002:map', this.Constructor.prototype.construct_yaml_map);

  this.Constructor.add_constructor(null, this.Constructor.prototype.construct_undefined);

}).call(this);
