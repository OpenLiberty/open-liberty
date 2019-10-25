(function() {
  var extend = function(child, parent) { for (var key in parent) { if (hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; },
    hasProp = {}.hasOwnProperty;

  this.Token = (function() {
    function Token(start_mark, end_mark) {
      this.start_mark = start_mark;
      this.end_mark = end_mark;
    }

    return Token;

  })();

  this.DirectiveToken = (function(superClass) {
    extend(DirectiveToken, superClass);

    DirectiveToken.prototype.id = '<directive>';

    function DirectiveToken(name, value, start_mark, end_mark) {
      this.name = name;
      this.value = value;
      this.start_mark = start_mark;
      this.end_mark = end_mark;
    }

    return DirectiveToken;

  })(this.Token);

  this.DocumentStartToken = (function(superClass) {
    extend(DocumentStartToken, superClass);

    function DocumentStartToken() {
      return DocumentStartToken.__super__.constructor.apply(this, arguments);
    }

    DocumentStartToken.prototype.id = '<document start>';

    return DocumentStartToken;

  })(this.Token);

  this.DocumentEndToken = (function(superClass) {
    extend(DocumentEndToken, superClass);

    function DocumentEndToken() {
      return DocumentEndToken.__super__.constructor.apply(this, arguments);
    }

    DocumentEndToken.prototype.id = '<document end>';

    return DocumentEndToken;

  })(this.Token);

  this.StreamStartToken = (function(superClass) {
    extend(StreamStartToken, superClass);

    StreamStartToken.prototype.id = '<stream start>';

    function StreamStartToken(start_mark, end_mark, encoding) {
      this.start_mark = start_mark;
      this.end_mark = end_mark;
      this.encoding = encoding;
    }

    return StreamStartToken;

  })(this.Token);

  this.StreamEndToken = (function(superClass) {
    extend(StreamEndToken, superClass);

    function StreamEndToken() {
      return StreamEndToken.__super__.constructor.apply(this, arguments);
    }

    StreamEndToken.prototype.id = '<stream end>';

    return StreamEndToken;

  })(this.Token);

  this.BlockSequenceStartToken = (function(superClass) {
    extend(BlockSequenceStartToken, superClass);

    function BlockSequenceStartToken() {
      return BlockSequenceStartToken.__super__.constructor.apply(this, arguments);
    }

    BlockSequenceStartToken.prototype.id = '<block sequence start>';

    return BlockSequenceStartToken;

  })(this.Token);

  this.BlockMappingStartToken = (function(superClass) {
    extend(BlockMappingStartToken, superClass);

    function BlockMappingStartToken() {
      return BlockMappingStartToken.__super__.constructor.apply(this, arguments);
    }

    BlockMappingStartToken.prototype.id = '<block mapping end>';

    return BlockMappingStartToken;

  })(this.Token);

  this.BlockEndToken = (function(superClass) {
    extend(BlockEndToken, superClass);

    function BlockEndToken() {
      return BlockEndToken.__super__.constructor.apply(this, arguments);
    }

    BlockEndToken.prototype.id = '<block end>';

    return BlockEndToken;

  })(this.Token);

  this.FlowSequenceStartToken = (function(superClass) {
    extend(FlowSequenceStartToken, superClass);

    function FlowSequenceStartToken() {
      return FlowSequenceStartToken.__super__.constructor.apply(this, arguments);
    }

    FlowSequenceStartToken.prototype.id = '[';

    return FlowSequenceStartToken;

  })(this.Token);

  this.FlowMappingStartToken = (function(superClass) {
    extend(FlowMappingStartToken, superClass);

    function FlowMappingStartToken() {
      return FlowMappingStartToken.__super__.constructor.apply(this, arguments);
    }

    FlowMappingStartToken.prototype.id = '{';

    return FlowMappingStartToken;

  })(this.Token);

  this.FlowSequenceEndToken = (function(superClass) {
    extend(FlowSequenceEndToken, superClass);

    function FlowSequenceEndToken() {
      return FlowSequenceEndToken.__super__.constructor.apply(this, arguments);
    }

    FlowSequenceEndToken.prototype.id = ']';

    return FlowSequenceEndToken;

  })(this.Token);

  this.FlowMappingEndToken = (function(superClass) {
    extend(FlowMappingEndToken, superClass);

    function FlowMappingEndToken() {
      return FlowMappingEndToken.__super__.constructor.apply(this, arguments);
    }

    FlowMappingEndToken.prototype.id = '}';

    return FlowMappingEndToken;

  })(this.Token);

  this.KeyToken = (function(superClass) {
    extend(KeyToken, superClass);

    function KeyToken() {
      return KeyToken.__super__.constructor.apply(this, arguments);
    }

    KeyToken.prototype.id = '?';

    return KeyToken;

  })(this.Token);

  this.ValueToken = (function(superClass) {
    extend(ValueToken, superClass);

    function ValueToken() {
      return ValueToken.__super__.constructor.apply(this, arguments);
    }

    ValueToken.prototype.id = ':';

    return ValueToken;

  })(this.Token);

  this.BlockEntryToken = (function(superClass) {
    extend(BlockEntryToken, superClass);

    function BlockEntryToken() {
      return BlockEntryToken.__super__.constructor.apply(this, arguments);
    }

    BlockEntryToken.prototype.id = '-';

    return BlockEntryToken;

  })(this.Token);

  this.FlowEntryToken = (function(superClass) {
    extend(FlowEntryToken, superClass);

    function FlowEntryToken() {
      return FlowEntryToken.__super__.constructor.apply(this, arguments);
    }

    FlowEntryToken.prototype.id = ',';

    return FlowEntryToken;

  })(this.Token);

  this.AliasToken = (function(superClass) {
    extend(AliasToken, superClass);

    AliasToken.prototype.id = '<alias>';

    function AliasToken(value, start_mark, end_mark) {
      this.value = value;
      this.start_mark = start_mark;
      this.end_mark = end_mark;
    }

    return AliasToken;

  })(this.Token);

  this.AnchorToken = (function(superClass) {
    extend(AnchorToken, superClass);

    AnchorToken.prototype.id = '<anchor>';

    function AnchorToken(value, start_mark, end_mark) {
      this.value = value;
      this.start_mark = start_mark;
      this.end_mark = end_mark;
    }

    return AnchorToken;

  })(this.Token);

  this.TagToken = (function(superClass) {
    extend(TagToken, superClass);

    TagToken.prototype.id = '<tag>';

    function TagToken(value, start_mark, end_mark) {
      this.value = value;
      this.start_mark = start_mark;
      this.end_mark = end_mark;
    }

    return TagToken;

  })(this.Token);

  this.ScalarToken = (function(superClass) {
    extend(ScalarToken, superClass);

    ScalarToken.prototype.id = '<scalar>';

    function ScalarToken(value, plain, start_mark, end_mark, style) {
      this.value = value;
      this.plain = plain;
      this.start_mark = start_mark;
      this.end_mark = end_mark;
      this.style = style;
    }

    return ScalarToken;

  })(this.Token);

}).call(this);
