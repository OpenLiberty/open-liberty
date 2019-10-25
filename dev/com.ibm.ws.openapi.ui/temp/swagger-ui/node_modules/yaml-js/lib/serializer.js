(function() {
  var YAMLError, events, nodes, util,
    extend = function(child, parent) { for (var key in parent) { if (hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; },
    hasProp = {}.hasOwnProperty;

  events = require('./events');

  nodes = require('./nodes');

  util = require('./util');

  YAMLError = require('./errors').YAMLError;

  this.SerializerError = (function(superClass) {
    extend(SerializerError, superClass);

    function SerializerError() {
      return SerializerError.__super__.constructor.apply(this, arguments);
    }

    return SerializerError;

  })(YAMLError);

  this.Serializer = (function() {
    function Serializer(arg) {
      var ref;
      ref = arg != null ? arg : {}, this.encoding = ref.encoding, this.explicit_start = ref.explicit_start, this.explicit_end = ref.explicit_end, this.version = ref.version, this.tags = ref.tags;
      this.serialized_nodes = {};
      this.anchors = {};
      this.last_anchor_id = 0;
      this.closed = null;
    }

    Serializer.prototype.open = function() {
      if (this.closed === null) {
        this.emit(new events.StreamStartEvent(this.encoding));
        return this.closed = false;
      } else if (this.closed) {
        throw new SerializerError('serializer is closed');
      } else {
        throw new SerializerError('serializer is already open');
      }
    };

    Serializer.prototype.close = function() {
      if (this.closed === null) {
        throw new SerializerError('serializer is not opened');
      } else if (!this.closed) {
        this.emit(new events.StreamEndEvent);
        return this.closed = true;
      }
    };

    Serializer.prototype.serialize = function(node) {
      if (this.closed === null) {
        throw new SerializerError('serializer is not opened');
      } else if (this.closed) {
        throw new SerializerError('serializer is closed');
      }
      if (node != null) {
        this.emit(new events.DocumentStartEvent(void 0, void 0, this.explicit_start, this.version, this.tags));
        this.anchor_node(node);
        this.serialize_node(node);
        this.emit(new events.DocumentEndEvent(void 0, void 0, this.explicit_end));
      }
      this.serialized_nodes = {};
      this.anchors = {};
      return this.last_anchor_id = 0;
    };

    Serializer.prototype.anchor_node = function(node) {
      var base, i, item, j, key, len, len1, name, ref, ref1, ref2, results, results1, value;
      if (node.unique_id in this.anchors) {
        return (base = this.anchors)[name = node.unique_id] != null ? base[name] : base[name] = this.generate_anchor(node);
      } else {
        this.anchors[node.unique_id] = null;
        if (node instanceof nodes.SequenceNode) {
          ref = node.value;
          results = [];
          for (i = 0, len = ref.length; i < len; i++) {
            item = ref[i];
            results.push(this.anchor_node(item));
          }
          return results;
        } else if (node instanceof nodes.MappingNode) {
          ref1 = node.value;
          results1 = [];
          for (j = 0, len1 = ref1.length; j < len1; j++) {
            ref2 = ref1[j], key = ref2[0], value = ref2[1];
            this.anchor_node(key);
            results1.push(this.anchor_node(value));
          }
          return results1;
        }
      }
    };

    Serializer.prototype.generate_anchor = function(node) {
      return "id" + (util.pad_left(++this.last_anchor_id, '0', 4));
    };

    Serializer.prototype.serialize_node = function(node, parent, index) {
      var alias, default_tag, detected_tag, i, implicit, item, j, key, len, len1, ref, ref1, ref2, value;
      alias = this.anchors[node.unique_id];
      if (node.unique_id in this.serialized_nodes) {
        return this.emit(new events.AliasEvent(alias));
      } else {
        this.serialized_nodes[node.unique_id] = true;
        this.descend_resolver(parent, index);
        if (node instanceof nodes.ScalarNode) {
          detected_tag = this.resolve(nodes.ScalarNode, node.value, [true, false]);
          default_tag = this.resolve(nodes.ScalarNode, node.value, [false, true]);
          implicit = [node.tag === detected_tag, node.tag === default_tag];
          this.emit(new events.ScalarEvent(alias, node.tag, implicit, node.value, void 0, void 0, node.style));
        } else if (node instanceof nodes.SequenceNode) {
          implicit = node.tag === this.resolve(nodes.SequenceNode, node.value, true);
          this.emit(new events.SequenceStartEvent(alias, node.tag, implicit, void 0, void 0, node.flow_style));
          ref = node.value;
          for (index = i = 0, len = ref.length; i < len; index = ++i) {
            item = ref[index];
            this.serialize_node(item, node, index);
          }
          this.emit(new events.SequenceEndEvent);
        } else if (node instanceof nodes.MappingNode) {
          implicit = node.tag === this.resolve(nodes.MappingNode, node.value, true);
          this.emit(new events.MappingStartEvent(alias, node.tag, implicit, void 0, void 0, node.flow_style));
          ref1 = node.value;
          for (j = 0, len1 = ref1.length; j < len1; j++) {
            ref2 = ref1[j], key = ref2[0], value = ref2[1];
            this.serialize_node(key, node, null);
            this.serialize_node(value, node, key);
          }
          this.emit(new events.MappingEndEvent);
        }
        return this.ascend_resolver();
      }
    };

    return Serializer;

  })();

}).call(this);
