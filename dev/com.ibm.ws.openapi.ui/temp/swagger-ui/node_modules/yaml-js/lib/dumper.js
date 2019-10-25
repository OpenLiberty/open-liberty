(function() {
  var emitter, representer, resolver, serializer, util,
    slice = [].slice;

  util = require('./util');

  emitter = require('./emitter');

  serializer = require('./serializer');

  representer = require('./representer');

  resolver = require('./resolver');

  this.make_dumper = function(Emitter, Serializer, Representer, Resolver) {
    var Dumper, components;
    if (Emitter == null) {
      Emitter = emitter.Emitter;
    }
    if (Serializer == null) {
      Serializer = serializer.Serializer;
    }
    if (Representer == null) {
      Representer = representer.Representer;
    }
    if (Resolver == null) {
      Resolver = resolver.Resolver;
    }
    components = [Emitter, Serializer, Representer, Resolver];
    return Dumper = (function() {
      var component;

      util.extend.apply(util, [Dumper.prototype].concat(slice.call((function() {
        var i, len, results;
        results = [];
        for (i = 0, len = components.length; i < len; i++) {
          component = components[i];
          results.push(component.prototype);
        }
        return results;
      })())));

      function Dumper(stream, options) {
        var i, len, ref;
        if (options == null) {
          options = {};
        }
        components[0].call(this, stream, options);
        ref = components.slice(1);
        for (i = 0, len = ref.length; i < len; i++) {
          component = ref[i];
          component.call(this, options);
        }
      }

      return Dumper;

    })();
  };

  this.Dumper = this.make_dumper();

}).call(this);
