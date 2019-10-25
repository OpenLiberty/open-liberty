(function() {
  var extend = function(child, parent) { for (var key in parent) { if (hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; },
    hasProp = {}.hasOwnProperty;

  this.Event = (function() {
    function Event(start_mark, end_mark) {
      this.start_mark = start_mark;
      this.end_mark = end_mark;
    }

    return Event;

  })();

  this.NodeEvent = (function(superClass) {
    extend(NodeEvent, superClass);

    function NodeEvent(anchor, start_mark, end_mark) {
      this.anchor = anchor;
      this.start_mark = start_mark;
      this.end_mark = end_mark;
    }

    return NodeEvent;

  })(this.Event);

  this.CollectionStartEvent = (function(superClass) {
    extend(CollectionStartEvent, superClass);

    function CollectionStartEvent(anchor, tag, implicit, start_mark, end_mark, flow_style) {
      this.anchor = anchor;
      this.tag = tag;
      this.implicit = implicit;
      this.start_mark = start_mark;
      this.end_mark = end_mark;
      this.flow_style = flow_style;
    }

    return CollectionStartEvent;

  })(this.NodeEvent);

  this.CollectionEndEvent = (function(superClass) {
    extend(CollectionEndEvent, superClass);

    function CollectionEndEvent() {
      return CollectionEndEvent.__super__.constructor.apply(this, arguments);
    }

    return CollectionEndEvent;

  })(this.Event);

  this.StreamStartEvent = (function(superClass) {
    extend(StreamStartEvent, superClass);

    function StreamStartEvent(start_mark, end_mark, encoding) {
      this.start_mark = start_mark;
      this.end_mark = end_mark;
      this.encoding = encoding;
    }

    return StreamStartEvent;

  })(this.Event);

  this.StreamEndEvent = (function(superClass) {
    extend(StreamEndEvent, superClass);

    function StreamEndEvent() {
      return StreamEndEvent.__super__.constructor.apply(this, arguments);
    }

    return StreamEndEvent;

  })(this.Event);

  this.DocumentStartEvent = (function(superClass) {
    extend(DocumentStartEvent, superClass);

    function DocumentStartEvent(start_mark, end_mark, explicit, version, tags) {
      this.start_mark = start_mark;
      this.end_mark = end_mark;
      this.explicit = explicit;
      this.version = version;
      this.tags = tags;
    }

    return DocumentStartEvent;

  })(this.Event);

  this.DocumentEndEvent = (function(superClass) {
    extend(DocumentEndEvent, superClass);

    function DocumentEndEvent(start_mark, end_mark, explicit) {
      this.start_mark = start_mark;
      this.end_mark = end_mark;
      this.explicit = explicit;
    }

    return DocumentEndEvent;

  })(this.Event);

  this.AliasEvent = (function(superClass) {
    extend(AliasEvent, superClass);

    function AliasEvent() {
      return AliasEvent.__super__.constructor.apply(this, arguments);
    }

    return AliasEvent;

  })(this.NodeEvent);

  this.ScalarEvent = (function(superClass) {
    extend(ScalarEvent, superClass);

    function ScalarEvent(anchor, tag, implicit, value, start_mark, end_mark, style) {
      this.anchor = anchor;
      this.tag = tag;
      this.implicit = implicit;
      this.value = value;
      this.start_mark = start_mark;
      this.end_mark = end_mark;
      this.style = style;
    }

    return ScalarEvent;

  })(this.NodeEvent);

  this.SequenceStartEvent = (function(superClass) {
    extend(SequenceStartEvent, superClass);

    function SequenceStartEvent() {
      return SequenceStartEvent.__super__.constructor.apply(this, arguments);
    }

    return SequenceStartEvent;

  })(this.CollectionStartEvent);

  this.SequenceEndEvent = (function(superClass) {
    extend(SequenceEndEvent, superClass);

    function SequenceEndEvent() {
      return SequenceEndEvent.__super__.constructor.apply(this, arguments);
    }

    return SequenceEndEvent;

  })(this.CollectionEndEvent);

  this.MappingStartEvent = (function(superClass) {
    extend(MappingStartEvent, superClass);

    function MappingStartEvent() {
      return MappingStartEvent.__super__.constructor.apply(this, arguments);
    }

    return MappingStartEvent;

  })(this.CollectionStartEvent);

  this.MappingEndEvent = (function(superClass) {
    extend(MappingEndEvent, superClass);

    function MappingEndEvent() {
      return MappingEndEvent.__super__.constructor.apply(this, arguments);
    }

    return MappingEndEvent;

  })(this.CollectionEndEvent);

}).call(this);
