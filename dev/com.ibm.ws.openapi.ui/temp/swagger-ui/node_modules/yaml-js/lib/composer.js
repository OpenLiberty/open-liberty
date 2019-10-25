(function() {
  var MarkedYAMLError, events, nodes,
    extend = function(child, parent) { for (var key in parent) { if (hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; },
    hasProp = {}.hasOwnProperty;

  events = require('./events');

  MarkedYAMLError = require('./errors').MarkedYAMLError;

  nodes = require('./nodes');


  /*
  Thrown for errors encountered during composing.
   */

  this.ComposerError = (function(superClass) {
    extend(ComposerError, superClass);

    function ComposerError() {
      return ComposerError.__super__.constructor.apply(this, arguments);
    }

    return ComposerError;

  })(MarkedYAMLError);


  /*
  The composer class handles the construction of representation trees from events.
  
  This uses the methods from {Parser} to process the event stream, and provides a similar stream-like
  interface to representation trees via {Composer#check_node}, {Composer#get_node}, and
  {Composer#get_single_node}.
   */

  this.Composer = (function() {

    /*
    Construct a new `Composer` instance.
     */
    function Composer() {
      this.anchors = {};
    }


    /*
    Checks if a document can be composed from the event stream.
    
    So long as the event stream hasn't ended (no [StreamEndEvent]), another document can be composed.
    
    @return {Boolean} True if a document can be composed, false otherwise.
     */

    Composer.prototype.check_node = function() {
      if (this.check_event(events.StreamStartEvent)) {
        this.get_event();
      }
      return !this.check_event(events.StreamEndEvent);
    };


    /*
    Compose a document from the remaining event stream.
    
    {Composer#check_node} must be called before calling this method.
    
    @return {Node} The next document in the stream. Returns `undefined` if the event stream has ended.
     */

    Composer.prototype.get_node = function() {
      if (!this.check_event(events.StreamEndEvent)) {
        return this.compose_document();
      }
    };


    /*
    Compose a single document from the entire event stream.
    
    @throw {ComposerError} if there's more than one document is in the stream.
    
    @return {Node} The single document in the stream.
     */

    Composer.prototype.get_single_node = function() {
      var document, event;
      this.get_event();
      document = null;
      if (!this.check_event(events.StreamEndEvent)) {
        document = this.compose_document();
      }
      if (!this.check_event(events.StreamEndEvent)) {
        event = this.get_event();
        throw new exports.ComposerError('expected a single document in the stream', document.start_mark, 'but found another document', event.start_mark);
      }
      this.get_event();
      return document;
    };


    /*
    Compose a document node from the event stream.
    
    A 'document' node is any single {Node} subclass.  {DocumentStart} and {DocumentEnd} events delimit
    the events used for composition.
    
    @private
    
    @return {Node} The document node.
     */

    Composer.prototype.compose_document = function() {
      var node;
      this.get_event();
      node = this.compose_node();
      this.get_event();
      this.anchors = {};
      return node;
    };


    /*
    Compose a node from the event stream.
    
    Composes a {ScalarNode}, {SequenceNode}, or {MappingNode} from the event stream, depending on the
    first event encountered ({ScalarEvent}, {SequenceStartEvent}, or {MappingStartEvent}
    respectively).
    
    @private
    
    @param parent {Node} The parent of the new node.
    @param index {Number} The index of the new node within the parent's children.
    @throw {ComposerError} if an alias is encountered for an undefined anchor.
    @throw {ComposerError} if a duplicate anchor is envountered.
    @return {Node} The composed node.
     */

    Composer.prototype.compose_node = function(parent, index) {
      var anchor, event, node;
      if (this.check_event(events.AliasEvent)) {
        event = this.get_event();
        anchor = event.anchor;
        if (!(anchor in this.anchors)) {
          throw new exports.ComposerError(null, null, "found undefined alias " + anchor, event.start_mark);
        }
        return this.anchors[anchor];
      }
      event = this.peek_event();
      anchor = event.anchor;
      if (anchor !== null && anchor in this.anchors) {
        throw new exports.ComposerError("found duplicate anchor " + anchor + "; first occurence", this.anchors[anchor].start_mark, 'second occurrence', event.start_mark);
      }
      this.descend_resolver(parent, index);
      if (this.check_event(events.ScalarEvent)) {
        node = this.compose_scalar_node(anchor);
      } else if (this.check_event(events.SequenceStartEvent)) {
        node = this.compose_sequence_node(anchor);
      } else if (this.check_event(events.MappingStartEvent)) {
        node = this.compose_mapping_node(anchor);
      }
      this.ascend_resolver();
      return node;
    };


    /*
    Compose a {ScalarNode} from the event stream.
    
    @private
    
    @param anchor {String} The anchor name for the node (if any).
    @return {ScalarNode} The node composed from a {ScalarEvent}.
     */

    Composer.prototype.compose_scalar_node = function(anchor) {
      var event, node, tag;
      event = this.get_event();
      tag = event.tag;
      if (tag === null || tag === '!') {
        tag = this.resolve(nodes.ScalarNode, event.value, event.implicit);
      }
      node = new nodes.ScalarNode(tag, event.value, event.start_mark, event.end_mark, event.style);
      if (anchor !== null) {
        this.anchors[anchor] = node;
      }
      return node;
    };


    /*
    Compose a {SequenceNode} from the event stream.
    
    The contents of the node are composed from events between a {SequenceStartEvent} and a
    {SequenceEndEvent}.
    
    @private
    
    @param anchor {String} The anchor name for the node (if any).
    @return {SequenceNode} The composed node.
     */

    Composer.prototype.compose_sequence_node = function(anchor) {
      var end_event, index, node, start_event, tag;
      start_event = this.get_event();
      tag = start_event.tag;
      if (tag === null || tag === '!') {
        tag = this.resolve(nodes.SequenceNode, null, start_event.implicit);
      }
      node = new nodes.SequenceNode(tag, [], start_event.start_mark, null, start_event.flow_style);
      if (anchor !== null) {
        this.anchors[anchor] = node;
      }
      index = 0;
      while (!this.check_event(events.SequenceEndEvent)) {
        node.value.push(this.compose_node(node, index));
        index++;
      }
      end_event = this.get_event();
      node.end_mark = end_event.end_mark;
      return node;
    };


    /*
    Compose a {MappingNode} from the event stream.
    
    The contents of the node are composed from events between a {MappingStartEvent} and a
    {MappingEndEvent}.
    
    @private
    
    @param anchor {String} The anchor name for the node (if any).
    @return {MappingNode} The composed node.
     */

    Composer.prototype.compose_mapping_node = function(anchor) {
      var end_event, item_key, item_value, node, start_event, tag;
      start_event = this.get_event();
      tag = start_event.tag;
      if (tag === null || tag === '!') {
        tag = this.resolve(nodes.MappingNode, null, start_event.implicit);
      }
      node = new nodes.MappingNode(tag, [], start_event.start_mark, null, start_event.flow_style);
      if (anchor !== null) {
        this.anchors[anchor] = node;
      }
      while (!this.check_event(events.MappingEndEvent)) {
        item_key = this.compose_node(node);
        item_value = this.compose_node(node, item_key);
        node.value.push([item_key, item_value]);
      }
      end_event = this.get_event();
      node.end_mark = end_event.end_mark;
      return node;
    };

    return Composer;

  })();

}).call(this);
