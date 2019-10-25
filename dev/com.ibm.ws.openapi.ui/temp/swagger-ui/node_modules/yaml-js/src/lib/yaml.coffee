composer    = @composer    = require './composer'
constructor = @constructor = require './constructor'
dumper      = @dumper      = require './dumper'
errors      = @errors      = require './errors'
events      = @events      = require './events'
loader      = @loader      = require './loader'
nodes       = @nodes       = require './nodes'
parser      = @parser      = require './parser'
reader      = @reader      = require './reader'
resolver    = @resolver    = require './resolver'
scanner     = @scanner     = require './scanner'
tokens      = @tokens      = require './tokens'
util        = require './util'

###
Scan a YAML stream and produce scanning tokens.
###
@scan = (stream, Loader = loader.Loader) ->
  _loader = new Loader stream
  _loader.get_token() while _loader.check_token()

###
Parse a YAML stream and produce parsing events.
###
@parse = (stream, Loader = loader.Loader) ->
  _loader = new Loader stream
  _loader.get_event() while _loader.check_event()

###
Parse the first YAML document in a stream and produce the corresponding
representation tree.
###
@compose = (stream, Loader = loader.Loader) ->
  _loader = new Loader stream
  _loader.get_single_node()

###
Parse all YAML documents in a stream and produce corresponding representation
trees.
###
@compose_all = (stream, Loader = loader.Loader) ->
  _loader = new Loader stream
  _loader.get_node() while _loader.check_node()

###
Parse the first YAML document in a stream and produce the corresponding
Javascript object.
###
@load = (stream, Loader = loader.Loader) ->
  _loader = new Loader stream
  _loader.get_single_data()

###
Parse all YAML documents in a stream and produce the corresponing Javascript
object.
###
@load_all = (stream, Loader = loader.Loader) ->
  _loader = new Loader stream
  _loader.get_data() while _loader.check_data()

###
Emit YAML parsing events into a stream.
If stream is falsey, return the produced string instead.
###
@emit = (events, stream, Dumper = dumper.Dumper, options = {}) ->
  dest    = stream or new util.StringStream
  _dumper = new Dumper dest, options
  try
    _dumper.emit event for event in events
  finally
    _dumper.dispose()
  stream or dest.string

###
Serialize a representation tree into a YAML stream.
If stream is falsey, return the produced string instead.
###
@serialize = (node, stream, Dumper = dumper.Dumper, options = {}) ->
  exports.serialize_all [ node ], stream, Dumper, options

###
Serialize a sequence of representation tress into a YAML stream.
If stream is falsey, return the produced string instead.
###
@serialize_all = (nodes, stream, Dumper = dumper.Dumper, options = {}) ->
  dest    = stream or new util.StringStream
  _dumper = new Dumper dest, options
  try
    _dumper.open()
    _dumper.serialize node for node in nodes
    _dumper.close()
  finally
    _dumper.dispose()
  stream or dest.string

###
Serialize a Javascript object into a YAML stream.
If stream is falsey, return the produced string instead.
###
@dump = (data, stream, Dumper = dumper.Dumper, options = {}) ->
  exports.dump_all [ data ], stream, Dumper, options

###
Serialize a sequence of Javascript objects into a YAML stream.
If stream is falsey, return the produced string instead.
###
@dump_all = (documents, stream, Dumper = dumper.Dumper, options = {}) ->
  dest    = stream or new util.StringStream
  _dumper = new Dumper dest, options
  try
    _dumper.open()
    _dumper.represent document for document in documents
    _dumper.close()
  finally
    _dumper.dispose()
  stream or dest.string
