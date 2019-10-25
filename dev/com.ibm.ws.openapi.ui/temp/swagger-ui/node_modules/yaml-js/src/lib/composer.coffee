events            = require './events'
{MarkedYAMLError} = require './errors'
nodes             = require './nodes'

###
Thrown for errors encountered during composing.
###
class @ComposerError extends MarkedYAMLError

###
The composer class handles the construction of representation trees from events.

This uses the methods from {Parser} to process the event stream, and provides a similar stream-like
interface to representation trees via {Composer#check_node}, {Composer#get_node}, and
{Composer#get_single_node}.
###
class @Composer

  ###
  Construct a new `Composer` instance.
  ###
  constructor: ->
    # @property {Object} A mapping from anchor names to nodes.
    @anchors = {}

  ###
  Checks if a document can be composed from the event stream.

  So long as the event stream hasn't ended (no [StreamEndEvent]), another document can be composed.

  @return {Boolean} True if a document can be composed, false otherwise.
  ###
  check_node: ->
    # Drop the STREAM-START event.
    @get_event() if @check_event events.StreamStartEvent

    # Are there more documents available?
    return not @check_event events.StreamEndEvent

  ###
  Compose a document from the remaining event stream.

  {Composer#check_node} must be called before calling this method.

  @return {Node} The next document in the stream. Returns `undefined` if the event stream has ended.
  ###
  get_node: ->
    return @compose_document() unless @check_event events.StreamEndEvent

  ###
  Compose a single document from the entire event stream.

  @throw {ComposerError} if there's more than one document is in the stream.

  @return {Node} The single document in the stream.
  ###
  get_single_node: ->
    # Drop the STREAM-START event.
    @get_event()

    # Compose a document if the stream is not empty.
    document = null
    document = @compose_document() unless @check_event events.StreamEndEvent

    # Ensure that the stream contains no more documents.
    unless @check_event events.StreamEndEvent
      event = @get_event()
      throw new exports.ComposerError 'expected a single document in the stream',
        document.start_mark, 'but found another document', event.start_mark

    # Drop the STREAM-END event.
    @get_event()

    return document

  ###
  Compose a document node from the event stream.

  A 'document' node is any single {Node} subclass.  {DocumentStart} and {DocumentEnd} events delimit
  the events used for composition.

  @private

  @return {Node} The document node.
  ###
  compose_document: ->
    # Drop the DOCUMENT-START event.
    @get_event()

    # Compose the root node.
    node = @compose_node()

    # Drop the DOCUMENT-END node.
    @get_event()

    # Reset the anchors
    @anchors = {}

    return node

  ###
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
  ###
  compose_node: (parent, index) ->
    if @check_event events.AliasEvent
      event = @get_event()
      anchor = event.anchor
      throw new exports.ComposerError null, null, \
        "found undefined alias #{anchor}", event.start_mark \
        if anchor not of @anchors
      return @anchors[anchor]

    event = @peek_event()
    anchor = event.anchor
    throw new exports.ComposerError \
      "found duplicate anchor #{anchor}; first occurence", \
      @anchors[anchor].start_mark, 'second occurrence', event.start_mark \
      if anchor isnt null and anchor of @anchors

    @descend_resolver parent, index
    if @check_event events.ScalarEvent
      node = @compose_scalar_node anchor
    else if @check_event events.SequenceStartEvent
      node = @compose_sequence_node anchor
    else if @check_event events.MappingStartEvent
      node = @compose_mapping_node anchor
    @ascend_resolver()

    return node

  ###
  Compose a {ScalarNode} from the event stream.

  @private

  @param anchor {String} The anchor name for the node (if any).
  @return {ScalarNode} The node composed from a {ScalarEvent}.
  ###
  compose_scalar_node: (anchor) ->
    event = @get_event()
    tag = event.tag
    tag = @resolve nodes.ScalarNode, event.value, event.implicit \
      if tag is null or tag is '!'
    node = new nodes.ScalarNode tag, event.value, event.start_mark,
      event.end_mark, event.style
    @anchors[anchor] = node if anchor isnt null
    return node

  ###
  Compose a {SequenceNode} from the event stream.

  The contents of the node are composed from events between a {SequenceStartEvent} and a
  {SequenceEndEvent}.

  @private

  @param anchor {String} The anchor name for the node (if any).
  @return {SequenceNode} The composed node.
  ###
  compose_sequence_node: (anchor) ->
    start_event = @get_event()
    tag = start_event.tag
    tag = @resolve nodes.SequenceNode, null, start_event.implicit \
      if tag is null or tag is '!'
    node = new nodes.SequenceNode tag, [], start_event.start_mark, null,
      start_event.flow_style
    @anchors[anchor] = node if anchor isnt null
    index = 0
    while not @check_event events.SequenceEndEvent
      node.value.push @compose_node node, index
      index++
    end_event = @get_event()
    node.end_mark = end_event.end_mark
    return node

  ###
  Compose a {MappingNode} from the event stream.

  The contents of the node are composed from events between a {MappingStartEvent} and a
  {MappingEndEvent}.

  @private

  @param anchor {String} The anchor name for the node (if any).
  @return {MappingNode} The composed node.
  ###
  compose_mapping_node: (anchor) ->
    start_event = @get_event()
    tag = start_event.tag
    tag = @resolve nodes.MappingNode, null, start_event.implicit \
      if tag is null or tag is '!'
    node = new nodes.MappingNode tag, [], start_event.start_mark, null,
      start_event.flow_style
    @anchors[anchor] = node if anchor isnt null
    while not @check_event events.MappingEndEvent
      item_key   = @compose_node node
      item_value = @compose_node node, item_key
      node.value.push [item_key, item_value]
    end_event = @get_event()
    node.end_mark = end_event.end_mark
    return node
