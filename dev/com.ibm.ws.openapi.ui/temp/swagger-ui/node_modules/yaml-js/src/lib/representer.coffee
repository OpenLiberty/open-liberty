nodes       = require './nodes'
{YAMLError} = require './errors'

class @RepresenterError extends YAMLError

class @BaseRepresenter

  yaml_representers_types:          []
  yaml_representers_handlers:       []
  yaml_multi_representers_types:    []
  yaml_multi_representers_handlers: []

  @add_representer = (data_type, handler) ->
    unless @::hasOwnProperty 'yaml_representers_types'
      @::yaml_representers_types    = [].concat @::yaml_representers_types
    unless @::hasOwnProperty 'yaml_representers_handlers'
      @::yaml_representers_handlers = [].concat @::yaml_representers_handlers

    @::yaml_representers_types.push data_type
    @::yaml_representers_handlers.push handler

  @add_multi_representer = (data_type, handler) ->
    unless @::hasOwnProperty 'yaml_multi_representers_types'
      @::yaml_multi_representers_types    = [].concat @::yaml_multi_representers_types
    unless @::hasOwnProperty 'yaml_multi_representers_handlers'
      @::yaml_multi_representers_handlers = [].concat @::yaml_multi_representers_handlers

    @::yaml_multi_representers_types.push data_type
    @::yaml_multi_representers_handlers.push handler

  constructor: ({ @default_style, @default_flow_style } = {}) ->
    @represented_objects = {}
    @object_keeper       = []
    @alias_key           = null

  represent: (data) ->
    node = @represent_data data
    @serialize node
    @represented_objects = {}
    @object_keeper       = []
    @alias_key           = null

  represent_data: (data) ->
    if @ignore_aliases data
      @alias_key = null
    else if (i = @object_keeper.indexOf data) isnt -1
      @alias_key = i
      return @represented_objects[@alias_key] if @alias_key of @represented_objects
    else
      @alias_key = @object_keeper.length
      @object_keeper.push data

    # Bit fiddly: we look into our non-multi representers using the JS type if `data` is not an
    # object, otherwise we use the object's constructor.  For multi-representers we just use
    # instanceof.  A representer for `undefined` can be called for any type.
    representer = null
    data_type   = if data is null then 'null' else typeof data
    data_type   = data.constructor if data_type is 'object'

    if (i = @yaml_representers_types.lastIndexOf data_type) isnt -1
      representer = @yaml_representers_handlers[i]

    unless representer?
      for type, i in @yaml_multi_representers_types when data instanceof type
        representer = @yaml_multi_representers_handlers[i]
        break

    unless representer?
      if (i = @yaml_multi_representers_types.lastIndexOf undefined) isnt -1
        representer = @yaml_multi_representers_handlers[i]
      else if (i = @yaml_representers_types.lastIndexOf undefined) isnt -1
        representer = @yaml_representers_handlers[i]

    if representer?
      representer.call @, data
    else
      new nodes.ScalarNode null, "#{data}"

  represent_scalar: (tag, value, style = @default_style) ->
    node = new nodes.ScalarNode tag, value, null, null, style
    @represented_objects[@alias_key] = node if @alias_key?
    node

  represent_sequence: (tag, sequence, flow_style) ->
    value = []
    node  = new nodes.SequenceNode tag, value, null, null, flow_style
    @represented_objects[@alias_key] = node if @alias_key?

    best_style = true
    for item in sequence
      node_item = @represent_data item
      unless node_item instanceof nodes.ScalarNode or node_item.style
        best_style = false
      value.push node_item

    node.flow_style = @default_flow_style ? best_style unless flow_style?
    node

  represent_mapping: (tag, mapping, flow_style) ->
    value = []
    node  = new nodes.MappingNode tag, value, flow_style
    @represented_objects[@alias_key] = node if @alias_key

    best_style = true
    for own item_key, item_value of mapping
      node_key   = @represent_data item_key
      node_value = @represent_data item_value
      unless node_key instanceof nodes.ScalarNode or node_key.style
        best_style = false
      unless node_value instanceof nodes.ScalarNode or node_value.style
        best_style = false
      value.push [ node_key, node_value ]

    node.flow_style = @default_flow_style ? best_style unless flow_style
    node

  ignore_aliases: (data) ->
    false

class @Representer extends @BaseRepresenter

  represent_boolean: (data) ->
    @represent_scalar 'tag:yaml.org,2002:bool', (if data then 'true' else 'false')

  represent_null: (data) ->
    @represent_scalar 'tag:yaml.org,2002:null', 'null'

  represent_number: (data) ->
    tag   = "tag:yaml.org,2002:#{if data % 1 is 0 then 'int' else 'float'}"
    value = if data != data
      '.nan'
    else if data == Infinity
      '.inf'
    else if data == -Infinity
      '-.inf'
    else
      data.toString()
    @represent_scalar tag, value

  represent_string: (data) ->
    @represent_scalar 'tag:yaml.org,2002:str', data

  represent_array: (data) ->
    @represent_sequence 'tag:yaml.org,2002:seq', data

  represent_date: (data) ->
    @represent_scalar 'tag:yaml.org,2002:timestamp', data.toISOString()

  represent_object: (data) ->
    @represent_mapping 'tag:yaml.org,2002:map', data

  represent_undefined: (data) ->
    throw new exports.RepresenterError "cannot represent an onbject: #{data}"

  ignore_aliases: (data) ->
    return true unless data?
    return true if typeof data in [ 'boolean', 'number', 'string' ]
    false

@Representer.add_representer 'boolean', @Representer::represent_boolean
@Representer.add_representer 'null',    @Representer::represent_null
@Representer.add_representer 'number',  @Representer::represent_number
@Representer.add_representer 'string',  @Representer::represent_string
@Representer.add_representer Array,     @Representer::represent_array
@Representer.add_representer Date,      @Representer::represent_date
@Representer.add_representer Object,    @Representer::represent_object
@Representer.add_representer null,      @Representer::represent_undefined