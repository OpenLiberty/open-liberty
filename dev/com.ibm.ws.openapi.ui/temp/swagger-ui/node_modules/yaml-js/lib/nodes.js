(function() {
  var unique_id,
    extend = function(child, parent) { for (var key in parent) { if (hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; },
    hasProp = {}.hasOwnProperty;

  unique_id = 0;

  this.Node = (function() {
    function Node(tag, value, start_mark, end_mark) {
      this.tag = tag;
      this.value = value;
      this.start_mark = start_mark;
      this.end_mark = end_mark;
      this.unique_id = "node_" + (unique_id++);
    }

    return Node;

  })();

  this.ScalarNode = (function(superClass) {
    extend(ScalarNode, superClass);

    ScalarNode.prototype.id = 'scalar';

    function ScalarNode(tag, value, start_mark, end_mark, style) {
      this.tag = tag;
      this.value = value;
      this.start_mark = start_mark;
      this.end_mark = end_mark;
      this.style = style;
      ScalarNode.__super__.constructor.apply(this, arguments);
    }

    return ScalarNode;

  })(this.Node);

  this.CollectionNode = (function(superClass) {
    extend(CollectionNode, superClass);

    function CollectionNode(tag, value, start_mark, end_mark, flow_style) {
      this.tag = tag;
      this.value = value;
      this.start_mark = start_mark;
      this.end_mark = end_mark;
      this.flow_style = flow_style;
      CollectionNode.__super__.constructor.apply(this, arguments);
    }

    return CollectionNode;

  })(this.Node);

  this.SequenceNode = (function(superClass) {
    extend(SequenceNode, superClass);

    function SequenceNode() {
      return SequenceNode.__super__.constructor.apply(this, arguments);
    }

    SequenceNode.prototype.id = 'sequence';

    return SequenceNode;

  })(this.CollectionNode);

  this.MappingNode = (function(superClass) {
    extend(MappingNode, superClass);

    function MappingNode() {
      return MappingNode.__super__.constructor.apply(this, arguments);
    }

    MappingNode.prototype.id = 'mapping';

    return MappingNode;

  })(this.CollectionNode);

}).call(this);
