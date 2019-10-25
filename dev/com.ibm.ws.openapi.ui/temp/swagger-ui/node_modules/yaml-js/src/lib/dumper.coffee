util        = require './util'
emitter     = require './emitter'
serializer  = require './serializer'
representer = require './representer'
resolver    = require './resolver'

@make_dumper = (Emitter = emitter.Emitter, Serializer = serializer.Serializer,
    Representer = representer.Representer, Resolver = resolver.Resolver) ->
  components = [ Emitter, Serializer, Representer, Resolver ]
  class Dumper
    util.extend @prototype, (component.prototype for component in components)...

    constructor: (stream, options = {}) ->
      components[0].call @, stream, options
      component.call @, options for component in components[1..]

@Dumper = @make_dumper()