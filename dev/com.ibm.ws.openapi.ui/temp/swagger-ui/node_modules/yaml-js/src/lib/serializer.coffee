events      = require './events'
nodes       = require './nodes'
util        = require './util'
{YAMLError} = require './errors'

class @SerializerError extends YAMLError

class @Serializer
  constructor: ({ @encoding, @explicit_start, @explicit_end, @version, @tags } = {}) ->
    @serialized_nodes = {}
    @anchors          = {}
    @last_anchor_id   = 0
    @closed           = null

  open: ->
    if @closed is null
      @emit new events.StreamStartEvent @encoding
      @closed = false
    else if @closed
      throw new SerializerError 'serializer is closed'
    else
      throw new SerializerError 'serializer is already open'

  close: ->
    if @closed is null
      throw new SerializerError 'serializer is not opened'
    else if not @closed
      @emit new events.StreamEndEvent
      @closed = true

  serialize: (node) ->
    if @closed is null
      throw new SerializerError 'serializer is not opened'
    else if @closed
      throw new SerializerError 'serializer is closed'

    if node?
      @emit new events.DocumentStartEvent undefined, undefined, @explicit_start, @version, @tags
      @anchor_node node
      @serialize_node node
      @emit new events.DocumentEndEvent undefined, undefined, @explicit_end

    @serialized_nodes = {}
    @anchors          = {}
    @last_anchor_id   = 0

  anchor_node: (node) ->
    if node.unique_id of @anchors
      @anchors[node.unique_id] ?= @generate_anchor node
    else
      @anchors[node.unique_id] = null
      if node instanceof nodes.SequenceNode
        @anchor_node item for item in node.value
      else if node instanceof nodes.MappingNode
        for [ key, value ] in node.value
          @anchor_node key
          @anchor_node value

  generate_anchor: (node) ->
    "id#{util.pad_left ++@last_anchor_id, '0', 4}"

  serialize_node: (node, parent, index) ->
    alias = @anchors[node.unique_id]
    if node.unique_id of @serialized_nodes
      @emit new events.AliasEvent alias
    else
      @serialized_nodes[node.unique_id] = true
      @descend_resolver parent, index
      if node instanceof nodes.ScalarNode
        detected_tag = @resolve nodes.ScalarNode, node.value, [ true, false ]
        default_tag  = @resolve nodes.ScalarNode, node.value, [ false, true ]
        implicit     = [ node.tag == detected_tag, node.tag == default_tag ]
        @emit new events.ScalarEvent alias, node.tag, implicit, node.value, undefined, undefined, node.style
      else if node instanceof nodes.SequenceNode
        implicit = node.tag == @resolve nodes.SequenceNode, node.value, true
        @emit new events.SequenceStartEvent alias, node.tag, implicit, undefined, undefined, node.flow_style
        @serialize_node item, node, index for item, index in node.value
        @emit new events.SequenceEndEvent
      else if node instanceof nodes.MappingNode
        implicit = node.tag == @resolve nodes.MappingNode, node.value, true
        @emit new events.MappingStartEvent alias, node.tag, implicit, undefined, undefined, node.flow_style
        for [ key, value ] in node.value
          @serialize_node key, node, null
          @serialize_node value, node, key
        @emit new events.MappingEndEvent
      @ascend_resolver()